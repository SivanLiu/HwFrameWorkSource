package org.bouncycastle.crypto.modes.kgcm;

import org.bouncycastle.math.raw.Interleave;

public class KGCMUtil_256 {
    public static final int SIZE = 4;

    public static void add(long[] jArr, long[] jArr2, long[] jArr3) {
        jArr3[0] = jArr[0] ^ jArr2[0];
        jArr3[1] = jArr[1] ^ jArr2[1];
        jArr3[2] = jArr[2] ^ jArr2[2];
        jArr3[3] = jArr2[3] ^ jArr[3];
    }

    public static void copy(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0];
        jArr2[1] = jArr[1];
        jArr2[2] = jArr[2];
        jArr2[3] = jArr[3];
    }

    public static boolean equal(long[] jArr, long[] jArr2) {
        return ((jArr2[3] ^ jArr[3]) | ((((jArr[0] ^ jArr2[0]) | 0) | (jArr[1] ^ jArr2[1])) | (jArr[2] ^ jArr2[2]))) == 0;
    }

    public static void multiply(long[] jArr, long[] jArr2, long[] jArr3) {
        int i;
        long j;
        long j2 = jArr[0];
        long j3 = jArr[1];
        int i2 = 2;
        long j4 = jArr[2];
        long j5 = jArr[3];
        long j6 = jArr2[0];
        long j7 = jArr2[1];
        long j8 = jArr2[2];
        long j9 = 0;
        long j10 = jArr2[3];
        int i3 = 0;
        long j11 = j3;
        long j12 = j8;
        j3 = 0;
        j8 = j3;
        long j13 = j8;
        long j14 = j13;
        while (true) {
            i = 64;
            if (i3 >= 64) {
                break;
            }
            long j15 = -(j2 & 1);
            j9 ^= j6 & j15;
            j3 ^= j7 & j15;
            j = -(j11 & 1);
            j11 >>>= 1;
            j3 ^= j6 & j;
            j8 = (j8 ^ (j12 & j15)) ^ (j7 & j);
            j13 = (j13 ^ (j10 & j15)) ^ (j12 & j);
            j14 ^= j10 & j;
            j = j10 >> 63;
            j10 = (j10 << 1) | (j12 >>> 63);
            j12 = (j12 << 1) | (j7 >>> 63);
            j7 = (j7 << 1) | (j6 >>> 63);
            j6 = (j6 << 1) ^ (j & 1061);
            i3++;
            j5 = j5;
            j2 >>>= 1;
        }
        long j16 = j5;
        j = (((j10 >>> 62) ^ j6) ^ (j10 >>> 59)) ^ (j10 >>> 54);
        j5 = ((j10 ^ (j10 << 2)) ^ (j10 << 5)) ^ (j10 << 10);
        j10 = j12;
        j12 = j7;
        j7 = j;
        int i4 = 0;
        while (i4 < i) {
            long j17 = -(j4 & 1);
            j9 ^= j5 & j17;
            j3 ^= j7 & j17;
            long j18 = -(j16 & 1);
            j16 >>>= 1;
            j3 ^= j5 & j18;
            j8 = (j8 ^ (j12 & j17)) ^ (j7 & j18);
            j13 = (j13 ^ (j10 & j17)) ^ (j12 & j18);
            j14 ^= j10 & j18;
            j17 = j10 >> 63;
            j10 = (j10 << 1) | (j12 >>> 63);
            j12 = (j12 << 1) | (j7 >>> 63);
            j7 = (j7 << 1) | (j5 >>> 63);
            j5 = (j5 << 1) ^ (j17 & 1061);
            i4++;
            j4 >>>= 1;
            i = 64;
            i2 = 2;
        }
        j = (((j14 >>> 62) ^ (j14 >>> 59)) ^ (j14 >>> 54)) ^ j3;
        jArr3[0] = j9 ^ (((j14 ^ (j14 << i2)) ^ (j14 << 5)) ^ (j14 << 10));
        jArr3[1] = j;
        jArr3[2] = j8;
        jArr3[3] = j13;
    }

    public static void multiplyX(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        jArr2[0] = ((j4 >> 63) & 1061) ^ (j << 1);
        jArr2[1] = (j >>> 63) | (j2 << 1);
        jArr2[2] = (j3 << 1) | (j2 >>> 63);
        jArr2[3] = (j4 << 1) | (j3 >>> 63);
    }

    public static void multiplyX8(long[] jArr, long[] jArr2) {
        long j = jArr[0];
        long j2 = jArr[1];
        long j3 = jArr[2];
        long j4 = jArr[3];
        long j5 = j4 >>> 56;
        jArr2[0] = ((((j << 8) ^ j5) ^ (j5 << 2)) ^ (j5 << 5)) ^ (j5 << 10);
        jArr2[1] = (j >>> 56) | (j2 << 8);
        jArr2[2] = (j3 << 8) | (j2 >>> 56);
        jArr2[3] = (j4 << 8) | (j3 >>> 56);
    }

    public static void one(long[] jArr) {
        jArr[0] = 1;
        jArr[1] = 0;
        jArr[2] = 0;
        jArr[3] = 0;
    }

    public static void square(long[] jArr, long[] jArr2) {
        int i = 8;
        long[] jArr3 = new long[8];
        for (int i2 = 0; i2 < 4; i2++) {
            Interleave.expand64To128(jArr[i2], jArr3, i2 << 1);
        }
        while (true) {
            i--;
            if (i >= 4) {
                long j = jArr3[i];
                int i3 = i - 4;
                jArr3[i3] = jArr3[i3] ^ ((((j << 2) ^ j) ^ (j << 5)) ^ (j << 10));
                i3++;
                jArr3[i3] = ((j >>> 54) ^ ((j >>> 62) ^ (j >>> 59))) ^ jArr3[i3];
            } else {
                copy(jArr3, jArr2);
                return;
            }
        }
    }

    public static void x(long[] jArr) {
        jArr[0] = 2;
        jArr[1] = 0;
        jArr[2] = 0;
        jArr[3] = 0;
    }

    public static void zero(long[] jArr) {
        jArr[0] = 0;
        jArr[1] = 0;
        jArr[2] = 0;
        jArr[3] = 0;
    }
}
