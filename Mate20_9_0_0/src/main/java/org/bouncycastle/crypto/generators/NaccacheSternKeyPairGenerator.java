package org.bouncycastle.crypto.generators;

import java.io.PrintStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Vector;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.NaccacheSternKeyGenerationParameters;
import org.bouncycastle.crypto.params.NaccacheSternKeyParameters;
import org.bouncycastle.crypto.params.NaccacheSternPrivateKeyParameters;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.ExtensionType;
import org.bouncycastle.math.Primes;

public class NaccacheSternKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static int[] smallPrimes = new int[]{3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, ExtensionType.negotiated_ff_dhe_groups, 103, CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256, 113, CertificateBody.profileType, 131, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA, CipherSuite.TLS_PSK_WITH_3DES_EDE_CBC_SHA, CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA, CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384, CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256, CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256, 199, Primes.SMALL_FACTOR_LIMIT, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557};
    private NaccacheSternKeyGenerationParameters param;

    private static Vector findFirstPrimes(int i) {
        Vector vector = new Vector(i);
        for (int i2 = 0; i2 != i; i2++) {
            vector.addElement(BigInteger.valueOf((long) smallPrimes[i2]));
        }
        return vector;
    }

    private static BigInteger generatePrime(int i, int i2, SecureRandom secureRandom) {
        BigInteger bigInteger = new BigInteger(i, i2, secureRandom);
        while (bigInteger.bitLength() != i) {
            bigInteger = new BigInteger(i, i2, secureRandom);
        }
        return bigInteger;
    }

    private static int getInt(SecureRandom secureRandom, int i) {
        if (((-i) & i) == i) {
            return (int) ((((long) i) * ((long) (secureRandom.nextInt() & Integer.MAX_VALUE))) >> 31);
        }
        int i2;
        int nextInt;
        do {
            nextInt = secureRandom.nextInt() & Integer.MAX_VALUE;
            i2 = nextInt % i;
        } while ((nextInt - i2) + (i - 1) < 0);
        return i2;
    }

    private static Vector permuteList(Vector vector, SecureRandom secureRandom) {
        Vector vector2 = new Vector();
        Vector vector3 = new Vector();
        for (int i = 0; i < vector.size(); i++) {
            vector3.addElement(vector.elementAt(i));
        }
        vector2.addElement(vector3.elementAt(0));
        while (true) {
            vector3.removeElementAt(0);
            if (vector3.size() == 0) {
                return vector2;
            }
            vector2.insertElementAt(vector3.elementAt(0), getInt(secureRandom, vector2.size() + 1));
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x023d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AsymmetricCipherKeyPair generateKeyPair() {
        int i;
        BigInteger generatePrime;
        BigInteger add;
        BigInteger generatePrime2;
        int i2;
        PrintStream printStream;
        StringBuilder stringBuilder;
        BigInteger bigInteger;
        BigInteger bigInteger2;
        BigInteger bigInteger3;
        long j;
        BigInteger bigInteger4;
        PrintStream printStream2;
        BigInteger bigInteger5;
        PrintStream printStream3;
        StringBuilder stringBuilder2;
        int strength = this.param.getStrength();
        Random random = this.param.getRandom();
        int certainty = this.param.getCertainty();
        boolean isDebug = this.param.isDebug();
        if (isDebug) {
            PrintStream printStream4 = System.out;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Fetching first ");
            stringBuilder3.append(this.param.getCntSmallPrimes());
            stringBuilder3.append(" primes.");
            printStream4.println(stringBuilder3.toString());
        }
        Vector permuteList = permuteList(findFirstPrimes(this.param.getCntSmallPrimes()), random);
        BigInteger bigInteger6 = ONE;
        BigInteger bigInteger7 = ONE;
        BigInteger bigInteger8 = bigInteger6;
        for (i = 0; i < permuteList.size() / 2; i++) {
            bigInteger8 = bigInteger8.multiply((BigInteger) permuteList.elementAt(i));
        }
        for (i = permuteList.size() / 2; i < permuteList.size(); i++) {
            bigInteger7 = bigInteger7.multiply((BigInteger) permuteList.elementAt(i));
        }
        bigInteger6 = bigInteger8.multiply(bigInteger7);
        int bitLength = (((strength - bigInteger6.bitLength()) - 48) / 2) + 1;
        BigInteger generatePrime3 = generatePrime(bitLength, certainty, random);
        BigInteger generatePrime4 = generatePrime(bitLength, certainty, random);
        if (isDebug) {
            System.out.println("generating p and q");
        }
        bigInteger8 = generatePrime3.multiply(bigInteger8).shiftLeft(1);
        bigInteger7 = generatePrime4.multiply(bigInteger7).shiftLeft(1);
        long j2 = 0;
        while (true) {
            BigInteger bigInteger9;
            BigInteger bigInteger10;
            j2++;
            int i3 = 24;
            generatePrime = generatePrime(24, certainty, random);
            add = generatePrime.multiply(bigInteger8).add(ONE);
            if (add.isProbablePrime(certainty)) {
                while (true) {
                    generatePrime2 = generatePrime(i3, certainty, random);
                    if (!generatePrime.equals(generatePrime2)) {
                        bigInteger9 = bigInteger7;
                        bigInteger7 = generatePrime2.multiply(bigInteger7).add(ONE);
                        if (bigInteger7.isProbablePrime(certainty)) {
                            break;
                        }
                        i2 = strength;
                        bigInteger7 = add;
                        bigInteger7 = bigInteger9;
                        i3 = 24;
                    }
                }
                bigInteger10 = bigInteger8;
                if (bigInteger6.gcd(generatePrime.multiply(generatePrime2)).equals(ONE)) {
                    if (add.multiply(bigInteger7).bitLength() >= strength) {
                        break;
                    } else if (isDebug) {
                        printStream = System.out;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("key size too small. Should be ");
                        stringBuilder.append(strength);
                        stringBuilder.append(" but is actually ");
                        stringBuilder.append(add.multiply(bigInteger7).bitLength());
                        printStream.println(stringBuilder.toString());
                    }
                } else {
                    continue;
                }
            } else {
                bigInteger9 = bigInteger7;
                bigInteger10 = bigInteger8;
            }
            bigInteger7 = bigInteger9;
            bigInteger8 = bigInteger10;
        }
        if (isDebug) {
            printStream = System.out;
            stringBuilder = new StringBuilder();
            bigInteger = generatePrime4;
            stringBuilder.append("needed ");
            stringBuilder.append(j2);
            stringBuilder.append(" tries to generate p and q.");
            printStream.println(stringBuilder.toString());
        } else {
            bigInteger = generatePrime4;
        }
        bigInteger8 = add.multiply(bigInteger7);
        BigInteger multiply = add.subtract(ONE).multiply(bigInteger7.subtract(ONE));
        if (isDebug) {
            System.out.println("generating g");
        }
        long j3 = 0;
        while (true) {
            Random random2;
            Object obj;
            Vector vector = new Vector();
            bigInteger2 = bigInteger7;
            bigInteger3 = add;
            j = j3;
            int i4 = 0;
            while (i4 != permuteList.size()) {
                long j4;
                BigInteger divide = multiply.divide((BigInteger) permuteList.elementAt(i4));
                while (true) {
                    j4 = j + 1;
                    bigInteger7 = new BigInteger(strength, certainty, random);
                    i2 = strength;
                    if (!bigInteger7.modPow(divide, bigInteger8).equals(ONE)) {
                        break;
                    }
                    j = j4;
                    strength = i2;
                }
                vector.addElement(bigInteger7);
                i4++;
                j = j4;
                strength = i2;
            }
            i2 = strength;
            bigInteger4 = ONE;
            strength = 0;
            while (strength < permuteList.size()) {
                random2 = random;
                bigInteger4 = bigInteger4.multiply(((BigInteger) vector.elementAt(strength)).modPow(bigInteger6.divide((BigInteger) permuteList.elementAt(strength)), bigInteger8)).mod(bigInteger8);
                strength++;
                random = random2;
            }
            random2 = random;
            strength = 0;
            while (strength < permuteList.size()) {
                if (bigInteger4.modPow(multiply.divide((BigInteger) permuteList.elementAt(strength)), bigInteger8).equals(ONE)) {
                    StringBuilder stringBuilder4;
                    if (isDebug) {
                        printStream2 = System.out;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("g has order phi(n)/");
                        stringBuilder4.append(permuteList.elementAt(strength));
                        stringBuilder4.append("\n g: ");
                        stringBuilder4.append(bigInteger4);
                        printStream2.println(stringBuilder4.toString());
                    }
                    obj = 1;
                    if (obj == null) {
                        String str;
                        if (!bigInteger4.modPow(multiply.divide(BigInteger.valueOf(4)), bigInteger8).equals(ONE)) {
                            if (!bigInteger4.modPow(multiply.divide(generatePrime), bigInteger8).equals(ONE)) {
                                if (!bigInteger4.modPow(multiply.divide(generatePrime2), bigInteger8).equals(ONE)) {
                                    if (!bigInteger4.modPow(multiply.divide(generatePrime3), bigInteger8).equals(ONE)) {
                                        bigInteger5 = bigInteger;
                                        if (!bigInteger4.modPow(multiply.divide(bigInteger5), bigInteger8).equals(ONE)) {
                                            break;
                                        }
                                        if (isDebug) {
                                            printStream2 = System.out;
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("g has order phi(n)/b\n g: ");
                                            stringBuilder4.append(bigInteger4);
                                            printStream2.println(stringBuilder4.toString());
                                        }
                                        bigInteger = bigInteger5;
                                        j3 = j;
                                        add = bigInteger3;
                                        bigInteger7 = bigInteger2;
                                        strength = i2;
                                        random = random2;
                                    } else if (isDebug) {
                                        printStream3 = System.out;
                                        stringBuilder2 = new StringBuilder();
                                        str = "g has order phi(n)/a\n g: ";
                                    }
                                } else if (isDebug) {
                                    printStream3 = System.out;
                                    stringBuilder2 = new StringBuilder();
                                    str = "g has order phi(n)/q'\n g: ";
                                }
                            } else if (isDebug) {
                                printStream3 = System.out;
                                stringBuilder2 = new StringBuilder();
                                str = "g has order phi(n)/p'\n g: ";
                            }
                        } else if (isDebug) {
                            printStream3 = System.out;
                            stringBuilder2 = new StringBuilder();
                            str = "g has order phi(n)/4\n g:";
                        }
                        stringBuilder2.append(str);
                        stringBuilder2.append(bigInteger4);
                        printStream3.println(stringBuilder2.toString());
                    }
                    bigInteger5 = bigInteger;
                    bigInteger = bigInteger5;
                    j3 = j;
                    add = bigInteger3;
                    bigInteger7 = bigInteger2;
                    strength = i2;
                    random = random2;
                } else {
                    strength++;
                }
            }
            obj = null;
            if (obj == null) {
            }
            bigInteger5 = bigInteger;
            bigInteger = bigInteger5;
            j3 = j;
            add = bigInteger3;
            bigInteger7 = bigInteger2;
            strength = i2;
            random = random2;
        }
        if (isDebug) {
            printStream2 = System.out;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("needed ");
            stringBuilder5.append(j);
            stringBuilder5.append(" tries to generate g");
            printStream2.println(stringBuilder5.toString());
            System.out.println();
            System.out.println("found new NaccacheStern cipher variables:");
            printStream2 = System.out;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append("smallPrimes: ");
            stringBuilder5.append(permuteList);
            printStream2.println(stringBuilder5.toString());
            printStream2 = System.out;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append("sigma:...... ");
            stringBuilder5.append(bigInteger6);
            stringBuilder5.append(" (");
            stringBuilder5.append(bigInteger6.bitLength());
            stringBuilder5.append(" bits)");
            printStream2.println(stringBuilder5.toString());
            printStream2 = System.out;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append("a:.......... ");
            stringBuilder5.append(generatePrime3);
            printStream2.println(stringBuilder5.toString());
            printStream2 = System.out;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append("b:.......... ");
            stringBuilder5.append(bigInteger5);
            printStream2.println(stringBuilder5.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("p':......... ");
            stringBuilder2.append(generatePrime);
            printStream3.println(stringBuilder2.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("q':......... ");
            stringBuilder2.append(generatePrime2);
            printStream3.println(stringBuilder2.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("p:.......... ");
            stringBuilder2.append(bigInteger3);
            printStream3.println(stringBuilder2.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("q:.......... ");
            stringBuilder2.append(bigInteger2);
            printStream3.println(stringBuilder2.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("n:.......... ");
            stringBuilder2.append(bigInteger8);
            printStream3.println(stringBuilder2.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("phi(n):..... ");
            stringBuilder2.append(multiply);
            printStream3.println(stringBuilder2.toString());
            printStream3 = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("g:.......... ");
            stringBuilder2.append(bigInteger4);
            printStream3.println(stringBuilder2.toString());
            System.out.println();
        }
        return new AsymmetricCipherKeyPair(new NaccacheSternKeyParameters(false, bigInteger4, bigInteger8, bigInteger6.bitLength()), new NaccacheSternPrivateKeyParameters(bigInteger4, bigInteger8, bigInteger6.bitLength(), permuteList, multiply));
    }

    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.param = (NaccacheSternKeyGenerationParameters) keyGenerationParameters;
    }
}
