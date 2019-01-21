package android.icu.lang;

import android.icu.text.DateTimePatternGenerator;
import dalvik.bytecode.Opcodes;

@Deprecated
public class CharSequences {
    @Deprecated
    public static int matchAfter(CharSequence a, CharSequence b, int aIndex, int bIndex) {
        int i = aIndex;
        int j = bIndex;
        int alen = a.length();
        int blen = b.length();
        while (i < alen && j < blen && a.charAt(i) == b.charAt(j)) {
            i++;
            j++;
        }
        int result = i - aIndex;
        if (result == 0 || onCharacterBoundary(a, i) || onCharacterBoundary(b, j)) {
            return result;
        }
        return result - 1;
    }

    @Deprecated
    public int codePointLength(CharSequence s) {
        return Character.codePointCount(s, 0, s.length());
    }

    @Deprecated
    public static final boolean equals(int codepoint, CharSequence other) {
        boolean z = false;
        if (other == null) {
            return false;
        }
        switch (other.length()) {
            case 1:
                if (codepoint == other.charAt(0)) {
                    z = true;
                }
                return z;
            case 2:
                if (codepoint > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH && codepoint == Character.codePointAt(other, 0)) {
                    z = true;
                }
                return z;
            default:
                return false;
        }
    }

    @Deprecated
    public static final boolean equals(CharSequence other, int codepoint) {
        return equals(codepoint, other);
    }

    @Deprecated
    public static int compare(CharSequence string, int codePoint) {
        if (codePoint < 0 || codePoint > 1114111) {
            throw new IllegalArgumentException();
        }
        int stringLength = string.length();
        if (stringLength == 0) {
            return -1;
        }
        char firstChar = string.charAt(0);
        int offset = codePoint - 65536;
        if (offset < 0) {
            int result = firstChar - codePoint;
            if (result != 0) {
                return result;
            }
            return stringLength - 1;
        }
        int result2 = firstChar - ((char) ((offset >>> 10) + 55296));
        if (result2 != 0) {
            return result2;
        }
        if (stringLength > 1) {
            result2 = string.charAt(1) - ((char) ((offset & Opcodes.OP_NEW_INSTANCE_JUMBO) + UCharacter.MIN_LOW_SURROGATE));
            if (result2 != 0) {
                return result2;
            }
        }
        return stringLength - 2;
    }

    @Deprecated
    public static int compare(int codepoint, CharSequence a) {
        int result = compare(a, codepoint);
        if (result > 0) {
            return -1;
        }
        return result < 0 ? 1 : 0;
    }

    @Deprecated
    public static int getSingleCodePoint(CharSequence s) {
        int length = s.length();
        int i = Integer.MAX_VALUE;
        int i2 = 1;
        if (length < 1 || length > 2) {
            return Integer.MAX_VALUE;
        }
        int result = Character.codePointAt(s, 0);
        int i3 = result < 65536 ? 1 : 0;
        if (length != 1) {
            i2 = 0;
        }
        if (i3 == i2) {
            i = result;
        }
        return i;
    }

    @Deprecated
    public static final <T> boolean equals(T a, T b) {
        if (a == null) {
            return b == null;
        } else {
            if (b == null) {
                return false;
            }
            return a.equals(b);
        }
    }

    @Deprecated
    public static int compare(CharSequence a, CharSequence b) {
        int alength = a.length();
        int blength = b.length();
        int min = alength <= blength ? alength : blength;
        for (int i = 0; i < min; i++) {
            int diff = a.charAt(i) - b.charAt(i);
            if (diff != 0) {
                return diff;
            }
        }
        return alength - blength;
    }

    @Deprecated
    public static boolean equalsChars(CharSequence a, CharSequence b) {
        return a.length() == b.length() && compare(a, b) == 0;
    }

    @Deprecated
    public static boolean onCharacterBoundary(CharSequence s, int i) {
        if (i <= 0 || i >= s.length() || !Character.isHighSurrogate(s.charAt(i - 1)) || !Character.isLowSurrogate(s.charAt(i))) {
            return true;
        }
        return false;
    }

    @Deprecated
    public static int indexOf(CharSequence s, int codePoint) {
        int i = 0;
        while (i < s.length()) {
            int cp = Character.codePointAt(s, i);
            if (cp == codePoint) {
                return i;
            }
            i += Character.charCount(cp);
        }
        return -1;
    }

    @Deprecated
    public static int[] codePoints(CharSequence s) {
        int[] result = new int[s.length()];
        int j = 0;
        int i = 0;
        while (i < s.length()) {
            char cp = s.charAt(i);
            if (cp >= UCharacter.MIN_LOW_SURROGATE && cp <= 57343 && i != 0) {
                char last = (char) result[j - 1];
                if (last >= 55296 && last <= UCharacter.MAX_HIGH_SURROGATE) {
                    result[j - 1] = Character.toCodePoint(last, cp);
                    i++;
                }
            }
            int j2 = j + 1;
            result[j] = cp;
            j = j2;
            i++;
        }
        if (j == result.length) {
            return result;
        }
        int[] shortResult = new int[j];
        System.arraycopy(result, 0, shortResult, 0, j);
        return shortResult;
    }

    private CharSequences() {
    }
}
