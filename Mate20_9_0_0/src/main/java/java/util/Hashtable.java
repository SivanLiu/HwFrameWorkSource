package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, Serializable {
    private static final int ENTRIES = 2;
    private static final int KEYS = 0;
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final int VALUES = 1;
    private static final long serialVersionUID = 1421746759512286392L;
    private transient int count;
    private volatile transient Set<Entry<K, V>> entrySet;
    private volatile transient Set<K> keySet;
    private float loadFactor;
    private transient int modCount;
    private transient HashtableEntry<?, ?>[] table;
    private int threshold;
    private volatile transient Collection<V> values;

    private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
        HashtableEntry<?, ?> entry;
        protected int expectedModCount = Hashtable.this.modCount;
        int index = this.table.length;
        boolean iterator;
        HashtableEntry<?, ?> lastReturned;
        HashtableEntry<?, ?>[] table = Hashtable.this.table;
        int type;

        Enumerator(int type, boolean iterator) {
            this.type = type;
            this.iterator = iterator;
        }

        public boolean hasMoreElements() {
            HashtableEntry<?, ?> e = this.entry;
            int i = this.index;
            HashtableEntry<?, ?>[] t = this.table;
            while (e == null && i > 0) {
                i--;
                e = t[i];
            }
            this.entry = e;
            this.index = i;
            return e != null;
        }

        public T nextElement() {
            HashtableEntry<?, ?> et = this.entry;
            int i = this.index;
            HashtableEntry<?, ?>[] t = this.table;
            while (et == null && i > 0) {
                i--;
                et = t[i];
            }
            this.entry = et;
            this.index = i;
            if (et != null) {
                T e = this.entry;
                this.lastReturned = e;
                this.entry = e.next;
                if (this.type == 0) {
                    return e.key;
                }
                return this.type == 1 ? e.value : e;
            } else {
                throw new NoSuchElementException("Hashtable Enumerator");
            }
        }

        public boolean hasNext() {
            return hasMoreElements();
        }

        public T next() {
            if (Hashtable.this.modCount == this.expectedModCount) {
                return nextElement();
            }
            throw new ConcurrentModificationException();
        }

        public void remove() {
            if (!this.iterator) {
                throw new UnsupportedOperationException();
            } else if (this.lastReturned == null) {
                throw new IllegalStateException("Hashtable Enumerator");
            } else if (Hashtable.this.modCount == this.expectedModCount) {
                synchronized (Hashtable.this) {
                    HashtableEntry<?, ?>[] tab = Hashtable.this.table;
                    int index = (this.lastReturned.hash & Integer.MAX_VALUE) % tab.length;
                    HashtableEntry<K, V> e = tab[index];
                    HashtableEntry<K, V> prev = null;
                    while (e != null) {
                        if (e == this.lastReturned) {
                            Hashtable.this.modCount = Hashtable.this.modCount + 1;
                            this.expectedModCount++;
                            if (prev == null) {
                                tab[index] = e.next;
                            } else {
                                prev.next = e.next;
                            }
                            Hashtable.this.count = Hashtable.this.count - 1;
                            this.lastReturned = null;
                        } else {
                            prev = e;
                            e = e.next;
                        }
                    }
                    throw new ConcurrentModificationException();
                }
            } else {
                throw new ConcurrentModificationException();
            }
        }
    }

    private static class HashtableEntry<K, V> implements Entry<K, V> {
        final int hash;
        final K key;
        HashtableEntry<K, V> next;
        V value;

        protected HashtableEntry(int hash, K key, V value, HashtableEntry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        protected Object clone() {
            return new HashtableEntry(this.hash, this.key, this.value, this.next == null ? null : (HashtableEntry) this.next.clone());
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.value;
        }

        public V setValue(V value) {
            if (value != null) {
                V oldValue = this.value;
                this.value = value;
                return oldValue;
            }
            throw new NullPointerException();
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry) o;
            if (this.key != null ? !this.key.equals(e.getKey()) : e.getKey() != null) {
                if (this.value != null ? !this.value.equals(e.getValue()) : e.getValue() != null) {
                    z = true;
                }
            }
            return z;
        }

        public int hashCode() {
            return this.hash ^ Objects.hashCode(this.value);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.key.toString());
            stringBuilder.append("=");
            stringBuilder.append(this.value.toString());
            return stringBuilder.toString();
        }
    }

    private class ValueCollection extends AbstractCollection<V> {
        private ValueCollection() {
        }

        public Iterator<V> iterator() {
            return Hashtable.this.getIterator(1);
        }

        public int size() {
            return Hashtable.this.count;
        }

        public boolean contains(Object o) {
            return Hashtable.this.containsValue(o);
        }

        public void clear() {
            Hashtable.this.clear();
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
        private EntrySet() {
        }

        public Iterator<Entry<K, V>> iterator() {
            return Hashtable.this.getIterator(2);
        }

        public boolean add(Entry<K, V> o) {
            return super.add(o);
        }

        public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> entry = (Entry) o;
            Object key = entry.getKey();
            HashtableEntry<?, ?>[] tab = Hashtable.this.table;
            int hash = key.hashCode();
            HashtableEntry<?, ?> e = tab[(Integer.MAX_VALUE & hash) % tab.length];
            while (e != null) {
                if (e.hash == hash && e.equals(entry)) {
                    return true;
                }
                e = e.next;
            }
            return false;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> entry = (Entry) o;
            Object key = entry.getKey();
            HashtableEntry<?, ?>[] tab = Hashtable.this.table;
            int hash = key.hashCode();
            int index = (Integer.MAX_VALUE & hash) % tab.length;
            HashtableEntry<K, V> e = tab[index];
            HashtableEntry<K, V> prev = null;
            while (e != null) {
                if (e.hash == hash && e.equals(entry)) {
                    Hashtable.this.modCount = Hashtable.this.modCount + 1;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    Hashtable.this.count = Hashtable.this.count - 1;
                    e.value = null;
                    return true;
                }
                prev = e;
                e = e.next;
            }
            return false;
        }

        public int size() {
            return Hashtable.this.count;
        }

        public void clear() {
            Hashtable.this.clear();
        }
    }

    private class KeySet extends AbstractSet<K> {
        private KeySet() {
        }

        public Iterator<K> iterator() {
            return Hashtable.this.getIterator(0);
        }

        public int size() {
            return Hashtable.this.count;
        }

        public boolean contains(Object o) {
            return Hashtable.this.containsKey(o);
        }

        public boolean remove(Object o) {
            return Hashtable.this.remove(o) != null;
        }

        public void clear() {
            Hashtable.this.clear();
        }
    }

    public Hashtable(int initialCapacity, float loadFactor) {
        this.modCount = 0;
        StringBuilder stringBuilder;
        if (initialCapacity < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal Capacity: ");
            stringBuilder.append(initialCapacity);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal Load: ");
            stringBuilder.append(loadFactor);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            if (initialCapacity == 0) {
                initialCapacity = 1;
            }
            this.loadFactor = loadFactor;
            this.table = new HashtableEntry[initialCapacity];
            this.threshold = Math.min(initialCapacity, 2147483640);
        }
    }

    public Hashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public Hashtable() {
        this(11, 0.75f);
    }

    public Hashtable(Map<? extends K, ? extends V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }

    public synchronized int size() {
        return this.count;
    }

    public synchronized boolean isEmpty() {
        return this.count == 0;
    }

    public synchronized Enumeration<K> keys() {
        return getEnumeration(0);
    }

    public synchronized Enumeration<V> elements() {
        return getEnumeration(1);
    }

    public synchronized boolean contains(Object value) {
        if (value != null) {
            HashtableEntry<?, ?>[] tab = this.table;
            int i = tab.length;
            while (true) {
                int i2 = i - 1;
                if (i <= 0) {
                    return false;
                }
                for (HashtableEntry<?, ?> e = tab[i2]; e != null; e = e.next) {
                    if (e.value.equals(value)) {
                        return true;
                    }
                }
                i = i2;
            }
        } else {
            throw new NullPointerException();
        }
    }

    public boolean containsValue(Object value) {
        return contains(value);
    }

    public synchronized boolean containsKey(Object key) {
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        HashtableEntry<?, ?> e = tab[(Integer.MAX_VALUE & hash) % tab.length];
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                return true;
            }
            e = e.next;
        }
        return false;
    }

    public synchronized V get(Object key) {
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        HashtableEntry<?, ?> e = tab[(Integer.MAX_VALUE & hash) % tab.length];
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                return e.value;
            }
            e = e.next;
        }
        return null;
    }

    protected void rehash() {
        int oldCapacity = this.table.length;
        HashtableEntry<?, ?>[] oldMap = this.table;
        int newCapacity = (oldCapacity << 1) + 1;
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            if (oldCapacity != MAX_ARRAY_SIZE) {
                newCapacity = MAX_ARRAY_SIZE;
            } else {
                return;
            }
        }
        HashtableEntry<?, ?>[] newMap = new HashtableEntry[newCapacity];
        this.modCount++;
        this.threshold = (int) Math.min(((float) newCapacity) * this.loadFactor, 2.14748365E9f);
        this.table = newMap;
        int i = oldCapacity;
        while (true) {
            int i2 = i - 1;
            if (i > 0) {
                HashtableEntry<K, V> old = oldMap[i2];
                while (old != null) {
                    HashtableEntry<K, V> e = old;
                    old = old.next;
                    int index = (e.hash & Integer.MAX_VALUE) % newCapacity;
                    e.next = newMap[index];
                    newMap[index] = e;
                }
                i = i2;
            } else {
                return;
            }
        }
    }

    private void addEntry(int hash, K key, V value, int index) {
        this.modCount++;
        HashtableEntry<?, ?>[] tab = this.table;
        if (this.count >= this.threshold) {
            rehash();
            tab = this.table;
            hash = key.hashCode();
            index = (Integer.MAX_VALUE & hash) % tab.length;
        }
        tab[index] = new HashtableEntry(hash, key, value, tab[index]);
        this.count++;
    }

    public synchronized V put(K key, V value) {
        if (value != null) {
            HashtableEntry<?, ?>[] tab = this.table;
            int hash = key.hashCode();
            int index = (Integer.MAX_VALUE & hash) % tab.length;
            HashtableEntry<K, V> entry = tab[index];
            while (entry != null) {
                if (entry.hash == hash && entry.key.equals(key)) {
                    V old = entry.value;
                    entry.value = value;
                    return old;
                }
                entry = entry.next;
            }
            addEntry(hash, key, value, index);
            return null;
        }
        throw new NullPointerException();
    }

    public synchronized V remove(Object key) {
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> e = tab[index];
        HashtableEntry<K, V> prev = null;
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                this.modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                this.count--;
                V oldValue = e.value;
                e.value = null;
                return oldValue;
            }
            prev = e;
            e = e.next;
        }
        return null;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> t) {
        for (Entry<? extends K, ? extends V> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public synchronized void clear() {
        HashtableEntry<?, ?>[] tab = this.table;
        this.modCount++;
        int index = tab.length;
        while (true) {
            index--;
            if (index >= 0) {
                tab[index] = null;
            } else {
                this.count = 0;
            }
        }
    }

    public synchronized Object clone() {
        Hashtable<?, ?> t;
        try {
            t = (Hashtable) super.clone();
            t.table = new HashtableEntry[this.table.length];
            int i = this.table.length;
            while (true) {
                int i2 = i - 1;
                HashtableEntry hashtableEntry = null;
                if (i > 0) {
                    HashtableEntry[] hashtableEntryArr = t.table;
                    if (this.table[i2] != null) {
                        hashtableEntry = (HashtableEntry) this.table[i2].clone();
                    }
                    hashtableEntryArr[i2] = hashtableEntry;
                    i = i2;
                } else {
                    t.keySet = null;
                    t.entrySet = null;
                    t.values = null;
                    t.modCount = 0;
                }
            }
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        return t;
    }

    public synchronized String toString() {
        int max = size() - 1;
        if (max == -1) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<K, V>> it = entrySet().iterator();
        sb.append('{');
        int i = 0;
        while (true) {
            Entry<K, V> e = (Entry) it.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key == this ? "(this Map)" : key.toString());
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value.toString());
            if (i == max) {
                sb.append('}');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    private <T> Enumeration<T> getEnumeration(int type) {
        if (this.count == 0) {
            return Collections.emptyEnumeration();
        }
        return new Enumerator(type, false);
    }

    private <T> Iterator<T> getIterator(int type) {
        if (this.count == 0) {
            return Collections.emptyIterator();
        }
        return new Enumerator(type, true);
    }

    public Set<K> keySet() {
        if (this.keySet == null) {
            this.keySet = Collections.synchronizedSet(new KeySet(), this);
        }
        return this.keySet;
    }

    public Set<Entry<K, V>> entrySet() {
        if (this.entrySet == null) {
            this.entrySet = Collections.synchronizedSet(new EntrySet(), this);
        }
        return this.entrySet;
    }

    public Collection<V> values() {
        if (this.values == null) {
            this.values = Collections.synchronizedCollection(new ValueCollection(), this);
        }
        return this.values;
    }

    /* JADX WARNING: Missing block: B:27:0x0047, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> t = (Map) o;
        if (t.size() != size()) {
            return false;
        }
        try {
            for (Entry<K, V> e : entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (t.get(key) == null && t.containsKey(key)) {
                    }
                } else if (!value.equals(t.get(key))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e2) {
            return false;
        } catch (NullPointerException e3) {
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0031, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int hashCode() {
        int h = 0;
        if (this.count != 0) {
            if (this.loadFactor >= 0.0f) {
                this.loadFactor = -this.loadFactor;
                for (HashtableEntry<?, ?> entry : this.table) {
                    for (HashtableEntry<?, ?> entry2 = tab[r3]; entry2 != null; entry2 = entry2.next) {
                        h += entry2.hashCode();
                    }
                }
                this.loadFactor = -this.loadFactor;
                return h;
            }
        }
    }

    public synchronized V getOrDefault(Object key, V defaultValue) {
        V result;
        result = get(key);
        return result == null ? defaultValue : result;
    }

    public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = this.modCount;
        for (HashtableEntry<?, ?> entry : this.table) {
            HashtableEntry<?, ?> entry2;
            while (entry2 != null) {
                action.accept(entry2.key, entry2.value);
                entry2 = entry2.next;
                if (expectedModCount != this.modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = this.modCount;
        for (HashtableEntry<K, V> entry : this.table) {
            HashtableEntry<K, V> entry2;
            while (entry2 != null) {
                entry2.value = Objects.requireNonNull(function.apply(entry2.key, entry2.value));
                entry2 = entry2.next;
                if (expectedModCount != this.modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0027, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized V putIfAbsent(K key, V value) {
        Objects.requireNonNull(value);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> entry = tab[index];
        while (entry != null) {
            if (entry.hash == hash && entry.key.equals(key)) {
                V old = entry.value;
                if (old == null) {
                    entry.value = value;
                }
            } else {
                entry = entry.next;
            }
        }
        addEntry(hash, key, value, index);
        return null;
    }

    public synchronized boolean remove(Object key, Object value) {
        Objects.requireNonNull(value);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> e = tab[index];
        HashtableEntry<K, V> prev = null;
        while (e != null) {
            if (e.hash == hash && e.key.equals(key) && e.value.equals(value)) {
                this.modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                this.count--;
                e.value = null;
                return true;
            }
            prev = e;
            e = e.next;
        }
        return false;
    }

    public synchronized boolean replace(K key, V oldValue, V newValue) {
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        HashtableEntry<K, V> e = tab[(Integer.MAX_VALUE & hash) % tab.length];
        while (e != null) {
            if (e.hash != hash || !e.key.equals(key)) {
                e = e.next;
            } else if (!e.value.equals(oldValue)) {
                return false;
            } else {
                e.value = newValue;
                return true;
            }
        }
        return false;
    }

    public synchronized V replace(K key, V value) {
        Objects.requireNonNull(value);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        HashtableEntry<K, V> e = tab[(Integer.MAX_VALUE & hash) % tab.length];
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
            e = e.next;
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:17:0x0032, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> e = tab[index];
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                return e.value;
            }
            e = e.next;
        }
        V newValue = mappingFunction.apply(key);
        if (newValue != null) {
            addEntry(hash, key, newValue, index);
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0046, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> e = tab[index];
        HashtableEntry<K, V> prev = null;
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                V newValue = remappingFunction.apply(key, e.value);
                if (newValue == null) {
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;
                } else {
                    e.value = newValue;
                }
            } else {
                prev = e;
                e = e.next;
            }
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:17:0x0046, code skipped:
            return r4;
     */
    /* JADX WARNING: Missing block: B:25:0x0056, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> e = tab[index];
        HashtableEntry<K, V> prev = null;
        while (e != null) {
            if (e.hash == hash && Objects.equals(e.key, key)) {
                V newValue = remappingFunction.apply(key, e.value);
                if (newValue == null) {
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;
                } else {
                    e.value = newValue;
                }
            } else {
                prev = e;
                e = e.next;
            }
        }
        V newValue2 = remappingFunction.apply(key, null);
        if (newValue2 != null) {
            addEntry(hash, key, newValue2, index);
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0044, code skipped:
            return r5;
     */
    /* JADX WARNING: Missing block: B:24:0x0050, code skipped:
            return r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        HashtableEntry<?, ?>[] tab = this.table;
        int hash = key.hashCode();
        int index = (Integer.MAX_VALUE & hash) % tab.length;
        HashtableEntry<K, V> e = tab[index];
        HashtableEntry<K, V> prev = null;
        while (e != null) {
            if (e.hash == hash && e.key.equals(key)) {
                V newValue = remappingFunction.apply(e.value, value);
                if (newValue == null) {
                    this.modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    this.count--;
                } else {
                    e.value = newValue;
                }
            } else {
                prev = e;
                e = e.next;
            }
        }
        if (value != null) {
            addEntry(hash, key, value, index);
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0030, code skipped:
            if (r2 == null) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:14:0x0032, code skipped:
            r8.writeObject(r2.key);
            r8.writeObject(r2.value);
            r2 = r2.next;
     */
    /* JADX WARNING: Missing block: B:15:0x003f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeObject(ObjectOutputStream s) throws IOException {
        HashtableEntry<Object, Object> entryStack;
        synchronized (this) {
            HashtableEntry<Object, Object> entryStack2;
            try {
                s.defaultWriteObject();
                s.writeInt(this.table.length);
                s.writeInt(this.count);
                entryStack2 = null;
                int index = 0;
                while (index < this.table.length) {
                    try {
                        for (HashtableEntry<?, ?> entry = this.table[index]; entry != null; entry = entry.next) {
                            entryStack2 = new HashtableEntry(0, entry.key, entry.value, entryStack2);
                        }
                        index++;
                    } catch (Throwable th) {
                        entryStack = th;
                        throw entryStack;
                    }
                }
            } catch (Throwable th2) {
                entryStack2 = null;
                entryStack = th2;
                throw entryStack;
            }
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (this.loadFactor <= 0.0f || Float.isNaN(this.loadFactor)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal Load: ");
            stringBuilder.append(this.loadFactor);
            throw new StreamCorruptedException(stringBuilder.toString());
        }
        int origlength = s.readInt();
        int elements = s.readInt();
        if (elements >= 0) {
            origlength = Math.max(origlength, ((int) (((float) elements) / this.loadFactor)) + 1);
            int length = ((int) (((float) ((elements / 20) + elements)) / this.loadFactor)) + 3;
            if (length > elements && (length & 1) == 0) {
                length--;
            }
            length = Math.min(length, origlength);
            this.table = new HashtableEntry[length];
            this.threshold = (int) Math.min(((float) length) * this.loadFactor, 2.14748365E9f);
            this.count = 0;
            while (elements > 0) {
                reconstitutionPut(this.table, s.readObject(), s.readObject());
                elements--;
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Illegal # of Elements: ");
        stringBuilder2.append(elements);
        throw new StreamCorruptedException(stringBuilder2.toString());
    }

    private void reconstitutionPut(HashtableEntry<?, ?>[] tab, K key, V value) throws StreamCorruptedException {
        if (value != null) {
            int hash = key.hashCode();
            int index = (Integer.MAX_VALUE & hash) % tab.length;
            HashtableEntry<?, ?> e = tab[index];
            while (e != null) {
                if (e.hash == hash && e.key.equals(key)) {
                    throw new StreamCorruptedException();
                }
                e = e.next;
            }
            tab[index] = new HashtableEntry(hash, key, value, tab[index]);
            this.count++;
            return;
        }
        throw new StreamCorruptedException();
    }
}
