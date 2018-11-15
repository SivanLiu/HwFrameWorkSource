package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class MontgomeryLadderMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        ECPoint[] eCPointArr = new ECPoint[]{eCPoint.getCurve().getInfinity(), eCPoint};
        int bitLength = bigInteger.bitLength();
        while (true) {
            bitLength--;
            if (bitLength < 0) {
                return eCPointArr[0];
            }
            boolean testBit = bigInteger.testBit(bitLength);
            int i = 1 - testBit;
            eCPointArr[i] = eCPointArr[i].add(eCPointArr[testBit]);
            eCPointArr[testBit] = eCPointArr[testBit].twice();
        }
    }
}
