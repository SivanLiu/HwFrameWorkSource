package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoublePredicate$01E7YsTWsjaQSI72YV852C1Uqco implements DoublePredicate {
    private final /* synthetic */ DoublePredicate f$0;

    public /* synthetic */ -$$Lambda$DoublePredicate$01E7YsTWsjaQSI72YV852C1Uqco(DoublePredicate doublePredicate) {
        this.f$0 = doublePredicate;
    }

    public final boolean test(double d) {
        return (this.f$0.test(d) ^ 1);
    }
}
