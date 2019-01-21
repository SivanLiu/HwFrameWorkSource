package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoublePredicate$9YmJG7lS-NUbb1veFxbs9aIWObk implements DoublePredicate {
    private final /* synthetic */ DoublePredicate f$0;
    private final /* synthetic */ DoublePredicate f$1;

    public /* synthetic */ -$$Lambda$DoublePredicate$9YmJG7lS-NUbb1veFxbs9aIWObk(DoublePredicate doublePredicate, DoublePredicate doublePredicate2) {
        this.f$0 = doublePredicate;
        this.f$1 = doublePredicate2;
    }

    public final boolean test(double d) {
        return DoublePredicate.lambda$or$2(this.f$0, this.f$1, d);
    }
}
