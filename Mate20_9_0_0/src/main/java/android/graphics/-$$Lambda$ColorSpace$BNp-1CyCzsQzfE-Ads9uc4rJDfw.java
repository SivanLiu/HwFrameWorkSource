package android.graphics;

import java.util.function.DoubleUnaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ColorSpace$BNp-1CyCzsQzfE-Ads9uc4rJDfw implements DoubleUnaryOperator {
    public static final /* synthetic */ -$$Lambda$ColorSpace$BNp-1CyCzsQzfE-Ads9uc4rJDfw INSTANCE = new -$$Lambda$ColorSpace$BNp-1CyCzsQzfE-Ads9uc4rJDfw();

    private /* synthetic */ -$$Lambda$ColorSpace$BNp-1CyCzsQzfE-Ads9uc4rJDfw() {
    }

    public final double applyAsDouble(double d) {
        return ColorSpace.absRcpResponse(d, 0.9478672985781991d, 0.05213270142180095d, 0.07739938080495357d, 0.04045d, 2.4d);
    }
}
