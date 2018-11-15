package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class ReferenceMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        return ECAlgorithms.referenceMultiply(eCPoint, bigInteger);
    }
}
