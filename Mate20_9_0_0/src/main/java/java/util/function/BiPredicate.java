package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface BiPredicate<T, U> {
    boolean test(T t, U u);

    BiPredicate<T, U> and(BiPredicate<? super T, ? super U> other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$BiPredicate$uIXzGqdBtyDdYjd7p0mbJFqyjRE(this, other);
    }

    static /* synthetic */ boolean lambda$and$0(BiPredicate biPredicate, BiPredicate other, Object t, Object u) {
        return biPredicate.test(t, u) && other.test(t, u);
    }

    BiPredicate<T, U> negate() {
        return new -$$Lambda$BiPredicate$-ZiDuSsQaw4dQsCoX8HU1cLSeS8(this);
    }

    BiPredicate<T, U> or(BiPredicate<? super T, ? super U> other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$BiPredicate$OpXxJPTnCjvZwHcfaL3bBDqxCyQ(this, other);
    }

    static /* synthetic */ boolean lambda$or$2(BiPredicate biPredicate, BiPredicate other, Object t, Object u) {
        return biPredicate.test(t, u) || other.test(t, u);
    }
}
