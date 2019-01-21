package java.text;

import java.util.Calendar;

class CalendarBuilder {
    private static final int COMPUTED = 1;
    public static final int ISO_DAY_OF_WEEK = 1000;
    private static final int MAX_FIELD = 18;
    private static final int MINIMUM_USER_STAMP = 2;
    private static final int UNSET = 0;
    public static final int WEEK_YEAR = 17;
    private final int[] field = new int[36];
    private int maxFieldIndex = -1;
    private int nextStamp = 2;

    CalendarBuilder() {
    }

    CalendarBuilder set(int index, int value) {
        if (index == 1000) {
            index = 7;
            value = toCalendarDayOfWeek(value);
        }
        int[] iArr = this.field;
        int i = this.nextStamp;
        this.nextStamp = i + 1;
        iArr[index] = i;
        this.field[18 + index] = value;
        if (index > this.maxFieldIndex && index < 17) {
            this.maxFieldIndex = index;
        }
        return this;
    }

    CalendarBuilder addYear(int value) {
        int[] iArr = this.field;
        iArr[19] = iArr[19] + value;
        iArr = this.field;
        iArr[35] = iArr[35] + value;
        return this;
    }

    boolean isSet(int index) {
        if (index == 1000) {
            index = 7;
        }
        return this.field[index] > 0;
    }

    CalendarBuilder clear(int index) {
        if (index == 1000) {
            index = 7;
        }
        this.field[index] = 0;
        this.field[18 + index] = 0;
        return this;
    }

    Calendar establish(Calendar cal) {
        int stamp;
        int index;
        boolean weekDate = isSet(17) && this.field[17] > this.field[1];
        if (weekDate && !cal.isWeekDateSupported()) {
            if (!isSet(1)) {
                set(1, this.field[35]);
            }
            weekDate = false;
        }
        cal.clear();
        for (stamp = 2; stamp < this.nextStamp; stamp++) {
            for (index = 0; index <= this.maxFieldIndex; index++) {
                if (this.field[index] == stamp) {
                    cal.set(index, this.field[18 + index]);
                    break;
                }
            }
        }
        if (weekDate) {
            int weekOfYear = isSet(3) ? this.field[21] : 1;
            stamp = isSet(7) ? this.field[25] : cal.getFirstDayOfWeek();
            if (!isValidDayOfWeek(stamp) && cal.isLenient()) {
                if (stamp >= 8) {
                    stamp--;
                    weekOfYear += stamp / 7;
                    index = (stamp % 7) + 1;
                } else {
                    index = stamp;
                    while (index <= 0) {
                        index += 7;
                        weekOfYear--;
                    }
                }
                stamp = toCalendarDayOfWeek(index);
            }
            cal.setWeekDate(this.field[35], weekOfYear, stamp);
        }
        return cal;
    }

    public String toString() {
        int i;
        StringBuilder sb = new StringBuilder();
        sb.append("CalendarBuilder:[");
        for (i = 0; i < this.field.length; i++) {
            if (isSet(i)) {
                sb.append(i);
                sb.append('=');
                sb.append(this.field[18 + i]);
                sb.append(',');
            }
        }
        i = sb.length() - 1;
        if (sb.charAt(i) == ',') {
            sb.setLength(i);
        }
        sb.append(']');
        return sb.toString();
    }

    static int toISODayOfWeek(int calendarDayOfWeek) {
        return calendarDayOfWeek == 1 ? 7 : calendarDayOfWeek - 1;
    }

    static int toCalendarDayOfWeek(int isoDayOfWeek) {
        if (!isValidDayOfWeek(isoDayOfWeek)) {
            return isoDayOfWeek;
        }
        return isoDayOfWeek == 7 ? 1 : isoDayOfWeek + 1;
    }

    static boolean isValidDayOfWeek(int dayOfWeek) {
        return dayOfWeek > 0 && dayOfWeek <= 7;
    }
}
