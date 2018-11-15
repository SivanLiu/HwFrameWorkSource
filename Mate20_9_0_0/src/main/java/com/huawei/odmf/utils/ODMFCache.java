package com.huawei.odmf.utils;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class ODMFCache<K, V> implements Cache<K, V> {
    private float DEFAULT_LOAD_FACTOR;
    private int DEFAULT_TABLE_SIZE;
    private int currentNum;
    private int hitCount;
    private final Object lock;
    private LinkedHashMap<K, V> map;
    private int maxNum;
    private int missCount;

    public ODMFCache() {
        this.maxNum = 1000;
        this.DEFAULT_TABLE_SIZE = 16;
        this.DEFAULT_LOAD_FACTOR = 0.75f;
        this.currentNum = 0;
        this.lock = new Object();
        this.map = new LinkedHashMap<K, V>(this.DEFAULT_TABLE_SIZE, this.DEFAULT_LOAD_FACTOR, true) {
            protected boolean removeEldestEntry(Entry<K, V> entry) {
                boolean b = ODMFCache.this.currentNum >= ODMFCache.this.maxNum;
                if (b) {
                    ODMFCache.this.currentNum = ODMFCache.this.currentNum - 1;
                }
                return b;
            }
        };
    }

    public ODMFCache(int maxNum) {
        this.maxNum = 1000;
        this.DEFAULT_TABLE_SIZE = 16;
        this.DEFAULT_LOAD_FACTOR = 0.75f;
        this.currentNum = 0;
        this.lock = new Object();
        if (maxNum <= 0) {
            throw new IllegalArgumentException("maxNum<=0||maxSize <= 0");
        }
        this.maxNum = maxNum;
        final int i = maxNum;
        this.map = new LinkedHashMap<K, V>(this.DEFAULT_TABLE_SIZE, this.DEFAULT_LOAD_FACTOR, true) {
            protected boolean removeEldestEntry(Entry<K, V> entry) {
                boolean b = ODMFCache.this.currentNum >= i;
                if (b) {
                    ODMFCache.this.currentNum = ODMFCache.this.currentNum - 1;
                }
                return b;
            }
        };
    }

    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key == null || value == null");
        }
        V previous;
        synchronized (this.lock) {
            previous = this.map.put(key, value);
            this.currentNum++;
            if (previous != null) {
                this.currentNum--;
            }
        }
        return previous;
    }

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

    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        V previous;
        synchronized (this.lock) {
            previous = this.map.remove(key);
            if (previous != null) {
                this.currentNum--;
            }
        }
        return previous;
    }

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

    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key == null || value == null");
        }
        boolean contains;
        synchronized (this.lock) {
            contains = this.map.keySet().contains(key);
        }
        return contains;
    }

    public boolean containsValue(V value) {
        if (value == null) {
            throw new IllegalArgumentException("key == null || value == null");
        }
        boolean contains;
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
