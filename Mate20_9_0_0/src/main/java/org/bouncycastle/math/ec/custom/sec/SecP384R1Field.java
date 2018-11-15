package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat384;

public class SecP384R1Field {
    private static final long M = 4294967295L;
    static final int[] P = new int[]{-1, 0, 0, -1, -2, -1, -1, -1, -1, -1, -1, -1};
    private static final int P11 = -1;
    static final int[] PExt = new int[]{1, -2, 0, 2, 0, -2, 0, 2, 1, 0, 0, 0, -2, 1, 0, -2, -3, -1, -1, -1, -1, -1, -1, -1};
    private static final int PExt23 = -1;
    private static final int[] PExtInv = new int[]{-1, 1, -1, -3, -1, 1, -1, -3, -2, -1, -1, -1, 1, -2, -1, 1, 2};

    public static void add(int[] iArr, int[] iArr2, int[] iArr3) {
        if (Nat.add(12, iArr, iArr2, iArr3) != 0 || (iArr3[11] == -1 && Nat.gte(12, iArr3, P))) {
            addPInvTo(iArr3);
        }
    }

    public static void addExt(int[] iArr, int[] iArr2, int[] iArr3) {
        if ((Nat.add(24, iArr, iArr2, iArr3) != 0 || (iArr3[23] == -1 && Nat.gte(24, iArr3, PExt))) && Nat.addTo(PExtInv.length, PExtInv, iArr3) != 0) {
            Nat.incAt(24, iArr3, PExtInv.length);
        }
    }

    public static void addOne(int[] iArr, int[] iArr2) {
        if (Nat.inc(12, iArr, iArr2) != 0 || (iArr2[11] == -1 && Nat.gte(12, iArr2, P))) {
            addPInvTo(iArr2);
        }
    }

    private static void addPInvTo(int[] iArr) {
        long j = (((long) iArr[0]) & 4294967295L) + 1;
        iArr[0] = (int) j;
        j = (j >> 32) + ((((long) iArr[1]) & 4294967295L) - 1);
        iArr[1] = (int) j;
        j >>= 32;
        if (j != 0) {
            j += ((long) iArr[2]) & 4294967295L;
            iArr[2] = (int) j;
            j >>= 32;
        }
        j += (((long) iArr[3]) & 4294967295L) + 1;
        iArr[3] = (int) j;
        j = (j >> 32) + ((4294967295L & ((long) iArr[4])) + 1);
        iArr[4] = (int) j;
        if ((j >> 32) != 0) {
            Nat.incAt(12, iArr, 5);
        }
    }

    public static int[] fromBigInteger(BigInteger bigInteger) {
        int[] fromBigInteger = Nat.fromBigInteger(384, bigInteger);
        if (fromBigInteger[11] == -1 && Nat.gte(12, fromBigInteger, P)) {
            Nat.subFrom(12, P, fromBigInteger);
        }
        return fromBigInteger;
    }

    public static void half(int[] iArr, int[] iArr2) {
        if ((iArr[0] & 1) == 0) {
            Nat.shiftDownBit(12, iArr, 0, iArr2);
        } else {
            Nat.shiftDownBit(12, iArr2, Nat.add(12, iArr, P, iArr2));
        }
    }

    public static void multiply(int[] iArr, int[] iArr2, int[] iArr3) {
        int[] create = Nat.create(24);
        Nat384.mul(iArr, iArr2, create);
        reduce(create, iArr3);
    }

    public static void negate(int[] iArr, int[] iArr2) {
        if (Nat.isZero(12, iArr)) {
            Nat.zero(12, iArr2);
        } else {
            Nat.sub(12, P, iArr, iArr2);
        }
    }

    public static void reduce(int[] iArr, int[] iArr2) {
        int[] iArr3 = iArr2;
        long j = ((long) iArr[17]) & 4294967295L;
        long j2 = ((long) iArr[20]) & 4294967295L;
        long j3 = ((long) iArr[21]) & 4294967295L;
        long j4 = ((long) iArr[19]) & 4294967295L;
        long j5 = ((long) iArr[22]) & 4294967295L;
        long j6 = ((long) iArr[18]) & 4294967295L;
        long j7 = ((long) iArr[23]) & 4294967295L;
        long j8 = ((long) iArr[16]) & 4294967295L;
        long j9 = ((((long) iArr[12]) & 4294967295L) + j2) - 1;
        long j10 = j2;
        long j11 = (((long) iArr[13]) & 4294967295L) + j5;
        long j12 = ((((long) iArr[14]) & 4294967295L) + j5) + j7;
        j2 = (((long) iArr[15]) & 4294967295L) + j7;
        long j13 = j + j3;
        long j14 = j3 - j7;
        long j15 = j9 + j14;
        long j16 = j5 - j7;
        j5 = 0 + ((((long) iArr[0]) & 4294967295L) + j15);
        iArr3[0] = (int) j5;
        long j17 = j;
        j5 = (j5 >> 32) + ((((((long) iArr[1]) & 4294967295L) + j7) - j9) + j11);
        iArr3[1] = (int) j5;
        long j18 = (j5 >> 32) + ((((((long) iArr[2]) & 4294967295L) - j3) - j11) + j12);
        iArr3[2] = (int) j18;
        j18 = (j18 >> 32) + ((((((long) iArr[3]) & 4294967295L) - j12) + j2) + j15);
        iArr3[3] = (int) j18;
        j18 = (j18 >> 32) + ((((((((long) iArr[4]) & 4294967295L) + j8) + j3) + j11) - j2) + j15);
        iArr3[4] = (int) j18;
        j18 = (j18 >> 32) + (((((((long) iArr[5]) & 4294967295L) - j8) + j11) + j12) + j13);
        iArr3[5] = (int) j18;
        j18 = (j18 >> 32) + (((((((long) iArr[6]) & 4294967295L) + j6) - j17) + j12) + j2);
        iArr3[6] = (int) j18;
        j18 = (j18 >> 32) + (((((((long) iArr[7]) & 4294967295L) + j8) + j4) - j6) + j2);
        iArr3[7] = (int) j18;
        j18 = (j18 >> 32) + (((((((long) iArr[8]) & 4294967295L) + j8) + j17) + j10) - j4);
        iArr3[8] = (int) j18;
        j18 = (j18 >> 32) + ((((((long) iArr[9]) & 4294967295L) + j6) - j10) + j13);
        iArr3[9] = (int) j18;
        j18 = (j18 >> 32) + (((((((long) iArr[10]) & 4294967295L) + j6) + j4) - j14) + j16);
        iArr3[10] = (int) j18;
        j18 = (j18 >> 32) + ((((((long) iArr[11]) & 4294967295L) + j4) + j10) - j16);
        iArr3[11] = (int) j18;
        reduce32((int) ((j18 >> 32) + 1), iArr3);
    }

    public static void reduce32(int i, int[] iArr) {
        long j;
        if (i != 0) {
            j = ((long) i) & 4294967295L;
            long j2 = ((((long) iArr[0]) & 4294967295L) + j) + 0;
            iArr[0] = (int) j2;
            j2 = (j2 >> 32) + ((((long) iArr[1]) & 4294967295L) - j);
            iArr[1] = (int) j2;
            j2 >>= 32;
            if (j2 != 0) {
                j2 += ((long) iArr[2]) & 4294967295L;
                iArr[2] = (int) j2;
                j2 >>= 32;
            }
            j2 += (((long) iArr[3]) & 4294967295L) + j;
            iArr[3] = (int) j2;
            j2 = (j2 >> 32) + ((4294967295L & ((long) iArr[4])) + j);
            iArr[4] = (int) j2;
            j = j2 >> 32;
        } else {
            j = 0;
        }
        if ((j != 0 && Nat.incAt(12, iArr, 5) != 0) || (iArr[11] == -1 && Nat.gte(12, iArr, P))) {
            addPInvTo(iArr);
        }
    }

    public static void square(int[] iArr, int[] iArr2) {
        int[] create = Nat.create(24);
        Nat384.square(iArr, create);
        reduce(create, iArr2);
    }

    public static void squareN(int[] iArr, int i, int[] iArr2) {
        int[] create = Nat.create(24);
        Nat384.square(iArr, create);
        while (true) {
            reduce(create, iArr2);
            i--;
            if (i > 0) {
                Nat384.square(iArr2, create);
            } else {
                return;
            }
        }
    }

    private static void subPInvFrom(int[] iArr) {
        long j = (((long) iArr[0]) & 4294967295L) - 1;
        iArr[0] = (int) j;
        j = (j >> 32) + ((((long) iArr[1]) & 4294967295L) + 1);
        iArr[1] = (int) j;
        j >>= 32;
        if (j != 0) {
            j += ((long) iArr[2]) & 4294967295L;
            iArr[2] = (int) j;
            j >>= 32;
        }
        j += (((long) iArr[3]) & 4294967295L) - 1;
        iArr[3] = (int) j;
        j = (j >> 32) + ((4294967295L & ((long) iArr[4])) - 1);
        iArr[4] = (int) j;
        if ((j >> 32) != 0) {
            Nat.decAt(12, iArr, 5);
        }
    }

    public static void subtract(int[] iArr, int[] iArr2, int[] iArr3) {
        if (Nat.sub(12, iArr, iArr2, iArr3) != 0) {
            subPInvFrom(iArr3);
        }
    }

    public static void subtractExt(int[] iArr, int[] iArr2, int[] iArr3) {
        if (Nat.sub(24, iArr, iArr2, iArr3) != 0 && Nat.subFrom(PExtInv.length, PExtInv, iArr3) != 0) {
            Nat.decAt(24, iArr3, PExtInv.length);
        }
    }

    public static void twice(int[] iArr, int[] iArr2) {
        if (Nat.shiftUpBit(12, iArr, 0, iArr2) != 0 || (iArr2[11] == -1 && Nat.gte(12, iArr2, P))) {
            addPInvTo(iArr2);
        }
    }
}
