package android.icu.number;

import android.icu.impl.StandardPlural;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.number.AffixPatternProvider;
import android.icu.impl.number.AffixUtils;
import android.icu.impl.number.CustomSymbolCurrency;
import android.icu.impl.number.DecimalFormatProperties;
import android.icu.impl.number.MacroProps;
import android.icu.impl.number.MultiplierImpl;
import android.icu.impl.number.Padder;
import android.icu.impl.number.PatternStringParser;
import android.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import android.icu.impl.number.RoundingUtils;
import android.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.text.CompactDecimalFormat.CompactStyle;
import android.icu.text.CurrencyPluralInfo;
import android.icu.text.DecimalFormatSymbols;
import android.icu.util.Currency;
import android.icu.util.Currency.CurrencyUsage;
import android.icu.util.ULocale;
import java.math.BigDecimal;
import java.math.MathContext;

final class NumberPropertyMapper {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    private static class CurrencyPluralInfoAffixProvider implements AffixPatternProvider {
        private final AffixPatternProvider[] affixesByPlural = new ParsedPatternInfo[StandardPlural.COUNT];

        public CurrencyPluralInfoAffixProvider(CurrencyPluralInfo cpi) {
            for (StandardPlural plural : StandardPlural.VALUES) {
                this.affixesByPlural[plural.ordinal()] = PatternStringParser.parseToPatternInfo(cpi.getCurrencyPluralPattern(plural.getKeyword()));
            }
        }

        public char charAt(int flags, int i) {
            return this.affixesByPlural[flags & 255].charAt(flags, i);
        }

        public int length(int flags) {
            return this.affixesByPlural[flags & 255].length(flags);
        }

        public boolean positiveHasPlusSign() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].positiveHasPlusSign();
        }

        public boolean hasNegativeSubpattern() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].hasNegativeSubpattern();
        }

        public boolean negativeHasMinusSign() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].negativeHasMinusSign();
        }

        public boolean hasCurrencySign() {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].hasCurrencySign();
        }

        public boolean containsSymbolType(int type) {
            return this.affixesByPlural[StandardPlural.OTHER.ordinal()].containsSymbolType(type);
        }
    }

    private static class PropertiesAffixPatternProvider implements AffixPatternProvider {
        private final String negPrefix;
        private final String negSuffix;
        private final String posPrefix;
        private final String posSuffix;

        public PropertiesAffixPatternProvider(DecimalFormatProperties properties) {
            String ppo = AffixUtils.escape(properties.getPositivePrefix());
            String pso = AffixUtils.escape(properties.getPositiveSuffix());
            String npo = AffixUtils.escape(properties.getNegativePrefix());
            String nso = AffixUtils.escape(properties.getNegativeSuffix());
            String ppp = properties.getPositivePrefixPattern();
            String psp = properties.getPositiveSuffixPattern();
            String npp = properties.getNegativePrefixPattern();
            String nsp = properties.getNegativeSuffixPattern();
            if (ppo != null) {
                this.posPrefix = ppo;
            } else if (ppp != null) {
                this.posPrefix = ppp;
            } else {
                this.posPrefix = "";
            }
            if (pso != null) {
                this.posSuffix = pso;
            } else if (psp != null) {
                this.posSuffix = psp;
            } else {
                this.posSuffix = "";
            }
            if (npo != null) {
                this.negPrefix = npo;
            } else if (npp != null) {
                this.negPrefix = npp;
            } else {
                String str;
                if (ppp == null) {
                    str = LanguageTag.SEP;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(LanguageTag.SEP);
                    stringBuilder.append(ppp);
                    str = stringBuilder.toString();
                }
                this.negPrefix = str;
            }
            if (nso != null) {
                this.negSuffix = nso;
            } else if (nsp != null) {
                this.negSuffix = nsp;
            } else {
                this.negSuffix = psp == null ? "" : psp;
            }
        }

        public char charAt(int flags, int i) {
            return getStringForFlags(flags).charAt(i);
        }

        public int length(int flags) {
            return getStringForFlags(flags).length();
        }

        private String getStringForFlags(int flags) {
            boolean negative = false;
            boolean prefix = (flags & 256) != 0;
            if ((flags & 512) != 0) {
                negative = true;
            }
            if (prefix && negative) {
                return this.negPrefix;
            }
            if (prefix) {
                return this.posPrefix;
            }
            if (negative) {
                return this.negSuffix;
            }
            return this.posSuffix;
        }

        public boolean positiveHasPlusSign() {
            return AffixUtils.containsType(this.posPrefix, -2) || AffixUtils.containsType(this.posSuffix, -2);
        }

        public boolean hasNegativeSubpattern() {
            return true;
        }

        public boolean negativeHasMinusSign() {
            return AffixUtils.containsType(this.negPrefix, -1) || AffixUtils.containsType(this.negSuffix, -1);
        }

        public boolean hasCurrencySign() {
            return AffixUtils.hasCurrencySymbols(this.posPrefix) || AffixUtils.hasCurrencySymbols(this.posSuffix) || AffixUtils.hasCurrencySymbols(this.negPrefix) || AffixUtils.hasCurrencySymbols(this.negSuffix);
        }

        public boolean containsSymbolType(int type) {
            return AffixUtils.containsType(this.posPrefix, type) || AffixUtils.containsType(this.posSuffix, type) || AffixUtils.containsType(this.negPrefix, type) || AffixUtils.containsType(this.negSuffix, type);
        }
    }

    NumberPropertyMapper() {
    }

    public static UnlocalizedNumberFormatter create(DecimalFormatProperties properties, DecimalFormatSymbols symbols) {
        return (UnlocalizedNumberFormatter) NumberFormatter.with().macros(oldToNew(properties, symbols, null));
    }

    public static UnlocalizedNumberFormatter create(String pattern, DecimalFormatSymbols symbols) {
        return create(PatternStringParser.parseToProperties(pattern), symbols);
    }

    public static MacroProps oldToNew(DecimalFormatProperties properties, DecimalFormatSymbols symbols, DecimalFormatProperties exportedProperties) {
        AffixPatternProvider affixProvider;
        int i;
        DecimalSeparatorDisplay decimalSeparatorDisplay;
        int minFrac_;
        int maxFrac_;
        DecimalFormatSymbols decimalFormatSymbols = symbols;
        DecimalFormatProperties decimalFormatProperties = exportedProperties;
        MacroProps macros = new MacroProps();
        ULocale locale = symbols.getULocale();
        macros.symbols = decimalFormatSymbols;
        macros.rules = properties.getPluralRules();
        if (properties.getCurrencyPluralInfo() == null) {
            affixProvider = new PropertiesAffixPatternProvider(properties);
        } else {
            DecimalFormatProperties decimalFormatProperties2 = properties;
            affixProvider = new CurrencyPluralInfoAffixProvider(properties.getCurrencyPluralInfo());
        }
        macros.affixProvider = affixProvider;
        boolean useCurrency = (properties.getCurrency() == null && properties.getCurrencyPluralInfo() == null && properties.getCurrencyUsage() == null && !affixProvider.hasCurrencySign()) ? false : true;
        Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, decimalFormatSymbols);
        CurrencyUsage currencyUsage = properties.getCurrencyUsage();
        boolean explicitCurrencyUsage = currencyUsage != null;
        if (!explicitCurrencyUsage) {
            currencyUsage = CurrencyUsage.STANDARD;
        }
        if (useCurrency) {
            macros.unit = currency;
        }
        int maxInt = properties.getMaximumIntegerDigits();
        int minInt = properties.getMinimumIntegerDigits();
        int maxFrac = properties.getMaximumFractionDigits();
        int minFrac = properties.getMinimumFractionDigits();
        int minSig = properties.getMinimumSignificantDigits();
        int maxSig = properties.getMaximumSignificantDigits();
        BigDecimal roundingIncrement = properties.getRoundingIncrement();
        MathContext mathContext = RoundingUtils.getMathContextOrUnlimited(properties);
        boolean explicitMinMaxFrac = (minFrac == -1 && maxFrac == -1) ? false : true;
        boolean explicitMinMaxSig = (minSig == -1 && maxSig == -1) ? false : true;
        if (useCurrency) {
            if (minFrac == -1 && maxFrac == -1) {
                minFrac = currency.getDefaultFractionDigits(currencyUsage);
                maxFrac = currency.getDefaultFractionDigits(currencyUsage);
            } else if (minFrac == -1) {
                minFrac = Math.min(maxFrac, currency.getDefaultFractionDigits(currencyUsage));
            } else if (maxFrac == -1) {
                maxFrac = Math.max(minFrac, currency.getDefaultFractionDigits(currencyUsage));
            }
        }
        if (minInt != 0 || maxFrac == 0) {
            minFrac = minFrac < 0 ? 0 : minFrac;
            i = maxFrac < 0 ? Integer.MAX_VALUE : maxFrac < minFrac ? minFrac : maxFrac;
            maxFrac = i;
            i = (minInt > 0 && minInt <= 100) ? minInt : 1;
            minInt = i;
            if (maxInt >= 0) {
                if (maxInt < minInt) {
                    i = minInt;
                } else if (maxInt <= 100) {
                    i = maxInt;
                }
                maxInt = i;
            }
            i = -1;
            maxInt = i;
        } else {
            minFrac = minFrac <= 0 ? 1 : minFrac;
            i = maxFrac < 0 ? Integer.MAX_VALUE : maxFrac < minFrac ? minFrac : maxFrac;
            maxFrac = i;
            minInt = 0;
            i = (maxInt >= 0 && maxInt <= 100) ? maxInt : -1;
            maxInt = i;
        }
        Rounder rounding = null;
        if (explicitCurrencyUsage) {
            rounding = Rounder.constructCurrency(currencyUsage).withCurrency(currency);
        } else if (roundingIncrement != null) {
            rounding = Rounder.constructIncrement(roundingIncrement);
        } else if (explicitMinMaxSig) {
            int i2 = minSig < 1 ? 1 : minSig > 100 ? 100 : minSig;
            minSig = i2;
            if (maxSig < 0) {
                i2 = 100;
            } else if (maxSig < minSig) {
                i2 = minSig;
            } else {
                i2 = 100;
                if (maxSig <= 100) {
                    i2 = maxSig;
                }
            }
            maxSig = i2;
            rounding = Rounder.constructSignificant(minSig, maxSig);
        } else if (explicitMinMaxFrac) {
            rounding = Rounder.constructFraction(minFrac, maxFrac);
        } else if (useCurrency) {
            rounding = Rounder.constructCurrency(currencyUsage);
        }
        Rounder rounding2 = rounding;
        if (rounding2 != null) {
            rounding2 = rounding2.withMode(mathContext);
            macros.rounder = rounding2;
        }
        macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(maxInt);
        int grouping1 = properties.getGroupingSize();
        i = properties.getSecondaryGroupingSize();
        int minGrouping = properties.getMinimumGroupingDigits();
        int i3 = (grouping1 <= 0 && i > 0) ? i : grouping1;
        grouping1 = i3;
        byte b = (byte) grouping1;
        grouping1 = i > 0 ? i : grouping1;
        macros.grouper = Grouper.getInstance(b, (byte) grouping1, minGrouping == 2);
        if (properties.getFormatWidth() != -1) {
            macros.padder = new Padder(properties.getPadString(), properties.getFormatWidth(), properties.getPadPosition());
        }
        if (properties.getDecimalSeparatorAlwaysShown()) {
            decimalSeparatorDisplay = DecimalSeparatorDisplay.ALWAYS;
        } else {
            decimalSeparatorDisplay = DecimalSeparatorDisplay.AUTO;
        }
        macros.decimal = decimalSeparatorDisplay;
        macros.sign = properties.getSignAlwaysShown() ? SignDisplay.ALWAYS : SignDisplay.AUTO;
        if (properties.getMinimumExponentDigits() != -1) {
            if (maxInt > 8) {
                grouping1 = minInt;
                macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(grouping1);
                maxInt = grouping1;
            } else if (maxInt > minInt && minInt > 1) {
                minInt = 1;
                macros.integerWidth = IntegerWidth.zeroFillTo(1).truncateAt(maxInt);
            }
            grouping1 = maxInt < 0 ? -1 : maxInt;
            int maxInt2 = maxInt;
            macros.notation = new ScientificNotation(grouping1, grouping1 == minInt, properties.getMinimumExponentDigits(), properties.getExponentSignAlwaysShown() ? SignDisplay.ALWAYS : SignDisplay.AUTO);
            if (macros.rounder instanceof FractionRounder) {
                minGrouping = properties.getMinimumIntegerDigits();
                minFrac_ = properties.getMinimumFractionDigits();
                maxFrac_ = properties.getMaximumFractionDigits();
                if (minGrouping == 0 && maxFrac_ == 0) {
                    macros.rounder = Rounder.constructInfinite().withMode(mathContext);
                } else if (minGrouping == 0 && minFrac_ == 0) {
                    macros.rounder = Rounder.constructSignificant(1, maxFrac_ + 1).withMode(mathContext);
                } else {
                    macros.rounder = Rounder.constructSignificant(minGrouping + minFrac_, minGrouping + maxFrac_).withMode(mathContext);
                }
            }
            maxInt = maxInt2;
        }
        if (properties.getCompactStyle() != null) {
            if (properties.getCompactCustomData() != null) {
                macros.notation = new CompactNotation(properties.getCompactCustomData());
            } else if (properties.getCompactStyle() == CompactStyle.LONG) {
                macros.notation = Notation.compactLong();
            } else {
                macros.notation = Notation.compactShort();
            }
            macros.affixProvider = null;
        }
        if (properties.getMagnitudeMultiplier() != 0) {
            macros.multiplier = new MultiplierImpl(properties.getMagnitudeMultiplier());
        } else if (properties.getMultiplier() != null) {
            macros.multiplier = new MultiplierImpl(properties.getMultiplier());
        }
        if (decimalFormatProperties != null) {
            BigDecimal bigDecimal;
            decimalFormatProperties.setMathContext(mathContext);
            decimalFormatProperties.setRoundingMode(mathContext.getRoundingMode());
            decimalFormatProperties.setMinimumIntegerDigits(minInt);
            decimalFormatProperties.setMaximumIntegerDigits(maxInt == -1 ? Integer.MAX_VALUE : maxInt);
            if (rounding2 instanceof CurrencyRounder) {
                roundingIncrement = ((CurrencyRounder) rounding2).withCurrency(currency);
            } else {
                roundingIncrement = rounding2;
            }
            minGrouping = minFrac;
            minFrac_ = maxFrac;
            maxFrac_ = minSig;
            int maxSig_ = maxSig;
            if ((roundingIncrement instanceof FractionRounderImpl) != null) {
                minGrouping = ((FractionRounderImpl) roundingIncrement).minFrac;
                minFrac_ = ((FractionRounderImpl) roundingIncrement).maxFrac;
            } else if ((roundingIncrement instanceof IncrementRounderImpl) != null) {
                mathContext = ((IncrementRounderImpl) roundingIncrement).increment;
                minGrouping = mathContext.scale();
                minFrac_ = mathContext.scale();
                bigDecimal = roundingIncrement;
                roundingIncrement = mathContext;
                mathContext = maxSig_;
                decimalFormatProperties.setMinimumFractionDigits(minGrouping);
                decimalFormatProperties.setMaximumFractionDigits(minFrac_);
                decimalFormatProperties.setMinimumSignificantDigits(maxFrac_);
                decimalFormatProperties.setMaximumSignificantDigits(mathContext);
                decimalFormatProperties.setRoundingIncrement(roundingIncrement);
            } else if ((roundingIncrement instanceof SignificantRounderImpl) != null) {
                maxFrac_ = ((SignificantRounderImpl) roundingIncrement).minSig;
                mathContext = ((SignificantRounderImpl) roundingIncrement).maxSig;
                bigDecimal = roundingIncrement;
                roundingIncrement = null;
                decimalFormatProperties.setMinimumFractionDigits(minGrouping);
                decimalFormatProperties.setMaximumFractionDigits(minFrac_);
                decimalFormatProperties.setMinimumSignificantDigits(maxFrac_);
                decimalFormatProperties.setMaximumSignificantDigits(mathContext);
                decimalFormatProperties.setRoundingIncrement(roundingIncrement);
            }
            bigDecimal = roundingIncrement;
            mathContext = maxSig_;
            roundingIncrement = null;
            decimalFormatProperties.setMinimumFractionDigits(minGrouping);
            decimalFormatProperties.setMaximumFractionDigits(minFrac_);
            decimalFormatProperties.setMinimumSignificantDigits(maxFrac_);
            decimalFormatProperties.setMaximumSignificantDigits(mathContext);
            decimalFormatProperties.setRoundingIncrement(roundingIncrement);
        }
        return macros;
    }
}
