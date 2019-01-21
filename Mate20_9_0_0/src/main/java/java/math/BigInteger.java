package java.math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

public class BigInteger extends Number implements Comparable<BigInteger>, Serializable {
    static final BigInteger MINUS_ONE = new BigInteger(-1, 1);
    public static final BigInteger ONE = new BigInteger(1, 1);
    static final BigInteger[] SMALL_VALUES = new BigInteger[]{ZERO, ONE, new BigInteger(1, 2), new BigInteger(1, 3), new BigInteger(1, 4), new BigInteger(1, 5), new BigInteger(1, 6), new BigInteger(1, 7), new BigInteger(1, 8), new BigInteger(1, 9), TEN};
    public static final BigInteger TEN = new BigInteger(1, 10);
    public static final BigInteger ZERO = new BigInteger(0, 0);
    private static final long serialVersionUID = -8287574255936472291L;
    private transient BigInt bigInt;
    transient int[] digits;
    private transient int firstNonzeroDigit = -2;
    private transient int hashCode = 0;
    private transient boolean javaIsValid = false;
    private byte[] magnitude;
    private transient boolean nativeIsValid = false;
    transient int numberLength;
    transient int sign;
    private int signum;

    BigInteger(BigInt bigInt) {
        if (bigInt == null || !bigInt.hasNativeBignum()) {
            throw new AssertionError();
        }
        setBigInt(bigInt);
    }

    BigInteger(int sign, long value) {
        boolean z = false;
        BigInt bigInt = new BigInt();
        if (sign < 0) {
            z = true;
        }
        bigInt.putULongInt(value, z);
        setBigInt(bigInt);
    }

    BigInteger(int sign, int numberLength, int[] digits) {
        setJavaRepresentation(sign, numberLength, digits);
    }

    public BigInteger(int numBits, Random random) {
        int i = 0;
        if (numBits >= 0) {
            if (numBits == 0) {
                setJavaRepresentation(0, 1, new int[]{0});
            } else {
                int numberLength = (numBits + 31) >> 5;
                int[] digits = new int[numberLength];
                while (i < numberLength) {
                    digits[i] = random.nextInt();
                    i++;
                }
                i = numberLength - 1;
                digits[i] = digits[i] >>> ((-numBits) & 31);
                setJavaRepresentation(1, numberLength, digits);
            }
            this.javaIsValid = true;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("numBits < 0: ");
        stringBuilder.append(numBits);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public BigInteger(int bitLength, int certainty, Random random) {
        if (bitLength < 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bitLength < 2: ");
            stringBuilder.append(bitLength);
            throw new ArithmeticException(stringBuilder.toString());
        } else if (bitLength < 16) {
            int candidate;
            do {
                candidate = (random.nextInt() & ((1 << bitLength) - 1)) | (1 << (bitLength - 1));
                if (bitLength > 2) {
                    candidate |= 1;
                }
            } while (!isSmallPrime(candidate));
            BigInt prime = new BigInt();
            prime.putULongInt((long) candidate, false);
            setBigInt(prime);
        } else {
            do {
                setBigInt(BigInt.generatePrimeDefault(bitLength));
            } while (bitLength() != bitLength);
        }
    }

    private static boolean isSmallPrime(int x) {
        if (x == 2) {
            return true;
        }
        if (x % 2 == 0) {
            return false;
        }
        int max = (int) Math.sqrt((double) x);
        for (int i = 3; i <= max; i += 2) {
            if (x % i == 0) {
                return false;
            }
        }
        return true;
    }

    public BigInteger(String value) {
        BigInt bigInt = new BigInt();
        bigInt.putDecString(value);
        setBigInt(bigInt);
    }

    public BigInteger(String value, int radix) {
        BigInt bigInt;
        if (value == null) {
            throw new NullPointerException("value == null");
        } else if (radix == 10) {
            bigInt = new BigInt();
            bigInt.putDecString(value);
            setBigInt(bigInt);
        } else if (radix == 16) {
            bigInt = new BigInt();
            bigInt.putHexString(value);
            setBigInt(bigInt);
        } else if (radix < 2 || radix > 36) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid radix: ");
            stringBuilder.append(radix);
            throw new NumberFormatException(stringBuilder.toString());
        } else if (value.isEmpty()) {
            throw new NumberFormatException("value.isEmpty()");
        } else {
            parseFromString(this, value, radix);
        }
    }

    public BigInteger(int signum, byte[] magnitude) {
        boolean z = false;
        if (magnitude == null) {
            throw new NullPointerException("magnitude == null");
        } else if (signum < -1 || signum > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid signum: ");
            stringBuilder.append(signum);
            throw new NumberFormatException(stringBuilder.toString());
        } else {
            if (signum == 0) {
                int length = magnitude.length;
                int i = 0;
                while (i < length) {
                    if (magnitude[i] == (byte) 0) {
                        i++;
                    } else {
                        throw new NumberFormatException("signum-magnitude mismatch");
                    }
                }
            }
            BigInt bigInt = new BigInt();
            if (signum < 0) {
                z = true;
            }
            bigInt.putBigEndian(magnitude, z);
            setBigInt(bigInt);
        }
    }

    public BigInteger(byte[] value) {
        if (value.length != 0) {
            BigInt bigInt = new BigInt();
            bigInt.putBigEndianTwosComplement(value);
            setBigInt(bigInt);
            return;
        }
        throw new NumberFormatException("value.length == 0");
    }

    BigInt getBigInt() {
        if (this.nativeIsValid) {
            return this.bigInt;
        }
        synchronized (this) {
            BigInt bigInt;
            if (this.nativeIsValid) {
                bigInt = this.bigInt;
                return bigInt;
            }
            bigInt = new BigInt();
            bigInt.putLittleEndianInts(this.digits, this.sign < 0);
            setBigInt(bigInt);
            return bigInt;
        }
    }

    private void setBigInt(BigInt bigInt) {
        this.bigInt = bigInt;
        this.nativeIsValid = true;
    }

    private void setJavaRepresentation(int sign, int numberLength, int[] digits) {
        while (numberLength > 0) {
            numberLength--;
            if (digits[numberLength] != 0) {
                break;
            }
        }
        int numberLength2 = numberLength + 1;
        if (digits[numberLength] == 0) {
            sign = 0;
        }
        this.sign = sign;
        this.digits = digits;
        this.numberLength = numberLength2;
        this.javaIsValid = true;
    }

    void prepareJavaRepresentation() {
        if (!this.javaIsValid) {
            synchronized (this) {
                if (this.javaIsValid) {
                    return;
                }
                int sign = this.bigInt.sign();
                int[] digits = sign != 0 ? this.bigInt.littleEndianIntsMagnitude() : new int[]{0};
                setJavaRepresentation(sign, digits.length, digits);
            }
        }
    }

    public static BigInteger valueOf(long value) {
        if (value < 0) {
            if (value != -1) {
                return new BigInteger(-1, -value);
            }
            return MINUS_ONE;
        } else if (value < ((long) SMALL_VALUES.length)) {
            return SMALL_VALUES[(int) value];
        } else {
            return new BigInteger(1, value);
        }
    }

    public byte[] toByteArray() {
        return twosComplement();
    }

    public BigInteger abs() {
        BigInt bigInt = getBigInt();
        if (bigInt.sign() >= 0) {
            return this;
        }
        BigInt a = bigInt.copy();
        a.setSign(1);
        return new BigInteger(a);
    }

    public BigInteger negate() {
        BigInt bigInt = getBigInt();
        int sign = bigInt.sign();
        if (sign == 0) {
            return this;
        }
        BigInt a = bigInt.copy();
        a.setSign(-sign);
        return new BigInteger(a);
    }

    public BigInteger add(BigInteger value) {
        BigInt lhs = getBigInt();
        BigInt rhs = value.getBigInt();
        if (rhs.sign() == 0) {
            return this;
        }
        if (lhs.sign() == 0) {
            return value;
        }
        return new BigInteger(BigInt.addition(lhs, rhs));
    }

    public BigInteger subtract(BigInteger value) {
        BigInt lhs = getBigInt();
        BigInt rhs = value.getBigInt();
        if (rhs.sign() == 0) {
            return this;
        }
        return new BigInteger(BigInt.subtraction(lhs, rhs));
    }

    public int signum() {
        if (this.javaIsValid) {
            return this.sign;
        }
        return getBigInt().sign();
    }

    public BigInteger shiftRight(int n) {
        return shiftLeft(-n);
    }

    public BigInteger shiftLeft(int n) {
        if (n == 0) {
            return this;
        }
        int sign = signum();
        if (sign == 0) {
            return this;
        }
        return (sign > 0 || n >= 0) ? new BigInteger(BigInt.shift(getBigInt(), n)) : BitLevel.shiftRight(this, -n);
    }

    BigInteger shiftLeftOneBit() {
        return signum() == 0 ? this : BitLevel.shiftLeftOneBit(this);
    }

    public int bitLength() {
        if (this.nativeIsValid || !this.javaIsValid) {
            return getBigInt().bitLength();
        }
        return BitLevel.bitLength(this);
    }

    public boolean testBit(int n) {
        if (n >= 0) {
            int sign = signum();
            if (sign > 0 && this.nativeIsValid && !this.javaIsValid) {
                return getBigInt().isBitSet(n);
            }
            prepareJavaRepresentation();
            boolean z = true;
            if (n == 0) {
                if ((this.digits[0] & 1) == 0) {
                    z = false;
                }
                return z;
            }
            int intCount = n >> 5;
            if (intCount >= this.numberLength) {
                if (sign >= 0) {
                    z = false;
                }
                return z;
            }
            int digit = this.digits[intCount];
            n = 1 << (n & 31);
            if (sign < 0) {
                int firstNonZeroDigit = getFirstNonzeroDigit();
                if (intCount < firstNonZeroDigit) {
                    return false;
                }
                if (firstNonZeroDigit == intCount) {
                    digit = -digit;
                } else {
                    digit = ~digit;
                }
            }
            if ((digit & n) == 0) {
                z = false;
            }
            return z;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("n < 0: ");
        stringBuilder.append(n);
        throw new ArithmeticException(stringBuilder.toString());
    }

    public BigInteger setBit(int n) {
        prepareJavaRepresentation();
        if (testBit(n)) {
            return this;
        }
        return BitLevel.flipBit(this, n);
    }

    public BigInteger clearBit(int n) {
        prepareJavaRepresentation();
        if (testBit(n)) {
            return BitLevel.flipBit(this, n);
        }
        return this;
    }

    public BigInteger flipBit(int n) {
        prepareJavaRepresentation();
        if (n >= 0) {
            return BitLevel.flipBit(this, n);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("n < 0: ");
        stringBuilder.append(n);
        throw new ArithmeticException(stringBuilder.toString());
    }

    public int getLowestSetBit() {
        prepareJavaRepresentation();
        if (this.sign == 0) {
            return -1;
        }
        int i = getFirstNonzeroDigit();
        return (i << 5) + Integer.numberOfTrailingZeros(this.digits[i]);
    }

    public int bitCount() {
        prepareJavaRepresentation();
        return BitLevel.bitCount(this);
    }

    public BigInteger not() {
        prepareJavaRepresentation();
        return Logical.not(this);
    }

    public BigInteger and(BigInteger value) {
        prepareJavaRepresentation();
        value.prepareJavaRepresentation();
        return Logical.and(this, value);
    }

    public BigInteger or(BigInteger value) {
        prepareJavaRepresentation();
        value.prepareJavaRepresentation();
        return Logical.or(this, value);
    }

    public BigInteger xor(BigInteger value) {
        prepareJavaRepresentation();
        value.prepareJavaRepresentation();
        return Logical.xor(this, value);
    }

    public BigInteger andNot(BigInteger value) {
        prepareJavaRepresentation();
        value.prepareJavaRepresentation();
        return Logical.andNot(this, value);
    }

    public int intValue() {
        if (this.nativeIsValid && this.bigInt.twosCompFitsIntoBytes(4)) {
            return (int) this.bigInt.longInt();
        }
        prepareJavaRepresentation();
        return this.sign * this.digits[0];
    }

    public long longValue() {
        if (this.nativeIsValid && this.bigInt.twosCompFitsIntoBytes(8)) {
            return this.bigInt.longInt();
        }
        long value;
        prepareJavaRepresentation();
        if (this.numberLength > 1) {
            value = (((long) this.digits[0]) & 4294967295L) | (((long) this.digits[1]) << 32);
        } else {
            value = ((long) this.digits[0]) & 4294967295L;
        }
        return ((long) this.sign) * value;
    }

    public float floatValue() {
        return (float) doubleValue();
    }

    public double doubleValue() {
        return Conversion.bigInteger2Double(this);
    }

    public int compareTo(BigInteger value) {
        return BigInt.cmp(getBigInt(), value.getBigInt());
    }

    public BigInteger min(BigInteger value) {
        return compareTo(value) == -1 ? this : value;
    }

    public BigInteger max(BigInteger value) {
        return compareTo(value) == 1 ? this : value;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            prepareJavaRepresentation();
            int hash = 0;
            for (int i = 0; i < this.numberLength; i++) {
                hash = (hash * 33) + this.digits[i];
            }
            this.hashCode = this.sign * hash;
        }
        return this.hashCode;
    }

    public boolean equals(Object x) {
        boolean z = true;
        if (this == x) {
            return true;
        }
        if (!(x instanceof BigInteger)) {
            return false;
        }
        if (compareTo((BigInteger) x) != 0) {
            z = false;
        }
        return z;
    }

    public String toString() {
        return getBigInt().decString();
    }

    public String toString(int radix) {
        if (radix == 10) {
            return getBigInt().decString();
        }
        prepareJavaRepresentation();
        return Conversion.bigInteger2String(this, radix);
    }

    public BigInteger gcd(BigInteger value) {
        return new BigInteger(BigInt.gcd(getBigInt(), value.getBigInt()));
    }

    public BigInteger multiply(BigInteger value) {
        return new BigInteger(BigInt.product(getBigInt(), value.getBigInt()));
    }

    public BigInteger pow(int exp) {
        if (exp >= 0) {
            return new BigInteger(BigInt.exp(getBigInt(), exp));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("exp < 0: ");
        stringBuilder.append(exp);
        throw new ArithmeticException(stringBuilder.toString());
    }

    public BigInteger[] divideAndRemainder(BigInteger divisor) {
        BigInt.division(getBigInt(), divisor.getBigInt(), new BigInt(), new BigInt());
        return new BigInteger[]{new BigInteger(quotient), new BigInteger(remainder)};
    }

    public BigInteger divide(BigInteger divisor) {
        BigInt quotient = new BigInt();
        BigInt.division(getBigInt(), divisor.getBigInt(), quotient, null);
        return new BigInteger(quotient);
    }

    public BigInteger remainder(BigInteger divisor) {
        BigInt remainder = new BigInt();
        BigInt.division(getBigInt(), divisor.getBigInt(), null, remainder);
        return new BigInteger(remainder);
    }

    public BigInteger modInverse(BigInteger m) {
        if (m.signum() > 0) {
            return new BigInteger(BigInt.modInverse(getBigInt(), m.getBigInt()));
        }
        throw new ArithmeticException("modulus not positive");
    }

    public BigInteger modPow(BigInteger exponent, BigInteger modulus) {
        if (modulus.signum() > 0) {
            int exponentSignum = exponent.signum();
            if (exponentSignum == 0) {
                return ONE.mod(modulus);
            }
            return new BigInteger(BigInt.modExp((exponentSignum < 0 ? modInverse(modulus) : this).getBigInt(), exponent.getBigInt(), modulus.getBigInt()));
        }
        throw new ArithmeticException("modulus.signum() <= 0");
    }

    public BigInteger mod(BigInteger m) {
        if (m.signum() > 0) {
            return new BigInteger(BigInt.modulus(getBigInt(), m.getBigInt()));
        }
        throw new ArithmeticException("m.signum() <= 0");
    }

    public boolean isProbablePrime(int certainty) {
        if (certainty <= 0) {
            return true;
        }
        return getBigInt().isPrime(certainty);
    }

    public BigInteger nextProbablePrime() {
        if (this.sign >= 0) {
            return Primality.nextProbablePrime(this);
        }
        throw new ArithmeticException("sign < 0");
    }

    public static BigInteger probablePrime(int bitLength, Random random) {
        return new BigInteger(bitLength, 100, random);
    }

    private byte[] twosComplement() {
        prepareJavaRepresentation();
        if (this.sign == 0) {
            return new byte[]{(byte) 0};
        }
        int highBytes;
        int bitLen = bitLength();
        int iThis = getFirstNonzeroDigit();
        int bytesLen = (bitLen >> 3) + 1;
        byte[] bytes = new byte[bytesLen];
        int firstByteNumber = 0;
        int bytesInInteger = 4;
        if (bytesLen - (this.numberLength << 2) == 1) {
            bytes[0] = (byte) (this.sign < 0 ? -1 : 0);
            highBytes = 4;
            firstByteNumber = 0 + 1;
        } else {
            highBytes = bytesLen & 3;
            highBytes = highBytes == 0 ? 4 : highBytes;
        }
        int digitIndex = iThis;
        bytesLen -= iThis << 2;
        int digit;
        int i;
        if (this.sign < 0) {
            digit = -this.digits[digitIndex];
            digitIndex++;
            if (digitIndex == this.numberLength) {
                bytesInInteger = highBytes;
            }
            i = 0;
            while (i < bytesInInteger) {
                bytesLen--;
                bytes[bytesLen] = (byte) digit;
                i++;
                digit >>= 8;
            }
            while (bytesLen > firstByteNumber) {
                i = ~this.digits[digitIndex];
                digitIndex++;
                if (digitIndex == this.numberLength) {
                    bytesInInteger = highBytes;
                }
                digit = i;
                i = 0;
                while (i < bytesInInteger) {
                    bytesLen--;
                    bytes[bytesLen] = (byte) digit;
                    i++;
                    digit >>= 8;
                }
            }
        } else {
            while (bytesLen > firstByteNumber) {
                i = this.digits[digitIndex];
                digitIndex++;
                if (digitIndex == this.numberLength) {
                    bytesInInteger = highBytes;
                }
                digit = bytesLen;
                bytesLen = i;
                i = 0;
                while (i < bytesInInteger) {
                    digit--;
                    bytes[digit] = (byte) bytesLen;
                    i++;
                    bytesLen >>= 8;
                }
                bytesLen = digit;
            }
        }
        return bytes;
    }

    static int multiplyByInt(int[] res, int[] a, int aSize, int factor) {
        long carry = 0;
        for (int i = 0; i < aSize; i++) {
            carry += (((long) a[i]) & 4294967295L) * (4294967295L & ((long) factor));
            res[i] = (int) carry;
            carry >>>= 32;
        }
        return (int) carry;
    }

    static int inplaceAdd(int[] a, int aSize, int addend) {
        long carry = ((long) addend) & 4294967295L;
        int i = 0;
        while (carry != 0 && i < aSize) {
            carry += ((long) a[i]) & 4294967295L;
            a[i] = (int) carry;
            carry >>= 32;
            i++;
        }
        return (int) carry;
    }

    private static void parseFromString(BigInteger bi, String value, int radix) {
        int sign;
        String str = value;
        int i = radix;
        int stringLength = value.length();
        int endChar = stringLength;
        int startChar = 0;
        if (str.charAt(0) == '-') {
            stringLength--;
            sign = -1;
            startChar = 1;
        } else {
            sign = 1;
        }
        int charsPerInt = Conversion.digitFitInInt[i];
        int bigRadixDigitsLength = stringLength / charsPerInt;
        int topChars = stringLength % charsPerInt;
        if (topChars != 0) {
            bigRadixDigitsLength++;
        }
        int[] digits = new int[bigRadixDigitsLength];
        int bigRadix = Conversion.bigRadices[i - 2];
        int substrEnd = (topChars == 0 ? charsPerInt : topChars) + startChar;
        int digitIndex = 0;
        int substrStart = startChar;
        while (substrStart < endChar) {
            int digitIndex2 = digitIndex + 1;
            digits[digitIndex] = multiplyByInt(digits, digits, digitIndex, bigRadix) + inplaceAdd(digits, digitIndex, Integer.parseInt(str.substring(substrStart, substrEnd), i));
            substrStart = substrEnd;
            substrEnd = substrStart + charsPerInt;
            digitIndex = digitIndex2;
        }
        bi.setJavaRepresentation(sign, digitIndex, digits);
    }

    int getFirstNonzeroDigit() {
        if (this.firstNonzeroDigit == -2) {
            int i;
            if (this.sign == 0) {
                i = -1;
            } else {
                i = 0;
                while (this.digits[i] == 0) {
                    i++;
                }
            }
            this.firstNonzeroDigit = i;
        }
        return this.firstNonzeroDigit;
    }

    BigInteger copy() {
        prepareJavaRepresentation();
        int[] copyDigits = new int[this.numberLength];
        System.arraycopy(this.digits, 0, copyDigits, 0, this.numberLength);
        return new BigInteger(this.sign, this.numberLength, copyDigits);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        BigInt bigInt = new BigInt();
        bigInt.putBigEndian(this.magnitude, this.signum < 0);
        setBigInt(bigInt);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        BigInt bigInt = getBigInt();
        this.signum = bigInt.sign();
        this.magnitude = bigInt.bigEndianMagnitude();
        out.defaultWriteObject();
    }
}
