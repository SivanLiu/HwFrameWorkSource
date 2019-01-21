package org.bouncycastle.math.ec.custom.sec;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractFp;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat192;

public class SecP192R1Point extends AbstractFp {
    public SecP192R1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public SecP192R1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
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

    SecP192R1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
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
        SecP192R1FieldElement secP192R1FieldElement = (SecP192R1FieldElement) this.x;
        SecP192R1FieldElement secP192R1FieldElement2 = (SecP192R1FieldElement) this.y;
        SecP192R1FieldElement secP192R1FieldElement3 = (SecP192R1FieldElement) eCPoint.getXCoord();
        SecP192R1FieldElement secP192R1FieldElement4 = (SecP192R1FieldElement) eCPoint.getYCoord();
        SecP192R1FieldElement secP192R1FieldElement5 = (SecP192R1FieldElement) this.zs[0];
        SecP192R1FieldElement secP192R1FieldElement6 = (SecP192R1FieldElement) eCPoint.getZCoord(0);
        int[] createExt = Nat192.createExt();
        int[] create = Nat192.create();
        int[] create2 = Nat192.create();
        int[] create3 = Nat192.create();
        boolean isOne = secP192R1FieldElement5.isOne();
        if (isOne) {
            iArr = secP192R1FieldElement3.x;
            iArr2 = secP192R1FieldElement4.x;
        } else {
            SecP192R1Field.square(secP192R1FieldElement5.x, create2);
            SecP192R1Field.multiply(create2, secP192R1FieldElement3.x, create);
            SecP192R1Field.multiply(create2, secP192R1FieldElement5.x, create2);
            SecP192R1Field.multiply(create2, secP192R1FieldElement4.x, create2);
            iArr = create;
            iArr2 = create2;
        }
        boolean isOne2 = secP192R1FieldElement6.isOne();
        if (isOne2) {
            iArr3 = secP192R1FieldElement.x;
            iArr4 = secP192R1FieldElement2.x;
        } else {
            SecP192R1Field.square(secP192R1FieldElement6.x, create3);
            SecP192R1Field.multiply(create3, secP192R1FieldElement.x, createExt);
            SecP192R1Field.multiply(create3, secP192R1FieldElement6.x, create3);
            SecP192R1Field.multiply(create3, secP192R1FieldElement2.x, create3);
            iArr3 = createExt;
            iArr4 = create3;
        }
        int[] create4 = Nat192.create();
        SecP192R1Field.subtract(iArr3, iArr, create4);
        SecP192R1Field.subtract(iArr4, iArr2, create);
        if (Nat192.isZero(create4)) {
            return Nat192.isZero(create) ? twice() : curve.getInfinity();
        } else {
            SecP192R1Field.square(create4, create2);
            iArr = Nat192.create();
            SecP192R1Field.multiply(create2, create4, iArr);
            SecP192R1Field.multiply(create2, iArr3, create2);
            SecP192R1Field.negate(iArr, iArr);
            Nat192.mul(iArr4, iArr, createExt);
            SecP192R1Field.reduce32(Nat192.addBothTo(create2, create2, iArr), iArr);
            secP192R1FieldElement2 = new SecP192R1FieldElement(create3);
            SecP192R1Field.square(create, secP192R1FieldElement2.x);
            SecP192R1Field.subtract(secP192R1FieldElement2.x, iArr, secP192R1FieldElement2.x);
            ECFieldElement secP192R1FieldElement7 = new SecP192R1FieldElement(iArr);
            SecP192R1Field.subtract(create2, secP192R1FieldElement2.x, secP192R1FieldElement7.x);
            SecP192R1Field.multiplyAddToExt(secP192R1FieldElement7.x, create, createExt);
            SecP192R1Field.reduce(createExt, secP192R1FieldElement7.x);
            secP192R1FieldElement = new SecP192R1FieldElement(create4);
            if (!isOne) {
                SecP192R1Field.multiply(secP192R1FieldElement.x, secP192R1FieldElement5.x, secP192R1FieldElement.x);
            }
            if (!isOne2) {
                SecP192R1Field.multiply(secP192R1FieldElement.x, secP192R1FieldElement6.x, secP192R1FieldElement.x);
            }
            return new SecP192R1Point(curve, secP192R1FieldElement2, secP192R1FieldElement7, new ECFieldElement[]{secP192R1FieldElement}, this.withCompression);
        }
    }

    protected ECPoint detach() {
        return new SecP192R1Point(null, getAffineXCoord(), getAffineYCoord());
    }

    public ECPoint negate() {
        return isInfinity() ? this : new SecP192R1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }

    public ECPoint threeTimes() {
        return (isInfinity() || this.y.isZero()) ? this : twice().add(this);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        SecP192R1FieldElement secP192R1FieldElement = (SecP192R1FieldElement) this.y;
        if (secP192R1FieldElement.isZero()) {
            return curve.getInfinity();
        }
        SecP192R1FieldElement secP192R1FieldElement2 = (SecP192R1FieldElement) this.x;
        SecP192R1FieldElement secP192R1FieldElement3 = (SecP192R1FieldElement) this.zs[0];
        int[] create = Nat192.create();
        int[] create2 = Nat192.create();
        int[] create3 = Nat192.create();
        SecP192R1Field.square(secP192R1FieldElement.x, create3);
        int[] create4 = Nat192.create();
        SecP192R1Field.square(create3, create4);
        boolean isOne = secP192R1FieldElement3.isOne();
        int[] iArr = secP192R1FieldElement3.x;
        if (!isOne) {
            SecP192R1Field.square(secP192R1FieldElement3.x, create2);
            iArr = create2;
        }
        SecP192R1Field.subtract(secP192R1FieldElement2.x, iArr, create);
        SecP192R1Field.add(secP192R1FieldElement2.x, iArr, create2);
        SecP192R1Field.multiply(create2, create, create2);
        SecP192R1Field.reduce32(Nat192.addBothTo(create2, create2, create2), create2);
        SecP192R1Field.multiply(create3, secP192R1FieldElement2.x, create3);
        SecP192R1Field.reduce32(Nat.shiftUpBits(6, create3, 2, 0), create3);
        SecP192R1Field.reduce32(Nat.shiftUpBits(6, create4, 3, 0, create), create);
        ECFieldElement secP192R1FieldElement4 = new SecP192R1FieldElement(create4);
        SecP192R1Field.square(create2, secP192R1FieldElement4.x);
        SecP192R1Field.subtract(secP192R1FieldElement4.x, create3, secP192R1FieldElement4.x);
        SecP192R1Field.subtract(secP192R1FieldElement4.x, create3, secP192R1FieldElement4.x);
        ECFieldElement secP192R1FieldElement5 = new SecP192R1FieldElement(create3);
        SecP192R1Field.subtract(create3, secP192R1FieldElement4.x, secP192R1FieldElement5.x);
        SecP192R1Field.multiply(secP192R1FieldElement5.x, create2, secP192R1FieldElement5.x);
        SecP192R1Field.subtract(secP192R1FieldElement5.x, create, secP192R1FieldElement5.x);
        secP192R1FieldElement2 = new SecP192R1FieldElement(create2);
        SecP192R1Field.twice(secP192R1FieldElement.x, secP192R1FieldElement2.x);
        if (!isOne) {
            SecP192R1Field.multiply(secP192R1FieldElement2.x, secP192R1FieldElement3.x, secP192R1FieldElement2.x);
        }
        return new SecP192R1Point(curve, secP192R1FieldElement4, secP192R1FieldElement5, new ECFieldElement[]{secP192R1FieldElement2}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return this == eCPoint ? threeTimes() : isInfinity() ? eCPoint : eCPoint.isInfinity() ? twice() : this.y.isZero() ? eCPoint : twice().add(eCPoint);
    }
}
