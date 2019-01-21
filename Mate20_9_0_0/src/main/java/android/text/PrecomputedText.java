package android.text;

import android.graphics.Rect;
import android.text.style.MetricAffectingSpan;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Objects;

public class PrecomputedText implements Spannable {
    private static final char LINE_FEED = '\n';
    private final int mEnd;
    private final ParagraphInfo[] mParagraphInfo;
    private final Params mParams;
    private final int mStart;
    private final SpannableString mText;

    public static class ParagraphInfo {
        public final MeasuredParagraph measured;
        public final int paragraphEnd;

        public ParagraphInfo(int paraEnd, MeasuredParagraph measured) {
            this.paragraphEnd = paraEnd;
            this.measured = measured;
        }
    }

    public static final class Params {
        private final int mBreakStrategy;
        private final int mHyphenationFrequency;
        private final TextPaint mPaint;
        private final TextDirectionHeuristic mTextDir;

        public static class Builder {
            private int mBreakStrategy = 1;
            private int mHyphenationFrequency = 1;
            private final TextPaint mPaint;
            private TextDirectionHeuristic mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;

            public Builder(TextPaint paint) {
                this.mPaint = paint;
            }

            public Builder setBreakStrategy(int strategy) {
                this.mBreakStrategy = strategy;
                return this;
            }

            public Builder setHyphenationFrequency(int frequency) {
                this.mHyphenationFrequency = frequency;
                return this;
            }

            public Builder setTextDirection(TextDirectionHeuristic textDir) {
                this.mTextDir = textDir;
                return this;
            }

            public Params build() {
                return new Params(this.mPaint, this.mTextDir, this.mBreakStrategy, this.mHyphenationFrequency);
            }
        }

        public Params(TextPaint paint, TextDirectionHeuristic textDir, int strategy, int frequency) {
            this.mPaint = paint;
            this.mTextDir = textDir;
            this.mBreakStrategy = strategy;
            this.mHyphenationFrequency = frequency;
        }

        public TextPaint getTextPaint() {
            return this.mPaint;
        }

        public TextDirectionHeuristic getTextDirection() {
            return this.mTextDir;
        }

        public int getBreakStrategy() {
            return this.mBreakStrategy;
        }

        public int getHyphenationFrequency() {
            return this.mHyphenationFrequency;
        }

        public boolean isSameTextMetricsInternal(TextPaint paint, TextDirectionHeuristic textDir, int strategy, int frequency) {
            return this.mTextDir == textDir && this.mBreakStrategy == strategy && this.mHyphenationFrequency == frequency && this.mPaint.equalsForTextMeasurement(paint);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || !(o instanceof Params)) {
                return false;
            }
            Params param = (Params) o;
            return isSameTextMetricsInternal(param.mPaint, param.mTextDir, param.mBreakStrategy, param.mHyphenationFrequency);
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Float.valueOf(this.mPaint.getLetterSpacing()), Float.valueOf(this.mPaint.getWordSpacing()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocales(), this.mPaint.getTypeface(), this.mPaint.getFontVariationSettings(), Boolean.valueOf(this.mPaint.isElegantTextHeight()), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency)});
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{textSize=");
            stringBuilder.append(this.mPaint.getTextSize());
            stringBuilder.append(", textScaleX=");
            stringBuilder.append(this.mPaint.getTextScaleX());
            stringBuilder.append(", textSkewX=");
            stringBuilder.append(this.mPaint.getTextSkewX());
            stringBuilder.append(", letterSpacing=");
            stringBuilder.append(this.mPaint.getLetterSpacing());
            stringBuilder.append(", textLocale=");
            stringBuilder.append(this.mPaint.getTextLocales());
            stringBuilder.append(", typeface=");
            stringBuilder.append(this.mPaint.getTypeface());
            stringBuilder.append(", variationSettings=");
            stringBuilder.append(this.mPaint.getFontVariationSettings());
            stringBuilder.append(", elegantTextHeight=");
            stringBuilder.append(this.mPaint.isElegantTextHeight());
            stringBuilder.append(", textDir=");
            stringBuilder.append(this.mTextDir);
            stringBuilder.append(", breakStrategy=");
            stringBuilder.append(this.mBreakStrategy);
            stringBuilder.append(", hyphenationFrequency=");
            stringBuilder.append(this.mHyphenationFrequency);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public static PrecomputedText create(CharSequence text, Params params) {
        return new PrecomputedText(text, 0, text.length(), params, createMeasuredParagraphs(text, params, 0, text.length(), true));
    }

    public static ParagraphInfo[] createMeasuredParagraphs(CharSequence text, Params params, int start, int end, boolean computeLayout) {
        CharSequence charSequence;
        int i = end;
        ArrayList<ParagraphInfo> result = new ArrayList();
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(params);
        boolean z = (params.getBreakStrategy() == 0 || params.getHyphenationFrequency() == 0) ? false : true;
        boolean needHyphenation = z;
        int paraEnd = 0;
        int paraStart = start;
        while (paraStart < i) {
            charSequence = text;
            paraEnd = TextUtils.indexOf(charSequence, (char) LINE_FEED, paraStart, i);
            if (paraEnd < 0) {
                paraEnd = i;
            } else {
                paraEnd++;
            }
            int paraEnd2 = paraEnd;
            result.add(new ParagraphInfo(paraEnd2, MeasuredParagraph.buildForStaticLayout(params.getTextPaint(), charSequence, paraStart, paraEnd2, params.getTextDirection(), needHyphenation, computeLayout, null)));
            paraStart = paraEnd2;
            paraEnd = paraEnd2;
        }
        charSequence = text;
        return (ParagraphInfo[]) result.toArray(new ParagraphInfo[result.size()]);
    }

    private PrecomputedText(CharSequence text, int start, int end, Params params, ParagraphInfo[] paraInfo) {
        this.mText = new SpannableString(text, true);
        this.mStart = start;
        this.mEnd = end;
        this.mParams = params;
        this.mParagraphInfo = paraInfo;
    }

    public CharSequence getText() {
        return this.mText;
    }

    public int getStart() {
        return this.mStart;
    }

    public int getEnd() {
        return this.mEnd;
    }

    public Params getParams() {
        return this.mParams;
    }

    public int getParagraphCount() {
        return this.mParagraphInfo.length;
    }

    public int getParagraphStart(int paraIndex) {
        Preconditions.checkArgumentInRange(paraIndex, 0, getParagraphCount(), "paraIndex");
        return paraIndex == 0 ? this.mStart : getParagraphEnd(paraIndex - 1);
    }

    public int getParagraphEnd(int paraIndex) {
        Preconditions.checkArgumentInRange(paraIndex, 0, getParagraphCount(), "paraIndex");
        return this.mParagraphInfo[paraIndex].paragraphEnd;
    }

    public MeasuredParagraph getMeasuredParagraph(int paraIndex) {
        return this.mParagraphInfo[paraIndex].measured;
    }

    public ParagraphInfo[] getParagraphInfo() {
        return this.mParagraphInfo;
    }

    public boolean canUseMeasuredResult(int start, int end, TextDirectionHeuristic textDir, TextPaint paint, int strategy, int frequency) {
        TextPaint mtPaint = this.mParams.getTextPaint();
        return this.mStart == start && this.mEnd == end && this.mParams.isSameTextMetricsInternal(paint, textDir, strategy, frequency);
    }

    public int findParaIndex(int pos) {
        for (int i = 0; i < this.mParagraphInfo.length; i++) {
            if (pos < this.mParagraphInfo[i].paragraphEnd) {
                return i;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pos must be less than ");
        stringBuilder.append(this.mParagraphInfo[this.mParagraphInfo.length - 1].paragraphEnd);
        stringBuilder.append(", gave ");
        stringBuilder.append(pos);
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    public float getWidth(int start, int end) {
        boolean z = false;
        boolean z2 = start >= 0 && start <= this.mText.length();
        Preconditions.checkArgument(z2, "invalid start offset");
        z2 = end >= 0 && end <= this.mText.length();
        Preconditions.checkArgument(z2, "invalid end offset");
        if (start <= end) {
            z = true;
        }
        Preconditions.checkArgument(z, "start offset can not be larger than end offset");
        if (start == end) {
            return 0.0f;
        }
        int paraIndex = findParaIndex(start);
        int paraStart = getParagraphStart(paraIndex);
        int paraEnd = getParagraphEnd(paraIndex);
        if (start >= paraStart && paraEnd >= end) {
            return getMeasuredParagraph(paraIndex).getWidth(start - paraStart, end - paraStart);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot measured across the paragraph:para: (");
        stringBuilder.append(paraStart);
        stringBuilder.append(", ");
        stringBuilder.append(paraEnd);
        stringBuilder.append("), request: (");
        stringBuilder.append(start);
        stringBuilder.append(", ");
        stringBuilder.append(end);
        stringBuilder.append(")");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void getBounds(int start, int end, Rect bounds) {
        boolean z = true;
        boolean z2 = start >= 0 && start <= this.mText.length();
        Preconditions.checkArgument(z2, "invalid start offset");
        z2 = end >= 0 && end <= this.mText.length();
        Preconditions.checkArgument(z2, "invalid end offset");
        if (start > end) {
            z = false;
        }
        Preconditions.checkArgument(z, "start offset can not be larger than end offset");
        Preconditions.checkNotNull(bounds);
        if (start == end) {
            bounds.set(0, 0, 0, 0);
            return;
        }
        int paraIndex = findParaIndex(start);
        int paraStart = getParagraphStart(paraIndex);
        int paraEnd = getParagraphEnd(paraIndex);
        if (start < paraStart || paraEnd < end) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot measured across the paragraph:para: (");
            stringBuilder.append(paraStart);
            stringBuilder.append(", ");
            stringBuilder.append(paraEnd);
            stringBuilder.append("), request: (");
            stringBuilder.append(start);
            stringBuilder.append(", ");
            stringBuilder.append(end);
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        getMeasuredParagraph(paraIndex).getBounds(start - paraStart, end - paraStart, bounds);
    }

    public int getMemoryUsage() {
        int r = 0;
        for (int i = 0; i < getParagraphCount(); i++) {
            r += getMeasuredParagraph(i).getMemoryUsage();
        }
        return r;
    }

    public void setSpan(Object what, int start, int end, int flags) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be set to PrecomputedText.");
        }
        this.mText.setSpan(what, start, end, flags);
    }

    public void removeSpan(Object what) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be removed from PrecomputedText.");
        }
        this.mText.removeSpan(what);
    }

    public <T> T[] getSpans(int start, int end, Class<T> type) {
        return this.mText.getSpans(start, end, type);
    }

    public int getSpanStart(Object tag) {
        return this.mText.getSpanStart(tag);
    }

    public int getSpanEnd(Object tag) {
        return this.mText.getSpanEnd(tag);
    }

    public int getSpanFlags(Object tag) {
        return this.mText.getSpanFlags(tag);
    }

    public int nextSpanTransition(int start, int limit, Class type) {
        return this.mText.nextSpanTransition(start, limit, type);
    }

    public int length() {
        return this.mText.length();
    }

    public char charAt(int index) {
        return this.mText.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return create(this.mText.subSequence(start, end), this.mParams);
    }

    public String toString() {
        return this.mText.toString();
    }
}
