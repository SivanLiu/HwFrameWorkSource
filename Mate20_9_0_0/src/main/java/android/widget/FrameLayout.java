package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewHierarchyEncoder;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import java.util.ArrayList;

@RemoteView
public class FrameLayout extends ViewGroup {
    private static final int DEFAULT_CHILD_GRAVITY = 8388659;
    @ExportedProperty(category = "padding")
    private int mForegroundPaddingBottom;
    @ExportedProperty(category = "padding")
    private int mForegroundPaddingLeft;
    @ExportedProperty(category = "padding")
    private int mForegroundPaddingRight;
    @ExportedProperty(category = "padding")
    private int mForegroundPaddingTop;
    private final ArrayList<View> mMatchParentChildren;
    @ExportedProperty(category = "measurement")
    boolean mMeasureAllChildren;

    public static class LayoutParams extends MarginLayoutParams {
        public static final int UNSPECIFIED_GRAVITY = -1;
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FrameLayout_Layout);
            this.gravity = a.getInt(0, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super((MarginLayoutParams) source);
            this.gravity = source.gravity;
        }
    }

    public FrameLayout(Context context) {
        super(context);
        this.mMeasureAllChildren = false;
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mMatchParentChildren = new ArrayList(1);
    }

    public FrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMeasureAllChildren = false;
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mMatchParentChildren = new ArrayList(1);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FrameLayout, defStyleAttr, defStyleRes);
        if (a.getBoolean(0, false)) {
            setMeasureAllChildren(true);
        }
        a.recycle();
    }

    @RemotableViewMethod
    public void setForegroundGravity(int foregroundGravity) {
        if (getForegroundGravity() != foregroundGravity) {
            super.setForegroundGravity(foregroundGravity);
            Drawable foreground = getForeground();
            if (getForegroundGravity() != 119 || foreground == null) {
                this.mForegroundPaddingLeft = 0;
                this.mForegroundPaddingTop = 0;
                this.mForegroundPaddingRight = 0;
                this.mForegroundPaddingBottom = 0;
            } else {
                Rect padding = new Rect();
                if (foreground.getPadding(padding)) {
                    this.mForegroundPaddingLeft = padding.left;
                    this.mForegroundPaddingTop = padding.top;
                    this.mForegroundPaddingRight = padding.right;
                    this.mForegroundPaddingBottom = padding.bottom;
                }
            }
            requestLayout();
        }
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    int getPaddingLeftWithForeground() {
        if (isForegroundInsidePadding()) {
            return Math.max(this.mPaddingLeft, this.mForegroundPaddingLeft);
        }
        return this.mPaddingLeft + this.mForegroundPaddingLeft;
    }

    int getPaddingRightWithForeground() {
        if (isForegroundInsidePadding()) {
            return Math.max(this.mPaddingRight, this.mForegroundPaddingRight);
        }
        return this.mPaddingRight + this.mForegroundPaddingRight;
    }

    private int getPaddingTopWithForeground() {
        if (isForegroundInsidePadding()) {
            return Math.max(this.mPaddingTop, this.mForegroundPaddingTop);
        }
        return this.mPaddingTop + this.mForegroundPaddingTop;
    }

    private int getPaddingBottomWithForeground() {
        if (isForegroundInsidePadding()) {
            return Math.max(this.mPaddingBottom, this.mForegroundPaddingBottom);
        }
        return this.mPaddingBottom + this.mForegroundPaddingBottom;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int i2;
        int childState;
        int maxWidth;
        int childState2;
        int i3 = widthMeasureSpec;
        int i4 = heightMeasureSpec;
        int count = getChildCount();
        boolean z = (MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && MeasureSpec.getMode(heightMeasureSpec) == 1073741824) ? false : true;
        boolean measureMatchParentChildren = z;
        this.mMatchParentChildren.clear();
        int maxHeight = 0;
        int maxWidth2 = 0;
        int childState3 = 0;
        int i5 = 0;
        while (true) {
            i = i5;
            if (i >= count) {
                break;
            }
            int i6;
            View child = getChildAt(i);
            if (this.mMeasureAllChildren || child.getVisibility() != 8) {
                View child2 = child;
                i2 = -1;
                i6 = i;
                childState = childState3;
                measureChildWithMargins(child, i3, 0, i4, 0);
                LayoutParams lp = (LayoutParams) child2.getLayoutParams();
                maxWidth = Math.max(maxWidth2, (child2.getMeasuredWidth() + lp.leftMargin) + lp.rightMargin);
                int maxHeight2 = Math.max(maxHeight, (child2.getMeasuredHeight() + lp.topMargin) + lp.bottomMargin);
                childState2 = View.combineMeasuredStates(childState, child2.getMeasuredState());
                if (measureMatchParentChildren && (lp.width == i2 || lp.height == i2)) {
                    this.mMatchParentChildren.add(child2);
                }
                maxWidth2 = maxWidth;
                maxHeight = maxHeight2;
                childState3 = childState2;
            } else {
                i6 = i;
            }
            i5 = i6 + 1;
        }
        i2 = -1;
        childState = childState3;
        maxWidth2 += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
        i5 = Math.max(maxHeight + (getPaddingTopWithForeground() + getPaddingBottomWithForeground()), getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth2, getSuggestedMinimumWidth());
        Drawable drawable = getForeground();
        if (drawable != null) {
            i5 = Math.max(i5, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }
        setMeasuredDimension(View.resolveSizeAndState(maxWidth, i3, childState), View.resolveSizeAndState(i5, i4, childState << 16));
        childState2 = this.mMatchParentChildren.size();
        if (childState2 > 1) {
            i = 0;
            while (i < childState2) {
                int childWidthMeasureSpec;
                View child3 = (View) this.mMatchParentChildren.get(i);
                MarginLayoutParams lp2 = (MarginLayoutParams) child3.getLayoutParams();
                if (lp2.width == i2) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, (((getMeasuredWidth() - getPaddingLeftWithForeground()) - getPaddingRightWithForeground()) - lp2.leftMargin) - lp2.rightMargin), 1073741824);
                } else {
                    childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(i3, ((getPaddingLeftWithForeground() + getPaddingRightWithForeground()) + lp2.leftMargin) + lp2.rightMargin, lp2.width);
                }
                if (lp2.height == i2) {
                    maxWidth2 = MeasureSpec.makeMeasureSpec(Math.max(0, (((getMeasuredHeight() - getPaddingTopWithForeground()) - getPaddingBottomWithForeground()) - lp2.topMargin) - lp2.bottomMargin), 1073741824);
                } else {
                    maxWidth2 = ViewGroup.getChildMeasureSpec(i4, ((getPaddingTopWithForeground() + getPaddingBottomWithForeground()) + lp2.topMargin) + lp2.bottomMargin, lp2.height);
                }
                child3.measure(childWidthMeasureSpec, maxWidth2);
                i++;
                i2 = -1;
            }
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren(left, top, right, bottom, false);
    }

    void layoutChildren(int left, int top, int right, int bottom, boolean forceLeftGravity) {
        int count;
        int parentLeft;
        int count2 = getChildCount();
        int parentLeft2 = getPaddingLeftWithForeground();
        int parentRight = (right - left) - getPaddingRightWithForeground();
        int parentTop = getPaddingTopWithForeground();
        int parentBottom = (bottom - top) - getPaddingBottomWithForeground();
        int i = 0;
        while (i < count2) {
            View child = getChildAt(i);
            if (child == null || child.getVisibility() == 8) {
                count = count2;
                parentLeft = parentLeft2;
            } else {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int gravity = lp.gravity;
                count = count2;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }
                count2 = getLayoutDirection();
                int absoluteGravity = Gravity.getAbsoluteGravity(gravity, count2);
                int layoutDirection = count2;
                count2 = gravity & 112;
                int i2 = absoluteGravity & 7;
                int childLeft = i2 != 1 ? (i2 == 5 && !forceLeftGravity) ? (parentRight - width) - lp.rightMargin : parentLeft2 + lp.leftMargin : (((((parentRight - parentLeft2) - width) / 2) + parentLeft2) + lp.leftMargin) - lp.rightMargin;
                i2 = childLeft;
                int verticalGravity;
                if (count2 == 16) {
                    verticalGravity = count2;
                    childLeft = (((((parentBottom - parentTop) - height) / 2) + parentTop) + lp.topMargin) - lp.bottomMargin;
                } else if (count2 == 48) {
                    verticalGravity = count2;
                    childLeft = parentTop + lp.topMargin;
                } else if (count2 != 80) {
                    childLeft = lp.topMargin + parentTop;
                    verticalGravity = count2;
                } else {
                    verticalGravity = count2;
                    childLeft = (parentBottom - height) - lp.bottomMargin;
                }
                count2 = childLeft;
                parentLeft = parentLeft2;
                child.layout(i2, count2, i2 + width, count2 + height);
            }
            i++;
            count2 = count;
            parentLeft2 = parentLeft;
        }
        count = count2;
        parentLeft = parentLeft2;
    }

    @RemotableViewMethod
    public void setMeasureAllChildren(boolean measureAll) {
        this.mMeasureAllChildren = measureAll;
    }

    @Deprecated
    public boolean getConsiderGoneChildrenWhenMeasuring() {
        return getMeasureAllChildren();
    }

    public boolean getMeasureAllChildren() {
        return this.mMeasureAllChildren;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    protected android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
        if (sPreserveMarginParamsInLayoutParamConversion) {
            if (lp instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) lp);
            }
            if (lp instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams) lp);
            }
        }
        return new LayoutParams(lp);
    }

    public CharSequence getAccessibilityClassName() {
        return FrameLayout.class.getName();
    }

    protected void encodeProperties(ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);
        encoder.addProperty("measurement:measureAllChildren", this.mMeasureAllChildren);
        encoder.addProperty("padding:foregroundPaddingLeft", this.mForegroundPaddingLeft);
        encoder.addProperty("padding:foregroundPaddingTop", this.mForegroundPaddingTop);
        encoder.addProperty("padding:foregroundPaddingRight", this.mForegroundPaddingRight);
        encoder.addProperty("padding:foregroundPaddingBottom", this.mForegroundPaddingBottom);
    }
}
