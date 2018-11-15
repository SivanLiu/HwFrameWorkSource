package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;

public class GOST3412_2015Engine implements BlockCipher {
    protected static final int BLOCK_SIZE = 16;
    private static final byte[] PI = new byte[]{(byte) -4, (byte) -18, (byte) -35, (byte) 17, (byte) -49, (byte) 110, (byte) 49, (byte) 22, (byte) -5, (byte) -60, (byte) -6, (byte) -38, (byte) 35, (byte) -59, (byte) 4, (byte) 77, (byte) -23, (byte) 119, (byte) -16, (byte) -37, (byte) -109, (byte) 46, (byte) -103, (byte) -70, (byte) 23, (byte) 54, (byte) -15, (byte) -69, (byte) 20, (byte) -51, (byte) 95, (byte) -63, (byte) -7, (byte) 24, (byte) 101, (byte) 90, (byte) -30, (byte) 92, (byte) -17, (byte) 33, (byte) -127, (byte) 28, (byte) 60, (byte) 66, (byte) -117, (byte) 1, (byte) -114, (byte) 79, (byte) 5, (byte) -124, (byte) 2, (byte) -82, (byte) -29, (byte) 106, (byte) -113, (byte) -96, (byte) 6, (byte) 11, (byte) -19, (byte) -104, Byte.MAX_VALUE, (byte) -44, (byte) -45, (byte) 31, (byte) -21, (byte) 52, (byte) 44, (byte) 81, (byte) -22, (byte) -56, (byte) 72, (byte) -85, (byte) -14, (byte) 42, (byte) 104, (byte) -94, (byte) -3, (byte) 58, (byte) -50, (byte) -52, (byte) -75, (byte) 112, (byte) 14, (byte) 86, (byte) 8, (byte) 12, (byte) 118, (byte) 18, (byte) -65, (byte) 114, (byte) 19, (byte) 71, (byte) -100, (byte) -73, (byte) 93, (byte) -121, (byte) 21, (byte) -95, (byte) -106, (byte) 41, Tnaf.POW_2_WIDTH, (byte) 123, (byte) -102, (byte) -57, (byte) -13, (byte) -111, (byte) 120, (byte) 111, (byte) -99, (byte) -98, (byte) -78, (byte) -79, (byte) 50, (byte) 117, (byte) 25, (byte) 61, (byte) -1, (byte) 53, (byte) -118, (byte) 126, (byte) 109, (byte) 84, (byte) -58, Byte.MIN_VALUE, (byte) -61, (byte) -67, (byte) 13, (byte) 87, (byte) -33, (byte) -11, (byte) 36, (byte) -87, (byte) 62, (byte) -88, (byte) 67, (byte) -55, (byte) -41, (byte) 121, (byte) -42, (byte) -10, (byte) 124, (byte) 34, (byte) -71, (byte) 3, (byte) -32, (byte) 15, (byte) -20, (byte) -34, (byte) 122, (byte) -108, (byte) -80, PSSSigner.TRAILER_IMPLICIT, (byte) -36, (byte) -24, (byte) 40, (byte) 80, (byte) 78, (byte) 51, (byte) 10, (byte) 74, (byte) -89, (byte) -105, (byte) 96, (byte) 115, (byte) 30, (byte) 0, (byte) 98, (byte) 68, (byte) 26, (byte) -72, (byte) 56, (byte) -126, (byte) 100, (byte) -97, (byte) 38, (byte) 65, (byte) -83, (byte) 69, (byte) 70, (byte) -110, (byte) 39, (byte) 94, (byte) 85, (byte) 47, (byte) -116, (byte) -93, (byte) -91, (byte) 125, (byte) 105, (byte) -43, (byte) -107, (byte) 59, (byte) 7, (byte) 88, (byte) -77, (byte) 64, (byte) -122, (byte) -84, (byte) 29, (byte) -9, (byte) 48, (byte) 55, (byte) 107, (byte) -28, (byte) -120, (byte) -39, (byte) -25, (byte) -119, (byte) -31, (byte) 27, (byte) -125, (byte) 73, (byte) 76, (byte) 63, (byte) -8, (byte) -2, (byte) -115, (byte) 83, (byte) -86, (byte) -112, (byte) -54, (byte) -40, (byte) -123, (byte) 97, (byte) 32, (byte) 113, (byte) 103, (byte) -92, (byte) 45, (byte) 43, (byte) 9, (byte) 91, (byte) -53, (byte) -101, (byte) 37, (byte) -48, (byte) -66, (byte) -27, (byte) 108, (byte) 82, (byte) 89, (byte) -90, (byte) 116, (byte) -46, (byte) -26, (byte) -12, (byte) -76, (byte) -64, (byte) -47, (byte) 102, (byte) -81, (byte) -62, (byte) 57, (byte) 75, (byte) 99, (byte) -74};
    private static final byte[] inversePI = new byte[]{(byte) -91, (byte) 45, (byte) 50, (byte) -113, (byte) 14, (byte) 48, (byte) 56, (byte) -64, (byte) 84, (byte) -26, (byte) -98, (byte) 57, (byte) 85, (byte) 126, (byte) 82, (byte) -111, (byte) 100, (byte) 3, (byte) 87, (byte) 90, (byte) 28, (byte) 96, (byte) 7, (byte) 24, (byte) 33, (byte) 114, (byte) -88, (byte) -47, (byte) 41, (byte) -58, (byte) -92, (byte) 63, (byte) -32, (byte) 39, (byte) -115, (byte) 12, (byte) -126, (byte) -22, (byte) -82, (byte) -76, (byte) -102, (byte) 99, (byte) 73, (byte) -27, (byte) 66, (byte) -28, (byte) 21, (byte) -73, (byte) -56, (byte) 6, (byte) 112, (byte) -99, (byte) 65, (byte) 117, (byte) 25, (byte) -55, (byte) -86, (byte) -4, (byte) 77, (byte) -65, (byte) 42, (byte) 115, (byte) -124, (byte) -43, (byte) -61, (byte) -81, (byte) 43, (byte) -122, (byte) -89, (byte) -79, (byte) -78, (byte) 91, (byte) 70, (byte) -45, (byte) -97, (byte) -3, (byte) -44, (byte) 15, (byte) -100, (byte) 47, (byte) -101, (byte) 67, (byte) -17, (byte) -39, (byte) 121, (byte) -74, (byte) 83, Byte.MAX_VALUE, (byte) -63, (byte) -16, (byte) 35, (byte) -25, (byte) 37, (byte) 94, (byte) -75, (byte) 30, (byte) -94, (byte) -33, (byte) -90, (byte) -2, (byte) -84, (byte) 34, (byte) -7, (byte) -30, (byte) 74, PSSSigner.TRAILER_IMPLICIT, (byte) 53, (byte) -54, (byte) -18, (byte) 120, (byte) 5, (byte) 107, (byte) 81, (byte) -31, (byte) 89, (byte) -93, (byte) -14, (byte) 113, (byte) 86, (byte) 17, (byte) 106, (byte) -119, (byte) -108, (byte) 101, (byte) -116, (byte) -69, (byte) 119, (byte) 60, (byte) 123, (byte) 40, (byte) -85, (byte) -46, (byte) 49, (byte) -34, (byte) -60, (byte) 95, (byte) -52, (byte) -49, (byte) 118, (byte) 44, (byte) -72, (byte) -40, (byte) 46, (byte) 54, (byte) -37, (byte) 105, (byte) -77, (byte) 20, (byte) -107, (byte) -66, (byte) 98, (byte) -95, (byte) 59, (byte) 22, (byte) 102, (byte) -23, (byte) 92, (byte) 108, (byte) 109, (byte) -83, (byte) 55, (byte) 97, (byte) 75, (byte) -71, (byte) -29, (byte) -70, (byte) -15, (byte) -96, (byte) -123, (byte) -125, (byte) -38, (byte) 71, (byte) -59, (byte) -80, (byte) 51, (byte) -6, (byte) -106, (byte) 111, (byte) 110, (byte) -62, (byte) -10, (byte) 80, (byte) -1, (byte) 93, (byte) -87, (byte) -114, (byte) 23, (byte) 27, (byte) -105, (byte) 125, (byte) -20, (byte) 88, (byte) -9, (byte) 31, (byte) -5, (byte) 124, (byte) 9, (byte) 13, (byte) 122, (byte) 103, (byte) 69, (byte) -121, (byte) -36, (byte) -24, (byte) 79, (byte) 29, (byte) 78, (byte) 4, (byte) -21, (byte) -8, (byte) -13, (byte) 62, (byte) 61, (byte) -67, (byte) -118, (byte) -120, (byte) -35, (byte) -51, (byte) 11, (byte) 19, (byte) -104, (byte) 2, (byte) -109, Byte.MIN_VALUE, (byte) -112, (byte) -48, (byte) 36, (byte) 52, (byte) -53, (byte) -19, (byte) -12, (byte) -50, (byte) -103, Tnaf.POW_2_WIDTH, (byte) 68, (byte) 64, (byte) -110, (byte) 58, (byte) 1, (byte) 38, (byte) 18, (byte) 26, (byte) 72, (byte) 104, (byte) -11, (byte) -127, (byte) -117, (byte) -57, (byte) -42, (byte) 32, (byte) 10, (byte) 8, (byte) 0, (byte) 76, (byte) -41, (byte) 116};
    private int KEY_LENGTH = 32;
    private int SUB_LENGTH = (this.KEY_LENGTH / 2);
    private byte[][] _gf_mul = init_gf256_mul_table();
    private boolean forEncryption;
    private final byte[] lFactors = new byte[]{(byte) -108, (byte) 32, (byte) -123, Tnaf.POW_2_WIDTH, (byte) -62, (byte) -64, (byte) 1, (byte) -5, (byte) 1, (byte) -64, (byte) -62, Tnaf.POW_2_WIDTH, (byte) -123, (byte) 32, (byte) -108, (byte) 1};
    private byte[][] subKeys = ((byte[][]) null);

    private void C(byte[] bArr, int i) {
        Arrays.clear(bArr);
        bArr[15] = (byte) i;
        L(bArr);
    }

    private void F(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        Object LSX = LSX(bArr, bArr2);
        X(LSX, bArr3);
        System.arraycopy(bArr2, 0, bArr3, 0, this.SUB_LENGTH);
        System.arraycopy(LSX, 0, bArr2, 0, this.SUB_LENGTH);
    }

    private void GOST3412_2015Func(byte[] bArr, int i, byte[] bArr2, int i2) {
        Object obj = new byte[16];
        System.arraycopy(bArr, i, obj, 0, 16);
        i = 9;
        if (this.forEncryption) {
            for (int i3 = 0; i3 < 9; i3++) {
                obj = Arrays.copyOf(LSX(this.subKeys[i3], obj), 16);
            }
            bArr = this.subKeys[9];
        } else {
            while (i > 0) {
                obj = Arrays.copyOf(XSL(this.subKeys[i], obj), 16);
                i--;
            }
            bArr = this.subKeys[0];
        }
        X(obj, bArr);
        System.arraycopy(obj, 0, bArr2, i2, 16);
    }

    private void L(byte[] bArr) {
        for (int i = 0; i < 16; i++) {
            R(bArr);
        }
    }

    private byte[] LSX(byte[] bArr, byte[] bArr2) {
        bArr = Arrays.copyOf(bArr, bArr.length);
        X(bArr, bArr2);
        S(bArr);
        L(bArr);
        return bArr;
    }

    private void R(byte[] bArr) {
        byte l = l(bArr);
        System.arraycopy(bArr, 0, bArr, 1, 15);
        bArr[0] = l;
    }

    private void S(byte[] bArr) {
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = PI[unsignedByte(bArr[i])];
        }
    }

    private void X(byte[] bArr, byte[] bArr2) {
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
        }
    }

    private byte[] XSL(byte[] bArr, byte[] bArr2) {
        bArr = Arrays.copyOf(bArr, bArr.length);
        X(bArr, bArr2);
        inverseL(bArr);
        inverseS(bArr);
        return bArr;
    }

    private void generateSubKeys(byte[] bArr) {
        if (bArr.length == this.KEY_LENGTH) {
            int i;
            this.subKeys = new byte[10][];
            for (int i2 = 0; i2 < 10; i2++) {
                this.subKeys[i2] = new byte[this.SUB_LENGTH];
            }
            Object obj = new byte[this.SUB_LENGTH];
            Object obj2 = new byte[this.SUB_LENGTH];
            for (i = 0; i < this.SUB_LENGTH; i++) {
                byte[] bArr2 = this.subKeys[0];
                byte b = bArr[i];
                obj[i] = b;
                bArr2[i] = b;
                bArr2 = this.subKeys[1];
                byte b2 = bArr[this.SUB_LENGTH + i];
                obj2[i] = b2;
                bArr2[i] = b2;
            }
            bArr = new byte[this.SUB_LENGTH];
            for (i = 1; i < 5; i++) {
                for (int i3 = 1; i3 <= 8; i3++) {
                    C(bArr, (8 * (i - 1)) + i3);
                    F(bArr, obj, obj2);
                }
                int i4 = 2 * i;
                System.arraycopy(obj, 0, this.subKeys[i4], 0, this.SUB_LENGTH);
                System.arraycopy(obj2, 0, this.subKeys[i4 + 1], 0, this.SUB_LENGTH);
            }
            return;
        }
        throw new IllegalArgumentException("Key length invalid. Key needs to be 32 byte - 256 bit!!!");
    }

    private static byte[][] init_gf256_mul_table() {
        byte[][] bArr = new byte[256][];
        for (int i = 0; i < 256; i++) {
            bArr[i] = new byte[256];
            for (int i2 = 0; i2 < 256; i2++) {
                bArr[i][i2] = kuz_mul_gf256_slow((byte) i, (byte) i2);
            }
        }
        return bArr;
    }

    private void inverseL(byte[] bArr) {
        for (int i = 0; i < 16; i++) {
            inverseR(bArr);
        }
    }

    private void inverseR(byte[] bArr) {
        Object obj = new byte[16];
        System.arraycopy(bArr, 1, obj, 0, 15);
        obj[15] = bArr[0];
        byte l = l(obj);
        System.arraycopy(bArr, 1, bArr, 0, 15);
        bArr[15] = l;
    }

    private void inverseS(byte[] bArr) {
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = inversePI[unsignedByte(bArr[i])];
        }
    }

    private static byte kuz_mul_gf256_slow(byte b, byte b2) {
        int i = 0;
        byte b3 = (byte) 0;
        while (i < 8 && b != 0 && b2 != 0) {
            if ((b2 & 1) != 0) {
                b3 = (byte) (b3 ^ b);
            }
            byte b4 = (byte) (b & 128);
            int b5 = (byte) (b5 << 1);
            if (b4 != (byte) 0) {
                b5 = (byte) (b5 ^ CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256);
            }
            int b22 = (byte) (b22 >> 1);
            i = (byte) (i + 1);
        }
        return b3;
    }

    private byte l(byte[] bArr) {
        byte b = bArr[15];
        for (int i = 14; i >= 0; i--) {
            b = (byte) (b ^ this._gf_mul[unsignedByte(bArr[i])][unsignedByte(this.lFactors[i])]);
        }
        return b;
    }

    private int unsignedByte(byte b) {
        return b & 255;
    }

    public String getAlgorithmName() {
        return "GOST3412_2015";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof KeyParameter) {
            this.forEncryption = z;
            generateSubKeys(((KeyParameter) cipherParameters).getKey());
        } else if (cipherParameters != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid parameter passed to GOST3412_2015 init - ");
            stringBuilder.append(cipherParameters.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.subKeys == null) {
            throw new IllegalStateException("GOST3412_2015 engine not initialised");
        } else if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 16 <= bArr2.length) {
            GOST3412_2015Func(bArr, i, bArr2, i2);
            return 16;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
