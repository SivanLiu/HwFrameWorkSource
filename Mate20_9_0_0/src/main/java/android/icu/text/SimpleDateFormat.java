package android.icu.text;

import android.icu.impl.DateNumberFormat;
import android.icu.impl.DayPeriodRules;
import android.icu.impl.DayPeriodRules.DayPeriod;
import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.lang.UCharacter;
import android.icu.text.DateFormat.BooleanAttribute;
import android.icu.text.DateFormat.Field;
import android.icu.text.DisplayContext.Type;
import android.icu.text.TimeZoneFormat.Style;
import android.icu.text.TimeZoneFormat.TimeType;
import android.icu.util.BasicTimeZone;
import android.icu.util.Calendar;
import android.icu.util.Calendar.FormatConfiguration;
import android.icu.util.HebrewCalendar;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneTransition;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.UUID;

public class SimpleDateFormat extends DateFormat {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int[] CALENDAR_FIELD_TO_LEVEL = new int[]{0, 10, 20, 20, 30, 30, 20, 30, 30, 40, 50, 50, 60, 70, 80, 0, 0, 10, 30, 10, 0, 40, 0, 0};
    static final UnicodeSet DATE_PATTERN_TYPE = new UnicodeSet("[GyYuUQqMLlwWd]").freeze();
    private static final int DECIMAL_BUF_SIZE = 10;
    static boolean DelayedHebrewMonthCheck = false;
    private static final String FALLBACKPATTERN = "yy/MM/dd HH:mm";
    private static final int HEBREW_CAL_CUR_MILLENIUM_END_YEAR = 6000;
    private static final int HEBREW_CAL_CUR_MILLENIUM_START_YEAR = 5000;
    private static final int ISOSpecialEra = -32000;
    private static final String NUMERIC_FORMAT_CHARS = "ADdFgHhKkmrSsuWwYy";
    private static final String NUMERIC_FORMAT_CHARS2 = "ceLMQq";
    private static ICUCache<String, Object[]> PARSED_PATTERN_CACHE = new SimpleCache();
    private static final boolean[] PATTERN_CHAR_IS_SYNTAX = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false};
    private static final int[] PATTERN_CHAR_TO_INDEX = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 22, 36, -1, 10, 9, 11, 0, 5, -1, -1, 16, 26, 2, -1, 31, -1, 27, -1, 8, -1, 30, 29, 13, 32, 18, 23, -1, -1, -1, -1, -1, -1, 14, 35, 25, 3, 19, -1, 21, 15, -1, -1, 4, -1, 6, -1, -1, -1, 28, 34, 7, -1, 20, 24, 12, 33, 1, 17, -1, -1, -1, -1, -1};
    private static final int[] PATTERN_CHAR_TO_LEVEL = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 40, -1, -1, 20, 30, 30, 0, 50, -1, -1, 50, 20, 20, -1, 0, -1, 20, -1, 80, -1, 10, 0, 30, 0, 10, 0, -1, -1, -1, -1, -1, -1, 40, -1, 30, 30, 30, -1, 0, 50, -1, -1, 50, -1, 60, -1, -1, -1, 20, 10, 70, -1, 10, 0, 20, 0, 10, 0, -1, -1, -1, -1, -1};
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD = new int[]{0, 1, 2, 5, 11, 11, 12, 13, 14, 7, 6, 8, 3, 4, 9, 10, 10, 15, 17, 18, 19, 20, 21, 15, 15, 18, 2, 2, 2, 15, 1, 15, 15, 15, 19, -1, -2};
    private static final Field[] PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE = new Field[]{Field.ERA, Field.YEAR, Field.MONTH, Field.DAY_OF_MONTH, Field.HOUR_OF_DAY1, Field.HOUR_OF_DAY0, Field.MINUTE, Field.SECOND, Field.MILLISECOND, Field.DAY_OF_WEEK, Field.DAY_OF_YEAR, Field.DAY_OF_WEEK_IN_MONTH, Field.WEEK_OF_YEAR, Field.WEEK_OF_MONTH, Field.AM_PM, Field.HOUR1, Field.HOUR0, Field.TIME_ZONE, Field.YEAR_WOY, Field.DOW_LOCAL, Field.EXTENDED_YEAR, Field.JULIAN_DAY, Field.MILLISECONDS_IN_DAY, Field.TIME_ZONE, Field.TIME_ZONE, Field.DAY_OF_WEEK, Field.MONTH, Field.QUARTER, Field.QUARTER, Field.TIME_ZONE, Field.YEAR, Field.TIME_ZONE, Field.TIME_ZONE, Field.TIME_ZONE, Field.RELATED_YEAR, Field.AM_PM_MIDNIGHT_NOON, Field.FLEXIBLE_DAY_PERIOD, Field.TIME_SEPARATOR};
    private static final int[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37};
    private static final String SUPPRESS_NEGATIVE_PREFIX = "꬀";
    private static ULocale cachedDefaultLocale = null;
    private static String cachedDefaultPattern = null;
    static final int currentSerialVersion = 2;
    private static final int millisPerHour = 3600000;
    private static final long serialVersionUID = 4774881970558875024L;
    private transient BreakIterator capitalizationBrkIter;
    private transient char[] decDigits;
    private transient char[] decimalBuf;
    private transient long defaultCenturyBase;
    private Date defaultCenturyStart;
    private transient int defaultCenturyStartYear;
    private DateFormatSymbols formatData;
    private transient boolean hasMinute;
    private transient boolean hasSecond;
    private transient ULocale locale;
    private HashMap<String, NumberFormat> numberFormatters;
    private String override;
    private HashMap<Character, String> overrideMap;
    private String pattern;
    private transient Object[] patternItems;
    private int serialVersionOnStream;
    private volatile TimeZoneFormat tzFormat;
    private transient boolean useFastFormat;
    private transient boolean useLocalZeroPaddingNumberFormat;

    private enum ContextValue {
        UNKNOWN,
        CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE,
        CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
        CAPITALIZATION_FOR_UI_LIST_OR_MENU,
        CAPITALIZATION_FOR_STANDALONE
    }

    private static class PatternItem {
        final boolean isNumeric;
        final int length;
        final char type;

        PatternItem(char type, int length) {
            this.type = type;
            this.length = length;
            this.isNumeric = SimpleDateFormat.isNumeric(type, length);
        }
    }

    private static int getLevelFromChar(char ch) {
        return ch < PATTERN_CHAR_TO_LEVEL.length ? PATTERN_CHAR_TO_LEVEL[ch & 255] : -1;
    }

    private static boolean isSyntaxChar(char ch) {
        return ch < PATTERN_CHAR_IS_SYNTAX.length ? PATTERN_CHAR_IS_SYNTAX[ch & 255] : false;
    }

    public SimpleDateFormat() {
        this(getDefaultPattern(), null, null, null, null, true, null);
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, null, null, null, null, true, null);
    }

    public SimpleDateFormat(String pattern, Locale loc) {
        this(pattern, null, null, null, ULocale.forLocale(loc), true, null);
    }

    public SimpleDateFormat(String pattern, ULocale loc) {
        this(pattern, null, null, null, loc, true, null);
    }

    public SimpleDateFormat(String pattern, String override, ULocale loc) {
        this(pattern, null, null, null, loc, false, override);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatData) {
        this(pattern, (DateFormatSymbols) formatData.clone(), null, null, null, true, null);
    }

    @Deprecated
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData, ULocale loc) {
        this(pattern, (DateFormatSymbols) formatData.clone(), null, null, loc, true, null);
    }

    SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar, ULocale locale, boolean useFastFormat, String override) {
        this(pattern, (DateFormatSymbols) formatData.clone(), (Calendar) calendar.clone(), null, locale, useFastFormat, override);
    }

    private SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar, NumberFormat numberFormat, ULocale locale, boolean useFastFormat, String override) {
        this.serialVersionOnStream = 2;
        this.capitalizationBrkIter = null;
        this.pattern = pattern;
        this.formatData = formatData;
        this.calendar = calendar;
        this.numberFormat = numberFormat;
        this.locale = locale;
        this.useFastFormat = useFastFormat;
        this.override = override;
        initialize();
    }

    @Deprecated
    public static SimpleDateFormat getInstance(FormatConfiguration formatConfig) {
        String ostr = formatConfig.getOverrideString();
        boolean z = ostr != null && ostr.length() > 0;
        return new SimpleDateFormat(formatConfig.getPatternString(), formatConfig.getDateFormatSymbols(), formatConfig.getCalendar(), null, formatConfig.getLocale(), z, formatConfig.getOverrideString());
    }

    private void initialize() {
        if (this.locale == null) {
            this.locale = ULocale.getDefault(Category.FORMAT);
        }
        if (this.formatData == null) {
            this.formatData = new DateFormatSymbols(this.locale);
        }
        if (this.calendar == null) {
            this.calendar = Calendar.getInstance(this.locale);
        }
        if (this.numberFormat == null) {
            NumberingSystem ns = NumberingSystem.getInstance(this.locale);
            String digitString = ns.getDescription();
            if (ns.isAlgorithmic() || digitString.length() != 10) {
                this.numberFormat = NumberFormat.getInstance(this.locale);
            } else {
                this.numberFormat = new DateNumberFormat(this.locale, digitString, ns.getName());
            }
        }
        if (this.numberFormat instanceof DecimalFormat) {
            DateFormat.fixNumberFormatForDates(this.numberFormat);
        }
        this.defaultCenturyBase = System.currentTimeMillis();
        setLocale(this.calendar.getLocale(ULocale.VALID_LOCALE), this.calendar.getLocale(ULocale.ACTUAL_LOCALE));
        initLocalZeroPaddingNumberFormat();
        if (this.override != null) {
            initNumberFormatters(this.locale);
        }
        parsePattern();
    }

    private synchronized void initializeTimeZoneFormat(boolean bForceUpdate) {
        if (!bForceUpdate) {
            try {
                if (this.tzFormat == null) {
                }
            } finally {
            }
        }
        this.tzFormat = TimeZoneFormat.getInstance(this.locale);
        String digits = null;
        if (this.numberFormat instanceof DecimalFormat) {
            String[] strDigits = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getDigitStringsLocal();
            StringBuilder digitsBuf = new StringBuilder();
            for (String digit : strDigits) {
                digitsBuf.append(digit);
            }
            digits = digitsBuf.toString();
        } else if (this.numberFormat instanceof DateNumberFormat) {
            digits = new String(((DateNumberFormat) this.numberFormat).getDigits());
        }
        if (!(digits == null || this.tzFormat.getGMTOffsetDigits().equals(digits))) {
            if (this.tzFormat.isFrozen()) {
                this.tzFormat = this.tzFormat.cloneAsThawed();
            }
            this.tzFormat.setGMTOffsetDigits(digits);
        }
    }

    private TimeZoneFormat tzFormat() {
        if (this.tzFormat == null) {
            initializeTimeZoneFormat(false);
        }
        return this.tzFormat;
    }

    private static synchronized String getDefaultPattern() {
        String str;
        synchronized (SimpleDateFormat.class) {
            ULocale defaultLocale = ULocale.getDefault(Category.FORMAT);
            if (!defaultLocale.equals(cachedDefaultLocale)) {
                cachedDefaultLocale = defaultLocale;
                Calendar cal = Calendar.getInstance(cachedDefaultLocale);
                try {
                    ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, cachedDefaultLocale);
                    String resourcePath = new StringBuilder();
                    resourcePath.append("calendar/");
                    resourcePath.append(cal.getType());
                    resourcePath.append("/DateTimePatterns");
                    ICUResourceBundle patternsRb = rb.findWithFallback(resourcePath.toString());
                    if (patternsRb == null) {
                        patternsRb = rb.findWithFallback("calendar/gregorian/DateTimePatterns");
                    }
                    if (patternsRb != null) {
                        if (patternsRb.getSize() >= 9) {
                            int defaultIndex = 8;
                            if (patternsRb.getSize() >= 13) {
                                defaultIndex = 8 + 4;
                            }
                            cachedDefaultPattern = SimpleFormatterImpl.formatRawPattern(patternsRb.getString(defaultIndex), 2, 2, patternsRb.getString(3), patternsRb.getString(7));
                        }
                    }
                    cachedDefaultPattern = FALLBACKPATTERN;
                } catch (MissingResourceException e) {
                    cachedDefaultPattern = FALLBACKPATTERN;
                }
            }
            str = cachedDefaultPattern;
        }
        return str;
    }

    private void parseAmbiguousDatesAsAfter(Date startDate) {
        this.defaultCenturyStart = startDate;
        this.calendar.setTime(startDate);
        this.defaultCenturyStartYear = this.calendar.get(1);
    }

    private void initializeDefaultCenturyStart(long baseTime) {
        this.defaultCenturyBase = baseTime;
        Calendar tmpCal = (Calendar) this.calendar.clone();
        tmpCal.setTimeInMillis(baseTime);
        tmpCal.add(1, -80);
        this.defaultCenturyStart = tmpCal.getTime();
        this.defaultCenturyStartYear = tmpCal.get(1);
    }

    private Date getDefaultCenturyStart() {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        return this.defaultCenturyStart;
    }

    private int getDefaultCenturyStartYear() {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        return this.defaultCenturyStartYear;
    }

    public void set2DigitYearStart(Date startDate) {
        parseAmbiguousDatesAsAfter(startDate);
    }

    public Date get2DigitYearStart() {
        return getDefaultCenturyStart();
    }

    public void setContext(DisplayContext context) {
        super.setContext(context);
        if (this.capitalizationBrkIter != null) {
            return;
        }
        if (context == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || context == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || context == DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
        }
    }

    public StringBuffer format(Calendar cal, StringBuffer toAppendTo, FieldPosition pos) {
        TimeZone backupTZ = null;
        if (!(cal == this.calendar || cal.getType().equals(this.calendar.getType()))) {
            this.calendar.setTimeInMillis(cal.getTimeInMillis());
            backupTZ = this.calendar.getTimeZone();
            this.calendar.setTimeZone(cal.getTimeZone());
            cal = this.calendar;
        }
        StringBuffer result = format(cal, getContext(Type.CAPITALIZATION), toAppendTo, pos, null);
        if (backupTZ != null) {
            this.calendar.setTimeZone(backupTZ);
        }
        return result;
    }

    private StringBuffer format(Calendar cal, DisplayContext capitalizationContext, StringBuffer toAppendTo, FieldPosition pos, List<FieldPosition> attributes) {
        StringBuffer stringBuffer = toAppendTo;
        FieldPosition fieldPosition = pos;
        List<FieldPosition> list = attributes;
        int i = 0;
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        Object[] items = getPatternItems();
        while (true) {
            int i2 = i;
            if (i2 < items.length) {
                Object[] items2;
                if (items[i2] instanceof String) {
                    stringBuffer.append((String) items[i2]);
                    items2 = items;
                } else {
                    PatternItem item = items[i2];
                    i = 0;
                    if (list != null) {
                        i = toAppendTo.length();
                    }
                    int start = i;
                    if (this.useFastFormat) {
                        items2 = items;
                        items = start;
                        subFormat(stringBuffer, item.type, item.length, toAppendTo.length(), i2, capitalizationContext, fieldPosition, cal);
                    } else {
                        items2 = items;
                        items = start;
                        stringBuffer.append(subFormat(item.type, item.length, toAppendTo.length(), i2, capitalizationContext, fieldPosition, cal));
                    }
                    if (list != null) {
                        i = toAppendTo.length();
                        if (i - items > 0) {
                            FieldPosition fp = new FieldPosition(patternCharToDateFormatField(item.type));
                            fp.setBeginIndex(items);
                            fp.setEndIndex(i);
                            list.add(fp);
                        }
                    }
                }
                i = i2 + 1;
                items = items2;
            } else {
                return stringBuffer;
            }
        }
    }

    private static int getIndexFromChar(char ch) {
        return ch < PATTERN_CHAR_TO_INDEX.length ? PATTERN_CHAR_TO_INDEX[ch & 255] : -1;
    }

    protected Field patternCharToDateFormatField(char ch) {
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex != -1) {
            return PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[patternCharIndex];
        }
        return null;
    }

    protected String subFormat(char ch, int count, int beginOffset, FieldPosition pos, DateFormatSymbols fmtData, Calendar cal) throws IllegalArgumentException {
        return subFormat(ch, count, beginOffset, 0, DisplayContext.CAPITALIZATION_NONE, pos, cal);
    }

    @Deprecated
    protected String subFormat(char ch, int count, int beginOffset, int fieldNum, DisplayContext capitalizationContext, FieldPosition pos, Calendar cal) {
        StringBuffer buf = new StringBuffer();
        subFormat(buf, ch, count, beginOffset, fieldNum, capitalizationContext, pos, cal);
        return buf.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:268:0x0687  */
    /* JADX WARNING: Removed duplicated region for block: B:264:0x067f  */
    /* JADX WARNING: Missing block: B:138:0x0306, code skipped:
            r17 = r0;
            r38 = r6;
            r0 = r20;
            r25 = r27;
            r1 = r30;
     */
    /* JADX WARNING: Missing block: B:203:0x04de, code skipped:
            r38 = r6;
            r25 = r27;
            r6 = r35;
     */
    /* JADX WARNING: Missing block: B:211:0x0518, code skipped:
            r17 = r0;
            r38 = r6;
            r0 = r20;
            r25 = r27;
            r1 = r35;
     */
    /* JADX WARNING: Missing block: B:228:0x059a, code skipped:
            if (r12 != 5) goto L_0x05ad;
     */
    /* JADX WARNING: Missing block: B:229:0x059c, code skipped:
            safeAppend(r9.formatData.narrowWeekdays, r1, r10);
            r20 = android.icu.text.DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
     */
    /* JADX WARNING: Missing block: B:230:0x05a5, code skipped:
            r38 = r6;
            r0 = r20;
            r25 = r27;
     */
    /* JADX WARNING: Missing block: B:231:0x05ad, code skipped:
            if (r12 != 4) goto L_0x05b9;
     */
    /* JADX WARNING: Missing block: B:232:0x05af, code skipped:
            safeAppend(r9.formatData.weekdays, r1, r10);
            r20 = android.icu.text.DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
     */
    /* JADX WARNING: Missing block: B:234:0x05ba, code skipped:
            if (r12 != 6) goto L_0x05cc;
     */
    /* JADX WARNING: Missing block: B:236:0x05c0, code skipped:
            if (r9.formatData.shorterWeekdays == null) goto L_0x05cc;
     */
    /* JADX WARNING: Missing block: B:237:0x05c2, code skipped:
            safeAppend(r9.formatData.shorterWeekdays, r1, r10);
            r20 = android.icu.text.DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
     */
    /* JADX WARNING: Missing block: B:238:0x05cc, code skipped:
            safeAppend(r9.formatData.shortWeekdays, r1, r10);
            r20 = android.icu.text.DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
     */
    /* JADX WARNING: Missing block: B:327:0x0794, code skipped:
            if (r9.override == null) goto L_0x07b6;
     */
    /* JADX WARNING: Missing block: B:329:0x079e, code skipped:
            if (r9.override.compareTo("hebr") == 0) goto L_0x07aa;
     */
    /* JADX WARNING: Missing block: B:331:0x07a8, code skipped:
            if (r9.override.indexOf("y=hebr") < 0) goto L_0x07b6;
     */
    /* JADX WARNING: Missing block: B:333:0x07ac, code skipped:
            if (r3 <= HEBREW_CAL_CUR_MILLENIUM_START_YEAR) goto L_0x07b6;
     */
    /* JADX WARNING: Missing block: B:335:0x07b0, code skipped:
            if (r3 >= HEBREW_CAL_CUR_MILLENIUM_END_YEAR) goto L_0x07b6;
     */
    /* JADX WARNING: Missing block: B:336:0x07b2, code skipped:
            r6 = r3 - 5000;
     */
    /* JADX WARNING: Missing block: B:337:0x07b6, code skipped:
            r6 = r3;
     */
    /* JADX WARNING: Missing block: B:338:0x07b7, code skipped:
            if (r12 != 2) goto L_0x07c5;
     */
    /* JADX WARNING: Missing block: B:339:0x07b9, code skipped:
            zeroPaddingNumber(r18, r10, r6, 2, 2);
     */
    /* JADX WARNING: Missing block: B:340:0x07c5, code skipped:
            zeroPaddingNumber(r18, r10, r6, r12, Integer.MAX_VALUE);
     */
    /* JADX WARNING: Missing block: B:355:0x0827, code skipped:
            if (r46 != 0) goto L_0x0890;
     */
    /* JADX WARNING: Missing block: B:356:0x0829, code skipped:
            if (r14 == null) goto L_0x0890;
     */
    /* JADX WARNING: Missing block: B:357:0x082b, code skipped:
            r3 = r32;
     */
    /* JADX WARNING: Missing block: B:358:0x0835, code skipped:
            if (android.icu.lang.UCharacter.isLowerCase(r10.codePointAt(r3)) == false) goto L_0x088b;
     */
    /* JADX WARNING: Missing block: B:359:0x0837, code skipped:
            r4 = false;
     */
    /* JADX WARNING: Missing block: B:360:0x0840, code skipped:
            switch(r47) {
                case android.icu.text.DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE :android.icu.text.DisplayContext: goto L_0x0861;
                case android.icu.text.DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU :android.icu.text.DisplayContext: goto L_0x0844;
                case android.icu.text.DisplayContext.CAPITALIZATION_FOR_STANDALONE :android.icu.text.DisplayContext: goto L_0x0844;
                default: goto L_0x0843;
            };
     */
    /* JADX WARNING: Missing block: B:362:0x0848, code skipped:
            if (r9.formatData.capitalization == null) goto L_0x0863;
     */
    /* JADX WARNING: Missing block: B:363:0x084a, code skipped:
            r5 = (boolean[]) r9.formatData.capitalization.get(r0);
     */
    /* JADX WARNING: Missing block: B:364:0x0856, code skipped:
            if (r14 != android.icu.text.DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU) goto L_0x085c;
     */
    /* JADX WARNING: Missing block: B:365:0x0858, code skipped:
            r6 = r5[0];
     */
    /* JADX WARNING: Missing block: B:366:0x085c, code skipped:
            r6 = r5[1];
     */
    /* JADX WARNING: Missing block: B:367:0x085f, code skipped:
            r4 = r6;
     */
    /* JADX WARNING: Missing block: B:368:0x0861, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:369:0x0863, code skipped:
            if (r4 == false) goto L_0x088b;
     */
    /* JADX WARNING: Missing block: B:371:0x0867, code skipped:
            if (r9.capitalizationBrkIter != null) goto L_0x0871;
     */
    /* JADX WARNING: Missing block: B:372:0x0869, code skipped:
            r9.capitalizationBrkIter = android.icu.text.BreakIterator.getSentenceInstance(r9.locale);
     */
    /* JADX WARNING: Missing block: B:373:0x0871, code skipped:
            r39 = r0;
            r40 = r1;
            r10.replace(r3, r42.length(), android.icu.lang.UCharacter.toTitleCase(r9.locale, r10.substring(r3), r9.capitalizationBrkIter, (int) android.icu.impl.coll.CollationSettings.CASE_FIRST_AND_UPPER_MASK));
     */
    /* JADX WARNING: Missing block: B:374:0x088b, code skipped:
            r39 = r0;
            r40 = r1;
     */
    /* JADX WARNING: Missing block: B:375:0x0890, code skipped:
            r39 = r0;
            r40 = r1;
            r3 = r32;
     */
    /* JADX WARNING: Missing block: B:377:0x089e, code skipped:
            if (r48.getBeginIndex() != r48.getEndIndex()) goto L_0x08cd;
     */
    /* JADX WARNING: Missing block: B:379:0x08a8, code skipped:
            if (r48.getField() != PATTERN_INDEX_TO_DATE_FORMAT_FIELD[r25]) goto L_0x08b7;
     */
    /* JADX WARNING: Missing block: B:380:0x08aa, code skipped:
            r15.setBeginIndex(r13);
            r15.setEndIndex((r42.length() + r13) - r3);
     */
    /* JADX WARNING: Missing block: B:382:0x08bf, code skipped:
            if (r48.getFieldAttribute() != PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[r25]) goto L_0x08cd;
     */
    /* JADX WARNING: Missing block: B:383:0x08c1, code skipped:
            r15.setBeginIndex(r13);
            r15.setEndIndex((r42.length() + r13) - r3);
     */
    /* JADX WARNING: Missing block: B:384:0x08cd, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    protected void subFormat(StringBuffer buf, char ch, int count, int beginOffset, int fieldNum, DisplayContext capitalizationContext, FieldPosition pos, Calendar cal) {
        StringBuffer stringBuffer = buf;
        char c = ch;
        int i = count;
        int i2 = beginOffset;
        DisplayContext displayContext = capitalizationContext;
        FieldPosition fieldPosition = pos;
        Calendar calendar = cal;
        int bufstart = buf.length();
        TimeZone tz = cal.getTimeZone();
        long date = cal.getTimeInMillis();
        String result = null;
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex != -1) {
            int value;
            int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
            if (field >= 0) {
                value = patternCharIndex != 34 ? calendar.get(field) : cal.getRelatedYear();
            } else {
                value = 0;
            }
            NumberFormat currentNumberFormat = getNumberFormat(c);
            CapitalizationContextUsage capContextUsageType = CapitalizationContextUsage.OTHER;
            long date2 = date;
            int bufstart2;
            Calendar calendar2;
            int patternCharIndex2;
            int value2;
            int value3;
            int patternCharIndex3;
            int i3;
            int i4;
            int isLeapMonth;
            TimeZone timeZone;
            CapitalizationContextUsage capContextUsageType2;
            int value4;
            int value5;
            long bufstart3;
            int value6;
            int value7;
            TimeZone tz2;
            long date3;
            String toAppend;
            switch (patternCharIndex) {
                case 0:
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    patternCharIndex2 = patternCharIndex;
                    patternCharIndex = value;
                    if (cal.getType().equals("chinese") || cal.getType().equals("dangi")) {
                        value2 = patternCharIndex;
                        zeroPaddingNumber(currentNumberFormat, stringBuffer, patternCharIndex, 1, 9);
                    } else {
                        if (i == 5) {
                            safeAppend(this.formatData.narrowEras, patternCharIndex, stringBuffer);
                            capContextUsageType = CapitalizationContextUsage.ERA_NARROW;
                        } else if (i == 4) {
                            safeAppend(this.formatData.eraNames, patternCharIndex, stringBuffer);
                            capContextUsageType = CapitalizationContextUsage.ERA_WIDE;
                        } else {
                            safeAppend(this.formatData.eras, patternCharIndex, stringBuffer);
                            capContextUsageType = CapitalizationContextUsage.ERA_ABBREV;
                        }
                        value = patternCharIndex;
                    }
                    break;
                case 1:
                case 18:
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    patternCharIndex2 = patternCharIndex;
                    patternCharIndex = value;
                    break;
                case 2:
                case 26:
                    value3 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    if (cal.getType().equals("hebrew")) {
                        boolean isLeap = HebrewCalendar.isLeapYear(calendar2.get(1));
                        if (isLeap) {
                            patternCharIndex = value3;
                            if (patternCharIndex == 6 && i >= 3) {
                                value = 13;
                                if (isLeap) {
                                    patternCharIndex = 6;
                                    if (value >= 6 && i < 3) {
                                        value--;
                                    }
                                } else {
                                    patternCharIndex = 6;
                                }
                                i3 = patternCharIndex;
                                patternCharIndex = value;
                            }
                        } else {
                            patternCharIndex = value3;
                        }
                        value = patternCharIndex;
                        if (isLeap) {
                        }
                        i3 = patternCharIndex;
                        patternCharIndex = value;
                    } else {
                        patternCharIndex = value3;
                        i3 = 6;
                    }
                    i4 = (this.formatData.leapMonthPatterns == null || this.formatData.leapMonthPatterns.length < 7) ? 0 : calendar2.get(22);
                    isLeapMonth = i4;
                    String str = null;
                    String[] strArr;
                    if (i != 5) {
                        value = patternCharIndex3;
                        if (i != 4) {
                            if (i != 3) {
                                StringBuffer monthNumber = new StringBuffer();
                                patternCharIndex2 = value;
                                timeZone = tz;
                                tz = null;
                                int value8 = patternCharIndex;
                                StringBuffer tz3 = monthNumber;
                                zeroPaddingNumber(currentNumberFormat, monthNumber, patternCharIndex + 1, i, Integer.MAX_VALUE);
                                String[] monthNumberStrings = new String[]{tz3.toString()};
                                if (isLeapMonth != 0) {
                                    str = this.formatData.leapMonthPatterns[i3];
                                }
                                safeAppendWithMonthPattern(monthNumberStrings, 0, stringBuffer, str);
                                capContextUsageType2 = capContextUsageType;
                                value = value8;
                                break;
                            } else if (value == 2) {
                                String[] strArr2 = this.formatData.shortMonths;
                                if (isLeapMonth != 0) {
                                    str = this.formatData.leapMonthPatterns[1];
                                }
                                safeAppendWithMonthPattern(strArr2, patternCharIndex, stringBuffer, str);
                                capContextUsageType = CapitalizationContextUsage.MONTH_FORMAT;
                            } else {
                                String[] strArr3 = this.formatData.standaloneShortMonths;
                                if (isLeapMonth != 0) {
                                    str = this.formatData.leapMonthPatterns[4];
                                }
                                safeAppendWithMonthPattern(strArr3, patternCharIndex, stringBuffer, str);
                                capContextUsageType = CapitalizationContextUsage.MONTH_STANDALONE;
                            }
                        } else if (value == 2) {
                            strArr = this.formatData.months;
                            if (isLeapMonth != 0) {
                                str = this.formatData.leapMonthPatterns[0];
                            }
                            safeAppendWithMonthPattern(strArr, patternCharIndex, stringBuffer, str);
                            capContextUsageType = CapitalizationContextUsage.MONTH_FORMAT;
                        } else {
                            strArr = this.formatData.standaloneMonths;
                            if (isLeapMonth != 0) {
                                str = this.formatData.leapMonthPatterns[3];
                            }
                            safeAppendWithMonthPattern(strArr, patternCharIndex, stringBuffer, str);
                            capContextUsageType = CapitalizationContextUsage.MONTH_STANDALONE;
                        }
                    } else {
                        value = patternCharIndex3;
                        if (value == 2) {
                            strArr = this.formatData.narrowMonths;
                            if (isLeapMonth != 0) {
                                str = this.formatData.leapMonthPatterns[2];
                            }
                            safeAppendWithMonthPattern(strArr, patternCharIndex, stringBuffer, str);
                        } else {
                            capContextUsageType2 = this.formatData.standaloneNarrowMonths;
                            if (isLeapMonth != 0) {
                                str = this.formatData.leapMonthPatterns[5];
                            }
                            safeAppendWithMonthPattern(capContextUsageType2, patternCharIndex, stringBuffer, str);
                        }
                        capContextUsageType = CapitalizationContextUsage.MONTH_NARROW;
                    }
                    patternCharIndex2 = value;
                    value = patternCharIndex;
                    break;
                case 4:
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    patternCharIndex = value;
                    if (patternCharIndex == 0) {
                        value3 = patternCharIndex;
                        zeroPaddingNumber(currentNumberFormat, stringBuffer, calendar2.getMaximum(11) + 1, i, Integer.MAX_VALUE);
                    } else {
                        value3 = patternCharIndex;
                        zeroPaddingNumber(currentNumberFormat, stringBuffer, patternCharIndex, i, Integer.MAX_VALUE);
                    }
                    patternCharIndex2 = patternCharIndex3;
                    value2 = value3;
                    value = value2;
                    capContextUsageType2 = capContextUsageType;
                    break;
                case 8:
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    patternCharIndex = value;
                    this.numberFormat.setMinimumIntegerDigits(Math.min(3, i));
                    this.numberFormat.setMaximumIntegerDigits(Integer.MAX_VALUE);
                    if (i == 1) {
                        value = patternCharIndex / 100;
                    } else if (i == 2) {
                        value = patternCharIndex / 10;
                    } else {
                        value = patternCharIndex;
                    }
                    FieldPosition p = new FieldPosition(-1);
                    this.numberFormat.format((long) value, stringBuffer, p);
                    if (i > 3) {
                        this.numberFormat.setMinimumIntegerDigits(i - 3);
                        this.numberFormat.format((long) 0, stringBuffer, p);
                        break;
                    }
                    break;
                case 9:
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    patternCharIndex = value;
                    break;
                case 14:
                    value4 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    if (i < 5) {
                        patternCharIndex = value4;
                    } else if (this.formatData.ampmsNarrow == null) {
                        patternCharIndex = value4;
                    } else {
                        patternCharIndex = value4;
                        safeAppend(this.formatData.ampmsNarrow, patternCharIndex, stringBuffer);
                    }
                    safeAppend(this.formatData.ampms, patternCharIndex, stringBuffer);
                case 15:
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    int value9 = value;
                    if (value9 == 0) {
                        value4 = value9;
                        zeroPaddingNumber(currentNumberFormat, stringBuffer, calendar2.getLeastMaximum(10) + 1, i, Integer.MAX_VALUE);
                    } else {
                        value4 = value9;
                        zeroPaddingNumber(currentNumberFormat, stringBuffer, value4, i, Integer.MAX_VALUE);
                    }
                    patternCharIndex2 = patternCharIndex3;
                    value2 = value4;
                    value = value2;
                    capContextUsageType2 = capContextUsageType;
                    break;
                case 17:
                    CapitalizationContextUsage capContextUsageType3;
                    value5 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart3 = date2;
                    if (i < 4) {
                        capContextUsageType2 = tzFormat().format(Style.SPECIFIC_SHORT, tz, bufstart3);
                        capContextUsageType3 = CapitalizationContextUsage.METAZONE_SHORT;
                    } else {
                        capContextUsageType2 = tzFormat().format(Style.SPECIFIC_LONG, tz, bufstart3);
                        capContextUsageType3 = CapitalizationContextUsage.METAZONE_LONG;
                    }
                    capContextUsageType = capContextUsageType3;
                    stringBuffer.append(capContextUsageType2);
                    break;
                case 19:
                    value5 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    if (i >= 3) {
                        value = calendar2.get(7);
                        break;
                    }
                    zeroPaddingNumber(currentNumberFormat, stringBuffer, value5, i, Integer.MAX_VALUE);
                case 23:
                    value5 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart3 = date2;
                    if (i < 4) {
                        capContextUsageType2 = tzFormat().format(Style.ISO_BASIC_LOCAL_FULL, tz, bufstart3);
                    } else if (i == 5) {
                        capContextUsageType2 = tzFormat().format(Style.ISO_EXTENDED_FULL, tz, bufstart3);
                    } else {
                        capContextUsageType2 = tzFormat().format(Style.LOCALIZED_GMT, tz, bufstart3);
                    }
                    stringBuffer.append(capContextUsageType2);
                    break;
                case 24:
                    value5 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart3 = date2;
                    if (i == 1) {
                        result = tzFormat().format(Style.GENERIC_SHORT, tz, bufstart3);
                        capContextUsageType = CapitalizationContextUsage.METAZONE_SHORT;
                    } else if (i == 4) {
                        result = tzFormat().format(Style.GENERIC_LONG, tz, bufstart3);
                        capContextUsageType = CapitalizationContextUsage.METAZONE_LONG;
                    }
                    capContextUsageType2 = result;
                    stringBuffer.append(capContextUsageType2);
                    break;
                case 25:
                    value5 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    if (i >= 3) {
                        value = calendar2.get(7);
                        if (i != 5) {
                            if (i != 4) {
                                if (i == 6 && this.formatData.standaloneShorterWeekdays != null) {
                                    safeAppend(this.formatData.standaloneShorterWeekdays, value, stringBuffer);
                                    capContextUsageType = CapitalizationContextUsage.DAY_STANDALONE;
                                    break;
                                }
                                safeAppend(this.formatData.standaloneShortWeekdays, value, stringBuffer);
                                capContextUsageType = CapitalizationContextUsage.DAY_STANDALONE;
                                break;
                            }
                            safeAppend(this.formatData.standaloneWeekdays, value, stringBuffer);
                            capContextUsageType = CapitalizationContextUsage.DAY_STANDALONE;
                            break;
                        }
                        safeAppend(this.formatData.standaloneNarrowWeekdays, value, stringBuffer);
                        capContextUsageType = CapitalizationContextUsage.DAY_NARROW;
                        break;
                    }
                    zeroPaddingNumber(currentNumberFormat, stringBuffer, value5, 1, Integer.MAX_VALUE);
                    break;
                case 27:
                    int value10;
                    value6 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    if (i >= 4) {
                        value10 = value6;
                        safeAppend(this.formatData.quarters, value10 / 3, stringBuffer);
                    } else {
                        value10 = value6;
                        if (i == 3) {
                            safeAppend(this.formatData.shortQuarters, value10 / 3, stringBuffer);
                        } else {
                            value5 = value10;
                            zeroPaddingNumber(currentNumberFormat, stringBuffer, (value10 / 3) + 1, i, Integer.MAX_VALUE);
                        }
                    }
                    patternCharIndex2 = patternCharIndex3;
                    value2 = value10;
                    value = value2;
                    capContextUsageType2 = capContextUsageType;
                    break;
                case 28:
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    patternCharIndex = value;
                    if (i < 4) {
                        if (i != 3) {
                            value6 = patternCharIndex;
                            zeroPaddingNumber(currentNumberFormat, stringBuffer, (patternCharIndex / 3) + 1, i, Integer.MAX_VALUE);
                            timeZone = tz;
                            patternCharIndex2 = patternCharIndex3;
                            value2 = value6;
                            value = value2;
                            capContextUsageType2 = capContextUsageType;
                            break;
                        }
                        safeAppend(this.formatData.standaloneShortQuarters, patternCharIndex / 3, stringBuffer);
                    } else {
                        safeAppend(this.formatData.standaloneQuarters, patternCharIndex / 3, stringBuffer);
                    }
                case 29:
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart3 = date2;
                    patternCharIndex = value;
                    if (i == 1) {
                        result = tzFormat().format(Style.ZONE_ID_SHORT, tz, bufstart3);
                    } else if (i == 2) {
                        result = tzFormat().format(Style.ZONE_ID, tz, bufstart3);
                    } else if (i == 3) {
                        result = tzFormat().format(Style.EXEMPLAR_LOCATION, tz, bufstart3);
                    } else if (i == 4) {
                        result = tzFormat().format(Style.GENERIC_LOCATION, tz, bufstart3);
                        capContextUsageType = CapitalizationContextUsage.ZONE_LONG;
                    }
                    String result2 = result;
                    stringBuffer.append(result2);
                    result = result2;
                    value = patternCharIndex;
                    break;
                case 30:
                    value7 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart = date2;
                    if (this.formatData.shortYearNames == null) {
                        timeZone = tz;
                        patternCharIndex2 = patternCharIndex3;
                        patternCharIndex = value7;
                        break;
                    }
                    patternCharIndex = value7;
                    if (patternCharIndex > this.formatData.shortYearNames.length) {
                        timeZone = tz;
                        patternCharIndex2 = patternCharIndex3;
                        break;
                    }
                    safeAppend(this.formatData.shortYearNames, patternCharIndex - 1, stringBuffer);
                    patternCharIndex2 = patternCharIndex3;
                    value2 = patternCharIndex;
                    value = value2;
                    capContextUsageType2 = capContextUsageType;
                    break;
                case 31:
                    value7 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart3 = date2;
                    if (i == 1) {
                        result = tzFormat().format(Style.LOCALIZED_GMT_SHORT, tz, bufstart3);
                    } else if (i == 4) {
                        result = tzFormat().format(Style.LOCALIZED_GMT, tz, bufstart3);
                    }
                    capContextUsageType2 = result;
                    stringBuffer.append(capContextUsageType2);
                    break;
                case 32:
                    value7 = value;
                    patternCharIndex3 = patternCharIndex;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    bufstart3 = date2;
                    if (i == 1) {
                        result = tzFormat().format(Style.ISO_BASIC_SHORT, tz, bufstart3);
                    } else if (i == 2) {
                        result = tzFormat().format(Style.ISO_BASIC_FIXED, tz, bufstart3);
                    } else if (i == 3) {
                        result = tzFormat().format(Style.ISO_EXTENDED_FIXED, tz, bufstart3);
                    } else if (i == 4) {
                        result = tzFormat().format(Style.ISO_BASIC_FULL, tz, bufstart3);
                    } else if (i == 5) {
                        result = tzFormat().format(Style.ISO_EXTENDED_FULL, tz, bufstart3);
                    }
                    capContextUsageType2 = result;
                    stringBuffer.append(capContextUsageType2);
                    break;
                case 33:
                    value7 = value;
                    patternCharIndex3 = patternCharIndex;
                    tz2 = tz;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    date3 = date2;
                    if (i == 1) {
                        tz = tz2;
                        result = tzFormat().format(Style.ISO_BASIC_LOCAL_SHORT, tz, date3);
                    } else {
                        bufstart3 = date3;
                        tz = tz2;
                        if (i == 2) {
                            result = tzFormat().format(Style.ISO_BASIC_LOCAL_FIXED, tz, bufstart3);
                        } else if (i == 3) {
                            result = tzFormat().format(Style.ISO_EXTENDED_LOCAL_FIXED, tz, bufstart3);
                        } else if (i == 4) {
                            result = tzFormat().format(Style.ISO_BASIC_LOCAL_FULL, tz, bufstart3);
                        } else if (i == 5) {
                            result = tzFormat().format(Style.ISO_EXTENDED_LOCAL_FULL, tz, bufstart3);
                        }
                    }
                    capContextUsageType2 = result;
                    stringBuffer.append(capContextUsageType2);
                    break;
                case 35:
                    value7 = value;
                    patternCharIndex3 = patternCharIndex;
                    tz2 = tz;
                    bufstart2 = bufstart;
                    date3 = date2;
                    calendar2 = cal;
                    int hour = calendar2.get(11);
                    value = 0;
                    if (hour == 12 && ((this.hasMinute == 0 || calendar2.get(12) == 0) && (!this.hasSecond || calendar2.get(13) == 0))) {
                        i4 = calendar2.get(9);
                        if (i <= 3) {
                            value = this.formatData.abbreviatedDayPeriods[i4];
                        } else if (i == 4 || i > 5) {
                            value = this.formatData.wideDayPeriods[i4];
                        } else {
                            value = this.formatData.narrowDayPeriods[i4];
                        }
                        value7 = i4;
                    }
                    toAppend = value;
                    if (toAppend == null) {
                        subFormat(stringBuffer, 'a', i, i2, fieldNum, displayContext, fieldPosition, calendar2);
                    } else {
                        i3 = hour;
                        stringBuffer.append(toAppend);
                    }
                    capContextUsageType2 = capContextUsageType;
                    patternCharIndex2 = patternCharIndex3;
                    bufstart = date3;
                    value = value7;
                    timeZone = tz2;
                    break;
                case 36:
                    DayPeriodRules ruleSet = DayPeriodRules.getInstance(getLocale());
                    if (ruleSet != null) {
                        DayPeriod periodType;
                        value7 = value;
                        patternCharIndex3 = patternCharIndex;
                        DayPeriodRules ruleSet2 = ruleSet;
                        tz2 = tz;
                        bufstart2 = bufstart;
                        date3 = date2;
                        calendar = cal;
                        toAppend = calendar.get(11);
                        value = 0;
                        patternCharIndex = 0;
                        if (this.hasMinute) {
                            value = calendar.get(12);
                        }
                        i3 = value;
                        if (this.hasSecond != 0) {
                            patternCharIndex = calendar.get(13);
                        }
                        isLeapMonth = patternCharIndex;
                        if (toAppend == null && i3 == 0 && isLeapMonth == 0 && ruleSet2.hasMidnight() != 0) {
                            periodType = DayPeriod.MIDNIGHT;
                        } else if (toAppend == 12 && i3 == 0 && isLeapMonth == 0 && ruleSet2.hasNoon()) {
                            periodType = DayPeriod.NOON;
                        } else {
                            periodType = ruleSet2.getDayPeriodForHour(toAppend);
                        }
                        value = 0;
                        if (!(periodType == DayPeriod.AM || periodType == DayPeriod.PM || periodType == DayPeriod.MIDNIGHT)) {
                            patternCharIndex = periodType.ordinal();
                            value = i <= 3 ? this.formatData.abbreviatedDayPeriods[patternCharIndex] : (i == 4 || i > 5) ? this.formatData.wideDayPeriods[patternCharIndex] : this.formatData.narrowDayPeriods[patternCharIndex];
                        }
                        if (value == 0 && (periodType == DayPeriod.MIDNIGHT || periodType == DayPeriod.NOON)) {
                            periodType = ruleSet2.getDayPeriodForHour(toAppend);
                            patternCharIndex = periodType.ordinal();
                            if (i <= 3) {
                                value = this.formatData.abbreviatedDayPeriods[patternCharIndex];
                            } else if (i == 4 || i > 5) {
                                value = this.formatData.wideDayPeriods[patternCharIndex];
                            } else {
                                value = this.formatData.narrowDayPeriods[patternCharIndex];
                            }
                        }
                        DayPeriod tz4 = periodType;
                        String toAppend2 = value;
                        if (tz4 != DayPeriod.AM && tz4 != DayPeriod.PM && toAppend2 != null) {
                            stringBuffer.append(toAppend2);
                            calendar2 = calendar;
                            patternCharIndex2 = patternCharIndex3;
                            toAppend = date3;
                            value2 = value7;
                            value = value2;
                            capContextUsageType2 = capContextUsageType;
                            break;
                        }
                        DayPeriod periodType2 = tz4;
                        patternCharIndex2 = toAppend;
                        subFormat(stringBuffer, 'a', i, i2, fieldNum, displayContext, fieldPosition, cal);
                    } else {
                        patternCharIndex3 = patternCharIndex;
                        date3 = date2;
                        value7 = value;
                        tz2 = tz;
                        bufstart2 = bufstart;
                        subFormat(stringBuffer, 'a', i, i2, fieldNum, displayContext, fieldPosition, cal);
                    }
                    calendar2 = cal;
                    patternCharIndex2 = patternCharIndex3;
                    toAppend = date3;
                    value2 = value7;
                    value = value2;
                    capContextUsageType2 = capContextUsageType;
                    break;
                case 37:
                    stringBuffer.append(this.formatData.getTimeSeparatorString());
                    timeZone = tz;
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    toAppend = date2;
                    value2 = value;
                    patternCharIndex2 = patternCharIndex;
                    value = value2;
                    capContextUsageType2 = capContextUsageType;
                    break;
                default:
                    bufstart2 = bufstart;
                    calendar2 = calendar;
                    toAppend = date2;
                    value2 = value;
                    patternCharIndex2 = patternCharIndex;
                    zeroPaddingNumber(currentNumberFormat, stringBuffer, value2, i, Integer.MAX_VALUE);
            }
        } else if (c != 'l') {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal pattern character '");
            stringBuilder.append(c);
            stringBuilder.append("' in \"");
            stringBuilder.append(this.pattern);
            stringBuilder.append('\"');
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static void safeAppend(String[] array, int value, StringBuffer appendTo) {
        if (array != null && value >= 0 && value < array.length) {
            appendTo.append(array[value]);
        }
    }

    private static void safeAppendWithMonthPattern(String[] array, int value, StringBuffer appendTo, String monthPattern) {
        if (array != null && value >= 0 && value < array.length) {
            if (monthPattern == null) {
                appendTo.append(array[value]);
                return;
            }
            appendTo.append(SimpleFormatterImpl.formatRawPattern(monthPattern, 1, 1, array[value]));
        }
    }

    private Object[] getPatternItems() {
        if (this.patternItems != null) {
            return this.patternItems;
        }
        this.patternItems = (Object[]) PARSED_PATTERN_CACHE.get(this.pattern);
        if (this.patternItems != null) {
            return this.patternItems;
        }
        StringBuilder text = new StringBuilder();
        char itemType = 0;
        List<Object> items = new ArrayList();
        int itemLength = 1;
        boolean inQuote = false;
        boolean isPrevQuote = false;
        for (int i = 0; i < this.pattern.length(); i++) {
            char ch = this.pattern.charAt(i);
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                if (isPrevQuote) {
                    text.append(PatternTokenizer.SINGLE_QUOTE);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        items.add(new PatternItem(itemType, itemLength));
                        itemType = 0;
                    }
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                if (inQuote) {
                    text.append(ch);
                } else if (!isSyntaxChar(ch)) {
                    if (itemType != 0) {
                        items.add(new PatternItem(itemType, itemLength));
                        itemType = 0;
                    }
                    text.append(ch);
                } else if (ch == itemType) {
                    itemLength++;
                } else {
                    if (itemType != 0) {
                        items.add(new PatternItem(itemType, itemLength));
                    } else if (text.length() > 0) {
                        items.add(text.toString());
                        text.setLength(0);
                    }
                    itemType = ch;
                    itemLength = 1;
                }
            }
        }
        if (itemType != 0) {
            items.add(new PatternItem(itemType, itemLength));
        } else if (text.length() > 0) {
            items.add(text.toString());
            text.setLength(0);
        }
        this.patternItems = items.toArray(new Object[items.size()]);
        PARSED_PATTERN_CACHE.put(this.pattern, this.patternItems);
        return this.patternItems;
    }

    @Deprecated
    protected void zeroPaddingNumber(NumberFormat nf, StringBuffer buf, int value, int minDigits, int maxDigits) {
        if (!this.useLocalZeroPaddingNumberFormat || value < 0) {
            nf.setMinimumIntegerDigits(minDigits);
            nf.setMaximumIntegerDigits(maxDigits);
            nf.format((long) value, buf, new FieldPosition(-1));
            return;
        }
        fastZeroPaddingNumber(buf, value, minDigits, maxDigits);
    }

    public void setNumberFormat(NumberFormat newNumberFormat) {
        super.setNumberFormat(newNumberFormat);
        initLocalZeroPaddingNumberFormat();
        initializeTimeZoneFormat(true);
        if (this.numberFormatters != null) {
            this.numberFormatters = null;
        }
        if (this.overrideMap != null) {
            this.overrideMap = null;
        }
    }

    private void initLocalZeroPaddingNumberFormat() {
        if (this.numberFormat instanceof DecimalFormat) {
            String[] tmpDigits = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getDigitStringsLocal();
            this.useLocalZeroPaddingNumberFormat = true;
            this.decDigits = new char[10];
            int i = 0;
            while (i < 10) {
                if (tmpDigits[i].length() > 1) {
                    this.useLocalZeroPaddingNumberFormat = false;
                    break;
                } else {
                    this.decDigits[i] = tmpDigits[i].charAt(0);
                    i++;
                }
            }
        } else if (this.numberFormat instanceof DateNumberFormat) {
            this.decDigits = ((DateNumberFormat) this.numberFormat).getDigits();
            this.useLocalZeroPaddingNumberFormat = true;
        } else {
            this.useLocalZeroPaddingNumberFormat = false;
        }
        if (this.useLocalZeroPaddingNumberFormat) {
            this.decimalBuf = new char[10];
        }
    }

    private void fastZeroPaddingNumber(StringBuffer buf, int value, int minDigits, int maxDigits) {
        int padding;
        int limit = this.decimalBuf.length < maxDigits ? this.decimalBuf.length : maxDigits;
        int index = limit - 1;
        while (true) {
            this.decimalBuf[index] = this.decDigits[value % 10];
            value /= 10;
            if (index == 0 || value == 0) {
                padding = minDigits - (limit - index);
            } else {
                index--;
            }
        }
        padding = minDigits - (limit - index);
        while (padding > 0 && index > 0) {
            index--;
            this.decimalBuf[index] = this.decDigits[0];
            padding--;
        }
        while (padding > 0) {
            buf.append(this.decDigits[0]);
            padding--;
        }
        buf.append(this.decimalBuf, index, limit - index);
    }

    protected String zeroPaddingNumber(long value, int minDigits, int maxDigits) {
        this.numberFormat.setMinimumIntegerDigits(minDigits);
        this.numberFormat.setMaximumIntegerDigits(maxDigits);
        return this.numberFormat.format(value);
    }

    private static final boolean isNumeric(char formatChar, int count) {
        return NUMERIC_FORMAT_CHARS.indexOf(formatChar) >= 0 || (count <= 2 && NUMERIC_FORMAT_CHARS2.indexOf(formatChar) >= 0);
    }

    /* JADX WARNING: Removed duplicated region for block: B:256:0x04ec  */
    /* JADX WARNING: Removed duplicated region for block: B:258:0x04fc  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x051b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void parse(String text, Calendar cal, ParsePosition parsePos) {
        TimeZone backupTZ;
        Calendar resultCal;
        Calendar cal2;
        TimeZone backupTZ2;
        ParsePosition parsePosition = parsePos;
        Calendar calendar = cal;
        if (calendar == this.calendar || cal.getType().equals(this.calendar.getType())) {
            backupTZ = null;
            resultCal = null;
            cal2 = calendar;
        } else {
            this.calendar.setTimeInMillis(cal.getTimeInMillis());
            TimeZone backupTZ3 = this.calendar.getTimeZone();
            this.calendar.setTimeZone(cal.getTimeZone());
            backupTZ = backupTZ3;
            resultCal = calendar;
            cal2 = this.calendar;
        }
        int pos = parsePos.getIndex();
        if (pos < 0) {
            parsePosition.setErrorIndex(0);
            return;
        }
        int start = pos;
        Output<DayPeriod> dayPeriod = new Output(null);
        Output<TimeType> tzTimeType = new Output(TimeType.UNKNOWN);
        boolean[] ambiguousYear = new boolean[]{false};
        int numericFieldStart = -1;
        MessageFormat numericLeapMonthFormatter = null;
        if (this.formatData.leapMonthPatterns != null && this.formatData.leapMonthPatterns.length >= 7) {
            numericLeapMonthFormatter = new MessageFormat(this.formatData.leapMonthPatterns[6], this.locale);
        }
        MessageFormat numericLeapMonthFormatter2 = numericLeapMonthFormatter;
        Object[] items = getPatternItems();
        int pos2 = pos;
        int numericFieldLength = 0;
        int numericStartPos = 0;
        pos = 0;
        while (true) {
            int i = pos;
            int i2;
            Output<TimeType> tzTimeType2;
            Output<DayPeriod> dayPeriod2;
            Calendar resultCal2;
            Calendar cal3;
            Object[] items2;
            int idx;
            if (i < items.length) {
                if (items[i] instanceof PatternItem) {
                    int numericFieldStart2;
                    PatternItem field = items[i];
                    if (field.isNumeric && numericFieldStart == -1 && i + 1 < items.length && (items[i + 1] instanceof PatternItem) && ((PatternItem) items[i + 1]).isNumeric) {
                        pos = i;
                        numericFieldLength = field.length;
                        numericStartPos = pos2;
                    } else {
                        pos = numericFieldStart;
                    }
                    Object[] items3;
                    if (pos != -1) {
                        numericFieldStart = field.length;
                        if (pos == i) {
                            numericFieldStart = numericFieldLength;
                        }
                        i2 = i;
                        tzTimeType2 = tzTimeType;
                        dayPeriod2 = dayPeriod;
                        int start2 = start;
                        items3 = items;
                        resultCal2 = resultCal;
                        pos2 = subParse(text, pos2, field.type, numericFieldStart, true, false, ambiguousYear, cal2, numericLeapMonthFormatter2, tzTimeType2);
                        if (pos2 < 0) {
                            numericFieldLength--;
                            if (numericFieldLength == 0) {
                                parsePosition.setIndex(start2);
                                parsePosition.setErrorIndex(pos2);
                                if (backupTZ != null) {
                                    this.calendar.setTimeZone(backupTZ);
                                }
                                return;
                            }
                            numericFieldStart = pos;
                            pos2 = numericStartPos;
                            start = start2;
                            tzTimeType = tzTimeType2;
                            dayPeriod = dayPeriod2;
                            items = items3;
                            resultCal = resultCal2;
                            pos = numericFieldStart;
                        } else {
                            numericFieldStart2 = pos;
                            start = start2;
                            cal3 = cal2;
                            i = i2;
                            items2 = items3;
                            backupTZ2 = backupTZ;
                        }
                    } else {
                        i2 = i;
                        tzTimeType2 = tzTimeType;
                        dayPeriod2 = dayPeriod;
                        int pos3 = pos2;
                        items3 = items;
                        resultCal2 = resultCal;
                        resultCal = start;
                        PatternItem field2 = field;
                        if (field2.type != 108) {
                            numericFieldStart2 = -1;
                            int s = pos3;
                            int start3 = resultCal;
                            cal3 = cal2;
                            backupTZ2 = backupTZ;
                            pos2 = subParse(text, pos3, field2.type, field2.length, false, true, ambiguousYear, cal2, numericLeapMonthFormatter2, tzTimeType2, dayPeriod2);
                            if (pos2 >= 0) {
                                items2 = items3;
                                start = start3;
                                i = i2;
                            } else if (pos2 == ISOSpecialEra) {
                                pos2 = s;
                                items2 = items3;
                                if (i2 + 1 < items2.length) {
                                    numericFieldStart = null;
                                    try {
                                        String patl = items2[i2 + 1];
                                        if (patl == null) {
                                            patl = (String) items2[i2 + 1];
                                        }
                                        numericFieldStart = patl.length();
                                        idx = 0;
                                        while (idx < numericFieldStart && PatternProps.isWhiteSpace(patl.charAt(idx))) {
                                            idx++;
                                        }
                                        if (idx == numericFieldStart) {
                                            i2++;
                                        }
                                        i = i2;
                                        start = start3;
                                    } catch (ClassCastException e) {
                                        parsePosition.setIndex(start3);
                                        parsePosition.setErrorIndex(s);
                                        if (backupTZ2 != null) {
                                            this.calendar.setTimeZone(backupTZ2);
                                        }
                                        return;
                                    }
                                }
                                start = start3;
                                i = i2;
                            } else {
                                idx = s;
                                parsePosition.setIndex(start3);
                                parsePosition.setErrorIndex(idx);
                                if (backupTZ2 != null) {
                                    this.calendar.setTimeZone(backupTZ2);
                                }
                                return;
                            }
                        }
                        start = resultCal;
                        cal3 = cal2;
                        items2 = items3;
                        backupTZ2 = backupTZ;
                        numericFieldStart2 = pos;
                        i = i2;
                        pos2 = pos3;
                    }
                    i2 = i;
                    numericFieldStart = pos2;
                    pos = numericFieldStart2;
                } else {
                    i2 = i;
                    tzTimeType2 = tzTimeType;
                    dayPeriod2 = dayPeriod;
                    items2 = items;
                    resultCal2 = resultCal;
                    cal3 = cal2;
                    backupTZ2 = backupTZ;
                    pos = -1;
                    boolean[] complete = new boolean[1];
                    numericFieldStart = matchLiteral(text, pos2, items2, i, complete);
                    if (!complete[0]) {
                        parsePosition.setIndex(start);
                        parsePosition.setErrorIndex(numericFieldStart);
                        if (backupTZ2 != null) {
                            this.calendar.setTimeZone(backupTZ2);
                        }
                        return;
                    }
                }
                pos2 = numericFieldStart;
                items = items2;
                backupTZ = backupTZ2;
                tzTimeType = tzTimeType2;
                dayPeriod = dayPeriod2;
                resultCal = resultCal2;
                cal2 = cal3;
                numericFieldStart = pos;
                pos = i2 + 1;
            } else {
                Calendar cal4;
                int pos4;
                i2 = i;
                tzTimeType2 = tzTimeType;
                dayPeriod2 = dayPeriod;
                items2 = items;
                resultCal2 = resultCal;
                cal3 = cal2;
                backupTZ2 = backupTZ;
                int pos5 = pos2;
                if (pos5 >= text.length()) {
                    String str = text;
                } else if (text.charAt(pos5) == '.' && getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_WHITESPACE) && items2.length != 0) {
                    Object lastItem = items2[items2.length - 1];
                    if ((lastItem instanceof PatternItem) && !((PatternItem) lastItem).isNumeric) {
                        pos5++;
                    }
                }
                Output<DayPeriod> dayPeriod3 = dayPeriod2;
                if (dayPeriod3.value != null) {
                    DayPeriodRules ruleSet = DayPeriodRules.getInstance(getLocale());
                    cal4 = cal3;
                    int i3;
                    if (cal4.isSet(10) || cal4.isSet(11)) {
                        pos4 = pos5;
                        if (cal4.isSet(11)) {
                            idx = cal4.get(11);
                        } else {
                            idx = cal4.get(10);
                            if (idx == 0) {
                                idx = 12;
                            }
                        }
                        if (idx == 0) {
                        } else if (13 > idx || idx > 23) {
                            if (idx == 12) {
                                idx = 0;
                            }
                            double hoursAheadMidPoint = (((double) idx) + (((double) cal4.get(12)) / 60.0d)) - ruleSet.getMidPointForDayPeriod((DayPeriod) dayPeriod3.value);
                            if (-6.0d > hoursAheadMidPoint || hoursAheadMidPoint >= 6.0d) {
                                cal4.set(9, 1);
                            } else {
                                cal4.set(9, 0);
                            }
                        } else {
                            i3 = numericFieldStart;
                        }
                        cal4.set(11, idx);
                    } else {
                        pos4 = pos5;
                        double midPoint = ruleSet.getMidPointForDayPeriod((DayPeriod) dayPeriod3.value);
                        int midPointHour = (int) midPoint;
                        int midPointMinute = midPoint - ((double) midPointHour) > 0.0d ? 30 : 0;
                        cal4.set(11, midPointHour);
                        cal4.set(12, midPointMinute);
                        i3 = numericFieldStart;
                    }
                } else {
                    pos4 = pos5;
                    cal4 = cal3;
                }
                pos5 = pos4;
                parsePosition.setIndex(pos5);
                Output<TimeType> tzTimeType3 = tzTimeType2;
                Output<TimeType> output;
                Output<DayPeriod> output2;
                SimpleDateFormat simpleDateFormat;
                try {
                    Calendar tzTimeType4;
                    TimeType tztype = tzTimeType3.value;
                    if (!ambiguousYear[0]) {
                        try {
                            if (tztype == TimeType.UNKNOWN) {
                                output = tzTimeType3;
                                output2 = dayPeriod3;
                                simpleDateFormat = this;
                                tzTimeType4 = resultCal2;
                                if (tzTimeType4 != null) {
                                    tzTimeType4.setTimeZone(cal4.getTimeZone());
                                    tzTimeType4.setTimeInMillis(cal4.getTimeInMillis());
                                }
                                if (backupTZ2 != null) {
                                    simpleDateFormat.calendar.setTimeZone(backupTZ2);
                                }
                                return;
                            }
                        } catch (IllegalArgumentException e2) {
                            output = tzTimeType3;
                            simpleDateFormat = this;
                            parsePosition.setErrorIndex(pos5);
                            parsePosition.setIndex(start);
                            if (backupTZ2 != null) {
                            }
                            return;
                        }
                    }
                    if (ambiguousYear[0]) {
                        try {
                            simpleDateFormat = this;
                        } catch (IllegalArgumentException e3) {
                            simpleDateFormat = this;
                            tzTimeType4 = resultCal2;
                            parsePosition.setErrorIndex(pos5);
                            parsePosition.setIndex(start);
                            if (backupTZ2 != null) {
                            }
                            return;
                        }
                        try {
                            if (((Calendar) cal4.clone()).getTime().before(getDefaultCenturyStart())) {
                                cal4.set(1, getDefaultCenturyStartYear() + 100);
                            }
                        } catch (IllegalArgumentException e4) {
                            tzTimeType4 = resultCal2;
                            parsePosition.setErrorIndex(pos5);
                            parsePosition.setIndex(start);
                            if (backupTZ2 != null) {
                            }
                            return;
                        }
                    }
                    simpleDateFormat = this;
                    try {
                        if (tztype != TimeType.UNKNOWN) {
                            try {
                                long localMillis;
                                int i4;
                                int i5;
                                Calendar copy = (Calendar) cal4.clone();
                                TimeZone tz = copy.getTimeZone();
                                BasicTimeZone btz = null;
                                if (tz instanceof BasicTimeZone) {
                                    btz = (BasicTimeZone) tz;
                                }
                                copy.set(15, 0);
                                copy.set(16, 0);
                                long localMillis2 = copy.getTimeInMillis();
                                int[] offsets = new int[2];
                                if (btz != null) {
                                    if (tztype == TimeType.STANDARD) {
                                        btz.getOffsetFromLocal(localMillis2, 1, 1, offsets);
                                    } else {
                                        btz.getOffsetFromLocal(localMillis2, 3, 3, offsets);
                                    }
                                    output = tzTimeType3;
                                    Calendar calendar2 = copy;
                                    output2 = dayPeriod3;
                                    localMillis = localMillis2;
                                    i4 = 1;
                                } else {
                                    output = tzTimeType3;
                                    localMillis = localMillis2;
                                    try {
                                        tz.getOffset(localMillis, true, offsets);
                                        if (tztype == TimeType.STANDARD) {
                                            try {
                                                if (offsets[1] != 0) {
                                                    i4 = 1;
                                                    output2 = dayPeriod3;
                                                    try {
                                                        tz.getOffset(localMillis - 86400000, i4, offsets);
                                                    } catch (IllegalArgumentException e5) {
                                                        tzTimeType4 = resultCal2;
                                                        parsePosition.setErrorIndex(pos5);
                                                        parsePosition.setIndex(start);
                                                        if (backupTZ2 != null) {
                                                        }
                                                        return;
                                                    }
                                                }
                                            } catch (IllegalArgumentException e6) {
                                                tzTimeType4 = resultCal2;
                                                parsePosition.setErrorIndex(pos5);
                                                parsePosition.setIndex(start);
                                                if (backupTZ2 != null) {
                                                }
                                                return;
                                            }
                                        }
                                        if (tztype == TimeType.DAYLIGHT) {
                                            i4 = 1;
                                            if (offsets[1] != 0) {
                                            }
                                            output2 = dayPeriod3;
                                            tz.getOffset(localMillis - 86400000, i4, offsets);
                                        } else {
                                            output2 = dayPeriod3;
                                            i4 = 1;
                                        }
                                    } catch (IllegalArgumentException e7) {
                                        output2 = dayPeriod3;
                                        tzTimeType4 = resultCal2;
                                        parsePosition.setErrorIndex(pos5);
                                        parsePosition.setIndex(start);
                                        if (backupTZ2 != null) {
                                            simpleDateFormat.calendar.setTimeZone(backupTZ2);
                                        }
                                        return;
                                    }
                                }
                                int resolvedSavings = offsets[i4];
                                long j;
                                if (tztype == TimeType.STANDARD) {
                                    TimeType timeType;
                                    if (offsets[i4] != 0) {
                                        resolvedSavings = 0;
                                        timeType = tztype;
                                        j = localMillis;
                                        cal4.set(15, offsets[0]);
                                        cal4.set(16, resolvedSavings);
                                    } else {
                                        timeType = tztype;
                                        j = localMillis;
                                        i5 = resolvedSavings;
                                    }
                                } else if (offsets[i4] == 0) {
                                    if (btz != null) {
                                        TimeZoneTransition afterTrs;
                                        long afterT;
                                        localMillis2 = localMillis + ((long) offsets[0]);
                                        long beforeT = localMillis2;
                                        i4 = 0;
                                        tztype = localMillis2;
                                        resolvedSavings = 0;
                                        do {
                                            dayPeriod3 = btz.getPreviousTransition(tztype, true);
                                            if (dayPeriod3 == null) {
                                                break;
                                            }
                                            tztype = dayPeriod3.getTime() - 1;
                                            i4 = dayPeriod3.getFrom().getDSTSavings();
                                        } while (i4 == 0);
                                        long j2 = tztype;
                                        tztype = beforeT;
                                        beforeT = j2;
                                        while (true) {
                                            afterTrs = btz.getNextTransition(tztype, false);
                                            if (afterTrs == null) {
                                                afterT = tztype;
                                                break;
                                            }
                                            afterT = afterTrs.getTime();
                                            resolvedSavings = afterTrs.getTo().getDSTSavings();
                                            if (resolvedSavings != 0) {
                                                break;
                                            }
                                            tztype = afterT;
                                        }
                                        if (dayPeriod3 == null || afterTrs == null) {
                                            if (dayPeriod3 != null && i4 != 0) {
                                                tztype = i4;
                                            } else if (afterTrs == null || resolvedSavings == 0) {
                                                tztype = btz.getDSTSavings();
                                            } else {
                                                tztype = resolvedSavings;
                                            }
                                        } else if (localMillis2 - beforeT > afterT - localMillis2) {
                                            tztype = resolvedSavings;
                                        } else {
                                            tztype = i4;
                                        }
                                    } else {
                                        j = localMillis;
                                        i5 = resolvedSavings;
                                        tztype = tz.getDSTSavings();
                                    }
                                    resolvedSavings = tztype;
                                    if (resolvedSavings == 0) {
                                        resolvedSavings = 3600000;
                                    }
                                    cal4.set(15, offsets[0]);
                                    cal4.set(16, resolvedSavings);
                                } else {
                                    j = localMillis;
                                    i5 = resolvedSavings;
                                }
                                resolvedSavings = i5;
                                cal4.set(15, offsets[0]);
                                cal4.set(16, resolvedSavings);
                            } catch (IllegalArgumentException e8) {
                                output = tzTimeType3;
                                output2 = dayPeriod3;
                                tzTimeType4 = resultCal2;
                                parsePosition.setErrorIndex(pos5);
                                parsePosition.setIndex(start);
                                if (backupTZ2 != null) {
                                }
                                return;
                            }
                        }
                        output2 = dayPeriod3;
                        tzTimeType4 = resultCal2;
                        if (tzTimeType4 != null) {
                        }
                        if (backupTZ2 != null) {
                        }
                        return;
                    } catch (IllegalArgumentException e9) {
                        output = tzTimeType3;
                        output2 = dayPeriod3;
                        tzTimeType4 = resultCal2;
                        parsePosition.setErrorIndex(pos5);
                        parsePosition.setIndex(start);
                        if (backupTZ2 != null) {
                        }
                        return;
                    }
                } catch (IllegalArgumentException e10) {
                    output = tzTimeType3;
                    output2 = dayPeriod3;
                    simpleDateFormat = this;
                    parsePosition.setErrorIndex(pos5);
                    parsePosition.setIndex(start);
                    if (backupTZ2 != null) {
                    }
                    return;
                }
            }
        }
    }

    private int matchLiteral(String text, int pos, Object[] items, int itemIndex, boolean[] complete) {
        Object before;
        String str = text;
        Object[] objArr = items;
        int i = itemIndex;
        int originalPos = pos;
        String patternLiteral = objArr[i];
        int plen = patternLiteral.length();
        int tlen = text.length();
        int pos2 = pos;
        int idx = 0;
        while (idx < plen && pos2 < tlen) {
            char pch = patternLiteral.charAt(idx);
            char ich = str.charAt(pos2);
            if (!PatternProps.isWhiteSpace(pch) || !PatternProps.isWhiteSpace(ich)) {
                if (pch != ich) {
                    if (ich != '.' || pos2 != originalPos || i <= 0 || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_WHITESPACE)) {
                        if ((pch != ' ' && pch != '.') || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_WHITESPACE)) {
                            if (pos2 == originalPos || !getBooleanAttribute(BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH)) {
                                break;
                            }
                            idx++;
                        } else {
                            idx++;
                        }
                    } else {
                        before = objArr[i - 1];
                        if (!(before instanceof PatternItem) || ((PatternItem) before).isNumeric) {
                            break;
                        }
                        pos2++;
                    }
                }
            } else {
                while (idx + 1 < plen && PatternProps.isWhiteSpace(patternLiteral.charAt(idx + 1))) {
                    idx++;
                }
                while (pos2 + 1 < tlen && PatternProps.isWhiteSpace(str.charAt(pos2 + 1))) {
                    pos2++;
                }
            }
            idx++;
            pos2++;
        }
        complete[0] = idx == plen;
        if (complete[0] || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_WHITESPACE) || i <= 0 || i >= objArr.length - 1 || originalPos >= tlen) {
            return pos2;
        }
        before = objArr[i - 1];
        Object after = objArr[i + 1];
        if (!(before instanceof PatternItem) || !(after instanceof PatternItem)) {
            return pos2;
        }
        if (DATE_PATTERN_TYPE.contains(((PatternItem) before).type) == DATE_PATTERN_TYPE.contains(((PatternItem) after).type)) {
            return pos2;
        }
        int newPos = originalPos;
        while (PatternProps.isWhiteSpace(str.charAt(newPos))) {
            newPos++;
        }
        complete[0] = newPos > originalPos;
        return newPos;
    }

    protected int matchString(String text, int start, int field, String[] data, Calendar cal) {
        return matchString(text, start, field, data, null, cal);
    }

    @Deprecated
    private int matchString(String text, int start, int field, String[] data, String monthPattern, Calendar cal) {
        String str = text;
        int i = start;
        int i2 = field;
        String[] strArr = data;
        String str2 = monthPattern;
        Calendar calendar = cal;
        int i3 = 0;
        int count = strArr.length;
        if (i2 == 7) {
            i3 = 1;
        }
        int isLeapMonth = 0;
        int bestMatch = -1;
        int bestMatchLength = 0;
        for (int i4 = i3; i4 < count; i4++) {
            int length = strArr[i4].length();
            if (length > bestMatchLength) {
                int regionMatchesWithOptionalDot = regionMatchesWithOptionalDot(str, i, strArr[i4], length);
                i3 = regionMatchesWithOptionalDot;
                if (regionMatchesWithOptionalDot >= 0) {
                    bestMatch = i4;
                    bestMatchLength = i3;
                    isLeapMonth = 0;
                }
            }
            if (str2 != null) {
                String leapMonthName = SimpleFormatterImpl.formatRawPattern(str2, 1, 1, new CharSequence[]{strArr[i4]});
                int length2 = leapMonthName.length();
                if (length2 > bestMatchLength) {
                    length = regionMatchesWithOptionalDot(str, i, leapMonthName, length2);
                    i3 = length;
                    if (length >= 0) {
                        bestMatch = i4;
                        bestMatchLength = i3;
                        isLeapMonth = 1;
                    }
                }
            }
        }
        if (bestMatch < 0) {
            return ~i;
        }
        if (i2 >= 0) {
            if (i2 == 1) {
                bestMatch++;
            }
            calendar.set(i2, bestMatch);
            if (str2 != null) {
                calendar.set(22, isLeapMonth);
            }
        }
        return i + bestMatchLength;
    }

    private int regionMatchesWithOptionalDot(String text, int start, String data, int length) {
        if (text.regionMatches(true, start, data, 0, length)) {
            return length;
        }
        if (data.length() > 0 && data.charAt(data.length() - 1) == '.') {
            if (text.regionMatches(true, start, data, 0, length - 1)) {
                return length - 1;
            }
        }
        return -1;
    }

    protected int matchQuarterString(String text, int start, int field, String[] data, Calendar cal) {
        int count = data.length;
        int bestMatchLength = 0;
        int bestMatch = -1;
        for (int i = 0; i < count; i++) {
            int length = data[i].length();
            if (length > bestMatchLength) {
                int regionMatchesWithOptionalDot = regionMatchesWithOptionalDot(text, start, data[i], length);
                int matchLength = regionMatchesWithOptionalDot;
                if (regionMatchesWithOptionalDot >= 0) {
                    bestMatch = i;
                    bestMatchLength = matchLength;
                }
            }
        }
        if (bestMatch < 0) {
            return -start;
        }
        cal.set(field, bestMatch * 3);
        return start + bestMatchLength;
    }

    private int matchDayPeriodString(String text, int start, String[] data, int dataLength, Output<DayPeriod> dayPeriod) {
        int bestMatchLength = 0;
        int bestMatch = -1;
        for (int i = 0; i < dataLength; i++) {
            if (data[i] != null) {
                int length = data[i].length();
                if (length > bestMatchLength) {
                    int regionMatchesWithOptionalDot = regionMatchesWithOptionalDot(text, start, data[i], length);
                    int matchLength = regionMatchesWithOptionalDot;
                    if (regionMatchesWithOptionalDot >= 0) {
                        bestMatch = i;
                        bestMatchLength = matchLength;
                    }
                }
            }
        }
        if (bestMatch < 0) {
            return -start;
        }
        dayPeriod.value = DayPeriod.VALUES[bestMatch];
        return start + bestMatchLength;
    }

    protected int subParse(String text, int start, char ch, int count, boolean obeyCount, boolean allowNegative, boolean[] ambiguousYear, Calendar cal) {
        return subParse(text, start, ch, count, obeyCount, allowNegative, ambiguousYear, cal, null, null);
    }

    private int subParse(String text, int start, char ch, int count, boolean obeyCount, boolean allowNegative, boolean[] ambiguousYear, Calendar cal, MessageFormat numericLeapMonthFormatter, Output<TimeType> output) {
        return subParse(text, start, ch, count, obeyCount, allowNegative, ambiguousYear, cal, null, null, null);
    }

    /* JADX WARNING: Missing block: B:134:0x027b, code skipped:
            if (r14 == 4) goto L_0x027f;
     */
    /* JADX WARNING: Missing block: B:193:0x0385, code skipped:
            if (r6 > r12.formatData.shortYearNames.length) goto L_0x038a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    private int subParse(String text, int start, char ch, int count, boolean obeyCount, boolean allowNegative, boolean[] ambiguousYear, Calendar cal, MessageFormat numericLeapMonthFormatter, Output<TimeType> tzTimeType, Output<DayPeriod> dayPeriod) {
        String str = text;
        int i = count;
        boolean z = allowNegative;
        Calendar calendar = cal;
        MessageFormat messageFormat = numericLeapMonthFormatter;
        Output<TimeType> output = tzTimeType;
        int value = 0;
        ParsePosition pos = new ParsePosition(0);
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex == -1) {
            return ~start;
        }
        Number number;
        boolean isChineseCalendar;
        int c;
        int start2;
        int patternCharIndex2;
        int field;
        ParsePosition pos2;
        NumberFormat currentNumberFormat;
        Number number2;
        Calendar calendar2;
        int defaultCenturyStartYear;
        int i2;
        int i3;
        int start3;
        int start4 = start;
        NumberFormat currentNumberFormat2 = getNumberFormat(ch);
        int field2 = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        if (messageFormat != null) {
            number = null;
            messageFormat.setFormatByArgumentIndex(null, currentNumberFormat2);
        } else {
            number = null;
        }
        NumberFormat currentNumberFormat3 = currentNumberFormat2;
        boolean isChineseCalendar2 = cal.getType().equals("chinese") || cal.getType().equals("dangi");
        while (true) {
            isChineseCalendar = isChineseCalendar2;
            if (start4 >= text.length()) {
                return ~start4;
            }
            c = UTF16.charAt(str, start4);
            if (UCharacter.isUWhiteSpace(c) && PatternProps.isWhiteSpace(c)) {
                start4 += UTF16.getCharCount(c);
                isChineseCalendar2 = isChineseCalendar;
            } else {
                pos.setIndex(start4);
            }
        }
        pos.setIndex(start4);
        if (patternCharIndex == 4 || patternCharIndex == 15 || ((patternCharIndex == 2 && i <= 2) || patternCharIndex == 26 || patternCharIndex == 19 || patternCharIndex == 25 || patternCharIndex == 1 || patternCharIndex == 18 || patternCharIndex == 30 || ((patternCharIndex == 0 && isChineseCalendar) || patternCharIndex == 27 || patternCharIndex == 28 || patternCharIndex == 8))) {
            boolean parsedNumericLeapMonth;
            if (messageFormat == null) {
                parsedNumericLeapMonth = false;
                c = 1;
            } else if (patternCharIndex == 2 || patternCharIndex == 26) {
                Object[] args = messageFormat.parse(str, pos);
                parsedNumericLeapMonth = false;
                if (args == null || pos.getIndex() <= start4 || !(args[0] instanceof Number)) {
                    c = 1;
                    pos.setIndex(start4);
                    calendar.set(22, 0);
                } else {
                    Number number3 = args[0];
                    c = 1;
                    calendar.set(22, 1);
                    parsedNumericLeapMonth = true;
                    number = number3;
                }
            } else {
                parsedNumericLeapMonth = false;
                c = 1;
            }
            if (parsedNumericLeapMonth) {
                start2 = start4;
                patternCharIndex2 = patternCharIndex;
                field = field2;
                pos2 = pos;
                currentNumberFormat = currentNumberFormat3;
                number2 = number;
            } else {
                if (!obeyCount) {
                    start2 = start4;
                    patternCharIndex2 = patternCharIndex;
                    field = field2;
                    pos2 = pos;
                    currentNumberFormat = currentNumberFormat3;
                    number2 = parseInt(str, pos2, z, currentNumberFormat);
                } else if (start4 + i > text.length()) {
                    return ~start4;
                } else {
                    field = field2;
                    field2 = c;
                    start2 = start4;
                    patternCharIndex2 = 4;
                    patternCharIndex2 = patternCharIndex;
                    pos2 = pos;
                    currentNumberFormat = currentNumberFormat3;
                    number2 = parseInt(str, i, pos, z, currentNumberFormat);
                }
                if (number2 == null && !allowNumericFallback(patternCharIndex2)) {
                    return ~start2;
                }
            }
            if (number2 != null) {
                value = number2.intValue();
            }
            number = number2;
        } else {
            start2 = start4;
            patternCharIndex2 = patternCharIndex;
            field = field2;
            pos2 = pos;
            currentNumberFormat = currentNumberFormat3;
        }
        ParsePosition pos3;
        Output<TimeType> pos4;
        int i4;
        int start5;
        int field3;
        int ambiguousTwoDigitYear;
        int patternCharIndex3;
        int value2;
        NumberFormat numberFormat;
        TimeZone tz;
        Style style;
        int field4;
        TimeZone tz2;
        int value3;
        int start6;
        NumberFormat numberFormat2;
        switch (patternCharIndex2) {
            case 0:
                calendar2 = calendar;
                int field5 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (isChineseCalendar) {
                    calendar2.set(0, value);
                    return pos3.getIndex();
                }
                int i5;
                if (i == 5) {
                    i4 = field5;
                    i5 = value;
                    c = matchString(str, start2, 0, this.formatData.narrowEras, null, calendar2);
                } else {
                    i5 = value;
                    if (i == 4) {
                        c = matchString(str, start2, 0, this.formatData.eraNames, null, calendar2);
                    } else {
                        c = matchString(str, start2, 0, this.formatData.eras, null, calendar2);
                    }
                }
                if (c == (~start2)) {
                    c = ISOSpecialEra;
                }
                return c;
            case 1:
            case 18:
                start5 = start2;
                calendar2 = calendar;
                field3 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (this.override != null && ((this.override.compareTo("hebr") == 0 || this.override.indexOf("y=hebr") >= 0) && value < 1000)) {
                    value += HEBREW_CAL_CUR_MILLENIUM_START_YEAR;
                    start2 = start5;
                } else if (i == 2) {
                    if (countDigits(str, start5, pos3.getIndex()) == 2 && cal.haveDefaultCentury()) {
                        start4 = 100;
                        ambiguousTwoDigitYear = getDefaultCenturyStartYear() % 100;
                        ambiguousYear[0] = value == ambiguousTwoDigitYear;
                        defaultCenturyStartYear = (getDefaultCenturyStartYear() / 100) * 100;
                        if (value >= ambiguousTwoDigitYear) {
                            start4 = 0;
                        }
                        value += defaultCenturyStartYear + start4;
                    }
                }
                calendar2.set(field3, value);
                if (DelayedHebrewMonthCheck) {
                    if (!HebrewCalendar.isLeapYear(value)) {
                        calendar2.add(2, 1);
                    }
                    DelayedHebrewMonthCheck = false;
                }
                return pos3.getIndex();
            case 2:
            case 26:
                int value4;
                currentNumberFormat = value;
                patternCharIndex3 = patternCharIndex2;
                value = start2;
                calendar2 = calendar;
                defaultCenturyStartYear = field;
                patternCharIndex2 = 3;
                start2 = 6;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (i <= 2) {
                    c = 2;
                    value4 = currentNumberFormat;
                    start5 = value;
                    patternCharIndex2 = patternCharIndex3;
                } else if (number == null || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC)) {
                    int patternCharIndex4;
                    String[] strArr;
                    c = (this.formatData.leapMonthPatterns == null || this.formatData.leapMonthPatterns.length < 7) ? 0 : 1;
                    start2 = c;
                    int newStart = 0;
                    NumberFormat numberFormat3;
                    if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 4) {
                        ambiguousTwoDigitYear = patternCharIndex3;
                        if (ambiguousTwoDigitYear == 2) {
                            String str2;
                            String[] strArr2 = this.formatData.months;
                            if (start2 != 0) {
                                str2 = this.formatData.leapMonthPatterns[0];
                            } else {
                                str2 = null;
                            }
                            patternCharIndex4 = ambiguousTwoDigitYear;
                            value4 = currentNumberFormat;
                            start5 = value;
                            c = matchString(str, value, 2, strArr2, str2, calendar2);
                        } else {
                            patternCharIndex4 = ambiguousTwoDigitYear;
                            field3 = defaultCenturyStartYear;
                            numberFormat3 = currentNumberFormat;
                            start5 = value;
                            strArr = this.formatData.standaloneMonths;
                            if (start2 != 0) {
                                currentNumberFormat = this.formatData.leapMonthPatterns[patternCharIndex2];
                            } else {
                                currentNumberFormat = null;
                            }
                            c = matchString(str, start5, 2, strArr, currentNumberFormat, calendar2);
                        }
                        newStart = c;
                        if (newStart > 0) {
                            return newStart;
                        }
                    }
                    field3 = defaultCenturyStartYear;
                    numberFormat3 = currentNumberFormat;
                    start5 = value;
                    patternCharIndex4 = patternCharIndex3;
                    if (!getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) && i != patternCharIndex2) {
                        return newStart;
                    }
                    if (patternCharIndex4 == 2) {
                        strArr = this.formatData.shortMonths;
                        if (start2 != 0) {
                            currentNumberFormat = this.formatData.leapMonthPatterns[1];
                        } else {
                            currentNumberFormat = null;
                        }
                        c = matchString(str, start5, 2, strArr, currentNumberFormat, calendar2);
                    } else {
                        strArr = this.formatData.standaloneShortMonths;
                        if (start2 != 0) {
                            currentNumberFormat = this.formatData.leapMonthPatterns[4];
                        } else {
                            currentNumberFormat = null;
                        }
                        c = matchString(str, start5, 2, strArr, currentNumberFormat, calendar2);
                    }
                    return c;
                } else {
                    field3 = defaultCenturyStartYear;
                    c = 2;
                    value4 = currentNumberFormat;
                    start5 = value;
                    patternCharIndex2 = patternCharIndex3;
                }
                value = value4;
                calendar2.set(c, value - 1);
                if (cal.getType().equals("hebrew") && value >= start2) {
                    if (!calendar2.isSet(1)) {
                        DelayedHebrewMonthCheck = true;
                    } else if (!HebrewCalendar.isLeapYear(calendar2.get(1))) {
                        calendar2.set(c, value);
                    }
                }
                return pos3.getIndex();
            case 4:
                currentNumberFormat = value;
                patternCharIndex3 = patternCharIndex2;
                value = start2;
                calendar2 = calendar;
                i2 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (currentNumberFormat == calendar2.getMaximum(11) + 1) {
                    currentNumberFormat = 0;
                }
                calendar2.set(11, currentNumberFormat);
                return pos3.getIndex();
            case 8:
                value2 = value;
                patternCharIndex3 = patternCharIndex2;
                calendar2 = calendar;
                i2 = field;
                patternCharIndex2 = 3;
                pos3 = pos2;
                pos4 = tzTimeType;
                c = countDigits(str, start2, pos3.getIndex());
                if (c < patternCharIndex2) {
                    while (c < patternCharIndex2) {
                        value2 *= 10;
                        c++;
                    }
                } else {
                    int a = 1;
                    while (true) {
                        ambiguousTwoDigitYear = a;
                        if (c > patternCharIndex2) {
                            a = ambiguousTwoDigitYear * 10;
                            c--;
                        } else {
                            value2 /= ambiguousTwoDigitYear;
                        }
                    }
                }
                ambiguousTwoDigitYear = c;
                calendar2.set(14, value2);
                return pos3.getIndex();
            case 9:
                value2 = value;
                patternCharIndex3 = patternCharIndex2;
                currentNumberFormat = start2;
                calendar2 = calendar;
                start2 = 4;
                patternCharIndex2 = 5;
                value = 6;
                pos3 = pos2;
                patternCharIndex = field;
                pos4 = tzTimeType;
                defaultCenturyStartYear = 3;
                break;
            case 14:
                int start7;
                start4 = value;
                patternCharIndex3 = patternCharIndex2;
                ambiguousTwoDigitYear = start2;
                calendar2 = calendar;
                currentNumberFormat = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (this.formatData.ampmsNarrow == null || i < 5 || getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                    start7 = ambiguousTwoDigitYear;
                    int newStart2 = 0;
                    patternCharIndex2 = 5;
                    start2 = currentNumberFormat;
                    c = matchString(str, start7, 9, this.formatData.ampms, null, calendar2);
                    ambiguousTwoDigitYear = c;
                    if (c > 0) {
                        return ambiguousTwoDigitYear;
                    }
                    i3 = ambiguousTwoDigitYear;
                } else {
                    start7 = ambiguousTwoDigitYear;
                    value2 = start4;
                    numberFormat = currentNumberFormat;
                    i3 = 0;
                    patternCharIndex2 = 5;
                }
                if (this.formatData.ampmsNarrow != null && (i >= patternCharIndex || getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH))) {
                    c = matchString(str, start7, 9, this.formatData.ampmsNarrow, null, calendar2);
                    i3 = c;
                    if (c > 0) {
                        return i3;
                    }
                }
                return ~start7;
            case 15:
                start4 = value;
                patternCharIndex3 = patternCharIndex2;
                ambiguousTwoDigitYear = start2;
                calendar2 = calendar;
                currentNumberFormat = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (start4 == calendar2.getLeastMaximum(10) + 1) {
                    start4 = 0;
                }
                calendar2.set(10, start4);
                return pos3.getIndex();
            case 17:
                start4 = value;
                patternCharIndex3 = patternCharIndex2;
                ambiguousTwoDigitYear = start2;
                calendar2 = calendar;
                currentNumberFormat = field;
                pos3 = pos2;
                tz = tzFormat().parse(i < 4 ? Style.SPECIFIC_SHORT : Style.SPECIFIC_LONG, str, pos3, tzTimeType);
                if (tz == null) {
                    return ~ambiguousTwoDigitYear;
                }
                calendar2.setTimeZone(tz);
                return pos3.getIndex();
            case 19:
                patternCharIndex3 = patternCharIndex2;
                ambiguousTwoDigitYear = start2;
                calendar2 = calendar;
                currentNumberFormat = field;
                start2 = 4;
                patternCharIndex2 = 3;
                start4 = value;
                pos3 = pos2;
                pos4 = tzTimeType;
                value = 6;
                if (i > 2 && (number == null || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC))) {
                    value2 = start4;
                    patternCharIndex = currentNumberFormat;
                    currentNumberFormat = ambiguousTwoDigitYear;
                    int i6 = patternCharIndex2;
                    patternCharIndex2 = 5;
                    defaultCenturyStartYear = i6;
                    break;
                }
                calendar2.set(currentNumberFormat, start4);
                return pos3.getIndex();
            case 23:
                start4 = value;
                patternCharIndex3 = patternCharIndex2;
                ambiguousTwoDigitYear = start2;
                calendar2 = calendar;
                currentNumberFormat = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                style = i < 4 ? Style.ISO_BASIC_LOCAL_FULL : i == 5 ? Style.ISO_EXTENDED_FULL : Style.LOCALIZED_GMT;
                tz = tzFormat().parse(style, str, pos3, pos4);
                if (tz == null) {
                    return ~ambiguousTwoDigitYear;
                }
                calendar2.setTimeZone(tz);
                return pos3.getIndex();
            case 24:
                start4 = value;
                patternCharIndex3 = patternCharIndex2;
                ambiguousTwoDigitYear = start2;
                calendar2 = calendar;
                currentNumberFormat = field;
                pos3 = pos2;
                tz = tzFormat().parse(i < 4 ? Style.GENERIC_SHORT : Style.GENERIC_LONG, str, pos3, tzTimeType);
                if (tz == null) {
                    return ~ambiguousTwoDigitYear;
                }
                calendar2.setTimeZone(tz);
                return pos3.getIndex();
            case 25:
                int value5;
                patternCharIndex3 = patternCharIndex2;
                currentNumberFormat = start2;
                calendar2 = calendar;
                field4 = field;
                patternCharIndex2 = 3;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (i == 1) {
                    start2 = currentNumberFormat;
                    value5 = value;
                } else if (number == null || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC)) {
                    i3 = 0;
                    if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 4) {
                        start2 = currentNumberFormat;
                        c = matchString(str, currentNumberFormat, 7, this.formatData.standaloneWeekdays, null, calendar2);
                        i3 = c;
                        if (c > 0) {
                            return i3;
                        }
                    }
                    start2 = currentNumberFormat;
                    value5 = value;
                    if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == patternCharIndex2) {
                        c = matchString(str, start2, 7, this.formatData.standaloneShortWeekdays, null, calendar2);
                        i3 = c;
                        if (c > 0) {
                            return i3;
                        }
                    }
                    if ((!getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) && i != 6) || this.formatData.standaloneShorterWeekdays == null) {
                        return i3;
                    }
                    return matchString(str, start2, 7, this.formatData.standaloneShorterWeekdays, null, calendar2);
                } else {
                    numberFormat = currentNumberFormat;
                    value5 = value;
                }
                calendar2.set(field4, value5);
                return pos3.getIndex();
            case 27:
                patternCharIndex3 = patternCharIndex2;
                currentNumberFormat = start2;
                calendar2 = calendar;
                field4 = field;
                patternCharIndex2 = 3;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (i <= 2) {
                    start2 = currentNumberFormat;
                } else if (number == null || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC)) {
                    i3 = 0;
                    if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 4) {
                        start2 = currentNumberFormat;
                        c = matchQuarterString(str, currentNumberFormat, 2, this.formatData.quarters, calendar2);
                        i3 = c;
                        if (c > 0) {
                            return i3;
                        }
                    }
                    start2 = currentNumberFormat;
                    if (!getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) && i != patternCharIndex2) {
                        return i3;
                    }
                    return matchQuarterString(str, start2, 2, this.formatData.shortQuarters, calendar2);
                } else {
                    numberFormat = currentNumberFormat;
                }
                calendar2.set(2, (value - 1) * patternCharIndex2);
                return pos3.getIndex();
            case 28:
                patternCharIndex3 = patternCharIndex2;
                currentNumberFormat = start2;
                calendar2 = calendar;
                field4 = field;
                patternCharIndex2 = 3;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (i <= 2) {
                    start2 = currentNumberFormat;
                } else if (number == null || !getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC)) {
                    i3 = 0;
                    if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 4) {
                        start2 = currentNumberFormat;
                        c = matchQuarterString(str, currentNumberFormat, 2, this.formatData.standaloneQuarters, calendar2);
                        i3 = c;
                        if (c > 0) {
                            return i3;
                        }
                    }
                    start2 = currentNumberFormat;
                    if (!getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) && i != patternCharIndex2) {
                        return i3;
                    }
                    return matchQuarterString(str, start2, 2, this.formatData.standaloneShortQuarters, calendar2);
                } else {
                    numberFormat = currentNumberFormat;
                }
                calendar2.set(2, (value - 1) * patternCharIndex2);
                return pos3.getIndex();
            case 29:
                patternCharIndex3 = patternCharIndex2;
                patternCharIndex2 = start2;
                calendar2 = calendar;
                field4 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                switch (i) {
                    case 1:
                        style = Style.ZONE_ID_SHORT;
                        break;
                    case 2:
                        style = Style.ZONE_ID;
                        break;
                    case 3:
                        style = Style.EXEMPLAR_LOCATION;
                        break;
                    default:
                        style = Style.GENERIC_LOCATION;
                        break;
                }
                tz2 = tzFormat().parse(style, str, pos3, pos4);
                if (tz2 == null) {
                    return ~patternCharIndex2;
                }
                calendar2.setTimeZone(tz2);
                return pos3.getIndex();
            case 30:
                value3 = value;
                patternCharIndex3 = patternCharIndex2;
                patternCharIndex2 = start2;
                calendar2 = calendar;
                field4 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (this.formatData.shortYearNames != null) {
                    c = matchString(str, patternCharIndex2, 1, this.formatData.shortYearNames, null, calendar2);
                    if (c > 0) {
                        return c;
                    }
                }
                if (number != null) {
                    if (!getBooleanAttribute(BooleanAttribute.PARSE_ALLOW_NUMERIC) && this.formatData.shortYearNames != null) {
                        value = value3;
                        break;
                    }
                    value = value3;
                    calendar2.set(1, value);
                    return pos3.getIndex();
                }
                return ~patternCharIndex2;
            case 31:
                value3 = value;
                patternCharIndex3 = patternCharIndex2;
                patternCharIndex2 = start2;
                calendar2 = calendar;
                field4 = field;
                pos3 = pos2;
                tz2 = tzFormat().parse(i < 4 ? Style.LOCALIZED_GMT_SHORT : Style.LOCALIZED_GMT, str, pos3, tzTimeType);
                if (tz2 == null) {
                    return ~patternCharIndex2;
                }
                calendar2.setTimeZone(tz2);
                return pos3.getIndex();
            case 32:
                value3 = value;
                patternCharIndex3 = patternCharIndex2;
                patternCharIndex2 = start2;
                calendar2 = calendar;
                field4 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                switch (i) {
                    case 1:
                        style = Style.ISO_BASIC_SHORT;
                        break;
                    case 2:
                        style = Style.ISO_BASIC_FIXED;
                        break;
                    case 3:
                        style = Style.ISO_EXTENDED_FIXED;
                        break;
                    case 4:
                        style = Style.ISO_BASIC_FULL;
                        break;
                    default:
                        style = Style.ISO_EXTENDED_FULL;
                        break;
                }
                tz2 = tzFormat().parse(style, str, pos3, pos4);
                if (tz2 == null) {
                    return ~patternCharIndex2;
                }
                calendar2.setTimeZone(tz2);
                return pos3.getIndex();
            case 33:
                value3 = value;
                ParsePosition pos5 = pos2;
                patternCharIndex3 = patternCharIndex2;
                start6 = start2;
                field4 = field;
                switch (i) {
                    case 1:
                        style = Style.ISO_BASIC_LOCAL_SHORT;
                        break;
                    case 2:
                        style = Style.ISO_BASIC_LOCAL_FIXED;
                        break;
                    case 3:
                        style = Style.ISO_EXTENDED_LOCAL_FIXED;
                        break;
                    case 4:
                        style = Style.ISO_BASIC_LOCAL_FULL;
                        break;
                    default:
                        style = Style.ISO_EXTENDED_LOCAL_FULL;
                        break;
                }
                pos3 = pos5;
                tz2 = tzFormat().parse(style, str, pos3, tzTimeType);
                if (tz2 != null) {
                    cal.setTimeZone(tz2);
                    return pos3.getIndex();
                }
                calendar2 = cal;
                return ~start6;
            case 35:
                patternCharIndex = 3;
                i3 = 4;
                start6 = start2;
                value = subParse(str, start2, 'a', i, obeyCount, z, ambiguousYear, calendar, numericLeapMonthFormatter, tzTimeType, dayPeriod);
                if (value > 0) {
                    return value;
                }
                pos2 = 0;
                if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 3) {
                    c = matchDayPeriodString(str, start6, this.formatData.abbreviatedDayPeriods, 2, dayPeriod);
                    pos2 = c;
                    if (c > 0) {
                        return pos2;
                    }
                }
                if (!getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH)) {
                    start2 = 4;
                    break;
                }
                start2 = 4;
                c = matchDayPeriodString(str, start6, this.formatData.wideDayPeriods, 2, dayPeriod);
                pos2 = c;
                if (c > 0) {
                    return pos2;
                }
                if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == r9) {
                    c = matchDayPeriodString(str, start6, this.formatData.narrowDayPeriods, 2, dayPeriod);
                    pos2 = c;
                    if (c > 0) {
                        return pos2;
                    }
                }
                return pos2;
            case 36:
                i3 = 0;
                if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 3) {
                    c = matchDayPeriodString(str, start2, this.formatData.abbreviatedDayPeriods, this.formatData.abbreviatedDayPeriods.length, dayPeriod);
                    i3 = c;
                    if (c > 0) {
                        return i3;
                    }
                }
                if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 4) {
                    c = matchDayPeriodString(str, start2, this.formatData.wideDayPeriods, this.formatData.wideDayPeriods.length, dayPeriod);
                    i3 = c;
                    if (c > 0) {
                        return i3;
                    }
                }
                if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 4) {
                    c = matchDayPeriodString(str, start2, this.formatData.narrowDayPeriods, this.formatData.narrowDayPeriods.length, dayPeriod);
                    i3 = c;
                    if (c > 0) {
                        return i3;
                    }
                }
                return i3;
            case 37:
                ArrayList<String> data = new ArrayList(3);
                data.add(this.formatData.getTimeSeparatorString());
                if (!this.formatData.getTimeSeparatorString().equals(":")) {
                    data.add(":");
                }
                if (getBooleanAttribute(BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH) && !this.formatData.getTimeSeparatorString().equals(".")) {
                    data.add(".");
                }
                numberFormat2 = currentNumberFormat;
                return matchString(str, start2, -1, (String[]) data.toArray(new String[0]), calendar);
            default:
                Number number4;
                numberFormat2 = currentNumberFormat;
                calendar2 = calendar;
                i4 = field;
                pos3 = pos2;
                pos4 = tzTimeType;
                if (!obeyCount) {
                    number4 = parseInt(str, pos3, allowNegative, numberFormat2);
                } else if (start2 + i > text.length()) {
                    return -start2;
                } else {
                    number4 = parseInt(str, i, pos3, allowNegative, numberFormat2);
                    currentNumberFormat2 = numberFormat2;
                    number2 = allowNegative;
                }
                if (number4 != null) {
                    if (patternCharIndex2 != 34) {
                        calendar2.set(i4, number4.intValue());
                    } else {
                        calendar2.setRelatedYear(number4.intValue());
                    }
                    return pos3.getIndex();
                }
                return ~start2;
        }
        i3 = 0;
        if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == start) {
            patternCharIndex2 = defaultCenturyStartYear;
            start3 = currentNumberFormat;
            start2 = value;
            c = matchString(str, currentNumberFormat, 7, this.formatData.weekdays, null, calendar2);
            i3 = c;
            if (c > 0) {
                return i3;
            }
        }
        patternCharIndex2 = defaultCenturyStartYear;
        i2 = patternCharIndex;
        start3 = currentNumberFormat;
        start2 = value;
        if (getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == r8) {
            c = matchString(str, start3, 7, this.formatData.shortWeekdays, null, calendar2);
            i3 = c;
            if (c > 0) {
                return i3;
            }
        }
        if ((getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == start) && this.formatData.shorterWeekdays != null) {
            c = matchString(str, start3, 7, this.formatData.shorterWeekdays, null, calendar2);
            i3 = c;
            if (c > 0) {
                return i3;
            }
        }
        if ((getBooleanAttribute(BooleanAttribute.PARSE_MULTIPLE_PATTERNS_FOR_MATCH) || i == 5) && this.formatData.narrowWeekdays != null) {
            c = matchString(str, start3, 7, this.formatData.narrowWeekdays, null, calendar2);
            i3 = c;
            if (c > 0) {
                return i3;
            }
        }
        return i3;
    }

    private boolean allowNumericFallback(int patternCharIndex) {
        if (patternCharIndex == 26 || patternCharIndex == 19 || patternCharIndex == 25 || patternCharIndex == 30 || patternCharIndex == 27 || patternCharIndex == 28) {
            return true;
        }
        return false;
    }

    private Number parseInt(String text, ParsePosition pos, boolean allowNegative, NumberFormat fmt) {
        return parseInt(text, -1, pos, allowNegative, fmt);
    }

    private Number parseInt(String text, int maxDigits, ParsePosition pos, boolean allowNegative, NumberFormat fmt) {
        Number number;
        int oldPos = pos.getIndex();
        if (allowNegative) {
            number = fmt.parse(text, pos);
        } else if (fmt instanceof DecimalFormat) {
            String oldPrefix = ((DecimalFormat) fmt).getNegativePrefix();
            ((DecimalFormat) fmt).setNegativePrefix(SUPPRESS_NEGATIVE_PREFIX);
            number = fmt.parse(text, pos);
            ((DecimalFormat) fmt).setNegativePrefix(oldPrefix);
        } else {
            boolean dateNumberFormat = fmt instanceof DateNumberFormat;
            if (dateNumberFormat) {
                ((DateNumberFormat) fmt).setParsePositiveOnly(true);
            }
            number = fmt.parse(text, pos);
            if (dateNumberFormat) {
                ((DateNumberFormat) fmt).setParsePositiveOnly(false);
            }
        }
        if (maxDigits <= 0) {
            return number;
        }
        int nDigits = pos.getIndex() - oldPos;
        if (nDigits <= maxDigits) {
            return number;
        }
        double val = number.doubleValue();
        for (nDigits -= maxDigits; nDigits > 0; nDigits--) {
            val /= 10.0d;
        }
        pos.setIndex(oldPos + maxDigits);
        return Integer.valueOf((int) val);
    }

    private static int countDigits(String text, int start, int end) {
        int numDigits = 0;
        int idx = start;
        while (idx < end) {
            int cp = text.codePointAt(idx);
            if (UCharacter.isDigit(cp)) {
                numDigits++;
            }
            idx += UCharacter.charCount(cp);
        }
        return numDigits;
    }

    private String translatePattern(String pat, String from, String to) {
        StringBuilder result = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < pat.length(); i++) {
            char c = pat.charAt(i);
            if (inQuote) {
                if (c == PatternTokenizer.SINGLE_QUOTE) {
                    inQuote = false;
                }
            } else if (c == PatternTokenizer.SINGLE_QUOTE) {
                inQuote = true;
            } else if (isSyntaxChar(c)) {
                int ci = from.indexOf(c);
                if (ci != -1) {
                    c = to.charAt(ci);
                }
            }
            result.append(c);
        }
        if (!inQuote) {
            return result.toString();
        }
        throw new IllegalArgumentException("Unfinished quote in pattern");
    }

    public String toPattern() {
        return this.pattern;
    }

    public String toLocalizedPattern() {
        return translatePattern(this.pattern, "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB", this.formatData.localPatternChars);
    }

    public void applyPattern(String pat) {
        this.pattern = pat;
        parsePattern();
        setLocale(null, null);
        this.patternItems = null;
    }

    public void applyLocalizedPattern(String pat) {
        this.pattern = translatePattern(pat, this.formatData.localPatternChars, "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB");
        setLocale(null, null);
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        this.formatData = (DateFormatSymbols) newFormatSymbols.clone();
    }

    protected DateFormatSymbols getSymbols() {
        return this.formatData;
    }

    public TimeZoneFormat getTimeZoneFormat() {
        return tzFormat().freeze();
    }

    public void setTimeZoneFormat(TimeZoneFormat tzfmt) {
        if (tzfmt.isFrozen()) {
            this.tzFormat = tzfmt;
        } else {
            this.tzFormat = tzfmt.cloneAsThawed().freeze();
        }
    }

    public Object clone() {
        SimpleDateFormat other = (SimpleDateFormat) super.clone();
        other.formatData = (DateFormatSymbols) this.formatData.clone();
        if (this.decimalBuf != null) {
            other.decimalBuf = new char[10];
        }
        return other;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!super.equals(obj)) {
            return false;
        }
        SimpleDateFormat that = (SimpleDateFormat) obj;
        if (this.pattern.equals(that.pattern) && this.formatData.equals(that.formatData)) {
            z = true;
        }
        return z;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (this.defaultCenturyStart == null) {
            initializeDefaultCenturyStart(this.defaultCenturyBase);
        }
        initializeTimeZoneFormat(false);
        stream.defaultWriteObject();
        stream.writeInt(getContext(Type.CAPITALIZATION).value());
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int capitalizationSettingValue = this.serialVersionOnStream > 1 ? stream.readInt() : -1;
        if (this.serialVersionOnStream < 1) {
            this.defaultCenturyBase = System.currentTimeMillis();
        } else {
            parseAmbiguousDatesAsAfter(this.defaultCenturyStart);
        }
        this.serialVersionOnStream = 2;
        this.locale = getLocale(ULocale.VALID_LOCALE);
        if (this.locale == null) {
            this.locale = ULocale.getDefault(Category.FORMAT);
        }
        initLocalZeroPaddingNumberFormat();
        setContext(DisplayContext.CAPITALIZATION_NONE);
        if (capitalizationSettingValue >= 0) {
            for (DisplayContext context : DisplayContext.values()) {
                if (context.value() == capitalizationSettingValue) {
                    setContext(context);
                    break;
                }
            }
        }
        if (!getBooleanAttribute(BooleanAttribute.PARSE_PARTIAL_MATCH)) {
            setBooleanAttribute(BooleanAttribute.PARSE_PARTIAL_LITERAL_MATCH, false);
        }
        parsePattern();
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        Calendar cal = this.calendar;
        if (obj instanceof Calendar) {
            cal = (Calendar) obj;
        } else if (obj instanceof Date) {
            this.calendar.setTime((Date) obj);
        } else if (obj instanceof Number) {
            this.calendar.setTimeInMillis(((Number) obj).longValue());
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        StringBuffer toAppendTo = new StringBuffer();
        int i = 0;
        FieldPosition pos = new FieldPosition(0);
        List<FieldPosition> attributes = new ArrayList();
        format(cal, getContext(Type.CAPITALIZATION), toAppendTo, pos, attributes);
        AttributedString as = new AttributedString(toAppendTo.toString());
        while (i < attributes.size()) {
            FieldPosition fp = (FieldPosition) attributes.get(i);
            Format.Field attribute = fp.getFieldAttribute();
            as.addAttribute(attribute, attribute, fp.getBeginIndex(), fp.getEndIndex());
            i++;
        }
        return as.getIterator();
    }

    ULocale getLocale() {
        return this.locale;
    }

    boolean isFieldUnitIgnored(int field) {
        return isFieldUnitIgnored(this.pattern, field);
    }

    static boolean isFieldUnitIgnored(String pattern, int field) {
        int fieldLevel = CALENDAR_FIELD_TO_LEVEL[field];
        char prevCh = 0;
        int count = 0;
        boolean inQuote = false;
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            if (ch != prevCh && count > 0) {
                if (fieldLevel <= getLevelFromChar(prevCh)) {
                    return false;
                }
                count = 0;
            }
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                if (i + 1 >= pattern.length() || pattern.charAt(i + 1) != PatternTokenizer.SINGLE_QUOTE) {
                    inQuote = !inQuote;
                } else {
                    i++;
                }
            } else if (!inQuote && isSyntaxChar(ch)) {
                prevCh = ch;
                count++;
            }
            i++;
        }
        return count <= 0 || fieldLevel > getLevelFromChar(prevCh);
    }

    @Deprecated
    public final StringBuffer intervalFormatByAlgorithm(Calendar fromCalendar, Calendar toCalendar, StringBuffer appendTo, FieldPosition pos) throws IllegalArgumentException {
        IllegalArgumentException e;
        Calendar calendar = fromCalendar;
        Calendar calendar2 = toCalendar;
        StringBuffer stringBuffer = appendTo;
        FieldPosition fieldPosition = pos;
        if (fromCalendar.isEquivalentTo(toCalendar)) {
            Object[] items = getPatternItems();
            int diffBegin = -1;
            int diffEnd = -1;
            int i = 0;
            int i2 = 0;
            while (i2 < items.length) {
                try {
                    if (diffCalFieldValue(calendar, calendar2, items, i2)) {
                        diffBegin = i2;
                        break;
                    }
                    i2++;
                } catch (IllegalArgumentException e2) {
                    throw new IllegalArgumentException(e2.toString());
                }
            }
            if (diffBegin == -1) {
                return format(calendar, stringBuffer, fieldPosition);
            }
            for (i2 = items.length - 1; i2 >= diffBegin; i2--) {
                if (diffCalFieldValue(calendar, calendar2, items, i2)) {
                    diffEnd = i2;
                    break;
                }
            }
            if (diffBegin == 0 && diffEnd == items.length - 1) {
                format(calendar, stringBuffer, fieldPosition);
                stringBuffer.append(" – ");
                format(calendar2, stringBuffer, fieldPosition);
                return stringBuffer;
            }
            int highestLevel = 1000;
            for (i2 = diffBegin; i2 <= diffEnd; i2++) {
                if (!(items[i2] instanceof String)) {
                    char ch = items[i2].type;
                    int patternCharIndex = getIndexFromChar(ch);
                    if (patternCharIndex == -1) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Illegal pattern character '");
                        stringBuilder.append(ch);
                        stringBuilder.append("' in \"");
                        stringBuilder.append(this.pattern);
                        stringBuilder.append('\"');
                        throw new IllegalArgumentException(stringBuilder.toString());
                    } else if (patternCharIndex < highestLevel) {
                        highestLevel = patternCharIndex;
                    }
                }
            }
            int i3 = 0;
            while (i3 < diffBegin) {
                try {
                    if (lowerLevel(items, i3, highestLevel)) {
                        diffBegin = i3;
                        break;
                    }
                    i3++;
                } catch (IllegalArgumentException e3) {
                    e2 = e3;
                    throw new IllegalArgumentException(e2.toString());
                }
            }
            int diffBegin2 = diffBegin;
            int highestLevel2;
            try {
                i3 = items.length - 1;
                while (i3 > diffEnd) {
                    try {
                        if (lowerLevel(items, i3, highestLevel)) {
                            break;
                        }
                        i3--;
                    } catch (IllegalArgumentException e4) {
                        e2 = e4;
                        throw new IllegalArgumentException(e2.toString());
                    }
                }
                i3 = diffEnd;
                if (diffBegin2 == 0 && diffEnd == items.length - 1) {
                    format(calendar, stringBuffer, fieldPosition);
                    stringBuffer.append(" – ");
                    format(calendar2, stringBuffer, fieldPosition);
                    return stringBuffer;
                }
                fieldPosition.setBeginIndex(0);
                fieldPosition.setEndIndex(0);
                DisplayContext capSetting = getContext(Type.CAPITALIZATION);
                while (true) {
                    int i4 = i;
                    if (i4 > i3) {
                        break;
                    }
                    int diffEnd2;
                    int i5;
                    if (items[i4] instanceof String) {
                        stringBuffer.append((String) items[i4]);
                        diffEnd2 = i3;
                        i5 = i4;
                        highestLevel2 = highestLevel;
                    } else {
                        PatternItem item = items[i4];
                        if (this.useFastFormat) {
                            diffEnd2 = i3;
                            i3 = item;
                            i5 = i4;
                            highestLevel2 = highestLevel;
                            subFormat(stringBuffer, item.type, item.length, appendTo.length(), i4, capSetting, fieldPosition, calendar);
                        } else {
                            diffEnd2 = i3;
                            i3 = item;
                            i5 = i4;
                            highestLevel2 = highestLevel;
                            stringBuffer.append(subFormat(i3.type, i3.length, appendTo.length(), i5, capSetting, fieldPosition, calendar));
                        }
                    }
                    i = i5 + 1;
                    i3 = diffEnd2;
                    highestLevel = highestLevel2;
                }
                highestLevel2 = highestLevel;
                stringBuffer.append(" – ");
                i3 = diffBegin2;
                while (i3 < items.length) {
                    if (items[i3] instanceof String) {
                        stringBuffer.append((String) items[i3]);
                    } else {
                        PatternItem highestLevel3 = (PatternItem) items[i3];
                        PatternItem item2;
                        if (this.useFastFormat) {
                            item2 = highestLevel3;
                            subFormat(stringBuffer, highestLevel3.type, highestLevel3.length, appendTo.length(), i3, capSetting, fieldPosition, calendar2);
                        } else {
                            item2 = highestLevel3;
                            stringBuffer.append(subFormat(item2.type, item2.length, appendTo.length(), i3, capSetting, fieldPosition, calendar2));
                        }
                    }
                    i3++;
                    calendar = fromCalendar;
                }
                return stringBuffer;
            } catch (IllegalArgumentException e5) {
                e2 = e5;
                highestLevel2 = highestLevel;
                throw new IllegalArgumentException(e2.toString());
            }
        }
        throw new IllegalArgumentException("can not format on two different calendars");
    }

    private boolean diffCalFieldValue(Calendar fromCalendar, Calendar toCalendar, Object[] items, int i) throws IllegalArgumentException {
        if (items[i] instanceof String) {
            return false;
        }
        char ch = items[i].type;
        int patternCharIndex = getIndexFromChar(ch);
        if (patternCharIndex != -1) {
            int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
            if (field < 0 || fromCalendar.get(field) == toCalendar.get(field)) {
                return false;
            }
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal pattern character '");
        stringBuilder.append(ch);
        stringBuilder.append("' in \"");
        stringBuilder.append(this.pattern);
        stringBuilder.append('\"');
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private boolean lowerLevel(Object[] items, int i, int level) throws IllegalArgumentException {
        if (items[i] instanceof String) {
            return false;
        }
        char ch = items[i].type;
        int patternCharIndex = getLevelFromChar(ch);
        if (patternCharIndex == -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal pattern character '");
            stringBuilder.append(ch);
            stringBuilder.append("' in \"");
            stringBuilder.append(this.pattern);
            stringBuilder.append('\"');
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (patternCharIndex >= level) {
            return true;
        } else {
            return false;
        }
    }

    public void setNumberFormat(String fields, NumberFormat overrideNF) {
        overrideNF.setGroupingUsed(false);
        String nsName = new StringBuilder();
        nsName.append("$");
        nsName.append(UUID.randomUUID().toString());
        nsName = nsName.toString();
        if (this.numberFormatters == null) {
            this.numberFormatters = new HashMap();
        }
        if (this.overrideMap == null) {
            this.overrideMap = new HashMap();
        }
        int i = 0;
        while (i < fields.length()) {
            char field = fields.charAt(i);
            if ("GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxrbB".indexOf(field) != -1) {
                this.overrideMap.put(Character.valueOf(field), nsName);
                this.numberFormatters.put(nsName, overrideNF);
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal field character '");
                stringBuilder.append(field);
                stringBuilder.append("' in setNumberFormat.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        this.useLocalZeroPaddingNumberFormat = false;
    }

    public NumberFormat getNumberFormat(char field) {
        Character ovrField = Character.valueOf(field);
        if (this.overrideMap == null || !this.overrideMap.containsKey(ovrField)) {
            return this.numberFormat;
        }
        return (NumberFormat) this.numberFormatters.get(((String) this.overrideMap.get(ovrField)).toString());
    }

    private void initNumberFormatters(ULocale loc) {
        this.numberFormatters = new HashMap();
        this.overrideMap = new HashMap();
        processOverrideString(loc, this.override);
    }

    private void processOverrideString(ULocale loc, String str) {
        if (str != null && str.length() != 0) {
            int start = 0;
            boolean moreToProcess = true;
            while (moreToProcess) {
                int end;
                String nsName;
                boolean fullOverride;
                int delimiterPosition = str.indexOf(";", start);
                if (delimiterPosition == -1) {
                    moreToProcess = false;
                    end = str.length();
                } else {
                    end = delimiterPosition;
                }
                String currentString = str.substring(start, end);
                int equalSignPosition = currentString.indexOf("=");
                if (equalSignPosition == -1) {
                    nsName = currentString;
                    fullOverride = true;
                } else {
                    nsName = currentString.substring(equalSignPosition + 1);
                    this.overrideMap.put(Character.valueOf(currentString.charAt(0)), nsName);
                    fullOverride = false;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(loc.getBaseName());
                stringBuilder.append("@numbers=");
                stringBuilder.append(nsName);
                NumberFormat nf = NumberFormat.createInstance(new ULocale(stringBuilder.toString()), 0);
                nf.setGroupingUsed(false);
                if (fullOverride) {
                    setNumberFormat(nf);
                } else {
                    this.useLocalZeroPaddingNumberFormat = false;
                }
                if (!(fullOverride || this.numberFormatters.containsKey(nsName))) {
                    this.numberFormatters.put(nsName, nf);
                }
                start = delimiterPosition + 1;
            }
        }
    }

    private void parsePattern() {
        this.hasMinute = false;
        this.hasSecond = false;
        boolean inQuote = false;
        for (int i = 0; i < this.pattern.length(); i++) {
            char ch = this.pattern.charAt(i);
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                inQuote = !inQuote;
            }
            if (!inQuote) {
                if (ch == 'm') {
                    this.hasMinute = true;
                }
                if (ch == 's') {
                    this.hasSecond = true;
                }
            }
        }
    }
}
