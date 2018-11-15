package org.bouncycastle.math.ec.custom.djb;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractFp;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat256;
import org.bouncycastle.util.encoders.Hex;

public class Curve25519 extends AbstractFp {
    private static final int Curve25519_DEFAULT_COORDS = 4;
    public static final BigInteger q = Nat256.toBigInteger(Curve25519Field.P);
    protected Curve25519Point infinity;

    public Curve25519() {
        super(q);
        this.infinity = new Curve25519Point(this, null, null);
        this.a = fromBigInteger(new BigInteger(1, Hex.decode("2AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA984914A144")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decode("7B425ED097B425ED097B425ED097B425ED097B425ED097B4260B5E9C7710C864")));
        this.order = new BigInteger(1, Hex.decode("1000000000000000000000000000000014DEF9DEA2F79CD65812631A5CF5D3ED"));
        this.cofactor = BigInteger.valueOf(8);
        this.coord = 4;
    }

    protected ECCurve cloneCurve() {
        return new Curve25519();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final int[] iArr = new int[((i2 * 8) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat256.copy(((Curve25519FieldElement) eCPoint.getRawXCoord()).x, 0, iArr, i4);
            i4 += 8;
            Nat256.copy(((Curve25519FieldElement) eCPoint.getRawYCoord()).x, 0, iArr, i4);
            i4 += 8;
            i3++;
        }
        return new ECLookupTable() {
            public int getSize() {
                return i2;
            }

            public ECPoint lookup(int i) {
                int[] create = Nat256.create();
                int[] create2 = Nat256.create();
                int i2 = 0;
                int i3 = i2;
                while (i2 < i2) {
                    int i4 = ((i2 ^ i) - 1) >> 31;
                    for (int i5 = 0; i5 < 8; i5++) {
                        create[i5] = create[i5] ^ (iArr[i3 + i5] & i4);
                        create2[i5] = create2[i5] ^ (iArr[(i3 + 8) + i5] & i4);
                    }
                    i3 += 16;
                    i2++;
                }
                return Curve25519.this.createRawPoint(new Curve25519FieldElement(create), new Curve25519FieldElement(create2), false);
            }
        };
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new Curve25519Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new Curve25519Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new Curve25519FieldElement(bigInteger);
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
        return i == 4;
    }
}
