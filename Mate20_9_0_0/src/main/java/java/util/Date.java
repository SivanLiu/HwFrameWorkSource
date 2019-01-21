package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.time.Instant;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;

public class Date implements Serializable, Cloneable, Comparable<Date> {
    private static int defaultCenturyStart = 0;
    private static final BaseCalendar gcal = CalendarSystem.getGregorianCalendar();
    private static BaseCalendar jcal = null;
    private static final long serialVersionUID = 7523967970034938905L;
    private static final int[] ttb = new int[]{14, 1, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 10000, 10000, 10000, 10300, 10240, 10360, 10300, 10420, 10360, 10480, 10420};
    private static final String[] wtb = new String[]{"am", "pm", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december", "gmt", "ut", "utc", "est", "edt", "cst", "cdt", "mst", "mdt", "pst", "pdt"};
    private transient sun.util.calendar.BaseCalendar.Date cdate;
    private transient long fastTime;

    public Date() {
        this(System.currentTimeMillis());
    }

    public Date(long date) {
        this.fastTime = date;
    }

    @Deprecated
    public Date(int year, int month, int date) {
        this(year, month, date, 0, 0, 0);
    }

    @Deprecated
    public Date(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    @Deprecated
    public Date(int year, int month, int date, int hrs, int min, int sec) {
        int y = year + 1900;
        if (month >= 12) {
            y += month / 12;
            month %= 12;
        } else if (month < 0) {
            y += CalendarUtils.floorDivide(month, 12);
            month = CalendarUtils.mod(month, 12);
        }
        this.cdate = (sun.util.calendar.BaseCalendar.Date) getCalendarSystem(y).newCalendarDate(TimeZone.getDefaultRef());
        this.cdate.setNormalizedDate(y, month + 1, date).setTimeOfDay(hrs, min, sec, 0);
        getTimeImpl();
        this.cdate = null;
    }

    @Deprecated
    public Date(String s) {
        this(parse(s));
    }

    public Object clone() {
        Date d = null;
        try {
            d = (Date) super.clone();
            if (this.cdate != null) {
                d.cdate = (sun.util.calendar.BaseCalendar.Date) this.cdate.clone();
            }
        } catch (CloneNotSupportedException e) {
        }
        return d;
    }

    @Deprecated
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        int y = year + 1900;
        if (month >= 12) {
            y += month / 12;
            month %= 12;
        } else if (month < 0) {
            y += CalendarUtils.floorDivide(month, 12);
            month = CalendarUtils.mod(month, 12);
        }
        sun.util.calendar.BaseCalendar.Date udate = (sun.util.calendar.BaseCalendar.Date) getCalendarSystem(y).newCalendarDate(null);
        udate.setNormalizedDate(y, month + 1, date).setTimeOfDay(hrs, min, sec, 0);
        Date d = new Date(0);
        d.normalize(udate);
        return d.fastTime;
    }

    /* JADX WARNING: Missing block: B:36:0x007b, code skipped:
            if (r10 != Integer.MIN_VALUE) goto L_0x00ea;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public static long parse(String s) {
        String hour = s;
        int hour2 = -1;
        int millis = -1;
        int i = 0;
        int min;
        int sec;
        int n;
        int tzoffset;
        int n2;
        int tzoffset2;
        int prevc;
        int i2;
        int i3;
        if (hour != null) {
            int i4;
            int year;
            int hour3;
            int limit = s.length();
            min = -1;
            sec = -1;
            n = -1;
            tzoffset = -1;
            int prevc2 = 0;
            n2 = Integer.MIN_VALUE;
            tzoffset2 = -1;
            prevc = -1;
            while (i < limit) {
                int c = hour.charAt(i);
                i++;
                if (c > 32) {
                    if (c != 44) {
                        if (c != 40) {
                            if (48 <= c && c <= 57) {
                                n = c - 48;
                                while (i < limit) {
                                    char charAt = hour.charAt(i);
                                    c = charAt;
                                    if ('0' > charAt || c > '9') {
                                        break;
                                    }
                                    n = ((n * 10) + c) - 48;
                                    i++;
                                }
                                if (prevc2 != 43) {
                                    i4 = prevc2 == 45 ? Integer.MIN_VALUE : Integer.MIN_VALUE;
                                    if (n >= 70) {
                                        if (n2 == i4 && (c <= 32 || c == 44 || c == 47 || i >= limit)) {
                                            year = n;
                                        }
                                        i2 = prevc2;
                                        i3 = millis;
                                        break;
                                    }
                                    if (c == 58) {
                                        if (hour2 >= 0) {
                                            if (min >= 0) {
                                                i2 = prevc2;
                                                i3 = millis;
                                                break;
                                            }
                                            year = (byte) n;
                                        } else {
                                            hour2 = (byte) n;
                                            prevc2 = 0;
                                        }
                                    } else {
                                        if (c != 47) {
                                            if (i < limit && c != 44 && c > 32 && c != 45) {
                                                i2 = prevc2;
                                                i3 = millis;
                                                break;
                                            } else if (hour2 >= 0 && min < 0) {
                                                year = (byte) n;
                                            } else if (min < 0 || sec >= 0) {
                                                if (prevc >= 0) {
                                                    if (n2 == Integer.MIN_VALUE && tzoffset2 >= 0 && prevc >= 0) {
                                                        year = n;
                                                    }
                                                    i2 = prevc2;
                                                    i3 = millis;
                                                    break;
                                                }
                                                year = (byte) n;
                                            } else {
                                                sec = (byte) n;
                                                prevc2 = 0;
                                            }
                                        } else if (tzoffset2 >= 0) {
                                            if (prevc >= 0) {
                                                i2 = prevc2;
                                                i3 = millis;
                                                break;
                                            }
                                            year = (byte) n;
                                        } else {
                                            tzoffset2 = (byte) (n - 1);
                                            prevc2 = 0;
                                        }
                                        prevc = year;
                                        prevc2 = 0;
                                    }
                                    min = year;
                                    prevc2 = 0;
                                    n2 = year;
                                    prevc2 = 0;
                                }
                                if (tzoffset != 0 && tzoffset != -1) {
                                    i2 = prevc2;
                                    i3 = millis;
                                    break;
                                }
                                if (n < 24) {
                                    n *= 60;
                                    i4 = 0;
                                    if (i < limit && hour.charAt(i) == ':') {
                                        i++;
                                        while (i < limit) {
                                            char charAt2 = hour.charAt(i);
                                            char c2 = charAt2;
                                            if ('0' > charAt2 || c2 > '9') {
                                                break;
                                            }
                                            i4 = (i4 * 10) + (c2 - 48);
                                            i++;
                                        }
                                    }
                                    n += i4;
                                } else {
                                    n = (n % 100) + ((n / 100) * 60);
                                }
                                if (prevc2 == 43) {
                                    n = -n;
                                }
                                tzoffset = n;
                                prevc2 = 0;
                            } else {
                                if (c != 47 && c != 58 && c != 43) {
                                    if (c != 45) {
                                        year = i - 1;
                                        while (i < limit) {
                                            c = hour.charAt(i);
                                            if ((65 > c || c > 90) && (97 > c || c > 122)) {
                                                break;
                                            }
                                            i++;
                                        }
                                        int c3 = c;
                                        if (i > year + 1) {
                                            int k;
                                            i4 = wtb.length;
                                            while (true) {
                                                k = i4 - 1;
                                                if (k < 0) {
                                                    hour3 = hour2;
                                                    i2 = prevc2;
                                                    i3 = millis;
                                                    millis = tzoffset;
                                                    break;
                                                }
                                                i3 = millis;
                                                millis = -1;
                                                millis = tzoffset;
                                                i2 = prevc2;
                                                hour3 = hour2;
                                                if (wtb[k].regionMatches(true, 0, hour, year, i - year)) {
                                                    i4 = ttb[k];
                                                    if (i4 != 0) {
                                                        if (i4 == 1) {
                                                            if (hour3 <= 12 && hour3 >= 1) {
                                                                if (hour3 < 12) {
                                                                    hour2 = hour3 + 12;
                                                                }
                                                            }
                                                        } else if (i4 == 14) {
                                                            if (hour3 <= 12 && hour3 >= 1) {
                                                                if (hour3 == 12) {
                                                                    hour2 = 0;
                                                                }
                                                            }
                                                        } else if (i4 > 13) {
                                                            tzoffset = i4 - 10000;
                                                            hour2 = hour3;
                                                        } else if (tzoffset2 < 0) {
                                                            tzoffset2 = (byte) (i4 - 2);
                                                        }
                                                    }
                                                } else {
                                                    hour2 = hour3;
                                                    tzoffset = millis;
                                                    i4 = k;
                                                    prevc2 = i2;
                                                    millis = i3;
                                                    hour = s;
                                                }
                                            }
                                            hour2 = hour3;
                                            tzoffset = millis;
                                            if (k < 0) {
                                                break;
                                            }
                                            prevc2 = 0;
                                            c = c3;
                                            millis = i3;
                                            hour = s;
                                        } else {
                                            hour3 = hour2;
                                            i2 = prevc2;
                                            i3 = millis;
                                            millis = tzoffset;
                                        }
                                        tzoffset = millis;
                                        break;
                                    }
                                    hour3 = hour2;
                                    i2 = prevc2;
                                    i3 = millis;
                                    millis = tzoffset;
                                } else {
                                    hour3 = hour2;
                                    i2 = prevc2;
                                    i3 = millis;
                                    millis = tzoffset;
                                }
                                prevc2 = c;
                                hour2 = hour3;
                                tzoffset = millis;
                                millis = i3;
                                hour = s;
                            }
                        } else {
                            i4 = 1;
                            while (i < limit) {
                                c = hour.charAt(i);
                                i++;
                                if (c == 40) {
                                    i4++;
                                } else if (c == 41) {
                                    i4--;
                                    if (i4 <= 0) {
                                        break;
                                    }
                                } else {
                                    continue;
                                }
                            }
                        }
                    } else {
                        hour3 = hour2;
                        i2 = prevc2;
                        i3 = millis;
                        millis = tzoffset;
                    }
                } else {
                    hour3 = hour2;
                    i2 = prevc2;
                    i3 = millis;
                    millis = tzoffset;
                }
                hour2 = hour3;
                tzoffset = millis;
                prevc2 = i2;
                millis = i3;
                hour = s;
            }
            hour3 = hour2;
            i2 = prevc2;
            i3 = millis;
            millis = tzoffset;
            if (n2 == Integer.MIN_VALUE || tzoffset2 < 0 || prevc < 0) {
                hour2 = hour3;
                tzoffset = millis;
            } else {
                if (n2 < 100) {
                    synchronized (Date.class) {
                        if (defaultCenturyStart == 0) {
                            defaultCenturyStart = gcal.getCalendarDate().getYear() - 80;
                        }
                    }
                    n2 += (defaultCenturyStart / 100) * 100;
                    if (n2 < defaultCenturyStart) {
                        n2 += 100;
                    }
                }
                if (sec < 0) {
                    sec = 0;
                }
                year = sec;
                if (min < 0) {
                    min = 0;
                }
                i4 = min;
                if (hour3 < 0) {
                    hour2 = 0;
                } else {
                    hour2 = hour3;
                }
                int mday = getCalendarSystem(n2);
                sun.util.calendar.BaseCalendar.Date ldate;
                if (millis == -1) {
                    ldate = (sun.util.calendar.BaseCalendar.Date) mday.newCalendarDate(TimeZone.getDefaultRef());
                    ldate.setDate(n2, tzoffset2 + 1, prevc);
                    ldate.setTimeOfDay(hour2, i4, year, 0);
                    return mday.getTime(ldate);
                }
                ldate = (sun.util.calendar.BaseCalendar.Date) mday.newCalendarDate(null);
                ldate.setDate(n2, tzoffset2 + 1, prevc);
                ldate.setTimeOfDay(hour2, i4, year, 0);
                int min2 = i4;
                return mday.getTime(ldate) + ((long) (60000 * millis));
            }
        }
        i3 = -1;
        min = -1;
        sec = -1;
        n = -1;
        tzoffset = -1;
        i2 = 0;
        n2 = Integer.MIN_VALUE;
        tzoffset2 = -1;
        prevc = -1;
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getYear() {
        return normalize().getYear() - 1900;
    }

    @Deprecated
    public void setYear(int year) {
        getCalendarDate().setNormalizedYear(year + 1900);
    }

    @Deprecated
    public int getMonth() {
        return normalize().getMonth() - 1;
    }

    @Deprecated
    public void setMonth(int month) {
        int y = 0;
        if (month >= 12) {
            y = month / 12;
            month %= 12;
        } else if (month < 0) {
            y = CalendarUtils.floorDivide(month, 12);
            month = CalendarUtils.mod(month, 12);
        }
        sun.util.calendar.BaseCalendar.Date d = getCalendarDate();
        if (y != 0) {
            d.setNormalizedYear(d.getNormalizedYear() + y);
        }
        d.setMonth(month + 1);
    }

    @Deprecated
    public int getDate() {
        return normalize().getDayOfMonth();
    }

    @Deprecated
    public void setDate(int date) {
        getCalendarDate().setDayOfMonth(date);
    }

    @Deprecated
    public int getDay() {
        return normalize().getDayOfWeek() - 1;
    }

    @Deprecated
    public int getHours() {
        return normalize().getHours();
    }

    @Deprecated
    public void setHours(int hours) {
        getCalendarDate().setHours(hours);
    }

    @Deprecated
    public int getMinutes() {
        return normalize().getMinutes();
    }

    @Deprecated
    public void setMinutes(int minutes) {
        getCalendarDate().setMinutes(minutes);
    }

    @Deprecated
    public int getSeconds() {
        return normalize().getSeconds();
    }

    @Deprecated
    public void setSeconds(int seconds) {
        getCalendarDate().setSeconds(seconds);
    }

    public long getTime() {
        return getTimeImpl();
    }

    private final long getTimeImpl() {
        if (!(this.cdate == null || this.cdate.isNormalized())) {
            normalize();
        }
        return this.fastTime;
    }

    public void setTime(long time) {
        this.fastTime = time;
        this.cdate = null;
    }

    public boolean before(Date when) {
        return getMillisOf(this) < getMillisOf(when);
    }

    public boolean after(Date when) {
        return getMillisOf(this) > getMillisOf(when);
    }

    public boolean equals(Object obj) {
        return (obj instanceof Date) && getTime() == ((Date) obj).getTime();
    }

    static final long getMillisOf(Date date) {
        if (date.cdate == null || date.cdate.isNormalized()) {
            return date.fastTime;
        }
        return gcal.getTime((sun.util.calendar.BaseCalendar.Date) date.cdate.clone());
    }

    public int compareTo(Date anotherDate) {
        long thisTime = getMillisOf(this);
        long anotherTime = getMillisOf(anotherDate);
        if (thisTime < anotherTime) {
            return -1;
        }
        return thisTime == anotherTime ? 0 : 1;
    }

    public int hashCode() {
        long ht = getTime();
        return ((int) ht) ^ ((int) (ht >> 32));
    }

    public String toString() {
        sun.util.calendar.BaseCalendar.Date date = normalize();
        StringBuilder sb = new StringBuilder(28);
        int index = date.getDayOfWeek();
        if (index == 1) {
            index = 8;
        }
        convertToAbbr(sb, wtb[index]).append(' ');
        convertToAbbr(sb, wtb[((date.getMonth() - 1) + 2) + 7]).append(' ');
        CalendarUtils.sprintf0d(sb, date.getDayOfMonth(), 2).append(' ');
        CalendarUtils.sprintf0d(sb, date.getHours(), 2).append(':');
        CalendarUtils.sprintf0d(sb, date.getMinutes(), 2).append(':');
        CalendarUtils.sprintf0d(sb, date.getSeconds(), 2).append(' ');
        TimeZone zi = date.getZone();
        if (zi != null) {
            sb.append(zi.getDisplayName(date.isDaylightTime(), 0, Locale.US));
        } else {
            sb.append("GMT");
        }
        sb.append(' ');
        sb.append(date.getYear());
        return sb.toString();
    }

    private static final StringBuilder convertToAbbr(StringBuilder sb, String name) {
        sb.append(Character.toUpperCase(name.charAt(0)));
        sb.append(name.charAt(1));
        sb.append(name.charAt(2));
        return sb;
    }

    @Deprecated
    public String toLocaleString() {
        return DateFormat.getDateTimeInstance().format(this);
    }

    @Deprecated
    public String toGMTString() {
        sun.util.calendar.BaseCalendar.Date date = (sun.util.calendar.BaseCalendar.Date) getCalendarSystem(getTime()).getCalendarDate(getTime(), (TimeZone) null);
        StringBuilder sb = new StringBuilder(32);
        CalendarUtils.sprintf0d(sb, date.getDayOfMonth(), 1).append(' ');
        convertToAbbr(sb, wtb[((date.getMonth() - 1) + 2) + 7]).append(' ');
        sb.append(date.getYear());
        sb.append(' ');
        CalendarUtils.sprintf0d(sb, date.getHours(), 2).append(':');
        CalendarUtils.sprintf0d(sb, date.getMinutes(), 2).append(':');
        CalendarUtils.sprintf0d(sb, date.getSeconds(), 2);
        sb.append(" GMT");
        return sb.toString();
    }

    @Deprecated
    public int getTimezoneOffset() {
        int zoneOffset;
        if (this.cdate == null) {
            GregorianCalendar cal = new GregorianCalendar(this.fastTime);
            zoneOffset = cal.get(15) + cal.get(16);
        } else {
            normalize();
            zoneOffset = this.cdate.getZoneOffset();
        }
        return (-zoneOffset) / 60000;
    }

    private final sun.util.calendar.BaseCalendar.Date getCalendarDate() {
        if (this.cdate == null) {
            this.cdate = (sun.util.calendar.BaseCalendar.Date) getCalendarSystem(this.fastTime).getCalendarDate(this.fastTime, TimeZone.getDefaultRef());
        }
        return this.cdate;
    }

    private final sun.util.calendar.BaseCalendar.Date normalize() {
        if (this.cdate == null) {
            this.cdate = (sun.util.calendar.BaseCalendar.Date) getCalendarSystem(this.fastTime).getCalendarDate(this.fastTime, TimeZone.getDefaultRef());
            return this.cdate;
        }
        if (!this.cdate.isNormalized()) {
            this.cdate = normalize(this.cdate);
        }
        TimeZone tz = TimeZone.getDefaultRef();
        if (tz != this.cdate.getZone()) {
            this.cdate.setZone(tz);
            getCalendarSystem(this.cdate).getCalendarDate(this.fastTime, this.cdate);
        }
        return this.cdate;
    }

    private final sun.util.calendar.BaseCalendar.Date normalize(sun.util.calendar.BaseCalendar.Date date) {
        int y = date.getNormalizedYear();
        int m = date.getMonth();
        int d = date.getDayOfMonth();
        int hh = date.getHours();
        int mm = date.getMinutes();
        int ss = date.getSeconds();
        int ms = date.getMillis();
        TimeZone tz = date.getZone();
        if (y == 1582 || y > 280000000 || y < -280000000) {
            if (tz == null) {
                tz = TimeZone.getTimeZone("GMT");
            }
            TimeZone tz2 = tz;
            GregorianCalendar gc = new GregorianCalendar(tz2);
            gc.clear();
            gc.set(14, ms);
            int i = y;
            GregorianCalendar gc2 = gc;
            gc.set(i, m - 1, d, hh, mm, ss);
            this.fastTime = gc2.getTimeInMillis();
            return (sun.util.calendar.BaseCalendar.Date) getCalendarSystem(this.fastTime).getCalendarDate(this.fastTime, tz2);
        }
        sun.util.calendar.BaseCalendar.Date date2;
        BaseCalendar cal = getCalendarSystem(y);
        if (cal != getCalendarSystem(date)) {
            date2 = (sun.util.calendar.BaseCalendar.Date) cal.newCalendarDate(tz);
            date2.setNormalizedDate(y, m, d).setTimeOfDay(hh, mm, ss, ms);
        } else {
            date2 = date;
        }
        this.fastTime = cal.getTime(date2);
        BaseCalendar ncal = getCalendarSystem(this.fastTime);
        if (ncal != cal) {
            date2 = (sun.util.calendar.BaseCalendar.Date) ncal.newCalendarDate(tz);
            date2.setNormalizedDate(y, m, d).setTimeOfDay(hh, mm, ss, ms);
            this.fastTime = ncal.getTime(date2);
        }
        return date2;
    }

    private static final BaseCalendar getCalendarSystem(int year) {
        if (year >= 1582) {
            return gcal;
        }
        return getJulianCalendar();
    }

    private static final BaseCalendar getCalendarSystem(long utc) {
        if (utc >= 0 || utc >= -12219292800000L - ((long) TimeZone.getDefaultRef().getOffset(utc))) {
            return gcal;
        }
        return getJulianCalendar();
    }

    private static final BaseCalendar getCalendarSystem(sun.util.calendar.BaseCalendar.Date cdate) {
        if (jcal == null) {
            return gcal;
        }
        if (cdate.getEra() != null) {
            return jcal;
        }
        return gcal;
    }

    private static final synchronized BaseCalendar getJulianCalendar() {
        BaseCalendar baseCalendar;
        synchronized (Date.class) {
            if (jcal == null) {
                jcal = (BaseCalendar) CalendarSystem.forName("julian");
            }
            baseCalendar = jcal;
        }
        return baseCalendar;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeLong(getTimeImpl());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.fastTime = s.readLong();
    }

    public static Date from(Instant instant) {
        try {
            return new Date(instant.toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public Instant toInstant() {
        return Instant.ofEpochMilli(getTime());
    }
}
