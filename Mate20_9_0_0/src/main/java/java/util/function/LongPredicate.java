package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface LongPredicate {
    boolean test(long j);

    LongPredicate and(LongPredicate other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$LongPredicate$DaAkBBvcfHLiOGrLHthRI1hFBUQ(this, other);
    }

    static /* synthetic */ boolean lambda$and$0(LongPredicate longPredicate, LongPredicate other, long value) {
        return longPredicate.test(value) && other.test(value);
    }

    LongPredicate negate() {
        return new -$$Lambda$LongPredicate$Qy6TdFCh7weCJdG-MkUTh3wmWoA(this);
    }

    LongPredicate or(LongPredicate other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$LongPredicate$-csV2YowuwbIk10M6jhbu2oq5to(this, other);
    }

    static /* synthetic */ boolean lambda$or$2(LongPredicate longPredicate, LongPredicate other, long value) {
        return longPredicate.test(value) || other.test(value);
    }
}
