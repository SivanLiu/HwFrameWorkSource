package org.bouncycastle.math.ec.custom.gm;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractFp;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat256;
import org.bouncycastle.util.encoders.Hex;

public class SM2P256V1Curve extends AbstractFp {
    private static final int SM2P256V1_DEFAULT_COORDS = 2;
    public static final BigInteger q = new BigInteger(1, Hex.decode("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF"));
    protected SM2P256V1Point infinity;

    public SM2P256V1Curve() {
        super(q);
        this.infinity = new SM2P256V1Point(this, null, null);
        this.a = fromBigInteger(new BigInteger(1, Hex.decode("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decode("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93")));
        this.order = new BigInteger(1, Hex.decode("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123"));
        this.cofactor = BigInteger.valueOf(1);
        this.coord = 2;
    }

    protected ECCurve cloneCurve() {
        return new SM2P256V1Curve();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final int[] iArr = new int[((i2 * 8) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat256.copy(((SM2P256V1FieldElement) eCPoint.getRawXCoord()).x, 0, iArr, i4);
            i4 += 8;
            Nat256.copy(((SM2P256V1FieldElement) eCPoint.getRawYCoord()).x, 0, iArr, i4);
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
                return SM2P256V1Curve.this.createRawPoint(new SM2P256V1FieldElement(create), new SM2P256V1FieldElement(create2), false);
            }
        };
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SM2P256V1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SM2P256V1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SM2P256V1FieldElement(bigInteger);
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
