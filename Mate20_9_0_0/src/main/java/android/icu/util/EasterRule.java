package android.icu.util;

import java.util.Date;

/* compiled from: EasterHoliday */
class EasterRule implements DateRule {
    private static GregorianCalendar gregorian = new GregorianCalendar();
    private static GregorianCalendar orthodox = new GregorianCalendar();
    private GregorianCalendar calendar = gregorian;
    private int daysAfterEaster;

    public EasterRule(int daysAfterEaster, boolean isOrthodox) {
        this.daysAfterEaster = daysAfterEaster;
        if (isOrthodox) {
            orthodox.setGregorianChange(new Date(Long.MAX_VALUE));
            this.calendar = orthodox;
        }
    }

    public Date firstAfter(Date start) {
        return doFirstBetween(start, null);
    }

    public Date firstBetween(Date start, Date end) {
        return doFirstBetween(start, end);
    }

    public boolean isOn(Date date) {
        boolean z;
        synchronized (this.calendar) {
            this.calendar.setTime(date);
            int dayOfYear = this.calendar.get(6);
            this.calendar.setTime(computeInYear(this.calendar.getTime(), this.calendar));
            z = this.calendar.get(6) == dayOfYear;
        }
        return z;
    }

    public boolean isBetween(Date start, Date end) {
        return firstBetween(start, end) != null;
    }

    /* JADX WARNING: Missing block: B:13:0x0038, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Date doFirstBetween(Date start, Date end) {
        synchronized (this.calendar) {
            Date result = computeInYear(start, this.calendar);
            if (result.before(start)) {
                this.calendar.setTime(start);
                this.calendar.get(1);
                this.calendar.add(1, 1);
                result = computeInYear(this.calendar.getTime(), this.calendar);
            }
            if (end == null || result.before(end)) {
            } else {
                return null;
            }
        }
    }

    private Date computeInYear(Date date, GregorianCalendar cal) {
        Date time;
        if (cal == null) {
            cal = this.calendar;
        }
        synchronized (cal) {
            int c;
            int h;
            int i;
            int j;
            cal.setTime(date);
            int year = cal.get(1);
            int g = year % 19;
            if (cal.getTime().after(cal.getGregorianChange())) {
                c = year / 100;
                h = ((((c - (c / 4)) - (((8 * c) + 13) / 25)) + (19 * g)) + 15) % 30;
                i = h - ((h / 28) * (1 - (((h / 28) * (29 / (h + 1))) * ((21 - g) / 11))));
                j = ((((((year / 4) + year) + i) + 2) - c) + (c / 4)) % 7;
            } else {
                i = ((19 * g) + 15) % 30;
                j = (((year / 4) + year) + i) % 7;
            }
            c = i - j;
            h = 3 + ((c + 40) / 44);
            int d = (c + 28) - (31 * (h / 4));
            cal.clear();
            cal.set(0, 1);
            cal.set(1, year);
            cal.set(2, h - 1);
            cal.set(5, d);
            cal.getTime();
            cal.add(5, this.daysAfterEaster);
            time = cal.getTime();
        }
        return time;
    }
}
