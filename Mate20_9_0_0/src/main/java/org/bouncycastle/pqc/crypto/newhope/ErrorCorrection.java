package org.bouncycastle.pqc.crypto.newhope;

import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;

class ErrorCorrection {
    ErrorCorrection() {
    }

    static short LDDecode(int i, int i2, int i3, int i4) {
        return (short) (((((g(i) + g(i2)) + g(i3)) + g(i4)) - 98312) >>> 31);
    }

    static int abs(int i) {
        int i2 = i >> 31;
        return (i ^ i2) - i2;
    }

    static int f(int[] iArr, int i, int i2, int i3) {
        int i4 = (i3 * 2730) >> 25;
        i4 -= (12288 - (i3 - (i4 * 12289))) >> 31;
        iArr[i] = (i4 >> 1) + (i4 & 1);
        i4--;
        iArr[i2] = (i4 >> 1) + (i4 & 1);
        return abs(i3 - ((iArr[i] * 2) * 12289));
    }

    static int g(int i) {
        int i2 = (i * 2730) >> 27;
        i2 -= (CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA - (i - (CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA * i2))) >> 31;
        return abs((((i2 >> 1) + (i2 & 1)) * 98312) - i);
    }

    static void helpRec(short[] sArr, short[] sArr2, byte[] bArr, byte b) {
        int i = 8;
        byte[] bArr2 = new byte[8];
        bArr2[0] = b;
        byte[] bArr3 = new byte[32];
        ChaCha20.process(bArr, bArr2, bArr3, 0, bArr3.length);
        int[] iArr = new int[8];
        int i2 = 4;
        int[] iArr2 = new int[4];
        int i3 = 0;
        while (i3 < 256) {
            int i4 = 0 + i3;
            int i5 = ((bArr3[i3 >>> 3] >>> (i3 & 7)) & 1) * i2;
            int i6 = 256 + i3;
            int i7 = 512 + i3;
            int i8 = 768 + i3;
            i5 = (24577 - (((f(iArr, 0, i2, (sArr2[i4] * i) + i5) + f(iArr, 1, 5, (sArr2[i6] * i) + i5)) + f(iArr, 2, 6, (sArr2[i7] * i) + i5)) + f(iArr, 3, 7, i5 + (sArr2[i8] * i)))) >> 31;
            int i9 = ~i5;
            iArr2[0] = (iArr[0] & i9) ^ (i5 & iArr[4]);
            iArr2[1] = (i9 & iArr[1]) ^ (iArr[5] & i5);
            iArr2[2] = (iArr[2] & i9) ^ (iArr[6] & i5);
            iArr2[3] = (iArr[7] & i5) ^ (i9 & iArr[3]);
            sArr[i4] = (short) ((iArr2[0] - iArr2[3]) & 3);
            sArr[i6] = (short) ((iArr2[1] - iArr2[3]) & 3);
            sArr[i7] = (short) ((iArr2[2] - iArr2[3]) & 3);
            sArr[i8] = (short) (((-i5) + (2 * iArr2[3])) & 3);
            i3++;
            i2 = 4;
            i = 8;
        }
    }

    static void rec(byte[] bArr, short[] sArr, short[] sArr2) {
        Arrays.fill(bArr, (byte) 0);
        int[] iArr = new int[4];
        for (int i = 0; i < 256; i++) {
            int i2 = 0 + i;
            int i3 = 768 + i;
            iArr[0] = ((sArr[i2] * 8) + 196624) - (((sArr2[i2] * 2) + sArr2[i3]) * 12289);
            int i4 = 256 + i;
            iArr[1] = ((sArr[i4] * 8) + 196624) - (((sArr2[i4] * 2) + sArr2[i3]) * 12289);
            i2 = 512 + i;
            iArr[2] = ((sArr[i2] * 8) + 196624) - (((sArr2[i2] * 2) + sArr2[i3]) * 12289);
            iArr[3] = (196624 + (8 * sArr[i3])) - (12289 * sArr2[i3]);
            int i5 = i >>> 3;
            bArr[i5] = (byte) ((LDDecode(iArr[0], iArr[1], iArr[2], iArr[3]) << (i & 7)) | bArr[i5]);
        }
    }
}
