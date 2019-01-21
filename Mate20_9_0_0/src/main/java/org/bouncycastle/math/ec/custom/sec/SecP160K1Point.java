package org.bouncycastle.math.ec.custom.sec;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractFp;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat160;

public class SecP160K1Point extends AbstractFp {
    public SecP160K1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public SecP160K1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
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

    SecP160K1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
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
        SecP160R2FieldElement secP160R2FieldElement = (SecP160R2FieldElement) this.x;
        SecP160R2FieldElement secP160R2FieldElement2 = (SecP160R2FieldElement) this.y;
        SecP160R2FieldElement secP160R2FieldElement3 = (SecP160R2FieldElement) eCPoint.getXCoord();
        SecP160R2FieldElement secP160R2FieldElement4 = (SecP160R2FieldElement) eCPoint.getYCoord();
        SecP160R2FieldElement secP160R2FieldElement5 = (SecP160R2FieldElement) this.zs[0];
        SecP160R2FieldElement secP160R2FieldElement6 = (SecP160R2FieldElement) eCPoint.getZCoord(0);
        int[] createExt = Nat160.createExt();
        int[] create = Nat160.create();
        int[] create2 = Nat160.create();
        int[] create3 = Nat160.create();
        boolean isOne = secP160R2FieldElement5.isOne();
        if (isOne) {
            iArr = secP160R2FieldElement3.x;
            iArr2 = secP160R2FieldElement4.x;
        } else {
            SecP160R2Field.square(secP160R2FieldElement5.x, create2);
            SecP160R2Field.multiply(create2, secP160R2FieldElement3.x, create);
            SecP160R2Field.multiply(create2, secP160R2FieldElement5.x, create2);
            SecP160R2Field.multiply(create2, secP160R2FieldElement4.x, create2);
            iArr = create;
            iArr2 = create2;
        }
        boolean isOne2 = secP160R2FieldElement6.isOne();
        if (isOne2) {
            iArr3 = secP160R2FieldElement.x;
            iArr4 = secP160R2FieldElement2.x;
        } else {
            SecP160R2Field.square(secP160R2FieldElement6.x, create3);
            SecP160R2Field.multiply(create3, secP160R2FieldElement.x, createExt);
            SecP160R2Field.multiply(create3, secP160R2FieldElement6.x, create3);
            SecP160R2Field.multiply(create3, secP160R2FieldElement2.x, create3);
            iArr3 = createExt;
            iArr4 = create3;
        }
        int[] create4 = Nat160.create();
        SecP160R2Field.subtract(iArr3, iArr, create4);
        SecP160R2Field.subtract(iArr4, iArr2, create);
        if (Nat160.isZero(create4)) {
            return Nat160.isZero(create) ? twice() : curve.getInfinity();
        } else {
            SecP160R2Field.square(create4, create2);
            iArr = Nat160.create();
            SecP160R2Field.multiply(create2, create4, iArr);
            SecP160R2Field.multiply(create2, iArr3, create2);
            SecP160R2Field.negate(iArr, iArr);
            Nat160.mul(iArr4, iArr, createExt);
            SecP160R2Field.reduce32(Nat160.addBothTo(create2, create2, iArr), iArr);
            secP160R2FieldElement2 = new SecP160R2FieldElement(create3);
            SecP160R2Field.square(create, secP160R2FieldElement2.x);
            SecP160R2Field.subtract(secP160R2FieldElement2.x, iArr, secP160R2FieldElement2.x);
            ECFieldElement secP160R2FieldElement7 = new SecP160R2FieldElement(iArr);
            SecP160R2Field.subtract(create2, secP160R2FieldElement2.x, secP160R2FieldElement7.x);
            SecP160R2Field.multiplyAddToExt(secP160R2FieldElement7.x, create, createExt);
            SecP160R2Field.reduce(createExt, secP160R2FieldElement7.x);
            secP160R2FieldElement = new SecP160R2FieldElement(create4);
            if (!isOne) {
                SecP160R2Field.multiply(secP160R2FieldElement.x, secP160R2FieldElement5.x, secP160R2FieldElement.x);
            }
            if (!isOne2) {
                SecP160R2Field.multiply(secP160R2FieldElement.x, secP160R2FieldElement6.x, secP160R2FieldElement.x);
            }
            return new SecP160K1Point(curve, secP160R2FieldElement2, secP160R2FieldElement7, new ECFieldElement[]{secP160R2FieldElement}, this.withCompression);
        }
    }

    protected ECPoint detach() {
        return new SecP160K1Point(null, getAffineXCoord(), getAffineYCoord());
    }

    public ECPoint negate() {
        return isInfinity() ? this : new SecP160K1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }

    public ECPoint threeTimes() {
        return (isInfinity() || this.y.isZero()) ? this : twice().add(this);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        SecP160R2FieldElement secP160R2FieldElement = (SecP160R2FieldElement) this.y;
        if (secP160R2FieldElement.isZero()) {
            return curve.getInfinity();
        }
        SecP160R2FieldElement secP160R2FieldElement2 = (SecP160R2FieldElement) this.x;
        SecP160R2FieldElement secP160R2FieldElement3 = (SecP160R2FieldElement) this.zs[0];
        int[] create = Nat160.create();
        SecP160R2Field.square(secP160R2FieldElement.x, create);
        int[] create2 = Nat160.create();
        SecP160R2Field.square(create, create2);
        int[] create3 = Nat160.create();
        SecP160R2Field.square(secP160R2FieldElement2.x, create3);
        SecP160R2Field.reduce32(Nat160.addBothTo(create3, create3, create3), create3);
        SecP160R2Field.multiply(create, secP160R2FieldElement2.x, create);
        SecP160R2Field.reduce32(Nat.shiftUpBits(5, create, 2, 0), create);
        int[] create4 = Nat160.create();
        SecP160R2Field.reduce32(Nat.shiftUpBits(5, create2, 3, 0, create4), create4);
        ECFieldElement secP160R2FieldElement4 = new SecP160R2FieldElement(create2);
        SecP160R2Field.square(create3, secP160R2FieldElement4.x);
        SecP160R2Field.subtract(secP160R2FieldElement4.x, create, secP160R2FieldElement4.x);
        SecP160R2Field.subtract(secP160R2FieldElement4.x, create, secP160R2FieldElement4.x);
        ECFieldElement secP160R2FieldElement5 = new SecP160R2FieldElement(create);
        SecP160R2Field.subtract(create, secP160R2FieldElement4.x, secP160R2FieldElement5.x);
        SecP160R2Field.multiply(secP160R2FieldElement5.x, create3, secP160R2FieldElement5.x);
        SecP160R2Field.subtract(secP160R2FieldElement5.x, create4, secP160R2FieldElement5.x);
        secP160R2FieldElement2 = new SecP160R2FieldElement(create3);
        SecP160R2Field.twice(secP160R2FieldElement.x, secP160R2FieldElement2.x);
        if (!secP160R2FieldElement3.isOne()) {
            SecP160R2Field.multiply(secP160R2FieldElement2.x, secP160R2FieldElement3.x, secP160R2FieldElement2.x);
        }
        return new SecP160K1Point(curve, secP160R2FieldElement4, secP160R2FieldElement5, new ECFieldElement[]{secP160R2FieldElement2}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return this == eCPoint ? threeTimes() : isInfinity() ? eCPoint : eCPoint.isInfinity() ? twice() : this.y.isZero() ? eCPoint : twice().add(eCPoint);
    }
}
