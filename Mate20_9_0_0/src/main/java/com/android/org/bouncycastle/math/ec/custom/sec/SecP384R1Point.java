package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.math.ec.ECPoint.AbstractFp;
import com.android.org.bouncycastle.math.raw.Nat;
import com.android.org.bouncycastle.math.raw.Nat384;

public class SecP384R1Point extends AbstractFp {
    public SecP384R1Point(ECCurve curve, ECFieldElement x, ECFieldElement y) {
        this(curve, x, y, false);
    }

    public SecP384R1Point(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
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

    SecP384R1Point(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
        super(curve, x, y, zs);
        this.withCompression = withCompression;
    }

    protected ECPoint detach() {
        return new SecP384R1Point(null, getAffineXCoord(), getAffineYCoord());
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
        SecP384R1FieldElement X1 = (SecP384R1FieldElement) this.x;
        SecP384R1FieldElement Y1 = (SecP384R1FieldElement) this.y;
        SecP384R1FieldElement X2 = (SecP384R1FieldElement) b.getXCoord();
        SecP384R1FieldElement Y2 = (SecP384R1FieldElement) b.getYCoord();
        SecP384R1FieldElement Z1 = (SecP384R1FieldElement) this.zs[0];
        SecP384R1FieldElement Z2 = (SecP384R1FieldElement) eCPoint.getZCoord(0);
        int[] tt1 = Nat.create(24);
        int[] tt2 = Nat.create(24);
        int[] t3 = Nat.create(12);
        int[] t4 = Nat.create(12);
        boolean Z1IsOne = Z1.isOne();
        if (Z1IsOne) {
            U2 = X2.x;
            S2 = Y2.x;
        } else {
            S2 = t3;
            SecP384R1Field.square(Z1.x, S2);
            U2 = tt2;
            SecP384R1Field.multiply(S2, X2.x, U2);
            SecP384R1Field.multiply(S2, Z1.x, S2);
            SecP384R1Field.multiply(S2, Y2.x, S2);
        }
        int[] S22 = S2;
        boolean Z2IsOne = Z2.isOne();
        if (Z2IsOne) {
            S2 = X1.x;
            S1 = Y1.x;
        } else {
            S1 = t4;
            SecP384R1Field.square(Z2.x, S1);
            S2 = tt1;
            SecP384R1Field.multiply(S1, X1.x, S2);
            SecP384R1Field.multiply(S1, Z2.x, S1);
            SecP384R1Field.multiply(S1, Y1.x, S1);
        }
        int[] U1 = S2;
        int[] H = Nat.create(12);
        SecP384R1Field.subtract(U1, U2, H);
        int[] R = Nat.create(12);
        SecP384R1Field.subtract(S1, S22, R);
        if (!Nat.isZero(12, H)) {
            ECFieldElement Y3;
            S2 = t3;
            SecP384R1Field.square(H, S2);
            int[] G = Nat.create(12);
            SecP384R1Field.multiply(S2, H, G);
            S22 = t3;
            SecP384R1Field.multiply(S2, U1, S22);
            SecP384R1Field.negate(G, G);
            Nat384.mul(S1, G, tt1);
            int c = Nat.addBothTo(12, S22, S22, G);
            SecP384R1Field.reduce32(c, G);
            ECFieldElement X3 = new SecP384R1FieldElement(t4);
            SecP384R1Field.square(R, X3.x);
            SecP384R1Field.subtract(X3.x, G, X3.x);
            ECFieldElement Y32 = new SecP384R1FieldElement(G);
            SecP384R1Field.subtract(S22, X3.x, Y32.x);
            Nat384.mul(Y32.x, R, tt2);
            SecP384R1Field.addExt(tt1, tt2, tt1);
            SecP384R1Field.reduce(tt1, Y32.x);
            SecP384R1FieldElement Z3 = new SecP384R1FieldElement(H);
            if (Z1IsOne) {
                Y3 = Y32;
            } else {
                Y3 = Y32;
                SecP384R1Field.multiply(Z3.x, Z1.x, Z3.x);
            }
            if (!Z2IsOne) {
                SecP384R1Field.multiply(Z3.x, Z2.x, Z3.x);
            }
            return new SecP384R1Point(curve, X3, Y3, new ECFieldElement[]{Z3}, this.withCompression);
        } else if (Nat.isZero(12, R)) {
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
        SecP384R1FieldElement Y1 = (SecP384R1FieldElement) this.y;
        if (Y1.isZero()) {
            return curve.getInfinity();
        }
        SecP384R1FieldElement X1 = (SecP384R1FieldElement) this.x;
        SecP384R1FieldElement Z1 = (SecP384R1FieldElement) this.zs[0];
        int[] t1 = Nat.create(12);
        int[] t2 = Nat.create(12);
        int[] Y1Squared = Nat.create(12);
        SecP384R1Field.square(Y1.x, Y1Squared);
        int[] T = Nat.create(12);
        SecP384R1Field.square(Y1Squared, T);
        boolean Z1IsOne = Z1.isOne();
        int[] Z1Squared = Z1.x;
        if (!Z1IsOne) {
            Z1Squared = t2;
            SecP384R1Field.square(Z1.x, Z1Squared);
        }
        int[] Z1Squared2 = Z1Squared;
        SecP384R1Field.subtract(X1.x, Z1Squared2, t1);
        int[] M = t2;
        SecP384R1Field.add(X1.x, Z1Squared2, M);
        SecP384R1Field.multiply(M, t1, M);
        int c = Nat.addBothTo(12, M, M, M);
        SecP384R1Field.reduce32(c, M);
        int[] S = Y1Squared;
        SecP384R1Field.multiply(Y1Squared, X1.x, S);
        int i = c;
        int c2 = Nat.shiftUpBits(12, S, 2, 0);
        SecP384R1Field.reduce32(c2, S);
        c = Nat.shiftUpBits(12, T, 3, 0, t1);
        SecP384R1Field.reduce32(c, t1);
        ECFieldElement X3 = new SecP384R1FieldElement(T);
        SecP384R1Field.square(M, X3.x);
        int c3 = c;
        SecP384R1Field.subtract(X3.x, S, X3.x);
        SecP384R1Field.subtract(X3.x, S, X3.x);
        ECFieldElement Y3 = new SecP384R1FieldElement(S);
        ECFieldElement X32 = X3;
        SecP384R1Field.subtract(S, X3.x, Y3.x);
        SecP384R1Field.multiply(Y3.x, M, Y3.x);
        SecP384R1Field.subtract(Y3.x, t1, Y3.x);
        SecP384R1FieldElement Z3 = new SecP384R1FieldElement(M);
        ECFieldElement Y32 = Y3;
        SecP384R1Field.twice(Y1.x, Z3.x);
        int[] S2;
        if (Z1IsOne) {
            S2 = S;
        } else {
            S2 = S;
            SecP384R1Field.multiply(Z3.x, Z1.x, Z3.x);
        }
        ECFieldElement[] eCFieldElementArr = new ECFieldElement[]{Z3};
        ECFieldElement Y33 = Y32;
        return new SecP384R1Point(curve, X32, Y33, eCFieldElementArr, this.withCompression);
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

    public ECPoint negate() {
        if (isInfinity()) {
            return this;
        }
        return new SecP384R1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }
}
