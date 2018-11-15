package org.bouncycastle.pqc.math.linearalgebra;

public final class BigEndianConversions {
    private BigEndianConversions() {
    }

    public static void I2OSP(int i, byte[] bArr, int i2) {
        int i3 = i2 + 1;
        bArr[i2] = (byte) (i >>> 24);
        i2 = i3 + 1;
        bArr[i3] = (byte) (i >>> 16);
        i3 = i2 + 1;
        bArr[i2] = (byte) (i >>> 8);
        bArr[i3] = (byte) i;
    }

    public static void I2OSP(int i, byte[] bArr, int i2, int i3) {
        i3--;
        for (int i4 = i3; i4 >= 0; i4--) {
            bArr[i2 + i4] = (byte) (i >>> (8 * (i3 - i4)));
        }
    }

    public static void I2OSP(long j, byte[] bArr, int i) {
        int i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 56));
        i = i2 + 1;
        bArr[i2] = (byte) ((int) (j >>> 48));
        i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 40));
        i = i2 + 1;
        bArr[i2] = (byte) ((int) (j >>> 32));
        i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 24));
        i = i2 + 1;
        bArr[i2] = (byte) ((int) (j >>> 16));
        i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 8));
        bArr[i2] = (byte) ((int) j);
    }

    public static byte[] I2OSP(int i) {
        return new byte[]{(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i};
    }

    public static byte[] I2OSP(int i, int i2) throws ArithmeticException {
        if (i < 0) {
            return null;
        }
        int ceilLog256 = IntegerFunctions.ceilLog256(i);
        if (ceilLog256 <= i2) {
            byte[] bArr = new byte[i2];
            int i3 = i2 - 1;
            for (int i4 = i3; i4 >= i2 - ceilLog256; i4--) {
                bArr[i4] = (byte) (i >>> (8 * (i3 - i4)));
            }
            return bArr;
        }
        throw new ArithmeticException("Cannot encode given integer into specified number of octets.");
    }

    public static byte[] I2OSP(long j) {
        return new byte[]{(byte) ((int) (j >>> 56)), (byte) ((int) (j >>> 48)), (byte) ((int) (j >>> 40)), (byte) ((int) (j >>> 32)), (byte) ((int) (j >>> 24)), (byte) ((int) (j >>> 16)), (byte) ((int) (j >>> 8)), (byte) ((int) j)};
    }

    public static int OS2IP(byte[] bArr) {
        if (bArr.length <= 4) {
            int i = 0;
            if (bArr.length == 0) {
                return 0;
            }
            int i2 = 0;
            while (i < bArr.length) {
                i2 |= (bArr[i] & 255) << (8 * ((bArr.length - 1) - i));
                i++;
            }
            return i2;
        }
        throw new ArithmeticException("invalid input length");
    }

    public static int OS2IP(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = i2 + 1;
        i = ((bArr[i] & 255) << 24) | ((bArr[i2] & 255) << 16);
        return (bArr[i3 + 1] & 255) | (i | ((bArr[i3] & 255) << 8));
    }

    public static int OS2IP(byte[] bArr, int i, int i2) {
        int i3 = 0;
        if (bArr.length == 0 || bArr.length < (i + i2) - 1) {
            return 0;
        }
        int i4 = 0;
        while (i3 < i2) {
            i4 |= (bArr[i + i3] & 255) << (8 * ((i2 - i3) - 1));
            i3++;
        }
        return i4;
    }

    public static long OS2LIP(byte[] bArr, int i) {
        int i2 = i + 1;
        i = i2 + 1;
        int i3 = i + 1;
        i = i3 + 1;
        i3 = i + 1;
        i = i3 + 1;
        long j = ((((((((long) bArr[i]) & 255) << 56) | ((((long) bArr[i2]) & 255) << 48)) | ((((long) bArr[i]) & 255) << 40)) | ((((long) bArr[i3]) & 255) << 32)) | ((255 & ((long) bArr[i])) << 24)) | ((long) ((bArr[i3] & 255) << 16));
        return ((long) (bArr[i + 1] & 255)) | (j | ((long) ((bArr[i] & 255) << 8)));
    }

    public static byte[] toByteArray(int[] iArr) {
        byte[] bArr = new byte[(iArr.length << 2)];
        for (int i = 0; i < iArr.length; i++) {
            I2OSP(iArr[i], bArr, i << 2);
        }
        return bArr;
    }

    public static byte[] toByteArray(int[] iArr, int i) {
        int length = iArr.length;
        byte[] bArr = new byte[i];
        int i2 = 0;
        int i3 = 0;
        while (i2 <= length - 2) {
            I2OSP(iArr[i2], bArr, i3);
            i2++;
            i3 += 4;
        }
        I2OSP(iArr[length - 1], bArr, i3, i - i3);
        return bArr;
    }

    public static int[] toIntArray(byte[] bArr) {
        int length = (bArr.length + 3) / 4;
        int length2 = bArr.length & 3;
        int[] iArr = new int[length];
        int i = 0;
        int i2 = 0;
        while (i <= length - 2) {
            iArr[i] = OS2IP(bArr, i2);
            i++;
            i2 += 4;
        }
        if (length2 != 0) {
            iArr[length - 1] = OS2IP(bArr, i2, length2);
            return iArr;
        }
        iArr[length - 1] = OS2IP(bArr, i2);
        return iArr;
    }
}
