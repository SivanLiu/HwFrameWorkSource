package android.icu.number;

import android.icu.impl.number.MacroProps;
import android.icu.impl.number.Padder;
import android.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.number.NumberFormatter.UnitWidth;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberingSystem;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;

public abstract class NumberFormatterSettings<T extends NumberFormatterSettings<?>> {
    static final int KEY_DECIMAL = 11;
    static final int KEY_GROUPER = 5;
    static final int KEY_INTEGER = 7;
    static final int KEY_LOCALE = 1;
    static final int KEY_MACROS = 0;
    static final int KEY_MAX = 13;
    static final int KEY_NOTATION = 2;
    static final int KEY_PADDER = 6;
    static final int KEY_ROUNDER = 4;
    static final int KEY_SIGN = 10;
    static final int KEY_SYMBOLS = 8;
    static final int KEY_THRESHOLD = 12;
    static final int KEY_UNIT = 3;
    static final int KEY_UNIT_WIDTH = 9;
    final int key;
    final NumberFormatterSettings<?> parent;
    volatile MacroProps resolvedMacros;
    final Object value;

    abstract T create(int i, Object obj);

    NumberFormatterSettings(NumberFormatterSettings<?> parent, int key, Object value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    public T notation(Notation notation) {
        return create(2, notation);
    }

    public T unit(MeasureUnit unit) {
        return create(3, unit);
    }

    public T rounding(Rounder rounder) {
        return create(4, rounder);
    }

    @Deprecated
    public T grouping(Grouper grouper) {
        return create(5, grouper);
    }

    public T integerWidth(IntegerWidth style) {
        return create(7, style);
    }

    public T symbols(DecimalFormatSymbols symbols) {
        return create(8, (DecimalFormatSymbols) symbols.clone());
    }

    public T symbols(NumberingSystem ns) {
        return create(8, ns);
    }

    public T unitWidth(UnitWidth style) {
        return create(9, style);
    }

    public T sign(SignDisplay style) {
        return create(10, style);
    }

    public T decimal(DecimalSeparatorDisplay style) {
        return create(11, style);
    }

    @Deprecated
    public T macros(MacroProps macros) {
        return create(0, macros);
    }

    @Deprecated
    public T padding(Padder padder) {
        return create(6, padder);
    }

    @Deprecated
    public T threshold(Long threshold) {
        return create(12, threshold);
    }

    MacroProps resolve() {
        if (this.resolvedMacros != null) {
            return this.resolvedMacros;
        }
        MacroProps macros = new MacroProps();
        for (NumberFormatterSettings<?> current = this; current != null; current = current.parent) {
            switch (current.key) {
                case 0:
                    macros.fallback((MacroProps) current.value);
                    break;
                case 1:
                    if (macros.loc != null) {
                        break;
                    }
                    macros.loc = (ULocale) current.value;
                    break;
                case 2:
                    if (macros.notation != null) {
                        break;
                    }
                    macros.notation = (Notation) current.value;
                    break;
                case 3:
                    if (macros.unit != null) {
                        break;
                    }
                    macros.unit = (MeasureUnit) current.value;
                    break;
                case 4:
                    if (macros.rounder != null) {
                        break;
                    }
                    macros.rounder = (Rounder) current.value;
                    break;
                case 5:
                    if (macros.grouper != null) {
                        break;
                    }
                    macros.grouper = (Grouper) current.value;
                    break;
                case 6:
                    if (macros.padder != null) {
                        break;
                    }
                    macros.padder = (Padder) current.value;
                    break;
                case 7:
                    if (macros.integerWidth != null) {
                        break;
                    }
                    macros.integerWidth = (IntegerWidth) current.value;
                    break;
                case 8:
                    if (macros.symbols != null) {
                        break;
                    }
                    macros.symbols = current.value;
                    break;
                case 9:
                    if (macros.unitWidth != null) {
                        break;
                    }
                    macros.unitWidth = (UnitWidth) current.value;
                    break;
                case 10:
                    if (macros.sign != null) {
                        break;
                    }
                    macros.sign = (SignDisplay) current.value;
                    break;
                case 11:
                    if (macros.decimal != null) {
                        break;
                    }
                    macros.decimal = (DecimalSeparatorDisplay) current.value;
                    break;
                case 12:
                    if (macros.threshold != null) {
                        break;
                    }
                    macros.threshold = (Long) current.value;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown key: ");
                    stringBuilder.append(current.key);
                    throw new AssertionError(stringBuilder.toString());
            }
        }
        this.resolvedMacros = macros;
        return macros;
    }

    public int hashCode() {
        return resolve().hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other != null && (other instanceof NumberFormatterSettings)) {
            return resolve().equals(((NumberFormatterSettings) other).resolve());
        }
        return false;
    }
}
