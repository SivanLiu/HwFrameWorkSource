package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntUnaryOperator$F5l8VUrgGacAzg6VKzynJCfDBx4 implements IntUnaryOperator {
    private final /* synthetic */ IntUnaryOperator f$0;
    private final /* synthetic */ IntUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$IntUnaryOperator$F5l8VUrgGacAzg6VKzynJCfDBx4(IntUnaryOperator intUnaryOperator, IntUnaryOperator intUnaryOperator2) {
        this.f$0 = intUnaryOperator;
        this.f$1 = intUnaryOperator2;
    }

    public final int applyAsInt(int i) {
        return this.f$1.applyAsInt(this.f$0.applyAsInt(i));
    }
}
