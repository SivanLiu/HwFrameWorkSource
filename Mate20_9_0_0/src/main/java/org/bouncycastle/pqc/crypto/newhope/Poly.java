package org.bouncycastle.pqc.crypto.newhope;

import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.util.Pack;

class Poly {
    Poly() {
    }

    static void add(short[] sArr, short[] sArr2, short[] sArr3) {
        for (int i = 0; i < 1024; i++) {
            sArr3[i] = Reduce.barrett((short) (sArr[i] + sArr2[i]));
        }
    }

    static void fromBytes(short[] sArr, byte[] bArr) {
        for (int i = 0; i < 256; i++) {
            int i2 = 7 * i;
            int i3 = bArr[i2 + 0] & 255;
            int i4 = bArr[i2 + 1] & 255;
            int i5 = bArr[i2 + 2] & 255;
            int i6 = bArr[i2 + 3] & 255;
            int i7 = bArr[i2 + 4] & 255;
            int i8 = bArr[i2 + 5] & 255;
            i2 = bArr[i2 + 6] & 255;
            int i9 = 4 * i;
            sArr[i9 + 0] = (short) (i3 | ((i4 & 63) << 8));
            sArr[i9 + 1] = (short) (((i4 >>> 6) | (i5 << 2)) | ((i6 & 15) << 10));
            sArr[i9 + 2] = (short) (((i6 >>> 4) | (i7 << 4)) | ((i8 & 3) << 12));
            sArr[i9 + 3] = (short) ((i2 << 6) | (i8 >>> 2));
        }
    }

    static void fromNTT(short[] sArr) {
        NTT.bitReverse(sArr);
        NTT.core(sArr, Precomp.OMEGAS_INV_MONTGOMERY);
        NTT.mulCoefficients(sArr, Precomp.PSIS_INV_MONTGOMERY);
    }

    static void getNoise(short[] sArr, byte[] bArr, byte b) {
        byte[] bArr2 = new byte[8];
        bArr2[0] = b;
        byte[] bArr3 = new byte[PKIFailureInfo.certConfirmed];
        ChaCha20.process(bArr, bArr2, bArr3, 0, bArr3.length);
        for (int i = 0; i < 1024; i++) {
            int bigEndianToInt = Pack.bigEndianToInt(bArr3, i * 4);
            int i2 = 0;
            int i3 = i2;
            while (i2 < 8) {
                i3 += (bigEndianToInt >> i2) & 16843009;
                i2++;
            }
            sArr[i] = (short) (((((i3 >>> 24) + (i3 >>> 0)) & 255) + 12289) - (((i3 >>> 16) + (i3 >>> 8)) & 255));
        }
    }

    private static short normalize(short s) {
        s = Reduce.barrett(s);
        int i = s - 12289;
        return (short) (((s ^ i) & (i >> 31)) ^ i);
    }

    static void pointWise(short[] sArr, short[] sArr2, short[] sArr3) {
        for (int i = 0; i < 1024; i++) {
            sArr3[i] = Reduce.montgomery((sArr[i] & 65535) * (65535 & Reduce.montgomery(3186 * (sArr2[i] & 65535))));
        }
    }

    static void toBytes(byte[] bArr, short[] sArr) {
        for (int i = 0; i < 256; i++) {
            int i2 = 4 * i;
            short normalize = normalize(sArr[i2 + 0]);
            short normalize2 = normalize(sArr[i2 + 1]);
            short normalize3 = normalize(sArr[i2 + 2]);
            short normalize4 = normalize(sArr[i2 + 3]);
            int i3 = 7 * i;
            bArr[i3 + 0] = (byte) normalize;
            bArr[i3 + 1] = (byte) ((normalize >> 8) | (normalize2 << 6));
            bArr[i3 + 2] = (byte) (normalize2 >> 2);
            bArr[i3 + 3] = (byte) ((normalize2 >> 10) | (normalize3 << 4));
            bArr[i3 + 4] = (byte) (normalize3 >> 4);
            bArr[i3 + 5] = (byte) ((normalize3 >> 12) | (normalize4 << 2));
            bArr[i3 + 6] = (byte) (normalize4 >> 6);
        }
    }

    static void toNTT(short[] sArr) {
        NTT.mulCoefficients(sArr, Precomp.PSIS_BITREV_MONTGOMERY);
        NTT.core(sArr, Precomp.OMEGAS_MONTGOMERY);
    }

    static void uniform(short[] sArr, byte[] bArr) {
        SHAKEDigest sHAKEDigest = new SHAKEDigest(128);
        sHAKEDigest.update(bArr, 0, bArr.length);
        int i = 0;
        while (true) {
            byte[] bArr2 = new byte[256];
            sHAKEDigest.doOutput(bArr2, 0, bArr2.length);
            int i2 = i;
            for (i = 0; i < bArr2.length; i += 2) {
                int i3 = (bArr2[i] & 255) | ((bArr2[i + 1] & 255) << 8);
                if (i3 < 61445) {
                    int i4 = i2 + 1;
                    sArr[i2] = (short) i3;
                    if (i4 != 1024) {
                        i2 = i4;
                    } else {
                        return;
                    }
                }
            }
            i = i2;
        }
    }
}
