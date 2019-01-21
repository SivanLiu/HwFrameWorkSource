package android.icu.impl.number;

import android.icu.text.NumberFormat.Field;

public class ConstantMultiFieldModifier implements Modifier {
    protected final char[] prefixChars;
    protected final Field[] prefixFields;
    private final boolean strong;
    protected final char[] suffixChars;
    protected final Field[] suffixFields;

    public ConstantMultiFieldModifier(NumberStringBuilder prefix, NumberStringBuilder suffix, boolean strong) {
        this.prefixChars = prefix.toCharArray();
        this.suffixChars = suffix.toCharArray();
        this.prefixFields = prefix.toFieldArray();
        this.suffixFields = suffix.toFieldArray();
        this.strong = strong;
    }

    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
        return output.insert(rightIndex, this.suffixChars, this.suffixFields) + output.insert(leftIndex, this.prefixChars, this.prefixFields);
    }

    public int getPrefixLength() {
        return this.prefixChars.length;
    }

    public int getCodePointCount() {
        return Character.codePointCount(this.prefixChars, 0, this.prefixChars.length) + Character.codePointCount(this.suffixChars, 0, this.suffixChars.length);
    }

    public boolean isStrong() {
        return this.strong;
    }

    public String toString() {
        apply(new NumberStringBuilder(), 0, 0);
        int prefixLength = getPrefixLength();
        return String.format("<ConstantMultiFieldModifier prefix:'%s' suffix:'%s'>", new Object[]{temp.subSequence(0, prefixLength), temp.subSequence(prefixLength, temp.length())});
    }
}
