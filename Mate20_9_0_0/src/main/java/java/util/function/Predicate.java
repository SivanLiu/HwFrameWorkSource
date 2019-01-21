package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);

    Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$Predicate$GyIVQ08CWbeMZxHDkkrN-5apRkc(this, other);
    }

    static /* synthetic */ boolean lambda$and$0(Predicate predicate, Predicate other, Object t) {
        return predicate.test(t) && other.test(t);
    }

    Predicate<T> negate() {
        return new -$$Lambda$Predicate$L51YwfosqnYQ8QKStSMYaDgSslA(this);
    }

    Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return new -$$Lambda$Predicate$17UUIF1CH_K9duk0ChtjSwOycuM(this, other);
    }

    static /* synthetic */ boolean lambda$or$2(Predicate predicate, Predicate other, Object t) {
        return predicate.test(t) || other.test(t);
    }

    static <T> Predicate<T> isEqual(Object targetRef) {
        if (targetRef == null) {
            return -$$Lambda$wLIh0GiBW9398cTP8uaTH8KoGwo.INSTANCE;
        }
        return new -$$Lambda$Predicate$SDsDck317M7uJ9htNLy7zOBr1L8(targetRef);
    }
}
