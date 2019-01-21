package huawei.android.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.android.internal.app.ToolbarActionBar;
import com.huawei.android.app.ActionBarEx;
import huawei.android.widget.effect.engine.HwBlurEngine;
import huawei.android.widget.effect.engine.HwBlurEngine.BlurType;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;

public class HwImmersiveMode extends RelativeLayout {
    private static final String KEY_NAVIGATION_BAR_STATUS = "navigationbar_is_min";
    private static final String TAG = "HwImmersiveMode";
    private Activity mActivity;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            String str = HwImmersiveMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onNavigationBarStatusChanged: ------------ selfChange = ");
            stringBuilder.append(selfChange);
            Log.d(str, stringBuilder.toString());
            HwImmersiveMode.this.updateImmersiveMode();
        }
    };
    private final Context mContext;
    private boolean mIsInMultiWindow;
    private boolean mIsInMultiWindowRight;
    private boolean mIsInMultiWindowTop;
    private boolean mIsNavigationBarBlurEnabled;
    private boolean mIsShowHwBlur = HwBlurEngine.getInstance().isShowHwBlur();
    private boolean mIsStatusBarBlurEnabled;
    private OnGlobalLayoutListener mLayoutListener = new OnGlobalLayoutListener() {
        public void onGlobalLayout() {
            if (HwImmersiveMode.this.isMultiWindowPositionChanged()) {
                HwImmersiveMode.this.updateImmersiveMode();
            }
        }
    };
    private NavigationBarBlurView mNavigationBarView;
    private int mOrientation;
    private View mSplitView;
    private StatusBarBlurView mStatusBarView;

    private class BlurView extends View {
        private static final int ALPHA_CHANNEL = -16777216;
        private static final int DEFAULT_GRAY = -855310;
        protected static final int TRANS_WHITE = 16777215;
        protected HwBlurEngine blurEngine = HwBlurEngine.getInstance();
        private boolean isOverlayColorChanged = false;
        protected int overlayColor;

        public BlurView(Context context) {
            super(context);
            setBackgroundColor(DEFAULT_GRAY);
        }

        public void draw(Canvas canvas) {
            if (this.blurEngine.isShowHwBlur(this)) {
                this.blurEngine.draw(canvas, this);
                super.dispatchDraw(canvas);
                return;
            }
            super.draw(canvas);
        }

        public void setTargetViewOverlayColor(int overlayColor) {
            if (!this.isOverlayColorChanged) {
                this.isOverlayColorChanged = true;
            }
            this.overlayColor = overlayColor;
            setBackgroundColor(ALPHA_CHANNEL | overlayColor);
        }

        protected void onWindowVisibilityChanged(View targetView, int visibility) {
            if (visibility == 0) {
                this.blurEngine.addBlurTargetView(targetView, BlurType.LightBlurWithGray);
                this.blurEngine.setTargetViewBlurEnable(targetView, true);
                return;
            }
            this.blurEngine.removeBlurTargetView(targetView);
        }

        protected void updateOverlayColor(View targetView) {
            if (this.blurEngine.isShowHwBlur(targetView) && this.isOverlayColorChanged) {
                this.blurEngine.setTargetViewOverlayColor(targetView, this.overlayColor);
            }
        }
    }

    private class NavigationBarBlurView extends BlurView {
        public NavigationBarBlurView(Context context) {
            super(context);
        }

        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            onWindowVisibilityChanged(this, visibility);
            if (visibility == 0) {
                updateStatus();
            }
        }

        public void updateStatus() {
            boolean isLandscape = getResources().getConfiguration().orientation == 2;
            String str = HwImmersiveMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateStatus: [navigation] isScreenLandscape = ");
            stringBuilder.append(isLandscape);
            Log.d(str, stringBuilder.toString());
            if (isLandscape) {
                setVisibility(8);
                return;
            }
            String str2 = HwImmersiveMode.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateStatus: [navigation] isNavigationBarExist = ");
            stringBuilder2.append(HwImmersiveMode.this.isNavigationBarExist());
            Log.d(str2, stringBuilder2.toString());
            if (HwImmersiveMode.this.isNavigationBarExist()) {
                str2 = HwImmersiveMode.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateStatus: [navigation] mIsNavigationBarBlurEnabled = ");
                stringBuilder2.append(HwImmersiveMode.this.mIsNavigationBarBlurEnabled);
                Log.d(str2, stringBuilder2.toString());
                if (HwImmersiveMode.this.mIsNavigationBarBlurEnabled) {
                    boolean isInMultiWindow = HwImmersiveMode.this.mActivity.isInMultiWindowMode();
                    String str3 = HwImmersiveMode.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("updateStatus: [navigation] isInMultiWindow = ");
                    stringBuilder3.append(isInMultiWindow);
                    Log.d(str3, stringBuilder3.toString());
                    if (isInMultiWindow) {
                        HwImmersiveMode.this.fetchMultiWindowPosition();
                        str3 = HwImmersiveMode.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("updateStatus: [navigation] mIsInMultiWindowTop = ");
                        stringBuilder3.append(HwImmersiveMode.this.mIsInMultiWindowTop);
                        Log.d(str3, stringBuilder3.toString());
                        str3 = HwImmersiveMode.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("updateStatus: [navigation] mIsInMultiWindowRight = ");
                        stringBuilder3.append(HwImmersiveMode.this.mIsInMultiWindowRight);
                        Log.d(str3, stringBuilder3.toString());
                    }
                    if (isInMultiWindow && HwImmersiveMode.this.mIsInMultiWindowTop) {
                        setVisibility(8);
                        return;
                    }
                    setVisibility(0);
                    HwImmersiveMode.this.mActivity.getWindow().setNavigationBarColor(16777215);
                    updateOverlayColor(this);
                    return;
                }
                setVisibility(8);
                return;
            }
            setVisibility(8);
        }
    }

    private class StatusBarBlurView extends BlurView {
        public StatusBarBlurView(Context context) {
            super(context);
        }

        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            onWindowVisibilityChanged(this, visibility);
            if (visibility == 0) {
                updateStatus();
            }
        }

        public void updateStatus() {
            String str = HwImmersiveMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateStatus: [status] mIsStatusBarBlurEnabled = ");
            stringBuilder.append(HwImmersiveMode.this.mIsStatusBarBlurEnabled);
            Log.d(str, stringBuilder.toString());
            if (HwImmersiveMode.this.mIsStatusBarBlurEnabled) {
                boolean isInMultiWindow = HwImmersiveMode.this.mActivity.isInMultiWindowMode();
                String str2 = HwImmersiveMode.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateStatus: [status] isInMultiWindow = ");
                stringBuilder2.append(isInMultiWindow);
                Log.d(str2, stringBuilder2.toString());
                if (isInMultiWindow) {
                    HwImmersiveMode.this.fetchMultiWindowPosition();
                    str2 = HwImmersiveMode.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateStatus: [status] mIsInMultiWindowTop = ");
                    stringBuilder2.append(HwImmersiveMode.this.mIsInMultiWindowTop);
                    Log.d(str2, stringBuilder2.toString());
                    str2 = HwImmersiveMode.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateStatus: [status] mIsInMultiWindowRight = ");
                    stringBuilder2.append(HwImmersiveMode.this.mIsInMultiWindowRight);
                    Log.d(str2, stringBuilder2.toString());
                }
                if (!isInMultiWindow || HwImmersiveMode.this.mIsInMultiWindowTop) {
                    LayoutParams params = (LayoutParams) getLayoutParams();
                    if (isInMultiWindow && HwImmersiveMode.this.mIsInMultiWindowRight && HwImmersiveMode.this.isNavigationBarExist()) {
                        params.setMargins(0, 0, HwImmersiveMode.this.getNavigationHeight(), 0);
                    } else {
                        params.setMargins(0, 0, 0, 0);
                    }
                    setLayoutParams(params);
                    setVisibility(0);
                    HwImmersiveMode.this.mActivity.getWindow().setStatusBarColor(16777215);
                    updateOverlayColor(this);
                    return;
                }
                setVisibility(8);
                return;
            }
            setVisibility(8);
        }
    }

    public HwImmersiveMode(Activity activity) {
        super(activity);
        this.mContext = activity;
        this.mActivity = activity;
        setupStatusBarView(this.mContext);
        setupNavigationBarView(this.mContext);
        ((ViewGroup) ((ViewGroup) this.mActivity.findViewById(16908290)).getRootView()).addView(this);
        this.mOrientation = this.mContext.getResources().getConfiguration().orientation;
    }

    public void setStatusBarBlurEnable(boolean enabled) {
        boolean z = enabled && isThemeSupport(this.mActivity);
        enabled = z;
        if (this.mIsStatusBarBlurEnabled != enabled) {
            this.mIsStatusBarBlurEnabled = enabled;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setStatusBarBlurEnable: enabled = ");
            stringBuilder.append(enabled);
            Log.d(str, stringBuilder.toString());
            if (getWindowVisibility() == 0 && this.mStatusBarView != null) {
                this.mStatusBarView.updateStatus();
            }
        }
    }

    public void setNavigationBarBlurEnable(boolean enabled) {
        boolean z = enabled && isThemeSupport(this.mActivity);
        enabled = z;
        if (this.mIsNavigationBarBlurEnabled != enabled) {
            this.mIsNavigationBarBlurEnabled = enabled;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNavigationBarBlurEnable: enabled = ");
            stringBuilder.append(enabled);
            Log.d(str, stringBuilder.toString());
            if (getWindowVisibility() == 0 && this.mNavigationBarView != null) {
                this.mNavigationBarView.updateStatus();
            }
        }
    }

    public void setStatusBarOverlayColor(int overlayColor) {
        if (this.mIsShowHwBlur && this.mStatusBarView != null && this.mIsStatusBarBlurEnabled) {
            this.mStatusBarView.setTargetViewOverlayColor(overlayColor);
        }
    }

    public void setNavigationBarOverlayColor(int overlayColor) {
        if (this.mIsShowHwBlur && this.mNavigationBarView != null && this.mIsNavigationBarBlurEnabled) {
            this.mNavigationBarView.setTargetViewOverlayColor(overlayColor);
        }
    }

    public void setHwToolbarBlurEnable(HwToolbar hwToolbar, boolean enabled) {
        if (hwToolbar != null) {
            hwToolbar.setBlurEnable(enabled);
        }
    }

    public void setSpiltViewBlurEnable(HwToolbar hwToolbar, boolean enabled) {
        if (hwToolbar != null) {
            ActionBarEx.setSpiltViewBlurEnable(hwToolbar, enabled);
        }
    }

    public void setActionBarBlurEnable(ActionBar actionBar, boolean enabled) {
        if (actionBar != null) {
            ActionBarEx.setBlurEnable(actionBar, enabled);
        }
    }

    public void setSpiltViewBlurEnable(ActionBar actionBar, boolean enabled) {
        if (actionBar != null) {
            ActionBarEx.setSpiltViewBlurEnable(actionBar, enabled);
        }
    }

    public void setSubTabWidgetBlurEnable(SubTabWidget subTabWidget, boolean enabled) {
        if (subTabWidget != null) {
            subTabWidget.setBlurEnable(enabled);
        }
    }

    public void setHwBottomNavigationViewBlurEnable(HwBottomNavigationView hwBottomNavigationView, boolean enabled) {
        if (hwBottomNavigationView != null) {
            hwBottomNavigationView.setBlurEnable(enabled);
        }
    }

    public void setMultiWindowModeChanged(boolean isInMultiWindowMode) {
        this.mIsInMultiWindow = isInMultiWindowMode;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onMultiWindowModeChanged: ------------isInMultiWindowMode = ");
        stringBuilder.append(isInMultiWindowMode);
        Log.d(str, stringBuilder.toString());
        updateImmersiveMode();
    }

    private void fetchMultiWindowPosition() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        boolean z = true;
        this.mIsInMultiWindowTop = location[1] <= 0;
        if (location[0] <= 0) {
            z = false;
        }
        this.mIsInMultiWindowRight = z;
    }

    private boolean isMultiWindowPositionChanged() {
        if (this.mActivity == null || !this.mActivity.isInMultiWindowMode()) {
            return false;
        }
        int[] location = new int[2];
        getLocationOnScreen(location);
        boolean isInMultiWindowTop = location[1] <= 0;
        boolean isInMultiWindowRight = location[0] > 0;
        if (isInMultiWindowTop == this.mIsInMultiWindowTop && isInMultiWindowRight == this.mIsInMultiWindowRight) {
            return false;
        }
        this.mIsInMultiWindowTop = isInMultiWindowTop;
        this.mIsInMultiWindowRight = isInMultiWindowRight;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onMultiWindowPositionChanged: ------------ ");
        stringBuilder.append(isInMultiWindowTop ? "[top]" : "[bottom]");
        stringBuilder.append(isInMultiWindowRight ? "[right]" : "[left]");
        Log.d(str, stringBuilder.toString());
        return true;
    }

    private void updateImmersiveMode() {
        if (this.mActivity != null && this.mStatusBarView != null && this.mNavigationBarView != null) {
            this.mStatusBarView.updateStatus();
            this.mNavigationBarView.updateStatus();
            updateSplitViewPositon();
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mOrientation = newConfig.orientation;
    }

    private void updateSplitViewPositon() {
        Log.d(TAG, "updateSplitViewPositon: ");
        int bottomMargin = 0;
        if (this.mActivity != null) {
            if (this.mSplitView == null) {
                this.mSplitView = getRootView().findViewById(16909357);
                if (this.mSplitView == null) {
                    return;
                }
            }
            if (!this.mActivity.isInMultiWindowMode() && isNavigationBarExist() && getResources().getConfiguration().orientation == 1) {
                bottomMargin = getNavigationHeight();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateSplitViewPositon: bottomMargin=");
                stringBuilder.append(bottomMargin);
                Log.d(str, stringBuilder.toString());
            }
            if (this.mActivity.getActionBar() instanceof ToolbarActionBar) {
                this.mSplitView.setTranslationY((float) (-bottomMargin));
            }
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this.mLayoutListener);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(KEY_NAVIGATION_BAR_STATUS), false, this.mContentObserver);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this.mLayoutListener);
        this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        updateSplitViewPositon();
    }

    private void setupStatusBarView(Context context) {
        this.mStatusBarView = new StatusBarBlurView(context);
        addView(this.mStatusBarView, new LayoutParams(-1, getStatusBarHeight()));
    }

    private void setupNavigationBarView(Context context) {
        this.mNavigationBarView = new NavigationBarBlurView(context);
        LayoutParams params = new LayoutParams(-1, getNavigationHeight());
        params.addRule(12, -1);
        addView(this.mNavigationBarView, params);
    }

    private boolean isNavigationBarExist() {
        return Global.getInt(this.mContext.getContentResolver(), KEY_NAVIGATION_BAR_STATUS, 0) == 0;
    }

    private int getStatusBarHeight() {
        return this.mContext.getResources().getDimensionPixelSize(this.mContext.getResources().getIdentifier("status_bar_height", ResLoaderUtil.DIMEN, "android"));
    }

    private int getNavigationHeight() {
        return this.mContext.getResources().getDimensionPixelSize(this.mContext.getResources().getIdentifier("navigation_bar_height", ResLoaderUtil.DIMEN, "android"));
    }

    private boolean isThemeSupport(Context context) {
        if (context == null) {
            return false;
        }
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{ResLoader.getInstance().getIdentifier(context, "attr", "hwBlurEffectEnable")});
        boolean hwBlurEffectEnable = typedArray.getBoolean(0, false);
        typedArray.recycle();
        return hwBlurEffectEnable;
    }
}
