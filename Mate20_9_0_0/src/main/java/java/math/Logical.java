package java.math;

class Logical {
    private Logical() {
    }

    static BigInteger not(BigInteger val) {
        if (val.sign == 0) {
            return BigInteger.MINUS_ONE;
        }
        if (val.equals(BigInteger.MINUS_ONE)) {
            return BigInteger.ZERO;
        }
        int i;
        int[] resDigits = new int[(val.numberLength + 1)];
        int i2 = 0;
        if (val.sign <= 0) {
            while (true) {
                i = i2;
                if (val.digits[i] != 0) {
                    break;
                }
                resDigits[i] = -1;
                i2 = i + 1;
            }
        } else if (val.digits[val.numberLength - 1] != -1) {
            while (true) {
                i = i2;
                if (val.digits[i] != -1) {
                    break;
                }
                i2 = i + 1;
            }
        } else {
            while (true) {
                i = i2;
                if (i < val.numberLength && val.digits[i] == -1) {
                    i2 = i + 1;
                }
            }
            if (i == val.numberLength) {
                resDigits[i] = 1;
                return new BigInteger(-val.sign, i + 1, resDigits);
            }
        }
        resDigits[i] = val.digits[i] + val.sign;
        i++;
        while (i < val.numberLength) {
            resDigits[i] = val.digits[i];
            i++;
        }
        return new BigInteger(-val.sign, i, resDigits);
    }

    static BigInteger and(BigInteger val, BigInteger that) {
        if (that.sign == 0 || val.sign == 0) {
            return BigInteger.ZERO;
        }
        if (that.equals(BigInteger.MINUS_ONE)) {
            return val;
        }
        if (val.equals(BigInteger.MINUS_ONE)) {
            return that;
        }
        if (val.sign > 0) {
            if (that.sign > 0) {
                return andPositive(val, that);
            }
            return andDiffSigns(val, that);
        } else if (that.sign > 0) {
            return andDiffSigns(that, val);
        } else {
            if (val.numberLength > that.numberLength) {
                return andNegative(val, that);
            }
            return andNegative(that, val);
        }
    }

    static BigInteger andPositive(BigInteger val, BigInteger that) {
        int resLength = Math.min(val.numberLength, that.numberLength);
        int i = Math.max(val.getFirstNonzeroDigit(), that.getFirstNonzeroDigit());
        if (i >= resLength) {
            return BigInteger.ZERO;
        }
        int[] resDigits = new int[resLength];
        while (i < resLength) {
            resDigits[i] = val.digits[i] & that.digits[i];
            i++;
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger andDiffSigns(BigInteger positive, BigInteger negative) {
        int iPos = positive.getFirstNonzeroDigit();
        int iNeg = negative.getFirstNonzeroDigit();
        if (iNeg >= positive.numberLength) {
            return BigInteger.ZERO;
        }
        int resLength = positive.numberLength;
        int[] resDigits = new int[resLength];
        int i = Math.max(iPos, iNeg);
        if (i == iNeg) {
            resDigits[i] = (-negative.digits[i]) & positive.digits[i];
            i++;
        }
        int limit = Math.min(negative.numberLength, positive.numberLength);
        while (i < limit) {
            resDigits[i] = (~negative.digits[i]) & positive.digits[i];
            i++;
        }
        if (i >= negative.numberLength) {
            while (i < positive.numberLength) {
                resDigits[i] = positive.digits[i];
                i++;
            }
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger andNegative(BigInteger longer, BigInteger shorter) {
        int iLonger = longer.getFirstNonzeroDigit();
        int iShorter = shorter.getFirstNonzeroDigit();
        if (iLonger >= shorter.numberLength) {
            return longer;
        }
        int digit;
        int i;
        int[] resDigits;
        int i2 = Math.max(iShorter, iLonger);
        if (iShorter > iLonger) {
            digit = (-shorter.digits[i2]) & (~longer.digits[i2]);
        } else if (iShorter < iLonger) {
            digit = (~shorter.digits[i2]) & (-longer.digits[i2]);
        } else {
            digit = (-shorter.digits[i2]) & (-longer.digits[i2]);
        }
        if (digit == 0) {
            while (true) {
                i2++;
                if (i2 >= shorter.numberLength) {
                    break;
                }
                i = ~(longer.digits[i2] | shorter.digits[i2]);
                digit = i;
                if (i != 0) {
                    break;
                }
            }
            if (digit == 0) {
                while (i2 < longer.numberLength) {
                    i = ~longer.digits[i2];
                    digit = i;
                    if (i != 0) {
                        break;
                    }
                    i2++;
                }
                if (digit == 0) {
                    i = longer.numberLength + 1;
                    resDigits = new int[i];
                    resDigits[i - 1] = 1;
                    return new BigInteger(-1, i, resDigits);
                }
            }
        }
        i = longer.numberLength;
        resDigits = new int[i];
        resDigits[i2] = -digit;
        i2++;
        while (i2 < shorter.numberLength) {
            resDigits[i2] = longer.digits[i2] | shorter.digits[i2];
            i2++;
        }
        while (i2 < longer.numberLength) {
            resDigits[i2] = longer.digits[i2];
            i2++;
        }
        return new BigInteger(-1, i, resDigits);
    }

    static BigInteger andNot(BigInteger val, BigInteger that) {
        if (that.sign == 0) {
            return val;
        }
        if (val.sign == 0) {
            return BigInteger.ZERO;
        }
        if (val.equals(BigInteger.MINUS_ONE)) {
            return that.not();
        }
        if (that.equals(BigInteger.MINUS_ONE)) {
            return BigInteger.ZERO;
        }
        if (val.sign > 0) {
            if (that.sign > 0) {
                return andNotPositive(val, that);
            }
            return andNotPositiveNegative(val, that);
        } else if (that.sign > 0) {
            return andNotNegativePositive(val, that);
        } else {
            return andNotNegative(val, that);
        }
    }

    static BigInteger andNotPositive(BigInteger val, BigInteger that) {
        int[] resDigits = new int[val.numberLength];
        int limit = Math.min(val.numberLength, that.numberLength);
        int i = val.getFirstNonzeroDigit();
        while (i < limit) {
            resDigits[i] = val.digits[i] & (~that.digits[i]);
            i++;
        }
        while (i < val.numberLength) {
            resDigits[i] = val.digits[i];
            i++;
        }
        return new BigInteger(1, val.numberLength, resDigits);
    }

    static BigInteger andNotPositiveNegative(BigInteger positive, BigInteger negative) {
        int iNeg = negative.getFirstNonzeroDigit();
        int iPos = positive.getFirstNonzeroDigit();
        if (iNeg >= positive.numberLength) {
            return positive;
        }
        int resLength = Math.min(positive.numberLength, negative.numberLength);
        int[] resDigits = new int[resLength];
        int i = iPos;
        while (i < iNeg) {
            resDigits[i] = positive.digits[i];
            i++;
        }
        if (i == iNeg) {
            resDigits[i] = positive.digits[i] & (negative.digits[i] - 1);
            i++;
        }
        while (i < resLength) {
            resDigits[i] = positive.digits[i] & negative.digits[i];
            i++;
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger andNotNegativePositive(BigInteger negative, BigInteger positive) {
        int iNeg = negative.getFirstNonzeroDigit();
        int iPos = positive.getFirstNonzeroDigit();
        if (iNeg >= positive.numberLength) {
            return negative;
        }
        int[] resDigits;
        int limit;
        int resLength = Math.max(negative.numberLength, positive.numberLength);
        int i = iNeg;
        if (iPos > iNeg) {
            resDigits = new int[resLength];
            limit = Math.min(negative.numberLength, iPos);
            while (i < limit) {
                resDigits[i] = negative.digits[i];
                i++;
            }
            if (i == negative.numberLength) {
                i = iPos;
                while (i < positive.numberLength) {
                    resDigits[i] = positive.digits[i];
                    i++;
                }
            }
        } else {
            int digit = (-negative.digits[i]) & (~positive.digits[i]);
            if (digit == 0) {
                int i2;
                int limit2 = Math.min(positive.numberLength, negative.numberLength);
                while (true) {
                    i++;
                    if (i >= limit2) {
                        break;
                    }
                    i2 = ~(negative.digits[i] | positive.digits[i]);
                    digit = i2;
                    if (i2 != 0) {
                        break;
                    }
                }
                if (digit == 0) {
                    while (i < positive.numberLength) {
                        i2 = ~positive.digits[i];
                        digit = i2;
                        if (i2 != 0) {
                            break;
                        }
                        i++;
                    }
                    while (i < negative.numberLength) {
                        i2 = ~negative.digits[i];
                        digit = i2;
                        if (i2 != 0) {
                            break;
                        }
                        i++;
                    }
                    if (digit == 0) {
                        resLength++;
                        int[] resDigits2 = new int[resLength];
                        resDigits2[resLength - 1] = 1;
                        return new BigInteger(-1, resLength, resDigits2);
                    }
                }
            }
            int[] resDigits3 = new int[resLength];
            resDigits3[i] = -digit;
            i++;
            resDigits = resDigits3;
        }
        limit = Math.min(positive.numberLength, negative.numberLength);
        while (i < limit) {
            resDigits[i] = negative.digits[i] | positive.digits[i];
            i++;
        }
        while (i < negative.numberLength) {
            resDigits[i] = negative.digits[i];
            i++;
        }
        while (i < positive.numberLength) {
            resDigits[i] = positive.digits[i];
            i++;
        }
        return new BigInteger(-1, resLength, resDigits);
    }

    static BigInteger andNotNegative(BigInteger val, BigInteger that) {
        int iVal = val.getFirstNonzeroDigit();
        int iThat = that.getFirstNonzeroDigit();
        if (iVal >= that.numberLength) {
            return BigInteger.ZERO;
        }
        int limit;
        int resLength = that.numberLength;
        int[] resDigits = new int[resLength];
        int i = iVal;
        if (iVal < iThat) {
            resDigits[i] = -val.digits[i];
            limit = Math.min(val.numberLength, iThat);
            while (true) {
                i++;
                if (i >= limit) {
                    break;
                }
                resDigits[i] = ~val.digits[i];
            }
            if (i == val.numberLength) {
                while (i < iThat) {
                    resDigits[i] = -1;
                    i++;
                }
                resDigits[i] = that.digits[i] - 1;
            } else {
                resDigits[i] = (~val.digits[i]) & (that.digits[i] - 1);
            }
        } else if (iThat < iVal) {
            resDigits[i] = (-val.digits[i]) & that.digits[i];
        } else {
            resDigits[i] = (-val.digits[i]) & (that.digits[i] - 1);
        }
        limit = Math.min(val.numberLength, that.numberLength);
        i++;
        while (i < limit) {
            resDigits[i] = (~val.digits[i]) & that.digits[i];
            i++;
        }
        while (i < that.numberLength) {
            resDigits[i] = that.digits[i];
            i++;
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger or(BigInteger val, BigInteger that) {
        if (that.equals(BigInteger.MINUS_ONE) || val.equals(BigInteger.MINUS_ONE)) {
            return BigInteger.MINUS_ONE;
        }
        if (that.sign == 0) {
            return val;
        }
        if (val.sign == 0) {
            return that;
        }
        if (val.sign > 0) {
            if (that.sign <= 0) {
                return orDiffSigns(val, that);
            }
            if (val.numberLength > that.numberLength) {
                return orPositive(val, that);
            }
            return orPositive(that, val);
        } else if (that.sign > 0) {
            return orDiffSigns(that, val);
        } else {
            if (that.getFirstNonzeroDigit() > val.getFirstNonzeroDigit()) {
                return orNegative(that, val);
            }
            return orNegative(val, that);
        }
    }

    static BigInteger orPositive(BigInteger longer, BigInteger shorter) {
        int resLength = longer.numberLength;
        int[] resDigits = new int[resLength];
        int i = 0;
        while (i < shorter.numberLength) {
            resDigits[i] = longer.digits[i] | shorter.digits[i];
            i++;
        }
        while (i < resLength) {
            resDigits[i] = longer.digits[i];
            i++;
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger orNegative(BigInteger val, BigInteger that) {
        int iThat = that.getFirstNonzeroDigit();
        int iVal = val.getFirstNonzeroDigit();
        if (iVal >= that.numberLength) {
            return that;
        }
        if (iThat >= val.numberLength) {
            return val;
        }
        int i;
        int resLength = Math.min(val.numberLength, that.numberLength);
        int[] resDigits = new int[resLength];
        if (iThat == iVal) {
            resDigits[iVal] = -((-val.digits[iVal]) | (-that.digits[iVal]));
            i = iVal;
        } else {
            i = iThat;
            while (i < iVal) {
                resDigits[i] = that.digits[i];
                i++;
            }
            resDigits[i] = that.digits[i] & (val.digits[i] - 1);
        }
        while (true) {
            i++;
            if (i >= resLength) {
                return new BigInteger(-1, resLength, resDigits);
            }
            resDigits[i] = val.digits[i] & that.digits[i];
        }
    }

    static BigInteger orDiffSigns(BigInteger positive, BigInteger negative) {
        int iNeg = negative.getFirstNonzeroDigit();
        int iPos = positive.getFirstNonzeroDigit();
        if (iPos >= negative.numberLength) {
            return negative;
        }
        int i;
        int limit;
        int resLength = negative.numberLength;
        int[] resDigits = new int[resLength];
        if (iNeg < iPos) {
            i = iNeg;
            while (i < iPos) {
                resDigits[i] = negative.digits[i];
                i++;
            }
        } else if (iPos < iNeg) {
            i = iPos;
            resDigits[i] = -positive.digits[i];
            limit = Math.min(positive.numberLength, iNeg);
            while (true) {
                i++;
                if (i >= limit) {
                    break;
                }
                resDigits[i] = ~positive.digits[i];
            }
            if (i != positive.numberLength) {
                resDigits[i] = ~((-negative.digits[i]) | positive.digits[i]);
            } else {
                while (i < iNeg) {
                    resDigits[i] = -1;
                    i++;
                }
                resDigits[i] = negative.digits[i] - 1;
            }
            i++;
        } else {
            i = iPos;
            resDigits[i] = -((-negative.digits[i]) | positive.digits[i]);
            i++;
        }
        limit = Math.min(negative.numberLength, positive.numberLength);
        while (i < limit) {
            resDigits[i] = negative.digits[i] & (~positive.digits[i]);
            i++;
        }
        while (i < negative.numberLength) {
            resDigits[i] = negative.digits[i];
            i++;
        }
        return new BigInteger(-1, resLength, resDigits);
    }

    static BigInteger xor(BigInteger val, BigInteger that) {
        if (that.sign == 0) {
            return val;
        }
        if (val.sign == 0) {
            return that;
        }
        if (that.equals(BigInteger.MINUS_ONE)) {
            return val.not();
        }
        if (val.equals(BigInteger.MINUS_ONE)) {
            return that.not();
        }
        if (val.sign > 0) {
            if (that.sign <= 0) {
                return xorDiffSigns(val, that);
            }
            if (val.numberLength > that.numberLength) {
                return xorPositive(val, that);
            }
            return xorPositive(that, val);
        } else if (that.sign > 0) {
            return xorDiffSigns(that, val);
        } else {
            if (that.getFirstNonzeroDigit() > val.getFirstNonzeroDigit()) {
                return xorNegative(that, val);
            }
            return xorNegative(val, that);
        }
    }

    static BigInteger xorPositive(BigInteger longer, BigInteger shorter) {
        int resLength = longer.numberLength;
        int[] resDigits = new int[resLength];
        int i = Math.min(longer.getFirstNonzeroDigit(), shorter.getFirstNonzeroDigit());
        while (i < shorter.numberLength) {
            resDigits[i] = longer.digits[i] ^ shorter.digits[i];
            i++;
        }
        while (i < longer.numberLength) {
            resDigits[i] = longer.digits[i];
            i++;
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger xorNegative(BigInteger val, BigInteger that) {
        int limit;
        int resLength = Math.max(val.numberLength, that.numberLength);
        int[] resDigits = new int[resLength];
        int iVal = val.getFirstNonzeroDigit();
        int iThat = that.getFirstNonzeroDigit();
        int i = iThat;
        if (iVal == iThat) {
            resDigits[i] = (-val.digits[i]) ^ (-that.digits[i]);
        } else {
            resDigits[i] = -that.digits[i];
            limit = Math.min(that.numberLength, iVal);
            while (true) {
                i++;
                if (i >= limit) {
                    break;
                }
                resDigits[i] = ~that.digits[i];
            }
            if (i == that.numberLength) {
                while (i < iVal) {
                    resDigits[i] = -1;
                    i++;
                }
                resDigits[i] = val.digits[i] - 1;
            } else {
                resDigits[i] = (-val.digits[i]) ^ (~that.digits[i]);
            }
        }
        limit = Math.min(val.numberLength, that.numberLength);
        i++;
        while (i < limit) {
            resDigits[i] = val.digits[i] ^ that.digits[i];
            i++;
        }
        while (i < val.numberLength) {
            resDigits[i] = val.digits[i];
            i++;
        }
        while (i < that.numberLength) {
            resDigits[i] = that.digits[i];
            i++;
        }
        return new BigInteger(1, resLength, resDigits);
    }

    static BigInteger xorDiffSigns(BigInteger positive, BigInteger negative) {
        int[] resDigits;
        int i;
        int resLength = Math.max(negative.numberLength, positive.numberLength);
        int iNeg = negative.getFirstNonzeroDigit();
        int iPos = positive.getFirstNonzeroDigit();
        int limit;
        if (iNeg < iPos) {
            resDigits = new int[resLength];
            i = iNeg;
            resDigits[i] = negative.digits[i];
            limit = Math.min(negative.numberLength, iPos);
            i++;
            while (i < limit) {
                resDigits[i] = negative.digits[i];
                i++;
            }
            if (i == negative.numberLength) {
                while (i < positive.numberLength) {
                    resDigits[i] = positive.digits[i];
                    i++;
                }
            }
        } else if (iPos < iNeg) {
            resDigits = new int[resLength];
            i = iPos;
            resDigits[i] = -positive.digits[i];
            limit = Math.min(positive.numberLength, iNeg);
            i++;
            while (i < limit) {
                resDigits[i] = ~positive.digits[i];
                i++;
            }
            if (i == iNeg) {
                resDigits[i] = ~(positive.digits[i] ^ (-negative.digits[i]));
                i++;
            } else {
                while (i < iNeg) {
                    resDigits[i] = -1;
                    i++;
                }
                while (i < negative.numberLength) {
                    resDigits[i] = negative.digits[i];
                    i++;
                }
            }
        } else {
            int i2 = iNeg;
            i = positive.digits[i2] ^ (-negative.digits[i2]);
            if (i == 0) {
                int i3;
                limit = Math.min(positive.numberLength, negative.numberLength);
                while (true) {
                    i2++;
                    if (i2 >= limit) {
                        break;
                    }
                    i3 = positive.digits[i2] ^ (~negative.digits[i2]);
                    i = i3;
                    if (i3 != 0) {
                        break;
                    }
                }
                if (i == 0) {
                    while (i2 < positive.numberLength) {
                        i3 = ~positive.digits[i2];
                        i = i3;
                        if (i3 != 0) {
                            break;
                        }
                        i2++;
                    }
                    while (i2 < negative.numberLength) {
                        i3 = ~negative.digits[i2];
                        i = i3;
                        if (i3 != 0) {
                            break;
                        }
                        i2++;
                    }
                    if (i == 0) {
                        resLength++;
                        int[] resDigits2 = new int[resLength];
                        resDigits2[resLength - 1] = 1;
                        return new BigInteger(-1, resLength, resDigits2);
                    }
                }
            }
            int[] resDigits3 = new int[resLength];
            resDigits3[i2] = -i;
            i = i2 + 1;
            resDigits = resDigits3;
        }
        int limit2 = Math.min(negative.numberLength, positive.numberLength);
        while (i < limit2) {
            resDigits[i] = ~((~negative.digits[i]) ^ positive.digits[i]);
            i++;
        }
        while (i < positive.numberLength) {
            resDigits[i] = positive.digits[i];
            i++;
        }
        while (i < negative.numberLength) {
            resDigits[i] = negative.digits[i];
            i++;
        }
        return new BigInteger(-1, resLength, resDigits);
    }
}
