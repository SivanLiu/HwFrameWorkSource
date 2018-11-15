package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;

public class RIPEMD320Digest extends GeneralDigest {
    private static final int DIGEST_LENGTH = 40;
    private int H0;
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int H5;
    private int H6;
    private int H7;
    private int H8;
    private int H9;
    private int[] X;
    private int xOff;

    public RIPEMD320Digest() {
        this.X = new int[16];
        reset();
    }

    public RIPEMD320Digest(RIPEMD320Digest rIPEMD320Digest) {
        super((GeneralDigest) rIPEMD320Digest);
        this.X = new int[16];
        doCopy(rIPEMD320Digest);
    }

    private int RL(int i, int i2) {
        return (i >>> (32 - i2)) | (i << i2);
    }

    private void doCopy(RIPEMD320Digest rIPEMD320Digest) {
        super.copyIn(rIPEMD320Digest);
        this.H0 = rIPEMD320Digest.H0;
        this.H1 = rIPEMD320Digest.H1;
        this.H2 = rIPEMD320Digest.H2;
        this.H3 = rIPEMD320Digest.H3;
        this.H4 = rIPEMD320Digest.H4;
        this.H5 = rIPEMD320Digest.H5;
        this.H6 = rIPEMD320Digest.H6;
        this.H7 = rIPEMD320Digest.H7;
        this.H8 = rIPEMD320Digest.H8;
        this.H9 = rIPEMD320Digest.H9;
        System.arraycopy(rIPEMD320Digest.X, 0, this.X, 0, rIPEMD320Digest.X.length);
        this.xOff = rIPEMD320Digest.xOff;
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

    private int f5(int i, int i2, int i3) {
        return i ^ (i2 | (~i3));
    }

    private void unpackWord(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        bArr[i2 + 1] = (byte) (i >>> 8);
        bArr[i2 + 2] = (byte) (i >>> 16);
        bArr[i2 + 3] = (byte) (i >>> 24);
    }

    public Memoable copy() {
        return new RIPEMD320Digest(this);
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
        unpackWord(this.H8, bArr, i + 32);
        unpackWord(this.H9, bArr, i + 36);
        reset();
        return 40;
    }

    public String getAlgorithmName() {
        return "RIPEMD320";
    }

    public int getDigestSize() {
        return 40;
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
        int i9 = this.H8;
        int i10 = this.H9;
        i = RL((i + f1(i2, i3, i4)) + this.X[0], 11) + i5;
        i3 = RL(i3, 10);
        i5 = RL((i5 + f1(i, i2, i3)) + this.X[1], 14) + i4;
        i2 = RL(i2, 10);
        i4 = RL((i4 + f1(i5, i, i2)) + this.X[2], 15) + i3;
        i = RL(i, 10);
        i3 = RL((i3 + f1(i4, i5, i)) + this.X[3], 12) + i2;
        i5 = RL(i5, 10);
        i2 = RL((i2 + f1(i3, i4, i5)) + this.X[4], 5) + i;
        i4 = RL(i4, 10);
        i = RL((i + f1(i2, i3, i4)) + this.X[5], 8) + i5;
        i3 = RL(i3, 10);
        i5 = RL((i5 + f1(i, i2, i3)) + this.X[6], 7) + i4;
        i2 = RL(i2, 10);
        i4 = RL((i4 + f1(i5, i, i2)) + this.X[7], 9) + i3;
        i = RL(i, 10);
        i3 = RL((i3 + f1(i4, i5, i)) + this.X[8], 11) + i2;
        i5 = RL(i5, 10);
        i2 = RL((i2 + f1(i3, i4, i5)) + this.X[9], 13) + i;
        i4 = RL(i4, 10);
        i = RL((i + f1(i2, i3, i4)) + this.X[10], 14) + i5;
        i3 = RL(i3, 10);
        i5 = RL((i5 + f1(i, i2, i3)) + this.X[11], 15) + i4;
        i2 = RL(i2, 10);
        i4 = RL((i4 + f1(i5, i, i2)) + this.X[12], 6) + i3;
        i = RL(i, 10);
        i3 = RL((i3 + f1(i4, i5, i)) + this.X[13], 7) + i2;
        i5 = RL(i5, 10);
        i2 = RL((i2 + f1(i3, i4, i5)) + this.X[14], 9) + i;
        i4 = RL(i4, 10);
        i = RL((i + f1(i2, i3, i4)) + this.X[15], 8) + i5;
        i3 = RL(i3, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[5]) + 1352829926, 8) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f5(i6, i7, i8)) + this.X[14]) + 1352829926, 9) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f5(i10, i6, i7)) + this.X[7]) + 1352829926, 9) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f5(i9, i10, i6)) + this.X[0]) + 1352829926, 11) + i7;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f5(i8, i9, i10)) + this.X[9]) + 1352829926, 13) + i6;
        i9 = RL(i9, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[2]) + 1352829926, 15) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f5(i6, i7, i8)) + this.X[11]) + 1352829926, 15) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f5(i10, i6, i7)) + this.X[4]) + 1352829926, 5) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f5(i9, i10, i6)) + this.X[13]) + 1352829926, 7) + i7;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f5(i8, i9, i10)) + this.X[6]) + 1352829926, 7) + i6;
        i9 = RL(i9, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[15]) + 1352829926, 8) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f5(i6, i7, i8)) + this.X[8]) + 1352829926, 11) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f5(i10, i6, i7)) + this.X[1]) + 1352829926, 14) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f5(i9, i10, i6)) + this.X[10]) + 1352829926, 14) + i7;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f5(i8, i9, i10)) + this.X[3]) + 1352829926, 12) + i6;
        i9 = RL(i9, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[12]) + 1352829926, 6) + i10;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f2(i6, i2, i3)) + this.X[7]) + 1518500249, 7) + i4;
        i2 = RL(i2, 10);
        i4 = RL(((i4 + f2(i5, i6, i2)) + this.X[4]) + 1518500249, 6) + i3;
        i6 = RL(i6, 10);
        i3 = RL(((i3 + f2(i4, i5, i6)) + this.X[13]) + 1518500249, 8) + i2;
        i5 = RL(i5, 10);
        i2 = RL(((i2 + f2(i3, i4, i5)) + this.X[1]) + 1518500249, 13) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f2(i2, i3, i4)) + this.X[10]) + 1518500249, 11) + i5;
        i3 = RL(i3, 10);
        i5 = RL(((i5 + f2(i6, i2, i3)) + this.X[6]) + 1518500249, 9) + i4;
        i2 = RL(i2, 10);
        i4 = RL(((i4 + f2(i5, i6, i2)) + this.X[15]) + 1518500249, 7) + i3;
        i6 = RL(i6, 10);
        i3 = RL(((i3 + f2(i4, i5, i6)) + this.X[3]) + 1518500249, 15) + i2;
        i5 = RL(i5, 10);
        i2 = RL(((i2 + f2(i3, i4, i5)) + this.X[12]) + 1518500249, 7) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f2(i2, i3, i4)) + this.X[0]) + 1518500249, 12) + i5;
        i3 = RL(i3, 10);
        i5 = RL(((i5 + f2(i6, i2, i3)) + this.X[9]) + 1518500249, 15) + i4;
        i2 = RL(i2, 10);
        i4 = RL(((i4 + f2(i5, i6, i2)) + this.X[5]) + 1518500249, 9) + i3;
        i6 = RL(i6, 10);
        i3 = RL(((i3 + f2(i4, i5, i6)) + this.X[2]) + 1518500249, 11) + i2;
        i5 = RL(i5, 10);
        i2 = RL(((i2 + f2(i3, i4, i5)) + this.X[14]) + 1518500249, 7) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f2(i2, i3, i4)) + this.X[11]) + 1518500249, 13) + i5;
        i3 = RL(i3, 10);
        i5 = RL(((i5 + f2(i6, i2, i3)) + this.X[8]) + 1518500249, 12) + i4;
        i2 = RL(i2, 10);
        i10 = RL(((i10 + f4(i, i7, i8)) + this.X[6]) + 1548603684, 9) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f4(i10, i, i7)) + this.X[11]) + 1548603684, 13) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f4(i9, i10, i)) + this.X[3]) + 1548603684, 15) + i7;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f4(i8, i9, i10)) + this.X[7]) + 1548603684, 7) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f4(i7, i8, i9)) + this.X[0]) + 1548603684, 12) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f4(i, i7, i8)) + this.X[13]) + 1548603684, 8) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f4(i10, i, i7)) + this.X[5]) + 1548603684, 9) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f4(i9, i10, i)) + this.X[10]) + 1548603684, 11) + i7;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f4(i8, i9, i10)) + this.X[14]) + 1548603684, 7) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f4(i7, i8, i9)) + this.X[15]) + 1548603684, 7) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f4(i, i7, i8)) + this.X[8]) + 1548603684, 12) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f4(i10, i, i7)) + this.X[12]) + 1548603684, 7) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f4(i9, i10, i)) + this.X[4]) + 1548603684, 6) + i7;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f4(i8, i9, i10)) + this.X[9]) + 1548603684, 15) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f4(i7, i8, i9)) + this.X[1]) + 1548603684, 13) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f4(i, i7, i8)) + this.X[2]) + 1548603684, 11) + i9;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f3(i5, i6, i7)) + this.X[3]) + 1859775393, 11) + i3;
        i6 = RL(i6, 10);
        i3 = RL(((i3 + f3(i4, i5, i6)) + this.X[10]) + 1859775393, 13) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f3(i3, i4, i5)) + this.X[14]) + 1859775393, 6) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f3(i7, i3, i4)) + this.X[4]) + 1859775393, 7) + i5;
        i3 = RL(i3, 10);
        i5 = RL(((i5 + f3(i6, i7, i3)) + this.X[9]) + 1859775393, 14) + i4;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f3(i5, i6, i7)) + this.X[15]) + 1859775393, 9) + i3;
        i6 = RL(i6, 10);
        i3 = RL(((i3 + f3(i4, i5, i6)) + this.X[8]) + 1859775393, 13) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f3(i3, i4, i5)) + this.X[1]) + 1859775393, 15) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f3(i7, i3, i4)) + this.X[2]) + 1859775393, 14) + i5;
        i3 = RL(i3, 10);
        i5 = RL(((i5 + f3(i6, i7, i3)) + this.X[7]) + 1859775393, 8) + i4;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f3(i5, i6, i7)) + this.X[0]) + 1859775393, 13) + i3;
        i6 = RL(i6, 10);
        i3 = RL(((i3 + f3(i4, i5, i6)) + this.X[6]) + 1859775393, 6) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f3(i3, i4, i5)) + this.X[13]) + 1859775393, 5) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f3(i7, i3, i4)) + this.X[11]) + 1859775393, 12) + i5;
        i3 = RL(i3, 10);
        i5 = RL(((i5 + f3(i6, i7, i3)) + this.X[5]) + 1859775393, 7) + i4;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f3(i5, i6, i7)) + this.X[12]) + 1859775393, 5) + i3;
        i6 = RL(i6, 10);
        i9 = RL(((i9 + f3(i10, i, i2)) + this.X[15]) + 1836072691, 9) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f3(i9, i10, i)) + this.X[5]) + 1836072691, 7) + i2;
        i10 = RL(i10, 10);
        i2 = RL(((i2 + f3(i8, i9, i10)) + this.X[1]) + 1836072691, 15) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f3(i2, i8, i9)) + this.X[3]) + 1836072691, 11) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f3(i, i2, i8)) + this.X[7]) + 1836072691, 8) + i9;
        i2 = RL(i2, 10);
        i9 = RL(((i9 + f3(i10, i, i2)) + this.X[14]) + 1836072691, 6) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f3(i9, i10, i)) + this.X[6]) + 1836072691, 6) + i2;
        i10 = RL(i10, 10);
        i2 = RL(((i2 + f3(i8, i9, i10)) + this.X[9]) + 1836072691, 14) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f3(i2, i8, i9)) + this.X[11]) + 1836072691, 12) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f3(i, i2, i8)) + this.X[8]) + 1836072691, 13) + i9;
        i2 = RL(i2, 10);
        i9 = RL(((i9 + f3(i10, i, i2)) + this.X[12]) + 1836072691, 5) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f3(i9, i10, i)) + this.X[2]) + 1836072691, 14) + i2;
        i10 = RL(i10, 10);
        i2 = RL(((i2 + f3(i8, i9, i10)) + this.X[10]) + 1836072691, 13) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f3(i2, i8, i9)) + this.X[0]) + 1836072691, 13) + i10;
        i8 = RL(i8, 10);
        i10 = RL(((i10 + f3(i, i2, i8)) + this.X[4]) + 1836072691, 7) + i9;
        i2 = RL(i2, 10);
        i9 = RL(((i9 + f3(i10, i, i2)) + this.X[13]) + 1836072691, 5) + i8;
        i = RL(i, 10);
        i8 = RL(((i8 + f4(i4, i5, i6)) + this.X[1]) - 1894007588, 11) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f4(i8, i4, i5)) + this.X[9]) - 1894007588, 12) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f4(i7, i8, i4)) + this.X[11]) - 1894007588, 14) + i5;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f4(i6, i7, i8)) + this.X[10]) - 1894007588, 15) + i4;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f4(i5, i6, i7)) + this.X[0]) - 1894007588, 14) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f4(i4, i5, i6)) + this.X[8]) - 1894007588, 15) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f4(i8, i4, i5)) + this.X[12]) - 1894007588, 9) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f4(i7, i8, i4)) + this.X[4]) - 1894007588, 8) + i5;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f4(i6, i7, i8)) + this.X[13]) - 1894007588, 9) + i4;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f4(i5, i6, i7)) + this.X[3]) - 1894007588, 14) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f4(i4, i5, i6)) + this.X[7]) - 1894007588, 5) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f4(i8, i4, i5)) + this.X[15]) - 1894007588, 6) + i6;
        i4 = RL(i4, 10);
        i6 = RL(((i6 + f4(i7, i8, i4)) + this.X[14]) - 1894007588, 8) + i5;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f4(i6, i7, i8)) + this.X[5]) - 1894007588, 6) + i4;
        i7 = RL(i7, 10);
        i4 = RL(((i4 + f4(i5, i6, i7)) + this.X[6]) - 1894007588, 5) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f4(i4, i5, i6)) + this.X[2]) - 1894007588, 12) + i7;
        i5 = RL(i5, 10);
        i3 = RL(((i3 + f2(i9, i10, i)) + this.X[8]) + 2053994217, 15) + i2;
        i10 = RL(i10, 10);
        i2 = RL(((i2 + f2(i3, i9, i10)) + this.X[6]) + 2053994217, 5) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f2(i2, i3, i9)) + this.X[4]) + 2053994217, 8) + i10;
        i3 = RL(i3, 10);
        i10 = RL(((i10 + f2(i, i2, i3)) + this.X[1]) + 2053994217, 11) + i9;
        i2 = RL(i2, 10);
        i9 = RL(((i9 + f2(i10, i, i2)) + this.X[3]) + 2053994217, 14) + i3;
        i = RL(i, 10);
        i3 = RL(((i3 + f2(i9, i10, i)) + this.X[11]) + 2053994217, 14) + i2;
        i10 = RL(i10, 10);
        i2 = RL(((i2 + f2(i3, i9, i10)) + this.X[15]) + 2053994217, 6) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f2(i2, i3, i9)) + this.X[0]) + 2053994217, 14) + i10;
        i3 = RL(i3, 10);
        i10 = RL(((i10 + f2(i, i2, i3)) + this.X[5]) + 2053994217, 6) + i9;
        i2 = RL(i2, 10);
        i9 = RL(((i9 + f2(i10, i, i2)) + this.X[12]) + 2053994217, 9) + i3;
        i = RL(i, 10);
        i3 = RL(((i3 + f2(i9, i10, i)) + this.X[2]) + 2053994217, 12) + i2;
        i10 = RL(i10, 10);
        i2 = RL(((i2 + f2(i3, i9, i10)) + this.X[13]) + 2053994217, 9) + i;
        i9 = RL(i9, 10);
        i = RL(((i + f2(i2, i3, i9)) + this.X[9]) + 2053994217, 12) + i10;
        i3 = RL(i3, 10);
        i10 = RL(((i10 + f2(i, i2, i3)) + this.X[7]) + 2053994217, 5) + i9;
        i2 = RL(i2, 10);
        i9 = RL(((i9 + f2(i10, i, i2)) + this.X[10]) + 2053994217, 15) + i3;
        i = RL(i, 10);
        i3 = RL(((i3 + f2(i9, i10, i)) + this.X[14]) + 2053994217, 8) + i2;
        i10 = RL(i10, 10);
        i7 = RL(((i7 + f5(i8, i9, i5)) + this.X[4]) - 1454113458, 9) + i6;
        i9 = RL(i9, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[0]) - 1454113458, 15) + i5;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f5(i6, i7, i8)) + this.X[5]) - 1454113458, 5) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f5(i5, i6, i7)) + this.X[9]) - 1454113458, 11) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f5(i9, i5, i6)) + this.X[7]) - 1454113458, 6) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f5(i8, i9, i5)) + this.X[12]) - 1454113458, 8) + i6;
        i9 = RL(i9, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[2]) - 1454113458, 13) + i5;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f5(i6, i7, i8)) + this.X[10]) - 1454113458, 12) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f5(i5, i6, i7)) + this.X[14]) - 1454113458, 5) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f5(i9, i5, i6)) + this.X[1]) - 1454113458, 12) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f5(i8, i9, i5)) + this.X[3]) - 1454113458, 13) + i6;
        i9 = RL(i9, 10);
        i6 = RL(((i6 + f5(i7, i8, i9)) + this.X[8]) - 1454113458, 14) + i5;
        i8 = RL(i8, 10);
        i5 = RL(((i5 + f5(i6, i7, i8)) + this.X[11]) - 1454113458, 11) + i9;
        i7 = RL(i7, 10);
        i9 = RL(((i9 + f5(i5, i6, i7)) + this.X[6]) - 1454113458, 8) + i8;
        i6 = RL(i6, 10);
        i8 = RL(((i8 + f5(i9, i5, i6)) + this.X[15]) - 1454113458, 5) + i7;
        i5 = RL(i5, 10);
        i7 = RL(((i7 + f5(i8, i9, i5)) + this.X[13]) - 1454113458, 6) + i6;
        i9 = RL(i9, 10);
        i2 = RL((i2 + f1(i3, i4, i10)) + this.X[12], 8) + i;
        i4 = RL(i4, 10);
        i = RL((i + f1(i2, i3, i4)) + this.X[15], 5) + i10;
        i3 = RL(i3, 10);
        i10 = RL((i10 + f1(i, i2, i3)) + this.X[10], 12) + i4;
        i2 = RL(i2, 10);
        i4 = RL((i4 + f1(i10, i, i2)) + this.X[4], 9) + i3;
        i = RL(i, 10);
        i3 = RL((i3 + f1(i4, i10, i)) + this.X[1], 12) + i2;
        i10 = RL(i10, 10);
        i2 = RL((i2 + f1(i3, i4, i10)) + this.X[5], 5) + i;
        i4 = RL(i4, 10);
        i = RL((i + f1(i2, i3, i4)) + this.X[8], 14) + i10;
        i3 = RL(i3, 10);
        i10 = RL((i10 + f1(i, i2, i3)) + this.X[7], 6) + i4;
        i2 = RL(i2, 10);
        i4 = RL((i4 + f1(i10, i, i2)) + this.X[6], 8) + i3;
        i = RL(i, 10);
        i3 = RL((i3 + f1(i4, i10, i)) + this.X[2], 13) + i2;
        i10 = RL(i10, 10);
        i2 = RL((i2 + f1(i3, i4, i10)) + this.X[13], 6) + i;
        i4 = RL(i4, 10);
        i = RL((i + f1(i2, i3, i4)) + this.X[14], 5) + i10;
        i3 = RL(i3, 10);
        i10 = RL((i10 + f1(i, i2, i3)) + this.X[0], 15) + i4;
        i2 = RL(i2, 10);
        i4 = RL((i4 + f1(i10, i, i2)) + this.X[3], 13) + i3;
        i = RL(i, 10);
        i3 = RL((i3 + f1(i4, i10, i)) + this.X[9], 11) + i2;
        i10 = RL(i10, 10);
        i2 = RL((i2 + f1(i3, i4, i10)) + this.X[11], 11) + i;
        i4 = RL(i4, 10);
        this.H0 += i6;
        this.H1 += i7;
        this.H2 += i8;
        this.H3 += i9;
        this.H4 += i10;
        this.H5 += i;
        this.H6 += i2;
        this.H7 += i3;
        this.H8 += i4;
        this.H9 += i5;
        i = 0;
        this.xOff = 0;
        while (i != this.X.length) {
            this.X[i] = 0;
            i++;
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
        this.H4 = -1009589776;
        this.H5 = 1985229328;
        this.H6 = -19088744;
        this.H7 = -1985229329;
        this.H8 = 19088743;
        this.H9 = 1009589775;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    public void reset(Memoable memoable) {
        doCopy((RIPEMD320Digest) memoable);
    }
}
