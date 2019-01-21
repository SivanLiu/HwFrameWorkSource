package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);

    <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return new -$$Lambda$Function$kjgb589uNKoZ3YFTNw1Kwl-DNBo(this, before);
    }

    <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$Function$T8wYIfMRq5hbW0Q4qNkHIIrI-BA(this, after);
    }

    static <T> Function<T, T> identity() {
        return -$$Lambda$Function$1mm3dZ9IMG2T6zAULCCEh3eoHSY.INSTANCE;
    }

    static /* synthetic */ Object lambda$identity$2(Object t) {
        return t;
    }
}
