package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.math.raw.Nat;

public class FixedPointCombMultiplier extends AbstractECMultiplier {
    protected int getWidthForCombSize(int i) {
        return i > 257 ? 6 : 5;
    }

    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        ECCurve curve = eCPoint.getCurve();
        int combSize = FixedPointUtil.getCombSize(curve);
        if (bigInteger.bitLength() <= combSize) {
            FixedPointPreCompInfo precompute = FixedPointUtil.precompute(eCPoint);
            ECLookupTable lookupTable = precompute.getLookupTable();
            int width = precompute.getWidth();
            combSize = ((combSize + width) - 1) / width;
            ECPoint infinity = curve.getInfinity();
            width *= combSize;
            int[] fromBigInteger = Nat.fromBigInteger(width, bigInteger);
            width--;
            ECPoint eCPoint2 = infinity;
            for (int i = 0; i < combSize; i++) {
                int i2 = 0;
                for (int i3 = width - i; i3 >= 0; i3 -= combSize) {
                    i2 = (i2 << 1) | Nat.getBit(fromBigInteger, i3);
                }
                eCPoint2 = eCPoint2.twicePlus(lookupTable.lookup(i2));
            }
            return eCPoint2.add(precompute.getOffset());
        }
        throw new IllegalStateException("fixed-point comb doesn't support scalars larger than the curve order");
    }
}
