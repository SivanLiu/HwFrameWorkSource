package sun.util.calendar;

import java.net.HttpURLConnection;
import java.sql.Types;
import java.util.TimeZone;

public abstract class BaseCalendar extends AbstractCalendar {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int[] ACCUMULATED_DAYS_IN_MONTH = new int[]{-30, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, HttpURLConnection.HTTP_NOT_MODIFIED, 334};
    static final int[] ACCUMULATED_DAYS_IN_MONTH_LEAP = new int[]{-30, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, HttpURLConnection.HTTP_USE_PROXY, 335};
    public static final int APRIL = 4;
    public static final int AUGUST = 8;
    private static final int BASE_YEAR = 1970;
    static final int[] DAYS_IN_MONTH = new int[]{31, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    public static final int DECEMBER = 12;
    public static final int FEBRUARY = 2;
    private static final int[] FIXED_DATES = new int[]{719163, 719528, 719893, 720259, 720624, 720989, 721354, 721720, 722085, 722450, 722815, 723181, 723546, 723911, 724276, 724642, 725007, 725372, 725737, 726103, 726468, 726833, 727198, 727564, 727929, 728294, 728659, 729025, 729390, 729755, 730120, 730486, 730851, 731216, 731581, 731947, 732312, 732677, 733042, 733408, 733773, 734138, 734503, 734869, 735234, 735599, 735964, 736330, 736695, 737060, 737425, 737791, 738156, 738521, 738886, 739252, 739617, 739982, 740347, 740713, 741078, 741443, 741808, 742174, 742539, 742904, 743269, 743635, 744000, 744365};
    public static final int FRIDAY = 6;
    public static final int JANUARY = 1;
    public static final int JULY = 7;
    public static final int JUNE = 6;
    public static final int MARCH = 3;
    public static final int MAY = 5;
    public static final int MONDAY = 2;
    public static final int NOVEMBER = 11;
    public static final int OCTOBER = 10;
    public static final int SATURDAY = 7;
    public static final int SEPTEMBER = 9;
    public static final int SUNDAY = 1;
    public static final int THURSDAY = 5;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;

    public static abstract class Date extends CalendarDate {
        long cachedFixedDateJan1 = 731581;
        long cachedFixedDateNextJan1 = (this.cachedFixedDateJan1 + 366);
        int cachedYear = Types.BLOB;

        public abstract int getNormalizedYear();

        public abstract void setNormalizedYear(int i);

        protected Date() {
        }

        protected Date(TimeZone zone) {
            super(zone);
        }

        public Date setNormalizedDate(int normalizedYear, int month, int dayOfMonth) {
            setNormalizedYear(normalizedYear);
            setMonth(month).setDayOfMonth(dayOfMonth);
            return this;
        }

        protected final boolean hit(int year) {
            return year == this.cachedYear ? true : BaseCalendar.$assertionsDisabled;
        }

        protected final boolean hit(long fixedDate) {
            return (fixedDate < this.cachedFixedDateJan1 || fixedDate >= this.cachedFixedDateNextJan1) ? BaseCalendar.$assertionsDisabled : true;
        }

        protected int getCachedYear() {
            return this.cachedYear;
        }

        protected long getCachedJan1() {
            return this.cachedFixedDateJan1;
        }

        protected void setCache(int year, long jan1, int len) {
            this.cachedYear = year;
            this.cachedFixedDateJan1 = jan1;
            this.cachedFixedDateNextJan1 = ((long) len) + jan1;
        }
    }

    public boolean validate(CalendarDate date) {
        Date bdate = (Date) date;
        if (bdate.isNormalized()) {
            return true;
        }
        int month = bdate.getMonth();
        if (month < 1 || month > 12) {
            return $assertionsDisabled;
        }
        int d = bdate.getDayOfMonth();
        if (d <= 0 || d > getMonthLength(bdate.getNormalizedYear(), month)) {
            return $assertionsDisabled;
        }
        int dow = bdate.getDayOfWeek();
        if ((dow != Integer.MIN_VALUE && dow != getDayOfWeek(bdate)) || !validateTime(date)) {
            return $assertionsDisabled;
        }
        bdate.setNormalized(true);
        return true;
    }

    public boolean normalize(CalendarDate date) {
        if (date.isNormalized()) {
            return true;
        }
        Date bdate = (Date) date;
        if (bdate.getZone() != null) {
            getTime(date);
            return true;
        }
        int days = normalizeTime(bdate);
        normalizeMonth(bdate);
        long d = ((long) bdate.getDayOfMonth()) + ((long) days);
        int m = bdate.getMonth();
        int y = bdate.getNormalizedYear();
        int ml = getMonthLength(y, m);
        if (d > 0 && d <= ((long) ml)) {
            bdate.setDayOfWeek(getDayOfWeek(bdate));
        } else if (d <= 0 && d > -28) {
            m--;
            bdate.setDayOfMonth((int) (d + ((long) getMonthLength(y, m))));
            if (m == 0) {
                m = 12;
                bdate.setNormalizedYear(y - 1);
            }
            bdate.setMonth(m);
        } else if (d <= ((long) ml) || d >= ((long) (ml + 28))) {
            getCalendarDateFromFixedDate(bdate, (getFixedDate(y, m, 1, bdate) + d) - 1);
        } else {
            m++;
            bdate.setDayOfMonth((int) (d - ((long) ml)));
            if (m > 12) {
                bdate.setNormalizedYear(y + 1);
                m = 1;
            }
            bdate.setMonth(m);
        }
        date.setLeapYear(isLeapYear(bdate.getNormalizedYear()));
        date.setZoneOffset(0);
        date.setDaylightSaving(0);
        bdate.setNormalized(true);
        return true;
    }

    void normalizeMonth(CalendarDate date) {
        Date bdate = (Date) date;
        int year = bdate.getNormalizedYear();
        long month = (long) bdate.getMonth();
        if (month <= 0) {
            long xm = 1 - month;
            month = 13 - (xm % 12);
            bdate.setNormalizedYear(year - ((int) ((xm / 12) + 1)));
            bdate.setMonth((int) month);
        } else if (month > 12) {
            month = ((month - 1) % 12) + 1;
            bdate.setNormalizedYear(year + ((int) ((month - 1) / 12)));
            bdate.setMonth((int) month);
        }
    }

    public int getYearLength(CalendarDate date) {
        return isLeapYear(((Date) date).getNormalizedYear()) ? 366 : 365;
    }

    public int getYearLengthInMonths(CalendarDate date) {
        return 12;
    }

    public int getMonthLength(CalendarDate date) {
        Date gdate = (Date) date;
        int month = gdate.getMonth();
        if (month >= 1 && month <= 12) {
            return getMonthLength(gdate.getNormalizedYear(), month);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal month value: ");
        stringBuilder.append(month);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int getMonthLength(int year, int month) {
        int days = DAYS_IN_MONTH[month];
        if (month == 2 && isLeapYear(year)) {
            return days + 1;
        }
        return days;
    }

    public long getDayOfYear(CalendarDate date) {
        return getDayOfYear(((Date) date).getNormalizedYear(), date.getMonth(), date.getDayOfMonth());
    }

    final long getDayOfYear(int year, int month, int dayOfMonth) {
        return ((long) dayOfMonth) + ((long) (isLeapYear(year) ? ACCUMULATED_DAYS_IN_MONTH_LEAP[month] : ACCUMULATED_DAYS_IN_MONTH[month]));
    }

    public long getFixedDate(CalendarDate date) {
        if (!date.isNormalized()) {
            normalizeMonth(date);
        }
        return getFixedDate(((Date) date).getNormalizedYear(), date.getMonth(), date.getDayOfMonth(), (Date) date);
    }

    public long getFixedDate(int year, int month, int dayOfMonth, Date cache) {
        int i = year;
        int i2 = month;
        int i3 = dayOfMonth;
        Date date = cache;
        boolean isJan1 = true;
        if (!(i2 == 1 && i3 == 1)) {
            isJan1 = $assertionsDisabled;
        }
        if (date == null || !date.hit(i)) {
            int n = i - 1970;
            long prevyear;
            if (n < 0 || n >= FIXED_DATES.length) {
                prevyear = ((long) i) - 1;
                long days = (long) i3;
                if (prevyear >= 0) {
                    days += ((((365 * prevyear) + (prevyear / 4)) - (prevyear / 100)) + (prevyear / 400)) + ((long) (((367 * i2) - 362) / 12));
                } else {
                    days += ((((365 * prevyear) + CalendarUtils.floorDivide(prevyear, 4)) - CalendarUtils.floorDivide(prevyear, 100)) + CalendarUtils.floorDivide(prevyear, 400)) + ((long) CalendarUtils.floorDivide((367 * i2) - 362, 12));
                }
                if (i2 > 2) {
                    days -= isLeapYear(year) ? 1 : 2;
                }
                if (date != null && isJan1) {
                    date.setCache(i, days, isLeapYear(year) ? 366 : 365);
                }
                return days;
            }
            prevyear = (long) FIXED_DATES[n];
            if (date != null) {
                date.setCache(i, prevyear, isLeapYear(year) ? 366 : 365);
            }
            return isJan1 ? prevyear : (getDayOfYear(year, month, dayOfMonth) + prevyear) - 1;
        } else if (isJan1) {
            return cache.getCachedJan1();
        } else {
            return (cache.getCachedJan1() + getDayOfYear(year, month, dayOfMonth)) - 1;
        }
    }

    public void getCalendarDateFromFixedDate(CalendarDate date, long fixedDate) {
        int year;
        long jan1;
        boolean isLeap;
        long j = fixedDate;
        Date gdate = (Date) date;
        if (gdate.hit(j)) {
            year = gdate.getCachedYear();
            jan1 = gdate.getCachedJan1();
            isLeap = isLeapYear(year);
        } else {
            year = getGregorianYearFromFixedDate(j);
            jan1 = getFixedDate(year, 1, 1, null);
            isLeap = isLeapYear(year);
            gdate.setCache(year, jan1, isLeap ? 366 : 365);
        }
        int priorDays = (int) (j - jan1);
        long mar1 = (31 + jan1) + 28;
        if (isLeap) {
            mar1++;
        }
        if (j >= mar1) {
            priorDays += isLeap ? 1 : 2;
        }
        int month = (12 * priorDays) + 373;
        if (month > 0) {
            month /= 367;
        } else {
            month = CalendarUtils.floorDivide(month, 367);
        }
        long month1 = ((long) ACCUMULATED_DAYS_IN_MONTH[month]) + jan1;
        if (isLeap && month >= 3) {
            month1++;
        }
        int dayOfMonth = ((int) (j - month1)) + 1;
        int dayOfWeek = getDayOfWeekFromFixedDate(fixedDate);
        gdate.setNormalizedYear(year);
        gdate.setMonth(month);
        gdate.setDayOfMonth(dayOfMonth);
        gdate.setDayOfWeek(dayOfWeek);
        gdate.setLeapYear(isLeap);
        gdate.setNormalized(true);
    }

    public int getDayOfWeek(CalendarDate date) {
        return getDayOfWeekFromFixedDate(getFixedDate(date));
    }

    public static final int getDayOfWeekFromFixedDate(long fixedDate) {
        if (fixedDate >= 0) {
            return ((int) (fixedDate % 7)) + 1;
        }
        return ((int) CalendarUtils.mod(fixedDate, 7)) + 1;
    }

    public int getYearFromFixedDate(long fixedDate) {
        return getGregorianYearFromFixedDate(fixedDate);
    }

    final int getGregorianYearFromFixedDate(long fixedDate) {
        int n400;
        int n100;
        int n4;
        int n1;
        long d0;
        int d1;
        int d2;
        int d3;
        int d4;
        if (fixedDate > 0) {
            d0 = fixedDate - 1;
            n400 = (int) (d0 / 146097);
            d1 = (int) (d0 % 146097);
            n100 = d1 / 36524;
            d2 = d1 % 36524;
            n4 = d2 / 1461;
            d3 = d2 % 1461;
            n1 = d3 / 365;
            d4 = (d3 % 365) + 1;
        } else {
            d0 = fixedDate - 1;
            n400 = (int) CalendarUtils.floorDivide(d0, 146097);
            d1 = (int) CalendarUtils.mod(d0, 146097);
            n100 = CalendarUtils.floorDivide(d1, 36524);
            d2 = CalendarUtils.mod(d1, 36524);
            d3 = CalendarUtils.floorDivide(d2, 1461);
            n4 = CalendarUtils.mod(d2, 1461);
            d4 = CalendarUtils.floorDivide(n4, 365);
            int i = d3;
            n4 = i;
            int i2 = d4;
            d4 = CalendarUtils.mod(n4, 365) + 1;
            n1 = i2;
        }
        int year = (((HttpURLConnection.HTTP_BAD_REQUEST * n400) + (100 * n100)) + (4 * n4)) + n1;
        if (n100 == 4 || n1 == 4) {
            return year;
        }
        return year + 1;
    }

    protected boolean isLeapYear(CalendarDate date) {
        return isLeapYear(((Date) date).getNormalizedYear());
    }

    boolean isLeapYear(int normalizedYear) {
        return CalendarUtils.isGregorianLeapYear(normalizedYear);
    }
}
