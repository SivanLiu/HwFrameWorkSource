package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import sun.util.calendar.AbstractCalendar;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Era;
import sun.util.calendar.Gregorian;
import sun.util.calendar.LocalGregorianCalendar;
import sun.util.calendar.LocalGregorianCalendar.Date;
import sun.util.locale.provider.CalendarDataUtility;

class JapaneseImperialCalendar extends Calendar {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int BEFORE_MEIJI = 0;
    private static final Era BEFORE_MEIJI_ERA = new Era("BeforeMeiji", "BM", Long.MIN_VALUE, $assertionsDisabled);
    private static final int EPOCH_OFFSET = 719163;
    private static final int EPOCH_YEAR = 1970;
    public static final int HEISEI = 4;
    static final int[] LEAST_MAX_VALUES = new int[]{0, 0, 0, 0, 4, 28, 0, 7, 4, 1, 11, 23, 59, 59, 999, 50400000, 1200000};
    static final int[] MAX_VALUES = new int[]{0, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59, 59, 999, 50400000, 7200000};
    public static final int MEIJI = 1;
    static final int[] MIN_VALUES = new int[]{0, -292275055, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000, 0};
    private static final long ONE_DAY = 86400000;
    private static final int ONE_HOUR = 3600000;
    private static final int ONE_MINUTE = 60000;
    private static final int ONE_SECOND = 1000;
    private static final long ONE_WEEK = 604800000;
    public static final int SHOWA = 3;
    public static final int TAISHO = 2;
    private static final Era[] eras;
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();
    private static final LocalGregorianCalendar jcal = ((LocalGregorianCalendar) CalendarSystem.forName("japanese"));
    private static final long serialVersionUID = -3364572813905467929L;
    private static final long[] sinceFixedDates;
    private transient long cachedFixedDate = Long.MIN_VALUE;
    private transient Date jdate;
    private transient int[] originalFields;
    private transient int[] zoneOffsets;

    static {
        Era[] es = jcal.getEras();
        int length = es.length + 1;
        eras = new Era[length];
        sinceFixedDates = new long[length];
        sinceFixedDates[0] = gcal.getFixedDate(BEFORE_MEIJI_ERA.getSinceDate());
        int index = 0 + 1;
        eras[0] = BEFORE_MEIJI_ERA;
        int length2 = es.length;
        int index2 = index;
        index = 0;
        while (index < length2) {
            Era e = es[index];
            sinceFixedDates[index2] = gcal.getFixedDate(e.getSinceDate());
            int index3 = index2 + 1;
            eras[index2] = e;
            index++;
            index2 = index3;
        }
        int[] iArr = LEAST_MAX_VALUES;
        int length3 = eras.length - 1;
        MAX_VALUES[0] = length3;
        iArr[0] = length3;
        CalendarDate date = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        length3 = Integer.MAX_VALUE;
        int year = Integer.MAX_VALUE;
        for (length2 = 1; length2 < eras.length; length2++) {
            long fd = sinceFixedDates[length2];
            CalendarDate transitionDate = eras[length2].getSinceDate();
            date.setDate(transitionDate.getYear(), 1, 1);
            long fdd = gcal.getFixedDate(date);
            if (fd != fdd) {
                length3 = Math.min(((int) (fd - fdd)) + 1, length3);
            }
            date.setDate(transitionDate.getYear(), 12, 31);
            fdd = gcal.getFixedDate(date);
            if (fd != fdd) {
                length3 = Math.min(((int) (fdd - fd)) + 1, length3);
            }
            Date lgd = getCalendarDate(fd - 1);
            int y = lgd.getYear();
            if (lgd.getMonth() != 1 || lgd.getDayOfMonth() != 1) {
                y--;
            }
            year = Math.min(y, year);
        }
        LEAST_MAX_VALUES[1] = year;
        LEAST_MAX_VALUES[6] = length3;
    }

    JapaneseImperialCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        this.jdate = jcal.newCalendarDate(zone);
        setTimeInMillis(System.currentTimeMillis());
    }

    JapaneseImperialCalendar(TimeZone zone, Locale aLocale, boolean flag) {
        super(zone, aLocale);
        this.jdate = jcal.newCalendarDate(zone);
    }

    public String getCalendarType() {
        return "japanese";
    }

    public boolean equals(Object obj) {
        return ((obj instanceof JapaneseImperialCalendar) && super.equals(obj)) ? true : $assertionsDisabled;
    }

    public int hashCode() {
        return super.hashCode() ^ this.jdate.hashCode();
    }

    public void add(int field, int amount) {
        int i = field;
        int i2 = amount;
        if (i2 != 0) {
            if (i < 0 || i >= 15) {
                throw new IllegalArgumentException();
            }
            complete();
            Date d;
            if (i == 1) {
                d = (Date) this.jdate.clone();
                d.addYear(i2);
                pinDayOfMonth(d);
                set(0, getEraIndex(d));
                set(1, d.getYear());
                set(2, d.getMonth() - 1);
                set(5, d.getDayOfMonth());
            } else if (i == 2) {
                d = (Date) this.jdate.clone();
                d.addMonth(i2);
                pinDayOfMonth(d);
                set(0, getEraIndex(d));
                set(1, d.getYear());
                set(2, d.getMonth() - 1);
                set(5, d.getDayOfMonth());
            } else if (i == 0) {
                int era = internalGet(0) + i2;
                if (era < 0) {
                    era = 0;
                } else if (era > eras.length - 1) {
                    era = eras.length - 1;
                }
                set(0, era);
            } else {
                long delta = (long) i2;
                long timeOfDay = 0;
                switch (i) {
                    case 3:
                    case 4:
                    case 8:
                        delta *= 7;
                        break;
                    case 9:
                        delta = (long) (i2 / 2);
                        timeOfDay = (long) ((i2 % 2) * 12);
                        break;
                    case 10:
                    case 11:
                        delta *= 3600000;
                        break;
                    case 12:
                        delta *= 60000;
                        break;
                    case 13:
                        delta *= 1000;
                        break;
                }
                if (i >= 10) {
                    setTimeInMillis(this.time + delta);
                    return;
                }
                long fd = this.cachedFixedDate;
                long delta2 = delta;
                timeOfDay = ((((((timeOfDay + ((long) internalGet(11))) * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
                if (timeOfDay >= ONE_DAY) {
                    fd++;
                    timeOfDay -= ONE_DAY;
                } else if (timeOfDay < 0) {
                    fd--;
                    timeOfDay += ONE_DAY;
                }
                fd += delta2;
                int zoneOffset = internalGet(15) + internalGet(16);
                setTimeInMillis((((fd - 719163) * ONE_DAY) + timeOfDay) - ((long) zoneOffset));
                zoneOffset -= internalGet(15) + internalGet(16);
                if (zoneOffset != 0) {
                    setTimeInMillis(this.time + ((long) zoneOffset));
                    if (this.cachedFixedDate != fd) {
                        setTimeInMillis(this.time - ((long) zoneOffset));
                    }
                }
            }
        }
    }

    public void roll(int field, boolean up) {
        roll(field, up ? 1 : -1);
    }

    /* JADX WARNING: Missing block: B:57:0x0192, code skipped:
            r6 = r17;
     */
    /* JADX WARNING: Missing block: B:189:0x0592, code skipped:
            r6 = r4;
     */
    /* JADX WARNING: Missing block: B:191:0x0595, code skipped:
            set(r1, getRolledValue(internalGet(r28), r2, r6, r5));
     */
    /* JADX WARNING: Missing block: B:192:0x05a0, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void roll(int field, int amount) {
        int i = field;
        int amount2 = amount;
        if (amount2 != 0) {
            if (i >= 0 && i < 15) {
                complete();
                int min = getMinimum(field);
                int max = getMaximum(field);
                int min2;
                int eraIndex;
                int n;
                int dom;
                long fd;
                long day1;
                long day12;
                long month1;
                int min3;
                switch (i) {
                    case 0:
                    case 9:
                    case 12:
                    case 13:
                    case 14:
                        min2 = min;
                        break;
                    case 1:
                        min = getActualMinimum(field);
                        max = getActualMaximum(field);
                        break;
                    case 2:
                        min2 = min;
                        if (isTransitionYear(this.jdate.getNormalizedYear())) {
                            eraIndex = getEraIndex(this.jdate);
                            min = 0;
                            if (this.jdate.getYear() == 1) {
                                min = eras[eraIndex].getSinceDate();
                                min2 = min.getMonth() - 1;
                            } else if (eraIndex < eras.length - 1) {
                                min = eras[eraIndex + 1].getSinceDate();
                                if (min.getYear() == this.jdate.getNormalizedYear()) {
                                    max = min.getMonth() - 1;
                                    if (min.getDayOfMonth() == 1) {
                                        max--;
                                    }
                                }
                            }
                            if (min2 != max) {
                                n = getRolledValue(internalGet(field), amount2, min2, max);
                                set(2, n);
                                if (n == min2) {
                                    if (!(min.getMonth() == 1 && min.getDayOfMonth() == 1) && this.jdate.getDayOfMonth() < min.getDayOfMonth()) {
                                        set(5, min.getDayOfMonth());
                                    }
                                } else if (n == max && min.getMonth() - 1 == n) {
                                    dom = min.getDayOfMonth();
                                    if (this.jdate.getDayOfMonth() >= dom) {
                                        set(5, dom - 1);
                                    }
                                }
                            } else {
                                return;
                            }
                        }
                        eraIndex = this.jdate.getYear();
                        CalendarDate d;
                        if (eraIndex == getMaximum(1)) {
                            min = jcal.getCalendarDate(this.time, getZone());
                            d = jcal.getCalendarDate((long) Long.MAX_VALUE, getZone());
                            max = d.getMonth() - 1;
                            dom = getRolledValue(internalGet(field), amount2, min2, max);
                            if (dom == max) {
                                min.addYear(-400);
                                min.setMonth(dom + 1);
                                if (min.getDayOfMonth() > d.getDayOfMonth()) {
                                    min.setDayOfMonth(d.getDayOfMonth());
                                    jcal.normalize(min);
                                }
                                if (min.getDayOfMonth() == d.getDayOfMonth() && min.getTimeOfDay() > d.getTimeOfDay()) {
                                    min.setMonth(dom + 1);
                                    min.setDayOfMonth(d.getDayOfMonth() - 1);
                                    jcal.normalize(min);
                                    dom = min.getMonth() - 1;
                                }
                                set(5, min.getDayOfMonth());
                            }
                            set(2, dom);
                        } else if (eraIndex == getMinimum(1)) {
                            min = jcal.getCalendarDate(this.time, getZone());
                            d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                            min2 = d.getMonth() - 1;
                            dom = getRolledValue(internalGet(field), amount2, min2, max);
                            if (dom == min2) {
                                min.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
                                min.setMonth(dom + 1);
                                if (min.getDayOfMonth() < d.getDayOfMonth()) {
                                    min.setDayOfMonth(d.getDayOfMonth());
                                    jcal.normalize(min);
                                }
                                if (min.getDayOfMonth() == d.getDayOfMonth() && min.getTimeOfDay() < d.getTimeOfDay()) {
                                    min.setMonth(dom + 1);
                                    min.setDayOfMonth(d.getDayOfMonth() + 1);
                                    jcal.normalize(min);
                                    dom = min.getMonth() - 1;
                                }
                                set(5, min.getDayOfMonth());
                            }
                            set(2, dom);
                        } else {
                            min = (internalGet(2) + amount2) % 12;
                            if (min < 0) {
                                min += 12;
                            }
                            set(2, min);
                            n = monthLength(min);
                            if (internalGet(5) > n) {
                                set(5, n);
                            }
                        }
                        return;
                    case 3:
                        min2 = min;
                        eraIndex = this.jdate.getNormalizedYear();
                        min = getActualMaximum(3);
                        set(7, internalGet(7));
                        max = internalGet(3);
                        n = max + amount2;
                        int i2;
                        if (isTransitionYear(this.jdate.getNormalizedYear())) {
                            i2 = n;
                            fd = this.cachedFixedDate;
                            day1 = fd - ((long) (7 * (max - min2)));
                            Date d2 = getCalendarDate(day1);
                            if (!(d2.getEra() == this.jdate.getEra() && d2.getYear() == this.jdate.getYear())) {
                                min2++;
                            }
                            jcal.getCalendarDateFromFixedDate(d2, fd + ((long) (7 * (min - max))));
                            if (!(d2.getEra() == this.jdate.getEra() && d2.getYear() == this.jdate.getYear())) {
                                min--;
                            }
                            d2 = getCalendarDate(((long) ((getRolledValue(max, amount2, min2, min) - 1) * 7)) + day1);
                            set(2, d2.getMonth() - 1);
                            set(5, d2.getDayOfMonth());
                            return;
                        }
                        dom = this.jdate.getYear();
                        if (dom == getMaximum(1)) {
                            min = getActualMaximum(3);
                        } else if (dom == getMinimum(1)) {
                            min2 = getActualMinimum(3);
                            min = getActualMaximum(3);
                            if (n > min2 && n < min) {
                                set(3, n);
                                return;
                            }
                        }
                        if (n <= min2 || n >= min) {
                            day1 = this.cachedFixedDate;
                            day12 = day1 - ((long) ((max - min2) * 7));
                            if (dom != getMinimum(1)) {
                                if (gcal.getYearFromFixedDate(day12) != eraIndex) {
                                    min2++;
                                }
                            } else {
                                i2 = n;
                                int i3 = dom;
                                if (day12 < jcal.getFixedDate(jcal.getCalendarDate((long) 0, getZone()))) {
                                    min2++;
                                }
                            }
                            if (gcal.getYearFromFixedDate(day1 + ((long) (7 * (min - internalGet(3))))) != eraIndex) {
                                min--;
                            }
                            max = min;
                            break;
                        }
                        set(3, n);
                        return;
                        break;
                    case 4:
                        int monthLength;
                        boolean isTransitionYear = isTransitionYear(this.jdate.getNormalizedYear());
                        min = internalGet(7) - getFirstDayOfWeek();
                        if (min < 0) {
                            min += 7;
                        }
                        fd = this.cachedFixedDate;
                        if (isTransitionYear) {
                            month1 = getFixedDateMonth1(this.jdate, fd);
                            monthLength = actualMonthLength();
                        } else {
                            month1 = (fd - ((long) internalGet(5))) + 1;
                            monthLength = jcal.getMonthLength(this.jdate);
                        }
                        int monthLength2 = monthLength;
                        day1 = AbstractCalendar.getDayOfWeekDateOnOrBefore(month1 + 6, getFirstDayOfWeek());
                        if (((int) (day1 - month1)) >= getMinimalDaysInFirstWeek()) {
                            day1 -= 7;
                        }
                        n = getRolledValue(internalGet(field), amount2, 1, getActualMaximum(field)) - 1;
                        fd = (((long) (n * 7)) + day1) + ((long) min);
                        if (fd < month1) {
                            fd = month1;
                        } else if (fd >= ((long) monthLength2) + month1) {
                            fd = (((long) monthLength2) + month1) - 1;
                        }
                        set(5, ((int) (fd - month1)) + 1);
                        return;
                    case 5:
                        min2 = min;
                        if (!isTransitionYear(this.jdate.getNormalizedYear())) {
                            max = jcal.getMonthLength(this.jdate);
                            break;
                        }
                        long month12 = getFixedDateMonth1(this.jdate, this.cachedFixedDate);
                        set(5, getCalendarDate(((long) getRolledValue((int) (this.cachedFixedDate - month12), amount2, 0, actualMonthLength() - 1)) + month12).getDayOfMonth());
                        return;
                    case 6:
                        min3 = min;
                        max = getActualMaximum(field);
                        if (isTransitionYear(this.jdate.getNormalizedYear())) {
                            fd = this.cachedFixedDate - ((long) internalGet(6));
                            Date d3 = getCalendarDate(((long) getRolledValue(internalGet(6), amount2, min3, max)) + fd);
                            set(2, d3.getMonth() - 1);
                            set(5, d3.getDayOfMonth());
                            return;
                        }
                        break;
                    case 7:
                        min3 = min;
                        eraIndex = this.jdate.getNormalizedYear();
                        if (isTransitionYear(eraIndex) == 0 && isTransitionYear(eraIndex - 1) == 0) {
                            min = internalGet(3);
                            if (min > 1 && min < 52) {
                                set(3, internalGet(3));
                                max = 7;
                                break;
                            }
                        }
                        amount2 %= 7;
                        if (amount2 != 0) {
                            day12 = this.cachedFixedDate;
                            month1 = AbstractCalendar.getDayOfWeekDateOnOrBefore(day12, getFirstDayOfWeek());
                            day12 += (long) amount2;
                            if (day12 < month1) {
                                day12 += 7;
                            } else if (day12 >= month1 + 7) {
                                day12 -= 7;
                            }
                            min = getCalendarDate(day12);
                            set(0, getEraIndex(min));
                            set(min.getYear(), min.getMonth() - 1, min.getDayOfMonth());
                            return;
                        }
                        return;
                    case 8:
                        min = 1;
                        if (!isTransitionYear(this.jdate.getNormalizedYear())) {
                            eraIndex = internalGet(5);
                            min2 = jcal.getMonthLength(this.jdate);
                            max = min2 / 7;
                            if ((eraIndex - 1) % 7 < min2 % 7) {
                                max++;
                            }
                            set(7, internalGet(7));
                            break;
                        }
                        fd = this.cachedFixedDate;
                        day1 = getFixedDateMonth1(this.jdate, fd);
                        eraIndex = actualMonthLength();
                        max = eraIndex / 7;
                        int x = ((int) (fd - day1)) % 7;
                        if (x < eraIndex % 7) {
                            max++;
                        }
                        int min4 = 1;
                        set(5, getCalendarDate((((long) ((getRolledValue(internalGet(field), amount2, 1, max) - 1) * 7)) + day1) + ((long) x)).getDayOfMonth());
                        return;
                    case 10:
                    case 11:
                        n = max + 1;
                        dom = internalGet(field);
                        int nh = (dom + amount2) % n;
                        if (nh < 0) {
                            nh += n;
                        }
                        this.time += (long) (ONE_HOUR * (nh - dom));
                        CalendarDate d4 = jcal.getCalendarDate(this.time, getZone());
                        if (internalGet(5) != d4.getDayOfMonth()) {
                            d4.setEra(this.jdate.getEra());
                            d4.setDate(internalGet(1), internalGet(2) + 1, internalGet(5));
                            if (i == 10) {
                                d4.addHours(12);
                            }
                            this.time = jcal.getTime(d4);
                        }
                        min = d4.getHours();
                        internalSet(i, min % n);
                        if (i == 10) {
                            internalSet(11, min);
                        } else {
                            internalSet(9, min / 12);
                            internalSet(10, min % 12);
                        }
                        min2 = d4.getZoneOffset();
                        int saving = d4.getDaylightSaving();
                        internalSet(15, min2 - saving);
                        internalSet(16, saving);
                        return;
                    default:
                        min2 = min;
                        break;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public String getDisplayName(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, 1, 4, locale, 647)) {
            return null;
        }
        int fieldValue = get(field);
        if (field == 1 && (getBaseStyle(style) != 2 || fieldValue != 1 || get(0) == 0)) {
            return null;
        }
        String name = CalendarDataUtility.retrieveFieldValueName(getCalendarType(), field, fieldValue, style, locale);
        if (name == null && field == 0 && fieldValue < eras.length) {
            Era era = eras[fieldValue];
            name = style == 1 ? era.getAbbreviation() : era.getName();
        }
        return name;
    }

    public Map<String, Integer> getDisplayNames(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, 0, 4, locale, 647)) {
            return null;
        }
        Map<String, Integer> names = CalendarDataUtility.retrieveFieldValueNames(getCalendarType(), field, style, locale);
        if (names != null && field == 0) {
            int size = names.size();
            if (style == 0) {
                Set<Integer> values = new HashSet();
                for (String key : names.keySet()) {
                    values.add((Integer) names.get(key));
                }
                size = values.size();
            }
            if (size < eras.length) {
                int baseStyle = getBaseStyle(style);
                for (int i = size; i < eras.length; i++) {
                    Era era = eras[i];
                    if (baseStyle == 0 || baseStyle == 1 || baseStyle == 4) {
                        names.put(era.getAbbreviation(), Integer.valueOf(i));
                    }
                    if (baseStyle == 0 || baseStyle == 2) {
                        names.put(era.getName(), Integer.valueOf(i));
                    }
                }
            }
        }
        return names;
    }

    public int getMinimum(int field) {
        return MIN_VALUES[field];
    }

    public int getMaximum(int field) {
        if (field != 1) {
            return MAX_VALUES[field];
        }
        return Math.max(LEAST_MAX_VALUES[1], jcal.getCalendarDate((long) Long.MAX_VALUE, getZone()).getYear());
    }

    public int getGreatestMinimum(int field) {
        return field == 1 ? 1 : MIN_VALUES[field];
    }

    public int getLeastMaximum(int field) {
        if (field != 1) {
            return LEAST_MAX_VALUES[field];
        }
        return Math.min(LEAST_MAX_VALUES[1], getMaximum(1));
    }

    public int getActualMinimum(int field) {
        int i = field;
        if (!Calendar.isFieldSet(14, i)) {
            return getMinimum(field);
        }
        int value = 0;
        Date jd = jcal.getCalendarDate(getNormalizedCalendar().getTimeInMillis(), getZone());
        int eraIndex = getEraIndex(jd);
        CalendarDate d;
        switch (i) {
            case 1:
                if (eraIndex <= 0) {
                    value = getMinimum(field);
                    d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                    int y = d.getYear();
                    if (y > HttpURLConnection.HTTP_BAD_REQUEST) {
                        y -= 400;
                    }
                    jd.setYear(y);
                    jcal.normalize(jd);
                    if (getYearOffsetInMillis(jd) < getYearOffsetInMillis(d)) {
                        value++;
                        break;
                    }
                }
                value = 1;
                CalendarDate d2 = jcal.getCalendarDate(eras[eraIndex].getSince(getZone()), getZone());
                jd.setYear(d2.getYear());
                jcal.normalize(jd);
                if (getYearOffsetInMillis(jd) < getYearOffsetInMillis(d2)) {
                    value = 1 + 1;
                    break;
                }
                break;
            case 2:
                if (eraIndex > 1 && jd.getYear() == 1) {
                    CalendarDate d3 = jcal.getCalendarDate(eras[eraIndex].getSince(getZone()), getZone());
                    int value2 = d3.getMonth() - 1;
                    if (jd.getDayOfMonth() < d3.getDayOfMonth()) {
                        value2++;
                    }
                    value = value2;
                    break;
                }
            case 3:
                value = 1;
                d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                d.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
                jcal.normalize(d);
                jd.setEra(d.getEra());
                jd.setYear(d.getYear());
                jcal.normalize(jd);
                long jan1 = jcal.getFixedDate(d);
                long fd = jcal.getFixedDate(jd);
                long day1 = fd - ((long) (7 * (getWeekNumber(jan1, fd) - 1)));
                if (day1 < jan1 || (day1 == jan1 && jd.getTimeOfDay() < d.getTimeOfDay())) {
                    value = 1 + 1;
                    break;
                }
        }
        return value;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getActualMaximum(int field) {
        int i = field;
        if (((1 << i) & 130689) != 0) {
            return getMaximum(field);
        }
        int fieldsForFixedMax;
        int value;
        JapaneseImperialCalendar jc = getNormalizedCalendar();
        Date date = jc.jdate;
        int normalizedYear = date.getNormalizedYear();
        int eraIndex;
        int value2;
        long transition;
        Date ldate;
        int fieldsForFixedMax2;
        int magic;
        CalendarDate d;
        long jan1;
        long nextJan1;
        int dayOfWeek;
        int nDaysFirstWeek;
        switch (i) {
            case 1:
                CalendarDate d2;
                fieldsForFixedMax = 130689;
                CalendarDate jd = jcal.getCalendarDate(jc.getTimeInMillis(), getZone());
                eraIndex = getEraIndex(date);
                if (eraIndex == eras.length - 1) {
                    d2 = jcal.getCalendarDate((long) Long.MAX_VALUE, getZone());
                    value2 = d2.getYear();
                    if (value2 > HttpURLConnection.HTTP_BAD_REQUEST) {
                        jd.setYear(value2 - 400);
                    }
                } else {
                    d2 = jcal.getCalendarDate(eras[eraIndex + 1].getSince(getZone()) - 1, getZone());
                    value2 = d2.getYear();
                    jd.setYear(value2);
                }
                jcal.normalize(jd);
                if (getYearOffsetInMillis(jd) > getYearOffsetInMillis(d2)) {
                    value2--;
                }
                value = value2;
                break;
            case 2:
                fieldsForFixedMax = 130689;
                if (!isTransitionYear(date.getNormalizedYear())) {
                    value2 = jcal.getCalendarDate((long) Long.MAX_VALUE, getZone());
                    if (date.getEra() != value2.getEra() || date.getYear() != value2.getYear()) {
                        value = 11;
                        break;
                    }
                    value = value2.getMonth() - 1;
                    break;
                }
                value2 = getEraIndex(date);
                if (date.getYear() != 1) {
                    value2++;
                }
                transition = sinceFixedDates[value2];
                if (jc.cachedFixedDate >= transition) {
                    value = 11;
                    break;
                }
                ldate = (Date) date.clone();
                jcal.getCalendarDateFromFixedDate(ldate, transition - 1);
                value = ldate.getMonth() - 1;
                break;
                break;
            case 3:
                fieldsForFixedMax = 130689;
                if (isTransitionYear(date.getNormalizedYear()) != 0) {
                    if (jc == this) {
                        jc = (JapaneseImperialCalendar) jc.clone();
                    }
                    fieldsForFixedMax2 = getActualMaximum(6);
                    jc.set(6, fieldsForFixedMax2);
                    value2 = jc.get(3);
                    if (value2 == 1 && fieldsForFixedMax2 > 7) {
                        jc.add(3, -1);
                        value = jc.get(3);
                        break;
                    }
                    value = value2;
                    break;
                }
                fieldsForFixedMax2 = jcal.getCalendarDate((long) Long.MAX_VALUE, getZone());
                if (date.getEra() != fieldsForFixedMax2.getEra() || date.getYear() != fieldsForFixedMax2.getYear()) {
                    Date jd2;
                    if (date.getEra() != null || date.getYear() != getMinimum(1)) {
                        jd2 = fieldsForFixedMax2;
                        fieldsForFixedMax2 = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                        fieldsForFixedMax2.setDate(date.getNormalizedYear(), 1, 1);
                        eraIndex = gcal.getDayOfWeek(fieldsForFixedMax2) - getFirstDayOfWeek();
                        if (eraIndex < 0) {
                            eraIndex += 7;
                        }
                        value2 = 52;
                        magic = (getMinimalDaysInFirstWeek() + eraIndex) - 1;
                        if (magic == 6 || (date.isLeapYear() && (magic == 5 || magic == 12))) {
                            value2 = 52 + 1;
                        }
                        value = value2;
                        break;
                    }
                    d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                    d.addYear(HttpURLConnection.HTTP_BAD_REQUEST);
                    jcal.normalize(d);
                    fieldsForFixedMax2.setEra(d.getEra());
                    fieldsForFixedMax2.setDate(d.getYear() + 1, 1, 1);
                    jcal.normalize(fieldsForFixedMax2);
                    jan1 = jcal.getFixedDate(d);
                    nextJan1 = jcal.getFixedDate(fieldsForFixedMax2);
                    long nextJan1st = AbstractCalendar.getDayOfWeekDateOnOrBefore(6 + nextJan1, getFirstDayOfWeek());
                    jd2 = fieldsForFixedMax2;
                    if (((int) (nextJan1st - nextJan1)) >= getMinimalDaysInFirstWeek()) {
                        nextJan1st -= 7;
                    }
                    value = getWeekNumber(jan1, nextJan1st);
                    break;
                }
                transition = jcal.getFixedDate(fieldsForFixedMax2);
                value = getWeekNumber(getFixedDateJan1(fieldsForFixedMax2, transition), transition);
                break;
                break;
            case 4:
                fieldsForFixedMax = 130689;
                fieldsForFixedMax2 = jcal.getCalendarDate((long) Long.MAX_VALUE, getZone());
                if (date.getEra() != fieldsForFixedMax2.getEra() || date.getYear() != fieldsForFixedMax2.getYear()) {
                    d = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    d.setDate(date.getNormalizedYear(), date.getMonth(), 1);
                    dayOfWeek = gcal.getDayOfWeek(d);
                    magic = gcal.getMonthLength(d);
                    dayOfWeek -= getFirstDayOfWeek();
                    if (dayOfWeek < 0) {
                        dayOfWeek += 7;
                    }
                    nDaysFirstWeek = 7 - dayOfWeek;
                    value2 = 3;
                    if (nDaysFirstWeek >= getMinimalDaysInFirstWeek()) {
                        value2 = 3 + 1;
                    }
                    magic -= nDaysFirstWeek + 21;
                    if (magic > 0) {
                        value2++;
                        if (magic > 7) {
                            value2++;
                        }
                    }
                    value = value2;
                    break;
                }
                transition = jcal.getFixedDate(fieldsForFixedMax2);
                value = getWeekNumber((transition - ((long) fieldsForFixedMax2.getDayOfMonth())) + 1, transition);
                break;
            case 5:
                fieldsForFixedMax = 130689;
                value = jcal.getMonthLength(date);
                break;
            case 6:
                if (!isTransitionYear(date.getNormalizedYear())) {
                    ldate = jcal.getCalendarDate((long) Long.MAX_VALUE, getZone());
                    if (date.getEra() != ldate.getEra() || date.getYear() != ldate.getYear()) {
                        if (date.getYear() == getMinimum(1)) {
                            d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                            nextJan1 = jcal.getFixedDate(d);
                            d.addYear(1);
                            d.setMonth(1).setDayOfMonth(1);
                            jcal.normalize(d);
                            fieldsForFixedMax = 130689;
                            fieldsForFixedMax2 = (int) (jcal.getFixedDate(d) - nextJan1);
                        } else {
                            fieldsForFixedMax = 130689;
                            fieldsForFixedMax2 = jcal.getYearLength(date);
                        }
                        value = fieldsForFixedMax2;
                        break;
                    }
                    transition = jcal.getFixedDate(ldate);
                    fieldsForFixedMax = 130689;
                    value = ((int) (transition - getFixedDateJan1(ldate, transition))) + 1;
                    break;
                }
                eraIndex = getEraIndex(date);
                if (date.getYear() != 1) {
                    eraIndex++;
                }
                jan1 = sinceFixedDates[eraIndex];
                nextJan1 = jc.cachedFixedDate;
                CalendarDate d3 = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                d3.setDate(date.getNormalizedYear(), 1, 1);
                if (nextJan1 < jan1) {
                    dayOfWeek = (int) (jan1 - gcal.getFixedDate(d3));
                } else {
                    d3.addYear(1);
                    dayOfWeek = (int) (gcal.getFixedDate(d3) - jan1);
                }
                value = dayOfWeek;
                break;
            case 8:
                eraIndex = date.getDayOfWeek();
                BaseCalendar.Date d4 = (BaseCalendar.Date) date.clone();
                nDaysFirstWeek = jcal.getMonthLength(d4);
                d4.setDayOfMonth(1);
                jcal.normalize(d4);
                int x = eraIndex - d4.getDayOfWeek();
                if (x < 0) {
                    x += 7;
                }
                value = ((nDaysFirstWeek - x) + 6) / 7;
                fieldsForFixedMax = 130689;
                break;
            default:
                fieldsForFixedMax = 130689;
                throw new ArrayIndexOutOfBoundsException(i);
        }
        fieldsForFixedMax = 130689;
        return value;
    }

    private long getYearOffsetInMillis(CalendarDate date) {
        return (date.getTimeOfDay() + ((jcal.getDayOfYear(date) - 1) * ONE_DAY)) - ((long) date.getZoneOffset());
    }

    public Object clone() {
        JapaneseImperialCalendar other = (JapaneseImperialCalendar) super.clone();
        other.jdate = (Date) this.jdate.clone();
        other.originalFields = null;
        other.zoneOffsets = null;
        return other;
    }

    public TimeZone getTimeZone() {
        TimeZone zone = super.getTimeZone();
        this.jdate.setZone(zone);
        return zone;
    }

    public void setTimeZone(TimeZone zone) {
        super.setTimeZone(zone);
        this.jdate.setZone(zone);
    }

    protected void computeFields() {
        int mask;
        if (isPartiallyNormalized()) {
            mask = getSetStateFields();
            int fieldMask = (~mask) & 131071;
            if (fieldMask != 0 || this.cachedFixedDate == Long.MIN_VALUE) {
                mask |= computeFields(fieldMask, 98304 & mask);
            }
        } else {
            mask = 131071;
            computeFields(131071, 0);
        }
        setFieldsComputed(mask);
    }

    private int computeFields(int fieldMask, int tzMask) {
        int timeOfDay;
        int i = fieldMask;
        int i2 = tzMask;
        int zoneOffset = 0;
        TimeZone tz = getZone();
        if (this.zoneOffsets == null) {
            this.zoneOffsets = new int[2];
        }
        if (i2 != 98304) {
            zoneOffset = tz.getOffset(this.time);
            this.zoneOffsets[0] = tz.getRawOffset();
            this.zoneOffsets[1] = zoneOffset - this.zoneOffsets[0];
        }
        if (i2 != 0) {
            if (Calendar.isFieldSet(i2, 15)) {
                this.zoneOffsets[0] = internalGet(15);
            }
            if (Calendar.isFieldSet(i2, 16)) {
                this.zoneOffsets[1] = internalGet(16);
            }
            zoneOffset = this.zoneOffsets[0] + this.zoneOffsets[1];
        }
        long fixedDate = (((long) zoneOffset) / ONE_DAY) + (this.time / ONE_DAY);
        int timeOfDay2 = (zoneOffset % 86400000) + ((int) (this.time % ONE_DAY));
        if (((long) timeOfDay2) >= ONE_DAY) {
            timeOfDay = (int) (((long) timeOfDay2) - 86400000);
            fixedDate++;
        } else {
            timeOfDay = timeOfDay2;
            while (timeOfDay < 0) {
                timeOfDay = (int) (((long) timeOfDay) + ONE_DAY);
                fixedDate--;
            }
        }
        fixedDate += 719163;
        if (fixedDate != this.cachedFixedDate || fixedDate < 0) {
            jcal.getCalendarDateFromFixedDate(this.jdate, fixedDate);
            this.cachedFixedDate = fixedDate;
        }
        int era = getEraIndex(this.jdate);
        int year = this.jdate.getYear();
        internalSet(0, era);
        internalSet(1, year);
        int mask = i | 3;
        int month = this.jdate.getMonth() - 1;
        int dayOfMonth = this.jdate.getDayOfMonth();
        if ((i & 164) != 0) {
            internalSet(2, month);
            internalSet(5, dayOfMonth);
            internalSet(7, this.jdate.getDayOfWeek());
            mask |= 164;
        }
        if ((i & 32256) != 0) {
            if (timeOfDay != 0) {
                int hours = timeOfDay / ONE_HOUR;
                internalSet(11, hours);
                internalSet(9, hours / 12);
                internalSet(10, hours % 12);
                int r = timeOfDay % ONE_HOUR;
                internalSet(12, r / ONE_MINUTE);
                r %= ONE_MINUTE;
                internalSet(13, r / 1000);
                internalSet(14, r % 1000);
            } else {
                internalSet(11, 0);
                internalSet(9, 0);
                internalSet(10, 0);
                internalSet(12, 0);
                internalSet(13, 0);
                internalSet(14, 0);
            }
            mask |= 32256;
        }
        if ((i & 98304) != 0) {
            internalSet(15, this.zoneOffsets[0]);
            internalSet(16, this.zoneOffsets[1]);
            mask |= 98304;
        }
        int i3;
        TimeZone timeZone;
        int i4;
        int i5;
        int i6;
        int i7;
        if ((i & 344) != 0) {
            long fixedDateJan1;
            int normalizedYear;
            i2 = this.jdate.getNormalizedYear();
            boolean transitionYear = isTransitionYear(this.jdate.getNormalizedYear());
            if (transitionYear) {
                fixedDateJan1 = getFixedDateJan1(this.jdate, fixedDate);
                zoneOffset = ((int) (fixedDate - fixedDateJan1)) + 1;
                i3 = timeOfDay;
            } else {
                timeZone = tz;
                if (i2 == MIN_VALUES[1]) {
                    fixedDateJan1 = jcal.getFixedDate(jcal.getCalendarDate(Long.MIN_VALUE, getZone()));
                    zoneOffset = ((int) (fixedDate - fixedDateJan1)) + 1;
                } else {
                    zoneOffset = (int) jcal.getDayOfYear(this.jdate);
                    fixedDateJan1 = (fixedDate - ((long) zoneOffset)) + 1;
                }
            }
            long fixedDateJan12 = fixedDateJan1;
            if (transitionYear) {
                fixedDateJan1 = getFixedDateMonth1(this.jdate, fixedDate);
                normalizedYear = i2;
            } else {
                normalizedYear = i2;
                fixedDateJan1 = (fixedDate - ((long) dayOfMonth)) + 1;
            }
            long fixedDateMonth1 = fixedDateJan1;
            internalSet(6, zoneOffset);
            internalSet(8, ((dayOfMonth - 1) / 7) + 1);
            tz = getWeekNumber(fixedDateJan12, fixedDate);
            int dayOfYear;
            int i8;
            if (tz == null) {
                long prevJan1;
                month = fixedDateJan12 - 1;
                Date d = getCalendarDate(month);
                if (transitionYear) {
                    dayOfYear = zoneOffset;
                } else {
                    dayOfYear = zoneOffset;
                    if (isTransitionYear(d.getNormalizedYear()) == 0) {
                        prevJan1 = fixedDateJan12 - 365;
                        if (d.isLeapYear() != 0) {
                            prevJan1--;
                        }
                        tz = getWeekNumber(prevJan1, month);
                    }
                }
                if (transitionYear) {
                    i4 = year;
                    if (this.jdate.getYear() == 1) {
                        if (era > 4) {
                            zoneOffset = eras[era - 1].getSinceDate();
                            i5 = era;
                            era = normalizedYear;
                            if (era == zoneOffset.getYear()) {
                                i8 = era;
                                d.setMonth(zoneOffset.getMonth()).setDayOfMonth(zoneOffset.getDayOfMonth());
                            } else {
                                i8 = era;
                            }
                        } else {
                            i8 = normalizedYear;
                            d.setMonth(1).setDayOfMonth(1);
                        }
                        jcal.normalize(d);
                        prevJan1 = jcal.getFixedDate(d);
                    } else {
                        i8 = normalizedYear;
                        prevJan1 = fixedDateJan12 - 365;
                        if (d.isLeapYear() != 0) {
                            prevJan1--;
                        }
                    }
                } else {
                    i4 = year;
                    i8 = normalizedYear;
                    zoneOffset = eras[getEraIndex(this.jdate)].getSinceDate();
                    d.setMonth(zoneOffset.getMonth()).setDayOfMonth(zoneOffset.getDayOfMonth());
                    jcal.normalize(d);
                    prevJan1 = jcal.getFixedDate(d);
                }
                tz = getWeekNumber(prevJan1, month);
            } else {
                TimeZone timeZone2;
                dayOfYear = zoneOffset;
                i5 = era;
                i4 = year;
                i6 = month;
                i7 = dayOfMonth;
                i8 = normalizedYear;
                long nextJan1;
                if (transitionYear) {
                    Date zoneOffset2 = (Date) this.jdate.clone();
                    if (this.jdate.getYear() == 1) {
                        zoneOffset2.addYear(1);
                        zoneOffset2.setMonth(1).setDayOfMonth(1);
                        nextJan1 = jcal.getFixedDate(zoneOffset2);
                    } else {
                        era = getEraIndex(zoneOffset2) + 1;
                        year = eras[era].getSinceDate();
                        zoneOffset2.setEra(eras[era]);
                        zoneOffset2.setDate(1, year.getMonth(), year.getDayOfMonth());
                        jcal.normalize(zoneOffset2);
                        nextJan1 = jcal.getFixedDate(zoneOffset2);
                    }
                    month = AbstractCalendar.getDayOfWeekDateOnOrBefore(6 + nextJan1, getFirstDayOfWeek());
                    Date d2 = zoneOffset2;
                    timeZone2 = tz;
                    if (((int) (month - nextJan1)) >= getMinimalDaysInFirstWeek() && fixedDate >= month - 7) {
                        tz = true;
                    }
                } else if (tz >= 52) {
                    fixedDateJan1 = fixedDateJan12 + 365;
                    if (this.jdate.isLeapYear() != 0) {
                        fixedDateJan1++;
                    }
                    nextJan1 = AbstractCalendar.getDayOfWeekDateOnOrBefore(6 + fixedDateJan1, getFirstDayOfWeek());
                    if (((int) (nextJan1 - fixedDateJan1)) >= getMinimalDaysInFirstWeek() && fixedDate >= nextJan1 - 7) {
                        tz = true;
                    }
                } else {
                    timeZone2 = tz;
                }
                tz = timeZone2;
            }
            internalSet(3, tz);
            internalSet(4, getWeekNumber(fixedDateMonth1, fixedDate));
            return mask | 344;
        }
        timeZone = tz;
        i5 = era;
        i4 = year;
        i3 = timeOfDay;
        i6 = month;
        i7 = dayOfMonth;
        return mask;
    }

    private int getWeekNumber(long fixedDay1, long fixedDate) {
        long fixedDay1st = AbstractCalendar.getDayOfWeekDateOnOrBefore(6 + fixedDay1, getFirstDayOfWeek());
        if (((int) (fixedDay1st - fixedDay1)) >= getMinimalDaysInFirstWeek()) {
            fixedDay1st -= 7;
        }
        int normalizedDayOfPeriod = (int) (fixedDate - fixedDay1st);
        if (normalizedDayOfPeriod >= 0) {
            return (normalizedDayOfPeriod / 7) + 1;
        }
        return CalendarUtils.floorDivide(normalizedDayOfPeriod, 7) + 1;
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x0136  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void computeTime() {
        int field;
        int value;
        int year;
        long timeOfDay;
        int i;
        int mask;
        int i2;
        int i3 = 17;
        if (!isLenient()) {
            if (this.originalFields == null) {
                this.originalFields = new int[17];
            }
            field = 0;
            while (field < 17) {
                value = internalGet(field);
                if (!isExternallySet(field) || (value >= getMinimum(field) && value <= getMaximum(field))) {
                    this.originalFields[field] = value;
                    field++;
                } else {
                    throw new IllegalArgumentException(Calendar.getFieldName(field));
                }
            }
        }
        field = selectFields();
        if (isSet(0)) {
            value = internalGet(0);
            year = isSet(1) ? internalGet(1) : 1;
        } else if (isSet(1)) {
            value = eras.length - 1;
            year = internalGet(1);
        } else {
            value = 3;
            year = 45;
        }
        if (Calendar.isFieldSet(field, 11)) {
            timeOfDay = 0 + ((long) internalGet(11));
        } else {
            timeOfDay = 0 + ((long) internalGet(10));
            if (Calendar.isFieldSet(field, 9)) {
                timeOfDay += (long) (internalGet(9) * 12);
            }
        }
        timeOfDay = (((((timeOfDay * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
        long fixedDate = timeOfDay / ONE_DAY;
        timeOfDay %= ONE_DAY;
        while (timeOfDay < 0) {
            timeOfDay += ONE_DAY;
            fixedDate--;
        }
        long millis = (((fixedDate + getFixedDate(value, year, field)) - 719163) * ONE_DAY) + timeOfDay;
        TimeZone zone = getZone();
        if (this.zoneOffsets == null) {
            this.zoneOffsets = new int[2];
        }
        int tzMask = field & 98304;
        if (tzMask != 98304) {
            zone.getOffsets(millis - ((long) zone.getRawOffset()), this.zoneOffsets);
        }
        if (tzMask != 0) {
            if (Calendar.isFieldSet(tzMask, 15)) {
                this.zoneOffsets[0] = internalGet(15);
            }
            if (Calendar.isFieldSet(tzMask, 16)) {
                i = 1;
                this.zoneOffsets[1] = internalGet(16);
                this.time = millis - ((long) (this.zoneOffsets[0] + this.zoneOffsets[i]));
                mask = computeFields(getSetStateFields() | field, tzMask);
                if (!isLenient()) {
                    year = 0;
                    while (year < i3) {
                        if (isExternallySet(year) && this.originalFields[year] != internalGet(year)) {
                            i3 = internalGet(year);
                            System.arraycopy(this.originalFields, 0, this.fields, 0, this.fields.length);
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(Calendar.getFieldName(year));
                            stringBuilder.append("=");
                            stringBuilder.append(i3);
                            stringBuilder.append(", expected ");
                            stringBuilder.append(this.originalFields[year]);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        year++;
                        i3 = 17;
                    }
                }
                i2 = value;
                setFieldsNormalized(mask);
            }
        }
        i = 1;
        this.time = millis - ((long) (this.zoneOffsets[0] + this.zoneOffsets[i]));
        mask = computeFields(getSetStateFields() | field, tzMask);
        if (isLenient()) {
        }
        i2 = value;
        setFieldsNormalized(mask);
    }

    private long getFixedDate(int era, int year, int fieldMask) {
        CalendarDate d;
        int m;
        int year2 = year;
        int i = fieldMask;
        int month = 0;
        int firstDayOfMonth = 1;
        if (Calendar.isFieldSet(i, 2)) {
            month = internalGet(2);
            if (month > 11) {
                year2 += month / 12;
                month %= 12;
            } else if (month < 0) {
                int[] rem = new int[1];
                year2 += CalendarUtils.floorDivide(month, 12, rem);
                month = rem[0];
            }
        } else if (year2 == 1 && era != 0) {
            d = eras[era].getSinceDate();
            month = d.getMonth() - 1;
            firstDayOfMonth = d.getDayOfMonth();
        }
        if (year2 == MIN_VALUES[1]) {
            d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
            m = d.getMonth() - 1;
            if (month < m) {
                month = m;
            }
            if (month == m) {
                firstDayOfMonth = d.getDayOfMonth();
            }
        }
        Date date = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        date.setEra(era > 0 ? eras[era] : null);
        date.setDate(year2, month + 1, firstDayOfMonth);
        jcal.normalize(date);
        long fixedDate = jcal.getFixedDate(date);
        Date date2;
        long fixedDate2;
        if (!Calendar.isFieldSet(i, 2)) {
            date2 = date;
            fixedDate2 = fixedDate;
            long fixedDate3;
            if (Calendar.isFieldSet(i, 6)) {
                date = date2;
                if (isTransitionYear(date.getNormalizedYear())) {
                    fixedDate3 = getFixedDateJan1(date, fixedDate2);
                } else {
                    fixedDate3 = fixedDate2;
                }
                return (fixedDate3 + ((long) internalGet(6))) - 1;
            }
            fixedDate3 = fixedDate2;
            long firstDayOfWeek = AbstractCalendar.getDayOfWeekDateOnOrBefore(fixedDate3 + 6, getFirstDayOfWeek());
            if (firstDayOfWeek - fixedDate3 >= ((long) getMinimalDaysInFirstWeek())) {
                firstDayOfWeek -= 7;
            }
            if (Calendar.isFieldSet(i, 7)) {
                int dayOfWeek = internalGet(7);
                if (dayOfWeek != getFirstDayOfWeek()) {
                    firstDayOfWeek = AbstractCalendar.getDayOfWeekDateOnOrBefore(firstDayOfWeek + 6, dayOfWeek);
                }
            }
            return firstDayOfWeek + (7 * (((long) internalGet(3)) - 1));
        } else if (!Calendar.isFieldSet(i, 5)) {
            long fixedDate4;
            if (Calendar.isFieldSet(i, 4)) {
                date2 = date;
                fixedDate4 = AbstractCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                if (fixedDate4 - fixedDate >= ((long) getMinimalDaysInFirstWeek())) {
                    fixedDate4 -= 7;
                }
                if (Calendar.isFieldSet(i, 7)) {
                    fixedDate4 = AbstractCalendar.getDayOfWeekDateOnOrBefore(fixedDate4 + 6, internalGet(7));
                }
                fixedDate = fixedDate4 + ((long) (7 * (internalGet(4) - 1)));
            } else {
                int dowim;
                date2 = date;
                fixedDate2 = fixedDate;
                m = 1;
                if (Calendar.isFieldSet(i, 7)) {
                    fixedDate4 = internalGet(7);
                } else {
                    fixedDate4 = getFirstDayOfWeek();
                }
                if (Calendar.isFieldSet(i, 8)) {
                    dowim = internalGet(8);
                } else {
                    dowim = m;
                }
                date = dowim;
                if (date >= null) {
                    fixedDate = AbstractCalendar.getDayOfWeekDateOnOrBefore((fixedDate2 + ((long) (7 * date))) - 1, fixedDate4);
                } else {
                    fixedDate = AbstractCalendar.getDayOfWeekDateOnOrBefore((fixedDate2 + ((long) (monthLength(month, year2) + (7 * (date + 1))))) - 1, fixedDate4);
                }
            }
            return fixedDate;
        } else if (isSet(5)) {
            return (fixedDate + ((long) internalGet(5))) - ((long) firstDayOfMonth);
        } else {
            return fixedDate;
        }
    }

    private long getFixedDateJan1(Date date, long fixedDate) {
        Era era = date.getEra();
        if (date.getEra() != null && date.getYear() == 1) {
            for (int eraIndex = getEraIndex(date); eraIndex > 0; eraIndex--) {
                long fd = gcal.getFixedDate(eras[eraIndex].getSinceDate());
                if (fd <= fixedDate) {
                    return fd;
                }
            }
        }
        CalendarDate d = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        d.setDate(date.getNormalizedYear(), 1, 1);
        return gcal.getFixedDate(d);
    }

    private long getFixedDateMonth1(Date date, long fixedDate) {
        int eraIndex = getTransitionEraIndex(date);
        if (eraIndex != -1) {
            long transition = sinceFixedDates[eraIndex];
            if (transition <= fixedDate) {
                return transition;
            }
        }
        return (fixedDate - ((long) date.getDayOfMonth())) + 1;
    }

    private static Date getCalendarDate(long fd) {
        Date d = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        jcal.getCalendarDateFromFixedDate(d, fd);
        return d;
    }

    private int monthLength(int month, int gregorianYear) {
        return CalendarUtils.isGregorianLeapYear(gregorianYear) ? GregorianCalendar.LEAP_MONTH_LENGTH[month] : GregorianCalendar.MONTH_LENGTH[month];
    }

    private int monthLength(int month) {
        return this.jdate.isLeapYear() ? GregorianCalendar.LEAP_MONTH_LENGTH[month] : GregorianCalendar.MONTH_LENGTH[month];
    }

    private int actualMonthLength() {
        int length = jcal.getMonthLength(this.jdate);
        int eraIndex = getTransitionEraIndex(this.jdate);
        if (eraIndex != -1) {
            return length;
        }
        long transitionFixedDate = sinceFixedDates[eraIndex];
        CalendarDate d = eras[eraIndex].getSinceDate();
        if (transitionFixedDate <= this.cachedFixedDate) {
            return length - (d.getDayOfMonth() - 1);
        }
        return d.getDayOfMonth() - 1;
    }

    private static int getTransitionEraIndex(Date date) {
        int eraIndex = getEraIndex(date);
        CalendarDate transitionDate = eras[eraIndex].getSinceDate();
        if (transitionDate.getYear() == date.getNormalizedYear() && transitionDate.getMonth() == date.getMonth()) {
            return eraIndex;
        }
        if (eraIndex < eras.length - 1) {
            eraIndex++;
            transitionDate = eras[eraIndex].getSinceDate();
            if (transitionDate.getYear() == date.getNormalizedYear() && transitionDate.getMonth() == date.getMonth()) {
                return eraIndex;
            }
        }
        return -1;
    }

    private boolean isTransitionYear(int normalizedYear) {
        for (int i = eras.length - 1; i > 0; i--) {
            int transitionYear = eras[i].getSinceDate().getYear();
            if (normalizedYear == transitionYear) {
                return true;
            }
            if (normalizedYear > transitionYear) {
                break;
            }
        }
        return $assertionsDisabled;
    }

    private static int getEraIndex(Date date) {
        Era era = date.getEra();
        for (int i = eras.length - 1; i > 0; i--) {
            if (eras[i] == era) {
                return i;
            }
        }
        return 0;
    }

    private JapaneseImperialCalendar getNormalizedCalendar() {
        if (isFullyNormalized()) {
            return this;
        }
        JapaneseImperialCalendar jc = (JapaneseImperialCalendar) clone();
        jc.setLenient(true);
        jc.complete();
        return jc;
    }

    private void pinDayOfMonth(Date date) {
        int year = date.getYear();
        int dom = date.getDayOfMonth();
        int monthLength;
        if (year != getMinimum(1)) {
            date.setDayOfMonth(1);
            jcal.normalize(date);
            monthLength = jcal.getMonthLength(date);
            if (dom > monthLength) {
                date.setDayOfMonth(monthLength);
            } else {
                date.setDayOfMonth(dom);
            }
            jcal.normalize(date);
            return;
        }
        Date d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
        Date realDate = jcal.getCalendarDate(this.time, getZone());
        long tod = realDate.getTimeOfDay();
        realDate.addYear((int) HttpURLConnection.HTTP_BAD_REQUEST);
        realDate.setMonth(date.getMonth());
        realDate.setDayOfMonth(1);
        jcal.normalize(realDate);
        monthLength = jcal.getMonthLength(realDate);
        if (dom > monthLength) {
            realDate.setDayOfMonth(monthLength);
        } else if (dom < d.getDayOfMonth()) {
            realDate.setDayOfMonth(d.getDayOfMonth());
        } else {
            realDate.setDayOfMonth(dom);
        }
        if (realDate.getDayOfMonth() == d.getDayOfMonth() && tod < d.getTimeOfDay()) {
            realDate.setDayOfMonth(Math.min(dom + 1, monthLength));
        }
        date.setDate(year, realDate.getMonth(), realDate.getDayOfMonth());
    }

    private static int getRolledValue(int value, int amount, int min, int max) {
        int range = (max - min) + 1;
        int n = value + (amount % range);
        if (n > max) {
            return n - range;
        }
        if (n < min) {
            return n + range;
        }
        return n;
    }

    private int internalGetEra() {
        return isSet(0) ? internalGet(0) : eras.length - 1;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (this.jdate == null) {
            this.jdate = jcal.newCalendarDate(getZone());
            this.cachedFixedDate = Long.MIN_VALUE;
        }
    }
}
