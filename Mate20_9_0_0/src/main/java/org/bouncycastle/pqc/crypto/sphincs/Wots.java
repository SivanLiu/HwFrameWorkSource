package org.bouncycastle.pqc.crypto.sphincs;

class Wots {
    static final int WOTS_L = 67;
    static final int WOTS_L1 = 64;
    static final int WOTS_LOGW = 4;
    static final int WOTS_LOG_L = 7;
    static final int WOTS_SIGBYTES = 2144;
    static final int WOTS_W = 16;

    Wots() {
    }

    private static void clear(byte[] bArr, int i, int i2) {
        for (int i3 = 0; i3 != i2; i3++) {
            bArr[i3 + i] = (byte) 0;
        }
    }

    static void expand_seed(byte[] bArr, int i, byte[] bArr2, int i2) {
        clear(bArr, i, WOTS_SIGBYTES);
        Seed.prg(bArr, i, 2144, bArr2, i2);
    }

    static void gen_chain(HashFunctions hashFunctions, byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3, int i4) {
        int i5 = 0;
        for (int i6 = 0; i6 < 32; i6++) {
            bArr[i6 + i] = bArr2[i6 + i2];
        }
        while (i5 < i4 && i5 < 16) {
            hashFunctions.hash_n_n_mask(bArr, i, bArr, i, bArr3, i3 + (i5 * 32));
            i5++;
        }
    }

    void wots_pkgen(HashFunctions hashFunctions, byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) {
        expand_seed(bArr, i, bArr2, i2);
        for (int i4 = 0; i4 < 67; i4++) {
            int i5 = i + (i4 * 32);
            gen_chain(hashFunctions, bArr, i5, bArr, i5, bArr3, i3, 15);
        }
    }

    void wots_sign(HashFunctions hashFunctions, byte[] bArr, int i, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        int i2 = i;
        int[] iArr = new int[67];
        int i3 = 0;
        int i4 = 0;
        int i5 = i4;
        while (i4 < 64) {
            int i6 = i4 / 2;
            iArr[i4] = bArr2[i6] & 15;
            int i7 = i4 + 1;
            iArr[i7] = (bArr2[i6] & 255) >>> 4;
            i5 = (i5 + (15 - iArr[i4])) + (15 - iArr[i7]);
            i4 += 2;
        }
        while (i4 < 67) {
            iArr[i4] = i5 & 15;
            i5 >>>= 4;
            i4++;
        }
        byte[] bArr5 = bArr;
        expand_seed(bArr5, i2, bArr3, 0);
        while (i3 < 67) {
            int i8 = i2 + (i3 * 32);
            gen_chain(hashFunctions, bArr5, i8, bArr5, i8, bArr4, 0, iArr[i3]);
            i3++;
        }
    }

    void wots_verify(HashFunctions hashFunctions, byte[] bArr, byte[] bArr2, int i, byte[] bArr3, byte[] bArr4) {
        int i2;
        int[] iArr = new int[67];
        int i3 = 0;
        int i4 = 0;
        int i5 = i4;
        while (i4 < 64) {
            i2 = i4 / 2;
            iArr[i4] = bArr3[i2] & 15;
            int i6 = i4 + 1;
            iArr[i6] = (bArr3[i2] & 255) >>> 4;
            i5 = (i5 + (15 - iArr[i4])) + (15 - iArr[i6]);
            i4 += 2;
        }
        while (i4 < 67) {
            iArr[i4] = i5 & 15;
            i5 >>>= 4;
            i4++;
        }
        while (i3 < 67) {
            i2 = i3 * 32;
            gen_chain(hashFunctions, bArr, i2, bArr2, i + i2, bArr4, iArr[i3] * 32, 15 - iArr[i3]);
            i3++;
        }
    }
}
