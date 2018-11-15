package android.support.v4.util;

import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.io.PrintWriter;

@RestrictTo({Scope.LIBRARY_GROUP})
public final class TimeUtils {
    @RestrictTo({Scope.LIBRARY_GROUP})
    public static final int HUNDRED_DAY_FIELD_LEN = 19;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static char[] sFormatStr = new char[24];
    private static final Object sFormatSync = new Object();

    private static int accumField(int amt, int suffix, boolean always, int zeropad) {
        if (amt > 99 || (always && zeropad >= 3)) {
            return 3 + suffix;
        }
        if (amt > 9 || (always && zeropad >= 2)) {
            return 2 + suffix;
        }
        if (always || amt > 0) {
            return 1 + suffix;
        }
        return 0;
    }

    private static int printField(char[] formatStr, int amt, char suffix, int pos, boolean always, int zeropad) {
        if (!always && amt <= 0) {
            return pos;
        }
        int dig;
        int startPos = pos;
        if ((always && zeropad >= 3) || amt > 99) {
            dig = amt / 100;
            formatStr[pos] = (char) (dig + 48);
            pos++;
            amt -= dig * 100;
        }
        if ((always && zeropad >= 2) || amt > 9 || startPos != pos) {
            dig = amt / 10;
            formatStr[pos] = (char) (dig + 48);
            pos++;
            amt -= dig * 10;
        }
        formatStr[pos] = (char) (amt + 48);
        pos++;
        formatStr[pos] = suffix;
        return pos + 1;
    }

    /* JADX WARNING: Missing block: B:72:0x0132, code:
            if (r9 != r7) goto L_0x0139;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int formatDurationLocked(long duration, int fieldLen) {
        long duration2 = duration;
        int fieldLen2 = fieldLen;
        if (sFormatStr.length < fieldLen2) {
            sFormatStr = new char[fieldLen2];
        }
        char[] formatStr = sFormatStr;
        if (duration2 == 0) {
            fieldLen2--;
            while (0 < fieldLen2) {
                formatStr[0] = ' ';
            }
            formatStr[0] = '0';
            return 0 + 1;
        }
        char prefix;
        int hours;
        int seconds;
        int minutes;
        if (duration2 > 0) {
            prefix = '+';
        } else {
            prefix = '-';
            duration2 = -duration2;
        }
        char prefix2 = prefix;
        int millis = (int) (duration2 % 1000);
        int seconds2 = (int) Math.floor((double) (duration2 / 1000));
        int days = 0;
        if (seconds2 > SECONDS_PER_DAY) {
            days = seconds2 / SECONDS_PER_DAY;
            seconds2 -= SECONDS_PER_DAY * days;
        }
        int days2 = days;
        if (seconds2 > SECONDS_PER_HOUR) {
            days = seconds2 / SECONDS_PER_HOUR;
            seconds2 -= days * SECONDS_PER_HOUR;
            hours = days;
        } else {
            hours = 0;
        }
        if (seconds2 > SECONDS_PER_MINUTE) {
            days = seconds2 / SECONDS_PER_MINUTE;
            seconds = seconds2 - (days * SECONDS_PER_MINUTE);
            minutes = days;
        } else {
            seconds = seconds2;
            minutes = 0;
        }
        seconds2 = 0;
        int i = 3;
        boolean z = false;
        if (fieldLen2 != 0) {
            days = accumField(days2, 1, false, 0);
            if (days > 0) {
                z = true;
            }
            days += accumField(hours, 1, z, 2);
            days += accumField(minutes, 1, days > 0, 2);
            days += accumField(seconds, 1, days > 0, 2);
            for (days += accumField(millis, 2, true, days > 0 ? 3 : 0) + 1; days < fieldLen2; days++) {
                formatStr[seconds2] = ' ';
                seconds2++;
            }
        }
        formatStr[seconds2] = prefix2;
        int pos = seconds2 + 1;
        boolean zeropad = fieldLen2 != 0;
        boolean z2 = true;
        int start = pos;
        int i2 = 2;
        int pos2 = printField(formatStr, days2, 'd', pos, false, 0);
        int start2 = start;
        int start3 = start2;
        pos = pos2;
        pos2 = printField(formatStr, hours, 'h', pos2, pos2 != start2 ? z2 : false, zeropad ? i2 : 0);
        start2 = start3;
        int start4 = start2;
        pos = pos2;
        pos2 = printField(formatStr, minutes, 'm', pos2, pos2 != start2 ? z2 : false, zeropad ? i2 : 0);
        start2 = start4;
        if (pos2 == start2) {
            z2 = false;
        }
        if (!zeropad) {
            i2 = 0;
        }
        int start5 = start2;
        pos = pos2;
        pos2 = printField(formatStr, seconds, 's', pos2, z2, i2);
        int start6 = zeropad ? start5 : start5;
        i = 0;
        pos = pos2;
        seconds2 = printField(formatStr, millis, 'm', pos2, true, i);
        formatStr[seconds2] = 's';
        return seconds2 + 1;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static void formatDuration(long duration, StringBuilder builder) {
        synchronized (sFormatSync) {
            builder.append(sFormatStr, 0, formatDurationLocked(duration, 0));
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static void formatDuration(long duration, PrintWriter pw, int fieldLen) {
        synchronized (sFormatSync) {
            pw.print(new String(sFormatStr, 0, formatDurationLocked(duration, fieldLen)));
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static void formatDuration(long duration, PrintWriter pw) {
        formatDuration(duration, pw, 0);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static void formatDuration(long time, long now, PrintWriter pw) {
        if (time == 0) {
            pw.print("--");
        } else {
            formatDuration(time - now, pw, 0);
        }
    }

    private TimeUtils() {
    }
}
