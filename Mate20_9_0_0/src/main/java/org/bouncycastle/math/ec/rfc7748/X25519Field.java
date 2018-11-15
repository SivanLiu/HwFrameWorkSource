package org.bouncycastle.math.ec.rfc7748;

public abstract class X25519Field {
    private static final int M24 = 16777215;
    private static final int M25 = 33554431;
    private static final int M26 = 67108863;
    public static final int SIZE = 10;

    private X25519Field() {
    }

    public static void add(int[] iArr, int[] iArr2, int[] iArr3) {
        for (int i = 0; i < 10; i++) {
            iArr3[i] = iArr[i] + iArr2[i];
        }
    }

    public static void apm(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4) {
        for (int i = 0; i < 10; i++) {
            int i2 = iArr[i];
            int i3 = iArr2[i];
            iArr3[i] = i2 + i3;
            iArr4[i] = i2 - i3;
        }
    }

    public static void carry(int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        int i4 = iArr[3];
        int i5 = iArr[4];
        int i6 = iArr[5];
        int i7 = iArr[6];
        int i8 = iArr[7];
        int i9 = iArr[8];
        int i10 = iArr[9];
        i4 += i3 >> 25;
        i6 += i5 >> 25;
        i9 += i8 >> 25;
        i += (i10 >> 25) * 38;
        i2 += i >> 26;
        i7 += i6 >> 26;
        i6 &= M26;
        i3 = (i3 & M25) + (i2 >> 26);
        i2 &= M26;
        i5 = (i5 & M25) + (i4 >> 26);
        i4 &= M26;
        i8 = (i8 & M25) + (i7 >> 26);
        i7 &= M26;
        i10 = (i10 & M25) + (i9 >> 26);
        i9 &= M26;
        iArr[0] = i & M26;
        iArr[1] = i2;
        iArr[2] = i3;
        iArr[3] = i4;
        iArr[4] = i5;
        iArr[5] = i6;
        iArr[6] = i7;
        iArr[7] = i8;
        iArr[8] = i9;
        iArr[9] = i10;
    }

    public static void copy(int[] iArr, int i, int[] iArr2, int i2) {
        for (int i3 = 0; i3 < 10; i3++) {
            iArr2[i2 + i3] = iArr[i + i3];
        }
    }

    public static int[] create() {
        return new int[10];
    }

    public static void cswap(int i, int[] iArr, int[] iArr2) {
        i = 0 - i;
        for (int i2 = 0; i2 < 10; i2++) {
            int i3 = iArr[i2];
            int i4 = iArr2[i2];
            int i5 = (i3 ^ i4) & i;
            iArr[i2] = i3 ^ i5;
            iArr2[i2] = i4 ^ i5;
        }
    }

    public static void decode(byte[] bArr, int i, int[] iArr) {
        decode128(bArr, i, iArr, 0);
        decode128(bArr, i + 16, iArr, 5);
        iArr[9] = iArr[9] & M24;
    }

    private static void decode128(byte[] bArr, int i, int[] iArr, int i2) {
        int decode32 = decode32(bArr, i + 0);
        int decode322 = decode32(bArr, i + 4);
        int decode323 = decode32(bArr, i + 8);
        int decode324 = decode32(bArr, i + 12);
        iArr[i2 + 0] = decode32 & M26;
        iArr[i2 + 1] = ((decode32 >>> 26) | (decode322 << 6)) & M26;
        iArr[i2 + 2] = ((decode323 << 12) | (decode322 >>> 20)) & M25;
        iArr[i2 + 3] = ((decode324 << 19) | (decode323 >>> 13)) & M26;
        iArr[i2 + 4] = decode324 >>> 7;
    }

    private static int decode32(byte[] bArr, int i) {
        i++;
        i++;
        return (bArr[i + 1] << 24) | (((bArr[i] & 255) | ((bArr[i] & 255) << 8)) | ((bArr[i] & 255) << 16));
    }

    public static void encode(int[] iArr, byte[] bArr, int i) {
        encode128(iArr, 0, bArr, i);
        encode128(iArr, 5, bArr, i + 16);
    }

    private static void encode128(int[] iArr, int i, byte[] bArr, int i2) {
        int i3 = iArr[i + 0];
        int i4 = iArr[i + 1];
        int i5 = iArr[i + 2];
        int i6 = iArr[i + 3];
        int i7 = iArr[i + 4];
        encode32((i4 << 26) | i3, bArr, i2 + 0);
        encode32((i4 >>> 6) | (i5 << 20), bArr, i2 + 4);
        encode32((i5 >>> 12) | (i6 << 13), bArr, i2 + 8);
        encode32((i7 << 7) | (i6 >>> 19), bArr, i2 + 12);
    }

    private static void encode32(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        i2++;
        bArr[i2] = (byte) (i >>> 8);
        i2++;
        bArr[i2] = (byte) (i >>> 16);
        bArr[i2 + 1] = (byte) (i >>> 24);
    }

    public static void inv(int[] iArr, int[] iArr2) {
        int[] create = create();
        sqr(iArr, create);
        mul(iArr, create, create);
        int[] create2 = create();
        sqr(create, create2);
        mul(iArr, create2, create2);
        sqr(create2, 2, create2);
        mul(create, create2, create2);
        int[] create3 = create();
        sqr(create2, 5, create3);
        mul(create2, create3, create3);
        int[] create4 = create();
        sqr(create3, 5, create4);
        mul(create2, create4, create4);
        sqr(create4, 10, create2);
        mul(create3, create2, create2);
        sqr(create2, 25, create3);
        mul(create2, create3, create3);
        sqr(create3, 25, create4);
        mul(create2, create4, create4);
        sqr(create4, 50, create2);
        mul(create3, create2, create2);
        sqr(create2, 125, create3);
        mul(create2, create3, create3);
        sqr(create3, 2, create2);
        mul(create2, iArr, create2);
        sqr(create2, 3, create2);
        mul(create2, create, iArr2);
    }

    public static void mul(int[] iArr, int i, int[] iArr2) {
        int i2 = iArr[0];
        int i3 = iArr[1];
        int i4 = iArr[2];
        int i5 = iArr[3];
        int i6 = iArr[4];
        int i7 = iArr[5];
        int i8 = iArr[6];
        int i9 = iArr[7];
        int i10 = iArr[8];
        int i11 = i8;
        int i12 = i5;
        long j = (long) i;
        long j2 = ((long) i4) * j;
        i4 = ((int) j2) & M25;
        j2 >>= 25;
        int i13 = i3;
        long j3 = ((long) i6) * j;
        j3 >>= 25;
        int i14 = i7;
        long j4 = ((long) i9) * j;
        j4 >>= 25;
        long j5 = ((long) iArr[9]) * j;
        int i15 = ((int) j5) & M25;
        int i16 = ((int) j4) & M25;
        int i17 = ((int) j3) & M25;
        j5 = ((j5 >> 25) * 38) + (((long) i2) * j);
        iArr2[0] = ((int) j5) & M26;
        j5 >>= 26;
        j3 += ((long) i14) * j;
        iArr2[5] = ((int) j3) & M26;
        j3 >>= 26;
        j5 += ((long) i13) * j;
        iArr2[1] = ((int) j5) & M26;
        j5 >>= 26;
        j2 += ((long) i12) * j;
        iArr2[3] = ((int) j2) & M26;
        j2 >>= 26;
        j3 += ((long) i11) * j;
        iArr2[6] = ((int) j3) & M26;
        j3 >>= 26;
        j4 += ((long) i10) * j;
        iArr2[8] = ((int) j4) & M26;
        j = j4 >> 26;
        iArr2[2] = i4 + ((int) j5);
        iArr2[4] = i17 + ((int) j2);
        iArr2[7] = i16 + ((int) j3);
        iArr2[9] = i15 + ((int) j);
    }

    public static void mul(int[] iArr, int[] iArr2, int[] iArr3) {
        int i = iArr[0];
        int i2 = iArr2[0];
        int i3 = iArr[1];
        int i4 = iArr2[1];
        int i5 = iArr[2];
        int i6 = iArr2[2];
        int i7 = iArr[3];
        int i8 = iArr2[3];
        int i9 = iArr[4];
        int i10 = iArr2[4];
        int i11 = iArr[5];
        int i12 = iArr2[5];
        int i13 = iArr[6];
        int i14 = iArr2[6];
        int i15 = i13;
        int i16 = iArr[7];
        int i17 = iArr2[7];
        int i18 = iArr[8];
        i13 = iArr2[8];
        int i19 = iArr[9];
        long j = (long) i;
        int i20 = i14;
        int i21 = i12;
        long j2 = (long) i2;
        long j3 = j * j2;
        int i22 = i;
        int i23 = i2;
        long j4 = (long) i4;
        int i24 = i4;
        long j5 = (long) i3;
        long j6 = (j * j4) + (j5 * j2);
        int i25 = i13;
        int i26 = i3;
        long j7 = (long) i6;
        int i27 = i6;
        long j8 = (long) i5;
        long j9 = ((j * j7) + (j5 * j4)) + (j8 * j2);
        long j10 = j4;
        j4 = (long) i8;
        int i28 = i8;
        long j11 = (long) i7;
        long j12 = (((j5 * j7) + (j8 * j4)) << 1) + ((j * j4) + (j11 * j2));
        long j13 = j7;
        int i29 = i7;
        i7 = i10;
        j7 = (long) i7;
        int i30 = i7;
        long j14 = j11;
        i7 = i9;
        j11 = (long) i7;
        long j15 = ((j8 * j7) << 1) + ((((j * j7) + (j5 * j4)) + (j11 * j10)) + (j2 * j11));
        long j16 = ((((j5 * j7) + (j8 * j4)) + (j14 * j13)) + (j11 * j10)) << 1;
        j5 = (((j8 * j7) + (j11 * j13)) << 1) + (j14 * j4);
        long j17 = (j11 * j7) << 1;
        int i31 = i11;
        long j18 = (long) i31;
        i3 = i21;
        j11 = (long) i3;
        j10 = j18 * j11;
        long j19 = j17;
        int i32 = i7;
        i7 = i20;
        j17 = (long) i7;
        int i33 = i5;
        i5 = i15;
        j8 = (long) i5;
        long j20 = (j18 * j17) + (j8 * j11);
        int i34 = i3;
        int i35 = i7;
        i7 = i17;
        long j21 = (long) i7;
        int i36 = i31;
        i10 = i16;
        j = (long) i10;
        long j22 = ((j18 * j21) + (j8 * j17)) + (j * j11);
        int i37 = i10;
        i14 = i25;
        long j23 = (long) i14;
        int i38 = i14;
        long j24 = j17;
        i14 = i18;
        j17 = (long) i14;
        long j25 = (((j8 * j21) + (j * j17)) << 1) + ((j18 * j23) + (j17 * j11));
        int i39 = i14;
        long j26 = j21;
        i14 = iArr2[9];
        j21 = (long) i14;
        int i40 = i14;
        long j27 = j17;
        i14 = i19;
        j17 = (long) i14;
        long j28 = ((j * j21) << 1) + ((((j18 * j21) + (j8 * j23)) + (j17 * j24)) + (j11 * j17));
        j8 = (((j8 * j21) + (j * j23)) + (j27 * j26)) + (j17 * j24);
        j3 -= j8 * 76;
        j6 -= ((((j * j21) + (j17 * j26)) << 1) + (j27 * j23)) * 38;
        j9 -= ((j27 * j21) + (j23 * j17)) * 38;
        j12 -= (j17 * j21) * 76;
        j18 = j19 - j25;
        long j29 = ((j14 * j7) + (j4 * j11)) - j22;
        long j30 = j5 - j20;
        j17 = (long) (i22 + i36);
        j23 = (long) (i23 + i34);
        j25 = j17 * j23;
        long j31 = j16 - j10;
        j = (long) (i24 + i35);
        j11 = (long) (i26 + i5);
        long j32 = (j17 * j) + (j11 * j23);
        long j33 = j18;
        j18 = (long) (i27 + i7);
        j21 = (long) (i33 + i37);
        j20 = ((j17 * j18) + (j11 * j)) + (j21 * j23);
        j8 = (long) (i28 + i38);
        long j34 = j;
        j = (long) (i29 + i39);
        long j35 = (((j11 * j18) + (j21 * j)) << 1) + ((j17 * j8) + (j * j23));
        long j36 = j18;
        j18 = (long) (i30 + i40);
        long j37 = j;
        j = (long) (i32 + i14);
        long j38 = ((j21 * j18) << 1) + ((((j17 * j18) + (j11 * j8)) + (j * j34)) + (j23 * j));
        j21 = (((j21 * j18) + (j * j36)) << 1) + (j37 * j8);
        j23 = (j37 * j18) + (j8 * j);
        j = (j * j18) << 1;
        j18 = j33 + (j35 - j12);
        i14 = ((int) j18) & M26;
        j18 = (j18 >> 26) + ((j38 - j15) - j28);
        i7 = ((int) j18) & M25;
        j18 = j3 + ((((j18 >> 25) + (((((j11 * j18) + (j21 * j8)) + (j37 * j36)) + (j * j34)) << 1)) - j31) * 38);
        iArr3[0] = ((int) j18) & M26;
        j18 = (j18 >> 26) + (j6 + ((j21 - j30) * 38));
        iArr3[1] = ((int) j18) & M26;
        j18 = (j18 >> 26) + (j9 + ((j23 - j29) * 38));
        iArr3[2] = ((int) j18) & M25;
        j18 = (j18 >> 25) + (j12 + ((j - j33) * 38));
        iArr3[3] = ((int) j18) & M26;
        j = (j18 >> 26) + (j15 + (j28 * 38));
        iArr3[4] = ((int) j) & M25;
        j = (j >> 25) + (j31 + (j25 - j3));
        iArr3[5] = ((int) j) & M26;
        j = (j >> 26) + (j30 + (j32 - j6));
        iArr3[6] = ((int) j) & M26;
        j = (j >> 26) + (j29 + (j20 - j9));
        iArr3[7] = ((int) j) & M25;
        j = (j >> 25) + ((long) i14);
        iArr3[8] = ((int) j) & M26;
        iArr3[9] = i7 + ((int) (j >> 26));
    }

    public static void normalize(int[] iArr) {
        int i = (iArr[9] >>> 23) & 1;
        reduce(iArr, i);
        reduce(iArr, -i);
    }

    private static void reduce(int[] iArr, int i) {
        int i2 = iArr[9];
        int i3 = M24 & i2;
        i2 = (((i2 >> 24) + i) * 19) + iArr[0];
        iArr[0] = i2 & M26;
        i = (i2 >> 26) + iArr[1];
        iArr[1] = i & M26;
        i = (i >> 26) + iArr[2];
        iArr[2] = i & M25;
        i = (i >> 25) + iArr[3];
        iArr[3] = i & M26;
        i = (i >> 26) + iArr[4];
        iArr[4] = i & M25;
        i = (i >> 25) + iArr[5];
        iArr[5] = i & M26;
        i = (i >> 26) + iArr[6];
        iArr[6] = i & M26;
        i = (i >> 26) + iArr[7];
        iArr[7] = M25 & i;
        i = (i >> 25) + iArr[8];
        iArr[8] = M26 & i;
        iArr[9] = (i >> 26) + i3;
    }

    public static void sqr(int[] iArr, int i, int[] iArr2) {
        sqr(iArr, iArr2);
        while (true) {
            i--;
            if (i > 0) {
                sqr(iArr2, iArr2);
            } else {
                return;
            }
        }
    }

    public static void sqr(int[] iArr, int[] iArr2) {
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        int i4 = iArr[3];
        int i5 = iArr[4];
        int i6 = iArr[5];
        int i7 = iArr[6];
        int i8 = iArr[7];
        int i9 = iArr[8];
        int i10 = iArr[9];
        long j = (long) i;
        long j2 = j * j;
        int i11 = i9;
        long j3 = (long) (i2 * 2);
        long j4 = j * j3;
        int i12 = i8;
        int i13 = i7;
        long j5 = (long) (i3 * 2);
        long j6 = (long) i2;
        long j7 = (j * j5) + (j6 * j6);
        int i14 = i2;
        long j8 = (long) (i4 * 2);
        long j9 = (j3 * j5) + (j * j8);
        int i15 = i;
        long j10 = (long) (i5 * 2);
        long j11 = ((((long) i3) * j5) + (j * j10)) + (j6 * j8);
        j3 = (j3 * j10) + (j8 * j5);
        int i16 = i4;
        j8 = (long) i16;
        j5 = (j5 * j10) + (j8 * j8);
        j8 *= j10;
        int i17 = i5;
        long j12 = j11;
        int i18 = i17;
        long j13 = ((long) i17) * j10;
        i17 = i6;
        j6 = (long) i17;
        long j14 = j6 * j6;
        int i19 = i16;
        j = (long) (i13 * 2);
        long j15 = j6 * j;
        j10 = (long) (i12 * 2);
        long j16 = j8;
        int i20 = i3;
        i3 = i13;
        j8 = (long) i3;
        long j17 = (j6 * j10) + (j8 * j8);
        int i21 = i3;
        long j18 = (long) (i11 * 2);
        long j19 = j3;
        long j20 = j5;
        i8 = i12;
        int i22 = i8;
        j5 = (long) (i10 * 2);
        j3 = ((((long) i8) * j10) + (j6 * j5)) + (j8 * j18);
        i2 = i11;
        long j21 = (long) i2;
        j2 -= ((j * j5) + (j18 * j10)) * 38;
        j4 -= ((j10 * j5) + (j21 * j21)) * 38;
        j7 -= (j21 * j5) * 38;
        j9 -= (((long) i10) * j5) * 38;
        i4 = i14 + i21;
        int i23 = i20 + i22;
        i2 = i19 + i2;
        i5 = i18 + i10;
        long j22 = j16 - j17;
        long j23 = j20 - j15;
        long j24 = j19 - j14;
        j = (long) (i15 + i17);
        long j25 = j * j;
        long j26 = j3;
        j3 = (long) (i4 * 2);
        long j27 = j * j3;
        long j28 = j13 - ((j * j10) + (j6 * j18));
        j21 = (long) (i23 * 2);
        j11 = (long) i4;
        long j29 = (j * j21) + (j11 * j11);
        j10 = (long) (i2 * 2);
        j5 = (long) (i5 * 2);
        j6 = ((((long) i23) * j21) + (j * j5)) + (j11 * j10);
        long j30 = (long) i2;
        j21 = (j21 * j5) + (j30 * j30);
        j30 *= j5;
        long j31 = ((long) i5) * j5;
        long j32 = j28 + (((j3 * j21) + (j * j10)) - j9);
        int i24 = ((int) j32) & M26;
        j32 = (j32 >> 26) + ((j6 - j12) - j26);
        i23 = ((int) j32) & M25;
        j32 = j2 + ((((j32 >> 25) + ((j3 * j5) + (j10 * j21))) - j24) * 38);
        iArr2[0] = ((int) j32) & M26;
        j32 = (j32 >> 26) + (j4 + ((j21 - j23) * 38));
        iArr2[1] = ((int) j32) & M26;
        j32 = (j32 >> 26) + (j7 + ((j30 - j22) * 38));
        iArr2[2] = ((int) j32) & M25;
        j32 = (j32 >> 25) + (j9 + ((j31 - j28) * 38));
        iArr2[3] = ((int) j32) & M26;
        j30 = (j32 >> 26) + (j12 + (38 * j26));
        iArr2[4] = ((int) j30) & M25;
        j30 = (j30 >> 25) + (j24 + (j25 - j2));
        iArr2[5] = ((int) j30) & M26;
        j30 = (j30 >> 26) + (j23 + (j27 - j4));
        iArr2[6] = ((int) j30) & M26;
        j30 = (j30 >> 26) + (j22 + (j29 - j7));
        iArr2[7] = ((int) j30) & M25;
        j30 = (j30 >> 25) + ((long) i24);
        iArr2[8] = ((int) j30) & M26;
        iArr2[9] = i23 + ((int) (j30 >> 26));
    }

    public static void sub(int[] iArr, int[] iArr2, int[] iArr3) {
        for (int i = 0; i < 10; i++) {
            iArr3[i] = iArr[i] - iArr2[i];
        }
    }
}
