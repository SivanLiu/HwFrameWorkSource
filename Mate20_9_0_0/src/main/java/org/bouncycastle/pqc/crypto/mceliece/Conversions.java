package org.bouncycastle.pqc.crypto.mceliece;

import java.math.BigInteger;
import org.bouncycastle.pqc.math.linearalgebra.BigIntUtils;
import org.bouncycastle.pqc.math.linearalgebra.GF2Vector;
import org.bouncycastle.pqc.math.linearalgebra.IntegerFunctions;

final class Conversions {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger ZERO = BigInteger.valueOf(0);

    private Conversions() {
    }

    public static byte[] decode(int i, int i2, GF2Vector gF2Vector) {
        if (gF2Vector.getLength() == i && gF2Vector.getHammingWeight() == i2) {
            int[] vecArray = gF2Vector.getVecArray();
            BigInteger binomial = IntegerFunctions.binomial(i, i2);
            BigInteger bigInteger = ZERO;
            int i3 = i2;
            i2 = i;
            for (int i4 = 0; i4 < i; i4++) {
                binomial = binomial.multiply(BigInteger.valueOf((long) (i2 - i3))).divide(BigInteger.valueOf((long) i2));
                i2--;
                if ((vecArray[i4 >> 5] & (1 << (i4 & 31))) != 0) {
                    bigInteger = bigInteger.add(binomial);
                    i3--;
                    binomial = i2 == i3 ? ONE : binomial.multiply(BigInteger.valueOf((long) (i3 + 1))).divide(BigInteger.valueOf((long) (i2 - i3)));
                }
            }
            return BigIntUtils.toMinimalByteArray(bigInteger);
        }
        throw new IllegalArgumentException("vector has wrong length or hamming weight");
    }

    public static GF2Vector encode(int i, int i2, byte[] bArr) {
        if (i >= i2) {
            BigInteger binomial = IntegerFunctions.binomial(i, i2);
            BigInteger bigInteger = new BigInteger(1, bArr);
            if (bigInteger.compareTo(binomial) < 0) {
                GF2Vector gF2Vector = new GF2Vector(i);
                BigInteger bigInteger2 = bigInteger;
                int i3 = i2;
                i2 = i;
                for (int i4 = 0; i4 < i; i4++) {
                    binomial = binomial.multiply(BigInteger.valueOf((long) (i2 - i3))).divide(BigInteger.valueOf((long) i2));
                    i2--;
                    if (binomial.compareTo(bigInteger2) <= 0) {
                        gF2Vector.setBit(i4);
                        bigInteger2 = bigInteger2.subtract(binomial);
                        i3--;
                        binomial = i2 == i3 ? ONE : binomial.multiply(BigInteger.valueOf((long) (i3 + 1))).divide(BigInteger.valueOf((long) (i2 - i3)));
                    }
                }
                return gF2Vector;
            }
            throw new IllegalArgumentException("Encoded number too large.");
        }
        throw new IllegalArgumentException("n < t");
    }

    public static byte[] signConversion(int i, int i2, byte[] bArr) {
        if (i >= i2) {
            int length;
            BigInteger binomial = IntegerFunctions.binomial(i, i2);
            int bitLength = binomial.bitLength() - 1;
            int i3 = bitLength >> 3;
            bitLength &= 7;
            int i4 = 8;
            if (bitLength == 0) {
                i3--;
                bitLength = 8;
            }
            int i5 = i >> 3;
            int i6 = i & 7;
            if (i6 == 0) {
                i5--;
            } else {
                i4 = i6;
            }
            Object obj = new byte[(i5 + 1)];
            if (bArr.length < obj.length) {
                System.arraycopy(bArr, 0, obj, 0, bArr.length);
                for (length = bArr.length; length < obj.length; length++) {
                    obj[length] = null;
                }
            } else {
                System.arraycopy(bArr, 0, obj, 0, i5);
                obj[i5] = (byte) (bArr[i5] & ((1 << i4) - 1));
            }
            i4 = i2;
            BigInteger bigInteger = ZERO;
            length = i;
            for (i2 = 0; i2 < i; i2++) {
                binomial = binomial.multiply(new BigInteger(Integer.toString(length - i4))).divide(new BigInteger(Integer.toString(length)));
                length--;
                if (((byte) (obj[i2 >>> 3] & (1 << (i2 & 7)))) != (byte) 0) {
                    bigInteger = bigInteger.add(binomial);
                    i4--;
                    binomial = length == i4 ? ONE : binomial.multiply(new BigInteger(Integer.toString(i4 + 1))).divide(new BigInteger(Integer.toString(length - i4)));
                }
            }
            Object obj2 = new byte[(i3 + 1)];
            Object toByteArray = bigInteger.toByteArray();
            if (toByteArray.length < obj2.length) {
                System.arraycopy(toByteArray, 0, obj2, 0, toByteArray.length);
                for (i2 = toByteArray.length; i2 < obj2.length; i2++) {
                    obj2[i2] = null;
                }
            } else {
                System.arraycopy(toByteArray, 0, obj2, 0, i3);
                obj2[i3] = (byte) (toByteArray[i3] & ((1 << bitLength) - 1));
            }
            return obj2;
        }
        throw new IllegalArgumentException("n < t");
    }
}
