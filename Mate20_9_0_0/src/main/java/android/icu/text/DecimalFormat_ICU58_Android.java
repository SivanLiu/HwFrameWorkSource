package android.icu.text;

import android.icu.impl.ICUConfig;
import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.impl.locale.LanguageTag;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.math.MathContext;
import android.icu.text.NumberFormat.Field;
import android.icu.text.PluralRules.FixedDecimal;
import android.icu.util.Currency;
import android.icu.util.Currency.CurrencyUsage;
import android.icu.util.CurrencyAmount;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Deprecated
public class DecimalFormat_ICU58_Android extends NumberFormat {
    private static final char CURRENCY_SIGN = '¤';
    private static final int CURRENCY_SIGN_COUNT_IN_ISO_FORMAT = 2;
    private static final int CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT = 3;
    private static final int CURRENCY_SIGN_COUNT_IN_SYMBOL_FORMAT = 1;
    private static final int CURRENCY_SIGN_COUNT_ZERO = 0;
    static final int DOUBLE_FRACTION_DIGITS = 340;
    static final int DOUBLE_INTEGER_DIGITS = 309;
    static final int MAX_INTEGER_DIGITS = 2000000000;
    static final int MAX_SCIENTIFIC_INTEGER_DIGITS = 8;
    static final Unit NULL_UNIT = new Unit("", "");
    public static final int PAD_AFTER_PREFIX = 1;
    public static final int PAD_AFTER_SUFFIX = 3;
    public static final int PAD_BEFORE_PREFIX = 0;
    public static final int PAD_BEFORE_SUFFIX = 2;
    static final char PATTERN_DECIMAL_SEPARATOR = '.';
    static final char PATTERN_DIGIT = '#';
    static final char PATTERN_EIGHT_DIGIT = '8';
    static final char PATTERN_EXPONENT = 'E';
    static final char PATTERN_FIVE_DIGIT = '5';
    static final char PATTERN_FOUR_DIGIT = '4';
    static final char PATTERN_GROUPING_SEPARATOR = ',';
    static final char PATTERN_MINUS_SIGN = '-';
    static final char PATTERN_NINE_DIGIT = '9';
    static final char PATTERN_ONE_DIGIT = '1';
    static final char PATTERN_PAD_ESCAPE = '*';
    private static final char PATTERN_PERCENT = '%';
    private static final char PATTERN_PER_MILLE = '‰';
    static final char PATTERN_PLUS_SIGN = '+';
    private static final char PATTERN_SEPARATOR = ';';
    static final char PATTERN_SEVEN_DIGIT = '7';
    static final char PATTERN_SIGNIFICANT_DIGIT = '@';
    static final char PATTERN_SIX_DIGIT = '6';
    static final char PATTERN_THREE_DIGIT = '3';
    static final char PATTERN_TWO_DIGIT = '2';
    static final char PATTERN_ZERO_DIGIT = '0';
    private static final char QUOTE = '\'';
    private static final int STATUS_INFINITE = 0;
    private static final int STATUS_LENGTH = 3;
    private static final int STATUS_POSITIVE = 1;
    private static final int STATUS_UNDERFLOW = 2;
    private static final UnicodeSet commaEquivalents = new UnicodeSet(44, 44, 1548, 1548, 1643, 1643, UProperty.DOUBLE_LIMIT, UProperty.DOUBLE_LIMIT, 65040, 65041, 65104, 65105, 65292, 65292, 65380, 65380).freeze();
    static final int currentSerialVersion = 4;
    private static final UnicodeSet defaultGroupingSeparators = new UnicodeSet(32, 32, 39, 39, 44, 44, 46, 46, 160, 160, 1548, 1548, 1643, 1644, 8192, 8202, 8216, 8217, 8228, 8228, 8239, 8239, 8287, 8287, 12288, 12290, 65040, 65042, 65104, 65106, 65287, 65287, 65292, 65292, 65294, 65294, 65377, 65377, 65380, 65380).freeze();
    private static final UnicodeSet dotEquivalents = new UnicodeSet(46, 46, 8228, 8228, 12290, 12290, 65042, 65042, 65106, 65106, 65294, 65294, 65377, 65377).freeze();
    private static double epsilon = 1.0E-11d;
    static final UnicodeSet minusSigns = new UnicodeSet(45, 45, 8315, 8315, 8331, 8331, 8722, 8722, 10134, 10134, 65123, 65123, 65293, 65293).freeze();
    static final UnicodeSet plusSigns = new UnicodeSet(43, 43, 8314, 8314, 8330, 8330, 10133, 10133, 64297, 64297, 65122, 65122, 65291, 65291).freeze();
    static final double roundingIncrementEpsilon = 1.0E-9d;
    private static final long serialVersionUID = 864413376551465018L;
    static final boolean skipExtendedSeparatorParsing = ICUConfig.get("android.icu.text.DecimalFormat.SkipExtendedSeparatorParsing", "false").equals("true");
    private static final UnicodeSet strictCommaEquivalents = new UnicodeSet(44, 44, 1643, 1643, 65040, 65040, 65104, 65104, 65292, 65292).freeze();
    private static final UnicodeSet strictDefaultGroupingSeparators = new UnicodeSet(32, 32, 39, 39, 44, 44, 46, 46, 160, 160, 1643, 1644, 8192, 8202, 8216, 8217, 8228, 8228, 8239, 8239, 8287, 8287, 12288, 12288, 65040, 65040, 65104, 65104, 65106, 65106, 65287, 65287, 65292, 65292, 65294, 65294, 65377, 65377).freeze();
    private static final UnicodeSet strictDotEquivalents = new UnicodeSet(46, 46, 8228, 8228, 65106, 65106, 65294, 65294, 65377, 65377).freeze();
    private int PARSE_MAX_EXPONENT = 1000;
    private transient BigDecimal actualRoundingIncrement = null;
    private transient android.icu.math.BigDecimal actualRoundingIncrementICU = null;
    private transient Set<AffixForCurrency> affixPatternsForCurrency = null;
    private ArrayList<FieldPosition> attributes = new ArrayList();
    private ChoiceFormat currencyChoice;
    private CurrencyPluralInfo currencyPluralInfo = null;
    private int currencySignCount = 0;
    private CurrencyUsage currencyUsage = CurrencyUsage.STANDARD;
    private boolean decimalSeparatorAlwaysShown = false;
    private transient DigitList_Android digitList = new DigitList_Android();
    private boolean exponentSignAlwaysShown = false;
    private String formatPattern = "";
    private int formatWidth = 0;
    private byte groupingSize = (byte) 3;
    private byte groupingSize2 = (byte) 0;
    private transient boolean isReadyForParsing = false;
    private MathContext mathContext = new MathContext(0, 0);
    private int maxSignificantDigits = 6;
    private byte minExponentDigits;
    private int minSignificantDigits = 1;
    private int multiplier = 1;
    private String negPrefixPattern;
    private String negSuffixPattern;
    private String negativePrefix = LanguageTag.SEP;
    private String negativeSuffix = "";
    private char pad = ' ';
    private int padPosition = 0;
    private boolean parseBigDecimal = false;
    boolean parseRequireDecimalPoint = false;
    private String posPrefixPattern;
    private String posSuffixPattern;
    private String positivePrefix = "";
    private String positiveSuffix = "";
    private transient double roundingDouble = 0.0d;
    private transient double roundingDoubleReciprocal = 0.0d;
    private BigDecimal roundingIncrement = null;
    private transient android.icu.math.BigDecimal roundingIncrementICU = null;
    private int roundingMode = 6;
    private int serialVersionOnStream = 4;
    private int style = 0;
    private DecimalFormatSymbols symbols = null;
    private boolean useExponentialNotation;
    private boolean useSignificantDigits = false;

    private static final class AffixForCurrency {
        private String negPrefixPatternForCurrency = null;
        private String negSuffixPatternForCurrency = null;
        private final int patternType;
        private String posPrefixPatternForCurrency = null;
        private String posSuffixPatternForCurrency = null;

        public AffixForCurrency(String negPrefix, String negSuffix, String posPrefix, String posSuffix, int type) {
            this.negPrefixPatternForCurrency = negPrefix;
            this.negSuffixPatternForCurrency = negSuffix;
            this.posPrefixPatternForCurrency = posPrefix;
            this.posSuffixPatternForCurrency = posSuffix;
            this.patternType = type;
        }

        public String getNegPrefix() {
            return this.negPrefixPatternForCurrency;
        }

        public String getNegSuffix() {
            return this.negSuffixPatternForCurrency;
        }

        public String getPosPrefix() {
            return this.posPrefixPatternForCurrency;
        }

        public String getPosSuffix() {
            return this.posSuffixPatternForCurrency;
        }

        public int getPatternType() {
            return this.patternType;
        }
    }

    static class Unit {
        private final String prefix;
        private final String suffix;

        public Unit(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public void writeSuffix(StringBuffer toAppendTo) {
            toAppendTo.append(this.suffix);
        }

        public void writePrefix(StringBuffer toAppendTo) {
            toAppendTo.append(this.prefix);
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Unit)) {
                return false;
            }
            Unit other = (Unit) obj;
            if (!(this.prefix.equals(other.prefix) && this.suffix.equals(other.suffix))) {
                z = false;
            }
            return z;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.prefix);
            stringBuilder.append("/");
            stringBuilder.append(this.suffix);
            return stringBuilder.toString();
        }
    }

    public DecimalFormat_ICU58_Android() {
        ULocale def = ULocale.getDefault(Category.FORMAT);
        String pattern = NumberFormat.getPattern(def, 0);
        this.symbols = new DecimalFormatSymbols(def);
        setCurrency(Currency.getInstance(def));
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(def);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat_ICU58_Android(String pattern) {
        ULocale def = ULocale.getDefault(Category.FORMAT);
        this.symbols = new DecimalFormatSymbols(def);
        setCurrency(Currency.getInstance(def));
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(def);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat_ICU58_Android(String pattern, DecimalFormatSymbols symbols) {
        createFromPatternAndSymbols(pattern, symbols);
    }

    private void createFromPatternAndSymbols(String pattern, DecimalFormatSymbols inputSymbols) {
        this.symbols = (DecimalFormatSymbols) inputSymbols.clone();
        if (pattern.indexOf(164) >= 0) {
            setCurrencyForSymbols();
        }
        applyPatternWithoutExpandAffix(pattern, false);
        if (this.currencySignCount == 3) {
            this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    public DecimalFormat_ICU58_Android(String pattern, DecimalFormatSymbols symbols, CurrencyPluralInfo infoInput, int style) {
        CurrencyPluralInfo info = infoInput;
        if (style == 6) {
            info = (CurrencyPluralInfo) infoInput.clone();
        }
        create(pattern, symbols, info, style);
    }

    private void create(String pattern, DecimalFormatSymbols inputSymbols, CurrencyPluralInfo info, int inputStyle) {
        if (inputStyle != 6) {
            createFromPatternAndSymbols(pattern, inputSymbols);
        } else {
            this.symbols = (DecimalFormatSymbols) inputSymbols.clone();
            this.currencyPluralInfo = info;
            applyPatternWithoutExpandAffix(this.currencyPluralInfo.getCurrencyPluralPattern(PluralRules.KEYWORD_OTHER), false);
            setCurrencyForSymbols();
        }
        this.style = inputStyle;
    }

    @Deprecated
    public DecimalFormat_ICU58_Android(String pattern, DecimalFormatSymbols inputSymbols, int style) {
        CurrencyPluralInfo info = null;
        if (style == 6) {
            info = new CurrencyPluralInfo(inputSymbols.getULocale());
        }
        create(pattern, inputSymbols, info, style);
    }

    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private boolean isNegative(double number) {
        return number < 0.0d || (number == 0.0d && 1.0d / number < 0.0d);
    }

    private double round(double number) {
        boolean isNegative = isNegative(number);
        if (isNegative) {
            number = -number;
        }
        if (this.roundingDouble <= 0.0d) {
            return number;
        }
        return round(number, this.roundingDouble, this.roundingDoubleReciprocal, this.roundingMode, isNegative);
    }

    private double multiply(double number) {
        if (this.multiplier != 1) {
            return ((double) this.multiplier) * number;
        }
        return number;
    }

    private StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        Throwable th;
        int i;
        StringBuffer stringBuffer = result;
        FieldPosition fieldPosition2 = fieldPosition;
        boolean z = false;
        fieldPosition2.setBeginIndex(0);
        fieldPosition2.setEndIndex(0);
        if (Double.isNaN(number)) {
            if (fieldPosition.getField() == 0) {
                fieldPosition2.setBeginIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
                fieldPosition2.setBeginIndex(result.length());
            }
            stringBuffer.append(this.symbols.getNaN());
            if (parseAttr) {
                addAttribute(Field.INTEGER, result.length() - this.symbols.getNaN().length(), result.length());
            }
            if (fieldPosition.getField() == 0) {
                fieldPosition2.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
                fieldPosition2.setEndIndex(result.length());
            }
            addPadding(stringBuffer, fieldPosition2, 0, 0);
            return stringBuffer;
        }
        double number2 = multiply(number);
        boolean isNegative = isNegative(number2);
        double number3 = round(number2);
        if (Double.isInfinite(number3)) {
            int prefixLen = appendAffix(stringBuffer, isNegative, true, fieldPosition2, parseAttr);
            if (fieldPosition.getField() == 0) {
                fieldPosition2.setBeginIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
                fieldPosition2.setBeginIndex(result.length());
            }
            stringBuffer.append(this.symbols.getInfinity());
            if (parseAttr) {
                addAttribute(Field.INTEGER, result.length() - this.symbols.getInfinity().length(), result.length());
            }
            if (fieldPosition.getField() == 0) {
                fieldPosition2.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
                fieldPosition2.setEndIndex(result.length());
            }
            addPadding(stringBuffer, fieldPosition2, prefixLen, appendAffix(stringBuffer, isNegative, false, fieldPosition2, parseAttr));
            return stringBuffer;
        }
        double number4;
        int precision = precision(false);
        if (!this.useExponentialNotation || precision <= 0 || number3 == 0.0d || this.roundingMode == 6) {
            number4 = number3;
        } else {
            int log10RoundingIncr = (1 - precision) + ((int) Math.floor(Math.log10(Math.abs(number3))));
            double roundingInc = 0.0d;
            number4 = 0.0d;
            if (log10RoundingIncr < 0) {
                roundingInc = android.icu.math.BigDecimal.ONE.movePointRight(-log10RoundingIncr).doubleValue();
            } else {
                number4 = android.icu.math.BigDecimal.ONE.movePointRight(log10RoundingIncr).doubleValue();
            }
            double roundingIncReciprocal = roundingInc;
            number4 = round(number3, number4, roundingIncReciprocal, this.roundingMode, isNegative);
        }
        synchronized (this.digitList) {
            try {
                DigitList_Android digitList_Android = this.digitList;
                if (!this.useExponentialNotation) {
                    try {
                        if (!areSignificantDigitsUsed()) {
                            z = true;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        i = precision;
                        throw th;
                    }
                }
                digitList_Android.set(number4, precision, z);
                StringBuffer subformat = subformat(number4, stringBuffer, fieldPosition2, isNegative, false, parseAttr);
                return subformat;
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    @Deprecated
    double adjustNumberAsInFormatting(double number) {
        if (Double.isNaN(number)) {
            return number;
        }
        number = round(multiply(number));
        if (Double.isInfinite(number)) {
            return number;
        }
        return toDigitList(number).getDouble();
    }

    @Deprecated
    DigitList_Android toDigitList(double number) {
        DigitList_Android result = new DigitList_Android();
        result.set(number, precision(false), false);
        return result;
    }

    @Deprecated
    boolean isNumberNegative(double number) {
        if (Double.isNaN(number)) {
            return false;
        }
        return isNegative(multiply(number));
    }

    private static double round(double number, double roundingInc, double roundingIncReciprocal, int mode, boolean isNegative) {
        int i = mode;
        double div = roundingIncReciprocal == 0.0d ? number / roundingInc : number * roundingIncReciprocal;
        if (i != 7) {
            switch (i) {
                case 0:
                    div = Math.ceil(div - epsilon);
                    break;
                case 1:
                    div = Math.floor(epsilon + div);
                    break;
                case 2:
                    div = isNegative ? Math.floor(epsilon + div) : Math.ceil(div - epsilon);
                    break;
                case 3:
                    div = isNegative ? Math.ceil(div - epsilon) : Math.floor(epsilon + div);
                    break;
                default:
                    double ceil = Math.ceil(div);
                    double ceildiff = ceil - div;
                    double floor = Math.floor(div);
                    double floordiff = div - floor;
                    switch (i) {
                        case 4:
                            div = ceildiff <= epsilon + floordiff ? ceil : floor;
                            break;
                        case 5:
                            div = floordiff <= epsilon + ceildiff ? floor : ceil;
                            break;
                        case 6:
                            if (epsilon + floordiff >= ceildiff) {
                                if (epsilon + ceildiff >= floordiff) {
                                    double testFloor = floor / 2.0d;
                                    div = testFloor == Math.floor(testFloor) ? floor : ceil;
                                    break;
                                }
                                div = ceil;
                                break;
                            }
                            div = floor;
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid rounding mode: ");
                            stringBuilder.append(i);
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
            }
            return roundingIncReciprocal == 0.0d ? div * roundingInc : div / roundingIncReciprocal;
        } else if (div == Math.floor(div)) {
            return number;
        } else {
            throw new ArithmeticException("Rounding necessary");
        }
    }

    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        Throwable th;
        long number2 = number;
        StringBuffer stringBuffer = result;
        FieldPosition fieldPosition2 = fieldPosition;
        boolean tooBig = false;
        fieldPosition2.setBeginIndex(0);
        fieldPosition2.setEndIndex(0);
        if (this.actualRoundingIncrementICU != null) {
            return format(android.icu.math.BigDecimal.valueOf(number), stringBuffer, fieldPosition2);
        }
        boolean isNegative = number2 < 0;
        if (isNegative) {
            number2 = -number2;
        }
        if (this.multiplier != 1) {
            if (number2 < 0) {
                if (number2 <= Long.MIN_VALUE / ((long) this.multiplier)) {
                    tooBig = true;
                }
            } else if (number2 > Long.MAX_VALUE / ((long) this.multiplier)) {
                tooBig = true;
            }
            if (tooBig) {
                return format(BigInteger.valueOf(isNegative ? -number2 : number2), stringBuffer, fieldPosition2, parseAttr);
            }
        }
        boolean z = parseAttr;
        long number3 = number2 * ((long) this.multiplier);
        DigitList_Android digitList_Android = this.digitList;
        synchronized (digitList_Android) {
            DigitList_Android digitList_Android2;
            try {
                this.digitList.set(number3, precision(true));
                if (this.digitList.wasRounded()) {
                    if (this.roundingMode == 7) {
                        throw new ArithmeticException("Rounding necessary");
                    }
                }
                digitList_Android2 = digitList_Android;
                StringBuffer subformat = subformat((double) number3, stringBuffer, fieldPosition2, isNegative, true, z);
                return subformat;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        if (this.actualRoundingIncrementICU != null) {
            return format(new android.icu.math.BigDecimal(number), result, fieldPosition);
        }
        StringBuffer subformat;
        boolean z = true;
        if (this.multiplier != 1) {
            number = number.multiply(BigInteger.valueOf((long) this.multiplier));
        }
        synchronized (this.digitList) {
            this.digitList.set(number, precision(true));
            if (this.digitList.wasRounded()) {
                if (this.roundingMode == 7) {
                    throw new ArithmeticException("Rounding necessary");
                }
            }
            int intValue = number.intValue();
            if (number.signum() >= 0) {
                z = false;
            }
            subformat = subformat(intValue, result, fieldPosition, z, true, parseAttr);
        }
        return subformat;
    }

    public StringBuffer format(BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(BigDecimal number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        StringBuffer subformat;
        if (this.multiplier != 1) {
            number = number.multiply(BigDecimal.valueOf((long) this.multiplier));
        }
        if (this.actualRoundingIncrement != null) {
            number = number.divide(this.actualRoundingIncrement, 0, this.roundingMode).multiply(this.actualRoundingIncrement);
        }
        synchronized (this.digitList) {
            DigitList_Android digitList_Android = this.digitList;
            int precision = precision(false);
            boolean z = (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true;
            digitList_Android.set(number, precision, z);
            if (this.digitList.wasRounded()) {
                if (this.roundingMode == 7) {
                    throw new ArithmeticException("Rounding necessary");
                }
            }
            subformat = subformat(number.doubleValue(), result, fieldPosition, number.signum() < 0, false, parseAttr);
        }
        return subformat;
    }

    public StringBuffer format(android.icu.math.BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
        StringBuffer subformat;
        if (this.multiplier != 1) {
            number = number.multiply(android.icu.math.BigDecimal.valueOf((long) this.multiplier), this.mathContext);
        }
        if (this.actualRoundingIncrementICU != null) {
            number = number.divide(this.actualRoundingIncrementICU, 0, this.roundingMode).multiply(this.actualRoundingIncrementICU, this.mathContext);
        }
        synchronized (this.digitList) {
            DigitList_Android digitList_Android = this.digitList;
            int precision = precision(false);
            boolean z = (this.useExponentialNotation || areSignificantDigitsUsed()) ? false : true;
            digitList_Android.set(number, precision, z);
            if (this.digitList.wasRounded()) {
                if (this.roundingMode == 7) {
                    throw new ArithmeticException("Rounding necessary");
                }
            }
            subformat = subformat(number.doubleValue(), result, fieldPosition, number.signum() < 0, false, false);
        }
        return subformat;
    }

    private boolean isGroupingPosition(int pos) {
        if (!isGroupingUsed() || pos <= 0 || this.groupingSize <= (byte) 0) {
            return false;
        }
        boolean z = false;
        if (this.groupingSize2 <= (byte) 0 || pos <= this.groupingSize) {
            if (pos % this.groupingSize == 0) {
                z = true;
            }
            return z;
        }
        if ((pos - this.groupingSize) % this.groupingSize2 == 0) {
            z = true;
        }
        return z;
    }

    private int precision(boolean isIntegral) {
        if (areSignificantDigitsUsed()) {
            return getMaximumSignificantDigits();
        }
        if (this.useExponentialNotation) {
            return getMinimumIntegerDigits() + getMaximumFractionDigits();
        }
        return isIntegral ? 0 : getMaximumFractionDigits();
    }

    private StringBuffer subformat(int number, StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.currencySignCount != 3) {
            return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
        }
        return subformat(this.currencyPluralInfo.select(getFixedDecimal((double) number)), result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    FixedDecimal getFixedDecimal(double number) {
        return getFixedDecimal(number, this.digitList);
    }

    FixedDecimal getFixedDecimal(double number, DigitList_Android dl) {
        int maxFractionalDigits;
        int minFractionalDigits;
        long f;
        DigitList_Android digitList_Android = dl;
        int fractionalDigitsInDigitList = digitList_Android.count - digitList_Android.decimalAt;
        if (this.useSignificantDigits) {
            maxFractionalDigits = this.maxSignificantDigits - digitList_Android.decimalAt;
            minFractionalDigits = this.minSignificantDigits - digitList_Android.decimalAt;
            if (minFractionalDigits < 0) {
                minFractionalDigits = 0;
            }
            if (maxFractionalDigits < 0) {
                maxFractionalDigits = 0;
            }
        } else {
            maxFractionalDigits = getMaximumFractionDigits();
            minFractionalDigits = getMinimumFractionDigits();
        }
        int v = fractionalDigitsInDigitList;
        if (v < minFractionalDigits) {
            v = minFractionalDigits;
        } else if (v > maxFractionalDigits) {
            v = maxFractionalDigits;
        }
        long f2 = 0;
        if (v > 0) {
            for (int i = Math.max(0, digitList_Android.decimalAt); i < digitList_Android.count; i++) {
                f2 = (f2 * 10) + ((long) (digitList_Android.digits[i] - 48));
            }
            long f3 = f2;
            for (f2 = v; f2 < fractionalDigitsInDigitList; f2++) {
                f3 *= 10;
            }
            f = f3;
        } else {
            f = 0;
        }
        return new FixedDecimal(number, v, f);
    }

    private StringBuffer subformat(double number, StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.currencySignCount != 3) {
            return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
        }
        return subformat(this.currencyPluralInfo.select(getFixedDecimal(number)), result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    private StringBuffer subformat(String pluralCount, StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.style == 6) {
            String currencyPluralPattern = this.currencyPluralInfo.getCurrencyPluralPattern(pluralCount);
            if (!this.formatPattern.equals(currencyPluralPattern)) {
                applyPatternWithoutExpandAffix(currencyPluralPattern, false);
            }
        }
        expandAffixAdjustWidth(pluralCount);
        return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    private StringBuffer subformat(StringBuffer result, FieldPosition fieldPosition, boolean isNegative, boolean isInteger, boolean parseAttr) {
        if (this.digitList.isZero()) {
            this.digitList.decimalAt = 0;
        }
        int prefixLen = appendAffix(result, isNegative, true, fieldPosition, parseAttr);
        if (this.useExponentialNotation) {
            subformatExponential(result, fieldPosition, parseAttr);
        } else {
            subformatFixed(result, fieldPosition, isInteger, parseAttr);
        }
        addPadding(result, fieldPosition, prefixLen, appendAffix(result, isNegative, false, fieldPosition, parseAttr));
        return result;
    }

    /* JADX WARNING: Removed duplicated region for block: B:115:0x021b  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x02ad  */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x02a5  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x02be  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02dc  */
    /* JADX WARNING: Removed duplicated region for block: B:155:0x02c6  */
    /* JADX WARNING: Missing block: B:108:0x020d, code skipped:
            if (r13 == r0.digitList.count) goto L_0x0212;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void subformatFixed(StringBuffer result, FieldPosition fieldPosition, boolean isInteger, boolean parseAttr) {
        int fractionalDigitsCount;
        int count;
        int i;
        String decimal;
        int count2;
        int count3;
        int fracBegin;
        long fractionalDigits;
        int fractionalDigitsCount2;
        StringBuffer stringBuffer = result;
        FieldPosition fieldPosition2 = fieldPosition;
        String[] digits = this.symbols.getDigitStrings();
        String grouping = this.currencySignCount == 0 ? this.symbols.getGroupingSeparatorString() : this.symbols.getMonetaryGroupingSeparatorString();
        String decimal2 = this.currencySignCount == 0 ? this.symbols.getDecimalSeparatorString() : this.symbols.getMonetaryDecimalSeparatorString();
        boolean useSigDig = areSignificantDigitsUsed();
        int maxIntDig = getMaximumIntegerDigits();
        int maxSigDig = getMinimumIntegerDigits();
        int intBegin = result.length();
        if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == Field.INTEGER) {
            fieldPosition2.setBeginIndex(intBegin);
        }
        boolean recordFractionDigits = false;
        int minSigDig = getMinimumSignificantDigits();
        int maxSigDig2 = getMaximumSignificantDigits();
        if (!useSigDig) {
            minSigDig = 0;
            maxSigDig2 = Integer.MAX_VALUE;
        }
        int minIntDig = maxSigDig;
        long fractionalDigits2 = 0;
        int minSigDig2 = minSigDig;
        maxSigDig = maxSigDig2;
        if (useSigDig) {
            fractionalDigitsCount = 0;
            count = Math.max(1, this.digitList.decimalAt);
        } else {
            fractionalDigitsCount = 0;
            count = minIntDig;
        }
        if (this.digitList.decimalAt > 0 && count < this.digitList.decimalAt) {
            count = this.digitList.decimalAt;
        }
        int digitIndex = 0;
        if (count <= maxIntDig || maxIntDig < 0) {
        } else {
            count = maxIntDig;
            digitIndex = this.digitList.decimalAt - count;
        }
        maxIntDig = result.length();
        minSigDig = count - 1;
        count = 0;
        while (true) {
            boolean recordFractionDigits2 = recordFractionDigits;
            i = minSigDig;
            if (i < 0) {
                break;
            }
            String grouping2;
            int digitIndex2;
            decimal = decimal2;
            if (i >= this.digitList.decimalAt || digitIndex >= this.digitList.count || count >= maxSigDig) {
                stringBuffer.append(digits[0]);
                if (count > 0) {
                    count++;
                }
            } else {
                int digitIndex3 = digitIndex + 1;
                stringBuffer.append(digits[this.digitList.getDigitValue(digitIndex)]);
                count++;
                digitIndex = digitIndex3;
            }
            if (isGroupingPosition(i)) {
                stringBuffer.append(grouping);
                grouping2 = grouping;
                if (fieldPosition.getFieldAttribute() == Field.GROUPING_SEPARATOR && fieldPosition.getBeginIndex() == 0 && fieldPosition.getEndIndex() == 0) {
                    fieldPosition2.setBeginIndex(result.length() - 1);
                    fieldPosition2.setEndIndex(result.length());
                }
                if (parseAttr) {
                    digitIndex2 = digitIndex;
                    addAttribute(Field.GROUPING_SEPARATOR, result.length() - 1, result.length());
                } else {
                    digitIndex2 = digitIndex;
                }
            } else {
                grouping2 = grouping;
                digitIndex2 = digitIndex;
            }
            minSigDig = i - 1;
            recordFractionDigits = recordFractionDigits2;
            decimal2 = decimal;
            grouping = grouping2;
            digitIndex = digitIndex2;
        }
        decimal = decimal2;
        if (fieldPosition.getField() == 0 || fieldPosition.getFieldAttribute() == Field.INTEGER) {
            fieldPosition2.setEndIndex(result.length());
        }
        if (count == 0 && this.digitList.count == 0) {
            count = 1;
        }
        boolean fractionPresent = (!isInteger && digitIndex < this.digitList.count) || (useSigDig ? count >= minSigDig2 : getMinimumFractionDigits() <= 0);
        if (!fractionPresent && result.length() == maxIntDig) {
            stringBuffer.append(digits[0]);
        }
        if (parseAttr) {
            addAttribute(Field.INTEGER, intBegin, result.length());
        }
        if (this.decimalSeparatorAlwaysShown || fractionPresent) {
            if (fieldPosition.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
                fieldPosition2.setBeginIndex(result.length());
            }
            decimal2 = decimal;
            stringBuffer.append(decimal2);
            if (fieldPosition.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
                fieldPosition2.setEndIndex(result.length());
            }
            if (parseAttr) {
                addAttribute(Field.DECIMAL_SEPARATOR, result.length() - 1, result.length());
            }
        } else {
            int i2 = intBegin;
            String str = decimal;
        }
        if (fieldPosition.getField() == 1) {
            fieldPosition2.setBeginIndex(result.length());
        } else if (fieldPosition.getFieldAttribute() == Field.FRACTION) {
            fieldPosition2.setBeginIndex(result.length());
        }
        int fracBegin2 = result.length();
        boolean recordFractionDigits3 = fieldPosition2 instanceof UFieldPosition;
        intBegin = useSigDig ? Integer.MAX_VALUE : getMaximumFractionDigits();
        if (useSigDig) {
            if (count == maxSigDig) {
            } else if (count >= minSigDig2) {
                count2 = intBegin;
            }
            intBegin = 0;
            i = 0;
            while (i < intBegin) {
                if (!useSigDig) {
                    count3 = intBegin;
                    if (i >= getMinimumFractionDigits() && (isInteger || digitIndex >= this.digitList.count)) {
                        fracBegin = fracBegin2;
                        break;
                    }
                }
                count3 = intBegin;
                fracBegin = fracBegin2;
                if (-1 - i <= this.digitList.decimalAt - 1) {
                    if (isInteger || digitIndex >= this.digitList.count) {
                        stringBuffer.append(digits[0]);
                        if (recordFractionDigits3) {
                            fractionalDigitsCount++;
                            fractionalDigits2 *= 10;
                        }
                    } else {
                        intBegin = digitIndex + 1;
                        fracBegin2 = this.digitList.getDigitValue(digitIndex);
                        stringBuffer.append(digits[fracBegin2]);
                        if (recordFractionDigits3) {
                            fractionalDigitsCount++;
                            fractionalDigits2 = (fractionalDigits2 * 10) + ((long) fracBegin2);
                        }
                        digitIndex = intBegin;
                    }
                    count++;
                    if (useSigDig) {
                        if (count != maxSigDig) {
                            if (digitIndex == this.digitList.count && count >= minSigDig2) {
                                break;
                            }
                        }
                        break;
                    }
                    continue;
                } else {
                    stringBuffer.append(digits[0]);
                    if (recordFractionDigits3) {
                        fractionalDigitsCount++;
                        fractionalDigits2 *= 10;
                    }
                }
                i++;
                intBegin = count3;
                fracBegin2 = fracBegin;
            }
            fracBegin = fracBegin2;
            count3 = intBegin;
            fractionalDigits = fractionalDigits2;
            fractionalDigitsCount2 = fractionalDigitsCount;
            if (fieldPosition.getField() != 1) {
                fieldPosition2.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == Field.FRACTION) {
                fieldPosition2.setEndIndex(result.length());
            }
            if (recordFractionDigits3) {
                ((UFieldPosition) fieldPosition2).setFractionDigits(fractionalDigitsCount2, fractionalDigits);
            }
            if (parseAttr) {
                return;
            } else if (this.decimalSeparatorAlwaysShown || fractionPresent) {
                addAttribute(Field.FRACTION, fracBegin, result.length());
                return;
            } else {
                int i3 = fracBegin;
                return;
            }
        }
        count2 = intBegin;
        intBegin = count2;
        i = 0;
        while (i < intBegin) {
        }
        fracBegin = fracBegin2;
        count3 = intBegin;
        fractionalDigits = fractionalDigits2;
        fractionalDigitsCount2 = fractionalDigitsCount;
        if (fieldPosition.getField() != 1) {
        }
        if (recordFractionDigits3) {
        }
        if (parseAttr) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:107:0x020b  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x01fd  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x025b  */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x0263  */
    /* JADX WARNING: Removed duplicated region for block: B:133:0x0281  */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x0299  */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x02a2  */
    /* JADX WARNING: Removed duplicated region for block: B:142:0x02c4  */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x02c2  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x02ca  */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x02c8  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x0311  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02ce  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x0374 A:{LOOP_END, LOOP:1: B:175:0x0372->B:176:0x0374} */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x0383  */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x03a2  */
    /* JADX WARNING: Removed duplicated region for block: B:198:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ae  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void subformatExponential(StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        int minFracDig;
        int minFracDig2;
        int exponent;
        StringBuffer stringBuffer = result;
        FieldPosition fieldPosition2 = fieldPosition;
        String[] digits = this.symbols.getDigitStringsLocal();
        String decimal = this.currencySignCount == 0 ? this.symbols.getDecimalSeparatorString() : this.symbols.getMonetaryDecimalSeparatorString();
        boolean useSigDig = areSignificantDigitsUsed();
        int maxIntDig = getMaximumIntegerDigits();
        int minIntDig = getMinimumIntegerDigits();
        if (fieldPosition.getField() == 0) {
            fieldPosition2.setBeginIndex(result.length());
            fieldPosition2.setEndIndex(-1);
        } else if (fieldPosition.getField() == 1) {
            fieldPosition2.setBeginIndex(-1);
        } else if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
            fieldPosition2.setBeginIndex(result.length());
            fieldPosition2.setEndIndex(-1);
        } else if (fieldPosition.getFieldAttribute() == Field.FRACTION) {
            fieldPosition2.setBeginIndex(-1);
        }
        int intBegin = result.length();
        int fracBegin = -1;
        if (useSigDig) {
            minIntDig = 1;
            maxIntDig = 1;
            minFracDig = getMinimumSignificantDigits() - 1;
        } else {
            minFracDig = getMinimumFractionDigits();
            if (maxIntDig > 8) {
                maxIntDig = 1;
                if (1 < minIntDig) {
                    maxIntDig = minIntDig;
                }
            }
            if (maxIntDig > minIntDig) {
                minIntDig = 1;
            }
        }
        boolean recordFractionDigits = false;
        int exponent2 = this.digitList.decimalAt;
        if (maxIntDig <= 1 || maxIntDig == minIntDig) {
            int i = (minIntDig > 0 || minFracDig > 0) ? minIntDig : 1;
            exponent2 -= i;
        } else {
            exponent2 = (exponent2 > 0 ? (exponent2 - 1) / maxIntDig : (exponent2 / maxIntDig) - 1) * maxIntDig;
        }
        int minimumDigits = minIntDig + minFracDig;
        int integerDigits = this.digitList.isZero() ? minIntDig : this.digitList.decimalAt - exponent2;
        maxIntDig = this.digitList.count;
        if (minimumDigits > maxIntDig) {
            maxIntDig = minimumDigits;
        }
        if (integerDigits > maxIntDig) {
            maxIntDig = integerDigits;
        }
        int exponent3 = exponent2;
        minIntDig = 0;
        long fractionalDigits = 0;
        int intEnd = -1;
        int i2 = 0;
        while (i2 < maxIntDig) {
            int integerDigits2;
            int fracBegin2;
            int intEnd2;
            if (i2 == integerDigits) {
                int fracBegin3;
                if (fieldPosition.getField() == 0) {
                    integerDigits2 = integerDigits;
                    fieldPosition2.setEndIndex(result.length());
                    minFracDig2 = minFracDig;
                } else {
                    integerDigits2 = integerDigits;
                    minFracDig2 = minFracDig;
                    if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
                        fieldPosition2.setEndIndex(result.length());
                    }
                }
                if (parseAttr) {
                    intEnd = result.length();
                    addAttribute(Field.INTEGER, intBegin, result.length());
                }
                if (fieldPosition.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
                    fieldPosition2.setBeginIndex(result.length());
                }
                stringBuffer.append(decimal);
                if (fieldPosition.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
                    fieldPosition2.setEndIndex(result.length());
                }
                integerDigits = result.length();
                if (parseAttr) {
                    fracBegin3 = integerDigits;
                    addAttribute(Field.DECIMAL_SEPARATOR, result.length() - 1, result.length());
                } else {
                    fracBegin3 = integerDigits;
                }
                if (fieldPosition.getField() == 1) {
                    fieldPosition2.setBeginIndex(result.length());
                } else if (fieldPosition.getFieldAttribute() == Field.FRACTION) {
                    fieldPosition2.setBeginIndex(result.length());
                }
                recordFractionDigits = fieldPosition2 instanceof UFieldPosition;
                fracBegin = fracBegin3;
            } else {
                integerDigits2 = integerDigits;
                minFracDig2 = minFracDig;
            }
            integerDigits = i2 < this.digitList.count ? this.digitList.getDigitValue(i2) : 0;
            stringBuffer.append(digits[integerDigits]);
            if (recordFractionDigits) {
                minIntDig++;
                fracBegin2 = fracBegin;
                intEnd2 = intEnd;
                fractionalDigits = (fractionalDigits * 10) + ((long) integerDigits);
            } else {
                fracBegin2 = fracBegin;
                intEnd2 = intEnd;
            }
            i2++;
            integerDigits = integerDigits2;
            minFracDig = minFracDig2;
            intEnd = intEnd2;
            fracBegin = fracBegin2;
        }
        minFracDig2 = minFracDig;
        if (this.digitList.isZero() && maxIntDig == 0) {
            stringBuffer.append(digits[0]);
        }
        if (fracBegin == -1 && this.decimalSeparatorAlwaysShown) {
            if (fieldPosition.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
                fieldPosition2.setBeginIndex(result.length());
            }
            stringBuffer.append(decimal);
            if (fieldPosition.getFieldAttribute() == Field.DECIMAL_SEPARATOR) {
                fieldPosition2.setEndIndex(result.length());
            }
            if (parseAttr) {
                addAttribute(Field.DECIMAL_SEPARATOR, result.length() - 1, result.length());
                if (fieldPosition.getField() != 0) {
                    if (fieldPosition.getEndIndex() < 0) {
                        fieldPosition2.setEndIndex(result.length());
                    }
                } else if (fieldPosition.getField() == 1) {
                    if (fieldPosition.getBeginIndex() < 0) {
                        fieldPosition2.setBeginIndex(result.length());
                    }
                    fieldPosition2.setEndIndex(result.length());
                } else if (fieldPosition.getFieldAttribute() == Field.INTEGER) {
                    if (fieldPosition.getEndIndex() < 0) {
                        fieldPosition2.setEndIndex(result.length());
                    }
                } else if (fieldPosition.getFieldAttribute() == Field.FRACTION) {
                    if (fieldPosition.getBeginIndex() < 0) {
                        fieldPosition2.setBeginIndex(result.length());
                    }
                    fieldPosition2.setEndIndex(result.length());
                }
                if (recordFractionDigits) {
                    ((UFieldPosition) fieldPosition2).setFractionDigits(minIntDig, fractionalDigits);
                }
                if (parseAttr) {
                    if (intEnd < 0) {
                        addAttribute(Field.INTEGER, intBegin, result.length());
                    }
                    if (fracBegin > 0) {
                        addAttribute(Field.FRACTION, fracBegin, result.length());
                    }
                }
                if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SYMBOL) {
                    fieldPosition2.setBeginIndex(result.length());
                }
                stringBuffer.append(this.symbols.getExponentSeparator());
                if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SYMBOL) {
                    fieldPosition2.setEndIndex(result.length());
                }
                if (parseAttr) {
                    addAttribute(Field.EXPONENT_SYMBOL, result.length() - this.symbols.getExponentSeparator().length(), result.length());
                }
                if (this.digitList.isZero()) {
                    exponent = exponent3;
                } else {
                    exponent = 0;
                }
                useSigDig = exponent >= 0;
                if (useSigDig) {
                    int exponent4;
                    if (this.exponentSignAlwaysShown) {
                        if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SIGN) {
                            fieldPosition2.setBeginIndex(result.length());
                        }
                        stringBuffer.append(this.symbols.getPlusSignString());
                        if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SIGN) {
                            fieldPosition2.setEndIndex(result.length());
                        }
                        if (parseAttr) {
                            exponent4 = exponent;
                            addAttribute(Field.EXPONENT_SIGN, result.length() - 1, result.length());
                            exponent = exponent4;
                        }
                    }
                    exponent4 = exponent;
                    exponent = exponent4;
                } else {
                    int exponent5 = -exponent;
                    if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SIGN) {
                        fieldPosition2.setBeginIndex(result.length());
                    }
                    stringBuffer.append(this.symbols.getMinusSignString());
                    if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SIGN) {
                        fieldPosition2.setEndIndex(result.length());
                    }
                    if (parseAttr) {
                        addAttribute(Field.EXPONENT_SIGN, result.length() - 1, result.length());
                    }
                    exponent = exponent5;
                }
                integerDigits = result.length();
                this.digitList.set((long) exponent);
                maxIntDig = this.minExponentDigits;
                if (this.useExponentialNotation && maxIntDig < 1) {
                    maxIntDig = 1;
                }
                for (minIntDig = this.digitList.decimalAt; minIntDig < maxIntDig; minIntDig++) {
                    stringBuffer.append(digits[0]);
                }
                for (maxIntDig = 0; maxIntDig < this.digitList.decimalAt; maxIntDig++) {
                    String str;
                    if (maxIntDig < this.digitList.count) {
                        str = digits[this.digitList.getDigitValue(maxIntDig)];
                    } else {
                        str = digits[0];
                    }
                    stringBuffer.append(str);
                }
                if (fieldPosition.getFieldAttribute() == Field.EXPONENT) {
                    fieldPosition2.setBeginIndex(integerDigits);
                    fieldPosition2.setEndIndex(result.length());
                }
                if (!parseAttr) {
                    addAttribute(Field.EXPONENT, integerDigits, result.length());
                    return;
                }
                return;
            }
        }
        if (fieldPosition.getField() != 0) {
        }
        if (recordFractionDigits) {
        }
        if (parseAttr) {
        }
        if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SYMBOL) {
        }
        stringBuffer.append(this.symbols.getExponentSeparator());
        if (fieldPosition.getFieldAttribute() == Field.EXPONENT_SYMBOL) {
        }
        if (parseAttr) {
        }
        if (this.digitList.isZero()) {
        }
        if (exponent >= 0) {
        }
        if (useSigDig) {
        }
        integerDigits = result.length();
        this.digitList.set((long) exponent);
        maxIntDig = this.minExponentDigits;
        maxIntDig = 1;
        while (minIntDig < maxIntDig) {
        }
        while (maxIntDig < this.digitList.decimalAt) {
        }
        if (fieldPosition.getFieldAttribute() == Field.EXPONENT) {
        }
        if (!parseAttr) {
        }
    }

    private final void addPadding(StringBuffer result, FieldPosition fieldPosition, int prefixLen, int suffixLen) {
        if (this.formatWidth > 0) {
            int len = this.formatWidth - result.length();
            if (len > 0) {
                char[] padding = new char[len];
                for (int i = 0; i < len; i++) {
                    padding[i] = this.pad;
                }
                switch (this.padPosition) {
                    case 0:
                        result.insert(0, padding);
                        break;
                    case 1:
                        result.insert(prefixLen, padding);
                        break;
                    case 2:
                        result.insert(result.length() - suffixLen, padding);
                        break;
                    case 3:
                        result.append(padding);
                        break;
                }
                if (this.padPosition == 0 || this.padPosition == 1) {
                    fieldPosition.setBeginIndex(fieldPosition.getBeginIndex() + len);
                    fieldPosition.setEndIndex(fieldPosition.getEndIndex() + len);
                }
            }
        }
    }

    public Number parse(String text, ParsePosition parsePosition) {
        return (Number) parse(text, parsePosition, null);
    }

    public CurrencyAmount parseCurrency(CharSequence text, ParsePosition pos) {
        return (CurrencyAmount) parse(text.toString(), pos, new Currency[1]);
    }

    private Object parse(String text, ParsePosition parsePosition, Currency[] currency) {
        String str = text;
        ParsePosition parsePosition2 = parsePosition;
        Currency[] currencyArr = currency;
        int i = parsePosition.getIndex();
        int backup = i;
        if (this.formatWidth > 0 && (this.padPosition == 0 || this.padPosition == 1)) {
            i = skipPadding(str, i);
        }
        if (str.regionMatches(i, this.symbols.getNaN(), 0, this.symbols.getNaN().length())) {
            i += this.symbols.getNaN().length();
            if (this.formatWidth > 0 && (this.padPosition == 2 || this.padPosition == 3)) {
                i = skipPadding(str, i);
            }
            parsePosition2.setIndex(i);
            return new Double(Double.NaN);
        }
        boolean[] status;
        int i2;
        int i3;
        Number n;
        int i4 = backup;
        boolean[] status2 = new boolean[3];
        int i5;
        if (this.currencySignCount != 0) {
            if (!parseForCurrency(str, parsePosition2, currencyArr, status2)) {
                return null;
            }
            status = status2;
            i2 = 2;
            i3 = 0;
            i5 = backup;
        } else if (currencyArr != null) {
            return null;
        } else {
            status = status2;
            i2 = 2;
            i3 = 0;
            i5 = 1;
            i5 = backup;
            if (!subparse(str, parsePosition2, this.digitList, status2, currencyArr, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, false, 0)) {
                parsePosition2.setIndex(i5);
                return null;
            }
        }
        if (status[i3]) {
            double d;
            if (status[1]) {
                d = Double.POSITIVE_INFINITY;
            } else {
                d = Double.NEGATIVE_INFINITY;
            }
            n = new Double(d);
        } else if (status[i2]) {
            n = status[1] ? new Double("0.0") : new Double("-0.0");
        } else if (status[1] || !this.digitList.isZero()) {
            int mult = this.multiplier;
            while (mult % 10 == 0) {
                DigitList_Android digitList_Android = this.digitList;
                digitList_Android.decimalAt--;
                mult /= 10;
            }
            Number big;
            if (this.parseBigDecimal || mult != 1 || !this.digitList.isIntegral()) {
                big = this.digitList.getBigDecimalICU(status[1]);
                n = big;
                if (mult != 1) {
                    n = big.divide(android.icu.math.BigDecimal.valueOf((long) mult), this.mathContext);
                }
            } else if (this.digitList.decimalAt < 12) {
                long l = 0;
                if (this.digitList.count > 0) {
                    long l2 = 0;
                    int nx = i3;
                    while (nx < this.digitList.count) {
                        l2 = ((10 * l2) + ((long) ((char) this.digitList.digits[nx]))) - 48;
                        nx++;
                    }
                    while (true) {
                        int nx2 = nx + 1;
                        if (nx >= this.digitList.decimalAt) {
                            break;
                        }
                        l2 *= 10;
                        nx = nx2;
                    }
                    if (status[1]) {
                        l = l2;
                    } else {
                        l = -l2;
                    }
                }
                n = Long.valueOf(l);
            } else {
                big = this.digitList.getBigInteger(status[1]);
                n = big.bitLength() < 64 ? Long.valueOf(big.longValue()) : big;
            }
        } else {
            n = new Double("-0.0");
        }
        return currencyArr != null ? new CurrencyAmount(n, currencyArr[i3]) : n;
    }

    private boolean parseForCurrency(String text, ParsePosition parsePosition, Currency[] currency, boolean[] status) {
        String str;
        String str2;
        DigitList_Android tmpDigitList;
        ParsePosition tmpPos;
        boolean[] tmpStatus;
        int maxPosIndex;
        int tmpDigitList2;
        boolean found;
        ParsePosition tmpPos2;
        DigitList_Android tmpDigitList3;
        boolean[] tmpStatus2;
        String str3;
        boolean found2;
        int origPos;
        ParsePosition tmpPos3;
        ParsePosition parsePosition2 = parsePosition;
        int origPos2 = parsePosition.getIndex();
        if (!this.isReadyForParsing) {
            int savedCurrencySignCount = this.currencySignCount;
            setupCurrencyAffixForAllPatterns();
            if (savedCurrencySignCount == 3) {
                applyPatternWithoutExpandAffix(this.formatPattern, false);
            } else {
                applyPattern(this.formatPattern, false);
            }
            this.isReadyForParsing = true;
        }
        int maxPosIndex2 = origPos2;
        int maxErrorPos = -1;
        boolean[] savedStatus = null;
        boolean[] tmpStatus3 = new boolean[3];
        ParsePosition tmpPos4 = new ParsePosition(origPos2);
        DigitList_Android tmpDigitList4 = new DigitList_Android();
        if (this.style == 6) {
            str = this.negPrefixPattern;
            str2 = this.negSuffixPattern;
            String str4 = this.posPrefixPattern;
            String str5 = str2;
            tmpDigitList = tmpDigitList4;
            tmpPos = tmpPos4;
            String str6 = str4;
            tmpStatus = tmpStatus3;
            maxPosIndex = maxPosIndex2;
            tmpDigitList2 = 3;
            found = subparse(text, tmpPos4, tmpDigitList4, tmpStatus3, currency, str, str5, str6, this.posSuffixPattern, true, 1);
        } else {
            tmpDigitList = tmpDigitList4;
            tmpPos = tmpPos4;
            tmpStatus = tmpStatus3;
            maxPosIndex = maxPosIndex2;
            tmpDigitList2 = 3;
            found = subparse(text, tmpPos, tmpDigitList, tmpStatus, currency, this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, true, 0);
        }
        if (found) {
            tmpPos2 = tmpPos;
            if (tmpPos2.getIndex() > maxPosIndex) {
                maxPosIndex2 = tmpPos2.getIndex();
                savedStatus = tmpStatus;
                this.digitList = tmpDigitList;
                maxPosIndex = maxPosIndex2;
            }
        } else {
            maxErrorPos = tmpPos.getErrorIndex();
        }
        Iterator it = this.affixPatternsForCurrency.iterator();
        maxPosIndex2 = maxPosIndex;
        int maxErrorPos2 = maxErrorPos;
        boolean found3 = found;
        while (it.hasNext()) {
            AffixForCurrency affix = (AffixForCurrency) it.next();
            boolean[] tmpStatus4 = new boolean[tmpDigitList2];
            ParsePosition tmpPos5 = new ParsePosition(origPos2);
            DigitList_Android tmpDigitList5 = new DigitList_Android();
            String negPrefix = affix.getNegPrefix();
            String negSuffix = affix.getNegSuffix();
            tmpDigitList3 = tmpDigitList5;
            ParsePosition tmpPos6 = tmpPos5;
            str = negPrefix;
            tmpStatus2 = tmpStatus4;
            str3 = negSuffix;
            found2 = found3;
            maxPosIndex = maxErrorPos2;
            origPos = origPos2;
            origPos2 = maxPosIndex2;
            Iterator it2 = it;
            if (subparse(text, tmpPos5, tmpDigitList5, tmpStatus4, currency, str, str3, affix.getPosPrefix(), affix.getPosSuffix(), true, affix.getPatternType())) {
                tmpPos3 = tmpPos6;
                if (tmpPos3.getIndex() > origPos2) {
                    int maxPosIndex3 = tmpPos3.getIndex();
                    boolean[] savedStatus2 = tmpStatus2;
                    this.digitList = tmpDigitList3;
                    maxPosIndex2 = maxPosIndex3;
                    savedStatus = savedStatus2;
                } else {
                    maxPosIndex2 = origPos2;
                }
                maxErrorPos2 = maxPosIndex;
                found3 = true;
            } else {
                tmpPos3 = tmpPos6;
                if (tmpPos3.getErrorIndex() > maxPosIndex) {
                    maxErrorPos2 = tmpPos3.getErrorIndex();
                } else {
                    maxErrorPos2 = maxPosIndex;
                }
                maxPosIndex2 = origPos2;
                found3 = found2;
            }
            tmpStatus = tmpStatus2;
            it = it2;
            origPos2 = origPos;
            tmpDigitList2 = 3;
        }
        origPos = origPos2;
        found2 = found3;
        maxPosIndex = maxErrorPos2;
        origPos2 = maxPosIndex2;
        boolean[] tmpStatus5 = new boolean[3];
        maxPosIndex2 = origPos;
        ParsePosition tmpPos7 = new ParsePosition(maxPosIndex2);
        DigitList_Android tmpDigitList6 = new DigitList_Android();
        str = this.negativePrefix;
        str3 = this.negativeSuffix;
        str2 = this.positivePrefix;
        tmpDigitList3 = tmpDigitList6;
        ParsePosition tmpPos8 = tmpPos7;
        tmpStatus2 = tmpStatus5;
        if (subparse(text, tmpPos7, tmpDigitList6, tmpStatus5, currency, str, str3, str2, this.positiveSuffix, false, null)) {
            tmpPos2 = tmpPos8;
            if (tmpPos2.getIndex() > origPos2) {
                maxPosIndex2 = tmpPos2.getIndex();
                savedStatus = tmpStatus2;
                this.digitList = tmpDigitList3;
            } else {
                maxPosIndex2 = origPos2;
            }
            found2 = true;
            maxErrorPos2 = maxPosIndex;
        } else {
            tmpPos2 = tmpPos8;
            if (tmpPos2.getErrorIndex() > maxPosIndex) {
                maxErrorPos2 = tmpPos2.getErrorIndex();
            } else {
                maxErrorPos2 = maxPosIndex;
            }
            maxPosIndex2 = origPos2;
        }
        if (found2) {
            tmpPos3 = parsePosition;
            tmpPos3.setIndex(maxPosIndex2);
            tmpPos3.setErrorIndex(-1);
            int index = 0;
            while (true) {
                int index2 = index;
                if (index2 >= 3) {
                    break;
                }
                status[index2] = savedStatus[index2];
                index = index2 + 1;
            }
        } else {
            parsePosition.setErrorIndex(maxErrorPos2);
        }
        return found2;
    }

    private void setupCurrencyAffixForAllPatterns() {
        if (this.currencyPluralInfo == null) {
            this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
        }
        this.affixPatternsForCurrency = new HashSet();
        String savedFormatPattern = this.formatPattern;
        applyPatternWithoutExpandAffix(NumberFormat.getPattern(this.symbols.getULocale(), 1), false);
        this.affixPatternsForCurrency.add(new AffixForCurrency(this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, 0));
        Iterator<String> iter = this.currencyPluralInfo.pluralPatternIterator();
        Set<String> currencyUnitPatternSet = new HashSet();
        while (iter.hasNext()) {
            String currencyPattern = this.currencyPluralInfo.getCurrencyPluralPattern((String) iter.next());
            if (!(currencyPattern == null || currencyUnitPatternSet.contains(currencyPattern))) {
                currencyUnitPatternSet.add(currencyPattern);
                applyPatternWithoutExpandAffix(currencyPattern, false);
                this.affixPatternsForCurrency.add(new AffixForCurrency(this.negPrefixPattern, this.negSuffixPattern, this.posPrefixPattern, this.posSuffixPattern, 1));
            }
        }
        this.formatPattern = savedFormatPattern;
    }

    /* JADX WARNING: Removed duplicated region for block: B:113:0x0236  */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x021a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x0389  */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x02f4  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x004c  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x008b  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0077  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x044f  */
    /* JADX WARNING: Removed duplicated region for block: B:214:0x0438  */
    /* JADX WARNING: Removed duplicated region for block: B:218:0x046b  */
    /* JADX WARNING: Removed duplicated region for block: B:217:0x0456  */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x0476  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x0474  */
    /* JADX WARNING: Removed duplicated region for block: B:227:0x047e  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x047c  */
    /* JADX WARNING: Removed duplicated region for block: B:230:0x0484  */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x0482  */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x048c  */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0488  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final boolean subparse(String text, ParsePosition parsePosition, DigitList_Android digits, boolean[] status, Currency[] currency, String negPrefix, String negSuffix, String posPrefix, String posSuffix, boolean parseComplexCurrency, int type) {
        int negMatch;
        boolean z;
        boolean z2;
        int posMatch;
        String str = text;
        ParsePosition parsePosition2 = parsePosition;
        DigitList_Android digitList_Android = digits;
        int position = parsePosition.getIndex();
        int oldStart = parsePosition.getIndex();
        if (this.formatWidth > 0 && this.padPosition == 0) {
            position = skipPadding(str, position);
        }
        int position2 = position;
        String str2 = str;
        int i = position2;
        boolean z3 = parseComplexCurrency;
        int i2 = type;
        int posMatch2 = compareAffix(str2, i, false, true, posPrefix, z3, i2, currency);
        position = compareAffix(str2, i, true, true, negPrefix, z3, i2, currency);
        if (posMatch2 >= 0 && position >= 0) {
            if (posMatch2 > position) {
                position = -1;
            } else if (position > posMatch2) {
                int i3;
                int gs2;
                ParsePosition parsePosition3;
                negMatch = position;
                posMatch2 = -1;
                if (posMatch2 < 0) {
                    position2 += posMatch2;
                } else if (negMatch >= 0) {
                    position2 += negMatch;
                } else {
                    z = false;
                    i3 = posMatch2;
                    parsePosition.setErrorIndex(position2);
                    return z;
                }
                if (this.formatWidth > 0 && this.padPosition == 1) {
                    position2 = skipPadding(str, position2);
                }
                status[0] = false;
                if (str.regionMatches(position2, this.symbols.getInfinity(), 0, this.symbols.getInfinity().length())) {
                    UnicodeSet unicodeSet;
                    int digitCount;
                    int gs22;
                    int lastGroup;
                    int backup;
                    int digitCount2;
                    String decimal;
                    UnicodeSet unicodeSet2;
                    digitList_Android.count = 0;
                    digitList_Android.decimalAt = 0;
                    String decimal2 = this.currencySignCount == 0 ? this.symbols.getDecimalSeparatorString() : this.symbols.getMonetaryDecimalSeparatorString();
                    str2 = this.currencySignCount == 0 ? this.symbols.getGroupingSeparatorString() : this.symbols.getMonetaryGroupingSeparatorString();
                    String exponentSep = this.symbols.getExponentSeparator();
                    long exponent = 0;
                    z3 = isParseStrict();
                    boolean strictFail = false;
                    gs2 = this.groupingSize2 == (byte) 0 ? this.groupingSize : this.groupingSize2;
                    if (skipExtendedSeparatorParsing) {
                        unicodeSet = UnicodeSet.EMPTY;
                    } else {
                        unicodeSet = getEquivalentDecimals(decimal2, z3);
                    }
                    UnicodeSet decimalEquiv = unicodeSet;
                    unicodeSet = skipExtendedSeparatorParsing ? UnicodeSet.EMPTY : z3 ? strictDefaultGroupingSeparators : defaultGroupingSeparators;
                    UnicodeSet groupEquiv = unicodeSet;
                    String decimal3 = decimal2;
                    int[] parsedDigit = new int[1];
                    boolean sawDigit = false;
                    parsedDigit[0] = -1;
                    String grouping = str2;
                    int lastGroup2 = -1;
                    i2 = 0;
                    int digitCount3 = 0;
                    boolean sawDecimal = false;
                    boolean sawGrouping = false;
                    int backup2 = -1;
                    String decimal4 = decimal3;
                    while (position2 < text.length()) {
                        int matchLen = matchesDigit(str, position2, parsedDigit);
                        if (matchLen <= 0) {
                            int[] parsedDigit2 = parsedDigit;
                            digitCount = digitCount3;
                            gs22 = gs2;
                            gs2 = decimal4.length();
                            if (str.regionMatches(position2, decimal4, null, gs2) == null) {
                                String decimal5;
                                if (isGroupingUsed() != null) {
                                    decimal5 = decimal4;
                                    parsedDigit = grouping;
                                    decimal4 = parsedDigit.length();
                                    if (str.regionMatches(position2, parsedDigit, 0, decimal4)) {
                                        if (!sawDecimal) {
                                            if (!z3 || (sawDigit && backup2 == -1)) {
                                                backup2 = position2;
                                                position2 += decimal4;
                                                sawGrouping = true;
                                                grouping = parsedDigit;
                                                digitCount3 = digitCount;
                                                parsedDigit = parsedDigit2;
                                                gs2 = gs22;
                                                decimal4 = decimal5;
                                            } else {
                                                strictFail = true;
                                            }
                                        }
                                        grouping = parsedDigit;
                                        lastGroup = lastGroup2;
                                        backup = backup2;
                                        digitCount2 = digitCount;
                                        decimal = decimal5;
                                        break;
                                    }
                                }
                                decimal5 = decimal4;
                                int i4 = gs2;
                                parsedDigit = grouping;
                                gs2 = str.codePointAt(position2);
                                String grouping2;
                                UnicodeSet decimalEquiv2;
                                int lastGroup3;
                                int[] iArr;
                                int[] parsedDigit3;
                                if (sawDecimal) {
                                    grouping2 = parsedDigit;
                                    decimalEquiv2 = decimalEquiv;
                                    if (isGroupingUsed() != null) {
                                    }
                                    lastGroup3 = lastGroup2;
                                    parsedDigit = groupEquiv;
                                    iArr = parsedDigit;
                                    parsedDigit3 = parsedDigit2;
                                    grouping = grouping2;
                                    lastGroup = lastGroup3;
                                    digitCount3 = decimalEquiv2;
                                    decimal = decimal5;
                                    backup = backup2;
                                    digitCount2 = digitCount;
                                    gs2 = exponentSep;
                                    if (str.regionMatches(true, position2, exponentSep, 0, exponentSep.length()) == null) {
                                    }
                                } else {
                                    decimalEquiv2 = decimalEquiv;
                                    if (decimalEquiv2.contains(gs2) != null) {
                                        if (z3) {
                                            grouping2 = parsedDigit;
                                            if (!(backup2 == -1 && (lastGroup2 == -1 || groupedDigitCount == this.groupingSize))) {
                                                strictFail = true;
                                            }
                                            if (isParseIntegerOnly() != null) {
                                                digitList_Android.decimalAt = digitCount;
                                                parsedDigit = String.valueOf(Character.toChars(gs2));
                                                sawDecimal = true;
                                                position2 += Character.charCount(gs2);
                                                decimalEquiv = decimalEquiv2;
                                                digitCount3 = digitCount;
                                                gs2 = gs22;
                                                grouping = grouping2;
                                                decimal4 = parsedDigit;
                                                parsedDigit = parsedDigit2;
                                            }
                                        } else {
                                            grouping2 = parsedDigit;
                                            if (isParseIntegerOnly() != null) {
                                            }
                                        }
                                        lastGroup = lastGroup2;
                                        backup = backup2;
                                        digitCount2 = digitCount;
                                        gs2 = exponentSep;
                                    } else {
                                        int[] iArr2;
                                        grouping2 = parsedDigit;
                                        if (isGroupingUsed() != null || sawGrouping) {
                                            lastGroup3 = lastGroup2;
                                            parsedDigit = groupEquiv;
                                        } else {
                                            parsedDigit = groupEquiv;
                                            UnicodeSet unicodeSet3;
                                            if (!parsedDigit.contains(gs2)) {
                                                lastGroup3 = lastGroup2;
                                            } else if (sawDecimal) {
                                                iArr = parsedDigit;
                                                lastGroup = lastGroup2;
                                                unicodeSet3 = decimalEquiv2;
                                                backup = backup2;
                                                digitCount2 = digitCount;
                                                gs2 = exponentSep;
                                            } else {
                                                if (z3) {
                                                    if (sawDigit) {
                                                        lastGroup3 = lastGroup2;
                                                        if (backup2 != -1) {
                                                        }
                                                    } else {
                                                        lastGroup3 = lastGroup2;
                                                    }
                                                    strictFail = true;
                                                    iArr = parsedDigit;
                                                    unicodeSet3 = decimalEquiv2;
                                                    backup = backup2;
                                                    digitCount2 = digitCount;
                                                    gs2 = exponentSep;
                                                    iArr2 = parsedDigit2;
                                                    decimal = decimal5;
                                                    grouping = grouping2;
                                                    lastGroup = lastGroup3;
                                                    break;
                                                }
                                                lastGroup3 = lastGroup2;
                                                grouping = String.valueOf(Character.toChars(gs2));
                                                backup2 = position2;
                                                position2 += Character.charCount(gs2);
                                                sawGrouping = true;
                                                groupEquiv = parsedDigit;
                                                decimalEquiv = decimalEquiv2;
                                                digitCount3 = digitCount;
                                                parsedDigit = parsedDigit2;
                                                gs2 = gs22;
                                                decimal4 = decimal5;
                                                lastGroup2 = lastGroup3;
                                            }
                                        }
                                        iArr = parsedDigit;
                                        parsedDigit3 = parsedDigit2;
                                        grouping = grouping2;
                                        lastGroup = lastGroup3;
                                        digitCount3 = decimalEquiv2;
                                        decimal = decimal5;
                                        backup = backup2;
                                        digitCount2 = digitCount;
                                        gs2 = exponentSep;
                                        if (str.regionMatches(true, position2, exponentSep, 0, exponentSep.length()) == null) {
                                            int[] parsedDigit4;
                                            parsedDigit = null;
                                            lastGroup2 = gs2.length() + position2;
                                            if (lastGroup2 < text.length()) {
                                                decimal4 = this.symbols.getPlusSignString();
                                                String minusSign = this.symbols.getMinusSignString();
                                                if (str.regionMatches(lastGroup2, decimal4, 0, decimal4.length())) {
                                                    lastGroup2 += decimal4.length();
                                                } else if (str.regionMatches(lastGroup2, minusSign, 0, minusSign.length())) {
                                                    lastGroup2 += minusSign.length();
                                                    parsedDigit = 1;
                                                }
                                            }
                                            decimal4 = new DigitList_Android();
                                            backup2 = 0;
                                            decimal4.count = 0;
                                            while (lastGroup2 < text.length()) {
                                                parsedDigit4 = parsedDigit3;
                                                int digitMatchLen = matchesDigit(str, lastGroup2, parsedDigit4);
                                                if (digitMatchLen <= 0) {
                                                    break;
                                                }
                                                decimal4.append((char) (parsedDigit4[backup2] + 48));
                                                lastGroup2 += digitMatchLen;
                                                parsedDigit3 = parsedDigit4;
                                                backup2 = 0;
                                            }
                                            parsedDigit4 = parsedDigit3;
                                            if (decimal4.count <= 0) {
                                            } else if (z3 && sawGrouping) {
                                                strictFail = true;
                                                iArr2 = parsedDigit4;
                                            } else {
                                                if (decimal4.count > 10) {
                                                    if (parsedDigit != null) {
                                                        status[2] = true;
                                                    } else {
                                                        status[null] = true;
                                                    }
                                                    iArr2 = parsedDigit4;
                                                } else {
                                                    decimal4.decimalAt = decimal4.count;
                                                    iArr2 = parsedDigit4;
                                                    backup2 = decimal4.getLong();
                                                    if (parsedDigit != null) {
                                                        backup2 = -backup2;
                                                    }
                                                    exponent = backup2;
                                                }
                                                position2 = lastGroup2;
                                            }
                                        }
                                    }
                                }
                                decimal = decimal5;
                                break;
                            }
                            if (z3 && (backup2 != -1 || (lastGroup2 != -1 && groupedDigitCount != this.groupingSize))) {
                                strictFail = true;
                            } else if (isParseIntegerOnly() == null && !sawDecimal) {
                                digitList_Android.decimalAt = digitCount;
                                sawDecimal = true;
                                position2 += gs2;
                                digitCount3 = digitCount;
                                parsedDigit = parsedDigit2;
                                gs2 = gs22;
                            }
                            lastGroup = lastGroup2;
                            decimal = decimal4;
                            backup = backup2;
                            digitCount2 = digitCount;
                            gs2 = exponentSep;
                            break;
                        }
                        if (backup2 != -1) {
                            if (z3 && ((lastGroup2 != -1 && groupedDigitCount != gs2) || (lastGroup2 == -1 && groupedDigitCount > gs2))) {
                                strictFail = true;
                                lastGroup = lastGroup2;
                                decimal = decimal4;
                                backup = backup2;
                                gs22 = gs2;
                                unicodeSet2 = groupEquiv;
                                digitCount2 = digitCount3;
                                gs2 = exponentSep;
                                break;
                            }
                            lastGroup2 = backup2;
                            i2 = 0;
                        }
                        i2++;
                        position2 += matchLen;
                        backup2 = -1;
                        sawDigit = true;
                        if (parsedDigit[0] != 0 || digitList_Android.count != 0) {
                            digitCount3++;
                            digitList_Android.append((char) (parsedDigit[0] + 48));
                        } else if (sawDecimal) {
                            digitList_Android.decimalAt--;
                        }
                    }
                    lastGroup = lastGroup2;
                    decimal = decimal4;
                    backup = backup2;
                    gs22 = gs2;
                    unicodeSet2 = groupEquiv;
                    digitCount2 = digitCount3;
                    if (digitList_Android.decimalAt == 0 && isDecimalPatternMatchRequired()) {
                        decimal4 = decimal;
                        lastGroup2 = -1;
                        if (this.formatPattern.indexOf(decimal4) != -1) {
                            parsePosition3 = parsePosition;
                            parsePosition3.setIndex(oldStart);
                            parsePosition3.setErrorIndex(position2);
                            return false;
                        }
                        parsePosition3 = parsePosition;
                    } else {
                        decimal4 = decimal;
                        lastGroup2 = -1;
                        parsePosition3 = parsePosition;
                    }
                    backup2 = backup;
                    if (backup2 != lastGroup2) {
                        position2 = backup2;
                    }
                    if (sawDecimal) {
                        digitCount = digitCount2;
                    } else {
                        digitCount = digitCount2;
                        digitList_Android.decimalAt = digitCount;
                    }
                    if (!z3 || sawDecimal) {
                        position = lastGroup;
                    } else {
                        position = lastGroup;
                        if (!(position == lastGroup2 || groupedDigitCount == this.groupingSize)) {
                            strictFail = true;
                        }
                    }
                    if (strictFail) {
                        parsePosition3.setIndex(oldStart);
                        parsePosition3.setErrorIndex(position2);
                        return false;
                    }
                    long exponent2 = exponent + ((long) digitList_Android.decimalAt);
                    if (exponent2 < ((long) (-getParseMaxDigits()))) {
                        z2 = true;
                        status[2] = true;
                    } else {
                        z2 = true;
                        if (exponent2 > ((long) getParseMaxDigits())) {
                            status[0] = true;
                        } else {
                            digitList_Android.decimalAt = (int) exponent2;
                        }
                    }
                    if (sawDigit || digitCount != 0) {
                        z = false;
                    } else {
                        parsePosition3.setIndex(oldStart);
                        parsePosition3.setErrorIndex(oldStart);
                        return false;
                    }
                }
                position2 += this.symbols.getInfinity().length();
                status[0] = true;
                z2 = true;
                z = false;
                parsePosition3 = parsePosition;
                if (this.formatWidth > 0 && this.padPosition == 2) {
                    position2 = skipPadding(str, position2);
                }
                if (posMatch2 < 0) {
                    parsePosition2 = parsePosition3;
                    gs2 = compareAffix(str, position2, false, false, posSuffix, parseComplexCurrency, type, currency);
                } else {
                    i3 = posMatch2;
                    parsePosition2 = parsePosition3;
                    gs2 = i3;
                }
                if (negMatch < 0) {
                    posMatch = gs2;
                    position = compareAffix(str, position2, true, false, negSuffix, parseComplexCurrency, type, currency);
                } else {
                    posMatch = gs2;
                    position = negMatch;
                }
                if (posMatch >= 0 && position >= 0) {
                    if (posMatch <= position) {
                        position = -1;
                    } else if (position > posMatch) {
                        posMatch = -1;
                    }
                }
                if ((posMatch < 0 ? z2 : z) != (position < 0 ? z2 : z)) {
                    parsePosition2.setErrorIndex(position2);
                    return z;
                }
                position2 += posMatch >= 0 ? posMatch : position;
                if (this.formatWidth > 0 && this.padPosition == 3) {
                    position2 = skipPadding(str, position2);
                }
                parsePosition2.setIndex(position2);
                status[z2] = posMatch >= 0 ? z2 : z;
                if (parsePosition.getIndex() != oldStart) {
                    return z2;
                }
                parsePosition2.setErrorIndex(position2);
                return z;
            }
        }
        negMatch = position;
        if (posMatch2 < 0) {
        }
        position2 = skipPadding(str, position2);
        status[0] = false;
        if (str.regionMatches(position2, this.symbols.getInfinity(), 0, this.symbols.getInfinity().length())) {
        }
        position2 = skipPadding(str, position2);
        if (posMatch2 < 0) {
        }
        if (negMatch < 0) {
        }
        if (posMatch <= position) {
        }
        if (posMatch < 0) {
        }
        if (position < 0) {
        }
        if ((posMatch < 0 ? z2 : z) != (position < 0 ? z2 : z)) {
        }
    }

    private int matchesDigit(String str, int start, int[] decVal) {
        int i;
        String[] localeDigits = this.symbols.getDigitStringsLocal();
        for (i = 0; i < 10; i++) {
            int digitStrLen = localeDigits[i].length();
            if (str.regionMatches(start, localeDigits[i], 0, digitStrLen)) {
                decVal[0] = i;
                return digitStrLen;
            }
        }
        i = str.codePointAt(start);
        decVal[0] = UCharacter.digit(i, 10);
        if (decVal[0] >= 0) {
            return Character.charCount(i);
        }
        return 0;
    }

    private UnicodeSet getEquivalentDecimals(String decimal, boolean strictParse) {
        UnicodeSet equivSet = UnicodeSet.EMPTY;
        if (strictParse) {
            if (strictDotEquivalents.contains((CharSequence) decimal)) {
                return strictDotEquivalents;
            }
            if (strictCommaEquivalents.contains((CharSequence) decimal)) {
                return strictCommaEquivalents;
            }
            return equivSet;
        } else if (dotEquivalents.contains((CharSequence) decimal)) {
            return dotEquivalents;
        } else {
            if (commaEquivalents.contains((CharSequence) decimal)) {
                return commaEquivalents;
            }
            return equivSet;
        }
    }

    private final int skipPadding(String text, int position) {
        while (position < text.length() && text.charAt(position) == this.pad) {
            position++;
        }
        return position;
    }

    private int compareAffix(String text, int pos, boolean isNegative, boolean isPrefix, String affixPat, boolean complexCurrencyParsing, int type, Currency[] currency) {
        if (currency != null || this.currencyChoice != null || (this.currencySignCount != 0 && complexCurrencyParsing)) {
            return compareComplexAffix(affixPat, text, pos, type, currency);
        }
        if (isPrefix) {
            return compareSimpleAffix(isNegative ? this.negativePrefix : this.positivePrefix, text, pos);
        }
        return compareSimpleAffix(isNegative ? this.negativeSuffix : this.positiveSuffix, text, pos);
    }

    private static boolean isBidiMark(int c) {
        return c == 8206 || c == 8207 || c == 1564;
    }

    private static String trimMarksFromAffix(String affix) {
        boolean hasBidiMark = false;
        int idx = 0;
        while (idx < affix.length()) {
            if (isBidiMark(affix.charAt(idx))) {
                hasBidiMark = true;
                break;
            }
            idx++;
        }
        if (!hasBidiMark) {
            return affix;
        }
        StringBuilder buf = new StringBuilder();
        buf.append(affix, 0, idx);
        while (true) {
            idx++;
            if (idx >= affix.length()) {
                return buf.toString();
            }
            char c = affix.charAt(idx);
            if (!isBidiMark(c)) {
                buf.append(c);
            }
        }
    }

    private static int compareSimpleAffix(String affix, String input, int pos) {
        int start = pos;
        String trimmedAffix = affix.length() > 1 ? trimMarksFromAffix(affix) : affix;
        int pos2 = pos;
        pos = 0;
        while (pos < trimmedAffix.length()) {
            int c = UTF16.charAt(trimmedAffix, pos);
            int len = UTF16.getCharCount(c);
            int len2;
            boolean literalMatch;
            int ic;
            if (PatternProps.isWhiteSpace(c)) {
                len2 = len;
                len = pos;
                literalMatch = false;
                while (pos2 < input.length()) {
                    ic = UTF16.charAt(input, pos2);
                    if (ic != c) {
                        if (!isBidiMark(ic)) {
                            break;
                        }
                        pos2++;
                    } else {
                        literalMatch = true;
                        len += len2;
                        pos2 += len2;
                        if (len == trimmedAffix.length()) {
                            break;
                        }
                        c = UTF16.charAt(trimmedAffix, len);
                        len2 = UTF16.getCharCount(c);
                        if (!PatternProps.isWhiteSpace(c)) {
                            break;
                        }
                    }
                }
                len = skipPatternWhiteSpace(trimmedAffix, len);
                ic = pos2;
                pos2 = skipUWhiteSpace(input, pos2);
                if (pos2 == ic && !literalMatch) {
                    return -1;
                }
                pos = skipUWhiteSpace(trimmedAffix, len);
            } else {
                len2 = pos;
                literalMatch = false;
                while (pos2 < input.length()) {
                    ic = UTF16.charAt(input, pos2);
                    if (literalMatch || !equalWithSignCompatibility(ic, c)) {
                        if (!isBidiMark(ic)) {
                            break;
                        }
                        pos2++;
                    } else {
                        len2 += len;
                        pos2 += len;
                        literalMatch = true;
                    }
                }
                if (!literalMatch) {
                    return -1;
                }
                pos = len2;
            }
        }
        return pos2 - start;
    }

    private static boolean equalWithSignCompatibility(int lhs, int rhs) {
        return lhs == rhs || ((minusSigns.contains(lhs) && minusSigns.contains(rhs)) || (plusSigns.contains(lhs) && plusSigns.contains(rhs)));
    }

    private static int skipPatternWhiteSpace(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!PatternProps.isWhiteSpace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    private static int skipUWhiteSpace(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!UCharacter.isUWhiteSpace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    private static int skipBidiMarks(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!isBidiMark(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    private int compareComplexAffix(String affixPat, String text, int pos, int type, Currency[] currency) {
        int i;
        String str = affixPat;
        String str2 = text;
        int start = pos;
        int i2 = 0;
        int pos2 = pos;
        char c = 0;
        while (c < affixPat.length() && pos2 >= 0) {
            char i3 = c + 1;
            int c2 = str.charAt(c);
            if (c2 == '\'') {
                while (true) {
                    int i4;
                    int j = str.indexOf(39, i4);
                    if (j == i4) {
                        pos2 = match(str2, pos2, 39);
                        i3 = j + 1;
                        break;
                    } else if (j > i4) {
                        pos2 = match(str2, pos2, str.substring(i4, j));
                        i3 = j + 1;
                        if (i3 >= affixPat.length() || str.charAt(i3) != '\'') {
                            break;
                        }
                        pos2 = match(str2, pos2, 39);
                        i4 = i3 + 1;
                    } else {
                        throw new RuntimeException();
                    }
                }
                c = i3;
            } else {
                String affix = null;
                if (c2 == PATTERN_PERCENT) {
                    i = type;
                    affix = this.symbols.getPercentString();
                } else if (c2 == PATTERN_PLUS_SIGN) {
                    i = type;
                    affix = this.symbols.getPlusSignString();
                } else if (c2 == PATTERN_MINUS_SIGN) {
                    i = type;
                    affix = this.symbols.getMinusSignString();
                } else if (c2 != CURRENCY_SIGN) {
                    if (c2 == PATTERN_PER_MILLE) {
                        affix = this.symbols.getPerMillString();
                    }
                    i = type;
                } else {
                    boolean z = true;
                    boolean intl = (i3 >= affixPat.length() || str.charAt(i3) != CURRENCY_SIGN) ? i2 : true;
                    if (intl) {
                        i3++;
                    }
                    if (i3 >= affixPat.length() || str.charAt(i3) != CURRENCY_SIGN) {
                        z = i2;
                    }
                    if (z) {
                        i3++;
                    }
                    ULocale uloc = getLocale(ULocale.VALID_LOCALE);
                    if (uloc == null) {
                        uloc = this.symbols.getLocale(ULocale.VALID_LOCALE);
                    }
                    ParsePosition ppos = new ParsePosition(pos2);
                    String iso = Currency.parse(uloc, str2, type, ppos);
                    if (iso != null) {
                        if (currency != null) {
                            currency[i2] = Currency.getInstance(iso);
                        } else if (iso.compareTo(getEffectiveCurrency().getCurrencyCode()) != 0) {
                            pos2 = -1;
                        }
                        pos2 = ppos.getIndex();
                    } else {
                        pos2 = -1;
                    }
                    c = i3;
                    i2 = 0;
                }
                if (affix != null) {
                    pos2 = match(str2, pos2, affix);
                    c = i3;
                    i2 = 0;
                } else {
                    pos2 = match(str2, pos2, c2);
                    if (PatternProps.isWhiteSpace(c2)) {
                        c = skipPatternWhiteSpace(str, i3);
                    } else {
                        c = i3;
                    }
                    i2 = 0;
                }
            }
        }
        i = type;
        return pos2 - start;
    }

    static final int match(String text, int pos, int ch) {
        if (pos < 0 || pos >= text.length()) {
            return -1;
        }
        pos = skipBidiMarks(text, pos);
        if (PatternProps.isWhiteSpace(ch)) {
            int s = pos;
            pos = skipPatternWhiteSpace(text, pos);
            if (pos == s) {
                return -1;
            }
            return pos;
        } else if (pos >= text.length() || UTF16.charAt(text, pos) != ch) {
            return -1;
        } else {
            return skipBidiMarks(text, UTF16.getCharCount(ch) + pos);
        }
    }

    static final int match(String text, int pos, String str) {
        int i = 0;
        while (i < str.length() && pos >= 0) {
            int ch = UTF16.charAt(str, i);
            i += UTF16.getCharCount(ch);
            if (!isBidiMark(ch)) {
                pos = match(text, pos, ch);
                if (PatternProps.isWhiteSpace(ch)) {
                    i = skipPatternWhiteSpace(str, i);
                }
            }
        }
        return pos;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        try {
            return (DecimalFormatSymbols) this.symbols.clone();
        } catch (Exception e) {
            return null;
        }
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        this.symbols = (DecimalFormatSymbols) newSymbols.clone();
        setCurrencyForSymbols();
        expandAffixes(null);
    }

    private void setCurrencyForSymbols() {
        DecimalFormatSymbols def = new DecimalFormatSymbols(this.symbols.getULocale());
        if (this.symbols.getCurrencySymbol().equals(def.getCurrencySymbol()) && this.symbols.getInternationalCurrencySymbol().equals(def.getInternationalCurrencySymbol())) {
            setCurrency(Currency.getInstance(this.symbols.getULocale()));
        } else {
            setCurrency(null);
        }
    }

    public String getPositivePrefix() {
        return this.positivePrefix;
    }

    public void setPositivePrefix(String newValue) {
        this.positivePrefix = newValue;
        this.posPrefixPattern = null;
    }

    public String getNegativePrefix() {
        return this.negativePrefix;
    }

    public void setNegativePrefix(String newValue) {
        this.negativePrefix = newValue;
        this.negPrefixPattern = null;
    }

    public String getPositiveSuffix() {
        return this.positiveSuffix;
    }

    public void setPositiveSuffix(String newValue) {
        this.positiveSuffix = newValue;
        this.posSuffixPattern = null;
    }

    public String getNegativeSuffix() {
        return this.negativeSuffix;
    }

    public void setNegativeSuffix(String newValue) {
        this.negativeSuffix = newValue;
        this.negSuffixPattern = null;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(int newValue) {
        if (newValue != 0) {
            this.multiplier = newValue;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad multiplier: ");
        stringBuilder.append(newValue);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public BigDecimal getRoundingIncrement() {
        if (this.roundingIncrementICU == null) {
            return null;
        }
        return this.roundingIncrementICU.toBigDecimal();
    }

    public void setRoundingIncrement(BigDecimal newValue) {
        if (newValue == null) {
            setRoundingIncrement((android.icu.math.BigDecimal) null);
        } else {
            setRoundingIncrement(new android.icu.math.BigDecimal(newValue));
        }
    }

    public void setRoundingIncrement(android.icu.math.BigDecimal newValue) {
        int i = newValue == null ? 0 : newValue.compareTo(android.icu.math.BigDecimal.ZERO);
        if (i >= 0) {
            if (i == 0) {
                setInternalRoundingIncrement(null);
            } else {
                setInternalRoundingIncrement(newValue);
            }
            resetActualRounding();
            return;
        }
        throw new IllegalArgumentException("Illegal rounding increment");
    }

    public void setRoundingIncrement(double newValue) {
        if (newValue >= 0.0d) {
            if (newValue == 0.0d) {
                setInternalRoundingIncrement((android.icu.math.BigDecimal) null);
            } else {
                setInternalRoundingIncrement(android.icu.math.BigDecimal.valueOf(newValue));
            }
            resetActualRounding();
            return;
        }
        throw new IllegalArgumentException("Illegal rounding increment");
    }

    public int getRoundingMode() {
        return this.roundingMode;
    }

    public void setRoundingMode(int roundingMode) {
        if (roundingMode < 0 || roundingMode > 7) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid rounding mode: ");
            stringBuilder.append(roundingMode);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.roundingMode = roundingMode;
        resetActualRounding();
    }

    public int getFormatWidth() {
        return this.formatWidth;
    }

    public void setFormatWidth(int width) {
        if (width >= 0) {
            this.formatWidth = width;
            return;
        }
        throw new IllegalArgumentException("Illegal format width");
    }

    public char getPadCharacter() {
        return this.pad;
    }

    public void setPadCharacter(char padChar) {
        this.pad = padChar;
    }

    public int getPadPosition() {
        return this.padPosition;
    }

    public void setPadPosition(int padPos) {
        if (padPos < 0 || padPos > 3) {
            throw new IllegalArgumentException("Illegal pad position");
        }
        this.padPosition = padPos;
    }

    public boolean isScientificNotation() {
        return this.useExponentialNotation;
    }

    public void setScientificNotation(boolean useScientific) {
        this.useExponentialNotation = useScientific;
    }

    public byte getMinimumExponentDigits() {
        return this.minExponentDigits;
    }

    public void setMinimumExponentDigits(byte minExpDig) {
        if (minExpDig >= (byte) 1) {
            this.minExponentDigits = minExpDig;
            return;
        }
        throw new IllegalArgumentException("Exponent digits must be >= 1");
    }

    public boolean isExponentSignAlwaysShown() {
        return this.exponentSignAlwaysShown;
    }

    public void setExponentSignAlwaysShown(boolean expSignAlways) {
        this.exponentSignAlwaysShown = expSignAlways;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int newValue) {
        this.groupingSize = (byte) newValue;
    }

    public int getSecondaryGroupingSize() {
        return this.groupingSize2;
    }

    public void setSecondaryGroupingSize(int newValue) {
        this.groupingSize2 = (byte) newValue;
    }

    public MathContext getMathContextICU() {
        return this.mathContext;
    }

    public java.math.MathContext getMathContext() {
        java.math.MathContext mathContext = null;
        try {
            if (this.mathContext != null) {
                mathContext = new java.math.MathContext(this.mathContext.getDigits(), RoundingMode.valueOf(this.mathContext.getRoundingMode()));
            }
            return mathContext;
        } catch (Exception e) {
            return null;
        }
    }

    public void setMathContextICU(MathContext newValue) {
        this.mathContext = newValue;
    }

    public void setMathContext(java.math.MathContext newValue) {
        this.mathContext = new MathContext(newValue.getPrecision(), 1, false, newValue.getRoundingMode().ordinal());
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.decimalSeparatorAlwaysShown;
    }

    public void setDecimalPatternMatchRequired(boolean value) {
        this.parseRequireDecimalPoint = value;
    }

    public boolean isDecimalPatternMatchRequired() {
        return this.parseRequireDecimalPoint;
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        this.decimalSeparatorAlwaysShown = newValue;
    }

    public CurrencyPluralInfo getCurrencyPluralInfo() {
        CurrencyPluralInfo currencyPluralInfo = null;
        try {
            if (this.currencyPluralInfo != null) {
                currencyPluralInfo = (CurrencyPluralInfo) this.currencyPluralInfo.clone();
            }
            return currencyPluralInfo;
        } catch (Exception e) {
            return null;
        }
    }

    public void setCurrencyPluralInfo(CurrencyPluralInfo newInfo) {
        this.currencyPluralInfo = (CurrencyPluralInfo) newInfo.clone();
        this.isReadyForParsing = false;
    }

    public Object clone() {
        try {
            DecimalFormat_ICU58_Android other = (DecimalFormat_ICU58_Android) super.clone();
            other.symbols = (DecimalFormatSymbols) this.symbols.clone();
            other.digitList = new DigitList_Android();
            if (this.currencyPluralInfo != null) {
                other.currencyPluralInfo = (CurrencyPluralInfo) this.currencyPluralInfo.clone();
            }
            other.attributes = new ArrayList();
            other.currencyUsage = this.currencyUsage;
            return other;
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null || !super.equals(obj)) {
            return false;
        }
        DecimalFormat_ICU58_Android other = (DecimalFormat_ICU58_Android) obj;
        if (this.currencySignCount == other.currencySignCount && ((this.style != 6 || (equals(this.posPrefixPattern, other.posPrefixPattern) && equals(this.posSuffixPattern, other.posSuffixPattern) && equals(this.negPrefixPattern, other.negPrefixPattern) && equals(this.negSuffixPattern, other.negSuffixPattern))) && this.multiplier == other.multiplier && this.groupingSize == other.groupingSize && this.groupingSize2 == other.groupingSize2 && this.decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown && this.useExponentialNotation == other.useExponentialNotation && ((!this.useExponentialNotation || this.minExponentDigits == other.minExponentDigits) && this.useSignificantDigits == other.useSignificantDigits && ((!this.useSignificantDigits || (this.minSignificantDigits == other.minSignificantDigits && this.maxSignificantDigits == other.maxSignificantDigits)) && this.symbols.equals(other.symbols) && Utility.objectEquals(this.currencyPluralInfo, other.currencyPluralInfo) && this.currencyUsage.equals(other.currencyUsage))))) {
            z = true;
        }
        return z;
    }

    private boolean equals(String pat1, String pat2) {
        boolean z = true;
        if (pat1 == null || pat2 == null) {
            if (!(pat1 == null && pat2 == null)) {
                z = false;
            }
            return z;
        } else if (pat1.equals(pat2)) {
            return true;
        } else {
            return unquote(pat1).equals(unquote(pat2));
        }
    }

    private String unquote(String pat) {
        StringBuilder buf = new StringBuilder(pat.length());
        char ch = 0;
        while (ch < pat.length()) {
            char i = ch + 1;
            ch = pat.charAt(ch);
            if (ch != '\'') {
                buf.append(ch);
            }
            ch = i;
        }
        return buf.toString();
    }

    public int hashCode() {
        return (super.hashCode() * 37) + this.positivePrefix.hashCode();
    }

    public String toPattern() {
        if (this.style == 6) {
            return this.formatPattern;
        }
        return toPattern(false);
    }

    public String toLocalizedPattern() {
        if (this.style == 6) {
            return this.formatPattern;
        }
        return toPattern(true);
    }

    private void expandAffixes(String pluralCount) {
        this.currencyChoice = null;
        StringBuffer buffer = new StringBuffer();
        if (this.posPrefixPattern != null) {
            expandAffix(this.posPrefixPattern, pluralCount, buffer);
            this.positivePrefix = buffer.toString();
        }
        if (this.posSuffixPattern != null) {
            expandAffix(this.posSuffixPattern, pluralCount, buffer);
            this.positiveSuffix = buffer.toString();
        }
        if (this.negPrefixPattern != null) {
            expandAffix(this.negPrefixPattern, pluralCount, buffer);
            this.negativePrefix = buffer.toString();
        }
        if (this.negSuffixPattern != null) {
            expandAffix(this.negSuffixPattern, pluralCount, buffer);
            this.negativeSuffix = buffer.toString();
        }
    }

    private void expandAffix(String pattern, String pluralCount, StringBuffer buffer) {
        buffer.setLength(0);
        char c = 0;
        while (c < pattern.length()) {
            char i = c + 1;
            c = pattern.charAt(c);
            if (c == '\'') {
                while (true) {
                    int i2;
                    int j = pattern.indexOf(39, i2);
                    if (j == i2) {
                        buffer.append('\'');
                        i = j + 1;
                        break;
                    } else if (j > i2) {
                        buffer.append(pattern.substring(i2, j));
                        i = j + 1;
                        if (i >= pattern.length() || pattern.charAt(i) != '\'') {
                            break;
                        }
                        buffer.append('\'');
                        i2 = i + 1;
                    } else {
                        throw new RuntimeException();
                    }
                }
                c = i;
            } else {
                if (c == PATTERN_PERCENT) {
                    buffer.append(this.symbols.getPercentString());
                } else if (c == PATTERN_MINUS_SIGN) {
                    buffer.append(this.symbols.getMinusSignString());
                } else if (c == CURRENCY_SIGN) {
                    String s;
                    boolean intl = i < pattern.length() && pattern.charAt(i) == CURRENCY_SIGN;
                    boolean plural = false;
                    if (intl) {
                        i++;
                        if (i < pattern.length() && pattern.charAt(i) == CURRENCY_SIGN) {
                            plural = true;
                            intl = false;
                            i++;
                        }
                    }
                    Currency currency = getCurrency();
                    if (currency == null) {
                        String internationalCurrencySymbol;
                        if (intl) {
                            internationalCurrencySymbol = this.symbols.getInternationalCurrencySymbol();
                        } else {
                            internationalCurrencySymbol = this.symbols.getCurrencySymbol();
                        }
                        s = internationalCurrencySymbol;
                    } else if (plural && pluralCount != null) {
                        s = currency.getName(this.symbols.getULocale(), 2, pluralCount, null);
                    } else if (intl) {
                        s = currency.getCurrencyCode();
                    } else {
                        s = currency.getName(this.symbols.getULocale(), 0, null);
                    }
                    buffer.append(s);
                } else if (c != PATTERN_PER_MILLE) {
                    buffer.append(c);
                } else {
                    buffer.append(this.symbols.getPerMillString());
                }
                c = i;
            }
        }
    }

    private int appendAffix(StringBuffer buf, boolean isNegative, boolean isPrefix, FieldPosition fieldPosition, boolean parseAttr) {
        StringBuffer stringBuffer = buf;
        FieldPosition fieldPosition2 = fieldPosition;
        String affixPat;
        if (this.currencyChoice != null) {
            if (isPrefix) {
                affixPat = isNegative ? this.negPrefixPattern : this.posPrefixPattern;
            } else {
                affixPat = isNegative ? this.negSuffixPattern : this.posSuffixPattern;
            }
            StringBuffer affixBuf = new StringBuffer();
            expandAffix(affixPat, null, affixBuf);
            stringBuffer.append(affixBuf);
            return affixBuf.length();
        }
        String pattern;
        if (isPrefix) {
            affixPat = isNegative ? this.negativePrefix : this.positivePrefix;
            pattern = isNegative ? this.negPrefixPattern : this.posPrefixPattern;
        } else {
            affixPat = isNegative ? this.negativeSuffix : this.positiveSuffix;
            pattern = isNegative ? this.negSuffixPattern : this.posSuffixPattern;
        }
        String affix = affixPat;
        String pattern2 = pattern;
        if (parseAttr) {
            int offset = affix.indexOf(this.symbols.getCurrencySymbol());
            if (offset > -1) {
                formatAffix2Attribute(isPrefix, Field.CURRENCY, stringBuffer, offset, this.symbols.getCurrencySymbol().length());
            }
            offset = affix.indexOf(this.symbols.getMinusSignString());
            if (offset > -1) {
                formatAffix2Attribute(isPrefix, Field.SIGN, stringBuffer, offset, this.symbols.getMinusSignString().length());
            }
            offset = affix.indexOf(this.symbols.getPercentString());
            if (offset > -1) {
                formatAffix2Attribute(isPrefix, Field.PERCENT, stringBuffer, offset, this.symbols.getPercentString().length());
            }
            offset = affix.indexOf(this.symbols.getPerMillString());
            if (offset > -1) {
                formatAffix2Attribute(isPrefix, Field.PERMILLE, stringBuffer, offset, this.symbols.getPerMillString().length());
            }
            offset = pattern2.indexOf("¤¤¤");
            if (offset > -1) {
                formatAffix2Attribute(isPrefix, Field.CURRENCY, stringBuffer, offset, affix.length() - offset);
            }
        }
        int firstPos;
        int startPos;
        int firstPos2;
        if (fieldPosition.getFieldAttribute() == Field.SIGN) {
            affixPat = isNegative ? this.symbols.getMinusSignString() : this.symbols.getPlusSignString();
            firstPos = affix.indexOf(affixPat);
            if (firstPos > -1) {
                startPos = stringBuffer.length() + firstPos;
                fieldPosition2.setBeginIndex(startPos);
                fieldPosition2.setEndIndex(affixPat.length() + startPos);
            }
        } else if (fieldPosition.getFieldAttribute() == Field.PERCENT) {
            firstPos2 = affix.indexOf(this.symbols.getPercentString());
            if (firstPos2 > -1) {
                firstPos = stringBuffer.length() + firstPos2;
                fieldPosition2.setBeginIndex(firstPos);
                fieldPosition2.setEndIndex(this.symbols.getPercentString().length() + firstPos);
            }
        } else if (fieldPosition.getFieldAttribute() == Field.PERMILLE) {
            firstPos2 = affix.indexOf(this.symbols.getPerMillString());
            if (firstPos2 > -1) {
                firstPos = stringBuffer.length() + firstPos2;
                fieldPosition2.setBeginIndex(firstPos);
                fieldPosition2.setEndIndex(this.symbols.getPerMillString().length() + firstPos);
            }
        } else if (fieldPosition.getFieldAttribute() == Field.CURRENCY) {
            int end;
            if (affix.indexOf(this.symbols.getCurrencySymbol()) > -1) {
                affixPat = this.symbols.getCurrencySymbol();
                startPos = stringBuffer.length() + affix.indexOf(affixPat);
                end = affixPat.length() + startPos;
                fieldPosition2.setBeginIndex(startPos);
                fieldPosition2.setEndIndex(end);
            } else if (affix.indexOf(this.symbols.getInternationalCurrencySymbol()) > -1) {
                affixPat = this.symbols.getInternationalCurrencySymbol();
                startPos = stringBuffer.length() + affix.indexOf(affixPat);
                end = affixPat.length() + startPos;
                fieldPosition2.setBeginIndex(startPos);
                fieldPosition2.setEndIndex(end);
            } else if (pattern2.indexOf("¤¤¤") > -1) {
                startPos = stringBuffer.length() + affix.length();
                fieldPosition2.setBeginIndex(stringBuffer.length() + pattern2.indexOf("¤¤¤"));
                fieldPosition2.setEndIndex(startPos);
            }
        }
        stringBuffer.append(affix);
        return affix.length();
    }

    private void formatAffix2Attribute(boolean isPrefix, Field fieldType, StringBuffer buf, int offset, int symbolSize) {
        int begin = offset;
        if (!isPrefix) {
            begin += buf.length();
        }
        addAttribute(fieldType, begin, begin + symbolSize);
    }

    private void addAttribute(Field field, int begin, int end) {
        FieldPosition pos = new FieldPosition(field);
        pos.setBeginIndex(begin);
        pos.setEndIndex(end);
        this.attributes.add(pos);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return formatToCharacterIterator(obj, NULL_UNIT);
    }

    AttributedCharacterIterator formatToCharacterIterator(Object obj, Unit unit) {
        if (obj instanceof Number) {
            Number number = (Number) obj;
            StringBuffer text = new StringBuffer();
            unit.writePrefix(text);
            this.attributes.clear();
            int i = 0;
            if (obj instanceof BigInteger) {
                format((BigInteger) number, text, new FieldPosition(0), true);
            } else if (obj instanceof BigDecimal) {
                format((BigDecimal) number, text, new FieldPosition(0), true);
            } else if (obj instanceof Double) {
                format(number.doubleValue(), text, new FieldPosition(0), true);
            } else if ((obj instanceof Integer) || (obj instanceof Long)) {
                format(number.longValue(), text, new FieldPosition(0), true);
            } else {
                throw new IllegalArgumentException();
            }
            unit.writeSuffix(text);
            AttributedString as = new AttributedString(text.toString());
            while (true) {
                int i2 = i;
                if (i2 >= this.attributes.size()) {
                    return as.getIterator();
                }
                FieldPosition pos = (FieldPosition) this.attributes.get(i2);
                Format.Field attribute = pos.getFieldAttribute();
                as.addAttribute(attribute, attribute, pos.getBeginIndex(), pos.getEndIndex());
                i = i2 + 1;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void appendAffixPattern(StringBuffer buffer, boolean isNegative, boolean isPrefix, boolean localized) {
        String affixPat;
        if (isPrefix) {
            affixPat = isNegative ? this.negPrefixPattern : this.posPrefixPattern;
        } else {
            affixPat = isNegative ? this.negSuffixPattern : this.posSuffixPattern;
        }
        int i = 0;
        if (affixPat == null) {
            String affix;
            if (isPrefix) {
                affix = isNegative ? this.negativePrefix : this.positivePrefix;
            } else {
                affix = isNegative ? this.negativeSuffix : this.positiveSuffix;
            }
            buffer.append('\'');
            while (i < affix.length()) {
                char ch = affix.charAt(i);
                if (ch == '\'') {
                    buffer.append(ch);
                }
                buffer.append(ch);
                i++;
            }
            buffer.append('\'');
            return;
        }
        if (localized) {
            while (i < affixPat.length()) {
                char ch2 = affixPat.charAt(i);
                if (ch2 == PATTERN_PERCENT) {
                    ch2 = this.symbols.getPercent();
                } else if (ch2 == '\'') {
                    int j = affixPat.indexOf(39, i + 1);
                    if (j >= 0) {
                        buffer.append(affixPat.substring(i, j + 1));
                        i = j;
                        i++;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Malformed affix pattern: ");
                        stringBuilder.append(affixPat);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (ch2 == PATTERN_MINUS_SIGN) {
                    ch2 = this.symbols.getMinusSign();
                } else if (ch2 == PATTERN_PER_MILLE) {
                    ch2 = this.symbols.getPerMill();
                }
                if (ch2 == this.symbols.getDecimalSeparator() || ch2 == this.symbols.getGroupingSeparator()) {
                    buffer.append('\'');
                    buffer.append(ch2);
                    buffer.append('\'');
                    i++;
                } else {
                    buffer.append(ch2);
                    i++;
                }
            }
        } else {
            buffer.append(affixPat);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:90:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x014d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String toPattern(boolean localized) {
        String padSpec;
        int i;
        char sigDigit;
        boolean z = localized;
        StringBuffer result = new StringBuffer();
        char zero = z ? this.symbols.getZeroDigit() : PATTERN_ZERO_DIGIT;
        char digit = z ? this.symbols.getDigit() : PATTERN_DIGIT;
        char sigDigit2 = 0;
        boolean useSigDig = areSignificantDigitsUsed();
        if (useSigDig) {
            sigDigit2 = z ? this.symbols.getSignificantDigit() : PATTERN_SIGNIFICANT_DIGIT;
        }
        char group = z ? this.symbols.getGroupingSeparator() : PATTERN_GROUPING_SEPARATOR;
        int roundingDecimalPos = 0;
        String roundingDigits = null;
        char padPos = this.formatWidth > 0 ? this.padPosition : 65535;
        int i2 = 2;
        if (this.formatWidth > 0) {
            char padEscape;
            StringBuffer stringBuffer = new StringBuffer(2);
            if (z) {
                padEscape = this.symbols.getPadEscape();
            } else {
                padEscape = PATTERN_PAD_ESCAPE;
            }
            stringBuffer.append(padEscape);
            stringBuffer.append(this.pad);
            padSpec = stringBuffer.toString();
        } else {
            padSpec = null;
        }
        if (this.roundingIncrementICU != null) {
            i = this.roundingIncrementICU.scale();
            roundingDigits = this.roundingIncrementICU.movePointRight(i).toString();
            roundingDecimalPos = roundingDigits.length() - i;
        }
        boolean z2 = false;
        int part = 0;
        while (part < i2) {
            int maxDig;
            int g;
            char c;
            int maxDig2;
            char zero2;
            boolean i3;
            if (padPos == 0) {
                result.append(padSpec);
            }
            appendAffixPattern(result, part != 0 ? true : z2, true, z);
            if (padPos == 1) {
                result.append(padSpec);
            }
            int sub0Start = result.length();
            i2 = isGroupingUsed() ? Math.max(z2, this.groupingSize) : z2;
            if (i2 <= 0 || this.groupingSize2 <= (byte) 0) {
                sigDigit = sigDigit2;
            } else {
                sigDigit = sigDigit2;
                if (this.groupingSize2 != this.groupingSize) {
                    i2 += this.groupingSize2;
                }
            }
            int maxSigDig = 0;
            if (useSigDig) {
                i = getMinimumSignificantDigits();
                int maximumSignificantDigits = getMaximumSignificantDigits();
                maxSigDig = maximumSignificantDigits;
                maxDig = maximumSignificantDigits;
            } else {
                i = getMinimumIntegerDigits();
                maxDig = getMaximumIntegerDigits();
            }
            char padPos2 = padPos;
            int maxSigDig2 = maxSigDig;
            char digit2 = digit;
            if (this.useExponentialNotation != 0) {
                if (maxDig > 8) {
                    maxDig = 1;
                }
            } else if (useSigDig) {
                maxDig = Math.max(maxDig, i2 + 1);
            } else {
                maxDig = Math.max(Math.max(i2, getMinimumIntegerDigits()), roundingDecimalPos) + 1;
            }
            int i4 = maxDig;
            while (i4 > 0) {
                g = i2;
                if (this.useExponentialNotation == 0 && i4 < maxDig && isGroupingPosition(i4)) {
                    result.append(group);
                }
                if (useSigDig) {
                    c = (maxSigDig2 < i4 || i4 <= maxSigDig2 - i) ? digit2 : sigDigit;
                    result.append(c);
                    maxDig2 = maxDig;
                } else {
                    if (roundingDigits != null) {
                        i2 = roundingDecimalPos - i4;
                        if (i2 >= 0) {
                            maxDig2 = maxDig;
                            if (i2 < roundingDigits.length()) {
                                result.append((char) ((roundingDigits.charAt(i2) - 48) + zero));
                            }
                            result.append(i4 > i ? zero : digit2);
                        }
                    }
                    maxDig2 = maxDig;
                    if (i4 > i) {
                    }
                    result.append(i4 > i ? zero : digit2);
                }
                i4--;
                i2 = g;
                maxDig = maxDig2;
            }
            maxDig2 = maxDig;
            g = i2;
            if (!useSigDig) {
                if (getMaximumFractionDigits() > 0 || this.decimalSeparatorAlwaysShown) {
                    if (z) {
                        sigDigit2 = this.symbols.getDecimalSeparator();
                    } else {
                        sigDigit2 = PATTERN_DECIMAL_SEPARATOR;
                    }
                    result.append(sigDigit2);
                }
                maxDig = roundingDecimalPos;
                i4 = 0;
                while (i4 < getMaximumFractionDigits()) {
                    if (roundingDigits == null || maxDig >= roundingDigits.length()) {
                        result.append(i4 < getMinimumFractionDigits() ? zero : digit2);
                    } else {
                        if (maxDig < 0) {
                            c = zero;
                        } else {
                            c = (char) ((roundingDigits.charAt(maxDig) - 48) + zero);
                        }
                        result.append(c);
                        maxDig++;
                    }
                    i4++;
                }
            }
            if (this.useExponentialNotation) {
                if (z) {
                    result.append(this.symbols.getExponentSeparator());
                } else {
                    result.append(PATTERN_EXPONENT);
                }
                if (this.exponentSignAlwaysShown) {
                    result.append(z ? this.symbols.getPlusSign() : PATTERN_PLUS_SIGN);
                }
                i4 = 0;
                while (i4 < this.minExponentDigits) {
                    result.append(zero);
                    i4++;
                }
            }
            int i5;
            if (padSpec == null || this.useExponentialNotation) {
                zero2 = zero;
                i5 = i4;
                sigDigit2 = digit2;
                i3 = true;
            } else {
                maxDig = (this.formatWidth - result.length()) + sub0Start;
                if (part == 0) {
                    zero2 = zero;
                    i2 = this.positivePrefix.length() + this.positiveSuffix.length();
                } else {
                    zero2 = zero;
                    i2 = this.negativeSuffix.length() + this.negativePrefix.length();
                }
                sigDigit2 = maxDig - i2;
                while (true) {
                    zero = sigDigit2;
                    if (zero <= 0) {
                        break;
                    }
                    sigDigit2 = digit2;
                    result.insert(sub0Start, sigDigit2);
                    i2 = maxDig2 + 1;
                    zero--;
                    i5 = i4;
                    if (zero > 1 && isGroupingPosition(i2)) {
                        result.insert(sub0Start, group);
                        zero--;
                    }
                    digit2 = sigDigit2;
                    maxDig2 = i2;
                    i4 = i5;
                    sigDigit2 = zero;
                }
                i5 = i4;
                sigDigit2 = digit2;
                i3 = true;
            }
            zero = padPos2;
            if (zero == 2) {
                result.append(padSpec);
            }
            if (part == 0) {
                i3 = false;
            }
            appendAffixPattern(result, i3, false, z);
            if (zero == 3) {
                result.append(padSpec);
            }
            if (part == 0) {
                if (this.negativeSuffix.equals(this.positiveSuffix)) {
                    String str = this.negativePrefix;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(45);
                    stringBuilder.append(this.positivePrefix);
                    if (str.equals(stringBuilder.toString())) {
                        break;
                    }
                }
                result.append(z ? this.symbols.getPatternSeparator() : PATTERN_SEPARATOR);
            }
            part++;
            padPos = zero;
            digit = sigDigit2;
            sigDigit2 = sigDigit;
            zero = zero2;
            i2 = 2;
            z2 = false;
        }
        sigDigit = sigDigit2;
        return result.toString();
    }

    public void applyPattern(String pattern) {
        applyPattern(pattern, false);
    }

    public void applyLocalizedPattern(String pattern) {
        applyPattern(pattern, true);
    }

    private void applyPattern(String pattern, boolean localized) {
        applyPatternWithoutExpandAffix(pattern, localized);
        expandAffixAdjustWidth(null);
    }

    private void expandAffixAdjustWidth(String pluralCount) {
        expandAffixes(pluralCount);
        if (this.formatWidth > 0) {
            this.formatWidth += this.positivePrefix.length() + this.positiveSuffix.length();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:125:0x0392  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0379  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x060d  */
    /* JADX WARNING: Removed duplicated region for block: B:255:0x05db  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x0721  */
    /* JADX WARNING: Removed duplicated region for block: B:270:0x0619  */
    /* JADX WARNING: Removed duplicated region for block: B:255:0x05db  */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x060d  */
    /* JADX WARNING: Removed duplicated region for block: B:270:0x0619  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x0721  */
    /* JADX WARNING: Missing block: B:240:0x05ad, code skipped:
            if (r12 <= (r2 + r27)) goto L_0x05b9;
     */
    /* JADX WARNING: Missing block: B:250:0x05ca, code skipped:
            if (r14 > 2) goto L_0x05d4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void applyPatternWithoutExpandAffix(String pattern, boolean localized) {
        char plus;
        String exponent;
        char c;
        char c2;
        char c3;
        char c4;
        char c5;
        char c6;
        char c7;
        char c8;
        char c9;
        char c10;
        String str;
        String str2 = pattern;
        char zeroDigit = PATTERN_ZERO_DIGIT;
        char sigDigit = PATTERN_SIGNIFICANT_DIGIT;
        char groupingSeparator = PATTERN_GROUPING_SEPARATOR;
        char decimalSeparator = PATTERN_DECIMAL_SEPARATOR;
        char percent = PATTERN_PERCENT;
        char perMill = PATTERN_PER_MILLE;
        char digit = PATTERN_DIGIT;
        char separator = PATTERN_SEPARATOR;
        String exponent2 = String.valueOf(PATTERN_EXPONENT);
        char plus2 = PATTERN_PLUS_SIGN;
        char padEscape = PATTERN_PAD_ESCAPE;
        char minus = PATTERN_MINUS_SIGN;
        if (localized) {
            zeroDigit = this.symbols.getZeroDigit();
            sigDigit = this.symbols.getSignificantDigit();
            groupingSeparator = this.symbols.getGroupingSeparator();
            decimalSeparator = this.symbols.getDecimalSeparator();
            percent = this.symbols.getPercent();
            perMill = this.symbols.getPerMill();
            digit = this.symbols.getDigit();
            separator = this.symbols.getPatternSeparator();
            exponent2 = this.symbols.getExponentSeparator();
            plus2 = this.symbols.getPlusSign();
            padEscape = this.symbols.getPadEscape();
            minus = this.symbols.getMinusSign();
        }
        char nineDigit = (char) (zeroDigit + 9);
        int pos = 0;
        boolean gotNegative = false;
        int part = 0;
        while (true) {
            plus = plus2;
            exponent = exponent2;
            int part2 = part;
            if (part2 >= 2 || pos >= pattern.length()) {
                c = sigDigit;
                c2 = groupingSeparator;
                c3 = decimalSeparator;
                c4 = percent;
                c5 = perMill;
                c6 = digit;
                c7 = separator;
                c8 = padEscape;
                c9 = minus;
                c10 = plus;
                str = exponent;
            } else {
                int start;
                int part3;
                int multpl;
                String exponent3;
                byte groupingCount;
                int multpl2;
                char zeroDigit2;
                boolean sigDigitCount;
                int padPos;
                byte expDigits;
                byte groupingCount2;
                int pos2;
                int sub0Limit = 0;
                int sub2Limit = 0;
                char subpart = 1;
                StringBuilder prefix = new StringBuilder();
                int start2 = pos;
                char digitLeftCount = 0;
                int zeroDigitCount = 0;
                int digitRightCount = 0;
                int incrementPos = -1;
                StringBuilder affix = prefix;
                StringBuilder prefix2 = prefix;
                char padEscape2 = padEscape;
                StringBuilder suffix = new StringBuilder();
                int sub0Start = 0;
                char subpart2 = subpart;
                int pos3 = start2;
                char decimalPos = 65535;
                int multpl3 = 1;
                boolean sigDigitCount2 = false;
                byte groupingCount3 = (byte) -1;
                byte groupingCount22 = (byte) -1;
                int padPos2 = -1;
                char padChar = 0;
                long incrementVal = 0;
                byte expDigits2 = (byte) -1;
                boolean expSignAlways = false;
                char currencySignCnt = 0;
                StringBuilder affix2 = affix;
                while (true) {
                    start = start2;
                    char minus2 = minus;
                    char perMill2 = perMill;
                    StringBuilder affix3;
                    if (pos3 < pattern.length()) {
                        minus = str2.charAt(pos3);
                        boolean sigDigitCount3;
                        int padPos3;
                        char percent2;
                        switch (subpart2) {
                            case 0:
                                c4 = percent;
                                c7 = separator;
                                part3 = part2;
                                c8 = padEscape2;
                                multpl = multpl3;
                                perMill = padPos2;
                                c9 = minus2;
                                c5 = perMill2;
                                if (minus == digit) {
                                    if (zeroDigitCount <= 0) {
                                        exponent3 = sigDigitCount2;
                                        if (exponent3 <= null) {
                                            digitLeftCount++;
                                            groupingCount = groupingCount3;
                                            if (groupingCount < (byte) 0) {
                                                affix3 = affix2;
                                                affix2 = decimalPos;
                                                if (affix2 < null) {
                                                    multpl2 = multpl;
                                                    zeroDigit2 = zeroDigit;
                                                    c = sigDigit;
                                                    sigDigitCount2 = exponent3;
                                                    padPos2 = perMill;
                                                    groupingCount3 = (byte) (groupingCount + 1);
                                                    decimalPos = affix2;
                                                    sigDigit = plus;
                                                    exponent3 = exponent;
                                                    affix2 = affix3;
                                                    multpl3 = multpl2;
                                                    break;
                                                }
                                                multpl2 = multpl;
                                            } else {
                                                multpl2 = multpl;
                                                affix3 = affix2;
                                                affix2 = decimalPos;
                                            }
                                            zeroDigit2 = zeroDigit;
                                            c = sigDigit;
                                            sigDigitCount2 = exponent3;
                                            padPos2 = perMill;
                                            groupingCount3 = groupingCount;
                                            decimalPos = affix2;
                                            sigDigit = plus;
                                            exponent3 = exponent;
                                            affix2 = affix3;
                                            multpl3 = multpl2;
                                        }
                                    } else {
                                        exponent3 = sigDigitCount2;
                                    }
                                    digitRightCount++;
                                    groupingCount = groupingCount3;
                                    if (groupingCount < (byte) 0) {
                                    }
                                    zeroDigit2 = zeroDigit;
                                    c = sigDigit;
                                    sigDigitCount2 = exponent3;
                                    padPos2 = perMill;
                                    groupingCount3 = groupingCount;
                                    decimalPos = affix2;
                                    sigDigit = plus;
                                    exponent3 = exponent;
                                    affix2 = affix3;
                                    multpl3 = multpl2;
                                } else {
                                    multpl2 = multpl;
                                    affix3 = affix2;
                                    sigDigitCount3 = sigDigitCount2;
                                    groupingCount = groupingCount3;
                                    padEscape = decimalPos;
                                    StringBuilder affix4;
                                    if ((minus < zeroDigit || minus > nineDigit) && minus != sigDigit) {
                                        sigDigitCount = sigDigitCount3;
                                        char padPos4 = perMill;
                                        if (minus == groupingSeparator) {
                                            if (minus == '\'' && pos3 + 1 < pattern.length()) {
                                                percent = str2.charAt(pos3 + 1);
                                                if (percent != digit && (percent < zeroDigit || percent > nineDigit)) {
                                                    if (percent == '\'') {
                                                        pos3++;
                                                    } else if (groupingCount < (byte) 0) {
                                                        zeroDigit2 = zeroDigit;
                                                        c = sigDigit;
                                                        subpart2 = 3;
                                                    } else {
                                                        affix4 = suffix;
                                                        zeroDigit2 = zeroDigit;
                                                        c = sigDigit;
                                                        groupingCount3 = groupingCount;
                                                        sub0Limit = pos3;
                                                        decimalPos = padEscape;
                                                        pos3--;
                                                        sigDigit = plus;
                                                        exponent3 = exponent;
                                                        multpl3 = multpl2;
                                                        padPos2 = padPos4;
                                                        sigDigitCount2 = sigDigitCount;
                                                        subpart2 = 2;
                                                    }
                                                }
                                            }
                                            if (padEscape >= 0) {
                                                patternError("Grouping separator after decimal", str2);
                                            }
                                            groupingCount22 = groupingCount;
                                            groupingCount3 = (byte) 0;
                                            zeroDigit2 = zeroDigit;
                                            c = sigDigit;
                                            decimalPos = padEscape;
                                            sigDigit = plus;
                                            exponent3 = exponent;
                                            affix2 = affix3;
                                            multpl3 = multpl2;
                                            padPos2 = padPos4;
                                            sigDigitCount2 = sigDigitCount;
                                            break;
                                        } else if (minus == decimalSeparator) {
                                            if (padEscape >= 0) {
                                                patternError("Multiple decimal separators", str2);
                                            }
                                            zeroDigit2 = zeroDigit;
                                            c = sigDigit;
                                            decimalPos = (digitLeftCount + zeroDigitCount) + digitRightCount;
                                            groupingCount3 = groupingCount;
                                            sigDigit = plus;
                                            exponent3 = exponent;
                                            affix2 = affix3;
                                            multpl3 = multpl2;
                                            padPos2 = padPos4;
                                            sigDigitCount2 = sigDigitCount;
                                        } else {
                                            exponent3 = exponent;
                                            if (str2.regionMatches(pos3, exponent3, 0, exponent3.length())) {
                                                if (expDigits2 >= (byte) 0) {
                                                    patternError("Multiple exponential symbols", str2);
                                                }
                                                if (groupingCount >= (byte) 0) {
                                                    patternError("Grouping separator in exponential", str2);
                                                }
                                                pos3 += exponent3.length();
                                                if (pos3 < pattern.length()) {
                                                    c = sigDigit;
                                                    sigDigit = plus;
                                                    if (str2.charAt(pos3) == sigDigit) {
                                                        pos3++;
                                                        expSignAlways = true;
                                                    }
                                                } else {
                                                    c = sigDigit;
                                                    sigDigit = plus;
                                                }
                                                expDigits = 0;
                                                while (pos3 < pattern.length() && str2.charAt(pos3) == zeroDigit) {
                                                    expDigits = (byte) (expDigits + 1);
                                                    pos3++;
                                                }
                                                zeroDigit2 = zeroDigit;
                                                if ((digitLeftCount + zeroDigitCount < 1 && sigDigitCount + digitRightCount < 1) || ((sigDigitCount <= false && digitLeftCount > 0) || expDigits < 1)) {
                                                    patternError("Malformed exponential", str2);
                                                }
                                            } else {
                                                zeroDigit2 = zeroDigit;
                                                c = sigDigit;
                                                sigDigit = plus;
                                                expDigits = expDigits2;
                                            }
                                            affix4 = suffix;
                                            expDigits2 = expDigits;
                                            groupingCount3 = groupingCount;
                                            sub0Limit = pos3;
                                            decimalPos = padEscape;
                                            pos3--;
                                            multpl3 = multpl2;
                                            padPos2 = padPos4;
                                            sigDigitCount2 = sigDigitCount;
                                            subpart2 = 2;
                                        }
                                        affix2 = affix4;
                                        break;
                                    }
                                    if (digitRightCount > 0) {
                                        affix4 = new StringBuilder();
                                        padPos4 = perMill;
                                        affix4.append("Unexpected '");
                                        affix4.append(minus);
                                        affix4.append('\'');
                                        patternError(affix4.toString(), str2);
                                    } else {
                                        padPos4 = perMill;
                                    }
                                    if (minus == sigDigit) {
                                        sigDigitCount = sigDigitCount3 + 1;
                                    } else {
                                        zeroDigitCount++;
                                        if (minus != zeroDigit) {
                                            padPos3 = (digitLeftCount + zeroDigitCount) + digitRightCount;
                                            if (incrementPos >= 0) {
                                                multpl = incrementPos;
                                                while (multpl < padPos3) {
                                                    incrementVal *= 10;
                                                    multpl++;
                                                }
                                            } else {
                                                multpl = padPos3;
                                            }
                                            incrementPos = multpl;
                                            sigDigitCount = sigDigitCount3;
                                            int p = padPos3;
                                            incrementVal += (long) (minus - zeroDigit);
                                        } else {
                                            sigDigitCount = sigDigitCount3;
                                        }
                                    }
                                    if (groupingCount < (byte) 0 || padEscape >= 0) {
                                        zeroDigit2 = zeroDigit;
                                        c = sigDigit;
                                    } else {
                                        zeroDigit2 = zeroDigit;
                                        c = sigDigit;
                                        groupingCount3 = (byte) (groupingCount + 1);
                                        decimalPos = padEscape;
                                        sigDigit = plus;
                                        exponent3 = exponent;
                                        affix2 = affix3;
                                        multpl3 = multpl2;
                                        padPos2 = padPos4;
                                        sigDigitCount2 = sigDigitCount;
                                    }
                                    groupingCount3 = groupingCount;
                                    decimalPos = padEscape;
                                    sigDigit = plus;
                                    exponent3 = exponent;
                                    affix2 = affix3;
                                    multpl3 = multpl2;
                                    padPos2 = padPos4;
                                    sigDigitCount2 = sigDigitCount;
                                }
                                break;
                            case 1:
                            case 2:
                                StringBuilder stringBuilder;
                                percent2 = percent;
                                if (minus == digit || minus == groupingSeparator || minus == decimalSeparator) {
                                    c7 = separator;
                                    part3 = part2;
                                    c8 = padEscape2;
                                    separator = multpl3;
                                    padPos3 = padPos2;
                                    c9 = minus2;
                                    c5 = perMill2;
                                    percent = percent2;
                                } else if ((minus < zeroDigit || minus > nineDigit) && minus != sigDigit) {
                                    if (minus == CURRENCY_SIGN) {
                                        sigDigitCount3 = pos3 + 1 < pattern.length() && str2.charAt(pos3 + 1) == CURRENCY_SIGN;
                                        if (sigDigitCount3) {
                                            pos3++;
                                            affix2.append(minus);
                                            if (pos3 + 1 >= pattern.length() || !str2.charAt(pos3 + 1)) {
                                                percent = 2;
                                            } else {
                                                pos3++;
                                                affix2.append(minus);
                                                percent = 3;
                                            }
                                        } else {
                                            boolean z = sigDigitCount3;
                                            percent = 1;
                                        }
                                        currencySignCnt = percent;
                                    } else if (minus == '\'') {
                                        if (pos3 + 1 >= pattern.length() || str2.charAt(pos3 + 1) != '\'') {
                                            subpart2 += 2;
                                        } else {
                                            pos3++;
                                            affix2.append(minus);
                                        }
                                    } else if (minus == separator) {
                                        if (subpart2 == 1 || part2 == 1) {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Unquoted special character '");
                                            stringBuilder.append(minus);
                                            stringBuilder.append('\'');
                                            patternError(stringBuilder.toString(), str2);
                                        }
                                        sub2Limit = pos3;
                                        zeroDigit2 = zeroDigit;
                                        c = sigDigit;
                                        pos3++;
                                        c7 = separator;
                                        part3 = part2;
                                        affix3 = affix2;
                                        sigDigit = plus;
                                        exponent3 = exponent;
                                        sigDigitCount = sigDigitCount2;
                                        groupingCount = groupingCount3;
                                        c8 = padEscape2;
                                        padEscape = decimalPos;
                                        expDigits = expDigits2;
                                        multpl2 = multpl3;
                                        padPos4 = padPos2;
                                        c9 = minus2;
                                        c5 = perMill2;
                                        c4 = percent2;
                                        break;
                                    } else {
                                        percent = percent2;
                                        if (minus != percent) {
                                            perMill = perMill2;
                                            if (minus == perMill) {
                                                c5 = perMill;
                                                c7 = separator;
                                                part3 = part2;
                                                c8 = padEscape2;
                                                padPos3 = padPos2;
                                                c9 = minus2;
                                            } else {
                                                c5 = perMill;
                                                perMill = minus2;
                                                if (minus == perMill) {
                                                    minus = PATTERN_MINUS_SIGN;
                                                    c4 = percent;
                                                    c9 = perMill;
                                                    c7 = separator;
                                                    part3 = part2;
                                                    c8 = padEscape2;
                                                    padPos3 = padPos2;
                                                    affix2.append(minus);
                                                    zeroDigit2 = zeroDigit;
                                                    c = sigDigit;
                                                    padPos2 = padPos3;
                                                    sigDigit = plus;
                                                    exponent3 = exponent;
                                                    break;
                                                }
                                                c9 = perMill;
                                                perMill = padEscape2;
                                                if (minus == perMill) {
                                                    c8 = perMill;
                                                    if (padPos2 >= 0) {
                                                        c7 = separator;
                                                        patternError("Multiple pad specifiers", str2);
                                                    } else {
                                                        c7 = separator;
                                                    }
                                                    part3 = part2;
                                                    if (pos3 + 1 == pattern.length()) {
                                                        patternError("Invalid pad specifier", str2);
                                                    }
                                                    separator = pos3 + 1;
                                                    zeroDigit2 = zeroDigit;
                                                    c = sigDigit;
                                                    c4 = percent;
                                                    padPos2 = pos3;
                                                    pos3 = separator;
                                                    padChar = str2.charAt(separator);
                                                    sigDigit = plus;
                                                    exponent3 = exponent;
                                                } else {
                                                    c8 = perMill;
                                                    c7 = separator;
                                                    part3 = part2;
                                                    padPos3 = padPos2;
                                                    c4 = percent;
                                                    separator = multpl3;
                                                    multpl3 = separator;
                                                    affix2.append(minus);
                                                    zeroDigit2 = zeroDigit;
                                                    c = sigDigit;
                                                    padPos2 = padPos3;
                                                    sigDigit = plus;
                                                    exponent3 = exponent;
                                                }
                                            }
                                        } else {
                                            c7 = separator;
                                            part3 = part2;
                                            c8 = padEscape2;
                                            padPos3 = padPos2;
                                            c9 = minus2;
                                            c5 = perMill2;
                                        }
                                        if (multpl3 != 1) {
                                            patternError("Too many percent/permille characters", str2);
                                        }
                                        multpl3 = minus == percent ? 100 : 1000;
                                        minus = minus == percent ? PATTERN_PERCENT : PATTERN_PER_MILLE;
                                        c4 = percent;
                                        affix2.append(minus);
                                        zeroDigit2 = zeroDigit;
                                        c = sigDigit;
                                        padPos2 = padPos3;
                                        sigDigit = plus;
                                        exponent3 = exponent;
                                    }
                                    c7 = separator;
                                    part3 = part2;
                                    c8 = padEscape2;
                                    padPos3 = padPos2;
                                    c9 = minus2;
                                    c5 = perMill2;
                                    c4 = percent2;
                                    affix2.append(minus);
                                    zeroDigit2 = zeroDigit;
                                    c = sigDigit;
                                    padPos2 = padPos3;
                                    sigDigit = plus;
                                    exponent3 = exponent;
                                } else {
                                    c7 = separator;
                                    part3 = part2;
                                    c8 = padEscape2;
                                    separator = multpl3;
                                    padPos3 = padPos2;
                                    c9 = minus2;
                                    c5 = perMill2;
                                    percent = percent2;
                                }
                                if (subpart2 == 1) {
                                    zeroDigit2 = zeroDigit;
                                    c = sigDigit;
                                    c4 = percent;
                                    padPos2 = padPos3;
                                    multpl3 = separator;
                                    sub0Start = pos3;
                                    pos3--;
                                    sigDigit = plus;
                                    exponent3 = exponent;
                                    subpart2 = 0;
                                    break;
                                } else if (minus == '\'') {
                                    c4 = percent;
                                    if (pos3 + 1 >= pattern.length() || str2.charAt(pos3 + 1) != '\'') {
                                        subpart2 += 2;
                                    } else {
                                        pos3++;
                                        affix2.append(minus);
                                    }
                                    zeroDigit2 = zeroDigit;
                                    c = sigDigit;
                                    padPos2 = padPos3;
                                    multpl3 = separator;
                                    sigDigit = plus;
                                    exponent3 = exponent;
                                } else {
                                    c4 = percent;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unquoted special character '");
                                    stringBuilder.append(minus);
                                    stringBuilder.append(39);
                                    patternError(stringBuilder.toString(), str2);
                                    multpl3 = separator;
                                    affix2.append(minus);
                                    zeroDigit2 = zeroDigit;
                                    c = sigDigit;
                                    padPos2 = padPos3;
                                    sigDigit = plus;
                                    exponent3 = exponent;
                                }
                                break;
                            case 3:
                            case 4:
                                if (minus == '\'') {
                                    percent2 = percent;
                                    if (pos3 + 1 >= pattern.length() || str2.charAt(pos3 + 1) != '\'') {
                                        subpart2 -= 2;
                                    } else {
                                        pos3++;
                                        affix2.append(minus);
                                    }
                                } else {
                                    percent2 = percent;
                                }
                                affix2.append(minus);
                                zeroDigit2 = zeroDigit;
                                c = sigDigit;
                                c7 = separator;
                                part3 = part2;
                                sigDigit = plus;
                                exponent3 = exponent;
                                c8 = padEscape2;
                                c9 = minus2;
                                c5 = perMill2;
                                c4 = percent2;
                                break;
                            default:
                                zeroDigit2 = zeroDigit;
                                c = sigDigit;
                                c4 = percent;
                                c7 = separator;
                                part3 = part2;
                                affix3 = affix2;
                                sigDigit = plus;
                                exponent3 = exponent;
                                groupingCount = groupingCount3;
                                c8 = padEscape2;
                                affix2 = decimalPos;
                                c9 = minus2;
                                c5 = perMill2;
                                affix2 = affix3;
                                break;
                        }
                    }
                    zeroDigit2 = zeroDigit;
                    c = sigDigit;
                    c4 = percent;
                    c7 = separator;
                    part3 = part2;
                    affix3 = affix2;
                    sigDigit = plus;
                    exponent3 = exponent;
                    sigDigitCount = sigDigitCount2;
                    groupingCount = groupingCount3;
                    c8 = padEscape2;
                    padEscape = decimalPos;
                    expDigits = expDigits2;
                    multpl2 = multpl3;
                    padPos4 = padPos2;
                    c9 = minus2;
                    c5 = perMill2;
                    pos3++;
                    plus = sigDigit;
                    exponent = exponent3;
                    start2 = start;
                    perMill = c5;
                    minus = c9;
                    padEscape2 = c8;
                    separator = c7;
                    part2 = part3;
                    percent = c4;
                    sigDigit = c;
                    zeroDigit = zeroDigit2;
                }
                if (subpart2 == 3 || subpart2 == 4) {
                    patternError("Unterminated quote", str2);
                }
                if (sub0Limit == 0) {
                    sub0Limit = pattern.length();
                }
                multpl = sub0Limit;
                if (sub2Limit == 0) {
                    sub2Limit = pattern.length();
                }
                int sub2Limit2 = sub2Limit;
                if (zeroDigitCount == 0 && !sigDigitCount && digitLeftCount > 0 && padEscape >= 0) {
                    part = padEscape;
                    if (part == 0) {
                        part++;
                    }
                    digitRightCount = digitLeftCount - part;
                    digitLeftCount = part - 1;
                    zeroDigitCount = 1;
                }
                zeroDigit = digitLeftCount;
                if (padEscape >= 0 || digitRightCount <= 0 || sigDigitCount) {
                    if (padEscape < 0) {
                        c10 = sigDigit;
                    } else if (sigDigitCount <= false || padEscape < zeroDigit) {
                        int padPos5;
                        int sub0Start2;
                        c10 = sigDigit;
                        c2 = groupingSeparator;
                        groupingCount2 = groupingCount22;
                        patternError("Malformed pattern", str2);
                        if (padPos4 >= 0) {
                            c3 = decimalSeparator;
                            int start3 = start;
                            padPos5 = padPos4;
                            if (padPos5 == start3) {
                                padPos2 = 0;
                                str = exponent3;
                                sub0Start2 = sub0Start;
                            } else {
                                str = exponent3;
                                sub0Start2 = sub0Start;
                                if (padPos5 + 2 == sub0Start2) {
                                    padPos2 = 1;
                                } else if (padPos5 == multpl) {
                                    padPos2 = 2;
                                } else if (padPos5 + 2 == sub2Limit2) {
                                    padPos2 = 3;
                                } else {
                                    patternError("Illegal pad position", str2);
                                }
                            }
                            padPos5 = padPos2;
                        } else {
                            c3 = decimalSeparator;
                            str = exponent3;
                            sub0Start2 = sub0Start;
                            int i = start;
                            padPos5 = padPos4;
                        }
                        boolean z2;
                        int i2;
                        char c11;
                        int i3;
                        long incrementVal2;
                        int i4;
                        if (part3 == 0) {
                            boolean expSignAlways2;
                            c6 = digit;
                            String stringBuilder2 = prefix2.toString();
                            this.negPrefixPattern = stringBuilder2;
                            this.posPrefixPattern = stringBuilder2;
                            pos2 = pos3;
                            String stringBuilder3 = suffix.toString();
                            this.negSuffixPattern = stringBuilder3;
                            this.posSuffixPattern = stringBuilder3;
                            this.useExponentialNotation = expDigits >= (byte) 0;
                            if (this.useExponentialNotation) {
                                this.minExponentDigits = expDigits;
                                expSignAlways2 = expSignAlways;
                                this.exponentSignAlwaysShown = expSignAlways2;
                            } else {
                                expSignAlways2 = expSignAlways;
                            }
                            perMill = (zeroDigit + zeroDigitCount) + digitRightCount;
                            part = padEscape >= 0 ? padEscape : perMill;
                            expSignAlways2 = sigDigitCount <= false;
                            setSignificantDigitsUsed(expSignAlways2);
                            if (expSignAlways2) {
                                expSignAlways2 = sigDigitCount;
                                setMinimumSignificantDigits(expSignAlways2);
                                setMaximumSignificantDigits(expSignAlways2 + digitRightCount);
                                z2 = expSignAlways2;
                            } else {
                                i2 = sub2Limit2;
                                expSignAlways2 = sigDigitCount;
                                sub2Limit2 = part - zeroDigit;
                                setMinimumIntegerDigits(sub2Limit2);
                                setMaximumIntegerDigits(this.useExponentialNotation != 0 ? zeroDigit + sub2Limit2 : DOUBLE_INTEGER_DIGITS);
                                if (padEscape >= 0) {
                                    pos3 = perMill - padEscape;
                                } else {
                                    pos3 = 0;
                                }
                                _setMaximumFractionDigits(pos3);
                                if (padEscape >= 0) {
                                    pos3 = (zeroDigit + zeroDigitCount) - padEscape;
                                } else {
                                    pos3 = 0;
                                }
                                setMinimumFractionDigits(pos3);
                            }
                            setGroupingUsed(groupingCount > (byte) 0);
                            this.groupingSize = groupingCount > (byte) 0 ? groupingCount : (byte) 0;
                            byte b = (groupingCount2 <= (byte) 0 || groupingCount2 == groupingCount) ? (byte) 0 : groupingCount2;
                            this.groupingSize2 = b;
                            this.multiplier = multpl2;
                            boolean z3 = padEscape == 0 || padEscape == perMill;
                            setDecimalSeparatorAlwaysShown(z3);
                            if (padPos5 >= 0) {
                                this.padPosition = padPos5;
                                this.formatWidth = multpl - sub0Start2;
                                this.pad = padChar;
                                c11 = zeroDigit;
                            } else {
                                minus = padChar;
                                c11 = zeroDigit;
                                this.formatWidth = 0;
                            }
                            zeroDigit = incrementVal;
                            if (zeroDigit != 0) {
                                padPos5 = incrementPos - part;
                                if (padPos5 > 0) {
                                    i3 = sub0Start2;
                                    sub0Start2 = padPos5;
                                } else {
                                    sub0Start2 = 0;
                                }
                                this.roundingIncrementICU = android.icu.math.BigDecimal.valueOf(zeroDigit, sub0Start2);
                                if (padPos5 < 0) {
                                    incrementVal2 = zeroDigit;
                                    this.roundingIncrementICU = this.roundingIncrementICU.movePointRight(-padPos5);
                                } else {
                                    incrementVal2 = zeroDigit;
                                }
                                this.roundingMode = 6;
                            } else {
                                incrementVal2 = zeroDigit;
                                i4 = padPos5;
                                i3 = sub0Start2;
                                setRoundingIncrement((android.icu.math.BigDecimal) 0);
                            }
                            this.currencySignCount = currencySignCnt;
                        } else {
                            c11 = zeroDigit;
                            byte b2 = groupingCount2;
                            i4 = padPos5;
                            i3 = sub0Start2;
                            byte b3 = expDigits;
                            c6 = digit;
                            pos2 = pos3;
                            i2 = sub2Limit2;
                            StringBuilder suffix2 = suffix;
                            incrementVal2 = incrementVal;
                            zeroDigit = currencySignCnt;
                            boolean z4 = expSignAlways;
                            z2 = sigDigitCount;
                            this.negPrefixPattern = prefix2.toString();
                            this.negSuffixPattern = suffix2.toString();
                            gotNegative = true;
                        }
                        part = part3 + 1;
                        perMill = c5;
                        minus = c9;
                        padEscape = c8;
                        separator = c7;
                        percent = c4;
                        sigDigit = c;
                        zeroDigit = zeroDigit2;
                        plus2 = c10;
                        groupingSeparator = c2;
                        decimalSeparator = c3;
                        exponent2 = str;
                        digit = c6;
                        pos = pos2;
                    } else {
                        c10 = sigDigit;
                    }
                    if (groupingCount != (byte) 0) {
                        groupingCount2 = groupingCount22;
                        c2 = groupingCount2 != (byte) 0 ? (sigDigitCount > false || zeroDigitCount <= 0) ? groupingSeparator : groupingSeparator : groupingSeparator;
                    } else {
                        c2 = groupingSeparator;
                        groupingCount2 = groupingCount22;
                    }
                    patternError("Malformed pattern", str2);
                    if (padPos4 >= 0) {
                    }
                    if (part3 == 0) {
                    }
                    part = part3 + 1;
                    perMill = c5;
                    minus = c9;
                    padEscape = c8;
                    separator = c7;
                    percent = c4;
                    sigDigit = c;
                    zeroDigit = zeroDigit2;
                    plus2 = c10;
                    groupingSeparator = c2;
                    decimalSeparator = c3;
                    exponent2 = str;
                    digit = c6;
                    pos = pos2;
                } else {
                    c10 = sigDigit;
                }
                c2 = groupingSeparator;
                groupingCount2 = groupingCount22;
                patternError("Malformed pattern", str2);
                if (padPos4 >= 0) {
                }
                if (part3 == 0) {
                }
                part = part3 + 1;
                perMill = c5;
                minus = c9;
                padEscape = c8;
                separator = c7;
                percent = c4;
                sigDigit = c;
                zeroDigit = zeroDigit2;
                plus2 = c10;
                groupingSeparator = c2;
                decimalSeparator = c3;
                exponent2 = str;
                digit = c6;
                pos = pos2;
            }
        }
        c = sigDigit;
        c2 = groupingSeparator;
        c3 = decimalSeparator;
        c4 = percent;
        c5 = perMill;
        c6 = digit;
        c7 = separator;
        c8 = padEscape;
        c9 = minus;
        c10 = plus;
        str = exponent;
        if (pattern.length() == 0) {
            String str3 = "";
            this.posSuffixPattern = str3;
            this.posPrefixPattern = str3;
            setMinimumIntegerDigits(0);
            setMaximumIntegerDigits(DOUBLE_INTEGER_DIGITS);
            setMinimumFractionDigits(0);
            _setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }
        if (!gotNegative || (this.negPrefixPattern.equals(this.posPrefixPattern) && this.negSuffixPattern.equals(this.posSuffixPattern))) {
            this.negSuffixPattern = this.posSuffixPattern;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PATTERN_MINUS_SIGN);
            stringBuilder4.append(this.posPrefixPattern);
            this.negPrefixPattern = stringBuilder4.toString();
        }
        setLocale(null, null);
        this.formatPattern = str2;
        if (this.currencySignCount != 0) {
            Currency theCurrency = getCurrency();
            if (theCurrency != null) {
                setRoundingIncrement(theCurrency.getRoundingIncrement(this.currencyUsage));
                int d = theCurrency.getDefaultFractionDigits(this.currencyUsage);
                setMinimumFractionDigits(d);
                _setMaximumFractionDigits(d);
            }
            if (this.currencySignCount == 3 && this.currencyPluralInfo == null) {
                this.currencyPluralInfo = new CurrencyPluralInfo(this.symbols.getULocale());
            }
        }
        resetActualRounding();
    }

    private void patternError(String msg, String pattern) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(" in pattern \"");
        stringBuilder.append(pattern);
        stringBuilder.append('\"');
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void setMaximumIntegerDigits(int newValue) {
        super.setMaximumIntegerDigits(Math.min(newValue, MAX_INTEGER_DIGITS));
    }

    public void setMinimumIntegerDigits(int newValue) {
        super.setMinimumIntegerDigits(Math.min(newValue, DOUBLE_INTEGER_DIGITS));
    }

    public int getMinimumSignificantDigits() {
        return this.minSignificantDigits;
    }

    public int getMaximumSignificantDigits() {
        return this.maxSignificantDigits;
    }

    public void setMinimumSignificantDigits(int min) {
        if (min < 1) {
            min = 1;
        }
        int max = Math.max(this.maxSignificantDigits, min);
        this.minSignificantDigits = min;
        this.maxSignificantDigits = max;
        setSignificantDigitsUsed(true);
    }

    public void setMaximumSignificantDigits(int max) {
        if (max < 1) {
            max = 1;
        }
        this.minSignificantDigits = Math.min(this.minSignificantDigits, max);
        this.maxSignificantDigits = max;
        setSignificantDigitsUsed(true);
    }

    public boolean areSignificantDigitsUsed() {
        return this.useSignificantDigits;
    }

    public void setSignificantDigitsUsed(boolean useSignificantDigits) {
        this.useSignificantDigits = useSignificantDigits;
    }

    public void setCurrency(Currency theCurrency) {
        super.setCurrency(theCurrency);
        if (theCurrency != null) {
            String s = theCurrency.getName(this.symbols.getULocale(), 0, null);
            this.symbols.setCurrency(theCurrency);
            this.symbols.setCurrencySymbol(s);
        }
        if (this.currencySignCount != 0) {
            if (theCurrency != null) {
                setRoundingIncrement(theCurrency.getRoundingIncrement(this.currencyUsage));
                int d = theCurrency.getDefaultFractionDigits(this.currencyUsage);
                setMinimumFractionDigits(d);
                setMaximumFractionDigits(d);
            }
            if (this.currencySignCount != 3) {
                expandAffixes(null);
            }
        }
    }

    public void setCurrencyUsage(CurrencyUsage newUsage) {
        if (newUsage != null) {
            this.currencyUsage = newUsage;
            Currency theCurrency = getCurrency();
            if (theCurrency != null) {
                setRoundingIncrement(theCurrency.getRoundingIncrement(this.currencyUsage));
                int d = theCurrency.getDefaultFractionDigits(this.currencyUsage);
                setMinimumFractionDigits(d);
                _setMaximumFractionDigits(d);
                return;
            }
            return;
        }
        throw new NullPointerException("return value is null at method AAA");
    }

    public CurrencyUsage getCurrencyUsage() {
        return this.currencyUsage;
    }

    @Deprecated
    protected Currency getEffectiveCurrency() {
        Currency c = getCurrency();
        if (c == null) {
            return Currency.getInstance(this.symbols.getInternationalCurrencySymbol());
        }
        return c;
    }

    public void setMaximumFractionDigits(int newValue) {
        _setMaximumFractionDigits(newValue);
        resetActualRounding();
    }

    private void _setMaximumFractionDigits(int newValue) {
        super.setMaximumFractionDigits(Math.min(newValue, DOUBLE_FRACTION_DIGITS));
    }

    public void setMinimumFractionDigits(int newValue) {
        super.setMinimumFractionDigits(Math.min(newValue, DOUBLE_FRACTION_DIGITS));
    }

    public void setParseBigDecimal(boolean value) {
        this.parseBigDecimal = value;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseMaxDigits(int newValue) {
        if (newValue > 0) {
            this.PARSE_MAX_EXPONENT = newValue;
        }
    }

    public int getParseMaxDigits() {
        return this.PARSE_MAX_EXPONENT;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        this.attributes.clear();
        stream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (getMaximumIntegerDigits() > MAX_INTEGER_DIGITS) {
            setMaximumIntegerDigits(MAX_INTEGER_DIGITS);
        }
        if (getMaximumFractionDigits() > DOUBLE_FRACTION_DIGITS) {
            _setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }
        if (this.serialVersionOnStream < 2) {
            this.exponentSignAlwaysShown = false;
            setInternalRoundingIncrement(null);
            this.roundingMode = 6;
            this.formatWidth = 0;
            this.pad = ' ';
            this.padPosition = 0;
            if (this.serialVersionOnStream < 1) {
                this.useExponentialNotation = false;
            }
        }
        if (this.serialVersionOnStream < 3) {
            setCurrencyForSymbols();
        }
        if (this.serialVersionOnStream < 4) {
            this.currencyUsage = CurrencyUsage.STANDARD;
        }
        this.serialVersionOnStream = 4;
        this.digitList = new DigitList_Android();
        if (this.roundingIncrement != null) {
            setInternalRoundingIncrement(new android.icu.math.BigDecimal(this.roundingIncrement));
        }
        resetActualRounding();
    }

    private void setInternalRoundingIncrement(android.icu.math.BigDecimal value) {
        this.roundingIncrementICU = value;
        this.roundingIncrement = value == null ? null : value.toBigDecimal();
    }

    private void resetActualRounding() {
        if (this.roundingIncrementICU != null) {
            android.icu.math.BigDecimal byWidth = getMaximumFractionDigits() > 0 ? android.icu.math.BigDecimal.ONE.movePointLeft(getMaximumFractionDigits()) : android.icu.math.BigDecimal.ONE;
            if (this.roundingIncrementICU.compareTo(byWidth) >= 0) {
                this.actualRoundingIncrementICU = this.roundingIncrementICU;
            } else {
                this.actualRoundingIncrementICU = byWidth.equals(android.icu.math.BigDecimal.ONE) ? null : byWidth;
            }
        } else if (this.roundingMode == 6 || isScientificNotation()) {
            this.actualRoundingIncrementICU = null;
        } else if (getMaximumFractionDigits() > 0) {
            this.actualRoundingIncrementICU = android.icu.math.BigDecimal.ONE.movePointLeft(getMaximumFractionDigits());
        } else {
            this.actualRoundingIncrementICU = android.icu.math.BigDecimal.ONE;
        }
        if (this.actualRoundingIncrementICU == null) {
            setRoundingDouble(0.0d);
            this.actualRoundingIncrement = null;
            return;
        }
        setRoundingDouble(this.actualRoundingIncrementICU.doubleValue());
        this.actualRoundingIncrement = this.actualRoundingIncrementICU.toBigDecimal();
    }

    private void setRoundingDouble(double newValue) {
        this.roundingDouble = newValue;
        if (this.roundingDouble > 0.0d) {
            double rawRoundedReciprocal = 1.0d / this.roundingDouble;
            this.roundingDoubleReciprocal = Math.rint(rawRoundedReciprocal);
            if (Math.abs(rawRoundedReciprocal - this.roundingDoubleReciprocal) > roundingIncrementEpsilon) {
                this.roundingDoubleReciprocal = 0.0d;
                return;
            }
            return;
        }
        this.roundingDoubleReciprocal = 0.0d;
    }
}
