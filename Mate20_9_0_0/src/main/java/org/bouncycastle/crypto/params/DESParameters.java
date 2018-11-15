package org.bouncycastle.crypto.params;

public class DESParameters extends KeyParameter {
    public static final int DES_KEY_LENGTH = 8;
    private static byte[] DES_weak_keys = new byte[]{(byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 31, (byte) 31, (byte) 31, (byte) 31, (byte) 14, (byte) 14, (byte) 14, (byte) 14, (byte) -32, (byte) -32, (byte) -32, (byte) -32, (byte) -15, (byte) -15, (byte) -15, (byte) -15, (byte) -2, (byte) -2, (byte) -2, (byte) -2, (byte) -2, (byte) -2, (byte) -2, (byte) -2, (byte) 1, (byte) -2, (byte) 1, (byte) -2, (byte) 1, (byte) -2, (byte) 1, (byte) -2, (byte) 31, (byte) -32, (byte) 31, (byte) -32, (byte) 14, (byte) -15, (byte) 14, (byte) -15, (byte) 1, (byte) -32, (byte) 1, (byte) -32, (byte) 1, (byte) -15, (byte) 1, (byte) -15, (byte) 31, (byte) -2, (byte) 31, (byte) -2, (byte) 14, (byte) -2, (byte) 14, (byte) -2, (byte) 1, (byte) 31, (byte) 1, (byte) 31, (byte) 1, (byte) 14, (byte) 1, (byte) 14, (byte) -32, (byte) -2, (byte) -32, (byte) -2, (byte) -15, (byte) -2, (byte) -15, (byte) -2, (byte) -2, (byte) 1, (byte) -2, (byte) 1, (byte) -2, (byte) 1, (byte) -2, (byte) 1, (byte) -32, (byte) 31, (byte) -32, (byte) 31, (byte) -15, (byte) 14, (byte) -15, (byte) 14, (byte) -32, (byte) 1, (byte) -32, (byte) 1, (byte) -15, (byte) 1, (byte) -15, (byte) 1, (byte) -2, (byte) 31, (byte) -2, (byte) 31, (byte) -2, (byte) 14, (byte) -2, (byte) 14, (byte) 31, (byte) 1, (byte) 31, (byte) 1, (byte) 14, (byte) 1, (byte) 14, (byte) 1, (byte) -2, (byte) -32, (byte) -2, (byte) -32, (byte) -2, (byte) -15, (byte) -2, (byte) -15};
    private static final int N_DES_WEAK_KEYS = 16;

    public DESParameters(byte[] bArr) {
        super(bArr);
        if (isWeakKey(bArr, 0)) {
            throw new IllegalArgumentException("attempt to create weak DES key");
        }
    }

    public static boolean isWeakKey(byte[] bArr, int i) {
        if (bArr.length - i >= 8) {
            int i2 = 0;
            while (i2 < 16) {
                int i3 = 0;
                while (i3 < 8) {
                    if (bArr[i3 + i] != DES_weak_keys[(i2 * 8) + i3]) {
                        i2++;
                    } else {
                        i3++;
                    }
                }
                return true;
            }
            return false;
        }
        throw new IllegalArgumentException("key material too short.");
    }

    public static void setOddParity(byte[] bArr) {
        for (int i = 0; i < bArr.length; i++) {
            byte b = bArr[i];
            bArr[i] = (byte) (((((b >> 7) ^ ((((((b >> 1) ^ (b >> 2)) ^ (b >> 3)) ^ (b >> 4)) ^ (b >> 5)) ^ (b >> 6))) ^ 1) & 1) | (b & 254));
        }
    }
}
