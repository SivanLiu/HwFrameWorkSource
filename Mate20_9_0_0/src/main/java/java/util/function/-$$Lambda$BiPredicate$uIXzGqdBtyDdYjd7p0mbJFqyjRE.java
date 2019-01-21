package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BiPredicate$uIXzGqdBtyDdYjd7p0mbJFqyjRE implements BiPredicate {
    private final /* synthetic */ BiPredicate f$0;
    private final /* synthetic */ BiPredicate f$1;

    public /* synthetic */ -$$Lambda$BiPredicate$uIXzGqdBtyDdYjd7p0mbJFqyjRE(BiPredicate biPredicate, BiPredicate biPredicate2) {
        this.f$0 = biPredicate;
        this.f$1 = biPredicate2;
    }

    public final boolean test(Object obj, Object obj2) {
        return BiPredicate.lambda$and$0(this.f$0, this.f$1, obj, obj2);
    }
}
