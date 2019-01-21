package android.graphics;

import android.graphics.ColorSpace.Rgb;
import java.util.function.DoubleUnaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ColorSpace$Rgb$8EkhO2jIf14tuA3BvrmYJMa7YXM implements DoubleUnaryOperator {
    private final /* synthetic */ Rgb f$0;

    public /* synthetic */ -$$Lambda$ColorSpace$Rgb$8EkhO2jIf14tuA3BvrmYJMa7YXM(Rgb rgb) {
        this.f$0 = rgb;
    }

    public final double applyAsDouble(double d) {
        return this.f$0.clamp(d);
    }
}
