package org.bouncycastle.crypto.digests;

import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class MD5Digest extends GeneralDigest implements EncodableDigest {
    private static final int DIGEST_LENGTH = 16;
    private static final int S11 = 7;
    private static final int S12 = 12;
    private static final int S13 = 17;
    private static final int S14 = 22;
    private static final int S21 = 5;
    private static final int S22 = 9;
    private static final int S23 = 14;
    private static final int S24 = 20;
    private static final int S31 = 4;
    private static final int S32 = 11;
    private static final int S33 = 16;
    private static final int S34 = 23;
    private static final int S41 = 6;
    private static final int S42 = 10;
    private static final int S43 = 15;
    private static final int S44 = 21;
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int[] X;
    private int xOff;

    public MD5Digest() {
        this.X = new int[16];
        reset();
    }

    public MD5Digest(MD5Digest mD5Digest) {
        super((GeneralDigest) mD5Digest);
        this.X = new int[16];
        copyIn(mD5Digest);
    }

    public MD5Digest(byte[] bArr) {
        super(bArr);
        this.X = new int[16];
        this.H1 = Pack.bigEndianToInt(bArr, 16);
        this.H2 = Pack.bigEndianToInt(bArr, 20);
        this.H3 = Pack.bigEndianToInt(bArr, 24);
        this.H4 = Pack.bigEndianToInt(bArr, 28);
        this.xOff = Pack.bigEndianToInt(bArr, 32);
        for (int i = 0; i != this.xOff; i++) {
            this.X[i] = Pack.bigEndianToInt(bArr, 36 + (i * 4));
        }
    }

    private int F(int i, int i2, int i3) {
        return ((~i) & i3) | (i2 & i);
    }

    private int G(int i, int i2, int i3) {
        return (i & i3) | (i2 & (~i3));
    }

    private int H(int i, int i2, int i3) {
        return (i ^ i2) ^ i3;
    }

    private int K(int i, int i2, int i3) {
        return (i | (~i3)) ^ i2;
    }

    private void copyIn(MD5Digest mD5Digest) {
        super.copyIn(mD5Digest);
        this.H1 = mD5Digest.H1;
        this.H2 = mD5Digest.H2;
        this.H3 = mD5Digest.H3;
        this.H4 = mD5Digest.H4;
        System.arraycopy(mD5Digest.X, 0, this.X, 0, mD5Digest.X.length);
        this.xOff = mD5Digest.xOff;
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
        return new MD5Digest(this);
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
        return "MD5";
    }

    public int getDigestSize() {
        return 16;
    }

    public byte[] getEncodedState() {
        byte[] bArr = new byte[((this.xOff * 4) + 36)];
        super.populateState(bArr);
        Pack.intToBigEndian(this.H1, bArr, 16);
        Pack.intToBigEndian(this.H2, bArr, 20);
        Pack.intToBigEndian(this.H3, bArr, 24);
        Pack.intToBigEndian(this.H4, bArr, 28);
        Pack.intToBigEndian(this.xOff, bArr, 32);
        for (int i = 0; i != this.xOff; i++) {
            Pack.intToBigEndian(this.X[i], bArr, (i * 4) + 36);
        }
        return bArr;
    }

    protected void processBlock() {
        int i = this.H1;
        int i2 = this.H2;
        int i3 = this.H3;
        int i4 = this.H4;
        i = rotateLeft(((i + F(i2, i3, i4)) + this.X[0]) - 680876936, 7) + i2;
        i4 = rotateLeft(((i4 + F(i, i2, i3)) + this.X[1]) - 389564586, 12) + i;
        i3 = rotateLeft(((i3 + F(i4, i, i2)) + this.X[2]) + 606105819, 17) + i4;
        i2 = rotateLeft(((i2 + F(i3, i4, i)) + this.X[3]) - 1044525330, 22) + i3;
        i = rotateLeft(((i + F(i2, i3, i4)) + this.X[4]) - 176418897, 7) + i2;
        i4 = rotateLeft(((i4 + F(i, i2, i3)) + this.X[5]) + 1200080426, 12) + i;
        i3 = rotateLeft(((i3 + F(i4, i, i2)) + this.X[6]) - 1473231341, 17) + i4;
        i2 = rotateLeft(((i2 + F(i3, i4, i)) + this.X[7]) - 45705983, 22) + i3;
        i = rotateLeft(((i + F(i2, i3, i4)) + this.X[8]) + 1770035416, 7) + i2;
        i4 = rotateLeft(((i4 + F(i, i2, i3)) + this.X[9]) - 1958414417, 12) + i;
        i3 = rotateLeft(((i3 + F(i4, i, i2)) + this.X[10]) - 42063, 17) + i4;
        i2 = rotateLeft(((i2 + F(i3, i4, i)) + this.X[11]) - 1990404162, 22) + i3;
        i = rotateLeft(((i + F(i2, i3, i4)) + this.X[12]) + 1804603682, 7) + i2;
        i4 = rotateLeft(((i4 + F(i, i2, i3)) + this.X[13]) - 40341101, 12) + i;
        i3 = rotateLeft(((i3 + F(i4, i, i2)) + this.X[14]) - 1502002290, 17) + i4;
        i2 = rotateLeft(((i2 + F(i3, i4, i)) + this.X[15]) + 1236535329, 22) + i3;
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[1]) - 165796510, 5) + i2;
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[6]) - 1069501632, 9) + i;
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[11]) + 643717713, 14) + i4;
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[0]) - 373897302, 20) + i3;
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[5]) - 701558691, 5) + i2;
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[10]) + 38016083, 9) + i;
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[15]) - 660478335, 14) + i4;
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[4]) - 405537848, 20) + i3;
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[9]) + 568446438, 5) + i2;
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[14]) - 1019803690, 9) + i;
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[3]) - 187363961, 14) + i4;
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[8]) + 1163531501, 20) + i3;
        i = rotateLeft(((i + G(i2, i3, i4)) + this.X[13]) - 1444681467, 5) + i2;
        i4 = rotateLeft(((i4 + G(i, i2, i3)) + this.X[2]) - 51403784, 9) + i;
        i3 = rotateLeft(((i3 + G(i4, i, i2)) + this.X[7]) + 1735328473, 14) + i4;
        i2 = rotateLeft(((i2 + G(i3, i4, i)) + this.X[12]) - 1926607734, 20) + i3;
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[5]) - 378558, 4) + i2;
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[8]) - 2022574463, 11) + i;
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[11]) + 1839030562, 16) + i4;
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[14]) - 35309556, 23) + i3;
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[1]) - 1530992060, 4) + i2;
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[4]) + 1272893353, 11) + i;
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[7]) - 155497632, 16) + i4;
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[10]) - 1094730640, 23) + i3;
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[13]) + 681279174, 4) + i2;
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[0]) - 358537222, 11) + i;
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[3]) - 722521979, 16) + i4;
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[6]) + 76029189, 23) + i3;
        i = rotateLeft(((i + H(i2, i3, i4)) + this.X[9]) - 640364487, 4) + i2;
        i4 = rotateLeft(((i4 + H(i, i2, i3)) + this.X[12]) - 421815835, 11) + i;
        i3 = rotateLeft(((i3 + H(i4, i, i2)) + this.X[15]) + 530742520, 16) + i4;
        i2 = rotateLeft(((i2 + H(i3, i4, i)) + this.X[2]) - 995338651, 23) + i3;
        i = rotateLeft(((i + K(i2, i3, i4)) + this.X[0]) - 198630844, 6) + i2;
        i4 = rotateLeft(((i4 + K(i, i2, i3)) + this.X[7]) + 1126891415, 10) + i;
        i3 = rotateLeft(((i3 + K(i4, i, i2)) + this.X[14]) - 1416354905, 15) + i4;
        i2 = rotateLeft(((i2 + K(i3, i4, i)) + this.X[5]) - 57434055, 21) + i3;
        i = rotateLeft(((i + K(i2, i3, i4)) + this.X[12]) + 1700485571, 6) + i2;
        i4 = rotateLeft(((i4 + K(i, i2, i3)) + this.X[3]) - 1894986606, 10) + i;
        i3 = rotateLeft(((i3 + K(i4, i, i2)) + this.X[10]) - 1051523, 15) + i4;
        i2 = rotateLeft(((i2 + K(i3, i4, i)) + this.X[1]) - 2054922799, 21) + i3;
        i = rotateLeft(((i + K(i2, i3, i4)) + this.X[8]) + 1873313359, 6) + i2;
        i4 = rotateLeft(((i4 + K(i, i2, i3)) + this.X[15]) - 30611744, 10) + i;
        i3 = rotateLeft(((i3 + K(i4, i, i2)) + this.X[6]) - 1560198380, 15) + i4;
        i2 = rotateLeft(((i2 + K(i3, i4, i)) + this.X[13]) + 1309151649, 21) + i3;
        i = rotateLeft(((i + K(i2, i3, i4)) + this.X[4]) - 145523070, 6) + i2;
        i4 = rotateLeft(((i4 + K(i, i2, i3)) + this.X[11]) - 1120210379, 10) + i;
        i3 = rotateLeft(((i3 + K(i4, i, i2)) + this.X[2]) + 718787259, 15) + i4;
        i2 = rotateLeft(((i2 + K(i3, i4, i)) + this.X[9]) - 343485551, 21) + i3;
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
        copyIn((MD5Digest) memoable);
    }
}
