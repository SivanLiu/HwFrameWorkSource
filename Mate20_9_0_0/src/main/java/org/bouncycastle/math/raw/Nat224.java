package org.bouncycastle.math.raw;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.util.Pack;

public abstract class Nat224 {
    private static final long M = 4294967295L;

    public static int add(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = 0 + ((((long) iArr[i + 0]) & 4294967295L) + (((long) iArr2[i2 + 0]) & 4294967295L));
        iArr3[i3 + 0] = (int) j;
        j = (j >>> 32) + ((((long) iArr[i + 1]) & 4294967295L) + (((long) iArr2[i2 + 1]) & 4294967295L));
        iArr3[i3 + 1] = (int) j;
        j = (j >>> 32) + ((((long) iArr[i + 2]) & 4294967295L) + (((long) iArr2[i2 + 2]) & 4294967295L));
        iArr3[i3 + 2] = (int) j;
        j = (j >>> 32) + ((((long) iArr[i + 3]) & 4294967295L) + (((long) iArr2[i2 + 3]) & 4294967295L));
        iArr3[i3 + 3] = (int) j;
        j = (j >>> 32) + ((((long) iArr[i + 4]) & 4294967295L) + (((long) iArr2[i2 + 4]) & 4294967295L));
        iArr3[i3 + 4] = (int) j;
        j = (j >>> 32) + ((((long) iArr[i + 5]) & 4294967295L) + (((long) iArr2[i2 + 5]) & 4294967295L));
        iArr3[i3 + 5] = (int) j;
        j = (j >>> 32) + ((((long) iArr[i + 6]) & 4294967295L) + (((long) iArr2[i2 + 6]) & 4294967295L));
        iArr3[i3 + 6] = (int) j;
        return (int) (j >>> 32);
    }

    public static int add(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + ((((long) iArr[0]) & 4294967295L) + (((long) iArr2[0]) & 4294967295L));
        iArr3[0] = (int) j;
        long j2 = (j >>> 32) + ((((long) iArr[1]) & 4294967295L) + (((long) iArr2[1]) & 4294967295L));
        iArr3[1] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[2]) & 4294967295L) + (((long) iArr2[2]) & 4294967295L));
        iArr3[2] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[3]) & 4294967295L) + (((long) iArr2[3]) & 4294967295L));
        iArr3[3] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[4]) & 4294967295L) + (((long) iArr2[4]) & 4294967295L));
        iArr3[4] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[5]) & 4294967295L) + (((long) iArr2[5]) & 4294967295L));
        iArr3[5] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[6]) & 4294967295L) + (((long) iArr2[6]) & 4294967295L));
        iArr3[6] = (int) j2;
        return (int) (j2 >>> 32);
    }

    public static int addBothTo(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        int i4 = i3 + 0;
        long j = 0 + (((((long) iArr[i + 0]) & 4294967295L) + (((long) iArr2[i2 + 0]) & 4294967295L)) + (((long) iArr3[i4]) & 4294967295L));
        iArr3[i4] = (int) j;
        int i5 = i3 + 1;
        long j2 = (j >>> 32) + (((((long) iArr[i + 1]) & 4294967295L) + (((long) iArr2[i2 + 1]) & 4294967295L)) + (((long) iArr3[i5]) & 4294967295L));
        iArr3[i5] = (int) j2;
        i5 = i3 + 2;
        j2 = (j2 >>> 32) + (((((long) iArr[i + 2]) & 4294967295L) + (((long) iArr2[i2 + 2]) & 4294967295L)) + (((long) iArr3[i5]) & 4294967295L));
        iArr3[i5] = (int) j2;
        i5 = i3 + 3;
        j2 = (j2 >>> 32) + (((((long) iArr[i + 3]) & 4294967295L) + (((long) iArr2[i2 + 3]) & 4294967295L)) + (((long) iArr3[i5]) & 4294967295L));
        iArr3[i5] = (int) j2;
        i5 = i3 + 4;
        j2 = (j2 >>> 32) + (((((long) iArr[i + 4]) & 4294967295L) + (((long) iArr2[i2 + 4]) & 4294967295L)) + (((long) iArr3[i5]) & 4294967295L));
        iArr3[i5] = (int) j2;
        i5 = i3 + 5;
        j2 = (j2 >>> 32) + (((((long) iArr[i + 5]) & 4294967295L) + (((long) iArr2[i2 + 5]) & 4294967295L)) + (((long) iArr3[i5]) & 4294967295L));
        iArr3[i5] = (int) j2;
        i3 += 6;
        j2 = (j2 >>> 32) + (((((long) iArr[i + 6]) & 4294967295L) + (((long) iArr2[i2 + 6]) & 4294967295L)) + (((long) iArr3[i3]) & 4294967295L));
        iArr3[i3] = (int) j2;
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
        j2 = (j2 >>> 32) + (((((long) iArr[4]) & 4294967295L) + (((long) iArr2[4]) & 4294967295L)) + (((long) iArr3[4]) & 4294967295L));
        iArr3[4] = (int) j2;
        j2 = (j2 >>> 32) + (((((long) iArr[5]) & 4294967295L) + (((long) iArr2[5]) & 4294967295L)) + (((long) iArr3[5]) & 4294967295L));
        iArr3[5] = (int) j2;
        j2 = (j2 >>> 32) + (((((long) iArr[6]) & 4294967295L) + (((long) iArr2[6]) & 4294967295L)) + (((long) iArr3[6]) & 4294967295L));
        iArr3[6] = (int) j2;
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
        i4 = i2 + 3;
        j = (j >>> 32) + ((((long) iArr[i + 3]) & 4294967295L) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j;
        i4 = i2 + 4;
        j = (j >>> 32) + ((((long) iArr[i + 4]) & 4294967295L) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j;
        i4 = i2 + 5;
        j = (j >>> 32) + ((((long) iArr[i + 5]) & 4294967295L) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j;
        i2 += 6;
        j = (j >>> 32) + ((((long) iArr[i + 6]) & 4294967295L) + (4294967295L & ((long) iArr2[i2])));
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
        j2 = (j2 >>> 32) + ((((long) iArr[3]) & 4294967295L) + (((long) iArr2[3]) & 4294967295L));
        iArr2[3] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[4]) & 4294967295L) + (((long) iArr2[4]) & 4294967295L));
        iArr2[4] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[5]) & 4294967295L) + (((long) iArr2[5]) & 4294967295L));
        iArr2[5] = (int) j2;
        j2 = (j2 >>> 32) + ((((long) iArr[6]) & 4294967295L) + (4294967295L & ((long) iArr2[6])));
        iArr2[6] = (int) j2;
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
        i4 = i + 3;
        i6 = i2 + 3;
        j2 = (j2 >>> 32) + ((((long) iArr[i4]) & 4294967295L) + (((long) iArr2[i6]) & 4294967295L));
        i7 = (int) j2;
        iArr[i4] = i7;
        iArr2[i6] = i7;
        i4 = i + 4;
        i6 = i2 + 4;
        j2 = (j2 >>> 32) + ((((long) iArr[i4]) & 4294967295L) + (((long) iArr2[i6]) & 4294967295L));
        i7 = (int) j2;
        iArr[i4] = i7;
        iArr2[i6] = i7;
        i4 = i + 5;
        i6 = i2 + 5;
        j2 = (j2 >>> 32) + ((((long) iArr[i4]) & 4294967295L) + (((long) iArr2[i6]) & 4294967295L));
        i7 = (int) j2;
        iArr[i4] = i7;
        iArr2[i6] = i7;
        i += 6;
        i2 += 6;
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
        iArr2[i2 + 4] = iArr[i + 4];
        iArr2[i2 + 5] = iArr[i + 5];
        iArr2[i2 + 6] = iArr[i + 6];
    }

    public static void copy(int[] iArr, int[] iArr2) {
        iArr2[0] = iArr[0];
        iArr2[1] = iArr[1];
        iArr2[2] = iArr[2];
        iArr2[3] = iArr[3];
        iArr2[4] = iArr[4];
        iArr2[5] = iArr[5];
        iArr2[6] = iArr[6];
    }

    public static int[] create() {
        return new int[7];
    }

    public static int[] createExt() {
        return new int[14];
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
        for (int i = 6; i >= 0; i--) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static int[] fromBigInteger(BigInteger bigInteger) {
        if (bigInteger.signum() < 0 || bigInteger.bitLength() > 224) {
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

    public static int getBit(int[] iArr, int i) {
        int i2;
        if (i == 0) {
            i2 = iArr[0];
        } else {
            int i3 = i >> 5;
            if (i3 < 0 || i3 >= 7) {
                return 0;
            }
            i2 = iArr[i3] >>> (i & 31);
        }
        return i2 & 1;
    }

    public static boolean gte(int[] iArr, int i, int[] iArr2, int i2) {
        for (int i3 = 6; i3 >= 0; i3--) {
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
        for (int i = 6; i >= 0; i--) {
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
        for (int i = 1; i < 7; i++) {
            if (iArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isZero(int[] iArr) {
        for (int i = 0; i < 7; i++) {
            if (iArr[i] != 0) {
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
        long j5 = ((long) iArr2[i2 + 4]) & 4294967295L;
        long j6 = ((long) iArr2[i2 + 5]) & 4294967295L;
        long j7 = ((long) iArr2[i2 + 6]) & 4294967295L;
        long j8 = ((long) iArr[i + 0]) & 4294967295L;
        long j9 = 0 + (j8 * j);
        long j10 = j;
        iArr3[i3 + 0] = (int) j9;
        long j11 = (j9 >>> 32) + (j8 * j2);
        long j12 = j2;
        iArr3[i3 + 1] = (int) j11;
        j11 = (j11 >>> 32) + (j8 * j3);
        iArr3[i3 + 2] = (int) j11;
        j11 = (j11 >>> 32) + (j8 * j4);
        iArr3[i3 + 3] = (int) j11;
        j11 = (j11 >>> 32) + (j8 * j5);
        iArr3[i3 + 4] = (int) j11;
        j11 = (j11 >>> 32) + (j8 * j6);
        iArr3[i3 + 5] = (int) j11;
        j11 = (j11 >>> 32) + (j8 * j7);
        iArr3[i3 + 6] = (int) j11;
        iArr3[i3 + 7] = (int) (j11 >>> 32);
        int i4 = 1;
        int i5 = i3;
        int i6 = 1;
        while (i6 < 7) {
            i5 += i4;
            j2 = ((long) iArr[i + i6]) & 4294967295L;
            int i7 = i5 + 0;
            j = 0 + ((j2 * j10) + (((long) iArr3[i7]) & 4294967295L));
            iArr3[i7] = (int) j;
            i7 = i5 + 1;
            int i8 = i6;
            j = (j >>> 32) + ((j2 * j12) + (((long) iArr3[i7]) & 4294967295L));
            iArr3[i7] = (int) j;
            i6 = i5 + 2;
            j = (j >>> 32) + ((j2 * j3) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            i6 = i5 + 3;
            j = (j >>> 32) + ((j2 * j4) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            i6 = i5 + 4;
            j = (j >>> 32) + ((j2 * j5) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            i6 = i5 + 5;
            j = (j >>> 32) + ((j2 * j6) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            i6 = i5 + 6;
            j = (j >>> 32) + ((j2 * j7) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            iArr3[i5 + 7] = (int) (j >>> 32);
            i6 = i8 + 1;
            int i9 = 32;
            i4 = 1;
        }
    }

    public static void mul(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = ((long) iArr2[0]) & 4294967295L;
        long j2 = ((long) iArr2[2]) & 4294967295L;
        long j3 = ((long) iArr2[3]) & 4294967295L;
        long j4 = ((long) iArr2[1]) & 4294967295L;
        long j5 = ((long) iArr2[4]) & 4294967295L;
        long j6 = ((long) iArr2[5]) & 4294967295L;
        long j7 = ((long) iArr2[6]) & 4294967295L;
        long j8 = ((long) iArr[0]) & 4294967295L;
        long j9 = 0 + (j8 * j);
        long j10 = j;
        iArr3[0] = (int) j9;
        j = (j9 >>> 32) + (j8 * j4);
        int i = 1;
        iArr3[1] = (int) j;
        j = (j >>> 32) + (j8 * j2);
        iArr3[2] = (int) j;
        j = (j >>> 32) + (j8 * j3);
        iArr3[3] = (int) j;
        j = (j >>> 32) + (j8 * j5);
        iArr3[4] = (int) j;
        j = (j >>> 32) + (j8 * j6);
        iArr3[5] = (int) j;
        j = (j >>> 32) + (j8 * j7);
        iArr3[6] = (int) j;
        int i2 = 7;
        iArr3[7] = (int) (j >>> 32);
        while (i < i2) {
            j = ((long) iArr[i]) & 4294967295L;
            int i3 = i + 0;
            long j11 = j;
            long j12 = 0 + ((j * j10) + (((long) iArr3[i3]) & 4294967295L));
            iArr3[i3] = (int) j12;
            int i4 = i + 1;
            j12 = (j12 >>> 32) + ((j11 * j4) + (((long) iArr3[i4]) & 4294967295L));
            iArr3[i4] = (int) j12;
            i2 = i + 2;
            long j13 = j2;
            j12 = (j12 >>> 32) + ((j11 * j2) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j12;
            i2 = i + 3;
            j12 = (j12 >>> 32) + ((j11 * j3) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j12;
            i2 = i + 4;
            j12 = (j12 >>> 32) + ((j11 * j5) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j12;
            i2 = i + 5;
            j12 = (j12 >>> 32) + ((j11 * j6) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j12;
            i2 = i + 6;
            j12 = (j12 >>> 32) + ((j11 * j7) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j12;
            iArr3[i + 7] = (int) (j12 >>> 32);
            int i5 = 32;
            i = i4;
            j2 = j13;
            i2 = 7;
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
        long j7 = ((long) iArr[i2 + 4]) & 4294967295L;
        long j8 = (j3 >>> 32) + (((j * j7) + j6) + (((long) iArr2[i3 + 4]) & 4294967295L));
        iArr3[i4 + 4] = (int) j8;
        j6 = ((long) iArr[i2 + 5]) & 4294967295L;
        j8 = (j8 >>> 32) + (((j * j6) + j7) + (((long) iArr2[i3 + 5]) & 4294967295L));
        iArr3[i4 + 5] = (int) j8;
        j7 = ((long) iArr[i2 + 6]) & 4294967295L;
        j8 = (j8 >>> 32) + (((j * j7) + j6) + (4294967295L & ((long) iArr2[i3 + 6])));
        iArr3[i4 + 6] = (int) j8;
        return (j8 >>> 32) + j7;
    }

    public static int mul33DWordAdd(int i, long j, int[] iArr, int i2) {
        int[] iArr2 = iArr;
        int i3 = i2;
        long j2 = ((long) i) & 4294967295L;
        long j3 = j & 4294967295L;
        int i4 = i3 + 0;
        long j4 = ((j2 * j3) + (((long) iArr2[i4]) & 4294967295L)) + 0;
        iArr2[i4] = (int) j4;
        long j5 = j >>> 32;
        j2 = (j2 * j5) + j3;
        int i5 = i3 + 1;
        j4 = (j4 >>> 32) + (j2 + (((long) iArr2[i5]) & 4294967295L));
        iArr2[i5] = (int) j4;
        i5 = i3 + 2;
        j2 = (j4 >>> 32) + (j5 + (((long) iArr2[i5]) & 4294967295L));
        iArr2[i5] = (int) j2;
        j5 = j2 >>> 32;
        int i6 = i3 + 3;
        j5 += ((long) iArr2[i6]) & 4294967295L;
        iArr2[i6] = (int) j5;
        return (j5 >>> 32) == 0 ? 0 : Nat.incAt(7, iArr2, i3, 4);
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
        return (j >>> 32) == 0 ? 0 : Nat.incAt(7, iArr, i3, 3);
    }

    public static int mulAddTo(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = ((long) iArr2[i2 + 0]) & 4294967295L;
        long j2 = ((long) iArr2[i2 + 1]) & 4294967295L;
        long j3 = ((long) iArr2[i2 + 2]) & 4294967295L;
        long j4 = ((long) iArr2[i2 + 3]) & 4294967295L;
        long j5 = ((long) iArr2[i2 + 4]) & 4294967295L;
        long j6 = ((long) iArr2[i2 + 5]) & 4294967295L;
        long j7 = ((long) iArr2[i2 + 6]) & 4294967295L;
        int i4 = 0;
        int i5 = i3;
        long j8 = 0;
        while (i4 < 7) {
            long j9 = ((long) iArr[i + i4]) & 4294967295L;
            int i6 = i5 + 0;
            long j10 = j;
            j = 0 + ((j9 * j) + (((long) iArr3[i6]) & 4294967295L));
            int i7 = i4;
            iArr3[i6] = (int) j;
            i6 = i5 + 1;
            long j11 = j2;
            j = (j >>> 32) + ((j9 * j2) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            int i8 = i5 + 2;
            j = (j >>> 32) + ((j9 * j3) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j;
            i8 = i5 + 3;
            j = (j >>> 32) + ((j9 * j4) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j;
            i8 = i5 + 4;
            j = (j >>> 32) + ((j9 * j5) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j;
            i8 = i5 + 5;
            j = (j >>> 32) + ((j9 * j6) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j;
            i8 = i5 + 6;
            j = (j >>> 32) + ((j9 * j7) + (((long) iArr3[i8]) & 4294967295L));
            iArr3[i8] = (int) j;
            i5 += 7;
            long j12 = j7;
            j = (j >>> 32) + (j8 + (((long) iArr3[i5]) & 4294967295L));
            iArr3[i5] = (int) j;
            j8 = j >>> 32;
            i4 = i7 + 1;
            i5 = i6;
            j = j10;
            j2 = j11;
            j7 = j12;
        }
        return (int) j8;
    }

    public static int mulAddTo(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = ((long) iArr2[1]) & 4294967295L;
        long j2 = ((long) iArr2[2]) & 4294967295L;
        long j3 = ((long) iArr2[3]) & 4294967295L;
        long j4 = ((long) iArr2[4]) & 4294967295L;
        long j5 = ((long) iArr2[0]) & 4294967295L;
        long j6 = ((long) iArr2[5]) & 4294967295L;
        long j7 = ((long) iArr2[6]) & 4294967295L;
        long j8 = 0;
        int i = 0;
        while (i < 7) {
            long j9 = j7;
            j7 = ((long) iArr[i]) & 4294967295L;
            int i2 = i + 0;
            long j10 = j4;
            j4 = 0 + ((j7 * j5) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j4;
            i2 = i + 1;
            long j11 = j;
            j4 = (j4 >>> 32) + ((j7 * j) + (((long) iArr3[i2]) & 4294967295L));
            iArr3[i2] = (int) j4;
            int i3 = i + 2;
            long j12 = j2;
            j = (j4 >>> 32) + ((j7 * j2) + (((long) iArr3[i3]) & 4294967295L));
            iArr3[i3] = (int) j;
            int i4 = i + 3;
            j = (j >>> 32) + ((j7 * j3) + (((long) iArr3[i4]) & 4294967295L));
            iArr3[i4] = (int) j;
            int i5 = i + 4;
            long j13 = j3;
            j = (j >>> 32) + ((j7 * j10) + (((long) iArr3[i5]) & 4294967295L));
            iArr3[i5] = (int) j;
            int i6 = i + 5;
            j = (j >>> 32) + ((j7 * j6) + (((long) iArr3[i6]) & 4294967295L));
            iArr3[i6] = (int) j;
            i5 = i + 6;
            j = (j >>> 32) + ((j7 * j9) + (((long) iArr3[i5]) & 4294967295L));
            iArr3[i5] = (int) j;
            i += 7;
            j7 = (j >>> 32) + (j8 + (((long) iArr3[i]) & 4294967295L));
            iArr3[i] = (int) j7;
            j8 = j7 >>> 32;
            j7 = j9;
            i = i2;
            j4 = j10;
            j = j11;
            j2 = j12;
            j3 = j13;
        }
        return (int) j8;
    }

    public static int mulByWord(int i, int[] iArr) {
        long j = ((long) i) & 4294967295L;
        long j2 = 0 + ((((long) iArr[0]) & 4294967295L) * j);
        iArr[0] = (int) j2;
        long j3 = (j2 >>> 32) + ((((long) iArr[1]) & 4294967295L) * j);
        iArr[1] = (int) j3;
        j3 = (j3 >>> 32) + ((((long) iArr[2]) & 4294967295L) * j);
        iArr[2] = (int) j3;
        j3 = (j3 >>> 32) + ((((long) iArr[3]) & 4294967295L) * j);
        iArr[3] = (int) j3;
        j3 = (j3 >>> 32) + ((((long) iArr[4]) & 4294967295L) * j);
        iArr[4] = (int) j3;
        j3 = (j3 >>> 32) + ((((long) iArr[5]) & 4294967295L) * j);
        iArr[5] = (int) j3;
        j3 = (j3 >>> 32) + (j * (4294967295L & ((long) iArr[6])));
        iArr[6] = (int) j3;
        return (int) (j3 >>> 32);
    }

    public static int mulByWordAddTo(int i, int[] iArr, int[] iArr2) {
        long j = ((long) i) & 4294967295L;
        long j2 = 0 + (((((long) iArr2[0]) & 4294967295L) * j) + (((long) iArr[0]) & 4294967295L));
        iArr2[0] = (int) j2;
        long j3 = (j2 >>> 32) + (((((long) iArr2[1]) & 4294967295L) * j) + (((long) iArr[1]) & 4294967295L));
        iArr2[1] = (int) j3;
        j3 = (j3 >>> 32) + (((((long) iArr2[2]) & 4294967295L) * j) + (((long) iArr[2]) & 4294967295L));
        iArr2[2] = (int) j3;
        j3 = (j3 >>> 32) + (((((long) iArr2[3]) & 4294967295L) * j) + (((long) iArr[3]) & 4294967295L));
        iArr2[3] = (int) j3;
        j3 = (j3 >>> 32) + (((((long) iArr2[4]) & 4294967295L) * j) + (((long) iArr[4]) & 4294967295L));
        iArr2[4] = (int) j3;
        j3 = (j3 >>> 32) + (((((long) iArr2[5]) & 4294967295L) * j) + (((long) iArr[5]) & 4294967295L));
        iArr2[5] = (int) j3;
        j3 = (j3 >>> 32) + ((j * (((long) iArr2[6]) & 4294967295L)) + (4294967295L & ((long) iArr[6])));
        iArr2[6] = (int) j3;
        return (int) (j3 >>> 32);
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
        } while (i < 7);
        return (int) j2;
    }

    public static int mulWordAddTo(int i, int[] iArr, int i2, int[] iArr2, int i3) {
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
        i4 = i3 + 3;
        j3 = (j3 >>> 32) + (((((long) iArr[i2 + 3]) & 4294967295L) * j) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j3;
        i4 = i3 + 4;
        j3 = (j3 >>> 32) + (((((long) iArr[i2 + 4]) & 4294967295L) * j) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j3;
        i4 = i3 + 5;
        j3 = (j3 >>> 32) + (((((long) iArr[i2 + 5]) & 4294967295L) * j) + (((long) iArr2[i4]) & 4294967295L));
        iArr2[i4] = (int) j3;
        i3 += 6;
        j3 = (j3 >>> 32) + ((j * (((long) iArr[i2 + 6]) & 4294967295L)) + (((long) iArr2[i3]) & 4294967295L));
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
        return (j >>> 32) == 0 ? 0 : Nat.incAt(7, iArr, i2, 3);
    }

    public static void square(int[] iArr, int i, int[] iArr2, int i2) {
        long j = 4294967295L;
        long j2 = ((long) iArr[i + 0]) & 4294967295L;
        int i3 = 14;
        int i4 = 0;
        int i5 = 6;
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
                j5 &= 4294967295L;
                j = (j & 4294967295L) + (j9 * j2);
                int i13 = (int) j;
                iArr2[i8] = (i10 >>> 31) | (i13 << 1);
                i10 = i13 >>> 31;
                j = j11 + ((j >>> 32) + (j9 * j6));
                j5 += (j >>> 32) + (j9 * j10);
                j8 += j5 >>> 32;
                long j12 = j9;
                j9 = ((long) iArr[i + 4]) & 4294967295L;
                int i14 = i2 + 7;
                long j13 = j5 & 4294967295L;
                j5 = (((long) iArr2[i14]) & 4294967295L) + (j8 >>> 32);
                int i15 = i2 + 8;
                long j14 = j8 & 4294967295L;
                j8 = (((long) iArr2[i15]) & 4294967295L) + (j5 >>> 32);
                j5 &= 4294967295L;
                j = (j & 4294967295L) + (j9 * j2);
                i13 = (int) j;
                iArr2[i9] = i10 | (i13 << 1);
                i10 = i13 >>> 31;
                j = j13 + ((j >>> 32) + (j9 * j6));
                j4 = j14 + ((j >>> 32) + (j9 * j10));
                j5 += (j4 >>> 32) + (j9 * j12);
                j4 &= 4294967295L;
                j8 += j5 >>> 32;
                long j15 = j9;
                j9 = ((long) iArr[i + 5]) & 4294967295L;
                i9 = i2 + 9;
                long j16 = j5 & 4294967295L;
                j5 = (((long) iArr2[i9]) & 4294967295L) + (j8 >>> 32);
                int i16 = i2 + 10;
                long j17 = j8 & 4294967295L;
                j8 = (((long) iArr2[i16]) & 4294967295L) + (j5 >>> 32);
                j5 &= 4294967295L;
                j = (j & 4294967295L) + (j9 * j2);
                long j18 = j2;
                int i17 = (int) j;
                iArr2[i11] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                j4 += (j >>> 32) + (j9 * j6);
                j2 = j16 + ((j4 >>> 32) + (j9 * j10));
                long j19 = j17 + ((j2 >>> 32) + (j9 * j12));
                j2 &= 4294967295L;
                j5 += (j19 >>> 32) + (j9 * j15);
                j19 &= 4294967295L;
                j8 += j5 >>> 32;
                long j20 = j9;
                j9 = ((long) iArr[i + 6]) & 4294967295L;
                int i18 = i2 + 11;
                long j21 = j5 & 4294967295L;
                j5 = (((long) iArr2[i18]) & 4294967295L) + (j8 >>> 32);
                i8 = i2 + 12;
                long j22 = j8 & 4294967295L;
                j8 = (((long) iArr2[i8]) & 4294967295L) + (j5 >>> 32);
                j = 4294967295L & j5;
                j4 = (j4 & 4294967295L) + (j9 * j18);
                i3 = (int) j4;
                iArr2[i12] = i10 | (i3 << 1);
                i10 = i3 >>> 31;
                j2 += (j4 >>> 32) + (j6 * j9);
                j4 = ((j2 >>> 32) + (j9 * j10)) + j19;
                j5 = j21 + ((j4 >>> 32) + (j9 * j12));
                j6 = j22 + ((j5 >>> 32) + (j9 * j15));
                j += (j6 >>> 32) + (j9 * j20);
                j8 += j >>> 32;
                i17 = (int) j2;
                iArr2[i14] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                i17 = (int) j4;
                iArr2[i15] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                i17 = (int) j5;
                iArr2[i9] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                i17 = (int) j6;
                iArr2[i16] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                i17 = (int) j;
                iArr2[i18] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                i17 = (int) j8;
                iArr2[i8] = i10 | (i17 << 1);
                i10 = i17 >>> 31;
                i17 = i2 + 13;
                iArr2[i17] = ((iArr2[i17] + ((int) (j8 >>> 32))) << 1) | i10;
                return;
            }
            i5 = i6;
            j = 4294967295L;
        }
    }

    public static void square(int[] iArr, int[] iArr2) {
        long j = ((long) iArr[0]) & 4294967295L;
        int i = 0;
        int i2 = 14;
        int i3 = 6;
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
                long j6 = j2;
                long j7 = ((long) iArr2[3]) & 4294967295L;
                long j8 = ((long) iArr2[4]) & 4294967295L;
                j5 += j4 * j;
                int i7 = (int) j5;
                iArr2[2] = (i7 << 1) | (i6 >>> 31);
                i6 = i7 >>> 31;
                j3 = j7 + ((j5 >>> 32) + (j4 * j6));
                j8 += j3 >>> 32;
                long j9 = j;
                long j10 = ((long) iArr[3]) & 4294967295L;
                long j11 = j4;
                long j12 = (((long) iArr2[5]) & 4294967295L) + (j8 >>> 32);
                long j13 = j8 & 4294967295L;
                j8 = (((long) iArr2[6]) & 4294967295L) + (j12 >>> 32);
                j12 &= 4294967295L;
                j3 = (j3 & 4294967295L) + (j10 * j9);
                int i8 = (int) j3;
                iArr2[3] = i6 | (i8 << 1);
                j3 = j13 + ((j3 >>> 32) + (j10 * j6));
                j12 += (j3 >>> 32) + (j10 * j11);
                long j14 = j12 & 4294967295L;
                long j15 = j8 + (j12 >>> 32);
                j4 = ((long) iArr[4]) & 4294967295L;
                j8 = (((long) iArr2[7]) & 4294967295L) + (j15 >>> 32);
                long j16 = j15 & 4294967295L;
                long j17 = j10;
                j10 = (((long) iArr2[8]) & 4294967295L) + (j8 >>> 32);
                j8 &= 4294967295L;
                j3 = (j3 & 4294967295L) + (j4 * j9);
                int i9 = (int) j3;
                iArr2[4] = (i8 >>> 31) | (i9 << 1);
                i8 = i9 >>> 31;
                j14 += (j3 >>> 32) + (j4 * j6);
                j16 += (j14 >>> 32) + (j4 * j11);
                j8 += (j16 >>> 32) + (j4 * j17);
                j3 = j16 & 4294967295L;
                j10 += j8 >>> 32;
                long j18 = j4;
                j4 = ((long) iArr[5]) & 4294967295L;
                long j19 = j8 & 4294967295L;
                j8 = (((long) iArr2[9]) & 4294967295L) + (j10 >>> 32);
                long j20 = j10 & 4294967295L;
                j10 = (((long) iArr2[10]) & 4294967295L) + (j8 >>> 32);
                j8 &= 4294967295L;
                j14 = (j14 & 4294967295L) + (j4 * j9);
                i9 = (int) j14;
                iArr2[5] = i8 | (i9 << 1);
                i8 = i9 >>> 31;
                j3 += (j14 >>> 32) + (j4 * j6);
                long j21 = j19 + ((j3 >>> 32) + (j4 * j11));
                long j22 = j20 + ((j21 >>> 32) + (j4 * j17));
                j21 &= 4294967295L;
                j8 += (j22 >>> 32) + (j4 * j18);
                j22 &= 4294967295L;
                j10 += j8 >>> 32;
                long j23 = j4;
                j4 = ((long) iArr[6]) & 4294967295L;
                long j24 = j8 & 4294967295L;
                j8 = (((long) iArr2[11]) & 4294967295L) + (j10 >>> 32);
                long j25 = j10 & 4294967295L;
                j10 = (((long) iArr2[12]) & 4294967295L) + (j8 >>> 32);
                j14 = 4294967295L & j8;
                j3 = (j3 & 4294967295L) + (j4 * j9);
                i4 = (int) j3;
                iArr2[6] = i8 | (i4 << 1);
                i8 = i4 >>> 31;
                j21 += (j3 >>> 32) + (j4 * j6);
                j8 = ((j21 >>> 32) + (j4 * j11)) + j22;
                j3 = j24 + ((j8 >>> 32) + (j4 * j17));
                long j26 = j3;
                j3 = j25 + ((j3 >>> 32) + (j4 * j18));
                j14 += (j3 >>> 32) + (j4 * j23);
                j10 += j14 >>> 32;
                int i10 = (int) j21;
                iArr2[7] = i8 | (i10 << 1);
                i8 = (int) j8;
                iArr2[8] = (i10 >>> 31) | (i8 << 1);
                i10 = i8 >>> 31;
                i8 = (int) j26;
                iArr2[9] = i10 | (i8 << 1);
                i10 = i8 >>> 31;
                i8 = (int) j3;
                iArr2[10] = i10 | (i8 << 1);
                i10 = i8 >>> 31;
                i8 = (int) j14;
                iArr2[11] = i10 | (i8 << 1);
                i10 = i8 >>> 31;
                i8 = (int) j10;
                iArr2[12] = i10 | (i8 << 1);
                iArr2[13] = (i8 >>> 31) | ((iArr2[13] + ((int) (j10 >>> 32))) << 1);
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
        j = (j >> 32) + ((((long) iArr[i + 4]) & 4294967295L) - (((long) iArr2[i2 + 4]) & 4294967295L));
        iArr3[i3 + 4] = (int) j;
        j = (j >> 32) + ((((long) iArr[i + 5]) & 4294967295L) - (((long) iArr2[i2 + 5]) & 4294967295L));
        iArr3[i3 + 5] = (int) j;
        j = (j >> 32) + ((((long) iArr[i + 6]) & 4294967295L) - (((long) iArr2[i2 + 6]) & 4294967295L));
        iArr3[i3 + 6] = (int) j;
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
        j2 = (j2 >> 32) + ((((long) iArr[4]) & 4294967295L) - (((long) iArr2[4]) & 4294967295L));
        iArr3[4] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr[5]) & 4294967295L) - (((long) iArr2[5]) & 4294967295L));
        iArr3[5] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr[6]) & 4294967295L) - (((long) iArr2[6]) & 4294967295L));
        iArr3[6] = (int) j2;
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
        j2 = (j2 >> 32) + (((((long) iArr3[4]) & 4294967295L) - (((long) iArr[4]) & 4294967295L)) - (((long) iArr2[4]) & 4294967295L));
        iArr3[4] = (int) j2;
        j2 = (j2 >> 32) + (((((long) iArr3[5]) & 4294967295L) - (((long) iArr[5]) & 4294967295L)) - (((long) iArr2[5]) & 4294967295L));
        iArr3[5] = (int) j2;
        j2 = (j2 >> 32) + (((((long) iArr3[6]) & 4294967295L) - (((long) iArr[6]) & 4294967295L)) - (((long) iArr2[6]) & 4294967295L));
        iArr3[6] = (int) j2;
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
        i4 = i2 + 3;
        j2 = (j2 >> 32) + ((((long) iArr2[i4]) & 4294967295L) - (((long) iArr[i + 3]) & 4294967295L));
        iArr2[i4] = (int) j2;
        i4 = i2 + 4;
        j2 = (j2 >> 32) + ((((long) iArr2[i4]) & 4294967295L) - (((long) iArr[i + 4]) & 4294967295L));
        iArr2[i4] = (int) j2;
        i4 = i2 + 5;
        j2 = (j2 >> 32) + ((((long) iArr2[i4]) & 4294967295L) - (((long) iArr[i + 5]) & 4294967295L));
        iArr2[i4] = (int) j2;
        i2 += 6;
        j2 = (j2 >> 32) + ((((long) iArr2[i2]) & 4294967295L) - (((long) iArr[i + 6]) & 4294967295L));
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
        j2 = (j2 >> 32) + ((((long) iArr2[3]) & 4294967295L) - (((long) iArr[3]) & 4294967295L));
        iArr2[3] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr2[4]) & 4294967295L) - (((long) iArr[4]) & 4294967295L));
        iArr2[4] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr2[5]) & 4294967295L) - (((long) iArr[5]) & 4294967295L));
        iArr2[5] = (int) j2;
        j2 = (j2 >> 32) + ((((long) iArr2[6]) & 4294967295L) - (4294967295L & ((long) iArr[6])));
        iArr2[6] = (int) j2;
        return (int) (j2 >> 32);
    }

    public static BigInteger toBigInteger(int[] iArr) {
        byte[] bArr = new byte[28];
        for (int i = 0; i < 7; i++) {
            int i2 = iArr[i];
            if (i2 != 0) {
                Pack.intToBigEndian(i2, bArr, (6 - i) << 2);
            }
        }
        return new BigInteger(1, bArr);
    }

    public static void zero(int[] iArr) {
        iArr[0] = 0;
        iArr[1] = 0;
        iArr[2] = 0;
        iArr[3] = 0;
        iArr[4] = 0;
        iArr[5] = 0;
        iArr[6] = 0;
    }
}
