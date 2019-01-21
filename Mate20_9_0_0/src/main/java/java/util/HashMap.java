package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int MAXIMUM_CAPACITY = 1073741824;
    static final int MIN_TREEIFY_CAPACITY = 64;
    static final int TREEIFY_THRESHOLD = 8;
    static final int UNTREEIFY_THRESHOLD = 6;
    private static final long serialVersionUID = 362498820763181265L;
    transient Set<Entry<K, V>> entrySet;
    final float loadFactor;
    transient int modCount;
    transient int size;
    transient Node<K, V>[] table;
    int threshold;

    abstract class HashIterator {
        Node<K, V> current = null;
        int expectedModCount;
        int index = 0;
        Node<K, V> next = null;

        HashIterator() {
            this.expectedModCount = HashMap.this.modCount;
            Node<K, V>[] t = HashMap.this.table;
            if (t != null && HashMap.this.size > 0) {
                while (this.index < t.length) {
                    int i = this.index;
                    this.index = i + 1;
                    Node node = t[i];
                    this.next = node;
                    if (node != null) {
                        return;
                    }
                }
            }
        }

        public final boolean hasNext() {
            return this.next != null;
        }

        final Node<K, V> nextNode() {
            Node<K, V> e = this.next;
            if (HashMap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            } else if (e != null) {
                this.current = e;
                Node node = e.next;
                this.next = node;
                if (node == null) {
                    Node<K, V>[] nodeArr = HashMap.this.table;
                    Node<K, V>[] t = nodeArr;
                    if (nodeArr != null) {
                        while (this.index < t.length) {
                            int i = this.index;
                            this.index = i + 1;
                            node = t[i];
                            this.next = node;
                            if (node != null) {
                                break;
                            }
                        }
                    }
                }
                return e;
            } else {
                throw new NoSuchElementException();
            }
        }

        public final void remove() {
            Node<K, V> p = this.current;
            if (p == null) {
                throw new IllegalStateException();
            } else if (HashMap.this.modCount == this.expectedModCount) {
                this.current = null;
                K key = p.key;
                HashMap.this.removeNode(HashMap.hash(key), key, null, false, false);
                this.expectedModCount = HashMap.this.modCount;
            } else {
                throw new ConcurrentModificationException();
            }
        }
    }

    static class HashMapSpliterator<K, V> {
        Node<K, V> current;
        int est;
        int expectedModCount;
        int fence;
        int index;
        final HashMap<K, V> map;

        HashMapSpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() {
            int i = this.fence;
            int hi = i;
            if (i >= 0) {
                return hi;
            }
            HashMap<K, V> m = this.map;
            this.est = m.size;
            this.expectedModCount = m.modCount;
            Node<K, V>[] tab = m.table;
            int length = tab == null ? 0 : tab.length;
            this.fence = length;
            return length;
        }

        public final long estimateSize() {
            getFence();
            return (long) this.est;
        }
    }

    final class EntryIterator extends HashIterator implements Iterator<Entry<K, V>> {
        EntryIterator() {
            super();
        }

        public final Entry<K, V> next() {
            return nextNode();
        }
    }

    static final class EntrySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<Entry<K, V>> {
        EntrySpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K, V> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = (lo + hi) >>> 1;
            if (lo >= mid || this.current != null) {
                return null;
            }
            HashMap hashMap = this.map;
            this.index = mid;
            int i = this.est >>> 1;
            this.est = i;
            return new EntrySpliterator(hashMap, lo, mid, i, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Entry<K, V>> action) {
            if (action != null) {
                int length;
                HashMap<K, V> m = this.map;
                Node<K, V>[] tab = m.table;
                int i = this.fence;
                int hi = i;
                if (i < 0) {
                    i = m.modCount;
                    this.expectedModCount = i;
                    length = tab == null ? 0 : tab.length;
                    this.fence = length;
                    hi = length;
                } else {
                    i = this.expectedModCount;
                }
                if (tab != null && tab.length >= hi) {
                    length = this.index;
                    int i2 = length;
                    if (length >= 0) {
                        this.index = hi;
                        if (i2 < hi || this.current != null) {
                            Node<K, V> p = this.current;
                            this.current = null;
                            while (true) {
                                if (p == null) {
                                    int i3 = i2 + 1;
                                    p = tab[i2];
                                    i2 = i3;
                                } else {
                                    action.accept(p);
                                    p = p.next;
                                }
                                if (p == null && i2 >= hi) {
                                    break;
                                }
                            }
                            if (m.modCount != i) {
                                throw new ConcurrentModificationException();
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super Entry<K, V>> action) {
            if (action != null) {
                Node<K, V>[] tab = this.map.table;
                if (tab != null) {
                    int length = tab.length;
                    int fence = getFence();
                    int hi = fence;
                    if (length >= fence && this.index >= 0) {
                        while (true) {
                            if (this.current == null && this.index >= hi) {
                                break;
                            } else if (this.current == null) {
                                length = this.index;
                                this.index = length + 1;
                                this.current = tab[length];
                            } else {
                                Node<K, V> e = this.current;
                                this.current = this.current.next;
                                action.accept(e);
                                if (this.map.modCount == this.expectedModCount) {
                                    return true;
                                }
                                throw new ConcurrentModificationException();
                            }
                        }
                    }
                }
                return false;
            }
            throw new NullPointerException();
        }

        public int characteristics() {
            int i = (this.fence < 0 || this.est == this.map.size) ? 64 : 0;
            return i | 1;
        }
    }

    final class KeyIterator extends HashIterator implements Iterator<K> {
        KeyIterator() {
            super();
        }

        public final K next() {
            return nextNode().key;
        }
    }

    static final class KeySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = (lo + hi) >>> 1;
            if (lo >= mid || this.current != null) {
                return null;
            }
            HashMap hashMap = this.map;
            this.index = mid;
            int i = this.est >>> 1;
            this.est = i;
            return new KeySpliterator(hashMap, lo, mid, i, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action != null) {
                int length;
                HashMap<K, V> m = this.map;
                Node<K, V>[] tab = m.table;
                int i = this.fence;
                int hi = i;
                if (i < 0) {
                    i = m.modCount;
                    this.expectedModCount = i;
                    length = tab == null ? 0 : tab.length;
                    this.fence = length;
                    hi = length;
                } else {
                    i = this.expectedModCount;
                }
                if (tab != null && tab.length >= hi) {
                    length = this.index;
                    int i2 = length;
                    if (length >= 0) {
                        this.index = hi;
                        if (i2 < hi || this.current != null) {
                            Node<K, V> p = this.current;
                            this.current = null;
                            while (true) {
                                if (p == null) {
                                    int i3 = i2 + 1;
                                    p = tab[i2];
                                    i2 = i3;
                                } else {
                                    action.accept(p.key);
                                    p = p.next;
                                }
                                if (p == null && i2 >= hi) {
                                    break;
                                }
                            }
                            if (m.modCount != i) {
                                throw new ConcurrentModificationException();
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action != null) {
                Node<K, V>[] tab = this.map.table;
                if (tab != null) {
                    int length = tab.length;
                    int fence = getFence();
                    int hi = fence;
                    if (length >= fence && this.index >= 0) {
                        while (true) {
                            if (this.current == null && this.index >= hi) {
                                break;
                            } else if (this.current == null) {
                                length = this.index;
                                this.index = length + 1;
                                this.current = tab[length];
                            } else {
                                K k = this.current.key;
                                this.current = this.current.next;
                                action.accept(k);
                                if (this.map.modCount == this.expectedModCount) {
                                    return true;
                                }
                                throw new ConcurrentModificationException();
                            }
                        }
                    }
                }
                return false;
            }
            throw new NullPointerException();
        }

        public int characteristics() {
            int i = (this.fence < 0 || this.est == this.map.size) ? 64 : 0;
            return i | 1;
        }
    }

    static class Node<K, V> implements Entry<K, V> {
        final int hash;
        final K key;
        Node<K, V> next;
        V value;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return this.key;
        }

        public final V getValue() {
            return this.value;
        }

        public final String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.key);
            stringBuilder.append("=");
            stringBuilder.append(this.value);
            return stringBuilder.toString();
        }

        public final int hashCode() {
            return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
        }

        public final V setValue(V newValue) {
            V oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Entry) {
                Entry<?, ?> e = (Entry) o;
                if (Objects.equals(this.key, e.getKey()) && Objects.equals(this.value, e.getValue())) {
                    return true;
                }
            }
            return false;
        }
    }

    final class ValueIterator extends HashIterator implements Iterator<V> {
        ValueIterator() {
            super();
        }

        public final V next() {
            return nextNode().value;
        }
    }

    static final class ValueSpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = (lo + hi) >>> 1;
            if (lo >= mid || this.current != null) {
                return null;
            }
            HashMap hashMap = this.map;
            this.index = mid;
            int i = this.est >>> 1;
            this.est = i;
            return new ValueSpliterator(hashMap, lo, mid, i, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action != null) {
                int length;
                HashMap<K, V> m = this.map;
                Node<K, V>[] tab = m.table;
                int i = this.fence;
                int hi = i;
                if (i < 0) {
                    i = m.modCount;
                    this.expectedModCount = i;
                    length = tab == null ? 0 : tab.length;
                    this.fence = length;
                    hi = length;
                } else {
                    i = this.expectedModCount;
                }
                if (tab != null && tab.length >= hi) {
                    length = this.index;
                    int i2 = length;
                    if (length >= 0) {
                        this.index = hi;
                        if (i2 < hi || this.current != null) {
                            Node<K, V> p = this.current;
                            this.current = null;
                            while (true) {
                                if (p == null) {
                                    int i3 = i2 + 1;
                                    p = tab[i2];
                                    i2 = i3;
                                } else {
                                    action.accept(p.value);
                                    p = p.next;
                                }
                                if (p == null && i2 >= hi) {
                                    break;
                                }
                            }
                            if (m.modCount != i) {
                                throw new ConcurrentModificationException();
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            throw new NullPointerException();
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action != null) {
                Node<K, V>[] tab = this.map.table;
                if (tab != null) {
                    int length = tab.length;
                    int fence = getFence();
                    int hi = fence;
                    if (length >= fence && this.index >= 0) {
                        while (true) {
                            if (this.current == null && this.index >= hi) {
                                break;
                            } else if (this.current == null) {
                                length = this.index;
                                this.index = length + 1;
                                this.current = tab[length];
                            } else {
                                V v = this.current.value;
                                this.current = this.current.next;
                                action.accept(v);
                                if (this.map.modCount == this.expectedModCount) {
                                    return true;
                                }
                                throw new ConcurrentModificationException();
                            }
                        }
                    }
                }
                return false;
            }
            throw new NullPointerException();
        }

        public int characteristics() {
            return (this.fence < 0 || this.est == this.map.size) ? 64 : 0;
        }
    }

    static final class TreeNode<K, V> extends LinkedHashMapEntry<K, V> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        TreeNode<K, V> left;
        TreeNode<K, V> parent;
        TreeNode<K, V> prev;
        boolean red;
        TreeNode<K, V> right;

        static {
            Class cls = HashMap.class;
        }

        TreeNode(int hash, K key, V val, Node<K, V> next) {
            super(hash, key, val, next);
        }

        final TreeNode<K, V> root() {
            TreeNode<K, V> r = this;
            while (true) {
                TreeNode<K, V> treeNode = r.parent;
                TreeNode<K, V> p = treeNode;
                if (treeNode == null) {
                    return r;
                }
                r = p;
            }
        }

        static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
            if (root != null && tab != null) {
                int length = tab.length;
                int n = length;
                if (length > 0) {
                    length = (n - 1) & root.hash;
                    TreeNode<K, V> first = tab[length];
                    if (root != first) {
                        tab[length] = root;
                        TreeNode<K, V> rp = root.prev;
                        Node<K, V> node = root.next;
                        Node<K, V> rn = node;
                        if (node != null) {
                            ((TreeNode) rn).prev = rp;
                        }
                        if (rp != null) {
                            rp.next = rn;
                        }
                        if (first != null) {
                            first.prev = root;
                        }
                        root.next = first;
                        root.prev = null;
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:17:0x002e, code skipped:
            if (r3 != null) goto L_0x0030;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
            Class<?> kc2 = kc;
            TreeNode<K, V> p = this;
            do {
                TreeNode<K, V> pl = p.left;
                TreeNode<K, V> pr = p.right;
                int i = p.hash;
                int ph = i;
                if (i > h) {
                    p = pl;
                    continue;
                } else if (ph < h) {
                    p = pr;
                    continue;
                } else {
                    K k2 = p.key;
                    K pk = k2;
                    if (k2 == k || (k != null && k.equals(pk))) {
                        return p;
                    }
                    if (pl == null) {
                        p = pr;
                        continue;
                    } else if (pr == null) {
                        p = pl;
                        continue;
                    } else {
                        if (kc2 == null) {
                            Class<?> comparableClassFor = HashMap.comparableClassFor(k);
                            kc2 = comparableClassFor;
                        }
                        i = HashMap.compareComparables(kc2, k, pk);
                        int dir = i;
                        if (i != 0) {
                            p = dir < 0 ? pl : pr;
                            continue;
                        }
                        TreeNode<K, V> find = pr.find(h, k, kc2);
                        TreeNode<K, V> q = find;
                        if (find != null) {
                            return q;
                        }
                        p = pl;
                        continue;
                    }
                }
            } while (p != null);
            return null;
        }

        final TreeNode<K, V> getTreeNode(int h, Object k) {
            return (this.parent != null ? root() : this).find(h, k, null);
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

        /* JADX WARNING: Missing block: B:13:0x0032, code skipped:
            if (r8 != null) goto L_0x0034;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final void treeify(Node<K, V>[] tab) {
            TreeNode<K, V> root = null;
            TreeNode<K, V> x = this;
            while (x != null) {
                TreeNode<K, V> next = x.next;
                x.right = null;
                x.left = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                } else {
                    int i;
                    TreeNode<K, V> xp;
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    TreeNode<K, V> p = root;
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
                                Class<?> comparableClassFor = HashMap.comparableClassFor(k);
                                kc = comparableClassFor;
                            }
                            i = HashMap.compareComparables(kc, k, pk);
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
                    root = balanceInsertion(root, x);
                }
                x = next;
            }
            moveRootToFront(tab, root);
        }

        final Node<K, V> untreeify(HashMap<K, V> map) {
            Node<K, V> tl = null;
            Node<K, V> hd = null;
            for (Node<K, V> q = this; q != null; q = q.next) {
                Node<K, V> p = map.replacementNode(q, null);
                if (tl == null) {
                    hd = p;
                } else {
                    tl.next = p;
                }
                tl = p;
            }
            return hd;
        }

        /* JADX WARNING: Missing block: B:17:0x0030, code skipped:
            if (r4 != null) goto L_0x0032;
     */
        /* JADX WARNING: Missing block: B:29:0x0054, code skipped:
            return r8;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] tab, int h, K k, V v) {
            K root = this.parent != null ? root() : this;
            boolean searched = false;
            Class<?> kc = null;
            K p = root;
            while (true) {
                K pk;
                TreeNode<K, V> q;
                int i = p.hash;
                int ph = i;
                if (i > h) {
                    i = -1;
                } else if (ph < h) {
                    i = 1;
                } else {
                    K k2 = p.key;
                    pk = k2;
                    if (k2 == k || (k != null && k.equals(pk))) {
                        return p;
                    }
                    if (kc == null) {
                        Class<?> comparableClassFor = HashMap.comparableClassFor(k);
                        kc = comparableClassFor;
                    }
                    i = HashMap.compareComparables(kc, k, pk);
                    int dir = i;
                    if (i != 0) {
                        i = dir;
                    }
                    if (!searched) {
                        TreeNode<K, V> find;
                        searched = true;
                        TreeNode treeNode = p.left;
                        dir = treeNode;
                        if (treeNode != null) {
                            find = dir.find(h, k, kc);
                            q = find;
                            if (find != null) {
                                break;
                            }
                        }
                        treeNode = p.right;
                        dir = treeNode;
                        if (treeNode != null) {
                            find = dir.find(h, k, kc);
                            q = find;
                            if (find != null) {
                                break;
                            }
                        }
                    }
                    i = tieBreakOrder(k, pk);
                }
                pk = p;
                K k3 = i <= 0 ? p.left : p.right;
                p = k3;
                if (k3 == null) {
                    Node<K, V> xpn = pk.next;
                    q = map.newTreeNode(h, k, v, xpn);
                    if (i <= 0) {
                        pk.left = q;
                    } else {
                        pk.right = q;
                    }
                    pk.next = q;
                    q.prev = pk;
                    q.parent = pk;
                    if (xpn != null) {
                        ((TreeNode) xpn).prev = q;
                    }
                    moveRootToFront(tab, balanceInsertion(root, q));
                    return null;
                }
            }
            return p;
        }

        final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] tab, boolean movable) {
            Node<K, V>[] nodeArr = tab;
            if (nodeArr != null) {
                int length = nodeArr.length;
                int n = length;
                if (length != 0) {
                    TreeNode<K, V> thisR;
                    length = (n - 1) & this.hash;
                    TreeNode<K, V> first = nodeArr[length];
                    TreeNode<K, V> root = first;
                    TreeNode<K, V> succ = this.next;
                    TreeNode<K, V> pred = this.prev;
                    if (pred == null) {
                        first = succ;
                        nodeArr[length] = succ;
                    } else {
                        pred.next = succ;
                    }
                    if (succ != null) {
                        succ.prev = pred;
                    }
                    if (first != null) {
                        TreeNode<K, V> treeNode;
                        if (root.parent != null) {
                            root = root.root();
                        }
                        if (!(root == null || root.right == null)) {
                            TreeNode<K, V> treeNode2 = root.left;
                            TreeNode<K, V> rl = treeNode2;
                            if (treeNode2 != null) {
                                int i;
                                if (rl.left == null) {
                                    i = n;
                                    treeNode = root;
                                    nodeArr[length] = first.untreeify(map);
                                    return;
                                }
                                TreeNode<K, V> sr;
                                TreeNode<K, V> pl = this.left;
                                TreeNode<K, V> pr = this.right;
                                if (pl == null || pr == null) {
                                    treeNode = root;
                                    if (pl != null) {
                                        thisR = pl;
                                    } else if (pr != null) {
                                        thisR = pr;
                                    } else {
                                        thisR = this;
                                    }
                                } else {
                                    TreeNode<K, V> s = pr;
                                    while (true) {
                                        TreeNode<K, V> treeNode3 = s.left;
                                        TreeNode<K, V> sl = treeNode3;
                                        if (treeNode3 == null) {
                                            break;
                                        }
                                        s = sl;
                                    }
                                    boolean c = s.red;
                                    s.red = this.red;
                                    this.red = c;
                                    sr = s.right;
                                    thisR = this.parent;
                                    if (s == pr) {
                                        this.parent = s;
                                        s.right = this;
                                        i = n;
                                        treeNode = root;
                                    } else {
                                        n = s.parent;
                                        this.parent = n;
                                        if (n != 0) {
                                            treeNode = root;
                                            if (s == n.left) {
                                                n.left = this;
                                            } else {
                                                n.right = this;
                                            }
                                        } else {
                                            treeNode = root;
                                        }
                                        s.right = pr;
                                        if (pr != null) {
                                            pr.parent = s;
                                        }
                                    }
                                    this.left = 0;
                                    this.right = sr;
                                    if (sr != null) {
                                        sr.parent = this;
                                    }
                                    s.left = pl;
                                    if (pl != null) {
                                        pl.parent = s;
                                    }
                                    s.parent = thisR;
                                    if (thisR == null) {
                                        root = s;
                                    } else {
                                        if (this == thisR.left) {
                                            thisR.left = s;
                                        } else {
                                            thisR.right = s;
                                        }
                                        root = treeNode;
                                    }
                                    if (sr != null) {
                                        n = sr;
                                    } else {
                                        n = this;
                                    }
                                    thisR = n;
                                    treeNode = root;
                                }
                                if (thisR != this) {
                                    n = this.parent;
                                    thisR.parent = n;
                                    if (n == 0) {
                                        treeNode = thisR;
                                    } else if (this == n.left) {
                                        n.left = thisR;
                                    } else {
                                        n.right = thisR;
                                    }
                                    this.parent = null;
                                    this.right = null;
                                    this.left = null;
                                }
                                n = treeNode;
                                root = this.red ? n : balanceDeletion(n, thisR);
                                if (thisR == this) {
                                    sr = this.parent;
                                    this.parent = null;
                                    if (sr != null) {
                                        if (this == sr.left) {
                                            sr.left = null;
                                        } else if (this == sr.right) {
                                            sr.right = null;
                                        }
                                    }
                                }
                                if (movable) {
                                    moveRootToFront(nodeArr, root);
                                }
                                return;
                            }
                        }
                        treeNode = root;
                        nodeArr[length] = first.untreeify(map);
                        return;
                    }
                    return;
                }
            }
            HashMap<K, V> hashMap = map;
        }

        final void split(HashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
            TreeNode<K, V> hiTail = null;
            int lc = 0;
            int hc = 0;
            TreeNode<K, V> hiHead = null;
            TreeNode<K, V> loTail = null;
            TreeNode<K, V> loHead = null;
            TreeNode<K, V> e = this;
            while (e != null) {
                TreeNode<K, V> next = e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    e.prev = loTail;
                    if (loTail == null) {
                        loHead = e;
                    } else {
                        loTail.next = e;
                    }
                    loTail = e;
                    lc++;
                } else {
                    e.prev = hiTail;
                    if (hiTail == null) {
                        hiHead = e;
                    } else {
                        hiTail.next = e;
                    }
                    hiTail = e;
                    hc++;
                }
                e = next;
            }
            if (loHead != null) {
                if (lc <= 6) {
                    tab[index] = loHead.untreeify(map);
                } else {
                    tab[index] = loHead;
                    if (hiHead != null) {
                        loHead.treeify(tab);
                    }
                }
            }
            if (hiHead == null) {
                return;
            }
            if (hc <= 6) {
                tab[index + bit] = hiHead.untreeify(map);
                return;
            }
            tab[index + bit] = hiHead;
            if (loHead != null) {
                hiHead.treeify(tab);
            }
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
                        r.red = false;
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
                        l.red = false;
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
                                xp.red = false;
                                if (xpp != null) {
                                    xpp.red = true;
                                    root = rotateRight(root, xpp);
                                }
                            }
                        } else {
                            xppr.red = false;
                            xp.red = false;
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
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    } else {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                } else {
                    x.red = false;
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
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
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
                            xpr2.red = false;
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
                                        treeNode.red = false;
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
                                    xpr2.red = xp == null ? false : xp.red;
                                    xpr = xpr2.right;
                                    sr = xpr;
                                    if (xpr != null) {
                                        sr.red = false;
                                    }
                                }
                                if (xp != null) {
                                    xp.red = false;
                                    root = rotateLeft(root, xp);
                                }
                                x = root;
                            }
                        }
                    } else {
                        if (xpl != null && xpl.red) {
                            xpl.red = false;
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
                                        xpr2.red = false;
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
                                    xpl.red = xp == null ? false : xp.red;
                                    xpr = xpl.left;
                                    treeNode = xpr;
                                    if (xpr != null) {
                                        treeNode.red = false;
                                    }
                                }
                                if (xp != null) {
                                    xp.red = false;
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
                return false;
            }
            if (tn != null && tn.prev != t) {
                return false;
            }
            if (tp != null && t != tp.left && t != tp.right) {
                return false;
            }
            if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
                return false;
            }
            if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
                return false;
            }
            if (t.red && tl != null && tl.red && tr != null && tr.red) {
                return false;
            }
            if (tl != null && !checkInvariants(tl)) {
                return false;
            }
            if (tr == null || checkInvariants(tr)) {
                return true;
            }
            return false;
        }
    }

    final class Values extends AbstractCollection<V> {
        Values() {
        }

        public final int size() {
            return HashMap.this.size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<V> iterator() {
            return new ValueIterator();
        }

        public final boolean contains(Object o) {
            return HashMap.this.containsValue(o);
        }

        public final Spliterator<V> spliterator() {
            return new ValueSpliterator(HashMap.this, 0, -1, 0, 0);
        }

        public final void forEach(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            } else if (HashMap.this.size > 0) {
                Node<K, V>[] nodeArr = HashMap.this.table;
                Node<K, V>[] tab = nodeArr;
                if (nodeArr != null) {
                    int mc = HashMap.this.modCount;
                    for (int i = 0; i < tab.length && HashMap.this.modCount == mc; i++) {
                        for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                            action.accept(e.value);
                        }
                    }
                    if (HashMap.this.modCount != mc) {
                        throw new ConcurrentModificationException();
                    }
                }
            }
        }
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {
        EntrySet() {
        }

        public final int size() {
            return HashMap.this.size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public final boolean contains(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry) o;
            Object key = e.getKey();
            Node<K, V> candidate = HashMap.this.getNode(HashMap.hash(key), key);
            if (candidate != null && candidate.equals(e)) {
                z = true;
            }
            return z;
        }

        public final boolean remove(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry) o;
            Object key = e.getKey();
            if (HashMap.this.removeNode(HashMap.hash(key), key, e.getValue(), true, true) != null) {
                z = true;
            }
            return z;
        }

        public final Spliterator<Entry<K, V>> spliterator() {
            return new EntrySpliterator(HashMap.this, 0, -1, 0, 0);
        }

        public final void forEach(Consumer<? super Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            } else if (HashMap.this.size > 0) {
                Node<K, V>[] nodeArr = HashMap.this.table;
                Node<K, V>[] tab = nodeArr;
                if (nodeArr != null) {
                    int mc = HashMap.this.modCount;
                    for (int i = 0; i < tab.length && HashMap.this.modCount == mc; i++) {
                        for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                            action.accept(e);
                        }
                    }
                    if (HashMap.this.modCount != mc) {
                        throw new ConcurrentModificationException();
                    }
                }
            }
        }
    }

    final class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        public final int size() {
            return HashMap.this.size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<K> iterator() {
            return new KeyIterator();
        }

        public final boolean contains(Object o) {
            return HashMap.this.containsKey(o);
        }

        public final boolean remove(Object key) {
            return HashMap.this.removeNode(HashMap.hash(key), key, null, false, true) != null;
        }

        public final Spliterator<K> spliterator() {
            return new KeySpliterator(HashMap.this, 0, -1, 0, 0);
        }

        public final void forEach(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            } else if (HashMap.this.size > 0) {
                Node<K, V>[] nodeArr = HashMap.this.table;
                Node<K, V>[] tab = nodeArr;
                if (nodeArr != null) {
                    int mc = HashMap.this.modCount;
                    for (int i = 0; i < tab.length && HashMap.this.modCount == mc; i++) {
                        for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                            action.accept(e.key);
                        }
                    }
                    if (HashMap.this.modCount != mc) {
                        throw new ConcurrentModificationException();
                    }
                }
            }
        }
    }

    static final int hash(Object key) {
        if (key == null) {
            return 0;
        }
        int hashCode = key.hashCode();
        return hashCode ^ (hashCode >>> 16);
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

    static final int tableSizeFor(int cap) {
        int n = cap - 1;
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

    public HashMap(int initialCapacity, float loadFactor) {
        StringBuilder stringBuilder;
        if (initialCapacity >= 0) {
            if (initialCapacity > MAXIMUM_CAPACITY) {
                initialCapacity = MAXIMUM_CAPACITY;
            }
            if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal load factor: ");
                stringBuilder.append(loadFactor);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.loadFactor = loadFactor;
            this.threshold = tableSizeFor(initialCapacity);
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal initial capacity: ");
        stringBuilder.append(initialCapacity);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            if (this.table == null) {
                float ft = (((float) s) / this.loadFactor) + 1.0f;
                int t = ft < 1.07374182E9f ? (int) ft : MAXIMUM_CAPACITY;
                if (t > this.threshold) {
                    this.threshold = tableSizeFor(t);
                }
            } else if (s > this.threshold) {
                resize();
            }
            for (Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                putVal(hash(key), key, e.getValue(), false, evict);
            }
        }
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public V get(Object key) {
        Node<K, V> node = getNode(hash(key), key);
        return node == null ? null : node.value;
    }

    final Node<K, V> getNode(int hash, Object key) {
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] tab = nodeArr;
        if (nodeArr != null) {
            int length = tab.length;
            int n = length;
            if (length > 0) {
                Node<K, V> node = tab[(n - 1) & hash];
                Node<K, V> first = node;
                if (node != null) {
                    K k;
                    if (first.hash == hash) {
                        k = first.key;
                        K k2 = k;
                        if (k == key || (key != null && key.equals(k2))) {
                            return first;
                        }
                    }
                    node = first.next;
                    Node<K, V> e = node;
                    if (node != null) {
                        if (first instanceof TreeNode) {
                            return ((TreeNode) first).getTreeNode(hash, key);
                        }
                        do {
                            if (e.hash == hash) {
                                k = e.key;
                                K k3 = k;
                                if (k == key || (key != null && key.equals(k3))) {
                                    return e;
                                }
                            }
                            node = e.next;
                            e = node;
                        } while (node != null);
                    }
                }
            }
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x0079  */
    /* JADX WARNING: Missing block: B:3:0x000e, code skipped:
            if (r0 == 0) goto L_0x0013;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        int length;
        int n;
        int i = hash;
        K k = key;
        V v = value;
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] tab = nodeArr;
        if (nodeArr != null) {
            length = tab.length;
            n = length;
        }
        nodeArr = resize();
        tab = nodeArr;
        n = nodeArr.length;
        Node<K, V>[] tab2 = tab;
        length = (n - 1) & i;
        int i2 = length;
        Node<K, V> node = tab2[length];
        Node<K, V> p = node;
        if (node == null) {
            tab2[i2] = newNode(i, k, v, null);
        } else {
            K k2;
            if (p.hash == i) {
                K k3 = p.key;
                k2 = k3;
                if (k3 == k || (k != null && k.equals(k2))) {
                    node = p;
                    if (node != null) {
                        V oldValue = node.value;
                        if (!onlyIfAbsent || oldValue == null) {
                            node.value = v;
                        }
                        afterNodeAccess(node);
                        return oldValue;
                    }
                }
            }
            if (p instanceof TreeNode) {
                node = ((TreeNode) p).putTreeVal(this, tab2, i, k, v);
            } else {
                Node<K, V> e;
                length = 0;
                while (true) {
                    Node<K, V> node2 = p.next;
                    e = node2;
                    if (node2 == null) {
                        p.next = newNode(i, k, v, null);
                        if (length >= 7) {
                            treeifyBin(tab2, i);
                        }
                    } else {
                        if (e.hash == i) {
                            k2 = e.key;
                            K k4 = k2;
                            if (k2 != k) {
                                if (k != null && k.equals(k4)) {
                                    break;
                                }
                            }
                            break;
                        }
                        boolean z = evict;
                        p = e;
                        length++;
                    }
                }
                node = e;
            }
            if (node != null) {
            }
        }
        this.modCount++;
        length = this.size + 1;
        this.size = length;
        if (length > this.threshold) {
            resize();
        }
        afterNodeInsertion(evict);
        return null;
    }

    final Node<K, V>[] resize() {
        int newCap;
        Node<K, V>[] oldTab = this.table;
        int j = 0;
        int oldCap = oldTab == null ? 0 : oldTab.length;
        int oldThr = this.threshold;
        int newThr = 0;
        int i = Integer.MAX_VALUE;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                this.threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            int i2 = oldCap << 1;
            newCap = i2;
            if (i2 < MAXIMUM_CAPACITY && oldCap >= 16) {
                newThr = oldThr << 1;
            }
        } else if (oldThr > 0) {
            newCap = oldThr;
        } else {
            newCap = 16;
            newThr = 12;
        }
        if (newThr == 0) {
            float ft = ((float) newCap) * this.loadFactor;
            if (newCap < MAXIMUM_CAPACITY && ft < 1.07374182E9f) {
                i = (int) ft;
            }
            newThr = i;
        }
        this.threshold = newThr;
        Node<K, V>[] newTab = new Node[newCap];
        this.table = newTab;
        if (oldTab != null) {
            while (j < oldCap) {
                Node<K, V> node = oldTab[j];
                Node<K, V> e = node;
                if (node != null) {
                    oldTab[j] = null;
                    if (e.next == null) {
                        newTab[e.hash & (newCap - 1)] = e;
                    } else if (e instanceof TreeNode) {
                        ((TreeNode) e).split(this, newTab, j, oldCap);
                    } else {
                        Node<K, V> loTail = null;
                        Node<K, V> hiHead = null;
                        Node<K, V> loHead = null;
                        Node<K, V> e2 = e;
                        e = null;
                        Node<K, V> next;
                        do {
                            next = e2.next;
                            if ((e2.hash & oldCap) == 0) {
                                if (loTail == null) {
                                    loHead = e2;
                                } else {
                                    loTail.next = e2;
                                }
                                loTail = e2;
                            } else {
                                if (e == null) {
                                    hiHead = e2;
                                } else {
                                    e.next = e2;
                                }
                                e = e2;
                            }
                            e2 = next;
                        } while (next != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (e != null) {
                            e.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
                j++;
            }
        }
        return newTab;
    }

    final void treeifyBin(Node<K, V>[] tab, int hash) {
        if (tab != null) {
            int length = tab.length;
            int n = length;
            if (length >= 64) {
                length = (n - 1) & hash;
                int index = length;
                Node<K, V> node = tab[length];
                Node<K, V> e = node;
                if (node != null) {
                    TreeNode<K, V> hd = null;
                    TreeNode<K, V> tl = null;
                    Node<K, V> node2;
                    do {
                        TreeNode<K, V> p = replacementTreeNode(e, null);
                        if (tl == null) {
                            hd = p;
                        } else {
                            p.prev = tl;
                            tl.next = p;
                        }
                        tl = p;
                        node2 = e.next;
                        e = node2;
                    } while (node2 != null);
                    tab[index] = hd;
                    if (hd != null) {
                        hd.treeify(tab);
                        return;
                    }
                    return;
                }
                return;
            }
        }
        resize();
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    public V remove(Object key) {
        Node<K, V> removeNode = removeNode(hash(key), key, null, false, true);
        return removeNode == null ? null : removeNode.value;
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0052  */
    /* JADX WARNING: Missing block: B:35:0x005f, code skipped:
            if (r11.equals(r6) == false) goto L_0x0087;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] tab = nodeArr;
        if (nodeArr != null) {
            int length = tab.length;
            int n = length;
            if (length > 0) {
                length = (n - 1) & hash;
                int index = length;
                Node<K, V> node = tab[length];
                Node<K, V> p = node;
                if (node != null) {
                    K k;
                    node = null;
                    if (p.hash == hash) {
                        k = p.key;
                        K k2 = k;
                        if (k == key || (key != null && key.equals(k2))) {
                            node = p;
                            if (node != null) {
                                if (matchValue) {
                                    V v = node.value;
                                    V v2 = v;
                                    if (v != value) {
                                        if (value != null) {
                                        }
                                    }
                                }
                                if (node instanceof TreeNode) {
                                    ((TreeNode) node).removeTreeNode(this, tab, movable);
                                } else if (node == p) {
                                    tab[index] = node.next;
                                } else {
                                    p.next = node.next;
                                }
                                this.modCount++;
                                this.size--;
                                afterNodeRemoval(node);
                                return node;
                            }
                        }
                    }
                    Node<K, V> node2 = p.next;
                    Node<K, V> e = node2;
                    if (node2 != null) {
                        if (p instanceof TreeNode) {
                            node = ((TreeNode) p).getTreeNode(hash, key);
                        } else {
                            do {
                                if (e.hash == hash) {
                                    k = e.key;
                                    K k3 = k;
                                    if (k == key || (key != null && key.equals(k3))) {
                                        node = e;
                                        break;
                                    }
                                }
                                p = e;
                                node2 = e.next;
                                e = node2;
                            } while (node2 != null);
                        }
                    }
                    if (node != null) {
                    }
                }
            }
        }
        return null;
    }

    public void clear() {
        this.modCount++;
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] tab = nodeArr;
        if (nodeArr != null && this.size > 0) {
            int i = 0;
            this.size = 0;
            while (i < tab.length) {
                tab[i] = null;
                i++;
            }
        }
    }

    public boolean containsValue(Object value) {
        Node<K, V>[] nodeArr = this.table;
        Node<K, V>[] tab = nodeArr;
        if (nodeArr != null && this.size > 0) {
            for (Node<K, V> e : tab) {
                for (Node<K, V> e2 = tab[i]; e2 != null; e2 = e2.next) {
                    V v = e2.value;
                    V v2 = v;
                    if (v == value || (value != null && value.equals(v2))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Set<K> keySet() {
        Set<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet ks2 = new KeySet();
        this.keySet = ks2;
        return ks2;
    }

    public Collection<V> values() {
        Collection<V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        Values vs2 = new Values();
        this.values = vs2;
        return vs2;
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> set = this.entrySet;
        Set<Entry<K, V>> es = set;
        if (set != null) {
            return es;
        }
        set = new EntrySet();
        this.entrySet = set;
        return set;
    }

    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> node = getNode(hash(key), key);
        return node == null ? defaultValue : node.value;
    }

    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        Node<K, V> node = getNode(hash(key), key);
        Node<K, V> e = node;
        if (node != null) {
            V v = e.value;
            V v2 = v;
            if (v == oldValue || (v2 != null && v2.equals(oldValue))) {
                e.value = newValue;
                afterNodeAccess(e);
                return true;
            }
        }
        return false;
    }

    public V replace(K key, V value) {
        Node<K, V> node = getNode(hash(key), key);
        Node<K, V> e = node;
        if (node == null) {
            return null;
        }
        V oldValue = e.value;
        e.value = value;
        afterNodeAccess(e);
        return oldValue;
    }

    /* JADX WARNING: Missing block: B:7:0x001c, code skipped:
            if (r3 == 0) goto L_0x001e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        K k = key;
        Function<? super K, ? extends V> function = mappingFunction;
        if (function != null) {
            Node<K, V>[] nodeArr;
            Node<K, V>[] tab;
            int length;
            int n;
            V oldValue;
            int hash = hash(key);
            int binCount = 0;
            TreeNode<K, V> t = null;
            Node<K, V> old = null;
            if (this.size <= this.threshold) {
                nodeArr = this.table;
                tab = nodeArr;
                if (nodeArr != null) {
                    length = tab.length;
                    n = length;
                }
            }
            nodeArr = resize();
            tab = nodeArr;
            n = nodeArr.length;
            Node<K, V>[] tab2 = tab;
            length = (n - 1) & hash;
            int i = length;
            Node<K, V> node = tab2[length];
            Node<K, V> first = node;
            if (node != null) {
                if (first instanceof TreeNode) {
                    TreeNode<K, V> treeNode = (TreeNode) first;
                    t = treeNode;
                    old = treeNode.getTreeNode(hash, k);
                } else {
                    length = 0;
                    Node<K, V> e = first;
                    Node<K, V> node2;
                    do {
                        if (e.hash == hash) {
                            K k2 = e.key;
                            K k3 = k2;
                            if (k2 == k || (k != null && k.equals(k3))) {
                                old = e;
                                break;
                            }
                        }
                        length++;
                        node2 = e.next;
                        e = node2;
                    } while (node2 != null);
                    binCount = length;
                }
                if (old != null) {
                    V v = old.value;
                    oldValue = v;
                    if (v != null) {
                        afterNodeAccess(old);
                        return oldValue;
                    }
                }
            }
            n = binCount;
            TreeNode<K, V> t2 = t;
            Node<K, V> old2 = old;
            oldValue = function.apply(k);
            if (oldValue == null) {
                return null;
            }
            if (old2 != null) {
                old2.value = oldValue;
                afterNodeAccess(old2);
                return oldValue;
            }
            V v2;
            if (t2 != null) {
                V v3 = oldValue;
                t2.putTreeVal(this, tab2, hash, k, v3);
                v2 = v3;
            } else {
                int binCount2 = n;
                v2 = oldValue;
                tab2[i] = newNode(hash, k, v2, first);
                if (binCount2 >= 7) {
                    treeifyBin(tab2, hash);
                }
            }
            this.modCount++;
            this.size++;
            afterNodeInsertion(true);
            return v2;
        }
        throw new NullPointerException();
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction != null) {
            int hash = hash(key);
            Node<K, V> node = getNode(hash, key);
            Node<K, V> e = node;
            if (node != null) {
                V v = e.value;
                V oldValue = v;
                if (v != null) {
                    V v2 = remappingFunction.apply(key, oldValue);
                    if (v2 != null) {
                        e.value = v2;
                        afterNodeAccess(e);
                        return v2;
                    }
                    removeNode(hash, key, null, false, true);
                }
            }
            return null;
        }
        throw new NullPointerException();
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0061  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006c  */
    /* JADX WARNING: Missing block: B:7:0x001c, code skipped:
            if (r3 == 0) goto L_0x001e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        K k = key;
        BiFunction<? super K, ? super V, ? extends V> biFunction = remappingFunction;
        if (biFunction != null) {
            Node<K, V>[] nodeArr;
            Node<K, V>[] tab;
            int length;
            int n;
            TreeNode<K, V> t;
            Node<K, V> old;
            V oldValue;
            V v;
            int hash = hash(key);
            TreeNode<K, V> treeNode = null;
            Node<K, V> old2 = null;
            if (this.size <= this.threshold) {
                nodeArr = this.table;
                tab = nodeArr;
                if (nodeArr != null) {
                    length = tab.length;
                    n = length;
                }
            }
            nodeArr = resize();
            tab = nodeArr;
            n = nodeArr.length;
            Node<K, V>[] tab2 = tab;
            length = (n - 1) & hash;
            int i = length;
            Node<K, V> node = tab2[length];
            Node<K, V> first = node;
            if (node != null) {
                if (first instanceof TreeNode) {
                    TreeNode<K, V> treeNode2 = (TreeNode) first;
                    treeNode = treeNode2;
                    old2 = treeNode2.getTreeNode(hash, k);
                } else {
                    length = 0;
                    Node<K, V> e = first;
                    Node<K, V> node2;
                    do {
                        if (e.hash == hash) {
                            K k2 = e.key;
                            K binCount = k2;
                            if (k2 == k || (k != null && k.equals(binCount))) {
                                old2 = e;
                                break;
                            }
                        }
                        length++;
                        node2 = e.next;
                        e = node2;
                    } while (node2 != null);
                    t = null;
                    old = old2;
                    n = length;
                    oldValue = old != null ? null : old.value;
                    v = biFunction.apply(k, oldValue);
                    V v2;
                    int binCount2;
                    if (old != null) {
                        v2 = oldValue;
                        binCount2 = n;
                        V v3 = v;
                        if (v3 == null) {
                            return v3;
                        }
                        V v4;
                        if (t != null) {
                            V v5 = v3;
                            t.putTreeVal(this, tab2, hash, k, v3);
                            v4 = v5;
                        } else {
                            v4 = v3;
                            tab2[i] = newNode(hash, k, v4, first);
                            if (binCount2 >= 7) {
                                treeifyBin(tab2, hash);
                            }
                        }
                        this.modCount++;
                        this.size++;
                        afterNodeInsertion(true);
                        return v4;
                    } else if (v != null) {
                        old.value = v;
                        afterNodeAccess(old);
                        v2 = oldValue;
                        binCount2 = n;
                        return v;
                    } else {
                        V v6 = v;
                        removeNode(hash, k, null, false, 1);
                        return v6;
                    }
                }
            }
            n = 0;
            t = treeNode;
            old = old2;
            if (old != null) {
            }
            oldValue = old != null ? null : old.value;
            v = biFunction.apply(k, oldValue);
            if (old != null) {
            }
        } else {
            throw new NullPointerException();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0067  */
    /* JADX WARNING: Missing block: B:8:0x0020, code skipped:
            if (r3 == 0) goto L_0x0022;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        K k = key;
        V v = value;
        BiFunction<? super V, ? super V, ? extends V> biFunction = remappingFunction;
        if (v == null) {
            throw new NullPointerException();
        } else if (biFunction != null) {
            Node<K, V>[] nodeArr;
            Node<K, V>[] tab;
            int length;
            int n;
            TreeNode<K, V> t;
            Node<K, V> old;
            int hash = hash(key);
            TreeNode<K, V> treeNode = null;
            Node<K, V> old2 = null;
            if (this.size <= this.threshold) {
                nodeArr = this.table;
                tab = nodeArr;
                if (nodeArr != null) {
                    length = tab.length;
                    n = length;
                }
            }
            nodeArr = resize();
            tab = nodeArr;
            n = nodeArr.length;
            Node<K, V>[] tab2 = tab;
            length = (n - 1) & hash;
            int i = length;
            Node<K, V> node = tab2[length];
            Node<K, V> first = node;
            if (node != null) {
                if (first instanceof TreeNode) {
                    TreeNode<K, V> treeNode2 = (TreeNode) first;
                    treeNode = treeNode2;
                    old2 = treeNode2.getTreeNode(hash, k);
                } else {
                    length = 0;
                    Node<K, V> e = first;
                    Node<K, V> node2;
                    do {
                        if (e.hash == hash) {
                            K k2 = e.key;
                            K binCount = k2;
                            if (k2 == k || (k != null && k.equals(binCount))) {
                                old2 = e;
                                break;
                            }
                        }
                        length++;
                        node2 = e.next;
                        e = node2;
                    } while (node2 != null);
                    t = null;
                    old = old2;
                    n = length;
                    int i2;
                    if (old == null) {
                        V v2;
                        V v3;
                        if (old.value != null) {
                            v2 = biFunction.apply(old.value, v);
                        } else {
                            v2 = v;
                        }
                        V v4 = v2;
                        if (v4 != null) {
                            old.value = v4;
                            afterNodeAccess(old);
                            v3 = v4;
                            i2 = n;
                        } else {
                            v3 = v4;
                            removeNode(hash, k, null, false, 1);
                        }
                        return v3;
                    }
                    i2 = n;
                    if (v != null) {
                        if (t != null) {
                            t.putTreeVal(this, tab2, hash, k, v);
                        } else {
                            tab2[i] = newNode(hash, k, v, first);
                            if (i2 >= 7) {
                                treeifyBin(tab2, hash);
                            }
                        }
                        this.modCount++;
                        this.size++;
                        afterNodeInsertion(true);
                    }
                    return v;
                }
            }
            n = 0;
            t = treeNode;
            old = old2;
            if (old == null) {
            }
        } else {
            throw new NullPointerException();
        }
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        } else if (this.size > 0) {
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] tab = nodeArr;
            if (nodeArr != null) {
                int mc = this.modCount;
                for (int i = 0; i < tab.length && mc == this.modCount; i++) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                        action.accept(e.key, e.value);
                    }
                }
                if (this.modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        } else if (this.size > 0) {
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] tab = nodeArr;
            if (nodeArr != null) {
                int mc = this.modCount;
                for (Node<K, V> e : tab) {
                    for (Node<K, V> e2 = tab[i]; e2 != null; e2 = e2.next) {
                        e2.value = function.apply(e2.key, e2.value);
                    }
                }
                if (this.modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    public Object clone() {
        try {
            HashMap<K, V> result = (HashMap) super.clone();
            result.reinitialize();
            result.putMapEntries(this, false);
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    final float loadFactor() {
        return this.loadFactor;
    }

    final int capacity() {
        if (this.table != null) {
            return this.table.length;
        }
        if (this.threshold > 0) {
            return this.threshold;
        }
        return 16;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        int buckets = capacity();
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(this.size);
        internalWriteEntries(s);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        reinitialize();
        StringBuilder stringBuilder;
        if (this.loadFactor <= 0.0f || Float.isNaN(this.loadFactor)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal load factor: ");
            stringBuilder.append(this.loadFactor);
            throw new InvalidObjectException(stringBuilder.toString());
        }
        s.readInt();
        int mappings = s.readInt();
        if (mappings < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal mappings count: ");
            stringBuilder.append(mappings);
            throw new InvalidObjectException(stringBuilder.toString());
        } else if (mappings > 0) {
            int i;
            float lf = Math.min(Math.max(0.25f, this.loadFactor), 4.0f);
            float fc = (((float) mappings) / lf) + 1.0f;
            if (fc < 16.0f) {
                i = 16;
            } else {
                i = fc >= 1.07374182E9f ? MAXIMUM_CAPACITY : tableSizeFor((int) fc);
            }
            int cap = i;
            float ft = ((float) cap) * lf;
            i = (cap >= MAXIMUM_CAPACITY || ft >= 1.07374182E9f) ? Integer.MAX_VALUE : (int) ft;
            this.threshold = i;
            this.table = new Node[cap];
            i = 0;
            while (true) {
                int i2 = i;
                if (i2 < mappings) {
                    K key = s.readObject();
                    putVal(hash(key), key, s.readObject(), false, false);
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
        return new Node(hash, key, value, next);
    }

    Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        return new Node(p.hash, p.key, p.value, next);
    }

    TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        return new TreeNode(hash, key, value, next);
    }

    TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        return new TreeNode(p.hash, p.key, p.value, next);
    }

    void reinitialize() {
        this.table = null;
        this.entrySet = null;
        this.keySet = null;
        this.values = null;
        this.modCount = 0;
        this.threshold = 0;
        this.size = 0;
    }

    void afterNodeAccess(Node<K, V> node) {
    }

    void afterNodeInsertion(boolean evict) {
    }

    void afterNodeRemoval(Node<K, V> node) {
    }

    void internalWriteEntries(ObjectOutputStream s) throws IOException {
        if (this.size > 0) {
            Node<K, V>[] nodeArr = this.table;
            Node<K, V>[] tab = nodeArr;
            if (nodeArr != null) {
                for (Node<K, V> e : tab) {
                    for (Node<K, V> e2 = tab[i]; e2 != null; e2 = e2.next) {
                        s.writeObject(e2.key);
                        s.writeObject(e2.value);
                    }
                }
            }
        }
    }
}
