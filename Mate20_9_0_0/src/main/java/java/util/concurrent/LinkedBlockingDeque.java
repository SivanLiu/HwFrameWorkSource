package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class LinkedBlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E>, Serializable {
    private static final long serialVersionUID = -387911632671998426L;
    private final int capacity;
    private transient int count;
    transient Node<E> first;
    transient Node<E> last;
    final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    static final class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(E x) {
            this.item = x;
        }
    }

    private abstract class AbstractItr implements Iterator<E> {
        private Node<E> lastRet;
        Node<E> next;
        E nextItem;

        abstract Node<E> firstNode();

        abstract Node<E> nextNode(Node<E> node);

        AbstractItr() {
            ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                this.next = firstNode();
                this.nextItem = this.next == null ? null : this.next.item;
            } finally {
                lock.unlock();
            }
        }

        private Node<E> succ(Node<E> n) {
            while (true) {
                Node<E> s = nextNode(n);
                if (s == null) {
                    return null;
                }
                if (s.item != null) {
                    return s;
                }
                if (s == n) {
                    return firstNode();
                }
                n = s;
            }
        }

        void advance() {
            ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                this.next = succ(this.next);
                this.nextItem = this.next == null ? null : this.next.item;
            } finally {
                lock.unlock();
            }
        }

        public boolean hasNext() {
            return this.next != null;
        }

        public E next() {
            if (this.next != null) {
                this.lastRet = this.next;
                E x = this.nextItem;
                advance();
                return x;
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            Node<E> n = this.lastRet;
            if (n != null) {
                this.lastRet = null;
                ReentrantLock lock = LinkedBlockingDeque.this.lock;
                lock.lock();
                try {
                    if (n.item != null) {
                        LinkedBlockingDeque.this.unlink(n);
                    }
                    lock.unlock();
                } catch (Throwable th) {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    static final class LBDSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node<E> current;
        long est;
        boolean exhausted;
        final LinkedBlockingDeque<E> queue;

        LBDSpliterator(LinkedBlockingDeque<E> queue) {
            this.queue = queue;
            this.est = (long) queue.size();
        }

        public long estimateSize() {
            return this.est;
        }

        /* JADX WARNING: Removed duplicated region for block: B:31:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:30:0x004f  */
        /* JADX WARNING: Removed duplicated region for block: B:35:0x0062  */
        /* JADX WARNING: Missing block: B:10:0x001c, code skipped:
            if (r4 != null) goto L_0x001e;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Spliterator<E> trySplit() {
            LinkedBlockingDeque<E> q = this.queue;
            int b = this.batch;
            int n = MAX_BATCH;
            if (b <= 0) {
                n = 1;
            } else if (b < MAX_BATCH) {
                n = b + 1;
            }
            if (!this.exhausted) {
                Node<E> node = this.current;
                Node<E> h = node;
                if (node == null) {
                    node = q.first;
                    h = node;
                }
                if (h.next != null) {
                    Node<E> node2;
                    Object[] a = new Object[n];
                    ReentrantLock lock = q.lock;
                    int i = 0;
                    Node<E> p = this.current;
                    lock.lock();
                    if (p == null) {
                        try {
                            node2 = q.first;
                            p = node2;
                            if (node2 != null) {
                            }
                            lock.unlock();
                            this.current = p;
                            if (p != null) {
                                this.est = 0;
                                this.exhausted = true;
                            } else {
                                long j = this.est - ((long) i);
                                this.est = j;
                                if (j < 0) {
                                    this.est = 0;
                                }
                            }
                            if (i > 0) {
                                this.batch = i;
                                return Spliterators.spliterator(a, 0, i, 4368);
                            }
                        } catch (Throwable th) {
                            lock.unlock();
                        }
                    }
                    do {
                        Object obj = p.item;
                        a[i] = obj;
                        if (obj != null) {
                            i++;
                        }
                        node2 = p.next;
                        p = node2;
                        if (node2 == null) {
                            break;
                        }
                    } while (i < n);
                    lock.unlock();
                    this.current = p;
                    if (p != null) {
                    }
                    if (i > 0) {
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            if (action != null) {
                LinkedBlockingDeque<E> q = this.queue;
                ReentrantLock lock = q.lock;
                if (!this.exhausted) {
                    this.exhausted = true;
                    Node<E> p = this.current;
                    do {
                        E e = null;
                        lock.lock();
                        if (p == null) {
                            try {
                                p = q.first;
                            } catch (Throwable th) {
                                lock.unlock();
                            }
                        }
                        while (p != null) {
                            e = p.item;
                            p = p.next;
                            if (e != null) {
                                break;
                            }
                        }
                        lock.unlock();
                        if (e != null) {
                            action.accept(e);
                            continue;
                        }
                    } while (p != null);
                    return;
                }
                return;
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action != null) {
                LinkedBlockingDeque<E> q = this.queue;
                ReentrantLock lock = q.lock;
                if (!this.exhausted) {
                    E e = null;
                    lock.lock();
                    try {
                        if (this.current == null) {
                            this.current = q.first;
                        }
                        while (this.current != null) {
                            e = this.current.item;
                            this.current = this.current.next;
                            if (e != null) {
                                break;
                            }
                        }
                        lock.unlock();
                        if (this.current == null) {
                            this.exhausted = true;
                        }
                        if (e != null) {
                            action.accept(e);
                            return true;
                        }
                    } catch (Throwable th) {
                        lock.unlock();
                    }
                }
                return false;
            }
            throw new NullPointerException();
        }

        public int characteristics() {
            return 4368;
        }
    }

    private class DescendingItr extends AbstractItr {
        private DescendingItr() {
            super();
        }

        Node<E> firstNode() {
            return LinkedBlockingDeque.this.last;
        }

        Node<E> nextNode(Node<E> n) {
            return n.prev;
        }
    }

    private class Itr extends AbstractItr {
        private Itr() {
            super();
        }

        Node<E> firstNode() {
            return LinkedBlockingDeque.this.first;
        }

        Node<E> nextNode(Node<E> n) {
            return n.next;
        }
    }

    public LinkedBlockingDeque() {
        this((int) Integer.MAX_VALUE);
    }

    public LinkedBlockingDeque(int capacity) {
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
        if (capacity > 0) {
            this.capacity = capacity;
            return;
        }
        throw new IllegalArgumentException();
    }

    public LinkedBlockingDeque(Collection<? extends E> c) {
        this((int) Integer.MAX_VALUE);
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                } else if (!linkLast(new Node(e))) {
                    throw new IllegalStateException("Deque full");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean linkFirst(Node<E> node) {
        if (this.count >= this.capacity) {
            return false;
        }
        Node<E> f = this.first;
        node.next = f;
        this.first = node;
        if (this.last == null) {
            this.last = node;
        } else {
            f.prev = node;
        }
        this.count++;
        this.notEmpty.signal();
        return true;
    }

    private boolean linkLast(Node<E> node) {
        if (this.count >= this.capacity) {
            return false;
        }
        Node<E> l = this.last;
        node.prev = l;
        this.last = node;
        if (this.first == null) {
            this.first = node;
        } else {
            l.next = node;
        }
        this.count++;
        this.notEmpty.signal();
        return true;
    }

    private E unlinkFirst() {
        Node<E> f = this.first;
        if (f == null) {
            return null;
        }
        Node<E> n = f.next;
        E item = f.item;
        f.item = null;
        f.next = f;
        this.first = n;
        if (n == null) {
            this.last = null;
        } else {
            n.prev = null;
        }
        this.count--;
        this.notFull.signal();
        return item;
    }

    private E unlinkLast() {
        Node<E> l = this.last;
        if (l == null) {
            return null;
        }
        Node<E> p = l.prev;
        E item = l.item;
        l.item = null;
        l.prev = l;
        this.last = p;
        if (p == null) {
            this.first = null;
        } else {
            p.next = null;
        }
        this.count--;
        this.notFull.signal();
        return item;
    }

    void unlink(Node<E> x) {
        Node<E> p = x.prev;
        Node<E> n = x.next;
        if (p == null) {
            unlinkFirst();
        } else if (n == null) {
            unlinkLast();
        } else {
            p.next = n;
            n.prev = p;
            x.item = null;
            this.count--;
            this.notFull.signal();
        }
    }

    public void addFirst(E e) {
        if (!offerFirst(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    public void addLast(E e) {
        if (!offerLast(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    public boolean offerFirst(E e) {
        if (e != null) {
            Node<E> node = new Node(e);
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                boolean linkFirst = linkFirst(node);
                return linkFirst;
            } finally {
                lock.unlock();
            }
        } else {
            throw new NullPointerException();
        }
    }

    public boolean offerLast(E e) {
        if (e != null) {
            Node<E> node = new Node(e);
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                boolean linkLast = linkLast(node);
                return linkLast;
            } finally {
                lock.unlock();
            }
        } else {
            throw new NullPointerException();
        }
    }

    public void putFirst(E e) throws InterruptedException {
        if (e != null) {
            Node<E> node = new Node(e);
            ReentrantLock lock = this.lock;
            lock.lock();
            while (!linkFirst(node)) {
                try {
                    this.notFull.await();
                } finally {
                    lock.unlock();
                }
            }
            return;
        }
        throw new NullPointerException();
    }

    public void putLast(E e) throws InterruptedException {
        if (e != null) {
            Node<E> node = new Node(e);
            ReentrantLock lock = this.lock;
            lock.lock();
            while (!linkLast(node)) {
                try {
                    this.notFull.await();
                } finally {
                    lock.unlock();
                }
            }
            return;
        }
        throw new NullPointerException();
    }

    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e != null) {
            Node<E> node = new Node(e);
            long nanos = unit.toNanos(timeout);
            ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            while (!linkFirst(node)) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = this.notFull.awaitNanos(nanos);
                } finally {
                    lock.unlock();
                }
            }
            lock.unlock();
            return true;
        }
        throw new NullPointerException();
    }

    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e != null) {
            Node<E> node = new Node(e);
            long nanos = unit.toNanos(timeout);
            ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            while (!linkLast(node)) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = this.notFull.awaitNanos(nanos);
                } finally {
                    lock.unlock();
                }
            }
            lock.unlock();
            return true;
        }
        throw new NullPointerException();
    }

    public E removeFirst() {
        E x = pollFirst();
        if (x != null) {
            return x;
        }
        throw new NoSuchElementException();
    }

    public E removeLast() {
        E x = pollLast();
        if (x != null) {
            return x;
        }
        throw new NoSuchElementException();
    }

    public E pollFirst() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E unlinkFirst = unlinkFirst();
            return unlinkFirst;
        } finally {
            lock.unlock();
        }
    }

    public E pollLast() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E unlinkLast = unlinkLast();
            return unlinkLast;
        } finally {
            lock.unlock();
        }
    }

    public E takeFirst() throws InterruptedException {
        E x;
        ReentrantLock lock = this.lock;
        lock.lock();
        while (true) {
            try {
                E unlinkFirst = unlinkFirst();
                x = unlinkFirst;
                if (unlinkFirst != null) {
                    break;
                }
                this.notEmpty.await();
            } finally {
                lock.unlock();
            }
        }
        return x;
    }

    public E takeLast() throws InterruptedException {
        E x;
        ReentrantLock lock = this.lock;
        lock.lock();
        while (true) {
            try {
                E unlinkLast = unlinkLast();
                x = unlinkLast;
                if (unlinkLast != null) {
                    break;
                }
                this.notEmpty.await();
            } finally {
                lock.unlock();
            }
        }
        return x;
    }

    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (true) {
            try {
                E unlinkFirst = unlinkFirst();
                E x = unlinkFirst;
                if (unlinkFirst != null) {
                    lock.unlock();
                    return x;
                } else if (nanos <= 0) {
                    break;
                } else {
                    nanos = this.notEmpty.awaitNanos(nanos);
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (true) {
            try {
                E unlinkLast = unlinkLast();
                E x = unlinkLast;
                if (unlinkLast != null) {
                    lock.unlock();
                    return x;
                } else if (nanos <= 0) {
                    break;
                } else {
                    nanos = this.notEmpty.awaitNanos(nanos);
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    public E getFirst() {
        E x = peekFirst();
        if (x != null) {
            return x;
        }
        throw new NoSuchElementException();
    }

    public E getLast() {
        E x = peekLast();
        if (x != null) {
            return x;
        }
        throw new NoSuchElementException();
    }

    public E peekFirst() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E e = this.first == null ? null : this.first.item;
            lock.unlock();
            return e;
        } catch (Throwable th) {
            lock.unlock();
        }
    }

    public E peekLast() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E e = this.last == null ? null : this.last.item;
            lock.unlock();
            return e;
        } catch (Throwable th) {
            lock.unlock();
        }
    }

    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = this.first; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            lock.unlock();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = this.last; p != null; p = p.prev) {
                if (o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            lock.unlock();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean add(E e) {
        addLast(e);
        return true;
    }

    public boolean offer(E e) {
        return offerLast(e);
    }

    public void put(E e) throws InterruptedException {
        putLast(e);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offerLast(e, timeout, unit);
    }

    public E remove() {
        return removeFirst();
    }

    public E poll() {
        return pollFirst();
    }

    public E take() throws InterruptedException {
        return takeFirst();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return pollFirst(timeout, unit);
    }

    public E element() {
        return getFirst();
    }

    public E peek() {
        return peekFirst();
    }

    public int remainingCapacity() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = this.capacity - this.count;
            return i;
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        } else if (c != this) {
            int i = 0;
            if (maxElements <= 0) {
                return 0;
            }
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int n = Math.min(maxElements, this.count);
                while (i < n) {
                    c.add(this.first.item);
                    unlinkFirst();
                    i++;
                }
                return n;
            } finally {
                lock.unlock();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void push(E e) {
        addFirst(e);
    }

    public E pop() {
        return removeFirst();
    }

    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    public int size() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = this.count;
            return i;
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = this.first; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    return true;
                }
            }
            lock.unlock();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] a = new Object[this.count];
            int k = 0;
            Node<E> p = this.first;
            while (p != null) {
                int k2 = k + 1;
                a[k] = p.item;
                p = p.next;
                k = k2;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    public <T> T[] toArray(T[] a) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (a.length < this.count) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), this.count);
            }
            int k = 0;
            Node<E> p = this.first;
            while (p != null) {
                int k2 = k + 1;
                a[k] = p.item;
                p = p.next;
                k = k2;
            }
            if (a.length > k) {
                a[k] = null;
            }
            lock.unlock();
            return a;
        } catch (Throwable th) {
            lock.unlock();
        }
    }

    public String toString() {
        return Helpers.collectionToString(this);
    }

    public void clear() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Node<E> f = this.first;
            while (f != null) {
                f.item = null;
                Node<E> n = f.next;
                f.prev = null;
                f.next = null;
                f = n;
            }
            this.last = null;
            this.first = null;
            this.count = 0;
            this.notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    public Spliterator<E> spliterator() {
        return new LBDSpliterator(this);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            s.defaultWriteObject();
            for (Node<E> p = this.first; p != null; p = p.next) {
                s.writeObject(p.item);
            }
            s.writeObject(null);
        } finally {
            lock.unlock();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.count = 0;
        this.first = null;
        this.last = null;
        while (true) {
            E item = s.readObject();
            if (item != null) {
                add(item);
            } else {
                return;
            }
        }
    }
}
