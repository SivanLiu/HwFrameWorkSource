package java.lang.reflect;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class WeakCache<K, P, V> {
    private final ConcurrentMap<Object, ConcurrentMap<Object, Supplier<V>>> map = new ConcurrentHashMap();
    private final ReferenceQueue<K> refQueue = new ReferenceQueue();
    private final ConcurrentMap<Supplier<V>, Boolean> reverseMap = new ConcurrentHashMap();
    private final BiFunction<K, P, ?> subKeyFactory;
    private final BiFunction<K, P, V> valueFactory;

    private final class Factory implements Supplier<V> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private final K key;
        private final P parameter;
        private final Object subKey;
        private final ConcurrentMap<Object, Supplier<V>> valuesMap;

        static {
            Class cls = WeakCache.class;
        }

        Factory(K key, P parameter, Object subKey, ConcurrentMap<Object, Supplier<V>> valuesMap) {
            this.key = key;
            this.parameter = parameter;
            this.subKey = subKey;
            this.valuesMap = valuesMap;
        }

        public synchronized V get() {
            if (((Supplier) this.valuesMap.get(this.subKey)) != this) {
                return null;
            }
            try {
                V value = Objects.requireNonNull(WeakCache.this.valueFactory.apply(this.key, this.parameter));
                if (value == null) {
                    this.valuesMap.remove(this.subKey, this);
                }
                CacheValue<V> cacheValue = new CacheValue(value);
                if (this.valuesMap.replace(this.subKey, this, cacheValue)) {
                    WeakCache.this.reverseMap.put(cacheValue, Boolean.TRUE);
                    return value;
                }
                throw new AssertionError((Object) "Should not reach here");
            } catch (Throwable th) {
                if (null == null) {
                    this.valuesMap.remove(this.subKey, this);
                }
            }
        }
    }

    private interface Value<V> extends Supplier<V> {
    }

    private static final class CacheKey<K> extends WeakReference<K> {
        private static final Object NULL_KEY = new Object();
        private final int hash;

        static <K> Object valueOf(K key, ReferenceQueue<K> refQueue) {
            if (key == null) {
                return NULL_KEY;
            }
            return new CacheKey(key, refQueue);
        }

        private CacheKey(K key, ReferenceQueue<K> refQueue) {
            super(key, refQueue);
            this.hash = System.identityHashCode(key);
        }

        public int hashCode() {
            return this.hash;
        }

        /* JADX WARNING: Missing block: B:7:0x001c, code skipped:
            if (r1 == ((java.lang.reflect.WeakCache.CacheKey) r3).get()) goto L_0x0021;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean equals(Object obj) {
            if (obj != this) {
                if (obj != null && obj.getClass() == getClass()) {
                    K k = get();
                    K key = k;
                    if (k != null) {
                    }
                }
                return false;
            }
            return true;
        }

        void expungeFrom(ConcurrentMap<?, ? extends ConcurrentMap<?, ?>> map, ConcurrentMap<?, Boolean> reverseMap) {
            ConcurrentMap<?, ?> valuesMap = (ConcurrentMap) map.remove(this);
            if (valuesMap != null) {
                for (Object cacheValue : valuesMap.values()) {
                    reverseMap.remove(cacheValue);
                }
            }
        }
    }

    private static final class CacheValue<V> extends WeakReference<V> implements Value<V> {
        private final int hash;

        CacheValue(V value) {
            super(value);
            this.hash = System.identityHashCode(value);
        }

        public int hashCode() {
            return this.hash;
        }

        /* JADX WARNING: Missing block: B:6:0x0014, code skipped:
            if (r1 == ((java.lang.reflect.WeakCache.Value) r3).get()) goto L_0x0019;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean equals(Object obj) {
            if (obj != this) {
                if (obj instanceof Value) {
                    V v = get();
                    V value = v;
                    if (v != null) {
                    }
                }
                return false;
            }
            return true;
        }
    }

    private static final class LookupValue<V> implements Value<V> {
        private final V value;

        LookupValue(V value) {
            this.value = value;
        }

        public V get() {
            return this.value;
        }

        public int hashCode() {
            return System.identityHashCode(this.value);
        }

        public boolean equals(Object obj) {
            return obj == this || ((obj instanceof Value) && this.value == ((Value) obj).get());
        }
    }

    public WeakCache(BiFunction<K, P, ?> subKeyFactory, BiFunction<K, P, V> valueFactory) {
        this.subKeyFactory = (BiFunction) Objects.requireNonNull(subKeyFactory);
        this.valueFactory = (BiFunction) Objects.requireNonNull(valueFactory);
    }

    public V get(K key, P parameter) {
        Objects.requireNonNull(parameter);
        expungeStaleEntries();
        Object cacheKey = CacheKey.valueOf(key, this.refQueue);
        ConcurrentMap<Object, Supplier<V>> valuesMap = (ConcurrentMap) this.map.get(cacheKey);
        if (valuesMap == null) {
            ConcurrentMap concurrentMap = this.map;
            ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
            valuesMap = concurrentHashMap;
            ConcurrentMap<Object, Supplier<V>> oldValuesMap = (ConcurrentMap) concurrentMap.putIfAbsent(cacheKey, concurrentHashMap);
            if (oldValuesMap != null) {
                valuesMap = oldValuesMap;
            }
        }
        Object subKey = Objects.requireNonNull(this.subKeyFactory.apply(key, parameter));
        Factory factory = null;
        Supplier<V> supplier = (Supplier) valuesMap.get(subKey);
        while (true) {
            Factory factory2 = factory;
            if (supplier != null) {
                V value = supplier.get();
                if (value != null) {
                    return value;
                }
            }
            if (factory2 == null) {
                factory = new Factory(key, parameter, subKey, valuesMap);
            } else {
                factory = factory2;
            }
            if (supplier == null) {
                supplier = (Supplier) valuesMap.putIfAbsent(subKey, factory);
                if (supplier == null) {
                    supplier = factory;
                }
            } else if (valuesMap.replace(subKey, supplier, factory)) {
                supplier = factory;
            } else {
                supplier = (Supplier) valuesMap.get(subKey);
            }
        }
    }

    public boolean containsValue(V value) {
        Objects.requireNonNull(value);
        expungeStaleEntries();
        return this.reverseMap.containsKey(new LookupValue(value));
    }

    public int size() {
        expungeStaleEntries();
        return this.reverseMap.size();
    }

    private void expungeStaleEntries() {
        while (true) {
            CacheKey<K> cacheKey = (CacheKey) this.refQueue.poll();
            CacheKey<K> cacheKey2 = cacheKey;
            if (cacheKey != null) {
                cacheKey2.expungeFrom(this.map, this.reverseMap);
            } else {
                return;
            }
        }
    }
}
