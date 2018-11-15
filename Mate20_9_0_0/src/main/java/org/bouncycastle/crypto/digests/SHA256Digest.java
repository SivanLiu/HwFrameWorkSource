package org.bouncycastle.crypto.digests;

import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Pack;

public class SHA256Digest extends GeneralDigest implements EncodableDigest {
    private static final int DIGEST_LENGTH = 32;
    static final int[] K = new int[]{1116352408, 1899447441, -1245643825, -373957723, 961987163, 1508970993, -1841331548, -1424204075, -670586216, 310598401, 607225278, 1426881987, 1925078388, -2132889090, -1680079193, -1046744716, -459576895, -272742522, 264347078, 604807628, 770255983, 1249150122, 1555081692, 1996064986, -1740746414, -1473132947, -1341970488, -1084653625, -958395405, -710438585, 113926993, 338241895, 666307205, 773529912, 1294757372, 1396182291, 1695183700, 1986661051, -2117940946, -1838011259, -1564481375, -1474664885, -1035236496, -949202525, -778901479, -694614492, -200395387, 275423344, 430227734, 506948616, 659060556, 883997877, 958139571, 1322822218, 1537002063, 1747873779, 1955562222, 2024104815, -2067236844, -1933114872, -1866530822, -1538233109, -1090935817, -965641998};
    private int H1;
    private int H2;
    private int H3;
    private int H4;
    private int H5;
    private int H6;
    private int H7;
    private int H8;
    private int[] X;
    private int xOff;

    public SHA256Digest() {
        this.X = new int[64];
        reset();
    }

    public SHA256Digest(SHA256Digest sHA256Digest) {
        super((GeneralDigest) sHA256Digest);
        this.X = new int[64];
        copyIn(sHA256Digest);
    }

    public SHA256Digest(byte[] bArr) {
        super(bArr);
        this.X = new int[64];
        this.H1 = Pack.bigEndianToInt(bArr, 16);
        this.H2 = Pack.bigEndianToInt(bArr, 20);
        this.H3 = Pack.bigEndianToInt(bArr, 24);
        this.H4 = Pack.bigEndianToInt(bArr, 28);
        this.H5 = Pack.bigEndianToInt(bArr, 32);
        this.H6 = Pack.bigEndianToInt(bArr, 36);
        this.H7 = Pack.bigEndianToInt(bArr, 40);
        this.H8 = Pack.bigEndianToInt(bArr, 44);
        this.xOff = Pack.bigEndianToInt(bArr, 48);
        for (int i = 0; i != this.xOff; i++) {
            this.X[i] = Pack.bigEndianToInt(bArr, 52 + (i * 4));
        }
    }

    private int Ch(int i, int i2, int i3) {
        return ((~i) & i3) ^ (i2 & i);
    }

    private int Maj(int i, int i2, int i3) {
        return ((i & i3) ^ (i & i2)) ^ (i2 & i3);
    }

    private int Sum0(int i) {
        return ((i << 10) | (i >>> 22)) ^ (((i >>> 2) | (i << 30)) ^ ((i >>> 13) | (i << 19)));
    }

    private int Sum1(int i) {
        return ((i << 7) | (i >>> 25)) ^ (((i >>> 6) | (i << 26)) ^ ((i >>> 11) | (i << 21)));
    }

    private int Theta0(int i) {
        return (i >>> 3) ^ (((i >>> 7) | (i << 25)) ^ ((i >>> 18) | (i << 14)));
    }

    private int Theta1(int i) {
        return (i >>> 10) ^ (((i >>> 17) | (i << 15)) ^ ((i >>> 19) | (i << 13)));
    }

    private void copyIn(SHA256Digest sHA256Digest) {
        super.copyIn(sHA256Digest);
        this.H1 = sHA256Digest.H1;
        this.H2 = sHA256Digest.H2;
        this.H3 = sHA256Digest.H3;
        this.H4 = sHA256Digest.H4;
        this.H5 = sHA256Digest.H5;
        this.H6 = sHA256Digest.H6;
        this.H7 = sHA256Digest.H7;
        this.H8 = sHA256Digest.H8;
        System.arraycopy(sHA256Digest.X, 0, this.X, 0, sHA256Digest.X.length);
        this.xOff = sHA256Digest.xOff;
    }

    public Memoable copy() {
        return new SHA256Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        finish();
        Pack.intToBigEndian(this.H1, bArr, i);
        Pack.intToBigEndian(this.H2, bArr, i + 4);
        Pack.intToBigEndian(this.H3, bArr, i + 8);
        Pack.intToBigEndian(this.H4, bArr, i + 12);
        Pack.intToBigEndian(this.H5, bArr, i + 16);
        Pack.intToBigEndian(this.H6, bArr, i + 20);
        Pack.intToBigEndian(this.H7, bArr, i + 24);
        Pack.intToBigEndian(this.H8, bArr, i + 28);
        reset();
        return 32;
    }

    public String getAlgorithmName() {
        return McElieceCCA2KeyGenParameterSpec.SHA256;
    }

    public int getDigestSize() {
        return 32;
    }

    public byte[] getEncodedState() {
        byte[] bArr = new byte[((this.xOff * 4) + 52)];
        super.populateState(bArr);
        Pack.intToBigEndian(this.H1, bArr, 16);
        Pack.intToBigEndian(this.H2, bArr, 20);
        Pack.intToBigEndian(this.H3, bArr, 24);
        Pack.intToBigEndian(this.H4, bArr, 28);
        Pack.intToBigEndian(this.H5, bArr, 32);
        Pack.intToBigEndian(this.H6, bArr, 36);
        Pack.intToBigEndian(this.H7, bArr, 40);
        Pack.intToBigEndian(this.H8, bArr, 44);
        Pack.intToBigEndian(this.xOff, bArr, 48);
        for (int i = 0; i != this.xOff; i++) {
            Pack.intToBigEndian(this.X[i], bArr, (i * 4) + 52);
        }
        return bArr;
    }

    protected void processBlock() {
        int i;
        for (i = 16; i <= 63; i++) {
            this.X[i] = ((Theta1(this.X[i - 2]) + this.X[i - 7]) + Theta0(this.X[i - 15])) + this.X[i - 16];
        }
        i = this.H1;
        int i2 = this.H2;
        int i3 = this.H3;
        int i4 = this.H4;
        int i5 = this.H5;
        int i6 = this.H6;
        int i7 = this.H7;
        int i8 = this.H8;
        int i9 = i2;
        int i10 = i3;
        i2 = 0;
        i3 = i;
        for (i = i2; i < 8; i++) {
            i8 += ((Sum1(i5) + Ch(i5, i6, i7)) + K[i2]) + this.X[i2];
            i4 += i8;
            i8 += Sum0(i3) + Maj(i3, i9, i10);
            i2++;
            i7 += ((Sum1(i4) + Ch(i4, i5, i6)) + K[i2]) + this.X[i2];
            i10 += i7;
            i7 += Sum0(i8) + Maj(i8, i3, i9);
            i2++;
            i6 += ((Sum1(i10) + Ch(i10, i4, i5)) + K[i2]) + this.X[i2];
            i9 += i6;
            i6 += Sum0(i7) + Maj(i7, i8, i3);
            i2++;
            i5 += ((Sum1(i9) + Ch(i9, i10, i4)) + K[i2]) + this.X[i2];
            i3 += i5;
            i5 += Sum0(i6) + Maj(i6, i7, i8);
            i2++;
            i4 += ((Sum1(i3) + Ch(i3, i9, i10)) + K[i2]) + this.X[i2];
            i8 += i4;
            i4 += Sum0(i5) + Maj(i5, i6, i7);
            i2++;
            i10 += ((Sum1(i8) + Ch(i8, i3, i9)) + K[i2]) + this.X[i2];
            i7 += i10;
            i10 += Sum0(i4) + Maj(i4, i5, i6);
            i2++;
            i9 += ((Sum1(i7) + Ch(i7, i8, i3)) + K[i2]) + this.X[i2];
            i6 += i9;
            i9 += Sum0(i10) + Maj(i10, i4, i5);
            i2++;
            i3 += ((Sum1(i6) + Ch(i6, i7, i8)) + K[i2]) + this.X[i2];
            i5 += i3;
            i3 += Sum0(i9) + Maj(i9, i10, i4);
            i2++;
        }
        this.H1 += i3;
        this.H2 += i9;
        this.H3 += i10;
        this.H4 += i4;
        this.H5 += i5;
        this.H6 += i6;
        this.H7 += i7;
        this.H8 += i8;
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
        this.X[15] = (int) (j & -1);
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
        this.H1 = 1779033703;
        this.H2 = -1150833019;
        this.H3 = 1013904242;
        this.H4 = -1521486534;
        this.H5 = 1359893119;
        this.H6 = -1694144372;
        this.H7 = 528734635;
        this.H8 = 1541459225;
        this.xOff = 0;
        for (int i = 0; i != this.X.length; i++) {
            this.X[i] = 0;
        }
    }

    public void reset(Memoable memoable) {
        copyIn((SHA256Digest) memoable);
    }
}
