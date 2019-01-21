package android.icu.impl.number;

import android.icu.number.Grouper;
import android.icu.number.IntegerWidth;
import android.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.number.Rounder;
import android.icu.text.DecimalFormatSymbols;

public class MicroProps implements Cloneable, MicroPropsGenerator {
    public DecimalSeparatorDisplay decimal;
    private volatile boolean exhausted;
    public Grouper grouping;
    private final boolean immutable;
    public IntegerWidth integerWidth;
    public Modifier modInner;
    public Modifier modMiddle;
    public Modifier modOuter;
    public Padder padding;
    public Rounder rounding;
    public SignDisplay sign;
    public DecimalFormatSymbols symbols;
    public boolean useCurrency;

    public MicroProps(boolean immutable) {
        this.immutable = immutable;
    }

    public MicroProps processQuantity(DecimalQuantity quantity) {
        if (this.immutable) {
            return (MicroProps) clone();
        }
        if (this.exhausted) {
            throw new AssertionError("Cannot re-use a mutable MicroProps in the quantity chain");
        }
        this.exhausted = true;
        return this;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
