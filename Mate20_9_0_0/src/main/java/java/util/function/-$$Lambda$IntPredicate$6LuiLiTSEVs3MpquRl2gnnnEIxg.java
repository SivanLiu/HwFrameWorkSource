package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntPredicate$6LuiLiTSEVs3MpquRl2gnnnEIxg implements IntPredicate {
    private final /* synthetic */ IntPredicate f$0;

    public /* synthetic */ -$$Lambda$IntPredicate$6LuiLiTSEVs3MpquRl2gnnnEIxg(IntPredicate intPredicate) {
        this.f$0 = intPredicate;
    }

    public final boolean test(int i) {
        return (this.f$0.test(i) ^ 1);
    }
}
