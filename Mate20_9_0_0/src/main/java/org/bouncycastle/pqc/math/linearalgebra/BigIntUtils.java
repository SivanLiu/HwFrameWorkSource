package org.bouncycastle.pqc.math.linearalgebra;

import java.math.BigInteger;

public final class BigIntUtils {
    private BigIntUtils() {
    }

    public static boolean equals(BigInteger[] bigIntegerArr, BigInteger[] bigIntegerArr2) {
        boolean z = false;
        if (bigIntegerArr.length != bigIntegerArr2.length) {
            return false;
        }
        int i = 0;
        int i2 = i;
        while (i < bigIntegerArr.length) {
            i2 |= bigIntegerArr[i].compareTo(bigIntegerArr2[i]);
            i++;
        }
        if (i2 == 0) {
            z = true;
        }
        return z;
    }

    public static void fill(BigInteger[] bigIntegerArr, BigInteger bigInteger) {
        for (int length = bigIntegerArr.length - 1; length >= 0; length--) {
            bigIntegerArr[length] = bigInteger;
        }
    }

    public static BigInteger[] subArray(BigInteger[] bigIntegerArr, int i, int i2) {
        i2 -= i;
        BigInteger[] bigIntegerArr2 = new BigInteger[i2];
        System.arraycopy(bigIntegerArr, i, bigIntegerArr2, 0, i2);
        return bigIntegerArr2;
    }

    public static int[] toIntArray(BigInteger[] bigIntegerArr) {
        int[] iArr = new int[bigIntegerArr.length];
        for (int i = 0; i < bigIntegerArr.length; i++) {
            iArr[i] = bigIntegerArr[i].intValue();
        }
        return iArr;
    }

    public static int[] toIntArrayModQ(int i, BigInteger[] bigIntegerArr) {
        BigInteger valueOf = BigInteger.valueOf((long) i);
        int[] iArr = new int[bigIntegerArr.length];
        for (int i2 = 0; i2 < bigIntegerArr.length; i2++) {
            iArr[i2] = bigIntegerArr[i2].mod(valueOf).intValue();
        }
        return iArr;
    }

    public static byte[] toMinimalByteArray(BigInteger bigInteger) {
        byte[] toByteArray = bigInteger.toByteArray();
        if (toByteArray.length == 1 || (bigInteger.bitLength() & 7) != 0) {
            return toByteArray;
        }
        byte[] bArr = new byte[(bigInteger.bitLength() >> 3)];
        System.arraycopy(toByteArray, 1, bArr, 0, bArr.length);
        return bArr;
    }
}
