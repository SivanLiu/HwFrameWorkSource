package java.util.concurrent;

import sun.misc.Unsafe;

public class Exchanger<V> {
    private static final int ABASE;
    private static final int ASHIFT = 7;
    private static final long BLOCKER;
    private static final long BOUND;
    static final int FULL = (NCPU >= 510 ? MMASK : NCPU >>> 1);
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
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Object obj = item;
        Object a = this.arena;
        Node p = (Node) this.participant.get();
        int i = p.index;
        long ns2 = ns;
        while (true) {
            long ns3;
            int b;
            Object a2;
            int i2 = i;
            long j = (long) ((i2 << 7) + ABASE);
            long j2 = j;
            Node q = (Node) U.getObjectVolatile(a, j);
            if (q != null) {
                ns3 = ns2;
                b = q;
                if (U.compareAndSwapObject(a, j2, q, null)) {
                    Object v = b.item;
                    b.match = obj;
                    Thread w = b.parked;
                    if (w != null) {
                        U.unpark(w);
                    }
                    return v;
                }
            }
            ns3 = ns2;
            b = q;
            i = this.bound;
            int b2 = i;
            i &= MMASK;
            int m = i;
            int m2;
            Object obj2;
            if (i2 > i || b != 0) {
                m2 = m;
                a2 = a;
                obj2 = b;
                ns2 = b2;
                if (p.bound != ns2) {
                    p.bound = ns2;
                    p.collides = 0;
                    m = (i2 != m2 || m2 == 0) ? m2 : m2 - 1;
                    i = m;
                } else {
                    i = p.collides;
                    int c = i;
                    if (i >= m2 && m2 != FULL) {
                        if (U.compareAndSwapInt(this, BOUND, ns2, (ns2 + 256) + 1)) {
                            i = m2 + 1;
                        }
                    }
                    p.collides = c + 1;
                    i = i2 == 0 ? m2 : i2 - 1;
                }
                p.index = i;
            } else {
                p.item = obj;
                obj = null;
                m2 = m;
                Thread thread = null;
                if (U.compareAndSwapObject(a, j2, null, p)) {
                    long nanoTime = (timed && m2 == 0) ? System.nanoTime() + ns3 : 0;
                    long end = nanoTime;
                    Thread t = Thread.currentThread();
                    int h = p.hash;
                    i = 1024;
                    while (true) {
                        int spins = i;
                        Object v2 = p.match;
                        Node q2;
                        if (v2 != null) {
                            q2 = b;
                            U.putOrderedObject(p, MATCH, thread);
                            p.item = thread;
                            p.hash = h;
                            return v2;
                        }
                        Thread t2;
                        Thread a3;
                        q2 = b;
                        int b3 = b2;
                        int spins2;
                        if (spins > 0) {
                            i = (h << 1) ^ h;
                            i ^= i >>> 3;
                            i ^= i << 10;
                            if (i == 0) {
                                i = 1024 | ((int) t.getId());
                            } else if (i < 0) {
                                spins2 = spins - 1;
                                if ((spins2 & 511) == 0) {
                                    Thread.yield();
                                }
                                h = i;
                                i = spins2;
                                t2 = t;
                                a2 = a;
                            }
                            h = i;
                            t2 = t;
                            a2 = a;
                            i = spins;
                        } else if (U.getObjectVolatile(a, j2) != p) {
                            i = 1024;
                            t2 = t;
                            a2 = a;
                        } else {
                            int h2;
                            Object v3;
                            if (t.isInterrupted() || m2 != 0) {
                                h2 = h;
                                v3 = v2;
                            } else {
                                if (timed) {
                                    nanoTime = end - System.nanoTime();
                                    ns3 = nanoTime;
                                    if (nanoTime <= 0) {
                                        h2 = h;
                                        v3 = v2;
                                    }
                                }
                                nanoTime = ns3;
                                h2 = h;
                                U.putObject(t, BLOCKER, this);
                                p.parked = t;
                                if (U.getObjectVolatile(a, j2) == p) {
                                    U.park(false, nanoTime);
                                }
                                p.parked = thread;
                                U.putObject(t, BLOCKER, thread);
                                ns3 = nanoTime;
                                t2 = t;
                                a2 = a;
                                i = spins;
                                b = b3;
                                h = h2;
                                a3 = thread;
                                thread = a3;
                                b2 = b;
                                b = q2;
                                t = t2;
                                a = a2;
                            }
                            if (U.getObjectVolatile(a, j2) == p) {
                                int h3 = h2;
                                t2 = t;
                                a2 = a;
                                a3 = thread;
                                if (U.compareAndSwapObject(a, j2, p, null)) {
                                    if (m2 != 0) {
                                        b = b3;
                                        U.compareAndSwapInt(this, BOUND, b, (b + 256) - 1);
                                    }
                                    p.item = a3;
                                    p.hash = h3;
                                    spins2 = p.index >>> 1;
                                    p.index = spins2;
                                    if (Thread.interrupted()) {
                                        return a3;
                                    }
                                    if (timed && m2 == 0 && ns <= 0) {
                                        return TIMED_OUT;
                                    }
                                    i = spins2;
                                } else {
                                    b = b3;
                                    i = h3;
                                }
                            } else {
                                t2 = t;
                                a2 = a;
                                b = b3;
                                i = h2;
                                a3 = thread;
                            }
                            h = i;
                            i = spins;
                            thread = a3;
                            b2 = b;
                            b = q2;
                            t = t2;
                            a = a2;
                        }
                        b = b3;
                        a3 = thread;
                        thread = a3;
                        b2 = b;
                        b = q2;
                        t = t2;
                        a = a2;
                    }
                } else {
                    a2 = a;
                    obj2 = b;
                    p.item = null;
                    i = i2;
                }
            }
            ns2 = ns3;
            a = a2;
            obj = item;
        }
    }

    private final Object slotExchange(Object item, boolean timed, long ns) {
        Object obj = item;
        Node p = (Node) this.participant.get();
        Thread t = Thread.currentThread();
        if (t.isInterrupted()) {
            return null;
        }
        while (true) {
            Node node = this.slot;
            Node q = node;
            int spins = 1;
            Object v;
            if (node != null) {
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    v = q.item;
                    q.match = obj;
                    Thread w = q.parked;
                    if (w != null) {
                        U.unpark(w);
                    }
                    return v;
                } else if (NCPU > 1 && this.bound == 0) {
                    if (U.compareAndSwapInt(this, BOUND, 0, 256)) {
                        this.arena = new Node[((FULL + 2) << 7)];
                    }
                }
            } else if (this.arena != null) {
                return null;
            } else {
                p.item = obj;
                if (U.compareAndSwapObject(this, SLOT, null, p)) {
                    Object v2;
                    int h = p.hash;
                    long end = timed ? System.nanoTime() + ns : 0;
                    int h2 = 1024;
                    if (NCPU > 1) {
                        spins = 1024;
                    }
                    long ns2 = ns;
                    int h3 = h;
                    while (true) {
                        v = p.match;
                        v2 = v;
                        if (v != null) {
                            q = h3;
                            break;
                        } else if (spins > 0) {
                            h = (h3 << 1) ^ h3;
                            h ^= h >>> 3;
                            h3 = h ^ (h << 10);
                            if (h3 == 0) {
                                h3 = h2 | ((int) t.getId());
                            } else if (h3 < 0) {
                                spins--;
                                if ((spins & 511) == 0) {
                                    Thread.yield();
                                }
                            }
                        } else if (this.slot != p) {
                            spins = 1024;
                        } else {
                            long ns3;
                            if (t.isInterrupted() || this.arena != null) {
                                ns3 = ns2;
                            } else {
                                if (timed) {
                                    long nanoTime = end - System.nanoTime();
                                    long ns4 = nanoTime;
                                    if (nanoTime > 0) {
                                        ns2 = ns4;
                                    } else {
                                        ns3 = ns4;
                                    }
                                }
                                U.putObject(t, BLOCKER, this);
                                p.parked = t;
                                if (this.slot == p) {
                                    U.park(false, ns2);
                                }
                                p.parked = null;
                                U.putObject(t, BLOCKER, null);
                            }
                            q = h3;
                            if (U.compareAndSwapObject(this, SLOT, p, null)) {
                                v = (!timed || ns3 > 0 || t.isInterrupted()) ? null : TIMED_OUT;
                                v2 = v;
                                ns2 = ns3;
                            } else {
                                h3 = q;
                                ns2 = ns3;
                                h2 = 1024;
                            }
                        }
                    }
                    U.putOrderedObject(p, MATCH, null);
                    p.item = null;
                    p.hash = q;
                    return v2;
                }
                p.item = null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0012, code skipped:
            if (r1 == null) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:10:0x001f, code skipped:
            if (r1 != null) goto L_0x0021;
     */
    /* JADX WARNING: Missing block: B:11:0x0021, code skipped:
            r1 = r5;
     */
    /* JADX WARNING: Missing block: B:12:0x0024, code skipped:
            if (r1 != NULL_ITEM) goto L_0x0028;
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V exchange(V x) throws InterruptedException {
        V slotExchange;
        V v;
        Object item = x == null ? NULL_ITEM : x;
        if (this.arena == null) {
            slotExchange = slotExchange(item, false, 0);
            v = slotExchange;
        }
        if (!Thread.interrupted()) {
            slotExchange = arenaExchange(item, false, 0);
            v = slotExchange;
        }
        throw new InterruptedException();
    }

    /* JADX WARNING: Missing block: B:6:0x0014, code skipped:
            if (r3 == null) goto L_0x0016;
     */
    /* JADX WARNING: Missing block: B:10:0x0021, code skipped:
            if (r3 != null) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:11:0x0023, code skipped:
            r3 = r5;
     */
    /* JADX WARNING: Missing block: B:12:0x0026, code skipped:
            if (r3 == TIMED_OUT) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            if (r3 != NULL_ITEM) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:18:0x0035, code skipped:
            throw new java.util.concurrent.TimeoutException();
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return r3;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        V slotExchange;
        V v;
        Object item = x == null ? NULL_ITEM : x;
        long ns = unit.toNanos(timeout);
        if (this.arena == null) {
            slotExchange = slotExchange(item, true, ns);
            v = slotExchange;
        }
        if (!Thread.interrupted()) {
            slotExchange = arenaExchange(item, true, ns);
            v = slotExchange;
        }
        throw new InterruptedException();
    }
}
