package org.bouncycastle.math.ec;

import java.math.BigInteger;
import java.util.Hashtable;

public abstract class ECPoint {
    protected static ECFieldElement[] EMPTY_ZS = new ECFieldElement[0];
    protected ECCurve curve;
    protected Hashtable preCompTable;
    protected boolean withCompression;
    protected ECFieldElement x;
    protected ECFieldElement y;
    protected ECFieldElement[] zs;

    public static abstract class AbstractF2m extends ECPoint {
        protected AbstractF2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
        }

        protected AbstractF2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
        }

        protected boolean satisfiesCurveEquation() {
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement a = curve.getA();
            ECFieldElement b = curve.getB();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement2;
            ECFieldElement eCFieldElement3;
            ECFieldElement square;
            if (coordinateSystem == 6) {
                eCFieldElement2 = this.zs[0];
                boolean isOne = eCFieldElement2.isOne();
                if (eCFieldElement.isZero()) {
                    Object multiply;
                    eCFieldElement = this.y.square();
                    if (!isOne) {
                        multiply = b.multiply(eCFieldElement2.square());
                    }
                    return eCFieldElement.equals(multiply);
                }
                Object add;
                eCFieldElement3 = this.y;
                eCFieldElement = eCFieldElement.square();
                if (isOne) {
                    eCFieldElement2 = eCFieldElement3.square().add(eCFieldElement3).add(a);
                    add = eCFieldElement.square().add(b);
                } else {
                    square = eCFieldElement2.square();
                    ECFieldElement square2 = square.square();
                    eCFieldElement2 = eCFieldElement3.add(eCFieldElement2).multiplyPlusProduct(eCFieldElement3, a, square);
                    add = eCFieldElement.squarePlusProduct(b, square2);
                }
                return eCFieldElement2.multiply(eCFieldElement).equals(add);
            }
            eCFieldElement3 = this.y;
            Object multiply2 = eCFieldElement3.add(eCFieldElement).multiply(eCFieldElement3);
            switch (coordinateSystem) {
                case 0:
                    break;
                case 1:
                    eCFieldElement2 = this.zs[0];
                    if (!eCFieldElement2.isOne()) {
                        square = eCFieldElement2.multiply(eCFieldElement2.square());
                        multiply2 = multiply2.multiply(eCFieldElement2);
                        a = a.multiply(eCFieldElement2);
                        b = b.multiply(square);
                        break;
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return multiply2.equals(eCFieldElement.add(a).multiply(eCFieldElement.square()).add(b));
        }

        public ECPoint scaleX(ECFieldElement eCFieldElement) {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement rawXCoord;
            ECFieldElement rawYCoord;
            switch (getCurveCoordinateSystem()) {
                case 5:
                    rawXCoord = getRawXCoord();
                    rawYCoord = getRawYCoord();
                    return getCurve().createRawPoint(rawXCoord, rawYCoord.add(rawXCoord).divide(eCFieldElement).add(rawXCoord.multiply(eCFieldElement)), getRawZCoords(), this.withCompression);
                case 6:
                    rawXCoord = getRawXCoord();
                    rawYCoord = getRawYCoord();
                    ECFieldElement eCFieldElement2 = getRawZCoords()[0];
                    ECFieldElement multiply = rawXCoord.multiply(eCFieldElement.square());
                    rawXCoord = rawYCoord.add(rawXCoord).add(multiply);
                    eCFieldElement = eCFieldElement2.multiply(eCFieldElement);
                    return getCurve().createRawPoint(multiply, rawXCoord, new ECFieldElement[]{eCFieldElement}, this.withCompression);
                default:
                    return super.scaleX(eCFieldElement);
            }
        }

        public ECPoint scaleY(ECFieldElement eCFieldElement) {
            if (isInfinity()) {
                return this;
            }
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    ECFieldElement rawXCoord = getRawXCoord();
                    return getCurve().createRawPoint(rawXCoord, getRawYCoord().add(rawXCoord).multiply(eCFieldElement).add(rawXCoord), getRawZCoords(), this.withCompression);
                default:
                    return super.scaleY(eCFieldElement);
            }
        }

        public ECPoint subtract(ECPoint eCPoint) {
            return eCPoint.isInfinity() ? this : add(eCPoint.negate());
        }

        public AbstractF2m tau() {
            if (isInfinity()) {
                return this;
            }
            ECPoint createRawPoint;
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement = this.x;
            switch (coordinateSystem) {
                case 0:
                case 5:
                    createRawPoint = curve.createRawPoint(eCFieldElement.square(), this.y.square(), this.withCompression);
                    break;
                case 1:
                case 6:
                    ECFieldElement eCFieldElement2 = this.y;
                    ECFieldElement eCFieldElement3 = this.zs[0];
                    createRawPoint = curve.createRawPoint(eCFieldElement.square(), eCFieldElement2.square(), new ECFieldElement[]{eCFieldElement3.square()}, this.withCompression);
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return (AbstractF2m) createRawPoint;
        }

        public AbstractF2m tauPow(int i) {
            if (isInfinity()) {
                return this;
            }
            ECPoint createRawPoint;
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement = this.x;
            switch (coordinateSystem) {
                case 0:
                case 5:
                    createRawPoint = curve.createRawPoint(eCFieldElement.squarePow(i), this.y.squarePow(i), this.withCompression);
                    break;
                case 1:
                case 6:
                    ECFieldElement eCFieldElement2 = this.y;
                    ECFieldElement eCFieldElement3 = this.zs[0];
                    createRawPoint = curve.createRawPoint(eCFieldElement.squarePow(i), eCFieldElement2.squarePow(i), new ECFieldElement[]{eCFieldElement3.squarePow(i)}, this.withCompression);
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return (AbstractF2m) createRawPoint;
        }
    }

    public static abstract class AbstractFp extends ECPoint {
        protected AbstractFp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
        }

        protected AbstractFp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
        }

        protected boolean getCompressionYTilde() {
            return getAffineYCoord().testBitZero();
        }

        protected boolean satisfiesCurveEquation() {
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement eCFieldElement2 = this.y;
            ECFieldElement a = this.curve.getA();
            ECFieldElement b = this.curve.getB();
            Object square = eCFieldElement2.square();
            ECFieldElement eCFieldElement3;
            ECFieldElement square2;
            switch (getCurveCoordinateSystem()) {
                case 0:
                    break;
                case 1:
                    eCFieldElement3 = this.zs[0];
                    if (!eCFieldElement3.isOne()) {
                        square2 = eCFieldElement3.square();
                        ECFieldElement multiply = eCFieldElement3.multiply(square2);
                        square = square.multiply(eCFieldElement3);
                        a = a.multiply(square2);
                        b = b.multiply(multiply);
                        break;
                    }
                    break;
                case 2:
                case 3:
                case 4:
                    eCFieldElement3 = this.zs[0];
                    if (!eCFieldElement3.isOne()) {
                        eCFieldElement3 = eCFieldElement3.square();
                        square2 = eCFieldElement3.square();
                        eCFieldElement3 = eCFieldElement3.multiply(square2);
                        a = a.multiply(square2);
                        b = b.multiply(eCFieldElement3);
                        break;
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return square.equals(eCFieldElement.square().add(a).multiply(eCFieldElement).add(b));
        }

        public ECPoint subtract(ECPoint eCPoint) {
            return eCPoint.isInfinity() ? this : add(eCPoint.negate());
        }
    }

    public static class F2m extends AbstractF2m {
        public F2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            this(eCCurve, eCFieldElement, eCFieldElement2, false);
        }

        public F2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
            Object obj = null;
            Object obj2 = eCFieldElement == null ? 1 : null;
            if (eCFieldElement2 == null) {
                obj = 1;
            }
            if (obj2 == obj) {
                if (eCFieldElement != null) {
                    org.bouncycastle.math.ec.ECFieldElement.F2m.checkFieldElements(this.x, this.y);
                    if (eCCurve != null) {
                        org.bouncycastle.math.ec.ECFieldElement.F2m.checkFieldElements(this.x, this.curve.getA());
                    }
                }
                this.withCompression = z;
                return;
            }
            throw new IllegalArgumentException("Exactly one of the field elements is null");
        }

        F2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
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
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement eCFieldElement2 = eCPoint.x;
            ECFieldElement eCFieldElement3;
            ECFieldElement eCFieldElement4;
            ECFieldElement eCFieldElement5;
            ECFieldElement eCFieldElement6;
            boolean isOne;
            ECFieldElement square;
            ECFieldElement multiply;
            ECFieldElement add;
            if (coordinateSystem != 6) {
                switch (coordinateSystem) {
                    case 0:
                        eCFieldElement3 = this.y;
                        eCFieldElement4 = eCPoint.y;
                        eCFieldElement2 = eCFieldElement.add(eCFieldElement2);
                        eCFieldElement4 = eCFieldElement3.add(eCFieldElement4);
                        if (eCFieldElement2.isZero()) {
                            return eCFieldElement4.isZero() ? twice() : curve.getInfinity();
                        } else {
                            eCFieldElement4 = eCFieldElement4.divide(eCFieldElement2);
                            eCFieldElement2 = eCFieldElement4.square().add(eCFieldElement4).add(eCFieldElement2).add(curve.getA());
                            return new F2m(curve, eCFieldElement2, eCFieldElement4.multiply(eCFieldElement.add(eCFieldElement2)).add(eCFieldElement2).add(eCFieldElement3), this.withCompression);
                        }
                    case 1:
                        eCFieldElement3 = this.y;
                        eCFieldElement5 = this.zs[0];
                        eCFieldElement6 = eCPoint.y;
                        eCFieldElement4 = eCPoint.zs[0];
                        isOne = eCFieldElement4.isOne();
                        eCFieldElement6 = eCFieldElement5.multiply(eCFieldElement6).add(isOne ? eCFieldElement3 : eCFieldElement3.multiply(eCFieldElement4));
                        eCFieldElement2 = eCFieldElement5.multiply(eCFieldElement2).add(isOne ? eCFieldElement : eCFieldElement.multiply(eCFieldElement4));
                        if (eCFieldElement2.isZero()) {
                            return eCFieldElement6.isZero() ? twice() : curve.getInfinity();
                        } else {
                            square = eCFieldElement2.square();
                            multiply = square.multiply(eCFieldElement2);
                            if (!isOne) {
                                eCFieldElement5 = eCFieldElement5.multiply(eCFieldElement4);
                            }
                            ECFieldElement add2 = eCFieldElement6.add(eCFieldElement2);
                            add = add2.multiplyPlusProduct(eCFieldElement6, square, curve.getA()).multiply(eCFieldElement5).add(multiply);
                            ECFieldElement multiply2 = eCFieldElement2.multiply(add);
                            if (!isOne) {
                                square = square.multiply(eCFieldElement4);
                            }
                            return new F2m(curve, multiply2, eCFieldElement6.multiplyPlusProduct(eCFieldElement, eCFieldElement2, eCFieldElement3).multiplyPlusProduct(square, add2, add), new ECFieldElement[]{multiply.multiply(eCFieldElement5)}, this.withCompression);
                        }
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            } else if (eCFieldElement.isZero()) {
                return eCFieldElement2.isZero() ? curve.getInfinity() : eCPoint.add(this);
            } else {
                eCFieldElement3 = this.y;
                eCFieldElement5 = this.zs[0];
                eCFieldElement6 = eCPoint.y;
                eCFieldElement4 = eCPoint.zs[0];
                isOne = eCFieldElement5.isOne();
                if (isOne) {
                    square = eCFieldElement2;
                    multiply = eCFieldElement6;
                } else {
                    square = eCFieldElement2.multiply(eCFieldElement5);
                    multiply = eCFieldElement6.multiply(eCFieldElement5);
                }
                boolean isOne2 = eCFieldElement4.isOne();
                if (isOne2) {
                    add = eCFieldElement3;
                } else {
                    eCFieldElement = eCFieldElement.multiply(eCFieldElement4);
                    add = eCFieldElement3.multiply(eCFieldElement4);
                }
                multiply = add.add(multiply);
                add = eCFieldElement.add(square);
                if (add.isZero()) {
                    return multiply.isZero() ? twice() : curve.getInfinity();
                } else {
                    if (eCFieldElement2.isZero()) {
                        eCPoint = normalize();
                        eCFieldElement3 = eCPoint.getXCoord();
                        eCFieldElement4 = eCPoint.getYCoord();
                        eCFieldElement = eCFieldElement4.add(eCFieldElement6).divide(eCFieldElement3);
                        eCFieldElement2 = eCFieldElement.square().add(eCFieldElement).add(eCFieldElement3).add(curve.getA());
                        if (eCFieldElement2.isZero()) {
                            return new F2m(curve, eCFieldElement2, curve.getB().sqrt(), this.withCompression);
                        }
                        eCFieldElement5 = eCFieldElement.multiply(eCFieldElement3.add(eCFieldElement2)).add(eCFieldElement2).add(eCFieldElement4).divide(eCFieldElement2).add(eCFieldElement2);
                        eCFieldElement4 = curve.fromBigInteger(ECConstants.ONE);
                    } else {
                        eCFieldElement2 = add.square();
                        eCFieldElement = multiply.multiply(eCFieldElement);
                        eCFieldElement6 = multiply.multiply(square);
                        eCFieldElement = eCFieldElement.multiply(eCFieldElement6);
                        if (eCFieldElement.isZero()) {
                            return new F2m(curve, eCFieldElement, curve.getB().sqrt(), this.withCompression);
                        }
                        square = multiply.multiply(eCFieldElement2);
                        eCFieldElement4 = !isOne2 ? square.multiply(eCFieldElement4) : square;
                        eCFieldElement3 = eCFieldElement6.add(eCFieldElement2).squarePlusProduct(eCFieldElement4, eCFieldElement3.add(eCFieldElement5));
                        if (!isOne) {
                            eCFieldElement4 = eCFieldElement4.multiply(eCFieldElement5);
                        }
                        eCFieldElement5 = eCFieldElement3;
                        eCFieldElement2 = eCFieldElement;
                    }
                    return new F2m(curve, eCFieldElement2, eCFieldElement5, new ECFieldElement[]{eCFieldElement4}, this.withCompression);
                }
            }
        }

        protected ECPoint detach() {
            return new F2m(null, getAffineXCoord(), getAffineYCoord());
        }

        protected boolean getCompressionYTilde() {
            ECFieldElement rawXCoord = getRawXCoord();
            boolean z = false;
            if (rawXCoord.isZero()) {
                return false;
            }
            ECFieldElement rawYCoord = getRawYCoord();
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    if (rawYCoord.testBitZero() != rawXCoord.testBitZero()) {
                        z = true;
                    }
                    return z;
                default:
                    return rawYCoord.divide(rawXCoord).testBitZero();
            }
        }

        public ECFieldElement getYCoord() {
            int curveCoordinateSystem = getCurveCoordinateSystem();
            switch (curveCoordinateSystem) {
                case 5:
                case 6:
                    ECFieldElement eCFieldElement = this.x;
                    ECFieldElement eCFieldElement2 = this.y;
                    if (isInfinity() || eCFieldElement.isZero()) {
                        return eCFieldElement2;
                    }
                    eCFieldElement = eCFieldElement2.add(eCFieldElement).multiply(eCFieldElement);
                    if (6 == curveCoordinateSystem) {
                        ECFieldElement eCFieldElement3 = this.zs[0];
                        if (!eCFieldElement3.isOne()) {
                            eCFieldElement = eCFieldElement.divide(eCFieldElement3);
                        }
                    }
                    return eCFieldElement;
                default:
                    return this.y;
            }
        }

        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement eCFieldElement = this.x;
            if (eCFieldElement.isZero()) {
                return this;
            }
            ECFieldElement eCFieldElement2;
            ECFieldElement eCFieldElement3;
            switch (getCurveCoordinateSystem()) {
                case 0:
                    return new F2m(this.curve, eCFieldElement, this.y.add(eCFieldElement), this.withCompression);
                case 1:
                    eCFieldElement2 = this.y;
                    eCFieldElement3 = this.zs[0];
                    return new F2m(this.curve, eCFieldElement, eCFieldElement2.add(eCFieldElement), new ECFieldElement[]{eCFieldElement3}, this.withCompression);
                case 5:
                    return new F2m(this.curve, eCFieldElement, this.y.addOne(), this.withCompression);
                case 6:
                    eCFieldElement2 = this.y;
                    eCFieldElement3 = this.zs[0];
                    return new F2m(this.curve, eCFieldElement, eCFieldElement2.add(eCFieldElement3), new ECFieldElement[]{eCFieldElement3}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
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
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement add;
            ECFieldElement add2;
            boolean isOne;
            ECFieldElement multiply;
            if (coordinateSystem != 6) {
                switch (coordinateSystem) {
                    case 0:
                        add = this.y.divide(eCFieldElement).add(eCFieldElement);
                        add2 = add.square().add(add).add(curve.getA());
                        return new F2m(curve, add2, eCFieldElement.squarePlusProduct(add2, add.addOne()), this.withCompression);
                    case 1:
                        add = this.y;
                        add2 = this.zs[0];
                        isOne = add2.isOne();
                        multiply = isOne ? eCFieldElement : eCFieldElement.multiply(add2);
                        if (!isOne) {
                            add = add.multiply(add2);
                        }
                        eCFieldElement = eCFieldElement.square();
                        add = eCFieldElement.add(add);
                        add2 = multiply.square();
                        ECFieldElement add3 = add.add(multiply);
                        add = add3.multiplyPlusProduct(add, add2, curve.getA());
                        return new F2m(curve, multiply.multiply(add), eCFieldElement.square().multiplyPlusProduct(multiply, add, add3), new ECFieldElement[]{multiply.multiply(add2)}, this.withCompression);
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            add = this.y;
            add2 = this.zs[0];
            isOne = add2.isOne();
            multiply = isOne ? add : add.multiply(add2);
            ECFieldElement square = isOne ? add2 : add2.square();
            ECFieldElement a = curve.getA();
            ECFieldElement multiply2 = isOne ? a : a.multiply(square);
            ECFieldElement add4 = add.square().add(multiply).add(multiply2);
            if (add4.isZero()) {
                return new F2m(curve, add4, curve.getB().sqrt(), this.withCompression);
            }
            ECFieldElement square2 = add4.square();
            ECFieldElement multiply3 = isOne ? add4 : add4.multiply(square);
            ECFieldElement b = curve.getB();
            ECCurve eCCurve = curve;
            if (b.bitLength() < (curve.getFieldSize() >> 1)) {
                eCFieldElement = add.add(eCFieldElement).square();
                eCFieldElement = eCFieldElement.add(add4).add(square).multiply(eCFieldElement).add(b.isOne() ? multiply2.add(square).square() : multiply2.squarePlusProduct(b, square.square())).add(square2);
                if (!a.isZero()) {
                    if (!a.isOne()) {
                        eCFieldElement = eCFieldElement.add(a.addOne().multiply(multiply3));
                    }
                    return new F2m(eCCurve, square2, eCFieldElement, new ECFieldElement[]{multiply3}, this.withCompression);
                }
            }
            if (!isOne) {
                eCFieldElement = eCFieldElement.multiply(add2);
            }
            eCFieldElement = eCFieldElement.squarePlusProduct(add4, multiply).add(square2);
            eCFieldElement = eCFieldElement.add(multiply3);
            return new F2m(eCCurve, square2, eCFieldElement, new ECFieldElement[]{multiply3}, this.withCompression);
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
            if (curve.getCoordinateSystem() != 6) {
                return twice().add(eCPoint);
            }
            ECFieldElement eCFieldElement2 = eCPoint.x;
            ECFieldElement eCFieldElement3 = eCPoint.zs[0];
            if (eCFieldElement2.isZero() || !eCFieldElement3.isOne()) {
                return twice().add(eCPoint);
            }
            eCFieldElement3 = this.y;
            ECFieldElement eCFieldElement4 = this.zs[0];
            ECFieldElement eCFieldElement5 = eCPoint.y;
            eCFieldElement = eCFieldElement.square();
            ECFieldElement square = eCFieldElement3.square();
            ECFieldElement square2 = eCFieldElement4.square();
            eCFieldElement3 = curve.getA().multiply(square2).add(square).add(eCFieldElement3.multiply(eCFieldElement4));
            eCFieldElement4 = eCFieldElement5.addOne();
            eCFieldElement = curve.getA().add(eCFieldElement4).multiply(square2).add(square).multiplyPlusProduct(eCFieldElement3, eCFieldElement, square2);
            eCFieldElement2 = eCFieldElement2.multiply(square2);
            eCFieldElement5 = eCFieldElement2.add(eCFieldElement3).square();
            if (eCFieldElement5.isZero()) {
                return eCFieldElement.isZero() ? eCPoint.twice() : curve.getInfinity();
            } else {
                if (eCFieldElement.isZero()) {
                    return new F2m(curve, eCFieldElement, curve.getB().sqrt(), this.withCompression);
                }
                return new F2m(curve, eCFieldElement.square().multiply(eCFieldElement2), eCFieldElement.add(eCFieldElement5).square().multiplyPlusProduct(eCFieldElement3, eCFieldElement4, eCFieldElement.multiply(eCFieldElement5).multiply(square2)), new ECFieldElement[]{eCFieldElement.multiply(eCFieldElement5).multiply(square2)}, this.withCompression);
            }
        }
    }

    public static class Fp extends AbstractFp {
        public Fp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            this(eCCurve, eCFieldElement, eCFieldElement2, false);
        }

        public Fp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
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

        Fp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
            this.withCompression = z;
        }

        /* JADX WARNING: Removed duplicated region for block: B:86:0x0204  */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x01f6  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ECPoint add(ECPoint eCPoint) {
            ECPoint eCPoint2 = eCPoint;
            if (isInfinity()) {
                return eCPoint2;
            }
            if (eCPoint.isInfinity()) {
                return this;
            }
            if (this == eCPoint2) {
                return twice();
            }
            ECFieldElement subtract;
            ECFieldElement subtract2;
            ECFieldElement multiply;
            ECFieldElement subtract3;
            ECFieldElement[] eCFieldElementArr;
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement eCFieldElement2 = this.y;
            ECFieldElement eCFieldElement3 = eCPoint2.x;
            ECFieldElement eCFieldElement4 = eCPoint2.y;
            if (coordinateSystem != 4) {
                switch (coordinateSystem) {
                    case 0:
                        subtract = eCFieldElement3.subtract(eCFieldElement);
                        subtract2 = eCFieldElement4.subtract(eCFieldElement2);
                        if (subtract.isZero()) {
                            return subtract2.isZero() ? twice() : curve.getInfinity();
                        } else {
                            subtract = subtract2.divide(subtract);
                            subtract2 = subtract.square().subtract(eCFieldElement).subtract(eCFieldElement3);
                            return new Fp(curve, subtract2, subtract.multiply(eCFieldElement.subtract(subtract2)).subtract(eCFieldElement2), this.withCompression);
                        }
                    case 1:
                        subtract2 = this.zs[0];
                        subtract = eCPoint2.zs[0];
                        boolean isOne = subtract2.isOne();
                        boolean isOne2 = subtract.isOne();
                        if (!isOne) {
                            eCFieldElement4 = eCFieldElement4.multiply(subtract2);
                        }
                        if (!isOne2) {
                            eCFieldElement2 = eCFieldElement2.multiply(subtract);
                        }
                        eCFieldElement4 = eCFieldElement4.subtract(eCFieldElement2);
                        if (!isOne) {
                            eCFieldElement3 = eCFieldElement3.multiply(subtract2);
                        }
                        if (!isOne2) {
                            eCFieldElement = eCFieldElement.multiply(subtract);
                        }
                        eCFieldElement3 = eCFieldElement3.subtract(eCFieldElement);
                        if (eCFieldElement3.isZero()) {
                            return eCFieldElement4.isZero() ? twice() : curve.getInfinity();
                        } else {
                            if (isOne) {
                                subtract2 = subtract;
                            } else if (!isOne2) {
                                subtract2 = subtract2.multiply(subtract);
                            }
                            subtract = eCFieldElement3.square();
                            ECFieldElement multiply2 = subtract.multiply(eCFieldElement3);
                            subtract = subtract.multiply(eCFieldElement);
                            eCFieldElement = eCFieldElement4.square().multiply(subtract2).subtract(multiply2).subtract(two(subtract));
                            return new Fp(curve, eCFieldElement3.multiply(eCFieldElement), subtract.subtract(eCFieldElement).multiplyMinusProduct(eCFieldElement4, eCFieldElement2, multiply2), new ECFieldElement[]{multiply2.multiply(subtract2)}, this.withCompression);
                        }
                    case 2:
                        break;
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            ECFieldElement eCFieldElement5 = this.zs[0];
            Object obj = eCPoint2.zs[0];
            boolean isOne3 = eCFieldElement5.isOne();
            if (isOne3 || !eCFieldElement5.equals(obj)) {
                ECFieldElement square;
                if (!isOne3) {
                    ECFieldElement square2 = eCFieldElement5.square();
                    eCFieldElement3 = square2.multiply(eCFieldElement3);
                    eCFieldElement4 = square2.multiply(eCFieldElement5).multiply(eCFieldElement4);
                }
                boolean isOne4 = obj.isOne();
                if (!isOne4) {
                    square = obj.square();
                    eCFieldElement = square.multiply(eCFieldElement);
                    eCFieldElement2 = square.multiply(obj).multiply(eCFieldElement2);
                }
                eCFieldElement3 = eCFieldElement.subtract(eCFieldElement3);
                eCFieldElement4 = eCFieldElement2.subtract(eCFieldElement4);
                if (eCFieldElement3.isZero()) {
                    return eCFieldElement4.isZero() ? twice() : curve.getInfinity();
                } else {
                    square = eCFieldElement3.square();
                    multiply = square.multiply(eCFieldElement3);
                    eCFieldElement = square.multiply(eCFieldElement);
                    subtract3 = eCFieldElement4.square().add(multiply).subtract(two(eCFieldElement));
                    eCFieldElement = eCFieldElement.subtract(subtract3).multiplyMinusProduct(eCFieldElement4, multiply, eCFieldElement2);
                    eCFieldElement2 = !isOne3 ? eCFieldElement3.multiply(eCFieldElement5) : eCFieldElement3;
                    subtract = !isOne4 ? eCFieldElement2.multiply(obj) : eCFieldElement2;
                    if (subtract == eCFieldElement3) {
                        multiply = square;
                        if (coordinateSystem != 4) {
                            subtract2 = calculateJacobianModifiedW(subtract, multiply);
                            eCFieldElementArr = new ECFieldElement[]{subtract, subtract2};
                        } else {
                            eCFieldElementArr = new ECFieldElement[]{subtract};
                        }
                        return new Fp(curve, subtract3, eCFieldElement, eCFieldElementArr, this.withCompression);
                    }
                }
            }
            subtract = eCFieldElement.subtract(eCFieldElement3);
            eCFieldElement4 = eCFieldElement2.subtract(eCFieldElement4);
            if (subtract.isZero()) {
                return eCFieldElement4.isZero() ? twice() : curve.getInfinity();
            } else {
                ECFieldElement square3 = subtract.square();
                eCFieldElement = eCFieldElement.multiply(square3);
                eCFieldElement3 = eCFieldElement3.multiply(square3);
                eCFieldElement2 = eCFieldElement.subtract(eCFieldElement3).multiply(eCFieldElement2);
                eCFieldElement3 = eCFieldElement4.square().subtract(eCFieldElement).subtract(eCFieldElement3);
                eCFieldElement = eCFieldElement.subtract(eCFieldElement3).multiply(eCFieldElement4).subtract(eCFieldElement2);
                subtract = subtract.multiply(eCFieldElement5);
                subtract3 = eCFieldElement3;
            }
            multiply = null;
            if (coordinateSystem != 4) {
            }
            return new Fp(curve, subtract3, eCFieldElement, eCFieldElementArr, this.withCompression);
        }

        protected ECFieldElement calculateJacobianModifiedW(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            ECFieldElement a = getCurve().getA();
            if (a.isZero() || eCFieldElement.isOne()) {
                return a;
            }
            if (eCFieldElement2 == null) {
                eCFieldElement2 = eCFieldElement.square();
            }
            eCFieldElement = eCFieldElement2.square();
            eCFieldElement2 = a.negate();
            return eCFieldElement2.bitLength() < a.bitLength() ? eCFieldElement.multiply(eCFieldElement2).negate() : eCFieldElement.multiply(a);
        }

        protected ECPoint detach() {
            return new Fp(null, getAffineXCoord(), getAffineYCoord());
        }

        protected ECFieldElement doubleProductFromSquares(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3, ECFieldElement eCFieldElement4) {
            return eCFieldElement.add(eCFieldElement2).square().subtract(eCFieldElement3).subtract(eCFieldElement4);
        }

        protected ECFieldElement eight(ECFieldElement eCFieldElement) {
            return four(two(eCFieldElement));
        }

        protected ECFieldElement four(ECFieldElement eCFieldElement) {
            return two(two(eCFieldElement));
        }

        protected ECFieldElement getJacobianModifiedW() {
            ECFieldElement eCFieldElement = this.zs[1];
            if (eCFieldElement != null) {
                return eCFieldElement;
            }
            ECFieldElement[] eCFieldElementArr = this.zs;
            ECFieldElement calculateJacobianModifiedW = calculateJacobianModifiedW(this.zs[0], null);
            eCFieldElementArr[1] = calculateJacobianModifiedW;
            return calculateJacobianModifiedW;
        }

        public ECFieldElement getZCoord(int i) {
            return (i == 1 && 4 == getCurveCoordinateSystem()) ? getJacobianModifiedW() : super.getZCoord(i);
        }

        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            return curve.getCoordinateSystem() != 0 ? new Fp(curve, this.x, this.y.negate(), this.zs, this.withCompression) : new Fp(curve, this.x, this.y.negate(), this.withCompression);
        }

        protected ECFieldElement three(ECFieldElement eCFieldElement) {
            return two(eCFieldElement).add(eCFieldElement);
        }

        public ECPoint threeTimes() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement eCFieldElement = this.y;
            if (eCFieldElement.isZero()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem != 0) {
                return coordinateSystem != 4 ? twice().add(this) : twiceJacobianModified(false).add(this);
            } else {
                ECFieldElement eCFieldElement2 = this.x;
                ECFieldElement two = two(eCFieldElement);
                ECFieldElement square = two.square();
                ECFieldElement add = three(eCFieldElement2.square()).add(getCurve().getA());
                ECFieldElement subtract = three(eCFieldElement2).multiply(square).subtract(add.square());
                if (subtract.isZero()) {
                    return getCurve().getInfinity();
                }
                two = subtract.multiply(two).invert();
                add = subtract.multiply(two).multiply(add);
                two = square.square().multiply(two).subtract(add);
                square = two.subtract(add).multiply(add.add(two)).add(eCFieldElement2);
                return new Fp(curve, square, eCFieldElement2.subtract(square).multiply(two).subtract(eCFieldElement), this.withCompression);
            }
        }

        public ECPoint timesPow2(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("'e' cannot be negative");
            } else if (i == 0 || isInfinity()) {
                return this;
            } else {
                if (i == 1) {
                    return twice();
                }
                ECCurve curve = getCurve();
                ECFieldElement eCFieldElement = this.y;
                if (eCFieldElement.isZero()) {
                    return curve.getInfinity();
                }
                int coordinateSystem = curve.getCoordinateSystem();
                ECFieldElement a = curve.getA();
                ECFieldElement eCFieldElement2 = this.x;
                ECFieldElement fromBigInteger = this.zs.length < 1 ? curve.fromBigInteger(ECConstants.ONE) : this.zs[0];
                if (!fromBigInteger.isOne()) {
                    if (coordinateSystem != 4) {
                        switch (coordinateSystem) {
                            case 0:
                                break;
                            case 1:
                                a = fromBigInteger.square();
                                eCFieldElement2 = eCFieldElement2.multiply(fromBigInteger);
                                eCFieldElement = eCFieldElement.multiply(a);
                                break;
                            case 2:
                                a = null;
                                break;
                            default:
                                throw new IllegalStateException("unsupported coordinate system");
                        }
                        a = calculateJacobianModifiedW(fromBigInteger, a);
                    } else {
                        a = getJacobianModifiedW();
                    }
                }
                ECFieldElement eCFieldElement3 = fromBigInteger;
                fromBigInteger = a;
                a = eCFieldElement;
                int i2 = 0;
                while (i2 < i) {
                    if (a.isZero()) {
                        return curve.getInfinity();
                    }
                    ECFieldElement three = three(eCFieldElement2.square());
                    ECFieldElement two = two(a);
                    a = two.multiply(a);
                    eCFieldElement2 = two(eCFieldElement2.multiply(a));
                    a = two(a.square());
                    if (!fromBigInteger.isZero()) {
                        three = three.add(fromBigInteger);
                        fromBigInteger = two(a.multiply(fromBigInteger));
                    }
                    ECFieldElement subtract = three.square().subtract(two(eCFieldElement2));
                    a = three.multiply(eCFieldElement2.subtract(subtract)).subtract(a);
                    eCFieldElement3 = eCFieldElement3.isOne() ? two : two.multiply(eCFieldElement3);
                    i2++;
                    eCFieldElement2 = subtract;
                }
                if (coordinateSystem != 4) {
                    switch (coordinateSystem) {
                        case 0:
                            ECFieldElement invert = eCFieldElement3.invert();
                            ECFieldElement square = invert.square();
                            return new Fp(curve, eCFieldElement2.multiply(square), a.multiply(square.multiply(invert)), this.withCompression);
                        case 1:
                            return new Fp(curve, eCFieldElement2.multiply(eCFieldElement3), a, new ECFieldElement[]{eCFieldElement3.multiply(eCFieldElement3.square())}, this.withCompression);
                        case 2:
                            return new Fp(curve, eCFieldElement2, a, new ECFieldElement[]{eCFieldElement3}, this.withCompression);
                        default:
                            throw new IllegalStateException("unsupported coordinate system");
                    }
                }
                return new Fp(curve, eCFieldElement2, a, new ECFieldElement[]{eCFieldElement3, fromBigInteger}, this.withCompression);
            }
        }

        public ECPoint twice() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElement = this.y;
            if (eCFieldElement.isZero()) {
                return curve.getInfinity();
            }
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement2 = this.x;
            if (coordinateSystem == 4) {
                return twiceJacobianModified(true);
            }
            ECFieldElement divide;
            boolean isOne;
            ECFieldElement a;
            ECFieldElement subtract;
            ECFieldElement two;
            ECFieldElement multiply;
            switch (coordinateSystem) {
                case 0:
                    divide = three(eCFieldElement2.square()).add(getCurve().getA()).divide(two(eCFieldElement));
                    ECFieldElement subtract2 = divide.square().subtract(two(eCFieldElement2));
                    return new Fp(curve, subtract2, divide.multiply(eCFieldElement2.subtract(subtract2)).subtract(eCFieldElement), this.withCompression);
                case 1:
                    divide = this.zs[0];
                    isOne = divide.isOne();
                    a = curve.getA();
                    if (!(a.isZero() || isOne)) {
                        a = a.multiply(divide.square());
                    }
                    a = a.add(three(eCFieldElement2.square()));
                    divide = isOne ? eCFieldElement : eCFieldElement.multiply(divide);
                    eCFieldElement = isOne ? eCFieldElement.square() : divide.multiply(eCFieldElement);
                    eCFieldElement2 = four(eCFieldElement2.multiply(eCFieldElement));
                    subtract = a.square().subtract(two(eCFieldElement2));
                    two = two(divide);
                    multiply = subtract.multiply(two);
                    eCFieldElement = two(eCFieldElement);
                    a = eCFieldElement2.subtract(subtract).multiply(a).subtract(two(eCFieldElement.square()));
                    eCFieldElement = isOne ? two(eCFieldElement) : two.square();
                    return new Fp(curve, multiply, a, new ECFieldElement[]{two(eCFieldElement).multiply(divide)}, this.withCompression);
                case 2:
                    divide = this.zs[0];
                    isOne = divide.isOne();
                    a = eCFieldElement.square();
                    subtract = a.square();
                    two = curve.getA();
                    multiply = two.negate();
                    if (multiply.toBigInteger().equals(BigInteger.valueOf(3))) {
                        two = isOne ? divide : divide.square();
                        two = three(eCFieldElement2.add(two).multiply(eCFieldElement2.subtract(two)));
                        eCFieldElement2 = a.multiply(eCFieldElement2);
                    } else {
                        ECFieldElement three = three(eCFieldElement2.square());
                        if (!isOne) {
                            if (two.isZero()) {
                                two = three;
                            } else {
                                ECFieldElement square = divide.square().square();
                                two = multiply.bitLength() < two.bitLength() ? three.subtract(square.multiply(multiply)) : square.multiply(two);
                            }
                            eCFieldElement2 = eCFieldElement2.multiply(a);
                        }
                        two = three.add(two);
                        eCFieldElement2 = eCFieldElement2.multiply(a);
                    }
                    eCFieldElement2 = four(eCFieldElement2);
                    a = two.square().subtract(two(eCFieldElement2));
                    subtract = eCFieldElement2.subtract(a).multiply(two).subtract(eight(subtract));
                    eCFieldElement = two(eCFieldElement);
                    if (!isOne) {
                        eCFieldElement = eCFieldElement.multiply(divide);
                    }
                    return new Fp(curve, a, subtract, new ECFieldElement[]{eCFieldElement}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        protected Fp twiceJacobianModified(boolean z) {
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement eCFieldElement2 = this.y;
            ECFieldElement eCFieldElement3 = this.zs[0];
            ECFieldElement jacobianModifiedW = getJacobianModifiedW();
            ECFieldElement add = three(eCFieldElement.square()).add(jacobianModifiedW);
            ECFieldElement two = two(eCFieldElement2);
            eCFieldElement2 = two.multiply(eCFieldElement2);
            eCFieldElement = two(eCFieldElement.multiply(eCFieldElement2));
            ECFieldElement subtract = add.square().subtract(two(eCFieldElement));
            eCFieldElement2 = two(eCFieldElement2.square());
            ECFieldElement subtract2 = add.multiply(eCFieldElement.subtract(subtract)).subtract(eCFieldElement2);
            eCFieldElement = z ? two(eCFieldElement2.multiply(jacobianModifiedW)) : null;
            if (!eCFieldElement3.isOne()) {
                two = two.multiply(eCFieldElement3);
            }
            return new Fp(getCurve(), subtract, subtract2, new ECFieldElement[]{two, eCFieldElement}, this.withCompression);
        }

        public ECPoint twicePlus(ECPoint eCPoint) {
            if (this == eCPoint) {
                return threeTimes();
            }
            if (isInfinity()) {
                return eCPoint;
            }
            if (eCPoint.isInfinity()) {
                return twice();
            }
            ECFieldElement eCFieldElement = this.y;
            if (eCFieldElement.isZero()) {
                return eCPoint;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem != 0) {
                return coordinateSystem != 4 ? twice().add(eCPoint) : twiceJacobianModified(false).add(eCPoint);
            } else {
                ECFieldElement eCFieldElement2 = this.x;
                ECFieldElement eCFieldElement3 = eCPoint.x;
                ECFieldElement eCFieldElement4 = eCPoint.y;
                ECFieldElement subtract = eCFieldElement3.subtract(eCFieldElement2);
                eCFieldElement4 = eCFieldElement4.subtract(eCFieldElement);
                if (subtract.isZero()) {
                    return eCFieldElement4.isZero() ? threeTimes() : this;
                } else {
                    ECFieldElement square = subtract.square();
                    ECFieldElement subtract2 = square.multiply(two(eCFieldElement2).add(eCFieldElement3)).subtract(eCFieldElement4.square());
                    if (subtract2.isZero()) {
                        return curve.getInfinity();
                    }
                    ECFieldElement invert = subtract2.multiply(subtract).invert();
                    eCFieldElement4 = subtract2.multiply(invert).multiply(eCFieldElement4);
                    subtract = two(eCFieldElement).multiply(square).multiply(subtract).multiply(invert).subtract(eCFieldElement4);
                    eCFieldElement4 = subtract.subtract(eCFieldElement4).multiply(eCFieldElement4.add(subtract)).add(eCFieldElement3);
                    return new Fp(curve, eCFieldElement4, eCFieldElement2.subtract(eCFieldElement4).multiply(subtract).subtract(eCFieldElement), this.withCompression);
                }
            }
        }

        protected ECFieldElement two(ECFieldElement eCFieldElement) {
            return eCFieldElement.add(eCFieldElement);
        }
    }

    protected ECPoint(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, getInitialZCoords(eCCurve));
    }

    protected ECPoint(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
        this.preCompTable = null;
        this.curve = eCCurve;
        this.x = eCFieldElement;
        this.y = eCFieldElement2;
        this.zs = eCFieldElementArr;
    }

    protected static ECFieldElement[] getInitialZCoords(ECCurve eCCurve) {
        int coordinateSystem = eCCurve == null ? 0 : eCCurve.getCoordinateSystem();
        if (coordinateSystem == 0 || coordinateSystem == 5) {
            return EMPTY_ZS;
        }
        ECFieldElement fromBigInteger = eCCurve.fromBigInteger(ECConstants.ONE);
        if (coordinateSystem != 6) {
            switch (coordinateSystem) {
                case 1:
                case 2:
                    break;
                case 3:
                    return new ECFieldElement[]{fromBigInteger, fromBigInteger, fromBigInteger};
                case 4:
                    return new ECFieldElement[]{fromBigInteger, eCCurve.getA()};
                default:
                    throw new IllegalArgumentException("unknown coordinate system");
            }
        }
        return new ECFieldElement[]{fromBigInteger};
    }

    public abstract ECPoint add(ECPoint eCPoint);

    protected void checkNormalized() {
        if (!isNormalized()) {
            throw new IllegalStateException("point not in normal form");
        }
    }

    protected ECPoint createScaledPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        return getCurve().createRawPoint(getRawXCoord().multiply(eCFieldElement), getRawYCoord().multiply(eCFieldElement2), this.withCompression);
    }

    protected abstract ECPoint detach();

    public boolean equals(Object obj) {
        return obj == this ? true : !(obj instanceof ECPoint) ? false : equals((ECPoint) obj);
    }

    public boolean equals(ECPoint eCPoint) {
        boolean z = false;
        if (eCPoint == null) {
            return false;
        }
        ECCurve curve = getCurve();
        ECCurve curve2 = eCPoint.getCurve();
        int i = curve == null ? 1 : false;
        int i2 = curve2 == null ? 1 : false;
        boolean isInfinity = isInfinity();
        boolean isInfinity2 = eCPoint.isInfinity();
        if (isInfinity || isInfinity2) {
            if (isInfinity && isInfinity2 && !(i == 0 && i2 == 0 && !curve.equals(curve2))) {
                z = true;
            }
            return z;
        }
        ECPoint eCPoint2;
        if (i == 0 || i2 == 0) {
            if (i != 0) {
                eCPoint = eCPoint.normalize();
            } else {
                if (i2 != 0) {
                    eCPoint2 = eCPoint;
                    eCPoint = normalize();
                } else if (!curve.equals(curve2)) {
                    return false;
                } else {
                    ECPoint[] eCPointArr = new ECPoint[]{this, curve.importPoint(eCPoint)};
                    curve.normalizeAll(eCPointArr);
                    eCPoint = eCPointArr[0];
                    eCPoint2 = eCPointArr[1];
                }
                if (eCPoint.getXCoord().equals(eCPoint2.getXCoord()) && eCPoint.getYCoord().equals(eCPoint2.getYCoord())) {
                    z = true;
                }
                return z;
            }
        }
        eCPoint2 = eCPoint;
        eCPoint = this;
        z = true;
        return z;
    }

    public ECFieldElement getAffineXCoord() {
        checkNormalized();
        return getXCoord();
    }

    public ECFieldElement getAffineYCoord() {
        checkNormalized();
        return getYCoord();
    }

    protected abstract boolean getCompressionYTilde();

    public ECCurve getCurve() {
        return this.curve;
    }

    protected int getCurveCoordinateSystem() {
        return this.curve == null ? 0 : this.curve.getCoordinateSystem();
    }

    public final ECPoint getDetachedPoint() {
        return normalize().detach();
    }

    public byte[] getEncoded() {
        return getEncoded(this.withCompression);
    }

    public byte[] getEncoded(boolean z) {
        if (isInfinity()) {
            return new byte[1];
        }
        ECPoint normalize = normalize();
        byte[] encoded = normalize.getXCoord().getEncoded();
        byte[] bArr;
        if (z) {
            bArr = new byte[(encoded.length + 1)];
            bArr[0] = (byte) (normalize.getCompressionYTilde() ? 3 : 2);
            System.arraycopy(encoded, 0, bArr, 1, encoded.length);
            return bArr;
        }
        bArr = normalize.getYCoord().getEncoded();
        byte[] bArr2 = new byte[((encoded.length + bArr.length) + 1)];
        bArr2[0] = (byte) 4;
        System.arraycopy(encoded, 0, bArr2, 1, encoded.length);
        System.arraycopy(bArr, 0, bArr2, encoded.length + 1, bArr.length);
        return bArr2;
    }

    public final ECFieldElement getRawXCoord() {
        return this.x;
    }

    public final ECFieldElement getRawYCoord() {
        return this.y;
    }

    protected final ECFieldElement[] getRawZCoords() {
        return this.zs;
    }

    public ECFieldElement getX() {
        return normalize().getXCoord();
    }

    public ECFieldElement getXCoord() {
        return this.x;
    }

    public ECFieldElement getY() {
        return normalize().getYCoord();
    }

    public ECFieldElement getYCoord() {
        return this.y;
    }

    public ECFieldElement getZCoord(int i) {
        return (i < 0 || i >= this.zs.length) ? null : this.zs[i];
    }

    public ECFieldElement[] getZCoords() {
        int length = this.zs.length;
        if (length == 0) {
            return EMPTY_ZS;
        }
        ECFieldElement[] eCFieldElementArr = new ECFieldElement[length];
        System.arraycopy(this.zs, 0, eCFieldElementArr, 0, length);
        return eCFieldElementArr;
    }

    public int hashCode() {
        ECCurve curve = getCurve();
        int i = curve == null ? 0 : ~curve.hashCode();
        if (isInfinity()) {
            return i;
        }
        ECPoint normalize = normalize();
        return (i ^ (normalize.getXCoord().hashCode() * 17)) ^ (normalize.getYCoord().hashCode() * 257);
    }

    public boolean isCompressed() {
        return this.withCompression;
    }

    public boolean isInfinity() {
        return this.x == null || this.y == null || (this.zs.length > 0 && this.zs[0].isZero());
    }

    public boolean isNormalized() {
        int curveCoordinateSystem = getCurveCoordinateSystem();
        return curveCoordinateSystem == 0 || curveCoordinateSystem == 5 || isInfinity() || this.zs[0].isOne();
    }

    public boolean isValid() {
        return isInfinity() || getCurve() == null || (satisfiesCurveEquation() && satisfiesCofactor());
    }

    public ECPoint multiply(BigInteger bigInteger) {
        return getCurve().getMultiplier().multiply(this, bigInteger);
    }

    public abstract ECPoint negate();

    public ECPoint normalize() {
        if (isInfinity()) {
            return this;
        }
        int curveCoordinateSystem = getCurveCoordinateSystem();
        if (curveCoordinateSystem == 0 || curveCoordinateSystem == 5) {
            return this;
        }
        ECFieldElement zCoord = getZCoord(0);
        return zCoord.isOne() ? this : normalize(zCoord.invert());
    }

    ECPoint normalize(ECFieldElement eCFieldElement) {
        int curveCoordinateSystem = getCurveCoordinateSystem();
        if (curveCoordinateSystem != 6) {
            switch (curveCoordinateSystem) {
                case 1:
                    break;
                case 2:
                case 3:
                case 4:
                    ECFieldElement square = eCFieldElement.square();
                    return createScaledPoint(square, square.multiply(eCFieldElement));
                default:
                    throw new IllegalStateException("not a projective coordinate system");
            }
        }
        return createScaledPoint(eCFieldElement, eCFieldElement);
    }

    protected boolean satisfiesCofactor() {
        BigInteger cofactor = this.curve.getCofactor();
        return cofactor == null || cofactor.equals(ECConstants.ONE) || !ECAlgorithms.referenceMultiply(this, cofactor).isInfinity();
    }

    protected abstract boolean satisfiesCurveEquation();

    public ECPoint scaleX(ECFieldElement eCFieldElement) {
        return isInfinity() ? this : getCurve().createRawPoint(getRawXCoord().multiply(eCFieldElement), getRawYCoord(), getRawZCoords(), this.withCompression);
    }

    public ECPoint scaleY(ECFieldElement eCFieldElement) {
        return isInfinity() ? this : getCurve().createRawPoint(getRawXCoord(), getRawYCoord().multiply(eCFieldElement), getRawZCoords(), this.withCompression);
    }

    public abstract ECPoint subtract(ECPoint eCPoint);

    public ECPoint threeTimes() {
        return twicePlus(this);
    }

    public ECPoint timesPow2(int i) {
        if (i >= 0) {
            ECPoint eCPoint = this;
            while (true) {
                i--;
                if (i < 0) {
                    return eCPoint;
                }
                eCPoint = eCPoint.twice();
            }
        } else {
            throw new IllegalArgumentException("'e' cannot be negative");
        }
    }

    public String toString() {
        if (isInfinity()) {
            return "INF";
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append('(');
        stringBuffer.append(getRawXCoord());
        stringBuffer.append(',');
        stringBuffer.append(getRawYCoord());
        for (Object append : this.zs) {
            stringBuffer.append(',');
            stringBuffer.append(append);
        }
        stringBuffer.append(')');
        return stringBuffer.toString();
    }

    public abstract ECPoint twice();

    public ECPoint twicePlus(ECPoint eCPoint) {
        return twice().add(eCPoint);
    }
}
