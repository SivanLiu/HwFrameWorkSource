package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public class StampedLock implements Serializable {
    private static final long ABITS = 255;
    private static final int CANCELLED = 1;
    private static final int HEAD_SPINS;
    private static final long INTERRUPTED = 1;
    private static final int LG_READERS = 7;
    private static final int MAX_HEAD_SPINS;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final long ORIGIN = 256;
    private static final int OVERFLOW_YIELD_RATE = 7;
    private static final long PARKBLOCKER;
    private static final long RBITS = 127;
    private static final long RFULL = 126;
    private static final int RMODE = 0;
    private static final long RUNIT = 1;
    private static final long SBITS = -128;
    private static final int SPINS = (NCPU > 1 ? 64 : 0);
    private static final long STATE;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final int WAITING = -1;
    private static final long WBIT = 128;
    private static final long WCOWAIT;
    private static final long WHEAD;
    private static final int WMODE = 1;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WTAIL;
    private static final long serialVersionUID = -6001602636862214147L;
    transient ReadLockView readLockView;
    transient ReadWriteLockView readWriteLockView;
    private transient int readerOverflow;
    private volatile transient long state = ORIGIN;
    private volatile transient WNode whead;
    transient WriteLockView writeLockView;
    private volatile transient WNode wtail;

    final class ReadLockView implements Lock {
        ReadLockView() {
        }

        public void lock() {
            StampedLock.this.readLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            StampedLock.this.readLockInterruptibly();
        }

        public boolean tryLock() {
            return StampedLock.this.tryReadLock() != 0;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return StampedLock.this.tryReadLock(time, unit) != 0;
        }

        public void unlock() {
            StampedLock.this.unstampedUnlockRead();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        ReadWriteLockView() {
        }

        public Lock readLock() {
            return StampedLock.this.asReadLock();
        }

        public Lock writeLock() {
            return StampedLock.this.asWriteLock();
        }
    }

    static final class WNode {
        volatile WNode cowait;
        final int mode;
        volatile WNode next;
        volatile WNode prev;
        volatile int status;
        volatile Thread thread;

        WNode(int m, WNode p) {
            this.mode = m;
            this.prev = p;
        }
    }

    final class WriteLockView implements Lock {
        WriteLockView() {
        }

        public void lock() {
            StampedLock.this.writeLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            StampedLock.this.writeLockInterruptibly();
        }

        public boolean tryLock() {
            return StampedLock.this.tryWriteLock() != 0;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return StampedLock.this.tryWriteLock(time, unit) != 0;
        }

        public void unlock() {
            StampedLock.this.unstampedUnlockWrite();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    static {
        int i;
        int i2 = 0;
        if (NCPU > 1) {
            i = 1024;
        } else {
            i = 0;
        }
        HEAD_SPINS = i;
        if (NCPU > 1) {
            i2 = 65536;
        }
        MAX_HEAD_SPINS = i2;
        try {
            STATE = U.objectFieldOffset(StampedLock.class.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset(StampedLock.class.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset(StampedLock.class.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset(WNode.class.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset(WNode.class.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset(WNode.class.getDeclaredField("cowait"));
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    public long writeLock() {
        long s = this.state;
        if ((ABITS & s) == 0) {
            long next = s + WBIT;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return acquireWrite(false, 0);
    }

    public long tryWriteLock() {
        long s = this.state;
        if ((ABITS & s) == 0) {
            long next = s + WBIT;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return 0;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next = tryWriteLock();
            if (next != 0) {
                return next;
            }
            if (nanos <= 0) {
                return 0;
            }
            long deadline = System.nanoTime() + nanos;
            if (deadline == 0) {
                deadline = 1;
            }
            next = acquireWrite(true, deadline);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long writeLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long next = acquireWrite(true, 0);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long readLock() {
        long s = this.state;
        if (this.whead == this.wtail && (ABITS & s) < RFULL) {
            long next = s + 1;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return acquireRead(false, 0);
    }

    public long tryReadLock() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == WBIT) {
                return 0;
            }
            long next;
            if (m < RFULL) {
                next = s + 1;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    return next;
                }
            } else {
                next = tryIncReaderOverflow(s);
                if (next != 0) {
                    return next;
                }
            }
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next;
            long s = this.state;
            long m = s & ABITS;
            if (m != WBIT) {
                if (m < RFULL) {
                    next = s + 1;
                    if (U.compareAndSwapLong(this, STATE, s, next)) {
                        return next;
                    }
                }
                next = tryIncReaderOverflow(s);
                if (next != 0) {
                    return next;
                }
            }
            if (nanos <= 0) {
                return 0;
            }
            long deadline = System.nanoTime() + nanos;
            if (deadline == 0) {
                deadline = 1;
            }
            next = acquireRead(true, deadline);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long readLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long next = acquireRead(true, 0);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long tryOptimisticRead() {
        long s = this.state;
        return (WBIT & s) == 0 ? SBITS & s : 0;
    }

    public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (this.state & SBITS);
    }

    public void unlockWrite(long stamp) {
        if (this.state != stamp || (stamp & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        long j;
        Unsafe unsafe = U;
        long j2 = STATE;
        stamp += WBIT;
        if (stamp == 0) {
            j = ORIGIN;
        } else {
            j = stamp;
        }
        unsafe.putLongVolatile(this, j2, j);
        WNode h = this.whead;
        if (h != null && h.status != 0) {
            release(h);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unlockRead(long stamp) {
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp) || (ABITS & stamp) == 0) {
                break;
            }
            long m = s & ABITS;
            if (m == 0 || m == WBIT) {
                break;
            } else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                    break;
                }
            } else if (tryDecReaderOverflow(s) != 0) {
                return;
            }
        }
        if (m == 1) {
            WNode h = this.whead;
            if (h != null && h.status != 0) {
                release(h);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unlock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            if (m != 0) {
                if (m != WBIT) {
                    if (a == 0 || a >= WBIT) {
                        break;
                    } else if (m < RFULL) {
                        if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                            break;
                        }
                    } else if (tryDecReaderOverflow(s) != 0) {
                        return;
                    }
                } else {
                    break;
                }
            }
            break;
        }
        throw new IllegalMonitorStateException();
    }

    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            long next;
            if (m != 0) {
                if (m != WBIT) {
                    if (m != 1 || a == 0) {
                        break;
                    }
                    next = (s - 1) + WBIT;
                    if (U.compareAndSwapLong(this, STATE, s, next)) {
                        return next;
                    }
                } else {
                    break;
                }
            } else if (a != 0) {
                break;
            } else {
                next = s + WBIT;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    return next;
                }
            }
        }
        if (a == m) {
            return stamp;
        }
        return 0;
    }

    public long tryConvertToReadLock(long stamp) {
        long next;
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            if (m != 0) {
                break;
            } else if (a != 0) {
                break;
            } else if (m < RFULL) {
                next = s + 1;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    return next;
                }
            } else {
                next = tryIncReaderOverflow(s);
                if (next != 0) {
                    return next;
                }
            }
        }
        if (m == WBIT) {
            if (a == m) {
                next = s + 129;
                U.putLongVolatile(this, STATE, next);
                WNode h = this.whead;
                if (!(h == null || h.status == 0)) {
                    release(h);
                }
                return next;
            }
        } else if (a != 0 && a < WBIT) {
            return stamp;
        }
        return 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS;
        U.loadFence();
        while (true) {
            long s = this.state;
            if ((SBITS & s) == (SBITS & stamp)) {
                long m = s & ABITS;
                if (m != 0) {
                    if (m != WBIT) {
                        if (a == 0 || a >= WBIT) {
                            break;
                        } else if (m < RFULL) {
                            next = s - 1;
                            if (U.compareAndSwapLong(this, STATE, s, next)) {
                                break;
                            }
                        } else {
                            next = tryDecReaderOverflow(s);
                            if (next != 0) {
                                return SBITS & next;
                            }
                        }
                    } else {
                        break;
                    }
                }
                break;
            }
            break;
        }
        return 0;
    }

    public boolean tryUnlockWrite() {
        long s = this.state;
        if ((s & WBIT) == 0) {
            return false;
        }
        long j;
        Unsafe unsafe = U;
        long j2 = STATE;
        s += WBIT;
        if (s == 0) {
            j = ORIGIN;
        } else {
            j = s;
        }
        unsafe.putLongVolatile(this, j2, j);
        WNode h = this.whead;
        if (!(h == null || h.status == 0)) {
            release(h);
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean tryUnlockRead() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m != 0 && m < WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        break;
                    }
                } else if (tryDecReaderOverflow(s) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getReadLockCount(long s) {
        long readers = s & RBITS;
        if (readers >= RFULL) {
            readers = RFULL + ((long) this.readerOverflow);
        }
        return (int) readers;
    }

    public boolean isWriteLocked() {
        return (this.state & WBIT) != 0;
    }

    public boolean isReadLocked() {
        return (this.state & RBITS) != 0;
    }

    public int getReadLockCount() {
        return getReadLockCount(this.state);
    }

    public String toString() {
        String str;
        long s = this.state;
        StringBuilder append = new StringBuilder().append(super.toString());
        if ((ABITS & s) == 0) {
            str = "[Unlocked]";
        } else if ((WBIT & s) != 0) {
            str = "[Write-locked]";
        } else {
            str = "[Read-locks:" + getReadLockCount(s) + "]";
        }
        return append.append(str).toString();
    }

    public Lock asReadLock() {
        ReadLockView v = this.readLockView;
        if (v != null) {
            return v;
        }
        v = new ReadLockView();
        this.readLockView = v;
        return v;
    }

    public Lock asWriteLock() {
        WriteLockView v = this.writeLockView;
        if (v != null) {
            return v;
        }
        v = new WriteLockView();
        this.writeLockView = v;
        return v;
    }

    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v = this.readWriteLockView;
        if (v != null) {
            return v;
        }
        v = new ReadWriteLockView();
        this.readWriteLockView = v;
        return v;
    }

    final void unstampedUnlockWrite() {
        long s = this.state;
        if ((s & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        long j;
        Unsafe unsafe = U;
        long j2 = STATE;
        s += WBIT;
        if (s == 0) {
            j = ORIGIN;
        } else {
            j = s;
        }
        unsafe.putLongVolatile(this, j2, j);
        WNode h = this.whead;
        if (h != null && h.status != 0) {
            release(h);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void unstampedUnlockRead() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m != 0 && m < WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        break;
                    }
                } else if (tryDecReaderOverflow(s) != 0) {
                    return;
                }
            }
        }
        throw new IllegalMonitorStateException();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        U.putLongVolatile(this, STATE, ORIGIN);
    }

    private long tryIncReaderOverflow(long s) {
        if ((ABITS & s) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                this.readerOverflow++;
                U.putLongVolatile(this, STATE, s);
                return s;
            }
        } else if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
        }
        return 0;
    }

    private long tryDecReaderOverflow(long s) {
        if ((ABITS & s) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, RBITS | s)) {
                long next;
                int r = this.readerOverflow;
                if (r > 0) {
                    this.readerOverflow = r - 1;
                    next = s;
                } else {
                    next = s - 1;
                }
                U.putLongVolatile(this, STATE, next);
                return next;
            }
        } else if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
        }
        return 0;
    }

    private void release(WNode h) {
        if (h != null) {
            U.compareAndSwapInt(h, WSTATUS, -1, 0);
            WNode q = h.next;
            if (q == null || q.status == 1) {
                WNode t = this.wtail;
                while (t != null && t != h) {
                    if (t.status <= 0) {
                        q = t;
                    }
                    t = t.prev;
                }
            }
            if (q != null) {
                Thread w = q.thread;
                if (w != null) {
                    U.unpark(w);
                }
            }
        }
    }

    private long acquireWrite(boolean interruptible, long deadline) {
        long ns;
        WNode p;
        Object node = null;
        int spins = -1;
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == 0) {
                ns = s + WBIT;
                if (U.compareAndSwapLong(this, STATE, s, ns)) {
                    return ns;
                }
            } else if (spins < 0) {
                spins = (m == WBIT && this.wtail == this.whead) ? SPINS : 0;
            } else if (spins <= 0) {
                p = this.wtail;
                WNode wNode;
                if (p == null) {
                    wNode = new WNode(1, null);
                    if (U.compareAndSwapObject(this, WHEAD, null, wNode)) {
                        this.wtail = wNode;
                    }
                } else if (node == null) {
                    wNode = new WNode(1, p);
                } else if (node.prev != p) {
                    node.prev = p;
                } else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    break;
                }
            } else if (LockSupport.nextSecondarySeed() >= 0) {
                spins--;
            }
        }
        p.next = node;
        boolean wasInterrupted = false;
        spins = -1;
        loop1:
        while (true) {
            WNode h = this.whead;
            if (h == p) {
                if (spins < 0) {
                    spins = HEAD_SPINS;
                } else if (spins < MAX_HEAD_SPINS) {
                    spins <<= 1;
                }
                int k = spins;
                while (true) {
                    s = this.state;
                    if ((ABITS & s) == 0) {
                        ns = s + WBIT;
                        if (U.compareAndSwapLong(this, STATE, s, ns)) {
                            break loop1;
                        }
                    } else if (LockSupport.nextSecondarySeed() >= 0) {
                        k--;
                        if (k <= 0) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            } else if (h != null) {
                while (true) {
                    WNode c = h.cowait;
                    if (c == null) {
                        break;
                    } else if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait)) {
                        Thread w = c.thread;
                        if (w != null) {
                            U.unpark(w);
                        }
                    }
                }
            }
            if (this.whead == h) {
                WNode np = node.prev;
                if (np == p) {
                    int ps = p.status;
                    if (ps == 0) {
                        U.compareAndSwapInt(p, WSTATUS, 0, -1);
                    } else if (ps == 1) {
                        WNode pp = p.prev;
                        if (pp != null) {
                            node.prev = pp;
                            pp.next = node;
                        }
                    } else {
                        long time;
                        if (deadline == 0) {
                            time = 0;
                        } else {
                            time = deadline - System.nanoTime();
                            if (time <= 0) {
                                return cancelWaiter(node, node, false);
                            }
                        }
                        Thread wt = Thread.currentThread();
                        U.putObject(wt, PARKBLOCKER, this);
                        node.thread = wt;
                        if (p.status < 0 && (!(p == h && (this.state & ABITS) == 0) && this.whead == h && node.prev == p)) {
                            U.park(false, time);
                        }
                        node.thread = null;
                        U.putObject(wt, PARKBLOCKER, null);
                        if (!Thread.interrupted()) {
                            continue;
                        } else if (interruptible) {
                            return cancelWaiter(node, node, true);
                        } else {
                            wasInterrupted = true;
                        }
                    }
                } else if (np != null) {
                    p = np;
                    np.next = node;
                }
            }
        }
        this.whead = node;
        node.prev = null;
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return ns;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long acquireRead(boolean interruptible, long deadline) {
        WNode p;
        long ns;
        WNode wNode;
        Thread w;
        WNode pp;
        boolean wasInterrupted = false;
        Object node = null;
        int spins = -1;
        loop0:
        while (true) {
            long s;
            long m;
            WNode np;
            WNode h;
            long time;
            WNode h2 = this.whead;
            p = this.wtail;
            if (h2 == p) {
                while (true) {
                    s = this.state;
                    m = s & ABITS;
                    WNode nh;
                    if (m < RFULL) {
                        ns = s + 1;
                        if (U.compareAndSwapLong(this, STATE, s, ns)) {
                            break loop0;
                        }
                        if (m < WBIT) {
                            if (spins > 0) {
                                if (spins == 0) {
                                    nh = this.whead;
                                    np = this.wtail;
                                    if (nh != h2 && np == p) {
                                        break;
                                    }
                                    h2 = nh;
                                    p = np;
                                    if (nh != np) {
                                        break;
                                    }
                                }
                                spins = SPINS;
                            } else if (LockSupport.nextSecondarySeed() >= 0) {
                                spins--;
                            }
                        }
                    } else {
                        if (m < WBIT) {
                            ns = tryIncReaderOverflow(s);
                            if (ns != 0) {
                                break loop0;
                            }
                        }
                        if (m < WBIT) {
                            if (spins > 0) {
                                if (spins == 0) {
                                    nh = this.whead;
                                    np = this.wtail;
                                    if (nh != h2) {
                                    }
                                    h2 = nh;
                                    p = np;
                                    if (nh != np) {
                                        break;
                                    }
                                }
                                spins = SPINS;
                            } else if (LockSupport.nextSecondarySeed() >= 0) {
                                spins--;
                            }
                        }
                    }
                }
                h = h2;
            } else {
                h = h2;
            }
            WNode wNode2;
            if (p == null) {
                wNode2 = new WNode(1, null);
                if (U.compareAndSwapObject(this, WHEAD, null, wNode2)) {
                    this.wtail = wNode2;
                    h2 = h;
                }
            } else if (node == null) {
                wNode2 = new WNode(0, p);
                h2 = h;
            } else if (h == p || p.mode != 0) {
                if (node.prev == p) {
                    if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                        break;
                    }
                }
                node.prev = p;
                h2 = h;
            } else {
                Unsafe unsafe = U;
                long j = WCOWAIT;
                wNode = p.cowait;
                node.cowait = wNode;
                if (unsafe.compareAndSwapObject(p, j, wNode, node)) {
                    while (true) {
                        h2 = this.whead;
                        if (h2 != null) {
                            wNode = h2.cowait;
                            if (wNode != null && U.compareAndSwapObject(h2, WCOWAIT, wNode, wNode.cowait)) {
                                w = wNode.thread;
                                if (w != null) {
                                    U.unpark(w);
                                }
                            }
                        }
                        pp = p.prev;
                        if (h2 == pp || h2 == p || pp == null) {
                            while (true) {
                                s = this.state;
                                m = s & ABITS;
                                if (m < RFULL) {
                                    ns = s + 1;
                                    if (U.compareAndSwapLong(this, STATE, s, ns)) {
                                        break loop0;
                                    }
                                    if (m < WBIT) {
                                        break;
                                    }
                                } else {
                                    if (m < WBIT) {
                                        ns = tryIncReaderOverflow(s);
                                        if (ns != 0) {
                                            break loop0;
                                        }
                                    }
                                    if (m < WBIT) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (this.whead == h2 && p.prev == pp) {
                            if (pp == null || h2 == p || p.status > 0) {
                                node = null;
                            } else {
                                if (deadline != 0) {
                                    time = deadline - System.nanoTime();
                                    if (time <= 0) {
                                        break loop0;
                                    }
                                }
                                time = 0;
                                Thread wt = Thread.currentThread();
                                U.putObject(wt, PARKBLOCKER, this);
                                node.thread = wt;
                                if ((h2 != pp || (this.state & ABITS) == WBIT) && this.whead == h2 && p.prev == pp) {
                                    U.park(false, time);
                                }
                                node.thread = null;
                                U.putObject(wt, PARKBLOCKER, null);
                                if (!Thread.interrupted()) {
                                    continue;
                                } else if (interruptible) {
                                    return cancelWaiter(node, p, true);
                                } else {
                                    wasInterrupted = true;
                                }
                            }
                        }
                    }
                    node = null;
                } else {
                    node.cowait = null;
                    h2 = h;
                }
            }
        }
        p.next = node;
        spins = -1;
        loop2:
        while (true) {
            h2 = this.whead;
            if (h2 == p) {
                if (spins < 0) {
                    spins = HEAD_SPINS;
                } else if (spins < MAX_HEAD_SPINS) {
                    spins <<= 1;
                }
                int k = spins;
                while (true) {
                    s = this.state;
                    m = s & ABITS;
                    if (m >= RFULL) {
                        if (m < WBIT) {
                            ns = tryIncReaderOverflow(s);
                            if (ns != 0) {
                                break loop2;
                            }
                        }
                        k--;
                        if (k <= 0) {
                            break;
                        }
                    } else {
                        ns = s + 1;
                        if (U.compareAndSwapLong(this, STATE, s, ns)) {
                            break loop2;
                        }
                        if (m >= WBIT && LockSupport.nextSecondarySeed() >= 0) {
                            k--;
                            if (k <= 0) {
                                break;
                            }
                        }
                    }
                }
            } else if (h2 != null) {
                while (true) {
                    wNode = h2.cowait;
                    if (wNode == null) {
                        break;
                    } else if (U.compareAndSwapObject(h2, WCOWAIT, wNode, wNode.cowait)) {
                        w = wNode.thread;
                        if (w != null) {
                            U.unpark(w);
                        }
                    }
                }
            }
            if (this.whead == h2) {
                np = node.prev;
                if (np == p) {
                    int ps = p.status;
                    if (ps == 0) {
                        U.compareAndSwapInt(p, WSTATUS, 0, -1);
                    } else if (ps == 1) {
                        pp = p.prev;
                        if (pp != null) {
                            node.prev = pp;
                            pp.next = node;
                        }
                    } else {
                        if (deadline == 0) {
                            time = 0;
                        } else {
                            time = deadline - System.nanoTime();
                            if (time <= 0) {
                                return cancelWaiter(node, node, false);
                            }
                        }
                        wt = Thread.currentThread();
                        U.putObject(wt, PARKBLOCKER, this);
                        node.thread = wt;
                        if (p.status < 0 && ((p != h2 || (this.state & ABITS) == WBIT) && this.whead == h2 && node.prev == p)) {
                            U.park(false, time);
                        }
                        node.thread = null;
                        U.putObject(wt, PARKBLOCKER, null);
                        if (!Thread.interrupted()) {
                            continue;
                        } else if (interruptible) {
                            return cancelWaiter(node, node, true);
                        } else {
                            wasInterrupted = true;
                        }
                    }
                } else if (np != null) {
                    p = np;
                    np.next = node;
                }
            }
        }
        this.whead = node;
        node.prev = null;
        while (true) {
            wNode = node.cowait;
            if (wNode == null) {
                break;
            }
            if (U.compareAndSwapObject(node, WCOWAIT, wNode, wNode.cowait)) {
                w = wNode.thread;
                if (w != null) {
                    U.unpark(w);
                }
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return ns;
    }

    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        WNode q;
        WNode t;
        if (node != null && group != null) {
            node.status = 1;
            WNode p = group;
            while (true) {
                q = p.cowait;
                if (q == null) {
                    break;
                } else if (q.status == 1) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group;
                } else {
                    p = q;
                }
            }
            if (group == node) {
                Thread w;
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    w = r.thread;
                    if (w != null) {
                        U.unpark(w);
                    }
                }
                WNode pred = node.prev;
                while (pred != null) {
                    WNode succ;
                    WNode succ2;
                    do {
                        succ = node.next;
                        if (succ != null && succ.status != 1) {
                            break;
                        }
                        q = null;
                        t = this.wtail;
                        while (t != null && t != node) {
                            if (t.status != 1) {
                                q = t;
                            }
                            t = t.prev;
                        }
                        if (succ == q) {
                            break;
                        }
                        succ2 = q;
                    } while (!U.compareAndSwapObject(node, WNEXT, succ, q));
                    succ = succ2;
                    if (succ == null && node == this.wtail) {
                        U.compareAndSwapObject(this, WTAIL, node, pred);
                    }
                    if (pred.next == node) {
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    }
                    if (succ != null) {
                        w = succ.thread;
                        if (w != null) {
                            succ.thread = null;
                            U.unpark(w);
                        }
                    }
                    if (pred.status != 1) {
                        break;
                    }
                    WNode pp = pred.prev;
                    if (pp == null) {
                        break;
                    }
                    node.prev = pp;
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h;
        do {
            h = this.whead;
            if (h == null) {
                break;
            }
            q = h.next;
            if (q == null || q.status == 1) {
                t = this.wtail;
                while (t != null && t != h) {
                    if (t.status <= 0) {
                        q = t;
                    }
                    t = t.prev;
                }
            }
        } while (h != this.whead);
        if (q != null && h.status == 0) {
            long s = this.state;
            if ((ABITS & s) != WBIT && (s == 0 || q.mode == 0)) {
                release(h);
            }
        }
        return (interrupted || Thread.interrupted()) ? 1 : 0;
    }
}
