package android.icu.impl;

import java.util.concurrent.ConcurrentHashMap;

public abstract class SoftCache<K, V, D> extends CacheBase<K, V, D> {
    private ConcurrentHashMap<K, Object> map = new ConcurrentHashMap();

    public final V getInstance(K key, D data) {
        CacheValue<V> mapValue = this.map.get(key);
        if (mapValue == null) {
            V value = createInstance(key, data);
            CacheValue instance = (value == null || !CacheValue.futureInstancesWillBeStrong()) ? CacheValue.getInstance(value) : value;
            mapValue = this.map.putIfAbsent(key, instance);
            if (mapValue == null) {
                return value;
            }
            if (mapValue instanceof CacheValue) {
                return mapValue.resetIfCleared(value);
            }
            return mapValue;
        } else if (!(mapValue instanceof CacheValue)) {
            return mapValue;
        } else {
            CacheValue<V> cv = mapValue;
            if (cv.isNull()) {
                return null;
            }
            V value2 = cv.get();
            if (value2 != null) {
                return value2;
            }
            return cv.resetIfCleared(createInstance(key, data));
        }
    }
}
