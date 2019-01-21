package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongUnaryOperator$e52YMvir03pwSw7KvpRuqEbSDRg implements LongUnaryOperator {
    private final /* synthetic */ LongUnaryOperator f$0;
    private final /* synthetic */ LongUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$LongUnaryOperator$e52YMvir03pwSw7KvpRuqEbSDRg(LongUnaryOperator longUnaryOperator, LongUnaryOperator longUnaryOperator2) {
        this.f$0 = longUnaryOperator;
        this.f$1 = longUnaryOperator2;
    }

    public final long applyAsLong(long j) {
        return this.f$0.applyAsLong(this.f$1.applyAsLong(j));
    }
}
