package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class ZSignedDigitR2LMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        ECPoint infinity = eCPoint.getCurve().getInfinity();
        int bitLength = bigInteger.bitLength();
        int lowestSetBit = bigInteger.getLowestSetBit();
        eCPoint = eCPoint.timesPow2(lowestSetBit);
        while (true) {
            lowestSetBit++;
            if (lowestSetBit >= bitLength) {
                return infinity.add(eCPoint);
            }
            infinity = infinity.add(bigInteger.testBit(lowestSetBit) ? eCPoint : eCPoint.negate());
            eCPoint = eCPoint.twice();
        }
    }
}
