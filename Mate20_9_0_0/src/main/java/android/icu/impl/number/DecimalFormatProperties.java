package android.icu.impl.number;

import android.icu.impl.number.Padder.PadPosition;
import android.icu.impl.number.Parse.GroupingMode;
import android.icu.impl.number.Parse.ParseMode;
import android.icu.text.CompactDecimalFormat.CompactStyle;
import android.icu.text.CurrencyPluralInfo;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.Currency.CurrencyUsage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;

public class DecimalFormatProperties implements Cloneable, Serializable {
    private static final DecimalFormatProperties DEFAULT = new DecimalFormatProperties();
    private static final long serialVersionUID = 4095518955889349243L;
    private transient Map<String, Map<String, String>> compactCustomData;
    private transient CompactStyle compactStyle;
    private transient Currency currency;
    private transient CurrencyPluralInfo currencyPluralInfo;
    private transient CurrencyUsage currencyUsage;
    private transient boolean decimalPatternMatchRequired;
    private transient boolean decimalSeparatorAlwaysShown;
    private transient boolean exponentSignAlwaysShown;
    private transient int formatWidth;
    private transient int groupingSize;
    private transient int magnitudeMultiplier;
    private transient MathContext mathContext;
    private transient int maximumFractionDigits;
    private transient int maximumIntegerDigits;
    private transient int maximumSignificantDigits;
    private transient int minimumExponentDigits;
    private transient int minimumFractionDigits;
    private transient int minimumGroupingDigits;
    private transient int minimumIntegerDigits;
    private transient int minimumSignificantDigits;
    private transient BigDecimal multiplier;
    private transient String negativePrefix;
    private transient String negativePrefixPattern;
    private transient String negativeSuffix;
    private transient String negativeSuffixPattern;
    private transient PadPosition padPosition;
    private transient String padString;
    private transient boolean parseCaseSensitive;
    private transient GroupingMode parseGroupingMode;
    private transient boolean parseIntegerOnly;
    private transient ParseMode parseMode;
    private transient boolean parseNoExponent;
    private transient boolean parseToBigDecimal;
    private transient PluralRules pluralRules;
    private transient String positivePrefix;
    private transient String positivePrefixPattern;
    private transient String positiveSuffix;
    private transient String positiveSuffixPattern;
    private transient BigDecimal roundingIncrement;
    private transient RoundingMode roundingMode;
    private transient int secondaryGroupingSize;
    private transient boolean signAlwaysShown;

    public DecimalFormatProperties() {
        clear();
    }

    private DecimalFormatProperties _clear() {
        this.compactCustomData = null;
        this.compactStyle = null;
        this.currency = null;
        this.currencyPluralInfo = null;
        this.currencyUsage = null;
        this.decimalPatternMatchRequired = false;
        this.decimalSeparatorAlwaysShown = false;
        this.exponentSignAlwaysShown = false;
        this.formatWidth = -1;
        this.groupingSize = -1;
        this.magnitudeMultiplier = 0;
        this.mathContext = null;
        this.maximumFractionDigits = -1;
        this.maximumIntegerDigits = -1;
        this.maximumSignificantDigits = -1;
        this.minimumExponentDigits = -1;
        this.minimumFractionDigits = -1;
        this.minimumGroupingDigits = -1;
        this.minimumIntegerDigits = -1;
        this.minimumSignificantDigits = -1;
        this.multiplier = null;
        this.negativePrefix = null;
        this.negativePrefixPattern = null;
        this.negativeSuffix = null;
        this.negativeSuffixPattern = null;
        this.padPosition = null;
        this.padString = null;
        this.parseCaseSensitive = false;
        this.parseGroupingMode = null;
        this.parseIntegerOnly = false;
        this.parseMode = null;
        this.parseNoExponent = false;
        this.parseToBigDecimal = false;
        this.pluralRules = null;
        this.positivePrefix = null;
        this.positivePrefixPattern = null;
        this.positiveSuffix = null;
        this.positiveSuffixPattern = null;
        this.roundingIncrement = null;
        this.roundingMode = null;
        this.secondaryGroupingSize = -1;
        this.signAlwaysShown = false;
        return this;
    }

    private DecimalFormatProperties _copyFrom(DecimalFormatProperties other) {
        this.compactCustomData = other.compactCustomData;
        this.compactStyle = other.compactStyle;
        this.currency = other.currency;
        this.currencyPluralInfo = other.currencyPluralInfo;
        this.currencyUsage = other.currencyUsage;
        this.decimalPatternMatchRequired = other.decimalPatternMatchRequired;
        this.decimalSeparatorAlwaysShown = other.decimalSeparatorAlwaysShown;
        this.exponentSignAlwaysShown = other.exponentSignAlwaysShown;
        this.formatWidth = other.formatWidth;
        this.groupingSize = other.groupingSize;
        this.magnitudeMultiplier = other.magnitudeMultiplier;
        this.mathContext = other.mathContext;
        this.maximumFractionDigits = other.maximumFractionDigits;
        this.maximumIntegerDigits = other.maximumIntegerDigits;
        this.maximumSignificantDigits = other.maximumSignificantDigits;
        this.minimumExponentDigits = other.minimumExponentDigits;
        this.minimumFractionDigits = other.minimumFractionDigits;
        this.minimumGroupingDigits = other.minimumGroupingDigits;
        this.minimumIntegerDigits = other.minimumIntegerDigits;
        this.minimumSignificantDigits = other.minimumSignificantDigits;
        this.multiplier = other.multiplier;
        this.negativePrefix = other.negativePrefix;
        this.negativePrefixPattern = other.negativePrefixPattern;
        this.negativeSuffix = other.negativeSuffix;
        this.negativeSuffixPattern = other.negativeSuffixPattern;
        this.padPosition = other.padPosition;
        this.padString = other.padString;
        this.parseCaseSensitive = other.parseCaseSensitive;
        this.parseGroupingMode = other.parseGroupingMode;
        this.parseIntegerOnly = other.parseIntegerOnly;
        this.parseMode = other.parseMode;
        this.parseNoExponent = other.parseNoExponent;
        this.parseToBigDecimal = other.parseToBigDecimal;
        this.pluralRules = other.pluralRules;
        this.positivePrefix = other.positivePrefix;
        this.positivePrefixPattern = other.positivePrefixPattern;
        this.positiveSuffix = other.positiveSuffix;
        this.positiveSuffixPattern = other.positiveSuffixPattern;
        this.roundingIncrement = other.roundingIncrement;
        this.roundingMode = other.roundingMode;
        this.secondaryGroupingSize = other.secondaryGroupingSize;
        this.signAlwaysShown = other.signAlwaysShown;
        return this;
    }

    private boolean _equals(DecimalFormatProperties other) {
        boolean z = false;
        boolean z2 = true && _equalsHelper(this.compactCustomData, other.compactCustomData);
        z2 = z2 && _equalsHelper(this.compactStyle, other.compactStyle);
        z2 = z2 && _equalsHelper(this.currency, other.currency);
        z2 = z2 && _equalsHelper(this.currencyPluralInfo, other.currencyPluralInfo);
        z2 = z2 && _equalsHelper(this.currencyUsage, other.currencyUsage);
        z2 = z2 && _equalsHelper(this.decimalPatternMatchRequired, other.decimalPatternMatchRequired);
        z2 = z2 && _equalsHelper(this.decimalSeparatorAlwaysShown, other.decimalSeparatorAlwaysShown);
        z2 = z2 && _equalsHelper(this.exponentSignAlwaysShown, other.exponentSignAlwaysShown);
        z2 = z2 && _equalsHelper(this.formatWidth, other.formatWidth);
        z2 = z2 && _equalsHelper(this.groupingSize, other.groupingSize);
        z2 = z2 && _equalsHelper(this.magnitudeMultiplier, other.magnitudeMultiplier);
        z2 = z2 && _equalsHelper(this.mathContext, other.mathContext);
        z2 = z2 && _equalsHelper(this.maximumFractionDigits, other.maximumFractionDigits);
        z2 = z2 && _equalsHelper(this.maximumIntegerDigits, other.maximumIntegerDigits);
        z2 = z2 && _equalsHelper(this.maximumSignificantDigits, other.maximumSignificantDigits);
        z2 = z2 && _equalsHelper(this.minimumExponentDigits, other.minimumExponentDigits);
        z2 = z2 && _equalsHelper(this.minimumFractionDigits, other.minimumFractionDigits);
        z2 = z2 && _equalsHelper(this.minimumGroupingDigits, other.minimumGroupingDigits);
        z2 = z2 && _equalsHelper(this.minimumIntegerDigits, other.minimumIntegerDigits);
        z2 = z2 && _equalsHelper(this.minimumSignificantDigits, other.minimumSignificantDigits);
        z2 = z2 && _equalsHelper(this.multiplier, other.multiplier);
        z2 = z2 && _equalsHelper(this.negativePrefix, other.negativePrefix);
        z2 = z2 && _equalsHelper(this.negativePrefixPattern, other.negativePrefixPattern);
        z2 = z2 && _equalsHelper(this.negativeSuffix, other.negativeSuffix);
        z2 = z2 && _equalsHelper(this.negativeSuffixPattern, other.negativeSuffixPattern);
        z2 = z2 && _equalsHelper(this.padPosition, other.padPosition);
        z2 = z2 && _equalsHelper(this.padString, other.padString);
        z2 = z2 && _equalsHelper(this.parseCaseSensitive, other.parseCaseSensitive);
        z2 = z2 && _equalsHelper(this.parseGroupingMode, other.parseGroupingMode);
        z2 = z2 && _equalsHelper(this.parseIntegerOnly, other.parseIntegerOnly);
        z2 = z2 && _equalsHelper(this.parseMode, other.parseMode);
        z2 = z2 && _equalsHelper(this.parseNoExponent, other.parseNoExponent);
        z2 = z2 && _equalsHelper(this.parseToBigDecimal, other.parseToBigDecimal);
        z2 = z2 && _equalsHelper(this.pluralRules, other.pluralRules);
        z2 = z2 && _equalsHelper(this.positivePrefix, other.positivePrefix);
        z2 = z2 && _equalsHelper(this.positivePrefixPattern, other.positivePrefixPattern);
        z2 = z2 && _equalsHelper(this.positiveSuffix, other.positiveSuffix);
        z2 = z2 && _equalsHelper(this.positiveSuffixPattern, other.positiveSuffixPattern);
        z2 = z2 && _equalsHelper(this.roundingIncrement, other.roundingIncrement);
        z2 = z2 && _equalsHelper(this.roundingMode, other.roundingMode);
        z2 = z2 && _equalsHelper(this.secondaryGroupingSize, other.secondaryGroupingSize);
        if (z2 && _equalsHelper(this.signAlwaysShown, other.signAlwaysShown)) {
            z = true;
        }
        return z;
    }

    private boolean _equalsHelper(boolean mine, boolean theirs) {
        return mine == theirs;
    }

    private boolean _equalsHelper(int mine, int theirs) {
        return mine == theirs;
    }

    private boolean _equalsHelper(Object mine, Object theirs) {
        if (mine == theirs) {
            return true;
        }
        if (mine == null) {
            return false;
        }
        return mine.equals(theirs);
    }

    private int _hashCode() {
        return (((((((((((((((((((((((((((((((((((((((((0 ^ _hashCodeHelper(this.compactCustomData)) ^ _hashCodeHelper(this.compactStyle)) ^ _hashCodeHelper(this.currency)) ^ _hashCodeHelper(this.currencyPluralInfo)) ^ _hashCodeHelper(this.currencyUsage)) ^ _hashCodeHelper(this.decimalPatternMatchRequired)) ^ _hashCodeHelper(this.decimalSeparatorAlwaysShown)) ^ _hashCodeHelper(this.exponentSignAlwaysShown)) ^ _hashCodeHelper(this.formatWidth)) ^ _hashCodeHelper(this.groupingSize)) ^ _hashCodeHelper(this.magnitudeMultiplier)) ^ _hashCodeHelper(this.mathContext)) ^ _hashCodeHelper(this.maximumFractionDigits)) ^ _hashCodeHelper(this.maximumIntegerDigits)) ^ _hashCodeHelper(this.maximumSignificantDigits)) ^ _hashCodeHelper(this.minimumExponentDigits)) ^ _hashCodeHelper(this.minimumFractionDigits)) ^ _hashCodeHelper(this.minimumGroupingDigits)) ^ _hashCodeHelper(this.minimumIntegerDigits)) ^ _hashCodeHelper(this.minimumSignificantDigits)) ^ _hashCodeHelper(this.multiplier)) ^ _hashCodeHelper(this.negativePrefix)) ^ _hashCodeHelper(this.negativePrefixPattern)) ^ _hashCodeHelper(this.negativeSuffix)) ^ _hashCodeHelper(this.negativeSuffixPattern)) ^ _hashCodeHelper(this.padPosition)) ^ _hashCodeHelper(this.padString)) ^ _hashCodeHelper(this.parseCaseSensitive)) ^ _hashCodeHelper(this.parseGroupingMode)) ^ _hashCodeHelper(this.parseIntegerOnly)) ^ _hashCodeHelper(this.parseMode)) ^ _hashCodeHelper(this.parseNoExponent)) ^ _hashCodeHelper(this.parseToBigDecimal)) ^ _hashCodeHelper(this.pluralRules)) ^ _hashCodeHelper(this.positivePrefix)) ^ _hashCodeHelper(this.positivePrefixPattern)) ^ _hashCodeHelper(this.positiveSuffix)) ^ _hashCodeHelper(this.positiveSuffixPattern)) ^ _hashCodeHelper(this.roundingIncrement)) ^ _hashCodeHelper(this.roundingMode)) ^ _hashCodeHelper(this.secondaryGroupingSize)) ^ _hashCodeHelper(this.signAlwaysShown);
    }

    private int _hashCodeHelper(boolean value) {
        return value;
    }

    private int _hashCodeHelper(int value) {
        return value * 13;
    }

    private int _hashCodeHelper(Object value) {
        if (value == null) {
            return 0;
        }
        return value.hashCode();
    }

    public DecimalFormatProperties clear() {
        return _clear();
    }

    public DecimalFormatProperties clone() {
        try {
            return (DecimalFormatProperties) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public DecimalFormatProperties copyFrom(DecimalFormatProperties other) {
        return _copyFrom(other);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof DecimalFormatProperties) {
            return _equals((DecimalFormatProperties) other);
        }
        return false;
    }

    public Map<String, Map<String, String>> getCompactCustomData() {
        return this.compactCustomData;
    }

    public CompactStyle getCompactStyle() {
        return this.compactStyle;
    }

    public Currency getCurrency() {
        return this.currency;
    }

    public CurrencyPluralInfo getCurrencyPluralInfo() {
        return this.currencyPluralInfo;
    }

    public CurrencyUsage getCurrencyUsage() {
        return this.currencyUsage;
    }

    public boolean getDecimalPatternMatchRequired() {
        return this.decimalPatternMatchRequired;
    }

    public boolean getDecimalSeparatorAlwaysShown() {
        return this.decimalSeparatorAlwaysShown;
    }

    public boolean getExponentSignAlwaysShown() {
        return this.exponentSignAlwaysShown;
    }

    public int getFormatWidth() {
        return this.formatWidth;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public int getMagnitudeMultiplier() {
        return this.magnitudeMultiplier;
    }

    public MathContext getMathContext() {
        return this.mathContext;
    }

    public int getMaximumFractionDigits() {
        return this.maximumFractionDigits;
    }

    public int getMaximumIntegerDigits() {
        return this.maximumIntegerDigits;
    }

    public int getMaximumSignificantDigits() {
        return this.maximumSignificantDigits;
    }

    public int getMinimumExponentDigits() {
        return this.minimumExponentDigits;
    }

    public int getMinimumFractionDigits() {
        return this.minimumFractionDigits;
    }

    public int getMinimumGroupingDigits() {
        return this.minimumGroupingDigits;
    }

    public int getMinimumIntegerDigits() {
        return this.minimumIntegerDigits;
    }

    public int getMinimumSignificantDigits() {
        return this.minimumSignificantDigits;
    }

    public BigDecimal getMultiplier() {
        return this.multiplier;
    }

    public String getNegativePrefix() {
        return this.negativePrefix;
    }

    public String getNegativePrefixPattern() {
        return this.negativePrefixPattern;
    }

    public String getNegativeSuffix() {
        return this.negativeSuffix;
    }

    public String getNegativeSuffixPattern() {
        return this.negativeSuffixPattern;
    }

    public PadPosition getPadPosition() {
        return this.padPosition;
    }

    public String getPadString() {
        return this.padString;
    }

    public boolean getParseCaseSensitive() {
        return this.parseCaseSensitive;
    }

    public GroupingMode getParseGroupingMode() {
        return this.parseGroupingMode;
    }

    public boolean getParseIntegerOnly() {
        return this.parseIntegerOnly;
    }

    public ParseMode getParseMode() {
        return this.parseMode;
    }

    public boolean getParseNoExponent() {
        return this.parseNoExponent;
    }

    public boolean getParseToBigDecimal() {
        return this.parseToBigDecimal;
    }

    public PluralRules getPluralRules() {
        return this.pluralRules;
    }

    public String getPositivePrefix() {
        return this.positivePrefix;
    }

    public String getPositivePrefixPattern() {
        return this.positivePrefixPattern;
    }

    public String getPositiveSuffix() {
        return this.positiveSuffix;
    }

    public String getPositiveSuffixPattern() {
        return this.positiveSuffixPattern;
    }

    public BigDecimal getRoundingIncrement() {
        return this.roundingIncrement;
    }

    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    public int getSecondaryGroupingSize() {
        return this.secondaryGroupingSize;
    }

    public boolean getSignAlwaysShown() {
        return this.signAlwaysShown;
    }

    public int hashCode() {
        return _hashCode();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        readObjectImpl(ois);
    }

    void readObjectImpl(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        clear();
        ois.readInt();
        int count = ois.readInt();
        for (int i = 0; i < count; i++) {
            String name = (String) ois.readObject();
            try {
                try {
                    DecimalFormatProperties.class.getDeclaredField(name).set(this, ois.readObject());
                } catch (IllegalArgumentException e) {
                    throw new AssertionError(e);
                } catch (IllegalAccessException e2) {
                    throw new AssertionError(e2);
                }
            } catch (NoSuchFieldException e3) {
            } catch (SecurityException e4) {
                throw new AssertionError(e4);
            }
        }
    }

    public DecimalFormatProperties setCompactCustomData(Map<String, Map<String, String>> compactCustomData) {
        this.compactCustomData = compactCustomData;
        return this;
    }

    public DecimalFormatProperties setCompactStyle(CompactStyle compactStyle) {
        this.compactStyle = compactStyle;
        return this;
    }

    public DecimalFormatProperties setCurrency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public DecimalFormatProperties setCurrencyPluralInfo(CurrencyPluralInfo currencyPluralInfo) {
        if (currencyPluralInfo != null) {
            currencyPluralInfo = (CurrencyPluralInfo) currencyPluralInfo.clone();
        }
        this.currencyPluralInfo = currencyPluralInfo;
        return this;
    }

    public DecimalFormatProperties setCurrencyUsage(CurrencyUsage currencyUsage) {
        this.currencyUsage = currencyUsage;
        return this;
    }

    public DecimalFormatProperties setDecimalPatternMatchRequired(boolean decimalPatternMatchRequired) {
        this.decimalPatternMatchRequired = decimalPatternMatchRequired;
        return this;
    }

    public DecimalFormatProperties setDecimalSeparatorAlwaysShown(boolean alwaysShowDecimal) {
        this.decimalSeparatorAlwaysShown = alwaysShowDecimal;
        return this;
    }

    public DecimalFormatProperties setExponentSignAlwaysShown(boolean exponentSignAlwaysShown) {
        this.exponentSignAlwaysShown = exponentSignAlwaysShown;
        return this;
    }

    public DecimalFormatProperties setFormatWidth(int paddingWidth) {
        this.formatWidth = paddingWidth;
        return this;
    }

    public DecimalFormatProperties setGroupingSize(int groupingSize) {
        this.groupingSize = groupingSize;
        return this;
    }

    public DecimalFormatProperties setMagnitudeMultiplier(int magnitudeMultiplier) {
        this.magnitudeMultiplier = magnitudeMultiplier;
        return this;
    }

    public DecimalFormatProperties setMathContext(MathContext mathContext) {
        this.mathContext = mathContext;
        return this;
    }

    public DecimalFormatProperties setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
        return this;
    }

    public DecimalFormatProperties setMaximumIntegerDigits(int maximumIntegerDigits) {
        this.maximumIntegerDigits = maximumIntegerDigits;
        return this;
    }

    public DecimalFormatProperties setMaximumSignificantDigits(int maximumSignificantDigits) {
        this.maximumSignificantDigits = maximumSignificantDigits;
        return this;
    }

    public DecimalFormatProperties setMinimumExponentDigits(int minimumExponentDigits) {
        this.minimumExponentDigits = minimumExponentDigits;
        return this;
    }

    public DecimalFormatProperties setMinimumFractionDigits(int minimumFractionDigits) {
        this.minimumFractionDigits = minimumFractionDigits;
        return this;
    }

    public DecimalFormatProperties setMinimumGroupingDigits(int minimumGroupingDigits) {
        this.minimumGroupingDigits = minimumGroupingDigits;
        return this;
    }

    public DecimalFormatProperties setMinimumIntegerDigits(int minimumIntegerDigits) {
        this.minimumIntegerDigits = minimumIntegerDigits;
        return this;
    }

    public DecimalFormatProperties setMinimumSignificantDigits(int minimumSignificantDigits) {
        this.minimumSignificantDigits = minimumSignificantDigits;
        return this;
    }

    public DecimalFormatProperties setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    public DecimalFormatProperties setNegativePrefix(String negativePrefix) {
        this.negativePrefix = negativePrefix;
        return this;
    }

    public DecimalFormatProperties setNegativePrefixPattern(String negativePrefixPattern) {
        this.negativePrefixPattern = negativePrefixPattern;
        return this;
    }

    public DecimalFormatProperties setNegativeSuffix(String negativeSuffix) {
        this.negativeSuffix = negativeSuffix;
        return this;
    }

    public DecimalFormatProperties setNegativeSuffixPattern(String negativeSuffixPattern) {
        this.negativeSuffixPattern = negativeSuffixPattern;
        return this;
    }

    public DecimalFormatProperties setPadPosition(PadPosition paddingLocation) {
        this.padPosition = paddingLocation;
        return this;
    }

    public DecimalFormatProperties setPadString(String paddingString) {
        this.padString = paddingString;
        return this;
    }

    public DecimalFormatProperties setParseCaseSensitive(boolean parseCaseSensitive) {
        this.parseCaseSensitive = parseCaseSensitive;
        return this;
    }

    public DecimalFormatProperties setParseGroupingMode(GroupingMode parseGroupingMode) {
        this.parseGroupingMode = parseGroupingMode;
        return this;
    }

    public DecimalFormatProperties setParseIntegerOnly(boolean parseIntegerOnly) {
        this.parseIntegerOnly = parseIntegerOnly;
        return this;
    }

    public DecimalFormatProperties setParseMode(ParseMode parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    public DecimalFormatProperties setParseNoExponent(boolean parseNoExponent) {
        this.parseNoExponent = parseNoExponent;
        return this;
    }

    public DecimalFormatProperties setParseToBigDecimal(boolean parseToBigDecimal) {
        this.parseToBigDecimal = parseToBigDecimal;
        return this;
    }

    public DecimalFormatProperties setPluralRules(PluralRules pluralRules) {
        this.pluralRules = pluralRules;
        return this;
    }

    public DecimalFormatProperties setPositivePrefix(String positivePrefix) {
        this.positivePrefix = positivePrefix;
        return this;
    }

    public DecimalFormatProperties setPositivePrefixPattern(String positivePrefixPattern) {
        this.positivePrefixPattern = positivePrefixPattern;
        return this;
    }

    public DecimalFormatProperties setPositiveSuffix(String positiveSuffix) {
        this.positiveSuffix = positiveSuffix;
        return this;
    }

    public DecimalFormatProperties setPositiveSuffixPattern(String positiveSuffixPattern) {
        this.positiveSuffixPattern = positiveSuffixPattern;
        return this;
    }

    public DecimalFormatProperties setRoundingIncrement(BigDecimal roundingIncrement) {
        this.roundingIncrement = roundingIncrement;
        return this;
    }

    public DecimalFormatProperties setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        return this;
    }

    public DecimalFormatProperties setSecondaryGroupingSize(int secondaryGroupingSize) {
        this.secondaryGroupingSize = secondaryGroupingSize;
        return this;
    }

    public DecimalFormatProperties setSignAlwaysShown(boolean signAlwaysShown) {
        this.signAlwaysShown = signAlwaysShown;
        return this;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("<Properties");
        toStringBare(result);
        result.append(">");
        return result.toString();
    }

    public void toStringBare(StringBuilder result) {
        Field[] fields = DecimalFormatProperties.class.getDeclaredFields();
        int length = fields.length;
        int i = 0;
        while (i < length) {
            Field field = fields[i];
            try {
                Object myValue = field.get(this);
                Object defaultValue = field.get(DEFAULT);
                StringBuilder stringBuilder;
                if (myValue == null && defaultValue == null) {
                    i++;
                } else if (myValue == null || defaultValue == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                    stringBuilder.append(field.getName());
                    stringBuilder.append(":");
                    stringBuilder.append(myValue);
                    result.append(stringBuilder.toString());
                    i++;
                } else {
                    if (!myValue.equals(defaultValue)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                        stringBuilder.append(field.getName());
                        stringBuilder.append(":");
                        stringBuilder.append(myValue);
                        result.append(stringBuilder.toString());
                    }
                    i++;
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e2) {
                e2.printStackTrace();
            }
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        writeObjectImpl(oos);
    }

    void writeObjectImpl(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        int i = 0;
        oos.writeInt(0);
        ArrayList<Field> fieldsToSerialize = new ArrayList();
        ArrayList<Object> valuesToSerialize = new ArrayList();
        for (Field field : DecimalFormatProperties.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                try {
                    Object myValue = field.get(this);
                    if (myValue != null) {
                        if (!myValue.equals(field.get(DEFAULT))) {
                            fieldsToSerialize.add(field);
                            valuesToSerialize.add(myValue);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new AssertionError(e);
                } catch (IllegalAccessException e2) {
                    throw new AssertionError(e2);
                }
            }
        }
        int size = fieldsToSerialize.size();
        oos.writeInt(size);
        while (i < size) {
            Field field2 = (Field) fieldsToSerialize.get(i);
            Object value = valuesToSerialize.get(i);
            oos.writeObject(field2.getName());
            oos.writeObject(value);
            i++;
        }
    }
}
