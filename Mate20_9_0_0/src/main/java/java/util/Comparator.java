package java.util;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

@FunctionalInterface
public interface Comparator<T> {
    int compare(T t, T t2);

    boolean equals(Object obj);

    Comparator<T> reversed() {
        return Collections.reverseOrder(this);
    }

    Comparator<T> thenComparing(Comparator<? super T> other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$Comparator$BZSVCoA8i87ehjxxZ1weEounfDQ(this, other);
    }

    static /* synthetic */ int lambda$thenComparing$36697e65$1(Comparator comparator, Comparator other, Object c1, Object c2) {
        int res = comparator.compare(c1, c2);
        return res != 0 ? res : other.compare(c1, c2);
    }

    <U> Comparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
        return thenComparing(comparing(keyExtractor, keyComparator));
    }

    <U extends Comparable<? super U>> Comparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor) {
        return thenComparing(comparing(keyExtractor));
    }

    Comparator<T> thenComparingInt(ToIntFunction<? super T> keyExtractor) {
        return thenComparing(comparingInt(keyExtractor));
    }

    Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
        return thenComparing(comparingLong(keyExtractor));
    }

    Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        return thenComparing(comparingDouble(keyExtractor));
    }

    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }

    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return NaturalOrderComparator.INSTANCE;
    }

    static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return new NullComparator(true, comparator);
    }

    static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return new NullComparator(false, comparator);
    }

    static <T, U> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return new -$$Lambda$Comparator$KVN0LWz1D1wyrL2gs1CbubvLa9o(keyComparator, keyExtractor);
    }

    static <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new -$$Lambda$Comparator$SPB8K9Yj7Pw1mljm7LpasV7zxWw(keyExtractor);
    }

    static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new -$$Lambda$Comparator$DNgpxUFZqmT4lOBzlVyPjWwvEvw(keyExtractor);
    }

    static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new -$$Lambda$Comparator$4V5k8aLimtS0VsEILEAqQ9UGZYo(keyExtractor);
    }

    static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new -$$Lambda$Comparator$edSxqANnwdmzeJ1aMMcwJWE2wII(keyExtractor);
    }
}
