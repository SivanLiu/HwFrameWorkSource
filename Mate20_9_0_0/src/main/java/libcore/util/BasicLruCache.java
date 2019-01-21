package libcore.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class BasicLruCache<K, V> {
    private final LinkedHashMap<K, V> map;
    private final int maxSize;

    public BasicLruCache(int maxSize) {
        if (maxSize > 0) {
            this.maxSize = maxSize;
            this.map = new LinkedHashMap(0, 0.75f, true);
            return;
        }
        throw new IllegalArgumentException("maxSize <= 0");
    }

    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            r0 = create(r3);
     */
    /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:10:0x0013, code skipped:
            if (r0 == null) goto L_0x0022;
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r2.map.put(r3, r0);
            trimToSize(r2.maxSize);
     */
    /* JADX WARNING: Missing block: B:15:0x0022, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:16:0x0023, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final V get(K key) {
        if (key != null) {
            synchronized (this) {
                V result = this.map.get(key);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new NullPointerException("key == null");
    }

    public final synchronized V put(K key, V value) {
        V previous;
        if (key == null) {
            throw new NullPointerException("key == null");
        } else if (value != null) {
            previous = this.map.put(key, value);
            trimToSize(this.maxSize);
        } else {
            throw new NullPointerException("value == null");
        }
        return previous;
    }

    private void trimToSize(int maxSize) {
        while (this.map.size() > maxSize) {
            Entry<K, V> toEvict = this.map.eldest();
            K key = toEvict.getKey();
            V value = toEvict.getValue();
            this.map.remove(key);
            entryEvicted(key, value);
        }
    }

    protected void entryEvicted(K k, V v) {
    }

    protected V create(K k) {
        return null;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized void evictAll() {
        trimToSize(0);
    }
}
