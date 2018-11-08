package java.util.function;

import java.util.Objects;
import java.util.function.-$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw.AnonymousClass1;

@FunctionalInterface
public interface LongUnaryOperator {
    long applyAsLong(long j);

    LongUnaryOperator compose(LongUnaryOperator before) {
        Objects.requireNonNull(before);
        return new AnonymousClass1((byte) 1, this, before);
    }

    /* synthetic */ long lambda$-java_util_function_LongUnaryOperator_2602(LongUnaryOperator before, long v) {
        return applyAsLong(before.applyAsLong(v));
    }

    LongUnaryOperator andThen(LongUnaryOperator after) {
        Objects.requireNonNull(after);
        return new AnonymousClass1((byte) 0, this, after);
    }

    /* synthetic */ long lambda$-java_util_function_LongUnaryOperator_3361(LongUnaryOperator after, long t) {
        return after.applyAsLong(applyAsLong(t));
    }

    static LongUnaryOperator identity() {
        return -$Lambda$3g4RjBxfqC_Dwp6jYcBusyNaYpw.$INST$0;
    }

    static /* synthetic */ long lambda$-java_util_function_LongUnaryOperator_3638(long t) {
        return t;
    }
}
