package org.bouncycastle.util;

import java.math.BigInteger;
import java.util.NoSuchElementException;

public final class Arrays {

    public static class Iterator<T> implements java.util.Iterator<T> {
        private final T[] dataArray;
        private int position = 0;

        public Iterator(T[] tArr) {
            this.dataArray = tArr;
        }

        public boolean hasNext() {
            return this.position < this.dataArray.length;
        }

        public T next() {
            if (this.position != this.dataArray.length) {
                Object[] objArr = this.dataArray;
                int i = this.position;
                this.position = i + 1;
                return objArr[i];
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Out of elements: ");
            stringBuilder.append(this.position);
            throw new NoSuchElementException(stringBuilder.toString());
        }

        public void remove() {
            throw new UnsupportedOperationException("Cannot remove element from an Array.");
        }
    }

    private Arrays() {
    }

    public static byte[] append(byte[] bArr, byte b) {
        if (bArr == null) {
            return new byte[]{b};
        }
        int length = bArr.length;
        byte[] bArr2 = new byte[(length + 1)];
        System.arraycopy(bArr, 0, bArr2, 0, length);
        bArr2[length] = b;
        return bArr2;
    }

    public static int[] append(int[] iArr, int i) {
        if (iArr == null) {
            return new int[]{i};
        }
        int length = iArr.length;
        int[] iArr2 = new int[(length + 1)];
        System.arraycopy(iArr, 0, iArr2, 0, length);
        iArr2[length] = i;
        return iArr2;
    }

    public static String[] append(String[] strArr, String str) {
        if (strArr == null) {
            return new String[]{str};
        }
        int length = strArr.length;
        String[] strArr2 = new String[(length + 1)];
        System.arraycopy(strArr, 0, strArr2, 0, length);
        strArr2[length] = str;
        return strArr2;
    }

    public static short[] append(short[] sArr, short s) {
        if (sArr == null) {
            return new short[]{s};
        }
        int length = sArr.length;
        short[] sArr2 = new short[(length + 1)];
        System.arraycopy(sArr, 0, sArr2, 0, length);
        sArr2[length] = s;
        return sArr2;
    }

    public static boolean areEqual(byte[] bArr, byte[] bArr2) {
        if (bArr == bArr2) {
            return true;
        }
        if (bArr == null || bArr2 == null || bArr.length != bArr2.length) {
            return false;
        }
        for (int i = 0; i != bArr.length; i++) {
            if (bArr[i] != bArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean areEqual(char[] cArr, char[] cArr2) {
        if (cArr == cArr2) {
            return true;
        }
        if (cArr == null || cArr2 == null || cArr.length != cArr2.length) {
            return false;
        }
        for (int i = 0; i != cArr.length; i++) {
            if (cArr[i] != cArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean areEqual(int[] iArr, int[] iArr2) {
        if (iArr == iArr2) {
            return true;
        }
        if (iArr == null || iArr2 == null || iArr.length != iArr2.length) {
            return false;
        }
        for (int i = 0; i != iArr.length; i++) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean areEqual(long[] jArr, long[] jArr2) {
        if (jArr == jArr2) {
            return true;
        }
        if (jArr == null || jArr2 == null || jArr.length != jArr2.length) {
            return false;
        }
        for (int i = 0; i != jArr.length; i++) {
            if (jArr[i] != jArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean areEqual(Object[] objArr, Object[] objArr2) {
        if (objArr == objArr2) {
            return true;
        }
        if (objArr == null || objArr2 == null || objArr.length != objArr2.length) {
            return false;
        }
        for (int i = 0; i != objArr.length; i++) {
            Object obj = objArr[i];
            Object obj2 = objArr2[i];
            if (obj == null) {
                if (obj2 != null) {
                    return false;
                }
            } else if (!obj.equals(obj2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areEqual(short[] sArr, short[] sArr2) {
        if (sArr == sArr2) {
            return true;
        }
        if (sArr == null || sArr2 == null || sArr.length != sArr2.length) {
            return false;
        }
        for (int i = 0; i != sArr.length; i++) {
            if (sArr[i] != sArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean areEqual(boolean[] zArr, boolean[] zArr2) {
        if (zArr == zArr2) {
            return true;
        }
        if (zArr == null || zArr2 == null || zArr.length != zArr2.length) {
            return false;
        }
        for (int i = 0; i != zArr.length; i++) {
            if (zArr[i] != zArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static void clear(byte[] bArr) {
        if (bArr != null) {
            for (int i = 0; i < bArr.length; i++) {
                bArr[i] = (byte) 0;
            }
        }
    }

    public static byte[] clone(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        byte[] bArr2 = new byte[bArr.length];
        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
        return bArr2;
    }

    public static byte[] clone(byte[] bArr, byte[] bArr2) {
        if (bArr == null) {
            return null;
        }
        if (bArr2 == null || bArr2.length != bArr.length) {
            return clone(bArr);
        }
        System.arraycopy(bArr, 0, bArr2, 0, bArr2.length);
        return bArr2;
    }

    public static char[] clone(char[] cArr) {
        if (cArr == null) {
            return null;
        }
        char[] cArr2 = new char[cArr.length];
        System.arraycopy(cArr, 0, cArr2, 0, cArr.length);
        return cArr2;
    }

    public static int[] clone(int[] iArr) {
        if (iArr == null) {
            return null;
        }
        int[] iArr2 = new int[iArr.length];
        System.arraycopy(iArr, 0, iArr2, 0, iArr.length);
        return iArr2;
    }

    public static long[] clone(long[] jArr) {
        if (jArr == null) {
            return null;
        }
        long[] jArr2 = new long[jArr.length];
        System.arraycopy(jArr, 0, jArr2, 0, jArr.length);
        return jArr2;
    }

    public static long[] clone(long[] jArr, long[] jArr2) {
        if (jArr == null) {
            return null;
        }
        if (jArr2 == null || jArr2.length != jArr.length) {
            return clone(jArr);
        }
        System.arraycopy(jArr, 0, jArr2, 0, jArr2.length);
        return jArr2;
    }

    public static BigInteger[] clone(BigInteger[] bigIntegerArr) {
        if (bigIntegerArr == null) {
            return null;
        }
        BigInteger[] bigIntegerArr2 = new BigInteger[bigIntegerArr.length];
        System.arraycopy(bigIntegerArr, 0, bigIntegerArr2, 0, bigIntegerArr.length);
        return bigIntegerArr2;
    }

    public static short[] clone(short[] sArr) {
        if (sArr == null) {
            return null;
        }
        short[] sArr2 = new short[sArr.length];
        System.arraycopy(sArr, 0, sArr2, 0, sArr.length);
        return sArr2;
    }

    public static byte[][] clone(byte[][] bArr) {
        if (bArr == null) {
            return (byte[][]) null;
        }
        byte[][] bArr2 = new byte[bArr.length][];
        for (int i = 0; i != bArr2.length; i++) {
            bArr2[i] = clone(bArr[i]);
        }
        return bArr2;
    }

    public static byte[][][] clone(byte[][][] bArr) {
        if (bArr == null) {
            return (byte[][][]) null;
        }
        byte[][][] bArr2 = new byte[bArr.length][][];
        for (int i = 0; i != bArr2.length; i++) {
            bArr2[i] = clone(bArr[i]);
        }
        return bArr2;
    }

    public static int compareUnsigned(byte[] bArr, byte[] bArr2) {
        if (bArr == bArr2) {
            return 0;
        }
        if (bArr == null) {
            return -1;
        }
        if (bArr2 == null) {
            return 1;
        }
        int min = Math.min(bArr.length, bArr2.length);
        for (int i = 0; i < min; i++) {
            int i2 = bArr[i] & 255;
            int i3 = bArr2[i] & 255;
            if (i2 < i3) {
                return -1;
            }
            if (i2 > i3) {
                return 1;
            }
        }
        return bArr.length < bArr2.length ? -1 : bArr.length > bArr2.length ? 1 : 0;
    }

    public static byte[] concatenate(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr2 == null) {
            return bArr2 != null ? clone(bArr2) : clone(bArr);
        } else {
            byte[] bArr3 = new byte[(bArr.length + bArr2.length)];
            System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
            System.arraycopy(bArr2, 0, bArr3, bArr.length, bArr2.length);
            return bArr3;
        }
    }

    public static byte[] concatenate(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr == null || bArr2 == null || bArr3 == null) {
            return bArr == null ? concatenate(bArr2, bArr3) : bArr2 == null ? concatenate(bArr, bArr3) : concatenate(bArr, bArr2);
        } else {
            byte[] bArr4 = new byte[((bArr.length + bArr2.length) + bArr3.length)];
            System.arraycopy(bArr, 0, bArr4, 0, bArr.length);
            System.arraycopy(bArr2, 0, bArr4, bArr.length, bArr2.length);
            System.arraycopy(bArr3, 0, bArr4, bArr.length + bArr2.length, bArr3.length);
            return bArr4;
        }
    }

    public static byte[] concatenate(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        if (bArr == null || bArr2 == null || bArr3 == null || bArr4 == null) {
            return bArr4 == null ? concatenate(bArr, bArr2, bArr3) : bArr3 == null ? concatenate(bArr, bArr2, bArr4) : bArr2 == null ? concatenate(bArr, bArr3, bArr4) : concatenate(bArr2, bArr3, bArr4);
        } else {
            byte[] bArr5 = new byte[(((bArr.length + bArr2.length) + bArr3.length) + bArr4.length)];
            System.arraycopy(bArr, 0, bArr5, 0, bArr.length);
            System.arraycopy(bArr2, 0, bArr5, bArr.length, bArr2.length);
            System.arraycopy(bArr3, 0, bArr5, bArr.length + bArr2.length, bArr3.length);
            System.arraycopy(bArr4, 0, bArr5, (bArr.length + bArr2.length) + bArr3.length, bArr4.length);
            return bArr5;
        }
    }

    public static byte[] concatenate(byte[][] bArr) {
        int i = 0;
        int i2 = i;
        while (i != bArr.length) {
            i2 += bArr[i].length;
            i++;
        }
        byte[] bArr2 = new byte[i2];
        i2 = 0;
        int i3 = i2;
        while (i2 != bArr.length) {
            System.arraycopy(bArr[i2], 0, bArr2, i3, bArr[i2].length);
            i3 += bArr[i2].length;
            i2++;
        }
        return bArr2;
    }

    public static int[] concatenate(int[] iArr, int[] iArr2) {
        if (iArr == null) {
            return clone(iArr2);
        }
        if (iArr2 == null) {
            return clone(iArr);
        }
        int[] iArr3 = new int[(iArr.length + iArr2.length)];
        System.arraycopy(iArr, 0, iArr3, 0, iArr.length);
        System.arraycopy(iArr2, 0, iArr3, iArr.length, iArr2.length);
        return iArr3;
    }

    public static boolean constantTimeAreEqual(byte[] bArr, byte[] bArr2) {
        if (bArr == bArr2) {
            return true;
        }
        if (bArr == null || bArr2 == null) {
            return false;
        }
        if (bArr.length != bArr2.length) {
            return constantTimeAreEqual(bArr, bArr) ^ 1;
        }
        int i = 0;
        int i2 = i;
        while (i != bArr.length) {
            i2 |= bArr[i] ^ bArr2[i];
            i++;
        }
        return i2 == 0;
    }

    public static boolean contains(int[] iArr, int i) {
        for (int i2 : iArr) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(short[] sArr, short s) {
        for (short s2 : sArr) {
            if (s2 == s) {
                return true;
            }
        }
        return false;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:5:0x000c in {3, 4} preds:[]
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
    public static byte[] copyOf(byte[] r3, int r4) {
        /*
        r0 = new byte[r4];
        r1 = r3.length;
        r2 = 0;
        if (r4 >= r1) goto L_0x000a;
        java.lang.System.arraycopy(r3, r2, r0, r2, r4);
        return r0;
        r4 = r3.length;
        goto L_0x0006;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.util.Arrays.copyOf(byte[], int):byte[]");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:5:0x000c in {3, 4} preds:[]
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
    public static char[] copyOf(char[] r3, int r4) {
        /*
        r0 = new char[r4];
        r1 = r3.length;
        r2 = 0;
        if (r4 >= r1) goto L_0x000a;
        java.lang.System.arraycopy(r3, r2, r0, r2, r4);
        return r0;
        r4 = r3.length;
        goto L_0x0006;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.util.Arrays.copyOf(char[], int):char[]");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:5:0x000c in {3, 4} preds:[]
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
    public static int[] copyOf(int[] r3, int r4) {
        /*
        r0 = new int[r4];
        r1 = r3.length;
        r2 = 0;
        if (r4 >= r1) goto L_0x000a;
        java.lang.System.arraycopy(r3, r2, r0, r2, r4);
        return r0;
        r4 = r3.length;
        goto L_0x0006;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.util.Arrays.copyOf(int[], int):int[]");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:5:0x000c in {3, 4} preds:[]
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
    public static long[] copyOf(long[] r3, int r4) {
        /*
        r0 = new long[r4];
        r1 = r3.length;
        r2 = 0;
        if (r4 >= r1) goto L_0x000a;
        java.lang.System.arraycopy(r3, r2, r0, r2, r4);
        return r0;
        r4 = r3.length;
        goto L_0x0006;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.util.Arrays.copyOf(long[], int):long[]");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:5:0x000c in {3, 4} preds:[]
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
    public static java.math.BigInteger[] copyOf(java.math.BigInteger[] r3, int r4) {
        /*
        r0 = new java.math.BigInteger[r4];
        r1 = r3.length;
        r2 = 0;
        if (r4 >= r1) goto L_0x000a;
        java.lang.System.arraycopy(r3, r2, r0, r2, r4);
        return r0;
        r4 = r3.length;
        goto L_0x0006;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.util.Arrays.copyOf(java.math.BigInteger[], int):java.math.BigInteger[]");
    }

    public static byte[] copyOfRange(byte[] bArr, int i, int i2) {
        i2 = getLength(i, i2);
        byte[] bArr2 = new byte[i2];
        if (bArr.length - i < i2) {
            i2 = bArr.length - i;
        }
        System.arraycopy(bArr, i, bArr2, 0, i2);
        return bArr2;
    }

    public static int[] copyOfRange(int[] iArr, int i, int i2) {
        i2 = getLength(i, i2);
        int[] iArr2 = new int[i2];
        if (iArr.length - i < i2) {
            i2 = iArr.length - i;
        }
        System.arraycopy(iArr, i, iArr2, 0, i2);
        return iArr2;
    }

    public static long[] copyOfRange(long[] jArr, int i, int i2) {
        i2 = getLength(i, i2);
        long[] jArr2 = new long[i2];
        if (jArr.length - i < i2) {
            i2 = jArr.length - i;
        }
        System.arraycopy(jArr, i, jArr2, 0, i2);
        return jArr2;
    }

    public static BigInteger[] copyOfRange(BigInteger[] bigIntegerArr, int i, int i2) {
        i2 = getLength(i, i2);
        BigInteger[] bigIntegerArr2 = new BigInteger[i2];
        if (bigIntegerArr.length - i < i2) {
            i2 = bigIntegerArr.length - i;
        }
        System.arraycopy(bigIntegerArr, i, bigIntegerArr2, 0, i2);
        return bigIntegerArr2;
    }

    public static void fill(byte[] bArr, byte b) {
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = b;
        }
    }

    public static void fill(byte[] bArr, int i, byte b) {
        if (i < bArr.length) {
            while (i < bArr.length) {
                bArr[i] = b;
                i++;
            }
        }
    }

    public static void fill(char[] cArr, char c) {
        for (int i = 0; i < cArr.length; i++) {
            cArr[i] = c;
        }
    }

    public static void fill(int[] iArr, int i) {
        for (int i2 = 0; i2 < iArr.length; i2++) {
            iArr[i2] = i;
        }
    }

    public static void fill(int[] iArr, int i, int i2) {
        if (i < iArr.length) {
            while (i < iArr.length) {
                iArr[i] = i2;
                i++;
            }
        }
    }

    public static void fill(long[] jArr, int i, long j) {
        if (i < jArr.length) {
            while (i < jArr.length) {
                jArr[i] = j;
                i++;
            }
        }
    }

    public static void fill(long[] jArr, long j) {
        for (int i = 0; i < jArr.length; i++) {
            jArr[i] = j;
        }
    }

    public static void fill(short[] sArr, int i, short s) {
        if (i < sArr.length) {
            while (i < sArr.length) {
                sArr[i] = s;
                i++;
            }
        }
    }

    public static void fill(short[] sArr, short s) {
        for (int i = 0; i < sArr.length; i++) {
            sArr[i] = s;
        }
    }

    private static int getLength(int i, int i2) {
        int i3 = i2 - i;
        if (i3 >= 0) {
            return i3;
        }
        StringBuffer stringBuffer = new StringBuffer(i);
        stringBuffer.append(" > ");
        stringBuffer.append(i2);
        throw new IllegalArgumentException(stringBuffer.toString());
    }

    public static int hashCode(byte[] bArr) {
        if (bArr == null) {
            return 0;
        }
        int length = bArr.length;
        int i = length + 1;
        while (true) {
            length--;
            if (length < 0) {
                return i;
            }
            i = (i * 257) ^ bArr[length];
        }
    }

    public static int hashCode(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            return 0;
        }
        int i3 = i2 + 1;
        while (true) {
            i2--;
            if (i2 < 0) {
                return i3;
            }
            i3 = (i3 * 257) ^ bArr[i + i2];
        }
    }

    public static int hashCode(char[] cArr) {
        if (cArr == null) {
            return 0;
        }
        int length = cArr.length;
        int i = length + 1;
        while (true) {
            length--;
            if (length < 0) {
                return i;
            }
            i = (i * 257) ^ cArr[length];
        }
    }

    public static int hashCode(int[] iArr) {
        if (iArr == null) {
            return 0;
        }
        int length = iArr.length;
        int i = length + 1;
        while (true) {
            length--;
            if (length < 0) {
                return i;
            }
            i = (i * 257) ^ iArr[length];
        }
    }

    public static int hashCode(int[] iArr, int i, int i2) {
        if (iArr == null) {
            return 0;
        }
        int i3 = i2 + 1;
        while (true) {
            i2--;
            if (i2 < 0) {
                return i3;
            }
            i3 = (i3 * 257) ^ iArr[i + i2];
        }
    }

    public static int hashCode(long[] jArr) {
        if (jArr == null) {
            return 0;
        }
        int length = jArr.length;
        int i = length + 1;
        while (true) {
            length--;
            if (length < 0) {
                return i;
            }
            long j = jArr[length];
            i = (((i * 257) ^ ((int) j)) * 257) ^ ((int) (j >>> 32));
        }
    }

    public static int hashCode(long[] jArr, int i, int i2) {
        if (jArr == null) {
            return 0;
        }
        int i3 = i2 + 1;
        while (true) {
            i2--;
            if (i2 < 0) {
                return i3;
            }
            long j = jArr[i + i2];
            i3 = (((i3 * 257) ^ ((int) j)) * 257) ^ ((int) (j >>> 32));
        }
    }

    public static int hashCode(Object[] objArr) {
        if (objArr == null) {
            return 0;
        }
        int length = objArr.length;
        int i = length + 1;
        while (true) {
            length--;
            if (length < 0) {
                return i;
            }
            i = (i * 257) ^ objArr[length].hashCode();
        }
    }

    public static int hashCode(short[] sArr) {
        if (sArr == null) {
            return 0;
        }
        int length = sArr.length;
        int i = length + 1;
        while (true) {
            length--;
            if (length < 0) {
                return i;
            }
            i = (i * 257) ^ (sArr[length] & 255);
        }
    }

    public static int hashCode(int[][] iArr) {
        int i = 0;
        int i2 = 0;
        while (i != iArr.length) {
            i2 = (i2 * 257) + hashCode(iArr[i]);
            i++;
        }
        return i2;
    }

    public static int hashCode(short[][] sArr) {
        int i = 0;
        int i2 = 0;
        while (i != sArr.length) {
            i2 = (i2 * 257) + hashCode(sArr[i]);
            i++;
        }
        return i2;
    }

    public static int hashCode(short[][][] sArr) {
        int i = 0;
        int i2 = 0;
        while (i != sArr.length) {
            i2 = (i2 * 257) + hashCode(sArr[i]);
            i++;
        }
        return i2;
    }

    public static byte[] prepend(byte[] bArr, byte b) {
        if (bArr == null) {
            return new byte[]{b};
        }
        int length = bArr.length;
        byte[] bArr2 = new byte[(length + 1)];
        System.arraycopy(bArr, 0, bArr2, 1, length);
        bArr2[0] = b;
        return bArr2;
    }

    public static int[] prepend(int[] iArr, int i) {
        if (iArr == null) {
            return new int[]{i};
        }
        int length = iArr.length;
        int[] iArr2 = new int[(length + 1)];
        System.arraycopy(iArr, 0, iArr2, 1, length);
        iArr2[0] = i;
        return iArr2;
    }

    public static short[] prepend(short[] sArr, short s) {
        if (sArr == null) {
            return new short[]{s};
        }
        int length = sArr.length;
        short[] sArr2 = new short[(length + 1)];
        System.arraycopy(sArr, 0, sArr2, 1, length);
        sArr2[0] = s;
        return sArr2;
    }

    public static byte[] reverse(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        int i = 0;
        int length = bArr.length;
        byte[] bArr2 = new byte[length];
        while (true) {
            length--;
            if (length < 0) {
                return bArr2;
            }
            int i2 = i + 1;
            bArr2[length] = bArr[i];
            i = i2;
        }
    }

    public static int[] reverse(int[] iArr) {
        if (iArr == null) {
            return null;
        }
        int i = 0;
        int length = iArr.length;
        int[] iArr2 = new int[length];
        while (true) {
            length--;
            if (length < 0) {
                return iArr2;
            }
            int i2 = i + 1;
            iArr2[length] = iArr[i];
            i = i2;
        }
    }
}
