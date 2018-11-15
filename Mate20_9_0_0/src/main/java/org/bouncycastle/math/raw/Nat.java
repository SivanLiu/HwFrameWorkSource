package org.bouncycastle.math.raw;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.util.Pack;

public abstract class Nat {
    private static final long M = 4294967295L;

    public static int add(int i, int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j += (((long) iArr[i2]) & 4294967295L) + (4294967295L & ((long) iArr2[i2]));
            iArr3[i2] = (int) j;
            j >>>= 32;
        }
        return (int) j;
    }

    public static int add33At(int i, int i2, int[] iArr, int i3) {
        int i4 = i3 + 0;
        long j = (((long) iArr[i4]) & 4294967295L) + (((long) i2) & 4294967295L);
        iArr[i4] = (int) j;
        int i5 = i3 + 1;
        long j2 = (j >>> 32) + ((4294967295L & ((long) iArr[i5])) + 1);
        iArr[i5] = (int) j2;
        return (j2 >>> 32) == 0 ? 0 : incAt(i, iArr, i3 + 2);
    }

    public static int add33At(int i, int i2, int[] iArr, int i3, int i4) {
        int i5 = i3 + i4;
        long j = (((long) iArr[i5]) & 4294967295L) + (((long) i2) & 4294967295L);
        iArr[i5] = (int) j;
        i5++;
        j = (j >>> 32) + ((4294967295L & ((long) iArr[i5])) + 1);
        iArr[i5] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, i3, i4 + 2);
    }

    public static int add33To(int i, int i2, int[] iArr) {
        long j = (((long) iArr[0]) & 4294967295L) + (((long) i2) & 4294967295L);
        iArr[0] = (int) j;
        j = (j >>> 32) + ((4294967295L & ((long) iArr[1])) + 1);
        iArr[1] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, 2);
    }

    public static int add33To(int i, int i2, int[] iArr, int i3) {
        int i4 = i3 + 0;
        long j = (((long) iArr[i4]) & 4294967295L) + (((long) i2) & 4294967295L);
        iArr[i4] = (int) j;
        int i5 = i3 + 1;
        long j2 = (j >>> 32) + ((4294967295L & ((long) iArr[i5])) + 1);
        iArr[i5] = (int) j2;
        return (j2 >>> 32) == 0 ? 0 : incAt(i, iArr, i3, 2);
    }

    public static int addBothTo(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        int i5 = 0;
        long j = 0;
        int i6 = i;
        while (i5 < i6) {
            int i7 = i4 + i5;
            j += ((((long) iArr[i2 + i5]) & 4294967295L) + (((long) iArr2[i3 + i5]) & 4294967295L)) + (((long) iArr3[i7]) & 4294967295L);
            iArr3[i7] = (int) j;
            j >>>= 32;
            i5++;
            i6 = i;
        }
        return (int) j;
    }

    public static int addBothTo(int i, int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j += ((((long) iArr[i2]) & 4294967295L) + (((long) iArr2[i2]) & 4294967295L)) + (4294967295L & ((long) iArr3[i2]));
            iArr3[i2] = (int) j;
            j >>>= 32;
        }
        return (int) j;
    }

    public static int addDWordAt(int i, long j, int[] iArr, int i2) {
        int i3 = i2 + 0;
        long j2 = (((long) iArr[i3]) & 4294967295L) + (j & 4294967295L);
        iArr[i3] = (int) j2;
        int i4 = i2 + 1;
        j2 = (j2 >>> 32) + ((4294967295L & ((long) iArr[i4])) + (j >>> 32));
        iArr[i4] = (int) j2;
        return (j2 >>> 32) == 0 ? 0 : incAt(i, iArr, i2 + 2);
    }

    public static int addDWordAt(int i, long j, int[] iArr, int i2, int i3) {
        int i4 = i2 + i3;
        long j2 = (((long) iArr[i4]) & 4294967295L) + (j & 4294967295L);
        iArr[i4] = (int) j2;
        i4++;
        j2 = (j2 >>> 32) + ((4294967295L & ((long) iArr[i4])) + (j >>> 32));
        iArr[i4] = (int) j2;
        return (j2 >>> 32) == 0 ? 0 : incAt(i, iArr, i2, i3 + 2);
    }

    public static int addDWordTo(int i, long j, int[] iArr) {
        long j2 = (((long) iArr[0]) & 4294967295L) + (j & 4294967295L);
        iArr[0] = (int) j2;
        j2 = (j2 >>> 32) + ((4294967295L & ((long) iArr[1])) + (j >>> 32));
        iArr[1] = (int) j2;
        return (j2 >>> 32) == 0 ? 0 : incAt(i, iArr, 2);
    }

    public static int addDWordTo(int i, long j, int[] iArr, int i2) {
        int i3 = i2 + 0;
        long j2 = (((long) iArr[i3]) & 4294967295L) + (j & 4294967295L);
        iArr[i3] = (int) j2;
        int i4 = i2 + 1;
        j2 = (j2 >>> 32) + ((4294967295L & ((long) iArr[i4])) + (j >>> 32));
        iArr[i4] = (int) j2;
        return (j2 >>> 32) == 0 ? 0 : incAt(i, iArr, i2, 2);
    }

    public static int addTo(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        long j = 0;
        for (int i4 = 0; i4 < i; i4++) {
            int i5 = i3 + i4;
            j += (((long) iArr[i2 + i4]) & 4294967295L) + (4294967295L & ((long) iArr2[i5]));
            iArr2[i5] = (int) j;
            j >>>= 32;
        }
        return (int) j;
    }

    public static int addTo(int i, int[] iArr, int[] iArr2) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j += (((long) iArr[i2]) & 4294967295L) + (4294967295L & ((long) iArr2[i2]));
            iArr2[i2] = (int) j;
            j >>>= 32;
        }
        return (int) j;
    }

    public static int addWordAt(int i, int i2, int[] iArr, int i3) {
        long j = (((long) i2) & 4294967295L) + (4294967295L & ((long) iArr[i3]));
        iArr[i3] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, i3 + 1);
    }

    public static int addWordAt(int i, int i2, int[] iArr, int i3, int i4) {
        i2 = i3 + i4;
        long j = (((long) i2) & 4294967295L) + (4294967295L & ((long) iArr[i2]));
        iArr[i2] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, i3, i4 + 1);
    }

    public static int addWordTo(int i, int i2, int[] iArr) {
        long j = (((long) i2) & 4294967295L) + (4294967295L & ((long) iArr[0]));
        iArr[0] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, 1);
    }

    public static int addWordTo(int i, int i2, int[] iArr, int i3) {
        long j = (((long) i2) & 4294967295L) + (4294967295L & ((long) iArr[i3]));
        iArr[i3] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, i3, 1);
    }

    public static void copy(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        System.arraycopy(iArr, i2, iArr2, i3, i);
    }

    public static void copy(int i, int[] iArr, int[] iArr2) {
        System.arraycopy(iArr, 0, iArr2, 0, i);
    }

    public static int[] copy(int i, int[] iArr) {
        Object obj = new int[i];
        System.arraycopy(iArr, 0, obj, 0, i);
        return obj;
    }

    public static int[] create(int i) {
        return new int[i];
    }

    public static long[] create64(int i) {
        return new long[i];
    }

    public static int dec(int i, int[] iArr) {
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = iArr[i2] - 1;
            iArr[i2] = i3;
            if (i3 != -1) {
                return 0;
            }
        }
        return -1;
    }

    public static int dec(int i, int[] iArr, int[] iArr2) {
        int i2 = 0;
        while (i2 < i) {
            int i3 = iArr[i2] - 1;
            iArr2[i2] = i3;
            i2++;
            if (i3 != -1) {
                while (i2 < i) {
                    iArr2[i2] = iArr[i2];
                    i2++;
                }
                return 0;
            }
        }
        return -1;
    }

    public static int decAt(int i, int[] iArr, int i2) {
        while (i2 < i) {
            int i3 = iArr[i2] - 1;
            iArr[i2] = i3;
            if (i3 != -1) {
                return 0;
            }
            i2++;
        }
        return -1;
    }

    public static int decAt(int i, int[] iArr, int i2, int i3) {
        while (i3 < i) {
            int i4 = i2 + i3;
            int i5 = iArr[i4] - 1;
            iArr[i4] = i5;
            if (i5 != -1) {
                return 0;
            }
            i3++;
        }
        return -1;
    }

    public static boolean eq(int i, int[] iArr, int[] iArr2) {
        for (i--; i >= 0; i--) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static int[] fromBigInteger(int i, BigInteger bigInteger) {
        if (bigInteger.signum() < 0 || bigInteger.bitLength() > i) {
            throw new IllegalArgumentException();
        }
        int[] create = create((i + 31) >> 5);
        int i2 = 0;
        while (bigInteger.signum() != 0) {
            int i3 = i2 + 1;
            create[i2] = bigInteger.intValue();
            bigInteger = bigInteger.shiftRight(32);
            i2 = i3;
        }
        return create;
    }

    public static int getBit(int[] iArr, int i) {
        int i2;
        if (i == 0) {
            i2 = iArr[0];
        } else {
            int i3 = i >> 5;
            if (i3 < 0 || i3 >= iArr.length) {
                return 0;
            }
            i2 = iArr[i3] >>> (i & 31);
        }
        return i2 & 1;
    }

    public static boolean gte(int i, int[] iArr, int[] iArr2) {
        for (i--; i >= 0; i--) {
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

    public static int inc(int i, int[] iArr) {
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = iArr[i2] + 1;
            iArr[i2] = i3;
            if (i3 != 0) {
                return 0;
            }
        }
        return 1;
    }

    public static int inc(int i, int[] iArr, int[] iArr2) {
        int i2 = 0;
        while (i2 < i) {
            int i3 = iArr[i2] + 1;
            iArr2[i2] = i3;
            i2++;
            if (i3 != 0) {
                while (i2 < i) {
                    iArr2[i2] = iArr[i2];
                    i2++;
                }
                return 0;
            }
        }
        return 1;
    }

    public static int incAt(int i, int[] iArr, int i2) {
        while (i2 < i) {
            int i3 = iArr[i2] + 1;
            iArr[i2] = i3;
            if (i3 != 0) {
                return 0;
            }
            i2++;
        }
        return 1;
    }

    public static int incAt(int i, int[] iArr, int i2, int i3) {
        while (i3 < i) {
            int i4 = i2 + i3;
            int i5 = iArr[i4] + 1;
            iArr[i4] = i5;
            if (i5 != 0) {
                return 0;
            }
            i3++;
        }
        return 1;
    }

    public static boolean isOne(int i, int[] iArr) {
        if (iArr[0] != 1) {
            return false;
        }
        for (int i2 = 1; i2 < i; i2++) {
            if (iArr[i2] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isZero(int i, int[] iArr) {
        for (int i2 = 0; i2 < i; i2++) {
            if (iArr[i2] != 0) {
                return false;
            }
        }
        return true;
    }

    public static void mul(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        iArr3[i4 + i] = mulWord(i, iArr[i2], iArr2, i3, iArr3, i4);
        for (int i5 = 1; i5 < i; i5++) {
            int i6 = i4 + i5;
            iArr3[i6 + i] = mulWordAddTo(i, iArr[i2 + i5], iArr2, i3, iArr3, i6);
        }
    }

    public static void mul(int i, int[] iArr, int[] iArr2, int[] iArr3) {
        iArr3[i] = mulWord(i, iArr[0], iArr2, iArr3);
        for (int i2 = 1; i2 < i; i2++) {
            iArr3[i2 + i] = mulWordAddTo(i, iArr[i2], iArr2, 0, iArr3, i2);
        }
    }

    public static int mul31BothAdd(int i, int i2, int[] iArr, int i3, int[] iArr2, int[] iArr3, int i4) {
        long j = ((long) i2) & 4294967295L;
        long j2 = ((long) i3) & 4294967295L;
        int i5 = 0;
        long j3 = 0;
        while (true) {
            int i6 = i4 + i5;
            long j4 = j;
            j3 += (((((long) iArr[i5]) & 4294967295L) * j) + ((((long) iArr2[i5]) & 4294967295L) * j2)) + (((long) iArr3[i6]) & 4294967295L);
            iArr3[i6] = (int) j3;
            j3 >>>= 32;
            i5++;
            if (i5 >= i) {
                return (int) j3;
            }
            j = j4;
        }
    }

    public static int mulAddTo(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        int i5 = i;
        int i6 = i4;
        long j = 0;
        for (int i7 = 0; i7 < i5; i7++) {
            int i8 = i6 + i5;
            long mulWordAddTo = (((long) mulWordAddTo(i5, iArr[i2 + i7], iArr2, i3, iArr3, i6)) & 4294967295L) + (j + (4294967295L & ((long) iArr3[i8])));
            iArr3[i8] = (int) mulWordAddTo;
            j = mulWordAddTo >>> 32;
            i6++;
        }
        return (int) j;
    }

    public static int mulAddTo(int i, int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = i2 + i;
            long mulWordAddTo = (((long) mulWordAddTo(i, iArr[i2], iArr2, 0, iArr3, i2)) & 4294967295L) + (j + (4294967295L & ((long) iArr3[i3])));
            iArr3[i3] = (int) mulWordAddTo;
            j = mulWordAddTo >>> 32;
        }
        return (int) j;
    }

    public static int mulWord(int i, int i2, int[] iArr, int i3, int[] iArr2, int i4) {
        long j = ((long) i2) & 4294967295L;
        long j2 = 0;
        i2 = 0;
        do {
            j2 += (((long) iArr[i3 + i2]) & 4294967295L) * j;
            iArr2[i4 + i2] = (int) j2;
            j2 >>>= 32;
            i2++;
        } while (i2 < i);
        return (int) j2;
    }

    public static int mulWord(int i, int i2, int[] iArr, int[] iArr2) {
        long j = ((long) i2) & 4294967295L;
        long j2 = 0;
        i2 = 0;
        do {
            j2 += (((long) iArr[i2]) & 4294967295L) * j;
            iArr2[i2] = (int) j2;
            j2 >>>= 32;
            i2++;
        } while (i2 < i);
        return (int) j2;
    }

    public static int mulWordAddTo(int i, int i2, int[] iArr, int i3, int[] iArr2, int i4) {
        long j = ((long) i2) & 4294967295L;
        int i5 = 0;
        long j2 = 0;
        do {
            int i6 = i4 + i5;
            j2 += ((((long) iArr[i3 + i5]) & 4294967295L) * j) + (((long) iArr2[i6]) & 4294967295L);
            iArr2[i6] = (int) j2;
            j2 >>>= 32;
            i5++;
        } while (i5 < i);
        return (int) j2;
    }

    public static int mulWordDwordAddAt(int i, int i2, long j, int[] iArr, int i3) {
        long j2 = ((long) i2) & 4294967295L;
        i2 = i3 + 0;
        long j3 = (((j & 4294967295L) * j2) + (((long) iArr[i2]) & 4294967295L)) + 0;
        iArr[i2] = (int) j3;
        j2 *= j >>> 32;
        int i4 = i3 + 1;
        j3 = (j3 >>> 32) + (j2 + (((long) iArr[i4]) & 4294967295L));
        iArr[i4] = (int) j3;
        int i5 = i3 + 2;
        j = (j3 >>> 32) + (((long) iArr[i5]) & 4294967295L);
        iArr[i5] = (int) j;
        return (j >>> 32) == 0 ? 0 : incAt(i, iArr, i3 + 3);
    }

    public static int shiftDownBit(int i, int[] iArr, int i2) {
        while (true) {
            i--;
            if (i < 0) {
                return i2 << 31;
            }
            int i3 = iArr[i];
            iArr[i] = (i2 << 31) | (i3 >>> 1);
            i2 = i3;
        }
    }

    public static int shiftDownBit(int i, int[] iArr, int i2, int i3) {
        while (true) {
            i--;
            if (i < 0) {
                return i3 << 31;
            }
            int i4 = i2 + i;
            int i5 = iArr[i4];
            iArr[i4] = (i3 << 31) | (i5 >>> 1);
            i3 = i5;
        }
    }

    public static int shiftDownBit(int i, int[] iArr, int i2, int i3, int[] iArr2, int i4) {
        while (true) {
            i--;
            if (i < 0) {
                return i3 << 31;
            }
            int i5 = iArr[i2 + i];
            iArr2[i4 + i] = (i3 << 31) | (i5 >>> 1);
            i3 = i5;
        }
    }

    public static int shiftDownBit(int i, int[] iArr, int i2, int[] iArr2) {
        while (true) {
            i--;
            if (i < 0) {
                return i2 << 31;
            }
            int i3 = iArr[i];
            iArr2[i] = (i2 << 31) | (i3 >>> 1);
            i2 = i3;
        }
    }

    public static int shiftDownBits(int i, int[] iArr, int i2, int i3) {
        while (true) {
            i--;
            if (i < 0) {
                return i3 << (-i2);
            }
            int i4 = iArr[i];
            iArr[i] = (i3 << (-i2)) | (i4 >>> i2);
            i3 = i4;
        }
    }

    public static int shiftDownBits(int i, int[] iArr, int i2, int i3, int i4) {
        while (true) {
            i--;
            if (i < 0) {
                return i4 << (-i3);
            }
            int i5 = i2 + i;
            int i6 = iArr[i5];
            iArr[i5] = (i4 << (-i3)) | (i6 >>> i3);
            i4 = i6;
        }
    }

    public static int shiftDownBits(int i, int[] iArr, int i2, int i3, int i4, int[] iArr2, int i5) {
        while (true) {
            i--;
            if (i < 0) {
                return i4 << (-i3);
            }
            int i6 = iArr[i2 + i];
            iArr2[i5 + i] = (i4 << (-i3)) | (i6 >>> i3);
            i4 = i6;
        }
    }

    public static int shiftDownBits(int i, int[] iArr, int i2, int i3, int[] iArr2) {
        while (true) {
            i--;
            if (i < 0) {
                return i3 << (-i2);
            }
            int i4 = iArr[i];
            iArr2[i] = (i3 << (-i2)) | (i4 >>> i2);
            i3 = i4;
        }
    }

    public static int shiftDownWord(int i, int[] iArr, int i2) {
        while (true) {
            i--;
            if (i < 0) {
                return i2;
            }
            int i3 = iArr[i];
            iArr[i] = i2;
            i2 = i3;
        }
    }

    public static int shiftUpBit(int i, int[] iArr, int i2) {
        int i3 = 0;
        while (i3 < i) {
            int i4 = iArr[i3];
            iArr[i3] = (i2 >>> 31) | (i4 << 1);
            i3++;
            i2 = i4;
        }
        return i2 >>> 31;
    }

    public static int shiftUpBit(int i, int[] iArr, int i2, int i3) {
        int i4 = 0;
        while (i4 < i) {
            int i5 = i2 + i4;
            int i6 = iArr[i5];
            iArr[i5] = (i3 >>> 31) | (i6 << 1);
            i4++;
            i3 = i6;
        }
        return i3 >>> 31;
    }

    public static int shiftUpBit(int i, int[] iArr, int i2, int i3, int[] iArr2, int i4) {
        int i5 = 0;
        while (i5 < i) {
            int i6 = iArr[i2 + i5];
            iArr2[i4 + i5] = (i3 >>> 31) | (i6 << 1);
            i5++;
            i3 = i6;
        }
        return i3 >>> 31;
    }

    public static int shiftUpBit(int i, int[] iArr, int i2, int[] iArr2) {
        int i3 = 0;
        while (i3 < i) {
            int i4 = iArr[i3];
            iArr2[i3] = (i2 >>> 31) | (i4 << 1);
            i3++;
            i2 = i4;
        }
        return i2 >>> 31;
    }

    public static long shiftUpBit64(int i, long[] jArr, int i2, long j, long[] jArr2, int i3) {
        int i4 = 0;
        while (i4 < i) {
            long j2 = jArr[i2 + i4];
            jArr2[i3 + i4] = (j >>> 63) | (j2 << 1);
            i4++;
            j = j2;
        }
        return j >>> 63;
    }

    public static int shiftUpBits(int i, int[] iArr, int i2, int i3) {
        int i4 = 0;
        while (i4 < i) {
            int i5 = iArr[i4];
            iArr[i4] = (i3 >>> (-i2)) | (i5 << i2);
            i4++;
            i3 = i5;
        }
        return i3 >>> (-i2);
    }

    public static int shiftUpBits(int i, int[] iArr, int i2, int i3, int i4) {
        int i5 = 0;
        while (i5 < i) {
            int i6 = i2 + i5;
            int i7 = iArr[i6];
            iArr[i6] = (i4 >>> (-i3)) | (i7 << i3);
            i5++;
            i4 = i7;
        }
        return i4 >>> (-i3);
    }

    public static int shiftUpBits(int i, int[] iArr, int i2, int i3, int i4, int[] iArr2, int i5) {
        int i6 = 0;
        while (i6 < i) {
            int i7 = iArr[i2 + i6];
            iArr2[i5 + i6] = (i4 >>> (-i3)) | (i7 << i3);
            i6++;
            i4 = i7;
        }
        return i4 >>> (-i3);
    }

    public static int shiftUpBits(int i, int[] iArr, int i2, int i3, int[] iArr2) {
        int i4 = 0;
        while (i4 < i) {
            int i5 = iArr[i4];
            iArr2[i4] = (i3 >>> (-i2)) | (i5 << i2);
            i4++;
            i3 = i5;
        }
        return i3 >>> (-i2);
    }

    public static long shiftUpBits64(int i, long[] jArr, int i2, int i3, long j) {
        int i4 = 0;
        while (i4 < i) {
            int i5 = i2 + i4;
            long j2 = jArr[i5];
            jArr[i5] = (j >>> (-i3)) | (j2 << i3);
            i4++;
            j = j2;
        }
        return j >>> (-i3);
    }

    public static long shiftUpBits64(int i, long[] jArr, int i2, int i3, long j, long[] jArr2, int i4) {
        int i5 = 0;
        while (i5 < i) {
            long j2 = jArr[i2 + i5];
            jArr2[i4 + i5] = (j >>> (-i3)) | (j2 << i3);
            i5++;
            j = j2;
        }
        return j >>> (-i3);
    }

    public static void square(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        int i4;
        int i5 = i << 1;
        int i6 = i5;
        int i7 = 0;
        int i8 = i;
        do {
            i8--;
            long j = ((long) iArr[i2 + i8]) & 4294967295L;
            j *= j;
            i6--;
            iArr2[i3 + i6] = (i7 << 31) | ((int) (j >>> 33));
            i6--;
            i4 = 1;
            iArr2[i3 + i6] = (int) (j >>> 1);
            i7 = (int) j;
        } while (i8 > 0);
        while (i4 < i) {
            addWordAt(i5, squareWordAdd(iArr, i2, i4, iArr2, i3), iArr2, i3, i4 << 1);
            i4++;
        }
        shiftUpBit(i5, iArr2, i3, iArr[i2] << 31);
    }

    public static void square(int i, int[] iArr, int[] iArr2) {
        int i2 = i << 1;
        int i3 = i;
        int i4 = i2;
        int i5 = 0;
        while (true) {
            i3--;
            long j = ((long) iArr[i3]) & 4294967295L;
            j *= j;
            i4--;
            iArr2[i4] = (i5 << 31) | ((int) (j >>> 33));
            i4--;
            i5 = 1;
            iArr2[i4] = (int) (j >>> 1);
            int i6 = (int) j;
            if (i3 <= 0) {
                break;
            }
            i5 = i6;
        }
        while (i5 < i) {
            addWordAt(i2, squareWordAdd(iArr, i5, iArr2), iArr2, i5 << 1);
            i5++;
        }
        shiftUpBit(i2, iArr2, iArr[0] << 31);
    }

    public static int squareWordAdd(int[] iArr, int i, int i2, int[] iArr2, int i3) {
        int i4 = i2;
        long j = ((long) iArr[i + i4]) & 4294967295L;
        long j2 = 0;
        int i5 = 0;
        int i6 = i3;
        do {
            int i7 = i4 + i6;
            j2 += ((((long) iArr[i + i5]) & 4294967295L) * j) + (((long) iArr2[i7]) & 4294967295L);
            iArr2[i7] = (int) j2;
            j2 >>>= 32;
            i6++;
            i5++;
        } while (i5 < i4);
        return (int) j2;
    }

    public static int squareWordAdd(int[] iArr, int i, int[] iArr2) {
        long j = ((long) iArr[i]) & 4294967295L;
        long j2 = 0;
        int i2 = 0;
        do {
            int i3 = i + i2;
            j2 += ((((long) iArr[i2]) & 4294967295L) * j) + (((long) iArr2[i3]) & 4294967295L);
            iArr2[i3] = (int) j2;
            j2 >>>= 32;
            i2++;
        } while (i2 < i);
        return (int) j2;
    }

    public static int sub(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        long j = 0;
        for (int i5 = 0; i5 < i; i5++) {
            j += (((long) iArr[i2 + i5]) & 4294967295L) - (4294967295L & ((long) iArr2[i3 + i5]));
            iArr3[i4 + i5] = (int) j;
            j >>= 32;
        }
        return (int) j;
    }

    public static int sub(int i, int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j += (((long) iArr[i2]) & 4294967295L) - (4294967295L & ((long) iArr2[i2]));
            iArr3[i2] = (int) j;
            j >>= 32;
        }
        return (int) j;
    }

    public static int sub33At(int i, int i2, int[] iArr, int i3) {
        int i4 = i3 + 0;
        long j = (((long) iArr[i4]) & 4294967295L) - (((long) i2) & 4294967295L);
        iArr[i4] = (int) j;
        int i5 = i3 + 1;
        long j2 = (j >> 32) + ((4294967295L & ((long) iArr[i5])) - 1);
        iArr[i5] = (int) j2;
        return (j2 >> 32) == 0 ? 0 : decAt(i, iArr, i3 + 2);
    }

    public static int sub33At(int i, int i2, int[] iArr, int i3, int i4) {
        int i5 = i3 + i4;
        long j = (((long) iArr[i5]) & 4294967295L) - (((long) i2) & 4294967295L);
        iArr[i5] = (int) j;
        i5++;
        j = (j >> 32) + ((4294967295L & ((long) iArr[i5])) - 1);
        iArr[i5] = (int) j;
        return (j >> 32) == 0 ? 0 : decAt(i, iArr, i3, i4 + 2);
    }

    public static int sub33From(int i, int i2, int[] iArr) {
        long j = (((long) iArr[0]) & 4294967295L) - (((long) i2) & 4294967295L);
        iArr[0] = (int) j;
        j = (j >> 32) + ((4294967295L & ((long) iArr[1])) - 1);
        iArr[1] = (int) j;
        return (j >> 32) == 0 ? 0 : decAt(i, iArr, 2);
    }

    public static int sub33From(int i, int i2, int[] iArr, int i3) {
        int i4 = i3 + 0;
        long j = (((long) iArr[i4]) & 4294967295L) - (((long) i2) & 4294967295L);
        iArr[i4] = (int) j;
        int i5 = i3 + 1;
        long j2 = (j >> 32) + ((4294967295L & ((long) iArr[i5])) - 1);
        iArr[i5] = (int) j2;
        return (j2 >> 32) == 0 ? 0 : decAt(i, iArr, i3, 2);
    }

    public static int subBothFrom(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        int i5 = 0;
        long j = 0;
        int i6 = i;
        while (i5 < i6) {
            int i7 = i4 + i5;
            j += ((((long) iArr3[i7]) & 4294967295L) - (((long) iArr[i2 + i5]) & 4294967295L)) - (((long) iArr2[i3 + i5]) & 4294967295L);
            iArr3[i7] = (int) j;
            j >>= 32;
            i5++;
            i6 = i;
        }
        return (int) j;
    }

    public static int subBothFrom(int i, int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j += ((((long) iArr3[i2]) & 4294967295L) - (((long) iArr[i2]) & 4294967295L)) - (4294967295L & ((long) iArr2[i2]));
            iArr3[i2] = (int) j;
            j >>= 32;
        }
        return (int) j;
    }

    public static int subDWordAt(int i, long j, int[] iArr, int i2) {
        int i3 = i2 + 0;
        long j2 = (((long) iArr[i3]) & 4294967295L) - (j & 4294967295L);
        iArr[i3] = (int) j2;
        int i4 = i2 + 1;
        j2 = (j2 >> 32) + ((4294967295L & ((long) iArr[i4])) - (j >>> 32));
        iArr[i4] = (int) j2;
        return (j2 >> 32) == 0 ? 0 : decAt(i, iArr, i2 + 2);
    }

    public static int subDWordAt(int i, long j, int[] iArr, int i2, int i3) {
        int i4 = i2 + i3;
        long j2 = (((long) iArr[i4]) & 4294967295L) - (j & 4294967295L);
        iArr[i4] = (int) j2;
        i4++;
        j2 = (j2 >> 32) + ((4294967295L & ((long) iArr[i4])) - (j >>> 32));
        iArr[i4] = (int) j2;
        return (j2 >> 32) == 0 ? 0 : decAt(i, iArr, i2, i3 + 2);
    }

    public static int subDWordFrom(int i, long j, int[] iArr) {
        long j2 = (((long) iArr[0]) & 4294967295L) - (j & 4294967295L);
        iArr[0] = (int) j2;
        j2 = (j2 >> 32) + ((4294967295L & ((long) iArr[1])) - (j >>> 32));
        iArr[1] = (int) j2;
        return (j2 >> 32) == 0 ? 0 : decAt(i, iArr, 2);
    }

    public static int subDWordFrom(int i, long j, int[] iArr, int i2) {
        int i3 = i2 + 0;
        long j2 = (((long) iArr[i3]) & 4294967295L) - (j & 4294967295L);
        iArr[i3] = (int) j2;
        int i4 = i2 + 1;
        j2 = (j2 >> 32) + ((4294967295L & ((long) iArr[i4])) - (j >>> 32));
        iArr[i4] = (int) j2;
        return (j2 >> 32) == 0 ? 0 : decAt(i, iArr, i2, 2);
    }

    public static int subFrom(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        long j = 0;
        for (int i4 = 0; i4 < i; i4++) {
            int i5 = i3 + i4;
            j += (((long) iArr2[i5]) & 4294967295L) - (4294967295L & ((long) iArr[i2 + i4]));
            iArr2[i5] = (int) j;
            j >>= 32;
        }
        return (int) j;
    }

    public static int subFrom(int i, int[] iArr, int[] iArr2) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j += (((long) iArr2[i2]) & 4294967295L) - (4294967295L & ((long) iArr[i2]));
            iArr2[i2] = (int) j;
            j >>= 32;
        }
        return (int) j;
    }

    public static int subWordAt(int i, int i2, int[] iArr, int i3) {
        long j = (((long) iArr[i3]) & 4294967295L) - (4294967295L & ((long) i2));
        iArr[i3] = (int) j;
        return (j >> 32) == 0 ? 0 : decAt(i, iArr, i3 + 1);
    }

    public static int subWordAt(int i, int i2, int[] iArr, int i3, int i4) {
        int i5 = i3 + i4;
        long j = (((long) iArr[i5]) & 4294967295L) - (4294967295L & ((long) i2));
        iArr[i5] = (int) j;
        return (j >> 32) == 0 ? 0 : decAt(i, iArr, i3, i4 + 1);
    }

    public static int subWordFrom(int i, int i2, int[] iArr) {
        long j = (((long) iArr[0]) & 4294967295L) - (4294967295L & ((long) i2));
        iArr[0] = (int) j;
        return (j >> 32) == 0 ? 0 : decAt(i, iArr, 1);
    }

    public static int subWordFrom(int i, int i2, int[] iArr, int i3) {
        int i4 = i3 + 0;
        long j = (((long) iArr[i4]) & 4294967295L) - (4294967295L & ((long) i2));
        iArr[i4] = (int) j;
        return (j >> 32) == 0 ? 0 : decAt(i, iArr, i3, 1);
    }

    public static BigInteger toBigInteger(int i, int[] iArr) {
        byte[] bArr = new byte[(i << 2)];
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = iArr[i2];
            if (i3 != 0) {
                Pack.intToBigEndian(i3, bArr, ((i - 1) - i2) << 2);
            }
        }
        return new BigInteger(1, bArr);
    }

    public static void zero(int i, int[] iArr) {
        for (int i2 = 0; i2 < i; i2++) {
            iArr[i2] = 0;
        }
    }

    public static void zero64(int i, long[] jArr) {
        for (int i2 = 0; i2 < i; i2++) {
            jArr[i2] = 0;
        }
    }
}
