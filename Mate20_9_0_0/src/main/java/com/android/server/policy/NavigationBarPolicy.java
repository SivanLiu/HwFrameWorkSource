package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hdm.HwDeviceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.HwSlog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.vkey.SettingsHelper;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.statistical.StatisticalUtils;
import huawei.android.os.HwGeneralManager;
import huawei.android.provider.FrontFingerPrintSettings;

public class NavigationBarPolicy implements OnGestureListener {
    static final boolean DEBUG = false;
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    static final int HIT_REGION_SCALE = 4;
    static int HIT_REGION_TO_MAX = 20;
    static int HIT_REGION_TO_TOP_BOTTOM = 130;
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final String TAG = "NavigationBarPolicy";
    private boolean IS_SUPPORT_PRESSURE;
    private Context mContext = null;
    private GestureDetector mDetector = null;
    boolean mForceMinNavigationBar;
    private int mHoleHeight;
    private boolean mImmersiveMode;
    private boolean mIsValidGesture;
    boolean mMinNavigationBar;
    private PhoneWindowManager mPolicy = null;
    private Point realSize;

    public NavigationBarPolicy(Context context, PhoneWindowManager policy) {
        boolean z = false;
        this.mMinNavigationBar = false;
        this.mForceMinNavigationBar = false;
        this.mIsValidGesture = false;
        this.IS_SUPPORT_PRESSURE = false;
        this.mImmersiveMode = false;
        this.realSize = new Point();
        this.mHoleHeight = 0;
        this.mContext = context;
        this.mPolicy = policy;
        this.mDetector = new GestureDetector(context, this);
        if (Global.getInt(this.mContext.getContentResolver(), "navigationbar_is_min", 0) != 0) {
            z = true;
        }
        this.mMinNavigationBar = z;
        updateRealSize();
        this.IS_SUPPORT_PRESSURE = HwGeneralManager.getInstance().isSupportForce();
        parseHole();
    }

    public void addPointerEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
    }

    private void reset() {
        this.mIsValidGesture = false;
    }

    public void setImmersiveMode(boolean mode) {
        this.mImmersiveMode = mode;
    }

    private boolean touchDownIsValid(float pointX, float pointY) {
        boolean z = false;
        if (this.mPolicy.mDisplay == null || this.mForceMinNavigationBar || (this.mPolicy.mKeyguardDelegate.isShowing() && !this.mPolicy.mKeyguardDelegate.isOccluded())) {
            return false;
        }
        if (this.IS_SUPPORT_PRESSURE && !IS_CHINA_AREA && !this.mImmersiveMode) {
            return false;
        }
        boolean ret = false;
        HIT_REGION_TO_MAX = (int) (((double) this.mContext.getResources().getDimensionPixelSize(17105186)) / 3.5d);
        if (this.mMinNavigationBar) {
            updateRealSize();
            int i = this.mPolicy.mNavigationBarPosition;
            PhoneWindowManager phoneWindowManager = this.mPolicy;
            if (i == 4) {
                if (pointY > ((float) (this.realSize.y - HIT_REGION_TO_MAX))) {
                    z = true;
                }
                ret = z;
            } else if (isNaviBarNearByHole()) {
                if (pointX > ((float) ((this.realSize.x - this.mHoleHeight) - HIT_REGION_TO_MAX))) {
                    z = true;
                }
                ret = z;
            } else {
                if (pointX > ((float) (this.realSize.x - HIT_REGION_TO_MAX))) {
                    z = true;
                }
                ret = z;
            }
        }
        return ret;
    }

    private void updateRealSize() {
        if (this.mPolicy.mDisplay != null) {
            this.mPolicy.mDisplay.getRealSize(this.realSize);
        }
    }

    public void updateNavigationBar(boolean minNaviBar) {
        this.mMinNavigationBar = minNaviBar;
        Global.putInt(this.mContext.getContentResolver(), "navigationbar_is_min", minNaviBar);
        this.mPolicy.mWindowManagerFuncs.reevaluateStatusBarSize(true);
    }

    private void sendBroadcast(boolean minNaviBar) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendBroadcast minNaviBar = ");
        stringBuilder.append(minNaviBar);
        HwSlog.d(str, stringBuilder.toString());
        Intent intent = new Intent("com.huawei.navigationbar.statuschange");
        intent.putExtra("minNavigationBar", minNaviBar);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        StatisticalUtils.reportc(this.mContext, 61);
    }

    public boolean onDown(MotionEvent event) {
        this.mIsValidGesture = touchDownIsValid(event.getRawX(), event.getRawY());
        return false;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (isFlingOnFrontNaviMode() && !isInLockTaskMode()) {
            HwSlog.d(TAG, "onFling::FRONT_FINGERPRINT_NAVIGATION, return! ");
            return false;
        } else if (isGestureNavigationEnable()) {
            HwSlog.d(TAG, "onFling::Gest_Navigation_Enable, return! ");
            return false;
        } else if (HwDeviceManager.disallowOp(103)) {
            return false;
        } else {
            boolean z = true;
            if (this.IS_SUPPORT_PRESSURE && IS_CHINA_AREA) {
                boolean ret = false;
                if (this.mMinNavigationBar) {
                    float pointX = e2.getX();
                    float pointY = e2.getY();
                    int i = this.mPolicy.mNavigationBarPosition;
                    PhoneWindowManager phoneWindowManager = this.mPolicy;
                    if (i == 4) {
                        ret = pointY < ((float) (this.realSize.y - HIT_REGION_TO_TOP_BOTTOM));
                    } else if (isNaviBarNearByHole()) {
                        ret = pointX < ((float) ((this.realSize.x - this.mHoleHeight) - HIT_REGION_TO_TOP_BOTTOM));
                    } else {
                        ret = pointX < ((float) (this.realSize.x - HIT_REGION_TO_TOP_BOTTOM));
                    }
                }
                if (!ret) {
                    HwSlog.d(TAG, "onFling::move distance is not enough, return! ");
                    return false;
                }
            }
            if (!this.mIsValidGesture || SettingsHelper.isTouchPlusOn(this.mContext)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onFling::not valid gesture or touch plus on, ");
                stringBuilder.append(this.mIsValidGesture);
                stringBuilder.append(", return!");
                HwSlog.d(str, stringBuilder.toString());
                return false;
            }
            int i2 = this.mPolicy.mNavigationBarPosition;
            PhoneWindowManager phoneWindowManager2 = this.mPolicy;
            if (i2 == 4) {
                if (e1.getRawY() >= e2.getRawY()) {
                    z = false;
                }
                sendBroadcast(z);
            } else {
                if (e1.getRawX() >= e2.getRawX()) {
                    z = false;
                }
                sendBroadcast(z);
            }
            reset();
            return false;
        }
    }

    private boolean isFlingOnFrontNaviMode() {
        return "normal".equals(SystemProperties.get("ro.runmode", "normal")) && FRONT_FINGERPRINT_NAVIGATION && !isNaviBarEnabled();
    }

    private boolean isNaviBarEnabled() {
        if (this.mContext == null) {
            return true;
        }
        boolean isNaviBarEnable = FrontFingerPrintSettings.isNaviBarEnabled(this.mContext.getContentResolver());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNaviBarEnable is: ");
        stringBuilder.append(isNaviBarEnable);
        Log.d(str, stringBuilder.toString());
        return isNaviBarEnable;
    }

    private boolean isGestureNavigationEnable() {
        if (this.mContext != null) {
            return FrontFingerPrintSettings.isGestureNavigationMode(this.mContext.getContentResolver());
        }
        return false;
    }

    private boolean isInLockTaskMode() {
        ActivityManager mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0 && mActivityManager.isInLockTaskMode()) {
            return true;
        }
        return false;
    }

    public void onLongPress(MotionEvent arg0) {
    }

    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    public void onShowPress(MotionEvent arg0) {
    }

    public boolean onSingleTapUp(MotionEvent arg0) {
        reset();
        return false;
    }

    private void parseHole() {
        String[] props = SystemProperties.get("ro.config.hw_notch_size", "").split(",");
        if (props != null && props.length == 4) {
            this.mHoleHeight = Integer.parseInt(props[1]);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mHoleHeight = ");
            stringBuilder.append(this.mHoleHeight);
            Log.d(str, stringBuilder.toString());
        }
    }

    private boolean isNaviBarNearByHole() {
        if (this.mHoleHeight <= 0 || this.mPolicy.mScreenRotation != 3) {
            return false;
        }
        return true;
    }
}
