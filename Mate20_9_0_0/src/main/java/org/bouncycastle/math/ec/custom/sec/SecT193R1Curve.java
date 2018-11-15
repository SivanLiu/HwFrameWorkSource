package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractF2m;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECLookupTable;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.raw.Nat256;
import org.bouncycastle.util.encoders.Hex;

public class SecT193R1Curve extends AbstractF2m {
    private static final int SecT193R1_DEFAULT_COORDS = 6;
    protected SecT193R1Point infinity;

    public SecT193R1Curve() {
        super(CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256, 15, 0, 0);
        this.infinity = new SecT193R1Point(this, null, null);
        this.a = fromBigInteger(new BigInteger(1, Hex.decode("0017858FEB7A98975169E171F77B4087DE098AC8A911DF7B01")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decode("00FDFB49BFE6C3A89FACADAA7A1E5BBC7CC1C2E5D831478814")));
        this.order = new BigInteger(1, Hex.decode("01000000000000000000000000C7F34A778F443ACC920EBA49"));
        this.cofactor = BigInteger.valueOf(2);
        this.coord = 6;
    }

    protected ECCurve cloneCurve() {
        return new SecT193R1Curve();
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] eCPointArr, int i, final int i2) {
        final long[] jArr = new long[((i2 * 4) * 2)];
        int i3 = 0;
        int i4 = i3;
        while (i3 < i2) {
            ECPoint eCPoint = eCPointArr[i + i3];
            Nat256.copy64(((SecT193FieldElement) eCPoint.getRawXCoord()).x, 0, jArr, i4);
            i4 += 4;
            Nat256.copy64(((SecT193FieldElement) eCPoint.getRawYCoord()).x, 0, jArr, i4);
            i4 += 4;
            i3++;
        }
        return new ECLookupTable() {
            public int getSize() {
                return i2;
            }

            public ECPoint lookup(int i) {
                long[] create64 = Nat256.create64();
                long[] create642 = Nat256.create64();
                int i2 = 0;
                int i3 = i2;
                while (i2 < i2) {
                    long j = (long) (((i2 ^ i) - 1) >> 31);
                    for (int i4 = 0; i4 < 4; i4++) {
                        create64[i4] = create64[i4] ^ (jArr[i3 + i4] & j);
                        create642[i4] = create642[i4] ^ (jArr[(i3 + 4) + i4] & j);
                    }
                    i3 += 8;
                    i2++;
                }
                return SecT193R1Curve.this.createRawPoint(new SecT193FieldElement(create64), new SecT193FieldElement(create642), false);
            }
        };
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecT193R1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecT193R1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecT193FieldElement(bigInteger);
    }

    public int getFieldSize() {
        return CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256;
    }

    public ECPoint getInfinity() {
        return this.infinity;
    }

    public int getK1() {
        return 15;
    }

    public int getK2() {
        return 0;
    }

    public int getK3() {
        return 0;
    }

    public int getM() {
        return CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256;
    }

    public boolean isKoblitz() {
        return false;
    }

    public boolean isTrinomial() {
        return true;
    }

    public boolean supportsCoordinateSystem(int i) {
        return i == 6;
    }
}
