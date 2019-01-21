package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongPredicate$-csV2YowuwbIk10M6jhbu2oq5to implements LongPredicate {
    private final /* synthetic */ LongPredicate f$0;
    private final /* synthetic */ LongPredicate f$1;

    public /* synthetic */ -$$Lambda$LongPredicate$-csV2YowuwbIk10M6jhbu2oq5to(LongPredicate longPredicate, LongPredicate longPredicate2) {
        this.f$0 = longPredicate;
        this.f$1 = longPredicate2;
    }

    public final boolean test(long j) {
        return LongPredicate.lambda$or$2(this.f$0, this.f$1, j);
    }
}
