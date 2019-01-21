package android.icu.impl.number;

import android.icu.impl.PatternTokenizer;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.number.Padder.PadPosition;
import android.icu.text.DateFormat;
import android.icu.text.DecimalFormatSymbols;
import java.lang.reflect.Array;
import java.math.BigDecimal;

public class PatternStringUtils {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    public static String propertiesToPatternString(DecimalFormatProperties properties) {
        int grouping;
        int grouping1;
        int grouping2;
        int groupingLength;
        StringBuilder sb = new StringBuilder();
        int groupingSize = Math.min(properties.getSecondaryGroupingSize(), 100);
        int firstGroupingSize = Math.min(properties.getGroupingSize(), 100);
        int paddingWidth = Math.min(properties.getFormatWidth(), 100);
        PadPosition paddingLocation = properties.getPadPosition();
        String paddingString = properties.getPadString();
        int minInt = Math.max(Math.min(properties.getMinimumIntegerDigits(), 100), 0);
        int maxInt = Math.min(properties.getMaximumIntegerDigits(), 100);
        int minFrac = Math.max(Math.min(properties.getMinimumFractionDigits(), 100), 0);
        int maxFrac = Math.min(properties.getMaximumFractionDigits(), 100);
        int minSig = Math.min(properties.getMinimumSignificantDigits(), 100);
        int maxSig = Math.min(properties.getMaximumSignificantDigits(), 100);
        boolean alwaysShowDecimal = properties.getDecimalSeparatorAlwaysShown();
        int exponentDigits = Math.min(properties.getMinimumExponentDigits(), 100);
        boolean exponentShowPlusSign = properties.getExponentSignAlwaysShown();
        String pp = properties.getPositivePrefix();
        String paddingString2 = paddingString;
        paddingString = properties.getPositivePrefixPattern();
        PadPosition paddingLocation2 = paddingLocation;
        String ps = properties.getPositiveSuffix();
        int paddingWidth2 = paddingWidth;
        String psp = properties.getPositiveSuffixPattern();
        String ps2 = ps;
        String np = properties.getNegativePrefix();
        String npp = properties.getNegativePrefixPattern();
        String ns = properties.getNegativeSuffix();
        ps = properties.getNegativeSuffixPattern();
        if (paddingString != null) {
            sb.append(paddingString);
        }
        AffixUtils.escape(pp, sb);
        int afterPrefixPos = sb.length();
        String nsp = ps;
        if (groupingSize != Math.min(100, -1) && firstGroupingSize != Math.min(100, -1) && groupingSize != firstGroupingSize) {
            grouping = groupingSize;
            grouping1 = groupingSize;
            grouping2 = firstGroupingSize;
        } else if (groupingSize != Math.min(100, -1)) {
            grouping = groupingSize;
            grouping1 = 0;
            grouping2 = groupingSize;
        } else if (firstGroupingSize != Math.min(100, -1)) {
            grouping = groupingSize;
            grouping1 = 0;
            grouping2 = firstGroupingSize;
        } else {
            grouping = 0;
            grouping1 = 0;
            grouping2 = 0;
        }
        int grouping22 = grouping2;
        firstGroupingSize = (grouping1 + grouping22) + 1;
        BigDecimal roundingInterval = properties.getRoundingIncrement();
        int afterPrefixPos2 = afterPrefixPos;
        StringBuilder digitsString = new StringBuilder();
        grouping2 = 0;
        String psp2 = psp;
        int exponentDigits2 = exponentDigits;
        int i;
        if (maxSig != Math.min(100, -1)) {
            while (digitsString.length() < minSig) {
                digitsString.append('@');
            }
            while (digitsString.length() < maxSig) {
                digitsString.append('#');
            }
            BigDecimal bigDecimal = roundingInterval;
            i = minSig;
        } else if (roundingInterval != null) {
            exponentDigits = -roundingInterval.scale();
            psp = roundingInterval.scaleByPowerOfTen(roundingInterval.scale()).toPlainString();
            if (psp.charAt(null) == '-') {
                digitsString.append(psp, 1, psp.length());
            } else {
                digitsString.append(psp);
            }
            grouping2 = exponentDigits;
        } else {
            i = minSig;
        }
        while (digitsString.length() + grouping2 < minInt) {
            digitsString.insert(0, '0');
        }
        groupingSize = grouping2;
        while ((-groupingSize) < minFrac) {
            digitsString.append('0');
            groupingSize--;
        }
        minSig = Math.max(firstGroupingSize, digitsString.length() + groupingSize);
        minSig = maxInt != 100 ? Math.max(maxInt, minSig) - 1 : minSig - 1;
        exponentDigits = maxFrac != 100 ? Math.min(-maxFrac, groupingSize) : groupingSize;
        grouping2 = minSig;
        while (true) {
            paddingWidth = grouping2;
            if (paddingWidth < exponentDigits) {
                break;
            }
            int digitsStringScale = groupingSize;
            groupingSize = ((digitsString.length() + groupingSize) - paddingWidth) - 1;
            if (groupingSize >= 0) {
                groupingLength = firstGroupingSize;
                if (groupingSize < digitsString.length()) {
                    sb.append(digitsString.charAt(groupingSize));
                    if (paddingWidth <= grouping22 && grouping > 0 && (paddingWidth - grouping22) % grouping == 0) {
                        sb.append(',');
                    } else if (paddingWidth <= 0 && paddingWidth == grouping22) {
                        sb.append(',');
                    } else if (paddingWidth == 0 && (alwaysShowDecimal || exponentDigits < 0)) {
                        sb.append('.');
                    }
                    grouping2 = paddingWidth - 1;
                    groupingSize = digitsStringScale;
                    firstGroupingSize = groupingLength;
                }
            } else {
                groupingLength = firstGroupingSize;
            }
            sb.append('#');
            if (paddingWidth <= grouping22) {
            }
            if (paddingWidth <= 0) {
            }
            sb.append('.');
            grouping2 = paddingWidth - 1;
            groupingSize = digitsStringScale;
            firstGroupingSize = groupingLength;
        }
        groupingLength = firstGroupingSize;
        groupingSize = exponentDigits2;
        if (groupingSize != Math.min(100, -1)) {
            sb.append('E');
            if (exponentShowPlusSign) {
                sb.append('+');
            }
            for (firstGroupingSize = 0; firstGroupingSize < groupingSize; firstGroupingSize++) {
                sb.append('0');
            }
        }
        firstGroupingSize = sb.length();
        if (psp2 != null) {
            psp = psp2;
            sb.append(psp);
        } else {
            psp = psp2;
        }
        int dosMax = 100;
        String ps3 = ps2;
        AffixUtils.escape(ps3, sb);
        int paddingWidth3 = paddingWidth2;
        String str;
        if (paddingWidth3 != -1) {
            while (paddingWidth3 - sb.length() > 0) {
                int paddingWidth4 = paddingWidth3;
                sb.insert(afterPrefixPos2, 35);
                firstGroupingSize++;
                paddingWidth3 = paddingWidth4;
            }
            groupingSize = afterPrefixPos2;
            PadPosition paddingLocation3 = paddingLocation2;
            switch (paddingLocation3) {
                case BEFORE_PREFIX:
                    paddingWidth3 = paddingString2;
                    paddingString2 = escapePaddingString(paddingWidth3, sb, 0);
                    Object obj = paddingWidth3;
                    sb.insert(0, 42);
                    firstGroupingSize += paddingString2 + 1;
                    groupingSize = (paddingString2 + 1) + groupingSize;
                    break;
                case AFTER_PREFIX:
                    paddingWidth3 = paddingString2;
                    paddingString2 = escapePaddingString(paddingWidth3, sb, groupingSize);
                    sb.insert(groupingSize, '*');
                    groupingSize += paddingString2 + 1;
                    firstGroupingSize += paddingString2 + 1;
                    break;
                case BEFORE_SUFFIX:
                    paddingWidth3 = paddingString2;
                    escapePaddingString(paddingWidth3, sb, firstGroupingSize);
                    sb.insert(firstGroupingSize, '*');
                    break;
                case AFTER_SUFFIX:
                    sb.append('*');
                    psp = paddingString2;
                    escapePaddingString(psp, sb, sb.length());
                    str = psp;
                    break;
                default:
                    break;
            }
            str = paddingWidth3;
        } else {
            String str2 = psp;
            str = paddingString2;
            PadPosition padPosition = paddingLocation2;
            groupingSize = afterPrefixPos2;
        }
        if (np != null || ns != null) {
            ps3 = npp;
            psp = nsp;
        } else if (npp != null || nsp == null) {
            String str3;
            if (npp != null) {
                ps3 = npp;
                if (ps3.length() == 1 && ps3.charAt(0) == '-') {
                    psp = nsp;
                    if (psp.length() == 0) {
                        str3 = ps3;
                        ps = np;
                        ps3 = ns;
                    }
                } else {
                    psp = nsp;
                }
            } else {
                psp = nsp;
                grouping = np;
                str3 = npp;
                ps3 = ns;
            }
            return sb.toString();
        } else {
            int i2 = grouping;
            ps3 = npp;
            psp = nsp;
        }
        sb.append(';');
        if (ps3 != null) {
            sb.append(ps3);
        }
        AffixUtils.escape(np, sb);
        sb.append(sb, groupingSize, firstGroupingSize);
        if (psp != null) {
            sb.append(psp);
        }
        AffixUtils.escape(ns, sb);
        return sb.toString();
    }

    private static int escapePaddingString(CharSequence input, StringBuilder output, int startIndex) {
        if (input == null || input.length() == 0) {
            input = Padder.FALLBACK_PADDING_STRING;
        }
        int startLength = output.length();
        if (input.length() != 1) {
            output.insert(startIndex, PatternTokenizer.SINGLE_QUOTE);
            int offset = 1;
            for (int i = 0; i < input.length(); i++) {
                char ch = input.charAt(i);
                if (ch == PatternTokenizer.SINGLE_QUOTE) {
                    output.insert(startIndex + offset, "''");
                    offset += 2;
                } else {
                    output.insert(startIndex + offset, ch);
                    offset++;
                }
            }
            output.insert(startIndex + offset, PatternTokenizer.SINGLE_QUOTE);
        } else if (input.equals("'")) {
            output.insert(startIndex, "''");
        } else {
            output.insert(startIndex, input);
        }
        return output.length() - startLength;
    }

    public static String convertLocalized(String input, DecimalFormatSymbols symbols, boolean toLocalized) {
        String str = input;
        if (str == null) {
            return null;
        }
        int i;
        char c;
        int i2 = 2;
        String[][] table = (String[][]) Array.newInstance(String.class, new int[]{21, 2});
        int standIdx = toLocalized ^ 1;
        boolean localIdx = toLocalized;
        table[0][standIdx] = "%";
        table[0][localIdx] = symbols.getPercentString();
        table[1][standIdx] = "‰";
        table[1][localIdx] = symbols.getPerMillString();
        table[2][standIdx] = ".";
        table[2][localIdx] = symbols.getDecimalSeparatorString();
        int i3 = 3;
        table[3][standIdx] = ",";
        table[3][localIdx] = symbols.getGroupingSeparatorString();
        int i4 = 4;
        table[4][standIdx] = LanguageTag.SEP;
        table[4][localIdx] = symbols.getMinusSignString();
        table[5][standIdx] = "+";
        table[5][localIdx] = symbols.getPlusSignString();
        table[6][standIdx] = ";";
        table[6][localIdx] = Character.toString(symbols.getPatternSeparator());
        table[7][standIdx] = "@";
        table[7][localIdx] = Character.toString(symbols.getSignificantDigit());
        table[8][standIdx] = DateFormat.ABBR_WEEKDAY;
        table[8][localIdx] = symbols.getExponentSeparator();
        table[9][standIdx] = "*";
        table[9][localIdx] = Character.toString(symbols.getPadEscape());
        table[10][standIdx] = "#";
        table[10][localIdx] = Character.toString(symbols.getDigit());
        for (i = 0; i < 10; i++) {
            table[11 + i][standIdx] = Character.toString((char) (48 + i));
            table[11 + i][localIdx] = symbols.getDigitStringsLocal()[i];
        }
        int i5 = 0;
        while (true) {
            i = table.length;
            c = PatternTokenizer.SINGLE_QUOTE;
            if (i5 >= i) {
                break;
            }
            table[i5][localIdx] = table[i5][localIdx].replace(PatternTokenizer.SINGLE_QUOTE, 8217);
            i5++;
        }
        StringBuilder result = new StringBuilder();
        int state = 0;
        i = 0;
        while (i < input.length()) {
            char ch = str.charAt(i);
            if (ch == c) {
                if (state == 0) {
                    result.append(c);
                    state = 1;
                } else if (state == 1) {
                    result.append(c);
                    state = 0;
                } else if (state == i2) {
                    state = 3;
                } else if (state == i3) {
                    result.append(c);
                    result.append(c);
                    state = 1;
                } else if (state == i4) {
                    state = 5;
                } else {
                    result.append(c);
                    result.append(c);
                    state = 4;
                }
            } else if (state == 0 || state == i3 || state == i4) {
                int length = table.length;
                i2 = 0;
                while (i2 < length) {
                    String[] pair = table[i2];
                    if (str.regionMatches(i, pair[0], 0, pair[0].length())) {
                        i += pair[0].length() - 1;
                        if (state == i3 || state == 4) {
                            result.append(PatternTokenizer.SINGLE_QUOTE);
                            state = 0;
                        }
                        result.append(pair[1]);
                    } else {
                        i2++;
                    }
                }
                i2 = table.length;
                i4 = 0;
                while (i4 < i2) {
                    String[] pair2 = table[i4];
                    if (str.regionMatches(i, pair2[1], 0, pair2[1].length())) {
                        if (state == 0) {
                            result.append(PatternTokenizer.SINGLE_QUOTE);
                            state = 4;
                        }
                        result.append(ch);
                    } else {
                        i4++;
                    }
                }
                if (state == 3 || state == 4) {
                    result.append(PatternTokenizer.SINGLE_QUOTE);
                    state = 0;
                }
                result.append(ch);
            } else {
                result.append(ch);
                state = 2;
            }
            i++;
            i2 = 2;
            i3 = 3;
            i4 = 4;
            c = PatternTokenizer.SINGLE_QUOTE;
        }
        if (state == 3 || state == 4) {
            result.append(PatternTokenizer.SINGLE_QUOTE);
            state = 0;
        }
        if (state == 0) {
            return result.toString();
        }
        throw new IllegalArgumentException("Malformed localized pattern: unterminated quote");
    }
}
