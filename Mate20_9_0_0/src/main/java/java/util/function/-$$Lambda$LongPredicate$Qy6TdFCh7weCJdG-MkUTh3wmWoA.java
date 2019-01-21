package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongPredicate$Qy6TdFCh7weCJdG-MkUTh3wmWoA implements LongPredicate {
    private final /* synthetic */ LongPredicate f$0;

    public /* synthetic */ -$$Lambda$LongPredicate$Qy6TdFCh7weCJdG-MkUTh3wmWoA(LongPredicate longPredicate) {
        this.f$0 = longPredicate;
    }

    public final boolean test(long j) {
        return (this.f$0.test(j) ^ 1);
    }
}
