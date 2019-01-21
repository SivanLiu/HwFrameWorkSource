package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.math.ec.ECPoint.AbstractFp;
import com.android.org.bouncycastle.math.raw.Nat;
import com.android.org.bouncycastle.math.raw.Nat256;

public class SecP256K1Point extends AbstractFp {
    public SecP256K1Point(ECCurve curve, ECFieldElement x, ECFieldElement y) {
        this(curve, x, y, false);
    }

    public SecP256K1Point(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
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

    SecP256K1Point(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
        super(curve, x, y, zs);
        this.withCompression = withCompression;
    }

    protected ECPoint detach() {
        return new SecP256K1Point(null, getAffineXCoord(), getAffineYCoord());
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
        SecP256K1FieldElement X1 = (SecP256K1FieldElement) this.x;
        SecP256K1FieldElement Y1 = (SecP256K1FieldElement) this.y;
        SecP256K1FieldElement X2 = (SecP256K1FieldElement) b.getXCoord();
        SecP256K1FieldElement Y2 = (SecP256K1FieldElement) b.getYCoord();
        SecP256K1FieldElement Z1 = (SecP256K1FieldElement) this.zs[0];
        SecP256K1FieldElement Z2 = (SecP256K1FieldElement) eCPoint.getZCoord(0);
        int[] tt1 = Nat256.createExt();
        int[] t2 = Nat256.create();
        int[] t3 = Nat256.create();
        int[] t4 = Nat256.create();
        boolean Z1IsOne = Z1.isOne();
        if (Z1IsOne) {
            U2 = X2.x;
            S2 = Y2.x;
        } else {
            S2 = t3;
            SecP256K1Field.square(Z1.x, S2);
            U2 = t2;
            SecP256K1Field.multiply(S2, X2.x, U2);
            SecP256K1Field.multiply(S2, Z1.x, S2);
            SecP256K1Field.multiply(S2, Y2.x, S2);
        }
        int[] U22 = U2;
        boolean Z2IsOne = Z2.isOne();
        if (Z2IsOne) {
            U2 = X1.x;
            S1 = Y1.x;
        } else {
            S1 = t4;
            SecP256K1Field.square(Z2.x, S1);
            U2 = tt1;
            SecP256K1Field.multiply(S1, X1.x, U2);
            SecP256K1Field.multiply(S1, Z2.x, S1);
            SecP256K1Field.multiply(S1, Y1.x, S1);
        }
        int[] U1 = U2;
        U2 = S1;
        S1 = Nat256.create();
        SecP256K1Field.subtract(U1, U22, S1);
        int[] R = t2;
        SecP256K1Field.subtract(U2, S2, R);
        if (!Nat256.isZero(S1)) {
            ECFieldElement Y3;
            int[] HSquared = t3;
            SecP256K1Field.square(S1, HSquared);
            int[] G = Nat256.create();
            SecP256K1Field.multiply(HSquared, S1, G);
            int[] V = t3;
            SecP256K1Field.multiply(HSquared, U1, V);
            SecP256K1Field.negate(G, G);
            Nat256.mul(U2, G, tt1);
            SecP256K1Field.reduce32(Nat256.addBothTo(V, V, G), G);
            int[] S12 = U2;
            ECFieldElement X3 = new SecP256K1FieldElement(t4);
            int[] U12 = U1;
            SecP256K1Field.square(R, X3.x);
            int[] S22 = S2;
            SecP256K1Field.subtract(X3.x, G, X3.x);
            ECFieldElement Y32 = new SecP256K1FieldElement(G);
            ECFieldElement X32 = X3;
            SecP256K1Field.subtract(V, X3.x, Y32.x);
            SecP256K1Field.multiplyAddToExt(Y32.x, R, tt1);
            SecP256K1Field.reduce(tt1, Y32.x);
            SecP256K1FieldElement Z3 = new SecP256K1FieldElement(S1);
            if (Z1IsOne) {
                Y3 = Y32;
            } else {
                Y3 = Y32;
                SecP256K1Field.multiply(Z3.x, Z1.x, Z3.x);
            }
            if (!Z2IsOne) {
                SecP256K1Field.multiply(Z3.x, Z2.x, Z3.x);
            }
            ECFieldElement Y33 = Y3;
            return new SecP256K1Point(curve, X32, Y33, new ECFieldElement[]{Z3}, this.withCompression);
        } else if (Nat256.isZero(R)) {
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
        SecP256K1FieldElement Y1 = (SecP256K1FieldElement) this.y;
        if (Y1.isZero()) {
            return curve.getInfinity();
        }
        ECFieldElement Y3;
        SecP256K1FieldElement X1 = (SecP256K1FieldElement) this.x;
        SecP256K1FieldElement Z1 = (SecP256K1FieldElement) this.zs[0];
        int[] Y1Squared = Nat256.create();
        SecP256K1Field.square(Y1.x, Y1Squared);
        int[] T = Nat256.create();
        SecP256K1Field.square(Y1Squared, T);
        int[] M = Nat256.create();
        SecP256K1Field.square(X1.x, M);
        SecP256K1Field.reduce32(Nat256.addBothTo(M, M, M), M);
        int[] S = Y1Squared;
        SecP256K1Field.multiply(Y1Squared, X1.x, S);
        SecP256K1Field.reduce32(Nat.shiftUpBits(8, S, 2, 0), S);
        int[] t1 = Nat256.create();
        int c = Nat.shiftUpBits(8, T, 3, 0, t1);
        SecP256K1Field.reduce32(c, t1);
        ECFieldElement X3 = new SecP256K1FieldElement(T);
        SecP256K1Field.square(M, X3.x);
        SecP256K1Field.subtract(X3.x, S, X3.x);
        SecP256K1Field.subtract(X3.x, S, X3.x);
        ECFieldElement Y32 = new SecP256K1FieldElement(S);
        SecP256K1Field.subtract(S, X3.x, Y32.x);
        SecP256K1Field.multiply(Y32.x, M, Y32.x);
        SecP256K1Field.subtract(Y32.x, t1, Y32.x);
        SecP256K1FieldElement Z3 = new SecP256K1FieldElement(M);
        SecP256K1Field.twice(Y1.x, Z3.x);
        if (Z1.isOne()) {
            Y3 = Y32;
        } else {
            Y3 = Y32;
            SecP256K1Field.multiply(Z3.x, Z1.x, Z3.x);
        }
        ECFieldElement[] eCFieldElementArr = new ECFieldElement[]{Z3};
        return new SecP256K1Point(curve, X3, Y3, eCFieldElementArr, this.withCompression);
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
        return new SecP256K1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }
}
