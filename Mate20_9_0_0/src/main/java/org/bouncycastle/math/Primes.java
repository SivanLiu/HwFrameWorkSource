package org.bouncycastle.math;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.ExtensionType;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;

public abstract class Primes {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    public static final int SMALL_FACTOR_LIMIT = 211;
    private static final BigInteger THREE = BigInteger.valueOf(3);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    public static class MROutput {
        private BigInteger factor;
        private boolean provablyComposite;

        private MROutput(boolean z, BigInteger bigInteger) {
            this.provablyComposite = z;
            this.factor = bigInteger;
        }

        private static MROutput probablyPrime() {
            return new MROutput(false, null);
        }

        private static MROutput provablyCompositeNotPrimePower() {
            return new MROutput(true, null);
        }

        private static MROutput provablyCompositeWithFactor(BigInteger bigInteger) {
            return new MROutput(true, bigInteger);
        }

        public BigInteger getFactor() {
            return this.factor;
        }

        public boolean isNotPrimePower() {
            return this.provablyComposite && this.factor == null;
        }

        public boolean isProvablyComposite() {
            return this.provablyComposite;
        }
    }

    public static class STOutput {
        private BigInteger prime;
        private int primeGenCounter;
        private byte[] primeSeed;

        private STOutput(BigInteger bigInteger, byte[] bArr, int i) {
            this.prime = bigInteger;
            this.primeSeed = bArr;
            this.primeGenCounter = i;
        }

        public BigInteger getPrime() {
            return this.prime;
        }

        public int getPrimeGenCounter() {
            return this.primeGenCounter;
        }

        public byte[] getPrimeSeed() {
            return this.primeSeed;
        }
    }

    private static void checkCandidate(BigInteger bigInteger, String str) {
        if (bigInteger == null || bigInteger.signum() < 1 || bigInteger.bitLength() < 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append(str);
            stringBuilder.append("' must be non-null and >= 2");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static MROutput enhancedMRProbablePrimeTest(BigInteger bigInteger, SecureRandom secureRandom, int i) {
        checkCandidate(bigInteger, "candidate");
        if (secureRandom == null) {
            throw new IllegalArgumentException("'random' cannot be null");
        } else if (i < 1) {
            throw new IllegalArgumentException("'iterations' must be > 0");
        } else if (bigInteger.bitLength() == 2) {
            return MROutput.probablyPrime();
        } else {
            if (!bigInteger.testBit(0)) {
                return MROutput.provablyCompositeWithFactor(TWO);
            }
            BigInteger subtract = bigInteger.subtract(ONE);
            BigInteger subtract2 = bigInteger.subtract(TWO);
            int lowestSetBit = subtract.getLowestSetBit();
            BigInteger shiftRight = subtract.shiftRight(lowestSetBit);
            for (int i2 = 0; i2 < i; i2++) {
                BigInteger createRandomInRange = BigIntegers.createRandomInRange(TWO, subtract2, secureRandom);
                BigInteger gcd = createRandomInRange.gcd(bigInteger);
                if (gcd.compareTo(ONE) > 0) {
                    return MROutput.provablyCompositeWithFactor(gcd);
                }
                createRandomInRange = createRandomInRange.modPow(shiftRight, bigInteger);
                if (!(createRandomInRange.equals(ONE) || createRandomInRange.equals(subtract))) {
                    BigInteger modPow;
                    gcd = createRandomInRange;
                    int i3 = 1;
                    while (i3 < lowestSetBit) {
                        modPow = gcd.modPow(TWO, bigInteger);
                        if (modPow.equals(subtract)) {
                            i3 = 1;
                            break;
                        } else if (modPow.equals(ONE)) {
                            i3 = 0;
                            break;
                        } else {
                            i3++;
                            gcd = modPow;
                        }
                    }
                    i3 = 0;
                    modPow = gcd;
                    if (i3 == 0) {
                        if (!modPow.equals(ONE)) {
                            gcd = modPow.modPow(TWO, bigInteger);
                            if (gcd.equals(ONE)) {
                                gcd = modPow;
                            }
                        }
                        bigInteger = gcd.subtract(ONE).gcd(bigInteger);
                        return bigInteger.compareTo(ONE) > 0 ? MROutput.provablyCompositeWithFactor(bigInteger) : MROutput.provablyCompositeNotPrimePower();
                    }
                }
            }
            return MROutput.probablyPrime();
        }
    }

    private static int extract32(byte[] bArr) {
        int i = 0;
        int i2 = 0;
        while (i < Math.min(4, bArr.length)) {
            int i3 = i + 1;
            i2 |= (bArr[bArr.length - i3] & 255) << (8 * i);
            i = i3;
        }
        return i2;
    }

    public static STOutput generateSTRandomPrime(Digest digest, int i, byte[] bArr) {
        if (digest == null) {
            throw new IllegalArgumentException("'hash' cannot be null");
        } else if (i < 2) {
            throw new IllegalArgumentException("'length' must be >= 2");
        } else if (bArr != null && bArr.length != 0) {
            return implSTRandomPrime(digest, i, Arrays.clone(bArr));
        } else {
            throw new IllegalArgumentException("'inputSeed' cannot be null or empty");
        }
    }

    public static boolean hasAnySmallFactors(BigInteger bigInteger) {
        checkCandidate(bigInteger, "candidate");
        return implHasAnySmallFactors(bigInteger);
    }

    private static void hash(Digest digest, byte[] bArr, byte[] bArr2, int i) {
        digest.update(bArr, 0, bArr.length);
        digest.doFinal(bArr2, i);
    }

    private static BigInteger hashGen(Digest digest, byte[] bArr, int i) {
        int digestSize = digest.getDigestSize();
        int i2 = i * digestSize;
        byte[] bArr2 = new byte[i2];
        for (int i3 = 0; i3 < i; i3++) {
            i2 -= digestSize;
            hash(digest, bArr, bArr2, i2);
            inc(bArr, 1);
        }
        return new BigInteger(1, bArr2);
    }

    /* JADX WARNING: Missing block: B:106:0x0169, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean implHasAnySmallFactors(BigInteger bigInteger) {
        int intValue = bigInteger.mod(BigInteger.valueOf((long) 223092870)).intValue();
        if (intValue % 2 == 0 || intValue % 3 == 0 || intValue % 5 == 0 || intValue % 7 == 0 || intValue % 11 == 0 || intValue % 13 == 0 || intValue % 17 == 0 || intValue % 19 == 0 || intValue % 23 == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 58642669)).intValue();
        if (intValue % 29 == 0 || intValue % 31 == 0 || intValue % 37 == 0 || intValue % 41 == 0 || intValue % 43 == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 600662303)).intValue();
        if (intValue % 47 == 0 || intValue % 53 == 0 || intValue % 59 == 0 || intValue % 61 == 0 || intValue % 67 == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 33984931)).intValue();
        if (intValue % 71 == 0 || intValue % 73 == 0 || intValue % 79 == 0 || intValue % 83 == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 89809099)).intValue();
        if (intValue % 89 == 0 || intValue % 97 == 0 || intValue % ExtensionType.negotiated_ff_dhe_groups == 0 || intValue % 103 == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 167375713)).intValue();
        if (intValue % CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 == 0 || intValue % CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256 == 0 || intValue % 113 == 0 || intValue % CertificateBody.profileType == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 371700317)).intValue();
        if (intValue % 131 == 0 || intValue % CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA == 0 || intValue % CipherSuite.TLS_PSK_WITH_3DES_EDE_CBC_SHA == 0 || intValue % CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 645328247)).intValue();
        if (intValue % CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA == 0 || intValue % CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384 == 0 || intValue % CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 == 0 || intValue % CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384 == 0) {
            return true;
        }
        intValue = bigInteger.mod(BigInteger.valueOf((long) 1070560157)).intValue();
        if (intValue % CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384 == 0 || intValue % CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384 == 0 || intValue % CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384 == 0 || intValue % CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256 == 0) {
            return true;
        }
        int intValue2 = bigInteger.mod(BigInteger.valueOf((long) 1596463769)).intValue();
        return intValue2 % CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256 == 0 || intValue2 % CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256 == 0 || intValue2 % 199 == 0 || intValue2 % SMALL_FACTOR_LIMIT == 0;
    }

    private static boolean implMRProbablePrimeToBase(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, int i, BigInteger bigInteger4) {
        bigInteger3 = bigInteger4.modPow(bigInteger3, bigInteger);
        boolean z = true;
        if (!bigInteger3.equals(ONE)) {
            if (bigInteger3.equals(bigInteger2)) {
                return true;
            }
            bigInteger4 = bigInteger3;
            for (int i2 = 1; i2 < i; i2++) {
                bigInteger4 = bigInteger4.modPow(TWO, bigInteger);
                if (bigInteger4.equals(bigInteger2)) {
                    return true;
                }
                if (bigInteger4.equals(ONE)) {
                    return false;
                }
            }
            z = false;
        }
        return z;
    }

    private static STOutput implSTRandomPrime(Digest digest, int i, byte[] bArr) {
        Digest digest2 = digest;
        int i2 = i;
        byte[] bArr2 = bArr;
        int digestSize = digest.getDigestSize();
        int i3 = 1;
        if (i2 < 33) {
            byte[] bArr3 = new byte[digestSize];
            byte[] bArr4 = new byte[digestSize];
            int i4 = 0;
            while (true) {
                hash(digest2, bArr2, bArr3, 0);
                inc(bArr2, 1);
                hash(digest2, bArr2, bArr4, 0);
                inc(bArr2, 1);
                i4++;
                long extract32 = ((long) (((extract32(bArr3) ^ extract32(bArr4)) & (-1 >>> (32 - i2))) | ((1 << (i2 - 1)) | 1))) & BodyPartID.bodyIdMax;
                if (isPrime32(extract32)) {
                    return new STOutput(BigInteger.valueOf(extract32), bArr2, i4);
                }
                if (i4 > 4 * i2) {
                    throw new IllegalStateException("Too many iterations in Shawe-Taylor Random_Prime Routine");
                }
            }
        } else {
            STOutput implSTRandomPrime = implSTRandomPrime(digest2, (i2 + 3) / 2, bArr2);
            BigInteger prime = implSTRandomPrime.getPrime();
            byte[] primeSeed = implSTRandomPrime.getPrimeSeed();
            int primeGenCounter = implSTRandomPrime.getPrimeGenCounter();
            int i5 = 8 * digestSize;
            digestSize = i2 - 1;
            i5 = (digestSize / i5) + 1;
            BigInteger bit = hashGen(digest2, primeSeed, i5).mod(ONE.shiftLeft(digestSize)).setBit(digestSize);
            BigInteger shiftLeft = prime.shiftLeft(1);
            bit = bit.subtract(ONE).divide(shiftLeft).add(ONE).shiftLeft(1);
            BigInteger add = bit.multiply(prime).add(ONE);
            int i6 = 0;
            BigInteger bigInteger = bit;
            int i7 = primeGenCounter;
            while (true) {
                int i8;
                if (add.bitLength() > i2) {
                    bigInteger = ONE.shiftLeft(digestSize).subtract(ONE).divide(shiftLeft).add(ONE).shiftLeft(i3);
                    add = bigInteger.multiply(prime).add(ONE);
                }
                i7 += i3;
                if (implHasAnySmallFactors(add)) {
                    inc(primeSeed, i5);
                    i8 = 4;
                } else {
                    BigInteger add2 = hashGen(digest2, primeSeed, i5).mod(add.subtract(THREE)).add(TWO);
                    BigInteger add3 = bigInteger.add(BigInteger.valueOf((long) i6));
                    BigInteger modPow = add2.modPow(add3, add);
                    if (add.gcd(modPow.subtract(ONE)).equals(ONE) && modPow.modPow(prime, add).equals(ONE)) {
                        return new STOutput(add, primeSeed, i7);
                    }
                    bigInteger = add3;
                    i8 = 4;
                    i6 = 0;
                }
                if (i7 < (i8 * i2) + primeGenCounter) {
                    i6 += 2;
                    add = add.add(shiftLeft);
                    i3 = 1;
                } else {
                    throw new IllegalStateException("Too many iterations in Shawe-Taylor Random_Prime Routine");
                }
            }
        }
    }

    private static void inc(byte[] bArr, int i) {
        int length = bArr.length;
        while (i > 0) {
            length--;
            if (length >= 0) {
                i += bArr[length] & 255;
                bArr[length] = (byte) i;
                i >>>= 8;
            } else {
                return;
            }
        }
    }

    public static boolean isMRProbablePrime(BigInteger bigInteger, SecureRandom secureRandom, int i) {
        checkCandidate(bigInteger, "candidate");
        if (secureRandom == null) {
            throw new IllegalArgumentException("'random' cannot be null");
        } else if (i < 1) {
            throw new IllegalArgumentException("'iterations' must be > 0");
        } else if (bigInteger.bitLength() == 2) {
            return true;
        } else {
            if (!bigInteger.testBit(0)) {
                return false;
            }
            BigInteger subtract = bigInteger.subtract(ONE);
            BigInteger subtract2 = bigInteger.subtract(TWO);
            int lowestSetBit = subtract.getLowestSetBit();
            BigInteger shiftRight = subtract.shiftRight(lowestSetBit);
            for (int i2 = 0; i2 < i; i2++) {
                if (!implMRProbablePrimeToBase(bigInteger, subtract, shiftRight, lowestSetBit, BigIntegers.createRandomInRange(TWO, subtract2, secureRandom))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static boolean isMRProbablePrimeToBase(BigInteger bigInteger, BigInteger bigInteger2) {
        checkCandidate(bigInteger, "candidate");
        checkCandidate(bigInteger2, "base");
        if (bigInteger2.compareTo(bigInteger.subtract(ONE)) >= 0) {
            throw new IllegalArgumentException("'base' must be < ('candidate' - 1)");
        } else if (bigInteger.bitLength() == 2) {
            return true;
        } else {
            BigInteger subtract = bigInteger.subtract(ONE);
            int lowestSetBit = subtract.getLowestSetBit();
            return implMRProbablePrimeToBase(bigInteger, subtract, subtract.shiftRight(lowestSetBit), lowestSetBit, bigInteger2);
        }
    }

    /* JADX WARNING: Missing block: B:32:0x0060, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isPrime32(long j) {
        if ((j >>> 32) == 0) {
            int i = (j > 5 ? 1 : (j == 5 ? 0 : -1));
            boolean z = false;
            if (i <= 0) {
                if (j == 2 || j == 3 || i == 0) {
                    z = true;
                }
                return z;
            } else if ((1 & j) == 0 || j % 3 == 0 || j % 5 == 0) {
                return false;
            } else {
                long[] jArr = new long[]{1, 7, 11, 13, 17, 19, 23, 29};
                long j2 = 0;
                int i2 = 1;
                while (true) {
                    if (i2 >= jArr.length) {
                        j2 += 30;
                        if (j2 * j2 >= j) {
                            return true;
                        }
                        i2 = 0;
                    } else if (j % (jArr[i2] + j2) == 0) {
                        if (j < 30) {
                            z = true;
                        }
                        return z;
                    } else {
                        i2++;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Size limit exceeded");
        }
    }
}
