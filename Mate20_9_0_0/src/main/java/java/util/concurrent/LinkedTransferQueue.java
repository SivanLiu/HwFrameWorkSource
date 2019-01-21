package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, Serializable {
    private static final int ASYNC = 1;
    private static final int CHAINED_SPINS = 64;
    private static final int FRONT_SPINS = 128;
    private static final long HEAD;
    private static final boolean MP;
    private static final int NOW = 0;
    private static final long SWEEPVOTES;
    static final int SWEEP_THRESHOLD = 32;
    private static final int SYNC = 2;
    private static final long TAIL;
    private static final int TIMED = 3;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = -3223113410248163686L;
    volatile transient Node head;
    private volatile transient int sweepVotes;
    private volatile transient Node tail;

    static final class Node {
        private static final long ITEM;
        private static final long NEXT;
        private static final Unsafe U = Unsafe.getUnsafe();
        private static final long WAITER;
        private static final long serialVersionUID = -3375979862319811754L;
        final boolean isData;
        volatile Object item;
        volatile Node next;
        volatile Thread waiter;

        final boolean casNext(Node cmp, Node val) {
            return U.compareAndSwapObject(this, NEXT, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            return U.compareAndSwapObject(this, ITEM, cmp, val);
        }

        Node(Object item, boolean isData) {
            U.putObject(this, ITEM, item);
            this.isData = isData;
        }

        final void forgetNext() {
            U.putObject(this, NEXT, this);
        }

        final void forgetContents() {
            U.putObject(this, ITEM, this);
            U.putObject(this, WAITER, null);
        }

        final boolean isMatched() {
            Node x = this.item;
            if (x != this) {
                if ((x == null ? true : LinkedTransferQueue.MP) != this.isData) {
                    return LinkedTransferQueue.MP;
                }
            }
            return true;
        }

        final boolean isUnmatchedRequest() {
            return (this.isData || this.item != null) ? LinkedTransferQueue.MP : true;
        }

        final boolean cannotPrecede(boolean haveData) {
            boolean d = this.isData;
            if (d == haveData) {
                return LinkedTransferQueue.MP;
            }
            Node node = this.item;
            Node x = node;
            if (node == this) {
                return LinkedTransferQueue.MP;
            }
            return (x != null ? true : LinkedTransferQueue.MP) == d ? true : LinkedTransferQueue.MP;
        }

        final boolean tryMatchData() {
            Node x = this.item;
            if (x == null || x == this || !casItem(x, null)) {
                return LinkedTransferQueue.MP;
            }
            LockSupport.unpark(this.waiter);
            return true;
        }

        static {
            try {
                ITEM = U.objectFieldOffset(Node.class.getDeclaredField("item"));
                NEXT = U.objectFieldOffset(Node.class.getDeclaredField("next"));
                WAITER = U.objectFieldOffset(Node.class.getDeclaredField("waiter"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    final class Itr implements Iterator<E> {
        private Node lastPred;
        private Node lastRet;
        private E nextItem;
        private Node nextNode;

        private void advance(Node prev) {
            Node node = this.lastRet;
            Node r = node;
            if (node == null || r.isMatched()) {
                node = this.lastPred;
                Node b = node;
                if (node != null && !b.isMatched()) {
                    while (true) {
                        node = b.next;
                        Node s = node;
                        if (node != null && s != b && s.isMatched()) {
                            node = s.next;
                            Node n = node;
                            if (node == null || n == s) {
                                break;
                            }
                            b.casNext(s, n);
                        } else {
                            break;
                        }
                    }
                }
                this.lastPred = null;
            } else {
                this.lastPred = r;
            }
            this.lastRet = prev;
            node = prev;
            while (true) {
                E s2 = node == null ? LinkedTransferQueue.this.head : node.next;
                if (s2 == null) {
                    break;
                } else if (s2 == node) {
                    node = null;
                } else {
                    E item = s2.item;
                    if (!s2.isData) {
                        if (item == null) {
                            break;
                        }
                    } else if (!(item == null || item == s2)) {
                        this.nextItem = item;
                        this.nextNode = s2;
                        return;
                    }
                    if (node == null) {
                        node = s2;
                    } else {
                        E e = s2.next;
                        E n2 = e;
                        if (e == null) {
                            break;
                        } else if (s2 == n2) {
                            node = null;
                        } else {
                            node.casNext(s2, n2);
                        }
                    }
                }
            }
            this.nextNode = null;
            this.nextItem = null;
        }

        Itr() {
            advance(null);
        }

        public final boolean hasNext() {
            return this.nextNode != null ? true : LinkedTransferQueue.MP;
        }

        public final E next() {
            Node p = this.nextNode;
            if (p != null) {
                E e = this.nextItem;
                advance(p);
                return e;
            }
            throw new NoSuchElementException();
        }

        public final void remove() {
            Node lastRet = this.lastRet;
            if (lastRet != null) {
                this.lastRet = null;
                if (lastRet.tryMatchData()) {
                    LinkedTransferQueue.this.unsplice(this.lastPred, lastRet);
                    return;
                }
                return;
            }
            throw new IllegalStateException();
        }
    }

    final class LTQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node current;
        boolean exhausted;

        LTQSpliterator() {
        }

        /* JADX WARNING: Missing block: B:10:0x001e, code skipped:
            if (r3 != null) goto L_0x0020;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Spliterator<E> trySplit() {
            int b = this.batch;
            int n = MAX_BATCH;
            if (b <= 0) {
                n = 1;
            } else if (b < MAX_BATCH) {
                n = b + 1;
            }
            if (!this.exhausted) {
                Node node = this.current;
                Node p = node;
                if (node == null) {
                    node = LinkedTransferQueue.this.firstDataNode();
                    p = node;
                }
                if (p.next != null) {
                    Object[] a = new Object[n];
                    Node p2 = p;
                    int i = 0;
                    do {
                        Node e = p2.item;
                        if (e != p2) {
                            a[i] = e;
                            if (e != null) {
                                i++;
                            }
                        }
                        Node node2 = p2.next;
                        Node p3 = node2;
                        if (p2 == node2) {
                            p2 = LinkedTransferQueue.this.firstDataNode();
                        } else {
                            p2 = p3;
                        }
                        if (p2 == null || i >= n) {
                            this.current = p2;
                        }
                    } while (p2.isData);
                    this.current = p2;
                    if (p2 == null) {
                        this.exhausted = true;
                    }
                    if (i > 0) {
                        this.batch = i;
                        return Spliterators.spliterator(a, 0, i, 4368);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) {
                throw new NullPointerException();
            } else if (!this.exhausted) {
                Node node = this.current;
                Node p = node;
                if (node == null) {
                    node = LinkedTransferQueue.this.firstDataNode();
                    p = node;
                    if (node == null) {
                        return;
                    }
                }
                this.exhausted = true;
                do {
                    node = p.item;
                    if (!(node == null || node == p)) {
                        action.accept(node);
                    }
                    Node node2 = p.next;
                    Node p2 = node2;
                    if (p == node2) {
                        p = LinkedTransferQueue.this.firstDataNode();
                    } else {
                        p = p2;
                    }
                    if (p == null) {
                        return;
                    }
                } while (p.isData);
            }
        }

        /* JADX WARNING: Missing block: B:6:0x0012, code skipped:
            if (r0 != null) goto L_0x0014;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean tryAdvance(Consumer<? super E> action) {
            if (action != null) {
                if (!this.exhausted) {
                    Object e;
                    Node node = this.current;
                    Node p = node;
                    if (node == null) {
                        node = LinkedTransferQueue.this.firstDataNode();
                        p = node;
                    }
                    do {
                        node = p.item;
                        e = node;
                        if (node == p) {
                            e = null;
                        }
                        node = p.next;
                        Node p2 = node;
                        if (p == node) {
                            p = LinkedTransferQueue.this.firstDataNode();
                        } else {
                            p = p2;
                        }
                        if (e != null || p == null) {
                            this.current = p;
                        }
                    } while (p.isData);
                    this.current = p;
                    if (p == null) {
                        this.exhausted = true;
                    }
                    if (e != null) {
                        action.accept(e);
                        return true;
                    }
                }
                return LinkedTransferQueue.MP;
            }
            throw new NullPointerException();
        }

        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        public int characteristics() {
            return 4368;
        }
    }

    static {
        boolean z = true;
        if (Runtime.getRuntime().availableProcessors() <= 1) {
            z = MP;
        }
        MP = z;
        try {
            HEAD = U.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("tail"));
            SWEEPVOTES = U.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("sweepVotes"));
            Class cls = LockSupport.class;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private boolean casTail(Node cmp, Node val) {
        return U.compareAndSwapObject(this, TAIL, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return U.compareAndSwapObject(this, HEAD, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return U.compareAndSwapInt(this, SWEEPVOTES, cmp, val);
    }

    private E xfer(E e, boolean haveData, int how, long nanos) {
        E e2 = e;
        boolean z = haveData;
        int i = how;
        if (z && e2 == null) {
            throw new NullPointerException();
        }
        Node s = null;
        while (true) {
            boolean z2;
            Node node;
            Node p = this.head;
            Node h = p;
            while (true) {
                z2 = true;
                if (p == null) {
                    break;
                }
                Node h2;
                Node n;
                boolean isData = p.isData;
                Node item = p.item;
                if (item != p) {
                    if ((item != null) == isData) {
                        if (isData == z) {
                            break;
                        } else if (p.casItem(item, e2)) {
                            h2 = h;
                            h = p;
                            while (h != h2) {
                                n = h.next;
                                if (this.head == h2) {
                                    if (casHead(h2, n == null ? h : n)) {
                                        h2.forgetNext();
                                        break;
                                    }
                                }
                                node = this.head;
                                h2 = node;
                                if (node == null) {
                                    break;
                                }
                                node = h2.next;
                                h = node;
                                if (node == null || !h.isMatched()) {
                                    break;
                                }
                            }
                            LockSupport.unpark(p.waiter);
                            return item;
                        }
                    }
                }
                h2 = p.next;
                if (p != h2) {
                    n = h2;
                } else {
                    n = this.head;
                    h = n;
                }
                p = n;
            }
            if (i == 0) {
                break;
            }
            if (s == null) {
                s = new Node(e2, z);
            }
            node = s;
            Node pred = tryAppend(node, z);
            if (pred == null) {
                s = node;
            } else if (i != 1) {
                if (i != 3) {
                    z2 = MP;
                }
                return awaitMatch(node, pred, e2, z2, nanos);
            }
        }
        return e2;
    }

    private Node tryAppend(Node s, boolean haveData) {
        Node p = this.tail;
        Node t = p;
        while (true) {
            Node node;
            Node node2 = null;
            if (p == null) {
                node = this.head;
                p = node;
                if (node == null) {
                    if (casHead(null, s)) {
                        return s;
                    }
                }
            }
            if (p.cannotPrecede(haveData)) {
                return null;
            }
            node = p.next;
            Node n = node;
            if (node != null) {
                if (p != t) {
                    node = this.tail;
                    Node u = node;
                    if (t != node) {
                        t = u;
                        node2 = u;
                        p = node2;
                    }
                }
                if (p != n) {
                    node2 = n;
                }
                p = node2;
            } else if (p.casNext(null, s)) {
                if (p != t) {
                    while (true) {
                        if (this.tail != t || !casTail(t, s)) {
                            node2 = this.tail;
                            t = node2;
                            if (node2 == null) {
                                break;
                            }
                            node2 = t.next;
                            s = node2;
                            if (node2 == null) {
                                break;
                            }
                            node2 = s.next;
                            s = node2;
                            if (node2 == null || s == t) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
                return p;
            } else {
                p = p.next;
            }
        }
    }

    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        Node node = s;
        Node node2 = pred;
        E e2 = e;
        long deadline = timed ? System.nanoTime() + nanos : 0;
        Thread w = Thread.currentThread();
        int spins = -1;
        ThreadLocalRandom randomYields = null;
        long nanos2 = nanos;
        while (true) {
            E item = node.item;
            if (item != e2) {
                s.forgetContents();
                return item;
            } else if (w.isInterrupted() || (timed && nanos2 <= 0)) {
                unsplice(node2, node);
                if (node.casItem(e2, node)) {
                    return e2;
                }
            } else if (spins < 0) {
                int spinsFor = spinsFor(node2, node.isData);
                spins = spinsFor;
                if (spinsFor > 0) {
                    randomYields = ThreadLocalRandom.current();
                }
            } else if (spins > 0) {
                spins--;
                if (randomYields.nextInt(64) == 0) {
                    Thread.yield();
                }
            } else if (node.waiter == null) {
                node.waiter = w;
            } else if (timed) {
                nanos2 = deadline - System.nanoTime();
                if (nanos2 > 0) {
                    LockSupport.parkNanos(this, nanos2);
                }
            } else {
                LockSupport.park(this);
            }
        }
    }

    private static int spinsFor(Node pred, boolean haveData) {
        if (MP && pred != null) {
            if (pred.isData != haveData) {
                return 192;
            }
            if (pred.isMatched()) {
                return 128;
            }
            if (pred.waiter == null) {
                return 64;
            }
        }
        return 0;
    }

    final Node succ(Node p) {
        Node next = p.next;
        return p == next ? this.head : next;
    }

    final Node firstDataNode() {
        loop0:
        while (true) {
            Node p = this.head;
            while (p != null) {
                Node item = p.item;
                if (!p.isData) {
                    if (item == null) {
                        break loop0;
                    }
                } else if (!(item == null || item == p)) {
                    return p;
                }
                Node node = p.next;
                Node p2 = node;
                if (p != node) {
                    p = p2;
                }
            }
            break loop0;
        }
        return null;
    }

    private int countOfMode(boolean data) {
        int count;
        loop0:
        while (true) {
            count = 0;
            Node p = this.head;
            while (p != null) {
                if (!p.isMatched()) {
                    if (p.isData == data) {
                        count++;
                        if (count == Integer.MAX_VALUE) {
                            break loop0;
                        }
                    }
                    return 0;
                }
                Node node = p.next;
                Node p2 = node;
                if (p != node) {
                    p = p2;
                }
            }
            break loop0;
        }
        return count;
    }

    public String toString() {
        int charLength;
        int size;
        String[] a = null;
        loop0:
        while (true) {
            charLength = 0;
            size = 0;
            Node p = this.head;
            while (p != null) {
                Node item = p.item;
                if (!p.isData) {
                    if (item == null) {
                        break loop0;
                    }
                } else if (!(item == null || item == p)) {
                    if (a == null) {
                        a = new String[4];
                    } else if (size == a.length) {
                        a = (String[]) Arrays.copyOf((Object[]) a, 2 * size);
                    }
                    String s = item.toString();
                    int size2 = size + 1;
                    a[size] = s;
                    charLength += s.length();
                    size = size2;
                }
                Node node = p.next;
                Node p2 = node;
                if (p != node) {
                    p = p2;
                }
            }
            break loop0;
        }
        if (size == 0) {
            return "[]";
        }
        return Helpers.toString(a, size, charLength);
    }

    private Object[] toArrayInternal(Object[] a) {
        int size;
        Object[] x = a;
        loop0:
        while (true) {
            size = 0;
            Node p = this.head;
            while (p != null) {
                Node item = p.item;
                if (!p.isData) {
                    if (item == null) {
                        break loop0;
                    }
                } else if (!(item == null || item == p)) {
                    if (x == null) {
                        x = new Object[4];
                    } else if (size == x.length) {
                        x = Arrays.copyOf(x, 2 * (size + 4));
                    }
                    int size2 = size + 1;
                    x[size] = item;
                    size = size2;
                }
                Node node = p.next;
                Node p2 = node;
                if (p != node) {
                    p = p2;
                }
            }
            break loop0;
        }
        if (x == null) {
            return new Object[0];
        }
        if (a == null || size > a.length) {
            return size == x.length ? x : Arrays.copyOf(x, size);
        }
        if (a != x) {
            System.arraycopy((Object) x, 0, (Object) a, 0, size);
        }
        if (size < a.length) {
            a[size] = null;
        }
        return a;
    }

    public Object[] toArray() {
        return toArrayInternal(null);
    }

    public <T> T[] toArray(T[] a) {
        if (a != null) {
            return toArrayInternal(a);
        }
        throw new NullPointerException();
    }

    public Spliterator<E> spliterator() {
        return new LTQSpliterator();
    }

    final void unsplice(Node pred, Node s) {
        s.waiter = null;
        if (pred != null && pred != s && pred.next == s) {
            Node n = s.next;
            if (n == null || (n != s && pred.casNext(s, n) && pred.isMatched())) {
                while (true) {
                    Node h = this.head;
                    if (h != pred && h != s && h != null) {
                        if (h.isMatched()) {
                            Node hn = h.next;
                            if (hn != null) {
                                if (hn != h && casHead(h, hn)) {
                                    h.forgetNext();
                                }
                            } else {
                                return;
                            }
                        } else if (pred.next != pred && s.next != s) {
                            while (true) {
                                h = this.sweepVotes;
                                if (h < 32) {
                                    if (casSweepVotes(h, h + 1)) {
                                        break;
                                    }
                                } else if (casSweepVotes(h, 0)) {
                                    sweep();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void sweep() {
        Node p = this.head;
        while (p != null) {
            Node node = p.next;
            Node s = node;
            if (node == null) {
                return;
            }
            if (s.isMatched()) {
                node = s.next;
                Node n = node;
                if (node != null) {
                    if (s == n) {
                        p = this.head;
                    } else {
                        p.casNext(s, n);
                    }
                } else {
                    return;
                }
            }
            p = s;
        }
    }

    private boolean findAndRemove(Object e) {
        if (e != null) {
            Node pred = null;
            Node p = this.head;
            while (p != null) {
                Node item = p.item;
                if (!p.isData) {
                    if (item == null) {
                        break;
                    }
                } else if (item != null && item != p && e.equals(item) && p.tryMatchData()) {
                    unsplice(pred, p);
                    return true;
                }
                pred = p;
                Node node = p.next;
                p = node;
                if (node == pred) {
                    pred = null;
                    p = this.head;
                }
            }
        }
        return MP;
    }

    public LinkedTransferQueue(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    public void put(E e) {
        xfer(e, true, 1, 0);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, 1, 0);
        return true;
    }

    public boolean offer(E e) {
        xfer(e, true, 1, 0);
        return true;
    }

    public boolean add(E e) {
        xfer(e, true, 1, 0);
        return true;
    }

    public boolean tryTransfer(E e) {
        return xfer(e, true, 0, 0) == null ? true : MP;
    }

    public void transfer(E e) throws InterruptedException {
        if (xfer(e, true, 2, 0) != null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    public boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (xfer(e, true, 3, unit.toNanos(timeout)) == null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return MP;
        }
        throw new InterruptedException();
    }

    public E take() throws InterruptedException {
        E e = xfer(null, MP, 2, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, MP, 3, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    public E poll() {
        return xfer(null, MP, 0, 0);
    }

    public int drainTo(Collection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        } else if (c != this) {
            int n = 0;
            while (true) {
                E poll = poll();
                E e = poll;
                if (poll == null) {
                    return n;
                }
                c.add(e);
                n++;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        } else if (c != this) {
            int n = 0;
            while (n < maxElements) {
                E poll = poll();
                E e = poll;
                if (poll == null) {
                    break;
                }
                c.add(e);
                n++;
            }
            return n;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public E peek() {
        loop0:
        while (true) {
            E p = this.head;
            while (p != null) {
                E item = p.item;
                if (!p.isData) {
                    if (item == null) {
                        break loop0;
                    }
                } else if (!(item == null || item == p)) {
                    return item;
                }
                E e = p.next;
                E p2 = e;
                if (p != e) {
                    p = p2;
                }
            }
            break loop0;
        }
        return null;
    }

    public boolean isEmpty() {
        return firstDataNode() == null ? true : MP;
    }

    public boolean hasWaitingConsumer() {
        loop0:
        while (true) {
            Node p = this.head;
            while (p != null) {
                Node item = p.item;
                if (p.isData) {
                    if (!(item == null || item == p)) {
                        break loop0;
                    }
                } else if (item == null) {
                    return true;
                }
                Node node = p.next;
                Node p2 = node;
                if (p != node) {
                    p = p2;
                }
            }
            break loop0;
        }
        return MP;
    }

    public int size() {
        return countOfMode(true);
    }

    public int getWaitingConsumerCount() {
        return countOfMode(MP);
    }

    public boolean remove(Object o) {
        return findAndRemove(o);
    }

    public boolean contains(Object o) {
        if (o != null) {
            Node p = this.head;
            while (p != null) {
                Node item = p.item;
                if (p.isData) {
                    if (!(item == null || item == p || !o.equals(item))) {
                        return true;
                    }
                } else if (item == null) {
                    break;
                }
                p = succ(p);
            }
        }
        return MP;
    }

    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        Iterator it = iterator();
        while (it.hasNext()) {
            s.writeObject(it.next());
        }
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        while (true) {
            E item = s.readObject();
            if (item != null) {
                offer(item);
            } else {
                return;
            }
        }
    }
}
