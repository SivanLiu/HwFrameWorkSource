package sun.util.locale;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class LocaleObjectCache<K, V> {
    private ConcurrentMap<K, CacheEntry<K, V>> map;
    private ReferenceQueue<V> queue;

    private static class CacheEntry<K, V> extends SoftReference<V> {
        private K key;

        CacheEntry(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        K getKey() {
            return this.key;
        }
    }

    protected abstract V createObject(K k);

    public LocaleObjectCache() {
        this(16, 0.75f, 16);
    }

    public LocaleObjectCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this.queue = new ReferenceQueue();
        this.map = new ConcurrentHashMap(initialCapacity, loadFactor, concurrencyLevel);
    }

    public V get(K key) {
        V value = null;
        cleanStaleEntries();
        CacheEntry<K, V> entry = (CacheEntry) this.map.get(key);
        if (entry != null) {
            value = entry.get();
        }
        if (value == null) {
            V newVal = createObject(key);
            key = normalizeKey(key);
            if (key == null || newVal == null) {
                return null;
            }
            CacheEntry<K, V> newEntry = new CacheEntry(key, newVal, this.queue);
            entry = (CacheEntry) this.map.putIfAbsent(key, newEntry);
            if (entry == null) {
                value = newVal;
            } else {
                value = entry.get();
                if (value == null) {
                    this.map.put(key, newEntry);
                    value = newVal;
                }
            }
        }
        return value;
    }

    protected V put(K key, V value) {
        CacheEntry<K, V> oldEntry = (CacheEntry) this.map.put(key, new CacheEntry(key, value, this.queue));
        return oldEntry == null ? null : oldEntry.get();
    }

    private void cleanStaleEntries() {
        while (true) {
            CacheEntry<K, V> cacheEntry = (CacheEntry) this.queue.poll();
            CacheEntry<K, V> entry = cacheEntry;
            if (cacheEntry != null) {
                this.map.remove(entry.getKey());
            } else {
                return;
            }
        }
    }

    protected K normalizeKey(K key) {
        return key;
    }
}
