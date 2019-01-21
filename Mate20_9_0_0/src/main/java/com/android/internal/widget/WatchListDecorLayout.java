package com.android.internal.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;
import java.util.ArrayList;

public class WatchListDecorLayout extends FrameLayout implements OnScrollChangedListener {
    private View mBottomPanel;
    private int mForegroundPaddingBottom = 0;
    private int mForegroundPaddingLeft = 0;
    private int mForegroundPaddingRight = 0;
    private int mForegroundPaddingTop = 0;
    private ListView mListView;
    private final ArrayList<View> mMatchParentChildren = new ArrayList(1);
    private ViewTreeObserver mObserver;
    private int mPendingScroll;
    private View mTopPanel;

    public WatchListDecorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WatchListDecorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WatchListDecorLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        int i = 0;
        this.mPendingScroll = 0;
        while (i < getChildCount()) {
            View child = getChildAt(i);
            if (!(child instanceof ListView)) {
                int gravity = ((LayoutParams) child.getLayoutParams()).gravity & 112;
                if (gravity == 48 && this.mTopPanel == null) {
                    this.mTopPanel = child;
                } else if (gravity == 80 && this.mBottomPanel == null) {
                    this.mBottomPanel = child;
                }
            } else if (this.mListView == null) {
                this.mListView = (ListView) child;
                this.mListView.setNestedScrollingEnabled(true);
                this.mObserver = this.mListView.getViewTreeObserver();
                this.mObserver.addOnScrollChangedListener(this);
            } else {
                throw new IllegalArgumentException("only one ListView child allowed");
            }
            i++;
        }
    }

    public void onDetachedFromWindow() {
        this.mListView = null;
        this.mBottomPanel = null;
        this.mTopPanel = null;
        if (this.mObserver != null) {
            if (this.mObserver.isAlive()) {
                this.mObserver.removeOnScrollChangedListener(this);
            }
            this.mObserver = null;
        }
    }

    private void applyMeasureToChild(View child, int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        if (lp.width == -1) {
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, (((getMeasuredWidth() - getPaddingLeftWithForeground()) - getPaddingRightWithForeground()) - lp.leftMargin) - lp.rightMargin), 1073741824);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, ((getPaddingLeftWithForeground() + getPaddingRightWithForeground()) + lp.leftMargin) + lp.rightMargin, lp.width);
        }
        if (lp.height == -1) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, (((getMeasuredHeight() - getPaddingTopWithForeground()) - getPaddingBottomWithForeground()) - lp.topMargin) - lp.bottomMargin), 1073741824);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, ((getPaddingTopWithForeground() + getPaddingBottomWithForeground()) + lp.topMargin) + lp.bottomMargin, lp.height);
        }
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private int measureAndGetHeight(View child, int widthMeasureSpec, int heightMeasureSpec) {
        if (child != null) {
            if (child.getVisibility() != 8) {
                applyMeasureToChild(this.mBottomPanel, widthMeasureSpec, heightMeasureSpec);
                return child.getMeasuredHeight();
            } else if (getMeasureAllChildren()) {
                applyMeasureToChild(this.mBottomPanel, widthMeasureSpec, heightMeasureSpec);
            }
        }
        return 0;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth;
        int childState;
        int paddingBottom;
        int i = widthMeasureSpec;
        int i2 = heightMeasureSpec;
        int count = getChildCount();
        int i3 = 0;
        boolean z = (MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && MeasureSpec.getMode(heightMeasureSpec) == 1073741824) ? false : true;
        boolean measureMatchParentChildren = z;
        this.mMatchParentChildren.clear();
        int maxHeight = 0;
        int maxWidth2 = 0;
        int childState2 = 0;
        int i4 = 0;
        while (true) {
            int i5 = i4;
            if (i5 >= count) {
                break;
            }
            int i6;
            View child = getChildAt(i5);
            if (getMeasureAllChildren() || child.getVisibility() != 8) {
                View child2 = child;
                i6 = i5;
                measureChildWithMargins(child, i, 0, i2, 0);
                LayoutParams lp = (LayoutParams) child2.getLayoutParams();
                maxWidth = Math.max(maxWidth2, (child2.getMeasuredWidth() + lp.leftMargin) + lp.rightMargin);
                int maxHeight2 = Math.max(maxHeight, (child2.getMeasuredHeight() + lp.topMargin) + lp.bottomMargin);
                childState = combineMeasuredStates(childState2, child2.getMeasuredState());
                if (measureMatchParentChildren && (lp.width == -1 || lp.height == -1)) {
                    this.mMatchParentChildren.add(child2);
                }
                maxWidth2 = maxWidth;
                maxHeight = maxHeight2;
                childState2 = childState;
            } else {
                i6 = i5;
            }
            i4 = i6 + 1;
        }
        maxWidth2 += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
        i4 = Math.max(maxHeight + (getPaddingTopWithForeground() + getPaddingBottomWithForeground()), getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth2, getSuggestedMinimumWidth());
        Drawable drawable = getForeground();
        if (drawable != null) {
            i4 = Math.max(i4, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth, i, childState2), resolveSizeAndState(i4, i2, childState2 << 16));
        if (this.mListView != null) {
            if (this.mPendingScroll != 0) {
                this.mListView.scrollListBy(this.mPendingScroll);
                this.mPendingScroll = 0;
            }
            childState = Math.max(this.mListView.getPaddingTop(), measureAndGetHeight(this.mTopPanel, i, i2));
            paddingBottom = Math.max(this.mListView.getPaddingBottom(), measureAndGetHeight(this.mBottomPanel, i, i2));
            if (!(childState == this.mListView.getPaddingTop() && paddingBottom == this.mListView.getPaddingBottom())) {
                this.mPendingScroll += this.mListView.getPaddingTop() - childState;
                this.mListView.setPadding(this.mListView.getPaddingLeft(), childState, this.mListView.getPaddingRight(), paddingBottom);
            }
        }
        childState = this.mMatchParentChildren.size();
        if (childState > 1) {
            while (true) {
                paddingBottom = i3;
                if (paddingBottom < childState) {
                    View child3 = (View) this.mMatchParentChildren.get(paddingBottom);
                    if (this.mListView == null || !(child3 == this.mTopPanel || child3 == this.mBottomPanel)) {
                        applyMeasureToChild(child3, i, i2);
                    }
                    i3 = paddingBottom + 1;
                } else {
                    return;
                }
            }
        }
    }

    public void setForegroundGravity(int foregroundGravity) {
        if (getForegroundGravity() != foregroundGravity) {
            super.setForegroundGravity(foregroundGravity);
            Drawable foreground = getForeground();
            if (getForegroundGravity() != 119 || foreground == null) {
                this.mForegroundPaddingLeft = 0;
                this.mForegroundPaddingTop = 0;
                this.mForegroundPaddingRight = 0;
                this.mForegroundPaddingBottom = 0;
                return;
            }
            Rect padding = new Rect();
            if (foreground.getPadding(padding)) {
                this.mForegroundPaddingLeft = padding.left;
                this.mForegroundPaddingTop = padding.top;
                this.mForegroundPaddingRight = padding.right;
                this.mForegroundPaddingBottom = padding.bottom;
            }
        }
    }

    private int getPaddingLeftWithForeground() {
        if (isForegroundInsidePadding()) {
            return Math.max(this.mPaddingLeft, this.mForegroundPaddingLeft);
        }
        return this.mPaddingLeft + this.mForegroundPaddingLeft;
    }

    private int getPaddingRightWithForeground() {
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

    public void onScrollChanged() {
        if (this.mListView != null) {
            if (this.mTopPanel != null) {
                if (this.mListView.getChildCount() <= 0) {
                    setScrolling(this.mTopPanel, 0.0f);
                } else if (this.mListView.getFirstVisiblePosition() == 0) {
                    setScrolling(this.mTopPanel, (this.mListView.getChildAt(0).getY() - ((float) this.mTopPanel.getHeight())) - ((float) this.mTopPanel.getTop()));
                } else {
                    setScrolling(this.mTopPanel, (float) (-this.mTopPanel.getHeight()));
                }
            }
            if (this.mBottomPanel != null) {
                if (this.mListView.getChildCount() <= 0) {
                    setScrolling(this.mBottomPanel, 0.0f);
                } else if (this.mListView.getLastVisiblePosition() >= this.mListView.getCount() - 1) {
                    View lastChild = this.mListView.getChildAt(this.mListView.getChildCount() - 1);
                    setScrolling(this.mBottomPanel, Math.max(0.0f, (lastChild.getY() + ((float) lastChild.getHeight())) - ((float) this.mBottomPanel.getTop())));
                } else {
                    setScrolling(this.mBottomPanel, (float) this.mBottomPanel.getHeight());
                }
            }
        }
    }

    private void setScrolling(View panel, float translationY) {
        if (panel.getTranslationY() != translationY) {
            panel.setTranslationY(translationY);
        }
    }
}
