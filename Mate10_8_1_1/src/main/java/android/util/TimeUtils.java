package android.util;

import android.os.SystemClock;
import android.text.format.DateFormat;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.SmsConstants;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB;

public class TimeUtils {
    private static final boolean DBG = false;
    public static final int HUNDRED_DAY_FIELD_LEN = 19;
    public static final long NANOS_PER_MS = 1000000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final String TAG = "TimeUtils";
    private static char[] sFormatStr = new char[29];
    private static final Object sFormatSync = new Object();
    private static String sLastUniqueCountry = null;
    private static final Object sLastUniqueLockObj = new Object();
    private static List<String> sLastUniqueZoneOffsets = null;
    private static SimpleDateFormat sLoggingFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static char[] sTmpFormatStr = new char[29];

    public static TimeZone getTimeZone(int offset, boolean dst, long when, String country) {
        android.icu.util.TimeZone icuTimeZone = getIcuTimeZone(offset, dst, when, country);
        if (icuTimeZone != null) {
            return TimeZone.getTimeZone(icuTimeZone.getID());
        }
        return null;
    }

    private static android.icu.util.TimeZone getIcuTimeZone(int offset, boolean dst, long when, String country) {
        if (country == null) {
            return null;
        }
        return TimeZoneFinder.getInstance().lookupTimeZoneByCountryAndOffset(country, offset, dst, when, android.icu.util.TimeZone.getDefault());
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static List<String> getTimeZoneIdsWithUniqueOffsets(String country) {
        synchronized (sLastUniqueLockObj) {
            if (country != null) {
                if (country.equals(sLastUniqueCountry)) {
                    List<String> list = sLastUniqueZoneOffsets;
                    return list;
                }
            }
        }
    }

    private static List<String> extractZoneIds(List<android.icu.util.TimeZone> timeZones) {
        List<String> ids = new ArrayList(timeZones.size());
        for (android.icu.util.TimeZone timeZone : timeZones) {
            ids.add(timeZone.getID());
        }
        return Collections.unmodifiableList(ids);
    }

    public static List<TimeZone> getTimeZones(String countryIso) {
        List<TimeZone> timeZones = new ArrayList();
        List<android.icu.util.TimeZone> icuTimeZones = getIcuTimeZones(countryIso);
        if (icuTimeZones == null) {
            return timeZones;
        }
        for (int i = 0; i < icuTimeZones.size(); i++) {
            timeZones.add(TimeZone.getTimeZone(((android.icu.util.TimeZone) icuTimeZones.get(i)).getID()));
        }
        return timeZones;
    }

    private static List<android.icu.util.TimeZone> getIcuTimeZones(String countryIso) {
        if (countryIso == null) {
            return Collections.emptyList();
        }
        List<android.icu.util.TimeZone> timeZones = TimeZoneFinder.getInstance().lookupTimeZonesByCountry(countryIso);
        if (timeZones == null) {
            return Collections.emptyList();
        }
        return timeZones;
    }

    public static String getTimeZoneDatabaseVersion() {
        return ZoneInfoDB.getInstance().getVersion();
    }

    private static int accumField(int amt, int suffix, boolean always, int zeropad) {
        if (amt > MetricsEvent.ASSIST_GESTURE_TRIGGERED) {
            int num = 0;
            while (amt != 0) {
                num++;
                amt /= 10;
            }
            return num + suffix;
        } else if (amt > 99 || (always && zeropad >= 3)) {
            return suffix + 3;
        } else {
            if (amt > 9 || (always && zeropad >= 2)) {
                return suffix + 2;
            }
            if (always || amt > 0) {
                return suffix + 1;
            }
            return 0;
        }
    }

    private static int printFieldLocked(char[] formatStr, int amt, char suffix, int pos, boolean always, int zeropad) {
        if (!always && amt <= 0) {
            return pos;
        }
        int startPos = pos;
        if (amt > MetricsEvent.ASSIST_GESTURE_TRIGGERED) {
            int tmp = 0;
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
            int dig;
            if (!always || zeropad < 3) {
                if (amt > 99) {
                }
                if ((!always || zeropad < 2) && amt <= 9) {
                    if (startPos != pos) {
                    }
                    formatStr[pos] = (char) (amt + 48);
                    pos++;
                }
                dig = amt / 10;
                formatStr[pos] = (char) (dig + 48);
                pos++;
                amt -= dig * 10;
                formatStr[pos] = (char) (amt + 48);
                pos++;
            }
            dig = amt / 100;
            formatStr[pos] = (char) (dig + 48);
            pos++;
            amt -= dig * 100;
            if (startPos != pos) {
                dig = amt / 10;
                formatStr[pos] = (char) (dig + 48);
                pos++;
                amt -= dig * 10;
            }
            formatStr[pos] = (char) (amt + 48);
            pos++;
        }
        formatStr[pos] = suffix;
        return pos + 1;
    }

    private static int formatDurationLocked(long duration, int fieldLen) {
        if (sFormatStr.length < fieldLen) {
            sFormatStr = new char[fieldLen];
        }
        char[] formatStr = sFormatStr;
        if (duration == 0) {
            fieldLen--;
            int pos = 0;
            while (pos < fieldLen) {
                int pos2 = pos + 1;
                formatStr[pos] = ' ';
                pos = pos2;
            }
            formatStr[pos] = '0';
            return pos + 1;
        }
        char prefix;
        if (duration > 0) {
            prefix = '+';
        } else {
            prefix = '-';
            duration = -duration;
        }
        int millis = (int) (duration % 1000);
        int seconds = (int) Math.floor((double) (duration / 1000));
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (seconds >= SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= SECONDS_PER_DAY * days;
        }
        if (seconds >= SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        pos2 = 0;
        if (fieldLen != 0) {
            int myLen = accumField(days, 1, false, 0);
            myLen += accumField(hours, 1, myLen > 0, 2);
            myLen += accumField(minutes, 1, myLen > 0, 2);
            myLen += accumField(seconds, 1, myLen > 0, 2);
            for (myLen += accumField(millis, 2, true, myLen > 0 ? 3 : 0) + 1; myLen < fieldLen; myLen++) {
                formatStr[pos2] = ' ';
                pos2++;
            }
        }
        formatStr[pos2] = prefix;
        pos2++;
        int start = pos2;
        boolean zeropad = fieldLen != 0;
        pos2 = printFieldLocked(formatStr, days, DateFormat.DATE, pos2, false, 0);
        pos2 = printFieldLocked(formatStr, hours, DateFormat.HOUR, pos2, pos2 != start, zeropad ? 2 : 0);
        pos2 = printFieldLocked(formatStr, minutes, DateFormat.MINUTE, pos2, pos2 != start, zeropad ? 2 : 0);
        pos2 = printFieldLocked(formatStr, seconds, DateFormat.SECONDS, pos2, pos2 != start, zeropad ? 2 : 0);
        int i = (!zeropad || pos2 == start) ? 0 : 3;
        pos2 = printFieldLocked(formatStr, millis, DateFormat.MINUTE, pos2, true, i);
        formatStr[pos2] = DateFormat.SECONDS;
        return pos2 + 1;
    }

    public static void formatDuration(long duration, StringBuilder builder) {
        synchronized (sFormatSync) {
            builder.append(sFormatStr, 0, formatDurationLocked(duration, 0));
        }
    }

    public static void formatDuration(long duration, PrintWriter pw, int fieldLen) {
        synchronized (sFormatSync) {
            pw.print(new String(sFormatStr, 0, formatDurationLocked(duration, fieldLen)));
        }
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
        if (diff > 0) {
            return time + " (in " + diff + " ms)";
        }
        if (diff < 0) {
            return time + " (" + (-diff) + " ms ago)";
        }
        return time + " (now)";
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
            return SmsConstants.FORMAT_UNKNOWN;
        }
        return sLoggingFormat.format(new Date(millis));
    }
}
