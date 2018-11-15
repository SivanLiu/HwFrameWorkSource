package org.apache.http.impl.conn.tsccm;

import android.net.TrafficStats;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.params.HttpParams;

@Deprecated
public class ThreadSafeClientConnManager implements ClientConnectionManager {
    protected ClientConnectionOperator connOperator;
    protected final AbstractConnPool connectionPool;
    private final Log log = LogFactory.getLog(getClass());
    protected SchemeRegistry schemeRegistry;

    public ThreadSafeClientConnManager(HttpParams params, SchemeRegistry schreg) {
        if (params != null) {
            this.schemeRegistry = schreg;
            this.connOperator = createConnectionOperator(schreg);
            this.connectionPool = createConnectionPool(params);
            return;
        }
        throw new IllegalArgumentException("HTTP parameters may not be null");
    }

    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    protected AbstractConnPool createConnectionPool(HttpParams params) {
        AbstractConnPool acp = new ConnPoolByRoute(this.connOperator, params);
        if (true) {
            acp.enableConnectionGC();
        }
        return acp;
    }

    protected ClientConnectionOperator createConnectionOperator(SchemeRegistry schreg) {
        return new DefaultClientConnectionOperator(schreg);
    }

    public SchemeRegistry getSchemeRegistry() {
        return this.schemeRegistry;
    }

    public ClientConnectionRequest requestConnection(final HttpRoute route, Object state) {
        final PoolEntryRequest poolRequest = this.connectionPool.requestPoolEntry(route, state);
        return new ClientConnectionRequest() {
            public void abortRequest() {
                poolRequest.abortRequest();
            }

            public ManagedClientConnection getConnection(long timeout, TimeUnit tunit) throws InterruptedException, ConnectionPoolTimeoutException {
                if (route != null) {
                    if (ThreadSafeClientConnManager.this.log.isDebugEnabled()) {
                        Log access$000 = ThreadSafeClientConnManager.this.log;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ThreadSafeClientConnManager.getConnection: ");
                        stringBuilder.append(route);
                        stringBuilder.append(", timeout = ");
                        stringBuilder.append(timeout);
                        access$000.debug(stringBuilder.toString());
                    }
                    BasicPoolEntry entry = poolRequest.getPoolEntry(timeout, tunit);
                    try {
                        Socket socket = entry.getConnection().getSocket();
                        if (socket != null) {
                            TrafficStats.tagSocket(socket);
                        }
                    } catch (IOException iox) {
                        ThreadSafeClientConnManager.this.log.debug("Problem tagging socket.", iox);
                    }
                    return new BasicPooledConnAdapter(ThreadSafeClientConnManager.this, entry);
                }
                throw new IllegalArgumentException("Route may not be null.");
            }
        };
    }

    public void releaseConnection(ManagedClientConnection conn, long validDuration, TimeUnit timeUnit) {
        if (conn instanceof BasicPooledConnAdapter) {
            BasicPooledConnAdapter hca = (BasicPooledConnAdapter) conn;
            if (hca.getPoolEntry() == null || hca.getManager() == this) {
                BasicPoolEntry entry;
                boolean reusable;
                try {
                    Socket socket = ((BasicPoolEntry) hca.getPoolEntry()).getConnection().getSocket();
                    if (socket != null) {
                        TrafficStats.untagSocket(socket);
                    }
                    if (hca.isOpen() && !hca.isMarkedReusable()) {
                        if (this.log.isDebugEnabled()) {
                            this.log.debug("Released connection open but not marked reusable.");
                        }
                        hca.shutdown();
                    }
                    entry = (BasicPoolEntry) hca.getPoolEntry();
                    reusable = hca.isMarkedReusable();
                    hca.detach();
                    if (entry == null) {
                        return;
                    }
                } catch (IOException iox) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Exception shutting down released connection.", iox);
                    }
                    entry = (BasicPoolEntry) hca.getPoolEntry();
                    reusable = hca.isMarkedReusable();
                    hca.detach();
                    if (entry == null) {
                        return;
                    }
                } catch (Throwable th) {
                    BasicPoolEntry entry2 = (BasicPoolEntry) hca.getPoolEntry();
                    boolean reusable2 = hca.isMarkedReusable();
                    hca.detach();
                    if (entry2 != null) {
                        this.connectionPool.freeEntry(entry2, reusable2, validDuration, timeUnit);
                    }
                }
                this.connectionPool.freeEntry(entry, reusable, validDuration, timeUnit);
                return;
            }
            throw new IllegalArgumentException("Connection not obtained from this manager.");
        }
        throw new IllegalArgumentException("Connection class mismatch, connection not obtained from this manager.");
    }

    public void shutdown() {
        this.connectionPool.shutdown();
    }

    public int getConnectionsInPool(HttpRoute route) {
        return ((ConnPoolByRoute) this.connectionPool).getConnectionsInPool(route);
    }

    public int getConnectionsInPool() {
        int i;
        synchronized (this.connectionPool) {
            i = this.connectionPool.numConnections;
        }
        return i;
    }

    public void closeIdleConnections(long idleTimeout, TimeUnit tunit) {
        this.connectionPool.closeIdleConnections(idleTimeout, tunit);
        this.connectionPool.deleteClosedConnections();
    }

    public void closeExpiredConnections() {
        this.connectionPool.closeExpiredConnections();
        this.connectionPool.deleteClosedConnections();
    }
}
