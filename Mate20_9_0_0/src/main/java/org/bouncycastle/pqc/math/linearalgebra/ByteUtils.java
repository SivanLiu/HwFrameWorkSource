package org.bouncycastle.pqc.math.linearalgebra;

public final class ByteUtils {
    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private ByteUtils() {
    }

    public static byte[] clone(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        byte[] bArr2 = new byte[bArr.length];
        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
        return bArr2;
    }

    public static byte[] concatenate(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[(bArr.length + bArr2.length)];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, bArr.length, bArr2.length);
        return bArr3;
    }

    public static byte[] concatenate(byte[][] bArr) {
        int length = bArr[0].length;
        byte[] bArr2 = new byte[(bArr.length * length)];
        int i = 0;
        int i2 = i;
        while (i < bArr.length) {
            System.arraycopy(bArr[i], 0, bArr2, i2, length);
            i2 += length;
            i++;
        }
        return bArr2;
    }

    public static int deepHashCode(byte[] bArr) {
        int i = 1;
        for (byte b : bArr) {
            i = b + (31 * i);
        }
        return i;
    }

    public static int deepHashCode(byte[][] bArr) {
        int i = 1;
        for (byte[] deepHashCode : bArr) {
            i = deepHashCode(deepHashCode) + (31 * i);
        }
        return i;
    }

    public static int deepHashCode(byte[][][] bArr) {
        int i = 1;
        for (byte[][] deepHashCode : bArr) {
            i = deepHashCode(deepHashCode) + (31 * i);
        }
        return i;
    }

    public static boolean equals(byte[] bArr, byte[] bArr2) {
        boolean z = false;
        if (bArr == null) {
            if (bArr2 == null) {
                z = true;
            }
            return z;
        } else if (bArr2 == null || bArr.length != bArr2.length) {
            return false;
        } else {
            int i = 1;
            for (int length = bArr.length - 1; length >= 0; length--) {
                i &= bArr[length] == bArr2[length] ? 1 : 0;
            }
            return i;
        }
    }

    public static boolean equals(byte[][] bArr, byte[][] bArr2) {
        if (bArr.length != bArr2.length) {
            return false;
        }
        int i = 1;
        for (int length = bArr.length - 1; length >= 0; length--) {
            i &= equals(bArr[length], bArr2[length]);
        }
        return i;
    }

    public static boolean equals(byte[][][] bArr, byte[][][] bArr2) {
        if (bArr.length != bArr2.length) {
            return false;
        }
        boolean z = true;
        for (int length = bArr.length - 1; length >= 0; length--) {
            if (bArr[length].length != bArr2[length].length) {
                return false;
            }
            for (int length2 = bArr[length].length - 1; length2 >= 0; length2--) {
                z &= equals(bArr[length][length2], bArr2[length][length2]);
            }
        }
        return z;
    }

    public static byte[] fromHexString(String str) {
        char[] toCharArray = str.toUpperCase().toCharArray();
        int i = 0;
        int i2 = 0;
        int i3 = i2;
        while (i2 < toCharArray.length) {
            if ((toCharArray[i2] >= '0' && toCharArray[i2] <= '9') || (toCharArray[i2] >= 'A' && toCharArray[i2] <= 'F')) {
                i3++;
            }
            i2++;
        }
        byte[] bArr = new byte[((i3 + 1) >> 1)];
        i3 &= 1;
        while (i < toCharArray.length) {
            int i4;
            if (toCharArray[i] < '0' || toCharArray[i] > '9') {
                if (toCharArray[i] >= 'A' && toCharArray[i] <= 'F') {
                    i4 = i3 >> 1;
                    bArr[i4] = (byte) (bArr[i4] << 4);
                    bArr[i4] = (byte) (bArr[i4] | ((toCharArray[i] - 65) + 10));
                }
                i++;
            } else {
                i4 = i3 >> 1;
                bArr[i4] = (byte) (bArr[i4] << 4);
                bArr[i4] = (byte) (bArr[i4] | (toCharArray[i] - 48));
            }
            i3++;
            i++;
        }
        return bArr;
    }

    public static byte[][] split(byte[] bArr, int i) throws ArrayIndexOutOfBoundsException {
        if (i <= bArr.length) {
            byte[][] bArr2 = new byte[][]{new byte[i], new byte[(bArr.length - i)]};
            System.arraycopy(bArr, 0, bArr2[0], 0, i);
            System.arraycopy(bArr, i, bArr2[1], 0, bArr.length - i);
            return bArr2;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public static byte[] subArray(byte[] bArr, int i) {
        return subArray(bArr, i, bArr.length);
    }

    public static byte[] subArray(byte[] bArr, int i, int i2) {
        i2 -= i;
        byte[] bArr2 = new byte[i2];
        System.arraycopy(bArr, i, bArr2, 0, i2);
        return bArr2;
    }

    public static String toBinaryString(byte[] bArr) {
        String str = "";
        for (int i = 0; i < bArr.length; i++) {
            byte b = bArr[i];
            String str2 = str;
            for (int i2 = 0; i2 < 8; i2++) {
                int i3 = (b >>> i2) & 1;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str2);
                stringBuilder.append(i3);
                str2 = stringBuilder.toString();
            }
            if (i != bArr.length - 1) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(str2);
                stringBuilder2.append(" ");
                str = stringBuilder2.toString();
            } else {
                str = str2;
            }
        }
        return str;
    }

    public static char[] toCharArray(byte[] bArr) {
        char[] cArr = new char[bArr.length];
        for (int i = 0; i < bArr.length; i++) {
            cArr[i] = (char) bArr[i];
        }
        return cArr;
    }

    public static String toHexString(byte[] bArr) {
        String str = "";
        for (int i = 0; i < bArr.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(HEX_CHARS[(bArr[i] >>> 4) & 15]);
            str = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(HEX_CHARS[bArr[i] & 15]);
            str = stringBuilder.toString();
        }
        return str;
    }

    public static String toHexString(byte[] bArr, String str, String str2) {
        String str3 = new String(str);
        for (int i = 0; i < bArr.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str3);
            stringBuilder.append(HEX_CHARS[(bArr[i] >>> 4) & 15]);
            str3 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(str3);
            stringBuilder.append(HEX_CHARS[bArr[i] & 15]);
            str3 = stringBuilder.toString();
            if (i < bArr.length - 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(str3);
                stringBuilder.append(str2);
                str3 = stringBuilder.toString();
            }
        }
        return str3;
    }

    public static byte[] xor(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[bArr.length];
        for (int length = bArr.length - 1; length >= 0; length--) {
            bArr3[length] = (byte) (bArr[length] ^ bArr2[length]);
        }
        return bArr3;
    }
}
