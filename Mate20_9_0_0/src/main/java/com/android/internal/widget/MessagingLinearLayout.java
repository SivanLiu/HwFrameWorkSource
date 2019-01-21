package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;

@RemoteView
public class MessagingLinearLayout extends ViewGroup {
    private int mMaxDisplayedLines = Integer.MAX_VALUE;
    private MessagingLayout mMessagingLayout;
    private int mSpacing;

    public interface MessagingChild {
        public static final int MEASURED_NORMAL = 0;
        public static final int MEASURED_SHORTENED = 1;
        public static final int MEASURED_TOO_SMALL = 2;

        int getConsumedLines();

        int getMeasuredType();

        void hideAnimated();

        boolean isHidingAnimated();

        void setMaxDisplayedLines(int i);

        int getExtraSpacing() {
            return 0;
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        public boolean hide = false;
        public int lastVisibleHeight;
        public boolean visibleBefore = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

    public MessagingLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MessagingLinearLayout, 0, 0);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            if (a.getIndex(i) == 0) {
                this.mSpacing = a.getDimensionPixelSize(i, 0);
            }
        }
        a.recycle();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int targetHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == 0) {
            targetHeight = Integer.MAX_VALUE;
        }
        int targetHeight2 = targetHeight;
        targetHeight = this.mPaddingLeft + this.mPaddingRight;
        int count = getChildCount();
        for (i = 0; i < count; i++) {
            ((LayoutParams) getChildAt(i).getLayoutParams()).hide = true;
        }
        int i2 = count - 1;
        int measuredWidth = targetHeight;
        int totalHeight = this.mPaddingTop + this.mPaddingBottom;
        boolean first = true;
        int linesRemaining = this.mMaxDisplayedLines;
        while (true) {
            int i3 = i2;
            if (i3 < 0 || totalHeight >= targetHeight2) {
            } else {
                int count2;
                if (getChildAt(i3).getVisibility() != 8) {
                    View child = getChildAt(i3);
                    LayoutParams lp = (LayoutParams) getChildAt(i3).getLayoutParams();
                    MessagingChild messagingChild = null;
                    i = this.mSpacing;
                    if (child instanceof MessagingChild) {
                        messagingChild = (MessagingChild) child;
                        messagingChild.setMaxDisplayedLines(linesRemaining);
                        i += messagingChild.getExtraSpacing();
                    }
                    int spacing = first ? 0 : i;
                    MessagingChild messagingChild2 = messagingChild;
                    LayoutParams lp2 = lp;
                    count2 = count;
                    count = child;
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, ((totalHeight - this.mPaddingTop) - this.mPaddingBottom) + spacing);
                    i = Math.max(totalHeight, (((totalHeight + count.getMeasuredHeight()) + lp2.topMargin) + lp2.bottomMargin) + spacing);
                    first = false;
                    int measureType = 0;
                    if (messagingChild2 != null) {
                        measureType = messagingChild2.getMeasuredType();
                        linesRemaining -= messagingChild2.getConsumedLines();
                    }
                    boolean isShortened = measureType == 1;
                    boolean isTooSmall = measureType == 2;
                    if (i > targetHeight2 || isTooSmall) {
                        break;
                    }
                    totalHeight = i;
                    measuredWidth = Math.max(measuredWidth, (((count.getMeasuredWidth() + lp2.leftMargin) + lp2.rightMargin) + this.mPaddingLeft) + this.mPaddingRight);
                    lp2.hide = false;
                    if (isShortened || linesRemaining <= 0) {
                        break;
                    }
                } else {
                    count2 = count;
                }
                i2 = i3 - 1;
                count = count2;
            }
        }
        setMeasuredDimension(resolveSize(Math.max(getSuggestedMinimumWidth(), measuredWidth), widthMeasureSpec), Math.max(getSuggestedMinimumHeight(), totalHeight));
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int paddingLeft = this.mPaddingLeft;
        int childRight = (right - left) - this.mPaddingRight;
        int layoutDirection = getLayoutDirection();
        int count = getChildCount();
        int childTop = this.mPaddingTop;
        boolean shown = isShown();
        boolean first = true;
        int childTop2 = childTop;
        childTop = 0;
        while (childTop < count) {
            int paddingLeft2;
            View child = getChildAt(childTop);
            if (child.getVisibility() == 8) {
                paddingLeft2 = paddingLeft;
            } else {
                int childLeft;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                MessagingChild messagingChild = (MessagingChild) child;
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                if (layoutDirection == 1) {
                    childLeft = (childRight - childWidth) - lp.rightMargin;
                } else {
                    childLeft = paddingLeft + lp.leftMargin;
                }
                int childLeft2 = childLeft;
                paddingLeft2 = paddingLeft;
                if (lp.hide != 0) {
                    if (shown && lp.visibleBefore != 0) {
                        child.layout(childLeft2, childTop2, childLeft2 + childWidth, lp.lastVisibleHeight + childTop2);
                        messagingChild.hideAnimated();
                    }
                    lp.visibleBefore = false;
                } else {
                    lp.visibleBefore = true;
                    lp.lastVisibleHeight = childHeight;
                    if (!first) {
                        childTop2 += this.mSpacing;
                    }
                    childTop2 += lp.topMargin;
                    child.layout(childLeft2, childTop2, childLeft2 + childWidth, childTop2 + childHeight);
                    childTop2 += lp.bottomMargin + childHeight;
                    first = false;
                }
            }
            childTop++;
            paddingLeft = paddingLeft2;
        }
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (!((LayoutParams) child.getLayoutParams()).hide || ((MessagingChild) child).isHidingAnimated()) {
            return super.drawChild(canvas, child, drawingTime);
        }
        return true;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(this.mContext, attrs);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -2);
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
        LayoutParams copy = new LayoutParams(lp.width, lp.height);
        if (lp instanceof MarginLayoutParams) {
            copy.copyMarginsFrom((MarginLayoutParams) lp);
        }
        return copy;
    }

    public static boolean isGone(View view) {
        if (view.getVisibility() == 8) {
            return true;
        }
        android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
        if ((lp instanceof LayoutParams) && ((LayoutParams) lp).hide) {
            return true;
        }
        return false;
    }

    @RemotableViewMethod
    public void setMaxDisplayedLines(int numberLines) {
        this.mMaxDisplayedLines = numberLines;
    }

    public void setMessagingLayout(MessagingLayout layout) {
        this.mMessagingLayout = layout;
    }

    public MessagingLayout getMessagingLayout() {
        return this.mMessagingLayout;
    }
}
