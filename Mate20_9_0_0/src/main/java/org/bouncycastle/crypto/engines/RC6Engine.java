package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC6Engine implements BlockCipher {
    private static final int LGW = 5;
    private static final int P32 = -1209970333;
    private static final int Q32 = -1640531527;
    private static final int _noRounds = 20;
    private static final int bytesPerWord = 4;
    private static final int wordSize = 32;
    private int[] _S = null;
    private boolean forEncryption;

    private int bytesToWord(byte[] bArr, int i) {
        int i2 = 0;
        for (int i3 = 3; i3 >= 0; i3--) {
            i2 = (i2 << 8) + (bArr[i3 + i] & 255);
        }
        return i2;
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToWord = bytesToWord(bArr, i);
        int bytesToWord2 = bytesToWord(bArr, i + 4);
        int bytesToWord3 = bytesToWord(bArr, i + 8);
        int bytesToWord4 = bytesToWord(bArr, i + 12);
        bytesToWord3 -= this._S[43];
        bytesToWord -= this._S[42];
        i = 20;
        while (i >= 1) {
            int rotateLeft = rotateLeft(((2 * bytesToWord) + 1) * bytesToWord, 5);
            int rotateLeft2 = rotateLeft(((2 * bytesToWord3) + 1) * bytesToWord3, 5);
            int i3 = 2 * i;
            i--;
            int i4 = bytesToWord;
            bytesToWord = rotateRight(bytesToWord4 - this._S[i3], rotateLeft2) ^ rotateLeft;
            bytesToWord4 = bytesToWord3;
            bytesToWord3 = rotateRight(bytesToWord2 - this._S[i3 + 1], rotateLeft) ^ rotateLeft2;
            bytesToWord2 = i4;
        }
        bytesToWord4 -= this._S[1];
        bytesToWord2 -= this._S[0];
        wordToBytes(bytesToWord, bArr2, i2);
        wordToBytes(bytesToWord2, bArr2, i2 + 4);
        wordToBytes(bytesToWord3, bArr2, i2 + 8);
        wordToBytes(bytesToWord4, bArr2, i2 + 12);
        return 16;
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToWord = bytesToWord(bArr, i);
        int bytesToWord2 = bytesToWord(bArr, i + 4);
        int bytesToWord3 = bytesToWord(bArr, i + 8);
        i = bytesToWord;
        bytesToWord = bytesToWord2 + this._S[0];
        bytesToWord2 = bytesToWord3;
        bytesToWord3 = bytesToWord(bArr, i + 12) + this._S[1];
        int i3 = 1;
        while (i3 <= 20) {
            int rotateLeft = rotateLeft(((2 * bytesToWord) + 1) * bytesToWord, 5);
            int rotateLeft2 = rotateLeft(((2 * bytesToWord3) + 1) * bytesToWord3, 5);
            int i4 = 2 * i3;
            i3++;
            int i5 = bytesToWord3;
            bytesToWord3 = rotateLeft(i ^ rotateLeft, rotateLeft2) + this._S[i4];
            i = bytesToWord;
            bytesToWord = rotateLeft(bytesToWord2 ^ rotateLeft2, rotateLeft) + this._S[i4 + 1];
            bytesToWord2 = i5;
        }
        bytesToWord2 += this._S[43];
        wordToBytes(i + this._S[42], bArr2, i2);
        wordToBytes(bytesToWord, bArr2, i2 + 4);
        wordToBytes(bytesToWord2, bArr2, i2 + 8);
        wordToBytes(bytesToWord3, bArr2, i2 + 12);
        return 16;
    }

    private int rotateLeft(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    private int rotateRight(int i, int i2) {
        return (i << (-i2)) | (i >>> i2);
    }

    private void setKey(byte[] bArr) {
        int length;
        int i;
        int i2;
        int length2 = (bArr.length + 3) / 4;
        int[] iArr = new int[(((bArr.length + 4) - 1) / 4)];
        for (length = bArr.length - 1; length >= 0; length--) {
            i = length / 4;
            iArr[i] = (iArr[i] << 8) + (bArr[length] & 255);
        }
        this._S = new int[44];
        i = 0;
        this._S[0] = P32;
        for (i2 = 1; i2 < this._S.length; i2++) {
            this._S[i2] = this._S[i2 - 1] + Q32;
        }
        i2 = (iArr.length > this._S.length ? iArr.length : this._S.length) * 3;
        length = 0;
        int i3 = length;
        int i4 = i3;
        int i5 = i4;
        while (i < i2) {
            int[] iArr2 = this._S;
            i3 = rotateLeft((this._S[length] + i3) + i4, 3);
            iArr2[length] = i3;
            i4 = rotateLeft((iArr[i5] + i3) + i4, i4 + i3);
            iArr[i5] = i4;
            length = (length + 1) % this._S.length;
            i5 = (i5 + 1) % iArr.length;
            i++;
        }
    }

    private void wordToBytes(int i, byte[] bArr, int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            bArr[i3 + i2] = (byte) i;
            i >>>= 8;
        }
    }

    public String getAlgorithmName() {
        return "RC6";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            KeyParameter keyParameter = (KeyParameter) cipherParameters;
            this.forEncryption = z;
            setKey(keyParameter.getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to RC6 init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int blockSize = getBlockSize();
        if (this._S == null) {
            throw new IllegalStateException("RC6 engine not initialised");
        } else if (i + blockSize > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (blockSize + i2 <= bArr2.length) {
            return this.forEncryption ? encryptBlock(bArr, i, bArr2, i2) : decryptBlock(bArr, i, bArr2, i2);
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
