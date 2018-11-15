package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;

public class XTEAEngine implements BlockCipher {
    private static final int block_size = 8;
    private static final int delta = -1640531527;
    private static final int rounds = 32;
    private int[] _S = new int[4];
    private boolean _forEncryption;
    private boolean _initialised = false;
    private int[] _sum0 = new int[32];
    private int[] _sum1 = new int[32];

    private int bytesToInt(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = i2 + 1;
        i = (bArr[i] << 24) | ((bArr[i2] & 255) << 16);
        return (bArr[i3 + 1] & 255) | (i | ((bArr[i3] & 255) << 8));
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToInt = bytesToInt(bArr, i);
        int bytesToInt2 = bytesToInt(bArr, i + 4);
        for (i = 31; i >= 0; i--) {
            bytesToInt2 -= (((bytesToInt << 4) ^ (bytesToInt >>> 5)) + bytesToInt) ^ this._sum1[i];
            bytesToInt -= (((bytesToInt2 << 4) ^ (bytesToInt2 >>> 5)) + bytesToInt2) ^ this._sum0[i];
        }
        unpackInt(bytesToInt, bArr2, i2);
        unpackInt(bytesToInt2, bArr2, i2 + 4);
        return 8;
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToInt = bytesToInt(bArr, i);
        int bytesToInt2 = bytesToInt(bArr, i + 4);
        for (i = 0; i < 32; i++) {
            bytesToInt += (((bytesToInt2 << 4) ^ (bytesToInt2 >>> 5)) + bytesToInt2) ^ this._sum0[i];
            bytesToInt2 += (((bytesToInt << 4) ^ (bytesToInt >>> 5)) + bytesToInt) ^ this._sum1[i];
        }
        unpackInt(bytesToInt, bArr2, i2);
        unpackInt(bytesToInt2, bArr2, i2 + 4);
        return 8;
    }

    private void setKey(byte[] bArr) {
        if (bArr.length == 16) {
            int i = 0;
            int i2 = 0;
            int i3 = i2;
            while (i2 < 4) {
                this._S[i2] = bytesToInt(bArr, i3);
                i2++;
                i3 += 4;
            }
            int i4 = 0;
            while (i < 32) {
                this._sum0[i] = this._S[i4 & 3] + i4;
                i4 -= 1640531527;
                this._sum1[i] = this._S[(i4 >>> 11) & 3] + i4;
                i++;
            }
            return;
        }
        throw new IllegalArgumentException("Key size must be 128 bits.");
    }

    private void unpackInt(int i, byte[] bArr, int i2) {
        int i3 = i2 + 1;
        bArr[i2] = (byte) (i >>> 24);
        i2 = i3 + 1;
        bArr[i3] = (byte) (i >>> 16);
        i3 = i2 + 1;
        bArr[i2] = (byte) (i >>> 8);
        bArr[i3] = (byte) i;
    }

    public String getAlgorithmName() {
        return "XTEA";
    }

    public int getBlockSize() {
        return 8;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this._forEncryption = z;
            this._initialised = true;
            setKey(((KeyParameter) cipherParameters).getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to TEA init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (!this._initialised) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" not initialised");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (i + 8 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 8 <= bArr2.length) {
            return this._forEncryption ? encryptBlock(bArr, i, bArr2, i2) : decryptBlock(bArr, i, bArr2, i2);
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
