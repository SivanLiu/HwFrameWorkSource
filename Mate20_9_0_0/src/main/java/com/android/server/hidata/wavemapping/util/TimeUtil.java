package com.android.server.hidata.wavemapping.util;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtil {
    public static final String DATE_FORMAT = "yyMMdd";
    private static String DATE_PATTERN = "yyyyMMdd HH:mm:ss.SSS";
    private static String DATE_PATTERN_02 = "MMddHHmmss";
    private static final String TAG;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(TimeUtil.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public String getTimePATTERN02() {
        return new SimpleDateFormat(DATE_PATTERN_02, Locale.getDefault()).format(new Date());
    }

    public int getTimeIntPATTERN02() {
        try {
            return Integer.parseInt(new SimpleDateFormat(DATE_PATTERN_02, Locale.getDefault()).format(new Date()));
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getTimeIntPATTERN02:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return 0;
        }
    }

    public String getTime(long time, SimpleDateFormat dateFormat) {
        return dateFormat.format(new Date(time));
    }

    public static String getTime() {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date());
    }

    public String changeDateFormat(String strDate) {
        String retDate = null;
        if (strDate == null || strDate.equals("")) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        try {
            retDate = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(dateFormat.parse(strDate));
        } catch (ParseException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("changeDateFormat,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return retDate;
    }

    public String getSomeDay(Date date, int day) {
        String newDate = null;
        if (date == null) {
            return null;
        }
        try {
            SimpleDateFormat dateFormat2 = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(5, day);
            newDate = dateFormat2.format(calendar.getTime());
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  getSomeDay ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        return newDate;
    }

    public int time2IntDate(String strDate) {
        int ret = 0;
        if (strDate == null || strDate.equals("")) {
            return 0;
        }
        try {
            ret = Integer.parseInt(changeDateFormat(strDate));
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("time2IntDate exception,");
            stringBuilder.append(strDate);
            LogUtil.e(stringBuilder.toString());
        }
        return ret;
    }

    public int timeStamp2DateInt(String timestampString) {
        int ret = 0;
        if (timestampString == null || timestampString.equals("")) {
            return 0;
        }
        try {
            ret = Integer.parseInt(new SimpleDateFormat(DATE_PATTERN_02, Locale.CHINA).format(new Date(Long.valueOf(Long.parseLong(timestampString)).longValue())));
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("timeStamp2DateInt exception,");
            stringBuilder.append(timestampString);
            LogUtil.e(stringBuilder.toString());
        }
        return ret;
    }
}
