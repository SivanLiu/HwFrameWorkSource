package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.RemoteViews.RemoteView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;

@RemoteView
public class NotificationActionListLayout extends LinearLayout {
    public static final Comparator<Pair<Integer, TextView>> MEASURE_ORDER_COMPARATOR = -$$Lambda$NotificationActionListLayout$uFZFEmIEBpI3kn6c3tNvvgmMSv8.INSTANCE;
    private int mDefaultPaddingBottom;
    private int mDefaultPaddingTop;
    private int mEmphasizedHeight;
    private boolean mEmphasizedMode;
    private final int mGravity;
    private ArrayList<View> mMeasureOrderOther;
    private ArrayList<Pair<Integer, TextView>> mMeasureOrderTextViews;
    private int mRegularHeight;
    private int mTotalWidth;

    public NotificationActionListLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationActionListLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationActionListLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTotalWidth = 0;
        this.mMeasureOrderTextViews = new ArrayList();
        this.mMeasureOrderOther = new ArrayList();
        TypedArray ta = context.obtainStyledAttributes(attrs, new int[]{16842927}, defStyleAttr, defStyleRes);
        this.mGravity = ta.getInt(0, 0);
        ta.recycle();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mEmphasizedMode) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int i;
        boolean needRebuild;
        int N = getChildCount();
        int i2 = 0;
        int textViews = 0;
        int otherViews = 0;
        int notGoneChildren = 0;
        int i3 = 0;
        while (true) {
            i = 8;
            if (i3 >= N) {
                break;
            }
            View c = getChildAt(i3);
            if (c instanceof TextView) {
                textViews++;
            } else {
                otherViews++;
            }
            if (c.getVisibility() != 8) {
                notGoneChildren++;
            }
            i3++;
        }
        boolean needRebuild2 = false;
        if (!(textViews == this.mMeasureOrderTextViews.size() && otherViews == this.mMeasureOrderOther.size())) {
            needRebuild2 = true;
        }
        if (needRebuild2) {
            needRebuild = needRebuild2;
        } else {
            boolean size = this.mMeasureOrderTextViews.size();
            boolean needRebuild3 = needRebuild2;
            for (needRebuild2 = false; needRebuild2 < size; needRebuild2++) {
                Pair<Integer, TextView> pair = (Pair) this.mMeasureOrderTextViews.get(needRebuild2);
                if (((Integer) pair.first).intValue() != ((TextView) pair.second).getText().length()) {
                    needRebuild3 = true;
                }
            }
            needRebuild = needRebuild3;
        }
        if (needRebuild) {
            rebuildMeasureOrder(textViews, otherViews);
        }
        boolean constrained = MeasureSpec.getMode(widthMeasureSpec) != 0;
        int innerWidth = (MeasureSpec.getSize(widthMeasureSpec) - this.mPaddingLeft) - this.mPaddingRight;
        int otherSize = this.mMeasureOrderOther.size();
        int usedWidth = 0;
        int measuredChildren = 0;
        while (true) {
            int i4 = i2;
            if (i4 < N) {
                View c2;
                int i5;
                int N2;
                if (i4 < otherSize) {
                    c2 = (View) this.mMeasureOrderOther.get(i4);
                } else {
                    c2 = (View) ((Pair) this.mMeasureOrderTextViews.get(i4 - otherSize)).second;
                }
                View c3 = c2;
                if (c3.getVisibility() == i) {
                    i5 = i4;
                    N2 = N;
                } else {
                    MarginLayoutParams lp = (MarginLayoutParams) c3.getLayoutParams();
                    i3 = usedWidth;
                    if (constrained) {
                        i3 = innerWidth - ((innerWidth - usedWidth) / (notGoneChildren - measuredChildren));
                    }
                    MarginLayoutParams lp2 = lp;
                    N2 = N;
                    N = c3;
                    i5 = i4;
                    measureChildWithMargins(c3, widthMeasureSpec, i3, heightMeasureSpec, 0);
                    usedWidth += (N.getMeasuredWidth() + lp2.rightMargin) + lp2.leftMargin;
                    measuredChildren++;
                }
                i2 = i5 + 1;
                N = N2;
                i = 8;
            } else {
                this.mTotalWidth = (usedWidth + this.mPaddingRight) + this.mPaddingLeft;
                setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec));
                return;
            }
        }
    }

    private void rebuildMeasureOrder(int capacityText, int capacityOther) {
        clearMeasureOrder();
        this.mMeasureOrderTextViews.ensureCapacity(capacityText);
        this.mMeasureOrderOther.ensureCapacity(capacityOther);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View c = getChildAt(i);
            if (!(c instanceof TextView) || ((TextView) c).getText().length() <= 0) {
                this.mMeasureOrderOther.add(c);
            } else {
                this.mMeasureOrderTextViews.add(Pair.create(Integer.valueOf(((TextView) c).getText().length()), (TextView) c));
            }
        }
        this.mMeasureOrderTextViews.sort(MEASURE_ORDER_COMPARATOR);
    }

    private void clearMeasureOrder() {
        this.mMeasureOrderOther.clear();
        this.mMeasureOrderTextViews.clear();
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        clearMeasureOrder();
        if (child.getBackground() instanceof RippleDrawable) {
            ((RippleDrawable) child.getBackground()).setForceSoftware(true);
        }
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        clearMeasureOrder();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        NotificationActionListLayout notificationActionListLayout = this;
        if (notificationActionListLayout.mEmphasizedMode) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        int childLeft;
        int paddingTop;
        boolean centerAligned;
        boolean isLayoutRtl = isLayoutRtl();
        int paddingTop2 = notificationActionListLayout.mPaddingTop;
        boolean z = true;
        int i = 0;
        if ((notificationActionListLayout.mGravity & 1) == 0) {
            z = false;
        }
        boolean centerAligned2 = z;
        if (centerAligned2) {
            childLeft = ((notificationActionListLayout.mPaddingLeft + left) + ((right - left) / 2)) - (notificationActionListLayout.mTotalWidth / 2);
        } else {
            childLeft = notificationActionListLayout.mPaddingLeft;
            if (Gravity.getAbsoluteGravity(8388611, getLayoutDirection()) == 5) {
                childLeft += (right - left) - notificationActionListLayout.mTotalWidth;
            }
        }
        int innerHeight = ((bottom - top) - paddingTop2) - notificationActionListLayout.mPaddingBottom;
        int count = getChildCount();
        int start = 0;
        int dir = 1;
        if (isLayoutRtl) {
            start = count - 1;
            dir = -1;
        }
        while (i < count) {
            boolean isLayoutRtl2;
            View child = notificationActionListLayout.getChildAt((dir * i) + start);
            if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                isLayoutRtl2 = isLayoutRtl;
                MarginLayoutParams isLayoutRtl3 = (MarginLayoutParams) child.getLayoutParams();
                paddingTop = paddingTop2;
                paddingTop2 = ((paddingTop2 + ((innerHeight - childHeight) / 2)) + isLayoutRtl3.topMargin) - isLayoutRtl3.bottomMargin;
                centerAligned = centerAligned2;
                childLeft += isLayoutRtl3.leftMargin;
                child.layout(childLeft, paddingTop2, childLeft + childWidth, paddingTop2 + childHeight);
                childLeft += isLayoutRtl3.rightMargin + childWidth;
            } else {
                isLayoutRtl2 = isLayoutRtl;
                paddingTop = paddingTop2;
                centerAligned = centerAligned2;
            }
            i++;
            isLayoutRtl = isLayoutRtl2;
            paddingTop2 = paddingTop;
            centerAligned2 = centerAligned;
            notificationActionListLayout = this;
        }
        paddingTop = paddingTop2;
        centerAligned = centerAligned2;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDefaultPaddingBottom = getPaddingBottom();
        this.mDefaultPaddingTop = getPaddingTop();
        updateHeights();
    }

    private void updateHeights() {
        this.mEmphasizedHeight = (getResources().getDimensionPixelSize(17105206) + getResources().getDimensionPixelSize(17105205)) + getResources().getDimensionPixelSize(17105194);
        this.mRegularHeight = getResources().getDimensionPixelSize(17105195);
    }

    @RemotableViewMethod
    public void setEmphasizedMode(boolean emphasizedMode) {
        int paddingTop;
        int height;
        this.mEmphasizedMode = emphasizedMode;
        if (emphasizedMode) {
            paddingTop = getResources().getDimensionPixelSize(17105205);
            int paddingBottom = getResources().getDimensionPixelSize(17105206);
            height = this.mEmphasizedHeight;
            int buttonPaddingInternal = getResources().getDimensionPixelSize(17104942);
            setPaddingRelative(getPaddingStart(), paddingTop - buttonPaddingInternal, getPaddingEnd(), paddingBottom - buttonPaddingInternal);
        } else {
            setPaddingRelative(getPaddingStart(), this.mDefaultPaddingTop, getPaddingEnd(), this.mDefaultPaddingBottom);
            height = this.mRegularHeight;
        }
        paddingTop = height;
        LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = paddingTop;
        setLayoutParams(layoutParams);
    }

    public int getExtraMeasureHeight() {
        if (this.mEmphasizedMode) {
            return this.mEmphasizedHeight - this.mRegularHeight;
        }
        return 0;
    }
}
