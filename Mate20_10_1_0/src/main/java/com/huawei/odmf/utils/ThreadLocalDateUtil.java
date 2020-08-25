package com.huawei.odmf.utils;

import com.huawei.odmf.exception.ODMFException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ThreadLocalDateUtil {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    private static final Object LOCK = new Object();
    private static final String TIME_FORMAT = "HH:mm:ss";
    private static DateFormat dateFormat;
    private static DateFormat timeFormat;

    private ThreadLocalDateUtil() {
    }

    public static Date parseDate(String strDate) {
        Date parse;
        synchronized (LOCK) {
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(DATE_FORMAT);
            }
            try {
                parse = dateFormat.parse(strDate);
            } catch (ParseException e) {
                throw new ODMFException("error happens when parsing date");
            }
        }
        return parse;
    }

    public static Time parseTime(String strDate) {
        Time time;
        synchronized (LOCK) {
            if (timeFormat == null) {
                timeFormat = new SimpleDateFormat(TIME_FORMAT);
            }
            try {
                time = new Time(timeFormat.parse(strDate).getTime());
            } catch (ParseException e) {
                throw new ODMFException("error happens when parsing time");
            }
        }
        return time;
    }

    public static Timestamp parseTimestamp(String strDate) {
        Timestamp timestamp;
        synchronized (LOCK) {
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(DATE_FORMAT);
            }
            try {
                timestamp = new Timestamp(dateFormat.parse(strDate).getTime());
            } catch (ParseException e) {
                throw new ODMFException("error happens when parsing time");
            }
        }
        return timestamp;
    }

    public static Calendar parseCalendar(String strCalendar) {
        Calendar calendar;
        synchronized (LOCK) {
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(DATE_FORMAT);
            }
            try {
                calendar = Calendar.getInstance();
                calendar.setTime(dateFormat.parse(strCalendar));
            } catch (ParseException e) {
                throw new ODMFException("error happens when parsing time");
            }
        }
        return calendar;
    }
}
