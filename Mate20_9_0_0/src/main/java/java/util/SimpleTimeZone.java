package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.BaseCalendar.Date;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Gregorian;

public class SimpleTimeZone extends TimeZone {
    private static final int DOM_MODE = 1;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_LE_DOM_MODE = 4;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;
    public static final int WALL_TIME = 0;
    static final int currentSerialVersion = 2;
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();
    private static final int millisPerDay = 86400000;
    private static final int millisPerHour = 3600000;
    static final long serialVersionUID = -403250971215465050L;
    private static final byte[] staticLeapMonthLength = new byte[]{(byte) 31, Character.INITIAL_QUOTE_PUNCTUATION, (byte) 31, (byte) 30, (byte) 31, (byte) 30, (byte) 31, (byte) 31, (byte) 30, (byte) 31, (byte) 30, (byte) 31};
    private static final byte[] staticMonthLength = new byte[]{(byte) 31, (byte) 28, (byte) 31, (byte) 30, (byte) 31, (byte) 30, (byte) 31, (byte) 31, (byte) 30, (byte) 31, (byte) 30, (byte) 31};
    private transient long cacheEnd;
    private transient long cacheStart;
    private transient long cacheYear;
    private int dstSavings;
    private int endDay;
    private int endDayOfWeek;
    private int endMode;
    private int endMonth;
    private int endTime;
    private int endTimeMode;
    private final byte[] monthLength;
    private int rawOffset;
    private int serialVersionOnStream;
    private int startDay;
    private int startDayOfWeek;
    private int startMode;
    private int startMonth;
    private int startTime;
    private int startTimeMode;
    private int startYear;
    private boolean useDaylight;

    public SimpleTimeZone(int rawOffset, String ID) {
        this.useDaylight = false;
        this.monthLength = staticMonthLength;
        this.serialVersionOnStream = 2;
        this.rawOffset = rawOffset;
        setID(ID);
        this.dstSavings = millisPerHour;
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay, int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this(rawOffset, ID, startMonth, startDay, startDayOfWeek, startTime, 0, endMonth, endDay, endDayOfWeek, endTime, 0, millisPerHour);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay, int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek, int endTime, int dstSavings) {
        this(rawOffset, ID, startMonth, startDay, startDayOfWeek, startTime, 0, endMonth, endDay, endDayOfWeek, endTime, 0, dstSavings);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay, int startDayOfWeek, int startTime, int startTimeMode, int endMonth, int endDay, int endDayOfWeek, int endTime, int endTimeMode, int dstSavings) {
        int i = dstSavings;
        this.useDaylight = false;
        this.monthLength = staticMonthLength;
        this.serialVersionOnStream = 2;
        setID(ID);
        this.rawOffset = rawOffset;
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        this.startTimeMode = startTimeMode;
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = endTimeMode;
        this.dstSavings = i;
        decodeRules();
        if (i <= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal daylight saving value: ");
            stringBuilder.append(i);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void setStartYear(int year) {
        this.startYear = year;
        invalidateCache();
    }

    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime) {
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        this.startTimeMode = 0;
        decodeStartRule();
        invalidateCache();
    }

    public void setStartRule(int startMonth, int startDay, int startTime) {
        setStartRule(startMonth, startDay, 0, startTime);
    }

    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime, boolean after) {
        if (after) {
            setStartRule(startMonth, startDay, -startDayOfWeek, startTime);
        } else {
            setStartRule(startMonth, -startDay, -startDayOfWeek, startTime);
        }
    }

    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = 0;
        decodeEndRule();
        invalidateCache();
    }

    public void setEndRule(int endMonth, int endDay, int endTime) {
        setEndRule(endMonth, endDay, 0, endTime);
    }

    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime, boolean after) {
        if (after) {
            setEndRule(endMonth, endDay, -endDayOfWeek, endTime);
        } else {
            setEndRule(endMonth, -endDay, -endDayOfWeek, endTime);
        }
    }

    public int getOffset(long date) {
        return getOffsets(date, null);
    }

    int getOffsets(long date, int[] offsets) {
        int offset = this.rawOffset;
        if (this.useDaylight) {
            synchronized (this) {
                if (this.cacheStart == 0 || date < this.cacheStart || date >= this.cacheEnd) {
                    BaseCalendar cal = date >= -12219292800000L ? gcal : (BaseCalendar) CalendarSystem.forName("julian");
                    Date cdate = (Date) cal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    cal.getCalendarDate(((long) this.rawOffset) + date, (CalendarDate) cdate);
                    int year = cdate.getNormalizedYear();
                    if (year >= this.startYear) {
                        cdate.setTimeOfDay(0, 0, 0, 0);
                        offset = getOffset(cal, cdate, year, date);
                    }
                } else {
                    offset += this.dstSavings;
                }
            }
        }
        if (offsets != null) {
            offsets[0] = this.rawOffset;
            offsets[1] = offset - this.rawOffset;
        }
        return offset;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        int i = era;
        int i2 = day;
        int i3 = dayOfWeek;
        int i4 = millis;
        if (i == 1 || i == 0) {
            int y;
            Date cdate;
            BaseCalendar cal;
            long time;
            int y2 = year;
            if (i == 0) {
                y2 = 1 - y2;
            }
            if (y2 >= 292278994) {
                y = 2800 + (y2 % 2800);
            } else {
                if (y2 <= -292269054) {
                    y2 = (int) CalendarUtils.mod((long) y2, 28);
                }
                y = y2;
            }
            int m = month + 1;
            BaseCalendar cal2 = gcal;
            Date cdate2 = (Date) cal2.newCalendarDate(TimeZone.NO_TIMEZONE);
            cdate2.setDate(y, m, i2);
            long time2 = cal2.getTime(cdate2) + ((long) (i4 - this.rawOffset));
            if (time2 < -12219292800000L) {
                cal2 = (BaseCalendar) CalendarSystem.forName("julian");
                cdate2 = (Date) cal2.newCalendarDate(TimeZone.NO_TIMEZONE);
                cdate2.setNormalizedDate(y, m, i2);
                cdate = cdate2;
                cal = cal2;
                time = (cal2.getTime(cdate2) + ((long) i4)) - ((long) this.rawOffset);
            } else {
                cal = cal2;
                cdate = cdate2;
                time = time2;
            }
            if (cdate.getNormalizedYear() != y || cdate.getMonth() != m || cdate.getDayOfMonth() != i2 || i3 < 1 || i3 > 7 || i4 < 0 || i4 >= millisPerDay) {
                throw new IllegalArgumentException();
            } else if (this.useDaylight && year >= this.startYear && i == 1) {
                return getOffset(cal, cdate, y, time);
            } else {
                return this.rawOffset;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal era ");
        stringBuilder.append(i);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:17:0x0028, code skipped:
            r0 = getStart(r11, r12, r13);
            r2 = getEnd(r11, r12, r13);
            r4 = r10.rawOffset;
     */
    /* JADX WARNING: Missing block: B:18:0x0034, code skipped:
            if (r0 > r2) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:20:0x0038, code skipped:
            if (r14 < r0) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:22:0x003c, code skipped:
            if (r14 >= r2) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:23:0x003e, code skipped:
            r4 = r4 + r10.dstSavings;
     */
    /* JADX WARNING: Missing block: B:24:0x0041, code skipped:
            r5 = r4;
     */
    /* JADX WARNING: Missing block: B:25:0x0042, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:28:?, code skipped:
            r10.cacheYear = (long) r13;
            r10.cacheStart = r0;
            r10.cacheEnd = r2;
     */
    /* JADX WARNING: Missing block: B:29:0x004a, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:35:0x0051, code skipped:
            if (r14 >= r2) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:36:0x0053, code skipped:
            r0 = getStart(r11, r12, r13 - 1);
     */
    /* JADX WARNING: Missing block: B:37:0x005b, code skipped:
            if (r14 < r0) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:38:0x005d, code skipped:
            r4 = r4 + r10.dstSavings;
     */
    /* JADX WARNING: Missing block: B:39:0x0060, code skipped:
            r5 = r4;
     */
    /* JADX WARNING: Missing block: B:41:0x0064, code skipped:
            if (r14 < r0) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:42:0x0066, code skipped:
            r2 = getEnd(r11, r12, r13 + 1);
     */
    /* JADX WARNING: Missing block: B:43:0x006e, code skipped:
            if (r14 >= r2) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:44:0x0070, code skipped:
            r4 = r4 + r10.dstSavings;
     */
    /* JADX WARNING: Missing block: B:46:0x0076, code skipped:
            if (r0 > r2) goto L_0x008a;
     */
    /* JADX WARNING: Missing block: B:47:0x0078, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            r10.cacheYear = ((long) r10.startYear) - 1;
            r10.cacheStart = r0;
            r10.cacheEnd = r2;
     */
    /* JADX WARNING: Missing block: B:50:0x0085, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:55:0x008a, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getOffset(BaseCalendar cal, Date cdate, int year, long time) {
        synchronized (this) {
            if (this.cacheStart != 0) {
                int i;
                if (time >= this.cacheStart && time < this.cacheEnd) {
                    i = this.rawOffset + this.dstSavings;
                    return i;
                } else if (((long) year) == this.cacheYear) {
                    i = this.rawOffset;
                    return i;
                }
            }
        }
    }

    private long getStart(BaseCalendar cal, Date cdate, int year) {
        int time = this.startTime;
        if (this.startTimeMode != 2) {
            time -= this.rawOffset;
        }
        return getTransition(cal, cdate, this.startMode, year, this.startMonth, this.startDay, this.startDayOfWeek, time);
    }

    private long getEnd(BaseCalendar cal, Date cdate, int year) {
        int time = this.endTime;
        if (this.endTimeMode != 2) {
            time -= this.rawOffset;
        }
        if (this.endTimeMode == 0) {
            time -= this.dstSavings;
        }
        return getTransition(cal, cdate, this.endMode, year, this.endMonth, this.endDay, this.endDayOfWeek, time);
    }

    private long getTransition(BaseCalendar cal, Date cdate, int mode, int year, int month, int dayOfMonth, int dayOfWeek, int timeOfDay) {
        CalendarDate cdate2;
        cdate2.setNormalizedYear(year);
        cdate2.setMonth(month + 1);
        switch (mode) {
            case 1:
                cdate2.setDayOfMonth(dayOfMonth);
                break;
            case 2:
                cdate2.setDayOfMonth(1);
                if (dayOfMonth < 0) {
                    cdate2.setDayOfMonth(cal.getMonthLength(cdate2));
                }
                cdate2 = (Date) cal.getNthDayOfWeek(dayOfMonth, dayOfWeek, cdate2);
                break;
            case 3:
                cdate2.setDayOfMonth(dayOfMonth);
                cdate2 = (Date) cal.getNthDayOfWeek(1, dayOfWeek, cdate2);
                break;
            case 4:
                cdate2.setDayOfMonth(dayOfMonth);
                cdate2 = (Date) cal.getNthDayOfWeek(-1, dayOfWeek, cdate2);
                break;
        }
        return cal.getTime(cdate2) + ((long) timeOfDay);
    }

    public int getRawOffset() {
        return this.rawOffset;
    }

    public void setRawOffset(int offsetMillis) {
        this.rawOffset = offsetMillis;
    }

    public void setDSTSavings(int millisSavedDuringDST) {
        if (millisSavedDuringDST > 0) {
            this.dstSavings = millisSavedDuringDST;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal daylight saving value: ");
        stringBuilder.append(millisSavedDuringDST);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getDSTSavings() {
        return this.useDaylight ? this.dstSavings : 0;
    }

    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    public boolean observesDaylightTime() {
        return useDaylightTime();
    }

    public boolean inDaylightTime(Date date) {
        return getOffset(date.getTime()) != this.rawOffset;
    }

    public Object clone() {
        return super.clone();
    }

    public synchronized int hashCode() {
        return (((((((this.startMonth ^ this.startDay) ^ this.startDayOfWeek) ^ this.startTime) ^ this.endMonth) ^ this.endDay) ^ this.endDayOfWeek) ^ this.endTime) ^ this.rawOffset;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) obj;
        if (!(getID().equals(that.getID()) && hasSameRules(that))) {
            z = false;
        }
        return z;
    }

    public boolean hasSameRules(TimeZone other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) other;
        if (!(this.rawOffset == that.rawOffset && this.useDaylight == that.useDaylight && (!this.useDaylight || (this.dstSavings == that.dstSavings && this.startMode == that.startMode && this.startMonth == that.startMonth && this.startDay == that.startDay && this.startDayOfWeek == that.startDayOfWeek && this.startTime == that.startTime && this.startTimeMode == that.startTimeMode && this.endMode == that.endMode && this.endMonth == that.endMonth && this.endDay == that.endDay && this.endDayOfWeek == that.endDayOfWeek && this.endTime == that.endTime && this.endTimeMode == that.endTimeMode && this.startYear == that.startYear)))) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[id=");
        stringBuilder.append(getID());
        stringBuilder.append(",offset=");
        stringBuilder.append(this.rawOffset);
        stringBuilder.append(",dstSavings=");
        stringBuilder.append(this.dstSavings);
        stringBuilder.append(",useDaylight=");
        stringBuilder.append(this.useDaylight);
        stringBuilder.append(",startYear=");
        stringBuilder.append(this.startYear);
        stringBuilder.append(",startMode=");
        stringBuilder.append(this.startMode);
        stringBuilder.append(",startMonth=");
        stringBuilder.append(this.startMonth);
        stringBuilder.append(",startDay=");
        stringBuilder.append(this.startDay);
        stringBuilder.append(",startDayOfWeek=");
        stringBuilder.append(this.startDayOfWeek);
        stringBuilder.append(",startTime=");
        stringBuilder.append(this.startTime);
        stringBuilder.append(",startTimeMode=");
        stringBuilder.append(this.startTimeMode);
        stringBuilder.append(",endMode=");
        stringBuilder.append(this.endMode);
        stringBuilder.append(",endMonth=");
        stringBuilder.append(this.endMonth);
        stringBuilder.append(",endDay=");
        stringBuilder.append(this.endDay);
        stringBuilder.append(",endDayOfWeek=");
        stringBuilder.append(this.endDayOfWeek);
        stringBuilder.append(",endTime=");
        stringBuilder.append(this.endTime);
        stringBuilder.append(",endTimeMode=");
        stringBuilder.append(this.endTimeMode);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private synchronized void invalidateCache() {
        this.cacheYear = (long) (this.startYear - 1);
        this.cacheEnd = 0;
        this.cacheStart = 0;
    }

    private void decodeRules() {
        decodeStartRule();
        decodeEndRule();
    }

    private void decodeStartRule() {
        boolean z = (this.startDay == 0 || this.endDay == 0) ? false : true;
        this.useDaylight = z;
        if (this.startDay == 0) {
            return;
        }
        StringBuilder stringBuilder;
        if (this.startMonth < 0 || this.startMonth > 11) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal start month ");
            stringBuilder.append(this.startMonth);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.startTime < 0 || this.startTime > millisPerDay) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal start time ");
            stringBuilder.append(this.startTime);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            if (this.startDayOfWeek == 0) {
                this.startMode = 1;
            } else {
                if (this.startDayOfWeek > 0) {
                    this.startMode = 2;
                } else {
                    this.startDayOfWeek = -this.startDayOfWeek;
                    if (this.startDay > 0) {
                        this.startMode = 3;
                    } else {
                        this.startDay = -this.startDay;
                        this.startMode = 4;
                    }
                }
                if (this.startDayOfWeek > 7) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal start day of week ");
                    stringBuilder.append(this.startDayOfWeek);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (this.startMode == 2) {
                if (this.startDay < -5 || this.startDay > 5) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal start day of week in month ");
                    stringBuilder.append(this.startDay);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else if (this.startDay < 1 || this.startDay > staticMonthLength[this.startMonth]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal start day ");
                stringBuilder.append(this.startDay);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private void decodeEndRule() {
        boolean z = (this.startDay == 0 || this.endDay == 0) ? false : true;
        this.useDaylight = z;
        if (this.endDay == 0) {
            return;
        }
        StringBuilder stringBuilder;
        if (this.endMonth < 0 || this.endMonth > 11) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal end month ");
            stringBuilder.append(this.endMonth);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.endTime < 0 || this.endTime > millisPerDay) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal end time ");
            stringBuilder.append(this.endTime);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            if (this.endDayOfWeek == 0) {
                this.endMode = 1;
            } else {
                if (this.endDayOfWeek > 0) {
                    this.endMode = 2;
                } else {
                    this.endDayOfWeek = -this.endDayOfWeek;
                    if (this.endDay > 0) {
                        this.endMode = 3;
                    } else {
                        this.endDay = -this.endDay;
                        this.endMode = 4;
                    }
                }
                if (this.endDayOfWeek > 7) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal end day of week ");
                    stringBuilder.append(this.endDayOfWeek);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (this.endMode == 2) {
                if (this.endDay < -5 || this.endDay > 5) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal end day of week in month ");
                    stringBuilder.append(this.endDay);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else if (this.endDay < 1 || this.endDay > staticMonthLength[this.endMonth]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal end day ");
                stringBuilder.append(this.endDay);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private void makeRulesCompatible() {
        int i = this.startMode;
        if (i != 1) {
            switch (i) {
                case 3:
                    if (this.startDay != 1) {
                        this.startDay = (this.startDay / 7) + 1;
                        break;
                    }
                    break;
                case 4:
                    if (this.startDay < 30) {
                        this.startDay = (this.startDay / 7) + 1;
                        break;
                    } else {
                        this.startDay = -1;
                        break;
                    }
            }
        }
        this.startDay = (this.startDay / 7) + 1;
        this.startDayOfWeek = 1;
        i = this.endMode;
        if (i != 1) {
            switch (i) {
                case 3:
                    if (this.endDay != 1) {
                        this.endDay = (this.endDay / 7) + 1;
                        break;
                    }
                    break;
                case 4:
                    if (this.endDay < 30) {
                        this.endDay = (this.endDay / 7) + 1;
                        break;
                    } else {
                        this.endDay = -1;
                        break;
                    }
            }
        }
        this.endDay = (this.endDay / 7) + 1;
        this.endDayOfWeek = 1;
        if (this.startTimeMode == 2) {
            this.startTime += this.rawOffset;
        }
        while (this.startTime < 0) {
            this.startTime += millisPerDay;
            this.startDayOfWeek = ((this.startDayOfWeek + 5) % 7) + 1;
        }
        while (this.startTime >= millisPerDay) {
            this.startTime -= millisPerDay;
            this.startDayOfWeek = (this.startDayOfWeek % 7) + 1;
        }
        switch (this.endTimeMode) {
            case 1:
                this.endTime += this.dstSavings;
                break;
            case 2:
                this.endTime += this.rawOffset + this.dstSavings;
                break;
        }
        while (this.endTime < 0) {
            this.endTime += millisPerDay;
            this.endDayOfWeek = ((this.endDayOfWeek + 5) % 7) + 1;
        }
        while (this.endTime >= millisPerDay) {
            this.endTime -= millisPerDay;
            this.endDayOfWeek = (this.endDayOfWeek % 7) + 1;
        }
    }

    private byte[] packRules() {
        return new byte[]{(byte) this.startDay, (byte) this.startDayOfWeek, (byte) this.endDay, (byte) this.endDayOfWeek, (byte) this.startTimeMode, (byte) this.endTimeMode};
    }

    private void unpackRules(byte[] rules) {
        this.startDay = rules[0];
        this.startDayOfWeek = rules[1];
        this.endDay = rules[2];
        this.endDayOfWeek = rules[3];
        if (rules.length >= 6) {
            this.startTimeMode = rules[4];
            this.endTimeMode = rules[5];
        }
    }

    private int[] packTimes() {
        return new int[]{this.startTime, this.endTime};
    }

    private void unpackTimes(int[] times) {
        this.startTime = times[0];
        this.endTime = times[1];
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        byte[] rules = packRules();
        int[] times = packTimes();
        makeRulesCompatible();
        stream.defaultWriteObject();
        stream.writeInt(rules.length);
        stream.write(rules);
        stream.writeObject(times);
        unpackRules(rules);
        unpackTimes(times);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            if (this.startDayOfWeek == 0) {
                this.startDayOfWeek = 1;
            }
            if (this.endDayOfWeek == 0) {
                this.endDayOfWeek = 1;
            }
            this.endMode = 2;
            this.startMode = 2;
            this.dstSavings = millisPerHour;
        } else {
            byte[] rules = new byte[stream.readInt()];
            stream.readFully(rules);
            unpackRules(rules);
        }
        if (this.serialVersionOnStream >= 2) {
            unpackTimes((int[]) stream.readObject());
        }
        this.serialVersionOnStream = 2;
    }
}
