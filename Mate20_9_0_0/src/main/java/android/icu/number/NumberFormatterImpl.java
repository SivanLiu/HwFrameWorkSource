package android.icu.number;

import android.icu.impl.number.CompactData.CompactType;
import android.icu.impl.number.ConstantAffixModifier;
import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.LongNameHandler;
import android.icu.impl.number.MacroProps;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.MicroPropsGenerator;
import android.icu.impl.number.MutablePatternModifier;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.impl.number.Padder;
import android.icu.impl.number.PatternStringParser;
import android.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import android.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.number.NumberFormatter.UnitWidth;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;
import android.icu.text.NumberFormat.Field;
import android.icu.text.NumberingSystem;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.MeasureUnit;

class NumberFormatterImpl {
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");
    final MicroPropsGenerator microPropsGenerator;

    public static NumberFormatterImpl fromMacros(MacroProps macros) {
        return new NumberFormatterImpl(macrosToMicroGenerator(macros, true));
    }

    public static MicroProps applyStatic(MacroProps macros, DecimalQuantity inValue, NumberStringBuilder outString) {
        MicroProps micros = macrosToMicroGenerator(macros, null).processQuantity(inValue);
        microsToString(micros, inValue, outString);
        return micros;
    }

    private NumberFormatterImpl(MicroPropsGenerator microPropsGenerator) {
        this.microPropsGenerator = microPropsGenerator;
    }

    public MicroProps apply(DecimalQuantity inValue, NumberStringBuilder outString) {
        MicroProps micros = this.microPropsGenerator.processQuantity(inValue);
        microsToString(micros, inValue, outString);
        return micros;
    }

    private static boolean unitIsCurrency(MeasureUnit unit) {
        return unit != null && "currency".equals(unit.getType());
    }

    private static boolean unitIsNoUnit(MeasureUnit unit) {
        return unit == null || "none".equals(unit.getType());
    }

    private static boolean unitIsPercent(MeasureUnit unit) {
        return unit != null && "percent".equals(unit.getSubtype());
    }

    private static boolean unitIsPermille(MeasureUnit unit) {
        return unit != null && "permille".equals(unit.getSubtype());
    }

    private static MicroPropsGenerator macrosToMicroGenerator(MacroProps macros, boolean safe) {
        NumberingSystem ns;
        int patternStyle;
        ParsedPatternInfo patternInfo;
        MacroProps macroProps = macros;
        boolean z = safe;
        MicroProps micros = new MicroProps(z);
        MicroPropsGenerator chain = micros;
        boolean isCurrency = unitIsCurrency(macroProps.unit);
        boolean isNoUnit = unitIsNoUnit(macroProps.unit);
        boolean isPercent = isNoUnit && unitIsPercent(macroProps.unit);
        boolean isPermille = isNoUnit && unitIsPermille(macroProps.unit);
        boolean isCldrUnit = (isCurrency || isNoUnit) ? false : true;
        boolean isAccounting = macroProps.sign == SignDisplay.ACCOUNTING || macroProps.sign == SignDisplay.ACCOUNTING_ALWAYS;
        Currency currency = isCurrency ? (Currency) macroProps.unit : DEFAULT_CURRENCY;
        UnitWidth unitWidth = UnitWidth.SHORT;
        if (macroProps.unitWidth != null) {
            unitWidth = macroProps.unitWidth;
        }
        PluralRules rules = macroProps.rules;
        if (macroProps.symbols instanceof NumberingSystem) {
            ns = macroProps.symbols;
        } else {
            ns = NumberingSystem.getInstance(macroProps.loc);
        }
        String nsName = ns.getName();
        if (isPercent || isPermille) {
            patternStyle = 2;
        } else if (!isCurrency || unitWidth == UnitWidth.FULL_NAME) {
            patternStyle = 0;
        } else if (isAccounting) {
            patternStyle = 7;
        } else {
            patternStyle = 1;
        }
        String pattern = NumberFormat.getPatternForStyleAndNumberingSystem(macroProps.loc, nsName, patternStyle);
        ParsedPatternInfo patternInfo2 = PatternStringParser.parseToPatternInfo(pattern);
        if (macroProps.symbols instanceof DecimalFormatSymbols) {
            micros.symbols = (DecimalFormatSymbols) macroProps.symbols;
        } else {
            micros.symbols = DecimalFormatSymbols.forNumberingSystem(macroProps.loc, ns);
        }
        if (macroProps.multiplier != null) {
            chain = macroProps.multiplier.copyAndChain(chain);
        }
        if (macroProps.rounder != null) {
            micros.rounding = macroProps.rounder;
        } else if (macroProps.notation instanceof CompactNotation) {
            micros.rounding = Rounder.COMPACT_STRATEGY;
        } else if (isCurrency) {
            micros.rounding = Rounder.MONETARY_STANDARD;
        } else {
            micros.rounding = Rounder.MAX_FRAC_6;
        }
        micros.rounding = micros.rounding.withLocaleData(currency);
        if (macroProps.grouper != null) {
            micros.grouping = macroProps.grouper;
        } else if (macroProps.notation instanceof CompactNotation) {
            micros.grouping = Grouper.minTwoDigits();
        } else {
            micros.grouping = Grouper.defaults();
        }
        micros.grouping = micros.grouping.withLocaleData(patternInfo2);
        if (macroProps.padder != null) {
            micros.padding = macroProps.padder;
        } else {
            micros.padding = Padder.NONE;
        }
        if (macroProps.integerWidth != null) {
            micros.integerWidth = macroProps.integerWidth;
        } else {
            micros.integerWidth = IntegerWidth.DEFAULT;
        }
        if (macroProps.sign != null) {
            micros.sign = macroProps.sign;
        } else {
            micros.sign = SignDisplay.AUTO;
        }
        if (macroProps.decimal != null) {
            micros.decimal = macroProps.decimal;
        } else {
            micros.decimal = DecimalSeparatorDisplay.AUTO;
        }
        micros.useCurrency = isCurrency;
        if (macroProps.notation instanceof ScientificNotation) {
            patternInfo = patternInfo2;
            chain = ((ScientificNotation) macroProps.notation).withLocaleData(micros.symbols, z, chain);
        } else {
            patternInfo = patternInfo2;
            micros.modInner = ConstantAffixModifier.EMPTY;
        }
        MutablePatternModifier patternMod = new MutablePatternModifier(false);
        patternMod.setPatternInfo(macroProps.affixProvider != null ? macroProps.affixProvider : patternInfo);
        patternMod.setPatternAttributes(micros.sign, isPermille);
        if (patternMod.needsPlurals()) {
            if (rules == null) {
                rules = PluralRules.forLocale(macroProps.loc);
            }
            patternMod.setSymbols(micros.symbols, currency, unitWidth, rules);
        } else {
            patternMod.setSymbols(micros.symbols, currency, unitWidth, null);
        }
        if (z) {
            chain = patternMod.createImmutableAndChain(chain);
        } else {
            chain = patternMod.addToChain(chain);
        }
        if (isCldrUnit) {
            if (rules == null) {
                rules = PluralRules.forLocale(macroProps.loc);
            }
            chain = LongNameHandler.forMeasureUnit(macroProps.loc, macroProps.unit, unitWidth, rules, chain);
        } else if (isCurrency && unitWidth == UnitWidth.FULL_NAME) {
            if (rules == null) {
                rules = PluralRules.forLocale(macroProps.loc);
            }
            chain = LongNameHandler.forCurrencyLongNames(macroProps.loc, currency, rules, chain);
        } else {
            micros.modOuter = ConstantAffixModifier.EMPTY;
        }
        if (!(macroProps.notation instanceof CompactNotation)) {
            return chain;
        }
        CompactType compactType;
        if (rules == null) {
            rules = PluralRules.forLocale(macroProps.loc);
        }
        if (!(macroProps.unit instanceof Currency) || macroProps.unitWidth == UnitWidth.FULL_NAME) {
            compactType = CompactType.DECIMAL;
        } else {
            compactType = CompactType.CURRENCY;
        }
        return ((CompactNotation) macroProps.notation).withLocaleData(macroProps.loc, nsName, compactType, rules, z ? patternMod : null, chain);
    }

    private static void microsToString(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        micros.rounding.apply(quantity);
        if (micros.integerWidth.maxInt == -1) {
            quantity.setIntegerLength(micros.integerWidth.minInt, Integer.MAX_VALUE);
        } else {
            quantity.setIntegerLength(micros.integerWidth.minInt, micros.integerWidth.maxInt);
        }
        int length = writeNumber(micros, quantity, string);
        length += micros.modInner.apply(string, 0, length);
        if (micros.padding.isValid()) {
            micros.padding.padAndApply(micros.modMiddle, micros.modOuter, string, 0, length);
            return;
        }
        length += micros.modMiddle.apply(string, 0, length);
        length += micros.modOuter.apply(string, 0, length);
    }

    private static int writeNumber(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        if (quantity.isInfinite()) {
            return 0 + string.insert(0, micros.symbols.getInfinity(), Field.INTEGER);
        }
        if (quantity.isNaN()) {
            return 0 + string.insert(0, micros.symbols.getNaN(), Field.INTEGER);
        }
        int length = 0 + writeIntegerDigits(micros, quantity, string);
        if (quantity.getLowerDisplayMagnitude() < 0 || micros.decimal == DecimalSeparatorDisplay.ALWAYS) {
            CharSequence monetaryDecimalSeparatorString;
            if (micros.useCurrency) {
                monetaryDecimalSeparatorString = micros.symbols.getMonetaryDecimalSeparatorString();
            } else {
                monetaryDecimalSeparatorString = micros.symbols.getDecimalSeparatorString();
            }
            length += string.insert(length, monetaryDecimalSeparatorString, Field.DECIMAL_SEPARATOR);
        }
        return length + writeFractionDigits(micros, quantity, string);
    }

    private static int writeIntegerDigits(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        int integerCount = quantity.getUpperDisplayMagnitude() + 1;
        int length = 0;
        for (int i = 0; i < integerCount; i++) {
            int insertCodePoint;
            if (micros.grouping.groupAtPosition(i, quantity)) {
                CharSequence monetaryGroupingSeparatorString;
                if (micros.useCurrency) {
                    monetaryGroupingSeparatorString = micros.symbols.getMonetaryGroupingSeparatorString();
                } else {
                    monetaryGroupingSeparatorString = micros.symbols.getGroupingSeparatorString();
                }
                length += string.insert(0, monetaryGroupingSeparatorString, Field.GROUPING_SEPARATOR);
            }
            byte nextDigit = quantity.getDigit(i);
            if (micros.symbols.getCodePointZero() != -1) {
                insertCodePoint = string.insertCodePoint(0, micros.symbols.getCodePointZero() + nextDigit, Field.INTEGER);
            } else {
                insertCodePoint = string.insert(0, micros.symbols.getDigitStringsLocal()[nextDigit], Field.INTEGER);
            }
            length += insertCodePoint;
        }
        return length;
    }

    private static int writeFractionDigits(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        int length = 0;
        int fractionCount = -quantity.getLowerDisplayMagnitude();
        for (int i = 0; i < fractionCount; i++) {
            int appendCodePoint;
            byte nextDigit = quantity.getDigit((-i) - 1);
            if (micros.symbols.getCodePointZero() != -1) {
                appendCodePoint = string.appendCodePoint(micros.symbols.getCodePointZero() + nextDigit, Field.FRACTION);
            } else {
                appendCodePoint = string.append(micros.symbols.getDigitStringsLocal()[nextDigit], Field.FRACTION);
            }
            length += appendCodePoint;
        }
        return length;
    }
}
