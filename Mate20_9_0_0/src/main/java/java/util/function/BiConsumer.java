package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u);

    BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$BiConsumer$V89VXFfSN6jmL-aAoQrZCMiBju4(this, after);
    }

    static /* synthetic */ void lambda$andThen$0(BiConsumer biConsumer, BiConsumer after, Object l, Object r) {
        biConsumer.accept(l, r);
        after.accept(l, r);
    }
}
