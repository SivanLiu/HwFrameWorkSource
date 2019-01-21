package android.util;

import android.os.SystemClock;
import android.text.format.DateFormat;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB;

public class TimeUtils {
    public static final int HUNDRED_DAY_FIELD_LEN = 19;
    public static final long NANOS_PER_MS = 1000000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static char[] sFormatStr = new char[29];
    private static final Object sFormatSync = new Object();
    private static SimpleDateFormat sLoggingFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static char[] sTmpFormatStr = new char[29];

    public static TimeZone getTimeZone(int offset, boolean dst, long when, String country) {
        android.icu.util.TimeZone icuTimeZone = getIcuTimeZone(offset, dst, when, country);
        return icuTimeZone != null ? TimeZone.getTimeZone(icuTimeZone.getID()) : null;
    }

    private static android.icu.util.TimeZone getIcuTimeZone(int offset, boolean dst, long when, String country) {
        if (country == null) {
            return null;
        }
        return TimeZoneFinder.getInstance().lookupTimeZoneByCountryAndOffset(country, offset, dst, when, android.icu.util.TimeZone.getDefault());
    }

    public static String getTimeZoneDatabaseVersion() {
        return ZoneInfoDB.getInstance().getVersion();
    }

    private static int accumField(int amt, int suffix, boolean always, int zeropad) {
        int num = 0;
        if (amt > StatsLogInternal.UID_PROCESS_STATE_CHANGED__STATE__PROCESS_STATE_UNKNOWN) {
            while (amt != 0) {
                num++;
                amt /= 10;
            }
            return num + suffix;
        } else if (amt > 99 || (always && zeropad >= 3)) {
            return 3 + suffix;
        } else {
            if (amt > 9 || (always && zeropad >= 2)) {
                return 2 + suffix;
            }
            if (always || amt > 0) {
                return 1 + suffix;
            }
            return 0;
        }
    }

    private static int printFieldLocked(char[] formatStr, int amt, char suffix, int pos, boolean always, int zeropad) {
        if (!always && amt <= 0) {
            return pos;
        }
        int startPos = pos;
        int tmp;
        if (amt > StatsLogInternal.UID_PROCESS_STATE_CHANGED__STATE__PROCESS_STATE_UNKNOWN) {
            tmp = 0;
            while (amt != 0 && tmp < sTmpFormatStr.length) {
                sTmpFormatStr[tmp] = (char) ((amt % 10) + 48);
                tmp++;
                amt /= 10;
            }
            for (tmp--; tmp >= 0; tmp--) {
                formatStr[pos] = sTmpFormatStr[tmp];
                pos++;
            }
        } else {
            if ((always && zeropad >= 3) || amt > 99) {
                tmp = amt / 100;
                formatStr[pos] = (char) (tmp + 48);
                pos++;
                amt -= tmp * 100;
            }
            if ((always && zeropad >= 2) || amt > 9 || startPos != pos) {
                tmp = amt / 10;
                formatStr[pos] = (char) (tmp + 48);
                pos++;
                amt -= tmp * 10;
            }
            formatStr[pos] = (char) (amt + 48);
            pos++;
        }
        formatStr[pos] = suffix;
        return pos + 1;
    }

    /* JADX WARNING: Missing block: B:72:0x0135, code skipped:
            if (r9 != r7) goto L_0x013c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int formatDurationLocked(long duration, int fieldLen) {
        long duration2 = duration;
        int fieldLen2 = fieldLen;
        if (sFormatStr.length < fieldLen2) {
            sFormatStr = new char[fieldLen2];
        }
        char[] formatStr = sFormatStr;
        int pos;
        int pos2;
        if (duration2 == 0) {
            pos = 0;
            fieldLen2--;
            while (pos < fieldLen2) {
                pos2 = pos + 1;
                formatStr[pos] = ' ';
                pos = pos2;
            }
            formatStr[pos] = '0';
            return pos + 1;
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
        pos = (int) Math.floor((double) (duration2 / 1000));
        pos2 = 0;
        if (pos >= SECONDS_PER_DAY) {
            pos2 = pos / SECONDS_PER_DAY;
            pos -= SECONDS_PER_DAY * pos2;
        }
        int days = pos2;
        if (pos >= 3600) {
            pos2 = pos / 3600;
            pos -= pos2 * 3600;
            hours = pos2;
        } else {
            hours = 0;
        }
        if (pos >= 60) {
            pos2 = pos / 60;
            seconds = pos - (pos2 * 60);
            minutes = pos2;
        } else {
            seconds = pos;
            minutes = 0;
        }
        pos = 0;
        int i = 3;
        boolean z = false;
        if (fieldLen2 != 0) {
            pos2 = accumField(days, 1, false, 0);
            if (pos2 > 0) {
                z = true;
            }
            pos2 += accumField(hours, 1, z, 2);
            pos2 += accumField(minutes, 1, pos2 > 0, 2);
            pos2 += accumField(seconds, 1, pos2 > 0, 2);
            for (pos2 += accumField(millis, 2, true, pos2 > 0 ? 3 : 0) + 1; pos2 < fieldLen2; pos2++) {
                formatStr[pos] = ' ';
                pos++;
            }
        }
        formatStr[pos] = prefix2;
        int pos3 = pos + 1;
        boolean zeropad = fieldLen2 != 0;
        boolean z2 = true;
        int start = pos3;
        int i2 = 2;
        int pos4 = printFieldLocked(formatStr, days, DateFormat.DATE, pos3, false, 0);
        int start2 = start;
        int start3 = start2;
        pos3 = pos4;
        pos4 = printFieldLocked(formatStr, hours, DateFormat.HOUR, pos4, pos4 != start2 ? z2 : false, zeropad ? i2 : 0);
        start2 = start3;
        int start4 = start2;
        pos3 = pos4;
        pos4 = printFieldLocked(formatStr, minutes, DateFormat.MINUTE, pos4, pos4 != start2 ? z2 : false, zeropad ? i2 : 0);
        start2 = start4;
        if (pos4 == start2) {
            z2 = false;
        }
        if (!zeropad) {
            i2 = 0;
        }
        int start5 = start2;
        pos3 = pos4;
        pos4 = printFieldLocked(formatStr, seconds, DateFormat.SECONDS, pos4, z2, i2);
        int start6 = zeropad ? start5 : start5;
        i = 0;
        pos3 = pos4;
        pos = printFieldLocked(formatStr, millis, DateFormat.MINUTE, pos4, true, i);
        formatStr[pos] = DateFormat.SECONDS;
        return pos + 1;
    }

    public static void formatDuration(long duration, StringBuilder builder) {
        synchronized (sFormatSync) {
            builder.append(sFormatStr, 0, formatDurationLocked(duration, 0));
        }
    }

    public static void formatDuration(long duration, StringBuilder builder, int fieldLen) {
        synchronized (sFormatSync) {
            builder.append(sFormatStr, 0, formatDurationLocked(duration, fieldLen));
        }
    }

    public static void formatDuration(long duration, PrintWriter pw, int fieldLen) {
        synchronized (sFormatSync) {
            pw.print(new String(sFormatStr, 0, formatDurationLocked(duration, fieldLen)));
        }
    }

    public static String formatDuration(long duration) {
        String str;
        synchronized (sFormatSync) {
            str = new String(sFormatStr, 0, formatDurationLocked(duration, 0));
        }
        return str;
    }

    public static void formatDuration(long duration, PrintWriter pw) {
        formatDuration(duration, pw, 0);
    }

    public static void formatDuration(long time, long now, PrintWriter pw) {
        if (time == 0) {
            pw.print("--");
        } else {
            formatDuration(time - now, pw, 0);
        }
    }

    public static String formatUptime(long time) {
        long diff = time - SystemClock.uptimeMillis();
        StringBuilder stringBuilder;
        if (diff > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(time);
            stringBuilder.append(" (in ");
            stringBuilder.append(diff);
            stringBuilder.append(" ms)");
            return stringBuilder.toString();
        } else if (diff < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(time);
            stringBuilder.append(" (");
            stringBuilder.append(-diff);
            stringBuilder.append(" ms ago)");
            return stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(time);
            stringBuilder.append(" (now)");
            return stringBuilder.toString();
        }
    }

    public static String logTimeOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        if (millis < 0) {
            return Long.toString(millis);
        }
        c.setTimeInMillis(millis);
        return String.format("%tm-%td %tH:%tM:%tS.%tL", new Object[]{c, c, c, c, c, c});
    }

    public static String formatForLogging(long millis) {
        if (millis <= 0) {
            return "unknown";
        }
        return sLoggingFormat.format(new Date(millis));
    }
}
