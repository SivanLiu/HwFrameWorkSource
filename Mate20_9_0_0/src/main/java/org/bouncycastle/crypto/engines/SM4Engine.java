package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.util.Pack;

public class SM4Engine implements BlockCipher {
    private static final int BLOCK_SIZE = 16;
    private static final int[] CK = new int[]{462357, 472066609, 943670861, 1415275113, 1886879365, -1936483679, -1464879427, -993275175, -521670923, -66909679, 404694573, 876298825, 1347903077, 1819507329, -2003855715, -1532251463, -1060647211, -589042959, -117504499, 337322537, 808926789, 1280531041, 1752135293, -2071227751, -1599623499, -1128019247, -656414995, -184876535, 269950501, 741554753, 1213159005, 1684763257};
    private static final int[] FK = new int[]{-1548633402, 1453994832, 1736282519, -1301273892};
    private static final byte[] Sbox = new byte[]{(byte) -42, (byte) -112, (byte) -23, (byte) -2, (byte) -52, (byte) -31, (byte) 61, (byte) -73, (byte) 22, (byte) -74, (byte) 20, (byte) -62, (byte) 40, (byte) -5, (byte) 44, (byte) 5, (byte) 43, (byte) 103, (byte) -102, (byte) 118, (byte) 42, (byte) -66, (byte) 4, (byte) -61, (byte) -86, (byte) 68, (byte) 19, (byte) 38, (byte) 73, (byte) -122, (byte) 6, (byte) -103, (byte) -100, (byte) 66, (byte) 80, (byte) -12, (byte) -111, (byte) -17, (byte) -104, (byte) 122, (byte) 51, (byte) 84, (byte) 11, (byte) 67, (byte) -19, (byte) -49, (byte) -84, (byte) 98, (byte) -28, (byte) -77, (byte) 28, (byte) -87, (byte) -55, (byte) 8, (byte) -24, (byte) -107, Byte.MIN_VALUE, (byte) -33, (byte) -108, (byte) -6, (byte) 117, (byte) -113, (byte) 63, (byte) -90, (byte) 71, (byte) 7, (byte) -89, (byte) -4, (byte) -13, (byte) 115, (byte) 23, (byte) -70, (byte) -125, (byte) 89, (byte) 60, (byte) 25, (byte) -26, (byte) -123, (byte) 79, (byte) -88, (byte) 104, (byte) 107, (byte) -127, (byte) -78, (byte) 113, (byte) 100, (byte) -38, (byte) -117, (byte) -8, (byte) -21, (byte) 15, (byte) 75, (byte) 112, (byte) 86, (byte) -99, (byte) 53, (byte) 30, (byte) 36, (byte) 14, (byte) 94, (byte) 99, (byte) 88, (byte) -47, (byte) -94, (byte) 37, (byte) 34, (byte) 124, (byte) 59, (byte) 1, (byte) 33, (byte) 120, (byte) -121, (byte) -44, (byte) 0, (byte) 70, (byte) 87, (byte) -97, (byte) -45, (byte) 39, (byte) 82, (byte) 76, (byte) 54, (byte) 2, (byte) -25, (byte) -96, (byte) -60, (byte) -56, (byte) -98, (byte) -22, (byte) -65, (byte) -118, (byte) -46, (byte) 64, (byte) -57, (byte) 56, (byte) -75, (byte) -93, (byte) -9, (byte) -14, (byte) -50, (byte) -7, (byte) 97, (byte) 21, (byte) -95, (byte) -32, (byte) -82, (byte) 93, (byte) -92, (byte) -101, (byte) 52, (byte) 26, (byte) 85, (byte) -83, (byte) -109, (byte) 50, (byte) 48, (byte) -11, (byte) -116, (byte) -79, (byte) -29, (byte) 29, (byte) -10, (byte) -30, (byte) 46, (byte) -126, (byte) 102, (byte) -54, (byte) 96, (byte) -64, (byte) 41, (byte) 35, (byte) -85, (byte) 13, (byte) 83, (byte) 78, (byte) 111, (byte) -43, (byte) -37, (byte) 55, (byte) 69, (byte) -34, (byte) -3, (byte) -114, (byte) 47, (byte) 3, (byte) -1, (byte) 106, (byte) 114, (byte) 109, (byte) 108, (byte) 91, (byte) 81, (byte) -115, (byte) 27, (byte) -81, (byte) -110, (byte) -69, (byte) -35, PSSSigner.TRAILER_IMPLICIT, Byte.MAX_VALUE, (byte) 17, (byte) -39, (byte) 92, (byte) 65, (byte) 31, Tnaf.POW_2_WIDTH, (byte) 90, (byte) -40, (byte) 10, (byte) -63, (byte) 49, (byte) -120, (byte) -91, (byte) -51, (byte) 123, (byte) -67, (byte) 45, (byte) 116, (byte) -48, (byte) 18, (byte) -72, (byte) -27, (byte) -76, (byte) -80, (byte) -119, (byte) 105, (byte) -105, (byte) 74, (byte) 12, (byte) -106, (byte) 119, (byte) 126, (byte) 101, (byte) -71, (byte) -15, (byte) 9, (byte) -59, (byte) 110, (byte) -58, (byte) -124, (byte) 24, (byte) -16, (byte) 125, (byte) -20, (byte) 58, (byte) -36, (byte) 77, (byte) 32, (byte) 121, (byte) -18, (byte) 95, (byte) 62, (byte) -41, (byte) -53, (byte) 57, (byte) 72};
    private final int[] X = new int[4];
    private int[] rk;

    private int F0(int[] iArr, int i) {
        return T((iArr[3] ^ (iArr[1] ^ iArr[2])) ^ i) ^ iArr[0];
    }

    private int F1(int[] iArr, int i) {
        return T((iArr[0] ^ (iArr[2] ^ iArr[3])) ^ i) ^ iArr[1];
    }

    private int F2(int[] iArr, int i) {
        return T((iArr[1] ^ (iArr[3] ^ iArr[0])) ^ i) ^ iArr[2];
    }

    private int F3(int[] iArr, int i) {
        return T((iArr[2] ^ (iArr[0] ^ iArr[1])) ^ i) ^ iArr[3];
    }

    private int L(int i) {
        return rotateLeft(i, 24) ^ (((rotateLeft(i, 2) ^ i) ^ rotateLeft(i, 10)) ^ rotateLeft(i, 18));
    }

    private int L_ap(int i) {
        return rotateLeft(i, 23) ^ (rotateLeft(i, 13) ^ i);
    }

    private void R(int[] iArr, int i) {
        int i2 = i + 1;
        int i3 = i + 2;
        int i4 = i + 3;
        iArr[i] = iArr[i] ^ iArr[i4];
        iArr[i4] = iArr[i] ^ iArr[i4];
        iArr[i] = iArr[i4] ^ iArr[i];
        iArr[i2] = iArr[i2] ^ iArr[i3];
        iArr[i3] = iArr[i2] ^ iArr[i3];
        iArr[i2] = iArr[i2] ^ iArr[i3];
    }

    private int T(int i) {
        return L(tau(i));
    }

    private int T_ap(int i) {
        return L_ap(tau(i));
    }

    private int[] expandKey(boolean z, byte[] bArr) {
        int[] iArr = new int[32];
        int i = 4;
        int[] iArr2 = new int[]{Pack.bigEndianToInt(bArr, 0), Pack.bigEndianToInt(bArr, 4), Pack.bigEndianToInt(bArr, 8), Pack.bigEndianToInt(bArr, 12)};
        int[] iArr3 = new int[]{iArr2[0] ^ FK[0], iArr2[1] ^ FK[1], iArr2[2] ^ FK[2], iArr2[3] ^ FK[3]};
        if (z) {
            iArr[0] = iArr3[0] ^ T_ap(((iArr3[1] ^ iArr3[2]) ^ iArr3[3]) ^ CK[0]);
            iArr[1] = iArr3[1] ^ T_ap(((iArr3[2] ^ iArr3[3]) ^ iArr[0]) ^ CK[1]);
            iArr[2] = iArr3[2] ^ T_ap(((iArr3[3] ^ iArr[0]) ^ iArr[1]) ^ CK[2]);
            iArr[3] = iArr3[3] ^ T_ap(((iArr[0] ^ iArr[1]) ^ iArr[2]) ^ CK[3]);
            while (i < 32) {
                iArr[i] = iArr[i - 4] ^ T_ap(((iArr[i - 3] ^ iArr[i - 2]) ^ iArr[i - 1]) ^ CK[i]);
                i++;
            }
        } else {
            iArr[31] = iArr3[0] ^ T_ap(((iArr3[1] ^ iArr3[2]) ^ iArr3[3]) ^ CK[0]);
            iArr[30] = iArr3[1] ^ T_ap(((iArr3[2] ^ iArr3[3]) ^ iArr[31]) ^ CK[1]);
            iArr[29] = iArr3[2] ^ T_ap(((iArr3[3] ^ iArr[31]) ^ iArr[30]) ^ CK[2]);
            iArr[28] = iArr3[3] ^ T_ap(((iArr[30] ^ iArr[31]) ^ iArr[29]) ^ CK[3]);
            for (int i2 = 27; i2 >= 0; i2--) {
                iArr[i2] = iArr[i2 + 4] ^ T_ap(((iArr[i2 + 3] ^ iArr[i2 + 2]) ^ iArr[i2 + 1]) ^ CK[31 - i2]);
            }
        }
        return iArr;
    }

    private int rotateLeft(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    private int tau(int i) {
        return (Sbox[i & 255] & 255) | ((((Sbox[(i >> 24) & 255] & 255) << 24) | ((Sbox[(i >> 16) & 255] & 255) << 16)) | ((Sbox[(i >> 8) & 255] & 255) << 8));
    }

    public String getAlgorithmName() {
        return "SM4";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof KeyParameter) {
            byte[] key = ((KeyParameter) cipherParameters).getKey();
            if (key.length == 16) {
                this.rk = expandKey(z, key);
                return;
            }
            throw new IllegalArgumentException("SM4 requires a 128 bit key");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to SM4 init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.rk == null) {
            throw new IllegalStateException("SM4 not initialised");
        } else if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 16 <= bArr2.length) {
            this.X[0] = Pack.bigEndianToInt(bArr, i);
            this.X[1] = Pack.bigEndianToInt(bArr, i + 4);
            this.X[2] = Pack.bigEndianToInt(bArr, i + 8);
            this.X[3] = Pack.bigEndianToInt(bArr, i + 12);
            for (int i3 = 0; i3 < 32; i3 += 4) {
                this.X[0] = F0(this.X, this.rk[i3]);
                this.X[1] = F1(this.X, this.rk[i3 + 1]);
                this.X[2] = F2(this.X, this.rk[i3 + 2]);
                this.X[3] = F3(this.X, this.rk[i3 + 3]);
            }
            R(this.X, 0);
            Pack.intToBigEndian(this.X[0], bArr2, i2);
            Pack.intToBigEndian(this.X[1], bArr2, i2 + 4);
            Pack.intToBigEndian(this.X[2], bArr2, i2 + 8);
            Pack.intToBigEndian(this.X[3], bArr2, i2 + 12);
            return 16;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
