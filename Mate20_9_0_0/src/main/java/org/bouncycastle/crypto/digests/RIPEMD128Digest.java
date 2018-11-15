package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;

public class RIPEMD128Digest extends GeneralDigest {
    private static final int DIGEST_LENGTH = 16;
    private int H0;
    private int H1;
    private int H2;
    private int H3;
    private int[] X;
    private int xOff;

    public RIPEMD128Digest() {
        this.X = new int[16];
        reset();
    }

    public RIPEMD128Digest(RIPEMD128Digest rIPEMD128Digest) {
        super((GeneralDigest) rIPEMD128Digest);
        this.X = new int[16];
        copyIn(rIPEMD128Digest);
    }

    private int F1(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL((i + f1(i2, i3, i4)) + i5, i6);
    }

    private int F2(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL(((i + f2(i2, i3, i4)) + i5) + 1518500249, i6);
    }

    private int F3(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL(((i + f3(i2, i3, i4)) + i5) + 1859775393, i6);
    }

    private int F4(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL(((i + f4(i2, i3, i4)) + i5) - 1894007588, i6);
    }

    private int FF1(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL((i + f1(i2, i3, i4)) + i5, i6);
    }

    private int FF2(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL(((i + f2(i2, i3, i4)) + i5) + 1836072691, i6);
    }

    private int FF3(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL(((i + f3(i2, i3, i4)) + i5) + 1548603684, i6);
    }

    private int FF4(int i, int i2, int i3, int i4, int i5, int i6) {
        return RL(((i + f4(i2, i3, i4)) + i5) + 1352829926, i6);
    }

    private int RL(int i, int i2) {
        return (i >>> (32 - i2)) | (i << i2);
    }

    private void copyIn(RIPEMD128Digest rIPEMD128Digest) {
        super.copyIn(rIPEMD128Digest);
        this.H0 = rIPEMD128Digest.H0;
        this.H1 = rIPEMD128Digest.H1;
        this.H2 = rIPEMD128Digest.H2;
        this.H3 = rIPEMD128Digest.H3;
        System.arraycopy(rIPEMD128Digest.X, 0, this.X, 0, rIPEMD128Digest.X.length);
        this.xOff = rIPEMD128Digest.xOff;
    }

    private int f1(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    private int f2(int i, int i2, int i3) {
        return ((~i) & i3) | (i2 & i);
    }

    private int f3(int i, int i2, int i3) {
        return (i | (~i2)) ^ i3;
    }

    private int f4(int i, int i2, int i3) {
        return (i & i3) | (i2 & (~i3));
    }

    private void unpackWord(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        bArr[i2 + 1] = (byte) (i >>> 8);
        bArr[i2 + 2] = (byte) (i >>> 16);
        bArr[i2 + 3] = (byte) (i >>> 24);
    }

    public Memoable copy() {
        return new RIPEMD128Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        finish();
        unpackWord(this.H0, bArr, i);
        unpackWord(this.H1, bArr, i + 4);
        unpackWord(this.H2, bArr, i + 8);
        unpackWord(this.H3, bArr, i + 12);
        reset();
        return 16;
    }

    public String getAlgorithmName() {
        return "RIPEMD128";
    }

    public int getDigestSize() {
        return 16;
    }

    protected void processBlock() {
        int i = this.H0;
        int i2 = this.H1;
        int i3 = this.H2;
        int i4 = this.H3;
        int F1 = F1(i, i2, i3, i4, this.X[0], 11);
        int F12 = F1(i4, F1, i2, i3, this.X[1], 14);
        int F13 = F1(i3, F12, F1, i2, this.X[2], 15);
        int F14 = F1(i2, F13, F12, F1, this.X[3], 12);
        F1 = F1(F1, F14, F13, F12, this.X[4], 5);
        F12 = F1(F12, F1, F14, F13, this.X[5], 8);
        F13 = F1(F13, F12, F1, F14, this.X[6], 7);
        F14 = F1(F14, F13, F12, F1, this.X[7], 9);
        F1 = F1(F1, F14, F13, F12, this.X[8], 11);
        F12 = F1(F12, F1, F14, F13, this.X[9], 13);
        F13 = F1(F13, F12, F1, F14, this.X[10], 14);
        F14 = F1(F14, F13, F12, F1, this.X[11], 15);
        F1 = F1(F1, F14, F13, F12, this.X[12], 6);
        F12 = F1(F12, F1, F14, F13, this.X[13], 7);
        F13 = F1(F13, F12, F1, F14, this.X[14], 9);
        F14 = F1(F14, F13, F12, F1, this.X[15], 8);
        F1 = F2(F1, F14, F13, F12, this.X[7], 7);
        F12 = F2(F12, F1, F14, F13, this.X[4], 6);
        F13 = F2(F13, F12, F1, F14, this.X[13], 8);
        F14 = F2(F14, F13, F12, F1, this.X[1], 13);
        F1 = F2(F1, F14, F13, F12, this.X[10], 11);
        F12 = F2(F12, F1, F14, F13, this.X[6], 9);
        F13 = F2(F13, F12, F1, F14, this.X[15], 7);
        F14 = F2(F14, F13, F12, F1, this.X[3], 15);
        F1 = F2(F1, F14, F13, F12, this.X[12], 7);
        F12 = F2(F12, F1, F14, F13, this.X[0], 12);
        F13 = F2(F13, F12, F1, F14, this.X[9], 15);
        F14 = F2(F14, F13, F12, F1, this.X[5], 9);
        F1 = F2(F1, F14, F13, F12, this.X[2], 11);
        F12 = F2(F12, F1, F14, F13, this.X[14], 7);
        F13 = F2(F13, F12, F1, F14, this.X[11], 13);
        F14 = F2(F14, F13, F12, F1, this.X[8], 12);
        F1 = F3(F1, F14, F13, F12, this.X[3], 11);
        F12 = F3(F12, F1, F14, F13, this.X[10], 13);
        F13 = F3(F13, F12, F1, F14, this.X[14], 6);
        F14 = F3(F14, F13, F12, F1, this.X[4], 7);
        F1 = F3(F1, F14, F13, F12, this.X[9], 14);
        F12 = F3(F12, F1, F14, F13, this.X[15], 9);
        F13 = F3(F13, F12, F1, F14, this.X[8], 13);
        F14 = F3(F14, F13, F12, F1, this.X[1], 15);
        F1 = F3(F1, F14, F13, F12, this.X[2], 14);
        F12 = F3(F12, F1, F14, F13, this.X[7], 8);
        F13 = F3(F13, F12, F1, F14, this.X[0], 13);
        F14 = F3(F14, F13, F12, F1, this.X[6], 6);
        F1 = F3(F1, F14, F13, F12, this.X[13], 5);
        F12 = F3(F12, F1, F14, F13, this.X[11], 12);
        F13 = F3(F13, F12, F1, F14, this.X[5], 7);
        F14 = F3(F14, F13, F12, F1, this.X[12], 5);
        F1 = F4(F1, F14, F13, F12, this.X[1], 11);
        F12 = F4(F12, F1, F14, F13, this.X[9], 12);
        F13 = F4(F13, F12, F1, F14, this.X[11], 14);
        F14 = F4(F14, F13, F12, F1, this.X[10], 15);
        F1 = F4(F1, F14, F13, F12, this.X[0], 14);
        F12 = F4(F12, F1, F14, F13, this.X[8], 15);
        F13 = F4(F13, F12, F1, F14, this.X[12], 9);
        F14 = F4(F14, F13, F12, F1, this.X[4], 8);
        F1 = F4(F1, F14, F13, F12, this.X[13], 9);
        F12 = F4(F12, F1, F14, F13, this.X[3], 14);
        F13 = F4(F13, F12, F1, F14, this.X[7], 5);
        F14 = F4(F14, F13, F12, F1, this.X[15], 6);
        F1 = F4(F1, F14, F13, F12, this.X[14], 8);
        F12 = F4(F12, F1, F14, F13, this.X[5], 6);
        F13 = F4(F13, F12, F1, F14, this.X[6], 5);
        F14 = F4(F14, F13, F12, F1, this.X[2], 12);
        i = FF4(i, i2, i3, i4, this.X[5], 8);
        i4 = FF4(i4, i, i2, i3, this.X[14], 9);
        i3 = FF4(i3, i4, i, i2, this.X[7], 9);
        i2 = FF4(i2, i3, i4, i, this.X[0], 11);
        i = FF4(i, i2, i3, i4, this.X[9], 13);
        i4 = FF4(i4, i, i2, i3, this.X[2], 15);
        i3 = FF4(i3, i4, i, i2, this.X[11], 15);
        i2 = FF4(i2, i3, i4, i, this.X[4], 5);
        i = FF4(i, i2, i3, i4, this.X[13], 7);
        i4 = FF4(i4, i, i2, i3, this.X[6], 7);
        i3 = FF4(i3, i4, i, i2, this.X[15], 8);
        i2 = FF4(i2, i3, i4, i, this.X[8], 11);
        i = FF4(i, i2, i3, i4, this.X[1], 14);
        i4 = FF4(i4, i, i2, i3, this.X[10], 14);
        i3 = FF4(i3, i4, i, i2, this.X[3], 12);
        i2 = FF4(i2, i3, i4, i, this.X[12], 6);
        i = FF3(i, i2, i3, i4, this.X[6], 9);
        i4 = FF3(i4, i, i2, i3, this.X[11], 13);
        i3 = FF3(i3, i4, i, i2, this.X[3], 15);
        i2 = FF3(i2, i3, i4, i, this.X[7], 7);
        i = FF3(i, i2, i3, i4, this.X[0], 12);
        i4 = FF3(i4, i, i2, i3, this.X[13], 8);
        i3 = FF3(i3, i4, i, i2, this.X[5], 9);
        i2 = FF3(i2, i3, i4, i, this.X[10], 11);
        i = FF3(i, i2, i3, i4, this.X[14], 7);
        i4 = FF3(i4, i, i2, i3, this.X[15], 7);
        i3 = FF3(i3, i4, i, i2, this.X[8], 12);
        i2 = FF3(i2, i3, i4, i, this.X[12], 7);
        i = FF3(i, i2, i3, i4, this.X[4], 6);
        i4 = FF3(i4, i, i2, i3, this.X[9], 15);
        i3 = FF3(i3, i4, i, i2, this.X[1], 13);
        i2 = FF3(i2, i3, i4, i, this.X[2], 11);
        i = FF2(i, i2, i3, i4, this.X[15], 9);
        i4 = FF2(i4, i, i2, i3, this.X[5], 7);
        i3 = FF2(i3, i4, i, i2, this.X[1], 15);
        i2 = FF2(i2, i3, i4, i, this.X[3], 11);
        i = FF2(i, i2, i3, i4, this.X[7], 8);
        i4 = FF2(i4, i, i2, i3, this.X[14], 6);
        i3 = FF2(i3, i4, i, i2, this.X[6], 6);
        i2 = FF2(i2, i3, i4, i, this.X[9], 14);
        i = FF2(i, i2, i3, i4, this.X[11], 12);
        i4 = FF2(i4, i, i2, i3, this.X[8], 13);
        i3 = FF2(i3, i4, i, i2, this.X[12], 5);
        i2 = FF2(i2, i3, i4, i, this.X[2], 14);
        i = FF2(i, i2, i3, i4, this.X[10], 13);
        i4 = FF2(i4, i, i2, i3, this.X[0], 13);
        i3 = FF2(i3, i4, i, i2, this.X[4], 7);
        i2 = FF2(i2, i3, i4, i, this.X[13], 5);
        i = FF1(i, i2, i3, i4, this.X[8], 15);
        i4 = FF1(i4, i, i2, i3, this.X[6], 5);
        i3 = FF1(i3, i4, i, i2, this.X[4], 8);
        i2 = FF1(i2, i3, i4, i, this.X[1], 11);
        i = FF1(i, i2, i3, i4, this.X[3], 14);
        i4 = FF1(i4, i, i2, i3, this.X[11], 14);
        i3 = FF1(i3, i4, i, i2, this.X[15], 6);
        i2 = FF1(i2, i3, i4, i, this.X[0], 14);
        i = FF1(i, i2, i3, i4, this.X[5], 6);
        i4 = FF1(i4, i, i2, i3, this.X[12], 9);
        i3 = FF1(i3, i4, i, i2, this.X[2], 12);
        i2 = FF1(i2, i3, i4, i, this.X[13], 9);
        i = FF1(i, i2, i3, i4, this.X[9], 12);
        i4 = FF1(i4, i, i2, i3, this.X[7], 5);
        i3 = FF1(i3, i4, i, i2, this.X[10], 15);
        int FF1 = FF1(i2, i3, i4, i, this.X[14], 8);
        i4 += F13 + this.H1;
        this.H1 = (this.H2 + F12) + i;
        this.H2 = (this.H3 + F1) + FF1;
        this.H3 = (this.H0 + F14) + i3;
        this.H0 = i4;
        this.xOff = 0;
        for (FF1 = 0; FF1 != this.X.length; FF1++) {
            this.X[FF1] = 0;
        }
    }

    protected void processLength(long j) {
        if (this.xOff > 14) {
            processBlock();
        }
        this.X[14] = (int) (-1 & j);
        this.X[15] = (int) (j >>> 32);
    }

    protected void processWord(byte[] bArr, int i) {
        int[] iArr = this.X;
        int i2 = this.xOff;
        this.xOff = i2 + 1;
        iArr[i2] = ((bArr[i + 3] & 255) << 24) | (((bArr[i] & 255) | ((bArr[i + 1] & 255) << 8)) | ((bArr[i + 2] & 255) << 16));
        if (this.xOff == 16) {
            processBlock();
        }
    }

    public void reset() {
        super.reset();
        this.H0 = 1732584193;
        this.H1 = -271733879;
        this.H2 = -1732584194;
        this.H3 = 271733878;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    public void reset(Memoable memoable) {
        copyIn((RIPEMD128Digest) memoable);
    }
}
