package org.bouncycastle.pqc.crypto.rainbow.util;

import java.lang.reflect.Array;

public class ComputeInField {
    private short[][] A;
    short[] x;

    private void computeZerosAbove() throws RuntimeException {
        for (int length = this.A.length - 1; length > 0; length--) {
            int i = length - 1;
            while (i >= 0) {
                short s = this.A[i][length];
                short invElem = GF2Field.invElem(this.A[length][length]);
                if (invElem != (short) 0) {
                    for (int i2 = length; i2 < 2 * this.A.length; i2++) {
                        this.A[i][i2] = GF2Field.addElem(this.A[i][i2], GF2Field.multElem(s, GF2Field.multElem(this.A[length][i2], invElem)));
                    }
                    i--;
                } else {
                    throw new RuntimeException("The matrix is not invertible");
                }
            }
        }
    }

    private void computeZerosUnder(boolean z) throws RuntimeException {
        int length = z ? 2 * this.A.length : this.A.length + 1;
        int i = 0;
        while (i < this.A.length - 1) {
            int i2 = i + 1;
            int i3 = i2;
            while (i3 < this.A.length) {
                short s = this.A[i3][i];
                short invElem = GF2Field.invElem(this.A[i][i]);
                if (invElem != (short) 0) {
                    for (int i4 = i; i4 < length; i4++) {
                        this.A[i3][i4] = GF2Field.addElem(this.A[i3][i4], GF2Field.multElem(s, GF2Field.multElem(this.A[i][i4], invElem)));
                    }
                    i3++;
                } else {
                    throw new IllegalStateException("Matrix not invertible! We have to choose another one!");
                }
            }
            i = i2;
        }
    }

    private void substitute() throws IllegalStateException {
        short invElem = GF2Field.invElem(this.A[this.A.length - 1][this.A.length - 1]);
        if (invElem != (short) 0) {
            this.x[this.A.length - 1] = GF2Field.multElem(this.A[this.A.length - 1][this.A.length], invElem);
            int length = this.A.length - 2;
            while (length >= 0) {
                short s = this.A[length][this.A.length];
                for (int length2 = this.A.length - 1; length2 > length; length2--) {
                    s = GF2Field.addElem(s, GF2Field.multElem(this.A[length][length2], this.x[length2]));
                }
                short invElem2 = GF2Field.invElem(this.A[length][length]);
                if (invElem2 != (short) 0) {
                    this.x[length] = GF2Field.multElem(s, invElem2);
                    length--;
                } else {
                    throw new IllegalStateException("Not solvable equation system");
                }
            }
            return;
        }
        throw new IllegalStateException("The equation system is not solvable");
    }

    public short[][] addSquareMatrix(short[][] sArr, short[][] sArr2) {
        if (sArr.length == sArr2.length && sArr[0].length == sArr2[0].length) {
            short[][] sArr3 = (short[][]) Array.newInstance(short.class, new int[]{sArr.length, sArr.length});
            for (int i = 0; i < sArr.length; i++) {
                for (int i2 = 0; i2 < sArr2.length; i2++) {
                    sArr3[i][i2] = GF2Field.addElem(sArr[i][i2], sArr2[i][i2]);
                }
            }
            return sArr3;
        }
        throw new RuntimeException("Addition is not possible!");
    }

    public short[] addVect(short[] sArr, short[] sArr2) {
        if (sArr.length == sArr2.length) {
            short[] sArr3 = new short[sArr.length];
            for (int i = 0; i < sArr3.length; i++) {
                sArr3[i] = GF2Field.addElem(sArr[i], sArr2[i]);
            }
            return sArr3;
        }
        throw new RuntimeException("Multiplication is not possible!");
    }

    public short[][] inverse(short[][] sArr) {
        try {
            this.A = (short[][]) Array.newInstance(short.class, new int[]{sArr.length, sArr.length * 2});
            int i = 0;
            if (sArr.length == sArr[0].length) {
                int i2;
                int i3;
                for (i2 = 0; i2 < sArr.length; i2++) {
                    for (i3 = 0; i3 < sArr.length; i3++) {
                        this.A[i2][i3] = sArr[i2][i3];
                    }
                    for (i3 = sArr.length; i3 < sArr.length * 2; i3++) {
                        this.A[i2][i3] = (short) 0;
                    }
                    this.A[i2][this.A.length + i2] = (short) 1;
                }
                computeZerosUnder(true);
                for (int i4 = 0; i4 < this.A.length; i4++) {
                    short invElem = GF2Field.invElem(this.A[i4][i4]);
                    for (i3 = i4; i3 < this.A.length * 2; i3++) {
                        this.A[i4][i3] = GF2Field.multElem(this.A[i4][i3], invElem);
                    }
                }
                computeZerosAbove();
                sArr = (short[][]) Array.newInstance(short.class, new int[]{this.A.length, this.A.length});
                while (i < this.A.length) {
                    for (i2 = this.A.length; i2 < this.A.length * 2; i2++) {
                        sArr[i][i2 - this.A.length] = this.A[i][i2];
                    }
                    i++;
                }
                return sArr;
            }
            throw new RuntimeException("The matrix is not invertible. Please choose another one!");
        } catch (RuntimeException e) {
            return (short[][]) null;
        }
    }

    public short[][] multMatrix(short s, short[][] sArr) {
        short[][] sArr2 = (short[][]) Array.newInstance(short.class, new int[]{sArr.length, sArr[0].length});
        for (int i = 0; i < sArr.length; i++) {
            for (int i2 = 0; i2 < sArr[0].length; i2++) {
                sArr2[i][i2] = GF2Field.multElem(s, sArr[i][i2]);
            }
        }
        return sArr2;
    }

    public short[] multVect(short s, short[] sArr) {
        short[] sArr2 = new short[sArr.length];
        for (int i = 0; i < sArr2.length; i++) {
            sArr2[i] = GF2Field.multElem(s, sArr[i]);
        }
        return sArr2;
    }

    public short[][] multVects(short[] sArr, short[] sArr2) {
        if (sArr.length == sArr2.length) {
            short[][] sArr3 = (short[][]) Array.newInstance(short.class, new int[]{sArr.length, sArr2.length});
            for (int i = 0; i < sArr.length; i++) {
                for (int i2 = 0; i2 < sArr2.length; i2++) {
                    sArr3[i][i2] = GF2Field.multElem(sArr[i], sArr2[i2]);
                }
            }
            return sArr3;
        }
        throw new RuntimeException("Multiplication is not possible!");
    }

    public short[] multiplyMatrix(short[][] sArr, short[] sArr2) throws RuntimeException {
        if (sArr[0].length == sArr2.length) {
            short[] sArr3 = new short[sArr.length];
            for (int i = 0; i < sArr.length; i++) {
                for (int i2 = 0; i2 < sArr2.length; i2++) {
                    sArr3[i] = GF2Field.addElem(sArr3[i], GF2Field.multElem(sArr[i][i2], sArr2[i2]));
                }
            }
            return sArr3;
        }
        throw new RuntimeException("Multiplication is not possible!");
    }

    public short[][] multiplyMatrix(short[][] sArr, short[][] sArr2) throws RuntimeException {
        if (sArr[0].length == sArr2.length) {
            this.A = (short[][]) Array.newInstance(short.class, new int[]{sArr.length, sArr2[0].length});
            for (int i = 0; i < sArr.length; i++) {
                for (int i2 = 0; i2 < sArr2.length; i2++) {
                    for (int i3 = 0; i3 < sArr2[0].length; i3++) {
                        this.A[i][i3] = GF2Field.addElem(this.A[i][i3], GF2Field.multElem(sArr[i][i2], sArr2[i2][i3]));
                    }
                }
            }
            return this.A;
        }
        throw new RuntimeException("Multiplication is not possible!");
    }

    public short[] solveEquation(short[][] sArr, short[] sArr2) {
        if (sArr.length != sArr2.length) {
            return null;
        }
        try {
            this.A = (short[][]) Array.newInstance(short.class, new int[]{sArr.length, sArr.length + 1});
            this.x = new short[sArr.length];
            for (int i = 0; i < sArr.length; i++) {
                for (int i2 = 0; i2 < sArr[0].length; i2++) {
                    this.A[i][i2] = sArr[i][i2];
                }
            }
            for (int i3 = 0; i3 < sArr2.length; i3++) {
                this.A[i3][sArr2.length] = GF2Field.addElem(sArr2[i3], this.A[i3][sArr2.length]);
            }
            computeZerosUnder(false);
            substitute();
            return this.x;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
