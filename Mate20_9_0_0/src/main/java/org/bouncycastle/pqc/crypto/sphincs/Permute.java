package org.bouncycastle.pqc.crypto.sphincs;

import org.bouncycastle.util.Pack;

class Permute {
    private static final int CHACHA_ROUNDS = 12;

    Permute() {
    }

    public static void permute(int i, int[] iArr) {
        int[] iArr2 = iArr;
        int i2 = 16;
        if (iArr2.length != 16) {
            throw new IllegalArgumentException();
        } else if (i % 2 == 0) {
            int rotl;
            int i3 = 0;
            int i4 = iArr2[0];
            int i5 = iArr2[1];
            int i6 = iArr2[2];
            int i7 = iArr2[3];
            int i8 = iArr2[4];
            int i9 = iArr2[5];
            int i10 = iArr2[6];
            int i11 = 7;
            int i12 = iArr2[7];
            int i13 = 8;
            int i14 = iArr2[8];
            int i15 = iArr2[9];
            int i16 = iArr2[10];
            int i17 = iArr2[11];
            int i18 = iArr2[12];
            int i19 = iArr2[13];
            int i20 = iArr2[14];
            int i21 = iArr2[15];
            int i22 = i;
            while (i22 > 0) {
                i4 += i8;
                int rotl2 = rotl(i18 ^ i4, i2);
                i14 += rotl2;
                i8 = rotl(i8 ^ i14, 12);
                i4 += i8;
                rotl2 = rotl(rotl2 ^ i4, i13);
                i14 += rotl2;
                i8 = rotl(i8 ^ i14, i11);
                i5 += i9;
                int rotl3 = rotl(i19 ^ i5, i2);
                i15 += rotl3;
                i9 = rotl(i9 ^ i15, 12);
                i5 += i9;
                rotl3 = rotl(rotl3 ^ i5, i13);
                i15 += rotl3;
                i9 = rotl(i9 ^ i15, i11);
                i6 += i10;
                rotl = rotl(i20 ^ i6, i2);
                i16 += rotl;
                i3 = rotl(i10 ^ i16, 12);
                i6 += i3;
                rotl = rotl(rotl ^ i6, i13);
                i16 += rotl;
                i3 = rotl(i3 ^ i16, i11);
                i7 += i12;
                i11 = rotl(i21 ^ i7, i2);
                i17 += i11;
                i2 = rotl(i12 ^ i17, 12);
                i7 += i2;
                i11 = rotl(i11 ^ i7, i13);
                i17 += i11;
                i2 = rotl(i2 ^ i17, 7);
                i4 += i9;
                i13 = rotl(i11 ^ i4, 16);
                i16 += i13;
                i9 = rotl(i9 ^ i16, 12);
                i4 += i9;
                i21 = rotl(i13 ^ i4, 8);
                i16 += i21;
                i13 = rotl(i9 ^ i16, 7);
                i5 += i3;
                rotl2 = rotl(rotl2 ^ i5, 16);
                i17 += rotl2;
                i3 = rotl(i3 ^ i17, 12);
                i5 += i3;
                i18 = rotl(rotl2 ^ i5, 8);
                i17 += i18;
                i10 = rotl(i3 ^ i17, 7);
                i6 += i2;
                i3 = rotl(rotl3 ^ i6, 16);
                i14 += i3;
                i2 = rotl(i2 ^ i14, 12);
                i6 += i2;
                i19 = rotl(i3 ^ i6, 8);
                i14 += i19;
                i12 = rotl(i2 ^ i14, 7);
                i7 += i8;
                i2 = rotl(rotl ^ i7, 16);
                i15 += i2;
                rotl = rotl(i8 ^ i15, 12);
                i7 += rotl;
                i20 = rotl(i2 ^ i7, 8);
                i15 += i20;
                i8 = rotl(rotl ^ i15, 7);
                i22 -= 2;
                i2 = 16;
                i11 = 7;
                i9 = i13;
                i3 = 0;
                i13 = 8;
            }
            rotl = i11;
            iArr2[i3] = i4;
            iArr2[1] = i5;
            iArr2[2] = i6;
            iArr2[3] = i7;
            iArr2[4] = i8;
            iArr2[5] = i9;
            iArr2[6] = i10;
            iArr2[rotl] = i12;
            iArr2[8] = i14;
            iArr2[9] = i15;
            iArr2[10] = i16;
            iArr2[11] = i17;
            iArr2[12] = i18;
            iArr2[13] = i19;
            iArr2[14] = i20;
            iArr2[15] = i21;
        } else {
            throw new IllegalArgumentException("Number of rounds must be even");
        }
    }

    protected static int rotl(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    void chacha_permute(byte[] bArr, byte[] bArr2) {
        int[] iArr = new int[16];
        int i = 0;
        for (int i2 = 0; i2 < 16; i2++) {
            iArr[i2] = Pack.littleEndianToInt(bArr2, 4 * i2);
        }
        permute(12, iArr);
        while (i < 16) {
            Pack.intToLittleEndian(iArr[i], bArr, 4 * i);
            i++;
        }
    }
}
