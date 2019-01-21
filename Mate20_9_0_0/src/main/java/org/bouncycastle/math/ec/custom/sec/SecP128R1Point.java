package org.bouncycastle.math.ec.custom.sec;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractFp;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.math.raw.Nat128;

public class SecP128R1Point extends AbstractFp {
    public SecP128R1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public SecP128R1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
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

    SecP128R1Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
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
        SecP128R1FieldElement secP128R1FieldElement = (SecP128R1FieldElement) this.x;
        SecP128R1FieldElement secP128R1FieldElement2 = (SecP128R1FieldElement) this.y;
        SecP128R1FieldElement secP128R1FieldElement3 = (SecP128R1FieldElement) eCPoint.getXCoord();
        SecP128R1FieldElement secP128R1FieldElement4 = (SecP128R1FieldElement) eCPoint.getYCoord();
        SecP128R1FieldElement secP128R1FieldElement5 = (SecP128R1FieldElement) this.zs[0];
        SecP128R1FieldElement secP128R1FieldElement6 = (SecP128R1FieldElement) eCPoint.getZCoord(0);
        int[] createExt = Nat128.createExt();
        int[] create = Nat128.create();
        int[] create2 = Nat128.create();
        int[] create3 = Nat128.create();
        boolean isOne = secP128R1FieldElement5.isOne();
        if (isOne) {
            iArr = secP128R1FieldElement3.x;
            iArr2 = secP128R1FieldElement4.x;
        } else {
            SecP128R1Field.square(secP128R1FieldElement5.x, create2);
            SecP128R1Field.multiply(create2, secP128R1FieldElement3.x, create);
            SecP128R1Field.multiply(create2, secP128R1FieldElement5.x, create2);
            SecP128R1Field.multiply(create2, secP128R1FieldElement4.x, create2);
            iArr = create;
            iArr2 = create2;
        }
        boolean isOne2 = secP128R1FieldElement6.isOne();
        if (isOne2) {
            iArr3 = secP128R1FieldElement.x;
            iArr4 = secP128R1FieldElement2.x;
        } else {
            SecP128R1Field.square(secP128R1FieldElement6.x, create3);
            SecP128R1Field.multiply(create3, secP128R1FieldElement.x, createExt);
            SecP128R1Field.multiply(create3, secP128R1FieldElement6.x, create3);
            SecP128R1Field.multiply(create3, secP128R1FieldElement2.x, create3);
            iArr3 = createExt;
            iArr4 = create3;
        }
        int[] create4 = Nat128.create();
        SecP128R1Field.subtract(iArr3, iArr, create4);
        SecP128R1Field.subtract(iArr4, iArr2, create);
        if (Nat128.isZero(create4)) {
            return Nat128.isZero(create) ? twice() : curve.getInfinity();
        } else {
            SecP128R1Field.square(create4, create2);
            iArr = Nat128.create();
            SecP128R1Field.multiply(create2, create4, iArr);
            SecP128R1Field.multiply(create2, iArr3, create2);
            SecP128R1Field.negate(iArr, iArr);
            Nat128.mul(iArr4, iArr, createExt);
            SecP128R1Field.reduce32(Nat128.addBothTo(create2, create2, iArr), iArr);
            secP128R1FieldElement2 = new SecP128R1FieldElement(create3);
            SecP128R1Field.square(create, secP128R1FieldElement2.x);
            SecP128R1Field.subtract(secP128R1FieldElement2.x, iArr, secP128R1FieldElement2.x);
            ECFieldElement secP128R1FieldElement7 = new SecP128R1FieldElement(iArr);
            SecP128R1Field.subtract(create2, secP128R1FieldElement2.x, secP128R1FieldElement7.x);
            SecP128R1Field.multiplyAddToExt(secP128R1FieldElement7.x, create, createExt);
            SecP128R1Field.reduce(createExt, secP128R1FieldElement7.x);
            secP128R1FieldElement = new SecP128R1FieldElement(create4);
            if (!isOne) {
                SecP128R1Field.multiply(secP128R1FieldElement.x, secP128R1FieldElement5.x, secP128R1FieldElement.x);
            }
            if (!isOne2) {
                SecP128R1Field.multiply(secP128R1FieldElement.x, secP128R1FieldElement6.x, secP128R1FieldElement.x);
            }
            return new SecP128R1Point(curve, secP128R1FieldElement2, secP128R1FieldElement7, new ECFieldElement[]{secP128R1FieldElement}, this.withCompression);
        }
    }

    protected ECPoint detach() {
        return new SecP128R1Point(null, getAffineXCoord(), getAffineYCoord());
    }

    public ECPoint negate() {
        return isInfinity() ? this : new SecP128R1Point(this.curve, this.x, this.y.negate(), this.zs, this.withCompression);
    }

    public ECPoint threeTimes() {
        return (isInfinity() || this.y.isZero()) ? this : twice().add(this);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        SecP128R1FieldElement secP128R1FieldElement = (SecP128R1FieldElement) this.y;
        if (secP128R1FieldElement.isZero()) {
            return curve.getInfinity();
        }
        SecP128R1FieldElement secP128R1FieldElement2 = (SecP128R1FieldElement) this.x;
        SecP128R1FieldElement secP128R1FieldElement3 = (SecP128R1FieldElement) this.zs[0];
        int[] create = Nat128.create();
        int[] create2 = Nat128.create();
        int[] create3 = Nat128.create();
        SecP128R1Field.square(secP128R1FieldElement.x, create3);
        int[] create4 = Nat128.create();
        SecP128R1Field.square(create3, create4);
        boolean isOne = secP128R1FieldElement3.isOne();
        int[] iArr = secP128R1FieldElement3.x;
        if (!isOne) {
            SecP128R1Field.square(secP128R1FieldElement3.x, create2);
            iArr = create2;
        }
        SecP128R1Field.subtract(secP128R1FieldElement2.x, iArr, create);
        SecP128R1Field.add(secP128R1FieldElement2.x, iArr, create2);
        SecP128R1Field.multiply(create2, create, create2);
        SecP128R1Field.reduce32(Nat128.addBothTo(create2, create2, create2), create2);
        SecP128R1Field.multiply(create3, secP128R1FieldElement2.x, create3);
        SecP128R1Field.reduce32(Nat.shiftUpBits(4, create3, 2, 0), create3);
        SecP128R1Field.reduce32(Nat.shiftUpBits(4, create4, 3, 0, create), create);
        ECFieldElement secP128R1FieldElement4 = new SecP128R1FieldElement(create4);
        SecP128R1Field.square(create2, secP128R1FieldElement4.x);
        SecP128R1Field.subtract(secP128R1FieldElement4.x, create3, secP128R1FieldElement4.x);
        SecP128R1Field.subtract(secP128R1FieldElement4.x, create3, secP128R1FieldElement4.x);
        ECFieldElement secP128R1FieldElement5 = new SecP128R1FieldElement(create3);
        SecP128R1Field.subtract(create3, secP128R1FieldElement4.x, secP128R1FieldElement5.x);
        SecP128R1Field.multiply(secP128R1FieldElement5.x, create2, secP128R1FieldElement5.x);
        SecP128R1Field.subtract(secP128R1FieldElement5.x, create, secP128R1FieldElement5.x);
        secP128R1FieldElement2 = new SecP128R1FieldElement(create2);
        SecP128R1Field.twice(secP128R1FieldElement.x, secP128R1FieldElement2.x);
        if (!isOne) {
            SecP128R1Field.multiply(secP128R1FieldElement2.x, secP128R1FieldElement3.x, secP128R1FieldElement2.x);
        }
        return new SecP128R1Point(curve, secP128R1FieldElement4, secP128R1FieldElement5, new ECFieldElement[]{secP128R1FieldElement2}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return this == eCPoint ? threeTimes() : isInfinity() ? eCPoint : eCPoint.isInfinity() ? twice() : this.y.isZero() ? eCPoint : twice().add(eCPoint);
    }
}
