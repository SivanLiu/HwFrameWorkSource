package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class DoubleAddMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        ECPoint[] eCPointArr = new ECPoint[]{eCPoint.getCurve().getInfinity(), eCPoint};
        int bitLength = bigInteger.bitLength();
        for (int i = 0; i < bitLength; i++) {
            boolean testBit = bigInteger.testBit(i);
            int i2 = 1 - testBit;
            eCPointArr[i2] = eCPointArr[i2].twicePlus(eCPointArr[testBit]);
        }
        return eCPointArr[0];
    }
}
