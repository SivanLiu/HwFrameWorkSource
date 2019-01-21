package sun.misc;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public class FloatingDecimal {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final ASCIIToBinaryConverter A2BC_NEGATIVE_INFINITY = new PreparedASCIIToBinaryBuffer(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    static final ASCIIToBinaryConverter A2BC_NEGATIVE_ZERO = new PreparedASCIIToBinaryBuffer(-0.0d, -0.0f);
    static final ASCIIToBinaryConverter A2BC_NOT_A_NUMBER = new PreparedASCIIToBinaryBuffer(Double.NaN, Float.NaN);
    static final ASCIIToBinaryConverter A2BC_POSITIVE_INFINITY = new PreparedASCIIToBinaryBuffer(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    static final ASCIIToBinaryConverter A2BC_POSITIVE_ZERO = new PreparedASCIIToBinaryBuffer(0.0d, 0.0f);
    private static final BinaryToASCIIConverter B2AC_NEGATIVE_INFINITY = new ExceptionalBinaryToASCIIBuffer("-Infinity", true);
    private static final BinaryToASCIIConverter B2AC_NEGATIVE_ZERO = new BinaryToASCIIBuffer(true, new char[]{'0'});
    private static final BinaryToASCIIConverter B2AC_NOT_A_NUMBER = new ExceptionalBinaryToASCIIBuffer(NAN_REP, $assertionsDisabled);
    private static final BinaryToASCIIConverter B2AC_POSITIVE_INFINITY = new ExceptionalBinaryToASCIIBuffer(INFINITY_REP, $assertionsDisabled);
    private static final BinaryToASCIIConverter B2AC_POSITIVE_ZERO = new BinaryToASCIIBuffer($assertionsDisabled, new char[]{'0'});
    static final int BIG_DECIMAL_EXPONENT = 324;
    static final long EXP_ONE = 4607182418800017408L;
    static final int EXP_SHIFT = 52;
    static final long FRACT_HOB = 4503599627370496L;
    private static final int INFINITY_LENGTH = INFINITY_REP.length();
    private static final String INFINITY_REP = "Infinity";
    static final int INT_DECIMAL_DIGITS = 9;
    static final int MAX_DECIMAL_DIGITS = 15;
    static final int MAX_DECIMAL_EXPONENT = 308;
    static final int MAX_NDIGITS = 1100;
    static final int MAX_SMALL_BIN_EXP = 62;
    static final int MIN_DECIMAL_EXPONENT = -324;
    static final int MIN_SMALL_BIN_EXP = -21;
    private static final int NAN_LENGTH = NAN_REP.length();
    private static final String NAN_REP = "NaN";
    static final int SINGLE_EXP_SHIFT = 23;
    static final int SINGLE_FRACT_HOB = 8388608;
    static final int SINGLE_MAX_DECIMAL_DIGITS = 7;
    static final int SINGLE_MAX_DECIMAL_EXPONENT = 38;
    static final int SINGLE_MAX_NDIGITS = 200;
    static final int SINGLE_MIN_DECIMAL_EXPONENT = -45;
    private static final ThreadLocal<BinaryToASCIIBuffer> threadLocalBinaryToASCIIBuffer = new ThreadLocal<BinaryToASCIIBuffer>() {
        protected BinaryToASCIIBuffer initialValue() {
            return new BinaryToASCIIBuffer();
        }
    };

    interface ASCIIToBinaryConverter {
        double doubleValue();

        float floatValue();
    }

    public interface BinaryToASCIIConverter {
        void appendTo(Appendable appendable);

        boolean decimalDigitsExact();

        boolean digitsRoundedUp();

        int getDecimalExponent();

        int getDigits(char[] cArr);

        boolean isExceptional();

        boolean isNegative();

        String toJavaFormatString();
    }

    private static class HexFloatPattern {
        private static final Pattern VALUE = Pattern.compile("([-+])?0[xX](((\\p{XDigit}+)\\.?)|((\\p{XDigit}*)\\.(\\p{XDigit}+)))[pP]([-+])?(\\p{Digit}+)[fFdD]?");

        private HexFloatPattern() {
        }
    }

    static class ASCIIToBinaryBuffer implements ASCIIToBinaryConverter {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final double[] BIG_10_POW = new double[]{1.0E16d, 1.0E32d, 1.0E64d, 1.0E128d, 1.0E256d};
        private static final int MAX_SMALL_TEN = (SMALL_10_POW.length - 1);
        private static final int SINGLE_MAX_SMALL_TEN = (SINGLE_SMALL_10_POW.length - 1);
        private static final float[] SINGLE_SMALL_10_POW = new float[]{1.0f, 10.0f, 100.0f, 1000.0f, 10000.0f, 100000.0f, 1000000.0f, 1.0E7f, 1.0E8f, 1.0E9f, 1.0E10f};
        private static final double[] SMALL_10_POW = new double[]{1.0d, 10.0d, 100.0d, 1000.0d, 10000.0d, 100000.0d, 1000000.0d, 1.0E7d, 1.0E8d, 1.0E9d, 1.0E10d, 1.0E11d, 1.0E12d, 1.0E13d, 1.0E14d, 1.0E15d, 1.0E16d, 1.0E17d, 1.0E18d, 1.0E19d, 1.0E20d, 1.0E21d, 1.0E22d};
        private static final double[] TINY_10_POW = new double[]{1.0E-16d, 1.0E-32d, 1.0E-64d, 1.0E-128d, 1.0E-256d};
        int decExponent;
        char[] digits;
        boolean isNegative;
        int nDigits;

        static {
            Class cls = FloatingDecimal.class;
        }

        ASCIIToBinaryBuffer(boolean negSign, int decExponent, char[] digits, int n) {
            this.isNegative = negSign;
            this.decExponent = decExponent;
            this.digits = digits;
            this.nDigits = n;
        }

        /* JADX WARNING: Removed duplicated region for block: B:98:0x015d  */
        /* JADX WARNING: Removed duplicated region for block: B:103:0x01aa  */
        /* JADX WARNING: Removed duplicated region for block: B:102:0x01a6  */
        /* JADX WARNING: Removed duplicated region for block: B:107:0x01d1  */
        /* JADX WARNING: Removed duplicated region for block: B:106:0x01ce  */
        /* JADX WARNING: Removed duplicated region for block: B:111:0x01e2  */
        /* JADX WARNING: Removed duplicated region for block: B:110:0x01db  */
        /* JADX WARNING: Removed duplicated region for block: B:127:0x0238  */
        /* JADX WARNING: Removed duplicated region for block: B:118:0x0216  */
        /* JADX WARNING: Removed duplicated region for block: B:132:0x024c  */
        /* JADX WARNING: Removed duplicated region for block: B:159:0x0282 A:{SYNTHETIC, EDGE_INSN: B:159:0x0282->B:146:0x0282 ?: BREAK  , EDGE_INSN: B:159:0x0282->B:146:0x0282 ?: BREAK  } */
        /* JADX WARNING: Removed duplicated region for block: B:148:0x0286  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public double doubleValue() {
            double rValue;
            double dValue;
            double dValue2;
            FDBigInteger bigD0;
            long ieeeBits;
            int D5;
            FDBigInteger bigD;
            int iDigits;
            double dValue3;
            long bigBbits;
            int shift;
            int bigIntExp;
            int kDigits;
            int B2;
            int D2;
            int Ulp2;
            int exp;
            int hulpbias;
            int D52;
            long lValue;
            int common2;
            FDBigInteger bigB;
            int cmp;
            FDBigInteger bigD02;
            boolean Ulp22;
            boolean overvalue;
            int kDigits2 = Math.min(this.nDigits, 16);
            int prevD2 = 0;
            int iValue = this.digits[0] - 48;
            int iDigits2 = Math.min(kDigits2, 9);
            int iValue2 = iValue;
            for (iValue = 1; iValue < iDigits2; iValue++) {
                iValue2 = ((iValue2 * 10) + this.digits[iValue]) - 48;
            }
            long lValue2 = (long) iValue2;
            for (iValue = iDigits2; iValue < kDigits2; iValue++) {
                lValue2 = (10 * lValue2) + ((long) (this.digits[iValue] - 48));
            }
            iValue = (double) lValue2;
            int exp2 = this.decExponent - kDigits2;
            if (this.nDigits <= 15) {
                if (exp2 == 0 || iValue == 0) {
                    return this.isNegative ? -iValue : iValue;
                } else if (exp2 >= 0) {
                    if (exp2 <= MAX_SMALL_TEN) {
                        rValue = SMALL_10_POW[exp2] * iValue;
                        return this.isNegative ? -rValue : rValue;
                    }
                    int slop = 15 - kDigits2;
                    if (exp2 <= MAX_SMALL_TEN + slop) {
                        double dValue4 = iValue * SMALL_10_POW[slop];
                        rValue = SMALL_10_POW[exp2 - slop] * dValue4;
                        if (this.isNegative) {
                            dValue4 = -rValue;
                        } else {
                            dValue4 = rValue;
                        }
                        return dValue4;
                    }
                } else if (exp2 >= (-MAX_SMALL_TEN)) {
                    rValue = iValue / SMALL_10_POW[-exp2];
                    return this.isNegative ? -rValue : rValue;
                }
            }
            double d;
            int i;
            if (exp2 > 0) {
                d = Double.NEGATIVE_INFINITY;
                if (this.decExponent > 309) {
                    if (!this.isNegative) {
                        d = Double.POSITIVE_INFINITY;
                    }
                    return d;
                }
                if ((exp2 & 15) != 0) {
                    iValue *= SMALL_10_POW[exp2 & 15];
                }
                i = exp2 >> 4;
                exp2 = i;
                if (i != 0) {
                    rValue = iValue;
                    iValue = 0;
                    while (exp2 > 1) {
                        if ((exp2 & 1) != 0) {
                            rValue *= BIG_10_POW[iValue];
                        }
                        iValue++;
                        exp2 >>= 1;
                    }
                    dValue = rValue * BIG_10_POW[iValue];
                    if (Double.isInfinite(dValue)) {
                        if (Double.isInfinite((rValue / 2.0d) * BIG_10_POW[iValue])) {
                            if (!this.isNegative) {
                                d = Double.POSITIVE_INFINITY;
                            }
                            return d;
                        }
                        dValue = Double.MAX_VALUE;
                    }
                    if (this.nDigits > FloatingDecimal.MAX_NDIGITS) {
                        this.nDigits = 1101;
                        this.digits[FloatingDecimal.MAX_NDIGITS] = '1';
                    }
                    dValue2 = dValue;
                    bigD0 = new FDBigInteger(lValue2, this.digits, kDigits2, this.nDigits);
                    exp2 = this.decExponent - this.nDigits;
                    ieeeBits = Double.doubleToRawLongBits(dValue2);
                    iValue = Math.max(0, -exp2);
                    D5 = Math.max(0, exp2);
                    bigD0 = bigD0.multByPow52(D5, 0);
                    bigD0.makeImmutable();
                    bigD = null;
                    while (true) {
                        iDigits = iDigits2;
                        dValue3 = dValue2;
                        iDigits2 = (int) (ieeeBits >>> FloatingDecimal.EXP_SHIFT);
                        bigBbits = DoubleConsts.SIGNIF_BIT_MASK & ieeeBits;
                        if (iDigits2 <= 0) {
                            bigBbits |= FloatingDecimal.FRACT_HOB;
                        } else {
                            shift = Long.numberOfLeadingZeros(bigBbits) - 11;
                            bigBbits <<= shift;
                            iDigits2 = 1 - shift;
                        }
                        iDigits2 -= 1023;
                        shift = Long.numberOfTrailingZeros(bigBbits);
                        bigBbits >>>= shift;
                        bigIntExp = (iDigits2 - 52) + shift;
                        kDigits = kDigits2;
                        kDigits2 = 53 - shift;
                        B2 = iValue;
                        D2 = D5;
                        if (bigIntExp < 0) {
                            B2 += bigIntExp;
                        } else {
                            D2 -= bigIntExp;
                        }
                        Ulp2 = B2;
                        exp = exp2;
                        if (iDigits2 > -1023) {
                            hulpbias = (iDigits2 + shift) + 1023;
                        } else {
                            hulpbias = 1 + shift;
                        }
                        exp2 = hulpbias;
                        D52 = D5;
                        D5 = B2 + exp2;
                        iDigits2 = D2 + exp2;
                        lValue = lValue2;
                        exp2 = Ulp2;
                        common2 = Math.min(D5, Math.min(iDigits2, exp2));
                        D5 -= common2;
                        iDigits2 -= common2;
                        Ulp2 = exp2 - common2;
                        bigB = FDBigInteger.valueOfMulPow52(bigBbits, iValue, D5);
                        if (bigD == null || prevD2 != iDigits2) {
                            bigD = bigD0.leftShift(iDigits2);
                            prevD2 = iDigits2;
                        }
                        cmp = bigB.cmp(bigD);
                        B2 = cmp;
                        if (cmp > 0) {
                            bigD02 = bigD0;
                            int i2 = D5;
                            if (B2 >= 0) {
                                break;
                            }
                            Ulp22 = FloatingDecimal.$assertionsDisabled;
                            bigD0 = bigD.rightInplaceSub(bigB);
                        } else {
                            Ulp22 = true;
                            bigD02 = bigD0;
                            bigD0 = bigB.leftInplaceSub(bigD);
                            if (kDigits2 == 1) {
                                if (bigIntExp > -1022) {
                                    Ulp2--;
                                    if (Ulp2 < 0) {
                                        Ulp2 = 0;
                                        bigD0 = bigD0.leftShift(1);
                                    }
                                }
                            }
                        }
                        overvalue = Ulp22;
                        B2 = bigD0.cmpPow52(iValue, Ulp2);
                        if (B2 < 0) {
                            long j = 1;
                            if (B2 != 0) {
                                if (overvalue) {
                                    j = -1;
                                }
                                ieeeBits += j;
                                if (ieeeBits == 0 || ieeeBits == DoubleConsts.EXP_BIT_MASK) {
                                    break;
                                }
                                iDigits2 = iDigits;
                                dValue2 = dValue3;
                                kDigits2 = kDigits;
                                exp2 = exp;
                                D5 = D52;
                                lValue2 = lValue;
                                bigD0 = bigD02;
                            } else if ((ieeeBits & 1) != 0) {
                                if (overvalue) {
                                    j = -1;
                                }
                                ieeeBits += j;
                            }
                        } else {
                            break;
                        }
                    }
                    if (this.isNegative) {
                        ieeeBits |= Long.MIN_VALUE;
                    }
                    return Double.longBitsToDouble(ieeeBits);
                }
            } else if (exp2 < 0) {
                exp2 = -exp2;
                d = -0.0d;
                if (this.decExponent < -325) {
                    if (!this.isNegative) {
                        d = 0.0d;
                    }
                    return d;
                }
                if ((exp2 & 15) != 0) {
                    iValue /= SMALL_10_POW[exp2 & 15];
                }
                i = exp2 >> 4;
                exp2 = i;
                if (i != 0) {
                    double dValue5 = iValue;
                    iValue = 0;
                    while (exp2 > 1) {
                        if ((exp2 & 1) != 0) {
                            dValue5 *= TINY_10_POW[iValue];
                        }
                        iValue++;
                        exp2 >>= 1;
                    }
                    double t = TINY_10_POW[iValue] * dValue5;
                    if (t == 0.0d) {
                        if ((2.0d * dValue5) * TINY_10_POW[iValue] == 0.0d) {
                            if (!this.isNegative) {
                                d = 0.0d;
                            }
                            return d;
                        }
                        t = Double.MIN_VALUE;
                    }
                    iValue = t;
                }
            }
            dValue = iValue;
            if (this.nDigits > FloatingDecimal.MAX_NDIGITS) {
            }
            dValue2 = dValue;
            bigD0 = new FDBigInteger(lValue2, this.digits, kDigits2, this.nDigits);
            exp2 = this.decExponent - this.nDigits;
            ieeeBits = Double.doubleToRawLongBits(dValue2);
            iValue = Math.max(0, -exp2);
            D5 = Math.max(0, exp2);
            bigD0 = bigD0.multByPow52(D5, 0);
            bigD0.makeImmutable();
            bigD = null;
            while (true) {
                iDigits = iDigits2;
                dValue3 = dValue2;
                iDigits2 = (int) (ieeeBits >>> FloatingDecimal.EXP_SHIFT);
                bigBbits = DoubleConsts.SIGNIF_BIT_MASK & ieeeBits;
                if (iDigits2 <= 0) {
                }
                iDigits2 -= 1023;
                shift = Long.numberOfTrailingZeros(bigBbits);
                bigBbits >>>= shift;
                bigIntExp = (iDigits2 - 52) + shift;
                kDigits = kDigits2;
                kDigits2 = 53 - shift;
                B2 = iValue;
                D2 = D5;
                if (bigIntExp < 0) {
                }
                Ulp2 = B2;
                exp = exp2;
                if (iDigits2 > -1023) {
                }
                exp2 = hulpbias;
                D52 = D5;
                D5 = B2 + exp2;
                iDigits2 = D2 + exp2;
                lValue = lValue2;
                exp2 = Ulp2;
                common2 = Math.min(D5, Math.min(iDigits2, exp2));
                D5 -= common2;
                iDigits2 -= common2;
                Ulp2 = exp2 - common2;
                bigB = FDBigInteger.valueOfMulPow52(bigBbits, iValue, D5);
                bigD = bigD0.leftShift(iDigits2);
                prevD2 = iDigits2;
                cmp = bigB.cmp(bigD);
                B2 = cmp;
                if (cmp > 0) {
                }
                overvalue = Ulp22;
                B2 = bigD0.cmpPow52(iValue, Ulp2);
                if (B2 < 0) {
                }
                iDigits2 = iDigits;
                dValue2 = dValue3;
                kDigits2 = kDigits;
                exp2 = exp;
                D5 = D52;
                lValue2 = lValue;
                bigD0 = bigD02;
            }
            if (this.isNegative) {
            }
            return Double.longBitsToDouble(ieeeBits);
        }

        /* JADX WARNING: Removed duplicated region for block: B:90:0x0135  */
        /* JADX WARNING: Removed duplicated region for block: B:95:0x0173  */
        /* JADX WARNING: Removed duplicated region for block: B:94:0x016e  */
        /* JADX WARNING: Removed duplicated region for block: B:99:0x0198  */
        /* JADX WARNING: Removed duplicated region for block: B:98:0x0195  */
        /* JADX WARNING: Removed duplicated region for block: B:103:0x01a7  */
        /* JADX WARNING: Removed duplicated region for block: B:102:0x01a2  */
        /* JADX WARNING: Removed duplicated region for block: B:119:0x01ff  */
        /* JADX WARNING: Removed duplicated region for block: B:110:0x01df  */
        /* JADX WARNING: Removed duplicated region for block: B:124:0x0211  */
        /* JADX WARNING: Removed duplicated region for block: B:152:0x0240 A:{SYNTHETIC, EDGE_INSN: B:152:0x0240->B:138:0x0240 ?: BREAK  , EDGE_INSN: B:152:0x0240->B:138:0x0240 ?: BREAK  } */
        /* JADX WARNING: Removed duplicated region for block: B:140:0x0244  */
        /* JADX WARNING: Removed duplicated region for block: B:90:0x0135  */
        /* JADX WARNING: Removed duplicated region for block: B:94:0x016e  */
        /* JADX WARNING: Removed duplicated region for block: B:95:0x0173  */
        /* JADX WARNING: Removed duplicated region for block: B:98:0x0195  */
        /* JADX WARNING: Removed duplicated region for block: B:99:0x0198  */
        /* JADX WARNING: Removed duplicated region for block: B:102:0x01a2  */
        /* JADX WARNING: Removed duplicated region for block: B:103:0x01a7  */
        /* JADX WARNING: Removed duplicated region for block: B:110:0x01df  */
        /* JADX WARNING: Removed duplicated region for block: B:119:0x01ff  */
        /* JADX WARNING: Removed duplicated region for block: B:152:0x0240 A:{SYNTHETIC, EDGE_INSN: B:152:0x0240->B:138:0x0240 ?: BREAK  , EDGE_INSN: B:152:0x0240->B:138:0x0240 ?: BREAK  , EDGE_INSN: B:152:0x0240->B:138:0x0240 ?: BREAK  } */
        /* JADX WARNING: Removed duplicated region for block: B:124:0x0211  */
        /* JADX WARNING: Removed duplicated region for block: B:140:0x0244  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public float floatValue() {
            int i;
            int slop;
            int i2;
            int i3;
            double dValue;
            FDBigInteger bigD0;
            int ieeeBits;
            int D5;
            FDBigInteger bigD;
            int bigBbits;
            int lowOrderZeros;
            int bigIntExp;
            int kDigits;
            int B2;
            int D2;
            int Ulp2;
            float fValue;
            int exp;
            int D52;
            int iValue;
            double dValue2;
            FDBigInteger bigB;
            int cmpResult;
            boolean overvalue;
            boolean Ulp22;
            FDBigInteger diff;
            FDBigInteger bigD02;
            int kDigits2 = Math.min(this.nDigits, 8);
            int prevD2 = 0;
            int iValue2 = this.digits[0] - 48;
            for (i = 1; i < kDigits2; i++) {
                iValue2 = ((iValue2 * 10) + this.digits[i]) - 48;
            }
            float fValue2 = (float) iValue2;
            int exp2 = this.decExponent - kDigits2;
            float f = 0.0f;
            if (this.nDigits <= 7) {
                if (exp2 == 0 || fValue2 == 0.0f) {
                    return this.isNegative ? -fValue2 : fValue2;
                } else if (exp2 >= 0) {
                    if (exp2 <= SINGLE_MAX_SMALL_TEN) {
                        fValue2 *= SINGLE_SMALL_10_POW[exp2];
                        return this.isNegative ? -fValue2 : fValue2;
                    }
                    slop = 7 - kDigits2;
                    if (exp2 <= SINGLE_MAX_SMALL_TEN + slop) {
                        fValue2 = (fValue2 * SINGLE_SMALL_10_POW[slop]) * SINGLE_SMALL_10_POW[exp2 - slop];
                        return this.isNegative ? -fValue2 : fValue2;
                    }
                } else if (exp2 >= (-SINGLE_MAX_SMALL_TEN)) {
                    fValue2 /= SINGLE_SMALL_10_POW[-exp2];
                    return this.isNegative ? -fValue2 : fValue2;
                }
            } else if (this.decExponent >= this.nDigits && this.nDigits + this.decExponent <= 15) {
                long lValue = (long) iValue2;
                for (i2 = kDigits2; i2 < this.nDigits; i2++) {
                    lValue = (10 * lValue) + ((long) (this.digits[i2] - 48));
                }
                i = (float) (((double) lValue) * SMALL_10_POW[this.decExponent - this.nDigits]);
                return this.isNegative != 0 ? -i : i;
            }
            double dValue3 = (double) fValue2;
            if (exp2 <= 0) {
                if (exp2 < 0) {
                    exp2 = -exp2;
                    if (this.decExponent < -46) {
                        if (this.isNegative) {
                            f = -0.0f;
                        }
                        return f;
                    }
                    if ((exp2 & 15) != 0) {
                        dValue3 /= SMALL_10_POW[exp2 & 15];
                    }
                    i2 = exp2 >> 4;
                    exp2 = i2;
                    if (i2 != 0) {
                        i2 = exp2;
                        exp2 = 0;
                        while (i2 > 0) {
                            if ((i2 & 1) != 0) {
                                dValue3 *= TINY_10_POW[exp2];
                            }
                            exp2++;
                            i2 >>= 1;
                        }
                    }
                }
                i3 = exp2;
                dValue = dValue3;
                fValue2 = Math.max(Float.MIN_VALUE, Math.min(Float.MAX_VALUE, (float) dValue));
                if (this.nDigits > 200) {
                }
                bigD0 = new FDBigInteger((long) iValue2, this.digits, kDigits2, this.nDigits);
                i2 = this.decExponent - this.nDigits;
                ieeeBits = Float.floatToRawIntBits(fValue2);
                slop = Math.max(0, -i2);
                D5 = Math.max(0, i2);
                bigD0 = bigD0.multByPow52(D5, 0);
                bigD0.makeImmutable();
                bigD = null;
                while (true) {
                    i3 = ieeeBits >>> FloatingDecimal.SINGLE_EXP_SHIFT;
                    bigBbits = FloatConsts.SIGNIF_BIT_MASK & ieeeBits;
                    if (i3 > 0) {
                    }
                    i3 -= 127;
                    lowOrderZeros = Integer.numberOfTrailingZeros(bigBbits);
                    bigBbits >>>= lowOrderZeros;
                    bigIntExp = (i3 - 23) + lowOrderZeros;
                    kDigits = kDigits2;
                    kDigits2 = 24 - lowOrderZeros;
                    B2 = slop;
                    D2 = D5;
                    if (bigIntExp >= 0) {
                    }
                    Ulp2 = B2;
                    fValue = fValue2;
                    if (i3 <= -127) {
                    }
                    exp = i2;
                    i2 = B2 + i;
                    D52 = D5;
                    D5 = D2 + i;
                    iValue = iValue2;
                    i = Ulp2;
                    iValue2 = Math.min(i2, Math.min(D5, i));
                    D5 -= iValue2;
                    Ulp2 = i - iValue2;
                    dValue2 = dValue;
                    bigB = FDBigInteger.valueOfMulPow52((long) bigBbits, slop, i2 - iValue2);
                    bigD = bigD0.leftShift(D5);
                    prevD2 = D5;
                    iValue2 = bigB.cmp(bigD);
                    cmpResult = iValue2;
                    if (iValue2 <= 0) {
                    }
                    overvalue = Ulp22;
                    cmpResult = diff.cmpPow52(slop, Ulp2);
                    if (cmpResult >= 0) {
                    }
                    kDigits2 = kDigits;
                    fValue2 = fValue;
                    i2 = exp;
                    D5 = D52;
                    iValue2 = iValue;
                    dValue = dValue2;
                    bigD0 = bigD02;
                }
                if (this.isNegative) {
                }
                return Float.intBitsToFloat(ieeeBits);
            } else if (this.decExponent > 39) {
                return this.isNegative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
            } else {
                if ((exp2 & 15) != 0) {
                    dValue3 *= SMALL_10_POW[exp2 & 15];
                }
                i2 = exp2 >> 4;
                exp2 = i2;
                if (i2 != 0) {
                    i2 = exp2;
                    exp2 = 0;
                    while (i2 > 0) {
                        if ((i2 & 1) != 0) {
                            dValue3 *= BIG_10_POW[exp2];
                        }
                        exp2++;
                        i2 >>= 1;
                    }
                }
                i3 = exp2;
                dValue = dValue3;
                fValue2 = Math.max(Float.MIN_VALUE, Math.min(Float.MAX_VALUE, (float) dValue));
                if (this.nDigits > 200) {
                    this.nDigits = HttpURLConnection.HTTP_CREATED;
                    this.digits[200] = '1';
                }
                bigD0 = new FDBigInteger((long) iValue2, this.digits, kDigits2, this.nDigits);
                i2 = this.decExponent - this.nDigits;
                ieeeBits = Float.floatToRawIntBits(fValue2);
                slop = Math.max(0, -i2);
                D5 = Math.max(0, i2);
                bigD0 = bigD0.multByPow52(D5, 0);
                bigD0.makeImmutable();
                bigD = null;
                while (true) {
                    i3 = ieeeBits >>> FloatingDecimal.SINGLE_EXP_SHIFT;
                    bigBbits = FloatConsts.SIGNIF_BIT_MASK & ieeeBits;
                    if (i3 > 0) {
                        bigBbits |= FloatingDecimal.SINGLE_FRACT_HOB;
                    } else {
                        B2 = Integer.numberOfLeadingZeros(bigBbits) - 8;
                        bigBbits <<= B2;
                        i3 = 1 - B2;
                    }
                    i3 -= 127;
                    lowOrderZeros = Integer.numberOfTrailingZeros(bigBbits);
                    bigBbits >>>= lowOrderZeros;
                    bigIntExp = (i3 - 23) + lowOrderZeros;
                    kDigits = kDigits2;
                    kDigits2 = 24 - lowOrderZeros;
                    B2 = slop;
                    D2 = D5;
                    if (bigIntExp >= 0) {
                        B2 += bigIntExp;
                    } else {
                        D2 -= bigIntExp;
                    }
                    Ulp2 = B2;
                    fValue = fValue2;
                    if (i3 <= -127) {
                        i = (i3 + lowOrderZeros) + 127;
                    } else {
                        i = 1 + lowOrderZeros;
                    }
                    exp = i2;
                    i2 = B2 + i;
                    D52 = D5;
                    D5 = D2 + i;
                    iValue = iValue2;
                    i = Ulp2;
                    iValue2 = Math.min(i2, Math.min(D5, i));
                    D5 -= iValue2;
                    Ulp2 = i - iValue2;
                    dValue2 = dValue;
                    bigB = FDBigInteger.valueOfMulPow52((long) bigBbits, slop, i2 - iValue2);
                    if (bigD == null || prevD2 != D5) {
                        bigD = bigD0.leftShift(D5);
                        prevD2 = D5;
                    }
                    iValue2 = bigB.cmp(bigD);
                    cmpResult = iValue2;
                    if (iValue2 <= 0) {
                        bigD02 = bigD0;
                        exp2 = 1;
                        if (cmpResult >= 0) {
                            break;
                        }
                        Ulp22 = FloatingDecimal.$assertionsDisabled;
                        diff = bigD.rightInplaceSub(bigB);
                    } else {
                        Ulp22 = true;
                        diff = bigB.leftInplaceSub(bigD);
                        bigD02 = bigD0;
                        exp2 = 1;
                        if (kDigits2 == 1) {
                            if (bigIntExp > -126) {
                                Ulp2--;
                                if (Ulp2 < 0) {
                                    Ulp2 = 0;
                                    exp2 = 1;
                                    diff = diff.leftShift(1);
                                }
                            }
                            exp2 = 1;
                        }
                    }
                    overvalue = Ulp22;
                    cmpResult = diff.cmpPow52(slop, Ulp2);
                    if (cmpResult >= 0) {
                        int i4 = -1;
                        if (cmpResult != 0) {
                            if (!overvalue) {
                                i4 = exp2;
                            }
                            ieeeBits += i4;
                            if (ieeeBits == 0 || ieeeBits == FloatConsts.EXP_BIT_MASK) {
                                break;
                            }
                            kDigits2 = kDigits;
                            fValue2 = fValue;
                            i2 = exp;
                            D5 = D52;
                            iValue2 = iValue;
                            dValue = dValue2;
                            bigD0 = bigD02;
                        } else if ((ieeeBits & 1) != 0) {
                            if (!overvalue) {
                                i4 = exp2;
                            }
                            ieeeBits += i4;
                        }
                    } else {
                        break;
                    }
                }
                if (this.isNegative) {
                    ieeeBits |= Integer.MIN_VALUE;
                }
                return Float.intBitsToFloat(ieeeBits);
            }
            dValue = dValue3;
            fValue2 = Math.max(Float.MIN_VALUE, Math.min(Float.MAX_VALUE, (float) dValue));
            if (this.nDigits > 200) {
            }
            bigD0 = new FDBigInteger((long) iValue2, this.digits, kDigits2, this.nDigits);
            i2 = this.decExponent - this.nDigits;
            ieeeBits = Float.floatToRawIntBits(fValue2);
            slop = Math.max(0, -i2);
            D5 = Math.max(0, i2);
            bigD0 = bigD0.multByPow52(D5, 0);
            bigD0.makeImmutable();
            bigD = null;
            while (true) {
                i3 = ieeeBits >>> FloatingDecimal.SINGLE_EXP_SHIFT;
                bigBbits = FloatConsts.SIGNIF_BIT_MASK & ieeeBits;
                if (i3 > 0) {
                }
                i3 -= 127;
                lowOrderZeros = Integer.numberOfTrailingZeros(bigBbits);
                bigBbits >>>= lowOrderZeros;
                bigIntExp = (i3 - 23) + lowOrderZeros;
                kDigits = kDigits2;
                kDigits2 = 24 - lowOrderZeros;
                B2 = slop;
                D2 = D5;
                if (bigIntExp >= 0) {
                }
                Ulp2 = B2;
                fValue = fValue2;
                if (i3 <= -127) {
                }
                exp = i2;
                i2 = B2 + i;
                D52 = D5;
                D5 = D2 + i;
                iValue = iValue2;
                i = Ulp2;
                iValue2 = Math.min(i2, Math.min(D5, i));
                D5 -= iValue2;
                Ulp2 = i - iValue2;
                dValue2 = dValue;
                bigB = FDBigInteger.valueOfMulPow52((long) bigBbits, slop, i2 - iValue2);
                bigD = bigD0.leftShift(D5);
                prevD2 = D5;
                iValue2 = bigB.cmp(bigD);
                cmpResult = iValue2;
                if (iValue2 <= 0) {
                }
                overvalue = Ulp22;
                cmpResult = diff.cmpPow52(slop, Ulp2);
                if (cmpResult >= 0) {
                }
                kDigits2 = kDigits;
                fValue2 = fValue;
                i2 = exp;
                D5 = D52;
                iValue2 = iValue;
                dValue = dValue2;
                bigD0 = bigD02;
            }
            if (this.isNegative) {
            }
            return Float.intBitsToFloat(ieeeBits);
        }
    }

    static class BinaryToASCIIBuffer implements BinaryToASCIIConverter {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final int[] N_5_BITS = new int[]{0, 3, 5, 7, 10, 12, 14, 17, 19, 21, 24, 26, 28, 31, 33, 35, 38, 40, 42, 45, 47, 49, FloatingDecimal.EXP_SHIFT, 54, 56, 59, 61};
        private static int[] insignificantDigitsNumber = new int[]{0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 18, 19};
        private final char[] buffer;
        private int decExponent;
        private boolean decimalDigitsRoundedUp;
        private final char[] digits;
        private boolean exactDecimalConversion;
        private int firstDigitIndex;
        private boolean isNegative;
        private int nDigits;

        static {
            Class cls = FloatingDecimal.class;
        }

        BinaryToASCIIBuffer() {
            this.buffer = new char[26];
            this.exactDecimalConversion = FloatingDecimal.$assertionsDisabled;
            this.decimalDigitsRoundedUp = FloatingDecimal.$assertionsDisabled;
            this.digits = new char[20];
        }

        BinaryToASCIIBuffer(boolean isNegative, char[] digits) {
            this.buffer = new char[26];
            this.exactDecimalConversion = FloatingDecimal.$assertionsDisabled;
            this.decimalDigitsRoundedUp = FloatingDecimal.$assertionsDisabled;
            this.isNegative = isNegative;
            this.decExponent = 0;
            this.digits = digits;
            this.firstDigitIndex = 0;
            this.nDigits = digits.length;
        }

        public String toJavaFormatString() {
            return new String(this.buffer, 0, getChars(this.buffer));
        }

        public void appendTo(Appendable buf) {
            int len = getChars(this.buffer);
            if (buf instanceof StringBuilder) {
                ((StringBuilder) buf).append(this.buffer, 0, len);
            } else if (buf instanceof StringBuffer) {
                ((StringBuffer) buf).append(this.buffer, 0, len);
            }
        }

        public int getDecimalExponent() {
            return this.decExponent;
        }

        public int getDigits(char[] digits) {
            System.arraycopy(this.digits, this.firstDigitIndex, (Object) digits, 0, this.nDigits);
            return this.nDigits;
        }

        public boolean isNegative() {
            return this.isNegative;
        }

        public boolean isExceptional() {
            return FloatingDecimal.$assertionsDisabled;
        }

        public boolean digitsRoundedUp() {
            return this.decimalDigitsRoundedUp;
        }

        public boolean decimalDigitsExact() {
            return this.exactDecimalConversion;
        }

        private void setSign(boolean isNegative) {
            this.isNegative = isNegative;
        }

        private void developLongDigits(int decExponent, long lvalue, int insignificantDigits) {
            if (insignificantDigits != 0) {
                long pow10 = FDBigInteger.LONG_5_POW[insignificantDigits] << insignificantDigits;
                long residue = lvalue % pow10;
                lvalue /= pow10;
                decExponent += insignificantDigits;
                if (residue >= (pow10 >> 1)) {
                    lvalue++;
                }
            }
            int digitno = this.digits.length - 1;
            int ivalue;
            if (lvalue <= 2147483647L) {
                ivalue = (int) lvalue;
                int c = ivalue % 10;
                ivalue /= 10;
                while (c == 0) {
                    decExponent++;
                    c = ivalue % 10;
                    ivalue /= 10;
                }
                while (ivalue != 0) {
                    int digitno2 = digitno - 1;
                    this.digits[digitno] = (char) (c + 48);
                    decExponent++;
                    c = ivalue % 10;
                    ivalue /= 10;
                    digitno = digitno2;
                }
                this.digits[digitno] = (char) (c + 48);
                ivalue = c;
            } else {
                ivalue = (int) (lvalue % 10);
                lvalue /= 10;
                while (ivalue == 0) {
                    decExponent++;
                    ivalue = (int) (lvalue % 10);
                    lvalue /= 10;
                }
                while (lvalue != 0) {
                    int digitno3 = digitno - 1;
                    this.digits[digitno] = (char) (ivalue + 48);
                    decExponent++;
                    ivalue = (int) (lvalue % 10);
                    lvalue /= 10;
                    digitno = digitno3;
                }
                this.digits[digitno] = (char) (ivalue + 48);
            }
            this.decExponent = decExponent + 1;
            this.firstDigitIndex = digitno;
            this.nDigits = this.digits.length - digitno;
        }

        /* JADX WARNING: Removed duplicated region for block: B:154:0x0306  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void dtoa(int binExp, long fractBits, int nSignificantBits, boolean isCompatibleFormat) {
            int i = binExp;
            long fractBits2 = fractBits;
            int i2 = nSignificantBits;
            int tailZeros = Long.numberOfTrailingZeros(fractBits);
            int nFractBits = 53 - tailZeros;
            this.decimalDigitsRoundedUp = FloatingDecimal.$assertionsDisabled;
            this.exactDecimalConversion = FloatingDecimal.$assertionsDisabled;
            int nTinyBits = Math.max(0, (nFractBits - i) - 1);
            int Bbits;
            if (i > FloatingDecimal.MAX_SMALL_BIN_EXP || i < FloatingDecimal.MIN_SMALL_BIN_EXP || nTinyBits >= FDBigInteger.LONG_5_POW.length || N_5_BITS[nTinyBits] + nFractBits >= 64 || nTinyBits != 0) {
                int ndigit;
                boolean high;
                int decExp = estimateDecExp(fractBits2, i);
                int B5 = Math.max(0, -decExp);
                int B2 = (B5 + nTinyBits) + i;
                int S5 = Math.max(0, decExp);
                int S2 = S5 + nTinyBits;
                int M5 = B5;
                int M2 = B2 - i2;
                fractBits2 >>>= tailZeros;
                B2 -= nFractBits - 1;
                int common2factor = Math.min(B2, S2);
                B2 -= common2factor;
                S2 -= common2factor;
                M2 -= common2factor;
                if (nFractBits == 1) {
                    M2--;
                }
                if (M2 < 0) {
                    B2 -= M2;
                    S2 -= M2;
                    M2 = 0;
                }
                Bbits = (nFractBits + B2) + (B5 < N_5_BITS.length ? N_5_BITS[B5] : B5 * 3);
                i = (S2 + 1) + (S5 + 1 < N_5_BITS.length ? N_5_BITS[S5 + 1] : (S5 + 1) * 3);
                int i3;
                int i4;
                int i5;
                int i6;
                int q;
                long j;
                boolean low;
                int i7;
                boolean low2;
                boolean nFractBits2;
                if (Bbits >= 64 || i >= 64) {
                    i3 = tailZeros;
                    i4 = nFractBits;
                    i5 = nTinyBits;
                    i6 = Bbits;
                    FDBigInteger Sval = FDBigInteger.valueOfPow52(S5, S2);
                    i2 = Sval.getNormalizationBias();
                    Sval = Sval.leftShift(i2);
                    FDBigInteger Bval = FDBigInteger.valueOfMulPow52(fractBits2, B5, B2 + i2);
                    FDBigInteger Mval = FDBigInteger.valueOfPow52(M5 + 1, (M2 + i2) + 1);
                    FDBigInteger tenSval = FDBigInteger.valueOfPow52(S5 + 1, (S2 + i2) + 1);
                    Bbits = 0;
                    q = Bval.quoRemIteration(Sval);
                    boolean low3 = Bval.cmp(Mval) < 0 ? true : FloatingDecimal.$assertionsDisabled;
                    boolean high2 = tenSval.addAndCmp(Bval, Mval) <= 0 ? true : FloatingDecimal.$assertionsDisabled;
                    if (q != 0 || high2) {
                        int ndigit2 = 0 + 1;
                        this.digits[0] = (char) (48 + q);
                        Bbits = ndigit2;
                    } else {
                        decExp--;
                        j = fractBits2;
                    }
                    if (!isCompatibleFormat || decExp < -3 || decExp >= 8) {
                        low = FloatingDecimal.$assertionsDisabled;
                        ndigit = Bbits;
                        high = FloatingDecimal.$assertionsDisabled;
                    } else {
                        ndigit = Bbits;
                        i7 = q;
                        low = low3;
                        high = high2;
                    }
                    while (!low && !high) {
                        i7 = Bval.quoRemIteration(Sval);
                        Mval = Mval.multBy10();
                        low = Bval.cmp(Mval) < 0 ? true : FloatingDecimal.$assertionsDisabled;
                        high = tenSval.addAndCmp(Bval, Mval) <= 0 ? true : FloatingDecimal.$assertionsDisabled;
                        int ndigit3 = ndigit + 1;
                        this.digits[ndigit] = (char) (48 + i7);
                        ndigit = ndigit3;
                    }
                    if (high && low) {
                        Bval = Bval.leftShift(1);
                        fractBits2 = (long) Bval.cmp(tenSval);
                    } else {
                        fractBits2 = 0;
                    }
                    this.exactDecimalConversion = Bval.cmp(FDBigInteger.ZERO) == 0 ? true : FloatingDecimal.$assertionsDisabled;
                } else if (Bbits >= 32 || i >= 32) {
                    boolean low4;
                    int decExp2;
                    int ndigit4;
                    i3 = tailZeros;
                    i4 = nFractBits;
                    long b = (FDBigInteger.LONG_5_POW[B5] * fractBits2) << B2;
                    long s = FDBigInteger.LONG_5_POW[S5] << S2;
                    long tens = s * 10;
                    i = 0;
                    i2 = (int) (b / s);
                    nTinyBits = (b % s) * 10;
                    long m = (FDBigInteger.LONG_5_POW[M5] << M2) * 10;
                    low2 = nTinyBits < m ? true : FloatingDecimal.$assertionsDisabled;
                    nFractBits2 = nTinyBits + m > tens ? true : FloatingDecimal.$assertionsDisabled;
                    if (i2 != 0 || nFractBits2) {
                        int ndigit5 = 0 + 1;
                        low4 = low2;
                        this.digits[0] = (char) (FloatingDecimal.$assertionsDisabled + i2);
                        i = ndigit5;
                    } else {
                        decExp--;
                        low4 = low2;
                    }
                    if (!isCompatibleFormat || decExp < -3 || decExp >= 8) {
                        nFractBits2 = false;
                        low = FloatingDecimal.$assertionsDisabled;
                    } else {
                        low = low4;
                    }
                    high = nFractBits2;
                    b = m;
                    while (!low && !high) {
                        boolean decExp3;
                        boolean high3;
                        decExp2 = decExp;
                        i2 = (int) (nTinyBits / s);
                        nTinyBits = 10 * (nTinyBits % s);
                        b *= 10;
                        if (b > 0) {
                            decExp3 = nTinyBits < b ? true : FloatingDecimal.$assertionsDisabled;
                            high3 = nTinyBits + b > tens ? true : FloatingDecimal.$assertionsDisabled;
                        } else {
                            decExp3 = true;
                            high3 = true;
                        }
                        low = decExp3;
                        high = high3;
                        q = i + 1;
                        long m2 = b;
                        this.digits[i] = (char) (48 + i2);
                        i = q;
                        decExp = decExp2;
                        b = m2;
                    }
                    decExp2 = decExp;
                    long lowDigitDifference = (nTinyBits << 1) - tens;
                    if (nTinyBits == 0) {
                        ndigit4 = i;
                        i = 1;
                    } else {
                        ndigit4 = i;
                        i = 0;
                    }
                    this.exactDecimalConversion = i;
                    j = fractBits2;
                    i7 = i2;
                    fractBits2 = lowDigitDifference;
                    decExp = decExp2;
                    i = ndigit4;
                    this.decExponent = decExp + 1;
                    this.firstDigitIndex = 0;
                    this.nDigits = i;
                    if (high) {
                        if (!low) {
                            roundup();
                        } else if (fractBits2 == 0) {
                            if ((this.digits[(this.firstDigitIndex + this.nDigits) - 1] & 1) != 0) {
                                roundup();
                            }
                        } else if (fractBits2 > 0) {
                            roundup();
                        }
                    }
                    return;
                } else {
                    int m3;
                    q = (((int) fractBits2) * FDBigInteger.SMALL_5_POW[B5]) << B2;
                    int s2 = FDBigInteger.SMALL_5_POW[S5] << S2;
                    i = s2 * 10;
                    i7 = q / s2;
                    q = 10 * (q % s2);
                    i2 = (FDBigInteger.SMALL_5_POW[M5] << M2) * 10;
                    low = q < i2 ? true : FloatingDecimal.$assertionsDisabled;
                    low2 = q + i2 > i ? true : FloatingDecimal.$assertionsDisabled;
                    if (i7 != 0 || low2) {
                        m3 = i2;
                        ndigit = 0 + 1;
                        high = low2;
                        this.digits[0] = (char) (FloatingDecimal.$assertionsDisabled + i7);
                    } else {
                        decExp--;
                        m3 = i2;
                        high = low2;
                        ndigit = 0;
                    }
                    if (!isCompatibleFormat || decExp < -3 || decExp >= 8) {
                        low = FloatingDecimal.$assertionsDisabled;
                        high = FloatingDecimal.$assertionsDisabled;
                    }
                    while (!low && !high) {
                        i7 = q / s2;
                        q = 10 * (q % s2);
                        i2 = m3 * 10;
                        i4 = nFractBits;
                        if (((long) i2) > 0) {
                            low2 = q < i2 ? true : FloatingDecimal.$assertionsDisabled;
                            nFractBits2 = q + i2 > i ? true : FloatingDecimal.$assertionsDisabled;
                        } else {
                            low2 = true;
                            nFractBits2 = true;
                        }
                        low = low2;
                        high = nFractBits2;
                        nFractBits = ndigit + 1;
                        int m4 = i2;
                        this.digits[ndigit] = (char) (48 + i7);
                        ndigit = nFractBits;
                        nFractBits = i4;
                        m3 = m4;
                    }
                    tailZeros = (long) ((q << 1) - i);
                    this.exactDecimalConversion = q == 0 ? true : FloatingDecimal.$assertionsDisabled;
                    j = fractBits2;
                    fractBits2 = tailZeros;
                    i5 = nTinyBits;
                    i6 = Bbits;
                }
                i = ndigit;
                this.decExponent = decExp + 1;
                this.firstDigitIndex = 0;
                this.nDigits = i;
                if (high) {
                }
                return;
            }
            if (i > i2) {
                Bbits = insignificantDigitsForPow2((i - i2) - 1);
            } else {
                Bbits = 0;
            }
            if (i >= FloatingDecimal.EXP_SHIFT) {
                fractBits2 <<= i - 52;
            } else {
                fractBits2 >>>= FloatingDecimal.EXP_SHIFT - i;
            }
            developLongDigits(0, fractBits2, Bbits);
        }

        private void roundup() {
            int i = (this.firstDigitIndex + this.nDigits) - 1;
            int q = this.digits[i];
            if (q == 57) {
                while (q == 57 && i > this.firstDigitIndex) {
                    this.digits[i] = '0';
                    i--;
                    q = this.digits[i];
                }
                if (q == 57) {
                    this.decExponent++;
                    this.digits[this.firstDigitIndex] = '1';
                    return;
                }
            }
            this.digits[i] = (char) (q + 1);
            this.decimalDigitsRoundedUp = true;
        }

        static int estimateDecExp(long fractBits, int binExp) {
            double d = (((Double.longBitsToDouble((fractBits & DoubleConsts.SIGNIF_BIT_MASK) | FloatingDecimal.EXP_ONE) - 1.5d) * 0.289529654d) + 0.176091259d) + (((double) binExp) * 0.301029995663981d);
            long dBits = Double.doubleToRawLongBits(d);
            int exponent = ((int) ((DoubleConsts.EXP_BIT_MASK & dBits) >> 52)) - 1023;
            int i = 0;
            boolean isNegative = (Long.MIN_VALUE & dBits) != 0 ? true : FloatingDecimal.$assertionsDisabled;
            if (exponent >= 0 && exponent < FloatingDecimal.EXP_SHIFT) {
                int r = (int) (((DoubleConsts.SIGNIF_BIT_MASK & dBits) | FloatingDecimal.FRACT_HOB) >> (FloatingDecimal.EXP_SHIFT - exponent));
                int i2 = isNegative ? ((DoubleConsts.SIGNIF_BIT_MASK >> exponent) & dBits) == 0 ? -r : (-r) - 1 : r;
                return i2;
            } else if (exponent >= 0) {
                return (int) d;
            } else {
                if ((Long.MAX_VALUE & dBits) != 0 && isNegative) {
                    i = -1;
                }
                return i;
            }
        }

        private static int insignificantDigits(int insignificant) {
            int i = 0;
            while (((long) insignificant) >= 10) {
                insignificant = (int) (((long) insignificant) / 10);
                i++;
            }
            return i;
        }

        private static int insignificantDigitsForPow2(int p2) {
            if (p2 <= 1 || p2 >= insignificantDigitsNumber.length) {
                return 0;
            }
            return insignificantDigitsNumber[p2];
        }

        private int getChars(char[] result) {
            int i = 0;
            if (this.isNegative) {
                result[0] = '-';
                i = 1;
            }
            int charLength;
            int charLength2;
            if (this.decExponent > 0 && this.decExponent < 8) {
                charLength = Math.min(this.nDigits, this.decExponent);
                System.arraycopy(this.digits, this.firstDigitIndex, (Object) result, i, charLength);
                i += charLength;
                if (charLength < this.decExponent) {
                    charLength2 = this.decExponent - charLength;
                    Arrays.fill(result, i, i + charLength2, '0');
                    i += charLength2;
                    charLength = i + 1;
                    result[i] = '.';
                    i = charLength + 1;
                    result[charLength] = '0';
                    return i;
                }
                charLength2 = i + 1;
                result[i] = '.';
                if (charLength < this.nDigits) {
                    i = this.nDigits - charLength;
                    System.arraycopy(this.digits, this.firstDigitIndex + charLength, (Object) result, charLength2, i);
                    return i + charLength2;
                }
                i = charLength2 + 1;
                result[charLength2] = '0';
                return i;
            } else if (this.decExponent > 0 || this.decExponent <= -3) {
                charLength = i + 1;
                result[i] = this.digits[this.firstDigitIndex];
                i = charLength + 1;
                result[charLength] = '.';
                if (this.nDigits > 1) {
                    System.arraycopy(this.digits, this.firstDigitIndex + 1, (Object) result, i, this.nDigits - 1);
                    i += this.nDigits - 1;
                } else {
                    charLength = i + 1;
                    result[i] = '0';
                    i = charLength;
                }
                charLength = i + 1;
                result[i] = 'E';
                if (this.decExponent <= 0) {
                    i = charLength + 1;
                    result[charLength] = '-';
                    charLength = i;
                    i = (-this.decExponent) + 1;
                } else {
                    i = this.decExponent - 1;
                }
                if (i <= 9) {
                    charLength2 = charLength + 1;
                    result[charLength] = (char) (i + 48);
                } else if (i <= 99) {
                    charLength2 = charLength + 1;
                    result[charLength] = (char) ((i / 10) + 48);
                    charLength = charLength2 + 1;
                    result[charLength2] = (char) ((i % 10) + 48);
                    return charLength;
                } else {
                    charLength2 = charLength + 1;
                    result[charLength] = (char) ((i / 100) + 48);
                    i %= 100;
                    charLength = charLength2 + 1;
                    result[charLength2] = (char) ((i / 10) + 48);
                    charLength2 = charLength + 1;
                    result[charLength] = (char) ((i % 10) + 48);
                }
                return charLength2;
            } else {
                charLength = i + 1;
                result[i] = '0';
                i = charLength + 1;
                result[charLength] = '.';
                if (this.decExponent != 0) {
                    Arrays.fill(result, i, i - this.decExponent, '0');
                    i -= this.decExponent;
                }
                System.arraycopy(this.digits, this.firstDigitIndex, (Object) result, i, this.nDigits);
                return i + this.nDigits;
            }
        }
    }

    private static class ExceptionalBinaryToASCIIBuffer implements BinaryToASCIIConverter {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private final String image;
        private boolean isNegative;

        static {
            Class cls = FloatingDecimal.class;
        }

        public ExceptionalBinaryToASCIIBuffer(String image, boolean isNegative) {
            this.image = image;
            this.isNegative = isNegative;
        }

        public String toJavaFormatString() {
            return this.image;
        }

        public void appendTo(Appendable buf) {
            if (buf instanceof StringBuilder) {
                ((StringBuilder) buf).append(this.image);
            } else if (buf instanceof StringBuffer) {
                ((StringBuffer) buf).append(this.image);
            }
        }

        public int getDecimalExponent() {
            throw new IllegalArgumentException("Exceptional value does not have an exponent");
        }

        public int getDigits(char[] digits) {
            throw new IllegalArgumentException("Exceptional value does not have digits");
        }

        public boolean isNegative() {
            return this.isNegative;
        }

        public boolean isExceptional() {
            return true;
        }

        public boolean digitsRoundedUp() {
            throw new IllegalArgumentException("Exceptional value is not rounded");
        }

        public boolean decimalDigitsExact() {
            throw new IllegalArgumentException("Exceptional value is not exact");
        }
    }

    static class PreparedASCIIToBinaryBuffer implements ASCIIToBinaryConverter {
        private final double doubleVal;
        private final float floatVal;

        public PreparedASCIIToBinaryBuffer(double doubleVal, float floatVal) {
            this.doubleVal = doubleVal;
            this.floatVal = floatVal;
        }

        public double doubleValue() {
            return this.doubleVal;
        }

        public float floatValue() {
            return this.floatVal;
        }
    }

    public static String toJavaFormatString(double d) {
        return getBinaryToASCIIConverter(d).toJavaFormatString();
    }

    public static String toJavaFormatString(float f) {
        return getBinaryToASCIIConverter(f).toJavaFormatString();
    }

    public static void appendTo(double d, Appendable buf) {
        getBinaryToASCIIConverter(d).appendTo(buf);
    }

    public static void appendTo(float f, Appendable buf) {
        getBinaryToASCIIConverter(f).appendTo(buf);
    }

    public static double parseDouble(String s) throws NumberFormatException {
        return readJavaFormatString(s).doubleValue();
    }

    public static float parseFloat(String s) throws NumberFormatException {
        return readJavaFormatString(s).floatValue();
    }

    private static BinaryToASCIIBuffer getBinaryToASCIIBuffer() {
        return (BinaryToASCIIBuffer) threadLocalBinaryToASCIIBuffer.get();
    }

    public static BinaryToASCIIConverter getBinaryToASCIIConverter(double d) {
        return getBinaryToASCIIConverter(d, true);
    }

    static BinaryToASCIIConverter getBinaryToASCIIConverter(double d, boolean isCompatibleFormat) {
        long dBits = Double.doubleToRawLongBits(d);
        boolean isNegative = (Long.MIN_VALUE & dBits) != 0 ? true : $assertionsDisabled;
        long fractBits = DoubleConsts.SIGNIF_BIT_MASK & dBits;
        int binExp = (int) ((DoubleConsts.EXP_BIT_MASK & dBits) >> EXP_SHIFT);
        if (binExp != 2047) {
            int i;
            if (binExp != 0) {
                fractBits |= FRACT_HOB;
                i = 53;
            } else if (fractBits == 0) {
                return isNegative ? B2AC_NEGATIVE_ZERO : B2AC_POSITIVE_ZERO;
            } else {
                int leadingZeros = Long.numberOfLeadingZeros(fractBits);
                int shift = leadingZeros - 11;
                fractBits <<= shift;
                binExp = 1 - shift;
                i = 64 - leadingZeros;
            }
            int nSignificantBits = i;
            i = binExp - 1023;
            BinaryToASCIIBuffer buf = getBinaryToASCIIBuffer();
            buf.setSign(isNegative);
            buf.dtoa(i, fractBits, nSignificantBits, isCompatibleFormat);
            return buf;
        } else if (fractBits != 0) {
            return B2AC_NOT_A_NUMBER;
        } else {
            return isNegative ? B2AC_NEGATIVE_INFINITY : B2AC_POSITIVE_INFINITY;
        }
    }

    private static BinaryToASCIIConverter getBinaryToASCIIConverter(float f) {
        int fBits = Float.floatToRawIntBits(f);
        boolean isNegative = (Integer.MIN_VALUE & fBits) != 0 ? true : $assertionsDisabled;
        int fractBits = FloatConsts.SIGNIF_BIT_MASK & fBits;
        int binExp = (FloatConsts.EXP_BIT_MASK & fBits) >> SINGLE_EXP_SHIFT;
        if (binExp != 255) {
            int i;
            if (binExp != 0) {
                fractBits |= SINGLE_FRACT_HOB;
                i = 24;
            } else if (fractBits == 0) {
                return isNegative ? B2AC_NEGATIVE_ZERO : B2AC_POSITIVE_ZERO;
            } else {
                int leadingZeros = Integer.numberOfLeadingZeros(fractBits);
                int shift = leadingZeros - 8;
                fractBits <<= shift;
                binExp = 1 - shift;
                i = 32 - leadingZeros;
            }
            int nSignificantBits = i;
            i = binExp - 127;
            BinaryToASCIIBuffer buf = getBinaryToASCIIBuffer();
            buf.setSign(isNegative);
            buf.dtoa(i, ((long) fractBits) << 29, nSignificantBits, true);
            return buf;
        } else if (((long) fractBits) != 0) {
            return B2AC_NOT_A_NUMBER;
        } else {
            return isNegative ? B2AC_NEGATIVE_INFINITY : B2AC_POSITIVE_INFINITY;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:128:0x0169 A:{Catch:{ StringIndexOutOfBoundsException -> 0x016a }} */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x0167 A:{Catch:{ StringIndexOutOfBoundsException -> 0x016a }} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x0130 A:{Catch:{ StringIndexOutOfBoundsException -> 0x016a }} */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x015b A:{Catch:{ StringIndexOutOfBoundsException -> 0x016a }} */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x0167 A:{Catch:{ StringIndexOutOfBoundsException -> 0x016a }} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x0169 A:{Catch:{ StringIndexOutOfBoundsException -> 0x016a }} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x003d A:{Catch:{ StringIndexOutOfBoundsException -> 0x01b7 }} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0027  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static ASCIIToBinaryConverter readJavaFormatString(String in) throws NumberFormatException {
        boolean isNegative = $assertionsDisabled;
        boolean signSeen = $assertionsDisabled;
        String in2;
        try {
            in2 = in.trim();
            try {
                int len = in2.length();
                if (len != 0) {
                    boolean signSeen2;
                    StringBuilder stringBuilder;
                    int i = 0;
                    char charAt = in2.charAt(0);
                    if (charAt != '+') {
                        if (charAt == '-') {
                            isNegative = true;
                        }
                        charAt = in2.charAt(i);
                        if (charAt != 'N') {
                            if (len - i == NAN_LENGTH && in2.indexOf(NAN_REP, i) == i) {
                                return A2BC_NOT_A_NUMBER;
                            }
                        } else if (charAt != 'I') {
                            char c;
                            int decPt;
                            if (charAt == '0' && len > i + 1) {
                                char ch = in2.charAt(i + 1);
                                if (ch == Locale.PRIVATE_USE_EXTENSION || ch == 'X') {
                                    return parseHexString(in2);
                                }
                            }
                            char[] digits = new char[len];
                            int nDigits = 0;
                            int nLeadZero = 0;
                            int decPt2 = 0;
                            boolean decSeen = $assertionsDisabled;
                            int i2 = i;
                            i = 0;
                            while (i2 < len) {
                                c = in2.charAt(i2);
                                if (c != '0') {
                                    if (c != '.') {
                                        break;
                                    } else if (decSeen) {
                                        throw new NumberFormatException("multiple points");
                                    } else {
                                        int decPt3 = i2;
                                        if (signSeen) {
                                            decPt3--;
                                        }
                                        decSeen = true;
                                        decPt2 = decPt3;
                                    }
                                } else {
                                    nLeadZero++;
                                }
                                i2++;
                            }
                            while (i2 < len) {
                                c = in2.charAt(i2);
                                if (c < '1' || c > '9') {
                                    if (c != '0') {
                                        if (c != '.') {
                                            break;
                                        } else if (decSeen) {
                                            throw new NumberFormatException("multiple points");
                                        } else {
                                            decPt = i2;
                                            if (signSeen) {
                                                decPt--;
                                            }
                                            decPt2 = decPt;
                                            decSeen = true;
                                            i2++;
                                        }
                                    } else {
                                        decPt = nDigits + 1;
                                        digits[nDigits] = c;
                                        i++;
                                    }
                                } else {
                                    decPt = nDigits + 1;
                                    digits[nDigits] = c;
                                    i = 0;
                                }
                                nDigits = decPt;
                                i2++;
                            }
                            nDigits -= i;
                            boolean isZero = nDigits == 0 ? true : $assertionsDisabled;
                            if (!(isZero && nLeadZero == 0)) {
                                int decExp;
                                if (decSeen) {
                                    decExp = decPt2 - nLeadZero;
                                } else {
                                    decExp = nDigits + i;
                                }
                                if (i2 < len) {
                                    char charAt2 = in2.charAt(i2);
                                    c = charAt2;
                                    if (charAt2 == 'e' || c == 'E') {
                                        signSeen2 = signSeen;
                                        boolean expOverflow = $assertionsDisabled;
                                        i2++;
                                        int expSign = 1;
                                        try {
                                            boolean expVal;
                                            int reallyBig;
                                            charAt2 = in2.charAt(i2);
                                            boolean expVal2 = false;
                                            if (charAt2 == '+') {
                                                decPt = expSign;
                                            } else if (charAt2 != '-') {
                                                decPt = i2;
                                                expVal = expVal2;
                                                while (decPt < len) {
                                                    if (expVal >= true) {
                                                        expOverflow = true;
                                                    }
                                                    int i3 = decPt + 1;
                                                    c = in2.charAt(decPt);
                                                    if (c < '0' || c > '9') {
                                                        decPt = i3 - 1;
                                                        break;
                                                    }
                                                    expVal = (expVal * 10) + (c - 48);
                                                    decPt = i3;
                                                }
                                                reallyBig = 214748364;
                                                signSeen = (BIG_DECIMAL_EXPONENT + nDigits) + i;
                                                if (!expOverflow) {
                                                    if (expVal <= signSeen) {
                                                        decExp += expSign * expVal;
                                                        if (decPt != i2) {
                                                            i2 = decPt;
                                                        } else {
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("For input string: \"");
                                                            stringBuilder.append(in2);
                                                            stringBuilder.append("\"");
                                                            throw new NumberFormatException(stringBuilder.toString());
                                                        }
                                                    }
                                                }
                                                decExp = expSign * signSeen;
                                                if (decPt != i2) {
                                                }
                                            } else {
                                                decPt = -1;
                                            }
                                            i2++;
                                            expSign = decPt;
                                            decPt = i2;
                                            expVal = expVal2;
                                            while (decPt < len) {
                                            }
                                            reallyBig = 214748364;
                                            signSeen = (BIG_DECIMAL_EXPONENT + nDigits) + i;
                                            if (expOverflow) {
                                            }
                                            decExp = expSign * signSeen;
                                            if (decPt != i2) {
                                            }
                                        } catch (StringIndexOutOfBoundsException e) {
                                        }
                                    } else {
                                        signSeen2 = signSeen;
                                    }
                                } else {
                                    signSeen2 = signSeen;
                                }
                                if (i2 < len) {
                                    if (i2 == len - 1) {
                                        if (!(in2.charAt(i2) == 'f' || in2.charAt(i2) == 'F' || in2.charAt(i2) == 'd')) {
                                            if (in2.charAt(i2) == 'D') {
                                            }
                                        }
                                    }
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("For input string: \"");
                                    stringBuilder.append(in2);
                                    stringBuilder.append("\"");
                                    throw new NumberFormatException(stringBuilder.toString());
                                }
                                if (!isZero) {
                                    return new ASCIIToBinaryBuffer(isNegative, decExp, digits, nDigits);
                                }
                                return isNegative ? A2BC_NEGATIVE_ZERO : A2BC_POSITIVE_ZERO;
                            }
                        } else if (len - i == INFINITY_LENGTH && in2.indexOf(INFINITY_REP, i) == i) {
                            return isNegative ? A2BC_NEGATIVE_INFINITY : A2BC_POSITIVE_INFINITY;
                        }
                        signSeen2 = signSeen;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("For input string: \"");
                        stringBuilder.append(in2);
                        stringBuilder.append("\"");
                        throw new NumberFormatException(stringBuilder.toString());
                    }
                    i = 0 + 1;
                    signSeen = true;
                    try {
                        charAt = in2.charAt(i);
                        if (charAt != 'N') {
                        }
                        signSeen2 = signSeen;
                    } catch (StringIndexOutOfBoundsException e2) {
                        signSeen2 = signSeen;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("For input string: \"");
                    stringBuilder.append(in2);
                    stringBuilder.append("\"");
                    throw new NumberFormatException(stringBuilder.toString());
                }
                throw new NumberFormatException("empty String");
            } catch (StringIndexOutOfBoundsException e3) {
            }
        } catch (StringIndexOutOfBoundsException e4) {
            in2 = in;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:141:0x027c  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x0274  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static ASCIIToBinaryConverter parseHexString(String s) {
        Matcher m = HexFloatPattern.VALUE.matcher(s);
        boolean validInput = m.matches();
        boolean z;
        if (validInput) {
            String significandString;
            int leftDigits;
            int exponentAdjust;
            String group1 = m.group(1);
            boolean isNegative = (group1 == null || !group1.equals(LanguageTag.SEP)) ? $assertionsDisabled : true;
            int rightDigits = 0;
            String group = m.group(4);
            String group4 = group;
            if (group != null) {
                significandString = stripLeadingZeros(group4);
                leftDigits = significandString.length();
            } else {
                group = stripLeadingZeros(m.group(6));
                leftDigits = group.length();
                String group7 = m.group(7);
                rightDigits = group7.length();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(group == null ? "" : group);
                stringBuilder.append(group7);
                significandString = stringBuilder.toString();
            }
            String significandString2 = stripLeadingZeros(significandString);
            int signifLength = significandString2.length();
            if (leftDigits >= 1) {
                exponentAdjust = 4 * (leftDigits - 1);
            } else {
                exponentAdjust = -4 * ((rightDigits - signifLength) + 1);
            }
            if (signifLength == 0) {
                return isNegative ? A2BC_NEGATIVE_ZERO : A2BC_POSITIVE_ZERO;
            }
            String group8 = m.group(8);
            boolean positiveExponent = (group8 == null || group8.equals("+")) ? true : $assertionsDisabled;
            String str;
            String str2;
            try {
                long significand;
                int nextShift;
                boolean z2;
                float fValue;
                long exponent = ((positiveExponent ? 1 : -1) * ((long) Integer.parseInt(m.group(9)))) + ((long) exponentAdjust);
                boolean round = $assertionsDisabled;
                boolean sticky = $assertionsDisabled;
                long leadingDigit = (long) getHexDigit(significandString2, 0);
                if (leadingDigit == 1) {
                    significand = 0 | (leadingDigit << 52);
                    nextShift = 48;
                } else if (leadingDigit <= 3) {
                    significand = 0 | (leadingDigit << 51);
                    nextShift = 47;
                    exponent++;
                } else if (leadingDigit <= 7) {
                    significand = 0 | (leadingDigit << 50);
                    nextShift = 46;
                    exponent += 2;
                } else if (leadingDigit <= 15) {
                    significand = 0 | (leadingDigit << 49);
                    nextShift = 45;
                    exponent += 3;
                } else {
                    z = validInput;
                    str = group1;
                    str2 = significandString2;
                    throw new AssertionError((Object) "Result from digit conversion too large!");
                }
                int nextShift2 = nextShift;
                nextShift = 1;
                while (nextShift < signifLength && nextShift2 >= 0) {
                    significand |= ((long) getHexDigit(significandString2, nextShift)) << nextShift2;
                    nextShift2 -= 4;
                    nextShift++;
                    leadingDigit = leadingDigit;
                }
                if (nextShift < signifLength) {
                    leadingDigit = (long) getHexDigit(significandString2, nextShift);
                    switch (nextShift2) {
                        case -4:
                            round = (leadingDigit & 8) != 0 ? true : $assertionsDisabled;
                            sticky = (leadingDigit & 7) != 0 ? true : $assertionsDisabled;
                            break;
                        case -3:
                            significand |= (leadingDigit & 8) >> 3;
                            round = (leadingDigit & 4) != 0 ? true : $assertionsDisabled;
                            sticky = (leadingDigit & 3) != 0 ? true : $assertionsDisabled;
                            break;
                        case -2:
                            significand |= (leadingDigit & 12) >> 2;
                            round = (leadingDigit & 2) != 0 ? true : $assertionsDisabled;
                            sticky = (leadingDigit & 1) != 0 ? true : $assertionsDisabled;
                            break;
                        case -1:
                            significand |= (leadingDigit & 14) >> 1;
                            round = (leadingDigit & 1) != 0 ? true : $assertionsDisabled;
                            break;
                        default:
                            long currentDigit = leadingDigit;
                            throw new AssertionError((Object) "Unexpected shift distance remainder.");
                    }
                    nextShift++;
                    while (nextShift < signifLength && !sticky) {
                        long currentDigit2 = leadingDigit;
                        leadingDigit = (long) getHexDigit(significandString2, nextShift);
                        z2 = (sticky || leadingDigit != 0) ? true : $assertionsDisabled;
                        sticky = z2;
                        nextShift++;
                    }
                }
                leadingDigit = isNegative ? Integer.MIN_VALUE : null;
                if (exponent < -126) {
                    z = validInput;
                    if (exponent >= -150) {
                        int threshShift = (int) (-98 - exponent);
                        boolean floatSticky = ((significand & ((1 << threshShift) - 1)) != 0 || round || sticky) ? true : $assertionsDisabled;
                        str = group1;
                        int iValue = (int) (significand >>> threshShift);
                        if ((iValue & 3) != 1 || floatSticky) {
                            iValue++;
                        }
                        leadingDigit |= iValue >> 1;
                        fValue = Float.intBitsToFloat(leadingDigit);
                        if (exponent <= 1023) {
                            return isNegative ? A2BC_NEGATIVE_INFINITY : A2BC_POSITIVE_INFINITY;
                        }
                        if (exponent <= 1023 && exponent >= -1022) {
                            m = (((1023 + exponent) << 52) & DoubleConsts.EXP_BIT_MASK) | (DoubleConsts.SIGNIF_BIT_MASK & significand);
                            significand = 0;
                        } else if (exponent < -1075) {
                            return isNegative ? A2BC_NEGATIVE_ZERO : A2BC_POSITIVE_ZERO;
                        } else {
                            int bitsDiscarded;
                            floatSticky = (sticky || round) ? true : $assertionsDisabled;
                            int bitsDiscarded2 = 53 - ((((int) exponent) + 1074) + 1);
                            round = (significand & (1 << (bitsDiscarded2 + -1))) != 0 ? true : $assertionsDisabled;
                            if (bitsDiscarded2 > 1) {
                                bitsDiscarded = bitsDiscarded2;
                                boolean z3 = (floatSticky || (significand & (~(-1 << (bitsDiscarded2 - 1)))) != 0) ? true : $assertionsDisabled;
                                sticky = z3;
                            } else {
                                bitsDiscarded = bitsDiscarded2;
                                sticky = floatSticky;
                            }
                            significand = 0;
                            m = 0 | (DoubleConsts.SIGNIF_BIT_MASK & (significand >> bitsDiscarded));
                        }
                        boolean leastZero = (m & 1) == significand ? true : $assertionsDisabled;
                        if ((leastZero && round && sticky) || (!leastZero && round)) {
                            m += 1;
                        }
                        if (isNegative) {
                            group1 = Double.longBitsToDouble(m | Long.MIN_VALUE);
                        } else {
                            str2 = significandString2;
                            group1 = Double.longBitsToDouble(m);
                        }
                        int floatBits = leadingDigit;
                        return new PreparedASCIIToBinaryBuffer(group1, fValue);
                    }
                } else if (exponent > 127) {
                    leadingDigit |= FloatConsts.EXP_BIT_MASK;
                } else {
                    z2 = ((significand & ((1 << 28) - 1)) != 0 || round || sticky) ? true : $assertionsDisabled;
                    int i = nextShift;
                    z = validInput;
                    nextShift = (int) (significand >>> 28);
                    int threshShift2 = 28;
                    if ((nextShift & 3) != 1 || z2) {
                        nextShift++;
                    }
                    leadingDigit |= ((((int) exponent) + 126) << SINGLE_EXP_SHIFT) + (nextShift >> 1);
                }
                fValue = Float.intBitsToFloat(leadingDigit);
                if (exponent <= 1023) {
                }
            } catch (NumberFormatException e) {
                Matcher matcher = m;
                z = validInput;
                str = group1;
                str2 = significandString2;
                ASCIIToBinaryConverter aSCIIToBinaryConverter = isNegative ? positiveExponent ? A2BC_NEGATIVE_INFINITY : A2BC_NEGATIVE_ZERO : positiveExponent ? A2BC_POSITIVE_INFINITY : A2BC_POSITIVE_ZERO;
                return aSCIIToBinaryConverter;
            }
        }
        z = validInput;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("For input string: \"");
        stringBuilder2.append(s);
        stringBuilder2.append("\"");
        throw new NumberFormatException(stringBuilder2.toString());
    }

    static String stripLeadingZeros(String s) {
        if (s.isEmpty() || s.charAt(0) != '0') {
            return s;
        }
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != '0') {
                return s.substring(i);
            }
        }
        return "";
    }

    static int getHexDigit(String s, int position) {
        int value = Character.digit(s.charAt(position), 16);
        if (value > -1 && value < 16) {
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected failure of digit conversion of ");
        stringBuilder.append(s.charAt(position));
        throw new AssertionError(stringBuilder.toString());
    }
}
