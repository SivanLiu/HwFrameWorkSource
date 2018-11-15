package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;

public class MD4Digest extends GeneralDigest {
    private static final int DIGEST_LENGTH = 16;
    private static final int S11 = 3;
    private static final int S12 = 7;
    private static final int S13 = 11;
    private static final int S14 = 19;
    private static final int S21 = 3;
    private static final int S22 = 5;
    private static final int S23 = 9;
    private static final int S24 = 13;
    private static final int S31 = 3;
    private static final int S32 = 9;
    private static final int S33 = 11;
    private static final int S34 = 15;
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int[] X;
    private int xOff;

    public MD4Digest() {
        this.X = new int[16];
        reset();
    }

    public MD4Digest(MD4Digest mD4Digest) {
        super((GeneralDigest) mD4Digest);
        this.X = new int[16];
        copyIn(mD4Digest);
    }

    private int F(int i, int i2, int i3) {
        return ((~i) & i3) | (i2 & i);
    }

    private int G(int i, int i2, int i3) {
        return ((i & i3) | (i & i2)) | (i2 & i3);
    }

    private int H(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    private void copyIn(MD4Digest mD4Digest) {
        super.copyIn(mD4Digest);
        this.H1 = mD4Digest.H1;
        this.H2 = mD4Digest.H2;
        this.H3 = mD4Digest.H3;
        this.H4 = mD4Digest.H4;
        System.arraycopy(mD4Digest.X, 0, this.X, 0, mD4Digest.X.length);
        this.xOff = mD4Digest.xOff;
    }

    private int rotateLeft(int i, int i2) {
        return (i >>> (32 - i2)) | (i << i2);
    }

    private void unpackWord(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        bArr[i2 + 1] = (byte) (i >>> 8);
        bArr[i2 + 2] = (byte) (i >>> 16);
        bArr[i2 + 3] = (byte) (i >>> 24);
    }

    public Memoable copy() {
        return new MD4Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        finish();
        unpackWord(this.H1, bArr, i);
        unpackWord(this.H2, bArr, i + 4);
        unpackWord(this.H3, bArr, i + 8);
        unpackWord(this.H4, bArr, i + 12);
        reset();
        return 16;
    }

    public String getAlgorithmName() {
        return "MD4";
    }

    public int getDigestSize() {
        return 16;
    }

    protected void processBlock() {
        int i = this.H1;
        int i2 = this.H2;
        int i3 = this.H3;
        int i4 = this.H4;
        i = rotateLeft((i + F(i2, i3, i4)) + this.X[0], 3);
        i4 = rotateLeft((i4 + F(i, i2, i3)) + this.X[1], 7);
        i3 = rotateLeft((i3 + F(i4, i, i2)) + this.X[2], 11);
        i2 = rotateLeft((i2 + F(i3, i4, i)) + this.X[3], 19);
        i = rotateLeft((i + F(i2, i3, i4)) + this.X[4], 3);
        i4 = rotateLeft((i4 + F(i, i2, i3)) + this.X[5], 7);
        i3 = rotateLeft((i3 + F(i4, i, i2)) + this.X[6], 11);
        i2 = rotateLeft((i2 + F(i3, i4, i)) + this.X[7], 19);
        i = rotateLeft((i + F(i2, i3, i4)) + this.X[8], 3);
        i4 = rotateLeft((i4 + F(i, i2, i3)) + this.X[9], 7);
        i3 = rotateLeft((i3 + F(i4, i, i2)) + this.X[10], 11);
        i2 = rotateLeft((i2 + F(i3, i4, i)) + this.X[11], 19);
        i = rotateLeft((i + F(i2, i3, i4)) + this.X[12], 3);
        i4 = rotateLeft((i4 + F(i, i2, i3)) + this.X[13], 7);
        i3 = rotateLeft((i3 + F(i4, i, i2)) + this.X[14], 11);
        i2 = rotateLeft((i2 + F(i3, i4, i)) + this.X[15], 19);
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[0]) + 1518500249, 3);
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[4]) + 1518500249, 5);
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[8]) + 1518500249, 9);
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[12]) + 1518500249, 13);
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[1]) + 1518500249, 3);
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[5]) + 1518500249, 5);
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[9]) + 1518500249, 9);
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[13]) + 1518500249, 13);
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[2]) + 1518500249, 3);
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[6]) + 1518500249, 5);
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[10]) + 1518500249, 9);
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[14]) + 1518500249, 13);
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[3]) + 1518500249, 3);
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[7]) + 1518500249, 5);
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[11]) + 1518500249, 9);
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[15]) + 1518500249, 13);
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[0]) + 1859775393, 3);
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[8]) + 1859775393, 9);
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[4]) + 1859775393, 11);
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[12]) + 1859775393, 15);
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[2]) + 1859775393, 3);
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[10]) + 1859775393, 9);
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[6]) + 1859775393, 11);
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[14]) + 1859775393, 15);
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[1]) + 1859775393, 3);
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[9]) + 1859775393, 9);
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[5]) + 1859775393, 11);
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[13]) + 1859775393, 15);
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[3]) + 1859775393, 3);
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[11]) + 1859775393, 9);
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[7]) + 1859775393, 11);
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[15]) + 1859775393, 15);
        this.H1 += i;
        this.H2 += i2;
        this.H3 += i3;
        this.H4 += i4;
        this.xOff = 0;
        for (i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
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
        this.H1 = 1732584193;
        this.H2 = -271733879;
        this.H3 = -1732584194;
        this.H4 = 271733878;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    public void reset(Memoable memoable) {
        copyIn((MD4Digest) memoable);
    }
}
