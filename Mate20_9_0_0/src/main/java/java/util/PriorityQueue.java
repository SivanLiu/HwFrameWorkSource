package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;

public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable {
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final long serialVersionUID = -7720805057305804111L;
    private final Comparator<? super E> comparator;
    transient int modCount;
    transient Object[] queue;
    int size;

    private final class Itr implements Iterator<E> {
        private int cursor;
        private int expectedModCount;
        private ArrayDeque<E> forgetMeNot;
        private int lastRet;
        private E lastRetElt;

        private Itr() {
            this.lastRet = -1;
            this.expectedModCount = PriorityQueue.this.modCount;
        }

        public boolean hasNext() {
            return this.cursor < PriorityQueue.this.size || !(this.forgetMeNot == null || this.forgetMeNot.isEmpty());
        }

        public E next() {
            if (this.expectedModCount != PriorityQueue.this.modCount) {
                throw new ConcurrentModificationException();
            } else if (this.cursor < PriorityQueue.this.size) {
                Object[] objArr = PriorityQueue.this.queue;
                int i = this.cursor;
                this.cursor = i + 1;
                this.lastRet = i;
                return objArr[i];
            } else {
                if (this.forgetMeNot != null) {
                    this.lastRet = -1;
                    this.lastRetElt = this.forgetMeNot.poll();
                    if (this.lastRetElt != null) {
                        return this.lastRetElt;
                    }
                }
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (this.expectedModCount == PriorityQueue.this.modCount) {
                if (this.lastRet != -1) {
                    E moved = PriorityQueue.this.removeAt(this.lastRet);
                    this.lastRet = -1;
                    if (moved == null) {
                        this.cursor--;
                    } else {
                        if (this.forgetMeNot == null) {
                            this.forgetMeNot = new ArrayDeque();
                        }
                        this.forgetMeNot.add(moved);
                    }
                } else if (this.lastRetElt != null) {
                    PriorityQueue.this.removeEq(this.lastRetElt);
                    this.lastRetElt = null;
                } else {
                    throw new IllegalStateException();
                }
                this.expectedModCount = PriorityQueue.this.modCount;
                return;
            }
            throw new ConcurrentModificationException();
        }
    }

    static final class PriorityQueueSpliterator<E> implements Spliterator<E> {
        private int expectedModCount;
        private int fence;
        private int index;
        private final PriorityQueue<E> pq;

        PriorityQueueSpliterator(PriorityQueue<E> pq, int origin, int fence, int expectedModCount) {
            this.pq = pq;
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() {
            int i = this.fence;
            int hi = i;
            if (i >= 0) {
                return hi;
            }
            this.expectedModCount = this.pq.modCount;
            i = this.pq.size;
            this.fence = i;
            return i;
        }

        public PriorityQueueSpliterator<E> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = (lo + hi) >>> 1;
            if (lo >= mid) {
                return null;
            }
            PriorityQueue priorityQueue = this.pq;
            this.index = mid;
            return new PriorityQueueSpliterator(priorityQueue, lo, mid, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super E> action) {
            if (action != null) {
                PriorityQueue<E> priorityQueue = this.pq;
                PriorityQueue<E> q = priorityQueue;
                if (priorityQueue != null) {
                    Object[] objArr = q.queue;
                    Object[] a = objArr;
                    if (objArr != null) {
                        int i = this.fence;
                        int hi = i;
                        if (i < 0) {
                            i = q.modCount;
                            hi = q.size;
                        } else {
                            i = this.expectedModCount;
                        }
                        int i2 = this.index;
                        if (i2 >= 0) {
                            this.index = hi;
                            if (hi <= a.length) {
                                for (int i3 = i2; i3 < hi; i3++) {
                                    E e = a[i3];
                                    E e2 = e;
                                    if (e == null) {
                                        break;
                                    }
                                    action.accept(e2);
                                }
                                if (q.modCount == i) {
                                    return;
                                }
                            }
                        }
                    }
                }
                throw new ConcurrentModificationException();
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action != null) {
                int hi = getFence();
                int lo = this.index;
                if (lo < 0 || lo >= hi) {
                    return false;
                }
                this.index = lo + 1;
                E e = this.pq.queue[lo];
                if (e != null) {
                    action.accept(e);
                    if (this.pq.modCount == this.expectedModCount) {
                        return true;
                    }
                    throw new ConcurrentModificationException();
                }
                throw new ConcurrentModificationException();
            }
            throw new NullPointerException();
        }

        public long estimateSize() {
            return (long) (getFence() - this.index);
        }

        public int characteristics() {
            return 16704;
        }
    }

    public PriorityQueue() {
        this(11, null);
    }

    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityQueue(Comparator<? super E> comparator) {
        this(11, comparator);
    }

    public PriorityQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity >= 1) {
            this.queue = new Object[initialCapacity];
            this.comparator = comparator;
            return;
        }
        throw new IllegalArgumentException();
    }

    public PriorityQueue(Collection<? extends E> c) {
        if (c instanceof SortedSet) {
            SortedSet<? extends E> ss = (SortedSet) c;
            this.comparator = ss.comparator();
            initElementsFromCollection(ss);
        } else if (c instanceof PriorityQueue) {
            PriorityQueue<? extends E> pq = (PriorityQueue) c;
            this.comparator = pq.comparator();
            initFromPriorityQueue(pq);
        } else {
            this.comparator = null;
            initFromCollection(c);
        }
    }

    public PriorityQueue(PriorityQueue<? extends E> c) {
        this.comparator = c.comparator();
        initFromPriorityQueue(c);
    }

    public PriorityQueue(SortedSet<? extends E> c) {
        this.comparator = c.comparator();
        initElementsFromCollection(c);
    }

    private void initFromPriorityQueue(PriorityQueue<? extends E> c) {
        if (c.getClass() == PriorityQueue.class) {
            this.queue = c.toArray();
            this.size = c.size();
            return;
        }
        initFromCollection(c);
    }

    private void initElementsFromCollection(Collection<? extends E> c) {
        Object[] a = c.toArray();
        if (a.getClass() != Object[].class) {
            a = Arrays.copyOf(a, a.length, Object[].class);
        }
        if (a.length == 1 || this.comparator != null) {
            int length = a.length;
            int i = 0;
            while (i < length) {
                if (a[i] != null) {
                    i++;
                } else {
                    throw new NullPointerException();
                }
            }
        }
        this.queue = a;
        this.size = a.length;
    }

    private void initFromCollection(Collection<? extends E> c) {
        initElementsFromCollection(c);
        heapify();
    }

    private void grow(int minCapacity) {
        int newCapacity;
        int oldCapacity = this.queue.length;
        if (oldCapacity < 64) {
            newCapacity = oldCapacity + 2;
        } else {
            newCapacity = oldCapacity >> 1;
        }
        newCapacity += oldCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = hugeCapacity(minCapacity);
        }
        this.queue = Arrays.copyOf(this.queue, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) {
            throw new OutOfMemoryError();
        } else if (minCapacity > MAX_ARRAY_SIZE) {
            return Integer.MAX_VALUE;
        } else {
            return MAX_ARRAY_SIZE;
        }
    }

    public boolean add(E e) {
        return offer(e);
    }

    public boolean offer(E e) {
        if (e != null) {
            this.modCount++;
            int i = this.size;
            if (i >= this.queue.length) {
                grow(i + 1);
            }
            this.size = i + 1;
            if (i == 0) {
                this.queue[0] = e;
            } else {
                siftUp(i, e);
            }
            return true;
        }
        throw new NullPointerException();
    }

    public E peek() {
        return this.size == 0 ? null : this.queue[0];
    }

    private int indexOf(Object o) {
        if (o != null) {
            for (int i = 0; i < this.size; i++) {
                if (o.equals(this.queue[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i == -1) {
            return false;
        }
        removeAt(i);
        return true;
    }

    boolean removeEq(Object o) {
        for (int i = 0; i < this.size; i++) {
            if (o == this.queue[i]) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public Object[] toArray() {
        return Arrays.copyOf(this.queue, this.size);
    }

    public <T> T[] toArray(T[] a) {
        int size = this.size;
        if (a.length < size) {
            return Arrays.copyOf(this.queue, size, a.getClass());
        }
        System.arraycopy(this.queue, 0, (Object) a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public int size() {
        return this.size;
    }

    public void clear() {
        this.modCount++;
        for (int i = 0; i < this.size; i++) {
            this.queue[i] = null;
        }
        this.size = 0;
    }

    public E poll() {
        if (this.size == 0) {
            return null;
        }
        int s = this.size - 1;
        this.size = s;
        this.modCount++;
        E result = this.queue[0];
        E x = this.queue[s];
        this.queue[s] = null;
        if (s != 0) {
            siftDown(0, x);
        }
        return result;
    }

    E removeAt(int i) {
        this.modCount++;
        int s = this.size - 1;
        this.size = s;
        if (s == i) {
            this.queue[i] = null;
        } else {
            E moved = this.queue[s];
            this.queue[s] = null;
            siftDown(i, moved);
            if (this.queue[i] == moved) {
                siftUp(i, moved);
                if (this.queue[i] != moved) {
                    return moved;
                }
            }
        }
        return null;
    }

    private void siftUp(int k, E x) {
        if (this.comparator != null) {
            siftUpUsingComparator(k, x);
        } else {
            siftUpComparable(k, x);
        }
    }

    private void siftUpComparable(int k, E x) {
        Comparable<? super E> key = (Comparable) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = this.queue[parent];
            if (key.compareTo(e) >= 0) {
                break;
            }
            this.queue[k] = e;
            k = parent;
        }
        this.queue[k] = key;
    }

    private void siftUpUsingComparator(int k, E x) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = this.queue[parent];
            if (this.comparator.compare(x, e) >= 0) {
                break;
            }
            this.queue[k] = e;
            k = parent;
        }
        this.queue[k] = x;
    }

    private void siftDown(int k, E x) {
        if (this.comparator != null) {
            siftDownUsingComparator(k, x);
        } else {
            siftDownComparable(k, x);
        }
    }

    private void siftDownComparable(int k, E x) {
        Comparable<? super E> key = (Comparable) x;
        int half = this.size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = this.queue[child];
            int right = child + 1;
            if (right < this.size && ((Comparable) c).compareTo(this.queue[right]) > 0) {
                child = right;
                c = this.queue[right];
            }
            if (key.compareTo(c) <= 0) {
                break;
            }
            this.queue[k] = c;
            k = child;
        }
        this.queue[k] = key;
    }

    private void siftDownUsingComparator(int k, E x) {
        int half = this.size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = this.queue[child];
            int right = child + 1;
            if (right < this.size && this.comparator.compare(c, this.queue[right]) > 0) {
                child = right;
                c = this.queue[right];
            }
            if (this.comparator.compare(x, c) <= 0) {
                break;
            }
            this.queue[k] = c;
            k = child;
        }
        this.queue[k] = x;
    }

    private void heapify() {
        for (int i = (this.size >>> 1) - 1; i >= 0; i--) {
            siftDown(i, this.queue[i]);
        }
    }

    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(Math.max(2, this.size + 1));
        for (int i = 0; i < this.size; i++) {
            s.writeObject(this.queue[i]);
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        s.readInt();
        this.queue = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            this.queue[i] = s.readObject();
        }
        heapify();
    }

    public final Spliterator<E> spliterator() {
        return new PriorityQueueSpliterator(this, 0, -1, 0);
    }
}
