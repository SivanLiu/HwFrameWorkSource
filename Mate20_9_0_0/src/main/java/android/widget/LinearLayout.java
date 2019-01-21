package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewDebug.FlagToString;
import android.view.ViewDebug.IntToString;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewHierarchyEncoder;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RemoteView
public class LinearLayout extends ViewGroup {
    public static final int HORIZONTAL = 0;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_FILL = 3;
    private static final int INDEX_TOP = 1;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_END = 4;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int VERTICAL = 1;
    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private static boolean sCompatibilityDone = false;
    private static boolean sRemeasureWeightedChildren = true;
    private final boolean mAllowInconsistentMeasurement;
    @ExportedProperty(category = "layout")
    private boolean mBaselineAligned;
    @ExportedProperty(category = "layout")
    private int mBaselineAlignedChildIndex;
    @ExportedProperty(category = "measurement")
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;
    @ExportedProperty(category = "measurement", flagMapping = {@FlagToString(equals = -1, mask = -1, name = "NONE"), @FlagToString(equals = 0, mask = 0, name = "NONE"), @FlagToString(equals = 48, mask = 48, name = "TOP"), @FlagToString(equals = 80, mask = 80, name = "BOTTOM"), @FlagToString(equals = 3, mask = 3, name = "LEFT"), @FlagToString(equals = 5, mask = 5, name = "RIGHT"), @FlagToString(equals = 8388611, mask = 8388611, name = "START"), @FlagToString(equals = 8388613, mask = 8388613, name = "END"), @FlagToString(equals = 16, mask = 16, name = "CENTER_VERTICAL"), @FlagToString(equals = 112, mask = 112, name = "FILL_VERTICAL"), @FlagToString(equals = 1, mask = 1, name = "CENTER_HORIZONTAL"), @FlagToString(equals = 7, mask = 7, name = "FILL_HORIZONTAL"), @FlagToString(equals = 17, mask = 17, name = "CENTER"), @FlagToString(equals = 119, mask = 119, name = "FILL"), @FlagToString(equals = 8388608, mask = 8388608, name = "RELATIVE")}, formatToHexString = true)
    private int mGravity;
    public boolean mHwActionBarTabLayoutUsed;
    public boolean mHwActionBarViewUsed;
    private int mLayoutDirection;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    @ExportedProperty(category = "measurement")
    private int mOrientation;
    private int mShowDividers;
    @ExportedProperty(category = "measurement")
    private int mTotalLength;
    @ExportedProperty(category = "layout")
    private boolean mUseLargestChild;
    @ExportedProperty(category = "layout")
    private float mWeightSum;

    @Retention(RetentionPolicy.SOURCE)
    public @interface DividerMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public static class LayoutParams extends MarginLayoutParams {
        @ExportedProperty(category = "layout", mapping = {@IntToString(from = -1, to = "NONE"), @IntToString(from = 0, to = "NONE"), @IntToString(from = 48, to = "TOP"), @IntToString(from = 80, to = "BOTTOM"), @IntToString(from = 3, to = "LEFT"), @IntToString(from = 5, to = "RIGHT"), @IntToString(from = 8388611, to = "START"), @IntToString(from = 8388613, to = "END"), @IntToString(from = 16, to = "CENTER_VERTICAL"), @IntToString(from = 112, to = "FILL_VERTICAL"), @IntToString(from = 1, to = "CENTER_HORIZONTAL"), @IntToString(from = 7, to = "FILL_HORIZONTAL"), @IntToString(from = 17, to = "CENTER"), @IntToString(from = 119, to = "FILL")})
        public int gravity;
        @ExportedProperty(category = "layout")
        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.LinearLayout_Layout);
            this.weight = a.getFloat(3, 0.0f);
            this.gravity = a.getInt(0, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = -1;
            this.weight = 0.0f;
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.gravity = -1;
            this.weight = weight;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams p) {
            super(p);
            this.gravity = -1;
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            this.gravity = -1;
        }

        public LayoutParams(LayoutParams source) {
            super((MarginLayoutParams) source);
            this.gravity = -1;
            this.weight = source.weight;
            this.gravity = source.gravity;
        }

        public String debug(String output) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(output);
            stringBuilder.append("LinearLayout.LayoutParams={width=");
            stringBuilder.append(android.view.ViewGroup.LayoutParams.sizeToString(this.width));
            stringBuilder.append(", height=");
            stringBuilder.append(android.view.ViewGroup.LayoutParams.sizeToString(this.height));
            stringBuilder.append(" weight=");
            stringBuilder.append(this.weight);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        protected void encodeProperties(ViewHierarchyEncoder encoder) {
            super.encodeProperties(encoder);
            encoder.addProperty("layout:weight", this.weight);
            encoder.addProperty("layout:gravity", this.gravity);
        }
    }

    public LinearLayout(Context context) {
        this(context, null);
    }

    public LinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        boolean z = true;
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        this.mLayoutDirection = -1;
        if (!(sCompatibilityDone || context == null)) {
            sRemeasureWeightedChildren = context.getApplicationInfo().targetSdkVersion >= 28;
            sCompatibilityDone = true;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LinearLayout, defStyleAttr, defStyleRes);
        int index = a.getInt(1, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        index = a.getInt(0, -1);
        if (index >= 0) {
            setGravity(index);
        }
        boolean baselineAligned = a.getBoolean(true, true);
        if (!baselineAligned) {
            setBaselineAligned(baselineAligned);
        }
        this.mWeightSum = a.getFloat(4, -1.0f);
        this.mBaselineAlignedChildIndex = a.getInt(3, -1);
        this.mUseLargestChild = a.getBoolean(6, false);
        this.mShowDividers = a.getInt(7, 0);
        this.mDividerPadding = a.getDimensionPixelSize(8, 0);
        setDividerDrawable(a.getDrawable(5));
        if (context.getApplicationInfo().targetSdkVersion > 23) {
            z = false;
        }
        this.mAllowInconsistentMeasurement = z;
        a.recycle();
    }

    private boolean isShowingDividers() {
        return (this.mShowDividers == 0 || this.mDivider == null) ? false : true;
    }

    public void setShowDividers(int showDividers) {
        if (showDividers != this.mShowDividers) {
            this.mShowDividers = showDividers;
            setWillNotDraw(isShowingDividers() ^ 1);
            requestLayout();
        }
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public int getShowDividers() {
        return this.mShowDividers;
    }

    public Drawable getDividerDrawable() {
        return this.mDivider;
    }

    public void setDividerDrawable(Drawable divider) {
        if (divider != this.mDivider) {
            this.mDivider = divider;
            if (divider != null) {
                this.mDividerWidth = divider.getIntrinsicWidth();
                this.mDividerHeight = divider.getIntrinsicHeight();
            } else {
                this.mDividerWidth = 0;
                this.mDividerHeight = 0;
            }
            setWillNotDraw(isShowingDividers() ^ 1);
            requestLayout();
        }
    }

    public void setDividerPadding(int padding) {
        if (padding != this.mDividerPadding) {
            this.mDividerPadding = padding;
            if (isShowingDividers()) {
                requestLayout();
                invalidate();
            }
        }
    }

    public int getDividerPadding() {
        return this.mDividerPadding;
    }

    public int getDividerWidth() {
        return this.mDividerWidth;
    }

    protected void onDraw(Canvas canvas) {
        if (this.mDivider != null) {
            if (this.mOrientation == 1) {
                drawDividersVertical(canvas);
            } else {
                drawDividersHorizontal(canvas);
            }
        }
    }

    void drawDividersVertical(Canvas canvas) {
        int count = getVirtualChildCount();
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                drawHorizontalDivider(canvas, (child.getTop() - ((LayoutParams) child.getLayoutParams()).topMargin) - this.mDividerHeight);
            }
            i++;
        }
        if (hasDividerBeforeChildAt(count)) {
            int bottom;
            View child2 = getLastNonGoneChild();
            if (child2 == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                bottom = child2.getBottom() + ((LayoutParams) child2.getLayoutParams()).bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    private View getLastNonGoneChild() {
        for (int i = getVirtualChildCount() - 1; i >= 0; i--) {
            View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    void drawDividersHorizontal(Canvas canvas) {
        int count = getVirtualChildCount();
        boolean isLayoutRtl = isLayoutRtl();
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                int position;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!isLayoutRtl) {
                    position = (child.getLeft() - lp.leftMargin) - this.mDividerWidth;
                } else if (i == 0) {
                    position = (child.getRight() + lp.rightMargin) - this.mDividerWidth;
                } else {
                    position = child.getRight() + lp.rightMargin;
                }
                drawVerticalDivider(canvas, position);
            }
            i++;
        }
        if (hasDividerBeforeChildAt(count)) {
            int position2;
            View child2 = getLastNonGoneChild();
            if (child2 != null) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (isLayoutRtl) {
                    position2 = (child2.getLeft() - lp2.leftMargin) - this.mDividerWidth;
                } else {
                    position2 = child2.getRight() + lp2.rightMargin;
                }
            } else if (isLayoutRtl) {
                position2 = getPaddingLeft();
            } else {
                position2 = (getWidth() - getPaddingRight()) - this.mDividerWidth;
            }
            drawVerticalDivider(canvas, position2);
        }
    }

    void drawHorizontalDivider(Canvas canvas, int top) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, top, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + top);
        this.mDivider.draw(canvas);
    }

    void drawVerticalDivider(Canvas canvas, int left) {
        this.mDivider.setBounds(left, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + left, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    public boolean isBaselineAligned() {
        return this.mBaselineAligned;
    }

    @RemotableViewMethod
    public void setBaselineAligned(boolean baselineAligned) {
        this.mBaselineAligned = baselineAligned;
    }

    public boolean isMeasureWithLargestChildEnabled() {
        return this.mUseLargestChild;
    }

    @RemotableViewMethod
    public void setMeasureWithLargestChildEnabled(boolean enabled) {
        this.mUseLargestChild = enabled;
    }

    public int getBaseline() {
        if (this.mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        if (getChildCount() > this.mBaselineAlignedChildIndex) {
            View child = getChildAt(this.mBaselineAlignedChildIndex);
            int childBaseline = child.getBaseline();
            if (childBaseline != -1) {
                int childTop = this.mBaselineChildTop;
                if (this.mOrientation == 1) {
                    int majorGravity = this.mGravity & 112;
                    if (majorGravity != 48) {
                        if (majorGravity == 16) {
                            childTop += ((((this.mBottom - this.mTop) - this.mPaddingTop) - this.mPaddingBottom) - this.mTotalLength) / 2;
                        } else if (majorGravity == 80) {
                            childTop = ((this.mBottom - this.mTop) - this.mPaddingBottom) - this.mTotalLength;
                        }
                    }
                }
                return (((LayoutParams) child.getLayoutParams()).topMargin + childTop) + childBaseline;
            } else if (this.mBaselineAlignedChildIndex == 0) {
                return -1;
            } else {
                throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
            }
        }
        throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
    }

    public int getBaselineAlignedChildIndex() {
        return this.mBaselineAlignedChildIndex;
    }

    @RemotableViewMethod
    public void setBaselineAlignedChildIndex(int i) {
        if (i < 0 || i >= getChildCount()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("base aligned child index out of range (0, ");
            stringBuilder.append(getChildCount());
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.mBaselineAlignedChildIndex = i;
    }

    View getVirtualChildAt(int index) {
        return getChildAt(index);
    }

    int getVirtualChildCount() {
        return getChildCount();
    }

    public float getWeightSum() {
        return this.mWeightSum;
    }

    @RemotableViewMethod
    public void setWeightSum(float weightSum) {
        this.mWeightSum = Math.max(0.0f, weightSum);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOrientation == 1) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    protected boolean hasDividerBeforeChildAt(int childIndex) {
        boolean z = false;
        if (childIndex == getVirtualChildCount()) {
            if ((this.mShowDividers & 4) != 0) {
                z = true;
            }
            return z;
        } else if (allViewsAreGoneBefore(childIndex)) {
            if ((this.mShowDividers & 1) != 0) {
                z = true;
            }
            return z;
        } else {
            if ((this.mShowDividers & 2) != 0) {
                z = true;
            }
            return z;
        }
    }

    private boolean allViewsAreGoneBefore(int childIndex) {
        for (int i = childIndex - 1; i >= 0; i--) {
            View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != 8) {
                return false;
            }
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:164:0x03e7  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x03e4  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x03f8  */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x03ee  */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x0474  */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x046e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int i = widthMeasureSpec;
        int i2 = heightMeasureSpec;
        this.mTotalLength = 0;
        float totalWeight = 0.0f;
        int count = getVirtualChildCount();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean skippedMeasure = false;
        int baselineChildIndex = this.mBaselineAlignedChildIndex;
        boolean useLargestChild = this.mUseLargestChild;
        int consumedExcessSpace = 0;
        int nonSkippedChildCount = 0;
        boolean matchWidth = false;
        int childState = 0;
        int maxWidth = 0;
        int i3 = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;
        boolean allFillParent = true;
        int largestChildHeight = Integer.MIN_VALUE;
        while (true) {
            int largestChildHeight2 = largestChildHeight;
            int count2;
            int heightMode2;
            boolean z;
            LayoutParams lp;
            boolean skippedMeasure2;
            int maxWidth2;
            int measuredWidth;
            if (i3 < count) {
                View child = getVirtualChildAt(i3);
                int maxWidth3;
                if (child == null) {
                    maxWidth3 = maxWidth;
                    this.mTotalLength += measureNullChild(i3);
                    count2 = count;
                    heightMode2 = heightMode;
                    largestChildHeight = largestChildHeight2;
                    maxWidth = maxWidth3;
                } else {
                    maxWidth3 = maxWidth;
                    boolean weightedMaxWidth2 = weightedMaxWidth;
                    if (child.getVisibility() == 8) {
                        i3 += getChildrenSkipCount(child, i3);
                        count2 = count;
                        heightMode2 = heightMode;
                        largestChildHeight = largestChildHeight2;
                        maxWidth = maxWidth3;
                        weightedMaxWidth = weightedMaxWidth2;
                    } else {
                        int alternativeMaxWidth2;
                        View child2;
                        int childState2;
                        boolean weightedMaxWidth3;
                        int i4;
                        nonSkippedChildCount++;
                        if (hasDividerBeforeChildAt(i3)) {
                            this.mTotalLength += this.mDividerHeight;
                        }
                        LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                        float totalWeight2 = totalWeight + lp2.weight;
                        z = lp2.height == 0 && lp2.weight > 0.0f;
                        boolean useExcessSpace = z;
                        int i5;
                        if (heightMode == 1073741824 && useExcessSpace) {
                            maxWidth = this.mTotalLength;
                            i5 = i3;
                            this.mTotalLength = Math.max(maxWidth, (lp2.topMargin + maxWidth) + lp2.bottomMargin);
                            lp = lp2;
                            alternativeMaxWidth2 = alternativeMaxWidth;
                            child2 = child;
                            childState2 = childState;
                            count2 = count;
                            heightMode2 = heightMode;
                            skippedMeasure2 = true;
                            count = largestChildHeight2;
                            maxWidth2 = maxWidth3;
                            weightedMaxWidth3 = weightedMaxWidth2;
                            i4 = i5;
                            heightMode = 1073741824;
                        } else {
                            i5 = i3;
                            if (useExcessSpace) {
                                lp2.height = -2;
                            }
                            i4 = i5;
                            skippedMeasure2 = skippedMeasure;
                            maxWidth2 = maxWidth3;
                            LayoutParams lp3 = lp2;
                            weightedMaxWidth3 = weightedMaxWidth2;
                            heightMode2 = heightMode;
                            heightMode = alternativeMaxWidth;
                            alternativeMaxWidth = i;
                            child2 = child;
                            count2 = count;
                            alternativeMaxWidth2 = heightMode;
                            count = largestChildHeight2;
                            heightMode = 1073741824;
                            childState2 = childState;
                            measureChildBeforeLayout(child, i4, alternativeMaxWidth, 0, i2, totalWeight2 == 0.0f ? this.mTotalLength : 0);
                            i3 = child2.getMeasuredHeight();
                            if (useExcessSpace) {
                                lp = lp3;
                                lp.height = 0;
                                consumedExcessSpace += i3;
                            } else {
                                lp = lp3;
                            }
                            weightedMaxWidth = this.mTotalLength;
                            this.mTotalLength = Math.max(weightedMaxWidth, (((weightedMaxWidth + i3) + lp.topMargin) + lp.bottomMargin) + getNextLocationOffset(child2));
                            if (useLargestChild) {
                                count = Math.max(i3, count);
                            }
                        }
                        if (baselineChildIndex >= 0) {
                            i3 = i4;
                            if (baselineChildIndex == i3 + 1) {
                                this.mBaselineChildTop = this.mTotalLength;
                            }
                        } else {
                            i3 = i4;
                        }
                        if (i3 >= baselineChildIndex || lp.weight <= 0.0f) {
                            boolean allFillParent2;
                            int childState3;
                            boolean matchWidthLocally = false;
                            if (widthMode != heightMode) {
                                largestChildHeight = -1;
                                if (lp.width == -1) {
                                    matchWidth = true;
                                    matchWidthLocally = true;
                                }
                            } else {
                                largestChildHeight = -1;
                            }
                            alternativeMaxWidth = lp.leftMargin + lp.rightMargin;
                            measuredWidth = child2.getMeasuredWidth() + alternativeMaxWidth;
                            childState = Math.max(maxWidth2, measuredWidth);
                            heightMode = View.combineMeasuredStates(childState2, child2.getMeasuredState());
                            boolean allFillParent3 = allFillParent && lp.width == largestChildHeight;
                            if (lp.weight > 0.0f) {
                                allFillParent2 = allFillParent3;
                                largestChildHeight = Math.max(weightedMaxWidth3, matchWidthLocally ? alternativeMaxWidth : measuredWidth);
                                childState3 = heightMode;
                                heightMode = alternativeMaxWidth2;
                            } else {
                                allFillParent2 = allFillParent3;
                                largestChildHeight = weightedMaxWidth3;
                                childState3 = heightMode;
                                heightMode = Math.max(alternativeMaxWidth2, matchWidthLocally ? alternativeMaxWidth : measuredWidth);
                            }
                            i3 += getChildrenSkipCount(child2, i3);
                            weightedMaxWidth = largestChildHeight;
                            maxWidth = childState;
                            largestChildHeight = count;
                            alternativeMaxWidth = heightMode;
                            totalWeight = totalWeight2;
                            skippedMeasure = skippedMeasure2;
                            allFillParent = allFillParent2;
                            childState = childState3;
                        } else {
                            throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                        }
                    }
                }
                i3++;
                heightMode = heightMode2;
                count = count2;
                i = widthMeasureSpec;
            } else {
                int childState4;
                int remainingExcess;
                float totalWeight3;
                int count3;
                int i6;
                boolean z2;
                int i7;
                largestChildHeight = weightedMaxWidth;
                count2 = count;
                heightMode2 = heightMode;
                skippedMeasure2 = skippedMeasure;
                count = largestChildHeight2;
                maxWidth2 = maxWidth;
                heightMode = alternativeMaxWidth;
                maxWidth = childState;
                if (nonSkippedChildCount > 0) {
                    alternativeMaxWidth = count2;
                    if (hasDividerBeforeChildAt(alternativeMaxWidth)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                } else {
                    alternativeMaxWidth = count2;
                }
                if (useLargestChild) {
                    i = heightMode2;
                    if (i == Integer.MIN_VALUE || i == 0) {
                        this.mTotalLength = 0;
                        childState = 0;
                        while (childState < alternativeMaxWidth) {
                            int i8;
                            View child3 = getVirtualChildAt(childState);
                            if (child3 == null) {
                                this.mTotalLength += measureNullChild(childState);
                                childState4 = maxWidth;
                            } else {
                                childState4 = maxWidth;
                                if (child3.getVisibility() == 8) {
                                    childState += getChildrenSkipCount(child3, childState);
                                } else {
                                    LayoutParams lp4 = (LayoutParams) child3.getLayoutParams();
                                    maxWidth = this.mTotalLength;
                                    i8 = childState;
                                    this.mTotalLength = Math.max(maxWidth, (((maxWidth + count) + lp4.topMargin) + lp4.bottomMargin) + getNextLocationOffset(child3));
                                    childState = i8 + 1;
                                    maxWidth = childState4;
                                }
                            }
                            i8 = childState;
                            childState = i8 + 1;
                            maxWidth = childState4;
                        }
                        childState4 = maxWidth;
                    } else {
                        childState4 = maxWidth;
                    }
                } else {
                    childState4 = maxWidth;
                    i = heightMode2;
                }
                this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
                weightedMaxWidth = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumHeight()), i2, 0);
                i3 = weightedMaxWidth & 16777215;
                maxWidth = (i3 - this.mTotalLength) + (this.mAllowInconsistentMeasurement ? 0 : consumedExcessSpace);
                int i9;
                int i10;
                if (skippedMeasure2) {
                    remainingExcess = maxWidth;
                    i9 = largestChildHeight;
                    totalWeight3 = totalWeight;
                } else if ((sRemeasureWeightedChildren || maxWidth != 0) && totalWeight > 0.0f) {
                    i10 = i3;
                    remainingExcess = maxWidth;
                    i9 = largestChildHeight;
                    totalWeight3 = totalWeight;
                } else {
                    childState = Math.max(heightMode, largestChildHeight);
                    if (useLargestChild && i != 1073741824) {
                        int i11 = 0;
                        while (true) {
                            heightMode = i11;
                            if (heightMode >= alternativeMaxWidth) {
                                break;
                            }
                            i10 = i3;
                            View child4 = getVirtualChildAt(heightMode);
                            if (child4 != null) {
                                remainingExcess = maxWidth;
                                i9 = largestChildHeight;
                                if (child4.getVisibility() == 8) {
                                    totalWeight3 = totalWeight;
                                } else {
                                    lp = (LayoutParams) child4.getLayoutParams();
                                    float childExtra = lp.weight;
                                    if (childExtra > 0.0f) {
                                        LayoutParams lp5 = lp;
                                        totalWeight3 = totalWeight;
                                        child4.measure(MeasureSpec.makeMeasureSpec(child4.getMeasuredWidth(), 1073741824), MeasureSpec.makeMeasureSpec(count, 1073741824));
                                    } else {
                                        totalWeight3 = totalWeight;
                                    }
                                }
                            } else {
                                remainingExcess = maxWidth;
                                i9 = largestChildHeight;
                                totalWeight3 = totalWeight;
                            }
                            i11 = heightMode + 1;
                            i3 = i10;
                            maxWidth = remainingExcess;
                            largestChildHeight = i9;
                            totalWeight = totalWeight3;
                        }
                    }
                    remainingExcess = maxWidth;
                    i9 = largestChildHeight;
                    totalWeight3 = totalWeight;
                    count3 = alternativeMaxWidth;
                    i6 = i;
                    z2 = useLargestChild;
                    i7 = baselineChildIndex;
                    largestChildHeight = childState4;
                    i = widthMeasureSpec;
                    if (!(allFillParent || widthMode == 1073741824)) {
                        maxWidth2 = childState;
                    }
                    setMeasuredDimension(View.resolveSizeAndState(Math.max(maxWidth2 + (this.mPaddingLeft + this.mPaddingRight), getSuggestedMinimumWidth()), i, largestChildHeight), weightedMaxWidth);
                    if (matchWidth) {
                        return;
                    } else {
                        forceUniformWidth(count3, i2);
                        return;
                    }
                }
                float remainingWeightSum = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight3;
                this.mTotalLength = 0;
                float remainingWeightSum2 = remainingWeightSum;
                childState = heightMode;
                largestChildHeight = childState4;
                measuredWidth = remainingExcess;
                i3 = 0;
                while (i3 < alternativeMaxWidth) {
                    View child5 = getVirtualChildAt(i3);
                    if (child5 != null) {
                        z2 = useLargestChild;
                        i7 = baselineChildIndex;
                        if (child5.getVisibility() == 8) {
                            count3 = alternativeMaxWidth;
                            i6 = i;
                            i = widthMeasureSpec;
                        } else {
                            int remainingExcess2;
                            int margin;
                            boolean allFillParent4;
                            int alternativeMaxWidth3;
                            LayoutParams useLargestChild2 = (LayoutParams) child5.getLayoutParams();
                            baselineChildIndex = useLargestChild2.weight;
                            if (baselineChildIndex > 0) {
                                count3 = alternativeMaxWidth;
                                alternativeMaxWidth = (int) ((((float) measuredWidth) * baselineChildIndex) / remainingWeightSum2);
                                measuredWidth -= alternativeMaxWidth;
                                float remainingWeightSum3 = remainingWeightSum2 - baselineChildIndex;
                                if (this.mUseLargestChild != null && i != 1073741824) {
                                    maxWidth = count;
                                } else if (useLargestChild2.height != 0 || (this.mAllowInconsistentMeasurement && i != 1073741824)) {
                                    maxWidth = child5.getMeasuredHeight() + alternativeMaxWidth;
                                } else {
                                    maxWidth = alternativeMaxWidth;
                                }
                                int share = alternativeMaxWidth;
                                remainingExcess2 = measuredWidth;
                                i6 = i;
                                child5.measure(ViewGroup.getChildMeasureSpec(widthMeasureSpec, ((this.mPaddingLeft + this.mPaddingRight) + useLargestChild2.leftMargin) + useLargestChild2.rightMargin, useLargestChild2.width), MeasureSpec.makeMeasureSpec(Math.max(0, maxWidth), 1073741824));
                                largestChildHeight = View.combineMeasuredStates(largestChildHeight, child5.getMeasuredState() & InputDevice.SOURCE_ANY);
                                remainingWeightSum2 = remainingWeightSum3;
                            } else {
                                count3 = alternativeMaxWidth;
                                i6 = i;
                                i = widthMeasureSpec;
                                remainingExcess2 = measuredWidth;
                            }
                            alternativeMaxWidth = useLargestChild2.leftMargin + useLargestChild2.rightMargin;
                            measuredWidth = child5.getMeasuredWidth() + alternativeMaxWidth;
                            maxWidth2 = Math.max(maxWidth2, measuredWidth);
                            float remainingWeightSum4 = remainingWeightSum2;
                            if (widthMode != 1073741824) {
                                margin = alternativeMaxWidth;
                                if (useLargestChild2.width == -1) {
                                    z = true;
                                    alternativeMaxWidth = Math.max(childState, z ? margin : measuredWidth);
                                    if (allFillParent) {
                                    } else {
                                        if (useLargestChild2.width) {
                                            allFillParent4 = true;
                                            maxWidth = this.mTotalLength;
                                            alternativeMaxWidth3 = alternativeMaxWidth;
                                            this.mTotalLength = Math.max(maxWidth, (((maxWidth + child5.getMeasuredHeight()) + useLargestChild2.topMargin) + useLargestChild2.bottomMargin) + getNextLocationOffset(child5));
                                            allFillParent = allFillParent4;
                                            measuredWidth = remainingExcess2;
                                            remainingWeightSum2 = remainingWeightSum4;
                                            childState = alternativeMaxWidth3;
                                        }
                                    }
                                    allFillParent4 = false;
                                    maxWidth = this.mTotalLength;
                                    alternativeMaxWidth3 = alternativeMaxWidth;
                                    this.mTotalLength = Math.max(maxWidth, (((maxWidth + child5.getMeasuredHeight()) + useLargestChild2.topMargin) + useLargestChild2.bottomMargin) + getNextLocationOffset(child5));
                                    allFillParent = allFillParent4;
                                    measuredWidth = remainingExcess2;
                                    remainingWeightSum2 = remainingWeightSum4;
                                    childState = alternativeMaxWidth3;
                                }
                            } else {
                                margin = alternativeMaxWidth;
                            }
                            z = false;
                            if (z) {
                            }
                            alternativeMaxWidth = Math.max(childState, z ? margin : measuredWidth);
                            if (allFillParent) {
                            }
                            allFillParent4 = false;
                            maxWidth = this.mTotalLength;
                            alternativeMaxWidth3 = alternativeMaxWidth;
                            this.mTotalLength = Math.max(maxWidth, (((maxWidth + child5.getMeasuredHeight()) + useLargestChild2.topMargin) + useLargestChild2.bottomMargin) + getNextLocationOffset(child5));
                            allFillParent = allFillParent4;
                            measuredWidth = remainingExcess2;
                            remainingWeightSum2 = remainingWeightSum4;
                            childState = alternativeMaxWidth3;
                        }
                    } else {
                        count3 = alternativeMaxWidth;
                        i6 = i;
                        z2 = useLargestChild;
                        i7 = baselineChildIndex;
                        i = widthMeasureSpec;
                    }
                    i3++;
                    useLargestChild = z2;
                    baselineChildIndex = i7;
                    alternativeMaxWidth = count3;
                    i = i6;
                }
                count3 = alternativeMaxWidth;
                i6 = i;
                z2 = useLargestChild;
                i7 = baselineChildIndex;
                i = widthMeasureSpec;
                this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
                remainingExcess = measuredWidth;
                maxWidth2 = childState;
                setMeasuredDimension(View.resolveSizeAndState(Math.max(maxWidth2 + (this.mPaddingLeft + this.mPaddingRight), getSuggestedMinimumWidth()), i, largestChildHeight), weightedMaxWidth);
                if (matchWidth) {
                }
            }
        }
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == -1) {
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:81:0x0208  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x01f7  */
    /* JADX WARNING: Removed duplicated region for block: B:218:0x053b  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x0505  */
    /* JADX WARNING: Removed duplicated region for block: B:239:0x05ec  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x05e4  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        boolean baselineAligned;
        boolean z;
        LayoutParams lp;
        int i;
        int childBaseline;
        int childState;
        float totalWeight;
        int i2;
        int alternativeMaxHeight;
        int i3;
        int i4;
        int count;
        int i5;
        boolean z2;
        int i6 = widthMeasureSpec;
        int i7 = heightMeasureSpec;
        this.mTotalLength = 0;
        int alternativeMaxHeight2 = 0;
        float totalWeight2 = 0.0f;
        int count2 = getVirtualChildCount();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (this.mMaxAscent == null || this.mMaxDescent == null) {
            this.mMaxAscent = new int[4];
            this.mMaxDescent = new int[4];
        }
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        boolean matchHeight = false;
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent[3] = -1;
        maxDescent[2] = -1;
        maxDescent[1] = -1;
        maxDescent[0] = -1;
        boolean baselineAligned2 = this.mBaselineAligned;
        boolean skippedMeasure = false;
        boolean useLargestChild = this.mUseLargestChild;
        int[] maxDescent2 = maxDescent;
        boolean isExactly = widthMode == 1073741824;
        int usedExcessSpace = 0;
        int nonSkippedChildCount = 0;
        int childState2 = 0;
        int weightedMaxHeight = 0;
        int maxHeight = 0;
        int i8 = 0;
        boolean allFillParent = true;
        int largestChildWidth = Integer.MIN_VALUE;
        while (i8 < count2) {
            View child = getVirtualChildAt(i8);
            int weightedMaxHeight2;
            if (child == null) {
                weightedMaxHeight2 = weightedMaxHeight;
                this.mTotalLength += measureNullChild(i8);
                baselineAligned = baselineAligned2;
                weightedMaxHeight = weightedMaxHeight2;
            } else {
                weightedMaxHeight2 = weightedMaxHeight;
                int alternativeMaxHeight3 = alternativeMaxHeight2;
                if (child.getVisibility() == 8) {
                    i8 += getChildrenSkipCount(child, i8);
                    baselineAligned = baselineAligned2;
                    weightedMaxHeight = weightedMaxHeight2;
                    alternativeMaxHeight2 = alternativeMaxHeight3;
                } else {
                    int maxHeight2;
                    int weightedMaxHeight3;
                    int alternativeMaxHeight4;
                    int i9;
                    int margin;
                    nonSkippedChildCount++;
                    if (hasDividerBeforeChildAt(i8)) {
                        this.mTotalLength += this.mDividerWidth;
                    }
                    LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                    float totalWeight3 = totalWeight2 + lp2.weight;
                    z = lp2.width == 0 && lp2.weight > 0.0f;
                    boolean useExcessSpace = z;
                    int i10;
                    if (widthMode == 1073741824 && useExcessSpace) {
                        if (isExactly) {
                            i10 = i8;
                            this.mTotalLength += lp2.leftMargin + lp2.rightMargin;
                        } else {
                            i10 = i8;
                            i8 = this.mTotalLength;
                            this.mTotalLength = Math.max(i8, (lp2.leftMargin + i8) + lp2.rightMargin);
                        }
                        if (baselineAligned2) {
                            child.measure(MeasureSpec.makeSafeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), 0), MeasureSpec.makeSafeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), 0));
                        } else {
                            skippedMeasure = true;
                        }
                        lp = lp2;
                        maxHeight2 = maxHeight;
                        baselineAligned = baselineAligned2;
                        weightedMaxHeight3 = weightedMaxHeight2;
                        alternativeMaxHeight4 = alternativeMaxHeight3;
                        i9 = i10;
                        i7 = -1;
                    } else {
                        i10 = i8;
                        if (useExcessSpace) {
                            lp2.width = -2;
                        }
                        i9 = i10;
                        weightedMaxHeight3 = weightedMaxHeight2;
                        LayoutParams lp3 = lp2;
                        alternativeMaxHeight4 = alternativeMaxHeight3;
                        maxHeight2 = maxHeight;
                        maxHeight = i6;
                        i6 = largestChildWidth;
                        i = i7;
                        baselineAligned = baselineAligned2;
                        i7 = -1;
                        measureChildBeforeLayout(child, i9, maxHeight, totalWeight3 == 0.0f ? this.mTotalLength : 0, i, false);
                        i8 = child.getMeasuredWidth();
                        if (useExcessSpace) {
                            lp = lp3;
                            lp.width = 0;
                            usedExcessSpace += i8;
                        } else {
                            lp = lp3;
                        }
                        if (isExactly) {
                            this.mTotalLength += ((lp.leftMargin + i8) + lp.rightMargin) + getNextLocationOffset(child);
                        } else {
                            alternativeMaxHeight2 = this.mTotalLength;
                            this.mTotalLength = Math.max(alternativeMaxHeight2, (((alternativeMaxHeight2 + i8) + lp.leftMargin) + lp.rightMargin) + getNextLocationOffset(child));
                        }
                        if (useLargestChild) {
                            largestChildWidth = Math.max(i8, i6);
                        } else {
                            largestChildWidth = i6;
                        }
                    }
                    boolean matchHeightLocally = false;
                    if (heightMode != 1073741824 && lp.height == i7) {
                        matchHeight = true;
                        matchHeightLocally = true;
                    }
                    alternativeMaxHeight2 = lp.topMargin + lp.bottomMargin;
                    maxHeight = child.getMeasuredHeight() + alternativeMaxHeight2;
                    i = View.combineMeasuredStates(childState2, child.getMeasuredState());
                    if (baselineAligned) {
                        childBaseline = child.getBaseline();
                        if (childBaseline != i7) {
                            childState2 = ((((lp.gravity < 0 ? this.mGravity : lp.gravity) & 112) >> 4) & -2) >> 1;
                            maxAscent[childState2] = Math.max(maxAscent[childState2], childBaseline);
                            margin = alternativeMaxHeight2;
                            maxDescent2[childState2] = Math.max(maxDescent2[childState2], maxHeight - childBaseline);
                            alternativeMaxHeight2 = Math.max(maxHeight2, maxHeight);
                            baselineAligned2 = allFillParent && lp.height == -1;
                            if (lp.weight <= 0.0f) {
                                i6 = Math.max(weightedMaxHeight3, matchHeightLocally ? margin : maxHeight);
                                boolean z3 = matchHeightLocally;
                                i8 = alternativeMaxHeight4;
                            } else {
                                i7 = weightedMaxHeight3;
                                i8 = Math.max(alternativeMaxHeight4, matchHeightLocally ? margin : maxHeight);
                                i6 = i7;
                            }
                            i7 = i9;
                            maxHeight = alternativeMaxHeight2;
                            childState2 = i;
                            allFillParent = baselineAligned2;
                            totalWeight2 = totalWeight3;
                            alternativeMaxHeight2 = i8;
                            i8 = i7 + getChildrenSkipCount(child, i7);
                            weightedMaxHeight = i6;
                        }
                    }
                    margin = alternativeMaxHeight2;
                    alternativeMaxHeight2 = Math.max(maxHeight2, maxHeight);
                    if (!allFillParent) {
                    }
                    if (lp.weight <= 0.0f) {
                    }
                    i7 = i9;
                    maxHeight = alternativeMaxHeight2;
                    childState2 = i;
                    allFillParent = baselineAligned2;
                    totalWeight2 = totalWeight3;
                    alternativeMaxHeight2 = i8;
                    i8 = i7 + getChildrenSkipCount(child, i7);
                    weightedMaxHeight = i6;
                }
            }
            i8++;
            baselineAligned2 = baselineAligned;
            i6 = widthMeasureSpec;
            i7 = heightMeasureSpec;
        }
        i7 = weightedMaxHeight;
        i8 = alternativeMaxHeight2;
        alternativeMaxHeight2 = maxHeight;
        i6 = largestChildWidth;
        baselineAligned = baselineAligned2;
        childBaseline = childState2;
        if (nonSkippedChildCount > 0 && hasDividerBeforeChildAt(count2)) {
            this.mTotalLength += this.mDividerWidth;
        }
        if (maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1) {
            childState = childBaseline;
        } else {
            childState = childBaseline;
            alternativeMaxHeight2 = Math.max(alternativeMaxHeight2, Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2]))) + Math.max(maxDescent2[3], Math.max(maxDescent2[0], Math.max(maxDescent2[1], maxDescent2[2]))));
        }
        if (useLargestChild && (widthMode == Integer.MIN_VALUE || widthMode == 0)) {
            this.mTotalLength = 0;
            weightedMaxHeight = 0;
            while (weightedMaxHeight < count2) {
                int i11;
                View child2 = getVirtualChildAt(weightedMaxHeight);
                if (child2 == null) {
                    this.mTotalLength += measureNullChild(weightedMaxHeight);
                    i11 = weightedMaxHeight;
                } else if (child2.getVisibility() == 8) {
                    weightedMaxHeight += getChildrenSkipCount(child2, weightedMaxHeight);
                    weightedMaxHeight++;
                } else {
                    if (this.mHwActionBarTabLayoutUsed && hasDividerBeforeChildAt(weightedMaxHeight)) {
                        this.mTotalLength += this.mDividerWidth;
                    }
                    LayoutParams lp4 = (LayoutParams) child2.getLayoutParams();
                    if (isExactly) {
                        i11 = weightedMaxHeight;
                        this.mTotalLength += ((lp4.leftMargin + i6) + lp4.rightMargin) + getNextLocationOffset(child2);
                    } else {
                        i11 = weightedMaxHeight;
                        weightedMaxHeight = this.mTotalLength;
                        this.mTotalLength = Math.max(weightedMaxHeight, (((weightedMaxHeight + i6) + lp4.leftMargin) + lp4.rightMargin) + getNextLocationOffset(child2));
                    }
                }
                weightedMaxHeight = i11;
                weightedMaxHeight++;
            }
        }
        this.mTotalLength += this.mPaddingLeft + this.mPaddingRight;
        largestChildWidth = i6;
        i6 = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumWidth()), widthMeasureSpec, 0);
        weightedMaxHeight = i6 & 16777215;
        childBaseline = (weightedMaxHeight - this.mTotalLength) + (this.mAllowInconsistentMeasurement ? 0 : usedExcessSpace);
        int i12;
        int i13;
        if (skippedMeasure) {
            i12 = alternativeMaxHeight2;
            totalWeight = totalWeight2;
        } else if ((sRemeasureWeightedChildren || childBaseline != 0) && totalWeight2 > 0.0f) {
            i13 = weightedMaxHeight;
            i12 = alternativeMaxHeight2;
            totalWeight = totalWeight2;
        } else {
            i8 = Math.max(i8, i7);
            if (useLargestChild && widthMode != 1073741824) {
                int i14 = 0;
                while (true) {
                    i2 = i14;
                    if (i2 >= count2) {
                        break;
                    }
                    alternativeMaxHeight = i8;
                    View child3 = getVirtualChildAt(i2);
                    if (child3 != null) {
                        i13 = weightedMaxHeight;
                        i12 = alternativeMaxHeight2;
                        if (child3.getVisibility() == 8) {
                            totalWeight = totalWeight2;
                        } else {
                            lp = (LayoutParams) child3.getLayoutParams();
                            float childExtra = lp.weight;
                            if (childExtra > 0.0f) {
                                LayoutParams lp5 = lp;
                                totalWeight = totalWeight2;
                                child3.measure(MeasureSpec.makeMeasureSpec(largestChildWidth, 1073741824), MeasureSpec.makeMeasureSpec(child3.getMeasuredHeight(), 1073741824));
                            } else {
                                totalWeight = totalWeight2;
                            }
                        }
                    } else {
                        i13 = weightedMaxHeight;
                        i12 = alternativeMaxHeight2;
                        totalWeight = totalWeight2;
                    }
                    i14 = i2 + 1;
                    i8 = alternativeMaxHeight;
                    weightedMaxHeight = i13;
                    alternativeMaxHeight2 = i12;
                    totalWeight2 = totalWeight;
                }
            }
            alternativeMaxHeight = i8;
            i13 = weightedMaxHeight;
            i12 = alternativeMaxHeight2;
            totalWeight = totalWeight2;
            i3 = largestChildWidth;
            alternativeMaxHeight2 = childBaseline;
            i4 = i7;
            count = count2;
            i5 = widthMode;
            z2 = useLargestChild;
            i2 = i12;
            count2 = heightMeasureSpec;
            if (!(allFillParent || heightMode == 1073741824)) {
                i2 = alternativeMaxHeight;
            }
            setMeasuredDimension((childState & -16777216) | i6, View.resolveSizeAndState(Math.max(i2 + (this.mPaddingTop + this.mPaddingBottom), getSuggestedMinimumHeight()), count2, childState << 16));
            if (matchHeight) {
                weightedMaxHeight = widthMeasureSpec;
                return;
            }
            forceUniformHeight(count, widthMeasureSpec);
            return;
        }
        float remainingWeightSum = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight;
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent2[3] = -1;
        maxDescent2[2] = -1;
        maxDescent2[1] = -1;
        maxDescent2[0] = -1;
        i2 = -1;
        this.mTotalLength = 0;
        alternativeMaxHeight2 = childBaseline;
        i = childState;
        childBaseline = i8;
        i8 = 0;
        while (i8 < count2) {
            i4 = i7;
            View child4 = getVirtualChildAt(i8);
            if (child4 != null) {
                z2 = useLargestChild;
                if (child4.getVisibility() == 8) {
                    i3 = largestChildWidth;
                    count = count2;
                    i5 = widthMode;
                    count2 = heightMeasureSpec;
                } else {
                    float remainingWeightSum2;
                    int remainingExcess;
                    boolean allFillParent2;
                    if (hasDividerBeforeChildAt(i8)) {
                        this.mTotalLength += this.mDividerWidth;
                    }
                    LayoutParams lp6 = (LayoutParams) child4.getLayoutParams();
                    useLargestChild = lp6.weight;
                    if (useLargestChild <= false) {
                        count = count2;
                        count2 = (int) ((((float) alternativeMaxHeight2) * useLargestChild) / remainingWeightSum);
                        alternativeMaxHeight2 -= count2;
                        remainingWeightSum2 = remainingWeightSum - useLargestChild;
                        if (this.mUseLargestChild != null && widthMode != 1073741824) {
                            weightedMaxHeight = largestChildWidth;
                        } else if (lp6.width != 0 || (this.mAllowInconsistentMeasurement && widthMode != 1073741824)) {
                            weightedMaxHeight = child4.getMeasuredWidth() + count2;
                        } else {
                            weightedMaxHeight = count2;
                        }
                        remainingExcess = alternativeMaxHeight2;
                        i3 = largestChildWidth;
                        i5 = widthMode;
                        widthMode = -1;
                        child4.measure(MeasureSpec.makeMeasureSpec(Math.max(0, weightedMaxHeight), 1073741824), ViewGroup.getChildMeasureSpec(heightMeasureSpec, ((this.mPaddingTop + this.mPaddingBottom) + lp6.topMargin) + lp6.bottomMargin, lp6.height));
                        i = View.combineMeasuredStates(i, child4.getMeasuredState() & -16777216);
                    } else {
                        i3 = largestChildWidth;
                        count = count2;
                        i5 = widthMode;
                        count2 = heightMeasureSpec;
                        widthMode = -1;
                        remainingWeightSum2 = remainingWeightSum;
                        remainingExcess = alternativeMaxHeight2;
                    }
                    if (isExactly) {
                        this.mTotalLength += ((child4.getMeasuredWidth() + lp6.leftMargin) + lp6.rightMargin) + getNextLocationOffset(child4);
                    } else {
                        weightedMaxHeight = this.mTotalLength;
                        this.mTotalLength = Math.max(weightedMaxHeight, (((child4.getMeasuredWidth() + weightedMaxHeight) + lp6.leftMargin) + lp6.rightMargin) + getNextLocationOffset(child4));
                    }
                    z = heightMode != 1073741824 && lp6.height == widthMode;
                    alternativeMaxHeight2 = lp6.topMargin + lp6.bottomMargin;
                    largestChildWidth = child4.getMeasuredHeight() + alternativeMaxHeight2;
                    i2 = Math.max(i2, largestChildWidth);
                    childBaseline = Math.max(childBaseline, z ? alternativeMaxHeight2 : largestChildWidth);
                    if (allFillParent) {
                        if (lp6.height) {
                            z = true;
                            if (baselineAligned) {
                                allFillParent2 = z;
                            } else {
                                boolean widthMode2 = child4.getBaseline();
                                allFillParent2 = z;
                                if (!widthMode2) {
                                    z = (lp6.gravity >= false ? this.mGravity : lp6.gravity) & 112;
                                    int index = ((z >> 4) & -2) >> 1;
                                    boolean gravity = z;
                                    maxAscent[index] = Math.max(maxAscent[index], widthMode2);
                                    maxDescent2[index] = Math.max(maxDescent2[index], largestChildWidth - widthMode2);
                                }
                            }
                            remainingWeightSum = remainingWeightSum2;
                            alternativeMaxHeight2 = remainingExcess;
                            allFillParent = allFillParent2;
                        }
                    }
                    z = false;
                    if (baselineAligned) {
                    }
                    remainingWeightSum = remainingWeightSum2;
                    alternativeMaxHeight2 = remainingExcess;
                    allFillParent = allFillParent2;
                }
            } else {
                i3 = largestChildWidth;
                count = count2;
                i5 = widthMode;
                z2 = useLargestChild;
                count2 = heightMeasureSpec;
            }
            i8++;
            i7 = i4;
            useLargestChild = z2;
            count2 = count;
            largestChildWidth = i3;
            widthMode = i5;
            maxHeight = widthMeasureSpec;
        }
        i4 = i7;
        count = count2;
        i5 = widthMode;
        z2 = useLargestChild;
        count2 = heightMeasureSpec;
        this.mTotalLength += this.mPaddingLeft + this.mPaddingRight;
        if (!(maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1)) {
            i2 = Math.max(i2, Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2]))) + Math.max(maxDescent2[3], Math.max(maxDescent2[0], Math.max(maxDescent2[1], maxDescent2[2]))));
        }
        childState = i;
        alternativeMaxHeight = childBaseline;
        i2 = alternativeMaxHeight;
        setMeasuredDimension((childState & -16777216) | i6, View.resolveSizeAndState(Math.max(i2 + (this.mPaddingTop + this.mPaddingBottom), getSuggestedMinimumHeight()), count2, childState << 16));
        if (matchHeight) {
        }
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == -1) {
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
    }

    int getChildrenSkipCount(View child, int index) {
        return 0;
    }

    int measureNullChild(int childIndex) {
        return 0;
    }

    void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec, int totalHeight) {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    int getLocationOffset(View child) {
        return 0;
    }

    int getNextLocationOffset(View child) {
        return 0;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mOrientation == 1) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    void layoutVertical(int left, int top, int right, int bottom) {
        int childTop;
        int paddingLeft = this.mPaddingLeft;
        int width = right - left;
        int childRight = width - this.mPaddingRight;
        int childSpace = (width - paddingLeft) - this.mPaddingRight;
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & 112;
        int minorGravity = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if (majorGravity == 16) {
            childTop = this.mPaddingTop + (((bottom - top) - this.mTotalLength) / 2);
        } else if (majorGravity != 80) {
            childTop = this.mPaddingTop;
        } else {
            childTop = ((this.mPaddingTop + bottom) - top) - this.mTotalLength;
        }
        int i = 0;
        while (true) {
            int i2 = i;
            int paddingLeft2;
            if (i2 < count) {
                int majorGravity2;
                View child = getVirtualChildAt(i2);
                if (child == null) {
                    childTop += measureNullChild(i2);
                    majorGravity2 = majorGravity;
                    paddingLeft2 = paddingLeft;
                } else if (child.getVisibility() != 8) {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int gravity = lp.gravity;
                    if (gravity < 0) {
                        gravity = minorGravity;
                    }
                    int layoutDirection = getLayoutDirection();
                    int gravity2 = gravity;
                    gravity = Gravity.getAbsoluteGravity(gravity, layoutDirection) & 7;
                    majorGravity2 = majorGravity;
                    gravity = gravity != 1 ? gravity != 5 ? lp.leftMargin + paddingLeft : (childRight - childWidth) - lp.rightMargin : ((((childSpace - childWidth) / 2) + paddingLeft) + lp.leftMargin) - lp.rightMargin;
                    if (hasDividerBeforeChildAt(i2) != 0) {
                        childTop += this.mDividerHeight;
                    }
                    gravity2 = childTop + lp.topMargin;
                    LayoutParams lp2 = lp;
                    View child2 = child;
                    paddingLeft2 = paddingLeft;
                    paddingLeft = i2;
                    setChildFrame(child, gravity, gravity2 + getLocationOffset(child), childWidth, childHeight);
                    i2 = paddingLeft + getChildrenSkipCount(child2, paddingLeft);
                    childTop = gravity2 + ((childHeight + lp2.bottomMargin) + getNextLocationOffset(child2));
                } else {
                    majorGravity2 = majorGravity;
                    paddingLeft2 = paddingLeft;
                    paddingLeft = i2;
                }
                i = i2 + 1;
                majorGravity = majorGravity2;
                paddingLeft = paddingLeft2;
            } else {
                paddingLeft2 = paddingLeft;
                return;
            }
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (layoutDirection != this.mLayoutDirection) {
            this.mLayoutDirection = layoutDirection;
            if (this.mOrientation == 0) {
                requestLayout();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x00b7  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00c3  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0102  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void layoutHorizontal(int left, int top, int right, int bottom) {
        boolean isLayoutRtl = isLayoutRtl();
        int paddingTop = this.mPaddingTop;
        int height = bottom - top;
        int childBottom = height - this.mPaddingBottom;
        int childSpace = (height - paddingTop) - this.mPaddingBottom;
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int minorGravity = this.mGravity & 112;
        boolean baselineAligned = this.mBaselineAligned;
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        int layoutDirection = getLayoutDirection();
        int absoluteGravity = Gravity.getAbsoluteGravity(majorGravity, layoutDirection);
        if (absoluteGravity != 1) {
            if (absoluteGravity != 5) {
                absoluteGravity = this.mPaddingLeft;
            } else {
                absoluteGravity = ((this.mPaddingLeft + right) - left) - this.mTotalLength;
            }
        } else {
            int i = layoutDirection;
            absoluteGravity = this.mPaddingLeft + (((right - left) - this.mTotalLength) / 2);
        }
        layoutDirection = absoluteGravity;
        absoluteGravity = 0;
        int dir = 1;
        if (isLayoutRtl) {
            absoluteGravity = count - 1;
            dir = -1;
        }
        int i2 = 0;
        int childLeft = layoutDirection;
        while (true) {
            layoutDirection = i2;
            int[] maxAscent2;
            boolean baselineAligned2;
            int majorGravity2;
            int count2;
            boolean isLayoutRtl2;
            if (layoutDirection < count) {
                int[] maxDescent2;
                int childIndex = absoluteGravity + (dir * layoutDirection);
                View child = getVirtualChildAt(childIndex);
                if (child == null) {
                    childLeft += measureNullChild(childIndex);
                    maxDescent2 = maxDescent;
                    maxAscent2 = maxAscent;
                    baselineAligned2 = baselineAligned;
                    majorGravity2 = majorGravity;
                    count2 = count;
                    isLayoutRtl2 = isLayoutRtl;
                } else {
                    int i3 = layoutDirection;
                    majorGravity2 = majorGravity;
                    if (child.getVisibility() != 8) {
                        int childBaseline;
                        i2 = child.getMeasuredWidth();
                        int childHeight = child.getMeasuredHeight();
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        int childBaseline2 = -1;
                        if (baselineAligned) {
                            baselineAligned2 = baselineAligned;
                            if (!lp.height) {
                                childBaseline = child.getBaseline();
                                layoutDirection = lp.gravity;
                                if (layoutDirection < 0) {
                                    layoutDirection = minorGravity;
                                }
                                layoutDirection &= 112;
                                count2 = count;
                                if (layoutDirection != 16) {
                                    layoutDirection = ((((childSpace - childHeight) / 2) + paddingTop) + lp.topMargin) - lp.bottomMargin;
                                } else if (layoutDirection == 48) {
                                    layoutDirection = lp.topMargin + paddingTop;
                                    if (childBaseline != -1) {
                                        layoutDirection += maxAscent[1] - childBaseline;
                                    }
                                } else if (layoutDirection != 80) {
                                    layoutDirection = paddingTop;
                                } else {
                                    layoutDirection = (childBottom - childHeight) - lp.bottomMargin;
                                    if (childBaseline != -1) {
                                        layoutDirection -= maxDescent[2] - (child.getMeasuredHeight() - childBaseline);
                                    }
                                }
                                if (hasDividerBeforeChildAt(childIndex)) {
                                    childLeft += this.mDividerWidth;
                                }
                                childLeft += lp.leftMargin;
                                maxDescent2 = maxDescent;
                                maxAscent2 = maxAscent;
                                isLayoutRtl2 = isLayoutRtl;
                                isLayoutRtl = lp;
                                setChildFrame(child, childLeft + getLocationOffset(child), layoutDirection, i2, childHeight);
                                childLeft += (i2 + isLayoutRtl.rightMargin) + getNextLocationOffset(child);
                                layoutDirection = i3 + getChildrenSkipCount(child, childIndex);
                            }
                        } else {
                            baselineAligned2 = baselineAligned;
                        }
                        childBaseline = childBaseline2;
                        layoutDirection = lp.gravity;
                        if (layoutDirection < 0) {
                        }
                        layoutDirection &= 112;
                        count2 = count;
                        if (layoutDirection != 16) {
                        }
                        if (hasDividerBeforeChildAt(childIndex)) {
                        }
                        childLeft += lp.leftMargin;
                        maxDescent2 = maxDescent;
                        maxAscent2 = maxAscent;
                        isLayoutRtl2 = isLayoutRtl;
                        isLayoutRtl = lp;
                        setChildFrame(child, childLeft + getLocationOffset(child), layoutDirection, i2, childHeight);
                        childLeft += (i2 + isLayoutRtl.rightMargin) + getNextLocationOffset(child);
                        layoutDirection = i3 + getChildrenSkipCount(child, childIndex);
                    } else {
                        maxDescent2 = maxDescent;
                        maxAscent2 = maxAscent;
                        baselineAligned2 = baselineAligned;
                        count2 = count;
                        isLayoutRtl2 = isLayoutRtl;
                        layoutDirection = i3;
                    }
                }
                i2 = layoutDirection + 1;
                majorGravity = majorGravity2;
                baselineAligned = baselineAligned2;
                maxDescent = maxDescent2;
                count = count2;
                maxAscent = maxAscent2;
                isLayoutRtl = isLayoutRtl2;
            } else {
                maxAscent2 = maxAscent;
                baselineAligned2 = baselineAligned;
                majorGravity2 = majorGravity;
                count2 = count;
                isLayoutRtl2 = isLayoutRtl;
                return;
            }
        }
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    @RemotableViewMethod
    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            if ((Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK & gravity) == 0) {
                gravity |= Gravity.START;
            }
            if ((gravity & 112) == 0) {
                gravity |= 48;
            }
            this.mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    @RemotableViewMethod
    public void setHorizontalGravity(int horizontalGravity) {
        int gravity = horizontalGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK & this.mGravity) != gravity) {
            this.mGravity = (this.mGravity & -8388616) | gravity;
            requestLayout();
        }
    }

    @RemotableViewMethod
    public void setVerticalGravity(int verticalGravity) {
        int gravity = verticalGravity & 112;
        if ((this.mGravity & 112) != gravity) {
            this.mGravity = (this.mGravity & -113) | gravity;
            requestLayout();
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -2);
        }
        if (this.mOrientation == 1) {
            return new LayoutParams(-1, -2);
        }
        return null;
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
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

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public CharSequence getAccessibilityClassName() {
        return LinearLayout.class.getName();
    }

    protected void encodeProperties(ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);
        encoder.addProperty("layout:baselineAligned", this.mBaselineAligned);
        encoder.addProperty("layout:baselineAlignedChildIndex", this.mBaselineAlignedChildIndex);
        encoder.addProperty("measurement:baselineChildTop", this.mBaselineChildTop);
        encoder.addProperty("measurement:orientation", this.mOrientation);
        encoder.addProperty("measurement:gravity", this.mGravity);
        encoder.addProperty("measurement:totalLength", this.mTotalLength);
        encoder.addProperty("layout:totalLength", this.mTotalLength);
        encoder.addProperty("layout:useLargestChild", this.mUseLargestChild);
    }
}
