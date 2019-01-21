package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Predicate$GyIVQ08CWbeMZxHDkkrN-5apRkc implements Predicate {
    private final /* synthetic */ Predicate f$0;
    private final /* synthetic */ Predicate f$1;

    public /* synthetic */ -$$Lambda$Predicate$GyIVQ08CWbeMZxHDkkrN-5apRkc(Predicate predicate, Predicate predicate2) {
        this.f$0 = predicate;
        this.f$1 = predicate2;
    }

    public final boolean test(Object obj) {
        return Predicate.lambda$and$0(this.f$0, this.f$1, obj);
    }
}
