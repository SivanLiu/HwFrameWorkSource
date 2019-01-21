package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BiPredicate$-ZiDuSsQaw4dQsCoX8HU1cLSeS8 implements BiPredicate {
    private final /* synthetic */ BiPredicate f$0;

    public /* synthetic */ -$$Lambda$BiPredicate$-ZiDuSsQaw4dQsCoX8HU1cLSeS8(BiPredicate biPredicate) {
        this.f$0 = biPredicate;
    }

    public final boolean test(Object obj, Object obj2) {
        return (this.f$0.test(obj, obj2) ^ 1);
    }
}
