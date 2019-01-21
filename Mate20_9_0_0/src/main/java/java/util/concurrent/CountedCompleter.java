package java.util.concurrent;

import sun.misc.Unsafe;

public abstract class CountedCompleter<T> extends ForkJoinTask<T> {
    private static final long PENDING;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 5232453752276485070L;
    final CountedCompleter<?> completer;
    volatile int pending;

    public abstract void compute();

    protected CountedCompleter(CountedCompleter<?> completer, int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    protected CountedCompleter(CountedCompleter<?> completer) {
        this.completer = completer;
    }

    protected CountedCompleter() {
        this.completer = null;
    }

    public void onCompletion(CountedCompleter<?> countedCompleter) {
    }

    public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> countedCompleter) {
        return true;
    }

    public final CountedCompleter<?> getCompleter() {
        return this.completer;
    }

    public final int getPendingCount() {
        return this.pending;
    }

    public final void setPendingCount(int count) {
        this.pending = count;
    }

    public final void addToPendingCount(int delta) {
        U.getAndAddInt(this, PENDING, delta);
    }

    public final boolean compareAndSetPendingCount(int expected, int count) {
        return U.compareAndSwapInt(this, PENDING, expected, count);
    }

    public final int decrementPendingCountUnlessZero() {
        int c;
        do {
            int i = this.pending;
            c = i;
            if (i == 0) {
                break;
            }
        } while (!U.compareAndSwapInt(this, PENDING, c, c - 1));
        return c;
    }

    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> a = this;
        while (true) {
            CountedCompleter<?> countedCompleter = a.completer;
            CountedCompleter<?> p = countedCompleter;
            if (countedCompleter == null) {
                return a;
            }
            a = p;
        }
    }

    public final void tryComplete() {
        CountedCompleter<?> s = this;
        CountedCompleter<?> a = s;
        while (true) {
            int i = a.pending;
            int c = i;
            if (i == 0) {
                a.onCompletion(s);
                s = a;
                CountedCompleter<?> countedCompleter = a.completer;
                a = countedCompleter;
                if (countedCompleter == null) {
                    s.quietlyComplete();
                    return;
                }
            } else {
                if (U.compareAndSwapInt(a, PENDING, c, c - 1)) {
                    return;
                }
            }
        }
    }

    public final void propagateCompletion() {
        CountedCompleter<?> a = this;
        while (true) {
            int i = a.pending;
            int c = i;
            if (i == 0) {
                CountedCompleter<?> s = a;
                CountedCompleter<?> countedCompleter = a.completer;
                a = countedCompleter;
                if (countedCompleter == null) {
                    s.quietlyComplete();
                    return;
                }
            } else {
                if (U.compareAndSwapInt(a, PENDING, c, c - 1)) {
                    return;
                }
            }
        }
    }

    public void complete(T rawResult) {
        setRawResult(rawResult);
        onCompletion(this);
        quietlyComplete();
        CountedCompleter<?> countedCompleter = this.completer;
        CountedCompleter<?> p = countedCompleter;
        if (countedCompleter != null) {
            p.tryComplete();
        }
    }

    public final CountedCompleter<?> firstComplete() {
        int c;
        do {
            int i = this.pending;
            c = i;
            if (i == 0) {
                return this;
            }
        } while (!U.compareAndSwapInt(this, PENDING, c, c - 1));
        return null;
    }

    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> countedCompleter = this.completer;
        CountedCompleter<?> p = countedCompleter;
        if (countedCompleter != null) {
            return p.firstComplete();
        }
        quietlyComplete();
        return null;
    }

    public final void quietlyCompleteRoot() {
        CountedCompleter<?> a = this;
        while (true) {
            CountedCompleter<?> countedCompleter = a.completer;
            CountedCompleter<?> p = countedCompleter;
            if (countedCompleter == null) {
                a.quietlyComplete();
                return;
            }
            a = p;
        }
    }

    public final void helpComplete(int maxTasks) {
        if (maxTasks > 0 && this.status >= 0) {
            Thread currentThread = Thread.currentThread();
            Thread t = currentThread;
            if (currentThread instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
                forkJoinWorkerThread.pool.helpComplete(forkJoinWorkerThread.workQueue, this, maxTasks);
                return;
            }
            ForkJoinPool.common.externalHelpComplete(this, maxTasks);
        }
    }

    void internalPropagateException(Throwable ex) {
        CountedCompleter<?> s = this;
        CountedCompleter<?> a = s;
        while (a.onExceptionalCompletion(ex, s)) {
            s = a;
            CountedCompleter<?> countedCompleter = a.completer;
            a = countedCompleter;
            if (countedCompleter == null || a.status < 0 || a.recordExceptionalCompletion(ex) != Integer.MIN_VALUE) {
                return;
            }
        }
    }

    protected final boolean exec() {
        compute();
        return false;
    }

    public T getRawResult() {
        return null;
    }

    protected void setRawResult(T t) {
    }

    static {
        try {
            PENDING = U.objectFieldOffset(CountedCompleter.class.getDeclaredField("pending"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
