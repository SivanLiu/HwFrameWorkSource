package org.bouncycastle.crypto.modes.gcm;

import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.math.raw.Interleave;
import org.bouncycastle.util.Pack;

public abstract class GCMUtil {
    private static final int E1 = -520093696;
    private static final long E1L = -2233785415175766016L;

    public static void asBytes(int[] iArr, byte[] bArr) {
        Pack.intToBigEndian(iArr, bArr, 0);
    }

    public static void asBytes(long[] jArr, byte[] bArr) {
        Pack.longToBigEndian(jArr, bArr, 0);
    }

    public static byte[] asBytes(int[] iArr) {
        byte[] bArr = new byte[16];
        Pack.intToBigEndian(iArr, bArr, 0);
        return bArr;
    }

    public static byte[] asBytes(long[] jArr) {
        byte[] bArr = new byte[16];
        Pack.longToBigEndian(jArr, bArr, 0);
        return bArr;
    }

    public static void asInts(byte[] bArr, int[] iArr) {
        Pack.bigEndianToInt(bArr, 0, iArr);
    }

    public static int[] asInts(byte[] bArr) {
        int[] iArr = new int[4];
        Pack.bigEndianToInt(bArr, 0, iArr);
        return iArr;
    }

    public static void asLongs(byte[] bArr, long[] jArr) {
        Pack.bigEndianToLong(bArr, 0, jArr);
    }

    public static long[] asLongs(byte[] bArr) {
        long[] jArr = new long[2];
        Pack.bigEndianToLong(bArr, 0, jArr);
        return jArr;
    }

    public static void copy(int[] iArr, int[] iArr2) {
        iArr2[0] = iArr[0];
        iArr2[1] = iArr[1];
        iArr2[2] = iArr[2];
        iArr2[3] = iArr[3];
    }

    public static void copy(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0];
        jArr2[1] = jArr[1];
    }

    public static void divideP(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = j >> 63;
        jArr2[0] = ((j ^ (E1L & j3)) << 1) | (j2 >>> 63);
        jArr2[1] = (j2 << 1) | (-j3);
    }

    public static void multiply(byte[] bArr, byte[] bArr2) {
        long[] asLongs = asLongs(bArr);
        multiply(asLongs, asLongs(bArr2));
        asBytes(asLongs, bArr);
    }

    public static void multiply(int[] iArr, int[] iArr2) {
        int i = iArr2[0];
        int i2 = iArr2[1];
        int i3 = iArr2[2];
        int i4 = iArr2[3];
        int i5 = 0;
        int i6 = i5;
        int i7 = i;
        int i8 = i2;
        int i9 = i3;
        i = i6;
        i2 = i;
        i3 = i2;
        while (i5 < 4) {
            int i10 = iArr[i5];
            int i11 = i4;
            i4 = i6;
            i6 = i3;
            i3 = i2;
            i2 = i;
            for (i = 0; i < 32; i++) {
                int i12 = i10 >> 31;
                i10 <<= 1;
                i2 ^= i7 & i12;
                i3 ^= i8 & i12;
                i6 ^= i9 & i12;
                i4 ^= i12 & i11;
                i11 = (i11 >>> 1) | (i9 << 31);
                i9 = (i9 >>> 1) | (i8 << 31);
                i8 = (i8 >>> 1) | (i7 << 31);
                i7 = (i7 >>> 1) ^ (((i11 << 31) >> 8) & E1);
            }
            i5++;
            i = i2;
            i2 = i3;
            i3 = i6;
            i6 = i4;
            i4 = i11;
        }
        iArr[0] = i;
        iArr[1] = i2;
        iArr[2] = i3;
        iArr[3] = i6;
    }

    public static void multiply(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr2[0];
        long j4 = jArr2[1];
        long j5 = 0;
        long j6 = j2;
        j2 = 0;
        long j7 = j2;
        for (int i = 0; i < 64; i++) {
            long j8 = j >> 63;
            j <<= 1;
            j5 ^= j3 & j8;
            j2 ^= j4 & j8;
            j8 = j6 >> 63;
            j6 <<= 1;
            j2 ^= j3 & j8;
            j7 ^= j4 & j8;
            j4 = (j4 >>> 1) | (j3 << 63);
            j3 = (j3 >>> 1) ^ (((j4 << 63) >> 8) & E1L);
        }
        j = (((j7 << 63) ^ (j7 << 62)) ^ (j7 << 57)) ^ j2;
        jArr[0] = ((((j7 >>> 1) ^ j7) ^ (j7 >>> 2)) ^ (j7 >>> 7)) ^ j5;
        jArr[1] = j;
    }

    public static void multiplyP(int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        int i4 = iArr[3];
        iArr[0] = (((i4 << 31) >> 31) & E1) ^ (i >>> 1);
        iArr[1] = (i2 >>> 1) | (i << 31);
        iArr[2] = (i3 >>> 1) | (i2 << 31);
        iArr[3] = (i4 >>> 1) | (i3 << 31);
    }

    public static void multiplyP(int[] iArr, int[] iArr2) {
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        int i4 = iArr[3];
        iArr2[0] = (((i4 << 31) >> 31) & E1) ^ (i >>> 1);
        iArr2[1] = (i2 >>> 1) | (i << 31);
        iArr2[2] = (i3 >>> 1) | (i2 << 31);
        iArr2[3] = (i4 >>> 1) | (i3 << 31);
    }

    public static void multiplyP(long[] jArr) {
        long j = jArr[0];
        long j2 = jArr[1];
        jArr[0] = (((j2 << 63) >> 63) & E1L) ^ (j >>> 1);
        jArr[1] = (j << 63) | (j2 >>> 1);
    }

    public static void multiplyP(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        jArr2[0] = (((j2 << 63) >> 63) & E1L) ^ (j >>> 1);
        jArr2[1] = (j << 63) | (j2 >>> 1);
    }

    public static void multiplyP3(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = j2 << 61;
        jArr2[0] = (j3 >>> 7) ^ ((((j >>> 3) ^ j3) ^ (j3 >>> 1)) ^ (j3 >>> 2));
        jArr2[1] = (j << 61) | (j2 >>> 3);
    }

    public static void multiplyP4(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = j2 << 60;
        jArr2[0] = (j3 >>> 7) ^ ((((j >>> 4) ^ j3) ^ (j3 >>> 1)) ^ (j3 >>> 2));
        jArr2[1] = (j << 60) | (j2 >>> 4);
    }

    public static void multiplyP7(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = j2 << 57;
        jArr2[0] = (j3 >>> 7) ^ ((((j >>> 7) ^ j3) ^ (j3 >>> 1)) ^ (j3 >>> 2));
        jArr2[1] = (j << 57) | (j2 >>> 7);
    }

    public static void multiplyP8(int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        int i4 = iArr[3];
        int i5 = i4 << 24;
        iArr[0] = (i5 >>> 7) ^ ((((i >>> 8) ^ i5) ^ (i5 >>> 1)) ^ (i5 >>> 2));
        iArr[1] = (i2 >>> 8) | (i << 24);
        iArr[2] = (i3 >>> 8) | (i2 << 24);
        iArr[3] = (i4 >>> 8) | (i3 << 24);
    }

    public static void multiplyP8(int[] iArr, int[] iArr2) {
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        int i4 = iArr[3];
        int i5 = i4 << 24;
        iArr2[0] = (i5 >>> 7) ^ ((((i >>> 8) ^ i5) ^ (i5 >>> 1)) ^ (i5 >>> 2));
        iArr2[1] = (i2 >>> 8) | (i << 24);
        iArr2[2] = (i3 >>> 8) | (i2 << 24);
        iArr2[3] = (i4 >>> 8) | (i3 << 24);
    }

    public static void multiplyP8(long[] jArr) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = j2 << 56;
        jArr[0] = (j3 >>> 7) ^ ((((j >>> 8) ^ j3) ^ (j3 >>> 1)) ^ (j3 >>> 2));
        jArr[1] = (j << 56) | (j2 >>> 8);
    }

    public static void multiplyP8(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = j2 << 56;
        jArr2[0] = (j3 >>> 7) ^ ((((j >>> 8) ^ j3) ^ (j3 >>> 1)) ^ (j3 >>> 2));
        jArr2[1] = (j << 56) | (j2 >>> 8);
    }

    public static byte[] oneAsBytes() {
        byte[] bArr = new byte[16];
        bArr[0] = Byte.MIN_VALUE;
        return bArr;
    }

    public static int[] oneAsInts() {
        int[] iArr = new int[4];
        iArr[0] = PKIFailureInfo.systemUnavail;
        return iArr;
    }

    public static long[] oneAsLongs() {
        long[] jArr = new long[2];
        jArr[0] = Long.MIN_VALUE;
        return jArr;
    }

    public static long[] pAsLongs() {
        long[] jArr = new long[2];
        jArr[0] = 4611686018427387904L;
        return jArr;
    }

    public static void square(long[] jArr, long[] jArr2) {
        long[] jArr3 = new long[4];
        Interleave.expand64To128Rev(jArr[0], jArr3, 0);
        Interleave.expand64To128Rev(jArr[1], jArr3, 2);
        long j = jArr3[0];
        long j2 = jArr3[1];
        long j3 = jArr3[2];
        long j4 = jArr3[3];
        j3 ^= (j4 << 57) ^ ((j4 << 63) ^ (j4 << 62));
        j ^= (((j3 >>> 1) ^ j3) ^ (j3 >>> 2)) ^ (j3 >>> 7);
        j2 = (j2 ^ ((((j4 >>> 1) ^ j4) ^ (j4 >>> 2)) ^ (j4 >>> 7))) ^ ((j3 << 57) ^ ((j3 << 63) ^ (j3 << 62)));
        jArr2[0] = j;
        jArr2[1] = j2;
    }

    public static void xor(byte[] bArr, int i, byte[] bArr2, int i2, int i3) {
        while (true) {
            i3--;
            if (i3 >= 0) {
                int i4 = i + i3;
                bArr[i4] = (byte) (bArr[i4] ^ bArr2[i2 + i3]);
            } else {
                return;
            }
        }
    }

    public static void xor(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) {
        int i4 = 0;
        do {
            bArr3[i3 + i4] = (byte) (bArr[i + i4] ^ bArr2[i2 + i4]);
            i4++;
            bArr3[i3 + i4] = (byte) (bArr[i + i4] ^ bArr2[i2 + i4]);
            i4++;
            bArr3[i3 + i4] = (byte) (bArr[i + i4] ^ bArr2[i2 + i4]);
            i4++;
            bArr3[i3 + i4] = (byte) (bArr[i + i4] ^ bArr2[i2 + i4]);
            i4++;
        } while (i4 < 16);
    }

    public static void xor(byte[] bArr, byte[] bArr2) {
        int i = 0;
        do {
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
        } while (i < 16);
    }

    public static void xor(byte[] bArr, byte[] bArr2, int i) {
        int i2 = 0;
        do {
            bArr[i2] = (byte) (bArr[i2] ^ bArr2[i + i2]);
            i2++;
            bArr[i2] = (byte) (bArr[i2] ^ bArr2[i + i2]);
            i2++;
            bArr[i2] = (byte) (bArr[i2] ^ bArr2[i + i2]);
            i2++;
            bArr[i2] = (byte) (bArr[i2] ^ bArr2[i + i2]);
            i2++;
        } while (i2 < 16);
    }

    public static void xor(byte[] bArr, byte[] bArr2, int i, int i2) {
        while (true) {
            i2--;
            if (i2 >= 0) {
                bArr[i2] = (byte) (bArr[i2] ^ bArr2[i + i2]);
            } else {
                return;
            }
        }
    }

    public static void xor(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        int i = 0;
        do {
            bArr3[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
            bArr3[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
            bArr3[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
            bArr3[i] = (byte) (bArr[i] ^ bArr2[i]);
            i++;
        } while (i < 16);
    }

    public static void xor(int[] iArr, int[] iArr2) {
        iArr[0] = iArr[0] ^ iArr2[0];
        iArr[1] = iArr[1] ^ iArr2[1];
        iArr[2] = iArr[2] ^ iArr2[2];
        iArr[3] = iArr2[3] ^ iArr[3];
    }

    public static void xor(int[] iArr, int[] iArr2, int[] iArr3) {
        iArr3[0] = iArr[0] ^ iArr2[0];
        iArr3[1] = iArr[1] ^ iArr2[1];
        iArr3[2] = iArr[2] ^ iArr2[2];
        iArr3[3] = iArr[3] ^ iArr2[3];
    }

    public static void xor(long[] jArr, long[] jArr2) {
        jArr[0] = jArr[0] ^ jArr2[0];
        jArr[1] = jArr[1] ^ jArr2[1];
    }

    public static void xor(long[] jArr, long[] jArr2, long[] jArr3) {
        jArr3[0] = jArr[0] ^ jArr2[0];
        jArr3[1] = jArr2[1] ^ jArr[1];
    }
}
