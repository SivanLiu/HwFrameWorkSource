package org.bouncycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.math.ec.WNafUtil;
import org.bouncycastle.util.BigIntegers;

class DHKeyGeneratorHelper {
    static final DHKeyGeneratorHelper INSTANCE = new DHKeyGeneratorHelper();
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private DHKeyGeneratorHelper() {
    }

    BigInteger calculatePrivate(DHParameters dHParameters, SecureRandom secureRandom) {
        int l = dHParameters.getL();
        int i;
        BigInteger bit;
        if (l != 0) {
            i = l >>> 2;
            do {
                bit = new BigInteger(l, secureRandom).setBit(l - 1);
            } while (WNafUtil.getNafWeight(bit) < i);
            return bit;
        }
        BigInteger createRandomInRange;
        BigInteger bigInteger = TWO;
        i = dHParameters.getM();
        if (i != 0) {
            bigInteger = ONE.shiftLeft(i - 1);
        }
        BigInteger q = dHParameters.getQ();
        if (q == null) {
            q = dHParameters.getP();
        }
        bit = q.subtract(TWO);
        i = bit.bitLength() >>> 2;
        do {
            createRandomInRange = BigIntegers.createRandomInRange(bigInteger, bit, secureRandom);
        } while (WNafUtil.getNafWeight(createRandomInRange) < i);
        return createRandomInRange;
    }

    BigInteger calculatePublic(DHParameters dHParameters, BigInteger bigInteger) {
        return dHParameters.getG().modPow(bigInteger, dHParameters.getP());
    }
}
