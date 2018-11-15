package android.icu.text;

import android.icu.impl.BMPSet;
import android.icu.impl.Norm2AllModes;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.RuleCharacterIterator;
import android.icu.impl.SortedSetRelation;
import android.icu.impl.StringRange;
import android.icu.impl.UBiDiProps;
import android.icu.impl.UCaseProps;
import android.icu.impl.UCharacterProperty;
import android.icu.impl.UPropertyAliases;
import android.icu.impl.UnicodeSetStringSpan;
import android.icu.impl.Utility;
import android.icu.lang.CharSequences;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.lang.UScript;
import android.icu.util.Freezable;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.OutputInt;
import android.icu.util.ULocale;
import android.icu.util.VersionInfo;
import dalvik.bytecode.Opcodes;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

public class UnicodeSet extends UnicodeFilter implements Iterable<String>, Comparable<UnicodeSet>, Freezable<UnicodeSet> {
    static final /* synthetic */ boolean -assertionsDisabled = (UnicodeSet.class.desiredAssertionStatus() ^ 1);
    public static final int ADD_CASE_MAPPINGS = 4;
    public static final UnicodeSet ALL_CODE_POINTS = new UnicodeSet(0, 1114111).freeze();
    private static final String ANY_ID = "ANY";
    private static final String ASCII_ID = "ASCII";
    private static final String ASSIGNED = "Assigned";
    public static final int CASE = 2;
    public static final int CASE_INSENSITIVE = 2;
    public static final UnicodeSet EMPTY = new UnicodeSet().freeze();
    private static final int GROW_EXTRA = 16;
    private static final int HIGH = 1114112;
    public static final int IGNORE_SPACE = 1;
    private static UnicodeSet[] INCLUSIONS = null;
    private static final int LAST0_START = 0;
    private static final int LAST1_RANGE = 1;
    private static final int LAST2_SET = 2;
    private static final int LOW = 0;
    public static final int MAX_VALUE = 1114111;
    public static final int MIN_VALUE = 0;
    private static final int MODE0_NONE = 0;
    private static final int MODE1_INBRACKET = 1;
    private static final int MODE2_OUTBRACKET = 2;
    private static final VersionInfo NO_VERSION = VersionInfo.getInstance(0, 0, 0, 0);
    private static final int SETMODE0_NONE = 0;
    private static final int SETMODE1_UNICODESET = 1;
    private static final int SETMODE2_PROPERTYPAT = 2;
    private static final int SETMODE3_PREPARSED = 3;
    private static final int START_EXTRA = 16;
    private static XSymbolTable XSYMBOL_TABLE = null;
    private volatile BMPSet bmpSet;
    private int[] buffer;
    private int len;
    private int[] list;
    private String pat;
    private int[] rangeList;
    private volatile UnicodeSetStringSpan stringSpan;
    TreeSet<String> strings;

    public enum ComparisonStyle {
        SHORTER_FIRST,
        LEXICOGRAPHIC,
        LONGER_FIRST
    }

    public static class EntryRange {
        public int codepoint;
        public int codepointEnd;

        EntryRange() {
        }

        public String toString() {
            StringBuilder stringBuilder;
            StringBuilder b = new StringBuilder();
            if (this.codepoint == this.codepointEnd) {
                stringBuilder = (StringBuilder) UnicodeSet._appendToPat((Appendable) b, this.codepoint, false);
            } else {
                stringBuilder = (StringBuilder) UnicodeSet._appendToPat(((StringBuilder) UnicodeSet._appendToPat((Appendable) b, this.codepoint, false)).append('-'), this.codepointEnd, false);
            }
            return stringBuilder.toString();
        }
    }

    private class EntryRangeIterable implements Iterable<EntryRange> {
        private EntryRangeIterable() {
        }

        public Iterator<EntryRange> iterator() {
            return new EntryRangeIterator();
        }
    }

    private class EntryRangeIterator implements Iterator<EntryRange> {
        int pos;
        EntryRange result;

        private EntryRangeIterator() {
            this.result = new EntryRange();
        }

        public boolean hasNext() {
            return this.pos < UnicodeSet.this.len + -1;
        }

        public EntryRange next() {
            if (this.pos < UnicodeSet.this.len - 1) {
                EntryRange entryRange = this.result;
                int[] -get2 = UnicodeSet.this.list;
                int i = this.pos;
                this.pos = i + 1;
                entryRange.codepoint = -get2[i];
                entryRange = this.result;
                -get2 = UnicodeSet.this.list;
                i = this.pos;
                this.pos = i + 1;
                entryRange.codepointEnd = -get2[i] - 1;
                return this.result;
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private interface Filter {
        boolean contains(int i);
    }

    private static class GeneralCategoryMaskFilter implements Filter {
        int mask;

        GeneralCategoryMaskFilter(int mask) {
            this.mask = mask;
        }

        public boolean contains(int ch) {
            return ((1 << UCharacter.getType(ch)) & this.mask) != 0;
        }
    }

    private static class IntPropertyFilter implements Filter {
        int prop;
        int value;

        IntPropertyFilter(int prop, int value) {
            this.prop = prop;
            this.value = value;
        }

        public boolean contains(int ch) {
            return UCharacter.getIntPropertyValue(ch, this.prop) == this.value;
        }
    }

    private static class NumericValueFilter implements Filter {
        double value;

        NumericValueFilter(double value) {
            this.value = value;
        }

        public boolean contains(int ch) {
            return UCharacter.getUnicodeNumericValue(ch) == this.value;
        }
    }

    private static class ScriptExtensionsFilter implements Filter {
        int script;

        ScriptExtensionsFilter(int script) {
            this.script = script;
        }

        public boolean contains(int c) {
            return UScript.hasScript(c, this.script);
        }
    }

    public enum SpanCondition {
        NOT_CONTAINED,
        CONTAINED,
        SIMPLE,
        CONDITION_COUNT
    }

    private static class UnicodeSetIterator2 implements Iterator<String> {
        private char[] buffer;
        private int current;
        private int item;
        private int len;
        private int limit;
        private int[] sourceList;
        private TreeSet<String> sourceStrings;
        private Iterator<String> stringIterator;

        UnicodeSetIterator2(UnicodeSet source) {
            this.len = source.len - 1;
            if (this.len > 0) {
                this.sourceStrings = source.strings;
                this.sourceList = source.list;
                int[] iArr = this.sourceList;
                int i = this.item;
                this.item = i + 1;
                this.current = iArr[i];
                iArr = this.sourceList;
                i = this.item;
                this.item = i + 1;
                this.limit = iArr[i];
                return;
            }
            this.stringIterator = source.strings.iterator();
            this.sourceList = null;
        }

        public boolean hasNext() {
            return this.sourceList == null ? this.stringIterator.hasNext() : true;
        }

        public String next() {
            if (this.sourceList == null) {
                return (String) this.stringIterator.next();
            }
            int codepoint = this.current;
            this.current = codepoint + 1;
            if (this.current >= this.limit) {
                if (this.item >= this.len) {
                    this.stringIterator = this.sourceStrings.iterator();
                    this.sourceList = null;
                } else {
                    int[] iArr = this.sourceList;
                    int i = this.item;
                    this.item = i + 1;
                    this.current = iArr[i];
                    iArr = this.sourceList;
                    i = this.item;
                    this.item = i + 1;
                    this.limit = iArr[i];
                }
            }
            if (codepoint <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                return String.valueOf((char) codepoint);
            }
            if (this.buffer == null) {
                this.buffer = new char[2];
            }
            int offset = codepoint - 65536;
            this.buffer[0] = (char) ((offset >>> 10) + 55296);
            this.buffer[1] = (char) ((offset & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE);
            return String.valueOf(this.buffer);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class VersionFilter implements Filter {
        VersionInfo version;

        VersionFilter(VersionInfo version) {
            this.version = version;
        }

        public boolean contains(int ch) {
            VersionInfo v = UCharacter.getAge(ch);
            if (Utility.sameObjects(v, UnicodeSet.NO_VERSION) || v.compareTo(this.version) > 0) {
                return false;
            }
            return true;
        }
    }

    public static abstract class XSymbolTable implements SymbolTable {
        public UnicodeMatcher lookupMatcher(int i) {
            return null;
        }

        public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
            return false;
        }

        public char[] lookup(String s) {
            return null;
        }

        public String parseReference(String text, ParsePosition pos, int limit) {
            return null;
        }
    }

    public UnicodeSet() {
        this.strings = new TreeSet();
        this.pat = null;
        this.list = new int[17];
        int[] iArr = this.list;
        int i = this.len;
        this.len = i + 1;
        iArr[i] = 1114112;
    }

    public UnicodeSet(UnicodeSet other) {
        this.strings = new TreeSet();
        this.pat = null;
        set(other);
    }

    public UnicodeSet(int start, int end) {
        this();
        complement(start, end);
    }

    public UnicodeSet(int... pairs) {
        this.strings = new TreeSet();
        this.pat = null;
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Must have even number of integers");
        }
        this.list = new int[(pairs.length + 1)];
        this.len = this.list.length;
        int last = -1;
        int i = 0;
        while (i < pairs.length) {
            int start = pairs[i];
            if (last >= start) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            int i2 = i + 1;
            last = start;
            this.list[i] = start;
            int end = pairs[i2] + 1;
            if (start >= end) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            i = i2 + 1;
            last = end;
            this.list[i2] = end;
        }
        this.list[i] = 1114112;
    }

    public UnicodeSet(String pattern) {
        this();
        applyPattern(pattern, null, null, 1);
    }

    public UnicodeSet(String pattern, boolean ignoreWhitespace) {
        this();
        applyPattern(pattern, null, null, ignoreWhitespace ? 1 : 0);
    }

    public UnicodeSet(String pattern, int options) {
        this();
        applyPattern(pattern, null, null, options);
    }

    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols) {
        this();
        applyPattern(pattern, pos, symbols, 1);
    }

    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols, int options) {
        this();
        applyPattern(pattern, pos, symbols, options);
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        UnicodeSet result = new UnicodeSet(this);
        result.bmpSet = this.bmpSet;
        result.stringSpan = this.stringSpan;
        return result;
    }

    public UnicodeSet set(int start, int end) {
        checkFrozen();
        clear();
        complement(start, end);
        return this;
    }

    public UnicodeSet set(UnicodeSet other) {
        checkFrozen();
        this.list = (int[]) other.list.clone();
        this.len = other.len;
        this.pat = other.pat;
        this.strings = new TreeSet(other.strings);
        return this;
    }

    public final UnicodeSet applyPattern(String pattern) {
        checkFrozen();
        return applyPattern(pattern, null, null, 1);
    }

    public UnicodeSet applyPattern(String pattern, boolean ignoreWhitespace) {
        checkFrozen();
        return applyPattern(pattern, null, null, ignoreWhitespace ? 1 : 0);
    }

    public UnicodeSet applyPattern(String pattern, int options) {
        checkFrozen();
        return applyPattern(pattern, null, null, options);
    }

    public static boolean resemblesPattern(String pattern, int pos) {
        if (pos + 1 >= pattern.length() || pattern.charAt(pos) != '[') {
            return resemblesPropertyPattern(pattern, pos);
        }
        return true;
    }

    private static void appendCodePoint(Appendable app, int c) {
        if (!-assertionsDisabled && (c < 0 || c > 1114111)) {
            throw new AssertionError();
        } else if (c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            try {
                app.append((char) c);
            } catch (Throwable e) {
                throw new ICUUncheckedIOException(e);
            }
        } else {
            app.append(UTF16.getLeadSurrogate(c)).append(UTF16.getTrailSurrogate(c));
        }
    }

    private static void append(Appendable app, CharSequence s) {
        try {
            app.append(s);
        } catch (Throwable e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static <T extends Appendable> T _appendToPat(T buf, String s, boolean escapeUnprintable) {
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            _appendToPat((Appendable) buf, cp, escapeUnprintable);
            i += Character.charCount(cp);
        }
        return buf;
    }

    private static <T extends Appendable> T _appendToPat(T buf, int c, boolean escapeUnprintable) {
        if (escapeUnprintable) {
            try {
                if (Utility.isUnprintable(c) && Utility.escapeUnprintable(buf, c)) {
                    return buf;
                }
            } catch (Throwable e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        switch (c) {
            case 36:
            case 38:
            case 45:
            case 58:
            case 91:
            case 92:
            case 93:
            case 94:
            case 123:
            case 125:
                buf.append(PatternTokenizer.BACK_SLASH);
                break;
            default:
                if (PatternProps.isWhiteSpace(c)) {
                    buf.append(PatternTokenizer.BACK_SLASH);
                    break;
                }
                break;
        }
        appendCodePoint(buf, c);
        return buf;
    }

    public String toPattern(boolean escapeUnprintable) {
        if (this.pat == null || (escapeUnprintable ^ 1) == 0) {
            return ((StringBuilder) _toPattern(new StringBuilder(), escapeUnprintable)).toString();
        }
        return this.pat;
    }

    private <T extends Appendable> T _toPattern(T result, boolean escapeUnprintable) {
        if (this.pat == null) {
            return appendNewPattern(result, escapeUnprintable, true);
        }
        if (escapeUnprintable) {
            boolean oddNumberOfBackslashes = false;
            int i = 0;
            while (i < this.pat.length()) {
                int c = this.pat.codePointAt(i);
                i += Character.charCount(c);
                if (Utility.isUnprintable(c)) {
                    Utility.escapeUnprintable(result, c);
                    oddNumberOfBackslashes = false;
                } else if (oddNumberOfBackslashes || c != 92) {
                    if (oddNumberOfBackslashes) {
                        result.append(PatternTokenizer.BACK_SLASH);
                    }
                    appendCodePoint(result, c);
                    oddNumberOfBackslashes = false;
                } else {
                    oddNumberOfBackslashes = true;
                }
            }
            if (oddNumberOfBackslashes) {
                result.append(PatternTokenizer.BACK_SLASH);
            }
            return result;
        }
        try {
            result.append(this.pat);
            return result;
        } catch (Throwable e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public StringBuffer _generatePattern(StringBuffer result, boolean escapeUnprintable) {
        return _generatePattern(result, escapeUnprintable, true);
    }

    public StringBuffer _generatePattern(StringBuffer result, boolean escapeUnprintable, boolean includeStrings) {
        return (StringBuffer) appendNewPattern(result, escapeUnprintable, includeStrings);
    }

    private <T extends Appendable> T appendNewPattern(T result, boolean escapeUnprintable, boolean includeStrings) {
        try {
            result.append('[');
            int count = getRangeCount();
            int i;
            int start;
            int end;
            if (count > 1 && getRangeStart(0) == 0 && getRangeEnd(count - 1) == 1114111) {
                result.append('^');
                for (i = 1; i < count; i++) {
                    start = getRangeEnd(i - 1) + 1;
                    end = getRangeStart(i) - 1;
                    _appendToPat((Appendable) result, start, escapeUnprintable);
                    if (start != end) {
                        if (start + 1 != end) {
                            result.append('-');
                        }
                        _appendToPat((Appendable) result, end, escapeUnprintable);
                    }
                }
            } else {
                for (i = 0; i < count; i++) {
                    start = getRangeStart(i);
                    end = getRangeEnd(i);
                    _appendToPat((Appendable) result, start, escapeUnprintable);
                    if (start != end) {
                        if (start + 1 != end) {
                            result.append('-');
                        }
                        _appendToPat((Appendable) result, end, escapeUnprintable);
                    }
                }
            }
            if (includeStrings && this.strings.size() > 0) {
                for (String s : this.strings) {
                    result.append('{');
                    _appendToPat((Appendable) result, s, escapeUnprintable);
                    result.append('}');
                }
            }
            result.append(']');
            return result;
        } catch (Throwable e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public int size() {
        int n = 0;
        for (int i = 0; i < getRangeCount(); i++) {
            n += (getRangeEnd(i) - getRangeStart(i)) + 1;
        }
        return this.strings.size() + n;
    }

    public boolean isEmpty() {
        return this.len == 1 && this.strings.size() == 0;
    }

    public boolean matchesIndexValue(int v) {
        for (int i = 0; i < getRangeCount(); i++) {
            int low = getRangeStart(i);
            int high = getRangeEnd(i);
            if ((low & -256) == (high & -256)) {
                if ((low & 255) <= v && v <= (high & 255)) {
                    return true;
                }
            } else if ((low & 255) <= v || v <= (high & 255)) {
                return true;
            }
        }
        if (this.strings.size() != 0) {
            for (String s : this.strings) {
                if ((UTF16.charAt(s, 0) & 255) == v) {
                    return true;
                }
            }
        }
        return false;
    }

    public int matches(Replaceable text, int[] offset, int limit, boolean incremental) {
        if (offset[0] != limit) {
            if (this.strings.size() != 0) {
                boolean forward = offset[0] < limit;
                char firstChar = text.charAt(offset[0]);
                int highWaterLength = 0;
                for (String trial : this.strings) {
                    char c = trial.charAt(forward ? 0 : trial.length() - 1);
                    if (forward && c > firstChar) {
                        break;
                    } else if (c == firstChar) {
                        int length = matchRest(text, offset[0], limit, trial);
                        if (incremental) {
                            if (length == (forward ? limit - offset[0] : offset[0] - limit)) {
                                return 1;
                            }
                        }
                        if (length == trial.length()) {
                            if (length > highWaterLength) {
                                highWaterLength = length;
                            }
                            if (forward && length < highWaterLength) {
                                break;
                            }
                        }
                        continue;
                    }
                }
                if (highWaterLength != 0) {
                    int i = offset[0];
                    if (!forward) {
                        highWaterLength = -highWaterLength;
                    }
                    offset[0] = i + highWaterLength;
                    return 2;
                }
            }
            return super.matches(text, offset, limit, incremental);
        } else if (!contains((int) DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH)) {
            return 0;
        } else {
            return incremental ? 1 : 2;
        }
    }

    private static int matchRest(Replaceable text, int start, int limit, String s) {
        int maxLen;
        int slen = s.length();
        int i;
        if (start < limit) {
            maxLen = limit - start;
            if (maxLen > slen) {
                maxLen = slen;
            }
            for (i = 1; i < maxLen; i++) {
                if (text.charAt(start + i) != s.charAt(i)) {
                    return 0;
                }
            }
        } else {
            maxLen = start - limit;
            if (maxLen > slen) {
                maxLen = slen;
            }
            slen--;
            for (i = 1; i < maxLen; i++) {
                if (text.charAt(start - i) != s.charAt(slen - i)) {
                    return 0;
                }
            }
        }
        return maxLen;
    }

    @Deprecated
    public int matchesAt(CharSequence text, int offset) {
        int lastLen = -1;
        if (this.strings.size() != 0) {
            char firstChar = text.charAt(offset);
            CharSequence trial = null;
            Iterator<String> it = this.strings.iterator();
            while (it.hasNext()) {
                String trial2 = (String) it.next();
                char firstStringChar = trial2.charAt(0);
                if (firstStringChar >= firstChar && firstStringChar > firstChar) {
                    break;
                }
            }
            while (true) {
                int tempLen = matchesAt(text, offset, trial);
                if (lastLen > tempLen) {
                    break;
                }
                lastLen = tempLen;
                if (!it.hasNext()) {
                    break;
                }
                trial = (String) it.next();
            }
        }
        if (lastLen < 2) {
            int cp = UTF16.charAt(text, offset);
            if (contains(cp)) {
                lastLen = UTF16.getCharCount(cp);
            }
        }
        return offset + lastLen;
    }

    private static int matchesAt(CharSequence text, int offsetInText, CharSequence substring) {
        int len = substring.length();
        if (text.length() + offsetInText > len) {
            return -1;
        }
        int i = 0;
        int j = offsetInText;
        while (i < len) {
            if (substring.charAt(i) != text.charAt(j)) {
                return -1;
            }
            i++;
            j++;
        }
        return i;
    }

    public void addMatchSetTo(UnicodeSet toUnionTo) {
        toUnionTo.addAll(this);
    }

    public int indexOf(int c) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) c, 6));
        }
        int i = 0;
        int n = 0;
        while (true) {
            int i2 = i + 1;
            int start = this.list[i];
            if (c < start) {
                return -1;
            }
            i = i2 + 1;
            int limit = this.list[i2];
            if (c < limit) {
                return (n + c) - start;
            }
            n += limit - start;
        }
    }

    public int charAt(int index) {
        if (index >= 0) {
            int len2 = this.len & -2;
            int i = 0;
            while (i < len2) {
                int i2 = i + 1;
                int start = this.list[i];
                i = i2 + 1;
                int count = this.list[i2] - start;
                if (index < count) {
                    return start + index;
                }
                index -= count;
            }
        }
        return -1;
    }

    public UnicodeSet add(int start, int end) {
        checkFrozen();
        return add_unchecked(start, end);
    }

    public UnicodeSet addAll(int start, int end) {
        checkFrozen();
        return add_unchecked(start, end);
    }

    private UnicodeSet add_unchecked(int start, int end) {
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) start, 6));
        } else if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) end, 6));
        } else {
            if (start < end) {
                add(range(start, end), 2, 0);
            } else if (start == end) {
                add(start);
            }
            return this;
        }
    }

    public final UnicodeSet add(int c) {
        checkFrozen();
        return add_unchecked(c);
    }

    private final UnicodeSet add_unchecked(int c) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) c, 6));
        }
        int i = findCodePoint(c);
        if ((i & 1) != 0) {
            return this;
        }
        int[] iArr;
        int i2;
        if (c == this.list[i] - 1) {
            this.list[i] = c;
            if (c == 1114111) {
                ensureCapacity(this.len + 1);
                iArr = this.list;
                i2 = this.len;
                this.len = i2 + 1;
                iArr[i2] = 1114112;
            }
            if (i > 0 && c == this.list[i - 1]) {
                System.arraycopy(this.list, i + 1, this.list, i - 1, (this.len - i) - 1);
                this.len -= 2;
            }
        } else if (i <= 0 || c != this.list[i - 1]) {
            if (this.len + 2 > this.list.length) {
                int[] temp = new int[((this.len + 2) + 16)];
                if (i != 0) {
                    System.arraycopy(this.list, 0, temp, 0, i);
                }
                System.arraycopy(this.list, i, temp, i + 2, this.len - i);
                this.list = temp;
            } else {
                System.arraycopy(this.list, i, this.list, i + 2, this.len - i);
            }
            this.list[i] = c;
            this.list[i + 1] = c + 1;
            this.len += 2;
        } else {
            iArr = this.list;
            i2 = i - 1;
            iArr[i2] = iArr[i2] + 1;
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet add(CharSequence s) {
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            this.strings.add(s.toString());
            this.pat = null;
        } else {
            add_unchecked(cp, cp);
        }
        return this;
    }

    private static int getSingleCP(CharSequence s) {
        if (s.length() < 1) {
            throw new IllegalArgumentException("Can't use zero-length strings in UnicodeSet");
        } else if (s.length() > 2) {
            return -1;
        } else {
            if (s.length() == 1) {
                return s.charAt(0);
            }
            int cp = UTF16.charAt(s, 0);
            if (cp > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                return cp;
            }
            return -1;
        }
    }

    public final UnicodeSet addAll(CharSequence s) {
        checkFrozen();
        int i = 0;
        while (i < s.length()) {
            int cp = UTF16.charAt(s, i);
            add_unchecked(cp, cp);
            i += UTF16.getCharCount(cp);
        }
        return this;
    }

    public final UnicodeSet retainAll(CharSequence s) {
        return retainAll(fromAll(s));
    }

    public final UnicodeSet complementAll(CharSequence s) {
        return complementAll(fromAll(s));
    }

    public final UnicodeSet removeAll(CharSequence s) {
        return removeAll(fromAll(s));
    }

    public final UnicodeSet removeAllStrings() {
        checkFrozen();
        if (this.strings.size() != 0) {
            this.strings.clear();
            this.pat = null;
        }
        return this;
    }

    public static UnicodeSet from(CharSequence s) {
        return new UnicodeSet().add(s);
    }

    public static UnicodeSet fromAll(CharSequence s) {
        return new UnicodeSet().addAll(s);
    }

    public UnicodeSet retain(int start, int end) {
        checkFrozen();
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) start, 6));
        } else if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) end, 6));
        } else {
            if (start <= end) {
                retain(range(start, end), 2, 0);
            } else {
                clear();
            }
            return this;
        }
    }

    public final UnicodeSet retain(int c) {
        return retain(c, c);
    }

    public final UnicodeSet retain(CharSequence cs) {
        int cp = getSingleCP(cs);
        if (cp < 0) {
            String s = cs.toString();
            if (this.strings.contains(s) && size() == 1) {
                return this;
            }
            clear();
            this.strings.add(s);
            this.pat = null;
        } else {
            retain(cp, cp);
        }
        return this;
    }

    public UnicodeSet remove(int start, int end) {
        checkFrozen();
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) start, 6));
        } else if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) end, 6));
        } else {
            if (start <= end) {
                retain(range(start, end), 2, 2);
            }
            return this;
        }
    }

    public final UnicodeSet remove(int c) {
        return remove(c, c);
    }

    public final UnicodeSet remove(CharSequence s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
            this.strings.remove(s.toString());
            this.pat = null;
        } else {
            remove(cp, cp);
        }
        return this;
    }

    public UnicodeSet complement(int start, int end) {
        checkFrozen();
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) start, 6));
        } else if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) end, 6));
        } else {
            if (start <= end) {
                xor(range(start, end), 2, 0);
            }
            this.pat = null;
            return this;
        }
    }

    public final UnicodeSet complement(int c) {
        return complement(c, c);
    }

    public UnicodeSet complement() {
        checkFrozen();
        if (this.list[0] == 0) {
            System.arraycopy(this.list, 1, this.list, 0, this.len - 1);
            this.len--;
        } else {
            ensureCapacity(this.len + 1);
            System.arraycopy(this.list, 0, this.list, 1, this.len);
            this.list[0] = 0;
            this.len++;
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet complement(CharSequence s) {
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            String s2 = s.toString();
            if (this.strings.contains(s2)) {
                this.strings.remove(s2);
            } else {
                this.strings.add(s2);
            }
            this.pat = null;
        } else {
            complement(cp, cp);
        }
        return this;
    }

    public boolean contains(int c) {
        boolean z = false;
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) c, 6));
        } else if (this.bmpSet != null) {
            return this.bmpSet.contains(c);
        } else {
            if (this.stringSpan != null) {
                return this.stringSpan.contains(c);
            }
            if ((findCodePoint(c) & 1) != 0) {
                z = true;
            }
            return z;
        }
    }

    private final int findCodePoint(int c) {
        if (c < this.list[0]) {
            return 0;
        }
        if (this.len >= 2 && c >= this.list[this.len - 2]) {
            return this.len - 1;
        }
        int lo = 0;
        int hi = this.len - 1;
        while (true) {
            int i = (lo + hi) >>> 1;
            if (i == lo) {
                return hi;
            }
            if (c < this.list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
    }

    public boolean contains(int start, int end) {
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) start, 6));
        } else if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) end, 6));
        } else {
            int i = findCodePoint(start);
            if ((i & 1) == 0 || end >= this.list[i]) {
                return false;
            }
            return true;
        }
    }

    public final boolean contains(CharSequence s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
            return this.strings.contains(s.toString());
        }
        return contains(cp);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean containsAll(UnicodeSet b) {
        int[] listB = b.list;
        boolean needA = true;
        boolean needB = true;
        int aLen = this.len - 1;
        int bLen = b.len - 1;
        int startA = 0;
        int startB = 0;
        int limitA = 0;
        int limitB = 0;
        int bPtr = 0;
        int aPtr = 0;
        while (true) {
            int aPtr2;
            int bPtr2;
            if (!needA) {
                aPtr2 = aPtr;
                if (!needB) {
                    bPtr2 = bPtr;
                } else if (bPtr < bLen) {
                    break;
                } else {
                    bPtr2 = bPtr + 1;
                    startB = listB[bPtr];
                    bPtr = bPtr2 + 1;
                    limitB = listB[bPtr2];
                    bPtr2 = bPtr;
                }
                if (startB < limitA) {
                    if (startB >= startA) {
                        break;
                    }
                    break;
                }
                needA = true;
                needB = false;
                bPtr = bPtr2;
                aPtr = aPtr2;
            } else if (aPtr >= aLen) {
                break;
            } else {
                aPtr2 = aPtr + 1;
                startA = this.list[aPtr];
                aPtr = aPtr2 + 1;
                limitA = this.list[aPtr2];
                aPtr2 = aPtr;
                if (!needB) {
                    if (bPtr < bLen) {
                        break;
                    }
                    bPtr2 = bPtr + 1;
                    startB = listB[bPtr];
                    bPtr = bPtr2 + 1;
                    limitB = listB[bPtr2];
                    bPtr2 = bPtr;
                } else {
                    bPtr2 = bPtr;
                }
                if (startB < limitA) {
                    needA = true;
                    needB = false;
                    bPtr = bPtr2;
                    aPtr = aPtr2;
                } else if (startB >= startA && limitB <= limitA) {
                    needA = false;
                    needB = true;
                    bPtr = bPtr2;
                    aPtr = aPtr2;
                }
            }
        }
        return false;
    }

    public boolean containsAll(String s) {
        int i = 0;
        while (i < s.length()) {
            int cp = UTF16.charAt(s, i);
            if (contains(cp)) {
                i += UTF16.getCharCount(cp);
            } else if (this.strings.size() == 0) {
                return false;
            } else {
                return containsAll(s, 0);
            }
        }
        return true;
    }

    private boolean containsAll(String s, int i) {
        if (i >= s.length()) {
            return true;
        }
        int cp = UTF16.charAt(s, i);
        if (contains(cp) && containsAll(s, UTF16.getCharCount(cp) + i)) {
            return true;
        }
        for (String setStr : this.strings) {
            if (s.startsWith(setStr, i) && containsAll(s, setStr.length() + i)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public String getRegexEquivalent() {
        if (this.strings.size() == 0) {
            return toString();
        }
        Appendable result = new StringBuilder("(?:");
        appendNewPattern(result, true, false);
        for (String s : this.strings) {
            result.append('|');
            _appendToPat(result, s, true);
        }
        return result.append(")").toString();
    }

    public boolean containsNone(int start, int end) {
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) start, 6));
        } else if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex((long) end, 6));
        } else {
            int i = -1;
            do {
                i++;
            } while (start >= this.list[i]);
            if ((i & 1) != 0 || end >= this.list[i]) {
                return false;
            }
            return true;
        }
    }

    public boolean containsNone(UnicodeSet b) {
        int aPtr;
        int[] listB = b.list;
        boolean needA = true;
        boolean needB = true;
        int aLen = this.len - 1;
        int bLen = b.len - 1;
        int startA = 0;
        int startB = 0;
        int limitA = 0;
        int limitB = 0;
        int bPtr = 0;
        int aPtr2 = 0;
        while (true) {
            int bPtr2;
            if (!needA) {
                aPtr = aPtr2;
                if (!needB) {
                    bPtr2 = bPtr;
                } else if (bPtr < bLen) {
                    break;
                } else {
                    bPtr2 = bPtr + 1;
                    startB = listB[bPtr];
                    bPtr = bPtr2 + 1;
                    limitB = listB[bPtr2];
                    bPtr2 = bPtr;
                }
                if (startB >= limitA) {
                    needA = true;
                    needB = false;
                    bPtr = bPtr2;
                    aPtr2 = aPtr;
                } else if (startA >= limitB) {
                    return false;
                } else {
                    needA = false;
                    needB = true;
                    bPtr = bPtr2;
                    aPtr2 = aPtr;
                }
            } else if (aPtr2 >= aLen) {
                break;
            } else {
                aPtr = aPtr2 + 1;
                startA = this.list[aPtr2];
                aPtr2 = aPtr + 1;
                limitA = this.list[aPtr];
                aPtr = aPtr2;
                if (!needB) {
                    if (bPtr < bLen) {
                        break;
                    }
                    bPtr2 = bPtr + 1;
                    startB = listB[bPtr];
                    bPtr = bPtr2 + 1;
                    limitB = listB[bPtr2];
                    bPtr2 = bPtr;
                } else {
                    bPtr2 = bPtr;
                }
                if (startB >= limitA) {
                    needA = true;
                    needB = false;
                    bPtr = bPtr2;
                    aPtr2 = aPtr;
                } else if (startA >= limitB) {
                    return false;
                } else {
                    needA = false;
                    needB = true;
                    bPtr = bPtr2;
                    aPtr2 = aPtr;
                }
            }
        }
        aPtr = aPtr2;
        if (SortedSetRelation.hasRelation(this.strings, 5, b.strings)) {
            return true;
        }
        return false;
    }

    public boolean containsNone(CharSequence s) {
        return span(s, SpanCondition.NOT_CONTAINED) == s.length();
    }

    public final boolean containsSome(int start, int end) {
        return containsNone(start, end) ^ 1;
    }

    public final boolean containsSome(UnicodeSet s) {
        return containsNone(s) ^ 1;
    }

    public final boolean containsSome(CharSequence s) {
        return containsNone(s) ^ 1;
    }

    public UnicodeSet addAll(UnicodeSet c) {
        checkFrozen();
        add(c.list, c.len, 0);
        this.strings.addAll(c.strings);
        return this;
    }

    public UnicodeSet retainAll(UnicodeSet c) {
        checkFrozen();
        retain(c.list, c.len, 0);
        this.strings.retainAll(c.strings);
        return this;
    }

    public UnicodeSet removeAll(UnicodeSet c) {
        checkFrozen();
        retain(c.list, c.len, 2);
        this.strings.removeAll(c.strings);
        return this;
    }

    public UnicodeSet complementAll(UnicodeSet c) {
        checkFrozen();
        xor(c.list, c.len, 0);
        SortedSetRelation.doOperation(this.strings, 5, c.strings);
        return this;
    }

    public UnicodeSet clear() {
        checkFrozen();
        this.list[0] = 1114112;
        this.len = 1;
        this.pat = null;
        this.strings.clear();
        return this;
    }

    public int getRangeCount() {
        return this.len / 2;
    }

    public int getRangeStart(int index) {
        return this.list[index * 2];
    }

    public int getRangeEnd(int index) {
        return this.list[(index * 2) + 1] - 1;
    }

    public UnicodeSet compact() {
        checkFrozen();
        if (this.len != this.list.length) {
            int[] temp = new int[this.len];
            System.arraycopy(this.list, 0, temp, 0, this.len);
            this.list = temp;
        }
        this.rangeList = null;
        this.buffer = null;
        return this;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        try {
            UnicodeSet that = (UnicodeSet) o;
            if (this.len != that.len) {
                return false;
            }
            for (int i = 0; i < this.len; i++) {
                if (this.list[i] != that.list[i]) {
                    return false;
                }
            }
            return this.strings.equals(that.strings);
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode() {
        int result = this.len;
        for (int i = 0; i < this.len; i++) {
            result = (result * 1000003) + this.list[i];
        }
        return result;
    }

    public String toString() {
        return toPattern(true);
    }

    @Deprecated
    public UnicodeSet applyPattern(String pattern, ParsePosition pos, SymbolTable symbols, int options) {
        boolean parsePositionWasNull = pos == null;
        if (parsePositionWasNull) {
            pos = new ParsePosition(0);
        }
        Appendable rebuiltPat = new StringBuilder();
        RuleCharacterIterator chars = new RuleCharacterIterator(pattern, symbols, pos);
        applyPattern(chars, symbols, rebuiltPat, options);
        if (chars.inVariable()) {
            syntaxError(chars, "Extra chars in variable value");
        }
        this.pat = rebuiltPat.toString();
        if (parsePositionWasNull) {
            int i = pos.getIndex();
            if ((options & 1) != 0) {
                i = PatternProps.skipWhiteSpace(pattern, i);
            }
            if (i != pattern.length()) {
                throw new IllegalArgumentException("Parse of \"" + pattern + "\" failed at " + i);
            }
        }
        return this;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void applyPattern(RuleCharacterIterator chars, SymbolTable symbols, Appendable rebuiltPat, int options) {
        int opts = 3;
        if ((options & 1) != 0) {
            opts = 7;
        }
        Appendable patBuf = new StringBuilder();
        StringBuilder buf = null;
        boolean usePat = false;
        UnicodeSet scratch = null;
        Object backup = null;
        int lastItem = 0;
        int lastChar = 0;
        int mode = 0;
        char op = '\u0000';
        boolean invert = false;
        clear();
        String str = null;
        while (mode != 2 && (chars.atEnd() ^ 1) != 0) {
            int c = 0;
            boolean literal = false;
            UnicodeSet nested = null;
            int setMode = 0;
            if (resemblesPropertyPattern(chars, opts)) {
                setMode = 2;
            } else {
                backup = chars.getPos(backup);
                c = chars.next(opts);
                literal = chars.isEscaped();
                if (c != 91 || (literal ^ 1) == 0) {
                    if (symbols != null) {
                        UnicodeMatcher m = symbols.lookupMatcher(c);
                        if (m != null) {
                            try {
                                nested = (UnicodeSet) m;
                                setMode = 3;
                            } catch (ClassCastException e) {
                                syntaxError(chars, "Syntax error");
                            }
                        }
                    }
                } else if (mode == 1) {
                    chars.setPos(backup);
                    setMode = 1;
                } else {
                    mode = 1;
                    patBuf.append('[');
                    backup = chars.getPos(backup);
                    c = chars.next(opts);
                    literal = chars.isEscaped();
                    if (c == 94 && (literal ^ 1) != 0) {
                        invert = true;
                        patBuf.append('^');
                        backup = chars.getPos(backup);
                        c = chars.next(opts);
                        literal = chars.isEscaped();
                    }
                    if (c == 45) {
                        literal = true;
                    } else {
                        chars.setPos(backup);
                    }
                }
            }
            if (setMode == 0) {
                if (mode == 0) {
                    syntaxError(chars, "Missing '['");
                }
                if (!literal) {
                    switch (c) {
                        case 36:
                            backup = chars.getPos(backup);
                            c = chars.next(opts);
                            boolean anchor = c == 93 ? chars.isEscaped() ^ 1 : false;
                            if (symbols == null && (anchor ^ 1) != 0) {
                                c = 36;
                                chars.setPos(backup);
                            } else if (anchor && op == '\u0000') {
                                if (lastItem == 1) {
                                    add_unchecked(lastChar, lastChar);
                                    _appendToPat(patBuf, lastChar, false);
                                }
                                add_unchecked(DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
                                usePat = true;
                                patBuf.append(SymbolTable.SYMBOL_REF).append(']');
                                mode = 2;
                                continue;
                            } else {
                                syntaxError(chars, "Unquoted '$'");
                            }
                            break;
                        case 38:
                            if (lastItem == 2 && op == '\u0000') {
                                op = (char) c;
                                continue;
                            } else {
                                syntaxError(chars, "'&' not after set");
                            }
                            break;
                        case 45:
                            if (op == '\u0000') {
                                if (lastItem == 0) {
                                    if (str == null) {
                                        add_unchecked(c, c);
                                        c = chars.next(opts);
                                        literal = chars.isEscaped();
                                        if (c == 93 && (literal ^ 1) != 0) {
                                            patBuf.append("-]");
                                            mode = 2;
                                            break;
                                        }
                                    }
                                    op = (char) c;
                                    break;
                                }
                                op = (char) c;
                                continue;
                            }
                            syntaxError(chars, "'-' not after char, string, or set");
                        case 93:
                            if (lastItem == 1) {
                                add_unchecked(lastChar, lastChar);
                                _appendToPat(patBuf, lastChar, false);
                            }
                            if (op == '-') {
                                add_unchecked(op, op);
                                patBuf.append(op);
                            } else if (op == '&') {
                                syntaxError(chars, "Trailing '&'");
                            }
                            patBuf.append(']');
                            mode = 2;
                            continue;
                        case 94:
                            syntaxError(chars, "'^' not after '['");
                            switch (lastItem) {
                                case 0:
                                    if (op == '-' && str != null) {
                                        syntaxError(chars, "Invalid range");
                                    }
                                    lastItem = 1;
                                    lastChar = c;
                                    str = null;
                                    break;
                                case 1:
                                    if (op != '-') {
                                        add_unchecked(lastChar, lastChar);
                                        _appendToPat(patBuf, lastChar, false);
                                        lastChar = c;
                                        break;
                                    }
                                    if (str != null) {
                                        syntaxError(chars, "Invalid range");
                                    }
                                    if (lastChar >= c) {
                                        syntaxError(chars, "Invalid range");
                                    }
                                    add_unchecked(lastChar, c);
                                    _appendToPat(patBuf, lastChar, false);
                                    patBuf.append(op);
                                    _appendToPat(patBuf, c, false);
                                    lastItem = 0;
                                    op = '\u0000';
                                    break;
                                case 2:
                                    if (op != '\u0000') {
                                        syntaxError(chars, "Set expected after operator");
                                    }
                                    lastChar = c;
                                    lastItem = 1;
                                    break;
                                default:
                                    break;
                            }
                        case 123:
                            CharSequence curString;
                            CharSequence charSequence;
                            Object obj;
                            int lastSingle;
                            int curSingle;
                            CharSequence lastString;
                            if (!(op == '\u0000' || op == '-')) {
                                syntaxError(chars, "Missing operand after operator");
                            }
                            if (lastItem == 1) {
                                add_unchecked(lastChar, lastChar);
                                _appendToPat(patBuf, lastChar, false);
                            }
                            lastItem = 0;
                            if (buf == null) {
                                buf = new StringBuilder();
                            } else {
                                buf.setLength(0);
                            }
                            boolean ok = false;
                            while (!chars.atEnd()) {
                                c = chars.next(opts);
                                literal = chars.isEscaped();
                                if (c != 125 || (literal ^ 1) == 0) {
                                    appendCodePoint(buf, c);
                                } else {
                                    ok = true;
                                    if (buf.length() < 1 || (ok ^ 1) != 0) {
                                        syntaxError(chars, "Invalid multicharacter string");
                                    }
                                    curString = buf.toString();
                                    if (op != '-') {
                                        if (str != null) {
                                            charSequence = "";
                                        } else {
                                            obj = str;
                                        }
                                        lastSingle = CharSequences.getSingleCodePoint(charSequence);
                                        curSingle = CharSequences.getSingleCodePoint(curString);
                                        if (lastSingle != Integer.MAX_VALUE || curSingle == Integer.MAX_VALUE) {
                                            try {
                                                StringRange.expand(str, curString, true, this.strings);
                                            } catch (Exception e2) {
                                                syntaxError(chars, e2.getMessage());
                                            }
                                        } else {
                                            add(lastSingle, curSingle);
                                        }
                                        str = null;
                                        op = '\u0000';
                                    } else {
                                        add(curString);
                                        lastString = curString;
                                    }
                                    patBuf.append('{');
                                    _appendToPat(patBuf, (String) curString, false);
                                    patBuf.append('}');
                                    continue;
                                }
                            }
                            syntaxError(chars, "Invalid multicharacter string");
                            curString = buf.toString();
                            if (op != '-') {
                                add(curString);
                                lastString = curString;
                            } else {
                                if (str != null) {
                                    obj = str;
                                } else {
                                    charSequence = "";
                                }
                                lastSingle = CharSequences.getSingleCodePoint(charSequence);
                                curSingle = CharSequences.getSingleCodePoint(curString);
                                if (lastSingle != Integer.MAX_VALUE) {
                                    break;
                                }
                                StringRange.expand(str, curString, true, this.strings);
                                str = null;
                                op = '\u0000';
                            }
                            patBuf.append('{');
                            _appendToPat(patBuf, (String) curString, false);
                            patBuf.append('}');
                            continue;
                    }
                }
                switch (lastItem) {
                    case 0:
                        syntaxError(chars, "Invalid range");
                        lastItem = 1;
                        lastChar = c;
                        str = null;
                        break;
                    case 1:
                        if (op != '-') {
                            if (str != null) {
                                syntaxError(chars, "Invalid range");
                            }
                            if (lastChar >= c) {
                                syntaxError(chars, "Invalid range");
                            }
                            add_unchecked(lastChar, c);
                            _appendToPat(patBuf, lastChar, false);
                            patBuf.append(op);
                            _appendToPat(patBuf, c, false);
                            lastItem = 0;
                            op = '\u0000';
                            break;
                        }
                        add_unchecked(lastChar, lastChar);
                        _appendToPat(patBuf, lastChar, false);
                        lastChar = c;
                        break;
                    case 2:
                        if (op != '\u0000') {
                            syntaxError(chars, "Set expected after operator");
                        }
                        lastChar = c;
                        lastItem = 1;
                        break;
                    default:
                        break;
                }
            }
            if (lastItem == 1) {
                if (op != '\u0000') {
                    syntaxError(chars, "Char expected after operator");
                }
                add_unchecked(lastChar, lastChar);
                _appendToPat(patBuf, lastChar, false);
                op = '\u0000';
            }
            if (op == '-' || op == '&') {
                patBuf.append(op);
            }
            if (nested == null) {
                if (scratch == null) {
                    scratch = new UnicodeSet();
                }
                nested = scratch;
            }
            switch (setMode) {
                case 1:
                    nested.applyPattern(chars, symbols, patBuf, options);
                    break;
                case 2:
                    chars.skipIgnored(opts);
                    nested.applyPropertyPattern(chars, patBuf, symbols);
                    break;
                case 3:
                    nested._toPattern(patBuf, false);
                    break;
            }
            usePat = true;
            if (mode == 0) {
                set(nested);
                mode = 2;
            } else {
                switch (op) {
                    case '\u0000':
                        addAll(nested);
                        break;
                    case '&':
                        retainAll(nested);
                        break;
                    case '-':
                        removeAll(nested);
                        break;
                }
                op = '\u0000';
                lastItem = 2;
            }
        }
        if (mode != 2) {
            syntaxError(chars, "Missing ']'");
        }
        chars.skipIgnored(opts);
        if ((options & 2) != 0) {
            closeOver(2);
        }
        if (invert) {
            complement();
        }
        if (usePat) {
            append(rebuiltPat, patBuf.toString());
        } else {
            appendNewPattern(rebuiltPat, false, true);
        }
    }

    private static void syntaxError(RuleCharacterIterator chars, String msg) {
        throw new IllegalArgumentException("Error: " + msg + " at \"" + Utility.escape(chars.toString()) + '\"');
    }

    public <T extends Collection<String>> T addAllTo(T target) {
        return addAllTo((Iterable) this, (Collection) target);
    }

    public String[] addAllTo(String[] target) {
        return (String[]) addAllTo((Iterable) this, (Object[]) target);
    }

    public static String[] toArray(UnicodeSet set) {
        return (String[]) addAllTo((Iterable) set, new String[set.size()]);
    }

    public UnicodeSet add(Iterable<?> source) {
        return addAll((Iterable) source);
    }

    public UnicodeSet addAll(Iterable<?> source) {
        checkFrozen();
        for (Object o : source) {
            add(o.toString());
        }
        return this;
    }

    private void ensureCapacity(int newLen) {
        if (newLen > this.list.length) {
            int[] temp = new int[(newLen + 16)];
            System.arraycopy(this.list, 0, temp, 0, this.len);
            this.list = temp;
        }
    }

    private void ensureBufferCapacity(int newLen) {
        if (this.buffer == null || newLen > this.buffer.length) {
            this.buffer = new int[(newLen + 16)];
        }
    }

    private int[] range(int start, int end) {
        if (this.rangeList == null) {
            this.rangeList = new int[]{start, end + 1, 1114112};
        } else {
            this.rangeList[0] = start;
            this.rangeList[1] = end + 1;
        }
        return this.rangeList;
    }

    private UnicodeSet xor(int[] other, int otherLen, int polarity) {
        int b;
        int k;
        int j;
        int i;
        ensureBufferCapacity(this.len + otherLen);
        int j2 = 0;
        int a = this.list[0];
        if (polarity == 1 || polarity == 2) {
            b = 0;
            if (other[0] == 0) {
                j2 = 1;
                b = other[1];
            }
            k = 0;
            j = j2;
            i = 1;
        } else {
            b = other[0];
            k = 0;
            j = 1;
            i = 1;
        }
        while (true) {
            int k2;
            int i2;
            if (a < b) {
                k2 = k + 1;
                this.buffer[k] = a;
                i2 = i + 1;
                a = this.list[i];
                j2 = j;
            } else if (b < a) {
                k2 = k + 1;
                this.buffer[k] = b;
                j2 = j + 1;
                b = other[j];
                i2 = i;
            } else if (a != 1114112) {
                i2 = i + 1;
                a = this.list[i];
                j2 = j + 1;
                b = other[j];
                k2 = k;
            } else {
                k2 = k + 1;
                this.buffer[k] = 1114112;
                this.len = k2;
                int[] temp = this.list;
                this.list = this.buffer;
                this.buffer = temp;
                this.pat = null;
                return this;
            }
            k = k2;
            j = j2;
            i = i2;
        }
    }

    private UnicodeSet add(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(this.len + otherLen);
        int a = this.list[0];
        int b = other[0];
        int k = 0;
        int j = 1;
        int i = 1;
        while (true) {
            int k2;
            int i2;
            int j2;
            switch (polarity) {
                case 0:
                    if (a >= b) {
                        if (b >= a) {
                            if (a != 1114112) {
                                if (k <= 0 || a > this.buffer[k - 1]) {
                                    k2 = k + 1;
                                    this.buffer[k] = a;
                                    a = this.list[i];
                                } else {
                                    k2 = k - 1;
                                    a = max(this.list[i], this.buffer[k2]);
                                }
                                i2 = i + 1;
                                polarity ^= 1;
                                j2 = j + 1;
                                b = other[j];
                                polarity ^= 2;
                                break;
                            }
                            break;
                        }
                        if (k <= 0 || b > this.buffer[k - 1]) {
                            k2 = k + 1;
                            this.buffer[k] = b;
                            b = other[j];
                        } else {
                            k2 = k - 1;
                            b = max(other[j], this.buffer[k2]);
                        }
                        j2 = j + 1;
                        polarity ^= 2;
                        i2 = i;
                        break;
                    }
                    if (k <= 0 || a > this.buffer[k - 1]) {
                        k2 = k + 1;
                        this.buffer[k] = a;
                        a = this.list[i];
                    } else {
                        k2 = k - 1;
                        a = max(this.list[i], this.buffer[k2]);
                    }
                    i2 = i + 1;
                    polarity ^= 1;
                    j2 = j;
                    continue;
                    break;
                case 1:
                    if (a >= b) {
                        if (b >= a) {
                            if (a == 1114112) {
                                break;
                            }
                            i2 = i + 1;
                            a = this.list[i];
                            polarity ^= 1;
                            j2 = j + 1;
                            b = other[j];
                            polarity ^= 2;
                            k2 = k;
                            break;
                        }
                        j2 = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        k2 = k;
                        i2 = i;
                        break;
                    }
                    k2 = k + 1;
                    this.buffer[k] = a;
                    i2 = i + 1;
                    a = this.list[i];
                    polarity ^= 1;
                    j2 = j;
                    continue;
                case 2:
                    if (b >= a) {
                        if (a >= b) {
                            if (a == 1114112) {
                                break;
                            }
                            i2 = i + 1;
                            a = this.list[i];
                            polarity ^= 1;
                            j2 = j + 1;
                            b = other[j];
                            polarity ^= 2;
                            k2 = k;
                            break;
                        }
                        i2 = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        k2 = k;
                        j2 = j;
                        break;
                    }
                    k2 = k + 1;
                    this.buffer[k] = b;
                    j2 = j + 1;
                    b = other[j];
                    polarity ^= 2;
                    i2 = i;
                    continue;
                case 3:
                    if (b > a) {
                        if (b == 1114112) {
                            break;
                        }
                        k2 = k + 1;
                        this.buffer[k] = b;
                    } else if (a == 1114112) {
                        break;
                    } else {
                        k2 = k + 1;
                        this.buffer[k] = a;
                    }
                    i2 = i + 1;
                    a = this.list[i];
                    polarity ^= 1;
                    j2 = j + 1;
                    b = other[j];
                    polarity ^= 2;
                    continue;
                default:
                    k2 = k;
                    j2 = j;
                    i2 = i;
                    continue;
            }
            k2 = k + 1;
            this.buffer[k] = 1114112;
            this.len = k2;
            int[] temp = this.list;
            this.list = this.buffer;
            this.buffer = temp;
            this.pat = null;
            return this;
            k = k2;
            j = j2;
            i = i2;
        }
    }

    private UnicodeSet retain(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(this.len + otherLen);
        int a = this.list[0];
        int b = other[0];
        int k = 0;
        int j = 1;
        int i = 1;
        while (true) {
            int k2;
            int i2;
            int j2;
            switch (polarity) {
                case 0:
                    if (a >= b) {
                        if (b >= a) {
                            if (a != 1114112) {
                                k2 = k + 1;
                                this.buffer[k] = a;
                                i2 = i + 1;
                                a = this.list[i];
                                polarity ^= 1;
                                j2 = j + 1;
                                b = other[j];
                                polarity ^= 2;
                                break;
                            }
                            break;
                        }
                        j2 = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        k2 = k;
                        i2 = i;
                        break;
                    }
                    i2 = i + 1;
                    a = this.list[i];
                    polarity ^= 1;
                    k2 = k;
                    j2 = j;
                    continue;
                case 1:
                    if (a >= b) {
                        if (b >= a) {
                            if (a == 1114112) {
                                break;
                            }
                            i2 = i + 1;
                            a = this.list[i];
                            polarity ^= 1;
                            j2 = j + 1;
                            b = other[j];
                            polarity ^= 2;
                            k2 = k;
                            break;
                        }
                        k2 = k + 1;
                        this.buffer[k] = b;
                        j2 = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        i2 = i;
                        break;
                    }
                    i2 = i + 1;
                    a = this.list[i];
                    polarity ^= 1;
                    k2 = k;
                    j2 = j;
                    continue;
                case 2:
                    if (b >= a) {
                        if (a >= b) {
                            if (a == 1114112) {
                                break;
                            }
                            i2 = i + 1;
                            a = this.list[i];
                            polarity ^= 1;
                            j2 = j + 1;
                            b = other[j];
                            polarity ^= 2;
                            k2 = k;
                            break;
                        }
                        k2 = k + 1;
                        this.buffer[k] = a;
                        i2 = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        j2 = j;
                        break;
                    }
                    j2 = j + 1;
                    b = other[j];
                    polarity ^= 2;
                    k2 = k;
                    i2 = i;
                    continue;
                case 3:
                    if (a >= b) {
                        if (b >= a) {
                            if (a == 1114112) {
                                break;
                            }
                            k2 = k + 1;
                            this.buffer[k] = a;
                            i2 = i + 1;
                            a = this.list[i];
                            polarity ^= 1;
                            j2 = j + 1;
                            b = other[j];
                            polarity ^= 2;
                            break;
                        }
                        k2 = k + 1;
                        this.buffer[k] = b;
                        j2 = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        i2 = i;
                        break;
                    }
                    k2 = k + 1;
                    this.buffer[k] = a;
                    i2 = i + 1;
                    a = this.list[i];
                    polarity ^= 1;
                    j2 = j;
                    continue;
                default:
                    k2 = k;
                    j2 = j;
                    i2 = i;
                    continue;
            }
            k2 = k + 1;
            this.buffer[k] = 1114112;
            this.len = k2;
            int[] temp = this.list;
            this.list = this.buffer;
            this.buffer = temp;
            this.pat = null;
            return this;
            k = k2;
            j = j2;
            i = i2;
        }
    }

    private static final int max(int a, int b) {
        return a > b ? a : b;
    }

    private static synchronized UnicodeSet getInclusions(int src) {
        UnicodeSet unicodeSet;
        synchronized (UnicodeSet.class) {
            if (INCLUSIONS == null) {
                INCLUSIONS = new UnicodeSet[12];
            }
            if (INCLUSIONS[src] == null) {
                UnicodeSet incl = new UnicodeSet();
                switch (src) {
                    case 1:
                        UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                        break;
                    case 2:
                        UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                        break;
                    case 4:
                        UCaseProps.INSTANCE.addPropertyStarts(incl);
                        break;
                    case 5:
                        UBiDiProps.INSTANCE.addPropertyStarts(incl);
                        break;
                    case 6:
                        UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                        UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                        break;
                    case 7:
                        Norm2AllModes.getNFCInstance().impl.addPropertyStarts(incl);
                        UCaseProps.INSTANCE.addPropertyStarts(incl);
                        break;
                    case 8:
                        Norm2AllModes.getNFCInstance().impl.addPropertyStarts(incl);
                        break;
                    case 9:
                        Norm2AllModes.getNFKCInstance().impl.addPropertyStarts(incl);
                        break;
                    case 10:
                        Norm2AllModes.getNFKC_CFInstance().impl.addPropertyStarts(incl);
                        break;
                    case 11:
                        Norm2AllModes.getNFCInstance().impl.addCanonIterPropertyStarts(incl);
                        break;
                    default:
                        throw new IllegalStateException("UnicodeSet.getInclusions(unknown src " + src + ")");
                }
                INCLUSIONS[src] = incl;
            }
            unicodeSet = INCLUSIONS[src];
        }
        return unicodeSet;
    }

    private UnicodeSet applyFilter(Filter filter, int src) {
        clear();
        int startHasProperty = -1;
        UnicodeSet inclusions = getInclusions(src);
        int limitRange = inclusions.getRangeCount();
        for (int j = 0; j < limitRange; j++) {
            int start = inclusions.getRangeStart(j);
            int end = inclusions.getRangeEnd(j);
            for (int ch = start; ch <= end; ch++) {
                if (filter.contains(ch)) {
                    if (startHasProperty < 0) {
                        startHasProperty = ch;
                    }
                } else if (startHasProperty >= 0) {
                    add_unchecked(startHasProperty, ch - 1);
                    startHasProperty = -1;
                }
            }
        }
        if (startHasProperty >= 0) {
            add_unchecked(startHasProperty, 1114111);
        }
        return this;
    }

    private static String mungeCharName(String source) {
        source = PatternProps.trimWhiteSpace(source);
        StringBuilder buf = null;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (PatternProps.isWhiteSpace(ch)) {
                if (buf == null) {
                    buf = new StringBuilder().append(source, 0, i);
                } else if (buf.charAt(buf.length() - 1) == ' ') {
                }
                ch = ' ';
            }
            if (buf != null) {
                buf.append(ch);
            }
        }
        return buf == null ? source : buf.toString();
    }

    public UnicodeSet applyIntPropertyValue(int prop, int value) {
        checkFrozen();
        if (prop == 8192) {
            applyFilter(new GeneralCategoryMaskFilter(value), 1);
        } else if (prop == 28672) {
            applyFilter(new ScriptExtensionsFilter(value), 2);
        } else {
            applyFilter(new IntPropertyFilter(prop, value), UCharacterProperty.INSTANCE.getSource(prop));
        }
        return this;
    }

    public UnicodeSet applyPropertyAlias(String propertyAlias, String valueAlias) {
        return applyPropertyAlias(propertyAlias, valueAlias, null);
    }

    public UnicodeSet applyPropertyAlias(String propertyAlias, String valueAlias, SymbolTable symbols) {
        int v;
        checkFrozen();
        boolean invert = false;
        if (symbols != null && (symbols instanceof XSymbolTable) && ((XSymbolTable) symbols).applyPropertyAlias(propertyAlias, valueAlias, this)) {
            return this;
        }
        if (XSYMBOL_TABLE != null && XSYMBOL_TABLE.applyPropertyAlias(propertyAlias, valueAlias, this)) {
            return this;
        }
        int p;
        if (valueAlias.length() > 0) {
            p = UCharacter.getPropertyEnum(propertyAlias);
            if (p == 4101) {
                p = 8192;
            }
            if ((p < 0 || p >= 61) && ((p < 4096 || p >= UProperty.INT_LIMIT) && (p < 8192 || p >= UProperty.MASK_LIMIT))) {
                switch (p) {
                    case 12288:
                        applyFilter(new NumericValueFilter(Double.parseDouble(PatternProps.trimWhiteSpace(valueAlias))), 1);
                        return this;
                    case 16384:
                        applyFilter(new VersionFilter(VersionInfo.getInstance(mungeCharName(valueAlias))), 2);
                        return this;
                    case UProperty.NAME /*16389*/:
                        int ch = UCharacter.getCharFromExtendedName(mungeCharName(valueAlias));
                        if (ch == -1) {
                            throw new IllegalArgumentException("Invalid character name");
                        }
                        clear();
                        add_unchecked(ch);
                        return this;
                    case UProperty.UNICODE_1_NAME /*16395*/:
                        throw new IllegalArgumentException("Unicode_1_Name (na1) not supported");
                    case 28672:
                        v = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, valueAlias);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported property");
                }
            }
            try {
                v = UCharacter.getPropertyValueEnum(p, valueAlias);
            } catch (IllegalArgumentException e) {
                if (p == 4098 || p == UProperty.LEAD_CANONICAL_COMBINING_CLASS || p == UProperty.TRAIL_CANONICAL_COMBINING_CLASS) {
                    v = Integer.parseInt(PatternProps.trimWhiteSpace(valueAlias));
                    if (v < 0 || v > 255) {
                        throw e;
                    }
                }
                throw e;
            }
        }
        UPropertyAliases pnames = UPropertyAliases.INSTANCE;
        p = 8192;
        v = pnames.getPropertyValueEnum(8192, propertyAlias);
        if (v == -1) {
            p = UProperty.SCRIPT;
            v = pnames.getPropertyValueEnum(UProperty.SCRIPT, propertyAlias);
            if (v == -1) {
                p = pnames.getPropertyEnum(propertyAlias);
                if (p == -1) {
                    p = -1;
                }
                if (p >= 0 && p < 61) {
                    v = 1;
                } else if (p != -1) {
                    throw new IllegalArgumentException("Missing property value");
                } else if (UPropertyAliases.compare(ANY_ID, propertyAlias) == 0) {
                    set(0, 1114111);
                    return this;
                } else if (UPropertyAliases.compare(ASCII_ID, propertyAlias) == 0) {
                    set(0, 127);
                    return this;
                } else if (UPropertyAliases.compare(ASSIGNED, propertyAlias) == 0) {
                    p = 8192;
                    v = 1;
                    invert = true;
                } else {
                    throw new IllegalArgumentException("Invalid property alias: " + propertyAlias + "=" + valueAlias);
                }
            }
        }
        applyIntPropertyValue(p, v);
        if (invert) {
            complement();
        }
        if (!false || !isEmpty()) {
            return this;
        }
        throw new IllegalArgumentException("Invalid property value");
    }

    private static boolean resemblesPropertyPattern(String pattern, int pos) {
        boolean z = true;
        if (pos + 5 > pattern.length()) {
            return false;
        }
        if (!pattern.regionMatches(pos, "[:", 0, 2)) {
            if (!pattern.regionMatches(true, pos, "\\p", 0, 2)) {
                z = pattern.regionMatches(pos, "\\N", 0, 2);
            }
        }
        return z;
    }

    private static boolean resemblesPropertyPattern(RuleCharacterIterator chars, int iterOpts) {
        boolean result = false;
        iterOpts &= -3;
        Object pos = chars.getPos(null);
        int c = chars.next(iterOpts);
        if (c == 91 || c == 92) {
            int d = chars.next(iterOpts & -5);
            result = c == 91 ? d == 58 : d == 78 || d == 112 || d == 80;
        }
        chars.setPos(pos);
        return result;
    }

    private UnicodeSet applyPropertyPattern(String pattern, ParsePosition ppos, SymbolTable symbols) {
        int pos = ppos.getIndex();
        if (pos + 5 > pattern.length()) {
            return null;
        }
        boolean posix = false;
        boolean isName = false;
        boolean invert = false;
        if (pattern.regionMatches(pos, "[:", 0, 2)) {
            posix = true;
            pos = PatternProps.skipWhiteSpace(pattern, pos + 2);
            if (pos < pattern.length() && pattern.charAt(pos) == '^') {
                pos++;
                invert = true;
            }
        } else {
            if (!pattern.regionMatches(true, pos, "\\p", 0, 2)) {
                if (!pattern.regionMatches(pos, "\\N", 0, 2)) {
                    return null;
                }
            }
            char c = pattern.charAt(pos + 1);
            invert = c == 'P';
            isName = c == 'N';
            pos = PatternProps.skipWhiteSpace(pattern, pos + 2);
            if (pos != pattern.length()) {
                int pos2 = pos + 1;
                if (pattern.charAt(pos) != '{') {
                    pos = pos2;
                } else {
                    pos = pos2;
                }
            }
            return null;
        }
        int close = pattern.indexOf(posix ? ":]" : "}", pos);
        if (close < 0) {
            return null;
        }
        String propName;
        String valueName;
        int equals = pattern.indexOf(61, pos);
        if (equals < 0 || equals >= close || (isName ^ 1) == 0) {
            propName = pattern.substring(pos, close);
            valueName = "";
            if (isName) {
                valueName = propName;
                propName = "na";
            }
        } else {
            propName = pattern.substring(pos, equals);
            valueName = pattern.substring(equals + 1, close);
        }
        applyPropertyAlias(propName, valueName, symbols);
        if (invert) {
            complement();
        }
        ppos.setIndex((posix ? 2 : 1) + close);
        return this;
    }

    private void applyPropertyPattern(RuleCharacterIterator chars, Appendable rebuiltPat, SymbolTable symbols) {
        String patStr = chars.lookahead();
        ParsePosition pos = new ParsePosition(0);
        applyPropertyPattern(patStr, pos, symbols);
        if (pos.getIndex() == 0) {
            syntaxError(chars, "Invalid property pattern");
        }
        chars.jumpahead(pos.getIndex());
        append(rebuiltPat, patStr.substring(0, pos.getIndex()));
    }

    private static final void addCaseMapping(UnicodeSet set, int result, StringBuilder full) {
        if (result < 0) {
            return;
        }
        if (result > 31) {
            set.add(result);
            return;
        }
        set.add(full.toString());
        full.setLength(0);
    }

    public UnicodeSet closeOver(int attribute) {
        checkFrozen();
        if ((attribute & 6) != 0) {
            UCaseProps csp = UCaseProps.INSTANCE;
            UnicodeSet foldSet = new UnicodeSet(this);
            ULocale root = ULocale.ROOT;
            if ((attribute & 2) != 0) {
                foldSet.strings.clear();
            }
            int n = getRangeCount();
            StringBuilder full = new StringBuilder();
            for (int i = 0; i < n; i++) {
                int start = getRangeStart(i);
                int end = getRangeEnd(i);
                int cp;
                if ((attribute & 2) != 0) {
                    for (cp = start; cp <= end; cp++) {
                        csp.addCaseClosure(cp, foldSet);
                    }
                } else {
                    for (cp = start; cp <= end; cp++) {
                        addCaseMapping(foldSet, csp.toFullLower(cp, null, full, 1), full);
                        addCaseMapping(foldSet, csp.toFullTitle(cp, null, full, 1), full);
                        addCaseMapping(foldSet, csp.toFullUpper(cp, null, full, 1), full);
                        addCaseMapping(foldSet, csp.toFullFolding(cp, full, 0), full);
                    }
                }
            }
            if (!this.strings.isEmpty()) {
                if ((attribute & 2) != 0) {
                    for (String s : this.strings) {
                        CharSequence str = UCharacter.foldCase(s, 0);
                        if (!csp.addStringCaseClosure(str, foldSet)) {
                            foldSet.add(str);
                        }
                    }
                } else {
                    BreakIterator bi = BreakIterator.getWordInstance(root);
                    for (String str2 : this.strings) {
                        foldSet.add((CharSequence) UCharacter.toLowerCase(root, str2));
                        foldSet.add((CharSequence) UCharacter.toTitleCase(root, str2, bi));
                        foldSet.add((CharSequence) UCharacter.toUpperCase(root, str2));
                        foldSet.add((CharSequence) UCharacter.foldCase(str2, 0));
                    }
                }
            }
            set(foldSet);
        }
        return this;
    }

    public boolean isFrozen() {
        return (this.bmpSet == null && this.stringSpan == null) ? false : true;
    }

    public UnicodeSet freeze() {
        if (!isFrozen()) {
            this.buffer = null;
            if (this.list.length > this.len + 16) {
                int capacity = this.len == 0 ? 1 : this.len;
                int[] oldList = this.list;
                this.list = new int[capacity];
                int i = capacity;
                while (true) {
                    int i2 = i - 1;
                    if (i <= 0) {
                        break;
                    }
                    this.list[i2] = oldList[i2];
                    i = i2;
                }
            }
            if (!this.strings.isEmpty()) {
                this.stringSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), 127);
            }
            if (this.stringSpan == null || (this.stringSpan.needsStringSpanUTF16() ^ 1) != 0) {
                this.bmpSet = new BMPSet(this.list, this.len);
            }
        }
        return this;
    }

    public int span(CharSequence s, SpanCondition spanCondition) {
        return span(s, 0, spanCondition);
    }

    public int span(CharSequence s, int start, SpanCondition spanCondition) {
        int end = s.length();
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return end;
        }
        if (this.bmpSet != null) {
            return this.bmpSet.span(s, start, spanCondition, null);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.span(s, start, spanCondition);
        }
        if (!this.strings.isEmpty()) {
            int which;
            if (spanCondition == SpanCondition.NOT_CONTAINED) {
                which = 33;
            } else {
                which = 34;
            }
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.span(s, start, spanCondition);
            }
        }
        return spanCodePointsAndCount(s, start, spanCondition, null);
    }

    @Deprecated
    public int spanAndCount(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        if (outCount == null) {
            throw new IllegalArgumentException("outCount must not be null");
        }
        int end = s.length();
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return end;
        }
        if (this.stringSpan != null) {
            return this.stringSpan.spanAndCount(s, start, spanCondition, outCount);
        }
        if (this.bmpSet != null) {
            return this.bmpSet.span(s, start, spanCondition, outCount);
        }
        if (this.strings.isEmpty()) {
            return spanCodePointsAndCount(s, start, spanCondition, outCount);
        }
        int which;
        if (spanCondition == SpanCondition.NOT_CONTAINED) {
            which = 33;
        } else {
            which = 34;
        }
        return new UnicodeSetStringSpan(this, new ArrayList(this.strings), which | 64).spanAndCount(s, start, spanCondition, outCount);
    }

    private int spanCodePointsAndCount(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        boolean spanContained = spanCondition != SpanCondition.NOT_CONTAINED;
        int next = start;
        int length = s.length();
        int count = 0;
        while (true) {
            int c = Character.codePointAt(s, next);
            if (spanContained == contains(c)) {
                count++;
                next += Character.charCount(c);
                if (next >= length) {
                    break;
                }
            } else {
                break;
            }
        }
        if (outCount != null) {
            outCount.value = count;
        }
        return next;
    }

    public int spanBack(CharSequence s, SpanCondition spanCondition) {
        return spanBack(s, s.length(), spanCondition);
    }

    public int spanBack(CharSequence s, int fromIndex, SpanCondition spanCondition) {
        if (fromIndex <= 0) {
            return 0;
        }
        if (fromIndex > s.length()) {
            fromIndex = s.length();
        }
        if (this.bmpSet != null) {
            return this.bmpSet.spanBack(s, fromIndex, spanCondition);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.spanBack(s, fromIndex, spanCondition);
        }
        if (!this.strings.isEmpty()) {
            int which;
            if (spanCondition == SpanCondition.NOT_CONTAINED) {
                which = 17;
            } else {
                which = 18;
            }
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.spanBack(s, fromIndex, spanCondition);
            }
        }
        boolean spanContained = spanCondition != SpanCondition.NOT_CONTAINED;
        int prev = fromIndex;
        while (true) {
            int c = Character.codePointBefore(s, prev);
            if (spanContained == contains(c)) {
                prev -= Character.charCount(c);
                if (prev <= 0) {
                    break;
                }
            } else {
                break;
            }
        }
        return prev;
    }

    public UnicodeSet cloneAsThawed() {
        UnicodeSet result = new UnicodeSet(this);
        if (-assertionsDisabled || !result.isFrozen()) {
            return result;
        }
        throw new AssertionError();
    }

    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    public Iterable<EntryRange> ranges() {
        return new EntryRangeIterable();
    }

    public Iterator<String> iterator() {
        return new UnicodeSetIterator2(this);
    }

    public <T extends CharSequence> boolean containsAll(Iterable<T> collection) {
        for (T o : collection) {
            if (!contains((CharSequence) o)) {
                return false;
            }
        }
        return true;
    }

    public <T extends CharSequence> boolean containsNone(Iterable<T> collection) {
        for (T o : collection) {
            if (contains((CharSequence) o)) {
                return false;
            }
        }
        return true;
    }

    public final <T extends CharSequence> boolean containsSome(Iterable<T> collection) {
        return containsNone((Iterable) collection) ^ 1;
    }

    public <T extends CharSequence> UnicodeSet addAll(T... collection) {
        checkFrozen();
        for (CharSequence str : collection) {
            add(str);
        }
        return this;
    }

    public <T extends CharSequence> UnicodeSet removeAll(Iterable<T> collection) {
        checkFrozen();
        for (T o : collection) {
            remove((CharSequence) o);
        }
        return this;
    }

    public <T extends CharSequence> UnicodeSet retainAll(Iterable<T> collection) {
        checkFrozen();
        UnicodeSet toRetain = new UnicodeSet();
        toRetain.addAll((Iterable) collection);
        retainAll(toRetain);
        return this;
    }

    public int compareTo(UnicodeSet o) {
        return compareTo(o, ComparisonStyle.SHORTER_FIRST);
    }

    public int compareTo(UnicodeSet o, ComparisonStyle style) {
        int i = -1;
        int i2 = 1;
        int i3 = 0;
        if (style != ComparisonStyle.LEXICOGRAPHIC) {
            int diff = size() - o.size();
            if (diff != 0) {
                int i4 = diff < 0 ? 1 : 0;
                if (style == ComparisonStyle.SHORTER_FIRST) {
                    i3 = 1;
                }
                if (i4 == i3) {
                    i2 = -1;
                }
                return i2;
            }
        }
        int i5 = 0;
        while (true) {
            int result = this.list[i5] - o.list[i5];
            if (result != 0) {
                break;
            } else if (this.list[i5] == 1114112) {
                return compare(this.strings, o.strings);
            } else {
                i5++;
            }
        }
        if (this.list[i5] == 1114112) {
            if (this.strings.isEmpty()) {
                return 1;
            }
            return compare((String) this.strings.first(), o.list[i5]);
        } else if (o.list[i5] != 1114112) {
            if ((i5 & 1) != 0) {
                result = -result;
            }
            return result;
        } else if (o.strings.isEmpty()) {
            return -1;
        } else {
            int compareResult = compare((String) o.strings.first(), this.list[i5]);
            if (compareResult <= 0) {
                i = compareResult < 0 ? 1 : 0;
            }
            return i;
        }
    }

    public int compareTo(Iterable<String> other) {
        return compare((Iterable) this, (Iterable) other);
    }

    public static int compare(CharSequence string, int codePoint) {
        return CharSequences.compare(string, codePoint);
    }

    public static int compare(int codePoint, CharSequence string) {
        return -CharSequences.compare(string, codePoint);
    }

    public static <T extends Comparable<T>> int compare(Iterable<T> collection1, Iterable<T> collection2) {
        return compare(collection1.iterator(), collection2.iterator());
    }

    @Deprecated
    public static <T extends Comparable<T>> int compare(Iterator<T> first, Iterator<T> other) {
        int i = 0;
        while (first.hasNext()) {
            if (!other.hasNext()) {
                return 1;
            }
            int result = ((Comparable) first.next()).compareTo((Comparable) other.next());
            if (result != 0) {
                return result;
            }
        }
        if (other.hasNext()) {
            i = -1;
        }
        return i;
    }

    public static <T extends Comparable<T>> int compare(Collection<T> collection1, Collection<T> collection2, ComparisonStyle style) {
        int i = 1;
        int i2 = 0;
        if (style != ComparisonStyle.LEXICOGRAPHIC) {
            int diff = collection1.size() - collection2.size();
            if (diff != 0) {
                int i3 = diff < 0 ? 1 : 0;
                if (style == ComparisonStyle.SHORTER_FIRST) {
                    i2 = 1;
                }
                if (i3 == i2) {
                    i = -1;
                }
                return i;
            }
        }
        return compare((Iterable) collection1, (Iterable) collection2);
    }

    public static <T, U extends Collection<T>> U addAllTo(Iterable<T> source, U target) {
        for (T item : source) {
            target.add(item);
        }
        return target;
    }

    public static <T> T[] addAllTo(Iterable<T> source, T[] target) {
        int i = 0;
        for (T item : source) {
            int i2 = i + 1;
            target[i] = item;
            i = i2;
        }
        return target;
    }

    public Collection<String> strings() {
        return Collections.unmodifiableSortedSet(this.strings);
    }

    @Deprecated
    public static int getSingleCodePoint(CharSequence s) {
        return CharSequences.getSingleCodePoint(s);
    }

    @Deprecated
    public UnicodeSet addBridges(UnicodeSet dontCare) {
        UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet(this).complement());
        while (it.nextRange()) {
            if (!(it.codepoint == 0 || it.codepoint == UnicodeSetIterator.IS_STRING || it.codepointEnd == 1114111 || !dontCare.contains(it.codepoint, it.codepointEnd))) {
                add(it.codepoint, it.codepointEnd);
            }
        }
        return this;
    }

    @Deprecated
    public int findIn(CharSequence value, int fromIndex, boolean findNot) {
        while (fromIndex < value.length()) {
            int cp = UTF16.charAt(value, fromIndex);
            if (contains(cp) != findNot) {
                break;
            }
            fromIndex += UTF16.getCharCount(cp);
        }
        return fromIndex;
    }

    @Deprecated
    public int findLastIn(CharSequence value, int fromIndex, boolean findNot) {
        fromIndex--;
        while (fromIndex >= 0) {
            int cp = UTF16.charAt(value, fromIndex);
            if (contains(cp) != findNot) {
                break;
            }
            fromIndex -= UTF16.getCharCount(cp);
        }
        return fromIndex < 0 ? -1 : fromIndex;
    }

    @Deprecated
    public String stripFrom(CharSequence source, boolean matches) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < source.length()) {
            int inside = findIn(source, pos, matches ^ 1);
            result.append(source.subSequence(pos, inside));
            pos = findIn(source, inside, matches);
        }
        return result.toString();
    }

    @Deprecated
    public static XSymbolTable getDefaultXSymbolTable() {
        return XSYMBOL_TABLE;
    }

    @Deprecated
    public static void setDefaultXSymbolTable(XSymbolTable xSymbolTable) {
        INCLUSIONS = null;
        XSYMBOL_TABLE = xSymbolTable;
    }
}
