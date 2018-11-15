package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.RC5Parameters;

public class RC564Engine implements BlockCipher {
    private static final long P64 = -5196783011329398165L;
    private static final long Q64 = -7046029254386353131L;
    private static final int bytesPerWord = 8;
    private static final int wordSize = 64;
    private long[] _S = null;
    private int _noRounds = 12;
    private boolean forEncryption;

    private long bytesToWord(byte[] bArr, int i) {
        long j = 0;
        for (int i2 = 7; i2 >= 0; i2--) {
            j = (j << 8) + ((long) (bArr[i2 + i] & 255));
        }
        return j;
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        long bytesToWord = bytesToWord(bArr, i);
        long bytesToWord2 = bytesToWord(bArr, i + 8);
        for (int i3 = this._noRounds; i3 >= 1; i3--) {
            int i4 = 2 * i3;
            bytesToWord2 = rotateRight(bytesToWord2 - this._S[i4 + 1], bytesToWord) ^ bytesToWord;
            bytesToWord = rotateRight(bytesToWord - this._S[i4], bytesToWord2) ^ bytesToWord2;
        }
        wordToBytes(bytesToWord - this._S[0], bArr2, i2);
        wordToBytes(bytesToWord2 - this._S[1], bArr2, i2 + 8);
        return 16;
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        long bytesToWord = bytesToWord(bArr, i) + this._S[0];
        long bytesToWord2 = bytesToWord(bArr, i + 8) + this._S[1];
        for (int i3 = 1; i3 <= this._noRounds; i3++) {
            int i4 = 2 * i3;
            bytesToWord = rotateLeft(bytesToWord ^ bytesToWord2, bytesToWord2) + this._S[i4];
            bytesToWord2 = rotateLeft(bytesToWord2 ^ bytesToWord, bytesToWord) + this._S[i4 + 1];
        }
        wordToBytes(bytesToWord, bArr2, i2);
        wordToBytes(bytesToWord2, bArr2, i2 + 8);
        return 16;
    }

    private long rotateLeft(long j, long j2) {
        j2 &= 63;
        return (j >>> ((int) (64 - j2))) | (j << ((int) j2));
    }

    private long rotateRight(long j, long j2) {
        j2 &= 63;
        return (j << ((int) (64 - j2))) | (j >>> ((int) j2));
    }

    private void setKey(byte[] bArr) {
        int i;
        int i2;
        long[] jArr = new long[((bArr.length + 7) / 8)];
        int i3 = 0;
        for (int i4 = 0; i4 != bArr.length; i4++) {
            i = i4 / 8;
            jArr[i] = jArr[i] + (((long) (bArr[i4] & 255)) << ((i4 % 8) * 8));
        }
        this._S = new long[(2 * (this._noRounds + 1))];
        this._S[0] = P64;
        for (i2 = 1; i2 < this._S.length; i2++) {
            this._S[i2] = this._S[i2 - 1] + Q64;
        }
        i = 3 * (jArr.length > this._S.length ? jArr.length : this._S.length);
        long j = 0;
        i2 = 0;
        int i5 = i2;
        long j2 = 0;
        while (i3 < i) {
            long[] jArr2 = this._S;
            j = rotateLeft((this._S[i2] + j) + j2, 3);
            jArr2[i2] = j;
            j2 = rotateLeft((jArr[i5] + j) + j2, j2 + j);
            jArr[i5] = j2;
            i2 = (i2 + 1) % this._S.length;
            i5 = (i5 + 1) % jArr.length;
            i3++;
        }
    }

    private void wordToBytes(long j, byte[] bArr, int i) {
        for (int i2 = 0; i2 < 8; i2++) {
            bArr[i2 + i] = (byte) ((int) j);
            j >>>= 8;
        }
    }

    public String getAlgorithmName() {
        return "RC5-64";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof RC5Parameters) {
            RC5Parameters rC5Parameters = (RC5Parameters) cipherParameters;
            this.forEncryption = z;
            this._noRounds = rC5Parameters.getRounds();
            setKey(rC5Parameters.getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to RC564 init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        return this.forEncryption ? encryptBlock(bArr, i, bArr2, i2) : decryptBlock(bArr, i, bArr2, i2);
    }

    public void reset() {
    }
}
