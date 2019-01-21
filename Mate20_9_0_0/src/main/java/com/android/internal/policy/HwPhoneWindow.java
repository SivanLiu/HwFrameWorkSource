package com.android.internal.policy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.hwcontrol.HwWidgetFactory;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.IWindowManager;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewRootImpl.ActivityConfigCallback;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import androidhwext.R;
import com.android.internal.widget.ActionBarOverlayLayout;
import com.android.internal.widget.FloatingToolbar;
import com.android.internal.widget.HwDecorCaptionView;
import com.huawei.hsm.permission.StubController;
import huawei.android.utils.HwRTBlurUtils;
import huawei.android.utils.HwRTBlurUtils.BlurParams;

public class HwPhoneWindow extends PhoneWindow {
    private static final int FLOATING_MASK = Integer.MIN_VALUE;
    protected static final boolean IS_NOTCH_PROP = (SystemProperties.get("ro.config.hw_notch_size", "").equals("") ^ 1);
    static final String TAG = "HwPhoneWindow";
    public boolean mForceDisableBlurBehind = false;
    private boolean mHwDrawerFeature;
    private boolean mHwFloating;
    private int mHwOverlayActionBar;
    private boolean mIsTranslucentImmersion;
    private boolean mSplitMode;
    private IBinder mToken;

    public static class Utils {
        private static final boolean mIsChina = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", ""));
        private static boolean mIsLastDeviceProvisioned = false;

        public static void setLastDeviceProvisioned(boolean lastDeviceProvisioned) {
            mIsLastDeviceProvisioned = lastDeviceProvisioned;
        }

        public static boolean getLastDeviceProvisioned() {
            return mIsLastDeviceProvisioned;
        }
    }

    public HwPhoneWindow(Context context) {
        super(context);
    }

    public HwPhoneWindow(Context context, Window preservedWindow, ActivityConfigCallback activityConfigCallback) {
        super(context, preservedWindow, activityConfigCallback);
        try {
            if (context.getDisplay() != null && HwPCUtils.isValidExtDisplayId(context.getDisplay().getDisplayId())) {
                this.mUseDecorContext = false;
            }
        } catch (NoSuchMethodError e) {
            Log.e(TAG, "HwPhoneWindow isValidExtDisplayId NoSuchMethodError");
        } catch (Exception e2) {
            Log.e(TAG, "HwPhoneWindow isValidExtDisplayId error");
        }
    }

    protected boolean isEmuiStyle() {
        return HwWidgetFactory.isHwTheme(getContext());
    }

    protected int getHeightMeasureSpec(int fixh, int heightSize, int defaultHeightMeasureSpec) {
        if (!isEmuiStyle()) {
            return super.getHeightMeasureSpec(fixh, heightSize, defaultHeightMeasureSpec);
        }
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        if (metrics.widthPixels > metrics.heightPixels) {
        }
        if (fixh < metrics.heightPixels) {
            fixh -= (int) (((float) getContext().getResources().getDimensionPixelSize(17105318)) * ((((float) fixh) + 0.0f) / ((float) metrics.heightPixels)));
        }
        return MeasureSpec.makeMeasureSpec(fixh < heightSize ? fixh : heightSize, Integer.MIN_VALUE);
    }

    protected int getEmuiActionBarLayout(int layoutResource) {
        if (isEmuiStyle()) {
            return 34013272;
        }
        return super.getEmuiActionBarLayout(layoutResource);
    }

    protected void setEmuiActionModeBar(ViewStub viewStub) {
        if (viewStub != null && isEmuiStyle()) {
            viewStub.setLayoutResource(34013271);
        }
    }

    protected boolean CheckPermanentMenuKey() {
        if (isEmuiStyle()) {
            return true;
        }
        return super.CheckPermanentMenuKey();
    }

    protected void updateLayoutParamsColor() {
        LayoutParams attrs = getAttributes();
        boolean changed = false;
        int statusBarColor = getStatusBarColor();
        if (attrs.statusBarColor != statusBarColor) {
            changed = true;
            attrs.statusBarColor = statusBarColor;
        }
        int navigationBarColor = getNavigationBarColor();
        if (attrs.navigationBarColor != navigationBarColor) {
            changed = true;
            attrs.navigationBarColor = navigationBarColor;
        }
        int emuiValue = -1;
        boolean isEmuiLight = false;
        if ((attrs.privateFlags & StubController.PERMISSION_DELETE_CALENDAR) == 0) {
            boolean emuiValue2 = isEmuiStyle();
            if (HwWidgetFactory.isHwLightTheme(getContext())) {
                isEmuiLight = true;
            }
            emuiValue = getEmuiValue(emuiValue2);
        }
        if (attrs.isEmuiStyle != emuiValue) {
            changed = true;
            attrs.isEmuiStyle = emuiValue;
        }
        if (isEmuiLight != ((attrs.hwFlags & 16) != 0)) {
            changed = true;
            if (isEmuiLight) {
                attrs.hwFlags |= 16;
            } else {
                attrs.hwFlags &= -17;
            }
        }
        if (changed) {
            dispatchWindowAttributesChanged(attrs);
        }
    }

    private int getEmuiValue(int emuiValue) {
        if (this.mIsFloating || this.mHwFloating || (emuiValue != 0 && windowIsTranslucent() && !isTranslucentImmersion())) {
            return emuiValue | Integer.MIN_VALUE;
        }
        return emuiValue;
    }

    public void setHwFloating(boolean isFloating) {
        if (this.mHwFloating != isFloating) {
            this.mHwFloating = isFloating;
            updateLayoutParamsColor();
        }
    }

    public boolean getHwFloating() {
        return this.mHwFloating;
    }

    protected boolean getTryForcedCloseAnimation(IWindowManager wm, boolean animate, Object tag) {
        if (animate && "TryForcedCloseAnimation".equals(tag)) {
            return true;
        }
        return false;
    }

    public int getStatusBarColor() {
        if (!HwWidgetFactory.isHwLightTheme(getContext()) || getIsForcedStatusBarColor()) {
            return super.getStatusBarColor();
        }
        return HwWidgetFactory.getPrimaryColor(getContext());
    }

    protected FloatingToolbar getFloatingToolbar(Window window) {
        return new FloatingToolbar(window, true);
    }

    protected void initTranslucentImmersion() {
        Context context = getContext();
        if (context != null) {
            TypedValue tv = new TypedValue();
            boolean z = false;
            context.getTheme().resolveAttribute(33620019, tv, false);
            if (tv.type == 18 && tv.data == -1) {
                z = true;
            }
            this.mIsTranslucentImmersion = z;
        }
    }

    protected void initSplitMode() {
        Context c = getContext();
        if (c != null && (c instanceof Activity)) {
            Intent intent = ((Activity) c).getIntent();
            if (intent != null) {
                this.mSplitMode = (intent.getHwFlags() & 4) != 0;
            }
        }
    }

    protected boolean isSplitMode() {
        return this.mSplitMode;
    }

    protected boolean isTranslucentImmersion() {
        return this.mIsTranslucentImmersion;
    }

    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        updateBlurStatus();
        initChildWindowIgnoreParentWindowClipRect();
    }

    public void setContentView(View view) {
        super.setContentView(view);
        updateBlurStatus();
        initChildWindowIgnoreParentWindowClipRect();
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        updateBlurStatus();
        initChildWindowIgnoreParentWindowClipRect();
    }

    public void forceDisableBlurBehind() {
        this.mForceDisableBlurBehind = true;
    }

    private void updateBlurStatus() {
        LayoutParams lp = getAttributes();
        BlurParams blurParams = HwRTBlurUtils.obtainBlurStyle(getContext(), null, 33619992, 0, TAG);
        if (lp != null) {
            try {
                if (getContext() != null && HwPCUtils.isValidExtDisplayId(getContext()) && getContext().getPackageName().equals("com.android.packageinstaller")) {
                    lp.flags &= -5;
                    return;
                }
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "HwPhoneWindow isValidExtDisplayId NoSuchMethodError");
            }
        }
        if (this.mForceDisableBlurBehind) {
            blurParams.enable = false;
        }
        HwRTBlurUtils.updateWindowBgForBlur(blurParams, getDecorView());
        HwRTBlurUtils.updateBlurStatus(getAttributes(), blurParams);
    }

    private void initChildWindowIgnoreParentWindowClipRect() {
        TypedArray ahwext = getContext().obtainStyledAttributes(null, R.styleable.Window, 0, 0);
        boolean ignore = ahwext.getBoolean(0, false);
        ahwext.recycle();
        LayoutParams lp = getAttributes();
        if (ignore) {
            lp.privateFlags |= StubController.PERMISSION_ACCESS_BROWSER_RECORDS;
        } else {
            lp.privateFlags &= -1073741825;
        }
    }

    public void setHwDrawerFeature(boolean using, int overlayActionBar) {
        this.mHwDrawerFeature = using;
        if (using) {
            this.mHwOverlayActionBar = overlayActionBar;
        } else {
            this.mHwOverlayActionBar = 0;
        }
        initHwDrawerFeature();
    }

    protected void initHwDrawerFeature() {
        if (this.mDecorContentParent instanceof ActionBarOverlayLayout) {
            this.mDecorContentParent.setHwDrawerFeature(this.mHwDrawerFeature, this.mHwOverlayActionBar);
        }
    }

    public void setDrawerOpend(boolean open) {
        if (this.mDecorContentParent != null && (this.mDecorContentParent instanceof ActionBarOverlayLayout)) {
            ((ActionBarOverlayLayout) this.mDecorContentParent).setDrawerOpend(open);
        }
    }

    public void onWindowStateChanged(int windowState) {
        if (this.mDecor != null && this.mDecor.mDecorCaptionView != null && (this.mDecor.mDecorCaptionView instanceof HwDecorCaptionView)) {
            ((HwDecorCaptionView) this.mDecor.mDecorCaptionView).onWindowStateChanged(windowState);
        }
    }

    public void setAppToken(IBinder token) {
        this.mToken = token;
    }

    public IBinder getAppToken() {
        return this.mToken;
    }

    public void setTitle(CharSequence title, boolean updateAccessibilityTitle) {
        super.setTitle(title, updateAccessibilityTitle);
        if (this.mDecor != null && this.mDecor.mDecorCaptionView != null && (this.mDecor.mDecorCaptionView instanceof HwDecorCaptionView)) {
            ((HwDecorCaptionView) this.mDecor.mDecorCaptionView).setTitle(title);
        }
    }

    protected void setHwFlagForNotch(boolean isTranslucent) {
        if (IS_NOTCH_PROP && isTranslucent) {
            LayoutParams attrs = getAttributes();
            attrs.hwFlags = (attrs.hwFlags & -32769) | StubController.PERMISSION_CALLLOG_WRITE;
        }
    }

    protected void setHwFlagForInvisibleWindowDetection(boolean isTranslucent, boolean noActionBar) {
        LayoutParams attrs = getAttributes();
        if (isTranslucent) {
            attrs.hwFlags = (attrs.hwFlags & -268435457) | 268435456;
        }
        if (noActionBar) {
            attrs.hwFlags = (attrs.hwFlags & -536870913) | StubController.PERMISSION_DELETE_CALENDAR;
        }
    }

    private boolean isDeviceProvisioned() {
        return Global.getInt(getContext().getContentResolver(), "device_provisioned", 0) != 0;
    }

    public boolean isNavigationBarSetWhite() {
        if (Utils.mIsChina || Utils.getLastDeviceProvisioned()) {
            return false;
        }
        Utils.setLastDeviceProvisioned(isDeviceProvisioned());
        return Utils.getLastDeviceProvisioned() ^ 1;
    }
}
