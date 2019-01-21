package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface DoubleUnaryOperator {
    double applyAsDouble(double d);

    DoubleUnaryOperator compose(DoubleUnaryOperator before) {
        Objects.requireNonNull(before);
        return new -$$Lambda$DoubleUnaryOperator$TV17Df571GWp0dWUym3y8OK6ZbM(this, before);
    }

    DoubleUnaryOperator andThen(DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        return new -$$Lambda$DoubleUnaryOperator$EzzlhUGRoL66wVBCG-_euZgC-CA(this, after);
    }

    static DoubleUnaryOperator identity() {
        return -$$Lambda$DoubleUnaryOperator$i7wtM_8Ous-CB32HCfZ4usZ4zaQ.INSTANCE;
    }

    static /* synthetic */ double lambda$identity$2(double t) {
        return t;
    }
}
