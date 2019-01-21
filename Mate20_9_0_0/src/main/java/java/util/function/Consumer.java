package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);

    Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$Consumer$fZIgy_f2Fa5seBa8ztxXTExq2p4(this, after);
    }

    static /* synthetic */ void lambda$andThen$0(Consumer consumer, Consumer after, Object t) {
        consumer.accept(t);
        after.accept(t);
    }
}
