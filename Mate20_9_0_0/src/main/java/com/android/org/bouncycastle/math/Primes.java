package com.android.org.bouncycastle.math;

import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.security.SecureRandom;

public abstract class Primes {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    public static final int SMALL_FACTOR_LIMIT = 211;
    private static final BigInteger THREE = BigInteger.valueOf(3);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    public static class MROutput {
        private BigInteger factor;
        private boolean provablyComposite;

        private static MROutput probablyPrime() {
            return new MROutput(false, null);
        }

        private static MROutput provablyCompositeWithFactor(BigInteger factor) {
            return new MROutput(true, factor);
        }

        private static MROutput provablyCompositeNotPrimePower() {
            return new MROutput(true, null);
        }

        private MROutput(boolean provablyComposite, BigInteger factor) {
            this.provablyComposite = provablyComposite;
            this.factor = factor;
        }

        public BigInteger getFactor() {
            return this.factor;
        }

        public boolean isProvablyComposite() {
            return this.provablyComposite;
        }

        public boolean isNotPrimePower() {
            return this.provablyComposite && this.factor == null;
        }
    }

    public static class STOutput {
        private BigInteger prime;
        private int primeGenCounter;
        private byte[] primeSeed;

        private STOutput(BigInteger prime, byte[] primeSeed, int primeGenCounter) {
            this.prime = prime;
            this.primeSeed = primeSeed;
            this.primeGenCounter = primeGenCounter;
        }

        public BigInteger getPrime() {
            return this.prime;
        }

        public byte[] getPrimeSeed() {
            return this.primeSeed;
        }

        public int getPrimeGenCounter() {
            return this.primeGenCounter;
        }
    }

    public static STOutput generateSTRandomPrime(Digest hash, int length, byte[] inputSeed) {
        if (hash == null) {
            throw new IllegalArgumentException("'hash' cannot be null");
        } else if (length < 2) {
            throw new IllegalArgumentException("'length' must be >= 2");
        } else if (inputSeed != null && inputSeed.length != 0) {
            return implSTRandomPrime(hash, length, Arrays.clone(inputSeed));
        } else {
            throw new IllegalArgumentException("'inputSeed' cannot be null or empty");
        }
    }

    public static MROutput enhancedMRProbablePrimeTest(BigInteger candidate, SecureRandom random, int iterations) {
        BigInteger bigInteger = candidate;
        SecureRandom secureRandom = random;
        int i = iterations;
        checkCandidate(bigInteger, "candidate");
        if (secureRandom != null) {
            int i2 = 1;
            if (i < 1) {
                throw new IllegalArgumentException("'iterations' must be > 0");
            } else if (candidate.bitLength() == 2) {
                return MROutput.probablyPrime();
            } else {
                int i3 = 0;
                if (!bigInteger.testBit(0)) {
                    return MROutput.provablyCompositeWithFactor(TWO);
                }
                BigInteger w = bigInteger;
                BigInteger wSubOne = bigInteger.subtract(ONE);
                BigInteger wSubTwo = bigInteger.subtract(TWO);
                int a = wSubOne.getLowestSetBit();
                BigInteger m = wSubOne.shiftRight(a);
                while (i3 < i) {
                    BigInteger b = BigIntegers.createRandomInRange(TWO, wSubTwo, secureRandom);
                    BigInteger g = b.gcd(w);
                    if (g.compareTo(ONE) > 0) {
                        return MROutput.provablyCompositeWithFactor(g);
                    }
                    BigInteger z = b.modPow(m, w);
                    if (!(z.equals(ONE) || z.equals(wSubOne))) {
                        boolean primeToBase = false;
                        BigInteger z2 = z;
                        BigInteger x = z2;
                        int j = i2;
                        while (j < a) {
                            z2 = z2.modPow(TWO, w);
                            if (z2.equals(wSubOne)) {
                                primeToBase = true;
                                break;
                            } else if (z2.equals(ONE)) {
                                break;
                            } else {
                                x = z2;
                                j++;
                            }
                        }
                        if (!primeToBase) {
                            if (!z2.equals(ONE)) {
                                x = z2;
                                z2 = z2.modPow(TWO, w);
                                if (!z2.equals(ONE)) {
                                    x = z2;
                                }
                            }
                            BigInteger g2 = x.subtract(ONE).gcd(w);
                            if (g2.compareTo(ONE) > 0) {
                                return MROutput.provablyCompositeWithFactor(g2);
                            }
                            return MROutput.provablyCompositeNotPrimePower();
                        }
                    }
                    i3++;
                    i2 = 1;
                }
                return MROutput.probablyPrime();
            }
        }
        throw new IllegalArgumentException("'random' cannot be null");
    }

    public static boolean hasAnySmallFactors(BigInteger candidate) {
        checkCandidate(candidate, "candidate");
        return implHasAnySmallFactors(candidate);
    }

    public static boolean isMRProbablePrime(BigInteger candidate, SecureRandom random, int iterations) {
        checkCandidate(candidate, "candidate");
        if (random == null) {
            throw new IllegalArgumentException("'random' cannot be null");
        } else if (iterations < 1) {
            throw new IllegalArgumentException("'iterations' must be > 0");
        } else if (candidate.bitLength() == 2) {
            return true;
        } else {
            if (!candidate.testBit(0)) {
                return false;
            }
            BigInteger w = candidate;
            BigInteger wSubOne = candidate.subtract(ONE);
            BigInteger wSubTwo = candidate.subtract(TWO);
            int a = wSubOne.getLowestSetBit();
            BigInteger m = wSubOne.shiftRight(a);
            for (int i = 0; i < iterations; i++) {
                if (!implMRProbablePrimeToBase(w, wSubOne, m, a, BigIntegers.createRandomInRange(TWO, wSubTwo, random))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static boolean isMRProbablePrimeToBase(BigInteger candidate, BigInteger base) {
        checkCandidate(candidate, "candidate");
        checkCandidate(base, "base");
        if (base.compareTo(candidate.subtract(ONE)) >= 0) {
            throw new IllegalArgumentException("'base' must be < ('candidate' - 1)");
        } else if (candidate.bitLength() == 2) {
            return true;
        } else {
            BigInteger w = candidate;
            BigInteger wSubOne = candidate.subtract(ONE);
            int a = wSubOne.getLowestSetBit();
            return implMRProbablePrimeToBase(w, wSubOne, wSubOne.shiftRight(a), a, base);
        }
    }

    private static void checkCandidate(BigInteger n, String name) {
        if (n == null || n.signum() < 1 || n.bitLength() < 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append(name);
            stringBuilder.append("' must be non-null and >= 2");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static boolean implHasAnySmallFactors(BigInteger x) {
        int r = x.mod(BigInteger.valueOf((long) 223092870)).intValue();
        if (r % 2 == 0 || r % 3 == 0 || r % 5 == 0 || r % 7 == 0 || r % 11 == 0 || r % 13 == 0 || r % 17 == 0 || r % 19 == 0 || r % 23 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 58642669)).intValue();
        if (r % 29 == 0 || r % 31 == 0 || r % 37 == 0 || r % 41 == 0 || r % 43 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 600662303)).intValue();
        if (r % 47 == 0 || r % 53 == 0 || r % 59 == 0 || r % 61 == 0 || r % 67 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 33984931)).intValue();
        if (r % 71 == 0 || r % 73 == 0 || r % 79 == 0 || r % 83 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 89809099)).intValue();
        if (r % 89 == 0 || r % 97 == 0 || r % 101 == 0 || r % 103 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 167375713)).intValue();
        if (r % 107 == 0 || r % 109 == 0 || r % 113 == 0 || r % 127 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 371700317)).intValue();
        if (r % 131 == 0 || r % 137 == 0 || r % 139 == 0 || r % 149 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 645328247)).intValue();
        if (r % 151 == 0 || r % 157 == 0 || r % 163 == 0 || r % 167 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 1070560157)).intValue();
        if (r % 173 == 0 || r % 179 == 0 || r % 181 == 0 || r % 191 == 0) {
            return true;
        }
        r = x.mod(BigInteger.valueOf((long) 1596463769)).intValue();
        if (r % 193 == 0 || r % 197 == 0 || r % 199 == 0 || r % SMALL_FACTOR_LIMIT == 0) {
            return true;
        }
        return false;
    }

    private static boolean implMRProbablePrimeToBase(BigInteger w, BigInteger wSubOne, BigInteger m, int a, BigInteger b) {
        BigInteger z = b.modPow(m, w);
        int j = 1;
        if (z.equals(ONE) || z.equals(wSubOne)) {
            return true;
        }
        boolean result = false;
        while (j < a) {
            z = z.modPow(TWO, w);
            if (z.equals(wSubOne)) {
                result = true;
                break;
            } else if (z.equals(ONE)) {
                return false;
            } else {
                j++;
            }
        }
        return result;
    }

    private static STOutput implSTRandomPrime(Digest d, int length, byte[] primeSeed) {
        Digest digest = d;
        int i = length;
        byte[] bArr = primeSeed;
        int dLen = d.getDigestSize();
        if (i < 33) {
            int primeGenCounter = 0;
            byte[] c0 = new byte[dLen];
            byte[] c1 = new byte[dLen];
            while (true) {
                hash(digest, bArr, c0, 0);
                inc(bArr, 1);
                hash(digest, bArr, c1, 0);
                inc(bArr, 1);
                primeGenCounter++;
                long c64 = ((long) (((extract32(c0) ^ extract32(c1)) & (-1 >>> (32 - i))) | ((1 << (i - 1)) | 1))) & 4294967295L;
                if (isPrime32(c64)) {
                    return new STOutput(BigInteger.valueOf(c64), bArr, primeGenCounter);
                }
                if (primeGenCounter > 4 * i) {
                    throw new IllegalStateException("Too many iterations in Shawe-Taylor Random_Prime Routine");
                }
            }
        } else {
            STOutput rec = implSTRandomPrime(digest, (i + 3) / 2, bArr);
            BigInteger c02 = rec.getPrime();
            bArr = rec.getPrimeSeed();
            int primeGenCounter2 = rec.getPrimeGenCounter();
            int iterations = (i - 1) / (8 * dLen);
            int oldCounter = primeGenCounter2;
            BigInteger x = hashGen(digest, bArr, iterations + 1).mod(ONE.shiftLeft(i - 1)).setBit(i - 1);
            BigInteger c0x2 = c02.shiftLeft(1);
            BigInteger tx2 = x.subtract(ONE).divide(c0x2).add(ONE).shiftLeft(1);
            int dt = 0;
            BigInteger c = tx2.multiply(c02).add(ONE);
            while (true) {
                int dLen2;
                STOutput rec2;
                if (c.bitLength() > i) {
                    dLen2 = dLen;
                    tx2 = ONE.shiftLeft(i - 1).subtract(ONE).divide(c0x2).add(ONE).shiftLeft(1);
                    c = tx2.multiply(c02).add(ONE);
                } else {
                    dLen2 = dLen;
                }
                primeGenCounter2++;
                if (implHasAnySmallFactors(c)) {
                    rec2 = rec;
                    inc(bArr, iterations + 1);
                } else {
                    BigInteger a = hashGen(digest, bArr, iterations + 1).mod(c.subtract(THREE)).add(TWO);
                    rec2 = rec;
                    BigInteger tx22 = tx2.add(BigInteger.valueOf((long) dt));
                    dt = 0;
                    tx2 = a.modPow(tx22, c);
                    if (c.gcd(tx2.subtract(ONE)).equals(ONE) && tx2.modPow(c02, c).equals(ONE)) {
                        return new STOutput(c, bArr, primeGenCounter2);
                    }
                    tx2 = tx22;
                }
                if (primeGenCounter2 < (4 * i) + oldCounter) {
                    dt += 2;
                    c = c.add(c0x2);
                    dLen = dLen2;
                    rec = rec2;
                    digest = d;
                } else {
                    throw new IllegalStateException("Too many iterations in Shawe-Taylor Random_Prime Routine");
                }
            }
        }
    }

    private static int extract32(byte[] bs) {
        int result = 0;
        for (int i = 0; i < Math.min(4, bs.length); i++) {
            result |= (bs[bs.length - (i + 1)] & 255) << (8 * i);
        }
        return result;
    }

    private static void hash(Digest d, byte[] input, byte[] output, int outPos) {
        d.update(input, 0, input.length);
        d.doFinal(output, outPos);
    }

    private static BigInteger hashGen(Digest d, byte[] seed, int count) {
        int dLen = d.getDigestSize();
        int pos = count * dLen;
        byte[] buf = new byte[pos];
        for (int i = 0; i < count; i++) {
            pos -= dLen;
            hash(d, seed, buf, pos);
            inc(seed, 1);
        }
        return new BigInteger(1, buf);
    }

    private static void inc(byte[] seed, int c) {
        int pos = seed.length;
        while (c > 0) {
            pos--;
            if (pos >= 0) {
                c += seed[pos] & 255;
                seed[pos] = (byte) c;
                c >>>= 8;
            } else {
                return;
            }
        }
    }

    private static boolean isPrime32(long x) {
        if ((x >>> 32) == 0) {
            boolean z = false;
            if (x <= 5) {
                if (x == 2 || x == 3 || x == 5) {
                    z = true;
                }
                return z;
            } else if ((1 & x) == 0 || x % 3 == 0 || x % 5 == 0) {
                return false;
            } else {
                long[] ds = new long[]{1, 7, 11, 13, 17, 19, 23, 29};
                long base = 0;
                int pos = 1;
                while (true) {
                    if (pos >= ds.length) {
                        base += 30;
                        if (base * base >= x) {
                            return true;
                        }
                        pos = 0;
                    } else if (x % (ds[pos] + base) == 0) {
                        if (x < 30) {
                            z = true;
                        }
                        return z;
                    } else {
                        pos++;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Size limit exceeded");
        }
    }
}
