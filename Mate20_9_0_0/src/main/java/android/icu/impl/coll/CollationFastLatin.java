package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.lang.UProperty;
import android.icu.text.DateTimePatternGenerator;
import dalvik.bytecode.Opcodes;

public final class CollationFastLatin {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int BAIL_OUT = 1;
    public static final int BAIL_OUT_RESULT = -2;
    static final int CASE_AND_TERTIARY_MASK = 31;
    static final int CASE_MASK = 24;
    static final int COMMON_SEC = 160;
    static final int COMMON_SEC_PLUS_OFFSET = 192;
    static final int COMMON_TER = 0;
    static final int COMMON_TER_PLUS_OFFSET = 32;
    static final int CONTRACTION = 1024;
    static final int CONTR_CHAR_MASK = 511;
    static final int CONTR_LENGTH_SHIFT = 9;
    static final int EOS = 2;
    static final int EXPANSION = 2048;
    static final int INDEX_MASK = 1023;
    public static final int LATIN_LIMIT = 384;
    public static final int LATIN_MAX = 383;
    static final int LATIN_MAX_UTF8_LEAD = 197;
    static final int LONG_INC = 8;
    static final int LONG_PRIMARY_MASK = 65528;
    static final int LOWER_CASE = 8;
    static final int MAX_LONG = 4088;
    static final int MAX_SEC_AFTER = 352;
    static final int MAX_SEC_BEFORE = 128;
    static final int MAX_SEC_HIGH = 992;
    static final int MAX_SHORT = 64512;
    static final int MAX_TER_AFTER = 7;
    static final int MERGE_WEIGHT = 3;
    static final int MIN_LONG = 3072;
    static final int MIN_SEC_AFTER = 192;
    static final int MIN_SEC_BEFORE = 0;
    static final int MIN_SEC_HIGH = 384;
    static final int MIN_SHORT = 4096;
    static final int NUM_FAST_CHARS = 448;
    static final int PUNCT_LIMIT = 8256;
    static final int PUNCT_START = 8192;
    static final int SECONDARY_MASK = 992;
    static final int SEC_INC = 32;
    static final int SEC_OFFSET = 32;
    static final int SHORT_INC = 1024;
    static final int SHORT_PRIMARY_MASK = 64512;
    static final int TERTIARY_MASK = 7;
    static final int TER_OFFSET = 32;
    static final int TWO_CASES_MASK = 1572888;
    static final int TWO_COMMON_SEC_PLUS_OFFSET = 12583104;
    static final int TWO_COMMON_TER_PLUS_OFFSET = 2097184;
    static final int TWO_LONG_PRIMARIES_MASK = -458760;
    static final int TWO_LOWER_CASES = 524296;
    static final int TWO_SECONDARIES_MASK = 65012704;
    static final int TWO_SEC_OFFSETS = 2097184;
    static final int TWO_SHORT_PRIMARIES_MASK = -67044352;
    static final int TWO_TERTIARIES_MASK = 458759;
    static final int TWO_TER_OFFSETS = 2097184;
    public static final int VERSION = 2;

    static int getCharIndex(char c) {
        if (c <= 383) {
            return c;
        }
        if (8192 > c || c >= 8256) {
            return -1;
        }
        return c - 7808;
    }

    public static int getOptions(CollationData data, CollationSettings settings, char[] primaries) {
        CollationData collationData = data;
        CollationSettings collationSettings = settings;
        char[] cArr = primaries;
        char[] header = collationData.fastLatinTableHeader;
        if (header == null || cArr.length != 384) {
            return -1;
        }
        int miniVarTop;
        int i;
        int c;
        if ((collationSettings.options & 12) == 0) {
            miniVarTop = Opcodes.OP_IGET_CHAR_JUMBO;
        } else {
            i = 1 + settings.getMaxVariable();
            if (i >= (header[0] & 255)) {
                return -1;
            }
            miniVarTop = header[i];
        }
        boolean digitsAreReordered = false;
        if (settings.hasReordering()) {
            long prevStart;
            long prevStart2 = 0;
            long beforeDigitStart = 0;
            long digitStart = 0;
            long afterDigitStart = 0;
            for (i = 4096; i < UProperty.LINE_BREAK; i++) {
                prevStart = collationSettings.reorder(collationData.getFirstPrimaryForGroup(i));
                if (i == 4100) {
                    beforeDigitStart = prevStart2;
                    digitStart = prevStart;
                } else if (prevStart == 0) {
                    continue;
                } else if (prevStart < prevStart2) {
                    return -1;
                } else {
                    if (digitStart != 0 && afterDigitStart == 0 && prevStart2 == beforeDigitStart) {
                        afterDigitStart = prevStart;
                    }
                    prevStart2 = prevStart;
                }
            }
            prevStart = collationSettings.reorder(collationData.getFirstPrimaryForGroup(25));
            if (prevStart < prevStart2) {
                return -1;
            }
            if (afterDigitStart == 0) {
                afterDigitStart = prevStart;
            }
            if (beforeDigitStart >= digitStart || digitStart >= afterDigitStart) {
                digitsAreReordered = true;
            }
        }
        char[] table = collationData.fastLatinTable;
        for (c = 0; c < 384; c++) {
            i = table[c];
            if (i >= 4096) {
                i &= Normalizer2Impl.MIN_NORMAL_MAYBE_YES;
            } else if (i > miniVarTop) {
                i &= LONG_PRIMARY_MASK;
            } else {
                i = 0;
            }
            cArr[c] = (char) i;
        }
        if (digitsAreReordered || (collationSettings.options & 2) != 0) {
            for (c = 48; c <= 57; c++) {
                cArr[c] = 0;
            }
        }
        return (miniVarTop << 16) | collationSettings.options;
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0059  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x00c3  */
    /* JADX WARNING: Removed duplicated region for block: B:325:0x00be A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int compareUTF16(char[] table, char[] primaries, int options, CharSequence left, CharSequence right, int startIndex) {
        int i;
        int i2;
        int leftIndex;
        int rightIndex;
        long pairAndInc;
        char[] cArr = table;
        CharSequence charSequence = left;
        CharSequence charSequence2 = right;
        int variableTop = options >> 16;
        int options2 = options & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
        int leftPair = 0;
        int rightIndex2 = startIndex;
        int leftIndex2 = startIndex;
        int rightPair = 0;
        while (true) {
            i = PUNCT_LIMIT;
            i2 = -2;
            if (leftPair != 0) {
            } else if (leftIndex2 == left.length()) {
                leftPair = 2;
            } else {
                leftIndex = leftIndex2 + 1;
                leftIndex2 = charSequence.charAt(leftIndex2);
                if (leftIndex2 <= LATIN_MAX) {
                    leftPair = primaries[leftIndex2];
                    if (leftPair == 0) {
                        if (leftIndex2 <= 57 && leftIndex2 >= 48 && (options2 & 2) != 0) {
                            return -2;
                        }
                        leftPair = cArr[leftIndex2];
                        if (leftPair < 4096) {
                            leftPair &= Normalizer2Impl.MIN_NORMAL_MAYBE_YES;
                        } else if (leftPair > variableTop) {
                            leftPair &= LONG_PRIMARY_MASK;
                        } else {
                            int rightPair2 = rightPair;
                            long pairAndInc2 = nextPair(cArr, leftIndex2, leftPair, charSequence, leftIndex);
                            if (pairAndInc2 < 0) {
                                leftIndex++;
                                pairAndInc2 = ~pairAndInc2;
                            }
                            leftPair = (int) pairAndInc2;
                            if (leftPair == 1) {
                                return -2;
                            }
                            leftPair = getPrimaries(variableTop, leftPair);
                            leftIndex2 = leftIndex;
                            rightPair = rightPair2;
                        }
                    }
                } else {
                    if (8192 > leftIndex2 || leftIndex2 >= PUNCT_LIMIT) {
                        leftPair = lookup(cArr, leftIndex2);
                    } else {
                        leftPair = cArr[(leftIndex2 - 8192) + 384];
                    }
                    if (leftPair < 4096) {
                    }
                }
                leftIndex2 = leftIndex;
            }
            while (rightPair == 0) {
                if (rightIndex2 != right.length()) {
                    rightIndex = rightIndex2 + 1;
                    rightIndex2 = charSequence2.charAt(rightIndex2);
                    if (rightIndex2 <= LATIN_MAX) {
                        rightPair = primaries[rightIndex2];
                        if (rightPair == 0) {
                            if (rightIndex2 <= 57 && rightIndex2 >= 48 && (options2 & 2) != 0) {
                                return i2;
                            }
                            rightPair = cArr[rightIndex2];
                            if (rightPair < 4096) {
                                rightPair &= Normalizer2Impl.MIN_NORMAL_MAYBE_YES;
                            } else if (rightPair > variableTop) {
                                rightPair &= LONG_PRIMARY_MASK;
                            } else {
                                pairAndInc = nextPair(cArr, rightIndex2, rightPair, charSequence2, rightIndex);
                                if (pairAndInc < 0) {
                                    rightIndex++;
                                    pairAndInc = ~pairAndInc;
                                }
                                rightPair = (int) pairAndInc;
                                if (rightPair == 1) {
                                    return -2;
                                }
                                rightPair = getPrimaries(variableTop, rightPair);
                                rightIndex2 = rightIndex;
                                i = PUNCT_LIMIT;
                                i2 = -2;
                            }
                        }
                    } else {
                        if (8192 > rightIndex2 || rightIndex2 >= i) {
                            rightPair = lookup(cArr, rightIndex2);
                        } else {
                            rightPair = cArr[(rightIndex2 - 8192) + 384];
                        }
                        if (rightPair < 4096) {
                        }
                    }
                    rightIndex2 = rightIndex;
                    break;
                }
                rightPair = 2;
                break;
            }
            if (leftPair != rightPair) {
                i2 = leftPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                leftIndex = rightPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                if (i2 != leftIndex) {
                    return i2 < leftIndex ? -1 : 1;
                } else if (leftPair == 2) {
                    break;
                } else {
                    leftPair >>>= 16;
                    rightPair >>>= 16;
                }
            } else if (leftPair == 2) {
                break;
            } else {
                rightPair = 0;
                leftPair = 0;
            }
        }
        if (CollationSettings.getStrength(options2) >= 1) {
            rightIndex2 = startIndex;
            leftIndex2 = startIndex;
            rightPair = 0;
            leftPair = 0;
            while (true) {
                if (leftPair == 0) {
                    if (leftIndex2 == left.length()) {
                        leftPair = 2;
                    } else {
                        leftIndex = leftIndex2 + 1;
                        leftIndex2 = charSequence.charAt(leftIndex2);
                        if (leftIndex2 <= LATIN_MAX) {
                            leftPair = cArr[leftIndex2];
                        } else if (8192 > leftIndex2 || leftIndex2 >= PUNCT_LIMIT) {
                            leftPair = lookup(cArr, leftIndex2);
                        } else {
                            leftPair = cArr[(leftIndex2 - 8192) + 384];
                        }
                        if (leftPair >= 4096) {
                            leftPair = getSecondariesFromOneShortCE(leftPair);
                        } else if (leftPair > variableTop) {
                            leftPair = 192;
                        } else {
                            pairAndInc = nextPair(cArr, leftIndex2, leftPair, charSequence, leftIndex);
                            if (pairAndInc < 0) {
                                leftIndex++;
                                pairAndInc = ~pairAndInc;
                            }
                            leftPair = getSecondaries(variableTop, (int) pairAndInc);
                            leftIndex2 = leftIndex;
                        }
                        leftIndex2 = leftIndex;
                    }
                }
                while (rightPair == 0) {
                    if (rightIndex2 != right.length()) {
                        rightIndex = rightIndex2 + 1;
                        rightIndex2 = charSequence2.charAt(rightIndex2);
                        if (rightIndex2 <= LATIN_MAX) {
                            rightPair = cArr[rightIndex2];
                        } else {
                            if (8192 <= rightIndex2) {
                                if (rightIndex2 < PUNCT_LIMIT) {
                                    rightPair = cArr[(rightIndex2 - 8192) + 384];
                                }
                            }
                            rightPair = lookup(cArr, rightIndex2);
                        }
                        if (rightPair >= 4096) {
                            rightPair = getSecondariesFromOneShortCE(rightPair);
                        } else if (rightPair > variableTop) {
                            rightPair = 192;
                        } else {
                            long pairAndInc3 = nextPair(cArr, rightIndex2, rightPair, charSequence2, rightIndex);
                            if (pairAndInc3 < 0) {
                                rightIndex++;
                                pairAndInc3 = ~pairAndInc3;
                            }
                            rightPair = getSecondaries(variableTop, (int) pairAndInc3);
                            rightIndex2 = rightIndex;
                        }
                        rightIndex2 = rightIndex;
                        break;
                    }
                    rightPair = 2;
                    break;
                }
                if (leftPair != rightPair) {
                    leftIndex = leftPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    i = rightPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    if (leftIndex != i) {
                        if ((options2 & 2048) != 0) {
                            return -2;
                        }
                        return leftIndex < i ? -1 : 1;
                    } else if (leftPair == 2) {
                        break;
                    } else {
                        leftPair >>>= 16;
                        rightPair >>>= 16;
                    }
                } else if (leftPair == 2) {
                    break;
                } else {
                    rightPair = 0;
                    leftPair = 0;
                }
            }
        }
        if ((options2 & 1024) != 0) {
            boolean strengthIsPrimary = CollationSettings.getStrength(options2) == 0;
            rightIndex2 = startIndex;
            leftIndex2 = startIndex;
            rightPair = 0;
            leftPair = 0;
            while (true) {
                if (leftPair == 0) {
                    if (leftIndex2 == left.length()) {
                        leftPair = 2;
                    } else {
                        leftIndex = leftIndex2 + 1;
                        leftIndex2 = charSequence.charAt(leftIndex2);
                        leftPair = leftIndex2 <= LATIN_MAX ? cArr[leftIndex2] : lookup(cArr, leftIndex2);
                        if (leftPair < MIN_LONG) {
                            pairAndInc = nextPair(cArr, leftIndex2, leftPair, charSequence, leftIndex);
                            if (pairAndInc < 0) {
                                leftIndex++;
                                pairAndInc = ~pairAndInc;
                            }
                            leftPair = (int) pairAndInc;
                        }
                        leftPair = getCases(variableTop, strengthIsPrimary, leftPair);
                        leftIndex2 = leftIndex;
                    }
                }
                while (rightPair == 0) {
                    if (rightIndex2 == right.length()) {
                        rightPair = 2;
                        break;
                    }
                    leftIndex = rightIndex2 + 1;
                    rightIndex2 = charSequence2.charAt(rightIndex2);
                    rightPair = rightIndex2 <= LATIN_MAX ? cArr[rightIndex2] : lookup(cArr, rightIndex2);
                    if (rightPair < MIN_LONG) {
                        pairAndInc = nextPair(cArr, rightIndex2, rightPair, charSequence2, leftIndex);
                        if (pairAndInc < 0) {
                            leftIndex++;
                            pairAndInc = ~pairAndInc;
                        }
                        rightPair = (int) pairAndInc;
                    }
                    rightPair = getCases(variableTop, strengthIsPrimary, rightPair);
                    rightIndex2 = leftIndex;
                }
                if (leftPair != rightPair) {
                    i = leftPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    i2 = rightPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    if (i != i2) {
                        if ((options2 & 256) == 0) {
                            return i < i2 ? -1 : 1;
                        }
                        return i < i2 ? 1 : -1;
                    } else if (leftPair == 2) {
                        break;
                    } else {
                        leftPair >>>= 16;
                        rightPair >>>= 16;
                    }
                } else if (leftPair == 2) {
                    break;
                } else {
                    rightPair = 0;
                    leftPair = 0;
                }
            }
        }
        if (CollationSettings.getStrength(options2) <= 1) {
            return 0;
        }
        int leftIndex3;
        leftIndex = CollationSettings.isTertiaryWithCaseBits(options2);
        rightIndex2 = startIndex;
        leftIndex2 = startIndex;
        rightPair = 0;
        leftPair = 0;
        while (true) {
            if (leftPair == 0) {
                if (leftIndex2 == left.length()) {
                    leftPair = 2;
                } else {
                    leftIndex3 = leftIndex2 + 1;
                    leftIndex2 = charSequence.charAt(leftIndex2);
                    leftPair = leftIndex2 <= LATIN_MAX ? cArr[leftIndex2] : lookup(cArr, leftIndex2);
                    if (leftPair < MIN_LONG) {
                        pairAndInc = nextPair(cArr, leftIndex2, leftPair, charSequence, leftIndex3);
                        if (pairAndInc < 0) {
                            leftIndex3++;
                            pairAndInc = ~pairAndInc;
                        }
                        leftPair = (int) pairAndInc;
                    }
                    leftPair = getTertiaries(variableTop, leftIndex, leftPair);
                    leftIndex2 = leftIndex3;
                }
            }
            while (rightPair == 0) {
                if (rightIndex2 == right.length()) {
                    rightPair = 2;
                    break;
                }
                leftIndex3 = rightIndex2 + 1;
                rightIndex2 = charSequence2.charAt(rightIndex2);
                rightPair = rightIndex2 <= LATIN_MAX ? cArr[rightIndex2] : lookup(cArr, rightIndex2);
                if (rightPair < MIN_LONG) {
                    pairAndInc = nextPair(cArr, rightIndex2, rightPair, charSequence2, leftIndex3);
                    if (pairAndInc < 0) {
                        leftIndex3++;
                        pairAndInc = ~pairAndInc;
                    }
                    rightPair = (int) pairAndInc;
                }
                rightPair = getTertiaries(variableTop, leftIndex, rightPair);
                rightIndex2 = leftIndex3;
            }
            if (leftPair == rightPair) {
                leftIndex3 = 2;
                if (leftPair == 2) {
                    break;
                }
                rightPair = 0;
                leftPair = 0;
            } else {
                i = leftPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                i2 = rightPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                if (i != i2) {
                    if (CollationSettings.sortsTertiaryUpperCaseFirst(options2)) {
                        if (i > 3) {
                            i ^= 24;
                        }
                        if (i2 > 3) {
                            i2 ^= 24;
                        }
                    }
                    return i < i2 ? -1 : 1;
                }
                leftIndex3 = 2;
                if (leftPair == 2) {
                    break;
                }
                leftPair >>>= 16;
                rightPair >>>= 16;
            }
        }
        if (CollationSettings.getStrength(options2) <= leftIndex3) {
            return 0;
        }
        rightIndex2 = startIndex;
        leftIndex2 = startIndex;
        rightPair = 0;
        leftPair = 0;
        while (true) {
            if (leftPair == 0) {
                if (leftIndex2 == left.length()) {
                    leftPair = 2;
                } else {
                    leftIndex3 = leftIndex2 + 1;
                    leftIndex2 = charSequence.charAt(leftIndex2);
                    leftPair = leftIndex2 <= LATIN_MAX ? cArr[leftIndex2] : lookup(cArr, leftIndex2);
                    if (leftPair < MIN_LONG) {
                        pairAndInc = nextPair(cArr, leftIndex2, leftPair, charSequence, leftIndex3);
                        if (pairAndInc < 0) {
                            leftIndex3++;
                            pairAndInc = ~pairAndInc;
                        }
                        leftPair = (int) pairAndInc;
                    }
                    leftPair = getQuaternaries(variableTop, leftPair);
                    leftIndex2 = leftIndex3;
                }
            }
            while (rightPair == 0) {
                if (rightIndex2 == right.length()) {
                    rightPair = 2;
                    break;
                }
                leftIndex3 = rightIndex2 + 1;
                rightIndex2 = charSequence2.charAt(rightIndex2);
                rightPair = rightIndex2 <= LATIN_MAX ? cArr[rightIndex2] : lookup(cArr, rightIndex2);
                if (rightPair < MIN_LONG) {
                    pairAndInc = nextPair(cArr, rightIndex2, rightPair, charSequence2, leftIndex3);
                    if (pairAndInc < 0) {
                        leftIndex3++;
                        pairAndInc = ~pairAndInc;
                    }
                    rightPair = (int) pairAndInc;
                }
                rightPair = getQuaternaries(variableTop, rightPair);
                rightIndex2 = leftIndex3;
            }
            if (leftPair != rightPair) {
                i = leftPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                i2 = rightPair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                if (i != i2) {
                    return i < i2 ? -1 : 1;
                } else if (leftPair == 2) {
                    break;
                } else {
                    leftPair >>>= 16;
                    rightPair >>>= 16;
                }
            } else if (leftPair == 2) {
                break;
            } else {
                rightPair = 0;
                leftPair = 0;
            }
        }
        return 0;
    }

    private static int lookup(char[] table, int c) {
        if (8192 <= c && c < PUNCT_LIMIT) {
            return table[(c - 8192) + 384];
        }
        if (c == 65534) {
            return 3;
        }
        if (c == DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            return 64680;
        }
        return 1;
    }

    private static long nextPair(char[] table, int c, int ce, CharSequence s16, int sIndex) {
        if (ce >= MIN_LONG || ce < 1024) {
            return (long) ce;
        }
        int index;
        if (ce >= 2048) {
            index = NUM_FAST_CHARS + (ce & 1023);
            return (((long) table[index + 1]) << 16) | ((long) table[index]);
        }
        int c2;
        index = NUM_FAST_CHARS + (ce & 1023);
        boolean inc = false;
        if (sIndex != s16.length()) {
            int x;
            c2 = sIndex;
            int nextIndex = c2 + 1;
            c2 = s16.charAt(c2);
            if (c2 > LATIN_MAX) {
                if (8192 <= c2 && c2 < PUNCT_LIMIT) {
                    c2 = (c2 - 8192) + 384;
                } else if (c2 != 65534 && c2 != DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                    return 1;
                } else {
                    c2 = -1;
                }
            }
            int i = index;
            int head = table[i];
            do {
                i += head >> 9;
                head = table[i];
                x = head & 511;
            } while (x < c2);
            if (x == c2) {
                index = i;
                inc = true;
            }
        }
        c2 = table[index] >> 9;
        if (c2 == 1) {
            return 1;
        }
        long result;
        ce = table[index + 1];
        if (c2 == 2) {
            result = (long) ce;
        } else {
            result = (((long) table[index + 2]) << 16) | ((long) ce);
        }
        return inc ? ~result : result;
    }

    private static int getPrimaries(int variableTop, int pair) {
        int ce = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & pair;
        if (ce >= 4096) {
            return TWO_SHORT_PRIMARIES_MASK & pair;
        }
        if (ce > variableTop) {
            return TWO_LONG_PRIMARIES_MASK & pair;
        }
        if (ce >= MIN_LONG) {
            return 0;
        }
        return pair;
    }

    private static int getSecondariesFromOneShortCE(int ce) {
        ce &= 992;
        if (ce < 384) {
            return ce + 32;
        }
        return ((ce + 32) << 16) | 192;
    }

    private static int getSecondaries(int variableTop, int pair) {
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            int ce = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & pair;
            if (ce >= 4096) {
                return (TWO_SECONDARIES_MASK & pair) + 2097184;
            }
            if (ce > variableTop) {
                return TWO_COMMON_SEC_PLUS_OFFSET;
            }
            return 0;
        } else if (pair >= 4096) {
            return getSecondariesFromOneShortCE(pair);
        } else {
            if (pair > variableTop) {
                return 192;
            }
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
    }

    private static int getCases(int variableTop, boolean strengthIsPrimary, int pair) {
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            int ce = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & pair;
            if (ce >= 4096) {
                if (strengthIsPrimary && (-67108864 & pair) == 0) {
                    return pair & 24;
                }
                return pair & TWO_CASES_MASK;
            } else if (ce > variableTop) {
                return TWO_LOWER_CASES;
            } else {
                return 0;
            }
        } else if (pair >= 4096) {
            int ce2 = pair;
            pair &= 24;
            if (strengthIsPrimary || (ce2 & 992) < 384) {
                return pair;
            }
            return pair | 524288;
        } else if (pair > variableTop) {
            return 8;
        } else {
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
    }

    private static int getTertiaries(int variableTop, boolean withCaseBits, int pair) {
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            int ce = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & pair;
            if (ce >= 4096) {
                if (withCaseBits) {
                    pair &= 2031647;
                } else {
                    pair &= TWO_TERTIARIES_MASK;
                }
                return pair + 2097184;
            } else if (ce <= variableTop) {
                return 0;
            } else {
                pair = (pair & TWO_TERTIARIES_MASK) + 2097184;
                if (withCaseBits) {
                    return pair | TWO_LOWER_CASES;
                }
                return pair;
            }
        } else if (pair >= 4096) {
            int pair2;
            int ce2 = pair;
            if (withCaseBits) {
                pair2 = (pair & 31) + 32;
                if ((ce2 & 992) >= 384) {
                    return 2621440 | pair2;
                }
            }
            pair2 = (pair & 7) + 32;
            if ((ce2 & 992) >= 384) {
                return 2097152 | pair2;
            }
            return pair2;
        } else if (pair > variableTop) {
            pair = (pair & 7) + 32;
            if (withCaseBits) {
                return pair | 8;
            }
            return pair;
        } else if (pair >= MIN_LONG) {
            return 0;
        } else {
            return pair;
        }
    }

    private static int getQuaternaries(int variableTop, int pair) {
        if (pair <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            if (pair >= 4096) {
                if ((pair & 992) >= 384) {
                    return TWO_SHORT_PRIMARIES_MASK;
                }
                return Normalizer2Impl.MIN_NORMAL_MAYBE_YES;
            } else if (pair > variableTop) {
                return Normalizer2Impl.MIN_NORMAL_MAYBE_YES;
            } else {
                if (pair >= MIN_LONG) {
                    return pair & LONG_PRIMARY_MASK;
                }
                return pair;
            }
        } else if ((DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & pair) > variableTop) {
            return TWO_SHORT_PRIMARIES_MASK;
        } else {
            return pair & TWO_LONG_PRIMARIES_MASK;
        }
    }

    private CollationFastLatin() {
    }
}
