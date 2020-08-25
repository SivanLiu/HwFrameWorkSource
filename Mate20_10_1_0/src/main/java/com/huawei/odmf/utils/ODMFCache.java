package com.huawei.odmf.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ODMFCache<K, V> implements Cache<K, V> {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_TABLE_SIZE = 16;
    /* access modifiers changed from: private */
    public int currentNum;
    private int hitCount;
    private final Object lock;
    private LinkedHashMap<K, V> map;
    /* access modifiers changed from: private */
    public int maxNum;
    private int missCount;

    static /* synthetic */ int access$010(ODMFCache x0) {
        int i = x0.currentNum;
        x0.currentNum = i - 1;
        return i;
    }

    public ODMFCache() {
        this.lock = new Object();
        this.maxNum = 1000;
        this.currentNum = 0;
        this.map = new LinkedHashMap<K, V>(16, DEFAULT_LOAD_FACTOR, true) {
            /* class com.huawei.odmf.utils.ODMFCache.AnonymousClass1 */

            /* access modifiers changed from: protected */
            @Override // java.util.LinkedHashMap
            public boolean removeEldestEntry(Entry<K, V> entry) {
                boolean needRemove = ODMFCache.this.currentNum >= ODMFCache.this.maxNum;
                if (needRemove) {
                    ODMFCache.access$010(ODMFCache.this);
                }
                return needRemove;
            }
        };
    }

    public ODMFCache(final int maxNum2) {
        this.lock = new Object();
        this.maxNum = 1000;
        this.currentNum = 0;
        if (maxNum2 <= 0) {
            throw new IllegalArgumentException("maxNum<=0||maxSize <= 0");
        }
        this.maxNum = maxNum2;
        this.map = new LinkedHashMap<K, V>(16, DEFAULT_LOAD_FACTOR, true) {
            /* class com.huawei.odmf.utils.ODMFCache.AnonymousClass2 */

            /* access modifiers changed from: protected */
            @Override // java.util.LinkedHashMap
            public boolean removeEldestEntry(Entry<K, V> entry) {
                boolean needRemove = ODMFCache.this.currentNum >= maxNum2;
                if (needRemove) {
                    ODMFCache.access$010(ODMFCache.this);
                }
                return needRemove;
            }
        };
    }

    @Override // com.huawei.odmf.utils.Cache
    public V put(K key, V value) {
        V previous;
        if (key == null || value == null) {
            throw new IllegalArgumentException("key == null || value == null");
        }
        synchronized (this.lock) {
            previous = this.map.put(key, value);
            this.currentNum++;
            if (previous != null) {
                this.currentNum--;
            }
        }
        return previous;
    }

    @Override // com.huawei.odmf.utils.Cache
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key == null");
        }
        synchronized (this.lock) {
            V mapValue = this.map.get(key);
            if (mapValue != null) {
                this.hitCount++;
                return mapValue;
            }
            this.missCount++;
            return null;
        }
    }

    @Override // com.huawei.odmf.utils.Cache
    public final V remove(K key) {
        V previous;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this.lock) {
            previous = this.map.remove(key);
            if (previous != null) {
                this.currentNum--;
            }
        }
        return previous;
    }

    @Override // com.huawei.odmf.utils.Cache
    public boolean clear() {
        synchronized (this.lock) {
            this.currentNum = 0;
            this.hitCount = 0;
            this.missCount = 0;
            this.map.clear();
        }
        return true;
    }

    public int capacity() {
        int i;
        synchronized (this.lock) {
            i = this.maxNum;
        }
        return i;
    }

    public int size() {
        int i;
        synchronized (this.lock) {
            i = this.currentNum;
        }
        return i;
    }

    @Override // com.huawei.odmf.utils.Cache
    public boolean containsKey(K key) {
        boolean contains;
        if (key == null) {
            throw new IllegalArgumentException("key == null || value == null");
        }
        synchronized (this.lock) {
            contains = this.map.keySet().contains(key);
        }
        return contains;
    }

    @Override // com.huawei.odmf.utils.Cache
    public boolean containsValue(V value) {
        boolean contains;
        if (value == null) {
            throw new IllegalArgumentException("key == null || value == null");
        }
        synchronized (this.lock) {
            contains = this.map.values().contains(value);
        }
        return contains;
    }

    public int getHitRate() {
        int hitPercent;
        synchronized (this.lock) {
            int accesses = this.hitCount + this.missCount;
            hitPercent = accesses != 0 ? (this.hitCount * 100) / accesses : 0;
        }
        return hitPercent;
    }
}
