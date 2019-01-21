package java.math;

import dalvik.system.VMDebug;

class Conversion {
    static final int[] bigRadices = new int[]{Integer.MIN_VALUE, 1162261467, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 1220703125, 362797056, 1977326743, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 387420489, 1000000000, 214358881, 429981696, 815730721, 1475789056, 170859375, VMDebug.KIND_THREAD_EXT_ALLOCATED_OBJECTS, 410338673, 612220032, 893871739, 1280000000, 1801088541, 113379904, 148035889, 191102976, 244140625, 308915776, 387420489, 481890304, 594823321, 729000000, 887503681, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 1291467969, 1544804416, 1838265625, 60466176};
    static final int[] digitFitInInt = new int[]{-1, -1, 31, 19, 15, 13, 11, 11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5};

    private Conversion() {
    }

    static String bigInteger2String(BigInteger val, int radix) {
        BigInteger bigInteger = val;
        int i = radix;
        val.prepareJavaRepresentation();
        int sign = bigInteger.sign;
        int numberLength = bigInteger.numberLength;
        int[] digits = bigInteger.digits;
        if (sign == 0) {
            return AndroidHardcodedSystemProperties.JAVA_VERSION;
        }
        if (numberLength == 1) {
            long v = ((long) digits[numberLength - 1]) & 4294967295L;
            if (sign < 0) {
                v = -v;
            }
            return Long.toString(v, i);
        } else if (i == 10 || i < 2 || i > 36) {
            return val.toString();
        } else {
            int currentChar;
            int i2;
            int resLengthInChars = ((int) ((((double) val.abs().bitLength()) / (Math.log((double) i) / Math.log(2.0d))) + ((double) (sign < 0 ? 1 : 0)))) + 1;
            char[] result = new char[resLengthInChars];
            int previous = resLengthInChars;
            char c = '0';
            int bigRadix;
            int bigRadix2;
            if (i != 16) {
                int[] temp = new int[numberLength];
                System.arraycopy(digits, 0, temp, 0, numberLength);
                int tempLen = numberLength;
                int charsPerInt = digitFitInInt[i];
                bigRadix = bigRadices[i - 2];
                while (true) {
                    int i3;
                    bigRadix2 = bigRadix;
                    int resDigit = Division.divideArrayByInt(temp, temp, tempLen, bigRadix2);
                    bigRadix = previous;
                    while (true) {
                        bigRadix--;
                        result[bigRadix] = Character.forDigit(resDigit % i, i);
                        i3 = resDigit / i;
                        resDigit = i3;
                        if (i3 == 0 || bigRadix == 0) {
                            i3 = (charsPerInt - previous) + bigRadix;
                            currentChar = bigRadix;
                            bigRadix = 0;
                        }
                    }
                    i3 = (charsPerInt - previous) + bigRadix;
                    currentChar = bigRadix;
                    bigRadix = 0;
                    while (true) {
                        i2 = bigRadix;
                        if (i2 >= i3 || currentChar <= 0) {
                            i2 = tempLen - 1;
                        } else {
                            currentChar--;
                            result[currentChar] = c;
                            bigRadix = i2 + 1;
                        }
                    }
                    i2 = tempLen - 1;
                    while (i2 > 0 && temp[i2] == 0) {
                        i2--;
                    }
                    tempLen = i2 + 1;
                    if (tempLen == 1) {
                        bigRadix = 0;
                        if (temp[0] == 0) {
                            break;
                        }
                    } else {
                        bigRadix = 0;
                    }
                    i2 = 1;
                    previous = currentChar;
                    c = '0';
                    int i4 = bigRadix;
                    bigRadix = bigRadix2;
                    bigRadix2 = i4;
                }
            } else {
                bigRadix = 0;
                currentChar = previous;
                for (i2 = bigRadix; i2 < numberLength; i2++) {
                    for (bigRadix2 = bigRadix; bigRadix2 < 8 && currentChar > 0; bigRadix2++) {
                        currentChar--;
                        result[currentChar] = Character.forDigit((digits[i2] >> (bigRadix2 << 2)) & 15, 16);
                    }
                }
            }
            while (result[currentChar] == '0') {
                currentChar++;
            }
            if (sign == -1) {
                currentChar--;
                result[currentChar] = '-';
            }
            i2 = currentChar;
            return new String(result, i2, resLengthInChars - i2);
        }
    }

    static String toDecimalScaledString(BigInteger val, int scale) {
        BigInteger bigInteger = val;
        int i = scale;
        val.prepareJavaRepresentation();
        int sign = bigInteger.sign;
        int numberLength = bigInteger.numberLength;
        int[] digits = bigInteger.digits;
        if (sign == 0) {
            switch (i) {
                case 0:
                    return AndroidHardcodedSystemProperties.JAVA_VERSION;
                case 1:
                    return "0.0";
                case 2:
                    return "0.00";
                case 3:
                    return "0.000";
                case 4:
                    return "0.0000";
                case 5:
                    return "0.00000";
                case 6:
                    return "0.000000";
                default:
                    StringBuilder result1 = new StringBuilder();
                    if (i < 0) {
                        result1.append("0E+");
                    } else {
                        result1.append("0E");
                    }
                    result1.append(-i);
                    return result1.toString();
            }
        }
        char[] result;
        int[] temp;
        int tempLen;
        long result11;
        int i2;
        int currentChar;
        int resLengthInChars = ((numberLength * 10) + 1) + 7;
        char[] result2 = new char[(resLengthInChars + 1)];
        int previous = resLengthInChars;
        long j = 4294967295L;
        if (numberLength == 1) {
            int highDigit = digits[0];
            if (highDigit < 0) {
                j = 4294967295L & ((long) highDigit);
                while (true) {
                    long prev = j;
                    j /= 10;
                    previous--;
                    result = result2;
                    result[previous] = (char) (((int) (prev - (10 * j))) + 48);
                    if (j == 0) {
                        break;
                    }
                    result2 = result;
                }
            } else {
                result = result2;
                int v = highDigit;
                do {
                    result2 = v;
                    v /= 10;
                    previous--;
                    result[previous] = (char) ((result2 - (v * 10)) + 48);
                } while (v != 0);
            }
        } else {
            result = result2;
            temp = new int[numberLength];
            tempLen = numberLength;
            System.arraycopy(digits, 0, temp, 0, tempLen);
            loop2:
            while (true) {
                result11 = 0;
                int i1 = tempLen - 1;
                while (i1 >= 0) {
                    result11 = divideLongByBillion((result11 << 32) + (((long) temp[i1]) & j));
                    temp[i1] = (int) result11;
                    result11 = (long) ((int) (result11 >> 32));
                    i1--;
                    j = 4294967295L;
                }
                long j2 = result11;
                int resDigit = (int) result11;
                int currentChar2 = previous;
                do {
                    currentChar2--;
                    result[currentChar2] = (char) ((resDigit % 10) + 48);
                    i2 = resDigit / 10;
                    resDigit = i2;
                    if (i2 == 0) {
                        break;
                    }
                } while (currentChar2 != 0);
                i2 = (9 - previous) + currentChar2;
                currentChar = currentChar2;
                for (currentChar2 = 0; currentChar2 < i2 && currentChar > 0; currentChar2++) {
                    currentChar--;
                    result[currentChar] = '0';
                }
                currentChar2 = tempLen - 1;
                while (temp[currentChar2] == 0) {
                    if (currentChar2 == 0) {
                        break loop2;
                    }
                    currentChar2--;
                }
                tempLen = currentChar2 + 1;
                previous = currentChar;
                currentChar = 48;
                j = 4294967295L;
            }
            previous = currentChar;
            while (result[previous] == '0') {
                previous++;
            }
        }
        temp = sign < 0 ? 1 : null;
        tempLen = ((resLengthInChars - previous) - i) - 1;
        if (i == 0) {
            if (temp != null) {
                previous--;
                result[previous] = '-';
            }
            return new String(result, previous, resLengthInChars - previous);
        }
        char[] result3 = result;
        if (i <= 0 || tempLen < -6) {
            i2 = previous + 1;
            currentChar = resLengthInChars;
            result11 = new StringBuilder((16 + currentChar) - i2);
            if (temp != null) {
                result11.append('-');
            }
            if (currentChar - i2 >= 1) {
                result11.append(result3[previous]);
                result11.append('.');
                result11.append(result3, previous + 1, (resLengthInChars - previous) - 1);
            } else {
                result11.append(result3, previous, resLengthInChars - previous);
            }
            result11.append('E');
            if (tempLen > 0) {
                result11.append('+');
            }
            result11.append(Integer.toString(tempLen));
            return result11.toString();
        } else if (tempLen >= 0) {
            i2 = previous + tempLen;
            for (currentChar = resLengthInChars - 1; currentChar >= i2; currentChar--) {
                result3[currentChar + 1] = result3[currentChar];
            }
            result3[i2 + 1] = '.';
            if (temp != null) {
                previous--;
                result3[previous] = '-';
            }
            return new String(result3, previous, (resLengthInChars - previous) + 1);
        } else {
            i2 = 2;
            for (currentChar = 1; i2 < (-tempLen) + currentChar; currentChar = 1) {
                previous--;
                result3[previous] = '0';
                i2++;
            }
            previous--;
            result3[previous] = '.';
            previous--;
            result3[previous] = '0';
            if (temp != null) {
                previous--;
                result3[previous] = '-';
            }
            return new String(result3, previous, resLengthInChars - previous);
        }
    }

    static String toDecimalScaledString(long value, int scale) {
        long value2 = value;
        int i = scale;
        boolean negNumber = value2 < 0;
        if (negNumber) {
            value2 = -value2;
        }
        if (value2 == 0) {
            switch (i) {
                case 0:
                    return AndroidHardcodedSystemProperties.JAVA_VERSION;
                case 1:
                    return "0.0";
                case 2:
                    return "0.00";
                case 3:
                    return "0.000";
                case 4:
                    return "0.0000";
                case 5:
                    return "0.00000";
                case 6:
                    return "0.000000";
                default:
                    StringBuilder result1 = new StringBuilder();
                    if (i < 0) {
                        result1.append("0E+");
                    } else {
                        result1.append("0E");
                    }
                    result1.append(i == Integer.MIN_VALUE ? "2147483648" : Integer.toString(-i));
                    return result1.toString();
            }
        }
        long prev;
        char[] result = new char[(18 + 1)];
        int currentChar = 18;
        long v = value2;
        do {
            prev = v;
            v /= 10;
            currentChar--;
            result[currentChar] = (char) ((int) (48 + (prev - (10 * v))));
        } while (v != 0);
        prev = ((((long) 18) - ((long) currentChar)) - ((long) i)) - 1;
        int insertPoint;
        if (i == 0) {
            if (negNumber) {
                currentChar--;
                result[currentChar] = '-';
            }
            return new String(result, currentChar, 18 - currentChar);
        } else if (i <= 0 || prev < -6) {
            int startPoint = currentChar + 1;
            int endPoint = 18;
            StringBuilder result12 = new StringBuilder((16 + endPoint) - startPoint);
            if (negNumber) {
                result12.append('-');
            }
            if (endPoint - startPoint >= 1) {
                result12.append(result[currentChar]);
                result12.append('.');
                result12.append(result, currentChar + 1, (18 - currentChar) - 1);
            } else {
                result12.append(result, currentChar, 18 - currentChar);
            }
            result12.append('E');
            if (prev > 0) {
                result12.append('+');
            }
            result12.append(Long.toString(prev));
            return result12.toString();
        } else if (prev >= 0) {
            insertPoint = ((int) prev) + currentChar;
            for (int j = 18 - 1; j >= insertPoint; j--) {
                result[j + 1] = result[j];
            }
            result[insertPoint + 1] = '.';
            if (negNumber) {
                currentChar--;
                result[currentChar] = '-';
            }
            return new String(result, currentChar, (18 - currentChar) + 1);
        } else {
            insertPoint = 2;
            while (true) {
                long value3 = value2;
                if (((long) insertPoint) >= (-prev) + 1) {
                    break;
                }
                currentChar--;
                result[currentChar] = '0';
                insertPoint++;
                value2 = value3;
            }
            currentChar--;
            result[currentChar] = '.';
            currentChar--;
            result[currentChar] = '0';
            if (negNumber) {
                currentChar--;
                result[currentChar] = '-';
            }
            return new String(result, currentChar, 18 - currentChar);
        }
    }

    static long divideLongByBillion(long a) {
        long quot;
        long rem;
        if (a >= 0) {
            quot = a / 1000000000;
            rem = a % 1000000000;
        } else {
            long aPos = a >>> 1;
            rem = ((aPos % 500000000) << 1) + (1 & a);
            quot = aPos / 500000000;
        }
        return (rem << 32) | (4294967295L & quot);
    }

    static double bigInteger2Double(BigInteger val) {
        BigInteger bigInteger = val;
        val.prepareJavaRepresentation();
        if (bigInteger.numberLength < 2 || (bigInteger.numberLength == 2 && bigInteger.digits[1] > 0)) {
            return (double) val.longValue();
        }
        double d = Double.NEGATIVE_INFINITY;
        if (bigInteger.numberLength > 32) {
            if (bigInteger.sign > 0) {
                d = Double.POSITIVE_INFINITY;
            }
            return d;
        }
        int bitLen = val.abs().bitLength();
        long exponent = (long) (bitLen - 1);
        int delta = bitLen - 54;
        long mantissa = val.abs().shiftRight(delta).longValue() & 9007199254740991L;
        if (exponent == 1023) {
            if (mantissa == 9007199254740991L) {
                if (bigInteger.sign > 0) {
                    d = Double.POSITIVE_INFINITY;
                }
                return d;
            } else if (mantissa == 9007199254740990L) {
                return bigInteger.sign > 0 ? Double.MAX_VALUE : -1.7976931348623157E308d;
            }
        }
        if ((mantissa & 1) == 1 && ((mantissa & 2) == 2 || BitLevel.nonZeroDroppedBits(delta, bigInteger.digits))) {
            mantissa += 2;
        }
        return Double.longBitsToDouble(((bigInteger.sign < 0 ? Long.MIN_VALUE : 0) | (((1023 + exponent) << 52) & 9218868437227405312L)) | (mantissa >> 1));
    }
}
