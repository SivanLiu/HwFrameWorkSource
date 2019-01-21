package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

public final class ListFormatter {
    static Cache cache = new Cache();
    private final String end;
    private final ULocale locale;
    private final String middle;
    private final String start;
    private final String two;

    private static class Cache {
        private final ICUCache<String, ListFormatter> cache;

        private Cache() {
            this.cache = new SimpleCache();
        }

        public ListFormatter get(ULocale locale, String style) {
            String key = String.format("%s:%s", new Object[]{locale.toString(), style});
            ListFormatter result = (ListFormatter) this.cache.get(key);
            if (result != null) {
                return result;
            }
            result = load(locale, style);
            this.cache.put(key, result);
            return result;
        }

        private static ListFormatter load(ULocale ulocale, String style) {
            ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ulocale);
            StringBuilder sb = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("listPattern/");
            stringBuilder.append(style);
            stringBuilder.append("/2");
            String access$000 = ListFormatter.compilePattern(r.getWithFallback(stringBuilder.toString()).getString(), sb);
            stringBuilder = new StringBuilder();
            stringBuilder.append("listPattern/");
            stringBuilder.append(style);
            stringBuilder.append("/start");
            String access$0002 = ListFormatter.compilePattern(r.getWithFallback(stringBuilder.toString()).getString(), sb);
            stringBuilder = new StringBuilder();
            stringBuilder.append("listPattern/");
            stringBuilder.append(style);
            stringBuilder.append("/middle");
            String access$0003 = ListFormatter.compilePattern(r.getWithFallback(stringBuilder.toString()).getString(), sb);
            stringBuilder = new StringBuilder();
            stringBuilder.append("listPattern/");
            stringBuilder.append(style);
            stringBuilder.append("/end");
            return new ListFormatter(access$000, access$0002, access$0003, ListFormatter.compilePattern(r.getWithFallback(stringBuilder.toString()).getString(), sb), ulocale);
        }
    }

    static class FormattedListBuilder {
        private StringBuilder current;
        private int offset;

        public FormattedListBuilder(Object start, boolean recordOffset) {
            this.current = new StringBuilder(start.toString());
            this.offset = recordOffset ? 0 : -1;
        }

        public FormattedListBuilder append(String pattern, Object next, boolean recordOffset) {
            int[] offsets = (recordOffset || offsetRecorded()) ? new int[2] : null;
            SimpleFormatterImpl.formatAndReplace(pattern, this.current, offsets, this.current, next.toString());
            if (offsets != null) {
                if (offsets[0] == -1 || offsets[1] == -1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{0} or {1} missing from pattern ");
                    stringBuilder.append(pattern);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (recordOffset) {
                    this.offset = offsets[1];
                } else {
                    this.offset += offsets[0];
                }
            }
            return this;
        }

        public String toString() {
            return this.current.toString();
        }

        public int getOffset() {
            return this.offset;
        }

        private boolean offsetRecorded() {
            return this.offset >= 0;
        }
    }

    @Deprecated
    public enum Style {
        STANDARD("standard"),
        DURATION("unit"),
        DURATION_SHORT("unit-short"),
        DURATION_NARROW("unit-narrow");
        
        private final String name;

        private Style(String name) {
            this.name = name;
        }

        @Deprecated
        public String getName() {
            return this.name;
        }
    }

    @Deprecated
    public ListFormatter(String two, String start, String middle, String end) {
        this(compilePattern(two, new StringBuilder()), compilePattern(start, new StringBuilder()), compilePattern(middle, new StringBuilder()), compilePattern(end, new StringBuilder()), null);
    }

    private ListFormatter(String two, String start, String middle, String end, ULocale locale) {
        this.two = two;
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.locale = locale;
    }

    private static String compilePattern(String pattern, StringBuilder sb) {
        return SimpleFormatterImpl.compileToStringMinMaxArguments(pattern, sb, 2, 2);
    }

    public static ListFormatter getInstance(ULocale locale) {
        return getInstance(locale, Style.STANDARD);
    }

    public static ListFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), Style.STANDARD);
    }

    @Deprecated
    public static ListFormatter getInstance(ULocale locale, Style style) {
        return cache.get(locale, style.getName());
    }

    public static ListFormatter getInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT));
    }

    public String format(Object... items) {
        return format(Arrays.asList(items));
    }

    public String format(Collection<?> items) {
        return format(items, -1).toString();
    }

    FormattedListBuilder format(Collection<?> items, int index) {
        Iterator<?> it = items.iterator();
        int count = items.size();
        boolean z = false;
        FormattedListBuilder builder;
        String str;
        Object next;
        switch (count) {
            case 0:
                return new FormattedListBuilder("", false);
            case 1:
                Object next2 = it.next();
                if (index == 0) {
                    z = true;
                }
                return new FormattedListBuilder(next2, z);
            case 2:
                builder = new FormattedListBuilder(it.next(), index == 0);
                str = this.two;
                next = it.next();
                if (index == 1) {
                    z = true;
                }
                return builder.append(str, next, z);
            default:
                builder = new FormattedListBuilder(it.next(), index == 0);
                builder.append(this.start, it.next(), index == 1);
                int idx = 2;
                while (idx < count - 1) {
                    builder.append(this.middle, it.next(), index == idx);
                    idx++;
                }
                str = this.end;
                next = it.next();
                if (index == count - 1) {
                    z = true;
                }
                return builder.append(str, next, z);
        }
    }

    public String getPatternForNumItems(int count) {
        if (count > 0) {
            Collection list = new ArrayList();
            for (int i = 0; i < count; i++) {
                list.add(String.format("{%d}", new Object[]{Integer.valueOf(i)}));
            }
            return format(list);
        }
        throw new IllegalArgumentException("count must be > 0");
    }

    @Deprecated
    public ULocale getLocale() {
        return this.locale;
    }
}
