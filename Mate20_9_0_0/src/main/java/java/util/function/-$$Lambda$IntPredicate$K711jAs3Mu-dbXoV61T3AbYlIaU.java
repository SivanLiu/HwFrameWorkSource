package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntPredicate$K711jAs3Mu-dbXoV61T3AbYlIaU implements IntPredicate {
    private final /* synthetic */ IntPredicate f$0;
    private final /* synthetic */ IntPredicate f$1;

    public /* synthetic */ -$$Lambda$IntPredicate$K711jAs3Mu-dbXoV61T3AbYlIaU(IntPredicate intPredicate, IntPredicate intPredicate2) {
        this.f$0 = intPredicate;
        this.f$1 = intPredicate2;
    }

    public final boolean test(int i) {
        return IntPredicate.lambda$or$2(this.f$0, this.f$1, i);
    }
}
