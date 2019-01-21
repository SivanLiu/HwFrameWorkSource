package android.text;

import android.graphics.Paint.FontMetricsInt;
import android.text.AutoGrowArray.FloatArray;
import android.text.Layout.Alignment;
import android.text.Layout.Directions;
import android.text.PrecomputedText.ParagraphInfo;
import android.text.PrecomputedText.Params;
import android.text.TextUtils.TruncateAt;
import android.text.style.LeadingMarginSpan;
import android.text.style.LeadingMarginSpan.LeadingMarginSpan2;
import android.text.style.LineHeightSpan;
import android.text.style.LineHeightSpan.WithDensity;
import android.text.style.TabStopSpan;
import android.util.Log;
import android.util.Pools.SynchronizedPool;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class StaticLayout extends Layout {
    private static final char CHAR_NEW_LINE = '\n';
    private static final int COLUMNS_ELLIPSIZE = 7;
    private static final int COLUMNS_NORMAL = 5;
    private static final int DEFAULT_MAX_LINE_HEIGHT = -1;
    private static final int DESCENT = 2;
    private static final int DIR = 0;
    private static final int DIR_SHIFT = 30;
    private static final int ELLIPSIS_COUNT = 6;
    private static final int ELLIPSIS_START = 5;
    private static final int EXTRA = 3;
    private static final double EXTRA_ROUNDING = 0.5d;
    private static final int HYPHEN = 4;
    private static final int HYPHEN_MASK = 255;
    private static final int ROWHEIGHTOFFSET_BO = 7;
    private static final int ROWHEIGHTOFFSET_MY = 2;
    private static final int START = 0;
    private static final int START_MASK = 536870911;
    private static final int TAB = 0;
    private static final int TAB_INCREMENT = 20;
    private static final int TAB_MASK = 536870912;
    static final String TAG = "StaticLayout";
    private static final int TOP = 1;
    private int mBottomPadding;
    private int mColumns;
    private boolean mEllipsized;
    private int mEllipsizedWidth;
    private int[] mLeftIndents;
    private int[] mLeftPaddings;
    private int mLineCount;
    private Directions[] mLineDirections;
    private int[] mLines;
    private int mMaxLineHeight;
    private int mMaximumVisibleLineCount;
    private int[] mRightIndents;
    private int[] mRightPaddings;
    private int mTopPadding;

    public static final class Builder {
        private static final SynchronizedPool<Builder> sPool = new SynchronizedPool(3);
        private boolean mAddLastLineLineSpacing;
        private Alignment mAlignment;
        private int mBreakStrategy;
        private TruncateAt mEllipsize;
        private int mEllipsizedWidth;
        private int mEnd;
        private boolean mFallbackLineSpacing;
        private final FontMetricsInt mFontMetricsInt = new FontMetricsInt();
        private int mHyphenationFrequency;
        private boolean mIncludePad;
        private int mJustificationMode;
        private int[] mLeftIndents;
        private int[] mLeftPaddings;
        private int mMaxLines;
        private TextPaint mPaint;
        private int[] mRightIndents;
        private int[] mRightPaddings;
        private float mSpacingAdd;
        private float mSpacingMult;
        private int mStart;
        private CharSequence mText;
        private TextDirectionHeuristic mTextDir;
        private int mWidth;

        private Builder() {
        }

        public static Builder obtain(CharSequence source, int start, int end, TextPaint paint, int width) {
            Builder b = (Builder) sPool.acquire();
            if (b == null) {
                b = new Builder();
            }
            b.mText = source;
            b.mStart = start;
            b.mEnd = end;
            b.mPaint = paint;
            b.mWidth = width;
            b.mAlignment = Alignment.ALIGN_NORMAL;
            b.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            b.mSpacingMult = 1.0f;
            b.mSpacingAdd = 0.0f;
            b.mIncludePad = true;
            b.mFallbackLineSpacing = false;
            b.mEllipsizedWidth = width;
            b.mEllipsize = null;
            b.mMaxLines = Integer.MAX_VALUE;
            b.mBreakStrategy = 0;
            b.mHyphenationFrequency = 0;
            b.mJustificationMode = 0;
            return b;
        }

        private static void recycle(Builder b) {
            b.mPaint = null;
            b.mText = null;
            b.mLeftIndents = null;
            b.mRightIndents = null;
            b.mLeftPaddings = null;
            b.mRightPaddings = null;
            sPool.release(b);
        }

        void finish() {
            this.mText = null;
            this.mPaint = null;
            this.mLeftIndents = null;
            this.mRightIndents = null;
            this.mLeftPaddings = null;
            this.mRightPaddings = null;
        }

        public Builder setText(CharSequence source) {
            return setText(source, 0, source.length());
        }

        public Builder setText(CharSequence source, int start, int end) {
            this.mText = source;
            this.mStart = start;
            this.mEnd = end;
            return this;
        }

        public Builder setPaint(TextPaint paint) {
            this.mPaint = paint;
            return this;
        }

        public Builder setWidth(int width) {
            this.mWidth = width;
            if (this.mEllipsize == null) {
                this.mEllipsizedWidth = width;
            }
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            this.mAlignment = alignment;
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic textDir) {
            this.mTextDir = textDir;
            return this;
        }

        public Builder setLineSpacing(float spacingAdd, float spacingMult) {
            this.mSpacingAdd = spacingAdd;
            this.mSpacingMult = spacingMult;
            return this;
        }

        public Builder setIncludePad(boolean includePad) {
            this.mIncludePad = includePad;
            return this;
        }

        public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks) {
            this.mFallbackLineSpacing = useLineSpacingFromFallbacks;
            return this;
        }

        public Builder setEllipsizedWidth(int ellipsizedWidth) {
            this.mEllipsizedWidth = ellipsizedWidth;
            return this;
        }

        public Builder setEllipsize(TruncateAt ellipsize) {
            this.mEllipsize = ellipsize;
            return this;
        }

        public Builder setMaxLines(int maxLines) {
            this.mMaxLines = maxLines;
            return this;
        }

        public Builder setBreakStrategy(int breakStrategy) {
            this.mBreakStrategy = breakStrategy;
            return this;
        }

        public Builder setHyphenationFrequency(int hyphenationFrequency) {
            this.mHyphenationFrequency = hyphenationFrequency;
            return this;
        }

        public Builder setIndents(int[] leftIndents, int[] rightIndents) {
            this.mLeftIndents = leftIndents;
            this.mRightIndents = rightIndents;
            return this;
        }

        public Builder setAvailablePaddings(int[] leftPaddings, int[] rightPaddings) {
            this.mLeftPaddings = leftPaddings;
            this.mRightPaddings = rightPaddings;
            return this;
        }

        public Builder setJustificationMode(int justificationMode) {
            this.mJustificationMode = justificationMode;
            return this;
        }

        Builder setAddLastLineLineSpacing(boolean value) {
            this.mAddLastLineLineSpacing = value;
            return this;
        }

        public StaticLayout build() {
            StaticLayout result = new StaticLayout(this);
            recycle(this);
            return result;
        }
    }

    static class LineBreaks {
        private static final int INITIAL_SIZE = 16;
        public float[] ascents = new float[16];
        public int[] breaks = new int[16];
        public float[] descents = new float[16];
        public int[] flags = new int[16];
        public float[] widths = new float[16];

        LineBreaks() {
        }
    }

    private static native int nComputeLineBreaks(long j, char[] cArr, long j2, int i, float f, int i2, float f2, int[] iArr, int i3, int i4, LineBreaks lineBreaks, int i5, int[] iArr2, float[] fArr, float[] fArr2, float[] fArr3, int[] iArr3, float[] fArr4);

    private static native void nFinish(long j);

    private static native long nInit(int i, int i2, boolean z, int[] iArr, int[] iArr2, int[] iArr3);

    @Deprecated
    public StaticLayout(CharSequence source, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(source, 0, source.length(), paint, width, align, spacingmult, spacingadd, includepad);
    }

    @Deprecated
    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(source, bufstart, bufend, paint, outerwidth, align, spacingmult, spacingadd, includepad, null, 0);
    }

    @Deprecated
    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Alignment align, float spacingmult, float spacingadd, boolean includepad, TruncateAt ellipsize, int ellipsizedWidth) {
        this(source, bufstart, bufend, paint, outerwidth, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingmult, spacingadd, includepad, ellipsize, ellipsizedWidth, Integer.MAX_VALUE);
    }

    @Deprecated
    public StaticLayout(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerwidth, Alignment align, TextDirectionHeuristic textDir, float spacingmult, float spacingadd, boolean includepad, TruncateAt ellipsize, int ellipsizedWidth, int maxLines) {
        CharSequence charSequence;
        CharSequence charSequence2 = source;
        TruncateAt truncateAt = ellipsize;
        int i = ellipsizedWidth;
        int i2 = maxLines;
        if (truncateAt == null) {
            charSequence = charSequence2;
        } else {
            SpannedEllipsizer spannedEllipsizer;
            if (charSequence2 instanceof Spanned) {
                spannedEllipsizer = new SpannedEllipsizer(charSequence2);
            } else {
                spannedEllipsizer = new Ellipsizer(charSequence2);
            }
            charSequence = spannedEllipsizer;
        }
        super(charSequence, paint, outerwidth, align, textDir, spacingmult, spacingadd);
        this.mMaxLineHeight = -1;
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        Builder b = Builder.obtain(source, bufstart, bufend, paint, outerwidth).setAlignment(align).setTextDirection(textDir).setLineSpacing(spacingadd, spacingmult).setIncludePad(includepad).setEllipsizedWidth(i).setEllipsize(truncateAt).setMaxLines(i2);
        if (truncateAt != null) {
            Ellipsizer e = (Ellipsizer) getText();
            e.mLayout = this;
            e.mWidth = i;
            e.mMethod = truncateAt;
            this.mEllipsizedWidth = i;
            this.mColumns = 7;
            int i3 = outerwidth;
        } else {
            this.mColumns = 5;
            this.mEllipsizedWidth = outerwidth;
        }
        this.mLineDirections = (Directions[]) ArrayUtils.newUnpaddedArray(Directions.class, 2);
        this.mLines = ArrayUtils.newUnpaddedIntArray(2 * this.mColumns);
        this.mMaximumVisibleLineCount = i2;
        generate(b, b.mIncludePad, b.mIncludePad);
        Builder.recycle(b);
    }

    StaticLayout(CharSequence text) {
        super(text, null, 0, null, 0.0f, 0.0f);
        this.mMaxLineHeight = -1;
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        this.mColumns = 7;
        this.mLineDirections = (Directions[]) ArrayUtils.newUnpaddedArray(Directions.class, 2);
        this.mLines = ArrayUtils.newUnpaddedIntArray(2 * this.mColumns);
    }

    private StaticLayout(Builder b) {
        CharSequence access$400;
        if (b.mEllipsize == null) {
            access$400 = b.mText;
        } else if (b.mText instanceof Spanned) {
            access$400 = new SpannedEllipsizer(b.mText);
        } else {
            access$400 = new Ellipsizer(b.mText);
        }
        super(access$400, b.mPaint, b.mWidth, b.mAlignment, b.mTextDir, b.mSpacingMult, b.mSpacingAdd);
        this.mMaxLineHeight = -1;
        this.mMaximumVisibleLineCount = Integer.MAX_VALUE;
        if (b.mEllipsize != null) {
            Ellipsizer e = (Ellipsizer) getText();
            e.mLayout = this;
            e.mWidth = b.mEllipsizedWidth;
            e.mMethod = b.mEllipsize;
            this.mEllipsizedWidth = b.mEllipsizedWidth;
            this.mColumns = 7;
        } else {
            this.mColumns = 5;
            this.mEllipsizedWidth = b.mWidth;
        }
        this.mLineDirections = (Directions[]) ArrayUtils.newUnpaddedArray(Directions.class, 2);
        this.mLines = ArrayUtils.newUnpaddedIntArray(2 * this.mColumns);
        this.mMaximumVisibleLineCount = b.mMaxLines;
        this.mLeftIndents = b.mLeftIndents;
        this.mRightIndents = b.mRightIndents;
        this.mLeftPaddings = b.mLeftPaddings;
        this.mRightPaddings = b.mRightPaddings;
        setJustificationMode(b.mJustificationMode);
        generate(b, b.mIncludePad, b.mIncludePad);
    }

    /* JADX WARNING: Removed duplicated region for block: B:149:0x0399 A:{Catch:{ all -> 0x036d }} */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x03e4  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x06de A:{LOOP_END, LOOP:2: B:49:0x0173->B:253:0x06de} */
    /* JADX WARNING: Removed duplicated region for block: B:305:0x06d9 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x015c  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0176  */
    /* JADX WARNING: Removed duplicated region for block: B:277:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:269:0x07d3  */
    /* JADX WARNING: Removed duplicated region for block: B:292:0x0855  */
    /* JADX WARNING: Removed duplicated region for block: B:281:0x07f7  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void generate(Builder b, boolean includepad, boolean trackpad) {
        FontMetricsInt fm;
        int[] indents;
        int leftLen;
        int rightLen;
        int i;
        int i2;
        ParagraphInfo[] paragraphInfo;
        FontMetricsInt fm2;
        Spanned spanned;
        long nativePtr;
        float ellipsizedWidth;
        boolean ellipsizedWidth2;
        FloatArray widths;
        LineBreaks lineBreaks;
        TruncateAt ellipsize;
        ParagraphInfo[] paragraphInfo2;
        ParagraphInfo[] paragraphInfo3;
        int[] chooseHtv;
        int paraStart;
        Throwable th;
        int i3;
        ParagraphInfo[] paragraphInfoArr;
        int i4;
        Spanned spanned2;
        StaticLayout staticLayout;
        long nativePtr2;
        float f;
        FloatArray floatArray;
        LineBreaks lineBreaks2;
        TruncateAt truncateAt;
        int[] iArr;
        TextDirectionHeuristic textDirectionHeuristic;
        TextPaint textDir;
        TextDirectionHeuristic textDir2;
        TextDirectionHeuristic textDir3;
        TextPaint paint;
        CharSequence source;
        StaticLayout staticLayout2 = this;
        String language = Locale.getDefault().getLanguage();
        HashMap<String, Integer> languageMap = new HashMap();
        languageMap.put("bo", Integer.valueOf(7));
        languageMap.put("my", Integer.valueOf(2));
        CharSequence source2 = b.mText;
        int bufStart = b.mStart;
        int bufEnd = b.mEnd;
        TextPaint paint2 = b.mPaint;
        int outerWidth = b.mWidth;
        TextDirectionHeuristic textDir4 = b.mTextDir;
        boolean fallbackLineSpacing = b.mFallbackLineSpacing;
        float spacingmult = b.mSpacingMult;
        float spacingadd = b.mSpacingAdd;
        float ellipsizedWidth3 = (float) b.mEllipsizedWidth;
        TruncateAt ellipsize2 = b.mEllipsize;
        boolean addLastLineSpacing = b.mAddLastLineLineSpacing;
        LineBreaks lineBreaks3 = new LineBreaks();
        FloatArray widths2 = new FloatArray();
        staticLayout2.mLineCount = 0;
        staticLayout2.mEllipsized = false;
        staticLayout2.mMaxLineHeight = staticLayout2.mMaximumVisibleLineCount < 1 ? 0 : -1;
        boolean needMultiply = (spacingmult == 1.0f && spacingadd == 0.0f) ? false : true;
        FontMetricsInt fm3 = b.mFontMetricsInt;
        if (staticLayout2.mLeftIndents == null && staticLayout2.mRightIndents == null) {
            fm = fm3;
            indents = null;
        } else {
            leftLen = staticLayout2.mLeftIndents == null ? 0 : staticLayout2.mLeftIndents.length;
            rightLen = staticLayout2.mRightIndents == null ? 0 : staticLayout2.mRightIndents.length;
            int[] indents2 = new int[Math.max(leftLen, rightLen)];
            i = 0;
            while (true) {
                fm = fm3;
                i2 = i;
                if (i2 >= leftLen) {
                    break;
                }
                int leftLen2 = leftLen;
                indents2[i2] = staticLayout2.mLeftIndents[i2];
                i = i2 + 1;
                fm3 = fm;
                leftLen = leftLen2;
            }
            leftLen = 0;
            while (leftLen < rightLen) {
                int rightLen2 = rightLen;
                indents2[leftLen] = indents2[leftLen] + staticLayout2.mRightIndents[leftLen];
                leftLen++;
                rightLen = rightLen2;
            }
            indents = indents2;
        }
        long nativePtr3 = nInit(b.mBreakStrategy, b.mHyphenationFrequency, b.mJustificationMode != 0, indents, staticLayout2.mLeftPaddings, staticLayout2.mRightPaddings);
        Spanned spanned3 = source2 instanceof Spanned ? (Spanned) source2 : null;
        if (source2 instanceof PrecomputedText) {
            PrecomputedText precomputedText = (PrecomputedText) source2;
            PrecomputedText precomputed = precomputedText;
            paragraphInfo = null;
            fm2 = fm;
            spanned = spanned3;
            nativePtr = nativePtr3;
            ellipsizedWidth = ellipsizedWidth3;
            ellipsizedWidth2 = false;
            widths = widths2;
            lineBreaks = lineBreaks3;
            ellipsize = ellipsize2;
            if (precomputedText.canUseMeasuredResult(bufStart, bufEnd, textDir4, paint2, b.mBreakStrategy, b.mHyphenationFrequency)) {
                CharSequence charSequence;
                paragraphInfo2 = precomputed.getParagraphInfo();
                if (paragraphInfo2 == null) {
                    paragraphInfo2 = PrecomputedText.createMeasuredParagraphs(source2, new Params(paint2, textDir4, b.mBreakStrategy, b.mHyphenationFrequency), bufStart, bufEnd, ellipsizedWidth2);
                }
                paragraphInfo3 = paragraphInfo2;
                leftLen = ellipsizedWidth2;
                rightLen = 0;
                chooseHtv = null;
                while (leftLen < paragraphInfo3.length) {
                    try {
                        int firstWidthLineCount;
                        int i5;
                        TextPaint paint3;
                        int bufStart2;
                        CharSequence source3;
                        LineHeightSpan[] chooseHt;
                        int firstWidthLineCount2;
                        int firstWidth;
                        MeasuredParagraph measuredPara;
                        char[] chs;
                        int[] spanEndCache;
                        int[] fmCache;
                        FloatArray widths3;
                        ParagraphInfo[] paragraphInfo4;
                        Spanned spanned4;
                        if (leftLen == 0) {
                            paraStart = bufStart;
                        } else {
                            try {
                                paraStart = paragraphInfo3[leftLen - 1].paragraphEnd;
                            } catch (Throwable th2) {
                                th = th2;
                                i3 = rightLen;
                                paragraphInfoArr = paragraphInfo3;
                                paraStart = bufEnd;
                                i4 = bufStart;
                                spanned2 = spanned;
                                rightLen = fm2;
                                staticLayout = staticLayout2;
                                nativePtr2 = nativePtr;
                                f = ellipsizedWidth;
                                floatArray = widths;
                                lineBreaks2 = lineBreaks;
                                truncateAt = ellipsize;
                                iArr = chooseHtv;
                                textDirectionHeuristic = textDir4;
                                textDir = paint2;
                                paragraphInfo3 = i4;
                                charSequence = source2;
                                nFinish(nativePtr2);
                                throw th;
                            }
                        }
                        int paraEnd = paragraphInfo3[leftLen].paragraphEnd;
                        int firstWidth2 = outerWidth;
                        int restWidth = outerWidth;
                        if (spanned != null) {
                            try {
                                LeadingMarginSpan[] sp = (LeadingMarginSpan[]) Layout.getParagraphSpans(spanned, paraStart, paraEnd, LeadingMarginSpan.class);
                                textDir2 = textDir4;
                                firstWidthLineCount = 1;
                                i5 = 0;
                                while (true) {
                                    paint3 = paint2;
                                    try {
                                        if (i5 >= sp.length) {
                                            break;
                                        }
                                        LeadingMarginSpan lms = sp[i5];
                                        bufStart2 = bufStart;
                                        try {
                                            source3 = source2;
                                            firstWidth2 -= sp[i5].getLeadingMargin(1);
                                            restWidth -= sp[i5].getLeadingMargin(0);
                                            if ((lms instanceof LeadingMarginSpan2) != 0) {
                                                firstWidthLineCount = Math.max(firstWidthLineCount, ((LeadingMarginSpan2) lms).getLeadingMarginLineCount());
                                            }
                                            i5++;
                                            paint2 = paint3;
                                            bufStart = bufStart2;
                                            source2 = source3;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            iArr = chooseHtv;
                                            paraStart = bufEnd;
                                            rightLen = fm2;
                                            staticLayout = staticLayout2;
                                            nativePtr2 = nativePtr;
                                            paraEnd = textDir2;
                                            paragraphInfo3 = bufStart2;
                                            nFinish(nativePtr2);
                                            throw th;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        iArr = chooseHtv;
                                        i3 = rightLen;
                                        paragraphInfoArr = paragraphInfo3;
                                        paragraphInfo3 = bufStart;
                                        charSequence = source2;
                                        spanned2 = spanned;
                                        rightLen = fm2;
                                        staticLayout = staticLayout2;
                                        nativePtr2 = nativePtr;
                                        f = ellipsizedWidth;
                                        floatArray = widths;
                                        lineBreaks2 = lineBreaks;
                                        truncateAt = ellipsize;
                                        textDir = paint3;
                                        nFinish(nativePtr2);
                                        throw th;
                                    }
                                }
                                bufStart2 = bufStart;
                                source3 = source2;
                                LineHeightSpan[] chooseHt2 = (LineHeightSpan[]) Layout.getParagraphSpans(spanned, paraStart, paraEnd, LineHeightSpan.class);
                                if (chooseHt2.length == 0) {
                                    chooseHt2 = null;
                                } else {
                                    if (chooseHtv == null || chooseHtv.length < chooseHt2.length) {
                                        chooseHtv = ArrayUtils.newUnpaddedIntArray(chooseHt2.length);
                                    }
                                    for (paint2 = null; paint2 < chooseHt2.length; paint2++) {
                                        bufStart = spanned.getSpanStart(chooseHt2[paint2]);
                                        if (bufStart < paraStart) {
                                            chooseHtv[paint2] = staticLayout2.getLineTop(staticLayout2.getLineForOffset(bufStart));
                                        } else {
                                            chooseHtv[paint2] = rightLen;
                                        }
                                    }
                                }
                                iArr = chooseHtv;
                                chooseHt = chooseHt2;
                                firstWidthLineCount2 = firstWidthLineCount;
                                firstWidth = firstWidth2;
                                bufStart = restWidth;
                            } catch (Throwable th5) {
                                th = th5;
                                iArr = chooseHtv;
                                i3 = rightLen;
                                textDir = paint2;
                                i5 = source2;
                                rightLen = fm2;
                                staticLayout = staticLayout2;
                                nativePtr2 = nativePtr;
                                nFinish(nativePtr2);
                                throw th;
                            }
                        }
                        textDir2 = textDir4;
                        paint3 = paint2;
                        bufStart2 = bufStart;
                        source3 = source2;
                        iArr = chooseHtv;
                        firstWidthLineCount2 = 1;
                        firstWidth = firstWidth2;
                        bufStart = restWidth;
                        chooseHt = null;
                        chooseHtv = null;
                        if (spanned != null) {
                            try {
                                TabStopSpan[] source4 = (TabStopSpan[]) Layout.getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
                                if (source4.length > 0) {
                                    int[] stops = new int[source4.length];
                                    for (firstWidthLineCount = 0; firstWidthLineCount < source4.length; firstWidthLineCount++) {
                                        stops[firstWidthLineCount] = source4[firstWidthLineCount].getTabStop();
                                    }
                                    Arrays.sort(stops, 0, stops.length);
                                    chooseHtv = stops;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                paraStart = bufEnd;
                                rightLen = fm2;
                                staticLayout = staticLayout2;
                                nativePtr2 = nativePtr;
                                paraEnd = textDir2;
                                paragraphInfo3 = bufStart2;
                                nFinish(nativePtr2);
                                throw th;
                            }
                        }
                        int[] variableTabStops = chooseHtv;
                        try {
                            measuredPara = paragraphInfo3[leftLen].measured;
                            chs = measuredPara.getChars();
                            spanEndCache = measuredPara.getSpanEndCache().getRawArray();
                            fmCache = measuredPara.getFontMetrics().getRawArray();
                            widths3 = widths;
                            try {
                                widths3.resize(chs.length);
                                i3 = rightLen;
                                try {
                                    paragraphInfo4 = paragraphInfo3;
                                    spanned4 = spanned;
                                    spanned = lineBreaks;
                                } catch (Throwable th7) {
                                    th = th7;
                                    paragraphInfoArr = paragraphInfo3;
                                    floatArray = widths3;
                                    paraStart = bufEnd;
                                    spanned2 = spanned;
                                    rightLen = fm2;
                                    staticLayout = staticLayout2;
                                    nativePtr2 = nativePtr;
                                    f = ellipsizedWidth;
                                    lineBreaks2 = lineBreaks;
                                    truncateAt = ellipsize;
                                    textDirectionHeuristic = textDir2;
                                    textDir = paint3;
                                    paragraphInfo3 = bufStart2;
                                    charSequence = source3;
                                    nFinish(nativePtr2);
                                    throw th;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                i3 = rightLen;
                                paragraphInfoArr = paragraphInfo3;
                                floatArray = widths3;
                                paraStart = bufEnd;
                                spanned2 = spanned;
                                rightLen = fm2;
                                staticLayout = staticLayout2;
                                nativePtr2 = nativePtr;
                                f = ellipsizedWidth;
                                lineBreaks2 = lineBreaks;
                                truncateAt = ellipsize;
                                textDirectionHeuristic = textDir2;
                                textDir = paint3;
                                paragraphInfo3 = bufStart2;
                                charSequence = source3;
                                nFinish(nativePtr2);
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            i3 = rightLen;
                            paragraphInfoArr = paragraphInfo3;
                            paraStart = bufEnd;
                            spanned2 = spanned;
                            rightLen = fm2;
                            staticLayout = staticLayout2;
                            nativePtr2 = nativePtr;
                            f = ellipsizedWidth;
                            floatArray = widths;
                            lineBreaks2 = lineBreaks;
                            truncateAt = ellipsize;
                            textDirectionHeuristic = textDir2;
                            textDir = paint3;
                            paragraphInfo3 = bufStart2;
                            charSequence = source3;
                            nFinish(nativePtr2);
                            throw th;
                        }
                        Spanned spanned5;
                        try {
                            MeasuredParagraph measuredPara2 = measuredPara;
                            int restWidth2 = bufStart;
                            int firstWidth3 = firstWidth;
                            int paraIndex = leftLen;
                            int bufEnd2 = bufEnd;
                            int i6;
                            try {
                                boolean z;
                                boolean ellipsisMayBeApplied;
                                float width;
                                int fmAscent;
                                int here;
                                int breakCount;
                                int paraStart2;
                                char[] chs2;
                                MeasuredParagraph measuredPara3;
                                int restWidth3;
                                int firstWidth4;
                                leftLen = nComputeLineBreaks(nativePtr, chs, measuredPara.getNativePtr(), paraEnd - paraStart, (float) firstWidth, firstWidthLineCount2, (float) bufStart, variableTabStops, 20, staticLayout2.mLineCount, spanned, spanned.breaks.length, spanned.breaks, spanned.widths, spanned.ascents, spanned.descents, spanned.flags, widths3.getRawArray());
                                int[] breaks = spanned.breaks;
                                float[] lineWidths = spanned.widths;
                                float[] ascents = spanned.ascents;
                                float[] descents = spanned.descents;
                                int[] flags = spanned.flags;
                                firstWidth = staticLayout2.mMaximumVisibleLineCount - staticLayout2.mLineCount;
                                TruncateAt ellipsize3 = ellipsize;
                                if (ellipsize3 != null) {
                                    try {
                                        if (ellipsize3 == TruncateAt.END || (staticLayout2.mMaximumVisibleLineCount == 1 && ellipsize3 != TruncateAt.MARQUEE)) {
                                            z = true;
                                            ellipsisMayBeApplied = z;
                                            if (firstWidth > 0 && firstWidth < leftLen && ellipsisMayBeApplied) {
                                                width = 0.0f;
                                                rightLen = 0;
                                                i6 = firstWidth - 1;
                                                while (i6 < leftLen) {
                                                    if (i6 == leftLen - 1) {
                                                        width += lineWidths[i6];
                                                    } else {
                                                        i5 = i6 == 0 ? 0 : breaks[i6 - 1];
                                                        while (i5 < breaks[i6]) {
                                                            width += widths3.get(i5);
                                                            i5++;
                                                        }
                                                    }
                                                    rightLen |= flags[i6] & 536870912;
                                                    i6++;
                                                }
                                                breaks[firstWidth - 1] = breaks[leftLen - 1];
                                                lineWidths[firstWidth - 1] = width;
                                                flags[firstWidth - 1] = rightLen;
                                                leftLen = firstWidth;
                                            }
                                            i2 = paraStart;
                                            firstWidth2 = 0;
                                            i = 0;
                                            restWidth = 0;
                                            bufEnd = 0;
                                            fmAscent = 0;
                                            i5 = 0;
                                            i6 = 0;
                                            rightLen = i2;
                                            while (i2 < paraEnd) {
                                                int spanEndCacheIndex = firstWidth2 + 1;
                                                int spanEnd = spanEndCache[firstWidth2];
                                                boolean z2 = false;
                                                here = rightLen;
                                                fm2.top = fmCache[(restWidth * 4) + 0];
                                                boolean z3 = true;
                                                fm2.bottom = fmCache[(restWidth * 4) + 1];
                                                Object obj = 2;
                                                fm2.ascent = fmCache[(restWidth * 4) + 2];
                                                fm2.descent = fmCache[(restWidth * 4) + 3];
                                                int fmCacheIndex = restWidth + 1;
                                                if (fm2.top < i6) {
                                                    i6 = fm2.top;
                                                }
                                                if (fm2.ascent < fmAscent) {
                                                    fmAscent = fm2.ascent;
                                                }
                                                try {
                                                    int fmTop;
                                                    int i7;
                                                    int fmDescent;
                                                    int paraEnd2;
                                                    int fmAscent2;
                                                    int breakIndex;
                                                    boolean z4;
                                                    boolean z5;
                                                    Object obj2;
                                                    int bufEnd3;
                                                    if (fm2.descent > bufEnd) {
                                                        bufEnd = fm2.descent;
                                                    }
                                                    if (fm2.bottom > i5) {
                                                        i5 = fm2.bottom;
                                                    }
                                                    rightLen = i;
                                                    while (rightLen < leftLen) {
                                                        fmTop = i6;
                                                        if (paraStart + breaks[rightLen] >= i2) {
                                                            break;
                                                        }
                                                        rightLen++;
                                                        i6 = fmTop;
                                                    }
                                                    fmTop = i6;
                                                    int fmBottom = i5;
                                                    i6 = bufEnd;
                                                    bufEnd = rightLen;
                                                    while (bufEnd < leftLen) {
                                                        i5 = spanEnd;
                                                        if (breaks[bufEnd] + paraStart > i5) {
                                                            breakCount = leftLen;
                                                            i7 = i2;
                                                            paraStart2 = paraStart;
                                                            fmDescent = i6;
                                                            leftLen = i5;
                                                            paraEnd2 = paraEnd;
                                                            floatArray = widths3;
                                                            chs2 = chs;
                                                            fmAscent2 = fmAscent;
                                                            breakIndex = bufEnd;
                                                            truncateAt = ellipsize3;
                                                            lineBreaks2 = spanned;
                                                            rightLen = fm2;
                                                            widths3 = staticLayout2;
                                                            z4 = z3;
                                                            z5 = z2;
                                                            obj2 = obj;
                                                            f = ellipsizedWidth;
                                                            textDir3 = textDir2;
                                                            paint = paint3;
                                                            i4 = bufStart2;
                                                            source = source3;
                                                            spanned2 = spanned4;
                                                            paragraphInfoArr = paragraphInfo4;
                                                            measuredPara3 = measuredPara2;
                                                            restWidth3 = restWidth2;
                                                            firstWidth4 = firstWidth3;
                                                            bufEnd3 = bufEnd2;
                                                            break;
                                                        }
                                                        rightLen = breaks[bufEnd] + paraStart;
                                                        TruncateAt ellipsize4 = ellipsize3;
                                                        bufStart = bufEnd2;
                                                        boolean moreChars = rightLen < bufStart ? z3 : z2;
                                                        if (fallbackLineSpacing) {
                                                            breakCount = leftLen;
                                                            try {
                                                                leftLen = Math.min(fmAscent, Math.round(ascents[bufEnd]));
                                                            } catch (Throwable th10) {
                                                                th = th10;
                                                                floatArray = widths3;
                                                                paraStart = bufStart;
                                                                spanned5 = spanned;
                                                                rightLen = fm2;
                                                                staticLayout = staticLayout2;
                                                                nativePtr2 = nativePtr;
                                                                f = ellipsizedWidth;
                                                                textDirectionHeuristic = textDir2;
                                                                textDir = paint3;
                                                                i6 = bufStart2;
                                                                charSequence = source3;
                                                                spanned2 = spanned4;
                                                                paragraphInfoArr = paragraphInfo4;
                                                                truncateAt = ellipsize4;
                                                                nFinish(nativePtr2);
                                                                throw th;
                                                            }
                                                        }
                                                        breakCount = leftLen;
                                                        leftLen = fmAscent;
                                                        int spanEnd2 = i5;
                                                        i5 = leftLen;
                                                        if (fallbackLineSpacing) {
                                                            leftLen = Math.max(i6, Math.round(descents[bufEnd]));
                                                        } else {
                                                            leftLen = i6;
                                                        }
                                                        paraEnd2 = paraEnd;
                                                        try {
                                                            i7 = i2;
                                                            int endPos = rightLen;
                                                            paraStart2 = paraStart;
                                                            paragraphInfoArr = paragraphInfo4;
                                                            floatArray = widths3;
                                                            z4 = z3;
                                                            z5 = z2;
                                                            f = ellipsizedWidth;
                                                            chs2 = chs;
                                                            textDir3 = textDir2;
                                                            paint = paint3;
                                                            measuredPara3 = measuredPara2;
                                                            breakIndex = bufEnd;
                                                            bufEnd3 = bufStart;
                                                            i4 = bufStart2;
                                                            restWidth3 = restWidth2;
                                                            truncateAt = ellipsize4;
                                                            int remainingLineCount = firstWidth;
                                                            source = source3;
                                                            firstWidth4 = firstWidth3;
                                                            spanned5 = spanned;
                                                            obj2 = obj;
                                                            spanned2 = spanned4;
                                                            FontMetricsInt fm4 = fm2;
                                                            try {
                                                                i3 = staticLayout2.out(source3, here, endPos, i5, leftLen, fmTop, fmBottom, i3, spacingmult, spacingadd, chooseHt, iArr, fm2, flags[bufEnd], needMultiply, measuredPara3, bufEnd3, includepad, trackpad, addLastLineSpacing, chs2, widths3.getRawArray(), paraStart2, truncateAt, f, lineWidths[bufEnd], paint, moreChars);
                                                                leftLen = spanEnd2;
                                                                paraStart = endPos;
                                                                if (paraStart < leftLen) {
                                                                    rightLen = fm4;
                                                                    try {
                                                                        fmTop = rightLen.top;
                                                                        fmBottom = rightLen.bottom;
                                                                        fmAscent = rightLen.ascent;
                                                                        i6 = rightLen.descent;
                                                                    } catch (Throwable th11) {
                                                                        th = th11;
                                                                        nativePtr2 = nativePtr;
                                                                        textDirectionHeuristic = textDir3;
                                                                        textDir = paint;
                                                                        i6 = i4;
                                                                        paraStart = bufEnd3;
                                                                        charSequence = source;
                                                                        nFinish(nativePtr2);
                                                                        throw th;
                                                                    }
                                                                }
                                                                rightLen = fm4;
                                                                fmAscent = z5;
                                                                fmBottom = z5;
                                                                fmTop = z5;
                                                                i6 = z5;
                                                                here = paraStart;
                                                                bufEnd = breakIndex + 1;
                                                                try {
                                                                    if (this.mLineCount < this.mMaximumVisibleLineCount || !this.mEllipsized) {
                                                                        spanEnd = leftLen;
                                                                        fm2 = rightLen;
                                                                        staticLayout2 = this;
                                                                        nativePtr = nativePtr;
                                                                        i2 = i7;
                                                                        paraStart = paraStart2;
                                                                        paragraphInfo4 = paragraphInfoArr;
                                                                        spanned = spanned5;
                                                                        widths3 = floatArray;
                                                                        z3 = z4;
                                                                        z2 = z5;
                                                                        chs = chs2;
                                                                        measuredPara2 = measuredPara3;
                                                                        ellipsize3 = truncateAt;
                                                                        restWidth2 = restWidth3;
                                                                        firstWidth3 = firstWidth4;
                                                                        firstWidth = remainingLineCount;
                                                                        spanned4 = spanned2;
                                                                        obj = obj2;
                                                                        leftLen = breakCount;
                                                                        paraEnd = paraEnd2;
                                                                        textDir2 = textDir3;
                                                                        paint3 = paint;
                                                                        bufStart2 = i4;
                                                                        bufEnd2 = bufEnd3;
                                                                        source3 = source;
                                                                        ellipsizedWidth = f;
                                                                    } else {
                                                                        nFinish(nativePtr);
                                                                        return;
                                                                    }
                                                                } catch (Throwable th12) {
                                                                    th = th12;
                                                                    nativePtr2 = nativePtr;
                                                                    textDirectionHeuristic = textDir3;
                                                                    textDir = paint;
                                                                    i6 = i4;
                                                                    paraStart = bufEnd3;
                                                                    charSequence = source;
                                                                    nFinish(nativePtr2);
                                                                    throw th;
                                                                }
                                                            } catch (Throwable th13) {
                                                                th = th13;
                                                                nativePtr2 = nativePtr;
                                                                rightLen = fm4;
                                                                textDirectionHeuristic = textDir3;
                                                                textDir = paint;
                                                                i6 = i4;
                                                                paraStart = bufEnd3;
                                                                charSequence = source;
                                                                nFinish(nativePtr2);
                                                                throw th;
                                                            }
                                                        } catch (Throwable th14) {
                                                            th = th14;
                                                            floatArray = widths3;
                                                            bufEnd3 = bufStart;
                                                            spanned5 = spanned;
                                                            rightLen = fm2;
                                                            staticLayout = staticLayout2;
                                                            nativePtr2 = nativePtr;
                                                            f = ellipsizedWidth;
                                                            spanned2 = spanned4;
                                                            paragraphInfoArr = paragraphInfo4;
                                                            truncateAt = ellipsize4;
                                                            paraEnd = textDir2;
                                                            chs = paint3;
                                                            i6 = bufStart2;
                                                            i5 = source3;
                                                            paraStart = bufEnd3;
                                                            nFinish(nativePtr2);
                                                            throw th;
                                                        }
                                                    }
                                                    breakCount = leftLen;
                                                    i7 = i2;
                                                    paraStart2 = paraStart;
                                                    fmDescent = i6;
                                                    paraEnd2 = paraEnd;
                                                    floatArray = widths3;
                                                    chs2 = chs;
                                                    fmAscent2 = fmAscent;
                                                    breakIndex = bufEnd;
                                                    truncateAt = ellipsize3;
                                                    lineBreaks2 = spanned;
                                                    rightLen = fm2;
                                                    widths3 = staticLayout2;
                                                    z4 = z3;
                                                    z5 = z2;
                                                    obj2 = obj;
                                                    f = ellipsizedWidth;
                                                    textDir3 = textDir2;
                                                    paint = paint3;
                                                    i4 = bufStart2;
                                                    source = source3;
                                                    spanned2 = spanned4;
                                                    paragraphInfoArr = paragraphInfo4;
                                                    measuredPara3 = measuredPara2;
                                                    restWidth3 = restWidth2;
                                                    firstWidth4 = firstWidth3;
                                                    bufEnd3 = bufEnd2;
                                                    leftLen = spanEnd;
                                                    i2 = leftLen;
                                                    fm2 = rightLen;
                                                    Object obj3 = widths3;
                                                    nativePtr = nativePtr;
                                                    firstWidth2 = spanEndCacheIndex;
                                                    restWidth = fmCacheIndex;
                                                    i5 = fmBottom;
                                                    paraStart = paraStart2;
                                                    paragraphInfo4 = paragraphInfoArr;
                                                    bufEnd = fmDescent;
                                                    Object spanned6 = lineBreaks2;
                                                    widths3 = floatArray;
                                                    chs = chs2;
                                                    measuredPara2 = measuredPara3;
                                                    fmAscent = fmAscent2;
                                                    i = breakIndex;
                                                    ellipsize3 = truncateAt;
                                                    restWidth2 = restWidth3;
                                                    firstWidth3 = firstWidth4;
                                                    firstWidth = firstWidth;
                                                    spanned4 = spanned2;
                                                    rightLen = here;
                                                    i6 = fmTop;
                                                    leftLen = breakCount;
                                                    paraEnd = paraEnd2;
                                                    textDir2 = textDir3;
                                                    paint3 = paint;
                                                    bufStart2 = i4;
                                                    bufEnd2 = bufEnd3;
                                                    source3 = source;
                                                    ellipsizedWidth = f;
                                                } catch (Throwable th15) {
                                                    th = th15;
                                                    floatArray = widths3;
                                                    truncateAt = ellipsize3;
                                                    spanned5 = spanned;
                                                    rightLen = fm2;
                                                    staticLayout = staticLayout2;
                                                    nativePtr2 = nativePtr;
                                                    f = ellipsizedWidth;
                                                    spanned2 = spanned4;
                                                    paragraphInfoArr = paragraphInfo4;
                                                    textDirectionHeuristic = textDir2;
                                                    textDir = paint3;
                                                    i6 = bufStart2;
                                                    i5 = source3;
                                                    paraStart = bufEnd2;
                                                    nFinish(nativePtr2);
                                                    throw th;
                                                }
                                            }
                                            breakCount = leftLen;
                                            here = rightLen;
                                            paraStart2 = paraStart;
                                            floatArray = widths3;
                                            chs2 = chs;
                                            truncateAt = ellipsize3;
                                            lineBreaks2 = spanned;
                                            rightLen = fm2;
                                            staticLayout = staticLayout2;
                                            f = ellipsizedWidth;
                                            textDir3 = textDir2;
                                            paint = paint3;
                                            i4 = bufStart2;
                                            source = source3;
                                            spanned2 = spanned4;
                                            paragraphInfoArr = paragraphInfo4;
                                            measuredPara3 = measuredPara2;
                                            restWidth3 = restWidth2;
                                            firstWidth4 = firstWidth3;
                                            nativePtr2 = nativePtr;
                                            paraStart = bufEnd2;
                                            if (paraEnd != paraStart) {
                                                chooseHtv = iArr;
                                                break;
                                            }
                                            leftLen = paraIndex + 1;
                                            fm2 = rightLen;
                                            bufEnd = paraStart;
                                            staticLayout2 = staticLayout;
                                            nativePtr = nativePtr2;
                                            chooseHtv = iArr;
                                            paragraphInfo3 = paragraphInfoArr;
                                            lineBreaks = lineBreaks2;
                                            ellipsizedWidth = f;
                                            widths = floatArray;
                                            Object obj4 = null;
                                            ellipsize = truncateAt;
                                            spanned = spanned2;
                                            rightLen = i3;
                                            textDir4 = textDir3;
                                            paint2 = paint;
                                            bufStart = i4;
                                            source2 = source;
                                        }
                                    } catch (Throwable th16) {
                                        th = th16;
                                        floatArray = widths3;
                                        truncateAt = ellipsize3;
                                        spanned5 = spanned;
                                        rightLen = fm2;
                                        staticLayout = staticLayout2;
                                        nativePtr2 = nativePtr;
                                        f = ellipsizedWidth;
                                        textDirectionHeuristic = textDir2;
                                        textDir = paint3;
                                        i6 = bufStart2;
                                        charSequence = source3;
                                        spanned2 = spanned4;
                                        paragraphInfoArr = paragraphInfo4;
                                        paraStart = bufEnd2;
                                        nFinish(nativePtr2);
                                        throw th;
                                    }
                                }
                                z = false;
                                ellipsisMayBeApplied = z;
                                width = 0.0f;
                                rightLen = 0;
                                i6 = firstWidth - 1;
                                while (i6 < leftLen) {
                                }
                                breaks[firstWidth - 1] = breaks[leftLen - 1];
                                lineWidths[firstWidth - 1] = width;
                                flags[firstWidth - 1] = rightLen;
                                leftLen = firstWidth;
                                i2 = paraStart;
                                firstWidth2 = 0;
                                i = 0;
                                restWidth = 0;
                                bufEnd = 0;
                                fmAscent = 0;
                                i5 = 0;
                                i6 = 0;
                                rightLen = i2;
                                while (i2 < paraEnd) {
                                }
                                breakCount = leftLen;
                                here = rightLen;
                                paraStart2 = paraStart;
                                floatArray = widths3;
                                chs2 = chs;
                                truncateAt = ellipsize3;
                                lineBreaks2 = spanned;
                                rightLen = fm2;
                                staticLayout = staticLayout2;
                                f = ellipsizedWidth;
                                textDir3 = textDir2;
                                paint = paint3;
                                i4 = bufStart2;
                                source = source3;
                                spanned2 = spanned4;
                                paragraphInfoArr = paragraphInfo4;
                                measuredPara3 = measuredPara2;
                                restWidth3 = restWidth2;
                                firstWidth4 = firstWidth3;
                                nativePtr2 = nativePtr;
                                paraStart = bufEnd2;
                                if (paraEnd != paraStart) {
                                }
                            } catch (Throwable th17) {
                                th = th17;
                                floatArray = widths3;
                                spanned5 = spanned;
                                rightLen = fm2;
                                staticLayout = staticLayout2;
                                nativePtr2 = nativePtr;
                                f = ellipsizedWidth;
                                truncateAt = ellipsize;
                                spanned2 = spanned4;
                                paragraphInfoArr = paragraphInfo4;
                                paraStart = bufEnd2;
                                textDirectionHeuristic = textDir2;
                                textDir = paint3;
                                i6 = bufStart2;
                                charSequence = source3;
                                nFinish(nativePtr2);
                                throw th;
                            }
                        } catch (Throwable th18) {
                            th = th18;
                            floatArray = widths3;
                            paraStart = bufEnd;
                            spanned5 = spanned;
                            rightLen = fm2;
                            staticLayout = staticLayout2;
                            nativePtr2 = nativePtr;
                            f = ellipsizedWidth;
                            truncateAt = ellipsize;
                            spanned2 = spanned4;
                            paragraphInfoArr = paragraphInfo4;
                            textDirectionHeuristic = textDir2;
                            textDir = paint3;
                            paragraphInfo3 = bufStart2;
                            charSequence = source3;
                            nFinish(nativePtr2);
                            throw th;
                        }
                    } catch (Throwable th19) {
                        th = th19;
                        i3 = rightLen;
                        paragraphInfoArr = paragraphInfo3;
                        textDirectionHeuristic = textDir4;
                        paraStart = bufEnd;
                        charSequence = source2;
                        spanned2 = spanned;
                        staticLayout = staticLayout2;
                        nativePtr2 = nativePtr;
                        f = ellipsizedWidth;
                        floatArray = widths;
                        lineBreaks2 = lineBreaks;
                        truncateAt = ellipsize;
                        iArr = chooseHtv;
                        nFinish(nativePtr2);
                        throw th;
                    }
                }
                i3 = rightLen;
                paragraphInfoArr = paragraphInfo3;
                textDir3 = textDir4;
                paint = paint2;
                paraStart = bufEnd;
                i4 = bufStart;
                source = source2;
                spanned2 = spanned;
                rightLen = fm2;
                staticLayout = staticLayout2;
                nativePtr2 = nativePtr;
                f = ellipsizedWidth;
                floatArray = widths;
                lineBreaks2 = lineBreaks;
                truncateAt = ellipsize;
                paragraphInfo3 = i4;
                if (paraStart == paragraphInfo3) {
                    charSequence = source;
                    try {
                        if (charSequence.charAt(paraStart - 1) != CHAR_NEW_LINE) {
                            textDirectionHeuristic = textDir3;
                            textDir = paint;
                            nFinish(nativePtr2);
                        }
                    } catch (Throwable th20) {
                        th = th20;
                        iArr = chooseHtv;
                        textDirectionHeuristic = textDir3;
                        textDir = paint;
                        nFinish(nativePtr2);
                        throw th;
                    }
                }
                charSequence = source;
                if (staticLayout.mLineCount >= staticLayout.mMaximumVisibleLineCount) {
                    MeasuredParagraph measuredPara4;
                    try {
                        measuredPara4 = MeasuredParagraph.buildForBidi(charSequence, paraStart, paraStart, textDir3, null);
                        textDir = paint;
                    } catch (Throwable th21) {
                        th = th21;
                        textDir = paint;
                        iArr = chooseHtv;
                        nFinish(nativePtr2);
                        throw th;
                    }
                    try {
                        textDir.getFontMetricsInt(rightLen);
                        i3 = staticLayout.out(charSequence, paraStart, paraStart, rightLen.ascent, rightLen.descent, rightLen.top, rightLen.bottom, i3, spacingmult, spacingadd, null, null, rightLen, 0, needMultiply, measuredPara4, paraStart, includepad, trackpad, addLastLineSpacing, null, null, paragraphInfo3, truncateAt, f, 0.0f, textDir, false);
                    } catch (Throwable th22) {
                        th = th22;
                        iArr = chooseHtv;
                        nFinish(nativePtr2);
                        throw th;
                    }
                }
                textDir = paint;
                nFinish(nativePtr2);
            }
        }
        nativePtr = nativePtr3;
        widths = widths2;
        lineBreaks = lineBreaks3;
        ellipsize = ellipsize2;
        ellipsizedWidth = ellipsizedWidth3;
        paragraphInfo = null;
        HashMap<String, Integer> hashMap = languageMap;
        fm2 = fm;
        ellipsizedWidth2 = false;
        spanned = spanned3;
        paragraphInfo2 = paragraphInfo;
        if (paragraphInfo2 == null) {
        }
        paragraphInfo3 = paragraphInfo2;
        leftLen = ellipsizedWidth2;
        rightLen = 0;
        chooseHtv = null;
        while (leftLen < paragraphInfo3.length) {
        }
        i3 = rightLen;
        paragraphInfoArr = paragraphInfo3;
        textDir3 = textDir4;
        paint = paint2;
        paraStart = bufEnd;
        i4 = bufStart;
        source = source2;
        spanned2 = spanned;
        rightLen = fm2;
        staticLayout = staticLayout2;
        nativePtr2 = nativePtr;
        f = ellipsizedWidth;
        floatArray = widths;
        lineBreaks2 = lineBreaks;
        truncateAt = ellipsize;
        paragraphInfo3 = i4;
        if (paraStart == paragraphInfo3) {
        }
        try {
            if (staticLayout.mLineCount >= staticLayout.mMaximumVisibleLineCount) {
            }
            nFinish(nativePtr2);
        } catch (Throwable th23) {
            th = th23;
            textDirectionHeuristic = textDir3;
            textDir = paint;
            iArr = chooseHtv;
            nFinish(nativePtr2);
            throw th;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:49:0x0147  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x012f  */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x01e6  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x01e3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int out(CharSequence text, int start, int end, int above, int below, int top, int bottom, int v, float spacingmult, float spacingadd, LineHeightSpan[] chooseHt, int[] chooseHtv, FontMetricsInt fm, int flags, boolean needMultiply, MeasuredParagraph measured, int bufEnd, boolean includePad, boolean trackPad, boolean addLastLineLineSpacing, char[] chs, float[] widths, int widthStart, TruncateAt ellipsize, float ellipsisWidth, float textWidth, TextPaint paint, boolean moreChars) {
        int i;
        TruncateAt truncateAt;
        int i2;
        int i3;
        boolean z;
        int above2;
        int below2;
        int top2;
        int bottom2;
        boolean z2;
        boolean z3;
        int i4;
        int i5;
        boolean lastLine;
        int i6;
        int i7 = start;
        int i8 = end;
        LineHeightSpan[] lineHeightSpanArr = chooseHt;
        FontMetricsInt fontMetricsInt = fm;
        int i9 = bufEnd;
        int i10 = widthStart;
        TruncateAt truncateAt2 = ellipsize;
        int j = this.mLineCount;
        int off = j * this.mColumns;
        Object obj = 1;
        int want = (off + this.mColumns) + 1;
        int[] lines = this.mLines;
        int dir = measured.getParagraphDir();
        if (want >= lines.length) {
            int[] grow = ArrayUtils.newUnpaddedIntArray(GrowingArrayUtils.growSize(want));
            System.arraycopy(lines, 0, grow, 0, lines.length);
            this.mLines = grow;
            lines = grow;
        }
        int[] lines2 = lines;
        if (j >= this.mLineDirections.length) {
            Directions[] grow2 = (Directions[]) ArrayUtils.newUnpaddedArray(Directions.class, GrowingArrayUtils.growSize(j));
            System.arraycopy(this.mLineDirections, 0, grow2, 0, this.mLineDirections.length);
            this.mLineDirections = grow2;
        }
        int want2;
        if (lineHeightSpanArr != null) {
            fontMetricsInt.ascent = above;
            fontMetricsInt.descent = below;
            fontMetricsInt.top = top;
            fontMetricsInt.bottom = bottom;
            i = 0;
            while (true) {
                i9 = i;
                if (i9 >= lineHeightSpanArr.length) {
                    break;
                }
                Object obj2;
                if (lineHeightSpanArr[i9] instanceof WithDensity) {
                    obj2 = null;
                    want2 = want;
                    Object obj3 = obj;
                    i = j;
                    truncateAt = truncateAt2;
                    ((WithDensity) lineHeightSpanArr[i9]).chooseHeight(text, i7, i8, chooseHtv[i9], v, fontMetricsInt, paint);
                } else {
                    want2 = want;
                    i = j;
                    truncateAt = truncateAt2;
                    obj2 = null;
                    lineHeightSpanArr[i9].chooseHeight(text, i7, i8, chooseHtv[i9], v, fontMetricsInt);
                }
                i2 = i9 + 1;
                int i11 = above;
                i3 = below;
                truncateAt2 = truncateAt;
                j = i;
                Object obj4 = obj2;
                want = want2;
                obj = 1;
                i9 = bottom;
                i10 = widthStart;
                i = i2;
                i2 = top;
            }
            i = j;
            truncateAt = truncateAt2;
            z = false;
            above2 = fontMetricsInt.ascent;
            below2 = fontMetricsInt.descent;
            top2 = fontMetricsInt.top;
            bottom2 = fontMetricsInt.bottom;
        } else {
            z = false;
            want2 = want;
            i = j;
            truncateAt = truncateAt2;
            above2 = above;
            below2 = below;
            top2 = top;
            bottom2 = bottom;
        }
        boolean firstLine = i == 0 ? true : z;
        boolean currentLineIsTheLastVisibleOne = i + 1 == this.mMaximumVisibleLineCount ? true : z;
        if (truncateAt != null) {
            boolean z4;
            boolean z5;
            boolean forceEllipsis;
            if (moreChars) {
                z4 = true;
                if (this.mLineCount + 1 == this.mMaximumVisibleLineCount) {
                    z2 = true;
                    z5 = z4;
                    truncateAt2 = truncateAt;
                    i9 = widthStart;
                    forceEllipsis = z2;
                    z2 = ((((this.mMaximumVisibleLineCount == z5 || !moreChars) && (!firstLine || moreChars)) || truncateAt2 == TruncateAt.MARQUEE) && (firstLine || ((!currentLineIsTheLastVisibleOne && moreChars) || truncateAt2 != TruncateAt.END))) ? z : z5;
                    if (z2) {
                        z3 = z5;
                        i4 = i9;
                        i5 = bufEnd;
                    } else {
                        z3 = z5;
                        i4 = i9;
                        i5 = bufEnd;
                        calculateEllipsis(i7, i8, widths, i9, ellipsisWidth, truncateAt2, i, textWidth, paint, forceEllipsis);
                    }
                }
            } else {
                z4 = true;
            }
            z2 = z;
            z5 = z4;
            truncateAt2 = truncateAt;
            i9 = widthStart;
            forceEllipsis = z2;
            if (this.mMaximumVisibleLineCount == z5) {
            }
            if (z2) {
            }
        } else {
            i4 = widthStart;
            i5 = bufEnd;
            z3 = true;
        }
        CharSequence charSequence;
        if (this.mEllipsized) {
            lastLine = true;
            charSequence = text;
        } else {
            if (i4 == i5 || i5 <= 0) {
                charSequence = text;
            } else {
                if (text.charAt(i5 - 1) == CHAR_NEW_LINE) {
                    z2 = z3;
                    if (i8 != i5 && !lastCharIsNewLine) {
                        lastLine = true;
                    } else if (i7 == i5 || !lastCharIsNewLine) {
                        lastLine = z;
                    } else {
                        lastLine = true;
                    }
                }
            }
            z2 = z;
            if (i8 != i5) {
            }
            if (i7 == i5) {
            }
            lastLine = z;
        }
        z2 = lastLine;
        if (firstLine) {
            if (trackPad) {
                this.mTopPadding = top2 - above2;
            }
            if (includePad) {
                above2 = top2;
            }
        }
        if (z2) {
            if (trackPad) {
                this.mBottomPadding = bottom2 - below2;
            }
            if (includePad) {
                below2 = bottom2;
            }
        }
        boolean z6;
        if (!needMultiply) {
        } else if (addLastLineLineSpacing || !z2) {
            double ex = (double) ((((float) (below2 - above2)) * (spacingmult - 1.0f)) + spacingadd);
            if (ex >= 0.0d) {
                want = (int) (EXTRA_ROUNDING + ex);
                z6 = z2;
            } else {
                want = -((int) ((-ex) + true));
            }
            i2 = want;
            lines2[off + 0] = i7;
            i6 = i4;
            lines2[off + 1] = v;
            lines2[off + 2] = below2 + i2;
            lines2[off + 3] = i2;
            if (!this.mEllipsized && currentLineIsTheLastVisibleOne) {
                this.mMaxLineHeight = v + ((includePad ? bottom2 : below2) - above2);
            }
            i3 = v + ((below2 - above2) + i2);
            lines2[(off + this.mColumns) + 0] = i8;
            lines2[(off + this.mColumns) + 1] = i3;
            want = off + 0;
            lines2[want] = lines2[want] | (flags & 536870912);
            lines2[off + 4] = flags;
            want = off + 0;
            lines2[want] = lines2[want] | (dir << 30);
            this.mLineDirections[i] = measured.getDirections(i7 - i6, i8 - i6);
            this.mLineCount++;
            return i3;
        } else {
            z6 = z2;
        }
        i2 = z;
        lines2[off + 0] = i7;
        i6 = i4;
        lines2[off + 1] = v;
        lines2[off + 2] = below2 + i2;
        lines2[off + 3] = i2;
        if (includePad) {
        }
        this.mMaxLineHeight = v + ((includePad ? bottom2 : below2) - above2);
        i3 = v + ((below2 - above2) + i2);
        lines2[(off + this.mColumns) + 0] = i8;
        lines2[(off + this.mColumns) + 1] = i3;
        want = off + 0;
        lines2[want] = lines2[want] | (flags & 536870912);
        lines2[off + 4] = flags;
        want = off + 0;
        lines2[want] = lines2[want] | (dir << 30);
        this.mLineDirections[i] = measured.getDirections(i7 - i6, i8 - i6);
        this.mLineCount++;
        return i3;
    }

    /* JADX WARNING: Removed duplicated region for block: B:43:0x00c9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void calculateEllipsis(int lineStart, int lineEnd, float[] widths, int widthStart, float avail, TruncateAt where, int line, float textWidth, TextPaint paint, boolean forceEllipsis) {
        TruncateAt truncateAt = where;
        int i = line;
        float avail2 = avail - getTotalInsets(i);
        if (textWidth > avail2 || forceEllipsis) {
            float ellipsisWidth = paint.measureText(TextUtils.getEllipsisString(where));
            int ellipsisStart = 0;
            int ellipsisCount = 0;
            int len = lineEnd - lineStart;
            float sum;
            int i2;
            float w;
            int i3;
            float w2;
            if (truncateAt == TruncateAt.START) {
                if (this.mMaximumVisibleLineCount == 1) {
                    sum = 0.0f;
                    i2 = len;
                    while (i2 > 0) {
                        w = widths[((i2 - 1) + lineStart) - widthStart];
                        if ((w + sum) + ellipsisWidth > avail2) {
                            while (i2 < len && widths[(i2 + lineStart) - widthStart] == 0.0f) {
                                i2++;
                            }
                            ellipsisStart = 0;
                            ellipsisCount = i2;
                        } else {
                            sum += w;
                            i2--;
                        }
                    }
                    ellipsisStart = 0;
                    ellipsisCount = i2;
                } else if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Start Ellipsis only supported with one line");
                }
            } else if (truncateAt == TruncateAt.END || truncateAt == TruncateAt.MARQUEE || truncateAt == TruncateAt.END_SMALL) {
                float sum2 = 0.0f;
                int i4 = 0;
                while (true) {
                    i3 = i4;
                    if (i3 >= len) {
                        break;
                    }
                    w2 = widths[(i3 + lineStart) - widthStart];
                    if ((w2 + sum2) + ellipsisWidth > avail2) {
                        break;
                    }
                    sum2 += w2;
                    i4 = i3 + 1;
                }
                i2 = i3;
                ellipsisStart = len - i3;
                if (forceEllipsis && ellipsisStart == 0 && len > 0) {
                    i2 = len - 1;
                    ellipsisCount = 1;
                } else {
                    ellipsisCount = ellipsisStart;
                }
                ellipsisStart = i2;
            } else if (this.mMaximumVisibleLineCount == 1) {
                int right;
                float lavail;
                w2 = 0.0f;
                int right2 = len;
                float ravail = (avail2 - ellipsisWidth) / 2.0f;
                while (right2 > 0) {
                    float w3 = widths[((right2 - 1) + lineStart) - widthStart];
                    if (w3 + w2 > ravail) {
                        right = right2;
                        while (right < len && widths[(right + lineStart) - widthStart] == 0.0f) {
                            right++;
                        }
                        lavail = (avail2 - ellipsisWidth) - w2;
                        sum = 0.0f;
                        i3 = 0;
                        while (i3 < right) {
                            w = widths[(i3 + lineStart) - widthStart];
                            if (w + sum > lavail) {
                                break;
                            }
                            sum += w;
                            i3++;
                        }
                        ellipsisStart = i3;
                        ellipsisCount = right - i3;
                    } else {
                        w2 += w3;
                        right2--;
                        truncateAt = where;
                    }
                }
                right = right2;
                lavail = (avail2 - ellipsisWidth) - w2;
                sum = 0.0f;
                i3 = 0;
                while (i3 < right) {
                }
                ellipsisStart = i3;
                ellipsisCount = right - i3;
            } else if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Middle Ellipsis only supported with one line");
            }
            this.mEllipsized = true;
            this.mLines[(this.mColumns * i) + 5] = ellipsisStart;
            this.mLines[(this.mColumns * i) + 6] = ellipsisCount;
            return;
        }
        this.mLines[(this.mColumns * i) + 5] = 0;
        this.mLines[(this.mColumns * i) + 6] = 0;
    }

    private float getTotalInsets(int line) {
        int totalIndent = 0;
        if (this.mLeftIndents != null) {
            totalIndent = this.mLeftIndents[Math.min(line, this.mLeftIndents.length - 1)];
        }
        if (this.mRightIndents != null) {
            totalIndent += this.mRightIndents[Math.min(line, this.mRightIndents.length - 1)];
        }
        return (float) totalIndent;
    }

    public int getLineForVertical(int vertical) {
        int high = this.mLineCount;
        int low = -1;
        int[] lines = this.mLines;
        while (high - low > 1) {
            int guess = (high + low) >> 1;
            if (lines[(this.mColumns * guess) + 1] > vertical) {
                high = guess;
            } else {
                low = guess;
            }
        }
        if (low < 0) {
            return 0;
        }
        return low;
    }

    public int getLineCount() {
        return this.mLineCount;
    }

    public int getLineTop(int line) {
        return this.mLines[(this.mColumns * line) + 1];
    }

    public int getLineExtra(int line) {
        return this.mLines[(this.mColumns * line) + 3];
    }

    public int getLineDescent(int line) {
        return this.mLines[(this.mColumns * line) + 2];
    }

    public int getLineStart(int line) {
        return this.mLines[(this.mColumns * line) + 0] & START_MASK;
    }

    public int getParagraphDirection(int line) {
        return this.mLines[(this.mColumns * line) + 0] >> 30;
    }

    public boolean getLineContainsTab(int line) {
        return (this.mLines[(this.mColumns * line) + 0] & 536870912) != 0;
    }

    public final Directions getLineDirections(int line) {
        if (line <= getLineCount()) {
            return this.mLineDirections[line];
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    public int getBottomPadding() {
        return this.mBottomPadding;
    }

    public int getHyphen(int line) {
        return this.mLines[(this.mColumns * line) + 4] & 255;
    }

    public int getIndentAdjust(int line, Alignment align) {
        if (align == Alignment.ALIGN_LEFT) {
            if (this.mLeftIndents == null) {
                return 0;
            }
            return this.mLeftIndents[Math.min(line, this.mLeftIndents.length - 1)];
        } else if (align == Alignment.ALIGN_RIGHT) {
            if (this.mRightIndents == null) {
                return 0;
            }
            return -this.mRightIndents[Math.min(line, this.mRightIndents.length - 1)];
        } else if (align == Alignment.ALIGN_CENTER) {
            int left = 0;
            if (this.mLeftIndents != null) {
                left = this.mLeftIndents[Math.min(line, this.mLeftIndents.length - 1)];
            }
            int right = 0;
            if (this.mRightIndents != null) {
                right = this.mRightIndents[Math.min(line, this.mRightIndents.length - 1)];
            }
            return (left - right) >> 1;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unhandled alignment ");
            stringBuilder.append(align);
            throw new AssertionError(stringBuilder.toString());
        }
    }

    public int getEllipsisCount(int line) {
        if (this.mColumns < 7) {
            return 0;
        }
        return this.mLines[(this.mColumns * line) + 6];
    }

    public int getEllipsisStart(int line) {
        if (this.mColumns < 7) {
            return 0;
        }
        return this.mLines[(this.mColumns * line) + 5];
    }

    public int getEllipsizedWidth() {
        return this.mEllipsizedWidth;
    }

    public int getHeight(boolean cap) {
        if (cap && this.mLineCount >= this.mMaximumVisibleLineCount && this.mMaxLineHeight == -1 && Log.isLoggable(TAG, 5)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("maxLineHeight should not be -1.  maxLines:");
            stringBuilder.append(this.mMaximumVisibleLineCount);
            stringBuilder.append(" lineCount:");
            stringBuilder.append(this.mLineCount);
            Log.w(str, stringBuilder.toString());
        }
        return (!cap || this.mLineCount < this.mMaximumVisibleLineCount || this.mMaxLineHeight == -1) ? super.getHeight() : this.mMaxLineHeight;
    }
}
