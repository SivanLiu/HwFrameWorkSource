package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public abstract class ForkJoinTask<V> implements Future<V>, Serializable {
    static final int CANCELLED = -1073741824;
    static final int DONE_MASK = -268435456;
    static final int EXCEPTIONAL = Integer.MIN_VALUE;
    private static final int EXCEPTION_MAP_CAPACITY = 32;
    static final int NORMAL = -268435456;
    static final int SIGNAL = 65536;
    static final int SMASK = 65535;
    private static final long STATUS;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final ExceptionNode[] exceptionTable = new ExceptionNode[32];
    private static final ReentrantLock exceptionTableLock = new ReentrantLock();
    private static final ReferenceQueue<Object> exceptionTableRefQueue = new ReferenceQueue();
    private static final long serialVersionUID = -7721805057305804111L;
    volatile int status;

    static final class AdaptedCallable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 2838392045355241008L;
        final Callable<? extends T> callable;
        T result;

        AdaptedCallable(Callable<? extends T> callable) {
            if (callable != null) {
                this.callable = callable;
                return;
            }
            throw new NullPointerException();
        }

        public final T getRawResult() {
            return this.result;
        }

        public final void setRawResult(T v) {
            this.result = v;
        }

        public final boolean exec() {
            try {
                this.result = this.callable.call();
                return true;
            } catch (RuntimeException rex) {
                throw rex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public final void run() {
            invoke();
        }
    }

    static final class AdaptedRunnable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 5232453952276885070L;
        T result;
        final Runnable runnable;

        AdaptedRunnable(Runnable runnable, T result) {
            if (runnable != null) {
                this.runnable = runnable;
                this.result = result;
                return;
            }
            throw new NullPointerException();
        }

        public final T getRawResult() {
            return this.result;
        }

        public final void setRawResult(T v) {
            this.result = v;
        }

        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        public final void run() {
            invoke();
        }
    }

    static final class AdaptedRunnableAction extends ForkJoinTask<Void> implements RunnableFuture<Void> {
        private static final long serialVersionUID = 5232453952276885070L;
        final Runnable runnable;

        AdaptedRunnableAction(Runnable runnable) {
            if (runnable != null) {
                this.runnable = runnable;
                return;
            }
            throw new NullPointerException();
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }

        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        public final void run() {
            invoke();
        }
    }

    static final class ExceptionNode extends WeakReference<ForkJoinTask<?>> {
        final Throwable ex;
        final int hashCode;
        ExceptionNode next;
        final long thrower = Thread.currentThread().getId();

        ExceptionNode(ForkJoinTask<?> task, Throwable ex, ExceptionNode next, ReferenceQueue<Object> exceptionTableRefQueue) {
            super(task, exceptionTableRefQueue);
            this.ex = ex;
            this.next = next;
            this.hashCode = System.identityHashCode(task);
        }
    }

    static final class RunnableExecuteAction extends ForkJoinTask<Void> {
        private static final long serialVersionUID = 5232453952276885070L;
        final Runnable runnable;

        RunnableExecuteAction(Runnable runnable) {
            if (runnable != null) {
                this.runnable = runnable;
                return;
            }
            throw new NullPointerException();
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }

        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        void internalPropagateException(Throwable ex) {
            ForkJoinTask.rethrow(ex);
        }
    }

    protected abstract boolean exec();

    public abstract V getRawResult();

    protected abstract void setRawResult(V v);

    private int setCompletion(int completion) {
        int s;
        do {
            int i = this.status;
            s = i;
            if (i < 0) {
                return s;
            }
        } while (!U.compareAndSwapInt(this, STATUS, s, s | completion));
        if ((s >>> 16) != 0) {
            synchronized (this) {
                notifyAll();
            }
        }
        return completion;
    }

    final int doExec() {
        int i = this.status;
        int s = i;
        if (i >= 0) {
            try {
                if (exec()) {
                    s = setCompletion(-268435456);
                }
            } catch (Throwable rex) {
                return setExceptionalCompletion(rex);
            }
        }
        return s;
    }

    final void internalWait(long timeout) {
        int i = this.status;
        int s = i;
        if (i >= 0) {
            if (U.compareAndSwapInt(this, STATUS, s, s | 65536)) {
                synchronized (this) {
                    if (this.status >= 0) {
                        try {
                            wait(timeout);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        notifyAll();
                    }
                }
            }
        }
    }

    private int externalAwaitDone() {
        boolean interrupted = false;
        int s = this instanceof CountedCompleter ? ForkJoinPool.common.externalHelpComplete((CountedCompleter) this, 0) : ForkJoinPool.common.tryExternalUnpush(this) ? doExec() : 0;
        if (s >= 0) {
            int i = this.status;
            s = i;
            if (i >= 0) {
                do {
                    if (U.compareAndSwapInt(this, STATUS, s, s | 65536)) {
                        synchronized (this) {
                            if (this.status >= 0) {
                                try {
                                    wait(STATUS);
                                } catch (InterruptedException e) {
                                    interrupted = true;
                                }
                            } else {
                                notifyAll();
                            }
                        }
                    }
                    i = this.status;
                    s = i;
                } while (i >= 0);
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return s;
    }

    private int externalInterruptibleAwaitDone() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        int i = this.status;
        int s = i;
        if (i >= 0) {
            int i2 = 0;
            if (this instanceof CountedCompleter) {
                i2 = ForkJoinPool.common.externalHelpComplete((CountedCompleter) this, 0);
            } else if (ForkJoinPool.common.tryExternalUnpush(this)) {
                i2 = doExec();
            }
            s = i2;
            if (i2 >= 0) {
                while (true) {
                    i = this.status;
                    s = i;
                    if (i < 0) {
                        break;
                    }
                    if (U.compareAndSwapInt(this, STATUS, s, s | 65536)) {
                        synchronized (this) {
                            if (this.status >= 0) {
                                wait(STATUS);
                            } else {
                                notifyAll();
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    /* JADX WARNING: Missing block: B:8:0x0022, code skipped:
            if (r0 < 0) goto L_0x0005;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int doJoin() {
        int i = this.status;
        int s = i;
        if (i >= 0) {
            Thread currentThread = Thread.currentThread();
            Thread t = currentThread;
            if (!(currentThread instanceof ForkJoinWorkerThread)) {
                return externalAwaitDone();
            }
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
            ForkJoinWorkerThread wt = forkJoinWorkerThread;
            WorkQueue workQueue = forkJoinWorkerThread.workQueue;
            WorkQueue w = workQueue;
            if (workQueue.tryUnpush(this)) {
                i = doExec();
                s = i;
            }
            return wt.pool.awaitJoin(w, this, STATUS);
        }
        return s;
    }

    private int doInvoke() {
        int doExec = doExec();
        int s = doExec;
        if (doExec < 0) {
            return s;
        }
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (!(currentThread instanceof ForkJoinWorkerThread)) {
            return externalAwaitDone();
        }
        ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
        return forkJoinWorkerThread.pool.awaitJoin(forkJoinWorkerThread.workQueue, this, STATUS);
    }

    final int recordExceptionalCompletion(Throwable ex) {
        int i = this.status;
        int s = i;
        if (i < 0) {
            return s;
        }
        i = System.identityHashCode(this);
        ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            expungeStaleExceptions();
            ExceptionNode[] t = exceptionTable;
            int i2 = (t.length - 1) & i;
            for (ExceptionNode e = t[i2]; e != null; e = e.next) {
                if (e.get() == this) {
                    break;
                }
            }
            t[i2] = new ExceptionNode(this, ex, t[i2], exceptionTableRefQueue);
            lock.unlock();
            return setCompletion(Integer.MIN_VALUE);
        } catch (Throwable th) {
            lock.unlock();
        }
    }

    private int setExceptionalCompletion(Throwable ex) {
        int s = recordExceptionalCompletion(ex);
        if ((-268435456 & s) == Integer.MIN_VALUE) {
            internalPropagateException(ex);
        }
        return s;
    }

    void internalPropagateException(Throwable ex) {
    }

    static final void cancelIgnoringExceptions(ForkJoinTask<?> t) {
        if (t != null && t.status >= 0) {
            try {
                t.cancel(false);
            } catch (Throwable th) {
            }
        }
    }

    private void clearExceptionalCompletion() {
        int h = System.identityHashCode(this);
        ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            ExceptionNode[] t = exceptionTable;
            int i = (t.length - 1) & h;
            ExceptionNode e = t[i];
            ExceptionNode pred = null;
            while (e != null) {
                ExceptionNode next = e.next;
                if (e.get() == this) {
                    if (pred == null) {
                        t[i] = next;
                    } else {
                        pred.next = next;
                    }
                    expungeStaleExceptions();
                    this.status = 0;
                }
                pred = e;
                e = next;
            }
            expungeStaleExceptions();
            this.status = 0;
        } finally {
            lock.unlock();
        }
    }

    private Throwable getThrowableException() {
        int h = System.identityHashCode(this);
        ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            expungeStaleExceptions();
            ExceptionNode[] t = exceptionTable;
            ExceptionNode e = t[(t.length - 1) & h];
            while (e != null && e.get() != this) {
                e = e.next;
            }
            lock.unlock();
            if (e != null) {
                Throwable th = e.ex;
                Throwable ex = th;
                if (th != null) {
                    if (e.thrower != Thread.currentThread().getId()) {
                        try {
                            Constructor<?> noArgCtor = null;
                            for (Constructor<?> c : ex.getClass().getConstructors()) {
                                Class<?>[] ps = c.getParameterTypes();
                                if (ps.length == 0) {
                                    noArgCtor = c;
                                } else if (ps.length == 1 && ps[0] == Throwable.class) {
                                    return (Throwable) c.newInstance(ex);
                                }
                            }
                            if (noArgCtor != null) {
                                th = (Throwable) noArgCtor.newInstance(new Object[0]);
                                th.initCause(ex);
                                return th;
                            }
                        } catch (Exception e2) {
                        }
                    }
                    return ex;
                }
            }
            return null;
        } catch (Throwable th2) {
            lock.unlock();
        }
    }

    private static void expungeStaleExceptions() {
        while (true) {
            Reference poll = exceptionTableRefQueue.poll();
            Reference x = poll;
            if (poll == null) {
                return;
            }
            if (x instanceof ExceptionNode) {
                int hashCode = ((ExceptionNode) x).hashCode;
                ExceptionNode[] t = exceptionTable;
                int i = (t.length - 1) & hashCode;
                Reference e = t[i];
                ExceptionNode pred = null;
                while (e != null) {
                    Reference next = e.next;
                    if (e != x) {
                        Reference pred2 = e;
                        e = next;
                    } else if (pred2 == null) {
                        t[i] = next;
                    } else {
                        pred2.next = next;
                    }
                }
            }
        }
    }

    static final void helpExpungeStaleExceptions() {
        ReentrantLock lock = exceptionTableLock;
        if (lock.tryLock()) {
            try {
                expungeStaleExceptions();
            } finally {
                lock.unlock();
            }
        }
    }

    static void rethrow(Throwable ex) {
        uncheckedThrow(ex);
    }

    static <T extends Throwable> void uncheckedThrow(Throwable t) throws Throwable {
        if (t != null) {
            throw t;
        }
        throw new Error("Unknown Exception");
    }

    private void reportException(int s) {
        if (s == CANCELLED) {
            throw new CancellationException();
        } else if (s == Integer.MIN_VALUE) {
            rethrow(getThrowableException());
        }
    }

    public final ForkJoinTask<V> fork() {
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (currentThread instanceof ForkJoinWorkerThread) {
            ((ForkJoinWorkerThread) t).workQueue.push(this);
        } else {
            ForkJoinPool.common.externalPush(this);
        }
        return this;
    }

    public final V join() {
        int doJoin = doJoin() & -268435456;
        int s = doJoin;
        if (doJoin != -268435456) {
            reportException(s);
        }
        return getRawResult();
    }

    public final V invoke() {
        int doInvoke = doInvoke() & -268435456;
        int s = doInvoke;
        if (doInvoke != -268435456) {
            reportException(s);
        }
        return getRawResult();
    }

    public static void invokeAll(ForkJoinTask<?> t1, ForkJoinTask<?> t2) {
        t2.fork();
        int doInvoke = t1.doInvoke() & -268435456;
        int s1 = doInvoke;
        if (doInvoke != -268435456) {
            t1.reportException(s1);
        }
        doInvoke = t2.doJoin() & -268435456;
        int s2 = doInvoke;
        if (doInvoke != -268435456) {
            t2.reportException(s2);
        }
    }

    public static void invokeAll(ForkJoinTask<?>... tasks) {
        int i;
        int i2 = 1;
        int last = tasks.length - 1;
        Throwable ex = null;
        for (i = last; i >= 0; i--) {
            ForkJoinTask<?> t = tasks[i];
            if (t == null) {
                if (ex == null) {
                    ex = new NullPointerException();
                }
            } else if (i != 0) {
                t.fork();
            } else if (t.doInvoke() < -268435456 && ex == null) {
                ex = t.getException();
            }
        }
        while (true) {
            i = i2;
            if (i > last) {
                break;
            }
            Throwable ex2 = tasks[i];
            if (ex2 != null) {
                if (ex != null) {
                    ex2.cancel(false);
                } else if (ex2.doJoin() < -268435456) {
                    ex = ex2.getException();
                }
            }
            i2 = i + 1;
        }
        if (ex != null) {
            rethrow(ex);
        }
    }

    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        if ((tasks instanceof RandomAccess) && (tasks instanceof List)) {
            int i;
            List<? extends ForkJoinTask<?>> ts = (List) tasks;
            int i2 = 1;
            int last = ts.size() - 1;
            Throwable ex = null;
            for (i = last; i >= 0; i--) {
                ForkJoinTask<?> t = (ForkJoinTask) ts.get(i);
                if (t == null) {
                    if (ex == null) {
                        ex = new NullPointerException();
                    }
                } else if (i != 0) {
                    t.fork();
                } else if (t.doInvoke() < -268435456 && ex == null) {
                    ex = t.getException();
                }
            }
            while (true) {
                i = i2;
                if (i > last) {
                    break;
                }
                ForkJoinTask ex2 = (ForkJoinTask) ts.get(i);
                if (ex2 != null) {
                    if (ex != null) {
                        ex2.cancel(false);
                    } else if (ex2.doJoin() < -268435456) {
                        ex = ex2.getException();
                    }
                }
                i2 = i + 1;
            }
            if (ex != null) {
                rethrow(ex);
            }
            return tasks;
        }
        invokeAll((ForkJoinTask[]) tasks.toArray(new ForkJoinTask[tasks.size()]));
        return tasks;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return (setCompletion(CANCELLED) & -268435456) == CANCELLED;
    }

    public final boolean isDone() {
        return this.status < 0;
    }

    public final boolean isCancelled() {
        return (this.status & -268435456) == CANCELLED;
    }

    public final boolean isCompletedAbnormally() {
        return this.status < -268435456;
    }

    public final boolean isCompletedNormally() {
        return (this.status & -268435456) == -268435456;
    }

    public final Throwable getException() {
        int s = this.status & -268435456;
        if (s >= -268435456) {
            return null;
        }
        if (s == CANCELLED) {
            return new CancellationException();
        }
        return getThrowableException();
    }

    public void completeExceptionally(Throwable ex) {
        Throwable th;
        if ((ex instanceof RuntimeException) || (ex instanceof Error)) {
            th = ex;
        } else {
            th = new RuntimeException(ex);
        }
        setExceptionalCompletion(th);
    }

    public void complete(V value) {
        try {
            setRawResult(value);
            setCompletion(-268435456);
        } catch (Throwable rex) {
            setExceptionalCompletion(rex);
        }
    }

    public final void quietlyComplete() {
        setCompletion(-268435456);
    }

    public final V get() throws InterruptedException, ExecutionException {
        int doJoin = -268435456 & (Thread.currentThread() instanceof ForkJoinWorkerThread ? doJoin() : externalInterruptibleAwaitDone());
        int s = doJoin;
        if (doJoin == CANCELLED) {
            throw new CancellationException();
        } else if (s != Integer.MIN_VALUE) {
            return getRawResult();
        } else {
            throw new ExecutionException(getThrowableException());
        }
    }

    public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Throwable th;
        long nanos = unit.toNanos(timeout);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        int i;
        int i2 = this.status;
        int s = i2;
        if (i2 >= 0 && nanos > STATUS) {
            long d = System.nanoTime() + nanos;
            long deadline = d == STATUS ? 1 : d;
            Thread t = Thread.currentThread();
            if (t instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
                s = wt.pool.awaitJoin(wt.workQueue, this, deadline);
            } else {
                i = 0;
                if (this instanceof CountedCompleter) {
                    i = ForkJoinPool.common.externalHelpComplete((CountedCompleter) this, 0);
                } else if (ForkJoinPool.common.tryExternalUnpush(this)) {
                    i = doExec();
                }
                s = i;
                if (i >= 0) {
                    int s2;
                    while (true) {
                        i2 = this.status;
                        s2 = i2;
                        if (i2 < 0) {
                            break;
                        }
                        long nanoTime = deadline - System.nanoTime();
                        long ns = nanoTime;
                        if (nanoTime <= STATUS) {
                            break;
                        }
                        Thread t2;
                        long ns2 = ns;
                        ns = TimeUnit.NANOSECONDS.toMillis(ns2);
                        long ms = ns;
                        if (ns > STATUS) {
                            t2 = t;
                            ns = deadline;
                            if (U.compareAndSwapInt(this, STATUS, s2, s2 | 65536)) {
                                synchronized (this) {
                                    try {
                                        if (this.status >= 0) {
                                            wait(ms);
                                        } else {
                                            notifyAll();
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                }
                            }
                        } else {
                            t2 = t;
                            ns = deadline;
                        }
                        s = s2;
                        deadline = ns;
                        t = t2;
                    }
                    s = s2;
                }
            }
        }
        if (s >= 0) {
            s = this.status;
        }
        i = s & -268435456;
        s = i;
        if (i == -268435456) {
            return getRawResult();
        }
        if (s == CANCELLED) {
            throw new CancellationException();
        } else if (s != Integer.MIN_VALUE) {
            throw new TimeoutException();
        } else {
            throw new ExecutionException(getThrowableException());
        }
    }

    public final void quietlyJoin() {
        doJoin();
    }

    public final void quietlyInvoke() {
        doInvoke();
    }

    public static void helpQuiesce() {
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (currentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
            wt.pool.helpQuiescePool(wt.workQueue);
            return;
        }
        ForkJoinPool.quiesceCommonPool();
    }

    public void reinitialize() {
        if ((this.status & -268435456) == Integer.MIN_VALUE) {
            clearExceptionalCompletion();
        } else {
            this.status = 0;
        }
    }

    public static ForkJoinPool getPool() {
        Thread t = Thread.currentThread();
        return t instanceof ForkJoinWorkerThread ? ((ForkJoinWorkerThread) t).pool : null;
    }

    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread;
    }

    public boolean tryUnfork() {
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (currentThread instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) t).workQueue.tryUnpush(this);
        }
        return ForkJoinPool.common.tryExternalUnpush(this);
    }

    public static int getQueuedTaskCount() {
        WorkQueue q;
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (currentThread instanceof ForkJoinWorkerThread) {
            q = ((ForkJoinWorkerThread) t).workQueue;
        } else {
            q = ForkJoinPool.commonSubmitterQueue();
        }
        return q == null ? 0 : q.queueSize();
    }

    public static int getSurplusQueuedTaskCount() {
        return ForkJoinPool.getSurplusQueuedTaskCount();
    }

    protected static ForkJoinTask<?> peekNextLocalTask() {
        WorkQueue q;
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (currentThread instanceof ForkJoinWorkerThread) {
            q = ((ForkJoinWorkerThread) t).workQueue;
        } else {
            q = ForkJoinPool.commonSubmitterQueue();
        }
        return q == null ? null : q.peek();
    }

    protected static ForkJoinTask<?> pollNextLocalTask() {
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (currentThread instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) t).workQueue.nextLocalTask();
        }
        return null;
    }

    protected static ForkJoinTask<?> pollTask() {
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        if (!(currentThread instanceof ForkJoinWorkerThread)) {
            return null;
        }
        ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
        return forkJoinWorkerThread.pool.nextTaskFor(forkJoinWorkerThread.workQueue);
    }

    protected static ForkJoinTask<?> pollSubmission() {
        Thread currentThread = Thread.currentThread();
        return currentThread instanceof ForkJoinWorkerThread ? ((ForkJoinWorkerThread) currentThread).pool.pollSubmission() : null;
    }

    public final short getForkJoinTaskTag() {
        return (short) this.status;
    }

    public final short setForkJoinTaskTag(short newValue) {
        int s;
        Unsafe unsafe;
        long j;
        int i;
        do {
            unsafe = U;
            j = STATUS;
            i = this.status;
            s = i;
        } while (!unsafe.compareAndSwapInt(this, j, i, (SMASK & newValue) | (-65536 & s)));
        return (short) s;
    }

    public final boolean compareAndSetForkJoinTaskTag(short expect, short update) {
        int s;
        do {
            int i = this.status;
            s = i;
            if (((short) i) != expect) {
                return false;
            }
        } while (!U.compareAndSwapInt(this, STATUS, s, (-65536 & s) | (SMASK & update)));
        return true;
    }

    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnableAction(runnable);
    }

    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable(runnable, result);
    }

    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable(callable);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(getException());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        Object ex = s.readObject();
        if (ex != null) {
            setExceptionalCompletion((Throwable) ex);
        }
    }

    static {
        try {
            STATUS = U.objectFieldOffset(ForkJoinTask.class.getDeclaredField("status"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
