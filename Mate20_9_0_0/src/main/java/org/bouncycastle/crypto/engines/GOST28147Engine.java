package org.bouncycastle.crypto.engines;

import java.util.Enumeration;
import java.util.Hashtable;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithSBox;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class GOST28147Engine implements BlockCipher {
    protected static final int BLOCK_SIZE = 8;
    private static byte[] DSbox_A = new byte[]{(byte) 10, (byte) 4, (byte) 5, (byte) 6, (byte) 8, (byte) 1, (byte) 3, (byte) 7, (byte) 13, (byte) 12, (byte) 14, (byte) 0, (byte) 9, (byte) 2, (byte) 11, (byte) 15, (byte) 5, (byte) 15, (byte) 4, (byte) 0, (byte) 2, (byte) 13, (byte) 11, (byte) 9, (byte) 1, (byte) 7, (byte) 6, (byte) 3, (byte) 12, (byte) 14, (byte) 10, (byte) 8, (byte) 7, (byte) 15, (byte) 12, (byte) 14, (byte) 9, (byte) 4, (byte) 1, (byte) 0, (byte) 3, (byte) 11, (byte) 5, (byte) 2, (byte) 6, (byte) 10, (byte) 8, (byte) 13, (byte) 4, (byte) 10, (byte) 7, (byte) 12, (byte) 0, (byte) 15, (byte) 2, (byte) 8, (byte) 14, (byte) 1, (byte) 6, (byte) 5, (byte) 13, (byte) 11, (byte) 9, (byte) 3, (byte) 7, (byte) 6, (byte) 4, (byte) 11, (byte) 9, (byte) 12, (byte) 2, (byte) 10, (byte) 1, (byte) 8, (byte) 0, (byte) 14, (byte) 15, (byte) 13, (byte) 3, (byte) 5, (byte) 7, (byte) 6, (byte) 2, (byte) 4, (byte) 13, (byte) 9, (byte) 15, (byte) 0, (byte) 10, (byte) 1, (byte) 5, (byte) 11, (byte) 8, (byte) 14, (byte) 12, (byte) 3, (byte) 13, (byte) 14, (byte) 4, (byte) 1, (byte) 7, (byte) 0, (byte) 5, (byte) 10, (byte) 3, (byte) 12, (byte) 8, (byte) 15, (byte) 6, (byte) 2, (byte) 9, (byte) 11, (byte) 1, (byte) 3, (byte) 10, (byte) 9, (byte) 5, (byte) 11, (byte) 4, (byte) 15, (byte) 8, (byte) 6, (byte) 7, (byte) 14, (byte) 13, (byte) 0, (byte) 2, (byte) 12};
    private static byte[] DSbox_Test = new byte[]{(byte) 4, (byte) 10, (byte) 9, (byte) 2, (byte) 13, (byte) 8, (byte) 0, (byte) 14, (byte) 6, (byte) 11, (byte) 1, (byte) 12, (byte) 7, (byte) 15, (byte) 5, (byte) 3, (byte) 14, (byte) 11, (byte) 4, (byte) 12, (byte) 6, (byte) 13, (byte) 15, (byte) 10, (byte) 2, (byte) 3, (byte) 8, (byte) 1, (byte) 0, (byte) 7, (byte) 5, (byte) 9, (byte) 5, (byte) 8, (byte) 1, (byte) 13, (byte) 10, (byte) 3, (byte) 4, (byte) 2, (byte) 14, (byte) 15, (byte) 12, (byte) 7, (byte) 6, (byte) 0, (byte) 9, (byte) 11, (byte) 7, (byte) 13, (byte) 10, (byte) 1, (byte) 0, (byte) 8, (byte) 9, (byte) 15, (byte) 14, (byte) 4, (byte) 6, (byte) 12, (byte) 11, (byte) 2, (byte) 5, (byte) 3, (byte) 6, (byte) 12, (byte) 7, (byte) 1, (byte) 5, (byte) 15, (byte) 13, (byte) 8, (byte) 4, (byte) 10, (byte) 9, (byte) 14, (byte) 0, (byte) 3, (byte) 11, (byte) 2, (byte) 4, (byte) 11, (byte) 10, (byte) 0, (byte) 7, (byte) 2, (byte) 1, (byte) 13, (byte) 3, (byte) 6, (byte) 8, (byte) 5, (byte) 9, (byte) 12, (byte) 15, (byte) 14, (byte) 13, (byte) 11, (byte) 4, (byte) 1, (byte) 3, (byte) 15, (byte) 5, (byte) 9, (byte) 0, (byte) 10, (byte) 14, (byte) 7, (byte) 6, (byte) 8, (byte) 2, (byte) 12, (byte) 1, (byte) 15, (byte) 13, (byte) 0, (byte) 5, (byte) 7, (byte) 10, (byte) 4, (byte) 9, (byte) 2, (byte) 3, (byte) 14, (byte) 6, (byte) 11, (byte) 8, (byte) 12};
    private static byte[] ESbox_A = new byte[]{(byte) 9, (byte) 6, (byte) 3, (byte) 2, (byte) 8, (byte) 11, (byte) 1, (byte) 7, (byte) 10, (byte) 4, (byte) 14, (byte) 15, (byte) 12, (byte) 0, (byte) 13, (byte) 5, (byte) 3, (byte) 7, (byte) 14, (byte) 9, (byte) 8, (byte) 10, (byte) 15, (byte) 0, (byte) 5, (byte) 2, (byte) 6, (byte) 12, (byte) 11, (byte) 4, (byte) 13, (byte) 1, (byte) 14, (byte) 4, (byte) 6, (byte) 2, (byte) 11, (byte) 3, (byte) 13, (byte) 8, (byte) 12, (byte) 15, (byte) 5, (byte) 10, (byte) 0, (byte) 7, (byte) 1, (byte) 9, (byte) 14, (byte) 7, (byte) 10, (byte) 12, (byte) 13, (byte) 1, (byte) 3, (byte) 9, (byte) 0, (byte) 2, (byte) 11, (byte) 4, (byte) 15, (byte) 8, (byte) 5, (byte) 6, (byte) 11, (byte) 5, (byte) 1, (byte) 9, (byte) 8, (byte) 13, (byte) 15, (byte) 0, (byte) 14, (byte) 4, (byte) 2, (byte) 3, (byte) 12, (byte) 7, (byte) 10, (byte) 6, (byte) 3, (byte) 10, (byte) 13, (byte) 12, (byte) 1, (byte) 2, (byte) 0, (byte) 11, (byte) 7, (byte) 5, (byte) 9, (byte) 4, (byte) 8, (byte) 15, (byte) 14, (byte) 6, (byte) 1, (byte) 13, (byte) 2, (byte) 9, (byte) 7, (byte) 10, (byte) 6, (byte) 0, (byte) 8, (byte) 12, (byte) 4, (byte) 5, (byte) 15, (byte) 3, (byte) 11, (byte) 14, (byte) 11, (byte) 10, (byte) 15, (byte) 5, (byte) 0, (byte) 12, (byte) 14, (byte) 8, (byte) 6, (byte) 2, (byte) 3, (byte) 9, (byte) 1, (byte) 7, (byte) 13, (byte) 4};
    private static byte[] ESbox_B = new byte[]{(byte) 8, (byte) 4, (byte) 11, (byte) 1, (byte) 3, (byte) 5, (byte) 0, (byte) 9, (byte) 2, (byte) 14, (byte) 10, (byte) 12, (byte) 13, (byte) 6, (byte) 7, (byte) 15, (byte) 0, (byte) 1, (byte) 2, (byte) 10, (byte) 4, (byte) 13, (byte) 5, (byte) 12, (byte) 9, (byte) 7, (byte) 3, (byte) 15, (byte) 11, (byte) 8, (byte) 6, (byte) 14, (byte) 14, (byte) 12, (byte) 0, (byte) 10, (byte) 9, (byte) 2, (byte) 13, (byte) 11, (byte) 7, (byte) 5, (byte) 8, (byte) 15, (byte) 3, (byte) 6, (byte) 1, (byte) 4, (byte) 7, (byte) 5, (byte) 0, (byte) 13, (byte) 11, (byte) 6, (byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 12, (byte) 15, (byte) 4, (byte) 14, (byte) 9, (byte) 8, (byte) 2, (byte) 7, (byte) 12, (byte) 15, (byte) 9, (byte) 5, (byte) 10, (byte) 11, (byte) 1, (byte) 4, (byte) 0, (byte) 13, (byte) 6, (byte) 8, (byte) 14, (byte) 3, (byte) 8, (byte) 3, (byte) 2, (byte) 6, (byte) 4, (byte) 13, (byte) 14, (byte) 11, (byte) 12, (byte) 1, (byte) 7, (byte) 15, (byte) 10, (byte) 0, (byte) 9, (byte) 5, (byte) 5, (byte) 2, (byte) 10, (byte) 11, (byte) 9, (byte) 1, (byte) 12, (byte) 3, (byte) 7, (byte) 4, (byte) 13, (byte) 0, (byte) 6, (byte) 15, (byte) 8, (byte) 14, (byte) 0, (byte) 4, (byte) 11, (byte) 14, (byte) 8, (byte) 3, (byte) 7, (byte) 1, (byte) 10, (byte) 2, (byte) 9, (byte) 6, (byte) 15, (byte) 13, (byte) 5, (byte) 12};
    private static byte[] ESbox_C = new byte[]{(byte) 1, (byte) 11, (byte) 12, (byte) 2, (byte) 9, (byte) 13, (byte) 0, (byte) 15, (byte) 4, (byte) 5, (byte) 8, (byte) 14, (byte) 10, (byte) 7, (byte) 6, (byte) 3, (byte) 0, (byte) 1, (byte) 7, (byte) 13, (byte) 11, (byte) 4, (byte) 5, (byte) 2, (byte) 8, (byte) 14, (byte) 15, (byte) 12, (byte) 9, (byte) 10, (byte) 6, (byte) 3, (byte) 8, (byte) 2, (byte) 5, (byte) 0, (byte) 4, (byte) 9, (byte) 15, (byte) 10, (byte) 3, (byte) 7, (byte) 12, (byte) 13, (byte) 6, (byte) 14, (byte) 1, (byte) 11, (byte) 3, (byte) 6, (byte) 0, (byte) 1, (byte) 5, (byte) 13, (byte) 10, (byte) 8, (byte) 11, (byte) 2, (byte) 9, (byte) 7, (byte) 14, (byte) 15, (byte) 12, (byte) 4, (byte) 8, (byte) 13, (byte) 11, (byte) 0, (byte) 4, (byte) 5, (byte) 1, (byte) 2, (byte) 9, (byte) 3, (byte) 12, (byte) 14, (byte) 6, (byte) 15, (byte) 10, (byte) 7, (byte) 12, (byte) 9, (byte) 11, (byte) 1, (byte) 8, (byte) 14, (byte) 2, (byte) 4, (byte) 7, (byte) 3, (byte) 6, (byte) 5, (byte) 10, (byte) 0, (byte) 15, (byte) 13, (byte) 10, (byte) 9, (byte) 6, (byte) 8, (byte) 13, (byte) 14, (byte) 2, (byte) 0, (byte) 15, (byte) 3, (byte) 5, (byte) 11, (byte) 4, (byte) 1, (byte) 12, (byte) 7, (byte) 7, (byte) 4, (byte) 0, (byte) 5, (byte) 10, (byte) 2, (byte) 15, (byte) 14, (byte) 12, (byte) 6, (byte) 1, (byte) 11, (byte) 13, (byte) 9, (byte) 3, (byte) 8};
    private static byte[] ESbox_D = new byte[]{(byte) 15, (byte) 12, (byte) 2, (byte) 10, (byte) 6, (byte) 4, (byte) 5, (byte) 0, (byte) 7, (byte) 9, (byte) 14, (byte) 13, (byte) 1, (byte) 11, (byte) 8, (byte) 3, (byte) 11, (byte) 6, (byte) 3, (byte) 4, (byte) 12, (byte) 15, (byte) 14, (byte) 2, (byte) 7, (byte) 13, (byte) 8, (byte) 0, (byte) 5, (byte) 10, (byte) 9, (byte) 1, (byte) 1, (byte) 12, (byte) 11, (byte) 0, (byte) 15, (byte) 14, (byte) 6, (byte) 5, (byte) 10, (byte) 13, (byte) 4, (byte) 8, (byte) 9, (byte) 3, (byte) 7, (byte) 2, (byte) 1, (byte) 5, (byte) 14, (byte) 12, (byte) 10, (byte) 7, (byte) 0, (byte) 13, (byte) 6, (byte) 2, (byte) 11, (byte) 4, (byte) 9, (byte) 3, (byte) 15, (byte) 8, (byte) 0, (byte) 12, (byte) 8, (byte) 9, (byte) 13, (byte) 2, (byte) 10, (byte) 11, (byte) 7, (byte) 3, (byte) 6, (byte) 5, (byte) 4, (byte) 14, (byte) 15, (byte) 1, (byte) 8, (byte) 0, (byte) 15, (byte) 3, (byte) 2, (byte) 5, (byte) 14, (byte) 11, (byte) 1, (byte) 10, (byte) 4, (byte) 7, (byte) 12, (byte) 9, (byte) 13, (byte) 6, (byte) 3, (byte) 0, (byte) 6, (byte) 15, (byte) 1, (byte) 14, (byte) 9, (byte) 2, (byte) 13, (byte) 8, (byte) 12, (byte) 4, (byte) 11, (byte) 10, (byte) 5, (byte) 7, (byte) 1, (byte) 10, (byte) 6, (byte) 8, (byte) 15, (byte) 11, (byte) 0, (byte) 4, (byte) 12, (byte) 3, (byte) 5, (byte) 9, (byte) 7, (byte) 13, (byte) 2, (byte) 14};
    private static byte[] ESbox_Test = new byte[]{(byte) 4, (byte) 2, (byte) 15, (byte) 5, (byte) 9, (byte) 1, (byte) 0, (byte) 8, (byte) 14, (byte) 3, (byte) 11, (byte) 12, (byte) 13, (byte) 7, (byte) 10, (byte) 6, (byte) 12, (byte) 9, (byte) 15, (byte) 14, (byte) 8, (byte) 1, (byte) 3, (byte) 10, (byte) 2, (byte) 7, (byte) 4, (byte) 13, (byte) 6, (byte) 0, (byte) 11, (byte) 5, (byte) 13, (byte) 8, (byte) 14, (byte) 12, (byte) 7, (byte) 3, (byte) 9, (byte) 10, (byte) 1, (byte) 5, (byte) 2, (byte) 4, (byte) 6, (byte) 15, (byte) 0, (byte) 11, (byte) 14, (byte) 9, (byte) 11, (byte) 2, (byte) 5, (byte) 15, (byte) 7, (byte) 1, (byte) 0, (byte) 13, (byte) 12, (byte) 6, (byte) 10, (byte) 4, (byte) 3, (byte) 8, (byte) 3, (byte) 14, (byte) 5, (byte) 9, (byte) 6, (byte) 8, (byte) 0, (byte) 13, (byte) 10, (byte) 11, (byte) 7, (byte) 12, (byte) 2, (byte) 1, (byte) 15, (byte) 4, (byte) 8, (byte) 15, (byte) 6, (byte) 11, (byte) 1, (byte) 9, (byte) 12, (byte) 5, (byte) 13, (byte) 3, (byte) 7, (byte) 10, (byte) 0, (byte) 14, (byte) 2, (byte) 4, (byte) 9, (byte) 11, (byte) 12, (byte) 0, (byte) 3, (byte) 6, (byte) 7, (byte) 5, (byte) 4, (byte) 8, (byte) 14, (byte) 15, (byte) 1, (byte) 10, (byte) 2, (byte) 13, (byte) 12, (byte) 6, (byte) 5, (byte) 2, (byte) 11, (byte) 0, (byte) 9, (byte) 13, (byte) 3, (byte) 14, (byte) 7, (byte) 10, (byte) 15, (byte) 4, (byte) 1, (byte) 8};
    private static byte[] Sbox_Default = new byte[]{(byte) 4, (byte) 10, (byte) 9, (byte) 2, (byte) 13, (byte) 8, (byte) 0, (byte) 14, (byte) 6, (byte) 11, (byte) 1, (byte) 12, (byte) 7, (byte) 15, (byte) 5, (byte) 3, (byte) 14, (byte) 11, (byte) 4, (byte) 12, (byte) 6, (byte) 13, (byte) 15, (byte) 10, (byte) 2, (byte) 3, (byte) 8, (byte) 1, (byte) 0, (byte) 7, (byte) 5, (byte) 9, (byte) 5, (byte) 8, (byte) 1, (byte) 13, (byte) 10, (byte) 3, (byte) 4, (byte) 2, (byte) 14, (byte) 15, (byte) 12, (byte) 7, (byte) 6, (byte) 0, (byte) 9, (byte) 11, (byte) 7, (byte) 13, (byte) 10, (byte) 1, (byte) 0, (byte) 8, (byte) 9, (byte) 15, (byte) 14, (byte) 4, (byte) 6, (byte) 12, (byte) 11, (byte) 2, (byte) 5, (byte) 3, (byte) 6, (byte) 12, (byte) 7, (byte) 1, (byte) 5, (byte) 15, (byte) 13, (byte) 8, (byte) 4, (byte) 10, (byte) 9, (byte) 14, (byte) 0, (byte) 3, (byte) 11, (byte) 2, (byte) 4, (byte) 11, (byte) 10, (byte) 0, (byte) 7, (byte) 2, (byte) 1, (byte) 13, (byte) 3, (byte) 6, (byte) 8, (byte) 5, (byte) 9, (byte) 12, (byte) 15, (byte) 14, (byte) 13, (byte) 11, (byte) 4, (byte) 1, (byte) 3, (byte) 15, (byte) 5, (byte) 9, (byte) 0, (byte) 10, (byte) 14, (byte) 7, (byte) 6, (byte) 8, (byte) 2, (byte) 12, (byte) 1, (byte) 15, (byte) 13, (byte) 0, (byte) 5, (byte) 7, (byte) 10, (byte) 4, (byte) 9, (byte) 2, (byte) 3, (byte) 14, (byte) 6, (byte) 11, (byte) 8, (byte) 12};
    private static Hashtable sBoxes = new Hashtable();
    private byte[] S = Sbox_Default;
    private boolean forEncryption;
    private int[] workingKey = null;

    static {
        addSBox("Default", Sbox_Default);
        addSBox("E-TEST", ESbox_Test);
        addSBox("E-A", ESbox_A);
        addSBox("E-B", ESbox_B);
        addSBox("E-C", ESbox_C);
        addSBox("E-D", ESbox_D);
        addSBox("D-TEST", DSbox_Test);
        addSBox("D-A", DSbox_A);
    }

    private void GOST28147Func(int[] iArr, byte[] bArr, int i, byte[] bArr2, int i2) {
        int bytesToint = bytesToint(bArr, i);
        int bytesToint2 = bytesToint(bArr, i + 4);
        int i3 = 7;
        int GOST28147_mainStep;
        if (this.forEncryption) {
            i = bytesToint2;
            bytesToint2 = 0;
            while (bytesToint2 < 3) {
                int i4 = i;
                i = 0;
                while (i < 8) {
                    i++;
                    GOST28147_mainStep = i4 ^ GOST28147_mainStep(bytesToint, iArr[i]);
                    i4 = bytesToint;
                    bytesToint = GOST28147_mainStep;
                }
                bytesToint2++;
                i = i4;
            }
            bytesToint2 = i;
            i = bytesToint;
            while (i3 > 0) {
                i3--;
                GOST28147_mainStep = i;
                i = bytesToint2 ^ GOST28147_mainStep(i, iArr[i3]);
                bytesToint2 = GOST28147_mainStep;
            }
        } else {
            i = bytesToint2;
            bytesToint2 = 0;
            while (bytesToint2 < 8) {
                bytesToint2++;
                GOST28147_mainStep = bytesToint;
                bytesToint = i ^ GOST28147_mainStep(bytesToint, iArr[bytesToint2]);
                i = GOST28147_mainStep;
            }
            bytesToint2 = 0;
            while (bytesToint2 < 3) {
                int i5 = i;
                i = 7;
                while (i >= 0 && (bytesToint2 != 2 || i != 0)) {
                    i--;
                    GOST28147_mainStep = i5 ^ GOST28147_mainStep(bytesToint, iArr[i]);
                    i5 = bytesToint;
                    bytesToint = GOST28147_mainStep;
                }
                bytesToint2++;
                i = i5;
            }
            bytesToint2 = i;
            i = bytesToint;
        }
        int GOST28147_mainStep2 = GOST28147_mainStep(i, iArr[0]) ^ bytesToint2;
        intTobytes(i, bArr2, i2);
        intTobytes(GOST28147_mainStep2, bArr2, i2 + 4);
    }

    private int GOST28147_mainStep(int i, int i2) {
        i2 += i;
        i = (((((((this.S[((i2 >> 0) & 15) + 0] << 0) + (this.S[((i2 >> 4) & 15) + 16] << 4)) + (this.S[32 + ((i2 >> 8) & 15)] << 8)) + (this.S[48 + ((i2 >> 12) & 15)] << 12)) + (this.S[64 + ((i2 >> 16) & 15)] << 16)) + (this.S[80 + ((i2 >> 20) & 15)] << 20)) + (this.S[96 + ((i2 >> 24) & 15)] << 24)) + (this.S[112 + ((i2 >> 28) & 15)] << 28);
        return (i >>> 21) | (i << 11);
    }

    private static void addSBox(String str, byte[] bArr) {
        sBoxes.put(Strings.toUpperCase(str), bArr);
    }

    private int bytesToint(byte[] bArr, int i) {
        return ((((bArr[i + 3] << 24) & -16777216) + ((bArr[i + 2] << 16) & 16711680)) + ((bArr[i + 1] << 8) & CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB)) + (bArr[i] & 255);
    }

    private int[] generateWorkingKey(boolean z, byte[] bArr) {
        this.forEncryption = z;
        if (bArr.length == 32) {
            int[] iArr = new int[8];
            for (int i = 0; i != 8; i++) {
                iArr[i] = bytesToint(bArr, i * 4);
            }
            return iArr;
        }
        throw new IllegalArgumentException("Key length invalid. Key needs to be 32 byte - 256 bit!!!");
    }

    public static byte[] getSBox(String str) {
        byte[] bArr = (byte[]) sBoxes.get(Strings.toUpperCase(str));
        if (bArr != null) {
            return Arrays.clone(bArr);
        }
        throw new IllegalArgumentException("Unknown S-Box - possible types: \"Default\", \"E-Test\", \"E-A\", \"E-B\", \"E-C\", \"E-D\", \"D-Test\", \"D-A\".");
    }

    public static String getSBoxName(byte[] bArr) {
        Enumeration keys = sBoxes.keys();
        while (keys.hasMoreElements()) {
            String str = (String) keys.nextElement();
            if (Arrays.areEqual((byte[]) sBoxes.get(str), bArr)) {
                return str;
            }
        }
        throw new IllegalArgumentException("SBOX provided did not map to a known one");
    }

    private void intTobytes(int i, byte[] bArr, int i2) {
        bArr[i2 + 3] = (byte) (i >>> 24);
        bArr[i2 + 2] = (byte) (i >>> 16);
        bArr[i2 + 1] = (byte) (i >>> 8);
        bArr[i2] = (byte) i;
    }

    public String getAlgorithmName() {
        return "GOST28147";
    }

    public int getBlockSize() {
        return 8;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof ParametersWithSBox) {
            ParametersWithSBox parametersWithSBox = (ParametersWithSBox) cipherParameters;
            byte[] sBox = parametersWithSBox.getSBox();
            if (sBox.length == Sbox_Default.length) {
                this.S = Arrays.clone(sBox);
                if (parametersWithSBox.getParameters() != null) {
                    cipherParameters = parametersWithSBox.getParameters();
                }
                return;
            }
            throw new IllegalArgumentException("invalid S-box passed to GOST28147 init");
        } else if (!(cipherParameters instanceof KeyParameter)) {
            if (cipherParameters != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid parameter passed to GOST28147 init - ");
                stringBuilder.append(cipherParameters.getClass().getName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            return;
        }
        this.workingKey = generateWorkingKey(z, ((KeyParameter) cipherParameters).getKey());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("GOST28147 engine not initialised");
        } else if (i + 8 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 8 <= bArr2.length) {
            GOST28147Func(this.workingKey, bArr, i, bArr2, i2);
            return 8;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
