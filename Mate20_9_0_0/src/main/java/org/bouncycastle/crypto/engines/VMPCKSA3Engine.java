package org.bouncycastle.crypto.engines;

public class VMPCKSA3Engine extends VMPCEngine {
    public String getAlgorithmName() {
        return "VMPC-KSA3";
    }

    protected void initKey(byte[] bArr, byte[] bArr2) {
        int i;
        int i2;
        this.s = (byte) 0;
        this.P = new byte[256];
        for (int i3 = 0; i3 < 256; i3++) {
            this.P[i3] = (byte) i3;
        }
        for (i = 0; i < 768; i++) {
            i2 = i & 255;
            this.s = this.P[((this.s + this.P[i2]) + bArr[i % bArr.length]) & 255];
            byte b = this.P[i2];
            this.P[i2] = this.P[this.s & 255];
            this.P[this.s & 255] = b;
        }
        for (i = 0; i < 768; i++) {
            int i4 = i & 255;
            this.s = this.P[((this.s + this.P[i4]) + bArr2[i % bArr2.length]) & 255];
            byte b2 = this.P[i4];
            this.P[i4] = this.P[this.s & 255];
            this.P[this.s & 255] = b2;
        }
        for (int i5 = 0; i5 < 768; i5++) {
            i2 = i5 & 255;
            this.s = this.P[((this.s + this.P[i2]) + bArr[i5 % bArr.length]) & 255];
            byte b3 = this.P[i2];
            this.P[i2] = this.P[this.s & 255];
            this.P[this.s & 255] = b3;
        }
        this.n = (byte) 0;
    }
}
