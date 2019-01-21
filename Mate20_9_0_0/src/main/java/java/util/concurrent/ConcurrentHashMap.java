package java.util.concurrent;

import java.awt.font.NumericShaper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import sun.misc.Unsafe;

public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
    private static final int ABASE;
    private static final int ASHIFT;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final int DEFAULT_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    static final int HASH_BITS = Integer.MAX_VALUE;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = 1073741824;
    static final int MAX_ARRAY_SIZE = 2147483639;
    private static final int MAX_RESIZERS = 65535;
    private static final int MIN_TRANSFER_STRIDE = 16;
    static final int MIN_TREEIFY_CAPACITY = 64;
    static final int MOVED = -1;
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    static final int RESERVED = -3;
    private static final int RESIZE_STAMP_BITS = 16;
    private static final int RESIZE_STAMP_SHIFT = 16;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    static final int TREEBIN = -2;
    static final int TREEIFY_THRESHOLD = 8;
    private static final Unsafe U = Unsafe.getUnsafe();
    static final int UNTREEIFY_THRESHOLD = 6;
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("segments", Segment[].class), new ObjectStreamField("segmentMask", Integer.TYPE), new ObjectStreamField("segmentShift", Integer.TYPE)};
    private static final long serialVersionUID = 7249069246763182397L;
    private volatile transient long baseCount;
    private volatile transient int cellsBusy;
    private volatile transient CounterCell[] counterCells;
    private transient EntrySetView<K, V> entrySet;
    private transient KeySetView<K, V> keySet;
    private volatile transient Node<K, V>[] nextTable;
    private volatile transient int sizeCtl;
    volatile transient Node<K, V>[] table;
    private volatile transient int transferIndex;
    private transient ValuesView<K, V> values;

    static final class CounterCell {
        volatile long value;

        CounterCell(long x) {
            this.value = x;
        }
    }

    static final class TableStack<K, V> {
        int index;
        int length;
        TableStack<K, V> next;
        Node<K, V>[] tab;

        TableStack() {
        }
    }

    static class Traverser<K, V> {
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int index;
        Node<K, V> next = null;
        TableStack<K, V> spare;
        TableStack<K, V> stack;
        Node<K, V>[] tab;

        Traverser(Node<K, V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.index = index;
            this.baseIndex = index;
            this.baseLimit = limit;
        }

        final Node<K, V> advance() {
            Node<K, V> node = this.next;
            Node<K, V> e = node;
            if (node != null) {
                e = e.next;
            }
            while (e == null) {
                if (this.baseIndex < this.baseLimit) {
                    Node<K, V>[] nodeArr = this.tab;
                    Node<K, V>[] t = nodeArr;
                    if (nodeArr != null) {
                        int length = t.length;
                        int n = length;
                        int i = this.index;
                        int i2 = i;
                        if (length > i && i2 >= 0) {
                            node = ConcurrentHashMap.tabAt(t, i2);
                            e = node;
                            if (node != null && e.hash < 0) {
                                if (e instanceof ForwardingNode) {
                                    this.tab = ((ForwardingNode) e).nextTable;
                                    e = null;
                                    pushState(t, i2, n);
                                } else {
                                    if (e instanceof TreeBin) {
                                        node = ((TreeBin) e).first;
                                    } else {
                                        node = null;
                                    }
                                    e = node;
                                }
                            }
                            if (this.stack != null) {
                                recoverState(n);
                            } else {
                                length = this.baseSize + i2;
                                this.index = length;
                                if (length >= n) {
                                    length = this.baseIndex + 1;
                                    this.baseIndex = length;
                                    this.index = length;
                                }
                            }
                        }
                    }
                }
                this.next = null;
                return null;
            }
            this.next = e;
            return e;
        }

        private void pushState(Node<K, V>[] t, int i, int n) {
            TableStack<K, V> s = this.spare;
            if (s != null) {
                this.spare = s.next;
            } else {
                s = new TableStack();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = this.stack;
            this.stack = s;
        }

        private void recoverState(int n) {
            TableStack<K, V> s;
            int i;
            while (true) {
                TableStack<K, V> tableStack = this.stack;
                s = tableStack;
                if (tableStack == null) {
                    break;
                }
                i = this.index;
                int i2 = s.length;
                int len = i2;
                i += i2;
                this.index = i;
                if (i < n) {
                    break;
                }
                n = len;
                this.index = s.index;
                this.tab = s.tab;
                s.tab = null;
                tableStack = s.next;
                s.next = this.spare;
                this.stack = tableStack;
                this.spare = s;
            }
            if (s == null) {
                i = this.index + this.baseSize;
                this.index = i;
                if (i >= n) {
                    i = this.baseIndex + 1;
                    this.baseIndex = i;
                    this.index = i;
                }
            }
        }
    }

    static class BaseIterator<K, V> extends Traverser<K, V> {
        Node<K, V> lastReturned;
        final ConcurrentHashMap<K, V> map;

        BaseIterator(Node<K, V>[] tab, int size, int index, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();
        }

        public final boolean hasNext() {
            return this.next != null;
        }

        public final boolean hasMoreElements() {
            return this.next != null;
        }

        public final void remove() {
            Node<K, V> node = this.lastReturned;
            Node<K, V> p = node;
            if (node != null) {
                this.lastReturned = null;
                this.map.replaceNode(p.key, null, null);
                return;
            }
            throw new IllegalStateException();
        }
    }

    static final class EntrySpliterator<K, V> extends Traverser<K, V> implements Spliterator<Entry<K, V>> {
        long est;
        final ConcurrentHashMap<K, V> map;

        EntrySpliterator(Node<K, V>[] tab, int size, int index, int limit, long est, ConcurrentHashMap<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            this.est = est;
        }

        public EntrySpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int i2 = i;
            int i3 = this.baseLimit;
            int f = i3;
            i = (i + i3) >>> 1;
            int h = i;
            if (i <= i2) {
                return null;
            }
            Node[] nodeArr = this.tab;
            int i4 = this.baseSize;
            this.baseLimit = h;
            long j = this.est >>> 1;
            this.est = j;
            return new EntrySpliterator(nodeArr, i4, h, f, j, this.map);
        }

        public void forEachRemaining(Consumer<? super Entry<K, V>> action) {
            if (action != null) {
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(new MapEntry(p.key, p.val, this.map));
                    } else {
                        return;
                    }
                }
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super Entry<K, V>> action) {
            if (action != null) {
                Node<K, V> advance = advance();
                Node<K, V> p = advance;
                if (advance == null) {
                    return false;
                }
                action.accept(new MapEntry(p.key, p.val, this.map));
                return true;
            }
            throw new NullPointerException();
        }

        public long estimateSize() {
            return this.est;
        }

        public int characteristics() {
            return 4353;
        }
    }

    static final class KeySpliterator<K, V> extends Traverser<K, V> implements Spliterator<K> {
        long est;

        KeySpliterator(Node<K, V>[] tab, int size, int index, int limit, long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        public KeySpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int i2 = i;
            int i3 = this.baseLimit;
            int f = i3;
            i = (i + i3) >>> 1;
            int h = i;
            if (i <= i2) {
                return null;
            }
            Node[] nodeArr = this.tab;
            int i4 = this.baseSize;
            this.baseLimit = h;
            long j = this.est >>> 1;
            this.est = j;
            return new KeySpliterator(nodeArr, i4, h, f, j);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action != null) {
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p.key);
                    } else {
                        return;
                    }
                }
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action != null) {
                Node<K, V> advance = advance();
                Node<K, V> p = advance;
                if (advance == null) {
                    return false;
                }
                action.accept(p.key);
                return true;
            }
            throw new NullPointerException();
        }

        public long estimateSize() {
            return this.est;
        }

        public int characteristics() {
            return 4353;
        }
    }

    static final class MapEntry<K, V> implements Entry<K, V> {
        final K key;
        final ConcurrentHashMap<K, V> map;
        V val;

        MapEntry(K key, V val, ConcurrentHashMap<K, V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.val;
        }

        public int hashCode() {
            return this.key.hashCode() ^ this.val.hashCode();
        }

        public String toString() {
            return Helpers.mapEntryToString(this.key, this.val);
        }

        public boolean equals(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry) o;
                Entry<?, ?> e = entry;
                Object key = entry.getKey();
                Object k = key;
                if (key != null) {
                    key = e.getValue();
                    Object v = key;
                    if (key != null && ((k == this.key || k.equals(this.key)) && (v == this.val || v.equals(this.val)))) {
                        return true;
                    }
                }
            }
            return false;
        }

        public V setValue(V value) {
            if (value != null) {
                V v = this.val;
                this.val = value;
                this.map.put(this.key, value);
                return v;
            }
            throw new NullPointerException();
        }
    }

    static class Node<K, V> implements Entry<K, V> {
        final int hash;
        final K key;
        volatile Node<K, V> next;
        volatile V val;

        Node(int hash, K key, V val, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey() {
            return this.key;
        }

        public final V getValue() {
            return this.val;
        }

        public final int hashCode() {
            return this.key.hashCode() ^ this.val.hashCode();
        }

        public final String toString() {
            return Helpers.mapEntryToString(this.key, this.val);
        }

        public final V setValue(V v) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry) o;
                Entry<?, ?> e = entry;
                Object key = entry.getKey();
                Object k = key;
                if (key != null) {
                    key = e.getValue();
                    Object v = key;
                    if (key != null && (k == this.key || k.equals(this.key))) {
                        key = this.val;
                        Object u = key;
                        if (v == key || v.equals(u)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        Node<K, V> find(int h, Object k) {
            Node<K, V> e = this;
            if (k != null) {
                Node<K, V> node;
                do {
                    if (e.hash == h) {
                        K k2 = e.key;
                        K ek = k2;
                        if (k2 == k || (ek != null && k.equals(ek))) {
                            return e;
                        }
                    }
                    node = e.next;
                    e = node;
                } while (node != null);
            }
            return null;
        }
    }

    static final class ValueSpliterator<K, V> extends Traverser<K, V> implements Spliterator<V> {
        long est;

        ValueSpliterator(Node<K, V>[] tab, int size, int index, int limit, long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        public ValueSpliterator<K, V> trySplit() {
            int i = this.baseIndex;
            int i2 = i;
            int i3 = this.baseLimit;
            int f = i3;
            i = (i + i3) >>> 1;
            int h = i;
            if (i <= i2) {
                return null;
            }
            Node[] nodeArr = this.tab;
            int i4 = this.baseSize;
            this.baseLimit = h;
            long j = this.est >>> 1;
            this.est = j;
            return new ValueSpliterator(nodeArr, i4, h, f, j);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action != null) {
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p.val);
                    } else {
                        return;
                    }
                }
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action != null) {
                Node<K, V> advance = advance();
                Node<K, V> p = advance;
                if (advance == null) {
                    return false;
                }
                action.accept(p.val);
                return true;
            }
            throw new NullPointerException();
        }

        public long estimateSize() {
            return this.est;
        }

        public int characteristics() {
            return 4352;
        }
    }

    static abstract class CollectionView<K, V, E> implements Collection<E>, Serializable {
        private static final String OOME_MSG = "Required array size too large";
        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentHashMap<K, V> map;

        public abstract boolean contains(Object obj);

        public abstract Iterator<E> iterator();

        public abstract boolean remove(Object obj);

        CollectionView(ConcurrentHashMap<K, V> map) {
            this.map = map;
        }

        public ConcurrentHashMap<K, V> getMap() {
            return this.map;
        }

        public final void clear() {
            this.map.clear();
        }

        public final int size() {
            return this.map.size();
        }

        public final boolean isEmpty() {
            return this.map.isEmpty();
        }

        public final Object[] toArray() {
            long sz = this.map.mappingCount();
            if (sz <= 2147483639) {
                int n = (int) sz;
                Object[] r = new Object[n];
                int i = 0;
                Iterator it = iterator();
                while (it.hasNext()) {
                    E e = it.next();
                    if (i == n) {
                        if (n < ConcurrentHashMap.MAX_ARRAY_SIZE) {
                            if (n >= 1073741819) {
                                n = ConcurrentHashMap.MAX_ARRAY_SIZE;
                            } else {
                                n += (n >>> 1) + 1;
                            }
                            r = Arrays.copyOf(r, n);
                        } else {
                            throw new OutOfMemoryError(OOME_MSG);
                        }
                    }
                    int i2 = i + 1;
                    r[i] = e;
                    i = i2;
                }
                return i == n ? r : Arrays.copyOf(r, i);
            } else {
                throw new OutOfMemoryError(OOME_MSG);
            }
        }

        public final <T> T[] toArray(T[] a) {
            long sz = this.map.mappingCount();
            if (sz <= 2147483639) {
                int m = (int) sz;
                Object[] r = a.length >= m ? a : (Object[]) Array.newInstance(a.getClass().getComponentType(), m);
                int n = r.length;
                int i = 0;
                Iterator it = iterator();
                while (it.hasNext()) {
                    E e = it.next();
                    if (i == n) {
                        if (n < ConcurrentHashMap.MAX_ARRAY_SIZE) {
                            if (n >= 1073741819) {
                                n = ConcurrentHashMap.MAX_ARRAY_SIZE;
                            } else {
                                n += (n >>> 1) + 1;
                            }
                            r = Arrays.copyOf(r, n);
                        } else {
                            throw new OutOfMemoryError(OOME_MSG);
                        }
                    }
                    int i2 = i + 1;
                    r[i] = e;
                    i = i2;
                }
                if (a != r || i >= n) {
                    return i == n ? r : Arrays.copyOf(r, i);
                }
                r[i] = null;
                return r;
            }
            throw new OutOfMemoryError(OOME_MSG);
        }

        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                while (true) {
                    CollectionView e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext()) {
                        break;
                    }
                    sb.append(',');
                    sb.append(' ');
                }
            }
            sb.append(']');
            return sb.toString();
        }

        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e)) {
                        return false;
                    }
                }
            }
            return true;
        }

        public final boolean removeAll(Collection<?> c) {
            if (c != null) {
                boolean modified = false;
                Iterator<E> it = iterator();
                while (it.hasNext()) {
                    if (c.contains(it.next())) {
                        it.remove();
                        modified = true;
                    }
                }
                return modified;
            }
            throw new NullPointerException();
        }

        public final boolean retainAll(Collection<?> c) {
            if (c != null) {
                boolean modified = false;
                Iterator<E> it = iterator();
                while (it.hasNext()) {
                    if (!c.contains(it.next())) {
                        it.remove();
                        modified = true;
                    }
                }
                return modified;
            }
            throw new NullPointerException();
        }
    }

    static final class EntryIterator<K, V> extends BaseIterator<K, V> implements Iterator<Entry<K, V>> {
        EntryIterator(Node<K, V>[] tab, int index, int size, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        public final Entry<K, V> next() {
            Node<K, V> node = this.next;
            Node<K, V> p = node;
            if (node != null) {
                K k = p.key;
                V v = p.val;
                this.lastReturned = p;
                advance();
                return new MapEntry(k, v, this.map);
            }
            throw new NoSuchElementException();
        }
    }

    static final class ForwardingNode<K, V> extends Node<K, V> {
        final Node<K, V>[] nextTable;

        ForwardingNode(Node<K, V>[] tab) {
            super(-1, null, null, null);
            this.nextTable = tab;
        }

        /* JADX WARNING: Missing block: B:18:0x002e, code skipped:
            if ((r2 instanceof java.util.concurrent.ConcurrentHashMap.ForwardingNode) == false) goto L_0x0036;
     */
        /* JADX WARNING: Missing block: B:19:0x0030, code skipped:
            r0 = ((java.util.concurrent.ConcurrentHashMap.ForwardingNode) r2).nextTable;
     */
        /* JADX WARNING: Missing block: B:21:0x003a, code skipped:
            return r2.find(r8, r9);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        Node<K, V> find(int h, Object k) {
            Node<K, V>[] tab = this.nextTable;
            loop0:
            while (k != null && tab != null) {
                int length = tab.length;
                int n = length;
                if (length == 0) {
                    break;
                }
                Node<K, V> tabAt = ConcurrentHashMap.tabAt(tab, (n - 1) & h);
                Node<K, V> e = tabAt;
                if (tabAt == null) {
                    break;
                }
                while (true) {
                    tabAt = e;
                    int i = tabAt.hash;
                    int eh = i;
                    if (i == h) {
                        K k2 = tabAt.key;
                        K ek = k2;
                        if (k2 == k || (ek != null && k.equals(ek))) {
                            return tabAt;
                        }
                    }
                    if (eh < 0) {
                        break;
                    }
                    e = tabAt.next;
                    tabAt = e;
                    if (e == null) {
                        return null;
                    }
                    e = tabAt;
                }
                return tabAt;
            }
            return null;
        }
    }

    static final class KeyIterator<K, V> extends BaseIterator<K, V> implements Iterator<K>, Enumeration<K> {
        KeyIterator(Node<K, V>[] tab, int index, int size, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        public final K next() {
            Node<K, V> node = this.next;
            Node<K, V> p = node;
            if (node != null) {
                K k = p.key;
                this.lastReturned = p;
                advance();
                return k;
            }
            throw new NoSuchElementException();
        }

        public final K nextElement() {
            return next();
        }
    }

    static final class ReservationNode<K, V> extends Node<K, V> {
        ReservationNode() {
            super(-3, null, null, null);
        }

        Node<K, V> find(int h, Object k) {
            return null;
        }
    }

    static class Segment<K, V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;

        Segment(float lf) {
            this.loadFactor = lf;
        }
    }

    static final class TreeBin<K, V> extends Node<K, V> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final long LOCKSTATE;
        static final int READER = 4;
        private static final Unsafe U = Unsafe.getUnsafe();
        static final int WAITER = 2;
        static final int WRITER = 1;
        volatile TreeNode<K, V> first;
        volatile int lockState;
        TreeNode<K, V> root;
        volatile Thread waiter;

        static {
            Class cls = ConcurrentHashMap.class;
            try {
                LOCKSTATE = U.objectFieldOffset(TreeBin.class.getDeclaredField("lockState"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        /* JADX WARNING: Missing block: B:3:0x0019, code skipped:
            if (r0 == 0) goto L_0x001b;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (!(a == null || b == null)) {
                int compareTo = a.getClass().getName().compareTo(b.getClass().getName());
                d = compareTo;
            }
            d = System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1;
            return d;
        }

        /* JADX WARNING: Missing block: B:13:0x0038, code skipped:
            if (r9 != null) goto L_0x003a;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        TreeBin(TreeNode<K, V> b) {
            super(-2, null, null, null);
            this.first = b;
            TreeNode<K, V> r = null;
            TreeNode<K, V> x = b;
            while (x != null) {
                TreeNode<K, V> next = x.next;
                x.right = null;
                x.left = null;
                if (r == null) {
                    x.parent = null;
                    x.red = $assertionsDisabled;
                    r = x;
                } else {
                    int i;
                    TreeNode<K, V> xp;
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    TreeNode<K, V> p = r;
                    while (true) {
                        K pk = p.key;
                        i = p.hash;
                        int ph = i;
                        if (i > h) {
                            i = -1;
                        } else if (ph < h) {
                            i = 1;
                        } else {
                            if (kc == null) {
                                Class<?> comparableClassFor = ConcurrentHashMap.comparableClassFor(k);
                                kc = comparableClassFor;
                            }
                            i = ConcurrentHashMap.compareComparables(kc, k, pk);
                            int dir = i;
                            if (i != 0) {
                                i = dir;
                            }
                            i = tieBreakOrder(k, pk);
                        }
                        xp = p;
                        TreeNode<K, V> treeNode = i <= 0 ? p.left : p.right;
                        p = treeNode;
                        if (treeNode == null) {
                            break;
                        }
                    }
                    x.parent = xp;
                    if (i <= 0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    r = balanceInsertion(r, x);
                }
                x = next;
            }
            this.root = r;
        }

        private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, 1)) {
                contendedLock();
            }
        }

        private final void unlockRoot() {
            this.lockState = 0;
        }

        private final void contendedLock() {
            boolean waiting = $assertionsDisabled;
            while (true) {
                int i = this.lockState;
                int s = i;
                if ((i & -3) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, 1)) {
                        break;
                    }
                } else if ((s & 2) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | 2)) {
                        waiting = true;
                        this.waiter = Thread.currentThread();
                    }
                } else if (waiting) {
                    LockSupport.park(this);
                }
            }
            if (waiting) {
                this.waiter = null;
            }
        }

        final Node<K, V> find(int h, Object k) {
            TreeNode<K, V> p = null;
            if (k != null) {
                Node<K, V> e = this.first;
                while (e != null) {
                    int i = this.lockState;
                    int s = i;
                    if ((i & 3) != 0) {
                        if (e.hash == h) {
                            K k2 = e.key;
                            K ek = k2;
                            if (k2 == k || (ek != null && k.equals(ek))) {
                                return e;
                            }
                        }
                        e = e.next;
                    } else {
                        if (U.compareAndSwapInt(this, LOCKSTATE, s, s + 4)) {
                            Thread thread;
                            Thread w;
                            try {
                                TreeNode<K, V> treeNode = this.root;
                                TreeNode<K, V> r = treeNode;
                                if (treeNode != null) {
                                    p = r.findTreeNode(h, k, null);
                                }
                                if (U.getAndAddInt(this, LOCKSTATE, -4) == 6) {
                                    thread = this.waiter;
                                    w = thread;
                                    if (thread != null) {
                                        LockSupport.unpark(w);
                                    }
                                }
                                return p;
                            } catch (Throwable th) {
                                if (U.getAndAddInt(this, LOCKSTATE, -4) == 6) {
                                    thread = this.waiter;
                                    w = thread;
                                    if (thread != null) {
                                        LockSupport.unpark(w);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        /* JADX WARNING: Missing block: B:17:0x0044, code skipped:
            if (r2 != null) goto L_0x0046;
     */
        /* JADX WARNING: Missing block: B:29:0x0068, code skipped:
            return r5;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final TreeNode<K, V> putTreeVal(int h, K k, V v) {
            int i = h;
            K k2 = k;
            TreeNode<K, V> p = this.root;
            boolean searched = null;
            boolean searched2 = $assertionsDisabled;
            while (true) {
                TreeNode<K, V> p2 = p;
                TreeNode treeNode;
                if (p2 == null) {
                    treeNode = new TreeNode(i, k2, v, null, null);
                    this.root = treeNode;
                    this.first = treeNode;
                    break;
                }
                TreeNode<K, V> findTreeNode;
                int i2 = p2.hash;
                int ph = i2;
                if (i2 > i) {
                    i2 = -1;
                } else if (ph < i) {
                    i2 = 1;
                } else {
                    K k3 = p2.key;
                    K pk = k3;
                    if (k3 == k2 || (pk != null && k2.equals(pk))) {
                        return p2;
                    }
                    if (!searched) {
                        Class comparableClassFor = ConcurrentHashMap.comparableClassFor(k);
                        searched = comparableClassFor;
                    }
                    i2 = ConcurrentHashMap.compareComparables(searched, k2, pk);
                    int dir = i2;
                    if (i2 != 0) {
                        i2 = dir;
                    }
                    if (!searched2) {
                        TreeNode<K, V> q;
                        searched2 = true;
                        treeNode = p2.left;
                        dir = treeNode;
                        if (treeNode != null) {
                            findTreeNode = dir.findTreeNode(i, k2, searched);
                            q = findTreeNode;
                            if (findTreeNode != null) {
                                break;
                            }
                        }
                        treeNode = p2.right;
                        dir = treeNode;
                        if (treeNode != null) {
                            findTreeNode = dir.findTreeNode(i, k2, searched);
                            q = findTreeNode;
                            if (findTreeNode != null) {
                                break;
                            }
                        }
                    }
                    i2 = tieBreakOrder(k2, pk);
                }
                Class<?> kc = searched;
                searched = searched2;
                int dir2 = i2;
                TreeNode<K, V> xp = p2;
                TreeNode<K, V> treeNode2 = dir2 <= 0 ? p2.left : p2.right;
                p2 = treeNode2;
                if (treeNode2 == null) {
                    Node f = this.first;
                    Node f2 = f;
                    findTreeNode = new TreeNode(i, k2, v, f, xp);
                    this.first = findTreeNode;
                    if (f2 != null) {
                        f2.prev = findTreeNode;
                    }
                    if (dir2 <= 0) {
                        xp.left = findTreeNode;
                    } else {
                        xp.right = findTreeNode;
                    }
                    if (xp.red) {
                        lockRoot();
                        try {
                            this.root = balanceInsertion(this.root, findTreeNode);
                        } finally {
                            unlockRoot();
                        }
                    } else {
                        findTreeNode.red = true;
                    }
                    searched2 = searched;
                    searched = kc;
                } else {
                    searched2 = searched;
                    p = p2;
                    searched = kc;
                    i = h;
                }
            }
            return null;
        }

        final boolean removeTreeNode(TreeNode<K, V> p) {
            TreeNode<K, V> next = p.next;
            TreeNode<K, V> pred = p.prev;
            if (pred == null) {
                this.first = next;
            } else {
                pred.next = next;
            }
            if (next != null) {
                next.prev = pred;
            }
            if (this.first == null) {
                this.root = null;
                return true;
            }
            TreeNode<K, V> treeNode = this.root;
            TreeNode<K, V> r = treeNode;
            if (!(treeNode == null || r.right == null)) {
                treeNode = r.left;
                TreeNode<K, V> rl = treeNode;
                if (!(treeNode == null || rl.left == null)) {
                    lockRoot();
                    try {
                        TreeNode<K, V> s;
                        TreeNode<K, V> treeNode2;
                        TreeNode<K, V> sl;
                        treeNode = p.left;
                        TreeNode<K, V> pr = p.right;
                        if (treeNode != null && pr != null) {
                            TreeNode<K, V> sp;
                            s = pr;
                            while (true) {
                                treeNode2 = s.left;
                                sl = treeNode2;
                                if (treeNode2 == null) {
                                    break;
                                }
                                s = sl;
                            }
                            boolean c = s.red;
                            s.red = p.red;
                            p.red = c;
                            TreeNode<K, V> sr = s.right;
                            TreeNode<K, V> pp = p.parent;
                            if (s == pr) {
                                p.parent = s;
                                s.right = p;
                            } else {
                                sp = s.parent;
                                p.parent = sp;
                                if (sp != null) {
                                    if (s == sp.left) {
                                        sp.left = p;
                                    } else {
                                        sp.right = p;
                                    }
                                }
                                s.right = pr;
                                if (pr != null) {
                                    pr.parent = s;
                                }
                            }
                            p.left = null;
                            p.right = sr;
                            if (sr != null) {
                                sr.parent = p;
                            }
                            s.left = treeNode;
                            if (treeNode != null) {
                                treeNode.parent = s;
                            }
                            s.parent = pp;
                            if (pp == null) {
                                r = s;
                            } else if (p == pp.left) {
                                pp.left = s;
                            } else {
                                pp.right = s;
                            }
                            if (sr != null) {
                                sp = sr;
                            } else {
                                sp = p;
                            }
                            s = sp;
                        } else if (treeNode != null) {
                            s = treeNode;
                        } else if (pr != null) {
                            s = pr;
                        } else {
                            s = p;
                        }
                        if (s != p) {
                            treeNode2 = p.parent;
                            s.parent = treeNode2;
                            if (treeNode2 == null) {
                                r = s;
                            } else if (p == treeNode2.left) {
                                treeNode2.left = s;
                            } else {
                                treeNode2.right = s;
                            }
                            p.parent = null;
                            p.right = null;
                            p.left = null;
                        }
                        this.root = p.red ? r : balanceDeletion(r, s);
                        if (p == s) {
                            treeNode2 = p.parent;
                            sl = treeNode2;
                            if (treeNode2 != null) {
                                if (p == sl.left) {
                                    sl.left = null;
                                } else if (p == sl.right) {
                                    sl.right = null;
                                }
                                p.parent = null;
                            }
                        }
                        unlockRoot();
                        return $assertionsDisabled;
                    } catch (Throwable th) {
                        unlockRoot();
                    }
                }
            }
            return true;
        }

        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
            if (p != null) {
                TreeNode<K, V> treeNode = p.right;
                TreeNode<K, V> r = treeNode;
                if (treeNode != null) {
                    treeNode = r.left;
                    p.right = treeNode;
                    TreeNode<K, V> rl = treeNode;
                    if (treeNode != null) {
                        rl.parent = p;
                    }
                    treeNode = p.parent;
                    r.parent = treeNode;
                    TreeNode<K, V> pp = treeNode;
                    if (treeNode == null) {
                        root = r;
                        r.red = $assertionsDisabled;
                    } else if (pp.left == p) {
                        pp.left = r;
                    } else {
                        pp.right = r;
                    }
                    r.left = p;
                    p.parent = r;
                }
            }
            return root;
        }

        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
            if (p != null) {
                TreeNode<K, V> treeNode = p.left;
                TreeNode<K, V> l = treeNode;
                if (treeNode != null) {
                    treeNode = l.right;
                    p.left = treeNode;
                    TreeNode<K, V> lr = treeNode;
                    if (treeNode != null) {
                        lr.parent = p;
                    }
                    treeNode = p.parent;
                    l.parent = treeNode;
                    TreeNode<K, V> pp = treeNode;
                    if (treeNode == null) {
                        root = l;
                        l.red = $assertionsDisabled;
                    } else if (pp.right == p) {
                        pp.right = l;
                    } else {
                        pp.left = l;
                    }
                    l.right = p;
                    p.parent = l;
                }
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
            x.red = true;
            while (true) {
                TreeNode<K, V> treeNode = x.parent;
                TreeNode<K, V> xp = treeNode;
                if (treeNode != null) {
                    if (!xp.red) {
                        break;
                    }
                    treeNode = xp.parent;
                    TreeNode<K, V> xpp = treeNode;
                    if (treeNode == null) {
                        break;
                    }
                    treeNode = xpp.left;
                    TreeNode<K, V> xppl = treeNode;
                    TreeNode<K, V> treeNode2 = null;
                    if (xp == treeNode) {
                        treeNode = xpp.right;
                        TreeNode<K, V> xppr = treeNode;
                        if (treeNode == null || !xppr.red) {
                            if (x == xp.right) {
                                x = xp;
                                root = rotateLeft(root, xp);
                                treeNode = x.parent;
                                xp = treeNode;
                                if (treeNode != null) {
                                    treeNode2 = xp.parent;
                                }
                                xpp = treeNode2;
                            }
                            if (xp != null) {
                                xp.red = $assertionsDisabled;
                                if (xpp != null) {
                                    xpp.red = true;
                                    root = rotateRight(root, xpp);
                                }
                            }
                        } else {
                            xppr.red = $assertionsDisabled;
                            xp.red = $assertionsDisabled;
                            xpp.red = true;
                            x = xpp;
                        }
                    } else if (xppl == null || !xppl.red) {
                        if (x == xp.left) {
                            x = xp;
                            root = rotateRight(root, xp);
                            treeNode = x.parent;
                            xp = treeNode;
                            if (treeNode != null) {
                                treeNode2 = xp.parent;
                            }
                            xpp = treeNode2;
                        }
                        if (xp != null) {
                            xp.red = $assertionsDisabled;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    } else {
                        xppl.red = $assertionsDisabled;
                        xp.red = $assertionsDisabled;
                        xpp.red = true;
                        x = xpp;
                    }
                } else {
                    x.red = $assertionsDisabled;
                    return x;
                }
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
            while (x != null && x != root) {
                TreeNode<K, V> treeNode = x.parent;
                TreeNode<K, V> xp = treeNode;
                if (treeNode == null) {
                    x.red = $assertionsDisabled;
                    return x;
                } else if (x.red) {
                    x.red = $assertionsDisabled;
                    return root;
                } else {
                    treeNode = xp.left;
                    TreeNode<K, V> xpl = treeNode;
                    TreeNode<K, V> xpr = null;
                    TreeNode<K, V> xpr2;
                    TreeNode<K, V> treeNode2;
                    if (treeNode == x) {
                        treeNode = xp.right;
                        xpr2 = treeNode;
                        if (treeNode != null && xpr2.red) {
                            xpr2.red = $assertionsDisabled;
                            xp.red = true;
                            root = rotateLeft(root, xp);
                            treeNode = x.parent;
                            xp = treeNode;
                            xpr2 = treeNode == null ? null : xp.right;
                        }
                        if (xpr2 == null) {
                            x = xp;
                        } else {
                            treeNode = xpr2.left;
                            TreeNode<K, V> sr = xpr2.right;
                            if ((sr == null || !sr.red) && (treeNode == null || !treeNode.red)) {
                                xpr2.red = true;
                                x = xp;
                            } else {
                                if (sr == null || !sr.red) {
                                    if (treeNode != null) {
                                        treeNode.red = $assertionsDisabled;
                                    }
                                    xpr2.red = true;
                                    root = rotateRight(root, xpr2);
                                    treeNode2 = x.parent;
                                    xp = treeNode2;
                                    if (treeNode2 != null) {
                                        xpr = xp.right;
                                    }
                                    xpr2 = xpr;
                                }
                                if (xpr2 != null) {
                                    xpr2.red = xp == null ? $assertionsDisabled : xp.red;
                                    xpr = xpr2.right;
                                    sr = xpr;
                                    if (xpr != null) {
                                        sr.red = $assertionsDisabled;
                                    }
                                }
                                if (xp != null) {
                                    xp.red = $assertionsDisabled;
                                    root = rotateLeft(root, xp);
                                }
                                x = root;
                            }
                        }
                    } else {
                        if (xpl != null && xpl.red) {
                            xpl.red = $assertionsDisabled;
                            xp.red = true;
                            root = rotateRight(root, xp);
                            treeNode = x.parent;
                            xp = treeNode;
                            xpl = treeNode == null ? null : xp.left;
                        }
                        if (xpl == null) {
                            x = xp;
                        } else {
                            treeNode = xpl.left;
                            xpr2 = xpl.right;
                            if ((treeNode == null || !treeNode.red) && (xpr2 == null || !xpr2.red)) {
                                xpl.red = true;
                                x = xp;
                            } else {
                                if (treeNode == null || !treeNode.red) {
                                    if (xpr2 != null) {
                                        xpr2.red = $assertionsDisabled;
                                    }
                                    xpl.red = true;
                                    root = rotateLeft(root, xpl);
                                    treeNode2 = x.parent;
                                    xp = treeNode2;
                                    if (treeNode2 != null) {
                                        xpr = xp.left;
                                    }
                                    xpl = xpr;
                                }
                                if (xpl != null) {
                                    xpl.red = xp == null ? $assertionsDisabled : xp.red;
                                    xpr = xpl.left;
                                    treeNode = xpr;
                                    if (xpr != null) {
                                        treeNode.red = $assertionsDisabled;
                                    }
                                }
                                if (xp != null) {
                                    xp.red = $assertionsDisabled;
                                    root = rotateRight(root, xp);
                                }
                                x = root;
                            }
                        }
                    }
                }
            }
            return root;
        }

        static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tp = t.parent;
            TreeNode<K, V> tl = t.left;
            TreeNode<K, V> tr = t.right;
            TreeNode<K, V> tb = t.prev;
            TreeNode<K, V> tn = t.next;
            if (tb != null && tb.next != t) {
                return $assertionsDisabled;
            }
            if (tn != null && tn.prev != t) {
                return $assertionsDisabled;
            }
            if (tp != null && t != tp.left && t != tp.right) {
                return $assertionsDisabled;
            }
            if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
                return $assertionsDisabled;
            }
            if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
                return $assertionsDisabled;
            }
            if (t.red && tl != null && tl.red && tr != null && tr.red) {
                return $assertionsDisabled;
            }
            if (tl != null && !checkInvariants(tl)) {
                return $assertionsDisabled;
            }
            if (tr == null || checkInvariants(tr)) {
                return true;
            }
            return $assertionsDisabled;
        }
    }

    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> left;
        TreeNode<K, V> parent;
        TreeNode<K, V> prev;
        boolean red;
        TreeNode<K, V> right;

        TreeNode(int hash, K key, V val, Node<K, V> next, TreeNode<K, V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        Node<K, V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /* JADX WARNING: Missing block: B:18:0x0030, code skipped:
            if (r3 != null) goto L_0x0032;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final TreeNode<K, V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                Class<?> kc2 = kc;
                kc = this;
                do {
                    TreeNode<K, V> pl = kc.left;
                    TreeNode<K, V> pr = kc.right;
                    int i = kc.hash;
                    int ph = i;
                    if (i > h) {
                        kc = pl;
                        continue;
                    } else if (ph < h) {
                        kc = pr;
                        continue;
                    } else {
                        K k2 = kc.key;
                        K pk = k2;
                        if (k2 == k || (pk != null && k.equals(pk))) {
                            return kc;
                        }
                        if (pl == null) {
                            kc = pr;
                            continue;
                        } else if (pr == null) {
                            kc = pl;
                            continue;
                        } else {
                            if (kc2 == null) {
                                Class<?> comparableClassFor = ConcurrentHashMap.comparableClassFor(k);
                                kc2 = comparableClassFor;
                            }
                            i = ConcurrentHashMap.compareComparables(kc2, k, pk);
                            int dir = i;
                            if (i != 0) {
                                kc = dir < 0 ? pl : pr;
                                continue;
                            }
                            TreeNode<K, V> findTreeNode = pr.findTreeNode(h, k, kc2);
                            TreeNode<K, V> q = findTreeNode;
                            if (findTreeNode != null) {
                                return q;
                            }
                            kc = pl;
                            continue;
                        }
                    }
                } while (kc != null);
            }
            return null;
        }
    }

    static final class ValueIterator<K, V> extends BaseIterator<K, V> implements Iterator<V>, Enumeration<V> {
        ValueIterator(Node<K, V>[] tab, int index, int size, int limit, ConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        public final V next() {
            Node<K, V> node = this.next;
            Node<K, V> p = node;
            if (node != null) {
                V v = p.val;
                this.lastReturned = p;
                advance();
                return v;
            }
            throw new NoSuchElementException();
        }

        public final V nextElement() {
            return next();
        }
    }

    static abstract class BulkTask<K, V, R> extends CountedCompleter<R> {
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int batch;
        int index;
        Node<K, V> next;
        TableStack<K, V> spare;
        TableStack<K, V> stack;
        Node<K, V>[] tab;

        BulkTask(BulkTask<K, V, ?> par, int b, int i, int f, Node<K, V>[] t) {
            super(par);
            this.batch = b;
            this.baseIndex = i;
            this.index = i;
            this.tab = t;
            if (t == null) {
                this.baseLimit = 0;
                this.baseSize = 0;
            } else if (par == null) {
                int length = t.length;
                this.baseLimit = length;
                this.baseSize = length;
            } else {
                this.baseLimit = f;
                this.baseSize = par.baseSize;
            }
        }

        final Node<K, V> advance() {
            Node<K, V> node = this.next;
            Node<K, V> e = node;
            if (node != null) {
                e = e.next;
            }
            while (e == null) {
                if (this.baseIndex < this.baseLimit) {
                    Node<K, V>[] nodeArr = this.tab;
                    Node<K, V>[] t = nodeArr;
                    if (nodeArr != null) {
                        int length = t.length;
                        int n = length;
                        int i = this.index;
                        int i2 = i;
                        if (length > i && i2 >= 0) {
                            node = ConcurrentHashMap.tabAt(t, i2);
                            e = node;
                            if (node != null && e.hash < 0) {
                                if (e instanceof ForwardingNode) {
                                    this.tab = ((ForwardingNode) e).nextTable;
                                    e = null;
                                    pushState(t, i2, n);
                                } else {
                                    if (e instanceof TreeBin) {
                                        node = ((TreeBin) e).first;
                                    } else {
                                        node = null;
                                    }
                                    e = node;
                                }
                            }
                            if (this.stack != null) {
                                recoverState(n);
                            } else {
                                length = this.baseSize + i2;
                                this.index = length;
                                if (length >= n) {
                                    length = this.baseIndex + 1;
                                    this.baseIndex = length;
                                    this.index = length;
                                }
                            }
                        }
                    }
                }
                this.next = null;
                return null;
            }
            this.next = e;
            return e;
        }

        private void pushState(Node<K, V>[] t, int i, int n) {
            TableStack<K, V> s = this.spare;
            if (s != null) {
                this.spare = s.next;
            } else {
                s = new TableStack();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = this.stack;
            this.stack = s;
        }

        private void recoverState(int n) {
            TableStack<K, V> s;
            int i;
            while (true) {
                TableStack<K, V> tableStack = this.stack;
                s = tableStack;
                if (tableStack == null) {
                    break;
                }
                i = this.index;
                int i2 = s.length;
                int len = i2;
                i += i2;
                this.index = i;
                if (i < n) {
                    break;
                }
                n = len;
                this.index = s.index;
                this.tab = s.tab;
                s.tab = null;
                tableStack = s.next;
                s.next = this.spare;
                this.stack = tableStack;
                this.spare = s;
            }
            if (s == null) {
                i = this.index + this.baseSize;
                this.index = i;
                if (i >= n) {
                    i = this.baseIndex + 1;
                    this.baseIndex = i;
                    this.index = i;
                }
            }
        }
    }

    static final class EntrySetView<K, V> extends CollectionView<K, V, Entry<K, V>> implements Set<Entry<K, V>>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        EntrySetView(ConcurrentHashMap<K, V> map) {
            super(map);
        }

        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry) o;
                Entry<?, ?> e = entry;
                Object key = entry.getKey();
                Object k = key;
                if (key != null) {
                    key = this.map.get(k);
                    Object r = key;
                    if (key != null) {
                        key = e.getValue();
                        Object v = key;
                        if (key != null && (v == r || v.equals(r))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public boolean remove(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry) o;
                Entry<?, ?> e = entry;
                Object key = entry.getKey();
                Object k = key;
                if (key != null) {
                    key = e.getValue();
                    Object v = key;
                    if (key != null && this.map.remove(k, v)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Iterator<Entry<K, V>> iterator() {
            ConcurrentHashMap<K, V> m = this.map;
            Node<K, V>[] nodeArr = m.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            return new EntryIterator(t, f, 0, f, m);
        }

        public boolean add(Entry<K, V> e) {
            return this.map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            boolean added = false;
            for (Entry e : c) {
                if (add(e)) {
                    added = true;
                }
            }
            return added;
        }

        public boolean removeIf(Predicate<? super Entry<K, V>> filter) {
            return this.map.removeEntryIf(filter);
        }

        public final int hashCode() {
            int h = 0;
            Node<K, V>[] nodeArr = this.map.table;
            Node<K, V>[] t = nodeArr;
            if (nodeArr != null) {
                Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> advance = it.advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    h += p.hashCode();
                }
            }
            return h;
        }

        public final boolean equals(Object o) {
            if (o instanceof Set) {
                Object obj = (Set) o;
                Set<?> c = obj;
                if (obj == this || (containsAll(c) && c.containsAll(this))) {
                    return true;
                }
            }
            return false;
        }

        public Spliterator<Entry<K, V>> spliterator() {
            ConcurrentHashMap<K, V> m = this.map;
            long n = m.sumCount();
            Node<K, V>[] nodeArr = m.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            return new EntrySpliterator(t, f, 0, f, n < 0 ? 0 : n, m);
        }

        public void forEach(Consumer<? super Entry<K, V>> action) {
            if (action != null) {
                Node<K, V>[] nodeArr = this.map.table;
                Node<K, V>[] t = nodeArr;
                if (nodeArr != null) {
                    Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                    while (true) {
                        Node<K, V> advance = it.advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            action.accept(new MapEntry(p.key, p.val, this.map));
                        } else {
                            return;
                        }
                    }
                }
                return;
            }
            throw new NullPointerException();
        }
    }

    public static class KeySetView<K, V> extends CollectionView<K, V, K> implements Set<K>, Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;

        public /* bridge */ /* synthetic */ ConcurrentHashMap getMap() {
            return super.getMap();
        }

        KeySetView(ConcurrentHashMap<K, V> map, V value) {
            super(map);
            this.value = value;
        }

        public V getMappedValue() {
            return this.value;
        }

        public boolean contains(Object o) {
            return this.map.containsKey(o);
        }

        public boolean remove(Object o) {
            return this.map.remove(o) != null;
        }

        public Iterator<K> iterator() {
            ConcurrentHashMap<K, V> m = this.map;
            Node<K, V>[] nodeArr = m.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            return new KeyIterator(t, f, 0, f, m);
        }

        public boolean add(K e) {
            V v = this.value;
            V v2 = v;
            if (v != null) {
                return this.map.putVal(e, v2, true) == null;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            V v = this.value;
            V v2 = v;
            if (v != null) {
                for (K e : c) {
                    if (this.map.putVal(e, v2, true) == null) {
                        added = true;
                    }
                }
                return added;
            }
            throw new UnsupportedOperationException();
        }

        public int hashCode() {
            int h = 0;
            Iterator it = iterator();
            while (it.hasNext()) {
                h += it.next().hashCode();
            }
            return h;
        }

        public boolean equals(Object o) {
            if (o instanceof Set) {
                Object obj = (Set) o;
                Set<?> c = obj;
                if (obj == this || (containsAll(c) && c.containsAll(this))) {
                    return true;
                }
            }
            return false;
        }

        public Spliterator<K> spliterator() {
            ConcurrentHashMap<K, V> m = this.map;
            long n = m.sumCount();
            Node<K, V>[] nodeArr = m.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            return new KeySpliterator(t, f, 0, f, n < 0 ? 0 : n);
        }

        public void forEach(Consumer<? super K> action) {
            if (action != null) {
                Node<K, V>[] nodeArr = this.map.table;
                Node<K, V>[] t = nodeArr;
                if (nodeArr != null) {
                    Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                    while (true) {
                        Node<K, V> advance = it.advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            action.accept(p.key);
                        } else {
                            return;
                        }
                    }
                }
                return;
            }
            throw new NullPointerException();
        }
    }

    static final class ValuesView<K, V> extends CollectionView<K, V, V> implements Collection<V>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        ValuesView(ConcurrentHashMap<K, V> map) {
            super(map);
        }

        public final boolean contains(Object o) {
            return this.map.containsValue(o);
        }

        public final boolean remove(Object o) {
            if (o != null) {
                Iterator<V> it = iterator();
                while (it.hasNext()) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        public final Iterator<V> iterator() {
            ConcurrentHashMap<K, V> m = this.map;
            Node<K, V>[] nodeArr = m.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            return new ValueIterator(t, f, 0, f, m);
        }

        public final boolean add(V v) {
            throw new UnsupportedOperationException();
        }

        public final boolean addAll(Collection<? extends V> collection) {
            throw new UnsupportedOperationException();
        }

        public boolean removeIf(Predicate<? super V> filter) {
            return this.map.removeValueIf(filter);
        }

        public Spliterator<V> spliterator() {
            ConcurrentHashMap<K, V> m = this.map;
            long n = m.sumCount();
            Node<K, V>[] nodeArr = m.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            return new ValueSpliterator(t, f, 0, f, n < 0 ? 0 : n);
        }

        public void forEach(Consumer<? super V> action) {
            if (action != null) {
                Node<K, V>[] nodeArr = this.map.table;
                Node<K, V>[] t = nodeArr;
                if (nodeArr != null) {
                    Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                    while (true) {
                        Node<K, V> advance = it.advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            action.accept(p.val);
                        } else {
                            return;
                        }
                    }
                }
                return;
            }
            throw new NullPointerException();
        }
    }

    static final class ForEachEntryTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super Entry<K, V>> action;

        ForEachEntryTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Consumer<? super Entry<K, V>> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        public final void compute() {
            Consumer<? super Entry<K, V>> consumer = this.action;
            Consumer<? super Entry<K, V>> action = consumer;
            if (consumer != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    new ForEachEntryTask(this, i3, h, f, this.tab, action).fork();
                }
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p);
                    } else {
                        propagateCompletion();
                        return;
                    }
                }
            }
        }
    }

    static final class ForEachKeyTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super K> action;

        ForEachKeyTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Consumer<? super K> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        public final void compute() {
            Consumer<? super K> consumer = this.action;
            Consumer<? super K> action = consumer;
            if (consumer != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    new ForEachKeyTask(this, i3, h, f, this.tab, action).fork();
                }
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p.key);
                    } else {
                        propagateCompletion();
                        return;
                    }
                }
            }
        }
    }

    static final class ForEachMappingTask<K, V> extends BulkTask<K, V, Void> {
        final BiConsumer<? super K, ? super V> action;

        ForEachMappingTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, BiConsumer<? super K, ? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        public final void compute() {
            BiConsumer<? super K, ? super V> biConsumer = this.action;
            BiConsumer<? super K, ? super V> action = biConsumer;
            if (biConsumer != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    new ForEachMappingTask(this, i3, h, f, this.tab, action).fork();
                }
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p.key, p.val);
                    } else {
                        propagateCompletion();
                        return;
                    }
                }
            }
        }
    }

    static final class ForEachTransformedEntryTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<Entry<K, V>, ? extends U> transformer;

        ForEachTransformedEntryTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<Entry<K, V>, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        public final void compute() {
            Function<Entry<K, V>, ? extends U> function = this.transformer;
            Function<Entry<K, V>, ? extends U> transformer = function;
            if (function != null) {
                Consumer<? super U> consumer = this.action;
                Consumer<? super U> action = consumer;
                if (consumer != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        new ForEachTransformedEntryTask(this, i3, h, f, this.tab, transformer, action).fork();
                    }
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            U apply = transformer.apply(p);
                            U u = apply;
                            if (apply != null) {
                                action.accept(u);
                            }
                        } else {
                            propagateCompletion();
                            return;
                        }
                    }
                }
            }
        }
    }

    static final class ForEachTransformedKeyTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<? super K, ? extends U> transformer;

        ForEachTransformedKeyTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        public final void compute() {
            Function<? super K, ? extends U> function = this.transformer;
            Function<? super K, ? extends U> transformer = function;
            if (function != null) {
                Consumer<? super U> consumer = this.action;
                Consumer<? super U> action = consumer;
                if (consumer != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        new ForEachTransformedKeyTask(this, i3, h, f, this.tab, transformer, action).fork();
                    }
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            U apply = transformer.apply(p.key);
                            U u = apply;
                            if (apply != null) {
                                action.accept(u);
                            }
                        } else {
                            propagateCompletion();
                            return;
                        }
                    }
                }
            }
        }
    }

    static final class ForEachTransformedMappingTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final BiFunction<? super K, ? super V, ? extends U> transformer;

        ForEachTransformedMappingTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, BiFunction<? super K, ? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        public final void compute() {
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.transformer;
            BiFunction<? super K, ? super V, ? extends U> transformer = biFunction;
            if (biFunction != null) {
                Consumer<? super U> consumer = this.action;
                Consumer<? super U> action = consumer;
                if (consumer != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        new ForEachTransformedMappingTask(this, i3, h, f, this.tab, transformer, action).fork();
                    }
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            U apply = transformer.apply(p.key, p.val);
                            U u = apply;
                            if (apply != null) {
                                action.accept(u);
                            }
                        } else {
                            propagateCompletion();
                            return;
                        }
                    }
                }
            }
        }
    }

    static final class ForEachTransformedValueTask<K, V, U> extends BulkTask<K, V, Void> {
        final Consumer<? super U> action;
        final Function<? super V, ? extends U> transformer;

        ForEachTransformedValueTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        public final void compute() {
            Function<? super V, ? extends U> function = this.transformer;
            Function<? super V, ? extends U> transformer = function;
            if (function != null) {
                Consumer<? super U> consumer = this.action;
                Consumer<? super U> action = consumer;
                if (consumer != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        new ForEachTransformedValueTask(this, i3, h, f, this.tab, transformer, action).fork();
                    }
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance != null) {
                            U apply = transformer.apply(p.val);
                            U u = apply;
                            if (apply != null) {
                                action.accept(u);
                            }
                        } else {
                            propagateCompletion();
                            return;
                        }
                    }
                }
            }
        }
    }

    static final class ForEachValueTask<K, V> extends BulkTask<K, V, Void> {
        final Consumer<? super V> action;

        ForEachValueTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Consumer<? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        public final void compute() {
            Consumer<? super V> consumer = this.action;
            Consumer<? super V> action = consumer;
            if (consumer != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    new ForEachValueTask(this, i3, h, f, this.tab, action).fork();
                }
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p.val);
                    } else {
                        propagateCompletion();
                        return;
                    }
                }
            }
        }
    }

    static final class MapReduceEntriesTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceEntriesTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceEntriesTask<K, V, U> rights;
        final Function<Entry<K, V>, ? extends U> transformer;

        MapReduceEntriesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesTask<K, V, U> nextRight, Function<Entry<K, V>, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        public final U getRawResult() {
            return this.result;
        }

        public final void compute() {
            Function<Entry<K, V>, ? extends U> function = this.transformer;
            Function<Entry<K, V>, ? extends U> transformer = function;
            if (function != null) {
                BiFunction<? super U, ? super U, ? extends U> biFunction = this.reducer;
                BiFunction<? super U, ? super U, ? extends U> reducer = biFunction;
                if (biFunction != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        MapReduceEntriesTask mapReduceEntriesTask = new MapReduceEntriesTask(this, i3, h, f, this.tab, this.rights, transformer, reducer);
                        this.rights = mapReduceEntriesTask;
                        mapReduceEntriesTask.fork();
                    }
                    U r = null;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        U apply = transformer.apply(p);
                        U u = apply;
                        if (apply != null) {
                            r = r == null ? u : reducer.apply(r, u);
                        }
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceEntriesTask<K, V, U> t = (MapReduceEntriesTask) c;
                        MapReduceEntriesTask<K, V, U> s = t.rights;
                        while (s != null) {
                            U u2 = s.result;
                            U sr = u2;
                            if (u2 != null) {
                                u2 = t.result;
                                t.result = u2 == null ? sr : reducer.apply(u2, sr);
                            }
                            MapReduceEntriesTask<K, V, U> mapReduceEntriesTask2 = s.nextRight;
                            t.rights = mapReduceEntriesTask2;
                            s = mapReduceEntriesTask2;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceEntriesToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceEntriesToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceEntriesToDoubleTask<K, V> rights;
        final ToDoubleFunction<Entry<K, V>> transformer;

        MapReduceEntriesToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesToDoubleTask<K, V> nextRight, ToDoubleFunction<Entry<K, V>> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        public final void compute() {
            ToDoubleFunction<Entry<K, V>> toDoubleFunction = this.transformer;
            ToDoubleFunction<Entry<K, V>> transformer = toDoubleFunction;
            if (toDoubleFunction != null) {
                DoubleBinaryOperator doubleBinaryOperator = this.reducer;
                DoubleBinaryOperator reducer = doubleBinaryOperator;
                if (doubleBinaryOperator != null) {
                    ToDoubleFunction<Entry<K, V>> transformer2;
                    double r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToDoubleFunction<Entry<K, V>> toDoubleFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceEntriesToDoubleTask mapReduceEntriesToDoubleTask = r0;
                        int i4 = i2;
                        MapReduceEntriesToDoubleTask mapReduceEntriesToDoubleTask2 = new MapReduceEntriesToDoubleTask(this, i3, h, f, this.tab, this.rights, toDoubleFunction2, r, reducer);
                        this.rights = mapReduceEntriesToDoubleTask;
                        mapReduceEntriesToDoubleTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsDouble(r, transformer2.applyAsDouble(p));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceEntriesToDoubleTask<K, V> t = (MapReduceEntriesToDoubleTask) c;
                        MapReduceEntriesToDoubleTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsDouble(t.result, s.result);
                            MapReduceEntriesToDoubleTask<K, V> mapReduceEntriesToDoubleTask3 = s.nextRight;
                            t.rights = mapReduceEntriesToDoubleTask3;
                            s = mapReduceEntriesToDoubleTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceEntriesToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceEntriesToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceEntriesToIntTask<K, V> rights;
        final ToIntFunction<Entry<K, V>> transformer;

        MapReduceEntriesToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesToIntTask<K, V> nextRight, ToIntFunction<Entry<K, V>> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        public final void compute() {
            ToIntFunction<Entry<K, V>> toIntFunction = this.transformer;
            ToIntFunction<Entry<K, V>> transformer = toIntFunction;
            if (toIntFunction != null) {
                IntBinaryOperator intBinaryOperator = this.reducer;
                IntBinaryOperator reducer = intBinaryOperator;
                if (intBinaryOperator != null) {
                    int r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        int i4 = r;
                        int r2 = r;
                        MapReduceEntriesToIntTask mapReduceEntriesToIntTask = r0;
                        MapReduceEntriesToIntTask mapReduceEntriesToIntTask2 = new MapReduceEntriesToIntTask(this, i3, h, f, this.tab, this.rights, transformer, i4, reducer);
                        this.rights = mapReduceEntriesToIntTask;
                        mapReduceEntriesToIntTask.fork();
                        i = i2;
                        r = r2;
                    }
                    i = r;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        i = reducer.applyAsInt(i, transformer.applyAsInt(p));
                    }
                    this.result = i;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceEntriesToIntTask<K, V> t = (MapReduceEntriesToIntTask) c;
                        MapReduceEntriesToIntTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsInt(t.result, s.result);
                            MapReduceEntriesToIntTask<K, V> mapReduceEntriesToIntTask3 = s.nextRight;
                            t.rights = mapReduceEntriesToIntTask3;
                            s = mapReduceEntriesToIntTask3;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceEntriesToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceEntriesToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceEntriesToLongTask<K, V> rights;
        final ToLongFunction<Entry<K, V>> transformer;

        MapReduceEntriesToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceEntriesToLongTask<K, V> nextRight, ToLongFunction<Entry<K, V>> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        public final void compute() {
            ToLongFunction<Entry<K, V>> toLongFunction = this.transformer;
            ToLongFunction<Entry<K, V>> transformer = toLongFunction;
            if (toLongFunction != null) {
                LongBinaryOperator longBinaryOperator = this.reducer;
                LongBinaryOperator reducer = longBinaryOperator;
                if (longBinaryOperator != null) {
                    ToLongFunction<Entry<K, V>> transformer2;
                    long r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToLongFunction<Entry<K, V>> toLongFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceEntriesToLongTask mapReduceEntriesToLongTask = r0;
                        int i4 = i2;
                        MapReduceEntriesToLongTask mapReduceEntriesToLongTask2 = new MapReduceEntriesToLongTask(this, i3, h, f, this.tab, this.rights, toLongFunction2, r, reducer);
                        this.rights = mapReduceEntriesToLongTask;
                        mapReduceEntriesToLongTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsLong(r, transformer2.applyAsLong(p));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceEntriesToLongTask<K, V> t = (MapReduceEntriesToLongTask) c;
                        MapReduceEntriesToLongTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsLong(t.result, s.result);
                            MapReduceEntriesToLongTask<K, V> mapReduceEntriesToLongTask3 = s.nextRight;
                            t.rights = mapReduceEntriesToLongTask3;
                            s = mapReduceEntriesToLongTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceKeysTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceKeysTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceKeysTask<K, V, U> rights;
        final Function<? super K, ? extends U> transformer;

        MapReduceKeysTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysTask<K, V, U> nextRight, Function<? super K, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        public final U getRawResult() {
            return this.result;
        }

        public final void compute() {
            Function<? super K, ? extends U> function = this.transformer;
            Function<? super K, ? extends U> transformer = function;
            if (function != null) {
                BiFunction<? super U, ? super U, ? extends U> biFunction = this.reducer;
                BiFunction<? super U, ? super U, ? extends U> reducer = biFunction;
                if (biFunction != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        MapReduceKeysTask mapReduceKeysTask = new MapReduceKeysTask(this, i3, h, f, this.tab, this.rights, transformer, reducer);
                        this.rights = mapReduceKeysTask;
                        mapReduceKeysTask.fork();
                    }
                    U r = null;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        U apply = transformer.apply(p.key);
                        U u = apply;
                        if (apply != null) {
                            r = r == null ? u : reducer.apply(r, u);
                        }
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceKeysTask<K, V, U> t = (MapReduceKeysTask) c;
                        MapReduceKeysTask<K, V, U> s = t.rights;
                        while (s != null) {
                            U u2 = s.result;
                            U sr = u2;
                            if (u2 != null) {
                                u2 = t.result;
                                t.result = u2 == null ? sr : reducer.apply(u2, sr);
                            }
                            MapReduceKeysTask<K, V, U> mapReduceKeysTask2 = s.nextRight;
                            t.rights = mapReduceKeysTask2;
                            s = mapReduceKeysTask2;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceKeysToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceKeysToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceKeysToDoubleTask<K, V> rights;
        final ToDoubleFunction<? super K> transformer;

        MapReduceKeysToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysToDoubleTask<K, V> nextRight, ToDoubleFunction<? super K> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        public final void compute() {
            ToDoubleFunction<? super K> toDoubleFunction = this.transformer;
            ToDoubleFunction<? super K> transformer = toDoubleFunction;
            if (toDoubleFunction != null) {
                DoubleBinaryOperator doubleBinaryOperator = this.reducer;
                DoubleBinaryOperator reducer = doubleBinaryOperator;
                if (doubleBinaryOperator != null) {
                    ToDoubleFunction<? super K> transformer2;
                    double r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToDoubleFunction<? super K> toDoubleFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceKeysToDoubleTask mapReduceKeysToDoubleTask = r0;
                        int i4 = i2;
                        MapReduceKeysToDoubleTask mapReduceKeysToDoubleTask2 = new MapReduceKeysToDoubleTask(this, i3, h, f, this.tab, this.rights, toDoubleFunction2, r, reducer);
                        this.rights = mapReduceKeysToDoubleTask;
                        mapReduceKeysToDoubleTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsDouble(r, transformer2.applyAsDouble(p.key));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceKeysToDoubleTask<K, V> t = (MapReduceKeysToDoubleTask) c;
                        MapReduceKeysToDoubleTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsDouble(t.result, s.result);
                            MapReduceKeysToDoubleTask<K, V> mapReduceKeysToDoubleTask3 = s.nextRight;
                            t.rights = mapReduceKeysToDoubleTask3;
                            s = mapReduceKeysToDoubleTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceKeysToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceKeysToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceKeysToIntTask<K, V> rights;
        final ToIntFunction<? super K> transformer;

        MapReduceKeysToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysToIntTask<K, V> nextRight, ToIntFunction<? super K> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        public final void compute() {
            ToIntFunction<? super K> toIntFunction = this.transformer;
            ToIntFunction<? super K> transformer = toIntFunction;
            if (toIntFunction != null) {
                IntBinaryOperator intBinaryOperator = this.reducer;
                IntBinaryOperator reducer = intBinaryOperator;
                if (intBinaryOperator != null) {
                    int r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        int i4 = r;
                        int r2 = r;
                        MapReduceKeysToIntTask mapReduceKeysToIntTask = r0;
                        MapReduceKeysToIntTask mapReduceKeysToIntTask2 = new MapReduceKeysToIntTask(this, i3, h, f, this.tab, this.rights, transformer, i4, reducer);
                        this.rights = mapReduceKeysToIntTask;
                        mapReduceKeysToIntTask.fork();
                        i = i2;
                        r = r2;
                    }
                    i = r;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        i = reducer.applyAsInt(i, transformer.applyAsInt(p.key));
                    }
                    this.result = i;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceKeysToIntTask<K, V> t = (MapReduceKeysToIntTask) c;
                        MapReduceKeysToIntTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsInt(t.result, s.result);
                            MapReduceKeysToIntTask<K, V> mapReduceKeysToIntTask3 = s.nextRight;
                            t.rights = mapReduceKeysToIntTask3;
                            s = mapReduceKeysToIntTask3;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceKeysToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceKeysToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceKeysToLongTask<K, V> rights;
        final ToLongFunction<? super K> transformer;

        MapReduceKeysToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceKeysToLongTask<K, V> nextRight, ToLongFunction<? super K> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        public final void compute() {
            ToLongFunction<? super K> toLongFunction = this.transformer;
            ToLongFunction<? super K> transformer = toLongFunction;
            if (toLongFunction != null) {
                LongBinaryOperator longBinaryOperator = this.reducer;
                LongBinaryOperator reducer = longBinaryOperator;
                if (longBinaryOperator != null) {
                    ToLongFunction<? super K> transformer2;
                    long r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToLongFunction<? super K> toLongFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceKeysToLongTask mapReduceKeysToLongTask = r0;
                        int i4 = i2;
                        MapReduceKeysToLongTask mapReduceKeysToLongTask2 = new MapReduceKeysToLongTask(this, i3, h, f, this.tab, this.rights, toLongFunction2, r, reducer);
                        this.rights = mapReduceKeysToLongTask;
                        mapReduceKeysToLongTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsLong(r, transformer2.applyAsLong(p.key));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceKeysToLongTask<K, V> t = (MapReduceKeysToLongTask) c;
                        MapReduceKeysToLongTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsLong(t.result, s.result);
                            MapReduceKeysToLongTask<K, V> mapReduceKeysToLongTask3 = s.nextRight;
                            t.rights = mapReduceKeysToLongTask3;
                            s = mapReduceKeysToLongTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceMappingsTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceMappingsTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceMappingsTask<K, V, U> rights;
        final BiFunction<? super K, ? super V, ? extends U> transformer;

        MapReduceMappingsTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsTask<K, V, U> nextRight, BiFunction<? super K, ? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        public final U getRawResult() {
            return this.result;
        }

        public final void compute() {
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.transformer;
            BiFunction<? super K, ? super V, ? extends U> transformer = biFunction;
            if (biFunction != null) {
                BiFunction<? super U, ? super U, ? extends U> biFunction2 = this.reducer;
                BiFunction<? super U, ? super U, ? extends U> reducer = biFunction2;
                if (biFunction2 != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        MapReduceMappingsTask mapReduceMappingsTask = new MapReduceMappingsTask(this, i3, h, f, this.tab, this.rights, transformer, reducer);
                        this.rights = mapReduceMappingsTask;
                        mapReduceMappingsTask.fork();
                    }
                    U r = null;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        U apply = transformer.apply(p.key, p.val);
                        U u = apply;
                        if (apply != null) {
                            r = r == null ? u : reducer.apply(r, u);
                        }
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceMappingsTask<K, V, U> t = (MapReduceMappingsTask) c;
                        MapReduceMappingsTask<K, V, U> s = t.rights;
                        while (s != null) {
                            U u2 = s.result;
                            U sr = u2;
                            if (u2 != null) {
                                u2 = t.result;
                                t.result = u2 == null ? sr : reducer.apply(u2, sr);
                            }
                            MapReduceMappingsTask<K, V, U> mapReduceMappingsTask2 = s.nextRight;
                            t.rights = mapReduceMappingsTask2;
                            s = mapReduceMappingsTask2;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceMappingsToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceMappingsToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceMappingsToDoubleTask<K, V> rights;
        final ToDoubleBiFunction<? super K, ? super V> transformer;

        MapReduceMappingsToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsToDoubleTask<K, V> nextRight, ToDoubleBiFunction<? super K, ? super V> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        public final void compute() {
            ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction = this.transformer;
            ToDoubleBiFunction<? super K, ? super V> transformer = toDoubleBiFunction;
            if (toDoubleBiFunction != null) {
                DoubleBinaryOperator doubleBinaryOperator = this.reducer;
                DoubleBinaryOperator reducer = doubleBinaryOperator;
                if (doubleBinaryOperator != null) {
                    ToDoubleBiFunction<? super K, ? super V> transformer2;
                    double r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceMappingsToDoubleTask mapReduceMappingsToDoubleTask = r0;
                        int i4 = i2;
                        MapReduceMappingsToDoubleTask mapReduceMappingsToDoubleTask2 = new MapReduceMappingsToDoubleTask(this, i3, h, f, this.tab, this.rights, toDoubleBiFunction2, r, reducer);
                        this.rights = mapReduceMappingsToDoubleTask;
                        mapReduceMappingsToDoubleTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsDouble(r, transformer2.applyAsDouble(p.key, p.val));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceMappingsToDoubleTask<K, V> t = (MapReduceMappingsToDoubleTask) c;
                        MapReduceMappingsToDoubleTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsDouble(t.result, s.result);
                            MapReduceMappingsToDoubleTask<K, V> mapReduceMappingsToDoubleTask3 = s.nextRight;
                            t.rights = mapReduceMappingsToDoubleTask3;
                            s = mapReduceMappingsToDoubleTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceMappingsToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceMappingsToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceMappingsToIntTask<K, V> rights;
        final ToIntBiFunction<? super K, ? super V> transformer;

        MapReduceMappingsToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsToIntTask<K, V> nextRight, ToIntBiFunction<? super K, ? super V> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        public final void compute() {
            ToIntBiFunction<? super K, ? super V> toIntBiFunction = this.transformer;
            ToIntBiFunction<? super K, ? super V> transformer = toIntBiFunction;
            if (toIntBiFunction != null) {
                IntBinaryOperator intBinaryOperator = this.reducer;
                IntBinaryOperator reducer = intBinaryOperator;
                if (intBinaryOperator != null) {
                    int r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        int i4 = r;
                        int r2 = r;
                        MapReduceMappingsToIntTask mapReduceMappingsToIntTask = r0;
                        MapReduceMappingsToIntTask mapReduceMappingsToIntTask2 = new MapReduceMappingsToIntTask(this, i3, h, f, this.tab, this.rights, transformer, i4, reducer);
                        this.rights = mapReduceMappingsToIntTask;
                        mapReduceMappingsToIntTask.fork();
                        i = i2;
                        r = r2;
                    }
                    i = r;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        i = reducer.applyAsInt(i, transformer.applyAsInt(p.key, p.val));
                    }
                    this.result = i;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceMappingsToIntTask<K, V> t = (MapReduceMappingsToIntTask) c;
                        MapReduceMappingsToIntTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsInt(t.result, s.result);
                            MapReduceMappingsToIntTask<K, V> mapReduceMappingsToIntTask3 = s.nextRight;
                            t.rights = mapReduceMappingsToIntTask3;
                            s = mapReduceMappingsToIntTask3;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceMappingsToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceMappingsToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceMappingsToLongTask<K, V> rights;
        final ToLongBiFunction<? super K, ? super V> transformer;

        MapReduceMappingsToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceMappingsToLongTask<K, V> nextRight, ToLongBiFunction<? super K, ? super V> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        public final void compute() {
            ToLongBiFunction<? super K, ? super V> toLongBiFunction = this.transformer;
            ToLongBiFunction<? super K, ? super V> transformer = toLongBiFunction;
            if (toLongBiFunction != null) {
                LongBinaryOperator longBinaryOperator = this.reducer;
                LongBinaryOperator reducer = longBinaryOperator;
                if (longBinaryOperator != null) {
                    ToLongBiFunction<? super K, ? super V> transformer2;
                    long r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToLongBiFunction<? super K, ? super V> toLongBiFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceMappingsToLongTask mapReduceMappingsToLongTask = r0;
                        int i4 = i2;
                        MapReduceMappingsToLongTask mapReduceMappingsToLongTask2 = new MapReduceMappingsToLongTask(this, i3, h, f, this.tab, this.rights, toLongBiFunction2, r, reducer);
                        this.rights = mapReduceMappingsToLongTask;
                        mapReduceMappingsToLongTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsLong(r, transformer2.applyAsLong(p.key, p.val));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceMappingsToLongTask<K, V> t = (MapReduceMappingsToLongTask) c;
                        MapReduceMappingsToLongTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsLong(t.result, s.result);
                            MapReduceMappingsToLongTask<K, V> mapReduceMappingsToLongTask3 = s.nextRight;
                            t.rights = mapReduceMappingsToLongTask3;
                            s = mapReduceMappingsToLongTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceValuesTask<K, V, U> extends BulkTask<K, V, U> {
        MapReduceValuesTask<K, V, U> nextRight;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceValuesTask<K, V, U> rights;
        final Function<? super V, ? extends U> transformer;

        MapReduceValuesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesTask<K, V, U> nextRight, Function<? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        public final U getRawResult() {
            return this.result;
        }

        public final void compute() {
            Function<? super V, ? extends U> function = this.transformer;
            Function<? super V, ? extends U> transformer = function;
            if (function != null) {
                BiFunction<? super U, ? super U, ? extends U> biFunction = this.reducer;
                BiFunction<? super U, ? super U, ? extends U> reducer = biFunction;
                if (biFunction != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        MapReduceValuesTask mapReduceValuesTask = new MapReduceValuesTask(this, i3, h, f, this.tab, this.rights, transformer, reducer);
                        this.rights = mapReduceValuesTask;
                        mapReduceValuesTask.fork();
                    }
                    U r = null;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        U apply = transformer.apply(p.val);
                        U u = apply;
                        if (apply != null) {
                            r = r == null ? u : reducer.apply(r, u);
                        }
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceValuesTask<K, V, U> t = (MapReduceValuesTask) c;
                        MapReduceValuesTask<K, V, U> s = t.rights;
                        while (s != null) {
                            U u2 = s.result;
                            U sr = u2;
                            if (u2 != null) {
                                u2 = t.result;
                                t.result = u2 == null ? sr : reducer.apply(u2, sr);
                            }
                            MapReduceValuesTask<K, V, U> mapReduceValuesTask2 = s.nextRight;
                            t.rights = mapReduceValuesTask2;
                            s = mapReduceValuesTask2;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceValuesToDoubleTask<K, V> extends BulkTask<K, V, Double> {
        final double basis;
        MapReduceValuesToDoubleTask<K, V> nextRight;
        final DoubleBinaryOperator reducer;
        double result;
        MapReduceValuesToDoubleTask<K, V> rights;
        final ToDoubleFunction<? super V> transformer;

        MapReduceValuesToDoubleTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesToDoubleTask<K, V> nextRight, ToDoubleFunction<? super V> transformer, double basis, DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Double getRawResult() {
            return Double.valueOf(this.result);
        }

        public final void compute() {
            ToDoubleFunction<? super V> toDoubleFunction = this.transformer;
            ToDoubleFunction<? super V> transformer = toDoubleFunction;
            if (toDoubleFunction != null) {
                DoubleBinaryOperator doubleBinaryOperator = this.reducer;
                DoubleBinaryOperator reducer = doubleBinaryOperator;
                if (doubleBinaryOperator != null) {
                    ToDoubleFunction<? super V> transformer2;
                    double r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToDoubleFunction<? super V> toDoubleFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceValuesToDoubleTask mapReduceValuesToDoubleTask = r0;
                        int i4 = i2;
                        MapReduceValuesToDoubleTask mapReduceValuesToDoubleTask2 = new MapReduceValuesToDoubleTask(this, i3, h, f, this.tab, this.rights, toDoubleFunction2, r, reducer);
                        this.rights = mapReduceValuesToDoubleTask;
                        mapReduceValuesToDoubleTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsDouble(r, transformer2.applyAsDouble(p.val));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceValuesToDoubleTask<K, V> t = (MapReduceValuesToDoubleTask) c;
                        MapReduceValuesToDoubleTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsDouble(t.result, s.result);
                            MapReduceValuesToDoubleTask<K, V> mapReduceValuesToDoubleTask3 = s.nextRight;
                            t.rights = mapReduceValuesToDoubleTask3;
                            s = mapReduceValuesToDoubleTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class MapReduceValuesToIntTask<K, V> extends BulkTask<K, V, Integer> {
        final int basis;
        MapReduceValuesToIntTask<K, V> nextRight;
        final IntBinaryOperator reducer;
        int result;
        MapReduceValuesToIntTask<K, V> rights;
        final ToIntFunction<? super V> transformer;

        MapReduceValuesToIntTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesToIntTask<K, V> nextRight, ToIntFunction<? super V> transformer, int basis, IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Integer getRawResult() {
            return Integer.valueOf(this.result);
        }

        public final void compute() {
            ToIntFunction<? super V> toIntFunction = this.transformer;
            ToIntFunction<? super V> transformer = toIntFunction;
            if (toIntFunction != null) {
                IntBinaryOperator intBinaryOperator = this.reducer;
                IntBinaryOperator reducer = intBinaryOperator;
                if (intBinaryOperator != null) {
                    int r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        int i4 = r;
                        int r2 = r;
                        MapReduceValuesToIntTask mapReduceValuesToIntTask = r0;
                        MapReduceValuesToIntTask mapReduceValuesToIntTask2 = new MapReduceValuesToIntTask(this, i3, h, f, this.tab, this.rights, transformer, i4, reducer);
                        this.rights = mapReduceValuesToIntTask;
                        mapReduceValuesToIntTask.fork();
                        i = i2;
                        r = r2;
                    }
                    i = r;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        i = reducer.applyAsInt(i, transformer.applyAsInt(p.val));
                    }
                    this.result = i;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceValuesToIntTask<K, V> t = (MapReduceValuesToIntTask) c;
                        MapReduceValuesToIntTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsInt(t.result, s.result);
                            MapReduceValuesToIntTask<K, V> mapReduceValuesToIntTask3 = s.nextRight;
                            t.rights = mapReduceValuesToIntTask3;
                            s = mapReduceValuesToIntTask3;
                        }
                    }
                }
            }
        }
    }

    static final class MapReduceValuesToLongTask<K, V> extends BulkTask<K, V, Long> {
        final long basis;
        MapReduceValuesToLongTask<K, V> nextRight;
        final LongBinaryOperator reducer;
        long result;
        MapReduceValuesToLongTask<K, V> rights;
        final ToLongFunction<? super V> transformer;

        MapReduceValuesToLongTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, MapReduceValuesToLongTask<K, V> nextRight, ToLongFunction<? super V> transformer, long basis, LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Long getRawResult() {
            return Long.valueOf(this.result);
        }

        public final void compute() {
            ToLongFunction<? super V> toLongFunction = this.transformer;
            ToLongFunction<? super V> transformer = toLongFunction;
            if (toLongFunction != null) {
                LongBinaryOperator longBinaryOperator = this.reducer;
                LongBinaryOperator reducer = longBinaryOperator;
                if (longBinaryOperator != null) {
                    ToLongFunction<? super V> transformer2;
                    long r = this.basis;
                    int i = this.baseIndex;
                    while (true) {
                        int i2 = i;
                        if (this.batch <= 0) {
                            break;
                        }
                        i = this.baseLimit;
                        int f = i;
                        i = (i + i2) >>> 1;
                        int h = i;
                        if (i <= i2) {
                            break;
                        }
                        addToPendingCount(1);
                        int i3 = this.batch >>> 1;
                        this.batch = i3;
                        this.baseLimit = h;
                        ToLongFunction<? super V> toLongFunction2 = transformer;
                        transformer2 = transformer;
                        MapReduceValuesToLongTask mapReduceValuesToLongTask = r0;
                        int i4 = i2;
                        MapReduceValuesToLongTask mapReduceValuesToLongTask2 = new MapReduceValuesToLongTask(this, i3, h, f, this.tab, this.rights, toLongFunction2, r, reducer);
                        this.rights = mapReduceValuesToLongTask;
                        mapReduceValuesToLongTask.fork();
                        transformer = transformer2;
                        i = i4;
                    }
                    transformer2 = transformer;
                    while (true) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            break;
                        }
                        r = reducer.applyAsLong(r, transformer2.applyAsLong(p.val));
                    }
                    this.result = r;
                    for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                        MapReduceValuesToLongTask<K, V> t = (MapReduceValuesToLongTask) c;
                        MapReduceValuesToLongTask<K, V> s = t.rights;
                        while (s != null) {
                            t.result = reducer.applyAsLong(t.result, s.result);
                            MapReduceValuesToLongTask<K, V> mapReduceValuesToLongTask3 = s.nextRight;
                            t.rights = mapReduceValuesToLongTask3;
                            s = mapReduceValuesToLongTask3;
                        }
                    }
                    return;
                }
            }
        }
    }

    static final class ReduceEntriesTask<K, V> extends BulkTask<K, V, Entry<K, V>> {
        ReduceEntriesTask<K, V> nextRight;
        final BiFunction<Entry<K, V>, Entry<K, V>, ? extends Entry<K, V>> reducer;
        Entry<K, V> result;
        ReduceEntriesTask<K, V> rights;

        ReduceEntriesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, ReduceEntriesTask<K, V> nextRight, BiFunction<Entry<K, V>, Entry<K, V>, ? extends Entry<K, V>> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        public final Entry<K, V> getRawResult() {
            return this.result;
        }

        public final void compute() {
            BiFunction<Entry<K, V>, Entry<K, V>, ? extends Entry<K, V>> biFunction = this.reducer;
            BiFunction<Entry<K, V>, Entry<K, V>, ? extends Entry<K, V>> reducer = biFunction;
            if (biFunction != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    ReduceEntriesTask reduceEntriesTask = new ReduceEntriesTask(this, i3, h, f, this.tab, this.rights, reducer);
                    this.rights = reduceEntriesTask;
                    reduceEntriesTask.fork();
                }
                Entry<K, V> r = null;
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    r = r == null ? p : (Entry) reducer.apply(r, p);
                }
                this.result = r;
                for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                    ReduceEntriesTask<K, V> t = (ReduceEntriesTask) c;
                    ReduceEntriesTask<K, V> s = t.rights;
                    while (s != null) {
                        Entry<K, V> entry = s.result;
                        Entry<K, V> sr = entry;
                        if (entry != null) {
                            entry = t.result;
                            t.result = entry == null ? sr : (Entry) reducer.apply(entry, sr);
                        }
                        ReduceEntriesTask<K, V> reduceEntriesTask2 = s.nextRight;
                        t.rights = reduceEntriesTask2;
                        s = reduceEntriesTask2;
                    }
                }
            }
        }
    }

    static final class ReduceKeysTask<K, V> extends BulkTask<K, V, K> {
        ReduceKeysTask<K, V> nextRight;
        final BiFunction<? super K, ? super K, ? extends K> reducer;
        K result;
        ReduceKeysTask<K, V> rights;

        ReduceKeysTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, ReduceKeysTask<K, V> nextRight, BiFunction<? super K, ? super K, ? extends K> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        public final K getRawResult() {
            return this.result;
        }

        public final void compute() {
            BiFunction<? super K, ? super K, ? extends K> biFunction = this.reducer;
            BiFunction<? super K, ? super K, ? extends K> reducer = biFunction;
            if (biFunction != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    ReduceKeysTask reduceKeysTask = new ReduceKeysTask(this, i3, h, f, this.tab, this.rights, reducer);
                    this.rights = reduceKeysTask;
                    reduceKeysTask.fork();
                }
                K r = null;
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    K u = p.key;
                    K apply = r == null ? u : u == null ? r : reducer.apply(r, u);
                    r = apply;
                }
                this.result = r;
                for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                    ReduceKeysTask<K, V> t = (ReduceKeysTask) c;
                    ReduceKeysTask<K, V> s = t.rights;
                    while (s != null) {
                        K k = s.result;
                        K sr = k;
                        if (k != null) {
                            k = t.result;
                            t.result = k == null ? sr : reducer.apply(k, sr);
                        }
                        ReduceKeysTask<K, V> reduceKeysTask2 = s.nextRight;
                        t.rights = reduceKeysTask2;
                        s = reduceKeysTask2;
                    }
                }
            }
        }
    }

    static final class ReduceValuesTask<K, V> extends BulkTask<K, V, V> {
        ReduceValuesTask<K, V> nextRight;
        final BiFunction<? super V, ? super V, ? extends V> reducer;
        V result;
        ReduceValuesTask<K, V> rights;

        ReduceValuesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, ReduceValuesTask<K, V> nextRight, BiFunction<? super V, ? super V, ? extends V> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        public final V getRawResult() {
            return this.result;
        }

        public final void compute() {
            BiFunction<? super V, ? super V, ? extends V> biFunction = this.reducer;
            BiFunction<? super V, ? super V, ? extends V> reducer = biFunction;
            if (biFunction != null) {
                int i = this.baseIndex;
                while (this.batch > 0) {
                    int i2 = this.baseLimit;
                    int f = i2;
                    i2 = (i2 + i) >>> 1;
                    int h = i2;
                    if (i2 <= i) {
                        break;
                    }
                    addToPendingCount(1);
                    int i3 = this.batch >>> 1;
                    this.batch = i3;
                    this.baseLimit = h;
                    ReduceValuesTask reduceValuesTask = new ReduceValuesTask(this, i3, h, f, this.tab, this.rights, reducer);
                    this.rights = reduceValuesTask;
                    reduceValuesTask.fork();
                }
                V r = null;
                while (true) {
                    Node<K, V> advance = advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    V v = p.val;
                    r = r == null ? v : reducer.apply(r, v);
                }
                this.result = r;
                for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
                    ReduceValuesTask<K, V> t = (ReduceValuesTask) c;
                    ReduceValuesTask<K, V> s = t.rights;
                    while (s != null) {
                        V v2 = s.result;
                        V sr = v2;
                        if (v2 != null) {
                            v2 = t.result;
                            t.result = v2 == null ? sr : reducer.apply(v2, sr);
                        }
                        ReduceValuesTask<K, V> reduceValuesTask2 = s.nextRight;
                        t.rights = reduceValuesTask2;
                        s = reduceValuesTask2;
                    }
                }
            }
        }
    }

    static final class SearchEntriesTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<Entry<K, V>, ? extends U> searchFunction;

        SearchEntriesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<Entry<K, V>, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return this.result.get();
        }

        public final void compute() {
            Function<Entry<K, V>, ? extends U> function = this.searchFunction;
            Function<Entry<K, V>, ? extends U> searchFunction = function;
            if (function != null) {
                AtomicReference<U> atomicReference = this.result;
                AtomicReference<U> result = atomicReference;
                if (atomicReference != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        } else if (result.get() == null) {
                            addToPendingCount(1);
                            int i3 = this.batch >>> 1;
                            this.batch = i3;
                            this.baseLimit = h;
                            new SearchEntriesTask(this, i3, h, f, this.tab, searchFunction, result).fork();
                        } else {
                            return;
                        }
                    }
                    while (result.get() == null) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            propagateCompletion();
                            break;
                        }
                        U apply = searchFunction.apply(p);
                        U u = apply;
                        if (apply != null) {
                            if (result.compareAndSet(null, u)) {
                                quietlyCompleteRoot();
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    static final class SearchKeysTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<? super K, ? extends U> searchFunction;

        SearchKeysTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super K, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return this.result.get();
        }

        public final void compute() {
            Function<? super K, ? extends U> function = this.searchFunction;
            Function<? super K, ? extends U> searchFunction = function;
            if (function != null) {
                AtomicReference<U> atomicReference = this.result;
                AtomicReference<U> result = atomicReference;
                if (atomicReference != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        } else if (result.get() == null) {
                            addToPendingCount(1);
                            int i3 = this.batch >>> 1;
                            this.batch = i3;
                            this.baseLimit = h;
                            new SearchKeysTask(this, i3, h, f, this.tab, searchFunction, result).fork();
                        } else {
                            return;
                        }
                    }
                    while (result.get() == null) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            propagateCompletion();
                            break;
                        }
                        U apply = searchFunction.apply(p.key);
                        U u = apply;
                        if (apply != null) {
                            if (result.compareAndSet(null, u)) {
                                quietlyCompleteRoot();
                            }
                        }
                    }
                }
            }
        }
    }

    static final class SearchMappingsTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final BiFunction<? super K, ? super V, ? extends U> searchFunction;

        SearchMappingsTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, BiFunction<? super K, ? super V, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return this.result.get();
        }

        public final void compute() {
            BiFunction<? super K, ? super V, ? extends U> biFunction = this.searchFunction;
            BiFunction<? super K, ? super V, ? extends U> searchFunction = biFunction;
            if (biFunction != null) {
                AtomicReference<U> atomicReference = this.result;
                AtomicReference<U> result = atomicReference;
                if (atomicReference != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        } else if (result.get() == null) {
                            addToPendingCount(1);
                            int i3 = this.batch >>> 1;
                            this.batch = i3;
                            this.baseLimit = h;
                            new SearchMappingsTask(this, i3, h, f, this.tab, searchFunction, result).fork();
                        } else {
                            return;
                        }
                    }
                    while (result.get() == null) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            propagateCompletion();
                            break;
                        }
                        U apply = searchFunction.apply(p.key, p.val);
                        U u = apply;
                        if (apply != null) {
                            if (result.compareAndSet(null, u)) {
                                quietlyCompleteRoot();
                            }
                        }
                    }
                }
            }
        }
    }

    static final class SearchValuesTask<K, V, U> extends BulkTask<K, V, U> {
        final AtomicReference<U> result;
        final Function<? super V, ? extends U> searchFunction;

        SearchValuesTask(BulkTask<K, V, ?> p, int b, int i, int f, Node<K, V>[] t, Function<? super V, ? extends U> searchFunction, AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return this.result.get();
        }

        public final void compute() {
            Function<? super V, ? extends U> function = this.searchFunction;
            Function<? super V, ? extends U> searchFunction = function;
            if (function != null) {
                AtomicReference<U> atomicReference = this.result;
                AtomicReference<U> result = atomicReference;
                if (atomicReference != null) {
                    int i = this.baseIndex;
                    while (this.batch > 0) {
                        int i2 = this.baseLimit;
                        int f = i2;
                        i2 = (i2 + i) >>> 1;
                        int h = i2;
                        if (i2 <= i) {
                            break;
                        } else if (result.get() == null) {
                            addToPendingCount(1);
                            int i3 = this.batch >>> 1;
                            this.batch = i3;
                            this.baseLimit = h;
                            new SearchValuesTask(this, i3, h, f, this.tab, searchFunction, result).fork();
                        } else {
                            return;
                        }
                    }
                    while (result.get() == null) {
                        Node<K, V> advance = advance();
                        Node<K, V> p = advance;
                        if (advance == null) {
                            propagateCompletion();
                            break;
                        }
                        U apply = searchFunction.apply(p.val);
                        U u = apply;
                        if (apply != null) {
                            if (result.compareAndSet(null, u)) {
                                quietlyCompleteRoot();
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        try {
            SIZECTL = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset(ConcurrentHashMap.class.getDeclaredField("cellsBusy"));
            CELLVALUE = U.objectFieldOffset(CounterCell.class.getDeclaredField("value"));
            ABASE = U.arrayBaseOffset(Node[].class);
            int scale = U.arrayIndexScale(Node[].class);
            if (((scale - 1) & scale) == 0) {
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
                Class cls = LockSupport.class;
                return;
            }
            throw new Error("array index scale not a power of two");
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static final int spread(int h) {
        return ((h >>> 16) ^ h) & Integer.MAX_VALUE;
    }

    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        if (n < 0) {
            return 1;
        }
        return n >= MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : n + 1;
    }

    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> cls = x.getClass();
            Type c = cls;
            if (cls == String.class) {
                return c;
            }
            Type[] genericInterfaces = c.getGenericInterfaces();
            Type[] ts = genericInterfaces;
            if (genericInterfaces != null) {
                for (Type type : ts) {
                    Type t = type;
                    if (type instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) t;
                        ParameterizedType p = parameterizedType;
                        if (parameterizedType.getRawType() == Comparable.class) {
                            Type[] actualTypeArguments = p.getActualTypeArguments();
                            Type[] as = actualTypeArguments;
                            if (actualTypeArguments != null && as.length == 1 && as[0] == c) {
                                return c;
                            }
                        }
                        continue;
                    }
                }
            }
        }
        return null;
    }

    static int compareComparables(Class<?> kc, Object k, Object x) {
        if (x == null || x.getClass() != kc) {
            return 0;
        }
        return ((Comparable) k).compareTo(x);
    }

    static final <K, V> Node<K, V> tabAt(Node<K, V>[] tab, int i) {
        return (Node) U.getObjectVolatile(tab, (((long) i) << ASHIFT) + ((long) ABASE));
    }

    static final <K, V> boolean casTabAt(Node<K, V>[] tab, int i, Node<K, V> c, Node<K, V> v) {
        return U.compareAndSwapObject(tab, ((long) ABASE) + (((long) i) << ASHIFT), c, v);
    }

    static final <K, V> void setTabAt(Node<K, V>[] tab, int i, Node<K, V> v) {
        U.putObjectVolatile(tab, (((long) i) << ASHIFT) + ((long) ABASE), v);
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity >= 0) {
            int cap;
            if (initialCapacity >= 536870912) {
                cap = MAXIMUM_CAPACITY;
            } else {
                cap = tableSizeFor(((initialCapacity >>> 1) + initialCapacity) + 1);
            }
            this.sizeCtl = cap;
            return;
        }
        throw new IllegalArgumentException();
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = 16;
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (loadFactor <= 0.0f || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }
        if (initialCapacity < concurrencyLevel) {
            initialCapacity = concurrencyLevel;
        }
        long size = (long) (4607182418800017408L + ((double) (((float) ((long) initialCapacity)) / loadFactor)));
        this.sizeCtl = size >= 1073741824 ? MAXIMUM_CAPACITY : tableSizeFor((int) size);
    }

    public int size() {
        long n = sumCount();
        if (n < 0) {
            return 0;
        }
        if (n > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        return (int) n;
    }

    public boolean isEmpty() {
        return sumCount() <= 0;
    }

    public V get(Object key) {
        int h = spread(key.hashCode());
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] tab = nodeArr;
        V v = null;
        if (nodeArr != null) {
            int length = tab.length;
            int n = length;
            if (length > 0) {
                Node<K, V> tabAt = tabAt(tab, (n - 1) & h);
                Node<K, V> e = tabAt;
                if (tabAt != null) {
                    K k;
                    K ek;
                    length = e.hash;
                    int eh = length;
                    if (length == h) {
                        k = e.key;
                        ek = k;
                        if (k == key || (ek != null && key.equals(ek))) {
                            return e.val;
                        }
                    } else if (eh < 0) {
                        tabAt = e.find(h, key);
                        Node<K, V> p = tabAt;
                        if (tabAt != null) {
                            v = p.val;
                        }
                        return v;
                    }
                    while (true) {
                        tabAt = e.next;
                        e = tabAt;
                        if (tabAt == null) {
                            break;
                        } else if (e.hash == h) {
                            k = e.key;
                            ek = k;
                            if (k == key || (ek != null && key.equals(ek))) {
                            }
                        }
                    }
                    return e.val;
                }
            }
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        if (value != null) {
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] t = nodeArr;
            if (nodeArr != null) {
                Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> advance = it.advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    V v = p.val;
                    V v2 = v;
                    if (v == value || (v2 != null && value.equals(v2))) {
                    }
                }
                return true;
            }
            return false;
        }
        throw new NullPointerException();
    }

    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /* JADX WARNING: Missing block: B:53:0x009f, code skipped:
            if (r1 == 0) goto L_0x00b8;
     */
    /* JADX WARNING: Missing block: B:55:0x00a3, code skipped:
            if (r1 < 8) goto L_0x00a8;
     */
    /* JADX WARNING: Missing block: B:56:0x00a5, code skipped:
            treeifyBin(r2, r5);
     */
    /* JADX WARNING: Missing block: B:57:0x00a8, code skipped:
            if (r3 == null) goto L_0x00ab;
     */
    /* JADX WARNING: Missing block: B:58:0x00aa, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        Throwable th;
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        int hash = spread(key.hashCode());
        int binCount = 0;
        Node[] tab = this.table;
        while (true) {
            if (tab != null) {
                int length = tab.length;
                int n = length;
                if (length != 0) {
                    length = (n - 1) & hash;
                    int i = length;
                    Node<K, V> tabAt = tabAt(tab, length);
                    Node<K, V> f = tabAt;
                    if (tabAt != null) {
                        length = f.hash;
                        int fh = length;
                        if (length == -1) {
                            tab = helpTransfer(tab, f);
                        } else {
                            V oldVal = null;
                            synchronized (f) {
                                try {
                                    if (tabAt(tab, i) == f) {
                                        if (fh >= 0) {
                                            int binCount2 = 1;
                                            Node<K, V> e = f;
                                            while (true) {
                                                try {
                                                    if (e.hash == hash) {
                                                        K k = e.key;
                                                        K ek = k;
                                                        if (k == key || (ek != null && key.equals(ek))) {
                                                            oldVal = e.val;
                                                        }
                                                    }
                                                    Node<K, V> pred = e;
                                                    Node<K, V> node = e.next;
                                                    e = node;
                                                    if (node == null) {
                                                        pred.next = new Node(hash, key, value, null);
                                                        break;
                                                    }
                                                    binCount2++;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    throw th;
                                                }
                                            }
                                            oldVal = e.val;
                                            if (!onlyIfAbsent) {
                                                e.val = value;
                                            }
                                            binCount = binCount2;
                                        } else if (f instanceof TreeBin) {
                                            binCount = 2;
                                            Node putTreeVal = ((TreeBin) f).putTreeVal(hash, key, value);
                                            Node p = putTreeVal;
                                            if (putTreeVal != null) {
                                                oldVal = p.val;
                                                if (!onlyIfAbsent) {
                                                    p.val = value;
                                                }
                                            }
                                        } else if (f instanceof ReservationNode) {
                                            throw new IllegalStateException("Recursive update");
                                        }
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    throw th;
                                }
                            }
                        }
                    } else if (casTabAt(tab, i, null, new Node(hash, key, value, null))) {
                        break;
                    }
                }
            }
            tab = initTable();
        }
        addCount(1, binCount);
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            putVal(e.getKey(), e.getValue(), false);
        }
    }

    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    final V replaceNode(Object key, V value, Object cv) {
        K k = key;
        V v = value;
        V v2 = cv;
        int hash = spread(key.hashCode());
        Node<K, V>[] tab = this.table;
        while (true) {
            Node<K, V>[] tab2 = tab;
            if (tab2 == null) {
                break;
            }
            int length = tab2.length;
            int n = length;
            if (length == 0) {
                break;
            }
            length = (n - 1) & hash;
            int i = length;
            Node<K, V> tabAt = tabAt(tab2, length);
            Node<K, V> f = tabAt;
            if (tabAt == null) {
                break;
            }
            length = f.hash;
            int fh = length;
            if (length == -1) {
                tab = helpTransfer(tab2, f);
            } else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab2, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            Node<K, V> e = f;
                            Node<K, V> pred = null;
                            while (true) {
                                if (e.hash == hash) {
                                    K k2 = e.key;
                                    K ek = k2;
                                    if (k2 == k) {
                                        break;
                                    }
                                    k2 = ek;
                                    if (k2 != null && k.equals(k2)) {
                                        break;
                                    }
                                }
                                pred = e;
                                Node<K, V> node = e.next;
                                e = node;
                                if (node == null) {
                                    break;
                                }
                            }
                            V ev = e.val;
                            if (v2 == null || v2 == ev || (ev != null && v2.equals(ev))) {
                                oldVal = ev;
                                if (v != null) {
                                    e.val = v;
                                } else if (pred != null) {
                                    pred.next = e.next;
                                } else {
                                    setTabAt(tab2, i, e.next);
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K, V> t = (TreeBin) f;
                            TreeNode<K, V> treeNode = t.root;
                            TreeNode<K, V> r = treeNode;
                            if (treeNode != null) {
                                TreeNode<K, V> findTreeNode = r.findTreeNode(hash, k, null);
                                treeNode = findTreeNode;
                                if (findTreeNode != null) {
                                    V pv = treeNode.val;
                                    if (v2 == null || v2 == pv || (pv != null && v2.equals(pv))) {
                                        oldVal = pv;
                                        if (v != null) {
                                            treeNode.val = v;
                                        } else if (t.removeTreeNode(treeNode)) {
                                            setTabAt(tab2, i, untreeify(t.first));
                                        }
                                    }
                                }
                            }
                        } else if (f instanceof ReservationNode) {
                            throw new IllegalStateException("Recursive update");
                        }
                    }
                }
                if (!validated) {
                    tab = tab2;
                } else if (oldVal != null) {
                    if (v == null) {
                        addCount(-1, -1);
                    }
                    return oldVal;
                }
            }
            k = key;
        }
        return null;
    }

    public void clear() {
        Throwable th;
        long delta = 0;
        int i = 0;
        Node<K, V>[] tab = this.table;
        while (tab != null && i < tab.length) {
            Node<K, V> f = tabAt(tab, i);
            if (f == null) {
                i++;
            } else {
                int i2 = f.hash;
                int fh = i2;
                if (i2 == -1) {
                    tab = helpTransfer(tab, f);
                    i = 0;
                } else {
                    synchronized (f) {
                        try {
                            if (tabAt(tab, i) == f) {
                                Node<K, V> p = fh >= 0 ? f : f instanceof TreeBin ? ((TreeBin) f).first : null;
                                while (p != null) {
                                    delta--;
                                    p = p.next;
                                }
                                int i3 = i + 1;
                                try {
                                    setTabAt(tab, i, null);
                                    i = i3;
                                } catch (Throwable th2) {
                                    th = th2;
                                    i = i3;
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                }
            }
        }
        if (delta != 0) {
            addCount(delta, -1);
        }
    }

    public Set<K> keySet() {
        KeySetView<K, V> keySetView = this.keySet;
        KeySetView<K, V> ks = keySetView;
        if (keySetView != null) {
            return ks;
        }
        KeySetView keySetView2 = new KeySetView(this, null);
        this.keySet = keySetView2;
        return keySetView2;
    }

    public Collection<V> values() {
        ValuesView<K, V> valuesView = this.values;
        ValuesView<K, V> vs = valuesView;
        if (valuesView != null) {
            return vs;
        }
        ValuesView valuesView2 = new ValuesView(this);
        this.values = valuesView2;
        return valuesView2;
    }

    public Set<Entry<K, V>> entrySet() {
        EntrySetView<K, V> entrySetView = this.entrySet;
        EntrySetView<K, V> es = entrySetView;
        if (entrySetView != null) {
            return es;
        }
        EntrySetView entrySetView2 = new EntrySetView(this);
        this.entrySet = entrySetView2;
        return entrySetView2;
    }

    public int hashCode() {
        int h = 0;
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] t = nodeArr;
        if (nodeArr != null) {
            Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> advance = it.advance();
                Node<K, V> p = advance;
                if (advance == null) {
                    break;
                }
                h += p.key.hashCode() ^ p.val.hashCode();
            }
        }
        return h;
    }

    public String toString() {
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] t = nodeArr;
        int f = nodeArr == null ? 0 : t.length;
        Traverser<K, V> it = new Traverser(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K, V> advance = it.advance();
        Node<K, V> p = advance;
        if (advance != null) {
            while (true) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                Node<K, V> advance2 = it.advance();
                p = advance2;
                if (advance2 == null) {
                    break;
                }
                sb.append(',');
                sb.append(' ');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map)) {
                return false;
            }
            Map<?, ?> m = (Map) o;
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] t = nodeArr;
            int f = nodeArr == null ? 0 : t.length;
            Traverser<K, V> it = new Traverser(t, f, 0, f);
            while (true) {
                Node<K, V> advance = it.advance();
                Node<K, V> p = advance;
                if (advance != null) {
                    V val = p.val;
                    V v = m.get(p.key);
                    if (v == null || !(v == val || v.equals(val))) {
                        return false;
                    }
                } else {
                    for (Entry<?, ?> e : m.entrySet()) {
                        Object key = e.getKey();
                        Object mk = key;
                        if (key != null) {
                            key = e.getValue();
                            Object mv = key;
                            if (key != null) {
                                key = get(mk);
                                Object v2 = key;
                                if (key != null && (mv == v2 || mv.equals(v2))) {
                                }
                            }
                        }
                        return false;
                    }
                }
            }
            return false;
        }
        return true;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        int sshift = 0;
        int ssize = 1;
        while (ssize < 16) {
            sshift++;
            ssize <<= 1;
        }
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        Object segments = new Segment[16];
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment(LOAD_FACTOR);
        }
        PutField streamFields = s.putFields();
        streamFields.put("segments", segments);
        streamFields.put("segmentShift", segmentShift);
        streamFields.put("segmentMask", segmentMask);
        s.writeFields();
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] t = nodeArr;
        if (nodeArr != null) {
            Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
            while (true) {
                Node<K, V> advance = it.advance();
                Node<K, V> p = advance;
                if (advance == null) {
                    break;
                }
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        long added;
        this.sizeCtl = -1;
        s.defaultReadObject();
        long size = 0;
        Node<K, V> p = null;
        while (true) {
            K k = s.readObject();
            V v = s.readObject();
            if (k == null || v == null) {
                added = 0;
            } else {
                p = new Node(spread(k.hashCode()), k, v, p);
                size++;
            }
        }
        added = 0;
        long j;
        if (size == 0) {
            this.sizeCtl = 0;
            j = size;
            return;
        }
        int n;
        if (size >= 536870912) {
            n = MAXIMUM_CAPACITY;
        } else {
            n = (int) size;
            n = tableSizeFor(((n >>> 1) + n) + 1);
        }
        Node<K, V>[] tab = new Node[n];
        int mask = n - 1;
        while (p != null) {
            boolean insertAtFront;
            Node<K, V> next = p.next;
            int h = p.hash;
            int j2 = h & mask;
            Node<K, V> tabAt = tabAt(tab, j2);
            Node<K, V> first = tabAt;
            if (tabAt == null) {
                insertAtFront = true;
                j = size;
            } else {
                K k2 = p.key;
                if (first.hash < 0) {
                    if (((TreeBin) first).putTreeVal(h, k2, p.val) == null) {
                        added++;
                    }
                    j = size;
                    insertAtFront = false;
                } else {
                    Node<K, V> q;
                    boolean insertAtFront2 = true;
                    j = size;
                    size = 0;
                    Node<K, V> q2 = first;
                    while (true) {
                        q = q2;
                        if (q == null) {
                            break;
                        }
                        if (q.hash == h) {
                            K k3 = q.key;
                            K qk = k3;
                            if (k3 == k2) {
                                break;
                            }
                            k3 = qk;
                            if (k3 != null && k2.equals(k3)) {
                                break;
                            }
                        }
                        size++;
                        q2 = q.next;
                    }
                    insertAtFront2 = false;
                    if (!insertAtFront2 || size < 8) {
                        insertAtFront = insertAtFront2;
                    } else {
                        long added2;
                        boolean insertAtFront3;
                        boolean insertAtFront4 = false;
                        added++;
                        p.next = first;
                        q = p;
                        int binCount = size;
                        size = false;
                        insertAtFront2 = null;
                        while (q != null) {
                            added2 = added;
                            insertAtFront3 = insertAtFront4;
                            added = new TreeNode(q.hash, q.key, q.val, null, null);
                            added.prev = insertAtFront2;
                            if (insertAtFront2) {
                                insertAtFront2.next = added;
                            } else {
                                size = added;
                            }
                            insertAtFront2 = added;
                            q = q.next;
                            added = added2;
                            insertAtFront4 = insertAtFront3;
                        }
                        added2 = added;
                        insertAtFront3 = insertAtFront4;
                        setTabAt(tab, j2, new TreeBin(size));
                        added = added2;
                        insertAtFront = insertAtFront3;
                    }
                }
            }
            if (insertAtFront) {
                size = 1;
                added++;
                p.next = first;
                setTabAt(tab, j2, p);
            } else {
                size = 1;
            }
            p = next;
            long j3 = size;
            size = j;
        }
        this.table = tab;
        this.sizeCtl = n - (n >>> 2);
        this.baseCount = added;
    }

    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    public boolean remove(Object key, Object value) {
        if (key != null) {
            return (value == null || replaceNode(key, null, value) == null) ? false : true;
        } else {
            throw new NullPointerException();
        }
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (key != null && oldValue != null && newValue != null) {
            return replaceNode(key, newValue, oldValue) != null;
        } else {
            throw new NullPointerException();
        }
    }

    public V replace(K key, V value) {
        if (key != null && value != null) {
            return replaceNode(key, value, null);
        }
        throw new NullPointerException();
    }

    public V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action != null) {
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] t = nodeArr;
            if (nodeArr != null) {
                Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> advance = it.advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        action.accept(p.key, p.val);
                    } else {
                        return;
                    }
                }
            }
            return;
        }
        throw new NullPointerException();
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function != null) {
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] t = nodeArr;
            if (nodeArr != null) {
                Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> advance = it.advance();
                    Node<K, V> p = advance;
                    if (advance != null) {
                        V oldValue = p.val;
                        K key = p.key;
                        while (true) {
                            V newValue = function.apply(key, oldValue);
                            if (newValue != null) {
                                if (replaceNode(key, newValue, oldValue) != null) {
                                    break;
                                }
                                V v = get(key);
                                oldValue = v;
                                if (v == null) {
                                    break;
                                }
                            } else {
                                throw new NullPointerException();
                            }
                        }
                    }
                    return;
                }
            }
            return;
        }
        throw new NullPointerException();
    }

    boolean removeEntryIf(Predicate<? super Entry<K, V>> function) {
        if (function != null) {
            boolean removed = false;
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] t = nodeArr;
            if (nodeArr != null) {
                Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> advance = it.advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    K k = p.key;
                    V v = p.val;
                    if (function.test(new SimpleImmutableEntry(k, v)) && replaceNode(k, null, v) != null) {
                        removed = true;
                    }
                }
            }
            return removed;
        }
        throw new NullPointerException();
    }

    boolean removeValueIf(Predicate<? super V> function) {
        if (function != null) {
            boolean removed = false;
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] t = nodeArr;
            if (nodeArr != null) {
                Traverser<K, V> it = new Traverser(t, t.length, 0, t.length);
                while (true) {
                    Node<K, V> advance = it.advance();
                    Node<K, V> p = advance;
                    if (advance == null) {
                        break;
                    }
                    K k = p.key;
                    V v = p.val;
                    if (function.test(v) && replaceNode(k, null, v) != null) {
                        removed = true;
                    }
                }
            }
            return removed;
        }
        throw new NullPointerException();
    }

    /* JADX WARNING: Missing block: B:75:0x00da, code skipped:
            if (r2 == 0) goto L_0x00f9;
     */
    /* JADX WARNING: Missing block: B:77:0x00de, code skipped:
            if (r2 < 8) goto L_0x00e3;
     */
    /* JADX WARNING: Missing block: B:78:0x00e0, code skipped:
            treeifyBin(r3, r6);
     */
    /* JADX WARNING: Missing block: B:79:0x00e3, code skipped:
            if (r4 != false) goto L_0x00e6;
     */
    /* JADX WARNING: Missing block: B:80:0x00e5, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        Node[] tab = this.table;
        loop2:
        while (true) {
            if (tab != null) {
                int length = tab.length;
                int n = length;
                if (length != 0) {
                    length = (n - 1) & h;
                    int i = length;
                    Node<K, V> tabAt = tabAt(tab, length);
                    Node<K, V> f = tabAt;
                    if (tabAt == null) {
                        tabAt = new ReservationNode();
                        synchronized (tabAt) {
                            if (casTabAt(tab, i, null, tabAt)) {
                                binCount = 1;
                                Node<K, V> node = null;
                                try {
                                    V apply = mappingFunction.apply(key);
                                    val = apply;
                                    if (apply != null) {
                                        node = new Node(h, key, val, null);
                                    }
                                } finally {
                                    setTabAt(tab, i, node);
                                }
                            }
                        }
                        if (binCount != 0) {
                            break loop2;
                        }
                    } else {
                        length = f.hash;
                        int fh = length;
                        if (length == -1) {
                            tab = helpTransfer(tab, f);
                        } else {
                            boolean added = false;
                            synchronized (f) {
                                try {
                                    if (tabAt(tab, i) == f) {
                                        if (fh >= 0) {
                                            int binCount2 = 1;
                                            Node<K, V> e = f;
                                            while (true) {
                                                try {
                                                    if (e.hash == h) {
                                                        K k = e.key;
                                                        K ek = k;
                                                        if (k == key || (ek != null && key.equals(ek))) {
                                                            val = e.val;
                                                        }
                                                    }
                                                    Node<K, V> pred = e;
                                                    Node<K, V> node2 = e.next;
                                                    e = node2;
                                                    if (node2 == null) {
                                                        V apply2 = mappingFunction.apply(key);
                                                        val = apply2;
                                                        if (apply2 != null) {
                                                            if (pred.next == null) {
                                                                added = true;
                                                                pred.next = new Node(h, key, val, null);
                                                            } else {
                                                                throw new IllegalStateException("Recursive update");
                                                            }
                                                        }
                                                    }
                                                    binCount2++;
                                                } catch (Throwable th) {
                                                    binCount = th;
                                                    throw binCount;
                                                }
                                            }
                                            val = e.val;
                                            binCount = binCount2;
                                        } else if (f instanceof TreeBin) {
                                            binCount = 2;
                                            TreeBin<K, V> t = (TreeBin) f;
                                            TreeNode<K, V> treeNode = t.root;
                                            TreeNode<K, V> r = treeNode;
                                            if (treeNode != null) {
                                                TreeNode<K, V> findTreeNode = r.findTreeNode(h, key, null);
                                                treeNode = findTreeNode;
                                                if (findTreeNode != null) {
                                                    val = treeNode.val;
                                                }
                                            }
                                            V apply3 = mappingFunction.apply(key);
                                            val = apply3;
                                            if (apply3 != null) {
                                                added = true;
                                                t.putTreeVal(h, key, val);
                                            }
                                        } else if (f instanceof ReservationNode) {
                                            throw new IllegalStateException("Recursive update");
                                        }
                                    }
                                } catch (Throwable th2) {
                                    binCount = th2;
                                    throw binCount;
                                }
                            }
                        }
                    }
                }
            }
            tab = initTable();
        }
        if (val != null) {
            addCount(1, binCount);
        }
        return val;
    }

    /* JADX WARNING: Missing block: B:57:0x00b4, code skipped:
            if (r2 == 0) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:58:0x00b6, code skipped:
            addCount((long) r2, r3);
     */
    /* JADX WARNING: Missing block: B:59:0x00ba, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        Node[] tab = this.table;
        while (true) {
            if (tab != null) {
                int length = tab.length;
                int n = length;
                if (length != 0) {
                    length = (n - 1) & h;
                    int i = length;
                    Node<K, V> tabAt = tabAt(tab, length);
                    Node<K, V> f = tabAt;
                    if (tabAt == null) {
                        break;
                    }
                    length = f.hash;
                    int fh = length;
                    if (length == -1) {
                        tab = helpTransfer(tab, f);
                    } else {
                        synchronized (f) {
                            if (tabAt(tab, i) == f) {
                                tabAt = null;
                                if (fh >= 0) {
                                    Node<K, V> node;
                                    binCount = 1;
                                    Node<K, V> e = f;
                                    while (true) {
                                        if (e.hash == h) {
                                            K k = e.key;
                                            K ek = k;
                                            if (k == key || (ek != null && key.equals(ek))) {
                                                val = remappingFunction.apply(key, e.val);
                                            }
                                        }
                                        tabAt = e;
                                        node = e.next;
                                        e = node;
                                        if (node == null) {
                                            break;
                                        }
                                        binCount++;
                                    }
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null) {
                                        e.val = val;
                                    } else {
                                        delta = -1;
                                        node = e.next;
                                        if (tabAt != null) {
                                            tabAt.next = node;
                                        } else {
                                            setTabAt(tab, i, node);
                                        }
                                    }
                                } else if (f instanceof TreeBin) {
                                    binCount = 2;
                                    TreeBin<K, V> t = (TreeBin) f;
                                    TreeNode<K, V> treeNode = t.root;
                                    TreeNode<K, V> r = treeNode;
                                    if (treeNode != null) {
                                        TreeNode<K, V> findTreeNode = r.findTreeNode(h, key, null);
                                        treeNode = findTreeNode;
                                        if (findTreeNode != null) {
                                            val = remappingFunction.apply(key, treeNode.val);
                                            if (val != null) {
                                                treeNode.val = val;
                                            } else {
                                                delta = -1;
                                                if (t.removeTreeNode(treeNode)) {
                                                    setTabAt(tab, i, untreeify(t.first));
                                                }
                                            }
                                        }
                                    }
                                } else if (f instanceof ReservationNode) {
                                    throw new IllegalStateException("Recursive update");
                                }
                            }
                        }
                        if (binCount != 0) {
                            break;
                        }
                    }
                }
            }
            tab = initTable();
        }
        while (true) {
        }
    }

    /* JADX WARNING: Missing block: B:95:0x011b, code skipped:
            if (r6 == 0) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:97:0x011f, code skipped:
            if (r6 < 8) goto L_0x0124;
     */
    /* JADX WARNING: Missing block: B:98:0x0121, code skipped:
            treeifyBin(r7, r10);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Throwable th;
        K k = key;
        BiFunction<? super K, ? super V, ? extends V> biFunction = remappingFunction;
        if (k == null || biFunction == null) {
            throw new NullPointerException();
        }
        int h = spread(key.hashCode());
        int binCount = 0;
        Node[] tab = this.table;
        int delta = 0;
        V val = null;
        loop2:
        while (true) {
            Node<K, V>[] tab2;
            if (tab != null) {
                int length = tab.length;
                int n = length;
                if (length != 0) {
                    length = (n - 1) & h;
                    int i = length;
                    Node<K, V> tabAt = tabAt(tab, length);
                    Node<K, V> f = tabAt;
                    V pv = null;
                    if (tabAt == null) {
                        Node r = new ReservationNode();
                        synchronized (r) {
                            if (casTabAt(tab, i, null, r)) {
                                binCount = 1;
                                Node<K, V> node = null;
                                try {
                                    V apply = biFunction.apply(k, null);
                                    val = apply;
                                    if (apply != null) {
                                        delta = 1;
                                        node = new Node(h, k, val, null);
                                    }
                                } finally {
                                    setTabAt(tab, i, node);
                                }
                            }
                        }
                        if (binCount != 0) {
                            break loop2;
                        }
                        k = key;
                    } else {
                        length = f.hash;
                        int fh = length;
                        if (length == -1) {
                            tab2 = helpTransfer(tab, f);
                            tab = tab2;
                            k = key;
                        } else {
                            synchronized (f) {
                                try {
                                    if (tabAt(tab, i) == f) {
                                        if (fh >= 0) {
                                            Node<K, V> e = f;
                                            int binCount2 = 1;
                                            tabAt = null;
                                            while (true) {
                                                try {
                                                    if (e.hash == h) {
                                                        K k2 = e.key;
                                                        K ek = k2;
                                                        if (k2 == k) {
                                                            break;
                                                        }
                                                        k2 = ek;
                                                        if (k2 != null && k.equals(k2)) {
                                                            break;
                                                        }
                                                    }
                                                    tabAt = e;
                                                    Node<K, V> node2 = e.next;
                                                    e = node2;
                                                    if (node2 == null) {
                                                        val = biFunction.apply(k, null);
                                                        if (val != null) {
                                                            if (tabAt.next == null) {
                                                                delta = 1;
                                                                tabAt.next = new Node(h, k, val, null);
                                                            } else {
                                                                throw new IllegalStateException("Recursive update");
                                                            }
                                                        }
                                                    }
                                                    binCount2++;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    throw th;
                                                }
                                            }
                                            val = biFunction.apply(k, e.val);
                                            if (val != null) {
                                                e.val = val;
                                            } else {
                                                delta = -1;
                                                Node<K, V> en = e.next;
                                                if (tabAt != null) {
                                                    tabAt.next = en;
                                                } else {
                                                    setTabAt(tab, i, en);
                                                }
                                            }
                                            binCount = binCount2;
                                        } else if (f instanceof TreeBin) {
                                            binCount = 1;
                                            TreeBin<K, V> t = (TreeBin) f;
                                            TreeNode<K, V> treeNode = t.root;
                                            TreeNode<K, V> r2 = treeNode;
                                            if (treeNode != null) {
                                                treeNode = r2.findTreeNode(h, k, null);
                                            } else {
                                                treeNode = null;
                                            }
                                            if (treeNode != null) {
                                                pv = treeNode.val;
                                            }
                                            val = biFunction.apply(k, pv);
                                            if (val != null) {
                                                if (treeNode != null) {
                                                    treeNode.val = val;
                                                } else {
                                                    delta = 1;
                                                    t.putTreeVal(h, k, val);
                                                }
                                            } else if (treeNode != null) {
                                                delta = -1;
                                                if (t.removeTreeNode(treeNode)) {
                                                    setTabAt(tab, i, untreeify(t.first));
                                                }
                                            }
                                        } else if (f instanceof ReservationNode) {
                                            throw new IllegalStateException("Recursive update");
                                        }
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    throw th;
                                }
                            }
                        }
                    }
                }
            }
            tab2 = initTable();
            tab = tab2;
            k = key;
        }
        if (delta != 0) {
            addCount((long) delta, binCount);
        }
        return val;
    }

    /* JADX WARNING: Missing block: B:59:0x00a9, code skipped:
            r7 = r3;
     */
    /* JADX WARNING: Missing block: B:62:0x00ad, code skipped:
            r20 = 1;
     */
    /* JADX WARNING: Missing block: B:64:?, code skipped:
            r0.next = new java.util.concurrent.ConcurrentHashMap.Node(r5, r2, r7, 0);
     */
    /* JADX WARNING: Missing block: B:65:0x00b5, code skipped:
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:67:0x00bb, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:68:0x00bc, code skipped:
            r9 = r15;
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:69:0x00c1, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:70:0x00c2, code skipped:
            r20 = 1;
            r9 = r15;
     */
    /* JADX WARNING: Missing block: B:114:0x0133, code skipped:
            if (r9 == 0) goto L_0x0155;
     */
    /* JADX WARNING: Missing block: B:116:0x0137, code skipped:
            if (r9 < 8) goto L_0x013c;
     */
    /* JADX WARNING: Missing block: B:117:0x0139, code skipped:
            treeifyBin(r8, r11);
     */
    /* JADX WARNING: Missing block: B:118:0x013c, code skipped:
            r0 = r6;
            r6 = r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        int binCount;
        Throwable th;
        K k = key;
        V v = value;
        BiFunction<? super V, ? super V, ? extends V> biFunction = remappingFunction;
        if (k == null || v == null || biFunction == null) {
            throw new NullPointerException();
        }
        int length;
        int h = spread(key.hashCode());
        int delta = 0;
        Node[] tab = this.table;
        int binCount2 = 0;
        V val = null;
        while (true) {
            int delta2;
            if (tab != null) {
                length = tab.length;
                int n = length;
                if (length == 0) {
                    delta2 = delta;
                } else {
                    length = (n - 1) & h;
                    int i = length;
                    Node<K, V> tabAt = tabAt(tab, length);
                    Node<K, V> f = tabAt;
                    if (tabAt != null) {
                        length = f.hash;
                        int fh = length;
                        if (length == -1) {
                            tab = helpTransfer(tab, f);
                        } else {
                            synchronized (f) {
                                try {
                                    if (tabAt(tab, i) != f) {
                                        delta2 = delta;
                                    } else if (fh >= 0) {
                                        Node<K, V> e = f;
                                        binCount = 1;
                                        tabAt = null;
                                        while (true) {
                                            try {
                                                if (e.hash == h) {
                                                    K k2 = e.key;
                                                    K ek = k2;
                                                    if (k2 == k) {
                                                        k2 = ek;
                                                        break;
                                                    }
                                                    k2 = ek;
                                                    if (k2 != null) {
                                                        try {
                                                            if (k.equals(k2)) {
                                                                break;
                                                            }
                                                        } catch (Throwable th2) {
                                                            th = th2;
                                                            binCount2 = binCount;
                                                            throw th;
                                                        }
                                                    }
                                                    delta2 = delta;
                                                } else {
                                                    delta2 = delta;
                                                }
                                                tabAt = e;
                                                Node<K, V> node = e.next;
                                                e = node;
                                                if (node == null) {
                                                    break;
                                                }
                                                binCount++;
                                                delta = delta2;
                                            } catch (Throwable th3) {
                                                th = th3;
                                                delta2 = delta;
                                                throw th;
                                            }
                                        }
                                        delta2 = delta;
                                        try {
                                            val = biFunction.apply(e.val, v);
                                            if (val != null) {
                                                e.val = val;
                                                delta = delta2;
                                            } else {
                                                int delta3 = -1;
                                                try {
                                                    delta = e.next;
                                                    if (tabAt != null) {
                                                        tabAt.next = delta;
                                                    } else {
                                                        setTabAt(tab, i, delta);
                                                    }
                                                    delta = delta3;
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    binCount2 = binCount;
                                                    throw th;
                                                }
                                            }
                                            binCount2 = binCount;
                                        } catch (Throwable th5) {
                                            th = th5;
                                            binCount2 = binCount;
                                            throw th;
                                        }
                                    } else {
                                        delta2 = delta;
                                        try {
                                            if (f instanceof TreeBin) {
                                                binCount2 = 2;
                                                TreeBin<K, V> t = (TreeBin) f;
                                                TreeNode<K, V> r = t.root;
                                                TreeNode<K, V> p = r == null ? null : r.findTreeNode(h, k, null);
                                                val = p == null ? v : biFunction.apply(p.val, v);
                                                if (val == null) {
                                                    if (p != null) {
                                                        binCount = -1;
                                                        if (t.removeTreeNode(p)) {
                                                            setTabAt(tab, i, untreeify(t.first));
                                                        }
                                                    }
                                                    delta = delta2;
                                                } else if (p != null) {
                                                    p.val = val;
                                                    delta = delta2;
                                                } else {
                                                    binCount = 1;
                                                    try {
                                                        t.putTreeVal(h, k, val);
                                                    } catch (Throwable th6) {
                                                        th = th6;
                                                        delta = 1;
                                                        throw th;
                                                    }
                                                }
                                                delta = binCount;
                                            } else if (f instanceof ReservationNode) {
                                                throw new IllegalStateException("Recursive update");
                                            }
                                        } catch (Throwable th7) {
                                            th = th7;
                                            throw th;
                                        }
                                    }
                                    delta = delta2;
                                    try {
                                    } catch (Throwable th8) {
                                        th = th8;
                                        throw th;
                                    }
                                } catch (Throwable th9) {
                                    th = th9;
                                    delta2 = delta;
                                    throw th;
                                }
                            }
                        }
                        k = key;
                    } else if (casTabAt(tab, i, null, new Node(h, k, v, null))) {
                        length = 1;
                        delta = v;
                        break;
                    } else {
                        k = key;
                    }
                }
            } else {
                delta2 = delta;
            }
            tab = initTable();
            delta = delta2;
            k = key;
        }
        if (length != 0) {
            addCount((long) length, binCount2);
        }
        return delta;
    }

    public boolean contains(Object value) {
        return containsValue(value);
    }

    public Enumeration<K> keys() {
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] t = nodeArr;
        int f = nodeArr == null ? 0 : t.length;
        return new KeyIterator(t, f, 0, f, this);
    }

    public Enumeration<V> elements() {
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] t = nodeArr;
        int f = nodeArr == null ? 0 : t.length;
        return new ValueIterator(t, f, 0, f, this);
    }

    public long mappingCount() {
        long n = sumCount();
        return n < 0 ? 0 : n;
    }

    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView(new ConcurrentHashMap(), Boolean.TRUE);
    }

    public static <K> KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new KeySetView(new ConcurrentHashMap(initialCapacity), Boolean.TRUE);
    }

    public KeySetView<K, V> keySet(V mappedValue) {
        if (mappedValue != null) {
            return new KeySetView(this, mappedValue);
        }
        throw new NullPointerException();
    }

    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | NumericShaper.MYANMAR;
    }

    private final Node<K, V>[] initTable() {
        Node<K, V>[] tab;
        while (true) {
            Node<K, V>[] nodeArr = this.table;
            tab = nodeArr;
            if (nodeArr != null && tab.length != 0) {
                break;
            }
            int i = this.sizeCtl;
            int sc = i;
            if (i < 0) {
                Thread.yield();
            } else {
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        nodeArr = this.table;
                        tab = nodeArr;
                        if (nodeArr == null || tab.length == 0) {
                            i = sc > 0 ? sc : 16;
                            Node<K, V>[] nt = new Node[i];
                            tab = nt;
                            this.table = nt;
                            sc = i - (i >>> 2);
                        }
                        this.sizeCtl = sc;
                    } catch (Throwable th) {
                        this.sizeCtl = sc;
                    }
                }
            }
        }
        return tab;
    }

    /* JADX WARNING: Missing block: B:3:0x001a, code skipped:
            if (r0.compareAndSwapLong(r8, r2, r4, r6) == false) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:44:0x00b9, code skipped:
            r23 = r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final void addCount(long x, int check) {
        long j;
        long s;
        long j2 = x;
        int i = check;
        CounterCell[] counterCellArr = this.counterCells;
        CounterCell[] as = counterCellArr;
        if (counterCellArr == null) {
            Unsafe unsafe = U;
            long j3 = BASECOUNT;
            long j4 = this.baseCount;
            j = j4 + j2;
            s = j;
        }
        boolean uncontended = true;
        if (as != null) {
            int length = as.length - 1;
            int m = length;
            if (length >= 0) {
                CounterCell counterCell = as[ThreadLocalRandom.getProbe() & m];
                CounterCell a = counterCell;
                if (counterCell != null) {
                    Unsafe unsafe2 = U;
                    s = CELLVALUE;
                    long j5 = a.value;
                    boolean compareAndSwapLong = unsafe2.compareAndSwapLong(a, s, j5, j5 + j2);
                    uncontended = compareAndSwapLong;
                    if (compareAndSwapLong) {
                        if (i > 1) {
                            s = sumCount();
                            long s2 = s;
                            if (i >= 0) {
                                long s3;
                                j = s2;
                                while (true) {
                                    int i2 = this.sizeCtl;
                                    int sc = i2;
                                    if (j < ((long) i2)) {
                                        break;
                                    }
                                    Node<K, V>[] nodeArr = this.table;
                                    Node<K, V>[] tab = nodeArr;
                                    if (nodeArr == null) {
                                        break;
                                    }
                                    i2 = tab.length;
                                    int n = i2;
                                    if (i2 >= MAXIMUM_CAPACITY) {
                                        break;
                                    }
                                    int rs = resizeStamp(n);
                                    if (sc < 0) {
                                        if ((sc >>> 16) == rs && sc != rs + 1 && sc != MAX_RESIZERS + rs) {
                                            nodeArr = this.nextTable;
                                            Node<K, V>[] nt = nodeArr;
                                            if (nodeArr == null) {
                                                break;
                                            } else if (this.transferIndex <= 0) {
                                                s3 = j;
                                                break;
                                            } else {
                                                j = nt;
                                                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                                                    transfer(tab, j);
                                                }
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                    if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << 16) + 2)) {
                                        transfer(tab, null);
                                    }
                                    j = sumCount();
                                }
                                s2 = s3;
                            }
                            return;
                        }
                        return;
                    }
                }
            }
        }
        fullAddCount(j2, uncontended);
    }

    final Node<K, V>[] helpTransfer(Node<K, V>[] tab, Node<K, V> f) {
        if (tab != null && (f instanceof ForwardingNode)) {
            Node<K, V>[] nodeArr = ((ForwardingNode) f).nextTable;
            Node<K, V>[] nextTab = nodeArr;
            if (nodeArr != null) {
                int rs = resizeStamp(tab.length);
                while (nextTab == this.nextTable && this.table == tab) {
                    int i = this.sizeCtl;
                    int sc = i;
                    if (i >= 0 || (sc >>> 16) != rs || sc == rs + 1 || sc == MAX_RESIZERS + rs || this.transferIndex <= 0) {
                        break;
                    }
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                        transfer(tab, nextTab);
                        break;
                    }
                }
                return nextTab;
            }
        }
        return this.table;
    }

    private final void tryPresize(int size) {
        int c = size >= 536870912 ? MAXIMUM_CAPACITY : tableSizeFor(((size >>> 1) + size) + 1);
        while (true) {
            int i = this.sizeCtl;
            int sc = i;
            if (i >= 0) {
                int n;
                Node<K, V>[] tab = this.table;
                if (tab != null) {
                    int length = tab.length;
                    n = length;
                    if (length != 0) {
                        if (c > sc && n < MAXIMUM_CAPACITY) {
                            if (tab == this.table) {
                                if (U.compareAndSwapInt(this, SIZECTL, sc, (resizeStamp(n) << 16) + 2)) {
                                    transfer(tab, null);
                                }
                            }
                        } else {
                            return;
                        }
                    }
                }
                n = sc > c ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (this.table == tab) {
                            this.table = new Node[n];
                            sc = n - (n >>> 2);
                        }
                        this.sizeCtl = sc;
                    } catch (Throwable th) {
                        this.sizeCtl = sc;
                    }
                }
            } else {
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:133:0x01e6, code skipped:
            r1 = 1;
            r3 = 16;
            r7 = r37;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final void transfer(Node<K, V>[] tab, Node<K, V>[] nextTab) {
        Node<K, V>[] nextTab2;
        Throwable ex;
        ConcurrentHashMap concurrentHashMap = this;
        Node<K, V>[] nodeArr = tab;
        int n = nodeArr.length;
        Object obj = 1;
        int i = NCPU > 1 ? (n >>> 3) / NCPU : n;
        int stride = i;
        int i2 = 16;
        if (i < 16) {
            stride = 16;
        }
        int stride2 = stride;
        if (nextTab == null) {
            try {
                Node<K, V>[] nextTab3 = new Node[(n << 1)];
                concurrentHashMap.nextTable = nextTab3;
                concurrentHashMap.transferIndex = n;
                nextTab2 = nextTab3;
            } catch (Throwable ex2) {
                Throwable th = ex2;
                concurrentHashMap.sizeCtl = Integer.MAX_VALUE;
                return;
            }
        }
        nextTab2 = nextTab;
        int nextn = nextTab2.length;
        ForwardingNode<K, V> fwd = new ForwardingNode(nextTab2);
        boolean advance = true;
        boolean finishing = false;
        int i3 = 0;
        i = 0;
        while (true) {
            int bound = i;
            int bound2;
            Unsafe unsafe;
            long j;
            if (advance) {
                i = i3 - 1;
                if (i >= bound) {
                    bound2 = bound;
                } else if (finishing) {
                    bound2 = bound;
                } else {
                    stride = concurrentHashMap.transferIndex;
                    i3 = stride;
                    if (stride <= 0) {
                        i3 = -1;
                        advance = false;
                        i = bound;
                    } else {
                        unsafe = U;
                        j = TRANSFERINDEX;
                        int i4 = i3 > stride2 ? i3 - stride2 : 0;
                        int nextBound = i4;
                        bound2 = bound;
                        int nextIndex = i3;
                        if (unsafe.compareAndSwapInt(concurrentHashMap, j, i3, i4)) {
                            i3 = nextIndex - 1;
                            advance = false;
                            i = nextBound;
                        } else {
                            i3 = i;
                            i = bound2;
                        }
                    }
                }
                i3 = i;
                advance = false;
                i = bound2;
            } else {
                int stride3;
                int nextn2;
                int fh;
                Object obj2;
                bound2 = bound;
                Node<K, V> ln = null;
                if (i3 < 0 || i3 >= n) {
                    stride3 = stride2;
                    nextn2 = nextn;
                } else if (i3 + n >= nextn) {
                    stride3 = stride2;
                    nextn2 = nextn;
                } else {
                    boolean advance2;
                    Node<K, V> tabAt = tabAt(nodeArr, i3);
                    Node<K, V> f = tabAt;
                    if (tabAt == null) {
                        advance2 = casTabAt(nodeArr, i3, null, fwd);
                    } else {
                        stride = f.hash;
                        fh = stride;
                        if (stride == -1) {
                            advance2 = true;
                        } else {
                            synchronized (f) {
                                int fh2;
                                try {
                                    int b;
                                    if (tabAt(nodeArr, i3) != f) {
                                        stride3 = stride2;
                                        nextn2 = nextn;
                                    } else if (fh >= 0) {
                                        stride = fh & n;
                                        Node<K, V> lastRun = f;
                                        try {
                                            Node<K, V> lastRun2;
                                            Node<K, V> p = f.next;
                                            while (p != null) {
                                                try {
                                                    b = p.hash & n;
                                                    if (b != stride) {
                                                        stride = b;
                                                        lastRun = p;
                                                    }
                                                    p = p.next;
                                                } catch (Throwable th2) {
                                                    ex2 = th2;
                                                    throw ex2;
                                                }
                                            }
                                            if (stride == 0) {
                                                ln = lastRun;
                                                p = null;
                                            } else {
                                                p = lastRun;
                                            }
                                            Node<K, V> hn = p;
                                            p = ln;
                                            ln = f;
                                            while (ln != lastRun) {
                                                i2 = ln.hash;
                                                int runBit = stride;
                                                stride = ln.key;
                                                fh2 = fh;
                                                try {
                                                    fh = ln.val;
                                                    if ((i2 & n) == 0) {
                                                        lastRun2 = lastRun;
                                                        p = new Node(i2, stride, fh, p);
                                                    } else {
                                                        lastRun2 = lastRun;
                                                        hn = new Node(i2, stride, fh, hn);
                                                    }
                                                    ln = ln.next;
                                                    stride = runBit;
                                                    fh = fh2;
                                                    lastRun = lastRun2;
                                                } catch (Throwable th3) {
                                                    ex2 = th3;
                                                    throw ex2;
                                                }
                                            }
                                            fh2 = fh;
                                            lastRun2 = lastRun;
                                            setTabAt(nextTab2, i3, p);
                                            setTabAt(nextTab2, i3 + n, hn);
                                            setTabAt(nodeArr, i3, fwd);
                                            advance = true;
                                            stride3 = stride2;
                                            nextn2 = nextn;
                                        } catch (Throwable th4) {
                                            ex2 = th4;
                                            fh2 = fh;
                                            stride3 = stride2;
                                            nextn2 = nextn;
                                            throw ex2;
                                        }
                                    } else {
                                        try {
                                            if (f instanceof TreeBin) {
                                                Node t = (TreeBin) f;
                                                b = 0;
                                                i2 = 0;
                                                TreeNode<K, V> lo = null;
                                                tabAt = t.first;
                                                Node<K, V> t2 = t;
                                                TreeNode<K, V> hiTail = null;
                                                TreeNode<K, V> hi = null;
                                                TreeNode<K, V> loTail = 0;
                                                fh = lo;
                                                while (tabAt != null) {
                                                    stride3 = stride2;
                                                    try {
                                                        stride2 = tabAt.hash;
                                                        nextn2 = nextn;
                                                        TreeNode<K, V> p2 = new TreeNode(stride2, tabAt.key, tabAt.val, null, null);
                                                        if ((stride2 & n) == 0) {
                                                            p2.prev = loTail;
                                                            if (loTail == null) {
                                                                fh = p2;
                                                            } else {
                                                                loTail.next = p2;
                                                            }
                                                            loTail = p2;
                                                            b++;
                                                        } else {
                                                            p2.prev = hiTail;
                                                            if (hiTail == null) {
                                                                hi = p2;
                                                            } else {
                                                                hiTail.next = p2;
                                                            }
                                                            hiTail = p2;
                                                            i2++;
                                                        }
                                                        tabAt = tabAt.next;
                                                        stride2 = stride3;
                                                        nextn = nextn2;
                                                    } catch (Throwable th5) {
                                                        ex2 = th5;
                                                        throw ex2;
                                                    }
                                                }
                                                stride3 = stride2;
                                                nextn2 = nextn;
                                                Node<K, V> ln2 = b <= 6 ? untreeify(fh) : i2 != 0 ? new TreeBin(fh) : t2;
                                                tabAt = i2 <= 6 ? untreeify(hi) : b != 0 ? new TreeBin(hi) : t2;
                                                setTabAt(nextTab2, i3, ln2);
                                                setTabAt(nextTab2, i3 + n, tabAt);
                                                setTabAt(nodeArr, i3, fwd);
                                                advance = true;
                                            } else {
                                                stride3 = stride2;
                                                nextn2 = nextn;
                                            }
                                        } catch (Throwable th6) {
                                            ex2 = th6;
                                            stride3 = stride2;
                                            nextn2 = nextn;
                                            throw ex2;
                                        }
                                    }
                                } catch (Throwable th7) {
                                    ex2 = th7;
                                    fh2 = fh;
                                    stride3 = stride2;
                                    nextn2 = nextn;
                                    throw ex2;
                                }
                            }
                        }
                    }
                    advance = advance2;
                    obj2 = obj;
                    fh = i2;
                    stride3 = stride2;
                    nextn2 = nextn;
                    obj = obj2;
                    i2 = fh;
                    i = bound2;
                    stride2 = stride3;
                    nextn = nextn2;
                }
                if (finishing) {
                    this.nextTable = null;
                    this.table = nextTab2;
                    this.sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                concurrentHashMap = this;
                unsafe = U;
                j = SIZECTL;
                bound = concurrentHashMap.sizeCtl;
                i = bound;
                i2 = i3;
                if (unsafe.compareAndSwapInt(concurrentHashMap, j, bound, i - 1)) {
                    fh = 16;
                    if (i - 2 == (resizeStamp(n) << 16)) {
                        obj2 = 1;
                        advance = true;
                        finishing = true;
                        i2 = n;
                    } else {
                        return;
                    }
                }
                obj2 = 1;
                fh = 16;
                i3 = i2;
                obj = obj2;
                i2 = fh;
                i = bound2;
                stride2 = stride3;
                nextn = nextn2;
            }
        }
    }

    final long sumCount() {
        CounterCell[] as = this.counterCells;
        long sum = this.baseCount;
        if (as != null) {
            for (CounterCell counterCell : as) {
                CounterCell a = counterCell;
                if (counterCell != null) {
                    sum += a.value;
                }
            }
        }
        return sum;
    }

    private final void fullAddCount(long x, boolean wasUncontended) {
        boolean wasUncontended2;
        long j = x;
        int probe = ThreadLocalRandom.getProbe();
        int h = probe;
        if (probe == 0) {
            ThreadLocalRandom.localInit();
            h = ThreadLocalRandom.getProbe();
            wasUncontended2 = true;
        } else {
            wasUncontended2 = wasUncontended;
        }
        boolean wasUncontended3 = wasUncontended2;
        int h2 = h;
        wasUncontended2 = false;
        while (true) {
            boolean created;
            boolean collide = wasUncontended2;
            CounterCell[] counterCellArr = this.counterCells;
            CounterCell[] as = counterCellArr;
            if (counterCellArr != null) {
                probe = as.length;
                int n = probe;
                if (probe > 0) {
                    CounterCell counterCell = as[(n - 1) & h2];
                    CounterCell a = counterCell;
                    CounterCell r;
                    CounterCell a2;
                    if (counterCell == null) {
                        if (this.cellsBusy == 0) {
                            r = new CounterCell(j);
                            if (this.cellsBusy == 0) {
                                a2 = a;
                                if (U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                                    created = false;
                                    try {
                                        counterCellArr = this.counterCells;
                                        CounterCell[] rs = counterCellArr;
                                        if (counterCellArr != null) {
                                            probe = rs.length;
                                            int m = probe;
                                            if (probe > 0) {
                                                probe = (m - 1) & h2;
                                                int j2 = probe;
                                                if (rs[probe] == null) {
                                                    rs[j2] = r;
                                                    created = true;
                                                }
                                            }
                                        }
                                        this.cellsBusy = 0;
                                        if (!created) {
                                            wasUncontended2 = collide;
                                        } else {
                                            return;
                                        }
                                    } catch (Throwable th) {
                                        this.cellsBusy = 0;
                                    }
                                }
                                collide = false;
                            }
                        }
                        a2 = a;
                        collide = false;
                    } else {
                        a2 = a;
                        if (wasUncontended3) {
                            Unsafe unsafe = U;
                            long j3 = CELLVALUE;
                            a = a2;
                            long j4 = a.value;
                            if (!unsafe.compareAndSwapLong(a, j3, j4, j4 + j)) {
                                if (this.counterCells != as) {
                                } else if (n >= NCPU) {
                                    r = a;
                                } else {
                                    if (!collide) {
                                        collide = true;
                                        r = a;
                                    } else if (this.cellsBusy == 0) {
                                        r = a;
                                        if (U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                                            try {
                                                if (this.counterCells == as) {
                                                    counterCellArr = new CounterCell[(n << 1)];
                                                    for (h = 0; h < n; h++) {
                                                        counterCellArr[h] = as[h];
                                                    }
                                                    this.counterCells = counterCellArr;
                                                }
                                                this.cellsBusy = 0;
                                                wasUncontended2 = false;
                                            } catch (Throwable th2) {
                                                this.cellsBusy = 0;
                                            }
                                        }
                                    }
                                    h2 = ThreadLocalRandom.advanceProbe(h2);
                                    wasUncontended2 = collide;
                                }
                                collide = false;
                                h2 = ThreadLocalRandom.advanceProbe(h2);
                                wasUncontended2 = collide;
                            } else {
                                return;
                            }
                        }
                        wasUncontended3 = true;
                    }
                    h2 = ThreadLocalRandom.advanceProbe(h2);
                    wasUncontended2 = collide;
                }
            }
            if (this.cellsBusy == 0 && this.counterCells == as) {
                if (U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    created = false;
                    try {
                        if (this.counterCells == as) {
                            counterCellArr = new CounterCell[2];
                            counterCellArr[h2 & 1] = new CounterCell(j);
                            this.counterCells = counterCellArr;
                            created = true;
                        }
                        this.cellsBusy = 0;
                        if (created) {
                            return;
                        }
                        wasUncontended2 = collide;
                    } catch (Throwable th3) {
                        this.cellsBusy = 0;
                    }
                }
            }
            Unsafe unsafe2 = U;
            long j5 = BASECOUNT;
            long j6 = this.baseCount;
            if (unsafe2.compareAndSwapLong(this, j5, j6, j6 + j)) {
                return;
            }
            wasUncontended2 = collide;
        }
    }

    private final void treeifyBin(Node<K, V>[] tab, int index) {
        if (tab != null) {
            int length = tab.length;
            int n = length;
            if (length < 64) {
                tryPresize(n << 1);
                return;
            }
            Node<K, V> tabAt = tabAt(tab, index);
            Node<K, V> b = tabAt;
            if (tabAt != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K, V> tl = null;
                        TreeNode<K, V> hd = null;
                        for (tabAt = b; tabAt != null; tabAt = tabAt.next) {
                            TreeNode<K, V> p = new TreeNode(tabAt.hash, tabAt.key, tabAt.val, null, null);
                            p.prev = tl;
                            if (tl == null) {
                                hd = p;
                            } else {
                                tl.next = p;
                            }
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin(hd));
                    }
                }
            }
        }
    }

    static <K, V> Node<K, V> untreeify(Node<K, V> b) {
        Node<K, V> tl = null;
        Node<K, V> hd = null;
        for (Node<K, V> q = b; q != null; q = q.next) {
            Node<K, V> p = new Node(q.hash, q.key, q.val, null);
            if (tl == null) {
                hd = p;
            } else {
                tl.next = p;
            }
            tl = p;
        }
        return hd;
    }

    final int batchFor(long b) {
        if (b != Long.MAX_VALUE) {
            long sumCount = sumCount();
            long n = sumCount;
            if (sumCount > 1 && n >= b) {
                int i;
                int sp = ForkJoinPool.getCommonPoolParallelism() << 2;
                if (b > 0) {
                    long j = n / b;
                    n = j;
                    if (j < ((long) sp)) {
                        i = (int) n;
                        return i;
                    }
                }
                i = sp;
                return i;
            }
        }
        return 0;
    }

    public void forEach(long parallelismThreshold, BiConsumer<? super K, ? super V> action) {
        if (action != null) {
            new ForEachMappingTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
            return;
        }
        throw new NullPointerException();
    }

    public <U> void forEach(long parallelismThreshold, BiFunction<? super K, ? super V, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedMappingTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U search(long parallelismThreshold, BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        if (searchFunction != null) {
            return new SearchMappingsTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
        }
        throw new NullPointerException();
    }

    public <U> U reduce(long parallelismThreshold, BiFunction<? super K, ? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer != null && reducer != null) {
            return new MapReduceMappingsTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public double reduceToDouble(long parallelismThreshold, ToDoubleBiFunction<? super K, ? super V> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Double) new MapReduceMappingsToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).doubleValue();
        }
        throw new NullPointerException();
    }

    public long reduceToLong(long parallelismThreshold, ToLongBiFunction<? super K, ? super V> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Long) new MapReduceMappingsToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).longValue();
        }
        throw new NullPointerException();
    }

    public int reduceToInt(long parallelismThreshold, ToIntBiFunction<? super K, ? super V> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Integer) new MapReduceMappingsToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).intValue();
        }
        throw new NullPointerException();
    }

    public void forEachKey(long parallelismThreshold, Consumer<? super K> action) {
        if (action != null) {
            new ForEachKeyTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
            return;
        }
        throw new NullPointerException();
    }

    public <U> void forEachKey(long parallelismThreshold, Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedKeyTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U searchKeys(long parallelismThreshold, Function<? super K, ? extends U> searchFunction) {
        if (searchFunction != null) {
            return new SearchKeysTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
        }
        throw new NullPointerException();
    }

    public K reduceKeys(long parallelismThreshold, BiFunction<? super K, ? super K, ? extends K> reducer) {
        if (reducer != null) {
            return new ReduceKeysTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public <U> U reduceKeys(long parallelismThreshold, Function<? super K, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer != null && reducer != null) {
            return new MapReduceKeysTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public double reduceKeysToDouble(long parallelismThreshold, ToDoubleFunction<? super K> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Double) new MapReduceKeysToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).doubleValue();
        }
        throw new NullPointerException();
    }

    public long reduceKeysToLong(long parallelismThreshold, ToLongFunction<? super K> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Long) new MapReduceKeysToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).longValue();
        }
        throw new NullPointerException();
    }

    public int reduceKeysToInt(long parallelismThreshold, ToIntFunction<? super K> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Integer) new MapReduceKeysToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).intValue();
        }
        throw new NullPointerException();
    }

    public void forEachValue(long parallelismThreshold, Consumer<? super V> action) {
        if (action != null) {
            new ForEachValueTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
            return;
        }
        throw new NullPointerException();
    }

    public <U> void forEachValue(long parallelismThreshold, Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedValueTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U searchValues(long parallelismThreshold, Function<? super V, ? extends U> searchFunction) {
        if (searchFunction != null) {
            return new SearchValuesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
        }
        throw new NullPointerException();
    }

    public V reduceValues(long parallelismThreshold, BiFunction<? super V, ? super V, ? extends V> reducer) {
        if (reducer != null) {
            return new ReduceValuesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public <U> U reduceValues(long parallelismThreshold, Function<? super V, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer != null && reducer != null) {
            return new MapReduceValuesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public double reduceValuesToDouble(long parallelismThreshold, ToDoubleFunction<? super V> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Double) new MapReduceValuesToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).doubleValue();
        }
        throw new NullPointerException();
    }

    public long reduceValuesToLong(long parallelismThreshold, ToLongFunction<? super V> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Long) new MapReduceValuesToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).longValue();
        }
        throw new NullPointerException();
    }

    public int reduceValuesToInt(long parallelismThreshold, ToIntFunction<? super V> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Integer) new MapReduceValuesToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).intValue();
        }
        throw new NullPointerException();
    }

    public void forEachEntry(long parallelismThreshold, Consumer<? super Entry<K, V>> action) {
        if (action != null) {
            new ForEachEntryTask(null, batchFor(parallelismThreshold), 0, 0, this.table, action).invoke();
            return;
        }
        throw new NullPointerException();
    }

    public <U> void forEachEntry(long parallelismThreshold, Function<Entry<K, V>, ? extends U> transformer, Consumer<? super U> action) {
        if (transformer == null || action == null) {
            throw new NullPointerException();
        }
        new ForEachTransformedEntryTask(null, batchFor(parallelismThreshold), 0, 0, this.table, transformer, action).invoke();
    }

    public <U> U searchEntries(long parallelismThreshold, Function<Entry<K, V>, ? extends U> searchFunction) {
        if (searchFunction != null) {
            return new SearchEntriesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, searchFunction, new AtomicReference()).invoke();
        }
        throw new NullPointerException();
    }

    public Entry<K, V> reduceEntries(long parallelismThreshold, BiFunction<Entry<K, V>, Entry<K, V>, ? extends Entry<K, V>> reducer) {
        if (reducer != null) {
            return (Entry) new ReduceEntriesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public <U> U reduceEntries(long parallelismThreshold, Function<Entry<K, V>, ? extends U> transformer, BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer != null && reducer != null) {
            return new MapReduceEntriesTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, reducer).invoke();
        }
        throw new NullPointerException();
    }

    public double reduceEntriesToDouble(long parallelismThreshold, ToDoubleFunction<Entry<K, V>> transformer, double basis, DoubleBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Double) new MapReduceEntriesToDoubleTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).doubleValue();
        }
        throw new NullPointerException();
    }

    public long reduceEntriesToLong(long parallelismThreshold, ToLongFunction<Entry<K, V>> transformer, long basis, LongBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Long) new MapReduceEntriesToLongTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).longValue();
        }
        throw new NullPointerException();
    }

    public int reduceEntriesToInt(long parallelismThreshold, ToIntFunction<Entry<K, V>> transformer, int basis, IntBinaryOperator reducer) {
        if (transformer != null && reducer != null) {
            return ((Integer) new MapReduceEntriesToIntTask(null, batchFor(parallelismThreshold), 0, 0, this.table, null, transformer, basis, reducer).invoke()).intValue();
        }
        throw new NullPointerException();
    }
}
