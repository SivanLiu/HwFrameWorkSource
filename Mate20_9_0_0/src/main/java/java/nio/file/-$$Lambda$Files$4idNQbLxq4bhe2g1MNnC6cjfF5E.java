package java.nio.file;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Files$4idNQbLxq4bhe2g1MNnC6cjfF5E implements Predicate {
    private final /* synthetic */ BiPredicate f$0;

    public /* synthetic */ -$$Lambda$Files$4idNQbLxq4bhe2g1MNnC6cjfF5E(BiPredicate biPredicate) {
        this.f$0 = biPredicate;
    }

    public final boolean test(Object obj) {
        return this.f$0.test(((Event) obj).file(), ((Event) obj).attributes());
    }
}
