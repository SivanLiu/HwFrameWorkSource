package com.android.internal.widget;

import android.content.Context;
import android.text.BoringLayout.Metrics;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout.Builder;
import android.text.TextUtils.TruncateAt;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View.MeasureSpec;
import android.widget.RemoteViews.RemoteView;
import android.widget.TextView;

@RemoteView
public class ImageFloatingTextView extends TextView {
    private int mImageEndMargin;
    private int mIndentLines;
    private int mLayoutMaxLines;
    private int mMaxLinesForHeight;
    private int mResolvedDirection;

    public ImageFloatingTextView(Context context) {
        this(context, null);
    }

    public ImageFloatingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageFloatingTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageFloatingTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mResolvedDirection = -1;
        this.mMaxLinesForHeight = -1;
        this.mLayoutMaxLines = -1;
    }

    protected Layout makeSingleLayout(int wantWidth, Metrics boring, int ellipsisWidth, Alignment alignment, boolean shouldEllipsize, TruncateAt effectiveEllipsize, boolean useSaved) {
        TransformationMethod transformationMethod = getTransformationMethod();
        CharSequence text = getText();
        if (transformationMethod != null) {
            text = transformationMethod.getTransformation(text, this);
        }
        String text2 = text == null ? "" : text;
        int i = 0;
        Builder builder = Builder.obtain(text2, 0, text2.length(), getPaint(), wantWidth).setAlignment(alignment).setTextDirection(getTextDirectionHeuristic()).setLineSpacing(getLineSpacingExtra(), getLineSpacingMultiplier()).setIncludePad(getIncludeFontPadding()).setUseLineSpacingFromFallbacks(true).setBreakStrategy(1).setHyphenationFrequency(2);
        int maxLines = this.mMaxLinesForHeight > 0 ? this.mMaxLinesForHeight : getMaxLines() >= 0 ? getMaxLines() : Integer.MAX_VALUE;
        builder.setMaxLines(maxLines);
        this.mLayoutMaxLines = maxLines;
        if (shouldEllipsize) {
            builder.setEllipsize(effectiveEllipsize).setEllipsizedWidth(ellipsisWidth);
        }
        int[] margins = null;
        if (this.mIndentLines > 0) {
            margins = new int[(this.mIndentLines + 1)];
            while (i < this.mIndentLines) {
                margins[i] = this.mImageEndMargin;
                i++;
            }
        }
        if (this.mResolvedDirection == 1) {
            builder.setIndents(margins, null);
        } else {
            builder.setIndents(null, margins);
        }
        return builder.build();
    }

    @RemotableViewMethod
    public void setImageEndMargin(int imageEndMargin) {
        this.mImageEndMargin = imageEndMargin;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableHeight = (MeasureSpec.getSize(heightMeasureSpec) - this.mPaddingTop) - this.mPaddingBottom;
        if (!(getLayout() == null || getLayout().getHeight() == availableHeight)) {
            this.mMaxLinesForHeight = -1;
            nullLayouts();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Layout layout = getLayout();
        if (layout.getHeight() > availableHeight) {
            int maxLines = layout.getLineCount() - 1;
            while (maxLines > 1 && layout.getLineBottom(maxLines - 1) > availableHeight) {
                maxLines--;
            }
            if (getMaxLines() > 0) {
                maxLines = Math.min(getMaxLines(), maxLines);
            }
            if (maxLines != this.mLayoutMaxLines) {
                this.mMaxLinesForHeight = maxLines;
                nullLayouts();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (layoutDirection != this.mResolvedDirection && isLayoutDirectionResolved()) {
            this.mResolvedDirection = layoutDirection;
            if (this.mIndentLines > 0) {
                nullLayouts();
                requestLayout();
            }
        }
    }

    @RemotableViewMethod
    public void setHasImage(boolean hasImage) {
        setNumIndentLines(hasImage ? 2 : 0);
    }

    public boolean setNumIndentLines(int lines) {
        if (this.mIndentLines == lines) {
            return false;
        }
        this.mIndentLines = lines;
        nullLayouts();
        requestLayout();
        return true;
    }
}
