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
        return ((delta * 36) / (delta + 38)) + count;
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
            return (char) (digit + 22);
        }
        if (uppercase) {
            return (char) (digit + 65);
        }
        return (char) (digit + 97);
    }

    public static StringBuilder encode(CharSequence src, boolean[] caseFlags) throws StringPrepParseException {
        int n;
        int srcLength = src.length();
        int[] cpBuffer = new int[srcLength];
        StringBuilder dest = new StringBuilder(srcLength);
        int j = 0;
        int srcCPCount = 0;
        while (j < srcLength) {
            int srcCPCount2;
            char c = src.charAt(j);
            if (isBasic(c)) {
                srcCPCount2 = srcCPCount + 1;
                cpBuffer[srcCPCount] = 0;
                if (caseFlags != null) {
                    c = asciiCaseMap(c, caseFlags[j]);
                }
                dest.append(c);
            } else {
                int i = (caseFlags == null || !caseFlags[j]) ? 0 : 1;
                n = i << 31;
                if (UTF16.isSurrogate(c)) {
                    if (UTF16.isLeadSurrogate(c) && j + 1 < srcLength) {
                        char c2 = src.charAt(j + 1);
                        if (UTF16.isTrailSurrogate(c2)) {
                            j++;
                            n |= UCharacter.getCodePoint(c, c2);
                        }
                    }
                    throw new StringPrepParseException("Illegal char found", 1);
                }
                n |= c;
                srcCPCount2 = srcCPCount + 1;
                cpBuffer[srcCPCount] = n;
            }
            j++;
            srcCPCount = srcCPCount2;
        }
        int basicLength = dest.length();
        if (basicLength > 0) {
            dest.append('-');
        }
        n = 128;
        int delta = 0;
        int bias = 72;
        int handledCPCount = basicLength;
        while (handledCPCount < srcCPCount) {
            int m = Integer.MAX_VALUE;
            for (j = 0; j < srcCPCount; j++) {
                int q = cpBuffer[j] & Integer.MAX_VALUE;
                if (n <= q && q < m) {
                    m = q;
                }
            }
            if (m - n > (Integer.MAX_VALUE - delta) / (handledCPCount + 1)) {
                throw new IllegalStateException("Internal program error");
            }
            delta += (m - n) * (handledCPCount + 1);
            n = m;
            for (j = 0; j < srcCPCount; j++) {
                q = cpBuffer[j] & Integer.MAX_VALUE;
                if (q < n) {
                    delta++;
                } else if (q == n) {
                    q = delta;
                    int k = 36;
                    while (true) {
                        int t = k - bias;
                        if (t < 1) {
                            t = 1;
                        } else if (k >= bias + 26) {
                            t = 26;
                        }
                        if (q < t) {
                            break;
                        }
                        dest.append(digitToBasic(((q - t) % (36 - t)) + t, false));
                        q = (q - t) / (36 - t);
                        k += 36;
                    }
                    dest.append(digitToBasic(q, cpBuffer[j] < 0));
                    bias = adaptBias(delta, handledCPCount + 1, handledCPCount == basicLength);
                    delta = 0;
                    handledCPCount++;
                }
            }
            delta++;
            n++;
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
        int srcLength = src.length();
        StringBuilder dest = new StringBuilder(src.length());
        int j = srcLength;
        while (j > 0) {
            j--;
            if (src.charAt(j) == '-') {
                break;
            }
        }
        int destCPCount = j;
        int basicLength = j;
        j = 0;
        while (j < basicLength) {
            char b = src.charAt(j);
            if (isBasic(b)) {
                dest.append(b);
                if (caseFlags != null && j < caseFlags.length) {
                    caseFlags[j] = isBasicUpperCase(b);
                }
                j++;
            } else {
                throw new StringPrepParseException("Illegal char found", 0);
            }
        }
        int n = 128;
        int i = 0;
        int bias = 72;
        int firstSupplementaryIndex = 1000000000;
        int in = basicLength > 0 ? basicLength + 1 : 0;
        while (in < srcLength) {
            int oldi = i;
            int w = 1;
            int k = 36;
            int in2 = in;
            while (in2 < srcLength) {
                in = in2 + 1;
                int digit = basicToDigit[src.charAt(in2) & 255];
                if (digit < 0) {
                    throw new StringPrepParseException("Invalid char found", 0);
                } else if (digit > (Integer.MAX_VALUE - i) / w) {
                    throw new StringPrepParseException("Illegal char found", 1);
                } else {
                    i += digit * w;
                    int t = k - bias;
                    if (t < 1) {
                        t = 1;
                    } else if (k >= bias + 26) {
                        t = 26;
                    }
                    if (digit < t) {
                        destCPCount++;
                        bias = adaptBias(i - oldi, destCPCount, oldi == 0);
                        if (i / destCPCount > Integer.MAX_VALUE - n) {
                            throw new StringPrepParseException("Illegal char found", 1);
                        }
                        n += i / destCPCount;
                        i %= destCPCount;
                        if (n > 1114111 || isSurrogate(n)) {
                            throw new StringPrepParseException("Illegal char found", 1);
                        }
                        int codeUnitIndex;
                        int cpLength = Character.charCount(n);
                        if (i <= firstSupplementaryIndex) {
                            codeUnitIndex = i;
                            if (cpLength > 1) {
                                firstSupplementaryIndex = i;
                            } else {
                                firstSupplementaryIndex++;
                            }
                        } else {
                            codeUnitIndex = dest.offsetByCodePoints(firstSupplementaryIndex, i - firstSupplementaryIndex);
                        }
                        if (caseFlags != null && dest.length() + cpLength <= caseFlags.length) {
                            if (codeUnitIndex < dest.length()) {
                                System.arraycopy(caseFlags, codeUnitIndex, caseFlags, codeUnitIndex + cpLength, dest.length() - codeUnitIndex);
                            }
                            caseFlags[codeUnitIndex] = isBasicUpperCase(src.charAt(in - 1));
                            if (cpLength == 2) {
                                caseFlags[codeUnitIndex + 1] = false;
                            }
                        }
                        if (cpLength == 1) {
                            dest.insert(codeUnitIndex, (char) n);
                        } else {
                            dest.insert(codeUnitIndex, UTF16.getLeadSurrogate(n));
                            dest.insert(codeUnitIndex + 1, UTF16.getTrailSurrogate(n));
                        }
                        i++;
                    } else if (w > Integer.MAX_VALUE / (36 - t)) {
                        throw new StringPrepParseException("Illegal char found", 1);
                    } else {
                        w *= 36 - t;
                        k += 36;
                        in2 = in;
                    }
                }
            }
            throw new StringPrepParseException("Illegal char found", 1);
        }
        return dest;
    }
}
