package org.bouncycastle.pqc.math.linearalgebra;

import java.lang.reflect.Array;
import java.security.SecureRandom;

public final class GoppaCode {

    public static class MaMaPe {
        private GF2Matrix h;
        private Permutation p;
        private GF2Matrix s;

        public MaMaPe(GF2Matrix gF2Matrix, GF2Matrix gF2Matrix2, Permutation permutation) {
            this.s = gF2Matrix;
            this.h = gF2Matrix2;
            this.p = permutation;
        }

        public GF2Matrix getFirstMatrix() {
            return this.s;
        }

        public Permutation getPermutation() {
            return this.p;
        }

        public GF2Matrix getSecondMatrix() {
            return this.h;
        }
    }

    public static class MatrixSet {
        private GF2Matrix g;
        private int[] setJ;

        public MatrixSet(GF2Matrix gF2Matrix, int[] iArr) {
            this.g = gF2Matrix;
            this.setJ = iArr;
        }

        public GF2Matrix getG() {
            return this.g;
        }

        public int[] getSetJ() {
            return this.setJ;
        }
    }

    private GoppaCode() {
    }

    public static MaMaPe computeSystematicForm(GF2Matrix gF2Matrix, SecureRandom secureRandom) {
        Permutation permutation;
        Matrix matrix;
        GF2Matrix leftSubMatrix;
        int numColumns = gF2Matrix.getNumColumns();
        GF2Matrix gF2Matrix2 = null;
        Object obj;
        do {
            permutation = new Permutation(numColumns, secureRandom);
            matrix = (GF2Matrix) gF2Matrix.rightMultiply(permutation);
            leftSubMatrix = matrix.getLeftSubMatrix();
            obj = 1;
            try {
                gF2Matrix2 = (GF2Matrix) leftSubMatrix.computeInverse();
                continue;
            } catch (ArithmeticException e) {
                obj = null;
                continue;
            }
        } while (obj == null);
        return new MaMaPe(leftSubMatrix, ((GF2Matrix) gF2Matrix2.rightMultiply(matrix)).getRightSubMatrix(), permutation);
    }

    public static GF2Matrix createCanonicalCheckMatrix(GF2mField gF2mField, PolynomialGF2mSmallM polynomialGF2mSmallM) {
        int i;
        int i2;
        int i3;
        int degree = gF2mField.getDegree();
        int i4 = 1 << degree;
        int degree2 = polynomialGF2mSmallM.getDegree();
        int[][] iArr = (int[][]) Array.newInstance(int.class, new int[]{degree2, i4});
        int[][] iArr2 = (int[][]) Array.newInstance(int.class, new int[]{degree2, i4});
        for (i = 0; i < i4; i++) {
            iArr2[0][i] = gF2mField.inverse(polynomialGF2mSmallM.evaluateAt(i));
        }
        for (i = 1; i < degree2; i++) {
            for (i2 = 0; i2 < i4; i2++) {
                iArr2[i][i2] = gF2mField.mult(iArr2[i - 1][i2], i2);
            }
        }
        for (i = 0; i < degree2; i++) {
            for (i2 = 0; i2 < i4; i2++) {
                for (i3 = 0; i3 <= i; i3++) {
                    iArr[i][i2] = gF2mField.add(iArr[i][i2], gF2mField.mult(iArr2[i3][i2], polynomialGF2mSmallM.getCoefficient((degree2 + i3) - i)));
                }
            }
        }
        int[][] iArr3 = (int[][]) Array.newInstance(int.class, new int[]{degree2 * degree, (i4 + 31) >>> 5});
        for (int i5 = 0; i5 < i4; i5++) {
            int i6 = i5 >>> 5;
            i = 1 << (i5 & 31);
            for (i2 = 0; i2 < degree2; i2++) {
                i3 = iArr[i2][i5];
                for (int i7 = 0; i7 < degree; i7++) {
                    if (((i3 >>> i7) & 1) != 0) {
                        int[] iArr4 = iArr3[(((i2 + 1) * degree) - i7) - 1];
                        iArr4[i6] = iArr4[i6] ^ i;
                    }
                }
            }
        }
        return new GF2Matrix(i4, iArr3);
    }

    public static GF2Vector syndromeDecode(GF2Vector gF2Vector, GF2mField gF2mField, PolynomialGF2mSmallM polynomialGF2mSmallM, PolynomialGF2mSmallM[] polynomialGF2mSmallMArr) {
        int degree = 1 << gF2mField.getDegree();
        GF2Vector gF2Vector2 = new GF2Vector(degree);
        if (!gF2Vector.isZero()) {
            PolynomialGF2mSmallM[] modPolynomialToFracton = new PolynomialGF2mSmallM(gF2Vector.toExtensionFieldVector(gF2mField)).modInverse(polynomialGF2mSmallM).addMonomial(1).modSquareRootMatrix(polynomialGF2mSmallMArr).modPolynomialToFracton(polynomialGF2mSmallM);
            int i = 0;
            PolynomialGF2mSmallM add = modPolynomialToFracton[0].multiply(modPolynomialToFracton[0]).add(modPolynomialToFracton[1].multiply(modPolynomialToFracton[1]).multWithMonomial(1));
            add = add.multWithElement(gF2mField.inverse(add.getHeadCoefficient()));
            while (i < degree) {
                if (add.evaluateAt(i) == 0) {
                    gF2Vector2.setBit(i);
                }
                i++;
            }
        }
        return gF2Vector2;
    }
}
