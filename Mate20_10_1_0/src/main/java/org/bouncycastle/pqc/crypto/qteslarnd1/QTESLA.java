package org.bouncycastle.pqc.crypto.qteslarnd1;

import java.security.SecureRandom;

public class QTESLA {
    private static int absolute(int i) {
        int i2 = i >> 31;
        return (i ^ i2) - i2;
    }

    private static long absolute(long j) {
        long j2 = j >> 63;
        return (j ^ j2) - j2;
    }

    private static boolean checkPolynomial(int[] iArr, int i, int i2, int i3) {
        int i4;
        int[] iArr2 = new int[i2];
        for (int i5 = 0; i5 < i2; i5++) {
            iArr2[i5] = absolute(iArr[i5]);
        }
        int i6 = i2;
        int i7 = 0;
        for (int i8 = 0; i8 < i3; i8++) {
            int i9 = 0;
            while (true) {
                i4 = i6 - 1;
                if (i9 >= i4) {
                    break;
                }
                int i10 = i9 + 1;
                int i11 = (iArr2[i10] - iArr2[i9]) >> 31;
                int i12 = iArr2[i9];
                int i13 = ~i11;
                int i14 = (iArr2[i10] & i11) | (i12 & i13);
                iArr2[i10] = (i11 & iArr2[i9]) | (iArr2[i10] & i13);
                iArr2[i9] = i14;
                i9 = i10;
            }
            i7 += iArr2[i4];
            i6--;
        }
        return i7 > i;
    }

    private static boolean checkPolynomial(long[] jArr, int i, int i2, int i3, int i4) {
        int i5;
        short[] sArr = new short[i3];
        for (int i6 = 0; i6 < i3; i6++) {
            sArr[i6] = (short) ((int) absolute(jArr[i + i6]));
        }
        int i7 = 0;
        for (int i8 = 0; i8 < i4; i8++) {
            int i9 = 0;
            while (true) {
                i5 = i3 - 1;
                if (i9 >= i5) {
                    break;
                }
                int i10 = i9 + 1;
                short s = (short) ((sArr[i10] - sArr[i9]) >> 15);
                short s2 = sArr[i9];
                int i11 = ~s;
                sArr[i10] = (short) ((s & sArr[i9]) | (sArr[i10] & i11));
                sArr[i9] = (short) ((sArr[i10] & s) | (s2 & i11));
                i9 = i10;
            }
            i7 += sArr[i5];
            i3--;
        }
        return i7 > i2;
    }

    private static int generateKeyPair(byte[] bArr, byte[] bArr2, SecureRandom secureRandom, int i, int i2, int i3, int i4, long j, int i5, int i6, int i7, double d, long[] jArr, int i8, int i9) {
        long[] jArr2;
        long[] jArr3;
        long[] jArr4;
        long[] jArr5;
        int i10;
        int i11;
        long[] jArr6;
        byte[] bArr3 = bArr;
        byte[] bArr4 = new byte[32];
        int i12 = (i2 + 3) * 32;
        byte[] bArr5 = new byte[i12];
        long[] jArr7 = new long[i];
        long[] jArr8 = new long[i];
        int i13 = i * i2;
        long[] jArr9 = new long[i13];
        long[] jArr10 = new long[i13];
        long[] jArr11 = new long[i13];
        secureRandom.nextBytes(bArr4);
        if (i4 == 485978113) {
            i10 = 485978113;
            jArr2 = jArr11;
            jArr5 = jArr10;
            jArr4 = jArr9;
            jArr3 = jArr8;
            HashUtils.secureHashAlgorithmKECCAK128(bArr5, 0, i12, bArr4, 0, 32);
        } else {
            i10 = 485978113;
            jArr2 = jArr11;
            jArr5 = jArr10;
            jArr4 = jArr9;
            jArr3 = jArr8;
        }
        if (i4 == 1129725953) {
            i11 = 1129725953;
            HashUtils.secureHashAlgorithmKECCAK256(bArr5, 0, i12, bArr4, 0, 32);
        } else {
            i11 = 1129725953;
        }
        int i14 = 0;
        int i15 = 0;
        int i16 = 0;
        while (i15 < i2) {
            while (true) {
                if (i4 == i10) {
                    i16++;
                    jArr6 = jArr4;
                    Sample.polynomialGaussSamplerIP(jArr6, i * i15, bArr5, i15 * 32, i16);
                } else {
                    jArr6 = jArr4;
                }
                if (i4 == i11) {
                    i16++;
                    Sample.polynomialGaussSamplerIIIP(jArr6, i * i15, bArr5, i15 * 32, i16);
                }
                if (!checkPolynomial(jArr6, i * i15, i8, i, i3)) {
                    break;
                }
                jArr4 = jArr6;
            }
            i15++;
            jArr4 = jArr6;
        }
        while (true) {
            if (i4 == i10) {
                i16++;
                Sample.polynomialGaussSamplerIP(jArr7, i14, bArr5, i2 * 32, i16);
            }
            if (i4 == i11) {
                i16++;
                Sample.polynomialGaussSamplerIIIP(jArr7, i14, bArr5, i2 * 32, i16);
            }
            if (!checkPolynomial(jArr7, i14, i9, i, i3)) {
                break;
            }
            bArr3 = bArr3;
            i10 = i10;
            i14 = i14;
        }
        int i17 = (i2 + 1) * 32;
        Polynomial.polynomialUniform(jArr5, bArr5, i17, i, i2, i4, j, i5, i6, i7);
        Polynomial.polynomialNumberTheoreticTransform(jArr3, jArr7, i);
        for (int i18 = i14; i18 < i2; i18++) {
            int i19 = i * i18;
            Polynomial.polynomialMultiplication(jArr2, i19, jArr5, i19, jArr3, 0, i, i4, j);
            Polynomial.polynomialAddition(jArr2, i19, jArr2, i19, jArr4, i19, i);
            for (int i20 = i14; i20 < i; i20++) {
                long j2 = (long) i4;
                int i21 = i19 + i20;
                jArr2[i21] = jArr2[i21] - (j2 & ((j2 - jArr2[i21]) >> 63));
            }
        }
        Pack.packPrivateKey(bArr2, jArr7, jArr4, bArr5, i17, i, i2);
        if (i4 == 485978113) {
            Pack.encodePublicKeyIP(bArr, jArr2, bArr5, i17);
        }
        if (i4 == 1129725953) {
            Pack.encodePublicKeyIIIP(bArr, jArr2, bArr5, i17);
        }
        return i14;
    }

    private static int generateKeyPair(byte[] bArr, byte[] bArr2, SecureRandom secureRandom, int i, int i2, int i3, long j, int i4, int i5, int i6, double d, int[] iArr, int i7, int i8) {
        int[] iArr2;
        int i9;
        int i10;
        int[] iArr3;
        int[] iArr4;
        int i11;
        boolean z;
        char c;
        int i12;
        int[] iArr5;
        int i13;
        boolean z2;
        byte[] bArr3 = new byte[32];
        byte[] bArr4 = new byte[128];
        int[] iArr6 = new int[i];
        int[] iArr7 = new int[i];
        int[] iArr8 = new int[i];
        int[] iArr9 = new int[i];
        secureRandom.nextBytes(bArr3);
        if (i3 == 4205569) {
            i9 = 4205569;
            iArr2 = iArr9;
            HashUtils.secureHashAlgorithmKECCAK128(bArr4, 0, 128, bArr3, 0, 32);
        } else {
            i9 = 4205569;
            iArr2 = iArr9;
        }
        if (i3 == 4206593 || i3 == 8404993) {
            i10 = 4206593;
            HashUtils.secureHashAlgorithmKECCAK256(bArr4, 0, 128, bArr3, 0, 32);
        } else {
            i10 = 4206593;
        }
        int i14 = 0;
        int i15 = 0;
        while (true) {
            if (i3 == i9) {
                i15++;
                Sample.polynomialGaussSamplerI(iArr7, i14, bArr4, i14, i15);
            }
            if (i3 == i10) {
                i11 = i15 + 1;
                iArr4 = iArr8;
                iArr3 = iArr7;
                Sample.polynomialGaussSamplerIII(iArr7, 0, bArr4, 0, i11, i, d, Sample.EXPONENTIAL_DISTRIBUTION_III_SIZE);
            } else {
                iArr4 = iArr8;
                iArr3 = iArr7;
                i11 = i15;
            }
            if (i3 == 8404993) {
                i11++;
                Sample.polynomialGaussSamplerIII(iArr3, 0, bArr4, 0, i11, i, d, Sample.EXPONENTIAL_DISTRIBUTION_III_SPEED);
            }
            iArr7 = iArr3;
            z = true;
            if (!checkPolynomial(iArr7, i7, i, i2)) {
                break;
            }
            i15 = i11;
            iArr8 = iArr4;
            i14 = 0;
            i10 = Parameter.Q_III_SIZE;
        }
        while (true) {
            if (i3 == i9) {
                int i16 = i11 + 1;
                i12 = 0;
                c = ' ';
                Sample.polynomialGaussSamplerI(iArr6, 0, bArr4, 32, i16);
                i11 = i16;
            } else {
                i12 = 0;
                c = ' ';
            }
            if (i3 == 4206593) {
                i11++;
                i13 = i12;
                z2 = z;
                iArr5 = iArr7;
                Sample.polynomialGaussSamplerIII(iArr6, 0, bArr4, 32, i11, i, d, Sample.EXPONENTIAL_DISTRIBUTION_III_SIZE);
            } else {
                i13 = i12;
                z2 = z;
                iArr5 = iArr7;
            }
            if (i3 == 8404993) {
                i11++;
                Sample.polynomialGaussSamplerIII(iArr6, 0, bArr4, 32, i11, i, d, Sample.EXPONENTIAL_DISTRIBUTION_III_SPEED);
            }
            if (checkPolynomial(iArr6, i8, i, i2) != z2) {
                break;
            }
            z = z2;
            iArr7 = iArr5;
            i9 = Parameter.Q_I;
        }
        Polynomial.polynomialUniform(iArr4, bArr4, 64, i, i3, j, i4, i5, i6);
        Polynomial.polynomialMultiplication(iArr2, iArr4, iArr6, i, i3, j, iArr);
        Polynomial.polynomialAdditionCorrection(iArr2, iArr2, iArr5, i, i3);
        if (i3 == 4205569) {
            Pack.encodePrivateKeyI(bArr2, iArr6, iArr5, bArr4, 64);
            Pack.encodePublicKey(bArr, iArr2, bArr4, 64, 512, 23);
        }
        if (i3 == 4206593) {
            Pack.encodePrivateKeyIIISize(bArr2, iArr6, iArr5, bArr4, 64);
            Pack.encodePublicKey(bArr, iArr2, bArr4, 64, 1024, 23);
        }
        if (i3 == 8404993) {
            Pack.encodePrivateKeyIIISpeed(bArr2, iArr6, iArr5, bArr4, 64);
            Pack.encodePublicKeyIIISpeed(bArr, iArr2, bArr4, 64);
        }
        return i13;
    }

    public static int generateKeyPairI(byte[] bArr, byte[] bArr2, SecureRandom secureRandom) {
        return generateKeyPair(bArr, bArr2, secureRandom, 512, 30, Parameter.Q_I, Parameter.Q_INVERSE_I, 23, 19, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_I, 27.0d, PolynomialHeuristic.ZETA_I, 1586, 1586);
    }

    public static int generateKeyPairIIIP(byte[] bArr, byte[] bArr2, SecureRandom secureRandom) {
        return generateKeyPair(bArr, bArr2, secureRandom, 2048, 5, 40, Parameter.Q_III_P, Parameter.Q_INVERSE_III_P, 31, 180, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_P, 10.0d, PolynomialProvablySecure.ZETA_III_P, 901, 901);
    }

    public static int generateKeyPairIIISize(byte[] bArr, byte[] bArr2, SecureRandom secureRandom) {
        return generateKeyPair(bArr, bArr2, secureRandom, 1024, 48, Parameter.Q_III_SIZE, Parameter.Q_INVERSE_III_SIZE, 23, 38, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_SIZE, 9.0d, PolynomialHeuristic.ZETA_III_SIZE, 910, 910);
    }

    public static int generateKeyPairIIISpeed(byte[] bArr, byte[] bArr2, SecureRandom secureRandom) {
        return generateKeyPair(bArr, bArr2, secureRandom, 1024, 48, Parameter.Q_III_SPEED, Parameter.Q_INVERSE_III_SPEED, 24, 38, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_SPEED, 12.0d, PolynomialHeuristic.ZETA_III_SPEED, 1147, 1233);
    }

    public static int generateKeyPairIP(byte[] bArr, byte[] bArr2, SecureRandom secureRandom) {
        return generateKeyPair(bArr, bArr2, secureRandom, 1024, 4, 25, Parameter.Q_I_P, Parameter.Q_INVERSE_I_P, 29, 108, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_I_P, 10.0d, PolynomialProvablySecure.ZETA_I_P, 554, 554);
    }

    private static void hashFunction(byte[] bArr, int i, int[] iArr, byte[] bArr2, int i2, int i3, int i4, int i5) {
        int i6 = i3 + 64;
        byte[] bArr3 = new byte[i6];
        for (int i7 = 0; i7 < i3; i7++) {
            int i8 = ((i5 / 2) - iArr[i7]) >> 31;
            iArr[i7] = ((~i8) & iArr[i7]) | ((iArr[i7] - i5) & i8);
            int i9 = 1 << i4;
            int i10 = iArr[i7] & (i9 - 1);
            int i11 = ((1 << (i4 - 1)) - i10) >> 31;
            int i12 = (i10 - i9) & i11;
            bArr3[i7] = (byte) ((iArr[i7] - ((i10 & (~i11)) | i12)) >> i4);
        }
        System.arraycopy(bArr2, i2, bArr3, i3, 64);
        if (i5 == 4205569) {
            HashUtils.secureHashAlgorithmKECCAK128(bArr, i, 32, bArr3, 0, i6);
        }
        if (i5 == 4206593 || i5 == 8404993) {
            HashUtils.secureHashAlgorithmKECCAK256(bArr, i, 32, bArr3, 0, i6);
        }
    }

    private static void hashFunction(byte[] bArr, int i, long[] jArr, byte[] bArr2, int i2, int i3, int i4, int i5, int i6) {
        int i7 = i3;
        int i8 = i4;
        int i9 = i7 * i8;
        int i10 = i9 + 64;
        byte[] bArr3 = new byte[i10];
        int i11 = 0;
        while (i11 < i8) {
            int i12 = i7 * i11;
            int i13 = 0;
            while (i13 < i7) {
                long j = jArr[i12];
                long j2 = (((long) (i6 / 2)) - j) >> 63;
                long j3 = ((j - ((long) i6)) & j2) | (j & (~j2));
                int i14 = 1 << i5;
                long j4 = ((long) (i14 - 1)) & j3;
                long j5 = (((long) (1 << (i5 - 1))) - j4) >> 63;
                bArr3[i12] = (byte) ((int) ((j3 - (((~j5) & j4) | ((j4 - ((long) i14)) & j5))) >> i5));
                i13++;
                i7 = i3;
                i12++;
                i11 = i11;
            }
            i11++;
            i7 = i3;
            i8 = i4;
        }
        System.arraycopy(bArr2, i2, bArr3, i9, 64);
        if (i6 == 485978113) {
            HashUtils.secureHashAlgorithmKECCAK128(bArr, i, 32, bArr3, 0, i10);
        }
        if (i6 == 1129725953) {
            HashUtils.secureHashAlgorithmKECCAK256(bArr, i, 32, bArr3, 0, i10);
        }
    }

    private static int signing(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom, int i3, int i4, int i5, int i6, long j, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17) {
        long[] jArr;
        long[] jArr2;
        int[] iArr;
        long[] jArr3;
        long[] jArr4;
        short[] sArr;
        boolean z;
        long[] jArr5;
        int i18;
        long[] jArr6;
        byte[] bArr4;
        long[] jArr7;
        long[] jArr8;
        byte[] bArr5 = new byte[32];
        byte[] bArr6 = new byte[32];
        byte[] bArr7 = new byte[128];
        byte[] bArr8 = new byte[32];
        int[] iArr2 = new int[i5];
        short[] sArr2 = new short[i5];
        int i19 = i3 * i4;
        long[] jArr9 = new long[i19];
        long[] jArr10 = new long[i19];
        long[] jArr11 = new long[i3];
        long[] jArr12 = new long[i3];
        long[] jArr13 = new long[i3];
        long[] jArr14 = new long[i3];
        long[] jArr15 = new long[i19];
        secureRandom.nextBytes(bArr8);
        System.arraycopy(bArr8, 0, bArr7, 32, 32);
        System.arraycopy(bArr3, i15 - 32, bArr7, 0, 32);
        if (i6 == 485978113) {
            jArr = jArr11;
            jArr4 = jArr10;
            jArr3 = jArr9;
            jArr2 = jArr12;
            sArr = sArr2;
            iArr = iArr2;
            HashUtils.secureHashAlgorithmKECCAK128(bArr7, 64, 64, bArr2, 0, i2);
            z = true;
            HashUtils.secureHashAlgorithmKECCAK128(bArr6, 0, 32, bArr7, 0, 128);
        } else {
            jArr = jArr11;
            jArr4 = jArr10;
            jArr3 = jArr9;
            iArr = iArr2;
            jArr2 = jArr12;
            sArr = sArr2;
            z = true;
        }
        if (i6 == 1129725953) {
            HashUtils.secureHashAlgorithmKECCAK256(bArr7, 64, 64, bArr2, 0, i2);
            HashUtils.secureHashAlgorithmKECCAK256(bArr6, 0, 32, bArr7, 0, 128);
        }
        Polynomial.polynomialUniform(jArr3, bArr3, i15 - 64, i3, i4, i6, j, i7, i13, i14);
        int i20 = 0;
        boolean z2 = false;
        while (true) {
            boolean z3 = true;
            int i21 = i20 + 1;
            Sample.sampleY(jArr, bArr6, 0, i21, i3, i6, i8, i9);
            long[] jArr16 = jArr;
            long[] jArr17 = jArr2;
            Polynomial.polynomialNumberTheoreticTransform(jArr17, jArr16, i3);
            int i22 = 0;
            while (i22 < i4) {
                int i23 = i3 * i22;
                Polynomial.polynomialMultiplication(jArr4, i23, jArr3, i23, jArr17, 0, i3, i6, j);
                i22++;
                z3 = z3;
                jArr17 = jArr17;
                jArr16 = jArr16;
            }
            hashFunction(bArr5, 0, jArr4, bArr7, 64, i3, i4, i10, i6);
            Sample.encodeC(iArr, sArr, bArr5, 0, i3, i5);
            Polynomial.sparsePolynomialMultiplication8(jArr14, 0, bArr3, 0, iArr, sArr, i3, i5);
            Polynomial.polynomialAddition(jArr13, 0, jArr16, 0, jArr14, 0, i3);
            long[] jArr18 = jArr13;
            if (testRejection(jArr18, i3, i8, i11) != z3) {
                int i24 = 0;
                while (true) {
                    if (i24 >= i4) {
                        jArr5 = jArr18;
                        break;
                    }
                    int i25 = i3 * i24;
                    int i26 = i24 + 1;
                    jArr5 = jArr18;
                    Polynomial.sparsePolynomialMultiplication8(jArr15, i25, bArr3, i3 * i26, iArr, sArr, i3, i5);
                    Polynomial.polynomialSubtraction(jArr4, i25, jArr4, i25, jArr15, i25, i3, i6, i16, i17);
                    z2 = testV(jArr4, i25, i3, i10, i6, i12);
                    if (z2 == z3) {
                        break;
                    }
                    i24 = i26;
                    jArr18 = jArr5;
                }
                if (z2 != z3) {
                    break;
                }
                i20 = i21;
                jArr7 = jArr17;
                jArr8 = jArr16;
                jArr13 = jArr5;
            } else {
                jArr13 = jArr18;
                i20 = i21;
                jArr7 = jArr17;
                jArr8 = jArr16;
            }
        }
        if (i6 == 485978113) {
            bArr4 = bArr5;
            jArr6 = jArr5;
            i18 = 0;
            Pack.encodeSignatureIP(bArr, 0, bArr4, 0, jArr6);
        } else {
            bArr4 = bArr5;
            jArr6 = jArr5;
            i18 = 0;
        }
        if (i6 == 1129725953) {
            Pack.encodeSignatureIIIP(bArr, i18, bArr4, i18, jArr6);
        }
        return i18;
    }

    private static int signing(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom, int i3, int i4, int i5, long j, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int[] iArr) {
        int[] iArr2;
        int[] iArr3;
        short[] sArr;
        int[] iArr4;
        short[] sArr2;
        short[] sArr3;
        int[] iArr5;
        int[] iArr6;
        int[] iArr7;
        int[] iArr8;
        int[] iArr9;
        byte[] bArr4 = new byte[32];
        byte[] bArr5 = new byte[32];
        byte[] bArr6 = new byte[128];
        byte[] bArr7 = new byte[64];
        byte[] bArr8 = new byte[32];
        int[] iArr10 = new int[i4];
        short[] sArr4 = new short[i4];
        short[] sArr5 = new short[i3];
        short[] sArr6 = new short[i3];
        int[] iArr11 = new int[i3];
        int[] iArr12 = new int[i3];
        int[] iArr13 = new int[i3];
        int[] iArr14 = new int[i3];
        int[] iArr15 = new int[i3];
        int[] iArr16 = new int[i3];
        if (i5 == 4205569) {
            Pack.decodePrivateKeyI(bArr7, sArr5, sArr6, bArr3);
        }
        if (i5 == 4206593) {
            Pack.decodePrivateKeyIIISize(bArr7, sArr5, sArr6, bArr3);
        }
        if (i5 == 8404993) {
            Pack.decodePrivateKeyIIISpeed(bArr7, sArr5, sArr6, bArr3);
        }
        secureRandom.nextBytes(bArr8);
        System.arraycopy(bArr8, 0, bArr6, 32, 32);
        System.arraycopy(bArr7, 32, bArr6, 0, 32);
        if (i5 == 4205569) {
            iArr2 = iArr12;
            sArr2 = sArr6;
            sArr = sArr5;
            iArr6 = iArr13;
            sArr3 = sArr4;
            iArr3 = iArr15;
            iArr4 = iArr10;
            HashUtils.secureHashAlgorithmKECCAK128(bArr6, 64, 64, bArr2, 0, i2);
            iArr5 = iArr14;
            HashUtils.secureHashAlgorithmKECCAK128(bArr5, 0, 32, bArr6, 0, 128);
        } else {
            iArr6 = iArr13;
            iArr2 = iArr12;
            sArr2 = sArr6;
            sArr = sArr5;
            iArr4 = iArr10;
            iArr5 = iArr14;
            sArr3 = sArr4;
            iArr3 = iArr15;
        }
        if (i5 == 4206593 || i5 == 8404993) {
            iArr7 = iArr5;
            HashUtils.secureHashAlgorithmKECCAK256(bArr6, 64, 64, bArr2, 0, i2);
            HashUtils.secureHashAlgorithmKECCAK256(bArr5, 0, 32, bArr6, 0, 128);
        } else {
            iArr7 = iArr5;
        }
        int[] iArr17 = iArr6;
        Polynomial.polynomialUniform(iArr11, bArr7, 0, i3, i5, j, i6, i12, i13);
        int i16 = 0;
        while (true) {
            int i17 = i16 + 1;
            Sample.sampleY(iArr17, bArr5, 0, i17, i3, i5, i7, i8);
            Polynomial.polynomialMultiplication(iArr2, iArr11, iArr17, i3, i5, j, iArr);
            hashFunction(bArr4, 0, iArr2, bArr6, 64, i3, i9, i5);
            Sample.encodeC(iArr4, sArr3, bArr4, 0, i3, i4);
            Polynomial.sparsePolynomialMultiplication16(iArr3, sArr, iArr4, sArr3, i3, i4);
            Polynomial.polynomialAddition(iArr7, iArr17, iArr3, i3);
            if (!testRejection(iArr7, i3, i7, i10)) {
                Polynomial.sparsePolynomialMultiplication16(iArr16, sArr2, iArr4, sArr3, i3, i4);
                iArr9 = iArr2;
                iArr8 = iArr16;
                Polynomial.polynomialSubtractionCorrection(iArr9, iArr9, iArr8, i3, i5);
                if (!testV(iArr9, i3, i9, i5, i11)) {
                    break;
                }
            } else {
                iArr9 = iArr2;
                iArr8 = iArr16;
            }
            iArr2 = iArr9;
            iArr16 = iArr8;
            iArr17 = iArr17;
            iArr3 = iArr3;
            i16 = i17;
        }
        if (i5 == 4205569) {
            Pack.encodeSignature(bArr, 0, bArr4, 0, iArr7, i3, i9);
        }
        if (i5 == 4206593) {
            Pack.encodeSignature(bArr, 0, bArr4, 0, iArr7, i3, i9);
        }
        if (i5 == 8404993) {
            Pack.encodeSignatureIIISpeed(bArr, 0, bArr4, 0, iArr7);
        }
        return 0;
    }

    static int signingI(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom) {
        return signing(bArr, bArr2, i, i2, bArr3, secureRandom, 512, 30, Parameter.Q_I, Parameter.Q_INVERSE_I, 23, 1048575, 20, 21, 1586, 1586, 19, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_I, 1021, 32, PolynomialHeuristic.ZETA_I);
    }

    public static int signingIIIP(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom) {
        return signing(bArr, bArr2, i, i2, bArr3, secureRandom, 2048, 5, 40, Parameter.Q_III_P, Parameter.Q_INVERSE_III_P, 31, Parameter.B_III_P, 23, 24, 901, 901, 180, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_P, Polynomial.PRIVATE_KEY_III_P, 15, 34);
    }

    static int signingIIISize(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom) {
        return signing(bArr, bArr2, i, i2, bArr3, secureRandom, 1024, 48, Parameter.Q_III_SIZE, Parameter.Q_INVERSE_III_SIZE, 23, 1048575, 20, 21, 910, 910, 38, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_SIZE, 1021, 32, PolynomialHeuristic.ZETA_III_SIZE);
    }

    static int signingIIISpeed(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom) {
        return signing(bArr, bArr2, i, i2, bArr3, secureRandom, 1024, 48, Parameter.Q_III_SPEED, Parameter.Q_INVERSE_III_SPEED, 24, 2097151, 21, 22, 1233, 1147, 38, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_SPEED, Parameter.BARRETT_MULTIPLICATION_III_SPEED, 32, PolynomialHeuristic.ZETA_III_SPEED);
    }

    public static int signingIP(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, SecureRandom secureRandom) {
        return signing(bArr, bArr2, i, i2, bArr3, secureRandom, 1024, 4, 25, Parameter.Q_I_P, Parameter.Q_INVERSE_I_P, 29, 2097151, 21, 22, 554, 554, 108, Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_I_P, Polynomial.PRIVATE_KEY_I_P, 1, 29);
    }

    private static boolean testRejection(int[] iArr, int i, int i2, int i3) {
        for (int i4 = 0; i4 < i; i4++) {
            if (absolute(iArr[i4]) > i2 - i3) {
                return true;
            }
        }
        return false;
    }

    private static boolean testRejection(long[] jArr, int i, int i2, int i3) {
        for (int i4 = 0; i4 < i; i4++) {
            if (absolute(jArr[i4]) > ((long) (i2 - i3))) {
                return true;
            }
        }
        return false;
    }

    private static boolean testV(int[] iArr, int i, int i2, int i3, int i4) {
        for (int i5 = 0; i5 < i; i5++) {
            int i6 = i3 / 2;
            int i7 = (i6 - iArr[i5]) >> 31;
            int i8 = ((~i7) & iArr[i5]) | ((iArr[i5] - i3) & i7);
            int i9 = 1 << (i2 - 1);
            if ((((~(absolute(i8) - (i6 - i4))) >>> 31) | ((~(absolute(i8 - ((((i8 + i9) - 1) >> i2) << i2)) - (i9 - i4))) >>> 31)) == 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean testV(long[] jArr, int i, int i2, int i3, int i4, int i5) {
        for (int i6 = 0; i6 < i2; i6++) {
            int i7 = i4 / 2;
            int i8 = i + i6;
            long j = (((long) i7) - jArr[i8]) >> 63;
            long j2 = ((~j) & jArr[i8]) | ((jArr[i8] - ((long) i4)) & j);
            int i9 = 1 << (i3 - 1);
            if ((((~(absolute(j2 - (((long) ((int) (((((long) i9) + j2) - 1) >> i3))) << i3)) - ((long) (i9 - i5)))) >>> 63) | ((~(absolute(j2) - ((long) (i7 - i5)))) >>> 63)) == 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean testZ(int[] iArr, int i, int i2, int i3) {
        for (int i4 = 0; i4 < i; i4++) {
            int i5 = i2 - i3;
            if (iArr[i4] < (-i5) || iArr[i4] > i5) {
                return true;
            }
        }
        return false;
    }

    private static boolean testZ(long[] jArr, int i, int i2, int i3) {
        for (int i4 = 0; i4 < i; i4++) {
            int i5 = i2 - i3;
            if (jArr[i4] < ((long) (-i5)) || jArr[i4] > ((long) i5)) {
                return true;
            }
        }
        return false;
    }

    private static int verifying(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, int i3, int i4, int i5, int i6, long j, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, long[] jArr) {
        byte[] bArr4;
        byte[] bArr5 = new byte[32];
        byte[] bArr6 = new byte[32];
        byte[] bArr7 = new byte[32];
        byte[] bArr8 = new byte[64];
        int i16 = i3 * i4;
        int[] iArr = new int[i16];
        int[] iArr2 = new int[i5];
        short[] sArr = new short[i5];
        long[] jArr2 = new long[i16];
        long[] jArr3 = new long[i3];
        long[] jArr4 = new long[i3];
        long[] jArr5 = new long[i16];
        long[] jArr6 = new long[i16];
        if (i2 < i11) {
            return -1;
        }
        if (i6 == 485978113) {
            Pack.decodeSignatureIP(bArr5, jArr3, bArr2, i);
        }
        if (i6 == 1129725953) {
            Pack.decodeSignatureIIIP(bArr5, jArr3, bArr2, i);
        }
        if (testZ(jArr3, i3, i8, i10)) {
            return -2;
        }
        if (i6 == 485978113) {
            Pack.decodePublicKeyIP(iArr, bArr7, 0, bArr3);
        }
        if (i6 == 1129725953) {
            Pack.decodePublicKeyIIIP(iArr, bArr7, 0, bArr3);
        }
        Polynomial.polynomialUniform(jArr6, bArr7, 0, i3, i4, i6, j, i7, i12, i13);
        Sample.encodeC(iArr2, sArr, bArr5, 0, i3, i5);
        Polynomial.polynomialNumberTheoreticTransform(jArr4, jArr3, i3);
        for (int i17 = 0; i17 < i4; i17++) {
            int i18 = i3 * i17;
            Polynomial.polynomialMultiplication(jArr2, i18, jArr6, i18, jArr4, 0, i3, i6, j);
            Polynomial.sparsePolynomialMultiplication32(jArr5, i18, iArr, i18, iArr2, sArr, i3, i5, i6, i14, i15);
            Polynomial.polynomialSubtraction(jArr2, i18, jArr2, i18, jArr5, i18, i3, i6, i14, i15);
        }
        if (i6 == 485978113) {
            bArr4 = bArr;
            HashUtils.secureHashAlgorithmKECCAK128(bArr8, 0, 64, bArr, 0, bArr4.length);
        } else {
            bArr4 = bArr;
        }
        if (i6 == 1129725953) {
            HashUtils.secureHashAlgorithmKECCAK256(bArr8, 0, 64, bArr, 0, bArr4.length);
        }
        hashFunction(bArr6, 0, jArr2, bArr8, 0, i3, i4, i9, i6);
        return !CommonFunction.memoryEqual(bArr5, 0, bArr6, 0, 32) ? -3 : 0;
    }

    private static int verifying(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3, int i3, int i4, int i5, long j, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int[] iArr) {
        int[] iArr2;
        short[] sArr;
        int[] iArr3;
        int[] iArr4;
        int[] iArr5;
        int[] iArr6;
        int i16;
        byte[] bArr4 = new byte[32];
        byte[] bArr5 = new byte[32];
        byte[] bArr6 = new byte[32];
        byte[] bArr7 = new byte[64];
        int[] iArr7 = new int[i3];
        int[] iArr8 = new int[i4];
        short[] sArr2 = new short[i4];
        int[] iArr9 = new int[i3];
        int[] iArr10 = new int[i3];
        int[] iArr11 = new int[i3];
        int[] iArr12 = new int[i3];
        if (i2 < i11) {
            return -1;
        }
        if (i5 == 4205569 || i5 == 4206593) {
            iArr5 = iArr12;
            iArr4 = iArr11;
            iArr6 = iArr10;
            iArr3 = iArr9;
            sArr = sArr2;
            iArr2 = iArr8;
            Pack.decodeSignature(bArr4, iArr10, bArr2, i, i3, i8);
        } else {
            iArr5 = iArr12;
            iArr4 = iArr11;
            iArr6 = iArr10;
            iArr3 = iArr9;
            sArr = sArr2;
            iArr2 = iArr8;
        }
        if (i5 == 8404993) {
            Pack.decodeSignatureIIISpeed(bArr4, iArr6, bArr2, i);
        }
        if (testZ(iArr6, i3, i7, i9)) {
            return -2;
        }
        if (i5 == 4205569 || i5 == 4206593) {
            i16 = 8404993;
            Pack.decodePublicKey(iArr7, bArr6, 0, bArr3, i3, i6);
        } else {
            i16 = 8404993;
        }
        if (i5 == i16) {
            Pack.decodePublicKeyIIISpeed(iArr7, bArr6, 0, bArr3);
        }
        Polynomial.polynomialUniform(iArr5, bArr6, 0, i3, i5, j, i6, i12, i13);
        Sample.encodeC(iArr2, sArr, bArr4, 0, i3, i4);
        Polynomial.sparsePolynomialMultiplication32(iArr4, iArr7, iArr2, sArr, i3, i4);
        Polynomial.polynomialMultiplication(iArr3, iArr5, iArr6, i3, i5, j, iArr);
        Polynomial.polynomialSubtractionMontgomery(iArr3, iArr3, iArr4, i3, i5, j, i10);
        if (i5 == 4205569) {
            HashUtils.secureHashAlgorithmKECCAK128(bArr7, 0, 64, bArr, 0, bArr.length);
        }
        if (i5 == 4206593 || i5 == i16) {
            HashUtils.secureHashAlgorithmKECCAK256(bArr7, 0, 64, bArr, 0, bArr.length);
        }
        hashFunction(bArr5, 0, iArr3, bArr7, 0, i3, i8, i5);
        return !CommonFunction.memoryEqual(bArr4, 0, bArr5, 0, 32) ? -3 : 0;
    }

    static int verifyingI(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3) {
        return verifying(bArr, bArr2, i, i2, bArr3, 512, 30, (int) Parameter.Q_I, (long) Parameter.Q_INVERSE_I, 23, 1048575, 21, 1586, (int) Parameter.R_I, (int) Polynomial.SIGNATURE_I, 19, (int) Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_I, 1021, 32, PolynomialHeuristic.ZETA_I);
    }

    static int verifyingIIISize(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3) {
        return verifying(bArr, bArr2, i, i2, bArr3, 1024, 48, (int) Parameter.Q_III_SIZE, (long) Parameter.Q_INVERSE_III_SIZE, 23, 1048575, 21, 910, (int) Parameter.R_III_SIZE, (int) Polynomial.SIGNATURE_III_SIZE, 38, (int) Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_SIZE, 1021, 32, PolynomialHeuristic.ZETA_III_SIZE);
    }

    static int verifyingIIISpeed(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3) {
        return verifying(bArr, bArr2, i, i2, bArr3, 1024, 48, (int) Parameter.Q_III_SPEED, (long) Parameter.Q_INVERSE_III_SPEED, 24, 2097151, 22, 1233, (int) Parameter.R_III_SPEED, 2848, 38, (int) Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_SPEED, (int) Parameter.BARRETT_MULTIPLICATION_III_SPEED, 32, PolynomialHeuristic.ZETA_III_SPEED);
    }

    static int verifyingPI(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3) {
        return verifying(bArr, bArr2, i, i2, bArr3, 1024, 4, 25, (int) Parameter.Q_I_P, (long) Parameter.Q_INVERSE_I_P, 29, 2097151, 22, 554, 2848, 108, (int) Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_I_P, 1, 29, PolynomialProvablySecure.ZETA_I_P);
    }

    static int verifyingPIII(byte[] bArr, byte[] bArr2, int i, int i2, byte[] bArr3) {
        return verifying(bArr, bArr2, i, i2, bArr3, 2048, 5, 40, (int) Parameter.Q_III_P, (long) Parameter.Q_INVERSE_III_P, 31, (int) Parameter.B_III_P, 24, 901, (int) Polynomial.SIGNATURE_III_P, 180, (int) Parameter.INVERSE_NUMBER_THEORETIC_TRANSFORM_III_P, 15, 34, PolynomialProvablySecure.ZETA_III_P);
    }
}
