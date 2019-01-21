package android.icu.impl;

import android.icu.impl.UCaseProps.ContextIterator;
import android.icu.impl.coll.CollationSettings;
import android.icu.text.BreakIterator;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Edits;
import android.icu.text.Edits.Iterator;
import android.icu.text.UTF16;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.text.CharacterIterator;
import java.util.Locale;

public final class CaseMapImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int LNS = 251792942;
    public static final int OMIT_UNCHANGED_TEXT = 16384;
    private static final int TITLECASE_ADJUSTMENT_MASK = 1536;
    public static final int TITLECASE_ADJUST_TO_CASED = 1024;
    private static final int TITLECASE_ITERATOR_MASK = 224;
    public static final int TITLECASE_SENTENCES = 64;
    public static final int TITLECASE_WHOLE_STRING = 32;

    private static final class GreekUpper {
        private static final int AFTER_CASED = 1;
        private static final int AFTER_VOWEL_WITH_ACCENT = 2;
        private static final int HAS_ACCENT = 16384;
        private static final int HAS_COMBINING_DIALYTIKA = 65536;
        private static final int HAS_DIALYTIKA = 32768;
        private static final int HAS_EITHER_DIALYTIKA = 98304;
        private static final int HAS_OTHER_GREEK_DIACRITIC = 131072;
        private static final int HAS_VOWEL = 4096;
        private static final int HAS_VOWEL_AND_ACCENT = 20480;
        private static final int HAS_VOWEL_AND_ACCENT_AND_DIALYTIKA = 53248;
        private static final int HAS_YPOGEGRAMMENI = 8192;
        private static final int UPPER_MASK = 1023;
        private static final char[] data0370 = new char[]{880, 880, 882, 882, 0, 0, 886, 886, 0, 0, 890, 1021, 1022, 1023, 0, 895, 0, 0, 0, 0, 0, 0, 21393, 0, 21397, 21399, 21401, 0, 21407, 0, 21413, 21417, 54169, 5009, 914, 915, 916, 5013, 918, 5015, 920, 5017, 922, 923, 924, 925, 926, 5023, 928, 929, 0, 931, 932, 5029, 934, 935, 936, data2126, 37785, 37797, 21393, 21397, 21399, 21401, 54181, 5009, 914, 915, 916, 5013, 918, 5015, 920, 5017, 922, 923, 924, 925, 926, 5023, 928, 929, 931, 931, 932, 5029, 934, 935, 936, data2126, 37785, 37797, 21407, 21413, 21417, 975, 914, 920, 978, 17362, 33746, 934, 928, 975, 984, 984, 986, 986, 988, 988, 990, 990, 992, 992, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 922, 929, 1017, 895, 1012, 5013, 0, 1015, 1015, 1017, 1018, 1018, 1020, 1021, 1022, 1023};
        private static final char[] data1F00 = new char[]{5009, 5009, 21393, 21393, 21393, 21393, 21393, 21393, 5009, 5009, 21393, 21393, 21393, 21393, 21393, 21393, 5013, 5013, 21397, 21397, 21397, 21397, 0, 0, 5013, 5013, 21397, 21397, 21397, 21397, 0, 0, 5015, 5015, 21399, 21399, 21399, 21399, 21399, 21399, 5015, 5015, 21399, 21399, 21399, 21399, 21399, 21399, 5017, 5017, 21401, 21401, 21401, 21401, 21401, 21401, 5017, 5017, 21401, 21401, 21401, 21401, 21401, 21401, 5023, 5023, 21407, 21407, 21407, 21407, 0, 0, 5023, 5023, 21407, 21407, 21407, 21407, 0, 0, 5029, 5029, 21413, 21413, 21413, 21413, 21413, 21413, 0, 5029, 0, 21413, 0, 21413, 0, 21413, data2126, data2126, 21417, 21417, 21417, 21417, 21417, 21417, data2126, data2126, 21417, 21417, 21417, 21417, 21417, 21417, 21393, 21393, 21397, 21397, 21399, 21399, 21401, 21401, 21407, 21407, 21413, 21413, 21417, 21417, 0, 0, 13201, 13201, 29585, 29585, 29585, 29585, 29585, 29585, 13201, 13201, 29585, 29585, 29585, 29585, 29585, 29585, 13207, 13207, 29591, 29591, 29591, 29591, 29591, 29591, 13207, 13207, 29591, 29591, 29591, 29591, 29591, 29591, 13225, 13225, 29609, 29609, 29609, 29609, 29609, 29609, 13225, 13225, 29609, 29609, 29609, 29609, 29609, 29609, 5009, 5009, 29585, 13201, 29585, 0, 21393, 29585, 5009, 5009, 21393, 21393, 13201, 0, 5017, 0, 0, 0, 29591, 13207, 29591, 0, 21399, 29591, 21397, 21397, 21399, 21399, 13207, 0, 0, 0, 5017, 5017, 54169, 54169, 0, 0, 21401, 54169, 5017, 5017, 21401, 21401, 0, 0, 0, 0, 5029, 5029, 54181, 54181, 929, 929, 21413, 54181, 5029, 5029, 21413, 21413, 929, 0, 0, 0, 0, 0, 29609, 13225, 29609, 0, 21417, 29609, 21407, 21407, 21417, 21417, 13225, 0, 0, 0};
        private static final char data2126 = 'Ꭹ';

        private GreekUpper() {
        }

        private static final int getLetterData(int c) {
            if (c < 880 || 8486 < c || (1023 < c && c < 7936)) {
                return 0;
            }
            if (c <= 1023) {
                return data0370[c - 880];
            }
            if (c <= Opcodes.OP_SPUT_BYTE_JUMBO) {
                return data1F00[c - 7936];
            }
            if (c == 8486) {
                return 5033;
            }
            return 0;
        }

        private static final int getDiacriticData(int c) {
            if (c != 774) {
                if (c == 776) {
                    return 65536;
                }
                if (c != 785) {
                    switch (c) {
                        case CollationSettings.CASE_FIRST_AND_UPPER_MASK /*768*/:
                        case 769:
                        case 770:
                        case 771:
                            break;
                        case 772:
                            break;
                        default:
                            switch (c) {
                                case 787:
                                case 788:
                                    break;
                                default:
                                    switch (c) {
                                        case 834:
                                            break;
                                        case 835:
                                            break;
                                        case 836:
                                            return 81920;
                                        case 837:
                                            return 8192;
                                        default:
                                            return 0;
                                    }
                            }
                    }
                }
                return 16384;
            }
            return 131072;
        }

        private static boolean isFollowedByCasedLetter(CharSequence s, int i) {
            while (i < s.length()) {
                int c = Character.codePointAt(s, i);
                int type = UCaseProps.INSTANCE.getTypeOrIgnorable(c);
                if ((type & 4) != 0) {
                    i += Character.charCount(c);
                } else if (type != 0) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }

        private static <A extends Appendable> A toUpper(int options, CharSequence src, A dest, Edits edits) throws IOException {
            int i;
            int i2 = options;
            CharSequence charSequence = src;
            A a = dest;
            Edits edits2 = edits;
            int state = 0;
            int i3 = 0;
            while (i3 < src.length()) {
                int nextState;
                int c = Character.codePointAt(charSequence, i3);
                int nextIndex = Character.charCount(c) + i3;
                int nextState2 = 0;
                int type = UCaseProps.INSTANCE.getTypeOrIgnorable(c);
                if ((type & 4) != 0) {
                    nextState2 = 0 | (state & 1);
                } else if (type != 0) {
                    nextState2 = 0 | 1;
                }
                int data = getLetterData(c);
                if (data > 0) {
                    int diacriticData;
                    boolean change;
                    char upper = data & 1023;
                    if (!((data & 4096) == 0 || (state & 2) == 0 || (upper != 921 && upper != 933))) {
                        data |= 32768;
                    }
                    int numYpogegrammeni = 0;
                    if ((data & 8192) != 0) {
                        numYpogegrammeni = 1;
                    }
                    while (nextIndex < src.length()) {
                        diacriticData = getDiacriticData(charSequence.charAt(nextIndex));
                        if (diacriticData == 0) {
                            break;
                        }
                        data |= diacriticData;
                        if ((diacriticData & 8192) != 0) {
                            numYpogegrammeni++;
                        }
                        nextIndex++;
                    }
                    if ((HAS_VOWEL_AND_ACCENT_AND_DIALYTIKA & data) == HAS_VOWEL_AND_ACCENT) {
                        nextState2 |= 2;
                    }
                    boolean addTonos = false;
                    if (upper == 919 && (data & 16384) != 0 && numYpogegrammeni == 0 && (state & 1) == 0 && !isFollowedByCasedLetter(charSequence, nextIndex)) {
                        if (i3 == nextIndex) {
                            upper = 905;
                        } else {
                            addTonos = true;
                        }
                    } else if ((32768 & data) != 0) {
                        if (upper == 921) {
                            upper = 938;
                            data &= -98305;
                        } else if (upper == 933) {
                            upper = 939;
                            data &= -98305;
                        }
                    }
                    if (edits2 == null && (i2 & 16384) == 0) {
                        change = true;
                        i = state;
                        nextState = nextState2;
                    } else {
                        boolean z = true;
                        change = charSequence.charAt(i3) != upper || numYpogegrammeni > 0;
                        diacriticData = i3 + 1;
                        if ((data & HAS_EITHER_DIALYTIKA) != 0) {
                            if (diacriticData < nextIndex) {
                                nextState = nextState2;
                                if (charSequence.charAt(diacriticData) == 776) {
                                    state = 0;
                                    change |= state;
                                    diacriticData++;
                                }
                            } else {
                                nextState = nextState2;
                            }
                            state = 1;
                            change |= state;
                            diacriticData++;
                        } else {
                            nextState = nextState2;
                        }
                        if (addTonos) {
                            state = (diacriticData >= nextIndex || charSequence.charAt(diacriticData) != 769) ? 1 : 0;
                            change |= state;
                            diacriticData++;
                        }
                        state = nextIndex - i3;
                        nextState2 = (diacriticData - i3) + numYpogegrammeni;
                        change |= state != nextState2 ? 1 : 0;
                        if (!change) {
                            if (edits2 != null) {
                                edits2.addUnchanged(state);
                            }
                            if ((i2 & 16384) != 0) {
                                z = false;
                            }
                            change = z;
                        } else if (edits2 != null) {
                            edits2.addReplace(state, nextState2);
                        }
                    }
                    if (change) {
                        a.append((char) upper);
                        if ((data & HAS_EITHER_DIALYTIKA) != 0) {
                            a.append(776);
                        }
                        if (addTonos) {
                            a.append(769);
                        }
                        while (numYpogegrammeni > 0) {
                            a.append(921);
                            numYpogegrammeni--;
                        }
                    }
                } else {
                    CaseMapImpl.appendResult(UCaseProps.INSTANCE.toFullUpper(c, null, a, 4), a, nextIndex - i3, i2, edits2);
                    nextState = nextState2;
                }
                i3 = nextIndex;
                state = nextState;
                charSequence = src;
            }
            i = state;
            return a;
        }
    }

    public static final class StringContextIterator implements ContextIterator {
        protected int cpLimit = 0;
        protected int cpStart = 0;
        protected int dir = 0;
        protected int index = 0;
        protected int limit;
        protected CharSequence s;

        public StringContextIterator(CharSequence src) {
            this.s = src;
            this.limit = src.length();
        }

        public void setLimit(int lim) {
            if (lim < 0 || lim > this.s.length()) {
                this.limit = this.s.length();
            } else {
                this.limit = lim;
            }
        }

        public void moveToLimit() {
            int i = this.limit;
            this.cpLimit = i;
            this.cpStart = i;
        }

        public int nextCaseMapCP() {
            this.cpStart = this.cpLimit;
            if (this.cpLimit >= this.limit) {
                return -1;
            }
            int c = Character.codePointAt(this.s, this.cpLimit);
            this.cpLimit += Character.charCount(c);
            return c;
        }

        public int getCPStart() {
            return this.cpStart;
        }

        public int getCPLimit() {
            return this.cpLimit;
        }

        public int getCPLength() {
            return this.cpLimit - this.cpStart;
        }

        public void reset(int direction) {
            if (direction > 0) {
                this.dir = 1;
                this.index = this.cpLimit;
            } else if (direction < 0) {
                this.dir = -1;
                this.index = this.cpStart;
            } else {
                this.dir = 0;
                this.index = 0;
            }
        }

        public int next() {
            int c;
            if (this.dir > 0 && this.index < this.s.length()) {
                c = Character.codePointAt(this.s, this.index);
                this.index += Character.charCount(c);
                return c;
            } else if (this.dir >= 0 || this.index <= 0) {
                return -1;
            } else {
                c = Character.codePointBefore(this.s, this.index);
                this.index -= Character.charCount(c);
                return c;
            }
        }
    }

    private static final class WholeStringBreakIterator extends BreakIterator {
        private int length;

        private WholeStringBreakIterator() {
        }

        private static void notImplemented() {
            throw new UnsupportedOperationException("should not occur");
        }

        public int first() {
            return 0;
        }

        public int last() {
            notImplemented();
            return 0;
        }

        public int next(int n) {
            notImplemented();
            return 0;
        }

        public int next() {
            return this.length;
        }

        public int previous() {
            notImplemented();
            return 0;
        }

        public int following(int offset) {
            notImplemented();
            return 0;
        }

        public int current() {
            notImplemented();
            return 0;
        }

        public CharacterIterator getText() {
            notImplemented();
            return null;
        }

        public void setText(CharacterIterator newText) {
            this.length = newText.getEndIndex();
        }

        public void setText(CharSequence newText) {
            this.length = newText.length();
        }

        public void setText(String newText) {
            this.length = newText.length();
        }
    }

    public static int addTitleAdjustmentOption(int options, int newOption) {
        int adjOptions = options & TITLECASE_ADJUSTMENT_MASK;
        if (adjOptions == 0 || adjOptions == newOption) {
            return options | newOption;
        }
        throw new IllegalArgumentException("multiple titlecasing index adjustment options");
    }

    private static boolean isLNS(int c) {
        int gc = UCharacterProperty.INSTANCE.getType(c);
        if (((1 << gc) & LNS) != 0) {
            return true;
        }
        if (gc != 4 || UCaseProps.INSTANCE.getType(c) == 0) {
            return false;
        }
        return true;
    }

    public static int addTitleIteratorOption(int options, int newOption) {
        int iterOptions = options & 224;
        if (iterOptions == 0 || iterOptions == newOption) {
            return options | newOption;
        }
        throw new IllegalArgumentException("multiple titlecasing iterator options");
    }

    public static BreakIterator getTitleBreakIterator(Locale locale, int options, BreakIterator iter) {
        options &= 224;
        if (options != 0 && iter != null) {
            throw new IllegalArgumentException("titlecasing iterator option together with an explicit iterator");
        } else if (iter != null) {
            return iter;
        } else {
            if (options == 0) {
                return BreakIterator.getWordInstance(locale);
            }
            if (options == 32) {
                return new WholeStringBreakIterator();
            }
            if (options == 64) {
                return BreakIterator.getSentenceInstance(locale);
            }
            throw new IllegalArgumentException("unknown titlecasing iterator option");
        }
    }

    public static BreakIterator getTitleBreakIterator(ULocale locale, int options, BreakIterator iter) {
        options &= 224;
        if (options != 0 && iter != null) {
            throw new IllegalArgumentException("titlecasing iterator option together with an explicit iterator");
        } else if (iter != null) {
            return iter;
        } else {
            if (options == 0) {
                return BreakIterator.getWordInstance(locale);
            }
            if (options == 32) {
                return new WholeStringBreakIterator();
            }
            if (options == 64) {
                return BreakIterator.getSentenceInstance(locale);
            }
            throw new IllegalArgumentException("unknown titlecasing iterator option");
        }
    }

    private static int appendCodePoint(Appendable a, int c) throws IOException {
        if (c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            a.append((char) c);
            return 1;
        }
        a.append((char) (55232 + (c >> 10)));
        a.append((char) (UTF16.TRAIL_SURROGATE_MIN_VALUE + (c & Opcodes.OP_NEW_INSTANCE_JUMBO)));
        return 2;
    }

    private static void appendResult(int result, Appendable dest, int cpLength, int options, Edits edits) throws IOException {
        if (result < 0) {
            if (edits != null) {
                edits.addUnchanged(cpLength);
            }
            if ((options & 16384) == 0) {
                appendCodePoint(dest, ~result);
            }
        } else if (result > 31) {
            int length = appendCodePoint(dest, result);
            if (edits != null) {
                edits.addReplace(cpLength, length);
            }
        } else if (edits != null) {
            edits.addReplace(cpLength, result);
        }
    }

    private static final void appendUnchanged(CharSequence src, int start, int length, Appendable dest, int options, Edits edits) throws IOException {
        if (length > 0) {
            if (edits != null) {
                edits.addUnchanged(length);
            }
            if ((options & 16384) == 0) {
                dest.append(src, start, start + length);
            }
        }
    }

    private static String applyEdits(CharSequence src, StringBuilder replacementChars, Edits edits) {
        if (!edits.hasChanges()) {
            return src.toString();
        }
        StringBuilder result = new StringBuilder(src.length() + edits.lengthDelta());
        Iterator ei = edits.getCoarseIterator();
        while (ei.next()) {
            int i;
            if (ei.hasChange()) {
                i = ei.replacementIndex();
                result.append(replacementChars, i, ei.newLength() + i);
            } else {
                i = ei.sourceIndex();
                result.append(src, i, ei.oldLength() + i);
            }
        }
        return result.toString();
    }

    private static void internalToLower(int caseLocale, int options, StringContextIterator iter, Appendable dest, Edits edits) throws IOException {
        while (true) {
            int nextCaseMapCP = iter.nextCaseMapCP();
            int c = nextCaseMapCP;
            if (nextCaseMapCP >= 0) {
                appendResult(UCaseProps.INSTANCE.toFullLower(c, iter, dest, caseLocale), dest, iter.getCPLength(), options, edits);
            } else {
                return;
            }
        }
    }

    public static String toLower(int caseLocale, int options, CharSequence src) {
        if (src.length() > 100 || (options & 16384) != 0) {
            return ((StringBuilder) toLower(caseLocale, options, src, new StringBuilder(src.length()), null)).toString();
        }
        if (src.length() == 0) {
            return src.toString();
        }
        Edits edits = new Edits();
        return applyEdits(src, (StringBuilder) toLower(caseLocale, options | 16384, src, new StringBuilder(), edits), edits);
    }

    public static <A extends Appendable> A toLower(int caseLocale, int options, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        internalToLower(caseLocale, options, new StringContextIterator(src), dest, edits);
        return dest;
    }

    public static String toUpper(int caseLocale, int options, CharSequence src) {
        if (src.length() > 100 || (options & 16384) != 0) {
            return ((StringBuilder) toUpper(caseLocale, options, src, new StringBuilder(src.length()), null)).toString();
        }
        if (src.length() == 0) {
            return src.toString();
        }
        Edits edits = new Edits();
        return applyEdits(src, (StringBuilder) toUpper(caseLocale, options | 16384, src, new StringBuilder(), edits), edits);
    }

    public static <A extends Appendable> A toUpper(int caseLocale, int options, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        if (caseLocale == 4) {
            return GreekUpper.toUpper(options, src, dest, edits);
        }
        IOException e2 = new StringContextIterator(src);
        while (true) {
            int nextCaseMapCP = e2.nextCaseMapCP();
            int c = nextCaseMapCP;
            if (nextCaseMapCP < 0) {
                return dest;
            }
            appendResult(UCaseProps.INSTANCE.toFullUpper(c, e2, dest, caseLocale), dest, e2.getCPLength(), options, edits);
        }
    }

    public static String toTitle(int caseLocale, int options, BreakIterator iter, CharSequence src) {
        if (src.length() > 100 || (options & 16384) != 0) {
            return ((StringBuilder) toTitle(caseLocale, options, iter, src, new StringBuilder(src.length()), null)).toString();
        } else if (src.length() == 0) {
            return src.toString();
        } else {
            Edits edits = new Edits();
            return applyEdits(src, (StringBuilder) toTitle(caseLocale, options | 16384, iter, src, new StringBuilder(), edits), edits);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:71:0x013b  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x003d A:{Catch:{ IOException -> 0x0010 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static <A extends Appendable> A toTitle(int caseLocale, int options, BreakIterator titleIter, CharSequence src, A dest, Edits edits) {
        int i = caseLocale;
        int i2 = options;
        CharSequence charSequence = src;
        A a = dest;
        Edits edits2 = edits;
        if (edits2 != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        IOException e2 = new StringContextIterator(charSequence);
        int srcLength = src.length();
        int prev = 0;
        boolean isFirstIndex = true;
        while (prev < srcLength) {
            int index;
            int index2;
            int index3;
            if (isFirstIndex) {
                isFirstIndex = false;
                index = titleIter.first();
            } else {
                index = titleIter.next();
            }
            boolean isFirstIndex2 = isFirstIndex;
            int index4 = index;
            if (index4 != -1) {
                if (index4 > srcLength) {
                }
                index2 = index4;
                if (prev >= index2) {
                    int titleStart;
                    int titleStart2;
                    index4 = prev;
                    e2.setLimit(index2);
                    index = e2.nextCaseMapCP();
                    if ((i2 & 512) == 0) {
                        boolean toCased = (i2 & 1024) != 0;
                        while (true) {
                            boolean toCased2 = toCased;
                            if (!toCased2) {
                                if (isLNS(index)) {
                                    break;
                                }
                            } else if (UCaseProps.INSTANCE.getType(index) != 0) {
                                break;
                            }
                            int nextCaseMapCP = e2.nextCaseMapCP();
                            index = nextCaseMapCP;
                            if (nextCaseMapCP < 0) {
                                break;
                            }
                            toCased = toCased2;
                        }
                        int c = index;
                        titleStart = e2.getCPStart();
                        if (prev < titleStart) {
                            titleStart2 = titleStart;
                            index3 = index2;
                            appendUnchanged(charSequence, prev, titleStart - prev, a, i2, edits2);
                        } else {
                            titleStart2 = titleStart;
                            index3 = index2;
                        }
                        index = c;
                        index2 = titleStart2;
                    } else {
                        index3 = index2;
                        index2 = index4;
                    }
                    if (index2 < index3) {
                        int titleLimit = e2.getCPLimit();
                        titleStart = UCaseProps.INSTANCE.toFullTitle(index, e2, a, i);
                        appendResult(titleStart, a, e2.getCPLength(), i2, edits2);
                        int i3;
                        int i4;
                        if (index2 + 1 >= index3 || i != 5) {
                            i3 = titleStart;
                            i4 = index2;
                        } else {
                            char c1 = charSequence.charAt(index2);
                            if (c1 != UCharacterProperty.LATIN_SMALL_LETTER_I_) {
                                if (c1 != 'I') {
                                    i3 = titleStart;
                                    i4 = index2;
                                }
                            }
                            char c2 = charSequence.charAt(index2 + 1);
                            if (c2 == 'j') {
                                a.append('J');
                                if (edits2 != null) {
                                    index4 = 1;
                                    edits2.addReplace(1, 1);
                                } else {
                                    index4 = 1;
                                }
                                titleLimit++;
                                titleStart2 = index4;
                                i3 = e2.nextCaseMapCP();
                                i4 = index2;
                            } else if (c2 == 'J') {
                                titleStart2 = 1;
                                i4 = index2;
                                appendUnchanged(charSequence, index2 + 1, 1, a, i2, edits2);
                                titleLimit++;
                                i3 = e2.nextCaseMapCP();
                            } else {
                                titleStart2 = 1;
                                i3 = titleStart;
                                i4 = index2;
                            }
                        }
                        index2 = titleLimit;
                        if (index2 < index3) {
                            if ((i2 & 256) == 0) {
                                internalToLower(i, i2, e2, a, edits2);
                            } else {
                                titleLimit = index2;
                                appendUnchanged(charSequence, index2, index3 - index2, a, i2, edits2);
                                e2.moveToLimit();
                            }
                        }
                    }
                } else {
                    index3 = index2;
                }
                prev = index3;
                isFirstIndex = isFirstIndex2;
            }
            index4 = srcLength;
            index2 = index4;
            if (prev >= index2) {
            }
            prev = index3;
            isFirstIndex = isFirstIndex2;
        }
        return a;
    }

    public static String fold(int options, CharSequence src) {
        if (src.length() > 100 || (options & 16384) != 0) {
            return ((StringBuilder) fold(options, src, new StringBuilder(src.length()), null)).toString();
        }
        if (src.length() == 0) {
            return src.toString();
        }
        Edits edits = new Edits();
        return applyEdits(src, (StringBuilder) fold(options | 16384, src, new StringBuilder(), edits), edits);
    }

    public static <A extends Appendable> A fold(int options, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        IOException e2 = src.length();
        int i = 0;
        while (i < e2) {
            int c = Character.codePointAt(src, i);
            int cpLength = Character.charCount(c);
            i += cpLength;
            appendResult(UCaseProps.INSTANCE.toFullFolding(c, dest, options), dest, cpLength, options, edits);
        }
        return dest;
    }
}
