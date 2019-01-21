package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntUnaryOperator$HBwqtJWwVkNFC818pCzqJ5KBLm0 implements IntUnaryOperator {
    private final /* synthetic */ IntUnaryOperator f$0;
    private final /* synthetic */ IntUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$IntUnaryOperator$HBwqtJWwVkNFC818pCzqJ5KBLm0(IntUnaryOperator intUnaryOperator, IntUnaryOperator intUnaryOperator2) {
        this.f$0 = intUnaryOperator;
        this.f$1 = intUnaryOperator2;
    }

    public final int applyAsInt(int i) {
        return this.f$0.applyAsInt(this.f$1.applyAsInt(i));
    }
}
