package huawei.android.widget;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hwcontrol.HwWidgetFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.widget.ActionMenuView.OnMenuItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;
import android.widget.Toolbar.LayoutParams;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuPresenter.Callback;
import huawei.android.widget.DecouplingUtil.ReflectUtil;

public class HwToolbar extends Toolbar {
    private static final String TAG = "HwToolbar";
    private HwActionMenuPresenter mActionMenuPresenter;
    private Callback mActionMenuPresenterCallback;
    private boolean mAlwaysSplit;
    private int mColor;
    private View mCustomTitleView;
    private boolean mDisplayNoSplitLine;
    private Drawable mIcon1Drawable;
    private ImageView mIcon1View;
    private boolean mIcon1Visible;
    private Drawable mIcon2Drawable;
    private ImageView mIcon2View;
    private boolean mIcon2Visible;
    private View mIconLayout;
    private OnClickListener mListener1;
    private OnClickListener mListener2;
    private MenuBuilder mMenu;
    private MenuBuilder.Callback mMenuBuilderCallback;
    private HwActionMenuView mMenuView;
    private final OnMenuItemClickListener mMenuViewItemClickListener = new OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            if (HwToolbar.this.mOnMenuItemClickListener != null) {
                return HwToolbar.this.mOnMenuItemClickListener.onMenuItemClick(item);
            }
            return false;
        }
    };
    private Toolbar.OnMenuItemClickListener mOnMenuItemClickListener;
    private int mPopupEndLocation;
    private int mPopupStartLocation;
    private int mResCancel;
    private int mResOk;
    private boolean mSplitActionBar;
    private HwToolBarMenuContainer mSplitView;
    private int mSplitViewMaxSize;
    private TextView mSubTitleView;
    private LinearLayout mTitleContainer;
    private TextView mTitleView;

    private static class ActionMenuPresenterCallback implements Callback {
        private ActionMenuPresenterCallback() {
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

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            if (HwToolbar.this.mMenuViewItemClickListener != null) {
                return HwToolbar.this.mMenuViewItemClickListener.onMenuItemClick(item);
            }
            return false;
        }

        public void onMenuModeChange(MenuBuilder menu) {
            if (HwToolbar.this.mMenuBuilderCallback != null) {
                HwToolbar.this.mMenuBuilderCallback.onMenuModeChange(menu);
            }
        }
    }

    public HwToolbar(Context context) {
        super(context);
        setBackgroundColors(getContext(), false);
    }

    public HwToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColors(getContext(), false);
    }

    public HwToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColors(getContext(), false);
    }

    public HwToolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setBackgroundColors(getContext(), false);
    }

    public void setStartIcon(boolean icon1Visible, Drawable icon1, OnClickListener listener1) {
        if (this.mIcon1View == null || this.mIcon2View == null) {
            initIconLayout();
        }
        if (this.mIcon1View != null && this.mIcon2View != null) {
            setStartIconVisible(icon1Visible);
            setStartIconImage(icon1);
            setStartIconListener(listener1);
        }
    }

    public void setEndIcon(boolean icon2Visible, Drawable icon2, OnClickListener listener2) {
        if (this.mIcon1View == null || this.mIcon2View == null) {
            initIconLayout();
        }
        if (this.mIcon1View != null && this.mIcon2View != null) {
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

    public View getIconLayout() {
        return this.mIconLayout;
    }

    public void setSplitViewAnimationEnable(boolean enableAnim) {
        if (this.mSplitView != null) {
            this.mSplitView.setAnimationEnable(enableAnim);
        }
    }

    public void setSplitBackgroundDrawable(Drawable d) {
        if (this.mSplitView != null) {
            this.mSplitView.setSplitBackground(d);
            this.mSplitView.setForcedSplitBackground(true);
        }
    }

    public void setTitle(CharSequence title) {
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
        super.setTitle(title);
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

    public void setLogo(Drawable drawable) {
    }

    public void setSplitViewLocation(int start, int end) {
        if (this.mSplitView != null) {
            this.mSplitView.setSplitViewLocation(start, end);
        }
        this.mSplitViewMaxSize = end - start;
        this.mPopupStartLocation = start;
        this.mPopupEndLocation = end;
        updateSplitLocation();
    }

    private void updateSplitLocation() {
        if (this.mMenuView != null) {
            this.mMenuView.setSplitViewMaxSize(this.mSplitViewMaxSize);
        }
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.setPopupLocation(this.mPopupStartLocation, this.mPopupEndLocation);
        }
    }

    public void setDisplayNoSplitLine(boolean displayNoSplitLine) {
        this.mDisplayNoSplitLine = displayNoSplitLine;
        if (this.mDisplayNoSplitLine) {
            setBackgroundColors(getContext(), true);
        }
    }

    public void setSplitActionBarAlways(boolean bAlwaysSplit) {
        this.mAlwaysSplit = bAlwaysSplit;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updateViews();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        splitToolbar();
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.onConfigurationChanged(newConfig);
        }
    }

    public Menu getMenu() {
        ensureHwMenu();
        splitToolbar();
        updateSplitLocation();
        return this.mMenu;
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
                    }
                    this.mMenuView.getLayoutParams().width = -1;
                } else {
                    addMenuViewForSystemView();
                }
                this.mMenuView.requestLayout();
            }
            if (this.mSplitView != null) {
                this.mSplitView.setVisibility(splitActionBar ? 0 : 8);
            }
            setMenuPresenterStatus(splitActionBar);
            this.mSplitActionBar = splitActionBar;
        }
    }

    private void setMenuPresenterStatus(boolean splitActionBar) {
        if (this.mActionMenuPresenter != null) {
            if (splitActionBar) {
                this.mActionMenuPresenter.setExpandedActionViewsExclusive(false);
                this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
                this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
            } else {
                this.mActionMenuPresenter.setExpandedActionViewsExclusive(getResources().getBoolean(17956866));
            }
        }
    }

    private void ensureHwMenu() {
        ensureHwMenuView();
        if (this.mMenu == null || this.mMenuView.peekMenu() == null) {
            this.mMenu = new MenuBuilder(getContext());
            this.mMenu.setCallback(new MenuBuilderCallback());
            this.mActionMenuPresenter = new HwActionMenuPresenter(getContext(), 34013188, 34013187);
            this.mActionMenuPresenter.setReserveOverflow(true);
            this.mActionMenuPresenter.setCallback(this.mActionMenuPresenterCallback != null ? this.mActionMenuPresenterCallback : new ActionMenuPresenterCallback());
            setMenuPresenterStatus(getSplitStatus());
            this.mMenu.addMenuPresenter(this.mActionMenuPresenter, getContext());
            this.mActionMenuPresenter.setMenuView(this.mMenuView);
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
        int titleMarginStart;
        ensureSplitView();
        ensureHwMenuView();
        if (isShowUp()) {
            titleMarginStart = getResources().getDimensionPixelSize(34472181);
        } else {
            titleMarginStart = getResources().getDimensionPixelSize(34472198);
        }
        setTitleMarginStart(titleMarginStart);
        View navView = getNavigationView();
        if (navView != null) {
            LayoutParams lpNav = (LayoutParams) navView.getLayoutParams();
            lpNav.setMarginStart(getResources().getDimensionPixelSize(34472177));
            navView.setLayoutParams(lpNav);
        }
        int gravity = getTitleGravity();
        int i = !isShowUp() ? this.mIcon1Visible ^ 1 : 0;
        int margin = getResources().getDimensionPixelSize(34472184);
        if (i == 0) {
            margin = 0;
        }
        invalidateTitleLayout(gravity, margin, this.mTitleView, this.mSubTitleView);
        initTitleAppearance();
        setNavButtonColor();
        if (this.mTitleView != null) {
            this.mTitleView.setText(getTitle());
        }
        if (this.mSubTitleView != null) {
            this.mSubTitleView.setText(getSubtitle());
        }
        triggerIconsVisible(this.mIcon1Visible, this.mIcon2Visible);
        setStartIconImage(this.mIcon1Drawable);
        setEndIconImage(this.mIcon2Drawable);
        setStartIconListener(this.mListener1);
        setEndIconListener(this.mListener2);
    }

    private void reflectMenuViewObejct() {
        this.mMenuView = (HwActionMenuView) LayoutInflater.from(getContext()).inflate(34013188, null);
        ReflectUtil.setObject("mMenuView", this, this.mMenuView, Toolbar.class);
    }

    private void ensureSplitView() {
        if (this.mSplitView == null) {
            this.mSplitView = new HwToolBarMenuContainer(getContext());
            this.mSplitView.setVisibility(0);
            this.mSplitView.setId(16909317);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
            lp.gravity = 80;
            Context context = getContext();
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }
            if (activity != null) {
                ((ViewGroup) activity.getWindow().getDecorView().findViewById(16908290)).addView(this.mSplitView, lp);
            }
        }
    }

    private boolean isShowUp() {
        View navView = getNavigationView();
        if (navView != null && navView.getVisibility() == 0 && navView.getParent() == this) {
            return true;
        }
        return false;
    }

    private void initIconLayout() {
        this.mIconLayout = LayoutInflater.from(getContext()).inflate(34013186, null);
        this.mTitleContainer = (LinearLayout) this.mIconLayout.findViewById(34603090);
        this.mIcon1View = (ImageView) ((ViewStub) this.mIconLayout.findViewById(34603091)).inflate().findViewById(16908295);
        this.mIcon2View = (ImageView) ((ViewStub) this.mIconLayout.findViewById(34603092)).inflate().findViewById(16908296);
        this.mTitleView = (TextView) this.mIconLayout.findViewById(16908685);
        this.mSubTitleView = (TextView) this.mIconLayout.findViewById(16908684);
        this.mResOk = 33751080;
        this.mResCancel = 33751079;
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionBar, 16843470, 0);
        int titleTextStyle = a.getResourceId(11, 0);
        if (titleTextStyle != 0) {
            this.mTitleView.setTextAppearance(titleTextStyle);
        }
        initIconsColor();
        a.recycle();
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
        int i = 4;
        if (this.mIcon1View != null && this.mIcon2View != null) {
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

    private void setBackgroundColors(Context context, boolean forceRefresh) {
        if (!HwWidgetUtils.isActionbarBackgroundThemed(context) && (HwWidgetFactory.isHwDarkTheme(context) ^ 1) != 0) {
            int color = HwWidgetFactory.getPrimaryColor(context);
            boolean force = forceRefresh;
            if (Color.alpha(color) == 0) {
                return;
            }
            if (this.mColor != color || forceRefresh) {
                LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{new ColorDrawable(color)});
                if (needSplitLine(color) && (this.mDisplayNoSplitLine ^ 1) != 0) {
                    Drawable splitDrawable = context.getDrawable(33751637);
                    if (splitDrawable != null) {
                        layerDrawable.addLayer(splitDrawable);
                    } else {
                        Log.w(TAG, "splitDrawable is null");
                    }
                }
                setBackground(layerDrawable);
                this.mColor = color;
            }
        } else if (HwWidgetFactory.isHwDarkTheme(context) || HwWidgetFactory.isHwEmphasizeTheme(context)) {
            setBackgroundResource(33751560);
        }
    }

    private boolean needSplitLine(int color) {
        if (color == -197380 || color == -16777216) {
            return true;
        }
        return false;
    }

    private void invalidateTitleLayout(int gravity, int margin, TextView title, TextView subTitle) {
        if (title != null) {
            LinearLayout.LayoutParams lpTitle = (LinearLayout.LayoutParams) title.getLayoutParams();
            lpTitle.gravity = gravity;
            lpTitle.setMarginStart(margin);
            title.setLayoutParams(lpTitle);
        }
        if (subTitle != null && subTitle.getVisibility() == 0) {
            LinearLayout.LayoutParams lpSubTitle = (LinearLayout.LayoutParams) subTitle.getLayoutParams();
            lpSubTitle.gravity = gravity;
            subTitle.setLayoutParams(lpSubTitle);
        }
    }

    private void initTitleAppearance() {
        if (this.mTitleView != null) {
            HwWidgetFactory.setImmersionStyle(getContext(), this.mTitleView, 33882238, 33882237, 0, false);
        }
        if (this.mSubTitleView != null) {
            HwWidgetFactory.setImmersionStyle(getContext(), this.mSubTitleView, 33882238, 33882237, 0, false);
        }
    }

    private int getTitleGravity() {
        return this.mIcon1Visible ? 1 : 8388611;
    }

    private void initIconsColor() {
        ColorStateList color = getImmersionTint(getContext());
        if (!(this.mResCancel == 0 || this.mIcon1View == null)) {
            this.mIcon1View.setImageResource(this.mResCancel);
            this.mIcon1View.setImageTintList(color);
        }
        if (this.mResOk != 0 && this.mIcon2View != null) {
            this.mIcon2View.setImageResource(this.mResOk);
            this.mIcon2View.setImageTintList(color);
        }
    }

    private void setNavButtonColor() {
        ColorStateList color = getImmersionTint(getContext());
        View navView = getNavigationView();
        if (navView instanceof ImageButton) {
            ((ImageButton) navView).setImageTintList(color);
        }
    }

    private ColorStateList getImmersionTint(Context context) {
        int resTint = HwWidgetFactory.getImmersionResource(context, 33882140, 0, 33882388, true);
        if (HwWidgetFactory.isHwEmphasizeTheme(context)) {
            resTint = 33882402;
        }
        return context.getColorStateList(resTint);
    }

    private boolean getSplitStatus() {
        Context context = getContext();
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        }
        if (activity == null) {
            Log.w(TAG, "can not get the Activity of toolbar in getSplitStatus()");
            return false;
        }
        ActivityInfo info = null;
        try {
            info = activity.getPackageManager().getActivityInfo(activity.getComponentName(), 1);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "activity.getComponentName not found");
        }
        if (info != null) {
            boolean splitActionBar = false;
            if ((info.uiOptions & 1) != 0) {
                splitActionBar = this.mAlwaysSplit ? true : getContext().getResources().getBoolean(17957097);
            }
            return splitActionBar;
        }
        Log.w(TAG, "can not get the uiOptions in getSplitStatus()");
        return false;
    }

    private void splitToolbar() {
        setSplitToolbar(getSplitStatus());
    }
}
