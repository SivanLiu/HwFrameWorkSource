package com.android.org.bouncycastle.math.ec;

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
        protected AbstractF2m(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            super(curve, x, y);
        }

        protected AbstractF2m(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            super(curve, x, y, zs);
        }

        protected boolean satisfiesCurveEquation() {
            ECCurve curve = getCurve();
            ECFieldElement X = this.x;
            ECFieldElement A = curve.getA();
            ECFieldElement B = curve.getB();
            int coord = curve.getCoordinateSystem();
            ECFieldElement Z;
            ECFieldElement rhs;
            ECFieldElement L;
            if (coord == 6) {
                Z = this.zs[0];
                boolean ZIsOne = Z.isOne();
                ECFieldElement lhs;
                if (X.isZero()) {
                    lhs = this.y.square();
                    rhs = B;
                    if (!ZIsOne) {
                        rhs = rhs.multiply(Z.square());
                    }
                    return lhs.equals(rhs);
                }
                ECFieldElement rhs2;
                L = this.y;
                lhs = X.square();
                if (ZIsOne) {
                    rhs = L.square().add(L).add(A);
                    rhs2 = lhs.square().add(B);
                } else {
                    rhs = Z.square();
                    rhs2 = rhs.square();
                    ECFieldElement lhs2 = L.add(Z).multiplyPlusProduct(L, A, rhs);
                    rhs2 = lhs.squarePlusProduct(B, rhs2);
                    rhs = lhs2;
                }
                return rhs.multiply(lhs).equals(rhs2);
            }
            ECFieldElement Y = this.y;
            L = Y.add(X).multiply(Y);
            switch (coord) {
                case 0:
                    break;
                case 1:
                    Z = this.zs[0];
                    if (!Z.isOne()) {
                        rhs = Z.multiply(Z.square());
                        L = L.multiply(Z);
                        A = A.multiply(Z);
                        B = B.multiply(rhs);
                        break;
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return L.equals(X.add(A).multiply(X.square()).add(B));
        }

        public ECPoint scaleX(ECFieldElement scale) {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement X;
            ECFieldElement L;
            switch (getCurveCoordinateSystem()) {
                case 5:
                    X = getRawXCoord();
                    L = getRawYCoord();
                    return getCurve().createRawPoint(X, L.add(X).divide(scale).add(X.multiply(scale)), getRawZCoords(), this.withCompression);
                case 6:
                    X = getRawXCoord();
                    L = getRawYCoord();
                    ECFieldElement Z = getRawZCoords()[0];
                    ECFieldElement X2 = X.multiply(scale.square());
                    ECFieldElement L2 = L.add(X).add(X2);
                    ECFieldElement Z2 = Z.multiply(scale);
                    return getCurve().createRawPoint(X2, L2, new ECFieldElement[]{Z2}, this.withCompression);
                default:
                    return super.scaleX(scale);
            }
        }

        public ECPoint scaleY(ECFieldElement scale) {
            if (isInfinity()) {
                return this;
            }
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    ECFieldElement X = getRawXCoord();
                    return getCurve().createRawPoint(X, getRawYCoord().add(X).multiply(scale).add(X), getRawZCoords(), this.withCompression);
                default:
                    return super.scaleY(scale);
            }
        }

        public ECPoint subtract(ECPoint b) {
            if (b.isInfinity()) {
                return this;
            }
            return add(b.negate());
        }

        public AbstractF2m tau() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            switch (coord) {
                case 0:
                case 5:
                    return (AbstractF2m) curve.createRawPoint(X1.square(), this.y.square(), this.withCompression);
                case 1:
                case 6:
                    ECFieldElement Y1 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    return (AbstractF2m) curve.createRawPoint(X1.square(), Y1.square(), new ECFieldElement[]{Z1.square()}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        public AbstractF2m tauPow(int pow) {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            switch (coord) {
                case 0:
                case 5:
                    return (AbstractF2m) curve.createRawPoint(X1.squarePow(pow), this.y.squarePow(pow), this.withCompression);
                case 1:
                case 6:
                    ECFieldElement Y1 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    return (AbstractF2m) curve.createRawPoint(X1.squarePow(pow), Y1.squarePow(pow), new ECFieldElement[]{Z1.squarePow(pow)}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }
    }

    public static abstract class AbstractFp extends ECPoint {
        protected AbstractFp(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            super(curve, x, y);
        }

        protected AbstractFp(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            super(curve, x, y, zs);
        }

        protected boolean getCompressionYTilde() {
            return getAffineYCoord().testBitZero();
        }

        protected boolean satisfiesCurveEquation() {
            ECFieldElement X = this.x;
            ECFieldElement Y = this.y;
            ECFieldElement A = this.curve.getA();
            ECFieldElement B = this.curve.getB();
            ECFieldElement lhs = Y.square();
            ECFieldElement Z;
            ECFieldElement Z2;
            ECFieldElement Z3;
            switch (getCurveCoordinateSystem()) {
                case 0:
                    break;
                case 1:
                    Z = this.zs[0];
                    if (!Z.isOne()) {
                        Z2 = Z.square();
                        Z3 = Z.multiply(Z2);
                        lhs = lhs.multiply(Z);
                        A = A.multiply(Z2);
                        B = B.multiply(Z3);
                        break;
                    }
                    break;
                case 2:
                case 3:
                case 4:
                    Z = this.zs[0];
                    if (!Z.isOne()) {
                        Z2 = Z.square();
                        Z3 = Z2.square();
                        ECFieldElement Z6 = Z2.multiply(Z3);
                        A = A.multiply(Z3);
                        B = B.multiply(Z6);
                        break;
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return lhs.equals(X.square().add(A).multiply(X).add(B));
        }

        public ECPoint subtract(ECPoint b) {
            if (b.isInfinity()) {
                return this;
            }
            return add(b.negate());
        }
    }

    public static class F2m extends AbstractF2m {
        public F2m(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            this(curve, x, y, false);
        }

        public F2m(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
            super(curve, x, y);
            Object obj = null;
            Object obj2 = x == null ? 1 : null;
            if (y == null) {
                obj = 1;
            }
            if (obj2 == obj) {
                if (x != null) {
                    com.android.org.bouncycastle.math.ec.ECFieldElement.F2m.checkFieldElements(this.x, this.y);
                    if (curve != null) {
                        com.android.org.bouncycastle.math.ec.ECFieldElement.F2m.checkFieldElements(this.x, this.curve.getA());
                    }
                }
                this.withCompression = withCompression;
                return;
            }
            throw new IllegalArgumentException("Exactly one of the field elements is null");
        }

        F2m(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
            super(curve, x, y, zs);
            this.withCompression = withCompression;
        }

        protected ECPoint detach() {
            return new F2m(null, getAffineXCoord(), getAffineYCoord());
        }

        public ECFieldElement getYCoord() {
            int coord = getCurveCoordinateSystem();
            switch (coord) {
                case 5:
                case 6:
                    ECFieldElement X = this.x;
                    ECFieldElement L = this.y;
                    if (isInfinity() || X.isZero()) {
                        return L;
                    }
                    ECFieldElement Y = L.add(X).multiply(X);
                    if (6 == coord) {
                        ECFieldElement Z = this.zs[0];
                        if (!Z.isOne()) {
                            Y = Y.divide(Z);
                        }
                    }
                    return Y;
                default:
                    return this.y;
            }
        }

        protected boolean getCompressionYTilde() {
            ECFieldElement X = getRawXCoord();
            boolean z = false;
            if (X.isZero()) {
                return false;
            }
            ECFieldElement Y = getRawYCoord();
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    if (Y.testBitZero() != X.testBitZero()) {
                        z = true;
                    }
                    return z;
                default:
                    return Y.divide(X).testBitZero();
            }
        }

        public ECPoint add(ECPoint b) {
            ECPoint eCPoint = b;
            if (isInfinity()) {
                return eCPoint;
            }
            if (b.isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            ECFieldElement X2 = eCPoint.x;
            ECFieldElement Y1;
            ECFieldElement Y2;
            ECFieldElement dx;
            ECFieldElement S1;
            ECFieldElement L;
            ECFieldElement S2;
            ECFieldElement Y12;
            ECFieldElement Z1;
            ECFieldElement Y22;
            ECFieldElement Z2;
            boolean Z2IsOne;
            if (coord != 6) {
                switch (coord) {
                    case 0:
                        ECFieldElement X22 = X2;
                        Y1 = this.y;
                        Y2 = eCPoint.y;
                        dx = X1.add(X22);
                        S1 = Y1.add(Y2);
                        if (!dx.isZero()) {
                            L = S1.divide(dx);
                            S2 = L.square().add(L).add(dx).add(curve.getA());
                            return new F2m(curve, S2, L.multiply(X1.add(S2)).add(S2).add(Y1), this.withCompression);
                        } else if (S1.isZero()) {
                            return twice();
                        } else {
                            return curve.getInfinity();
                        }
                    case 1:
                        Y12 = this.y;
                        Z1 = this.zs[0];
                        Y22 = eCPoint.y;
                        Z2 = eCPoint.zs[0];
                        Z2IsOne = Z2.isOne();
                        S2 = Z1.multiply(Y22);
                        L = Z2IsOne ? Y12 : Y12.multiply(Z2);
                        S1 = S2.add(L);
                        Y1 = Z1.multiply(X2);
                        dx = Z2IsOne ? X1 : X1.multiply(Z2);
                        Y2 = Y1.add(dx);
                        if (!Y2.isZero()) {
                            coord = Y2.square();
                            Y22 = coord.multiply(Y2);
                            ECFieldElement W = Z2IsOne ? Z1 : Z1.multiply(Z2);
                            Z1 = S1.add(Y2);
                            ECFieldElement V1 = Y1;
                            Y1 = Z1.multiplyPlusProduct(S1, coord, curve.getA()).multiply(W).add(Y22);
                            ECFieldElement VSq = coord;
                            return new F2m(curve, Y2.multiply(Y1), S1.multiplyPlusProduct(X1, Y2, Y12).multiplyPlusProduct(Z2IsOne ? coord : coord.multiply(Z2), Z1, Y1), new ECFieldElement[]{Y22.multiply(X2)}, this.withCompression);
                        } else if (S1.isZero()) {
                            return twice();
                        } else {
                            return curve.getInfinity();
                        }
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            ECFieldElement X23 = X2;
            if (!X1.isZero()) {
                X2 = this.y;
                Y12 = this.zs[0];
                Z1 = eCPoint.y;
                Y22 = eCPoint.zs[0];
                boolean Z1IsOne = Y12.isOne();
                Y1 = X23;
                Y2 = Z1;
                if (!Z1IsOne) {
                    Y1 = Y1.multiply(Y12);
                    Y2 = Y2.multiply(Y12);
                }
                L = Y1;
                S2 = Y2;
                Z2IsOne = Y22.isOne();
                Y1 = X1;
                Y2 = X2;
                if (!Z2IsOne) {
                    Y1 = Y1.multiply(Y22);
                    Y2 = Y2.multiply(Y22);
                }
                dx = Y1;
                S1 = Y2;
                Y2 = S1.add(S2);
                Y1 = dx.add(L);
                if (!Y1.isZero()) {
                    ECFieldElement Y23;
                    ECFieldElement S12;
                    ECFieldElement S22;
                    boolean z;
                    if (X23.isZero()) {
                        eCPoint = normalize();
                        X1 = eCPoint.getXCoord();
                        S12 = S1;
                        S1 = eCPoint.getYCoord();
                        S22 = S2;
                        Y23 = Z1;
                        S2 = S1.add(Y23).divide(X1);
                        Y23 = S2.square().add(S2).add(X1).add(curve.getA());
                        if (Y23.isZero()) {
                            return new F2m(curve, Y23, curve.getB().sqrt(), this.withCompression);
                        }
                        z = Z1IsOne;
                        Z1 = S2.multiply(X1.add(Y23)).add(Y23).add(S1).divide(Y23).add(Y23);
                        X23 = Y1;
                        Z2 = curve.fromBigInteger(ECConstants.ONE);
                    } else {
                        S12 = S1;
                        S22 = S2;
                        ECFieldElement eCFieldElement = X23;
                        ECFieldElement eCFieldElement2 = Z1;
                        z = Z1IsOne;
                        Y1 = Y1.square();
                        Y23 = Y2.multiply(dx);
                        S1 = Y2.multiply(L);
                        S2 = Y23.multiply(S1);
                        if (S2.isZero()) {
                            return new F2m(curve, S2, curve.getB().sqrt(), this.withCompression);
                        }
                        X23 = Y2.multiply(Y1);
                        if (!Z2IsOne) {
                            X23 = X23.multiply(Y22);
                        }
                        Z1 = S1.add(Y1).squarePlusProduct(X23, X2.add(Y12));
                        Z2 = X23;
                        if (z) {
                        } else {
                            X23 = Y1;
                            Z2 = Z2.multiply(Y12);
                        }
                        Y23 = S2;
                    }
                    return new F2m(curve, Y23, Z1, new ECFieldElement[]{Z2}, this.withCompression);
                } else if (Y2.isZero()) {
                    return twice();
                } else {
                    return curve.getInfinity();
                }
            } else if (X23.isZero()) {
                return curve.getInfinity();
            } else {
                return eCPoint.add(this);
            }
        }

        public ECPoint twice() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement X1 = this.x;
            if (X1.isZero()) {
                return curve.getInfinity();
            }
            int coord = curve.getCoordinateSystem();
            ECFieldElement L1;
            ECFieldElement X3;
            ECFieldElement Y1;
            ECFieldElement Z1;
            boolean Z1IsOne;
            ECFieldElement X1Z1;
            ECFieldElement Y1Z1;
            ECFieldElement X1Sq;
            ECFieldElement S;
            ECFieldElement V;
            ECFieldElement vSquared;
            ECFieldElement sv;
            if (coord != 6) {
                switch (coord) {
                    case 0:
                        L1 = this.y.divide(X1).add(X1);
                        X3 = L1.square().add(L1).add(curve.getA());
                        return new F2m(curve, X3, X1.squarePlusProduct(X3, L1.addOne()), this.withCompression);
                    case 1:
                        Y1 = this.y;
                        Z1 = this.zs[0];
                        Z1IsOne = Z1.isOne();
                        X1Z1 = Z1IsOne ? X1 : X1.multiply(Z1);
                        Y1Z1 = Z1IsOne ? Y1 : Y1.multiply(Z1);
                        X1Sq = X1.square();
                        S = X1Sq.add(Y1Z1);
                        V = X1Z1;
                        vSquared = V.square();
                        sv = S.add(V);
                        X3 = sv.multiplyPlusProduct(S, vSquared, curve.getA());
                        return new F2m(curve, V.multiply(X3), X1Sq.square().multiplyPlusProduct(V, X3, sv), new ECFieldElement[]{V.multiply(vSquared)}, this.withCompression);
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            Y1 = this.y;
            Z1 = this.zs[0];
            Z1IsOne = Z1.isOne();
            X1Z1 = Z1IsOne ? Y1 : Y1.multiply(Z1);
            Y1Z1 = Z1IsOne ? Z1 : Z1.square();
            X1Sq = curve.getA();
            S = Z1IsOne ? X1Sq : X1Sq.multiply(Y1Z1);
            V = Y1.square().add(X1Z1).add(S);
            if (V.isZero()) {
                return new F2m(curve, V, curve.getB().sqrt(), this.withCompression);
            }
            X3 = V.square();
            L1 = Z1IsOne ? V : V.multiply(Y1Z1);
            sv = curve.getB();
            ECFieldElement b;
            if (sv.bitLength() < (curve.getFieldSize() >> 1)) {
                ECFieldElement t2;
                vSquared = Y1.add(X1).square();
                if (sv.isOne()) {
                    t2 = S.add(Y1Z1).square();
                } else {
                    t2 = S.squarePlusProduct(sv, Y1Z1.square());
                }
                b = sv;
                sv = vSquared.add(V).add(Y1Z1).multiply(vSquared).add(t2).add(X3);
                if (X1Sq.isZero()) {
                    sv = sv.add(L1);
                } else if (!X1Sq.isOne()) {
                    sv = sv.add(X1Sq.addOne().multiply(L1));
                }
            } else {
                b = sv;
                sv = (Z1IsOne ? X1 : X1.multiply(Z1)).squarePlusProduct(V, X1Z1).add(X3).add(L1);
            }
            ECFieldElement X32 = X3;
            return new F2m(curve, X3, sv, new ECFieldElement[]{L1}, this.withCompression);
        }

        public ECPoint twicePlus(ECPoint b) {
            ECPoint eCPoint = b;
            if (isInfinity()) {
                return eCPoint;
            }
            if (b.isInfinity()) {
                return twice();
            }
            ECCurve curve = getCurve();
            ECFieldElement X1 = this.x;
            if (X1.isZero()) {
                return eCPoint;
            }
            int coord = curve.getCoordinateSystem();
            if (coord != 6) {
                return twice().add(eCPoint);
            }
            ECFieldElement X2 = eCPoint.x;
            ECFieldElement Z2 = eCPoint.zs[0];
            int i;
            ECFieldElement eCFieldElement;
            if (X2.isZero()) {
                i = coord;
                eCFieldElement = X2;
            } else if (Z2.isOne()) {
                ECFieldElement L1 = this.y;
                ECFieldElement Z1 = this.zs[0];
                ECFieldElement L2 = eCPoint.y;
                ECFieldElement X1Sq = X1.square();
                ECFieldElement L1Sq = L1.square();
                ECFieldElement Z1Sq = Z1.square();
                ECFieldElement L1Z1 = L1.multiply(Z1);
                ECFieldElement T = curve.getA().multiply(Z1Sq).add(L1Sq).add(L1Z1);
                ECFieldElement L2plus1 = L2.addOne();
                L1Z1 = curve.getA().add(L2plus1).multiply(Z1Sq).add(L1Sq).multiplyPlusProduct(T, X1Sq, Z1Sq);
                X1 = X2.multiply(Z1Sq);
                L1Sq = X1.add(T).square();
                if (L1Sq.isZero()) {
                    if (L1Z1.isZero()) {
                        return b.twice();
                    }
                    return curve.getInfinity();
                } else if (L1Z1.isZero()) {
                    return new F2m(curve, L1Z1, curve.getB().sqrt(), this.withCompression);
                } else {
                    i = coord;
                    eCFieldElement = X2;
                    ECFieldElement X2Z1Sq = X1;
                    return new F2m(curve, L1Z1.square().multiply(X1), L1Z1.add(L1Sq).square().multiplyPlusProduct(T, L2plus1, L1Z1.multiply(L1Sq).multiply(Z1Sq)), new ECFieldElement[]{X2}, this.withCompression);
                }
            } else {
                ECFieldElement eCFieldElement2 = X1;
                i = coord;
                eCFieldElement = X2;
            }
            return twice().add(eCPoint);
        }

        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement X = this.x;
            if (X.isZero()) {
                return this;
            }
            ECFieldElement Y;
            ECFieldElement Z;
            switch (getCurveCoordinateSystem()) {
                case 0:
                    return new F2m(this.curve, X, this.y.add(X), this.withCompression);
                case 1:
                    Y = this.y;
                    Z = this.zs[0];
                    return new F2m(this.curve, X, Y.add(X), new ECFieldElement[]{Z}, this.withCompression);
                case 5:
                    return new F2m(this.curve, X, this.y.addOne(), this.withCompression);
                case 6:
                    Y = this.y;
                    Z = this.zs[0];
                    return new F2m(this.curve, X, Y.add(Z), new ECFieldElement[]{Z}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }
    }

    public static class Fp extends AbstractFp {
        public Fp(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            this(curve, x, y, false);
        }

        public Fp(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
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

        Fp(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
            super(curve, x, y, zs);
            this.withCompression = withCompression;
        }

        protected ECPoint detach() {
            return new Fp(null, getAffineXCoord(), getAffineYCoord());
        }

        public ECFieldElement getZCoord(int index) {
            if (index == 1 && 4 == getCurveCoordinateSystem()) {
                return getJacobianModifiedW();
            }
            return super.getZCoord(index);
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
            ECFieldElement dx;
            ECFieldElement dy;
            ECFieldElement gamma;
            ECFieldElement X3;
            ECFieldElement Z1;
            ECFieldElement u1;
            ECFieldElement u2;
            int coord;
            ECFieldElement Z1Squared;
            ECFieldElement Y3;
            ECFieldElement[] zs;
            ECCurve curve = getCurve();
            int coord2 = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            ECFieldElement Y1 = this.y;
            ECFieldElement X2 = eCPoint.x;
            ECFieldElement Y2 = eCPoint.y;
            if (coord2 != 4) {
                switch (coord2) {
                    case 0:
                        dx = X2.subtract(X1);
                        dy = Y2.subtract(Y1);
                        if (!dx.isZero()) {
                            gamma = dy.divide(dx);
                            X3 = gamma.square().subtract(X1).subtract(X2);
                            return new Fp(curve, X3, gamma.multiply(X1.subtract(X3)).subtract(Y1), this.withCompression);
                        } else if (dy.isZero()) {
                            return twice();
                        } else {
                            return curve.getInfinity();
                        }
                    case 1:
                        Z1 = this.zs[0];
                        ECFieldElement Z2 = eCPoint.zs[0];
                        boolean Z1IsOne = Z1.isOne();
                        boolean Z2IsOne = Z2.isOne();
                        u1 = Z1IsOne ? Y2 : Y2.multiply(Z1);
                        u2 = Z2IsOne ? Y1 : Y1.multiply(Z2);
                        X3 = u1.subtract(u2);
                        dx = Z1IsOne ? X2 : X2.multiply(Z1);
                        gamma = Z2IsOne ? X1 : X1.multiply(Z2);
                        dy = dx.subtract(gamma);
                        if (!dy.isZero()) {
                            ECFieldElement multiply = Z1IsOne ? Z2 : Z2IsOne ? Z1 : Z1.multiply(Z2);
                            ECFieldElement w = multiply;
                            Z1 = dy.square();
                            Z2 = Z1.multiply(dy);
                            coord2 = Z1.multiply(gamma);
                            ECFieldElement v1 = dx;
                            gamma = X3.square().multiply(w).subtract(Z2).subtract(two(coord2));
                            return new Fp(curve, dy.multiply(gamma), coord2.subtract(gamma).multiplyMinusProduct(X3, u2, Z2), new ECFieldElement[]{Z2.multiply(Z1)}, this.withCompression);
                        } else if (X3.isZero()) {
                            return twice();
                        } else {
                            return curve.getInfinity();
                        }
                    case 2:
                        coord = coord2;
                        break;
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            coord = coord2;
            ECFieldElement Z12 = this.zs[0];
            Z1 = eCPoint.zs[0];
            boolean Z1IsOne2 = Z12.isOne();
            ECFieldElement Z3Squared;
            if (Z1IsOne2 || !Z12.equals(Z1)) {
                Z3Squared = null;
                if (Z1IsOne2) {
                    Z1Squared = Z12;
                    dx = X2;
                    dy = Y2;
                } else {
                    Z1Squared = Z12.square();
                    dx = Z1Squared.multiply(X2);
                    dy = Z1Squared.multiply(Z12).multiply(Y2);
                }
                boolean Z2IsOne2 = Z1.isOne();
                if (Z2IsOne2) {
                    X3 = Z1;
                    u2 = X1;
                    u1 = Y1;
                } else {
                    X3 = Z1.square();
                    u2 = X3.multiply(X1);
                    u1 = X3.multiply(Z1).multiply(Y1);
                }
                Z1Squared = u2.subtract(dx);
                dx = u1.subtract(dy);
                if (!Z1Squared.isZero()) {
                    dy = Z1Squared.square();
                    X3 = dy.multiply(Z1Squared);
                    X1 = dy.multiply(u2);
                    u2 = dx.square().add(X3).subtract(two(X1));
                    Y1 = X1.subtract(u2).multiplyMinusProduct(dx, X3, u1);
                    ECFieldElement Z3 = Z1Squared;
                    if (Z1IsOne2) {
                        dx = Z3;
                    } else {
                        dx = Z3.multiply(Z12);
                    }
                    if (!Z2IsOne2) {
                        dx = dx.multiply(Z1);
                    }
                    if (dx == Z1Squared) {
                        Z1Squared = dx;
                        X1 = dy;
                        Y3 = Y1;
                    } else {
                        Z1Squared = dx;
                        Y3 = Y1;
                        X1 = Z3Squared;
                    }
                    Y1 = u2;
                } else if (dx.isZero()) {
                    return twice();
                } else {
                    return curve.getInfinity();
                }
            }
            gamma = X1.subtract(X2);
            X3 = Y1.subtract(Y2);
            if (!gamma.isZero()) {
                u2 = gamma.square();
                u1 = X1.multiply(u2);
                dx = X2.multiply(u2);
                Z1Squared = u1.subtract(dx).multiply(Y1);
                Z3Squared = null;
                dy = X3.square().subtract(u1).subtract(dx);
                dx = u1.subtract(dy).multiply(X3).subtract(Z1Squared);
                ECFieldElement eCFieldElement = Z1Squared;
                Z1Squared = gamma.multiply(Z12);
                Y3 = dx;
                ECFieldElement eCFieldElement2 = X1;
                ECFieldElement eCFieldElement3 = Y1;
                X1 = Z3Squared;
                Y1 = dy;
            } else if (X3.isZero()) {
                return twice();
            } else {
                return curve.getInfinity();
            }
            int coord3 = coord;
            if (coord3 == 4) {
                dx = calculateJacobianModifiedW(Z1Squared, X1);
                zs = new ECFieldElement[]{Z1Squared, dx};
            } else {
                zs = new ECFieldElement[]{Z1Squared};
            }
            return new Fp(curve, Y1, Y3, zs, this.withCompression);
        }

        public ECPoint twice() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return curve.getInfinity();
            }
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            if (coord == 4) {
                return twiceJacobianModified(true);
            }
            ECFieldElement gamma;
            ECFieldElement X3;
            ECFieldElement Z1;
            boolean Z1IsOne;
            ECFieldElement w;
            ECFieldElement w2;
            ECFieldElement s;
            ECFieldElement t;
            ECFieldElement B;
            ECFieldElement _4B;
            ECFieldElement h;
            ECFieldElement X32;
            switch (coord) {
                case 0:
                    gamma = three(X1.square()).add(getCurve().getA()).divide(two(Y1));
                    X3 = gamma.square().subtract(two(X1));
                    return new Fp(curve, X3, gamma.multiply(X1.subtract(X3)).subtract(Y1), this.withCompression);
                case 1:
                    Z1 = this.zs[0];
                    Z1IsOne = Z1.isOne();
                    w = curve.getA();
                    if (!(w.isZero() || Z1IsOne)) {
                        w = w.multiply(Z1.square());
                    }
                    w2 = w.add(three(X1.square()));
                    s = Z1IsOne ? Y1 : Y1.multiply(Z1);
                    t = Z1IsOne ? Y1.square() : s.multiply(Y1);
                    B = X1.multiply(t);
                    _4B = four(B);
                    h = w2.square().subtract(two(_4B));
                    X3 = two(s);
                    X32 = h.multiply(X3);
                    gamma = two(t);
                    ECFieldElement h2 = h;
                    ECFieldElement Y3 = _4B.subtract(h).multiply(w2).subtract(two(gamma.square()));
                    h = Z1IsOne ? two(gamma) : X3.square();
                    ECFieldElement Z3 = two(h).multiply(s);
                    return new Fp(curve, X32, Y3, new ECFieldElement[]{Z3}, this.withCompression);
                case 2:
                    Z1 = this.zs[0];
                    Z1IsOne = Z1.isOne();
                    w2 = Y1.square();
                    s = w2.square();
                    t = curve.getA();
                    B = t.negate();
                    if (B.toBigInteger().equals(BigInteger.valueOf(3))) {
                        X3 = Z1IsOne ? Z1 : Z1.square();
                        h = three(X1.add(X3).multiply(X1.subtract(X3)));
                        X3 = four(w2.multiply(X1));
                    } else {
                        X3 = X1.square();
                        h = three(X3);
                        if (Z1IsOne) {
                            h = h.add(t);
                            X32 = X3;
                        } else if (t.isZero()) {
                            X32 = X3;
                        } else {
                            w = Z1.square().square();
                            X32 = X3;
                            if (B.bitLength() < t.bitLength()) {
                                h = h.subtract(w.multiply(B));
                            } else {
                                h = h.add(w.multiply(t));
                            }
                        }
                        X3 = four(X1.multiply(w2));
                    }
                    _4B = h;
                    h = X3;
                    X3 = _4B.square().subtract(two(h));
                    X32 = h.subtract(X3).multiply(_4B).subtract(eight(s));
                    w = two(Y1);
                    if (!Z1IsOne) {
                        w = w.multiply(Z1);
                    }
                    ECFieldElement X33 = X3;
                    return new Fp(curve, X3, X32, new ECFieldElement[]{w}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        public ECPoint twicePlus(ECPoint b) {
            ECPoint eCPoint = b;
            if (this == eCPoint) {
                return threeTimes();
            }
            if (isInfinity()) {
                return eCPoint;
            }
            if (b.isInfinity()) {
                return twice();
            }
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return eCPoint;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            if (coord == 0) {
                ECFieldElement X1 = this.x;
                ECFieldElement X2 = eCPoint.x;
                ECFieldElement Y2 = eCPoint.y;
                ECFieldElement dx = X2.subtract(X1);
                ECFieldElement dy = Y2.subtract(Y1);
                if (!dx.isZero()) {
                    ECFieldElement X = dx.square();
                    ECFieldElement d = X.multiply(two(X1).add(X2)).subtract(dy.square());
                    if (d.isZero()) {
                        return curve.getInfinity();
                    }
                    ECFieldElement I = d.multiply(dx).invert();
                    ECFieldElement L1 = d.multiply(I).multiply(dy);
                    ECFieldElement L2 = two(Y1).multiply(X).multiply(dx).multiply(I).subtract(L1);
                    ECFieldElement X4 = L2.subtract(L1).multiply(L1.add(L2)).add(X2);
                    return new Fp(curve, X4, X1.subtract(X4).multiply(L2).subtract(Y1), this.withCompression);
                } else if (dy.isZero()) {
                    return threeTimes();
                } else {
                    return this;
                }
            } else if (coord != 4) {
                return twice().add(eCPoint);
            } else {
                return twiceJacobianModified(false).add(eCPoint);
            }
        }

        public ECPoint threeTimes() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            if (coord == 0) {
                ECFieldElement X1 = this.x;
                ECFieldElement _2Y1 = two(Y1);
                ECFieldElement X = _2Y1.square();
                ECFieldElement Z = three(X1.square()).add(getCurve().getA());
                ECFieldElement d = three(X1).multiply(X).subtract(Z.square());
                if (d.isZero()) {
                    return getCurve().getInfinity();
                }
                ECFieldElement I = d.multiply(_2Y1).invert();
                ECFieldElement L1 = d.multiply(I).multiply(Z);
                ECFieldElement L2 = X.square().multiply(I).subtract(L1);
                ECFieldElement X4 = L2.subtract(L1).multiply(L1.add(L2)).add(X1);
                return new Fp(curve, X4, X1.subtract(X4).multiply(L2).subtract(Y1), this.withCompression);
            } else if (coord != 4) {
                return twice().add(this);
            } else {
                return twiceJacobianModified(false).add(this);
            }
        }

        public ECPoint timesPow2(int e) {
            int i = e;
            if (i < 0) {
                throw new IllegalArgumentException("'e' cannot be negative");
            } else if (i == 0 || isInfinity()) {
                return this;
            } else {
                if (i == 1) {
                    return twice();
                }
                ECCurve curve = getCurve();
                ECFieldElement Y1 = this.y;
                if (Y1.isZero()) {
                    return curve.getInfinity();
                }
                ECFieldElement Z1Sq;
                ECFieldElement _4T;
                int coord = curve.getCoordinateSystem();
                ECFieldElement W1 = curve.getA();
                ECFieldElement X1 = this.x;
                ECFieldElement Z1 = this.zs.length < 1 ? curve.fromBigInteger(ECConstants.ONE) : this.zs[0];
                if (!Z1.isOne()) {
                    if (coord != 4) {
                        switch (coord) {
                            case 0:
                                break;
                            case 1:
                                Z1Sq = Z1.square();
                                X1 = X1.multiply(Z1);
                                Y1 = Y1.multiply(Z1Sq);
                                W1 = calculateJacobianModifiedW(Z1, Z1Sq);
                                break;
                            case 2:
                                W1 = calculateJacobianModifiedW(Z1, null);
                                break;
                            default:
                                throw new IllegalStateException("unsupported coordinate system");
                        }
                    }
                    W1 = getJacobianModifiedW();
                }
                ECFieldElement Y12 = Y1;
                ECFieldElement W12 = W1;
                ECFieldElement X12 = X1;
                ECFieldElement Z12 = Z1;
                int i2 = 0;
                while (i2 < i) {
                    if (Y12.isZero()) {
                        return curve.getInfinity();
                    }
                    X1 = three(X12.square());
                    Z1 = two(Y12);
                    Z1Sq = Z1.multiply(Y12);
                    ECFieldElement S = two(X12.multiply(Z1Sq));
                    _4T = Z1Sq.square();
                    ECFieldElement _8T = two(_4T);
                    if (!W12.isZero()) {
                        X1 = X1.add(W12);
                        W12 = two(_8T.multiply(W12));
                    }
                    X12 = X1.square().subtract(two(S));
                    Y12 = X1.multiply(S.subtract(X12)).subtract(_8T);
                    Z12 = Z12.isOne() ? Z1 : Z1.multiply(Z12);
                    i2++;
                    i = e;
                }
                if (coord != 4) {
                    switch (coord) {
                        case 0:
                            ECFieldElement zInv = Z12.invert();
                            _4T = zInv.square();
                            return new Fp(curve, X12.multiply(_4T), Y12.multiply(_4T.multiply(zInv)), this.withCompression);
                        case 1:
                            return new Fp(curve, X12.multiply(Z12), Y12, new ECFieldElement[]{Z12.multiply(Z12.square())}, this.withCompression);
                        case 2:
                            return new Fp(curve, X12, Y12, new ECFieldElement[]{Z12}, this.withCompression);
                        default:
                            throw new IllegalStateException("unsupported coordinate system");
                    }
                }
                return new Fp(curve, X12, Y12, new ECFieldElement[]{Z12, W12}, this.withCompression);
            }
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
            ECCurve curve = getCurve();
            if (curve.getCoordinateSystem() == 0) {
                return new Fp(curve, this.x, this.y.negate(), this.withCompression);
            }
            return new Fp(curve, this.x, this.y.negate(), this.zs, this.withCompression);
        }

        protected ECFieldElement calculateJacobianModifiedW(ECFieldElement Z, ECFieldElement ZSquared) {
            ECFieldElement a4 = getCurve().getA();
            if (a4.isZero() || Z.isOne()) {
                return a4;
            }
            if (ZSquared == null) {
                ZSquared = Z.square();
            }
            ECFieldElement W = ZSquared.square();
            ECFieldElement a4Neg = a4.negate();
            if (a4Neg.bitLength() < a4.bitLength()) {
                W = W.multiply(a4Neg).negate();
            } else {
                W = W.multiply(a4);
            }
            return W;
        }

        protected ECFieldElement getJacobianModifiedW() {
            ECFieldElement W = this.zs[1];
            if (W != null) {
                return W;
            }
            ECFieldElement[] eCFieldElementArr = this.zs;
            ECFieldElement calculateJacobianModifiedW = calculateJacobianModifiedW(this.zs[0], null);
            W = calculateJacobianModifiedW;
            eCFieldElementArr[1] = calculateJacobianModifiedW;
            return W;
        }

        protected Fp twiceJacobianModified(boolean calculateW) {
            ECFieldElement X1 = this.x;
            ECFieldElement Y1 = this.y;
            ECFieldElement Z1 = this.zs[0];
            ECFieldElement W1 = getJacobianModifiedW();
            ECFieldElement M = three(X1.square()).add(W1);
            ECFieldElement _2Y1 = two(Y1);
            ECFieldElement _2Y1Squared = _2Y1.multiply(Y1);
            ECFieldElement S = two(X1.multiply(_2Y1Squared));
            ECFieldElement X3 = M.square().subtract(two(S));
            ECFieldElement _8T = two(_2Y1Squared.square());
            ECFieldElement Y3 = M.multiply(S.subtract(X3)).subtract(_8T);
            ECFieldElement W3 = calculateW ? two(_8T.multiply(W1)) : null;
            ECFieldElement Z3 = Z1.isOne() ? _2Y1 : _2Y1.multiply(Z1);
            return new Fp(getCurve(), X3, Y3, new ECFieldElement[]{Z3, W3}, this.withCompression);
        }
    }

    public abstract ECPoint add(ECPoint eCPoint);

    protected abstract ECPoint detach();

    protected abstract boolean getCompressionYTilde();

    public abstract ECPoint negate();

    protected abstract boolean satisfiesCurveEquation();

    public abstract ECPoint subtract(ECPoint eCPoint);

    public abstract ECPoint twice();

    protected static ECFieldElement[] getInitialZCoords(ECCurve curve) {
        int coord = curve == null ? 0 : curve.getCoordinateSystem();
        if (coord == 0 || coord == 5) {
            return EMPTY_ZS;
        }
        ECFieldElement one = curve.fromBigInteger(ECConstants.ONE);
        if (coord != 6) {
            switch (coord) {
                case 1:
                case 2:
                    break;
                case 3:
                    return new ECFieldElement[]{one, one, one};
                case 4:
                    return new ECFieldElement[]{one, curve.getA()};
                default:
                    throw new IllegalArgumentException("unknown coordinate system");
            }
        }
        return new ECFieldElement[]{one};
    }

    protected ECPoint(ECCurve curve, ECFieldElement x, ECFieldElement y) {
        this(curve, x, y, getInitialZCoords(curve));
    }

    protected ECPoint(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
        this.preCompTable = null;
        this.curve = curve;
        this.x = x;
        this.y = y;
        this.zs = zs;
    }

    protected boolean satisfiesCofactor() {
        BigInteger h = this.curve.getCofactor();
        return h == null || h.equals(ECConstants.ONE) || !ECAlgorithms.referenceMultiply(this, h).isInfinity();
    }

    public final ECPoint getDetachedPoint() {
        return normalize().detach();
    }

    public ECCurve getCurve() {
        return this.curve;
    }

    protected int getCurveCoordinateSystem() {
        return this.curve == null ? 0 : this.curve.getCoordinateSystem();
    }

    public ECFieldElement getX() {
        return normalize().getXCoord();
    }

    public ECFieldElement getY() {
        return normalize().getYCoord();
    }

    public ECFieldElement getAffineXCoord() {
        checkNormalized();
        return getXCoord();
    }

    public ECFieldElement getAffineYCoord() {
        checkNormalized();
        return getYCoord();
    }

    public ECFieldElement getXCoord() {
        return this.x;
    }

    public ECFieldElement getYCoord() {
        return this.y;
    }

    public ECFieldElement getZCoord(int index) {
        return (index < 0 || index >= this.zs.length) ? null : this.zs[index];
    }

    public ECFieldElement[] getZCoords() {
        int zsLen = this.zs.length;
        if (zsLen == 0) {
            return EMPTY_ZS;
        }
        ECFieldElement[] copy = new ECFieldElement[zsLen];
        System.arraycopy(this.zs, 0, copy, 0, zsLen);
        return copy;
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

    protected void checkNormalized() {
        if (!isNormalized()) {
            throw new IllegalStateException("point not in normal form");
        }
    }

    public boolean isNormalized() {
        int coord = getCurveCoordinateSystem();
        if (coord == 0 || coord == 5 || isInfinity() || this.zs[0].isOne()) {
            return true;
        }
        return false;
    }

    public ECPoint normalize() {
        if (isInfinity()) {
            return this;
        }
        int curveCoordinateSystem = getCurveCoordinateSystem();
        if (curveCoordinateSystem == 0 || curveCoordinateSystem == 5) {
            return this;
        }
        ECFieldElement Z1 = getZCoord(null);
        if (Z1.isOne()) {
            return this;
        }
        return normalize(Z1.invert());
    }

    ECPoint normalize(ECFieldElement zInv) {
        int curveCoordinateSystem = getCurveCoordinateSystem();
        if (curveCoordinateSystem != 6) {
            switch (curveCoordinateSystem) {
                case 1:
                    break;
                case 2:
                case 3:
                case 4:
                    ECFieldElement zInv2 = zInv.square();
                    return createScaledPoint(zInv2, zInv2.multiply(zInv));
                default:
                    throw new IllegalStateException("not a projective coordinate system");
            }
        }
        return createScaledPoint(zInv, zInv);
    }

    protected ECPoint createScaledPoint(ECFieldElement sx, ECFieldElement sy) {
        return getCurve().createRawPoint(getRawXCoord().multiply(sx), getRawYCoord().multiply(sy), this.withCompression);
    }

    public boolean isInfinity() {
        return this.x == null || this.y == null || (this.zs.length > 0 && this.zs[0].isZero());
    }

    public boolean isCompressed() {
        return this.withCompression;
    }

    public boolean isValid() {
        if (isInfinity() || getCurve() == null || (satisfiesCurveEquation() && satisfiesCofactor())) {
            return true;
        }
        return false;
    }

    public ECPoint scaleX(ECFieldElement scale) {
        if (isInfinity()) {
            return this;
        }
        return getCurve().createRawPoint(getRawXCoord().multiply(scale), getRawYCoord(), getRawZCoords(), this.withCompression);
    }

    public ECPoint scaleY(ECFieldElement scale) {
        if (isInfinity()) {
            return this;
        }
        return getCurve().createRawPoint(getRawXCoord(), getRawYCoord().multiply(scale), getRawZCoords(), this.withCompression);
    }

    public boolean equals(ECPoint other) {
        boolean z = false;
        if (other == null) {
            return false;
        }
        ECCurve c1 = getCurve();
        ECCurve c2 = other.getCurve();
        boolean n1 = c1 == null;
        boolean n2 = c2 == null;
        boolean i1 = isInfinity();
        boolean i2 = other.isInfinity();
        if (i1 || i2) {
            if (i1 && i2 && (n1 || n2 || c1.equals(c2))) {
                z = true;
            }
            return z;
        }
        ECPoint p1 = this;
        ECPoint p2 = other;
        if (!(n1 && n2)) {
            if (n1) {
                p2 = p2.normalize();
            } else if (n2) {
                p1 = p1.normalize();
            } else if (!c1.equals(c2)) {
                return false;
            } else {
                ECPoint[] points = new ECPoint[]{this, c1.importPoint(p2)};
                c1.normalizeAll(points);
                p1 = points[0];
                p2 = points[1];
            }
        }
        if (p1.getXCoord().equals(p2.getXCoord()) && p1.getYCoord().equals(p2.getYCoord())) {
            z = true;
        }
        return z;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof ECPoint) {
            return equals((ECPoint) other);
        }
        return false;
    }

    public int hashCode() {
        ECCurve c = getCurve();
        int hc = c == null ? 0 : ~c.hashCode();
        if (isInfinity()) {
            return hc;
        }
        ECPoint p = normalize();
        return (hc ^ (p.getXCoord().hashCode() * 17)) ^ (p.getYCoord().hashCode() * 257);
    }

    public String toString() {
        if (isInfinity()) {
            return "INF";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        sb.append(getRawXCoord());
        sb.append(',');
        sb.append(getRawYCoord());
        for (Object append : this.zs) {
            sb.append(',');
            sb.append(append);
        }
        sb.append(')');
        return sb.toString();
    }

    public byte[] getEncoded() {
        return getEncoded(this.withCompression);
    }

    public byte[] getEncoded(boolean compressed) {
        if (isInfinity()) {
            return new byte[1];
        }
        ECPoint normed = normalize();
        byte[] X = normed.getXCoord().getEncoded();
        byte[] PO;
        if (compressed) {
            PO = new byte[(X.length + 1)];
            PO[0] = (byte) (normed.getCompressionYTilde() ? 3 : 2);
            System.arraycopy(X, 0, PO, 1, X.length);
            return PO;
        }
        PO = normed.getYCoord().getEncoded();
        byte[] PO2 = new byte[((X.length + PO.length) + 1)];
        PO2[0] = (byte) 4;
        System.arraycopy(X, 0, PO2, 1, X.length);
        System.arraycopy(PO, 0, PO2, X.length + 1, PO.length);
        return PO2;
    }

    public ECPoint timesPow2(int e) {
        if (e >= 0) {
            int e2 = e;
            e = this;
            while (true) {
                e2--;
                if (e2 < 0) {
                    return e;
                }
                e = e.twice();
            }
        } else {
            throw new IllegalArgumentException("'e' cannot be negative");
        }
    }

    public ECPoint twicePlus(ECPoint b) {
        return twice().add(b);
    }

    public ECPoint threeTimes() {
        return twicePlus(this);
    }

    public ECPoint multiply(BigInteger k) {
        return getCurve().getMultiplier().multiply(this, k);
    }
}
