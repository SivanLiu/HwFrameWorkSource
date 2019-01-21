package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.MicroPropsGenerator;
import android.icu.impl.number.Modifier;
import android.icu.impl.number.MultiplierProducer;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat.Field;

public class ScientificNotation extends Notation implements Cloneable {
    int engineeringInterval;
    SignDisplay exponentSignDisplay;
    int minExponentDigits;
    boolean requireMinInt;

    private static class ScientificHandler implements MicroPropsGenerator, MultiplierProducer, Modifier {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        int exponent;
        final ScientificNotation notation;
        final MicroPropsGenerator parent;
        final ScientificModifier[] precomputedMods;
        final DecimalFormatSymbols symbols;

        static {
            Class cls = ScientificNotation.class;
        }

        private ScientificHandler(ScientificNotation notation, DecimalFormatSymbols symbols, boolean safe, MicroPropsGenerator parent) {
            this.notation = notation;
            this.symbols = symbols;
            this.parent = parent;
            if (safe) {
                this.precomputedMods = new ScientificModifier[25];
                for (int i = -12; i <= 12; i++) {
                    this.precomputedMods[i + 12] = new ScientificModifier(i, this);
                }
                return;
            }
            this.precomputedMods = null;
        }

        public MicroProps processQuantity(DecimalQuantity quantity) {
            int exponent;
            MicroProps micros = this.parent.processQuantity(quantity);
            if (!quantity.isZero()) {
                exponent = -micros.rounding.chooseMultiplierAndApply(quantity, this);
            } else if (this.notation.requireMinInt && (micros.rounding instanceof SignificantRounderImpl)) {
                ((SignificantRounderImpl) micros.rounding).apply(quantity, this.notation.engineeringInterval);
                exponent = 0;
            } else {
                micros.rounding.apply(quantity);
                exponent = 0;
            }
            if (this.precomputedMods != null && exponent >= -12 && exponent <= 12) {
                micros.modInner = this.precomputedMods[exponent + 12];
            } else if (this.precomputedMods != null) {
                micros.modInner = new ScientificModifier(exponent, this);
            } else {
                this.exponent = exponent;
                micros.modInner = this;
            }
            micros.rounding = Rounder.constructPassThrough();
            return micros;
        }

        public int getMultiplier(int magnitude) {
            int digitsShown;
            int interval = this.notation.engineeringInterval;
            if (this.notation.requireMinInt) {
                digitsShown = interval;
            } else if (interval <= 1) {
                digitsShown = 1;
            } else {
                digitsShown = (((magnitude % interval) + interval) % interval) + 1;
            }
            return (digitsShown - magnitude) - 1;
        }

        public int getPrefixLength() {
            return 0;
        }

        public int getCodePointCount() {
            throw new AssertionError();
        }

        public boolean isStrong() {
            return true;
        }

        public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
            return doApply(this.exponent, output, rightIndex);
        }

        private int doApply(int exponent, NumberStringBuilder output, int rightIndex) {
            int i = rightIndex;
            i += output.insert(i, this.symbols.getExponentSeparator(), Field.EXPONENT_SYMBOL);
            if (exponent < 0 && this.notation.exponentSignDisplay != SignDisplay.NEVER) {
                i += output.insert(i, this.symbols.getMinusSignString(), Field.EXPONENT_SIGN);
            } else if (exponent >= 0 && this.notation.exponentSignDisplay == SignDisplay.ALWAYS) {
                i += output.insert(i, this.symbols.getPlusSignString(), Field.EXPONENT_SIGN);
            }
            int disp = Math.abs(exponent);
            int j = 0;
            while (true) {
                if (j >= this.notation.minExponentDigits && disp <= 0) {
                    return i - rightIndex;
                }
                int i2 = i - j;
                i += output.insert(i2, this.symbols.getDigitStringsLocal()[disp % 10], Field.EXPONENT);
                j++;
                disp /= 10;
            }
        }
    }

    private static class ScientificModifier implements Modifier {
        final int exponent;
        final ScientificHandler handler;

        ScientificModifier(int exponent, ScientificHandler handler) {
            this.exponent = exponent;
            this.handler = handler;
        }

        public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
            return this.handler.doApply(this.exponent, output, rightIndex);
        }

        public int getPrefixLength() {
            return 0;
        }

        public int getCodePointCount() {
            throw new AssertionError();
        }

        public boolean isStrong() {
            return true;
        }
    }

    ScientificNotation(int engineeringInterval, boolean requireMinInt, int minExponentDigits, SignDisplay exponentSignDisplay) {
        this.engineeringInterval = engineeringInterval;
        this.requireMinInt = requireMinInt;
        this.minExponentDigits = minExponentDigits;
        this.exponentSignDisplay = exponentSignDisplay;
    }

    public ScientificNotation withMinExponentDigits(int minExponentDigits) {
        if (minExponentDigits < 0 || minExponentDigits >= 100) {
            throw new IllegalArgumentException("Integer digits must be between 0 and 100");
        }
        ScientificNotation other = (ScientificNotation) clone();
        other.minExponentDigits = minExponentDigits;
        return other;
    }

    public ScientificNotation withExponentSignDisplay(SignDisplay exponentSignDisplay) {
        ScientificNotation other = (ScientificNotation) clone();
        other.exponentSignDisplay = exponentSignDisplay;
        return other;
    }

    @Deprecated
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    MicroPropsGenerator withLocaleData(DecimalFormatSymbols symbols, boolean build, MicroPropsGenerator parent) {
        return new ScientificHandler(symbols, build, parent);
    }
}
