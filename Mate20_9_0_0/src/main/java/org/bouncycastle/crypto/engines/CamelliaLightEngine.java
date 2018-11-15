package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;

public class CamelliaLightEngine implements BlockCipher {
    private static final int BLOCK_SIZE = 16;
    private static final int MASK8 = 255;
    private static final byte[] SBOX1 = new byte[]{(byte) 112, (byte) -126, (byte) 44, (byte) -20, (byte) -77, (byte) 39, (byte) -64, (byte) -27, (byte) -28, (byte) -123, (byte) 87, (byte) 53, (byte) -22, (byte) 12, (byte) -82, (byte) 65, (byte) 35, (byte) -17, (byte) 107, (byte) -109, (byte) 69, (byte) 25, (byte) -91, (byte) 33, (byte) -19, (byte) 14, (byte) 79, (byte) 78, (byte) 29, (byte) 101, (byte) -110, (byte) -67, (byte) -122, (byte) -72, (byte) -81, (byte) -113, (byte) 124, (byte) -21, (byte) 31, (byte) -50, (byte) 62, (byte) 48, (byte) -36, (byte) 95, (byte) 94, (byte) -59, (byte) 11, (byte) 26, (byte) -90, (byte) -31, (byte) 57, (byte) -54, (byte) -43, (byte) 71, (byte) 93, (byte) 61, (byte) -39, (byte) 1, (byte) 90, (byte) -42, (byte) 81, (byte) 86, (byte) 108, (byte) 77, (byte) -117, (byte) 13, (byte) -102, (byte) 102, (byte) -5, (byte) -52, (byte) -80, (byte) 45, (byte) 116, (byte) 18, (byte) 43, (byte) 32, (byte) -16, (byte) -79, (byte) -124, (byte) -103, (byte) -33, (byte) 76, (byte) -53, (byte) -62, (byte) 52, (byte) 126, (byte) 118, (byte) 5, (byte) 109, (byte) -73, (byte) -87, (byte) 49, (byte) -47, (byte) 23, (byte) 4, (byte) -41, (byte) 20, (byte) 88, (byte) 58, (byte) 97, (byte) -34, (byte) 27, (byte) 17, (byte) 28, (byte) 50, (byte) 15, (byte) -100, (byte) 22, (byte) 83, (byte) 24, (byte) -14, (byte) 34, (byte) -2, (byte) 68, (byte) -49, (byte) -78, (byte) -61, (byte) -75, (byte) 122, (byte) -111, (byte) 36, (byte) 8, (byte) -24, (byte) -88, (byte) 96, (byte) -4, (byte) 105, (byte) 80, (byte) -86, (byte) -48, (byte) -96, (byte) 125, (byte) -95, (byte) -119, (byte) 98, (byte) -105, (byte) 84, (byte) 91, (byte) 30, (byte) -107, (byte) -32, (byte) -1, (byte) 100, (byte) -46, Tnaf.POW_2_WIDTH, (byte) -60, (byte) 0, (byte) 72, (byte) -93, (byte) -9, (byte) 117, (byte) -37, (byte) -118, (byte) 3, (byte) -26, (byte) -38, (byte) 9, (byte) 63, (byte) -35, (byte) -108, (byte) -121, (byte) 92, (byte) -125, (byte) 2, (byte) -51, (byte) 74, (byte) -112, (byte) 51, (byte) 115, (byte) 103, (byte) -10, (byte) -13, (byte) -99, Byte.MAX_VALUE, (byte) -65, (byte) -30, (byte) 82, (byte) -101, (byte) -40, (byte) 38, (byte) -56, (byte) 55, (byte) -58, (byte) 59, (byte) -127, (byte) -106, (byte) 111, (byte) 75, (byte) 19, (byte) -66, (byte) 99, (byte) 46, (byte) -23, (byte) 121, (byte) -89, (byte) -116, (byte) -97, (byte) 110, PSSSigner.TRAILER_IMPLICIT, (byte) -114, (byte) 41, (byte) -11, (byte) -7, (byte) -74, (byte) 47, (byte) -3, (byte) -76, (byte) 89, (byte) 120, (byte) -104, (byte) 6, (byte) 106, (byte) -25, (byte) 70, (byte) 113, (byte) -70, (byte) -44, (byte) 37, (byte) -85, (byte) 66, (byte) -120, (byte) -94, (byte) -115, (byte) -6, (byte) 114, (byte) 7, (byte) -71, (byte) 85, (byte) -8, (byte) -18, (byte) -84, (byte) 10, (byte) 54, (byte) 73, (byte) 42, (byte) 104, (byte) 60, (byte) 56, (byte) -15, (byte) -92, (byte) 64, (byte) 40, (byte) -45, (byte) 123, (byte) -69, (byte) -55, (byte) 67, (byte) -63, (byte) 21, (byte) -29, (byte) -83, (byte) -12, (byte) 119, (byte) -57, Byte.MIN_VALUE, (byte) -98};
    private static final int[] SIGMA = new int[]{-1600231809, 1003262091, -1233459112, 1286239154, -957401297, -380665154, 1426019237, -237801700, 283453434, -563598051, -1336506174, -1276722691};
    private boolean _keyis128;
    private boolean initialized;
    private int[] ke = new int[12];
    private int[] kw = new int[8];
    private int[] state = new int[4];
    private int[] subkey = new int[96];

    private int bytes2int(byte[] bArr, int i) {
        int i2 = 0;
        int i3 = 0;
        while (i2 < 4) {
            i3 = (i3 << 8) + (bArr[i2 + i] & 255);
            i2++;
        }
        return i3;
    }

    private void camelliaF2(int[] iArr, int[] iArr2, int i) {
        int i2 = iArr[0] ^ iArr2[0 + i];
        i2 = ((SBOX1[(i2 >>> 24) & 255] & 255) << 24) | ((sbox4(i2 & 255) | (sbox3((i2 >>> 8) & 255) << 8)) | (sbox2((i2 >>> 16) & 255) << 16));
        int i3 = iArr[1] ^ iArr2[1 + i];
        i3 = leftRotate((sbox2((i3 >>> 24) & 255) << 24) | (((SBOX1[i3 & 255] & 255) | (sbox4((i3 >>> 8) & 255) << 8)) | (sbox3((i3 >>> 16) & 255) << 16)), 8);
        i2 ^= i3;
        i3 = leftRotate(i3, 8) ^ i2;
        i2 = rightRotate(i2, 8) ^ i3;
        iArr[2] = (leftRotate(i3, 16) ^ i2) ^ iArr[2];
        iArr[3] = leftRotate(i2, 8) ^ iArr[3];
        i2 = iArr[2] ^ iArr2[2 + i];
        i2 = ((SBOX1[(i2 >>> 24) & 255] & 255) << 24) | ((sbox4(i2 & 255) | (sbox3((i2 >>> 8) & 255) << 8)) | (sbox2((i2 >>> 16) & 255) << 16));
        int i4 = iArr2[3 + i] ^ iArr[3];
        i4 = leftRotate((sbox2((i4 >>> 24) & 255) << 24) | (((SBOX1[i4 & 255] & 255) | (sbox4((i4 >>> 8) & 255) << 8)) | (sbox3((i4 >>> 16) & 255) << 16)), 8);
        i = i2 ^ i4;
        i4 = leftRotate(i4, 8) ^ i;
        i = rightRotate(i, 8) ^ i4;
        iArr[0] = (leftRotate(i4, 16) ^ i) ^ iArr[0];
        iArr[1] = iArr[1] ^ leftRotate(i, 8);
    }

    private void camelliaFLs(int[] iArr, int[] iArr2, int i) {
        iArr[1] = iArr[1] ^ leftRotate(iArr[0] & iArr2[0 + i], 1);
        iArr[0] = iArr[0] ^ (iArr2[1 + i] | iArr[1]);
        iArr[2] = iArr[2] ^ (iArr2[3 + i] | iArr[3]);
        iArr[3] = leftRotate(iArr2[i + 2] & iArr[2], 1) ^ iArr[3];
    }

    private static void decroldq(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        int i4 = 2 + i3;
        int i5 = 0 + i2;
        int i6 = 1 + i2;
        int i7 = 32 - i;
        iArr2[i4] = (iArr[i5] << i) | (iArr[i6] >>> i7);
        int i8 = 3 + i3;
        int i9 = 2 + i2;
        iArr2[i8] = (iArr[i6] << i) | (iArr[i9] >>> i7);
        int i10 = 0 + i3;
        int i11 = 3 + i2;
        iArr2[i10] = (iArr[i11] >>> i7) | (iArr[i9] << i);
        int i12 = 1 + i3;
        iArr2[i12] = (iArr[i11] << i) | (iArr[i5] >>> i7);
        iArr[i5] = iArr2[i4];
        iArr[i6] = iArr2[i8];
        iArr[i9] = iArr2[i10];
        iArr[i11] = iArr2[i12];
    }

    private static void decroldqo32(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        int i4 = 2 + i3;
        int i5 = 1 + i2;
        int i6 = i - 32;
        int i7 = 2 + i2;
        i = 64 - i;
        iArr2[i4] = (iArr[i5] << i6) | (iArr[i7] >>> i);
        int i8 = 3 + i3;
        int i9 = 3 + i2;
        iArr2[i8] = (iArr[i7] << i6) | (iArr[i9] >>> i);
        int i10 = 0 + i3;
        int i11 = 0 + i2;
        iArr2[i10] = (iArr[i11] >>> i) | (iArr[i9] << i6);
        int i12 = 1 + i3;
        iArr2[i12] = (iArr[i5] >>> i) | (iArr[i11] << i6);
        iArr[i11] = iArr2[i4];
        iArr[i5] = iArr2[i8];
        iArr[i7] = iArr2[i10];
        iArr[i9] = iArr2[i12];
    }

    private void int2bytes(int i, byte[] bArr, int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            bArr[(3 - i3) + i2] = (byte) i;
            i >>>= 8;
        }
    }

    private byte lRot8(byte b, int i) {
        return (byte) (((b & 255) >>> (8 - i)) | (b << i));
    }

    private static int leftRotate(int i, int i2) {
        return (i << i2) + (i >>> (32 - i2));
    }

    private int processBlock128(byte[] bArr, int i, byte[] bArr2, int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            this.state[i3] = bytes2int(bArr, (i3 * 4) + i);
            int[] iArr = this.state;
            iArr[i3] = iArr[i3] ^ this.kw[i3];
        }
        camelliaF2(this.state, this.subkey, 0);
        camelliaF2(this.state, this.subkey, 4);
        camelliaF2(this.state, this.subkey, 8);
        camelliaFLs(this.state, this.ke, 0);
        camelliaF2(this.state, this.subkey, 12);
        camelliaF2(this.state, this.subkey, 16);
        camelliaF2(this.state, this.subkey, 20);
        camelliaFLs(this.state, this.ke, 4);
        camelliaF2(this.state, this.subkey, 24);
        camelliaF2(this.state, this.subkey, 28);
        camelliaF2(this.state, this.subkey, 32);
        int[] iArr2 = this.state;
        iArr2[2] = this.kw[4] ^ iArr2[2];
        iArr2 = this.state;
        iArr2[3] = iArr2[3] ^ this.kw[5];
        iArr2 = this.state;
        iArr2[0] = iArr2[0] ^ this.kw[6];
        iArr2 = this.state;
        iArr2[1] = iArr2[1] ^ this.kw[7];
        int2bytes(this.state[2], bArr2, i2);
        int2bytes(this.state[3], bArr2, i2 + 4);
        int2bytes(this.state[0], bArr2, i2 + 8);
        int2bytes(this.state[1], bArr2, i2 + 12);
        return 16;
    }

    private int processBlock192or256(byte[] bArr, int i, byte[] bArr2, int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            this.state[i3] = bytes2int(bArr, (i3 * 4) + i);
            int[] iArr = this.state;
            iArr[i3] = iArr[i3] ^ this.kw[i3];
        }
        camelliaF2(this.state, this.subkey, 0);
        camelliaF2(this.state, this.subkey, 4);
        camelliaF2(this.state, this.subkey, 8);
        camelliaFLs(this.state, this.ke, 0);
        camelliaF2(this.state, this.subkey, 12);
        camelliaF2(this.state, this.subkey, 16);
        camelliaF2(this.state, this.subkey, 20);
        camelliaFLs(this.state, this.ke, 4);
        camelliaF2(this.state, this.subkey, 24);
        camelliaF2(this.state, this.subkey, 28);
        camelliaF2(this.state, this.subkey, 32);
        camelliaFLs(this.state, this.ke, 8);
        camelliaF2(this.state, this.subkey, 36);
        camelliaF2(this.state, this.subkey, 40);
        camelliaF2(this.state, this.subkey, 44);
        int[] iArr2 = this.state;
        iArr2[2] = iArr2[2] ^ this.kw[4];
        iArr2 = this.state;
        iArr2[3] = iArr2[3] ^ this.kw[5];
        iArr2 = this.state;
        iArr2[0] = iArr2[0] ^ this.kw[6];
        iArr2 = this.state;
        iArr2[1] = iArr2[1] ^ this.kw[7];
        int2bytes(this.state[2], bArr2, i2);
        int2bytes(this.state[3], bArr2, i2 + 4);
        int2bytes(this.state[0], bArr2, i2 + 8);
        int2bytes(this.state[1], bArr2, i2 + 12);
        return 16;
    }

    private static int rightRotate(int i, int i2) {
        return (i >>> i2) + (i << (32 - i2));
    }

    private static void roldq(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        int i4 = 0 + i3;
        int i5 = 0 + i2;
        int i6 = 1 + i2;
        int i7 = 32 - i;
        iArr2[i4] = (iArr[i5] << i) | (iArr[i6] >>> i7);
        int i8 = 1 + i3;
        int i9 = 2 + i2;
        iArr2[i8] = (iArr[i6] << i) | (iArr[i9] >>> i7);
        int i10 = 2 + i3;
        i2 += 3;
        iArr2[i10] = (iArr[i9] << i) | (iArr[i2] >>> i7);
        int i11 = 3 + i3;
        iArr2[i11] = (iArr[i2] << i) | (iArr[i5] >>> i7);
        iArr[i5] = iArr2[i4];
        iArr[i6] = iArr2[i8];
        iArr[i9] = iArr2[i10];
        iArr[i2] = iArr2[i11];
    }

    private static void roldqo32(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        int i4 = 0 + i3;
        int i5 = 1 + i2;
        int i6 = i - 32;
        int i7 = 2 + i2;
        i = 64 - i;
        iArr2[i4] = (iArr[i5] << i6) | (iArr[i7] >>> i);
        int i8 = 1 + i3;
        int i9 = 3 + i2;
        iArr2[i8] = (iArr[i7] << i6) | (iArr[i9] >>> i);
        int i10 = 2 + i3;
        int i11 = 0 + i2;
        iArr2[i10] = (iArr[i11] >>> i) | (iArr[i9] << i6);
        int i12 = 3 + i3;
        iArr2[i12] = (iArr[i5] >>> i) | (iArr[i11] << i6);
        iArr[i11] = iArr2[i4];
        iArr[i5] = iArr2[i8];
        iArr[i7] = iArr2[i10];
        iArr[i9] = iArr2[i12];
    }

    private int sbox2(int i) {
        return lRot8(SBOX1[i], 1) & 255;
    }

    private int sbox3(int i) {
        return lRot8(SBOX1[i], 7) & 255;
    }

    private int sbox4(int i) {
        return SBOX1[lRot8((byte) i, 1) & 255] & 255;
    }

    private void setKey(boolean z, byte[] bArr) {
        int i;
        byte[] bArr2 = bArr;
        int[] iArr = new int[8];
        int[] iArr2 = new int[4];
        int[] iArr3 = new int[4];
        int[] iArr4 = new int[4];
        int length = bArr2.length;
        if (length != 16) {
            if (length == 24) {
                iArr[0] = bytes2int(bArr2, 0);
                iArr[1] = bytes2int(bArr2, 4);
                iArr[2] = bytes2int(bArr2, 8);
                iArr[3] = bytes2int(bArr2, 12);
                iArr[4] = bytes2int(bArr2, 16);
                iArr[5] = bytes2int(bArr2, 20);
                iArr[6] = ~iArr[4];
                iArr[7] = ~iArr[5];
            } else if (length == 32) {
                iArr[0] = bytes2int(bArr2, 0);
                iArr[1] = bytes2int(bArr2, 4);
                iArr[2] = bytes2int(bArr2, 8);
                iArr[3] = bytes2int(bArr2, 12);
                iArr[4] = bytes2int(bArr2, 16);
                iArr[5] = bytes2int(bArr2, 20);
                iArr[6] = bytes2int(bArr2, 24);
                iArr[7] = bytes2int(bArr2, 28);
            } else {
                throw new IllegalArgumentException("key sizes are only 16/24/32 bytes.");
            }
            this._keyis128 = false;
        } else {
            this._keyis128 = true;
            iArr[0] = bytes2int(bArr2, 0);
            iArr[1] = bytes2int(bArr2, 4);
            iArr[2] = bytes2int(bArr2, 8);
            iArr[3] = bytes2int(bArr2, 12);
            iArr[7] = 0;
            iArr[6] = 0;
            iArr[5] = 0;
            iArr[4] = 0;
        }
        for (i = 0; i < 4; i++) {
            iArr2[i] = iArr[i] ^ iArr[i + 4];
        }
        camelliaF2(iArr2, SIGMA, 0);
        for (i = 0; i < 4; i++) {
            iArr2[i] = iArr2[i] ^ iArr[i];
        }
        camelliaF2(iArr2, SIGMA, 4);
        if (!this._keyis128) {
            for (i = 0; i < 4; i++) {
                iArr3[i] = iArr2[i] ^ iArr[i + 4];
            }
            camelliaF2(iArr3, SIGMA, 8);
            if (z) {
                this.kw[0] = iArr[0];
                this.kw[1] = iArr[1];
                this.kw[2] = iArr[2];
                this.kw[3] = iArr[3];
                roldqo32(45, iArr, 0, this.subkey, 16);
                roldq(15, iArr, 0, this.ke, 4);
                roldq(17, iArr, 0, this.subkey, 32);
                roldqo32(34, iArr, 0, this.subkey, 44);
                roldq(15, iArr, 4, this.subkey, 4);
                roldq(15, iArr, 4, this.ke, 0);
                roldq(30, iArr, 4, this.subkey, 24);
                roldqo32(34, iArr, 4, this.subkey, 36);
                roldq(15, iArr2, 0, this.subkey, 8);
                roldq(30, iArr2, 0, this.subkey, 20);
                this.ke[8] = iArr2[1];
                this.ke[9] = iArr2[2];
                this.ke[10] = iArr2[3];
                this.ke[11] = iArr2[0];
                roldqo32(49, iArr2, 0, this.subkey, 40);
                this.subkey[0] = iArr3[0];
                this.subkey[1] = iArr3[1];
                this.subkey[2] = iArr3[2];
                this.subkey[3] = iArr3[3];
                roldq(30, iArr3, 0, this.subkey, 12);
                roldq(30, iArr3, 0, this.subkey, 28);
                roldqo32(51, iArr3, 0, this.kw, 4);
                return;
            }
            this.kw[4] = iArr[0];
            this.kw[5] = iArr[1];
            this.kw[6] = iArr[2];
            this.kw[7] = iArr[3];
            decroldqo32(45, iArr, 0, this.subkey, 28);
            decroldq(15, iArr, 0, this.ke, 4);
            decroldq(17, iArr, 0, this.subkey, 12);
            decroldqo32(34, iArr, 0, this.subkey, 0);
            decroldq(15, iArr, 4, this.subkey, 40);
            decroldq(15, iArr, 4, this.ke, 8);
            decroldq(30, iArr, 4, this.subkey, 20);
            decroldqo32(34, iArr, 4, this.subkey, 8);
            decroldq(15, iArr2, 0, this.subkey, 36);
            decroldq(30, iArr2, 0, this.subkey, 24);
            this.ke[2] = iArr2[1];
            this.ke[3] = iArr2[2];
            this.ke[0] = iArr2[3];
            this.ke[1] = iArr2[0];
            decroldqo32(49, iArr2, 0, this.subkey, 4);
            this.subkey[46] = iArr3[0];
            this.subkey[47] = iArr3[1];
            this.subkey[44] = iArr3[2];
            this.subkey[45] = iArr3[3];
            decroldq(30, iArr3, 0, this.subkey, 32);
            decroldq(30, iArr3, 0, this.subkey, 16);
            roldqo32(51, iArr3, 0, this.kw, 0);
        } else if (z) {
            this.kw[0] = iArr[0];
            this.kw[1] = iArr[1];
            this.kw[2] = iArr[2];
            this.kw[3] = iArr[3];
            roldq(15, iArr, 0, this.subkey, 4);
            roldq(30, iArr, 0, this.subkey, 12);
            roldq(15, iArr, 0, iArr4, 0);
            this.subkey[18] = iArr4[2];
            this.subkey[19] = iArr4[3];
            roldq(17, iArr, 0, this.ke, 4);
            roldq(17, iArr, 0, this.subkey, 24);
            roldq(17, iArr, 0, this.subkey, 32);
            this.subkey[0] = iArr2[0];
            this.subkey[1] = iArr2[1];
            this.subkey[2] = iArr2[2];
            this.subkey[3] = iArr2[3];
            roldq(15, iArr2, 0, this.subkey, 8);
            roldq(15, iArr2, 0, this.ke, 0);
            roldq(15, iArr2, 0, iArr4, 0);
            this.subkey[16] = iArr4[0];
            this.subkey[17] = iArr4[1];
            roldq(15, iArr2, 0, this.subkey, 20);
            roldqo32(34, iArr2, 0, this.subkey, 28);
            roldq(17, iArr2, 0, this.kw, 4);
        } else {
            this.kw[4] = iArr[0];
            this.kw[5] = iArr[1];
            this.kw[6] = iArr[2];
            this.kw[7] = iArr[3];
            decroldq(15, iArr, 0, this.subkey, 28);
            decroldq(30, iArr, 0, this.subkey, 20);
            decroldq(15, iArr, 0, iArr4, 0);
            this.subkey[16] = iArr4[0];
            this.subkey[17] = iArr4[1];
            decroldq(17, iArr, 0, this.ke, 0);
            decroldq(17, iArr, 0, this.subkey, 8);
            decroldq(17, iArr, 0, this.subkey, 0);
            this.subkey[34] = iArr2[0];
            this.subkey[35] = iArr2[1];
            this.subkey[32] = iArr2[2];
            this.subkey[33] = iArr2[3];
            decroldq(15, iArr2, 0, this.subkey, 24);
            decroldq(15, iArr2, 0, this.ke, 4);
            decroldq(15, iArr2, 0, iArr4, 0);
            this.subkey[18] = iArr4[2];
            this.subkey[19] = iArr4[3];
            decroldq(15, iArr2, 0, this.subkey, 12);
            decroldqo32(34, iArr2, 0, this.subkey, 4);
            roldq(17, iArr2, 0, this.kw, 0);
        }
    }

    public String getAlgorithmName() {
        return "Camellia";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            setKey(z, ((KeyParameter) cipherParameters).getKey());
            this.initialized = true;
            return;
        }
        throw new IllegalArgumentException("only simple KeyParameter expected.");
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws IllegalStateException {
        if (!this.initialized) {
            throw new IllegalStateException("Camellia is not initialized");
        } else if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 16 <= bArr2.length) {
            return this._keyis128 ? processBlock128(bArr, i, bArr2, i2) : processBlock192or256(bArr, i, bArr2, i2);
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
