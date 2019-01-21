package java.sql;

import java.time.Year;
import java.util.Date;
import sun.util.locale.LanguageTag;

public class Timestamp extends Date {
    static final long serialVersionUID = 2745179027874758501L;
    private int nanos;

    @Deprecated
    public Timestamp(int year, int month, int date, int hour, int minute, int second, int nano) {
        super(year, month, date, hour, minute, second);
        if (nano > Year.MAX_VALUE || nano < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        this.nanos = nano;
    }

    public Timestamp(long time) {
        super((time / 1000) * 1000);
        this.nanos = (int) ((time % 1000) * 1000000);
        if (this.nanos < 0) {
            this.nanos = 1000000000 + this.nanos;
            super.setTime(((time / 1000) - 1) * 1000);
        }
    }

    public void setTime(long time) {
        super.setTime((time / 1000) * 1000);
        this.nanos = (int) ((time % 1000) * 1000000);
        if (this.nanos < 0) {
            this.nanos = 1000000000 + this.nanos;
            super.setTime(((time / 1000) - 1) * 1000);
        }
    }

    public long getTime() {
        return ((long) (this.nanos / 1000000)) + super.getTime();
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x01bc  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00e1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Timestamp valueOf(String s) {
        int day = 0;
        String formatError = "Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]";
        String zeros = "000000000";
        String delimiterDate = LanguageTag.SEP;
        String delimiterTime = ":";
        int YEAR_LENGTH;
        int MONTH_LENGTH;
        int DAY_LENGTH;
        int MAX_MONTH;
        int MAX_DAY;
        int year;
        int month;
        int a_nanos;
        if (s != null) {
            YEAR_LENGTH = 4;
            int YEAR_LENGTH2 = s.trim();
            MONTH_LENGTH = 2;
            int MONTH_LENGTH2 = YEAR_LENGTH2.indexOf(32);
            String s2;
            int dividingSpace;
            if (MONTH_LENGTH2 > 0) {
                DAY_LENGTH = 2;
                MAX_MONTH = 12;
                int MAX_MONTH2 = YEAR_LENGTH2.substring(0, MONTH_LENGTH2);
                int DAY_LENGTH2 = YEAR_LENGTH2.substring(MONTH_LENGTH2 + 1);
                s2 = YEAR_LENGTH2;
                dividingSpace = MONTH_LENGTH2;
                MONTH_LENGTH2 = MAX_MONTH2.indexOf(45);
                MAX_DAY = 31;
                YEAR_LENGTH2 = MAX_MONTH2.indexOf(45, MONTH_LENGTH2 + 1);
                int secondDash;
                int i;
                Object obj;
                if (DAY_LENGTH2 != 0) {
                    int firstColon = DAY_LENGTH2.indexOf(58);
                    year = 0;
                    int MAX_DAY2 = DAY_LENGTH2.indexOf(58, firstColon + 1);
                    int year2 = DAY_LENGTH2.indexOf(46, MAX_DAY2 + 1);
                    boolean secondColon = false;
                    if (MONTH_LENGTH2 <= 0 || YEAR_LENGTH2 <= 0) {
                        secondDash = YEAR_LENGTH2;
                        i = MONTH_LENGTH2;
                        obj = MAX_MONTH2;
                        month = 0;
                    } else {
                        month = 0;
                        if (YEAR_LENGTH2 < MAX_MONTH2.length() - 1) {
                            String yyyy = MAX_MONTH2.substring(0, MONTH_LENGTH2);
                            int month2 = MAX_MONTH2.substring(MONTH_LENGTH2 + 1, YEAR_LENGTH2);
                            i = MONTH_LENGTH2;
                            MONTH_LENGTH2 = MAX_MONTH2.substring(YEAR_LENGTH2 + 1);
                            secondDash = YEAR_LENGTH2;
                            String date_s = MAX_MONTH2;
                            if (yyyy.length() == 4 && month2.length() >= 1 && month2.length() <= 2 && MONTH_LENGTH2.length() >= 1 && MONTH_LENGTH2.length() <= 2) {
                                YEAR_LENGTH2 = Integer.parseInt(yyyy);
                                MAX_MONTH2 = Integer.parseInt(month2);
                                day = Integer.parseInt(MONTH_LENGTH2);
                                int year3 = YEAR_LENGTH2;
                                if (MAX_MONTH2 >= 1 && MAX_MONTH2 <= 12 && day >= 1 && day <= 31) {
                                    secondColon = true;
                                }
                                YEAR_LENGTH2 = year3;
                                int i2;
                                boolean z;
                                if (secondColon) {
                                    a_nanos = 0;
                                    i2 = firstColon;
                                    z = secondColon;
                                    throw new IllegalArgumentException(formatError);
                                }
                                if ((((firstColon > 0 ? 1 : 0) & (MAX_DAY2 > 0 ? 1 : 0)) & (MAX_DAY2 < DAY_LENGTH2.length() - 1 ? 1 : 0)) != 0) {
                                    int second;
                                    MONTH_LENGTH2 = Integer.parseInt(DAY_LENGTH2.substring(0, firstColon));
                                    month2 = Integer.parseInt(DAY_LENGTH2.substring(firstColon + 1, MAX_DAY2));
                                    int i3 = year2 > 0 ? 1 : 0;
                                    a_nanos = 0;
                                    year = 1;
                                    if (year2 >= DAY_LENGTH2.length() - 1) {
                                        year = 0;
                                    }
                                    if ((i3 & year) != 0) {
                                        int a_nanos2 = Integer.parseInt(DAY_LENGTH2.substring(MAX_DAY2 + 1, year2));
                                        yyyy = DAY_LENGTH2.substring(year2 + 1);
                                        int second2 = a_nanos2;
                                        if (yyyy.length() > 9) {
                                            z = secondColon;
                                            throw new IllegalArgumentException(formatError);
                                        } else if (Character.isDigit(yyyy.charAt(0)) != 0) {
                                            firstColon = new StringBuilder();
                                            firstColon.append(yyyy);
                                            z = secondColon;
                                            firstColon.append(zeros.substring(0, 9 - yyyy.length()));
                                            a_nanos = Integer.parseInt(firstColon.toString());
                                            second = second2;
                                        } else {
                                            z = secondColon;
                                            throw new IllegalArgumentException(formatError);
                                        }
                                    }
                                    z = secondColon;
                                    if (year2 <= 0) {
                                        second = Integer.parseInt(DAY_LENGTH2.substring(MAX_DAY2 + 1));
                                    } else {
                                        throw new IllegalArgumentException(formatError);
                                    }
                                    return new Timestamp(YEAR_LENGTH2 - 1900, MAX_MONTH2 - 1, day, MONTH_LENGTH2, month2, second, a_nanos);
                                }
                                a_nanos = 0;
                                i2 = firstColon;
                                z = secondColon;
                                throw new IllegalArgumentException(formatError);
                            }
                        }
                        secondDash = YEAR_LENGTH2;
                        i = MONTH_LENGTH2;
                        obj = MAX_MONTH2;
                    }
                    YEAR_LENGTH2 = year;
                    MAX_MONTH2 = month;
                    if (secondColon) {
                    }
                } else {
                    secondDash = YEAR_LENGTH2;
                    i = MONTH_LENGTH2;
                    obj = MAX_MONTH2;
                    year = 0;
                    month = 0;
                    a_nanos = 0;
                    throw new IllegalArgumentException(formatError);
                }
            }
            s2 = YEAR_LENGTH2;
            dividingSpace = MONTH_LENGTH2;
            DAY_LENGTH = 2;
            MAX_MONTH = 12;
            MAX_DAY = 31;
            year = 0;
            month = 0;
            a_nanos = 0;
            throw new IllegalArgumentException(formatError);
        }
        YEAR_LENGTH = 4;
        MONTH_LENGTH = 2;
        DAY_LENGTH = 2;
        MAX_MONTH = 12;
        MAX_DAY = 31;
        year = 0;
        month = 0;
        a_nanos = 0;
        throw new IllegalArgumentException("null string");
    }

    public String toString() {
        String yearString;
        String monthString;
        String dayString;
        String hourString;
        String minuteString;
        String secondString;
        String nanosString;
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int day = super.getDate();
        int hour = super.getHours();
        int minute = super.getMinutes();
        int second = super.getSeconds();
        String zeros = "000000000";
        String yearZeros = "0000";
        if (year < 1000) {
            yearString = new StringBuilder();
            yearString.append("");
            yearString.append(year);
            yearString = yearString.toString();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(yearZeros.substring(0, 4 - yearString.length()));
            stringBuilder.append(yearString);
            yearString = stringBuilder.toString();
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("");
            stringBuilder2.append(year);
            yearString = stringBuilder2.toString();
        }
        if (month < 10) {
            monthString = new StringBuilder();
            monthString.append("0");
            monthString.append(month);
            monthString = monthString.toString();
        } else {
            monthString = Integer.toString(month);
        }
        if (day < 10) {
            dayString = new StringBuilder();
            dayString.append("0");
            dayString.append(day);
            dayString = dayString.toString();
        } else {
            dayString = Integer.toString(day);
        }
        if (hour < 10) {
            hourString = new StringBuilder();
            hourString.append("0");
            hourString.append(hour);
            hourString = hourString.toString();
        } else {
            hourString = Integer.toString(hour);
        }
        if (minute < 10) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("0");
            stringBuilder3.append(minute);
            minuteString = stringBuilder3.toString();
        } else {
            minuteString = Integer.toString(minute);
        }
        if (second < 10) {
            secondString = new StringBuilder();
            secondString.append("0");
            secondString.append(second);
            secondString = secondString.toString();
        } else {
            secondString = Integer.toString(second);
        }
        if (this.nanos == 0) {
            nanosString = "0";
            int i = year;
            int i2 = month;
        } else {
            nanosString = Integer.toString(this.nanos);
            String nanosString2 = new StringBuilder();
            nanosString2.append(zeros.substring(0, 9 - nanosString.length()));
            nanosString2.append(nanosString);
            nanosString2 = nanosString2.toString();
            char[] nanosChar = new char[nanosString2.length()];
            nanosString2.getChars(0, nanosString2.length(), nanosChar, 0);
            month = 8;
            while (true) {
                String nanosString3 = nanosString2;
                if (nanosChar[month] != '0') {
                    break;
                }
                month--;
                nanosString2 = nanosString3;
            }
            nanosString = new String(nanosChar, 0, month + 1);
        }
        StringBuffer timestampBuf = new StringBuffer(20 + nanosString.length());
        timestampBuf.append(yearString);
        timestampBuf.append(LanguageTag.SEP);
        timestampBuf.append(monthString);
        timestampBuf.append(LanguageTag.SEP);
        timestampBuf.append(dayString);
        timestampBuf.append(" ");
        timestampBuf.append(hourString);
        timestampBuf.append(":");
        timestampBuf.append(minuteString);
        timestampBuf.append(":");
        timestampBuf.append(secondString);
        timestampBuf.append(".");
        timestampBuf.append(nanosString);
        return timestampBuf.toString();
    }

    public int getNanos() {
        return this.nanos;
    }

    public void setNanos(int n) {
        if (n > Year.MAX_VALUE || n < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        this.nanos = n;
    }

    public boolean equals(Timestamp ts) {
        if (super.equals(ts) && this.nanos == ts.nanos) {
            return true;
        }
        return false;
    }

    public boolean equals(Object ts) {
        if (ts instanceof Timestamp) {
            return equals((Timestamp) ts);
        }
        return false;
    }

    public boolean before(Timestamp ts) {
        return compareTo(ts) < 0;
    }

    public boolean after(Timestamp ts) {
        return compareTo(ts) > 0;
    }

    public int compareTo(Timestamp ts) {
        long thisTime = getTime();
        long anotherTime = ts.getTime();
        int i = thisTime < anotherTime ? -1 : thisTime == anotherTime ? 0 : 1;
        if (i == 0) {
            if (this.nanos > ts.nanos) {
                return 1;
            }
            if (this.nanos < ts.nanos) {
                return -1;
            }
        }
        return i;
    }

    public int compareTo(Date o) {
        if (o instanceof Timestamp) {
            return compareTo((Timestamp) o);
        }
        return compareTo(new Timestamp(o.getTime()));
    }

    public int hashCode() {
        return super.hashCode();
    }
}
