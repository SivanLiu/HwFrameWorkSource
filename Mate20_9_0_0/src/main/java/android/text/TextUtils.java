package android.text;

import android.content.Context;
import android.content.res.Resources;
import android.icu.lang.UCharacter;
import android.icu.text.CaseMap;
import android.icu.text.Edits;
import android.icu.util.ULocale;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.text.format.DateFormat;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AccessibilityClickableSpan;
import android.text.style.AccessibilityURLSpan;
import android.text.style.AlignmentSpan.Standard;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.EasyEditSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LocaleSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.ScaleXSpan;
import android.text.style.SpellCheckSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuggestionRangeSpan;
import android.text.style.SuggestionSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TtsSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.text.style.UpdateAppearance;
import android.util.Log;
import android.util.Printer;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextUtils {
    public static final int ABSOLUTE_SIZE_SPAN = 16;
    public static final int ACCESSIBILITY_CLICKABLE_SPAN = 25;
    public static final int ACCESSIBILITY_URL_SPAN = 26;
    public static final int ALIGNMENT_SPAN = 1;
    public static final int ANNOTATION = 18;
    public static final int BACKGROUND_COLOR_SPAN = 12;
    public static final int BULLET_SPAN = 8;
    public static final int CAP_MODE_CHARACTERS = 4096;
    public static final int CAP_MODE_SENTENCES = 16384;
    public static final int CAP_MODE_WORDS = 8192;
    public static final Creator<CharSequence> CHAR_SEQUENCE_CREATOR = new Creator<CharSequence>() {
        public CharSequence createFromParcel(Parcel p) {
            int kind = p.readInt();
            String string = p.readString();
            if (string == null) {
                return null;
            }
            if (kind == 1) {
                return string;
            }
            SpannableString sp = new SpannableString(string);
            while (true) {
                kind = p.readInt();
                if (kind == 0) {
                    return sp;
                }
                switch (kind) {
                    case 1:
                        TextUtils.readSpan(p, sp, new Standard(p));
                        break;
                    case 2:
                        TextUtils.readSpan(p, sp, new ForegroundColorSpan(p));
                        break;
                    case 3:
                        TextUtils.readSpan(p, sp, new RelativeSizeSpan(p));
                        break;
                    case 4:
                        TextUtils.readSpan(p, sp, new ScaleXSpan(p));
                        break;
                    case 5:
                        TextUtils.readSpan(p, sp, new StrikethroughSpan(p));
                        break;
                    case 6:
                        TextUtils.readSpan(p, sp, new UnderlineSpan(p));
                        break;
                    case 7:
                        TextUtils.readSpan(p, sp, new StyleSpan(p));
                        break;
                    case 8:
                        TextUtils.readSpan(p, sp, new BulletSpan(p));
                        break;
                    case 9:
                        TextUtils.readSpan(p, sp, new QuoteSpan(p));
                        break;
                    case 10:
                        TextUtils.readSpan(p, sp, new LeadingMarginSpan.Standard(p));
                        break;
                    case 11:
                        TextUtils.readSpan(p, sp, new URLSpan(p));
                        break;
                    case 12:
                        TextUtils.readSpan(p, sp, new BackgroundColorSpan(p));
                        break;
                    case 13:
                        TextUtils.readSpan(p, sp, new TypefaceSpan(p));
                        break;
                    case 14:
                        TextUtils.readSpan(p, sp, new SuperscriptSpan(p));
                        break;
                    case 15:
                        TextUtils.readSpan(p, sp, new SubscriptSpan(p));
                        break;
                    case 16:
                        TextUtils.readSpan(p, sp, new AbsoluteSizeSpan(p));
                        break;
                    case 17:
                        TextUtils.readSpan(p, sp, new TextAppearanceSpan(p));
                        break;
                    case 18:
                        TextUtils.readSpan(p, sp, new Annotation(p));
                        break;
                    case 19:
                        TextUtils.readSpan(p, sp, new SuggestionSpan(p));
                        break;
                    case 20:
                        TextUtils.readSpan(p, sp, new SpellCheckSpan(p));
                        break;
                    case 21:
                        TextUtils.readSpan(p, sp, new SuggestionRangeSpan(p));
                        break;
                    case 22:
                        TextUtils.readSpan(p, sp, new EasyEditSpan(p));
                        break;
                    case 23:
                        TextUtils.readSpan(p, sp, new LocaleSpan(p));
                        break;
                    case 24:
                        TextUtils.readSpan(p, sp, new TtsSpan(p));
                        break;
                    case 25:
                        TextUtils.readSpan(p, sp, new AccessibilityClickableSpan(p));
                        break;
                    case 26:
                        TextUtils.readSpan(p, sp, new AccessibilityURLSpan(p));
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("bogus span encoding ");
                        stringBuilder.append(kind);
                        throw new RuntimeException(stringBuilder.toString());
                }
            }
        }

        public CharSequence[] newArray(int size) {
            return new CharSequence[size];
        }
    };
    public static final int EASY_EDIT_SPAN = 22;
    static final char ELLIPSIS_FILLER = '﻿';
    private static final String ELLIPSIS_NORMAL = "…";
    private static final String ELLIPSIS_TWO_DOTS = "‥";
    private static String[] EMPTY_STRING_ARRAY = new String[0];
    public static final int FIRST_SPAN = 1;
    public static final int FOREGROUND_COLOR_SPAN = 2;
    public static final int LAST_SPAN = 26;
    public static final int LEADING_MARGIN_SPAN = 10;
    public static final int LOCALE_SPAN = 23;
    private static final int PARCEL_SAFE_TEXT_LENGTH = 100000;
    public static final int QUOTE_SPAN = 9;
    public static final int RELATIVE_SIZE_SPAN = 3;
    public static final int SCALE_X_SPAN = 4;
    public static final int SPELL_CHECK_SPAN = 20;
    public static final int STRIKETHROUGH_SPAN = 5;
    public static final int STYLE_SPAN = 7;
    public static final int SUBSCRIPT_SPAN = 15;
    public static final int SUGGESTION_RANGE_SPAN = 21;
    public static final int SUGGESTION_SPAN = 19;
    public static final int SUPERSCRIPT_SPAN = 14;
    private static final String TAG = "TextUtils";
    public static final int TEXT_APPEARANCE_SPAN = 17;
    public static final int TTS_SPAN = 24;
    public static final int TYPEFACE_SPAN = 13;
    public static final int UNDERLINE_SPAN = 6;
    public static final int URL_SPAN = 11;
    private static Object sLock = new Object();
    private static char[] sTemp = null;

    public interface EllipsizeCallback {
        void ellipsized(int i, int i2);
    }

    public interface StringSplitter extends Iterable<String> {
        void setString(String str);
    }

    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE,
        END_SMALL
    }

    private static class Reverser implements CharSequence, GetChars {
        private int mEnd;
        private CharSequence mSource;
        private int mStart;

        public Reverser(CharSequence source, int start, int end) {
            this.mSource = source;
            this.mStart = start;
            this.mEnd = end;
        }

        public int length() {
            return this.mEnd - this.mStart;
        }

        public CharSequence subSequence(int start, int end) {
            char[] buf = new char[(end - start)];
            getChars(start, end, buf, 0);
            return new String(buf);
        }

        public String toString() {
            return subSequence(0, length()).toString();
        }

        public char charAt(int off) {
            return (char) UCharacter.getMirror(this.mSource.charAt((this.mEnd - 1) - off));
        }

        public void getChars(int start, int end, char[] dest, int destoff) {
            TextUtils.getChars(this.mSource, this.mStart + start, this.mStart + end, dest, destoff);
            int i = 0;
            AndroidCharacter.mirror(dest, 0, end - start);
            int len = end - start;
            int n = (end - start) / 2;
            while (i < n) {
                char tmp = dest[destoff + i];
                dest[destoff + i] = dest[((destoff + len) - i) - 1];
                dest[((destoff + len) - i) - 1] = tmp;
                i++;
            }
        }
    }

    public static class SimpleStringSplitter implements StringSplitter, Iterator<String> {
        private char mDelimiter;
        private int mLength;
        private int mPosition;
        private String mString;

        public SimpleStringSplitter(char delimiter) {
            this.mDelimiter = delimiter;
        }

        public void setString(String string) {
            this.mString = string;
            this.mPosition = 0;
            this.mLength = this.mString.length();
        }

        public Iterator<String> iterator() {
            return this;
        }

        public boolean hasNext() {
            return this.mPosition < this.mLength;
        }

        public String next() {
            int end = this.mString.indexOf(this.mDelimiter, this.mPosition);
            if (end == -1) {
                end = this.mLength;
            }
            String nextString = this.mString.substring(this.mPosition, end);
            this.mPosition = end + 1;
            return nextString;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static String getEllipsisString(TruncateAt method) {
        return method == TruncateAt.END_SMALL ? ELLIPSIS_TWO_DOTS : ELLIPSIS_NORMAL;
    }

    private TextUtils() {
    }

    public static void getChars(CharSequence s, int start, int end, char[] dest, int destoff) {
        Class<? extends CharSequence> c = s.getClass();
        if (c == String.class) {
            ((String) s).getChars(start, end, dest, destoff);
        } else if (c == StringBuffer.class) {
            ((StringBuffer) s).getChars(start, end, dest, destoff);
        } else if (c == StringBuilder.class) {
            ((StringBuilder) s).getChars(start, end, dest, destoff);
        } else if (s instanceof GetChars) {
            ((GetChars) s).getChars(start, end, dest, destoff);
        } else {
            int destoff2 = destoff;
            destoff = start;
            while (destoff < end) {
                int destoff3 = destoff2 + 1;
                dest[destoff2] = s.charAt(destoff);
                destoff++;
                destoff2 = destoff3;
            }
        }
    }

    public static int indexOf(CharSequence s, char ch) {
        return indexOf(s, ch, 0);
    }

    public static int indexOf(CharSequence s, char ch, int start) {
        if (s.getClass() == String.class) {
            return ((String) s).indexOf(ch, start);
        }
        return indexOf(s, ch, start, s.length());
    }

    public static int indexOf(CharSequence s, char ch, int start, int end) {
        Class<? extends CharSequence> c = s.getClass();
        if ((s instanceof GetChars) || c == StringBuffer.class || c == StringBuilder.class || c == String.class) {
            char[] temp = obtain(500);
            while (start < end) {
                int segend = start + 500;
                if (segend > end) {
                    segend = end;
                }
                int i = 0;
                getChars(s, start, segend, temp, 0);
                int count = segend - start;
                while (i < count) {
                    if (temp[i] == ch) {
                        recycle(temp);
                        return i + start;
                    }
                    i++;
                }
                start = segend;
            }
            recycle(temp);
            return -1;
        }
        for (int i2 = start; i2 < end; i2++) {
            if (s.charAt(i2) == ch) {
                return i2;
            }
        }
        return -1;
    }

    public static int lastIndexOf(CharSequence s, char ch) {
        return lastIndexOf(s, ch, s.length() - 1);
    }

    public static int lastIndexOf(CharSequence s, char ch, int last) {
        if (s.getClass() == String.class) {
            return ((String) s).lastIndexOf(ch, last);
        }
        return lastIndexOf(s, ch, 0, last);
    }

    public static int lastIndexOf(CharSequence s, char ch, int start, int last) {
        if (last < 0) {
            return -1;
        }
        if (last >= s.length()) {
            last = s.length() - 1;
        }
        int end = last + 1;
        Class<? extends CharSequence> c = s.getClass();
        if ((s instanceof GetChars) || c == StringBuffer.class || c == StringBuilder.class || c == String.class) {
            char[] temp = obtain(500);
            while (start < end) {
                int segstart = end - 500;
                if (segstart < start) {
                    segstart = start;
                }
                getChars(s, segstart, end, temp, 0);
                for (int i = (end - segstart) - 1; i >= 0; i--) {
                    if (temp[i] == ch) {
                        recycle(temp);
                        return i + segstart;
                    }
                }
                end = segstart;
            }
            recycle(temp);
            return -1;
        }
        for (int i2 = end - 1; i2 >= start; i2--) {
            if (s.charAt(i2) == ch) {
                return i2;
            }
        }
        return -1;
    }

    public static int indexOf(CharSequence s, CharSequence needle) {
        return indexOf(s, needle, 0, s.length());
    }

    public static int indexOf(CharSequence s, CharSequence needle, int start) {
        return indexOf(s, needle, start, s.length());
    }

    public static int indexOf(CharSequence s, CharSequence needle, int start, int end) {
        int nlen = needle.length();
        if (nlen == 0) {
            return start;
        }
        char c = needle.charAt(0);
        while (true) {
            start = indexOf(s, c, start);
            if (start > end - nlen || start < 0) {
                return -1;
            }
            if (regionMatches(s, start, needle, 0, nlen)) {
                return start;
            }
            start++;
        }
    }

    public static boolean regionMatches(CharSequence one, int toffset, CharSequence two, int ooffset, int len) {
        int tempLen = 2 * len;
        if (tempLen >= len) {
            char[] temp = obtain(tempLen);
            int i = 0;
            getChars(one, toffset, toffset + len, temp, 0);
            getChars(two, ooffset, ooffset + len, temp, len);
            boolean match = true;
            while (i < len) {
                if (temp[i] != temp[i + len]) {
                    match = false;
                    break;
                }
                i++;
            }
            recycle(temp);
            return match;
        }
        throw new IndexOutOfBoundsException();
    }

    public static String substring(CharSequence source, int start, int end) {
        if (source instanceof String) {
            return ((String) source).substring(start, end);
        }
        if (source instanceof StringBuilder) {
            return ((StringBuilder) source).substring(start, end);
        }
        if (source instanceof StringBuffer) {
            return ((StringBuffer) source).substring(start, end);
        }
        char[] temp = obtain(end - start);
        getChars(source, start, end, temp, 0);
        String ret = new String(temp, 0, end - start);
        recycle(temp);
        return ret;
    }

    public static String join(CharSequence delimiter, Object[] tokens) {
        int length = tokens.length;
        if (length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(tokens[0]);
        for (int i = 1; i < length; i++) {
            sb.append(delimiter);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    public static String join(CharSequence delimiter, Iterable tokens) {
        Iterator<?> it = tokens.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return text.split(expression, -1);
    }

    public static String[] split(String text, Pattern pattern) {
        if (text.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        return pattern.split(text, -1);
    }

    public static CharSequence stringOrSpannedString(CharSequence source) {
        if (source == null) {
            return null;
        }
        if (source instanceof SpannedString) {
            return source;
        }
        if (source instanceof Spanned) {
            return new SpannedString(source);
        }
        return source.toString();
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static String nullIfEmpty(String str) {
        return isEmpty(str) ? null : str;
    }

    public static String emptyIfNull(String str) {
        return str == null ? "" : str;
    }

    public static String firstNotEmpty(String a, String b) {
        return !isEmpty(a) ? a : (String) Preconditions.checkStringNotEmpty(b);
    }

    public static int length(String s) {
        return isEmpty(s) ? 0 : s.length();
    }

    public static String safeIntern(String s) {
        return s != null ? s.intern() : null;
    }

    public static int getTrimmedLength(CharSequence s) {
        int len = s.length();
        int start = 0;
        while (start < len && s.charAt(start) <= ' ') {
            start++;
        }
        int end = len;
        while (end > start && s.charAt(end - 1) <= ' ') {
            end--;
        }
        return end - start;
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) {
            return true;
        }
        if (!(a == null || b == null)) {
            int length = a.length();
            int length2 = length;
            if (length == b.length()) {
                if ((a instanceof String) && (b instanceof String)) {
                    return a.equals(b);
                }
                for (length = 0; length < length2; length++) {
                    if (a.charAt(length) != b.charAt(length)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static CharSequence getReverse(CharSequence source, int start, int end) {
        return new Reverser(source, start, end);
    }

    public static void writeToParcel(CharSequence cs, Parcel p, int parcelableFlags) {
        if (cs instanceof Spanned) {
            p.writeInt(0);
            p.writeString(cs.toString());
            Spanned sp = (Spanned) cs;
            Object[] os = sp.getSpans(0, cs.length(), Object.class);
            for (int i = 0; i < os.length; i++) {
                Object o = os[i];
                ParcelableSpan prop = os[i];
                if (prop instanceof CharacterStyle) {
                    prop = ((CharacterStyle) prop).getUnderlying();
                }
                if (prop instanceof ParcelableSpan) {
                    ParcelableSpan ps = prop;
                    int spanTypeId = ps.getSpanTypeIdInternal();
                    if (spanTypeId < 1 || spanTypeId > 26) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("External class \"");
                        stringBuilder.append(ps.getClass().getSimpleName());
                        stringBuilder.append("\" is attempting to use the frameworks-only ParcelableSpan interface");
                        Log.e(str, stringBuilder.toString());
                    } else {
                        p.writeInt(spanTypeId);
                        ps.writeToParcelInternal(p, parcelableFlags);
                        writeWhere(p, sp, o);
                    }
                }
            }
            p.writeInt(0);
            return;
        }
        p.writeInt(1);
        if (cs != null) {
            p.writeString(cs.toString());
        } else {
            p.writeString(null);
        }
    }

    private static void writeWhere(Parcel p, Spanned sp, Object o) {
        p.writeInt(sp.getSpanStart(o));
        p.writeInt(sp.getSpanEnd(o));
        p.writeInt(sp.getSpanFlags(o));
    }

    public static void dumpSpans(CharSequence cs, Printer printer, String prefix) {
        if (cs instanceof Spanned) {
            Spanned sp = (Spanned) cs;
            int i = 0;
            Object[] os = sp.getSpans(0, cs.length(), Object.class);
            while (true) {
                int i2 = i;
                if (i2 < os.length) {
                    Object o = os[i2];
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append(cs.subSequence(sp.getSpanStart(o), sp.getSpanEnd(o)));
                    stringBuilder.append(": ");
                    stringBuilder.append(Integer.toHexString(System.identityHashCode(o)));
                    stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    stringBuilder.append(o.getClass().getCanonicalName());
                    stringBuilder.append(" (");
                    stringBuilder.append(sp.getSpanStart(o));
                    stringBuilder.append("-");
                    stringBuilder.append(sp.getSpanEnd(o));
                    stringBuilder.append(") fl=#");
                    stringBuilder.append(sp.getSpanFlags(o));
                    printer.println(stringBuilder.toString());
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append(cs);
        stringBuilder2.append(": (no spans)");
        printer.println(stringBuilder2.toString());
    }

    public static CharSequence replace(CharSequence template, String[] sources, CharSequence[] destinations) {
        int i;
        int where;
        CharSequence tb = new SpannableStringBuilder(template);
        int i2 = 0;
        for (i = 0; i < sources.length; i++) {
            where = indexOf(tb, sources[i]);
            if (where >= 0) {
                tb.setSpan(sources[i], where, sources[i].length() + where, 33);
            }
        }
        while (i2 < sources.length) {
            i = tb.getSpanStart(sources[i2]);
            where = tb.getSpanEnd(sources[i2]);
            if (i >= 0) {
                tb.replace(i, where, destinations[i2]);
            }
            i2++;
        }
        return tb;
    }

    public static CharSequence expandTemplate(CharSequence template, CharSequence... values) {
        if (values.length <= 9) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(template);
            int i = 0;
            while (i < ssb.length()) {
                try {
                    if (ssb.charAt(i) == '^') {
                        char next = ssb.charAt(i + 1);
                        if (next == '^') {
                            ssb.delete(i + 1, i + 2);
                            i++;
                        } else if (Character.isDigit(next)) {
                            int which = Character.getNumericValue(next) - 1;
                            StringBuilder stringBuilder;
                            if (which < 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("template requests value ^");
                                stringBuilder.append(which + 1);
                                throw new IllegalArgumentException(stringBuilder.toString());
                            } else if (which < values.length) {
                                ssb.replace(i, i + 2, values[which]);
                                i += values[which].length();
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("template requests value ^");
                                stringBuilder.append(which + 1);
                                stringBuilder.append("; only ");
                                stringBuilder.append(values.length);
                                stringBuilder.append(" provided");
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        }
                    }
                    i++;
                } catch (IndexOutOfBoundsException e) {
                }
            }
            return ssb;
        }
        throw new IllegalArgumentException("max of 9 values are supported");
    }

    public static int getOffsetBefore(CharSequence text, int offset) {
        int i = 0;
        if (offset == 0 || offset == 1) {
            return 0;
        }
        char c = text.charAt(offset - 1);
        if (c < 56320 || c > 57343) {
            offset--;
        } else {
            char c1 = text.charAt(offset - 2);
            if (c1 < 55296 || c1 > 56319) {
                offset--;
            } else {
                offset -= 2;
            }
        }
        if (text instanceof Spanned) {
            ReplacementSpan[] spans = (ReplacementSpan[]) ((Spanned) text).getSpans(offset, offset, ReplacementSpan.class);
            while (i < spans.length) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);
                if (start < offset && end > offset) {
                    offset = start;
                }
                i++;
            }
        }
        return offset;
    }

    public static int getOffsetAfter(CharSequence text, int offset) {
        int len = text.length();
        if (offset == len || offset == len - 1) {
            return len;
        }
        char c = text.charAt(offset);
        if (c < 55296 || c > 56319) {
            offset++;
        } else {
            char c1 = text.charAt(offset + 1);
            if (c1 < 56320 || c1 > 57343) {
                offset++;
            } else {
                offset += 2;
            }
        }
        if (text instanceof Spanned) {
            ReplacementSpan[] spans = (ReplacementSpan[]) ((Spanned) text).getSpans(offset, offset, ReplacementSpan.class);
            for (int i = 0; i < spans.length; i++) {
                int start = ((Spanned) text).getSpanStart(spans[i]);
                int end = ((Spanned) text).getSpanEnd(spans[i]);
                if (start < offset && end > offset) {
                    offset = end;
                }
            }
        }
        return offset;
    }

    private static void readSpan(Parcel p, Spannable sp, Object o) {
        sp.setSpan(o, p.readInt(), p.readInt(), p.readInt());
    }

    public static void copySpansFrom(Spanned source, int start, int end, Class kind, Spannable dest, int destoff) {
        if (kind == null) {
            kind = Object.class;
        }
        Object[] spans = source.getSpans(start, end, kind);
        for (int i = 0; i < spans.length; i++) {
            int st = source.getSpanStart(spans[i]);
            int en = source.getSpanEnd(spans[i]);
            int fl = source.getSpanFlags(spans[i]);
            if (st < start) {
                st = start;
            }
            if (en > end) {
                en = end;
            }
            dest.setSpan(spans[i], (st - start) + destoff, (en - start) + destoff, fl);
        }
    }

    public static CharSequence toUpperCase(Locale locale, CharSequence source, boolean copySpans) {
        Locale locale2 = locale;
        CharSequence charSequence = source;
        Edits edits = new Edits();
        if (copySpans) {
            SpannableStringBuilder result = (SpannableStringBuilder) CaseMap.toUpper().apply(locale2, charSequence, new SpannableStringBuilder(), edits);
            if (!edits.hasChanges()) {
                return charSequence;
            }
            Edits.Iterator iterator = edits.getFineIterator();
            int sourceLength = source.length();
            Spanned spanned = (Spanned) charSequence;
            int i = 0;
            Object[] spans = spanned.getSpans(0, sourceLength, Object.class);
            int length = spans.length;
            while (i < length) {
                int destStart;
                int length2;
                Object span = spans[i];
                int sourceStart = spanned.getSpanStart(span);
                int sourceEnd = spanned.getSpanEnd(span);
                int flags = spanned.getSpanFlags(span);
                if (sourceStart == sourceLength) {
                    destStart = result.length();
                } else {
                    destStart = toUpperMapToDest(iterator, sourceStart);
                }
                if (sourceEnd == sourceLength) {
                    length2 = result.length();
                } else {
                    length2 = toUpperMapToDest(iterator, sourceEnd);
                }
                result.setSpan(span, destStart, length2, flags);
                i++;
                locale2 = locale;
            }
            return result;
        }
        return edits.hasChanges() ? (StringBuilder) CaseMap.toUpper().apply(locale2, charSequence, new StringBuilder(), edits) : charSequence;
    }

    private static int toUpperMapToDest(Edits.Iterator iterator, int sourceIndex) {
        iterator.findSourceIndex(sourceIndex);
        if (sourceIndex == iterator.sourceIndex()) {
            return iterator.destinationIndex();
        }
        if (iterator.hasChange()) {
            return iterator.destinationIndex() + iterator.newLength();
        }
        return iterator.destinationIndex() + (sourceIndex - iterator.sourceIndex());
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint p, float avail, TruncateAt where) {
        return ellipsize(text, p, avail, where, false, null);
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint paint, float avail, TruncateAt where, boolean preserveLength, EllipsizeCallback callback) {
        return ellipsize(text, paint, avail, where, preserveLength, callback, TextDirectionHeuristics.FIRSTSTRONG_LTR, getEllipsisString(where));
    }

    /* JADX WARNING: Removed duplicated region for block: B:85:0x0140  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0140  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0140  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static CharSequence ellipsize(CharSequence text, TextPaint paint, float avail, TruncateAt where, boolean preserveLength, EllipsizeCallback callback, TextDirectionHeuristic textDir, String ellipsis) {
        Throwable th;
        float f;
        TextPaint textPaint;
        CharSequence charSequence = text;
        TruncateAt truncateAt = where;
        EllipsizeCallback ellipsizeCallback = callback;
        CharSequence charSequence2 = ellipsis;
        int len = text.length();
        Spanned sp = null;
        MeasuredParagraph mt = null;
        MeasuredParagraph mt2;
        try {
            mt2 = MeasuredParagraph.buildForMeasurement(paint, charSequence, 0, text.length(), textDir, mt);
            try {
                if (mt2.getWholeWidth() <= avail) {
                    if (ellipsizeCallback != null) {
                        ellipsizeCallback.ellipsized(0, 0);
                    }
                    if (mt2 != null) {
                        mt2.recycle();
                    }
                    return charSequence;
                }
                try {
                    float avail2 = avail - paint.measureText(charSequence2);
                    int left = 0;
                    int right = len;
                    if (avail2 >= 0.0f) {
                        try {
                            if (truncateAt == TruncateAt.START) {
                                right = len - mt2.breakText(len, false, avail2);
                            } else {
                                if (truncateAt != TruncateAt.END) {
                                    if (truncateAt != TruncateAt.END_SMALL) {
                                        right = len - mt2.breakText(len, false, avail2 / 2.0f);
                                        avail2 -= mt2.measure(right, len);
                                        left = mt2.breakText(right, true, avail2);
                                    }
                                }
                                left = mt2.breakText(len, true, avail2);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            f = avail2;
                            if (mt2 != null) {
                            }
                            throw th;
                        }
                    }
                    int right2 = right;
                    if (ellipsizeCallback != null) {
                        try {
                            ellipsizeCallback.ellipsized(left, right2);
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    mt = mt2.getChars();
                    if (charSequence instanceof Spanned) {
                        sp = (Spanned) charSequence;
                    }
                    int removed = right2 - left;
                    int remaining = len - removed;
                    char[] buf;
                    if (preserveLength) {
                        if (remaining > 0 && removed >= ellipsis.length()) {
                            charSequence2.getChars(0, ellipsis.length(), mt, left);
                            left += ellipsis.length();
                        }
                        for (int i = left; i < right2; i++) {
                            mt[i] = ELLIPSIS_FILLER;
                        }
                        String s = new String(mt, 0, len);
                        if (sp == null) {
                            if (mt2 != null) {
                                mt2.recycle();
                            }
                            return s;
                        }
                        SpannableString ss = new SpannableString(s);
                        buf = mt;
                        copySpansFrom(sp, 0, len, Object.class, ss, null);
                        if (mt2 != null) {
                            mt2.recycle();
                        }
                        return ss;
                    }
                    buf = mt;
                    if (remaining == 0) {
                        String str = "";
                        if (mt2 != null) {
                            mt2.recycle();
                        }
                        return str;
                    } else if (sp == null) {
                        StringBuilder sb = new StringBuilder(remaining + ellipsis.length());
                        sb.append(buf, 0, left);
                        sb.append(charSequence2);
                        sb.append(buf, right2, len - right2);
                        String stringBuilder = sb.toString();
                        if (mt2 != null) {
                            mt2.recycle();
                        }
                        return stringBuilder;
                    } else {
                        avail2 = new SpannableStringBuilder();
                        avail2.append(charSequence, 0, left);
                        avail2.append(charSequence2);
                        avail2.append(charSequence, right2, len);
                        if (mt2 != null) {
                            mt2.recycle();
                        }
                        return avail2;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    if (mt2 != null) {
                        mt2.recycle();
                    }
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                textPaint = paint;
                if (mt2 != null) {
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            textPaint = paint;
            f = avail;
            mt2 = mt;
            if (mt2 != null) {
            }
            throw th;
        }
    }

    public static CharSequence listEllipsize(Context context, List<CharSequence> elements, String separator, TextPaint paint, float avail, int moreId) {
        if (elements == null) {
            return "";
        }
        int totalLen = elements.size();
        if (totalLen == 0) {
            return "";
        }
        Resources res;
        BidiFormatter bidiFormatter;
        int i;
        if (context == null) {
            res = null;
            bidiFormatter = BidiFormatter.getInstance();
        } else {
            res = context.getResources();
            bidiFormatter = BidiFormatter.getInstance(res.getConfiguration().getLocales().get(0));
        }
        SpannableStringBuilder output = new SpannableStringBuilder();
        int[] endIndexes = new int[totalLen];
        for (i = 0; i < totalLen; i++) {
            output.append(bidiFormatter.unicodeWrap((CharSequence) elements.get(i)));
            if (i != totalLen - 1) {
                output.append((CharSequence) separator);
            }
            endIndexes[i] = output.length();
        }
        for (i = totalLen - 1; i >= 0; i--) {
            output.delete(endIndexes[i], output.length());
            int remainingElements = (totalLen - i) - 1;
            if (remainingElements > 0) {
                CharSequence morePiece;
                if (res == null) {
                    morePiece = ELLIPSIS_NORMAL;
                } else {
                    morePiece = res.getQuantityString(moreId, remainingElements, new Object[]{Integer.valueOf(remainingElements)});
                }
                output.append(bidiFormatter.unicodeWrap(morePiece));
            }
            if (paint.measureText(output, 0, output.length()) <= avail) {
                return output;
            }
        }
        return "";
    }

    @Deprecated
    public static CharSequence commaEllipsize(CharSequence text, TextPaint p, float avail, String oneMore, String more) {
        return commaEllipsize(text, p, avail, oneMore, more, TextDirectionHeuristics.FIRSTSTRONG_LTR);
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x010c  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0111  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x010c  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0111  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x010c  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0111  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public static CharSequence commaEllipsize(CharSequence text, TextPaint p, float avail, String oneMore, String more, TextDirectionHeuristic textDir) {
        Throwable th;
        CharSequence charSequence = text;
        MeasuredParagraph mt = null;
        MeasuredParagraph tempMt = null;
        String str;
        try {
            int len = text.length();
            mt = MeasuredParagraph.buildForMeasurement(p, charSequence, 0, len, textDir, null);
            float width = mt.getWholeWidth();
            if (width <= avail) {
                if (mt != null) {
                    mt.recycle();
                }
                if (tempMt != null) {
                    tempMt.recycle();
                }
                return charSequence;
            }
            char c;
            int len2;
            float width2;
            char[] buf = mt.getChars();
            int commaCount = 0;
            int i = 0;
            while (true) {
                c = ',';
                if (i >= len) {
                    break;
                }
                if (buf[i] == ',') {
                    commaCount++;
                }
                i++;
            }
            i = commaCount + 1;
            int ok = 0;
            String okFormat = "";
            int w = 0;
            int count = 0;
            float[] widths = mt.getWidths().getRawArray();
            MeasuredParagraph tempMt2 = tempMt;
            int remaining = i;
            i = 0;
            while (i < len) {
                try {
                    w = (int) (((float) w) + widths[i]);
                    if (buf[i] == c) {
                        String format;
                        count++;
                        remaining--;
                        if (remaining == 1) {
                            format = new StringBuilder();
                            format.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                            try {
                                format.append(oneMore);
                                format = format.toString();
                                len2 = len;
                                width2 = width;
                                width = more;
                            } catch (Throwable th2) {
                                th = th2;
                                tempMt = tempMt2;
                                if (mt != null) {
                                }
                                if (tempMt != null) {
                                }
                                throw th;
                            }
                        }
                        str = oneMore;
                        StringBuilder stringBuilder = new StringBuilder();
                        len2 = len;
                        stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        width2 = width;
                        stringBuilder.append(String.format(more, new Object[]{Integer.valueOf(remaining)}));
                        format = stringBuilder.toString();
                        String okFormat2 = format;
                        MeasuredParagraph tempMt3 = MeasuredParagraph.buildForMeasurement(p, okFormat2, 0, okFormat2.length(), textDir, tempMt2);
                        try {
                            if (((float) w) + tempMt3.getWholeWidth() <= avail) {
                                okFormat = okFormat2;
                                ok = i + 1;
                            }
                            tempMt2 = tempMt3;
                        } catch (Throwable th3) {
                            th = th3;
                            tempMt = tempMt3;
                            if (mt != null) {
                            }
                            if (tempMt != null) {
                            }
                            throw th;
                        }
                    }
                    str = oneMore;
                    len2 = len;
                    width2 = width;
                    i++;
                    len = len2;
                    width = width2;
                    c = ',';
                } catch (Throwable th4) {
                    th = th4;
                    str = oneMore;
                    tempMt = tempMt2;
                    if (mt != null) {
                    }
                    if (tempMt != null) {
                    }
                    throw th;
                }
            }
            str = oneMore;
            len2 = len;
            width2 = width;
            try {
                SpannableStringBuilder out = new SpannableStringBuilder(okFormat);
                out.insert(0, charSequence, 0, ok);
                if (mt != null) {
                    mt.recycle();
                }
                MeasuredParagraph tempMt4 = tempMt2;
                if (tempMt4 != null) {
                    tempMt4.recycle();
                }
                return out;
            } catch (Throwable th5) {
                th = th5;
                tempMt = tempMt2;
            }
        } catch (Throwable th6) {
            th = th6;
            str = oneMore;
            if (mt != null) {
                mt.recycle();
            }
            if (tempMt != null) {
                tempMt.recycle();
            }
            throw th;
        }
    }

    static boolean couldAffectRtl(char c) {
        return (1424 <= c && c <= 2303) || c == 8206 || c == 8207 || ((8234 <= c && c <= 8238) || ((8294 <= c && c <= 8297) || ((55296 <= c && c <= 57343) || ((64285 <= c && c <= 65023) || (65136 <= c && c <= 65278)))));
    }

    static boolean doesNotNeedBidi(char[] text, int start, int len) {
        int end = start + len;
        for (int i = start; i < end; i++) {
            if (couldAffectRtl(text[i])) {
                return false;
            }
        }
        return true;
    }

    static char[] obtain(int len) {
        char[] buf;
        synchronized (sLock) {
            buf = sTemp;
            sTemp = null;
        }
        if (buf == null || buf.length < len) {
            return ArrayUtils.newUnpaddedCharArray(len);
        }
        return buf;
    }

    static void recycle(char[] temp) {
        if (temp.length <= 1000) {
            synchronized (sLock) {
                sTemp = temp;
            }
        }
    }

    public static String htmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"') {
                sb.append("&quot;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c != '>') {
                switch (c) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '\'':
                        sb.append("&#39;");
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else {
                sb.append("&gt;");
            }
        }
        return sb.toString();
    }

    public static CharSequence concat(CharSequence... text) {
        if (text.length == 0) {
            return "";
        }
        int i = 0;
        if (text.length == 1) {
            return text[0];
        }
        int length;
        CharSequence piece;
        boolean spanned = false;
        for (CharSequence piece2 : text) {
            if (piece2 instanceof Spanned) {
                spanned = true;
                break;
            }
        }
        if (spanned) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            length = text.length;
            while (i < length) {
                piece2 = text[i];
                ssb.append(piece2 == null ? "null" : piece2);
                i++;
            }
            return new SpannedString(ssb);
        }
        StringBuilder sb = new StringBuilder();
        length = text.length;
        while (i < length) {
            sb.append(text[i]);
            i++;
        }
        return sb.toString();
    }

    public static boolean isGraphic(CharSequence str) {
        int len = str.length();
        int i = 0;
        while (i < len) {
            int cp = Character.codePointAt(str, i);
            int gc = Character.getType(cp);
            if (gc != 15 && gc != 16 && gc != 19 && gc != 0 && gc != 13 && gc != 14 && gc != 12) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    @Deprecated
    public static boolean isGraphic(char c) {
        int gc = Character.getType(c);
        return (gc == 15 || gc == 16 || gc == 19 || gc == 0 || gc == 13 || gc == 14 || gc == 12) ? false : true;
    }

    public static boolean isDigitsOnly(CharSequence str) {
        int len = str.length();
        int i = 0;
        while (i < len) {
            int cp = Character.codePointAt(str, i);
            if (!Character.isDigit(cp)) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    public static boolean isPrintableAscii(char c) {
        return (' ' <= c && c <= '~') || c == 13 || c == 10;
    }

    public static boolean isPrintableAsciiOnly(CharSequence str) {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (!isPrintableAscii(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int getCapsMode(CharSequence cs, int off, int reqModes) {
        if (off < 0) {
            return 0;
        }
        int mode = 0;
        if ((reqModes & 4096) != 0) {
            mode = 0 | 4096;
        }
        if ((reqModes & 24576) == 0) {
            return mode;
        }
        char charAt;
        int i = off;
        while (i > 0) {
            char c = cs.charAt(i - 1);
            if (c != '\"' && c != DateFormat.QUOTE && Character.getType(c) != 21) {
                break;
            }
            i--;
        }
        int j = i;
        while (j > 0) {
            charAt = cs.charAt(j - 1);
            char c2 = charAt;
            if (charAt != ' ' && c2 != 9) {
                break;
            }
            j--;
        }
        if (j == 0 || cs.charAt(j - 1) == 10) {
            return mode | 8192;
        }
        if ((reqModes & 16384) == 0) {
            if (i != j) {
                mode |= 8192;
            }
            return mode;
        } else if (i == j) {
            return mode;
        } else {
            while (j > 0) {
                charAt = cs.charAt(j - 1);
                if (charAt != '\"' && charAt != DateFormat.QUOTE && Character.getType(charAt) != 22) {
                    break;
                }
                j--;
            }
            if (j > 0) {
                char c3 = cs.charAt(j - 1);
                if (c3 == '.' || c3 == '?' || c3 == '!') {
                    if (c3 == '.') {
                        for (int k = j - 2; k >= 0; k--) {
                            c3 = cs.charAt(k);
                            if (c3 == '.') {
                                return mode;
                            }
                            if (!Character.isLetter(c3)) {
                                break;
                            }
                        }
                    }
                    return mode | 16384;
                }
            }
            return mode;
        }
    }

    public static boolean delimitedStringContains(String delimitedString, char delimiter, String item) {
        if (isEmpty(delimitedString) || isEmpty(item)) {
            return false;
        }
        int pos = -1;
        int length = delimitedString.length();
        while (true) {
            int indexOf = delimitedString.indexOf(item, pos + 1);
            pos = indexOf;
            if (indexOf == -1) {
                return false;
            }
            if (pos <= 0 || delimitedString.charAt(pos - 1) == delimiter) {
                indexOf = item.length() + pos;
                if (indexOf == length || delimitedString.charAt(indexOf) == delimiter) {
                    return true;
                }
            }
        }
    }

    public static <T> T[] removeEmptySpans(T[] spans, Spanned spanned, Class<T> klass) {
        int count = 0;
        T[] copy = null;
        for (int i = 0; i < spans.length; i++) {
            T span = spans[i];
            if (spanned.getSpanStart(span) == spanned.getSpanEnd(span)) {
                if (copy == null) {
                    copy = (Object[]) Array.newInstance(klass, spans.length - 1);
                    System.arraycopy(spans, 0, copy, 0, i);
                    count = i;
                }
            } else if (copy != null) {
                copy[count] = span;
                count++;
            }
        }
        if (copy == null) {
            return spans;
        }
        Object[] result = (Object[]) Array.newInstance(klass, count);
        System.arraycopy(copy, 0, result, 0, count);
        return result;
    }

    public static long packRangeInLong(int start, int end) {
        return (((long) start) << 32) | ((long) end);
    }

    public static int unpackRangeStartFromLong(long range) {
        return (int) (range >>> 32);
    }

    public static int unpackRangeEndFromLong(long range) {
        return (int) (4294967295L & range);
    }

    public static int getLayoutDirectionFromLocale(Locale locale) {
        if ((locale == null || locale.equals(Locale.ROOT) || !ULocale.forLocale(locale).isRightToLeft()) && !SystemProperties.getBoolean(Global.DEVELOPMENT_FORCE_RTL, false)) {
            return 0;
        }
        return 1;
    }

    public static CharSequence formatSelectedCount(int count) {
        return Resources.getSystem().getQuantityString(18153499, count, new Object[]{Integer.valueOf(count)});
    }

    public static boolean hasStyleSpan(Spanned spanned) {
        Preconditions.checkArgument(spanned != null);
        for (Class<?> clazz : new Class[]{CharacterStyle.class, ParagraphStyle.class, UpdateAppearance.class}) {
            if (spanned.nextSpanTransition(-1, spanned.length(), clazz) < spanned.length()) {
                return true;
            }
        }
        return false;
    }

    public static CharSequence trimNoCopySpans(CharSequence charSequence) {
        if (charSequence == null || !(charSequence instanceof Spanned)) {
            return charSequence;
        }
        return new SpannableStringBuilder(charSequence);
    }

    public static void wrap(StringBuilder builder, String start, String end) {
        builder.insert(0, start);
        builder.append(end);
    }

    public static <T extends CharSequence> T trimToParcelableSize(T text) {
        return trimToSize(text, 100000);
    }

    public static <T extends CharSequence> T trimToSize(T text, int size) {
        Preconditions.checkArgument(size > 0);
        if (isEmpty(text) || text.length() <= size) {
            return text;
        }
        if (Character.isHighSurrogate(text.charAt(size - 1)) && Character.isLowSurrogate(text.charAt(size))) {
            size--;
        }
        return text.subSequence(0, size);
    }
}
