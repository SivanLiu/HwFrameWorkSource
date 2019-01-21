package org.apache.http.impl.conn.tsccm;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.params.HttpParams;

@Deprecated
public class ConnPoolByRoute extends AbstractConnPool {
    private final ConnPerRoute connPerRoute;
    protected Queue<BasicPoolEntry> freeConnections;
    private final Log log = LogFactory.getLog(getClass());
    protected final int maxTotalConnections;
    protected final ClientConnectionOperator operator;
    protected final Map<HttpRoute, RouteSpecificPool> routeToPool;
    protected Queue<WaitingThread> waitingThreads;

    public ConnPoolByRoute(ClientConnectionOperator operator, HttpParams params) {
        if (operator != null) {
            this.operator = operator;
            this.freeConnections = createFreeConnQueue();
            this.waitingThreads = createWaitingThreadQueue();
            this.routeToPool = createRouteToPoolMap();
            this.maxTotalConnections = ConnManagerParams.getMaxTotalConnections(params);
            this.connPerRoute = ConnManagerParams.getMaxConnectionsPerRoute(params);
            return;
        }
        throw new IllegalArgumentException("Connection operator may not be null");
    }

    protected Queue<BasicPoolEntry> createFreeConnQueue() {
        return new LinkedList();
    }

    protected Queue<WaitingThread> createWaitingThreadQueue() {
        return new LinkedList();
    }

    protected Map<HttpRoute, RouteSpecificPool> createRouteToPoolMap() {
        return new HashMap();
    }

    protected RouteSpecificPool newRouteSpecificPool(HttpRoute route) {
        return new RouteSpecificPool(route, this.connPerRoute.getMaxForRoute(route));
    }

    protected WaitingThread newWaitingThread(Condition cond, RouteSpecificPool rospl) {
        return new WaitingThread(cond, rospl);
    }

    protected RouteSpecificPool getRoutePool(HttpRoute route, boolean create) {
        this.poolLock.lock();
        try {
            RouteSpecificPool rospl = (RouteSpecificPool) this.routeToPool.get(route);
            if (rospl == null && create) {
                rospl = newRouteSpecificPool(route);
                this.routeToPool.put(route, rospl);
            }
            this.poolLock.unlock();
            return rospl;
        } catch (Throwable th) {
            this.poolLock.unlock();
        }
    }

    public int getConnectionsInPool(HttpRoute route) {
        this.poolLock.lock();
        int i = 0;
        try {
            RouteSpecificPool rospl = getRoutePool(route, false);
            if (rospl != null) {
                i = rospl.getEntryCount();
            }
            this.poolLock.unlock();
            return i;
        } catch (Throwable th) {
            this.poolLock.unlock();
        }
    }

    public PoolEntryRequest requestPoolEntry(final HttpRoute route, final Object state) {
        final WaitingThreadAborter aborter = new WaitingThreadAborter();
        return new PoolEntryRequest() {
            public void abortRequest() {
                ConnPoolByRoute.this.poolLock.lock();
                try {
                    aborter.abort();
                } finally {
                    ConnPoolByRoute.this.poolLock.unlock();
                }
            }

            public BasicPoolEntry getPoolEntry(long timeout, TimeUnit tunit) throws InterruptedException, ConnectionPoolTimeoutException {
                return ConnPoolByRoute.this.getEntryBlocking(route, state, timeout, tunit, aborter);
            }
        };
    }

    protected BasicPoolEntry getEntryBlocking(HttpRoute route, Object state, long timeout, TimeUnit tunit, WaitingThreadAborter aborter) throws ConnectionPoolTimeoutException, InterruptedException {
        Date deadline;
        Throwable th;
        HttpRoute httpRoute = route;
        Object obj = state;
        long j = timeout;
        if (j > 0) {
            deadline = new Date(System.currentTimeMillis() + tunit.toMillis(j));
        } else {
            TimeUnit timeUnit = tunit;
            deadline = null;
        }
        BasicPoolEntry entry = null;
        this.poolLock.lock();
        WaitingThreadAborter waitingThreadAborter;
        try {
            RouteSpecificPool rospl = getRoutePool(httpRoute, true);
            WaitingThread waitingThread = null;
            while (entry == null) {
                if (this.isShutDown) {
                    waitingThreadAborter = aborter;
                    throw new IllegalStateException("Connection pool shut down.");
                }
                if (this.log.isDebugEnabled()) {
                    Log log = this.log;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Total connections kept alive: ");
                    stringBuilder.append(this.freeConnections.size());
                    log.debug(stringBuilder.toString());
                    log = this.log;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Total issued connections: ");
                    stringBuilder.append(this.issuedConnections.size());
                    log.debug(stringBuilder.toString());
                    log = this.log;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Total allocated connection: ");
                    stringBuilder.append(this.numConnections);
                    stringBuilder.append(" out of ");
                    stringBuilder.append(this.maxTotalConnections);
                    log.debug(stringBuilder.toString());
                }
                entry = getFreeEntry(rospl, obj);
                if (entry != null) {
                    break;
                }
                Log log2;
                StringBuilder stringBuilder2;
                boolean hasCapacity = rospl.getCapacity() > 0;
                if (this.log.isDebugEnabled()) {
                    log2 = this.log;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Available capacity: ");
                    stringBuilder2.append(rospl.getCapacity());
                    stringBuilder2.append(" out of ");
                    stringBuilder2.append(rospl.getMaxEntries());
                    stringBuilder2.append(" [");
                    stringBuilder2.append(httpRoute);
                    stringBuilder2.append("][");
                    stringBuilder2.append(obj);
                    stringBuilder2.append("]");
                    log2.debug(stringBuilder2.toString());
                }
                if (hasCapacity && this.numConnections < this.maxTotalConnections) {
                    entry = createEntry(rospl, this.operator);
                } else if (!hasCapacity || this.freeConnections.isEmpty()) {
                    if (this.log.isDebugEnabled()) {
                        log2 = this.log;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Need to wait for connection [");
                        stringBuilder2.append(httpRoute);
                        stringBuilder2.append("][");
                        stringBuilder2.append(obj);
                        stringBuilder2.append("]");
                        log2.debug(stringBuilder2.toString());
                    }
                    if (waitingThread == null) {
                        waitingThread = newWaitingThread(this.poolLock.newCondition(), rospl);
                        try {
                            aborter.setWaitingThread(waitingThread);
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } else {
                        waitingThreadAborter = aborter;
                    }
                    rospl.queueThread(waitingThread);
                    this.waitingThreads.add(waitingThread);
                    boolean success = waitingThread.await(deadline);
                    rospl.removeThread(waitingThread);
                    this.waitingThreads.remove(waitingThread);
                    if (!(success || deadline == null)) {
                        if (deadline.getTime() <= System.currentTimeMillis()) {
                            throw new ConnectionPoolTimeoutException("Timeout waiting for connection");
                        }
                    }
                } else {
                    deleteLeastUsedEntry();
                    entry = createEntry(rospl, this.operator);
                }
                waitingThreadAborter = aborter;
            }
            waitingThreadAborter = aborter;
            this.poolLock.unlock();
            return entry;
        } catch (Throwable th3) {
            th = th3;
            waitingThreadAborter = aborter;
            this.poolLock.unlock();
            throw th;
        }
    }

    public void freeEntry(BasicPoolEntry entry, boolean reusable, long validDuration, TimeUnit timeUnit) {
        HttpRoute route = entry.getPlannedRoute();
        if (this.log.isDebugEnabled()) {
            Log log = this.log;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Freeing connection [");
            stringBuilder.append(route);
            stringBuilder.append("][");
            stringBuilder.append(entry.getState());
            stringBuilder.append("]");
            log.debug(stringBuilder.toString());
        }
        this.poolLock.lock();
        try {
            if (this.isShutDown) {
                closeConnection(entry.getConnection());
                return;
            }
            this.issuedConnections.remove(entry.getWeakRef());
            RouteSpecificPool rospl = getRoutePool(route, true);
            if (reusable) {
                rospl.freeEntry(entry);
                this.freeConnections.add(entry);
                this.idleConnHandler.add(entry.getConnection(), validDuration, timeUnit);
            } else {
                rospl.dropEntry();
                this.numConnections--;
            }
            notifyWaitingThread(rospl);
            this.poolLock.unlock();
        } finally {
            this.poolLock.unlock();
        }
    }

    protected BasicPoolEntry getFreeEntry(RouteSpecificPool rospl, Object state) {
        BasicPoolEntry entry = null;
        this.poolLock.lock();
        boolean done = false;
        while (!done) {
            try {
                entry = rospl.allocEntry(state);
                Log log;
                StringBuilder stringBuilder;
                if (entry != null) {
                    if (this.log.isDebugEnabled()) {
                        log = this.log;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Getting free connection [");
                        stringBuilder.append(rospl.getRoute());
                        stringBuilder.append("][");
                        stringBuilder.append(state);
                        stringBuilder.append("]");
                        log.debug(stringBuilder.toString());
                    }
                    this.freeConnections.remove(entry);
                    if (this.idleConnHandler.remove(entry.getConnection())) {
                        this.issuedConnections.add(entry.getWeakRef());
                        done = true;
                    } else {
                        if (this.log.isDebugEnabled()) {
                            Log log2 = this.log;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Closing expired free connection [");
                            stringBuilder2.append(rospl.getRoute());
                            stringBuilder2.append("][");
                            stringBuilder2.append(state);
                            stringBuilder2.append("]");
                            log2.debug(stringBuilder2.toString());
                        }
                        closeConnection(entry.getConnection());
                        rospl.dropEntry();
                        this.numConnections--;
                    }
                } else {
                    done = true;
                    if (this.log.isDebugEnabled()) {
                        log = this.log;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("No free connections [");
                        stringBuilder.append(rospl.getRoute());
                        stringBuilder.append("][");
                        stringBuilder.append(state);
                        stringBuilder.append("]");
                        log.debug(stringBuilder.toString());
                    }
                }
            } catch (Throwable th) {
                this.poolLock.unlock();
            }
        }
        this.poolLock.unlock();
        return entry;
    }

    protected BasicPoolEntry createEntry(RouteSpecificPool rospl, ClientConnectionOperator op) {
        if (this.log.isDebugEnabled()) {
            Log log = this.log;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Creating new connection [");
            stringBuilder.append(rospl.getRoute());
            stringBuilder.append("]");
            log.debug(stringBuilder.toString());
        }
        BasicPoolEntry entry = new BasicPoolEntry(op, rospl.getRoute(), this.refQueue);
        this.poolLock.lock();
        try {
            rospl.createdEntry(entry);
            this.numConnections++;
            this.issuedConnections.add(entry.getWeakRef());
            return entry;
        } finally {
            this.poolLock.unlock();
        }
    }

    protected void deleteEntry(BasicPoolEntry entry) {
        HttpRoute route = entry.getPlannedRoute();
        if (this.log.isDebugEnabled()) {
            Log log = this.log;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Deleting connection [");
            stringBuilder.append(route);
            stringBuilder.append("][");
            stringBuilder.append(entry.getState());
            stringBuilder.append("]");
            log.debug(stringBuilder.toString());
        }
        this.poolLock.lock();
        try {
            closeConnection(entry.getConnection());
            RouteSpecificPool rospl = getRoutePool(route, true);
            rospl.deleteEntry(entry);
            this.numConnections--;
            if (rospl.isUnused()) {
                this.routeToPool.remove(route);
            }
            this.idleConnHandler.remove(entry.getConnection());
        } finally {
            this.poolLock.unlock();
        }
    }

    protected void deleteLeastUsedEntry() {
        try {
            this.poolLock.lock();
            BasicPoolEntry entry = (BasicPoolEntry) this.freeConnections.remove();
            if (entry != null) {
                deleteEntry(entry);
            } else if (this.log.isDebugEnabled()) {
                this.log.debug("No free connection to delete.");
            }
            this.poolLock.unlock();
        } catch (Throwable th) {
            this.poolLock.unlock();
        }
    }

    protected void handleLostEntry(HttpRoute route) {
        this.poolLock.lock();
        try {
            RouteSpecificPool rospl = getRoutePool(route, true);
            rospl.dropEntry();
            if (rospl.isUnused()) {
                this.routeToPool.remove(route);
            }
            this.numConnections--;
            notifyWaitingThread(rospl);
        } finally {
            this.poolLock.unlock();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x006f A:{Catch:{ all -> 0x003b }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void notifyWaitingThread(RouteSpecificPool rospl) {
        WaitingThread waitingThread = null;
        this.poolLock.lock();
        if (rospl != null) {
            try {
                if (rospl.hasThread()) {
                    if (this.log.isDebugEnabled()) {
                        Log log = this.log;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Notifying thread waiting on pool [");
                        stringBuilder.append(rospl.getRoute());
                        stringBuilder.append("]");
                        log.debug(stringBuilder.toString());
                    }
                    waitingThread = rospl.nextThread();
                    if (waitingThread != null) {
                        waitingThread.wakeup();
                    }
                    this.poolLock.unlock();
                }
            } catch (Throwable th) {
                this.poolLock.unlock();
            }
        }
        if (!this.waitingThreads.isEmpty()) {
            if (this.log.isDebugEnabled()) {
                this.log.debug("Notifying thread waiting on any pool");
            }
            waitingThread = (WaitingThread) this.waitingThreads.remove();
        } else if (this.log.isDebugEnabled()) {
            this.log.debug("Notifying no-one, there are no waiting threads");
        }
        if (waitingThread != null) {
        }
        this.poolLock.unlock();
    }

    public void deleteClosedConnections() {
        this.poolLock.lock();
        try {
            Iterator<BasicPoolEntry> iter = this.freeConnections.iterator();
            while (iter.hasNext()) {
                BasicPoolEntry entry = (BasicPoolEntry) iter.next();
                if (!entry.getConnection().isOpen()) {
                    iter.remove();
                    deleteEntry(entry);
                }
            }
        } finally {
            this.poolLock.unlock();
        }
    }

    public void shutdown() {
        this.poolLock.lock();
        try {
            super.shutdown();
            Iterator<BasicPoolEntry> ibpe = this.freeConnections.iterator();
            while (ibpe.hasNext()) {
                BasicPoolEntry entry = (BasicPoolEntry) ibpe.next();
                ibpe.remove();
                closeConnection(entry.getConnection());
            }
            Iterator<WaitingThread> iwth = this.waitingThreads.iterator();
            while (iwth.hasNext()) {
                WaitingThread waiter = (WaitingThread) iwth.next();
                iwth.remove();
                waiter.wakeup();
            }
            this.routeToPool.clear();
        } finally {
            this.poolLock.unlock();
        }
    }
}
