package org.bouncycastle.pqc.crypto.newhope;

import java.security.SecureRandom;
import org.bouncycastle.crypto.digests.SHA3Digest;

class NewHope {
    public static final int AGREEMENT_SIZE = 32;
    public static final int POLY_SIZE = 1024;
    public static final int SENDA_BYTES = 1824;
    public static final int SENDB_BYTES = 2048;
    private static final boolean STATISTICAL_TEST = false;

    NewHope() {
    }

    static void decodeA(short[] sArr, byte[] bArr, byte[] bArr2) {
        Poly.fromBytes(sArr, bArr2);
        System.arraycopy(bArr2, 1792, bArr, 0, 32);
    }

    static void decodeB(short[] sArr, short[] sArr2, byte[] bArr) {
        Poly.fromBytes(sArr, bArr);
        for (int i = 0; i < 256; i++) {
            int i2 = 4 * i;
            int i3 = bArr[1792 + i] & 255;
            sArr2[i2 + 0] = (short) (i3 & 3);
            sArr2[i2 + 1] = (short) ((i3 >>> 2) & 3);
            sArr2[i2 + 2] = (short) ((i3 >>> 4) & 3);
            sArr2[i2 + 3] = (short) (i3 >>> 6);
        }
    }

    static void encodeA(byte[] bArr, short[] sArr, byte[] bArr2) {
        Poly.toBytes(bArr, sArr);
        System.arraycopy(bArr2, 0, bArr, 1792, 32);
    }

    static void encodeB(byte[] bArr, short[] sArr, short[] sArr2) {
        Poly.toBytes(bArr, sArr);
        for (int i = 0; i < 256; i++) {
            int i2 = 4 * i;
            bArr[1792 + i] = (byte) (((sArr2[i2 + 2] << 4) | (sArr2[i2] | (sArr2[i2 + 1] << 2))) | (sArr2[i2 + 3] << 6));
        }
    }

    static void generateA(short[] sArr, byte[] bArr) {
        Poly.uniform(sArr, bArr);
    }

    public static void keygen(SecureRandom secureRandom, byte[] bArr, short[] sArr) {
        byte[] bArr2 = new byte[32];
        secureRandom.nextBytes(bArr2);
        sha3(bArr2);
        short[] sArr2 = new short[1024];
        generateA(sArr2, bArr2);
        byte[] bArr3 = new byte[32];
        secureRandom.nextBytes(bArr3);
        Poly.getNoise(sArr, bArr3, (byte) 0);
        Poly.toNTT(sArr);
        short[] sArr3 = new short[1024];
        Poly.getNoise(sArr3, bArr3, (byte) 1);
        Poly.toNTT(sArr3);
        short[] sArr4 = new short[1024];
        Poly.pointWise(sArr2, sArr, sArr4);
        sArr = new short[1024];
        Poly.add(sArr4, sArr3, sArr);
        encodeA(bArr, sArr, bArr2);
    }

    static void sha3(byte[] bArr) {
        SHA3Digest sHA3Digest = new SHA3Digest(256);
        sHA3Digest.update(bArr, 0, 32);
        sHA3Digest.doFinal(bArr, 0);
    }

    public static void sharedA(byte[] bArr, short[] sArr, byte[] bArr2) {
        short[] sArr2 = new short[1024];
        short[] sArr3 = new short[1024];
        decodeB(sArr2, sArr3, bArr2);
        short[] sArr4 = new short[1024];
        Poly.pointWise(sArr, sArr2, sArr4);
        Poly.fromNTT(sArr4);
        ErrorCorrection.rec(bArr, sArr4, sArr3);
        sha3(bArr);
    }

    public static void sharedB(SecureRandom secureRandom, byte[] bArr, byte[] bArr2, byte[] bArr3) {
        short[] sArr = new short[1024];
        byte[] bArr4 = new byte[32];
        decodeA(sArr, bArr4, bArr3);
        short[] sArr2 = new short[1024];
        generateA(sArr2, bArr4);
        byte[] bArr5 = new byte[32];
        secureRandom.nextBytes(bArr5);
        short[] sArr3 = new short[1024];
        Poly.getNoise(sArr3, bArr5, (byte) 0);
        Poly.toNTT(sArr3);
        short[] sArr4 = new short[1024];
        Poly.getNoise(sArr4, bArr5, (byte) 1);
        Poly.toNTT(sArr4);
        short[] sArr5 = new short[1024];
        Poly.pointWise(sArr2, sArr3, sArr5);
        Poly.add(sArr5, sArr4, sArr5);
        sArr2 = new short[1024];
        Poly.pointWise(sArr, sArr3, sArr2);
        Poly.fromNTT(sArr2);
        sArr3 = new short[1024];
        Poly.getNoise(sArr3, bArr5, (byte) 2);
        Poly.add(sArr2, sArr3, sArr2);
        sArr3 = new short[1024];
        ErrorCorrection.helpRec(sArr3, sArr2, bArr5, (byte) 3);
        encodeB(bArr2, sArr5, sArr3);
        ErrorCorrection.rec(bArr, sArr2, sArr3);
        sha3(bArr);
    }
}
