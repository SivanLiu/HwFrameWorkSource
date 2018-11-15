package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class HC256Engine implements StreamCipher {
    private byte[] buf = new byte[4];
    private int cnt = 0;
    private int idx = 0;
    private boolean initialised;
    private byte[] iv;
    private byte[] key;
    private int[] p = new int[1024];
    private int[] q = new int[1024];

    private byte getByte() {
        if (this.idx == 0) {
            int step = step();
            this.buf[0] = (byte) (step & 255);
            step >>= 8;
            this.buf[1] = (byte) (step & 255);
            step >>= 8;
            this.buf[2] = (byte) (step & 255);
            this.buf[3] = (byte) ((step >> 8) & 255);
        }
        byte b = this.buf[this.idx];
        this.idx = 3 & (this.idx + 1);
        return b;
    }

    private void init() {
        int i = 16;
        if (this.key.length != 32 && this.key.length != 16) {
            throw new IllegalArgumentException("The key must be 128/256 bits long");
        } else if (this.iv.length >= 16) {
            Object obj;
            int i2;
            int i3;
            if (this.key.length != 32) {
                obj = new byte[32];
                System.arraycopy(this.key, 0, obj, 0, this.key.length);
                System.arraycopy(this.key, 0, obj, 16, this.key.length);
                this.key = obj;
            }
            if (this.iv.length < 32) {
                obj = new byte[32];
                System.arraycopy(this.iv, 0, obj, 0, this.iv.length);
                System.arraycopy(this.iv, 0, obj, this.iv.length, obj.length - this.iv.length);
                this.iv = obj;
            }
            this.idx = 0;
            this.cnt = 0;
            Object obj2 = new int[2560];
            for (i2 = 0; i2 < 32; i2++) {
                i3 = i2 >> 2;
                obj2[i3] = ((this.key[i2] & 255) << (8 * (i2 & 3))) | obj2[i3];
            }
            for (i2 = 0; i2 < 32; i2++) {
                i3 = (i2 >> 2) + 8;
                obj2[i3] = obj2[i3] | ((this.iv[i2] & 255) << ((i2 & 3) * 8));
            }
            while (i < 2560) {
                int i4 = obj2[i - 2];
                i2 = obj2[i - 15];
                obj2[i] = (((((i4 >>> 10) ^ (rotateRight(i4, 17) ^ rotateRight(i4, 19))) + obj2[i - 7]) + ((i2 >>> 3) ^ (rotateRight(i2, 7) ^ rotateRight(i2, 18)))) + obj2[i - 16]) + i;
                i++;
            }
            System.arraycopy(obj2, 512, this.p, 0, 1024);
            System.arraycopy(obj2, 1536, this.q, 0, 1024);
            for (int i5 = 0; i5 < PKIFailureInfo.certConfirmed; i5++) {
                step();
            }
            this.cnt = 0;
        } else {
            throw new IllegalArgumentException("The IV must be at least 128 bits long");
        }
    }

    private static int rotateRight(int i, int i2) {
        return (i << (-i2)) | (i >>> i2);
    }

    private int step() {
        int i;
        int i2 = this.cnt & 1023;
        int i3;
        int i4;
        int[] iArr;
        if (this.cnt < 1024) {
            i3 = this.p[(i2 - 3) & 1023];
            i4 = this.p[(i2 - 1023) & 1023];
            iArr = this.p;
            iArr[i2] = iArr[i2] + ((this.p[(i2 - 10) & 1023] + (rotateRight(i4, 23) ^ rotateRight(i3, 10))) + this.q[(i3 ^ i4) & 1023]);
            i3 = this.p[(i2 - 12) & 1023];
            i = ((this.q[i3 & 255] + this.q[((i3 >> 8) & 255) + 256]) + this.q[((i3 >> 16) & 255) + 512]) + this.q[((i3 >> 24) & 255) + 768];
            i2 = this.p[i2];
        } else {
            i3 = this.q[(i2 - 3) & 1023];
            i4 = this.q[(i2 - 1023) & 1023];
            iArr = this.q;
            iArr[i2] = iArr[i2] + ((this.q[(i2 - 10) & 1023] + (rotateRight(i4, 23) ^ rotateRight(i3, 10))) + this.p[(i3 ^ i4) & 1023]);
            i3 = this.q[(i2 - 12) & 1023];
            i = ((this.p[i3 & 255] + this.p[((i3 >> 8) & 255) + 256]) + this.p[((i3 >> 16) & 255) + 512]) + this.p[((i3 >> 24) & 255) + 768];
            i2 = this.q[i2];
        }
        i2 ^= i;
        this.cnt = (this.cnt + 1) & 2047;
        return i2;
    }

    public String getAlgorithmName() {
        return "HC-256";
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        CipherParameters parameters;
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            this.iv = parametersWithIV.getIV();
            parameters = parametersWithIV.getParameters();
        } else {
            this.iv = new byte[0];
            parameters = cipherParameters;
        }
        if (parameters instanceof KeyParameter) {
            this.key = ((KeyParameter) parameters).getKey();
            init();
            this.initialised = true;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid parameter passed to HC256 init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
        if (!this.initialised) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" not initialised");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (i + i2 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i3 + i2 <= bArr2.length) {
            for (int i4 = 0; i4 < i2; i4++) {
                bArr2[i3 + i4] = (byte) (bArr[i + i4] ^ getByte());
            }
            return i2;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
        init();
    }

    public byte returnByte(byte b) {
        return (byte) (b ^ getByte());
    }
}
