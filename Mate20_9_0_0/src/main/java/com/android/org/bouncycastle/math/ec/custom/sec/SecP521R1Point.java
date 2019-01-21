package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.math.ec.ECPoint.AbstractFp;
import com.android.org.bouncycastle.math.raw.Nat;

public class SecP521R1Point extends AbstractFp {
    public SecP521R1Point(ECCurve curve, ECFieldElement x, ECFieldElement y) {
        this(curve, x, y, false);
    }

    public SecP521R1Point(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
        super(curve, x, y);
        Object obj = null;
        Object obj2 = x == null ? 1 : null;
        if (y == null) {
            obj = 1;
        }
        if (obj2 == obj) {
            this.withCompression = withCompression;
            return;
        }
        throw new IllegalArgumentException("Exactly one of the field elements is null");
    }

    SecP521R1Point(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
        super(curve, x, y, zs);
        this.withCompression = withCompression;
    }

    protected ECPoint detach() {
        return new SecP521R1Point(null, getAffineXCoord(), getAffineYCoord());
    }

    public ECPoint add(ECPoint b) {
        ECPoint eCPoint = b;
        if (isInfinity()) {
            return eCPoint;
        }
        if (b.isInfinity()) {
            return this;
        }
        if (this == eCPoint) {
            return twice();
        }
        int[] U2;
        int[] S2;
        int[] S1;
        ECCurve curve = getCurve();
        SecP521R1FieldElement X1 = (SecP521R1FieldElement) this.x;
        SecP521R1FieldElement Y1 = (SecP521R1FieldElement) this.y;
        SecP521R1FieldElement X2 = (SecP521R1FieldElement) b.getXCoord();
        SecP521R1FieldElement Y2 = (SecP521R1FieldElement) b.getYCoord();
        SecP521R1FieldElement Z1 = (SecP521R1FieldElement) this.zs[0];
        SecP521R1FieldElement Z2 = (SecP521R1FieldElement) eCPoint.getZCoord(0);
        int[] t1 = Nat.create(17);
        int[] t2 = Nat.create(17);
        int[] t3 = Nat.create(17);
        int[] t4 = Nat.create(17);
        boolean Z1IsOne = Z1.isOne();
        if (Z1IsOne) {
            U2 = X2.x;
            S2 = Y2.x;
        } else {
            S2 = t3;
            SecP521R1Field.square(Z1.x, S2);
            U2 = t2;
            SecP521R1Field.multiply(S2, X2.x, U2);
            SecP521R1Field.multiply(S2, Z1.x, S2);
            SecP521R1Field.multiply(S2, Y2.x, S2);
        }
        int[] S22 = S2;
        boolean Z2IsOne = Z2.isOne();
        if (Z2IsOne) {
            S2 = X1.x;
            S1 = Y1.x;
        } else {
            S1 = t4;
            SecP521R1Field.square(Z2.x, S1);
            S2 = t1;
            SecP521R1Field.multiply(S1, X1.x, S2);
            SecP521R1Field.multiply(S1, Z2.x, S1);
            SecP521R1Field.multiply(S1, Y1.x, S1);
        }
        int[] U1 = S2;
        int[] H = Nat.create(17);
        SecP521R1Field.subtract(U1, U2, H);
        S2 = t2;
        SecP521R1Field.subtract(S1, S22, S2);
        int[] S23 = S22;
        if (!Nat.isZero(17, H)) {
            ECFieldElement Y3;
            S22 = t3;
            SecP521R1Field.square(H, S22);
            U2 = Nat.create(17);
            SecP521R1Field.multiply(S22, H, U2);
            int[] V = t3;
            SecP521R1Field.multiply(S22, U1, V);
            SecP521R1Field.multiply(S1, U2, t1);
            ECFieldElement X3 = new SecP521R1FieldElement(t4);
            int[] S12 = S1;
            SecP521R1Field.square(S2, X3.x);
            int[] HSquared = S22;
            SecP521R1Field.add(X3.x, U2, X3.x);
            SecP521R1Field.subtract(X3.x, V, X3.x);
            SecP521R1Field.subtract(X3.x, V, X3.x);
            ECFieldElement Y32 = new SecP521R1FieldElement(U2);
            int[] G = U2;
            SecP521R1Field.subtract(V, X3.x, Y32.x);
            SecP521R1Field.multiply(Y32.x, S2, t2);
            SecP521R1Field.subtract(t2, t1, Y32.x);
            SecP521R1FieldElement Z3 = new SecP521R1FieldElement(H);
            int[] t42;
            if (Z1IsOne) {
                Y3 = Y32;
                t42 = t4;
            } else {
                Y3 = Y32;
                t42 = t4;
                SecP521R1Field.multiply(Z3.x, Z1.x, Z3.x);
            }
            if (!Z2IsOne) {
                SecP521R1Field.multiply(Z3.x, Z2.x, Z3.x);
            }
            return new SecP521R1Point(curve, X3, Y3, new ECFieldElement[]{Z3}, this.withCompression);
        } else if (Nat.isZero(17, S2)) {
            return twice();
        } else {
            return curve.getInfinity();
        }
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        SecP521R1FieldElement Y1 = (SecP521R1FieldElement) this.y;
        if (Y1.isZero()) {
            return curve.getInfinity();
        }
        SecP521R1FieldElement X1 = (SecP521R1FieldElement) this.x;
        SecP521R1FieldElement Z1 = (SecP521R1FieldElement) this.zs[0];
        int[] t1 = Nat.create(17);
        int[] t2 = Nat.create(17);
        int[] Y1Squared = Nat.create(17);
        SecP521R1Field.square(Y1.x, Y1Squared);
        int[] T = Nat.create(17);
        SecP521R1Field.square(Y1Squared, T);
        boolean Z1IsOne = Z1.isOne();
        int[] Z1Squared = Z1.x;
        if (!Z1IsOne) {
            Z1Squared = t2;
            SecP521R1Field.square(Z1.x, Z1Squared);
        }
        int[] Z1Squared2 = Z1Squared;
        SecP521R1Field.subtract(X1.x, Z1Squared2, t1);
        int[] M = t2;
        SecP521R1Field.add(X1.x, Z1Squared2, M);
        SecP521R1Field.multiply(M, t1, M);
        Nat.addBothTo(17, M, M, M);
        SecP521R1Field.reduce23(M);
        int[] S = Y1Squared;
        SecP521R1Field.multiply(Y1Squared, X1.x, S);
        Nat.shiftUpBits(17, S, 2, 0);
        SecP521R1Field.reduce23(S);
        Nat.shiftUpBits(17, T, 3, 0, t1);
        SecP521R1Field.reduce23(t1);
        ECFieldElement X3 = new SecP521R1FieldElement(T);
        SecP521R1Field.square(M, X3.x);
        SecP521R1Field.subtract(X3.x, S, X3.x);
        SecP521R1Field.subtract(X3.x, S, X3.x);
        ECFieldElement Y3 = new SecP521R1FieldElement(S);
        ECFieldElement X32 = X3;
        SecP521R1Field.subtract(S, X3.x, Y3.x);
        SecP521R1Field.multiply(Y3.x, M, Y3.x);
        SecP521R1Field.subtract(Y3.x, t1, Y3.x);
        SecP521R1FieldElement Z3 = new SecP521R1FieldElement(M);
        ECFieldElement Y32 = Y3;
        SecP521R1Field.twice(Y1.x, Z3.x);
        if (Z1IsOne) {
        } else {
            SecP521R1Field.multiply(Z3.x, Z1.x, Z3.x);
        }
        ECFieldElement[] eCFieldElementArr = new ECFieldElement[]{Z3};
        return new SecP521R1Point(curve, X32, Y32, eCFieldElementArr, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint b) {
        if (this == b) {
            return threeTimes();
        }
        if (isInfinity()) {
            return b;
        }
        if (b.isInfinity()) {
            return twice();
        }
        if (this.y.isZero()) {
            return b;
        }
        return twice().add(b);
    }

    public ECPoint threeTimes() {
        if (isInfinity() || this.y.isZero()) {
            return this;
        }
        return twice().add(this);
    }

    protected ECFieldElement two(ECFieldElement x) {
        return x.add(x);
    }

    protected ECFieldElement three(ECFieldElement x) {
        return two(x).add(x);
    }

    protected ECFieldElement four(ECFieldElement x) {
        return two(two(x));
    }

    protected ECFieldElement eight(ECFieldElement x) {
        return four(two(x));
    }

    protected ECFieldElement doubleProductFromSquares(ECFieldElement a, ECFieldElement b, ECFieldElement aSquared, ECFieldElement bSquared) {
        return a.add(b).square().subtract(aSquared).subtract(bSquared);
    }

    public ECPoint negate() {
        if (isInfinity()) {
            return this;
        }
        return new SecP521R1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }
}
