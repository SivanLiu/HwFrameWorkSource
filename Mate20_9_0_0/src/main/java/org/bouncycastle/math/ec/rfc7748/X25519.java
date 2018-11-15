package org.bouncycastle.math.ec.rfc7748;

public abstract class X25519 {
    private static final int C_A = 486662;
    private static final int C_A24 = 121666;
    private static final int[] PsubS_x = new int[]{64258704, 46628941, 18905110, 42949224, 8920788, 10663709, 35115447, 21804323, 8973338, 4366948};
    private static int[] precompBase = null;

    private static int decode32(byte[] bArr, int i) {
        i++;
        i++;
        return (bArr[i + 1] << 24) | (((bArr[i] & 255) | ((bArr[i] & 255) << 8)) | ((bArr[i] & 255) << 16));
    }

    private static void decodeScalar(byte[] bArr, int i, int[] iArr) {
        for (int i2 = 0; i2 < 8; i2++) {
            iArr[i2] = decode32(bArr, (i2 * 4) + i);
        }
        iArr[0] = iArr[0] & -8;
        iArr[7] = iArr[7] & Integer.MAX_VALUE;
        iArr[7] = iArr[7] | 1073741824;
    }

    private static void pointDouble(int[] iArr, int[] iArr2) {
        int[] create = X25519Field.create();
        int[] create2 = X25519Field.create();
        X25519Field.apm(iArr, iArr2, create, create2);
        X25519Field.sqr(create, create);
        X25519Field.sqr(create2, create2);
        X25519Field.mul(create, create2, iArr);
        X25519Field.sub(create, create2, create);
        X25519Field.mul(create, (int) C_A24, iArr2);
        X25519Field.add(iArr2, create2, iArr2);
        X25519Field.mul(iArr2, create, iArr2);
    }

    public static synchronized void precompute() {
        synchronized (X25519.class) {
            if (precompBase != null) {
                return;
            }
            precompBase = new int[2520];
            int[] iArr = precompBase;
            int[] iArr2 = new int[2510];
            int[] create = X25519Field.create();
            create[0] = 9;
            int[] create2 = X25519Field.create();
            create2[0] = 1;
            int[] create3 = X25519Field.create();
            int[] create4 = X25519Field.create();
            X25519Field.apm(create, create2, create3, create4);
            int[] create5 = X25519Field.create();
            X25519Field.copy(create4, 0, create5, 0);
            int i = 0;
            while (true) {
                X25519Field.copy(create3, 0, iArr, i);
                if (i == 2510) {
                    break;
                }
                pointDouble(create, create2);
                X25519Field.apm(create, create2, create3, create4);
                X25519Field.mul(create3, create5, create3);
                X25519Field.mul(create5, create4, create5);
                X25519Field.copy(create4, 0, iArr2, i);
                i += 10;
            }
            int[] create6 = X25519Field.create();
            X25519Field.inv(create5, create6);
            while (true) {
                X25519Field.copy(iArr, i, create, 0);
                X25519Field.mul(create, create6, create);
                X25519Field.copy(create, 0, precompBase, i);
                if (i == 0) {
                    return;
                }
                i -= 10;
                X25519Field.copy(iArr2, i, create2, 0);
                X25519Field.mul(create6, create2, create6);
            }
        }
    }

    public static void scalarMult(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) {
        r0 = new int[8];
        decodeScalar(bArr, i, r0);
        int[] create = X25519Field.create();
        X25519Field.decode(bArr2, i2, create);
        int[] create2 = X25519Field.create();
        int i4 = 0;
        X25519Field.copy(create, 0, create2, 0);
        int[] create3 = X25519Field.create();
        create3[0] = 1;
        int[] create4 = X25519Field.create();
        create4[0] = 1;
        int[] create5 = X25519Field.create();
        int[] create6 = X25519Field.create();
        int[] create7 = X25519Field.create();
        int i5 = 254;
        int i6 = 1;
        while (true) {
            X25519Field.apm(create4, create5, create6, create4);
            X25519Field.apm(create2, create3, create5, create2);
            X25519Field.mul(create6, create2, create6);
            X25519Field.mul(create4, create5, create4);
            X25519Field.sqr(create5, create5);
            X25519Field.sqr(create2, create2);
            X25519Field.sub(create5, create2, create7);
            X25519Field.mul(create7, (int) C_A24, create3);
            X25519Field.add(create3, create2, create3);
            X25519Field.mul(create3, create7, create3);
            X25519Field.mul(create2, create5, create2);
            X25519Field.apm(create6, create4, create4, create5);
            X25519Field.sqr(create4, create4);
            X25519Field.sqr(create5, create5);
            X25519Field.mul(create5, create, create5);
            i5--;
            int i7 = (r0[i5 >>> 5] >>> (i5 & 31)) & 1;
            i6 ^= i7;
            X25519Field.cswap(i6, create2, create4);
            X25519Field.cswap(i6, create3, create5);
            if (i5 < 3) {
                break;
            }
            i6 = i7;
        }
        while (i4 < 3) {
            pointDouble(create2, create3);
            i4++;
        }
        X25519Field.inv(create3, create3);
        X25519Field.mul(create2, create3, create2);
        X25519Field.normalize(create2);
        X25519Field.encode(create2, bArr3, i3);
    }

    public static void scalarMultBase(byte[] bArr, int i, byte[] bArr2, int i2) {
        precompute();
        int[] iArr = new int[8];
        decodeScalar(bArr, i, iArr);
        int[] create = X25519Field.create();
        int[] create2 = X25519Field.create();
        int i3 = 0;
        create2[0] = 1;
        int[] create3 = X25519Field.create();
        create3[0] = 1;
        int[] create4 = X25519Field.create();
        X25519Field.copy(PsubS_x, 0, create4, 0);
        int[] create5 = X25519Field.create();
        create5[0] = 1;
        int i4 = 1;
        int i5 = 0;
        int i6 = 3;
        while (true) {
            X25519Field.copy(precompBase, i5, create, 0);
            i5 += 10;
            int i7 = (iArr[i6 >>> 5] >>> (i6 & 31)) & 1;
            i4 ^= i7;
            X25519Field.cswap(i4, create2, create4);
            X25519Field.cswap(i4, create3, create5);
            X25519Field.apm(create2, create3, create2, create3);
            X25519Field.mul(create, create3, create);
            X25519Field.carry(create2);
            X25519Field.apm(create2, create, create2, create3);
            X25519Field.sqr(create2, create2);
            X25519Field.sqr(create3, create3);
            X25519Field.mul(create5, create2, create2);
            X25519Field.mul(create4, create3, create3);
            i6++;
            if (i6 >= 255) {
                break;
            }
            i4 = i7;
        }
        while (i3 < 3) {
            pointDouble(create2, create3);
            i3++;
        }
        X25519Field.inv(create3, create3);
        X25519Field.mul(create2, create3, create2);
        X25519Field.normalize(create2);
        X25519Field.encode(create2, bArr2, i2);
    }
}
