package android.graphics;

import android.graphics.ColorSpace.Rgb;
import android.graphics.ColorSpace.Rgb.TransferParameters;
import java.util.function.DoubleUnaryOperator;

final /* synthetic */ class -$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8 implements DoubleUnaryOperator {
    public static final /* synthetic */ -$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8 $INST$0 = new -$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8((byte) 0);
    public static final /* synthetic */ -$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8 $INST$1 = new -$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: android.graphics.-$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8$1 */
    final /* synthetic */ class AnonymousClass1 implements DoubleUnaryOperator {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ double $m$0(double arg0) {
            return ColorSpace.rcpResponse(arg0, ((TransferParameters) this.-$f0).a, ((TransferParameters) this.-$f0).b, ((TransferParameters) this.-$f0).c, ((TransferParameters) this.-$f0).d, ((TransferParameters) this.-$f0).g);
        }

        private final /* synthetic */ double $m$1(double arg0) {
            return ColorSpace.rcpResponse(arg0, ((TransferParameters) this.-$f0).a, ((TransferParameters) this.-$f0).b, ((TransferParameters) this.-$f0).c, ((TransferParameters) this.-$f0).d, ((TransferParameters) this.-$f0).e, ((TransferParameters) this.-$f0).f, ((TransferParameters) this.-$f0).g);
        }

        private final /* synthetic */ double $m$2(double arg0) {
            return ColorSpace.response(arg0, ((TransferParameters) this.-$f0).a, ((TransferParameters) this.-$f0).b, ((TransferParameters) this.-$f0).c, ((TransferParameters) this.-$f0).d, ((TransferParameters) this.-$f0).g);
        }

        private final /* synthetic */ double $m$3(double arg0) {
            return ColorSpace.response(arg0, ((TransferParameters) this.-$f0).a, ((TransferParameters) this.-$f0).b, ((TransferParameters) this.-$f0).c, ((TransferParameters) this.-$f0).d, ((TransferParameters) this.-$f0).e, ((TransferParameters) this.-$f0).f, ((TransferParameters) this.-$f0).g);
        }

        private final /* synthetic */ double $m$4(double arg0) {
            return ((Rgb) this.-$f0).-android_graphics_ColorSpace$Rgb-mthref-0(arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final double applyAsDouble(double d) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(d);
                case (byte) 1:
                    return $m$1(d);
                case (byte) 2:
                    return $m$2(d);
                case (byte) 3:
                    return $m$3(d);
                case (byte) 4:
                    return $m$4(d);
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: android.graphics.-$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8$2 */
    final /* synthetic */ class AnonymousClass2 implements DoubleUnaryOperator {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ double -$f0;

        private final /* synthetic */ double $m$0(double arg0) {
            return Rgb.lambda$-android_graphics_ColorSpace$Rgb_113213(this.-$f0, arg0);
        }

        private final /* synthetic */ double $m$1(double arg0) {
            return Rgb.lambda$-android_graphics_ColorSpace$Rgb_113354(this.-$f0, arg0);
        }

        public /* synthetic */ AnonymousClass2(byte b, double d) {
            this.$id = b;
            this.-$f0 = d;
        }

        public final double applyAsDouble(double d) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(d);
                case (byte) 1:
                    return $m$1(d);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ double $m$0(double arg0) {
        return ColorSpace.absRcpResponse(arg0, 0.9478672985781991d, 0.05213270142180095d, 0.07739938080495357d, 0.04045d, 2.4d);
    }

    private final /* synthetic */ double $m$1(double arg0) {
        return ColorSpace.absResponse(arg0, 0.9478672985781991d, 0.05213270142180095d, 0.07739938080495357d, 0.04045d, 2.4d);
    }

    private /* synthetic */ -$Lambda$ZrP-zejiEXAqfwV-MyP5lE9mYC8(byte b) {
        this.$id = b;
    }

    public final double applyAsDouble(double d) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(d);
            case (byte) 1:
                return $m$1(d);
            default:
                throw new AssertionError();
        }
    }
}
