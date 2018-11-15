package org.bouncycastle.pqc.math.linearalgebra;

public final class LittleEndianConversions {
    private LittleEndianConversions() {
    }

    public static void I2OSP(int i, byte[] bArr, int i2) {
        int i3 = i2 + 1;
        bArr[i2] = (byte) i;
        i2 = i3 + 1;
        bArr[i3] = (byte) (i >>> 8);
        i3 = i2 + 1;
        bArr[i2] = (byte) (i >>> 16);
        bArr[i3] = (byte) (i >>> 24);
    }

    public static void I2OSP(int i, byte[] bArr, int i2, int i3) {
        for (i3--; i3 >= 0; i3--) {
            bArr[i2 + i3] = (byte) (i >>> (8 * i3));
        }
    }

    public static void I2OSP(long j, byte[] bArr, int i) {
        int i2 = i + 1;
        bArr[i] = (byte) ((int) j);
        i = i2 + 1;
        bArr[i2] = (byte) ((int) (j >>> 8));
        i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 16));
        i = i2 + 1;
        bArr[i2] = (byte) ((int) (j >>> 24));
        i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 32));
        i = i2 + 1;
        bArr[i2] = (byte) ((int) (j >>> 40));
        i2 = i + 1;
        bArr[i] = (byte) ((int) (j >>> 48));
        bArr[i2] = (byte) ((int) (j >>> 56));
    }

    public static byte[] I2OSP(int i) {
        return new byte[]{(byte) i, (byte) (i >>> 8), (byte) (i >>> 16), (byte) (i >>> 24)};
    }

    public static byte[] I2OSP(long j) {
        return new byte[]{(byte) ((int) j), (byte) ((int) (j >>> 8)), (byte) ((int) (j >>> 16)), (byte) ((int) (j >>> 24)), (byte) ((int) (j >>> 32)), (byte) ((int) (j >>> 40)), (byte) ((int) (j >>> 48)), (byte) ((int) (j >>> 56))};
    }

    public static int OS2IP(byte[] bArr) {
        return ((bArr[3] & 255) << 24) | (((bArr[0] & 255) | ((bArr[1] & 255) << 8)) | ((bArr[2] & 255) << 16));
    }

    public static int OS2IP(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = i2 + 1;
        i = (bArr[i] & 255) | ((bArr[i2] & 255) << 8);
        return ((bArr[i3 + 1] & 255) << 24) | (i | ((bArr[i3] & 255) << 16));
    }

    public static int OS2IP(byte[] bArr, int i, int i2) {
        int i3 = 0;
        for (i2--; i2 >= 0; i2--) {
            i3 |= (bArr[i + i2] & 255) << (8 * i2);
        }
        return i3;
    }

    public static long OS2LIP(byte[] bArr, int i) {
        int i2 = i + 1;
        long j = (long) (bArr[i] & 255);
        i = i2 + 1;
        int i3 = i + 1;
        i = i3 + 1;
        long j2 = ((j | ((long) ((bArr[i2] & 255) << 8))) | ((long) ((bArr[i] & 255) << 16))) | ((((long) bArr[i3]) & 255) << 24);
        i3 = i + 1;
        i = i3 + 1;
        j2 = (j2 | ((((long) bArr[i]) & 255) << 32)) | ((((long) bArr[i3]) & 255) << 40);
        return ((((long) bArr[i + 1]) & 255) << 56) | (j2 | ((((long) bArr[i]) & 255) << 48));
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
