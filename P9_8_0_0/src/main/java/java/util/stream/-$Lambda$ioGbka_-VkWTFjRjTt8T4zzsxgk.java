package java.util.stream;

import java.util.OptionalDouble;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk implements Predicate {
    private final /* synthetic */ boolean $m$0(Object arg0) {
        return ((OptionalDouble) arg0).isPresent();
    }

    public final boolean test(Object obj) {
        return $m$0(obj);
    }
}
