package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.StringPrepParseException;
import android.icu.text.UTF16;

public final class Punycode {
    private static final int BASE = 36;
    private static final int CAPITAL_A = 65;
    private static final int CAPITAL_Z = 90;
    private static final int DAMP = 700;
    private static final char DELIMITER = '-';
    private static final char HYPHEN = '-';
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 128;
    private static final int SKEW = 38;
    private static final int SMALL_A = 97;
    private static final int SMALL_Z = 122;
    private static final int TMAX = 26;
    private static final int TMIN = 1;
    private static final int ZERO = 48;
    static final int[] basicToDigit = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private static int adaptBias(int delta, int length, boolean firstTime) {
        if (firstTime) {
            delta /= DAMP;
        } else {
            delta /= 2;
        }
        delta += delta / length;
        int count = 0;
        while (delta > 455) {
            delta /= 35;
            count += 36;
        }
        return ((36 * delta) / (delta + 38)) + count;
    }

    private static char asciiCaseMap(char b, boolean uppercase) {
        if (uppercase) {
            if ('a' > b || b > 'z') {
                return b;
            }
            return (char) (b - 32);
        } else if ('A' > b || b > 'Z') {
            return b;
        } else {
            return (char) (b + 32);
        }
    }

    private static char digitToBasic(int digit, boolean uppercase) {
        if (digit >= 26) {
            return (char) (22 + digit);
        }
        if (uppercase) {
            return (char) (65 + digit);
        }
        return (char) (97 + digit);
    }

    public static StringBuilder encode(CharSequence src, boolean[] caseFlags) throws StringPrepParseException {
        int srcCPCount;
        int srcCPCount2;
        CharSequence charSequence = src;
        int srcLength = src.length();
        int[] cpBuffer = new int[srcLength];
        StringBuilder dest = new StringBuilder(srcLength);
        int srcCPCount3 = 0;
        int j = 0;
        while (j < srcLength) {
            char c = charSequence.charAt(j);
            if (isBasic(c)) {
                srcCPCount = srcCPCount3 + 1;
                cpBuffer[srcCPCount3] = 0;
                dest.append(caseFlags != null ? asciiCaseMap(c, caseFlags[j]) : c);
                srcCPCount3 = srcCPCount;
            } else {
                srcCPCount = (caseFlags == null || !caseFlags[j]) ? 0 : 1;
                srcCPCount <<= 31;
                if (UTF16.isSurrogate(c)) {
                    if (UTF16.isLeadSurrogate(c) && j + 1 < srcLength) {
                        char charAt = charSequence.charAt(j + 1);
                        char c2 = charAt;
                        if (UTF16.isTrailSurrogate(charAt)) {
                            j++;
                            srcCPCount |= UCharacter.getCodePoint(c, c2);
                        }
                    }
                    throw new StringPrepParseException("Illegal char found", 1);
                }
                srcCPCount |= c;
                srcCPCount2 = srcCPCount3 + 1;
                cpBuffer[srcCPCount3] = srcCPCount;
                srcCPCount3 = srcCPCount2;
            }
            j++;
        }
        int basicLength = dest.length();
        if (basicLength > 0) {
            dest.append('-');
        }
        int bias = 72;
        int delta = 0;
        srcCPCount2 = 128;
        j = basicLength;
        while (j < srcCPCount3) {
            int i;
            int m = Integer.MAX_VALUE;
            srcCPCount = 0;
            while (true) {
                i = Integer.MAX_VALUE;
                if (srcCPCount >= srcCPCount3) {
                    break;
                }
                i = cpBuffer[srcCPCount] & Integer.MAX_VALUE;
                if (srcCPCount2 <= i && i < m) {
                    m = i;
                }
                srcCPCount++;
            }
            if (m - srcCPCount2 <= (Integer.MAX_VALUE - delta) / (j + 1)) {
                delta += (m - srcCPCount2) * (j + 1);
                int n = m;
                srcCPCount = 0;
                while (srcCPCount < srcCPCount3) {
                    int q = cpBuffer[srcCPCount] & i;
                    if (q < n) {
                        delta++;
                    } else if (q == n) {
                        i = delta;
                        q = 36;
                        while (true) {
                            srcCPCount2 = q - bias;
                            if (srcCPCount2 < 1) {
                                srcCPCount2 = 1;
                            } else if (q >= bias + 26) {
                                srcCPCount2 = 26;
                            }
                            if (i < srcCPCount2) {
                                break;
                            }
                            dest.append(digitToBasic(((i - srcCPCount2) % (36 - srcCPCount2)) + srcCPCount2, false));
                            i = (i - srcCPCount2) / (36 - srcCPCount2);
                            q += 36;
                            charSequence = src;
                        }
                        dest.append(digitToBasic(i, cpBuffer[srcCPCount] < 0));
                        bias = adaptBias(delta, j + 1, j == basicLength);
                        j++;
                        delta = 0;
                        q = i;
                    }
                    srcCPCount++;
                    charSequence = src;
                    i = Integer.MAX_VALUE;
                }
                delta++;
                srcCPCount2 = n + 1;
                n = 0;
                charSequence = src;
            } else {
                throw new IllegalStateException("Internal program error");
            }
        }
        return dest;
    }

    private static boolean isBasic(int ch) {
        return ch < 128;
    }

    private static boolean isBasicUpperCase(int ch) {
        return 65 <= ch && ch >= 90;
    }

    private static boolean isSurrogate(int ch) {
        return (ch & -2048) == 55296;
    }

    public static StringBuilder decode(CharSequence src, boolean[] caseFlags) throws StringPrepParseException {
        int i;
        int i2;
        CharSequence charSequence = src;
        Object obj = caseFlags;
        int srcLength = src.length();
        StringBuilder dest = new StringBuilder(src.length());
        int j = srcLength;
        while (j > 0) {
            j--;
            if (charSequence.charAt(j) == '-') {
                break;
            }
        }
        int destCPCount = j;
        int basicLength = j;
        j = 0;
        while (j < basicLength) {
            char b = charSequence.charAt(j);
            if (isBasic(b)) {
                dest.append(b);
                if (obj != null && j < obj.length) {
                    obj[j] = isBasicUpperCase(b);
                }
                j++;
            } else {
                throw new StringPrepParseException("Illegal char found", 0);
            }
        }
        int n = 128;
        int i3 = 0;
        int bias = 72;
        int firstSupplementaryIndex = 1000000000;
        int in = basicLength > 0 ? basicLength + 1 : 0;
        while (in < srcLength) {
            int oldi = i3;
            int w = 1;
            int i4 = i3;
            i3 = 36;
            while (in < srcLength) {
                int in2 = in + 1;
                in = basicToDigit[charSequence.charAt(in) & 255];
                if (in < 0) {
                    i = j;
                    i2 = basicLength;
                    throw new StringPrepParseException("Invalid char found", 0);
                } else if (in <= (Integer.MAX_VALUE - i4) / w) {
                    i4 += in * w;
                    int t = i3 - bias;
                    int srcLength2 = srcLength;
                    if (t < 1) {
                        t = 1;
                    } else if (i3 >= bias + 26) {
                        t = 26;
                    }
                    if (in < t) {
                        destCPCount++;
                        srcLength = i4 - oldi;
                        if (oldi == 0) {
                            i = j;
                            j = 1;
                        } else {
                            i = j;
                            j = 0;
                        }
                        bias = adaptBias(srcLength, destCPCount, j);
                        if (i4 / destCPCount <= Integer.MAX_VALUE - n) {
                            n += i4 / destCPCount;
                            i4 %= destCPCount;
                            if (n > 1114111 || isSurrogate(n) != 0) {
                                i2 = basicLength;
                                throw new StringPrepParseException("Illegal char found", 1);
                            }
                            int destCPCount2;
                            srcLength = Character.charCount(n);
                            if (i4 <= firstSupplementaryIndex) {
                                j = i4;
                                destCPCount2 = destCPCount;
                                if (srcLength > 1) {
                                    firstSupplementaryIndex = j;
                                } else {
                                    firstSupplementaryIndex++;
                                }
                            } else {
                                destCPCount2 = destCPCount;
                                j = dest.offsetByCodePoints(firstSupplementaryIndex, i4 - firstSupplementaryIndex);
                            }
                            if (obj != null) {
                                i2 = basicLength;
                                if (dest.length() + srcLength <= obj.length) {
                                    if (j < dest.length()) {
                                        System.arraycopy(obj, j, obj, j + srcLength, dest.length() - j);
                                    }
                                    obj[j] = isBasicUpperCase(charSequence.charAt(in2 - 1));
                                    if (srcLength == 2) {
                                        obj[j + 1] = false;
                                    }
                                }
                            } else {
                                i2 = basicLength;
                            }
                            if (srcLength == 1) {
                                dest.insert(j, (char) n);
                            } else {
                                dest.insert(j, UTF16.getLeadSurrogate(n));
                                dest.insert(j + 1, UTF16.getTrailSurrogate(n));
                            }
                            i3 = i4 + 1;
                            in = in2;
                            srcLength = srcLength2;
                            j = i;
                            destCPCount = destCPCount2;
                            basicLength = i2;
                        } else {
                            i2 = basicLength;
                            throw new StringPrepParseException("Illegal char found", 1);
                        }
                    }
                    i = j;
                    i2 = basicLength;
                    if (w <= Integer.MAX_VALUE / (36 - t)) {
                        w *= 36 - t;
                        i3 += 36;
                        in = in2;
                        srcLength = srcLength2;
                        j = i;
                        basicLength = i2;
                    } else {
                        throw new StringPrepParseException("Illegal char found", 1);
                    }
                } else {
                    i = j;
                    i2 = basicLength;
                    throw new StringPrepParseException("Illegal char found", 1);
                }
            }
            i = j;
            i2 = basicLength;
            throw new StringPrepParseException("Illegal char found", 1);
        }
        i = j;
        i2 = basicLength;
        return dest;
    }
}
