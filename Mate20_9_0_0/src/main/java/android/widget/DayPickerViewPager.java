package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import com.android.internal.widget.ViewPager;
import com.android.internal.widget.ViewPager.LayoutParams;
import java.util.ArrayList;
import java.util.function.Predicate;

class DayPickerViewPager extends ViewPager {
    private final ArrayList<View> mMatchParentChildren;

    public DayPickerViewPager(Context context) {
        this(context, null);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DayPickerViewPager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMatchParentChildren = new ArrayList(1);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int i2 = widthMeasureSpec;
        int i3 = heightMeasureSpec;
        populate();
        int count = getChildCount();
        int i4 = 0;
        int i5 = 1073741824;
        boolean measureMatchParentChildren = (MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && MeasureSpec.getMode(heightMeasureSpec) == 1073741824) ? false : true;
        int maxWidth = 0;
        int childState = 0;
        int maxHeight = 0;
        for (i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                measureChild(child, i2, i3);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren && (lp.width == -1 || lp.height == -1)) {
                    this.mMatchParentChildren.add(child);
                }
            }
        }
        maxWidth += getPaddingLeft() + getPaddingRight();
        i = Math.max(maxHeight + (getPaddingTop() + getPaddingBottom()), getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        Drawable drawable = getForeground();
        if (drawable != null) {
            i = Math.max(i, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth, i2, childState), resolveSizeAndState(i, i3, childState << 16));
        count = this.mMatchParentChildren.size();
        if (count > 1) {
            while (i4 < count) {
                int childWidthMeasureSpec;
                int childHeightMeasureSpec;
                View child2 = (View) this.mMatchParentChildren.get(i4);
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (lp2.width == -1) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), i5);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(i2, getPaddingLeft() + getPaddingRight(), lp2.width);
                }
                if (lp2.height == -1) {
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom(), i5);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(i3, getPaddingTop() + getPaddingBottom(), lp2.height);
                }
                child2.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                i4++;
                i5 = 1073741824;
            }
        }
        this.mMatchParentChildren.clear();
    }

    protected <T extends View> T findViewByPredicateTraversal(Predicate<View> predicate, View childToSkip) {
        if (predicate.test(this)) {
            return this;
        }
        View current = ((DayPickerPagerAdapter) getAdapter()).getView(getCurrent());
        if (!(current == childToSkip || current == null)) {
            View v = current.findViewByPredicate(predicate);
            if (v != null) {
                return v;
            }
        }
        int len = getChildCount();
        for (int i = 0; i < len; i++) {
            View child = getChildAt(i);
            if (!(child == childToSkip || child == current)) {
                View v2 = child.findViewByPredicate(predicate);
                if (v2 != null) {
                    return v2;
                }
            }
        }
        return null;
    }
}
