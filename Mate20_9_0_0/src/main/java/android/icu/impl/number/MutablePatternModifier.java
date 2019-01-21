package android.icu.impl.number;

import android.icu.impl.StandardPlural;
import android.icu.impl.locale.XLocaleDistance;
import android.icu.impl.number.AffixUtils.SymbolProvider;
import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.number.NumberFormatter.UnitWidth;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.PluralRules;
import android.icu.util.Currency;

public class MutablePatternModifier implements Modifier, SymbolProvider, CharSequence, MicroPropsGenerator {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    Currency currency;
    int flags;
    boolean inCharSequenceMode;
    boolean isNegative;
    final boolean isStrong;
    int length;
    MicroPropsGenerator parent;
    AffixPatternProvider patternInfo;
    boolean perMilleReplacesPercent;
    StandardPlural plural;
    boolean plusReplacesMinusSign;
    boolean prependSign;
    PluralRules rules;
    SignDisplay signDisplay;
    DecimalFormatSymbols symbols;
    UnitWidth unitWidth;

    public static class ImmutablePatternModifier implements MicroPropsGenerator {
        final MicroPropsGenerator parent;
        final ParameterizedModifier pm;
        final PluralRules rules;

        ImmutablePatternModifier(ParameterizedModifier pm, PluralRules rules, MicroPropsGenerator parent) {
            this.pm = pm;
            this.rules = rules;
            this.parent = parent;
        }

        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = this.parent.processQuantity(quantity);
            applyToMicros(micros, quantity);
            return micros;
        }

        public void applyToMicros(MicroProps micros, DecimalQuantity quantity) {
            if (this.rules == null) {
                micros.modMiddle = this.pm.getModifier(quantity.isNegative());
                return;
            }
            DecimalQuantity copy = quantity.createCopy();
            copy.roundToInfinity();
            micros.modMiddle = this.pm.getModifier(quantity.isNegative(), copy.getStandardPlural(this.rules));
        }
    }

    public MutablePatternModifier(boolean isStrong) {
        this.isStrong = isStrong;
    }

    public void setPatternInfo(AffixPatternProvider patternInfo) {
        this.patternInfo = patternInfo;
    }

    public void setPatternAttributes(SignDisplay signDisplay, boolean perMille) {
        this.signDisplay = signDisplay;
        this.perMilleReplacesPercent = perMille;
    }

    public void setSymbols(DecimalFormatSymbols symbols, Currency currency, UnitWidth unitWidth, PluralRules rules) {
        this.symbols = symbols;
        this.currency = currency;
        this.unitWidth = unitWidth;
        this.rules = rules;
    }

    public void setNumberProperties(boolean isNegative, StandardPlural plural) {
        this.isNegative = isNegative;
        this.plural = plural;
    }

    public boolean needsPlurals() {
        return this.patternInfo.containsSymbolType(-7);
    }

    public ImmutablePatternModifier createImmutable() {
        return createImmutableAndChain(null);
    }

    public ImmutablePatternModifier createImmutableAndChain(MicroPropsGenerator parent) {
        NumberStringBuilder a = new NumberStringBuilder();
        NumberStringBuilder b = new NumberStringBuilder();
        if (needsPlurals()) {
            ParameterizedModifier pm = new ParameterizedModifier();
            for (StandardPlural plural : StandardPlural.VALUES) {
                setNumberProperties(false, plural);
                pm.setModifier(false, plural, createConstantModifier(a, b));
                setNumberProperties(true, plural);
                pm.setModifier(true, plural, createConstantModifier(a, b));
            }
            pm.freeze();
            return new ImmutablePatternModifier(pm, this.rules, parent);
        }
        setNumberProperties(false, null);
        Modifier positive = createConstantModifier(a, b);
        setNumberProperties(true, null);
        return new ImmutablePatternModifier(new ParameterizedModifier(positive, createConstantModifier(a, b)), null, parent);
    }

    private ConstantMultiFieldModifier createConstantModifier(NumberStringBuilder a, NumberStringBuilder b) {
        insertPrefix(a.clear(), 0);
        insertSuffix(b.clear(), 0);
        if (this.patternInfo.hasCurrencySign()) {
            return new CurrencySpacingEnabledModifier(a, b, this.isStrong, this.symbols);
        }
        return new ConstantMultiFieldModifier(a, b, this.isStrong);
    }

    public MicroPropsGenerator addToChain(MicroPropsGenerator parent) {
        this.parent = parent;
        return this;
    }

    public MicroProps processQuantity(DecimalQuantity fq) {
        MicroProps micros = this.parent.processQuantity(fq);
        if (needsPlurals()) {
            DecimalQuantity copy = fq.createCopy();
            micros.rounding.apply(copy);
            setNumberProperties(fq.isNegative(), copy.getStandardPlural(this.rules));
        } else {
            setNumberProperties(fq.isNegative(), null);
        }
        micros.modMiddle = this;
        return micros;
    }

    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
        int prefixLen = insertPrefix(output, leftIndex);
        int suffixLen = insertSuffix(output, rightIndex + prefixLen);
        CurrencySpacingEnabledModifier.applyCurrencySpacing(output, leftIndex, prefixLen, rightIndex + prefixLen, suffixLen, this.symbols);
        return prefixLen + suffixLen;
    }

    public int getPrefixLength() {
        enterCharSequenceMode(true);
        int result = AffixUtils.unescapedCodePointCount(this, this);
        exitCharSequenceMode();
        return result;
    }

    public int getCodePointCount() {
        enterCharSequenceMode(true);
        int result = AffixUtils.unescapedCodePointCount(this, this);
        exitCharSequenceMode();
        enterCharSequenceMode(false);
        result += AffixUtils.unescapedCodePointCount(this, this);
        exitCharSequenceMode();
        return result;
    }

    public boolean isStrong() {
        return this.isStrong;
    }

    private int insertPrefix(NumberStringBuilder sb, int position) {
        enterCharSequenceMode(true);
        int length = AffixUtils.unescape(this, sb, position, this);
        exitCharSequenceMode();
        return length;
    }

    private int insertSuffix(NumberStringBuilder sb, int position) {
        enterCharSequenceMode(false);
        int length = AffixUtils.unescape(this, sb, position, this);
        exitCharSequenceMode();
        return length;
    }

    public CharSequence getSymbol(int type) {
        switch (type) {
            case AffixUtils.TYPE_CURRENCY_QUINT /*-9*/:
                return this.currency.getName(this.symbols.getULocale(), 3, null);
            case AffixUtils.TYPE_CURRENCY_QUAD /*-8*/:
                return XLocaleDistance.ANY;
            case AffixUtils.TYPE_CURRENCY_TRIPLE /*-7*/:
                return this.currency.getName(this.symbols.getULocale(), 2, this.plural.getKeyword(), null);
            case AffixUtils.TYPE_CURRENCY_DOUBLE /*-6*/:
                return this.currency.getCurrencyCode();
            case AffixUtils.TYPE_CURRENCY_SINGLE /*-5*/:
                if (this.unitWidth == UnitWidth.ISO_CODE) {
                    return this.currency.getCurrencyCode();
                }
                if (this.unitWidth == UnitWidth.HIDDEN) {
                    return "";
                }
                if (this.unitWidth == UnitWidth.NARROW) {
                    return this.currency.getName(this.symbols.getULocale(), 3, null);
                }
                return this.currency.getName(this.symbols.getULocale(), 0, null);
            case AffixUtils.TYPE_PERMILLE /*-4*/:
                return this.symbols.getPerMillString();
            case AffixUtils.TYPE_PERCENT /*-3*/:
                return this.symbols.getPercentString();
            case -2:
                return this.symbols.getPlusSignString();
            case -1:
                return this.symbols.getMinusSignString();
            default:
                throw new AssertionError();
        }
    }

    private void enterCharSequenceMode(boolean isPrefix) {
        boolean z = true;
        this.inCharSequenceMode = true;
        boolean z2 = !this.isNegative && ((this.signDisplay == SignDisplay.ALWAYS || this.signDisplay == SignDisplay.ACCOUNTING_ALWAYS) && !this.patternInfo.positiveHasPlusSign());
        this.plusReplacesMinusSign = z2;
        z2 = this.patternInfo.hasNegativeSubpattern() && (this.isNegative || (this.patternInfo.negativeHasMinusSign() && this.plusReplacesMinusSign));
        this.flags = 0;
        if (z2) {
            this.flags |= 512;
        }
        if (isPrefix) {
            this.flags |= 256;
        }
        if (this.plural != null) {
            this.flags |= this.plural.ordinal();
        }
        if (!isPrefix || z2) {
            this.prependSign = false;
        } else if (this.isNegative) {
            if (this.signDisplay == SignDisplay.NEVER) {
                z = false;
            }
            this.prependSign = z;
        } else {
            this.prependSign = this.plusReplacesMinusSign;
        }
        this.length = this.patternInfo.length(this.flags) + this.prependSign;
    }

    private void exitCharSequenceMode() {
        this.inCharSequenceMode = false;
    }

    public int length() {
        return this.length;
    }

    public char charAt(int index) {
        char candidate;
        if (this.prependSign && index == 0) {
            candidate = '-';
        } else if (this.prependSign) {
            candidate = this.patternInfo.charAt(this.flags, index - 1);
        } else {
            candidate = this.patternInfo.charAt(this.flags, index);
        }
        if (this.plusReplacesMinusSign && candidate == '-') {
            return '+';
        }
        if (this.perMilleReplacesPercent && candidate == '%') {
            return 8240;
        }
        return candidate;
    }

    public CharSequence subSequence(int start, int end) {
        throw new AssertionError();
    }
}
