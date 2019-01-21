package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    static final int MAX_TIMED_SPINS = (Runtime.getRuntime().availableProcessors() < 2 ? 0 : 32);
    static final int MAX_UNTIMED_SPINS = (MAX_TIMED_SPINS * 16);
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000;
    private static final long serialVersionUID = -3223113410248163686L;
    private ReentrantLock qlock;
    private volatile transient Transferer<E> transferer;
    private WaitQueue waitingConsumers;
    private WaitQueue waitingProducers;

    static abstract class Transferer<E> {
        abstract E transfer(E e, boolean z, long j);

        Transferer() {
        }
    }

    static final class TransferQueue<E> extends Transferer<E> {
        private static final long CLEANME;
        private static final long HEAD;
        private static final long TAIL;
        private static final Unsafe U = Unsafe.getUnsafe();
        volatile transient QNode cleanMe;
        volatile transient QNode head;
        volatile transient QNode tail;

        static final class QNode {
            private static final long ITEM;
            private static final long NEXT;
            private static final Unsafe U = Unsafe.getUnsafe();
            final boolean isData;
            volatile Object item;
            volatile QNode next;
            volatile Thread waiter;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                if (this.next == cmp) {
                    if (U.compareAndSwapObject(this, NEXT, cmp, val)) {
                        return true;
                    }
                }
                return false;
            }

            boolean casItem(Object cmp, Object val) {
                if (this.item == cmp) {
                    if (U.compareAndSwapObject(this, ITEM, cmp, val)) {
                        return true;
                    }
                }
                return false;
            }

            void tryCancel(Object cmp) {
                U.compareAndSwapObject(this, ITEM, cmp, this);
            }

            boolean isCancelled() {
                return this.item == this;
            }

            boolean isOffList() {
                return this.next == this;
            }

            static {
                try {
                    ITEM = U.objectFieldOffset(QNode.class.getDeclaredField("item"));
                    NEXT = U.objectFieldOffset(QNode.class.getDeclaredField("next"));
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }
        }

        TransferQueue() {
            QNode h = new QNode(null, false);
            this.head = h;
            this.tail = h;
        }

        void advanceHead(QNode h, QNode nh) {
            if (h == this.head) {
                if (U.compareAndSwapObject(this, HEAD, h, nh)) {
                    h.next = h;
                }
            }
        }

        void advanceTail(QNode t, QNode nt) {
            if (this.tail == t) {
                U.compareAndSwapObject(this, TAIL, t, nt);
            }
        }

        boolean casCleanMe(QNode cmp, QNode val) {
            if (this.cleanMe == cmp) {
                if (U.compareAndSwapObject(this, CLEANME, cmp, val)) {
                    return true;
                }
            }
            return false;
        }

        E transfer(E e, boolean timed, long nanos) {
            E e2 = e;
            QNode s = null;
            boolean isData = e2 != null;
            while (true) {
                boolean isData2 = isData;
                QNode t = this.tail;
                QNode h = this.head;
                if (!(t == null || h == null)) {
                    E s2;
                    if (h == t || t.isData == isData2) {
                        QNode tn = t.next;
                        if (t != this.tail) {
                            continue;
                        } else if (tn != null) {
                            advanceTail(t, tn);
                        } else if (timed && nanos <= 0) {
                            return null;
                        } else {
                            E s3;
                            if (s == null) {
                                s3 = new QNode(e2, isData2);
                            }
                            s2 = s3;
                            if (t.casNext(null, s2)) {
                                advanceTail(t, s2);
                                E s4 = s2;
                                s3 = awaitFulfill(s2, e2, timed, nanos);
                                if (s3 == s4) {
                                    clean(t, s4);
                                    return null;
                                }
                                if (!s4.isOffList()) {
                                    advanceHead(t, s4);
                                    if (s3 != null) {
                                        s4.item = s4;
                                    }
                                    s4.waiter = null;
                                }
                                return s3 != null ? s3 : e2;
                            }
                            s = s2;
                        }
                    } else {
                        E m = h.next;
                        if (t == this.tail && m != null && h == this.head) {
                            s2 = m.item;
                            if (isData2 == (s2 != null) || s2 == m || !m.casItem(s2, e2)) {
                                advanceHead(h, m);
                            } else {
                                advanceHead(h, m);
                                LockSupport.unpark(m.waiter);
                                return s2 != null ? s2 : e2;
                            }
                        }
                    }
                }
                isData = isData2;
            }
        }

        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            long deadline = timed ? System.nanoTime() + nanos : 0;
            Thread w = Thread.currentThread();
            int spins = this.head.next == s ? timed ? SynchronousQueue.MAX_TIMED_SPINS : SynchronousQueue.MAX_UNTIMED_SPINS : 0;
            while (true) {
                if (w.isInterrupted()) {
                    s.tryCancel(e);
                }
                E x = s.item;
                if (x != e) {
                    return x;
                }
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0) {
                        s.tryCancel(e);
                    }
                }
                if (spins > 0) {
                    spins--;
                } else if (s.waiter == null) {
                    s.waiter = w;
                } else if (!timed) {
                    LockSupport.park(this);
                } else if (nanos > SynchronousQueue.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:45:0x005a A:{SYNTHETIC} */
        /* JADX WARNING: Missing block: B:34:0x0053, code skipped:
            if (r5.casNext(r6, r8) != false) goto L_0x0055;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        void clean(QNode pred, QNode s) {
            s.waiter = null;
            while (pred.next == s) {
                QNode h = this.head;
                QNode hn = h.next;
                if (hn == null || !hn.isCancelled()) {
                    QNode t = this.tail;
                    if (t != h) {
                        QNode tn = t.next;
                        if (t == this.tail) {
                            if (tn != null) {
                                advanceTail(t, tn);
                            } else {
                                QNode sn;
                                if (s != t) {
                                    sn = s.next;
                                    if (sn == s || pred.casNext(s, sn)) {
                                        return;
                                    }
                                }
                                sn = this.cleanMe;
                                if (sn != null) {
                                    QNode d = sn.next;
                                    if (!(d == null || d == sn || !d.isCancelled())) {
                                        if (d != t) {
                                            QNode qNode = d.next;
                                            QNode dn = qNode;
                                            if (qNode != null) {
                                                if (dn != d) {
                                                }
                                            }
                                        }
                                        if (sn == pred) {
                                            return;
                                        }
                                    }
                                    casCleanMe(sn, null);
                                    if (sn == pred) {
                                    }
                                } else if (casCleanMe(null, pred)) {
                                    return;
                                }
                            }
                        }
                    } else {
                        return;
                    }
                }
                advanceHead(h, hn);
            }
        }

        static {
            try {
                HEAD = U.objectFieldOffset(TransferQueue.class.getDeclaredField("head"));
                TAIL = U.objectFieldOffset(TransferQueue.class.getDeclaredField("tail"));
                CLEANME = U.objectFieldOffset(TransferQueue.class.getDeclaredField("cleanMe"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static final class TransferStack<E> extends Transferer<E> {
        static final int DATA = 1;
        static final int FULFILLING = 2;
        private static final long HEAD;
        static final int REQUEST = 0;
        private static final Unsafe U = Unsafe.getUnsafe();
        volatile SNode head;

        static final class SNode {
            private static final long MATCH;
            private static final long NEXT;
            private static final Unsafe U = Unsafe.getUnsafe();
            Object item;
            volatile SNode match;
            int mode;
            volatile SNode next;
            volatile Thread waiter;

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                if (cmp == this.next) {
                    if (U.compareAndSwapObject(this, NEXT, cmp, val)) {
                        return true;
                    }
                }
                return false;
            }

            boolean tryMatch(SNode s) {
                boolean z = true;
                if (this.match == null) {
                    if (U.compareAndSwapObject(this, MATCH, null, s)) {
                        Thread w = this.waiter;
                        if (w != null) {
                            this.waiter = null;
                            LockSupport.unpark(w);
                        }
                        return true;
                    }
                }
                if (this.match != s) {
                    z = false;
                }
                return z;
            }

            void tryCancel() {
                U.compareAndSwapObject(this, MATCH, null, this);
            }

            boolean isCancelled() {
                return this.match == this;
            }

            static {
                try {
                    MATCH = U.objectFieldOffset(SNode.class.getDeclaredField("match"));
                    NEXT = U.objectFieldOffset(SNode.class.getDeclaredField("next"));
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }
        }

        TransferStack() {
        }

        static boolean isFulfilling(int m) {
            return (m & 2) != 0;
        }

        boolean casHead(SNode h, SNode nh) {
            if (h == this.head) {
                if (U.compareAndSwapObject(this, HEAD, h, nh)) {
                    return true;
                }
            }
            return false;
        }

        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) {
                s = new SNode(e);
            }
            s.mode = mode;
            s.next = next;
            return s;
        }

        E transfer(E e, boolean timed, long nanos) {
            SNode s = null;
            int mode = e == null ? 0 : 1;
            while (true) {
                SNode h = this.head;
                SNode snode;
                SNode sNode;
                if (h == null || h.mode == mode) {
                    if (!timed || nanos > 0) {
                        snode = snode(s, e, h, mode);
                        s = snode;
                        if (casHead(h, snode)) {
                            snode = awaitFulfill(s, timed, nanos);
                            if (snode == s) {
                                clean(s);
                                return null;
                            }
                            sNode = this.head;
                            h = sNode;
                            if (sNode != null && h.next == s) {
                                casHead(h, s.next);
                            }
                            return mode == 0 ? snode.item : s.item;
                        }
                    } else if (h == null || !h.isCancelled()) {
                        return null;
                    } else {
                        casHead(h, h.next);
                    }
                } else if (isFulfilling(h.mode)) {
                    snode = h.next;
                    if (snode == null) {
                        casHead(h, null);
                    } else {
                        sNode = snode.next;
                        if (snode.tryMatch(h)) {
                            casHead(h, sNode);
                        } else {
                            h.casNext(snode, sNode);
                        }
                    }
                } else if (h.isCancelled()) {
                    casHead(h, h.next);
                } else {
                    snode = snode(s, e, h, 2 | mode);
                    s = snode;
                    if (casHead(h, snode)) {
                        while (true) {
                            snode = s.next;
                            if (snode == null) {
                                casHead(s, null);
                                s = null;
                                break;
                            }
                            SNode mn = snode.next;
                            if (snode.tryMatch(s)) {
                                casHead(s, mn);
                                return mode == 0 ? snode.item : s.item;
                            }
                            s.casNext(snode, mn);
                        }
                    } else {
                        continue;
                    }
                }
            }
            return null;
        }

        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            long deadline = timed ? System.nanoTime() + nanos : 0;
            Thread w = Thread.currentThread();
            int spins = shouldSpin(s) ? timed ? SynchronousQueue.MAX_TIMED_SPINS : SynchronousQueue.MAX_UNTIMED_SPINS : 0;
            while (true) {
                if (w.isInterrupted()) {
                    s.tryCancel();
                }
                SNode m = s.match;
                if (m != null) {
                    return m;
                }
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0) {
                        s.tryCancel();
                    }
                }
                if (spins > 0) {
                    spins = shouldSpin(s) ? spins - 1 : 0;
                } else if (s.waiter == null) {
                    s.waiter = w;
                } else if (!timed) {
                    LockSupport.park(this);
                } else if (nanos > SynchronousQueue.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }

        boolean shouldSpin(SNode s) {
            SNode h = this.head;
            return h == s || h == null || isFulfilling(h.mode);
        }

        /* JADX WARNING: Missing block: B:28:?, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        void clean(SNode s) {
            SNode sNode;
            SNode p;
            s.item = null;
            s.waiter = null;
            SNode past = s.next;
            if (past != null && past.isCancelled()) {
                past = past.next;
            }
            while (true) {
                sNode = this.head;
                p = sNode;
                if (sNode == null || p == past || !p.isCancelled()) {
                    while (p != null && p != past) {
                        sNode = p.next;
                        if (sNode == null && sNode.isCancelled()) {
                            p.casNext(sNode, sNode.next);
                        } else {
                            p = sNode;
                        }
                    }
                } else {
                    casHead(p, p.next);
                }
            }
            while (p != null) {
                sNode = p.next;
                if (sNode == null) {
                }
                p = sNode;
            }
        }

        static {
            try {
                HEAD = U.objectFieldOffset(TransferStack.class.getDeclaredField("head"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static class WaitQueue implements Serializable {
        WaitQueue() {
        }
    }

    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;

        FifoWaitQueue() {
        }
    }

    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;

        LifoWaitQueue() {
        }
    }

    static {
        Class cls = LockSupport.class;
    }

    public SynchronousQueue() {
        this(false);
    }

    public SynchronousQueue(boolean fair) {
        this.transferer = fair ? new TransferQueue() : new TransferStack();
    }

    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        } else if (this.transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        } else if (this.transferer.transfer(e, true, unit.toNanos(timeout)) != null) {
            return true;
        } else {
            if (!Thread.interrupted()) {
                return false;
            }
            throw new InterruptedException();
        }
    }

    public boolean offer(E e) {
        if (e != null) {
            return this.transferer.transfer(e, true, 0) != null;
        } else {
            throw new NullPointerException();
        }
    }

    public E take() throws InterruptedException {
        E e = this.transferer.transfer(null, false, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = this.transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    public E poll() {
        return this.transferer.transfer(null, true, 0);
    }

    public boolean isEmpty() {
        return true;
    }

    public int size() {
        return 0;
    }

    public int remainingCapacity() {
        return 0;
    }

    public void clear() {
    }

    public boolean contains(Object o) {
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    public boolean removeAll(Collection<?> collection) {
        return false;
    }

    public boolean retainAll(Collection<?> collection) {
        return false;
    }

    public E peek() {
        return null;
    }

    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }

    public String toString() {
        return "[]";
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

    private void writeObject(ObjectOutputStream s) throws IOException {
        if (this.transferer instanceof TransferQueue) {
            this.qlock = new ReentrantLock(true);
            this.waitingProducers = new FifoWaitQueue();
            this.waitingConsumers = new FifoWaitQueue();
        } else {
            this.qlock = new ReentrantLock();
            this.waitingProducers = new LifoWaitQueue();
            this.waitingConsumers = new LifoWaitQueue();
        }
        s.defaultWriteObject();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (this.waitingProducers instanceof FifoWaitQueue) {
            this.transferer = new TransferQueue();
        } else {
            this.transferer = new TransferStack();
        }
    }
}
