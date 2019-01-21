package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoubleUnaryOperator$EzzlhUGRoL66wVBCG-_euZgC-CA implements DoubleUnaryOperator {
    private final /* synthetic */ DoubleUnaryOperator f$0;
    private final /* synthetic */ DoubleUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$DoubleUnaryOperator$EzzlhUGRoL66wVBCG-_euZgC-CA(DoubleUnaryOperator doubleUnaryOperator, DoubleUnaryOperator doubleUnaryOperator2) {
        this.f$0 = doubleUnaryOperator;
        this.f$1 = doubleUnaryOperator2;
    }

    public final double applyAsDouble(double d) {
        return this.f$1.applyAsDouble(this.f$0.applyAsDouble(d));
    }
}
