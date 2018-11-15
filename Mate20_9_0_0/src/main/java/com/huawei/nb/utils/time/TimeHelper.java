package com.huawei.nb.utils.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeHelper {
    public static final long DAY_TIME_IN_MS = 86400000;
    public static final String TIME_FORMAT_PATTEN = "^([2][0-3]|[0-1][0-9]):([0-5][0-9]):([0-5][0-9])$";
    public static final int TIME_UNIT_DAY = 4;
    public static final int TIME_UNIT_MONTH = 2;
    public static final int TIME_UNIT_WEEK = 3;
    public static final int TIME_UNIT_YEAR = 1;

    public static long now() {
        return System.nanoTime();
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long getTimeMillis(String time) {
        try {
            return new SimpleDateFormat("yy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yy-MM-dd").format(new Date()) + " " + time).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private static long getMilliSecs(int threshold, int unit) {
        if (threshold <= 0) {
            return 0;
        }
        switch (unit) {
            case 1:
                return 31536000000L * ((long) threshold);
            case 2:
                return 2592000000L * ((long) threshold);
            case 3:
                return 604800000 * ((long) threshold);
            case 4:
                return 86400000;
            default:
                return 0;
        }
    }

    public static boolean isTimeExpired(long ground, long ceil, int threshold, int unit) {
        if (ground <= 0 || threshold <= 0) {
            return false;
        }
        if (ceil - ground > getMilliSecs(threshold, unit)) {
            return true;
        }
        return false;
    }
}
