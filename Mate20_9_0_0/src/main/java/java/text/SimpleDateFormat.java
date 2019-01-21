package java.text;

import android.icu.text.TimeZoneNames;
import android.icu.text.TimeZoneNames.MatchInfo;
import android.icu.text.TimeZoneNames.NameType;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.DateFormat.Field;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import libcore.icu.LocaleData;
import sun.util.calendar.CalendarUtils;

public class SimpleDateFormat extends DateFormat {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final Set<NameType> DST_NAME_TYPES = Collections.unmodifiableSet(EnumSet.of(NameType.LONG_DAYLIGHT, NameType.SHORT_DAYLIGHT));
    private static final String GMT = "GMT";
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final EnumSet<NameType> NAME_TYPES = EnumSet.of(NameType.LONG_GENERIC, NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, NameType.SHORT_GENERIC, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT);
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD = new int[]{0, 1, 2, 5, 11, 11, 12, 13, 14, 7, 6, 8, 3, 4, 9, 10, 10, 15, 15, 17, 1000, 15, 2, 7, 9, 9};
    private static final int[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 17, 1, 9, 17, 2, 9, 14, 14};
    private static final Field[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD_ID = new Field[]{Field.ERA, Field.YEAR, Field.MONTH, Field.DAY_OF_MONTH, Field.HOUR_OF_DAY1, Field.HOUR_OF_DAY0, Field.MINUTE, Field.SECOND, Field.MILLISECOND, Field.DAY_OF_WEEK, Field.DAY_OF_YEAR, Field.DAY_OF_WEEK_IN_MONTH, Field.WEEK_OF_YEAR, Field.WEEK_OF_MONTH, Field.AM_PM, Field.HOUR1, Field.HOUR0, Field.TIME_ZONE, Field.TIME_ZONE, Field.YEAR, Field.DAY_OF_WEEK, Field.TIME_ZONE, Field.MONTH, Field.DAY_OF_WEEK, Field.AM_PM, Field.AM_PM};
    private static final int TAG_QUOTE_ASCII_CHAR = 100;
    private static final int TAG_QUOTE_CHARS = 101;
    private static final ConcurrentMap<Locale, NumberFormat> cachedNumberFormatData = new ConcurrentHashMap(3);
    static final int currentSerialVersion = 1;
    static final long serialVersionUID = 4774881970558875024L;
    private transient char[] compiledPattern;
    private Date defaultCenturyStart;
    private transient int defaultCenturyStartYear;
    private DateFormatSymbols formatData;
    private transient boolean hasFollowingMinusSign;
    private Locale locale;
    private transient char minusSign;
    private transient NumberFormat originalNumberFormat;
    private transient String originalNumberPattern;
    private String pattern;
    private int serialVersionOnStream;
    private transient TimeZoneNames timeZoneNames;
    transient boolean useDateFormatSymbols;
    private transient char zeroDigit;

    public SimpleDateFormat() {
        this(3, 3, Locale.getDefault(Category.FORMAT));
    }

    SimpleDateFormat(int timeStyle, int dateStyle, Locale locale) {
        this(getDateTimeFormat(timeStyle, dateStyle, locale), locale);
    }

    private static String getDateTimeFormat(int timeStyle, int dateStyle, Locale locale) {
        LocaleData localeData = LocaleData.get(locale);
        if (timeStyle >= 0 && dateStyle >= 0) {
            return MessageFormat.format("{0} {1}", localeData.getDateFormat(dateStyle), localeData.getTimeFormat(timeStyle));
        } else if (timeStyle >= 0) {
            return localeData.getTimeFormat(timeStyle);
        } else {
            if (dateStyle >= 0) {
                return localeData.getDateFormat(dateStyle);
            }
            throw new IllegalArgumentException("No date or time style specified");
        }
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, Locale.getDefault(Category.FORMAT));
    }

    public SimpleDateFormat(String pattern, Locale locale) {
        this.serialVersionOnStream = 1;
        this.minusSign = '-';
        this.hasFollowingMinusSign = $assertionsDisabled;
        if (pattern == null || locale == null) {
            throw new NullPointerException();
        }
        initializeCalendar(locale);
        this.pattern = pattern;
        this.formatData = DateFormatSymbols.getInstanceRef(locale);
        this.locale = locale;
        initialize(locale);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        this.serialVersionOnStream = 1;
        this.minusSign = '-';
        this.hasFollowingMinusSign = $assertionsDisabled;
        if (pattern == null || formatSymbols == null) {
            throw new NullPointerException();
        }
        this.pattern = pattern;
        this.formatData = (DateFormatSymbols) formatSymbols.clone();
        this.locale = Locale.getDefault(Category.FORMAT);
        initializeCalendar(this.locale);
        initialize(this.locale);
        this.useDateFormatSymbols = true;
    }

    private void initialize(Locale loc) {
        this.compiledPattern = compile(this.pattern);
        this.numberFormat = (NumberFormat) cachedNumberFormatData.get(loc);
        if (this.numberFormat == null) {
            this.numberFormat = NumberFormat.getIntegerInstance(loc);
            this.numberFormat.setGroupingUsed($assertionsDisabled);
            cachedNumberFormatData.putIfAbsent(loc, this.numberFormat);
        }
        this.numberFormat = (NumberFormat) this.numberFormat.clone();
        initializeDefaultCentury();
    }

    private void initializeCalendar(Locale loc) {
        if (this.calendar == null) {
            this.calendar = Calendar.getInstance(TimeZone.getDefault(), loc);
        }
    }

    private char[] compile(String pattern) {
        String str = pattern;
        int length = pattern.length();
        StringBuilder compiledCode = new StringBuilder(length * 2);
        int i = 0;
        int lastTag = -1;
        int count = 0;
        StringBuilder tmpBuffer = null;
        boolean inQuote = $assertionsDisabled;
        int i2 = 0;
        while (i2 < length) {
            char c = str.charAt(i2);
            int j;
            if (c == '\'') {
                if (i2 + 1 < length) {
                    c = str.charAt(i2 + 1);
                    if (c == '\'') {
                        i2++;
                        if (count != 0) {
                            encode(lastTag, count, compiledCode);
                            lastTag = -1;
                            count = 0;
                        }
                        if (inQuote) {
                            tmpBuffer.append(c);
                        } else {
                            compiledCode.append((char) (25600 | c));
                        }
                    }
                }
                if (inQuote) {
                    int len = tmpBuffer.length();
                    if (len == 1) {
                        char ch = tmpBuffer.charAt(i);
                        if (ch < 128) {
                            compiledCode.append((char) (25600 | ch));
                        } else {
                            compiledCode.append(25857);
                            compiledCode.append(ch);
                        }
                    } else {
                        encode(TAG_QUOTE_CHARS, len, compiledCode);
                        compiledCode.append((CharSequence) tmpBuffer);
                    }
                    inQuote = $assertionsDisabled;
                } else {
                    if (count != 0) {
                        encode(lastTag, count, compiledCode);
                        lastTag = -1;
                        count = 0;
                    }
                    if (tmpBuffer == null) {
                        tmpBuffer = new StringBuilder(length);
                    } else {
                        tmpBuffer.setLength(i);
                    }
                    inQuote = true;
                }
            } else if (inQuote) {
                tmpBuffer.append(c);
            } else if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
                if (count != 0) {
                    encode(lastTag, count, compiledCode);
                    lastTag = -1;
                    count = 0;
                }
                if (c < 128) {
                    compiledCode.append((char) (25600 | c));
                } else {
                    j = i2 + 1;
                    while (j < length) {
                        char d = str.charAt(j);
                        if (d == '\'' || ((d >= 'a' && d <= 'z') || (d >= 'A' && d <= 'Z'))) {
                            break;
                        }
                        j++;
                    }
                    compiledCode.append((char) (25856 | (j - i2)));
                    while (i2 < j) {
                        compiledCode.append(str.charAt(i2));
                        i2++;
                    }
                    i2--;
                }
            } else {
                i = "GyMdkHmsSEDFwWahKzZYuXLcbB".indexOf((int) c);
                j = i;
                if (i != -1) {
                    if (lastTag == -1 || lastTag == j) {
                        i = j;
                        count++;
                    } else {
                        encode(lastTag, count, compiledCode);
                        i = j;
                        count = 1;
                    }
                    lastTag = i;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal pattern character '");
                    stringBuilder.append(c);
                    stringBuilder.append("'");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            i2++;
            i = 0;
        }
        if (inQuote) {
            throw new IllegalArgumentException("Unterminated quote");
        }
        if (count != 0) {
            encode(lastTag, count, compiledCode);
        }
        i2 = compiledCode.length();
        char[] r = new char[i2];
        compiledCode.getChars(0, i2, r, 0);
        return r;
    }

    private static void encode(int tag, int length, StringBuilder buffer) {
        if (tag == 21 && length >= 4) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid ISO 8601 format: length=");
            stringBuilder.append(length);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (length < 255) {
            buffer.append((char) ((tag << 8) | length));
        } else {
            buffer.append((char) (255 | (tag << 8)));
            buffer.append((char) (length >>> 16));
            buffer.append((char) (65535 & length));
        }
    }

    private void initializeDefaultCentury() {
        this.calendar.setTimeInMillis(System.currentTimeMillis());
        this.calendar.add(1, -80);
        parseAmbiguousDatesAsAfter(this.calendar.getTime());
    }

    private void parseAmbiguousDatesAsAfter(Date startDate) {
        this.defaultCenturyStart = startDate;
        this.calendar.setTime(startDate);
        this.defaultCenturyStartYear = this.calendar.get(1);
    }

    public void set2DigitYearStart(Date startDate) {
        parseAmbiguousDatesAsAfter(new Date(startDate.getTime()));
    }

    public Date get2DigitYearStart() {
        return (Date) this.defaultCenturyStart.clone();
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        pos.endIndex = 0;
        pos.beginIndex = 0;
        return format(date, toAppendTo, pos.getFieldDelegate());
    }

    private StringBuffer format(Date date, StringBuffer toAppendTo, FieldDelegate delegate) {
        this.calendar.setTime(date);
        boolean useDateFormatSymbols = useDateFormatSymbols();
        int i = 0;
        while (i < this.compiledPattern.length) {
            int tag = this.compiledPattern[i] >>> 8;
            int i2 = i + 1;
            i = this.compiledPattern[i] & 255;
            if (i == 255) {
                int i3 = i2 + 1;
                i2 = i3 + 1;
                i = (this.compiledPattern[i2] << 16) | this.compiledPattern[i3];
            }
            int count = i;
            int i4 = i2;
            switch (tag) {
                case TAG_QUOTE_ASCII_CHAR /*100*/:
                    toAppendTo.append((char) count);
                    break;
                case TAG_QUOTE_CHARS /*101*/:
                    toAppendTo.append(this.compiledPattern, i4, count);
                    i4 += count;
                    break;
                default:
                    subFormat(tag, count, delegate, toAppendTo, useDateFormatSymbols);
                    break;
            }
            i = i4;
        }
        return toAppendTo;
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        StringBuffer sb = new StringBuffer();
        FieldDelegate delegate = new CharacterIteratorFieldDelegate();
        if (obj instanceof Date) {
            format((Date) obj, sb, delegate);
        } else if (obj instanceof Number) {
            format(new Date(((Number) obj).longValue()), sb, delegate);
        } else if (obj == null) {
            throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        return delegate.getIterator(sb.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0057  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x0221  */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x0203  */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x01f2  */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x01d9  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x01bf  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x01b3  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x01a4  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0189  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x010a  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00e6  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0087  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0078  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x023d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void subFormat(int patternCharIndex, int count, FieldDelegate delegate, StringBuffer buffer, boolean useDateFormatSymbols) {
        int value;
        int field;
        int patternCharIndex2;
        String current;
        boolean z;
        int style;
        int value2;
        String str;
        int fieldID;
        Format.Field f;
        int i = count;
        StringBuffer stringBuffer = buffer;
        boolean z2 = useDateFormatSymbols;
        String current2 = null;
        int beginOffset = buffer.length();
        int field2 = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        if (field2 == 17) {
            if (this.calendar.isWeekDateSupported()) {
                value = this.calendar.getWeekYear();
            } else {
                field2 = PATTERN_INDEX_TO_CALENDAR_FIELD[1];
                value = this.calendar.get(field2);
                field = field2;
                patternCharIndex2 = 1;
                current = value;
                z = true;
                style = i < 4 ? 2 : true;
                if (!(z2 || field == 1000)) {
                    current2 = this.calendar.getDisplayName(field, style, this.locale);
                }
                value2 = current2;
                int i2;
                switch (patternCharIndex2) {
                    case 0:
                        if (z2) {
                            String[] eras = this.formatData.getEras();
                            if (current < eras.length) {
                                value2 = eras[current];
                            }
                        }
                        if (value2 == 0) {
                            value2 = "";
                            break;
                        }
                        break;
                    case 1:
                    case 19:
                        field2 = 1;
                        i2 = field;
                        field = style;
                        if (!(this.calendar instanceof GregorianCalendar)) {
                            if (value2 == 0) {
                                if (field != 2) {
                                    field2 = i;
                                }
                                zeroPaddingNumber(current, field2, Integer.MAX_VALUE, stringBuffer);
                                break;
                            }
                        } else if (i == 2) {
                            zeroPaddingNumber(current, 2, 2, stringBuffer);
                            break;
                        } else {
                            zeroPaddingNumber(current, i, Integer.MAX_VALUE, stringBuffer);
                            break;
                        }
                        break;
                    case 2:
                        field = style;
                        if (z2) {
                            value2 = formatMonth(i, current, Integer.MAX_VALUE, stringBuffer, z2, $assertionsDisabled);
                            break;
                        }
                        break;
                    case 4:
                        field = style;
                        if (value2 == 0) {
                            if (current != null) {
                                zeroPaddingNumber(current, i, Integer.MAX_VALUE, stringBuffer);
                                break;
                            } else {
                                zeroPaddingNumber(this.calendar.getMaximum(11) + 1, i, Integer.MAX_VALUE, stringBuffer);
                                break;
                            }
                        }
                        break;
                    case 8:
                        field = style;
                        if (value2 == 0) {
                            current = (int) ((((double) current) / 1000.0d) * Math.pow(10.0d, (double) i));
                            zeroPaddingNumber(current, i, i, stringBuffer);
                            break;
                        }
                        break;
                    case 9:
                        field = style;
                        if (value2 == 0) {
                            value2 = formatWeekday(i, current, z2, $assertionsDisabled);
                            break;
                        }
                        break;
                    case 14:
                        field = style;
                        if (z2) {
                            value2 = this.formatData.getAmPmStrings()[current];
                            break;
                        }
                        break;
                    case 15:
                        field = style;
                        if (value2 == 0) {
                            if (current != null) {
                                zeroPaddingNumber(current, i, Integer.MAX_VALUE, stringBuffer);
                                break;
                            } else {
                                zeroPaddingNumber(this.calendar.getLeastMaximum(10) + 1, i, Integer.MAX_VALUE, stringBuffer);
                                break;
                            }
                        }
                        break;
                    case 17:
                        field = style;
                        if (value2 == 0) {
                            String zoneString;
                            TimeZone tz = this.calendar.getTimeZone();
                            style = this.calendar.get(16) != 0 ? 1 : 0;
                            if (this.formatData.isZoneStringsSet) {
                                zoneString = libcore.icu.TimeZoneNames.getDisplayName(this.formatData.getZoneStringsWrapper(), tz.getID(), style, i < 4 ? $assertionsDisabled : true);
                                TimeZone timeZone = tz;
                            } else {
                                NameType nameType;
                                if (i < 4) {
                                    if (style != 0) {
                                        nameType = NameType.SHORT_DAYLIGHT;
                                    } else {
                                        nameType = NameType.SHORT_STANDARD;
                                    }
                                } else if (style != 0) {
                                    nameType = NameType.LONG_DAYLIGHT;
                                } else {
                                    nameType = NameType.LONG_STANDARD;
                                }
                                zoneString = getTimeZoneNames().getDisplayName(android.icu.util.TimeZone.getCanonicalID(tz.getID()), nameType, this.calendar.getTimeInMillis());
                            }
                            if (zoneString == null) {
                                stringBuffer.append(TimeZone.createGmtOffsetString(true, true, this.calendar.get(15) + this.calendar.get(16)));
                                break;
                            } else {
                                stringBuffer.append(zoneString);
                                break;
                            }
                        }
                        break;
                    case 18:
                        field = style;
                        current = this.calendar.get(15) + this.calendar.get(16);
                        boolean includeSeparator = i >= 4 ? true : $assertionsDisabled;
                        if (i != 4) {
                            z = $assertionsDisabled;
                        }
                        stringBuffer.append(TimeZone.createGmtOffsetString(z, includeSeparator, current));
                        break;
                    case 21:
                        field = style;
                        current = this.calendar.get(15) + this.calendar.get(16);
                        if (current != null) {
                            current /= MILLIS_PER_MINUTE;
                            if (current >= null) {
                                stringBuffer.append('+');
                            } else {
                                stringBuffer.append('-');
                                current = -current;
                            }
                            CalendarUtils.sprintf0d(stringBuffer, current / 60, 2);
                            if (i != 1) {
                                if (i == 3) {
                                    stringBuffer.append(':');
                                }
                                CalendarUtils.sprintf0d(stringBuffer, current % 60, 2);
                                break;
                            }
                        }
                        stringBuffer.append('Z');
                        break;
                        break;
                    case 22:
                        if (!z2) {
                            field = style;
                            break;
                        }
                        i2 = field;
                        field = style;
                        value2 = formatMonth(i, current, Integer.MAX_VALUE, stringBuffer, z2, 1);
                        break;
                    case 23:
                        if (value2 == 0) {
                            value2 = formatWeekday(i, current, z2, true);
                            break;
                        }
                        break;
                    case 24:
                    case 25:
                        value2 = "";
                        break;
                    default:
                        field = style;
                        if (value2 == 0) {
                            zeroPaddingNumber(current, i, Integer.MAX_VALUE, stringBuffer);
                            break;
                        }
                        break;
                }
                field = style;
                str = value2;
                value2 = current;
                current = str;
                if (current != null) {
                    stringBuffer.append(current);
                }
                fieldID = PATTERN_INDEX_TO_DATE_FORMAT_FIELD[patternCharIndex2];
                f = PATTERN_INDEX_TO_DATE_FORMAT_FIELD_ID[patternCharIndex2];
                delegate.formatted(fieldID, f, f, beginOffset, buffer.length(), stringBuffer);
            }
        } else if (field2 == 1000) {
            value = CalendarBuilder.toISODayOfWeek(this.calendar.get(7));
        } else {
            value = this.calendar.get(field2);
        }
        patternCharIndex2 = patternCharIndex;
        field = field2;
        current = value;
        z = true;
        if (i < 4) {
        }
        style = i < 4 ? 2 : true;
        current2 = this.calendar.getDisplayName(field, style, this.locale);
        value2 = current2;
        switch (patternCharIndex2) {
            case 0:
                break;
            case 1:
            case 19:
                break;
            case 2:
                break;
            case 4:
                break;
            case 8:
                break;
            case 9:
                break;
            case 14:
                break;
            case 15:
                break;
            case 17:
                break;
            case 18:
                break;
            case 21:
                break;
            case 22:
                break;
            case 23:
                break;
            case 24:
            case 25:
                break;
            default:
                break;
        }
        field = style;
        str = value2;
        value2 = current;
        current = str;
        if (current != null) {
        }
        fieldID = PATTERN_INDEX_TO_DATE_FORMAT_FIELD[patternCharIndex2];
        f = PATTERN_INDEX_TO_DATE_FORMAT_FIELD_ID[patternCharIndex2];
        delegate.formatted(fieldID, f, f, beginOffset, buffer.length(), stringBuffer);
    }

    private String formatWeekday(int count, int value, boolean useDateFormatSymbols, boolean standalone) {
        if (!useDateFormatSymbols) {
            return null;
        }
        String[] weekdays = count == 4 ? standalone ? this.formatData.getStandAloneWeekdays() : this.formatData.getWeekdays() : count == 5 ? standalone ? this.formatData.getTinyStandAloneWeekdays() : this.formatData.getTinyWeekdays() : standalone ? this.formatData.getShortStandAloneWeekdays() : this.formatData.getShortWeekdays();
        return weekdays[value];
    }

    private String formatMonth(int count, int value, int maxIntCount, StringBuffer buffer, boolean useDateFormatSymbols, boolean standalone) {
        String current = null;
        if (useDateFormatSymbols) {
            String[] months = count == 4 ? standalone ? this.formatData.getStandAloneMonths() : this.formatData.getMonths() : count == 5 ? standalone ? this.formatData.getTinyStandAloneMonths() : this.formatData.getTinyMonths() : count == 3 ? standalone ? this.formatData.getShortStandAloneMonths() : this.formatData.getShortMonths() : null;
            if (months != null) {
                current = months[value];
            }
        } else if (count < 3) {
            current = null;
        }
        if (current == null) {
            zeroPaddingNumber(value + 1, count, maxIntCount, buffer);
        }
        return current;
    }

    private void zeroPaddingNumber(int value, int minDigits, int maxDigits, StringBuffer buffer) {
        try {
            if (this.zeroDigit == 0) {
                this.zeroDigit = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getZeroDigit();
            }
            if (value >= 0) {
                if (value < TAG_QUOTE_ASCII_CHAR && minDigits >= 1 && minDigits <= 2) {
                    if (value < 10) {
                        if (minDigits == 2) {
                            buffer.append(this.zeroDigit);
                        }
                        buffer.append((char) (this.zeroDigit + value));
                    } else {
                        buffer.append((char) (this.zeroDigit + (value / 10)));
                        buffer.append((char) (this.zeroDigit + (value % 10)));
                    }
                    return;
                } else if (value >= 1000 && value < 10000) {
                    if (minDigits == 4) {
                        buffer.append((char) (this.zeroDigit + (value / 1000)));
                        value %= 1000;
                        buffer.append((char) (this.zeroDigit + (value / TAG_QUOTE_ASCII_CHAR)));
                        value %= TAG_QUOTE_ASCII_CHAR;
                        buffer.append((char) (this.zeroDigit + (value / 10)));
                        buffer.append((char) (this.zeroDigit + (value % 10)));
                        return;
                    } else if (minDigits == 2 && maxDigits == 2) {
                        zeroPaddingNumber(value % TAG_QUOTE_ASCII_CHAR, 2, 2, buffer);
                        return;
                    }
                }
            }
        } catch (Exception e) {
        }
        this.numberFormat.setMinimumIntegerDigits(minDigits);
        this.numberFormat.setMaximumIntegerDigits(maxDigits);
        this.numberFormat.format((long) value, buffer, DontCareFieldPosition.INSTANCE);
    }

    public Date parse(String text, ParsePosition pos) {
        TimeZone tz = getTimeZone();
        try {
            Date parseInternal = parseInternal(text, pos);
            return parseInternal;
        } finally {
            setTimeZone(tz);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00da  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00d7 A:{SYNTHETIC} */
    /* JADX WARNING: Missing block: B:15:0x006a, code skipped:
            if (r8 <= 0) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:16:0x006c, code skipped:
            if (r9 >= r15) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:17:0x006e, code skipped:
            r4 = r0 + 1;
     */
    /* JADX WARNING: Missing block: B:18:0x0078, code skipped:
            if (r12.charAt(r9) == r11.compiledPattern[r0]) goto L_0x007c;
     */
    /* JADX WARNING: Missing block: B:19:0x007a, code skipped:
            r0 = r4;
     */
    /* JADX WARNING: Missing block: B:21:0x0081, code skipped:
            r13.index = r14;
            r13.errorIndex = r9;
     */
    /* JADX WARNING: Missing block: B:22:0x0085, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Date parseInternal(String text, ParsePosition pos) {
        CalendarBuilder calb;
        String str = text;
        ParsePosition parsePosition = pos;
        checkNegativeNumberExpression();
        int start = parsePosition.index;
        int oldStart = start;
        int textLength = text.length();
        boolean[] ambiguousYear = new boolean[]{$assertionsDisabled};
        CalendarBuilder calb2 = new CalendarBuilder();
        int start2 = start;
        start = 0;
        while (start < this.compiledPattern.length) {
            int i;
            int count;
            int tag = this.compiledPattern[start] >>> 8;
            int i2 = start + 1;
            start = this.compiledPattern[start] & 255;
            if (start == 255) {
                i = i2 + 1;
                count = (this.compiledPattern[i2] << 16) | this.compiledPattern[i];
                start = i + 1;
            } else {
                count = start;
                start = i2;
            }
            int count2;
            switch (tag) {
                case TAG_QUOTE_ASCII_CHAR /*100*/:
                    if (start2 < textLength && str.charAt(start2) == ((char) count)) {
                        start2++;
                        break;
                    }
                    parsePosition.index = oldStart;
                    parsePosition.errorIndex = start2;
                    return null;
                case TAG_QUOTE_CHARS /*101*/:
                    while (true) {
                        count2 = count - 1;
                        start2++;
                        count = count2;
                        boolean i3 = useFollowingMinusSignAsDelimiter;
                        break;
                    }
                default:
                    boolean useFollowingMinusSignAsDelimiter;
                    boolean obeyCount;
                    boolean obeyCount2 = $assertionsDisabled;
                    if (start < this.compiledPattern.length) {
                        i = this.compiledPattern[start] >>> 8;
                        if (!(i == TAG_QUOTE_ASCII_CHAR || i == TAG_QUOTE_CHARS)) {
                            obeyCount2 = true;
                        }
                        if (this.hasFollowingMinusSign && (i == TAG_QUOTE_ASCII_CHAR || i == TAG_QUOTE_CHARS)) {
                            char c;
                            if (i == TAG_QUOTE_ASCII_CHAR) {
                                c = 255 & this.compiledPattern[start];
                            } else {
                                c = this.compiledPattern[start + 1];
                            }
                            if (c == this.minusSign) {
                                useFollowingMinusSignAsDelimiter = true;
                                obeyCount = obeyCount2;
                                calb = calb2;
                                count2 = subParse(str, start2, tag, count, obeyCount, ambiguousYear, parsePosition, useFollowingMinusSignAsDelimiter, calb2);
                                if (count2 >= 0) {
                                    parsePosition.index = oldStart;
                                    return null;
                                }
                                start2 = count2;
                                continue;
                            }
                        }
                    }
                    obeyCount = obeyCount2;
                    useFollowingMinusSignAsDelimiter = $assertionsDisabled;
                    calb = calb2;
                    count2 = subParse(str, start2, tag, count, obeyCount, ambiguousYear, parsePosition, useFollowingMinusSignAsDelimiter, calb2);
                    if (count2 >= 0) {
                    }
            }
            calb = calb2;
            calb2 = calb;
            str = text;
        }
        int start3 = start2;
        calb = calb2;
        parsePosition.index = start3;
        CalendarBuilder calb3;
        try {
            calb3 = calb;
            try {
                Date parsedDate = calb3.establish(this.calendar).getTime();
                if (ambiguousYear[0] && parsedDate.before(this.defaultCenturyStart)) {
                    parsedDate = calb3.addYear(TAG_QUOTE_ASCII_CHAR).establish(this.calendar).getTime();
                }
                return parsedDate;
            } catch (IllegalArgumentException e) {
                parsePosition.errorIndex = start3;
                parsePosition.index = oldStart;
                return null;
            }
        } catch (IllegalArgumentException e2) {
            calb3 = calb;
            parsePosition.errorIndex = start3;
            parsePosition.index = oldStart;
            return null;
        }
    }

    private int matchString(String text, int start, int field, String[] data, CalendarBuilder calb) {
        int bestMatch;
        int i = start;
        int i2 = field;
        String[] strArr = data;
        int i3 = 0;
        int count = strArr.length;
        if (i2 == 7) {
            i3 = 1;
        }
        int bestMatch2 = -1;
        int i4 = i3;
        int bestMatchLength = 0;
        while (true) {
            bestMatch = bestMatch2;
            if (i4 >= count) {
                break;
            }
            int bestMatchLength2 = strArr[i4].length();
            if (bestMatchLength2 > bestMatchLength) {
                if (text.regionMatches(true, i, strArr[i4], 0, bestMatchLength2)) {
                    bestMatch = i4;
                    bestMatchLength = bestMatchLength2;
                }
            }
            if (strArr[i4].charAt(bestMatchLength2 - 1) == '.' && bestMatchLength2 - 1 > bestMatchLength) {
                if (text.regionMatches(true, i, strArr[i4], 0, bestMatchLength2 - 1)) {
                    bestMatch2 = i4;
                    bestMatchLength = bestMatchLength2 - 1;
                    i4++;
                }
            }
            bestMatch2 = bestMatch;
            i4++;
        }
        if (bestMatch >= 0) {
            calb.set(i2, bestMatch);
            return i + bestMatchLength;
        }
        CalendarBuilder calendarBuilder = calb;
        return -i;
    }

    private int matchString(String text, int start, int field, Map<String, Integer> data, CalendarBuilder calb) {
        if (data != null) {
            String bestMatch = null;
            for (String name : data.keySet()) {
                int length = name.length();
                if ((bestMatch == null || length > bestMatch.length()) && text.regionMatches(true, start, name, 0, length)) {
                    bestMatch = name;
                }
            }
            if (bestMatch != null) {
                calb.set(field, ((Integer) data.get(bestMatch)).intValue());
                return bestMatch.length() + start;
            }
        }
        return -start;
    }

    private int matchZoneString(String text, int start, String[] zoneNames) {
        for (int i = 1; i <= 4; i++) {
            String zoneName = zoneNames[i];
            if (text.regionMatches(true, start, zoneName, 0, zoneName.length())) {
                return i;
            }
        }
        return -1;
    }

    private int subParseZoneString(String text, int start, CalendarBuilder calb) {
        if (this.formatData.isZoneStringsSet) {
            return subParseZoneStringFromSymbols(text, start, calb);
        }
        return subParseZoneStringFromICU(text, start, calb);
    }

    private TimeZoneNames getTimeZoneNames() {
        if (this.timeZoneNames == null) {
            this.timeZoneNames = TimeZoneNames.getInstance(this.locale);
        }
        return this.timeZoneNames;
    }

    private int subParseZoneStringFromICU(String text, int start, CalendarBuilder calb) {
        String currentTimeZoneID = android.icu.util.TimeZone.getCanonicalID(getTimeZone().getID());
        TimeZoneNames tzNames = getTimeZoneNames();
        MatchInfo bestMatch = null;
        Set<String> currentTzMetaZoneIds = null;
        for (MatchInfo match : tzNames.find(text, start, NAME_TYPES)) {
            if (bestMatch == null || bestMatch.matchLength() < match.matchLength()) {
                bestMatch = match;
            } else if (bestMatch.matchLength() != match.matchLength()) {
                continue;
            } else if (currentTimeZoneID.equals(match.tzID())) {
                bestMatch = match;
                break;
            } else if (match.mzID() == null) {
                continue;
            } else {
                if (currentTzMetaZoneIds == null) {
                    currentTzMetaZoneIds = tzNames.getAvailableMetaZoneIDs(currentTimeZoneID);
                }
                if (currentTzMetaZoneIds.contains(match.mzID())) {
                    bestMatch = match;
                    break;
                }
            }
        }
        if (bestMatch == null) {
            return -start;
        }
        String tzId = bestMatch.tzID();
        if (tzId == null) {
            if (currentTzMetaZoneIds == null) {
                currentTzMetaZoneIds = tzNames.getAvailableMetaZoneIDs(currentTimeZoneID);
            }
            if (currentTzMetaZoneIds.contains(bestMatch.mzID())) {
                tzId = currentTimeZoneID;
            } else {
                ULocale uLocale = ULocale.forLocale(this.locale);
                String region = uLocale.getCountry();
                if (region.length() == 0) {
                    region = ULocale.addLikelySubtags(uLocale).getCountry();
                }
                tzId = tzNames.getReferenceZoneID(bestMatch.mzID(), region);
            }
        }
        TimeZone newTimeZone = TimeZone.getTimeZone(tzId);
        if (!currentTimeZoneID.equals(tzId)) {
            setTimeZone(newTimeZone);
        }
        boolean isDst = DST_NAME_TYPES.contains(bestMatch.nameType());
        int dstAmount = isDst ? newTimeZone.getDSTSavings() : 0;
        if (!(isDst && dstAmount == 0)) {
            calb.clear(15).set(16, dstAmount);
        }
        return bestMatch.matchLength() + start;
    }

    private int subParseZoneStringFromSymbols(String text, int start, CalendarBuilder calb) {
        int matchZoneString;
        int matchZoneString2;
        String[] zoneNames;
        boolean useSameName = $assertionsDisabled;
        TimeZone currentTimeZone = getTimeZone();
        int zoneIndex = this.formatData.getZoneIndex(currentTimeZone.getID());
        TimeZone tz = null;
        String[][] zoneStrings = this.formatData.getZoneStringsWrapper();
        String[] zoneNames2 = null;
        int nameIndex = 0;
        int i = 0;
        if (zoneIndex != -1) {
            zoneNames2 = zoneStrings[zoneIndex];
            matchZoneString = matchZoneString(text, start, zoneNames2);
            nameIndex = matchZoneString;
            if (matchZoneString > 0) {
                if (nameIndex <= 2) {
                    useSameName = zoneNames2[nameIndex].equalsIgnoreCase(zoneNames2[nameIndex + 2]);
                }
                tz = TimeZone.getTimeZone(zoneNames2[0]);
            }
        }
        if (tz == null) {
            zoneIndex = this.formatData.getZoneIndex(TimeZone.getDefault().getID());
            if (zoneIndex != -1) {
                zoneNames2 = zoneStrings[zoneIndex];
                matchZoneString2 = matchZoneString(text, start, zoneNames2);
                nameIndex = matchZoneString2;
                if (matchZoneString2 > 0) {
                    if (nameIndex <= 2) {
                        useSameName = zoneNames2[nameIndex].equalsIgnoreCase(zoneNames2[nameIndex + 2]);
                    }
                    tz = TimeZone.getTimeZone(zoneNames2[0]);
                }
            }
        }
        if (tz == null) {
            matchZoneString2 = zoneStrings.length;
            matchZoneString = nameIndex;
            zoneNames = zoneNames2;
            zoneNames2 = null;
            while (zoneNames2 < matchZoneString2) {
                zoneNames = zoneStrings[zoneNames2];
                int matchZoneString3 = matchZoneString(text, start, zoneNames);
                matchZoneString = matchZoneString3;
                if (matchZoneString3 > 0) {
                    if (matchZoneString <= 2) {
                        useSameName = zoneNames[matchZoneString].equalsIgnoreCase(zoneNames[matchZoneString + 2]);
                    }
                    tz = TimeZone.getTimeZone(zoneNames[0]);
                } else {
                    zoneNames2++;
                }
            }
        } else {
            matchZoneString = nameIndex;
            zoneNames = zoneNames2;
        }
        if (tz == null) {
            return -start;
        }
        if (!tz.equals(currentTimeZone)) {
            setTimeZone(tz);
        }
        if (matchZoneString >= 3) {
            i = tz.getDSTSavings();
        }
        matchZoneString2 = i;
        if (!useSameName && (matchZoneString < 3 || matchZoneString2 != 0)) {
            calb.clear(15).set(16, matchZoneString2);
        }
        return zoneNames[matchZoneString].length() + start;
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x0053  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int subParseNumericZone(String text, int start, int sign, int count, boolean colonRequired, CalendarBuilder calb) {
        char c = start;
        int index = c + 1;
        try {
            c = text.charAt(c);
            if (isDigit(c)) {
                int hours = c - 48;
                int index2 = index + 1;
                try {
                    c = text.charAt(index);
                    if (isDigit(c)) {
                        hours = (hours * 10) + (c - 48);
                    } else {
                        index2--;
                    }
                    index = index2;
                    if (hours <= 23) {
                        index2 = 0;
                        if (count != 1) {
                            int index3 = index + 1;
                            try {
                                c = text.charAt(index);
                                if (c == ':') {
                                    index = index3 + 1;
                                    c = text.charAt(index3);
                                    if (isDigit(c)) {
                                        index2 = c - 48;
                                        index3 = index + 1;
                                        c = text.charAt(index);
                                        if (isDigit(c)) {
                                            index2 = (index2 * 10) + (c - 48);
                                            if (index2 <= 59) {
                                                index = index3;
                                            }
                                        }
                                    }
                                } else if (!colonRequired) {
                                    index = index3;
                                    if (isDigit(c)) {
                                    }
                                }
                                index = index3;
                            } catch (IndexOutOfBoundsException e) {
                                index = index3;
                            }
                        }
                        calb.set(15, (MILLIS_PER_MINUTE * (index2 + (hours * 60))) * sign).set(16, 0);
                        return index;
                    }
                } catch (IndexOutOfBoundsException e2) {
                    index = index2;
                }
            }
        } catch (IndexOutOfBoundsException e3) {
        }
        return 1 - index;
    }

    private boolean isDigit(char c) {
        return (c < '0' || c > '9') ? $assertionsDisabled : true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:120:0x0228 A:{Catch:{ IndexOutOfBoundsException -> 0x0240 }} */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x01a0  */
    /* JADX WARNING: Missing block: B:29:0x007f, code skipped:
            if ((r11.calendar instanceof java.util.GregorianCalendar) == false) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:58:0x011c, code skipped:
            r8 = r37;
     */
    /* JADX WARNING: Missing block: B:133:0x025d, code skipped:
            if (r9 <= 12) goto L_0x0262;
     */
    /* JADX WARNING: Missing block: B:146:0x0294, code skipped:
            r27 = r9;
            r8 = r10;
            r4 = r15;
            r15 = r20;
     */
    /* JADX WARNING: Missing block: B:153:0x02cb, code skipped:
            r27 = r9;
            r4 = r15;
            r26 = r21;
            r15 = r8;
            r8 = r10;
     */
    /* JADX WARNING: Missing block: B:208:0x03c6, code skipped:
            r26 = r21;
     */
    /* JADX WARNING: Missing block: B:229:0x0450, code skipped:
            if (r12.charAt(r4.index - 1) == r11.minusSign) goto L_0x0452;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int subParse(String text, int start, int patternCharIndex, int count, boolean obeyCount, boolean[] ambiguousYear, ParsePosition origPos, boolean useFollowingMinusSignAsDelimiter, CalendarBuilder calb) {
        int i;
        int i2;
        ParsePosition pos;
        CalendarBuilder pos2;
        int patternCharIndex2;
        int i3;
        CalendarBuilder calendarBuilder;
        int value;
        String str = text;
        int i4 = start;
        int i5 = count;
        ParsePosition parsePosition = origPos;
        CalendarBuilder calendarBuilder2 = calb;
        int value2 = 0;
        int i6 = 0;
        ParsePosition pos3 = new ParsePosition(0);
        pos3.index = i4;
        int i7 = patternCharIndex;
        if (i7 == 19 && !this.calendar.isWeekDateSupported()) {
            i7 = 1;
        }
        int patternCharIndex3 = i7;
        i7 = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex3];
        while (true) {
            int field = i7;
            if (pos3.index < text.length()) {
                char c = str.charAt(pos3.index);
                if (c != ' ' && c != 9) {
                    Number number;
                    if (patternCharIndex3 == 4 || patternCharIndex3 == 15 || ((patternCharIndex3 == 2 && i5 <= 2) || patternCharIndex3 == 1 || patternCharIndex3 == 19)) {
                        if (obeyCount) {
                            if (i4 + i5 <= text.length()) {
                                number = this.numberFormat.parse(str.substring(0, i4 + i5), pos3);
                            }
                            i = field;
                            i2 = patternCharIndex3;
                            pos = pos3;
                            pos2 = calendarBuilder2;
                            origPos.errorIndex = pos.index;
                            return -1;
                        }
                        number = this.numberFormat.parse(str, pos3);
                        if (number == null) {
                            if (patternCharIndex3 == 1) {
                            }
                            i = field;
                            i2 = patternCharIndex3;
                            pos = pos3;
                            pos2 = calendarBuilder2;
                            origPos.errorIndex = pos.index;
                            return -1;
                        }
                        value2 = number.intValue();
                        if (useFollowingMinusSignAsDelimiter && value2 < 0 && ((pos3.index < text.length() && str.charAt(pos3.index) != this.minusSign) || (pos3.index == text.length() && str.charAt(pos3.index - 1) == this.minusSign))) {
                            value2 = -value2;
                            pos3.index--;
                        }
                    }
                    i6 = value2;
                    boolean useDateFormatSymbols = useDateFormatSymbols();
                    int value3;
                    int index;
                    int value4;
                    ParsePosition pos4;
                    int field2;
                    int field3;
                    int i8;
                    int i9;
                    switch (patternCharIndex3) {
                        case 0:
                            i = field;
                            patternCharIndex2 = patternCharIndex3;
                            parsePosition = pos3;
                            pos2 = calendarBuilder2;
                            value3 = i6;
                            if (useDateFormatSymbols) {
                                value2 = matchString(str, i4, 0, this.formatData.getEras(), pos2);
                                index = value2;
                                if (value2 > 0) {
                                    return index;
                                }
                            }
                            index = matchString(str, i4, i, (Map) this.calendar.getDisplayNames(i, 0, this.locale), pos2);
                            i7 = index;
                            if (index > 0) {
                                return i7;
                            }
                            i3 = value3;
                            pos = parsePosition;
                            break;
                        case 1:
                        case 19:
                            i = field;
                            patternCharIndex2 = patternCharIndex3;
                            value4 = i6;
                            pos4 = pos3;
                            if (this.calendar instanceof GregorianCalendar) {
                                pos2 = calb;
                                parsePosition = pos4;
                                value3 = value4;
                                if (i5 <= 2 && parsePosition.index - i4 == 2 && Character.isDigit(text.charAt(start)) && Character.isDigit(str.charAt(i4 + 1))) {
                                    value2 = this.defaultCenturyStartYear;
                                    i6 = TAG_QUOTE_ASCII_CHAR;
                                    value2 %= TAG_QUOTE_ASCII_CHAR;
                                    ambiguousYear[0] = value3 == value2 ? true : $assertionsDisabled;
                                    index = (this.defaultCenturyStartYear / TAG_QUOTE_ASCII_CHAR) * TAG_QUOTE_ASCII_CHAR;
                                    if (value3 >= value2) {
                                        i6 = 0;
                                    }
                                    i6 = value3 + (index + i6);
                                } else {
                                    i6 = value3;
                                }
                                pos2.set(i, i6);
                                return parsePosition.index;
                            }
                            Map<String, Integer> map = this.calendar.getDisplayNames(i, i5 >= 4 ? 2 : 1, this.locale);
                            if (map != null) {
                                index = matchString(str, i4, i, (Map) map, calb);
                                i7 = index;
                                if (index > 0) {
                                    return i7;
                                }
                            }
                            calb.set(i, value4);
                            return pos4.index;
                        case 2:
                            patternCharIndex2 = patternCharIndex3;
                            ParsePosition pos5 = pos3;
                            calendarBuilder = calendarBuilder2;
                            field2 = field;
                            value3 = i6;
                            ParsePosition parsePosition2 = pos5;
                            pos4 = pos5;
                            value4 = value3;
                            value2 = parseMonth(str, i5, value3, i4, field2, parsePosition2, useDateFormatSymbols, $assertionsDisabled, calb);
                            if (value2 <= 0) {
                                pos2 = calb;
                                i2 = patternCharIndex2;
                                pos = pos4;
                                i3 = value4;
                                break;
                            }
                            return value2;
                        case 4:
                            patternCharIndex2 = patternCharIndex3;
                            i = pos3;
                            calendarBuilder = calendarBuilder2;
                            field2 = field;
                            value3 = i6;
                            if (isLenient() || (value3 >= 1 && value3 <= 24)) {
                                if (value3 == this.calendar.getMaximum(11) + 1) {
                                    i6 = 0;
                                } else {
                                    i6 = value3;
                                }
                                calendarBuilder.set(11, i6);
                                return i.index;
                            }
                        case 9:
                            patternCharIndex2 = patternCharIndex3;
                            i = pos3;
                            calendarBuilder = calendarBuilder2;
                            field2 = field;
                            value3 = i6;
                            value2 = parseWeekday(str, i4, field2, useDateFormatSymbols, $assertionsDisabled, calendarBuilder);
                            if (value2 > 0) {
                                return value2;
                            }
                            break;
                        case 14:
                            field3 = field;
                            patternCharIndex2 = patternCharIndex3;
                            i = pos3;
                            calendarBuilder = calendarBuilder2;
                            value3 = i6;
                            if (useDateFormatSymbols) {
                                value2 = matchString(str, i4, 9, this.formatData.getAmPmStrings(), calendarBuilder);
                                index = value2;
                                if (value2 > 0) {
                                    return index;
                                }
                            }
                            field2 = field3;
                            index = matchString(str, i4, field2, (Map) this.calendar.getDisplayNames(field2, 0, this.locale), calendarBuilder);
                            i7 = index;
                            if (index > 0) {
                                return i7;
                            }
                            break;
                        case 15:
                            field3 = field;
                            patternCharIndex2 = patternCharIndex3;
                            value = i6;
                            i = pos3;
                            calendarBuilder = calendarBuilder2;
                            if (!isLenient()) {
                                value3 = value;
                                if (value3 >= 1) {
                                    break;
                                }
                            }
                            value3 = value;
                            if (value3 == this.calendar.getLeastMaximum(10) + 1) {
                                i6 = 0;
                            } else {
                                i6 = value3;
                            }
                            calendarBuilder.set(10, i6);
                            return i.index;
                        case 17:
                        case 18:
                            field3 = field;
                            patternCharIndex2 = patternCharIndex3;
                            value = i6;
                            i = pos3;
                            calendarBuilder = calendarBuilder2;
                            i6 = 1;
                            field = 0;
                            try {
                                char c2 = str.charAt(i.index);
                                if (c2 != '+') {
                                    if (c2 != '-') {
                                        value3 = field;
                                        if (value3 == 0) {
                                            i8 = i.index + 1;
                                            i.index = i8;
                                            value2 = subParseNumericZone(str, i8, value3, 0, $assertionsDisabled, calendarBuilder);
                                            if (value2 <= 0) {
                                                i.index = -value2;
                                                break;
                                            }
                                            return value2;
                                        }
                                        if (c2 == 'G' || c2 == 'g') {
                                            try {
                                                if (text.length() - i4 >= GMT.length()) {
                                                    char sign = '-';
                                                    i9 = 15;
                                                    if (str.regionMatches(true, i4, GMT, 0, GMT.length())) {
                                                        i.index = GMT.length() + i4;
                                                        if (text.length() - i.index > 0) {
                                                            c2 = str.charAt(i.index);
                                                            if (c2 == '+') {
                                                                value2 = 1;
                                                            } else if (c2 == sign) {
                                                                value2 = -1;
                                                            }
                                                            value3 = value2;
                                                        }
                                                        if (value3 != 0) {
                                                            i8 = i.index + 1;
                                                            i.index = i8;
                                                            value2 = subParseNumericZone(str, i8, value3, 0, $assertionsDisabled, calendarBuilder);
                                                            if (value2 <= 0) {
                                                                i.index = -value2;
                                                                break;
                                                            }
                                                            return value2;
                                                        }
                                                        calendarBuilder.set(15, 0).set(16, 0);
                                                        return i.index;
                                                    }
                                                }
                                            } catch (IndexOutOfBoundsException e) {
                                                break;
                                            }
                                        }
                                        value2 = subParseZoneString(str, i.index, calendarBuilder);
                                        if (value2 <= 0) {
                                            i.index = -value2;
                                            break;
                                        }
                                        return value2;
                                    }
                                    i9 = -1;
                                } else {
                                    i9 = 1;
                                }
                                value3 = i9;
                                if (value3 == 0) {
                                }
                            } catch (IndexOutOfBoundsException e2) {
                                value3 = field;
                                break;
                            }
                        case 21:
                            field3 = field;
                            patternCharIndex2 = patternCharIndex3;
                            value = i6;
                            i = pos3;
                            if (text.length() - i.index > 0) {
                                pos3 = str.charAt(i.index);
                                if (pos3 != 90) {
                                    calendarBuilder = calb;
                                    if (pos3 != 43) {
                                        if (pos3 != 45) {
                                            i.index++;
                                            break;
                                        }
                                        value2 = -1;
                                    } else {
                                        value2 = 1;
                                    }
                                    boolean z = true;
                                    i9 = value2;
                                    i8 = i.index + 1;
                                    i.index = i8;
                                    value2 = subParseNumericZone(str, i8, i9, i5, i5 == 3 ? z : $assertionsDisabled, calendarBuilder);
                                    if (value2 <= 0) {
                                        i.index = -value2;
                                        break;
                                    }
                                    return value2;
                                }
                                calb.set(15, 0).set(16, 0);
                                value2 = i.index + 1;
                                i.index = value2;
                                return value2;
                            }
                            break;
                        case 22:
                            field3 = field;
                            patternCharIndex2 = patternCharIndex3;
                            value = i6;
                            i = pos3;
                            value2 = parseMonth(str, i5, value, i4, field3, pos3, useDateFormatSymbols, true, calb);
                            if (value2 > 0) {
                                return value2;
                            }
                            break;
                        case 23:
                            field3 = field;
                            patternCharIndex2 = patternCharIndex3;
                            value = i6;
                            value2 = parseWeekday(str, i4, field, useDateFormatSymbols, true, calendarBuilder2);
                            if (value2 <= 0) {
                                pos = pos3;
                                pos2 = calendarBuilder2;
                                break;
                            }
                            return value2;
                        default:
                            i = field;
                            patternCharIndex2 = patternCharIndex3;
                            parsePosition = pos3;
                            pos2 = calendarBuilder2;
                            value3 = i6;
                            value2 = parsePosition.getIndex();
                            pos = parsePosition;
                            if (!obeyCount) {
                                number = this.numberFormat.parse(str, pos);
                            } else if (i4 + i5 > text.length()) {
                                i3 = value3;
                                break;
                            } else {
                                number = this.numberFormat.parse(str.substring(0, i4 + i5), pos);
                            }
                            if (number == null) {
                                i3 = value3;
                                i2 = patternCharIndex2;
                                break;
                            }
                            patternCharIndex3 = patternCharIndex2;
                            if (patternCharIndex3 == 8) {
                                c = (int) ((number.doubleValue() / Math.pow(0, (double) (pos.getIndex() - value2))) * 1000.0d);
                            } else {
                                i3 = value3;
                                c = number.intValue();
                            }
                            if (useFollowingMinusSignAsDelimiter && c < 0) {
                                if (pos.index >= text.length() || str.charAt(pos.index) == this.minusSign) {
                                    if (pos.index == text.length()) {
                                        patternCharIndex3 = 1;
                                        break;
                                    }
                                }
                                patternCharIndex3 = 1;
                                c = -c;
                                pos.index -= patternCharIndex3;
                            }
                            pos2.set(i, c);
                            return pos.index;
                            break;
                    }
                }
                i = field;
                i2 = patternCharIndex3;
                field = i6;
                pos = pos3;
                pos2 = calendarBuilder2;
                ParsePosition parsePosition3 = parsePosition;
                pos.index++;
                i6 = field;
                parsePosition = parsePosition3;
                calendarBuilder2 = pos2;
                i7 = i;
                patternCharIndex3 = i2;
                pos3 = pos;
            } else {
                parsePosition.errorIndex = i4;
                return -1;
            }
        }
        pos2 = calendarBuilder;
        pos = i;
        i2 = patternCharIndex2;
        i3 = value;
        value2 = i3;
        origPos.errorIndex = pos.index;
        return -1;
    }

    private int parseMonth(String text, int count, int value, int start, int field, ParsePosition pos, boolean useDateFormatSymbols, boolean standalone, CalendarBuilder out) {
        if (count <= 2) {
            out.set(2, value - 1);
            return pos.index;
        }
        int index;
        ParsePosition parsePosition = pos;
        CalendarBuilder calendarBuilder = out;
        int matchString;
        int i;
        if (useDateFormatSymbols) {
            matchString = matchString(text, start, 2, standalone ? this.formatData.getStandAloneMonths() : this.formatData.getMonths(), calendarBuilder);
            int index2 = matchString;
            if (matchString > 0) {
                return index2;
            }
            matchString = matchString(text, start, 2, standalone ? this.formatData.getShortStandAloneMonths() : this.formatData.getShortMonths(), calendarBuilder);
            index = matchString;
            if (matchString > 0) {
                return index;
            }
            i = field;
        } else {
            i = field;
            matchString = matchString(text, start, i, (Map) this.calendar.getDisplayNames(i, 0, this.locale), calendarBuilder);
            index = matchString;
            if (matchString > 0) {
                return index;
            }
        }
        return index;
    }

    private int parseWeekday(String text, int start, int field, boolean useDateFormatSymbols, boolean standalone, CalendarBuilder out) {
        int index;
        int matchString;
        int index2;
        int i;
        if (useDateFormatSymbols) {
            matchString = matchString(text, start, 7, standalone ? this.formatData.getStandAloneWeekdays() : this.formatData.getWeekdays(), out);
            index2 = matchString;
            if (matchString > 0) {
                return index2;
            }
            matchString = matchString(text, start, 7, standalone ? this.formatData.getShortStandAloneWeekdays() : this.formatData.getShortWeekdays(), out);
            int index3 = matchString;
            if (matchString > 0) {
                return index3;
            }
            i = field;
            index = index3;
        } else {
            index = -1;
            for (int style : new int[]{2, 1}) {
                i = field;
                matchString = matchString(text, start, i, (Map) this.calendar.getDisplayNames(i, style, this.locale), out);
                index = matchString;
                if (matchString > 0) {
                    return index;
                }
            }
            i = field;
        }
        return index;
    }

    private final String getCalendarName() {
        return this.calendar.getClass().getName();
    }

    private boolean useDateFormatSymbols() {
        boolean z = true;
        if (this.useDateFormatSymbols) {
            return true;
        }
        if (!(isGregorianCalendar() || this.locale == null)) {
            z = $assertionsDisabled;
        }
        return z;
    }

    private boolean isGregorianCalendar() {
        return "java.util.GregorianCalendar".equals(getCalendarName());
    }

    private String translatePattern(String pattern, String from, String to) {
        StringBuilder result = new StringBuilder();
        boolean inQuote = $assertionsDisabled;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (inQuote) {
                if (c == '\'') {
                    inQuote = $assertionsDisabled;
                }
            } else if (c == '\'') {
                inQuote = true;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int ci = from.indexOf((int) c);
                if (ci < 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal pattern  character '");
                    stringBuilder.append(c);
                    stringBuilder.append("'");
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (ci < to.length()) {
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
        return translatePattern(this.pattern, "GyMdkHmsSEDFwWahKzZYuXLcbB", this.formatData.getLocalPatternChars());
    }

    public void applyPattern(String pattern) {
        this.compiledPattern = compile(pattern);
        this.pattern = pattern;
    }

    public void applyLocalizedPattern(String pattern) {
        String p = translatePattern(pattern, this.formatData.getLocalPatternChars(), "GyMdkHmsSEDFwWahKzZYuXLcbB");
        this.compiledPattern = compile(p);
        this.pattern = p;
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        this.formatData = (DateFormatSymbols) newFormatSymbols.clone();
        this.useDateFormatSymbols = true;
    }

    public Object clone() {
        SimpleDateFormat other = (SimpleDateFormat) super.clone();
        other.formatData = (DateFormatSymbols) this.formatData.clone();
        return other;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    public boolean equals(Object obj) {
        boolean equals = super.equals(obj);
        boolean z = $assertionsDisabled;
        if (!equals) {
            return $assertionsDisabled;
        }
        SimpleDateFormat that = (SimpleDateFormat) obj;
        if (this.pattern.equals(that.pattern) && this.formatData.equals(that.formatData)) {
            z = true;
        }
        return z;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        try {
            this.compiledPattern = compile(this.pattern);
            if (this.serialVersionOnStream < 1) {
                initializeDefaultCentury();
            } else {
                parseAmbiguousDatesAsAfter(this.defaultCenturyStart);
            }
            this.serialVersionOnStream = 1;
            TimeZone tz = getTimeZone();
            if (tz instanceof SimpleTimeZone) {
                String id = tz.getID();
                TimeZone zi = TimeZone.getTimeZone(id);
                if (zi != null && zi.hasSameRules(tz) && zi.getID().equals(id)) {
                    setTimeZone(zi);
                }
            }
        } catch (Exception e) {
            throw new InvalidObjectException("invalid pattern");
        }
    }

    private void checkNegativeNumberExpression() {
        if ((this.numberFormat instanceof DecimalFormat) && !this.numberFormat.equals(this.originalNumberFormat)) {
            String numberPattern = ((DecimalFormat) this.numberFormat).toPattern();
            if (!numberPattern.equals(this.originalNumberPattern)) {
                this.hasFollowingMinusSign = $assertionsDisabled;
                int separatorIndex = numberPattern.indexOf(59);
                if (separatorIndex > -1) {
                    int minusIndex = numberPattern.indexOf(45, separatorIndex);
                    if (minusIndex > numberPattern.lastIndexOf(48) && minusIndex > numberPattern.lastIndexOf(35)) {
                        this.hasFollowingMinusSign = true;
                        this.minusSign = ((DecimalFormat) this.numberFormat).getDecimalFormatSymbols().getMinusSign();
                    }
                }
                this.originalNumberPattern = numberPattern;
            }
            this.originalNumberFormat = this.numberFormat;
        }
    }
}
