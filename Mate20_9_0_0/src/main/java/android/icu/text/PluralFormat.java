package android.icu.text;

import android.icu.impl.Utility;
import android.icu.text.MessagePattern.Part;
import android.icu.text.MessagePattern.Part.Type;
import android.icu.text.PluralRules.FixedDecimal;
import android.icu.text.PluralRules.IFixedDecimal;
import android.icu.text.PluralRules.PluralType;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Map;

public class PluralFormat extends UFormat {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long serialVersionUID = 1;
    private transient MessagePattern msgPattern;
    private NumberFormat numberFormat;
    private transient double offset;
    private Map<String, String> parsedValues;
    private String pattern;
    private PluralRules pluralRules;
    private transient PluralSelectorAdapter pluralRulesWrapper;
    private ULocale ulocale;

    interface PluralSelector {
        String select(Object obj, double d);
    }

    private final class PluralSelectorAdapter implements PluralSelector {
        private PluralSelectorAdapter() {
        }

        public String select(Object context, double number) {
            return PluralFormat.this.pluralRules.select((IFixedDecimal) context);
        }
    }

    public PluralFormat() {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralType.CARDINAL, ULocale.getDefault(Category.FORMAT), null);
    }

    public PluralFormat(ULocale ulocale) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralType.CARDINAL, ulocale, null);
    }

    public PluralFormat(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    public PluralFormat(PluralRules rules) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(rules, PluralType.CARDINAL, ULocale.getDefault(Category.FORMAT), null);
    }

    public PluralFormat(ULocale ulocale, PluralRules rules) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(rules, PluralType.CARDINAL, ulocale, null);
    }

    public PluralFormat(Locale locale, PluralRules rules) {
        this(ULocale.forLocale(locale), rules);
    }

    public PluralFormat(ULocale ulocale, PluralType type) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, type, ulocale, null);
    }

    public PluralFormat(Locale locale, PluralType type) {
        this(ULocale.forLocale(locale), type);
    }

    public PluralFormat(String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralType.CARDINAL, ULocale.getDefault(Category.FORMAT), null);
        applyPattern(pattern);
    }

    public PluralFormat(ULocale ulocale, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, PluralType.CARDINAL, ulocale, null);
        applyPattern(pattern);
    }

    public PluralFormat(PluralRules rules, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(rules, PluralType.CARDINAL, ULocale.getDefault(Category.FORMAT), null);
        applyPattern(pattern);
    }

    public PluralFormat(ULocale ulocale, PluralRules rules, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(rules, PluralType.CARDINAL, ulocale, null);
        applyPattern(pattern);
    }

    public PluralFormat(ULocale ulocale, PluralType type, String pattern) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, type, ulocale, null);
        applyPattern(pattern);
    }

    PluralFormat(ULocale ulocale, PluralType type, String pattern, NumberFormat numberFormat) {
        this.ulocale = null;
        this.pluralRules = null;
        this.pattern = null;
        this.parsedValues = null;
        this.numberFormat = null;
        this.offset = 0.0d;
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        init(null, type, ulocale, numberFormat);
        applyPattern(pattern);
    }

    private void init(PluralRules rules, PluralType type, ULocale locale, NumberFormat numberFormat) {
        PluralRules forLocale;
        this.ulocale = locale;
        if (rules == null) {
            forLocale = PluralRules.forLocale(this.ulocale, type);
        } else {
            forLocale = rules;
        }
        this.pluralRules = forLocale;
        resetPattern();
        this.numberFormat = numberFormat == null ? NumberFormat.getInstance(this.ulocale) : numberFormat;
    }

    private void resetPattern() {
        this.pattern = null;
        if (this.msgPattern != null) {
            this.msgPattern.clear();
        }
        this.offset = 0.0d;
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        try {
            this.msgPattern.parsePluralStyle(pattern);
            this.offset = this.msgPattern.getPluralOffset(0);
        } catch (RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    public String toPattern() {
        return this.pattern;
    }

    static int findSubMessage(MessagePattern pattern, int partIndex, PluralSelector selector, Object context, double number) {
        double offset;
        int partIndex2;
        int partIndex3;
        MessagePattern messagePattern = pattern;
        int count = messagePattern.countParts();
        Part part = messagePattern.getPart(partIndex);
        if (part.getType().hasNumericValue()) {
            offset = messagePattern.getNumericValue(part);
            partIndex2 = partIndex + 1;
        } else {
            offset = 0.0d;
            partIndex2 = partIndex;
        }
        String keyword = null;
        boolean haveKeywordMatch = false;
        int msgStart = 0;
        do {
            partIndex3 = partIndex2 + 1;
            part = messagePattern.getPart(partIndex2);
            PluralSelector pluralSelector;
            Object obj;
            if (part.getType() == Type.ARG_LIMIT) {
                pluralSelector = selector;
                obj = context;
                break;
            }
            if (messagePattern.getPartType(partIndex3).hasNumericValue()) {
                int partIndex4 = partIndex3 + 1;
                if (number == messagePattern.getNumericValue(messagePattern.getPart(partIndex3))) {
                    return partIndex4;
                }
                pluralSelector = selector;
                obj = context;
                partIndex3 = partIndex4;
            } else {
                if (!haveKeywordMatch) {
                    if (!messagePattern.partSubstringMatches(part, PluralRules.KEYWORD_OTHER)) {
                        if (keyword == null) {
                            keyword = selector.select(context, number - offset);
                            if (msgStart != 0 && keyword.equals(PluralRules.KEYWORD_OTHER)) {
                                haveKeywordMatch = true;
                            }
                        } else {
                            pluralSelector = selector;
                            obj = context;
                        }
                        if (!haveKeywordMatch && messagePattern.partSubstringMatches(part, keyword)) {
                            msgStart = partIndex3;
                            haveKeywordMatch = true;
                        }
                    } else if (msgStart == 0) {
                        msgStart = partIndex3;
                        if (keyword != null && keyword.equals(PluralRules.KEYWORD_OTHER)) {
                            haveKeywordMatch = true;
                        }
                    }
                }
                pluralSelector = selector;
                obj = context;
            }
            partIndex2 = messagePattern.getLimitPartIndex(partIndex3) + 1;
        } while (partIndex2 < count);
        partIndex3 = partIndex2;
        return msgStart;
    }

    public final String format(double number) {
        return format(Double.valueOf(number), number);
    }

    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof Number) {
            Number numberObject = (Number) number;
            toAppendTo.append(format(numberObject, numberObject.doubleValue()));
            return toAppendTo;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");
        stringBuilder.append(number);
        stringBuilder.append("' is not a Number");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private String format(Number numberObject, double number) {
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            return this.numberFormat.format(numberObject);
        }
        String numberString;
        IFixedDecimal fixedDecimal;
        int index;
        double numberMinusOffset = number - this.offset;
        if (this.offset == 0.0d) {
            numberString = this.numberFormat.format(numberObject);
        } else {
            numberString = this.numberFormat.format(numberMinusOffset);
        }
        if (this.numberFormat instanceof DecimalFormat) {
            fixedDecimal = ((DecimalFormat) this.numberFormat).getFixedDecimal(numberMinusOffset);
        } else {
            fixedDecimal = new FixedDecimal(numberMinusOffset);
        }
        int partIndex = findSubMessage(this.msgPattern, 0, this.pluralRulesWrapper, fixedDecimal, number);
        StringBuilder result = null;
        int prevIndex = this.msgPattern.getPart(partIndex).getLimit();
        while (true) {
            partIndex++;
            Part part = this.msgPattern.getPart(partIndex);
            Type type = part.getType();
            index = part.getIndex();
            if (type == Type.MSG_LIMIT) {
                break;
            } else if (type == Type.REPLACE_NUMBER || (type == Type.SKIP_SYNTAX && this.msgPattern.jdkAposMode())) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(this.pattern, prevIndex, index);
                if (type == Type.REPLACE_NUMBER) {
                    result.append(numberString);
                }
                prevIndex = part.getLimit();
            } else if (type == Type.ARG_START) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(this.pattern, prevIndex, index);
                prevIndex = index;
                partIndex = this.msgPattern.getLimitPartIndex(partIndex);
                index = this.msgPattern.getPart(partIndex).getLimit();
                MessagePattern.appendReducedApostrophes(this.pattern, prevIndex, index, result);
                prevIndex = index;
            }
        }
        if (result == null) {
            return this.pattern.substring(prevIndex, index);
        }
        result.append(this.pattern, prevIndex, index);
        return result.toString();
    }

    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    String parseType(String source, RbnfLenientScanner scanner, FieldPosition pos) {
        PluralFormat pluralFormat = this;
        String str = source;
        RbnfLenientScanner rbnfLenientScanner = scanner;
        FieldPosition fieldPosition = pos;
        if (pluralFormat.msgPattern == null || pluralFormat.msgPattern.countParts() == 0) {
            fieldPosition.setBeginIndex(-1);
            fieldPosition.setEndIndex(-1);
            return null;
        }
        int count = pluralFormat.msgPattern.countParts();
        int startingAt = pos.getBeginIndex();
        if (startingAt < 0) {
            startingAt = 0;
        }
        String matchedWord = null;
        String keyword = null;
        Part partSelector = 0;
        int matchedIndex = -1;
        while (partSelector < count) {
            int partIndex = partSelector + 1;
            if (pluralFormat.msgPattern.getPart(partSelector).getType() != Type.ARG_SELECTOR) {
                partSelector = partIndex;
            } else {
                int partIndex2 = partIndex + 1;
                Part partStart = pluralFormat.msgPattern.getPart(partIndex);
                if (partStart.getType() != Type.MSG_START) {
                    partSelector = partIndex2;
                } else {
                    int partIndex3 = partIndex2 + 1;
                    Part partLimit = pluralFormat.msgPattern.getPart(partIndex2);
                    if (partLimit.getType() != Type.MSG_LIMIT) {
                        partSelector = partIndex3;
                    } else {
                        int currMatchIndex;
                        String currArg = pluralFormat.pattern.substring(partStart.getLimit(), partLimit.getIndex());
                        if (rbnfLenientScanner != null) {
                            currMatchIndex = rbnfLenientScanner.findText(str, currArg, startingAt)[0];
                        } else {
                            currMatchIndex = str.indexOf(currArg, startingAt);
                        }
                        if (currMatchIndex >= 0 && currMatchIndex >= matchedIndex && (matchedWord == null || currArg.length() > matchedWord.length())) {
                            int matchedIndex2 = currMatchIndex;
                            String matchedWord2 = currArg;
                            keyword = pluralFormat.pattern.substring(partStart.getLimit(), partLimit.getIndex());
                            matchedWord = matchedWord2;
                            matchedIndex = matchedIndex2;
                        }
                        partSelector = partIndex3;
                        pluralFormat = this;
                        str = source;
                    }
                }
            }
        }
        if (keyword != null) {
            fieldPosition.setBeginIndex(matchedIndex);
            fieldPosition.setEndIndex(matchedWord.length() + matchedIndex);
            return keyword;
        }
        fieldPosition.setBeginIndex(-1);
        fieldPosition.setEndIndex(-1);
        return null;
    }

    @Deprecated
    public void setLocale(ULocale ulocale) {
        if (ulocale == null) {
            ulocale = ULocale.getDefault(Category.FORMAT);
        }
        init(null, PluralType.CARDINAL, ulocale, null);
    }

    public void setNumberFormat(NumberFormat format) {
        this.numberFormat = format;
    }

    public boolean equals(Object rhs) {
        boolean z = true;
        if (this == rhs) {
            return true;
        }
        if (rhs == null || getClass() != rhs.getClass()) {
            return false;
        }
        PluralFormat pf = (PluralFormat) rhs;
        if (!(Utility.objectEquals(this.ulocale, pf.ulocale) && Utility.objectEquals(this.pluralRules, pf.pluralRules) && Utility.objectEquals(this.msgPattern, pf.msgPattern) && Utility.objectEquals(this.numberFormat, pf.numberFormat))) {
            z = false;
        }
        return z;
    }

    public boolean equals(PluralFormat rhs) {
        return equals((Object) rhs);
    }

    public int hashCode() {
        return this.pluralRules.hashCode() ^ this.parsedValues.hashCode();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("locale=");
        stringBuilder.append(this.ulocale);
        buf.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", rules='");
        stringBuilder.append(this.pluralRules);
        stringBuilder.append("'");
        buf.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", pattern='");
        stringBuilder.append(this.pattern);
        stringBuilder.append("'");
        buf.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", format='");
        stringBuilder.append(this.numberFormat);
        stringBuilder.append("'");
        buf.append(stringBuilder.toString());
        return buf.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.pluralRulesWrapper = new PluralSelectorAdapter();
        this.parsedValues = null;
        if (this.pattern != null) {
            applyPattern(this.pattern);
        }
    }
}
