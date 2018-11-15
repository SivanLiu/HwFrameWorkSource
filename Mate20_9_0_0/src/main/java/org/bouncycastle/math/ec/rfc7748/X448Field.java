package org.bouncycastle.math.ec.rfc7748;

public abstract class X448Field {
    private static final int M28 = 268435455;
    public static final int SIZE = 16;

    private X448Field() {
    }

    public static void add(int[] iArr, int[] iArr2, int[] iArr3) {
        for (int i = 0; i < 16; i++) {
            iArr3[i] = iArr[i] + iArr2[i];
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
        int i11 = iArr[10];
        int i12 = iArr[11];
        int i13 = iArr[12];
        int i14 = iArr[13];
        i3 += i2 >>> 28;
        i7 += i6 >>> 28;
        i11 += i10 >>> 28;
        int i15 = iArr[14] + (i14 >>> 28);
        i4 += i3 >>> 28;
        i3 &= M28;
        i8 += i7 >>> 28;
        i7 &= M28;
        i12 += i11 >>> 28;
        i11 &= M28;
        int i16 = iArr[15] + (i15 >>> 28);
        i15 &= M28;
        int i17 = i16 >>> 28;
        i16 &= M28;
        i += i17;
        i5 += i4 >>> 28;
        i4 &= M28;
        i9 = (i9 + i17) + (i8 >>> 28);
        i8 &= M28;
        i13 += i12 >>> 28;
        i12 &= M28;
        i2 = (i2 & M28) + (i >>> 28);
        i6 = (i6 & M28) + (i5 >>> 28);
        i5 &= M28;
        i10 = (i10 & M28) + (i9 >>> 28);
        i9 &= M28;
        i14 = (i14 & M28) + (i13 >>> 28);
        i13 &= M28;
        iArr[0] = i & M28;
        iArr[1] = i2;
        iArr[2] = i3;
        iArr[3] = i4;
        iArr[4] = i5;
        iArr[5] = i6;
        iArr[6] = i7;
        iArr[7] = i8;
        iArr[8] = i9;
        iArr[9] = i10;
        iArr[10] = i11;
        iArr[11] = i12;
        iArr[12] = i13;
        iArr[13] = i14;
        iArr[14] = i15;
        iArr[15] = i16;
    }

    public static void copy(int[] iArr, int i, int[] iArr2, int i2) {
        for (int i3 = 0; i3 < 16; i3++) {
            iArr2[i2 + i3] = iArr[i + i3];
        }
    }

    public static int[] create() {
        return new int[16];
    }

    public static void cswap(int i, int[] iArr, int[] iArr2) {
        i = 0 - i;
        for (int i2 = 0; i2 < 16; i2++) {
            int i3 = iArr[i2];
            int i4 = iArr2[i2];
            int i5 = (i3 ^ i4) & i;
            iArr[i2] = i3 ^ i5;
            iArr2[i2] = i4 ^ i5;
        }
    }

    public static void decode(byte[] bArr, int i, int[] iArr) {
        decode56(bArr, i, iArr, 0);
        decode56(bArr, i + 7, iArr, 2);
        decode56(bArr, i + 14, iArr, 4);
        decode56(bArr, i + 21, iArr, 6);
        decode56(bArr, i + 28, iArr, 8);
        decode56(bArr, i + 35, iArr, 10);
        decode56(bArr, i + 42, iArr, 12);
        decode56(bArr, i + 49, iArr, 14);
    }

    private static int decode24(byte[] bArr, int i) {
        i++;
        return ((bArr[i + 1] & 255) << 16) | ((bArr[i] & 255) | ((bArr[i] & 255) << 8));
    }

    private static int decode32(byte[] bArr, int i) {
        i++;
        i++;
        return (bArr[i + 1] << 24) | (((bArr[i] & 255) | ((bArr[i] & 255) << 8)) | ((bArr[i] & 255) << 16));
    }

    private static void decode56(byte[] bArr, int i, int[] iArr, int i2) {
        int decode32 = decode32(bArr, i);
        int decode24 = decode24(bArr, i + 4);
        iArr[i2] = M28 & decode32;
        iArr[i2 + 1] = (decode24 << 4) | (decode32 >>> 28);
    }

    public static void encode(int[] iArr, byte[] bArr, int i) {
        encode56(iArr, 0, bArr, i);
        encode56(iArr, 2, bArr, i + 7);
        encode56(iArr, 4, bArr, i + 14);
        encode56(iArr, 6, bArr, i + 21);
        encode56(iArr, 8, bArr, i + 28);
        encode56(iArr, 10, bArr, i + 35);
        encode56(iArr, 12, bArr, i + 42);
        encode56(iArr, 14, bArr, i + 49);
    }

    private static void encode24(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        i2++;
        bArr[i2] = (byte) (i >>> 8);
        bArr[i2 + 1] = (byte) (i >>> 16);
    }

    private static void encode32(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        i2++;
        bArr[i2] = (byte) (i >>> 8);
        i2++;
        bArr[i2] = (byte) (i >>> 16);
        bArr[i2 + 1] = (byte) (i >>> 24);
    }

    private static void encode56(int[] iArr, int i, byte[] bArr, int i2) {
        int i3 = iArr[i];
        int i4 = iArr[i + 1];
        encode32((i4 << 28) | i3, bArr, i2);
        encode24(i4 >>> 4, bArr, i2 + 4);
    }

    public static void inv(int[] iArr, int[] iArr2) {
        int[] create = create();
        sqr(iArr, create);
        mul(iArr, create, create);
        int[] create2 = create();
        sqr(create, create2);
        mul(iArr, create2, create2);
        create = create();
        sqr(create2, 3, create);
        mul(create2, create, create);
        int[] create3 = create();
        sqr(create, 3, create3);
        mul(create2, create3, create3);
        create = create();
        sqr(create3, 9, create);
        mul(create3, create, create);
        create2 = create();
        sqr(create, create2);
        mul(iArr, create2, create2);
        int[] create4 = create();
        sqr(create2, 18, create4);
        mul(create, create4, create4);
        create = create();
        sqr(create4, 37, create);
        mul(create4, create, create);
        create3 = create();
        sqr(create, 37, create3);
        mul(create4, create3, create3);
        create = create();
        sqr(create3, 111, create);
        mul(create3, create, create);
        create2 = create();
        sqr(create, create2);
        mul(iArr, create2, create2);
        create4 = create();
        sqr(create2, 223, create4);
        mul(create4, create, create4);
        sqr(create4, 2, create4);
        mul(create4, iArr, iArr2);
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
        int i11 = iArr[9];
        int i12 = iArr[10];
        int i13 = iArr[11];
        int i14 = iArr[12];
        int i15 = iArr[13];
        int i16 = i2;
        i2 = iArr[14];
        int i17 = i10;
        int i18 = i6;
        int i19 = i9;
        int i20 = i14;
        long j = (long) i;
        long j2 = ((long) i3) * j;
        int i21 = ((int) j2) & M28;
        long j3 = ((long) i7) * j;
        int i22 = iArr[15];
        int i23 = ((int) j3) & M28;
        j3 >>>= 28;
        int i24 = i13;
        int i25 = i5;
        long j4 = ((long) i11) * j;
        j4 >>>= 28;
        int i26 = ((int) j4) & M28;
        int i27 = i2;
        long j5 = ((long) i15) * j;
        j5 >>>= 28;
        int i28 = ((int) j5) & M28;
        j2 = (j2 >>> 28) + (((long) i4) * j);
        iArr2[2] = ((int) j2) & M28;
        j2 >>>= 28;
        j3 += ((long) i8) * j;
        iArr2[6] = ((int) j3) & M28;
        long j6 = j3 >>> 28;
        j4 += ((long) i12) * j;
        iArr2[10] = ((int) j4) & M28;
        j4 >>>= 28;
        j5 += ((long) i27) * j;
        iArr2[14] = ((int) j5) & M28;
        j5 >>>= 28;
        j2 += ((long) i25) * j;
        iArr2[3] = ((int) j2) & M28;
        j2 >>>= 28;
        j6 += ((long) i19) * j;
        iArr2[7] = ((int) j6) & M28;
        j6 >>>= 28;
        j4 += ((long) i24) * j;
        iArr2[11] = ((int) j4) & M28;
        j4 >>>= 28;
        j5 += ((long) i22) * j;
        iArr2[15] = ((int) j5) & M28;
        j5 >>>= 28;
        j6 += j5;
        j2 += ((long) i18) * j;
        iArr2[4] = ((int) j2) & M28;
        j2 >>>= 28;
        j6 += ((long) i17) * j;
        iArr2[8] = ((int) j6) & M28;
        j6 >>>= 28;
        j4 += ((long) i20) * j;
        iArr2[12] = ((int) j4) & M28;
        j4 >>>= 28;
        j5 += ((long) i16) * j;
        iArr2[0] = ((int) j5) & M28;
        iArr2[1] = i21 + ((int) (j5 >>> 28));
        iArr2[5] = i23 + ((int) j2);
        iArr2[9] = i26 + ((int) j6);
        iArr2[13] = i28 + ((int) j4);
    }

    public static void mul(int[] iArr, int[] iArr2, int[] iArr3) {
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
        int i11 = iArr[10];
        int i12 = iArr[11];
        int i13 = iArr[12];
        int i14 = iArr[13];
        int i15 = i8;
        i8 = iArr[14];
        int i16 = iArr[15];
        int i17 = iArr2[0];
        int i18 = iArr2[1];
        int i19 = iArr2[2];
        int i20 = iArr2[3];
        int i21 = iArr2[4];
        int i22 = iArr2[5];
        int i23 = iArr2[6];
        int i24 = iArr2[7];
        int i25 = iArr2[8];
        int i26 = iArr2[9];
        int i27 = iArr2[10];
        int i28 = iArr2[11];
        int i29 = iArr2[12];
        int i30 = iArr2[13];
        int i31 = iArr2[14];
        int i32 = iArr2[15];
        int i33 = i + i9;
        int i34 = i2 + i10;
        int i35 = i3 + i11;
        int i36 = i4 + i12;
        int i37 = i5 + i13;
        int i38 = i6 + i14;
        int i39 = i7 + i8;
        int i40 = i15 + i16;
        int i41 = i17 + i25;
        int i42 = i19 + i27;
        int i43 = i21 + i29;
        int i44 = i23 + i31;
        int i45 = i31;
        long j = (long) i;
        long j2 = (long) i17;
        long j3 = j * j2;
        long j4 = j;
        j = (long) i15;
        long j5 = j2;
        j2 = (long) i18;
        long j6 = j * j2;
        long j7 = j;
        j = (long) i7;
        long j8 = j2;
        j2 = (long) i19;
        long j9 = (long) i6;
        long j10 = j;
        j = (long) i20;
        long j11 = j9;
        long j12 = (long) i5;
        long j13 = j;
        j = (long) i21;
        long j14 = j12;
        j12 = (long) i4;
        long j15 = j;
        j = (long) i22;
        long j16 = j12;
        j12 = (long) i3;
        long j17 = j;
        j = (long) i23;
        long j18 = j12;
        j12 = (long) i2;
        long j19 = j;
        j = (long) i24;
        j6 = (((((j6 + (j * j2)) + (j9 * j)) + (j12 * j)) + (j12 * j)) + (j12 * j)) + (j12 * j);
        long j20 = (long) i9;
        long j21 = j;
        j = (long) i25;
        long j22 = j20 * j;
        long j23 = j20;
        j20 = (long) i16;
        long j24 = j;
        j = (long) i26;
        long j25 = j20 * j;
        long j26 = (long) i8;
        long j27 = j20;
        j20 = (long) i27;
        long j28 = j26;
        j26 = (long) i14;
        long j29 = j20;
        j20 = (long) i28;
        long j30 = (long) i13;
        long j31 = j26;
        j26 = (long) i29;
        long j32 = j30;
        j30 = (long) i12;
        long j33 = j26;
        j26 = (long) i30;
        long j34 = (long) i11;
        long j35 = j30;
        j30 = (long) i45;
        long j36 = (long) i10;
        long j37 = j34;
        j34 = (long) i32;
        j25 = (((((j25 + (j26 * j20)) + (j26 * j20)) + (j30 * j26)) + (j30 * j26)) + (j34 * j30)) + (j36 * j34);
        long j38 = j34;
        j34 = (long) i33;
        long j39 = j30;
        j30 = (long) i41;
        long j40 = j34 * j30;
        long j41 = j34;
        j34 = (long) i40;
        long j42 = j30;
        j30 = (long) (i18 + i26);
        long j43 = j34 * j30;
        long j44 = j34;
        j34 = (long) i39;
        long j45 = j30;
        j30 = (long) i42;
        long j46 = j34;
        j34 = (long) i38;
        long j47 = j30;
        j30 = (long) (i20 + i28);
        long j48 = j34;
        j34 = (long) i37;
        long j49 = j30;
        j30 = (long) i43;
        long j50 = j34;
        j34 = (long) i36;
        long j51 = j30;
        j30 = (long) (i22 + i30);
        long j52 = j34;
        j34 = (long) i35;
        long j53 = j30;
        j30 = (long) i44;
        long j54 = j34;
        j34 = (long) i34;
        long j55 = j30;
        j30 = (long) (i24 + i32);
        j43 = (((((j43 + (j34 * j30)) + (j34 * j30)) + (j34 * j30)) + (j34 * j30)) + (j34 * j30)) + (j34 * j30);
        long j56 = j30;
        j30 = ((j3 + j22) + j43) - j6;
        long j57 = j34;
        long j58 = j30 >>> 28;
        j30 = ((j25 + j40) - j3) + j43;
        int i46 = ((int) j30) & M28;
        j25 = (j12 * j5) + (j4 * j8);
        long j59 = (((((j44 * j47) + (j46 * j49)) + (j48 * j51)) + (j50 * j53)) + (j52 * j55)) + (j54 * j56);
        long j60 = j26;
        j26 = j58 + (((j25 + ((j36 * j24) + (j23 * j))) + j59) - ((((((j7 * j2) + (j10 * j13)) + (j11 * j15)) + (j14 * j17)) + (j16 * j19)) + (j18 * j21)));
        int i47 = ((int) j30) & M28;
        j30 = (j30 >>> 28) + (((((((((j27 * j29) + (j28 * j20)) + (j31 * j33)) + (j32 * j26)) + (j35 * j39)) + (j37 * j38)) + ((j57 * j42) + (j41 * j45))) - j25) + j59);
        int i48 = ((int) j26) & M28;
        j25 = ((j18 * j5) + (j12 * j8)) + (j4 * j2);
        j59 = ((((j44 * j49) + (j46 * j51)) + (j48 * j53)) + (j50 * j55)) + (j52 * j56);
        j26 = (j26 >>> 28) + (((j25 + (((j37 * j24) + (j36 * j)) + (j23 * j29))) + j59) - (((((j7 * j13) + (j10 * j15)) + (j11 * j17)) + (j14 * j19)) + (j16 * j21)));
        int i49 = ((int) j30) & M28;
        j30 = (j30 >>> 28) + ((((((((j27 * j20) + (j28 * j33)) + (j31 * j60)) + (j32 * j39)) + (j35 * j38)) + (((j54 * j42) + (j57 * j45)) + (j41 * j47))) - j25) + j59);
        int i50 = ((int) j26) & M28;
        j25 = (((j16 * j5) + (j18 * j8)) + (j12 * j2)) + (j4 * j13);
        j59 = (((j44 * j51) + (j46 * j53)) + (j48 * j55)) + (j50 * j56);
        j26 = (j26 >>> 28) + (((j25 + ((((j35 * j24) + (j37 * j)) + (j36 * j29)) + (j23 * j20))) + j59) - ((((j7 * j15) + (j10 * j17)) + (j11 * j19)) + (j14 * j21)));
        int i51 = ((int) j30) & M28;
        j30 = (j30 >>> 28) + (((((((j27 * j33) + (j28 * j60)) + (j31 * j39)) + (j32 * j38)) + ((((j52 * j42) + (j54 * j45)) + (j57 * j47)) + (j41 * j49))) - j25) + j59);
        int i52 = ((int) j26) & M28;
        j25 = ((((j14 * j5) + (j16 * j8)) + (j18 * j2)) + (j12 * j13)) + (j4 * j15);
        j59 = ((j44 * j53) + (j46 * j55)) + (j48 * j56);
        j26 = (j26 >>> 28) + (((j25 + (((((j32 * j24) + (j35 * j)) + (j37 * j29)) + (j36 * j20)) + (j23 * j33))) + j59) - (((j7 * j17) + (j10 * j19)) + (j11 * j21)));
        int i53 = ((int) j30) & M28;
        j30 = (j30 >>> 28) + ((((((j27 * j60) + (j28 * j39)) + (j31 * j38)) + (((((j50 * j42) + (j52 * j45)) + (j54 * j47)) + (j57 * j49)) + (j41 * j51))) - j25) + j59);
        int i54 = ((int) j26) & M28;
        j25 = (((((j11 * j5) + (j14 * j8)) + (j16 * j2)) + (j18 * j13)) + (j12 * j15)) + (j4 * j17);
        j59 = (j44 * j55) + (j46 * j56);
        j26 = (j26 >>> 28) + (((j25 + ((((((j31 * j24) + (j32 * j)) + (j35 * j29)) + (j37 * j20)) + (j36 * j33)) + (j23 * j60))) + j59) - ((j7 * j19) + (j10 * j21)));
        int i55 = ((int) j30) & M28;
        j30 = (j30 >>> 28) + (((((j27 * j39) + (j28 * j38)) + ((((((j48 * j42) + (j50 * j45)) + (j52 * j47)) + (j54 * j49)) + (j57 * j51)) + (j41 * j53))) - j25) + j59);
        int i56 = ((int) j26) & M28;
        j25 = ((((((j10 * j5) + (j11 * j8)) + (j14 * j2)) + (j16 * j13)) + (j18 * j15)) + (j12 * j17)) + (j4 * j19);
        j59 = j44 * j56;
        j26 = (j26 >>> 28) + (((j25 + (((((((j28 * j24) + (j31 * j)) + (j32 * j29)) + (j35 * j20)) + (j37 * j33)) + (j36 * j60)) + (j23 * j39))) + j59) - (j7 * j21));
        int i57 = ((int) j30) & M28;
        j30 = (j30 >>> 28) + ((((j27 * j38) + (((((((j46 * j42) + (j48 * j45)) + (j50 * j47)) + (j52 * j49)) + (j54 * j51)) + (j57 * j53)) + (j41 * j55))) - j25) + j59);
        int i58 = ((int) j26) & M28;
        i2 = ((int) j30) & M28;
        j25 = (((((((j7 * j5) + (j10 * j8)) + (j2 * j11)) + (j14 * j13)) + (j16 * j15)) + (j18 * j17)) + (j12 * j19)) + (j4 * j21);
        j26 = (j26 >>> 28) + (j25 + ((((((((j27 * j24) + (j * j28)) + (j31 * j29)) + (j32 * j20)) + (j35 * j33)) + (j37 * j60)) + (j36 * j39)) + (j23 * j38)));
        i = ((int) j26) & M28;
        j30 = (j30 >>> 28) + (((((((((j44 * j42) + (j46 * j45)) + (j48 * j47)) + (j50 * j49)) + (j52 * j51)) + (j54 * j53)) + (j57 * j55)) + (j41 * j56)) - j25);
        i31 = ((int) j30) & M28;
        long j61 = j30 >>> 28;
        j36 = ((j26 >>> 28) + j61) + ((long) i47);
        i13 = ((int) j36) & M28;
        j61 += (long) i46;
        i11 = i49 + ((int) (j36 >>> 28));
        int i59 = i48 + ((int) (j61 >>> 28));
        iArr3[0] = ((int) j61) & M28;
        iArr3[1] = i59;
        iArr3[2] = i50;
        iArr3[3] = i52;
        iArr3[4] = i54;
        iArr3[5] = i56;
        iArr3[6] = i58;
        iArr3[7] = i;
        iArr3[8] = i13;
        iArr3[9] = i11;
        iArr3[10] = i51;
        iArr3[11] = i53;
        iArr3[12] = i55;
        iArr3[13] = i57;
        iArr3[14] = i2;
        iArr3[15] = i31;
    }

    public static void normalize(int[] iArr) {
        reduce(iArr, 1);
        reduce(iArr, -1);
    }

    private static void reduce(int[] iArr, int i) {
        int i2 = iArr[15];
        int i3 = i2 & M28;
        i2 = (i2 >> 28) + i;
        iArr[8] = iArr[8] + i2;
        for (i = 0; i < 15; i++) {
            i2 += iArr[i];
            iArr[i] = i2 & M28;
            i2 >>= 28;
        }
        iArr[15] = i3 + i2;
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
        int i11 = iArr[10];
        int i12 = iArr[11];
        int i13 = iArr[12];
        int i14 = iArr[13];
        int i15 = iArr[14];
        int i16 = iArr[15];
        int i17 = i * 2;
        int i18 = i2 * 2;
        int i19 = i3 * 2;
        int i20 = i4 * 2;
        int i21 = i5 * 2;
        int i22 = i6 * 2;
        int i23 = i9 * 2;
        int i24 = i10 * 2;
        int i25 = i11 * 2;
        int i26 = i12 * 2;
        int i27 = i15 * 2;
        int i28 = i + i9;
        int i29 = i9;
        i9 = i2 + i10;
        int i30 = i10;
        i10 = i3 + i11;
        int i31 = i11;
        i11 = i4 + i12;
        int i32 = i12;
        i12 = i5 + i13;
        int i33 = i4;
        i4 = i6 + i14;
        int i34 = i3;
        i3 = i7 + i15;
        int i35 = i2;
        int i36 = i8 + i16;
        int i37 = i28 * 2;
        i2 = i9 * 2;
        int i38 = i9;
        i9 = i10 * 2;
        int i39 = i10;
        i10 = i11 * 2;
        int i40 = i11;
        int i41 = i12 * 2;
        int i42 = i4 * 2;
        int i43 = i3 * 2;
        long j = (long) i;
        long j2 = j * j;
        j = (long) i8;
        int i44 = i12;
        long j3 = (long) i18;
        long j4 = j * j3;
        long j5 = (long) i7;
        long j6 = j3;
        j3 = (long) i19;
        long j7 = j5;
        long j8 = (long) i6;
        long j9 = j3;
        j3 = (long) i20;
        long j10 = j8;
        j8 = (long) i5;
        j4 = ((j4 + (j5 * j3)) + (j8 * j3)) + (j8 * j8);
        long j11 = j8;
        j8 = (long) i29;
        long j12 = j3;
        j3 = (long) i16;
        long j13 = j;
        j = (long) i24;
        long j14 = j3 * j;
        long j15 = j;
        j = (long) i15;
        long j16 = j3;
        j3 = (long) i25;
        long j17 = (long) i14;
        long j18 = j;
        j = (long) i26;
        long j19 = (long) i13;
        long j20 = j19;
        j19 = (long) i28;
        long j21 = j17;
        long j22 = (long) i36;
        long j23 = j;
        j = (long) i2;
        long j24 = j22 * j;
        long j25 = j;
        j = (long) i3;
        long j26 = (long) i9;
        long j27 = j;
        j = (long) i4;
        long j28 = j22;
        j22 = (long) i10;
        long j29 = j;
        j = (long) i44;
        j24 = ((j24 + (j * j26)) + (j * j22)) + (j * j);
        j8 = ((j2 + (j8 * j8)) + j24) - j4;
        j19 = (((((j14 + (j * j3)) + (j17 * j)) + (j19 * j19)) + (j19 * j19)) - j2) + j24;
        long j30 = j3;
        long j31 = (long) i35;
        long j32 = j;
        long j33 = (long) i17;
        long j34 = j31 * j33;
        int i45 = ((int) j8) & M28;
        long j35 = j31;
        j31 = (long) i21;
        long j36 = ((j13 * j9) + (j7 * j12)) + (j10 * j31);
        long j37 = j31;
        j31 = (long) i30;
        long j38 = j33;
        j33 = (long) i23;
        j14 = j31 * j33;
        long j39 = j31;
        j31 = (long) (i13 * 2);
        long j40 = j31;
        j31 = (long) i38;
        long j41 = j33;
        j33 = (long) i37;
        long j42 = j26;
        j26 = (long) i41;
        long j43 = ((j28 * j26) + (j27 * j22)) + (j29 * j26);
        j8 = (j8 >>> 28) + (((j34 + j14) + j43) - j36);
        j19 = (j19 >>> 28) + ((((((j16 * j30) + (j18 * j23)) + (j21 * j31)) + (j31 * j33)) - j34) + j43);
        int i46 = ((int) j19) & M28;
        j34 = (long) i34;
        j36 = (j34 * j38) + (j35 * j35);
        int i47 = ((int) j8) & M28;
        long j44 = j34;
        j34 = (long) i31;
        long j45 = (j34 * j41) + (j39 * j39);
        long j46 = j34;
        j34 = (long) i39;
        j43 = (j34 * j33) + (j31 * j31);
        j31 = ((j28 * j22) + (j27 * j26)) + (j29 * j29);
        j8 = (j8 >>> 28) + (((j36 + j45) + j31) - (((j13 * j12) + (j7 * j37)) + (j10 * j10)));
        j19 = (j19 >>> 28) + ((((((j16 * j23) + (j18 * j40)) + (j21 * j21)) + j43) - j36) + j31);
        int i48 = ((int) j19) & M28;
        j31 = (long) i33;
        j36 = (j31 * j38) + (j44 * j6);
        long j47 = j22;
        int i49 = ((int) j8) & M28;
        j22 = (long) i22;
        j14 = (j13 * j37) + (j7 * j22);
        long j48 = j22;
        j22 = (long) i32;
        j45 = (j22 * j41) + (j46 * j15);
        long j49 = j22;
        j22 = (long) (i14 * 2);
        long j50 = (j16 * j40) + (j18 * j22);
        long j51 = j22;
        j22 = (long) i40;
        j43 = (j22 * j33) + (j34 * j25);
        long j52 = j34;
        j34 = (long) i42;
        j26 = (j26 * j28) + (j27 * j34);
        j8 = (j8 >>> 28) + (((j36 + j45) + j26) - j14);
        j19 = (j19 >>> 28) + (((j50 + j43) - j36) + j26);
        j36 = ((j11 * j38) + (j31 * j6)) + (j44 * j44);
        j34 = (j34 * j28) + (j27 * j27);
        j8 = (j8 >>> 28) + (((j36 + (((j20 * j41) + (j49 * j15)) + (j46 * j46))) + j34) - ((j13 * j48) + (j7 * j7)));
        j19 = (j19 >>> 28) + (((((j16 * j51) + (j18 * j18)) + (((j32 * j33) + (j22 * j25)) + (j52 * j52))) - j36) + j34);
        j36 = ((j10 * j38) + (j11 * j6)) + (j31 * j9);
        int i50 = ((int) j19) & M28;
        int i51 = ((int) j19) & M28;
        int i52 = ((int) j8) & M28;
        long j53 = j22;
        int i53 = ((int) j8) & M28;
        j22 = ((long) i43) * j28;
        j14 = (j36 + (((j21 * j41) + (j20 * j15)) + (j49 * j30))) + j22;
        j8 = (j8 >>> 28) + (j14 - (((long) (i7 * 2)) * j13));
        i14 = ((int) j8) & M28;
        j19 = (j19 >>> 28) + ((((((long) i27) * j16) + (((j29 * j33) + (j32 * j25)) + (j22 * j42))) - j36) + j22);
        i28 = ((int) j19) & M28;
        j34 = (((j7 * j38) + (j10 * j6)) + (j11 * j9)) + (j31 * j31);
        j45 = j28 * j28;
        j8 = (j8 >>> 28) + (((((((j18 * j41) + (j21 * j15)) + (j20 * j30)) + (j49 * j49)) + j34) + j45) - (j13 * j13));
        i15 = ((int) j8) & M28;
        j19 = (j19 >>> 28) + ((((j16 * j16) + ((((j27 * j33) + (j29 * j25)) + (j32 * j42)) + (j53 * j53))) - j34) + j45);
        i4 = ((int) j19) & M28;
        j31 = (((j13 * j38) + (j7 * j6)) + (j10 * j9)) + (j11 * j12);
        j26 = (j8 >>> 28) + (((((j16 * j41) + (j18 * j15)) + (j21 * j30)) + (j20 * j23)) + j31);
        i10 = ((int) j26) & M28;
        j19 = (j19 >>> 28) + (((((j33 * j28) + (j27 * j25)) + (j29 * j42)) + (j32 * j47)) - j31);
        i2 = ((int) j19) & M28;
        j19 >>>= 28;
        j26 = ((j26 >>> 28) + j19) + ((long) (((int) j19) & M28));
        i12 = ((int) j26) & M28;
        j19 += (long) i45;
        i3 = i46 + ((int) (j26 >>> 28));
        i = i47 + ((int) (j19 >>> 28));
        iArr2[0] = ((int) j19) & M28;
        iArr2[1] = i;
        iArr2[2] = i49;
        iArr2[3] = i53;
        iArr2[4] = i52;
        iArr2[5] = i14;
        iArr2[6] = i15;
        iArr2[7] = i10;
        iArr2[8] = i12;
        iArr2[9] = i3;
        iArr2[10] = i48;
        iArr2[11] = i51;
        iArr2[12] = i50;
        iArr2[13] = i28;
        iArr2[14] = i4;
        iArr2[15] = i2;
    }

    public static void sub(int[] iArr, int[] iArr2, int[] iArr3) {
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
        int i11 = iArr[10];
        int i12 = iArr[11];
        int i13 = iArr[12];
        int i14 = iArr[13];
        int i15 = iArr[14];
        int i16 = iArr[15];
        int i17 = iArr2[0];
        int i18 = iArr2[1];
        int i19 = iArr2[2];
        int i20 = iArr2[3];
        int i21 = iArr2[4];
        int i22 = iArr2[5];
        int i23 = iArr2[6];
        int i24 = iArr2[7];
        int i25 = iArr2[8];
        int i26 = iArr2[9];
        int i27 = iArr2[10];
        int i28 = iArr2[11];
        int i29 = iArr2[12];
        int i30 = iArr2[13];
        i = (i + 536870910) - i17;
        i2 = (i2 + 536870910) - i18;
        i6 = (i6 + 536870910) - i22;
        i10 = (i10 + 536870910) - i26;
        i14 = (i14 + 536870910) - i30;
        i3 = ((i3 + 536870910) - i19) + (i2 >>> 28);
        i7 = ((i7 + 536870910) - i23) + (i6 >>> 28);
        i11 = ((i11 + 536870910) - i27) + (i10 >>> 28);
        i15 = ((i15 + 536870910) - iArr2[14]) + (i14 >>> 28);
        i4 = ((i4 + 536870910) - i20) + (i3 >>> 28);
        i3 &= M28;
        i8 = ((i8 + 536870910) - i24) + (i7 >>> 28);
        i7 &= M28;
        i12 = ((i12 + 536870910) - i28) + (i11 >>> 28);
        i11 &= M28;
        i16 = ((i16 + 536870910) - iArr2[15]) + (i15 >>> 28);
        i15 &= M28;
        i17 = i16 >>> 28;
        i16 &= M28;
        i += i17;
        i5 = ((i5 + 536870910) - i21) + (i4 >>> 28);
        i4 &= M28;
        i9 = (((i9 + 536870908) - i25) + i17) + (i8 >>> 28);
        i8 &= M28;
        i13 = ((i13 + 536870910) - i29) + (i12 >>> 28);
        i12 &= M28;
        i2 = (i2 & M28) + (i >>> 28);
        i6 = (i6 & M28) + (i5 >>> 28);
        i5 &= M28;
        i10 = (i10 & M28) + (i9 >>> 28);
        i9 &= M28;
        i14 = (i14 & M28) + (i13 >>> 28);
        int i31 = i13 & M28;
        iArr3[0] = i & M28;
        iArr3[1] = i2;
        iArr3[2] = i3;
        iArr3[3] = i4;
        iArr3[4] = i5;
        iArr3[5] = i6;
        iArr3[6] = i7;
        iArr3[7] = i8;
        iArr3[8] = i9;
        iArr3[9] = i10;
        iArr3[10] = i11;
        iArr3[11] = i12;
        iArr3[12] = i31;
        iArr3[13] = i14;
        iArr3[14] = i15;
        iArr3[15] = i16;
    }
}
