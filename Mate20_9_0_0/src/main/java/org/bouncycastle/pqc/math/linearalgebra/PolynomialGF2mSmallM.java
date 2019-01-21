package org.bouncycastle.pqc.math.linearalgebra;

import java.security.SecureRandom;

public class PolynomialGF2mSmallM {
    public static final char RANDOM_IRREDUCIBLE_POLYNOMIAL = 'I';
    private int[] coefficients;
    private int degree;
    private GF2mField field;

    public PolynomialGF2mSmallM(GF2mField gF2mField) {
        this.field = gF2mField;
        this.degree = -1;
        this.coefficients = new int[1];
    }

    public PolynomialGF2mSmallM(GF2mField gF2mField, int i) {
        this.field = gF2mField;
        this.degree = i;
        this.coefficients = new int[(i + 1)];
        this.coefficients[i] = 1;
    }

    public PolynomialGF2mSmallM(GF2mField gF2mField, int i, char c, SecureRandom secureRandom) {
        this.field = gF2mField;
        if (c == 'I') {
            this.coefficients = createRandomIrreduciblePolynomial(i, secureRandom);
            computeDegree();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" Error: type ");
        stringBuilder.append(c);
        stringBuilder.append(" is not defined for GF2smallmPolynomial");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public PolynomialGF2mSmallM(GF2mField gF2mField, byte[] bArr) {
        this.field = gF2mField;
        int i = 8;
        int i2 = 1;
        while (gF2mField.getDegree() > i) {
            i2++;
            i += 8;
        }
        if (bArr.length % i2 == 0) {
            this.coefficients = new int[(bArr.length / i2)];
            i2 = 0;
            int i3 = i2;
            while (i2 < this.coefficients.length) {
                int i4 = i3;
                i3 = 0;
                while (i3 < i) {
                    int[] iArr = this.coefficients;
                    int i5 = i4 + 1;
                    iArr[i2] = ((bArr[i4] & 255) << i3) ^ iArr[i2];
                    i3 += 8;
                    i4 = i5;
                }
                if (this.field.isElementOfThisField(this.coefficients[i2])) {
                    i2++;
                    i3 = i4;
                } else {
                    throw new IllegalArgumentException(" Error: byte array is not encoded polynomial over given finite field GF2m");
                }
            }
            if (this.coefficients.length == 1 || this.coefficients[this.coefficients.length - 1] != 0) {
                computeDegree();
                return;
            }
            throw new IllegalArgumentException(" Error: byte array is not encoded polynomial over given finite field GF2m");
        }
        throw new IllegalArgumentException(" Error: byte array is not encoded polynomial over given finite field GF2m");
    }

    public PolynomialGF2mSmallM(GF2mField gF2mField, int[] iArr) {
        this.field = gF2mField;
        this.coefficients = normalForm(iArr);
        computeDegree();
    }

    public PolynomialGF2mSmallM(GF2mVector gF2mVector) {
        this(gF2mVector.getField(), gF2mVector.getIntArrayForm());
    }

    public PolynomialGF2mSmallM(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        this.field = polynomialGF2mSmallM.field;
        this.degree = polynomialGF2mSmallM.degree;
        this.coefficients = IntUtils.clone(polynomialGF2mSmallM.coefficients);
    }

    private int[] add(int[] iArr, int[] iArr2) {
        int[] iArr3;
        if (iArr.length < iArr2.length) {
            iArr3 = new int[iArr2.length];
            System.arraycopy(iArr2, 0, iArr3, 0, iArr2.length);
        } else {
            iArr3 = new int[iArr.length];
            System.arraycopy(iArr, 0, iArr3, 0, iArr.length);
            iArr = iArr2;
        }
        for (int length = iArr.length - 1; length >= 0; length--) {
            iArr3[length] = this.field.add(iArr3[length], iArr[length]);
        }
        return iArr3;
    }

    private static int computeDegree(int[] iArr) {
        int length = iArr.length - 1;
        while (length >= 0 && iArr[length] == 0) {
            length--;
        }
        return length;
    }

    private void computeDegree() {
        int length = this.coefficients.length;
        while (true) {
            this.degree = length - 1;
            if (this.degree >= 0 && this.coefficients[this.degree] == 0) {
                length = this.degree;
            } else {
                return;
            }
        }
    }

    private int[] createRandomIrreduciblePolynomial(int i, SecureRandom secureRandom) {
        int[] iArr = new int[(i + 1)];
        int i2 = 1;
        iArr[i] = 1;
        iArr[0] = this.field.getRandomNonZeroElement(secureRandom);
        while (i2 < i) {
            iArr[i2] = this.field.getRandomElement(secureRandom);
            i2++;
        }
        while (!isIrreducible(iArr)) {
            i2 = RandUtils.nextInt(secureRandom, i);
            if (i2 == 0) {
                iArr[0] = this.field.getRandomNonZeroElement(secureRandom);
            } else {
                iArr[i2] = this.field.getRandomElement(secureRandom);
            }
        }
        return iArr;
    }

    private int[][] div(int[] iArr, int[] iArr2) {
        int computeDegree = computeDegree(iArr2);
        int computeDegree2 = computeDegree(iArr) + 1;
        if (computeDegree != -1) {
            int[][] iArr3 = new int[][]{new int[1], new int[computeDegree2]};
            computeDegree2 = this.field.inverse(headCoefficient(iArr2));
            iArr3[0][0] = 0;
            System.arraycopy(iArr, 0, iArr3[1], 0, iArr3[1].length);
            while (computeDegree <= computeDegree(iArr3[1])) {
                iArr = new int[]{this.field.mult(headCoefficient(iArr3[1]), computeDegree2)};
                int computeDegree3 = computeDegree(iArr3[1]) - computeDegree;
                int[] multWithMonomial = multWithMonomial(multWithElement(iArr2, iArr[0]), computeDegree3);
                iArr3[0] = add(multWithMonomial(iArr, computeDegree3), iArr3[0]);
                iArr3[1] = add(multWithMonomial, iArr3[1]);
            }
            return iArr3;
        }
        throw new ArithmeticException("Division by zero.");
    }

    private int[] gcd(int[] iArr, int[] iArr2) {
        if (computeDegree(iArr) == -1) {
            return iArr2;
        }
        while (computeDegree(iArr2) != -1) {
            iArr = mod(iArr, iArr2);
            int[] iArr3 = new int[iArr2.length];
            System.arraycopy(iArr2, 0, iArr3, 0, iArr3.length);
            iArr2 = new int[iArr.length];
            System.arraycopy(iArr, 0, iArr2, 0, iArr2.length);
            iArr = iArr3;
        }
        return multWithElement(iArr, this.field.inverse(headCoefficient(iArr)));
    }

    private static int headCoefficient(int[] iArr) {
        int computeDegree = computeDegree(iArr);
        return computeDegree == -1 ? 0 : iArr[computeDegree];
    }

    private static boolean isEqual(int[] iArr, int[] iArr2) {
        int computeDegree = computeDegree(iArr);
        if (computeDegree != computeDegree(iArr2)) {
            return false;
        }
        for (int i = 0; i <= computeDegree; i++) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isIrreducible(int[] iArr) {
        if (iArr[0] == 0) {
            return false;
        }
        int computeDegree = computeDegree(iArr) >> 1;
        int[] iArr2 = new int[]{0, 1};
        int[] iArr3 = new int[]{0, 1};
        int degree = this.field.getDegree();
        int[] iArr4 = iArr2;
        for (int i = 0; i < computeDegree; i++) {
            for (int i2 = degree - 1; i2 >= 0; i2--) {
                iArr4 = modMultiply(iArr4, iArr4, iArr);
            }
            iArr4 = normalForm(iArr4);
            if (computeDegree(gcd(add(iArr4, iArr3), iArr)) != 0) {
                return false;
            }
        }
        return true;
    }

    private int[] mod(int[] iArr, int[] iArr2) {
        int computeDegree = computeDegree(iArr2);
        if (computeDegree != -1) {
            int[] iArr3 = new int[iArr.length];
            int inverse = this.field.inverse(headCoefficient(iArr2));
            System.arraycopy(iArr, 0, iArr3, 0, iArr3.length);
            while (computeDegree <= computeDegree(iArr3)) {
                iArr3 = add(multWithElement(multWithMonomial(iArr2, computeDegree(iArr3) - computeDegree), this.field.mult(headCoefficient(iArr3), inverse)), iArr3);
            }
            return iArr3;
        }
        throw new ArithmeticException("Division by zero");
    }

    private int[] modDiv(int[] iArr, int[] iArr2, int[] iArr3) {
        int[] normalForm = normalForm(iArr3);
        iArr2 = mod(iArr2, iArr3);
        int[] iArr4 = new int[]{0};
        iArr = mod(iArr, iArr3);
        while (computeDegree(iArr2) != -1) {
            int[][] div = div(normalForm, iArr2);
            iArr2 = normalForm(iArr2);
            int[] normalForm2 = normalForm(div[1]);
            normalForm = add(iArr4, modMultiply(div[0], iArr, iArr3));
            iArr4 = normalForm(iArr);
            iArr = normalForm(normalForm);
            normalForm = iArr2;
            iArr2 = normalForm2;
        }
        return multWithElement(iArr4, this.field.inverse(headCoefficient(normalForm)));
    }

    private int[] modMultiply(int[] iArr, int[] iArr2, int[] iArr3) {
        return mod(multiply(iArr, iArr2), iArr3);
    }

    private int[] multWithElement(int[] iArr, int i) {
        int computeDegree = computeDegree(iArr);
        if (computeDegree == -1 || i == 0) {
            return new int[1];
        }
        if (i == 1) {
            return IntUtils.clone(iArr);
        }
        int[] iArr2 = new int[(computeDegree + 1)];
        while (computeDegree >= 0) {
            iArr2[computeDegree] = this.field.mult(iArr[computeDegree], i);
            computeDegree--;
        }
        return iArr2;
    }

    private static int[] multWithMonomial(int[] iArr, int i) {
        int computeDegree = computeDegree(iArr);
        if (computeDegree == -1) {
            return new int[1];
        }
        int[] iArr2 = new int[((computeDegree + i) + 1)];
        System.arraycopy(iArr, 0, iArr2, i, computeDegree + 1);
        return iArr2;
    }

    private int[] multiply(int[] iArr, int[] iArr2) {
        if (computeDegree(iArr) < computeDegree(iArr2)) {
            int[] iArr3 = iArr2;
            iArr2 = iArr;
            iArr = iArr3;
        }
        iArr = normalForm(iArr);
        iArr2 = normalForm(iArr2);
        if (iArr2.length == 1) {
            return multWithElement(iArr, iArr2[0]);
        }
        int length = iArr.length;
        int length2 = iArr2.length;
        int[] iArr4 = new int[((length + length2) - 1)];
        int[] iArr5;
        if (length2 != length) {
            int[] iArr6 = new int[length2];
            iArr5 = new int[(length - length2)];
            System.arraycopy(iArr, 0, iArr6, 0, iArr6.length);
            System.arraycopy(iArr, length2, iArr5, 0, iArr5.length);
            return add(multiply(iArr6, iArr2), multWithMonomial(multiply(iArr5, iArr2), length2));
        }
        int i = (length + 1) >>> 1;
        length -= i;
        int[] iArr7 = new int[i];
        iArr4 = new int[i];
        int[] iArr8 = new int[length];
        iArr5 = new int[length];
        System.arraycopy(iArr, 0, iArr7, 0, iArr7.length);
        System.arraycopy(iArr, i, iArr8, 0, iArr8.length);
        System.arraycopy(iArr2, 0, iArr4, 0, iArr4.length);
        System.arraycopy(iArr2, i, iArr5, 0, iArr5.length);
        iArr = add(iArr7, iArr8);
        iArr2 = add(iArr4, iArr5);
        int[] multiply = multiply(iArr7, iArr4);
        iArr = multiply(iArr, iArr2);
        iArr2 = multiply(iArr8, iArr5);
        return add(multWithMonomial(add(add(add(iArr, multiply), iArr2), multWithMonomial(iArr2, i)), i), multiply);
    }

    private static int[] normalForm(int[] iArr) {
        int computeDegree = computeDegree(iArr);
        if (computeDegree == -1) {
            return new int[1];
        }
        computeDegree++;
        if (iArr.length == computeDegree) {
            return IntUtils.clone(iArr);
        }
        int[] iArr2 = new int[computeDegree];
        System.arraycopy(iArr, 0, iArr2, 0, computeDegree);
        return iArr2;
    }

    public PolynomialGF2mSmallM add(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        return new PolynomialGF2mSmallM(this.field, add(this.coefficients, polynomialGF2mSmallM.coefficients));
    }

    public PolynomialGF2mSmallM addMonomial(int i) {
        int[] iArr = new int[(i + 1)];
        iArr[i] = 1;
        return new PolynomialGF2mSmallM(this.field, add(this.coefficients, iArr));
    }

    public void addToThis(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        this.coefficients = add(this.coefficients, polynomialGF2mSmallM.coefficients);
        computeDegree();
    }

    public PolynomialGF2mSmallM[] div(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        int[][] div = div(this.coefficients, polynomialGF2mSmallM.coefficients);
        return new PolynomialGF2mSmallM[]{new PolynomialGF2mSmallM(this.field, div[0]), new PolynomialGF2mSmallM(this.field, div[1])};
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PolynomialGF2mSmallM)) {
            return false;
        }
        PolynomialGF2mSmallM polynomialGF2mSmallM = (PolynomialGF2mSmallM) obj;
        if (this.field.equals(polynomialGF2mSmallM.field) && this.degree == polynomialGF2mSmallM.degree && isEqual(this.coefficients, polynomialGF2mSmallM.coefficients)) {
            return true;
        }
        return false;
    }

    public int evaluateAt(int i) {
        int i2 = this.coefficients[this.degree];
        for (int i3 = this.degree - 1; i3 >= 0; i3--) {
            i2 = this.field.mult(i2, i) ^ this.coefficients[i3];
        }
        return i2;
    }

    public PolynomialGF2mSmallM gcd(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        return new PolynomialGF2mSmallM(this.field, gcd(this.coefficients, polynomialGF2mSmallM.coefficients));
    }

    public int getCoefficient(int i) {
        return (i < 0 || i > this.degree) ? 0 : this.coefficients[i];
    }

    public int getDegree() {
        int length = this.coefficients.length - 1;
        return this.coefficients[length] == 0 ? -1 : length;
    }

    public byte[] getEncoded() {
        int i = 8;
        int i2 = 1;
        while (this.field.getDegree() > i) {
            i2++;
            i += 8;
        }
        byte[] bArr = new byte[(this.coefficients.length * i2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < this.coefficients.length) {
            int i5 = i4;
            i4 = 0;
            while (i4 < i) {
                int i6 = i5 + 1;
                bArr[i5] = (byte) (this.coefficients[i3] >>> i4);
                i4 += 8;
                i5 = i6;
            }
            i3++;
            i4 = i5;
        }
        return bArr;
    }

    public int getHeadCoefficient() {
        return this.degree == -1 ? 0 : this.coefficients[this.degree];
    }

    public int hashCode() {
        int hashCode = this.field.hashCode();
        for (int i : this.coefficients) {
            hashCode = (hashCode * 31) + i;
        }
        return hashCode;
    }

    public PolynomialGF2mSmallM mod(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        return new PolynomialGF2mSmallM(this.field, mod(this.coefficients, polynomialGF2mSmallM.coefficients));
    }

    public PolynomialGF2mSmallM modDiv(PolynomialGF2mSmallM polynomialGF2mSmallM, PolynomialGF2mSmallM polynomialGF2mSmallM2) {
        return new PolynomialGF2mSmallM(this.field, modDiv(this.coefficients, polynomialGF2mSmallM.coefficients, polynomialGF2mSmallM2.coefficients));
    }

    public PolynomialGF2mSmallM modInverse(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        return new PolynomialGF2mSmallM(this.field, modDiv(new int[]{1}, this.coefficients, polynomialGF2mSmallM.coefficients));
    }

    public PolynomialGF2mSmallM modMultiply(PolynomialGF2mSmallM polynomialGF2mSmallM, PolynomialGF2mSmallM polynomialGF2mSmallM2) {
        return new PolynomialGF2mSmallM(this.field, modMultiply(this.coefficients, polynomialGF2mSmallM.coefficients, polynomialGF2mSmallM2.coefficients));
    }

    public PolynomialGF2mSmallM[] modPolynomialToFracton(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        int i = polynomialGF2mSmallM.degree >> 1;
        int[] normalForm = normalForm(polynomialGF2mSmallM.coefficients);
        int[] iArr = new int[]{0};
        int[] iArr2 = new int[]{1};
        int[] mod = mod(this.coefficients, polynomialGF2mSmallM.coefficients);
        int[] iArr3 = normalForm;
        normalForm = mod;
        while (computeDegree(normalForm) > i) {
            int[][] div = div(iArr3, normalForm);
            int[] iArr4 = div[1];
            iArr3 = add(iArr, modMultiply(div[0], iArr2, polynomialGF2mSmallM.coefficients));
            iArr = iArr2;
            iArr2 = iArr3;
            iArr3 = normalForm;
            normalForm = iArr4;
        }
        return new PolynomialGF2mSmallM[]{new PolynomialGF2mSmallM(this.field, normalForm), new PolynomialGF2mSmallM(this.field, iArr2)};
    }

    public PolynomialGF2mSmallM modSquareMatrix(PolynomialGF2mSmallM[] polynomialGF2mSmallMArr) {
        int i;
        int length = polynomialGF2mSmallMArr.length;
        int[] iArr = new int[length];
        int[] iArr2 = new int[length];
        for (i = 0; i < this.coefficients.length; i++) {
            iArr2[i] = this.field.mult(this.coefficients[i], this.coefficients[i]);
        }
        for (i = 0; i < length; i++) {
            for (int i2 = 0; i2 < length; i2++) {
                if (i < polynomialGF2mSmallMArr[i2].coefficients.length) {
                    iArr[i] = this.field.add(iArr[i], this.field.mult(polynomialGF2mSmallMArr[i2].coefficients[i], iArr2[i2]));
                }
            }
        }
        return new PolynomialGF2mSmallM(this.field, iArr);
    }

    public PolynomialGF2mSmallM modSquareRoot(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        int[] clone = IntUtils.clone(this.coefficients);
        while (true) {
            int[] modMultiply = modMultiply(clone, clone, polynomialGF2mSmallM.coefficients);
            if (isEqual(modMultiply, this.coefficients)) {
                return new PolynomialGF2mSmallM(this.field, clone);
            }
            clone = normalForm(modMultiply);
        }
    }

    public PolynomialGF2mSmallM modSquareRootMatrix(PolynomialGF2mSmallM[] polynomialGF2mSmallMArr) {
        int length = polynomialGF2mSmallMArr.length;
        int[] iArr = new int[length];
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = 0;
            while (i3 < length) {
                if (i2 < polynomialGF2mSmallMArr[i3].coefficients.length && i3 < this.coefficients.length) {
                    iArr[i2] = this.field.add(iArr[i2], this.field.mult(polynomialGF2mSmallMArr[i3].coefficients[i2], this.coefficients[i3]));
                }
                i3++;
            }
        }
        while (i < length) {
            iArr[i] = this.field.sqRoot(iArr[i]);
            i++;
        }
        return new PolynomialGF2mSmallM(this.field, iArr);
    }

    public void multThisWithElement(int i) {
        if (this.field.isElementOfThisField(i)) {
            this.coefficients = multWithElement(this.coefficients, i);
            computeDegree();
            return;
        }
        throw new ArithmeticException("Not an element of the finite field this polynomial is defined over.");
    }

    public PolynomialGF2mSmallM multWithElement(int i) {
        if (this.field.isElementOfThisField(i)) {
            return new PolynomialGF2mSmallM(this.field, multWithElement(this.coefficients, i));
        }
        throw new ArithmeticException("Not an element of the finite field this polynomial is defined over.");
    }

    public PolynomialGF2mSmallM multWithMonomial(int i) {
        return new PolynomialGF2mSmallM(this.field, multWithMonomial(this.coefficients, i));
    }

    public PolynomialGF2mSmallM multiply(PolynomialGF2mSmallM polynomialGF2mSmallM) {
        return new PolynomialGF2mSmallM(this.field, multiply(this.coefficients, polynomialGF2mSmallM.coefficients));
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" Polynomial over ");
        stringBuilder.append(this.field.toString());
        stringBuilder.append(": \n");
        String stringBuilder2 = stringBuilder.toString();
        for (int i = 0; i < this.coefficients.length; i++) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(stringBuilder2);
            stringBuilder3.append(this.field.elementToStr(this.coefficients[i]));
            stringBuilder3.append("Y^");
            stringBuilder3.append(i);
            stringBuilder3.append("+");
            stringBuilder2 = stringBuilder3.toString();
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(stringBuilder2);
        stringBuilder4.append(";");
        return stringBuilder4.toString();
    }
}
