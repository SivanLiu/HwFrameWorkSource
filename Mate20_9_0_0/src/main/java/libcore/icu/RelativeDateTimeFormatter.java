package libcore.icu;

import android.icu.text.ArabicShaping;
import android.icu.text.DisplayContext;
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit;
import android.icu.text.RelativeDateTimeFormatter.Direction;
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit;
import android.icu.text.RelativeDateTimeFormatter.Style;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import java.util.Locale;
import java.util.TimeZone;
import libcore.util.BasicLruCache;

public final class RelativeDateTimeFormatter {
    private static final FormatterCache CACHED_FORMATTERS = new FormatterCache();
    public static final long DAY_IN_MILLIS = 86400000;
    private static final int DAY_IN_MS = 86400000;
    private static final int EPOCH_JULIAN_DAY = 2440588;
    public static final long HOUR_IN_MILLIS = 3600000;
    public static final long MINUTE_IN_MILLIS = 60000;
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long WEEK_IN_MILLIS = 604800000;
    public static final long YEAR_IN_MILLIS = 31449600000L;

    static class FormatterCache extends BasicLruCache<String, android.icu.text.RelativeDateTimeFormatter> {
        FormatterCache() {
            super(8);
        }
    }

    private RelativeDateTimeFormatter() {
    }

    public static String getRelativeTimeSpanString(Locale locale, TimeZone tz, long time, long now, long minResolution, int flags) {
        return getRelativeTimeSpanString(locale, tz, time, now, minResolution, flags, DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
    }

    public static String getRelativeTimeSpanString(Locale locale, TimeZone tz, long time, long now, long minResolution, int flags, DisplayContext displayContext) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        } else if (tz != null) {
            return getRelativeTimeSpanString(ULocale.forLocale(locale), DateUtilsBridge.icuTimeZone(tz), time, now, minResolution, flags, displayContext);
        } else {
            throw new NullPointerException("tz == null");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f7 A:{SYNTHETIC, Splitter:B:69:0x00f7} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00f7 A:{SYNTHETIC, Splitter:B:69:0x00f7} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String getRelativeTimeSpanString(ULocale icuLocale, android.icu.util.TimeZone icuTimeZone, long time, long now, long minResolution, int flags, DisplayContext displayContext) {
        Style style;
        Direction direction;
        boolean count;
        ULocale uLocale;
        Direction direction2;
        AbsoluteUnit aunit;
        boolean count2;
        Throwable th;
        ULocale uLocale2 = icuLocale;
        android.icu.util.TimeZone timeZone = icuTimeZone;
        long j = time;
        boolean relative = now;
        DisplayContext displayContext2 = displayContext;
        long duration = Math.abs(relative - j);
        boolean past = relative >= j;
        if ((flags & ArabicShaping.TASHKEEL_REPLACE_BY_TATWEEL) != 0) {
            style = Style.SHORT;
        } else {
            style = Style.LONG;
        }
        Style style2 = style;
        if (past) {
            direction = Direction.LAST;
        } else {
            direction = Direction.NEXT;
        }
        Direction direction3 = direction;
        boolean relative2 = true;
        AbsoluteUnit aunit2 = null;
        RelativeUnit unit;
        if (duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS) {
            count = (int) (duration / 1000);
            unit = RelativeUnit.SECONDS;
        } else if (duration < HOUR_IN_MILLIS && minResolution < HOUR_IN_MILLIS) {
            count = (int) (duration / MINUTE_IN_MILLIS);
            unit = RelativeUnit.MINUTES;
        } else if (duration < 86400000 && minResolution < 86400000) {
            count = (int) (duration / HOUR_IN_MILLIS);
            unit = RelativeUnit.HOURS;
        } else if (duration >= WEEK_IN_MILLIS || minResolution >= WEEK_IN_MILLIS) {
            uLocale = icuLocale;
            if (minResolution == WEEK_IN_MILLIS) {
                count = (int) (duration / WEEK_IN_MILLIS);
                unit = RelativeUnit.WEEKS;
                direction2 = direction3;
                relative = true;
                aunit = null;
                count2 = count;
                synchronized (CACHED_FORMATTERS) {
                    try {
                        android.icu.text.RelativeDateTimeFormatter formatter = getFormatter(uLocale, style2, displayContext2);
                        String format;
                        if (relative) {
                            format = formatter.format((double) count2, direction2, unit);
                            return format;
                        }
                        relative = count2;
                        format = formatter.format(direction2, aunit);
                        return format;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
            int flags2;
            Calendar timeCalendar = DateUtilsBridge.createIcuCalendar(timeZone, uLocale, j);
            if ((flags & 12) == 0) {
                if (timeCalendar.get(1) != DateUtilsBridge.createIcuCalendar(timeZone, uLocale, now).get(1)) {
                    flags2 = flags | 4;
                } else {
                    flags2 = flags | 8;
                }
            } else {
                long j2 = now;
                flags2 = flags;
            }
            return DateTimeFormat.format(uLocale, timeCalendar, flags2, displayContext2);
        } else {
            boolean count3 = Math.abs(dayDistance(icuTimeZone, time, now));
            RelativeUnit relativeUnit = RelativeUnit.DAYS;
            if (count3) {
                String str;
                if (past) {
                    synchronized (CACHED_FORMATTERS) {
                        boolean z = past;
                        uLocale = icuLocale;
                        str = getFormatter(uLocale, style2, displayContext2).format(Direction.LAST_2, AbsoluteUnit.DAY);
                    }
                } else {
                    uLocale = icuLocale;
                    synchronized (CACHED_FORMATTERS) {
                        str = getFormatter(uLocale, style2, displayContext2).format(Direction.NEXT_2, AbsoluteUnit.DAY);
                    }
                }
                if (!(str == null || str.isEmpty())) {
                    return str;
                }
            }
            uLocale = icuLocale;
            if (count3) {
                aunit2 = AbsoluteUnit.DAY;
                relative2 = false;
            } else if (!count3) {
                aunit2 = AbsoluteUnit.DAY;
                direction3 = Direction.THIS;
                relative2 = false;
            }
            count = count3;
            direction2 = direction3;
            relative = relative2;
            aunit = aunit2;
            unit = relativeUnit;
            count2 = count;
            synchronized (CACHED_FORMATTERS) {
            }
        }
        direction2 = direction3;
        relative = true;
        aunit = null;
        uLocale = icuLocale;
        count2 = count;
        synchronized (CACHED_FORMATTERS) {
        }
    }

    public static String getRelativeDateTimeString(Locale locale, TimeZone tz, long time, long now, long minResolution, long transitionResolution, int flags) {
        long j = time;
        long j2 = now;
        if (locale == null) {
            throw new NullPointerException("locale == null");
        } else if (tz != null) {
            Style style;
            Style style2;
            Calendar timeCalendar;
            ULocale icuLocale;
            String dateClause;
            ULocale icuLocale2 = ULocale.forLocale(locale);
            android.icu.util.TimeZone icuTimeZone = DateUtilsBridge.icuTimeZone(tz);
            long duration = Math.abs(j2 - j);
            long transitionResolution2 = transitionResolution > WEEK_IN_MILLIS ? WEEK_IN_MILLIS : transitionResolution;
            if ((flags & ArabicShaping.TASHKEEL_REPLACE_BY_TATWEEL) != 0) {
                style = Style.SHORT;
            } else {
                style = Style.LONG;
            }
            Style style3 = style;
            Calendar timeCalendar2 = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale2, j);
            Calendar nowCalendar = DateUtilsBridge.createIcuCalendar(icuTimeZone, icuLocale2, j2);
            int days = Math.abs(DateUtilsBridge.dayDistance(timeCalendar2, nowCalendar));
            if (duration < transitionResolution2) {
                long minResolution2 = (days <= 0 || minResolution >= 86400000) ? minResolution : 86400000;
                long j3 = j;
                int i = 1;
                style2 = style3;
                timeCalendar = timeCalendar2;
                icuLocale = icuLocale2;
                dateClause = getRelativeTimeSpanString(icuLocale2, icuTimeZone, j3, j2, minResolution2, flags, DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
                int i2 = flags;
                long j4 = minResolution2;
            } else {
                int flags2;
                style2 = style3;
                timeCalendar = timeCalendar2;
                android.icu.util.TimeZone timeZone = icuTimeZone;
                icuLocale = icuLocale2;
                if (timeCalendar.get(1) != nowCalendar.get(1)) {
                    flags2 = 131092;
                } else {
                    flags2 = 65560;
                }
                dateClause = DateTimeFormat.format(icuLocale, timeCalendar, flags2, DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
            }
            String dateClause2 = dateClause;
            String timeClause = DateTimeFormat.format(icuLocale, timeCalendar, 1, DisplayContext.CAPITALIZATION_NONE);
            DisplayContext capitalizationContext = DisplayContext.CAPITALIZATION_NONE;
            synchronized (CACHED_FORMATTERS) {
                dateClause = getFormatter(icuLocale, style2, capitalizationContext).combineDateAndTime(dateClause2, timeClause);
            }
            return dateClause;
        } else {
            throw new NullPointerException("tz == null");
        }
    }

    private static android.icu.text.RelativeDateTimeFormatter getFormatter(ULocale locale, Style style, DisplayContext displayContext) {
        String key = new StringBuilder();
        key.append(locale);
        key.append("\t");
        key.append(style);
        key.append("\t");
        key.append(displayContext);
        key = key.toString();
        android.icu.text.RelativeDateTimeFormatter formatter = (android.icu.text.RelativeDateTimeFormatter) CACHED_FORMATTERS.get(key);
        if (formatter != null) {
            return formatter;
        }
        formatter = android.icu.text.RelativeDateTimeFormatter.getInstance(locale, null, style, displayContext);
        CACHED_FORMATTERS.put(key, formatter);
        return formatter;
    }

    private static int dayDistance(android.icu.util.TimeZone icuTimeZone, long startTime, long endTime) {
        return julianDay(icuTimeZone, endTime) - julianDay(icuTimeZone, startTime);
    }

    private static int julianDay(android.icu.util.TimeZone icuTimeZone, long time) {
        return ((int) ((((long) icuTimeZone.getOffset(time)) + time) / 86400000)) + EPOCH_JULIAN_DAY;
    }
}
