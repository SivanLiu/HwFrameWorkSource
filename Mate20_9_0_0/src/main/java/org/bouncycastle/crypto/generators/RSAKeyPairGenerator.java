package org.bouncycastle.crypto.generators;

import java.math.BigInteger;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.math.Primes;
import org.bouncycastle.math.ec.WNafUtil;

public class RSAKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private int iterations;
    private RSAKeyGenerationParameters param;

    private static int getNumberOfIterations(int i, int i2) {
        return i >= 1536 ? i2 <= 100 ? 3 : i2 <= 128 ? 4 : 4 + (((i2 - 128) + 1) / 2) : i >= 1024 ? i2 <= 100 ? 4 : i2 <= 112 ? 5 : 5 + (((i2 - 112) + 1) / 2) : i >= 512 ? i2 <= 80 ? 5 : i2 <= 100 ? 7 : 7 + (((i2 - 100) + 1) / 2) : i2 <= 80 ? 40 : 40 + (((i2 - 80) + 1) / 2);
    }

    protected BigInteger chooseRandomPrime(int i, BigInteger bigInteger, BigInteger bigInteger2) {
        for (int i2 = 0; i2 != 5 * i; i2++) {
            BigInteger bigInteger3 = new BigInteger(i, 1, this.param.getRandom());
            if (!bigInteger3.mod(bigInteger).equals(ONE) && bigInteger3.multiply(bigInteger3).compareTo(bigInteger2) >= 0 && isProbablePrime(bigInteger3) && bigInteger.gcd(bigInteger3.subtract(ONE)).equals(ONE)) {
                return bigInteger3;
            }
        }
        throw new IllegalStateException("unable to generate prime number for RSA key");
    }

    public AsymmetricCipherKeyPair generateKeyPair() {
        RSAKeyPairGenerator rSAKeyPairGenerator = this;
        int strength = rSAKeyPairGenerator.param.getStrength();
        int i = (strength + 1) / 2;
        int i2 = strength - i;
        int i3 = strength / 2;
        int i4 = i3 - 100;
        int i5 = strength / 3;
        if (i4 < i5) {
            i4 = i5;
        }
        i5 = strength >> 2;
        BigInteger pow = BigInteger.valueOf(2).pow(i3);
        BigInteger shiftLeft = ONE.shiftLeft(strength - 1);
        BigInteger shiftLeft2 = ONE.shiftLeft(i4);
        AsymmetricCipherKeyPair asymmetricCipherKeyPair = null;
        Object obj = null;
        while (obj == null) {
            BigInteger chooseRandomPrime;
            BigInteger chooseRandomPrime2;
            BigInteger abs;
            BigInteger bigInteger;
            BigInteger publicExponent = rSAKeyPairGenerator.param.getPublicExponent();
            while (true) {
                chooseRandomPrime = rSAKeyPairGenerator.chooseRandomPrime(i, publicExponent, shiftLeft);
                while (true) {
                    chooseRandomPrime2 = rSAKeyPairGenerator.chooseRandomPrime(i2, publicExponent, shiftLeft);
                    abs = chooseRandomPrime2.subtract(chooseRandomPrime).abs();
                    if (abs.bitLength() >= i4 && abs.compareTo(shiftLeft2) > 0) {
                        abs = chooseRandomPrime.multiply(chooseRandomPrime2);
                        if (abs.bitLength() == strength) {
                            break;
                        }
                        chooseRandomPrime = chooseRandomPrime.max(chooseRandomPrime2);
                    } else {
                        strength = strength;
                        rSAKeyPairGenerator = this;
                    }
                }
                if (WNafUtil.getNafWeight(abs) >= i5) {
                    break;
                }
            }
            if (chooseRandomPrime.compareTo(chooseRandomPrime2) < 0) {
                bigInteger = chooseRandomPrime;
                chooseRandomPrime = chooseRandomPrime2;
            } else {
                bigInteger = chooseRandomPrime2;
            }
            chooseRandomPrime2 = chooseRandomPrime.subtract(ONE);
            BigInteger subtract = bigInteger.subtract(ONE);
            int i6 = strength;
            BigInteger modInverse = publicExponent.modInverse(chooseRandomPrime2.divide(chooseRandomPrime2.gcd(subtract)).multiply(subtract));
            if (modInverse.compareTo(pow) > 0) {
                BigInteger remainder = modInverse.remainder(chooseRandomPrime2);
                BigInteger remainder2 = modInverse.remainder(subtract);
                BigInteger modInverse2 = bigInteger.modInverse(chooseRandomPrime);
                AsymmetricKeyParameter rSAKeyParameters = new RSAKeyParameters(false, abs, publicExponent);
                AsymmetricKeyParameter asymmetricKeyParameter = r13;
                AsymmetricKeyParameter rSAPrivateCrtKeyParameters = new RSAPrivateCrtKeyParameters(abs, publicExponent, modInverse, chooseRandomPrime, bigInteger, remainder, remainder2, modInverse2);
                asymmetricCipherKeyPair = new AsymmetricCipherKeyPair(rSAKeyParameters, asymmetricKeyParameter);
                int obj2 = 1;
            }
            strength = i6;
            rSAKeyPairGenerator = this;
        }
        return asymmetricCipherKeyPair;
    }

    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.param = (RSAKeyGenerationParameters) keyGenerationParameters;
        this.iterations = getNumberOfIterations(this.param.getStrength(), this.param.getCertainty());
    }

    protected boolean isProbablePrime(BigInteger bigInteger) {
        return !Primes.hasAnySmallFactors(bigInteger) && Primes.isMRProbablePrime(bigInteger, this.param.getRandom(), this.iterations);
    }
}
