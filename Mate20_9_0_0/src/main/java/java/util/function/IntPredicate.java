package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface IntPredicate {
    boolean test(int i);

    IntPredicate and(IntPredicate other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$IntPredicate$Gjqjw1UkLLbkSrWX6rKKkHJDvzI(this, other);
    }

    static /* synthetic */ boolean lambda$and$0(IntPredicate intPredicate, IntPredicate other, int value) {
        return intPredicate.test(value) && other.test(value);
    }

    IntPredicate negate() {
        return new -$$Lambda$IntPredicate$6LuiLiTSEVs3MpquRl2gnnnEIxg(this);
    }

    IntPredicate or(IntPredicate other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$IntPredicate$K711jAs3Mu-dbXoV61T3AbYlIaU(this, other);
    }

    static /* synthetic */ boolean lambda$or$2(IntPredicate intPredicate, IntPredicate other, int value) {
        return intPredicate.test(value) || other.test(value);
    }
}
