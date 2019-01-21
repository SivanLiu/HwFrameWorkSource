package android.icu.text;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Norm2AllModes.Normalizer2WithImpl;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Normalizer2Impl.UTF16Plus;
import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.util.ICUCloneNotSupportedException;
import java.nio.CharBuffer;
import java.text.CharacterIterator;

public final class Normalizer implements Cloneable {
    public static final int COMPARE_CODE_POINT_ORDER = 32768;
    private static final int COMPARE_EQUIV = 524288;
    public static final int COMPARE_IGNORE_CASE = 65536;
    @Deprecated
    public static final int COMPARE_NORM_OPTIONS_SHIFT = 20;
    @Deprecated
    public static final Mode COMPOSE = NFC;
    @Deprecated
    public static final Mode COMPOSE_COMPAT = NFKC;
    @Deprecated
    public static final Mode DECOMP = NFD;
    @Deprecated
    public static final Mode DECOMP_COMPAT = NFKD;
    @Deprecated
    public static final Mode DEFAULT = NFC;
    @Deprecated
    public static final int DONE = -1;
    @Deprecated
    public static final Mode FCD = new FCDMode();
    public static final int FOLD_CASE_DEFAULT = 0;
    public static final int FOLD_CASE_EXCLUDE_SPECIAL_I = 1;
    @Deprecated
    public static final int IGNORE_HANGUL = 1;
    public static final int INPUT_IS_FCD = 131072;
    public static final QuickCheckResult MAYBE = new QuickCheckResult(2);
    @Deprecated
    public static final Mode NFC = new NFCMode();
    @Deprecated
    public static final Mode NFD = new NFDMode();
    @Deprecated
    public static final Mode NFKC = new NFKCMode();
    @Deprecated
    public static final Mode NFKD = new NFKDMode();
    public static final QuickCheckResult NO = new QuickCheckResult(0);
    @Deprecated
    public static final Mode NONE = new NONEMode();
    @Deprecated
    public static final Mode NO_OP = NONE;
    @Deprecated
    public static final int UNICODE_3_2 = 32;
    public static final QuickCheckResult YES = new QuickCheckResult(1);
    private StringBuilder buffer;
    private int bufferPos;
    private int currentIndex;
    private Mode mode;
    private int nextIndex;
    private Normalizer2 norm2;
    private int options;
    private UCharacterIterator text;

    private static final class CharsAppendable implements Appendable {
        private final char[] chars;
        private final int limit;
        private int offset;
        private final int start;

        public CharsAppendable(char[] dest, int destStart, int destLimit) {
            this.chars = dest;
            this.offset = destStart;
            this.start = destStart;
            this.limit = destLimit;
        }

        public int length() {
            int len = this.offset - this.start;
            if (this.offset <= this.limit) {
                return len;
            }
            throw new IndexOutOfBoundsException(Integer.toString(len));
        }

        public Appendable append(char c) {
            if (this.offset < this.limit) {
                this.chars[this.offset] = c;
            }
            this.offset++;
            return this;
        }

        public Appendable append(CharSequence s) {
            return append(s, 0, s.length());
        }

        public Appendable append(CharSequence s, int sStart, int sLimit) {
            int len = sLimit - sStart;
            if (len <= this.limit - this.offset) {
                while (sStart < sLimit) {
                    char[] cArr = this.chars;
                    int i = this.offset;
                    this.offset = i + 1;
                    int sStart2 = sStart + 1;
                    cArr[i] = s.charAt(sStart);
                    sStart = sStart2;
                }
            } else {
                this.offset += len;
            }
            return this;
        }
    }

    private static final class CmpEquivLevel {
        CharSequence cs;
        int s;

        private CmpEquivLevel() {
        }
    }

    private static final class FCD32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Norm2AllModes.getFCDNormalizer2(), Unicode32.INSTANCE));

        private FCD32ModeImpl() {
        }
    }

    private static final class FCDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Norm2AllModes.getFCDNormalizer2());

        private FCDModeImpl() {
        }
    }

    @Deprecated
    public static abstract class Mode {
        @Deprecated
        protected abstract Normalizer2 getNormalizer2(int i);

        @Deprecated
        protected Mode() {
        }
    }

    private static final class ModeImpl {
        private final Normalizer2 normalizer2;

        private ModeImpl(Normalizer2 n2) {
            this.normalizer2 = n2;
        }
    }

    private static final class NFC32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFCInstance(), Unicode32.INSTANCE));

        private NFC32ModeImpl() {
        }
    }

    private static final class NFCModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFCInstance());

        private NFCModeImpl() {
        }
    }

    private static final class NFD32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFDInstance(), Unicode32.INSTANCE));

        private NFD32ModeImpl() {
        }
    }

    private static final class NFDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFDInstance());

        private NFDModeImpl() {
        }
    }

    private static final class NFKC32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFKCInstance(), Unicode32.INSTANCE));

        private NFKC32ModeImpl() {
        }
    }

    private static final class NFKCModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFKCInstance());

        private NFKCModeImpl() {
        }
    }

    private static final class NFKD32ModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFKDInstance(), Unicode32.INSTANCE));

        private NFKD32ModeImpl() {
        }
    }

    private static final class NFKDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFKDInstance());

        private NFKDModeImpl() {
        }
    }

    public static final class QuickCheckResult {
        private QuickCheckResult(int value) {
        }
    }

    private static final class Unicode32 {
        private static final UnicodeSet INSTANCE = new UnicodeSet("[:age=3.2:]").freeze();

        private Unicode32() {
        }
    }

    private static final class FCDMode extends Mode {
        private FCDMode() {
        }

        protected Normalizer2 getNormalizer2(int options) {
            return ((options & 32) != 0 ? FCD32ModeImpl.INSTANCE : FCDModeImpl.INSTANCE).normalizer2;
        }
    }

    private static final class NFCMode extends Mode {
        private NFCMode() {
        }

        protected Normalizer2 getNormalizer2(int options) {
            return ((options & 32) != 0 ? NFC32ModeImpl.INSTANCE : NFCModeImpl.INSTANCE).normalizer2;
        }
    }

    private static final class NFDMode extends Mode {
        private NFDMode() {
        }

        protected Normalizer2 getNormalizer2(int options) {
            return ((options & 32) != 0 ? NFD32ModeImpl.INSTANCE : NFDModeImpl.INSTANCE).normalizer2;
        }
    }

    private static final class NFKCMode extends Mode {
        private NFKCMode() {
        }

        protected Normalizer2 getNormalizer2(int options) {
            return ((options & 32) != 0 ? NFKC32ModeImpl.INSTANCE : NFKCModeImpl.INSTANCE).normalizer2;
        }
    }

    private static final class NFKDMode extends Mode {
        private NFKDMode() {
        }

        protected Normalizer2 getNormalizer2(int options) {
            return ((options & 32) != 0 ? NFKD32ModeImpl.INSTANCE : NFKDModeImpl.INSTANCE).normalizer2;
        }
    }

    private static final class NONEMode extends Mode {
        private NONEMode() {
        }

        protected Normalizer2 getNormalizer2(int options) {
            return Norm2AllModes.NOOP_NORMALIZER2;
        }
    }

    @Deprecated
    public Normalizer(String str, Mode mode, int opt) {
        this.text = UCharacterIterator.getInstance(str);
        this.mode = mode;
        this.options = opt;
        this.norm2 = mode.getNormalizer2(opt);
        this.buffer = new StringBuilder();
    }

    @Deprecated
    public Normalizer(CharacterIterator iter, Mode mode, int opt) {
        this.text = UCharacterIterator.getInstance((CharacterIterator) iter.clone());
        this.mode = mode;
        this.options = opt;
        this.norm2 = mode.getNormalizer2(opt);
        this.buffer = new StringBuilder();
    }

    @Deprecated
    public Normalizer(UCharacterIterator iter, Mode mode, int options) {
        try {
            this.text = (UCharacterIterator) iter.clone();
            this.mode = mode;
            this.options = options;
            this.norm2 = mode.getNormalizer2(options);
            this.buffer = new StringBuilder();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    @Deprecated
    public Object clone() {
        try {
            Normalizer copy = (Normalizer) super.clone();
            copy.text = (UCharacterIterator) this.text.clone();
            copy.mode = this.mode;
            copy.options = this.options;
            copy.norm2 = this.norm2;
            copy.buffer = new StringBuilder(this.buffer);
            copy.bufferPos = this.bufferPos;
            copy.currentIndex = this.currentIndex;
            copy.nextIndex = this.nextIndex;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    private static final Normalizer2 getComposeNormalizer2(boolean compat, int options) {
        return (compat ? NFKC : NFC).getNormalizer2(options);
    }

    private static final Normalizer2 getDecomposeNormalizer2(boolean compat, int options) {
        return (compat ? NFKD : NFD).getNormalizer2(options);
    }

    @Deprecated
    public static String compose(String str, boolean compat) {
        return compose(str, compat, 0);
    }

    @Deprecated
    public static String compose(String str, boolean compat, int options) {
        return getComposeNormalizer2(compat, options).normalize(str);
    }

    @Deprecated
    public static int compose(char[] source, char[] target, boolean compat, int options) {
        return compose(source, 0, source.length, target, 0, target.length, compat, options);
    }

    @Deprecated
    public static int compose(char[] src, int srcStart, int srcLimit, char[] dest, int destStart, int destLimit, boolean compat, int options) {
        CharSequence srcBuffer = CharBuffer.wrap(src, srcStart, srcLimit - srcStart);
        Appendable app = new CharsAppendable(dest, destStart, destLimit);
        getComposeNormalizer2(compat, options).normalize(srcBuffer, app);
        return app.length();
    }

    @Deprecated
    public static String decompose(String str, boolean compat) {
        return decompose(str, compat, 0);
    }

    @Deprecated
    public static String decompose(String str, boolean compat, int options) {
        return getDecomposeNormalizer2(compat, options).normalize(str);
    }

    @Deprecated
    public static int decompose(char[] source, char[] target, boolean compat, int options) {
        return decompose(source, 0, source.length, target, 0, target.length, compat, options);
    }

    @Deprecated
    public static int decompose(char[] src, int srcStart, int srcLimit, char[] dest, int destStart, int destLimit, boolean compat, int options) {
        CharSequence srcBuffer = CharBuffer.wrap(src, srcStart, srcLimit - srcStart);
        Appendable app = new CharsAppendable(dest, destStart, destLimit);
        getDecomposeNormalizer2(compat, options).normalize(srcBuffer, app);
        return app.length();
    }

    @Deprecated
    public static String normalize(String str, Mode mode, int options) {
        return mode.getNormalizer2(options).normalize(str);
    }

    @Deprecated
    public static String normalize(String src, Mode mode) {
        return normalize(src, mode, 0);
    }

    @Deprecated
    public static int normalize(char[] source, char[] target, Mode mode, int options) {
        return normalize(source, 0, source.length, target, 0, target.length, mode, options);
    }

    @Deprecated
    public static int normalize(char[] src, int srcStart, int srcLimit, char[] dest, int destStart, int destLimit, Mode mode, int options) {
        CharSequence srcBuffer = CharBuffer.wrap(src, srcStart, srcLimit - srcStart);
        Appendable app = new CharsAppendable(dest, destStart, destLimit);
        mode.getNormalizer2(options).normalize(srcBuffer, app);
        return app.length();
    }

    @Deprecated
    public static String normalize(int char32, Mode mode, int options) {
        if (mode != NFD || options != 0) {
            return normalize(UTF16.valueOf(char32), mode, options);
        }
        String decomposition = Normalizer2.getNFCInstance().getDecomposition(char32);
        if (decomposition == null) {
            decomposition = UTF16.valueOf(char32);
        }
        return decomposition;
    }

    @Deprecated
    public static String normalize(int char32, Mode mode) {
        return normalize(char32, mode, 0);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(String source, Mode mode) {
        return quickCheck(source, mode, 0);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(String source, Mode mode, int options) {
        return mode.getNormalizer2(options).quickCheck(source);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(char[] source, Mode mode, int options) {
        return quickCheck(source, 0, source.length, mode, options);
    }

    @Deprecated
    public static QuickCheckResult quickCheck(char[] source, int start, int limit, Mode mode, int options) {
        return mode.getNormalizer2(options).quickCheck(CharBuffer.wrap(source, start, limit - start));
    }

    @Deprecated
    public static boolean isNormalized(char[] src, int start, int limit, Mode mode, int options) {
        return mode.getNormalizer2(options).isNormalized(CharBuffer.wrap(src, start, limit - start));
    }

    @Deprecated
    public static boolean isNormalized(String str, Mode mode, int options) {
        return mode.getNormalizer2(options).isNormalized(str);
    }

    @Deprecated
    public static boolean isNormalized(int char32, Mode mode, int options) {
        return isNormalized(UTF16.valueOf(char32), mode, options);
    }

    public static int compare(char[] s1, int s1Start, int s1Limit, char[] s2, int s2Start, int s2Limit, int options) {
        if (s1 != null && s1Start >= 0 && s1Limit >= 0 && s2 != null && s2Start >= 0 && s2Limit >= 0 && s1Limit >= s1Start && s2Limit >= s2Start) {
            return internalCompare(CharBuffer.wrap(s1, s1Start, s1Limit - s1Start), CharBuffer.wrap(s2, s2Start, s2Limit - s2Start), options);
        }
        throw new IllegalArgumentException();
    }

    public static int compare(String s1, String s2, int options) {
        return internalCompare(s1, s2, options);
    }

    public static int compare(char[] s1, char[] s2, int options) {
        return internalCompare(CharBuffer.wrap(s1), CharBuffer.wrap(s2), options);
    }

    public static int compare(int char32a, int char32b, int options) {
        return internalCompare(UTF16.valueOf(char32a), UTF16.valueOf(char32b), 131072 | options);
    }

    public static int compare(int char32a, String str2, int options) {
        return internalCompare(UTF16.valueOf(char32a), str2, options);
    }

    @Deprecated
    public static int concatenate(char[] left, int leftStart, int leftLimit, char[] right, int rightStart, int rightLimit, char[] dest, int destStart, int destLimit, Mode mode, int options) {
        if (dest == null) {
            throw new IllegalArgumentException();
        } else if (right != dest || rightStart >= destLimit || destStart >= rightLimit) {
            StringBuilder destBuilder = new StringBuilder((((leftLimit - leftStart) + rightLimit) - rightStart) + 16);
            destBuilder.append(left, leftStart, leftLimit - leftStart);
            mode.getNormalizer2(options).append(destBuilder, CharBuffer.wrap(right, rightStart, rightLimit - rightStart));
            int destLength = destBuilder.length();
            if (destLength <= destLimit - destStart) {
                destBuilder.getChars(0, destLength, dest, destStart);
                return destLength;
            }
            throw new IndexOutOfBoundsException(Integer.toString(destLength));
        } else {
            throw new IllegalArgumentException("overlapping right and dst ranges");
        }
    }

    @Deprecated
    public static String concatenate(char[] left, char[] right, Mode mode, int options) {
        return mode.getNormalizer2(options).append(new StringBuilder((left.length + right.length) + 16).append(left), CharBuffer.wrap(right)).toString();
    }

    @Deprecated
    public static String concatenate(String left, String right, Mode mode, int options) {
        return mode.getNormalizer2(options).append(new StringBuilder((left.length() + right.length()) + 16).append(left), right).toString();
    }

    @Deprecated
    public static int getFC_NFKC_Closure(int c, char[] dest) {
        String closure = getFC_NFKC_Closure(c);
        int length = closure.length();
        if (!(length == 0 || dest == null || length > dest.length)) {
            closure.getChars(0, length, dest, 0);
        }
        return length;
    }

    @Deprecated
    public static String getFC_NFKC_Closure(int c) {
        Normalizer2 nfkc = NFKCModeImpl.INSTANCE.normalizer2;
        UCaseProps csp = UCaseProps.INSTANCE;
        StringBuilder folded = new StringBuilder();
        int folded1Length = csp.toFullFolding(c, folded, 0);
        if (folded1Length < 0) {
            Normalizer2Impl nfkcImpl = ((Normalizer2WithImpl) nfkc).impl;
            if (nfkcImpl.getCompQuickCheck(nfkcImpl.getNorm16(c)) != 0) {
                return "";
            }
            folded.appendCodePoint(c);
        } else if (folded1Length > 31) {
            folded.appendCodePoint(folded1Length);
        }
        String kc1 = nfkc.normalize(folded);
        String kc2 = nfkc.normalize(UCharacter.foldCase(kc1, 0));
        if (kc1.equals(kc2)) {
            return "";
        }
        return kc2;
    }

    @Deprecated
    public int current() {
        if (this.bufferPos < this.buffer.length() || nextNormalize()) {
            return this.buffer.codePointAt(this.bufferPos);
        }
        return -1;
    }

    @Deprecated
    public int next() {
        if (this.bufferPos >= this.buffer.length() && !nextNormalize()) {
            return -1;
        }
        int c = this.buffer.codePointAt(this.bufferPos);
        this.bufferPos += Character.charCount(c);
        return c;
    }

    @Deprecated
    public int previous() {
        if (this.bufferPos <= 0 && !previousNormalize()) {
            return -1;
        }
        int c = this.buffer.codePointBefore(this.bufferPos);
        this.bufferPos -= Character.charCount(c);
        return c;
    }

    @Deprecated
    public void reset() {
        this.text.setToStart();
        this.nextIndex = 0;
        this.currentIndex = 0;
        clearBuffer();
    }

    @Deprecated
    public void setIndexOnly(int index) {
        this.text.setIndex(index);
        this.nextIndex = index;
        this.currentIndex = index;
        clearBuffer();
    }

    @Deprecated
    public int setIndex(int index) {
        setIndexOnly(index);
        return current();
    }

    @Deprecated
    public int getBeginIndex() {
        return 0;
    }

    @Deprecated
    public int getEndIndex() {
        return endIndex();
    }

    @Deprecated
    public int first() {
        reset();
        return next();
    }

    @Deprecated
    public int last() {
        this.text.setToLimit();
        int index = this.text.getIndex();
        this.nextIndex = index;
        this.currentIndex = index;
        clearBuffer();
        return previous();
    }

    @Deprecated
    public int getIndex() {
        if (this.bufferPos < this.buffer.length()) {
            return this.currentIndex;
        }
        return this.nextIndex;
    }

    @Deprecated
    public int startIndex() {
        return 0;
    }

    @Deprecated
    public int endIndex() {
        return this.text.getLength();
    }

    @Deprecated
    public void setMode(Mode newMode) {
        this.mode = newMode;
        this.norm2 = this.mode.getNormalizer2(this.options);
    }

    @Deprecated
    public Mode getMode() {
        return this.mode;
    }

    @Deprecated
    public void setOption(int option, boolean value) {
        if (value) {
            this.options |= option;
        } else {
            this.options &= ~option;
        }
        this.norm2 = this.mode.getNormalizer2(this.options);
    }

    @Deprecated
    public int getOption(int option) {
        if ((this.options & option) != 0) {
            return 1;
        }
        return 0;
    }

    @Deprecated
    public int getText(char[] fillIn) {
        return this.text.getText(fillIn);
    }

    @Deprecated
    public int getLength() {
        return this.text.getLength();
    }

    @Deprecated
    public String getText() {
        return this.text.getText();
    }

    @Deprecated
    public void setText(StringBuffer newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter != null) {
            this.text = newIter;
            reset();
            return;
        }
        throw new IllegalStateException("Could not create a new UCharacterIterator");
    }

    @Deprecated
    public void setText(char[] newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter != null) {
            this.text = newIter;
            reset();
            return;
        }
        throw new IllegalStateException("Could not create a new UCharacterIterator");
    }

    @Deprecated
    public void setText(String newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter != null) {
            this.text = newIter;
            reset();
            return;
        }
        throw new IllegalStateException("Could not create a new UCharacterIterator");
    }

    @Deprecated
    public void setText(CharacterIterator newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter != null) {
            this.text = newIter;
            reset();
            return;
        }
        throw new IllegalStateException("Could not create a new UCharacterIterator");
    }

    @Deprecated
    public void setText(UCharacterIterator newText) {
        try {
            UCharacterIterator newIter = (UCharacterIterator) newText.clone();
            if (newIter != null) {
                this.text = newIter;
                reset();
                return;
            }
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("Could not clone the UCharacterIterator", e);
        }
    }

    private void clearBuffer() {
        this.buffer.setLength(0);
        this.bufferPos = 0;
    }

    private boolean nextNormalize() {
        clearBuffer();
        this.currentIndex = this.nextIndex;
        this.text.setIndex(this.nextIndex);
        int c = this.text.nextCodePoint();
        boolean z = false;
        if (c < 0) {
            return false;
        }
        CharSequence segment = new StringBuilder().appendCodePoint(c);
        while (true) {
            int nextCodePoint = this.text.nextCodePoint();
            c = nextCodePoint;
            if (nextCodePoint < 0) {
                break;
            } else if (this.norm2.hasBoundaryBefore(c)) {
                this.text.moveCodePointIndex(-1);
                break;
            } else {
                segment.appendCodePoint(c);
            }
        }
        this.nextIndex = this.text.getIndex();
        this.norm2.normalize(segment, this.buffer);
        if (this.buffer.length() != 0) {
            z = true;
        }
        return z;
    }

    private boolean previousNormalize() {
        clearBuffer();
        this.nextIndex = this.currentIndex;
        this.text.setIndex(this.currentIndex);
        CharSequence segment = new StringBuilder();
        int c;
        do {
            int previousCodePoint = this.text.previousCodePoint();
            c = previousCodePoint;
            if (previousCodePoint < 0) {
                break;
            } else if (c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                segment.insert(0, (char) c);
            } else {
                segment.insert(0, Character.toChars(c));
            }
        } while (!this.norm2.hasBoundaryBefore(c));
        this.currentIndex = this.text.getIndex();
        this.norm2.normalize(segment, this.buffer);
        this.bufferPos = this.buffer.length();
        if (this.buffer.length() != 0) {
            return true;
        }
        return false;
    }

    private static int internalCompare(CharSequence s1, CharSequence s2, int options) {
        int normOptions = options >>> 20;
        options |= 524288;
        if ((131072 & options) == 0 || (options & 1) != 0) {
            Normalizer2 n2;
            if ((options & 1) != 0) {
                n2 = NFD.getNormalizer2(normOptions);
            } else {
                n2 = FCD.getNormalizer2(normOptions);
            }
            int spanQCYes1 = n2.spanQuickCheckYes(s1);
            int spanQCYes2 = n2.spanQuickCheckYes(s2);
            if (spanQCYes1 < s1.length()) {
                s1 = n2.normalizeSecondAndAppend(new StringBuilder(s1.length() + 16).append(s1, 0, spanQCYes1), s1.subSequence(spanQCYes1, s1.length()));
            }
            if (spanQCYes2 < s2.length()) {
                s2 = n2.normalizeSecondAndAppend(new StringBuilder(s2.length() + 16).append(s2, 0, spanQCYes2), s2.subSequence(spanQCYes2, s2.length()));
            }
        }
        return cmpEquivFold(s1, s2, options);
    }

    private static final CmpEquivLevel[] createCmpEquivLevelStack() {
        return new CmpEquivLevel[]{new CmpEquivLevel(), new CmpEquivLevel()};
    }

    /* JADX WARNING: Removed duplicated region for block: B:77:0x0175  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01db  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x0312 A:{SYNTHETIC, EDGE_INSN: B:190:0x0312->B:150:0x0312 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02ba  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x012a  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0175  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01db  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x023d  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02ba  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x0312 A:{SYNTHETIC, EDGE_INSN: B:190:0x0312->B:150:0x0312 ?: BREAK  } */
    /* JADX WARNING: Missing block: B:160:0x0332, code skipped:
            if (java.lang.Character.isLowSurrogate(r2.charAt(r11)) != false) goto L_0x0351;
     */
    /* JADX WARNING: Missing block: B:174:0x0362, code skipped:
            if (java.lang.Character.isLowSurrogate(r13.charAt(r14)) != false) goto L_0x0386;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int cmpEquivFold(CharSequence cs1, CharSequence cs2, int options) {
        Normalizer2Impl nfcImpl;
        UCaseProps csp;
        StringBuilder fold1;
        StringBuilder fold2;
        int s1;
        int c2;
        int s2;
        int limit1;
        int limit2;
        int i = options;
        if ((i & 524288) != 0) {
            nfcImpl = Norm2AllModes.getNFCInstance().impl;
        } else {
            nfcImpl = null;
        }
        if ((i & 65536) != 0) {
            csp = UCaseProps.INSTANCE;
            fold1 = new StringBuilder();
            fold2 = new StringBuilder();
        } else {
            csp = null;
            fold2 = null;
            fold1 = null;
        }
        int limit12 = cs1.length();
        int level1 = 0;
        int c22 = -1;
        int s22 = 0;
        int limit22 = cs2.length();
        int level2 = 0;
        CharSequence cs22 = cs2;
        CmpEquivLevel[] stack2 = null;
        int limit13 = limit12;
        CharSequence cs12 = cs1;
        limit12 = 0;
        CmpEquivLevel[] stack1 = null;
        int c1 = -1;
        while (true) {
            Normalizer2Impl nfcImpl2;
            int level22;
            int limit23;
            if (c1 < 0) {
                while (limit12 == limit13) {
                    if (level1 == 0) {
                        c1 = -1;
                        break;
                    }
                    while (true) {
                        level1--;
                        cs12 = stack1[level1].cs;
                        if (cs12 != null) {
                            break;
                        }
                    }
                    limit12 = stack1[level1].s;
                    limit13 = cs12.length();
                }
                s1 = limit12 + 1;
                c1 = cs12.charAt(limit12);
                limit12 = s1;
            }
            s1 = level1;
            if (c22 < 0) {
                CharSequence cs23 = cs22;
                int s23 = s22;
                c2 = limit22;
                while (s23 == c2) {
                    if (level2 == 0) {
                        c22 = -1;
                        nfcImpl2 = nfcImpl;
                        s2 = s23;
                        level22 = level2;
                        cs22 = cs23;
                        break;
                    }
                    while (true) {
                        level2--;
                        cs23 = stack2[level2].cs;
                        if (cs23 != null) {
                            break;
                        }
                    }
                    s23 = stack2[level2].s;
                    c2 = cs23.length();
                }
                s2 = s23 + 1;
                c22 = cs23.charAt(s23);
                nfcImpl2 = nfcImpl;
                cs22 = cs23;
                level22 = level2;
                limit23 = c2;
                c2 = c22;
            } else {
                nfcImpl2 = nfcImpl;
                c2 = c22;
                s2 = s22;
                limit23 = limit22;
                level22 = level2;
            }
            if (c1 == c2) {
                if (c1 < 0) {
                    return 0;
                }
                c22 = -1;
                c1 = -1;
                level2 = level22;
                level1 = s1;
                limit22 = limit23;
                s22 = s2;
                nfcImpl = nfcImpl2;
            } else if (c1 < 0) {
                return -1;
            } else {
                if (c2 < 0) {
                    return 1;
                }
                int cp2;
                int cp22;
                int length;
                int length2;
                UCaseProps csp2;
                Normalizer2Impl nfcImpl3;
                String decomposition;
                String decomp2;
                level1 = c1;
                CmpEquivLevel[] stack22 = stack2;
                if (UTF16.isSurrogate((char) c1)) {
                    char c;
                    if (!UTF16Plus.isSurrogateLead(c1)) {
                        limit1 = limit13;
                        if (limit12 - 2 >= 0) {
                            char charAt = cs12.charAt(limit12 - 2);
                            c = charAt;
                            if (Character.isHighSurrogate(charAt)) {
                                level1 = Character.toCodePoint(c, (char) c1);
                            }
                        }
                    } else if (limit12 != limit13) {
                        c = cs12.charAt(limit12);
                        char c3 = c;
                        if (Character.isLowSurrogate(c)) {
                            limit1 = limit13;
                            level1 = Character.toCodePoint((char) c1, c3);
                        }
                    }
                    limit13 = level1;
                    cp2 = c2;
                    if (UTF16.isSurrogate((char) c2)) {
                        Object cs13;
                        if (!UTF16Plus.isSurrogateLead(c2)) {
                            limit2 = limit23;
                            if (s2 - 2 >= 0) {
                                char charAt2 = cs22.charAt(s2 - 2);
                                c = charAt2;
                                if (Character.isHighSurrogate(charAt2)) {
                                    cp22 = Character.toCodePoint(c, (char) c2);
                                }
                            }
                            cp22 = cp2;
                        } else if (s2 != limit23) {
                            c = cs22.charAt(s2);
                            char c4 = c;
                            if (Character.isLowSurrogate(c)) {
                                limit2 = limit23;
                                cp22 = Character.toCodePoint((char) c2, c4);
                            }
                        }
                        if (s1 == 0 && (i & 65536) != 0) {
                            level1 = csp.toFullFolding(limit13, fold1, i);
                            length = level1;
                            if (level1 >= 0) {
                                if (UTF16.isSurrogate((char) c1)) {
                                    if (UTF16Plus.isSurrogateLead(c1)) {
                                        limit12++;
                                    } else {
                                        s2--;
                                        c2 = cs22.charAt(s2 - 1);
                                    }
                                }
                                c22 = c2;
                                s22 = s2;
                                if (stack1 == null) {
                                    stack1 = createCmpEquivLevelStack();
                                }
                                stack1[0].cs = cs12;
                                stack1[0].s = limit12;
                                level1 = s1 + 1;
                                s1 = length;
                                if (s1 <= 31) {
                                    fold1.delete(0, fold1.length() - s1);
                                } else {
                                    fold1.setLength(0);
                                    fold1.appendCodePoint(s1);
                                }
                                cs13 = fold1;
                                limit12 = 0;
                                c1 = -1;
                                limit13 = fold1.length();
                                level2 = level22;
                                nfcImpl = nfcImpl2;
                                stack2 = stack22;
                                limit22 = limit2;
                            }
                        }
                        if (level22 == 0 && (i & 65536) != 0) {
                            level1 = csp.toFullFolding(cp22, fold2, i);
                            length2 = level1;
                            if (level1 >= 0) {
                                if (UTF16.isSurrogate((char) c2)) {
                                    if (UTF16Plus.isSurrogateLead(c2)) {
                                        s2++;
                                    } else {
                                        limit12--;
                                        c1 = cs12.charAt(limit12 - 1);
                                    }
                                }
                                if (stack22 == null) {
                                    stack22 = createCmpEquivLevelStack();
                                }
                                int c12 = c1;
                                stack22[0].cs = cs22;
                                stack22[0].s = s2;
                                level2 = level22 + 1;
                                c1 = length2;
                                if (c1 <= 31) {
                                    fold2.delete(0, fold2.length() - c1);
                                } else {
                                    fold2.setLength(0);
                                    fold2.appendCodePoint(c1);
                                }
                                cs22 = fold2;
                                s22 = 0;
                                limit22 = fold2.length();
                                c22 = -1;
                                level1 = s1;
                                s2 = 0;
                                nfcImpl = nfcImpl2;
                                stack2 = stack22;
                                limit13 = limit1;
                                c1 = c12;
                            }
                        }
                        if (s1 < 2 || (i & 524288) == 0) {
                            csp2 = csp;
                            nfcImpl3 = nfcImpl2;
                        } else {
                            nfcImpl3 = nfcImpl2;
                            decomposition = nfcImpl3.getDecomposition(limit13);
                            String decomp1 = decomposition;
                            if (decomposition != null) {
                                csp2 = csp;
                                if (UTF16.isSurrogate((char) c1)) {
                                    if (UTF16Plus.isSurrogateLead(c1)) {
                                        limit12++;
                                    } else {
                                        s2--;
                                        c2 = cs22.charAt(s2 - 1);
                                    }
                                }
                                c22 = c2;
                                s22 = s2;
                                if (stack1 == null) {
                                    stack1 = createCmpEquivLevelStack();
                                }
                                stack1[s1].cs = cs12;
                                stack1[s1].s = limit12;
                                s1++;
                                if (s1 < 2) {
                                    c2 = s1 + 1;
                                    stack1[s1].cs = null;
                                    level1 = c2;
                                } else {
                                    level1 = s1;
                                }
                                cs13 = decomp1;
                                limit12 = 0;
                                c1 = -1;
                                level2 = level22;
                                limit13 = decomp1.length();
                                nfcImpl = nfcImpl3;
                                stack2 = stack22;
                                limit22 = limit2;
                                csp = csp2;
                            } else {
                                csp2 = csp;
                            }
                        }
                        if (level22 >= 2 || (i & 524288) == 0) {
                            break;
                        }
                        decomposition = nfcImpl3.getDecomposition(cp22);
                        decomp2 = decomposition;
                        if (decomposition == null) {
                            break;
                        }
                        int level12;
                        if (UTF16.isSurrogate((char) c2)) {
                            if (UTF16Plus.isSurrogateLead(c2)) {
                                s2++;
                            } else {
                                limit12--;
                                c1 = cs12.charAt(limit12 - 1);
                            }
                        }
                        if (stack22 == null) {
                            stack22 = createCmpEquivLevelStack();
                        }
                        stack22[level22].cs = cs22;
                        stack22[level22].s = s2;
                        level22++;
                        if (level22 < 2) {
                            int level23 = level22 + 1;
                            level12 = s1;
                            stack22[level22].cs = 0;
                            level2 = level23;
                        } else {
                            level12 = s1;
                            level2 = level22;
                        }
                        Object cs24 = decomp2;
                        s22 = 0;
                        limit22 = decomp2.length();
                        c22 = -1;
                        nfcImpl = nfcImpl3;
                        stack2 = stack22;
                        limit13 = limit1;
                        csp = csp2;
                        level1 = level12;
                    }
                    limit2 = limit23;
                    cp22 = cp2;
                    level1 = csp.toFullFolding(limit13, fold1, i);
                    length = level1;
                    if (level1 >= 0) {
                    }
                    level1 = csp.toFullFolding(cp22, fold2, i);
                    length2 = level1;
                    if (level1 >= 0) {
                    }
                    if (s1 < 2) {
                    }
                    csp2 = csp;
                    nfcImpl3 = nfcImpl2;
                    decomposition = nfcImpl3.getDecomposition(cp22);
                    decomp2 = decomposition;
                    if (decomposition == null) {
                    }
                }
                limit1 = limit13;
                limit13 = level1;
                cp2 = c2;
                if (UTF16.isSurrogate((char) c2)) {
                }
                limit2 = limit23;
                cp22 = cp2;
                level1 = csp.toFullFolding(limit13, fold1, i);
                length = level1;
                if (level1 >= 0) {
                }
                level1 = csp.toFullFolding(cp22, fold2, i);
                length2 = level1;
                if (level1 >= 0) {
                }
                if (s1 < 2) {
                }
                csp2 = csp;
                nfcImpl3 = nfcImpl2;
                decomposition = nfcImpl3.getDecomposition(cp22);
                decomp2 = decomposition;
                if (decomposition == null) {
                }
            }
        }
        if (c1 < 55296 || c2 < 55296 || (32768 & i) == 0) {
            s1 = limit2;
        } else {
            if (c1 > UTF16.LEAD_SURROGATE_MAX_VALUE) {
            } else if (limit12 != limit1) {
            }
            if (!(Character.isLowSurrogate((char) c1) && limit12 - 1 != 0 && Character.isHighSurrogate(cs12.charAt(limit12 - 2)))) {
                c1 -= 10240;
            }
            if (c2 > UTF16.LEAD_SURROGATE_MAX_VALUE) {
            } else if (s2 != limit2) {
            }
            if (!(Character.isLowSurrogate((char) c2) && s2 - 1 != 0 && Character.isHighSurrogate(cs22.charAt(s2 - 2)))) {
                c2 -= 10240;
            }
        }
        return c1 - c2;
    }
}
