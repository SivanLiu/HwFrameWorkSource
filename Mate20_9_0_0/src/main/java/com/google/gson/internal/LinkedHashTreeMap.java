package com.google.gson.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

public final class LinkedHashTreeMap<K, V> extends AbstractMap<K, V> implements Serializable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final Comparator<Comparable> NATURAL_ORDER = new Comparator<Comparable>() {
        public int compare(Comparable a, Comparable b) {
            return a.compareTo(b);
        }
    };
    Comparator<? super K> comparator;
    private EntrySet entrySet;
    final Node<K, V> header;
    private KeySet keySet;
    int modCount;
    int size;
    Node<K, V>[] table;
    int threshold;

    static final class AvlBuilder<K, V> {
        private int leavesSkipped;
        private int leavesToSkip;
        private int size;
        private Node<K, V> stack;

        AvlBuilder() {
        }

        void reset(int targetSize) {
            this.leavesToSkip = ((Integer.highestOneBit(targetSize) * 2) - 1) - targetSize;
            this.size = 0;
            this.leavesSkipped = 0;
            this.stack = null;
        }

        void add(Node<K, V> node) {
            node.right = null;
            node.parent = null;
            node.left = null;
            node.height = 1;
            if (this.leavesToSkip > 0 && (this.size & 1) == 0) {
                this.size++;
                this.leavesToSkip--;
                this.leavesSkipped++;
            }
            node.parent = this.stack;
            this.stack = node;
            this.size++;
            if (this.leavesToSkip > 0 && (this.size & 1) == 0) {
                this.size++;
                this.leavesToSkip--;
                this.leavesSkipped++;
            }
            for (int scale = 4; (this.size & (scale - 1)) == scale - 1; scale *= 2) {
                Node<K, V> right;
                Node<K, V> left;
                if (this.leavesSkipped == 0) {
                    right = this.stack;
                    Node<K, V> center = right.parent;
                    left = center.parent;
                    center.parent = left.parent;
                    this.stack = center;
                    center.left = left;
                    center.right = right;
                    center.height = right.height + 1;
                    left.parent = center;
                    right.parent = center;
                } else if (this.leavesSkipped == 1) {
                    right = this.stack;
                    left = right.parent;
                    this.stack = left;
                    left.right = right;
                    left.height = right.height + 1;
                    right.parent = left;
                    this.leavesSkipped = 0;
                } else if (this.leavesSkipped == 2) {
                    this.leavesSkipped = 0;
                }
            }
        }

        Node<K, V> root() {
            Node<K, V> stackTop = this.stack;
            if (stackTop.parent == null) {
                return stackTop;
            }
            throw new IllegalStateException();
        }
    }

    static class AvlIterator<K, V> {
        private Node<K, V> stackTop;

        AvlIterator() {
        }

        void reset(Node<K, V> root) {
            Node<K, V> stackTop = null;
            for (Node<K, V> n = root; n != null; n = n.left) {
                n.parent = stackTop;
                stackTop = n;
            }
            this.stackTop = stackTop;
        }

        public Node<K, V> next() {
            Node<K, V> stackTop = this.stackTop;
            if (stackTop == null) {
                return null;
            }
            Node<K, V> result = stackTop;
            stackTop = result.parent;
            result.parent = null;
            for (Node<K, V> n = result.right; n != null; n = n.left) {
                n.parent = stackTop;
                stackTop = n;
            }
            this.stackTop = stackTop;
            return result;
        }
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {
        EntrySet() {
        }

        public int size() {
            return LinkedHashTreeMap.this.size;
        }

        public Iterator<Entry<K, V>> iterator() {
            return new LinkedTreeMapIterator<Entry<K, V>>() {
                {
                    LinkedHashTreeMap linkedHashTreeMap = LinkedHashTreeMap.this;
                }

                public Entry<K, V> next() {
                    return nextNode();
                }
            };
        }

        public boolean contains(Object o) {
            return (o instanceof Entry) && LinkedHashTreeMap.this.findByEntry((Entry) o) != null;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Node<K, V> node = LinkedHashTreeMap.this.findByEntry((Entry) o);
            if (node == null) {
                return false;
            }
            LinkedHashTreeMap.this.removeInternal(node, true);
            return true;
        }

        public void clear() {
            LinkedHashTreeMap.this.clear();
        }
    }

    final class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        public int size() {
            return LinkedHashTreeMap.this.size;
        }

        public Iterator<K> iterator() {
            return new LinkedTreeMapIterator<K>() {
                {
                    LinkedHashTreeMap linkedHashTreeMap = LinkedHashTreeMap.this;
                }

                public K next() {
                    return nextNode().key;
                }
            };
        }

        public boolean contains(Object o) {
            return LinkedHashTreeMap.this.containsKey(o);
        }

        public boolean remove(Object key) {
            return LinkedHashTreeMap.this.removeInternalByKey(key) != null;
        }

        public void clear() {
            LinkedHashTreeMap.this.clear();
        }
    }

    private abstract class LinkedTreeMapIterator<T> implements Iterator<T> {
        int expectedModCount = LinkedHashTreeMap.this.modCount;
        Node<K, V> lastReturned = null;
        Node<K, V> next = LinkedHashTreeMap.this.header.next;

        LinkedTreeMapIterator() {
        }

        public final boolean hasNext() {
            return this.next != LinkedHashTreeMap.this.header;
        }

        final Node<K, V> nextNode() {
            Node<K, V> e = this.next;
            if (e == LinkedHashTreeMap.this.header) {
                throw new NoSuchElementException();
            } else if (LinkedHashTreeMap.this.modCount == this.expectedModCount) {
                this.next = e.next;
                this.lastReturned = e;
                return e;
            } else {
                throw new ConcurrentModificationException();
            }
        }

        public final void remove() {
            if (this.lastReturned != null) {
                LinkedHashTreeMap.this.removeInternal(this.lastReturned, true);
                this.lastReturned = null;
                this.expectedModCount = LinkedHashTreeMap.this.modCount;
                return;
            }
            throw new IllegalStateException();
        }
    }

    static final class Node<K, V> implements Entry<K, V> {
        final int hash;
        int height;
        final K key;
        Node<K, V> left;
        Node<K, V> next;
        Node<K, V> parent;
        Node<K, V> prev;
        Node<K, V> right;
        V value;

        Node() {
            this.key = null;
            this.hash = -1;
            this.prev = this;
            this.next = this;
        }

        Node(Node<K, V> parent, K key, int hash, Node<K, V> next, Node<K, V> prev) {
            this.parent = parent;
            this.key = key;
            this.hash = hash;
            this.height = 1;
            this.next = next;
            this.prev = prev;
            prev.next = this;
            next.prev = this;
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry other = (Entry) o;
            if (this.key != null ? !this.key.equals(other.getKey()) : other.getKey() != null) {
                if (this.value != null ? !this.value.equals(other.getValue()) : other.getValue() != null) {
                    z = true;
                }
            }
            return z;
        }

        public int hashCode() {
            int i = 0;
            int hashCode = this.key == null ? 0 : this.key.hashCode();
            if (this.value != null) {
                i = this.value.hashCode();
            }
            return hashCode ^ i;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.key);
            stringBuilder.append("=");
            stringBuilder.append(this.value);
            return stringBuilder.toString();
        }

        public Node<K, V> first() {
            Node<K, V> node = this;
            Node<K, V> child = node.left;
            while (child != null) {
                node = child;
                child = node.left;
            }
            return node;
        }

        public Node<K, V> last() {
            Node<K, V> node = this;
            Node<K, V> child = node.right;
            while (child != null) {
                node = child;
                child = node.right;
            }
            return node;
        }
    }

    public LinkedHashTreeMap() {
        this(NATURAL_ORDER);
    }

    public LinkedHashTreeMap(Comparator<? super K> comparator) {
        this.size = 0;
        this.modCount = 0;
        this.comparator = comparator != null ? comparator : NATURAL_ORDER;
        this.header = new Node();
        this.table = new Node[16];
        this.threshold = (this.table.length / 2) + (this.table.length / 4);
    }

    public int size() {
        return this.size;
    }

    public V get(Object key) {
        Node<K, V> node = findByObject(key);
        return node != null ? node.value : null;
    }

    public boolean containsKey(Object key) {
        return findByObject(key) != null;
    }

    public V put(K key, V value) {
        if (key != null) {
            Node<K, V> created = find(key, true);
            V result = created.value;
            created.value = value;
            return result;
        }
        throw new NullPointerException("key == null");
    }

    public void clear() {
        Arrays.fill(this.table, null);
        this.size = 0;
        this.modCount++;
        Node<K, V> header = this.header;
        Node<K, V> e = header.next;
        while (e != header) {
            Node<K, V> next = e.next;
            e.prev = null;
            e.next = null;
            e = next;
        }
        header.prev = header;
        header.next = header;
    }

    public V remove(Object key) {
        Node<K, V> node = removeInternalByKey(key);
        return node != null ? node.value : null;
    }

    Node<K, V> find(K key, boolean create) {
        K k = key;
        Comparator<? super K> comparator = this.comparator;
        Node<K, V>[] table = this.table;
        int hash = secondaryHash(key.hashCode());
        int index = hash & (table.length - 1);
        Node<K, V> nearest = table[index];
        int comparison = 0;
        if (nearest != null) {
            Comparable<Object> comparableKey = comparator == NATURAL_ORDER ? (Comparable) k : null;
            while (true) {
                int compareTo;
                if (comparableKey != null) {
                    compareTo = comparableKey.compareTo(nearest.key);
                } else {
                    compareTo = comparator.compare(k, nearest.key);
                }
                comparison = compareTo;
                if (comparison == 0) {
                    return nearest;
                }
                Node<K, V> child = comparison < 0 ? nearest.left : nearest.right;
                if (child == null) {
                    break;
                }
                nearest = child;
            }
        }
        Node<K, V> nearest2 = nearest;
        int comparison2 = comparison;
        if (!create) {
            return null;
        }
        Node<K, V> header = this.header;
        if (nearest2 != null) {
            Node<K, V> header2 = header;
            nearest = new Node(nearest2, k, hash, header2, header2.prev);
            if (comparison2 < 0) {
                nearest2.left = nearest;
            } else {
                nearest2.right = nearest;
            }
            rebalance(nearest2, true);
        } else if (comparator != NATURAL_ORDER || (k instanceof Comparable)) {
            nearest = new Node(nearest2, k, hash, header, header.prev);
            table[index] = nearest;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(key.getClass().getName());
            stringBuilder.append(" is not Comparable");
            throw new ClassCastException(stringBuilder.toString());
        }
        comparison = this.size;
        this.size = comparison + 1;
        if (comparison > this.threshold) {
            doubleCapacity();
        }
        this.modCount++;
        return nearest;
    }

    Node<K, V> findByObject(Object key) {
        Node<K, V> node = null;
        if (key != null) {
            try {
                node = find(key, false);
            } catch (ClassCastException e) {
                return null;
            }
        }
        return node;
    }

    Node<K, V> findByEntry(Entry<?, ?> entry) {
        Node<K, V> mine = findByObject(entry.getKey());
        boolean valuesEqual = mine != null && equal(mine.value, entry.getValue());
        return valuesEqual ? mine : null;
    }

    private boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static int secondaryHash(int h) {
        h ^= (h >>> 20) ^ (h >>> 12);
        return ((h >>> 7) ^ h) ^ (h >>> 4);
    }

    void removeInternal(Node<K, V> node, boolean unlink) {
        if (unlink) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
        }
        Node<K, V> left = node.left;
        Node<K, V> right = node.right;
        Node<K, V> originalParent = node.parent;
        if (left == null || right == null) {
            if (left != null) {
                replaceInParent(node, left);
                node.left = null;
            } else if (right != null) {
                replaceInParent(node, right);
                node.right = null;
            } else {
                replaceInParent(node, null);
            }
            rebalance(originalParent, false);
            this.size--;
            this.modCount++;
            return;
        }
        Node<K, V> adjacent = left.height > right.height ? left.last() : right.first();
        removeInternal(adjacent, false);
        int leftHeight = 0;
        left = node.left;
        if (left != null) {
            leftHeight = left.height;
            adjacent.left = left;
            left.parent = adjacent;
            node.left = null;
        }
        int rightHeight = 0;
        right = node.right;
        if (right != null) {
            rightHeight = right.height;
            adjacent.right = right;
            right.parent = adjacent;
            node.right = null;
        }
        adjacent.height = Math.max(leftHeight, rightHeight) + 1;
        replaceInParent(node, adjacent);
    }

    Node<K, V> removeInternalByKey(Object key) {
        Node<K, V> node = findByObject(key);
        if (node != null) {
            removeInternal(node, true);
        }
        return node;
    }

    private void replaceInParent(Node<K, V> node, Node<K, V> replacement) {
        Node<K, V> parent = node.parent;
        node.parent = null;
        if (replacement != null) {
            replacement.parent = parent;
        }
        if (parent == null) {
            this.table[node.hash & (this.table.length - 1)] = replacement;
        } else if (parent.left == node) {
            parent.left = replacement;
        } else {
            parent.right = replacement;
        }
    }

    private void rebalance(Node<K, V> unbalanced, boolean insert) {
        for (Node<K, V> node = unbalanced; node != null; node = node.parent) {
            Node<K, V> left = node.left;
            Node<K, V> right = node.right;
            int rightLeftHeight = 0;
            int leftHeight = left != null ? left.height : 0;
            int rightHeight = right != null ? right.height : 0;
            int delta = leftHeight - rightHeight;
            Node<K, V> rightLeft;
            int rightDelta;
            if (delta == -2) {
                rightLeft = right.left;
                Node<K, V> rightRight = right.right;
                int rightRightHeight = rightRight != null ? rightRight.height : 0;
                if (rightLeft != null) {
                    rightLeftHeight = rightLeft.height;
                }
                rightDelta = rightLeftHeight - rightRightHeight;
                if (rightDelta == -1 || (rightDelta == 0 && !insert)) {
                    rotateLeft(node);
                } else {
                    rotateRight(right);
                    rotateLeft(node);
                }
                if (insert) {
                    return;
                }
            } else if (delta == 2) {
                rightLeft = left.left;
                Node<K, V> leftRight = left.right;
                rightDelta = leftRight != null ? leftRight.height : 0;
                if (rightLeft != null) {
                    rightLeftHeight = rightLeft.height;
                }
                int leftDelta = rightLeftHeight - rightDelta;
                if (leftDelta == 1 || (leftDelta == 0 && !insert)) {
                    rotateRight(node);
                } else {
                    rotateLeft(left);
                    rotateRight(node);
                }
                if (insert) {
                    return;
                }
            } else if (delta == 0) {
                node.height = leftHeight + 1;
                if (insert) {
                    return;
                }
            } else {
                node.height = Math.max(leftHeight, rightHeight) + 1;
                if (!insert) {
                    return;
                }
            }
        }
    }

    private void rotateLeft(Node<K, V> root) {
        Node<K, V> left = root.left;
        Node<K, V> pivot = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;
        root.right = pivotLeft;
        if (pivotLeft != null) {
            pivotLeft.parent = root;
        }
        replaceInParent(root, pivot);
        pivot.left = root;
        root.parent = pivot;
        int i = 0;
        root.height = Math.max(left != null ? left.height : 0, pivotLeft != null ? pivotLeft.height : 0) + 1;
        int i2 = root.height;
        if (pivotRight != null) {
            i = pivotRight.height;
        }
        pivot.height = Math.max(i2, i) + 1;
    }

    private void rotateRight(Node<K, V> root) {
        Node<K, V> pivot = root.left;
        Node<K, V> right = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;
        root.left = pivotRight;
        if (pivotRight != null) {
            pivotRight.parent = root;
        }
        replaceInParent(root, pivot);
        pivot.right = root;
        root.parent = pivot;
        int i = 0;
        root.height = Math.max(right != null ? right.height : 0, pivotRight != null ? pivotRight.height : 0) + 1;
        int i2 = root.height;
        if (pivotLeft != null) {
            i = pivotLeft.height;
        }
        pivot.height = Math.max(i2, i) + 1;
    }

    public Set<Entry<K, V>> entrySet() {
        EntrySet result = this.entrySet;
        if (result != null) {
            return result;
        }
        Set entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    public Set<K> keySet() {
        KeySet result = this.keySet;
        if (result != null) {
            return result;
        }
        Set keySet = new KeySet();
        this.keySet = keySet;
        return keySet;
    }

    private void doubleCapacity() {
        this.table = doubleCapacity(this.table);
        this.threshold = (this.table.length / 2) + (this.table.length / 4);
    }

    static <K, V> Node<K, V>[] doubleCapacity(Node<K, V>[] oldTable) {
        int oldCapacity = oldTable.length;
        Node<K, V>[] newTable = new Node[(oldCapacity * 2)];
        AvlIterator<K, V> iterator = new AvlIterator();
        AvlBuilder<K, V> leftBuilder = new AvlBuilder();
        AvlBuilder<K, V> rightBuilder = new AvlBuilder();
        for (int i = 0; i < oldCapacity; i++) {
            Node<K, V> root = oldTable[i];
            if (root != null) {
                Node<K, V> next;
                Node<K, V> node;
                iterator.reset(root);
                int leftSize = 0;
                int rightSize = 0;
                while (true) {
                    next = iterator.next();
                    node = next;
                    if (next == null) {
                        break;
                    } else if ((node.hash & oldCapacity) == 0) {
                        leftSize++;
                    } else {
                        rightSize++;
                    }
                }
                leftBuilder.reset(leftSize);
                rightBuilder.reset(rightSize);
                iterator.reset(root);
                while (true) {
                    next = iterator.next();
                    node = next;
                    if (next == null) {
                        break;
                    } else if ((node.hash & oldCapacity) == 0) {
                        leftBuilder.add(node);
                    } else {
                        rightBuilder.add(node);
                    }
                }
                Node node2 = null;
                newTable[i] = leftSize > 0 ? leftBuilder.root() : null;
                int i2 = i + oldCapacity;
                if (rightSize > 0) {
                    node2 = rightBuilder.root();
                }
                newTable[i2] = node2;
            }
        }
        return newTable;
    }

    private Object writeReplace() throws ObjectStreamException {
        return new LinkedHashMap(this);
    }
}
