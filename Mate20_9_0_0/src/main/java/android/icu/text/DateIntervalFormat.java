package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.text.DateIntervalInfo.PatternInfo;
import android.icu.text.MessagePattern.ApostropheMode;
import android.icu.util.Calendar;
import android.icu.util.DateInterval;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DateIntervalFormat extends UFormat {
    private static ICUCache<String, Map<String, PatternInfo>> LOCAL_PATTERN_CACHE = new SimpleCache();
    private static final long serialVersionUID = 1;
    private SimpleDateFormat fDateFormat;
    private String fDatePattern = null;
    private String fDateTimeFormat = null;
    private Calendar fFromCalendar;
    private DateIntervalInfo fInfo;
    private transient Map<String, PatternInfo> fIntervalPatterns = null;
    private String fSkeleton = null;
    private String fTimePattern = null;
    private Calendar fToCalendar;
    private boolean isDateIntervalInfoDefault;

    static final class BestMatchInfo {
        final int bestMatchDistanceInfo;
        final String bestMatchSkeleton;

        BestMatchInfo(String bestSkeleton, int difference) {
            this.bestMatchSkeleton = bestSkeleton;
            this.bestMatchDistanceInfo = difference;
        }
    }

    private static final class SkeletonAndItsBestMatch {
        final String bestMatchSkeleton;
        final String skeleton;

        SkeletonAndItsBestMatch(String skeleton, String bestMatch) {
            this.skeleton = skeleton;
            this.bestMatchSkeleton = bestMatch;
        }
    }

    private DateIntervalFormat() {
    }

    @Deprecated
    public DateIntervalFormat(String skeleton, DateIntervalInfo dtItvInfo, SimpleDateFormat simpleDateFormat) {
        this.fDateFormat = simpleDateFormat;
        dtItvInfo.freeze();
        this.fSkeleton = skeleton;
        this.fInfo = dtItvInfo;
        this.isDateIntervalInfoDefault = false;
        this.fFromCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        this.fToCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        initializePattern(null);
    }

    private DateIntervalFormat(String skeleton, ULocale locale, SimpleDateFormat simpleDateFormat) {
        this.fDateFormat = simpleDateFormat;
        this.fSkeleton = skeleton;
        this.fInfo = new DateIntervalInfo(locale).freeze();
        this.isDateIntervalInfoDefault = true;
        this.fFromCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        this.fToCalendar = (Calendar) this.fDateFormat.getCalendar().clone();
        initializePattern(LOCAL_PATTERN_CACHE);
    }

    public static final DateIntervalFormat getInstance(String skeleton) {
        return getInstance(skeleton, ULocale.getDefault(Category.FORMAT));
    }

    public static final DateIntervalFormat getInstance(String skeleton, Locale locale) {
        return getInstance(skeleton, ULocale.forLocale(locale));
    }

    public static final DateIntervalFormat getInstance(String skeleton, ULocale locale) {
        return new DateIntervalFormat(skeleton, locale, new SimpleDateFormat(DateTimePatternGenerator.getInstance(locale).getBestPattern(skeleton), locale));
    }

    public static final DateIntervalFormat getInstance(String skeleton, DateIntervalInfo dtitvinf) {
        return getInstance(skeleton, ULocale.getDefault(Category.FORMAT), dtitvinf);
    }

    public static final DateIntervalFormat getInstance(String skeleton, Locale locale, DateIntervalInfo dtitvinf) {
        return getInstance(skeleton, ULocale.forLocale(locale), dtitvinf);
    }

    public static final DateIntervalFormat getInstance(String skeleton, ULocale locale, DateIntervalInfo dtitvinf) {
        return new DateIntervalFormat(skeleton, (DateIntervalInfo) dtitvinf.clone(), new SimpleDateFormat(DateTimePatternGenerator.getInstance(locale).getBestPattern(skeleton), locale));
    }

    public synchronized Object clone() {
        DateIntervalFormat other;
        other = (DateIntervalFormat) super.clone();
        other.fDateFormat = (SimpleDateFormat) this.fDateFormat.clone();
        other.fInfo = (DateIntervalInfo) this.fInfo.clone();
        other.fFromCalendar = (Calendar) this.fFromCalendar.clone();
        other.fToCalendar = (Calendar) this.fToCalendar.clone();
        other.fDatePattern = this.fDatePattern;
        other.fTimePattern = this.fTimePattern;
        other.fDateTimeFormat = this.fDateTimeFormat;
        return other;
    }

    public final StringBuffer format(Object obj, StringBuffer appendTo, FieldPosition fieldPosition) {
        if (obj instanceof DateInterval) {
            return format((DateInterval) obj, appendTo, fieldPosition);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot format given Object (");
        stringBuilder.append(obj.getClass().getName());
        stringBuilder.append(") as a DateInterval");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public final synchronized StringBuffer format(DateInterval dtInterval, StringBuffer appendTo, FieldPosition fieldPosition) {
        this.fFromCalendar.setTimeInMillis(dtInterval.getFromDate());
        this.fToCalendar.setTimeInMillis(dtInterval.getToDate());
        return format(this.fFromCalendar, this.fToCalendar, appendTo, fieldPosition);
    }

    @Deprecated
    public String getPatterns(Calendar fromCalendar, Calendar toCalendar, Output<String> part2) {
        int field;
        if (fromCalendar.get(0) != toCalendar.get(0)) {
            field = 0;
        } else if (fromCalendar.get(1) != toCalendar.get(1)) {
            field = 1;
        } else if (fromCalendar.get(2) != toCalendar.get(2)) {
            field = 2;
        } else if (fromCalendar.get(5) != toCalendar.get(5)) {
            field = 5;
        } else if (fromCalendar.get(9) != toCalendar.get(9)) {
            field = 9;
        } else if (fromCalendar.get(10) != toCalendar.get(10)) {
            field = 10;
        } else if (fromCalendar.get(12) != toCalendar.get(12)) {
            field = 12;
        } else if (fromCalendar.get(13) == toCalendar.get(13)) {
            return null;
        } else {
            field = 13;
        }
        PatternInfo intervalPattern = (PatternInfo) this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        part2.value = intervalPattern.getSecondPart();
        return intervalPattern.getFirstPart();
    }

    /* JADX WARNING: Removed duplicated region for block: B:49:0x00b9 A:{SYNTHETIC, Splitter:B:49:0x00b9} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x009e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final synchronized StringBuffer format(Calendar fromCalendar, Calendar toCalendar, StringBuffer appendTo, FieldPosition pos) {
        Calendar calendar = fromCalendar;
        Calendar calendar2 = toCalendar;
        StringBuffer stringBuffer = appendTo;
        FieldPosition fieldPosition = pos;
        synchronized (this) {
            if (fromCalendar.isEquivalentTo(toCalendar)) {
                int field;
                boolean fromToOnSameDay;
                PatternInfo intervalPattern;
                if (calendar.get(0) != calendar2.get(0)) {
                    field = 0;
                } else if (calendar.get(1) != calendar2.get(1)) {
                    field = 1;
                } else if (calendar.get(2) != calendar2.get(2)) {
                    field = 2;
                } else if (calendar.get(5) != calendar2.get(5)) {
                    field = 5;
                } else if (calendar.get(9) != calendar2.get(9)) {
                    field = 9;
                } else if (calendar.get(10) != calendar2.get(10)) {
                    field = 10;
                } else if (calendar.get(12) != calendar2.get(12)) {
                    field = 12;
                } else if (calendar.get(13) != calendar2.get(13)) {
                    field = 13;
                } else {
                    StringBuffer format = this.fDateFormat.format(calendar, stringBuffer, fieldPosition);
                    return format;
                }
                int field2 = field;
                if (!(field2 == 9 || field2 == 10 || field2 == 12)) {
                    if (field2 != 13) {
                        fromToOnSameDay = false;
                        intervalPattern = (PatternInfo) this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field2]);
                        StringBuffer format2;
                        if (intervalPattern != null) {
                            if (this.fDateFormat.isFieldUnitIgnored(field2)) {
                                format2 = this.fDateFormat.format(calendar, stringBuffer, fieldPosition);
                                return format2;
                            }
                            format2 = fallbackFormat(calendar, calendar2, fromToOnSameDay, stringBuffer, fieldPosition);
                            return format2;
                        } else if (intervalPattern.getFirstPart() == null) {
                            format2 = fallbackFormat(calendar, calendar2, fromToOnSameDay, stringBuffer, fieldPosition, intervalPattern.getSecondPart());
                            return format2;
                        } else {
                            Calendar secondCal;
                            PatternInfo intervalPattern2 = intervalPattern;
                            int i = field2;
                            if (intervalPattern2.firstDateInPtnIsLaterDate()) {
                                field = calendar2;
                                secondCal = calendar;
                            } else {
                                field = calendar;
                                secondCal = calendar2;
                            }
                            String originalPattern = this.fDateFormat.toPattern();
                            this.fDateFormat.applyPattern(intervalPattern2.getFirstPart());
                            this.fDateFormat.format(field, stringBuffer, fieldPosition);
                            if (intervalPattern2.getSecondPart() != null) {
                                this.fDateFormat.applyPattern(intervalPattern2.getSecondPart());
                                FieldPosition otherPos = new FieldPosition(pos.getField());
                                this.fDateFormat.format(secondCal, stringBuffer, otherPos);
                                if (pos.getEndIndex() == 0 && otherPos.getEndIndex() > 0) {
                                    fieldPosition.setBeginIndex(otherPos.getBeginIndex());
                                    fieldPosition.setEndIndex(otherPos.getEndIndex());
                                }
                            }
                            this.fDateFormat.applyPattern(originalPattern);
                            return stringBuffer;
                        }
                    }
                }
                fromToOnSameDay = true;
                intervalPattern = (PatternInfo) this.fIntervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field2]);
                if (intervalPattern != null) {
                }
            } else {
                throw new IllegalArgumentException("can not format on two different calendars");
            }
        }
    }

    private void adjustPosition(String combiningPattern, String pat0, FieldPosition pos0, String pat1, FieldPosition pos1, FieldPosition posResult) {
        int index0 = combiningPattern.indexOf("{0}");
        int index1 = combiningPattern.indexOf("{1}");
        if (index0 >= 0 && index1 >= 0) {
            if (index0 < index1) {
                if (pos0.getEndIndex() > 0) {
                    posResult.setBeginIndex(pos0.getBeginIndex() + index0);
                    posResult.setEndIndex(pos0.getEndIndex() + index0);
                } else if (pos1.getEndIndex() > 0) {
                    index1 += pat0.length() - 3;
                    posResult.setBeginIndex(pos1.getBeginIndex() + index1);
                    posResult.setEndIndex(pos1.getEndIndex() + index1);
                }
            } else if (pos1.getEndIndex() > 0) {
                posResult.setBeginIndex(pos1.getBeginIndex() + index1);
                posResult.setEndIndex(pos1.getEndIndex() + index1);
            } else if (pos0.getEndIndex() > 0) {
                index0 += pat1.length() - 3;
                posResult.setBeginIndex(pos0.getBeginIndex() + index0);
                posResult.setEndIndex(pos0.getEndIndex() + index0);
            }
        }
    }

    private final StringBuffer fallbackFormat(Calendar fromCalendar, Calendar toCalendar, boolean fromToOnSameDay, StringBuffer appendTo, FieldPosition pos) {
        Calendar calendar = fromCalendar;
        StringBuffer stringBuffer = appendTo;
        String fullPattern = null;
        boolean z = (!fromToOnSameDay || this.fDatePattern == null || this.fTimePattern == null) ? false : true;
        boolean formatDatePlusTimeRange = z;
        if (formatDatePlusTimeRange) {
            fullPattern = this.fDateFormat.toPattern();
            this.fDateFormat.applyPattern(this.fTimePattern);
        }
        String fullPattern2 = fullPattern;
        FieldPosition otherPos = new FieldPosition(pos.getField());
        FieldPosition fieldPosition = pos;
        StringBuffer earlierDate = this.fDateFormat.format(calendar, new StringBuffer(64), fieldPosition);
        StringBuffer laterDate = this.fDateFormat.format(toCalendar, new StringBuffer(64), otherPos);
        String fallbackIntervalPattern = this.fInfo.getFallbackIntervalPattern();
        String fallbackPattern = fallbackIntervalPattern;
        adjustPosition(fallbackIntervalPattern, earlierDate.toString(), fieldPosition, laterDate.toString(), otherPos, pos);
        String fallbackPattern2 = fallbackPattern;
        String fallbackRange = SimpleFormatterImpl.formatRawPattern(fallbackPattern2, 2, 2, earlierDate, laterDate);
        if (formatDatePlusTimeRange) {
            this.fDateFormat.applyPattern(this.fDatePattern);
            StringBuffer datePortion = new StringBuffer(64);
            otherPos.setBeginIndex(0);
            otherPos.setEndIndex(0);
            earlierDate = this.fDateFormat.format(calendar, datePortion, otherPos);
            StringBuffer datePortion2 = earlierDate;
            fallbackPattern = fallbackPattern2;
            int i = 2;
            adjustPosition(this.fDateTimeFormat, fallbackRange, pos, earlierDate.toString(), otherPos, pos);
            MessageFormat msgFmt = new MessageFormat("");
            msgFmt.applyPattern(this.fDateTimeFormat, ApostropheMode.DOUBLE_REQUIRED);
            StringBuffer fallbackRangeBuffer = new StringBuffer(128);
            Object[] objArr = new Object[i];
            objArr[0] = fallbackRange;
            objArr[1] = datePortion2;
            fullPattern = msgFmt.format(objArr, fallbackRangeBuffer, new FieldPosition(0)).toString();
        } else {
            fullPattern = fallbackRange;
        }
        stringBuffer.append(fullPattern);
        if (formatDatePlusTimeRange) {
            this.fDateFormat.applyPattern(fullPattern2);
        }
        return stringBuffer;
    }

    private final StringBuffer fallbackFormat(Calendar fromCalendar, Calendar toCalendar, boolean fromToOnSameDay, StringBuffer appendTo, FieldPosition pos, String fullPattern) {
        String originalPattern = this.fDateFormat.toPattern();
        this.fDateFormat.applyPattern(fullPattern);
        fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos);
        this.fDateFormat.applyPattern(originalPattern);
        return appendTo;
    }

    @Deprecated
    public Object parseObject(String source, ParsePosition parse_pos) {
        throw new UnsupportedOperationException("parsing is not supported");
    }

    public DateIntervalInfo getDateIntervalInfo() {
        return (DateIntervalInfo) this.fInfo.clone();
    }

    public void setDateIntervalInfo(DateIntervalInfo newItvPattern) {
        this.fInfo = (DateIntervalInfo) newItvPattern.clone();
        this.isDateIntervalInfoDefault = false;
        this.fInfo.freeze();
        if (this.fDateFormat != null) {
            initializePattern(null);
        }
    }

    public TimeZone getTimeZone() {
        if (this.fDateFormat != null) {
            return (TimeZone) this.fDateFormat.getTimeZone().clone();
        }
        return TimeZone.getDefault();
    }

    public void setTimeZone(TimeZone zone) {
        TimeZone zoneToSet = (TimeZone) zone.clone();
        if (this.fDateFormat != null) {
            this.fDateFormat.setTimeZone(zoneToSet);
        }
        if (this.fFromCalendar != null) {
            this.fFromCalendar.setTimeZone(zoneToSet);
        }
        if (this.fToCalendar != null) {
            this.fToCalendar.setTimeZone(zoneToSet);
        }
    }

    public synchronized DateFormat getDateFormat() {
        return (DateFormat) this.fDateFormat.clone();
    }

    private void initializePattern(ICUCache<String, Map<String, PatternInfo>> cache) {
        String fullPattern = this.fDateFormat.toPattern();
        ULocale locale = this.fDateFormat.getLocale();
        String key = null;
        Map<String, PatternInfo> patterns = null;
        if (cache != null) {
            StringBuilder stringBuilder;
            if (this.fSkeleton != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(locale.toString());
                stringBuilder.append("+");
                stringBuilder.append(fullPattern);
                stringBuilder.append("+");
                stringBuilder.append(this.fSkeleton);
                key = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(locale.toString());
                stringBuilder.append("+");
                stringBuilder.append(fullPattern);
                key = stringBuilder.toString();
            }
            patterns = (Map) cache.get(key);
        }
        if (patterns == null) {
            patterns = Collections.unmodifiableMap(initializeIntervalPattern(fullPattern, locale));
            if (cache != null) {
                cache.put(key, patterns);
            }
        }
        this.fIntervalPatterns = patterns;
    }

    private Map<String, PatternInfo> initializeIntervalPattern(String fullPattern, ULocale locale) {
        String str;
        DateTimePatternGenerator dtpng = DateTimePatternGenerator.getInstance(locale);
        if (this.fSkeleton == null) {
            this.fSkeleton = dtpng.getSkeleton(fullPattern);
        } else {
            str = fullPattern;
        }
        String skeleton = this.fSkeleton;
        HashMap<String, PatternInfo> intervalPatterns = new HashMap();
        StringBuilder date = new StringBuilder(skeleton.length());
        StringBuilder normalizedDate = new StringBuilder(skeleton.length());
        StringBuilder time = new StringBuilder(skeleton.length());
        StringBuilder normalizedTime = new StringBuilder(skeleton.length());
        getDateTimeSkeleton(skeleton, date, normalizedDate, time, normalizedTime);
        String dateSkeleton = date.toString();
        String timeSkeleton = time.toString();
        String normalizedDateSkeleton = normalizedDate.toString();
        String normalizedTimeSkeleton = normalizedTime.toString();
        if (time.length() == 0 || date.length() == 0) {
            ULocale uLocale = locale;
        } else {
            this.fDateTimeFormat = getConcatenationPattern(locale);
        }
        StringBuilder stringBuilder;
        PatternInfo ptn;
        if (genSeparateDateTimePtn(normalizedDateSkeleton, normalizedTimeSkeleton, intervalPatterns, dtpng)) {
            stringBuilder = normalizedTime;
            if (time.length() != 0) {
                if (date.length() == 0) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(DateFormat.YEAR_NUM_MONTH_DAY);
                    stringBuilder2.append(timeSkeleton);
                    ptn = new PatternInfo(null, dtpng.getBestPattern(stringBuilder2.toString()), this.fInfo.getDefaultOrder());
                    intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5], ptn);
                    intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2], ptn);
                    intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1], ptn);
                } else {
                    if (!fieldExistsInSkeleton(5, dateSkeleton)) {
                        normalizedDate = new StringBuilder();
                        normalizedDate.append(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5]);
                        normalizedDate.append(skeleton);
                        skeleton = normalizedDate.toString();
                        genFallbackPattern(5, skeleton, intervalPatterns, dtpng);
                    }
                    if (!fieldExistsInSkeleton(2, dateSkeleton)) {
                        normalizedDate = new StringBuilder();
                        normalizedDate.append(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2]);
                        normalizedDate.append(skeleton);
                        skeleton = normalizedDate.toString();
                        genFallbackPattern(2, skeleton, intervalPatterns, dtpng);
                    }
                    if (!fieldExistsInSkeleton(1, dateSkeleton)) {
                        normalizedDate = new StringBuilder();
                        normalizedDate.append(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1]);
                        normalizedDate.append(skeleton);
                        genFallbackPattern(1, normalizedDate.toString(), intervalPatterns, dtpng);
                    }
                    if (this.fDateTimeFormat == null) {
                        this.fDateTimeFormat = "{1} {0}";
                    }
                    str = dtpng.getBestPattern(dateSkeleton);
                    concatSingleDate2TimeInterval(this.fDateTimeFormat, str, 9, intervalPatterns);
                    concatSingleDate2TimeInterval(this.fDateTimeFormat, str, 10, intervalPatterns);
                    concatSingleDate2TimeInterval(this.fDateTimeFormat, str, 12, intervalPatterns);
                }
            }
            return intervalPatterns;
        }
        if (time.length() == 0 || date.length() != 0) {
            stringBuilder = normalizedTime;
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(DateFormat.YEAR_NUM_MONTH_DAY);
            stringBuilder3.append(timeSkeleton);
            ptn = new PatternInfo(null, dtpng.getBestPattern(stringBuilder3.toString()), this.fInfo.getDefaultOrder());
            intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[5], ptn);
            intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[2], ptn);
            intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[1], ptn);
        }
        return intervalPatterns;
    }

    private String getConcatenationPattern(ULocale locale) {
        ICUResourceBundle concatenationPatternRb = (ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale)).getWithFallback("calendar/gregorian/DateTimePatterns").get(8);
        if (concatenationPatternRb.getType() == 0) {
            return concatenationPatternRb.getString();
        }
        return concatenationPatternRb.getString(0);
    }

    private void genFallbackPattern(int field, String skeleton, Map<String, PatternInfo> intervalPatterns, DateTimePatternGenerator dtpng) {
        intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], new PatternInfo(null, dtpng.getBestPattern(skeleton), this.fInfo.getDefaultOrder()));
    }

    private static void getDateTimeSkeleton(String skeleton, StringBuilder dateSkeleton, StringBuilder normalizedDateSkeleton, StringBuilder timeSkeleton, StringBuilder normalizedTimeSkeleton) {
        int i;
        StringBuilder stringBuilder = dateSkeleton;
        StringBuilder stringBuilder2 = normalizedDateSkeleton;
        StringBuilder stringBuilder3 = timeSkeleton;
        StringBuilder stringBuilder4 = normalizedTimeSkeleton;
        int ECount = 0;
        int dCount = 0;
        int MCount = 0;
        int yCount = 0;
        int hCount = 0;
        int HCount = 0;
        int mCount = 0;
        int vCount = 0;
        int zCount = 0;
        for (i = 0; i < skeleton.length(); i++) {
            char ch = skeleton.charAt(i);
            switch (ch) {
                case 'A':
                case 'K':
                case 'S':
                case 'V':
                case 'Z':
                case 'j':
                case 'k':
                case 's':
                    stringBuilder3.append(ch);
                    stringBuilder4.append(ch);
                    break;
                case 'D':
                case 'F':
                case 'G':
                case 'L':
                case 'Q':
                case 'U':
                case 'W':
                case 'Y':
                case 'c':
                case 'e':
                case 'g':
                case 'l':
                case 'q':
                case 'r':
                case 'u':
                case 'w':
                    stringBuilder2.append(ch);
                    stringBuilder.append(ch);
                    break;
                case 'E':
                    stringBuilder.append(ch);
                    ECount++;
                    break;
                case 'H':
                    stringBuilder3.append(ch);
                    HCount++;
                    break;
                case 'M':
                    stringBuilder.append(ch);
                    MCount++;
                    break;
                case 'a':
                    stringBuilder3.append(ch);
                    break;
                case 'd':
                    stringBuilder.append(ch);
                    dCount++;
                    break;
                case 'h':
                    stringBuilder3.append(ch);
                    hCount++;
                    break;
                case 'm':
                    stringBuilder3.append(ch);
                    mCount++;
                    break;
                case 'v':
                    vCount++;
                    stringBuilder3.append(ch);
                    break;
                case 'y':
                    stringBuilder.append(ch);
                    yCount++;
                    break;
                case 'z':
                    zCount++;
                    stringBuilder3.append(ch);
                    break;
                default:
                    break;
            }
        }
        String str = skeleton;
        if (yCount != 0) {
            for (i = 0; i < yCount; i++) {
                stringBuilder2.append('y');
            }
        }
        if (MCount != 0) {
            if (MCount < 3) {
                stringBuilder2.append('M');
            } else {
                i = 0;
                while (i < MCount && i < 5) {
                    stringBuilder2.append('M');
                    i++;
                }
            }
        }
        if (ECount != 0) {
            if (ECount <= 3) {
                stringBuilder2.append('E');
            } else {
                i = 0;
                while (i < ECount && i < 5) {
                    stringBuilder2.append('E');
                    i++;
                }
            }
        }
        if (dCount != 0) {
            stringBuilder2.append('d');
        }
        if (HCount != 0) {
            stringBuilder4.append('H');
        } else if (hCount != 0) {
            stringBuilder4.append('h');
        }
        if (mCount != 0) {
            stringBuilder4.append('m');
        }
        if (zCount != 0) {
            stringBuilder4.append('z');
        }
        if (vCount != 0) {
            stringBuilder4.append('v');
        }
    }

    private boolean genSeparateDateTimePtn(String dateSkeleton, String timeSkeleton, Map<String, PatternInfo> intervalPatterns, DateTimePatternGenerator dtpng) {
        String skeleton;
        if (timeSkeleton.length() != 0) {
            skeleton = timeSkeleton;
        } else {
            skeleton = dateSkeleton;
        }
        BestMatchInfo retValue = this.fInfo.getBestSkeleton(skeleton);
        String bestSkeleton = retValue.bestMatchSkeleton;
        int differenceInfo = retValue.bestMatchDistanceInfo;
        if (dateSkeleton.length() != 0) {
            this.fDatePattern = dtpng.getBestPattern(dateSkeleton);
        }
        if (timeSkeleton.length() != 0) {
            this.fTimePattern = dtpng.getBestPattern(timeSkeleton);
        }
        if (differenceInfo == -1) {
            return false;
        }
        String str;
        String str2;
        int i;
        Map<String, PatternInfo> map;
        if (timeSkeleton.length() == 0) {
            str = skeleton;
            str2 = bestSkeleton;
            i = differenceInfo;
            map = intervalPatterns;
            genIntervalPattern(5, str, str2, i, map);
            SkeletonAndItsBestMatch skeletons = genIntervalPattern(2, str, str2, i, map);
            if (skeletons != null) {
                bestSkeleton = skeletons.skeleton;
                skeleton = skeletons.bestMatchSkeleton;
            }
            genIntervalPattern(1, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
        } else {
            str = skeleton;
            str2 = bestSkeleton;
            i = differenceInfo;
            map = intervalPatterns;
            genIntervalPattern(12, str, str2, i, map);
            genIntervalPattern(10, str, str2, i, map);
            genIntervalPattern(9, str, str2, i, map);
        }
        return true;
    }

    private SkeletonAndItsBestMatch genIntervalPattern(int field, String skeleton, String bestSkeleton, int differenceInfo, Map<String, PatternInfo> intervalPatterns) {
        SkeletonAndItsBestMatch retValue = null;
        PatternInfo pattern = this.fInfo.getIntervalPattern(bestSkeleton, field);
        if (pattern == null) {
            if (SimpleDateFormat.isFieldUnitIgnored(bestSkeleton, field)) {
                intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], new PatternInfo(this.fDateFormat.toPattern(), null, this.fInfo.getDefaultOrder()));
                return null;
            } else if (field == 9) {
                pattern = this.fInfo.getIntervalPattern(bestSkeleton, 10);
                if (pattern != null) {
                    intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], pattern);
                }
                return null;
            } else {
                String fieldLetter = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(fieldLetter);
                stringBuilder.append(bestSkeleton);
                bestSkeleton = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(fieldLetter);
                stringBuilder.append(skeleton);
                skeleton = stringBuilder.toString();
                pattern = this.fInfo.getIntervalPattern(bestSkeleton, field);
                if (pattern == null && differenceInfo == 0) {
                    BestMatchInfo tmpRetValue = this.fInfo.getBestSkeleton(skeleton);
                    String tmpBestSkeleton = tmpRetValue.bestMatchSkeleton;
                    differenceInfo = tmpRetValue.bestMatchDistanceInfo;
                    if (!(tmpBestSkeleton.length() == 0 || differenceInfo == -1)) {
                        pattern = this.fInfo.getIntervalPattern(tmpBestSkeleton, field);
                        bestSkeleton = tmpBestSkeleton;
                    }
                }
                if (pattern != null) {
                    retValue = new SkeletonAndItsBestMatch(skeleton, bestSkeleton);
                }
            }
        }
        if (pattern != null) {
            if (differenceInfo != 0) {
                pattern = new PatternInfo(adjustFieldWidth(skeleton, bestSkeleton, pattern.getFirstPart(), differenceInfo), adjustFieldWidth(skeleton, bestSkeleton, pattern.getSecondPart(), differenceInfo), pattern.firstDateInPtnIsLaterDate());
            }
            intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], pattern);
        }
        return retValue;
    }

    /* JADX WARNING: Missing block: B:44:0x009b, code skipped:
            if (r13 > 'z') goto L_0x00a0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String adjustFieldWidth(String inputSkeleton, String bestMatchSkeleton, String bestMatchIntervalPattern, int differenceInfo) {
        String bestMatchIntervalPattern2 = bestMatchIntervalPattern;
        if (bestMatchIntervalPattern2 == null) {
            return null;
        }
        int fieldCount;
        int[] inputSkeletonFieldWidth = new int[58];
        int[] bestMatchSkeletonFieldWidth = new int[58];
        DateIntervalInfo.parseSkeleton(inputSkeleton, inputSkeletonFieldWidth);
        DateIntervalInfo.parseSkeleton(bestMatchSkeleton, bestMatchSkeletonFieldWidth);
        if (differenceInfo == 2) {
            bestMatchIntervalPattern2 = bestMatchIntervalPattern2.replace('v', 'z');
        }
        StringBuilder adjustedPtn = new StringBuilder(bestMatchIntervalPattern2);
        char prevCh = 0;
        int count = 0;
        int adjustedPtnLength = adjustedPtn.length();
        boolean inQuote = false;
        int i = 0;
        while (i < adjustedPtnLength) {
            String bestMatchIntervalPattern3;
            String str;
            char ch = adjustedPtn.charAt(i);
            if (ch == prevCh || count <= 0) {
                bestMatchIntervalPattern3 = bestMatchIntervalPattern2;
            } else {
                char skeletonChar;
                char skeletonChar2 = prevCh;
                if (skeletonChar2 == 'L') {
                    skeletonChar = 'M';
                } else {
                    skeletonChar = skeletonChar2;
                }
                fieldCount = bestMatchSkeletonFieldWidth[skeletonChar - 65];
                int inputFieldCount = inputSkeletonFieldWidth[skeletonChar - 65];
                if (fieldCount != count || inputFieldCount <= fieldCount) {
                    bestMatchIntervalPattern3 = bestMatchIntervalPattern2;
                } else {
                    count = inputFieldCount - fieldCount;
                    int j = 0;
                    while (true) {
                        bestMatchIntervalPattern3 = bestMatchIntervalPattern2;
                        bestMatchIntervalPattern2 = j;
                        if (bestMatchIntervalPattern2 >= count) {
                            break;
                        }
                        adjustedPtn.insert(i, prevCh);
                        j = bestMatchIntervalPattern2 + 1;
                        bestMatchIntervalPattern2 = bestMatchIntervalPattern3;
                    }
                    i += count;
                    adjustedPtnLength += count;
                }
                count = 0;
            }
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                if (i + 1 >= adjustedPtn.length() || adjustedPtn.charAt(i + 1) != PatternTokenizer.SINGLE_QUOTE) {
                    inQuote = !inQuote ? true : null;
                } else {
                    i++;
                }
            } else if (!inQuote) {
                bestMatchIntervalPattern2 = ch >= 'a' ? 122 : 122;
                if (ch >= 'A') {
                    if (ch > 'Z') {
                    }
                    count++;
                    prevCh = ch;
                }
                i++;
                str = bestMatchIntervalPattern2;
                bestMatchIntervalPattern2 = bestMatchIntervalPattern3;
            }
            bestMatchIntervalPattern2 = 122;
            i++;
            str = bestMatchIntervalPattern2;
            bestMatchIntervalPattern2 = bestMatchIntervalPattern3;
        }
        if (count > 0) {
            char skeletonChar3 = prevCh;
            if (skeletonChar3 == 'L') {
                skeletonChar3 = 'M';
            }
            fieldCount = bestMatchSkeletonFieldWidth[skeletonChar3 - 65];
            i = inputSkeletonFieldWidth[skeletonChar3 - 65];
            if (fieldCount == count && i > fieldCount) {
                count = i - fieldCount;
                int j2 = 0;
                while (true) {
                    int j3 = j2;
                    if (j3 >= count) {
                        break;
                    }
                    adjustedPtn.append(prevCh);
                    j2 = j3 + 1;
                }
            }
        }
        return adjustedPtn.toString();
    }

    private void concatSingleDate2TimeInterval(String dtfmt, String datePattern, int field, Map<String, PatternInfo> intervalPatterns) {
        PatternInfo timeItvPtnInfo = (PatternInfo) intervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        if (timeItvPtnInfo != null) {
            String timeIntervalPattern = new StringBuilder();
            timeIntervalPattern.append(timeItvPtnInfo.getFirstPart());
            timeIntervalPattern.append(timeItvPtnInfo.getSecondPart());
            intervalPatterns.put(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], DateIntervalInfo.genPatternInfo(SimpleFormatterImpl.formatRawPattern(dtfmt, 2, 2, timeIntervalPattern.toString(), datePattern), timeItvPtnInfo.firstDateInPtnIsLaterDate()));
        }
    }

    private static boolean fieldExistsInSkeleton(int field, String skeleton) {
        return skeleton.indexOf(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]) != -1;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initializePattern(this.isDateIntervalInfoDefault ? LOCAL_PATTERN_CACHE : null);
    }

    @Deprecated
    public Map<String, PatternInfo> getRawPatterns() {
        return this.fIntervalPatterns;
    }
}
