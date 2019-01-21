package org.bouncycastle.pqc.math.linearalgebra;

public final class IntUtils {
    private IntUtils() {
    }

    public static int[] clone(int[] iArr) {
        int[] iArr2 = new int[iArr.length];
        System.arraycopy(iArr, 0, iArr2, 0, iArr.length);
        return iArr2;
    }

    public static boolean equals(int[] iArr, int[] iArr2) {
        if (iArr.length != iArr2.length) {
            return false;
        }
        int i = 1;
        for (int length = iArr.length - 1; length >= 0; length--) {
            i &= iArr[length] == iArr2[length] ? 1 : 0;
        }
        return i;
    }

    public static void fill(int[] iArr, int i) {
        for (int length = iArr.length - 1; length >= 0; length--) {
            iArr[length] = i;
        }
    }

    private static int partition(int[] iArr, int i, int i2, int i3) {
        int i4 = iArr[i3];
        iArr[i3] = iArr[i2];
        iArr[i2] = i4;
        i3 = i;
        while (i < i2) {
            if (iArr[i] <= i4) {
                int i5 = iArr[i3];
                iArr[i3] = iArr[i];
                iArr[i] = i5;
                i3++;
            }
            i++;
        }
        i = iArr[i3];
        iArr[i3] = iArr[i2];
        iArr[i2] = i;
        return i3;
    }

    public static void quicksort(int[] iArr) {
        quicksort(iArr, 0, iArr.length - 1);
    }

    public static void quicksort(int[] iArr, int i, int i2) {
        if (i2 > i) {
            int partition = partition(iArr, i, i2, i2);
            quicksort(iArr, i, partition - 1);
            quicksort(iArr, partition + 1, i2);
        }
    }

    public static int[] subArray(int[] iArr, int i, int i2) {
        i2 -= i;
        int[] iArr2 = new int[i2];
        System.arraycopy(iArr, i, iArr2, 0, i2);
        return iArr2;
    }

    public static String toHexString(int[] iArr) {
        return ByteUtils.toHexString(BigEndianConversions.toByteArray(iArr));
    }

    public static String toString(int[] iArr) {
        String str = "";
        for (int append : iArr) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(append);
            stringBuilder.append(" ");
            str = stringBuilder.toString();
        }
        return str;
    }
}
