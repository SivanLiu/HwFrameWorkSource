package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoubleUnaryOperator$TV17Df571GWp0dWUym3y8OK6ZbM implements DoubleUnaryOperator {
    private final /* synthetic */ DoubleUnaryOperator f$0;
    private final /* synthetic */ DoubleUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$DoubleUnaryOperator$TV17Df571GWp0dWUym3y8OK6ZbM(DoubleUnaryOperator doubleUnaryOperator, DoubleUnaryOperator doubleUnaryOperator2) {
        this.f$0 = doubleUnaryOperator;
        this.f$1 = doubleUnaryOperator2;
    }

    public final double applyAsDouble(double d) {
        return this.f$0.applyAsDouble(this.f$1.applyAsDouble(d));
    }
}
