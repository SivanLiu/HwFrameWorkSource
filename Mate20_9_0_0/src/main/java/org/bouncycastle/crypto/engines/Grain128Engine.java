package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.tls.CipherSuite;

public class Grain128Engine implements StreamCipher {
    private static final int STATE_SIZE = 4;
    private int index = 4;
    private boolean initialised = false;
    private int[] lfsr;
    private int[] nfsr;
    private byte[] out;
    private int output;
    private byte[] workingIV;
    private byte[] workingKey;

    private byte getKeyStream() {
        if (this.index > 3) {
            oneRound();
            this.index = 0;
        }
        byte[] bArr = this.out;
        int i = this.index;
        this.index = i + 1;
        return bArr[i];
    }

    private int getOutput() {
        int i = (this.nfsr[0] >>> 2) | (this.nfsr[1] << 30);
        int i2 = (this.nfsr[0] >>> 12) | (this.nfsr[1] << 20);
        int i3 = (this.nfsr[0] >>> 15) | (this.nfsr[1] << 17);
        int i4 = (this.nfsr[1] >>> 4) | (this.nfsr[2] << 28);
        int i5 = (this.nfsr[1] >>> 13) | (this.nfsr[2] << 19);
        int i6 = (this.nfsr[2] >>> 31) | (this.nfsr[3] << 1);
        int i7 = (this.lfsr[1] << 19) | (this.lfsr[0] >>> 13);
        int i8 = (this.lfsr[2] << 22) | (this.lfsr[1] >>> 10);
        int i9 = (this.lfsr[2] << 4) | (this.lfsr[1] >>> 28);
        int i10 = (this.lfsr[3] << 17) | (this.lfsr[2] >>> 15);
        int i11 = ((this.lfsr[0] >>> 20) | (this.lfsr[1] << 12)) & i7;
        return (((((((((((this.lfsr[3] << 1) | (this.lfsr[2] >>> 31)) & (i2 & i6)) ^ (((i11 ^ (((this.lfsr[0] >>> 8) | (this.lfsr[1] << 24)) & i2)) ^ (i8 & i6)) ^ (i10 & i9))) ^ ((this.lfsr[3] << 3) | (this.lfsr[2] >>> 29))) ^ i) ^ i3) ^ i4) ^ i5) ^ this.nfsr[2]) ^ ((this.nfsr[2] >>> 9) | (this.nfsr[3] << 23))) ^ ((this.nfsr[2] >>> 25) | (this.nfsr[3] << 7));
    }

    private int getOutputLFSR() {
        int i = (this.lfsr[1] >>> 6) | (this.lfsr[2] << 26);
        int i2 = (this.lfsr[2] >>> 6) | (this.lfsr[3] << 26);
        int i3 = (this.lfsr[2] >>> 17) | (this.lfsr[3] << 15);
        return ((((this.lfsr[0] ^ ((this.lfsr[0] >>> 7) | (this.lfsr[1] << 25))) ^ i) ^ i2) ^ i3) ^ this.lfsr[3];
    }

    private int getOutputNFSR() {
        int i = (this.nfsr[2] << 3) | (this.nfsr[1] >>> 29);
        int i2 = (this.nfsr[3] << 31) | (this.nfsr[2] >>> 1);
        int i3 = i;
        int i4 = i2;
        i2 = (this.nfsr[3] << 28) | (this.nfsr[2] >>> 4);
        int i5 = i2;
        i2 = (this.nfsr[3] << 12) | (this.nfsr[2] >>> 20);
        int i6 = i2;
        return (((((((this.nfsr[3] ^ (((this.nfsr[0] ^ ((this.nfsr[0] >>> 26) | (this.nfsr[1] << 6))) ^ ((this.nfsr[1] >>> 24) | (this.nfsr[2] << 8))) ^ ((this.nfsr[3] << 5) | (this.nfsr[2] >>> 27)))) ^ (((this.nfsr[0] >>> 3) | (this.nfsr[1] << 29)) & ((this.nfsr[3] << 29) | (this.nfsr[2] >>> 3)))) ^ (((this.nfsr[0] >>> 11) | (this.nfsr[1] << 21)) & ((this.nfsr[0] >>> 13) | (this.nfsr[1] << 19)))) ^ (((this.nfsr[0] >>> 17) | (this.nfsr[1] << 15)) & ((this.nfsr[0] >>> 18) | (this.nfsr[1] << 14)))) ^ (((this.nfsr[0] >>> 27) | (this.nfsr[1] << 5)) & ((this.nfsr[2] << 5) | (this.nfsr[1] >>> 27)))) ^ (((this.nfsr[1] >>> 8) | (this.nfsr[2] << 24)) & ((this.nfsr[1] >>> 16) | (this.nfsr[2] << 16)))) ^ (i3 & i4)) ^ (i5 & i6);
    }

    private void initGrain() {
        for (int i = 0; i < 8; i++) {
            this.output = getOutput();
            this.nfsr = shift(this.nfsr, (getOutputNFSR() ^ this.lfsr[0]) ^ this.output);
            this.lfsr = shift(this.lfsr, getOutputLFSR() ^ this.output);
        }
        this.initialised = true;
    }

    private void oneRound() {
        this.output = getOutput();
        this.out[0] = (byte) this.output;
        this.out[1] = (byte) (this.output >> 8);
        this.out[2] = (byte) (this.output >> 16);
        this.out[3] = (byte) (this.output >> 24);
        this.nfsr = shift(this.nfsr, getOutputNFSR() ^ this.lfsr[0]);
        this.lfsr = shift(this.lfsr, getOutputLFSR());
    }

    private void setKey(byte[] bArr, byte[] bArr2) {
        bArr2[12] = (byte) -1;
        bArr2[13] = (byte) -1;
        bArr2[14] = (byte) -1;
        bArr2[15] = (byte) -1;
        this.workingKey = bArr;
        this.workingIV = bArr2;
        int i = 0;
        int i2 = 0;
        while (i < this.nfsr.length) {
            int i3 = i2 + 3;
            int i4 = i2 + 2;
            int i5 = i2 + 1;
            this.nfsr[i] = (((this.workingKey[i3] << 24) | ((this.workingKey[i4] << 16) & 16711680)) | ((this.workingKey[i5] << 8) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB)) | (this.workingKey[i2] & 255);
            this.lfsr[i] = (((this.workingIV[i3] << 24) | ((this.workingIV[i4] << 16) & 16711680)) | ((this.workingIV[i5] << 8) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB)) | (this.workingIV[i2] & 255);
            i2 += 4;
            i++;
        }
    }

    private int[] shift(int[] iArr, int i) {
        iArr[0] = iArr[1];
        iArr[1] = iArr[2];
        iArr[2] = iArr[3];
        iArr[3] = i;
        return iArr;
    }

    public String getAlgorithmName() {
        return "Grain-128";
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof ParametersWithIV) {
            ParametersWithIV parametersWithIV = (ParametersWithIV) cipherParameters;
            Object iv = parametersWithIV.getIV();
            if (iv == null || iv.length != 12) {
                throw new IllegalArgumentException("Grain-128  requires exactly 12 bytes of IV");
            } else if (parametersWithIV.getParameters() instanceof KeyParameter) {
                KeyParameter keyParameter = (KeyParameter) parametersWithIV.getParameters();
                this.workingIV = new byte[keyParameter.getKey().length];
                this.workingKey = new byte[keyParameter.getKey().length];
                this.lfsr = new int[4];
                this.nfsr = new int[4];
                this.out = new byte[4];
                System.arraycopy(iv, 0, this.workingIV, 0, iv.length);
                System.arraycopy(keyParameter.getKey(), 0, this.workingKey, 0, keyParameter.getKey().length);
                reset();
                return;
            } else {
                throw new IllegalArgumentException("Grain-128 Init parameters must include a key");
            }
        }
        throw new IllegalArgumentException("Grain-128 Init parameters must include an IV");
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
                bArr2[i3 + i4] = (byte) (bArr[i + i4] ^ getKeyStream());
            }
            return i2;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
        this.index = 4;
        setKey(this.workingKey, this.workingIV);
        initGrain();
    }

    public byte returnByte(byte b) {
        if (this.initialised) {
            return (byte) (b ^ getKeyStream());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getAlgorithmName());
        stringBuilder.append(" not initialised");
        throw new IllegalStateException(stringBuilder.toString());
    }
}
