package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface LongUnaryOperator {
    long applyAsLong(long j);

    LongUnaryOperator compose(LongUnaryOperator before) {
        Objects.requireNonNull(before);
        return new -$$Lambda$LongUnaryOperator$e52YMvir03pwSw7KvpRuqEbSDRg(this, before);
    }

    LongUnaryOperator andThen(LongUnaryOperator after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$LongUnaryOperator$MxQtG8PPTeFzxiwgpkR3tXC-HLU(this, after);
    }

    static LongUnaryOperator identity() {
        return -$$Lambda$LongUnaryOperator$kI3lBaNH3h6ldTmGeiEUd61CYJI.INSTANCE;
    }

    static /* synthetic */ long lambda$identity$2(long t) {
        return t;
    }
}
