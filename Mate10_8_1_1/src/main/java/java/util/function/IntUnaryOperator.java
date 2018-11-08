package java.util.function;

import java.util.Objects;
import java.util.function.-$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs.AnonymousClass1;

@FunctionalInterface
public interface IntUnaryOperator {
    int applyAsInt(int i);

    IntUnaryOperator compose(IntUnaryOperator before) {
        Objects.requireNonNull(before);
        return new AnonymousClass1((byte) 1, this, before);
    }

    /* synthetic */ int lambda$-java_util_function_IntUnaryOperator_2591(IntUnaryOperator before, int v) {
        return applyAsInt(before.applyAsInt(v));
    }

    IntUnaryOperator andThen(IntUnaryOperator after) {
        Objects.requireNonNull(after);
        return new AnonymousClass1((byte) 0, this, after);
    }

    /* synthetic */ int lambda$-java_util_function_IntUnaryOperator_3344(IntUnaryOperator after, int t) {
        return after.applyAsInt(applyAsInt(t));
    }

    static IntUnaryOperator identity() {
        return -$Lambda$n4VFJtoJH2VuvmBTzi4u5lJWNJs.$INST$0;
    }

    static /* synthetic */ int lambda$-java_util_function_IntUnaryOperator_3617(int t) {
        return t;
    }
}
