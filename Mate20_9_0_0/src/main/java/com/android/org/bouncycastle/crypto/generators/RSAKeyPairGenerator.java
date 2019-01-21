package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.android.org.bouncycastle.crypto.KeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
import com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import com.android.org.bouncycastle.math.Primes;
import com.android.org.bouncycastle.math.ec.WNafUtil;
import java.math.BigInteger;

public class RSAKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private RSAKeyGenerationParameters param;

    public void init(KeyGenerationParameters param) {
        this.param = (RSAKeyGenerationParameters) param;
    }

    public AsymmetricCipherKeyPair generateKeyPair() {
        int pbitlength;
        int qbitlength;
        RSAKeyPairGenerator d = this;
        AsymmetricCipherKeyPair result = null;
        boolean done = false;
        int strength = d.param.getStrength();
        int pbitlength2 = (strength + 1) / 2;
        int qbitlength2 = strength - pbitlength2;
        int mindiffbits = (strength / 2) - 100;
        if (mindiffbits < strength / 3) {
            mindiffbits = strength / 3;
        }
        boolean minWeight = strength >> 2;
        BigInteger dLowerBound = BigInteger.valueOf(2).pow(strength / 2);
        BigInteger squaredBound = ONE.shiftLeft(strength - 1);
        BigInteger minDiff = ONE.shiftLeft(mindiffbits);
        while (!done) {
            BigInteger q;
            BigInteger diff;
            BigInteger n;
            boolean done2;
            BigInteger e = d.param.getPublicExponent();
            BigInteger p = d.chooseRandomPrime(pbitlength2, e, squaredBound);
            while (true) {
                q = d.chooseRandomPrime(qbitlength2, e, squaredBound);
                diff = q.subtract(p).abs();
                if (diff.bitLength() < mindiffbits || diff.compareTo(minDiff) <= 0) {
                    done = done;
                    strength = strength;
                    pbitlength2 = pbitlength2;
                    qbitlength2 = qbitlength2;
                    d = this;
                } else {
                    n = p.multiply(q);
                    done2 = done;
                    if (n.bitLength() == strength) {
                        if (WNafUtil.getNafWeight(n) >= minWeight) {
                            break;
                        }
                        p = d.chooseRandomPrime(pbitlength2, e, squaredBound);
                    } else {
                        p = p.max(q);
                    }
                    done = done2;
                }
            }
            if (p.compareTo(q) >= false) {
                diff = p;
                done = q;
            } else {
                done = p;
                diff = q;
            }
            q = done.subtract(ONE);
            p = diff.subtract(ONE);
            BigInteger gcd = q.gcd(p);
            boolean strength2 = strength;
            BigInteger lcm = q.divide(gcd).multiply(p);
            gcd = e.modInverse(lcm);
            if (gcd.compareTo(dLowerBound) <= 0) {
                done = done2;
                strength = strength2;
            } else {
                pbitlength = pbitlength2;
                qbitlength = qbitlength2;
                BigInteger q2 = diff;
                result = new AsymmetricCipherKeyPair(new RSAKeyParameters(0, n, e), new RSAPrivateCrtKeyParameters(n, e, gcd, done, q2, gcd.remainder(q), gcd.remainder(p), diff.modInverse(done)));
                done = true;
                boolean strength3 = strength2;
                pbitlength2 = pbitlength;
                qbitlength2 = qbitlength;
            }
            d = this;
        }
        int i = strength3;
        pbitlength = pbitlength2;
        qbitlength = qbitlength2;
        return result;
    }

    protected BigInteger chooseRandomPrime(int bitlength, BigInteger e, BigInteger sqrdBound) {
        for (int i = 0; i != 5 * bitlength; i++) {
            BigInteger p = new BigInteger(bitlength, 1, this.param.getRandom());
            if (!p.mod(e).equals(ONE) && p.multiply(p).compareTo(sqrdBound) >= 0 && isProbablePrime(p) && e.gcd(p.subtract(ONE)).equals(ONE)) {
                return p;
            }
        }
        throw new IllegalStateException("unable to generate prime number for RSA key");
    }

    protected boolean isProbablePrime(BigInteger x) {
        return !Primes.hasAnySmallFactors(x) && Primes.isMRProbablePrime(x, this.param.getRandom(), getNumberOfIterations(x.bitLength(), this.param.getCertainty()));
    }

    private static int getNumberOfIterations(int bits, int certainty) {
        int i = 4;
        if (bits >= 1536) {
            if (certainty <= 100) {
                i = 3;
            } else if (certainty > 128) {
                i = 4 + (((certainty - 128) + 1) / 2);
            }
            return i;
        }
        int i2 = 5;
        if (bits >= 1024) {
            if (certainty > 100) {
                if (certainty <= 112) {
                    i = 5;
                } else {
                    i = (((certainty - 112) + 1) / 2) + 5;
                }
            }
            return i;
        } else if (bits >= 512) {
            if (certainty > 80) {
                if (certainty <= 100) {
                    i2 = 7;
                } else {
                    i2 = 7 + (((certainty - 100) + 1) / 2);
                }
            }
            return i2;
        } else {
            i = 40;
            if (certainty > 80) {
                i = 40 + (((certainty - 80) + 1) / 2);
            }
            return i;
        }
    }
}
