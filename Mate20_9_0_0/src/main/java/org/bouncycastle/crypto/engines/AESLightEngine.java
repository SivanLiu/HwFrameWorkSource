package org.bouncycastle.crypto.engines;

import java.lang.reflect.Array;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Pack;

public class AESLightEngine implements BlockCipher {
    private static final int BLOCK_SIZE = 16;
    private static final byte[] S = new byte[]{(byte) 99, (byte) 124, (byte) 119, (byte) 123, (byte) -14, (byte) 107, (byte) 111, (byte) -59, (byte) 48, (byte) 1, (byte) 103, (byte) 43, (byte) -2, (byte) -41, (byte) -85, (byte) 118, (byte) -54, (byte) -126, (byte) -55, (byte) 125, (byte) -6, (byte) 89, (byte) 71, (byte) -16, (byte) -83, (byte) -44, (byte) -94, (byte) -81, (byte) -100, (byte) -92, (byte) 114, (byte) -64, (byte) -73, (byte) -3, (byte) -109, (byte) 38, (byte) 54, (byte) 63, (byte) -9, (byte) -52, (byte) 52, (byte) -91, (byte) -27, (byte) -15, (byte) 113, (byte) -40, (byte) 49, (byte) 21, (byte) 4, (byte) -57, (byte) 35, (byte) -61, (byte) 24, (byte) -106, (byte) 5, (byte) -102, (byte) 7, (byte) 18, Byte.MIN_VALUE, (byte) -30, (byte) -21, (byte) 39, (byte) -78, (byte) 117, (byte) 9, (byte) -125, (byte) 44, (byte) 26, (byte) 27, (byte) 110, (byte) 90, (byte) -96, (byte) 82, (byte) 59, (byte) -42, (byte) -77, (byte) 41, (byte) -29, (byte) 47, (byte) -124, (byte) 83, (byte) -47, (byte) 0, (byte) -19, (byte) 32, (byte) -4, (byte) -79, (byte) 91, (byte) 106, (byte) -53, (byte) -66, (byte) 57, (byte) 74, (byte) 76, (byte) 88, (byte) -49, (byte) -48, (byte) -17, (byte) -86, (byte) -5, (byte) 67, (byte) 77, (byte) 51, (byte) -123, (byte) 69, (byte) -7, (byte) 2, Byte.MAX_VALUE, (byte) 80, (byte) 60, (byte) -97, (byte) -88, (byte) 81, (byte) -93, (byte) 64, (byte) -113, (byte) -110, (byte) -99, (byte) 56, (byte) -11, PSSSigner.TRAILER_IMPLICIT, (byte) -74, (byte) -38, (byte) 33, Tnaf.POW_2_WIDTH, (byte) -1, (byte) -13, (byte) -46, (byte) -51, (byte) 12, (byte) 19, (byte) -20, (byte) 95, (byte) -105, (byte) 68, (byte) 23, (byte) -60, (byte) -89, (byte) 126, (byte) 61, (byte) 100, (byte) 93, (byte) 25, (byte) 115, (byte) 96, (byte) -127, (byte) 79, (byte) -36, (byte) 34, (byte) 42, (byte) -112, (byte) -120, (byte) 70, (byte) -18, (byte) -72, (byte) 20, (byte) -34, (byte) 94, (byte) 11, (byte) -37, (byte) -32, (byte) 50, (byte) 58, (byte) 10, (byte) 73, (byte) 6, (byte) 36, (byte) 92, (byte) -62, (byte) -45, (byte) -84, (byte) 98, (byte) -111, (byte) -107, (byte) -28, (byte) 121, (byte) -25, (byte) -56, (byte) 55, (byte) 109, (byte) -115, (byte) -43, (byte) 78, (byte) -87, (byte) 108, (byte) 86, (byte) -12, (byte) -22, (byte) 101, (byte) 122, (byte) -82, (byte) 8, (byte) -70, (byte) 120, (byte) 37, (byte) 46, (byte) 28, (byte) -90, (byte) -76, (byte) -58, (byte) -24, (byte) -35, (byte) 116, (byte) 31, (byte) 75, (byte) -67, (byte) -117, (byte) -118, (byte) 112, (byte) 62, (byte) -75, (byte) 102, (byte) 72, (byte) 3, (byte) -10, (byte) 14, (byte) 97, (byte) 53, (byte) 87, (byte) -71, (byte) -122, (byte) -63, (byte) 29, (byte) -98, (byte) -31, (byte) -8, (byte) -104, (byte) 17, (byte) 105, (byte) -39, (byte) -114, (byte) -108, (byte) -101, (byte) 30, (byte) -121, (byte) -23, (byte) -50, (byte) 85, (byte) 40, (byte) -33, (byte) -116, (byte) -95, (byte) -119, (byte) 13, (byte) -65, (byte) -26, (byte) 66, (byte) 104, (byte) 65, (byte) -103, (byte) 45, (byte) 15, (byte) -80, (byte) 84, (byte) -69, (byte) 22};
    private static final byte[] Si = new byte[]{(byte) 82, (byte) 9, (byte) 106, (byte) -43, (byte) 48, (byte) 54, (byte) -91, (byte) 56, (byte) -65, (byte) 64, (byte) -93, (byte) -98, (byte) -127, (byte) -13, (byte) -41, (byte) -5, (byte) 124, (byte) -29, (byte) 57, (byte) -126, (byte) -101, (byte) 47, (byte) -1, (byte) -121, (byte) 52, (byte) -114, (byte) 67, (byte) 68, (byte) -60, (byte) -34, (byte) -23, (byte) -53, (byte) 84, (byte) 123, (byte) -108, (byte) 50, (byte) -90, (byte) -62, (byte) 35, (byte) 61, (byte) -18, (byte) 76, (byte) -107, (byte) 11, (byte) 66, (byte) -6, (byte) -61, (byte) 78, (byte) 8, (byte) 46, (byte) -95, (byte) 102, (byte) 40, (byte) -39, (byte) 36, (byte) -78, (byte) 118, (byte) 91, (byte) -94, (byte) 73, (byte) 109, (byte) -117, (byte) -47, (byte) 37, (byte) 114, (byte) -8, (byte) -10, (byte) 100, (byte) -122, (byte) 104, (byte) -104, (byte) 22, (byte) -44, (byte) -92, (byte) 92, (byte) -52, (byte) 93, (byte) 101, (byte) -74, (byte) -110, (byte) 108, (byte) 112, (byte) 72, (byte) 80, (byte) -3, (byte) -19, (byte) -71, (byte) -38, (byte) 94, (byte) 21, (byte) 70, (byte) 87, (byte) -89, (byte) -115, (byte) -99, (byte) -124, (byte) -112, (byte) -40, (byte) -85, (byte) 0, (byte) -116, PSSSigner.TRAILER_IMPLICIT, (byte) -45, (byte) 10, (byte) -9, (byte) -28, (byte) 88, (byte) 5, (byte) -72, (byte) -77, (byte) 69, (byte) 6, (byte) -48, (byte) 44, (byte) 30, (byte) -113, (byte) -54, (byte) 63, (byte) 15, (byte) 2, (byte) -63, (byte) -81, (byte) -67, (byte) 3, (byte) 1, (byte) 19, (byte) -118, (byte) 107, (byte) 58, (byte) -111, (byte) 17, (byte) 65, (byte) 79, (byte) 103, (byte) -36, (byte) -22, (byte) -105, (byte) -14, (byte) -49, (byte) -50, (byte) -16, (byte) -76, (byte) -26, (byte) 115, (byte) -106, (byte) -84, (byte) 116, (byte) 34, (byte) -25, (byte) -83, (byte) 53, (byte) -123, (byte) -30, (byte) -7, (byte) 55, (byte) -24, (byte) 28, (byte) 117, (byte) -33, (byte) 110, (byte) 71, (byte) -15, (byte) 26, (byte) 113, (byte) 29, (byte) 41, (byte) -59, (byte) -119, (byte) 111, (byte) -73, (byte) 98, (byte) 14, (byte) -86, (byte) 24, (byte) -66, (byte) 27, (byte) -4, (byte) 86, (byte) 62, (byte) 75, (byte) -58, (byte) -46, (byte) 121, (byte) 32, (byte) -102, (byte) -37, (byte) -64, (byte) -2, (byte) 120, (byte) -51, (byte) 90, (byte) -12, (byte) 31, (byte) -35, (byte) -88, (byte) 51, (byte) -120, (byte) 7, (byte) -57, (byte) 49, (byte) -79, (byte) 18, Tnaf.POW_2_WIDTH, (byte) 89, (byte) 39, Byte.MIN_VALUE, (byte) -20, (byte) 95, (byte) 96, (byte) 81, Byte.MAX_VALUE, (byte) -87, (byte) 25, (byte) -75, (byte) 74, (byte) 13, (byte) 45, (byte) -27, (byte) 122, (byte) -97, (byte) -109, (byte) -55, (byte) -100, (byte) -17, (byte) -96, (byte) -32, (byte) 59, (byte) 77, (byte) -82, (byte) 42, (byte) -11, (byte) -80, (byte) -56, (byte) -21, (byte) -69, (byte) 60, (byte) -125, (byte) 83, (byte) -103, (byte) 97, (byte) 23, (byte) 43, (byte) 4, (byte) 126, (byte) -70, (byte) 119, (byte) -42, (byte) 38, (byte) -31, (byte) 105, (byte) 20, (byte) 99, (byte) 85, (byte) 33, (byte) 12, (byte) 125};
    private static final int m1 = -2139062144;
    private static final int m2 = 2139062143;
    private static final int m3 = 27;
    private static final int m4 = -1061109568;
    private static final int m5 = 1061109567;
    private static final int[] rcon = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 27, 54, CipherSuite.TLS_DH_anon_WITH_AES_128_CBC_SHA256, 216, CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384, 77, CipherSuite.TLS_DHE_RSA_WITH_SEED_CBC_SHA, 47, 94, 188, 99, 198, CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA, 53, CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256, 212, CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384, 125, 250, 239, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256, CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA};
    private int C0;
    private int C1;
    private int C2;
    private int C3;
    private int ROUNDS;
    private int[][] WorkingKey = ((int[][]) null);
    private boolean forEncryption;

    private static int FFmulX(int i) {
        return (((i & m1) >>> 7) * 27) ^ ((m2 & i) << 1);
    }

    private static int FFmulX2(int i) {
        i &= m4;
        i ^= i >>> 1;
        return (i >>> 5) ^ (((m5 & i) << 2) ^ (i >>> 2));
    }

    private void decryptBlock(int[][] iArr) {
        int inv_mcol;
        int inv_mcol2;
        int inv_mcol3;
        int i = this.C0 ^ iArr[this.ROUNDS][0];
        int i2 = this.C1 ^ iArr[this.ROUNDS][1];
        int i3 = this.C2 ^ iArr[this.ROUNDS][2];
        int i4 = this.ROUNDS - 1;
        int i5 = this.C3 ^ iArr[this.ROUNDS][3];
        while (i4 > 1) {
            inv_mcol = inv_mcol((((Si[i & 255] & 255) ^ ((Si[(i5 >> 8) & 255] & 255) << 8)) ^ ((Si[(i3 >> 16) & 255] & 255) << 16)) ^ (Si[(i2 >> 24) & 255] << 24)) ^ iArr[i4][0];
            inv_mcol2 = inv_mcol((((Si[i2 & 255] & 255) ^ ((Si[(i >> 8) & 255] & 255) << 8)) ^ ((Si[(i5 >> 16) & 255] & 255) << 16)) ^ (Si[(i3 >> 24) & 255] << 24)) ^ iArr[i4][1];
            inv_mcol3 = inv_mcol((((Si[i3 & 255] & 255) ^ ((Si[(i2 >> 8) & 255] & 255) << 8)) ^ ((Si[(i >> 16) & 255] & 255) << 16)) ^ (Si[(i5 >> 24) & 255] << 24)) ^ iArr[i4][2];
            i2 = ((Si[(i2 >> 16) & 255] & 255) << 16) ^ (((Si[(i3 >> 8) & 255] & 255) << 8) ^ (Si[i5 & 255] & 255));
            i2 = i4 - 1;
            i = inv_mcol((Si[(i >> 24) & 255] << 24) ^ i2) ^ iArr[i4][3];
            i3 = inv_mcol((((Si[inv_mcol & 255] & 255) ^ ((Si[(i >> 8) & 255] & 255) << 8)) ^ ((Si[(inv_mcol3 >> 16) & 255] & 255) << 16)) ^ (Si[(inv_mcol2 >> 24) & 255] << 24)) ^ iArr[i2][0];
            i4 = inv_mcol((((Si[inv_mcol2 & 255] & 255) ^ ((Si[(inv_mcol >> 8) & 255] & 255) << 8)) ^ ((Si[(i >> 16) & 255] & 255) << 16)) ^ (Si[(inv_mcol3 >> 24) & 255] << 24)) ^ iArr[i2][1];
            i5 = inv_mcol((((Si[inv_mcol3 & 255] & 255) ^ ((Si[(inv_mcol2 >> 8) & 255] & 255) << 8)) ^ ((Si[(inv_mcol >> 16) & 255] & 255) << 16)) ^ (Si[(i >> 24) & 255] << 24)) ^ iArr[i2][2];
            inv_mcol = i2 - 1;
            i = inv_mcol((((Si[i & 255] & 255) ^ ((Si[(inv_mcol3 >> 8) & 255] & 255) << 8)) ^ ((Si[(inv_mcol2 >> 16) & 255] & 255) << 16)) ^ (Si[(inv_mcol >> 24) & 255] << 24)) ^ iArr[i2][3];
            i2 = i4;
            i4 = inv_mcol;
            int i6 = i5;
            i5 = i;
            i = i3;
            i3 = i6;
        }
        inv_mcol = inv_mcol((((Si[i & 255] & 255) ^ ((Si[(i5 >> 8) & 255] & 255) << 8)) ^ ((Si[(i3 >> 16) & 255] & 255) << 16)) ^ (Si[(i2 >> 24) & 255] << 24)) ^ iArr[i4][0];
        inv_mcol2 = inv_mcol((((Si[i2 & 255] & 255) ^ ((Si[(i >> 8) & 255] & 255) << 8)) ^ ((Si[(i5 >> 16) & 255] & 255) << 16)) ^ (Si[(i3 >> 24) & 255] << 24)) ^ iArr[i4][1];
        inv_mcol3 = inv_mcol((((Si[i3 & 255] & 255) ^ ((Si[(i2 >> 8) & 255] & 255) << 8)) ^ ((Si[(i >> 16) & 255] & 255) << 16)) ^ (Si[(i5 >> 24) & 255] << 24)) ^ iArr[i4][2];
        i = inv_mcol((Si[(i >> 24) & 255] << 24) ^ (((Si[(i2 >> 16) & 255] & 255) << 16) ^ (((Si[(i3 >> 8) & 255] & 255) << 8) ^ (Si[i5 & 255] & 255)))) ^ iArr[i4][3];
        this.C0 = ((((Si[inv_mcol & 255] & 255) ^ ((Si[(i >> 8) & 255] & 255) << 8)) ^ ((Si[(inv_mcol3 >> 16) & 255] & 255) << 16)) ^ (Si[(inv_mcol2 >> 24) & 255] << 24)) ^ iArr[0][0];
        this.C1 = ((((Si[inv_mcol2 & 255] & 255) ^ ((Si[(inv_mcol >> 8) & 255] & 255) << 8)) ^ ((Si[(i >> 16) & 255] & 255) << 16)) ^ (Si[(inv_mcol3 >> 24) & 255] << 24)) ^ iArr[0][1];
        this.C2 = ((((Si[inv_mcol3 & 255] & 255) ^ ((Si[(inv_mcol2 >> 8) & 255] & 255) << 8)) ^ ((Si[(inv_mcol >> 16) & 255] & 255) << 16)) ^ (Si[(i >> 24) & 255] << 24)) ^ iArr[0][2];
        this.C3 = iArr[0][3] ^ ((((Si[i & 255] & 255) ^ ((Si[(inv_mcol3 >> 8) & 255] & 255) << 8)) ^ ((Si[(inv_mcol2 >> 16) & 255] & 255) << 16)) ^ (Si[(inv_mcol >> 24) & 255] << 24));
    }

    private void encryptBlock(int[][] iArr) {
        int mcol;
        int mcol2;
        int mcol3;
        int i = this.C3 ^ iArr[0][3];
        int i2 = this.C2 ^ iArr[0][2];
        int i3 = this.C1 ^ iArr[0][1];
        int i4 = this.C0 ^ iArr[0][0];
        int i5 = 1;
        while (i5 < this.ROUNDS - 1) {
            mcol = mcol((((S[i4 & 255] & 255) ^ ((S[(i3 >> 8) & 255] & 255) << 8)) ^ ((S[(i2 >> 16) & 255] & 255) << 16)) ^ (S[(i >> 24) & 255] << 24)) ^ iArr[i5][0];
            mcol2 = mcol((((S[i3 & 255] & 255) ^ ((S[(i2 >> 8) & 255] & 255) << 8)) ^ ((S[(i >> 16) & 255] & 255) << 16)) ^ (S[(i4 >> 24) & 255] << 24)) ^ iArr[i5][1];
            mcol3 = mcol((((S[i2 & 255] & 255) ^ ((S[(i >> 8) & 255] & 255) << 8)) ^ ((S[(i4 >> 16) & 255] & 255) << 16)) ^ (S[(i3 >> 24) & 255] << 24)) ^ iArr[i5][2];
            i4 = mcol(((((S[(i4 >> 8) & 255] & 255) << 8) ^ (S[i & 255] & 255)) ^ ((S[(i3 >> 16) & 255] & 255) << 16)) ^ (S[(i2 >> 24) & 255] << 24));
            i3 = i5 + 1;
            i5 = iArr[i5][3] ^ i4;
            i4 = mcol((((S[mcol & 255] & 255) ^ ((S[(mcol2 >> 8) & 255] & 255) << 8)) ^ ((S[(mcol3 >> 16) & 255] & 255) << 16)) ^ (S[(i5 >> 24) & 255] << 24)) ^ iArr[i3][0];
            i2 = mcol((((S[mcol2 & 255] & 255) ^ ((S[(mcol3 >> 8) & 255] & 255) << 8)) ^ ((S[(i5 >> 16) & 255] & 255) << 16)) ^ (S[(mcol >> 24) & 255] << 24)) ^ iArr[i3][1];
            i = mcol((((S[mcol3 & 255] & 255) ^ ((S[(i5 >> 8) & 255] & 255) << 8)) ^ ((S[(mcol >> 16) & 255] & 255) << 16)) ^ (S[(mcol2 >> 24) & 255] << 24)) ^ iArr[i3][2];
            mcol = i3 + 1;
            i5 = mcol((((S[i5 & 255] & 255) ^ ((S[(mcol >> 8) & 255] & 255) << 8)) ^ ((S[(mcol2 >> 16) & 255] & 255) << 16)) ^ (S[(mcol3 >> 24) & 255] << 24)) ^ iArr[i3][3];
            i3 = i2;
            i2 = i;
            i = i5;
            i5 = mcol;
        }
        mcol = mcol((((S[i4 & 255] & 255) ^ ((S[(i3 >> 8) & 255] & 255) << 8)) ^ ((S[(i2 >> 16) & 255] & 255) << 16)) ^ (S[(i >> 24) & 255] << 24)) ^ iArr[i5][0];
        mcol2 = mcol((((S[i3 & 255] & 255) ^ ((S[(i2 >> 8) & 255] & 255) << 8)) ^ ((S[(i >> 16) & 255] & 255) << 16)) ^ (S[(i4 >> 24) & 255] << 24)) ^ iArr[i5][1];
        mcol3 = mcol((((S[i2 & 255] & 255) ^ ((S[(i >> 8) & 255] & 255) << 8)) ^ ((S[(i4 >> 16) & 255] & 255) << 16)) ^ (S[(i3 >> 24) & 255] << 24)) ^ iArr[i5][2];
        i4 = mcol(((((S[(i4 >> 8) & 255] & 255) << 8) ^ (S[i & 255] & 255)) ^ ((S[(i3 >> 16) & 255] & 255) << 16)) ^ (S[(i2 >> 24) & 255] << 24));
        i3 = i5 + 1;
        i5 = iArr[i5][3] ^ i4;
        this.C0 = iArr[i3][0] ^ ((((S[mcol & 255] & 255) ^ ((S[(mcol2 >> 8) & 255] & 255) << 8)) ^ ((S[(mcol3 >> 16) & 255] & 255) << 16)) ^ (S[(i5 >> 24) & 255] << 24));
        this.C1 = ((((S[mcol2 & 255] & 255) ^ ((S[(mcol3 >> 8) & 255] & 255) << 8)) ^ ((S[(i5 >> 16) & 255] & 255) << 16)) ^ (S[(mcol >> 24) & 255] << 24)) ^ iArr[i3][1];
        this.C2 = ((((S[mcol3 & 255] & 255) ^ ((S[(i5 >> 8) & 255] & 255) << 8)) ^ ((S[(mcol >> 16) & 255] & 255) << 16)) ^ (S[(mcol2 >> 24) & 255] << 24)) ^ iArr[i3][2];
        this.C3 = iArr[i3][3] ^ ((((S[i5 & 255] & 255) ^ ((S[(mcol >> 8) & 255] & 255) << 8)) ^ ((S[(mcol2 >> 16) & 255] & 255) << 16)) ^ (S[(mcol3 >> 24) & 255] << 24));
    }

    private int[][] generateWorkingKey(byte[] bArr, boolean z) {
        byte[] bArr2 = bArr;
        int length = bArr2.length;
        if (length < 16 || length > 32 || (length & 7) != 0) {
            throw new IllegalArgumentException("Key length not 128/192/256 bits.");
        }
        int littleEndianToInt;
        length >>= 2;
        this.ROUNDS = length + 6;
        int i = 1;
        int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{this.ROUNDS + 1, 4});
        int i2 = 12;
        int littleEndianToInt2;
        int littleEndianToInt3;
        int littleEndianToInt4;
        int littleEndianToInt5;
        int i3;
        int i4;
        int subWord;
        if (length == 4) {
            length = Pack.littleEndianToInt(bArr2, 0);
            iArr[0][0] = length;
            littleEndianToInt2 = Pack.littleEndianToInt(bArr2, 4);
            iArr[0][1] = littleEndianToInt2;
            littleEndianToInt3 = Pack.littleEndianToInt(bArr2, 8);
            iArr[0][2] = littleEndianToInt3;
            littleEndianToInt = Pack.littleEndianToInt(bArr2, 12);
            iArr[0][3] = littleEndianToInt;
            i2 = littleEndianToInt3;
            littleEndianToInt3 = length;
            length = littleEndianToInt;
            for (littleEndianToInt = 1; littleEndianToInt <= 10; littleEndianToInt++) {
                littleEndianToInt3 ^= subWord(shift(length, 8)) ^ rcon[littleEndianToInt - 1];
                iArr[littleEndianToInt][0] = littleEndianToInt3;
                littleEndianToInt2 ^= littleEndianToInt3;
                iArr[littleEndianToInt][1] = littleEndianToInt2;
                i2 ^= littleEndianToInt2;
                iArr[littleEndianToInt][2] = i2;
                length ^= i2;
                iArr[littleEndianToInt][3] = length;
            }
        } else if (length == 6) {
            length = Pack.littleEndianToInt(bArr2, 0);
            iArr[0][0] = length;
            littleEndianToInt4 = Pack.littleEndianToInt(bArr2, 4);
            iArr[0][1] = littleEndianToInt4;
            littleEndianToInt2 = Pack.littleEndianToInt(bArr2, 8);
            iArr[0][2] = littleEndianToInt2;
            littleEndianToInt5 = Pack.littleEndianToInt(bArr2, 12);
            iArr[0][3] = littleEndianToInt5;
            littleEndianToInt3 = Pack.littleEndianToInt(bArr2, 16);
            iArr[1][0] = littleEndianToInt3;
            littleEndianToInt = Pack.littleEndianToInt(bArr2, 20);
            iArr[1][1] = littleEndianToInt;
            length ^= subWord(shift(littleEndianToInt, 8)) ^ 1;
            iArr[1][2] = length;
            littleEndianToInt4 ^= length;
            iArr[1][3] = littleEndianToInt4;
            littleEndianToInt2 ^= littleEndianToInt4;
            iArr[2][0] = littleEndianToInt2;
            i3 = littleEndianToInt5 ^ littleEndianToInt2;
            iArr[2][1] = i3;
            littleEndianToInt3 ^= i3;
            iArr[2][2] = littleEndianToInt3;
            littleEndianToInt ^= littleEndianToInt3;
            iArr[2][3] = littleEndianToInt;
            i4 = littleEndianToInt3;
            littleEndianToInt3 = 2;
            littleEndianToInt5 = i3;
            i3 = littleEndianToInt2;
            littleEndianToInt2 = length;
            length = littleEndianToInt;
            littleEndianToInt = 3;
            while (littleEndianToInt < i2) {
                subWord = subWord(shift(length, 8)) ^ littleEndianToInt3;
                littleEndianToInt3 <<= 1;
                littleEndianToInt2 ^= subWord;
                iArr[littleEndianToInt][0] = littleEndianToInt2;
                littleEndianToInt4 ^= littleEndianToInt2;
                iArr[littleEndianToInt][1] = littleEndianToInt4;
                i3 ^= littleEndianToInt4;
                iArr[littleEndianToInt][2] = i3;
                littleEndianToInt5 ^= i3;
                iArr[littleEndianToInt][3] = littleEndianToInt5;
                i4 ^= littleEndianToInt5;
                subWord = littleEndianToInt + 1;
                iArr[subWord][0] = i4;
                length ^= i4;
                iArr[subWord][1] = length;
                i2 = subWord(shift(length, 8)) ^ littleEndianToInt3;
                littleEndianToInt3 <<= 1;
                littleEndianToInt2 ^= i2;
                iArr[subWord][2] = littleEndianToInt2;
                littleEndianToInt4 ^= littleEndianToInt2;
                iArr[subWord][3] = littleEndianToInt4;
                i3 ^= littleEndianToInt4;
                i2 = littleEndianToInt + 2;
                iArr[i2][0] = i3;
                littleEndianToInt5 ^= i3;
                iArr[i2][1] = littleEndianToInt5;
                i4 ^= littleEndianToInt5;
                iArr[i2][2] = i4;
                length ^= i4;
                iArr[i2][3] = length;
                littleEndianToInt += 3;
                i2 = 12;
            }
            littleEndianToInt = (subWord(shift(length, 8)) ^ littleEndianToInt3) ^ littleEndianToInt2;
            iArr[12][0] = littleEndianToInt;
            littleEndianToInt ^= littleEndianToInt4;
            iArr[12][1] = littleEndianToInt;
            littleEndianToInt ^= i3;
            iArr[12][2] = littleEndianToInt;
            iArr[12][3] = littleEndianToInt ^ littleEndianToInt5;
        } else if (length == 8) {
            length = Pack.littleEndianToInt(bArr2, 0);
            iArr[0][0] = length;
            littleEndianToInt5 = Pack.littleEndianToInt(bArr2, 4);
            iArr[0][1] = littleEndianToInt5;
            i4 = Pack.littleEndianToInt(bArr2, 8);
            iArr[0][2] = i4;
            i2 = Pack.littleEndianToInt(bArr2, 12);
            iArr[0][3] = i2;
            littleEndianToInt3 = Pack.littleEndianToInt(bArr2, 16);
            iArr[1][0] = littleEndianToInt3;
            i3 = Pack.littleEndianToInt(bArr2, 20);
            iArr[1][1] = i3;
            subWord = Pack.littleEndianToInt(bArr2, 24);
            iArr[1][2] = subWord;
            littleEndianToInt = Pack.littleEndianToInt(bArr2, 28);
            iArr[1][3] = littleEndianToInt;
            littleEndianToInt2 = length;
            int i5 = subWord;
            length = littleEndianToInt;
            subWord = i3;
            i3 = littleEndianToInt3;
            littleEndianToInt3 = 1;
            for (littleEndianToInt = 2; littleEndianToInt < 14; littleEndianToInt += 2) {
                littleEndianToInt4 = subWord(shift(length, 8)) ^ littleEndianToInt3;
                littleEndianToInt3 <<= 1;
                littleEndianToInt2 ^= littleEndianToInt4;
                iArr[littleEndianToInt][0] = littleEndianToInt2;
                littleEndianToInt5 ^= littleEndianToInt2;
                iArr[littleEndianToInt][1] = littleEndianToInt5;
                i4 ^= littleEndianToInt5;
                iArr[littleEndianToInt][2] = i4;
                i2 ^= i4;
                iArr[littleEndianToInt][3] = i2;
                i3 ^= subWord(i2);
                littleEndianToInt4 = littleEndianToInt + 1;
                iArr[littleEndianToInt4][0] = i3;
                subWord ^= i3;
                iArr[littleEndianToInt4][1] = subWord;
                i5 ^= subWord;
                iArr[littleEndianToInt4][2] = i5;
                length ^= i5;
                iArr[littleEndianToInt4][3] = length;
            }
            littleEndianToInt = (subWord(shift(length, 8)) ^ littleEndianToInt3) ^ littleEndianToInt2;
            iArr[14][0] = littleEndianToInt;
            littleEndianToInt ^= littleEndianToInt5;
            iArr[14][1] = littleEndianToInt;
            littleEndianToInt ^= i4;
            iArr[14][2] = littleEndianToInt;
            iArr[14][3] = littleEndianToInt ^ i2;
        } else {
            throw new IllegalStateException("Should never get here");
        }
        if (!z) {
            while (i < this.ROUNDS) {
                for (littleEndianToInt = 0; littleEndianToInt < 4; littleEndianToInt++) {
                    iArr[i][littleEndianToInt] = inv_mcol(iArr[i][littleEndianToInt]);
                }
                i++;
            }
        }
        return iArr;
    }

    private static int inv_mcol(int i) {
        int shift = shift(i, 8) ^ i;
        i ^= FFmulX(shift);
        shift ^= FFmulX2(i);
        return i ^ (shift ^ shift(shift, 16));
    }

    private static int mcol(int i) {
        int shift = shift(i, 8);
        i ^= shift;
        return FFmulX(i) ^ (shift ^ shift(i, 16));
    }

    private void packBlock(byte[] bArr, int i) {
        int i2 = i + 1;
        bArr[i] = (byte) this.C0;
        i = i2 + 1;
        bArr[i2] = (byte) (this.C0 >> 8);
        i2 = i + 1;
        bArr[i] = (byte) (this.C0 >> 16);
        i = i2 + 1;
        bArr[i2] = (byte) (this.C0 >> 24);
        i2 = i + 1;
        bArr[i] = (byte) this.C1;
        i = i2 + 1;
        bArr[i2] = (byte) (this.C1 >> 8);
        i2 = i + 1;
        bArr[i] = (byte) (this.C1 >> 16);
        i = i2 + 1;
        bArr[i2] = (byte) (this.C1 >> 24);
        i2 = i + 1;
        bArr[i] = (byte) this.C2;
        i = i2 + 1;
        bArr[i2] = (byte) (this.C2 >> 8);
        i2 = i + 1;
        bArr[i] = (byte) (this.C2 >> 16);
        i = i2 + 1;
        bArr[i2] = (byte) (this.C2 >> 24);
        i2 = i + 1;
        bArr[i] = (byte) this.C3;
        i = i2 + 1;
        bArr[i2] = (byte) (this.C3 >> 8);
        i2 = i + 1;
        bArr[i] = (byte) (this.C3 >> 16);
        bArr[i2] = (byte) (this.C3 >> 24);
    }

    private static int shift(int i, int i2) {
        return (i << (-i2)) | (i >>> i2);
    }

    private static int subWord(int i) {
        return (S[(i >> 24) & 255] << 24) | (((S[i & 255] & 255) | ((S[(i >> 8) & 255] & 255) << 8)) | ((S[(i >> 16) & 255] & 255) << 16));
    }

    private void unpackBlock(byte[] bArr, int i) {
        int i2 = i + 1;
        this.C0 = bArr[i] & 255;
        int i3 = i2 + 1;
        this.C0 |= (bArr[i2] & 255) << 8;
        i2 = i3 + 1;
        this.C0 |= (bArr[i3] & 255) << 16;
        i3 = i2 + 1;
        this.C0 |= bArr[i2] << 24;
        i = i3 + 1;
        this.C1 = bArr[i3] & 255;
        i3 = i + 1;
        this.C1 = ((bArr[i] & 255) << 8) | this.C1;
        i2 = i3 + 1;
        this.C1 |= (bArr[i3] & 255) << 16;
        i3 = i2 + 1;
        this.C1 |= bArr[i2] << 24;
        i = i3 + 1;
        this.C2 = bArr[i3] & 255;
        i3 = i + 1;
        this.C2 = ((bArr[i] & 255) << 8) | this.C2;
        i2 = i3 + 1;
        this.C2 |= (bArr[i3] & 255) << 16;
        i3 = i2 + 1;
        this.C2 |= bArr[i2] << 24;
        i = i3 + 1;
        this.C3 = bArr[i3] & 255;
        i3 = i + 1;
        this.C3 = ((bArr[i] & 255) << 8) | this.C3;
        i2 = i3 + 1;
        this.C3 |= (bArr[i3] & 255) << 16;
        this.C3 = (bArr[i2] << 24) | this.C3;
    }

    public String getAlgorithmName() {
        return "AES";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.WorkingKey = generateWorkingKey(((KeyParameter) cipherParameters).getKey(), z);
            this.forEncryption = z;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to AES init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.WorkingKey == null) {
            throw new IllegalStateException("AES engine not initialised");
        } else if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 16 <= bArr2.length) {
            if (this.forEncryption) {
                unpackBlock(bArr, i);
                encryptBlock(this.WorkingKey);
            } else {
                unpackBlock(bArr, i);
                decryptBlock(this.WorkingKey);
            }
            packBlock(bArr2, i2);
            return 16;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
