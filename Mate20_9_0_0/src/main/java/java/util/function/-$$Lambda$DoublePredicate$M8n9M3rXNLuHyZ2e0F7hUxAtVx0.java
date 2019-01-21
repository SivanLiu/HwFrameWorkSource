package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoublePredicate$M8n9M3rXNLuHyZ2e0F7hUxAtVx0 implements DoublePredicate {
    private final /* synthetic */ DoublePredicate f$0;
    private final /* synthetic */ DoublePredicate f$1;

    public /* synthetic */ -$$Lambda$DoublePredicate$M8n9M3rXNLuHyZ2e0F7hUxAtVx0(DoublePredicate doublePredicate, DoublePredicate doublePredicate2) {
        this.f$0 = doublePredicate;
        this.f$1 = doublePredicate2;
    }

    public final boolean test(double d) {
        return DoublePredicate.lambda$and$0(this.f$0, this.f$1, d);
    }
}
