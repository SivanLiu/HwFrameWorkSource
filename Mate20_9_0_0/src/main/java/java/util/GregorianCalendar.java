package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Locale.Category;
import libcore.util.ZoneInfo;
import sun.util.calendar.AbstractCalendar;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.BaseCalendar.Date;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Era;
import sun.util.calendar.Gregorian;
import sun.util.calendar.JulianCalendar;

public class GregorianCalendar extends Calendar {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int AD = 1;
    public static final int BC = 0;
    static final int BCE = 0;
    static final int CE = 1;
    static final long DEFAULT_GREGORIAN_CUTOVER = -12219292800000L;
    private static final int EPOCH_OFFSET = 719163;
    private static final int EPOCH_YEAR = 1970;
    static final int[] LEAP_MONTH_LENGTH = new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static final int[] LEAST_MAX_VALUES = new int[]{1, 292269054, 11, 52, 4, 28, 365, 7, 4, 1, 11, 23, 59, 59, 999, 50400000, 1200000};
    static final int[] MAX_VALUES = new int[]{1, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59, 59, 999, 50400000, 7200000};
    static final int[] MIN_VALUES = new int[]{0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -46800000, 0};
    static final int[] MONTH_LENGTH = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final long ONE_DAY = 86400000;
    private static final int ONE_HOUR = 3600000;
    private static final int ONE_MINUTE = 60000;
    private static final int ONE_SECOND = 1000;
    private static final long ONE_WEEK = 604800000;
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();
    private static JulianCalendar jcal = null;
    private static Era[] jeras = null;
    static final long serialVersionUID = -8125100834729963327L;
    private transient long cachedFixedDate;
    private transient BaseCalendar calsys;
    private transient Date cdate;
    private transient Date gdate;
    private long gregorianCutover;
    private transient long gregorianCutoverDate;
    private transient int gregorianCutoverYear;
    private transient int gregorianCutoverYearJulian;
    private transient int[] originalFields;
    private transient int[] zoneOffsets;

    public GregorianCalendar() {
        this(TimeZone.getDefaultRef(), Locale.getDefault(Category.FORMAT));
        setZoneShared(true);
    }

    public GregorianCalendar(TimeZone zone) {
        this(zone, Locale.getDefault(Category.FORMAT));
    }

    public GregorianCalendar(Locale aLocale) {
        this(TimeZone.getDefaultRef(), aLocale);
        setZoneShared(true);
    }

    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        this.gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
        this.gregorianCutoverDate = 577736;
        this.gregorianCutoverYear = 1582;
        this.gregorianCutoverYearJulian = 1582;
        this.cachedFixedDate = Long.MIN_VALUE;
        this.gdate = gcal.newCalendarDate(zone);
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(int year, int month, int dayOfMonth) {
        this(year, month, dayOfMonth, 0, 0, 0, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this(year, month, dayOfMonth, hourOfDay, minute, 0, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        this(year, month, dayOfMonth, hourOfDay, minute, second, 0);
    }

    GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second, int millis) {
        this.gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
        this.gregorianCutoverDate = 577736;
        this.gregorianCutoverYear = 1582;
        this.gregorianCutoverYearJulian = 1582;
        this.cachedFixedDate = Long.MIN_VALUE;
        this.gdate = gcal.newCalendarDate(getZone());
        set(1, year);
        set(2, month);
        set(5, dayOfMonth);
        if (hourOfDay < 12 || hourOfDay > 23) {
            internalSet(10, hourOfDay);
        } else {
            internalSet(9, 1);
            internalSet(10, hourOfDay - 12);
        }
        setFieldsComputed(1536);
        set(11, hourOfDay);
        set(12, minute);
        set(13, second);
        internalSet(14, millis);
    }

    GregorianCalendar(TimeZone zone, Locale locale, boolean flag) {
        super(zone, locale);
        this.gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
        this.gregorianCutoverDate = 577736;
        this.gregorianCutoverYear = 1582;
        this.gregorianCutoverYearJulian = 1582;
        this.cachedFixedDate = Long.MIN_VALUE;
        this.gdate = gcal.newCalendarDate(getZone());
    }

    GregorianCalendar(long milliseconds) {
        this();
        setTimeInMillis(milliseconds);
    }

    public void setGregorianChange(Date date) {
        long cutoverTime = date.getTime();
        if (cutoverTime != this.gregorianCutover) {
            complete();
            setGregorianChange(cutoverTime);
        }
    }

    private void setGregorianChange(long cutoverTime) {
        this.gregorianCutover = cutoverTime;
        this.gregorianCutoverDate = CalendarUtils.floorDivide(cutoverTime, (long) ONE_DAY) + 719163;
        if (cutoverTime == Long.MAX_VALUE) {
            this.gregorianCutoverDate++;
        }
        this.gregorianCutoverYear = getGregorianCutoverDate().getYear();
        BaseCalendar julianCal = getJulianCalendarSystem();
        Date d = (Date) julianCal.newCalendarDate(TimeZone.NO_TIMEZONE);
        julianCal.getCalendarDateFromFixedDate(d, this.gregorianCutoverDate - 1);
        this.gregorianCutoverYearJulian = d.getNormalizedYear();
        if (this.time < this.gregorianCutover) {
            setUnnormalized();
        }
    }

    public final Date getGregorianChange() {
        return new Date(this.gregorianCutover);
    }

    public boolean isLeapYear(int year) {
        int i = year & 3;
        boolean z = $assertionsDisabled;
        if (i != 0) {
            return $assertionsDisabled;
        }
        boolean z2 = true;
        if (year > this.gregorianCutoverYear) {
            if (year % 100 != 0 || year % HttpURLConnection.HTTP_BAD_REQUEST == 0) {
                z = true;
            }
            return z;
        } else if (year < this.gregorianCutoverYearJulian) {
            return true;
        } else {
            boolean gregorian;
            if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
                gregorian = getCalendarDate(this.gregorianCutoverDate).getMonth() < 3 ? true : $assertionsDisabled;
            } else {
                gregorian = year == this.gregorianCutoverYear ? true : $assertionsDisabled;
            }
            if (gregorian && year % 100 == 0 && year % HttpURLConnection.HTTP_BAD_REQUEST != 0) {
                z2 = $assertionsDisabled;
            }
            return z2;
        }
    }

    public String getCalendarType() {
        return "gregory";
    }

    public boolean equals(Object obj) {
        return ((obj instanceof GregorianCalendar) && super.equals(obj) && this.gregorianCutover == ((GregorianCalendar) obj).gregorianCutover) ? true : $assertionsDisabled;
    }

    public int hashCode() {
        return super.hashCode() ^ ((int) this.gregorianCutoverDate);
    }

    public void add(int field, int amount) {
        if (amount != 0) {
            if (field < 0 || field >= 15) {
                throw new IllegalArgumentException();
            }
            complete();
            int year;
            if (field == 1) {
                year = internalGet(1);
                if (internalGetEra() == 1) {
                    year += amount;
                    if (year > 0) {
                        set(1, year);
                    } else {
                        set(1, 1 - year);
                        set(0, 0);
                    }
                } else {
                    year -= amount;
                    if (year > 0) {
                        set(1, year);
                    } else {
                        set(1, 1 - year);
                        set(0, 1);
                    }
                }
                pinDayOfMonth();
            } else if (field == 2) {
                int y_amount;
                int month = internalGet(2) + amount;
                int year2 = internalGet(1);
                if (month >= 0) {
                    y_amount = month / 12;
                } else {
                    y_amount = ((month + 1) / 12) - 1;
                }
                if (y_amount != 0) {
                    if (internalGetEra() == 1) {
                        year2 += y_amount;
                        if (year2 > 0) {
                            set(1, year2);
                        } else {
                            set(1, 1 - year2);
                            set(0, 0);
                        }
                    } else {
                        year2 -= y_amount;
                        if (year2 > 0) {
                            set(1, year2);
                        } else {
                            set(1, 1 - year2);
                            set(0, 1);
                        }
                    }
                }
                if (month >= 0) {
                    set(2, month % 12);
                } else {
                    month %= 12;
                    if (month < 0) {
                        month += 12;
                    }
                    set(2, 0 + month);
                }
                pinDayOfMonth();
            } else if (field == 0) {
                year = internalGet(0) + amount;
                if (year < 0) {
                    year = 0;
                }
                if (year > 1) {
                    year = 1;
                }
                set(0, year);
            } else {
                long delta = (long) amount;
                long timeOfDay = 0;
                switch (field) {
                    case 3:
                    case 4:
                    case 8:
                        delta *= 7;
                        break;
                    case 9:
                        delta = (long) (amount / 2);
                        timeOfDay = (long) ((amount % 2) * 12);
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
                if (field >= 10) {
                    setTimeInMillis(this.time + delta);
                    return;
                }
                long fd = getCurrentFixedDate();
                timeOfDay = ((((((timeOfDay + ((long) internalGet(11))) * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000) + ((long) internalGet(14));
                if (timeOfDay >= ONE_DAY) {
                    fd++;
                    timeOfDay -= ONE_DAY;
                } else if (timeOfDay < 0) {
                    fd--;
                    timeOfDay += ONE_DAY;
                }
                setTimeInMillis(adjustForZoneAndDaylightSavingsTime(0, (((fd + delta) - 719163) * ONE_DAY) + timeOfDay, getZone()));
            }
        }
    }

    public void roll(int field, boolean up) {
        roll(field, up ? 1 : -1);
    }

    /* JADX WARNING: Missing block: B:63:0x0199, code skipped:
            r4 = r18;
     */
    /* JADX WARNING: Missing block: B:152:0x03ca, code skipped:
            r4 = r8;
     */
    /* JADX WARNING: Missing block: B:153:0x03cb, code skipped:
            set(r1, getRolledValue(internalGet(r27), r2, r4, r5));
     */
    /* JADX WARNING: Missing block: B:154:0x03d6, code skipped:
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
                int mon;
                int monthLen;
                int monthLen2;
                int min3;
                long fd;
                long day1;
                int value;
                long fd2;
                long month1;
                int min4;
                switch (i) {
                    case 0:
                    case 1:
                    case 9:
                    case 12:
                    case 13:
                    case 14:
                        min2 = min;
                        break;
                    case 2:
                        if (isCutoverYear(this.cdate.getNormalizedYear())) {
                            min = getActualMaximum(2) + 1;
                            mon = (internalGet(2) + amount2) % min;
                            if (mon < 0) {
                                mon += min;
                            }
                            set(2, mon);
                            monthLen = getActualMaximum(5);
                            if (internalGet(5) > monthLen) {
                                set(5, monthLen);
                            }
                        } else {
                            min = (internalGet(2) + amount2) % 12;
                            if (min < 0) {
                                min += 12;
                            }
                            set(2, min);
                            monthLen2 = monthLength(min);
                            if (internalGet(5) > monthLen2) {
                                set(5, monthLen2);
                            }
                        }
                        return;
                    case 3:
                        min3 = min;
                        monthLen2 = this.cdate.getNormalizedYear();
                        min = getActualMaximum(3);
                        set(7, internalGet(7));
                        max = internalGet(3);
                        mon = max + amount2;
                        if (isCutoverYear(monthLen2)) {
                            BaseCalendar cal;
                            min2 = min3;
                            fd = getCurrentFixedDate();
                            if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
                                cal = getCutoverCalendarSystem();
                            } else if (monthLen2 == this.gregorianCutoverYear) {
                                cal = gcal;
                            } else {
                                cal = getJulianCalendarSystem();
                            }
                            day1 = fd - ((long) ((max - min2) * 7));
                            if (cal.getYearFromFixedDate(day1) != monthLen2) {
                                min2++;
                            }
                            fd += (long) (7 * (min - max));
                            if ((fd >= this.gregorianCutoverDate ? gcal : getJulianCalendarSystem()).getYearFromFixedDate(fd) != monthLen2) {
                                min--;
                            }
                            Date d = getCalendarDate(((long) ((getRolledValue(max, amount2, min2, min) - 1) * 7)) + day1);
                            set(2, d.getMonth() - 1);
                            set(5, d.getDayOfMonth());
                            return;
                        }
                        monthLen = getWeekYear();
                        if (monthLen == monthLen2) {
                            min2 = min3;
                            if (mon <= min2 || mon >= min) {
                                fd = getCurrentFixedDate();
                                if (this.calsys.getYearFromFixedDate(fd - ((long) ((max - min2) * 7))) != monthLen2) {
                                    min2++;
                                }
                                if (this.calsys.getYearFromFixedDate(fd + ((long) (7 * (min - internalGet(3))))) != monthLen2) {
                                    min--;
                                }
                            } else {
                                set(3, mon);
                                return;
                            }
                        }
                        min2 = min3;
                        if (monthLen > monthLen2) {
                            if (amount2 < 0) {
                                amount2++;
                            }
                            max = min;
                        } else {
                            if (amount2 > 0) {
                                amount2 -= max - min;
                            }
                            max = min2;
                        }
                        set(i, getRolledValue(max, amount2, min2, min));
                        return;
                    case 4:
                        long month12;
                        boolean isCutoverYear = isCutoverYear(this.cdate.getNormalizedYear());
                        min = internalGet(7) - getFirstDayOfWeek();
                        if (min < 0) {
                            min += 7;
                        }
                        day1 = getCurrentFixedDate();
                        if (isCutoverYear) {
                            month12 = getFixedDateMonth1(this.cdate, day1);
                            mon = actualMonthLength();
                        } else {
                            month12 = (day1 - ((long) internalGet(5))) + 1;
                            mon = this.calsys.getMonthLength(this.cdate);
                        }
                        long monthDay1st = AbstractCalendar.getDayOfWeekDateOnOrBefore(month12 + 6, getFirstDayOfWeek());
                        if (((int) (monthDay1st - month12)) >= getMinimalDaysInFirstWeek()) {
                            monthDay1st -= 7;
                        }
                        value = getRolledValue(internalGet(field), amount2, 1, getActualMaximum(field)) - 1;
                        fd = (((long) (value * 7)) + monthDay1st) + ((long) min);
                        if (fd < month12) {
                            fd = month12;
                        } else if (fd >= ((long) mon) + month12) {
                            fd = (((long) mon) + month12) - 1;
                        }
                        if (isCutoverYear) {
                            monthLen = getCalendarDate(fd).getDayOfMonth();
                        } else {
                            monthLen = ((int) (fd - month12)) + 1;
                        }
                        set(5, monthLen);
                        return;
                    case 5:
                        min3 = min;
                        if (!isCutoverYear(this.cdate.getNormalizedYear())) {
                            max = this.calsys.getMonthLength(this.cdate);
                            min = min3;
                            break;
                        }
                        fd2 = getCurrentFixedDate();
                        month1 = getFixedDateMonth1(this.cdate, fd2);
                        set(5, getCalendarDate(((long) getRolledValue((int) (fd2 - month1), amount2, 0, actualMonthLength() - 1)) + month1).getDayOfMonth());
                        return;
                    case 6:
                        min4 = min;
                        max = getActualMaximum(field);
                        if (isCutoverYear(this.cdate.getNormalizedYear())) {
                            fd2 = getCurrentFixedDate();
                            month1 = (fd2 - ((long) internalGet(6))) + 1;
                            int min5 = min4;
                            Date d2 = getCalendarDate((((long) getRolledValue(((int) (fd2 - month1)) + 1, amount2, min5, max)) + month1) - 1);
                            set(2, d2.getMonth() - 1);
                            set(5, d2.getDayOfMonth());
                            return;
                        }
                        break;
                    case 7:
                        min4 = min;
                        if (!isCutoverYear(this.cdate.getNormalizedYear())) {
                            monthLen2 = internalGet(3);
                            if (monthLen2 > 1 && monthLen2 < 52) {
                                set(3, monthLen2);
                                max = 7;
                                break;
                            }
                        }
                        amount2 %= 7;
                        if (amount2 != 0) {
                            fd2 = getCurrentFixedDate();
                            day1 = AbstractCalendar.getDayOfWeekDateOnOrBefore(fd2, getFirstDayOfWeek());
                            fd2 += (long) amount2;
                            if (fd2 < day1) {
                                fd2 += 7;
                            } else if (fd2 >= day1 + 7) {
                                fd2 -= 7;
                            }
                            Date d3 = getCalendarDate(fd2);
                            set(0, d3.getNormalizedYear() <= 0 ? 0 : 1);
                            set(d3.getYear(), d3.getMonth() - 1, d3.getDayOfMonth());
                            return;
                        }
                        return;
                    case 8:
                        min = 1;
                        if (!isCutoverYear(this.cdate.getNormalizedYear())) {
                            monthLen2 = internalGet(5);
                            mon = this.calsys.getMonthLength(this.cdate);
                            max = mon / 7;
                            if ((monthLen2 - 1) % 7 < mon % 7) {
                                max++;
                            }
                            set(7, internalGet(7));
                            break;
                        }
                        month1 = getCurrentFixedDate();
                        long month13 = getFixedDateMonth1(this.cdate, month1);
                        monthLen2 = actualMonthLength();
                        max = monthLen2 / 7;
                        int x = ((int) (month1 - month13)) % 7;
                        if (x < monthLen2 % 7) {
                            max++;
                        }
                        int min6 = 1;
                        long fd3 = (((long) ((getRolledValue(internalGet(field), amount2, 1, max) - 1) * 7)) + month13) + ((long) x);
                        BaseCalendar cal2 = fd3 >= this.gregorianCutoverDate ? gcal : getJulianCalendarSystem();
                        Date min7 = (Date) cal2.newCalendarDate(TimeZone.NO_TIMEZONE);
                        cal2.getCalendarDateFromFixedDate(min7, fd3);
                        set(5, min7.getDayOfMonth());
                        return;
                    case 10:
                    case 11:
                        mon = max + 1;
                        min2 = internalGet(field);
                        value = (min2 + amount2) % mon;
                        if (value < 0) {
                            value += mon;
                        }
                        this.time += (long) (ONE_HOUR * (value - min2));
                        CalendarDate d4 = this.calsys.getCalendarDate(this.time, (TimeZone) getZone());
                        if (internalGet(5) != d4.getDayOfMonth()) {
                            d4.setDate(internalGet(1), internalGet(2) + 1, internalGet(5));
                            if (i == 10) {
                                d4.addHours(12);
                            }
                            this.time = this.calsys.getTime(d4);
                        }
                        min = d4.getHours();
                        internalSet(i, min % mon);
                        if (i == 10) {
                            internalSet(11, min);
                        } else {
                            internalSet(9, min / 12);
                            internalSet(10, min % 12);
                        }
                        monthLen = d4.getZoneOffset();
                        int saving = d4.getDaylightSaving();
                        internalSet(15, monthLen - saving);
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

    public int getMinimum(int field) {
        return MIN_VALUES[field];
    }

    public int getMaximum(int field) {
        switch (field) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                if (this.gregorianCutoverYear <= HttpURLConnection.HTTP_OK) {
                    GregorianCalendar gc = (GregorianCalendar) clone();
                    gc.setLenient(true);
                    gc.setTimeInMillis(this.gregorianCutover);
                    int v1 = gc.getActualMaximum(field);
                    gc.setTimeInMillis(this.gregorianCutover - 1);
                    return Math.max(MAX_VALUES[field], Math.max(v1, gc.getActualMaximum(field)));
                }
                break;
        }
        return MAX_VALUES[field];
    }

    public int getGreatestMinimum(int field) {
        if (field != 5) {
            return MIN_VALUES[field];
        }
        return Math.max(MIN_VALUES[field], getCalendarDate(getFixedDateMonth1(getGregorianCutoverDate(), this.gregorianCutoverDate)).getDayOfMonth());
    }

    public int getLeastMaximum(int field) {
        switch (field) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                GregorianCalendar gc = (GregorianCalendar) clone();
                gc.setLenient(true);
                gc.setTimeInMillis(this.gregorianCutover);
                int v1 = gc.getActualMaximum(field);
                gc.setTimeInMillis(this.gregorianCutover - 1);
                return Math.min(LEAST_MAX_VALUES[field], Math.min(v1, gc.getActualMaximum(field)));
            default:
                return LEAST_MAX_VALUES[field];
        }
    }

    public int getActualMinimum(int field) {
        if (field == 5) {
            GregorianCalendar gc = getNormalizedCalendar();
            int year = gc.cdate.getNormalizedYear();
            if (year == this.gregorianCutoverYear || year == this.gregorianCutoverYearJulian) {
                return getCalendarDate(getFixedDateMonth1(gc.cdate, gc.calsys.getFixedDate(gc.cdate))).getDayOfMonth();
            }
        }
        return getMinimum(field);
    }

    /* JADX WARNING: Missing block: B:71:0x0185, code skipped:
            r12 = r8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getActualMaximum(int field) {
        int i = field;
        if (((1 << i) & 130689) != 0) {
            return getMaximum(field);
        }
        int value;
        GregorianCalendar gc = getNormalizedCalendar();
        Date date = gc.cdate;
        BaseCalendar cal = gc.calsys;
        int normalizedYear = date.getNormalizedYear();
        long current;
        int value2;
        long maxEnd;
        CalendarDate d;
        int value3;
        int maxDayOfYear;
        int monthLength;
        int nDaysFirstWeek;
        switch (i) {
            case 1:
                GregorianCalendar gc2;
                if (gc == this) {
                    gc = (GregorianCalendar) clone();
                }
                current = gc.getYearOffsetInMillis();
                if (gc.internalGetEra() == 1) {
                    gc.setTimeInMillis(Long.MAX_VALUE);
                    value2 = gc.get(1);
                    if (current > gc.getYearOffsetInMillis()) {
                        value2--;
                    }
                    value = value2;
                    gc2 = gc;
                } else {
                    CalendarDate d2 = (gc.getTimeInMillis() >= this.gregorianCutover ? gcal : getJulianCalendarSystem()).getCalendarDate((long) 0, getZone());
                    gc2 = gc;
                    maxEnd = ((((((((cal.getDayOfYear(d2) - 1) * 24) + ((long) d2.getHours())) * 60) + ((long) d2.getMinutes())) * 60) + ((long) d2.getSeconds())) * 1000) + ((long) d2.getMillis());
                    value2 = d2.getYear();
                    if (value2 <= 0) {
                        value2 = 1 - value2;
                    }
                    if (current < maxEnd) {
                        value2--;
                    }
                    value = value2;
                }
                gc = gc2;
                break;
            case 2:
                if (!gc.isCutoverYear(normalizedYear)) {
                    value = 11;
                    break;
                }
                do {
                    normalizedYear++;
                    current = gcal.getFixedDate(normalizedYear, 1, 1, null);
                } while (current < this.gregorianCutoverDate);
                Date d3 = (Date) date.clone();
                cal.getCalendarDateFromFixedDate(d3, current - 1);
                value = d3.getMonth() - 1;
                break;
            case 3:
                if (!gc.isCutoverYear(normalizedYear)) {
                    d = cal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    d.setDate(date.getYear(), 1, 1);
                    value = cal.getDayOfWeek(d) - getFirstDayOfWeek();
                    if (value < 0) {
                        value += 7;
                    }
                    value3 = 52;
                    int magic = (getMinimalDaysInFirstWeek() + value) - 1;
                    if (magic == 6 || (date.isLeapYear() && (magic == 5 || magic == 12))) {
                        value = 52 + 1;
                        break;
                    }
                }
                if (gc == this) {
                    gc = (GregorianCalendar) gc.clone();
                }
                maxDayOfYear = getActualMaximum(6);
                gc.set(6, maxDayOfYear);
                value3 = gc.get(3);
                if (internalGet(1) == gc.getWeekYear()) {
                    value = value3;
                    break;
                }
                gc.set(6, maxDayOfYear - 7);
                value = gc.get(3);
                break;
            case 4:
                if (!gc.isCutoverYear(normalizedYear)) {
                    d = cal.newCalendarDate(null);
                    d.setDate(date.getYear(), date.getMonth(), 1);
                    value2 = cal.getDayOfWeek(d);
                    monthLength = cal.getMonthLength(d);
                    value2 -= getFirstDayOfWeek();
                    if (value2 < 0) {
                        value2 += 7;
                    }
                    nDaysFirstWeek = 7 - value2;
                    value3 = 3;
                    if (nDaysFirstWeek >= getMinimalDaysInFirstWeek()) {
                        value3 = 3 + 1;
                    }
                    value = value3;
                    monthLength -= nDaysFirstWeek + 21;
                    if (monthLength > 0) {
                        value++;
                        if (monthLength > 7) {
                            value++;
                            break;
                        }
                    }
                }
                if (gc == this) {
                    gc = (GregorianCalendar) gc.clone();
                }
                maxDayOfYear = gc.internalGet(1);
                nDaysFirstWeek = gc.internalGet(2);
                do {
                    value3 = gc.get(4);
                    gc.add(4, 1);
                    if (gc.get(1) == maxDayOfYear) {
                    }
                } while (gc.get(2) == nDaysFirstWeek);
                break;
            case 5:
                value = cal.getMonthLength(date);
                if (gc.isCutoverYear(normalizedYear) && date.getDayOfMonth() != value) {
                    maxEnd = gc.getCurrentFixedDate();
                    if (maxEnd < this.gregorianCutoverDate) {
                        value = gc.getCalendarDate((gc.getFixedDateMonth1(gc.cdate, maxEnd) + ((long) gc.actualMonthLength())) - 1).getDayOfMonth();
                        break;
                    }
                }
                break;
            case 6:
                if (!gc.isCutoverYear(normalizedYear)) {
                    value = cal.getYearLength(date);
                    break;
                }
                if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
                    current = gc.getCutoverCalendarSystem().getFixedDate(normalizedYear, 1, 1, null);
                } else if (normalizedYear == this.gregorianCutoverYearJulian) {
                    current = cal.getFixedDate(normalizedYear, 1, 1, null);
                } else {
                    current = this.gregorianCutoverDate;
                }
                long nextJan1 = gcal.getFixedDate(normalizedYear + 1, 1, 1, null);
                if (nextJan1 < this.gregorianCutoverDate) {
                    nextJan1 = this.gregorianCutoverDate;
                }
                value = (int) (nextJan1 - current);
                break;
            case 8:
                maxDayOfYear = date.getDayOfWeek();
                if (gc.isCutoverYear(normalizedYear)) {
                    if (gc == this) {
                        gc = (GregorianCalendar) clone();
                    }
                    value2 = gc.actualMonthLength();
                    gc.set(5, gc.getActualMinimum(5));
                    nDaysFirstWeek = value2;
                    value2 = gc.get(7);
                } else {
                    Date d4 = (Date) date.clone();
                    nDaysFirstWeek = cal.getMonthLength(d4);
                    d4.setDayOfMonth(1);
                    cal.normalize(d4);
                    value2 = d4.getDayOfWeek();
                }
                monthLength = maxDayOfYear - value2;
                if (monthLength < 0) {
                    monthLength += 7;
                }
                value = ((nDaysFirstWeek - monthLength) + 6) / 7;
                break;
            default:
                throw new ArrayIndexOutOfBoundsException(i);
        }
        return value;
    }

    private long getYearOffsetInMillis() {
        return (((long) internalGet(14)) + ((((((((long) ((internalGet(6) - 1) * 24)) + ((long) internalGet(11))) * 60) + ((long) internalGet(12))) * 60) + ((long) internalGet(13))) * 1000)) - ((long) (internalGet(15) + internalGet(16)));
    }

    public Object clone() {
        GregorianCalendar other = (GregorianCalendar) super.clone();
        other.gdate = (Date) this.gdate.clone();
        if (this.cdate != null) {
            if (this.cdate != this.gdate) {
                other.cdate = (Date) this.cdate.clone();
            } else {
                other.cdate = other.gdate;
            }
        }
        other.originalFields = null;
        other.zoneOffsets = null;
        return other;
    }

    public TimeZone getTimeZone() {
        TimeZone zone = super.getTimeZone();
        this.gdate.setZone(zone);
        if (!(this.cdate == null || this.cdate == this.gdate)) {
            this.cdate.setZone(zone);
        }
        return zone;
    }

    public void setTimeZone(TimeZone zone) {
        super.setTimeZone(zone);
        this.gdate.setZone(zone);
        if (this.cdate != null && this.cdate != this.gdate) {
            this.cdate.setZone(zone);
        }
    }

    public final boolean isWeekDateSupported() {
        return true;
    }

    public int getWeekYear() {
        int year = get(1);
        if (internalGetEra() == 0) {
            year = 1 - year;
        }
        int weekOfYear;
        if (year > this.gregorianCutoverYear + 1) {
            weekOfYear = internalGet(3);
            if (internalGet(2) == 0) {
                if (weekOfYear >= 52) {
                    year--;
                }
            } else if (weekOfYear == 1) {
                year++;
            }
            return year;
        }
        int dayOfYear = internalGet(6);
        int maxDayOfYear = getActualMaximum(6);
        int minimalDays = getMinimalDaysInFirstWeek();
        if (dayOfYear > minimalDays && dayOfYear < maxDayOfYear - 6) {
            return year;
        }
        GregorianCalendar cal = (GregorianCalendar) clone();
        cal.setLenient(true);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(6, 1);
        cal.complete();
        int delta = getFirstDayOfWeek() - cal.get(7);
        if (delta != 0) {
            if (delta < 0) {
                delta += 7;
            }
            cal.add(6, delta);
        }
        int minDayOfYear = cal.get(6);
        if (dayOfYear >= minDayOfYear) {
            cal.set(1, year + 1);
            cal.set(6, 1);
            cal.complete();
            int del = getFirstDayOfWeek() - cal.get(7);
            if (del != 0) {
                if (del < 0) {
                    del += 7;
                }
                cal.add(6, del);
            }
            weekOfYear = cal.get(6) - 1;
            if (weekOfYear == 0) {
                weekOfYear = 7;
            }
            minDayOfYear = weekOfYear;
            if (minDayOfYear >= minimalDays && (maxDayOfYear - dayOfYear) + 1 <= 7 - minDayOfYear) {
                year++;
            }
        } else if (minDayOfYear <= minimalDays) {
            year--;
        }
        return year;
    }

    public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid dayOfWeek: ");
            stringBuilder.append(dayOfWeek);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        GregorianCalendar gc = (GregorianCalendar) clone();
        gc.setLenient(true);
        int era = gc.get(0);
        gc.clear();
        gc.setTimeZone(TimeZone.getTimeZone("GMT"));
        gc.set(0, era);
        gc.set(1, weekYear);
        gc.set(3, 1);
        gc.set(7, getFirstDayOfWeek());
        int days = dayOfWeek - getFirstDayOfWeek();
        if (days < 0) {
            days += 7;
        }
        days += (weekOfYear - 1) * 7;
        if (days != 0) {
            gc.add(6, days);
        } else {
            gc.complete();
        }
        if (isLenient() || (gc.getWeekYear() == weekYear && gc.internalGet(3) == weekOfYear && gc.internalGet(7) == dayOfWeek)) {
            set(0, gc.internalGet(0));
            set(1, gc.internalGet(1));
            set(2, gc.internalGet(2));
            set(5, gc.internalGet(5));
            internalSet(3, weekOfYear);
            complete();
            return;
        }
        throw new IllegalArgumentException();
    }

    public int getWeeksInWeekYear() {
        GregorianCalendar gc = getNormalizedCalendar();
        int weekYear = gc.getWeekYear();
        if (weekYear == gc.internalGet(1)) {
            return gc.getActualMaximum(3);
        }
        if (gc == this) {
            gc = (GregorianCalendar) gc.clone();
        }
        gc.setWeekDate(weekYear, 2, internalGet(7));
        return gc.getActualMaximum(3);
    }

    protected void computeFields() {
        int mask;
        if (isPartiallyNormalized()) {
            mask = getSetStateFields();
            int fieldMask = (~mask) & 131071;
            if (fieldMask != 0 || this.calsys == null) {
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
        int year;
        int i = fieldMask;
        int i2 = tzMask;
        int zoneOffset = 0;
        TimeZone tz = getZone();
        if (this.zoneOffsets == null) {
            this.zoneOffsets = new int[2];
        }
        if (i2 != 98304) {
            if (tz instanceof ZoneInfo) {
                zoneOffset = ((ZoneInfo) tz).getOffsetsByUtcTime(this.time, this.zoneOffsets);
            } else {
                zoneOffset = tz.getOffset(this.time);
                this.zoneOffsets[0] = tz.getRawOffset();
                this.zoneOffsets[1] = zoneOffset - this.zoneOffsets[0];
            }
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
        int era = 1;
        if (fixedDate >= this.gregorianCutoverDate) {
            if (fixedDate != this.cachedFixedDate) {
                gcal.getCalendarDateFromFixedDate(this.gdate, fixedDate);
                this.cachedFixedDate = fixedDate;
            }
            year = this.gdate.getYear();
            if (year <= 0) {
                year = 1 - year;
                era = 0;
            }
            this.calsys = gcal;
            this.cdate = this.gdate;
        } else {
            this.calsys = getJulianCalendarSystem();
            this.cdate = jcal.newCalendarDate(getZone());
            jcal.getCalendarDateFromFixedDate(this.cdate, fixedDate);
            if (this.cdate.getEra() == jeras[0]) {
                era = 0;
            }
            year = this.cdate.getYear();
        }
        internalSet(0, era);
        internalSet(1, year);
        int mask = i | 3;
        int month = this.cdate.getMonth() - 1;
        int dayOfMonth = this.cdate.getDayOfMonth();
        if ((i & 164) != 0) {
            internalSet(2, month);
            internalSet(5, dayOfMonth);
            internalSet(7, this.cdate.getDayOfWeek());
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
        int i4;
        int i5;
        int i6;
        if ((i & 344) != 0) {
            int relativeDayOfMonth;
            int mask2;
            i2 = this.cdate.getNormalizedYear();
            long fixedDateJan1 = this.calsys.getFixedDate(i2, 1, 1, this.cdate);
            zoneOffset = ((int) (fixedDate - fixedDateJan1)) + 1;
            timeOfDay2 = 0;
            long fixedDateMonth1 = (fixedDate - ((long) dayOfMonth)) + 1;
            i = this.calsys == gcal ? this.gregorianCutoverYear : this.gregorianCutoverYearJulian;
            int relativeDayOfMonth2 = dayOfMonth - 1;
            if (i2 == i) {
                if (this.gregorianCutoverYearJulian <= this.gregorianCutoverYear) {
                    relativeDayOfMonth2 = getFixedDateJan1(this.cdate, fixedDate);
                    if (fixedDate >= this.gregorianCutoverDate) {
                        long j = relativeDayOfMonth2;
                        tz = getFixedDateMonth1(this.cdate, fixedDate);
                        fixedDateJan1 = j;
                        month = ((int) (fixedDate - fixedDateJan1)) + 1;
                        timeOfDay2 = zoneOffset - month;
                        zoneOffset = month;
                        relativeDayOfMonth = (int) (fixedDate - tz);
                        fixedDateJan1 = fixedDateJan1;
                    } else {
                        fixedDateJan1 = relativeDayOfMonth2;
                    }
                }
                tz = fixedDateMonth1;
                month = ((int) (fixedDate - fixedDateJan1)) + 1;
                timeOfDay2 = zoneOffset - month;
                zoneOffset = month;
                relativeDayOfMonth = (int) (fixedDate - tz);
                fixedDateJan1 = fixedDateJan1;
            } else {
                relativeDayOfMonth = relativeDayOfMonth2;
                i3 = year;
                i4 = month;
                i5 = dayOfMonth;
                tz = fixedDateMonth1;
            }
            internalSet(6, zoneOffset);
            internalSet(8, (relativeDayOfMonth / 7) + 1);
            year = getWeekNumber(fixedDateJan1, fixedDate);
            long fixedDec31;
            int dayOfYear;
            int i7;
            int i8;
            if (year == 0) {
                fixedDec31 = fixedDateJan1 - 1;
                long prevJan1 = fixedDateJan1 - 365;
                dayOfYear = zoneOffset;
                if (i2 > i + 1) {
                    if (CalendarUtils.isGregorianLeapYear(i2 - 1) != 0) {
                        prevJan1--;
                    }
                    i6 = timeOfDay;
                    mask2 = mask;
                    i7 = timeOfDay2;
                    timeOfDay = prevJan1;
                    year = getWeekNumber(timeOfDay, fixedDec31);
                    i8 = i2;
                } else if (i2 <= this.gregorianCutoverYearJulian) {
                    if (CalendarUtils.isJulianLeapYear(i2 - 1) != 0) {
                        prevJan1--;
                    }
                    i6 = timeOfDay;
                    mask2 = mask;
                    i7 = timeOfDay2;
                    timeOfDay = prevJan1;
                    year = getWeekNumber(timeOfDay, fixedDec31);
                    i8 = i2;
                } else {
                    zoneOffset = this.calsys;
                    int i9 = i;
                    i = getCalendarDate(fixedDec31).getNormalizedYear();
                    BaseCalendar calForJan1 = zoneOffset;
                    if (i == this.gregorianCutoverYear) {
                        zoneOffset = getCutoverCalendarSystem();
                        i6 = timeOfDay;
                        if (zoneOffset == jcal) {
                            i7 = timeOfDay2;
                            prevJan1 = zoneOffset.getFixedDate(i, 1, 1, 0);
                            mask2 = mask;
                        } else {
                            mask2 = mask;
                            timeOfDay = this.gregorianCutoverDate;
                            zoneOffset = gcal;
                            year = getWeekNumber(timeOfDay, fixedDec31);
                            i8 = i2;
                        }
                    } else {
                        mask2 = mask;
                        i7 = timeOfDay2;
                        if (i <= this.gregorianCutoverYearJulian) {
                            prevJan1 = getJulianCalendarSystem().getFixedDate(i, 1, 1, null);
                        }
                        timeOfDay = prevJan1;
                        year = getWeekNumber(timeOfDay, fixedDec31);
                        i8 = i2;
                    }
                    timeOfDay = prevJan1;
                    year = getWeekNumber(timeOfDay, fixedDec31);
                    i8 = i2;
                }
                mask2 = mask;
                timeOfDay = prevJan1;
                year = getWeekNumber(timeOfDay, fixedDec31);
                i8 = i2;
            } else {
                long nextJan1st;
                dayOfYear = zoneOffset;
                i6 = timeOfDay;
                mask2 = mask;
                i7 = timeOfDay2;
                if (i2 > this.gregorianCutoverYear) {
                } else if (i2 < this.gregorianCutoverYearJulian - 1) {
                    i8 = i2;
                } else {
                    BaseCalendar calForJan12 = this.calsys;
                    timeOfDay = i2 + 1;
                    if (timeOfDay == this.gregorianCutoverYearJulian + 1 && timeOfDay < this.gregorianCutoverYear) {
                        timeOfDay = this.gregorianCutoverYear;
                    }
                    if (timeOfDay == this.gregorianCutoverYear) {
                        calForJan12 = getCutoverCalendarSystem();
                    }
                    if (timeOfDay > this.gregorianCutoverYear || this.gregorianCutoverYearJulian == this.gregorianCutoverYear || timeOfDay == this.gregorianCutoverYearJulian) {
                        fixedDec31 = calForJan12.getFixedDate(timeOfDay, 1, 1, null);
                    } else {
                        fixedDec31 = this.gregorianCutoverDate;
                        calForJan12 = gcal;
                    }
                    nextJan1st = AbstractCalendar.getDayOfWeekDateOnOrBefore(fixedDec31 + 6, getFirstDayOfWeek());
                    if (((int) (nextJan1st - fixedDec31)) >= getMinimalDaysInFirstWeek() && fixedDate >= nextJan1st - 7) {
                        year = 1;
                    }
                }
                if (year >= 52) {
                    nextJan1st = 365 + fixedDateJan1;
                    if (this.cdate.isLeapYear() != 0) {
                        nextJan1st++;
                    }
                    timeOfDay = AbstractCalendar.getDayOfWeekDateOnOrBefore(6 + nextJan1st, getFirstDayOfWeek());
                    if (((int) (timeOfDay - nextJan1st)) >= getMinimalDaysInFirstWeek() && fixedDate >= timeOfDay - 7) {
                        year = 1;
                    }
                }
            }
            internalSet(3, year);
            internalSet(4, getWeekNumber(tz, fixedDate));
            return mask2 | 344;
        }
        TimeZone timeZone = tz;
        int i10 = era;
        i3 = year;
        i6 = timeOfDay;
        i4 = month;
        i5 = dayOfMonth;
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

    /* JADX WARNING: Removed duplicated region for block: B:85:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0128  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0126  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0126  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0128  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0183  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void computeTime() {
        int field;
        long timeOfDay;
        long fixedDate;
        long fixedDate2;
        long millis;
        int mask;
        long j;
        if (!isLenient()) {
            if (this.originalFields == null) {
                this.originalFields = new int[17];
            }
            field = 0;
            while (field < 17) {
                int value = internalGet(field);
                if (!isExternallySet(field) || (value >= getMinimum(field) && value <= getMaximum(field))) {
                    this.originalFields[field] = value;
                    field++;
                } else {
                    throw new IllegalArgumentException(Calendar.getFieldName(field));
                }
            }
        }
        field = selectFields();
        int year = isSet(1) ? internalGet(1) : EPOCH_YEAR;
        int era = internalGetEra();
        if (era == 0) {
            year = 1 - year;
        } else if (era != 1) {
            throw new IllegalArgumentException("Invalid era");
        }
        if (year <= 0 && !isSet(0)) {
            field |= 1;
            setFieldsComputed(1);
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
        long fixedDate3 = timeOfDay / ONE_DAY;
        timeOfDay %= ONE_DAY;
        while (timeOfDay < 0) {
            timeOfDay += ONE_DAY;
            fixedDate3--;
        }
        long gfd;
        if (year > this.gregorianCutoverYear && year > this.gregorianCutoverYearJulian) {
            gfd = getFixedDate(gcal, year, field) + fixedDate3;
            if (gfd >= this.gregorianCutoverDate) {
                fixedDate = gfd;
            } else {
                fixedDate = getFixedDate(getJulianCalendarSystem(), year, field) + fixedDate3;
                if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
                }
                fixedDate2 = fixedDate3;
                fixedDate3 = 98304 & field;
                millis = adjustForZoneAndDaylightSavingsTime(fixedDate3, ((fixedDate2 - 719163) * ONE_DAY) + timeOfDay, getZone());
                this.time = millis;
                mask = computeFields(getSetStateFields() | field, fixedDate3);
                if (!isLenient()) {
                }
                j = millis;
                setFieldsNormalized(mask);
            }
        } else if (year >= this.gregorianCutoverYear || year >= this.gregorianCutoverYearJulian) {
            fixedDate = getFixedDate(getJulianCalendarSystem(), year, field) + fixedDate3;
            gfd = getFixedDate(gcal, year, field) + fixedDate3;
            if (Calendar.isFieldSet(field, 6) || Calendar.isFieldSet(field, 3)) {
                if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
                    fixedDate3 = fixedDate;
                } else if (year == this.gregorianCutoverYear) {
                    fixedDate3 = gfd;
                }
                fixedDate2 = fixedDate3;
                fixedDate3 = 98304 & field;
                millis = adjustForZoneAndDaylightSavingsTime(fixedDate3, ((fixedDate2 - 719163) * ONE_DAY) + timeOfDay, getZone());
                this.time = millis;
                mask = computeFields(getSetStateFields() | field, fixedDate3);
                if (isLenient()) {
                    gfd = null;
                    while (gfd < 17) {
                        if (isExternallySet(gfd) && this.originalFields[gfd] != internalGet(gfd)) {
                            String s = new StringBuilder();
                            s.append(this.originalFields[gfd]);
                            s.append(" -> ");
                            s.append(internalGet(gfd));
                            s = s.toString();
                            System.arraycopy(this.originalFields, 0, this.fields, 0, this.fields.length);
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(Calendar.getFieldName(gfd));
                            stringBuilder.append(": ");
                            stringBuilder.append(s);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        gfd++;
                    }
                }
                j = millis;
                setFieldsNormalized(mask);
            }
            if (gfd >= this.gregorianCutoverDate) {
                fixedDate2 = fixedDate >= this.gregorianCutoverDate ? gfd : (this.calsys == gcal || this.calsys == null) ? gfd : fixedDate;
            } else if (fixedDate < this.gregorianCutoverDate) {
                fixedDate2 = fixedDate;
            } else if (!isLenient()) {
                throw new IllegalArgumentException("the specified date doesn't exist");
            }
            fixedDate3 = 98304 & field;
            millis = adjustForZoneAndDaylightSavingsTime(fixedDate3, ((fixedDate2 - 719163) * ONE_DAY) + timeOfDay, getZone());
            this.time = millis;
            mask = computeFields(getSetStateFields() | field, fixedDate3);
            if (isLenient()) {
            }
            j = millis;
            setFieldsNormalized(mask);
        } else {
            fixedDate = getFixedDate(getJulianCalendarSystem(), year, field) + fixedDate3;
            if (fixedDate < this.gregorianCutoverDate) {
                fixedDate3 = fixedDate;
                fixedDate2 = fixedDate3;
                fixedDate3 = 98304 & field;
                millis = adjustForZoneAndDaylightSavingsTime(fixedDate3, ((fixedDate2 - 719163) * ONE_DAY) + timeOfDay, getZone());
                this.time = millis;
                mask = computeFields(getSetStateFields() | field, fixedDate3);
                if (isLenient()) {
                }
                j = millis;
                setFieldsNormalized(mask);
            }
            gfd = fixedDate;
            if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian) {
            }
            fixedDate2 = fixedDate3;
            fixedDate3 = 98304 & field;
            millis = adjustForZoneAndDaylightSavingsTime(fixedDate3, ((fixedDate2 - 719163) * ONE_DAY) + timeOfDay, getZone());
            this.time = millis;
            mask = computeFields(getSetStateFields() | field, fixedDate3);
            if (isLenient()) {
            }
            j = millis;
            setFieldsNormalized(mask);
        }
        fixedDate2 = fixedDate;
        fixedDate3 = 98304 & field;
        millis = adjustForZoneAndDaylightSavingsTime(fixedDate3, ((fixedDate2 - 719163) * ONE_DAY) + timeOfDay, getZone());
        this.time = millis;
        mask = computeFields(getSetStateFields() | field, fixedDate3);
        if (isLenient()) {
        }
        j = millis;
        setFieldsNormalized(mask);
    }

    private long adjustForZoneAndDaylightSavingsTime(int tzMask, long utcTimeInMillis, TimeZone zone) {
        int zoneOffset = 0;
        int dstOffset = 0;
        if (tzMask != 98304) {
            if (this.zoneOffsets == null) {
                this.zoneOffsets = new int[2];
            }
            long standardTimeInZone = utcTimeInMillis - ((long) (Calendar.isFieldSet(tzMask, 15) ? internalGet(15) : zone.getRawOffset()));
            if (zone instanceof ZoneInfo) {
                ((ZoneInfo) zone).getOffsetsByUtcTime(standardTimeInZone, this.zoneOffsets);
            } else {
                zone.getOffsets(standardTimeInZone, this.zoneOffsets);
            }
            zoneOffset = this.zoneOffsets[0];
            dstOffset = adjustDstOffsetForInvalidWallClock(standardTimeInZone, zone, this.zoneOffsets[1]);
        }
        if (tzMask != 0) {
            if (Calendar.isFieldSet(tzMask, 15)) {
                zoneOffset = internalGet(15);
            }
            if (Calendar.isFieldSet(tzMask, 16)) {
                dstOffset = internalGet(16);
            }
        }
        return (utcTimeInMillis - ((long) zoneOffset)) - ((long) dstOffset);
    }

    private int adjustDstOffsetForInvalidWallClock(long standardTimeInZone, TimeZone zone, int dstOffset) {
        if (dstOffset == 0 || zone.inDaylightTime(new Date(standardTimeInZone - ((long) dstOffset)))) {
            return dstOffset;
        }
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0039  */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0036  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00d5  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x004b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long getFixedDate(BaseCalendar cal, int year, int fieldMask) {
        int year2;
        long fixedDate;
        BaseCalendar baseCalendar = cal;
        int i = fieldMask;
        int month = 0;
        int dowim = 1;
        if (Calendar.isFieldSet(i, 2)) {
            month = internalGet(2);
            if (month > 11) {
                year2 = year + (month / 12);
                month %= 12;
            } else if (month < 0) {
                int[] rem = new int[1];
                year2 = year + CalendarUtils.floorDivide(month, 12, rem);
                month = rem[0];
            }
            fixedDate = baseCalendar.getFixedDate(year2, month + 1, 1, baseCalendar != gcal ? this.gdate : null);
            if (Calendar.isFieldSet(i, 2)) {
                if (year2 == this.gregorianCutoverYear && baseCalendar == gcal && fixedDate < this.gregorianCutoverDate && this.gregorianCutoverYear != this.gregorianCutoverYearJulian) {
                    fixedDate = this.gregorianCutoverDate;
                }
                if (Calendar.isFieldSet(i, 6)) {
                    return (fixedDate + ((long) internalGet(6))) - 1;
                }
                long firstDayOfWeek = AbstractCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                if (firstDayOfWeek - fixedDate >= ((long) getMinimalDaysInFirstWeek())) {
                    firstDayOfWeek -= 7;
                }
                if (Calendar.isFieldSet(i, 7)) {
                    int dayOfWeek = internalGet(7);
                    if (dayOfWeek != getFirstDayOfWeek()) {
                        firstDayOfWeek = AbstractCalendar.getDayOfWeekDateOnOrBefore(6 + firstDayOfWeek, dayOfWeek);
                    }
                }
                return firstDayOfWeek + (7 * (((long) internalGet(3)) - 1));
            } else if (Calendar.isFieldSet(i, 5)) {
                if (isSet(5)) {
                    return (fixedDate + ((long) internalGet(5))) - 1;
                }
                return fixedDate;
            } else if (Calendar.isFieldSet(i, 4)) {
                long firstDayOfWeek2 = AbstractCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                if (firstDayOfWeek2 - fixedDate >= ((long) getMinimalDaysInFirstWeek())) {
                    firstDayOfWeek2 -= 7;
                }
                if (Calendar.isFieldSet(i, 7)) {
                    firstDayOfWeek2 = AbstractCalendar.getDayOfWeekDateOnOrBefore(firstDayOfWeek2 + 6, internalGet(7));
                }
                return firstDayOfWeek2 + ((long) (7 * (internalGet(4) - 1)));
            } else {
                long fixedDate2;
                if (Calendar.isFieldSet(i, 7)) {
                    fixedDate2 = internalGet(7);
                } else {
                    fixedDate2 = getFirstDayOfWeek();
                }
                if (Calendar.isFieldSet(i, 8)) {
                    dowim = internalGet(8);
                }
                if (dowim >= 0) {
                    return AbstractCalendar.getDayOfWeekDateOnOrBefore((((long) (7 * dowim)) + fixedDate) - 1, fixedDate2);
                }
                return AbstractCalendar.getDayOfWeekDateOnOrBefore((((long) (monthLength(month, year2) + (7 * (dowim + 1)))) + fixedDate) - 1, fixedDate2);
            }
        }
        year2 = year;
        if (baseCalendar != gcal) {
        }
        fixedDate = baseCalendar.getFixedDate(year2, month + 1, 1, baseCalendar != gcal ? this.gdate : null);
        if (Calendar.isFieldSet(i, 2)) {
        }
    }

    private GregorianCalendar getNormalizedCalendar() {
        if (isFullyNormalized()) {
            return this;
        }
        GregorianCalendar gc = (GregorianCalendar) clone();
        gc.setLenient(true);
        gc.complete();
        return gc;
    }

    private static synchronized BaseCalendar getJulianCalendarSystem() {
        JulianCalendar julianCalendar;
        synchronized (GregorianCalendar.class) {
            if (jcal == null) {
                jcal = (JulianCalendar) CalendarSystem.forName("julian");
                jeras = jcal.getEras();
            }
            julianCalendar = jcal;
        }
        return julianCalendar;
    }

    private BaseCalendar getCutoverCalendarSystem() {
        if (this.gregorianCutoverYearJulian < this.gregorianCutoverYear) {
            return gcal;
        }
        return getJulianCalendarSystem();
    }

    private boolean isCutoverYear(int normalizedYear) {
        return normalizedYear == (this.calsys == gcal ? this.gregorianCutoverYear : this.gregorianCutoverYearJulian) ? true : $assertionsDisabled;
    }

    private long getFixedDateJan1(Date date, long fixedDate) {
        if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian || fixedDate < this.gregorianCutoverDate) {
            return getJulianCalendarSystem().getFixedDate(date.getNormalizedYear(), 1, 1, null);
        }
        return this.gregorianCutoverDate;
    }

    private long getFixedDateMonth1(Date date, long fixedDate) {
        Date gCutover = getGregorianCutoverDate();
        if (gCutover.getMonth() == 1 && gCutover.getDayOfMonth() == 1) {
            return (fixedDate - ((long) date.getDayOfMonth())) + 1;
        }
        long fixedDateMonth1;
        if (date.getMonth() == gCutover.getMonth()) {
            long fixedDateMonth12;
            Date jLastDate = getLastJulianDate();
            if (this.gregorianCutoverYear == this.gregorianCutoverYearJulian && gCutover.getMonth() == jLastDate.getMonth()) {
                fixedDateMonth12 = jcal.getFixedDate(date.getNormalizedYear(), date.getMonth(), 1, null);
            } else {
                fixedDateMonth12 = this.gregorianCutoverDate;
            }
            fixedDateMonth1 = fixedDateMonth12;
        } else {
            fixedDateMonth1 = (fixedDate - ((long) date.getDayOfMonth())) + 1;
        }
        return fixedDateMonth1;
    }

    private Date getCalendarDate(long fd) {
        BaseCalendar cal = fd >= this.gregorianCutoverDate ? gcal : getJulianCalendarSystem();
        Date d = (Date) cal.newCalendarDate(TimeZone.NO_TIMEZONE);
        cal.getCalendarDateFromFixedDate(d, fd);
        return d;
    }

    private Date getGregorianCutoverDate() {
        return getCalendarDate(this.gregorianCutoverDate);
    }

    private Date getLastJulianDate() {
        return getCalendarDate(this.gregorianCutoverDate - 1);
    }

    private int monthLength(int month, int year) {
        return isLeapYear(year) ? LEAP_MONTH_LENGTH[month] : MONTH_LENGTH[month];
    }

    private int monthLength(int month) {
        int year = internalGet(1);
        if (internalGetEra() == 0) {
            year = 1 - year;
        }
        return monthLength(month, year);
    }

    private int actualMonthLength() {
        int year = this.cdate.getNormalizedYear();
        if (year != this.gregorianCutoverYear && year != this.gregorianCutoverYearJulian) {
            return this.calsys.getMonthLength(this.cdate);
        }
        Date date = (Date) this.cdate.clone();
        long month1 = getFixedDateMonth1(date, this.calsys.getFixedDate(date));
        long next1 = ((long) this.calsys.getMonthLength(date)) + month1;
        if (next1 < this.gregorianCutoverDate) {
            return (int) (next1 - month1);
        }
        if (this.cdate != this.gdate) {
            date = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        }
        gcal.getCalendarDateFromFixedDate(date, next1);
        return (int) (getFixedDateMonth1(date, next1) - month1);
    }

    private int yearLength(int year) {
        return isLeapYear(year) ? 366 : 365;
    }

    private int yearLength() {
        int year = internalGet(1);
        if (internalGetEra() == 0) {
            year = 1 - year;
        }
        return yearLength(year);
    }

    private void pinDayOfMonth() {
        int monthLen;
        int year = internalGet(1);
        if (year > this.gregorianCutoverYear || year < this.gregorianCutoverYearJulian) {
            monthLen = monthLength(internalGet(2));
        } else {
            monthLen = getNormalizedCalendar().getActualMaximum(5);
        }
        if (internalGet(5) > monthLen) {
            set(5, monthLen);
        }
    }

    private long getCurrentFixedDate() {
        return this.calsys == gcal ? this.cachedFixedDate : this.calsys.getFixedDate(this.cdate);
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
        return isSet(0) ? internalGet(0) : 1;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (this.gdate == null) {
            this.gdate = gcal.newCalendarDate(getZone());
            this.cachedFixedDate = Long.MIN_VALUE;
        }
        setGregorianChange(this.gregorianCutover);
    }

    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(getTimeInMillis()), getTimeZone().toZoneId());
    }

    public static GregorianCalendar from(ZonedDateTime zdt) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone(zdt.getZone()));
        cal.setGregorianChange(new Date(Long.MIN_VALUE));
        cal.setFirstDayOfWeek(2);
        cal.setMinimalDaysInFirstWeek(4);
        try {
            cal.setTimeInMillis(Math.addExact(Math.multiplyExact(zdt.toEpochSecond(), 1000), (long) zdt.get(ChronoField.MILLI_OF_SECOND)));
            return cal;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
