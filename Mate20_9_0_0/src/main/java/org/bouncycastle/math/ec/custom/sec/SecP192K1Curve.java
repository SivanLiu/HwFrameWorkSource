package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractFp;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat192;
import org.bouncycastle.util.encoders.Hex;

public class SecP192K1Curve extends AbstractFp {
    private static final int SecP192K1_DEFAULT_COORDS = 2;
    public static final BigInteger q = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFEE37"));
    protected SecP192K1Point infinity;

    public SecP192K1Curve() {
        super(q);
        this.infinity = new SecP192K1Point(this, null, null);
        this.a = fromBigInteger(ECConstants.ZERO);
        this.b = fromBigInteger(BigInteger.valueOf(3));
        this.order = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFE26F2FC170F69466A74DEFD8D"));
        this.cofactor = BigInteger.valueOf(1);
        this.coord = 2;
    }

    protected ECCurve cloneCurve() {
        return new SecP192K1Curve();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final int[] iArr = new int[((i2 * 6) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat192.copy(((SecP192K1FieldElement) eCPoint.getRawXCoord()).x, 0, iArr, i4);
            i4 += 6;
            Nat192.copy(((SecP192K1FieldElement) eCPoint.getRawYCoord()).x, 0, iArr, i4);
            i4 += 6;
            i3++;
        }
        return new ECLookupTable() {
            public int getSize() {
                return i2;
            }

            public ECPoint lookup(int i) {
                int[] create = Nat192.create();
                int[] create2 = Nat192.create();
                int i2 = 0;
                int i3 = i2;
                while (i2 < i2) {
                    int i4 = ((i2 ^ i) - 1) >> 31;
                    for (int i5 = 0; i5 < 6; i5++) {
                        create[i5] = create[i5] ^ (iArr[i3 + i5] & i4);
                        create2[i5] = create2[i5] ^ (iArr[(i3 + 6) + i5] & i4);
                    }
                    i3 += 12;
                    i2++;
                }
                return SecP192K1Curve.this.createRawPoint(new SecP192K1FieldElement(create), new SecP192K1FieldElement(create2), false);
            }
        };
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecP192K1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecP192K1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecP192K1FieldElement(bigInteger);
    }

    public int getFieldSize() {
        return q.bitLength();
    }

    public ECPoint getInfinity() {
        return this.infinity;
    }

    public BigInteger getQ() {
        return q;
    }

    public boolean supportsCoordinateSystem(int i) {
        return i == 2;
    }
}
