package org.bouncycastle.pqc.math.linearalgebra;

import java.security.SecureRandom;

public class Permutation {
    private int[] perm;

    public Permutation(int i) {
        if (i > 0) {
            this.perm = new int[i];
            for (i--; i >= 0; i--) {
                this.perm[i] = i;
            }
            return;
        }
        throw new IllegalArgumentException("invalid length");
    }

    public Permutation(int i, SecureRandom secureRandom) {
        if (i > 0) {
            int i2;
            this.perm = new int[i];
            int[] iArr = new int[i];
            int i3 = 0;
            for (i2 = 0; i2 < i; i2++) {
                iArr[i2] = i2;
            }
            i2 = i;
            while (i3 < i) {
                int nextInt = RandUtils.nextInt(secureRandom, i2);
                i2--;
                this.perm[i3] = iArr[nextInt];
                iArr[nextInt] = iArr[i2];
                i3++;
            }
            return;
        }
        throw new IllegalArgumentException("invalid length");
    }

    public Permutation(byte[] bArr) {
        if (bArr.length > 4) {
            int i = 0;
            int OS2IP = LittleEndianConversions.OS2IP(bArr, 0);
            int ceilLog256 = IntegerFunctions.ceilLog256(OS2IP - 1);
            if (bArr.length == (OS2IP * ceilLog256) + 4) {
                this.perm = new int[OS2IP];
                while (i < OS2IP) {
                    this.perm[i] = LittleEndianConversions.OS2IP(bArr, (i * ceilLog256) + 4, ceilLog256);
                    i++;
                }
                if (!isPermutation(this.perm)) {
                    throw new IllegalArgumentException("invalid encoding");
                }
                return;
            }
            throw new IllegalArgumentException("invalid encoding");
        }
        throw new IllegalArgumentException("invalid encoding");
    }

    public Permutation(int[] iArr) {
        if (isPermutation(iArr)) {
            this.perm = IntUtils.clone(iArr);
            return;
        }
        throw new IllegalArgumentException("array is not a permutation vector");
    }

    /* JADX WARNING: Missing block: B:11:0x001e, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isPermutation(int[] iArr) {
        int length = iArr.length;
        boolean[] zArr = new boolean[length];
        int i = 0;
        while (i < length) {
            if (iArr[i] < 0 || iArr[i] >= length || zArr[iArr[i]]) {
                return false;
            }
            zArr[iArr[i]] = true;
            i++;
        }
        return true;
    }

    public Permutation computeInverse() {
        Permutation permutation = new Permutation(this.perm.length);
        for (int length = this.perm.length - 1; length >= 0; length--) {
            permutation.perm[this.perm[length]] = length;
        }
        return permutation;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Permutation)) {
            return false;
        }
        return IntUtils.equals(this.perm, ((Permutation) obj).perm);
    }

    public byte[] getEncoded() {
        int length = this.perm.length;
        int ceilLog256 = IntegerFunctions.ceilLog256(length - 1);
        byte[] bArr = new byte[((length * ceilLog256) + 4)];
        int i = 0;
        LittleEndianConversions.I2OSP(length, bArr, 0);
        while (i < length) {
            LittleEndianConversions.I2OSP(this.perm[i], bArr, (i * ceilLog256) + 4, ceilLog256);
            i++;
        }
        return bArr;
    }

    public int[] getVector() {
        return IntUtils.clone(this.perm);
    }

    public int hashCode() {
        return this.perm.hashCode();
    }

    public Permutation rightMultiply(Permutation permutation) {
        if (permutation.perm.length == this.perm.length) {
            Permutation permutation2 = new Permutation(this.perm.length);
            for (int length = this.perm.length - 1; length >= 0; length--) {
                permutation2.perm[length] = this.perm[permutation.perm[length]];
            }
            return permutation2;
        }
        throw new IllegalArgumentException("length mismatch");
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.perm[0]);
        String stringBuilder2 = stringBuilder.toString();
        for (int i = 1; i < this.perm.length; i++) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(stringBuilder2);
            stringBuilder3.append(", ");
            stringBuilder3.append(this.perm[i]);
            stringBuilder2 = stringBuilder3.toString();
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(stringBuilder2);
        stringBuilder4.append("]");
        return stringBuilder4.toString();
    }
}
