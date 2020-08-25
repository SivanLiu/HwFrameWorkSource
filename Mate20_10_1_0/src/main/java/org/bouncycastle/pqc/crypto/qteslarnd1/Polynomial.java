package org.bouncycastle.pqc.crypto.qteslarnd1;

import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.util.Arrays;

class Polynomial {
    public static final int HASH = 32;
    public static final int MESSAGE = 64;
    public static final int PRIVATE_KEY_I = 1344;
    public static final int PRIVATE_KEY_III_P = 12352;
    public static final int PRIVATE_KEY_III_SIZE = 2112;
    public static final int PRIVATE_KEY_III_SPEED = 2368;
    public static final int PRIVATE_KEY_I_P = 5184;
    public static final int PUBLIC_KEY_I = 1504;
    public static final int PUBLIC_KEY_III_P = 39712;
    public static final int PUBLIC_KEY_III_SIZE = 2976;
    public static final int PUBLIC_KEY_III_SPEED = 3104;
    public static final int PUBLIC_KEY_I_P = 14880;
    public static final int RANDOM = 32;
    public static final int SEED = 32;
    public static final int SIGNATURE_I = 1376;
    public static final int SIGNATURE_III_P = 6176;
    public static final int SIGNATURE_III_SIZE = 2720;
    public static final int SIGNATURE_III_SPEED = 2848;
    public static final int SIGNATURE_I_P = 2848;

    Polynomial() {
    }

    public static int barrett(int i, int i2, int i3, int i4) {
        return i - (((int) ((((long) i) * ((long) i3)) >> i4)) * i2);
    }

    public static long barrett(long j, int i, int i2, int i3) {
        return j - (((((long) i2) * j) >> i3) * ((long) i));
    }

    private static void componentWisePolynomialMultiplication(int[] iArr, int[] iArr2, int[] iArr3, int i, int i2, long j) {
        for (int i3 = 0; i3 < i; i3++) {
            iArr[i3] = montgomery(((long) iArr2[i3]) * ((long) iArr3[i3]), i2, j);
        }
    }

    private static void componentWisePolynomialMultiplication(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3, int i4, int i5, long j) {
        for (int i6 = 0; i6 < i4; i6++) {
            jArr[i + i6] = montgomeryP(jArr2[i2 + i6] * jArr3[i3 + i6], i5, j);
        }
    }

    private static void inverseNumberTheoreticTransform(int[] iArr, int[] iArr2, int i, int i2, long j, int i3, int i4, int i5) {
        int i6 = 1;
        int i7 = 0;
        while (i6 < i) {
            int i8 = i7;
            int i9 = 0;
            while (i9 < i) {
                int i10 = i8 + 1;
                long j2 = (long) iArr2[i8];
                int i11 = i9;
                while (i11 < i9 + i6) {
                    int i12 = iArr[i11];
                    if (i6 == 16) {
                        iArr[i11] = barrett(iArr[i11 + i6] + i12, i2, i4, i5);
                    } else {
                        iArr[i11] = iArr[i11 + i6] + i12;
                    }
                    int i13 = i11 + i6;
                    iArr[i13] = montgomery(((long) (i12 - iArr[i13])) * j2, i2, j);
                    i11++;
                    i6 = i6;
                }
                i9 = i11 + i6;
                i8 = i10;
            }
            i6 *= 2;
            i7 = i8;
        }
        for (int i14 = 0; i14 < i / 2; i14++) {
            iArr[i14] = montgomery(((long) i3) * ((long) iArr[i14]), i2, j);
        }
    }

    private static void inverseNumberTheoreticTransformI(int[] iArr, int[] iArr2) {
        int i = 1;
        int i2 = 0;
        while (i < 512) {
            int i3 = i2;
            int i4 = 0;
            while (i4 < 512) {
                int i5 = i3 + 1;
                long j = (long) iArr2[i3];
                int i6 = i4;
                while (i6 < i4 + i) {
                    int i7 = iArr[i6];
                    int i8 = i6 + i;
                    iArr[i6] = iArr[i8] + i7;
                    iArr[i8] = montgomery(((long) (i7 - iArr[i8])) * j, Parameter.Q_I, Parameter.Q_INVERSE_I);
                    i6++;
                }
                i4 = i6 + i;
                i3 = i5;
            }
            i *= 2;
            i2 = i3;
        }
        for (int i9 = 0; i9 < 256; i9++) {
            iArr[i9] = montgomery(((long) iArr[i9]) * 1081347, Parameter.Q_I, Parameter.Q_INVERSE_I);
        }
    }

    private static void inverseNumberTheoreticTransformIIIP(long[] jArr, int i, long[] jArr2, int i2) {
        int i3 = 1;
        int i4 = 0;
        while (i3 < 2048) {
            int i5 = i4;
            int i6 = 0;
            while (i6 < 2048) {
                int i7 = i5 + 1;
                long j = jArr2[i2 + i5];
                int i8 = i6;
                while (i8 < i6 + i3) {
                    int i9 = i + i8;
                    long j2 = jArr[i9];
                    int i10 = i9 + i3;
                    jArr[i9] = barrett(jArr[i10] + j2, (int) Parameter.Q_III_P, 15, 34);
                    jArr[i10] = barrett(montgomeryP((j2 + (2259451906L - jArr[i10])) * j, Parameter.Q_III_P, Parameter.Q_INVERSE_III_P), (int) Parameter.Q_III_P, 15, 34);
                    i8++;
                }
                i6 = i8 + i3;
                i5 = i7;
            }
            i3 *= 2;
            i4 = i5;
        }
    }

    private static void inverseNumberTheoreticTransformIP(long[] jArr, int i, long[] jArr2, int i2) {
        int i3 = 1;
        int i4 = 0;
        while (true) {
            if (i3 < 1024) {
                int i5 = i4;
                int i6 = 0;
                while (i6 < 1024) {
                    int i7 = i5 + 1;
                    long j = jArr2[i2 + i5];
                    int i8 = i6;
                    while (i8 < i6 + i3) {
                        int i9 = i + i8;
                        long j2 = jArr[i9];
                        int i10 = i9 + i3;
                        jArr[i9] = j2 + jArr[i10];
                        jArr[i10] = montgomeryP(j * (j2 + (971956226 - jArr[i10])), Parameter.Q_I_P, Parameter.Q_INVERSE_I_P);
                        i8++;
                    }
                    i6 = i8 + i3;
                    i5 = i7;
                }
                int i11 = i3 * 2;
                int i12 = 0;
                for (int i13 = 1024; i12 < i13; i13 = 1024) {
                    int i14 = i5 + 1;
                    long j3 = jArr2[i2 + i5];
                    int i15 = i12;
                    while (i15 < i12 + i11) {
                        int i16 = i + i15;
                        long j4 = jArr[i16];
                        int i17 = i16 + i11;
                        jArr[i16] = barrett(j4 + jArr[i17], (int) Parameter.Q_I_P, 1, 29);
                        jArr[i17] = montgomeryP(j3 * (j4 + (971956226 - jArr[i17])), Parameter.Q_I_P, Parameter.Q_INVERSE_I_P);
                        i15++;
                    }
                    i12 = i15 + i11;
                    i5 = i14;
                }
                i3 = i11 * 2;
                i4 = i5;
            } else {
                return;
            }
        }
    }

    private static int montgomery(long j, int i, long j2) {
        return (int) ((j + (((j2 * j) & BodyPartID.bodyIdMax) * ((long) i))) >> 32);
    }

    private static long montgomeryP(long j, int i, long j2) {
        return (j + (((j2 * j) & BodyPartID.bodyIdMax) * ((long) i))) >> 32;
    }

    private static void numberTheoreticTransform(int[] iArr, int[] iArr2, int i, int i2, long j) {
        int i3 = i >> 1;
        int i4 = 0;
        while (i3 > 0) {
            int i5 = i4;
            int i6 = 0;
            while (i6 < i) {
                int i7 = i5 + 1;
                long j2 = (long) iArr2[i5];
                int i8 = i6;
                while (i8 < i6 + i3) {
                    int i9 = i8 + i3;
                    int montgomery = montgomery(((long) iArr[i9]) * j2, i2, j);
                    iArr[i9] = iArr[i8] - montgomery;
                    iArr[i8] = iArr[i8] + montgomery;
                    i8++;
                }
                i6 = i8 + i3;
                i5 = i7;
            }
            i3 >>= 1;
            i4 = i5;
        }
    }

    private static void numberTheoreticTransformIIIP(long[] jArr, long[] jArr2) {
        int i = 1024;
        int i2 = 0;
        while (i > 0) {
            int i3 = i2;
            int i4 = 0;
            while (i4 < 2048) {
                int i5 = i3 + 1;
                int i6 = (int) jArr2[i3];
                int i7 = i4;
                while (i7 < i4 + i) {
                    int i8 = i7 + i;
                    long barrett = barrett(montgomeryP(((long) i6) * jArr[i8], Parameter.Q_III_P, Parameter.Q_INVERSE_III_P), (int) Parameter.Q_III_P, 15, 34);
                    jArr[i8] = barrett(jArr[i7] + (2259451906L - barrett), (int) Parameter.Q_III_P, 15, 34);
                    jArr[i7] = barrett(jArr[i7] + barrett, (int) Parameter.Q_III_P, 15, 34);
                    i7++;
                }
                i4 = i7 + i;
                i3 = i5;
            }
            i >>= 1;
            i2 = i3;
        }
    }

    private static void numberTheoreticTransformIP(long[] jArr, long[] jArr2) {
        int i = 512;
        int i2 = 0;
        while (i > 0) {
            int i3 = i2;
            int i4 = 0;
            while (i4 < 1024) {
                int i5 = i3 + 1;
                long j = jArr2[i3];
                int i6 = i4;
                while (i6 < i4 + i) {
                    int i7 = i6 + i;
                    long montgomeryP = montgomeryP(jArr[i7] * j, Parameter.Q_I_P, Parameter.Q_INVERSE_I_P);
                    jArr[i7] = jArr[i6] + (485978113 - montgomeryP);
                    jArr[i6] = jArr[i6] + montgomeryP;
                    i6++;
                }
                i4 = i6 + i;
                i3 = i5;
            }
            i >>= 1;
            i2 = i3;
        }
    }

    public static void polynomialAddition(int[] iArr, int[] iArr2, int[] iArr3, int i) {
        for (int i2 = 0; i2 < i; i2++) {
            iArr[i2] = iArr2[i2] + iArr3[i2];
        }
    }

    public static void polynomialAddition(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3, int i4) {
        for (int i5 = 0; i5 < i4; i5++) {
            jArr[i + i5] = jArr2[i2 + i5] + jArr3[i3 + i5];
        }
    }

    public static void polynomialAdditionCorrection(int[] iArr, int[] iArr2, int[] iArr3, int i, int i2) {
        for (int i3 = 0; i3 < i; i3++) {
            iArr[i3] = iArr2[i3] + iArr3[i3];
            iArr[i3] = iArr[i3] + ((iArr[i3] >> 31) & i2);
            iArr[i3] = iArr[i3] - i2;
            iArr[i3] = iArr[i3] + ((iArr[i3] >> 31) & i2);
        }
    }

    public static void polynomialMultiplication(int[] iArr, int[] iArr2, int[] iArr3, int i, int i2, long j, int[] iArr4) {
        int[] iArr5 = new int[i];
        for (int i3 = 0; i3 < i; i3++) {
            iArr5[i3] = iArr3[i3];
        }
        numberTheoreticTransform(iArr5, iArr4, i, i2, j);
        componentWisePolynomialMultiplication(iArr, iArr2, iArr5, i, i2, j);
        if (i2 == 4205569) {
            inverseNumberTheoreticTransformI(iArr, PolynomialHeuristic.ZETA_INVERSE_I);
        }
        if (i2 == 4206593) {
            inverseNumberTheoreticTransform(iArr, PolynomialHeuristic.ZETA_INVERSE_III_SIZE, 1024, Parameter.Q_III_SIZE, Parameter.Q_INVERSE_III_SIZE, Parameter.R_III_SIZE, 1021, 32);
        }
        if (i2 == 8404993) {
            inverseNumberTheoreticTransform(iArr, PolynomialHeuristic.ZETA_INVERSE_III_SPEED, 1024, Parameter.Q_III_SPEED, Parameter.Q_INVERSE_III_SPEED, Parameter.R_III_SPEED, Parameter.BARRETT_MULTIPLICATION_III_SPEED, 32);
        }
    }

    public static void polynomialMultiplication(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3, int i4, int i5, long j) {
        componentWisePolynomialMultiplication(jArr, i, jArr2, i2, jArr3, i3, i4, i5, j);
        if (i5 == 485978113) {
            inverseNumberTheoreticTransformIP(jArr, i, PolynomialProvablySecure.ZETA_INVERSE_I_P, 0);
        }
        if (i5 == 1129725953) {
            inverseNumberTheoreticTransformIIIP(jArr, i, PolynomialProvablySecure.ZETA_INVERSE_III_P, 0);
        }
    }

    public static void polynomialNumberTheoreticTransform(long[] jArr, long[] jArr2, int i) {
        for (int i2 = 0; i2 < i; i2++) {
            jArr[i2] = jArr2[i2];
        }
        if (i == 1024) {
            numberTheoreticTransformIP(jArr, PolynomialProvablySecure.ZETA_I_P);
        }
        if (i == 2048) {
            numberTheoreticTransformIIIP(jArr, PolynomialProvablySecure.ZETA_III_P);
        }
    }

    public static void polynomialSubtraction(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3, int i4, int i5, int i6, int i7) {
        for (int i8 = 0; i8 < i4; i8++) {
            jArr[i + i8] = barrett(jArr2[i2 + i8] - jArr3[i3 + i8], i5, i6, i7);
        }
    }

    public static void polynomialSubtractionCorrection(int[] iArr, int[] iArr2, int[] iArr3, int i, int i2) {
        for (int i3 = 0; i3 < i; i3++) {
            iArr[i3] = iArr2[i3] - iArr3[i3];
            iArr[i3] = iArr[i3] + ((iArr[i3] >> 31) & i2);
        }
    }

    public static void polynomialSubtractionMontgomery(int[] iArr, int[] iArr2, int[] iArr3, int i, int i2, long j, int i3) {
        for (int i4 = 0; i4 < i; i4++) {
            iArr[i4] = montgomery(((long) i3) * ((long) (iArr2[i4] - iArr3[i4])), i2, j);
        }
    }

    public static void polynomialUniform(int[] iArr, byte[] bArr, int i, int i2, int i3, long j, int i4, int i5, int i6) {
        int i7;
        int i8;
        int i9;
        int i10;
        byte[] bArr2;
        int i11 = (i4 + 7) / 8;
        int i12 = 1;
        int i13 = (1 << i4) - 1;
        int i14 = i5 * 168;
        byte[] bArr3 = new byte[i14];
        byte[] bArr4 = bArr3;
        HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr3, 0, i14, 0, bArr, i, 32);
        int i15 = i5;
        short s = (short) 1;
        int i16 = 0;
        int i17 = 0;
        while (i16 < i2) {
            if (i17 > (i15 * 168) - (i11 * 4)) {
                HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr4, 0, 168, s, bArr, i, 32);
                i7 = i12;
                s = (short) (s + 1);
                i8 = 0;
            } else {
                i7 = i15;
                i8 = i17;
            }
            int load32 = CommonFunction.load32(bArr4, i8) & i13;
            int i18 = i8 + i11;
            int load322 = CommonFunction.load32(bArr4, i18) & i13;
            int i19 = i18 + i11;
            int load323 = CommonFunction.load32(bArr4, i19) & i13;
            int i20 = i19 + i11;
            int load324 = CommonFunction.load32(bArr4, i20) & i13;
            int i21 = i20 + i11;
            if (load32 >= i3 || i16 >= i2) {
                bArr2 = bArr4;
                i9 = i21;
                i10 = i7;
            } else {
                bArr2 = bArr4;
                i9 = i21;
                i10 = i7;
                iArr[i16] = montgomery(((long) load32) * ((long) i6), i3, j);
                i16++;
            }
            if (load322 < i3 && i16 < i2) {
                iArr[i16] = montgomery(((long) load322) * ((long) i6), i3, j);
                i16++;
            }
            if (load323 < i3 && i16 < i2) {
                iArr[i16] = montgomery(((long) load323) * ((long) i6), i3, j);
                i16++;
            }
            if (load324 >= i3 || i16 >= i2) {
                i17 = i9;
            } else {
                iArr[i16] = montgomery(((long) load324) * ((long) i6), i3, j);
                i17 = i9;
                i16++;
            }
            bArr4 = bArr2;
            i15 = i10;
            i12 = 1;
        }
    }

    public static void polynomialUniform(long[] jArr, byte[] bArr, int i, int i2, int i3, int i4, long j, int i5, int i6, int i7) {
        byte[] bArr2;
        int i8;
        int i9;
        byte[] bArr3;
        int i10;
        int i11 = (i5 + 7) / 8;
        int i12 = (1 << i5) - 1;
        int i13 = i6 * 168;
        byte[] bArr4 = new byte[i13];
        HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr4, 0, i13, 0, bArr, i, 32);
        int i14 = i6;
        short s = (short) 1;
        int i15 = 0;
        int i16 = 0;
        while (true) {
            int i17 = i2 * i3;
            if (i15 < i17) {
                if (i16 > (i14 * 168) - (i11 * 4)) {
                    i8 = i17;
                    HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr4, 0, 168, s, bArr, i, 32);
                    bArr2 = bArr4;
                    s = (short) (s + 1);
                    i16 = 0;
                    i14 = 1;
                } else {
                    i8 = i17;
                    bArr2 = bArr4;
                }
                int load32 = CommonFunction.load32(bArr2, i16) & i12;
                int i18 = i16 + i11;
                int load322 = CommonFunction.load32(bArr2, i18) & i12;
                int i19 = i18 + i11;
                int load323 = CommonFunction.load32(bArr2, i19) & i12;
                int i20 = i19 + i11;
                int load324 = CommonFunction.load32(bArr2, i20) & i12;
                int i21 = i20 + i11;
                if (load32 >= i4 || i15 >= i8) {
                    i10 = i21;
                    i9 = i14;
                    bArr3 = bArr2;
                } else {
                    i10 = i21;
                    i9 = i14;
                    bArr3 = bArr2;
                    jArr[i15] = montgomeryP(((long) load32) * ((long) i7), i4, j);
                    i15++;
                }
                if (load322 < i4 && i15 < i8) {
                    jArr[i15] = montgomeryP(((long) load322) * ((long) i7), i4, j);
                    i15++;
                }
                if (load323 < i4 && i15 < i8) {
                    jArr[i15] = montgomeryP(((long) load323) * ((long) i7), i4, j);
                    i15++;
                }
                if (load324 >= i4 || i15 >= i8) {
                    i14 = i9;
                } else {
                    jArr[i15] = montgomeryP(((long) load324) * ((long) i7), i4, j);
                    i14 = i9;
                    i15++;
                }
                i16 = i10;
                bArr4 = bArr3;
            } else {
                return;
            }
        }
    }

    public static void sparsePolynomialMultiplication16(int[] iArr, short[] sArr, int[] iArr2, short[] sArr2, int i, int i2) {
        Arrays.fill(iArr, 0);
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = iArr2[i3];
            for (int i5 = 0; i5 < i4; i5++) {
                iArr[i5] = iArr[i5] - (sArr2[i3] * sArr[(i + i5) - i4]);
            }
            for (int i6 = i4; i6 < i; i6++) {
                iArr[i6] = iArr[i6] + (sArr2[i3] * sArr[i6 - i4]);
            }
        }
    }

    public static void sparsePolynomialMultiplication32(int[] iArr, int[] iArr2, int[] iArr3, short[] sArr, int i, int i2) {
        Arrays.fill(iArr, 0);
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = iArr3[i3];
            for (int i5 = 0; i5 < i4; i5++) {
                iArr[i5] = iArr[i5] - (sArr[i3] * iArr2[(i + i5) - i4]);
            }
            for (int i6 = i4; i6 < i; i6++) {
                iArr[i6] = iArr[i6] + (sArr[i3] * iArr2[i6 - i4]);
            }
        }
    }

    public static void sparsePolynomialMultiplication32(long[] jArr, int i, int[] iArr, int i2, int[] iArr2, short[] sArr, int i3, int i4, int i5, int i6, int i7) {
        Arrays.fill(jArr, 0);
        for (int i8 = 0; i8 < i4; i8++) {
            int i9 = iArr2[i8];
            for (int i10 = 0; i10 < i9; i10++) {
                int i11 = i + i10;
                jArr[i11] = jArr[i11] - ((long) (sArr[i8] * iArr[((i2 + i3) + i10) - i9]));
            }
            for (int i12 = i9; i12 < i3; i12++) {
                int i13 = i + i12;
                jArr[i13] = jArr[i13] + ((long) (sArr[i8] * iArr[(i2 + i12) - i9]));
            }
        }
        for (int i14 = 0; i14 < i3; i14++) {
            int i15 = i + i14;
            jArr[i15] = barrett(jArr[i15], i5, i6, i7);
        }
    }

    public static void sparsePolynomialMultiplication8(long[] jArr, int i, byte[] bArr, int i2, int[] iArr, short[] sArr, int i3, int i4) {
        Arrays.fill(jArr, 0);
        for (int i5 = 0; i5 < i4; i5++) {
            int i6 = iArr[i5];
            for (int i7 = 0; i7 < i6; i7++) {
                int i8 = i + i7;
                jArr[i8] = jArr[i8] - ((long) (sArr[i5] * bArr[((i2 + i3) + i7) - i6]));
            }
            for (int i9 = i6; i9 < i3; i9++) {
                int i10 = i + i9;
                jArr[i10] = jArr[i10] + ((long) (sArr[i5] * bArr[(i2 + i9) - i6]));
            }
        }
    }
}
