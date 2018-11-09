package java.util.concurrent;

import sun.misc.Unsafe;

public class Exchanger<V> {
    private static final int ABASE;
    private static final int ASHIFT = 7;
    private static final long BLOCKER;
    private static final long BOUND;
    static final int FULL;
    private static final long MATCH;
    private static final int MMASK = 255;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final Object NULL_ITEM = new Object();
    private static final int SEQ = 256;
    private static final long SLOT;
    private static final int SPINS = 1024;
    private static final Object TIMED_OUT = new Object();
    private static final Unsafe U = Unsafe.getUnsafe();
    private volatile Node[] arena;
    private volatile int bound;
    private final Participant participant = new Participant();
    private volatile Node slot;

    static final class Node {
        int bound;
        int collides;
        int hash;
        int index;
        Object item;
        volatile Object match;
        volatile Thread parked;

        Node() {
        }
    }

    static final class Participant extends ThreadLocal<Node> {
        Participant() {
        }

        public Node initialValue() {
            return new Node();
        }
    }

    static {
        int i;
        if (NCPU >= 510) {
            i = MMASK;
        } else {
            i = NCPU >>> 1;
        }
        FULL = i;
        try {
            BOUND = U.objectFieldOffset(Exchanger.class.getDeclaredField("bound"));
            SLOT = U.objectFieldOffset(Exchanger.class.getDeclaredField("slot"));
            MATCH = U.objectFieldOffset(Node.class.getDeclaredField("match"));
            BLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            int scale = U.arrayIndexScale(Node[].class);
            if (((scale - 1) & scale) != 0 || scale > 128) {
                throw new Error("Unsupported array scale");
            }
            ABASE = U.arrayBaseOffset(Node[].class) + 128;
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Node q;
        Object v;
        Object a = this.arena;
        Node p = (Node) this.participant.get();
        int i = p.index;
        while (true) {
            int i2 = (i << 7) + ABASE;
            long j = (long) i2;
            q = (Node) U.getObjectVolatile(a, (long) i2);
            if (q != null && U.compareAndSwapObject(a, j, q, null)) {
                break;
            }
            int b = this.bound;
            int m = b & MMASK;
            if (i > m || q != null) {
                if (p.bound != b) {
                    p.bound = b;
                    p.collides = 0;
                    i = (i != m || m == 0) ? m : m - 1;
                } else {
                    int c = p.collides;
                    if (c < m || m == FULL || (U.compareAndSwapInt(this, BOUND, b, (b + 256) + 1) ^ 1) != 0) {
                        p.collides = c + 1;
                        i = i == 0 ? m : i - 1;
                    } else {
                        i = m + 1;
                    }
                }
                p.index = i;
            } else {
                p.item = item;
                if (U.compareAndSwapObject(a, j, null, p)) {
                    long end = (timed && m == 0) ? System.nanoTime() + ns : 0;
                    Thread t = Thread.currentThread();
                    int h = p.hash;
                    int spins = 1024;
                    while (true) {
                        v = p.match;
                        if (v == null) {
                            if (spins <= 0) {
                                if (U.getObjectVolatile(a, j) == p) {
                                    if (!t.isInterrupted() && m == 0) {
                                        if (timed) {
                                            ns = end - System.nanoTime();
                                        }
                                        U.putObject(t, BLOCKER, this);
                                        p.parked = t;
                                        if (U.getObjectVolatile(a, j) == p) {
                                            U.park(false, ns);
                                        }
                                        p.parked = null;
                                        U.putObject(t, BLOCKER, null);
                                    }
                                    if (U.getObjectVolatile(a, j) == p && U.compareAndSwapObject(a, j, p, null)) {
                                        break;
                                    }
                                }
                                spins = 1024;
                            } else {
                                h ^= h << 1;
                                h ^= h >>> 3;
                                h ^= h << 10;
                                if (h == 0) {
                                    h = ((int) t.getId()) | 1024;
                                } else if (h < 0) {
                                    spins--;
                                    if ((spins & 511) == 0) {
                                        Thread.yield();
                                    }
                                }
                            }
                        } else {
                            U.putOrderedObject(p, MATCH, null);
                            p.item = null;
                            p.hash = h;
                            return v;
                        }
                    }
                    if (m != 0) {
                        U.compareAndSwapInt(this, BOUND, b, (b + 256) - 1);
                    }
                    p.item = null;
                    p.hash = h;
                    i = p.index >>> 1;
                    p.index = i;
                    if (Thread.interrupted()) {
                        return null;
                    }
                    if (timed && m == 0 && ns <= 0) {
                        return TIMED_OUT;
                    }
                }
                p.item = null;
            }
        }
        v = q.item;
        q.match = item;
        Thread w = q.parked;
        if (w != null) {
            U.unpark(w);
        }
        return v;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final Object slotExchange(Object item, boolean timed, long ns) {
        Node p = (Node) this.participant.get();
        Thread t = Thread.currentThread();
        if (t.isInterrupted()) {
            return null;
        }
        Node q;
        while (true) {
            q = this.slot;
            if (q != null) {
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    break;
                } else if (NCPU > 1 && this.bound == 0 && U.compareAndSwapInt(this, BOUND, 0, 256)) {
                    this.arena = new Node[((FULL + 2) << 7)];
                }
            } else if (this.arena != null) {
                return null;
            } else {
                p.item = item;
                if (U.compareAndSwapObject(this, SLOT, null, p)) {
                    break;
                }
                p.item = null;
            }
        }
        Object v = q.item;
        q.match = item;
        Thread w = q.parked;
        if (w != null) {
            U.unpark(w);
        }
        return v;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V exchange(V x) throws InterruptedException {
        if (x == null) {
            Object item = NULL_ITEM;
        } else {
            V item2 = x;
        }
        if (this.arena == null) {
            Object v = slotExchange(item, false, 0);
        }
        if (!Thread.interrupted()) {
            v = arenaExchange(item, false, 0);
        }
        throw new InterruptedException();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (x == null) {
            Object item = NULL_ITEM;
        } else {
            V item2 = x;
        }
        long ns = unit.toNanos(timeout);
        if (this.arena == null) {
            Object v = slotExchange(item, true, ns);
        }
        if (!Thread.interrupted()) {
            v = arenaExchange(item, true, ns);
        }
        throw new InterruptedException();
    }
}
