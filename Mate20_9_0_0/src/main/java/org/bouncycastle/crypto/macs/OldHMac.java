package org.bouncycastle.crypto.macs;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.params.KeyParameter;

public class OldHMac implements Mac {
    private static final int BLOCK_LENGTH = 64;
    private static final byte IPAD = (byte) 54;
    private static final byte OPAD = (byte) 92;
    private Digest digest;
    private int digestSize;
    private byte[] inputPad = new byte[64];
    private byte[] outputPad = new byte[64];

    public OldHMac(Digest digest) {
        this.digest = digest;
        this.digestSize = digest.getDigestSize();
    }

    public int doFinal(byte[] bArr, int i) {
        byte[] bArr2 = new byte[this.digestSize];
        this.digest.doFinal(bArr2, 0);
        this.digest.update(this.outputPad, 0, this.outputPad.length);
        this.digest.update(bArr2, 0, bArr2.length);
        int doFinal = this.digest.doFinal(bArr, i);
        reset();
        return doFinal;
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.digest.getAlgorithmName());
        stringBuilder.append("/HMAC");
        return stringBuilder.toString();
    }

    public int getMacSize() {
        return this.digestSize;
    }

    public Digest getUnderlyingDigest() {
        return this.digest;
    }

    public void init(CipherParameters cipherParameters) {
        int i;
        byte[] bArr;
        this.digest.reset();
        byte[] key = ((KeyParameter) cipherParameters).getKey();
        if (key.length > 64) {
            this.digest.update(key, 0, key.length);
            this.digest.doFinal(this.inputPad, 0);
            for (i = this.digestSize; i < this.inputPad.length; i++) {
                this.inputPad[i] = (byte) 0;
            }
        } else {
            System.arraycopy(key, 0, this.inputPad, 0, key.length);
            for (i = key.length; i < this.inputPad.length; i++) {
                this.inputPad[i] = (byte) 0;
            }
        }
        this.outputPad = new byte[this.inputPad.length];
        System.arraycopy(this.inputPad, 0, this.outputPad, 0, this.inputPad.length);
        for (i = 0; i < this.inputPad.length; i++) {
            bArr = this.inputPad;
            bArr[i] = (byte) (bArr[i] ^ 54);
        }
        for (i = 0; i < this.outputPad.length; i++) {
            bArr = this.outputPad;
            bArr[i] = (byte) (bArr[i] ^ 92);
        }
        this.digest.update(this.inputPad, 0, this.inputPad.length);
    }

    public void reset() {
        this.digest.reset();
        this.digest.update(this.inputPad, 0, this.inputPad.length);
    }

    public void update(byte b) {
        this.digest.update(b);
    }

    public void update(byte[] bArr, int i, int i2) {
        this.digest.update(bArr, i, i2);
    }
}
