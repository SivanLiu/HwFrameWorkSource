package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface DoubleConsumer {
    void accept(double d);

    DoubleConsumer andThen(DoubleConsumer after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$DoubleConsumer$HNSB3MjwB-DXE7Kpt1C-BT9h3T8(this, after);
    }

    static /* synthetic */ void lambda$andThen$0(DoubleConsumer doubleConsumer, DoubleConsumer after, double t) {
        doubleConsumer.accept(t);
        after.accept(t);
    }
}
