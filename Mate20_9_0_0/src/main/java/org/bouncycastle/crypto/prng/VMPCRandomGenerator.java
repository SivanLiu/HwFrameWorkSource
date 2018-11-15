package org.bouncycastle.crypto.prng;

import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.util.Pack;

public class VMPCRandomGenerator implements RandomGenerator {
    private byte[] P = new byte[]{(byte) -69, (byte) 44, (byte) 98, Byte.MAX_VALUE, (byte) -75, (byte) -86, (byte) -44, (byte) 13, (byte) -127, (byte) -2, (byte) -78, (byte) -126, (byte) -53, (byte) -96, (byte) -95, (byte) 8, (byte) 24, (byte) 113, (byte) 86, (byte) -24, (byte) 73, (byte) 2, Tnaf.POW_2_WIDTH, (byte) -60, (byte) -34, (byte) 53, (byte) -91, (byte) -20, Byte.MIN_VALUE, (byte) 18, (byte) -72, (byte) 105, (byte) -38, (byte) 47, (byte) 117, (byte) -52, (byte) -94, (byte) 9, (byte) 54, (byte) 3, (byte) 97, (byte) 45, (byte) -3, (byte) -32, (byte) -35, (byte) 5, (byte) 67, (byte) -112, (byte) -83, (byte) -56, (byte) -31, (byte) -81, (byte) 87, (byte) -101, (byte) 76, (byte) -40, (byte) 81, (byte) -82, (byte) 80, (byte) -123, (byte) 60, (byte) 10, (byte) -28, (byte) -13, (byte) -100, (byte) 38, (byte) 35, (byte) 83, (byte) -55, (byte) -125, (byte) -105, (byte) 70, (byte) -79, (byte) -103, (byte) 100, (byte) 49, (byte) 119, (byte) -43, (byte) 29, (byte) -42, (byte) 120, (byte) -67, (byte) 94, (byte) -80, (byte) -118, (byte) 34, (byte) 56, (byte) -8, (byte) 104, (byte) 43, (byte) 42, (byte) -59, (byte) -45, (byte) -9, PSSSigner.TRAILER_IMPLICIT, (byte) 111, (byte) -33, (byte) 4, (byte) -27, (byte) -107, (byte) 62, (byte) 37, (byte) -122, (byte) -90, (byte) 11, (byte) -113, (byte) -15, (byte) 36, (byte) 14, (byte) -41, (byte) 64, (byte) -77, (byte) -49, (byte) 126, (byte) 6, (byte) 21, (byte) -102, (byte) 77, (byte) 28, (byte) -93, (byte) -37, (byte) 50, (byte) -110, (byte) 88, (byte) 17, (byte) 39, (byte) -12, (byte) 89, (byte) -48, (byte) 78, (byte) 106, (byte) 23, (byte) 91, (byte) -84, (byte) -1, (byte) 7, (byte) -64, (byte) 101, (byte) 121, (byte) -4, (byte) -57, (byte) -51, (byte) 118, (byte) 66, (byte) 93, (byte) -25, (byte) 58, (byte) 52, (byte) 122, (byte) 48, (byte) 40, (byte) 15, (byte) 115, (byte) 1, (byte) -7, (byte) -47, (byte) -46, (byte) 25, (byte) -23, (byte) -111, (byte) -71, (byte) 90, (byte) -19, (byte) 65, (byte) 109, (byte) -76, (byte) -61, (byte) -98, (byte) -65, (byte) 99, (byte) -6, (byte) 31, (byte) 51, (byte) 96, (byte) 71, (byte) -119, (byte) -16, (byte) -106, (byte) 26, (byte) 95, (byte) -109, (byte) 61, (byte) 55, (byte) 75, (byte) -39, (byte) -88, (byte) -63, (byte) 27, (byte) -10, (byte) 57, (byte) -117, (byte) -73, (byte) 12, (byte) 32, (byte) -50, (byte) -120, (byte) 110, (byte) -74, (byte) 116, (byte) -114, (byte) -115, (byte) 22, (byte) 41, (byte) -14, (byte) -121, (byte) -11, (byte) -21, (byte) 112, (byte) -29, (byte) -5, (byte) 85, (byte) -97, (byte) -58, (byte) 68, (byte) 74, (byte) 69, (byte) 125, (byte) -30, (byte) 107, (byte) 92, (byte) 108, (byte) 102, (byte) -87, (byte) -116, (byte) -18, (byte) -124, (byte) 19, (byte) -89, (byte) 30, (byte) -99, (byte) -36, (byte) 103, (byte) 72, (byte) -70, (byte) 46, (byte) -26, (byte) -92, (byte) -85, (byte) 124, (byte) -108, (byte) 0, (byte) 33, (byte) -17, (byte) -22, (byte) -66, (byte) -54, (byte) 114, (byte) 79, (byte) 82, (byte) -104, (byte) 63, (byte) -62, (byte) 20, (byte) 123, (byte) 59, (byte) 84};
    private byte n = (byte) 0;
    private byte s = (byte) -66;

    public void addSeedMaterial(long j) {
        addSeedMaterial(Pack.longToBigEndian(j));
    }

    public void addSeedMaterial(byte[] bArr) {
        for (byte b : bArr) {
            this.s = this.P[((this.s + this.P[this.n & 255]) + b) & 255];
            byte b2 = this.P[this.n & 255];
            this.P[this.n & 255] = this.P[this.s & 255];
            this.P[this.s & 255] = b2;
            this.n = (byte) ((this.n + 1) & 255);
        }
    }

    public void nextBytes(byte[] bArr) {
        nextBytes(bArr, 0, bArr.length);
    }

    public void nextBytes(byte[] bArr, int i, int i2) {
        synchronized (this.P) {
            i2 += i;
            while (i != i2) {
                this.s = this.P[(this.s + this.P[this.n & 255]) & 255];
                bArr[i] = this.P[(this.P[this.P[this.s & 255] & 255] + 1) & 255];
                byte b = this.P[this.n & 255];
                this.P[this.n & 255] = this.P[this.s & 255];
                this.P[this.s & 255] = b;
                this.n = (byte) ((this.n + 1) & 255);
                i++;
            }
        }
    }
}
