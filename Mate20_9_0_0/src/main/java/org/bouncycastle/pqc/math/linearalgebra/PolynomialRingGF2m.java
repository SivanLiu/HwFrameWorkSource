package org.bouncycastle.pqc.math.linearalgebra;

public class PolynomialRingGF2m {
    private GF2mField field;
    private PolynomialGF2mSmallM p;
    protected PolynomialGF2mSmallM[] sqMatrix;
    protected PolynomialGF2mSmallM[] sqRootMatrix;

    public PolynomialRingGF2m(GF2mField gF2mField, PolynomialGF2mSmallM polynomialGF2mSmallM) {
        this.field = gF2mField;
        this.p = polynomialGF2mSmallM;
        computeSquaringMatrix();
        computeSquareRootMatrix();
    }

    private void computeSquareRootMatrix() {
        int i;
        int degree = this.p.getDegree();
        PolynomialGF2mSmallM[] polynomialGF2mSmallMArr = new PolynomialGF2mSmallM[degree];
        int i2 = degree - 1;
        for (i = i2; i >= 0; i--) {
            polynomialGF2mSmallMArr[i] = new PolynomialGF2mSmallM(this.sqMatrix[i]);
        }
        this.sqRootMatrix = new PolynomialGF2mSmallM[degree];
        while (i2 >= 0) {
            this.sqRootMatrix[i2] = new PolynomialGF2mSmallM(this.field, i2);
            i2--;
        }
        for (i = 0; i < degree; i++) {
            int i3;
            int i4;
            if (polynomialGF2mSmallMArr[i].getCoefficient(i) == 0) {
                i3 = i + 1;
                i4 = 0;
                while (i3 < degree) {
                    if (polynomialGF2mSmallMArr[i3].getCoefficient(i) != 0) {
                        swapColumns(polynomialGF2mSmallMArr, i, i3);
                        swapColumns(this.sqRootMatrix, i, i3);
                        i3 = degree;
                        i4 = 1;
                    }
                    i3++;
                }
                if (i4 == 0) {
                    throw new ArithmeticException("Squaring matrix is not invertible.");
                }
            }
            i3 = this.field.inverse(polynomialGF2mSmallMArr[i].getCoefficient(i));
            polynomialGF2mSmallMArr[i].multThisWithElement(i3);
            this.sqRootMatrix[i].multThisWithElement(i3);
            for (i3 = 0; i3 < degree; i3++) {
                if (i3 != i) {
                    i4 = polynomialGF2mSmallMArr[i3].getCoefficient(i);
                    if (i4 != 0) {
                        PolynomialGF2mSmallM multWithElement = polynomialGF2mSmallMArr[i].multWithElement(i4);
                        PolynomialGF2mSmallM multWithElement2 = this.sqRootMatrix[i].multWithElement(i4);
                        polynomialGF2mSmallMArr[i3].addToThis(multWithElement);
                        this.sqRootMatrix[i3].addToThis(multWithElement2);
                    }
                }
            }
        }
    }

    private void computeSquaringMatrix() {
        int i;
        int[] iArr;
        int degree = this.p.getDegree();
        this.sqMatrix = new PolynomialGF2mSmallM[degree];
        int i2 = 0;
        while (true) {
            i = degree >> 1;
            if (i2 >= i) {
                break;
            }
            i = i2 << 1;
            iArr = new int[(i + 1)];
            iArr[i] = 1;
            this.sqMatrix[i2] = new PolynomialGF2mSmallM(this.field, iArr);
            i2++;
        }
        while (i < degree) {
            i2 = i << 1;
            iArr = new int[(i2 + 1)];
            iArr[i2] = 1;
            this.sqMatrix[i] = new PolynomialGF2mSmallM(this.field, iArr).mod(this.p);
            i++;
        }
    }

    private static void swapColumns(PolynomialGF2mSmallM[] polynomialGF2mSmallMArr, int i, int i2) {
        PolynomialGF2mSmallM polynomialGF2mSmallM = polynomialGF2mSmallMArr[i];
        polynomialGF2mSmallMArr[i] = polynomialGF2mSmallMArr[i2];
        polynomialGF2mSmallMArr[i2] = polynomialGF2mSmallM;
    }

    public PolynomialGF2mSmallM[] getSquareRootMatrix() {
        return this.sqRootMatrix;
    }

    public PolynomialGF2mSmallM[] getSquaringMatrix() {
        return this.sqMatrix;
    }
}
