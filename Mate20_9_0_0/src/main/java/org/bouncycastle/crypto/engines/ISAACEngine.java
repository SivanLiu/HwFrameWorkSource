package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Pack;

public class ISAACEngine implements StreamCipher {
    private int a = 0;
    private int b = 0;
    private int c = 0;
    private int[] engineState = null;
    private int index = 0;
    private boolean initialised = false;
    private byte[] keyStream = new byte[1024];
    private int[] results = null;
    private final int sizeL = 8;
    private final int stateArraySize = 256;
    private byte[] workingKey = null;

    private void isaac() {
        int i = this.b;
        int i2 = this.c + 1;
        this.c = i2;
        this.b = i + i2;
        for (i = 0; i < 256; i++) {
            int i3;
            int i4;
            i2 = this.engineState[i];
            switch (i & 3) {
                case 0:
                    i3 = this.a;
                    i4 = this.a << 13;
                    break;
                case 1:
                    i3 = this.a;
                    i4 = this.a >>> 6;
                    break;
                case 2:
                    i3 = this.a;
                    i4 = this.a << 2;
                    break;
                case 3:
                    i3 = this.a;
                    i4 = this.a >>> 16;
                    break;
                default:
                    break;
            }
            this.a = i3 ^ i4;
            this.a += this.engineState[(i + 128) & 255];
            i4 = (this.engineState[(i2 >>> 2) & 255] + this.a) + this.b;
            this.engineState[i] = i4;
            int[] iArr = this.results;
            i4 = this.engineState[(i4 >>> 10) & 255] + i2;
            this.b = i4;
            iArr[i] = i4;
        }
    }

    private void mix(int[] iArr) {
        iArr[0] = iArr[0] ^ (iArr[1] << 11);
        iArr[3] = iArr[3] + iArr[0];
        iArr[1] = iArr[1] + iArr[2];
        iArr[1] = iArr[1] ^ (iArr[2] >>> 2);
        iArr[4] = iArr[4] + iArr[1];
        iArr[2] = iArr[2] + iArr[3];
        iArr[2] = iArr[2] ^ (iArr[3] << 8);
        iArr[5] = iArr[5] + iArr[2];
        iArr[3] = iArr[3] + iArr[4];
        iArr[3] = iArr[3] ^ (iArr[4] >>> 16);
        iArr[6] = iArr[6] + iArr[3];
        iArr[4] = iArr[4] + iArr[5];
        iArr[4] = iArr[4] ^ (iArr[5] << 10);
        iArr[7] = iArr[7] + iArr[4];
        iArr[5] = iArr[5] + iArr[6];
        iArr[5] = (iArr[6] >>> 4) ^ iArr[5];
        iArr[0] = iArr[0] + iArr[5];
        iArr[6] = iArr[6] + iArr[7];
        iArr[6] = iArr[6] ^ (iArr[7] << 8);
        iArr[1] = iArr[1] + iArr[6];
        iArr[7] = iArr[7] + iArr[0];
        iArr[7] = iArr[7] ^ (iArr[0] >>> 9);
        iArr[2] = iArr[2] + iArr[7];
        iArr[0] = iArr[0] + iArr[1];
    }

    private void setKey(byte[] bArr) {
        int i;
        this.workingKey = bArr;
        if (this.engineState == null) {
            this.engineState = new int[256];
        }
        if (this.results == null) {
            this.results = new int[256];
        }
        for (int i2 = 0; i2 < 256; i2++) {
            int[] iArr = this.engineState;
            this.results[i2] = 0;
            iArr[i2] = 0;
        }
        this.c = 0;
        this.b = 0;
        this.a = 0;
        this.index = 0;
        Object obj = new byte[(bArr.length + (bArr.length & 3))];
        System.arraycopy(bArr, 0, obj, 0, bArr.length);
        for (int i3 = 0; i3 < obj.length; i3 += 4) {
            this.results[i3 >>> 2] = Pack.littleEndianToInt(obj, i3);
        }
        int[] iArr2 = new int[8];
        for (i = 0; i < 8; i++) {
            iArr2[i] = -1640531527;
        }
        for (i = 0; i < 4; i++) {
            mix(iArr2);
        }
        i = 0;
        while (i < 2) {
            for (int i4 = 0; i4 < 256; i4 += 8) {
                int i5;
                for (i5 = 0; i5 < 8; i5++) {
                    iArr2[i5] = iArr2[i5] + (i < 1 ? this.results[i4 + i5] : this.engineState[i4 + i5]);
                }
                mix(iArr2);
                for (i5 = 0; i5 < 8; i5++) {
                    this.engineState[i4 + i5] = iArr2[i5];
                }
            }
            i++;
        }
        isaac();
        this.initialised = true;
    }

    public String getAlgorithmName() {
        return "ISAAC";
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            setKey(((KeyParameter) cipherParameters).getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to ISAAC init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        if (!this.initialised) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" not initialised");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (i + i2 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i3 + i2 <= bArr2.length) {
            for (int i4 = 0; i4 < i2; i4++) {
                if (this.index == 0) {
                    isaac();
                    this.keyStream = Pack.intToBigEndian(this.results);
                }
                bArr2[i4 + i3] = (byte) (this.keyStream[this.index] ^ bArr[i4 + i]);
                this.index = (this.index + 1) & 1023;
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
        if (this.index == 0) {
            isaac();
            this.keyStream = Pack.intToBigEndian(this.results);
        }
        b = (byte) (b ^ this.keyStream[this.index]);
        this.index = (this.index + 1) & 1023;
        return b;
    }
}
