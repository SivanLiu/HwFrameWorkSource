package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Paint.Style;
import android.text.Layout.Directions;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;

@VisibleForTesting(visibility = Visibility.PACKAGE)
public class TextLine {
    private static final boolean DEBUG = false;
    private static final int TAB_INCREMENT = 20;
    private static final TextLine[] sCached = new TextLine[3];
    private final TextPaint mActivePaint = new TextPaint();
    private float mAddedWidth;
    private final SpanSet<CharacterStyle> mCharacterStyleSpanSet = new SpanSet(CharacterStyle.class);
    private char[] mChars;
    private boolean mCharsValid;
    private PrecomputedText mComputed;
    private final DecorationInfo mDecorationInfo = new DecorationInfo();
    private final ArrayList<DecorationInfo> mDecorations = new ArrayList();
    private int mDir;
    private Directions mDirections;
    private boolean mHasTabs;
    private int mLen;
    private final SpanSet<MetricAffectingSpan> mMetricAffectingSpanSpanSet = new SpanSet(MetricAffectingSpan.class);
    private TextPaint mPaint;
    private final SpanSet<ReplacementSpan> mReplacementSpanSpanSet = new SpanSet(ReplacementSpan.class);
    private Spanned mSpanned;
    private int mStart;
    private TabStops mTabs;
    private CharSequence mText;
    private final TextPaint mWorkPaint = new TextPaint();

    private static final class DecorationInfo {
        public int end;
        public boolean isStrikeThruText;
        public boolean isUnderlineText;
        public int start;
        public int underlineColor;
        public float underlineThickness;

        private DecorationInfo() {
            this.start = -1;
            this.end = -1;
        }

        public boolean hasDecoration() {
            return this.isStrikeThruText || this.isUnderlineText || this.underlineColor != 0;
        }

        public DecorationInfo copyInfo() {
            DecorationInfo copy = new DecorationInfo();
            copy.isStrikeThruText = this.isStrikeThruText;
            copy.isUnderlineText = this.isUnderlineText;
            copy.underlineColor = this.underlineColor;
            copy.underlineThickness = this.underlineThickness;
            return copy;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public static TextLine obtain() {
        synchronized (sCached) {
            int i = sCached.length;
            do {
                i--;
                if (i < 0) {
                    return new TextLine();
                }
            } while (sCached[i] == null);
            TextLine tl = sCached[i];
            sCached[i] = null;
            return tl;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public static TextLine recycle(TextLine tl) {
        tl.mText = null;
        tl.mPaint = null;
        tl.mDirections = null;
        tl.mSpanned = null;
        tl.mTabs = null;
        tl.mChars = null;
        tl.mComputed = null;
        tl.mMetricAffectingSpanSpanSet.recycle();
        tl.mCharacterStyleSpanSet.recycle();
        tl.mReplacementSpanSpanSet.recycle();
        synchronized (sCached) {
            for (int i = 0; i < sCached.length; i++) {
                if (sCached[i] == null) {
                    sCached[i] = tl;
                    break;
                }
            }
        }
        return null;
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void set(TextPaint paint, CharSequence text, int start, int limit, int dir, Directions directions, boolean hasTabs, TabStops tabStops) {
        TextPaint textPaint = paint;
        CharSequence charSequence = text;
        int i = start;
        int i2 = limit;
        Directions directions2 = directions;
        boolean z = hasTabs;
        this.mPaint = textPaint;
        this.mText = charSequence;
        this.mStart = i;
        this.mLen = i2 - i;
        this.mDir = dir;
        this.mDirections = directions2;
        if (this.mDirections != null) {
            this.mHasTabs = z;
            this.mSpanned = null;
            boolean hasReplacement = false;
            if (charSequence instanceof Spanned) {
                this.mSpanned = (Spanned) charSequence;
                this.mReplacementSpanSpanSet.init(this.mSpanned, i, i2);
                hasReplacement = this.mReplacementSpanSpanSet.numberOfSpans > 0;
            }
            this.mComputed = null;
            if (charSequence instanceof PrecomputedText) {
                this.mComputed = (PrecomputedText) charSequence;
                if (!this.mComputed.getParams().getTextPaint().equalsForTextMeasurement(textPaint)) {
                    this.mComputed = null;
                }
            }
            boolean z2 = hasReplacement || z || directions2 != Layout.DIRS_ALL_LEFT_TO_RIGHT;
            this.mCharsValid = z2;
            if (this.mCharsValid) {
                if (this.mChars == null || this.mChars.length < this.mLen) {
                    this.mChars = ArrayUtils.newUnpaddedCharArray(this.mLen);
                }
                TextUtils.getChars(charSequence, i, i2, this.mChars, 0);
                if (hasReplacement) {
                    char[] chars = this.mChars;
                    int i3 = i;
                    while (i3 < i2) {
                        int inext = this.mReplacementSpanSpanSet.getNextTransition(i3, i2);
                        if (this.mReplacementSpanSpanSet.hasSpansIntersecting(i3, inext)) {
                            chars[i3 - i] = 65532;
                            int e = inext - i;
                            for (int j = (i3 - i) + 1; j < e; j++) {
                                chars[j] = 65279;
                            }
                        }
                        i3 = inext;
                    }
                }
            }
            this.mTabs = tabStops;
            this.mAddedWidth = 0.0f;
            return;
        }
        TabStops tabStops2 = tabStops;
        throw new IllegalArgumentException("Directions cannot be null");
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void justify(float justifyWidth) {
        int end = this.mLen;
        while (end > 0 && isLineEndSpace(this.mText.charAt((this.mStart + end) - 1))) {
            end--;
        }
        int spaces = countStretchableSpaces(0, end);
        if (spaces != 0) {
            this.mAddedWidth = (justifyWidth - Math.abs(measure(end, false, null))) / ((float) spaces);
        }
    }

    void draw(Canvas c, float x, int top, int y, int bottom) {
        if (!this.mHasTabs) {
            if (this.mDirections == Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                drawRun(c, 0, this.mLen, false, x, top, y, bottom, false);
                return;
            } else if (this.mDirections == Layout.DIRS_ALL_RIGHT_TO_LEFT) {
                drawRun(c, 0, this.mLen, true, x, top, y, bottom, false);
                return;
            }
        }
        int[] runs = this.mDirections.mDirections;
        int lastRunIndex = runs.length - 2;
        float h = 0.0f;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < runs.length) {
                int runStart = runs[i2];
                i = (runs[i2 + 1] & 67108863) + runStart;
                if (i > this.mLen) {
                    i = this.mLen;
                }
                int runLimit = i;
                boolean runIsRtl = (runs[i2 + 1] & 67108864) != 0;
                i = runStart;
                int j = this.mHasTabs ? runStart : runLimit;
                int segstart = i;
                float h2 = h;
                while (true) {
                    int j2 = j;
                    if (j2 > runLimit) {
                        break;
                    }
                    int runLimit2;
                    i = 0;
                    if (this.mHasTabs && j2 < runLimit) {
                        i = this.mChars[j2];
                        if (i >= 55296 && i < 56320 && j2 + 1 < runLimit) {
                            i = Character.codePointAt(this.mChars, j2);
                            if (i > 65535) {
                                j2++;
                                runLimit2 = runLimit;
                                j = j2 + 1;
                                runLimit = runLimit2;
                            }
                        }
                    }
                    int codept = i;
                    if (j2 == runLimit || codept == 9) {
                        float f = x + h2;
                        int i3 = (i2 == lastRunIndex && j2 == this.mLen) ? 0 : 1;
                        int i4 = 9;
                        int j3 = j2;
                        runLimit2 = runLimit;
                        h2 += drawRun(c, segstart, j2, runIsRtl, f, top, y, bottom, i3);
                        if (codept == 9) {
                            h2 = ((float) this.mDir) * nextTab(((float) this.mDir) * h2);
                        }
                        segstart = j3 + 1;
                        j2 = j3;
                        j = j2 + 1;
                        runLimit = runLimit2;
                    }
                    runLimit2 = runLimit;
                    j = j2 + 1;
                    runLimit = runLimit2;
                }
                i = i2 + 2;
                h = h2;
            } else {
                return;
            }
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public float metrics(FontMetricsInt fmi) {
        return measure(this.mLen, false, fmi);
    }

    float measure(int offset, boolean trailing, FontMetricsInt fmi) {
        int i = offset;
        int target = trailing ? i - 1 : i;
        if (target < 0) {
            return 0.0f;
        }
        float h = 0.0f;
        if (!this.mHasTabs) {
            if (this.mDirections == Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                return measureRun(0, i, this.mLen, false, fmi);
            } else if (this.mDirections == Layout.DIRS_ALL_RIGHT_TO_LEFT) {
                return measureRun(0, i, this.mLen, true, fmi);
            }
        }
        char[] chars = this.mChars;
        int[] runs = this.mDirections.mDirections;
        int i2 = 0;
        while (true) {
            float h2;
            int i3 = i2;
            if (i3 < runs.length) {
                int runStart = runs[i3];
                i2 = (runs[i3 + 1] & 67108863) + runStart;
                if (i2 > this.mLen) {
                    i2 = this.mLen;
                }
                int runLimit = i2;
                boolean runIsRtl = (runs[i3 + 1] & 67108864) != 0;
                i2 = runStart;
                int j = this.mHasTabs ? runStart : runLimit;
                h2 = h;
                h = i2;
                while (true) {
                    int j2 = j;
                    if (j2 > runLimit) {
                        break;
                    }
                    boolean runIsRtl2;
                    int runLimit2;
                    i2 = 0;
                    if (this.mHasTabs && j2 < runLimit) {
                        i2 = chars[j2];
                        if (i2 >= 55296 && i2 < 56320 && j2 + 1 < runLimit) {
                            i2 = Character.codePointAt(chars, j2);
                            if (i2 > 65535) {
                                j2++;
                                runIsRtl2 = runIsRtl;
                                runLimit2 = runLimit;
                                j = j2 + 1;
                                runIsRtl = runIsRtl2;
                                runLimit = runLimit2;
                            }
                        }
                    }
                    int codept = i2;
                    if (j2 == runLimit || codept == 9) {
                        boolean z = target >= h && target < j2;
                        boolean inSegment = z;
                        boolean advance = (this.mDir == -1) == runIsRtl;
                        if (inSegment && advance) {
                            runIsRtl2 = runIsRtl;
                            runLimit2 = runLimit;
                            return h2 + measureRun(h, i, j2, runIsRtl, fmi);
                        }
                        int j3 = j2;
                        runIsRtl2 = runIsRtl;
                        runLimit2 = runLimit;
                        int i4 = 9;
                        i4 = codept;
                        float w = measureRun(h, j3, j3, runIsRtl2, fmi);
                        h2 += advance ? w : -w;
                        if (inSegment) {
                            return h2 + measureRun(h, i, j3, runIsRtl2, null);
                        }
                        if (i4 == 9) {
                            i2 = j3;
                            if (i == i2) {
                                return h2;
                            }
                            float h3 = ((float) this.mDir) * nextTab(((float) this.mDir) * h2);
                            if (target == i2) {
                                return h3;
                            }
                            h2 = h3;
                        } else {
                            i2 = j3;
                        }
                        h = i2 + 1;
                        j2 = i2;
                        j = j2 + 1;
                        runIsRtl = runIsRtl2;
                        runLimit = runLimit2;
                    }
                    runIsRtl2 = runIsRtl;
                    runLimit2 = runLimit;
                    j = j2 + 1;
                    runIsRtl = runIsRtl2;
                    runLimit = runLimit2;
                }
            } else {
                return h;
            }
            i2 = i3 + 2;
            h = h2;
        }
    }

    float[] measureAllOffsets(boolean[] trailing, FontMetricsInt fmi) {
        int offset;
        int i = 1;
        float[] measurement = new float[(this.mLen + 1)];
        int[] target = new int[(this.mLen + 1)];
        boolean offset2 = false;
        for (offset = 0; offset < target.length; offset++) {
            target[offset] = trailing[offset] ? offset - 1 : offset;
        }
        if (target[0] < 0) {
            measurement[0] = 0.0f;
        }
        float h = 0.0f;
        if (!this.mHasTabs) {
            int offset3;
            if (this.mDirections == Layout.DIRS_ALL_LEFT_TO_RIGHT) {
                while (true) {
                    i = offset3;
                    if (i > this.mLen) {
                        return measurement;
                    }
                    measurement[i] = measureRun(0, i, this.mLen, false, fmi);
                    offset3 = i + 1;
                }
            } else if (this.mDirections == Layout.DIRS_ALL_RIGHT_TO_LEFT) {
                while (true) {
                    i = offset3;
                    if (i > this.mLen) {
                        return measurement;
                    }
                    measurement[i] = measureRun(0, i, this.mLen, true, fmi);
                    offset3 = i + 1;
                }
            }
        }
        char[] chars = this.mChars;
        int[] runs = this.mDirections.mDirections;
        offset = 0;
        while (true) {
            int i2 = offset;
            if (i2 >= runs.length) {
                break;
            }
            int runStart = runs[i2];
            offset = (runs[i2 + 1] & 67108863) + runStart;
            if (offset > this.mLen) {
                offset = this.mLen;
            }
            int runLimit = offset;
            boolean w = (runs[i2 + 1] & 67108864) != 0 ? i : offset2;
            offset = runStart;
            int j = this.mHasTabs ? runStart : runLimit;
            float h2 = h;
            int segstart = offset;
            while (true) {
                int j2 = j;
                if (j2 > runLimit) {
                    break;
                }
                boolean runIsRtl;
                int runLimit2;
                offset = 0;
                if (this.mHasTabs && j2 < runLimit) {
                    offset = chars[j2];
                    if (offset >= 55296 && offset < 56320 && j2 + 1 < runLimit) {
                        offset = Character.codePointAt(chars, j2);
                        if (offset > 65535) {
                            j2++;
                            runIsRtl = w;
                            runLimit2 = runLimit;
                            j = j2 + 1;
                            i = 1;
                            w = runIsRtl;
                            runLimit = runLimit2;
                            offset2 = false;
                        }
                    }
                }
                int codept = offset;
                if (j2 == runLimit || codept == 9) {
                    int j3;
                    float f;
                    float oldh = h2;
                    boolean advance = (this.mDir == -1 ? i : offset2) == w ? i : offset2;
                    i = codept;
                    int j4 = j2;
                    runIsRtl = w;
                    runLimit2 = runLimit;
                    float w2 = measureRun(segstart, j2, j2, w, fmi);
                    h2 += advance ? w2 : -w2;
                    float baseh = advance ? oldh : h2;
                    FontMetricsInt crtfmi = advance ? fmi : null;
                    offset = segstart;
                    while (true) {
                        j2 = offset;
                        codept = j4;
                        if (j2 > codept || j2 > this.mLen) {
                            j3 = codept;
                            f = w2;
                        } else {
                            if (target[j2] < segstart || target[j2] >= codept) {
                                j3 = codept;
                                j4 = j2;
                                f = w2;
                            } else {
                                j3 = codept;
                                j4 = j2;
                                f = w2;
                                measurement[j4] = baseh + measureRun(segstart, j2, j3, runIsRtl, crtfmi);
                            }
                            offset = j4 + 1;
                            j4 = j3;
                            w2 = f;
                        }
                    }
                    j3 = codept;
                    f = w2;
                    if (i == 9) {
                        offset = j3;
                        if (target[offset] == offset) {
                            measurement[offset] = h2;
                        }
                        float h3 = ((float) this.mDir) * nextTab(((float) this.mDir) * h2);
                        if (target[offset + 1] == offset) {
                            measurement[offset + 1] = h3;
                        }
                        h2 = h3;
                    } else {
                        offset = j3;
                    }
                    segstart = offset + 1;
                    j2 = offset;
                    j = j2 + 1;
                    i = 1;
                    w = runIsRtl;
                    runLimit = runLimit2;
                    offset2 = false;
                }
                runIsRtl = w;
                runLimit2 = runLimit;
                j = j2 + 1;
                i = 1;
                w = runIsRtl;
                runLimit = runLimit2;
                offset2 = false;
            }
            boolean z = i;
            offset = i2 + 2;
            h = h2;
            offset2 = false;
        }
        if (target[this.mLen] == this.mLen) {
            measurement[this.mLen] = h;
        }
        return measurement;
    }

    private float drawRun(Canvas c, int start, int limit, boolean runIsRtl, float x, int top, int y, int bottom, boolean needWidth) {
        boolean z = true;
        if (this.mDir != 1) {
            z = false;
        }
        boolean z2 = runIsRtl;
        if (z != z2) {
            return handleRun(start, limit, limit, z2, c, x, top, y, bottom, null, needWidth);
        }
        int i = start;
        int i2 = limit;
        int i3 = limit;
        boolean z3 = z2;
        float w = -measureRun(i, i2, i3, z3, null);
        handleRun(i, i2, i3, z3, c, x + w, top, y, bottom, null, false);
        return w;
    }

    private float measureRun(int start, int offset, int limit, boolean runIsRtl, FontMetricsInt fmi) {
        return handleRun(start, offset, limit, runIsRtl, null, 0.0f, 0, 0, 0, fmi, true);
    }

    /* JADX WARNING: Removed duplicated region for block: B:70:0x0100  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00fe  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00f2  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00b4  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00fe  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0100  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x0175 A:{SYNTHETIC, SKIP, EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  , EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  , EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x010a  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0100  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x00fe  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x0103  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x010a  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x0175 A:{SYNTHETIC, SKIP, EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  , EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  , EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  , EDGE_INSN: B:125:0x0175->B:106:0x0175 ?: BREAK  } */
    /* JADX WARNING: Missing block: B:107:0x0178, code skipped:
            if (r13 != -1) goto L_0x0183;
     */
    /* JADX WARNING: Missing block: B:108:0x017a, code skipped:
            if (r0 == false) goto L_0x0181;
     */
    /* JADX WARNING: Missing block: B:109:0x017c, code skipped:
            r1 = r7.mLen + 1;
     */
    /* JADX WARNING: Missing block: B:110:0x0181, code skipped:
            r13 = r1;
     */
    /* JADX WARNING: Missing block: B:111:0x0183, code skipped:
            if (r13 > r11) goto L_0x018b;
     */
    /* JADX WARNING: Missing block: B:112:0x0185, code skipped:
            if (r0 == false) goto L_0x0189;
     */
    /* JADX WARNING: Missing block: B:113:0x0187, code skipped:
            r1 = r11;
     */
    /* JADX WARNING: Missing block: B:114:0x0189, code skipped:
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:115:0x018a, code skipped:
            r13 = r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int getOffsetToLeftRightOf(int cursor, boolean toLeft) {
        int runIndex;
        boolean runLevel;
        int prevRunStart;
        int prevRunLimit;
        boolean z;
        int[] runs;
        int i;
        boolean runLevel2;
        int otherRunIndex;
        boolean otherRunLevel;
        int otherRunIndex2;
        int i2 = cursor;
        boolean z2 = toLeft;
        int lineEnd = this.mLen;
        boolean paraIsRtl = this.mDir == -1;
        int[] runs2 = this.mDirections.mDirections;
        int runStart = 0;
        int runLimit = lineEnd;
        boolean trailing = false;
        if (i2 == 0) {
            runIndex = -2;
        } else if (i2 == lineEnd) {
            runIndex = runs2.length;
        } else {
            int prevRunIndex;
            int prevRunIndex2;
            boolean runLevel3;
            boolean trailing2;
            int i3;
            runIndex = runLimit;
            runLimit = runStart;
            runStart = 0;
            while (runStart < runs2.length) {
                int runStart2;
                runLimit = 0 + runs2[runStart];
                if (i2 >= runLimit) {
                    int runLimit2 = (runs2[runStart + 1] & 67108863) + runLimit;
                    if (runLimit2 > lineEnd) {
                        runLimit2 = lineEnd;
                    }
                    if (i2 < runLimit2) {
                        runLevel = (runs2[runStart + 1] >>> 26) & 63;
                        if (i2 == runLimit) {
                            runIndex = i2 - 1;
                            prevRunIndex = 0;
                            while (true) {
                                prevRunIndex2 = prevRunIndex;
                                if (prevRunIndex2 >= runs2.length) {
                                    prevRunIndex = runLimit;
                                    break;
                                }
                                prevRunStart = runs2[prevRunIndex2] + 0;
                                if (runIndex >= prevRunStart) {
                                    prevRunLimit = prevRunStart + (runs2[prevRunIndex2 + 1] & 67108863);
                                    if (prevRunLimit > lineEnd) {
                                        prevRunLimit = lineEnd;
                                    }
                                    if (runIndex < prevRunLimit) {
                                        runStart2 = runLimit;
                                        boolean runStart3 = (runs2[prevRunIndex2 + 1] >>> 26) & 63;
                                        if (runStart3 < runLevel) {
                                            runStart = prevRunIndex2;
                                            runLevel = runStart3;
                                            prevRunIndex = prevRunStart;
                                            runLimit2 = prevRunLimit;
                                            trailing = true;
                                            break;
                                        }
                                        prevRunIndex = prevRunIndex2 + 2;
                                        runLimit = runStart2;
                                    }
                                }
                                runStart2 = runLimit;
                                prevRunIndex = prevRunIndex2 + 2;
                                runLimit = runStart2;
                            }
                            runLevel3 = runLevel;
                            prevRunLimit = runStart;
                            prevRunIndex2 = runLimit2;
                            trailing2 = trailing;
                        } else {
                            runLevel3 = runLevel;
                            prevRunLimit = runStart;
                            prevRunIndex2 = runLimit2;
                            prevRunIndex = runLimit;
                            trailing2 = false;
                        }
                        if (prevRunLimit == runs2.length) {
                            boolean z3 = (runLevel3 & 1) != 0;
                            trailing = z2 == z3;
                            if (i2 == (trailing ? prevRunIndex2 : prevRunIndex) && trailing == trailing2) {
                                z = trailing2;
                                runs = runs2;
                            } else {
                                boolean advance = trailing;
                                boolean runIsRtl = z3;
                                z = trailing2;
                                runLimit2 = i2;
                                runs = runs2;
                                int newCaret = getOffsetBeforeAfter(prevRunLimit, prevRunIndex, prevRunIndex2, z3, runLimit2, advance);
                                if (newCaret != (advance ? prevRunIndex2 : prevRunIndex)) {
                                    return newCaret;
                                }
                                i = prevRunIndex2;
                                runLevel2 = runLevel3;
                                prevRunStart = newCaret;
                                while (true) {
                                    runLevel = z2 != paraIsRtl;
                                    otherRunIndex = prevRunLimit + (runLevel ? 2 : -2);
                                    if (otherRunIndex < 0 || otherRunIndex >= runs.length) {
                                        runStart = -1;
                                    } else {
                                        int otherRunStart = 0 + runs[otherRunIndex];
                                        runStart = otherRunStart + (runs[otherRunIndex + 1] & 67108863);
                                        if (runStart > lineEnd) {
                                            runStart = lineEnd;
                                        }
                                        runStart2 = runStart;
                                        trailing2 = (runs[otherRunIndex + 1] >>> 26) & 63;
                                        z3 = (trailing2 & 1) != 0;
                                        boolean advance2 = z2 == z3;
                                        if (prevRunStart == -1) {
                                            otherRunLevel = trailing2;
                                            otherRunIndex2 = otherRunIndex;
                                            prevRunStart = getOffsetBeforeAfter(otherRunIndex, otherRunStart, runStart2, z3, advance2 ? otherRunStart : runStart2, advance2);
                                            if (prevRunStart != (advance2 ? runStart2 : otherRunStart)) {
                                                break;
                                            }
                                            prevRunLimit = otherRunIndex2;
                                            runLevel2 = otherRunLevel;
                                        } else {
                                            otherRunIndex2 = otherRunIndex;
                                            if (trailing2 < runLevel2) {
                                                prevRunStart = advance2 ? otherRunStart : runStart2;
                                            }
                                        }
                                    }
                                }
                                return prevRunStart;
                            }
                        }
                        runs = runs2;
                        i3 = prevRunIndex2;
                        runLevel2 = runLevel3;
                        prevRunStart = -1;
                        i = i3;
                        while (true) {
                            if (z2 != paraIsRtl) {
                            }
                            if (runLevel) {
                            }
                            otherRunIndex = prevRunLimit + (runLevel ? 2 : -2);
                            if (otherRunIndex < 0) {
                                break;
                            }
                            break;
                            prevRunLimit = otherRunIndex2;
                            runLevel2 = otherRunLevel;
                        }
                        return prevRunStart;
                    }
                    runStart2 = runLimit;
                    runIndex = runLimit2;
                } else {
                    runStart2 = runLimit;
                }
                runStart += 2;
                runLimit = runStart2;
            }
            runLevel3 = false;
            prevRunLimit = runStart;
            prevRunIndex = runLimit;
            trailing2 = false;
            prevRunIndex2 = runIndex;
            if (prevRunLimit == runs2.length) {
            }
            i3 = prevRunIndex2;
            runLevel2 = runLevel3;
            prevRunStart = -1;
            i = i3;
            while (true) {
                if (z2 != paraIsRtl) {
                }
                if (runLevel) {
                }
                otherRunIndex = prevRunLimit + (runLevel ? 2 : -2);
                if (otherRunIndex < 0) {
                }
                prevRunLimit = otherRunIndex2;
                runLevel2 = otherRunLevel;
            }
            return prevRunStart;
        }
        runLevel2 = false;
        z = false;
        prevRunLimit = runIndex;
        runs = runs2;
        prevRunStart = -1;
        i = runLimit;
        while (true) {
            if (z2 != paraIsRtl) {
            }
            if (runLevel) {
            }
            otherRunIndex = prevRunLimit + (runLevel ? 2 : -2);
            if (otherRunIndex < 0) {
            }
            prevRunLimit = otherRunIndex2;
            runLevel2 = otherRunLevel;
        }
        return prevRunStart;
    }

    private int getOffsetBeforeAfter(int runIndex, int runStart, int runLimit, boolean runIsRtl, int offset, boolean after) {
        int i = offset;
        if (runIndex >= 0) {
            int i2 = 0;
            if (i != (after ? this.mLen : 0)) {
                int spanStart;
                int spanLimit;
                TextPaint wp = this.mWorkPaint;
                wp.set(this.mPaint);
                wp.setWordSpacing(this.mAddedWidth);
                int spanStart2 = runStart;
                if (this.mSpanned == null) {
                    spanStart = spanStart2;
                    spanLimit = runLimit;
                } else {
                    int spanLimit2;
                    int limit = this.mStart + runLimit;
                    while (true) {
                        spanLimit2 = this.mSpanned.nextSpanTransition(this.mStart + spanStart2, limit, MetricAffectingSpan.class) - this.mStart;
                        if (spanLimit2 >= (after ? i + 1 : i)) {
                            break;
                        }
                        spanStart2 = spanLimit2;
                    }
                    MetricAffectingSpan[] spans = (MetricAffectingSpan[]) TextUtils.removeEmptySpans((MetricAffectingSpan[]) this.mSpanned.getSpans(this.mStart + spanStart2, this.mStart + spanLimit2, MetricAffectingSpan.class), this.mSpanned, MetricAffectingSpan.class);
                    if (spans.length > 0) {
                        ReplacementSpan replacement = null;
                        for (MetricAffectingSpan span : spans) {
                            if (span instanceof ReplacementSpan) {
                                replacement = (ReplacementSpan) span;
                            } else {
                                span.updateMeasureState(wp);
                            }
                        }
                        if (replacement != null) {
                            return after ? spanLimit2 : spanStart2;
                        }
                    }
                    spanStart = spanStart2;
                    spanLimit = spanLimit2;
                }
                boolean spanLimit3 = runIsRtl;
                if (!after) {
                    i2 = 2;
                }
                int j = i2;
                if (this.mCharsValid) {
                    return wp.getTextRunCursor(this.mChars, spanStart, spanLimit - spanStart, spanLimit3, i, j);
                }
                return wp.getTextRunCursor(this.mText, this.mStart + spanStart, this.mStart + spanLimit, spanLimit3, this.mStart + i, j) - this.mStart;
            }
        }
        if (after) {
            return TextUtils.getOffsetAfter(this.mText, this.mStart + i) - this.mStart;
        }
        return TextUtils.getOffsetBefore(this.mText, this.mStart + i) - this.mStart;
    }

    private static void expandMetricsFromPaint(FontMetricsInt fmi, TextPaint wp) {
        int previousTop = fmi.top;
        int previousAscent = fmi.ascent;
        int previousDescent = fmi.descent;
        int previousBottom = fmi.bottom;
        int previousLeading = fmi.leading;
        wp.getFontMetricsInt(fmi);
        updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom, previousLeading);
    }

    static void updateMetrics(FontMetricsInt fmi, int previousTop, int previousAscent, int previousDescent, int previousBottom, int previousLeading) {
        fmi.top = Math.min(fmi.top, previousTop);
        fmi.ascent = Math.min(fmi.ascent, previousAscent);
        fmi.descent = Math.max(fmi.descent, previousDescent);
        fmi.bottom = Math.max(fmi.bottom, previousBottom);
        fmi.leading = Math.max(fmi.leading, previousLeading);
    }

    private static void drawStroke(TextPaint wp, Canvas c, int color, float position, float thickness, float xleft, float xright, float baseline) {
        Paint paint = wp;
        float strokeTop = (baseline + ((float) paint.baselineShift)) + position;
        int previousColor = paint.getColor();
        Style previousStyle = paint.getStyle();
        boolean previousAntiAlias = paint.isAntiAlias();
        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(color);
        c.drawRect(xleft, strokeTop, xright, strokeTop + thickness, paint);
        paint.setStyle(previousStyle);
        paint.setColor(previousColor);
        paint.setAntiAlias(previousAntiAlias);
    }

    private float getRunAdvance(TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, int offset) {
        if (this.mCharsValid) {
            return wp.getRunAdvance(this.mChars, start, end, contextStart, contextEnd, runIsRtl, offset);
        }
        int delta = this.mStart;
        if (this.mComputed != null) {
            return this.mComputed.getWidth(start + delta, end + delta);
        }
        return wp.getRunAdvance(this.mText, delta + start, delta + end, delta + contextStart, delta + contextEnd, runIsRtl, delta + offset);
    }

    private float handleText(TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, FontMetricsInt fmi, boolean needWidth, int offset, ArrayList<DecorationInfo> decorations) {
        Paint paint = wp;
        int i = start;
        int i2 = y;
        FontMetricsInt fontMetricsInt = fmi;
        ArrayList arrayList = decorations;
        TextLine textLine = this;
        paint.setWordSpacing(textLine.mAddedWidth);
        if (fontMetricsInt != null) {
            expandMetricsFromPaint(fontMetricsInt, paint);
        }
        int i3 = end;
        if (i3 == i) {
            return 0.0f;
        }
        int numDecorations;
        float totalWidth;
        float totalWidth2 = 0.0f;
        int i4 = 0;
        int numDecorations2 = arrayList == null ? 0 : decorations.size();
        if (needWidth || !(c == null || (paint.bgColor == 0 && numDecorations2 == 0 && !runIsRtl))) {
            numDecorations = numDecorations2;
            totalWidth2 = textLine.getRunAdvance(paint, i, i3, contextStart, contextEnd, runIsRtl, offset);
        } else {
            numDecorations = numDecorations2;
        }
        int numDecorations3;
        if (c != null) {
            float leftX;
            float rightX;
            if (runIsRtl) {
                leftX = x - totalWidth2;
                rightX = x;
            } else {
                leftX = x;
                rightX = x + totalWidth2;
            }
            float leftX2 = leftX;
            float rightX2 = rightX;
            if (paint.bgColor != 0) {
                int previousColor = wp.getColor();
                Style previousStyle = wp.getStyle();
                paint.setColor(paint.bgColor);
                paint.setStyle(Style.FILL);
                c.drawRect(leftX2, (float) top, rightX2, (float) bottom, paint);
                paint.setStyle(previousStyle);
                paint.setColor(previousColor);
            }
            if (numDecorations != 0) {
                while (true) {
                    numDecorations2 = i4;
                    if (numDecorations2 >= numDecorations) {
                        break;
                    }
                    float totalWidth3;
                    DecorationInfo info;
                    DecorationInfo info2 = (DecorationInfo) arrayList.get(numDecorations2);
                    i4 = Math.max(info2.start, i);
                    int decorationEnd = Math.min(info2.end, offset);
                    TextLine textLine2 = textLine;
                    Paint paint2 = paint;
                    int i5 = i;
                    int i6 = i3;
                    int i7 = contextStart;
                    int i8 = contextEnd;
                    int numDecorations4 = numDecorations;
                    DecorationInfo info3 = info2;
                    boolean decorationXRight = runIsRtl;
                    int i9 = numDecorations2;
                    float decorationStartAdvance = textLine2.getRunAdvance(paint2, i5, i6, i7, i8, decorationXRight, i4);
                    float decorationEndAdvance = textLine2.getRunAdvance(paint2, i5, i6, i7, i8, decorationXRight, decorationEnd);
                    if (runIsRtl) {
                        leftX = rightX2 - decorationEndAdvance;
                        rightX = rightX2 - decorationStartAdvance;
                    } else {
                        leftX = leftX2 + decorationStartAdvance;
                        rightX = leftX2 + decorationEndAdvance;
                    }
                    float decorationXLeft = leftX;
                    float decorationXRight2 = rightX;
                    if (info3.underlineColor != 0) {
                        drawStroke(paint, c, info3.underlineColor, wp.getUnderlinePosition(), info3.underlineThickness, decorationXLeft, decorationXRight2, (float) i2);
                    }
                    if (info3.isUnderlineText) {
                        totalWidth3 = totalWidth2;
                        info = info3;
                        numDecorations3 = numDecorations4;
                        i6 = i2;
                        drawStroke(paint, c, wp.getColor(), wp.getUnderlinePosition(), Math.max(wp.getUnderlineThickness(), 1.0f), decorationXLeft, decorationXRight2, (float) i2);
                    } else {
                        totalWidth3 = totalWidth2;
                        info = info3;
                        i6 = i2;
                        numDecorations3 = numDecorations4;
                    }
                    if (info.isStrikeThruText) {
                        drawStroke(paint, c, wp.getColor(), wp.getStrikeThruPosition(), Math.max(wp.getStrikeThruThickness(), 1.0f), decorationXLeft, decorationXRight2, (float) i6);
                    }
                    i4 = i9 + 1;
                    textLine = this;
                    i = start;
                    i3 = end;
                    ArrayList<DecorationInfo> arrayList2 = decorations;
                    i2 = i6;
                    totalWidth2 = totalWidth3;
                    numDecorations = numDecorations3;
                }
            }
            numDecorations3 = numDecorations;
            totalWidth = totalWidth2;
            drawTextRun(c, paint, start, end, contextStart, contextEnd, runIsRtl, leftX2, i2 + paint.baselineShift);
        } else {
            totalWidth = totalWidth2;
            numDecorations3 = numDecorations;
        }
        return runIsRtl ? -totalWidth : totalWidth;
    }

    private float handleReplacement(ReplacementSpan replacement, TextPaint wp, int start, int limit, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, FontMetricsInt fmi, boolean needWidth) {
        float ret;
        FontMetricsInt fontMetricsInt = fmi;
        float ret2 = 0.0f;
        int textStart = this.mStart + start;
        int textLimit = this.mStart + limit;
        if (needWidth || (c != null && runIsRtl)) {
            int previousTop = 0;
            int previousAscent = 0;
            int previousDescent = 0;
            int previousBottom = 0;
            int previousLeading = 0;
            boolean needUpdateMetrics = fontMetricsInt != null;
            if (needUpdateMetrics) {
                previousTop = fontMetricsInt.top;
                previousAscent = fontMetricsInt.ascent;
                previousDescent = fontMetricsInt.descent;
                previousBottom = fontMetricsInt.bottom;
                previousLeading = fontMetricsInt.leading;
            }
            int previousTop2 = previousTop;
            int ret3 = previousAscent;
            int previousDescent2 = previousDescent;
            int previousBottom2 = previousBottom;
            int previousLeading2 = previousLeading;
            ret2 = (float) replacement.getSize(wp, this.mText, textStart, textLimit, fontMetricsInt);
            if (needUpdateMetrics) {
                updateMetrics(fontMetricsInt, previousTop2, ret3, previousDescent2, previousBottom2, previousLeading2);
            }
        }
        float ret4 = ret2;
        float f;
        if (c != null) {
            float x2;
            if (runIsRtl) {
                x2 = x - ret4;
            } else {
                x2 = x;
            }
            ret = ret4;
            replacement.draw(c, this.mText, textStart, textLimit, x2, top, y, bottom, wp);
            f = x2;
        } else {
            ret = ret4;
            f = x;
        }
        return runIsRtl ? -ret : ret;
    }

    private int adjustHyphenEdit(int start, int limit, int hyphenEdit) {
        int result = hyphenEdit;
        if (start > 0) {
            result &= -25;
        }
        if (limit < this.mLen) {
            return result & -8;
        }
        return result;
    }

    private void extractDecorationInfo(TextPaint paint, DecorationInfo info) {
        info.isStrikeThruText = paint.isStrikeThruText();
        if (info.isStrikeThruText) {
            paint.setStrikeThruText(false);
        }
        info.isUnderlineText = paint.isUnderlineText();
        if (info.isUnderlineText) {
            paint.setUnderlineText(false);
        }
        info.underlineColor = paint.underlineColor;
        info.underlineThickness = paint.underlineThickness;
        paint.setUnderlineText(0, 0.0f);
    }

    /* JADX WARNING: Removed duplicated region for block: B:73:0x024d  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x023b  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x023b  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x024d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private float handleRun(int start, int measureLimit, int limit, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, FontMetricsInt fmi, boolean needWidth) {
        int i = start;
        int i2 = measureLimit;
        int i3 = limit;
        FontMetricsInt fontMetricsInt = fmi;
        if (i2 < i || i2 > i3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("measureLimit (");
            stringBuilder.append(measureLimit);
            stringBuilder.append(") is out of start (");
            stringBuilder.append(start);
            stringBuilder.append(") and limit (");
            stringBuilder.append(limit);
            stringBuilder.append(") bounds");
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (i == i2) {
            TextPaint wp = this.mWorkPaint;
            wp.set(this.mPaint);
            if (fontMetricsInt != null) {
                expandMetricsFromPaint(fontMetricsInt, wp);
            }
            return 0.0f;
        } else {
            boolean needsSpanMeasurement;
            if (this.mSpanned == null) {
                needsSpanMeasurement = false;
            } else {
                this.mMetricAffectingSpanSpanSet.init(this.mSpanned, this.mStart + i, this.mStart + i3);
                this.mCharacterStyleSpanSet.init(this.mSpanned, this.mStart + i, this.mStart + i3);
                needsSpanMeasurement = (this.mMetricAffectingSpanSpanSet.numberOfSpans == 0 && this.mCharacterStyleSpanSet.numberOfSpans == 0) ? false : true;
            }
            if (needsSpanMeasurement) {
                float originalX = x;
                float x2 = x;
                int i4 = start;
                while (true) {
                    int i5 = i4;
                    i = measureLimit;
                    if (i5 >= i) {
                        return x2 - originalX;
                    }
                    TextLine thisR;
                    int inext;
                    TextPaint wp2 = this.mWorkPaint;
                    wp2.set(this.mPaint);
                    int inext2 = this.mMetricAffectingSpanSpanSet.getNextTransition(this.mStart + i5, this.mStart + limit) - this.mStart;
                    int mlimit = Math.min(inext2, i);
                    ReplacementSpan replacement = null;
                    i4 = 0;
                    while (i4 < this.mMetricAffectingSpanSpanSet.numberOfSpans) {
                        if (this.mMetricAffectingSpanSpanSet.spanStarts[i4] < this.mStart + mlimit && this.mMetricAffectingSpanSpanSet.spanEnds[i4] > this.mStart + i5) {
                            MetricAffectingSpan span = ((MetricAffectingSpan[]) this.mMetricAffectingSpanSpanSet.spans)[i4];
                            if (span instanceof ReplacementSpan) {
                                replacement = (ReplacementSpan) span;
                            } else {
                                span.updateDrawState(wp2);
                            }
                        }
                        i4++;
                    }
                    int i6;
                    if (replacement != null) {
                        boolean z = needWidth || mlimit < i;
                        inext = inext2;
                        x2 += handleReplacement(replacement, wp2, i5, mlimit, runIsRtl, c, x2, top, y, bottom, fmi, z);
                        i6 = i5;
                    } else {
                        int mlimit2;
                        int i7;
                        int i8;
                        TextPaint wp3;
                        TextLine textLine;
                        TextPaint activePaint;
                        boolean z2;
                        boolean z3;
                        int mlimit3 = mlimit;
                        inext = inext2;
                        TextPaint wp4 = wp2;
                        TextLine activePaint2 = this;
                        TextPaint activePaint3 = activePaint2.mActivePaint;
                        activePaint3.set(activePaint2.mPaint);
                        i4 = i5;
                        int activeEnd = mlimit3;
                        DecorationInfo decorationInfo = activePaint2.mDecorationInfo;
                        activePaint2.mDecorations.clear();
                        int activeEnd2 = activeEnd;
                        float x3 = x2;
                        i2 = i4;
                        while (true) {
                            inext2 = i4;
                            mlimit = mlimit3;
                            if (inext2 >= mlimit) {
                                break;
                            }
                            int jnext;
                            DecorationInfo decorationInfo2;
                            int jnext2;
                            TextPaint textPaint;
                            int jnext3 = activePaint2.mCharacterStyleSpanSet.getNextTransition(activePaint2.mStart + inext2, activePaint2.mStart + inext) - activePaint2.mStart;
                            mlimit3 = Math.min(jnext3, mlimit);
                            TextPaint wp5 = wp4;
                            wp5.set(activePaint2.mPaint);
                            i4 = 0;
                            while (i4 < activePaint2.mCharacterStyleSpanSet.numberOfSpans) {
                                if (activePaint2.mCharacterStyleSpanSet.spanStarts[i4] < activePaint2.mStart + mlimit3 && activePaint2.mCharacterStyleSpanSet.spanEnds[i4] > activePaint2.mStart + inext2) {
                                    ((CharacterStyle[]) activePaint2.mCharacterStyleSpanSet.spans)[i4].updateDrawState(wp5);
                                }
                                i4++;
                            }
                            activePaint2.extractDecorationInfo(wp5, decorationInfo);
                            int j;
                            if (inext2 == i5) {
                                activePaint3.set(wp5);
                                jnext = jnext3;
                                mlimit2 = mlimit;
                                j = inext2;
                                i7 = activeEnd2;
                                decorationInfo2 = decorationInfo;
                                i8 = i2;
                                i6 = i5;
                                wp3 = wp5;
                                textLine = activePaint2;
                                activePaint = activePaint3;
                                thisR = textLine;
                            } else if (wp5.hasEqualAttributes(activePaint3)) {
                                jnext = jnext3;
                                mlimit2 = mlimit;
                                j = inext2;
                                i7 = activeEnd2;
                                decorationInfo2 = decorationInfo;
                                i8 = i2;
                                i6 = i5;
                                wp3 = wp5;
                                textLine = activePaint2;
                                activePaint = activePaint3;
                                thisR = textLine;
                            } else {
                                TextPaint wp6;
                                boolean z4;
                                activePaint3.setHyphenEdit(activePaint2.adjustHyphenEdit(i2, activeEnd2, activePaint2.mPaint.getHyphenEdit()));
                                if (needWidth) {
                                    wp6 = wp5;
                                    int i9 = measureLimit;
                                } else {
                                    wp6 = wp5;
                                    if (activeEnd2 >= measureLimit) {
                                        z4 = false;
                                        jnext = jnext3;
                                        mlimit2 = mlimit;
                                        j = inext2;
                                        decorationInfo2 = decorationInfo;
                                        i6 = i5;
                                        x3 += activePaint2.handleText(activePaint3, i2, activeEnd2, i5, inext, runIsRtl, c, x3, top, y, bottom, fmi, z4, Math.min(activeEnd2, mlimit), activePaint2.mDecorations);
                                        i2 = j;
                                        wp3 = wp6;
                                        activePaint = activePaint3;
                                        activePaint.set(wp3);
                                        thisR = this;
                                        thisR.mDecorations.clear();
                                        activeEnd2 = jnext;
                                        decorationInfo = decorationInfo2;
                                        if (decorationInfo.hasDecoration()) {
                                            DecorationInfo copy = decorationInfo.copyInfo();
                                            copy.start = j;
                                            jnext2 = jnext;
                                            copy.end = jnext2;
                                            thisR.mDecorations.add(copy);
                                        } else {
                                            jnext2 = jnext;
                                            activeEnd = j;
                                        }
                                        i4 = jnext2;
                                        wp4 = wp3;
                                        i5 = i6;
                                        mlimit3 = mlimit2;
                                        textPaint = activePaint;
                                        activePaint2 = thisR;
                                        activePaint3 = textPaint;
                                    }
                                }
                                z4 = true;
                                jnext = jnext3;
                                mlimit2 = mlimit;
                                j = inext2;
                                decorationInfo2 = decorationInfo;
                                i6 = i5;
                                x3 += activePaint2.handleText(activePaint3, i2, activeEnd2, i5, inext, runIsRtl, c, x3, top, y, bottom, fmi, z4, Math.min(activeEnd2, mlimit), activePaint2.mDecorations);
                                i2 = j;
                                wp3 = wp6;
                                activePaint = activePaint3;
                                activePaint.set(wp3);
                                thisR = this;
                                thisR.mDecorations.clear();
                                activeEnd2 = jnext;
                                decorationInfo = decorationInfo2;
                                if (decorationInfo.hasDecoration()) {
                                }
                                i4 = jnext2;
                                wp4 = wp3;
                                i5 = i6;
                                mlimit3 = mlimit2;
                                textPaint = activePaint;
                                activePaint2 = thisR;
                                activePaint3 = textPaint;
                            }
                            i2 = i8;
                            activeEnd2 = jnext;
                            decorationInfo = decorationInfo2;
                            if (decorationInfo.hasDecoration()) {
                            }
                            i4 = jnext2;
                            wp4 = wp3;
                            i5 = i6;
                            mlimit3 = mlimit2;
                            textPaint = activePaint;
                            activePaint2 = thisR;
                            activePaint3 = textPaint;
                        }
                        mlimit2 = mlimit;
                        i7 = activeEnd2;
                        i8 = i2;
                        i6 = i5;
                        wp3 = wp4;
                        textLine = activePaint2;
                        activePaint = activePaint3;
                        thisR = textLine;
                        activePaint.setHyphenEdit(thisR.adjustHyphenEdit(i2, activeEnd2, thisR.mPaint.getHyphenEdit()));
                        if (needWidth) {
                            inext2 = measureLimit;
                        } else if (activeEnd2 >= measureLimit) {
                            z2 = false;
                            mlimit = mlimit2;
                            z3 = z2;
                            wp4 = wp3;
                            x2 = x3 + thisR.handleText(activePaint, i2, activeEnd2, i6, inext, runIsRtl, c, x3, top, y, bottom, fmi, z3, Math.min(activeEnd2, mlimit), thisR.mDecorations);
                        }
                        z2 = true;
                        mlimit = mlimit2;
                        z3 = z2;
                        wp4 = wp3;
                        x2 = x3 + thisR.handleText(activePaint, i2, activeEnd2, i6, inext, runIsRtl, c, x3, top, y, bottom, fmi, z3, Math.min(activeEnd2, mlimit), thisR.mDecorations);
                    }
                    i4 = inext;
                }
            } else {
                TextPaint wp7 = this.mWorkPaint;
                wp7.set(this.mPaint);
                wp7.setHyphenEdit(adjustHyphenEdit(i, i3, wp7.getHyphenEdit()));
                return handleText(wp7, i, i3, i, i3, runIsRtl, c, x, top, y, bottom, fmi, needWidth, measureLimit, null);
            }
        }
    }

    private void drawTextRun(Canvas c, TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, float x, int y) {
        int i = y;
        if (this.mCharsValid) {
            c.drawTextRun(this.mChars, start, end - start, contextStart, contextEnd - contextStart, x, (float) i, runIsRtl, wp);
            return;
        }
        int delta = this.mStart;
        c.drawTextRun(this.mText, delta + start, delta + end, delta + contextStart, delta + contextEnd, x, (float) i, runIsRtl, wp);
    }

    float nextTab(float h) {
        if (this.mTabs != null) {
            return this.mTabs.nextTab(h);
        }
        return TabStops.nextDefaultStop(h, 20);
    }

    private boolean isStretchableWhitespace(int ch) {
        return ch == 32;
    }

    private int countStretchableSpaces(int start, int end) {
        int count = 0;
        int i = start;
        while (i < end) {
            if (isStretchableWhitespace(this.mCharsValid ? this.mChars[i] : this.mText.charAt(this.mStart + i))) {
                count++;
            }
            i++;
        }
        return count;
    }

    public static boolean isLineEndSpace(char ch) {
        return ch == ' ' || ch == 9 || ch == 5760 || ((8192 <= ch && ch <= 8202 && ch != 8199) || ch == 8287 || ch == 12288);
    }
}
