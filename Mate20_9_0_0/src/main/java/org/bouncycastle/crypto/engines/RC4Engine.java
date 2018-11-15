package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4Engine implements StreamCipher {
    private static final int STATE_LENGTH = 256;
    private byte[] engineState = null;
    private byte[] workingKey = null;
    private int x = 0;
    private int y = 0;

    private void setKey(byte[] bArr) {
        int i;
        this.workingKey = bArr;
        int i2 = 0;
        this.x = 0;
        this.y = 0;
        if (this.engineState == null) {
            this.engineState = new byte[256];
        }
        for (i = 0; i < 256; i++) {
            this.engineState[i] = (byte) i;
        }
        i = 0;
        int i3 = i;
        while (i2 < 256) {
            i3 = (((bArr[i] & 255) + this.engineState[i2]) + i3) & 255;
            byte b = this.engineState[i2];
            this.engineState[i2] = this.engineState[i3];
            this.engineState[i3] = b;
            i = (i + 1) % bArr.length;
            i2++;
        }
    }

    public String getAlgorithmName() {
        return "RC4";
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.workingKey = ((KeyParameter) cipherParameters).getKey();
            setKey(this.workingKey);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to RC4 init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        if (i + i2 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i3 + i2 <= bArr2.length) {
            for (int i4 = 0; i4 < i2; i4++) {
                this.x = (this.x + 1) & 255;
                this.y = (this.engineState[this.x] + this.y) & 255;
                byte b = this.engineState[this.x];
                this.engineState[this.x] = this.engineState[this.y];
                this.engineState[this.y] = b;
                bArr2[i4 + i3] = (byte) (bArr[i4 + i] ^ this.engineState[(this.engineState[this.x] + this.engineState[this.y]) & 255]);
            }
            return i2;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
        setKey(this.workingKey);
    }

    public byte returnByte(byte b) {
        this.x = (this.x + 1) & 255;
        this.y = (this.engineState[this.x] + this.y) & 255;
        byte b2 = this.engineState[this.x];
        this.engineState[this.x] = this.engineState[this.y];
        this.engineState[this.y] = b2;
        return (byte) (b ^ this.engineState[(this.engineState[this.x] + this.engineState[this.y]) & 255]);
    }
}
