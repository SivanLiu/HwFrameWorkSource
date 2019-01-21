package java.util.concurrent;

import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import sun.misc.Unsafe;

public class Phaser {
    private static final long COUNTS_MASK = 4294967295L;
    private static final int EMPTY = 1;
    private static final int MAX_PARTIES = 65535;
    private static final int MAX_PHASE = Integer.MAX_VALUE;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final int ONE_ARRIVAL = 1;
    private static final int ONE_DEREGISTER = 65537;
    private static final int ONE_PARTY = 65536;
    private static final long PARTIES_MASK = 4294901760L;
    private static final int PARTIES_SHIFT = 16;
    private static final int PHASE_SHIFT = 32;
    static final int SPINS_PER_ARRIVAL = (NCPU < 2 ? 1 : 256);
    private static final long STATE;
    private static final long TERMINATION_BIT = Long.MIN_VALUE;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final int UNARRIVED_MASK = 65535;
    private final AtomicReference<QNode> evenQ;
    private final AtomicReference<QNode> oddQ;
    private final Phaser parent;
    private final Phaser root;
    private volatile long state;

    static final class QNode implements ManagedBlocker {
        final long deadline;
        final boolean interruptible;
        long nanos;
        QNode next;
        final int phase;
        final Phaser phaser;
        volatile Thread thread;
        final boolean timed;
        boolean wasInterrupted;

        QNode(Phaser phaser, int phase, boolean interruptible, boolean timed, long nanos) {
            this.phaser = phaser;
            this.phase = phase;
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.timed = timed;
            this.deadline = timed ? System.nanoTime() + nanos : Phaser.STATE;
            this.thread = Thread.currentThread();
        }

        /* JADX WARNING: Missing block: B:21:0x003e, code skipped:
            if (r4 <= java.util.concurrent.Phaser.STATE) goto L_0x0040;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean isReleasable() {
            if (this.thread == null) {
                return true;
            }
            if (this.phaser.getPhase() != this.phase) {
                this.thread = null;
                return true;
            }
            if (Thread.interrupted()) {
                this.wasInterrupted = true;
            }
            if (this.wasInterrupted && this.interruptible) {
                this.thread = null;
                return true;
            }
            if (this.timed) {
                if (this.nanos > Phaser.STATE) {
                    long nanoTime = this.deadline - System.nanoTime();
                    this.nanos = nanoTime;
                }
                this.thread = null;
                return true;
            }
            return false;
        }

        public boolean block() {
            while (!isReleasable()) {
                if (this.timed) {
                    LockSupport.parkNanos(this, this.nanos);
                } else {
                    LockSupport.park(this);
                }
            }
            return true;
        }
    }

    private static int unarrivedOf(long s) {
        int counts = (int) s;
        return counts == 1 ? 0 : 65535 & counts;
    }

    private static int partiesOf(long s) {
        return ((int) s) >>> 16;
    }

    private static int phaseOf(long s) {
        return (int) (s >>> 32);
    }

    private static int arrivedOf(long s) {
        int counts = (int) s;
        if (counts == 1) {
            return 0;
        }
        return (counts >>> 16) - (65535 & counts);
    }

    private AtomicReference<QNode> queueFor(int phase) {
        return (phase & 1) == 0 ? this.evenQ : this.oddQ;
    }

    private String badArrive(long s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempted arrival of unregistered party for ");
        stringBuilder.append(stateToString(s));
        return stringBuilder.toString();
    }

    private String badRegister(long s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempt to register more than 65535 parties for ");
        stringBuilder.append(stateToString(s));
        return stringBuilder.toString();
    }

    private int doArrive(int adjust) {
        Phaser root = this.root;
        while (true) {
            Phaser root2 = root;
            long s = root2 == this ? this.state : reconcileState();
            int phase = (int) (s >>> 32);
            if (phase < 0) {
                return phase;
            }
            int counts = (int) s;
            int unarrived = counts == 1 ? 0 : 65535 & counts;
            if (unarrived > 0) {
                long j = s - ((long) adjust);
                long s2 = j;
                if (U.compareAndSwapLong(this, STATE, s, j)) {
                    if (unarrived == 1) {
                        j = s2 & PARTIES_MASK;
                        int nextUnarrived = ((int) j) >>> 16;
                        if (root2 == this) {
                            long n;
                            if (onAdvance(phase, nextUnarrived)) {
                                n = Long.MIN_VALUE | j;
                            } else if (nextUnarrived == 0) {
                                n = 1 | j;
                            } else {
                                n = ((long) nextUnarrived) | j;
                            }
                            U.compareAndSwapLong(this, STATE, s2, n | (((long) ((phase + 1) & Integer.MAX_VALUE)) << 32));
                            releaseWaiters(phase);
                        } else if (nextUnarrived == 0) {
                            phase = this.parent.doArrive(ONE_DEREGISTER);
                            U.compareAndSwapLong(this, STATE, s2, s2 | 1);
                        } else {
                            phase = this.parent.doArrive(1);
                        }
                    }
                    return phase;
                }
                root = root2;
            } else {
                int i = adjust;
                throw new IllegalStateException(badArrive(s));
            }
        }
    }

    /* JADX WARNING: Missing block: B:28:0x0080, code skipped:
            r8 = r13.doRegister(1);
     */
    /* JADX WARNING: Missing block: B:29:0x0081, code skipped:
            if (r8 >= 0) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:33:0x0086, code skipped:
            r10 = r8;
     */
    /* JADX WARNING: Missing block: B:36:0x0095, code skipped:
            if (U.compareAndSwapLong(r9, STATE, r14, (((long) r10) << 32) | r11) != false) goto L_0x00a1;
     */
    /* JADX WARNING: Missing block: B:37:0x0097, code skipped:
            r14 = r9.state;
            r10 = (int) (r9.root.state >>> 32);
     */
    /* JADX WARNING: Missing block: B:46:0x00ae, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:59:?, code skipped:
            return r10;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int doRegister(int registrations) {
        int phase;
        int phase2 = registrations;
        long adjust = (((long) phase2) << 16) | ((long) phase2);
        Phaser parent = this.parent;
        while (true) {
            Phaser parent2 = parent;
            long s = parent2 == null ? this.state : reconcileState();
            int counts = (int) s;
            int unarrived = counts & 65535;
            if (phase2 <= 65535 - (counts >>> 16)) {
                phase = (int) (s >>> 32);
                if (phase < 0) {
                    break;
                }
                if (counts != 1) {
                    if (parent2 == null || reconcileState() == s) {
                        if (unarrived == 0) {
                            this.root.internalAwaitAdvance(phase, null);
                        } else {
                            phase2 = phase;
                            if (U.compareAndSwapLong(this, STATE, s, s + adjust)) {
                                return phase2;
                            }
                        }
                    }
                } else {
                    phase2 = phase;
                    if (parent2 == null) {
                        if (U.compareAndSwapLong(this, STATE, s, (((long) phase2) << 32) | adjust)) {
                            return phase2;
                        }
                    } else {
                        synchronized (this) {
                            try {
                                if (this.state == s) {
                                    break;
                                }
                            } catch (Throwable th) {
                                Throwable th2 = th;
                                phase = phase2;
                                throw th2;
                            }
                        }
                    }
                }
                parent = parent2;
                phase2 = registrations;
            } else {
                throw new IllegalStateException(badRegister(s));
            }
        }
        return phase;
    }

    private long reconcileState() {
        Phaser root = this.root;
        long s = this.state;
        if (root == this) {
            return s;
        }
        long s2 = s;
        while (true) {
            int i = (int) (root.state >>> 32);
            int phase = i;
            if (i == ((int) (s2 >>> 32))) {
                return s2;
            }
            long j;
            Unsafe unsafe = U;
            long j2 = STATE;
            long j3 = ((long) phase) << 32;
            if (phase < 0) {
                j = COUNTS_MASK & s2;
            } else {
                int i2 = ((int) s2) >>> 16;
                j = i2 == 0 ? 1 : (PARTIES_MASK & s2) | ((long) i2);
            }
            j |= j3;
            long s3 = j;
            if (unsafe.compareAndSwapLong(this, j2, s2, j)) {
                return s3;
            }
            s2 = this.state;
        }
    }

    public Phaser() {
        this(null, 0);
    }

    public Phaser(int parties) {
        this(null, parties);
    }

    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    public Phaser(Phaser parent, int parties) {
        if ((parties >>> 16) == 0) {
            long j;
            int phase = 0;
            this.parent = parent;
            if (parent != null) {
                Phaser root = parent.root;
                this.root = root;
                this.evenQ = root.evenQ;
                this.oddQ = root.oddQ;
                if (parties != 0) {
                    phase = parent.doRegister(1);
                }
            } else {
                this.root = this;
                this.evenQ = new AtomicReference();
                this.oddQ = new AtomicReference();
            }
            if (parties == 0) {
                j = 1;
            } else {
                j = ((((long) phase) << 32) | (((long) parties) << 16)) | ((long) parties);
            }
            this.state = j;
            return;
        }
        throw new IllegalArgumentException("Illegal number of parties");
    }

    public int register() {
        return doRegister(1);
    }

    public int bulkRegister(int parties) {
        if (parties < 0) {
            throw new IllegalArgumentException();
        } else if (parties == 0) {
            return getPhase();
        } else {
            return doRegister(parties);
        }
    }

    public int arrive() {
        return doArrive(1);
    }

    public int arriveAndDeregister() {
        return doArrive(ONE_DEREGISTER);
    }

    public int arriveAndAwaitAdvance() {
        Phaser root = this.root;
        while (true) {
            Phaser root2 = root;
            long s = root2 == this ? this.state : reconcileState();
            int phase = (int) (s >>> 32);
            if (phase < 0) {
                return phase;
            }
            int counts = (int) s;
            int unarrived = counts == 1 ? 0 : 65535 & counts;
            if (unarrived > 0) {
                long j = s - 1;
                long s2 = j;
                if (!U.compareAndSwapLong(this, STATE, s, j)) {
                    root = root2;
                } else if (unarrived > 1) {
                    return root2.internalAwaitAdvance(phase, null);
                } else {
                    if (root2 != this) {
                        return this.parent.arriveAndAwaitAdvance();
                    }
                    long n = s2 & PARTIES_MASK;
                    int nextUnarrived = ((int) n) >>> 16;
                    if (onAdvance(phase, nextUnarrived)) {
                        n |= Long.MIN_VALUE;
                    } else if (nextUnarrived == 0) {
                        n |= 1;
                    } else {
                        n |= (long) nextUnarrived;
                    }
                    int nextPhase = (phase + 1) & Integer.MAX_VALUE;
                    if (!U.compareAndSwapLong(this, STATE, s2, n | (((long) nextPhase) << 32))) {
                        return (int) (this.state >>> 32);
                    }
                    releaseWaiters(phase);
                    return nextPhase;
                }
            }
            throw new IllegalStateException(badArrive(s));
        }
    }

    public int awaitAdvance(int phase) {
        Phaser root = this.root;
        int p = (int) ((root == this ? this.state : reconcileState()) >>> 32);
        if (phase < 0) {
            return phase;
        }
        if (p == phase) {
            return root.internalAwaitAdvance(phase, null);
        }
        return p;
    }

    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        Phaser root = this.root;
        int p = (int) ((root == this ? this.state : reconcileState()) >>> 32);
        if (phase < 0) {
            return phase;
        }
        if (p == phase) {
            QNode node = new QNode(this, phase, true, false, STATE);
            p = root.internalAwaitAdvance(phase, node);
            if (node.wasInterrupted) {
                throw new InterruptedException();
            }
        }
        return p;
    }

    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        int i = phase;
        long nanos = unit.toNanos(timeout);
        Phaser root = this.root;
        int p = (int) ((root == this ? this.state : reconcileState()) >>> 32);
        if (i < 0) {
            return i;
        }
        int p2;
        if (p == i) {
            QNode node = new QNode(this, i, true, true, nanos);
            p = root.internalAwaitAdvance(i, node);
            if (node.wasInterrupted) {
                throw new InterruptedException();
            } else if (p != i) {
                p2 = p;
            } else {
                throw new TimeoutException();
            }
        }
        p2 = p;
        return p2;
    }

    public void forceTermination() {
        Phaser root = this.root;
        long s;
        do {
            long j = root.state;
            s = j;
            if (j >= STATE) {
            } else {
                return;
            }
        } while (!U.compareAndSwapLong(root, STATE, s, s | Long.MIN_VALUE));
        releaseWaiters(0);
        releaseWaiters(1);
    }

    public final int getPhase() {
        return (int) (this.root.state >>> 32);
    }

    public int getRegisteredParties() {
        return partiesOf(this.state);
    }

    public int getArrivedParties() {
        return arrivedOf(reconcileState());
    }

    public int getUnarrivedParties() {
        return unarrivedOf(reconcileState());
    }

    public Phaser getParent() {
        return this.parent;
    }

    public Phaser getRoot() {
        return this.root;
    }

    public boolean isTerminated() {
        return this.root.state < STATE;
    }

    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    public String toString() {
        return stateToString(reconcileState());
    }

    private String stateToString(long s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("[phase = ");
        stringBuilder.append(phaseOf(s));
        stringBuilder.append(" parties = ");
        stringBuilder.append(partiesOf(s));
        stringBuilder.append(" arrived = ");
        stringBuilder.append(arrivedOf(s));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    private void releaseWaiters(int phase) {
        AtomicReference<QNode> head = (phase & 1) == 0 ? this.evenQ : this.oddQ;
        while (true) {
            QNode qNode = (QNode) head.get();
            QNode q = qNode;
            if (qNode != null && q.phase != ((int) (this.root.state >>> 32))) {
                if (head.compareAndSet(q, q.next)) {
                    Thread thread = q.thread;
                    Thread t = thread;
                    if (thread != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                }
            } else {
                return;
            }
        }
    }

    private int abortWait(int phase) {
        int p;
        AtomicReference<QNode> head = (phase & 1) == 0 ? this.evenQ : this.oddQ;
        while (true) {
            QNode q = (QNode) head.get();
            p = (int) (this.root.state >>> 32);
            if (q == null) {
                break;
            }
            Thread thread = q.thread;
            Thread t = thread;
            if (thread != null && q.phase == p) {
                break;
            } else if (head.compareAndSet(q, q.next) && t != null) {
                q.thread = null;
                LockSupport.unpark(t);
            }
        }
        return p;
    }

    static {
        try {
            STATE = U.objectFieldOffset(Phaser.class.getDeclaredField("state"));
            Class cls = LockSupport.class;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0034, code skipped:
            if (r2 < 0) goto L_0x0036;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int internalAwaitAdvance(int phase, QNode node) {
        int p;
        int unarrived;
        int i = phase;
        releaseWaiters(i - 1);
        int lastUnarrived = 0;
        int spins = SPINS_PER_ARRIVAL;
        QNode node2 = node;
        boolean queued = false;
        while (true) {
            long j = this.state;
            long s = j;
            int i2 = (int) (j >>> 32);
            p = i2;
            if (i2 != i) {
                break;
            } else if (node2 == null) {
                unarrived = ((int) s) & 65535;
                if (unarrived != lastUnarrived) {
                    lastUnarrived = unarrived;
                    if (unarrived < NCPU) {
                        spins += SPINS_PER_ARRIVAL;
                    }
                }
                int lastUnarrived2 = lastUnarrived;
                boolean interrupted = Thread.interrupted();
                if (!interrupted) {
                    spins--;
                }
                int spins2 = spins;
                boolean interrupted2 = interrupted;
                QNode node3 = new QNode(this, i, false, false, 0);
                node3.wasInterrupted = interrupted2;
                node2 = node3;
                spins = spins2;
                lastUnarrived = lastUnarrived2;
            } else if (node2.isReleasable()) {
                break;
            } else if (queued) {
                try {
                    ForkJoinPool.managedBlock(node2);
                } catch (InterruptedException cantHappen) {
                    InterruptedException interruptedException = cantHappen;
                    node2.wasInterrupted = true;
                }
            } else {
                AtomicReference<QNode> head = (i & 1) == 0 ? this.evenQ : this.oddQ;
                QNode q = (QNode) head.get();
                node2.next = q;
                if ((q == null || q.phase == i) && ((int) (this.state >>> 32)) == i) {
                    queued = head.compareAndSet(q, node2);
                }
            }
        }
        if (node2 != null) {
            if (node2.thread != null) {
                node2.thread = null;
            }
            if (node2.wasInterrupted && !node2.interruptible) {
                Thread.currentThread().interrupt();
            }
            if (p == i) {
                unarrived = (int) (this.state >>> 32);
                p = unarrived;
                if (unarrived == i) {
                    return abortWait(phase);
                }
            }
        }
        releaseWaiters(phase);
        return p;
    }
}
