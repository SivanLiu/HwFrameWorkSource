package android.icu.impl.number;

import android.icu.text.NumberFormat.Field;

public class ConstantAffixModifier implements Modifier {
    public static final ConstantAffixModifier EMPTY = new ConstantAffixModifier();
    private final Field field;
    private final String prefix;
    private final boolean strong;
    private final String suffix;

    public ConstantAffixModifier(String prefix, String suffix, Field field, boolean strong) {
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
        this.field = field;
        this.strong = strong;
    }

    public ConstantAffixModifier() {
        this.prefix = "";
        this.suffix = "";
        this.field = null;
        this.strong = false;
    }

    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
        return output.insert(rightIndex, this.suffix, this.field) + output.insert(leftIndex, this.prefix, this.field);
    }

    public int getPrefixLength() {
        return this.prefix.length();
    }

    public int getCodePointCount() {
        return this.prefix.codePointCount(0, this.prefix.length()) + this.suffix.codePointCount(0, this.suffix.length());
    }

    public boolean isStrong() {
        return this.strong;
    }

    public String toString() {
        return String.format("<ConstantAffixModifier prefix:'%s' suffix:'%s'>", new Object[]{this.prefix, this.suffix});
    }
}
