package org.bouncycastle.crypto.digests;

import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class SHA1Digest extends GeneralDigest implements EncodableDigest {
    private static final int DIGEST_LENGTH = 20;
    private static final int Y1 = 1518500249;
    private static final int Y2 = 1859775393;
    private static final int Y3 = -1894007588;
    private static final int Y4 = -899497514;
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int H5;
    private int[] X;
    private int xOff;

    public SHA1Digest() {
        this.X = new int[80];
        reset();
    }

    public SHA1Digest(SHA1Digest sHA1Digest) {
        super((GeneralDigest) sHA1Digest);
        this.X = new int[80];
        copyIn(sHA1Digest);
    }

    public SHA1Digest(byte[] bArr) {
        super(bArr);
        this.X = new int[80];
        this.H1 = Pack.bigEndianToInt(bArr, 16);
        this.H2 = Pack.bigEndianToInt(bArr, 20);
        this.H3 = Pack.bigEndianToInt(bArr, 24);
        this.H4 = Pack.bigEndianToInt(bArr, 28);
        this.H5 = Pack.bigEndianToInt(bArr, 32);
        this.xOff = Pack.bigEndianToInt(bArr, 36);
        for (int i = 0; i != this.xOff; i++) {
            this.X[i] = Pack.bigEndianToInt(bArr, 40 + (i * 4));
        }
    }

    private void copyIn(SHA1Digest sHA1Digest) {
        this.H1 = sHA1Digest.H1;
        this.H2 = sHA1Digest.H2;
        this.H3 = sHA1Digest.H3;
        this.H4 = sHA1Digest.H4;
        this.H5 = sHA1Digest.H5;
        System.arraycopy(sHA1Digest.X, 0, this.X, 0, sHA1Digest.X.length);
        this.xOff = sHA1Digest.xOff;
    }

    private int f(int i, int i2, int i3) {
        return ((~i) & i3) | (i2 & i);
    }

    private int g(int i, int i2, int i3) {
        return ((i & i3) | (i & i2)) | (i2 & i3);
    }

    private int h(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    public Memoable copy() {
        return new SHA1Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        finish();
        Pack.intToBigEndian(this.H1, bArr, i);
        Pack.intToBigEndian(this.H2, bArr, i + 4);
        Pack.intToBigEndian(this.H3, bArr, i + 8);
        Pack.intToBigEndian(this.H4, bArr, i + 12);
        Pack.intToBigEndian(this.H5, bArr, i + 16);
        reset();
        return 20;
    }

    public String getAlgorithmName() {
        return McElieceCCA2KeyGenParameterSpec.SHA1;
    }

    public int getDigestSize() {
        return 20;
    }

    public byte[] getEncodedState() {
        byte[] bArr = new byte[((this.xOff * 4) + 40)];
        super.populateState(bArr);
        Pack.intToBigEndian(this.H1, bArr, 16);
        Pack.intToBigEndian(this.H2, bArr, 20);
        Pack.intToBigEndian(this.H3, bArr, 24);
        Pack.intToBigEndian(this.H4, bArr, 28);
        Pack.intToBigEndian(this.H5, bArr, 32);
        Pack.intToBigEndian(this.xOff, bArr, 36);
        for (int i = 0; i != this.xOff; i++) {
            Pack.intToBigEndian(this.X[i], bArr, (i * 4) + 40);
        }
        return bArr;
    }

    protected void processBlock() {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        for (i = 16; i < 80; i++) {
            i2 = ((this.X[i - 3] ^ this.X[i - 8]) ^ this.X[i - 14]) ^ this.X[i - 16];
            this.X[i] = (i2 >>> 31) | (i2 << 1);
        }
        i = this.H1;
        i2 = this.H2;
        int i6 = this.H3;
        int i7 = this.H4;
        int i8 = this.H5;
        int i9 = i7;
        int i10 = 0;
        i7 = i6;
        i6 = i2;
        i2 = i;
        i = i10;
        while (i < 4) {
            i3 = i10 + 1;
            i8 += ((((i2 << 5) | (i2 >>> 27)) + f(i6, i7, i9)) + this.X[i10]) + Y1;
            i6 = (i6 >>> 2) | (i6 << 30);
            i4 = i3 + 1;
            i9 += ((((i8 << 5) | (i8 >>> 27)) + f(i2, i6, i7)) + this.X[i3]) + Y1;
            i2 = (i2 >>> 2) | (i2 << 30);
            i3 = i4 + 1;
            i7 += ((((i9 << 5) | (i9 >>> 27)) + f(i8, i2, i6)) + this.X[i4]) + Y1;
            i8 = (i8 >>> 2) | (i8 << 30);
            i4 = i3 + 1;
            i6 += ((((i7 << 5) | (i7 >>> 27)) + f(i9, i8, i2)) + this.X[i3]) + Y1;
            i9 = (i9 >>> 2) | (i9 << 30);
            i2 += ((((i6 << 5) | (i6 >>> 27)) + f(i7, i9, i8)) + this.X[i4]) + Y1;
            i7 = (i7 >>> 2) | (i7 << 30);
            i++;
            i10 = i4 + 1;
        }
        i = 0;
        while (i < 4) {
            i4 = i10 + 1;
            i8 += ((((i2 << 5) | (i2 >>> 27)) + h(i6, i7, i9)) + this.X[i10]) + Y2;
            i6 = (i6 >>> 2) | (i6 << 30);
            i5 = i4 + 1;
            i9 += ((((i8 << 5) | (i8 >>> 27)) + h(i2, i6, i7)) + this.X[i4]) + Y2;
            i2 = (i2 >>> 2) | (i2 << 30);
            i4 = i5 + 1;
            i7 += ((((i9 << 5) | (i9 >>> 27)) + h(i8, i2, i6)) + this.X[i5]) + Y2;
            i8 = (i8 >>> 2) | (i8 << 30);
            i5 = i4 + 1;
            i6 += ((((i7 << 5) | (i7 >>> 27)) + h(i9, i8, i2)) + this.X[i4]) + Y2;
            i9 = (i9 >>> 2) | (i9 << 30);
            i2 += ((((i6 << 5) | (i6 >>> 27)) + h(i7, i9, i8)) + this.X[i5]) + Y2;
            i7 = (i7 >>> 2) | (i7 << 30);
            i++;
            i10 = i5 + 1;
        }
        i = 0;
        while (i < 4) {
            i4 = i10 + 1;
            i8 += ((((i2 << 5) | (i2 >>> 27)) + g(i6, i7, i9)) + this.X[i10]) + Y3;
            i6 = (i6 >>> 2) | (i6 << 30);
            i5 = i4 + 1;
            i9 += ((((i8 << 5) | (i8 >>> 27)) + g(i2, i6, i7)) + this.X[i4]) + Y3;
            i2 = (i2 >>> 2) | (i2 << 30);
            i4 = i5 + 1;
            i7 += ((((i9 << 5) | (i9 >>> 27)) + g(i8, i2, i6)) + this.X[i5]) + Y3;
            i8 = (i8 >>> 2) | (i8 << 30);
            i5 = i4 + 1;
            i6 += ((((i7 << 5) | (i7 >>> 27)) + g(i9, i8, i2)) + this.X[i4]) + Y3;
            i9 = (i9 >>> 2) | (i9 << 30);
            i2 += ((((i6 << 5) | (i6 >>> 27)) + g(i7, i9, i8)) + this.X[i5]) + Y3;
            i7 = (i7 >>> 2) | (i7 << 30);
            i++;
            i10 = i5 + 1;
        }
        i = 0;
        while (i <= 3) {
            i3 = i10 + 1;
            i8 += ((((i2 << 5) | (i2 >>> 27)) + h(i6, i7, i9)) + this.X[i10]) + Y4;
            i6 = (i6 >>> 2) | (i6 << 30);
            i4 = i3 + 1;
            i9 += ((((i8 << 5) | (i8 >>> 27)) + h(i2, i6, i7)) + this.X[i3]) + Y4;
            i2 = (i2 >>> 2) | (i2 << 30);
            i3 = i4 + 1;
            i7 += ((((i9 << 5) | (i9 >>> 27)) + h(i8, i2, i6)) + this.X[i4]) + Y4;
            i8 = (i8 >>> 2) | (i8 << 30);
            i4 = i3 + 1;
            i6 += ((((i7 << 5) | (i7 >>> 27)) + h(i9, i8, i2)) + this.X[i3]) + Y4;
            i9 = (i9 >>> 2) | (i9 << 30);
            i2 += ((((i6 << 5) | (i6 >>> 27)) + h(i7, i9, i8)) + this.X[i4]) + Y4;
            i7 = (i7 >>> 2) | (i7 << 30);
            i++;
            i10 = i4 + 1;
        }
        this.H1 += i2;
        this.H2 += i6;
        this.H3 += i7;
        this.H4 += i9;
        this.H5 += i8;
        this.xOff = 0;
        for (i = 0; i < 16; i++) {
            this.X[i] = 0;
        }
    }

    protected void processLength(long j) {
        if (this.xOff > 14) {
            processBlock();
        }
        this.X[14] = (int) (j >>> 32);
        this.X[15] = (int) j;
    }

    protected void processWord(byte[] bArr, int i) {
        i++;
        i++;
        this.X[this.xOff] = (bArr[i + 1] & 255) | (((bArr[i] << 24) | ((bArr[i] & 255) << 16)) | ((bArr[i] & 255) << 8));
        int i2 = this.xOff + 1;
        this.xOff = i2;
        if (i2 == 16) {
            processBlock();
        }
    }

    public void reset() {
        super.reset();
        this.H1 = 1732584193;
        this.H2 = -271733879;
        this.H3 = -1732584194;
        this.H4 = 271733878;
        this.H5 = -1009589776;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    public void reset(Memoable memoable) {
        SHA1Digest sHA1Digest = (SHA1Digest) memoable;
        super.copyIn(sHA1Digest);
        copyIn(sHA1Digest);
    }
}
