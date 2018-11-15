package org.bouncycastle.math.raw;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.util.Pack;

public abstract class Nat128 {
    private static final long M = 4294967295L;

    public static int add(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + ((((long) iArr[0]) & 4294967295L) + (((long) iArr2[0]) & 4294967295L));
        iArr3[0] = (int) j;
        long j2 = (j >>> 32) + ((((long) iArr[1]) & 4294967295L) + (((long) iArr2[1]) & 4294967295L));
        iArr3[1] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[2]) & 4294967295L) + (((long) iArr2[2]) & 4294967295L));
        iArr3[2] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[3]) & 4294967295L) + (((long) iArr2[3]) & 4294967295L));
        iArr3[3] = (int) j2;
        return (int) (j2 >>> 32);
    }

    public static int addBothTo(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + (((((long) iArr[0]) & 4294967295L) + (((long) iArr2[0]) & 4294967295L)) + (((long) iArr3[0]) & 4294967295L));
        iArr3[0] = (int) j;
        long j2 = (j >>> 32) + (((((long) iArr[1]) & 4294967295L) + (((long) iArr2[1]) & 4294967295L)) + (((long) iArr3[1]) & 4294967295L));
        iArr3[1] = (int) j2;
        j2 = (j2 >>> 32) + (((((long) iArr[2]) & 4294967295L) + (((long) iArr2[2]) & 4294967295L)) + (((long) iArr3[2]) & 4294967295L));
        iArr3[2] = (int) j2;
        j2 = (j2 >>> 32) + (((((long) iArr[3]) & 4294967295L) + (((long) iArr2[3]) & 4294967295L)) + (((long) iArr3[3]) & 4294967295L));
        iArr3[3] = (int) j2;
        return (int) (j2 >>> 32);
    }

    public static int addTo(int[] iArr, int i, int[] iArr2, int i2, int i3) {
        i3 = i2 + 0;
        long j = (((long) i3) & 4294967295L) + ((((long) iArr[i + 0]) & 4294967295L) + (((long) iArr2[i3]) & 4294967295L));
        iArr2[i3] = (int) j;
        int i4 = i2 + 1;
        j = (j >>> 32) + ((((long) iArr[i + 1]) & 4294967295L) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j;
        i4 = i2 + 2;
        j = (j >>> 32) + ((((long) iArr[i + 2]) & 4294967295L) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j;
        i2 += 3;
        j = (j >>> 32) + ((((long) iArr[i + 3]) & 4294967295L) + (4294967295L & ((long) iArr2[i2])));
        iArr2[i2] = (int) j;
        return (int) (j >>> 32);
    }

    public static int addTo(int[] iArr, int[] iArr2) {
        long j = 0 + ((((long) iArr[0]) & 4294967295L) + (((long) iArr2[0]) & 4294967295L));
        iArr2[0] = (int) j;
        long j2 = (j >>> 32) + ((((long) iArr[1]) & 4294967295L) + (((long) iArr2[1]) & 4294967295L));
        iArr2[1] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[2]) & 4294967295L) + (((long) iArr2[2]) & 4294967295L));
        iArr2[2] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[3]) & 4294967295L) + (4294967295L & ((long) iArr2[3])));
        iArr2[3] = (int) j2;
        return (int) (j2 >>> 32);
    }

    public static int addToEachOther(int[] iArr, int i, int[] iArr2, int i2) {
        int i3 = i + 0;
        int i4 = i2 + 0;
        long j = 0 + ((((long) iArr[i3]) & 4294967295L) + (((long) iArr2[i4]) & 4294967295L));
        int i5 = (int) j;
        iArr[i3] = i5;
        iArr2[i4] = i5;
        i4 = i + 1;
        int i6 = i2 + 1;
        long j2 = (j >>> 32) + ((((long) iArr[i4]) & 4294967295L) + (((long) iArr2[i6]) & 4294967295L));
        int i7 = (int) j2;
        iArr[i4] = i7;
        iArr2[i6] = i7;
        i4 = i + 2;
        i6 = i2 + 2;
        j2 = (j2 >>> 32) + ((((long) iArr[i4]) & 4294967295L) + (((long) iArr2[i6]) & 4294967295L));
        i7 = (int) j2;
        iArr[i4] = i7;
        iArr2[i6] = i7;
        i += 3;
        i2 += 3;
        j2 = (j2 >>> 32) + ((((long) iArr[i]) & 4294967295L) + (4294967295L & ((long) iArr2[i2])));
        int i8 = (int) j2;
        iArr[i] = i8;
        iArr2[i2] = i8;
        return (int) (j2 >>> 32);
    }

    public static void copy(int[] iArr, int i, int[] iArr2, int i2) {
        iArr2[i2 + 0] = iArr[i + 0];
        iArr2[i2 + 1] = iArr[i + 1];
        iArr2[i2 + 2] = iArr[i + 2];
        iArr2[i2 + 3] = iArr[i + 3];
    }

    public static void copy(int[] iArr, int[] iArr2) {
        iArr2[0] = iArr[0];
        iArr2[1] = iArr[1];
        iArr2[2] = iArr[2];
        iArr2[3] = iArr[3];
    }

    public static void copy64(long[] jArr, int i, long[] jArr2, int i2) {
        jArr2[i2 + 0] = jArr[i + 0];
        jArr2[i2 + 1] = jArr[i + 1];
    }

    public static void copy64(long[] jArr, long[] jArr2) {
        jArr2[0] = jArr[0];
        jArr2[1] = jArr[1];
    }

    public static int[] create() {
        return new int[4];
    }

    public static long[] create64() {
        return new long[2];
    }

    public static int[] createExt() {
        return new int[8];
    }

    public static long[] createExt64() {
        return new long[4];
    }

    public static boolean diff(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        boolean gte = gte(iArr, i, iArr2, i2);
        if (gte) {
            sub(iArr, i, iArr2, i2, iArr3, i3);
            return gte;
        }
        sub(iArr2, i2, iArr, i, iArr3, i3);
        return gte;
    }

    public static boolean eq(int[] iArr, int[] iArr2) {
        for (int i = 3; i >= 0; i--) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean eq64(long[] jArr, long[] jArr2) {
        for (int i = 1; i >= 0; i--) {
            if (jArr[i] != jArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static int[] fromBigInteger(BigInteger bigInteger) {
        if (bigInteger.signum() < 0 || bigInteger.bitLength() > 128) {
            throw new IllegalArgumentException();
        }
        int[] create = create();
        int i = 0;
        while (bigInteger.signum() != 0) {
            int i2 = i + 1;
            create[i] = bigInteger.intValue();
            bigInteger = bigInteger.shiftRight(32);
            i = i2;
        }
        return create;
    }

    public static long[] fromBigInteger64(BigInteger bigInteger) {
        if (bigInteger.signum() < 0 || bigInteger.bitLength() > 128) {
            throw new IllegalArgumentException();
        }
        long[] create64 = create64();
        int i = 0;
        while (bigInteger.signum() != 0) {
            int i2 = i + 1;
            create64[i] = bigInteger.longValue();
            bigInteger = bigInteger.shiftRight(64);
            i = i2;
        }
        return create64;
    }

    public static int getBit(int[] iArr, int i) {
        int i2;
        if (i == 0) {
            i2 = iArr[0];
        } else {
            int i3 = i >> 5;
            if (i3 < 0 || i3 >= 4) {
                return 0;
            }
            i2 = iArr[i3] >>> (i & 31);
        }
        return i2 & 1;
    }

    public static boolean gte(int[] iArr, int i, int[] iArr2, int i2) {
        for (int i3 = 3; i3 >= 0; i3--) {
            int i4 = iArr[i + i3] ^ PKIFailureInfo.systemUnavail;
            int i5 = PKIFailureInfo.systemUnavail ^ iArr2[i2 + i3];
            if (i4 < i5) {
                return false;
            }
            if (i4 > i5) {
                return true;
            }
        }
        return true;
    }

    public static boolean gte(int[] iArr, int[] iArr2) {
        for (int i = 3; i >= 0; i--) {
            int i2 = iArr[i] ^ PKIFailureInfo.systemUnavail;
            int i3 = PKIFailureInfo.systemUnavail ^ iArr2[i];
            if (i2 < i3) {
                return false;
            }
            if (i2 > i3) {
                return true;
            }
        }
        return true;
    }

    public static boolean isOne(int[] iArr) {
        if (iArr[0] != 1) {
            return false;
        }
        for (int i = 1; i < 4; i++) {
            if (iArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOne64(long[] jArr) {
        if (jArr[0] != 1) {
            return false;
        }
        for (int i = 1; i < 2; i++) {
            if (jArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isZero(int[] iArr) {
        for (int i = 0; i < 4; i++) {
            if (iArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isZero64(long[] jArr) {
        for (int i = 0; i < 2; i++) {
            if (jArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static void mul(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = ((long) iArr2[i2 + 0]) & 4294967295L;
        long j2 = ((long) iArr2[i2 + 1]) & 4294967295L;
        long j3 = ((long) iArr2[i2 + 2]) & 4294967295L;
        long j4 = ((long) iArr2[i2 + 3]) & 4294967295L;
        long j5 = ((long) iArr[i + 0]) & 4294967295L;
        long j6 = 0 + (j5 * j);
        long j7 = j;
        iArr3[i3 + 0] = (int) j6;
        long j8 = (j6 >>> 32) + (j5 * j2);
        long j9 = j2;
        iArr3[i3 + 1] = (int) j8;
        j8 = (j8 >>> 32) + (j5 * j3);
        iArr3[i3 + 2] = (int) j8;
        j8 = (j8 >>> 32) + (j5 * j4);
        iArr3[i3 + 3] = (int) j8;
        iArr3[i3 + 4] = (int) (j8 >>> 32);
        int i4 = 1;
        int i5 = i3;
        int i6 = 1;
        while (i6 < 4) {
            i5 += i4;
            long j10 = ((long) iArr[i + i6]) & 4294967295L;
            int i7 = i5 + 0;
            j = 0 + ((j10 * j7) + (((long) iArr3[i7]) & 4294967295L));
            iArr3[i7] = (int) j;
            int i8 = i5 + 1;
            j = (j >>> 32) + ((j10 * j9) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j;
            int i9 = i5 + 2;
            j = (j >>> 32) + ((j10 * j3) + (((long) iArr3[i9]) & 4294967295L));
            iArr3[i9] = (int) j;
            i9 = i5 + 3;
            j = (j >>> 32) + ((j10 * j4) + (((long) iArr3[i9]) & 4294967295L));
            iArr3[i9] = (int) j;
            iArr3[i5 + 4] = (int) (j >>> 32);
            i6++;
            int i10 = 32;
            i4 = 1;
        }
    }

    public static void mul(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = ((long) iArr2[0]) & 4294967295L;
        int i = 1;
        long j2 = ((long) iArr2[1]) & 4294967295L;
        long j3 = ((long) iArr2[3]) & 4294967295L;
        long j4 = ((long) iArr2[2]) & 4294967295L;
        long j5 = ((long) iArr[0]) & 4294967295L;
        long j6 = 0 + (j5 * j);
        iArr3[0] = (int) j6;
        long j7 = 32;
        j6 = (j6 >>> 32) + (j5 * j2);
        iArr3[1] = (int) j6;
        j6 = (j6 >>> 32) + (j5 * j4);
        iArr3[2] = (int) j6;
        j6 = (j6 >>> 32) + (j5 * j3);
        iArr3[3] = (int) j6;
        int i2 = (int) (j6 >>> 32);
        int i3 = 4;
        iArr3[4] = i2;
        while (i < i3) {
            j5 = ((long) iArr[i]) & 4294967295L;
            i2 = i + 0;
            j6 = 0 + ((j5 * j) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j6;
            i2 = i + 1;
            j6 = (j6 >>> j7) + ((j5 * j2) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j6;
            int i4 = i + 2;
            j6 = (j6 >>> 32) + ((j5 * j4) + (((long) iArr3[i4]) & 4294967295L));
            iArr3[i4] = (int) j6;
            int i5 = i + 3;
            j6 = (j6 >>> 32) + ((j5 * j3) + (((long) iArr3[i5]) & 4294967295L));
            iArr3[i5] = (int) j6;
            iArr3[i + 4] = (int) (j6 >>> 32);
            j7 = 32;
            i = i2;
            i3 = 4;
        }
    }

    public static long mul33Add(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        long j = ((long) i) & 4294967295L;
        long j2 = ((long) iArr[i2 + 0]) & 4294967295L;
        long j3 = 0 + ((j * j2) + (((long) iArr2[i3 + 0]) & 4294967295L));
        iArr3[i4 + 0] = (int) j3;
        long j4 = ((long) iArr[i2 + 1]) & 4294967295L;
        j3 = (j3 >>> 32) + (((j * j4) + j2) + (((long) iArr2[i3 + 1]) & 4294967295L));
        iArr3[i4 + 1] = (int) j3;
        long j5 = ((long) iArr[i2 + 2]) & 4294967295L;
        j3 = (j3 >>> 32) + (((j * j5) + j4) + (((long) iArr2[i3 + 2]) & 4294967295L));
        iArr3[i4 + 2] = (int) j3;
        long j6 = ((long) iArr[i2 + 3]) & 4294967295L;
        j3 = (j3 >>> 32) + (((j * j6) + j5) + (((long) iArr2[i3 + 3]) & 4294967295L));
        iArr3[i4 + 3] = (int) j3;
        return (j3 >>> 32) + j6;
    }

    public static int mul33DWordAdd(int i, long j, int[] iArr, int i2) {
        long j2 = ((long) i) & 4294967295L;
        long j3 = j & 4294967295L;
        i = i2 + 0;
        long j4 = 0 + ((j2 * j3) + (((long) iArr[i]) & 4294967295L));
        iArr[i] = (int) j4;
        j >>>= 32;
        j2 = (j2 * j) + j3;
        int i3 = i2 + 1;
        long j5 = (j4 >>> 32) + (j2 + (((long) iArr[i3]) & 4294967295L));
        iArr[i3] = (int) j5;
        i3 = i2 + 2;
        j2 = (j5 >>> 32) + (j + (((long) iArr[i3]) & 4294967295L));
        iArr[i3] = (int) j2;
        i2 += 3;
        j = (j2 >>> 32) + (((long) iArr[i2]) & 4294967295L);
        iArr[i2] = (int) j;
        return (int) (j >>> 32);
    }

    public static int mul33WordAdd(int i, int i2, int[] iArr, int i3) {
        long j = ((long) i2) & 4294967295L;
        int i4 = i3 + 0;
        long j2 = (((((long) i) & 4294967295L) * j) + (((long) iArr[i4]) & 4294967295L)) + 0;
        iArr[i4] = (int) j2;
        int i5 = i3 + 1;
        j2 = (j2 >>> 32) + (j + (((long) iArr[i5]) & 4294967295L));
        iArr[i5] = (int) j2;
        j = j2 >>> 32;
        int i6 = i3 + 2;
        j += ((long) iArr[i6]) & 4294967295L;
        iArr[i6] = (int) j;
        return (j >>> 32) == 0 ? 0 : Nat.incAt(4, iArr, i3, 3);
    }

    public static int mulAddTo(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = 4294967295L;
        long j2 = ((long) iArr2[i2 + 0]) & 4294967295L;
        long j3 = ((long) iArr2[i2 + 1]) & 4294967295L;
        long j4 = ((long) iArr2[i2 + 2]) & 4294967295L;
        long j5 = ((long) iArr2[i2 + 3]) & 4294967295L;
        int i4 = 0;
        int i5 = i3;
        long j6 = 0;
        while (i4 < 4) {
            long j7 = ((long) iArr[i + i4]) & j;
            int i6 = i5 + 0;
            long j8 = j2;
            j = 0 + ((j7 * j2) + (((long) iArr3[i6]) & j));
            iArr3[i6] = (int) j;
            int i7 = i5 + 1;
            long j9 = j3;
            long j10 = (j >>> 32) + ((j7 * j3) + (((long) iArr3[i7]) & 4294967295L));
            iArr3[i7] = (int) j10;
            int i8 = i5 + 2;
            long j11 = j4;
            j10 = (j10 >>> 32) + ((j7 * j4) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j10;
            int i9 = i5 + 3;
            j10 = (j10 >>> 32) + ((j7 * j5) + (((long) iArr3[i9]) & 4294967295L));
            iArr3[i9] = (int) j10;
            i5 += 4;
            j10 = (j10 >>> 32) + (j6 + (((long) iArr3[i5]) & 4294967295L));
            iArr3[i5] = (int) j10;
            j6 = j10 >>> 32;
            i4++;
            i5 = i7;
            j = 4294967295L;
            j2 = j8;
            j3 = j9;
            j4 = j11;
        }
        return (int) j6;
    }

    public static int mulAddTo(int[] iArr, int[] iArr2, int[] iArr3) {
        int i = 0;
        long j = 4294967295L;
        long j2 = ((long) iArr2[0]) & 4294967295L;
        long j3 = ((long) iArr2[1]) & 4294967295L;
        long j4 = ((long) iArr2[2]) & 4294967295L;
        long j5 = ((long) iArr2[3]) & 4294967295L;
        long j6 = 0;
        while (i < 4) {
            long j7 = ((long) iArr[i]) & j;
            int i2 = i + 0;
            long j8 = j2;
            j = 0 + ((j7 * j2) + (((long) iArr3[i2]) & j));
            iArr3[i2] = (int) j;
            int i3 = i + 1;
            j = (j >>> 32) + ((j7 * j3) + (((long) iArr3[i3]) & 4294967295L));
            iArr3[i3] = (int) j;
            int i4 = i + 2;
            long j9 = j3;
            j2 = (j >>> 32) + ((j7 * j4) + (((long) iArr3[i4]) & 4294967295L));
            iArr3[i4] = (int) j2;
            int i5 = i + 3;
            j2 = (j2 >>> 32) + ((j7 * j5) + (((long) iArr3[i5]) & 4294967295L));
            iArr3[i5] = (int) j2;
            i += 4;
            j2 = (j2 >>> 32) + (j6 + (((long) iArr3[i]) & 4294967295L));
            iArr3[i] = (int) j2;
            j6 = j2 >>> 32;
            i = i3;
            j = 4294967295L;
            j2 = j8;
            j3 = j9;
        }
        return (int) j6;
    }

    public static int mulWord(int i, int[] iArr, int[] iArr2, int i2) {
        long j = ((long) i) & 4294967295L;
        long j2 = 0;
        i = 0;
        do {
            j2 += (((long) iArr[i]) & 4294967295L) * j;
            iArr2[i2 + i] = (int) j2;
            j2 >>>= 32;
            i++;
        } while (i < 4);
        return (int) j2;
    }

    public static int mulWordAddExt(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        long j = ((long) i) & 4294967295L;
        i = i3 + 0;
        long j2 = 0 + (((((long) iArr[i2 + 0]) & 4294967295L) * j) + (((long) iArr2[i]) & 4294967295L));
        iArr2[i] = (int) j2;
        int i4 = i3 + 1;
        long j3 = (j2 >>> 32) + (((((long) iArr[i2 + 1]) & 4294967295L) * j) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j3;
        i4 = i3 + 2;
        j3 = (j3 >>> 32) + (((((long) iArr[i2 + 2]) & 4294967295L) * j) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j3;
        i3 += 3;
        j3 = (j3 >>> 32) + ((j * (((long) iArr[i2 + 3]) & 4294967295L)) + (((long) iArr2[i3]) & 4294967295L));
        iArr2[i3] = (int) j3;
        return (int) (j3 >>> 32);
    }

    public static int mulWordDwordAdd(int i, long j, int[] iArr, int i2) {
        long j2 = ((long) i) & 4294967295L;
        i = i2 + 0;
        long j3 = (((j & 4294967295L) * j2) + (((long) iArr[i]) & 4294967295L)) + 0;
        iArr[i] = (int) j3;
        j2 *= j >>> 32;
        int i3 = i2 + 1;
        j3 = (j3 >>> 32) + (j2 + (((long) iArr[i3]) & 4294967295L));
        iArr[i3] = (int) j3;
        int i4 = i2 + 2;
        j = (j3 >>> 32) + (((long) iArr[i4]) & 4294967295L);
        iArr[i4] = (int) j;
        return (j >>> 32) == 0 ? 0 : Nat.incAt(4, iArr, i2, 3);
    }

    public static int mulWordsAdd(int i, int i2, int[] iArr, int i3) {
        int i4 = i3 + 0;
        long j = (((((long) i2) & 4294967295L) * (((long) i) & 4294967295L)) + (((long) iArr[i4]) & 4294967295L)) + 0;
        iArr[i4] = (int) j;
        int i5 = i3 + 1;
        j = (j >>> 32) + (4294967295L & ((long) iArr[i5]));
        iArr[i5] = (int) j;
        return (j >>> 32) == 0 ? 0 : Nat.incAt(4, iArr, i3, 2);
    }

    public static void square(int[] iArr, int i, int[] iArr2, int i2) {
        long j = 4294967295L;
        long j2 = ((long) iArr[i + 0]) & 4294967295L;
        int i3 = 8;
        int i4 = 0;
        int i5 = 3;
        while (true) {
            int i6 = i5 - 1;
            long j3 = ((long) iArr[i + i5]) & j;
            j3 *= j3;
            i3--;
            iArr2[i2 + i3] = ((int) (j3 >>> 33)) | (i4 << 31);
            i3--;
            iArr2[i2 + i3] = (int) (j3 >>> 1);
            i4 = (int) j3;
            if (i6 <= 0) {
                long j4 = j2 * j2;
                long j5 = (((long) (i4 << 31)) & 4294967295L) | (j4 >>> 33);
                iArr2[i2 + 0] = (int) j4;
                long j6 = ((long) iArr[i + 1]) & 4294967295L;
                i5 = i2 + 2;
                j = ((long) iArr2[i5]) & 4294967295L;
                j5 += j6 * j2;
                int i7 = (int) j5;
                iArr2[i2 + 1] = (i7 << 1) | (((int) (j4 >>> 32)) & 1);
                j += j5 >>> 32;
                j5 = ((long) iArr[i + 2]) & 4294967295L;
                int i8 = i2 + 3;
                int i9 = i2 + 4;
                long j7 = ((long) iArr2[i8]) & 4294967295L;
                long j8 = ((long) iArr2[i9]) & 4294967295L;
                j += j5 * j2;
                int i10 = (int) j;
                iArr2[i5] = (i7 >>> 31) | (i10 << 1);
                j = j7 + ((j >>> 32) + (j5 * j6));
                j8 += j >>> 32;
                long j9 = ((long) iArr[i + 3]) & 4294967295L;
                int i11 = i2 + 5;
                long j10 = j5;
                j5 = (((long) iArr2[i11]) & 4294967295L) + (j8 >>> 32);
                int i12 = i2 + 6;
                long j11 = j8 & 4294967295L;
                j8 = (((long) iArr2[i12]) & 4294967295L) + (j5 >>> 32);
                j4 = 4294967295L & j5;
                j = (j & 4294967295L) + (j2 * j9);
                int i13 = (int) j;
                iArr2[i8] = (i10 >>> 31) | (i13 << 1);
                i10 = i13 >>> 31;
                j2 = j11 + ((j >>> 32) + (j6 * j9));
                j4 += (j2 >>> 32) + (j9 * j10);
                j8 += j4 >>> 32;
                i13 = (int) j2;
                iArr2[i9] = i10 | (i13 << 1);
                i10 = i13 >>> 31;
                i13 = (int) j4;
                iArr2[i11] = i10 | (i13 << 1);
                i10 = i13 >>> 31;
                i13 = (int) j8;
                iArr2[i12] = i10 | (i13 << 1);
                i10 = i13 >>> 31;
                i13 = i2 + 7;
                iArr2[i13] = ((iArr2[i13] + ((int) (j8 >>> 32))) << 1) | i10;
                return;
            }
            i5 = i6;
            j = 4294967295L;
        }
    }

    public static void square(int[] iArr, int[] iArr2) {
        long j = ((long) iArr[0]) & 4294967295L;
        int i = 0;
        int i2 = 8;
        int i3 = 3;
        while (true) {
            int i4 = i3 - 1;
            long j2 = ((long) iArr[i3]) & 4294967295L;
            j2 *= j2;
            i2--;
            iArr2[i2] = (i << 31) | ((int) (j2 >>> 33));
            i2--;
            iArr2[i2] = (int) (j2 >>> 1);
            int i5 = (int) j2;
            if (i4 <= 0) {
                long j3 = j * j;
                long j4 = (j3 >>> 33) | (((long) (i5 << 31)) & 4294967295L);
                iArr2[0] = (int) j3;
                j2 = ((long) iArr[1]) & 4294967295L;
                long j5 = ((long) iArr2[2]) & 4294967295L;
                j4 += j2 * j;
                int i6 = (int) j4;
                iArr2[1] = (i6 << 1) | (((int) (j3 >>> 32)) & 1);
                j5 += j4 >>> 32;
                j4 = ((long) iArr[2]) & 4294967295L;
                long j6 = j;
                long j7 = ((long) iArr2[3]) & 4294967295L;
                long j8 = ((long) iArr2[4]) & 4294967295L;
                j5 += j4 * j6;
                i3 = (int) j5;
                iArr2[2] = (i6 >>> 31) | (i3 << 1);
                i6 = i3 >>> 31;
                j5 = j7 + ((j5 >>> 32) + (j4 * j2));
                j8 += j5 >>> 32;
                long j9 = j4;
                long j10 = ((long) iArr[3]) & 4294967295L;
                long j11 = (((long) iArr2[5]) & 4294967295L) + (j8 >>> 32);
                long j12 = j8 & 4294967295L;
                j8 = (((long) iArr2[6]) & 4294967295L) + (j11 >>> 32);
                j11 &= 4294967295L;
                j5 = (j5 & 4294967295L) + (j6 * j10);
                int i7 = (int) j5;
                iArr2[3] = (i7 << 1) | i6;
                j2 = j12 + ((j5 >>> 32) + (j2 * j10));
                j11 += (j2 >>> 32) + (j10 * j9);
                j8 += j11 >>> 32;
                long j13 = 4294967295L & j11;
                i6 = (int) j2;
                iArr2[4] = (i7 >>> 31) | (i6 << 1);
                int i8 = (int) j13;
                iArr2[5] = (i6 >>> 31) | (i8 << 1);
                i7 = i8 >>> 31;
                i8 = (int) j8;
                iArr2[6] = i7 | (i8 << 1);
                iArr2[7] = (i8 >>> 31) | ((iArr2[7] + ((int) (j8 >>> 32))) << 1);
                return;
            }
            i3 = i4;
            i = i5;
        }
    }

    public static int sub(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = 0 + ((((long) iArr[i + 0]) & 4294967295L) - (((long) iArr2[i2 + 0]) & 4294967295L));
        iArr3[i3 + 0] = (int) j;
        j = (j >> 32) + ((((long) iArr[i + 1]) & 4294967295L) - (((long) iArr2[i2 + 1]) & 4294967295L));
        iArr3[i3 + 1] = (int) j;
        j = (j >> 32) + ((((long) iArr[i + 2]) & 4294967295L) - (((long) iArr2[i2 + 2]) & 4294967295L));
        iArr3[i3 + 2] = (int) j;
        j = (j >> 32) + ((((long) iArr[i + 3]) & 4294967295L) - (((long) iArr2[i2 + 3]) & 4294967295L));
        iArr3[i3 + 3] = (int) j;
        return (int) (j >> 32);
    }

    public static int sub(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + ((((long) iArr[0]) & 4294967295L) - (((long) iArr2[0]) & 4294967295L));
        iArr3[0] = (int) j;
        long j2 = (j >> 32) + ((((long) iArr[1]) & 4294967295L) - (((long) iArr2[1]) & 4294967295L));
        iArr3[1] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr[2]) & 4294967295L) - (((long) iArr2[2]) & 4294967295L));
        iArr3[2] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr[3]) & 4294967295L) - (((long) iArr2[3]) & 4294967295L));
        iArr3[3] = (int) j2;
        return (int) (j2 >> 32);
    }

    public static int subBothFrom(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + (((((long) iArr3[0]) & 4294967295L) - (((long) iArr[0]) & 4294967295L)) - (((long) iArr2[0]) & 4294967295L));
        iArr3[0] = (int) j;
        long j2 = (j >> 32) + (((((long) iArr3[1]) & 4294967295L) - (((long) iArr[1]) & 4294967295L)) - (((long) iArr2[1]) & 4294967295L));
        iArr3[1] = (int) j2;
        j2 = (j2 >> 32) + (((((long) iArr3[2]) & 4294967295L) - (((long) iArr[2]) & 4294967295L)) - (((long) iArr2[2]) & 4294967295L));
        iArr3[2] = (int) j2;
        j2 = (j2 >> 32) + (((((long) iArr3[3]) & 4294967295L) - (((long) iArr[3]) & 4294967295L)) - (((long) iArr2[3]) & 4294967295L));
        iArr3[3] = (int) j2;
        return (int) (j2 >> 32);
    }

    public static int subFrom(int[] iArr, int i, int[] iArr2, int i2) {
        int i3 = i2 + 0;
        long j = 0 + ((((long) iArr2[i3]) & 4294967295L) - (((long) iArr[i + 0]) & 4294967295L));
        iArr2[i3] = (int) j;
        long j2 = j >> 32;
        int i4 = i2 + 1;
        j2 += (((long) iArr2[i4]) & 4294967295L) - (((long) iArr[i + 1]) & 4294967295L);
        iArr2[i4] = (int) j2;
        i4 = i2 + 2;
        j2 = (j2 >> 32) + ((((long) iArr2[i4]) & 4294967295L) - (((long) iArr[i + 2]) & 4294967295L));
        iArr2[i4] = (int) j2;
        i2 += 3;
        j2 = (j2 >> 32) + ((((long) iArr2[i2]) & 4294967295L) - (((long) iArr[i + 3]) & 4294967295L));
        iArr2[i2] = (int) j2;
        return (int) (j2 >> 32);
    }

    public static int subFrom(int[] iArr, int[] iArr2) {
        long j = 0 + ((((long) iArr2[0]) & 4294967295L) - (((long) iArr[0]) & 4294967295L));
        iArr2[0] = (int) j;
        long j2 = (j >> 32) + ((((long) iArr2[1]) & 4294967295L) - (((long) iArr[1]) & 4294967295L));
        iArr2[1] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr2[2]) & 4294967295L) - (((long) iArr[2]) & 4294967295L));
        iArr2[2] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr2[3]) & 4294967295L) - (4294967295L & ((long) iArr[3])));
        iArr2[3] = (int) j2;
        return (int) (j2 >> 32);
    }

    public static BigInteger toBigInteger(int[] iArr) {
        byte[] bArr = new byte[16];
        for (int i = 0; i < 4; i++) {
            int i2 = iArr[i];
            if (i2 != 0) {
                Pack.intToBigEndian(i2, bArr, (3 - i) << 2);
            }
        }
        return new BigInteger(1, bArr);
    }

    public static BigInteger toBigInteger64(long[] jArr) {
        byte[] bArr = new byte[16];
        for (int i = 0; i < 2; i++) {
            long j = jArr[i];
            if (j != 0) {
                Pack.longToBigEndian(j, bArr, (1 - i) << 3);
            }
        }
        return new BigInteger(1, bArr);
    }

    public static void zero(int[] iArr) {
        iArr[0] = 0;
        iArr[1] = 0;
        iArr[2] = 0;
        iArr[3] = 0;
    }
}
