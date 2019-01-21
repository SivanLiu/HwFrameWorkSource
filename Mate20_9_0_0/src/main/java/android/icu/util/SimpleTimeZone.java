package android.icu.util;

import android.icu.impl.Grego;
import android.icu.lang.UCharacterEnums.ECharacterCategory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;

public class SimpleTimeZone extends BasicTimeZone {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int DOM_MODE = 1;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_LE_DOM_MODE = 4;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;
    public static final int WALL_TIME = 0;
    private static final long serialVersionUID = -7034676239311322769L;
    private static final byte[] staticMonthLength = new byte[]{(byte) 31, (byte) 29, (byte) 31, ECharacterCategory.CHAR_CATEGORY_COUNT, (byte) 31, ECharacterCategory.CHAR_CATEGORY_COUNT, (byte) 31, (byte) 31, ECharacterCategory.CHAR_CATEGORY_COUNT, (byte) 31, ECharacterCategory.CHAR_CATEGORY_COUNT, (byte) 31};
    private int dst = 3600000;
    private transient AnnualTimeZoneRule dstRule;
    private int endDay;
    private int endDayOfWeek;
    private int endMode;
    private int endMonth;
    private int endTime;
    private int endTimeMode;
    private transient TimeZoneTransition firstTransition;
    private transient InitialTimeZoneRule initialRule;
    private volatile transient boolean isFrozen = false;
    private int raw;
    private int startDay;
    private int startDayOfWeek;
    private int startMode;
    private int startMonth;
    private int startTime;
    private int startTimeMode;
    private int startYear;
    private transient AnnualTimeZoneRule stdRule;
    private transient boolean transitionRulesInitialized;
    private boolean useDaylight;
    private STZInfo xinfo = null;

    public SimpleTimeZone(int rawOffset, String ID) {
        super(ID);
        construct(rawOffset, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3600000);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay, int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek, int endTime) {
        super(ID);
        construct(rawOffset, startMonth, startDay, startDayOfWeek, startTime, 0, endMonth, endDay, endDayOfWeek, endTime, 0, 3600000);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay, int startDayOfWeek, int startTime, int startTimeMode, int endMonth, int endDay, int endDayOfWeek, int endTime, int endTimeMode, int dstSavings) {
        super(ID);
        construct(rawOffset, startMonth, startDay, startDayOfWeek, startTime, startTimeMode, endMonth, endDay, endDayOfWeek, endTime, endTimeMode, dstSavings);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay, int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek, int endTime, int dstSavings) {
        super(ID);
        construct(rawOffset, startMonth, startDay, startDayOfWeek, startTime, 0, endMonth, endDay, endDayOfWeek, endTime, 0, dstSavings);
    }

    public void setID(String ID) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        super.setID(ID);
        this.transitionRulesInitialized = false;
    }

    public void setRawOffset(int offsetMillis) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        this.raw = offsetMillis;
        this.transitionRulesInitialized = false;
    }

    public int getRawOffset() {
        return this.raw;
    }

    public void setStartYear(int year) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().sy = year;
        this.startYear = year;
        this.transitionRulesInitialized = false;
    }

    public void setStartRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setStart(month, dayOfWeekInMonth, dayOfWeek, time, -1, false);
        setStartRule(month, dayOfWeekInMonth, dayOfWeek, time, 0);
    }

    private void setStartRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time, int mode) {
        this.startMonth = month;
        this.startDay = dayOfWeekInMonth;
        this.startDayOfWeek = dayOfWeek;
        this.startTime = time;
        this.startTimeMode = mode;
        decodeStartRule();
        this.transitionRulesInitialized = false;
    }

    public void setStartRule(int month, int dayOfMonth, int time) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setStart(month, -1, -1, time, dayOfMonth, false);
        setStartRule(month, dayOfMonth, 0, time, 0);
    }

    public void setStartRule(int month, int dayOfMonth, int dayOfWeek, int time, boolean after) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setStart(month, -1, dayOfWeek, time, dayOfMonth, after);
        setStartRule(month, after ? dayOfMonth : -dayOfMonth, -dayOfWeek, time, 0);
    }

    public void setEndRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setEnd(month, dayOfWeekInMonth, dayOfWeek, time, -1, false);
        setEndRule(month, dayOfWeekInMonth, dayOfWeek, time, 0);
    }

    public void setEndRule(int month, int dayOfMonth, int time) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setEnd(month, -1, -1, time, dayOfMonth, false);
        setEndRule(month, dayOfMonth, 0, time);
    }

    public void setEndRule(int month, int dayOfMonth, int dayOfWeek, int time, boolean after) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        int i = dayOfMonth;
        getSTZInfo().setEnd(month, -1, dayOfWeek, time, i, after);
        setEndRule(month, i, dayOfWeek, time, 0, after);
    }

    private void setEndRule(int month, int dayOfMonth, int dayOfWeek, int time, int mode, boolean after) {
        setEndRule(month, after ? dayOfMonth : -dayOfMonth, -dayOfWeek, time, mode);
    }

    private void setEndRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time, int mode) {
        this.endMonth = month;
        this.endDay = dayOfWeekInMonth;
        this.endDayOfWeek = dayOfWeek;
        this.endTime = time;
        this.endTimeMode = mode;
        decodeEndRule();
        this.transitionRulesInitialized = false;
    }

    public void setDSTSavings(int millisSavedDuringDST) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        } else if (millisSavedDuringDST > 0) {
            this.dst = millisSavedDuringDST;
            this.transitionRulesInitialized = false;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getDSTSavings() {
        return this.dst;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.xinfo != null) {
            this.xinfo.applyTo(this);
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SimpleTimeZone: ");
        stringBuilder.append(getID());
        return stringBuilder.toString();
    }

    private STZInfo getSTZInfo() {
        if (this.xinfo == null) {
            this.xinfo = new STZInfo();
        }
        return this.xinfo;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        if (month < 0 || month > 11) {
            throw new IllegalArgumentException();
        }
        return getOffset(era, year, month, day, dayOfWeek, millis, Grego.monthLength(year, month));
    }

    @Deprecated
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis, int monthLength) {
        int i = month;
        if (i < 0 || i > 11) {
            throw new IllegalArgumentException();
        }
        return getOffset(era, year, i, day, dayOfWeek, millis, Grego.monthLength(year, i), Grego.previousMonthLength(year, i));
    }

    private int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis, int monthLength, int prevMonthLength) {
        int i = era;
        int i2 = month;
        int i3 = day;
        int i4 = dayOfWeek;
        int i5 = millis;
        int i6 = monthLength;
        int i7 = prevMonthLength;
        if ((i == 1 || i == 0) && i2 >= 0 && i2 <= 11 && i3 >= 1 && i3 <= i6 && i4 >= 1 && i4 <= 7 && i5 >= 0 && i5 < 86400000 && i6 >= 28 && i6 <= 31 && i7 >= 28 && i7 <= 31) {
            int result;
            int result2 = this.raw;
            if (!this.useDaylight || year < this.startYear) {
                result = result2;
            } else if (i != 1) {
                result = result2;
            } else {
                boolean southern = this.startMonth > this.endMonth;
                int i8 = this.startTimeMode == 2 ? -this.raw : 0;
                i2 = 2;
                boolean southern2 = southern;
                result = result2;
                boolean z = true;
                i = compareToRule(i2, i6, i7, i3, i4, i5, i8, this.startMode, this.startMonth, this.startDayOfWeek, this.startDay, this.startTime);
                i8 = 0;
                if (southern2 != (i >= 0 ? z : false)) {
                    int i9;
                    int i10;
                    if (this.endTimeMode == 0) {
                        i9 = this.dst;
                    } else if (this.endTimeMode == 2) {
                        i9 = -this.raw;
                    } else {
                        i10 = 0;
                        i8 = compareToRule(month, monthLength, prevMonthLength, day, dayOfWeek, millis, i10, this.endMode, this.endMonth, this.endDayOfWeek, this.endDay, this.endTime);
                    }
                    i10 = i9;
                    i8 = compareToRule(month, monthLength, prevMonthLength, day, dayOfWeek, millis, i10, this.endMode, this.endMonth, this.endDayOfWeek, this.endDay, this.endTime);
                }
                if ((southern2 || i < 0 || endCompare >= 0) && (!southern2 || (i < 0 && endCompare >= 0))) {
                    result2 = result;
                } else {
                    result2 = result + this.dst;
                }
                return result2;
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        long date2 = date;
        offsets[0] = getRawOffset();
        int[] fields = new int[6];
        Grego.timeToFields(date2, fields);
        int i = 3;
        offsets[1] = getOffset(1, fields[0], fields[1], fields[2], fields[3], fields[5]) - offsets[0];
        boolean recalc = false;
        if (offsets[1] > 0) {
            if ((nonExistingTimeOpt & 3) == 1 || !((nonExistingTimeOpt & 3) == i || (nonExistingTimeOpt & 12) == 12)) {
                date2 -= (long) getDSTSavings();
                recalc = true;
            }
        } else if ((duplicatedTimeOpt & 3) == i || ((duplicatedTimeOpt & 3) != 1 && (duplicatedTimeOpt & 12) == 4)) {
            date2 -= (long) getDSTSavings();
            recalc = true;
        }
        if (recalc) {
            Grego.timeToFields(date2, fields);
            offsets[1] = getOffset(1, fields[0], fields[1], fields[2], fields[i], fields[5]) - offsets[0];
        }
    }

    private int compareToRule(int month, int monthLen, int prevMonthLen, int dayOfMonth, int dayOfWeek, int millis, int millisDelta, int ruleMode, int ruleMonth, int ruleDayOfWeek, int ruleDay, int ruleMillis) {
        int i = monthLen;
        int i2 = ruleMonth;
        int i3 = ruleMillis;
        int millis2 = millis + millisDelta;
        int month2 = month;
        int dayOfMonth2 = dayOfMonth;
        int dayOfWeek2 = dayOfWeek;
        while (millis2 >= Grego.MILLIS_PER_DAY) {
            millis2 -= Grego.MILLIS_PER_DAY;
            dayOfMonth2++;
            dayOfWeek2 = 1 + (dayOfWeek2 % 7);
            if (dayOfMonth2 > i) {
                dayOfMonth2 = 1;
                month2++;
            }
        }
        while (millis2 < 0) {
            dayOfMonth2--;
            dayOfWeek2 = 1 + ((dayOfWeek2 + 5) % 7);
            if (dayOfMonth2 < 1) {
                dayOfMonth2 = prevMonthLen;
                month2--;
            }
            millis2 += Grego.MILLIS_PER_DAY;
        }
        if (month2 < i2) {
            return -1;
        }
        if (month2 > i2) {
            return 1;
        }
        int ruleDayOfMonth = 0;
        int ruleDay2 = ruleDay;
        if (ruleDay2 > i) {
            ruleDay2 = i;
        }
        switch (ruleMode) {
            case 1:
                ruleDayOfMonth = ruleDay2;
                break;
            case 2:
                if (ruleDay2 <= 0) {
                    ruleDayOfMonth = (((ruleDay2 + 1) * 7) + i) - (((((dayOfWeek2 + i) - dayOfMonth2) + 7) - ruleDayOfWeek) % 7);
                    break;
                }
                ruleDayOfMonth = (((ruleDay2 - 1) * 7) + 1) + (((7 + ruleDayOfWeek) - ((dayOfWeek2 - dayOfMonth2) + 1)) % 7);
                break;
            case 3:
                ruleDayOfMonth = ruleDay2 + (((((49 + ruleDayOfWeek) - ruleDay2) - dayOfWeek2) + dayOfMonth2) % 7);
                break;
            case 4:
                ruleDayOfMonth = ruleDay2 - (((((49 - ruleDayOfWeek) + ruleDay2) + dayOfWeek2) - dayOfMonth2) % 7);
                break;
        }
        if (dayOfMonth2 < ruleDayOfMonth) {
            return -1;
        }
        if (dayOfMonth2 > ruleDayOfMonth) {
            return 1;
        }
        if (millis2 < i3) {
            return -1;
        }
        if (millis2 > i3) {
            return 1;
        }
        return 0;
    }

    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    public boolean observesDaylightTime() {
        return this.useDaylight;
    }

    public boolean inDaylightTime(Date date) {
        GregorianCalendar gc = new GregorianCalendar((TimeZone) this);
        gc.setTime(date);
        return gc.inDaylightTime();
    }

    private void construct(int _raw, int _startMonth, int _startDay, int _startDayOfWeek, int _startTime, int _startTimeMode, int _endMonth, int _endDay, int _endDayOfWeek, int _endTime, int _endTimeMode, int _dst) {
        this.raw = _raw;
        this.startMonth = _startMonth;
        this.startDay = _startDay;
        this.startDayOfWeek = _startDayOfWeek;
        this.startTime = _startTime;
        this.startTimeMode = _startTimeMode;
        this.endMonth = _endMonth;
        this.endDay = _endDay;
        this.endDayOfWeek = _endDayOfWeek;
        this.endTime = _endTime;
        this.endTimeMode = _endTimeMode;
        this.dst = _dst;
        this.startYear = 0;
        this.startMode = 1;
        this.endMode = 1;
        decodeRules();
        if (_dst <= 0) {
            throw new IllegalArgumentException();
        }
    }

    private void decodeRules() {
        decodeStartRule();
        decodeEndRule();
    }

    private void decodeStartRule() {
        boolean z = (this.startDay == 0 || this.endDay == 0) ? false : true;
        this.useDaylight = z;
        if (this.useDaylight && this.dst == 0) {
            this.dst = Grego.MILLIS_PER_DAY;
        }
        if (this.startDay == 0) {
            return;
        }
        if (this.startMonth < 0 || this.startMonth > 11) {
            throw new IllegalArgumentException();
        } else if (this.startTime < 0 || this.startTime > Grego.MILLIS_PER_DAY || this.startTimeMode < 0 || this.startTimeMode > 2) {
            throw new IllegalArgumentException();
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
                    throw new IllegalArgumentException();
                }
            }
            if (this.startMode == 2) {
                if (this.startDay < -5 || this.startDay > 5) {
                    throw new IllegalArgumentException();
                }
            } else if (this.startDay < 1 || this.startDay > staticMonthLength[this.startMonth]) {
                throw new IllegalArgumentException();
            }
        }
    }

    private void decodeEndRule() {
        boolean z = (this.startDay == 0 || this.endDay == 0) ? false : true;
        this.useDaylight = z;
        if (this.useDaylight && this.dst == 0) {
            this.dst = Grego.MILLIS_PER_DAY;
        }
        if (this.endDay == 0) {
            return;
        }
        if (this.endMonth < 0 || this.endMonth > 11) {
            throw new IllegalArgumentException();
        } else if (this.endTime < 0 || this.endTime > Grego.MILLIS_PER_DAY || this.endTimeMode < 0 || this.endTimeMode > 2) {
            throw new IllegalArgumentException();
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
                    throw new IllegalArgumentException();
                }
            }
            if (this.endMode == 2) {
                if (this.endDay < -5 || this.endDay > 5) {
                    throw new IllegalArgumentException();
                }
            } else if (this.endDay < 1 || this.endDay > staticMonthLength[this.endMonth]) {
                throw new IllegalArgumentException();
            }
        }
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) obj;
        if (!(this.raw == that.raw && this.useDaylight == that.useDaylight && idEquals(getID(), that.getID()) && (!this.useDaylight || (this.dst == that.dst && this.startMode == that.startMode && this.startMonth == that.startMonth && this.startDay == that.startDay && this.startDayOfWeek == that.startDayOfWeek && this.startTime == that.startTime && this.startTimeMode == that.startTimeMode && this.endMode == that.endMode && this.endMonth == that.endMonth && this.endDay == that.endDay && this.endDayOfWeek == that.endDayOfWeek && this.endTime == that.endTime && this.endTimeMode == that.endTimeMode && this.startYear == that.startYear)))) {
            z = false;
        }
        return z;
    }

    private boolean idEquals(String id1, String id2) {
        if (id1 == null && id2 == null) {
            return true;
        }
        if (id1 == null || id2 == null) {
            return false;
        }
        return id1.equals(id2);
    }

    public int hashCode() {
        int ret = (super.hashCode() + this.raw) ^ ((this.raw >>> 8) + (this.useDaylight ^ 1));
        return !this.useDaylight ? ret + ((((((((((((((this.dst ^ ((this.dst >>> 10) + this.startMode)) ^ ((this.startMode >>> 11) + this.startMonth)) ^ ((this.startMonth >>> 12) + this.startDay)) ^ ((this.startDay >>> 13) + this.startDayOfWeek)) ^ ((this.startDayOfWeek >>> 14) + this.startTime)) ^ ((this.startTime >>> 15) + this.startTimeMode)) ^ ((this.startTimeMode >>> 16) + this.endMode)) ^ ((this.endMode >>> 17) + this.endMonth)) ^ ((this.endMonth >>> 18) + this.endDay)) ^ ((this.endDay >>> 19) + this.endDayOfWeek)) ^ ((this.endDayOfWeek >>> 20) + this.endTime)) ^ ((this.endTime >>> 21) + this.endTimeMode)) ^ ((this.endTimeMode >>> 22) + this.startYear)) ^ (this.startYear >>> 23)) : ret;
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    public boolean hasSameRules(TimeZone othr) {
        boolean z = true;
        if (this == othr) {
            return true;
        }
        if (!(othr instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone other = (SimpleTimeZone) othr;
        if (other == null || this.raw != other.raw || this.useDaylight != other.useDaylight || (this.useDaylight && !(this.dst == other.dst && this.startMode == other.startMode && this.startMonth == other.startMonth && this.startDay == other.startDay && this.startDayOfWeek == other.startDayOfWeek && this.startTime == other.startTime && this.startTimeMode == other.startTimeMode && this.endMode == other.endMode && this.endMonth == other.endMonth && this.endDay == other.endDay && this.endDayOfWeek == other.endDayOfWeek && this.endTime == other.endTime && this.endTimeMode == other.endTimeMode && this.startYear == other.startYear))) {
            z = false;
        }
        return z;
    }

    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        if (!this.useDaylight) {
            return null;
        }
        initTransitionRules();
        long firstTransitionTime = this.firstTransition.getTime();
        if (base < firstTransitionTime || (inclusive && base == firstTransitionTime)) {
            return this.firstTransition;
        }
        boolean z = inclusive;
        Date stdDate = this.stdRule.getNextStart(base, this.dstRule.getRawOffset(), this.dstRule.getDSTSavings(), z);
        Date dstDate = this.dstRule.getNextStart(base, this.stdRule.getRawOffset(), this.stdRule.getDSTSavings(), z);
        if (stdDate != null && (dstDate == null || stdDate.before(dstDate))) {
            return new TimeZoneTransition(stdDate.getTime(), this.dstRule, this.stdRule);
        }
        if (dstDate == null || (stdDate != null && !dstDate.before(stdDate))) {
            return null;
        }
        return new TimeZoneTransition(dstDate.getTime(), this.stdRule, this.dstRule);
    }

    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        if (!this.useDaylight) {
            return null;
        }
        initTransitionRules();
        long firstTransitionTime = this.firstTransition.getTime();
        if (base < firstTransitionTime || (!inclusive && base == firstTransitionTime)) {
            return null;
        }
        boolean z = inclusive;
        Date stdDate = this.stdRule.getPreviousStart(base, this.dstRule.getRawOffset(), this.dstRule.getDSTSavings(), z);
        Date dstDate = this.dstRule.getPreviousStart(base, this.stdRule.getRawOffset(), this.stdRule.getDSTSavings(), z);
        if (stdDate != null && (dstDate == null || stdDate.after(dstDate))) {
            return new TimeZoneTransition(stdDate.getTime(), this.dstRule, this.stdRule);
        }
        if (dstDate == null || (stdDate != null && !dstDate.after(stdDate))) {
            return null;
        }
        return new TimeZoneTransition(dstDate.getTime(), this.stdRule, this.dstRule);
    }

    public TimeZoneRule[] getTimeZoneRules() {
        initTransitionRules();
        TimeZoneRule[] rules = new TimeZoneRule[(this.useDaylight ? 3 : 1)];
        rules[0] = this.initialRule;
        if (this.useDaylight) {
            rules[1] = this.stdRule;
            rules[2] = this.dstRule;
        }
        return rules;
    }

    private synchronized void initTransitionRules() {
        if (!this.transitionRulesInitialized) {
            if (this.useDaylight) {
                DateTimeRule dtRule = null;
                boolean z = true;
                int timeRuleType = this.startTimeMode == 1 ? 1 : this.startTimeMode == 2 ? 2 : 0;
                switch (this.startMode) {
                    case 1:
                        dtRule = new DateTimeRule(this.startMonth, this.startDay, this.startTime, timeRuleType);
                        break;
                    case 2:
                        dtRule = new DateTimeRule(this.startMonth, this.startDay, this.startDayOfWeek, this.startTime, timeRuleType);
                        break;
                    case 3:
                        dtRule = new DateTimeRule(this.startMonth, this.startDay, this.startDayOfWeek, true, this.startTime, timeRuleType);
                        break;
                    case 4:
                        dtRule = new DateTimeRule(this.startMonth, this.startDay, this.startDayOfWeek, false, this.startTime, timeRuleType);
                        break;
                    default:
                        break;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getID());
                stringBuilder.append("(DST)");
                this.dstRule = new AnnualTimeZoneRule(stringBuilder.toString(), getRawOffset(), getDSTSavings(), dtRule, this.startYear, Integer.MAX_VALUE);
                long firstDstStart = this.dstRule.getFirstStart(getRawOffset(), 0).getTime();
                if (this.endTimeMode == 1) {
                    z = true;
                } else if (this.endTimeMode != 2) {
                    z = false;
                }
                boolean timeRuleType2 = z;
                switch (this.endMode) {
                    case 1:
                        dtRule = new DateTimeRule(this.endMonth, this.endDay, this.endTime, timeRuleType2);
                        break;
                    case 2:
                        dtRule = new DateTimeRule(this.endMonth, this.endDay, this.endDayOfWeek, this.endTime, timeRuleType2);
                        break;
                    case 3:
                        dtRule = new DateTimeRule(this.endMonth, this.endDay, this.endDayOfWeek, true, this.endTime, timeRuleType2);
                        break;
                    case 4:
                        dtRule = new DateTimeRule(this.endMonth, this.endDay, this.endDayOfWeek, false, this.endTime, timeRuleType2);
                        break;
                    default:
                        break;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(getID());
                stringBuilder2.append("(STD)");
                this.stdRule = new AnnualTimeZoneRule(stringBuilder2.toString(), getRawOffset(), 0, dtRule, this.startYear, Integer.MAX_VALUE);
                long firstStdStart = this.stdRule.getFirstStart(getRawOffset(), this.dstRule.getDSTSavings()).getTime();
                if (firstStdStart < firstDstStart) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(getID());
                    stringBuilder3.append("(DST)");
                    this.initialRule = new InitialTimeZoneRule(stringBuilder3.toString(), getRawOffset(), this.dstRule.getDSTSavings());
                    this.firstTransition = new TimeZoneTransition(firstStdStart, this.initialRule, this.stdRule);
                } else {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(getID());
                    stringBuilder4.append("(STD)");
                    this.initialRule = new InitialTimeZoneRule(stringBuilder4.toString(), getRawOffset(), 0);
                    this.firstTransition = new TimeZoneTransition(firstDstStart, this.initialRule, this.dstRule);
                }
            } else {
                this.initialRule = new InitialTimeZoneRule(getID(), getRawOffset(), 0);
            }
            this.transitionRulesInitialized = true;
        }
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    public TimeZone cloneAsThawed() {
        SimpleTimeZone tz = (SimpleTimeZone) super.cloneAsThawed();
        tz.isFrozen = false;
        return tz;
    }
}
