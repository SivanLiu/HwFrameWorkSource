package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.util.Memoable;

public class MD2Digest implements ExtendedDigest, Memoable {
    private static final int DIGEST_LENGTH = 16;
    private static final byte[] S = new byte[]{(byte) 41, (byte) 46, (byte) 67, (byte) -55, (byte) -94, (byte) -40, (byte) 124, (byte) 1, (byte) 61, (byte) 54, (byte) 84, (byte) -95, (byte) -20, (byte) -16, (byte) 6, (byte) 19, (byte) 98, (byte) -89, (byte) 5, (byte) -13, (byte) -64, (byte) -57, (byte) 115, (byte) -116, (byte) -104, (byte) -109, (byte) 43, (byte) -39, PSSSigner.TRAILER_IMPLICIT, (byte) 76, (byte) -126, (byte) -54, (byte) 30, (byte) -101, (byte) 87, (byte) 60, (byte) -3, (byte) -44, (byte) -32, (byte) 22, (byte) 103, (byte) 66, (byte) 111, (byte) 24, (byte) -118, (byte) 23, (byte) -27, (byte) 18, (byte) -66, (byte) 78, (byte) -60, (byte) -42, (byte) -38, (byte) -98, (byte) -34, (byte) 73, (byte) -96, (byte) -5, (byte) -11, (byte) -114, (byte) -69, (byte) 47, (byte) -18, (byte) 122, (byte) -87, (byte) 104, (byte) 121, (byte) -111, (byte) 21, (byte) -78, (byte) 7, (byte) 63, (byte) -108, (byte) -62, Tnaf.POW_2_WIDTH, (byte) -119, (byte) 11, (byte) 34, (byte) 95, (byte) 33, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 93, (byte) -102, (byte) 90, (byte) -112, (byte) 50, (byte) 39, (byte) 53, (byte) 62, (byte) -52, (byte) -25, (byte) -65, (byte) -9, (byte) -105, (byte) 3, (byte) -1, (byte) 25, (byte) 48, (byte) -77, (byte) 72, (byte) -91, (byte) -75, (byte) -47, (byte) -41, (byte) 94, (byte) -110, (byte) 42, (byte) -84, (byte) 86, (byte) -86, (byte) -58, (byte) 79, (byte) -72, (byte) 56, (byte) -46, (byte) -106, (byte) -92, (byte) 125, (byte) -74, (byte) 118, (byte) -4, (byte) 107, (byte) -30, (byte) -100, (byte) 116, (byte) 4, (byte) -15, (byte) 69, (byte) -99, (byte) 112, (byte) 89, (byte) 100, (byte) 113, (byte) -121, (byte) 32, (byte) -122, (byte) 91, (byte) -49, (byte) 101, (byte) -26, (byte) 45, (byte) -88, (byte) 2, (byte) 27, (byte) 96, (byte) 37, (byte) -83, (byte) -82, (byte) -80, (byte) -71, (byte) -10, (byte) 28, (byte) 70, (byte) 97, (byte) 105, (byte) 52, (byte) 64, (byte) 126, (byte) 15, (byte) 85, (byte) 71, (byte) -93, (byte) 35, (byte) -35, (byte) 81, (byte) -81, (byte) 58, (byte) -61, (byte) 92, (byte) -7, (byte) -50, (byte) -70, (byte) -59, (byte) -22, (byte) 38, (byte) 44, (byte) 83, (byte) 13, (byte) 110, (byte) -123, (byte) 40, (byte) -124, (byte) 9, (byte) -45, (byte) -33, (byte) -51, (byte) -12, (byte) 65, (byte) -127, (byte) 77, (byte) 82, (byte) 106, (byte) -36, (byte) 55, (byte) -56, (byte) 108, (byte) -63, (byte) -85, (byte) -6, (byte) 36, (byte) -31, (byte) 123, (byte) 8, (byte) 12, (byte) -67, (byte) -79, (byte) 74, (byte) 120, (byte) -120, (byte) -107, (byte) -117, (byte) -29, (byte) 99, (byte) -24, (byte) 109, (byte) -23, (byte) -53, (byte) -43, (byte) -2, (byte) 59, (byte) 0, (byte) 29, (byte) 57, (byte) -14, (byte) -17, (byte) -73, (byte) 14, (byte) 102, (byte) 88, (byte) -48, (byte) -28, (byte) -90, (byte) 119, (byte) 114, (byte) -8, (byte) -21, (byte) 117, (byte) 75, (byte) 10, (byte) 49, (byte) 68, (byte) 80, (byte) -76, (byte) -113, (byte) -19, (byte) 31, (byte) 26, (byte) -37, (byte) -103, (byte) -115, (byte) 51, (byte) -97, (byte) 17, (byte) -125, (byte) 20};
    private byte[] C;
    private int COff;
    private byte[] M;
    private byte[] X;
    private int mOff;
    private int xOff;

    public MD2Digest() {
        this.X = new byte[48];
        this.M = new byte[16];
        this.C = new byte[16];
        reset();
    }

    public MD2Digest(MD2Digest mD2Digest) {
        this.X = new byte[48];
        this.M = new byte[16];
        this.C = new byte[16];
        copyIn(mD2Digest);
    }

    private void copyIn(MD2Digest mD2Digest) {
        System.arraycopy(mD2Digest.X, 0, this.X, 0, mD2Digest.X.length);
        this.xOff = mD2Digest.xOff;
        System.arraycopy(mD2Digest.M, 0, this.M, 0, mD2Digest.M.length);
        this.mOff = mD2Digest.mOff;
        System.arraycopy(mD2Digest.C, 0, this.C, 0, mD2Digest.C.length);
        this.COff = mD2Digest.COff;
    }

    public Memoable copy() {
        return new MD2Digest(this);
    }

    public int doFinal(byte[] bArr, int i) {
        byte length = (byte) (this.M.length - this.mOff);
        for (int i2 = this.mOff; i2 < this.M.length; i2++) {
            this.M[i2] = length;
        }
        processCheckSum(this.M);
        processBlock(this.M);
        processBlock(this.C);
        System.arraycopy(this.X, this.xOff, bArr, i, 16);
        reset();
        return 16;
    }

    public String getAlgorithmName() {
        return "MD2";
    }

    public int getByteLength() {
        return 16;
    }

    public int getDigestSize() {
        return 16;
    }

    protected void processBlock(byte[] bArr) {
        int i;
        for (i = 0; i < 16; i++) {
            this.X[i + 16] = bArr[i];
            this.X[i + 32] = (byte) (bArr[i] ^ this.X[i]);
        }
        int i2 = 0;
        i = i2;
        while (i2 < 18) {
            int i3 = i;
            for (i = 0; i < 48; i++) {
                byte[] bArr2 = this.X;
                byte b = (byte) (S[i3] ^ bArr2[i]);
                bArr2[i] = b;
                i3 = b & 255;
            }
            i = (i3 + i2) % 256;
            i2++;
        }
    }

    protected void processCheckSum(byte[] bArr) {
        int i = this.C[15];
        for (int i2 = 0; i2 < 16; i2++) {
            byte[] bArr2 = this.C;
            bArr2[i2] = (byte) (S[(i ^ bArr[i2]) & 255] ^ bArr2[i2]);
            i = this.C[i2];
        }
    }

    public void reset() {
        int i;
        this.xOff = 0;
        for (i = 0; i != this.X.length; i++) {
            this.X[i] = (byte) 0;
        }
        this.mOff = 0;
        for (i = 0; i != this.M.length; i++) {
            this.M[i] = (byte) 0;
        }
        this.COff = 0;
        for (i = 0; i != this.C.length; i++) {
            this.C[i] = (byte) 0;
        }
    }

    public void reset(Memoable memoable) {
        copyIn((MD2Digest) memoable);
    }

    public void update(byte b) {
        byte[] bArr = this.M;
        int i = this.mOff;
        this.mOff = i + 1;
        bArr[i] = b;
        if (this.mOff == 16) {
            processCheckSum(this.M);
            processBlock(this.M);
            this.mOff = 0;
        }
    }

    public void update(byte[] bArr, int i, int i2) {
        while (this.mOff != 0 && i2 > 0) {
            update(bArr[i]);
            i++;
            i2--;
        }
        while (i2 > 16) {
            System.arraycopy(bArr, i, this.M, 0, 16);
            processCheckSum(this.M);
            processBlock(this.M);
            i2 -= 16;
            i += 16;
        }
        while (i2 > 0) {
            update(bArr[i]);
            i++;
            i2--;
        }
    }
}
