package org.bouncycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.CramerShoupParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.util.BigIntegers;

public class CramerShoupParametersGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private int certainty;
    private SecureRandom random;
    private int size;

    private static class ParametersHelper {
        private static final BigInteger TWO = BigInteger.valueOf(2);

        private ParametersHelper() {
        }

        static BigInteger[] generateSafePrimes(int i, int i2, SecureRandom secureRandom) {
            BigInteger bigInteger;
            i--;
            while (true) {
                bigInteger = new BigInteger(i, 2, secureRandom);
                if (!bigInteger.shiftLeft(1).add(CramerShoupParametersGenerator.ONE).isProbablePrime(i2) || (i2 > 2 && !bigInteger.isProbablePrime(i2))) {
                }
            }
            return new BigInteger[]{r3, bigInteger};
        }

        static BigInteger selectGenerator(BigInteger bigInteger, SecureRandom secureRandom) {
            BigInteger modPow;
            BigInteger subtract = bigInteger.subtract(TWO);
            do {
                modPow = BigIntegers.createRandomInRange(TWO, subtract, secureRandom).modPow(TWO, bigInteger);
            } while (modPow.equals(CramerShoupParametersGenerator.ONE));
            return modPow;
        }
    }

    public CramerShoupParameters generateParameters() {
        BigInteger bigInteger = ParametersHelper.generateSafePrimes(this.size, this.certainty, this.random)[1];
        BigInteger selectGenerator = ParametersHelper.selectGenerator(bigInteger, this.random);
        while (true) {
            BigInteger selectGenerator2 = ParametersHelper.selectGenerator(bigInteger, this.random);
            if (!selectGenerator.equals(selectGenerator2)) {
                return new CramerShoupParameters(bigInteger, selectGenerator, selectGenerator2, new SHA256Digest());
            }
        }
    }

    public CramerShoupParameters generateParameters(DHParameters dHParameters) {
        BigInteger p = dHParameters.getP();
        BigInteger g = dHParameters.getG();
        while (true) {
            BigInteger selectGenerator = ParametersHelper.selectGenerator(p, this.random);
            if (!g.equals(selectGenerator)) {
                return new CramerShoupParameters(p, g, selectGenerator, new SHA256Digest());
            }
        }
    }

    public void init(int i, int i2, SecureRandom secureRandom) {
        this.size = i;
        this.certainty = i2;
        this.random = secureRandom;
    }
}
