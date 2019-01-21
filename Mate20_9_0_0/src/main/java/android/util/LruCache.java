package android.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LruCache<K, V> {
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private final LinkedHashMap<K, V> map;
    private int maxSize;
    private int missCount;
    private int putCount;
    private int size;

    public LruCache(int maxSize) {
        if (maxSize > 0) {
            this.maxSize = maxSize;
            this.map = new LinkedHashMap(0, 0.75f, true);
            return;
        }
        throw new IllegalArgumentException("maxSize <= 0");
    }

    public void resize(int maxSize) {
        if (maxSize > 0) {
            synchronized (this) {
                this.maxSize = maxSize;
            }
            trimToSize(maxSize);
            return;
        }
        throw new IllegalArgumentException("maxSize <= 0");
    }

    /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r1 = create(r5);
     */
    /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            if (r1 != null) goto L_0x0022;
     */
    /* JADX WARNING: Missing block: B:13:0x0021, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:14:0x0022, code skipped:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:16:?, code skipped:
            r4.createCount++;
            r0 = r4.map.put(r5, r1);
     */
    /* JADX WARNING: Missing block: B:17:0x0030, code skipped:
            if (r0 == null) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:18:0x0032, code skipped:
            r4.map.put(r5, r0);
     */
    /* JADX WARNING: Missing block: B:19:0x0038, code skipped:
            r4.size += safeSizeOf(r5, r1);
     */
    /* JADX WARNING: Missing block: B:20:0x0041, code skipped:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            if (r0 == null) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:22:0x0044, code skipped:
            entryRemoved(false, r5, r1, r0);
     */
    /* JADX WARNING: Missing block: B:23:0x0048, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:24:0x0049, code skipped:
            trimToSize(r4.maxSize);
     */
    /* JADX WARNING: Missing block: B:25:0x004e, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final V get(K key) {
        if (key != null) {
            synchronized (this) {
                V mapValue = this.map.get(key);
                if (mapValue != null) {
                    this.hitCount++;
                    return mapValue;
                }
                this.missCount++;
            }
        } else {
            throw new NullPointerException("key == null");
        }
    }

    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }
        V previous;
        synchronized (this) {
            this.putCount++;
            this.size += safeSizeOf(key, value);
            previous = this.map.put(key, value);
            if (previous != null) {
                this.size -= safeSizeOf(key, previous);
            }
        }
        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }
        trimToSize(this.maxSize);
        return previous;
    }

    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append(getClass().getName());
            r1.append(".sizeOf() is reporting inconsistent results!");
     */
    /* JADX WARNING: Missing block: B:20:0x0061, code skipped:
            throw new java.lang.IllegalStateException(r1.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void trimToSize(int maxSize) {
        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (this.size < 0 || (this.map.isEmpty() && this.size != 0)) {
                    break;
                } else if (this.size <= maxSize) {
                    return;
                } else {
                    Entry<K, V> toEvict = this.map.eldest();
                    if (toEvict == null) {
                        return;
                    }
                    key = toEvict.getKey();
                    value = toEvict.getValue();
                    this.map.remove(key);
                    this.size -= safeSizeOf(key, value);
                    this.evictionCount++;
                }
            }
            entryRemoved(true, key, value, null);
        }
    }

    public final V remove(K key) {
        if (key != null) {
            V previous;
            synchronized (this) {
                previous = this.map.remove(key);
                if (previous != null) {
                    this.size -= safeSizeOf(key, previous);
                }
            }
            if (previous != null) {
                entryRemoved(false, key, previous, null);
            }
            return previous;
        }
        throw new NullPointerException("key == null");
    }

    protected void entryRemoved(boolean evicted, K k, V v, V v2) {
    }

    protected V create(K k) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result >= 0) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Negative size: ");
        stringBuilder.append(key);
        stringBuilder.append("=");
        stringBuilder.append(value);
        throw new IllegalStateException(stringBuilder.toString());
    }

    protected int sizeOf(K k, V v) {
        return 1;
    }

    public final void evictAll() {
        trimToSize(-1);
    }

    public final synchronized int size() {
        return this.size;
    }

    public final synchronized int maxSize() {
        return this.maxSize;
    }

    public final synchronized int hitCount() {
        return this.hitCount;
    }

    public final synchronized int missCount() {
        return this.missCount;
    }

    public final synchronized int createCount() {
        return this.createCount;
    }

    public final synchronized int putCount() {
        return this.putCount;
    }

    public final synchronized int evictionCount() {
        return this.evictionCount;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized String toString() {
        int hitPercent;
        int accesses = this.hitCount + this.missCount;
        hitPercent = accesses != 0 ? (100 * this.hitCount) / accesses : 0;
        return String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]", new Object[]{Integer.valueOf(this.maxSize), Integer.valueOf(this.hitCount), Integer.valueOf(this.missCount), Integer.valueOf(hitPercent)});
    }
}
