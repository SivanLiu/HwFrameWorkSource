package org.bouncycastle.crypto.macs;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithSBox;
import org.bouncycastle.crypto.tls.CipherSuite;

public class GOST28147Mac implements Mac {
    private byte[] S = new byte[]{(byte) 9, (byte) 6, (byte) 3, (byte) 2, (byte) 8, (byte) 11, (byte) 1, (byte) 7, (byte) 10, (byte) 4, (byte) 14, (byte) 15, (byte) 12, (byte) 0, (byte) 13, (byte) 5, (byte) 3, (byte) 7, (byte) 14, (byte) 9, (byte) 8, (byte) 10, (byte) 15, (byte) 0, (byte) 5, (byte) 2, (byte) 6, (byte) 12, (byte) 11, (byte) 4, (byte) 13, (byte) 1, (byte) 14, (byte) 4, (byte) 6, (byte) 2, (byte) 11, (byte) 3, (byte) 13, (byte) 8, (byte) 12, (byte) 15, (byte) 5, (byte) 10, (byte) 0, (byte) 7, (byte) 1, (byte) 9, (byte) 14, (byte) 7, (byte) 10, (byte) 12, (byte) 13, (byte) 1, (byte) 3, (byte) 9, (byte) 0, (byte) 2, (byte) 11, (byte) 4, (byte) 15, (byte) 8, (byte) 5, (byte) 6, (byte) 11, (byte) 5, (byte) 1, (byte) 9, (byte) 8, (byte) 13, (byte) 15, (byte) 0, (byte) 14, (byte) 4, (byte) 2, (byte) 3, (byte) 12, (byte) 7, (byte) 10, (byte) 6, (byte) 3, (byte) 10, (byte) 13, (byte) 12, (byte) 1, (byte) 2, (byte) 0, (byte) 11, (byte) 7, (byte) 5, (byte) 9, (byte) 4, (byte) 8, (byte) 15, (byte) 14, (byte) 6, (byte) 1, (byte) 13, (byte) 2, (byte) 9, (byte) 7, (byte) 10, (byte) 6, (byte) 0, (byte) 8, (byte) 12, (byte) 4, (byte) 5, (byte) 15, (byte) 3, (byte) 11, (byte) 14, (byte) 11, (byte) 10, (byte) 15, (byte) 5, (byte) 0, (byte) 12, (byte) 14, (byte) 8, (byte) 6, (byte) 2, (byte) 3, (byte) 9, (byte) 1, (byte) 7, (byte) 13, (byte) 4};
    private int blockSize = 8;
    private byte[] buf = new byte[this.blockSize];
    private int bufOff = 0;
    private boolean firstStep = true;
    private byte[] mac = new byte[this.blockSize];
    private byte[] macIV = null;
    private int macSize = 4;
    private int[] workingKey = null;

    private byte[] CM5func(byte[] bArr, int i, byte[] bArr2) {
        byte[] bArr3 = new byte[(bArr.length - i)];
        int i2 = 0;
        System.arraycopy(bArr, i, bArr3, 0, bArr2.length);
        while (i2 != bArr2.length) {
            bArr3[i2] = (byte) (bArr3[i2] ^ bArr2[i2]);
            i2++;
        }
        return bArr3;
    }

    private int bytesToint(byte[] bArr, int i) {
        return ((((bArr[i + 3] << 24) & -16777216) + ((bArr[i + 2] << 16) & 16711680)) + ((bArr[i + 1] << 8) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB)) + (bArr[i] & 255);
    }

    private int[] generateWorkingKey(byte[] bArr) {
        if (bArr.length == 32) {
            int[] iArr = new int[8];
            for (int i = 0; i != 8; i++) {
                iArr[i] = bytesToint(bArr, i * 4);
            }
            return iArr;
        }
        throw new IllegalArgumentException("Key length invalid. Key needs to be 32 byte - 256 bit!!!");
    }

    private void gost28147MacFunc(int[] iArr, byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToint = bytesToint(bArr, i);
        int bytesToint2 = bytesToint(bArr, i + 4);
        int i3 = 0;
        while (i3 < 2) {
            int i4 = bytesToint2;
            bytesToint2 = bytesToint;
            bytesToint = 0;
            while (bytesToint < 8) {
                bytesToint++;
                int gost28147_mainStep = i4 ^ gost28147_mainStep(bytesToint2, iArr[bytesToint]);
                i4 = bytesToint2;
                bytesToint2 = gost28147_mainStep;
            }
            i3++;
            bytesToint = bytesToint2;
            bytesToint2 = i4;
        }
        intTobytes(bytesToint, bArr2, i2);
        intTobytes(bytesToint2, bArr2, i2 + 4);
    }

    private int gost28147_mainStep(int i, int i2) {
        i2 += i;
        i = (((((((this.S[((i2 >> 0) & 15) + 0] << 0) + (this.S[((i2 >> 4) & 15) + 16] << 4)) + (this.S[32 + ((i2 >> 8) & 15)] << 8)) + (this.S[48 + ((i2 >> 12) & 15)] << 12)) + (this.S[64 + ((i2 >> 16) & 15)] << 16)) + (this.S[80 + ((i2 >> 20) & 15)] << 20)) + (this.S[96 + ((i2 >> 24) & 15)] << 24)) + (this.S[112 + ((i2 >> 28) & 15)] << 28);
        return (i >>> 21) | (i << 11);
    }

    private void intTobytes(int i, byte[] bArr, int i2) {
        bArr[i2 + 3] = (byte) (i >>> 24);
        bArr[i2 + 2] = (byte) (i >>> 16);
        bArr[i2 + 1] = (byte) (i >>> 8);
        bArr[i2] = (byte) i;
    }

    public int doFinal(byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        while (this.bufOff < this.blockSize) {
            this.buf[this.bufOff] = (byte) 0;
            this.bufOff++;
        }
        byte[] bArr2 = new byte[this.buf.length];
        System.arraycopy(this.buf, 0, bArr2, 0, this.mac.length);
        if (this.firstStep) {
            this.firstStep = false;
        } else {
            bArr2 = CM5func(this.buf, 0, this.mac);
        }
        gost28147MacFunc(this.workingKey, bArr2, 0, this.mac, 0);
        System.arraycopy(this.mac, (this.mac.length / 2) - this.macSize, bArr, i, this.macSize);
        reset();
        return this.macSize;
    }

    public String getAlgorithmName() {
        return "GOST28147Mac";
    }

    public int getMacSize() {
        return this.macSize;
    }

    public void init(CipherParameters cipherParameters) throws IllegalArgumentException {
        reset();
        this.buf = new byte[this.blockSize];
        this.macIV = null;
        if (cipherParameters instanceof ParametersWithSBox) {
            ParametersWithSBox parametersWithSBox = (ParametersWithSBox) cipherParameters;
            System.arraycopy(parametersWithSBox.getSBox(), 0, this.S, 0, parametersWithSBox.getSBox().length);
            if (parametersWithSBox.getParameters() != null) {
                cipherParameters = parametersWithSBox.getParameters();
            }
            return;
        } else if (!(cipherParameters instanceof KeyParameter)) {
            if (cipherParameters instanceof ParametersWithIV) {
                ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
                this.workingKey = generateWorkingKey(((KeyParameter) parametersWithIV.getParameters()).getKey());
                System.arraycopy(parametersWithIV.getIV(), 0, this.mac, 0, this.mac.length);
                this.macIV = parametersWithIV.getIV();
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid parameter passed to GOST28147 init - ");
            stringBuilder.append(cipherParameters.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.workingKey = generateWorkingKey(((KeyParameter) cipherParameters).getKey());
    }

    public void reset() {
        for (int i = 0; i < this.buf.length; i++) {
            this.buf[i] = (byte) 0;
        }
        this.bufOff = 0;
        this.firstStep = true;
    }

    public void update(byte b) throws IllegalStateException {
        byte[] bArr;
        if (this.bufOff == this.buf.length) {
            bArr = new byte[this.buf.length];
            System.arraycopy(this.buf, 0, bArr, 0, this.mac.length);
            byte[] bArr2;
            if (this.firstStep) {
                this.firstStep = false;
                if (this.macIV != null) {
                    bArr = this.buf;
                    bArr2 = this.macIV;
                    bArr = CM5func(bArr, 0, bArr2);
                }
            } else {
                bArr = this.buf;
                bArr2 = this.mac;
                bArr = CM5func(bArr, 0, bArr2);
            }
            gost28147MacFunc(this.workingKey, bArr, 0, this.mac, 0);
            this.bufOff = 0;
        }
        bArr = this.buf;
        int i = this.bufOff;
        this.bufOff = i + 1;
        bArr[i] = b;
    }

    public void update(byte[] bArr, int i, int i2) throws DataLengthException, IllegalStateException {
        if (i2 >= 0) {
            int i3 = this.blockSize - this.bufOff;
            if (i2 > i3) {
                System.arraycopy(bArr, i, this.buf, this.bufOff, i3);
                byte[] bArr2 = new byte[this.buf.length];
                System.arraycopy(this.buf, 0, bArr2, 0, this.mac.length);
                byte[] bArr3;
                if (this.firstStep) {
                    this.firstStep = false;
                    if (this.macIV != null) {
                        bArr2 = this.buf;
                        bArr3 = this.macIV;
                        bArr2 = CM5func(bArr2, 0, bArr3);
                    }
                } else {
                    bArr2 = this.buf;
                    bArr3 = this.mac;
                    bArr2 = CM5func(bArr2, 0, bArr3);
                }
                gost28147MacFunc(this.workingKey, bArr2, 0, this.mac, 0);
                this.bufOff = 0;
                i2 -= i3;
                while (true) {
                    i += i3;
                    if (i2 <= this.blockSize) {
                        break;
                    }
                    gost28147MacFunc(this.workingKey, CM5func(bArr, i, this.mac), 0, this.mac, 0);
                    i2 -= this.blockSize;
                    i3 = this.blockSize;
                }
            }
            System.arraycopy(bArr, i, this.buf, this.bufOff, i2);
            this.bufOff += i2;
            return;
        }
        throw new IllegalArgumentException("Can't have a negative input length!");
    }
}
