package org.bouncycastle.math.ec.custom.sec;

import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.ECPoint.AbstractF2m;

public class SecT193R2Point extends AbstractF2m {
    public SecT193R2Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, false);
    }

    public SecT193R2Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
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

    SecT193R2Point(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
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
        ECCurve curve = getCurve();
        ECFieldElement eCFieldElement = this.x;
        ECFieldElement rawXCoord = eCPoint.getRawXCoord();
        if (eCFieldElement.isZero()) {
            return rawXCoord.isZero() ? curve.getInfinity() : eCPoint.add(this);
        } else {
            ECFieldElement eCFieldElement2;
            ECFieldElement eCFieldElement3;
            ECFieldElement eCFieldElement4;
            ECFieldElement eCFieldElement5 = this.y;
            ECFieldElement eCFieldElement6 = this.zs[0];
            ECFieldElement rawYCoord = eCPoint.getRawYCoord();
            ECFieldElement zCoord = eCPoint.getZCoord(0);
            boolean isOne = eCFieldElement6.isOne();
            if (isOne) {
                eCFieldElement2 = rawXCoord;
                eCFieldElement3 = rawYCoord;
            } else {
                eCFieldElement2 = rawXCoord.multiply(eCFieldElement6);
                eCFieldElement3 = rawYCoord.multiply(eCFieldElement6);
            }
            boolean isOne2 = zCoord.isOne();
            if (isOne2) {
                eCFieldElement4 = eCFieldElement5;
            } else {
                eCFieldElement = eCFieldElement.multiply(zCoord);
                eCFieldElement4 = eCFieldElement5.multiply(zCoord);
            }
            eCFieldElement3 = eCFieldElement4.add(eCFieldElement3);
            eCFieldElement4 = eCFieldElement.add(eCFieldElement2);
            if (eCFieldElement4.isZero()) {
                return eCFieldElement3.isZero() ? twice() : curve.getInfinity();
            } else {
                if (rawXCoord.isZero()) {
                    eCPoint = normalize();
                    eCFieldElement = eCPoint.getXCoord();
                    zCoord = eCPoint.getYCoord();
                    rawXCoord = zCoord.add(rawYCoord).divide(eCFieldElement);
                    eCFieldElement5 = rawXCoord.square().add(rawXCoord).add(eCFieldElement).add(curve.getA());
                    if (eCFieldElement5.isZero()) {
                        return new SecT193R2Point(curve, eCFieldElement5, curve.getB().sqrt(), this.withCompression);
                    }
                    eCFieldElement6 = rawXCoord.multiply(eCFieldElement.add(eCFieldElement5)).add(eCFieldElement5).add(zCoord).divide(eCFieldElement5).add(eCFieldElement5);
                    zCoord = curve.fromBigInteger(ECConstants.ONE);
                } else {
                    rawXCoord = eCFieldElement4.square();
                    eCFieldElement = eCFieldElement3.multiply(eCFieldElement);
                    rawYCoord = eCFieldElement3.multiply(eCFieldElement2);
                    eCFieldElement = eCFieldElement.multiply(rawYCoord);
                    if (eCFieldElement.isZero()) {
                        return new SecT193R2Point(curve, eCFieldElement, curve.getB().sqrt(), this.withCompression);
                    }
                    eCFieldElement2 = eCFieldElement3.multiply(rawXCoord);
                    zCoord = !isOne2 ? eCFieldElement2.multiply(zCoord) : eCFieldElement2;
                    rawXCoord = rawYCoord.add(rawXCoord).squarePlusProduct(zCoord, eCFieldElement5.add(eCFieldElement6));
                    if (!isOne) {
                        zCoord = zCoord.multiply(eCFieldElement6);
                    }
                    eCFieldElement5 = eCFieldElement;
                    eCFieldElement6 = rawXCoord;
                }
                return new SecT193R2Point(curve, eCFieldElement5, eCFieldElement6, new ECFieldElement[]{zCoord}, this.withCompression);
            }
        }
    }

    protected ECPoint detach() {
        return new SecT193R2Point(null, getAffineXCoord(), getAffineYCoord());
    }

    protected boolean getCompressionYTilde() {
        ECFieldElement rawXCoord = getRawXCoord();
        boolean z = false;
        if (rawXCoord.isZero()) {
            return false;
        }
        if (getRawYCoord().testBitZero() != rawXCoord.testBitZero()) {
            z = true;
        }
        return z;
    }

    public ECFieldElement getYCoord() {
        ECFieldElement eCFieldElement = this.x;
        ECFieldElement eCFieldElement2 = this.y;
        if (isInfinity() || eCFieldElement.isZero()) {
            return eCFieldElement2;
        }
        eCFieldElement = eCFieldElement2.add(eCFieldElement).multiply(eCFieldElement);
        eCFieldElement2 = this.zs[0];
        if (!eCFieldElement2.isOne()) {
            eCFieldElement = eCFieldElement.divide(eCFieldElement2);
        }
        return eCFieldElement;
    }

    public ECPoint negate() {
        if (isInfinity()) {
            return this;
        }
        ECFieldElement eCFieldElement = this.x;
        if (eCFieldElement.isZero()) {
            return this;
        }
        ECFieldElement eCFieldElement2 = this.y;
        ECFieldElement eCFieldElement3 = this.zs[0];
        ECCurve eCCurve = this.curve;
        ECFieldElement[] eCFieldElementArr = new ECFieldElement[]{eCFieldElement3};
        return new SecT193R2Point(eCCurve, eCFieldElement, eCFieldElement2.add(eCFieldElement3), eCFieldElementArr, this.withCompression);
    }

    public ECPoint twice() {
        if (isInfinity()) {
            return this;
        }
        ECCurve curve = getCurve();
        ECFieldElement eCFieldElement = this.x;
        if (eCFieldElement.isZero()) {
            return curve.getInfinity();
        }
        ECFieldElement eCFieldElement2 = this.y;
        ECFieldElement eCFieldElement3 = this.zs[0];
        boolean isOne = eCFieldElement3.isOne();
        ECFieldElement multiply = isOne ? eCFieldElement2 : eCFieldElement2.multiply(eCFieldElement3);
        ECFieldElement square = isOne ? eCFieldElement3 : eCFieldElement3.square();
        ECFieldElement a = curve.getA();
        if (!isOne) {
            a = a.multiply(square);
        }
        eCFieldElement2 = eCFieldElement2.square().add(multiply).add(a);
        if (eCFieldElement2.isZero()) {
            return new SecT193R2Point(curve, eCFieldElement2, curve.getB().sqrt(), this.withCompression);
        }
        a = eCFieldElement2.square();
        square = isOne ? eCFieldElement2 : eCFieldElement2.multiply(square);
        if (!isOne) {
            eCFieldElement = eCFieldElement.multiply(eCFieldElement3);
        }
        return new SecT193R2Point(curve, a, eCFieldElement.squarePlusProduct(eCFieldElement2, multiply).add(a).add(square), new ECFieldElement[]{square}, this.withCompression);
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        if (isInfinity()) {
            return eCPoint;
        }
        if (eCPoint.isInfinity()) {
            return twice();
        }
        ECCurve curve = getCurve();
        ECFieldElement eCFieldElement = this.x;
        if (eCFieldElement.isZero()) {
            return eCPoint;
        }
        ECFieldElement rawXCoord = eCPoint.getRawXCoord();
        ECFieldElement zCoord = eCPoint.getZCoord(0);
        if (rawXCoord.isZero() || !zCoord.isOne()) {
            return twice().add(eCPoint);
        }
        zCoord = this.y;
        ECFieldElement eCFieldElement2 = this.zs[0];
        ECFieldElement rawYCoord = eCPoint.getRawYCoord();
        eCFieldElement = eCFieldElement.square();
        ECFieldElement square = zCoord.square();
        ECFieldElement square2 = eCFieldElement2.square();
        zCoord = curve.getA().multiply(square2).add(square).add(zCoord.multiply(eCFieldElement2));
        eCFieldElement2 = rawYCoord.addOne();
        eCFieldElement = curve.getA().add(eCFieldElement2).multiply(square2).add(square).multiplyPlusProduct(zCoord, eCFieldElement, square2);
        rawXCoord = rawXCoord.multiply(square2);
        rawYCoord = rawXCoord.add(zCoord).square();
        if (rawYCoord.isZero()) {
            return eCFieldElement.isZero() ? eCPoint.twice() : curve.getInfinity();
        } else {
            if (eCFieldElement.isZero()) {
                return new SecT193R2Point(curve, eCFieldElement, curve.getB().sqrt(), this.withCompression);
            }
            return new SecT193R2Point(curve, eCFieldElement.square().multiply(rawXCoord), eCFieldElement.add(rawYCoord).square().multiplyPlusProduct(zCoord, eCFieldElement2, eCFieldElement.multiply(rawYCoord).multiply(square2)), new ECFieldElement[]{eCFieldElement.multiply(rawYCoord).multiply(square2)}, this.withCompression);
        }
    }
}
