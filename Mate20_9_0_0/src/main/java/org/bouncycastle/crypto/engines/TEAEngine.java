package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;

public class TEAEngine implements BlockCipher {
    private static final int block_size = 8;
    private static final int d_sum = -957401312;
    private static final int delta = -1640531527;
    private static final int rounds = 32;
    private int _a;
    private int _b;
    private int _c;
    private int _d;
    private boolean _forEncryption;
    private boolean _initialised = false;

    private int bytesToInt(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = i2 + 1;
        i = (bArr[i] << 24) | ((bArr[i2] & 255) << 16);
        return (bArr[i3 + 1] & 255) | (i | ((bArr[i3] & 255) << 8));
    }

    private int decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToInt = bytesToInt(bArr, i);
        int bytesToInt2 = bytesToInt(bArr, i + 4);
        i = d_sum;
        for (int i3 = 0; i3 != 32; i3++) {
            bytesToInt2 -= (((bytesToInt << 4) + this._c) ^ (bytesToInt + i)) ^ ((bytesToInt >>> 5) + this._d);
            bytesToInt -= (((bytesToInt2 << 4) + this._a) ^ (bytesToInt2 + i)) ^ ((bytesToInt2 >>> 5) + this._b);
            i += 1640531527;
        }
        unpackInt(bytesToInt, bArr2, i2);
        unpackInt(bytesToInt2, bArr2, i2 + 4);
        return 8;
    }

    private int encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToInt = bytesToInt(bArr, i);
        int bytesToInt2 = bytesToInt(bArr, i + 4);
        i = 0;
        int i3 = bytesToInt2;
        bytesToInt2 = 0;
        while (i != 32) {
            bytesToInt2 -= 1640531527;
            bytesToInt += (((i3 << 4) + this._a) ^ (i3 + bytesToInt2)) ^ ((i3 >>> 5) + this._b);
            i3 += (((bytesToInt << 4) + this._c) ^ (bytesToInt + bytesToInt2)) ^ ((bytesToInt >>> 5) + this._d);
            i++;
        }
        unpackInt(bytesToInt, bArr2, i2);
        unpackInt(i3, bArr2, i2 + 4);
        return 8;
    }

    private void setKey(byte[] bArr) {
        if (bArr.length == 16) {
            this._a = bytesToInt(bArr, 0);
            this._b = bytesToInt(bArr, 4);
            this._c = bytesToInt(bArr, 8);
            this._d = bytesToInt(bArr, 12);
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
        return "TEA";
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
