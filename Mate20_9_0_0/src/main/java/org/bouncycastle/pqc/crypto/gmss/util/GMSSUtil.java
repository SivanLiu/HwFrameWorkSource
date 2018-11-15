package org.bouncycastle.pqc.crypto.gmss.util;

import java.io.PrintStream;

public class GMSSUtil {
    public int bytesToIntLittleEndian(byte[] bArr) {
        return ((bArr[3] & 255) << 24) | (((bArr[0] & 255) | ((bArr[1] & 255) << 8)) | ((bArr[2] & 255) << 16));
    }

    public int bytesToIntLittleEndian(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = i2 + 1;
        i = (bArr[i] & 255) | ((bArr[i2] & 255) << 8);
        return ((bArr[i3 + 1] & 255) << 24) | (i | ((bArr[i3] & 255) << 16));
    }

    public byte[] concatenateArray(byte[][] bArr) {
        Object obj = new byte[(bArr.length * bArr[0].length)];
        int i = 0;
        int i2 = i;
        while (i < bArr.length) {
            System.arraycopy(bArr[i], 0, obj, i2, bArr[i].length);
            i2 += bArr[i].length;
            i++;
        }
        return obj;
    }

    public int getLog(int i) {
        int i2 = 1;
        int i3 = 2;
        while (i3 < i) {
            i3 <<= 1;
            i2++;
        }
        return i2;
    }

    public byte[] intToBytesLittleEndian(int i) {
        return new byte[]{(byte) (i & 255), (byte) ((i >> 8) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 24) & 255)};
    }

    public void printArray(String str, byte[] bArr) {
        System.out.println(str);
        int i = 0;
        int i2 = 0;
        while (i < bArr.length) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(i2);
            stringBuilder.append("; ");
            stringBuilder.append(bArr[i]);
            printStream.println(stringBuilder.toString());
            i2++;
            i++;
        }
    }

    public void printArray(String str, byte[][] bArr) {
        System.out.println(str);
        int i = 0;
        int i2 = i;
        while (i < bArr.length) {
            int i3 = i2;
            for (i2 = 0; i2 < bArr[0].length; i2++) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(i3);
                stringBuilder.append("; ");
                stringBuilder.append(bArr[i][i2]);
                printStream.println(stringBuilder.toString());
                i3++;
            }
            i++;
            i2 = i3;
        }
    }

    public boolean testPowerOfTwo(int i) {
        int i2 = 1;
        while (i2 < i) {
            i2 <<= 1;
        }
        return i == i2;
    }
}
