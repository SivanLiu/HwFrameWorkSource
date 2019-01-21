package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface DoublePredicate {
    boolean test(double d);

    DoublePredicate and(DoublePredicate other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$DoublePredicate$M8n9M3rXNLuHyZ2e0F7hUxAtVx0(this, other);
    }

    static /* synthetic */ boolean lambda$and$0(DoublePredicate doublePredicate, DoublePredicate other, double value) {
        return doublePredicate.test(value) && other.test(value);
    }

    DoublePredicate negate() {
        return new -$$Lambda$DoublePredicate$01E7YsTWsjaQSI72YV852C1Uqco(this);
    }

    DoublePredicate or(DoublePredicate other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$DoublePredicate$9YmJG7lS-NUbb1veFxbs9aIWObk(this, other);
    }

    static /* synthetic */ boolean lambda$or$2(DoublePredicate doublePredicate, DoublePredicate other, double value) {
        return doublePredicate.test(value) || other.test(value);
    }
}
