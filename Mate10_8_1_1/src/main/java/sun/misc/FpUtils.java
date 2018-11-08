package sun.misc;

public class FpUtils {
    static final /* synthetic */ boolean -assertionsDisabled = (FpUtils.class.desiredAssertionStatus() ^ 1);

    private FpUtils() {
    }

    @Deprecated
    public static int getExponent(double d) {
        return Math.getExponent(d);
    }

    @Deprecated
    public static int getExponent(float f) {
        return Math.getExponent(f);
    }

    @Deprecated
    public static double rawCopySign(double magnitude, double sign) {
        return Math.copySign(magnitude, sign);
    }

    @Deprecated
    public static float rawCopySign(float magnitude, float sign) {
        return Math.copySign(magnitude, sign);
    }

    @Deprecated
    public static boolean isFinite(double d) {
        return Double.isFinite(d);
    }

    @Deprecated
    public static boolean isFinite(float f) {
        return Float.isFinite(f);
    }

    public static boolean isInfinite(double d) {
        return Double.isInfinite(d);
    }

    public static boolean isInfinite(float f) {
        return Float.isInfinite(f);
    }

    public static boolean isNaN(double d) {
        return Double.isNaN(d);
    }

    public static boolean isNaN(float f) {
        return Float.isNaN(f);
    }

    public static boolean isUnordered(double arg1, double arg2) {
        return !isNaN(arg1) ? isNaN(arg2) : true;
    }

    public static boolean isUnordered(float arg1, float arg2) {
        return !isNaN(arg1) ? isNaN(arg2) : true;
    }

    public static int ilogb(double d) {
        int exponent = getExponent(d);
        switch (exponent) {
            case -1023:
                if (d == 0.0d) {
                    return -268435456;
                }
                long transducer = Double.doubleToRawLongBits(d) & DoubleConsts.SIGNIF_BIT_MASK;
                if (-assertionsDisabled || transducer != 0) {
                    while (transducer < 4503599627370496L) {
                        transducer *= 2;
                        exponent--;
                    }
                    exponent++;
                    if (-assertionsDisabled || (exponent >= DoubleConsts.MIN_SUB_EXPONENT && exponent < -1022)) {
                        return exponent;
                    }
                    throw new AssertionError();
                }
                throw new AssertionError();
            case 1024:
                if (isNaN(d)) {
                    return 1073741824;
                }
                return 268435456;
            default:
                if (-assertionsDisabled || (exponent >= -1022 && exponent <= 1023)) {
                    return exponent;
                }
                throw new AssertionError();
        }
    }

    public static int ilogb(float f) {
        int exponent = getExponent(f);
        switch (exponent) {
            case -127:
                if (f == 0.0f) {
                    return -268435456;
                }
                int transducer = Float.floatToRawIntBits(f) & FloatConsts.SIGNIF_BIT_MASK;
                if (-assertionsDisabled || transducer != 0) {
                    while (transducer < 8388608) {
                        transducer *= 2;
                        exponent--;
                    }
                    exponent++;
                    if (-assertionsDisabled || (exponent >= FloatConsts.MIN_SUB_EXPONENT && exponent < -126)) {
                        return exponent;
                    }
                    throw new AssertionError();
                }
                throw new AssertionError();
            case 128:
                if (isNaN(f)) {
                    return 1073741824;
                }
                return 268435456;
            default:
                if (-assertionsDisabled || (exponent >= -126 && exponent <= 127)) {
                    return exponent;
                }
                throw new AssertionError();
        }
    }

    @Deprecated
    public static double scalb(double d, int scale_factor) {
        return Math.scalb(d, scale_factor);
    }

    @Deprecated
    public static float scalb(float f, int scale_factor) {
        return Math.scalb(f, scale_factor);
    }

    @Deprecated
    public static double nextAfter(double start, double direction) {
        return Math.nextAfter(start, direction);
    }

    @Deprecated
    public static float nextAfter(float start, double direction) {
        return Math.nextAfter(start, direction);
    }

    @Deprecated
    public static double nextUp(double d) {
        return Math.nextUp(d);
    }

    @Deprecated
    public static float nextUp(float f) {
        return Math.nextUp(f);
    }

    @Deprecated
    public static double nextDown(double d) {
        return Math.nextDown(d);
    }

    @Deprecated
    public static double nextDown(float f) {
        return (double) Math.nextDown(f);
    }

    @Deprecated
    public static double copySign(double magnitude, double sign) {
        return StrictMath.copySign(magnitude, sign);
    }

    @Deprecated
    public static float copySign(float magnitude, float sign) {
        return StrictMath.copySign(magnitude, sign);
    }

    @Deprecated
    public static double ulp(double d) {
        return Math.ulp(d);
    }

    @Deprecated
    public static float ulp(float f) {
        return Math.ulp(f);
    }

    @Deprecated
    public static double signum(double d) {
        return Math.signum(d);
    }

    @Deprecated
    public static float signum(float f) {
        return Math.signum(f);
    }
}
