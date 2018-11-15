package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class ZSignedDigitL2RMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        eCPoint = eCPoint.normalize();
        ECPoint negate = eCPoint.negate();
        int bitLength = bigInteger.bitLength();
        int lowestSetBit = bigInteger.getLowestSetBit();
        ECPoint eCPoint2 = eCPoint;
        while (true) {
            bitLength--;
            if (bitLength <= lowestSetBit) {
                return eCPoint2.timesPow2(lowestSetBit);
            }
            eCPoint2 = eCPoint2.twicePlus(bigInteger.testBit(bitLength) ? eCPoint : negate);
        }
    }
}
