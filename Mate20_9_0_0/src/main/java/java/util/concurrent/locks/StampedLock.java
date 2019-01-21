package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public class StampedLock implements Serializable {
    private static final long ABITS = 255;
    private static final int CANCELLED = 1;
    private static final int HEAD_SPINS = (NCPU > 1 ? 1024 : 0);
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
        int i = 0;
        if (NCPU > 1) {
            i = 65536;
        }
        MAX_HEAD_SPINS = i;
        try {
            STATE = U.objectFieldOffset(StampedLock.class.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset(StampedLock.class.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset(StampedLock.class.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset(WNode.class.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset(WNode.class.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset(WNode.class.getDeclaredField("cowait"));
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    public long writeLock() {
        long j = this.state;
        long s = j;
        if ((j & ABITS) == 0) {
            long j2 = s + WBIT;
            j = j2;
            if (U.compareAndSwapLong(this, STATE, s, j2)) {
                return j;
            }
        }
        return acquireWrite(false, 0);
    }

    public long tryWriteLock() {
        long j = this.state;
        long s = j;
        if ((j & ABITS) == 0) {
            long j2 = s + WBIT;
            j = j2;
            if (U.compareAndSwapLong(this, STATE, s, j2)) {
                return j;
            }
        }
        return 0;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long tryWriteLock = tryWriteLock();
            long next = tryWriteLock;
            if (tryWriteLock != 0) {
                return next;
            }
            if (nanos <= 0) {
                return 0;
            }
            tryWriteLock = System.nanoTime() + nanos;
            long deadline = tryWriteLock;
            if (tryWriteLock == 0) {
                deadline = 1;
            }
            tryWriteLock = acquireWrite(true, deadline);
            next = tryWriteLock;
            if (tryWriteLock != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long writeLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long acquireWrite = acquireWrite(true, 0);
            long next = acquireWrite;
            if (acquireWrite != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long readLock() {
        long s = this.state;
        if (this.whead == this.wtail && (ABITS & s) < RFULL) {
            long j = s + 1;
            long j2 = j;
            if (U.compareAndSwapLong(this, STATE, s, j)) {
                return j2;
            }
        }
        return acquireRead(false, 0);
    }

    public long tryReadLock() {
        while (true) {
            long j = this.state;
            long s = j;
            j &= ABITS;
            long m = j;
            if (j == WBIT) {
                return 0;
            }
            if (m < RFULL) {
                long j2 = s + 1;
                j = j2;
                if (U.compareAndSwapLong(this, STATE, s, j2)) {
                    return j;
                }
            } else {
                long tryIncReaderOverflow = tryIncReaderOverflow(s);
                long next = tryIncReaderOverflow;
                if (tryIncReaderOverflow != 0) {
                    return next;
                }
            }
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next;
            long j = this.state;
            long s = j;
            j &= ABITS;
            long m = j;
            if (j != WBIT) {
                if (m < RFULL) {
                    long j2 = s + 1;
                    long next2 = j2;
                    if (U.compareAndSwapLong(this, STATE, s, j2)) {
                        return next2;
                    }
                }
                j = tryIncReaderOverflow(s);
                next = j;
                if (j != 0) {
                    return next;
                }
            }
            if (nanos <= 0) {
                return 0;
            }
            j = System.nanoTime() + nanos;
            next = j;
            if (j == 0) {
                next = 1;
            }
            j = acquireRead(true, next);
            long next3 = j;
            if (j != 1) {
                return next3;
            }
        }
        throw new InterruptedException();
    }

    public long readLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long acquireRead = acquireRead(true, 0);
            long next = acquireRead;
            if (acquireRead != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long tryOptimisticRead() {
        long j = this.state;
        return (j & WBIT) == 0 ? j & SBITS : 0;
    }

    public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (SBITS & this.state);
    }

    public void unlockWrite(long stamp) {
        if (this.state != stamp || (stamp & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        long j = WBIT + stamp;
        U.putLongVolatile(this, STATE, j == 0 ? ORIGIN : j);
        WNode wNode = this.whead;
        WNode h = wNode;
        if (wNode != null && h.status != 0) {
            release(h);
        }
    }

    public void unlockRead(long stamp) {
        while (true) {
            long j = this.state;
            long s = j;
            if ((j & SBITS) != (stamp & SBITS) || (stamp & ABITS) == 0) {
                break;
            }
            j = ABITS & s;
            long m = j;
            if (j == 0 || m == WBIT) {
                break;
            } else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                    if (m == 1) {
                        WNode wNode = this.whead;
                        WNode h = wNode;
                        if (wNode != null && h.status != 0) {
                            release(h);
                            return;
                        }
                        return;
                    }
                    return;
                }
            } else if (tryDecReaderOverflow(s) != 0) {
                return;
            }
        }
        throw new IllegalMonitorStateException();
    }

    /* JADX WARNING: Missing block: B:42:0x0091, code skipped:
            r9 = r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unlock(long stamp) {
        long j = ABITS;
        long a = stamp & ABITS;
        while (true) {
            long a2 = a;
            a = this.state;
            long s = a;
            if ((a & SBITS) != (stamp & SBITS)) {
                break;
            }
            a = s & j;
            long m = a;
            if (a != 0) {
                WNode wNode;
                WNode h;
                if (m != WBIT) {
                    if (a2 != 0) {
                        if (a2 >= WBIT) {
                            j = s;
                            break;
                        }
                        if (m < RFULL) {
                            if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                                if (m == 1) {
                                    wNode = this.whead;
                                    h = wNode;
                                    if (!(wNode == null || h.status == 0)) {
                                        release(h);
                                    }
                                }
                                return;
                            }
                        } else if (tryDecReaderOverflow(s) != 0) {
                            return;
                        }
                        a = a2;
                        j = ABITS;
                    } else {
                        break;
                    }
                } else if (a2 == m) {
                    a = WBIT + s;
                    U.putLongVolatile(this, STATE, a == 0 ? 256 : a);
                    wNode = this.whead;
                    h = wNode;
                    if (!(wNode == null || h.status == 0)) {
                        release(h);
                    }
                    return;
                } else {
                    j = s;
                }
            } else {
                break;
            }
        }
        throw new IllegalMonitorStateException();
    }

    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long j = this.state;
            long s = j;
            if ((j & SBITS) != (stamp & SBITS)) {
                break;
            }
            long j2 = s & ABITS;
            long m = j2;
            long next;
            if (j2 != 0) {
                if (m != WBIT) {
                    if (m != 1 || a == 0) {
                        break;
                    }
                    long j3 = (s - 1) + WBIT;
                    next = j3;
                    if (U.compareAndSwapLong(this, STATE, s, j3)) {
                        return next;
                    }
                } else if (a != m) {
                    return 0;
                } else {
                    return stamp;
                }
            } else if (a != 0) {
                break;
            } else {
                long j4 = s + WBIT;
                next = j4;
                if (U.compareAndSwapLong(this, STATE, s, j4)) {
                    return next;
                }
            }
        }
        return 0;
    }

    public long tryConvertToReadLock(long stamp) {
        long j = ABITS;
        long a = stamp & ABITS;
        while (true) {
            long a2 = a;
            a = this.state;
            long s = a;
            if ((a & SBITS) != (stamp & SBITS)) {
                break;
            }
            long j2 = s & j;
            long m = j2;
            if (j2 != 0) {
                j = s;
                if (m == WBIT) {
                    if (a2 == m) {
                        long j3 = 129 + j;
                        s = j3;
                        U.putLongVolatile(this, STATE, j3);
                        WNode wNode = this.whead;
                        WNode h = wNode;
                        if (!(wNode == null || h.status == 0)) {
                            release(h);
                        }
                        return s;
                    }
                } else if (a2 == 0 || a2 >= WBIT) {
                    return 0;
                } else {
                    return stamp;
                }
            } else if (a2 != 0) {
                j = s;
                break;
            } else {
                if (m < RFULL) {
                    long j4 = s + 1;
                    long next = j4;
                    if (U.compareAndSwapLong(this, STATE, s, j4)) {
                        return next;
                    }
                } else {
                    j2 = tryIncReaderOverflow(s);
                    long next2 = j2;
                    if (j2 != 0) {
                        return next2;
                    }
                }
                a = a2;
                j = ABITS;
            }
        }
        return 0;
    }

    public long tryConvertToOptimisticRead(long stamp) {
        long s = ABITS;
        long a = stamp & ABITS;
        U.loadFence();
        while (true) {
            long j = this.state;
            long s2 = j;
            if ((j & SBITS) == (stamp & SBITS)) {
                long j2 = s2 & s;
                long m = j2;
                if (j2 != 0) {
                    WNode wNode;
                    WNode h;
                    if (m != WBIT) {
                        if (a == 0) {
                            break;
                        } else if (a >= WBIT) {
                            break;
                        } else {
                            if (m < RFULL) {
                                long j3 = s2 - 1;
                                long next = j3;
                                s = s2;
                                if (U.compareAndSwapLong(this, STATE, s2, j3)) {
                                    if (m == 1) {
                                        wNode = this.whead;
                                        h = wNode;
                                        if (!(wNode == null || h.status == 0)) {
                                            release(h);
                                        }
                                    }
                                    return next & SBITS;
                                }
                            } else {
                                j2 = tryDecReaderOverflow(s2);
                                long next2 = j2;
                                if (j2 != 0) {
                                    return next2 & SBITS;
                                }
                            }
                            s = ABITS;
                        }
                    } else if (a == m) {
                        Unsafe unsafe = U;
                        s = STATE;
                        j2 = WBIT + s2;
                        long j4 = j2 == 0 ? ORIGIN : j2;
                        long next3 = j4;
                        unsafe.putLongVolatile(this, s, j4);
                        wNode = this.whead;
                        h = wNode;
                        if (!(wNode == null || h.status == 0)) {
                            release(h);
                        }
                        return next3;
                    }
                } else if (a == 0) {
                    return s2;
                }
            } else {
                break;
            }
        }
        return 0;
    }

    public boolean tryUnlockWrite() {
        long j = this.state;
        long s = j;
        if ((j & WBIT) == 0) {
            return false;
        }
        long j2 = WBIT + s;
        U.putLongVolatile(this, STATE, j2 == 0 ? ORIGIN : j2);
        WNode wNode = this.whead;
        WNode h = wNode;
        if (!(wNode == null || h.status == 0)) {
            release(h);
        }
        return true;
    }

    public boolean tryUnlockRead() {
        while (true) {
            long j = this.state;
            long s = j;
            j &= ABITS;
            long m = j;
            if (j != 0 && m < WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        if (m == 1) {
                            WNode wNode = this.whead;
                            WNode h = wNode;
                            if (!(wNode == null || h.status == 0)) {
                                release(h);
                            }
                        }
                        return true;
                    }
                } else if (tryDecReaderOverflow(s) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getReadLockCount(long s) {
        long j = RBITS & s;
        long readers = j;
        if (j >= RFULL) {
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        if ((ABITS & s) == 0) {
            str = "[Unlocked]";
        } else if ((WBIT & s) != 0) {
            str = "[Write-locked]";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[Read-locks:");
            stringBuilder2.append(getReadLockCount(s));
            stringBuilder2.append("]");
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        return stringBuilder.toString();
    }

    public Lock asReadLock() {
        ReadLockView readLockView = this.readLockView;
        ReadLockView v = readLockView;
        if (readLockView != null) {
            return v;
        }
        readLockView = new ReadLockView();
        this.readLockView = readLockView;
        return readLockView;
    }

    public Lock asWriteLock() {
        WriteLockView writeLockView = this.writeLockView;
        WriteLockView v = writeLockView;
        if (writeLockView != null) {
            return v;
        }
        writeLockView = new WriteLockView();
        this.writeLockView = writeLockView;
        return writeLockView;
    }

    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView readWriteLockView = this.readWriteLockView;
        ReadWriteLockView v = readWriteLockView;
        if (readWriteLockView != null) {
            return v;
        }
        readWriteLockView = new ReadWriteLockView();
        this.readWriteLockView = readWriteLockView;
        return readWriteLockView;
    }

    final void unstampedUnlockWrite() {
        long j = this.state;
        long s = j;
        if ((j & WBIT) != 0) {
            long j2 = WBIT + s;
            U.putLongVolatile(this, STATE, j2 == 0 ? ORIGIN : j2);
            WNode wNode = this.whead;
            WNode h = wNode;
            if (wNode != null && h.status != 0) {
                release(h);
                return;
            }
            return;
        }
        throw new IllegalMonitorStateException();
    }

    final void unstampedUnlockRead() {
        while (true) {
            long j = this.state;
            long s = j;
            j &= ABITS;
            long m = j;
            if (j != 0 && m < WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        if (m == 1) {
                            WNode wNode = this.whead;
                            WNode h = wNode;
                            if (wNode != null && h.status != 0) {
                                release(h);
                                return;
                            }
                            return;
                        }
                        return;
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
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                long next;
                int i = this.readerOverflow;
                int r = i;
                if (i > 0) {
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
            WNode wNode = h.next;
            WNode q = wNode;
            if (wNode == null || q.status == 1) {
                wNode = this.wtail;
                while (wNode != null && wNode != h) {
                    if (wNode.status <= 0) {
                        q = wNode;
                    }
                    wNode = wNode.prev;
                }
            }
            if (q != null) {
                Thread thread = q.thread;
                Thread w = thread;
                if (thread != null) {
                    U.unpark(w);
                }
            }
        }
    }

    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null;
        int spins = -1;
        while (true) {
            WNode node2;
            int spins2 = spins;
            long j = this.state;
            long s = j;
            j &= ABITS;
            long m = j;
            if (j == 0) {
                long j2 = s + WBIT;
                long ns = j2;
                if (U.compareAndSwapLong(this, STATE, s, j2)) {
                    return ns;
                }
            }
            int i = 0;
            if (spins2 < 0) {
                if (m == WBIT && this.wtail == this.whead) {
                    i = SPINS;
                }
                spins = i;
            } else {
                if (spins2 <= 0) {
                    WNode wNode = this.wtail;
                    WNode p = wNode;
                    WNode hd;
                    if (wNode == null) {
                        hd = new WNode(1, null);
                        if (U.compareAndSwapObject(this, WHEAD, null, hd)) {
                            this.wtail = hd;
                        }
                    } else if (node == null) {
                        node = new WNode(1, p);
                    } else if (node.prev != p) {
                        node.prev = p;
                    } else {
                        int i2 = 1;
                        WNode wNode2 = null;
                        if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                            p.next = node;
                            boolean wasInterrupted = false;
                            spins2 = p;
                            int spins3 = -1;
                            while (true) {
                                Thread thread;
                                boolean z;
                                Thread thread2;
                                spins = spins3;
                                WNode wNode3 = this.whead;
                                WNode h = wNode3;
                                if (wNode3 == spins2) {
                                    if (spins < 0) {
                                        spins = HEAD_SPINS;
                                    } else if (spins < MAX_HEAD_SPINS) {
                                        spins <<= 1;
                                    }
                                    int spins4 = spins;
                                    spins = spins4;
                                    while (true) {
                                        int k = spins;
                                        j = this.state;
                                        long s2 = j;
                                        if ((j & ABITS) == 0) {
                                            long j3 = s2 + WBIT;
                                            long ns2 = j3;
                                            if (U.compareAndSwapLong(this, STATE, s2, j3)) {
                                                this.whead = node;
                                                node.prev = wNode2;
                                                if (wasInterrupted) {
                                                    Thread.currentThread().interrupt();
                                                }
                                                return ns2;
                                            }
                                        } else if (LockSupport.nextSecondarySeed() >= 0) {
                                            k--;
                                            if (k <= 0) {
                                                spins3 = spins4;
                                                break;
                                            }
                                        } else {
                                            continue;
                                        }
                                        spins = k;
                                    }
                                } else {
                                    if (h != null) {
                                        while (true) {
                                            wNode3 = h.cowait;
                                            WNode c = wNode3;
                                            if (wNode3 == null) {
                                                break;
                                            }
                                            if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait)) {
                                                thread = c.thread;
                                                Thread w = thread;
                                                if (thread != null) {
                                                    U.unpark(w);
                                                }
                                            }
                                        }
                                    }
                                    spins3 = spins;
                                }
                                if (this.whead == h) {
                                    wNode = node.prev;
                                    hd = wNode;
                                    if (wNode == spins2) {
                                        spins = spins2.status;
                                        int ps = spins;
                                        if (spins == 0) {
                                            U.compareAndSwapInt(spins2, WSTATUS, 0, -1);
                                        } else if (ps == 1) {
                                            wNode = spins2.prev;
                                            wNode3 = wNode;
                                            if (wNode != null) {
                                                node.prev = wNode3;
                                                wNode3.next = node;
                                            }
                                        } else {
                                            long time;
                                            if (deadline == 0) {
                                                time = 0;
                                            } else {
                                                j = deadline - System.nanoTime();
                                                time = j;
                                                if (j <= 0) {
                                                    return cancelWaiter(node, node, false);
                                                }
                                            }
                                            z = false;
                                            thread = Thread.currentThread();
                                            WNode node3 = node;
                                            U.putObject(thread, PARKBLOCKER, this);
                                            node2 = node3;
                                            node2.thread = thread;
                                            if (spins2.status < 0 && (!(spins2 == h && (this.state & ABITS) == 0) && this.whead == h && node2.prev == spins2)) {
                                                U.park(false, time);
                                            }
                                            thread2 = null;
                                            node2.thread = null;
                                            U.putObject(thread, PARKBLOCKER, null);
                                            if (Thread.interrupted()) {
                                                if (interruptible) {
                                                    return cancelWaiter(node2, node2, true);
                                                }
                                                wasInterrupted = true;
                                            }
                                        }
                                    } else if (hd != null) {
                                        wNode = hd;
                                        hd.next = node;
                                        spins2 = wNode;
                                    }
                                    thread2 = wNode2;
                                    node2 = node;
                                    z = false;
                                } else {
                                    thread2 = wNode2;
                                    node2 = node;
                                    z = false;
                                }
                                boolean z2 = z;
                                node = node2;
                                Object wNode22 = thread2;
                            }
                        } else {
                            node2 = node;
                            node = node2;
                            spins = spins2;
                        }
                    }
                } else if (LockSupport.nextSecondarySeed() >= 0) {
                    spins2--;
                }
                spins = spins2;
            }
            node2 = node;
            node = node2;
            spins = spins2;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0067  */
    /* JADX WARNING: Removed duplicated region for block: B:255:0x029e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:252:0x029a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x01de A:{LOOP_END, LOOP:3: B:67:0x010c->B:121:0x01de} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0158 A:{SYNTHETIC, EDGE_INSN: B:245:0x0158->B:83:0x0158 ?: BREAK  } */
    /* JADX WARNING: Missing block: B:77:0x0148, code skipped:
            if (r14 == false) goto L_0x0151;
     */
    /* JADX WARNING: Missing block: B:78:0x014a, code skipped:
            java.lang.Thread.currentThread().interrupt();
     */
    /* JADX WARNING: Missing block: B:79:0x0151, code skipped:
            return r26;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long acquireRead(boolean interruptible, long deadline) {
        long ns;
        boolean wasInterrupted = false;
        WNode node = null;
        int spins = -1;
        loop0:
        while (true) {
            int spins2;
            WNode h;
            WNode p;
            long s;
            long m;
            long j;
            WNode p2;
            WNode h2;
            WNode p3;
            long tryIncReaderOverflow;
            boolean wasInterrupted2;
            WNode wNode = this.whead;
            WNode h3 = wNode;
            WNode wNode2 = this.wtail;
            WNode p4 = wNode2;
            long j2 = 1;
            if (wNode == wNode2) {
                spins2 = spins;
                h = h3;
                p = p4;
                while (true) {
                    spins = this.state;
                    s = spins;
                    spins &= 255;
                    m = spins;
                    long s2;
                    if (spins < 126) {
                        j = s + j2;
                        ns = j;
                        s2 = s;
                        p2 = p;
                        h2 = h;
                        if (U.compareAndSwapLong(this, STATE, s, j) != 0) {
                            spins = s2;
                            break loop0;
                        }
                        spins = s2;
                        if (m >= WBIT) {
                            if (spins2 <= 0) {
                                if (spins2 == 0) {
                                    h3 = this.whead;
                                    wNode2 = this.wtail;
                                    if (h3 == h2 && wNode2 == p2) {
                                        break;
                                    }
                                    p4 = h3;
                                    p3 = wNode2;
                                    if (h3 != wNode2) {
                                        h2 = p4;
                                        p2 = p3;
                                        break;
                                    }
                                    h = p4;
                                    p = p3;
                                } else {
                                    p = p2;
                                    h = h2;
                                }
                                spins2 = SPINS;
                                j2 = 1;
                            } else if (LockSupport.nextSecondarySeed() >= 0) {
                                spins2--;
                            }
                        }
                        p = p2;
                        h = h2;
                        j2 = 1;
                    } else {
                        s2 = s;
                        p2 = p;
                        h2 = h;
                        if (m < WBIT) {
                            tryIncReaderOverflow = tryIncReaderOverflow(s2);
                            ns = tryIncReaderOverflow;
                            if (tryIncReaderOverflow != 0) {
                                break loop0;
                            }
                        }
                        spins = s2;
                        if (m >= WBIT) {
                        }
                        p = p2;
                        h = h2;
                        j2 = 1;
                    }
                }
            } else {
                spins2 = spins;
                h2 = h3;
                p2 = p4;
            }
            WNode wNode3 = null;
            if (p2 == null) {
                p = new WNode(1, null);
                if (U.compareAndSwapObject(this, WHEAD, null, p)) {
                    this.wtail = p;
                }
            } else {
                long j3;
                Thread thread;
                Thread w;
                boolean z;
                Object obj;
                if (node == null) {
                    node = new WNode(0, p2);
                } else if (h2 == p2 || p2.mode != 0) {
                    boolean z2 = true;
                    h = null;
                    if (node.prev != p2) {
                        node.prev = p2;
                    } else {
                        wasInterrupted2 = wasInterrupted;
                        wasInterrupted = z2;
                        if (U.compareAndSwapObject(this, WTAIL, p2, node)) {
                            long ns2;
                            WNode wNode4;
                            long s3;
                            p2.next = node;
                            int spins3 = -1;
                            loop4:
                            while (true) {
                                boolean z3;
                                Thread thread2;
                                Object obj2;
                                Object obj3;
                                spins = spins3;
                                wNode = this.whead;
                                h2 = wNode;
                                if (wNode == p2) {
                                    if (spins < 0) {
                                        spins = HEAD_SPINS;
                                    } else if (spins < MAX_HEAD_SPINS) {
                                        spins <<= 1;
                                    }
                                    int spins4 = spins;
                                    spins = spins4;
                                    while (true) {
                                        spins2 = spins;
                                        j3 = this.state;
                                        s = j3;
                                        j3 &= ABITS;
                                        m = j3;
                                        long s4;
                                        if (j3 < RFULL) {
                                            j = s + 1;
                                            ns2 = j;
                                            s4 = s;
                                            wNode4 = h;
                                            if (U.compareAndSwapLong(this, STATE, s, j)) {
                                                s3 = s4;
                                                break loop4;
                                            }
                                            j = s4;
                                            if (m >= WBIT && LockSupport.nextSecondarySeed() >= 0) {
                                                spins2--;
                                                if (spins2 > 0) {
                                                    spins3 = spins4;
                                                    break;
                                                }
                                            }
                                            spins = spins2;
                                            h = wNode4;
                                        } else {
                                            s4 = s;
                                            wNode4 = h;
                                            if (m < WBIT) {
                                                s3 = s4;
                                                j3 = tryIncReaderOverflow(s3);
                                                ns2 = j3;
                                                if (j3 != 0) {
                                                    break loop4;
                                                }
                                            }
                                            spins2--;
                                            if (spins2 > 0) {
                                            }
                                        }
                                    }
                                } else {
                                    wNode4 = h;
                                    if (h2 != null) {
                                        while (true) {
                                            wNode = h2.cowait;
                                            h3 = wNode;
                                            if (wNode == null) {
                                                break;
                                            }
                                            if (U.compareAndSwapObject(h2, WCOWAIT, h3, h3.cowait)) {
                                                thread = h3.thread;
                                                w = thread;
                                                if (thread != null) {
                                                    U.unpark(w);
                                                }
                                            }
                                        }
                                    }
                                    spins3 = spins;
                                }
                                if (this.whead == h2) {
                                    wNode3 = node.prev;
                                    p = wNode3;
                                    if (wNode3 == p2) {
                                        spins = p2.status;
                                        int ps = spins;
                                        if (spins == 0) {
                                            U.compareAndSwapInt(p2, WSTATUS, 0, -1);
                                        } else if (ps == 1) {
                                            wNode3 = p2.prev;
                                            wNode = wNode3;
                                            if (wNode3 != null) {
                                                node.prev = wNode;
                                                wNode.next = node;
                                            }
                                        } else {
                                            if (deadline == 0) {
                                                tryIncReaderOverflow = 0;
                                            } else {
                                                j3 = deadline - System.nanoTime();
                                                tryIncReaderOverflow = j3;
                                                if (j3 <= 0) {
                                                    return cancelWaiter(node, node, false);
                                                }
                                            }
                                            z3 = false;
                                            thread = Thread.currentThread();
                                            WNode h4 = h2;
                                            U.putObject(thread, PARKBLOCKER, this);
                                            node.thread = thread;
                                            if (p2.status < 0) {
                                                p4 = h4;
                                                if ((p2 != p4 || (this.state & ABITS) == WBIT) && this.whead == p4 && node.prev == p2) {
                                                    U.park(false, tryIncReaderOverflow);
                                                }
                                            }
                                            thread2 = null;
                                            node.thread = null;
                                            U.putObject(thread, PARKBLOCKER, null);
                                            if (Thread.interrupted()) {
                                                if (interruptible) {
                                                    return cancelWaiter(node, node, true);
                                                }
                                                obj2 = 1;
                                                wasInterrupted2 = true;
                                                z = z3;
                                                obj3 = obj2;
                                                obj = thread2;
                                            }
                                            obj2 = 1;
                                            z = z3;
                                            obj3 = obj2;
                                            obj = thread2;
                                        }
                                    } else if (p != null) {
                                        wNode3 = p;
                                        p.next = node;
                                        p2 = wNode3;
                                    }
                                }
                                thread2 = wNode4;
                                z3 = false;
                                obj2 = 1;
                                z = z3;
                                obj3 = obj2;
                                obj = thread2;
                            }
                            this.whead = node;
                            node.prev = wNode4;
                            while (true) {
                                wNode3 = node.cowait;
                                wNode4 = wNode3;
                                if (wNode3 == null) {
                                    break;
                                }
                                j = s3;
                                if (U.compareAndSwapObject(node, WCOWAIT, wNode4, wNode4.cowait)) {
                                    Thread thread3 = wNode4.thread;
                                    thread = thread3;
                                    if (thread3 != null) {
                                        U.unpark(thread);
                                    }
                                }
                                s3 = j;
                            }
                            j = s3;
                            if (wasInterrupted2) {
                                Thread.currentThread().interrupt();
                            }
                            return ns2;
                        }
                        wasInterrupted = wasInterrupted2;
                    }
                } else {
                    Unsafe unsafe = U;
                    long j4 = WCOWAIT;
                    p3 = p2.cowait;
                    node.cowait = p3;
                    if (unsafe.compareAndSwapObject(p2, j4, p3, node)) {
                        boolean wasInterrupted3 = wasInterrupted;
                        while (true) {
                            Thread thread4;
                            Object obj4;
                            Thread thread5;
                            Object obj5;
                            wNode = this.whead;
                            wasInterrupted = wNode;
                            if (wNode != null) {
                                wNode = wasInterrupted.cowait;
                                h3 = wNode;
                                if (wNode != null) {
                                    if (U.compareAndSwapObject(wasInterrupted, WCOWAIT, h3, h3.cowait)) {
                                        thread = h3.thread;
                                        w = thread;
                                        if (thread != null) {
                                            U.unpark(w);
                                        }
                                    }
                                }
                            }
                            wNode = p2.prev;
                            h2 = wNode;
                            if (wasInterrupted == wNode || wasInterrupted == p2 || h2 == null) {
                                while (true) {
                                    long j5 = this.state;
                                    s = j5;
                                    j5 &= ABITS;
                                    m = j5;
                                    long s5;
                                    if (j5 < RFULL) {
                                        ns = s + 1;
                                        j = ns;
                                        p = wNode3;
                                        s5 = s;
                                        thread4 = p;
                                        if (U.compareAndSwapLong(this, STATE, s, ns)) {
                                            j3 = s5;
                                            break loop0;
                                        }
                                        j3 = s5;
                                        if (m < WBIT) {
                                            break;
                                        }
                                        obj4 = thread4;
                                    } else {
                                        thread4 = wNode3;
                                        s5 = s;
                                        if (m < WBIT) {
                                            tryIncReaderOverflow = tryIncReaderOverflow(s5);
                                            j = tryIncReaderOverflow;
                                            if (tryIncReaderOverflow != 0) {
                                                break loop0;
                                            }
                                        }
                                        if (m < WBIT) {
                                        }
                                    }
                                }
                            } else {
                                thread4 = wNode3;
                            }
                            if (this.whead != wasInterrupted || p2.prev != h2) {
                                thread5 = thread4;
                                obj5 = 1;
                            } else if (h2 == null || wasInterrupted == p2 || p2.status > 0) {
                                node = null;
                                wasInterrupted = wasInterrupted3;
                            } else {
                                if (deadline == 0) {
                                    j3 = 0;
                                    z = false;
                                } else {
                                    j3 = deadline - System.nanoTime();
                                    tryIncReaderOverflow = j3;
                                    if (j3 <= 0) {
                                        if (wasInterrupted3) {
                                            Thread.currentThread().interrupt();
                                        }
                                        return cancelWaiter(node, p2, false);
                                    }
                                    z = false;
                                    j3 = tryIncReaderOverflow;
                                }
                                Thread wt = Thread.currentThread();
                                U.putObject(wt, PARKBLOCKER, this);
                                node.thread = wt;
                                if ((wasInterrupted != h2 || (this.state & ABITS) == WBIT) && this.whead == wasInterrupted && p2.prev == h2) {
                                    U.park(z, j3);
                                }
                                node.thread = thread4;
                                U.putObject(wt, PARKBLOCKER, thread4);
                                if (Thread.interrupted()) {
                                    thread5 = thread4;
                                    if (interruptible) {
                                        return cancelWaiter(node, p2, true);
                                    }
                                    obj5 = 1;
                                    wasInterrupted3 = true;
                                } else {
                                    thread5 = thread4;
                                    obj5 = 1;
                                }
                            }
                            obj4 = thread5;
                            h2 = wasInterrupted;
                            obj = obj5;
                        }
                        node = null;
                        wasInterrupted = wasInterrupted3;
                    } else {
                        node.cowait = null;
                    }
                }
                spins = spins2;
            }
            wasInterrupted2 = wasInterrupted;
            wasInterrupted = wasInterrupted2;
            spins = spins2;
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return ns;
    }

    /* JADX WARNING: Removed duplicated region for block: B:95:0x0066 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0056  */
    /* JADX WARNING: Missing block: B:38:0x0067, code skipped:
            if (r8 != null) goto L_0x0077;
     */
    /* JADX WARNING: Missing block: B:40:0x006b, code skipped:
            if (r11 != r10.wtail) goto L_0x0077;
     */
    /* JADX WARNING: Missing block: B:41:0x006d, code skipped:
            U.compareAndSwapObject(r10, WTAIL, r11, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        WNode p;
        WNode wNode;
        if (node != null && group != null) {
            WNode q;
            node.status = 1;
            p = group;
            while (true) {
                wNode = p.cowait;
                q = wNode;
                if (wNode == null) {
                    break;
                } else if (q.status == 1) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group;
                } else {
                    p = q;
                }
            }
            if (group == node) {
                Thread thread;
                Thread w;
                for (p = group.cowait; p != null; p = p.cowait) {
                    thread = p.thread;
                    w = thread;
                    if (thread != null) {
                        U.unpark(w);
                    }
                }
                p = node.prev;
                while (p != null) {
                    WNode q2;
                    while (true) {
                        wNode = node.next;
                        WNode succ = wNode;
                        if (wNode != null && succ.status != 1) {
                            q = succ;
                            break;
                        }
                        WNode t = this.wtail;
                        q2 = null;
                        while (true) {
                            wNode = t;
                            if (wNode == null || wNode == node) {
                                if (succ != q2) {
                                    q = succ;
                                    break;
                                }
                                wNode = q2;
                                if (U.compareAndSwapObject(node, WNEXT, succ, q2)) {
                                    q = wNode;
                                    break;
                                }
                            } else {
                                if (wNode.status != 1) {
                                    q2 = wNode;
                                }
                                t = wNode.prev;
                            }
                        }
                        if (succ != q2) {
                        }
                    }
                    if (p.next == node) {
                        U.compareAndSwapObject(p, WNEXT, node, q);
                    }
                    if (q != null) {
                        thread = q.thread;
                        w = thread;
                        if (thread != null) {
                            q.thread = null;
                            U.unpark(w);
                        }
                    }
                    if (p.status != 1) {
                        break;
                    }
                    wNode = p.prev;
                    q2 = wNode;
                    if (wNode == null) {
                        break;
                    }
                    node.prev = q2;
                    U.compareAndSwapObject(q2, WNEXT, p, q);
                    p = q2;
                }
            }
        }
        while (true) {
            p = this.whead;
            wNode = p;
            if (p == null) {
                break;
            }
            p = wNode.next;
            WNode q3 = p;
            if (p == null || q3.status == 1) {
                p = this.wtail;
                while (p != null && p != wNode) {
                    if (p.status <= 0) {
                        q3 = p;
                    }
                    p = p.prev;
                }
            }
            if (wNode == this.whead) {
                if (q3 != null && wNode.status == 0) {
                    long j = this.state;
                    long s = j;
                    if ((j & ABITS) != WBIT && (s == 0 || q3.mode == 0)) {
                        release(wNode);
                    }
                }
            }
        }
        if (interrupted || Thread.interrupted()) {
            return 1;
        }
        return 0;
    }
}
