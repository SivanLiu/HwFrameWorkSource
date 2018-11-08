package java.util.concurrent;

import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public class ForkJoinPool extends AbstractExecutorService {
    private static final int ABASE;
    private static final long AC_MASK = -281474976710656L;
    private static final int AC_SHIFT = 48;
    private static final long AC_UNIT = 281474976710656L;
    private static final long ADD_WORKER = 140737488355328L;
    private static final int ASHIFT;
    private static final int COMMON_MAX_SPARES;
    static final int COMMON_PARALLELISM = Math.max(common.config & SMASK, 1);
    private static final long CTL;
    private static final int DEFAULT_COMMON_MAX_SPARES = 256;
    static final int EVENMASK = 65534;
    static final int FIFO_QUEUE = Integer.MIN_VALUE;
    private static final long IDLE_TIMEOUT_MS = 2000;
    static final int IS_OWNED = 1;
    static final int LIFO_QUEUE = 0;
    static final int MAX_CAP = 32767;
    static final int MODE_MASK = -65536;
    static final int POLL_LIMIT = 1023;
    private static final long RUNSTATE;
    private static final int SEED_INCREMENT = -1640531527;
    private static final int SHUTDOWN = Integer.MIN_VALUE;
    static final int SMASK = 65535;
    static final int SPARE_WORKER = 131072;
    private static final long SP_MASK = 4294967295L;
    static final int SQMASK = 126;
    static final int SS_SEQ = 65536;
    private static final int STARTED = 1;
    private static final int STOP = 2;
    private static final long TC_MASK = 281470681743360L;
    private static final int TC_SHIFT = 32;
    private static final long TC_UNIT = 4294967296L;
    private static final int TERMINATED = 4;
    private static final long TIMEOUT_SLOP_MS = 20;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long UC_MASK = -4294967296L;
    static final int UNREGISTERED = 262144;
    static final int UNSIGNALLED = Integer.MIN_VALUE;
    static final ForkJoinPool common = ((ForkJoinPool) AccessController.doPrivileged(new PrivilegedAction<ForkJoinPool>() {
        public ForkJoinPool run() {
            return ForkJoinPool.makeCommonPool();
        }
    }));
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
    static final RuntimePermission modifyThreadPermission = new RuntimePermission("modifyThread");
    private static int poolNumberSequence;
    AuxState auxState;
    final int config;
    volatile long ctl;
    final ForkJoinWorkerThreadFactory factory;
    volatile int runState;
    final UncaughtExceptionHandler ueh;
    volatile WorkQueue[] workQueues;
    final String workerNamePrefix;

    public interface ManagedBlocker {
        boolean block() throws InterruptedException;

        boolean isReleasable();
    }

    private static final class AuxState extends ReentrantLock {
        private static final long serialVersionUID = -6001602636862214147L;
        long indexSeed;
        volatile long stealCount;

        AuxState() {
        }
    }

    public interface ForkJoinWorkerThreadFactory {
        ForkJoinWorkerThread newThread(ForkJoinPool forkJoinPool);
    }

    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private DefaultForkJoinWorkerThreadFactory() {
        }

        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
        }
    }

    private static final class EmptyTask extends ForkJoinTask<Void> {
        private static final long serialVersionUID = -7721805057305804111L;

        EmptyTask() {
            this.status = -268435456;
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void x) {
        }

        public final boolean exec() {
            return true;
        }
    }

    private static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private static final AccessControlContext innocuousAcc;

        private InnocuousForkJoinWorkerThreadFactory() {
        }

        static {
            Permissions innocuousPerms = new Permissions();
            innocuousPerms.add(ForkJoinPool.modifyThreadPermission);
            innocuousPerms.add(new RuntimePermission("enableContextClassLoaderOverride"));
            innocuousPerms.add(new RuntimePermission("modifyThreadGroup"));
            innocuousAcc = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, innocuousPerms)});
        }

        public final ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
            return (ForkJoinWorkerThread) AccessController.doPrivileged(new PrivilegedAction<ForkJoinWorkerThread>() {
                public ForkJoinWorkerThread run() {
                    return new InnocuousForkJoinWorkerThread(pool);
                }
            }, innocuousAcc);
        }
    }

    static final class WorkQueue {
        private static final int ABASE;
        private static final int ASHIFT;
        static final int INITIAL_QUEUE_CAPACITY = 8192;
        static final int MAXIMUM_QUEUE_CAPACITY = 67108864;
        private static final long QLOCK;
        private static final Unsafe U = Unsafe.getUnsafe();
        ForkJoinTask<?>[] array;
        volatile int base = 4096;
        int config;
        volatile ForkJoinTask<?> currentJoin;
        volatile ForkJoinTask<?> currentSteal;
        int hint;
        int nsteals;
        final ForkJoinWorkerThread owner;
        volatile Thread parker;
        final ForkJoinPool pool;
        volatile int qlock;
        volatile int scanState;
        int stackPred;
        int top = 4096;

        WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner) {
            this.pool = pool;
            this.owner = owner;
        }

        final int getPoolIndex() {
            return (this.config & ForkJoinPool.SMASK) >>> 1;
        }

        final int queueSize() {
            int n = this.base - this.top;
            if (n >= 0) {
                return 0;
            }
            return -n;
        }

        final boolean isEmpty() {
            int i = this.base;
            int s = this.top;
            int n = i - s;
            if (n >= 0) {
                return true;
            }
            if (n != -1) {
                return false;
            }
            ForkJoinTask<?>[] a = this.array;
            if (a == null) {
                return true;
            }
            int al = a.length;
            if (al == 0 || a[(al - 1) & (s - 1)] == null) {
                return true;
            }
            return false;
        }

        final void push(ForkJoinTask<?> task) {
            U.storeFence();
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null) {
                int al = a.length;
                if (al > 0) {
                    a[(al - 1) & s] = task;
                    this.top = s + 1;
                    ForkJoinPool p = this.pool;
                    int d = this.base - s;
                    if (d == 0 && p != null) {
                        U.fullFence();
                        p.signalWork();
                    } else if (al + d == 1) {
                        growArray();
                    }
                }
            }
        }

        final ForkJoinTask<?>[] growArray() {
            ForkJoinTask<?>[] oldA = this.array;
            int size = oldA != null ? oldA.length << 1 : 8192;
            if (size < 8192 || size > MAXIMUM_QUEUE_CAPACITY) {
                throw new RejectedExecutionException("Queue capacity exceeded");
            }
            ForkJoinTask<?>[] a = new ForkJoinTask[size];
            this.array = a;
            if (oldA != null) {
                int oldMask = oldA.length - 1;
                if (oldMask > 0) {
                    int t = this.top;
                    int b = this.base;
                    if (t - b > 0) {
                        int mask = size - 1;
                        do {
                            long offset = (((long) (b & oldMask)) << ASHIFT) + ((long) ABASE);
                            ForkJoinTask<?> x = (ForkJoinTask) U.getObjectVolatile(oldA, offset);
                            if (x != null && U.compareAndSwapObject(oldA, offset, x, null)) {
                                a[b & mask] = x;
                            }
                            b++;
                        } while (b != t);
                        U.storeFence();
                    }
                }
            }
            return a;
        }

        final ForkJoinTask<?> pop() {
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (!(a == null || b == s)) {
                int al = a.length;
                if (al > 0) {
                    s--;
                    long offset = (((long) ((al - 1) & s)) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> t = (ForkJoinTask) U.getObject(a, offset);
                    if (t != null && U.compareAndSwapObject(a, offset, t, null)) {
                        this.top = s;
                        return t;
                    }
                }
            }
            return null;
        }

        final ForkJoinTask<?> pollAt(int b) {
            ForkJoinTask<?>[] a = this.array;
            if (a != null) {
                int al = a.length;
                if (al > 0) {
                    long offset = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (t != null) {
                        int b2 = b + 1;
                        if (b != this.base) {
                            b = b2;
                        } else if (U.compareAndSwapObject(a, offset, t, null)) {
                            this.base = b2;
                            return t;
                        }
                    }
                }
            }
            return null;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final ForkJoinTask<?> poll() {
            while (true) {
                int i = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a == null) {
                    break;
                }
                int d = i - s;
                if (d >= 0) {
                    break;
                }
                int al = a.length;
                if (al <= 0) {
                    break;
                }
                long offset = (((long) ((al - 1) & i)) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                int b = i + 1;
                if (i == this.base) {
                    if (t != null) {
                        if (U.compareAndSwapObject(a, offset, t, null)) {
                            this.base = b;
                            return t;
                        }
                    } else if (d == -1) {
                        break;
                    }
                }
            }
            return null;
        }

        final ForkJoinTask<?> nextLocalTask() {
            return this.config < 0 ? poll() : pop();
        }

        final ForkJoinTask<?> peek() {
            ForkJoinTask<?>[] a = this.array;
            if (a == null) {
                return null;
            }
            int al = a.length;
            if (al <= 0) {
                return null;
            }
            return a[(this.config < 0 ? this.base : this.top - 1) & (al - 1)];
        }

        final boolean tryUnpush(ForkJoinTask<?> task) {
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (!(a == null || b == s)) {
                int al = a.length;
                if (al > 0) {
                    s--;
                    if (U.compareAndSwapObject(a, (((long) ((al - 1) & s)) << ASHIFT) + ((long) ABASE), task, null)) {
                        this.top = s;
                        return true;
                    }
                }
            }
            return false;
        }

        final int sharedPush(ForkJoinTask<?> task) {
            if (!U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                return 1;
            }
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null) {
                int al = a.length;
                if (al > 0) {
                    int d = b - s;
                    if ((al - 1) + d > 0) {
                        a[(al - 1) & s] = task;
                        this.top = s + 1;
                        this.qlock = 0;
                        return (d >= 0 || b != this.base) ? 0 : d;
                    }
                }
            }
            growAndSharedPush(task);
            return 0;
        }

        private void growAndSharedPush(ForkJoinTask<?> task) {
            try {
                growArray();
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a != null) {
                    int al = a.length;
                    if (al > 0) {
                        a[(al - 1) & s] = task;
                        this.top = s + 1;
                    }
                }
                this.qlock = 0;
            } catch (Throwable th) {
                this.qlock = 0;
            }
        }

        final boolean trySharedUnpush(ForkJoinTask<?> task) {
            boolean popped = false;
            int s = this.top - 1;
            Object a = this.array;
            if (a != null) {
                int al = a.length;
                if (al > 0) {
                    long offset = (((long) ((al - 1) & s)) << ASHIFT) + ((long) ABASE);
                    if (((ForkJoinTask) U.getObject(a, offset)) == task && U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                        if (this.top == s + 1 && this.array == a && U.compareAndSwapObject(a, offset, task, null)) {
                            popped = true;
                            this.top = s;
                        }
                        U.putOrderedInt(this, QLOCK, 0);
                    }
                }
            }
            return popped;
        }

        final void cancelAll() {
            ForkJoinTask<?> t = this.currentJoin;
            if (t != null) {
                this.currentJoin = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            t = this.currentSteal;
            if (t != null) {
                this.currentSteal = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            while (true) {
                t = poll();
                if (t != null) {
                    ForkJoinTask.cancelIgnoringExceptions(t);
                } else {
                    return;
                }
            }
        }

        final void localPopAndExec() {
            int nexec = 0;
            do {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a != null && b != s) {
                    int al = a.length;
                    if (al > 0) {
                        s--;
                        ForkJoinTask<?> t = (ForkJoinTask) U.getAndSetObject(a, (((long) ((al - 1) & s)) << ASHIFT) + ((long) ABASE), null);
                        if (t != null) {
                            this.top = s;
                            this.currentSteal = t;
                            t.doExec();
                            nexec++;
                        } else {
                            return;
                        }
                    }
                    return;
                }
                return;
            } while (nexec <= 1023);
        }

        final void localPollAndExec() {
            int nexec = 0;
            while (true) {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a != null && b != s) {
                    int al = a.length;
                    if (al > 0) {
                        int b2 = b + 1;
                        ForkJoinTask<?> t = (ForkJoinTask) U.getAndSetObject(a, (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE), null);
                        if (t != null) {
                            this.base = b2;
                            t.doExec();
                            nexec++;
                            if (nexec > 1023) {
                                b = b2;
                                return;
                            }
                        }
                    } else {
                        return;
                    }
                }
                return;
            }
        }

        final void runTask(ForkJoinTask<?> task) {
            if (task != null) {
                task.doExec();
                if (this.config < 0) {
                    localPollAndExec();
                } else {
                    localPopAndExec();
                }
                int ns = this.nsteals + 1;
                this.nsteals = ns;
                ForkJoinWorkerThread thread = this.owner;
                this.currentSteal = null;
                if (ns < 0) {
                    transferStealCount(this.pool);
                }
                if (thread != null) {
                    thread.afterTopLevelExec();
                }
            }
        }

        final void transferStealCount(ForkJoinPool p) {
            if (p != null) {
                AuxState aux = p.auxState;
                if (aux != null) {
                    long s = (long) this.nsteals;
                    this.nsteals = 0;
                    if (s < 0) {
                        s = 2147483647L;
                    }
                    aux.lock();
                    try {
                        aux.stealCount += s;
                    } finally {
                        aux.unlock();
                    }
                }
            }
        }

        final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
            if (task != null && task.status >= 0) {
                do {
                    int b = this.base;
                    int s = this.top;
                    int d = b - s;
                    if (d < 0) {
                        ForkJoinTask<?>[] a = this.array;
                        if (a != null) {
                            int al = a.length;
                            if (al > 0) {
                                do {
                                    s--;
                                    long offset = (long) (((s & (al - 1)) << ASHIFT) + ABASE);
                                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                                    if (t == null) {
                                        break;
                                    } else if (t == task) {
                                        boolean removed = false;
                                        if (s + 1 == this.top) {
                                            if (U.compareAndSwapObject(a, offset, t, null)) {
                                                this.top = s;
                                                removed = true;
                                            }
                                        } else if (this.base == b) {
                                            removed = U.compareAndSwapObject(a, offset, t, new EmptyTask());
                                        }
                                        if (removed) {
                                            ForkJoinTask<?> ps = this.currentSteal;
                                            this.currentSteal = task;
                                            task.doExec();
                                            this.currentSteal = ps;
                                        }
                                    } else if (t.status >= 0 || s + 1 != this.top) {
                                        d++;
                                    } else if (U.compareAndSwapObject(a, offset, t, null)) {
                                        this.top = s;
                                    }
                                } while (d != 0);
                                if (this.base == b) {
                                    return false;
                                }
                            }
                        }
                    }
                } while (task.status >= 0);
                return false;
            }
            return true;
        }

        final CountedCompleter<?> popCC(CountedCompleter<?> task, int mode) {
            int b = this.base;
            int s = this.top;
            Object a = this.array;
            if (a != null && b != s) {
                int al = a.length;
                if (al > 0) {
                    long offset = (((long) ((al - 1) & (s - 1))) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> o = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (o instanceof CountedCompleter) {
                        CountedCompleter<?> t = (CountedCompleter) o;
                        CountedCompleter<?> r = t;
                        while (r != task) {
                            r = r.completer;
                            if (r == null) {
                                break;
                            }
                        }
                        if ((mode & 1) == 0) {
                            boolean popped = false;
                            if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                                if (this.top == s && this.array == a && U.compareAndSwapObject(a, offset, t, null)) {
                                    popped = true;
                                    this.top = s - 1;
                                }
                                U.putOrderedInt(this, QLOCK, 0);
                                if (popped) {
                                    return t;
                                }
                            }
                        } else if (U.compareAndSwapObject(a, offset, t, null)) {
                            this.top = s - 1;
                            return t;
                        }
                    }
                }
            }
            return null;
        }

        final int pollAndExecCC(CountedCompleter<?> task) {
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (!(a == null || b == s)) {
                int al = a.length;
                if (al > 0) {
                    long offset = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> o = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (o == null) {
                        return 2;
                    }
                    if (!(o instanceof CountedCompleter)) {
                        return -1;
                    }
                    int h;
                    CountedCompleter<?> t = (CountedCompleter) o;
                    CountedCompleter<?> r = t;
                    while (r != task) {
                        r = r.completer;
                        if (r == null) {
                            return -1;
                        }
                    }
                    int b2 = b + 1;
                    if (b == this.base && U.compareAndSwapObject(a, offset, t, null)) {
                        this.base = b2;
                        t.doExec();
                        h = 1;
                    } else {
                        h = 2;
                    }
                    b = b2;
                    return h;
                }
            }
            return b | Integer.MIN_VALUE;
        }

        final boolean isApparentlyUnblocked() {
            if (this.scanState < 0) {
                return false;
            }
            Thread wt = this.owner;
            if (wt == null) {
                return false;
            }
            State s = wt.getState();
            if (s == State.BLOCKED || s == State.WAITING || s == State.TIMED_WAITING) {
                return false;
            }
            return true;
        }

        static {
            try {
                QLOCK = U.objectFieldOffset(WorkQueue.class.getDeclaredField("qlock"));
                ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
                int scale = U.arrayIndexScale(ForkJoinTask[].class);
                if (((scale - 1) & scale) != 0) {
                    throw new Error("array index scale not a power of two");
                }
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            } catch (Throwable e) {
                throw new Error(e);
            }
        }
    }

    final int helpComplete(java.util.concurrent.ForkJoinPool.WorkQueue r1, java.util.concurrent.CountedCompleter<?> r2, int r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: java.util.concurrent.ForkJoinPool.helpComplete(java.util.concurrent.ForkJoinPool$WorkQueue, java.util.concurrent.CountedCompleter, int):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.ForkJoinPool.helpComplete(java.util.concurrent.ForkJoinPool$WorkQueue, java.util.concurrent.CountedCompleter, int):int");
    }

    private static void checkPermission() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(modifyThreadPermission);
        }
    }

    private static final synchronized int nextPoolId() {
        int i;
        synchronized (ForkJoinPool.class) {
            i = poolNumberSequence + 1;
            poolNumberSequence = i;
        }
        return i;
    }

    private void tryInitialize(boolean checkTermination) {
        if (this.runState == 0) {
            int p = this.config & SMASK;
            int n = p > 1 ? p - 1 : 1;
            n |= n >>> 1;
            n |= n >>> 2;
            n |= n >>> 4;
            n |= n >>> 8;
            n = (((n | (n >>> 16)) + 1) << 1) & SMASK;
            AuxState aux = new AuxState();
            WorkQueue[] ws = new WorkQueue[n];
            synchronized (modifyThreadPermission) {
                if (this.runState == 0) {
                    this.workQueues = ws;
                    this.auxState = aux;
                    this.runState = 1;
                }
            }
        }
        if (checkTermination && this.runState < 0) {
            tryTerminate(false, false);
            throw new RejectedExecutionException();
        }
    }

    private boolean createWorker(boolean isSpare) {
        ForkJoinWorkerThreadFactory fac = this.factory;
        Throwable ex = null;
        ForkJoinWorkerThread forkJoinWorkerThread = null;
        if (fac != null) {
            try {
                forkJoinWorkerThread = fac.newThread(this);
                if (forkJoinWorkerThread != null) {
                    if (isSpare) {
                        WorkQueue q = forkJoinWorkerThread.workQueue;
                        if (q != null) {
                            q.config |= 131072;
                        }
                    }
                    forkJoinWorkerThread.start();
                    return true;
                }
            } catch (Throwable rex) {
                ex = rex;
            }
        }
        deregisterWorker(forkJoinWorkerThread, ex);
        return false;
    }

    private void tryAddWorker(long c) {
        while (true) {
            long nc = ((AC_UNIT + c) & AC_MASK) | ((TC_UNIT + c) & TC_MASK);
            if (this.ctl == c && U.compareAndSwapLong(this, CTL, c, nc)) {
                createWorker(false);
                return;
            }
            c = this.ctl;
            if ((ADD_WORKER & c) == 0 || ((int) c) != 0) {
                return;
            }
        }
    }

    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        wt.setDaemon(true);
        UncaughtExceptionHandler handler = this.ueh;
        if (handler != null) {
            wt.setUncaughtExceptionHandler(handler);
        }
        WorkQueue w = new WorkQueue(this, wt);
        int i = 0;
        int mode = this.config & MODE_MASK;
        AuxState aux = this.auxState;
        if (aux != null) {
            aux.lock();
            try {
                long j = aux.indexSeed - 1640531527;
                aux.indexSeed = j;
                int s = (int) j;
                WorkQueue[] ws = this.workQueues;
                if (ws != null) {
                    int n = ws.length;
                    if (n > 0) {
                        int m = n - 1;
                        i = m & ((s << 1) | 1);
                        if (ws[i] != null) {
                            int probes = 0;
                            int step = n <= 4 ? 2 : ((n >>> 1) & EVENMASK) + 2;
                            while (true) {
                                i = (i + step) & m;
                                Object[] ws2;
                                if (ws2[i] == null) {
                                    break;
                                }
                                probes++;
                                if (probes >= n) {
                                    n <<= 1;
                                    ws2 = (WorkQueue[]) Arrays.copyOf(ws2, n);
                                    this.workQueues = ws2;
                                    m = n - 1;
                                    probes = 0;
                                }
                            }
                        }
                        w.hint = s;
                        w.config = i | mode;
                        w.scanState = (2147418112 & s) | i;
                        ws[i] = w;
                    }
                }
                aux.unlock();
            } catch (Throwable th) {
                aux.unlock();
            }
        }
        wt.setName(this.workerNamePrefix.concat(Integer.toString(i >>> 1)));
        return w;
    }

    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        WorkQueue[] ws;
        WorkQueue workQueue = null;
        if (wt != null) {
            workQueue = wt.workQueue;
            if (workQueue != null) {
                int idx = workQueue.config & SMASK;
                int ns = workQueue.nsteals;
                AuxState aux = this.auxState;
                if (aux != null) {
                    aux.lock();
                    try {
                        ws = this.workQueues;
                        if (ws != null && ws.length > idx && ws[idx] == workQueue) {
                            ws[idx] = null;
                        }
                        aux.stealCount += (long) ns;
                    } finally {
                        aux.unlock();
                    }
                }
            }
        }
        long c;
        int wl;
        int sp;
        if (workQueue == null || (workQueue.config & 262144) == 0) {
            while (true) {
                Unsafe unsafe = U;
                long j = CTL;
                c = this.ctl;
                if (unsafe.compareAndSwapLong(this, j, c, (((c - AC_UNIT) & AC_MASK) | ((c - TC_UNIT) & TC_MASK)) | (SP_MASK & c))) {
                    break;
                }
            }
            if (workQueue != null) {
                workQueue.currentSteal = null;
                workQueue.qlock = -1;
                workQueue.cancelAll();
            }
            while (tryTerminate(false, false) >= 0 && workQueue != null && workQueue.array != null) {
                ws = this.workQueues;
                if (ws == null) {
                    break;
                }
                wl = ws.length;
                if (wl <= 0) {
                    break;
                }
                c = this.ctl;
                sp = (int) c;
                if (sp != 0) {
                    if (tryRelease(c, ws[(wl - 1) & sp], AC_UNIT)) {
                        break;
                    }
                } else if (ex != null && (ADD_WORKER & c) != 0) {
                    tryAddWorker(c);
                }
            }
            if (ex != null) {
                ForkJoinTask.helpExpungeStaleExceptions();
            } else {
                ForkJoinTask.rethrow(ex);
            }
        }
        if (workQueue != null) {
            workQueue.currentSteal = null;
            workQueue.qlock = -1;
            workQueue.cancelAll();
        }
        while (tryTerminate(false, false) >= 0) {
            ws = this.workQueues;
            if (ws == null) {
                wl = ws.length;
                if (wl <= 0) {
                    c = this.ctl;
                    sp = (int) c;
                    if (sp != 0) {
                        if (tryRelease(c, ws[(wl - 1) & sp], AC_UNIT)) {
                            break;
                        }
                    }
                    tryAddWorker(c);
                } else {
                    break;
                }
            }
            break;
        }
        if (ex != null) {
            ForkJoinTask.rethrow(ex);
        } else {
            ForkJoinTask.helpExpungeStaleExceptions();
        }
    }

    final void signalWork() {
        while (true) {
            long c = this.ctl;
            if (c < 0) {
                int sp = (int) c;
                if (sp == 0) {
                    break;
                }
                WorkQueue[] ws = this.workQueues;
                if (ws != null) {
                    int i = sp & SMASK;
                    if (ws.length > i) {
                        WorkQueue v = ws[i];
                        if (v != null) {
                            int ns = sp & Integer.MAX_VALUE;
                            long nc = (((long) v.stackPred) & SP_MASK) | ((AC_UNIT + c) & UC_MASK);
                            if (sp == v.scanState && U.compareAndSwapLong(this, CTL, c, nc)) {
                                v.scanState = ns;
                                LockSupport.unpark(v.parker);
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            return;
        }
        if ((ADD_WORKER & c) != 0) {
            tryAddWorker(c);
        }
    }

    private boolean tryRelease(long c, WorkQueue v, long inc) {
        int sp = (int) c;
        int ns = sp & Integer.MAX_VALUE;
        if (v != null) {
            long nc = (((long) v.stackPred) & SP_MASK) | ((c + inc) & UC_MASK);
            if (sp == v.scanState && U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = ns;
                LockSupport.unpark(v.parker);
                return true;
            }
        }
        return false;
    }

    private void tryReactivate(WorkQueue w, WorkQueue[] ws, int r) {
        long c = this.ctl;
        int sp = (int) c;
        if (sp != 0 && w != null && ws != null) {
            int wl = ws.length;
            if (wl > 0 && ((sp ^ r) & 65536) == 0) {
                WorkQueue v = ws[(wl - 1) & sp];
                if (v != null) {
                    long nc = (((long) v.stackPred) & SP_MASK) | ((AC_UNIT + c) & UC_MASK);
                    int ns = sp & Integer.MAX_VALUE;
                    if (w.scanState < 0 && v.scanState == sp && U.compareAndSwapLong(this, CTL, c, nc)) {
                        v.scanState = ns;
                        LockSupport.unpark(v.parker);
                    }
                }
            }
        }
    }

    private void inactivate(WorkQueue w, int ss) {
        int ns = (65536 + ss) | Integer.MIN_VALUE;
        long lc = ((long) ns) & SP_MASK;
        if (w != null) {
            w.scanState = ns;
            long c;
            long nc;
            do {
                c = this.ctl;
                nc = lc | ((c - AC_UNIT) & UC_MASK);
                w.stackPred = (int) c;
            } while (!U.compareAndSwapLong(this, CTL, c, nc));
        }
    }

    private int awaitWork(WorkQueue w) {
        if (w == null || w.scanState >= 0) {
            return 0;
        }
        long c = this.ctl;
        if (((int) (c >> AC_SHIFT)) + (this.config & SMASK) <= 0) {
            return timedAwaitWork(w, c);
        }
        if ((this.runState & 2) != 0) {
            w.qlock = -1;
            return -1;
        } else if (w.scanState >= 0) {
            return 0;
        } else {
            w.parker = Thread.currentThread();
            if (w.scanState < 0) {
                LockSupport.park(this);
            }
            w.parker = null;
            if ((this.runState & 2) != 0) {
                w.qlock = -1;
                return -1;
            } else if (w.scanState >= 0) {
                return 0;
            } else {
                Thread.interrupted();
                return 0;
            }
        }
    }

    private int timedAwaitWork(WorkQueue w, long c) {
        int stat = 0;
        int scale = 1 - ((short) ((int) (c >>> 32)));
        if (scale <= 0) {
            scale = 1;
        }
        long deadline = (((long) scale) * IDLE_TIMEOUT_MS) + System.currentTimeMillis();
        if (this.runState < 0) {
            stat = tryTerminate(false, false);
            if (stat <= 0) {
                return stat;
            }
        }
        if (w == null || w.scanState >= 0) {
            return stat;
        }
        w.parker = Thread.currentThread();
        if (w.scanState < 0) {
            LockSupport.parkUntil(this, deadline);
        }
        w.parker = null;
        if ((this.runState & 2) != 0) {
            w.qlock = -1;
            return -1;
        }
        int ss = w.scanState;
        if (ss >= 0 || (Thread.interrupted() ^ 1) == 0 || ((int) c) != ss) {
            return stat;
        }
        AuxState aux = this.auxState;
        if (aux == null || this.ctl != c || deadline - System.currentTimeMillis() > TIMEOUT_SLOP_MS) {
            return stat;
        }
        aux.lock();
        try {
            int cfg = w.config;
            int idx = cfg & SMASK;
            long nc = ((c - TC_UNIT) & UC_MASK) | (((long) w.stackPred) & SP_MASK);
            if ((this.runState & 2) == 0) {
                WorkQueue[] ws = this.workQueues;
                if (ws != null && idx < ws.length && idx >= 0 && ws[idx] == w && U.compareAndSwapLong(this, CTL, c, nc)) {
                    ws[idx] = null;
                    w.config = 262144 | cfg;
                    w.qlock = -1;
                    stat = -1;
                }
            }
            aux.unlock();
            return stat;
        } catch (Throwable th) {
            aux.unlock();
        }
    }

    private boolean tryDropSpare(WorkQueue w) {
        if (w != null && w.isEmpty()) {
            WorkQueue[] ws;
            boolean compareAndSwapLong;
            do {
                long c = this.ctl;
                if (((short) ((int) (c >> 32))) > (short) 0) {
                    int sp = (int) c;
                    if (sp != 0 || ((int) (c >> AC_SHIFT)) > 0) {
                        ws = this.workQueues;
                        if (ws != null) {
                            int wl = ws.length;
                            if (wl > 0) {
                                if (sp == 0) {
                                    compareAndSwapLong = U.compareAndSwapLong(this, CTL, c, (((c - AC_UNIT) & AC_MASK) | ((c - TC_UNIT) & TC_MASK)) | (SP_MASK & c));
                                    continue;
                                } else {
                                    WorkQueue v = ws[(wl - 1) & sp];
                                    if (v == null || v.scanState != sp) {
                                        compareAndSwapLong = false;
                                        continue;
                                    } else {
                                        boolean canDrop;
                                        long nc = ((long) v.stackPred) & SP_MASK;
                                        if (w == v || w.scanState >= 0) {
                                            canDrop = true;
                                            nc |= (AC_MASK & c) | ((c - TC_UNIT) & TC_MASK);
                                        } else {
                                            canDrop = false;
                                            nc |= ((AC_UNIT + c) & AC_MASK) | (TC_MASK & c);
                                        }
                                        if (U.compareAndSwapLong(this, CTL, c, nc)) {
                                            v.scanState = Integer.MAX_VALUE & sp;
                                            LockSupport.unpark(v.parker);
                                            compareAndSwapLong = canDrop;
                                            continue;
                                        } else {
                                            compareAndSwapLong = false;
                                            continue;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } while (!compareAndSwapLong);
            int cfg = w.config;
            int idx = cfg & SMASK;
            if (idx >= 0 && idx < ws.length && ws[idx] == w) {
                ws[idx] = null;
            }
            w.config = 262144 | cfg;
            w.qlock = -1;
            return true;
        }
        return false;
    }

    final void runWorker(WorkQueue w) {
        w.growArray();
        int bound = (w.config & 131072) != 0 ? 0 : 1023;
        long seed = ((long) w.hint) * -2685821657736338717L;
        if ((this.runState & 2) == 0) {
            long r = seed == 0 ? 1 : seed;
            while (true) {
                if (bound != 0 || !tryDropSpare(w)) {
                    int step = ((int) (r >>> AC_SHIFT)) | 1;
                    r ^= r >>> 12;
                    r ^= r << 25;
                    r ^= r >>> 27;
                    if (scan(w, bound, step, (int) r) < 0 && awaitWork(w) < 0) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
    }

    private int scan(WorkQueue w, int bound, int step, int r) {
        WorkQueue[] ws = this.workQueues;
        if (ws == null || w == null) {
            return 0;
        }
        int wl = ws.length;
        if (wl <= 0) {
            return 0;
        }
        int m = wl - 1;
        int origin = m & r;
        int idx = origin;
        int npolls = 0;
        int ss = w.scanState;
        while (true) {
            WorkQueue q = ws[idx];
            if (q != null) {
                int i = q.base;
                if (i - q.top < 0) {
                    ForkJoinTask<?>[] a = q.array;
                    if (a != null) {
                        int al = a.length;
                        if (al > 0) {
                            long offset = (((long) ((al - 1) & i)) << ASHIFT) + ((long) ABASE);
                            ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                            if (t == null) {
                                return 0;
                            }
                            int b = i + 1;
                            if (i != q.base) {
                                return 0;
                            }
                            if (ss < 0) {
                                tryReactivate(w, ws, r);
                                return 0;
                            } else if (!U.compareAndSwapObject(a, offset, t, null)) {
                                return 0;
                            } else {
                                q.base = b;
                                w.currentSteal = t;
                                if (b != q.top) {
                                    signalWork();
                                }
                                w.runTask(t);
                                npolls++;
                                if (npolls > bound) {
                                    return 0;
                                }
                            }
                        }
                    }
                }
            }
            if (npolls != 0) {
                return 0;
            }
            idx = (idx + step) & m;
            if (idx != origin) {
                continue;
            } else if (ss < 0) {
                return ss;
            } else {
                if (r >= 0) {
                    inactivate(w, ss);
                    return 0;
                }
                r <<= 1;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void helpStealer(WorkQueue w, ForkJoinTask<?> task) {
        if (task != null && w != null) {
            ForkJoinTask<?> ps = w.currentSteal;
            int oldSum = 0;
            while (w.tryRemoveAndExec(task) && task.status >= 0) {
                WorkQueue[] ws = this.workQueues;
                if (ws != null) {
                    int wl = ws.length;
                    if (wl > 0) {
                        int m = wl - 1;
                        int checkSum = 0;
                        WorkQueue j = w;
                        ForkJoinTask<?> subtask = task;
                        while (subtask.status >= 0) {
                            int i;
                            WorkQueue v;
                            int h = j.hint | 1;
                            int k = 0;
                            while (true) {
                                i = ((k << 1) + h) & m;
                                v = ws[i];
                                if (v != null) {
                                    if (v.currentSteal == subtask) {
                                        break;
                                    }
                                    checkSum += v.base;
                                }
                                k++;
                                if (k > m) {
                                    return;
                                }
                            }
                            j.hint = i;
                            while (subtask.status >= 0) {
                                int b = v.base;
                                checkSum += b;
                                ForkJoinTask<?> next = v.currentJoin;
                                ForkJoinTask<?> t = null;
                                ForkJoinTask<?>[] a = v.array;
                                if (a != null) {
                                    int al = a.length;
                                    if (al > 0) {
                                        long offset = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                                        t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                                        if (t != null) {
                                            int b2 = b + 1;
                                            if (b == v.base) {
                                                if (j.currentJoin != subtask || v.currentSteal != subtask || subtask.status < 0) {
                                                    break;
                                                } else if (U.compareAndSwapObject(a, offset, t, null)) {
                                                    v.base = b2;
                                                    w.currentSteal = t;
                                                    int top = w.top;
                                                    while (true) {
                                                        t.doExec();
                                                        w.currentSteal = ps;
                                                        if (task.status >= 0) {
                                                            if (w.top != top) {
                                                                t = w.pop();
                                                                if (t == null) {
                                                                    break;
                                                                }
                                                                w.currentSteal = t;
                                                            } else {
                                                                break;
                                                            }
                                                        }
                                                        return;
                                                    }
                                                } else {
                                                    b = b2;
                                                }
                                            } else {
                                                b = b2;
                                            }
                                        }
                                    }
                                }
                                if (t == null && b == v.base && b - v.top >= 0) {
                                    subtask = next;
                                    if (next != null) {
                                        j = v;
                                    } else if (next == v.currentJoin) {
                                        int oldSum2 = checkSum;
                                        if (oldSum != checkSum) {
                                            oldSum = oldSum2;
                                        } else {
                                            return;
                                        }
                                    } else {
                                        continue;
                                    }
                                }
                            }
                            continue;
                        }
                    } else {
                        return;
                    }
                }
                return;
            }
        }
    }

    private boolean tryCompensate(WorkQueue w) {
        long c = this.ctl;
        WorkQueue[] ws = this.workQueues;
        int pc = this.config & SMASK;
        int ac = pc + ((int) (c >> AC_SHIFT));
        int tc = pc + ((short) ((int) (c >> 32)));
        if (!(w == null || w.qlock < 0 || pc == 0 || ws == null)) {
            int wl = ws.length;
            if (wl > 0) {
                int m = wl - 1;
                boolean busy = true;
                for (int i = 0; i <= m; i++) {
                    int k = (i << 1) | 1;
                    if (k <= m && k >= 0) {
                        WorkQueue v = ws[k];
                        if (v != null && v.scanState >= 0 && v.currentSteal == null) {
                            busy = false;
                            break;
                        }
                    }
                }
                if (!busy || this.ctl != c) {
                    return false;
                }
                int sp = (int) c;
                if (sp != 0) {
                    return tryRelease(c, ws[m & sp], 0);
                } else if (tc >= pc && ac > 1 && w.isEmpty()) {
                    return U.compareAndSwapLong(this, CTL, c, ((c - AC_UNIT) & AC_MASK) | (281474976710655L & c));
                } else if (tc >= MAX_CAP || (this == common && tc >= COMMON_MAX_SPARES + pc)) {
                    throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
                } else {
                    boolean isSpare = tc >= pc;
                    if (U.compareAndSwapLong(this, CTL, c, (AC_MASK & c) | ((TC_UNIT + c) & TC_MASK))) {
                        return createWorker(isSpare);
                    }
                    return false;
                }
            }
        }
        return false;
    }

    final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
        int s = 0;
        if (w != null) {
            ForkJoinTask<?> prevJoin = w.currentJoin;
            if (task != null) {
                s = task.status;
                if (s >= 0) {
                    w.currentJoin = task;
                    CountedCompleter countedCompleter = task instanceof CountedCompleter ? (CountedCompleter) task : null;
                    do {
                        if (countedCompleter != null) {
                            helpComplete(w, countedCompleter, 0);
                        } else {
                            helpStealer(w, task);
                        }
                        s = task.status;
                        if (s < 0) {
                            break;
                        }
                        long ms;
                        if (deadline != 0) {
                            long ns = deadline - System.nanoTime();
                            if (ns <= 0) {
                                break;
                            }
                            ms = TimeUnit.NANOSECONDS.toMillis(ns);
                            if (ms <= 0) {
                                ms = 1;
                            }
                        } else {
                            ms = 0;
                        }
                        if (tryCompensate(w)) {
                            task.internalWait(ms);
                            U.getAndAddLong(this, CTL, AC_UNIT);
                        }
                        s = task.status;
                    } while (s >= 0);
                    w.currentJoin = prevJoin;
                }
            }
        }
        return s;
    }

    private WorkQueue findNonEmptyStealQueue() {
        int r = ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            int wl = ws.length;
            if (wl > 0) {
                int m = wl - 1;
                int origin = r & m;
                int k = origin;
                int oldSum = 0;
                int checkSum = 0;
                while (true) {
                    WorkQueue q = ws[k];
                    if (q != null) {
                        int b = q.base;
                        if (b - q.top < 0) {
                            return q;
                        }
                        checkSum += b;
                    }
                    k = (k + 1) & m;
                    if (k == origin) {
                        int oldSum2 = checkSum;
                        if (oldSum == checkSum) {
                            break;
                        }
                        checkSum = 0;
                        oldSum = oldSum2;
                    }
                }
            }
        }
        return null;
    }

    final void helpQuiescePool(WorkQueue w) {
        ForkJoinTask<?> ps = w.currentSteal;
        int wc = w.config;
        boolean active = true;
        while (true) {
            ForkJoinTask<?> t;
            if (wc >= 0) {
                t = w.pop();
                if (t != null) {
                    w.currentSteal = t;
                    t.doExec();
                    w.currentSteal = ps;
                }
            }
            WorkQueue q = findNonEmptyStealQueue();
            if (q != null) {
                if (!active) {
                    active = true;
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
                t = q.pollAt(q.base);
                if (t != null) {
                    w.currentSteal = t;
                    t.doExec();
                    w.currentSteal = ps;
                    int i = w.nsteals + 1;
                    w.nsteals = i;
                    if (i < 0) {
                        w.transferStealCount(this);
                    }
                }
            } else if (active) {
                c = this.ctl;
                if (U.compareAndSwapLong(this, CTL, c, ((c - AC_UNIT) & AC_MASK) | (281474976710655L & c))) {
                    active = false;
                }
            } else {
                c = this.ctl;
                if (((int) (c >> AC_SHIFT)) + (this.config & SMASK) <= 0) {
                    if (U.compareAndSwapLong(this, CTL, c, c + AC_UNIT)) {
                        return;
                    }
                } else {
                    continue;
                }
            }
        }
    }

    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        ForkJoinTask<?> t;
        do {
            t = w.nextLocalTask();
            if (t != null) {
                return t;
            }
            WorkQueue q = findNonEmptyStealQueue();
            if (q == null) {
                return null;
            }
            t = q.pollAt(q.base);
        } while (t == null);
        return t;
    }

    static int getSurplusQueuedTaskCount() {
        int i = 0;
        Thread t = Thread.currentThread();
        if (!(t instanceof ForkJoinWorkerThread)) {
            return 0;
        }
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
        ForkJoinPool pool = wt.pool;
        int p = pool.config & SMASK;
        WorkQueue q = wt.workQueue;
        int n = q.top - q.base;
        int a = ((int) (pool.ctl >> AC_SHIFT)) + p;
        p >>>= 1;
        if (a <= p) {
            p >>>= 1;
            if (a > p) {
                i = 1;
            } else {
                p >>>= 1;
                if (a > p) {
                    i = 2;
                } else if (a > (p >>> 1)) {
                    i = 4;
                } else {
                    i = 8;
                }
            }
        }
        return n - i;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int tryTerminate(boolean now, boolean enable) {
        while (true) {
            int rs = this.runState;
            if (rs < 0) {
                break;
            } else if (enable && this != common) {
                if (rs == 0) {
                    tryInitialize(false);
                } else {
                    U.compareAndSwapInt(this, RUNSTATE, rs, rs | Integer.MIN_VALUE);
                }
            }
        }
        return 1;
        int i++;
    }

    private void tryCreateExternalQueue(int index) {
        AuxState aux = this.auxState;
        if (aux != null && index >= 0) {
            WorkQueue q = new WorkQueue(this, null);
            q.config = index;
            q.scanState = Integer.MAX_VALUE;
            q.qlock = 1;
            boolean installed = false;
            aux.lock();
            try {
                WorkQueue[] ws = this.workQueues;
                if (ws != null && index < ws.length && ws[index] == null) {
                    ws[index] = q;
                    installed = true;
                }
                aux.unlock();
                if (installed) {
                    try {
                        q.growArray();
                    } finally {
                        q.qlock = 0;
                    }
                }
            } catch (Throwable th) {
                aux.unlock();
            }
        }
    }

    final void externalPush(ForkJoinTask<?> task) {
        int r = ThreadLocalRandom.getProbe();
        if (r == 0) {
            ThreadLocalRandom.localInit();
            r = ThreadLocalRandom.getProbe();
        }
        while (true) {
            int rs = this.runState;
            WorkQueue[] ws = this.workQueues;
            if (rs > 0 && ws != null) {
                int wl = ws.length;
                if (wl > 0) {
                    int k = ((wl - 1) & r) & SQMASK;
                    WorkQueue q = ws[k];
                    if (q == null) {
                        tryCreateExternalQueue(k);
                    } else {
                        int stat = q.sharedPush(task);
                        if (stat >= 0) {
                            if (stat == 0) {
                                signalWork();
                                return;
                            }
                            r = ThreadLocalRandom.advanceProbe(r);
                        } else {
                            return;
                        }
                    }
                }
            }
            tryInitialize(true);
        }
    }

    private <T> ForkJoinTask<T> externalSubmit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread w = (ForkJoinWorkerThread) t;
            if (w.pool == this) {
                WorkQueue q = w.workQueue;
                if (q != null) {
                    q.push(task);
                    return task;
                }
            }
        }
        externalPush(task);
        return task;
    }

    static WorkQueue commonSubmitterQueue() {
        ForkJoinPool p = common;
        int r = ThreadLocalRandom.getProbe();
        if (p == null) {
            return null;
        }
        WorkQueue[] ws = p.workQueues;
        if (ws == null) {
            return null;
        }
        int wl = ws.length;
        if (wl > 0) {
            return ws[((wl - 1) & r) & SQMASK];
        }
        return null;
    }

    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] ws = this.workQueues;
        if (ws == null) {
            return false;
        }
        int wl = ws.length;
        if (wl <= 0) {
            return false;
        }
        WorkQueue w = ws[((wl - 1) & r) & SQMASK];
        if (w != null) {
            return w.trySharedUnpush(task);
        }
        return false;
    }

    final int externalHelpComplete(CountedCompleter<?> task, int maxTasks) {
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] ws = this.workQueues;
        if (ws == null) {
            return 0;
        }
        int wl = ws.length;
        if (wl > 0) {
            return helpComplete(ws[((wl - 1) & r) & SQMASK], task, maxTasks);
        }
        return 0;
    }

    public ForkJoinPool() {
        this(Math.min((int) MAX_CAP, Runtime.getRuntime().availableProcessors()), defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        this(checkParallelism(parallelism), checkFactory(factory), handler, asyncMode ? Integer.MIN_VALUE : 0, "ForkJoinPool-" + nextPoolId() + "-worker-");
        checkPermission();
    }

    private static int checkParallelism(int parallelism) {
        if (parallelism > 0 && parallelism <= MAX_CAP) {
            return parallelism;
        }
        throw new IllegalArgumentException();
    }

    private static ForkJoinWorkerThreadFactory checkFactory(ForkJoinWorkerThreadFactory factory) {
        if (factory != null) {
            return factory;
        }
        throw new NullPointerException();
    }

    private ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, int mode, String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
        this.factory = factory;
        this.ueh = handler;
        this.config = (SMASK & parallelism) | mode;
        long np = (long) (-parallelism);
        this.ctl = ((np << AC_SHIFT) & AC_MASK) | ((np << 32) & TC_MASK);
    }

    public static ForkJoinPool commonPool() {
        return common;
    }

    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        externalSubmit(task);
        return task.join();
    }

    public void execute(ForkJoinTask<?> task) {
        externalSubmit(task);
    }

    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask) {
            job = (ForkJoinTask) task;
        } else {
            job = new RunnableExecuteAction(task);
        }
        externalSubmit(job);
    }

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        return externalSubmit(task);
    }

    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return externalSubmit(new AdaptedCallable(task));
    }

    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return externalSubmit(new AdaptedRunnable(task, result));
    }

    public ForkJoinTask<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask) {
            job = (ForkJoinTask) task;
        } else {
            job = new AdaptedRunnableAction(task);
        }
        return externalSubmit(job);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList(tasks.size());
        int size;
        int i;
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = new AdaptedCallable(t);
                futures.add(f);
                externalSubmit(f);
            }
            size = futures.size();
            for (i = 0; i < size; i++) {
                ((ForkJoinTask) futures.get(i)).quietlyJoin();
            }
            return futures;
        } catch (Throwable th) {
            size = futures.size();
            for (i = 0; i < size; i++) {
                ((Future) futures.get(i)).cancel(false);
            }
        }
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return this.factory;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.ueh;
    }

    public int getParallelism() {
        int par = this.config & SMASK;
        return par > 0 ? par : 1;
    }

    public static int getCommonPoolParallelism() {
        return COMMON_PARALLELISM;
    }

    public int getPoolSize() {
        return (this.config & SMASK) + ((short) ((int) (this.ctl >>> 32)));
    }

    public boolean getAsyncMode() {
        return (this.config & Integer.MIN_VALUE) != 0;
    }

    public int getRunningThreadCount() {
        int rc = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null && w.isApparentlyUnblocked()) {
                    rc++;
                }
            }
        }
        return rc;
    }

    public int getActiveThreadCount() {
        int r = (this.config & SMASK) + ((int) (this.ctl >> AC_SHIFT));
        return r <= 0 ? 0 : r;
    }

    public boolean isQuiescent() {
        return (this.config & SMASK) + ((int) (this.ctl >> AC_SHIFT)) <= 0;
    }

    public long getStealCount() {
        AuxState sc = this.auxState;
        long count = sc == null ? 0 : sc.stealCount;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null) {
                    count += (long) w.nsteals;
                }
            }
        }
        return count;
    }

    public long getQueuedTaskCount() {
        long count = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null) {
                    count += (long) w.queueSize();
                }
            }
        }
        return count;
    }

    public int getQueuedSubmissionCount() {
        int count = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null) {
                    count += w.queueSize();
                }
            }
        }
        return count;
    }

    public boolean hasQueuedSubmissions() {
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null && (w.isEmpty() ^ 1) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected ForkJoinTask<?> pollSubmission() {
        int r = ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            int wl = ws.length;
            if (wl > 0) {
                int m = wl - 1;
                for (int i = 0; i < wl; i++) {
                    WorkQueue w = ws[(i << 1) & m];
                    if (w != null) {
                        ForkJoinTask<?> t = w.poll();
                        if (t != null) {
                            return t;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        int count = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (WorkQueue w : ws) {
                if (w != null) {
                    while (true) {
                        ForkJoinTask<?> t = w.poll();
                        if (t == null) {
                            break;
                        }
                        c.add(t);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public String toString() {
        String level;
        long qt = 0;
        long qs = 0;
        int rc = 0;
        AuxState sc = this.auxState;
        long st = sc == null ? 0 : sc.stealCount;
        long c = this.ctl;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i++) {
                WorkQueue w = ws[i];
                if (w != null) {
                    int size = w.queueSize();
                    if ((i & 1) == 0) {
                        qs += (long) size;
                    } else {
                        qt += (long) size;
                        st += (long) w.nsteals;
                        if (w.isApparentlyUnblocked()) {
                            rc++;
                        }
                    }
                }
            }
        }
        int pc = this.config & SMASK;
        int tc = pc + ((short) ((int) (c >>> 32)));
        int ac = pc + ((int) (c >> AC_SHIFT));
        if (ac < 0) {
            ac = 0;
        }
        int rs = this.runState;
        if ((rs & 4) != 0) {
            level = "Terminated";
        } else if ((rs & 2) != 0) {
            level = "Terminating";
        } else if ((Integer.MIN_VALUE & rs) != 0) {
            level = "Shutting down";
        } else {
            level = "Running";
        }
        return super.toString() + "[" + level + ", parallelism = " + pc + ", size = " + tc + ", active = " + ac + ", running = " + rc + ", steals = " + st + ", tasks = " + qt + ", submissions = " + qs + "]";
    }

    public void shutdown() {
        checkPermission();
        tryTerminate(false, true);
    }

    public List<Runnable> shutdownNow() {
        checkPermission();
        tryTerminate(true, true);
        return Collections.emptyList();
    }

    public boolean isTerminated() {
        return (this.runState & 4) != 0;
    }

    public boolean isTerminating() {
        int rs = this.runState;
        if ((rs & 2) == 0 || (rs & 4) != 0) {
            return false;
        }
        return true;
    }

    public boolean isShutdown() {
        return (this.runState & Integer.MIN_VALUE) != 0;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else if (this == common) {
            awaitQuiescence(timeout, unit);
            return false;
        } else {
            long nanos = unit.toNanos(timeout);
            if (isTerminated()) {
                return true;
            }
            if (nanos <= 0) {
                return false;
            }
            long deadline = System.nanoTime() + nanos;
            synchronized (this) {
                while (!isTerminated()) {
                    if (nanos <= 0) {
                        return false;
                    }
                    long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
                    if (millis <= 0) {
                        millis = 1;
                    }
                    wait(millis);
                    nanos = deadline - System.nanoTime();
                }
                return true;
            }
        }
    }

    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        long nanos = unit.toNanos(timeout);
        Thread thread = Thread.currentThread();
        if (thread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) thread;
            if (wt.pool == this) {
                helpQuiescePool(wt.workQueue);
                return true;
            }
        }
        long startTime = System.nanoTime();
        int r = 0;
        boolean found = true;
        while (!isQuiescent()) {
            WorkQueue[] ws = this.workQueues;
            if (ws == null) {
                break;
            }
            int wl = ws.length;
            if (wl <= 0) {
                break;
            }
            if (!found) {
                if (System.nanoTime() - startTime > nanos) {
                    return false;
                }
                Thread.yield();
            }
            found = false;
            int m = wl - 1;
            int j = (m + 1) << 2;
            int r2 = r;
            while (j >= 0) {
                r = r2 + 1;
                int k = r2 & m;
                if (k <= m && k >= 0) {
                    WorkQueue q = ws[k];
                    if (q != null) {
                        int b = q.base;
                        if (b - q.top < 0) {
                            found = true;
                            ForkJoinTask<?> t = q.pollAt(b);
                            if (t != null) {
                                t.doExec();
                            }
                        }
                    } else {
                        continue;
                    }
                }
                j--;
                r2 = r;
            }
            r = r2;
        }
        return true;
    }

    static void quiesceCommonPool() {
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
            ForkJoinPool p = wt.pool;
            if (p != null) {
                WorkQueue w = wt.workQueue;
                while (!blocker.isReleasable()) {
                    if (p.tryCompensate(w)) {
                        do {
                            try {
                                if (blocker.isReleasable()) {
                                    break;
                                }
                            } catch (Throwable th) {
                                Throwable th2 = th;
                                U.getAndAddLong(p, CTL, AC_UNIT);
                            }
                        } while ((blocker.block() ^ 1) != 0);
                        U.getAndAddLong(p, CTL, AC_UNIT);
                        return;
                    }
                }
                return;
            }
        }
        while (!blocker.isReleasable() && (blocker.block() ^ 1) != 0) {
        }
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new AdaptedRunnable(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new AdaptedCallable(callable);
    }

    static {
        try {
            CTL = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("ctl"));
            RUNSTATE = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("runState"));
            ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
            int scale = U.arrayIndexScale(ForkJoinTask[].class);
            if (((scale - 1) & scale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            Class<?> ensureLoaded = LockSupport.class;
            int commonMaxSpares = 256;
            try {
                String p = System.getProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares");
                if (p != null) {
                    commonMaxSpares = Integer.parseInt(p);
                }
            } catch (Exception e) {
            }
            COMMON_MAX_SPARES = commonMaxSpares;
        } catch (Throwable e2) {
            throw new Error(e2);
        }
    }

    static ForkJoinPool makeCommonPool() {
        int parallelism = -1;
        ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory = null;
        UncaughtExceptionHandler handler = null;
        try {
            String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            String fp = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String hp = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            if (pp != null) {
                parallelism = Integer.parseInt(pp);
            }
            if (fp != null) {
                forkJoinWorkerThreadFactory = (ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance();
            }
            if (hp != null) {
                handler = (UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance();
            }
        } catch (Exception e) {
        }
        if (forkJoinWorkerThreadFactory == null) {
            if (System.getSecurityManager() == null) {
                forkJoinWorkerThreadFactory = defaultForkJoinWorkerThreadFactory;
            } else {
                forkJoinWorkerThreadFactory = new InnocuousForkJoinWorkerThreadFactory();
            }
        }
        if (parallelism < 0) {
            parallelism = Runtime.getRuntime().availableProcessors() - 1;
            if (parallelism <= 0) {
                parallelism = 1;
            }
        }
        if (parallelism > MAX_CAP) {
            parallelism = MAX_CAP;
        }
        return new ForkJoinPool(parallelism, forkJoinWorkerThreadFactory, handler, 0, "ForkJoinPool.commonPool-worker-");
    }
}
