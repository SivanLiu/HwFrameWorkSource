package java.time.format;

import android.icu.impl.ZoneMeta;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.TimeZoneNames;
import android.icu.text.TimeZoneNames.NameType;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Types;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneRulesProvider;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.LanguageTag;

public final class DateTimeFormatterBuilder {
    private static final Map<Character, TemporalField> FIELD_MAP = new HashMap();
    static final Comparator<String> LENGTH_SORT = new Comparator<String>() {
        public int compare(String str1, String str2) {
            return str1.length() == str2.length() ? str1.compareTo(str2) : str1.length() - str2.length();
        }
    };
    private static final TemporalQuery<ZoneId> QUERY_REGION_ONLY = -$$Lambda$DateTimeFormatterBuilder$M-GACNxm6552EiylPRPw4dyNXKo.INSTANCE;
    private DateTimeFormatterBuilder active;
    private final boolean optional;
    private char padNextChar;
    private int padNextWidth;
    private final DateTimeFormatterBuilder parent;
    private final List<DateTimePrinterParser> printerParsers;
    private int valueParserIndex;

    interface DateTimePrinterParser {
        boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder stringBuilder);

        int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i);
    }

    static class PrefixTree {
        protected char c0;
        protected PrefixTree child;
        protected String key;
        protected PrefixTree sibling;
        protected String value;

        private static class CI extends PrefixTree {
            /* synthetic */ CI(String x0, String x1, PrefixTree x2, AnonymousClass1 x3) {
                this(x0, x1, x2);
            }

            private CI(String k, String v, PrefixTree child) {
                super(k, v, child, null);
            }

            protected CI newNode(String k, String v, PrefixTree child) {
                return new CI(k, v, child);
            }

            protected boolean isEqual(char c1, char c2) {
                return DateTimeParseContext.charEqualsIgnoreCase(c1, c2);
            }

            protected boolean prefixOf(CharSequence text, int off, int end) {
                int len = this.key.length();
                if (len > end - off) {
                    return false;
                }
                int off2 = off;
                off = 0;
                while (true) {
                    int len2 = len - 1;
                    if (len <= 0) {
                        return true;
                    }
                    int off0 = off + 1;
                    len = off2 + 1;
                    if (!isEqual(this.key.charAt(off), text.charAt(off2))) {
                        return false;
                    }
                    off2 = len;
                    len = len2;
                    off = off0;
                }
            }
        }

        private static class LENIENT extends CI {
            private LENIENT(String k, String v, PrefixTree child) {
                super(k, v, child, null);
            }

            protected CI newNode(String k, String v, PrefixTree child) {
                return new LENIENT(k, v, child);
            }

            private boolean isLenientChar(char c) {
                return c == ' ' || c == '_' || c == '/';
            }

            protected String toKey(String k) {
                int i = 0;
                while (i < k.length()) {
                    if (isLenientChar(k.charAt(i))) {
                        StringBuilder sb = new StringBuilder(k.length());
                        sb.append((CharSequence) k, 0, i);
                        while (true) {
                            i++;
                            if (i >= k.length()) {
                                return sb.toString();
                            }
                            if (!isLenientChar(k.charAt(i))) {
                                sb.append(k.charAt(i));
                            }
                        }
                    } else {
                        i++;
                    }
                }
                return k;
            }

            public String match(CharSequence text, ParsePosition pos) {
                int off = pos.getIndex();
                int end = text.length();
                int len = this.key.length();
                int koff = 0;
                while (koff < len && off < end) {
                    if (isLenientChar(text.charAt(off))) {
                        off++;
                    } else {
                        int koff2 = koff + 1;
                        int off2 = off + 1;
                        if (!isEqual(this.key.charAt(koff), text.charAt(off))) {
                            return null;
                        }
                        off = off2;
                        koff = koff2;
                    }
                }
                if (koff != len) {
                    return null;
                }
                if (this.child != null && off != end) {
                    int off0 = off;
                    while (off0 < end && isLenientChar(text.charAt(off0))) {
                        off0++;
                    }
                    if (off0 < end) {
                        PrefixTree c = this.child;
                        while (!isEqual(c.c0, text.charAt(off0))) {
                            c = c.sibling;
                            if (c == null) {
                                break;
                            }
                        }
                        pos.setIndex(off0);
                        String found = c.match(text, pos);
                        if (found != null) {
                            return found;
                        }
                    }
                }
                pos.setIndex(off);
                return this.value;
            }
        }

        /* synthetic */ PrefixTree(String x0, String x1, PrefixTree x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private PrefixTree(String k, String v, PrefixTree child) {
            this.key = k;
            this.value = v;
            this.child = child;
            if (k.length() == 0) {
                this.c0 = 65535;
            } else {
                this.c0 = this.key.charAt(0);
            }
        }

        public static PrefixTree newTree(DateTimeParseContext context) {
            if (context.isCaseSensitive()) {
                return new PrefixTree("", null, null);
            }
            return new CI("", null, null, null);
        }

        public static PrefixTree newTree(Set<String> keys, DateTimeParseContext context) {
            PrefixTree tree = newTree(context);
            for (String k : keys) {
                tree.add0(k, k);
            }
            return tree;
        }

        public PrefixTree copyTree() {
            PrefixTree copy = new PrefixTree(this.key, this.value, null);
            if (this.child != null) {
                copy.child = this.child.copyTree();
            }
            if (this.sibling != null) {
                copy.sibling = this.sibling.copyTree();
            }
            return copy;
        }

        public boolean add(String k, String v) {
            return add0(k, v);
        }

        private boolean add0(String k, String v) {
            k = toKey(k);
            int prefixLen = prefixLength(k);
            if (prefixLen != this.key.length()) {
                PrefixTree n1 = newNode(this.key.substring(prefixLen), this.value, this.child);
                this.key = k.substring(0, prefixLen);
                this.child = n1;
                if (prefixLen < k.length()) {
                    this.child.sibling = newNode(k.substring(prefixLen), v, null);
                    this.value = null;
                } else {
                    this.value = v;
                }
                return true;
            } else if (prefixLen < k.length()) {
                String subKey = k.substring(prefixLen);
                for (PrefixTree c = this.child; c != null; c = c.sibling) {
                    if (isEqual(c.c0, subKey.charAt(0))) {
                        return c.add0(subKey, v);
                    }
                }
                PrefixTree c2 = newNode(subKey, v, null);
                c2.sibling = this.child;
                this.child = c2;
                return true;
            } else {
                this.value = v;
                return true;
            }
        }

        public String match(CharSequence text, int off, int end) {
            if (!prefixOf(text, off, end)) {
                return null;
            }
            if (this.child != null) {
                int length = this.key.length() + off;
                off = length;
                if (length != end) {
                    PrefixTree c = this.child;
                    while (!isEqual(c.c0, text.charAt(off))) {
                        c = c.sibling;
                        if (c == null) {
                        }
                    }
                    String found = c.match(text, off, end);
                    if (found != null) {
                        return found;
                    }
                    return this.value;
                }
            }
            return this.value;
        }

        public String match(CharSequence text, ParsePosition pos) {
            int off = pos.getIndex();
            int end = text.length();
            if (!prefixOf(text, off, end)) {
                return null;
            }
            off += this.key.length();
            if (this.child != null && off != end) {
                PrefixTree c = this.child;
                while (!isEqual(c.c0, text.charAt(off))) {
                    c = c.sibling;
                    if (c == null) {
                        break;
                    }
                }
                pos.setIndex(off);
                String found = c.match(text, pos);
                if (found != null) {
                    return found;
                }
            }
            pos.setIndex(off);
            return this.value;
        }

        protected String toKey(String k) {
            return k;
        }

        protected PrefixTree newNode(String k, String v, PrefixTree child) {
            return new PrefixTree(k, v, child);
        }

        protected boolean isEqual(char c1, char c2) {
            return c1 == c2;
        }

        protected boolean prefixOf(CharSequence text, int off, int end) {
            if (text instanceof String) {
                return ((String) text).startsWith(this.key, off);
            }
            int len = this.key.length();
            if (len > end - off) {
                return false;
            }
            int off2 = off;
            off = 0;
            while (true) {
                int len2 = len - 1;
                if (len <= 0) {
                    return true;
                }
                int off0 = off + 1;
                len = off2 + 1;
                if (!isEqual(this.key.charAt(off), text.charAt(off2))) {
                    return false;
                }
                off2 = len;
                len = len2;
                off = off0;
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0023, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int prefixLength(String k) {
            int off = 0;
            while (off < k.length() && off < this.key.length() && isEqual(k.charAt(off), this.key.charAt(off))) {
                off++;
            }
            return off;
        }
    }

    static final class CharLiteralPrinterParser implements DateTimePrinterParser {
        private final char literal;

        CharLiteralPrinterParser(char literal) {
            this.literal = literal;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            buf.append(this.literal);
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            if (position == text.length()) {
                return ~position;
            }
            char ch = text.charAt(position);
            if (ch == this.literal || (!context.isCaseSensitive() && (Character.toUpperCase(ch) == Character.toUpperCase(this.literal) || Character.toLowerCase(ch) == Character.toLowerCase(this.literal)))) {
                return position + 1;
            }
            return ~position;
        }

        public String toString() {
            if (this.literal == '\'') {
                return "''";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append(this.literal);
            stringBuilder.append("'");
            return stringBuilder.toString();
        }
    }

    static final class ChronoPrinterParser implements DateTimePrinterParser {
        private final TextStyle textStyle;

        ChronoPrinterParser(TextStyle textStyle) {
            this.textStyle = textStyle;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Chronology chrono = (Chronology) context.getValue(TemporalQueries.chronology());
            if (chrono == null) {
                return false;
            }
            if (this.textStyle == null) {
                buf.append(chrono.getId());
            } else {
                buf.append(getChronologyName(chrono, context.getLocale()));
            }
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int i = position;
            if (i < 0 || i > text.length()) {
                DateTimeParseContext dateTimeParseContext = context;
                throw new IndexOutOfBoundsException();
            }
            Chronology bestMatch = null;
            int matchLen = -1;
            for (Chronology chrono : Chronology.getAvailableChronologies()) {
                String name;
                if (this.textStyle == null) {
                    name = chrono.getId();
                } else {
                    name = getChronologyName(chrono, context.getLocale());
                }
                String name2 = name;
                int nameLen = name2.length();
                if (nameLen > matchLen && context.subSequenceEquals(text, i, name2, 0, nameLen)) {
                    bestMatch = chrono;
                    matchLen = nameLen;
                }
            }
            if (bestMatch == null) {
                return ~i;
            }
            context.setParsed(bestMatch);
            return i + matchLen;
        }

        private String getChronologyName(Chronology chrono, Locale locale) {
            String name = LocaleDisplayNames.getInstance(ULocale.forLocale(locale)).keyValueDisplayName("calendar", chrono.getCalendarType());
            return name != null ? name : chrono.getId();
        }
    }

    static final class CompositePrinterParser implements DateTimePrinterParser {
        private final boolean optional;
        private final DateTimePrinterParser[] printerParsers;

        CompositePrinterParser(List<DateTimePrinterParser> printerParsers, boolean optional) {
            this((DateTimePrinterParser[]) printerParsers.toArray(new DateTimePrinterParser[printerParsers.size()]), optional);
        }

        CompositePrinterParser(DateTimePrinterParser[] printerParsers, boolean optional) {
            this.printerParsers = printerParsers;
            this.optional = optional;
        }

        public CompositePrinterParser withOptional(boolean optional) {
            if (optional == this.optional) {
                return this;
            }
            return new CompositePrinterParser(this.printerParsers, optional);
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            int length = buf.length();
            if (this.optional) {
                context.startOptional();
            }
            try {
                DateTimePrinterParser[] dateTimePrinterParserArr = this.printerParsers;
                int length2 = dateTimePrinterParserArr.length;
                int i = 0;
                while (i < length2) {
                    if (dateTimePrinterParserArr[i].format(context, buf)) {
                        i++;
                    } else {
                        buf.setLength(length);
                        return true;
                    }
                }
                if (this.optional) {
                    context.endOptional();
                }
                return true;
            } finally {
                if (this.optional) {
                    context.endOptional();
                }
            }
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int i = 0;
            if (this.optional) {
                context.startOptional();
                int pos = position;
                int pos2 = pos;
                for (DateTimePrinterParser pp : this.printerParsers) {
                    pos2 = pp.parse(context, text, pos2);
                    if (pos2 < 0) {
                        context.endOptional(false);
                        return position;
                    }
                }
                context.endOptional(true);
                return pos2;
            }
            DateTimePrinterParser[] dateTimePrinterParserArr = this.printerParsers;
            int length = dateTimePrinterParserArr.length;
            while (i < length) {
                position = dateTimePrinterParserArr[i].parse(context, text, position);
                if (position < 0) {
                    break;
                }
                i++;
            }
            return position;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            if (this.printerParsers != null) {
                buf.append(this.optional ? "[" : "(");
                for (Object pp : this.printerParsers) {
                    buf.append(pp);
                }
                buf.append(this.optional ? "]" : ")");
            }
            return buf.toString();
        }
    }

    static class DefaultValueParser implements DateTimePrinterParser {
        private final TemporalField field;
        private final long value;

        DefaultValueParser(TemporalField field, long value) {
            this.field = field;
            this.value = value;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            if (context.getParsed(this.field) == null) {
                context.setParsedField(this.field, this.value, position, position);
            }
            return position;
        }
    }

    static final class FractionPrinterParser implements DateTimePrinterParser {
        private final boolean decimalPoint;
        private final TemporalField field;
        private final int maxWidth;
        private final int minWidth;

        FractionPrinterParser(TemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {
            Objects.requireNonNull((Object) field, "field");
            StringBuilder stringBuilder;
            if (!field.range().isFixed()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Field must have a fixed set of values: ");
                stringBuilder.append((Object) field);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (minWidth < 0 || minWidth > 9) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Minimum width must be from 0 to 9 inclusive but was ");
                stringBuilder.append(minWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (maxWidth < 1 || maxWidth > 9) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Maximum width must be from 1 to 9 inclusive but was ");
                stringBuilder.append(maxWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (maxWidth >= minWidth) {
                this.field = field;
                this.minWidth = minWidth;
                this.maxWidth = maxWidth;
                this.decimalPoint = decimalPoint;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Maximum width must exceed or equal the minimum width but ");
                stringBuilder.append(maxWidth);
                stringBuilder.append(" < ");
                stringBuilder.append(minWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long value = context.getValue(this.field);
            int i = 0;
            if (value == null) {
                return false;
            }
            DecimalStyle decimalStyle = context.getDecimalStyle();
            BigDecimal fraction = convertToFraction(value.longValue());
            if (fraction.scale() != 0) {
                String str = decimalStyle.convertNumberToI18N(fraction.setScale(Math.min(Math.max(fraction.scale(), this.minWidth), this.maxWidth), RoundingMode.FLOOR).toPlainString().substring(2));
                if (this.decimalPoint) {
                    buf.append(decimalStyle.getDecimalSeparator());
                }
                buf.append(str);
            } else if (this.minWidth > 0) {
                if (this.decimalPoint) {
                    buf.append(decimalStyle.getDecimalSeparator());
                }
                while (i < this.minWidth) {
                    buf.append(decimalStyle.getZeroDigit());
                    i++;
                }
            }
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int position2 = position;
            int effectiveMin = context.isStrict() ? this.minWidth : 0;
            int effectiveMax = context.isStrict() ? this.maxWidth : 9;
            int length = text.length();
            if (position2 == length) {
                return effectiveMin > 0 ? ~position2 : position2;
            }
            if (this.decimalPoint) {
                if (text.charAt(position) != context.getDecimalStyle().getDecimalSeparator()) {
                    return effectiveMin > 0 ? ~position2 : position2;
                }
                position2++;
            }
            int minEndPos = position2 + effectiveMin;
            if (minEndPos > length) {
                return ~position2;
            }
            int pos;
            BigDecimal fraction;
            int maxEndPos = Math.min(position2 + effectiveMax, length);
            int total = 0;
            int pos2 = position2;
            while (pos2 < maxEndPos) {
                int pos3 = pos2 + 1;
                int digit = context.getDecimalStyle().convertToDigit(text.charAt(pos2));
                if (digit >= 0) {
                    total = (total * 10) + digit;
                    pos2 = pos3;
                } else if (pos3 < minEndPos) {
                    return ~position2;
                } else {
                    pos = pos3 - 1;
                    fraction = new BigDecimal(total).movePointLeft(pos - position2);
                    return context.setParsedField(this.field, convertFromFraction(fraction), position2, pos);
                }
            }
            CharSequence charSequence = text;
            pos = pos2;
            fraction = new BigDecimal(total).movePointLeft(pos - position2);
            return context.setParsedField(this.field, convertFromFraction(fraction), position2, pos);
        }

        private BigDecimal convertToFraction(long value) {
            ValueRange range = this.field.range();
            range.checkValidValue(value, this.field);
            BigDecimal minBD = BigDecimal.valueOf(range.getMinimum());
            BigDecimal fraction = BigDecimal.valueOf(value).subtract(minBD).divide(BigDecimal.valueOf(range.getMaximum()).subtract(minBD).add(BigDecimal.ONE), 9, RoundingMode.FLOOR);
            return fraction.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : fraction.stripTrailingZeros();
        }

        private long convertFromFraction(BigDecimal fraction) {
            ValueRange range = this.field.range();
            BigDecimal minBD = BigDecimal.valueOf(range.getMinimum());
            return fraction.multiply(BigDecimal.valueOf(range.getMaximum()).subtract(minBD).add(BigDecimal.ONE)).setScale(0, RoundingMode.FLOOR).add(minBD).longValueExact();
        }

        public String toString() {
            String decimal = this.decimalPoint ? ",DecimalPoint" : "";
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fraction(");
            stringBuilder.append(this.field);
            stringBuilder.append(",");
            stringBuilder.append(this.minWidth);
            stringBuilder.append(",");
            stringBuilder.append(this.maxWidth);
            stringBuilder.append(decimal);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    static final class InstantPrinterParser implements DateTimePrinterParser {
        private static final long SECONDS_0000_TO_1970 = 62167219200L;
        private static final long SECONDS_PER_10000_YEARS = 315569520000L;
        private final int fractionalDigits;

        InstantPrinterParser(int fractionalDigits) {
            this.fractionalDigits = fractionalDigits;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            StringBuilder stringBuilder = buf;
            Long inSecs = context.getValue(ChronoField.INSTANT_SECONDS);
            Long inNanos = null;
            if (context.getTemporal().isSupported(ChronoField.NANO_OF_SECOND)) {
                inNanos = Long.valueOf(context.getTemporal().getLong(ChronoField.NANO_OF_SECOND));
            }
            if (inSecs == null) {
                return false;
            }
            int div;
            int i;
            int i2;
            int digit;
            long inSec = inSecs.longValue();
            int inNano = ChronoField.NANO_OF_SECOND.checkValidIntValue(inNanos != null ? inNanos.longValue() : 0);
            if (inSec >= -62167219200L) {
                long zeroSecs = (inSec - SECONDS_PER_10000_YEARS) + SECONDS_0000_TO_1970;
                long hi = Math.floorDiv(zeroSecs, (long) SECONDS_PER_10000_YEARS) + 1;
                Object ldt = LocalDateTime.ofEpochSecond(Math.floorMod(zeroSecs, (long) SECONDS_PER_10000_YEARS) - SECONDS_0000_TO_1970, 0, ZoneOffset.UTC);
                if (hi > 0) {
                    stringBuilder.append('+');
                    stringBuilder.append(hi);
                }
                stringBuilder.append(ldt);
                if (ldt.getSecond() == 0) {
                    stringBuilder.append(":00");
                }
            } else {
                inSec += SECONDS_0000_TO_1970;
                long hi2 = inSec / SECONDS_PER_10000_YEARS;
                long lo = inSec % SECONDS_PER_10000_YEARS;
                Object ldt2 = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, 0, ZoneOffset.UTC);
                int pos = buf.length();
                stringBuilder.append(ldt2);
                if (ldt2.getSecond() == 0) {
                    stringBuilder.append(":00");
                }
                Long l;
                if (hi2 >= 0) {
                    l = inNanos;
                } else if (ldt2.getYear() == -10000) {
                    Long l2 = inSecs;
                    stringBuilder.replace(pos, pos + 2, Long.toString(hi2 - 1));
                } else {
                    if (lo == 0) {
                        stringBuilder.insert(pos, hi2);
                    } else {
                        l = inNanos;
                        stringBuilder.insert(pos + 1, Math.abs(hi2));
                    }
                }
                if ((this.fractionalDigits < 0 && inNano > 0) || this.fractionalDigits > 0) {
                    stringBuilder.append('.');
                    div = 100000000;
                    i = 0;
                    while (true) {
                        i2 = i;
                        if ((this.fractionalDigits == -1 || inNano <= 0) && ((this.fractionalDigits != -2 || (inNano <= 0 && i2 % 3 == 0)) && i2 >= this.fractionalDigits)) {
                            break;
                        }
                        digit = inNano / div;
                        stringBuilder.append((char) (digit + 48));
                        inNano -= digit * div;
                        div /= 10;
                        i = i2 + 1;
                    }
                }
                stringBuilder.append('Z');
                return true;
            }
            stringBuilder.append('.');
            div = 100000000;
            i = 0;
            while (true) {
                i2 = i;
                if (this.fractionalDigits == -1) {
                }
                digit = inNano / div;
                stringBuilder.append((char) (digit + 48));
                inNano -= digit * div;
                div /= 10;
                i = i2 + 1;
            }
            stringBuilder.append('Z');
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int min;
            Long nanoVal;
            Long secVal;
            long yearParsed;
            Long l;
            long j;
            int i;
            Long l2;
            int i2 = position;
            int nano = 0;
            int minDigits = this.fractionalDigits < 0 ? 0 : this.fractionalDigits;
            CompositePrinterParser parser = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendFraction(ChronoField.NANO_OF_SECOND, minDigits, this.fractionalDigits < 0 ? 9 : this.fractionalDigits, true).appendLiteral('Z').toFormatter().toPrinterParser(false);
            DateTimeParseContext newContext = context.copy();
            int pos = parser.parse(newContext, text, i2);
            if (pos < 0) {
                return pos;
            }
            int sec;
            int hour;
            int nano2;
            long instantSecs;
            DateTimeParseContext dateTimeParseContext;
            long yearParsed2 = newContext.getParsed(ChronoField.YEAR).longValue();
            int month = newContext.getParsed(ChronoField.MONTH_OF_YEAR).intValue();
            int day = newContext.getParsed(ChronoField.DAY_OF_MONTH).intValue();
            int hour2 = newContext.getParsed(ChronoField.HOUR_OF_DAY).intValue();
            int min2 = newContext.getParsed(ChronoField.MINUTE_OF_HOUR).intValue();
            Long secVal2 = newContext.getParsed(ChronoField.SECOND_OF_MINUTE);
            Long nanoVal2 = newContext.getParsed(ChronoField.NANO_OF_SECOND);
            int days = secVal2 != null ? secVal2.intValue() : 0;
            if (nanoVal2 != null) {
                nano = nanoVal2.intValue();
            }
            int days2 = 0;
            if (hour2 == 24 && min2 == 0 && days == 0 && nano == 0) {
                hour2 = 0;
                days2 = 1;
            } else if (hour2 == 23 && min2 == 59 && days == 60) {
                context.setParsedLeapSecond();
                sec = 59;
                days = 0;
                hour = hour2;
                nano2 = nano;
                try {
                    min = min2;
                    nanoVal = nanoVal2;
                    try {
                        secVal = secVal2;
                        yearParsed = yearParsed2;
                        instantSecs = LocalDateTime.of(((int) yearParsed2) % 10000, month, day, hour, min2, sec, 0).plusDays((long) days).toEpochSecond(ZoneOffset.UTC) + Math.multiplyExact(yearParsed2 / 10000, (long) 2036907392);
                        dateTimeParseContext = context;
                        min2 = i2;
                        return dateTimeParseContext.setParsedField(ChronoField.NANO_OF_SECOND, (long) nano2, min2, dateTimeParseContext.setParsedField(ChronoField.INSTANT_SECONDS, instantSecs, min2, pos));
                    } catch (RuntimeException e) {
                        l = secVal2;
                        j = yearParsed2;
                        hour = nano2;
                        i = min;
                        l2 = nanoVal;
                        return ~i2;
                    }
                } catch (RuntimeException e2) {
                    l = secVal2;
                    j = yearParsed2;
                    i = min2;
                    l2 = nanoVal2;
                    hour = nano2;
                    return ~i2;
                }
            }
            hour = hour2;
            sec = days;
            days = days2;
            try {
                nano2 = nano;
                min = min2;
                nanoVal = nanoVal2;
                secVal = secVal2;
                yearParsed = yearParsed2;
                try {
                    instantSecs = LocalDateTime.of(((int) yearParsed2) % 10000, month, day, hour, min2, sec, 0).plusDays((long) days).toEpochSecond(ZoneOffset.UTC) + Math.multiplyExact(yearParsed2 / 10000, (long) 2036907392);
                    dateTimeParseContext = context;
                    min2 = i2;
                    return dateTimeParseContext.setParsedField(ChronoField.NANO_OF_SECOND, (long) nano2, min2, dateTimeParseContext.setParsedField(ChronoField.INSTANT_SECONDS, instantSecs, min2, pos));
                } catch (RuntimeException e3) {
                    hour = nano2;
                    i = min;
                    l2 = nanoVal;
                    j = yearParsed;
                    l = secVal;
                    return ~i2;
                }
            } catch (RuntimeException e4) {
                int i3 = hour;
                l = secVal2;
                j = yearParsed2;
                i = min2;
                l2 = nanoVal2;
                return ~i2;
            }
        }

        public String toString() {
            return "Instant()";
        }
    }

    static final class LocalizedOffsetIdPrinterParser implements DateTimePrinterParser {
        private final TextStyle style;

        LocalizedOffsetIdPrinterParser(TextStyle style) {
            this.style = style;
        }

        private static StringBuilder appendHMS(StringBuilder buf, int t) {
            buf.append((char) ((t / 10) + 48));
            buf.append((char) ((t % 10) + 48));
            return buf;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long offsetSecs = context.getValue(ChronoField.OFFSET_SECONDS);
            if (offsetSecs == null) {
                return false;
            }
            buf.append("GMT");
            int totalSecs = Math.toIntExact(offsetSecs.longValue());
            if (totalSecs != 0) {
                int absHours = Math.abs((totalSecs / 3600) % 100);
                int absMinutes = Math.abs((totalSecs / 60) % 60);
                int absSeconds = Math.abs(totalSecs % 60);
                buf.append(totalSecs < 0 ? LanguageTag.SEP : "+");
                if (this.style == TextStyle.FULL) {
                    appendHMS(buf, absHours);
                    buf.append(':');
                    appendHMS(buf, absMinutes);
                    if (absSeconds != 0) {
                        buf.append(':');
                        appendHMS(buf, absSeconds);
                    }
                } else {
                    if (absHours >= 10) {
                        buf.append((char) ((absHours / 10) + 48));
                    }
                    buf.append((char) ((absHours % 10) + 48));
                    if (!(absMinutes == 0 && absSeconds == 0)) {
                        buf.append(':');
                        appendHMS(buf, absMinutes);
                        if (absSeconds != 0) {
                            buf.append(':');
                            appendHMS(buf, absSeconds);
                        }
                    }
                }
            }
            return true;
        }

        int getDigit(CharSequence text, int position) {
            char c = text.charAt(position);
            if (c < '0' || c > '9') {
                return -1;
            }
            return c - 48;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            CharSequence charSequence = text;
            int i = position;
            int pos = i;
            int end = pos + text.length();
            String gmtText = "GMT";
            if (!context.subSequenceEquals(charSequence, pos, gmtText, 0, gmtText.length())) {
                return ~i;
            }
            pos += gmtText.length();
            if (pos == end) {
                return context.setParsedField(ChronoField.OFFSET_SECONDS, 0, i, pos);
            }
            int negative;
            int m1;
            int pos2;
            int pos3;
            char sign = charSequence.charAt(pos);
            if (sign == '+') {
                negative = 1;
            } else if (sign == '-') {
                negative = -1;
            } else {
                return context.setParsedField(ChronoField.OFFSET_SECONDS, 0, i, pos);
            }
            int negative2 = negative;
            pos++;
            int m = 0;
            int s = 0;
            int h2;
            int h1;
            int s2;
            if (this.style == TextStyle.FULL) {
                h2 = pos + 1;
                h1 = getDigit(charSequence, pos);
                pos = h2 + 1;
                h2 = getDigit(charSequence, h2);
                if (h1 < 0 || h2 < 0) {
                } else {
                    m1 = pos + 1;
                    if (charSequence.charAt(pos) == 58) {
                        pos = (h1 * 10) + h2;
                        negative = m1 + 1;
                        m1 = getDigit(charSequence, m1);
                        pos2 = negative + 1;
                        negative = getDigit(charSequence, negative);
                        int i2;
                        if (m1 < 0) {
                            i2 = negative;
                        } else if (negative < 0) {
                            i2 = negative;
                        } else {
                            m = (m1 * 10) + negative;
                            if (pos2 + 2 < end) {
                                if (charSequence.charAt(pos2) == ':') {
                                    negative = getDigit(charSequence, pos2 + 1);
                                    s2 = getDigit(charSequence, pos2 + 2);
                                    if (negative >= 0 && s2 >= 0) {
                                        s = (negative * 10) + s2;
                                        pos2 += 3;
                                    }
                                }
                            }
                            m1 = m;
                            pos3 = pos2;
                            pos2 = s;
                        }
                        return ~i;
                    }
                }
                return ~i;
            }
            h2 = pos + 1;
            pos = getDigit(charSequence, pos);
            if (pos < 0) {
                return ~i;
            }
            if (h2 < end) {
                negative = getDigit(charSequence, h2);
                if (negative >= 0) {
                    h2++;
                    pos = (pos * 10) + negative;
                }
                if (h2 + 2 < end && charSequence.charAt(h2) == ':' && h2 + 2 < end && charSequence.charAt(h2) == ':') {
                    h1 = getDigit(charSequence, h2 + 1);
                    s2 = getDigit(charSequence, h2 + 2);
                    if (h1 >= 0 && s2 >= 0) {
                        m = (h1 * 10) + s2;
                        h2 += 3;
                        if (h2 + 2 < end && charSequence.charAt(h2) == ':') {
                            m1 = getDigit(charSequence, h2 + 1);
                            pos2 = getDigit(charSequence, h2 + 2);
                            if (m1 >= 0 && pos2 >= 0) {
                                s = (m1 * 10) + pos2;
                                h2 += 3;
                            }
                        }
                    }
                }
            }
            m1 = m;
            pos2 = s;
            pos3 = h2;
            return context.setParsedField(ChronoField.OFFSET_SECONDS, ((long) negative2) * (((((long) pos) * 3600) + (((long) m1) * 60)) + ((long) pos2)), i, pos3);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LocalizedOffset(");
            stringBuilder.append(this.style);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    static final class LocalizedPrinterParser implements DateTimePrinterParser {
        private static final ConcurrentMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap(16, 0.75f, 2);
        private final FormatStyle dateStyle;
        private final FormatStyle timeStyle;

        LocalizedPrinterParser(FormatStyle dateStyle, FormatStyle timeStyle) {
            this.dateStyle = dateStyle;
            this.timeStyle = timeStyle;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return formatter(context.getLocale(), Chronology.from(context.getTemporal())).toPrinterParser(false).format(context, buf);
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            return formatter(context.getLocale(), context.getEffectiveChronology()).toPrinterParser(false).parse(context, text, position);
        }

        private DateTimeFormatter formatter(Locale locale, Chronology chrono) {
            String key = new StringBuilder();
            key.append(chrono.getId());
            key.append('|');
            key.append(locale.toString());
            key.append('|');
            key.append(this.dateStyle);
            key.append(this.timeStyle);
            key = key.toString();
            DateTimeFormatter formatter = (DateTimeFormatter) FORMATTER_CACHE.get(key);
            if (formatter != null) {
                return formatter;
            }
            formatter = new DateTimeFormatterBuilder().appendPattern(DateTimeFormatterBuilder.getLocalizedDateTimePattern(this.dateStyle, this.timeStyle, chrono, locale)).toFormatter(locale);
            DateTimeFormatter old = (DateTimeFormatter) FORMATTER_CACHE.putIfAbsent(key, formatter);
            if (old != null) {
                return old;
            }
            return formatter;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Localized(");
            stringBuilder.append(this.dateStyle != null ? this.dateStyle : "");
            stringBuilder.append(",");
            stringBuilder.append(this.timeStyle != null ? this.timeStyle : "");
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    static class NumberPrinterParser implements DateTimePrinterParser {
        static final long[] EXCEED_POINTS = new long[]{0, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L};
        final TemporalField field;
        final int maxWidth;
        final int minWidth;
        private final SignStyle signStyle;
        final int subsequentWidth;

        NumberPrinterParser(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle) {
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.signStyle = signStyle;
            this.subsequentWidth = 0;
        }

        protected NumberPrinterParser(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle, int subsequentWidth) {
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.signStyle = signStyle;
            this.subsequentWidth = subsequentWidth;
        }

        NumberPrinterParser withFixedWidth() {
            if (this.subsequentWidth == -1) {
                return this;
            }
            return new NumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle, -1);
        }

        NumberPrinterParser withSubsequentWidth(int subsequentWidth) {
            return new NumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle, this.subsequentWidth + subsequentWidth);
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long valueLong = context.getValue(this.field);
            int i = 0;
            if (valueLong == null) {
                return false;
            }
            long value = getValue(context, valueLong.longValue());
            DecimalStyle decimalStyle = context.getDecimalStyle();
            String str = value == Long.MIN_VALUE ? "9223372036854775808" : Long.toString(Math.abs(value));
            StringBuilder stringBuilder;
            if (str.length() <= this.maxWidth) {
                str = decimalStyle.convertNumberToI18N(str);
                if (value >= 0) {
                    switch (this.signStyle) {
                        case EXCEEDS_PAD:
                            if (this.minWidth < 19 && value >= EXCEED_POINTS[this.minWidth]) {
                                buf.append(decimalStyle.getPositiveSign());
                                break;
                            }
                        case ALWAYS:
                            buf.append(decimalStyle.getPositiveSign());
                            break;
                    }
                }
                switch (this.signStyle) {
                    case EXCEEDS_PAD:
                    case ALWAYS:
                    case NORMAL:
                        buf.append(decimalStyle.getNegativeSign());
                        break;
                    case NOT_NEGATIVE:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Field ");
                        stringBuilder.append(this.field);
                        stringBuilder.append(" cannot be printed as the value ");
                        stringBuilder.append(value);
                        stringBuilder.append(" cannot be negative according to the SignStyle");
                        throw new DateTimeException(stringBuilder.toString());
                }
                while (i < this.minWidth - str.length()) {
                    buf.append(decimalStyle.getZeroDigit());
                    i++;
                }
                buf.append(str);
                return true;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Field ");
            stringBuilder.append(this.field);
            stringBuilder.append(" cannot be printed as the value ");
            stringBuilder.append(value);
            stringBuilder.append(" exceeds the maximum print width of ");
            stringBuilder.append(this.maxWidth);
            throw new DateTimeException(stringBuilder.toString());
        }

        long getValue(DateTimePrintContext context, long value) {
            return value;
        }

        boolean isFixedWidth(DateTimeParseContext context) {
            return this.subsequentWidth == -1 || (this.subsequentWidth > 0 && this.minWidth == this.maxWidth && this.signStyle == SignStyle.NOT_NEGATIVE);
        }

        /* JADX WARNING: Removed duplicated region for block: B:111:0x01a3  */
        /* JADX WARNING: Removed duplicated region for block: B:106:0x0185  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int i = position;
            BigInteger totalBig = text.length();
            if (i == totalBig) {
                return ~i;
            }
            char sign = text.charAt(position);
            boolean negative = false;
            boolean positive = false;
            int pass = 0;
            boolean z = true;
            if (sign == context.getDecimalStyle().getPositiveSign()) {
                if (!this.signStyle.parse(true, context.isStrict(), this.minWidth == this.maxWidth)) {
                    return ~i;
                }
                positive = true;
                i++;
            } else if (sign == context.getDecimalStyle().getNegativeSign()) {
                if (!this.signStyle.parse(false, context.isStrict(), this.minWidth == this.maxWidth)) {
                    return ~i;
                }
                negative = true;
                i++;
            } else if (this.signStyle == SignStyle.ALWAYS && context.isStrict()) {
                return ~i;
            }
            int position2 = i;
            boolean negative2 = negative;
            boolean positive2 = positive;
            if (context.isStrict() || isFixedWidth(context)) {
                z = this.minWidth;
            }
            int effMinWidth = z;
            int minEndPos = position2 + effMinWidth;
            if (minEndPos > totalBig) {
                return ~position2;
            }
            int pos;
            long total;
            i = (context.isStrict() || isFixedWidth(context)) ? this.maxWidth : 9;
            long total2 = 0;
            BigInteger totalBig2 = null;
            int pos2 = position2;
            int effMaxWidth = i + Math.max(this.subsequentWidth, 0);
            while (true) {
                i = pass;
                int length;
                char c;
                if (i >= 2) {
                    length = totalBig;
                    c = sign;
                    pos = pos2;
                    break;
                }
                int maxEndPos;
                long total3;
                pass = Math.min(pos2 + effMaxWidth, (int) totalBig);
                while (pos2 < pass) {
                    pos = pos2 + 1;
                    length = totalBig;
                    char ch = text.charAt(pos2);
                    maxEndPos = pass;
                    pass = context.getDecimalStyle().convertToDigit(ch);
                    if (pass < 0) {
                        pos--;
                        if (pos < minEndPos) {
                            return ~position2;
                        }
                        total3 = total2;
                        c = sign;
                        if (this.subsequentWidth <= 0 || i != 0) {
                            total2 = total3;
                        } else {
                            effMaxWidth = Math.max(effMinWidth, (pos - position2) - this.subsequentWidth);
                            pos2 = position2;
                            totalBig2 = null;
                            pass = i + 1;
                            total2 = null;
                            totalBig = length;
                            sign = c;
                        }
                    } else {
                        char c2 = ch;
                        if (pos - position2 > 18) {
                            if (totalBig2 == null) {
                                totalBig2 = BigInteger.valueOf(total2);
                            }
                            c = sign;
                            totalBig2 = totalBig2.multiply(BigInteger.TEN).add(BigInteger.valueOf((long) pass));
                        } else {
                            c = sign;
                            total2 = ((long) pass) + (10 * total2);
                        }
                        pos2 = pos;
                        totalBig = length;
                        pass = maxEndPos;
                        sign = c;
                    }
                }
                total3 = total2;
                maxEndPos = pass;
                length = totalBig;
                c = sign;
                pos = pos2;
                if (this.subsequentWidth <= 0) {
                    break;
                }
                break;
            }
            if (negative2) {
                if (totalBig2 != null) {
                    if (totalBig2.equals(BigInteger.ZERO) && context.isStrict()) {
                        return ~(position2 - 1);
                    }
                    totalBig2 = totalBig2.negate();
                } else if (total2 == 0 && context.isStrict()) {
                    return ~(position2 - 1);
                } else {
                    total = -total2;
                    totalBig = totalBig2;
                    if (totalBig != null) {
                        return setValue(context, total, position2, pos);
                    }
                    if (totalBig.bitLength() > 63) {
                        totalBig = totalBig.divide(BigInteger.TEN);
                        pos--;
                    }
                    return setValue(context, totalBig.longValue(), position2, pos);
                }
            } else if (this.signStyle == SignStyle.EXCEEDS_PAD && context.isStrict()) {
                i = pos - position2;
                if (positive2) {
                    if (i <= this.minWidth) {
                        return ~(position2 - 1);
                    }
                } else if (i > this.minWidth) {
                    return ~position2;
                }
            }
            total = total2;
            totalBig = totalBig2;
            if (totalBig != null) {
            }
        }

        int setValue(DateTimeParseContext context, long value, int errorPos, int successPos) {
            return context.setParsedField(this.field, value, errorPos, successPos);
        }

        public String toString() {
            StringBuilder stringBuilder;
            if (this.minWidth == 1 && this.maxWidth == 19 && this.signStyle == SignStyle.NORMAL) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Value(");
                stringBuilder.append(this.field);
                stringBuilder.append(")");
                return stringBuilder.toString();
            } else if (this.minWidth == this.maxWidth && this.signStyle == SignStyle.NOT_NEGATIVE) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Value(");
                stringBuilder.append(this.field);
                stringBuilder.append(",");
                stringBuilder.append(this.minWidth);
                stringBuilder.append(")");
                return stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Value(");
                stringBuilder.append(this.field);
                stringBuilder.append(",");
                stringBuilder.append(this.minWidth);
                stringBuilder.append(",");
                stringBuilder.append(this.maxWidth);
                stringBuilder.append(",");
                stringBuilder.append(this.signStyle);
                stringBuilder.append(")");
                return stringBuilder.toString();
            }
        }
    }

    static final class OffsetIdPrinterParser implements DateTimePrinterParser {
        static final OffsetIdPrinterParser INSTANCE_ID_Z = new OffsetIdPrinterParser("+HH:MM:ss", "Z");
        static final OffsetIdPrinterParser INSTANCE_ID_ZERO = new OffsetIdPrinterParser("+HH:MM:ss", "0");
        static final String[] PATTERNS = new String[]{"+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss", "+HHMMSS", "+HH:MM:SS"};
        private final String noOffsetText;
        private final int type;

        OffsetIdPrinterParser(String pattern, String noOffsetText) {
            Objects.requireNonNull((Object) pattern, "pattern");
            Objects.requireNonNull((Object) noOffsetText, "noOffsetText");
            this.type = checkPattern(pattern);
            this.noOffsetText = noOffsetText;
        }

        private int checkPattern(String pattern) {
            for (int i = 0; i < PATTERNS.length; i++) {
                if (PATTERNS[i].equals(pattern)) {
                    return i;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid zone offset pattern: ");
            stringBuilder.append(pattern);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long offsetSecs = context.getValue(ChronoField.OFFSET_SECONDS);
            if (offsetSecs == null) {
                return false;
            }
            int totalSecs = Math.toIntExact(offsetSecs.longValue());
            if (totalSecs == 0) {
                buf.append(this.noOffsetText);
            } else {
                int absHours = Math.abs((totalSecs / 3600) % 100);
                int absMinutes = Math.abs((totalSecs / 60) % 60);
                int absSeconds = Math.abs(totalSecs % 60);
                int bufPos = buf.length();
                int output = absHours;
                buf.append(totalSecs < 0 ? LanguageTag.SEP : "+");
                buf.append((char) ((absHours / 10) + 48));
                buf.append((char) ((absHours % 10) + 48));
                if (this.type >= 3 || (this.type >= 1 && absMinutes > 0)) {
                    buf.append(this.type % 2 == 0 ? ":" : "");
                    buf.append((char) ((absMinutes / 10) + 48));
                    buf.append((char) ((absMinutes % 10) + 48));
                    output += absMinutes;
                    if (this.type >= 7 || (this.type >= 5 && absSeconds > 0)) {
                        buf.append(this.type % 2 == 0 ? ":" : "");
                        buf.append((char) ((absSeconds / 10) + 48));
                        buf.append((char) ((absSeconds % 10) + 48));
                        output += absSeconds;
                    }
                }
                if (output == 0) {
                    buf.setLength(bufPos);
                    buf.append(this.noOffsetText);
                }
            }
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:32:0x007e  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            CharSequence charSequence = text;
            int i = position;
            int length = text.length();
            int noOffsetLen = this.noOffsetText.length();
            if (noOffsetLen == 0) {
                if (i == length) {
                    return context.setParsedField(ChronoField.OFFSET_SECONDS, 0, i, i);
                }
            } else if (i == length) {
                return ~i;
            } else {
                if (context.subSequenceEquals(charSequence, i, this.noOffsetText, 0, noOffsetLen)) {
                    return context.setParsedField(ChronoField.OFFSET_SECONDS, 0, i, i + noOffsetLen);
                }
            }
            char sign = text.charAt(position);
            if (sign == '+' || sign == '-') {
                int i2;
                int negative = sign == '-' ? -1 : 1;
                int[] array = new int[4];
                array[0] = i + 1;
                if (!parseNumber(array, 1, charSequence, true)) {
                    if (!(parseNumber(array, 2, charSequence, this.type >= 3) || parseNumber(array, 3, charSequence, false))) {
                        i2 = 0;
                        if (i2 == 0) {
                            return context.setParsedField(ChronoField.OFFSET_SECONDS, ((long) negative) * (((((long) array[1]) * 3600) + (((long) array[2]) * 60)) + ((long) array[3])), i, array[0]);
                        }
                    }
                }
                i2 = 1;
                if (i2 == 0) {
                }
            }
            if (noOffsetLen != 0) {
                return ~i;
            }
            return context.setParsedField(ChronoField.OFFSET_SECONDS, 0, i, i + noOffsetLen);
        }

        private boolean parseNumber(int[] array, int arrayIndex, CharSequence parseText, boolean required) {
            if ((this.type + 3) / 2 < arrayIndex) {
                return false;
            }
            char ch1 = array[0];
            if (this.type % 2 == 0 && arrayIndex > 1) {
                if (ch1 + 1 > parseText.length() || parseText.charAt(ch1) != ':') {
                    return required;
                }
                ch1++;
            }
            if (ch1 + 2 > parseText.length()) {
                return required;
            }
            char ch2 = ch1 + 1;
            ch1 = parseText.charAt(ch1);
            int pos = ch2 + 1;
            ch2 = parseText.charAt(ch2);
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                return required;
            }
            int value = ((ch1 - 48) * 10) + (ch2 - 48);
            if (value < 0 || value > 59) {
                return required;
            }
            array[arrayIndex] = value;
            array[0] = pos;
            return false;
        }

        public String toString() {
            String converted = this.noOffsetText.replace((CharSequence) "'", (CharSequence) "''");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Offset(");
            stringBuilder.append(PATTERNS[this.type]);
            stringBuilder.append(",'");
            stringBuilder.append(converted);
            stringBuilder.append("')");
            return stringBuilder.toString();
        }
    }

    static final class PadPrinterParserDecorator implements DateTimePrinterParser {
        private final char padChar;
        private final int padWidth;
        private final DateTimePrinterParser printerParser;

        PadPrinterParserDecorator(DateTimePrinterParser printerParser, int padWidth, char padChar) {
            this.printerParser = printerParser;
            this.padWidth = padWidth;
            this.padChar = padChar;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            int preLen = buf.length();
            int i = 0;
            if (!this.printerParser.format(context, buf)) {
                return false;
            }
            int len = buf.length() - preLen;
            if (len <= this.padWidth) {
                while (i < this.padWidth - len) {
                    buf.insert(preLen, this.padChar);
                    i++;
                }
                return true;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot print as output of ");
            stringBuilder.append(len);
            stringBuilder.append(" characters exceeds pad width of ");
            stringBuilder.append(this.padWidth);
            throw new DateTimeException(stringBuilder.toString());
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            boolean strict = context.isStrict();
            if (position > text.length()) {
                throw new IndexOutOfBoundsException();
            } else if (position == text.length()) {
                return ~position;
            } else {
                int endPos = this.padWidth + position;
                if (endPos > text.length()) {
                    if (strict) {
                        return ~position;
                    }
                    endPos = text.length();
                }
                int pos = position;
                while (pos < endPos && context.charEquals(text.charAt(pos), this.padChar)) {
                    pos++;
                }
                int resultPos = this.printerParser.parse(context, text.subSequence(0, endPos), pos);
                if (resultPos == endPos || !strict) {
                    return resultPos;
                }
                return ~(position + pos);
            }
        }

        public String toString() {
            String str;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Pad(");
            stringBuilder.append(this.printerParser);
            stringBuilder.append(",");
            stringBuilder.append(this.padWidth);
            if (this.padChar == ' ') {
                str = ")";
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(",'");
                stringBuilder2.append(this.padChar);
                stringBuilder2.append("')");
                str = stringBuilder2.toString();
            }
            stringBuilder.append(str);
            return stringBuilder.toString();
        }
    }

    static final class StringLiteralPrinterParser implements DateTimePrinterParser {
        private final String literal;

        StringLiteralPrinterParser(String literal) {
            this.literal = literal;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            buf.append(this.literal);
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            if (position > text.length() || position < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (context.subSequenceEquals(text, position, this.literal, 0, this.literal.length())) {
                return this.literal.length() + position;
            }
            return ~position;
        }

        public String toString() {
            String converted = this.literal.replace((CharSequence) "'", (CharSequence) "''");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append(converted);
            stringBuilder.append("'");
            return stringBuilder.toString();
        }
    }

    static final class TextPrinterParser implements DateTimePrinterParser {
        private final TemporalField field;
        private volatile NumberPrinterParser numberPrinterParser;
        private final DateTimeTextProvider provider;
        private final TextStyle textStyle;

        TextPrinterParser(TemporalField field, TextStyle textStyle, DateTimeTextProvider provider) {
            this.field = field;
            this.textStyle = textStyle;
            this.provider = provider;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long value = context.getValue(this.field);
            if (value == null) {
                return false;
            }
            String text;
            Chronology chrono = (Chronology) context.getTemporal().query(TemporalQueries.chronology());
            if (chrono == null || chrono == IsoChronology.INSTANCE) {
                text = this.provider.getText(this.field, value.longValue(), this.textStyle, context.getLocale());
            } else {
                text = this.provider.getText(chrono, this.field, value.longValue(), this.textStyle, context.getLocale());
            }
            if (text == null) {
                return numberPrinterParser().format(context, buf);
            }
            buf.append(text);
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence parseText, int position) {
            int i = position;
            int length = parseText.length();
            if (i < 0 || i > length) {
                DateTimeParseContext dateTimeParseContext = context;
                CharSequence charSequence = parseText;
                throw new IndexOutOfBoundsException();
            }
            Iterator<Entry<String, Long>> it;
            TextStyle style = context.isStrict() ? this.textStyle : null;
            Chronology chrono = context.getEffectiveChronology();
            if (chrono == null || chrono == IsoChronology.INSTANCE) {
                it = this.provider.getTextIterator(this.field, style, context.getLocale());
            } else {
                it = this.provider.getTextIterator(chrono, this.field, style, context.getLocale());
            }
            Iterator<Entry<String, Long>> it2 = it;
            if (it2 != null) {
                while (it2.hasNext()) {
                    Entry<String, Long> entry = (Entry) it2.next();
                    String itText = (String) entry.getKey();
                    if (context.subSequenceEquals(itText, 0, parseText, i, itText.length())) {
                        return context.setParsedField(this.field, ((Long) entry.getValue()).longValue(), i, i + itText.length());
                    }
                }
                if (context.isStrict()) {
                    return ~i;
                }
            }
            return numberPrinterParser().parse(context, parseText, i);
        }

        private NumberPrinterParser numberPrinterParser() {
            if (this.numberPrinterParser == null) {
                this.numberPrinterParser = new NumberPrinterParser(this.field, 1, 19, SignStyle.NORMAL);
            }
            return this.numberPrinterParser;
        }

        public String toString() {
            StringBuilder stringBuilder;
            if (this.textStyle == TextStyle.FULL) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Text(");
                stringBuilder.append(this.field);
                stringBuilder.append(")");
                return stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Text(");
            stringBuilder.append(this.field);
            stringBuilder.append(",");
            stringBuilder.append(this.textStyle);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    static final class WeekBasedFieldPrinterParser implements DateTimePrinterParser {
        private char chr;
        private int count;

        WeekBasedFieldPrinterParser(char chr, int count) {
            this.chr = chr;
            this.count = count;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return printerParser(context.getLocale()).format(context, buf);
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            return printerParser(context.getLocale()).parse(context, text, position);
        }

        private DateTimePrinterParser printerParser(Locale locale) {
            TemporalField field;
            WeekFields weekDef = WeekFields.of(locale);
            char c = this.chr;
            if (c == 'W') {
                field = weekDef.weekOfMonth();
            } else if (c == 'Y') {
                field = weekDef.weekBasedYear();
                if (this.count == 2) {
                    return new ReducedPrinterParser(field, 2, 2, 0, ReducedPrinterParser.BASE_DATE, 0, null);
                }
                return new NumberPrinterParser(field, this.count, 19, this.count < 4 ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD, -1);
            } else if (c == 'c' || c == 'e') {
                field = weekDef.dayOfWeek();
            } else if (c == 'w') {
                field = weekDef.weekOfWeekBasedYear();
            } else {
                throw new IllegalStateException("unreachable");
            }
            return new NumberPrinterParser(field, this.count == 2 ? 2 : 1, 2, SignStyle.NOT_NEGATIVE);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(30);
            sb.append("Localized(");
            if (this.chr != 'Y') {
                char c = this.chr;
                if (c == 'W') {
                    sb.append("WeekOfMonth");
                } else if (c == 'c' || c == 'e') {
                    sb.append("DayOfWeek");
                } else if (c == 'w') {
                    sb.append("WeekOfWeekBasedYear");
                }
                sb.append(",");
                sb.append(this.count);
            } else if (this.count == 1) {
                sb.append("WeekBasedYear");
            } else if (this.count == 2) {
                sb.append("ReducedValue(WeekBasedYear,2,2,2000-01-01)");
            } else {
                sb.append("WeekBasedYear,");
                sb.append(this.count);
                sb.append(",");
                sb.append(19);
                sb.append(",");
                sb.append(this.count < 4 ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    static class ZoneIdPrinterParser implements DateTimePrinterParser {
        private static volatile Entry<Integer, PrefixTree> cachedPrefixTree;
        private static volatile Entry<Integer, PrefixTree> cachedPrefixTreeCI;
        private final String description;
        private final TemporalQuery<ZoneId> query;

        ZoneIdPrinterParser(TemporalQuery<ZoneId> query, String description) {
            this.query = query;
            this.description = description;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            ZoneId zone = (ZoneId) context.getValue(this.query);
            if (zone == null) {
                return false;
            }
            buf.append(zone.getId());
            return true;
        }

        protected PrefixTree getTree(DateTimeParseContext context) {
            Set<String> regionIds = ZoneRulesProvider.getAvailableZoneIds();
            int regionIdsSize = regionIds.size();
            Entry<Integer, PrefixTree> cached = context.isCaseSensitive() ? cachedPrefixTree : cachedPrefixTreeCI;
            if (cached == null || ((Integer) cached.getKey()).intValue() != regionIdsSize) {
                synchronized (this) {
                    cached = context.isCaseSensitive() ? cachedPrefixTree : cachedPrefixTreeCI;
                    if (cached == null || ((Integer) cached.getKey()).intValue() != regionIdsSize) {
                        cached = new SimpleImmutableEntry(Integer.valueOf(regionIdsSize), PrefixTree.newTree(regionIds, context));
                        if (context.isCaseSensitive()) {
                            cachedPrefixTree = cached;
                        } else {
                            cachedPrefixTreeCI = cached;
                        }
                    }
                }
            }
            return (PrefixTree) cached.getValue();
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int length = text.length();
            if (position > length) {
                throw new IndexOutOfBoundsException();
            } else if (position == length) {
                return ~position;
            } else {
                char nextChar = text.charAt(position);
                if (nextChar == '+' || nextChar == '-') {
                    return parseOffsetBased(context, text, position, position, OffsetIdPrinterParser.INSTANCE_ID_Z);
                }
                if (length >= position + 2) {
                    char nextNextChar = text.charAt(position + 1);
                    if (context.charEquals(nextChar, 'U') && context.charEquals(nextNextChar, 'T')) {
                        if (length < position + 3 || !context.charEquals(text.charAt(position + 2), 'C')) {
                            return parseOffsetBased(context, text, position, position + 2, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                        }
                        return parseOffsetBased(context, text, position, position + 3, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                    } else if (context.charEquals(nextChar, 'G') && length >= position + 3 && context.charEquals(nextNextChar, 'M') && context.charEquals(text.charAt(position + 2), 'T')) {
                        return parseOffsetBased(context, text, position, position + 3, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                    }
                }
                PrefixTree tree = getTree(context);
                ParsePosition ppos = new ParsePosition(position);
                String parsedZoneId = tree.match(text, ppos);
                if (parsedZoneId != null) {
                    context.setParsed(ZoneId.of(parsedZoneId));
                    return ppos.getIndex();
                } else if (!context.charEquals(nextChar, 'Z')) {
                    return ~position;
                } else {
                    context.setParsed(ZoneOffset.UTC);
                    return position + 1;
                }
            }
        }

        private int parseOffsetBased(DateTimeParseContext context, CharSequence text, int prefixPos, int position, OffsetIdPrinterParser parser) {
            String prefix = text.toString().substring(prefixPos, position).toUpperCase();
            if (position >= text.length()) {
                context.setParsed(ZoneId.of(prefix));
                return position;
            } else if (text.charAt(position) == '0' && prefix.equals("GMT")) {
                context.setParsed(ZoneId.of("GMT0"));
                return position + 1;
            } else if (text.charAt(position) == '0' || context.charEquals(text.charAt(position), 'Z')) {
                context.setParsed(ZoneId.of(prefix));
                return position;
            } else {
                DateTimeParseContext newContext = context.copy();
                int endPos = parser.parse(newContext, text, position);
                if (endPos < 0) {
                    try {
                        if (parser == OffsetIdPrinterParser.INSTANCE_ID_Z) {
                            return ~prefixPos;
                        }
                        context.setParsed(ZoneId.of(prefix));
                        return position;
                    } catch (DateTimeException e) {
                        return ~prefixPos;
                    }
                }
                context.setParsed(ZoneId.ofOffset(prefix, ZoneOffset.ofTotalSeconds((int) newContext.getParsed(ChronoField.OFFSET_SECONDS).longValue())));
                return endPos;
            }
        }

        public String toString() {
            return this.description;
        }
    }

    static final class ReducedPrinterParser extends NumberPrinterParser {
        static final LocalDate BASE_DATE = LocalDate.of((int) Types.JAVA_OBJECT, 1, 1);
        private final ChronoLocalDate baseDate;
        private final int baseValue;

        /* synthetic */ ReducedPrinterParser(TemporalField x0, int x1, int x2, int x3, ChronoLocalDate x4, int x5, AnonymousClass1 x6) {
            this(x0, x1, x2, x3, x4, x5);
        }

        ReducedPrinterParser(TemporalField field, int minWidth, int maxWidth, int baseValue, ChronoLocalDate baseDate) {
            this(field, minWidth, maxWidth, baseValue, baseDate, 0);
            StringBuilder stringBuilder;
            if (minWidth < 1 || minWidth > 10) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("The minWidth must be from 1 to 10 inclusive but was ");
                stringBuilder.append(minWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (maxWidth < 1 || maxWidth > 10) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("The maxWidth must be from 1 to 10 inclusive but was ");
                stringBuilder.append(minWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (maxWidth < minWidth) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Maximum width must exceed or equal the minimum width but ");
                stringBuilder.append(maxWidth);
                stringBuilder.append(" < ");
                stringBuilder.append(minWidth);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (baseDate != null) {
            } else {
                if (!field.range().isValidValue((long) baseValue)) {
                    throw new IllegalArgumentException("The base value must be within the range of the field");
                } else if (((long) baseValue) + EXCEED_POINTS[maxWidth] > 2147483647L) {
                    throw new DateTimeException("Unable to add printer-parser as the range exceeds the capacity of an int");
                }
            }
        }

        private ReducedPrinterParser(TemporalField field, int minWidth, int maxWidth, int baseValue, ChronoLocalDate baseDate, int subsequentWidth) {
            super(field, minWidth, maxWidth, SignStyle.NOT_NEGATIVE, subsequentWidth);
            this.baseValue = baseValue;
            this.baseDate = baseDate;
        }

        long getValue(DateTimePrintContext context, long value) {
            long absValue = Math.abs(value);
            int baseValue = this.baseValue;
            if (this.baseDate != null) {
                baseValue = Chronology.from(context.getTemporal()).date(this.baseDate).get(this.field);
            }
            if (value < ((long) baseValue) || value >= ((long) baseValue) + EXCEED_POINTS[this.minWidth]) {
                return absValue % EXCEED_POINTS[this.maxWidth];
            }
            return absValue % EXCEED_POINTS[this.minWidth];
        }

        int setValue(DateTimeParseContext context, long value, int errorPos, int successPos) {
            int baseValue = this.baseValue;
            if (this.baseDate != null) {
                baseValue = context.getEffectiveChronology().date(this.baseDate).get(this.field);
                context.addChronoChangedListener(new -$$Lambda$DateTimeFormatterBuilder$ReducedPrinterParser$O7fxxUm4cHduGbldToNj0T92oIo(this, context, value, errorPos, successPos));
            }
            if (successPos - errorPos == this.minWidth && value >= 0) {
                long range = EXCEED_POINTS[this.minWidth];
                long basePart = ((long) baseValue) - (((long) baseValue) % range);
                if (baseValue > 0) {
                    value += basePart;
                } else {
                    value = basePart - value;
                }
                if (value < ((long) baseValue)) {
                    value += range;
                }
            }
            return context.setParsedField(this.field, value, errorPos, successPos);
        }

        ReducedPrinterParser withFixedWidth() {
            if (this.subsequentWidth == -1) {
                return this;
            }
            return new ReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate, -1);
        }

        ReducedPrinterParser withSubsequentWidth(int subsequentWidth) {
            return new ReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate, this.subsequentWidth + subsequentWidth);
        }

        boolean isFixedWidth(DateTimeParseContext context) {
            if (context.isStrict()) {
                return super.isFixedWidth(context);
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ReducedValue(");
            stringBuilder.append(this.field);
            stringBuilder.append(",");
            stringBuilder.append(this.minWidth);
            stringBuilder.append(",");
            stringBuilder.append(this.maxWidth);
            stringBuilder.append(",");
            stringBuilder.append(this.baseDate != null ? this.baseDate : Integer.valueOf(this.baseValue));
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    enum SettingsParser implements DateTimePrinterParser {
        SENSITIVE,
        INSENSITIVE,
        STRICT,
        LENIENT;

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            switch (ordinal()) {
                case 0:
                    context.setCaseSensitive(true);
                    break;
                case 1:
                    context.setCaseSensitive(false);
                    break;
                case 2:
                    context.setStrict(true);
                    break;
                case 3:
                    context.setStrict(false);
                    break;
            }
            return position;
        }

        public String toString() {
            switch (ordinal()) {
                case 0:
                    return "ParseCaseSensitive(true)";
                case 1:
                    return "ParseCaseSensitive(false)";
                case 2:
                    return "ParseStrict(true)";
                case 3:
                    return "ParseStrict(false)";
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }
    }

    static final class ZoneTextPrinterParser extends ZoneIdPrinterParser {
        private static final int DST = 1;
        private static final NameType[] FULL_TYPES = new NameType[]{NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, NameType.LONG_GENERIC};
        private static final int GENERIC = 2;
        private static final NameType[] SHORT_TYPES = new NameType[]{NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT, NameType.SHORT_GENERIC};
        private static final int STD = 0;
        private static final NameType[] TYPES = new NameType[]{NameType.LONG_STANDARD, NameType.SHORT_STANDARD, NameType.LONG_DAYLIGHT, NameType.SHORT_DAYLIGHT, NameType.LONG_GENERIC, NameType.SHORT_GENERIC};
        private static final Map<String, SoftReference<Map<Locale, String[]>>> cache = new ConcurrentHashMap();
        private final Map<Locale, Entry<Integer, SoftReference<PrefixTree>>> cachedTree = new HashMap();
        private final Map<Locale, Entry<Integer, SoftReference<PrefixTree>>> cachedTreeCI = new HashMap();
        private Set<String> preferredZones;
        private final TextStyle textStyle;

        ZoneTextPrinterParser(TextStyle textStyle, Set<ZoneId> preferredZones) {
            TemporalQuery zone = TemporalQueries.zone();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ZoneText(");
            stringBuilder.append((Object) textStyle);
            stringBuilder.append(")");
            super(zone, stringBuilder.toString());
            this.textStyle = (TextStyle) Objects.requireNonNull((Object) textStyle, "textStyle");
            if (preferredZones != null && preferredZones.size() != 0) {
                this.preferredZones = new HashSet();
                for (ZoneId id : preferredZones) {
                    this.preferredZones.add(id.getId());
                }
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:46:0x00d5  */
        /* JADX WARNING: Removed duplicated region for block: B:50:0x00ea  */
        /* JADX WARNING: Removed duplicated region for block: B:48:0x00e0  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private String getDisplayName(String id, int type, Locale locale) {
            String str = id;
            Locale locale2 = locale;
            if (this.textStyle != TextStyle.NARROW) {
                String[] names;
                SoftReference<Map<Locale, String[]>> ref = (SoftReference) cache.get(str);
                Map<Locale, String[]> perLocale = null;
                if (ref != null) {
                    Map<Locale, String[]> map = (Map) ref.get();
                    perLocale = map;
                    if (map != null) {
                        String[] strArr = (String[]) perLocale.get(locale2);
                        String[] names2 = strArr;
                        if (strArr != null) {
                            names = names2;
                            switch (type) {
                                case 0:
                                    return names[this.textStyle.zoneNameStyleIndex() + 1];
                                case 1:
                                    return names[this.textStyle.zoneNameStyleIndex() + 3];
                                default:
                                    return names[this.textStyle.zoneNameStyleIndex() + 5];
                            }
                        }
                    }
                }
                TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
                String[] names3 = new String[(TYPES.length + 1)];
                names3[0] = str;
                names = names3;
                timeZoneNames.getDisplayNames(ZoneMeta.getCanonicalCLDRID(id), TYPES, System.currentTimeMillis(), names3, 1);
                if (names[1] == null || names[2] == null || names[3] == null || names[4] == null) {
                    TimeZone tz = TimeZone.getTimeZone(id);
                    String stdString = TimeZone.createGmtOffsetString(true, true, tz.getRawOffset());
                    String dstString = TimeZone.createGmtOffsetString(true, true, tz.getRawOffset() + tz.getDSTSavings());
                    names[1] = names[1] != null ? names[1] : stdString;
                    names[2] = names[2] != null ? names[2] : stdString;
                    names[3] = names[3] != null ? names[3] : dstString;
                    names[4] = names[4] != null ? names[4] : dstString;
                }
                if (names[5] == null) {
                    names[5] = names[0];
                }
                if (names[6] == null) {
                    names[6] = names[0];
                }
                if (perLocale == null) {
                    perLocale = new ConcurrentHashMap();
                }
                perLocale.put(locale2, names);
                cache.put(str, new SoftReference(perLocale));
                switch (type) {
                    case 0:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
            return null;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            ZoneId zone = (ZoneId) context.getValue(TemporalQueries.zoneId());
            String name = null;
            if (zone == null) {
                return false;
            }
            String zname = zone.getId();
            if (!(zone instanceof ZoneOffset)) {
                TemporalAccessor dt = context.getTemporal();
                if (!dt.isSupported(ChronoField.INSTANT_SECONDS)) {
                    name = 2;
                } else if (zone.getRules().isDaylightSavings(Instant.from(dt))) {
                    name = 1;
                }
                name = getDisplayName(zname, name, context.getLocale());
                if (name != null) {
                    zname = name;
                }
            }
            buf.append(zname);
            return true;
        }

        protected PrefixTree getTree(DateTimeParseContext context) {
            ZoneTextPrinterParser zoneTextPrinterParser = this;
            if (zoneTextPrinterParser.textStyle == TextStyle.NARROW) {
                return super.getTree(context);
            }
            PrefixTree tree;
            boolean z;
            NameType[] types;
            int i;
            Locale locale = context.getLocale();
            boolean isCaseSensitive = context.isCaseSensitive();
            Set<String> regionIds = ZoneRulesProvider.getAvailableZoneIds();
            int regionIdsSize = regionIds.size();
            Map<Locale, Entry<Integer, SoftReference<PrefixTree>>> cached = isCaseSensitive ? zoneTextPrinterParser.cachedTree : zoneTextPrinterParser.cachedTreeCI;
            Entry<Integer, SoftReference<PrefixTree>> entry = (Entry) cached.get(locale);
            Entry<Integer, SoftReference<PrefixTree>> entry2 = entry;
            if (entry != null && ((Integer) entry2.getKey()).intValue() == regionIdsSize) {
                PrefixTree prefixTree = (PrefixTree) ((SoftReference) entry2.getValue()).get();
                tree = prefixTree;
                if (prefixTree != null) {
                    z = isCaseSensitive;
                    return tree;
                }
            }
            tree = PrefixTree.newTree(context);
            TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
            long now = System.currentTimeMillis();
            NameType[] types2 = zoneTextPrinterParser.textStyle == TextStyle.FULL ? FULL_TYPES : SHORT_TYPES;
            String[] names = new String[types2.length];
            Iterator it = regionIds.iterator();
            while (true) {
                int i2 = 0;
                if (!it.hasNext()) {
                    break;
                }
                String zid = (String) it.next();
                tree.add(zid, zid);
                String zid2 = ZoneName.toZid(zid, locale);
                Iterator it2 = it;
                String zid3 = zid2;
                String[] names2 = names;
                types = types2;
                timeZoneNames.getDisplayNames(zid2, types2, now, names, null);
                while (true) {
                    i = i2;
                    types2 = names2;
                    if (i >= types2.length) {
                        break;
                    }
                    String zid4;
                    if (types2[i] != null) {
                        zid4 = zid3;
                        tree.add(types2[i], zid4);
                    } else {
                        zid4 = zid3;
                    }
                    i2 = i + 1;
                    zid3 = zid4;
                    names2 = types2;
                }
                names = types2;
                it = it2;
                types2 = types;
            }
            types = types2;
            String[] names3 = names;
            if (zoneTextPrinterParser.preferredZones != null) {
                Iterator it3 = regionIds.iterator();
                while (it3.hasNext()) {
                    String zid5 = (String) it3.next();
                    if (zoneTextPrinterParser.preferredZones.contains(zid5)) {
                        String zid6 = zid5;
                        Iterator it4 = it3;
                        z = isCaseSensitive;
                        String[] names4 = names3;
                        timeZoneNames.getDisplayNames(ZoneName.toZid(zid5, locale), types, now, names3, null);
                        for (i = 0; i < names4.length; i++) {
                            if (names4[i] != null) {
                                tree.add(names4[i], zid6);
                            }
                        }
                        names3 = names4;
                        it3 = it4;
                        isCaseSensitive = z;
                        zoneTextPrinterParser = this;
                    }
                }
            }
            cached.put(locale, new SimpleImmutableEntry(Integer.valueOf(regionIdsSize), new SoftReference(tree)));
            return tree;
        }
    }

    static {
        FIELD_MAP.put(Character.valueOf('G'), ChronoField.ERA);
        FIELD_MAP.put(Character.valueOf('y'), ChronoField.YEAR_OF_ERA);
        FIELD_MAP.put(Character.valueOf('u'), ChronoField.YEAR);
        FIELD_MAP.put(Character.valueOf('Q'), IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put(Character.valueOf('q'), IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put(Character.valueOf('M'), ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put(Character.valueOf('L'), ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put(Character.valueOf('D'), ChronoField.DAY_OF_YEAR);
        FIELD_MAP.put(Character.valueOf('d'), ChronoField.DAY_OF_MONTH);
        FIELD_MAP.put(Character.valueOf('F'), ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        FIELD_MAP.put(Character.valueOf('E'), ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put(Character.valueOf('c'), ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put(Character.valueOf('e'), ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put(Character.valueOf('a'), ChronoField.AMPM_OF_DAY);
        FIELD_MAP.put(Character.valueOf('H'), ChronoField.HOUR_OF_DAY);
        FIELD_MAP.put(Character.valueOf('k'), ChronoField.CLOCK_HOUR_OF_DAY);
        FIELD_MAP.put(Character.valueOf('K'), ChronoField.HOUR_OF_AMPM);
        FIELD_MAP.put(Character.valueOf('h'), ChronoField.CLOCK_HOUR_OF_AMPM);
        FIELD_MAP.put(Character.valueOf('m'), ChronoField.MINUTE_OF_HOUR);
        FIELD_MAP.put(Character.valueOf('s'), ChronoField.SECOND_OF_MINUTE);
        FIELD_MAP.put(Character.valueOf('S'), ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put(Character.valueOf('A'), ChronoField.MILLI_OF_DAY);
        FIELD_MAP.put(Character.valueOf('n'), ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put(Character.valueOf('N'), ChronoField.NANO_OF_DAY);
    }

    static /* synthetic */ ZoneId lambda$static$0(TemporalAccessor temporal) {
        ZoneId zone = (ZoneId) temporal.query(TemporalQueries.zoneId());
        return (zone == null || (zone instanceof ZoneOffset)) ? null : zone;
    }

    public static String getLocalizedDateTimePattern(FormatStyle dateStyle, FormatStyle timeStyle, Chronology chrono, Locale locale) {
        Objects.requireNonNull((Object) locale, "locale");
        Objects.requireNonNull((Object) chrono, "chrono");
        if (dateStyle != null || timeStyle != null) {
            return Calendar.getDateTimeFormatString(ULocale.forLocale(locale), chrono.getCalendarType(), convertStyle(dateStyle), convertStyle(timeStyle));
        }
        throw new IllegalArgumentException("Either dateStyle or timeStyle must be non-null");
    }

    private static int convertStyle(FormatStyle style) {
        if (style == null) {
            return -1;
        }
        return style.ordinal();
    }

    public DateTimeFormatterBuilder() {
        this.active = this;
        this.printerParsers = new ArrayList();
        this.valueParserIndex = -1;
        this.parent = null;
        this.optional = false;
    }

    private DateTimeFormatterBuilder(DateTimeFormatterBuilder parent, boolean optional) {
        this.active = this;
        this.printerParsers = new ArrayList();
        this.valueParserIndex = -1;
        this.parent = parent;
        this.optional = optional;
    }

    public DateTimeFormatterBuilder parseCaseSensitive() {
        appendInternal(SettingsParser.SENSITIVE);
        return this;
    }

    public DateTimeFormatterBuilder parseCaseInsensitive() {
        appendInternal(SettingsParser.INSENSITIVE);
        return this;
    }

    public DateTimeFormatterBuilder parseStrict() {
        appendInternal(SettingsParser.STRICT);
        return this;
    }

    public DateTimeFormatterBuilder parseLenient() {
        appendInternal(SettingsParser.LENIENT);
        return this;
    }

    public DateTimeFormatterBuilder parseDefaulting(TemporalField field, long value) {
        Objects.requireNonNull((Object) field, "field");
        appendInternal(new DefaultValueParser(field, value));
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field) {
        Objects.requireNonNull((Object) field, "field");
        appendValue(new NumberPrinterParser(field, 1, 19, SignStyle.NORMAL));
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int width) {
        Objects.requireNonNull((Object) field, "field");
        if (width < 1 || width > 19) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The width must be from 1 to 19 inclusive but was ");
            stringBuilder.append(width);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        appendValue(new NumberPrinterParser(field, width, width, SignStyle.NOT_NEGATIVE));
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle) {
        if (minWidth == maxWidth && signStyle == SignStyle.NOT_NEGATIVE) {
            return appendValue(field, maxWidth);
        }
        Objects.requireNonNull((Object) field, "field");
        Objects.requireNonNull((Object) signStyle, "signStyle");
        StringBuilder stringBuilder;
        if (minWidth < 1 || minWidth > 19) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("The minimum width must be from 1 to 19 inclusive but was ");
            stringBuilder.append(minWidth);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (maxWidth < 1 || maxWidth > 19) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("The maximum width must be from 1 to 19 inclusive but was ");
            stringBuilder.append(maxWidth);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (maxWidth >= minWidth) {
            appendValue(new NumberPrinterParser(field, minWidth, maxWidth, signStyle));
            return this;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("The maximum width must exceed or equal the minimum width but ");
            stringBuilder.append(maxWidth);
            stringBuilder.append(" < ");
            stringBuilder.append(minWidth);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth, int baseValue) {
        Objects.requireNonNull((Object) field, "field");
        appendValue(new ReducedPrinterParser(field, width, maxWidth, baseValue, null));
        return this;
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth, ChronoLocalDate baseDate) {
        Objects.requireNonNull((Object) field, "field");
        Objects.requireNonNull((Object) baseDate, "baseDate");
        appendValue(new ReducedPrinterParser(field, width, maxWidth, 0, baseDate));
        return this;
    }

    private DateTimeFormatterBuilder appendValue(NumberPrinterParser pp) {
        if (this.active.valueParserIndex >= 0) {
            int activeValueParser = this.active.valueParserIndex;
            NumberPrinterParser basePP = (NumberPrinterParser) this.active.printerParsers.get(activeValueParser);
            if (pp.minWidth == pp.maxWidth && pp.signStyle == SignStyle.NOT_NEGATIVE) {
                basePP = basePP.withSubsequentWidth(pp.maxWidth);
                appendInternal(pp.withFixedWidth());
                this.active.valueParserIndex = activeValueParser;
            } else {
                basePP = basePP.withFixedWidth();
                this.active.valueParserIndex = appendInternal(pp);
            }
            this.active.printerParsers.set(activeValueParser, basePP);
        } else {
            this.active.valueParserIndex = appendInternal(pp);
        }
        return this;
    }

    public DateTimeFormatterBuilder appendFraction(TemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {
        appendInternal(new FractionPrinterParser(field, minWidth, maxWidth, decimalPoint));
        return this;
    }

    public DateTimeFormatterBuilder appendText(TemporalField field) {
        return appendText(field, TextStyle.FULL);
    }

    public DateTimeFormatterBuilder appendText(TemporalField field, TextStyle textStyle) {
        Objects.requireNonNull((Object) field, "field");
        Objects.requireNonNull((Object) textStyle, "textStyle");
        appendInternal(new TextPrinterParser(field, textStyle, DateTimeTextProvider.getInstance()));
        return this;
    }

    public DateTimeFormatterBuilder appendText(TemporalField field, Map<Long, String> textLookup) {
        Objects.requireNonNull((Object) field, "field");
        Objects.requireNonNull((Object) textLookup, "textLookup");
        final LocaleStore store = new LocaleStore(Collections.singletonMap(TextStyle.FULL, new LinkedHashMap((Map) textLookup)));
        appendInternal(new TextPrinterParser(field, TextStyle.FULL, new DateTimeTextProvider() {
            public String getText(TemporalField field, long value, TextStyle style, Locale locale) {
                return store.getText(value, style);
            }

            public Iterator<Entry<String, Long>> getTextIterator(TemporalField field, TextStyle style, Locale locale) {
                return store.getTextIterator(style);
            }
        }));
        return this;
    }

    public DateTimeFormatterBuilder appendInstant() {
        appendInternal(new InstantPrinterParser(-2));
        return this;
    }

    public DateTimeFormatterBuilder appendInstant(int fractionalDigits) {
        if (fractionalDigits < -1 || fractionalDigits > 9) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The fractional digits must be from -1 to 9 inclusive but was ");
            stringBuilder.append(fractionalDigits);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        appendInternal(new InstantPrinterParser(fractionalDigits));
        return this;
    }

    public DateTimeFormatterBuilder appendOffsetId() {
        appendInternal(OffsetIdPrinterParser.INSTANCE_ID_Z);
        return this;
    }

    public DateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) {
        appendInternal(new OffsetIdPrinterParser(pattern, noOffsetText));
        return this;
    }

    public DateTimeFormatterBuilder appendLocalizedOffset(TextStyle style) {
        Objects.requireNonNull((Object) style, "style");
        if (style == TextStyle.FULL || style == TextStyle.SHORT) {
            appendInternal(new LocalizedOffsetIdPrinterParser(style));
            return this;
        }
        throw new IllegalArgumentException("Style must be either full or short");
    }

    public DateTimeFormatterBuilder appendZoneId() {
        appendInternal(new ZoneIdPrinterParser(TemporalQueries.zoneId(), "ZoneId()"));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneRegionId() {
        appendInternal(new ZoneIdPrinterParser(QUERY_REGION_ONLY, "ZoneRegionId()"));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneOrOffsetId() {
        appendInternal(new ZoneIdPrinterParser(TemporalQueries.zone(), "ZoneOrOffsetId()"));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle) {
        appendInternal(new ZoneTextPrinterParser(textStyle, null));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle, Set<ZoneId> preferredZones) {
        Objects.requireNonNull((Object) preferredZones, "preferredZones");
        appendInternal(new ZoneTextPrinterParser(textStyle, preferredZones));
        return this;
    }

    public DateTimeFormatterBuilder appendChronologyId() {
        appendInternal(new ChronoPrinterParser(null));
        return this;
    }

    public DateTimeFormatterBuilder appendChronologyText(TextStyle textStyle) {
        Objects.requireNonNull((Object) textStyle, "textStyle");
        appendInternal(new ChronoPrinterParser(textStyle));
        return this;
    }

    public DateTimeFormatterBuilder appendLocalized(FormatStyle dateStyle, FormatStyle timeStyle) {
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either the date or time style must be non-null");
        }
        appendInternal(new LocalizedPrinterParser(dateStyle, timeStyle));
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(char literal) {
        appendInternal(new CharLiteralPrinterParser(literal));
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(String literal) {
        Objects.requireNonNull((Object) literal, "literal");
        if (literal.length() > 0) {
            if (literal.length() == 1) {
                appendInternal(new CharLiteralPrinterParser(literal.charAt(0)));
            } else {
                appendInternal(new StringLiteralPrinterParser(literal));
            }
        }
        return this;
    }

    public DateTimeFormatterBuilder append(DateTimeFormatter formatter) {
        Objects.requireNonNull((Object) formatter, "formatter");
        appendInternal(formatter.toPrinterParser(false));
        return this;
    }

    public DateTimeFormatterBuilder appendOptional(DateTimeFormatter formatter) {
        Objects.requireNonNull((Object) formatter, "formatter");
        appendInternal(formatter.toPrinterParser(true));
        return this;
    }

    public DateTimeFormatterBuilder appendPattern(String pattern) {
        Objects.requireNonNull((Object) pattern, "pattern");
        parsePattern(pattern);
        return this;
    }

    private void parsePattern(String pattern) {
        int pos = 0;
        while (pos < pattern.length()) {
            char cur = pattern.charAt(pos);
            StringBuilder stringBuilder;
            if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
                int pos2 = pos + 1;
                while (pos2 < pattern.length() && pattern.charAt(pos2) == cur) {
                    pos2++;
                }
                int count = pos2 - pos;
                if (cur == 'p') {
                    int pad = 0;
                    if (pos2 < pattern.length()) {
                        cur = pattern.charAt(pos2);
                        if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
                            pad = count;
                            int pos3 = pos2 + 1;
                            pos = pos2;
                            while (pos3 < pattern.length() && pattern.charAt(pos3) == cur) {
                                pos3++;
                            }
                            pos2 = pos3;
                            count = pos3 - pos;
                        }
                    }
                    if (pad != 0) {
                        padNext(pad);
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Pad letter 'p' must be followed by valid pad pattern: ");
                        stringBuilder.append(pattern);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                TemporalField field = (TemporalField) FIELD_MAP.get(Character.valueOf(cur));
                StringBuilder stringBuilder2;
                if (field != null) {
                    parseField(cur, count, field);
                } else if (cur == 'z') {
                    if (count > 4) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many pattern letters: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    } else if (count == 4) {
                        appendZoneText(TextStyle.FULL);
                    } else {
                        appendZoneText(TextStyle.SHORT);
                    }
                } else if (cur == 'V') {
                    if (count == 2) {
                        appendZoneId();
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Pattern letter count must be 2: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == 'Z') {
                    if (count < 4) {
                        appendOffset("+HHMM", "+0000");
                    } else if (count == 4) {
                        appendLocalizedOffset(TextStyle.FULL);
                    } else if (count == 5) {
                        appendOffset("+HH:MM:ss", "Z");
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many pattern letters: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == 'O') {
                    if (count == 1) {
                        appendLocalizedOffset(TextStyle.SHORT);
                    } else if (count == 4) {
                        appendLocalizedOffset(TextStyle.FULL);
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Pattern letter count must be 1 or 4: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == 'X') {
                    if (count <= 5) {
                        appendOffset(OffsetIdPrinterParser.PATTERNS[(count == 1 ? 0 : 1) + count], "Z");
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many pattern letters: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == Locale.PRIVATE_USE_EXTENSION) {
                    if (count <= 5) {
                        String zero = count == 1 ? "+00" : count % 2 == 0 ? "+0000" : "+00:00";
                        appendOffset(OffsetIdPrinterParser.PATTERNS[(count == 1 ? 0 : 1) + count], zero);
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many pattern letters: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == 'W') {
                    if (count <= 1) {
                        appendInternal(new WeekBasedFieldPrinterParser(cur, count));
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many pattern letters: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == 'w') {
                    if (count <= 2) {
                        appendInternal(new WeekBasedFieldPrinterParser(cur, count));
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many pattern letters: ");
                        stringBuilder2.append(cur);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } else if (cur == 'Y') {
                    appendInternal(new WeekBasedFieldPrinterParser(cur, count));
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown pattern letter: ");
                    stringBuilder2.append(cur);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
                pos = pos2 - 1;
            } else if (cur == '\'') {
                int pos4 = pos + 1;
                while (pos4 < pattern.length()) {
                    if (pattern.charAt(pos4) == '\'') {
                        if (pos4 + 1 >= pattern.length() || pattern.charAt(pos4 + 1) != '\'') {
                            break;
                        }
                        pos4++;
                    }
                    pos4++;
                }
                if (pos4 < pattern.length()) {
                    String str = pattern.substring(pos + 1, pos4);
                    if (str.length() == 0) {
                        appendLiteral('\'');
                    } else {
                        appendLiteral(str.replace((CharSequence) "''", (CharSequence) "'"));
                    }
                    pos = pos4;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Pattern ends with an incomplete string literal: ");
                    stringBuilder.append(pattern);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else if (cur == '[') {
                optionalStart();
            } else if (cur == ']') {
                if (this.active.parent != null) {
                    optionalEnd();
                } else {
                    throw new IllegalArgumentException("Pattern invalid as it contains ] without previous [");
                }
            } else if (cur == '{' || cur == '}' || cur == '#') {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Pattern includes reserved character: '");
                stringBuilder.append(cur);
                stringBuilder.append("'");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                appendLiteral(cur);
            }
            pos++;
        }
    }

    /* JADX WARNING: Missing block: B:22:0x006a, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:23:0x006c, code skipped:
            if (r6 != 1) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:24:0x006e, code skipped:
            appendValue(r7);
     */
    /* JADX WARNING: Missing block: B:25:0x0073, code skipped:
            if (r6 != 2) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:26:0x0075, code skipped:
            appendValue(r7, r6);
     */
    /* JADX WARNING: Missing block: B:27:0x007a, code skipped:
            r2 = new java.lang.StringBuilder();
            r2.append("Too many pattern letters: ");
            r2.append(r5);
     */
    /* JADX WARNING: Missing block: B:28:0x0090, code skipped:
            throw new java.lang.IllegalArgumentException(r2.toString());
     */
    /* JADX WARNING: Missing block: B:39:0x00de, code skipped:
            switch(r6) {
                case 1: goto L_0x0119;
                case 2: goto L_0x0119;
                case 3: goto L_0x010e;
                case 4: goto L_0x0103;
                case 5: goto L_0x00f8;
                default: goto L_0x00e1;
            };
     */
    /* JADX WARNING: Missing block: B:40:0x00e1, code skipped:
            r2 = new java.lang.StringBuilder();
            r2.append("Too many pattern letters: ");
            r2.append(r5);
     */
    /* JADX WARNING: Missing block: B:41:0x00f7, code skipped:
            throw new java.lang.IllegalArgumentException(r2.toString());
     */
    /* JADX WARNING: Missing block: B:42:0x00f8, code skipped:
            if (r0 == false) goto L_0x00fd;
     */
    /* JADX WARNING: Missing block: B:43:0x00fa, code skipped:
            r1 = java.time.format.TextStyle.NARROW_STANDALONE;
     */
    /* JADX WARNING: Missing block: B:44:0x00fd, code skipped:
            r1 = java.time.format.TextStyle.NARROW;
     */
    /* JADX WARNING: Missing block: B:45:0x00ff, code skipped:
            appendText(r7, r1);
     */
    /* JADX WARNING: Missing block: B:46:0x0103, code skipped:
            if (r0 == false) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:47:0x0105, code skipped:
            r1 = java.time.format.TextStyle.FULL_STANDALONE;
     */
    /* JADX WARNING: Missing block: B:48:0x0108, code skipped:
            r1 = java.time.format.TextStyle.FULL;
     */
    /* JADX WARNING: Missing block: B:49:0x010a, code skipped:
            appendText(r7, r1);
     */
    /* JADX WARNING: Missing block: B:50:0x010e, code skipped:
            if (r0 == false) goto L_0x0113;
     */
    /* JADX WARNING: Missing block: B:51:0x0110, code skipped:
            r1 = java.time.format.TextStyle.SHORT_STANDALONE;
     */
    /* JADX WARNING: Missing block: B:52:0x0113, code skipped:
            r1 = java.time.format.TextStyle.SHORT;
     */
    /* JADX WARNING: Missing block: B:53:0x0115, code skipped:
            appendText(r7, r1);
     */
    /* JADX WARNING: Missing block: B:55:0x011b, code skipped:
            if (r5 == 'c') goto L_0x0136;
     */
    /* JADX WARNING: Missing block: B:57:0x011f, code skipped:
            if (r5 != 'e') goto L_0x0122;
     */
    /* JADX WARNING: Missing block: B:59:0x0124, code skipped:
            if (r5 != 'E') goto L_0x012c;
     */
    /* JADX WARNING: Missing block: B:60:0x0126, code skipped:
            appendText(r7, java.time.format.TextStyle.SHORT);
     */
    /* JADX WARNING: Missing block: B:61:0x012c, code skipped:
            if (r6 != 1) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:62:0x012e, code skipped:
            appendValue(r7);
     */
    /* JADX WARNING: Missing block: B:63:0x0132, code skipped:
            appendValue(r7, 2);
     */
    /* JADX WARNING: Missing block: B:64:0x0136, code skipped:
            appendInternal(new java.time.format.DateTimeFormatterBuilder.WeekBasedFieldPrinterParser(r5, r6));
     */
    /* JADX WARNING: Missing block: B:80:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:81:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:86:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:87:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:88:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:89:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:90:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:91:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:92:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseField(char cur, int count, TemporalField field) {
        boolean standalone = false;
        StringBuilder stringBuilder;
        switch (cur) {
            case 'D':
                if (count == 1) {
                    appendValue(field);
                    return;
                } else if (count <= 3) {
                    appendValue(field, count);
                    return;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Too many pattern letters: ");
                    stringBuilder.append(cur);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            case 'E':
                break;
            case Types.DATALINK /*70*/:
                if (count == 1) {
                    appendValue(field);
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Too many pattern letters: ");
                stringBuilder.append(cur);
                throw new IllegalArgumentException(stringBuilder.toString());
            case 'G':
                switch (count) {
                    case 1:
                    case 2:
                    case 3:
                        appendText(field, TextStyle.SHORT);
                        return;
                    case 4:
                        appendText(field, TextStyle.FULL);
                        return;
                    case 5:
                        appendText(field, TextStyle.NARROW);
                        return;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Too many pattern letters: ");
                        stringBuilder.append(cur);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            case 'H':
                break;
            default:
                switch (cur) {
                    case 'K':
                        break;
                    case 'L':
                        break;
                    case 'M':
                        break;
                    default:
                        switch (cur) {
                            case 'c':
                                if (count == 2) {
                                    throw new IllegalArgumentException("Invalid pattern \"cc\"");
                                }
                                break;
                            case 'd':
                                break;
                            case 'e':
                                break;
                            default:
                                switch (cur) {
                                    case 'Q':
                                        break;
                                    case 'S':
                                        appendFraction(ChronoField.NANO_OF_SECOND, count, count, false);
                                        return;
                                    case 'a':
                                        if (count == 1) {
                                            appendText(field, TextStyle.SHORT);
                                            return;
                                        }
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Too many pattern letters: ");
                                        stringBuilder.append(cur);
                                        throw new IllegalArgumentException(stringBuilder.toString());
                                    case 'h':
                                    case 'k':
                                    case 'm':
                                    case 's':
                                        break;
                                    case 'q':
                                        break;
                                    case 'u':
                                    case 'y':
                                        if (count == 2) {
                                            appendValueReduced(field, 2, 2, ReducedPrinterParser.BASE_DATE);
                                            return;
                                        } else if (count < 4) {
                                            appendValue(field, count, 19, SignStyle.NORMAL);
                                            return;
                                        } else {
                                            appendValue(field, count, 19, SignStyle.EXCEEDS_PAD);
                                            return;
                                        }
                                    default:
                                        if (count == 1) {
                                            appendValue(field);
                                            return;
                                        } else {
                                            appendValue(field, count);
                                            return;
                                        }
                                }
                        }
                }
        }
    }

    public DateTimeFormatterBuilder padNext(int padWidth) {
        return padNext(padWidth, ' ');
    }

    public DateTimeFormatterBuilder padNext(int padWidth, char padChar) {
        if (padWidth >= 1) {
            this.active.padNextWidth = padWidth;
            this.active.padNextChar = padChar;
            this.active.valueParserIndex = -1;
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The pad width must be at least one but was ");
        stringBuilder.append(padWidth);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public DateTimeFormatterBuilder optionalStart() {
        this.active.valueParserIndex = -1;
        this.active = new DateTimeFormatterBuilder(this.active, true);
        return this;
    }

    public DateTimeFormatterBuilder optionalEnd() {
        if (this.active.parent != null) {
            if (this.active.printerParsers.size() > 0) {
                CompositePrinterParser cpp = new CompositePrinterParser(this.active.printerParsers, this.active.optional);
                this.active = this.active.parent;
                appendInternal(cpp);
            } else {
                this.active = this.active.parent;
            }
            return this;
        }
        throw new IllegalStateException("Cannot call optionalEnd() as there was no previous call to optionalStart()");
    }

    private int appendInternal(DateTimePrinterParser pp) {
        Object pp2;
        Objects.requireNonNull((Object) pp, "pp");
        if (this.active.padNextWidth > 0) {
            if (pp != null) {
                pp2 = new PadPrinterParserDecorator(pp, this.active.padNextWidth, this.active.padNextChar);
            }
            this.active.padNextWidth = 0;
            this.active.padNextChar = 0;
        }
        this.active.printerParsers.add(pp2);
        this.active.valueParserIndex = -1;
        return this.active.printerParsers.size() - 1;
    }

    public DateTimeFormatter toFormatter() {
        return toFormatter(Locale.getDefault(Category.FORMAT));
    }

    public DateTimeFormatter toFormatter(Locale locale) {
        return toFormatter(locale, ResolverStyle.SMART, null);
    }

    DateTimeFormatter toFormatter(ResolverStyle resolverStyle, Chronology chrono) {
        return toFormatter(Locale.getDefault(Category.FORMAT), resolverStyle, chrono);
    }

    private DateTimeFormatter toFormatter(Locale locale, ResolverStyle resolverStyle, Chronology chrono) {
        Objects.requireNonNull((Object) locale, "locale");
        while (this.active.parent != null) {
            optionalEnd();
        }
        return new DateTimeFormatter(new CompositePrinterParser(this.printerParsers, false), locale, DecimalStyle.STANDARD, resolverStyle, null, chrono, null);
    }
}
