package org.bouncycastle.pqc.crypto.sphincs;

import org.bouncycastle.asn1.cmp.PKIFailureInfo;

class Horst {
    static final int HORST_K = 32;
    static final int HORST_LOGT = 16;
    static final int HORST_SIGBYTES = 13312;
    static final int HORST_SKBYTES = 32;
    static final int HORST_T = 65536;
    static final int N_MASKS = 32;

    Horst() {
    }

    static void expand_seed(byte[] bArr, byte[] bArr2) {
        Seed.prg(bArr, 0, 2097152, bArr2, 0);
    }

    static int horst_sign(HashFunctions hashFunctions, byte[] bArr, int i, byte[] bArr2, byte[] bArr3, byte[] bArr4, byte[] bArr5) {
        int i2;
        int i3;
        int i4;
        int i5;
        byte[] bArr6 = new byte[PKIFailureInfo.badSenderNonce];
        byte[] bArr7 = new byte[4194272];
        expand_seed(bArr6, bArr3);
        for (i2 = 0; i2 < 65536; i2++) {
            hashFunctions.hash_n_n(bArr7, (65535 + i2) * 32, bArr6, i2 * 32);
        }
        HashFunctions hashFunctions2 = hashFunctions;
        int i6 = 0;
        while (i6 < 16) {
            i2 = 16 - i6;
            long j = (long) ((1 << i2) - 1);
            i3 = 1 << (i2 - 1);
            long j2 = (long) (i3 - 1);
            int i7 = 0;
            while (i7 < i3) {
                int i8 = i7;
                long j3 = j2;
                i4 = i3;
                long j4 = j;
                hashFunctions.hash_2n_n_mask(bArr7, (int) ((((long) i7) + j2) * 32), bArr7, (int) ((((long) (2 * i7)) + j) * 32), bArr4, (2 * i6) * 32);
                i7 = i8 + 1;
                hashFunctions2 = hashFunctions;
                i3 = i4;
                j2 = j3;
                j = j4;
            }
            i6++;
            hashFunctions2 = hashFunctions;
        }
        i2 = 2016;
        int i9 = i;
        while (i2 < 4064) {
            i5 = i9 + 1;
            bArr[i9] = bArr7[i2];
            i2++;
            i9 = i5;
        }
        i2 = 0;
        while (i2 < 32) {
            int i10;
            i5 = 2 * i2;
            i3 = (bArr5[i5] & 255) + ((bArr5[i5 + 1] & 255) << 8);
            i5 = i9;
            i9 = 0;
            while (i9 < 32) {
                i10 = i5 + 1;
                bArr[i5] = bArr6[(i3 * 32) + i9];
                i9++;
                i5 = i10;
            }
            i3 += 65535;
            i10 = i5;
            i5 = 0;
            while (i5 < 10) {
                i3 = (i3 & 1) != 0 ? i3 + 1 : i3 - 1;
                int i11 = i10;
                i10 = 0;
                while (i10 < 32) {
                    i4 = i11 + 1;
                    bArr[i11] = bArr7[(i3 * 32) + i10];
                    i10++;
                    i11 = i4;
                }
                i3 = (i3 - 1) / 2;
                i5++;
                i10 = i11;
            }
            i2++;
            i9 = i10;
        }
        int i12 = 32;
        for (int i13 = 0; i13 < i12; i13++) {
            bArr2[i13] = bArr7[i13];
        }
        return HORST_SIGBYTES;
    }

    static int horst_verify(HashFunctions hashFunctions, byte[] bArr, byte[] bArr2, int i, byte[] bArr3, byte[] bArr4) {
        HashFunctions hashFunctions2 = hashFunctions;
        byte[] bArr5 = bArr2;
        int i2 = i;
        byte[] bArr6 = new byte[1024];
        int i3 = i2 + 2048;
        int i4 = 0;
        int i5 = 0;
        while (true) {
            int i6 = 32;
            if (i5 < 32) {
                int i7 = 2 * i5;
                int i8 = (bArr4[i7] & 255) + ((bArr4[i7 + 1] & 255) << 8);
                if ((i8 & 1) == 0) {
                    hashFunctions2.hash_n_n(bArr6, i4, bArr5, i3);
                    for (i7 = i4; i7 < 32; i7++) {
                        bArr6[32 + i7] = bArr5[(i3 + 32) + i7];
                    }
                } else {
                    hashFunctions2.hash_n_n(bArr6, 32, bArr5, i3);
                    for (i7 = i4; i7 < 32; i7++) {
                        bArr6[i7] = bArr5[(i3 + 32) + i7];
                    }
                }
                int i9 = i3 + 64;
                int i10 = 1;
                while (i10 < 10) {
                    int i11 = i8 >>> 1;
                    if ((i11 & 1) == 0) {
                        i4 = i6;
                        hashFunctions2.hash_2n_n_mask(bArr6, 0, bArr6, 0, bArr3, ((i10 - 1) * 2) * 32);
                        for (i3 = 0; i3 < i4; i3++) {
                            bArr6[i4 + i3] = bArr5[i9 + i3];
                        }
                    } else {
                        i4 = i6;
                        hashFunctions2.hash_2n_n_mask(bArr6, 32, bArr6, 0, bArr3, ((i10 - 1) * 2) * 32);
                        for (i3 = 0; i3 < i4; i3++) {
                            bArr6[i3] = bArr5[i9 + i3];
                        }
                    }
                    i9 += 32;
                    i10++;
                    i6 = i4;
                    i8 = i11;
                }
                i4 = i6;
                i10 = i8 >>> 1;
                hashFunctions2.hash_2n_n_mask(bArr6, 0, bArr6, 0, bArr3, 576);
                for (i3 = 0; i3 < i4; i3++) {
                    if (bArr5[(i2 + (i10 * 32)) + i3] != bArr6[i3]) {
                        for (i3 = 0; i3 < i4; i3++) {
                            bArr[i3] = (byte) 0;
                        }
                        return -1;
                    }
                }
                i5++;
                i3 = i9;
                i4 = 0;
            } else {
                HashFunctions hashFunctions3;
                byte[] bArr7;
                int i12;
                i4 = 32;
                for (int i13 = 0; i13 < i4; i13++) {
                    hashFunctions3 = hashFunctions2;
                    bArr7 = bArr6;
                    hashFunctions3.hash_2n_n_mask(bArr7, i13 * 32, bArr5, i2 + ((2 * i13) * i4), bArr3, 640);
                }
                for (i12 = 0; i12 < 16; i12++) {
                    hashFunctions3 = hashFunctions2;
                    bArr7 = bArr6;
                    hashFunctions3.hash_2n_n_mask(bArr7, i12 * 32, bArr6, (2 * i12) * 32, bArr3, 704);
                }
                for (i12 = 0; i12 < 8; i12++) {
                    hashFunctions3 = hashFunctions2;
                    bArr7 = bArr6;
                    hashFunctions3.hash_2n_n_mask(bArr7, i12 * 32, bArr6, (2 * i12) * 32, bArr3, 768);
                }
                for (i12 = 0; i12 < 4; i12++) {
                    hashFunctions3 = hashFunctions2;
                    bArr7 = bArr6;
                    hashFunctions3.hash_2n_n_mask(bArr7, i12 * 32, bArr6, (2 * i12) * 32, bArr3, 832);
                }
                for (i12 = 0; i12 < 2; i12++) {
                    hashFunctions3 = hashFunctions2;
                    bArr7 = bArr6;
                    hashFunctions3.hash_2n_n_mask(bArr7, i12 * 32, bArr6, (2 * i12) * 32, bArr3, 896);
                }
                hashFunctions2.hash_2n_n_mask(bArr, 0, bArr6, 0, bArr3, 960);
                return 0;
            }
        }
    }
}
