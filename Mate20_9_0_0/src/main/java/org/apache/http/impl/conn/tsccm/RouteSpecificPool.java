package org.apache.http.impl.conn.tsccm;

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.util.LangUtils;

@Deprecated
public class RouteSpecificPool {
    protected final LinkedList<BasicPoolEntry> freeEntries;
    private final Log log = LogFactory.getLog(getClass());
    protected final int maxEntries;
    protected int numEntries;
    protected final HttpRoute route;
    protected final Queue<WaitingThread> waitingThreads;

    public RouteSpecificPool(HttpRoute route, int maxEntries) {
        this.route = route;
        this.maxEntries = maxEntries;
        this.freeEntries = new LinkedList();
        this.waitingThreads = new LinkedList();
        this.numEntries = 0;
    }

    public final HttpRoute getRoute() {
        return this.route;
    }

    public final int getMaxEntries() {
        return this.maxEntries;
    }

    public boolean isUnused() {
        return this.numEntries < 1 && this.waitingThreads.isEmpty();
    }

    public int getCapacity() {
        return this.maxEntries - this.numEntries;
    }

    public final int getEntryCount() {
        return this.numEntries;
    }

    public BasicPoolEntry allocEntry(Object state) {
        if (!this.freeEntries.isEmpty()) {
            ListIterator<BasicPoolEntry> it = this.freeEntries.listIterator(this.freeEntries.size());
            while (it.hasPrevious()) {
                BasicPoolEntry entry = (BasicPoolEntry) it.previous();
                if (LangUtils.equals(state, entry.getState())) {
                    it.remove();
                    return entry;
                }
            }
        }
        if (this.freeEntries.isEmpty()) {
            return null;
        }
        BasicPoolEntry entry2 = (BasicPoolEntry) this.freeEntries.remove();
        entry2.setState(null);
        try {
            entry2.getConnection().close();
        } catch (IOException ex) {
            this.log.debug("I/O error closing connection", ex);
        }
        return entry2;
    }

    public void freeEntry(BasicPoolEntry entry) {
        StringBuilder stringBuilder;
        if (this.numEntries < 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No entry created for this pool. ");
            stringBuilder.append(this.route);
            throw new IllegalStateException(stringBuilder.toString());
        } else if (this.numEntries > this.freeEntries.size()) {
            this.freeEntries.add(entry);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No entry allocated from this pool. ");
            stringBuilder.append(this.route);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public void createdEntry(BasicPoolEntry entry) {
        if (this.route.equals(entry.getPlannedRoute())) {
            this.numEntries++;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Entry not planned for this pool.\npool: ");
        stringBuilder.append(this.route);
        stringBuilder.append("\nplan: ");
        stringBuilder.append(entry.getPlannedRoute());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean deleteEntry(BasicPoolEntry entry) {
        boolean found = this.freeEntries.remove(entry);
        if (found) {
            this.numEntries--;
        }
        return found;
    }

    public void dropEntry() {
        if (this.numEntries >= 1) {
            this.numEntries--;
            return;
        }
        throw new IllegalStateException("There is no entry that could be dropped.");
    }

    public void queueThread(WaitingThread wt) {
        if (wt != null) {
            this.waitingThreads.add(wt);
            return;
        }
        throw new IllegalArgumentException("Waiting thread must not be null.");
    }

    public boolean hasThread() {
        return this.waitingThreads.isEmpty() ^ 1;
    }

    public WaitingThread nextThread() {
        return (WaitingThread) this.waitingThreads.peek();
    }

    public void removeThread(WaitingThread wt) {
        if (wt != null) {
            this.waitingThreads.remove(wt);
        }
    }
}
