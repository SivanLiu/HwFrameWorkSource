package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.crypto.tls.CipherSuite;

public final class TwofishEngine implements BlockCipher {
    private static final int BLOCK_SIZE = 16;
    private static final int GF256_FDBK = 361;
    private static final int GF256_FDBK_2 = 180;
    private static final int GF256_FDBK_4 = 90;
    private static final int INPUT_WHITEN = 0;
    private static final int MAX_KEY_BITS = 256;
    private static final int MAX_ROUNDS = 16;
    private static final int OUTPUT_WHITEN = 4;
    private static final byte[][] P = new byte[][]{new byte[]{(byte) -87, (byte) 103, (byte) -77, (byte) -24, (byte) 4, (byte) -3, (byte) -93, (byte) 118, (byte) -102, (byte) -110, Byte.MIN_VALUE, (byte) 120, (byte) -28, (byte) -35, (byte) -47, (byte) 56, (byte) 13, (byte) -58, (byte) 53, (byte) -104, (byte) 24, (byte) -9, (byte) -20, (byte) 108, (byte) 67, (byte) 117, (byte) 55, (byte) 38, (byte) -6, (byte) 19, (byte) -108, (byte) 72, (byte) -14, (byte) -48, (byte) -117, (byte) 48, (byte) -124, (byte) 84, (byte) -33, (byte) 35, (byte) 25, (byte) 91, (byte) 61, (byte) 89, (byte) -13, (byte) -82, (byte) -94, (byte) -126, (byte) 99, (byte) 1, (byte) -125, (byte) 46, (byte) -39, (byte) 81, (byte) -101, (byte) 124, (byte) -90, (byte) -21, (byte) -91, (byte) -66, (byte) 22, (byte) 12, (byte) -29, (byte) 97, (byte) -64, (byte) -116, (byte) 58, (byte) -11, (byte) 115, (byte) 44, (byte) 37, (byte) 11, (byte) -69, (byte) 78, (byte) -119, (byte) 107, (byte) 83, (byte) 106, (byte) -76, (byte) -15, (byte) -31, (byte) -26, (byte) -67, (byte) 69, (byte) -30, (byte) -12, (byte) -74, (byte) 102, (byte) -52, (byte) -107, (byte) 3, (byte) 86, (byte) -44, (byte) 28, (byte) 30, (byte) -41, (byte) -5, (byte) -61, (byte) -114, (byte) -75, (byte) -23, (byte) -49, (byte) -65, (byte) -70, (byte) -22, (byte) 119, (byte) 57, (byte) -81, (byte) 51, (byte) -55, (byte) 98, (byte) 113, (byte) -127, (byte) 121, (byte) 9, (byte) -83, (byte) 36, (byte) -51, (byte) -7, (byte) -40, (byte) -27, (byte) -59, (byte) -71, (byte) 77, (byte) 68, (byte) 8, (byte) -122, (byte) -25, (byte) -95, (byte) 29, (byte) -86, (byte) -19, (byte) 6, (byte) 112, (byte) -78, (byte) -46, (byte) 65, (byte) 123, (byte) -96, (byte) 17, (byte) 49, (byte) -62, (byte) 39, (byte) -112, (byte) 32, (byte) -10, (byte) 96, (byte) -1, (byte) -106, (byte) 92, (byte) -79, (byte) -85, (byte) -98, (byte) -100, (byte) 82, (byte) 27, (byte) 95, (byte) -109, (byte) 10, (byte) -17, (byte) -111, (byte) -123, (byte) 73, (byte) -18, (byte) 45, (byte) 79, (byte) -113, (byte) 59, (byte) 71, (byte) -121, (byte) 109, (byte) 70, (byte) -42, (byte) 62, (byte) 105, (byte) 100, (byte) 42, (byte) -50, (byte) -53, (byte) 47, (byte) -4, (byte) -105, (byte) 5, (byte) 122, (byte) -84, Byte.MAX_VALUE, (byte) -43, (byte) 26, (byte) 75, (byte) 14, (byte) -89, (byte) 90, (byte) 40, (byte) 20, (byte) 63, (byte) 41, (byte) -120, (byte) 60, (byte) 76, (byte) 2, (byte) -72, (byte) -38, (byte) -80, (byte) 23, (byte) 85, (byte) 31, (byte) -118, (byte) 125, (byte) 87, (byte) -57, (byte) -115, (byte) 116, (byte) -73, (byte) -60, (byte) -97, (byte) 114, (byte) 126, (byte) 21, (byte) 34, (byte) 18, (byte) 88, (byte) 7, (byte) -103, (byte) 52, (byte) 110, (byte) 80, (byte) -34, (byte) 104, (byte) 101, PSSSigner.TRAILER_IMPLICIT, (byte) -37, (byte) -8, (byte) -56, (byte) -88, (byte) 43, (byte) 64, (byte) -36, (byte) -2, (byte) 50, (byte) -92, (byte) -54, Tnaf.POW_2_WIDTH, (byte) 33, (byte) -16, (byte) -45, (byte) 93, (byte) 15, (byte) 0, (byte) 111, (byte) -99, (byte) 54, (byte) 66, (byte) 74, (byte) 94, (byte) -63, (byte) -32}, new byte[]{(byte) 117, (byte) -13, (byte) -58, (byte) -12, (byte) -37, (byte) 123, (byte) -5, (byte) -56, (byte) 74, (byte) -45, (byte) -26, (byte) 107, (byte) 69, (byte) 125, (byte) -24, (byte) 75, (byte) -42, (byte) 50, (byte) -40, (byte) -3, (byte) 55, (byte) 113, (byte) -15, (byte) -31, (byte) 48, (byte) 15, (byte) -8, (byte) 27, (byte) -121, (byte) -6, (byte) 6, (byte) 63, (byte) 94, (byte) -70, (byte) -82, (byte) 91, (byte) -118, (byte) 0, PSSSigner.TRAILER_IMPLICIT, (byte) -99, (byte) 109, (byte) -63, (byte) -79, (byte) 14, Byte.MIN_VALUE, (byte) 93, (byte) -46, (byte) -43, (byte) -96, (byte) -124, (byte) 7, (byte) 20, (byte) -75, (byte) -112, (byte) 44, (byte) -93, (byte) -78, (byte) 115, (byte) 76, (byte) 84, (byte) -110, (byte) 116, (byte) 54, (byte) 81, (byte) 56, (byte) -80, (byte) -67, (byte) 90, (byte) -4, (byte) 96, (byte) 98, (byte) -106, (byte) 108, (byte) 66, (byte) -9, Tnaf.POW_2_WIDTH, (byte) 124, (byte) 40, (byte) 39, (byte) -116, (byte) 19, (byte) -107, (byte) -100, (byte) -57, (byte) 36, (byte) 70, (byte) 59, (byte) 112, (byte) -54, (byte) -29, (byte) -123, (byte) -53, (byte) 17, (byte) -48, (byte) -109, (byte) -72, (byte) -90, (byte) -125, (byte) 32, (byte) -1, (byte) -97, (byte) 119, (byte) -61, (byte) -52, (byte) 3, (byte) 111, (byte) 8, (byte) -65, (byte) 64, (byte) -25, (byte) 43, (byte) -30, (byte) 121, (byte) 12, (byte) -86, (byte) -126, (byte) 65, (byte) 58, (byte) -22, (byte) -71, (byte) -28, (byte) -102, (byte) -92, (byte) -105, (byte) 126, (byte) -38, (byte) 122, (byte) 23, (byte) 102, (byte) -108, (byte) -95, (byte) 29, (byte) 61, (byte) -16, (byte) -34, (byte) -77, (byte) 11, (byte) 114, (byte) -89, (byte) 28, (byte) -17, (byte) -47, (byte) 83, (byte) 62, (byte) -113, (byte) 51, (byte) 38, (byte) 95, (byte) -20, (byte) 118, (byte) 42, (byte) 73, (byte) -127, (byte) -120, (byte) -18, (byte) 33, (byte) -60, (byte) 26, (byte) -21, (byte) -39, (byte) -59, (byte) 57, (byte) -103, (byte) -51, (byte) -83, (byte) 49, (byte) -117, (byte) 1, (byte) 24, (byte) 35, (byte) -35, (byte) 31, (byte) 78, (byte) 45, (byte) -7, (byte) 72, (byte) 79, (byte) -14, (byte) 101, (byte) -114, (byte) 120, (byte) 92, (byte) 88, (byte) 25, (byte) -115, (byte) -27, (byte) -104, (byte) 87, (byte) 103, Byte.MAX_VALUE, (byte) 5, (byte) 100, (byte) -81, (byte) 99, (byte) -74, (byte) -2, (byte) -11, (byte) -73, (byte) 60, (byte) -91, (byte) -50, (byte) -23, (byte) 104, (byte) 68, (byte) -32, (byte) 77, (byte) 67, (byte) 105, (byte) 41, (byte) 46, (byte) -84, (byte) 21, (byte) 89, (byte) -88, (byte) 10, (byte) -98, (byte) 110, (byte) 71, (byte) -33, (byte) 52, (byte) 53, (byte) 106, (byte) -49, (byte) -36, (byte) 34, (byte) -55, (byte) -64, (byte) -101, (byte) -119, (byte) -44, (byte) -19, (byte) -85, (byte) 18, (byte) -94, (byte) 13, (byte) 82, (byte) -69, (byte) 2, (byte) 47, (byte) -87, (byte) -41, (byte) 97, (byte) 30, (byte) -76, (byte) 80, (byte) 4, (byte) -10, (byte) -62, (byte) 22, (byte) 37, (byte) -122, (byte) 86, (byte) 85, (byte) 9, (byte) -66, (byte) -111}};
    private static final int P_00 = 1;
    private static final int P_01 = 0;
    private static final int P_02 = 0;
    private static final int P_03 = 1;
    private static final int P_04 = 1;
    private static final int P_10 = 0;
    private static final int P_11 = 0;
    private static final int P_12 = 1;
    private static final int P_13 = 1;
    private static final int P_14 = 0;
    private static final int P_20 = 1;
    private static final int P_21 = 1;
    private static final int P_22 = 0;
    private static final int P_23 = 0;
    private static final int P_24 = 0;
    private static final int P_30 = 0;
    private static final int P_31 = 1;
    private static final int P_32 = 1;
    private static final int P_33 = 0;
    private static final int P_34 = 1;
    private static final int ROUNDS = 16;
    private static final int ROUND_SUBKEYS = 8;
    private static final int RS_GF_FDBK = 333;
    private static final int SK_BUMP = 16843009;
    private static final int SK_ROTL = 9;
    private static final int SK_STEP = 33686018;
    private static final int TOTAL_SUBKEYS = 40;
    private boolean encrypting = false;
    private int[] gMDS0 = new int[256];
    private int[] gMDS1 = new int[256];
    private int[] gMDS2 = new int[256];
    private int[] gMDS3 = new int[256];
    private int[] gSBox;
    private int[] gSubKeys;
    private int k64Cnt = 0;
    private byte[] workingKey = null;

    public TwofishEngine() {
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        int[] iArr3 = new int[2];
        for (int i = 0; i < 256; i++) {
            int i2 = P[0][i] & 255;
            iArr[0] = i2;
            iArr2[0] = Mx_X(i2) & 255;
            iArr3[0] = Mx_Y(i2) & 255;
            i2 = P[1][i] & 255;
            iArr[1] = i2;
            iArr2[1] = Mx_X(i2) & 255;
            iArr3[1] = Mx_Y(i2) & 255;
            this.gMDS0[i] = ((iArr[1] | (iArr2[1] << 8)) | (iArr3[1] << 16)) | (iArr3[1] << 24);
            this.gMDS1[i] = ((iArr3[0] | (iArr3[0] << 8)) | (iArr2[0] << 16)) | (iArr[0] << 24);
            this.gMDS2[i] = (iArr3[1] << 24) | ((iArr2[1] | (iArr3[1] << 8)) | (iArr[1] << 16));
            this.gMDS3[i] = ((iArr2[0] | (iArr[0] << 8)) | (iArr3[0] << 16)) | (iArr2[0] << 24);
        }
    }

    private void Bits32ToBytes(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
        bArr[i2 + 1] = (byte) (i >> 8);
        bArr[i2 + 2] = (byte) (i >> 16);
        bArr[i2 + 3] = (byte) (i >> 24);
    }

    private int BytesTo32Bits(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 24) | (((bArr[i] & 255) | ((bArr[i + 1] & 255) << 8)) | ((bArr[i + 2] & 255) << 16));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:9:0x0150 in {2, 3, 5, 6, 7, 8} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private int F32(int r11, int[] r12) {
        /*
        r10 = this;
        r0 = r10.b0(r11);
        r1 = r10.b1(r11);
        r2 = r10.b2(r11);
        r11 = r10.b3(r11);
        r3 = 0;
        r4 = r12[r3];
        r5 = 1;
        r6 = r12[r5];
        r7 = 2;
        r7 = r12[r7];
        r8 = 3;
        r12 = r12[r8];
        r9 = r10.k64Cnt;
        r8 = r8 & r9;
        switch(r8) {
            case 0: goto L_0x006c;
            case 1: goto L_0x0023;
            case 2: goto L_0x00d4;
            case 3: goto L_0x00a0;
            default: goto L_0x0022;
        };
    L_0x0022:
        return r3;
    L_0x0023:
        r12 = r10.gMDS0;
        r6 = P;
        r6 = r6[r3];
        r0 = r6[r0];
        r0 = r0 & 255;
        r6 = r10.b0(r4);
        r0 = r0 ^ r6;
        r12 = r12[r0];
        r0 = r10.gMDS1;
        r6 = P;
        r3 = r6[r3];
        r1 = r3[r1];
        r1 = r1 & 255;
        r3 = r10.b1(r4);
        r1 = r1 ^ r3;
        r0 = r0[r1];
        r12 = r12 ^ r0;
        r0 = r10.gMDS2;
        r1 = P;
        r1 = r1[r5];
        r1 = r1[r2];
        r1 = r1 & 255;
        r2 = r10.b2(r4);
        r1 = r1 ^ r2;
        r0 = r0[r1];
        r12 = r12 ^ r0;
        r0 = r10.gMDS3;
        r1 = P;
        r1 = r1[r5];
        r11 = r1[r11];
        r11 = r11 & 255;
        r1 = r10.b3(r4);
        r11 = r11 ^ r1;
        r11 = r0[r11];
    L_0x0069:
        r3 = r12 ^ r11;
        return r3;
    L_0x006c:
        r8 = P;
        r8 = r8[r5];
        r0 = r8[r0];
        r0 = r0 & 255;
        r8 = r10.b0(r12);
        r0 = r0 ^ r8;
        r8 = P;
        r8 = r8[r3];
        r1 = r8[r1];
        r1 = r1 & 255;
        r8 = r10.b1(r12);
        r1 = r1 ^ r8;
        r8 = P;
        r8 = r8[r3];
        r2 = r8[r2];
        r2 = r2 & 255;
        r8 = r10.b2(r12);
        r2 = r2 ^ r8;
        r8 = P;
        r8 = r8[r5];
        r11 = r8[r11];
        r11 = r11 & 255;
        r12 = r10.b3(r12);
        r11 = r11 ^ r12;
    L_0x00a0:
        r12 = P;
        r12 = r12[r5];
        r12 = r12[r0];
        r12 = r12 & 255;
        r0 = r10.b0(r7);
        r0 = r0 ^ r12;
        r12 = P;
        r12 = r12[r5];
        r12 = r12[r1];
        r12 = r12 & 255;
        r1 = r10.b1(r7);
        r1 = r1 ^ r12;
        r12 = P;
        r12 = r12[r3];
        r12 = r12[r2];
        r12 = r12 & 255;
        r2 = r10.b2(r7);
        r2 = r2 ^ r12;
        r12 = P;
        r12 = r12[r3];
        r11 = r12[r11];
        r11 = r11 & 255;
        r12 = r10.b3(r7);
        r11 = r11 ^ r12;
    L_0x00d4:
        r12 = r10.gMDS0;
        r7 = P;
        r7 = r7[r3];
        r8 = P;
        r8 = r8[r3];
        r0 = r8[r0];
        r0 = r0 & 255;
        r8 = r10.b0(r6);
        r0 = r0 ^ r8;
        r0 = r7[r0];
        r0 = r0 & 255;
        r7 = r10.b0(r4);
        r0 = r0 ^ r7;
        r12 = r12[r0];
        r0 = r10.gMDS1;
        r7 = P;
        r7 = r7[r3];
        r8 = P;
        r8 = r8[r5];
        r1 = r8[r1];
        r1 = r1 & 255;
        r8 = r10.b1(r6);
        r1 = r1 ^ r8;
        r1 = r7[r1];
        r1 = r1 & 255;
        r7 = r10.b1(r4);
        r1 = r1 ^ r7;
        r0 = r0[r1];
        r12 = r12 ^ r0;
        r0 = r10.gMDS2;
        r1 = P;
        r1 = r1[r5];
        r7 = P;
        r3 = r7[r3];
        r2 = r3[r2];
        r2 = r2 & 255;
        r3 = r10.b2(r6);
        r2 = r2 ^ r3;
        r1 = r1[r2];
        r1 = r1 & 255;
        r2 = r10.b2(r4);
        r1 = r1 ^ r2;
        r0 = r0[r1];
        r12 = r12 ^ r0;
        r0 = r10.gMDS3;
        r1 = P;
        r1 = r1[r5];
        r2 = P;
        r2 = r2[r5];
        r11 = r2[r11];
        r11 = r11 & 255;
        r2 = r10.b3(r6);
        r11 = r11 ^ r2;
        r11 = r1[r11];
        r11 = r11 & 255;
        r1 = r10.b3(r4);
        r11 = r11 ^ r1;
        r11 = r0[r11];
        goto L_0x0069;
        return r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.TwofishEngine.F32(int, int[]):int");
    }

    private int Fe32_0(int i) {
        return this.gSBox[513 + (2 * ((i >>> 24) & 255))] ^ ((this.gSBox[0 + ((i & 255) * 2)] ^ this.gSBox[1 + (((i >>> 8) & 255) * 2)]) ^ this.gSBox[512 + (((i >>> 16) & 255) * 2)]);
    }

    private int Fe32_3(int i) {
        return this.gSBox[513 + (2 * ((i >>> 16) & 255))] ^ ((this.gSBox[0 + (((i >>> 24) & 255) * 2)] ^ this.gSBox[1 + ((i & 255) * 2)]) ^ this.gSBox[512 + (((i >>> 8) & 255) * 2)]);
    }

    private int LFSR1(int i) {
        return ((i & 1) != 0 ? 180 : 0) ^ (i >> 1);
    }

    private int LFSR2(int i) {
        int i2 = 0;
        int i3 = (i >> 2) ^ ((i & 2) != 0 ? 180 : 0);
        if ((i & 1) != 0) {
            i2 = GF256_FDBK_4;
        }
        return i3 ^ i2;
    }

    private int Mx_X(int i) {
        return i ^ LFSR2(i);
    }

    private int Mx_Y(int i) {
        return LFSR2(i) ^ (LFSR1(i) ^ i);
    }

    private int RS_MDS_Encode(int i, int i2) {
        int i3 = 0;
        int i4 = i2;
        for (i2 = 0; i2 < 4; i2++) {
            i4 = RS_rem(i4);
        }
        i ^= i4;
        while (i3 < 4) {
            i = RS_rem(i);
            i3++;
        }
        return i;
    }

    private int RS_rem(int i) {
        int i2 = (i >>> 24) & 255;
        int i3 = 0;
        int i4 = ((i2 << 1) ^ ((i2 & 128) != 0 ? RS_GF_FDBK : 0)) & 255;
        int i5 = i2 >>> 1;
        if ((i2 & 1) != 0) {
            i3 = CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256;
        }
        i5 = (i5 ^ i3) ^ i4;
        return ((((i << 8) ^ (i5 << 24)) ^ (i4 << 16)) ^ (i5 << 8)) ^ i2;
    }

    private int b0(int i) {
        return i & 255;
    }

    private int b1(int i) {
        return (i >>> 8) & 255;
    }

    private int b2(int i) {
        return (i >>> 16) & 255;
    }

    private int b3(int i) {
        return (i >>> 24) & 255;
    }

    private void decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int BytesTo32Bits = BytesTo32Bits(bArr, i) ^ this.gSubKeys[4];
        int BytesTo32Bits2 = BytesTo32Bits(bArr, i + 4) ^ this.gSubKeys[5];
        int i3 = 39;
        int BytesTo32Bits3 = BytesTo32Bits(bArr, i + 8) ^ this.gSubKeys[6];
        int BytesTo32Bits4 = BytesTo32Bits(bArr, i + 12) ^ this.gSubKeys[7];
        int i4 = 0;
        while (i4 < 16) {
            int Fe32_0 = Fe32_0(BytesTo32Bits);
            int Fe32_3 = Fe32_3(BytesTo32Bits2);
            int i5 = i3 - 1;
            BytesTo32Bits4 ^= ((2 * Fe32_3) + Fe32_0) + this.gSubKeys[i3];
            Fe32_0 += Fe32_3;
            Fe32_3 = i5 - 1;
            BytesTo32Bits3 = ((BytesTo32Bits3 << 1) | (BytesTo32Bits3 >>> 31)) ^ (Fe32_0 + this.gSubKeys[i5]);
            BytesTo32Bits4 = (BytesTo32Bits4 << 31) | (BytesTo32Bits4 >>> 1);
            i3 = Fe32_0(BytesTo32Bits3);
            Fe32_0 = Fe32_3(BytesTo32Bits4);
            int i6 = Fe32_3 - 1;
            BytesTo32Bits2 ^= ((2 * Fe32_0) + i3) + this.gSubKeys[Fe32_3];
            BytesTo32Bits = (BytesTo32Bits >>> 31) | (BytesTo32Bits << 1);
            BytesTo32Bits ^= (i3 + Fe32_0) + this.gSubKeys[i6];
            BytesTo32Bits2 = (BytesTo32Bits2 << 31) | (BytesTo32Bits2 >>> 1);
            i4 += 2;
            i3 = i6 - 1;
        }
        Bits32ToBytes(this.gSubKeys[0] ^ BytesTo32Bits3, bArr2, i2);
        Bits32ToBytes(this.gSubKeys[1] ^ BytesTo32Bits4, bArr2, i2 + 4);
        Bits32ToBytes(this.gSubKeys[2] ^ BytesTo32Bits, bArr2, i2 + 8);
        Bits32ToBytes(this.gSubKeys[3] ^ BytesTo32Bits2, bArr2, i2 + 12);
    }

    private void encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3 = 0;
        int BytesTo32Bits = BytesTo32Bits(bArr, i) ^ this.gSubKeys[0];
        int BytesTo32Bits2 = BytesTo32Bits(bArr, i + 4) ^ this.gSubKeys[1];
        int BytesTo32Bits3 = BytesTo32Bits(bArr, i + 8) ^ this.gSubKeys[2];
        int BytesTo32Bits4 = BytesTo32Bits(bArr, i + 12) ^ this.gSubKeys[3];
        i = 8;
        while (i3 < 16) {
            int Fe32_0 = Fe32_0(BytesTo32Bits);
            int Fe32_3 = Fe32_3(BytesTo32Bits2);
            int i4 = i + 1;
            i = BytesTo32Bits3 ^ ((Fe32_0 + Fe32_3) + this.gSubKeys[i]);
            BytesTo32Bits3 = (i >>> 1) | (i << 31);
            Fe32_0 += Fe32_3 * 2;
            Fe32_3 = i4 + 1;
            BytesTo32Bits4 = ((BytesTo32Bits4 >>> 31) | (BytesTo32Bits4 << 1)) ^ (Fe32_0 + this.gSubKeys[i4]);
            i = Fe32_0(BytesTo32Bits3);
            Fe32_0 = Fe32_3(BytesTo32Bits4);
            i4 = Fe32_3 + 1;
            BytesTo32Bits ^= (i + Fe32_0) + this.gSubKeys[Fe32_3];
            BytesTo32Bits = (BytesTo32Bits << 31) | (BytesTo32Bits >>> 1);
            BytesTo32Bits2 = (BytesTo32Bits2 >>> 31) | (BytesTo32Bits2 << 1);
            BytesTo32Bits2 ^= (i + (Fe32_0 * 2)) + this.gSubKeys[i4];
            i3 += 2;
            i = i4 + 1;
        }
        Bits32ToBytes(this.gSubKeys[4] ^ BytesTo32Bits3, bArr2, i2);
        Bits32ToBytes(BytesTo32Bits4 ^ this.gSubKeys[5], bArr2, i2 + 4);
        Bits32ToBytes(this.gSubKeys[6] ^ BytesTo32Bits, bArr2, i2 + 8);
        Bits32ToBytes(this.gSubKeys[7] ^ BytesTo32Bits2, bArr2, i2 + 12);
    }

    private void setKey(byte[] bArr) {
        byte[] bArr2 = bArr;
        int[] iArr = new int[4];
        int[] iArr2 = new int[4];
        int[] iArr3 = new int[4];
        this.gSubKeys = new int[40];
        if (this.k64Cnt < 1) {
            throw new IllegalArgumentException("Key size less than 64 bits");
        } else if (this.k64Cnt <= 4) {
            int i;
            int i2;
            int i3;
            int i4;
            int i5;
            for (i = 0; i < this.k64Cnt; i++) {
                i2 = i * 8;
                iArr[i] = BytesTo32Bits(bArr2, i2);
                iArr2[i] = BytesTo32Bits(bArr2, i2 + 4);
                iArr3[(this.k64Cnt - 1) - i] = RS_MDS_Encode(iArr[i], iArr2[i]);
            }
            for (i3 = 0; i3 < 20; i3++) {
                i4 = SK_STEP * i3;
                i = F32(i4, iArr);
                i4 = F32(i4 + SK_BUMP, iArr2);
                i4 = (i4 >>> 24) | (i4 << 8);
                i += i4;
                i5 = i3 * 2;
                this.gSubKeys[i5] = i;
                i += i4;
                this.gSubKeys[i5 + 1] = (i >>> 23) | (i << 9);
            }
            i3 = iArr3[0];
            i4 = iArr3[1];
            int i6 = iArr3[2];
            int i7 = iArr3[3];
            this.gSBox = new int[1024];
            for (i = 0; i < 256; i++) {
                int b2;
                int b3;
                switch (this.k64Cnt & 3) {
                    case 0:
                        i2 = (P[1][i] & 255) ^ b0(i7);
                        i5 = (P[0][i] & 255) ^ b1(i7);
                        b2 = (P[0][i] & 255) ^ b2(i7);
                        b3 = (P[1][i] & 255) ^ b3(i7);
                        break;
                    case 1:
                        i5 = i * 2;
                        this.gSBox[i5] = this.gMDS0[(P[0][i] & 255) ^ b0(i3)];
                        this.gSBox[i5 + 1] = this.gMDS1[(P[0][i] & 255) ^ b1(i3)];
                        this.gSBox[i5 + 512] = this.gMDS2[(P[1][i] & 255) ^ b2(i3)];
                        this.gSBox[i5 + 513] = this.gMDS3[(P[1][i] & 255) ^ b3(i3)];
                        continue;
                    case 2:
                        i2 = i;
                        i5 = i2;
                        b2 = i5;
                        b3 = b2;
                        break;
                    case 3:
                        i2 = i;
                        i5 = i2;
                        b2 = i5;
                        b3 = b2;
                        break;
                    default:
                        break;
                }
                i2 = (P[1][i2] & 255) ^ b0(i6);
                i5 = (P[1][i5] & 255) ^ b1(i6);
                b2 = (P[0][b2] & 255) ^ b2(i6);
                b3 = (P[0][b3] & 255) ^ b3(i6);
                int i8 = i * 2;
                this.gSBox[i8] = this.gMDS0[(P[0][(P[0][i2] & 255) ^ b0(i4)] & 255) ^ b0(i3)];
                this.gSBox[i8 + 1] = this.gMDS1[(P[0][(P[1][i5] & 255) ^ b1(i4)] & 255) ^ b1(i3)];
                this.gSBox[i8 + 512] = this.gMDS2[(P[1][(P[0][b2] & 255) ^ b2(i4)] & 255) ^ b2(i3)];
                this.gSBox[i8 + 513] = this.gMDS3[(P[1][(P[1][b3] & 255) ^ b3(i4)] & 255) ^ b3(i3)];
            }
        } else {
            throw new IllegalArgumentException("Key size larger than 256 bits");
        }
    }

    public String getAlgorithmName() {
        return "Twofish";
    }

    public int getBlockSize() {
        return 16;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.encrypting = z;
            this.workingKey = ((KeyParameter) cipherParameters).getKey();
            this.k64Cnt = this.workingKey.length / 8;
            setKey(this.workingKey);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to Twofish init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("Twofish not initialised");
        } else if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 16 <= bArr2.length) {
            if (this.encrypting) {
                encryptBlock(bArr, i, bArr2, i2);
            } else {
                decryptBlock(bArr, i, bArr2, i2);
            }
            return 16;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
        if (this.workingKey != null) {
            setKey(this.workingKey);
        }
    }
}
