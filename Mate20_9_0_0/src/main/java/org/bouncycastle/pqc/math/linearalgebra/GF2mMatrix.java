package org.bouncycastle.pqc.math.linearalgebra;

import java.lang.reflect.Array;

public class GF2mMatrix extends Matrix {
    protected GF2mField field;
    protected int[][] matrix;

    public GF2mMatrix(GF2mField gF2mField, byte[] bArr) {
        this.field = gF2mField;
        int i = 8;
        int i2 = 1;
        while (gF2mField.getDegree() > i) {
            i2++;
            i += 8;
        }
        if (bArr.length >= 5) {
            this.numRows = ((((bArr[3] & 255) << 24) ^ ((bArr[2] & 255) << 16)) ^ ((bArr[1] & 255) << 8)) ^ (bArr[0] & 255);
            i2 *= this.numRows;
            if (this.numRows > 0) {
                int i3 = 4;
                if ((bArr.length - 4) % i2 == 0) {
                    this.numColumns = (bArr.length - 4) / i2;
                    this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.numColumns});
                    int i4 = 0;
                    while (i4 < this.numRows) {
                        i2 = i3;
                        i3 = 0;
                        while (i3 < this.numColumns) {
                            int i5 = i2;
                            i2 = 0;
                            while (i2 < i) {
                                int[] iArr = this.matrix[i4];
                                int i6 = i5 + 1;
                                iArr[i3] = ((bArr[i5] & 255) << i2) ^ iArr[i3];
                                i2 += 8;
                                i5 = i6;
                            }
                            if (this.field.isElementOfThisField(this.matrix[i4][i3])) {
                                i3++;
                                i2 = i5;
                            } else {
                                throw new IllegalArgumentException(" Error: given array is not encoded matrix over GF(2^m)");
                            }
                        }
                        i4++;
                        i3 = i2;
                    }
                    return;
                }
            }
            throw new IllegalArgumentException(" Error: given array is not encoded matrix over GF(2^m)");
        }
        throw new IllegalArgumentException(" Error: given array is not encoded matrix over GF(2^m)");
    }

    protected GF2mMatrix(GF2mField gF2mField, int[][] iArr) {
        this.field = gF2mField;
        this.matrix = iArr;
        this.numRows = iArr.length;
        this.numColumns = iArr[0].length;
    }

    public GF2mMatrix(GF2mMatrix gF2mMatrix) {
        this.numRows = gF2mMatrix.numRows;
        this.numColumns = gF2mMatrix.numColumns;
        this.field = gF2mMatrix.field;
        this.matrix = new int[this.numRows][];
        for (int i = 0; i < this.numRows; i++) {
            this.matrix[i] = IntUtils.clone(gF2mMatrix.matrix[i]);
        }
    }

    private void addToRow(int[] iArr, int[] iArr2) {
        for (int length = iArr2.length - 1; length >= 0; length--) {
            iArr2[length] = this.field.add(iArr[length], iArr2[length]);
        }
    }

    private int[] multRowWithElement(int[] iArr, int i) {
        int[] iArr2 = new int[iArr.length];
        for (int length = iArr.length - 1; length >= 0; length--) {
            iArr2[length] = this.field.mult(iArr[length], i);
        }
        return iArr2;
    }

    private void multRowWithElementThis(int[] iArr, int i) {
        for (int length = iArr.length - 1; length >= 0; length--) {
            iArr[length] = this.field.mult(iArr[length], i);
        }
    }

    private static void swapColumns(int[][] iArr, int i, int i2) {
        int[] iArr2 = iArr[i];
        iArr[i] = iArr[i2];
        iArr[i2] = iArr2;
    }

    public Matrix computeInverse() {
        if (this.numRows == this.numColumns) {
            int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.numRows});
            for (int i = this.numRows - 1; i >= 0; i--) {
                iArr[i] = IntUtils.clone(this.matrix[i]);
            }
            int[][] iArr2 = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.numRows});
            for (int i2 = this.numRows - 1; i2 >= 0; i2--) {
                iArr2[i2][i2] = 1;
            }
            for (int i3 = 0; i3 < this.numRows; i3++) {
                int i4;
                int i5;
                if (iArr[i3][i3] == 0) {
                    i4 = i3 + 1;
                    i5 = 0;
                    while (i4 < this.numRows) {
                        if (iArr[i4][i3] != 0) {
                            swapColumns(iArr, i3, i4);
                            swapColumns(iArr2, i3, i4);
                            i4 = this.numRows;
                            i5 = 1;
                        }
                        i4++;
                    }
                    if (i5 == 0) {
                        throw new ArithmeticException("Matrix is not invertible.");
                    }
                }
                i4 = this.field.inverse(iArr[i3][i3]);
                multRowWithElementThis(iArr[i3], i4);
                multRowWithElementThis(iArr2[i3], i4);
                for (i4 = 0; i4 < this.numRows; i4++) {
                    if (i4 != i3) {
                        i5 = iArr[i4][i3];
                        if (i5 != 0) {
                            int[] multRowWithElement = multRowWithElement(iArr[i3], i5);
                            int[] multRowWithElement2 = multRowWithElement(iArr2[i3], i5);
                            addToRow(multRowWithElement, iArr[i4]);
                            addToRow(multRowWithElement2, iArr2[i4]);
                        }
                    }
                }
            }
            return new GF2mMatrix(this.field, iArr2);
        }
        throw new ArithmeticException("Matrix is not invertible.");
    }

    /* JADX WARNING: Missing block: B:25:0x0042, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GF2mMatrix)) {
            return false;
        }
        GF2mMatrix gF2mMatrix = (GF2mMatrix) obj;
        if (!this.field.equals(gF2mMatrix.field) || gF2mMatrix.numRows != this.numColumns || gF2mMatrix.numColumns != this.numColumns) {
            return false;
        }
        for (int i = 0; i < this.numRows; i++) {
            for (int i2 = 0; i2 < this.numColumns; i2++) {
                if (this.matrix[i][i2] != gF2mMatrix.matrix[i][i2]) {
                    return false;
                }
            }
        }
        return true;
    }

    public byte[] getEncoded() {
        int i = 8;
        int i2 = 1;
        while (this.field.getDegree() > i) {
            i2++;
            i += 8;
        }
        int i3 = (this.numRows * this.numColumns) * i2;
        i2 = 4;
        byte[] bArr = new byte[(i3 + 4)];
        bArr[0] = (byte) (this.numRows & 255);
        bArr[1] = (byte) ((this.numRows >>> 8) & 255);
        bArr[2] = (byte) ((this.numRows >>> 16) & 255);
        bArr[3] = (byte) ((this.numRows >>> 24) & 255);
        for (int i4 = 0; i4 < this.numRows; i4++) {
            int i5 = 0;
            while (i5 < this.numColumns) {
                int i6 = i2;
                i2 = 0;
                while (i2 < i) {
                    int i7 = i6 + 1;
                    bArr[i6] = (byte) (this.matrix[i4][i5] >>> i2);
                    i2 += 8;
                    i6 = i7;
                }
                i5++;
                i2 = i6;
            }
        }
        return bArr;
    }

    public int hashCode() {
        int hashCode = (((this.field.hashCode() * 31) + this.numRows) * 31) + this.numColumns;
        int i = 0;
        while (i < this.numRows) {
            int i2 = hashCode;
            for (hashCode = 0; hashCode < this.numColumns; hashCode++) {
                i2 = (i2 * 31) + this.matrix[i][hashCode];
            }
            i++;
            hashCode = i2;
        }
        return hashCode;
    }

    public boolean isZero() {
        for (int i = 0; i < this.numRows; i++) {
            for (int i2 = 0; i2 < this.numColumns; i2++) {
                if (this.matrix[i][i2] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public Vector leftMultiply(Vector vector) {
        throw new RuntimeException("Not implemented.");
    }

    public Matrix rightMultiply(Matrix matrix) {
        throw new RuntimeException("Not implemented.");
    }

    public Matrix rightMultiply(Permutation permutation) {
        throw new RuntimeException("Not implemented.");
    }

    public Vector rightMultiply(Vector vector) {
        throw new RuntimeException("Not implemented.");
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.numRows);
        stringBuilder.append(" x ");
        stringBuilder.append(this.numColumns);
        stringBuilder.append(" Matrix over ");
        stringBuilder.append(this.field.toString());
        stringBuilder.append(": \n");
        String stringBuilder2 = stringBuilder.toString();
        for (int i = 0; i < this.numRows; i++) {
            String str = stringBuilder2;
            for (int i2 = 0; i2 < this.numColumns; i2++) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append(this.field.elementToStr(this.matrix[i][i2]));
                stringBuilder3.append(" : ");
                str = stringBuilder3.toString();
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(str);
            stringBuilder4.append("\n");
            stringBuilder2 = stringBuilder4.toString();
        }
        return stringBuilder2;
    }
}
