package org.bouncycastle.math.raw;

public abstract class Mont256 {
    private static final long M = 4294967295L;

    public static int inverse32(int i) {
        int i2 = (2 - (i * i)) * i;
        i2 *= 2 - (i * i2);
        i2 *= 2 - (i * i2);
        return i2 * (2 - (i * i2));
    }

    public static void multAdd(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, int i) {
        int[] iArr5 = iArr3;
        int[] iArr6 = iArr4;
        int i2 = 0;
        long j = ((long) iArr2[0]) & 4294967295L;
        int i3 = 0;
        int i4 = i3;
        while (i3 < 8) {
            long j2 = ((long) iArr[i3]) & 4294967295L;
            long j3 = j2 * j;
            long j4 = (j3 & 4294967295L) + (((long) iArr5[i2]) & 4294967295L);
            long j5 = j;
            j = ((long) (((int) j4) * i)) & 4294967295L;
            int i5 = i3;
            int i6 = i4;
            long j6 = (((long) iArr6[i2]) & 4294967295L) * j;
            Object obj = 32;
            j4 = (((j4 + (j6 & 4294967295L)) >>> 32) + (j3 >>> 32)) + (j6 >>> 32);
            i3 = 1;
            while (i3 < 8) {
                long j7 = (((long) iArr2[i3]) & 4294967295L) * j2;
                long j8 = j2;
                long j9 = (((long) iArr6[i3]) & 4294967295L) * j;
                long j10 = j;
                j4 += ((j7 & 4294967295L) + (j9 & 4294967295L)) + (((long) iArr5[i3]) & 4294967295L);
                iArr5[i3 - 1] = (int) j4;
                j4 = ((j4 >>> 32) + (j7 >>> 32)) + (j9 >>> 32);
                i3++;
                int obj2 = 32;
                j2 = j8;
                j = j10;
            }
            Object obj3 = obj2;
            j4 += ((long) i6) & 4294967295L;
            iArr5[7] = (int) j4;
            i4 = (int) (j4 >>> obj3);
            i3 = i5 + 1;
            j = j5;
            i2 = 0;
        }
        if (i4 != 0 || Nat256.gte(iArr3, iArr4)) {
            Nat256.sub(iArr5, iArr6, iArr5);
        }
    }

    public static void multAddXF(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4) {
        int[] iArr5 = iArr3;
        int[] iArr6 = iArr4;
        int i = 0;
        long j = ((long) iArr2[0]) & 4294967295L;
        int i2 = 0;
        int i3 = i2;
        while (i2 < 8) {
            int i4;
            long j2 = ((long) iArr[i2]) & 4294967295L;
            long j3 = j;
            long j4 = (j2 * j) + (((long) iArr5[i]) & 4294967295L);
            long j5 = j4 & 4294967295L;
            j4 = (j4 >>> 32) + j5;
            int i5 = 1;
            while (i5 < 8) {
                i4 = i2;
                int i6 = i3;
                long j6 = (((long) iArr2[i5]) & 4294967295L) * j2;
                long j7 = j2;
                long j8 = (((long) iArr6[i5]) & 4294967295L) * j5;
                long j9 = j5;
                j4 += ((j6 & 4294967295L) + (j8 & 4294967295L)) + (((long) iArr5[i5]) & 4294967295L);
                iArr5[i5 - 1] = (int) j4;
                j4 = ((j4 >>> 32) + (j6 >>> 32)) + (j8 >>> 32);
                i5++;
                i2 = i4;
                i3 = i6;
                j2 = j7;
                j5 = j9;
            }
            i4 = i2;
            j4 += ((long) i3) & 4294967295L;
            iArr5[7] = (int) j4;
            i3 = (int) (j4 >>> 32);
            i2 = i4 + 1;
            j = j3;
            i = 0;
        }
        if (i3 != 0 || Nat256.gte(iArr3, iArr4)) {
            Nat256.sub(iArr5, iArr6, iArr5);
        }
    }

    public static void reduce(int[] iArr, int[] iArr2, int i) {
        int[] iArr3 = iArr;
        int[] iArr4 = iArr2;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int i4 = 8;
            if (i3 >= 8) {
                break;
            }
            int i5 = iArr3[i2];
            long j = ((long) (i5 * i)) & 4294967295L;
            long j2 = (((((long) iArr4[i2]) & 4294967295L) * j) + (((long) i5) & 4294967295L)) >>> 32;
            int i6 = 1;
            while (i6 < i4) {
                j2 += ((((long) iArr4[i6]) & 4294967295L) * j) + (((long) iArr3[i6]) & 4294967295L);
                iArr3[i6 - 1] = (int) j2;
                j2 >>>= 32;
                i6++;
                i5 = 32;
                i4 = 8;
            }
            iArr3[7] = (int) j2;
            i3++;
            i2 = 0;
        }
        if (Nat256.gte(iArr, iArr2)) {
            Nat256.sub(iArr3, iArr4, iArr3);
        }
    }

    public static void reduceXF(int[] iArr, int[] iArr2) {
        for (int i = 0; i < 8; i++) {
            long j = ((long) iArr[0]) & 4294967295L;
            long j2 = j;
            for (int i2 = 1; i2 < 8; i2++) {
                j2 += ((((long) iArr2[i2]) & 4294967295L) * j) + (((long) iArr[i2]) & 4294967295L);
                iArr[i2 - 1] = (int) j2;
                j2 >>>= 32;
            }
            iArr[7] = (int) j2;
        }
        if (Nat256.gte(iArr, iArr2)) {
            Nat256.sub(iArr, iArr2, iArr);
        }
    }
}
