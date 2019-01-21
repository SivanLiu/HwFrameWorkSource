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

    public interface ForkJoinWorkerThreadFactory {
        ForkJoinWorkerThread newThread(ForkJoinPool forkJoinPool);
    }

    public interface ManagedBlocker {
        boolean block() throws InterruptedException;

        boolean isReleasable();
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
            return n >= 0 ? 0 : -n;
        }

        /* JADX WARNING: Missing block: B:9:0x001c, code skipped:
            if (r3[(r4 - 1) & (r2 - 1)] != null) goto L_0x001f;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final boolean isEmpty() {
            int i = this.base;
            int i2 = this.top;
            int s = i2;
            i -= i2;
            i2 = i;
            if (i < 0) {
                if (i2 == -1) {
                    ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                    ForkJoinTask<?>[] a = forkJoinTaskArr;
                    if (forkJoinTaskArr != null) {
                        i = a.length;
                        int al = i;
                        if (i != 0) {
                        }
                    }
                }
                return false;
            }
            return true;
        }

        final void push(ForkJoinTask<?> task) {
            U.storeFence();
            int s = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            ForkJoinTask<?>[] a = forkJoinTaskArr;
            if (forkJoinTaskArr != null) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    a[(al - 1) & s] = task;
                    this.top = s + 1;
                    ForkJoinPool p = this.pool;
                    int i = this.base - s;
                    int d = i;
                    if (i == 0 && p != null) {
                        U.fullFence();
                        p.signalWork();
                    } else if (al + d == 1) {
                        growArray();
                    }
                }
            }
        }

        final ForkJoinTask<?>[] growArray() {
            Object oldA = this.array;
            int size = oldA != null ? oldA.length << 1 : 8192;
            if (size < 8192 || size > MAXIMUM_QUEUE_CAPACITY) {
                throw new RejectedExecutionException("Queue capacity exceeded");
            }
            ForkJoinTask<?>[] forkJoinTaskArr = new ForkJoinTask[size];
            this.array = forkJoinTaskArr;
            ForkJoinTask<?>[] a = forkJoinTaskArr;
            if (oldA != null) {
                int length = oldA.length - 1;
                int oldMask = length;
                if (length > 0) {
                    length = this.top;
                    int t = length;
                    int i = this.base;
                    int b = i;
                    if (length - i > 0) {
                        length = size - 1;
                        int b2 = b;
                        while (true) {
                            int mask = length;
                            long offset = (((long) (b2 & oldMask)) << ASHIFT) + ((long) ABASE);
                            ForkJoinTask<?> x = (ForkJoinTask) U.getObjectVolatile(oldA, offset);
                            if (x != null) {
                                if (U.compareAndSwapObject(oldA, offset, x, null)) {
                                    a[b2 & mask] = x;
                                }
                            }
                            b2++;
                            if (b2 == t) {
                                break;
                            }
                            length = mask;
                        }
                        U.storeFence();
                    }
                }
            }
            return a;
        }

        final ForkJoinTask<?> pop() {
            int b = this.base;
            int s = this.top;
            Object obj = this.array;
            Object a = obj;
            if (!(obj == null || b == s)) {
                int length = a.length;
                int al = length;
                if (length > 0) {
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
            Object obj = this.array;
            Object a = obj;
            if (obj != null) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    long offset = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (t != null) {
                        int b2 = b + 1;
                        if (b == this.base && U.compareAndSwapObject(a, offset, t, null)) {
                            this.base = b2;
                            return t;
                        }
                        return null;
                    }
                }
            }
            return null;
        }

        final ForkJoinTask<?> poll() {
            while (true) {
                int b = this.base;
                int s = this.top;
                Object obj = this.array;
                Object a = obj;
                if (obj == null) {
                    break;
                }
                int i = b - s;
                int d = i;
                if (i >= 0) {
                    break;
                }
                i = a.length;
                int al = i;
                if (i <= 0) {
                    break;
                }
                long offset = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                int b2 = b + 1;
                if (b == this.base) {
                    if (t != null) {
                        s = b2;
                        if (U.compareAndSwapObject(a, offset, t, null)) {
                            this.base = s;
                            return t;
                        }
                    } else {
                        if (d == -1) {
                            break;
                        }
                    }
                }
            }
            return null;
        }

        final ForkJoinTask<?> nextLocalTask() {
            return this.config < 0 ? poll() : pop();
        }

        final ForkJoinTask<?> peek() {
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            ForkJoinTask<?>[] a = forkJoinTaskArr;
            if (forkJoinTaskArr != null) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    return a[(al - 1) & (this.config < 0 ? this.base : this.top - 1)];
                }
            }
            return null;
        }

        final boolean tryUnpush(ForkJoinTask<?> task) {
            int b = this.base;
            int s = this.top;
            Object obj = this.array;
            Object a = obj;
            if (!(obj == null || b == s)) {
                int length = a.length;
                int al = length;
                if (length > 0) {
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
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            ForkJoinTask<?>[] a = forkJoinTaskArr;
            int stat = 0;
            if (forkJoinTaskArr != null) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    int i = b - s;
                    int d = i;
                    if ((al - 1) + i > 0) {
                        a[(al - 1) & s] = task;
                        this.top = s + 1;
                        this.qlock = 0;
                        if (d < 0 && b == this.base) {
                            stat = d;
                        }
                        return stat;
                    }
                }
            }
            growAndSharedPush(task);
            return stat;
        }

        private void growAndSharedPush(ForkJoinTask<?> task) {
            try {
                growArray();
                int s = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                ForkJoinTask<?>[] a = forkJoinTaskArr;
                if (forkJoinTaskArr != null) {
                    int length = a.length;
                    int al = length;
                    if (length > 0) {
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
            Object obj = this.array;
            Object a = obj;
            if (obj != null) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    long offset = (((long) ((al - 1) & s)) << ASHIFT) + ((long) ABASE);
                    if (((ForkJoinTask) U.getObject(a, offset)) == task) {
                        if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                            if (this.top == s + 1 && this.array == a && U.compareAndSwapObject(a, offset, task, null)) {
                                popped = true;
                                this.top = s;
                            }
                            U.putOrderedInt(this, QLOCK, 0);
                        }
                    }
                }
            }
            return popped;
        }

        final void cancelAll() {
            ForkJoinTask<?> forkJoinTask = this.currentJoin;
            ForkJoinTask<?> t = forkJoinTask;
            if (forkJoinTask != null) {
                this.currentJoin = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            forkJoinTask = this.currentSteal;
            t = forkJoinTask;
            if (forkJoinTask != null) {
                this.currentSteal = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            while (true) {
                forkJoinTask = poll();
                t = forkJoinTask;
                if (forkJoinTask != null) {
                    ForkJoinTask.cancelIgnoringExceptions(t);
                } else {
                    return;
                }
            }
        }

        final void localPopAndExec() {
            int nexec = 0;
            while (true) {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                ForkJoinTask<?>[] a = forkJoinTaskArr;
                if (forkJoinTaskArr != null && b != s) {
                    int length = a.length;
                    int al = length;
                    if (length > 0) {
                        s--;
                        ForkJoinTask<?> t = (ForkJoinTask) U.getAndSetObject(a, (((long) ((al - 1) & s)) << ASHIFT) + ((long) ABASE), null);
                        if (t != null) {
                            this.top = s;
                            this.currentSteal = t;
                            t.doExec();
                            nexec++;
                            if (nexec > 1023) {
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
        }

        final void localPollAndExec() {
            int nexec = 0;
            while (true) {
                int index = this.base;
                int s = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                ForkJoinTask<?>[] a = forkJoinTaskArr;
                if (forkJoinTaskArr != null && index != s) {
                    int length = a.length;
                    int al = length;
                    if (length > 0) {
                        int b = index + 1;
                        ForkJoinTask<?> t = (ForkJoinTask) U.getAndSetObject(a, (((long) (index & (al - 1))) << ASHIFT) + ((long) ABASE), null);
                        if (t != null) {
                            this.base = b;
                            t.doExec();
                            nexec++;
                            if (nexec > 1023) {
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
                AuxState auxState = p.auxState;
                AuxState aux = auxState;
                if (auxState != null) {
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
            ForkJoinTask<?> forkJoinTask = task;
            if (forkJoinTask != null && forkJoinTask.status >= 0) {
                do {
                    int i = this.base;
                    int b = i;
                    int i2 = this.top;
                    int s = i2;
                    i -= i2;
                    i2 = i;
                    if (i < 0) {
                        Object obj = this.array;
                        Object a = obj;
                        if (obj != null) {
                            i = a.length;
                            int al = i;
                            if (i > 0) {
                                while (true) {
                                    s--;
                                    long offset = (long) ((((al - 1) & s) << ASHIFT) + ABASE);
                                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                                    if (t == null) {
                                        break;
                                    } else if (t == forkJoinTask) {
                                        boolean removed = false;
                                        if (s + 1 == this.top) {
                                            if (U.compareAndSwapObject(a, offset, t, null)) {
                                                this.top = s;
                                                removed = true;
                                            }
                                        } else {
                                            ForkJoinTask<?> t2 = t;
                                            if (this.base == b) {
                                                removed = U.compareAndSwapObject(a, offset, t2, new EmptyTask());
                                            }
                                        }
                                        if (removed) {
                                            ForkJoinTask<?> ps = this.currentSteal;
                                            this.currentSteal = forkJoinTask;
                                            task.doExec();
                                            this.currentSteal = ps;
                                        }
                                    } else {
                                        if (t.status >= 0 || s + 1 != this.top) {
                                            i2++;
                                            if (i2 == 0) {
                                                if (this.base == b) {
                                                    return false;
                                                }
                                            }
                                        } else {
                                            if (U.compareAndSwapObject(a, offset, t, null)) {
                                                this.top = s;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while (forkJoinTask.status >= 0);
                return false;
            }
            return true;
        }

        final CountedCompleter<?> popCC(CountedCompleter<?> task, int mode) {
            int b = this.base;
            int s = this.top;
            Object obj = this.array;
            Object a = obj;
            if (obj != null && b != s) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    int index = (al - 1) & (s - 1);
                    long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> o = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (o instanceof CountedCompleter) {
                        CountedCompleter<?> t = (CountedCompleter) o;
                        CountedCompleter<?> r = t;
                        while (true) {
                            CountedCompleter<?> r2 = r;
                            long offset2;
                            ForkJoinTask<?> o2;
                            int index2;
                            if (r2 != task) {
                                offset2 = offset;
                                o2 = o;
                                index2 = index;
                                CountedCompleter<?> countedCompleter = r2.completer;
                                r = countedCompleter;
                                if (countedCompleter == null) {
                                    break;
                                }
                                offset = offset2;
                                o = o2;
                                index = index2;
                            } else if ((mode & 1) == 0) {
                                boolean popped = false;
                                if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                                    if (this.top == s && this.array == a) {
                                        r = r2;
                                        if (U.compareAndSwapObject(a, offset, t, null)) {
                                            popped = true;
                                            this.top = s - 1;
                                        }
                                    } else {
                                        offset2 = offset;
                                        o2 = o;
                                        index2 = index;
                                    }
                                    U.putOrderedInt(this, QLOCK, 0);
                                    if (popped) {
                                        return t;
                                    }
                                } else {
                                    offset2 = offset;
                                    o2 = o;
                                    index2 = index;
                                }
                            } else {
                                offset2 = offset;
                                o2 = o;
                                index2 = index;
                                if (U.compareAndSwapObject(a, offset, t, null)) {
                                    this.top = s - 1;
                                    return t;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        final int pollAndExecCC(CountedCompleter<?> task) {
            int h;
            int b = this.base;
            int s = this.top;
            Object obj = this.array;
            Object a = obj;
            if (!(obj == null || b == s)) {
                int length = a.length;
                int al = length;
                if (length > 0) {
                    long offset = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> o = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (o == null) {
                        h = 2;
                    } else if (o instanceof CountedCompleter) {
                        CountedCompleter<?> t = (CountedCompleter) o;
                        CountedCompleter<?> r = t;
                        while (true) {
                            CountedCompleter<?> r2 = r;
                            int i;
                            if (r2 == task) {
                                int b2 = b + 1;
                                if (b == this.base) {
                                    int b3 = b2;
                                    i = s;
                                    s = r2;
                                    if (U.compareAndSwapObject(a, offset, t, null)) {
                                        this.base = b3;
                                        t.doExec();
                                        h = 1;
                                    }
                                } else {
                                    b = b2;
                                    s = r2;
                                }
                                h = 2;
                            } else {
                                i = s;
                                CountedCompleter countedCompleter = r2.completer;
                                s = countedCompleter;
                                if (countedCompleter == null) {
                                    h = -1;
                                    break;
                                }
                                r = s;
                                s = i;
                            }
                        }
                        return h;
                    } else {
                        h = -1;
                    }
                    return h;
                }
            }
            h = b | Integer.MIN_VALUE;
            return h;
        }

        final boolean isApparentlyUnblocked() {
            if (this.scanState >= 0) {
                Thread thread = this.owner;
                Thread wt = thread;
                if (thread != null) {
                    State state = wt.getState();
                    State s = state;
                    if (!(state == State.BLOCKED || s == State.WAITING || s == State.TIMED_WAITING)) {
                        return true;
                    }
                }
            }
            return false;
        }

        static {
            try {
                QLOCK = U.objectFieldOffset(WorkQueue.class.getDeclaredField("qlock"));
                ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
                int scale = U.arrayIndexScale(ForkJoinTask[].class);
                if (((scale - 1) & scale) == 0) {
                    ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
                    return;
                }
                throw new Error("array index scale not a power of two");
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private DefaultForkJoinWorkerThreadFactory() {
        }

        /* synthetic */ DefaultForkJoinWorkerThreadFactory(AnonymousClass1 x0) {
            this();
        }

        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
        }
    }

    private static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private static final AccessControlContext innocuousAcc;

        private InnocuousForkJoinWorkerThreadFactory() {
        }

        /* synthetic */ InnocuousForkJoinWorkerThreadFactory(AnonymousClass1 x0) {
            this();
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

    private static final class AuxState extends ReentrantLock {
        private static final long serialVersionUID = -6001602636862214147L;
        long indexSeed;
        volatile long stealCount;

        AuxState() {
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
            int n2 = SMASK & (((n | (n >>> 16)) + 1) << 1);
            AuxState aux = new AuxState();
            WorkQueue[] ws = new WorkQueue[n2];
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
        ForkJoinWorkerThread wt = null;
        if (fac != null) {
            try {
                ForkJoinWorkerThread newThread = fac.newThread(this);
                wt = newThread;
                if (newThread != null) {
                    if (isSpare) {
                        WorkQueue workQueue = wt.workQueue;
                        WorkQueue q = workQueue;
                        if (workQueue != null) {
                            q.config |= 131072;
                        }
                    }
                    wt.start();
                    return true;
                }
            } catch (Throwable rex) {
                ex = rex;
            }
        }
        deregisterWorker(wt, ex);
        return false;
    }

    private void tryAddWorker(long c) {
        do {
            long nc = (AC_MASK & (AC_UNIT + c)) | (TC_MASK & (TC_UNIT + c));
            if (this.ctl == c) {
                if (U.compareAndSwapLong(this, CTL, c, nc)) {
                    createWorker(false);
                    return;
                }
            }
            long j = this.ctl;
            c = j;
            if ((j & ADD_WORKER) == 0) {
                return;
            }
        } while (((int) c) == 0);
    }

    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        wt.setDaemon(true);
        UncaughtExceptionHandler uncaughtExceptionHandler = this.ueh;
        UncaughtExceptionHandler handler = uncaughtExceptionHandler;
        if (uncaughtExceptionHandler != null) {
            wt.setUncaughtExceptionHandler(handler);
        }
        WorkQueue w = new WorkQueue(this, wt);
        int i = 0;
        int mode = this.config & MODE_MASK;
        AuxState auxState = this.auxState;
        AuxState aux = auxState;
        if (auxState != null) {
            aux.lock();
            try {
                long j = aux.indexSeed - 1640531527;
                aux.indexSeed = j;
                int s = (int) j;
                WorkQueue[] ws = this.workQueues;
                if (ws != null) {
                    int length = ws.length;
                    int n = length;
                    if (length > 0) {
                        length = n - 1;
                        int m = length;
                        i = length & (1 | (s << 1));
                        if (ws[i] != null) {
                            int probes = 0;
                            int step = 2;
                            if (n > 4) {
                                step = 2 + ((n >>> 1) & EVENMASK);
                            }
                            while (true) {
                                length = (i + step) & m;
                                i = length;
                                if (ws[length] == null) {
                                    break;
                                }
                                probes++;
                                if (probes >= n) {
                                    length = n << 1;
                                    n = length;
                                    WorkQueue[] workQueueArr = (WorkQueue[]) Arrays.copyOf((Object[]) ws, length);
                                    ws = workQueueArr;
                                    this.workQueues = workQueueArr;
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

    /* JADX WARNING: Removed duplicated region for block: B:25:0x0071  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x00ba A:{SYNTHETIC, EDGE_INSN: B:51:0x00ba->B:44:0x00ba ?: BREAK  , EDGE_INSN: B:51:0x00ba->B:44:0x00ba ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x008b  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00c0  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00bc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        int idx;
        ForkJoinWorkerThread forkJoinWorkerThread = wt;
        WorkQueue w = null;
        if (forkJoinWorkerThread != null) {
            WorkQueue workQueue = forkJoinWorkerThread.workQueue;
            WorkQueue w2 = workQueue;
            if (workQueue != null) {
                idx = SMASK & w2.config;
                int ns = w2.nsteals;
                AuxState auxState = this.auxState;
                AuxState aux = auxState;
                if (auxState != null) {
                    aux.lock();
                    try {
                        WorkQueue[] workQueueArr = this.workQueues;
                        WorkQueue[] ws = workQueueArr;
                        if (workQueueArr != null && ws.length > idx && ws[idx] == w2) {
                            ws[idx] = null;
                        }
                        aux.stealCount += (long) ns;
                    } finally {
                        aux.unlock();
                    }
                }
            }
            w = w2;
        }
        WorkQueue[] workQueueArr2;
        WorkQueue[] ws2;
        if (w == null || (w.config & 262144) == 0) {
            while (true) {
                Unsafe unsafe = U;
                long j = CTL;
                long j2 = this.ctl;
                long c = j2;
                if (unsafe.compareAndSwapLong(this, j, j2, ((AC_MASK & (c - AC_UNIT)) | (TC_MASK & (c - TC_UNIT))) | (SP_MASK & c))) {
                    break;
                }
            }
            if (w != null) {
                w.currentSteal = null;
                w.qlock = -1;
                w.cancelAll();
            }
            while (tryTerminate(false, false) >= 0 && w != null && w.array != null) {
                workQueueArr2 = this.workQueues;
                ws2 = workQueueArr2;
                if (workQueueArr2 != null) {
                    break;
                }
                idx = ws2.length;
                int wl = idx;
                if (idx <= 0) {
                    break;
                }
                long j3 = this.ctl;
                long c2 = j3;
                idx = (int) j3;
                int sp = idx;
                if (idx != 0) {
                    if (tryRelease(c2, ws2[(wl - 1) & sp], AC_UNIT)) {
                        break;
                    }
                } else if (ex != null && (ADD_WORKER & c2) != 0) {
                    tryAddWorker(c2);
                }
            }
            if (ex != null) {
                ForkJoinTask.helpExpungeStaleExceptions();
                return;
            } else {
                ForkJoinTask.rethrow(ex);
                return;
            }
        }
        if (w != null) {
        }
        while (tryTerminate(false, false) >= 0) {
            workQueueArr2 = this.workQueues;
            ws2 = workQueueArr2;
            if (workQueueArr2 != null) {
            }
        }
        if (ex != null) {
        }
    }

    final void signalWork() {
        while (true) {
            long j = this.ctl;
            long c = j;
            if (j < 0) {
                int i = (int) c;
                int sp = i;
                if (i != 0) {
                    WorkQueue[] workQueueArr = this.workQueues;
                    WorkQueue[] ws = workQueueArr;
                    if (workQueueArr != null) {
                        int i2 = SMASK & sp;
                        int i3 = i2;
                        if (ws.length > i2) {
                            WorkQueue workQueue = ws[i3];
                            WorkQueue v = workQueue;
                            if (workQueue != null) {
                                int ns = sp & Integer.MAX_VALUE;
                                int vs = v.scanState;
                                long nc = (((long) v.stackPred) & SP_MASK) | (UC_MASK & (AC_UNIT + c));
                                if (sp == vs) {
                                    if (U.compareAndSwapLong(this, CTL, c, nc)) {
                                        v.scanState = ns;
                                        LockSupport.unpark(v.parker);
                                        return;
                                    }
                                }
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                    return;
                } else if ((ADD_WORKER & c) != 0) {
                    tryAddWorker(c);
                    return;
                } else {
                    return;
                }
            }
            return;
        }
    }

    private boolean tryRelease(long c, WorkQueue v, long inc) {
        long j = c;
        WorkQueue workQueue = v;
        int sp = (int) j;
        int ns = sp & Integer.MAX_VALUE;
        if (workQueue != null) {
            long nc = (((long) workQueue.stackPred) & SP_MASK) | (UC_MASK & (j + inc));
            if (sp == workQueue.scanState) {
                if (U.compareAndSwapLong(this, CTL, j, nc)) {
                    workQueue.scanState = ns;
                    LockSupport.unpark(workQueue.parker);
                    return true;
                }
            }
        }
        return false;
    }

    private void tryReactivate(WorkQueue w, WorkQueue[] ws, int r) {
        WorkQueue workQueue = w;
        WorkQueue[] workQueueArr = ws;
        long j = this.ctl;
        long c = j;
        int i = (int) j;
        int sp = i;
        if (i != 0 && workQueue != null && workQueueArr != null) {
            i = workQueueArr.length;
            int wl = i;
            if (i > 0 && ((sp ^ r) & 65536) == 0) {
                WorkQueue workQueue2 = workQueueArr[(wl - 1) & sp];
                WorkQueue v = workQueue2;
                if (workQueue2 != null) {
                    long nc = (((long) v.stackPred) & SP_MASK) | (UC_MASK & (AC_UNIT + c));
                    int ns = sp & Integer.MAX_VALUE;
                    if (workQueue.scanState < 0 && v.scanState == sp) {
                        workQueue = v;
                        int ns2 = ns;
                        if (U.compareAndSwapLong(this, CTL, c, nc)) {
                            workQueue.scanState = ns2;
                            LockSupport.unpark(workQueue.parker);
                        }
                    }
                }
            }
        }
    }

    private void inactivate(WorkQueue w, int ss) {
        WorkQueue workQueue = w;
        int ns = (ss + 65536) | Integer.MIN_VALUE;
        long lc = ((long) ns) & SP_MASK;
        if (workQueue != null) {
            workQueue.scanState = ns;
            long c;
            long nc;
            do {
                long j = this.ctl;
                c = j;
                nc = (UC_MASK & (j - AC_UNIT)) | lc;
                workQueue.stackPred = (int) c;
            } while (!U.compareAndSwapLong(this, CTL, c, nc));
            return;
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
        int tryTerminate;
        Throwable th;
        WorkQueue workQueue = w;
        long j = c;
        int stat = 0;
        int i = 1;
        int scale = 1 - ((short) ((int) (j >>> 32)));
        if (scale > 0) {
            i = scale;
        }
        long deadline = (((long) i) * IDLE_TIMEOUT_MS) + System.currentTimeMillis();
        if (this.runState < 0) {
            tryTerminate = tryTerminate(false, false);
            stat = tryTerminate;
            if (tryTerminate <= 0) {
                return stat;
            }
        }
        int stat2 = stat;
        if (workQueue != null && workQueue.scanState < 0) {
            workQueue.parker = Thread.currentThread();
            if (workQueue.scanState < 0) {
                LockSupport.parkUntil(this, deadline);
            }
            workQueue.parker = null;
            if ((this.runState & 2) != 0) {
                workQueue.qlock = -1;
                return -1;
            }
            tryTerminate = workQueue.scanState;
            int ss = tryTerminate;
            if (tryTerminate < 0 && !Thread.interrupted() && ((int) j) == ss) {
                AuxState auxState = this.auxState;
                AuxState aux = auxState;
                if (auxState != null && this.ctl == j && deadline - System.currentTimeMillis() <= TIMEOUT_SLOP_MS) {
                    aux.lock();
                    AuxState aux2;
                    int i2;
                    try {
                        int cfg = workQueue.config;
                        int idx = cfg & SMASK;
                        int ss2 = ss;
                        long nc = (UC_MASK & (j - TC_UNIT)) | (SP_MASK & ((long) workQueue.stackPred));
                        try {
                            if ((this.runState & 2) == 0) {
                                WorkQueue[] workQueueArr = this.workQueues;
                                WorkQueue[] ws = workQueueArr;
                                if (workQueueArr != null && idx < ws.length && idx >= 0 && ws[idx] == workQueue) {
                                    int idx2 = idx;
                                    aux2 = aux;
                                    ss2 = ws;
                                    try {
                                        if (U.compareAndSwapLong(this, CTL, j, nc)) {
                                            ss2[idx2] = null;
                                            workQueue.config = cfg | 262144;
                                            stat = -1;
                                            workQueue.qlock = -1;
                                            aux2.unlock();
                                            return stat;
                                        }
                                        stat = stat2;
                                        aux2.unlock();
                                        return stat;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        aux2.unlock();
                                        throw th;
                                    }
                                }
                            }
                            aux2 = aux;
                            i2 = ss2;
                            stat = stat2;
                            aux2.unlock();
                            return stat;
                        } catch (Throwable th3) {
                            th = th3;
                            aux2 = aux;
                            i2 = ss2;
                            aux2.unlock();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        aux2 = aux;
                        i2 = ss;
                        aux2.unlock();
                        throw th;
                    }
                }
            }
        }
        return stat2;
    }

    private boolean tryDropSpare(WorkQueue w) {
        WorkQueue workQueue = w;
        if (workQueue != null && w.isEmpty()) {
            while (true) {
                long j = this.ctl;
                long c = j;
                if (((short) ((int) (j >> 32))) <= (short) 0) {
                    break;
                }
                int i = (int) c;
                int sp = i;
                if (i == 0 && ((int) (c >> AC_SHIFT)) <= 0) {
                    break;
                }
                WorkQueue[] workQueueArr = this.workQueues;
                WorkQueue[] ws = workQueueArr;
                if (workQueueArr == null) {
                    break;
                }
                i = ws.length;
                int wl = i;
                if (i <= 0) {
                    break;
                }
                boolean dropped;
                if (sp == 0) {
                    dropped = U.compareAndSwapLong(this, CTL, c, (((c - TC_UNIT) & TC_MASK) | (AC_MASK & (c - AC_UNIT))) | (SP_MASK & c));
                } else {
                    boolean canDrop = ws[(wl - 1) & sp];
                    WorkQueue v = canDrop;
                    WorkQueue v2;
                    if (canDrop) {
                        v2 = v;
                        if (v2.scanState == sp) {
                            boolean canDrop2;
                            j = SP_MASK & ((long) v2.stackPred);
                            if (workQueue == v2 || workQueue.scanState >= 0) {
                                canDrop2 = true;
                                j |= (AC_MASK & c) | (TC_MASK & (c - TC_UNIT));
                            } else {
                                canDrop2 = false;
                                j |= (AC_MASK & (AC_UNIT + c)) | (TC_MASK & c);
                            }
                            canDrop = canDrop2;
                            if (U.compareAndSwapLong(this, CTL, c, j)) {
                                v2.scanState = Integer.MAX_VALUE & sp;
                                LockSupport.unpark(v2.parker);
                                dropped = canDrop;
                            } else {
                                dropped = false;
                            }
                        }
                    } else {
                        v2 = v;
                    }
                    dropped = false;
                }
                if (dropped) {
                    int cfg = workQueue.config;
                    int idx = SMASK & cfg;
                    if (idx >= 0 && idx < ws.length && ws[idx] == workQueue) {
                        ws[idx] = null;
                    }
                    workQueue.config = 262144 | cfg;
                    workQueue.qlock = -1;
                    return true;
                }
            }
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
                    r ^= r >>> 12;
                    r ^= r << 25;
                    r ^= r >>> 27;
                    if (scan(w, bound, ((int) (r >>> AC_SHIFT)) | 1, (int) r) < 0 && awaitWork(w) < 0) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
    }

    private int scan(WorkQueue w, int bound, int step, int r) {
        int r2;
        int i;
        WorkQueue workQueue = w;
        int stat = 0;
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (!(workQueueArr == null || workQueue == null)) {
            int length = ws.length;
            int wl = length;
            if (length > 0) {
                int stat2;
                length = wl - 1;
                int origin = length & r;
                int idx = origin;
                int npolls = 0;
                int ss = workQueue.scanState;
                r2 = r;
                while (true) {
                    int m;
                    int origin2;
                    int idx2;
                    int i2;
                    WorkQueue workQueue2 = ws[idx];
                    WorkQueue q = workQueue2;
                    if (workQueue2 != null) {
                        int i3 = q.base;
                        int b = i3;
                        if (i3 - q.top < 0) {
                            Object obj = q.array;
                            Object obj2 = obj;
                            if (obj != null) {
                                i3 = obj2.length;
                                int al = i3;
                                if (i3 > 0) {
                                    stat2 = stat;
                                    m = length;
                                    origin2 = origin;
                                    idx2 = idx;
                                    stat = (((long) ((al - 1) & b)) << ASHIFT) + ((long) ABASE);
                                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(obj2, stat);
                                    if (t != null) {
                                        idx = b + 1;
                                        if (b != q.base) {
                                            break;
                                        } else if (ss < 0) {
                                            tryReactivate(workQueue, ws, r2);
                                            break;
                                        } else {
                                            Object a = obj2;
                                            if (!U.compareAndSwapObject(obj2, stat, t, null)) {
                                                break;
                                            }
                                            q.base = idx;
                                            workQueue.currentSteal = t;
                                            if (idx != q.top) {
                                                signalWork();
                                            }
                                            workQueue.runTask(t);
                                            npolls++;
                                            if (npolls > bound) {
                                                return stat2;
                                            }
                                            idx = origin2;
                                            origin = idx2;
                                            stat = stat2;
                                            length = m;
                                            i2 = idx;
                                            idx = origin;
                                            origin = i2;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    i = bound;
                    stat2 = stat;
                    m = length;
                    origin2 = origin;
                    idx2 = idx;
                    if (npolls != 0) {
                        return stat2;
                    }
                    length = (idx2 + step) & m;
                    origin = length;
                    idx = origin2;
                    if (length != idx) {
                        continue;
                    } else if (ss < 0) {
                        return ss;
                    } else {
                        if (r2 >= 0) {
                            inactivate(workQueue, ss);
                            return stat2;
                        }
                        r2 <<= 1;
                    }
                    stat = stat2;
                    length = m;
                    i2 = idx;
                    idx = origin;
                    origin = i2;
                }
                i = bound;
                return stat2;
            }
        }
        i = bound;
        r2 = r;
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x009d  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0076  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final int helpComplete(WorkQueue w, CountedCompleter<?> task, int maxTasks) {
        WorkQueue workQueue = w;
        CountedCompleter<?> countedCompleter = task;
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            int length = ws.length;
            int wl = length;
            int i = 1;
            if (!(length <= 1 || countedCompleter == null || workQueue == null)) {
                length = wl - 1;
                int mode = workQueue.config;
                int r = ~mode;
                int origin = r & length;
                int maxTasks2 = maxTasks;
                int oldSum = 0;
                int h = 1;
                int step = 3;
                int k = origin;
                int r2 = r;
                r = 0;
                int checkSum = 0;
                while (true) {
                    int i2 = countedCompleter.status;
                    r = i2;
                    if (i2 < 0) {
                        return r;
                    }
                    if (h == i) {
                        CountedCompleter<?> popCC = workQueue.popCC(countedCompleter, mode);
                        CountedCompleter<?> p = popCC;
                        if (popCC != null) {
                            p.doExec();
                            if (maxTasks2 != 0) {
                                maxTasks2--;
                                if (maxTasks2 == 0) {
                                    return r;
                                }
                            }
                            origin = k;
                            checkSum = 0;
                            oldSum = 0;
                            workQueue = w;
                            i = 1;
                        }
                    }
                    i2 = k | 1;
                    int i3 = i2;
                    if (i2 >= 0) {
                        i2 = i3;
                        if (i2 <= length) {
                            WorkQueue workQueue2 = ws[i2];
                            WorkQueue q = workQueue2;
                            if (workQueue2 != null) {
                                int pollAndExecCC = q.pollAndExecCC(countedCompleter);
                                h = pollAndExecCC;
                                if (pollAndExecCC < 0) {
                                    checkSum += h;
                                }
                                if (h <= 0) {
                                    if (h == 1 && maxTasks2 != 0) {
                                        maxTasks2--;
                                        if (maxTasks2 == 0) {
                                            return r;
                                        }
                                    }
                                    step = (r2 >>> 16) | 3;
                                    r2 ^= r2 << 13;
                                    r2 ^= r2 >>> 17;
                                    r2 ^= r2 << 5;
                                    pollAndExecCC = r2 & length;
                                    origin = pollAndExecCC;
                                    k = pollAndExecCC;
                                    checkSum = 0;
                                    oldSum = 0;
                                } else {
                                    i = (k + step) & length;
                                    k = i;
                                    if (i == origin) {
                                        i = checkSum;
                                        if (oldSum == checkSum) {
                                            return r;
                                        }
                                        checkSum = 0;
                                        oldSum = i;
                                    } else {
                                        int checkSum2 = oldSum;
                                    }
                                }
                                workQueue = w;
                                i = 1;
                            }
                        }
                    }
                    h = 0;
                    if (h <= 0) {
                    }
                    workQueue = w;
                    i = 1;
                }
            }
        }
        return 0;
    }

    /* JADX WARNING: Missing block: B:20:0x0041, code skipped:
            r10.hint = r15;
     */
    /* JADX WARNING: Missing block: B:22:0x0046, code skipped:
            if (r9.status >= 0) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:23:0x0048, code skipped:
            r24 = r7;
            r25 = r8;
     */
    /* JADX WARNING: Missing block: B:24:0x004f, code skipped:
            r3 = r14.base;
            r11 = r3;
            r13 = r13 + r3;
            r3 = r14.currentJoin;
            r12 = 0;
            r15 = r14.array;
            r23 = r15;
     */
    /* JADX WARNING: Missing block: B:25:0x005a, code skipped:
            if (r15 == null) goto L_0x00d4;
     */
    /* JADX WARNING: Missing block: B:26:0x005c, code skipped:
            r15 = r23;
            r5 = r15.length;
            r16 = r5;
     */
    /* JADX WARNING: Missing block: B:27:0x0061, code skipped:
            if (r5 <= 0) goto L_0x00cd;
     */
    /* JADX WARNING: Missing block: B:28:0x0063, code skipped:
            r5 = (r16 - 1) & r11;
            r24 = r7;
            r25 = r8;
            r26 = r5;
            r27 = r6;
            r5 = ((long) ABASE) + (((long) r5) << ASHIFT);
            r7 = (java.util.concurrent.ForkJoinTask) U.getObjectVolatile(r15, r5);
     */
    /* JADX WARNING: Missing block: B:29:0x007f, code skipped:
            if (r7 == null) goto L_0x00cb;
     */
    /* JADX WARNING: Missing block: B:30:0x0081, code skipped:
            r8 = r11 + 1;
     */
    /* JADX WARNING: Missing block: B:31:0x0085, code skipped:
            if (r11 != r14.base) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:33:0x0089, code skipped:
            if (r10.currentJoin != r9) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:35:0x008d, code skipped:
            if (r14.currentSteal != r9) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:37:0x0091, code skipped:
            if (r9.status >= 0) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:39:0x00a3, code skipped:
            if (U.compareAndSwapObject(r15, r5, r7, null) == false) goto L_0x00c8;
     */
    /* JADX WARNING: Missing block: B:40:0x00a5, code skipped:
            r14.base = r8;
            r0.currentSteal = r7;
            r11 = r0.top;
            r12 = r7;
     */
    /* JADX WARNING: Missing block: B:41:0x00ac, code skipped:
            r12.doExec();
            r0.currentSteal = r2;
     */
    /* JADX WARNING: Missing block: B:42:0x00b3, code skipped:
            if (r1.status >= null) goto L_0x00b7;
     */
    /* JADX WARNING: Missing block: B:44:0x00b9, code skipped:
            if (r0.top != r11) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:45:0x00bc, code skipped:
            r7 = r29.pop();
            r12 = r7;
     */
    /* JADX WARNING: Missing block: B:46:0x00c1, code skipped:
            if (r7 != null) goto L_0x00c5;
     */
    /* JADX WARNING: Missing block: B:47:0x00c5, code skipped:
            r0.currentSteal = r12;
     */
    /* JADX WARNING: Missing block: B:48:0x00c8, code skipped:
            r12 = r7;
     */
    /* JADX WARNING: Missing block: B:49:0x00c9, code skipped:
            r11 = r8;
     */
    /* JADX WARNING: Missing block: B:50:0x00cb, code skipped:
            r12 = r7;
     */
    /* JADX WARNING: Missing block: B:51:0x00cd, code skipped:
            r27 = r6;
            r24 = r7;
            r25 = r8;
     */
    /* JADX WARNING: Missing block: B:52:0x00d4, code skipped:
            r27 = r6;
            r24 = r7;
            r25 = r8;
            r15 = r23;
     */
    /* JADX WARNING: Missing block: B:53:0x00dc, code skipped:
            if (r12 != 0) goto L_0x0102;
     */
    /* JADX WARNING: Missing block: B:55:0x00e0, code skipped:
            if (r11 != r14.base) goto L_0x0102;
     */
    /* JADX WARNING: Missing block: B:57:0x00e6, code skipped:
            if ((r11 - r14.top) < 0) goto L_0x0102;
     */
    /* JADX WARNING: Missing block: B:58:0x00e8, code skipped:
            r9 = r3;
     */
    /* JADX WARNING: Missing block: B:59:0x00e9, code skipped:
            if (r3 != null) goto L_0x00f5;
     */
    /* JADX WARNING: Missing block: B:61:0x00ed, code skipped:
            if (r3 != r14.currentJoin) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:62:0x00ef, code skipped:
            r5 = r13;
     */
    /* JADX WARNING: Missing block: B:63:0x00f0, code skipped:
            if (r4 != r13) goto L_0x00f3;
     */
    /* JADX WARNING: Missing block: B:64:0x00f3, code skipped:
            r4 = r5;
     */
    /* JADX WARNING: Missing block: B:65:0x00f5, code skipped:
            r10 = r14;
            r11 = r13;
            r7 = r24;
            r8 = r25;
            r6 = r27;
            r5 = r28;
     */
    /* JADX WARNING: Missing block: B:66:0x0102, code skipped:
            r7 = r24;
            r8 = r25;
            r6 = r27;
            r5 = r28;
     */
    /* JADX WARNING: Missing block: B:83:0x0133, code skipped:
            continue;
     */
    /* JADX WARNING: Missing block: B:84:0x0133, code skipped:
            continue;
     */
    /* JADX WARNING: Missing block: B:85:0x0133, code skipped:
            continue;
     */
    /* JADX WARNING: Missing block: B:87:0x0133, code skipped:
            continue;
     */
    /* JADX WARNING: Missing block: B:105:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:106:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void helpStealer(WorkQueue w, ForkJoinTask<?> task) {
        WorkQueue workQueue = w;
        ForkJoinTask<?> forkJoinTask = task;
        if (forkJoinTask != null && workQueue != null) {
            ForkJoinTask<?> ps = workQueue.currentSteal;
            int oldSum = 0;
            while (w.tryRemoveAndExec(task) && forkJoinTask.status >= 0) {
                WorkQueue[] workQueueArr = this.workQueues;
                WorkQueue[] ws = workQueueArr;
                if (workQueueArr != null) {
                    int length = ws.length;
                    int wl = length;
                    if (length > 0) {
                        int wl2;
                        length = wl - 1;
                        WorkQueue j = workQueue;
                        int checkSum = 0;
                        ForkJoinTask<?> subtask = forkJoinTask;
                        while (subtask.status >= 0) {
                            int h = j.hint | 1;
                            int checkSum2 = checkSum;
                            checkSum = 0;
                            while (true) {
                                int m;
                                WorkQueue[] ws2;
                                int i = ((checkSum << 1) + h) & length;
                                int i2 = i;
                                WorkQueue workQueue2 = ws[i];
                                WorkQueue v = workQueue2;
                                if (workQueue2 != null) {
                                    workQueue2 = v;
                                    if (workQueue2.currentSteal == subtask) {
                                        break;
                                    }
                                    m = length;
                                    ws2 = ws;
                                    wl2 = wl;
                                    checkSum2 += workQueue2.base;
                                } else {
                                    m = length;
                                    ws2 = ws;
                                    wl2 = wl;
                                    workQueue2 = v;
                                }
                                checkSum++;
                                wl = m;
                                if (checkSum <= wl) {
                                    length = wl;
                                    ws = ws2;
                                    wl = wl2;
                                } else {
                                    return;
                                }
                            }
                        }
                        wl2 = wl;
                    } else {
                        return;
                    }
                }
                return;
            }
        }
    }

    private boolean tryCompensate(WorkQueue w) {
        WorkQueue workQueue = w;
        long c = this.ctl;
        WorkQueue[] ws = this.workQueues;
        int pc = this.config & SMASK;
        int ac = pc + ((int) (c >> AC_SHIFT));
        int tc = pc + ((short) ((int) (c >> 32)));
        if (!(workQueue == null || workQueue.qlock < 0 || pc == 0 || ws == null)) {
            int length = ws.length;
            int wl = length;
            if (length > 0) {
                int m = wl - 1;
                boolean busy = true;
                for (int i = 0; i <= m; i++) {
                    int i2 = (i << 1) | 1;
                    int k = i2;
                    if (i2 <= m && k >= 0) {
                        WorkQueue workQueue2 = ws[k];
                        WorkQueue v = workQueue2;
                        if (workQueue2 != null && v.scanState >= 0 && v.currentSteal == null) {
                            busy = false;
                            break;
                        }
                    }
                }
                if (!busy) {
                } else if (this.ctl != c) {
                    int i3 = m;
                } else {
                    length = (int) c;
                    int sp = length;
                    if (length != 0) {
                        return tryRelease(c, ws[m & sp], 0);
                    } else if (tc < pc || ac <= 1 || !w.isEmpty()) {
                        if (tc >= MAX_CAP || (this == common && tc >= COMMON_MAX_SPARES + pc)) {
                            throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
                        }
                        return U.compareAndSwapLong(this, CTL, c, (AC_MASK & c) | (TC_MASK & (TC_UNIT + c))) && createWorker(tc >= pc);
                    } else {
                        return U.compareAndSwapLong(this, CTL, c, (AC_MASK & (c - AC_UNIT)) | (281474976710655L & c));
                    }
                }
                return false;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x006a  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x005a  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0074 A:{LOOP_END, LOOP:0: B:10:0x001b->B:32:0x0074} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0071 A:{SYNTHETIC, EDGE_INSN: B:37:0x0071->B:31:0x0071 ?: BREAK  } */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
        WorkQueue workQueue = w;
        ForkJoinTask<?> forkJoinTask = task;
        int s = 0;
        if (workQueue != null) {
            ForkJoinTask<?> prevJoin = workQueue.currentJoin;
            if (forkJoinTask != null) {
                int i = forkJoinTask.status;
                s = i;
                if (i >= 0) {
                    workQueue.currentJoin = forkJoinTask;
                    CountedCompleter<?> cc = forkJoinTask instanceof CountedCompleter ? (CountedCompleter) forkJoinTask : null;
                    while (true) {
                        ForkJoinPool forkJoinPool;
                        if (cc != null) {
                            forkJoinPool = this;
                            forkJoinPool.helpComplete(workQueue, cc, 0);
                        } else {
                            forkJoinPool = this;
                            helpStealer(w, task);
                        }
                        int i2 = forkJoinTask.status;
                        s = i2;
                        if (i2 < 0) {
                            break;
                        }
                        long ms;
                        long ms2;
                        if (deadline == 0) {
                            ms = 0;
                        } else {
                            long nanoTime = deadline - System.nanoTime();
                            long ns = nanoTime;
                            if (nanoTime <= 0) {
                                break;
                            }
                            nanoTime = TimeUnit.NANOSECONDS.toMillis(ns);
                            long ms3 = nanoTime;
                            if (nanoTime <= 0) {
                                ms = 1;
                            } else {
                                ms2 = ms3;
                                if (tryCompensate(w)) {
                                } else {
                                    forkJoinTask.internalWait(ms2);
                                    U.getAndAddLong(forkJoinPool, CTL, AC_UNIT);
                                }
                                i2 = forkJoinTask.status;
                                s = i2;
                                if (i2 >= 0) {
                                    break;
                                }
                            }
                        }
                        ms2 = ms;
                        if (tryCompensate(w)) {
                        }
                        i2 = forkJoinTask.status;
                        s = i2;
                        if (i2 >= 0) {
                        }
                    }
                    workQueue.currentJoin = prevJoin;
                    return s;
                }
            }
        }
        return s;
    }

    private WorkQueue findNonEmptyStealQueue() {
        int r = ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            int length = ws.length;
            int wl = length;
            if (length > 0) {
                length = wl - 1;
                int origin = r & length;
                int k = origin;
                int oldSum = 0;
                int checkSum = 0;
                while (true) {
                    int i;
                    WorkQueue workQueue = ws[k];
                    WorkQueue q = workQueue;
                    if (workQueue != null) {
                        i = q.base;
                        int b = i;
                        if (i - q.top < 0) {
                            return q;
                        }
                        checkSum += b;
                    }
                    i = (k + 1) & length;
                    k = i;
                    if (i == origin) {
                        i = checkSum;
                        if (oldSum == checkSum) {
                            break;
                        }
                        checkSum = 0;
                        oldSum = i;
                    }
                }
            }
        }
        return null;
    }

    final void helpQuiescePool(WorkQueue w) {
        WorkQueue workQueue = w;
        ForkJoinTask<?> ps = workQueue.currentSteal;
        int wc = workQueue.config;
        boolean active = true;
        while (true) {
            ForkJoinTask<?> pop;
            ForkJoinTask<?> t;
            boolean active2 = active;
            if (wc >= 0) {
                pop = w.pop();
                t = pop;
                if (pop != null) {
                    workQueue.currentSteal = t;
                    t.doExec();
                    workQueue.currentSteal = ps;
                    active = active2;
                }
            }
            WorkQueue findNonEmptyStealQueue = findNonEmptyStealQueue();
            WorkQueue q = findNonEmptyStealQueue;
            long c;
            if (findNonEmptyStealQueue != null) {
                if (!active2) {
                    active2 = true;
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
                pop = q.pollAt(q.base);
                t = pop;
                if (pop != null) {
                    workQueue.currentSteal = t;
                    t.doExec();
                    workQueue.currentSteal = ps;
                    int i = workQueue.nsteals + 1;
                    workQueue.nsteals = i;
                    if (i < 0) {
                        workQueue.transferStealCount(this);
                    }
                }
            } else if (active2) {
                long j = this.ctl;
                c = j;
                if (U.compareAndSwapLong(this, CTL, c, (AC_MASK & (j - AC_UNIT)) | (281474976710655L & c))) {
                    active2 = false;
                }
            } else {
                long j2 = this.ctl;
                c = j2;
                if (((int) (j2 >> AC_SHIFT)) + (this.config & SMASK) <= 0) {
                    if (U.compareAndSwapLong(this, CTL, c, c + AC_UNIT)) {
                        return;
                    }
                } else {
                    continue;
                }
            }
            active = active2;
        }
    }

    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        while (true) {
            ForkJoinTask<?> nextLocalTask = w.nextLocalTask();
            ForkJoinTask<?> t = nextLocalTask;
            if (nextLocalTask != null) {
                return t;
            }
            WorkQueue findNonEmptyStealQueue = findNonEmptyStealQueue();
            WorkQueue q = findNonEmptyStealQueue;
            if (findNonEmptyStealQueue == null) {
                return null;
            }
            nextLocalTask = q.pollAt(q.base);
            t = nextLocalTask;
            if (nextLocalTask != null) {
                return t;
            }
        }
    }

    static int getSurplusQueuedTaskCount() {
        Thread currentThread = Thread.currentThread();
        Thread t = currentThread;
        int i = 0;
        if (!(currentThread instanceof ForkJoinWorkerThread)) {
            return 0;
        }
        ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
        ForkJoinWorkerThread wt = forkJoinWorkerThread;
        ForkJoinPool forkJoinPool = forkJoinWorkerThread.pool;
        ForkJoinPool pool = forkJoinPool;
        int p = forkJoinPool.config & SMASK;
        WorkQueue workQueue = wt.workQueue;
        int n = workQueue.top - workQueue.base;
        int a = ((int) (pool.ctl >> AC_SHIFT)) + p;
        int i2 = p >>> 1;
        p = i2;
        if (a <= i2) {
            i = p >>> 1;
            p = i;
            if (a > i) {
                i = 1;
            } else {
                i = p >>> 1;
                p = i;
                if (a > i) {
                    i = 2;
                } else {
                    i = p >>> 1;
                    p = i;
                    i = a > i ? 4 : 8;
                }
            }
        }
        return n - i;
    }

    private int tryTerminate(boolean now, boolean enable) {
        while (true) {
            int i = this.runState;
            int rs = i;
            boolean z = false;
            if (i < 0) {
                long oldSum;
                WorkQueue[] ws;
                long checkSum;
                if ((rs & 2) != 0) {
                    oldSum = 0;
                } else {
                    if (!now) {
                        long oldSum2 = 0;
                        loop1:
                        while (true) {
                            long checkSum2 = this.ctl;
                            if (((int) (checkSum2 >> AC_SHIFT)) + (this.config & SMASK) > 0) {
                                return 0;
                            }
                            WorkQueue[] workQueueArr = this.workQueues;
                            ws = workQueueArr;
                            if (workQueueArr != null) {
                                checkSum = checkSum2;
                                for (WorkQueue workQueue : ws) {
                                    WorkQueue w = workQueue;
                                    if (workQueue != null) {
                                        int i2 = w.base;
                                        int b = i2;
                                        checkSum += (long) i2;
                                        if (w.currentSteal != null) {
                                            break loop1;
                                        } else if (b != w.top) {
                                            break loop1;
                                        }
                                    }
                                }
                                checkSum2 = checkSum;
                            }
                            long oldSum3 = checkSum2;
                            if (oldSum2 == checkSum2) {
                                break;
                            }
                            oldSum2 = oldSum3;
                        }
                        return 0;
                    }
                    Unsafe unsafe;
                    int i3;
                    do {
                        unsafe = U;
                        checkSum = RUNSTATE;
                        i3 = this.runState;
                    } while (!unsafe.compareAndSwapInt(this, checkSum, i3, i3 | 2));
                }
                oldSum = 0;
                while (true) {
                    long oldSum4 = oldSum;
                    checkSum = this.ctl;
                    WorkQueue[] workQueueArr2 = this.workQueues;
                    ws = workQueueArr2;
                    if (workQueueArr2 != null) {
                        i = z;
                        while (true) {
                            int i4 = i;
                            if (i4 >= ws.length) {
                                break;
                            }
                            WorkQueue workQueue2 = ws[i4];
                            WorkQueue w2 = workQueue2;
                            if (workQueue2 != null) {
                                w2.cancelAll();
                                checkSum += (long) w2.base;
                                if (w2.qlock >= 0) {
                                    w2.qlock = -1;
                                    ForkJoinWorkerThread forkJoinWorkerThread = w2.owner;
                                    ForkJoinWorkerThread wt = forkJoinWorkerThread;
                                    if (forkJoinWorkerThread != null) {
                                        try {
                                            wt.interrupt();
                                        } catch (Throwable th) {
                                        }
                                    }
                                }
                            }
                            i = i4 + 1;
                        }
                    }
                    oldSum = checkSum;
                    if (oldSum4 == checkSum) {
                        break;
                    }
                    z = false;
                }
                if (((short) ((int) (this.ctl >>> 32))) + (this.config & SMASK) <= 0) {
                    this.runState = -2147483641;
                    synchronized (this) {
                        notifyAll();
                    }
                }
                return -1;
            } else if (enable && this != common) {
                if (rs == 0) {
                    tryInitialize(false);
                } else {
                    U.compareAndSwapInt(this, RUNSTATE, rs, rs | Integer.MIN_VALUE);
                }
            }
        }
        return 1;
    }

    private void tryCreateExternalQueue(int index) {
        AuxState auxState = this.auxState;
        AuxState aux = auxState;
        if (auxState != null && index >= 0) {
            WorkQueue q = new WorkQueue(this, null);
            q.config = index;
            q.scanState = Integer.MAX_VALUE;
            q.qlock = 1;
            boolean installed = false;
            aux.lock();
            try {
                WorkQueue[] workQueueArr = this.workQueues;
                WorkQueue[] ws = workQueueArr;
                if (workQueueArr != null && index < ws.length && ws[index] == null) {
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
        int probe = ThreadLocalRandom.getProbe();
        int r = probe;
        if (probe == 0) {
            ThreadLocalRandom.localInit();
            r = ThreadLocalRandom.getProbe();
        }
        while (true) {
            probe = this.runState;
            WorkQueue[] ws = this.workQueues;
            if (probe > 0 && ws != null) {
                int length = ws.length;
                int wl = length;
                if (length > 0) {
                    length = ((wl - 1) & r) & SQMASK;
                    int k = length;
                    WorkQueue workQueue = ws[length];
                    WorkQueue q = workQueue;
                    if (workQueue == null) {
                        tryCreateExternalQueue(k);
                    } else {
                        length = q.sharedPush(task);
                        int stat = length;
                        if (length >= 0) {
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
        if (task != null) {
            Thread currentThread = Thread.currentThread();
            Thread t = currentThread;
            if (currentThread instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
                ForkJoinWorkerThread w = forkJoinWorkerThread;
                if (forkJoinWorkerThread.pool == this) {
                    WorkQueue workQueue = w.workQueue;
                    WorkQueue q = workQueue;
                    if (workQueue != null) {
                        q.push(task);
                        return task;
                    }
                }
            }
            externalPush(task);
            return task;
        }
        throw new NullPointerException();
    }

    static WorkQueue commonSubmitterQueue() {
        ForkJoinPool p = common;
        int r = ThreadLocalRandom.getProbe();
        if (p != null) {
            WorkQueue[] workQueueArr = p.workQueues;
            WorkQueue[] ws = workQueueArr;
            if (workQueueArr != null) {
                int length = ws.length;
                int wl = length;
                if (length > 0) {
                    return ws[((wl - 1) & r) & SQMASK];
                }
            }
        }
        return null;
    }

    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            int length = ws.length;
            int wl = length;
            if (length > 0) {
                WorkQueue workQueue = ws[((wl - 1) & r) & SQMASK];
                WorkQueue w = workQueue;
                if (workQueue != null && w.trySharedUnpush(task)) {
                    return true;
                }
            }
        }
        return false;
    }

    final int externalHelpComplete(CountedCompleter<?> task, int maxTasks) {
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            int length = ws.length;
            int wl = length;
            if (length > 0) {
                return helpComplete(ws[((wl - 1) & r) & SQMASK], task, maxTasks);
            }
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
        int checkParallelism = checkParallelism(parallelism);
        ForkJoinWorkerThreadFactory checkFactory = checkFactory(factory);
        int i = asyncMode ? Integer.MIN_VALUE : 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ForkJoinPool-");
        stringBuilder.append(nextPoolId());
        stringBuilder.append("-worker-");
        String stringBuilder2 = stringBuilder.toString();
        this(checkParallelism, checkFactory, handler, i, stringBuilder2);
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
        if (task != null) {
            externalSubmit(task);
            return task.join();
        }
        throw new NullPointerException();
    }

    public void execute(ForkJoinTask<?> task) {
        externalSubmit(task);
    }

    public void execute(Runnable task) {
        if (task != null) {
            ForkJoinTask<?> job;
            if (task instanceof ForkJoinTask) {
                job = (ForkJoinTask) task;
            } else {
                job = new RunnableExecuteAction(task);
            }
            externalSubmit(job);
            return;
        }
        throw new NullPointerException();
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
        if (task != null) {
            ForkJoinTask<?> job;
            if (task instanceof ForkJoinTask) {
                job = (ForkJoinTask) task;
            } else {
                job = new AdaptedRunnableAction(task);
            }
            return externalSubmit(job);
        }
        throw new NullPointerException();
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList(tasks.size());
        int size;
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = new AdaptedCallable(t);
                futures.add(f);
                externalSubmit(f);
            }
            size = futures.size();
            for (int i = 0; i < size; i++) {
                ((ForkJoinTask) futures.get(i)).quietlyJoin();
            }
            return futures;
        } catch (Throwable th) {
            int size2 = futures.size();
            for (size = 0; size < size2; size++) {
                ((Future) futures.get(size)).cancel(false);
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
        int i = this.config & SMASK;
        return i > 0 ? i : 1;
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
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue workQueue = ws[i];
                WorkQueue w = workQueue;
                if (workQueue != null && w.isApparentlyUnblocked()) {
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
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue workQueue = ws[i];
                WorkQueue w = workQueue;
                if (workQueue != null) {
                    count += (long) w.nsteals;
                }
            }
        }
        return count;
    }

    public long getQueuedTaskCount() {
        long count = 0;
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue workQueue = ws[i];
                WorkQueue w = workQueue;
                if (workQueue != null) {
                    count += (long) w.queueSize();
                }
            }
        }
        return count;
    }

    public int getQueuedSubmissionCount() {
        int count = 0;
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue workQueue = ws[i];
                WorkQueue w = workQueue;
                if (workQueue != null) {
                    count += w.queueSize();
                }
            }
        }
        return count;
    }

    public boolean hasQueuedSubmissions() {
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue workQueue = ws[i];
                WorkQueue w = workQueue;
                if (workQueue != null && !w.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected ForkJoinTask<?> pollSubmission() {
        int r = ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            int length = ws.length;
            int wl = length;
            if (length > 0) {
                length = wl - 1;
                for (int i = 0; i < wl; i++) {
                    WorkQueue workQueue = ws[(i << 1) & length];
                    WorkQueue w = workQueue;
                    if (workQueue != null) {
                        ForkJoinTask<?> poll = w.poll();
                        ForkJoinTask<?> t = poll;
                        if (poll != null) {
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
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            for (WorkQueue workQueue : ws) {
                WorkQueue w = workQueue;
                if (workQueue != null) {
                    while (true) {
                        ForkJoinTask<?> poll = w.poll();
                        ForkJoinTask<?> t = poll;
                        if (poll == null) {
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
        int i;
        int size;
        long c;
        String level;
        long qt = 0;
        long qs = 0;
        int rc = 0;
        AuxState sc = this.auxState;
        long st = sc == null ? 0 : sc.stealCount;
        long c2 = this.ctl;
        WorkQueue[] workQueueArr = this.workQueues;
        WorkQueue[] ws = workQueueArr;
        if (workQueueArr != null) {
            i = 0;
            while (i < ws.length) {
                WorkQueue workQueue = ws[i];
                WorkQueue w = workQueue;
                if (workQueue != null) {
                    size = w.queueSize();
                    if ((i & 1) == 0) {
                        c = c2;
                        qs += (long) size;
                    } else {
                        c = c2;
                        qt += (long) size;
                        st += (long) w.nsteals;
                        if (w.isApparentlyUnblocked()) {
                            rc++;
                        }
                    }
                } else {
                    c = c2;
                }
                i++;
                c2 = c;
            }
        }
        c = c2;
        int pc = this.config & SMASK;
        int tc = ((short) ((int) (c >>> 32))) + pc;
        i = ((int) (c >> AC_SHIFT)) + pc;
        if (i < 0) {
            i = 0;
        }
        size = this.runState;
        if ((size & 4) != 0) {
            level = "Terminated";
        } else if ((size & 2) != 0) {
            level = "Terminating";
        } else if ((Integer.MIN_VALUE & size) != 0) {
            level = "Shutting down";
        } else {
            level = "Running";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("[");
        stringBuilder.append(level);
        stringBuilder.append(", parallelism = ");
        stringBuilder.append(pc);
        stringBuilder.append(", size = ");
        stringBuilder.append(tc);
        stringBuilder.append(", active = ");
        stringBuilder.append(i);
        stringBuilder.append(", running = ");
        stringBuilder.append(rc);
        stringBuilder.append(", steals = ");
        stringBuilder.append(st);
        stringBuilder.append(", tasks = ");
        stringBuilder.append(qt);
        stringBuilder.append(", submissions = ");
        stringBuilder.append(qs);
        stringBuilder.append("]");
        return stringBuilder.toString();
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
        return (rs & 2) != 0 && (rs & 4) == 0;
    }

    public boolean isShutdown() {
        return (this.runState & Integer.MIN_VALUE) != 0;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            TimeUnit timeUnit = unit;
            throw new InterruptedException();
        }
        boolean z = false;
        if (this == common) {
            awaitQuiescence(timeout, unit);
            return false;
        }
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
                    return z;
                }
                long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
                wait(millis > 0 ? millis : 1);
                nanos = deadline - System.nanoTime();
                z = false;
                long j = timeout;
            }
            return true;
        }
    }

    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        ForkJoinPool forkJoinPool = this;
        long nanos = unit.toNanos(timeout);
        Thread thread = Thread.currentThread();
        if (thread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) thread;
            ForkJoinWorkerThread wt = forkJoinWorkerThread;
            if (forkJoinWorkerThread.pool == forkJoinPool) {
                forkJoinPool.helpQuiescePool(wt.workQueue);
                return true;
            }
        }
        long startTime = System.nanoTime();
        int r = 0;
        boolean found = true;
        while (!isQuiescent()) {
            WorkQueue[] workQueueArr = forkJoinPool.workQueues;
            WorkQueue[] ws = workQueueArr;
            if (workQueueArr == null) {
                break;
            }
            int length = ws.length;
            int wl = length;
            if (length <= 0) {
                break;
            }
            if (!found) {
                if (System.nanoTime() - startTime > nanos) {
                    return false;
                }
                Thread.yield();
            }
            found = false;
            length = wl - 1;
            int j = (length + 1) << 2;
            while (j >= 0) {
                int r2 = r + 1;
                r &= length;
                int k = r;
                if (r <= length && k >= 0) {
                    WorkQueue workQueue = ws[k];
                    WorkQueue q = workQueue;
                    if (workQueue != null) {
                        workQueue = q;
                        int i = workQueue.base;
                        int b = i;
                        if (i - workQueue.top < 0) {
                            ForkJoinTask<?> pollAt = workQueue.pollAt(b);
                            ForkJoinTask<?> t = pollAt;
                            if (pollAt != null) {
                                t.doExec();
                            }
                            found = true;
                            r = r2;
                            forkJoinPool = this;
                        }
                    } else {
                        continue;
                    }
                }
                j--;
                r = r2;
            }
            forkJoinPool = this;
        }
        return true;
    }

    static void quiesceCommonPool() {
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) t;
            ForkJoinWorkerThread wt = forkJoinWorkerThread;
            ForkJoinPool forkJoinPool = forkJoinWorkerThread.pool;
            ForkJoinPool p = forkJoinPool;
            if (forkJoinPool != null) {
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
                        } while (!blocker.block());
                        U.getAndAddLong(p, CTL, AC_UNIT);
                        return;
                    }
                }
                return;
            }
        }
        while (!blocker.isReleasable()) {
            if (blocker.block()) {
                return;
            }
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
            if (((scale - 1) & scale) == 0) {
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
                scale = LockSupport.class;
                int commonMaxSpares = 256;
                try {
                    String p = System.getProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares");
                    if (p != null) {
                        commonMaxSpares = Integer.parseInt(p);
                    }
                } catch (Exception e) {
                }
                COMMON_MAX_SPARES = commonMaxSpares;
                return;
            }
            throw new Error("array index scale not a power of two");
        } catch (ReflectiveOperationException e2) {
            throw new Error(e2);
        }
    }

    static ForkJoinPool makeCommonPool() {
        int parallelism = -1;
        ForkJoinWorkerThreadFactory factory = null;
        UncaughtExceptionHandler handler = null;
        try {
            String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            String fp = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String hp = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            if (pp != null) {
                parallelism = Integer.parseInt(pp);
            }
            if (fp != null) {
                factory = (ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance();
            }
            if (hp != null) {
                handler = (UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance();
            }
        } catch (Exception e) {
        }
        UncaughtExceptionHandler handler2 = handler;
        if (factory == null) {
            if (System.getSecurityManager() == null) {
                factory = defaultForkJoinWorkerThreadFactory;
            } else {
                factory = new InnocuousForkJoinWorkerThreadFactory();
            }
        }
        if (parallelism < 0) {
            int availableProcessors = Runtime.getRuntime().availableProcessors() - 1;
            parallelism = availableProcessors;
            if (availableProcessors <= 0) {
                parallelism = 1;
            }
        }
        if (parallelism > MAX_CAP) {
            parallelism = MAX_CAP;
        }
        return new ForkJoinPool(parallelism, factory, handler2, 0, "ForkJoinPool.commonPool-worker-");
    }
}
