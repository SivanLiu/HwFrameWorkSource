package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.android.internal.R;
import com.android.internal.util.Protocol;

public class ActionBarContainer extends FrameLayout {
    private View mActionBarView;
    private View mActionContextView;
    private boolean mAnimationEnabled;
    private Drawable mBackground;
    protected boolean mForcedPrimaryBackground;
    protected boolean mForcedSplitBackground;
    protected boolean mForcedStackedBackground;
    private int mHeight;
    private boolean mIsSplit;
    private boolean mIsStacked;
    private boolean mIsTransitioning;
    private Drawable mSplitBackground;
    private Drawable mStackedBackground;
    private View mTabContainer;

    private class ActionBarBackgroundDrawable extends Drawable {
        private ActionBarBackgroundDrawable() {
        }

        public void draw(Canvas canvas) {
            if (!ActionBarContainer.this.mIsSplit) {
                if (ActionBarContainer.this.mBackground != null) {
                    ActionBarContainer.this.mBackground.draw(canvas);
                }
                if (ActionBarContainer.this.mStackedBackground != null && ActionBarContainer.this.mIsStacked) {
                    ActionBarContainer.this.mStackedBackground.draw(canvas);
                }
            } else if (ActionBarContainer.this.mSplitBackground != null) {
                ActionBarContainer.this.mSplitBackground.draw(canvas);
            }
        }

        public void getOutline(Outline outline) {
            if (ActionBarContainer.this.mIsSplit) {
                if (ActionBarContainer.this.mSplitBackground != null) {
                    ActionBarContainer.this.mSplitBackground.getOutline(outline);
                }
            } else if (ActionBarContainer.this.mBackground != null) {
                ActionBarContainer.this.mBackground.getOutline(outline);
            }
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        /* JADX WARNING: Missing block: B:13:0x003b, code skipped:
            return 0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int getOpacity() {
            if (ActionBarContainer.this.mIsSplit) {
                return (ActionBarContainer.this.mSplitBackground == null || ActionBarContainer.this.mSplitBackground.getOpacity() != -1) ? 0 : -1;
            } else {
                if ((!ActionBarContainer.this.mIsStacked || (ActionBarContainer.this.mStackedBackground != null && ActionBarContainer.this.mStackedBackground.getOpacity() == -1)) && !ActionBarContainer.isCollapsed(ActionBarContainer.this.mActionBarView) && ActionBarContainer.this.mBackground != null && ActionBarContainer.this.mBackground.getOpacity() == -1) {
                    return -1;
                }
            }
        }
    }

    public ActionBarContainer(Context context) {
        this(context, null);
    }

    public ActionBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean z = true;
        this.mAnimationEnabled = true;
        setBackground(new ActionBarBackgroundDrawable());
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionBar);
        this.mBackground = a.getDrawable(2);
        this.mStackedBackground = a.getDrawable(18);
        this.mHeight = a.getDimensionPixelSize(4, -1);
        if (getId() == 16909357) {
            this.mIsSplit = true;
            this.mSplitBackground = a.getDrawable(19);
        }
        a.recycle();
        if (this.mIsSplit ? this.mSplitBackground != null : !(this.mBackground == null && this.mStackedBackground == null)) {
            z = false;
        }
        setWillNotDraw(z);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mActionBarView = findViewById(16908687);
        this.mActionContextView = findViewById(16908692);
    }

    public void setPrimaryBackground(Drawable bg) {
        if (this.mBackground != null) {
            this.mBackground.setCallback(null);
            unscheduleDrawable(this.mBackground);
        }
        this.mBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (this.mActionBarView != null) {
                this.mBackground.setBounds(this.mActionBarView.getLeft(), this.mActionBarView.getTop(), this.mActionBarView.getRight(), this.mActionBarView.getBottom());
            }
        }
        boolean z = false;
        if (this.mIsSplit ? this.mSplitBackground != null : !(this.mBackground == null && this.mStackedBackground == null)) {
            z = true;
        }
        setWillNotDraw(z);
        invalidate();
    }

    public void setStackedBackground(Drawable bg) {
        if (this.mStackedBackground != null) {
            this.mStackedBackground.setCallback(null);
            unscheduleDrawable(this.mStackedBackground);
        }
        this.mStackedBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (!(!this.mIsStacked || this.mStackedBackground == null || this.mTabContainer == null)) {
                this.mStackedBackground.setBounds(this.mTabContainer.getLeft(), this.mTabContainer.getTop(), this.mTabContainer.getRight(), this.mTabContainer.getBottom());
            }
        }
        boolean z = false;
        if (this.mIsSplit ? this.mSplitBackground != null : !(this.mBackground == null && this.mStackedBackground == null)) {
            z = true;
        }
        setWillNotDraw(z);
        invalidate();
    }

    public void setSplitBackground(Drawable bg) {
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setCallback(null);
            unscheduleDrawable(this.mSplitBackground);
        }
        this.mSplitBackground = bg;
        boolean z = false;
        if (bg != null) {
            bg.setCallback(this);
            if (this.mIsSplit && this.mSplitBackground != null) {
                this.mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
        if (this.mIsSplit ? this.mSplitBackground != null : !(this.mBackground == null && this.mStackedBackground == null)) {
            z = true;
        }
        setWillNotDraw(z);
        invalidate();
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean isVisible = visibility == 0;
        if (this.mBackground != null) {
            this.mBackground.setVisible(isVisible, false);
        }
        if (this.mStackedBackground != null) {
            this.mStackedBackground.setVisible(isVisible, false);
        }
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setVisible(isVisible, false);
        }
    }

    protected boolean verifyDrawable(Drawable who) {
        return (who == this.mBackground && !this.mIsSplit) || ((who == this.mStackedBackground && this.mIsStacked) || ((who == this.mSplitBackground && this.mIsSplit) || super.verifyDrawable(who)));
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] state = getDrawableState();
        boolean changed = false;
        Drawable background = this.mBackground;
        if (background != null && background.isStateful()) {
            changed = false | background.setState(state);
        }
        Drawable stackedBackground = this.mStackedBackground;
        if (stackedBackground != null && stackedBackground.isStateful()) {
            changed |= stackedBackground.setState(state);
        }
        Drawable splitBackground = this.mSplitBackground;
        if (splitBackground != null && splitBackground.isStateful()) {
            changed |= splitBackground.setState(state);
        }
        if (changed) {
            invalidate();
        }
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mBackground != null) {
            this.mBackground.jumpToCurrentState();
        }
        if (this.mStackedBackground != null) {
            this.mStackedBackground.jumpToCurrentState();
        }
        if (this.mSplitBackground != null) {
            this.mSplitBackground.jumpToCurrentState();
        }
    }

    public void onResolveDrawables(int layoutDirection) {
        super.onResolveDrawables(layoutDirection);
        if (this.mBackground != null) {
            this.mBackground.setLayoutDirection(layoutDirection);
        }
        if (this.mStackedBackground != null) {
            this.mStackedBackground.setLayoutDirection(layoutDirection);
        }
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setLayoutDirection(layoutDirection);
        }
    }

    public void setTransitioning(boolean isTransitioning) {
        int i;
        this.mIsTransitioning = isTransitioning;
        if (isTransitioning) {
            i = Protocol.BASE_NSD_MANAGER;
        } else {
            i = 262144;
        }
        setDescendantFocusability(i);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mIsTransitioning || super.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        return true;
    }

    public boolean onHoverEvent(MotionEvent ev) {
        super.onHoverEvent(ev);
        return true;
    }

    public void setTabContainer(ScrollingTabContainerView tabView) {
        if (this.mTabContainer != null) {
            removeView(this.mTabContainer);
        }
        this.mTabContainer = tabView;
        if (tabView != null) {
            addView(tabView);
            LayoutParams lp = tabView.getLayoutParams();
            lp.width = -1;
            lp.height = -2;
            tabView.setAllowCollapse(false);
        }
    }

    public void setSplitViewLocation(int start, int end) {
    }

    public View getTabContainer() {
        return this.mTabContainer;
    }

    public ActionMode startActionModeForChild(View child, Callback callback, int type) {
        if (type != 0) {
            return super.startActionModeForChild(child, callback, type);
        }
        return null;
    }

    private static boolean isCollapsed(View view) {
        return view == null || view.getVisibility() == 8 || view.getMeasuredHeight() == 0;
    }

    private int getMeasuredHeightWithMargins(View view) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        return (view.getMeasuredHeight() + lp.topMargin) + lp.bottomMargin;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mActionBarView == null && MeasureSpec.getMode(heightMeasureSpec) == Integer.MIN_VALUE && this.mHeight >= 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(this.mHeight, MeasureSpec.getSize(heightMeasureSpec)), Integer.MIN_VALUE);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!(this.mActionBarView == null || this.mTabContainer == null || this.mTabContainer.getVisibility() == 8)) {
            int childCount = getChildCount();
            int nonTabMaxHeight = 0;
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child != this.mTabContainer) {
                    int i2;
                    if (isCollapsed(child)) {
                        i2 = 0;
                    } else {
                        i2 = getMeasuredHeightWithMargins(child);
                    }
                    nonTabMaxHeight = Math.max(nonTabMaxHeight, i2);
                }
            }
            setMeasuredDimension(getMeasuredWidth(), Math.min(getMeasuredHeightWithMargins(this.mTabContainer) + nonTabMaxHeight, MeasureSpec.getMode(heightMeasureSpec) == Integer.MIN_VALUE ? MeasureSpec.getSize(heightMeasureSpec) : Integer.MAX_VALUE));
        }
    }

    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        View tabContainer = this.mTabContainer;
        boolean hasTabs = (tabContainer == null || tabContainer.getVisibility() == 8) ? false : true;
        if (!(tabContainer == null || tabContainer.getVisibility() == 8)) {
            int containerHeight = getMeasuredHeight();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tabContainer.getLayoutParams();
            tabContainer.layout(l, (containerHeight - tabContainer.getMeasuredHeight()) - lp.bottomMargin, r, containerHeight - lp.bottomMargin);
        }
        boolean needsInvalidate = false;
        if (!this.mIsSplit) {
            if (this.mBackground != null) {
                if (this.mActionBarView.getVisibility() == 0) {
                    this.mBackground.setBounds(this.mActionBarView.getLeft(), this.mActionBarView.getTop(), this.mActionBarView.getRight(), this.mActionBarView.getBottom());
                } else if (this.mActionContextView == null || this.mActionContextView.getVisibility() != 0) {
                    this.mBackground.setBounds(0, 0, 0, 0);
                } else {
                    this.mBackground.setBounds(this.mActionContextView.getLeft(), this.mActionContextView.getTop(), this.mActionContextView.getRight(), this.mActionContextView.getBottom());
                }
                needsInvalidate = true;
            }
            this.mIsStacked = hasTabs;
            if (hasTabs && this.mStackedBackground != null) {
                this.mStackedBackground.setBounds(tabContainer.getLeft(), tabContainer.getTop(), tabContainer.getRight(), tabContainer.getBottom());
                needsInvalidate = true;
            }
        } else if (this.mSplitBackground != null) {
            this.mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            needsInvalidate = true;
        }
        if (needsInvalidate) {
            invalidate();
        }
    }

    protected void setHeight(int h) {
        this.mHeight = h;
    }

    protected View getActionBarView() {
        return this.mActionBarView;
    }

    protected Drawable getPrimaryBackground() {
        return this.mBackground;
    }

    protected Drawable getStackedBackground() {
        return this.mStackedBackground;
    }

    protected Drawable getSplitBackground() {
        return this.mSplitBackground;
    }

    public void setForcedPrimaryBackground(boolean forced) {
        this.mForcedPrimaryBackground = forced;
    }

    public void setForcedStackedBackground(boolean forced) {
        this.mForcedStackedBackground = forced;
    }

    public void setForcedSplitBackground(boolean forced) {
        this.mForcedSplitBackground = forced;
    }

    public void setAnimationEnable(boolean enableAnim) {
        this.mAnimationEnabled = enableAnim;
    }

    public boolean isAnimationEnabled() {
        return this.mAnimationEnabled;
    }
}
