package org.bouncycastle.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class BigIntegers {
    private static final int MAX_ITERATIONS = 1000;
    private static final BigInteger ZERO = BigInteger.valueOf(0);

    public static byte[] asUnsignedByteArray(int i, BigInteger bigInteger) {
        Object toByteArray = bigInteger.toByteArray();
        if (toByteArray.length == i) {
            return toByteArray;
        }
        int i2 = 0;
        if (toByteArray[0] == (byte) 0) {
            i2 = 1;
        }
        int length = toByteArray.length - i2;
        if (length <= i) {
            Object obj = new byte[i];
            System.arraycopy(toByteArray, i2, obj, obj.length - length, length);
            return obj;
        }
        throw new IllegalArgumentException("standard length exceeded for value");
    }

    public static byte[] asUnsignedByteArray(BigInteger bigInteger) {
        Object toByteArray = bigInteger.toByteArray();
        if (toByteArray[0] != (byte) 0) {
            return toByteArray;
        }
        Object obj = new byte[(toByteArray.length - 1)];
        System.arraycopy(toByteArray, 1, obj, 0, obj.length);
        return obj;
    }

    public static BigInteger createRandomInRange(BigInteger bigInteger, BigInteger bigInteger2, SecureRandom secureRandom) {
        int compareTo = bigInteger.compareTo(bigInteger2);
        if (compareTo >= 0) {
            if (compareTo <= 0) {
                return bigInteger;
            }
            throw new IllegalArgumentException("'min' may not be greater than 'max'");
        } else if (bigInteger.bitLength() > bigInteger2.bitLength() / 2) {
            return createRandomInRange(ZERO, bigInteger2.subtract(bigInteger), secureRandom).add(bigInteger);
        } else {
            for (compareTo = 0; compareTo < MAX_ITERATIONS; compareTo++) {
                BigInteger bigInteger3 = new BigInteger(bigInteger2.bitLength(), secureRandom);
                if (bigInteger3.compareTo(bigInteger) >= 0 && bigInteger3.compareTo(bigInteger2) <= 0) {
                    return bigInteger3;
                }
            }
            return new BigInteger(bigInteger2.subtract(bigInteger).bitLength() - 1, secureRandom).add(bigInteger);
        }
    }

    public static BigInteger fromUnsignedByteArray(byte[] bArr) {
        return new BigInteger(1, bArr);
    }

    public static BigInteger fromUnsignedByteArray(byte[] bArr, int i, int i2) {
        if (!(i == 0 && i2 == bArr.length)) {
            Object obj = new byte[i2];
            System.arraycopy(bArr, i, obj, 0, i2);
            bArr = obj;
        }
        return new BigInteger(1, bArr);
    }
}
