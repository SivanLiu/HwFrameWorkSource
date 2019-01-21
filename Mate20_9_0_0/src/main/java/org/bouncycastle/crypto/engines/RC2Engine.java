package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.signers.PSSSigner;

public class RC2Engine implements BlockCipher {
    private static final int BLOCK_SIZE = 8;
    private static byte[] piTable = new byte[]{(byte) -39, (byte) 120, (byte) -7, (byte) -60, (byte) 25, (byte) -35, (byte) -75, (byte) -19, (byte) 40, (byte) -23, (byte) -3, (byte) 121, (byte) 74, (byte) -96, (byte) -40, (byte) -99, (byte) -58, (byte) 126, (byte) 55, (byte) -125, (byte) 43, (byte) 118, (byte) 83, (byte) -114, (byte) 98, (byte) 76, (byte) 100, (byte) -120, (byte) 68, (byte) -117, (byte) -5, (byte) -94, (byte) 23, (byte) -102, (byte) 89, (byte) -11, (byte) -121, (byte) -77, (byte) 79, (byte) 19, (byte) 97, (byte) 69, (byte) 109, (byte) -115, (byte) 9, (byte) -127, (byte) 125, (byte) 50, (byte) -67, (byte) -113, (byte) 64, (byte) -21, (byte) -122, (byte) -73, (byte) 123, (byte) 11, (byte) -16, (byte) -107, (byte) 33, (byte) 34, (byte) 92, (byte) 107, (byte) 78, (byte) -126, (byte) 84, (byte) -42, (byte) 101, (byte) -109, (byte) -50, (byte) 96, (byte) -78, (byte) 28, (byte) 115, (byte) 86, (byte) -64, (byte) 20, (byte) -89, (byte) -116, (byte) -15, (byte) -36, (byte) 18, (byte) 117, (byte) -54, (byte) 31, (byte) 59, (byte) -66, (byte) -28, (byte) -47, (byte) 66, (byte) 61, (byte) -44, (byte) 48, (byte) -93, (byte) 60, (byte) -74, (byte) 38, (byte) 111, (byte) -65, (byte) 14, (byte) -38, (byte) 70, (byte) 105, (byte) 7, (byte) 87, (byte) 39, (byte) -14, (byte) 29, (byte) -101, PSSSigner.TRAILER_IMPLICIT, (byte) -108, (byte) 67, (byte) 3, (byte) -8, (byte) 17, (byte) -57, (byte) -10, (byte) -112, (byte) -17, (byte) 62, (byte) -25, (byte) 6, (byte) -61, (byte) -43, (byte) 47, (byte) -56, (byte) 102, (byte) 30, (byte) -41, (byte) 8, (byte) -24, (byte) -22, (byte) -34, Byte.MIN_VALUE, (byte) 82, (byte) -18, (byte) -9, (byte) -124, (byte) -86, (byte) 114, (byte) -84, (byte) 53, (byte) 77, (byte) 106, (byte) 42, (byte) -106, (byte) 26, (byte) -46, (byte) 113, (byte) 90, (byte) 21, (byte) 73, (byte) 116, (byte) 75, (byte) -97, (byte) -48, (byte) 94, (byte) 4, (byte) 24, (byte) -92, (byte) -20, (byte) -62, (byte) -32, (byte) 65, (byte) 110, (byte) 15, (byte) 81, (byte) -53, (byte) -52, (byte) 36, (byte) -111, (byte) -81, (byte) 80, (byte) -95, (byte) -12, (byte) 112, (byte) 57, (byte) -103, (byte) 124, (byte) 58, (byte) -123, (byte) 35, (byte) -72, (byte) -76, (byte) 122, (byte) -4, (byte) 2, (byte) 54, (byte) 91, (byte) 37, (byte) 85, (byte) -105, (byte) 49, (byte) 45, (byte) 93, (byte) -6, (byte) -104, (byte) -29, (byte) -118, (byte) -110, (byte) -82, (byte) 5, (byte) -33, (byte) 41, Tnaf.POW_2_WIDTH, (byte) 103, (byte) 108, (byte) -70, (byte) -55, (byte) -45, (byte) 0, (byte) -26, (byte) -49, (byte) -31, (byte) -98, (byte) -88, (byte) 44, (byte) 99, (byte) 22, (byte) 1, (byte) 63, (byte) 88, (byte) -30, (byte) -119, (byte) -87, (byte) 13, (byte) 56, (byte) 52, (byte) 27, (byte) -85, (byte) 51, (byte) -1, (byte) -80, (byte) -69, (byte) 72, (byte) 12, (byte) 95, (byte) -71, (byte) -79, (byte) -51, (byte) 46, (byte) -59, (byte) -13, (byte) -37, (byte) 71, (byte) -27, (byte) -91, (byte) -100, (byte) 119, (byte) 10, (byte) -90, (byte) 32, (byte) 104, (byte) -2, Byte.MAX_VALUE, (byte) -63, (byte) -83};
    private boolean encrypting;
    private int[] workingKey;

    private void decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3;
        int i4 = ((bArr[i + 7] & 255) << 8) + (bArr[i + 6] & 255);
        int i5 = ((bArr[i + 5] & 255) << 8) + (bArr[i + 4] & 255);
        int i6 = ((bArr[i + 3] & 255) << 8) + (bArr[i + 2] & 255);
        int i7 = ((bArr[i + 1] & 255) << 8) + (bArr[i + 0] & 255);
        for (i3 = 60; i3 >= 44; i3 -= 4) {
            i4 = rotateWordLeft(i4, 11) - ((((~i5) & i7) + (i6 & i5)) + this.workingKey[i3 + 3]);
            i5 = rotateWordLeft(i5, 13) - ((((~i6) & i4) + (i7 & i6)) + this.workingKey[i3 + 2]);
            i6 = rotateWordLeft(i6, 14) - ((((~i7) & i5) + (i4 & i7)) + this.workingKey[i3 + 1]);
            i7 = rotateWordLeft(i7, 15) - ((((~i4) & i6) + (i5 & i4)) + this.workingKey[i3]);
        }
        i4 -= this.workingKey[i5 & 63];
        i5 -= this.workingKey[i6 & 63];
        i6 -= this.workingKey[i7 & 63];
        i7 -= this.workingKey[i4 & 63];
        for (i3 = 40; i3 >= 20; i3 -= 4) {
            i4 = rotateWordLeft(i4, 11) - ((((~i5) & i7) + (i6 & i5)) + this.workingKey[i3 + 3]);
            i5 = rotateWordLeft(i5, 13) - ((((~i6) & i4) + (i7 & i6)) + this.workingKey[i3 + 2]);
            i6 = rotateWordLeft(i6, 14) - ((((~i7) & i5) + (i4 & i7)) + this.workingKey[i3 + 1]);
            i7 = rotateWordLeft(i7, 15) - ((((~i4) & i6) + (i5 & i4)) + this.workingKey[i3]);
        }
        i4 -= this.workingKey[i5 & 63];
        i5 -= this.workingKey[i6 & 63];
        i6 -= this.workingKey[i7 & 63];
        i7 -= this.workingKey[i4 & 63];
        for (i3 = 16; i3 >= 0; i3 -= 4) {
            i4 = rotateWordLeft(i4, 11) - ((((~i5) & i7) + (i6 & i5)) + this.workingKey[i3 + 3]);
            i5 = rotateWordLeft(i5, 13) - ((((~i6) & i4) + (i7 & i6)) + this.workingKey[i3 + 2]);
            i6 = rotateWordLeft(i6, 14) - ((((~i7) & i5) + (i4 & i7)) + this.workingKey[i3 + 1]);
            i7 = rotateWordLeft(i7, 15) - ((((~i4) & i6) + (i5 & i4)) + this.workingKey[i3]);
        }
        bArr2[i2 + 0] = (byte) i7;
        bArr2[i2 + 1] = (byte) (i7 >> 8);
        bArr2[i2 + 2] = (byte) i6;
        bArr2[i2 + 3] = (byte) (i6 >> 8);
        bArr2[i2 + 4] = (byte) i5;
        bArr2[i2 + 5] = (byte) (i5 >> 8);
        bArr2[i2 + 6] = (byte) i4;
        bArr2[i2 + 7] = (byte) (i4 >> 8);
    }

    private void encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        int i3;
        int i4 = ((bArr[i + 7] & 255) << 8) + (bArr[i + 6] & 255);
        int i5 = ((bArr[i + 5] & 255) << 8) + (bArr[i + 4] & 255);
        int i6 = ((bArr[i + 3] & 255) << 8) + (bArr[i + 2] & 255);
        int i7 = 0;
        int i8 = ((bArr[i + 1] & 255) << 8) + (bArr[i + 0] & 255);
        while (i7 <= 16) {
            i8 = rotateWordLeft(((i8 + ((~i4) & i6)) + (i5 & i4)) + this.workingKey[i7], 1);
            i6 = rotateWordLeft(((i6 + ((~i8) & i5)) + (i4 & i8)) + this.workingKey[i7 + 1], 2);
            i5 = rotateWordLeft(((i5 + ((~i6) & i4)) + (i8 & i6)) + this.workingKey[i7 + 2], 3);
            i4 = rotateWordLeft(((i4 + ((~i5) & i8)) + (i6 & i5)) + this.workingKey[i7 + 3], 5);
            i7 += 4;
        }
        i8 += this.workingKey[i4 & 63];
        i6 += this.workingKey[i8 & 63];
        i5 += this.workingKey[i6 & 63];
        i4 += this.workingKey[i5 & 63];
        for (i3 = 20; i3 <= 40; i3 += 4) {
            i8 = rotateWordLeft(((i8 + ((~i4) & i6)) + (i5 & i4)) + this.workingKey[i3], 1);
            i6 = rotateWordLeft(((i6 + ((~i8) & i5)) + (i4 & i8)) + this.workingKey[i3 + 1], 2);
            i5 = rotateWordLeft(((i5 + ((~i6) & i4)) + (i8 & i6)) + this.workingKey[i3 + 2], 3);
            i4 = rotateWordLeft(((i4 + ((~i5) & i8)) + (i6 & i5)) + this.workingKey[i3 + 3], 5);
        }
        i8 += this.workingKey[i4 & 63];
        i6 += this.workingKey[i8 & 63];
        i5 += this.workingKey[i6 & 63];
        i4 += this.workingKey[i5 & 63];
        for (i3 = 44; i3 < 64; i3 += 4) {
            i8 = rotateWordLeft(((i8 + ((~i4) & i6)) + (i5 & i4)) + this.workingKey[i3], 1);
            i6 = rotateWordLeft(((i6 + ((~i8) & i5)) + (i4 & i8)) + this.workingKey[i3 + 1], 2);
            i5 = rotateWordLeft(((i5 + ((~i6) & i4)) + (i8 & i6)) + this.workingKey[i3 + 2], 3);
            i4 = rotateWordLeft(((i4 + ((~i5) & i8)) + (i6 & i5)) + this.workingKey[i3 + 3], 5);
        }
        bArr2[i2 + 0] = (byte) i8;
        bArr2[i2 + 1] = (byte) (i8 >> 8);
        bArr2[i2 + 2] = (byte) i6;
        bArr2[i2 + 3] = (byte) (i6 >> 8);
        bArr2[i2 + 4] = (byte) i5;
        bArr2[i2 + 5] = (byte) (i5 >> 8);
        bArr2[i2 + 6] = (byte) i4;
        bArr2[i2 + 7] = (byte) (i4 >> 8);
    }

    private int[] generateWorkingKey(byte[] bArr, int i) {
        int i2;
        int[] iArr = new int[128];
        int i3 = 0;
        for (i2 = 0; i2 != bArr.length; i2++) {
            iArr[i2] = bArr[i2] & 255;
        }
        int length = bArr.length;
        if (length < 128) {
            i2 = iArr[length - 1];
            int i4 = length;
            length = 0;
            while (true) {
                int i5 = length + 1;
                i2 = piTable[(i2 + iArr[length]) & 255] & 255;
                length = i4 + 1;
                iArr[i4] = i2;
                if (length >= 128) {
                    break;
                }
                i4 = length;
                length = i5;
            }
        }
        length = (i + 7) >> 3;
        int i6 = 128 - length;
        i = piTable[(255 >> ((-i) & 7)) & iArr[i6]] & 255;
        iArr[i6] = i;
        for (i6--; i6 >= 0; i6--) {
            i = piTable[i ^ iArr[i6 + length]] & 255;
            iArr[i6] = i;
        }
        int[] iArr2 = new int[64];
        while (i3 != iArr2.length) {
            i = 2 * i3;
            iArr2[i3] = iArr[i] + (iArr[i + 1] << 8);
            i3++;
        }
        return iArr2;
    }

    private int rotateWordLeft(int i, int i2) {
        i &= 65535;
        return (i >> (16 - i2)) | (i << i2);
    }

    public String getAlgorithmName() {
        return "RC2";
    }

    public int getBlockSize() {
        return 8;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0025 in {2, 4, 7, 10} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void init(boolean r3, org.bouncycastle.crypto.CipherParameters r4) {
        /*
        r2 = this;
        r2.encrypting = r3;
        r3 = r4 instanceof org.bouncycastle.crypto.params.RC2Parameters;
        if (r3 == 0) goto L_0x0017;
        r4 = (org.bouncycastle.crypto.params.RC2Parameters) r4;
        r3 = r4.getKey();
        r4 = r4.getEffectiveKeyBits();
        r3 = r2.generateWorkingKey(r3, r4);
        r2.workingKey = r3;
        return;
        r3 = r4 instanceof org.bouncycastle.crypto.params.KeyParameter;
        if (r3 == 0) goto L_0x0026;
        r4 = (org.bouncycastle.crypto.params.KeyParameter) r4;
        r3 = r4.getKey();
        r4 = r3.length;
        r4 = r4 * 8;
        goto L_0x0010;
        return;
        r3 = new java.lang.IllegalArgumentException;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "invalid parameter passed to RC2 init - ";
        r0.append(r1);
        r4 = r4.getClass();
        r4 = r4.getName();
        r0.append(r4);
        r4 = r0.toString();
        r3.<init>(r4);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.RC2Engine.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }

    public final int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("RC2 engine not initialised");
        } else if (i + 8 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 8 <= bArr2.length) {
            if (this.encrypting) {
                encryptBlock(bArr, i, bArr2, i2);
            } else {
                decryptBlock(bArr, i, bArr2, i2);
            }
            return 8;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
