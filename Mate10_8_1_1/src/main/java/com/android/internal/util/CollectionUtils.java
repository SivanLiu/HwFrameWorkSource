package com.android.internal.util;

import android.util.ArraySet;
import android.util.ExceptionUtils;
import com.android.internal.util.FunctionalUtils.ThrowingConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> List<T> filter(List<T> list, Predicate<? super T> predicate) {
        List result = null;
        for (int i = 0; i < size(list); i++) {
            Object item = list.get(i);
            if (predicate.test(item)) {
                result = ArrayUtils.add((ArrayList) result, item);
            }
        }
        return emptyIfNull(result);
    }

    public static <T> Set<T> filter(Set<T> set, Predicate<? super T> predicate) {
        if (set == null || set.size() == 0) {
            return Collections.emptySet();
        }
        Set add;
        ArraySet<T> result = null;
        Object item;
        if (set instanceof ArraySet) {
            ArraySet<T> arraySet = (ArraySet) set;
            int size = arraySet.size();
            for (int i = 0; i < size; i++) {
                item = arraySet.valueAt(i);
                if (predicate.test(item)) {
                    add = ArrayUtils.add((ArraySet) add, item);
                }
            }
        } else {
            for (Object item2 : set) {
                if (predicate.test(item2)) {
                    result = ArrayUtils.add((ArraySet) result, item2);
                }
            }
        }
        return emptyIfNull(add);
    }

    public static <I, O> List<O> map(List<I> cur, Function<? super I, ? extends O> f) {
        if (ArrayUtils.isEmpty((Collection) cur)) {
            return Collections.emptyList();
        }
        ArrayList<O> result = new ArrayList();
        for (int i = 0; i < cur.size(); i++) {
            result.add(f.apply(cur.get(i)));
        }
        return result;
    }

    public static <I, O> Set<O> map(Set<I> cur, Function<? super I, ? extends O> f) {
        if (ArrayUtils.isEmpty((Collection) cur)) {
            return Collections.emptySet();
        }
        ArraySet<O> result = new ArraySet();
        if (cur instanceof ArraySet) {
            ArraySet<I> arraySet = (ArraySet) cur;
            int size = arraySet.size();
            for (int i = 0; i < size; i++) {
                result.add(f.apply(arraySet.valueAt(i)));
            }
        } else {
            for (I item : cur) {
                result.add(f.apply(item));
            }
        }
        return result;
    }

    public static <I, O> List<O> mapNotNull(List<I> cur, Function<? super I, ? extends O> f) {
        if (ArrayUtils.isEmpty((Collection) cur)) {
            return Collections.emptyList();
        }
        ArrayList<O> result = new ArrayList();
        for (int i = 0; i < cur.size(); i++) {
            O transformed = f.apply(cur.get(i));
            if (transformed != null) {
                result.add(transformed);
            }
        }
        return result;
    }

    public static <T> List<T> emptyIfNull(List<T> cur) {
        return cur == null ? Collections.emptyList() : cur;
    }

    public static <T> Set<T> emptyIfNull(Set<T> cur) {
        return cur == null ? Collections.emptySet() : cur;
    }

    public static int size(Collection<?> cur) {
        return cur != null ? cur.size() : 0;
    }

    public static <T> List<T> filter(List<?> list, Class<T> c) {
        if (ArrayUtils.isEmpty((Collection) list)) {
            return Collections.emptyList();
        }
        List result = null;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (c.isInstance(item)) {
                result = ArrayUtils.add((ArrayList) result, item);
            }
        }
        return emptyIfNull(result);
    }

    public static <T> boolean any(List<T> items, Predicate<T> predicate) {
        return find(items, predicate) != null;
    }

    public static <T> T find(List<T> items, Predicate<T> predicate) {
        if (ArrayUtils.isEmpty((Collection) items)) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    public static <T> List<T> add(List<T> cur, T val) {
        if (cur == null || cur == Collections.emptyList()) {
            cur = new ArrayList();
        }
        cur.add(val);
        return cur;
    }

    public static <T> Set<T> add(Set<T> cur, T val) {
        if (cur == null || cur == Collections.emptySet()) {
            cur = new ArraySet();
        }
        cur.add(val);
        return cur;
    }

    public static <T> List<T> remove(List<T> cur, T val) {
        if (ArrayUtils.isEmpty((Collection) cur)) {
            return emptyIfNull((List) cur);
        }
        cur.remove(val);
        return cur;
    }

    public static <T> Set<T> remove(Set<T> cur, T val) {
        if (ArrayUtils.isEmpty((Collection) cur)) {
            return emptyIfNull((Set) cur);
        }
        cur.remove(val);
        return cur;
    }

    public static <T> List<T> copyOf(List<T> cur) {
        return ArrayUtils.isEmpty((Collection) cur) ? Collections.emptyList() : new ArrayList(cur);
    }

    public static <T> Set<T> copyOf(Set<T> cur) {
        return ArrayUtils.isEmpty((Collection) cur) ? Collections.emptySet() : new ArraySet((Collection) cur);
    }

    public static <T> void forEach(Set<T> cur, ThrowingConsumer<T> action) {
        if (cur != null && action != null) {
            int size = cur.size();
            if (size != 0) {
                try {
                    if (cur instanceof ArraySet) {
                        ArraySet<T> arraySet = (ArraySet) cur;
                        for (int i = 0; i < size; i++) {
                            action.accept(arraySet.valueAt(i));
                        }
                    } else {
                        for (T t : cur) {
                            action.accept(t);
                        }
                    }
                } catch (Exception e) {
                    throw ExceptionUtils.propagate(e);
                }
            }
        }
    }
}
