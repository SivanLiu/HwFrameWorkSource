package com.android.server.input;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.KeyguardManager;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Flog;
import android.util.Log;
import android.view.IDockedStackListener;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.hiai.awareness.AwarenessConstants;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.List;

public class FrontFingerprintNavigation {
    private static final String FINGER_PRINT_ACTION_KEYEVENT = "com.android.huawei.FINGER_PRINT_ACTION_KEYEVENT";
    private static final String FP_KEYGUARD_ENABLE = "fp_keyguard_enable";
    public static final String FRONT_FINGERPRINT_BUTTON_LIGHT_MODE = "button_light_mode";
    public static final int FRONT_FINGERPRINT_KEYCODE_HOME_UP = 515;
    public static final String FRONT_FINGERPRINT_SWAP_KEY_POSITION = "swap_key_position";
    private static final String GESTURE_NAV_TRIKEY_SETTINGS = "secure_gesture_navigation";
    public static final String HAPTIC_FEEDBACK_TRIKEY_SETTINGS = "physic_navi_haptic_feedback_enabled";
    private static final int INIT_FINGER_PRINT_ID = -1;
    public static final String INTENT_KEY = "keycode";
    private static final int INVALID_NAVIMODE = -10000;
    private static final boolean IS_SUPPORT_GESTURE_NAV = SystemProperties.getBoolean("ro.config.gesture_front_support", false);
    private static final String LOG_MMI_TESTINT_NOW = "MMITesting now.";
    /* access modifiers changed from: private */
    public static final String TAG = FrontFingerprintNavigation.class.getSimpleName();
    private static final int TRIKEY_NAVI_MODE = -1;
    private static final long VIBRATE_TIME = 60;
    private static final int VIBRATOR_MODE_LONG_PRESS = 16;
    private static final int VIBRATOR_MODE_SHORT_PRESS = 8;
    private boolean isNormalRunmode = "normal".equals(SystemProperties.get("ro.runmode", "normal"));
    /* access modifiers changed from: private */
    public Context mContext;
    DMDUtils mDmdUtils = null;
    private int mFingerPrintId = -1;
    private FingerPrintHomeUpInspector mFingerPrintUpInspector;
    /* access modifiers changed from: private */
    public FingerprintManager mFingerprintManager;
    private final Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsDeviceProvisioned = true;
    /* access modifiers changed from: private */
    public boolean mIsDockedStackMinimized = false;
    /* access modifiers changed from: private */
    public boolean mIsFingerprintRemoveHome = SystemProperties.getBoolean("ro.config.finger_remove_home", false);
    /* access modifiers changed from: private */
    public boolean mIsFpKeyguardEnable = false;
    /* access modifiers changed from: private */
    public boolean mIsGestureNavEnable = false;
    /* access modifiers changed from: private */
    public boolean mIsHapticEnabled = true;
    private boolean mIsPadDevice = false;
    /* access modifiers changed from: private */
    public boolean mIsWakeUpScreen = false;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public final ContentResolver mResolver;
    private SettingsObserver mSettingsObserver;
    private FingerprintNavigationInspector mSystemUiBackInspector;
    private FingerprintNavigationInspector mSystemUiHomeInspector;
    private FingerprintNavigationInspector mSystemUiRecentInspector;
    /* access modifiers changed from: private */
    public int mTrikeyNaviMode = -1;
    /* access modifiers changed from: private */
    public int mVibrateHomeUpMode = 8;
    private Vibrator mVibrator = null;
    /* access modifiers changed from: private */
    public int mVibratorModeLongPressForFrontFp = 16;
    /* access modifiers changed from: private */
    public int mVibratorModeLongPressForHomeFrontFp = 16;
    /* access modifiers changed from: private */
    public int mVibratorModeShortPressForFrontFp = 8;

    public FrontFingerprintNavigation(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSystemUiBackInspector = new SystemUiBackInspector();
        this.mSystemUiRecentInspector = new SystemUiRecentInspector();
        this.mSystemUiHomeInspector = new SystemUiHomeInspector();
        this.mFingerPrintUpInspector = new FingerPrintHomeUpInspector();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mVibrator = (SystemVibrator) ((Vibrator) this.mContext.getSystemService("vibrator"));
        this.mDmdUtils = new DMDUtils(context);
    }

    private void updateDockedStackFlag() {
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new IDockedStackListener.Stub() {
                /* class com.android.server.input.FrontFingerprintNavigation.AnonymousClass1 */

                public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
                }

                public void onDockedStackExistsChanged(boolean exists) throws RemoteException {
                }

                public void onDockedStackMinimizedChanged(boolean minimized, long animDuration, boolean isHomeStackResizable) throws RemoteException {
                    String access$000 = FrontFingerprintNavigation.TAG;
                    Log.d(access$000, "onDockedStackMinimizedChanged:" + minimized);
                    boolean unused = FrontFingerprintNavigation.this.mIsDockedStackMinimized = minimized;
                }

                public void onAdjustedForImeChanged(boolean adjustedForIme, long animDuration) throws RemoteException {
                }

                public void onDockSideChanged(int newDockSide) throws RemoteException {
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Failed registering docked stack exists listener", e);
        }
    }

    public void systemRunning() {
        Log.d(TAG, "systemRunning");
        updateDockedStackFlag();
        initDefaultNaviValue();
        initDefaultVibrateValue();
    }

    private void initDefaultVibrateValue() {
        this.mVibratorModeShortPressForFrontFp = SystemProperties.getInt("ro.config.trikey_vibrate_touch", 8);
        String str = TAG;
        Log.d(str, "The trikey touch vibrate config value is:" + this.mVibratorModeShortPressForFrontFp);
        this.mVibratorModeLongPressForFrontFp = SystemProperties.getInt("ro.config.trikey_vibrate_press", 16);
        this.mVibratorModeLongPressForHomeFrontFp = this.mVibratorModeLongPressForFrontFp;
        String str2 = TAG;
        Log.d(str2, "The trikey longPress vibrate config value is:" + this.mVibratorModeLongPressForFrontFp);
        this.mIsPadDevice = isPad();
        String str3 = TAG;
        Log.d(str3, "mIsPadDevice is:" + this.mIsPadDevice);
        this.mFingerPrintId = SystemProperties.getInt("sys.fingerprint.deviceId", -1);
        this.mVibrateHomeUpMode = SystemProperties.getInt("ro.config.trikey_vibrate_touch_up", this.mVibratorModeShortPressForFrontFp);
        String str4 = TAG;
        Log.d(str4, "mVibrateHomeUpMode is:" + this.mVibrateHomeUpMode);
    }

    private void initDefaultNaviValue() {
        if (this.mResolver != null && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION) {
            String str = TAG;
            Log.d(str, "initDefaultNaviValue with user:" + ActivityManager.getCurrentUser());
            initDefaultHapticProp();
            initDefaultNaviBarStatus();
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                initTrikeyNaviMode();
                if (Settings.System.getIntForUser(this.mResolver, "button_light_mode", INVALID_NAVIMODE, ActivityManager.getCurrentUser()) == INVALID_NAVIMODE) {
                    int buttonLightMode = FrontFingerPrintSettings.getDefaultBtnLightMode();
                    String str2 = TAG;
                    Log.d(str2, "init default buttonlight mode to:" + buttonLightMode);
                    Settings.System.putIntForUser(this.mResolver, "button_light_mode", buttonLightMode, ActivityManager.getCurrentUser());
                }
            }
        }
    }

    private void initTrikeyNaviMode() {
        if (Settings.System.getIntForUser(this.mResolver, "swap_key_position", INVALID_NAVIMODE, ActivityManager.getCurrentUser()) != INVALID_NAVIMODE) {
            Log.d(TAG, "trikeyNaviMode is not INVALID_NAVIMODE!");
            return;
        }
        boolean isDeviceProvisioned = false;
        if (Settings.Secure.getIntForUser(this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0) {
            isDeviceProvisioned = true;
        }
        if (!isDeviceProvisioned) {
            int trikeyNaviMode = FrontFingerPrintSettings.getDefaultNaviMode();
            String str = TAG;
            Log.d(str, "init default trikeyNaviMode to:" + trikeyNaviMode);
            Settings.System.putIntForUser(this.mResolver, "swap_key_position", trikeyNaviMode, ActivityManager.getCurrentUser());
        } else if (FrontFingerPrintSettings.isChinaArea()) {
            Log.d(TAG, "init default trikeyNaviMode to singleButtonMode!");
            Settings.System.putIntForUser(this.mResolver, "swap_key_position", INVALID_NAVIMODE, ActivityManager.getCurrentUser());
        }
    }

    private void initDefaultHapticProp() {
        if ((FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0 || FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) && Settings.System.getIntForUser(this.mResolver, "physic_navi_haptic_feedback_enabled", INVALID_NAVIMODE, ActivityManager.getCurrentUser()) == INVALID_NAVIMODE) {
            Log.d(TAG, "init default hapicProp to enabled!");
            Settings.System.putIntForUser(this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser());
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.input.FrontFingerprintNavigation */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [int, boolean] */
    private void initDefaultNaviBarStatus() {
        int userId = ActivityManager.getCurrentUser();
        if (Settings.System.getIntForUser(this.mResolver, "enable_navbar", INVALID_NAVIMODE, userId) == INVALID_NAVIMODE) {
            ?? isNaviBarEnabled = FrontFingerPrintSettings.isNaviBarEnabled(this.mResolver);
            String str = TAG;
            Log.d(str, "init defaultNaviBarStatus to:" + (isNaviBarEnabled == true ? 1 : 0));
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
                putNaviBarStatusForUser(userId, isNaviBarEnabled);
            } else if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                Settings.System.putIntForUser(this.mResolver, "enable_navbar", isNaviBarEnabled, userId);
            }
        }
    }

    private void putNaviBarStatusForUser(int userId, int naviBarStatus) {
        int status = 0;
        if (!(Settings.Secure.getIntForUser(this.mResolver, "device_provisioned", 0, userId) != 0)) {
            Settings.System.putIntForUser(this.mResolver, "enable_navbar", naviBarStatus, userId);
        } else if (FrontFingerPrintSettings.isChinaArea()) {
            if (FrontFingerPrintSettings.SINGLE_VIRTUAL_NAVIGATION_MODE == 1) {
                status = 1;
            }
            Settings.System.putIntForUser(this.mResolver, "enable_navbar", status, userId);
        } else {
            Settings.System.putIntForUser(this.mResolver, "enable_navbar", 1, userId);
        }
    }

    /* access modifiers changed from: private */
    public boolean isTopTaskRecent() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ActivityManager.getService().getFilteredTasks(1, 0, 2);
            return !tasks.isEmpty() && tasks.get(0).configuration.windowConfiguration.getActivityType() == 3;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void notifyTrikeyEvent(int keyCode) {
        if (this.mIsDeviceProvisioned) {
            int isShowNaviGuide = Settings.System.getIntForUser(this.mResolver, "systemui_tips_already_shown", 0, ActivityManager.getCurrentUser());
            String str = TAG;
            Log.d(str, "isShowNaviGuide:" + isShowNaviGuide);
            if (isShowNaviGuide == 0) {
                Intent intent = new Intent(FINGER_PRINT_ACTION_KEYEVENT);
                intent.putExtra("keycode", keyCode);
                intent.setPackage(FingerViewController.PKGNAME_OF_KEYGUARD);
                intent.addFlags(268435456);
                this.mContext.sendBroadcast(intent, "android.permission.STATUS_BAR");
            }
        }
    }

    public boolean handleFingerprintEvent(InputEvent event) {
        if (!FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION || !this.isNormalRunmode || isGestureNavEnable()) {
            Log.d(TAG, "do not support frontfingerprint");
            return false;
        } else if (this.mSystemUiBackInspector.probe(event)) {
            this.mSystemUiBackInspector.handle(event);
            return true;
        } else if (this.mSystemUiRecentInspector.probe(event)) {
            this.mSystemUiRecentInspector.handle(event);
            return true;
        } else if (this.mSystemUiHomeInspector.probe(event)) {
            this.mSystemUiHomeInspector.handle(event);
            return true;
        } else if (!this.mFingerPrintUpInspector.probe(event)) {
            return false;
        } else {
            this.mFingerPrintUpInspector.handle(event);
            return true;
        }
    }

    /* access modifiers changed from: private */
    public boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return !power.isScreenOn();
        }
        return false;
    }

    private String getTopApp() {
        ActivityInfo topActivity = HwActivityTaskManager.getLastResumedActivity();
        if (topActivity == null) {
            return null;
        }
        String pkgName = topActivity.getComponentName().flattenToShortString();
        String str = TAG;
        Log.d(str, "TopApp is " + pkgName);
        return pkgName;
    }

    private boolean isKeyguardOccluded() {
        HwPhoneWindowManager policy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        return policy != null && policy.isKeyguardOccluded();
    }

    /* access modifiers changed from: private */
    public boolean isSpecialKey(InputEvent event, int code) {
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        KeyEvent ev = (KeyEvent) event;
        String str = TAG;
        Log.d(str, "keycode is : " + ev.getKeyCode());
        if (ev.getKeyCode() == code) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void sendKeyEvent(int keycode) {
        int[] actions;
        for (int i : new int[]{0, 1}) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, i, keycode, 0, 0, this.mFingerPrintId, 0, 8, 257), 0);
        }
    }

    /* access modifiers changed from: private */
    public boolean isSuperPowerSaveMode() {
        return SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false);
    }

    abstract class FingerprintNavigationInspector {
        FingerprintNavigationInspector() {
        }

        public boolean probe(InputEvent event) {
            return false;
        }

        public void handle(InputEvent event) {
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
            boolean z = true;
            boolean unused = FrontFingerprintNavigation.this.mIsDeviceProvisioned = Settings.Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            int unused2 = FrontFingerprintNavigation.this.mTrikeyNaviMode = Settings.System.getIntForUser(FrontFingerprintNavigation.this.mResolver, "swap_key_position", FrontFingerPrintSettings.getDefaultNaviMode(), ActivityManager.getCurrentUser());
            boolean unused3 = FrontFingerprintNavigation.this.mIsFpKeyguardEnable = Settings.Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, FrontFingerprintNavigation.FP_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser()) != 0;
            boolean unused4 = FrontFingerprintNavigation.this.mIsHapticEnabled = Settings.System.getIntForUser(FrontFingerprintNavigation.this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) != 0;
            boolean unused5 = FrontFingerprintNavigation.this.mIsGestureNavEnable = Settings.Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "secure_gesture_navigation", 0, ActivityManager.getCurrentUser()) == 0 ? false : z;
        }

        public void registerContentObserver(int userId) {
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Settings.System.getUriFor("swap_key_position"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Settings.System.getUriFor("device_provisioned"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FrontFingerprintNavigation.FP_KEYGUARD_ENABLE), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Settings.System.getUriFor("physic_navi_haptic_feedback_enabled"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Settings.Secure.getUriFor("secure_gesture_navigation"), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
            boolean z = true;
            boolean unused = frontFingerprintNavigation.mIsDeviceProvisioned = Settings.Secure.getIntForUser(frontFingerprintNavigation.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation frontFingerprintNavigation2 = FrontFingerprintNavigation.this;
            int unused2 = frontFingerprintNavigation2.mTrikeyNaviMode = Settings.System.getIntForUser(frontFingerprintNavigation2.mResolver, "swap_key_position", FrontFingerPrintSettings.getDefaultNaviMode(), ActivityManager.getCurrentUser());
            FrontFingerprintNavigation frontFingerprintNavigation3 = FrontFingerprintNavigation.this;
            boolean unused3 = frontFingerprintNavigation3.mIsFpKeyguardEnable = Settings.Secure.getIntForUser(frontFingerprintNavigation3.mResolver, FrontFingerprintNavigation.FP_KEYGUARD_ENABLE, 0, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation frontFingerprintNavigation4 = FrontFingerprintNavigation.this;
            boolean unused4 = frontFingerprintNavigation4.mIsHapticEnabled = Settings.System.getIntForUser(frontFingerprintNavigation4.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation frontFingerprintNavigation5 = FrontFingerprintNavigation.this;
            if (Settings.Secure.getIntForUser(frontFingerprintNavigation5.mResolver, "secure_gesture_navigation", 0, ActivityManager.getCurrentUser()) == 0) {
                z = false;
            }
            boolean unused5 = frontFingerprintNavigation5.mIsGestureNavEnable = z;
        }
    }

    public void setCurrentUser(int newUserId) {
        String str = TAG;
        Log.d(str, "setCurrentUser:" + newUserId);
        initDefaultValueWithUser();
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
    }

    /* JADX WARN: Type inference failed for: r3v3, types: [int, boolean] */
    private void initDefaultValueWithUser() {
        if (this.mResolver != null && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION) {
            int userId = ActivityManager.getCurrentUser();
            String str = TAG;
            Log.d(str, "initDefaultNaviValue with user:" + userId);
            initDefaultHapticProp();
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                if (Settings.System.getIntForUser(this.mResolver, "swap_key_position", INVALID_NAVIMODE, userId) == INVALID_NAVIMODE) {
                    int trikeyNaviMode = FrontFingerPrintSettings.getDefaultNaviMode();
                    String str2 = TAG;
                    Log.d(str2, "init default trikeyNaviMode to:" + trikeyNaviMode);
                    Settings.System.putIntForUser(this.mResolver, "swap_key_position", trikeyNaviMode, userId);
                }
                if (Settings.System.getIntForUser(this.mResolver, "button_light_mode", INVALID_NAVIMODE, userId) == INVALID_NAVIMODE) {
                    int buttonLightMode = FrontFingerPrintSettings.getDefaultBtnLightMode();
                    String str3 = TAG;
                    Log.d(str3, "init user buttonlight mode to:" + buttonLightMode);
                    Settings.System.putIntForUser(this.mResolver, "button_light_mode", buttonLightMode, userId);
                }
            }
            if (Settings.System.getIntForUser(this.mResolver, "enable_navbar", INVALID_NAVIMODE, userId) == INVALID_NAVIMODE) {
                ?? isNaviBarEnabled = FrontFingerPrintSettings.isNaviBarEnabled(this.mResolver);
                String str4 = TAG;
                Log.d(str4, "init defaultNaviBarStatus to:" + (isNaviBarEnabled == true ? 1 : 0));
                Settings.System.putIntForUser(this.mResolver, "enable_navbar", isNaviBarEnabled, userId);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isSingleTrikeyNaviMode() {
        return this.mTrikeyNaviMode < 0;
    }

    final class SystemUiBackInspector extends FingerprintNavigationInspector {
        SystemUiBackInspector() {
            super();
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (!FrontFingerprintNavigation.this.isSpecialKey(event, WifiProCommonUtils.RESP_CODE_UNSTABLE) || FrontFingerprintNavigation.this.isScreenOff() || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isStrongBox()) {
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "SystemUIBackInspector State ok");
            return true;
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            KeyEvent ev = null;
            if (event instanceof KeyEvent) {
                ev = (KeyEvent) event;
            }
            if (ev != null && FrontFingerprintNavigation.this.mIsDeviceProvisioned && ev.getAction() != 0) {
                Log.i(FrontFingerprintNavigation.TAG, "mSystemUiBackInspector handle sendKeyEvent : 4");
                if (!FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                    handleNaviTrikeySetting();
                } else if (FrontFingerprintNavigation.this.mIsFingerprintRemoveHome) {
                    Log.d(FrontFingerprintNavigation.TAG, "Clicking to home was removed in gesture navigation mode");
                } else {
                    FrontFingerprintNavigation.this.sendKeyEvent(3);
                    FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                    frontFingerprintNavigation.startVibrate(frontFingerprintNavigation.mVibratorModeShortPressForFrontFp);
                    Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode,trans to home");
                }
            }
        }

        private void handleNaviTrikeySetting() {
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1 || !FrontFingerPrintSettings.isSupportTrikey()) {
                if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                    FrontFingerprintNavigation.this.sendKeyEvent(4);
                    FrontFingerprintNavigation.this.notifyTrikeyEvent(4);
                    return;
                }
                handleNaviBarEvent();
            } else if (!FrontFingerprintNavigation.this.isSingleTrikeyNaviMode()) {
                FrontFingerprintNavigation.this.sendKeyEvent(3);
                FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                frontFingerprintNavigation.startVibrate(frontFingerprintNavigation.mVibratorModeShortPressForFrontFp);
            } else if (FrontFingerprintNavigation.this.isMmiTesting()) {
                Log.d(FrontFingerprintNavigation.TAG, FrontFingerprintNavigation.LOG_MMI_TESTINT_NOW);
            } else {
                FrontFingerprintNavigation.this.sendKeyEvent(4);
                FrontFingerprintNavigation.this.notifyTrikeyEvent(4);
            }
        }

        private void handleNaviBarEvent() {
            if (FrontFingerprintNavigation.this.isMmiTesting()) {
                Log.d(FrontFingerprintNavigation.TAG, FrontFingerprintNavigation.LOG_MMI_TESTINT_NOW);
            } else if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
                Log.i(FrontFingerprintNavigation.TAG, "handle home event as NaviBarEnabled.");
                if (!FrontFingerprintNavigation.this.mIsFingerprintRemoveHome || !FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                    FrontFingerprintNavigation.this.sendKeyEvent(3);
                    FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                    frontFingerprintNavigation.startVibrate(frontFingerprintNavigation.mVibratorModeShortPressForFrontFp);
                    return;
                }
                Log.d(FrontFingerprintNavigation.TAG, "Clicking to home was removed");
            }
        }
    }

    final class SystemUiRecentInspector extends FingerprintNavigationInspector {
        SystemUiRecentInspector() {
            super();
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (FrontFingerprintNavigation.this.isScreenOff() || FrontFingerprintNavigation.this.isKeyguardLocked() || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isSuperPowerSaveMode() || FrontFingerprintNavigation.this.isAlarm() || FrontFingerprintNavigation.this.isStrongBox() || !FrontFingerprintNavigation.this.isValidRecentKeyEvent(event)) {
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "SystemUIRecentInspector State ok");
            return true;
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (!FrontFingerprintNavigation.this.mIsDeviceProvisioned || ev.getAction() == 0) {
                return;
            }
            if (FrontFingerprintNavigation.this.isTopTaskRecent() && !FrontFingerprintNavigation.this.mIsDockedStackMinimized) {
                return;
            }
            if (FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode, do not handle KEYCODE_APP_SWITCH");
            } else if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1 || !FrontFingerPrintSettings.isSupportTrikey()) {
                if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                    Flog.bdReport(FrontFingerprintNavigation.this.mContext, 10);
                    if (HwFrameworkFactory.getVRSystemServiceManager() == null || !HwFrameworkFactory.getVRSystemServiceManager().isVRMode()) {
                        Log.i(FrontFingerprintNavigation.TAG, "SystemUiRecentInspector handle sendKeyEvent KEYCODE_APP_SWITCH");
                        FrontFingerprintNavigation.this.sendKeyEvent(187);
                        FrontFingerprintNavigation.this.notifyTrikeyEvent(187);
                        return;
                    }
                    Log.d(FrontFingerprintNavigation.TAG, "Now is VRMode. return");
                }
            } else if (!FrontFingerprintNavigation.this.isSingleTrikeyNaviMode()) {
            } else {
                if (FrontFingerprintNavigation.this.isMmiTesting()) {
                    Log.d(FrontFingerprintNavigation.TAG, FrontFingerprintNavigation.LOG_MMI_TESTINT_NOW);
                    return;
                }
                Flog.bdReport(FrontFingerprintNavigation.this.mContext, 10);
                Log.i(FrontFingerprintNavigation.TAG, "SystemUiRecentInspector handle sendKeyEvent KEYCODE_APP_SWITCH");
                FrontFingerprintNavigation.this.sendKeyEvent(187);
                FrontFingerprintNavigation.this.notifyTrikeyEvent(187);
            }
        }
    }

    final class FingerPrintHomeUpInspector extends FingerprintNavigationInspector {
        FingerPrintHomeUpInspector() {
            super();
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if (!FrontFingerprintNavigation.this.isSpecialKey(event, FrontFingerprintNavigation.FRONT_FINGERPRINT_KEYCODE_HOME_UP) || FrontFingerprintNavigation.this.isScreenOff() || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isStrongBox()) {
                boolean unused = FrontFingerprintNavigation.this.mIsWakeUpScreen = false;
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "FingerPrintHomeUpInspector State ok");
            return true;
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (!FrontFingerprintNavigation.this.mIsDeviceProvisioned || ev.getAction() == 0) {
                return;
            }
            if (FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode, do not vibrate for long press");
                return;
            }
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1 || !FrontFingerPrintSettings.isSupportTrikey()) {
                if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver) && !FrontFingerprintNavigation.this.mIsWakeUpScreen) {
                    Log.i(FrontFingerprintNavigation.TAG, "handle FingerPrintHomeUpInspector!");
                    FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                    frontFingerprintNavigation.startVibrate(frontFingerprintNavigation.mVibrateHomeUpMode);
                }
            } else if (!FrontFingerprintNavigation.this.mIsWakeUpScreen) {
                Log.i(FrontFingerprintNavigation.TAG, "handle FingerPrintHomeUpInspector!");
                FrontFingerprintNavigation frontFingerprintNavigation2 = FrontFingerprintNavigation.this;
                frontFingerprintNavigation2.startVibrate(frontFingerprintNavigation2.mVibratorModeShortPressForFrontFp);
            }
            boolean unused = FrontFingerprintNavigation.this.mIsWakeUpScreen = false;
        }
    }

    final class SystemUiHomeInspector extends FingerprintNavigationInspector {
        SystemUiHomeInspector() {
            super();
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public boolean probe(InputEvent event) {
            if ((!FrontFingerprintNavigation.this.isSpecialKey(event, 66) && !FrontFingerprintNavigation.this.isSpecialKey(event, 502)) || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isStrongBox()) {
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "mSystemUIHomeInspector State ok");
            return true;
        }

        @Override // com.android.server.input.FrontFingerprintNavigation.FingerprintNavigationInspector
        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (FrontFingerprintNavigation.this.mIsDeviceProvisioned) {
                if (!FrontFingerprintNavigation.this.isScreenOff()) {
                    if (ev.getAction() != 0) {
                        Log.i(FrontFingerprintNavigation.TAG, "mSystemUiHomeInspector handle sendKeyEvent KEYCODE_HOME");
                        if (FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                            Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode, do not handle KEYCODE_FINGERPRINT_LONGPRESS");
                        } else {
                            handleNaviTrickeySetting();
                        }
                    }
                } else if (ev.getAction() == 1) {
                    Flog.bdReport(FrontFingerprintNavigation.this.mContext, 15);
                    FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                    FingerprintManager unused = frontFingerprintNavigation.mFingerprintManager = (FingerprintManager) frontFingerprintNavigation.mContext.getSystemService("fingerprint");
                    if (FrontFingerprintNavigation.this.mFingerprintManager == null) {
                        Log.e(FrontFingerprintNavigation.TAG, "mFingerprintManager is null");
                        return;
                    }
                    String access$000 = FrontFingerprintNavigation.TAG;
                    Log.i(access$000, "hasEnrolledFingerprints is:" + FrontFingerprintNavigation.this.mFingerprintManager.hasEnrolledFingerprints() + "mIsFpKeyguardEnable is:" + FrontFingerprintNavigation.this.mIsFpKeyguardEnable);
                    if (!FrontFingerprintNavigation.this.mFingerprintManager.hasEnrolledFingerprints() || !FrontFingerprintNavigation.this.mIsFpKeyguardEnable) {
                        boolean unused2 = FrontFingerprintNavigation.this.mIsWakeUpScreen = true;
                        FrontFingerprintNavigation.this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
                    }
                }
            }
        }

        private void handleNaviTrickeySetting() {
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1 || !FrontFingerPrintSettings.isSupportTrikey()) {
                if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                    Flog.bdReport(FrontFingerprintNavigation.this.mContext, 9);
                    FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                    frontFingerprintNavigation.startVibrate(frontFingerprintNavigation.mVibratorModeLongPressForHomeFrontFp);
                    FrontFingerprintNavigation.this.sendKeyEvent(3);
                    FrontFingerprintNavigation.this.notifyTrikeyEvent(3);
                    FrontFingerprintNavigation.this.checkLockMode();
                }
            } else if (FrontFingerprintNavigation.this.isSingleTrikeyNaviMode()) {
                Flog.bdReport(FrontFingerprintNavigation.this.mContext, 9);
                FrontFingerprintNavigation frontFingerprintNavigation2 = FrontFingerprintNavigation.this;
                frontFingerprintNavigation2.startVibrate(frontFingerprintNavigation2.mVibratorModeLongPressForHomeFrontFp);
                FrontFingerprintNavigation.this.sendKeyEvent(3);
                FrontFingerprintNavigation.this.notifyTrikeyEvent(3);
                FrontFingerprintNavigation.this.checkLockMode();
            } else {
                handleKeyguardLock();
            }
        }

        private void handleKeyguardLock() {
            if (FrontFingerprintNavigation.this.isKeyguardLocked()) {
                boolean unused = FrontFingerprintNavigation.this.mIsWakeUpScreen = true;
            } else if (FrontFingerprintNavigation.this.isMmiTesting()) {
                Log.d(FrontFingerprintNavigation.TAG, FrontFingerprintNavigation.LOG_MMI_TESTINT_NOW);
            } else {
                FrontFingerprintNavigation.this.startVoiceAssist();
                FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
                frontFingerprintNavigation.startVibrate(frontFingerprintNavigation.mVibratorModeLongPressForFrontFp);
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkLockMode() {
        IActivityTaskManager activityManager = ActivityTaskManager.getService();
        try {
            if (activityManager.isInLockTaskMode()) {
                activityManager.stopSystemLockTaskMode();
                Log.i(TAG, "longclick exit lockMode");
            }
        } catch (RemoteException e) {
            Log.i(TAG, "exit lockMode exception ");
        }
    }

    /* access modifiers changed from: private */
    public boolean isValidRecentKeyEvent(InputEvent event) {
        if (this.mContext.getResources().getConfiguration().orientation == 1) {
            return isSpecialKey(event, 514) || isSpecialKey(event, 513);
        }
        if (this.mContext.getResources().getConfiguration().orientation == 2) {
            return isSpecialKey(event, AwarenessConstants.SWING_GESTURE_ACTION_MAX) || isSpecialKey(event, 512);
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isKeyguardLocked() {
        KeyguardManager keyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguard != null) {
            return keyguard.isKeyguardLocked();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isStrongBox() {
        String activityName = getTopApp();
        if (activityName != null) {
            return activityName.equals("com.huawei.hidisk/.strongbox.ui.activity.StrongBoxVerifyPassActivity");
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isAlarm() {
        String pkgName = getTopApp();
        if (pkgName == null) {
            return false;
        }
        if (pkgName.equals("com.huawei.deskclock/com.android.deskclock.alarmclock.LockAlarmFullActivity") || pkgName.equals("com.android.deskclock/.alarmclock.LockAlarmFullActivity")) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isSettingEnroll() {
        String pkgName = getTopApp();
        if (pkgName != null) {
            return pkgName.equals("com.android.settings/.fingerprint.enrollment.FingerprintEnrollActivity");
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isSettingCalibrationIntro() {
        String pkgName = getTopApp();
        if (pkgName != null) {
            return pkgName.equals("com.android.settings/.fingerprint.enrollment.FingerprintCalibrationIntroActivity");
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void startVibrate(int virbateMode) {
        if (!isKeyguardLocked() && this.mIsHapticEnabled && !"true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false")) && this.mVibrator != null) {
            if (this.mIsPadDevice) {
                String str = TAG;
                Log.d(str, "startVibrateWithPattern:" + virbateMode);
                this.mVibrator.vibrate(VIBRATE_TIME);
                return;
            }
            String str2 = TAG;
            Log.d(str2, "startVibrateWithConfigProp:" + virbateMode);
            this.mVibrator.vibrate((long) virbateMode);
        }
    }

    private boolean isPad() {
        if ("tablet".equals(SystemProperties.get("ro.build.characteristics", ""))) {
            Log.d(TAG, "current device is pad!");
            return true;
        }
        Log.d(TAG, "current device is phone!");
        return false;
    }

    /* access modifiers changed from: private */
    public void startVoiceAssist() {
        Bundle bundle = new Bundle();
        StatusBarManagerInternal statusBarService = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        if (statusBarService != null) {
            statusBarService.startAssist(bundle);
        } else {
            Log.e(TAG, "Failed to execute startAssist");
        }
    }

    /* access modifiers changed from: private */
    public boolean isMmiTesting() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    private boolean isGestureNavEnable() {
        return IS_SUPPORT_GESTURE_NAV && this.mIsGestureNavEnable && (!isKeyguardOccluded() || !isKeyguardLocked());
    }
}
