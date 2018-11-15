package org.bouncycastle.util.encoders;

public class HexTranslator implements Translator {
    private static final byte[] hexTable = new byte[]{(byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102};

    public int decode(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        i2 /= 2;
        for (int i4 = 0; i4 < i2; i4++) {
            int i5 = (i4 * 2) + i;
            byte b = bArr[i5];
            byte b2 = bArr[i5 + 1];
            if (b < (byte) 97) {
                bArr2[i3] = (byte) ((b - 48) << 4);
            } else {
                bArr2[i3] = (byte) (((b - 97) + 10) << 4);
            }
            if (b2 < (byte) 97) {
                bArr2[i3] = (byte) (bArr2[i3] + ((byte) (b2 - 48)));
            } else {
                bArr2[i3] = (byte) (bArr2[i3] + ((byte) ((b2 - 97) + 10)));
            }
            i3++;
        }
        return i2;
    }

    public int encode(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        int i4 = 0;
        int i5 = i;
        i = 0;
        while (i4 < i2) {
            int i6 = i3 + i;
            bArr2[i6] = hexTable[(bArr[i5] >> 4) & 15];
            bArr2[i6 + 1] = hexTable[bArr[i5] & 15];
            i5++;
            i4++;
            i += 2;
        }
        return i2 * 2;
    }

    public int getDecodedBlockSize() {
        return 1;
    }

    public int getEncodedBlockSize() {
        return 2;
    }
}
