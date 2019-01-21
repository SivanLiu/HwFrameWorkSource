package java.sql;

import java.util.Date;

public class Time extends Date {
    static final long serialVersionUID = 8397324403548013681L;

    @Deprecated
    public Time(int hour, int minute, int second) {
        super(70, 0, 1, hour, minute, second);
    }

    public Time(long time) {
        super(time);
    }

    public void setTime(long time) {
        super.setTime(time);
    }

    public static Time valueOf(String s) {
        if (s != null) {
            int firstColon = s.indexOf(58);
            int secondColon = s.indexOf(58, firstColon + 1);
            int i = 1;
            int i2 = (firstColon > 0 ? 1 : 0) & (secondColon > 0 ? 1 : 0);
            if (secondColon >= s.length() - 1) {
                i = 0;
            }
            if ((i & i2) != 0) {
                return new Time(Integer.parseInt(s.substring(0, firstColon)), Integer.parseInt(s.substring(firstColon + 1, secondColon)), Integer.parseInt(s.substring(secondColon + 1)));
            }
            throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException();
    }

    public String toString() {
        String hourString;
        String minuteString;
        String secondString;
        int hour = super.getHours();
        int minute = super.getMinutes();
        int second = super.getSeconds();
        if (hour < 10) {
            hourString = new StringBuilder();
            hourString.append("0");
            hourString.append(hour);
            hourString = hourString.toString();
        } else {
            hourString = Integer.toString(hour);
        }
        if (minute < 10) {
            minuteString = new StringBuilder();
            minuteString.append("0");
            minuteString.append(minute);
            minuteString = minuteString.toString();
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hourString);
        stringBuilder.append(":");
        stringBuilder.append(minuteString);
        stringBuilder.append(":");
        stringBuilder.append(secondString);
        return stringBuilder.toString();
    }

    @Deprecated
    public int getYear() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getMonth() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getDay() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public int getDate() {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setYear(int i) {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setMonth(int i) {
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setDate(int i) {
        throw new IllegalArgumentException();
    }
}
