package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.util.Memoable;
import com.android.org.bouncycastle.util.Pack;

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

    public SHA1Digest(SHA1Digest t) {
        super((GeneralDigest) t);
        this.X = new int[80];
        copyIn(t);
    }

    public SHA1Digest(byte[] encodedState) {
        super(encodedState);
        this.X = new int[80];
        this.H1 = Pack.bigEndianToInt(encodedState, 16);
        this.H2 = Pack.bigEndianToInt(encodedState, 20);
        this.H3 = Pack.bigEndianToInt(encodedState, 24);
        this.H4 = Pack.bigEndianToInt(encodedState, 28);
        this.H5 = Pack.bigEndianToInt(encodedState, 32);
        this.xOff = Pack.bigEndianToInt(encodedState, 36);
        for (int i = 0; i != this.xOff; i++) {
            this.X[i] = Pack.bigEndianToInt(encodedState, 40 + (i * 4));
        }
    }

    private void copyIn(SHA1Digest t) {
        this.H1 = t.H1;
        this.H2 = t.H2;
        this.H3 = t.H3;
        this.H4 = t.H4;
        this.H5 = t.H5;
        System.arraycopy(t.X, 0, this.X, 0, t.X.length);
        this.xOff = t.xOff;
    }

    public String getAlgorithmName() {
        return "SHA-1";
    }

    public int getDigestSize() {
        return 20;
    }

    protected void processWord(byte[] in, int inOff) {
        inOff++;
        inOff++;
        this.X[this.xOff] = (((in[inOff] << 24) | ((in[inOff] & 255) << 16)) | ((in[inOff] & 255) << 8)) | (in[inOff + 1] & 255);
        int i = this.xOff + 1;
        this.xOff = i;
        if (i == 16) {
            processBlock();
        }
    }

    protected void processLength(long bitLength) {
        if (this.xOff > 14) {
            processBlock();
        }
        this.X[14] = (int) (bitLength >>> 32);
        this.X[15] = (int) (-1 & bitLength);
    }

    public int doFinal(byte[] out, int outOff) {
        finish();
        Pack.intToBigEndian(this.H1, out, outOff);
        Pack.intToBigEndian(this.H2, out, outOff + 4);
        Pack.intToBigEndian(this.H3, out, outOff + 8);
        Pack.intToBigEndian(this.H4, out, outOff + 12);
        Pack.intToBigEndian(this.H5, out, outOff + 16);
        reset();
        return 20;
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

    private int f(int u, int v, int w) {
        return (u & v) | ((~u) & w);
    }

    private int h(int u, int v, int w) {
        return (u ^ v) ^ w;
    }

    private int g(int u, int v, int w) {
        return ((u & v) | (u & w)) | (v & w);
    }

    protected void processBlock() {
        int i;
        int t;
        int idx;
        int idx2;
        int idx3;
        for (i = 16; i < 80; i++) {
            t = ((this.X[i - 3] ^ this.X[i - 8]) ^ this.X[i - 14]) ^ this.X[i - 16];
            this.X[i] = (t << 1) | (t >>> 31);
        }
        i = this.H1;
        t = this.H2;
        int C = this.H3;
        int D = this.H4;
        int E = this.H5;
        int idx4 = 0;
        int D2 = D;
        D = C;
        C = t;
        t = i;
        i = 0;
        while (i < 4) {
            idx = idx4 + 1;
            E += ((((t << 5) | (t >>> 27)) + f(C, D, D2)) + this.X[idx4]) + Y1;
            C = (C << 30) | (C >>> 2);
            idx2 = idx + 1;
            D2 += ((((E << 5) | (E >>> 27)) + f(t, C, D)) + this.X[idx]) + Y1;
            t = (t << 30) | (t >>> 2);
            idx = idx2 + 1;
            D += ((((D2 << 5) | (D2 >>> 27)) + f(E, t, C)) + this.X[idx2]) + Y1;
            E = (E << 30) | (E >>> 2);
            idx2 = idx + 1;
            C += ((((D << 5) | (D >>> 27)) + f(D2, E, t)) + this.X[idx]) + Y1;
            D2 = (D2 << 30) | (D2 >>> 2);
            t += ((((C << 5) | (C >>> 27)) + f(D, D2, E)) + this.X[idx2]) + Y1;
            D = (D << 30) | (D >>> 2);
            i++;
            idx4 = idx2 + 1;
        }
        i = 0;
        while (i < 4) {
            idx2 = idx4 + 1;
            E += ((((t << 5) | (t >>> 27)) + h(C, D, D2)) + this.X[idx4]) + Y2;
            C = (C << 30) | (C >>> 2);
            idx3 = idx2 + 1;
            D2 += ((((E << 5) | (E >>> 27)) + h(t, C, D)) + this.X[idx2]) + Y2;
            t = (t << 30) | (t >>> 2);
            idx2 = idx3 + 1;
            D += ((((D2 << 5) | (D2 >>> 27)) + h(E, t, C)) + this.X[idx3]) + Y2;
            E = (E << 30) | (E >>> 2);
            idx3 = idx2 + 1;
            C += ((((D << 5) | (D >>> 27)) + h(D2, E, t)) + this.X[idx2]) + Y2;
            D2 = (D2 << 30) | (D2 >>> 2);
            t += ((((C << 5) | (C >>> 27)) + h(D, D2, E)) + this.X[idx3]) + Y2;
            D = (D << 30) | (D >>> 2);
            i++;
            idx4 = idx3 + 1;
        }
        i = 0;
        while (i < 4) {
            idx2 = idx4 + 1;
            E += ((((t << 5) | (t >>> 27)) + g(C, D, D2)) + this.X[idx4]) + Y3;
            C = (C << 30) | (C >>> 2);
            idx3 = idx2 + 1;
            D2 += ((((E << 5) | (E >>> 27)) + g(t, C, D)) + this.X[idx2]) + Y3;
            t = (t << 30) | (t >>> 2);
            idx2 = idx3 + 1;
            D += ((((D2 << 5) | (D2 >>> 27)) + g(E, t, C)) + this.X[idx3]) + Y3;
            E = (E << 30) | (E >>> 2);
            idx3 = idx2 + 1;
            C += ((((D << 5) | (D >>> 27)) + g(D2, E, t)) + this.X[idx2]) + Y3;
            D2 = (D2 << 30) | (D2 >>> 2);
            t += ((((C << 5) | (C >>> 27)) + g(D, D2, E)) + this.X[idx3]) + Y3;
            D = (D << 30) | (D >>> 2);
            i++;
            idx4 = idx3 + 1;
        }
        i = 0;
        while (i <= 3) {
            idx = idx4 + 1;
            E += ((((t << 5) | (t >>> 27)) + h(C, D, D2)) + this.X[idx4]) + Y4;
            C = (C << 30) | (C >>> 2);
            idx2 = idx + 1;
            D2 += ((((E << 5) | (E >>> 27)) + h(t, C, D)) + this.X[idx]) + Y4;
            t = (t << 30) | (t >>> 2);
            idx = idx2 + 1;
            D += ((((D2 << 5) | (D2 >>> 27)) + h(E, t, C)) + this.X[idx2]) + Y4;
            E = (E << 30) | (E >>> 2);
            idx2 = idx + 1;
            C += ((((D << 5) | (D >>> 27)) + h(D2, E, t)) + this.X[idx]) + Y4;
            D2 = (D2 << 30) | (D2 >>> 2);
            t += ((((C << 5) | (C >>> 27)) + h(D, D2, E)) + this.X[idx2]) + Y4;
            D = (D << 30) | (D >>> 2);
            i++;
            idx4 = idx2 + 1;
        }
        this.H1 += t;
        this.H2 += C;
        this.H3 += D;
        this.H4 += D2;
        this.H5 += E;
        this.xOff = 0;
        for (i = 0; i < 16; i++) {
            this.X[i] = 0;
        }
    }

    public Memoable copy() {
        return new SHA1Digest(this);
    }

    public void reset(Memoable other) {
        SHA1Digest d = (SHA1Digest) other;
        super.copyIn(d);
        copyIn(d);
    }

    public byte[] getEncodedState() {
        byte[] state = new byte[((this.xOff * 4) + 40)];
        super.populateState(state);
        Pack.intToBigEndian(this.H1, state, 16);
        Pack.intToBigEndian(this.H2, state, 20);
        Pack.intToBigEndian(this.H3, state, 24);
        Pack.intToBigEndian(this.H4, state, 28);
        Pack.intToBigEndian(this.H5, state, 32);
        Pack.intToBigEndian(this.xOff, state, 36);
        for (int i = 0; i != this.xOff; i++) {
            Pack.intToBigEndian(this.X[i], state, (i * 4) + 40);
        }
        return state;
    }
}
