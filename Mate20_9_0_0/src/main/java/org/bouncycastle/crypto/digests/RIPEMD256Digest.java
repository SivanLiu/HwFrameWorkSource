package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;

public class RIPEMD256Digest extends GeneralDigest {
    private static final int DIGEST_LENGTH = 32;
    private int H0;
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int H5;
    private int H6;
    private int H7;
    private int[] X;
    private int xOff;

    public RIPEMD256Digest() {
        this.X = new int[16];
        reset();
    }

    public RIPEMD256Digest(RIPEMD256Digest rIPEMD256Digest) {
        super((GeneralDigest) rIPEMD256Digest);
        this.X = new int[16];
        copyIn(rIPEMD256Digest);
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

    private void copyIn(RIPEMD256Digest rIPEMD256Digest) {
        super.copyIn(rIPEMD256Digest);
        this.H0 = rIPEMD256Digest.H0;
        this.H1 = rIPEMD256Digest.H1;
        this.H2 = rIPEMD256Digest.H2;
        this.H3 = rIPEMD256Digest.H3;
        this.H4 = rIPEMD256Digest.H4;
        this.H5 = rIPEMD256Digest.H5;
        this.H6 = rIPEMD256Digest.H6;
        this.H7 = rIPEMD256Digest.H7;
        System.arraycopy(rIPEMD256Digest.X, 0, this.X, 0, rIPEMD256Digest.X.length);
        this.xOff = rIPEMD256Digest.xOff;
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
        return new RIPEMD256Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        finish();
        unpackWord(this.H0, bArr, i);
        unpackWord(this.H1, bArr, i + 4);
        unpackWord(this.H2, bArr, i + 8);
        unpackWord(this.H3, bArr, i + 12);
        unpackWord(this.H4, bArr, i + 16);
        unpackWord(this.H5, bArr, i + 20);
        unpackWord(this.H6, bArr, i + 24);
        unpackWord(this.H7, bArr, i + 28);
        reset();
        return 32;
    }

    public String getAlgorithmName() {
        return "RIPEMD256";
    }

    public int getDigestSize() {
        return 32;
    }

    protected void processBlock() {
        int i = this.H0;
        int i2 = this.H1;
        int i3 = this.H2;
        int i4 = this.H3;
        int i5 = this.H4;
        int i6 = this.H5;
        int i7 = this.H6;
        int i8 = this.H7;
        int F1 = F1(i, i2, i3, i4, this.X[0], 11);
        i4 = F1(i4, F1, i2, i3, this.X[1], 14);
        i3 = F1(i3, i4, F1, i2, this.X[2], 15);
        i2 = F1(i2, i3, i4, F1, this.X[3], 12);
        F1 = F1(F1, i2, i3, i4, this.X[4], 5);
        i4 = F1(i4, F1, i2, i3, this.X[5], 8);
        i3 = F1(i3, i4, F1, i2, this.X[6], 7);
        i2 = F1(i2, i3, i4, F1, this.X[7], 9);
        F1 = F1(F1, i2, i3, i4, this.X[8], 11);
        i4 = F1(i4, F1, i2, i3, this.X[9], 13);
        i3 = F1(i3, i4, F1, i2, this.X[10], 14);
        i2 = F1(i2, i3, i4, F1, this.X[11], 15);
        F1 = F1(F1, i2, i3, i4, this.X[12], 6);
        i4 = F1(i4, F1, i2, i3, this.X[13], 7);
        i3 = F1(i3, i4, F1, i2, this.X[14], 9);
        i2 = F1(i2, i3, i4, F1, this.X[15], 8);
        i5 = FF4(i5, i6, i7, i8, this.X[5], 8);
        i8 = FF4(i8, i5, i6, i7, this.X[14], 9);
        i7 = FF4(i7, i8, i5, i6, this.X[7], 9);
        i6 = FF4(i6, i7, i8, i5, this.X[0], 11);
        i5 = FF4(i5, i6, i7, i8, this.X[9], 13);
        i8 = FF4(i8, i5, i6, i7, this.X[2], 15);
        i7 = FF4(i7, i8, i5, i6, this.X[11], 15);
        i6 = FF4(i6, i7, i8, i5, this.X[4], 5);
        i5 = FF4(i5, i6, i7, i8, this.X[13], 7);
        i8 = FF4(i8, i5, i6, i7, this.X[6], 7);
        i7 = FF4(i7, i8, i5, i6, this.X[15], 8);
        i6 = FF4(i6, i7, i8, i5, this.X[8], 11);
        i5 = FF4(i5, i6, i7, i8, this.X[1], 14);
        i8 = FF4(i8, i5, i6, i7, this.X[10], 14);
        i7 = FF4(i7, i8, i5, i6, this.X[3], 12);
        i6 = FF4(i6, i7, i8, i5, this.X[12], 6);
        i5 = F2(i5, i2, i3, i4, this.X[7], 7);
        i4 = F2(i4, i5, i2, i3, this.X[4], 6);
        i3 = F2(i3, i4, i5, i2, this.X[13], 8);
        i2 = F2(i2, i3, i4, i5, this.X[1], 13);
        i5 = F2(i5, i2, i3, i4, this.X[10], 11);
        i4 = F2(i4, i5, i2, i3, this.X[6], 9);
        i3 = F2(i3, i4, i5, i2, this.X[15], 7);
        i2 = F2(i2, i3, i4, i5, this.X[3], 15);
        i5 = F2(i5, i2, i3, i4, this.X[12], 7);
        i4 = F2(i4, i5, i2, i3, this.X[0], 12);
        i3 = F2(i3, i4, i5, i2, this.X[9], 15);
        i2 = F2(i2, i3, i4, i5, this.X[5], 9);
        i5 = F2(i5, i2, i3, i4, this.X[2], 11);
        i4 = F2(i4, i5, i2, i3, this.X[14], 7);
        i3 = F2(i3, i4, i5, i2, this.X[11], 13);
        i2 = F2(i2, i3, i4, i5, this.X[8], 12);
        F1 = FF3(F1, i6, i7, i8, this.X[6], 9);
        i8 = FF3(i8, F1, i6, i7, this.X[11], 13);
        i7 = FF3(i7, i8, F1, i6, this.X[3], 15);
        i6 = FF3(i6, i7, i8, F1, this.X[7], 7);
        F1 = FF3(F1, i6, i7, i8, this.X[0], 12);
        i8 = FF3(i8, F1, i6, i7, this.X[13], 8);
        i7 = FF3(i7, i8, F1, i6, this.X[5], 9);
        i6 = FF3(i6, i7, i8, F1, this.X[10], 11);
        F1 = FF3(F1, i6, i7, i8, this.X[14], 7);
        i8 = FF3(i8, F1, i6, i7, this.X[15], 7);
        i7 = FF3(i7, i8, F1, i6, this.X[8], 12);
        i6 = FF3(i6, i7, i8, F1, this.X[12], 7);
        F1 = FF3(F1, i6, i7, i8, this.X[4], 6);
        i8 = FF3(i8, F1, i6, i7, this.X[9], 15);
        i7 = FF3(i7, i8, F1, i6, this.X[1], 13);
        i6 = FF3(i6, i7, i8, F1, this.X[2], 11);
        i5 = F3(i5, i6, i3, i4, this.X[3], 11);
        i4 = F3(i4, i5, i6, i3, this.X[10], 13);
        i3 = F3(i3, i4, i5, i6, this.X[14], 6);
        i6 = F3(i6, i3, i4, i5, this.X[4], 7);
        i5 = F3(i5, i6, i3, i4, this.X[9], 14);
        i4 = F3(i4, i5, i6, i3, this.X[15], 9);
        i3 = F3(i3, i4, i5, i6, this.X[8], 13);
        i6 = F3(i6, i3, i4, i5, this.X[1], 15);
        i5 = F3(i5, i6, i3, i4, this.X[2], 14);
        i4 = F3(i4, i5, i6, i3, this.X[7], 8);
        i3 = F3(i3, i4, i5, i6, this.X[0], 13);
        i6 = F3(i6, i3, i4, i5, this.X[6], 6);
        i5 = F3(i5, i6, i3, i4, this.X[13], 5);
        i4 = F3(i4, i5, i6, i3, this.X[11], 12);
        i3 = F3(i3, i4, i5, i6, this.X[5], 7);
        i6 = F3(i6, i3, i4, i5, this.X[12], 5);
        F1 = FF2(F1, i2, i7, i8, this.X[15], 9);
        i8 = FF2(i8, F1, i2, i7, this.X[5], 7);
        i7 = FF2(i7, i8, F1, i2, this.X[1], 15);
        i2 = FF2(i2, i7, i8, F1, this.X[3], 11);
        F1 = FF2(F1, i2, i7, i8, this.X[7], 8);
        i8 = FF2(i8, F1, i2, i7, this.X[14], 6);
        i7 = FF2(i7, i8, F1, i2, this.X[6], 6);
        i2 = FF2(i2, i7, i8, F1, this.X[9], 14);
        F1 = FF2(F1, i2, i7, i8, this.X[11], 12);
        i8 = FF2(i8, F1, i2, i7, this.X[8], 13);
        i7 = FF2(i7, i8, F1, i2, this.X[12], 5);
        i2 = FF2(i2, i7, i8, F1, this.X[2], 14);
        F1 = FF2(F1, i2, i7, i8, this.X[10], 13);
        i8 = FF2(i8, F1, i2, i7, this.X[0], 13);
        i7 = FF2(i7, i8, F1, i2, this.X[4], 7);
        i2 = FF2(i2, i7, i8, F1, this.X[13], 5);
        i5 = F4(i5, i6, i7, i4, this.X[1], 11);
        i4 = F4(i4, i5, i6, i7, this.X[9], 12);
        i7 = F4(i7, i4, i5, i6, this.X[11], 14);
        i6 = F4(i6, i7, i4, i5, this.X[10], 15);
        i5 = F4(i5, i6, i7, i4, this.X[0], 14);
        i4 = F4(i4, i5, i6, i7, this.X[8], 15);
        i7 = F4(i7, i4, i5, i6, this.X[12], 9);
        i6 = F4(i6, i7, i4, i5, this.X[4], 8);
        i5 = F4(i5, i6, i7, i4, this.X[13], 9);
        i4 = F4(i4, i5, i6, i7, this.X[3], 14);
        i7 = F4(i7, i4, i5, i6, this.X[7], 5);
        i6 = F4(i6, i7, i4, i5, this.X[15], 6);
        i5 = F4(i5, i6, i7, i4, this.X[14], 8);
        i4 = F4(i4, i5, i6, i7, this.X[5], 6);
        i7 = F4(i7, i4, i5, i6, this.X[6], 5);
        i6 = F4(i6, i7, i4, i5, this.X[2], 12);
        F1 = FF1(F1, i2, i3, i8, this.X[8], 15);
        i8 = FF1(i8, F1, i2, i3, this.X[6], 5);
        i3 = FF1(i3, i8, F1, i2, this.X[4], 8);
        i2 = FF1(i2, i3, i8, F1, this.X[1], 11);
        F1 = FF1(F1, i2, i3, i8, this.X[3], 14);
        i8 = FF1(i8, F1, i2, i3, this.X[11], 14);
        i3 = FF1(i3, i8, F1, i2, this.X[15], 6);
        i2 = FF1(i2, i3, i8, F1, this.X[0], 14);
        F1 = FF1(F1, i2, i3, i8, this.X[5], 6);
        i8 = FF1(i8, F1, i2, i3, this.X[12], 9);
        i3 = FF1(i3, i8, F1, i2, this.X[2], 12);
        i2 = FF1(i2, i3, i8, F1, this.X[13], 9);
        F1 = FF1(F1, i2, i3, i8, this.X[9], 12);
        i8 = FF1(i8, F1, i2, i3, this.X[7], 5);
        i3 = FF1(i3, i8, F1, i2, this.X[10], 15);
        int FF1 = FF1(i2, i3, i8, F1, this.X[14], 8);
        this.H0 += i5;
        this.H1 += i6;
        this.H2 += i7;
        this.H3 += i8;
        this.H4 += F1;
        this.H5 += FF1;
        this.H6 += i3;
        this.H7 += i4;
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
        this.H4 = 1985229328;
        this.H5 = -19088744;
        this.H6 = -1985229329;
        this.H7 = 19088743;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    public void reset(Memoable memoable) {
        copyIn((RIPEMD256Digest) memoable);
    }
}
