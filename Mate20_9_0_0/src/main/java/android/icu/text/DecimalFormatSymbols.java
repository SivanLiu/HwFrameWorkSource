package android.icu.text;

import android.icu.impl.CacheBase;
import android.icu.impl.CurrencyData;
import android.icu.impl.CurrencyData.CurrencyDisplayInfo;
import android.icu.impl.CurrencyData.CurrencyFormatInfo;
import android.icu.impl.CurrencyData.CurrencySpacingInfo;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SoftCache;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.util.Currency;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import android.icu.util.ULocale.Type;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;

public class DecimalFormatSymbols implements Cloneable, Serializable {
    public static final int CURRENCY_SPC_CURRENCY_MATCH = 0;
    public static final int CURRENCY_SPC_INSERT = 2;
    public static final int CURRENCY_SPC_SURROUNDING_MATCH = 1;
    private static final char DEF_DECIMAL_SEPARATOR = '.';
    private static final char[] DEF_DIGIT_CHARS_ARRAY = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static final String[] DEF_DIGIT_STRINGS_ARRAY = new String[]{AndroidHardcodedSystemProperties.JAVA_VERSION, "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final char DEF_GROUPING_SEPARATOR = ',';
    private static final char DEF_MINUS_SIGN = '-';
    private static final char DEF_PERCENT = '%';
    private static final char DEF_PERMILL = '‰';
    private static final char DEF_PLUS_SIGN = '+';
    private static final String LATIN_NUMBERING_SYSTEM = "latn";
    private static final String NUMBER_ELEMENTS = "NumberElements";
    private static final String SYMBOLS = "symbols";
    private static final String[] SYMBOL_DEFAULTS = new String[]{String.valueOf(DEF_DECIMAL_SEPARATOR), String.valueOf(DEF_GROUPING_SEPARATOR), ";", String.valueOf(DEF_PERCENT), String.valueOf(DEF_MINUS_SIGN), String.valueOf(DEF_PLUS_SIGN), DateFormat.ABBR_WEEKDAY, String.valueOf(DEF_PERMILL), "∞", "NaN", null, null, "×"};
    private static final String[] SYMBOL_KEYS = new String[]{"decimal", "group", "list", "percentSign", "minusSign", "plusSign", "exponential", "perMille", "infinity", "nan", "currencyDecimal", "currencyGroup", "superscriptingExponent"};
    private static final CacheBase<ULocale, CacheData, Void> cachedLocaleData = new SoftCache<ULocale, CacheData, Void>() {
        protected CacheData createInstance(ULocale locale, Void unused) {
            return DecimalFormatSymbols.loadData(locale);
        }
    };
    private static final int currentSerialVersion = 8;
    private static final long serialVersionUID = 5772796243397350300L;
    private String NaN;
    private ULocale actualLocale;
    private transient int codePointZero;
    private transient Currency currency;
    private String currencyPattern;
    private String[] currencySpcAfterSym;
    private String[] currencySpcBeforeSym;
    private String currencySymbol;
    private char decimalSeparator;
    private String decimalSeparatorString;
    private char digit;
    private String[] digitStrings;
    private char[] digits;
    private String exponentMultiplicationSign;
    private String exponentSeparator;
    private char exponential;
    private char groupingSeparator;
    private String groupingSeparatorString;
    private String infinity;
    private String intlCurrencySymbol;
    private char minusSign;
    private String minusString;
    private char monetaryGroupingSeparator;
    private String monetaryGroupingSeparatorString;
    private char monetarySeparator;
    private String monetarySeparatorString;
    private char padEscape;
    private char patternSeparator;
    private char perMill;
    private String perMillString;
    private char percent;
    private String percentString;
    private char plusSign;
    private String plusString;
    private Locale requestedLocale;
    private int serialVersionOnStream;
    private char sigDigit;
    private ULocale ulocale;
    private ULocale validLocale;
    private char zeroDigit;

    private static class CacheData {
        final String[] digits;
        final String[] numberElements;
        final ULocale validLocale;

        public CacheData(ULocale loc, String[] digits, String[] numberElements) {
            this.validLocale = loc;
            this.digits = digits;
            this.numberElements = numberElements;
        }
    }

    private static final class DecFmtDataSink extends Sink {
        private String[] numberElements;

        public DecFmtDataSink(String[] numberElements) {
            this.numberElements = numberElements;
        }

        public void put(Key key, Value value, boolean noFallback) {
            Table symbolsTable = value.getTable();
            for (int j = 0; symbolsTable.getKeyAndValue(j, key, value); j++) {
                int i = 0;
                while (i < DecimalFormatSymbols.SYMBOL_KEYS.length) {
                    if (key.contentEquals(DecimalFormatSymbols.SYMBOL_KEYS[i])) {
                        if (this.numberElements[i] == null) {
                            this.numberElements[i] = value.toString();
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
    }

    public DecimalFormatSymbols() {
        this(ULocale.getDefault(Category.FORMAT));
    }

    public DecimalFormatSymbols(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public DecimalFormatSymbols(ULocale locale) {
        this.exponentMultiplicationSign = null;
        this.serialVersionOnStream = 8;
        this.currencyPattern = null;
        initialize(locale, null);
    }

    private DecimalFormatSymbols(Locale locale, NumberingSystem ns) {
        this(ULocale.forLocale(locale), ns);
    }

    private DecimalFormatSymbols(ULocale locale, NumberingSystem ns) {
        this.exponentMultiplicationSign = null;
        this.serialVersionOnStream = 8;
        this.currencyPattern = null;
        initialize(locale, ns);
    }

    public static DecimalFormatSymbols getInstance() {
        return new DecimalFormatSymbols();
    }

    public static DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    public static DecimalFormatSymbols getInstance(ULocale locale) {
        return new DecimalFormatSymbols(locale);
    }

    public static DecimalFormatSymbols forNumberingSystem(Locale locale, NumberingSystem ns) {
        return new DecimalFormatSymbols(locale, ns);
    }

    public static DecimalFormatSymbols forNumberingSystem(ULocale locale, NumberingSystem ns) {
        return new DecimalFormatSymbols(locale, ns);
    }

    public static Locale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableLocales();
    }

    public static ULocale[] getAvailableULocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    public char getZeroDigit() {
        return this.zeroDigit;
    }

    public char[] getDigits() {
        return (char[]) this.digits.clone();
    }

    public void setZeroDigit(char zeroDigit) {
        this.zeroDigit = zeroDigit;
        this.digitStrings = (String[]) this.digitStrings.clone();
        this.digits = (char[]) this.digits.clone();
        this.digitStrings[0] = String.valueOf(zeroDigit);
        this.digits[0] = zeroDigit;
        for (int i = 1; i < 10; i++) {
            char d = (char) (zeroDigit + i);
            this.digitStrings[i] = String.valueOf(d);
            this.digits[i] = d;
        }
        this.codePointZero = zeroDigit;
    }

    public String[] getDigitStrings() {
        return (String[]) this.digitStrings.clone();
    }

    @Deprecated
    public String[] getDigitStringsLocal() {
        return this.digitStrings;
    }

    @Deprecated
    public int getCodePointZero() {
        return this.codePointZero;
    }

    public void setDigitStrings(String[] digitStrings) {
        if (digitStrings == null) {
            throw new NullPointerException("The input digit string array is null");
        } else if (digitStrings.length == 10) {
            String[] tmpDigitStrings = new String[10];
            int tmpCodePointZero = -1;
            char[] tmpDigits = new char[10];
            int i = 0;
            while (i < 10) {
                String digitStr = digitStrings[i];
                if (digitStr != null) {
                    int cp;
                    int cc;
                    tmpDigitStrings[i] = digitStr;
                    if (digitStr.length() == 0) {
                        cp = -1;
                        cc = 0;
                    } else {
                        cp = Character.codePointAt(digitStrings[i], 0);
                        cc = Character.charCount(cp);
                    }
                    if (cc == digitStr.length()) {
                        if (cc != 1 || tmpDigits == null) {
                            tmpDigits = null;
                        } else {
                            tmpDigits[i] = (char) cp;
                        }
                        if (i == 0) {
                            tmpCodePointZero = cp;
                        } else if (cp != tmpCodePointZero + i) {
                            tmpCodePointZero = -1;
                        }
                    } else {
                        tmpCodePointZero = -1;
                        tmpDigits = null;
                    }
                    i++;
                } else {
                    throw new IllegalArgumentException("The input digit string array contains a null element");
                }
            }
            this.digitStrings = tmpDigitStrings;
            this.codePointZero = tmpCodePointZero;
            if (tmpDigits == null) {
                this.zeroDigit = DEF_DIGIT_CHARS_ARRAY[0];
                this.digits = DEF_DIGIT_CHARS_ARRAY;
                return;
            }
            this.zeroDigit = tmpDigits[0];
            this.digits = tmpDigits;
        } else {
            throw new IllegalArgumentException("Number of digit strings is not 10");
        }
    }

    public char getSignificantDigit() {
        return this.sigDigit;
    }

    public void setSignificantDigit(char sigDigit) {
        this.sigDigit = sigDigit;
    }

    public char getGroupingSeparator() {
        return this.groupingSeparator;
    }

    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
        this.groupingSeparatorString = String.valueOf(groupingSeparator);
    }

    public String getGroupingSeparatorString() {
        return this.groupingSeparatorString;
    }

    public void setGroupingSeparatorString(String groupingSeparatorString) {
        if (groupingSeparatorString != null) {
            this.groupingSeparatorString = groupingSeparatorString;
            if (groupingSeparatorString.length() == 1) {
                this.groupingSeparator = groupingSeparatorString.charAt(0);
                return;
            } else {
                this.groupingSeparator = DEF_GROUPING_SEPARATOR;
                return;
            }
        }
        throw new NullPointerException("The input grouping separator is null");
    }

    public char getDecimalSeparator() {
        return this.decimalSeparator;
    }

    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
        this.decimalSeparatorString = String.valueOf(decimalSeparator);
    }

    public String getDecimalSeparatorString() {
        return this.decimalSeparatorString;
    }

    public void setDecimalSeparatorString(String decimalSeparatorString) {
        if (decimalSeparatorString != null) {
            this.decimalSeparatorString = decimalSeparatorString;
            if (decimalSeparatorString.length() == 1) {
                this.decimalSeparator = decimalSeparatorString.charAt(0);
                return;
            } else {
                this.decimalSeparator = DEF_DECIMAL_SEPARATOR;
                return;
            }
        }
        throw new NullPointerException("The input decimal separator is null");
    }

    public char getPerMill() {
        return this.perMill;
    }

    public void setPerMill(char perMill) {
        this.perMill = perMill;
        this.perMillString = String.valueOf(perMill);
    }

    public String getPerMillString() {
        return this.perMillString;
    }

    public void setPerMillString(String perMillString) {
        if (perMillString != null) {
            this.perMillString = perMillString;
            if (perMillString.length() == 1) {
                this.perMill = perMillString.charAt(0);
                return;
            } else {
                this.perMill = DEF_PERMILL;
                return;
            }
        }
        throw new NullPointerException("The input permille string is null");
    }

    public char getPercent() {
        return this.percent;
    }

    public void setPercent(char percent) {
        this.percent = percent;
        this.percentString = String.valueOf(percent);
    }

    public String getPercentString() {
        return this.percentString;
    }

    public void setPercentString(String percentString) {
        if (percentString != null) {
            this.percentString = percentString;
            if (percentString.length() == 1) {
                this.percent = percentString.charAt(0);
                return;
            } else {
                this.percent = DEF_PERCENT;
                return;
            }
        }
        throw new NullPointerException("The input percent sign is null");
    }

    public char getDigit() {
        return this.digit;
    }

    public void setDigit(char digit) {
        this.digit = digit;
    }

    public char getPatternSeparator() {
        return this.patternSeparator;
    }

    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }

    public String getInfinity() {
        return this.infinity;
    }

    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }

    public String getNaN() {
        return this.NaN;
    }

    public void setNaN(String NaN) {
        this.NaN = NaN;
    }

    public char getMinusSign() {
        return this.minusSign;
    }

    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
        this.minusString = String.valueOf(minusSign);
    }

    public String getMinusSignString() {
        return this.minusString;
    }

    public void setMinusSignString(String minusSignString) {
        if (minusSignString != null) {
            this.minusString = minusSignString;
            if (minusSignString.length() == 1) {
                this.minusSign = minusSignString.charAt(0);
                return;
            } else {
                this.minusSign = DEF_MINUS_SIGN;
                return;
            }
        }
        throw new NullPointerException("The input minus sign is null");
    }

    public char getPlusSign() {
        return this.plusSign;
    }

    public void setPlusSign(char plus) {
        this.plusSign = plus;
        this.plusString = String.valueOf(plus);
    }

    public String getPlusSignString() {
        return this.plusString;
    }

    public void setPlusSignString(String plusSignString) {
        if (plusSignString != null) {
            this.plusString = plusSignString;
            if (plusSignString.length() == 1) {
                this.plusSign = plusSignString.charAt(0);
                return;
            } else {
                this.plusSign = DEF_PLUS_SIGN;
                return;
            }
        }
        throw new NullPointerException("The input plus sign is null");
    }

    public String getCurrencySymbol() {
        return this.currencySymbol;
    }

    public void setCurrencySymbol(String currency) {
        this.currencySymbol = currency;
    }

    public String getInternationalCurrencySymbol() {
        return this.intlCurrencySymbol;
    }

    public void setInternationalCurrencySymbol(String currency) {
        this.intlCurrencySymbol = currency;
    }

    public Currency getCurrency() {
        return this.currency;
    }

    public void setCurrency(Currency currency) {
        if (currency != null) {
            this.currency = currency;
            this.intlCurrencySymbol = currency.getCurrencyCode();
            this.currencySymbol = currency.getSymbol(this.requestedLocale);
            return;
        }
        throw new NullPointerException();
    }

    public char getMonetaryDecimalSeparator() {
        return this.monetarySeparator;
    }

    public void setMonetaryDecimalSeparator(char sep) {
        this.monetarySeparator = sep;
        this.monetarySeparatorString = String.valueOf(sep);
    }

    public String getMonetaryDecimalSeparatorString() {
        return this.monetarySeparatorString;
    }

    public void setMonetaryDecimalSeparatorString(String sep) {
        if (sep != null) {
            this.monetarySeparatorString = sep;
            if (sep.length() == 1) {
                this.monetarySeparator = sep.charAt(0);
                return;
            } else {
                this.monetarySeparator = DEF_DECIMAL_SEPARATOR;
                return;
            }
        }
        throw new NullPointerException("The input monetary decimal separator is null");
    }

    public char getMonetaryGroupingSeparator() {
        return this.monetaryGroupingSeparator;
    }

    public void setMonetaryGroupingSeparator(char sep) {
        this.monetaryGroupingSeparator = sep;
        this.monetaryGroupingSeparatorString = String.valueOf(sep);
    }

    public String getMonetaryGroupingSeparatorString() {
        return this.monetaryGroupingSeparatorString;
    }

    public void setMonetaryGroupingSeparatorString(String sep) {
        if (sep != null) {
            this.monetaryGroupingSeparatorString = sep;
            if (sep.length() == 1) {
                this.monetaryGroupingSeparator = sep.charAt(0);
                return;
            } else {
                this.monetaryGroupingSeparator = DEF_GROUPING_SEPARATOR;
                return;
            }
        }
        throw new NullPointerException("The input monetary grouping separator is null");
    }

    String getCurrencyPattern() {
        return this.currencyPattern;
    }

    public String getExponentMultiplicationSign() {
        return this.exponentMultiplicationSign;
    }

    public void setExponentMultiplicationSign(String exponentMultiplicationSign) {
        this.exponentMultiplicationSign = exponentMultiplicationSign;
    }

    public String getExponentSeparator() {
        return this.exponentSeparator;
    }

    public void setExponentSeparator(String exp) {
        this.exponentSeparator = exp;
    }

    public char getPadEscape() {
        return this.padEscape;
    }

    public void setPadEscape(char c) {
        this.padEscape = c;
    }

    public String getPatternForCurrencySpacing(int itemType, boolean beforeCurrency) {
        if (itemType < 0 || itemType > 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown currency spacing: ");
            stringBuilder.append(itemType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (beforeCurrency) {
            return this.currencySpcBeforeSym[itemType];
        } else {
            return this.currencySpcAfterSym[itemType];
        }
    }

    public void setPatternForCurrencySpacing(int itemType, boolean beforeCurrency, String pattern) {
        if (itemType < 0 || itemType > 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown currency spacing: ");
            stringBuilder.append(itemType);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (beforeCurrency) {
            this.currencySpcBeforeSym[itemType] = pattern;
        } else {
            this.currencySpcAfterSym[itemType] = pattern;
        }
    }

    public Locale getLocale() {
        return this.requestedLocale;
    }

    public ULocale getULocale() {
        return this.ulocale;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DecimalFormatSymbols)) {
            return false;
        }
        boolean z = true;
        if (this == obj) {
            return true;
        }
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        int i = 0;
        while (i <= 2) {
            if (!this.currencySpcBeforeSym[i].equals(other.currencySpcBeforeSym[i]) || !this.currencySpcAfterSym[i].equals(other.currencySpcAfterSym[i])) {
                return false;
            }
            i++;
        }
        if (other.digits == null) {
            for (i = 0; i < 10; i++) {
                if (this.digits[i] != other.zeroDigit + i) {
                    return false;
                }
            }
        } else if (!Arrays.equals(this.digits, other.digits)) {
            return false;
        }
        if (!(this.groupingSeparator == other.groupingSeparator && this.decimalSeparator == other.decimalSeparator && this.percent == other.percent && this.perMill == other.perMill && this.digit == other.digit && this.minusSign == other.minusSign && this.minusString.equals(other.minusString) && this.patternSeparator == other.patternSeparator && this.infinity.equals(other.infinity) && this.NaN.equals(other.NaN) && this.currencySymbol.equals(other.currencySymbol) && this.intlCurrencySymbol.equals(other.intlCurrencySymbol) && this.padEscape == other.padEscape && this.plusSign == other.plusSign && this.plusString.equals(other.plusString) && this.exponentSeparator.equals(other.exponentSeparator) && this.monetarySeparator == other.monetarySeparator && this.monetaryGroupingSeparator == other.monetaryGroupingSeparator && this.exponentMultiplicationSign.equals(other.exponentMultiplicationSign))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (((this.digits[0] * 37) + this.groupingSeparator) * 37) + this.decimalSeparator;
    }

    private void initialize(ULocale locale, NumberingSystem ns) {
        this.requestedLocale = locale.toLocale();
        this.ulocale = locale;
        CacheData data = (CacheData) cachedLocaleData.getInstance(ns == null ? locale : locale.setKeywordValue("numbers", ns.getName()), null);
        setLocale(data.validLocale, data.validLocale);
        setDigitStrings(data.digits);
        String[] numberElements = data.numberElements;
        setDecimalSeparatorString(numberElements[0]);
        setGroupingSeparatorString(numberElements[1]);
        this.patternSeparator = numberElements[2].charAt(0);
        setPercentString(numberElements[3]);
        setMinusSignString(numberElements[4]);
        setPlusSignString(numberElements[5]);
        setExponentSeparator(numberElements[6]);
        setPerMillString(numberElements[7]);
        setInfinity(numberElements[8]);
        setNaN(numberElements[9]);
        setMonetaryDecimalSeparatorString(numberElements[10]);
        setMonetaryGroupingSeparatorString(numberElements[11]);
        setExponentMultiplicationSign(numberElements[12]);
        this.digit = '#';
        this.padEscape = '*';
        this.sigDigit = '@';
        CurrencyDisplayInfo info = CurrencyData.provider.getInstance(locale, true);
        this.currency = Currency.getInstance(locale);
        if (this.currency != null) {
            this.intlCurrencySymbol = this.currency.getCurrencyCode();
            this.currencySymbol = this.currency.getName(locale, 0, null);
            CurrencyFormatInfo fmtInfo = info.getFormatInfo(this.intlCurrencySymbol);
            if (fmtInfo != null) {
                this.currencyPattern = fmtInfo.currencyPattern;
                setMonetaryDecimalSeparatorString(fmtInfo.monetaryDecimalSeparator);
                setMonetaryGroupingSeparatorString(fmtInfo.monetaryGroupingSeparator);
            }
        } else {
            this.intlCurrencySymbol = "XXX";
            this.currencySymbol = "¤";
        }
        initSpacingInfo(info.getSpacingInfo());
    }

    private static CacheData loadData(ULocale locale) {
        String nsName;
        int i;
        NumberingSystem ns = NumberingSystem.getInstance(locale);
        String[] digits = new String[10];
        if (ns == null || ns.getRadix() != 10 || ns.isAlgorithmic() || !NumberingSystem.isValidDigitString(ns.getDescription())) {
            digits = DEF_DIGIT_STRINGS_ARRAY;
            nsName = LATIN_NUMBERING_SYSTEM;
        } else {
            nsName = ns.getDescription();
            int offset = 0;
            for (int i2 = 0; i2 < 10; i2++) {
                int nextOffset = Character.charCount(nsName.codePointAt(offset)) + offset;
                digits[i2] = nsName.substring(offset, nextOffset);
                offset = nextOffset;
            }
            nsName = ns.getName();
        }
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        ULocale validLocale = rb.getULocale();
        String[] numberElements = new String[SYMBOL_KEYS.length];
        DecFmtDataSink sink = new DecFmtDataSink(numberElements);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NumberElements/");
            stringBuilder.append(nsName);
            stringBuilder.append("/");
            stringBuilder.append(SYMBOLS);
            rb.getAllItemsWithFallback(stringBuilder.toString(), sink);
        } catch (MissingResourceException e) {
        }
        boolean hasNull = false;
        for (String entry : numberElements) {
            if (entry == null) {
                hasNull = true;
                break;
            }
        }
        if (hasNull && !nsName.equals(LATIN_NUMBERING_SYSTEM)) {
            rb.getAllItemsWithFallback("NumberElements/latn/symbols", sink);
        }
        for (i = 0; i < SYMBOL_KEYS.length; i++) {
            if (numberElements[i] == null) {
                numberElements[i] = SYMBOL_DEFAULTS[i];
            }
        }
        if (numberElements[10] == null) {
            numberElements[10] = numberElements[0];
        }
        if (numberElements[11] == null) {
            numberElements[11] = numberElements[1];
        }
        return new CacheData(validLocale, digits, numberElements);
    }

    private void initSpacingInfo(CurrencySpacingInfo spcInfo) {
        this.currencySpcBeforeSym = spcInfo.getBeforeSymbols();
        this.currencySpcAfterSym = spcInfo.getAfterSymbols();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            this.monetarySeparator = this.decimalSeparator;
            this.exponential = 'E';
        }
        if (this.serialVersionOnStream < 2) {
            this.padEscape = '*';
            this.plusSign = DEF_PLUS_SIGN;
            this.exponentSeparator = String.valueOf(this.exponential);
        }
        if (this.serialVersionOnStream < 3) {
            this.requestedLocale = Locale.getDefault();
        }
        if (this.serialVersionOnStream < 4) {
            this.ulocale = ULocale.forLocale(this.requestedLocale);
        }
        if (this.serialVersionOnStream < 5) {
            this.monetaryGroupingSeparator = this.groupingSeparator;
        }
        if (this.serialVersionOnStream < 6) {
            if (this.currencySpcBeforeSym == null) {
                this.currencySpcBeforeSym = new String[3];
            }
            if (this.currencySpcAfterSym == null) {
                this.currencySpcAfterSym = new String[3];
            }
            initSpacingInfo(CurrencySpacingInfo.DEFAULT);
        }
        if (this.serialVersionOnStream < 7) {
            if (this.minusString == null) {
                this.minusString = String.valueOf(this.minusSign);
            }
            if (this.plusString == null) {
                this.plusString = String.valueOf(this.plusSign);
            }
        }
        if (this.serialVersionOnStream < 8 && this.exponentMultiplicationSign == null) {
            this.exponentMultiplicationSign = "×";
        }
        if (this.serialVersionOnStream < 9) {
            if (this.digitStrings == null) {
                this.digitStrings = new String[10];
                int i = 0;
                if (this.digits != null && this.digits.length == 10) {
                    this.zeroDigit = this.digits[0];
                    while (true) {
                        int i2 = i;
                        if (i2 >= 10) {
                            break;
                        }
                        this.digitStrings[i2] = String.valueOf(this.digits[i2]);
                        i = i2 + 1;
                    }
                } else {
                    char digit = this.zeroDigit;
                    if (this.digits == null) {
                        this.digits = new char[10];
                    }
                    while (i < 10) {
                        this.digits[i] = digit;
                        this.digitStrings[i] = String.valueOf(digit);
                        digit = (char) (digit + 1);
                        i++;
                    }
                }
            }
            if (this.decimalSeparatorString == null) {
                this.decimalSeparatorString = String.valueOf(this.decimalSeparator);
            }
            if (this.groupingSeparatorString == null) {
                this.groupingSeparatorString = String.valueOf(this.groupingSeparator);
            }
            if (this.percentString == null) {
                this.percentString = String.valueOf(this.percent);
            }
            if (this.perMillString == null) {
                this.perMillString = String.valueOf(this.perMill);
            }
            if (this.monetarySeparatorString == null) {
                this.monetarySeparatorString = String.valueOf(this.monetarySeparator);
            }
            if (this.monetaryGroupingSeparatorString == null) {
                this.monetaryGroupingSeparatorString = String.valueOf(this.monetaryGroupingSeparator);
            }
        }
        this.serialVersionOnStream = 8;
        this.currency = Currency.getInstance(this.intlCurrencySymbol);
        setDigitStrings(this.digitStrings);
    }

    public final ULocale getLocale(Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale valid, ULocale actual) {
        Object obj = null;
        Object obj2 = valid == null ? 1 : null;
        if (actual == null) {
            obj = 1;
        }
        if (obj2 == obj) {
            this.validLocale = valid;
            this.actualLocale = actual;
            return;
        }
        throw new IllegalArgumentException();
    }
}
