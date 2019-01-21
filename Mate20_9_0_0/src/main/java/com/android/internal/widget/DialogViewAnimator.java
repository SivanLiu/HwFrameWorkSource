package com.android.internal.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ViewAnimator;
import com.android.internal.colorextraction.types.Tonal;
import java.util.ArrayList;

public class DialogViewAnimator extends ViewAnimator {
    private final ArrayList<View> mMatchParentChildren = new ArrayList(1);

    public DialogViewAnimator(Context context) {
        super(context);
    }

    public DialogViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int childState;
        int i2 = widthMeasureSpec;
        int i3 = heightMeasureSpec;
        boolean z = (MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && MeasureSpec.getMode(heightMeasureSpec) == 1073741824) ? false : true;
        boolean measureMatchParentChildren = z;
        int count = getChildCount();
        int maxHeight = 0;
        int maxWidth = 0;
        int childState2 = 0;
        int i4 = 0;
        while (true) {
            i = i4;
            i4 = -1;
            if (i >= count) {
                break;
            }
            int i5;
            View child = getChildAt(i);
            if (getMeasureAllChildren() || child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                boolean matchWidth = lp.width == -1;
                boolean matchHeight = lp.height == -1;
                if (measureMatchParentChildren && (matchWidth || matchHeight)) {
                    this.mMatchParentChildren.add(child);
                }
                LayoutParams lp2 = lp;
                View child2 = child;
                i5 = i;
                childState = childState2;
                measureChildWithMargins(child, i2, 0, i3, 0);
                i4 = 0;
                if (measureMatchParentChildren && !matchWidth) {
                    maxWidth = Math.max(maxWidth, (child2.getMeasuredWidth() + lp2.leftMargin) + lp2.rightMargin);
                    i4 = 0 | (child2.getMeasuredWidthAndState() & Tonal.MAIN_COLOR_DARK);
                }
                if (measureMatchParentChildren && !matchHeight) {
                    maxHeight = Math.max(maxHeight, (child2.getMeasuredHeight() + lp2.topMargin) + lp2.bottomMargin);
                    i4 |= (child2.getMeasuredHeightAndState() >> 16) & -256;
                }
                childState2 = combineMeasuredStates(childState, i4);
            } else {
                i5 = i;
            }
            i4 = i5 + 1;
        }
        childState = childState2;
        maxWidth += getPaddingLeft() + getPaddingRight();
        int maxHeight2 = Math.max(maxHeight + (getPaddingTop() + getPaddingBottom()), getSuggestedMinimumHeight());
        int maxWidth2 = Math.max(maxWidth, getSuggestedMinimumWidth());
        Drawable drawable = getForeground();
        if (drawable != null) {
            maxHeight2 = Math.max(maxHeight2, drawable.getMinimumHeight());
            maxWidth2 = Math.max(maxWidth2, drawable.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth2, i2, childState), resolveSizeAndState(maxHeight2, i3, childState << 16));
        i = this.mMatchParentChildren.size();
        int i6 = 0;
        while (true) {
            childState2 = i6;
            if (childState2 < i) {
                View child3 = (View) this.mMatchParentChildren.get(childState2);
                MarginLayoutParams lp3 = (MarginLayoutParams) child3.getLayoutParams();
                if (lp3.width == i4) {
                    maxWidth = MeasureSpec.makeMeasureSpec((((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight()) - lp3.leftMargin) - lp3.rightMargin, 1073741824);
                } else {
                    maxWidth = getChildMeasureSpec(i2, ((getPaddingLeft() + getPaddingRight()) + lp3.leftMargin) + lp3.rightMargin, lp3.width);
                }
                if (lp3.height == i4) {
                    maxHeight = MeasureSpec.makeMeasureSpec((((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom()) - lp3.topMargin) - lp3.bottomMargin, 1073741824);
                } else {
                    maxHeight = getChildMeasureSpec(i3, ((getPaddingTop() + getPaddingBottom()) + lp3.topMargin) + lp3.bottomMargin, lp3.height);
                }
                child3.measure(maxWidth, maxHeight);
                i6 = childState2 + 1;
                i4 = -1;
            } else {
                this.mMatchParentChildren.clear();
                return;
            }
        }
    }
}
