package org.bouncycastle.pqc.math.linearalgebra;

import java.lang.reflect.Array;
import java.security.SecureRandom;

public class GF2Matrix extends Matrix {
    private int length;
    private int[][] matrix;

    public GF2Matrix(int i, char c) {
        this(i, c, new SecureRandom());
    }

    public GF2Matrix(int i, char c, SecureRandom secureRandom) {
        if (i <= 0) {
            throw new ArithmeticException("Size of matrix is non-positive.");
        } else if (c == 'I') {
            assignUnitMatrix(i);
        } else if (c == Matrix.MATRIX_TYPE_RANDOM_LT) {
            assignRandomLowerTriangularMatrix(i, secureRandom);
        } else if (c == Matrix.MATRIX_TYPE_RANDOM_REGULAR) {
            assignRandomRegularMatrix(i, secureRandom);
        } else if (c == Matrix.MATRIX_TYPE_RANDOM_UT) {
            assignRandomUpperTriangularMatrix(i, secureRandom);
        } else if (c == Matrix.MATRIX_TYPE_ZERO) {
            assignZeroMatrix(i, i);
        } else {
            throw new ArithmeticException("Unknown matrix type.");
        }
    }

    private GF2Matrix(int i, int i2) {
        if (i2 <= 0 || i <= 0) {
            throw new ArithmeticException("size of matrix is non-positive");
        }
        assignZeroMatrix(i, i2);
    }

    public GF2Matrix(int i, int[][] iArr) {
        int i2 = 0;
        if (iArr[0].length == ((i + 31) >> 5)) {
            this.numColumns = i;
            this.numRows = iArr.length;
            this.length = iArr[0].length;
            i &= 31;
            i = i == 0 ? -1 : (1 << i) - 1;
            while (i2 < this.numRows) {
                int[] iArr2 = iArr[i2];
                int i3 = this.length - 1;
                iArr2[i3] = iArr2[i3] & i;
                i2++;
            }
            this.matrix = iArr;
            return;
        }
        throw new ArithmeticException("Int array does not match given number of columns.");
    }

    public GF2Matrix(GF2Matrix gF2Matrix) {
        this.numColumns = gF2Matrix.getNumColumns();
        this.numRows = gF2Matrix.getNumRows();
        this.length = gF2Matrix.length;
        this.matrix = new int[gF2Matrix.matrix.length][];
        for (int i = 0; i < this.matrix.length; i++) {
            this.matrix[i] = IntUtils.clone(gF2Matrix.matrix[i]);
        }
    }

    public GF2Matrix(byte[] bArr) {
        if (bArr.length >= 9) {
            this.numRows = LittleEndianConversions.OS2IP(bArr, 0);
            this.numColumns = LittleEndianConversions.OS2IP(bArr, 4);
            int i = ((this.numColumns + 7) >>> 3) * this.numRows;
            if (this.numRows <= 0 || i != bArr.length - 8) {
                throw new ArithmeticException("given array is not an encoded matrix over GF(2)");
            }
            this.length = (this.numColumns + 31) >>> 5;
            this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
            i = this.numColumns >> 5;
            int i2 = this.numColumns & 31;
            int i3 = 8;
            int i4 = 0;
            while (i4 < this.numRows) {
                int i5 = i3;
                i3 = 0;
                while (i3 < i) {
                    this.matrix[i4][i3] = LittleEndianConversions.OS2IP(bArr, i5);
                    i3++;
                    i5 += 4;
                }
                i3 = 0;
                while (i3 < i2) {
                    int[] iArr = this.matrix[i4];
                    int i6 = i5 + 1;
                    iArr[i] = ((bArr[i5] & 255) << i3) ^ iArr[i];
                    i3 += 8;
                    i5 = i6;
                }
                i4++;
                i3 = i5;
            }
            return;
        }
        throw new ArithmeticException("given array is not an encoded matrix over GF(2)");
    }

    private static void addToRow(int[] iArr, int[] iArr2, int i) {
        for (int length = iArr2.length - 1; length >= i; length--) {
            iArr2[length] = iArr[length] ^ iArr2[length];
        }
    }

    private void assignRandomLowerTriangularMatrix(int i, SecureRandom secureRandom) {
        this.numRows = i;
        this.numColumns = i;
        this.length = (i + 31) >>> 5;
        this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
        for (int i2 = 0; i2 < this.numRows; i2++) {
            int i3 = i2 >>> 5;
            int i4 = i2 & 31;
            int i5 = 31 - i4;
            i4 = 1 << i4;
            for (int i6 = 0; i6 < i3; i6++) {
                this.matrix[i2][i6] = secureRandom.nextInt();
            }
            this.matrix[i2][i3] = i4 | (secureRandom.nextInt() >>> i5);
            while (true) {
                i3++;
                if (i3 >= this.length) {
                    break;
                }
                this.matrix[i2][i3] = 0;
            }
        }
    }

    private void assignRandomRegularMatrix(int i, SecureRandom secureRandom) {
        this.numRows = i;
        this.numColumns = i;
        this.length = (i + 31) >>> 5;
        this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
        GF2Matrix gF2Matrix = (GF2Matrix) new GF2Matrix(i, Matrix.MATRIX_TYPE_RANDOM_LT, secureRandom).rightMultiply(new GF2Matrix(i, Matrix.MATRIX_TYPE_RANDOM_UT, secureRandom));
        int[] vector = new Permutation(i, secureRandom).getVector();
        for (int i2 = 0; i2 < i; i2++) {
            System.arraycopy(gF2Matrix.matrix[i2], 0, this.matrix[vector[i2]], 0, this.length);
        }
    }

    private void assignRandomUpperTriangularMatrix(int i, SecureRandom secureRandom) {
        this.numRows = i;
        this.numColumns = i;
        this.length = (i + 31) >>> 5;
        this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
        i &= 31;
        i = i == 0 ? -1 : (1 << i) - 1;
        for (int i2 = 0; i2 < this.numRows; i2++) {
            int i3 = i2 >>> 5;
            int i4 = i2 & 31;
            int i5 = 1 << i4;
            for (int i6 = 0; i6 < i3; i6++) {
                this.matrix[i2][i6] = 0;
            }
            this.matrix[i2][i3] = (secureRandom.nextInt() << i4) | i5;
            while (true) {
                i3++;
                if (i3 >= this.length) {
                    break;
                }
                this.matrix[i2][i3] = secureRandom.nextInt();
            }
            int[] iArr = this.matrix[i2];
            i4 = this.length - 1;
            iArr[i4] = iArr[i4] & i;
        }
    }

    private void assignUnitMatrix(int i) {
        this.numRows = i;
        this.numColumns = i;
        this.length = (i + 31) >>> 5;
        this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
        i = 0;
        for (int i2 = 0; i2 < this.numRows; i2++) {
            for (int i3 = 0; i3 < this.length; i3++) {
                this.matrix[i2][i3] = 0;
            }
        }
        while (i < this.numRows) {
            this.matrix[i][i >>> 5] = 1 << (i & 31);
            i++;
        }
    }

    private void assignZeroMatrix(int i, int i2) {
        this.numRows = i;
        this.numColumns = i2;
        this.length = (i2 + 31) >>> 5;
        this.matrix = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
        for (i2 = 0; i2 < this.numRows; i2++) {
            for (int i3 = 0; i3 < this.length; i3++) {
                this.matrix[i2][i3] = 0;
            }
        }
    }

    public static GF2Matrix[] createRandomRegularMatrixAndItsInverse(int i, SecureRandom secureRandom) {
        int i2;
        int i3;
        int i4 = i;
        SecureRandom secureRandom2 = secureRandom;
        GF2Matrix[] gF2MatrixArr = new GF2Matrix[2];
        int i5 = (i4 + 31) >> 5;
        GF2Matrix gF2Matrix = new GF2Matrix(i4, Matrix.MATRIX_TYPE_RANDOM_LT, secureRandom2);
        Matrix gF2Matrix2 = new GF2Matrix(i4, Matrix.MATRIX_TYPE_RANDOM_UT, secureRandom2);
        GF2Matrix gF2Matrix3 = (GF2Matrix) gF2Matrix.rightMultiply(gF2Matrix2);
        Permutation permutation = new Permutation(i4, secureRandom2);
        int[] vector = permutation.getVector();
        int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{i4, i5});
        int i6 = 0;
        for (i2 = 0; i2 < i4; i2++) {
            System.arraycopy(gF2Matrix3.matrix[vector[i2]], 0, iArr[i2], 0, i5);
        }
        gF2MatrixArr[0] = new GF2Matrix(i4, iArr);
        GF2Matrix gF2Matrix4 = new GF2Matrix(i4, 'I');
        int i7 = 0;
        while (i7 < i4) {
            int i8 = i7 >>> 5;
            i2 = 1 << (i7 & 31);
            i3 = i7 + 1;
            int i9 = i3;
            while (i9 < i4) {
                if ((gF2Matrix.matrix[i9][i8] & i2) != 0) {
                    for (int i10 = i6; i10 <= i8; i10++) {
                        int[] iArr2 = gF2Matrix4.matrix[i9];
                        iArr2[i10] = iArr2[i10] ^ gF2Matrix4.matrix[i7][i10];
                    }
                }
                i9++;
                i6 = 0;
            }
            i7 = i3;
        }
        gF2Matrix = new GF2Matrix(i4, 'I');
        for (i4--; i4 >= 0; i4--) {
            i7 = i4 >>> 5;
            int i11 = 1 << (i4 & 31);
            for (i6 = i4 - 1; i6 >= 0; i6--) {
                if ((gF2Matrix2.matrix[i6][i7] & i11) != 0) {
                    for (i3 = i7; i3 < i5; i3++) {
                        int[] iArr3 = gF2Matrix.matrix[i6];
                        iArr3[i3] = iArr3[i3] ^ gF2Matrix.matrix[i4][i3];
                    }
                }
            }
        }
        gF2MatrixArr[1] = (GF2Matrix) gF2Matrix.rightMultiply(gF2Matrix4.rightMultiply(permutation));
        return gF2MatrixArr;
    }

    private static void swapRows(int[][] iArr, int i, int i2) {
        int[] iArr2 = iArr[i];
        iArr[i] = iArr[i2];
        iArr[i2] = iArr2;
    }

    public Matrix computeInverse() {
        if (this.numRows == this.numColumns) {
            int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
            for (int i = this.numRows - 1; i >= 0; i--) {
                iArr[i] = IntUtils.clone(this.matrix[i]);
            }
            int[][] iArr2 = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, this.length});
            for (int i2 = this.numRows - 1; i2 >= 0; i2--) {
                iArr2[i2][i2 >> 5] = 1 << (i2 & 31);
            }
            for (int i3 = 0; i3 < this.numRows; i3++) {
                int i4;
                int i5 = i3 >> 5;
                int i6 = 1 << (i3 & 31);
                if ((iArr[i3][i5] & i6) == 0) {
                    i4 = i3 + 1;
                    int i7 = 0;
                    while (i4 < this.numRows) {
                        if ((iArr[i4][i5] & i6) != 0) {
                            swapRows(iArr, i3, i4);
                            swapRows(iArr2, i3, i4);
                            i4 = this.numRows;
                            i7 = 1;
                        }
                        i4++;
                    }
                    if (i7 == 0) {
                        throw new ArithmeticException("Matrix is not invertible.");
                    }
                }
                i4 = this.numRows - 1;
                while (i4 >= 0) {
                    if (!(i4 == i3 || (iArr[i4][i5] & i6) == 0)) {
                        addToRow(iArr[i3], iArr[i4], i5);
                        addToRow(iArr2[i3], iArr2[i4], 0);
                    }
                    i4--;
                }
            }
            return new GF2Matrix(this.numColumns, iArr2);
        }
        throw new ArithmeticException("Matrix is not invertible.");
    }

    public Matrix computeTranspose() {
        int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{this.numColumns, (this.numRows + 31) >>> 5});
        for (int i = 0; i < this.numRows; i++) {
            for (int i2 = 0; i2 < this.numColumns; i2++) {
                int i3 = i >>> 5;
                int i4 = i & 31;
                if (((this.matrix[i][i2 >>> 5] >>> (i2 & 31)) & 1) == 1) {
                    int[] iArr2 = iArr[i2];
                    iArr2[i3] = (1 << i4) | iArr2[i3];
                }
            }
        }
        return new GF2Matrix(this.numRows, iArr);
    }

    /* JADX WARNING: Missing block: B:19:0x0034, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(Object obj) {
        if (!(obj instanceof GF2Matrix)) {
            return false;
        }
        GF2Matrix gF2Matrix = (GF2Matrix) obj;
        if (this.numRows != gF2Matrix.numRows || this.numColumns != gF2Matrix.numColumns || this.length != gF2Matrix.length) {
            return false;
        }
        for (int i = 0; i < this.numRows; i++) {
            if (!IntUtils.equals(this.matrix[i], gF2Matrix.matrix[i])) {
                return false;
            }
        }
        return true;
    }

    public GF2Matrix extendLeftCompactForm() {
        GF2Matrix gF2Matrix = new GF2Matrix(this.numRows, this.numColumns + this.numRows);
        int i = (this.numRows - 1) + this.numColumns;
        int i2 = this.numRows - 1;
        while (i2 >= 0) {
            System.arraycopy(this.matrix[i2], 0, gF2Matrix.matrix[i2], 0, this.length);
            int[] iArr = gF2Matrix.matrix[i2];
            int i3 = i >> 5;
            iArr[i3] = iArr[i3] | (1 << (i & 31));
            i2--;
            i--;
        }
        return gF2Matrix;
    }

    public GF2Matrix extendRightCompactForm() {
        GF2Matrix gF2Matrix = new GF2Matrix(this.numRows, this.numRows + this.numColumns);
        int i = this.numRows >> 5;
        int i2 = this.numRows & 31;
        for (int i3 = this.numRows - 1; i3 >= 0; i3--) {
            int[] iArr = gF2Matrix.matrix[i3];
            int i4 = i3 >> 5;
            iArr[i4] = iArr[i4] | (1 << (i3 & 31));
            int i5 = 0;
            if (i2 != 0) {
                int[] iArr2;
                i4 = i;
                while (i5 < this.length - 1) {
                    int i6 = this.matrix[i3][i5];
                    int[] iArr3 = gF2Matrix.matrix[i3];
                    int i7 = i4 + 1;
                    iArr3[i4] = iArr3[i4] | (i6 << i2);
                    iArr2 = gF2Matrix.matrix[i3];
                    iArr2[i7] = (i6 >>> (32 - i2)) | iArr2[i7];
                    i5++;
                    i4 = i7;
                }
                i5 = this.matrix[i3][this.length - 1];
                int[] iArr4 = gF2Matrix.matrix[i3];
                int i8 = i4 + 1;
                iArr4[i4] = iArr4[i4] | (i5 << i2);
                if (i8 < gF2Matrix.length) {
                    iArr2 = gF2Matrix.matrix[i3];
                    iArr2[i8] = (i5 >>> (32 - i2)) | iArr2[i8];
                }
            } else {
                System.arraycopy(this.matrix[i3], 0, gF2Matrix.matrix[i3], i, this.length);
            }
        }
        return gF2Matrix;
    }

    public byte[] getEncoded() {
        byte[] bArr = new byte[((((this.numColumns + 7) >>> 3) * this.numRows) + 8)];
        LittleEndianConversions.I2OSP(this.numRows, bArr, 0);
        LittleEndianConversions.I2OSP(this.numColumns, bArr, 4);
        int i = this.numColumns >>> 5;
        int i2 = this.numColumns & 31;
        int i3 = 8;
        int i4 = 0;
        while (i4 < this.numRows) {
            int i5 = i3;
            i3 = 0;
            while (i3 < i) {
                LittleEndianConversions.I2OSP(this.matrix[i4][i3], bArr, i5);
                i3++;
                i5 += 4;
            }
            i3 = 0;
            while (i3 < i2) {
                int i6 = i5 + 1;
                bArr[i5] = (byte) ((this.matrix[i4][i] >>> i3) & 255);
                i3 += 8;
                i5 = i6;
            }
            i4++;
            i3 = i5;
        }
        return bArr;
    }

    public double getHammingWeight() {
        int i = this.numColumns & 31;
        int i2 = i == 0 ? this.length : this.length - 1;
        double d = 0.0d;
        double d2 = d;
        for (int i3 = 0; i3 < this.numRows; i3++) {
            int i4 = 0;
            while (i4 < i2) {
                int i5 = this.matrix[i3][i4];
                double d3 = d2;
                double d4 = d;
                for (int i6 = 0; i6 < 32; i6++) {
                    d4 += (double) ((i5 >>> i6) & 1);
                    d3 += 1.0d;
                }
                i4++;
                d = d4;
                d2 = d3;
            }
            i4 = this.matrix[i3][this.length - 1];
            for (int i7 = 0; i7 < i; i7++) {
                d += (double) ((i4 >>> i7) & 1);
                d2 += 1.0d;
            }
        }
        return d / d2;
    }

    public int[][] getIntArray() {
        return this.matrix;
    }

    public GF2Matrix getLeftSubMatrix() {
        if (this.numColumns > this.numRows) {
            int i = (this.numRows + 31) >> 5;
            int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{this.numRows, i});
            int i2 = (1 << (this.numRows & 31)) - 1;
            if (i2 == 0) {
                i2 = -1;
            }
            for (int i3 = this.numRows - 1; i3 >= 0; i3--) {
                System.arraycopy(this.matrix[i3], 0, iArr[i3], 0, i);
                int[] iArr2 = iArr[i3];
                int i4 = i - 1;
                iArr2[i4] = iArr2[i4] & i2;
            }
            return new GF2Matrix(this.numRows, iArr);
        }
        throw new ArithmeticException("empty submatrix");
    }

    public int getLength() {
        return this.length;
    }

    public GF2Matrix getRightSubMatrix() {
        if (this.numColumns > this.numRows) {
            int i = this.numRows >> 5;
            int i2 = this.numRows & 31;
            GF2Matrix gF2Matrix = new GF2Matrix(this.numRows, this.numColumns - this.numRows);
            for (int i3 = this.numRows - 1; i3 >= 0; i3--) {
                int i4 = 0;
                if (i2 != 0) {
                    int i5;
                    int i6 = i;
                    while (i4 < gF2Matrix.length - 1) {
                        i5 = i6 + 1;
                        gF2Matrix.matrix[i3][i4] = (this.matrix[i3][i6] >>> i2) | (this.matrix[i3][i5] << (32 - i2));
                        i4++;
                        i6 = i5;
                    }
                    i5 = i6 + 1;
                    gF2Matrix.matrix[i3][gF2Matrix.length - 1] = this.matrix[i3][i6] >>> i2;
                    if (i5 < this.length) {
                        int[] iArr = gF2Matrix.matrix[i3];
                        i6 = gF2Matrix.length - 1;
                        iArr[i6] = iArr[i6] | (this.matrix[i3][i5] << (32 - i2));
                    }
                } else {
                    System.arraycopy(this.matrix[i3], i, gF2Matrix.matrix[i3], 0, gF2Matrix.length);
                }
            }
            return gF2Matrix;
        }
        throw new ArithmeticException("empty submatrix");
    }

    public int[] getRow(int i) {
        return this.matrix[i];
    }

    public int hashCode() {
        int i = (((this.numRows * 31) + this.numColumns) * 31) + this.length;
        for (int i2 = 0; i2 < this.numRows; i2++) {
            i = (i * 31) + this.matrix[i2].hashCode();
        }
        return i;
    }

    public boolean isZero() {
        for (int i = 0; i < this.numRows; i++) {
            for (int i2 = 0; i2 < this.length; i2++) {
                if (this.matrix[i][i2] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public Matrix leftMultiply(Permutation permutation) {
        int[] vector = permutation.getVector();
        if (vector.length == this.numRows) {
            int[][] iArr = new int[this.numRows][];
            for (int i = this.numRows - 1; i >= 0; i--) {
                iArr[i] = IntUtils.clone(this.matrix[vector[i]]);
            }
            return new GF2Matrix(this.numRows, iArr);
        }
        throw new ArithmeticException("length mismatch");
    }

    public Vector leftMultiply(Vector vector) {
        if (!(vector instanceof GF2Vector)) {
            throw new ArithmeticException("vector is not defined over GF(2)");
        } else if (vector.length == this.numRows) {
            int[] vecArray = ((GF2Vector) vector).getVecArray();
            int[] iArr = new int[this.length];
            int i = this.numRows >> 5;
            int i2 = 1;
            int i3 = 1 << (this.numRows & 31);
            int i4 = 0;
            int i5 = i4;
            while (i4 < i) {
                int i6 = i5;
                i5 = 1;
                do {
                    if ((vecArray[i4] & i5) != 0) {
                        for (int i7 = 0; i7 < this.length; i7++) {
                            iArr[i7] = iArr[i7] ^ this.matrix[i6][i7];
                        }
                    }
                    i6++;
                    i5 <<= 1;
                } while (i5 != 0);
                i4++;
                i5 = i6;
            }
            while (i2 != i3) {
                if ((vecArray[i] & i2) != 0) {
                    for (i4 = 0; i4 < this.length; i4++) {
                        iArr[i4] = iArr[i4] ^ this.matrix[i5][i4];
                    }
                }
                i5++;
                i2 <<= 1;
            }
            return new GF2Vector(iArr, this.numColumns);
        } else {
            throw new ArithmeticException("length mismatch");
        }
    }

    public Vector leftMultiplyLeftCompactForm(Vector vector) {
        if (!(vector instanceof GF2Vector)) {
            throw new ArithmeticException("vector is not defined over GF(2)");
        } else if (vector.length == this.numRows) {
            int i;
            int i2;
            int[] vecArray = ((GF2Vector) vector).getVecArray();
            int[] iArr = new int[(((this.numRows + this.numColumns) + 31) >>> 5)];
            int i3 = this.numRows >>> 5;
            int i4 = 0;
            int i5 = i4;
            while (i4 < i3) {
                i = i5;
                i5 = 1;
                do {
                    if ((vecArray[i4] & i5) != 0) {
                        for (i2 = 0; i2 < this.length; i2++) {
                            iArr[i2] = iArr[i2] ^ this.matrix[i][i2];
                        }
                        i2 = (this.numColumns + i) >>> 5;
                        iArr[i2] = (1 << ((this.numColumns + i) & 31)) | iArr[i2];
                    }
                    i++;
                    i5 <<= 1;
                } while (i5 != 0);
                i4++;
                i5 = i;
            }
            i4 = 1 << (this.numRows & 31);
            i = i5;
            for (i5 = 1; i5 != i4; i5 <<= 1) {
                if ((vecArray[i3] & i5) != 0) {
                    for (i2 = 0; i2 < this.length; i2++) {
                        iArr[i2] = iArr[i2] ^ this.matrix[i][i2];
                    }
                    i2 = (this.numColumns + i) >>> 5;
                    iArr[i2] = (1 << ((this.numColumns + i) & 31)) | iArr[i2];
                }
                i++;
            }
            return new GF2Vector(iArr, this.numRows + this.numColumns);
        } else {
            throw new ArithmeticException("length mismatch");
        }
    }

    public Matrix rightMultiply(Matrix matrix) {
        if (!(matrix instanceof GF2Matrix)) {
            throw new ArithmeticException("matrix is not defined over GF(2)");
        } else if (matrix.numRows == this.numColumns) {
            GF2Matrix gF2Matrix = (GF2Matrix) matrix;
            GF2Matrix gF2Matrix2 = new GF2Matrix(this.numRows, matrix.numColumns);
            int i = this.numColumns & 31;
            int i2 = i == 0 ? this.length : this.length - 1;
            for (int i3 = 0; i3 < this.numRows; i3++) {
                int i4;
                int i5;
                int i6 = 0;
                int i7 = i6;
                while (i6 < i2) {
                    i4 = this.matrix[i3][i6];
                    i5 = i7;
                    for (i7 = 0; i7 < 32; i7++) {
                        if (((1 << i7) & i4) != 0) {
                            for (int i8 = 0; i8 < gF2Matrix.length; i8++) {
                                int[] iArr = gF2Matrix2.matrix[i3];
                                iArr[i8] = iArr[i8] ^ gF2Matrix.matrix[i5][i8];
                            }
                        }
                        i5++;
                    }
                    i6++;
                    i7 = i5;
                }
                i6 = this.matrix[i3][this.length - 1];
                i4 = i7;
                for (i7 = 0; i7 < i; i7++) {
                    if (((1 << i7) & i6) != 0) {
                        for (i5 = 0; i5 < gF2Matrix.length; i5++) {
                            int[] iArr2 = gF2Matrix2.matrix[i3];
                            iArr2[i5] = iArr2[i5] ^ gF2Matrix.matrix[i4][i5];
                        }
                    }
                    i4++;
                }
            }
            return gF2Matrix2;
        } else {
            throw new ArithmeticException("length mismatch");
        }
    }

    public Matrix rightMultiply(Permutation permutation) {
        int[] vector = permutation.getVector();
        if (vector.length == this.numColumns) {
            GF2Matrix gF2Matrix = new GF2Matrix(this.numRows, this.numColumns);
            for (int i = this.numColumns - 1; i >= 0; i--) {
                int i2 = i >>> 5;
                int i3 = i & 31;
                int i4 = vector[i] >>> 5;
                int i5 = vector[i] & 31;
                for (int i6 = this.numRows - 1; i6 >= 0; i6--) {
                    int[] iArr = gF2Matrix.matrix[i6];
                    iArr[i2] = iArr[i2] | (((this.matrix[i6][i4] >>> i5) & 1) << i3);
                }
            }
            return gF2Matrix;
        }
        throw new ArithmeticException("length mismatch");
    }

    public Vector rightMultiply(Vector vector) {
        if (!(vector instanceof GF2Vector)) {
            throw new ArithmeticException("vector is not defined over GF(2)");
        } else if (vector.length == this.numColumns) {
            int[] vecArray = ((GF2Vector) vector).getVecArray();
            int[] iArr = new int[((this.numRows + 31) >>> 5)];
            for (int i = 0; i < this.numRows; i++) {
                int i2 = 0;
                int i3 = i2;
                while (i2 < this.length) {
                    i3 ^= this.matrix[i][i2] & vecArray[i2];
                    i2++;
                }
                i2 = 0;
                int i4 = i2;
                while (i2 < 32) {
                    i4 ^= (i3 >>> i2) & 1;
                    i2++;
                }
                if (i4 == 1) {
                    i2 = i >>> 5;
                    iArr[i2] = iArr[i2] | (1 << (i & 31));
                }
            }
            return new GF2Vector(iArr, this.numRows);
        } else {
            throw new ArithmeticException("length mismatch");
        }
    }

    public Vector rightMultiplyRightCompactForm(Vector vector) {
        Vector vector2 = vector;
        if (!(vector2 instanceof GF2Vector)) {
            throw new ArithmeticException("vector is not defined over GF(2)");
        } else if (vector2.length == this.numColumns + this.numRows) {
            int[] vecArray = ((GF2Vector) vector2).getVecArray();
            int[] iArr = new int[((this.numRows + 31) >>> 5)];
            int i = this.numRows >> 5;
            int i2 = this.numRows & 31;
            for (int i3 = 0; i3 < this.numRows; i3++) {
                int i4;
                int i5;
                int i6 = i3 >> 5;
                int i7 = i3 & 31;
                int i8 = (vecArray[i6] >>> i7) & 1;
                if (i2 != 0) {
                    i4 = i;
                    i5 = i8;
                    i8 = 0;
                    while (i8 < this.length - 1) {
                        int i9 = i4 + 1;
                        i5 ^= ((vecArray[i4] >>> i2) | (vecArray[i9] << (32 - i2))) & this.matrix[i3][i8];
                        i8++;
                        i4 = i9;
                    }
                    i8 = i4 + 1;
                    i4 = vecArray[i4] >>> i2;
                    if (i8 < vecArray.length) {
                        i4 |= vecArray[i8] << (32 - i2);
                    }
                    i4 = (this.matrix[i3][this.length - 1] & i4) ^ i5;
                } else {
                    i5 = i;
                    i4 = i8;
                    i8 = 0;
                    while (i8 < this.length) {
                        i4 ^= vecArray[i5] & this.matrix[i3][i8];
                        i8++;
                        i5++;
                    }
                }
                i8 = 0;
                i5 = i4;
                i4 = i8;
                while (i8 < 32) {
                    i4 ^= i5 & 1;
                    i5 >>>= 1;
                    i8++;
                }
                if (i4 == 1) {
                    iArr[i6] = iArr[i6] | (1 << i7);
                }
            }
            return new GF2Vector(iArr, this.numRows);
        } else {
            throw new ArithmeticException("length mismatch");
        }
    }

    public String toString() {
        int i = this.numColumns & 31;
        int i2 = i == 0 ? this.length : this.length - 1;
        StringBuffer stringBuffer = new StringBuffer();
        for (int i3 = 0; i3 < this.numRows; i3++) {
            int i4;
            int i5;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(i3);
            stringBuilder.append(": ");
            stringBuffer.append(stringBuilder.toString());
            for (i4 = 0; i4 < i2; i4++) {
                i5 = this.matrix[i3][i4];
                for (int i6 = 0; i6 < 32; i6++) {
                    if (((i5 >>> i6) & 1) == 0) {
                        stringBuffer.append('0');
                    } else {
                        stringBuffer.append('1');
                    }
                }
                stringBuffer.append(' ');
            }
            i4 = this.matrix[i3][this.length - 1];
            for (i5 = 0; i5 < i; i5++) {
                if (((i4 >>> i5) & 1) == 0) {
                    stringBuffer.append('0');
                } else {
                    stringBuffer.append('1');
                }
            }
            stringBuffer.append(10);
        }
        return stringBuffer.toString();
    }
}
