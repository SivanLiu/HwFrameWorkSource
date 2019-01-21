package org.bouncycastle.pqc.math.linearalgebra;

import java.security.SecureRandom;

public class GF2Vector extends Vector {
    private int[] v;

    public GF2Vector(int i) {
        if (i >= 0) {
            this.length = i;
            this.v = new int[((i + 31) >> 5)];
            return;
        }
        throw new ArithmeticException("Negative length.");
    }

    public GF2Vector(int i, int i2, SecureRandom secureRandom) {
        if (i2 <= i) {
            int i3;
            this.length = i;
            this.v = new int[((i + 31) >> 5)];
            int[] iArr = new int[i];
            int i4 = 0;
            for (i3 = 0; i3 < i; i3++) {
                iArr[i3] = i3;
            }
            while (i4 < i2) {
                i3 = RandUtils.nextInt(secureRandom, i);
                setBit(iArr[i3]);
                i--;
                iArr[i3] = iArr[i];
                i4++;
            }
            return;
        }
        throw new ArithmeticException("The hamming weight is greater than the length of vector.");
    }

    public GF2Vector(int i, SecureRandom secureRandom) {
        this.length = i;
        int i2 = (i + 31) >> 5;
        this.v = new int[i2];
        i2--;
        for (int i3 = i2; i3 >= 0; i3--) {
            this.v[i3] = secureRandom.nextInt();
        }
        i &= 31;
        if (i != 0) {
            int[] iArr = this.v;
            iArr[i2] = ((1 << i) - 1) & iArr[i2];
        }
    }

    public GF2Vector(int i, int[] iArr) {
        if (i >= 0) {
            this.length = i;
            int i2 = (i + 31) >> 5;
            if (iArr.length == i2) {
                this.v = IntUtils.clone(iArr);
                i &= 31;
                if (i != 0) {
                    iArr = this.v;
                    i2--;
                    iArr[i2] = ((1 << i) - 1) & iArr[i2];
                    return;
                }
                return;
            }
            throw new ArithmeticException("length mismatch");
        }
        throw new ArithmeticException("negative length");
    }

    public GF2Vector(GF2Vector gF2Vector) {
        this.length = gF2Vector.length;
        this.v = IntUtils.clone(gF2Vector.v);
    }

    protected GF2Vector(int[] iArr, int i) {
        this.v = iArr;
        this.length = i;
    }

    public static GF2Vector OS2VP(int i, byte[] bArr) {
        if (i >= 0) {
            if (bArr.length <= ((i + 7) >> 3)) {
                return new GF2Vector(i, LittleEndianConversions.toIntArray(bArr));
            }
            throw new ArithmeticException("length mismatch");
        }
        throw new ArithmeticException("negative length");
    }

    public Vector add(Vector vector) {
        if (vector instanceof GF2Vector) {
            GF2Vector gF2Vector = (GF2Vector) vector;
            if (this.length == gF2Vector.length) {
                int[] clone = IntUtils.clone(gF2Vector.v);
                for (int length = clone.length - 1; length >= 0; length--) {
                    clone[length] = clone[length] ^ this.v[length];
                }
                return new GF2Vector(this.length, clone);
            }
            throw new ArithmeticException("length mismatch");
        }
        throw new ArithmeticException("vector is not defined over GF(2)");
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof GF2Vector)) {
            return false;
        }
        GF2Vector gF2Vector = (GF2Vector) obj;
        if (this.length == gF2Vector.length && IntUtils.equals(this.v, gF2Vector.v)) {
            z = true;
        }
        return z;
    }

    public GF2Vector extractLeftVector(int i) {
        if (i > this.length) {
            throw new ArithmeticException("invalid length");
        } else if (i == this.length) {
            return new GF2Vector(this);
        } else {
            GF2Vector gF2Vector = new GF2Vector(i);
            int i2 = i >> 5;
            i &= 31;
            System.arraycopy(this.v, 0, gF2Vector.v, 0, i2);
            if (i != 0) {
                gF2Vector.v[i2] = ((1 << i) - 1) & this.v[i2];
            }
            return gF2Vector;
        }
    }

    public GF2Vector extractRightVector(int i) {
        if (i > this.length) {
            throw new ArithmeticException("invalid length");
        } else if (i == this.length) {
            return new GF2Vector(this);
        } else {
            GF2Vector gF2Vector = new GF2Vector(i);
            int i2 = (this.length - i) >> 5;
            int i3 = (this.length - i) & 31;
            i = (i + 31) >> 5;
            int i4 = 0;
            if (i3 != 0) {
                int i5;
                while (true) {
                    i5 = i - 1;
                    if (i4 >= i5) {
                        break;
                    }
                    int i6 = i2 + 1;
                    gF2Vector.v[i4] = (this.v[i2] >>> i3) | (this.v[i6] << (32 - i3));
                    i4++;
                    i2 = i6;
                }
                int i7 = i2 + 1;
                gF2Vector.v[i5] = this.v[i2] >>> i3;
                if (i7 < this.v.length) {
                    int[] iArr = gF2Vector.v;
                    iArr[i5] = iArr[i5] | (this.v[i7] << (32 - i3));
                    return gF2Vector;
                }
            }
            System.arraycopy(this.v, i2, gF2Vector.v, 0, i);
            return gF2Vector;
        }
    }

    public GF2Vector extractVector(int[] iArr) {
        int length = iArr.length;
        if (iArr[length - 1] <= this.length) {
            GF2Vector gF2Vector = new GF2Vector(length);
            for (int i = 0; i < length; i++) {
                if ((this.v[iArr[i] >> 5] & (1 << (iArr[i] & 31))) != 0) {
                    int[] iArr2 = gF2Vector.v;
                    int i2 = i >> 5;
                    iArr2[i2] = (1 << (i & 31)) | iArr2[i2];
                }
            }
            return gF2Vector;
        }
        throw new ArithmeticException("invalid index set");
    }

    public int getBit(int i) {
        if (i < this.length) {
            int i2 = i >> 5;
            i &= 31;
            return (this.v[i2] & (1 << i)) >>> i;
        }
        throw new IndexOutOfBoundsException();
    }

    public byte[] getEncoded() {
        return LittleEndianConversions.toByteArray(this.v, (this.length + 7) >> 3);
    }

    public int getHammingWeight() {
        int i = 0;
        int i2 = i;
        while (i < this.v.length) {
            int i3 = this.v[i];
            int i4 = i2;
            for (i2 = 0; i2 < 32; i2++) {
                if ((i3 & 1) != 0) {
                    i4++;
                }
                i3 >>>= 1;
            }
            i++;
            i2 = i4;
        }
        return i2;
    }

    public int[] getVecArray() {
        return this.v;
    }

    public int hashCode() {
        return (this.length * 31) + this.v.hashCode();
    }

    public boolean isZero() {
        for (int length = this.v.length - 1; length >= 0; length--) {
            if (this.v[length] != 0) {
                return false;
            }
        }
        return true;
    }

    public Vector multiply(Permutation permutation) {
        int[] vector = permutation.getVector();
        if (this.length == vector.length) {
            GF2Vector gF2Vector = new GF2Vector(this.length);
            for (int i = 0; i < vector.length; i++) {
                if ((this.v[vector[i] >> 5] & (1 << (vector[i] & 31))) != 0) {
                    int[] iArr = gF2Vector.v;
                    int i2 = i >> 5;
                    iArr[i2] = (1 << (i & 31)) | iArr[i2];
                }
            }
            return gF2Vector;
        }
        throw new ArithmeticException("length mismatch");
    }

    public void setBit(int i) {
        if (i < this.length) {
            int[] iArr = this.v;
            int i2 = i >> 5;
            iArr[i2] = (1 << (i & 31)) | iArr[i2];
            return;
        }
        throw new IndexOutOfBoundsException();
    }

    public GF2mVector toExtensionFieldVector(GF2mField gF2mField) {
        int degree = gF2mField.getDegree();
        if (this.length % degree == 0) {
            int i = this.length / degree;
            int[] iArr = new int[i];
            int i2 = 0;
            for (i--; i >= 0; i--) {
                for (int degree2 = gF2mField.getDegree() - 1; degree2 >= 0; degree2--) {
                    if (((this.v[i2 >>> 5] >>> (i2 & 31)) & 1) == 1) {
                        iArr[i] = iArr[i] ^ (1 << degree2);
                    }
                    i2++;
                }
            }
            return new GF2mVector(gF2mField, iArr);
        }
        throw new ArithmeticException("conversion is impossible");
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        int i = 0;
        while (i < this.length) {
            if (i != 0 && (i & 31) == 0) {
                stringBuffer.append(' ');
            }
            stringBuffer.append((this.v[i >> 5] & (1 << (i & 31))) == 0 ? '0' : '1');
            i++;
        }
        return stringBuffer.toString();
    }
}
