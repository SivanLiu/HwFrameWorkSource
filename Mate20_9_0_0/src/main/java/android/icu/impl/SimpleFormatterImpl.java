package android.icu.impl;

import android.icu.text.PluralRules;

public final class SimpleFormatterImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ARG_NUM_LIMIT = 256;
    private static final String[][] COMMON_PATTERNS = new String[][]{new String[]{"{0} {1}", "\u0002\u0000ā \u0001"}, new String[]{"{0} ({1})", "\u0002\u0000Ă (\u0001ā)"}, new String[]{"{0}, {1}", "\u0002\u0000Ă, \u0001"}, new String[]{"{0} – {1}", "\u0002\u0000ă – \u0001"}};
    private static final char LEN1_CHAR = 'ā';
    private static final char LEN2_CHAR = 'Ă';
    private static final char LEN3_CHAR = 'ă';
    private static final int MAX_SEGMENT_LENGTH = 65279;
    private static final char SEGMENT_LENGTH_ARGUMENT_CHAR = '￿';

    private SimpleFormatterImpl() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00d7  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String compileToStringMinMaxArguments(CharSequence pattern, StringBuilder sb, int min, int max) {
        int argStart;
        CharSequence charSequence = pattern;
        StringBuilder stringBuilder = sb;
        int i = min;
        int i2 = max;
        int i3 = 1;
        if (i <= 2 && 2 <= i2) {
            for (String[] pair : COMMON_PATTERNS) {
                if (pair[0].contentEquals(charSequence)) {
                    return pair[1];
                }
            }
        }
        char patternLength = pattern.length();
        stringBuilder.ensureCapacity(patternLength);
        stringBuilder.setLength(1);
        boolean inQuote = false;
        int maxArg = -1;
        int i4 = 0;
        char c = 0;
        while (c < patternLength) {
            char i5 = c + 1;
            c = charSequence.charAt(c);
            if (c == PatternTokenizer.SINGLE_QUOTE) {
                if (i5 < patternLength) {
                    char charAt = charSequence.charAt(i5);
                    c = charAt;
                    if (charAt == PatternTokenizer.SINGLE_QUOTE) {
                        i5++;
                    }
                }
                if (inQuote) {
                    inQuote = false;
                    c = i5;
                } else if (c == '{' || c == '}') {
                    i5++;
                    inQuote = true;
                } else {
                    c = PatternTokenizer.SINGLE_QUOTE;
                }
            } else if (!inQuote && c == '{') {
                int charAt2;
                int argNumber;
                if (i4 > 0) {
                    stringBuilder.setCharAt((sb.length() - i4) - i3, (char) (256 + i4));
                    i4 = 0;
                }
                if (i5 + 1 < patternLength) {
                    charAt2 = charSequence.charAt(i5) - 48;
                    argNumber = charAt2;
                    if (charAt2 >= 0) {
                        argStart = argNumber;
                        if (argStart <= 9 && charSequence.charAt(i5 + 1) == '}') {
                            i5 += 2;
                            if (argStart > maxArg) {
                                maxArg = argStart;
                            }
                            stringBuilder.append((char) argStart);
                            c = i5;
                            i3 = 1;
                        }
                    }
                }
                argStart = i5 - 1;
                charAt2 = -1;
                if (i5 < patternLength) {
                    char i6 = i5 + 1;
                    i5 = charSequence.charAt(i5);
                    c = i5;
                    if ('1' <= i5 && c <= '9') {
                        charAt2 = c - 48;
                        do {
                            i5 = i6;
                            if (i5 >= patternLength) {
                                break;
                            }
                            i6 = i5 + 1;
                            i5 = charSequence.charAt(i5);
                            c = i5;
                            if ('0' > i5 || c > '9') {
                                break;
                            }
                            charAt2 = (charAt2 * 10) + (c - 48);
                        } while (charAt2 < 256);
                    }
                    i5 = i6;
                }
                argNumber = charAt2;
                if (argNumber < 0 || c != '}') {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Argument syntax error in pattern \"");
                    stringBuilder2.append(charSequence);
                    stringBuilder2.append("\" at index ");
                    stringBuilder2.append(argStart);
                    stringBuilder2.append(PluralRules.KEYWORD_RULE_SEPARATOR);
                    stringBuilder2.append(charSequence.subSequence(argStart, i5));
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
                argStart = argNumber;
                if (argStart > maxArg) {
                }
                stringBuilder.append((char) argStart);
                c = i5;
                i3 = 1;
            }
            if (i4 == 0) {
                stringBuilder.append(65535);
            }
            stringBuilder.append(c);
            i4++;
            if (i4 == MAX_SEGMENT_LENGTH) {
                i4 = 0;
            }
            c = i5;
            i3 = 1;
        }
        if (i4 > 0) {
            stringBuilder.setCharAt((sb.length() - i4) - 1, (char) (256 + i4));
        }
        argStart = maxArg + 1;
        StringBuilder stringBuilder3;
        if (argStart < i) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Fewer than minimum ");
            stringBuilder3.append(i);
            stringBuilder3.append(" arguments in pattern \"");
            stringBuilder3.append(charSequence);
            stringBuilder3.append("\"");
            throw new IllegalArgumentException(stringBuilder3.toString());
        } else if (argStart <= i2) {
            stringBuilder.setCharAt(0, (char) argStart);
            return sb.toString();
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("More than maximum ");
            stringBuilder3.append(i2);
            stringBuilder3.append(" arguments in pattern \"");
            stringBuilder3.append(charSequence);
            stringBuilder3.append("\"");
            throw new IllegalArgumentException(stringBuilder3.toString());
        }
    }

    public static int getArgumentLimit(String compiledPattern) {
        return compiledPattern.charAt(0);
    }

    public static String formatCompiledPattern(String compiledPattern, CharSequence... values) {
        return formatAndAppend(compiledPattern, new StringBuilder(), null, values).toString();
    }

    public static String formatRawPattern(String pattern, int min, int max, CharSequence... values) {
        StringBuilder sb = new StringBuilder();
        String compiledPattern = compileToStringMinMaxArguments(pattern, sb, min, max);
        sb.setLength(0);
        return formatAndAppend(compiledPattern, sb, null, values).toString();
    }

    public static StringBuilder formatAndAppend(String compiledPattern, StringBuilder appendTo, int[] offsets, CharSequence... values) {
        if ((values != null ? values.length : 0) >= getArgumentLimit(compiledPattern)) {
            return format(compiledPattern, values, appendTo, null, true, offsets);
        }
        throw new IllegalArgumentException("Too few values.");
    }

    public static StringBuilder formatAndReplace(String compiledPattern, StringBuilder result, int[] offsets, CharSequence... values) {
        if ((values != null ? values.length : 0) >= getArgumentLimit(compiledPattern)) {
            int firstArg = -1;
            String resultCopy = null;
            if (getArgumentLimit(compiledPattern) > 0) {
                int i = 1;
                while (i < compiledPattern.length()) {
                    int i2 = i + 1;
                    i = compiledPattern.charAt(i);
                    if (i >= 256) {
                        i2 += i - 256;
                    } else if (values[i] == result) {
                        if (i2 == 2) {
                            firstArg = i;
                        } else if (resultCopy == null) {
                            resultCopy = result.toString();
                        }
                    }
                    i = i2;
                }
            }
            String resultCopy2 = resultCopy;
            if (firstArg < 0) {
                result.setLength(0);
            }
            return format(compiledPattern, values, result, resultCopy2, false, offsets);
        }
        throw new IllegalArgumentException("Too few values.");
    }

    public static String getTextWithNoArguments(String compiledPattern) {
        int i = 1;
        StringBuilder sb = new StringBuilder((compiledPattern.length() - 1) - getArgumentLimit(compiledPattern));
        while (i < compiledPattern.length()) {
            int i2 = i + 1;
            i = compiledPattern.charAt(i) - 256;
            if (i > 0) {
                int limit = i2 + i;
                sb.append(compiledPattern, i2, limit);
                i = limit;
            } else {
                i = i2;
            }
        }
        return sb.toString();
    }

    private static StringBuilder format(String compiledPattern, CharSequence[] values, StringBuilder result, String resultCopy, boolean forbidResultAsValue, int[] offsets) {
        int offsetsLength;
        int i;
        if (offsets == null) {
            offsetsLength = 0;
        } else {
            offsetsLength = offsets.length;
            for (i = 0; i < offsetsLength; i++) {
                offsets[i] = -1;
            }
        }
        i = 1;
        while (i < compiledPattern.length()) {
            int i2 = i + 1;
            i = compiledPattern.charAt(i);
            if (i < 256) {
                StringBuilder value = values[i];
                if (value != result) {
                    if (i < offsetsLength) {
                        offsets[i] = result.length();
                    }
                    result.append(value);
                } else if (forbidResultAsValue) {
                    throw new IllegalArgumentException("Value must not be same object as result");
                } else if (i2 != 2) {
                    if (i < offsetsLength) {
                        offsets[i] = result.length();
                    }
                    result.append(resultCopy);
                } else if (i < offsetsLength) {
                    offsets[i] = 0;
                }
                i = i2;
            } else {
                int limit = (i - 256) + i2;
                result.append(compiledPattern, i2, limit);
                i = limit;
            }
        }
        return result;
    }
}
