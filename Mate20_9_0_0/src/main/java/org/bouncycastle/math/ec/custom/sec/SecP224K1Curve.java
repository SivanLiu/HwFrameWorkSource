package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractFp;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat224;
import org.bouncycastle.util.encoders.Hex;

public class SecP224K1Curve extends AbstractFp {
    private static final int SECP224K1_DEFAULT_COORDS = 2;
    public static final BigInteger q = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFE56D"));
    protected SecP224K1Point infinity;

    public SecP224K1Curve() {
        super(q);
        this.infinity = new SecP224K1Point(this, null, null);
        this.a = fromBigInteger(ECConstants.ZERO);
        this.b = fromBigInteger(BigInteger.valueOf(5));
        this.order = new BigInteger(1, Hex.decode("010000000000000000000000000001DCE8D2EC6184CAF0A971769FB1F7"));
        this.cofactor = BigInteger.valueOf(1);
        this.coord = 2;
    }

    protected ECCurve cloneCurve() {
        return new SecP224K1Curve();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final int[] iArr = new int[((i2 * 7) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat224.copy(((SecP224K1FieldElement) eCPoint.getRawXCoord()).x, 0, iArr, i4);
            i4 += 7;
            Nat224.copy(((SecP224K1FieldElement) eCPoint.getRawYCoord()).x, 0, iArr, i4);
            i4 += 7;
            i3++;
        }
        return new ECLookupTable() {
            public int getSize() {
                return i2;
            }

            public ECPoint lookup(int i) {
                int[] create = Nat224.create();
                int[] create2 = Nat224.create();
                int i2 = 0;
                int i3 = i2;
                while (i2 < i2) {
                    int i4 = ((i2 ^ i) - 1) >> 31;
                    for (int i5 = 0; i5 < 7; i5++) {
                        create[i5] = create[i5] ^ (iArr[i3 + i5] & i4);
                        create2[i5] = create2[i5] ^ (iArr[(i3 + 7) + i5] & i4);
                    }
                    i3 += 14;
                    i2++;
                }
                return SecP224K1Curve.this.createRawPoint(new SecP224K1FieldElement(create), new SecP224K1FieldElement(create2), false);
            }
        };
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecP224K1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecP224K1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecP224K1FieldElement(bigInteger);
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
