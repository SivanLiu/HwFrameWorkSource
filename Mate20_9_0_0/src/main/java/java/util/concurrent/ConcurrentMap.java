package java.util.concurrent;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ConcurrentMap<K, V> extends Map<K, V> {
    V putIfAbsent(K k, V v);

    boolean remove(Object obj, Object obj2);

    V replace(K k, V v);

    boolean replace(K k, V v, V v2);

    V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v != null ? v : defaultValue;
    }

    void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Entry<K, V> entry : entrySet()) {
            try {
                action.accept(entry.getKey(), entry.getValue());
            } catch (IllegalStateException e) {
            }
        }
    }

    void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach(new -$$Lambda$ConcurrentMap$T12JRbgGLhxGbYCuTfff6_dTrMk(this, function));
    }

    static /* synthetic */ void lambda$replaceAll$0(ConcurrentMap concurrentMap, BiFunction function, Object k, Object v) {
        while (!concurrentMap.replace(k, v, function.apply(k, v))) {
            Object obj = concurrentMap.get(k);
            v = obj;
            if (obj == null) {
                return;
            }
        }
    }

    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V oldValue;
        Objects.requireNonNull(mappingFunction);
        V v = get(key);
        V oldValue2 = v;
        if (v == null) {
            v = mappingFunction.apply(key);
            oldValue = v;
            if (v != null) {
                v = putIfAbsent(key, oldValue);
                oldValue2 = v;
                if (v == null) {
                    return oldValue;
                }
            }
        }
        oldValue = oldValue2;
        return oldValue;
    }

    V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V v;
        Objects.requireNonNull(remappingFunction);
        while (true) {
            v = get(key);
            V oldValue = v;
            if (v == null) {
                return null;
            }
            v = remappingFunction.apply(key, oldValue);
            if (v == null) {
                if (remove(key, oldValue)) {
                    break;
                }
            } else if (replace(key, oldValue, v)) {
                break;
            }
        }
        return v;
    }

    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        while (true) {
            V oldValue = get(key);
            while (true) {
                V newValue = remappingFunction.apply(key, oldValue);
                if (newValue != null) {
                    if (oldValue == null) {
                        V putIfAbsent = putIfAbsent(key, newValue);
                        oldValue = putIfAbsent;
                        if (putIfAbsent == null) {
                            return newValue;
                        }
                    } else if (replace(key, oldValue, newValue)) {
                        return newValue;
                    }
                } else if (oldValue == null || remove(key, oldValue)) {
                }
            }
        }
        return null;
    }

    V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        while (true) {
            V putIfAbsent;
            V oldValue = get(key);
            while (oldValue == null) {
                putIfAbsent = putIfAbsent(key, value);
                oldValue = putIfAbsent;
                if (putIfAbsent == null) {
                    return value;
                }
            }
            putIfAbsent = remappingFunction.apply(oldValue, value);
            if (putIfAbsent != null) {
                if (replace(key, oldValue, putIfAbsent)) {
                    return putIfAbsent;
                }
            } else if (remove(key, oldValue)) {
                return null;
            }
        }
    }
}
