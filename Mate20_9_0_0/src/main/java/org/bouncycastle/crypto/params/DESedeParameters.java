package org.bouncycastle.crypto.params;

public class DESedeParameters extends DESParameters {
    public static final int DES_EDE_KEY_LENGTH = 24;

    public DESedeParameters(byte[] bArr) {
        super(bArr);
        if (isWeakKey(bArr, 0, bArr.length)) {
            throw new IllegalArgumentException("attempt to create weak DESede key");
        }
    }

    public static boolean isReal2Key(byte[] bArr, int i) {
        boolean z = false;
        for (int i2 = i; i2 != i + 8; i2++) {
            if (bArr[i2] != bArr[i2 + 8]) {
                z = true;
            }
        }
        return z;
    }

    public static boolean isReal3Key(byte[] bArr, int i) {
        int i2 = i;
        int i3 = 0;
        int i4 = i3;
        int i5 = i4;
        while (true) {
            int i6 = 1;
            if (i2 == i + 8) {
                break;
            }
            int i7 = i2 + 8;
            i3 |= bArr[i2] != bArr[i7] ? 1 : 0;
            int i8 = i2 + 16;
            i4 |= bArr[i2] != bArr[i8] ? 1 : 0;
            if (bArr[i7] == bArr[i8]) {
                i6 = 0;
            }
            i5 |= i6;
            i2++;
        }
        return (i3 == 0 || i4 == 0 || i5 == 0) ? false : true;
    }

    public static boolean isRealEDEKey(byte[] bArr, int i) {
        return bArr.length == 16 ? isReal2Key(bArr, i) : isReal3Key(bArr, i);
    }

    public static boolean isWeakKey(byte[] bArr, int i) {
        return isWeakKey(bArr, i, bArr.length - i);
    }

    public static boolean isWeakKey(byte[] bArr, int i, int i2) {
        while (i < i2) {
            if (DESParameters.isWeakKey(bArr, i)) {
                return true;
            }
            i += 8;
        }
        return false;
    }
}
