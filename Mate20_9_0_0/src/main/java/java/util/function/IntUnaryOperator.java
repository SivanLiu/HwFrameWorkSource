package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface IntUnaryOperator {
    int applyAsInt(int i);

    IntUnaryOperator compose(IntUnaryOperator before) {
        Objects.requireNonNull(before);
        return new -$$Lambda$IntUnaryOperator$HBwqtJWwVkNFC818pCzqJ5KBLm0(this, before);
    }

    IntUnaryOperator andThen(IntUnaryOperator after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$IntUnaryOperator$F5l8VUrgGacAzg6VKzynJCfDBx4(this, after);
    }

    static IntUnaryOperator identity() {
        return -$$Lambda$IntUnaryOperator$DKxG0-oyUAYjk17nXTQ5x-EXFgU.INSTANCE;
    }

    static /* synthetic */ int lambda$identity$2(int t) {
        return t;
    }
}
