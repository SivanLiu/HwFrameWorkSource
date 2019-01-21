package android.icu.impl;

import android.icu.impl.locale.LanguageTag;
import android.icu.lang.UCharacter;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Replaceable;
import android.icu.text.UTF16;
import android.icu.text.UnicodeMatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Utility {
    private static final char APOSTROPHE = '\'';
    private static final char BACKSLASH = '\\';
    static final char[] DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    private static final char ESCAPE = 'ꖥ';
    static final byte ESCAPE_BYTE = (byte) -91;
    static final char[] HEX_DIGIT = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int MAGIC_UNSIGNED = Integer.MIN_VALUE;
    private static final char[] UNESCAPE_MAP = new char[]{'a', 7, 'b', 8, 'e', 27, 'f', 12, 'n', 10, 'r', 13, 't', 9, 'v', 11};

    public static final boolean arrayEquals(Object[] source, Object target) {
        boolean z = true;
        if (source == null) {
            if (target != null) {
                z = false;
            }
            return z;
        } else if (!(target instanceof Object[])) {
            return false;
        } else {
            Object[] targ = (Object[]) target;
            if (!(source.length == targ.length && arrayRegionMatches(source, 0, targ, 0, source.length))) {
                z = false;
            }
            return z;
        }
    }

    public static final boolean arrayEquals(int[] source, Object target) {
        boolean z = true;
        if (source == null) {
            if (target != null) {
                z = false;
            }
            return z;
        } else if (!(target instanceof int[])) {
            return false;
        } else {
            int[] targ = (int[]) target;
            if (!(source.length == targ.length && arrayRegionMatches(source, 0, targ, 0, source.length))) {
                z = false;
            }
            return z;
        }
    }

    public static final boolean arrayEquals(double[] source, Object target) {
        boolean z = true;
        if (source == null) {
            if (target != null) {
                z = false;
            }
            return z;
        } else if (!(target instanceof double[])) {
            return false;
        } else {
            double[] targ = (double[]) target;
            if (!(source.length == targ.length && arrayRegionMatches(source, 0, targ, 0, source.length))) {
                z = false;
            }
            return z;
        }
    }

    public static final boolean arrayEquals(byte[] source, Object target) {
        boolean z = true;
        if (source == null) {
            if (target != null) {
                z = false;
            }
            return z;
        } else if (!(target instanceof byte[])) {
            return false;
        } else {
            byte[] targ = (byte[]) target;
            if (!(source.length == targ.length && arrayRegionMatches(source, 0, targ, 0, source.length))) {
                z = false;
            }
            return z;
        }
    }

    public static final boolean arrayEquals(Object source, Object target) {
        if (source == null) {
            return target == null;
        } else if (source instanceof Object[]) {
            return arrayEquals((Object[]) source, target);
        } else {
            if (source instanceof int[]) {
                return arrayEquals((int[]) source, target);
            }
            if (source instanceof double[]) {
                return arrayEquals((double[]) source, target);
            }
            if (source instanceof byte[]) {
                return arrayEquals((byte[]) source, target);
            }
            return source.equals(target);
        }
    }

    public static final boolean arrayRegionMatches(Object[] source, int sourceStart, Object[] target, int targetStart, int len) {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (!arrayEquals(source[i], target[i + delta])) {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(char[] source, int sourceStart, char[] target, int targetStart, int len) {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta]) {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(int[] source, int sourceStart, int[] target, int targetStart, int len) {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta]) {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(double[] source, int sourceStart, double[] target, int targetStart, int len) {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta]) {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(byte[] source, int sourceStart, byte[] target, int targetStart, int len) {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta]) {
                return false;
            }
        }
        return true;
    }

    public static final boolean sameObjects(Object a, Object b) {
        return a == b;
    }

    public static final boolean objectEquals(Object a, Object b) {
        if (a == null) {
            if (b == null) {
                return true;
            }
            return false;
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static <T extends Comparable<T>> int checkCompare(T a, T b) {
        return a == null ? b == null ? 0 : -1 : b == null ? 1 : a.compareTo(b);
    }

    public static int checkHash(Object a) {
        return a == null ? 0 : a.hashCode();
    }

    public static final String arrayToRLEString(int[] a) {
        Appendable buffer = new StringBuilder();
        appendInt(buffer, a.length);
        int runValue = a[0];
        int runLength = 1;
        for (int i = 1; i < a.length; i++) {
            int s = a[i];
            if (s != runValue || runLength >= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                encodeRun(buffer, runValue, runLength);
                runValue = s;
                runLength = 1;
            } else {
                runLength++;
            }
        }
        encodeRun(buffer, runValue, runLength);
        return buffer.toString();
    }

    public static final String arrayToRLEString(short[] a) {
        Appendable buffer = new StringBuilder();
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        short runValue = a[(short) 0];
        int runLength = 1;
        for (int i = 1; i < a.length; i++) {
            short s = a[i];
            if (s != runValue || runLength >= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                encodeRun(buffer, runValue, runLength);
                runValue = s;
                runLength = 1;
            } else {
                runLength++;
            }
        }
        encodeRun(buffer, runValue, runLength);
        return buffer.toString();
    }

    public static final String arrayToRLEString(char[] a) {
        Appendable buffer = new StringBuilder();
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        char runValue = a[0];
        int runLength = 1;
        for (int i = 1; i < a.length; i++) {
            char s = a[i];
            if (s != runValue || runLength >= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                encodeRun(buffer, (short) runValue, runLength);
                runValue = s;
                runLength = 1;
            } else {
                runLength++;
            }
        }
        encodeRun(buffer, (short) runValue, runLength);
        return buffer.toString();
    }

    public static final String arrayToRLEString(byte[] a) {
        StringBuilder buffer = new StringBuilder();
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        byte runValue = a[0];
        int runLength = 1;
        byte[] state = new byte[2];
        for (int i = 1; i < a.length; i++) {
            byte b = a[i];
            if (b != runValue || runLength >= 255) {
                encodeRun(buffer, runValue, runLength, state);
                runValue = b;
                runLength = 1;
            } else {
                runLength++;
            }
        }
        encodeRun(buffer, runValue, runLength, state);
        if (state[0] != (byte) 0) {
            appendEncodedByte(buffer, (byte) 0, state);
        }
        return buffer.toString();
    }

    private static final <T extends Appendable> void encodeRun(T buffer, int value, int length) {
        if (length < 4) {
            for (int j = 0; j < length; j++) {
                if (value == 42405) {
                    appendInt(buffer, value);
                }
                appendInt(buffer, value);
            }
            return;
        }
        if (length == 42405) {
            if (value == 42405) {
                appendInt(buffer, 42405);
            }
            appendInt(buffer, value);
            length--;
        }
        appendInt(buffer, 42405);
        appendInt(buffer, length);
        appendInt(buffer, value);
    }

    private static final <T extends Appendable> void appendInt(T buffer, int value) {
        try {
            buffer.append((char) (value >>> 16));
            buffer.append((char) (DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & value));
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    private static final <T extends Appendable> void encodeRun(T buffer, short value, int length) {
        char valueChar = (char) value;
        if (length < 4) {
            for (int j = 0; j < length; j++) {
                if (valueChar == ESCAPE) {
                    try {
                        buffer.append(ESCAPE);
                    } catch (IOException e) {
                        throw new IllegalIcuArgumentException(e);
                    }
                }
                buffer.append(valueChar);
            }
            return;
        }
        if (length == 42405) {
            if (valueChar == ESCAPE) {
                buffer.append(ESCAPE);
            }
            buffer.append(valueChar);
            length--;
        }
        buffer.append(ESCAPE);
        buffer.append((char) length);
        buffer.append(valueChar);
    }

    private static final <T extends Appendable> void encodeRun(T buffer, byte value, int length, byte[] state) {
        if (length < 4) {
            for (int j = 0; j < length; j++) {
                if (value == ESCAPE_BYTE) {
                    appendEncodedByte(buffer, ESCAPE_BYTE, state);
                }
                appendEncodedByte(buffer, value, state);
            }
            return;
        }
        if (((byte) length) == ESCAPE_BYTE) {
            if (value == ESCAPE_BYTE) {
                appendEncodedByte(buffer, ESCAPE_BYTE, state);
            }
            appendEncodedByte(buffer, value, state);
            length--;
        }
        appendEncodedByte(buffer, ESCAPE_BYTE, state);
        appendEncodedByte(buffer, (byte) length, state);
        appendEncodedByte(buffer, value, state);
    }

    private static final <T extends Appendable> void appendEncodedByte(T buffer, byte value, byte[] state) {
        try {
            if (state[0] != (byte) 0) {
                buffer.append((char) ((state[1] << 8) | (value & 255)));
                state[0] = (byte) 0;
                return;
            }
            state[0] = (byte) 1;
            state[1] = value;
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static final int[] RLEStringToIntArray(String s) {
        int length = getInt(s, 0);
        int[] array = new int[length];
        int ai = 0;
        int i = 1;
        int maxI = s.length() / 2;
        while (ai < length && i < maxI) {
            int i2 = i + 1;
            i = getInt(s, i);
            int i3;
            if (i == 42405) {
                int runValue = i2 + 1;
                i = getInt(s, i2);
                if (i == 42405) {
                    i2 = ai + 1;
                    array[ai] = i;
                    ai = i2;
                    i = runValue;
                } else {
                    i2 = i;
                    i3 = runValue + 1;
                    runValue = getInt(s, runValue);
                    int ai2 = ai;
                    ai = 0;
                    while (ai < i2) {
                        int ai3 = ai2 + 1;
                        array[ai2] = runValue;
                        ai++;
                        ai2 = ai3;
                    }
                    i = i3;
                    ai = ai2;
                }
            } else {
                i3 = ai + 1;
                array[ai] = i;
                i = i2;
                ai = i3;
            }
        }
        if (ai == length && i == maxI) {
            return array;
        }
        throw new IllegalStateException("Bad run-length encoded int array");
    }

    static final int getInt(String s, int i) {
        return s.charAt((2 * i) + 1) | (s.charAt(2 * i) << 16);
    }

    public static final short[] RLEStringToShortArray(String s) {
        int length = (s.charAt(0) << 16) | s.charAt(1);
        short[] array = new short[length];
        int ai = 0;
        int i = 2;
        while (i < s.length()) {
            int ai2;
            char c = s.charAt(i);
            if (c == ESCAPE) {
                i++;
                c = s.charAt(i);
                if (c == ESCAPE) {
                    ai2 = ai + 1;
                    array[ai] = (short) c;
                } else {
                    char runLength = c;
                    i++;
                    short runValue = (short) s.charAt(i);
                    int ai3 = ai;
                    char ai4 = 0;
                    while (ai4 < runLength) {
                        int ai5 = ai3 + 1;
                        array[ai3] = runValue;
                        ai4++;
                        ai3 = ai5;
                    }
                    ai = ai3;
                    i++;
                }
            } else {
                ai2 = ai + 1;
                array[ai] = (short) c;
            }
            ai = ai2;
            i++;
        }
        if (ai == length) {
            return array;
        }
        throw new IllegalStateException("Bad run-length encoded short array");
    }

    public static final char[] RLEStringToCharArray(String s) {
        int length = (s.charAt(0) << 16) | s.charAt(1);
        char[] array = new char[length];
        int ai = 0;
        int i = 2;
        while (i < s.length()) {
            int ai2;
            char c = s.charAt(i);
            if (c == ESCAPE) {
                i++;
                c = s.charAt(i);
                if (c == ESCAPE) {
                    ai2 = ai + 1;
                    array[ai] = c;
                } else {
                    char runLength = c;
                    i++;
                    char runValue = s.charAt(i);
                    int ai3 = ai;
                    char ai4 = 0;
                    while (ai4 < runLength) {
                        int ai5 = ai3 + 1;
                        array[ai3] = runValue;
                        ai4++;
                        ai3 = ai5;
                    }
                    ai = ai3;
                    i++;
                }
            } else {
                ai2 = ai + 1;
                array[ai] = c;
            }
            ai = ai2;
            i++;
        }
        if (ai == length) {
            return array;
        }
        throw new IllegalStateException("Bad run-length encoded short array");
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static final byte[] RLEStringToByteArray(String s) {
        int length = (s.charAt(0) << 16) | s.charAt(1);
        byte[] array = new byte[length];
        int node = 0;
        int i = 2;
        int runLength = 0;
        char c = 0;
        boolean nextChar = true;
        int nextChar2 = 0;
        while (nextChar2 < length) {
            int i2;
            byte b;
            int ai;
            if (nextChar) {
                i2 = i + 1;
                c = s.charAt(i);
                b = (byte) (c >> 8);
                nextChar = false;
            } else {
                nextChar = true;
                i2 = i;
                b = (byte) (c & 255);
            }
            int ai2;
            switch (node) {
                case 0:
                    if (b == ESCAPE_BYTE) {
                        node = 1;
                        continue;
                    } else {
                        ai = nextChar2 + 1;
                        array[nextChar2] = b;
                    }
                case 1:
                    if (b != ESCAPE_BYTE) {
                        runLength = b;
                        if (runLength < (byte) 0) {
                            runLength += 256;
                        }
                        node = 2;
                        break;
                    }
                    ai2 = nextChar2 + 1;
                    array[nextChar2] = ESCAPE_BYTE;
                    node = 0;
                    nextChar2 = ai2;
                    continue;
                case 2:
                    ai = nextChar2;
                    nextChar2 = 0;
                    while (nextChar2 < runLength) {
                        ai2 = ai + 1;
                        array[ai] = b;
                        nextChar2++;
                        ai = ai2;
                    }
                    node = 0;
                    nextChar2 = ai;
                    break;
                default:
                    break;
            }
            nextChar2 = ai;
            i = i2;
        }
        if (node != 0) {
            throw new IllegalStateException("Bad run-length encoded byte array");
        } else if (i == s.length()) {
            return array;
        } else {
            throw new IllegalStateException("Excess data in RLE byte array string");
        }
    }

    public static final String formatForSource(String s) {
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (i > 0) {
                buffer.append('+');
                buffer.append(LINE_SEPARATOR);
            }
            buffer.append("        \"");
            int count = 11;
            while (i < s.length() && count < 80) {
                int i2 = i + 1;
                char c = s.charAt(i);
                if (c < ' ' || c == '\"' || c == '\\') {
                    if (c == 10) {
                        buffer.append("\\n");
                        count += 2;
                    } else if (c == 9) {
                        buffer.append("\\t");
                        count += 2;
                    } else if (c == 13) {
                        buffer.append("\\r");
                        count += 2;
                    } else {
                        buffer.append('\\');
                        buffer.append(HEX_DIGIT[(c & 448) >> 6]);
                        buffer.append(HEX_DIGIT[(c & 56) >> 3]);
                        buffer.append(HEX_DIGIT[c & 7]);
                        count += 4;
                    }
                } else if (c <= '~') {
                    buffer.append(c);
                    count++;
                } else {
                    buffer.append("\\u");
                    buffer.append(HEX_DIGIT[(61440 & c) >> 12]);
                    buffer.append(HEX_DIGIT[(c & 3840) >> 8]);
                    buffer.append(HEX_DIGIT[(c & 240) >> 4]);
                    buffer.append(HEX_DIGIT[c & 15]);
                    count += 6;
                }
                i = i2;
            }
            buffer.append('\"');
        }
        return buffer.toString();
    }

    public static final String format1ForSource(String s) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\"");
        char c = 0;
        while (c < s.length()) {
            char i = c + 1;
            c = s.charAt(c);
            if (c < ' ' || c == '\"' || c == '\\') {
                if (c == 10) {
                    buffer.append("\\n");
                } else if (c == 9) {
                    buffer.append("\\t");
                } else if (c == 13) {
                    buffer.append("\\r");
                } else {
                    buffer.append('\\');
                    buffer.append(HEX_DIGIT[(c & 448) >> 6]);
                    buffer.append(HEX_DIGIT[(c & 56) >> 3]);
                    buffer.append(HEX_DIGIT[c & 7]);
                }
            } else if (c <= '~') {
                buffer.append(c);
            } else {
                buffer.append("\\u");
                buffer.append(HEX_DIGIT[(61440 & c) >> 12]);
                buffer.append(HEX_DIGIT[(c & 3840) >> 8]);
                buffer.append(HEX_DIGIT[(c & 240) >> 4]);
                buffer.append(HEX_DIGIT[c & 15]);
            }
            c = i;
        }
        buffer.append('\"');
        return buffer.toString();
    }

    public static final String escape(String s) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int c = Character.codePointAt(s, i);
            i += UTF16.getCharCount(c);
            if (c < 32 || c > 127) {
                boolean four = c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                buf.append(four ? "\\u" : "\\U");
                buf.append(hex((long) c, four ? 4 : 8));
            } else if (c == 92) {
                buf.append("\\\\");
            } else {
                buf.append((char) c);
            }
        }
        return buf.toString();
    }

    public static int unescapeAt(String s, int[] offset16) {
        int result = 0;
        int n = 0;
        int minDig = 0;
        int maxDig = 0;
        int bitsPerDigit = 4;
        boolean braces = false;
        int offset = offset16[0];
        int length = s.length();
        if (offset < 0 || offset >= length) {
            return -1;
        }
        int dig;
        char c = Character.codePointAt(s, offset);
        offset += UTF16.getCharCount(c);
        if (c == 'U') {
            maxDig = 8;
            minDig = 8;
        } else if (c == 'u') {
            maxDig = 4;
            minDig = 4;
        } else if (c != 'x') {
            dig = UCharacter.digit(c, 8);
            if (dig >= 0) {
                minDig = 1;
                maxDig = 3;
                n = 1;
                bitsPerDigit = 3;
                result = dig;
            }
        } else {
            minDig = 1;
            if (offset >= length || UTF16.charAt(s, offset) != 123) {
                maxDig = 2;
            } else {
                offset++;
                braces = true;
                maxDig = 8;
            }
        }
        int c2;
        int ahead;
        if (minDig != 0) {
            while (offset < length && n < maxDig) {
                c2 = UTF16.charAt(s, offset);
                dig = UCharacter.digit(c2, bitsPerDigit == 3 ? 8 : 16);
                if (dig < 0) {
                    break;
                }
                result = (result << bitsPerDigit) | dig;
                offset += UTF16.getCharCount(c2);
                n++;
            }
            if (n < minDig) {
                return -1;
            }
            if (braces) {
                if (c2 != 125) {
                    return -1;
                }
                offset++;
            }
            if (result < 0 || result >= 1114112) {
                return -1;
            }
            if (offset < length && UTF16.isLeadSurrogate((char) result)) {
                ahead = offset + 1;
                c2 = s.charAt(offset);
                if (c2 == 92 && ahead < length) {
                    int[] o = new int[]{ahead};
                    c2 = unescapeAt(s, o);
                    ahead = o[0];
                }
                if (UTF16.isTrailSurrogate((char) c2)) {
                    offset = ahead;
                    result = Character.toCodePoint((char) result, (char) c2);
                }
            }
            offset16[0] = offset;
            return result;
        }
        ahead = 0;
        while (ahead < UNESCAPE_MAP.length) {
            if (c == UNESCAPE_MAP[ahead]) {
                offset16[0] = offset;
                return UNESCAPE_MAP[ahead + 1];
            } else if (c < UNESCAPE_MAP[ahead]) {
                break;
            } else {
                ahead += 2;
            }
        }
        if (c != 'c' || offset >= length) {
            offset16[0] = offset;
            return c;
        }
        c2 = UTF16.charAt(s, offset);
        offset16[0] = UTF16.getCharCount(c2) + offset;
        return 31 & c2;
    }

    public static String unescape(String s) {
        StringBuilder buf = new StringBuilder();
        int[] pos = new int[1];
        char c = 0;
        while (c < s.length()) {
            char i = c + 1;
            c = s.charAt(c);
            if (c == '\\') {
                pos[0] = i;
                int e = unescapeAt(s, pos);
                if (e >= 0) {
                    buf.appendCodePoint(e);
                    i = pos[0];
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid escape sequence ");
                    stringBuilder.append(s.substring(i - 1, Math.min(i + 8, s.length())));
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            buf.append(c);
            c = i;
        }
        return buf.toString();
    }

    public static String unescapeLeniently(String s) {
        StringBuilder buf = new StringBuilder();
        int[] pos = new int[1];
        char c = 0;
        while (c < s.length()) {
            char i = c + 1;
            c = s.charAt(c);
            if (c == '\\') {
                pos[0] = i;
                int e = unescapeAt(s, pos);
                if (e < 0) {
                    buf.append(c);
                } else {
                    buf.appendCodePoint(e);
                    i = pos[0];
                }
            } else {
                buf.append(c);
            }
            c = i;
        }
        return buf.toString();
    }

    public static String hex(long ch) {
        return hex(ch, 4);
    }

    public static String hex(long i, int places) {
        if (i == Long.MIN_VALUE) {
            return "-8000000000000000";
        }
        StringBuilder stringBuilder;
        boolean negative = i < 0;
        if (negative) {
            i = -i;
        }
        String result = Long.toString(i, 16).toUpperCase(Locale.ENGLISH);
        if (result.length() < places) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("0000000000000000".substring(result.length(), places));
            stringBuilder.append(result);
            result = stringBuilder.toString();
        }
        if (!negative) {
            return result;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append('-');
        stringBuilder.append(result);
        return stringBuilder.toString();
    }

    public static String hex(CharSequence s) {
        return ((StringBuilder) hex(s, 4, ",", true, new StringBuilder())).toString();
    }

    public static <S extends CharSequence, U extends CharSequence, T extends Appendable> T hex(S s, int width, U separator, boolean useCodePoints, T result) {
        IOException i = null;
        if (useCodePoints) {
            int i2;
            while (i2 < s.length()) {
                try {
                    int cp = Character.codePointAt(s, i2);
                    if (i2 != 0) {
                        result.append(separator);
                    }
                    result.append(hex((long) cp, width));
                    i2 += UTF16.getCharCount(cp);
                } catch (IOException e) {
                    throw new IllegalIcuArgumentException(e);
                }
            }
        }
        while (i < s.length()) {
            if (i != null) {
                result.append(separator);
            }
            result.append(hex((long) s.charAt(i), width));
            i++;
        }
        return result;
    }

    public static String hex(byte[] o, int start, int end, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i != 0) {
                result.append(separator);
            }
            result.append(hex((long) o[i]));
        }
        return result.toString();
    }

    public static <S extends CharSequence> String hex(S s, int width, S separator) {
        return ((StringBuilder) hex(s, width, separator, true, new StringBuilder())).toString();
    }

    public static void split(String s, char divider, String[] output) {
        int current;
        int last = 0;
        int current2 = 0;
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == divider) {
                current = current2 + 1;
                output[current2] = s.substring(last, i);
                last = i + 1;
                current2 = current;
            }
            i++;
        }
        current = current2 + 1;
        output[current2] = s.substring(last, i);
        while (current < output.length) {
            current2 = current + 1;
            output[current] = "";
            current = current2;
        }
    }

    public static String[] split(String s, char divider) {
        int last = 0;
        ArrayList<String> output = new ArrayList();
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == divider) {
                output.add(s.substring(last, i));
                last = i + 1;
            }
            i++;
        }
        output.add(s.substring(last, i));
        return (String[]) output.toArray(new String[output.size()]);
    }

    public static int lookup(String source, String[] target) {
        for (int i = 0; i < target.length; i++) {
            if (source.equals(target[i])) {
                return i;
            }
        }
        return -1;
    }

    public static boolean parseChar(String id, int[] pos, char ch) {
        int start = pos[0];
        pos[0] = PatternProps.skipWhiteSpace(id, pos[0]);
        if (pos[0] == id.length() || id.charAt(pos[0]) != ch) {
            pos[0] = start;
            return false;
        }
        pos[0] = pos[0] + 1;
        return true;
    }

    public static int parsePattern(String rule, int pos, int limit, String pattern, int[] parsedInts) {
        int[] p = new int[1];
        int intCount = 0;
        int pos2 = pos;
        for (pos = 0; pos < pattern.length(); pos++) {
            char cpat = pattern.charAt(pos);
            int intCount2;
            if (cpat != ' ') {
                if (cpat == '#') {
                    p[0] = pos2;
                    intCount2 = intCount + 1;
                    parsedInts[intCount] = parseInteger(rule, p, limit);
                    if (p[0] == pos2) {
                        return -1;
                    }
                    pos2 = p[0];
                    intCount = intCount2;
                } else if (cpat != '~') {
                    if (pos2 >= limit) {
                        return -1;
                    }
                    intCount2 = pos2 + 1;
                    if (((char) UCharacter.toLowerCase(rule.charAt(pos2))) != cpat) {
                        return -1;
                    }
                    pos2 = intCount2;
                }
            } else if (pos2 >= limit) {
                return -1;
            } else {
                intCount2 = pos2 + 1;
                if (!PatternProps.isWhiteSpace(rule.charAt(pos2))) {
                    return -1;
                }
                pos2 = intCount2;
            }
            pos2 = PatternProps.skipWhiteSpace(rule, pos2);
        }
        return pos2;
    }

    public static int parsePattern(String pat, Replaceable text, int index, int limit) {
        int ipat = 0;
        if (0 == pat.length()) {
            return index;
        }
        int cpat = Character.codePointAt(pat, 0);
        while (index < limit) {
            int c = text.char32At(index);
            if (cpat == 126) {
                if (PatternProps.isWhiteSpace(c)) {
                    index += UTF16.getCharCount(c);
                } else {
                    ipat++;
                    if (ipat == pat.length()) {
                        return index;
                    }
                }
            } else if (c != cpat) {
                return -1;
            } else {
                int n = UTF16.getCharCount(c);
                index += n;
                ipat += n;
                if (ipat == pat.length()) {
                    return index;
                }
            }
            cpat = UTF16.charAt(pat, ipat);
        }
        return -1;
    }

    public static int parseInteger(String rule, int[] pos, int limit) {
        int count = 0;
        int value = 0;
        int p = pos[0];
        int radix = 10;
        if (rule.regionMatches(true, p, "0x", 0, 2)) {
            p += 2;
            radix = 16;
        } else if (p < limit && rule.charAt(p) == '0') {
            p++;
            count = 1;
            radix = 8;
        }
        while (p < limit) {
            int p2 = p + 1;
            int d = UCharacter.digit(rule.charAt(p), radix);
            if (d < 0) {
                p = p2 - 1;
                break;
            }
            count++;
            int v = (value * radix) + d;
            if (v <= value) {
                return 0;
            }
            value = v;
            p = p2;
        }
        if (count > 0) {
            pos[0] = p;
        }
        return value;
    }

    public static String parseUnicodeIdentifier(String str, int[] pos) {
        StringBuilder buf = new StringBuilder();
        int p = pos[0];
        while (p < str.length()) {
            int ch = Character.codePointAt(str, p);
            if (buf.length() != 0) {
                if (!UCharacter.isUnicodeIdentifierPart(ch)) {
                    break;
                }
                buf.appendCodePoint(ch);
            } else if (!UCharacter.isUnicodeIdentifierStart(ch)) {
                return null;
            } else {
                buf.appendCodePoint(ch);
            }
            p += UTF16.getCharCount(ch);
        }
        pos[0] = p;
        return buf.toString();
    }

    private static <T extends Appendable> void recursiveAppendNumber(T result, int n, int radix, int minDigits) {
        try {
            int digit = n % radix;
            if (n >= radix || minDigits > 1) {
                recursiveAppendNumber(result, n / radix, radix, minDigits - 1);
            }
            result.append(DIGITS[digit]);
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static <T extends Appendable> T appendNumber(T result, int n, int radix, int minDigits) {
        if (radix < 2 || radix > 36) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal radix ");
            stringBuilder.append(radix);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        int abs = n;
        if (n < 0) {
            abs = -n;
            try {
                result.append(LanguageTag.SEP);
            } catch (IOException e) {
                throw new IllegalIcuArgumentException(e);
            }
        }
        recursiveAppendNumber(result, abs, radix, minDigits);
        return result;
    }

    public static int parseNumber(String text, int[] pos, int radix) {
        int n = 0;
        int p = pos[0];
        while (p < text.length()) {
            int d = UCharacter.digit(Character.codePointAt(text, p), radix);
            if (d < 0) {
                break;
            }
            n = (radix * n) + d;
            if (n < 0) {
                return -1;
            }
            p++;
        }
        if (p == pos[0]) {
            return -1;
        }
        pos[0] = p;
        return n;
    }

    public static boolean isUnprintable(int c) {
        return c < 32 || c > 126;
    }

    public static <T extends Appendable> boolean escapeUnprintable(T result, int c) {
        try {
            if (!isUnprintable(c)) {
                return false;
            }
            result.append('\\');
            if ((-65536 & c) != 0) {
                result.append('U');
                result.append(DIGITS[(c >> 28) & 15]);
                result.append(DIGITS[(c >> 24) & 15]);
                result.append(DIGITS[(c >> 20) & 15]);
                result.append(DIGITS[(c >> 16) & 15]);
            } else {
                result.append('u');
            }
            result.append(DIGITS[(c >> 12) & 15]);
            result.append(DIGITS[(c >> 8) & 15]);
            result.append(DIGITS[(c >> 4) & 15]);
            result.append(DIGITS[15 & c]);
            return true;
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static int quotedIndexOf(String text, int start, int limit, String setOfChars) {
        int i = start;
        while (i < limit) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '\'') {
                while (true) {
                    i++;
                    if (i >= limit || text.charAt(i) == '\'') {
                        break;
                    }
                }
            } else if (setOfChars.indexOf(c) >= 0) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static void appendToRule(StringBuffer rule, int c, boolean isLiteral, boolean escapeUnprintable, StringBuffer quoteBuf) {
        if (isLiteral || (escapeUnprintable && isUnprintable(c))) {
            if (quoteBuf.length() > 0) {
                while (quoteBuf.length() >= 2 && quoteBuf.charAt(0) == '\'' && quoteBuf.charAt(1) == '\'') {
                    rule.append('\\');
                    rule.append('\'');
                    quoteBuf.delete(0, 2);
                }
                int trailingCount = 0;
                while (quoteBuf.length() >= 2 && quoteBuf.charAt(quoteBuf.length() - 2) == '\'' && quoteBuf.charAt(quoteBuf.length() - 1) == '\'') {
                    quoteBuf.setLength(quoteBuf.length() - 2);
                    trailingCount++;
                }
                if (quoteBuf.length() > 0) {
                    rule.append('\'');
                    rule.append(quoteBuf);
                    rule.append('\'');
                    quoteBuf.setLength(0);
                }
                while (true) {
                    int trailingCount2 = trailingCount - 1;
                    if (trailingCount <= 0) {
                        break;
                    }
                    rule.append('\\');
                    rule.append('\'');
                    trailingCount = trailingCount2;
                }
            }
            if (c == -1) {
                return;
            }
            if (c == 32) {
                int len = rule.length();
                if (len > 0 && rule.charAt(len - 1) != ' ') {
                    rule.append(' ');
                }
            } else if (!escapeUnprintable || !escapeUnprintable(rule, c)) {
                rule.appendCodePoint(c);
            }
        } else if (quoteBuf.length() == 0 && (c == 39 || c == 92)) {
            rule.append('\\');
            rule.append((char) c);
        } else if (quoteBuf.length() > 0 || ((c >= 33 && c <= 126 && ((c < 48 || c > 57) && ((c < 65 || c > 90) && (c < 97 || c > 122)))) || PatternProps.isWhiteSpace(c))) {
            quoteBuf.appendCodePoint(c);
            if (c == 39) {
                quoteBuf.append((char) c);
            }
        } else {
            rule.appendCodePoint(c);
        }
    }

    public static void appendToRule(StringBuffer rule, String text, boolean isLiteral, boolean escapeUnprintable, StringBuffer quoteBuf) {
        for (int i = 0; i < text.length(); i++) {
            appendToRule(rule, text.charAt(i), isLiteral, escapeUnprintable, quoteBuf);
        }
    }

    public static void appendToRule(StringBuffer rule, UnicodeMatcher matcher, boolean escapeUnprintable, StringBuffer quoteBuf) {
        if (matcher != null) {
            appendToRule(rule, matcher.toPattern(escapeUnprintable), true, escapeUnprintable, quoteBuf);
        }
    }

    public static final int compareUnsigned(int source, int target) {
        source -= Integer.MIN_VALUE;
        target -= Integer.MIN_VALUE;
        if (source < target) {
            return -1;
        }
        if (source > target) {
            return 1;
        }
        return 0;
    }

    public static final byte highBit(int n) {
        if (n <= 0) {
            return (byte) -1;
        }
        byte bit = (byte) 0;
        if (n >= 65536) {
            n >>= 16;
            bit = (byte) (0 + 16);
        }
        if (n >= 256) {
            n >>= 8;
            bit = (byte) (bit + 8);
        }
        if (n >= 16) {
            n >>= 4;
            bit = (byte) (bit + 4);
        }
        if (n >= 4) {
            n >>= 2;
            bit = (byte) (bit + 2);
        }
        if (n >= 2) {
            n >>= 1;
            bit = (byte) (bit + 1);
        }
        return bit;
    }

    public static String valueOf(int[] source) {
        StringBuilder result = new StringBuilder(source.length);
        for (int appendCodePoint : source) {
            result.appendCodePoint(appendCodePoint);
        }
        return result.toString();
    }

    public static String repeat(String s, int count) {
        if (count <= 0) {
            return "";
        }
        if (count == 1) {
            return s;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(s);
        }
        return result.toString();
    }

    public static String[] splitString(String src, String target) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\\Q");
        stringBuilder.append(target);
        stringBuilder.append("\\E");
        return src.split(stringBuilder.toString());
    }

    public static String[] splitWhitespace(String src) {
        return src.split("\\s+");
    }

    public static String fromHex(String string, int minLength, String separator) {
        return fromHex(string, minLength, Pattern.compile(separator != null ? separator : "\\s+"));
    }

    public static String fromHex(String string, int minLength, Pattern separator) {
        StringBuilder buffer = new StringBuilder();
        String[] parts = separator.split(string);
        int length = parts.length;
        int i = 0;
        while (i < length) {
            String part = parts[i];
            if (part.length() >= minLength) {
                buffer.appendCodePoint(Integer.parseInt(part, 16));
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("code point too short: ");
                stringBuilder.append(part);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return buffer.toString();
    }

    public static boolean equals(Object a, Object b) {
        return a == b || !(a == null || b == null || !a.equals(b));
    }

    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static String toString(Object o) {
        return o == null ? "null" : o.toString();
    }
}
