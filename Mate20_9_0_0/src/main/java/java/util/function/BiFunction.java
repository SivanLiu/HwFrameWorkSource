package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);

    <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$BiFunction$q-2HhQ1fzCu6oYNirKhp1W_vpSM(this, after);
    }
}
