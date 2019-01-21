package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongUnaryOperator$MxQtG8PPTeFzxiwgpkR3tXC-HLU implements LongUnaryOperator {
    private final /* synthetic */ LongUnaryOperator f$0;
    private final /* synthetic */ LongUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$LongUnaryOperator$MxQtG8PPTeFzxiwgpkR3tXC-HLU(LongUnaryOperator longUnaryOperator, LongUnaryOperator longUnaryOperator2) {
        this.f$0 = longUnaryOperator;
        this.f$1 = longUnaryOperator2;
    }

    public final long applyAsLong(long j) {
        return this.f$1.applyAsLong(this.f$0.applyAsLong(j));
    }
}
