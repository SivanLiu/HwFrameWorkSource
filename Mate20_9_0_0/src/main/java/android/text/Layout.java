package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextUtils.TruncateAt;
import android.text.method.MetaKeyKeyListener;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LeadingMarginSpan.LeadingMarginSpan2;
import android.text.style.LineBackgroundSpan;
import android.text.style.ParagraphStyle;
import android.text.style.ReplacementSpan;
import android.text.style.TabStopSpan;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public abstract class Layout {
    public static final int BREAK_STRATEGY_BALANCED = 2;
    public static final int BREAK_STRATEGY_HIGH_QUALITY = 1;
    public static final int BREAK_STRATEGY_SIMPLE = 0;
    public static final float DEFAULT_LINESPACING_ADDITION = 0.0f;
    public static final float DEFAULT_LINESPACING_MULTIPLIER = 1.0f;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public static final Directions DIRS_ALL_LEFT_TO_RIGHT = new Directions(new int[]{0, RUN_LENGTH_MASK});
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public static final Directions DIRS_ALL_RIGHT_TO_LEFT = new Directions(new int[]{0, 134217727});
    public static final int DIR_LEFT_TO_RIGHT = 1;
    static final int DIR_REQUEST_DEFAULT_LTR = 2;
    static final int DIR_REQUEST_DEFAULT_RTL = -2;
    static final int DIR_REQUEST_LTR = 1;
    static final int DIR_REQUEST_RTL = -1;
    public static final int DIR_RIGHT_TO_LEFT = -1;
    public static final int HYPHENATION_FREQUENCY_FULL = 2;
    public static final int HYPHENATION_FREQUENCY_NONE = 0;
    public static final int HYPHENATION_FREQUENCY_NORMAL = 1;
    public static final int JUSTIFICATION_MODE_INTER_WORD = 1;
    public static final int JUSTIFICATION_MODE_NONE = 0;
    private static final ParagraphStyle[] NO_PARA_SPANS = ((ParagraphStyle[]) ArrayUtils.emptyArray(ParagraphStyle.class));
    static final int RUN_LENGTH_MASK = 67108863;
    static final int RUN_LEVEL_MASK = 63;
    static final int RUN_LEVEL_SHIFT = 26;
    static final int RUN_RTL_FLAG = 67108864;
    private static final int TAB_INCREMENT = 20;
    public static final int TEXT_SELECTION_LAYOUT_LEFT_TO_RIGHT = 1;
    public static final int TEXT_SELECTION_LAYOUT_RIGHT_TO_LEFT = 0;
    private static final Rect sTempRect = new Rect();
    private Alignment mAlignment;
    private int mJustificationMode;
    private SpanSet<LineBackgroundSpan> mLineBackgroundSpans;
    private TextPaint mPaint;
    private float mSpacingAdd;
    private float mSpacingMult;
    private boolean mSpannedText;
    private CharSequence mText;
    private TextDirectionHeuristic mTextDir;
    private int mWidth;
    private TextPaint mWorkPaint;

    public enum Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface BreakStrategy {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

    public static class Directions {
        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public int[] mDirections;

        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public Directions(int[] dirs) {
            this.mDirections = dirs;
        }
    }

    private class HorizontalMeasurementProvider {
        private float[] mHorizontals;
        private final int mLine;
        private int mLineStartOffset;
        private final boolean mPrimary;

        HorizontalMeasurementProvider(int line, boolean primary) {
            this.mLine = line;
            this.mPrimary = primary;
            init();
        }

        private void init() {
            if (Layout.this.getLineDirections(this.mLine) != Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                this.mHorizontals = Layout.this.getLineHorizontals(this.mLine, false, this.mPrimary);
                this.mLineStartOffset = Layout.this.getLineStart(this.mLine);
            }
        }

        float get(int offset) {
            if (this.mHorizontals == null || offset < this.mLineStartOffset || offset >= this.mLineStartOffset + this.mHorizontals.length) {
                return Layout.this.getHorizontal(offset, this.mPrimary);
            }
            return this.mHorizontals[offset - this.mLineStartOffset];
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HyphenationFrequency {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface JustificationMode {
    }

    @FunctionalInterface
    public interface SelectionRectangleConsumer {
        void accept(float f, float f2, float f3, float f4, int i);
    }

    static class TabStops {
        private int mIncrement;
        private int mNumStops;
        private int[] mStops;

        TabStops(int increment, Object[] spans) {
            reset(increment, spans);
        }

        void reset(int increment, Object[] spans) {
            this.mIncrement = increment;
            int ns = 0;
            if (spans != null) {
                int[] stops = this.mStops;
                int[] stops2 = stops;
                int ns2 = 0;
                for (Object o : spans) {
                    if (o instanceof TabStopSpan) {
                        if (stops2 == null) {
                            stops2 = new int[10];
                        } else if (ns2 == stops2.length) {
                            int[] nstops = new int[(ns2 * 2)];
                            for (int i = 0; i < ns2; i++) {
                                nstops[i] = stops2[i];
                            }
                            stops2 = nstops;
                        }
                        int ns3 = ns2 + 1;
                        stops2[ns2] = ((TabStopSpan) o).getTabStop();
                        ns2 = ns3;
                    }
                }
                if (ns2 > 1) {
                    Arrays.sort(stops2, 0, ns2);
                }
                if (stops2 != this.mStops) {
                    this.mStops = stops2;
                }
                ns = ns2;
            }
            this.mNumStops = ns;
        }

        float nextTab(float h) {
            int ns = this.mNumStops;
            if (ns > 0) {
                int[] stops = this.mStops;
                for (int i = 0; i < ns; i++) {
                    int stop = stops[i];
                    if (((float) stop) > h) {
                        return (float) stop;
                    }
                }
            }
            return nextDefaultStop(h, this.mIncrement);
        }

        public static float nextDefaultStop(float h, int inc) {
            return (float) (((int) ((((float) inc) + h) / ((float) inc))) * inc);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TextSelectionLayout {
    }

    static class Ellipsizer implements CharSequence, GetChars {
        Layout mLayout;
        TruncateAt mMethod;
        CharSequence mText;
        int mWidth;

        public Ellipsizer(CharSequence s) {
            this.mText = s;
        }

        public char charAt(int off) {
            char[] buf = TextUtils.obtain(1);
            getChars(off, off + 1, buf, 0);
            char ret = buf[0];
            TextUtils.recycle(buf);
            return ret;
        }

        public void getChars(int start, int end, char[] dest, int destoff) {
            int line1 = this.mLayout.getLineForOffset(start);
            int line2 = this.mLayout.getLineForOffset(end);
            TextUtils.getChars(this.mText, start, end, dest, destoff);
            for (int i = line1; i <= line2; i++) {
                this.mLayout.ellipsize(start, end, i, dest, destoff, this.mMethod);
            }
        }

        public int length() {
            return this.mText.length();
        }

        public CharSequence subSequence(int start, int end) {
            char[] s = new char[(end - start)];
            getChars(start, end, s, 0);
            return new String(s);
        }

        public String toString() {
            char[] s = new char[length()];
            getChars(0, length(), s, 0);
            return new String(s);
        }
    }

    static class SpannedEllipsizer extends Ellipsizer implements Spanned {
        private Spanned mSpanned;

        public SpannedEllipsizer(CharSequence display) {
            super(display);
            this.mSpanned = (Spanned) display;
        }

        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return this.mSpanned.getSpans(start, end, type);
        }

        public int getSpanStart(Object tag) {
            return this.mSpanned.getSpanStart(tag);
        }

        public int getSpanEnd(Object tag) {
            return this.mSpanned.getSpanEnd(tag);
        }

        public int getSpanFlags(Object tag) {
            return this.mSpanned.getSpanFlags(tag);
        }

        public int nextSpanTransition(int start, int limit, Class type) {
            return this.mSpanned.nextSpanTransition(start, limit, type);
        }

        public CharSequence subSequence(int start, int end) {
            char[] s = new char[(end - start)];
            getChars(start, end, s, 0);
            SpannableString ss = new SpannableString(new String(s));
            TextUtils.copySpansFrom(this.mSpanned, start, end, Object.class, ss, 0);
            return ss;
        }
    }

    public abstract int getBottomPadding();

    public abstract int getEllipsisCount(int i);

    public abstract int getEllipsisStart(int i);

    public abstract boolean getLineContainsTab(int i);

    public abstract int getLineCount();

    public abstract int getLineDescent(int i);

    public abstract Directions getLineDirections(int i);

    public abstract int getLineStart(int i);

    public abstract int getLineTop(int i);

    public abstract int getParagraphDirection(int i);

    public abstract int getTopPadding();

    public static float getDesiredWidth(CharSequence source, TextPaint paint) {
        return getDesiredWidth(source, 0, source.length(), paint);
    }

    public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint) {
        return getDesiredWidth(source, start, end, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR);
    }

    public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint, TextDirectionHeuristic textDir) {
        return getDesiredWidthWithLimit(source, start, end, paint, textDir, Float.MAX_VALUE);
    }

    public static float getDesiredWidthWithLimit(CharSequence source, int start, int end, TextPaint paint, TextDirectionHeuristic textDir, float upperLimit) {
        float need = 0.0f;
        int i = start;
        while (i <= end) {
            int next = TextUtils.indexOf(source, (char) 10, i, end);
            if (next < 0) {
                next = end;
            }
            float w = measurePara(paint, source, i, next, textDir);
            if (w > upperLimit) {
                return upperLimit;
            }
            if (w > need) {
                need = w;
            }
            i = next + 1;
        }
        return need;
    }

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, float spacingMult, float spacingAdd) {
        this(text, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR, spacingMult, spacingAdd);
    }

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, TextDirectionHeuristic textDir, float spacingMult, float spacingAdd) {
        this.mWorkPaint = new TextPaint();
        this.mAlignment = Alignment.ALIGN_NORMAL;
        if (width >= 0) {
            if (paint != null) {
                paint.bgColor = 0;
                paint.baselineShift = 0;
            }
            this.mText = text;
            this.mPaint = paint;
            this.mWidth = width;
            this.mAlignment = align;
            this.mSpacingMult = spacingMult;
            this.mSpacingAdd = spacingAdd;
            this.mSpannedText = text instanceof Spanned;
            this.mTextDir = textDir;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Layout: ");
        stringBuilder.append(width);
        stringBuilder.append(" < 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected void setJustificationMode(int justificationMode) {
        this.mJustificationMode = justificationMode;
    }

    void replaceWith(CharSequence text, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd) {
        if (width >= 0) {
            this.mText = text;
            this.mPaint = paint;
            this.mWidth = width;
            this.mAlignment = align;
            this.mSpacingMult = spacingmult;
            this.mSpacingAdd = spacingadd;
            this.mSpannedText = text instanceof Spanned;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Layout: ");
        stringBuilder.append(width);
        stringBuilder.append(" < 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void draw(Canvas c) {
        draw(c, null, null, 0);
    }

    public void draw(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffsetVertical) {
        long lineRange = getLineRangeForDraw(canvas);
        int firstLine = TextUtils.unpackRangeStartFromLong(lineRange);
        int lastLine = TextUtils.unpackRangeEndFromLong(lineRange);
        if (lastLine >= 0) {
            drawBackground(canvas, highlight, highlightPaint, cursorOffsetVertical, firstLine, lastLine);
            drawText(canvas, firstLine, lastLine);
        }
    }

    private boolean isJustificationRequired(int lineNum) {
        boolean z = false;
        if (this.mJustificationMode == 0) {
            return false;
        }
        int lineEnd = getLineEnd(lineNum);
        if (lineEnd < this.mText.length() && this.mText.charAt(lineEnd - 1) != 10) {
            z = true;
        }
        return z;
    }

    private float getJustifyWidth(int lineNum) {
        int n;
        int i = lineNum;
        Alignment paraAlign = this.mAlignment;
        int left = 0;
        int right = this.mWidth;
        int dir = getParagraphDirection(lineNum);
        ParagraphStyle[] spans = NO_PARA_SPANS;
        if (this.mSpannedText) {
            Spanned sp = this.mText;
            int start = getLineStart(lineNum);
            boolean isFirstParaLine = start == 0 || this.mText.charAt(start - 1) == 10;
            if (isFirstParaLine) {
                spans = (ParagraphStyle[]) getParagraphSpans(sp, start, sp.nextSpanTransition(start, this.mText.length(), ParagraphStyle.class), ParagraphStyle.class);
                for (int n2 = spans.length - 1; n2 >= 0; n2--) {
                    if (spans[n2] instanceof AlignmentSpan) {
                        paraAlign = ((AlignmentSpan) spans[n2]).getAlignment();
                        break;
                    }
                }
            }
            int length = spans.length;
            boolean useFirstLineMargin = isFirstParaLine;
            for (int n3 = 0; n3 < length; n3++) {
                if (spans[n3] instanceof LeadingMarginSpan2) {
                    if (i < getLineForOffset(sp.getSpanStart(spans[n3])) + ((LeadingMarginSpan2) spans[n3]).getLeadingMarginLineCount()) {
                        useFirstLineMargin = true;
                        break;
                    }
                }
            }
            int n4 = 0;
            while (true) {
                n = n4;
                if (n >= length) {
                    break;
                }
                if (spans[n] instanceof LeadingMarginSpan) {
                    LeadingMarginSpan margin = spans[n];
                    if (dir == -1) {
                        right -= margin.getLeadingMargin(useFirstLineMargin);
                    } else {
                        left += margin.getLeadingMargin(useFirstLineMargin);
                    }
                }
                n4 = n + 1;
            }
        }
        Alignment align = paraAlign == Alignment.ALIGN_LEFT ? dir == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE : paraAlign == Alignment.ALIGN_RIGHT ? dir == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL : paraAlign;
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == 1) {
                n = getIndentAdjust(i, Alignment.ALIGN_LEFT);
            } else {
                n = -getIndentAdjust(i, Alignment.ALIGN_RIGHT);
            }
        } else if (align != Alignment.ALIGN_OPPOSITE) {
            n = getIndentAdjust(i, Alignment.ALIGN_CENTER);
        } else if (dir == 1) {
            n = -getIndentAdjust(i, Alignment.ALIGN_RIGHT);
        } else {
            n = getIndentAdjust(i, Alignment.ALIGN_LEFT);
        }
        return (float) ((right - left) - n);
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x01ed A:{SYNTHETIC, EDGE_INSN: B:109:0x01ed->B:48:0x01ed ?: BREAK  , EDGE_INSN: B:109:0x01ed->B:48:0x01ed ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0116  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00c9  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0087  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0116  */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x01ed A:{SYNTHETIC, EDGE_INSN: B:109:0x01ed->B:48:0x01ed ?: BREAK  , EDGE_INSN: B:109:0x01ed->B:48:0x01ed ?: BREAK  , EDGE_INSN: B:109:0x01ed->B:48:0x01ed ?: BREAK  } */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void drawText(Canvas canvas, int firstLine, int lastLine) {
        Layout layout = this;
        int i = firstLine;
        int previousLineBottom = layout.getLineTop(i);
        int previousLineEnd = layout.getLineStart(i);
        ParagraphStyle[] spans = NO_PARA_SPANS;
        Paint paint = layout.mWorkPaint;
        paint.set(layout.mPaint);
        CharSequence buf = layout.mText;
        Alignment paraAlign = layout.mAlignment;
        TextLine tl = TextLine.obtain();
        TabStops tabStops = null;
        boolean tabStopsIsInitialized = false;
        int spanEnd = 0;
        ParagraphStyle[] spans2 = spans;
        int previousLineBottom2 = previousLineBottom;
        previousLineBottom = i;
        while (true) {
            int lineNum = previousLineBottom;
            CharSequence buf2;
            Paint paint2;
            if (lineNum <= lastLine) {
                int start;
                boolean tabStopsIsInitialized2;
                int spanEnd2;
                int right;
                int dir;
                int lbaseline;
                int lineNum2;
                TabStops tabStops2;
                TextLine tl2;
                boolean useFirstLineMargin;
                int i2;
                Layout spans3;
                ParagraphStyle[] spans4;
                TabStops tabStops3;
                int i3;
                ParagraphStyle[] spans5;
                TextLine tl3;
                previousLineBottom = previousLineEnd;
                previousLineEnd = layout.getLineStart(lineNum + 1);
                boolean justify = layout.isJustificationRequired(lineNum);
                int end = layout.getLineVisibleEnd(lineNum, previousLineBottom, previousLineEnd);
                paint.setHyphenEdit(layout.getHyphen(lineNum));
                int ltop = previousLineBottom2;
                int previousLineEnd2 = previousLineEnd;
                boolean lbottom = layout.getLineTop(lineNum + 1);
                boolean previousLineBottom3 = lbottom;
                previousLineBottom2 = lbottom - layout.getLineDescent(lineNum);
                previousLineEnd = layout.getParagraphDirection(lineNum);
                int lbaseline2 = previousLineBottom2;
                int right2 = layout.mWidth;
                int ltop2 = ltop;
                ParagraphStyle[] spans6;
                Alignment paraAlign2;
                if (layout.mSpannedText) {
                    boolean z;
                    boolean isFirstParaLine;
                    boolean start2;
                    int n;
                    Spanned sp;
                    boolean useFirstLineMargin2;
                    Spanned sp2;
                    int n2;
                    int length;
                    ParagraphStyle[] spans7;
                    int i4;
                    Spanned sp3 = (Spanned) buf;
                    ltop = buf.length();
                    if (previousLineBottom != 0) {
                        spans6 = spans2;
                        paraAlign2 = paraAlign;
                        if (buf.charAt(previousLineBottom - 1) != 10) {
                            z = false;
                            isFirstParaLine = z;
                            if (previousLineBottom >= spanEnd) {
                                start = previousLineBottom;
                            } else if (lineNum == i || isFirstParaLine) {
                                spanEnd = sp3.nextSpanTransition(previousLineBottom, ltop, ParagraphStyle.class);
                                spans2 = (ParagraphStyle[]) getParagraphSpans(sp3, previousLineBottom, spanEnd, ParagraphStyle.class);
                                paraAlign = layout.mAlignment;
                                start = previousLineBottom;
                                previousLineBottom = spans2.length - 1;
                                while (previousLineBottom >= 0) {
                                    Alignment paraAlign3 = paraAlign;
                                    if (spans2[previousLineBottom] instanceof AlignmentSpan) {
                                        paraAlign = ((AlignmentSpan) spans2[previousLineBottom]).getAlignment();
                                        break;
                                    } else {
                                        previousLineBottom--;
                                        paraAlign = paraAlign3;
                                    }
                                }
                                tabStopsIsInitialized2 = false;
                                tabStopsIsInitialized = spans2;
                                paraAlign2 = paraAlign;
                                spanEnd2 = spanEnd;
                                spanEnd = tabStopsIsInitialized.length;
                                start2 = isFirstParaLine;
                                n = 0;
                                while (n < spanEnd) {
                                    boolean useFirstLineMargin3;
                                    if (tabStopsIsInitialized[n] instanceof LeadingMarginSpan2) {
                                        useFirstLineMargin3 = start2;
                                        sp = sp3;
                                        if (lineNum < layout.getLineForOffset(sp3.getSpanStart(tabStopsIsInitialized[n])) + ((LeadingMarginSpan2) tabStopsIsInitialized[n]).getLeadingMarginLineCount()) {
                                            useFirstLineMargin2 = true;
                                            break;
                                        }
                                    } else {
                                        useFirstLineMargin3 = start2;
                                        sp = sp3;
                                    }
                                    n++;
                                    start2 = useFirstLineMargin3;
                                    sp3 = sp;
                                }
                                sp = sp3;
                                useFirstLineMargin2 = start2;
                                right = right2;
                                previousLineBottom = 0;
                                right2 = 0;
                                while (true) {
                                    n = previousLineBottom;
                                    if (n >= spanEnd) {
                                        break;
                                    }
                                    if ((tabStopsIsInitialized[n] instanceof LeadingMarginSpan) != 0) {
                                        LeadingMarginSpan margin = (LeadingMarginSpan) tabStopsIsInitialized[n];
                                        LeadingMarginSpan margin2;
                                        Paint paint3;
                                        if (previousLineEnd == -1) {
                                            dir = previousLineEnd;
                                            margin2 = margin;
                                            lbaseline = lbaseline2;
                                            sp2 = sp;
                                            n2 = n;
                                            paint3 = paint;
                                            TextPaint paint4 = useFirstLineMargin2;
                                            length = spanEnd;
                                            spans7 = tabStopsIsInitialized;
                                            lineNum2 = lineNum;
                                            tabStops2 = tabStops;
                                            tl2 = tl;
                                            buf2 = buf;
                                            i4 = ltop2;
                                            ltop2 = ltop;
                                            ltop = i4;
                                            margin.drawLeadingMargin(canvas, paint, right, dir, ltop, lbaseline, lbottom, buf, start, end, isFirstParaLine, layout);
                                            right -= margin2.getLeadingMargin(paint4);
                                            useFirstLineMargin = paint4;
                                            n = layout;
                                            previousLineBottom = paint3;
                                        } else {
                                            dir = previousLineEnd;
                                            margin2 = margin;
                                            n2 = n;
                                            spans7 = tabStopsIsInitialized;
                                            lineNum2 = lineNum;
                                            tabStops2 = tabStops;
                                            tl2 = tl;
                                            buf2 = buf;
                                            paint3 = paint;
                                            lbaseline = lbaseline2;
                                            sp2 = sp;
                                            length = spanEnd;
                                            i4 = ltop2;
                                            ltop2 = ltop;
                                            ltop = i4;
                                            useFirstLineMargin = useFirstLineMargin2;
                                            previousLineBottom = paint3;
                                            previousLineEnd = margin2;
                                            n = layout;
                                            margin2.drawLeadingMargin(canvas, paint3, right2, dir, ltop, lbaseline, lbottom, buf2, start, end, isFirstParaLine, n);
                                            right2 += previousLineEnd.getLeadingMargin(useFirstLineMargin);
                                        }
                                    } else {
                                        dir = previousLineEnd;
                                        n2 = n;
                                        useFirstLineMargin = useFirstLineMargin2;
                                        spans7 = tabStopsIsInitialized;
                                        lineNum2 = lineNum;
                                        tabStops2 = tabStops;
                                        tl2 = tl;
                                        buf2 = buf;
                                        previousLineBottom = paint;
                                        n = layout;
                                        lbaseline = lbaseline2;
                                        sp2 = sp;
                                        length = spanEnd;
                                        i4 = ltop2;
                                        ltop2 = ltop;
                                        ltop = i4;
                                    }
                                    i = firstLine;
                                    i2 = lastLine;
                                    paint = previousLineBottom;
                                    previousLineBottom = n2 + 1;
                                    useFirstLineMargin2 = useFirstLineMargin;
                                    layout = n;
                                    spanEnd = length;
                                    previousLineEnd = dir;
                                    tabStopsIsInitialized = spans7;
                                    lineNum = lineNum2;
                                    tabStops = tabStops2;
                                    tl = tl2;
                                    buf = buf2;
                                    sp = sp2;
                                    lbaseline2 = lbaseline;
                                    i4 = ltop2;
                                    ltop2 = ltop;
                                    ltop = i4;
                                }
                                dir = previousLineEnd;
                                lineNum2 = lineNum;
                                tabStops2 = tabStops;
                                tl2 = tl;
                                buf2 = buf;
                                paint2 = paint;
                                spans3 = layout;
                                lbaseline = lbaseline2;
                                ltop = ltop2;
                                paraAlign = paraAlign2;
                                spans4 = tabStopsIsInitialized;
                            } else {
                                start = previousLineBottom;
                            }
                            tabStopsIsInitialized2 = tabStopsIsInitialized;
                            tabStopsIsInitialized = spans6;
                            spanEnd2 = spanEnd;
                            spanEnd = tabStopsIsInitialized.length;
                            start2 = isFirstParaLine;
                            n = 0;
                            while (n < spanEnd) {
                            }
                            sp = sp3;
                            useFirstLineMargin2 = start2;
                            right = right2;
                            previousLineBottom = 0;
                            right2 = 0;
                            while (true) {
                                n = previousLineBottom;
                                if (n >= spanEnd) {
                                }
                                i = firstLine;
                                i2 = lastLine;
                                paint = previousLineBottom;
                                previousLineBottom = n2 + 1;
                                useFirstLineMargin2 = useFirstLineMargin;
                                layout = n;
                                spanEnd = length;
                                previousLineEnd = dir;
                                tabStopsIsInitialized = spans7;
                                lineNum = lineNum2;
                                tabStops = tabStops2;
                                tl = tl2;
                                buf = buf2;
                                sp = sp2;
                                lbaseline2 = lbaseline;
                                i4 = ltop2;
                                ltop2 = ltop;
                                ltop = i4;
                            }
                            dir = previousLineEnd;
                            lineNum2 = lineNum;
                            tabStops2 = tabStops;
                            tl2 = tl;
                            buf2 = buf;
                            paint2 = paint;
                            spans3 = layout;
                            lbaseline = lbaseline2;
                            ltop = ltop2;
                            paraAlign = paraAlign2;
                            spans4 = tabStopsIsInitialized;
                        }
                    } else {
                        spans6 = spans2;
                        paraAlign2 = paraAlign;
                    }
                    z = true;
                    isFirstParaLine = z;
                    if (previousLineBottom >= spanEnd) {
                    }
                    tabStopsIsInitialized2 = tabStopsIsInitialized;
                    tabStopsIsInitialized = spans6;
                    spanEnd2 = spanEnd;
                    spanEnd = tabStopsIsInitialized.length;
                    start2 = isFirstParaLine;
                    n = 0;
                    while (n < spanEnd) {
                    }
                    sp = sp3;
                    useFirstLineMargin2 = start2;
                    right = right2;
                    previousLineBottom = 0;
                    right2 = 0;
                    while (true) {
                        n = previousLineBottom;
                        if (n >= spanEnd) {
                        }
                        i = firstLine;
                        i2 = lastLine;
                        paint = previousLineBottom;
                        previousLineBottom = n2 + 1;
                        useFirstLineMargin2 = useFirstLineMargin;
                        layout = n;
                        spanEnd = length;
                        previousLineEnd = dir;
                        tabStopsIsInitialized = spans7;
                        lineNum = lineNum2;
                        tabStops = tabStops2;
                        tl = tl2;
                        buf = buf2;
                        sp = sp2;
                        lbaseline2 = lbaseline;
                        i4 = ltop2;
                        ltop2 = ltop;
                        ltop = i4;
                    }
                    dir = previousLineEnd;
                    lineNum2 = lineNum;
                    tabStops2 = tabStops;
                    tl2 = tl;
                    buf2 = buf;
                    paint2 = paint;
                    spans3 = layout;
                    lbaseline = lbaseline2;
                    ltop = ltop2;
                    paraAlign = paraAlign2;
                    spans4 = tabStopsIsInitialized;
                } else {
                    start = previousLineBottom;
                    dir = previousLineEnd;
                    spans6 = spans2;
                    paraAlign2 = paraAlign;
                    lineNum2 = lineNum;
                    tabStops2 = tabStops;
                    tl2 = tl;
                    buf2 = buf;
                    paint2 = paint;
                    spans3 = layout;
                    lbaseline = lbaseline2;
                    ltop = ltop2;
                    tabStopsIsInitialized2 = tabStopsIsInitialized;
                    right = right2;
                    spans4 = spans6;
                    spanEnd2 = spanEnd;
                    right2 = 0;
                }
                previousLineBottom2 = lineNum2;
                boolean hasTab = spans3.getLineContainsTab(previousLineBottom2);
                if (!hasTab || tabStopsIsInitialized) {
                    tabStops3 = tabStops2;
                } else {
                    TabStops tabStops4 = tabStops2;
                    if (tabStops4 == null) {
                        tabStops = new TabStops(20, spans4);
                    } else {
                        tabStops4.reset(20, spans4);
                        tabStops = tabStops4;
                    }
                    tabStopsIsInitialized2 = true;
                    tabStops3 = tabStops;
                }
                Alignment align = paraAlign;
                if (align == Alignment.ALIGN_LEFT) {
                    i = dir;
                    i3 = 1;
                    align = i == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
                } else {
                    i = dir;
                    i3 = 1;
                    if (align == Alignment.ALIGN_RIGHT) {
                        align = i == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
                    }
                }
                Alignment align2 = align;
                if (align2 == Alignment.ALIGN_NORMAL) {
                    if (i == i3) {
                        spanEnd = spans3.getIndentAdjust(previousLineBottom2, Alignment.ALIGN_LEFT);
                        i3 = right2 + spanEnd;
                    } else {
                        spanEnd = -spans3.getIndentAdjust(previousLineBottom2, Alignment.ALIGN_RIGHT);
                        i3 = right - spanEnd;
                    }
                    ltop2 = spanEnd;
                } else {
                    spanEnd = (int) spans3.getLineExtent(previousLineBottom2, tabStops3, false);
                    if (align2 == Alignment.ALIGN_OPPOSITE) {
                        if (i == i3) {
                            i3 = -spans3.getIndentAdjust(previousLineBottom2, Alignment.ALIGN_RIGHT);
                            i2 = (right - spanEnd) - i3;
                        } else {
                            i3 = spans3.getIndentAdjust(previousLineBottom2, Alignment.ALIGN_LEFT);
                            i2 = (right2 - spanEnd) + i3;
                        }
                        ltop2 = i3;
                        i3 = i2;
                    } else {
                        i2 = spans3.getIndentAdjust(previousLineBottom2, Alignment.ALIGN_CENTER);
                        i3 = (((right + right2) - (spanEnd & -2)) >> 1) + i2;
                        ltop2 = i2;
                    }
                }
                int x = i3;
                Directions directions = spans3.getLineDirections(previousLineBottom2);
                if (directions != DIRS_ALL_LEFT_TO_RIGHT || spans3.mSpannedText || hasTab || justify) {
                    spans5 = spans4;
                    previousLineEnd = x;
                    int lbaseline3 = lbaseline;
                    tl2.set(paint2, buf2, start, end, i, directions, hasTab, tabStops3);
                    if (justify) {
                        tl3 = tl2;
                        tl3.justify((float) ((right - right2) - ltop2));
                    } else {
                        tl3 = tl2;
                    }
                    tabStops = tabStops3;
                    tl3.draw(canvas, (float) previousLineEnd, ltop, lbaseline3, lbottom);
                } else {
                    lineNum = lbaseline;
                    spans5 = spans4;
                    spans4 = x;
                    canvas.drawText(buf2, start, end, (float) x, (float) lineNum, paint2);
                    tabStops = tabStops3;
                    tl3 = tl2;
                }
                i = firstLine;
                paint = paint2;
                previousLineBottom = previousLineBottom2 + 1;
                layout = spans3;
                tl = tl3;
                previousLineEnd = previousLineEnd2;
                useFirstLineMargin = previousLineBottom3;
                tabStopsIsInitialized = tabStopsIsInitialized2;
                spanEnd = spanEnd2;
                buf = buf2;
                spans2 = spans5;
            } else {
                buf2 = buf;
                paint2 = paint;
                spans2 = layout;
                TextLine.recycle(tl);
                return;
            }
        }
    }

    public void drawBackground(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffsetVertical, int firstLine, int lastLine) {
        Canvas canvas2 = canvas;
        int i = cursorOffsetVertical;
        int i2 = firstLine;
        if (this.mSpannedText) {
            Spanned buffer;
            if (this.mLineBackgroundSpans == null) {
                this.mLineBackgroundSpans = new SpanSet(LineBackgroundSpan.class);
            }
            Spanned buffer2 = (Spanned) this.mText;
            int textLength = buffer2.length();
            this.mLineBackgroundSpans.init(buffer2, 0, textLength);
            if (this.mLineBackgroundSpans.numberOfSpans > 0) {
                int previousLineBottom = getLineTop(i2);
                int previousLineEnd = getLineStart(i2);
                ParagraphStyle[] spans = NO_PARA_SPANS;
                TextPaint paint = this.mPaint;
                int width = this.mWidth;
                int spanEnd = 0;
                int spansLength = 0;
                int previousLineEnd2 = previousLineEnd;
                previousLineEnd = previousLineBottom;
                previousLineBottom = i2;
                while (previousLineBottom <= lastLine) {
                    int i3;
                    int spanEnd2;
                    ParagraphStyle[] spans2;
                    int spansLength2;
                    int width2;
                    Paint paint2;
                    Paint paint3;
                    int textLength2;
                    int start = previousLineEnd2;
                    int end = getLineStart(previousLineBottom + 1);
                    int previousLineEnd3 = end;
                    int ltop = previousLineEnd;
                    int lbottom = getLineTop(previousLineBottom + 1);
                    int previousLineBottom2 = lbottom;
                    int lbaseline = lbottom - getLineDescent(previousLineBottom);
                    previousLineEnd2 = start;
                    if (previousLineEnd2 >= spanEnd) {
                        previousLineEnd = this.mLineBackgroundSpans.getNextTransition(previousLineEnd2, textLength);
                        spansLength = 0;
                        if (previousLineEnd2 != end || previousLineEnd2 == 0) {
                            spanEnd = spans;
                            spans = null;
                            while (true) {
                                i3 = previousLineBottom;
                                if (spans >= this.mLineBackgroundSpans.numberOfSpans) {
                                    break;
                                }
                                if (this.mLineBackgroundSpans.spanStarts[spans] < end && this.mLineBackgroundSpans.spanEnds[spans] > previousLineEnd2) {
                                    spansLength++;
                                    spanEnd = (ParagraphStyle[]) GrowingArrayUtils.append(spanEnd, spansLength, ((LineBackgroundSpan[]) this.mLineBackgroundSpans.spans)[spans]);
                                }
                                spans++;
                                previousLineBottom = i3;
                            }
                            spanEnd2 = previousLineEnd;
                            spans2 = spanEnd;
                        } else {
                            i3 = previousLineBottom;
                            spanEnd2 = previousLineEnd;
                            spans2 = spans;
                        }
                        spanEnd = spansLength;
                    } else {
                        i3 = previousLineBottom;
                        spans2 = spans;
                        spanEnd2 = spanEnd;
                        spanEnd = spansLength;
                    }
                    previousLineBottom = 0;
                    while (true) {
                        spansLength = previousLineBottom;
                        if (spansLength >= spanEnd) {
                            break;
                        }
                        int start2 = previousLineEnd2;
                        int n = spansLength;
                        spansLength2 = spanEnd;
                        width2 = width;
                        paint2 = paint3;
                        int end2 = end;
                        textLength2 = textLength;
                        buffer = buffer2;
                        ((LineBackgroundSpan) spans2[spansLength]).drawBackground(canvas2, paint3, 0, width, ltop, lbaseline, lbottom, buffer2, start2, end2, i3);
                        previousLineBottom = n + 1;
                        end = end2;
                        previousLineEnd2 = start2;
                        spanEnd = spansLength2;
                        width = width2;
                        paint3 = paint2;
                        textLength = textLength2;
                        buffer2 = buffer;
                    }
                    spansLength2 = spanEnd;
                    width2 = width;
                    paint2 = paint3;
                    textLength2 = textLength;
                    buffer = buffer2;
                    previousLineBottom = i3 + 1;
                    spans = spans2;
                    previousLineEnd2 = previousLineEnd3;
                    previousLineEnd = previousLineBottom2;
                    spanEnd = spanEnd2;
                    spansLength = spansLength2;
                    Object obj = null;
                }
            }
            buffer = buffer2;
            this.mLineBackgroundSpans.recycle();
        }
        if (highlight != null) {
            if (i != 0) {
                canvas2.translate(0.0f, (float) i);
            }
            canvas.drawPath(highlight, highlightPaint);
            if (i != 0) {
                canvas2.translate(0.0f, (float) (-i));
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x001c, code skipped:
            r0 = java.lang.Math.max(r1, 0);
            r5 = java.lang.Math.min(getLineTop(getLineCount()), r4);
     */
    /* JADX WARNING: Missing block: B:11:0x002c, code skipped:
            if (r0 < r5) goto L_0x0033;
     */
    /* JADX WARNING: Missing block: B:13:0x0032, code skipped:
            return android.text.TextUtils.packRangeInLong(0, -1);
     */
    /* JADX WARNING: Missing block: B:15:0x003f, code skipped:
            return android.text.TextUtils.packRangeInLong(getLineForVertical(r0), getLineForVertical(r5));
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getLineRangeForDraw(Canvas canvas) {
        synchronized (sTempRect) {
            if (canvas.getClipBounds(sTempRect)) {
                int dtop = sTempRect.top;
                int dbottom = sTempRect.bottom;
            } else {
                long packRangeInLong = TextUtils.packRangeInLong(0, -1);
                return packRangeInLong;
            }
        }
    }

    private int getLineStartPos(int line, int left, int right) {
        Alignment align = getParagraphAlignment(line);
        int dir = getParagraphDirection(line);
        if (align == Alignment.ALIGN_LEFT) {
            align = dir == 1 ? Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
        } else if (align == Alignment.ALIGN_RIGHT) {
            align = dir == 1 ? Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
        }
        if (align != Alignment.ALIGN_NORMAL) {
            TabStops tabStops = null;
            if (this.mSpannedText && getLineContainsTab(line)) {
                Spanned spanned = this.mText;
                int start = getLineStart(line);
                TabStopSpan[] tabSpans = (TabStopSpan[]) getParagraphSpans(spanned, start, spanned.nextSpanTransition(start, spanned.length(), TabStopSpan.class), TabStopSpan.class);
                if (tabSpans.length > 0) {
                    tabStops = new TabStops(20, tabSpans);
                }
            }
            int max = (int) getLineExtent(line, tabStops, false);
            if (align == Alignment.ALIGN_OPPOSITE) {
                int x;
                if (dir == 1) {
                    x = (right - max) + getIndentAdjust(line, Alignment.ALIGN_RIGHT);
                } else {
                    x = (left - max) + getIndentAdjust(line, Alignment.ALIGN_LEFT);
                }
                return x;
            }
            return ((left + right) - (max & -2)) >> (1 + getIndentAdjust(line, Alignment.ALIGN_CENTER));
        } else if (dir == 1) {
            return getIndentAdjust(line, Alignment.ALIGN_LEFT) + left;
        } else {
            return getIndentAdjust(line, Alignment.ALIGN_RIGHT) + right;
        }
    }

    public final CharSequence getText() {
        return this.mText;
    }

    public final TextPaint getPaint() {
        return this.mPaint;
    }

    public final int getWidth() {
        return this.mWidth;
    }

    public int getEllipsizedWidth() {
        return this.mWidth;
    }

    public final void increaseWidthTo(int wid) {
        if (wid >= this.mWidth) {
            this.mWidth = wid;
            return;
        }
        throw new RuntimeException("attempted to reduce Layout width");
    }

    public int getHeight() {
        return getLineTop(getLineCount());
    }

    public int getHeight(boolean cap) {
        return getHeight();
    }

    public final Alignment getAlignment() {
        return this.mAlignment;
    }

    public final float getSpacingMultiplier() {
        return this.mSpacingMult;
    }

    public final float getSpacingAdd() {
        return this.mSpacingAdd;
    }

    public final TextDirectionHeuristic getTextDirectionHeuristic() {
        return this.mTextDir;
    }

    public int getLineBounds(int line, Rect bounds) {
        if (bounds != null) {
            bounds.left = 0;
            bounds.top = getLineTop(line);
            bounds.right = this.mWidth;
            bounds.bottom = getLineTop(line + 1);
        }
        return getLineBaseline(line);
    }

    public int getHyphen(int line) {
        return 0;
    }

    public int getIndentAdjust(int line, Alignment alignment) {
        return 0;
    }

    public boolean isLevelBoundary(int offset) {
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        boolean z = false;
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT || dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return false;
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        if (offset == lineStart || offset == lineEnd) {
            if (((runs[(offset == lineStart ? 0 : runs.length - 2) + 1] >>> 26) & 63) != (getParagraphDirection(line) == 1 ? 0 : 1)) {
                z = true;
            }
            return z;
        }
        offset -= lineStart;
        for (int i = 0; i < runs.length; i += 2) {
            if (offset == runs[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isRtlCharAt(int offset) {
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        boolean z = false;
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT) {
            return false;
        }
        if (dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return true;
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        int i = 0;
        while (i < runs.length) {
            int start = runs[i] + lineStart;
            int limit = (runs[i + 1] & RUN_LENGTH_MASK) + start;
            if (offset < start || offset >= limit) {
                i += 2;
            } else {
                if ((((runs[i + 1] >>> 26) & 63) & 1) != 0) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    public long getRunRange(int offset) {
        int line = getLineForOffset(offset);
        Directions dirs = getLineDirections(line);
        if (dirs == DIRS_ALL_LEFT_TO_RIGHT || dirs == DIRS_ALL_RIGHT_TO_LEFT) {
            return TextUtils.packRangeInLong(0, getLineEnd(line));
        }
        int[] runs = dirs.mDirections;
        int lineStart = getLineStart(line);
        for (int i = 0; i < runs.length; i += 2) {
            int start = runs[i] + lineStart;
            int limit = (runs[i + 1] & RUN_LENGTH_MASK) + start;
            if (offset >= start && offset < limit) {
                return TextUtils.packRangeInLong(start, limit);
            }
        }
        return TextUtils.packRangeInLong(0, getLineEnd(line));
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x003e  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x004b  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x007a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean primaryIsTrailingPrevious(int offset) {
        int line = getLineForOffset(offset);
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int[] runs = getLineDirections(line).mDirections;
        int levelAt = -1;
        boolean z = false;
        int i = 0;
        while (i < runs.length) {
            int start = runs[i] + lineStart;
            int limit = (runs[i + 1] & RUN_LENGTH_MASK) + start;
            if (limit > lineEnd) {
                limit = lineEnd;
            }
            if (offset < start || offset >= limit) {
                i += 2;
            } else if (offset > start) {
                return false;
            } else {
                levelAt = (runs[i + 1] >>> 26) & 63;
                if (levelAt == -1) {
                    levelAt = getParagraphDirection(line) != 1;
                }
                i = -1;
                if (offset == lineStart) {
                    offset--;
                    for (limit = 0; limit < runs.length; limit += 2) {
                        int start2 = runs[limit] + lineStart;
                        int limit2 = (runs[limit + 1] & RUN_LENGTH_MASK) + start2;
                        if (limit2 > lineEnd) {
                            limit2 = lineEnd;
                        }
                        if (offset >= start2 && offset < limit2) {
                            i = (runs[limit + 1] >>> 26) & 63;
                            break;
                        }
                    }
                } else {
                    i = getParagraphDirection(line) != 1;
                }
                if (i < levelAt) {
                    z = true;
                }
                return z;
            }
        }
        if (levelAt == -1) {
        }
        i = -1;
        if (offset == lineStart) {
        }
        if (i < levelAt) {
        }
        return z;
    }

    private boolean[] primaryIsTrailingPreviousAllLineOffsets(int line) {
        int i;
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int[] runs = getLineDirections(line).mDirections;
        boolean[] trailing = new boolean[((lineEnd - lineStart) + 1)];
        byte[] level = new byte[((lineEnd - lineStart) + 1)];
        for (i = 0; i < runs.length; i += 2) {
            int limit = (runs[i + 1] & RUN_LENGTH_MASK) + (runs[i] + lineStart);
            if (limit > lineEnd) {
                limit = lineEnd;
            }
            level[(limit - lineStart) - 1] = (byte) ((runs[i + 1] >>> 26) & 63);
        }
        for (i = 0; i < runs.length; i += 2) {
            int start = runs[i] + lineStart;
            byte currentLevel = (byte) ((runs[i + 1] >>> 26) & 63);
            int i2 = start - lineStart;
            byte b = start == lineStart ? getParagraphDirection(line) == 1 ? (byte) 0 : (byte) 1 : level[(start - lineStart) - 1];
            trailing[i2] = currentLevel > b;
        }
        return trailing;
    }

    public float getPrimaryHorizontal(int offset) {
        return getPrimaryHorizontal(offset, false);
    }

    public float getPrimaryHorizontal(int offset, boolean clamped) {
        return getHorizontal(offset, primaryIsTrailingPrevious(offset), clamped);
    }

    public float getSecondaryHorizontal(int offset) {
        return getSecondaryHorizontal(offset, false);
    }

    public float getSecondaryHorizontal(int offset, boolean clamped) {
        return getHorizontal(offset, primaryIsTrailingPrevious(offset) ^ 1, clamped);
    }

    private float getHorizontal(int offset, boolean primary) {
        return primary ? getPrimaryHorizontal(offset) : getSecondaryHorizontal(offset);
    }

    private float getHorizontal(int offset, boolean trailing, boolean clamped) {
        return getHorizontal(offset, trailing, getLineForOffset(offset), clamped);
    }

    private float getHorizontal(int offset, boolean trailing, int line, boolean clamped) {
        int i = line;
        int start = getLineStart(i);
        int end = getLineEnd(i);
        int dir = getParagraphDirection(i);
        boolean hasTab = getLineContainsTab(i);
        Directions directions = getLineDirections(i);
        TabStops tabStops = null;
        if (hasTab && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        TabStops tabStops2 = tabStops;
        TextLine tl = TextLine.obtain();
        TextLine tl2 = tl;
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTab, tabStops2);
        float wid = tl2.measure(offset - start, trailing, null);
        TextLine.recycle(tl2);
        if (clamped && wid > ((float) this.mWidth)) {
            wid = (float) this.mWidth;
        }
        return ((float) getLineStartPos(i, getParagraphLeft(i), getParagraphRight(i))) + wid;
    }

    private float[] getLineHorizontals(int line, boolean clamped, boolean primary) {
        int start = getLineStart(line);
        int end = getLineEnd(line);
        int dir = getParagraphDirection(line);
        boolean hasTab = getLineContainsTab(line);
        Directions directions = getLineDirections(line);
        TabStops tabStops = null;
        if (hasTab && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        TabStops tabStops2 = tabStops;
        TextLine tl = TextLine.obtain();
        TextLine tl2 = tl;
        tl.set(this.mPaint, this.mText, start, end, dir, directions, hasTab, tabStops2);
        boolean[] trailings = primaryIsTrailingPreviousAllLineOffsets(line);
        if (!primary) {
            for (int offset = 0; offset < trailings.length; offset++) {
                trailings[offset] = trailings[offset] ^ 1;
            }
        }
        float[] wid = tl2.measureAllOffsets(trailings, null);
        TextLine.recycle(tl2);
        if (clamped) {
            for (int offset2 = 0; offset2 <= wid.length; offset2++) {
                if (wid[offset2] > ((float) this.mWidth)) {
                    wid[offset2] = (float) this.mWidth;
                }
            }
        }
        int lineStartPos = getLineStartPos(line, getParagraphLeft(line), getParagraphRight(line));
        float[] horizontal = new float[((end - start) + 1)];
        int offset3 = 0;
        while (true) {
            boolean[] trailings2 = trailings;
            int offset4 = offset3;
            if (offset4 >= horizontal.length) {
                return horizontal;
            }
            horizontal[offset4] = ((float) lineStartPos) + wid[offset4];
            offset3 = offset4 + 1;
            trailings = trailings2;
        }
    }

    public float getLineLeft(int line) {
        int dir = getParagraphDirection(line);
        Alignment align = getParagraphAlignment(line);
        if (align == Alignment.ALIGN_LEFT) {
            return 0.0f;
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == -1) {
                return ((float) getParagraphRight(line)) - getLineMax(line);
            }
            return 0.0f;
        } else if (align == Alignment.ALIGN_RIGHT) {
            return ((float) this.mWidth) - getLineMax(line);
        } else {
            if (align != Alignment.ALIGN_OPPOSITE) {
                int left = getParagraphLeft(line);
                return (float) ((((getParagraphRight(line) - left) - (((int) getLineMax(line)) & -2)) / 2) + left);
            } else if (dir == -1) {
                return 0.0f;
            } else {
                return ((float) this.mWidth) - getLineMax(line);
            }
        }
    }

    public float getLineRight(int line) {
        int dir = getParagraphDirection(line);
        Alignment align = getParagraphAlignment(line);
        if (align == Alignment.ALIGN_LEFT) {
            return ((float) getParagraphLeft(line)) + getLineMax(line);
        }
        if (align == Alignment.ALIGN_NORMAL) {
            if (dir == -1) {
                return (float) this.mWidth;
            }
            return ((float) getParagraphLeft(line)) + getLineMax(line);
        } else if (align == Alignment.ALIGN_RIGHT) {
            return (float) this.mWidth;
        } else {
            if (align != Alignment.ALIGN_OPPOSITE) {
                int left = getParagraphLeft(line);
                int right = getParagraphRight(line);
                return (float) (right - (((right - left) - (((int) getLineMax(line)) & -2)) / 2));
            } else if (dir == -1) {
                return getLineMax(line);
            } else {
                return (float) this.mWidth;
            }
        }
    }

    public float getLineMax(int line) {
        float margin = (float) getParagraphLeadingMargin(line);
        float signedExtent = getLineExtent(line, 0.0f);
        return (signedExtent >= 0.0f ? signedExtent : -signedExtent) + margin;
    }

    public float getLineWidth(int line) {
        float margin = (float) getParagraphLeadingMargin(line);
        float signedExtent = getLineExtent(line, Float.MIN_VALUE);
        return (signedExtent >= 0.0f ? signedExtent : -signedExtent) + margin;
    }

    private float getLineExtent(int line, boolean full) {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        boolean hasTabs = getLineContainsTab(line);
        TabStops tabStops = null;
        if (hasTabs && (this.mText instanceof Spanned)) {
            TabStopSpan[] tabs = (TabStopSpan[]) getParagraphSpans((Spanned) this.mText, start, end, TabStopSpan.class);
            if (tabs.length > 0) {
                tabStops = new TabStops(20, tabs);
            }
        }
        TabStops tabStops2 = tabStops;
        Directions directions = getLineDirections(line);
        if (directions == null) {
            return 0.0f;
        }
        int dir = getParagraphDirection(line);
        TextLine tl = TextLine.obtain();
        TextPaint paint = this.mWorkPaint;
        paint.set(this.mPaint);
        paint.setHyphenEdit(getHyphen(line));
        TextLine tl2 = tl;
        tl.set(paint, this.mText, start, end, dir, directions, hasTabs, tabStops2);
        if (isJustificationRequired(line)) {
            tl2.justify(getJustifyWidth(line));
        }
        float width = tl2.metrics(0.0f);
        TextLine.recycle(tl2);
        return width;
    }

    private float getLineExtent(int line, TabStops tabStops, boolean full) {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        boolean hasTabs = getLineContainsTab(line);
        Directions directions = getLineDirections(line);
        int dir = getParagraphDirection(line);
        TextLine tl = TextLine.obtain();
        TextPaint paint = this.mWorkPaint;
        paint.set(this.mPaint);
        paint.setHyphenEdit(getHyphen(line));
        tl.set(paint, this.mText, start, end, dir, directions, hasTabs, tabStops);
        if (isJustificationRequired(line)) {
            tl.justify(getJustifyWidth(line));
        }
        float width = tl.metrics(0.0f);
        TextLine.recycle(tl);
        return width;
    }

    public int getLineForVertical(int vertical) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineTop(guess) > vertical) {
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

    public int getLineForOffset(int offset) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineStart(guess) > offset) {
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

    public int getOffsetForHorizontal(int line, float horiz) {
        return getOffsetForHorizontal(line, horiz, true);
    }

    public int getOffsetForHorizontal(int line, float horiz, boolean primary) {
        int max;
        int best;
        Layout swap = this;
        int i = line;
        int lineEndOffset = getLineEnd(line);
        int lineStartOffset = getLineStart(line);
        Directions dirs = getLineDirections(line);
        TextLine tl = TextLine.obtain();
        tl.set(swap.mPaint, swap.mText, lineStartOffset, lineEndOffset, getParagraphDirection(line), dirs, false, null);
        HorizontalMeasurementProvider horizontal = new HorizontalMeasurementProvider(i, primary);
        if (i == getLineCount() - 1) {
            max = lineEndOffset;
        } else {
            max = tl.getOffsetToLeftRightOf(lineEndOffset - lineStartOffset, swap.isRtlCharAt(lineEndOffset - 1) ^ 1) + lineStartOffset;
        }
        int best2 = lineStartOffset;
        float bestdist = Math.abs(horizontal.get(lineStartOffset) - horiz);
        int best3 = best2;
        best2 = 0;
        while (best2 < dirs.mDirections.length) {
            int low;
            int swap2;
            boolean z;
            int here = dirs.mDirections[best2] + lineStartOffset;
            int there = (dirs.mDirections[best2 + 1] & RUN_LENGTH_MASK) + here;
            boolean isRtl = (dirs.mDirections[best2 + 1] & 67108864) != 0;
            int swap3 = isRtl ? -1 : 1;
            if (there > max) {
                there = max;
            }
            i = 1;
            int high = (there - 1) + 1;
            int low2 = (here + 1) - 1;
            while (true) {
                best = best3;
                low = low2;
                if (high - low <= i) {
                    break;
                }
                best3 = (high + low) / 2;
                i = swap.getOffsetAtStartOf(best3);
                swap2 = swap3;
                if (horizontal.get(i) * ((float) swap2) >= ((float) swap2) * horiz) {
                    high = best3;
                    low2 = low;
                } else {
                    low2 = best3;
                }
                swap3 = swap2;
                best3 = best;
                swap = this;
                i = 1;
                z = primary;
            }
            swap2 = swap3;
            if (low < here + 1) {
                low = here + 1;
            }
            if (low < there) {
                i = tl.getOffsetToLeftRightOf(low - lineStartOffset, isRtl) + lineStartOffset;
                best3 = i - lineStartOffset;
                if (isRtl) {
                    swap2 = 0;
                } else {
                    int i2 = swap2;
                    swap2 = 1;
                }
                low = tl.getOffsetToLeftRightOf(best3, swap2) + lineStartOffset;
                if (low >= here && low < there) {
                    swap2 = Math.abs(horizontal.get(low) - horiz);
                    if (i < there) {
                        best3 = Math.abs(horizontal.get(i) - horiz);
                        if (best3 < swap2) {
                            swap2 = best3;
                            low = i;
                        }
                    }
                    if (swap2 < bestdist) {
                        bestdist = swap2;
                        best = low;
                    }
                }
            }
            float dist = Math.abs(horizontal.get(here) - horiz);
            if (dist < bestdist) {
                best3 = here;
                bestdist = dist;
            } else {
                best3 = best;
            }
            best2 += 2;
            swap = this;
            i = line;
            z = primary;
        }
        best = best3;
        if (Math.abs(horizontal.get(max) - horiz) <= bestdist) {
            best3 = max;
        } else {
            best3 = best;
        }
        TextLine.recycle(tl);
        return best3;
    }

    public final int getLineEnd(int line) {
        return getLineStart(line + 1);
    }

    public int getLineVisibleEnd(int line) {
        return getLineVisibleEnd(line, getLineStart(line), getLineStart(line + 1));
    }

    private int getLineVisibleEnd(int line, int start, int end) {
        CharSequence text = this.mText;
        if (line == getLineCount() - 1) {
            return end;
        }
        while (end > start) {
            char ch = text.charAt(end - 1);
            if (ch == 10) {
                return end - 1;
            }
            if (!TextLine.isLineEndSpace(ch)) {
                break;
            }
            end--;
        }
        return end;
    }

    public final int getLineBottom(int line) {
        return getLineTop(line + 1);
    }

    public final int getLineBottomWithoutSpacing(int line) {
        return getLineTop(line + 1) - getLineExtra(line);
    }

    public final int getLineBaseline(int line) {
        return getLineTop(line + 1) - getLineDescent(line);
    }

    public final int getLineAscent(int line) {
        return getLineTop(line) - (getLineTop(line + 1) - getLineDescent(line));
    }

    public int getLineExtra(int line) {
        return 0;
    }

    public int getOffsetToLeftOf(int offset) {
        return getOffsetToLeftRightOf(offset, true);
    }

    public int getOffsetToRightOf(int offset) {
        return getOffsetToLeftRightOf(offset, false);
    }

    private int getOffsetToLeftRightOf(int caret, boolean toLeft) {
        int newDir;
        int i = caret;
        boolean toLeft2 = toLeft;
        int line = getLineForOffset(caret);
        int lineStart = getLineStart(line);
        int lineEnd = getLineEnd(line);
        int lineDir = getParagraphDirection(line);
        boolean lineChanged = false;
        boolean advance = false;
        if (toLeft2 == (lineDir == -1)) {
            advance = true;
        }
        if (advance) {
            if (i == lineEnd) {
                if (line >= getLineCount() - 1) {
                    return i;
                }
                lineChanged = true;
                line++;
            }
        } else if (i == lineStart) {
            if (line <= 0) {
                return i;
            }
            lineChanged = true;
            line--;
        }
        if (lineChanged) {
            lineStart = getLineStart(line);
            lineEnd = getLineEnd(line);
            newDir = getParagraphDirection(line);
            if (newDir != lineDir) {
                toLeft2 ^= 1;
                lineDir = newDir;
            }
        }
        Directions directions = getLineDirections(line);
        TextLine tl = TextLine.obtain();
        TextLine tl2 = tl;
        tl.set(this.mPaint, this.mText, lineStart, lineEnd, lineDir, directions, false, null);
        newDir = tl2.getOffsetToLeftRightOf(i - lineStart, toLeft2) + lineStart;
        if (newDir > lineEnd) {
            newDir = lineEnd;
        }
        if (newDir < lineStart) {
            newDir = lineStart;
        }
        TextLine.recycle(tl2);
        return newDir;
    }

    private int getOffsetAtStartOf(int offset) {
        int i = 0;
        if (offset == 0) {
            return 0;
        }
        CharSequence text = this.mText;
        char c = text.charAt(offset);
        if (c >= 56320 && c <= 57343) {
            char c1 = text.charAt(offset - 1);
            if (c1 >= 55296 && c1 <= 56319) {
                offset--;
            }
        }
        if (this.mSpannedText) {
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

    public boolean shouldClampCursor(int line) {
        boolean z = true;
        switch (getParagraphAlignment(line)) {
            case ALIGN_LEFT:
                return true;
            case ALIGN_NORMAL:
                if (getParagraphDirection(line) <= 0) {
                    z = false;
                }
                return z;
            default:
                return false;
        }
    }

    public void getCursorPath(int point, Path dest, CharSequence editingBuffer) {
        int i = point;
        Path path = dest;
        CharSequence charSequence = editingBuffer;
        dest.reset();
        int line = getLineForOffset(point);
        int top = getLineTop(line);
        int bottom = getLineBottomWithoutSpacing(line);
        boolean clamped = shouldClampCursor(line);
        float h1 = getPrimaryHorizontal(i, clamped) - 0.5f;
        float h2 = isLevelBoundary(point) ? getSecondaryHorizontal(i, clamped) - 0.5f : h1;
        int caps = MetaKeyKeyListener.getMetaState(charSequence, 1) | MetaKeyKeyListener.getMetaState(charSequence, 2048);
        int fn = MetaKeyKeyListener.getMetaState(charSequence, 2);
        int dist = 0;
        if (!(caps == 0 && fn == 0)) {
            dist = (bottom - top) >> 2;
            if (fn != 0) {
                top += dist;
            }
            if (caps != 0) {
                bottom -= dist;
            }
        }
        if (h1 < 0.5f) {
            h1 = 0.5f;
        }
        if (h2 < 0.5f) {
            h2 = 0.5f;
        }
        if (Float.compare(h1, h2) == 0) {
            path.moveTo(h1, (float) top);
            path.lineTo(h1, (float) bottom);
        } else {
            path.moveTo(h1, (float) top);
            path.lineTo(h1, (float) ((top + bottom) >> 1));
            path.moveTo(h2, (float) ((top + bottom) >> 1));
            path.lineTo(h2, (float) bottom);
        }
        if (caps == 2) {
            path.moveTo(h2, (float) bottom);
            path.lineTo(h2 - ((float) dist), (float) (bottom + dist));
            path.lineTo(h2, (float) bottom);
            path.lineTo(((float) dist) + h2, (float) (bottom + dist));
        } else if (caps == 1) {
            path.moveTo(h2, (float) bottom);
            path.lineTo(h2 - ((float) dist), (float) (bottom + dist));
            path.moveTo(h2 - ((float) dist), ((float) (bottom + dist)) - 0.5f);
            path.lineTo(((float) dist) + h2, ((float) (bottom + dist)) - 0.5f);
            path.moveTo(((float) dist) + h2, (float) (bottom + dist));
            path.lineTo(h2, (float) bottom);
        }
        if (fn == 2) {
            path.moveTo(h1, (float) top);
            path.lineTo(h1 - ((float) dist), (float) (top - dist));
            path.lineTo(h1, (float) top);
            path.lineTo(((float) dist) + h1, (float) (top - dist));
        } else if (fn == 1) {
            path.moveTo(h1, (float) top);
            path.lineTo(h1 - ((float) dist), (float) (top - dist));
            path.moveTo(h1 - ((float) dist), ((float) (top - dist)) + 0.5f);
            path.lineTo(((float) dist) + h1, ((float) (top - dist)) + 0.5f);
            path.moveTo(((float) dist) + h1, (float) (top - dist));
            path.lineTo(h1, (float) top);
        }
    }

    private void addSelection(int line, int start, int end, int top, int bottom, SelectionRectangleConsumer consumer) {
        int i;
        int i2;
        Layout layout = this;
        int i3 = line;
        int i4 = start;
        int i5 = end;
        int linestart = getLineStart(line);
        int lineend = getLineEnd(line);
        Directions dirs = getLineDirections(line);
        if (lineend > linestart && layout.mText.charAt(lineend - 1) == 10) {
            lineend--;
        }
        boolean z = false;
        int i6 = 0;
        while (i6 < dirs.mDirections.length) {
            int here = dirs.mDirections[i6] + linestart;
            int there = (dirs.mDirections[i6 + 1] & RUN_LENGTH_MASK) + here;
            if (there > lineend) {
                there = lineend;
            }
            if (i4 <= there && i5 >= here) {
                int st = Math.max(i4, here);
                int en = Math.min(i5, there);
                if (st != en) {
                    float h1 = layout.getHorizontal(st, z, i3, z);
                    float h2 = layout.getHorizontal(en, true, i3, z);
                    consumer.accept(Math.min(h1, h2), (float) top, Math.max(h1, h2), (float) bottom, (dirs.mDirections[i6 + 1] & 67108864) != 0 ? 0 : 1);
                    i6 += 2;
                    layout = this;
                    i3 = line;
                    z = false;
                }
            }
            i = top;
            i2 = bottom;
            i6 += 2;
            layout = this;
            i3 = line;
            z = false;
        }
        i = top;
        i2 = bottom;
    }

    public void getSelectionPath(int start, int end, Path dest) {
        dest.reset();
        getSelection(start, end, new -$$Lambda$Layout$MzjK2UE2G8VG0asK8_KWY3gHAmY(dest));
    }

    public final void getSelection(int start, int end, SelectionRectangleConsumer consumer) {
        int i = start;
        int end2 = end;
        if (i != end2) {
            if (end2 < i) {
                int temp = end2;
                end2 = i;
                i = temp;
            }
            int start2 = i;
            int end3 = end2;
            int startline = getLineForOffset(start2);
            int endline = getLineForOffset(end3);
            int top = getLineTop(startline);
            int bottom = getLineBottomWithoutSpacing(endline);
            if (startline == endline) {
                addSelection(startline, start2, end3, top, bottom, consumer);
            } else {
                float width = (float) this.mWidth;
                addSelection(startline, start2, getLineEnd(startline), top, getLineBottom(startline), consumer);
                if (getParagraphDirection(startline) == -1) {
                    consumer.accept(getLineLeft(startline), (float) top, 0.0f, (float) getLineBottom(startline), 0);
                } else {
                    consumer.accept(getLineRight(startline), (float) top, width, (float) getLineBottom(startline), 1);
                }
                for (i = startline + 1; i < endline; i++) {
                    top = getLineTop(i);
                    bottom = getLineBottom(i);
                    if (getParagraphDirection(i) == -1) {
                        consumer.accept(0.0f, (float) top, width, (float) bottom, 0);
                    } else {
                        consumer.accept(0.0f, (float) top, width, (float) bottom, 1);
                    }
                }
                top = getLineTop(endline);
                bottom = getLineBottomWithoutSpacing(endline);
                addSelection(endline, getLineStart(endline), end3, top, bottom, consumer);
                if (getParagraphDirection(endline) == -1) {
                    consumer.accept(width, (float) top, getLineRight(endline), (float) bottom, 0);
                } else {
                    consumer.accept(0.0f, (float) top, getLineLeft(endline), (float) bottom, 1);
                }
            }
        }
    }

    public final Alignment getParagraphAlignment(int line) {
        Alignment align = this.mAlignment;
        if (!this.mSpannedText) {
            return align;
        }
        AlignmentSpan[] spans = (AlignmentSpan[]) getParagraphSpans(this.mText, getLineStart(line), getLineEnd(line), AlignmentSpan.class);
        int spanLength = spans.length;
        if (spanLength > 0) {
            return spans[spanLength - 1].getAlignment();
        }
        return align;
    }

    public final int getParagraphLeft(int line) {
        if (getParagraphDirection(line) == -1 || !this.mSpannedText) {
            return 0;
        }
        return getParagraphLeadingMargin(line);
    }

    public final int getParagraphRight(int line) {
        int right = this.mWidth;
        if (getParagraphDirection(line) == 1 || !this.mSpannedText) {
            return right;
        }
        return right - getParagraphLeadingMargin(line);
    }

    private int getParagraphLeadingMargin(int line) {
        int i = 0;
        if (!this.mSpannedText) {
            return 0;
        }
        Spanned spanned = this.mText;
        int lineStart = getLineStart(line);
        LeadingMarginSpan[] spans = (LeadingMarginSpan[]) getParagraphSpans(spanned, lineStart, spanned.nextSpanTransition(lineStart, getLineEnd(line), LeadingMarginSpan.class), LeadingMarginSpan.class);
        if (spans.length == 0) {
            return 0;
        }
        int margin = 0;
        boolean useFirstLineMargin = lineStart == 0 || spanned.charAt(lineStart - 1) == 10;
        boolean useFirstLineMargin2 = useFirstLineMargin;
        for (int i2 = 0; i2 < spans.length; i2++) {
            if (spans[i2] instanceof LeadingMarginSpan2) {
                useFirstLineMargin2 |= line < getLineForOffset(spanned.getSpanStart(spans[i2])) + ((LeadingMarginSpan2) spans[i2]).getLeadingMarginLineCount() ? 1 : 0;
            }
        }
        while (i < spans.length) {
            margin += spans[i].getLeadingMargin(useFirstLineMargin2);
            i++;
        }
        return margin;
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x00d4  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static float measurePara(TextPaint paint, CharSequence text, int start, int end, TextDirectionHeuristic textDir) {
        Throwable th;
        CharSequence charSequence = text;
        int i = start;
        int i2 = end;
        TextLine tl = TextLine.obtain();
        MeasuredParagraph mt;
        try {
            mt = MeasuredParagraph.buildForBidi(charSequence, i, i2, textDir, null);
            try {
                int length;
                boolean hasTabs;
                int margin;
                TabStops tabStops;
                boolean hasTabs2;
                char[] chars = mt.getChars();
                int len = chars.length;
                Directions directions = mt.getDirections(0, len);
                int dir = mt.getParagraphDir();
                boolean hasTabs3 = false;
                TabStops tabStops2 = null;
                if (charSequence instanceof Spanned) {
                    LeadingMarginSpan[] spans = (LeadingMarginSpan[]) getParagraphSpans((Spanned) charSequence, i, i2, LeadingMarginSpan.class);
                    length = spans.length;
                    int margin2 = 0;
                    int margin3 = 0;
                    while (margin3 < length) {
                        margin2 += spans[margin3].getLeadingMargin(true);
                        margin3++;
                        length = length;
                        hasTabs3 = hasTabs3;
                    }
                    hasTabs = hasTabs3;
                    margin = margin2;
                } else {
                    hasTabs = false;
                    margin = 0;
                }
                int i3 = 0;
                while (true) {
                    length = i3;
                    if (length >= len) {
                        tabStops = null;
                        hasTabs2 = hasTabs;
                        break;
                    } else if (chars[length] != 9) {
                        tabStops = null;
                        i3 = length + 1;
                    } else if (charSequence instanceof Spanned) {
                        Spanned spanned = (Spanned) charSequence;
                        TabStopSpan[] chars2 = (TabStopSpan[]) getParagraphSpans(spanned, i, spanned.nextSpanTransition(i, i2, TabStopSpan.class), TabStopSpan.class);
                        hasTabs2 = true;
                        if (chars2.length <= false) {
                            tabStops = null;
                            tabStops2 = new TabStops(20, chars2);
                        } else {
                            tabStops = null;
                        }
                        tabStops = tabStops2;
                    } else {
                        hasTabs2 = true;
                        tabStops = null;
                    }
                }
                int margin4 = margin;
                tl.set(paint, charSequence, i, i2, dir, directions, hasTabs2, tabStops);
                MeasuredParagraph mt2 = ((float) margin4) + Math.abs(tl.metrics(null));
                TextLine.recycle(tl);
                if (mt != null) {
                    mt.recycle();
                }
                return mt2;
            } catch (Throwable th2) {
                th = th2;
                TextLine.recycle(tl);
                if (mt != null) {
                    mt.recycle();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            mt = null;
            TextLine.recycle(tl);
            if (mt != null) {
            }
            throw th;
        }
    }

    static float nextTab(CharSequence text, int start, int end, float h, Object[] tabs) {
        float nh = Float.MAX_VALUE;
        boolean alltabs = false;
        if (text instanceof Spanned) {
            if (tabs == null) {
                tabs = getParagraphSpans((Spanned) text, start, end, TabStopSpan.class);
                alltabs = true;
            }
            int i = 0;
            while (i < tabs.length) {
                if (alltabs || (tabs[i] instanceof TabStopSpan)) {
                    int where = ((TabStopSpan) tabs[i]).getTabStop();
                    if (((float) where) < nh && ((float) where) > h) {
                        nh = (float) where;
                    }
                }
                i++;
            }
            if (nh != Float.MAX_VALUE) {
                return nh;
            }
        }
        return (float) (((int) ((h + 20.0f) / 20.0f)) * 20);
    }

    protected final boolean isSpanned() {
        return this.mSpannedText;
    }

    static <T> T[] getParagraphSpans(Spanned text, int start, int end, Class<T> type) {
        if (start == end && start > 0) {
            return ArrayUtils.emptyArray(type);
        }
        if (text instanceof SpannableStringBuilder) {
            return ((SpannableStringBuilder) text).getSpans(start, end, type, false);
        }
        return text.getSpans(start, end, type);
    }

    private void ellipsize(int start, int end, int line, char[] dest, int destoff, TruncateAt method) {
        int i = start;
        int i2 = line;
        int ellipsisCount = getEllipsisCount(i2);
        if (ellipsisCount != 0) {
            int i3;
            int ellipsisStart = getEllipsisStart(i2);
            int lineStart = getLineStart(i2);
            String ellipsisString = TextUtils.getEllipsisString(method);
            int ellipsisStringLen = ellipsisString.length();
            int i4 = 0;
            boolean useEllipsisString = ellipsisCount >= ellipsisStringLen;
            while (i4 < ellipsisCount) {
                char c;
                if (!useEllipsisString || i4 >= ellipsisStringLen) {
                    c = 65279;
                } else {
                    c = ellipsisString.charAt(i4);
                }
                int a = (i4 + ellipsisStart) + lineStart;
                if (i > a) {
                    i3 = end;
                } else if (a < end) {
                    dest[(destoff + a) - i] = c;
                }
                i4++;
            }
            i3 = end;
        }
    }
}
