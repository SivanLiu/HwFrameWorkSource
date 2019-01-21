package libcore.icu;

import android.icu.text.DateFormat;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.DisplayContext;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import libcore.util.BasicLruCache;

public class DateTimeFormat {
    private static final FormatterCache CACHED_FORMATTERS = new FormatterCache();

    static class FormatterCache extends BasicLruCache<String, DateFormat> {
        FormatterCache() {
            super(8);
        }
    }

    private DateTimeFormat() {
    }

    public static String format(ULocale icuLocale, Calendar time, int flags, DisplayContext displayContext) {
        String format;
        String skeleton = DateUtilsBridge.toSkeleton(time, flags);
        String key = new StringBuilder();
        key.append(skeleton);
        key.append("\t");
        key.append(icuLocale);
        key.append("\t");
        key.append(time.getTimeZone());
        key = key.toString();
        synchronized (CACHED_FORMATTERS) {
            DateFormat formatter = (DateFormat) CACHED_FORMATTERS.get(key);
            if (formatter == null) {
                formatter = new SimpleDateFormat(DateTimePatternGenerator.getInstance(icuLocale).getBestPattern(skeleton), icuLocale);
                CACHED_FORMATTERS.put(key, formatter);
            }
            formatter.setContext(displayContext);
            format = formatter.format(time);
        }
        return format;
    }
}
