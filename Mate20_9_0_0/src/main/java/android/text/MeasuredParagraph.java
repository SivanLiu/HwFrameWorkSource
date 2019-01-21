package android.text;

import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.os.Trace;
import android.text.AutoGrowArray.ByteArray;
import android.text.AutoGrowArray.FloatArray;
import android.text.AutoGrowArray.IntArray;
import android.text.Layout.Directions;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.util.Pools.SynchronizedPool;
import java.util.Arrays;
import libcore.util.NativeAllocationRegistry;

public class MeasuredParagraph {
    private static final char OBJECT_REPLACEMENT_CHARACTER = '￼';
    private static final SynchronizedPool<MeasuredParagraph> sPool = new SynchronizedPool(1);
    private static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(MeasuredParagraph.class.getClassLoader(), nGetReleaseFunc(), Trace.TRACE_TAG_CAMERA);
    private FontMetricsInt mCachedFm;
    private TextPaint mCachedPaint = new TextPaint();
    private char[] mCopiedBuffer;
    private IntArray mFontMetrics = new IntArray(16);
    private ByteArray mLevels = new ByteArray();
    private boolean mLtrWithoutBidi;
    private Runnable mNativeObjectCleaner;
    private long mNativePtr = 0;
    private int mParaDir;
    private IntArray mSpanEndCache = new IntArray(4);
    private Spanned mSpanned;
    private int mTextLength;
    private int mTextStart;
    private float mWholeWidth;
    private FloatArray mWidths = new FloatArray();

    private static native void nAddReplacementRun(long j, long j2, int i, int i2, float f);

    private static native void nAddStyleRun(long j, long j2, int i, int i2, boolean z);

    private static native long nBuildNativeMeasuredParagraph(long j, char[] cArr, boolean z, boolean z2);

    private static native void nFreeBuilder(long j);

    private static native void nGetBounds(long j, char[] cArr, int i, int i2, Rect rect);

    private static native int nGetMemoryUsage(long j);

    private static native long nGetReleaseFunc();

    private static native float nGetWidth(long j, int i, int i2);

    private static native long nInitBuilder();

    private MeasuredParagraph() {
    }

    private static MeasuredParagraph obtain() {
        MeasuredParagraph mt = (MeasuredParagraph) sPool.acquire();
        return mt != null ? mt : new MeasuredParagraph();
    }

    public void recycle() {
        release();
        sPool.release(this);
    }

    private void bindNativeObject(long nativePtr) {
        this.mNativePtr = nativePtr;
        this.mNativeObjectCleaner = sRegistry.registerNativeAllocation(this, nativePtr);
    }

    private void unbindNativeObject() {
        if (this.mNativePtr != 0) {
            this.mNativeObjectCleaner.run();
            this.mNativePtr = 0;
        }
    }

    public void release() {
        reset();
        this.mLevels.clearWithReleasingLargeArray();
        this.mWidths.clearWithReleasingLargeArray();
        this.mFontMetrics.clearWithReleasingLargeArray();
        this.mSpanEndCache.clearWithReleasingLargeArray();
    }

    private void reset() {
        this.mSpanned = null;
        this.mCopiedBuffer = null;
        this.mWholeWidth = 0.0f;
        this.mLevels.clear();
        this.mWidths.clear();
        this.mFontMetrics.clear();
        this.mSpanEndCache.clear();
        unbindNativeObject();
    }

    public int getTextLength() {
        return this.mTextLength;
    }

    public char[] getChars() {
        return this.mCopiedBuffer;
    }

    public int getParagraphDir() {
        return this.mParaDir;
    }

    public Directions getDirections(int start, int end) {
        if (this.mLtrWithoutBidi) {
            return Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
        return AndroidBidi.directions(this.mParaDir, this.mLevels.getRawArray(), start, this.mCopiedBuffer, start, end - start);
    }

    public float getWholeWidth() {
        return this.mWholeWidth;
    }

    public FloatArray getWidths() {
        return this.mWidths;
    }

    public IntArray getSpanEndCache() {
        return this.mSpanEndCache;
    }

    public IntArray getFontMetrics() {
        return this.mFontMetrics;
    }

    public long getNativePtr() {
        return this.mNativePtr;
    }

    public float getWidth(int start, int end) {
        if (this.mNativePtr != 0) {
            return nGetWidth(this.mNativePtr, start, end);
        }
        float[] widths = this.mWidths.getRawArray();
        float r = 0.0f;
        for (int i = start; i < end; i++) {
            r += widths[i];
        }
        return r;
    }

    public void getBounds(int start, int end, Rect bounds) {
        nGetBounds(this.mNativePtr, this.mCopiedBuffer, start, end, bounds);
    }

    public static MeasuredParagraph buildForBidi(CharSequence text, int start, int end, TextDirectionHeuristic textDir, MeasuredParagraph recycle) {
        MeasuredParagraph mt = recycle == null ? obtain() : recycle;
        mt.resetAndAnalyzeBidi(text, start, end, textDir);
        return mt;
    }

    public static MeasuredParagraph buildForMeasurement(TextPaint paint, CharSequence text, int start, int end, TextDirectionHeuristic textDir, MeasuredParagraph recycle) {
        int i = end;
        MeasuredParagraph mt = recycle == null ? obtain() : recycle;
        int i2 = start;
        mt.resetAndAnalyzeBidi(text, i2, i, textDir);
        mt.mWidths.resize(mt.mTextLength);
        if (mt.mTextLength == 0) {
            return mt;
        }
        MeasuredParagraph mt2;
        if (mt.mSpanned == null) {
            mt.applyMetricsAffectingSpan(paint, null, i2, i, 0);
            mt2 = mt;
        } else {
            int spanStart = i2;
            while (spanStart < i) {
                int spanEnd = mt.mSpanned.nextSpanTransition(spanStart, i, MetricAffectingSpan.class);
                mt2 = mt;
                mt.applyMetricsAffectingSpan(paint, (MetricAffectingSpan[]) TextUtils.removeEmptySpans((MetricAffectingSpan[]) mt.mSpanned.getSpans(spanStart, spanEnd, MetricAffectingSpan.class), mt.mSpanned, MetricAffectingSpan.class), spanStart, spanEnd, 0);
                spanStart = spanEnd;
                CharSequence charSequence = text;
                i2 = start;
                TextDirectionHeuristic textDirectionHeuristic = textDir;
                mt = mt2;
            }
            mt2 = mt;
        }
        return mt2;
    }

    public static MeasuredParagraph buildForStaticLayout(TextPaint paint, CharSequence text, int start, int end, TextDirectionHeuristic textDir, boolean computeHyphenation, boolean computeLayout, MeasuredParagraph recycle) {
        long nativeBuilderPtr;
        Throwable th;
        int i = end;
        boolean z = computeHyphenation;
        boolean z2 = computeLayout;
        MeasuredParagraph mt = recycle == null ? obtain() : recycle;
        int i2 = start;
        mt.resetAndAnalyzeBidi(text, i2, i, textDir);
        long nativeBuilderPtr2;
        if (mt.mTextLength == 0) {
            nativeBuilderPtr2 = nInitBuilder();
            try {
                mt.bindNativeObject(nBuildNativeMeasuredParagraph(nativeBuilderPtr2, mt.mCopiedBuffer, z, z2));
                return mt;
            } finally {
                nFreeBuilder(nativeBuilderPtr2);
            }
        } else {
            long nativeBuilderPtr3 = nInitBuilder();
            try {
                if (mt.mSpanned == null) {
                    nativeBuilderPtr = nativeBuilderPtr3;
                    try {
                        mt.applyMetricsAffectingSpan(paint, null, i2, i, nativeBuilderPtr3);
                        mt.mSpanEndCache.append(i);
                    } catch (Throwable th2) {
                        th = th2;
                        nativeBuilderPtr2 = nativeBuilderPtr;
                        nFreeBuilder(nativeBuilderPtr2);
                        throw th;
                    }
                }
                nativeBuilderPtr = nativeBuilderPtr3;
                int spanStart = i2;
                while (spanStart < i) {
                    int spanEnd = mt.mSpanned.nextSpanTransition(spanStart, i, MetricAffectingSpan.class);
                    spanStart = spanEnd;
                    mt.applyMetricsAffectingSpan(paint, (MetricAffectingSpan[]) TextUtils.removeEmptySpans((MetricAffectingSpan[]) mt.mSpanned.getSpans(spanStart, spanEnd, MetricAffectingSpan.class), mt.mSpanned, MetricAffectingSpan.class), spanStart, spanEnd, nativeBuilderPtr);
                    mt.mSpanEndCache.append(spanStart);
                }
                try {
                    nativeBuilderPtr2 = nativeBuilderPtr;
                } catch (Throwable th3) {
                    th = th3;
                    nativeBuilderPtr2 = nativeBuilderPtr;
                    nFreeBuilder(nativeBuilderPtr2);
                    throw th;
                }
                try {
                    mt.bindNativeObject(nBuildNativeMeasuredParagraph(nativeBuilderPtr2, mt.mCopiedBuffer, z, z2));
                    nFreeBuilder(nativeBuilderPtr2);
                    return mt;
                } catch (Throwable th4) {
                    th = th4;
                    nFreeBuilder(nativeBuilderPtr2);
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                nativeBuilderPtr2 = nativeBuilderPtr3;
                nFreeBuilder(nativeBuilderPtr2);
                throw th;
            }
        }
    }

    private void resetAndAnalyzeBidi(CharSequence text, int start, int end, TextDirectionHeuristic textDir) {
        int i;
        reset();
        this.mSpanned = text instanceof Spanned ? (Spanned) text : null;
        this.mTextStart = start;
        this.mTextLength = end - start;
        if (this.mCopiedBuffer == null || this.mCopiedBuffer.length != this.mTextLength) {
            this.mCopiedBuffer = new char[this.mTextLength];
        }
        TextUtils.getChars(text, start, end, this.mCopiedBuffer, 0);
        if (this.mSpanned != null) {
            ReplacementSpan[] spans = (ReplacementSpan[]) this.mSpanned.getSpans(start, end, ReplacementSpan.class);
            for (i = 0; i < spans.length; i++) {
                int startInPara = this.mSpanned.getSpanStart(spans[i]) - start;
                int endInPara = this.mSpanned.getSpanEnd(spans[i]) - start;
                if (startInPara < 0) {
                    startInPara = 0;
                }
                if (endInPara > this.mTextLength) {
                    endInPara = this.mTextLength;
                }
                Arrays.fill(this.mCopiedBuffer, startInPara, endInPara, OBJECT_REPLACEMENT_CHARACTER);
            }
        }
        i = 1;
        if ((textDir == TextDirectionHeuristics.LTR || textDir == TextDirectionHeuristics.FIRSTSTRONG_LTR || textDir == TextDirectionHeuristics.ANYRTL_LTR) && TextUtils.doesNotNeedBidi(this.mCopiedBuffer, 0, this.mTextLength)) {
            this.mLevels.clear();
            this.mParaDir = 1;
            this.mLtrWithoutBidi = true;
            return;
        }
        int bidiRequest;
        if (textDir == TextDirectionHeuristics.LTR) {
            bidiRequest = 1;
        } else if (textDir == TextDirectionHeuristics.RTL) {
            bidiRequest = -1;
        } else if (textDir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
            bidiRequest = 2;
        } else if (textDir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
            bidiRequest = -2;
        } else {
            if (textDir.isRtl(this.mCopiedBuffer, 0, this.mTextLength)) {
                i = -1;
            }
            bidiRequest = i;
        }
        this.mLevels.resize(this.mTextLength);
        this.mParaDir = AndroidBidi.bidi(bidiRequest, this.mCopiedBuffer, this.mLevels.getRawArray());
        this.mLtrWithoutBidi = false;
    }

    private void applyReplacementRun(ReplacementSpan replacement, int start, int end, long nativeBuilderPtr) {
        float width = (float) replacement.getSize(this.mCachedPaint, this.mSpanned, start + this.mTextStart, end + this.mTextStart, this.mCachedFm);
        if (nativeBuilderPtr == 0) {
            this.mWidths.set(start, width);
            if (end > start + 1) {
                Arrays.fill(this.mWidths.getRawArray(), start + 1, end, 0.0f);
            }
            this.mWholeWidth += width;
            return;
        }
        nAddReplacementRun(nativeBuilderPtr, this.mCachedPaint.getNativeInstance(), start, end, width);
    }

    private void applyStyleRun(int start, int end, long nativeBuilderPtr) {
        int i = start;
        int i2 = end;
        if (!this.mLtrWithoutBidi) {
            int levelEnd = i + 1;
            int level = this.mLevels.get(i);
            int levelStart = i;
            while (true) {
                int levelEnd2;
                int levelEnd3 = levelEnd;
                if (levelEnd3 == i2 || this.mLevels.get(levelEnd3) != level) {
                    boolean isRtl = (level & 1) != 0;
                    if (nativeBuilderPtr == 0) {
                        int levelLength = levelEnd3 - levelStart;
                        this.mWholeWidth += this.mCachedPaint.getTextRunAdvances(this.mCopiedBuffer, levelStart, levelLength, levelStart, levelLength, isRtl, this.mWidths.getRawArray(), levelStart);
                        levelEnd2 = levelEnd3;
                    } else {
                        levelEnd2 = levelEnd3;
                        nAddStyleRun(nativeBuilderPtr, this.mCachedPaint.getNativeInstance(), levelStart, levelEnd3, isRtl);
                    }
                    if (levelEnd2 != i2) {
                        levelStart = levelEnd2;
                        level = this.mLevels.get(levelEnd2);
                    } else {
                        return;
                    }
                }
                levelEnd2 = levelEnd3;
                levelEnd = levelEnd2 + 1;
            }
        } else if (nativeBuilderPtr == 0) {
            this.mWholeWidth += this.mCachedPaint.getTextRunAdvances(this.mCopiedBuffer, i, i2 - i, i, i2 - i, false, this.mWidths.getRawArray(), i);
        } else {
            nAddStyleRun(nativeBuilderPtr, this.mCachedPaint.getNativeInstance(), i, i2, false);
        }
    }

    private void applyMetricsAffectingSpan(TextPaint paint, MetricAffectingSpan[] spans, int start, int end, long nativeBuilderPtr) {
        MetricAffectingSpan[] metricAffectingSpanArr = spans;
        long j = nativeBuilderPtr;
        this.mCachedPaint.set(paint);
        int i = 0;
        this.mCachedPaint.baselineShift = 0;
        boolean needFontMetrics = j != 0;
        if (needFontMetrics && this.mCachedFm == null) {
            this.mCachedFm = new FontMetricsInt();
        }
        ReplacementSpan replacement = null;
        if (metricAffectingSpanArr != null) {
            while (i < metricAffectingSpanArr.length) {
                MetricAffectingSpan span = metricAffectingSpanArr[i];
                if (span instanceof ReplacementSpan) {
                    replacement = (ReplacementSpan) span;
                } else {
                    span.updateMeasureState(this.mCachedPaint);
                }
                i++;
            }
        }
        ReplacementSpan replacement2 = replacement;
        int startInCopiedBuffer = start - this.mTextStart;
        int endInCopiedBuffer = end - this.mTextStart;
        if (j != 0) {
            this.mCachedPaint.getFontMetricsInt(this.mCachedFm);
        }
        if (replacement2 != null) {
            applyReplacementRun(replacement2, startInCopiedBuffer, endInCopiedBuffer, j);
        } else {
            applyStyleRun(startInCopiedBuffer, endInCopiedBuffer, j);
        }
        if (needFontMetrics) {
            FontMetricsInt fontMetricsInt;
            if (this.mCachedPaint.baselineShift < 0) {
                fontMetricsInt = this.mCachedFm;
                fontMetricsInt.ascent += this.mCachedPaint.baselineShift;
                fontMetricsInt = this.mCachedFm;
                fontMetricsInt.top += this.mCachedPaint.baselineShift;
            } else {
                fontMetricsInt = this.mCachedFm;
                fontMetricsInt.descent += this.mCachedPaint.baselineShift;
                fontMetricsInt = this.mCachedFm;
                fontMetricsInt.bottom += this.mCachedPaint.baselineShift;
            }
            this.mFontMetrics.append(this.mCachedFm.top);
            this.mFontMetrics.append(this.mCachedFm.bottom);
            this.mFontMetrics.append(this.mCachedFm.ascent);
            this.mFontMetrics.append(this.mCachedFm.descent);
        }
    }

    int breakText(int limit, boolean forwards, float width) {
        float[] w = this.mWidths.getRawArray();
        int i;
        if (forwards) {
            i = 0;
            while (i < limit) {
                width -= w[i];
                if (width < 0.0f) {
                    break;
                }
                i++;
            }
            while (i > 0 && this.mCopiedBuffer[i - 1] == ' ') {
                i--;
            }
            return i;
        }
        i = limit - 1;
        while (i >= 0) {
            width -= w[i];
            if (width < 0.0f) {
                break;
            }
            i--;
        }
        while (i < limit - 1 && (this.mCopiedBuffer[i + 1] == ' ' || w[i + 1] == 0.0f)) {
            i++;
        }
        return (limit - i) - 1;
    }

    float measure(int start, int limit) {
        float[] w = this.mWidths.getRawArray();
        float width = 0.0f;
        for (int i = start; i < limit; i++) {
            width += w[i];
        }
        return width;
    }

    public int getMemoryUsage() {
        return nGetMemoryUsage(this.mNativePtr);
    }
}
