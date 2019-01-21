package org.bouncycastle.crypto.engines;

import java.lang.reflect.Array;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.encoders.Hex;

public class ARIAEngine implements BlockCipher {
    protected static final int BLOCK_SIZE = 16;
    private static final byte[][] C = new byte[][]{Hex.decode("517cc1b727220a94fe13abe8fa9a6ee0"), Hex.decode("6db14acc9e21c820ff28b1d5ef5de2b0"), Hex.decode("db92371d2126e9700324977504e8c90e")};
    private static final byte[] SB1_sbox = new byte[]{(byte) 99, (byte) 124, (byte) 119, (byte) 123, (byte) -14, (byte) 107, (byte) 111, (byte) -59, (byte) 48, (byte) 1, (byte) 103, (byte) 43, (byte) -2, (byte) -41, (byte) -85, (byte) 118, (byte) -54, (byte) -126, (byte) -55, (byte) 125, (byte) -6, (byte) 89, (byte) 71, (byte) -16, (byte) -83, (byte) -44, (byte) -94, (byte) -81, (byte) -100, (byte) -92, (byte) 114, (byte) -64, (byte) -73, (byte) -3, (byte) -109, (byte) 38, (byte) 54, (byte) 63, (byte) -9, (byte) -52, (byte) 52, (byte) -91, (byte) -27, (byte) -15, (byte) 113, (byte) -40, (byte) 49, (byte) 21, (byte) 4, (byte) -57, (byte) 35, (byte) -61, (byte) 24, (byte) -106, (byte) 5, (byte) -102, (byte) 7, (byte) 18, Byte.MIN_VALUE, (byte) -30, (byte) -21, (byte) 39, (byte) -78, (byte) 117, (byte) 9, (byte) -125, (byte) 44, (byte) 26, (byte) 27, (byte) 110, (byte) 90, (byte) -96, (byte) 82, (byte) 59, (byte) -42, (byte) -77, (byte) 41, (byte) -29, (byte) 47, (byte) -124, (byte) 83, (byte) -47, (byte) 0, (byte) -19, (byte) 32, (byte) -4, (byte) -79, (byte) 91, (byte) 106, (byte) -53, (byte) -66, (byte) 57, (byte) 74, (byte) 76, (byte) 88, (byte) -49, (byte) -48, (byte) -17, (byte) -86, (byte) -5, (byte) 67, (byte) 77, (byte) 51, (byte) -123, (byte) 69, (byte) -7, (byte) 2, Byte.MAX_VALUE, (byte) 80, (byte) 60, (byte) -97, (byte) -88, (byte) 81, (byte) -93, (byte) 64, (byte) -113, (byte) -110, (byte) -99, (byte) 56, (byte) -11, PSSSigner.TRAILER_IMPLICIT, (byte) -74, (byte) -38, (byte) 33, Tnaf.POW_2_WIDTH, (byte) -1, (byte) -13, (byte) -46, (byte) -51, (byte) 12, (byte) 19, (byte) -20, (byte) 95, (byte) -105, (byte) 68, (byte) 23, (byte) -60, (byte) -89, (byte) 126, (byte) 61, (byte) 100, (byte) 93, (byte) 25, (byte) 115, (byte) 96, (byte) -127, (byte) 79, (byte) -36, (byte) 34, (byte) 42, (byte) -112, (byte) -120, (byte) 70, (byte) -18, (byte) -72, (byte) 20, (byte) -34, (byte) 94, (byte) 11, (byte) -37, (byte) -32, (byte) 50, (byte) 58, (byte) 10, (byte) 73, (byte) 6, (byte) 36, (byte) 92, (byte) -62, (byte) -45, (byte) -84, (byte) 98, (byte) -111, (byte) -107, (byte) -28, (byte) 121, (byte) -25, (byte) -56, (byte) 55, (byte) 109, (byte) -115, (byte) -43, (byte) 78, (byte) -87, (byte) 108, (byte) 86, (byte) -12, (byte) -22, (byte) 101, (byte) 122, (byte) -82, (byte) 8, (byte) -70, (byte) 120, (byte) 37, (byte) 46, (byte) 28, (byte) -90, (byte) -76, (byte) -58, (byte) -24, (byte) -35, (byte) 116, (byte) 31, (byte) 75, (byte) -67, (byte) -117, (byte) -118, (byte) 112, (byte) 62, (byte) -75, (byte) 102, (byte) 72, (byte) 3, (byte) -10, (byte) 14, (byte) 97, (byte) 53, (byte) 87, (byte) -71, (byte) -122, (byte) -63, (byte) 29, (byte) -98, (byte) -31, (byte) -8, (byte) -104, (byte) 17, (byte) 105, (byte) -39, (byte) -114, (byte) -108, (byte) -101, (byte) 30, (byte) -121, (byte) -23, (byte) -50, (byte) 85, (byte) 40, (byte) -33, (byte) -116, (byte) -95, (byte) -119, (byte) 13, (byte) -65, (byte) -26, (byte) 66, (byte) 104, (byte) 65, (byte) -103, (byte) 45, (byte) 15, (byte) -80, (byte) 84, (byte) -69, (byte) 22};
    private static final byte[] SB2_sbox = new byte[]{(byte) -30, (byte) 78, (byte) 84, (byte) -4, (byte) -108, (byte) -62, (byte) 74, (byte) -52, (byte) 98, (byte) 13, (byte) 106, (byte) 70, (byte) 60, (byte) 77, (byte) -117, (byte) -47, (byte) 94, (byte) -6, (byte) 100, (byte) -53, (byte) -76, (byte) -105, (byte) -66, (byte) 43, PSSSigner.TRAILER_IMPLICIT, (byte) 119, (byte) 46, (byte) 3, (byte) -45, (byte) 25, (byte) 89, (byte) -63, (byte) 29, (byte) 6, (byte) 65, (byte) 107, (byte) 85, (byte) -16, (byte) -103, (byte) 105, (byte) -22, (byte) -100, (byte) 24, (byte) -82, (byte) 99, (byte) -33, (byte) -25, (byte) -69, (byte) 0, (byte) 115, (byte) 102, (byte) -5, (byte) -106, (byte) 76, (byte) -123, (byte) -28, (byte) 58, (byte) 9, (byte) 69, (byte) -86, (byte) 15, (byte) -18, Tnaf.POW_2_WIDTH, (byte) -21, (byte) 45, Byte.MAX_VALUE, (byte) -12, (byte) 41, (byte) -84, (byte) -49, (byte) -83, (byte) -111, (byte) -115, (byte) 120, (byte) -56, (byte) -107, (byte) -7, (byte) 47, (byte) -50, (byte) -51, (byte) 8, (byte) 122, (byte) -120, (byte) 56, (byte) 92, (byte) -125, (byte) 42, (byte) 40, (byte) 71, (byte) -37, (byte) -72, (byte) -57, (byte) -109, (byte) -92, (byte) 18, (byte) 83, (byte) -1, (byte) -121, (byte) 14, (byte) 49, (byte) 54, (byte) 33, (byte) 88, (byte) 72, (byte) 1, (byte) -114, (byte) 55, (byte) 116, (byte) 50, (byte) -54, (byte) -23, (byte) -79, (byte) -73, (byte) -85, (byte) 12, (byte) -41, (byte) -60, (byte) 86, (byte) 66, (byte) 38, (byte) 7, (byte) -104, (byte) 96, (byte) -39, (byte) -74, (byte) -71, (byte) 17, (byte) 64, (byte) -20, (byte) 32, (byte) -116, (byte) -67, (byte) -96, (byte) -55, (byte) -124, (byte) 4, (byte) 73, (byte) 35, (byte) -15, (byte) 79, (byte) 80, (byte) 31, (byte) 19, (byte) -36, (byte) -40, (byte) -64, (byte) -98, (byte) 87, (byte) -29, (byte) -61, (byte) 123, (byte) 101, (byte) 59, (byte) 2, (byte) -113, (byte) 62, (byte) -24, (byte) 37, (byte) -110, (byte) -27, (byte) 21, (byte) -35, (byte) -3, (byte) 23, (byte) -87, (byte) -65, (byte) -44, (byte) -102, (byte) 126, (byte) -59, (byte) 57, (byte) 103, (byte) -2, (byte) 118, (byte) -99, (byte) 67, (byte) -89, (byte) -31, (byte) -48, (byte) -11, (byte) 104, (byte) -14, (byte) 27, (byte) 52, (byte) 112, (byte) 5, (byte) -93, (byte) -118, (byte) -43, (byte) 121, (byte) -122, (byte) -88, (byte) 48, (byte) -58, (byte) 81, (byte) 75, (byte) 30, (byte) -90, (byte) 39, (byte) -10, (byte) 53, (byte) -46, (byte) 110, (byte) 36, (byte) 22, (byte) -126, (byte) 95, (byte) -38, (byte) -26, (byte) 117, (byte) -94, (byte) -17, (byte) 44, (byte) -78, (byte) 28, (byte) -97, (byte) 93, (byte) 111, Byte.MIN_VALUE, (byte) 10, (byte) 114, (byte) 68, (byte) -101, (byte) 108, (byte) -112, (byte) 11, (byte) 91, (byte) 51, (byte) 125, (byte) 90, (byte) 82, (byte) -13, (byte) 97, (byte) -95, (byte) -9, (byte) -80, (byte) -42, (byte) 63, (byte) 124, (byte) 109, (byte) -19, (byte) 20, (byte) -32, (byte) -91, (byte) 61, (byte) 34, (byte) -77, (byte) -8, (byte) -119, (byte) -34, (byte) 113, (byte) 26, (byte) -81, (byte) -70, (byte) -75, (byte) -127};
    private static final byte[] SB3_sbox = new byte[]{(byte) 82, (byte) 9, (byte) 106, (byte) -43, (byte) 48, (byte) 54, (byte) -91, (byte) 56, (byte) -65, (byte) 64, (byte) -93, (byte) -98, (byte) -127, (byte) -13, (byte) -41, (byte) -5, (byte) 124, (byte) -29, (byte) 57, (byte) -126, (byte) -101, (byte) 47, (byte) -1, (byte) -121, (byte) 52, (byte) -114, (byte) 67, (byte) 68, (byte) -60, (byte) -34, (byte) -23, (byte) -53, (byte) 84, (byte) 123, (byte) -108, (byte) 50, (byte) -90, (byte) -62, (byte) 35, (byte) 61, (byte) -18, (byte) 76, (byte) -107, (byte) 11, (byte) 66, (byte) -6, (byte) -61, (byte) 78, (byte) 8, (byte) 46, (byte) -95, (byte) 102, (byte) 40, (byte) -39, (byte) 36, (byte) -78, (byte) 118, (byte) 91, (byte) -94, (byte) 73, (byte) 109, (byte) -117, (byte) -47, (byte) 37, (byte) 114, (byte) -8, (byte) -10, (byte) 100, (byte) -122, (byte) 104, (byte) -104, (byte) 22, (byte) -44, (byte) -92, (byte) 92, (byte) -52, (byte) 93, (byte) 101, (byte) -74, (byte) -110, (byte) 108, (byte) 112, (byte) 72, (byte) 80, (byte) -3, (byte) -19, (byte) -71, (byte) -38, (byte) 94, (byte) 21, (byte) 70, (byte) 87, (byte) -89, (byte) -115, (byte) -99, (byte) -124, (byte) -112, (byte) -40, (byte) -85, (byte) 0, (byte) -116, PSSSigner.TRAILER_IMPLICIT, (byte) -45, (byte) 10, (byte) -9, (byte) -28, (byte) 88, (byte) 5, (byte) -72, (byte) -77, (byte) 69, (byte) 6, (byte) -48, (byte) 44, (byte) 30, (byte) -113, (byte) -54, (byte) 63, (byte) 15, (byte) 2, (byte) -63, (byte) -81, (byte) -67, (byte) 3, (byte) 1, (byte) 19, (byte) -118, (byte) 107, (byte) 58, (byte) -111, (byte) 17, (byte) 65, (byte) 79, (byte) 103, (byte) -36, (byte) -22, (byte) -105, (byte) -14, (byte) -49, (byte) -50, (byte) -16, (byte) -76, (byte) -26, (byte) 115, (byte) -106, (byte) -84, (byte) 116, (byte) 34, (byte) -25, (byte) -83, (byte) 53, (byte) -123, (byte) -30, (byte) -7, (byte) 55, (byte) -24, (byte) 28, (byte) 117, (byte) -33, (byte) 110, (byte) 71, (byte) -15, (byte) 26, (byte) 113, (byte) 29, (byte) 41, (byte) -59, (byte) -119, (byte) 111, (byte) -73, (byte) 98, (byte) 14, (byte) -86, (byte) 24, (byte) -66, (byte) 27, (byte) -4, (byte) 86, (byte) 62, (byte) 75, (byte) -58, (byte) -46, (byte) 121, (byte) 32, (byte) -102, (byte) -37, (byte) -64, (byte) -2, (byte) 120, (byte) -51, (byte) 90, (byte) -12, (byte) 31, (byte) -35, (byte) -88, (byte) 51, (byte) -120, (byte) 7, (byte) -57, (byte) 49, (byte) -79, (byte) 18, Tnaf.POW_2_WIDTH, (byte) 89, (byte) 39, Byte.MIN_VALUE, (byte) -20, (byte) 95, (byte) 96, (byte) 81, Byte.MAX_VALUE, (byte) -87, (byte) 25, (byte) -75, (byte) 74, (byte) 13, (byte) 45, (byte) -27, (byte) 122, (byte) -97, (byte) -109, (byte) -55, (byte) -100, (byte) -17, (byte) -96, (byte) -32, (byte) 59, (byte) 77, (byte) -82, (byte) 42, (byte) -11, (byte) -80, (byte) -56, (byte) -21, (byte) -69, (byte) 60, (byte) -125, (byte) 83, (byte) -103, (byte) 97, (byte) 23, (byte) 43, (byte) 4, (byte) 126, (byte) -70, (byte) 119, (byte) -42, (byte) 38, (byte) -31, (byte) 105, (byte) 20, (byte) 99, (byte) 85, (byte) 33, (byte) 12, (byte) 125};
    private static final byte[] SB4_sbox = new byte[]{(byte) 48, (byte) 104, (byte) -103, (byte) 27, (byte) -121, (byte) -71, (byte) 33, (byte) 120, (byte) 80, (byte) 57, (byte) -37, (byte) -31, (byte) 114, (byte) 9, (byte) 98, (byte) 60, (byte) 62, (byte) 126, (byte) 94, (byte) -114, (byte) -15, (byte) -96, (byte) -52, (byte) -93, (byte) 42, (byte) 29, (byte) -5, (byte) -74, (byte) -42, (byte) 32, (byte) -60, (byte) -115, (byte) -127, (byte) 101, (byte) -11, (byte) -119, (byte) -53, (byte) -99, (byte) 119, (byte) -58, (byte) 87, (byte) 67, (byte) 86, (byte) 23, (byte) -44, (byte) 64, (byte) 26, (byte) 77, (byte) -64, (byte) 99, (byte) 108, (byte) -29, (byte) -73, (byte) -56, (byte) 100, (byte) 106, (byte) 83, (byte) -86, (byte) 56, (byte) -104, (byte) 12, (byte) -12, (byte) -101, (byte) -19, Byte.MAX_VALUE, (byte) 34, (byte) 118, (byte) -81, (byte) -35, (byte) 58, (byte) 11, (byte) 88, (byte) 103, (byte) -120, (byte) 6, (byte) -61, (byte) 53, (byte) 13, (byte) 1, (byte) -117, (byte) -116, (byte) -62, (byte) -26, (byte) 95, (byte) 2, (byte) 36, (byte) 117, (byte) -109, (byte) 102, (byte) 30, (byte) -27, (byte) -30, (byte) 84, (byte) -40, Tnaf.POW_2_WIDTH, (byte) -50, (byte) 122, (byte) -24, (byte) 8, (byte) 44, (byte) 18, (byte) -105, (byte) 50, (byte) -85, (byte) -76, (byte) 39, (byte) 10, (byte) 35, (byte) -33, (byte) -17, (byte) -54, (byte) -39, (byte) -72, (byte) -6, (byte) -36, (byte) 49, (byte) 107, (byte) -47, (byte) -83, (byte) 25, (byte) 73, (byte) -67, (byte) 81, (byte) -106, (byte) -18, (byte) -28, (byte) -88, (byte) 65, (byte) -38, (byte) -1, (byte) -51, (byte) 85, (byte) -122, (byte) 54, (byte) -66, (byte) 97, (byte) 82, (byte) -8, (byte) -69, (byte) 14, (byte) -126, (byte) 72, (byte) 105, (byte) -102, (byte) -32, (byte) 71, (byte) -98, (byte) 92, (byte) 4, (byte) 75, (byte) 52, (byte) 21, (byte) 121, (byte) 38, (byte) -89, (byte) -34, (byte) 41, (byte) -82, (byte) -110, (byte) -41, (byte) -124, (byte) -23, (byte) -46, (byte) -70, (byte) 93, (byte) -13, (byte) -59, (byte) -80, (byte) -65, (byte) -92, (byte) 59, (byte) 113, (byte) 68, (byte) 70, (byte) 43, (byte) -4, (byte) -21, (byte) 111, (byte) -43, (byte) -10, (byte) 20, (byte) -2, (byte) 124, (byte) 112, (byte) 90, (byte) 125, (byte) -3, (byte) 47, (byte) 24, (byte) -125, (byte) 22, (byte) -91, (byte) -111, (byte) 31, (byte) 5, (byte) -107, (byte) 116, (byte) -87, (byte) -63, (byte) 91, (byte) 74, (byte) -123, (byte) 109, (byte) 19, (byte) 7, (byte) 79, (byte) 78, (byte) 69, (byte) -78, (byte) 15, (byte) -55, (byte) 28, (byte) -90, PSSSigner.TRAILER_IMPLICIT, (byte) -20, (byte) 115, (byte) -112, (byte) 123, (byte) -49, (byte) 89, (byte) -113, (byte) -95, (byte) -7, (byte) 45, (byte) -14, (byte) -79, (byte) 0, (byte) -108, (byte) 55, (byte) -97, (byte) -48, (byte) 46, (byte) -100, (byte) 110, (byte) 40, (byte) 63, Byte.MIN_VALUE, (byte) -16, (byte) 61, (byte) -45, (byte) 37, (byte) -118, (byte) -75, (byte) -25, (byte) 66, (byte) -77, (byte) -57, (byte) -22, (byte) -9, (byte) 76, (byte) 17, (byte) 51, (byte) 3, (byte) -94, (byte) -84, (byte) 96};
    private byte[][] roundKeys;

    protected static void A(byte[] bArr) {
        byte b = bArr[0];
        byte b2 = bArr[1];
        byte b3 = bArr[2];
        byte b4 = bArr[3];
        byte b5 = bArr[4];
        byte b6 = bArr[5];
        byte b7 = bArr[6];
        byte b8 = bArr[7];
        byte b9 = bArr[8];
        byte b10 = bArr[9];
        byte b11 = bArr[10];
        byte b12 = bArr[11];
        byte b13 = bArr[12];
        byte b14 = bArr[13];
        byte b15 = bArr[14];
        byte b16 = bArr[15];
        bArr[0] = (byte) ((((((b4 ^ b5) ^ b7) ^ b9) ^ b10) ^ b14) ^ b15);
        bArr[1] = (byte) ((((((b3 ^ b6) ^ b8) ^ b9) ^ b10) ^ b13) ^ b16);
        bArr[2] = (byte) ((((((b2 ^ b5) ^ b7) ^ b11) ^ b12) ^ b13) ^ b16);
        bArr[3] = (byte) ((((((b ^ b6) ^ b8) ^ b11) ^ b12) ^ b14) ^ b15);
        int i = b ^ b3;
        bArr[4] = (byte) (((((i ^ b6) ^ b9) ^ b12) ^ b15) ^ b16);
        int i2 = b2 ^ b4;
        bArr[5] = (byte) (((((i2 ^ b5) ^ b10) ^ b11) ^ b15) ^ b16);
        bArr[6] = (byte) (((((i ^ b8) ^ b10) ^ b11) ^ b13) ^ b14);
        bArr[7] = (byte) (((((i2 ^ b7) ^ b9) ^ b12) ^ b13) ^ b14);
        i = b ^ b2;
        bArr[8] = (byte) (((((i ^ b5) ^ b8) ^ b11) ^ b14) ^ b16);
        bArr[9] = (byte) (((((i ^ b6) ^ b7) ^ b12) ^ b13) ^ b15);
        i = b3 ^ b4;
        bArr[10] = (byte) (((((i ^ b6) ^ b7) ^ b9) ^ b14) ^ b16);
        bArr[11] = (byte) (((((i ^ b5) ^ b8) ^ b10) ^ b13) ^ b15);
        i = b2 ^ b3;
        bArr[12] = (byte) (((((i ^ b7) ^ b8) ^ b10) ^ b12) ^ b13);
        int i3 = b ^ b4;
        bArr[13] = (byte) (((((i3 ^ b7) ^ b8) ^ b9) ^ b11) ^ b14);
        bArr[14] = (byte) (((((i3 ^ b5) ^ b6) ^ b10) ^ b12) ^ b15);
        bArr[15] = (byte) (((((i ^ b5) ^ b6) ^ b9) ^ b11) ^ b16);
    }

    protected static void FE(byte[] bArr, byte[] bArr2) {
        xor(bArr, bArr2);
        SL2(bArr);
        A(bArr);
    }

    protected static void FO(byte[] bArr, byte[] bArr2) {
        xor(bArr, bArr2);
        SL1(bArr);
        A(bArr);
    }

    protected static byte SB1(byte b) {
        return SB1_sbox[b & 255];
    }

    protected static byte SB2(byte b) {
        return SB2_sbox[b & 255];
    }

    protected static byte SB3(byte b) {
        return SB3_sbox[b & 255];
    }

    protected static byte SB4(byte b) {
        return SB4_sbox[b & 255];
    }

    protected static void SL1(byte[] bArr) {
        bArr[0] = SB1(bArr[0]);
        bArr[1] = SB2(bArr[1]);
        bArr[2] = SB3(bArr[2]);
        bArr[3] = SB4(bArr[3]);
        bArr[4] = SB1(bArr[4]);
        bArr[5] = SB2(bArr[5]);
        bArr[6] = SB3(bArr[6]);
        bArr[7] = SB4(bArr[7]);
        bArr[8] = SB1(bArr[8]);
        bArr[9] = SB2(bArr[9]);
        bArr[10] = SB3(bArr[10]);
        bArr[11] = SB4(bArr[11]);
        bArr[12] = SB1(bArr[12]);
        bArr[13] = SB2(bArr[13]);
        bArr[14] = SB3(bArr[14]);
        bArr[15] = SB4(bArr[15]);
    }

    protected static void SL2(byte[] bArr) {
        bArr[0] = SB3(bArr[0]);
        bArr[1] = SB4(bArr[1]);
        bArr[2] = SB1(bArr[2]);
        bArr[3] = SB2(bArr[3]);
        bArr[4] = SB3(bArr[4]);
        bArr[5] = SB4(bArr[5]);
        bArr[6] = SB1(bArr[6]);
        bArr[7] = SB2(bArr[7]);
        bArr[8] = SB3(bArr[8]);
        bArr[9] = SB4(bArr[9]);
        bArr[10] = SB1(bArr[10]);
        bArr[11] = SB2(bArr[11]);
        bArr[12] = SB3(bArr[12]);
        bArr[13] = SB4(bArr[13]);
        bArr[14] = SB1(bArr[14]);
        bArr[15] = SB2(bArr[15]);
    }

    protected static byte[][] keySchedule(boolean z, byte[] bArr) {
        int length = bArr.length;
        if (length < 16 || length > 32 || (length & 7) != 0) {
            throw new IllegalArgumentException("Key length not 128/192/256 bits.");
        }
        int i = (length >>> 3) - 2;
        byte[] bArr2 = C[i];
        byte[] bArr3 = C[(i + 1) % 3];
        byte[] bArr4 = C[(i + 2) % 3];
        byte[] bArr5 = new byte[16];
        byte[] bArr6 = new byte[16];
        System.arraycopy(bArr, 0, bArr5, 0, 16);
        System.arraycopy(bArr, 16, bArr6, 0, length - 16);
        bArr = new byte[16];
        byte[] bArr7 = new byte[16];
        byte[] bArr8 = new byte[16];
        byte[] bArr9 = new byte[16];
        System.arraycopy(bArr5, 0, bArr, 0, 16);
        System.arraycopy(bArr, 0, bArr7, 0, 16);
        FO(bArr7, bArr2);
        xor(bArr7, bArr6);
        System.arraycopy(bArr7, 0, bArr8, 0, 16);
        FE(bArr8, bArr3);
        xor(bArr8, bArr);
        System.arraycopy(bArr8, 0, bArr9, 0, 16);
        FO(bArr9, bArr4);
        xor(bArr9, bArr7);
        i = (i * 2) + 12;
        byte[][] bArr10 = (byte[][]) Array.newInstance(byte.class, new int[]{i + 1, 16});
        keyScheduleRound(bArr10[0], bArr, bArr7, 19);
        int i2 = 1;
        keyScheduleRound(bArr10[1], bArr7, bArr8, 19);
        keyScheduleRound(bArr10[2], bArr8, bArr9, 19);
        keyScheduleRound(bArr10[3], bArr9, bArr, 19);
        keyScheduleRound(bArr10[4], bArr, bArr7, 31);
        keyScheduleRound(bArr10[5], bArr7, bArr8, 31);
        keyScheduleRound(bArr10[6], bArr8, bArr9, 31);
        keyScheduleRound(bArr10[7], bArr9, bArr, 31);
        keyScheduleRound(bArr10[8], bArr, bArr7, 67);
        keyScheduleRound(bArr10[9], bArr7, bArr8, 67);
        keyScheduleRound(bArr10[10], bArr8, bArr9, 67);
        keyScheduleRound(bArr10[11], bArr9, bArr, 67);
        keyScheduleRound(bArr10[12], bArr, bArr7, 97);
        if (i > 12) {
            keyScheduleRound(bArr10[13], bArr7, bArr8, 97);
            keyScheduleRound(bArr10[14], bArr8, bArr9, 97);
            if (i > 14) {
                keyScheduleRound(bArr10[15], bArr9, bArr, 97);
                keyScheduleRound(bArr10[16], bArr, bArr7, CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256);
            }
        }
        if (!z) {
            reverseKeys(bArr10);
            while (i2 < i) {
                A(bArr10[i2]);
                i2++;
            }
        }
        return bArr10;
    }

    protected static void keyScheduleRound(byte[] bArr, byte[] bArr2, byte[] bArr3, int i) {
        int i2 = i >>> 3;
        i &= 7;
        int i3 = 8 - i;
        int i4 = bArr3[15 - i2] & 255;
        int i5 = 0;
        while (i5 < 16) {
            int i6 = bArr3[(i5 - i2) & 15] & 255;
            bArr[i5] = (byte) (((i4 << i3) | (i6 >>> i)) ^ (bArr2[i5] & 255));
            i5++;
            i4 = i6;
        }
    }

    protected static void reverseKeys(byte[][] bArr) {
        int length = bArr.length;
        int i = length / 2;
        length--;
        for (int i2 = 0; i2 < i; i2++) {
            byte[] bArr2 = bArr[i2];
            int i3 = length - i2;
            bArr[i2] = bArr[i3];
            bArr[i3] = bArr2;
        }
    }

    protected static void xor(byte[] bArr, byte[] bArr2) {
        for (int i = 0; i < 16; i++) {
            bArr[i] = (byte) (bArr[i] ^ bArr2[i]);
        }
    }

    public String getAlgorithmName() {
        return "ARIA";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof KeyParameter) {
            this.roundKeys = keySchedule(z, ((KeyParameter) cipherParameters).getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to ARIA init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.roundKeys == null) {
            throw new IllegalStateException("ARIA engine not initialised");
        } else if (i > bArr.length - 16) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 <= bArr2.length - 16) {
            int i3;
            byte[] bArr3 = new byte[16];
            System.arraycopy(bArr, i, bArr3, 0, 16);
            int length = this.roundKeys.length - 3;
            i = 0;
            while (i < length) {
                int i4 = i + 1;
                FO(bArr3, this.roundKeys[i]);
                i3 = i4 + 1;
                FE(bArr3, this.roundKeys[i4]);
                i = i3;
            }
            i3 = i + 1;
            FO(bArr3, this.roundKeys[i]);
            i = i3 + 1;
            xor(bArr3, this.roundKeys[i3]);
            SL2(bArr3);
            xor(bArr3, this.roundKeys[i]);
            System.arraycopy(bArr3, 0, bArr2, i2, 16);
            return 16;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
