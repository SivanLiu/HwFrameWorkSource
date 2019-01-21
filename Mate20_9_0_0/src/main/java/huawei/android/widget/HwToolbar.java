package huawei.android.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuView.OnMenuItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toolbar;
import android.widget.Toolbar.LayoutParams;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.view.menu.MenuPresenter.Callback;
import com.android.internal.widget.DecorToolbar;
import com.android.internal.widget.ToolbarWidgetWrapper;
import huawei.android.widget.DecouplingUtil.ReflectUtil;
import huawei.android.widget.effect.engine.HwBlurEngine;
import huawei.android.widget.effect.engine.HwBlurEngine.BlurType;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;
import huawei.com.android.internal.widget.HwToolbarWidgetWrapper;

public class HwToolbar extends Toolbar {
    private static final boolean DEBUG = true;
    private static final String TAG = "HwToolbar";
    private boolean isBlurEnable;
    private HwActionMenuPresenter mActionMenuPresenter;
    private Callback mActionMenuPresenterCallback;
    private HwBlurEngine mBlurEngine;
    private ClickEffectEntry mClickEffectEntry;
    private Context mContext;
    private View mCustomTitleView;
    private boolean mDynamicSplitMenu;
    private MenuPresenter mExpandMenuPresenter;
    private boolean mForceSplit;
    private HwCutoutUtil mHwCutoutUtil;
    private Drawable mIcon1Drawable;
    private ImageView mIcon1View;
    private boolean mIcon1Visible;
    private Drawable mIcon2Drawable;
    private ImageView mIcon2View;
    private boolean mIcon2Visible;
    private ColorStateList mIconColor;
    private View mIconLayout;
    private boolean mIsSetDynamicSplitMenu;
    private int mLeftPaddingBackup;
    private OnClickListener mListener1;
    private OnClickListener mListener2;
    private ImageView mLogoView;
    private MenuBuilder mMenu;
    private MenuBuilder.Callback mMenuBuilderCallback;
    private int mMenuItemLimit;
    private HwToolbarMenuView mMenuView;
    private final OnMenuItemClickListener mMenuViewItemClickListener;
    private Toolbar.OnMenuItemClickListener mOnMenuItemClickListener;
    private TextView mParentClassSubTitleTextView;
    private TextView mParentClassTitleTextView;
    private int mPopupEndLocation;
    private int mPopupStartLocation;
    private int mResCancel;
    private ResLoader mResLoader;
    private int mResOk;
    private int mRightPaddingBackup;
    private Spinner mSpinner;
    private SpinnerAdapter mSpinnerAdapter;
    private boolean mSplitActionBar;
    private HwToolBarMenuContainer mSplitView;
    private int mSplitViewMaxSize;
    private int mSubTitleMarginBottom;
    private int mSubTitleMarginTop;
    private int mSubTitleMinSize;
    private int mSubTitleNormalSize;
    private HwTextView mSubTitleView;
    private CharSequence mSubtitleText;
    private LinearLayout mTitleContainer;
    private int mTitleMarginTop;
    private int mTitleMinSize;
    private int mTitleNormalSize;
    private int mTitleSizeStep;
    private CharSequence mTitleText;
    private HwTextView mTitleView;
    private ToolbarWidgetWrapper mWrapper;

    private static class ActionMenuPresenterCallback implements Callback {
        private ActionMenuPresenterCallback() {
        }

        /* synthetic */ ActionMenuPresenterCallback(AnonymousClass1 x0) {
            this();
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            return false;
        }
    }

    private class MenuBuilderCallback implements MenuBuilder.Callback {
        private MenuBuilderCallback() {
        }

        /* synthetic */ MenuBuilderCallback(HwToolbar x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            return (HwToolbar.this.mMenuViewItemClickListener == null || !HwToolbar.this.mMenuViewItemClickListener.onMenuItemClick(item)) ? false : HwToolbar.DEBUG;
        }

        public void onMenuModeChange(MenuBuilder menu) {
            if (HwToolbar.this.mMenuBuilderCallback != null) {
                HwToolbar.this.mMenuBuilderCallback.onMenuModeChange(menu);
            }
        }
    }

    public HwToolbar(Context context) {
        this(context, null);
    }

    public HwToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 16843946);
    }

    public HwToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HwToolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mIsSetDynamicSplitMenu = false;
        this.mLeftPaddingBackup = 0;
        this.mRightPaddingBackup = 0;
        this.mHwCutoutUtil = null;
        this.mBlurEngine = HwBlurEngine.getInstance();
        this.mMenuViewItemClickListener = new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (HwToolbar.this.mOnMenuItemClickListener != null) {
                    return HwToolbar.this.mOnMenuItemClickListener.onMenuItemClick(item);
                }
                return false;
            }
        };
        this.isBlurEnable = false;
        this.mClickEffectEntry = null;
        this.mContext = context;
        init(attrs, defStyleAttr, defStyleRes);
    }

    public void setStartIcon(boolean icon1Visible, Drawable icon1, OnClickListener listener1) {
        if (this.mIcon1View != null && this.mIcon2View != null) {
            setTitle(this.mTitleText);
            setSubtitle(this.mSubtitleText);
            setStartIconVisible(icon1Visible);
            setStartIconImage(icon1);
            setStartIconListener(listener1);
        }
    }

    public void setEndIcon(boolean icon2Visible, Drawable icon2, OnClickListener listener2) {
        if (this.mIcon1View != null && this.mIcon2View != null) {
            setTitle(this.mTitleText);
            setSubtitle(this.mSubtitleText);
            setEndIconVisible(icon2Visible);
            setEndIconImage(icon2);
            setEndIconListener(listener2);
        }
    }

    public void setStartContentDescription(CharSequence contentDescription) {
        if (this.mIcon1View == null) {
            initIconLayout();
        }
        if (this.mIcon1View != null) {
            this.mIcon1View.setContentDescription(contentDescription);
        }
    }

    public void setEndContentDescription(CharSequence contentDescription) {
        if (this.mIcon2View == null) {
            initIconLayout();
        }
        if (this.mIcon2View != null) {
            this.mIcon2View.setContentDescription(contentDescription);
        }
    }

    public void setDynamicSplitMenu(boolean splitMenu) {
        this.mDynamicSplitMenu = splitMenu;
        this.mIsSetDynamicSplitMenu = DEBUG;
        requestLayout();
    }

    public View getIconLayout() {
        return this.mIconLayout;
    }

    public void setSplitBackgroundDrawable(Drawable d) {
        if (this.mSplitView != null) {
            this.mSplitView.setSplitBackground(d);
        }
    }

    public void setSmartColor(ColorStateList iconColor, ColorStateList titleColor) {
        if (this.mMenuView != null) {
            this.mMenuView.onSetSmartColor(iconColor, titleColor);
        }
    }

    public void setTitle(CharSequence title) {
        this.mTitleText = title;
        if (!(this.mTitleContainer == null || this.mCustomTitleView == null || this.mCustomTitleView.getParent() != this.mTitleContainer)) {
            this.mTitleContainer.removeView(this.mCustomTitleView);
            View titleView = this.mTitleView;
            if (titleView != null) {
                ViewParent titleViewVp = titleView.getParent();
                if (titleViewVp != null && (titleViewVp instanceof ViewGroup)) {
                    ((ViewGroup) titleViewVp).removeView(titleView);
                }
            }
            View subTitleView = this.mSubTitleView;
            if (subTitleView != null) {
                ViewParent subTitleViewVp = subTitleView.getParent();
                if (subTitleViewVp != null && (subTitleViewVp instanceof ViewGroup)) {
                    ((ViewGroup) subTitleViewVp).removeView(subTitleView);
                }
            }
            this.mTitleContainer.addView(titleView);
            this.mTitleContainer.addView(subTitleView);
        }
        if (shouldLayout(this.mIconLayout)) {
            if (this.mTitleView != null) {
                this.mTitleView.setText(title);
            }
            super.setTitle(null);
        } else {
            super.setTitle(title);
        }
        if (this.mParentClassTitleTextView == null) {
            this.mParentClassTitleTextView = (TextView) ReflectUtil.getObject(this, "mTitleTextView", Toolbar.class);
            if (this.mParentClassTitleTextView != null) {
                this.mParentClassTitleTextView.setSingleLine(false);
                this.mParentClassTitleTextView.setMaxLines(2);
                MarginLayoutParams tvLp = (MarginLayoutParams) this.mParentClassTitleTextView.getLayoutParams();
                tvLp.topMargin = this.mTitleMarginTop;
                this.mParentClassTitleTextView.setLayoutParams(tvLp);
            }
        }
    }

    public void setSubtitle(CharSequence subtitle) {
        this.mSubtitleText = subtitle;
        if (this.mIconLayout == null || this.mIconLayout.getParent() != this) {
            super.setSubtitle(subtitle);
        } else {
            if (this.mSubTitleView != null) {
                this.mSubTitleView.setText(subtitle);
            }
            super.setSubtitle(null);
        }
        if (this.mSubTitleView != null) {
            this.mSubTitleView.setVisibility(subtitle != null ? 0 : 8);
        }
        if (this.mParentClassSubTitleTextView == null) {
            this.mParentClassSubTitleTextView = (TextView) ReflectUtil.getObject(this, "mSubtitleTextView", Toolbar.class);
            if (this.mParentClassSubTitleTextView != null) {
                this.mParentClassSubTitleTextView.setSingleLine(DEBUG);
                MarginLayoutParams stvLp = (MarginLayoutParams) this.mParentClassSubTitleTextView.getLayoutParams();
                stvLp.topMargin = this.mSubTitleMarginTop;
                stvLp.bottomMargin = this.mSubTitleMarginBottom;
                this.mParentClassSubTitleTextView.setLayoutParams(stvLp);
            }
        }
    }

    public void setCustomTitle(View view) {
        if (this.mTitleContainer == null) {
            initIconLayout();
        }
        if (view != null && this.mIconLayout != null) {
            this.mTitleContainer.removeView(this.mTitleView);
            this.mTitleContainer.removeView(this.mSubTitleView);
            if (this.mCustomTitleView != null && this.mCustomTitleView.getParent() == this.mTitleContainer) {
                this.mTitleContainer.removeView(this.mCustomTitleView);
            }
            ViewParent vp = view.getParent();
            if (vp != null && (vp instanceof ViewGroup)) {
                ((ViewGroup) vp).removeView(view);
            }
            this.mTitleContainer.addView(view);
            this.mCustomTitleView = view;
        }
    }

    public void setDisplaySpinner(int contentId, OnItemSelectedListener listener) {
        if (this.mIconLayout == null) {
            initIconLayout();
        }
        if (this.mIconLayout != null && this.mTitleView != null && this.mSpinner != null && this.mSubTitleView != null) {
            this.mTitleView.setVisibility(8);
            this.mSubTitleView.setVisibility(8);
            this.mSpinner.setVisibility(0);
            ensureSpinnerAdapter(contentId);
            this.mSpinner.setAdapter(this.mSpinnerAdapter);
            this.mSpinner.setOnItemSelectedListener(listener);
        }
    }

    public SpinnerAdapter getSpinnerAdapter() {
        return this.mSpinnerAdapter;
    }

    public int getDropdownSelectedPosition() {
        return this.mSpinner != null ? this.mSpinner.getSelectedItemPosition() : 0;
    }

    public int getDropdownItemCount() {
        return this.mSpinner != null ? this.mSpinner.getCount() : 0;
    }

    public void setSplitViewLocation(int start, int end) {
        if (this.mSplitView != null) {
            this.mSplitView.setSplitViewLocation(start, end);
        }
        this.mPopupStartLocation = start;
        this.mPopupEndLocation = end;
        updateSplitLocation();
    }

    public void setSplitToolbarForce(boolean forceSplit) {
        this.mForceSplit = forceSplit;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updateViews();
        splitToolbar();
        View collapseButtonView = (View) ReflectUtil.getObject(this, "mCollapseButtonView", Toolbar.class);
        if (!(collapseButtonView == null || collapseButtonView.getVisibility() == 8)) {
            collapseButtonView.setVisibility(8);
        }
        if (shouldLayout(this.mParentClassTitleTextView)) {
            this.mParentClassTitleTextView.setTextSize(2, (float) this.mTitleNormalSize);
            this.mParentClassTitleTextView.setSingleLine(false);
            this.mParentClassTitleTextView.setMaxLines(2);
            if (shouldLayout(this.mParentClassSubTitleTextView)) {
                this.mParentClassTitleTextView.setTextSize(1, (float) this.mTitleNormalSize);
                this.mParentClassTitleTextView.setSingleLine(DEBUG);
            }
        }
        if (shouldLayout(this.mParentClassSubTitleTextView)) {
            this.mParentClassSubTitleTextView.setTextSize(1, (float) this.mSubTitleNormalSize);
        }
        if (!(!shouldLayout(this.mIconLayout) || this.mTitleView == null || this.mTitleView.getVisibility() == 8)) {
            if (this.mSubTitleView == null || this.mSubTitleView.getVisibility() == 8) {
                this.mTitleView.setAutoTextSize(2, (float) this.mTitleNormalSize);
                this.mTitleView.setAutoTextInfo(this.mTitleMinSize, this.mTitleSizeStep, 2);
                this.mTitleView.setSingleLine(false);
                this.mTitleView.setMaxLines(2);
            } else {
                this.mTitleView.setSingleLine(DEBUG);
                this.mTitleView.setAutoTextSize(1, (float) this.mTitleNormalSize);
                this.mTitleView.setAutoTextInfo(this.mTitleMinSize, this.mTitleSizeStep, 1);
            }
            this.mTitleView.requestLayout();
            this.mSubTitleView.requestLayout();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (shouldLayout(this.mParentClassSubTitleTextView)) {
            AutoTextUtils.autoText(this.mSubTitleNormalSize, this.mSubTitleMinSize, this.mTitleSizeStep, shouldLayout(this.mParentClassSubTitleTextView) ? 1 : 2, this.mParentClassSubTitleTextView.getMeasuredWidth(), MeasureSpec.getSize(heightMeasureSpec), this.mParentClassSubTitleTextView);
        }
        if (shouldLayout(this.mParentClassTitleTextView)) {
            AutoTextUtils.autoText(this.mTitleNormalSize, this.mTitleMinSize, this.mTitleSizeStep, 2, this.mParentClassTitleTextView.getMeasuredWidth(), MeasureSpec.getSize(heightMeasureSpec), this.mParentClassTitleTextView);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private boolean shouldLayout(View view) {
        return (view == null || view.getParent() != this || view.getVisibility() == 8) ? false : DEBUG;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (shouldLayout(this.mParentClassTitleTextView)) {
            int top = this.mParentClassTitleTextView.getTop();
            boolean hasSubTitle = shouldLayout(this.mParentClassSubTitleTextView);
            int titleHeight = this.mParentClassTitleTextView.getMeasuredHeight();
            int height = getMeasuredHeight();
            if (hasSubTitle) {
                titleHeight += this.mSubTitleMarginTop + this.mParentClassSubTitleTextView.getMeasuredHeight();
            }
            int realTop = (height - titleHeight) / 2;
            if (realTop > top) {
                int offset = realTop - top;
                int bottom = this.mParentClassTitleTextView.getMeasuredHeight() + realTop;
                this.mParentClassTitleTextView.layout(this.mParentClassTitleTextView.getLeft(), realTop, this.mParentClassTitleTextView.getRight(), bottom);
                if (hasSubTitle) {
                    int subtitleTop = this.mSubTitleMarginTop + bottom;
                    this.mParentClassSubTitleTextView.layout(this.mParentClassSubTitleTextView.getLeft(), subtitleTop, this.mParentClassSubTitleTextView.getRight(), this.mParentClassSubTitleTextView.getMeasuredHeight() + subtitleTop);
                }
            }
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.onConfigurationChanged(newConfig);
        }
    }

    public Menu getMenu() {
        ensureHwMenu();
        updateSplitLocation();
        return this.mMenu;
    }

    public void setMenu(MenuBuilder menu, ActionMenuPresenter outerPresenter) {
        Callback cb = outerPresenter.getCallback();
        ensureHwMenuView();
        initHwActionMenuPresenter();
        this.mActionMenuPresenter.setCallback(cb);
        this.mActionMenuPresenter.setId(16908695);
        initExpandMenuPresenter();
        setMenuPresenterStatus(getSplitStatus());
        updateSplitLocation();
        super.setMenu(menu, this.mActionMenuPresenter);
    }

    public void setMenuCallbacks(Callback pcb, MenuBuilder.Callback mcb) {
        super.setMenuCallbacks(pcb, mcb);
        this.mActionMenuPresenterCallback = pcb;
        this.mMenuBuilderCallback = mcb;
    }

    public void setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener listener) {
        super.setOnMenuItemClickListener(listener);
        this.mOnMenuItemClickListener = listener;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mHwCutoutUtil.checkCutoutStatus(insets, this, this.mContext);
        this.mHwCutoutUtil.doCutoutPadding(this, this.mLeftPaddingBackup, this.mRightPaddingBackup);
        return super.onApplyWindowInsets(insets);
    }

    public DecorToolbar getWrapper() {
        if (this.mWrapper == null) {
            this.mWrapper = new HwToolbarWidgetWrapper(this, false);
        }
        return this.mWrapper;
    }

    public CharSequence getTitle() {
        return this.mTitleText;
    }

    public CharSequence getSubtitle() {
        return this.mSubtitleText;
    }

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        int i = defStyleAttr;
        this.mResLoader = ResLoader.getInstance();
        Resources res = this.mResLoader.getResources(this.mContext);
        int itemlimitId = this.mResLoader.getIdentifier(this.mContext, "integer", "hwtoolbar_split_menu_itemlimit");
        this.mMenuItemLimit = res.getInteger(itemlimitId);
        int titleNormalSizeId = this.mResLoader.getIdentifier(this.mContext, "integer", "hwtoolbar_title_normal_textsize");
        this.mTitleNormalSize = res.getInteger(titleNormalSizeId);
        int titleMinSizeId = this.mResLoader.getIdentifier(this.mContext, "integer", "hwtoolbar_title_min_textsize");
        this.mTitleMinSize = res.getInteger(titleMinSizeId);
        this.mSubTitleNormalSize = res.getInteger(this.mResLoader.getIdentifier(this.mContext, "integer", "hwtoolbar_subtitle_normal_textsize"));
        this.mSubTitleMinSize = res.getInteger(this.mResLoader.getIdentifier(this.mContext, "integer", "hwtoolbar_subtitle_min_textsize"));
        this.mTitleSizeStep = res.getInteger(this.mResLoader.getIdentifier(this.mContext, "integer", "hwtoolbar_title_textsize_step"));
        this.mSubTitleMarginTop = res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_subtitle_margin_top"));
        this.mSubTitleMarginBottom = res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_title_margin_bottom"));
        this.mTitleMarginTop = res.getDimensionPixelOffset(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_title_margin_top"));
        Theme theme = this.mResLoader.getTheme(this.mContext);
        if (theme != null) {
            int[] hwToolbarSteable = this.mResLoader.getIdentifierArray(this.mContext, ResLoaderUtil.STAYLEABLE, TAG);
            if (hwToolbarSteable != null) {
                TypedArray a = theme.obtainStyledAttributes(attrs, hwToolbarSteable, i, defStyleRes);
                this.mIconColor = a.getColorStateList(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.STAYLEABLE, "HwToolbar_hwToolbarIconColor"));
                a.recycle();
                this.mLeftPaddingBackup = getPaddingLeft();
                this.mRightPaddingBackup = getPaddingRight();
                this.mHwCutoutUtil = new HwCutoutUtil();
                this.mClickEffectEntry = HwWidgetUtils.getCleckEffectEntry(this.mContext, i);
                initIconLayout();
            }
        }
    }

    private void updateSplitLocation() {
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.setPopupLocation(this.mPopupStartLocation, this.mPopupEndLocation);
        }
    }

    private void initExpandMenuPresenter() {
        if (this.mExpandMenuPresenter == null) {
            Object expandPresenter = ReflectUtil.createPrivateInnerInstance(ReflectUtil.getPrivateClass("android.widget.Toolbar$ExpandedActionViewMenuPresenter"), Toolbar.class, this, null, null);
            if ((expandPresenter instanceof MenuPresenter) && this.mMenu != null) {
                this.mExpandMenuPresenter = (MenuPresenter) expandPresenter;
                ReflectUtil.setObject("mExpandedMenuPresenter", this, this.mExpandMenuPresenter, Toolbar.class);
            }
        }
    }

    private void setSplitToolbar(boolean splitActionBar) {
        if (this.mSplitView == null) {
            ensureSplitView();
        }
        if (this.mMenuView == null) {
            ensureHwMenuView();
        }
        if (this.mSplitActionBar != splitActionBar) {
            if (this.mMenuView != null) {
                ViewGroup oldParent = (ViewGroup) this.mMenuView.getParent();
                if (oldParent != null) {
                    oldParent.removeView(this.mMenuView);
                }
                if (splitActionBar) {
                    if (this.mSplitView != null) {
                        this.mSplitView.addView(this.mMenuView);
                        this.mSplitActionBar = splitActionBar;
                    }
                    this.mMenuView.getLayoutParams().width = -1;
                } else {
                    addMenuViewForSystemView();
                    this.mSplitActionBar = splitActionBar;
                }
                this.mMenuView.requestLayout();
            }
            if (this.mSplitView != null) {
                this.mSplitView.setVisibility(splitActionBar ? 0 : 8);
            }
            setMenuPresenterStatus(splitActionBar);
        }
    }

    private void setMenuPresenterStatus(boolean splitActionBar) {
        if (this.mActionMenuPresenter != null) {
            if (splitActionBar) {
                this.mActionMenuPresenter.setExpandedActionViewsExclusive(false);
                this.mActionMenuPresenter.setWidthLimit(this.mContext.getResources().getDisplayMetrics().widthPixels, DEBUG);
                this.mActionMenuPresenter.setItemLimit(this.mMenuItemLimit);
            } else {
                this.mActionMenuPresenter.setExpandedActionViewsExclusive(getResources().getBoolean(17956866));
            }
        }
    }

    private void ensureHwMenu() {
        ensureHwMenuView();
        if (this.mMenu == null || this.mMenuView.peekMenu() == null) {
            this.mMenu = new MenuBuilder(this.mContext);
            this.mMenu.setCallback(new MenuBuilderCallback(this, null));
            initHwActionMenuPresenter();
            this.mActionMenuPresenter.setReserveOverflow(DEBUG);
            this.mActionMenuPresenter.setCallback(this.mActionMenuPresenterCallback != null ? this.mActionMenuPresenterCallback : new ActionMenuPresenterCallback());
            setMenuPresenterStatus(getSplitStatus());
            this.mMenu.addMenuPresenter(this.mActionMenuPresenter, this.mContext);
            this.mMenuView.setPresenter(this.mActionMenuPresenter);
            initExpandMenuPresenter();
            this.mMenu.addMenuPresenter(this.mExpandMenuPresenter, this.mContext);
        }
    }

    private void initHwActionMenuPresenter() {
        if (this.mActionMenuPresenter == null) {
            this.mActionMenuPresenter = new HwActionMenuPresenter(this.mContext, this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.LAYOUT, "hwtoolbar_menu_layout"), this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.LAYOUT, "hwtoolbar_menu_item_layout"));
        }
    }

    private void ensureHwMenuView() {
        if (this.mMenuView == null) {
            reflectMenuViewObejct();
            this.mMenuView.setPopupTheme(getPopupTheme());
            this.mMenuView.setOnMenuItemClickListener(this.mMenuViewItemClickListener);
            this.mMenuView.setMenuCallbacks(this.mActionMenuPresenterCallback, this.mMenuBuilderCallback);
            addMenuViewForSystemView();
        }
    }

    private void addMenuViewForSystemView() {
        if (this.mMenuView != null && this.mMenuView.getParent() != this) {
            LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = 8388613;
            lp.width = -2;
            this.mMenuView.setLayoutParams(lp);
            ReflectUtil.callMethod(this, "addSystemView", new Class[]{View.class, Boolean.TYPE}, new Object[]{this.mMenuView, Boolean.valueOf(false)}, Toolbar.class);
        }
    }

    private void updateViews() {
        int iconSize;
        int logoStartMaginId;
        int logoEndMaginId;
        ensureSplitView();
        ensureHwMenuView();
        Resources res = this.mResLoader.getResources(this.mContext);
        int hasIconMargin = res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_title_margin_start_with_icon"));
        int noIconMargin = res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_title_margin_start_no_icon"));
        int titleMarginEnd = !hasViewsEnd() ? noIconMargin : hasIconMargin;
        setTitleMarginStart(hasViewsStart() ? hasIconMargin : noIconMargin);
        setTitleMarginEnd(titleMarginEnd);
        View navView = getNavigationView();
        if (navView != null) {
            LayoutParams lpNav = (LayoutParams) navView.getLayoutParams();
            lpNav.setMarginStart(res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_navicon_margin_start")));
            iconSize = res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_icon_size"));
            lpNav.width = iconSize;
            lpNav.height = iconSize;
            lpNav.gravity = 16;
            navView.setLayoutParams(lpNav);
        }
        if (this.mLogoView == null) {
            this.mLogoView = (ImageView) ReflectUtil.getObject(this, "mLogoView", Toolbar.class);
        }
        if (this.mLogoView != null) {
            ActionBar.LayoutParams logoLp = (ActionBar.LayoutParams) this.mLogoView.getLayoutParams();
            logoStartMaginId = this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_logo_margin_start");
            logoEndMaginId = this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_logo_margin_end");
            int logoSize = res.getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_logo_size"));
            logoLp.setMarginStart(res.getDimensionPixelSize(logoStartMaginId));
            logoLp.setMarginEnd(res.getDimensionPixelSize(logoEndMaginId));
            logoLp.width = logoSize;
            logoLp.height = logoSize;
            this.mLogoView.setLayoutParams(logoLp);
        }
        int gravity = getTitleGravity();
        iconSize = 0;
        boolean z = (hasViewsStart() || this.mIcon1Visible) ? false : true;
        boolean marginStartNotZero = z;
        boolean marginEndNotZero = hasViewsEnd() ^ 1;
        int marginId = this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_title_layout_margin_start_no_icon");
        int marginStart = res.getDimensionPixelSize(marginId);
        logoStartMaginId = marginStart;
        logoEndMaginId = marginStartNotZero ? marginStart : 0;
        if (marginEndNotZero) {
            iconSize = logoStartMaginId;
        }
        invalidateTitleLayout(gravity, logoEndMaginId, iconSize, this.mTitleView, this.mSubTitleView);
        setNavButtonColor();
        triggerIconsVisible(this.mIcon1Visible, this.mIcon2Visible);
        setStartIconImage(this.mIcon1Drawable);
        setEndIconImage(this.mIcon2Drawable);
        setStartIconListener(this.mListener1);
        setEndIconListener(this.mListener2);
    }

    private void reflectMenuViewObejct() {
        this.mMenuView = (HwToolbarMenuView) LayoutInflater.from(this.mResLoader.getContext(this.mContext)).inflate(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.LAYOUT, "hwtoolbar_menu_layout"), null);
        ReflectUtil.setObject("mMenuView", this, this.mMenuView, Toolbar.class);
    }

    private void ensureSpinnerAdapter(int contentId) {
        this.mSpinnerAdapter = ArrayAdapter.createFromResource(this.mContext, contentId, this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.LAYOUT, "hwtoolbar_spinner_layout"));
    }

    private void ensureSplitView() {
        if (this.mSplitView == null) {
            Activity activity = null;
            if (this.mContext instanceof Activity) {
                activity = (Activity) this.mContext;
            }
            if (activity != null) {
                View decor = activity.getWindow().getDecorView();
                View splitView = decor.findViewById(16909357);
                if (splitView instanceof HwToolBarMenuContainer) {
                    this.mSplitView = (HwToolBarMenuContainer) splitView;
                } else {
                    this.mSplitView = new HwToolBarMenuContainer(this.mContext);
                    this.mSplitView.setVisibility(0);
                    this.mSplitView.setId(16909357);
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
                    lp.gravity = 80;
                    ((ViewGroup) decor.findViewById(16908290)).addView(this.mSplitView, lp);
                }
            }
        }
    }

    private boolean hasViewsStart() {
        View navView = getNavigationView();
        boolean hasnavView = (navView != null && navView.getVisibility() == 0 && navView.getParent() == this) ? DEBUG : false;
        boolean hasLogo = (this.mLogoView != null && this.mLogoView.getVisibility() == 0 && this.mLogoView.getParent() == this) ? DEBUG : false;
        return (hasnavView || hasLogo) ? DEBUG : false;
    }

    private void initIconLayout() {
        this.mIconLayout = LayoutInflater.from(this.mResLoader.getContext(this.mContext)).inflate(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.LAYOUT, "hwtoolbar_title_item_layout"), null);
        this.mTitleContainer = (LinearLayout) this.mIconLayout.findViewById(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.ID, "titleContainer"));
        this.mIcon1View = (ImageView) ((ViewStub) this.mIconLayout.findViewById(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.ID, "hwtoolbar_icon1"))).inflate().findViewById(16908295);
        this.mIcon1View.setBackground(HwWidgetUtils.getHwAnimatedGradientDrawable(this.mContext, this.mClickEffectEntry));
        this.mIcon2View = (ImageView) ((ViewStub) this.mIconLayout.findViewById(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.ID, "hwtoolbar_icon2"))).inflate().findViewById(16908296);
        this.mIcon2View.setBackground(HwWidgetUtils.getHwAnimatedGradientDrawable(this.mContext, this.mClickEffectEntry));
        this.mTitleView = (HwTextView) this.mIconLayout.findViewById(16908691);
        this.mSubTitleView = (HwTextView) this.mIconLayout.findViewById(16908690);
        this.mSpinner = (Spinner) this.mIconLayout.findViewById(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.ID, "titleSpinner"));
        this.mTitleView.setAutoTextInfo(this.mTitleMinSize, this.mTitleSizeStep, 2);
        this.mSubTitleView.setAutoTextInfo(this.mSubTitleMinSize, this.mTitleSizeStep, 1);
        this.mResOk = this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DRAWABLE, "ic_public_ok");
        this.mResCancel = this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DRAWABLE, "ic_public_cancel");
        int titleTextAppearance = ((Integer) ReflectUtil.getObject(this, "mTitleTextAppearance", Toolbar.class)).intValue();
        int subTitleTextAppearance = ((Integer) ReflectUtil.getObject(this, "mSubtitleTextAppearance", Toolbar.class)).intValue();
        if (titleTextAppearance != 0) {
            this.mTitleView.setTextAppearance(titleTextAppearance);
        }
        if (subTitleTextAppearance != 0) {
            this.mSubTitleView.setTextAppearance(subTitleTextAppearance);
        }
        this.mSubTitleView.setAutoTextSize(1, (float) this.mSubTitleNormalSize);
        initIconsColor();
    }

    private void setStartIconVisible(boolean icon1Visible) {
        this.mIcon1Visible = icon1Visible;
        triggerIconsVisible(this.mIcon1Visible, this.mIcon2Visible);
    }

    private void setEndIconVisible(boolean icon2Visible) {
        this.mIcon2Visible = icon2Visible;
        triggerIconsVisible(this.mIcon1Visible, this.mIcon2Visible);
    }

    private void triggerIconsVisible(boolean icon1Visible, boolean icon2Visible) {
        if (this.mIcon1View != null && this.mIcon2View != null) {
            int i = 4;
            if (icon1Visible) {
                this.mIcon1View.setVisibility(0);
            } else {
                this.mIcon1View.setVisibility(getTitleGravity() == 8388611 ? 8 : 4);
            }
            if (icon2Visible) {
                this.mIcon2View.setVisibility(0);
            } else {
                ImageView imageView = this.mIcon2View;
                if (!icon1Visible) {
                    i = 8;
                }
                imageView.setVisibility(i);
            }
        }
    }

    private void setStartIconImage(Drawable icon1) {
        if (this.mIcon1View != null) {
            if (icon1 != null) {
                this.mIcon1Drawable = icon1;
                this.mIcon1View.setImageDrawable(icon1);
            } else {
                this.mIcon1Drawable = null;
                if (this.mResCancel != 0) {
                    this.mIcon1View.setImageResource(this.mResCancel);
                }
            }
        }
    }

    private void setEndIconImage(Drawable icon2) {
        if (this.mIcon2View != null) {
            if (icon2 != null) {
                this.mIcon2Drawable = icon2;
                this.mIcon2View.setImageDrawable(icon2);
            } else {
                this.mIcon2Drawable = null;
                if (this.mResOk != 0) {
                    this.mIcon2View.setImageResource(this.mResOk);
                }
            }
        }
    }

    private void setStartIconListener(OnClickListener listener1) {
        if (this.mIcon1View != null) {
            this.mListener1 = listener1;
            this.mIcon1View.setOnClickListener(listener1);
        }
    }

    private void setEndIconListener(OnClickListener listener2) {
        if (this.mIcon2View != null) {
            this.mListener2 = listener2;
            this.mIcon2View.setOnClickListener(listener2);
        }
    }

    private boolean hasViewsEnd() {
        int menuCount = this.mMenuView.getChildCount();
        boolean childVisible = false;
        for (int i = 0; i < menuCount; i++) {
            if (this.mMenuView.getChildAt(i).getVisibility() != 8) {
                childVisible = DEBUG;
                break;
            }
        }
        boolean hasMenu = (shouldLayout(this.mMenuView) && childVisible) ? DEBUG : false;
        return (hasMenu || !(!shouldLayout(this.mIconLayout) || this.mIcon2View == null || this.mIcon2View.getVisibility() == 8)) ? DEBUG : false;
    }

    private void invalidateTitleLayout(int gravity, int marginStart, int marginEnd, TextView title, TextView subTitle) {
        LinearLayout.LayoutParams lpTitle;
        if (title != null) {
            lpTitle = (LinearLayout.LayoutParams) title.getLayoutParams();
            lpTitle.gravity = gravity;
            lpTitle.setMarginStart(marginStart);
            lpTitle.setMarginEnd(marginEnd);
            title.setLayoutParams(lpTitle);
        }
        if (subTitle != null && subTitle.getVisibility() == 0) {
            lpTitle = (LinearLayout.LayoutParams) subTitle.getLayoutParams();
            lpTitle.gravity = gravity;
            lpTitle.setMarginStart(marginStart);
            lpTitle.setMarginEnd(marginEnd);
            subTitle.setLayoutParams(lpTitle);
        }
        if (this.mSpinner != null && this.mSpinner.getVisibility() == 0) {
            lpTitle = (LinearLayout.LayoutParams) this.mSpinner.getLayoutParams();
            lpTitle.setMarginStart(marginStart);
            lpTitle.setMarginEnd(marginEnd);
            this.mSpinner.setLayoutParams(lpTitle);
        }
    }

    private int getTitleGravity() {
        return 8388611;
    }

    private void initIconsColor() {
        if (!(this.mResCancel == 0 || this.mIcon1View == null)) {
            this.mIcon1View.setImageResource(this.mResCancel);
            if (this.mIconColor != null) {
                this.mIcon1View.setImageTintList(this.mIconColor);
            }
        }
        if (this.mResOk != 0 && this.mIcon2View != null) {
            this.mIcon2View.setImageResource(this.mResOk);
            if (this.mIconColor != null) {
                this.mIcon2View.setImageTintList(this.mIconColor);
            }
        }
    }

    private void setNavButtonColor() {
        View navView = getNavigationView();
        if (navView instanceof ImageButton) {
            ImageButton navButton = (ImageButton) navView;
            if (this.mIconColor != null) {
                navButton.setImageTintList(this.mIconColor);
                navButton.setBackground(HwWidgetUtils.getHwAnimatedGradientDrawable(this.mContext, this.mClickEffectEntry));
            }
        }
    }

    public boolean getSplitStatus() {
        Activity activity = null;
        if (this.mContext instanceof Activity) {
            activity = (Activity) this.mContext;
        }
        boolean z = false;
        if (activity == null) {
            Log.w(TAG, "can not get the Activity of toolbar in getSplitStatus()");
            return false;
        }
        try {
            ActivityInfo info = activity.getPackageManager().getActivityInfo(activity.getComponentName(), 1);
            if (info != null) {
                boolean splitActionBar = false;
                if ((info.uiOptions & 1) != 0 ? DEBUG : false) {
                    if (this.mIsSetDynamicSplitMenu) {
                        return this.mDynamicSplitMenu;
                    }
                    boolean isMultiWindow = activity.isInMultiWindowMode();
                    boolean portrait = this.mContext.getResources().getConfiguration().orientation == 1 ? DEBUG : false;
                    if (this.mForceSplit || isMultiWindow || portrait) {
                        z = DEBUG;
                    }
                    splitActionBar = z;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getSplitStatus: orientation is portrait: ");
                    stringBuilder.append(portrait);
                    stringBuilder.append(" and isInMultiWindow: ");
                    stringBuilder.append(isMultiWindow);
                    stringBuilder.append(" and forceSplit: ");
                    stringBuilder.append(this.mForceSplit);
                    Log.d(str, stringBuilder.toString());
                }
                return splitActionBar;
            }
            Log.w(TAG, "can not get the uiOptions in getSplitStatus()");
            return false;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "activity.getComponentName not found");
            return false;
        }
    }

    private void splitToolbar() {
        setSplitToolbar(getSplitStatus());
    }

    public void draw(Canvas canvas) {
        if (this.mBlurEngine.isShowHwBlur(this)) {
            this.mBlurEngine.draw(canvas, this);
            super.dispatchDraw(canvas);
            return;
        }
        super.draw(canvas);
    }

    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == 0) {
            this.mBlurEngine.addBlurTargetView(this, BlurType.LightBlurWithGray);
            this.mBlurEngine.setTargetViewBlurEnable(this, isBlurEnable());
            return;
        }
        this.mBlurEngine.removeBlurTargetView(this);
    }

    public boolean isBlurEnable() {
        return this.isBlurEnable;
    }

    public void setBlurEnable(boolean blurEnable) {
        this.isBlurEnable = blurEnable;
        this.mBlurEngine.setTargetViewBlurEnable(this, isBlurEnable());
    }

    public HwToolBarMenuContainer getSplieView() {
        if (this.mSplitView == null) {
            ensureSplitView();
        }
        return this.mSplitView;
    }
}
