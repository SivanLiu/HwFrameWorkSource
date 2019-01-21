package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface LongConsumer {
    void accept(long j);

    LongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$LongConsumer$2wx0fq0YJI8kSCwhsFrV0qxRiZ4(this, after);
    }

    static /* synthetic */ void lambda$andThen$0(LongConsumer longConsumer, LongConsumer after, long t) {
        longConsumer.accept(t);
        after.accept(t);
    }
}
