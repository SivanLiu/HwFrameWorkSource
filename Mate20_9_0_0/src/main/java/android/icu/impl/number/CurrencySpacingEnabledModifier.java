package android.icu.impl.number;

import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat.Field;
import android.icu.text.UnicodeSet;

public class CurrencySpacingEnabledModifier extends ConstantMultiFieldModifier {
    static final short IN_CURRENCY = (short) 0;
    static final short IN_NUMBER = (short) 1;
    static final byte PREFIX = (byte) 0;
    static final byte SUFFIX = (byte) 1;
    private static final UnicodeSet UNISET_DIGIT = new UnicodeSet("[:digit:]").freeze();
    private static final UnicodeSet UNISET_NOTS = new UnicodeSet("[:^S:]").freeze();
    private final String afterPrefixInsert;
    private final UnicodeSet afterPrefixUnicodeSet;
    private final String beforeSuffixInsert;
    private final UnicodeSet beforeSuffixUnicodeSet;

    public CurrencySpacingEnabledModifier(NumberStringBuilder prefix, NumberStringBuilder suffix, boolean strong, DecimalFormatSymbols symbols) {
        super(prefix, suffix, strong);
        if (prefix.length() <= 0 || prefix.fieldAt(prefix.length() - 1) != Field.CURRENCY) {
            this.afterPrefixUnicodeSet = null;
            this.afterPrefixInsert = null;
        } else {
            if (getUnicodeSet(symbols, (short) 0, (byte) 0).contains(prefix.getLastCodePoint())) {
                this.afterPrefixUnicodeSet = getUnicodeSet(symbols, (short) 1, (byte) 0);
                this.afterPrefixUnicodeSet.freeze();
                this.afterPrefixInsert = getInsertString(symbols, (byte) 0);
            } else {
                this.afterPrefixUnicodeSet = null;
                this.afterPrefixInsert = null;
            }
        }
        if (suffix.length() <= 0 || suffix.fieldAt(0) != Field.CURRENCY) {
            this.beforeSuffixUnicodeSet = null;
            this.beforeSuffixInsert = null;
            return;
        }
        if (getUnicodeSet(symbols, (short) 0, (byte) 1).contains(suffix.getLastCodePoint())) {
            this.beforeSuffixUnicodeSet = getUnicodeSet(symbols, (short) 1, (byte) 1);
            this.beforeSuffixUnicodeSet.freeze();
            this.beforeSuffixInsert = getInsertString(symbols, (byte) 1);
            return;
        }
        this.beforeSuffixUnicodeSet = null;
        this.beforeSuffixInsert = null;
    }

    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
        int length = 0;
        if (rightIndex - leftIndex > 0 && this.afterPrefixUnicodeSet != null && this.afterPrefixUnicodeSet.contains(output.codePointAt(leftIndex))) {
            length = 0 + output.insert(leftIndex, this.afterPrefixInsert, null);
        }
        if (rightIndex - leftIndex > 0 && this.beforeSuffixUnicodeSet != null && this.beforeSuffixUnicodeSet.contains(output.codePointBefore(rightIndex))) {
            length += output.insert(rightIndex + length, this.beforeSuffixInsert, null);
        }
        return length + super.apply(output, leftIndex, rightIndex + length);
    }

    public static int applyCurrencySpacing(NumberStringBuilder output, int prefixStart, int prefixLen, int suffixStart, int suffixLen, DecimalFormatSymbols symbols) {
        int length = 0;
        boolean hasPrefix = prefixLen > 0;
        boolean hasSuffix = suffixLen > 0;
        boolean hasNumber = (suffixStart - prefixStart) - prefixLen > 0;
        if (hasPrefix && hasNumber) {
            length = 0 + applyCurrencySpacingAffix(output, prefixStart + prefixLen, (byte) 0, symbols);
        }
        if (hasSuffix && hasNumber) {
            return length + applyCurrencySpacingAffix(output, suffixStart + length, (byte) 1, symbols);
        }
        return length;
    }

    private static int applyCurrencySpacingAffix(NumberStringBuilder output, int index, byte affix, DecimalFormatSymbols symbols) {
        if ((affix == (byte) 0 ? output.fieldAt(index - 1) : output.fieldAt(index)) != Field.CURRENCY) {
            return 0;
        }
        if (!getUnicodeSet(symbols, (short) 0, affix).contains(affix == (byte) 0 ? output.codePointBefore(index) : output.codePointAt(index))) {
            return 0;
        }
        if (getUnicodeSet(symbols, (short) 1, affix).contains(affix == (byte) 0 ? output.codePointAt(index) : output.codePointBefore(index))) {
            return output.insert(index, getInsertString(symbols, affix), null);
        }
        return 0;
    }

    private static UnicodeSet getUnicodeSet(DecimalFormatSymbols symbols, short position, byte affix) {
        String pattern = null;
        int i = position == (short) 0 ? 0 : 1;
        if (affix == (byte) 1) {
            pattern = 1;
        }
        pattern = symbols.getPatternForCurrencySpacing(i, pattern);
        if (pattern.equals("[:digit:]")) {
            return UNISET_DIGIT;
        }
        if (pattern.equals("[:^S:]")) {
            return UNISET_NOTS;
        }
        return new UnicodeSet(pattern);
    }

    private static String getInsertString(DecimalFormatSymbols symbols, byte affix) {
        boolean z = true;
        if (affix != (byte) 1) {
            z = false;
        }
        return symbols.getPatternForCurrencySpacing(2, z);
    }
}
