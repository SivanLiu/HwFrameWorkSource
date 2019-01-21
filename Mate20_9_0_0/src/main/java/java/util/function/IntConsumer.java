package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface IntConsumer {
    void accept(int i);

    IntConsumer andThen(IntConsumer after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$IntConsumer$Zkqv8_f6uSuSHCYm5dVGj2OCUKA(this, after);
    }

    static /* synthetic */ void lambda$andThen$0(IntConsumer intConsumer, IntConsumer after, int t) {
        intConsumer.accept(t);
        after.accept(t);
    }
}
