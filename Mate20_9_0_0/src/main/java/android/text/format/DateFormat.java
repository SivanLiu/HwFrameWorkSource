package android.text.format;

import android.content.Context;
import android.net.wifi.WifiScanLog;
import android.provider.Settings.System;
import android.telephony.NetworkScanRequest;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.ICU;
import libcore.icu.LocaleData;

public class DateFormat {
    @Deprecated
    public static final char AM_PM = 'a';
    @Deprecated
    public static final char CAPITAL_AM_PM = 'A';
    @Deprecated
    public static final char DATE = 'd';
    @Deprecated
    public static final char DAY = 'E';
    @Deprecated
    public static final char HOUR = 'h';
    @Deprecated
    public static final char HOUR_OF_DAY = 'k';
    @Deprecated
    public static final char MINUTE = 'm';
    @Deprecated
    public static final char MONTH = 'M';
    @Deprecated
    public static final char QUOTE = '\'';
    @Deprecated
    public static final char SECONDS = 's';
    @Deprecated
    public static final char STANDALONE_MONTH = 'L';
    @Deprecated
    public static final char TIME_ZONE = 'z';
    @Deprecated
    public static final char YEAR = 'y';
    private static boolean sIs24Hour;
    private static Locale sIs24HourLocale;
    private static final Object sLocaleLock = new Object();

    public static boolean is24HourFormat(Context context) {
        return is24HourFormat(context, context.getUserId());
    }

    public static boolean is24HourFormat(Context context, int userHandle) {
        String value = System.getStringForUser(context.getContentResolver(), System.TIME_12_24, userHandle);
        if (value != null) {
            return value.equals(WifiScanLog.EVENT_KEY24);
        }
        return is24HourLocale(context.getResources().getConfiguration().locale);
    }

    /* JADX WARNING: Missing block: B:11:0x0014, code skipped:
            r1 = java.text.DateFormat.getTimeInstance(1, r4);
     */
    /* JADX WARNING: Missing block: B:12:0x001b, code skipped:
            if ((r1 instanceof java.text.SimpleDateFormat) == false) goto L_0x002b;
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            r0 = hasDesignator(((java.text.SimpleDateFormat) r1).toPattern(), 'H');
     */
    /* JADX WARNING: Missing block: B:14:0x002b, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:15:0x002c, code skipped:
            r2 = r0;
            r3 = sLocaleLock;
     */
    /* JADX WARNING: Missing block: B:16:0x002f, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            sIs24HourLocale = r4;
            sIs24Hour = r2;
     */
    /* JADX WARNING: Missing block: B:19:0x0034, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:20:0x0035, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean is24HourLocale(Locale locale) {
        synchronized (sLocaleLock) {
            if (sIs24HourLocale == null || !sIs24HourLocale.equals(locale)) {
            } else {
                boolean z = sIs24Hour;
                return z;
            }
        }
    }

    public static String getBestDateTimePattern(Locale locale, String skeleton) {
        return ICU.getBestDateTimePattern(skeleton, locale);
    }

    public static java.text.DateFormat getTimeFormat(Context context) {
        return new SimpleDateFormat(getTimeFormatString(context), context.getResources().getConfiguration().locale);
    }

    public static String getTimeFormatString(Context context) {
        return getTimeFormatString(context, context.getUserId());
    }

    public static String getTimeFormatString(Context context, int userHandle) {
        LocaleData d = LocaleData.get(context.getResources().getConfiguration().locale);
        return is24HourFormat(context, userHandle) ? d.timeFormat_Hm : d.timeFormat_hm;
    }

    public static java.text.DateFormat getDateFormat(Context context) {
        return java.text.DateFormat.getDateInstance(3, context.getResources().getConfiguration().locale);
    }

    public static java.text.DateFormat getLongDateFormat(Context context) {
        return java.text.DateFormat.getDateInstance(1, context.getResources().getConfiguration().locale);
    }

    public static java.text.DateFormat getMediumDateFormat(Context context) {
        return java.text.DateFormat.getDateInstance(2, context.getResources().getConfiguration().locale);
    }

    public static char[] getDateFormatOrder(Context context) {
        return ICU.getDateFormatOrder(getDateFormatString(context));
    }

    private static String getDateFormatString(Context context) {
        java.text.DateFormat df = java.text.DateFormat.getDateInstance(3, context.getResources().getConfiguration().locale);
        if (df instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) df).toPattern();
        }
        throw new AssertionError("!(df instanceof SimpleDateFormat)");
    }

    public static CharSequence format(CharSequence inFormat, long inTimeInMillis) {
        return format(inFormat, new Date(inTimeInMillis));
    }

    public static CharSequence format(CharSequence inFormat, Date inDate) {
        Calendar c = new GregorianCalendar();
        c.setTime(inDate);
        return format(inFormat, c);
    }

    public static boolean hasSeconds(CharSequence inFormat) {
        return hasDesignator(inFormat, SECONDS);
    }

    public static boolean hasDesignator(CharSequence inFormat, char designator) {
        if (inFormat == null) {
            return false;
        }
        int length = inFormat.length();
        boolean insideQuote = false;
        for (int i = 0; i < length; i++) {
            char c = inFormat.charAt(i);
            boolean z = true;
            if (c == QUOTE) {
                if (insideQuote) {
                    z = false;
                }
                insideQuote = z;
            } else if (!insideQuote && c == designator) {
                return true;
            }
        }
        return false;
    }

    public static CharSequence format(CharSequence inFormat, Calendar inDate) {
        SpannableStringBuilder s = new SpannableStringBuilder(inFormat);
        LocaleData localeData = LocaleData.get(Locale.getDefault());
        int len = inFormat.length();
        int i = 0;
        while (i < len) {
            int count = 1;
            char c = s.charAt(i);
            if (c == '\'') {
                count = appendQuotedText(s, i);
                len = s.length();
            } else {
                CharSequence replacement;
                while (i + count < len && s.charAt(i + count) == c) {
                    count++;
                }
                switch (c) {
                    case 'A':
                    case 'a':
                        replacement = localeData.amPm[inDate.get(9) - 0];
                        break;
                    case 'E':
                    case 'c':
                        replacement = getDayOfWeekString(localeData, inDate.get(7), count, c);
                        break;
                    case 'H':
                    case 'k':
                        replacement = zeroPad(inDate.get(11), count);
                        break;
                    case 'K':
                    case 'h':
                        String replacement2 = inDate.get(10);
                        if (c == 'h' && replacement2 == null) {
                            replacement2 = 12;
                        }
                        replacement = zeroPad(replacement2, count);
                        break;
                    case 'L':
                    case 'M':
                        replacement = getMonthString(localeData, inDate.get(2), count, c);
                        break;
                    case 'd':
                        replacement = zeroPad(inDate.get(5), count);
                        break;
                    case 'm':
                        replacement = zeroPad(inDate.get(12), count);
                        break;
                    case 's':
                        replacement = zeroPad(inDate.get(13), count);
                        break;
                    case 'y':
                        replacement = getYearString(inDate.get(1), count);
                        break;
                    case 'z':
                        replacement = getTimeZoneString(inDate, count);
                        break;
                    default:
                        replacement = null;
                        break;
                }
                if (replacement != null) {
                    s.replace(i, i + count, replacement);
                    count = replacement.length();
                    len = s.length();
                }
            }
            i += count;
        }
        if (inFormat instanceof Spanned) {
            return new SpannedString(s);
        }
        return s.toString();
    }

    private static String getDayOfWeekString(LocaleData ld, int day, int count, int kind) {
        boolean standalone = kind == 99;
        if (count == 5) {
            return standalone ? ld.tinyStandAloneWeekdayNames[day] : ld.tinyWeekdayNames[day];
        } else if (count == 4) {
            return standalone ? ld.longStandAloneWeekdayNames[day] : ld.longWeekdayNames[day];
        } else {
            return standalone ? ld.shortStandAloneWeekdayNames[day] : ld.shortWeekdayNames[day];
        }
    }

    private static String getMonthString(LocaleData ld, int month, int count, int kind) {
        boolean standalone = kind == 76;
        if (count == 5) {
            return standalone ? ld.tinyStandAloneMonthNames[month] : ld.tinyMonthNames[month];
        } else if (count == 4) {
            return standalone ? ld.longStandAloneMonthNames[month] : ld.longMonthNames[month];
        } else if (count != 3) {
            return zeroPad(month + 1, count);
        } else {
            return standalone ? ld.shortStandAloneMonthNames[month] : ld.shortMonthNames[month];
        }
    }

    private static String getTimeZoneString(Calendar inDate, int count) {
        TimeZone tz = inDate.getTimeZone();
        if (count < 2) {
            return formatZoneOffset(inDate.get(16) + inDate.get(15), count);
        }
        return tz.getDisplayName(inDate.get(16) != 0, 0);
    }

    private static String formatZoneOffset(int offset, int count) {
        offset /= 1000;
        StringBuilder tb = new StringBuilder();
        if (offset < 0) {
            tb.insert(0, "-");
            offset = -offset;
        } else {
            tb.insert(0, "+");
        }
        int minutes = (offset % NetworkScanRequest.MAX_SEARCH_MAX_SEC) / 60;
        tb.append(zeroPad(offset / NetworkScanRequest.MAX_SEARCH_MAX_SEC, 2));
        tb.append(zeroPad(minutes, 2));
        return tb.toString();
    }

    private static String getYearString(int year, int count) {
        if (count == 2) {
            return zeroPad(year % 100, 2);
        }
        return String.format(Locale.getDefault(), "%d", new Object[]{Integer.valueOf(year)});
    }

    public static int appendQuotedText(SpannableStringBuilder formatString, int index) {
        int length = formatString.length();
        if (index + 1 >= length || formatString.charAt(index + 1) != QUOTE) {
            int count = 0;
            formatString.delete(index, index + 1);
            length--;
            while (index < length) {
                if (formatString.charAt(index) != QUOTE) {
                    index++;
                    count++;
                } else if (index + 1 >= length || formatString.charAt(index + 1) != QUOTE) {
                    formatString.delete(index, index + 1);
                    break;
                } else {
                    formatString.delete(index, index + 1);
                    length--;
                    count++;
                    index++;
                }
            }
            return count;
        }
        formatString.delete(index, index + 1);
        return 1;
    }

    private static String zeroPad(int inValue, int inMinDigits) {
        Locale locale = Locale.getDefault();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%0");
        stringBuilder.append(inMinDigits);
        stringBuilder.append("d");
        return String.format(locale, stringBuilder.toString(), new Object[]{Integer.valueOf(inValue)});
    }
}
