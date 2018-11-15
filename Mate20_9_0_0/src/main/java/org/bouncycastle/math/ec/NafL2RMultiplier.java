package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class NafL2RMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        int[] generateCompactNaf = WNafUtil.generateCompactNaf(bigInteger);
        ECPoint normalize = eCPoint.normalize();
        ECPoint negate = normalize.negate();
        eCPoint = eCPoint.getCurve().getInfinity();
        int length = generateCompactNaf.length;
        while (true) {
            length--;
            if (length < 0) {
                return eCPoint;
            }
            int i = generateCompactNaf[length];
            eCPoint = eCPoint.twicePlus((i >> 16) < 0 ? negate : normalize).timesPow2(i & 65535);
        }
    }
}
