package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.text.MeasureFormat.FormatWidth;
import android.icu.util.Measure;
import android.icu.util.TimeUnit;
import android.icu.util.TimeUnitAmount;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import android.icu.util.UResourceBundle;
import java.io.ObjectStreamException;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

@Deprecated
public class TimeUnitFormat extends MeasureFormat {
    @Deprecated
    public static final int ABBREVIATED_NAME = 1;
    private static final String DEFAULT_PATTERN_FOR_DAY = "{0} d";
    private static final String DEFAULT_PATTERN_FOR_HOUR = "{0} h";
    private static final String DEFAULT_PATTERN_FOR_MINUTE = "{0} min";
    private static final String DEFAULT_PATTERN_FOR_MONTH = "{0} m";
    private static final String DEFAULT_PATTERN_FOR_SECOND = "{0} s";
    private static final String DEFAULT_PATTERN_FOR_WEEK = "{0} w";
    private static final String DEFAULT_PATTERN_FOR_YEAR = "{0} y";
    @Deprecated
    public static final int FULL_NAME = 0;
    private static final int TOTAL_STYLES = 2;
    private static final long serialVersionUID = -3707773153184971529L;
    private NumberFormat format;
    private transient boolean isReady;
    private ULocale locale;
    private transient MeasureFormat mf;
    private transient PluralRules pluralRules;
    private int style;
    private transient Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns;

    private static final class TimeUnitFormatSetupSink extends Sink {
        boolean beenHere = false;
        ULocale locale;
        Set<String> pluralKeywords;
        int style;
        Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns;

        TimeUnitFormatSetupSink(Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns, int style, Set<String> pluralKeywords, ULocale locale) {
            this.timeUnitToCountToPatterns = timeUnitToCountToPatterns;
            this.style = style;
            this.pluralKeywords = pluralKeywords;
            this.locale = locale;
        }

        public void put(Key key, Value value, boolean noFallback) {
            Key key2 = key;
            Value value2 = value;
            if (!this.beenHere) {
                this.beenHere = true;
                Table units = value.getTable();
                for (int i = 0; units.getKeyAndValue(i, key2, value2); i++) {
                    TimeUnit timeUnit;
                    String timeUnitName = key.toString();
                    if (timeUnitName.equals("year")) {
                        timeUnit = TimeUnit.YEAR;
                    } else if (timeUnitName.equals("month")) {
                        timeUnit = TimeUnit.MONTH;
                    } else if (timeUnitName.equals("day")) {
                        timeUnit = TimeUnit.DAY;
                    } else if (timeUnitName.equals("hour")) {
                        timeUnit = TimeUnit.HOUR;
                    } else if (timeUnitName.equals("minute")) {
                        timeUnit = TimeUnit.MINUTE;
                    } else if (timeUnitName.equals("second")) {
                        timeUnit = TimeUnit.SECOND;
                    } else if (timeUnitName.equals("week")) {
                        timeUnit = TimeUnit.WEEK;
                    } else {
                    }
                    Map<String, Object[]> countToPatterns = (Map) this.timeUnitToCountToPatterns.get(timeUnit);
                    if (countToPatterns == null) {
                        countToPatterns = new TreeMap();
                        this.timeUnitToCountToPatterns.put(timeUnit, countToPatterns);
                    }
                    Table countsToPatternTable = value.getTable();
                    for (int j = 0; countsToPatternTable.getKeyAndValue(j, key2, value2); j++) {
                        String pluralCount = key.toString();
                        if (this.pluralKeywords.contains(pluralCount)) {
                            Object[] pair = (Object[]) countToPatterns.get(pluralCount);
                            if (pair == null) {
                                pair = new Object[2];
                                countToPatterns.put(pluralCount, pair);
                            }
                            if (pair[this.style] == null) {
                                pair[this.style] = new MessageFormat(value.getString(), this.locale);
                            }
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    public TimeUnitFormat() {
        this.mf = MeasureFormat.getInstance(ULocale.getDefault(), FormatWidth.WIDE);
        this.isReady = false;
        this.style = 0;
    }

    @Deprecated
    public TimeUnitFormat(ULocale locale) {
        this(locale, 0);
    }

    @Deprecated
    public TimeUnitFormat(Locale locale) {
        this(locale, 0);
    }

    @Deprecated
    public TimeUnitFormat(ULocale locale, int style) {
        if (style < 0 || style >= 2) {
            throw new IllegalArgumentException("style should be either FULL_NAME or ABBREVIATED_NAME style");
        }
        this.mf = MeasureFormat.getInstance(locale, style == 0 ? FormatWidth.WIDE : FormatWidth.SHORT);
        this.style = style;
        setLocale(locale, locale);
        this.locale = locale;
        this.isReady = false;
    }

    private TimeUnitFormat(ULocale locale, int style, NumberFormat numberFormat) {
        this(locale, style);
        if (numberFormat != null) {
            setNumberFormat((NumberFormat) numberFormat.clone());
        }
    }

    @Deprecated
    public TimeUnitFormat(Locale locale, int style) {
        this(ULocale.forLocale(locale), style);
    }

    @Deprecated
    public TimeUnitFormat setLocale(ULocale locale) {
        if (locale != this.locale) {
            this.mf = this.mf.withLocale(locale);
            setLocale(locale, locale);
            this.locale = locale;
            this.isReady = false;
        }
        return this;
    }

    @Deprecated
    public TimeUnitFormat setLocale(Locale locale) {
        return setLocale(ULocale.forLocale(locale));
    }

    @Deprecated
    public TimeUnitFormat setNumberFormat(NumberFormat format) {
        if (format == this.format) {
            return this;
        }
        if (format != null) {
            this.format = format;
            this.mf = this.mf.withNumberFormat(this.format);
        } else if (this.locale == null) {
            this.isReady = false;
            this.mf = this.mf.withLocale(ULocale.getDefault());
        } else {
            this.format = NumberFormat.getNumberInstance(this.locale);
            this.mf = this.mf.withNumberFormat(this.format);
        }
        return this;
    }

    @Deprecated
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        return this.mf.format(obj, toAppendTo, pos);
    }

    @Deprecated
    public TimeUnitAmount parseObject(String source, ParsePosition pos) {
        String str;
        TimeUnitFormat timeUnitFormat = this;
        ParsePosition parsePosition = pos;
        if (!timeUnitFormat.isReady) {
            setup();
        }
        Number resultNumber = null;
        TimeUnit resultTimeUnit = null;
        int oldPos = pos.getIndex();
        int newPos = -1;
        int longestParseDistance = 0;
        String countOfLongestMatch = null;
        Iterator it = timeUnitFormat.timeUnitToCountToPatterns.keySet().iterator();
        while (true) {
            int i = 2;
            int i2 = -1;
            if (!it.hasNext()) {
                break;
            }
            TimeUnit timeUnit = (TimeUnit) it.next();
            for (Entry<String, Object[]> patternEntry : ((Map) r1.timeUnitToCountToPatterns.get(timeUnit)).entrySet()) {
                Number resultNumber2;
                String count = (String) patternEntry.getKey();
                int newPos2 = newPos;
                TimeUnit resultTimeUnit2 = resultTimeUnit;
                Number resultNumber3 = resultNumber;
                int styl = 0;
                while (true) {
                    int styl2 = styl;
                    if (styl2 >= i) {
                        break;
                    }
                    MessageFormat pattern = ((Object[]) patternEntry.getValue())[styl2];
                    parsePosition.setErrorIndex(i2);
                    parsePosition.setIndex(oldPos);
                    Object parsed = pattern.parseObject(source, parsePosition);
                    resultNumber2 = resultNumber3;
                    if (pos.getErrorIndex() == -1 && pos.getIndex() != oldPos) {
                        if (((Object[]) parsed).length != 0) {
                            Number temp = null;
                            resultNumber3 = ((Object[]) parsed)[0];
                            if (resultNumber3 instanceof Number) {
                                resultNumber = resultNumber3;
                            } else {
                                try {
                                    resultNumber = timeUnitFormat.format.parse(resultNumber3.toString());
                                } catch (ParseException e) {
                                }
                            }
                        } else {
                            resultNumber = null;
                        }
                        int parseDistance = pos.getIndex() - oldPos;
                        if (parseDistance > longestParseDistance) {
                            resultNumber3 = resultNumber;
                            resultTimeUnit2 = timeUnit;
                            newPos2 = pos.getIndex();
                            longestParseDistance = parseDistance;
                            countOfLongestMatch = count;
                            styl = styl2 + 1;
                            timeUnitFormat = this;
                            i = 2;
                            i2 = -1;
                        }
                    }
                    resultNumber3 = resultNumber2;
                    styl = styl2 + 1;
                    timeUnitFormat = this;
                    i = 2;
                    i2 = -1;
                }
                str = source;
                resultNumber2 = resultNumber3;
                resultTimeUnit = resultTimeUnit2;
                newPos = newPos2;
                resultNumber = resultNumber2;
                timeUnitFormat = this;
                i = 2;
                i2 = -1;
            }
            str = source;
            timeUnitFormat = this;
        }
        str = source;
        if (resultNumber == null && longestParseDistance != 0) {
            if (countOfLongestMatch.equals(PluralRules.KEYWORD_ZERO)) {
                resultNumber = Integer.valueOf(0);
            } else if (countOfLongestMatch.equals(PluralRules.KEYWORD_ONE)) {
                resultNumber = Integer.valueOf(1);
            } else if (countOfLongestMatch.equals(PluralRules.KEYWORD_TWO)) {
                resultNumber = Integer.valueOf(2);
            } else {
                resultNumber = Integer.valueOf(3);
            }
        }
        if (longestParseDistance == 0) {
            parsePosition.setIndex(oldPos);
            parsePosition.setErrorIndex(0);
            return null;
        }
        parsePosition.setIndex(newPos);
        parsePosition.setErrorIndex(-1);
        return new TimeUnitAmount(resultNumber, resultTimeUnit);
    }

    private void setup() {
        if (this.locale == null) {
            if (this.format != null) {
                this.locale = this.format.getLocale(null);
            } else {
                this.locale = ULocale.getDefault(Category.FORMAT);
            }
            setLocale(this.locale, this.locale);
        }
        if (this.format == null) {
            this.format = NumberFormat.getNumberInstance(this.locale);
        }
        this.pluralRules = PluralRules.forLocale(this.locale);
        this.timeUnitToCountToPatterns = new HashMap();
        Set<String> pluralKeywords = this.pluralRules.getKeywords();
        setup("units/duration", this.timeUnitToCountToPatterns, 0, pluralKeywords);
        setup("unitsShort/duration", this.timeUnitToCountToPatterns, 1, pluralKeywords);
        this.isReady = true;
    }

    private void setup(String resourceKey, Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns, int style, Set<String> pluralKeywords) {
        String str;
        Map<TimeUnit, Map<String, Object[]>> map = timeUnitToCountToPatterns;
        int i = style;
        try {
            try {
                str = resourceKey;
                try {
                    ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, this.locale)).getAllItemsWithFallback(str, new TimeUnitFormatSetupSink(map, i, pluralKeywords, this.locale));
                } catch (MissingResourceException e) {
                }
            } catch (MissingResourceException e2) {
                str = resourceKey;
            }
        } catch (MissingResourceException e3) {
            str = resourceKey;
            Set<String> set = pluralKeywords;
        }
        TimeUnit[] timeUnits = TimeUnit.values();
        Set<String> keywords = this.pluralRules.getKeywords();
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < timeUnits.length) {
                TimeUnit timeUnit = timeUnits[i3];
                Map<String, Object[]> countToPatterns = (Map) map.get(timeUnit);
                if (countToPatterns == null) {
                    countToPatterns = new TreeMap();
                    map.put(timeUnit, countToPatterns);
                }
                Map<String, Object[]> countToPatterns2 = countToPatterns;
                Iterator it = keywords.iterator();
                while (it.hasNext()) {
                    Iterator it2;
                    Map<String, Object[]> countToPatterns3;
                    String str2 = (String) it.next();
                    if (countToPatterns2.get(str2) == null || ((Object[]) countToPatterns2.get(str2))[i] == null) {
                        it2 = it;
                        countToPatterns3 = countToPatterns2;
                        searchInTree(str, i, timeUnit, str2, str2, countToPatterns2);
                    } else {
                        it2 = it;
                        countToPatterns3 = countToPatterns2;
                    }
                    it = it2;
                    countToPatterns2 = countToPatterns3;
                }
                i2 = i3 + 1;
            } else {
                return;
            }
        }
    }

    private void searchInTree(String resourceKey, int styl, TimeUnit timeUnit, String srcPluralCount, String searchPluralCount, Map<String, Object[]> countToPatterns) {
        String str = resourceKey;
        TimeUnit timeUnit2 = timeUnit;
        String str2 = srcPluralCount;
        String str3 = searchPluralCount;
        Map<String, Object[]> map = countToPatterns;
        ULocale parentLocale = this.locale;
        String srcTimeUnitName = timeUnit.toString();
        ULocale parentLocale2 = parentLocale;
        while (true) {
            String srcTimeUnitName2 = srcTimeUnitName;
            if (parentLocale2 != null) {
                try {
                    MessageFormat messageFormat = new MessageFormat(((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, parentLocale2)).getWithFallback(str).getWithFallback(srcTimeUnitName2).getStringWithFallback(str3), this.locale);
                    Object[] pair = (Object[]) map.get(str2);
                    if (pair == null) {
                        pair = new Object[2];
                        map.put(str2, pair);
                    }
                    pair[styl] = messageFormat;
                    return;
                } catch (MissingResourceException e) {
                    parentLocale2 = parentLocale2.getFallback();
                    srcTimeUnitName = srcTimeUnitName2;
                }
            } else {
                if (parentLocale2 == null && str.equals("unitsShort")) {
                    searchInTree("units", styl, timeUnit2, str2, str3, map);
                    if (!(map.get(str2) == null || ((Object[]) map.get(str2))[styl] == null)) {
                        return;
                    }
                }
                if (str3.equals(PluralRules.KEYWORD_OTHER)) {
                    MessageFormat messageFormat2 = null;
                    if (timeUnit2 == TimeUnit.SECOND) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_SECOND, this.locale);
                    } else if (timeUnit2 == TimeUnit.MINUTE) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_MINUTE, this.locale);
                    } else if (timeUnit2 == TimeUnit.HOUR) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_HOUR, this.locale);
                    } else if (timeUnit2 == TimeUnit.WEEK) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_WEEK, this.locale);
                    } else if (timeUnit2 == TimeUnit.DAY) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_DAY, this.locale);
                    } else if (timeUnit2 == TimeUnit.MONTH) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_MONTH, this.locale);
                    } else if (timeUnit2 == TimeUnit.YEAR) {
                        messageFormat2 = new MessageFormat(DEFAULT_PATTERN_FOR_YEAR, this.locale);
                    }
                    Object[] pair2 = (Object[]) map.get(str2);
                    if (pair2 == null) {
                        pair2 = new Object[2];
                        map.put(str2, pair2);
                    }
                    pair2[styl] = messageFormat2;
                } else {
                    searchInTree(str, styl, timeUnit2, str2, PluralRules.KEYWORD_OTHER, map);
                }
                return;
            }
        }
    }

    @Deprecated
    public StringBuilder formatMeasures(StringBuilder appendTo, FieldPosition fieldPosition, Measure... measures) {
        return this.mf.formatMeasures(appendTo, fieldPosition, measures);
    }

    @Deprecated
    public FormatWidth getWidth() {
        return this.mf.getWidth();
    }

    @Deprecated
    public NumberFormat getNumberFormat() {
        return this.mf.getNumberFormat();
    }

    @Deprecated
    public Object clone() {
        TimeUnitFormat result = (TimeUnitFormat) super.clone();
        result.format = (NumberFormat) this.format.clone();
        return result;
    }

    private Object writeReplace() throws ObjectStreamException {
        return this.mf.toTimeUnitProxy();
    }

    private Object readResolve() throws ObjectStreamException {
        return new TimeUnitFormat(this.locale, this.style, this.format);
    }
}
