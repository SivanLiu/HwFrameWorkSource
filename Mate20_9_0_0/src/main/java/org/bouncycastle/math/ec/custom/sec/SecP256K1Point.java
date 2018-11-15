package org.bouncycastle.math.ec.custom.sec;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractFp;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat256;

public class SecP256K1Point extends AbstractFp {
    public SecP256K1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public SecP256K1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        super(eCCurve, eCFieldElement, eCFieldElement2);
        Object obj = null;
        Object obj2 = eCFieldElement == null ? 1 : null;
        if (eCFieldElement2 == null) {
            obj = 1;
        }
        if (obj2 == obj) {
            this.withCompression = z;
            return;
        }
        throw new IllegalArgumentException("Exactly one of the field elements is null");
    }

    SecP256K1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
        this.withCompression = z;
    }

    public ECPoint add(ECPoint eCPoint) {
        if (isInfinity()) {
            return eCPoint;
        }
        if (eCPoint.isInfinity()) {
            return this;
        }
        if (this == eCPoint) {
            return twice();
        }
        int[] iArr;
        int[] iArr2;
        int[] iArr3;
        int[] iArr4;
        ECCurve curve = getCurve();
        SecP256K1FieldElement secP256K1FieldElement = (SecP256K1FieldElement) this.x;
        SecP256K1FieldElement secP256K1FieldElement2 = (SecP256K1FieldElement) this.y;
        SecP256K1FieldElement secP256K1FieldElement3 = (SecP256K1FieldElement) eCPoint.getXCoord();
        SecP256K1FieldElement secP256K1FieldElement4 = (SecP256K1FieldElement) eCPoint.getYCoord();
        SecP256K1FieldElement secP256K1FieldElement5 = (SecP256K1FieldElement) this.zs[0];
        SecP256K1FieldElement secP256K1FieldElement6 = (SecP256K1FieldElement) eCPoint.getZCoord(0);
        int[] createExt = Nat256.createExt();
        int[] create = Nat256.create();
        int[] create2 = Nat256.create();
        int[] create3 = Nat256.create();
        boolean isOne = secP256K1FieldElement5.isOne();
        if (isOne) {
            iArr = secP256K1FieldElement3.x;
            iArr2 = secP256K1FieldElement4.x;
        } else {
            SecP256K1Field.square(secP256K1FieldElement5.x, create2);
            SecP256K1Field.multiply(create2, secP256K1FieldElement3.x, create);
            SecP256K1Field.multiply(create2, secP256K1FieldElement5.x, create2);
            SecP256K1Field.multiply(create2, secP256K1FieldElement4.x, create2);
            iArr = create;
            iArr2 = create2;
        }
        boolean isOne2 = secP256K1FieldElement6.isOne();
        if (isOne2) {
            iArr3 = secP256K1FieldElement.x;
            iArr4 = secP256K1FieldElement2.x;
        } else {
            SecP256K1Field.square(secP256K1FieldElement6.x, create3);
            SecP256K1Field.multiply(create3, secP256K1FieldElement.x, createExt);
            SecP256K1Field.multiply(create3, secP256K1FieldElement6.x, create3);
            SecP256K1Field.multiply(create3, secP256K1FieldElement2.x, create3);
            iArr3 = createExt;
            iArr4 = create3;
        }
        int[] create4 = Nat256.create();
        SecP256K1Field.subtract(iArr3, iArr, create4);
        SecP256K1Field.subtract(iArr4, iArr2, create);
        if (Nat256.isZero(create4)) {
            return Nat256.isZero(create) ? twice() : curve.getInfinity();
        } else {
            SecP256K1Field.square(create4, create2);
            iArr = Nat256.create();
            SecP256K1Field.multiply(create2, create4, iArr);
            SecP256K1Field.multiply(create2, iArr3, create2);
            SecP256K1Field.negate(iArr, iArr);
            Nat256.mul(iArr4, iArr, createExt);
            SecP256K1Field.reduce32(Nat256.addBothTo(create2, create2, iArr), iArr);
            ECFieldElement secP256K1FieldElement7 = new SecP256K1FieldElement(create3);
            SecP256K1Field.square(create, secP256K1FieldElement7.x);
            SecP256K1Field.subtract(secP256K1FieldElement7.x, iArr, secP256K1FieldElement7.x);
            ECFieldElement secP256K1FieldElement8 = new SecP256K1FieldElement(iArr);
            SecP256K1Field.subtract(create2, secP256K1FieldElement7.x, secP256K1FieldElement8.x);
            SecP256K1Field.multiplyAddToExt(secP256K1FieldElement8.x, create, createExt);
            SecP256K1Field.reduce(createExt, secP256K1FieldElement8.x);
            secP256K1FieldElement = new SecP256K1FieldElement(create4);
            if (!isOne) {
                SecP256K1Field.multiply(secP256K1FieldElement.x, secP256K1FieldElement5.x, secP256K1FieldElement.x);
            }
            if (!isOne2) {
                SecP256K1Field.multiply(secP256K1FieldElement.x, secP256K1FieldElement6.x, secP256K1FieldElement.x);
            }
            return new SecP256K1Point(curve, secP256K1FieldElement7, secP256K1FieldElement8, new ECFieldElement[]{secP256K1FieldElement}, this.withCompression);
        }
    }

    protected ECPoint detach() {
        return new SecP256K1Point(null, getAffineXCoord(), getAffineYCoord());
    }

    public ECPoint negate() {
        return isInfinity() ? this : new SecP256K1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }

    public ECPoint threeTimes() {
        return (isInfinity() || this.y.isZero()) ? this : twice().add(this);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        SecP256K1FieldElement secP256K1FieldElement = (SecP256K1FieldElement) this.y;
        if (secP256K1FieldElement.isZero()) {
            return curve.getInfinity();
        }
        SecP256K1FieldElement secP256K1FieldElement2 = (SecP256K1FieldElement) this.x;
        SecP256K1FieldElement secP256K1FieldElement3 = (SecP256K1FieldElement) this.zs[0];
        int[] create = Nat256.create();
        SecP256K1Field.square(secP256K1FieldElement.x, create);
        int[] create2 = Nat256.create();
        SecP256K1Field.square(create, create2);
        int[] create3 = Nat256.create();
        SecP256K1Field.square(secP256K1FieldElement2.x, create3);
        SecP256K1Field.reduce32(Nat256.addBothTo(create3, create3, create3), create3);
        SecP256K1Field.multiply(create, secP256K1FieldElement2.x, create);
        SecP256K1Field.reduce32(Nat.shiftUpBits(8, create, 2, 0), create);
        int[] create4 = Nat256.create();
        SecP256K1Field.reduce32(Nat.shiftUpBits(8, create2, 3, 0, create4), create4);
        ECFieldElement secP256K1FieldElement4 = new SecP256K1FieldElement(create2);
        SecP256K1Field.square(create3, secP256K1FieldElement4.x);
        SecP256K1Field.subtract(secP256K1FieldElement4.x, create, secP256K1FieldElement4.x);
        SecP256K1Field.subtract(secP256K1FieldElement4.x, create, secP256K1FieldElement4.x);
        ECFieldElement secP256K1FieldElement5 = new SecP256K1FieldElement(create);
        SecP256K1Field.subtract(create, secP256K1FieldElement4.x, secP256K1FieldElement5.x);
        SecP256K1Field.multiply(secP256K1FieldElement5.x, create3, secP256K1FieldElement5.x);
        SecP256K1Field.subtract(secP256K1FieldElement5.x, create4, secP256K1FieldElement5.x);
        secP256K1FieldElement2 = new SecP256K1FieldElement(create3);
        SecP256K1Field.twice(secP256K1FieldElement.x, secP256K1FieldElement2.x);
        if (!secP256K1FieldElement3.isOne()) {
            SecP256K1Field.multiply(secP256K1FieldElement2.x, secP256K1FieldElement3.x, secP256K1FieldElement2.x);
        }
        return new SecP256K1Point(curve, secP256K1FieldElement4, secP256K1FieldElement5, new ECFieldElement[]{secP256K1FieldElement2}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return this == eCPoint ? threeTimes() : isInfinity() ? eCPoint : eCPoint.isInfinity() ? twice() : this.y.isZero() ? eCPoint : twice().add(eCPoint);
    }
}
