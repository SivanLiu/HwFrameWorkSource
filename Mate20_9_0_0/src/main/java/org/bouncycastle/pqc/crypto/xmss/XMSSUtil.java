package org.bouncycastle.pqc.crypto.xmss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class XMSSUtil {
    public static boolean areEqual(byte[][] bArr, byte[][] bArr2) {
        if (hasNullPointer(bArr) || hasNullPointer(bArr2)) {
            throw new NullPointerException("a or b == null");
        }
        for (int i = 0; i < bArr.length; i++) {
            if (!Arrays.areEqual(bArr[i], bArr2[i])) {
                return false;
            }
        }
        return true;
    }

    public static long bytesToXBigEndian(byte[] bArr, int i, int i2) {
        if (bArr != null) {
            long j = 0;
            for (int i3 = i; i3 < i + i2; i3++) {
                j = (j << 8) | ((long) (bArr[i3] & 255));
            }
            return j;
        }
        throw new NullPointerException("in == null");
    }

    public static int calculateTau(int i, int i2) {
        for (int i3 = 0; i3 < i2; i3++) {
            if (((i >> i3) & 1) == 0) {
                return i3;
            }
        }
        return 0;
    }

    public static byte[] cloneArray(byte[] bArr) {
        if (bArr != null) {
            byte[] bArr2 = new byte[bArr.length];
            for (int i = 0; i < bArr.length; i++) {
                bArr2[i] = bArr[i];
            }
            return bArr2;
        }
        throw new NullPointerException("in == null");
    }

    public static byte[][] cloneArray(byte[][] bArr) {
        if (hasNullPointer(bArr)) {
            throw new NullPointerException("in has null pointers");
        }
        byte[][] bArr2 = new byte[bArr.length][];
        for (int i = 0; i < bArr.length; i++) {
            bArr2[i] = new byte[bArr[i].length];
            for (int i2 = 0; i2 < bArr[i].length; i2++) {
                bArr2[i][i2] = bArr[i][i2];
            }
        }
        return bArr2;
    }

    public static void copyBytesAtOffset(byte[] bArr, byte[] bArr2, int i) {
        if (bArr == null) {
            throw new NullPointerException("dst == null");
        } else if (bArr2 == null) {
            throw new NullPointerException("src == null");
        } else if (i < 0) {
            throw new IllegalArgumentException("offset hast to be >= 0");
        } else if (bArr2.length + i <= bArr.length) {
            for (int i2 = 0; i2 < bArr2.length; i2++) {
                bArr[i + i2] = bArr2[i2];
            }
        } else {
            throw new IllegalArgumentException("src length + offset must not be greater than size of destination");
        }
    }

    public static Object deserialize(byte[] bArr) throws IOException, ClassNotFoundException {
        return new ObjectInputStream(new ByteArrayInputStream(bArr)).readObject();
    }

    public static void dumpByteArray(byte[][] bArr) {
        if (hasNullPointer(bArr)) {
            throw new NullPointerException("x has null pointers");
        }
        for (byte[] toHexString : bArr) {
            System.out.println(Hex.toHexString(toHexString));
        }
    }

    public static byte[] extractBytesAtOffset(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException("src == null");
        } else if (i < 0) {
            throw new IllegalArgumentException("offset hast to be >= 0");
        } else if (i2 < 0) {
            throw new IllegalArgumentException("length hast to be >= 0");
        } else if (i + i2 <= bArr.length) {
            byte[] bArr2 = new byte[i2];
            for (int i3 = 0; i3 < bArr2.length; i3++) {
                bArr2[i3] = bArr[i + i3];
            }
            return bArr2;
        } else {
            throw new IllegalArgumentException("offset + length must not be greater then size of source array");
        }
    }

    public static int getDigestSize(Digest digest) {
        if (digest != null) {
            String algorithmName = digest.getAlgorithmName();
            return algorithmName.equals("SHAKE128") ? 32 : algorithmName.equals("SHAKE256") ? 64 : digest.getDigestSize();
        } else {
            throw new NullPointerException("digest == null");
        }
    }

    public static int getLeafIndex(long j, int i) {
        return (int) (j & ((1 << i) - 1));
    }

    public static long getTreeIndex(long j, int i) {
        return j >> i;
    }

    public static boolean hasNullPointer(byte[][] bArr) {
        if (bArr == null) {
            return true;
        }
        for (byte[] bArr2 : bArr) {
            if (bArr2 == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIndexValid(int i, long j) {
        if (j >= 0) {
            return j < (1 << i);
        } else {
            throw new IllegalStateException("index must not be negative");
        }
    }

    public static boolean isNewAuthenticationPathNeeded(long j, int i, int i2) {
        return j == 0 ? false : (j + 1) % ((long) Math.pow((double) (1 << i), (double) i2)) == 0;
    }

    public static boolean isNewBDSInitNeeded(long j, int i, int i2) {
        return j == 0 ? false : j % ((long) Math.pow((double) (1 << i), (double) (i2 + 1))) == 0;
    }

    public static int log2(int i) {
        int i2 = 0;
        while (true) {
            i >>= 1;
            if (i == 0) {
                return i2;
            }
            i2++;
        }
    }

    public static void longToBigEndian(long j, byte[] bArr, int i) {
        if (bArr == null) {
            throw new NullPointerException("in == null");
        } else if (bArr.length - i >= 8) {
            bArr[i] = (byte) ((int) ((j >> 56) & 255));
            bArr[i + 1] = (byte) ((int) ((j >> 48) & 255));
            bArr[i + 2] = (byte) ((int) ((j >> 40) & 255));
            bArr[i + 3] = (byte) ((int) ((j >> 32) & 255));
            bArr[i + 4] = (byte) ((int) ((j >> 24) & 255));
            bArr[i + 5] = (byte) ((int) ((j >> 16) & 255));
            bArr[i + 6] = (byte) ((int) ((j >> 8) & 255));
            bArr[i + 7] = (byte) ((int) (j & 255));
        } else {
            throw new IllegalArgumentException("not enough space in array");
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(obj);
        objectOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] toBytesBigEndian(long j, int i) {
        byte[] bArr = new byte[i];
        for (i--; i >= 0; i--) {
            bArr[i] = (byte) ((int) j);
            j >>>= 8;
        }
        return bArr;
    }
}
