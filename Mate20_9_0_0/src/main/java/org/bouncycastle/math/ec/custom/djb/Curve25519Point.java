package org.bouncycastle.math.ec.custom.djb;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractFp;
import org.bouncycastle.math.raw.Nat256;

public class Curve25519Point extends AbstractFp {
    public Curve25519Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public Curve25519Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
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

    Curve25519Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
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
        Curve25519FieldElement curve25519FieldElement = (Curve25519FieldElement) this.x;
        Curve25519FieldElement curve25519FieldElement2 = (Curve25519FieldElement) this.y;
        Curve25519FieldElement curve25519FieldElement3 = (Curve25519FieldElement) this.zs[0];
        Curve25519FieldElement curve25519FieldElement4 = (Curve25519FieldElement) eCPoint.getXCoord();
        Curve25519FieldElement curve25519FieldElement5 = (Curve25519FieldElement) eCPoint.getYCoord();
        Curve25519FieldElement curve25519FieldElement6 = (Curve25519FieldElement) eCPoint.getZCoord(0);
        int[] createExt = Nat256.createExt();
        int[] create = Nat256.create();
        int[] create2 = Nat256.create();
        int[] create3 = Nat256.create();
        boolean isOne = curve25519FieldElement3.isOne();
        if (isOne) {
            iArr = curve25519FieldElement4.x;
            iArr2 = curve25519FieldElement5.x;
        } else {
            Curve25519Field.square(curve25519FieldElement3.x, create2);
            Curve25519Field.multiply(create2, curve25519FieldElement4.x, create);
            Curve25519Field.multiply(create2, curve25519FieldElement3.x, create2);
            Curve25519Field.multiply(create2, curve25519FieldElement5.x, create2);
            iArr = create;
            iArr2 = create2;
        }
        boolean isOne2 = curve25519FieldElement6.isOne();
        if (isOne2) {
            iArr3 = curve25519FieldElement.x;
            iArr4 = curve25519FieldElement2.x;
        } else {
            Curve25519Field.square(curve25519FieldElement6.x, create3);
            Curve25519Field.multiply(create3, curve25519FieldElement.x, createExt);
            Curve25519Field.multiply(create3, curve25519FieldElement6.x, create3);
            Curve25519Field.multiply(create3, curve25519FieldElement2.x, create3);
            iArr3 = createExt;
            iArr4 = create3;
        }
        int[] create4 = Nat256.create();
        Curve25519Field.subtract(iArr3, iArr, create4);
        Curve25519Field.subtract(iArr4, iArr2, create);
        if (Nat256.isZero(create4)) {
            return Nat256.isZero(create) ? twice() : curve.getInfinity();
        } else {
            iArr = Nat256.create();
            Curve25519Field.square(create4, iArr);
            iArr2 = Nat256.create();
            Curve25519Field.multiply(iArr, create4, iArr2);
            Curve25519Field.multiply(iArr, iArr3, create2);
            Curve25519Field.negate(iArr2, iArr2);
            Nat256.mul(iArr4, iArr2, createExt);
            Curve25519Field.reduce27(Nat256.addBothTo(create2, create2, iArr2), iArr2);
            curve25519FieldElement2 = new Curve25519FieldElement(create3);
            Curve25519Field.square(create, curve25519FieldElement2.x);
            Curve25519Field.subtract(curve25519FieldElement2.x, iArr2, curve25519FieldElement2.x);
            ECFieldElement curve25519FieldElement7 = new Curve25519FieldElement(iArr2);
            Curve25519Field.subtract(create2, curve25519FieldElement2.x, curve25519FieldElement7.x);
            Curve25519Field.multiplyAddToExt(curve25519FieldElement7.x, create, createExt);
            Curve25519Field.reduce(createExt, curve25519FieldElement7.x);
            curve25519FieldElement = new Curve25519FieldElement(create4);
            if (!isOne) {
                Curve25519Field.multiply(curve25519FieldElement.x, curve25519FieldElement3.x, curve25519FieldElement.x);
            }
            if (!isOne2) {
                Curve25519Field.multiply(curve25519FieldElement.x, curve25519FieldElement6.x, curve25519FieldElement.x);
            }
            if (!(isOne && isOne2)) {
                iArr = null;
            }
            curve25519FieldElement6 = calculateJacobianModifiedW(curve25519FieldElement, iArr);
            return new Curve25519Point(curve, curve25519FieldElement2, curve25519FieldElement7, new ECFieldElement[]{curve25519FieldElement, curve25519FieldElement6}, this.withCompression);
        }
    }

    protected Curve25519FieldElement calculateJacobianModifiedW(Curve25519FieldElement curve25519FieldElement, int[] iArr) {
        Curve25519FieldElement curve25519FieldElement2 = (Curve25519FieldElement) getCurve().getA();
        if (curve25519FieldElement.isOne()) {
            return curve25519FieldElement2;
        }
        Curve25519FieldElement curve25519FieldElement3 = new Curve25519FieldElement();
        if (iArr == null) {
            iArr = curve25519FieldElement3.x;
            Curve25519Field.square(curve25519FieldElement.x, iArr);
        }
        Curve25519Field.square(iArr, curve25519FieldElement3.x);
        Curve25519Field.multiply(curve25519FieldElement3.x, curve25519FieldElement2.x, curve25519FieldElement3.x);
        return curve25519FieldElement3;
    }

    protected ECPoint detach() {
        return new Curve25519Point(null, getAffineXCoord(), getAffineYCoord());
    }

    protected Curve25519FieldElement getJacobianModifiedW() {
        Curve25519FieldElement curve25519FieldElement = (Curve25519FieldElement) this.zs[1];
        if (curve25519FieldElement != null) {
            return curve25519FieldElement;
        }
        ECFieldElement[] eCFieldElementArr = this.zs;
        Curve25519FieldElement calculateJacobianModifiedW = calculateJacobianModifiedW((Curve25519FieldElement) this.zs[0], null);
        eCFieldElementArr[1] = calculateJacobianModifiedW;
        return calculateJacobianModifiedW;
    }

    public ECFieldElement getZCoord(int i) {
        return i == 1 ? getJacobianModifiedW() : super.getZCoord(i);
    }

    public ECPoint negate() {
        return isInfinity() ? this : new Curve25519Point(getCurve(), this.x, this.y.negate(), this.zs, this.withCompression);
    }

    public ECPoint threeTimes() {
        return (isInfinity() || this.y.isZero()) ? this : twiceJacobianModified(false).add(this);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        return this.y.isZero() ? getCurve().getInfinity() : twiceJacobianModified(true);
    }

    protected Curve25519Point twiceJacobianModified(boolean z) {
        Curve25519FieldElement curve25519FieldElement = (Curve25519FieldElement) this.x;
        Curve25519FieldElement curve25519FieldElement2 = (Curve25519FieldElement) this.y;
        Curve25519FieldElement curve25519FieldElement3 = (Curve25519FieldElement) this.zs[0];
        Curve25519FieldElement jacobianModifiedW = getJacobianModifiedW();
        int[] create = Nat256.create();
        Curve25519Field.square(curve25519FieldElement.x, create);
        Curve25519Field.reduce27(Nat256.addBothTo(create, create, create) + Nat256.addTo(jacobianModifiedW.x, create), create);
        int[] create2 = Nat256.create();
        Curve25519Field.twice(curve25519FieldElement2.x, create2);
        int[] create3 = Nat256.create();
        Curve25519Field.multiply(create2, curve25519FieldElement2.x, create3);
        int[] create4 = Nat256.create();
        Curve25519Field.multiply(create3, curve25519FieldElement.x, create4);
        Curve25519Field.twice(create4, create4);
        int[] create5 = Nat256.create();
        Curve25519Field.square(create3, create5);
        Curve25519Field.twice(create5, create5);
        Curve25519FieldElement curve25519FieldElement4 = new Curve25519FieldElement(create3);
        Curve25519Field.square(create, curve25519FieldElement4.x);
        Curve25519Field.subtract(curve25519FieldElement4.x, create4, curve25519FieldElement4.x);
        Curve25519Field.subtract(curve25519FieldElement4.x, create4, curve25519FieldElement4.x);
        Curve25519FieldElement curve25519FieldElement5 = new Curve25519FieldElement(create4);
        Curve25519Field.subtract(create4, curve25519FieldElement4.x, curve25519FieldElement5.x);
        Curve25519Field.multiply(curve25519FieldElement5.x, create, curve25519FieldElement5.x);
        Curve25519Field.subtract(curve25519FieldElement5.x, create5, curve25519FieldElement5.x);
        curve25519FieldElement2 = new Curve25519FieldElement(create2);
        if (!Nat256.isOne(curve25519FieldElement3.x)) {
            Curve25519Field.multiply(curve25519FieldElement2.x, curve25519FieldElement3.x, curve25519FieldElement2.x);
        }
        curve25519FieldElement3 = null;
        if (z) {
            curve25519FieldElement3 = new Curve25519FieldElement(create5);
            Curve25519Field.multiply(curve25519FieldElement3.x, jacobianModifiedW.x, curve25519FieldElement3.x);
            Curve25519Field.twice(curve25519FieldElement3.x, curve25519FieldElement3.x);
        }
        return new Curve25519Point(getCurve(), curve25519FieldElement4, curve25519FieldElement5, new ECFieldElement[]{curve25519FieldElement2, curve25519FieldElement3}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return this == eCPoint ? threeTimes() : isInfinity() ? eCPoint : eCPoint.isInfinity() ? twice() : this.y.isZero() ? eCPoint : twiceJacobianModified(false).add(eCPoint);
    }
}
