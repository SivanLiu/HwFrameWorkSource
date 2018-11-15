package com.android.server.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.provider.FrontFingerPrintSettings;

public class FingerprintActionsListener implements PointerEventListener {
    private static final boolean ENABLE_MWSWITCH = true;
    private static final String FRONT_FINGERPRINT_SWAP_KEY_POSITION = "swap_key_position";
    private static final String GSETTINGS_VDRIVE_IS_RUN = "vdrive_is_run_state";
    static final int HIT_REGION_SCALE = 4;
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final int MSG_CLOSE_SEARCH_PANEL = 1;
    private static final String TAG = "FingerprintActionsListener";
    private static final String TALKBACK_COMPONENT_NAME = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final int VDRIVE_IS_RUN = 1;
    private static final int VDRIVE_IS_UNRUN = 0;
    private static final boolean mDisableMultiWin = SystemProperties.getBoolean("ro.huawei.disable_multiwindow", false);
    private boolean isCoordinateForPad = SystemProperties.getBoolean("ro.config.coordinateforpad", false);
    private Context mContext = null;
    private boolean mDeviceProvisioned = true;
    private ContentObserver mDisplayDensityObserver;
    private boolean mDriveState = false;
    private boolean mGestureNavEnabled;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                FingerprintActionsListener.this.hideSearchPanelView();
            }
        }
    };
    private boolean mIsDoubleFlinger = false;
    private boolean mIsNeedHideMultiWindowView = false;
    private boolean mIsSingleFlinger = false;
    private boolean mIsStatusBarExplaned = false;
    private boolean mIsValidGesture = false;
    private boolean mIsValidHiboardGesture = false;
    private boolean mIsValidLazyModeGesture = false;
    private HwSplitScreenArrowView mLandMultiWinArrowView = null;
    private HwSplitScreenArrowView mMultiWinArrowView = null;
    private PhoneWindowManager mPolicy = null;
    private HwSplitScreenArrowView mPortMultiWinArrowView = null;
    private SearchPanelView mSearchPanelView = null;
    private SettingsObserver mSettingsObserver;
    private SlideTouchEvent mSlideTouchEvent;
    private boolean mTalkBackOn;
    private int mTrikeyNaviMode = -1;
    private WindowManager mWindowManager;
    private Point realSize = new Point();

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
            updateCurrentSettigns();
        }

        public void registerContentObserver(int userId) {
            FingerprintActionsListener.this.mContext.getContentResolver().registerContentObserver(System.getUriFor("swap_key_position"), false, this, userId);
            FingerprintActionsListener.this.mContext.getContentResolver().registerContentObserver(System.getUriFor("device_provisioned"), false, this, userId);
            FingerprintActionsListener.this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION), false, this, userId);
            FingerprintActionsListener.this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("accessibility_enabled"), false, this, userId);
            FingerprintActionsListener.this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("enabled_accessibility_services"), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            updateCurrentSettigns();
        }

        private void updateCurrentSettigns() {
            boolean z = true;
            FingerprintActionsListener.this.mDeviceProvisioned = Secure.getIntForUser(FingerprintActionsListener.this.mContext.getContentResolver(), "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            FingerprintActionsListener.this.mTrikeyNaviMode = System.getIntForUser(FingerprintActionsListener.this.mContext.getContentResolver(), "swap_key_position", FrontFingerPrintSettings.getDefaultNaviMode(), ActivityManager.getCurrentUser());
            FingerprintActionsListener.this.mGestureNavEnabled = GestureNavConst.isGestureNavEnabled(FingerprintActionsListener.this.mContext, -2);
            boolean accessibilityEnabled = Secure.getIntForUser(FingerprintActionsListener.this.mContext.getContentResolver(), "accessibility_enabled", 0, -2) == 1;
            String enabledSerices = Secure.getStringForUser(FingerprintActionsListener.this.mContext.getContentResolver(), "enabled_accessibility_services", -2);
            boolean isContainsTalkBackService = enabledSerices != null && enabledSerices.contains(FingerprintActionsListener.TALKBACK_COMPONENT_NAME);
            String str = FingerprintActionsListener.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("accessibilityEnabled:");
            stringBuilder.append(accessibilityEnabled);
            stringBuilder.append(",isContainsTalkBackService:");
            stringBuilder.append(isContainsTalkBackService);
            stringBuilder.append(",mGestureNavEnabled:");
            stringBuilder.append(FingerprintActionsListener.this.mGestureNavEnabled);
            Log.i(str, stringBuilder.toString());
            FingerprintActionsListener fingerprintActionsListener = FingerprintActionsListener.this;
            if (!(accessibilityEnabled && isContainsTalkBackService)) {
                z = false;
            }
            fingerprintActionsListener.mTalkBackOn = z;
        }
    }

    private class StatusBarStatesChangedReceiver extends BroadcastReceiver {
        private StatusBarStatesChangedReceiver() {
        }

        /* synthetic */ StatusBarStatesChangedReceiver(FingerprintActionsListener x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && "com.android.systemui.statusbar.visible.change".equals(intent.getAction())) {
                FingerprintActionsListener.this.mIsStatusBarExplaned = Boolean.valueOf(intent.getExtras().getString("visible")).booleanValue();
                String str = FingerprintActionsListener.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mIsStatusBarExplaned = ");
                stringBuilder.append(FingerprintActionsListener.this.mIsStatusBarExplaned);
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    public class TouchOutsideListener implements OnTouchListener {
        private int mMsg;

        public TouchOutsideListener(int msg, SearchPanelView panel) {
            this.mMsg = msg;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            int action = ev.getAction();
            if (action != 4 && action != 0) {
                return false;
            }
            FingerprintActionsListener.this.mHandler.removeMessages(this.mMsg);
            FingerprintActionsListener.this.mHandler.sendEmptyMessage(this.mMsg);
            return true;
        }
    }

    public FingerprintActionsListener(Context context, PhoneWindowManager policy) {
        this.mContext = context;
        this.mPolicy = policy;
        updateRealSize();
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mSlideTouchEvent = new SlideTouchEvent(context);
        initialDensityObserver(this.mHandler);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        initView();
        initStatusBarReciver();
        initDriveStateReciver();
    }

    private void initialDensityObserver(Handler handler) {
        this.mDisplayDensityObserver = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                Log.i(FingerprintActionsListener.TAG, "Density has been changed");
                FingerprintActionsListener.this.initView();
                FingerprintActionsListener.this.createSearchPanelView();
                FingerprintActionsListener.this.createMultiWinArrowView();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("display_density_forced"), false, this.mDisplayDensityObserver, UserHandle.myUserId());
    }

    private void initDriveStateReciver() {
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(GSETTINGS_VDRIVE_IS_RUN), false, new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                int state = Global.getInt(FingerprintActionsListener.this.mContext.getContentResolver(), FingerprintActionsListener.GSETTINGS_VDRIVE_IS_RUN, 0);
                String str = FingerprintActionsListener.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mVDriveStateObserver onChange selfChange = ");
                stringBuilder.append(selfChange);
                stringBuilder.append("   state = ");
                stringBuilder.append(state);
                Log.i(str, stringBuilder.toString());
                if (1 == state) {
                    FingerprintActionsListener.this.mDriveState = true;
                } else {
                    FingerprintActionsListener.this.mDriveState = false;
                }
            }
        });
    }

    private void initStatusBarReciver() {
        if (this.mContext != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.systemui.statusbar.visible.change");
            this.mContext.registerReceiver(new StatusBarStatesChangedReceiver(this, null), filter, "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM", null);
            Log.i(TAG, "initStatusBarReciver completed");
        }
    }

    private void initView() {
        Point screenDims = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(screenDims);
        this.mSearchPanelView = (SearchPanelView) LayoutInflater.from(this.mContext).inflate(34013255, null);
        this.mPortMultiWinArrowView = (HwSplitScreenArrowView) LayoutInflater.from(this.mContext).inflate(34013263, null);
        if (this.mPortMultiWinArrowView != null) {
            this.mPortMultiWinArrowView.initViewParams(1, screenDims);
        }
        this.mLandMultiWinArrowView = (HwSplitScreenArrowView) LayoutInflater.from(this.mContext).inflate(34013264, null);
        if (this.mLandMultiWinArrowView != null) {
            this.mLandMultiWinArrowView.initViewParams(2, new Point(screenDims.y, screenDims.x));
        }
    }

    public void createSearchPanelView() {
        if (this.mSearchPanelView != null) {
            this.mSearchPanelView.setOnTouchListener(new TouchOutsideListener(1, this.mSearchPanelView));
            this.mSearchPanelView.setVisibility(8);
            addWindowView(this.mWindowManager, this.mSearchPanelView, getSearchLayoutParams(this.mSearchPanelView.getLayoutParams()));
            this.mSearchPanelView.initUI(this.mHandler.getLooper());
        }
    }

    public void destroySearchPanelView() {
        if (this.mSearchPanelView != null) {
            removeWindowView(this.mWindowManager, this.mSearchPanelView, true);
        }
    }

    public void createMultiWinArrowView() {
        if (ActivityManager.supportsMultiWindow(this.mContext)) {
            if (this.mMultiWinArrowView != null) {
                this.mMultiWinArrowView.removeViewToWindow();
            }
            if (1 == this.mContext.getResources().getConfiguration().orientation) {
                this.mMultiWinArrowView = this.mPortMultiWinArrowView;
            } else {
                this.mMultiWinArrowView = this.mLandMultiWinArrowView;
            }
            this.mMultiWinArrowView.addViewToWindow();
        }
    }

    public void destroyMultiWinArrowView() {
        if (ActivityManager.supportsMultiWindow(this.mContext) && this.mMultiWinArrowView != null) {
            this.mMultiWinArrowView.removeViewToWindow();
        }
    }

    private void hideSearchPanelView() {
        try {
            if (this.mSearchPanelView != null) {
                this.mSearchPanelView.hideSearchPanelView();
            }
        } catch (Exception exp) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hideSearchPanelView");
            stringBuilder.append(exp.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private boolean isSuperPowerSaveMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    public void setCurrentUser(int newUserId) {
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
        if (this.mSlideTouchEvent != null) {
            this.mSlideTouchEvent.updateSettings();
        }
    }

    protected LayoutParams getSearchLayoutParams(ViewGroup.LayoutParams layoutParams) {
        LayoutParams lp = new LayoutParams(-1, -1, HwArbitrationDEFS.MSG_VPN_STATE_OPEN, 8519936, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.setTitle("Framework_SearchPanel");
        lp.windowAnimations = 16974590;
        lp.softInputMode = 49;
        return lp;
    }

    public void addWindowView(WindowManager mWindowManager, View view, LayoutParams params) {
        try {
            mWindowManager.addView(view, params);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("the exception happen in addWindowView, e=");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public void removeWindowView(WindowManager mWindowManager, View view, boolean immediate) {
        if (view == null) {
            return;
        }
        if (immediate) {
            try {
                mWindowManager.removeViewImmediate(view);
                return;
            } catch (IllegalArgumentException e) {
                return;
            } catch (Exception e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("the exception happen in removeWindowView, e=");
                stringBuilder.append(e2.getMessage());
                Log.e(str, stringBuilder.toString());
                return;
            }
        }
        mWindowManager.removeView(view);
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (this.mDeviceProvisioned) {
            if (motionEvent.getPointerCount() == 1) {
                this.mIsSingleFlinger = true;
                this.mIsDoubleFlinger = false;
            } else if (motionEvent.getPointerCount() == 2) {
                this.mIsDoubleFlinger = true;
                this.mIsSingleFlinger = false;
                this.mIsValidLazyModeGesture = false;
                this.mIsValidHiboardGesture = false;
            } else {
                this.mIsDoubleFlinger = false;
                this.mIsSingleFlinger = false;
                this.mIsNeedHideMultiWindowView = true;
                if (this.mMultiWinArrowView != null && this.mMultiWinArrowView.getVisibility() == 0) {
                    this.mMultiWinArrowView.setVisibility(8);
                }
            }
            if (this.mIsSingleFlinger && !this.mGestureNavEnabled) {
                if (motionEvent.getActionMasked() == 0) {
                    Log.d(TAG, "touchDownIsValid MotionEvent.ACTION_DOWN ");
                    touchDownIsValidLazyMode(motionEvent.getRawX(), motionEvent.getRawY());
                }
                if (this.mIsValidLazyModeGesture) {
                    this.mSlideTouchEvent.handleTouchEvent(motionEvent);
                }
                if (!(!this.mIsValidHiboardGesture || isSuperPowerSaveMode() || isInLockTaskMode() || this.mIsStatusBarExplaned || this.mDriveState)) {
                    this.mSearchPanelView.handleGesture(motionEvent);
                }
            }
            if (!(this.mIsValidHiboardGesture || motionEvent.getActionMasked() != 1 || this.mSearchPanelView == null)) {
                this.mSearchPanelView.hideSearchPanelView();
            }
            if (!(!this.mIsDoubleFlinger || this.mIsNeedHideMultiWindowView || this.mTalkBackOn)) {
                if (motionEvent.getActionMasked() == 5) {
                    Log.d(TAG, "touchDownIsValidMultiWin MotionEvent.ACTION_DOWN ");
                    this.mIsValidGesture = touchDownIsValidMultiWin(motionEvent);
                }
                if (this.mIsValidGesture && this.mMultiWinArrowView != null) {
                    this.mMultiWinArrowView.handleSplitScreenGesture(motionEvent);
                    if (this.mSearchPanelView != null) {
                        this.mSearchPanelView.hideSearchPanelView();
                    }
                }
            }
            if (motionEvent.getActionMasked() == 1) {
                reset();
            }
            if (motionEvent.getActionMasked() == 6) {
                this.mIsNeedHideMultiWindowView = true;
            }
        }
    }

    private void reset() {
        this.mIsValidGesture = false;
        this.mIsValidLazyModeGesture = false;
        this.mIsValidHiboardGesture = false;
        this.mIsNeedHideMultiWindowView = false;
    }

    private boolean isNaviBarEnable() {
        return FrontFingerPrintSettings.isNaviBarEnabled(this.mContext.getContentResolver());
    }

    private boolean canAssistEnable() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean isNaviBarEnabled = FrontFingerPrintSettings.isNaviBarEnabled(resolver);
        boolean isSingleNavBarAIEnable = FrontFingerPrintSettings.isSingleNavBarAIEnable(resolver);
        boolean isSingleVirtualNavbarEnable = FrontFingerPrintSettings.isSingleVirtualNavbarEnable(resolver);
        boolean isNavShown = Global.getInt(resolver, "navigationbar_is_min", 0) == 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("canAssistEnable():isNaviBarEnabled(resolver)=");
        stringBuilder.append(isNaviBarEnabled);
        stringBuilder.append(";---isSingleNavBarAIEnable(resolver)");
        stringBuilder.append(isSingleNavBarAIEnable);
        stringBuilder.append(";isNavShown=");
        stringBuilder.append(isNavShown);
        Log.i(str, stringBuilder.toString());
        if (!isNaviBarEnabled || (isSingleVirtualNavbarEnable && !isSingleNavBarAIEnable && isNavShown)) {
            return true;
        }
        return false;
    }

    private void touchDownIsValidLazyMode(float pointX, float pointY) {
        if (this.mPolicy.mDisplay == null || (this.mPolicy.mKeyguardDelegate.isShowing() && !this.mPolicy.mKeyguardDelegate.isOccluded())) {
            this.mIsValidLazyModeGesture = false;
            this.mIsValidHiboardGesture = false;
            return;
        }
        int HIT_REGION_TO_MAX_LAZYMODE = this.mContext.getResources().getDimensionPixelSize(17105186);
        int HIT_REGION_TO_MAX_HIBOARD = (int) (((double) this.mContext.getResources().getDimensionPixelSize(17105186)) / 4.0d);
        updateRealSize();
        if (this.isCoordinateForPad) {
            HIT_REGION_TO_MAX_HIBOARD *= 2;
        }
        boolean z = true;
        boolean z2;
        if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
            z2 = true;
        } else {
            z2 = false;
        }
        int i = this.mPolicy.mNavigationBarPosition;
        PhoneWindowManager phoneWindowManager = this.mPolicy;
        int invalidPointX;
        if (i == 4) {
            boolean isDistanceValid = pointY > ((float) (this.realSize.y - HIT_REGION_TO_MAX_LAZYMODE)) && (pointX < ((float) HIT_REGION_TO_MAX_LAZYMODE) || pointX > ((float) (this.realSize.x - HIT_REGION_TO_MAX_LAZYMODE)));
            boolean z3 = isDistanceValid && canAssistEnable();
            this.mIsValidLazyModeGesture = z3;
            invalidPointX = this.realSize.x / 2;
            int invalidPointX2 = this.realSize.y;
            if (pointY <= ((float) (this.realSize.y - HIT_REGION_TO_MAX_HIBOARD)) || pointX == ((float) invalidPointX) || pointY == ((float) invalidPointX2) || !canAssistEnable() || (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 0 && (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1 || this.mTrikeyNaviMode >= 0))) {
                z = false;
            }
            this.mIsValidHiboardGesture = z;
        } else {
            int invalidPointY = this.realSize.y / 2;
            invalidPointX = this.realSize.x;
            this.mIsValidLazyModeGesture = false;
            if (pointX <= ((float) (this.realSize.x - HIT_REGION_TO_MAX_HIBOARD)) || pointX == ((float) invalidPointX) || pointY == ((float) invalidPointY) || !canAssistEnable() || (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 0 && (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1 || this.mTrikeyNaviMode >= 0))) {
                z = false;
            }
            this.mIsValidHiboardGesture = z;
        }
        if (this.mPolicy.isKeyguardLocked()) {
            this.mIsValidLazyModeGesture = false;
            this.mIsValidHiboardGesture = false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("touchDownIsValidLazyMode = ");
        stringBuilder.append(this.mIsValidLazyModeGesture);
        stringBuilder.append("  touchDownIsValidHiBoard = ");
        stringBuilder.append(this.mIsValidHiboardGesture);
        Log.d(str, stringBuilder.toString());
    }

    private void updateRealSize() {
        if (this.mPolicy.mDisplay != null) {
            this.mPolicy.mDisplay.getRealSize(this.realSize);
        }
    }

    private boolean touchDownIsValidMultiWin(MotionEvent event) {
        boolean z = false;
        if (isNaviBarEnable() || this.mPolicy.mDisplay == null || event.getPointerCount() != 2 || ((this.mPolicy.mKeyguardDelegate.isShowing() && !this.mPolicy.mKeyguardDelegate.isOccluded()) || isSuperPowerSaveMode() || mDisableMultiWin)) {
            return false;
        }
        boolean ret;
        float pointX0 = event.getX(0);
        float pointY0 = event.getY(0);
        float pointX1 = event.getX(1);
        float pointY1 = event.getY(1);
        int navigation_bar_height = (int) (((double) this.mContext.getResources().getDimensionPixelSize(17105186)) / 4.0d);
        if (this.isCoordinateForPad) {
            navigation_bar_height *= 2;
        }
        updateRealSize();
        int i = this.mPolicy.mNavigationBarPosition;
        PhoneWindowManager phoneWindowManager = this.mPolicy;
        if (i == 4) {
            if (pointY0 > ((float) (this.realSize.y - navigation_bar_height)) && pointY1 > ((float) (this.realSize.y - navigation_bar_height))) {
                z = true;
            }
            ret = z;
        } else {
            if (pointX0 > ((float) (this.realSize.x - navigation_bar_height)) && pointX1 > ((float) (this.realSize.x - navigation_bar_height))) {
                z = true;
            }
            ret = z;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("touchDownIsValidMultiWin ret = ");
        stringBuilder.append(ret);
        Log.d(str, stringBuilder.toString());
        return ret;
    }

    private boolean isInLockTaskMode() {
        return ((ActivityManager) this.mContext.getSystemService("activity")).isInLockTaskMode();
    }
}
