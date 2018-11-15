package org.bouncycastle.crypto.engines;

public final class CAST6Engine extends CAST5Engine {
    protected static final int BLOCK_SIZE = 16;
    protected static final int ROUNDS = 12;
    protected int[] _Km = new int[48];
    protected int[] _Kr = new int[48];
    protected int[] _Tm = new int[192];
    protected int[] _Tr = new int[192];
    private int[] _workingKey = new int[8];

    protected final void CAST_Decipher(int i, int i2, int i3, int i4, int[] iArr) {
        int i5;
        int i6;
        int i7 = i;
        i = 0;
        while (true) {
            i5 = 6;
            if (i >= 6) {
                break;
            }
            i5 = (11 - i) * 4;
            i3 ^= F1(i4, this._Km[i5], this._Kr[i5]);
            i6 = i5 + 1;
            i2 ^= F2(i3, this._Km[i6], this._Kr[i6]);
            i6 = i5 + 2;
            i7 ^= F3(i2, this._Km[i6], this._Kr[i6]);
            i5 += 3;
            i4 ^= F1(i7, this._Km[i5], this._Kr[i5]);
            i++;
        }
        while (i5 < 12) {
            i = (11 - i5) * 4;
            i6 = i + 3;
            i4 ^= F1(i7, this._Km[i6], this._Kr[i6]);
            i6 = i + 2;
            i7 ^= F3(i2, this._Km[i6], this._Kr[i6]);
            i6 = i + 1;
            i2 ^= F2(i3, this._Km[i6], this._Kr[i6]);
            i3 ^= F1(i4, this._Km[i], this._Kr[i]);
            i5++;
        }
        iArr[0] = i7;
        iArr[1] = i2;
        iArr[2] = i3;
        iArr[3] = i4;
    }

    protected final void CAST_Encipher(int i, int i2, int i3, int i4, int[] iArr) {
        int i5;
        int i6;
        int i7 = i;
        i = 0;
        while (true) {
            i5 = 6;
            if (i >= 6) {
                break;
            }
            i5 = i * 4;
            i3 ^= F1(i4, this._Km[i5], this._Kr[i5]);
            i6 = i5 + 1;
            i2 ^= F2(i3, this._Km[i6], this._Kr[i6]);
            i6 = i5 + 2;
            i7 ^= F3(i2, this._Km[i6], this._Kr[i6]);
            i5 += 3;
            i4 ^= F1(i7, this._Km[i5], this._Kr[i5]);
            i++;
        }
        while (i5 < 12) {
            i = i5 * 4;
            i6 = i + 3;
            i4 ^= F1(i7, this._Km[i6], this._Kr[i6]);
            i6 = i + 2;
            i7 ^= F3(i2, this._Km[i6], this._Kr[i6]);
            i6 = i + 1;
            i2 ^= F2(i3, this._Km[i6], this._Kr[i6]);
            i3 ^= F1(i4, this._Km[i], this._Kr[i]);
            i5++;
        }
        iArr[0] = i7;
        iArr[1] = i2;
        iArr[2] = i3;
        iArr[3] = i4;
    }

    protected int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int[] iArr = new int[4];
        CAST_Decipher(BytesTo32bits(bArr, i), BytesTo32bits(bArr, i + 4), BytesTo32bits(bArr, i + 8), BytesTo32bits(bArr, i + 12), iArr);
        Bits32ToBytes(iArr[0], bArr2, i2);
        Bits32ToBytes(iArr[1], bArr2, i2 + 4);
        Bits32ToBytes(iArr[2], bArr2, i2 + 8);
        Bits32ToBytes(iArr[3], bArr2, i2 + 12);
        return 16;
    }

    protected int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int[] iArr = new int[4];
        CAST_Encipher(BytesTo32bits(bArr, i), BytesTo32bits(bArr, i + 4), BytesTo32bits(bArr, i + 8), BytesTo32bits(bArr, i + 12), iArr);
        Bits32ToBytes(iArr[0], bArr2, i2);
        Bits32ToBytes(iArr[1], bArr2, i2 + 4);
        Bits32ToBytes(iArr[2], bArr2, i2 + 8);
        Bits32ToBytes(iArr[3], bArr2, i2 + 12);
        return 16;
    }

    public String getAlgorithmName() {
        return "CAST6";
    }

    public int getBlockSize() {
        return 16;
    }

    public void reset() {
    }

    protected void setKey(byte[] bArr) {
        int i;
        int i2;
        Object obj = bArr;
        int i3 = 19;
        int i4 = 1518500249;
        int i5 = 0;
        while (i5 < 24) {
            int i6 = i3;
            i3 = i4;
            for (i4 = 0; i4 < 8; i4++) {
                i = (i5 * 8) + i4;
                this._Tm[i] = i3;
                i3 += 1859775393;
                this._Tr[i] = i6;
                i6 = (i6 + 17) & 31;
            }
            i5++;
            i4 = i3;
            i3 = i6;
        }
        Object obj2 = new byte[64];
        System.arraycopy(obj, 0, obj2, 0, obj.length);
        for (i2 = 0; i2 < 8; i2++) {
            this._workingKey[i2] = BytesTo32bits(obj2, i2 * 4);
        }
        for (i2 = 0; i2 < 12; i2++) {
            i5 = i2 * 2;
            i4 = i5 * 8;
            int[] iArr = this._workingKey;
            iArr[6] = iArr[6] ^ F1(this._workingKey[7], this._Tm[i4], this._Tr[i4]);
            iArr = this._workingKey;
            int i7 = i4 + 1;
            iArr[5] = iArr[5] ^ F2(this._workingKey[6], this._Tm[i7], this._Tr[i7]);
            iArr = this._workingKey;
            int i8 = i4 + 2;
            iArr[4] = iArr[4] ^ F3(this._workingKey[5], this._Tm[i8], this._Tr[i8]);
            iArr = this._workingKey;
            int i9 = i4 + 3;
            iArr[3] = F1(this._workingKey[4], this._Tm[i9], this._Tr[i9]) ^ iArr[3];
            iArr = this._workingKey;
            i9 = i4 + 4;
            iArr[2] = F2(this._workingKey[3], this._Tm[i9], this._Tr[i9]) ^ iArr[2];
            iArr = this._workingKey;
            i9 = i4 + 5;
            iArr[1] = F3(this._workingKey[2], this._Tm[i9], this._Tr[i9]) ^ iArr[1];
            iArr = this._workingKey;
            i8 = i4 + 6;
            iArr[0] = iArr[0] ^ F1(this._workingKey[1], this._Tm[i8], this._Tr[i8]);
            iArr = this._workingKey;
            i4 += 7;
            iArr[7] = F2(this._workingKey[0], this._Tm[i4], this._Tr[i4]) ^ iArr[7];
            i5 = (i5 + 1) * 8;
            int[] iArr2 = this._workingKey;
            iArr2[6] = iArr2[6] ^ F1(this._workingKey[7], this._Tm[i5], this._Tr[i5]);
            iArr2 = this._workingKey;
            i7 = i5 + 1;
            iArr2[5] = iArr2[5] ^ F2(this._workingKey[6], this._Tm[i7], this._Tr[i7]);
            iArr2 = this._workingKey;
            i8 = i5 + 2;
            iArr2[4] = iArr2[4] ^ F3(this._workingKey[5], this._Tm[i8], this._Tr[i8]);
            iArr2 = this._workingKey;
            i8 = i5 + 3;
            iArr2[3] = F1(this._workingKey[4], this._Tm[i8], this._Tr[i8]) ^ iArr2[3];
            iArr2 = this._workingKey;
            i8 = i5 + 4;
            iArr2[2] = F2(this._workingKey[3], this._Tm[i8], this._Tr[i8]) ^ iArr2[2];
            iArr2 = this._workingKey;
            i7 = i5 + 5;
            iArr2[1] = F3(this._workingKey[2], this._Tm[i7], this._Tr[i7]) ^ iArr2[1];
            iArr2 = this._workingKey;
            i7 = i5 + 6;
            iArr2[0] = iArr2[0] ^ F1(this._workingKey[1], this._Tm[i7], this._Tr[i7]);
            iArr2 = this._workingKey;
            i5 += 7;
            iArr2[7] = F2(this._workingKey[0], this._Tm[i5], this._Tr[i5]) ^ iArr2[7];
            i4 = i2 * 4;
            this._Kr[i4] = this._workingKey[0] & 31;
            i3 = i4 + 1;
            this._Kr[i3] = this._workingKey[2] & 31;
            i = i4 + 2;
            this._Kr[i] = this._workingKey[4] & 31;
            int i10 = i4 + 3;
            this._Kr[i10] = this._workingKey[6] & 31;
            this._Km[i4] = this._workingKey[7];
            this._Km[i3] = this._workingKey[5];
            this._Km[i] = this._workingKey[3];
            this._Km[i10] = this._workingKey[1];
        }
    }
}
