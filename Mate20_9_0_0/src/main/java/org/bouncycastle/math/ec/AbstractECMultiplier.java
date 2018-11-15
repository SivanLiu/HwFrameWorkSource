package org.bouncycastle.math.ec;

import java.math.BigInteger;

public abstract class AbstractECMultiplier implements ECMultiplier {
    public ECPoint multiply(ECPoint eCPoint, BigInteger bigInteger) {
        int signum = bigInteger.signum();
        if (signum == 0 || eCPoint.isInfinity()) {
            return eCPoint.getCurve().getInfinity();
        }
        eCPoint = multiplyPositive(eCPoint, bigInteger.abs());
        if (signum <= 0) {
            eCPoint = eCPoint.negate();
        }
        return ECAlgorithms.validatePoint(eCPoint);
    }

    protected abstract ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger);
}
