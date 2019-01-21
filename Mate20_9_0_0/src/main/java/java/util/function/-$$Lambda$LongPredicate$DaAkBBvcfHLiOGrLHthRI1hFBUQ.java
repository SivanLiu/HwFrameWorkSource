package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongPredicate$DaAkBBvcfHLiOGrLHthRI1hFBUQ implements LongPredicate {
    private final /* synthetic */ LongPredicate f$0;
    private final /* synthetic */ LongPredicate f$1;

    public /* synthetic */ -$$Lambda$LongPredicate$DaAkBBvcfHLiOGrLHthRI1hFBUQ(LongPredicate longPredicate, LongPredicate longPredicate2) {
        this.f$0 = longPredicate;
        this.f$1 = longPredicate2;
    }

    public final boolean test(long j) {
        return LongPredicate.lambda$and$0(this.f$0, this.f$1, j);
    }
}
