package java.util.function;

import java.util.Objects;
import java.util.function.-$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE.AnonymousClass1;
import java.util.function.-$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE.AnonymousClass2;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);

    Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return new AnonymousClass2((byte) 0, this, other);
    }

    /* synthetic */ boolean lambda$-java_util_function_Predicate_2759(Predicate other, Object t) {
        return test(t) ? other.test(t) : false;
    }

    /* synthetic */ boolean lambda$-java_util_function_Predicate_3052(Object t) {
        return test(t) ^ 1;
    }

    Predicate<T> negate() {
        return new AnonymousClass1((byte) 1, this);
    }

    Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return new AnonymousClass2((byte) 1, this, other);
    }

    /* synthetic */ boolean lambda$-java_util_function_Predicate_3988(Predicate other, Object t) {
        return !test(t) ? other.test(t) : true;
    }

    static <T> Predicate<T> isEqual(Object targetRef) {
        if (targetRef == null) {
            return -$Lambda$1rDGbc8p7Mv-tQJZzJy5uAxVFbE.$INST$0;
        }
        return new AnonymousClass1((byte) 0, targetRef);
    }
}
