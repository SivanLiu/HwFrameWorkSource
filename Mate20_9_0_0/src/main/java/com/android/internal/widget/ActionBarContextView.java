package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.view.menu.MenuBuilder;

public class ActionBarContextView extends AbsActionBarView {
    private static final String TAG = "ActionBarContextView";
    private View mClose;
    private int mCloseItemLayout;
    private View mCustomView;
    private Drawable mSplitBackground;
    private CharSequence mSubtitle;
    private int mSubtitleStyleRes;
    private TextView mSubtitleView;
    private CharSequence mTitle;
    private LinearLayout mTitleLayout;
    private boolean mTitleOptional;
    private int mTitleStyleRes;
    private TextView mTitleView;

    public ActionBarContextView(Context context) {
        this(context, null);
    }

    public ActionBarContextView(Context context, AttributeSet attrs) {
        this(context, attrs, 16843668);
    }

    public ActionBarContextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ActionBarContextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionMode, defStyleAttr, defStyleRes);
        setBackground(a.getDrawable(0));
        this.mTitleStyleRes = a.getResourceId(2, 0);
        this.mSubtitleStyleRes = a.getResourceId(3, 0);
        this.mContentHeight = a.getLayoutDimension(1, 0);
        this.mSplitBackground = a.getDrawable(4);
        this.mCloseItemLayout = a.getResourceId(5, 17367073);
        a.recycle();
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.hideOverflowMenu();
            this.mActionMenuPresenter.hideSubMenus();
        }
    }

    public void setSplitToolbar(boolean split) {
        if (this.mSplitActionBar != split) {
            if (this.mActionMenuPresenter != null) {
                LayoutParams layoutParams = new LayoutParams(-2, -1);
                ViewGroup oldParent;
                if (split) {
                    this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
                    this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
                    layoutParams.width = -1;
                    layoutParams.height = getMenuViewHeight(this.mContentHeight);
                    this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
                    this.mMenuView.setBackground(this.mSplitBackground);
                    oldParent = (ViewGroup) this.mMenuView.getParent();
                    if (oldParent != null) {
                        oldParent.removeView(this.mMenuView);
                    }
                    if (this.mSplitView != null) {
                        this.mSplitView.addView(this.mMenuView, layoutParams);
                    }
                } else {
                    this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
                    this.mMenuView.setBackground(null);
                    oldParent = (ViewGroup) this.mMenuView.getParent();
                    if (oldParent != null) {
                        oldParent.removeView(this.mMenuView);
                    }
                    addView(this.mMenuView, layoutParams);
                }
            }
            super.setSplitToolbar(split);
        }
    }

    public void setContentHeight(int height) {
        this.mContentHeight = height;
    }

    public void setCustomView(View view) {
        if (this.mCustomView != null) {
            removeView(this.mCustomView);
        }
        this.mCustomView = view;
        if (!(view == null || this.mTitleLayout == null)) {
            removeView(this.mTitleLayout);
            this.mTitleLayout = null;
        }
        if (view != null) {
            addView(view);
        }
        requestLayout();
    }

    public void setTitle(CharSequence title) {
        this.mTitle = title;
        initTitle();
    }

    public void setSubtitle(CharSequence subtitle) {
        this.mSubtitle = subtitle;
        initTitle();
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public CharSequence getSubtitle() {
        return this.mSubtitle;
    }

    protected void initTitle() {
        if (this.mTitleLayout == null) {
            LayoutInflater.from(getContext()).inflate(17367068, this);
            this.mTitleLayout = (LinearLayout) getChildAt(getChildCount() - 1);
            this.mTitleView = (TextView) this.mTitleLayout.findViewById(16908691);
            this.mSubtitleView = (TextView) this.mTitleLayout.findViewById(16908690);
            if (this.mTitleStyleRes != 0) {
                this.mTitleView.setTextAppearance(this.mTitleStyleRes);
            }
            if (this.mSubtitleStyleRes != 0) {
                this.mSubtitleView.setTextAppearance(this.mSubtitleStyleRes);
            }
        }
        this.mTitleView.setText(this.mTitle);
        this.mSubtitleView.setText(this.mSubtitle);
        boolean hasTitle = TextUtils.isEmpty(this.mTitle) ^ 1;
        boolean hasSubtitle = TextUtils.isEmpty(this.mSubtitle) ^ 1;
        int i = 8;
        this.mSubtitleView.setVisibility(hasSubtitle ? 0 : 8);
        LinearLayout linearLayout = this.mTitleLayout;
        if (hasTitle || hasSubtitle) {
            i = 0;
        }
        linearLayout.setVisibility(i);
        if (this.mTitleLayout.getParent() == null) {
            addView(this.mTitleLayout);
        }
    }

    public void initForMode(final ActionMode mode) {
        if (this.mClose == null) {
            this.mClose = LayoutInflater.from(this.mContext).inflate(this.mCloseItemLayout, this, false);
            addView(this.mClose);
        } else if (this.mClose.getParent() == null) {
            addView(this.mClose);
        }
        this.mClose.findViewById(16908698).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mode.finish();
            }
        });
        MenuBuilder menu = (MenuBuilder) mode.getMenu();
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.dismissPopupMenus();
        }
        this.mActionMenuPresenter = new ActionMenuPresenter(this.mContext);
        this.mActionMenuPresenter.setReserveOverflow(true);
        LayoutParams layoutParams = new LayoutParams(-2, -1);
        if (this.mSplitActionBar) {
            this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
            this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
            layoutParams.width = -1;
            layoutParams.height = this.mContentHeight;
            menu.addMenuPresenter(this.mActionMenuPresenter, this.mPopupContext);
            this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
            this.mMenuView.setBackgroundDrawable(this.mSplitBackground);
            if (this.mSplitView != null) {
                this.mSplitView.addView(this.mMenuView, layoutParams);
                return;
            }
            return;
        }
        menu.addMenuPresenter(this.mActionMenuPresenter, this.mPopupContext);
        this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
        this.mMenuView.setBackground(null);
        addView(this.mMenuView, layoutParams);
    }

    public void closeMode() {
        if (this.mClose == null) {
            killMode();
        }
    }

    public void killMode() {
        removeAllViews();
        if (this.mSplitView != null) {
            this.mSplitView.removeView(this.mMenuView);
        }
        this.mCustomView = null;
        this.mMenuView = null;
    }

    public boolean showOverflowMenu() {
        if (this.mActionMenuPresenter != null) {
            return this.mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    public boolean hideOverflowMenu() {
        if (this.mActionMenuPresenter != null) {
            return this.mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    public boolean isOverflowMenuShowing() {
        if (this.mActionMenuPresenter != null) {
            return this.mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(-1, -2);
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        StringBuilder stringBuilder;
        if (widthMode != 1073741824) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(getClass().getSimpleName());
            stringBuilder.append(" can only be used with android:layout_width=\"match_parent\" (or fill_parent)");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (MeasureSpec.getMode(heightMeasureSpec) != 0) {
            int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
            int maxHeight = this.mContentHeight > 0 ? this.mContentHeight : MeasureSpec.getSize(heightMeasureSpec);
            int verticalPadding = getPaddingTop() + getPaddingBottom();
            int availableWidth = (contentWidth - getPaddingLeft()) - getPaddingRight();
            int height = maxHeight - verticalPadding;
            int childSpecHeight = MeasureSpec.makeMeasureSpec(height, Integer.MIN_VALUE);
            if (this.mClose != null) {
                MarginLayoutParams lp = (MarginLayoutParams) this.mClose.getLayoutParams();
                availableWidth = measureChildView(this.mClose, availableWidth, childSpecHeight, 0) - (lp.leftMargin + lp.rightMargin);
            }
            if (this.mMenuView != null && this.mMenuView.getParent() == this) {
                availableWidth = measureChildView(this.mMenuView, availableWidth, childSpecHeight, 0);
            }
            if (this.mTitleLayout != null && this.mCustomView == null) {
                if (this.mTitleOptional) {
                    this.mTitleLayout.measure(MeasureSpec.makeSafeMeasureSpec(contentWidth, 0), childSpecHeight);
                    int titleWidth = this.mTitleLayout.getMeasuredWidth();
                    boolean titleFits = titleWidth <= availableWidth;
                    if (titleFits) {
                        availableWidth -= titleWidth;
                    }
                    this.mTitleLayout.setVisibility(titleFits ? 0 : 8);
                } else {
                    availableWidth = measureChildView(this.mTitleLayout, availableWidth, childSpecHeight, 0);
                }
            }
            if (this.mCustomView != null) {
                LayoutParams lp2 = this.mCustomView.getLayoutParams();
                int customWidthMode = lp2.width != -2 ? 1073741824 : Integer.MIN_VALUE;
                this.mCustomView.measure(MeasureSpec.makeMeasureSpec(lp2.width >= 0 ? Math.min(lp2.width, availableWidth) : availableWidth, customWidthMode), MeasureSpec.makeMeasureSpec(lp2.height >= 0 ? Math.min(lp2.height, height) : height, lp2.height != -2 ? 1073741824 : Integer.MIN_VALUE));
            }
            if (this.mContentHeight <= 0) {
                widthMode = 0;
                int count = getChildCount();
                int i = 0;
                while (true) {
                    int i2 = i;
                    if (i2 < count) {
                        int paddedViewHeight = getChildAt(i2).getMeasuredHeight() + verticalPadding;
                        if (paddedViewHeight > widthMode) {
                            widthMode = paddedViewHeight;
                        }
                        i = i2 + 1;
                    } else {
                        setMeasuredDimension(contentWidth, widthMode);
                        return;
                    }
                }
            }
            setMeasuredDimension(contentWidth, maxHeight);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(getClass().getSimpleName());
            stringBuilder.append(" can only be used with android:layout_height=\"wrap_content\"");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean isLayoutRtl = isRtlLocale();
        int x = isLayoutRtl ? (r - l) - getPaddingRight() : getPaddingLeft();
        int y = getPaddingTop();
        int contentHeight = ((b - t) - getPaddingTop()) - getPaddingBottom();
        if (!(this.mClose == null || this.mClose.getVisibility() == 8)) {
            MarginLayoutParams lp = (MarginLayoutParams) this.mClose.getLayoutParams();
            int startMargin = isLayoutRtl ? lp.rightMargin : lp.leftMargin;
            int endMargin = isLayoutRtl ? lp.leftMargin : lp.rightMargin;
            int x2 = AbsActionBarView.next(x, startMargin, isLayoutRtl);
            x = AbsActionBarView.next(positionChild(this.mClose, x2, y, contentHeight, isLayoutRtl) + x2, endMargin, isLayoutRtl);
        }
        int x3 = x;
        if (!(this.mTitleLayout == null || this.mCustomView != null || this.mTitleLayout.getVisibility() == 8)) {
            x3 += positionChild(this.mTitleLayout, x3, y, contentHeight, isLayoutRtl);
        }
        if (this.mCustomView != null) {
            x3 += positionChild(this.mCustomView, x3, y, contentHeight, isLayoutRtl);
        }
        x3 = isLayoutRtl ? getPaddingLeft() : (r - l) - getPaddingRight();
        if (this.mMenuView != null && this.mMenuView.getParent() == this) {
            x3 += positionChild(this.mMenuView, x3, y, contentHeight, isLayoutRtl ^ 1);
        }
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        if (event.getEventType() == 32) {
            event.setSource(this);
            event.setClassName(getClass().getName());
            event.setPackageName(getContext().getPackageName());
            event.setContentDescription(this.mTitle);
            return;
        }
        super.onInitializeAccessibilityEventInternal(event);
    }

    public void setTitleOptional(boolean titleOptional) {
        if (titleOptional != this.mTitleOptional) {
            requestLayout();
        }
        this.mTitleOptional = titleOptional;
    }

    public boolean isTitleOptional() {
        return this.mTitleOptional;
    }

    public TextView getTitleView() {
        return this.mTitleView;
    }

    public void setTitleView(TextView view) {
        this.mTitleView = view;
    }

    public TextView getSubtitleView() {
        return this.mSubtitleView;
    }

    public void setSubtitleView(TextView view) {
        this.mSubtitleView = view;
    }

    public LinearLayout getTitleLayout() {
        return this.mTitleLayout;
    }

    public void setTitleLayout(LinearLayout ll) {
        this.mTitleLayout = ll;
    }

    protected int getMenuViewHeight(int contentHeight) {
        return contentHeight;
    }

    protected void initTitleAppearance() {
        if (this.mTitleStyleRes != 0) {
            this.mTitleView.setTextAppearance(this.mContext, this.mTitleStyleRes);
        }
        if (this.mSubtitleStyleRes != 0) {
            this.mSubtitleView.setTextAppearance(this.mContext, this.mSubtitleStyleRes);
        }
    }
}
