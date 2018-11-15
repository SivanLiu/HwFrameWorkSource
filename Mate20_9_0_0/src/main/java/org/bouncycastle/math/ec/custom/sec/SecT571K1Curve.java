package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractF2m;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECMultiplier;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.WTauNafMultiplier;
import org.bouncycastle.math.raw.Nat576;
import org.bouncycastle.util.encoders.Hex;

public class SecT571K1Curve extends AbstractF2m {
    private static final int SecT571K1_DEFAULT_COORDS = 6;
    protected SecT571K1Point infinity;

    public SecT571K1Curve() {
        super(571, 2, 5, 10);
        this.infinity = new SecT571K1Point(this, null, null);
        this.a = fromBigInteger(BigInteger.valueOf(0));
        this.b = fromBigInteger(BigInteger.valueOf(1));
        this.order = new BigInteger(1, Hex.decode("020000000000000000000000000000000000000000000000000000000000000000000000131850E1F19A63E4B391A8DB917F4138B630D84BE5D639381E91DEB45CFE778F637C1001"));
        this.cofactor = BigInteger.valueOf(4);
        this.coord = 6;
    }

    protected ECCurve cloneCurve() {
        return new SecT571K1Curve();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final long[] jArr = new long[((i2 * 9) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat576.copy64(((SecT571FieldElement) eCPoint.getRawXCoord()).x, 0, jArr, i4);
            i4 += 9;
            Nat576.copy64(((SecT571FieldElement) eCPoint.getRawYCoord()).x, 0, jArr, i4);
            i4 += 9;
            i3++;
        }
        return new ECLookupTable() {
            public int getSize() {
                return i2;
            }

            public ECPoint lookup(int i) {
                long[] create64 = Nat576.create64();
                long[] create642 = Nat576.create64();
                int i2 = 0;
                int i3 = i2;
                while (i2 < i2) {
                    long j = (long) (((i2 ^ i) - 1) >> 31);
                    for (int i4 = 0; i4 < 9; i4++) {
                        create64[i4] = create64[i4] ^ (jArr[i3 + i4] & j);
                        create642[i4] = create642[i4] ^ (jArr[(i3 + 9) + i4] & j);
                    }
                    i3 += 18;
                    i2++;
                }
                return SecT571K1Curve.this.createRawPoint(new SecT571FieldElement(create64), new SecT571FieldElement(create642), false);
            }
        };
    }

    protected ECMultiplier createDefaultMultiplier() {
        return new WTauNafMultiplier();
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecT571K1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecT571K1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecT571FieldElement(bigInteger);
    }

    public int getFieldSize() {
        return 571;
    }

    public ECPoint getInfinity() {
        return this.infinity;
    }

    public int getK1() {
        return 2;
    }

    public int getK2() {
        return 5;
    }

    public int getK3() {
        return 10;
    }

    public int getM() {
        return 571;
    }

    public boolean isKoblitz() {
        return true;
    }

    public boolean isTrinomial() {
        return false;
    }

    public boolean supportsCoordinateSystem(int i) {
        return i == 6;
    }
}
