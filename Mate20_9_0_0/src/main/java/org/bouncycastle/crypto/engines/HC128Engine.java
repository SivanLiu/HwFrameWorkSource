package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class HC128Engine implements StreamCipher {
    private byte[] buf = new byte[4];
    private int cnt = 0;
    private int idx = 0;
    private boolean initialised;
    private byte[] iv;
    private byte[] key;
    private int[] p = new int[512];
    private int[] q = new int[512];

    private static int dim(int i, int i2) {
        return mod512(i - i2);
    }

    private static int f1(int i) {
        return (i >>> 3) ^ (rotateRight(i, 7) ^ rotateRight(i, 18));
    }

    private static int f2(int i) {
        return (i >>> 10) ^ (rotateRight(i, 17) ^ rotateRight(i, 19));
    }

    private int g1(int i, int i2, int i3) {
        return (rotateRight(i, 10) ^ rotateRight(i3, 23)) + rotateRight(i2, 8);
    }

    private int g2(int i, int i2, int i3) {
        return (rotateLeft(i, 10) ^ rotateLeft(i3, 23)) + rotateLeft(i2, 8);
    }

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

    private int h1(int i) {
        return this.q[i & 255] + this.q[((i >> 16) & 255) + 256];
    }

    private int h2(int i) {
        return this.p[i & 255] + this.p[((i >> 16) & 255) + 256];
    }

    private void init() {
        int i = 16;
        if (this.key.length == 16) {
            int i2;
            this.idx = 0;
            this.cnt = 0;
            Object obj = new int[1280];
            for (int i3 = 0; i3 < 16; i3++) {
                i2 = i3 >> 2;
                obj[i2] = ((this.key[i3] & 255) << (8 * (i3 & 3))) | obj[i2];
            }
            System.arraycopy(obj, 0, obj, 4, 4);
            i2 = 0;
            while (i2 < this.iv.length && i2 < 16) {
                int i4 = (i2 >> 2) + 8;
                obj[i4] = obj[i4] | ((this.iv[i2] & 255) << ((i2 & 3) * 8));
                i2++;
            }
            System.arraycopy(obj, 8, obj, 12, 4);
            while (i < 1280) {
                obj[i] = (((f2(obj[i - 2]) + obj[i - 7]) + f1(obj[i - 15])) + obj[i - 16]) + i;
                i++;
            }
            System.arraycopy(obj, 256, this.p, 0, 512);
            System.arraycopy(obj, 768, this.q, 0, 512);
            for (i = 0; i < 512; i++) {
                this.p[i] = step();
            }
            for (i = 0; i < 512; i++) {
                this.q[i] = step();
            }
            this.cnt = 0;
            return;
        }
        throw new IllegalArgumentException("The key must be 128 bits long");
    }

    private static int mod1024(int i) {
        return i & 1023;
    }

    private static int mod512(int i) {
        return i & 511;
    }

    private static int rotateLeft(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    private static int rotateRight(int i, int i2) {
        return (i << (-i2)) | (i >>> i2);
    }

    private int step() {
        int h1;
        int mod512 = mod512(this.cnt);
        int[] iArr;
        if (this.cnt < 512) {
            iArr = this.p;
            iArr[mod512] = iArr[mod512] + g1(this.p[dim(mod512, 3)], this.p[dim(mod512, 10)], this.p[dim(mod512, 511)]);
            h1 = h1(this.p[dim(mod512, 12)]);
            mod512 = this.p[mod512];
        } else {
            iArr = this.q;
            iArr[mod512] = iArr[mod512] + g2(this.q[dim(mod512, 3)], this.q[dim(mod512, 10)], this.q[dim(mod512, 511)]);
            h1 = h2(this.q[dim(mod512, 12)]);
            mod512 = this.q[mod512];
        }
        mod512 ^= h1;
        this.cnt = mod1024(this.cnt + 1);
        return mod512;
    }

    public String getAlgorithmName() {
        return "HC-128";
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
        stringBuilder.append("Invalid parameter passed to HC128 init - ");
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
