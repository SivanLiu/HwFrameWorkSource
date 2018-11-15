package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class SM3Digest extends GeneralDigest {
    private static final int BLOCK_SIZE = 16;
    private static final int DIGEST_LENGTH = 32;
    private static final int[] T = new int[64];
    private int[] V;
    private int[] W;
    private int[] inwords;
    private int xOff;

    static {
        int i;
        int i2 = 0;
        while (true) {
            i = 16;
            if (i2 >= 16) {
                break;
            }
            T[i2] = (2043430169 >>> (32 - i2)) | (2043430169 << i2);
            i2++;
        }
        while (i < 64) {
            i2 = i % 32;
            T[i] = (2055708042 >>> (32 - i2)) | (2055708042 << i2);
            i++;
        }
    }

    public SM3Digest() {
        this.V = new int[8];
        this.inwords = new int[16];
        this.W = new int[68];
        reset();
    }

    public SM3Digest(SM3Digest sM3Digest) {
        super((GeneralDigest) sM3Digest);
        this.V = new int[8];
        this.inwords = new int[16];
        this.W = new int[68];
        copyIn(sM3Digest);
    }

    private int FF0(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    private int FF1(int i, int i2, int i3) {
        return ((i & i3) | (i & i2)) | (i2 & i3);
    }

    private int GG0(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    private int GG1(int i, int i2, int i3) {
        return ((~i) & i3) | (i2 & i);
    }

    private int P0(int i) {
        return (i ^ ((i << 9) | (i >>> 23))) ^ ((i << 17) | (i >>> 15));
    }

    private int P1(int i) {
        return (i ^ ((i << 15) | (i >>> 17))) ^ ((i << 23) | (i >>> 9));
    }

    private void copyIn(SM3Digest sM3Digest) {
        System.arraycopy(sM3Digest.V, 0, this.V, 0, this.V.length);
        System.arraycopy(sM3Digest.inwords, 0, this.inwords, 0, this.inwords.length);
        this.xOff = sM3Digest.xOff;
    }

    public Memoable copy() {
        return new SM3Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        finish();
        Pack.intToBigEndian(this.V, bArr, i);
        reset();
        return 32;
    }

    public String getAlgorithmName() {
        return "SM3";
    }

    public int getDigestSize() {
        return 32;
    }

    protected void processBlock() {
        int i;
        int i2;
        int GG0;
        int i3 = 0;
        while (true) {
            i = 16;
            if (i3 >= 16) {
                break;
            }
            this.W[i3] = this.inwords[i3];
            i3++;
        }
        for (i3 = 16; i3 < 68; i3++) {
            i2 = this.W[i3 - 3];
            i2 = (i2 >>> 17) | (i2 << 15);
            int i4 = this.W[i3 - 13];
            this.W[i3] = (P1(i2 ^ (this.W[i3 - 16] ^ this.W[i3 - 9])) ^ ((i4 >>> 25) | (i4 << 7))) ^ this.W[i3 - 6];
        }
        i3 = this.V[0];
        i2 = this.V[1];
        int i5 = this.V[2];
        int i6 = this.V[3];
        int i7 = this.V[4];
        int i8 = this.V[5];
        int i9 = this.V[6];
        int i10 = this.V[7];
        int i11 = i9;
        i9 = i8;
        i8 = i7;
        i7 = i6;
        i6 = i5;
        i5 = i2;
        i2 = i3;
        i3 = 0;
        while (i3 < i) {
            int i12 = (i2 << 12) | (i2 >>> 20);
            int i13 = (i12 + i8) + T[i3];
            i13 = (i13 << 7) | (i13 >>> 25);
            i12 = i13 ^ i12;
            i = this.W[i3];
            i7 = ((FF0(i2, i5, i6) + i7) + i12) + (this.W[i3 + 4] ^ i);
            GG0 = ((GG0(i8, i9, i11) + i10) + i13) + i;
            i = (i5 << 9) | (i5 >>> 23);
            i5 = (i9 << 19) | (i9 >>> 13);
            i3++;
            i9 = i8;
            i8 = P0(GG0);
            i10 = i11;
            i11 = i5;
            i5 = i2;
            i2 = i7;
            i7 = i6;
            i6 = i;
            i = 16;
        }
        i = i5;
        i3 = 16;
        i5 = i2;
        i2 = i7;
        i7 = i6;
        i6 = i9;
        i9 = i8;
        while (i3 < 64) {
            i8 = (i5 << 12) | (i5 >>> 20);
            GG0 = (i8 + i9) + T[i3];
            GG0 = (GG0 << 7) | (GG0 >>> 25);
            i8 ^= GG0;
            int i14 = this.W[i3];
            i2 = ((FF1(i5, i, i7) + i2) + i8) + (this.W[i3 + 4] ^ i14);
            int GG1 = ((GG1(i9, i6, i11) + i10) + GG0) + i14;
            i = (i >>> 23) | (i << 9);
            i3++;
            i10 = i11;
            i11 = (i6 >>> 13) | (i6 << 19);
            i6 = i9;
            i9 = P0(GG1);
            int i15 = i7;
            i7 = i;
            i = i5;
            i5 = i2;
            i2 = i15;
        }
        int[] iArr = this.V;
        iArr[0] = i5 ^ iArr[0];
        iArr = this.V;
        iArr[1] = i ^ iArr[1];
        iArr = this.V;
        iArr[2] = iArr[2] ^ i7;
        iArr = this.V;
        iArr[3] = i2 ^ iArr[3];
        iArr = this.V;
        iArr[4] = iArr[4] ^ i9;
        iArr = this.V;
        iArr[5] = iArr[5] ^ i6;
        iArr = this.V;
        iArr[6] = iArr[6] ^ i11;
        iArr = this.V;
        iArr[7] = iArr[7] ^ i10;
        this.xOff = 0;
    }

    protected void processLength(long j) {
        if (this.xOff > 14) {
            this.inwords[this.xOff] = 0;
            this.xOff++;
            processBlock();
        }
        while (this.xOff < 14) {
            this.inwords[this.xOff] = 0;
            this.xOff++;
        }
        int[] iArr = this.inwords;
        int i = this.xOff;
        this.xOff = i + 1;
        iArr[i] = (int) (j >>> 32);
        iArr = this.inwords;
        i = this.xOff;
        this.xOff = i + 1;
        iArr[i] = (int) j;
    }

    protected void processWord(byte[] bArr, int i) {
        i++;
        i++;
        this.inwords[this.xOff] = (bArr[i + 1] & 255) | ((((bArr[i] & 255) << 24) | ((bArr[i] & 255) << 16)) | ((bArr[i] & 255) << 8));
        this.xOff++;
        if (this.xOff >= 16) {
            processBlock();
        }
    }

    public void reset() {
        super.reset();
        this.V[0] = 1937774191;
        this.V[1] = 1226093241;
        this.V[2] = 388252375;
        this.V[3] = -628488704;
        this.V[4] = -1452330820;
        this.V[5] = 372324522;
        this.V[6] = -477237683;
        this.V[7] = -1325724082;
        this.xOff = 0;
    }

    public void reset(Memoable memoable) {
        SM3Digest sM3Digest = (SM3Digest) memoable;
        super.copyIn(sM3Digest);
        copyIn(sM3Digest);
    }
}
