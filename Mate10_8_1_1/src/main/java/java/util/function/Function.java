package java.util.function;

import java.util.Objects;
import java.util.function.-$Lambda$8RHFAqc40555mGbHb_ZRDG-W__4.AnonymousClass1;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);

    <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return new AnonymousClass1((byte) 1, this, before);
    }

    /* synthetic */ Object lambda$-java_util_function_Function_2660(Function before, Object v) {
        return apply(before.apply(v));
    }

    <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return new AnonymousClass1((byte) 0, this, after);
    }

    /* synthetic */ Object lambda$-java_util_function_Function_3525(Function after, Object t) {
        return after.apply(apply(t));
    }

    static <T> Function<T, T> identity() {
        return -$Lambda$8RHFAqc40555mGbHb_ZRDG-W__4.$INST$0;
    }

    static /* synthetic */ Object lambda$-java_util_function_Function_3851(Object t) {
        return t;
    }
}
