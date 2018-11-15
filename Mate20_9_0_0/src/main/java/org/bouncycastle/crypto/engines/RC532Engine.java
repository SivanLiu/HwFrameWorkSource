package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RC5Parameters;

public class RC532Engine implements BlockCipher {
    private static final int P32 = -1209970333;
    private static final int Q32 = -1640531527;
    private int[] _S = null;
    private int _noRounds = 12;
    private boolean forEncryption;

    private int bytesToWord(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 24) | (((bArr[i] & 255) | ((bArr[i + 1] & 255) << 8)) | ((bArr[i + 2] & 255) << 16));
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToWord = bytesToWord(bArr, i);
        int bytesToWord2 = bytesToWord(bArr, i + 4);
        for (i = this._noRounds; i >= 1; i--) {
            int i3 = 2 * i;
            bytesToWord2 = rotateRight(bytesToWord2 - this._S[i3 + 1], bytesToWord) ^ bytesToWord;
            bytesToWord = rotateRight(bytesToWord - this._S[i3], bytesToWord2) ^ bytesToWord2;
        }
        wordToBytes(bytesToWord - this._S[0], bArr2, i2);
        wordToBytes(bytesToWord2 - this._S[1], bArr2, i2 + 4);
        return 8;
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToWord = bytesToWord(bArr, i) + this._S[0];
        i = bytesToWord(bArr, i + 4) + this._S[1];
        for (int i3 = 1; i3 <= this._noRounds; i3++) {
            int i4 = 2 * i3;
            bytesToWord = rotateLeft(bytesToWord ^ i, i) + this._S[i4];
            i = rotateLeft(i ^ bytesToWord, bytesToWord) + this._S[i4 + 1];
        }
        wordToBytes(bytesToWord, bArr2, i2);
        wordToBytes(i, bArr2, i2 + 4);
        return 8;
    }

    private int rotateLeft(int i, int i2) {
        i2 &= 31;
        return (i >>> (32 - i2)) | (i << i2);
    }

    private int rotateRight(int i, int i2) {
        i2 &= 31;
        return (i << (32 - i2)) | (i >>> i2);
    }

    private void setKey(byte[] bArr) {
        int i;
        int i2;
        int[] iArr = new int[((bArr.length + 3) / 4)];
        int i3 = 0;
        for (i = 0; i != bArr.length; i++) {
            int i4 = i / 4;
            iArr[i4] = iArr[i4] + ((bArr[i] & 255) << (8 * (i % 4)));
        }
        this._S = new int[(2 * (this._noRounds + 1))];
        this._S[0] = P32;
        for (i2 = 1; i2 < this._S.length; i2++) {
            this._S[i2] = this._S[i2 - 1] + Q32;
        }
        i2 = (iArr.length > this._S.length ? iArr.length : this._S.length) * 3;
        i = 0;
        int i5 = i;
        int i6 = i5;
        int i7 = i6;
        while (i3 < i2) {
            int[] iArr2 = this._S;
            i5 = rotateLeft((this._S[i] + i5) + i6, 3);
            iArr2[i] = i5;
            i6 = rotateLeft((iArr[i7] + i5) + i6, i6 + i5);
            iArr[i7] = i6;
            i = (i + 1) % this._S.length;
            i7 = (i7 + 1) % iArr.length;
            i3++;
        }
    }

    private void wordToBytes(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        bArr[i2 + 1] = (byte) (i >> 8);
        bArr[i2 + 2] = (byte) (i >> 16);
        bArr[i2 + 3] = (byte) (i >> 24);
    }

    public String getAlgorithmName() {
        return "RC5-32";
    }

    public int getBlockSize() {
        return 8;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        byte[] key;
        if (cipherParameters instanceof RC5Parameters) {
            RC5Parameters rC5Parameters = (RC5Parameters) cipherParameters;
            this._noRounds = rC5Parameters.getRounds();
            key = rC5Parameters.getKey();
        } else if (cipherParameters instanceof KeyParameter) {
            key = ((KeyParameter) cipherParameters).getKey();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid parameter passed to RC532 init - ");
            stringBuilder.append(cipherParameters.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        setKey(key);
        this.forEncryption = z;
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        return this.forEncryption ? encryptBlock(bArr, i, bArr2, i2) : decryptBlock(bArr, i, bArr2, i2);
    }

    public void reset() {
    }
}
