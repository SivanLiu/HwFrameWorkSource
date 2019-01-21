package android.graphics;

import android.graphics.ColorSpace.Rgb.TransferParameters;
import java.util.function.DoubleUnaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ColorSpace$Rgb$b9VGKuNnse0bbguR9jbOM_wK2Ac implements DoubleUnaryOperator {
    private final /* synthetic */ TransferParameters f$0;

    public /* synthetic */ -$$Lambda$ColorSpace$Rgb$b9VGKuNnse0bbguR9jbOM_wK2Ac(TransferParameters transferParameters) {
        this.f$0 = transferParameters;
    }

    public final double applyAsDouble(double d) {
        return ColorSpace.response(d, this.f$0.a, this.f$0.b, this.f$0.c, this.f$0.d, this.f$0.g);
    }
}
