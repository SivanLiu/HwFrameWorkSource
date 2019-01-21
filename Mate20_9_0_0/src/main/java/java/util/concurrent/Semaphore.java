package java.util.concurrent;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Semaphore implements Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    private final Sync sync;

    static abstract class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            int remaining;
            while (true) {
                int available = getState();
                remaining = available - acquires;
                if (remaining < 0 || compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
            return remaining;
        }

        protected final boolean tryReleaseShared(int releases) {
            while (true) {
                int current = getState();
                int next = current + releases;
                if (next < current) {
                    throw new Error("Maximum permit count exceeded");
                } else if (compareAndSetState(current, next)) {
                    return true;
                }
            }
        }

        final void reducePermits(int reductions) {
            while (true) {
                int current = getState();
                int next = current - reductions;
                if (next > current) {
                    throw new Error("Permit count underflow");
                } else if (compareAndSetState(current, next)) {
                    return;
                }
            }
        }

        final int drainPermits() {
            int current;
            while (true) {
                current = getState();
                if (current == 0 || compareAndSetState(current, 0)) {
                    return current;
                }
            }
            return current;
        }
    }

    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            while (!hasQueuedPredecessors()) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 || compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
            return -1;
        }
    }

    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    public Semaphore(int permits) {
        this.sync = new NonfairSync(permits);
    }

    public Semaphore(int permits, boolean fair) {
        this.sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    public void acquire() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    public void acquireUninterruptibly() {
        this.sync.acquireShared(1);
    }

    public boolean tryAcquire() {
        return this.sync.nonfairTryAcquireShared(1) >= 0;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void release() {
        this.sync.releaseShared(1);
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits >= 0) {
            this.sync.acquireSharedInterruptibly(permits);
            return;
        }
        throw new IllegalArgumentException();
    }

    public void acquireUninterruptibly(int permits) {
        if (permits >= 0) {
            this.sync.acquireShared(permits);
            return;
        }
        throw new IllegalArgumentException();
    }

    public boolean tryAcquire(int permits) {
        if (permits >= 0) {
            return this.sync.nonfairTryAcquireShared(permits) >= 0;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits >= 0) {
            return this.sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
        }
        throw new IllegalArgumentException();
    }

    public void release(int permits) {
        if (permits >= 0) {
            this.sync.releaseShared(permits);
            return;
        }
        throw new IllegalArgumentException();
    }

    public int availablePermits() {
        return this.sync.getPermits();
    }

    public int drainPermits() {
        return this.sync.drainPermits();
    }

    protected void reducePermits(int reduction) {
        if (reduction >= 0) {
            this.sync.reducePermits(reduction);
            return;
        }
        throw new IllegalArgumentException();
    }

    public boolean isFair() {
        return this.sync instanceof FairSync;
    }

    public final boolean hasQueuedThreads() {
        return this.sync.hasQueuedThreads();
    }

    public final int getQueueLength() {
        return this.sync.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return this.sync.getQueuedThreads();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("[Permits = ");
        stringBuilder.append(this.sync.getPermits());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
