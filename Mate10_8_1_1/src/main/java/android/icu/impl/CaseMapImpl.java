package android.icu.impl;

import android.icu.impl.UCaseProps.ContextIterator;
import android.icu.text.BreakIterator;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Edits;
import android.icu.text.UTF16;
import android.icu.util.ICUUncheckedIOException;
import dalvik.bytecode.Opcodes;
import java.io.IOException;

public final class CaseMapImpl {
    static final /* synthetic */ boolean -assertionsDisabled = (CaseMapImpl.class.desiredAssertionStatus() ^ 1);
    public static final int OMIT_UNCHANGED_TEXT = 16384;

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
        private static final char[] data0370 = new char[]{'Ͱ', 'Ͱ', 'Ͳ', 'Ͳ', '\u0000', '\u0000', 'Ͷ', 'Ͷ', '\u0000', '\u0000', 'ͺ', 'Ͻ', 'Ͼ', 'Ͽ', '\u0000', 'Ϳ', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '厑', '\u0000', '厕', '厗', '厙', '\u0000', '原', '\u0000', '厥', '厩', '펙', '᎑', 'Β', 'Γ', 'Δ', '᎕', 'Ζ', '᎗', 'Θ', '᎙', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', '᎟', 'Π', 'Ρ', '\u0000', 'Σ', 'Τ', 'Ꭵ', 'Φ', 'Χ', 'Ψ', data2126, '鎙', '鎥', '厑', '厕', '厗', '厙', '펥', '᎑', 'Β', 'Γ', 'Δ', '᎕', 'Ζ', '᎗', 'Θ', '᎙', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', '᎟', 'Π', 'Ρ', 'Σ', 'Σ', 'Τ', 'Ꭵ', 'Φ', 'Χ', 'Ψ', data2126, '鎙', '鎥', '原', '厥', '厩', 'Ϗ', 'Β', 'Θ', 'ϒ', '䏒', '菒', 'Φ', 'Π', 'Ϗ', 'Ϙ', 'Ϙ', 'Ϛ', 'Ϛ', 'Ϝ', 'Ϝ', 'Ϟ', 'Ϟ', 'Ϡ', 'Ϡ', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', 'Κ', 'Ρ', 'Ϲ', 'Ϳ', 'ϴ', '᎕', '\u0000', 'Ϸ', 'Ϸ', 'Ϲ', 'Ϻ', 'Ϻ', 'ϼ', 'Ͻ', 'Ͼ', 'Ͽ'};
        private static final char[] data1F00 = new char[]{'᎑', '᎑', '厑', '厑', '厑', '厑', '厑', '厑', '᎑', '᎑', '厑', '厑', '厑', '厑', '厑', '厑', '᎕', '᎕', '厕', '厕', '厕', '厕', '\u0000', '\u0000', '᎕', '᎕', '厕', '厕', '厕', '厕', '\u0000', '\u0000', '᎗', '᎗', '厗', '厗', '厗', '厗', '厗', '厗', '᎗', '᎗', '厗', '厗', '厗', '厗', '厗', '厗', '᎙', '᎙', '厙', '厙', '厙', '厙', '厙', '厙', '᎙', '᎙', '厙', '厙', '厙', '厙', '厙', '厙', '᎟', '᎟', '原', '原', '原', '原', '\u0000', '\u0000', '᎟', '᎟', '原', '原', '原', '原', '\u0000', '\u0000', 'Ꭵ', 'Ꭵ', '厥', '厥', '厥', '厥', '厥', '厥', '\u0000', 'Ꭵ', '\u0000', '厥', '\u0000', '厥', '\u0000', '厥', data2126, data2126, '厩', '厩', '厩', '厩', '厩', '厩', data2126, data2126, '厩', '厩', '厩', '厩', '厩', '厩', '厑', '厑', '厕', '厕', '厗', '厗', '厙', '厙', '原', '原', '厥', '厥', '厩', '厩', '\u0000', '\u0000', '㎑', '㎑', '玑', '玑', '玑', '玑', '玑', '玑', '㎑', '㎑', '玑', '玑', '玑', '玑', '玑', '玑', '㎗', '㎗', '玗', '玗', '玗', '玗', '玗', '玗', '㎗', '㎗', '玗', '玗', '玗', '玗', '玗', '玗', '㎩', '㎩', '玩', '玩', '玩', '玩', '玩', '玩', '㎩', '㎩', '玩', '玩', '玩', '玩', '玩', '玩', '᎑', '᎑', '玑', '㎑', '玑', '\u0000', '厑', '玑', '᎑', '᎑', '厑', '厑', '㎑', '\u0000', '᎙', '\u0000', '\u0000', '\u0000', '玗', '㎗', '玗', '\u0000', '厗', '玗', '厕', '厕', '厗', '厗', '㎗', '\u0000', '\u0000', '\u0000', '᎙', '᎙', '펙', '펙', '\u0000', '\u0000', '厙', '펙', '᎙', '᎙', '厙', '厙', '\u0000', '\u0000', '\u0000', '\u0000', 'Ꭵ', 'Ꭵ', '펥', '펥', 'Ρ', 'Ρ', '厥', '펥', 'Ꭵ', 'Ꭵ', '厥', '厥', 'Ρ', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '玩', '㎩', '玩', '\u0000', '厩', '玩', '原', '原', '厩', '厩', '㎩', '\u0000', '\u0000', '\u0000'};
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
            switch (c) {
                case 768:
                case 769:
                case 770:
                case 771:
                case 785:
                case 834:
                    return 16384;
                case 772:
                case 774:
                case 787:
                case 788:
                case 835:
                    return 131072;
                case 776:
                    return 65536;
                case 836:
                    return 81920;
                case 837:
                    return 8192;
                default:
                    return 0;
            }
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
            int state = 0;
            int i = 0;
            while (i < src.length()) {
                int c = Character.codePointAt(src, i);
                int nextIndex = i + Character.charCount(c);
                int nextState = 0;
                int type = UCaseProps.INSTANCE.getTypeOrIgnorable(c);
                if ((type & 4) != 0) {
                    nextState = (state & 1) | 0;
                } else if (type != 0) {
                    nextState = 1;
                }
                int data = getLetterData(c);
                if (data > 0) {
                    boolean z;
                    char upper = data & 1023;
                    if (!((data & 4096) == 0 || (state & 2) == 0 || (upper != 'Ι' && upper != 'Υ'))) {
                        data |= 32768;
                    }
                    int numYpogegrammeni = 0;
                    if ((data & 8192) != 0) {
                        numYpogegrammeni = 1;
                    }
                    while (nextIndex < src.length()) {
                        int diacriticData = getDiacriticData(src.charAt(nextIndex));
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
                        nextState |= 2;
                    }
                    boolean addTonos = false;
                    if (upper == 'Η' && (data & 16384) != 0 && numYpogegrammeni == 0 && (state & 1) == 0 && (isFollowedByCasedLetter(src, nextIndex) ^ 1) != 0) {
                        if (i == nextIndex) {
                            upper = 'Ή';
                        } else {
                            addTonos = true;
                        }
                    } else if ((32768 & data) != 0) {
                        if (upper == 'Ι') {
                            upper = 'Ϊ';
                            data &= -98305;
                        } else if (upper == 'Υ') {
                            upper = 'Ϋ';
                            data &= -98305;
                        }
                    }
                    if (edits == null) {
                        z = true;
                    } else {
                        int i2;
                        int change = src.charAt(i) != upper || numYpogegrammeni > 0;
                        int i22 = i + 1;
                        if ((HAS_EITHER_DIALYTIKA & data) != 0) {
                            i2 = (i22 >= nextIndex || src.charAt(i22) != '̈') ? 1 : 0;
                            change |= i2;
                            i22++;
                        }
                        if (addTonos) {
                            i2 = (i22 >= nextIndex || src.charAt(i22) != '́') ? 1 : 0;
                            change |= i2;
                            i22++;
                        }
                        int oldLength = nextIndex - i;
                        int newLength = (i22 - i) + numYpogegrammeni;
                        z = change | (oldLength != newLength ? 1 : 0);
                        if (!z) {
                            if (edits != null) {
                                edits.addUnchanged(oldLength);
                            }
                            z = (options & 16384) == 0;
                        } else if (edits != null) {
                            edits.addReplace(oldLength, newLength);
                        }
                    }
                    if (z) {
                        dest.append((char) upper);
                        if ((HAS_EITHER_DIALYTIKA & data) != 0) {
                            dest.append('̈');
                        }
                        if (addTonos) {
                            dest.append('́');
                        }
                        while (numYpogegrammeni > 0) {
                            dest.append('Ι');
                            numYpogegrammeni--;
                        }
                    }
                } else {
                    CaseMapImpl.appendResult(UCaseProps.INSTANCE.toFullUpper(c, null, dest, 4), dest, nextIndex - i, options, edits);
                }
                i = nextIndex;
                state = nextState;
            }
            return dest;
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

    private static void appendResult(int r1, java.lang.Appendable r2, int r3, int r4, android.icu.text.Edits r5) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.icu.impl.CaseMapImpl.appendResult(int, java.lang.Appendable, int, int, android.icu.text.Edits):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.impl.CaseMapImpl.appendResult(int, java.lang.Appendable, int, int, android.icu.text.Edits):void");
    }

    private static int appendCodePoint(Appendable a, int c) throws IOException {
        if (c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            a.append((char) c);
            return 1;
        }
        a.append((char) ((c >> 10) + 55232));
        a.append((char) ((c & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE));
        return 2;
    }

    private static final void appendUnchanged(CharSequence src, int start, int length, Appendable dest, int options, Edits edits) throws IOException {
        if (length > 0) {
            if (edits != null) {
                edits.addUnchanged(length);
                if ((options & 16384) != 0) {
                    return;
                }
            }
            dest.append(src, start, start + length);
        }
    }

    private static void internalToLower(int caseLocale, int options, StringContextIterator iter, Appendable dest, Edits edits) throws IOException {
        while (true) {
            int c = iter.nextCaseMapCP();
            if (c >= 0) {
                appendResult(UCaseProps.INSTANCE.toFullLower(c, iter, dest, caseLocale), dest, iter.getCPLength(), options, edits);
            } else {
                return;
            }
        }
    }

    public static <A extends Appendable> A toLower(int caseLocale, int options, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (Throwable e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        internalToLower(caseLocale, options, new StringContextIterator(src), dest, edits);
        return dest;
    }

    public static <A extends Appendable> A toUpper(int caseLocale, int options, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (Throwable e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        if (caseLocale == 4) {
            return GreekUpper.toUpper(options, src, dest, edits);
        }
        StringContextIterator iter = new StringContextIterator(src);
        while (true) {
            int c = iter.nextCaseMapCP();
            if (c < 0) {
                return dest;
            }
            appendResult(UCaseProps.INSTANCE.toFullUpper(c, iter, dest, caseLocale), dest, iter.getCPLength(), options, edits);
        }
    }

    public static <A extends Appendable> A toTitle(int caseLocale, int options, BreakIterator titleIter, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (Throwable e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        StringContextIterator stringContextIterator = new StringContextIterator(src);
        int srcLength = src.length();
        boolean isFirstIndex = true;
        int index;
        for (int prev = 0; prev < srcLength; prev = index) {
            if (isFirstIndex) {
                isFirstIndex = false;
                index = titleIter.first();
            } else {
                index = titleIter.next();
            }
            if (index == -1 || index > srcLength) {
                index = srcLength;
            }
            if (prev < index) {
                int titleStart = prev;
                stringContextIterator.setLimit(index);
                int c = stringContextIterator.nextCaseMapCP();
                if ((options & 512) == 0 && UCaseProps.INSTANCE.getType(c) == 0) {
                    do {
                        c = stringContextIterator.nextCaseMapCP();
                        if (c < 0) {
                            break;
                        }
                    } while (UCaseProps.INSTANCE.getType(c) == 0);
                    titleStart = stringContextIterator.getCPStart();
                    appendUnchanged(src, prev, titleStart - prev, dest, options, edits);
                }
                if (titleStart < index) {
                    int titleLimit;
                    int titleLimit2 = stringContextIterator.getCPLimit();
                    appendResult(UCaseProps.INSTANCE.toFullTitle(c, stringContextIterator, dest, caseLocale), dest, stringContextIterator.getCPLength(), options, edits);
                    if (titleStart + 1 >= index || caseLocale != 5) {
                        titleLimit = titleLimit2;
                    } else {
                        char c1 = src.charAt(titleStart);
                        if (c1 == UCharacterProperty.LATIN_SMALL_LETTER_I_ || c1 == 'I') {
                            char c2 = src.charAt(titleStart + 1);
                            char c3;
                            if (c2 == 'j') {
                                dest.append('J');
                                if (edits != null) {
                                    edits.addReplace(1, 1);
                                }
                                c3 = stringContextIterator.nextCaseMapCP();
                                titleLimit = titleLimit2 + 1;
                                if (!-assertionsDisabled && c3 != c2) {
                                    throw new AssertionError();
                                } else if (!(-assertionsDisabled || titleLimit == stringContextIterator.getCPLimit())) {
                                    throw new AssertionError();
                                }
                            } else if (c2 == 'J') {
                                appendUnchanged(src, titleStart + 1, 1, dest, options, edits);
                                c3 = stringContextIterator.nextCaseMapCP();
                                titleLimit = titleLimit2 + 1;
                                if (!-assertionsDisabled && c3 != c2) {
                                    throw new AssertionError();
                                } else if (!(-assertionsDisabled || titleLimit == stringContextIterator.getCPLimit())) {
                                    throw new AssertionError();
                                }
                            } else {
                                titleLimit = titleLimit2;
                            }
                        } else {
                            titleLimit = titleLimit2;
                        }
                    }
                    if (titleLimit < index) {
                        if ((options & 256) == 0) {
                            internalToLower(caseLocale, options, stringContextIterator, dest, edits);
                        } else {
                            appendUnchanged(src, titleLimit, index - titleLimit, dest, options, edits);
                            stringContextIterator.moveToLimit();
                        }
                    }
                } else {
                    continue;
                }
            }
        }
        return dest;
    }

    public static <A extends Appendable> A fold(int options, CharSequence src, A dest, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (Throwable e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        int length = src.length();
        int i = 0;
        while (i < length) {
            int c = Character.codePointAt(src, i);
            int cpLength = Character.charCount(c);
            i += cpLength;
            appendResult(UCaseProps.INSTANCE.toFullFolding(c, dest, options), dest, cpLength, options, edits);
        }
        return dest;
    }
}
