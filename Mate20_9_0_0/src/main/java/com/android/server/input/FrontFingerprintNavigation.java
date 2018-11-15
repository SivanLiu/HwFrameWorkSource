package com.android.server.input;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Flog;
import android.util.Log;
import android.view.IDockedStackListener.Stub;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.List;

public class FrontFingerprintNavigation {
    private static final String FINGER_PRINT_ACTION_KEYEVENT = "com.android.huawei.FINGER_PRINT_ACTION_KEYEVENT";
    private static final long FP_HOME_VIBRATE_TIME = 60;
    public static final String FRONT_FINGERPRINT_BUTTON_LIGHT_MODE = "button_light_mode";
    public static final int FRONT_FINGERPRINT_KEYCODE_HOME_UP = 515;
    public static final String FRONT_FINGERPRINT_SWAP_KEY_POSITION = "swap_key_position";
    private static final String GESTURE_NAV_TRIKEY_SETTINGS = "secure_gesture_navigation";
    public static final String HAPTIC_FEEDBACK_TRIKEY_SETTINGS = "physic_navi_haptic_feedback_enabled";
    public static final String INTENT_KEY = "keycode";
    private static final int INVALID_NAVIMODE = -10000;
    private static final boolean IS_SUPPORT_GESTURE_NAV = SystemProperties.getBoolean("ro.config.gesture_front_support", false);
    private static final String TAG = FrontFingerprintNavigation.class.getSimpleName();
    private int VIBRATOR_MODE_LONG_PRESS_FOR_FRONT_FP = 16;
    private int VIBRATOR_MODE_LONG_PRESS_FOR_HOME_FRONT_FP = 16;
    private int VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP = 8;
    private boolean isNormalRunmode = "normal".equals(SystemProperties.get("ro.runmode", "normal"));
    private Context mContext;
    private boolean mDeviceProvisioned = true;
    DMDUtils mDmdUtils = null;
    private boolean mDockedStackMinimized = false;
    private int mFingerPrintId = -1;
    private FingerPrintHomeUpInspector mFingerPrintUpInspector;
    private FingerprintManager mFingerprintManager;
    private boolean mFingerprintRemoveHome = SystemProperties.getBoolean("ro.config.finger_remove_home", false);
    private final Handler mHandler;
    private boolean mHapticEnabled = true;
    private boolean mIsFPKeyguardEnable = false;
    private boolean mIsGestureNavEnable = false;
    private boolean mIsPadDevice = false;
    private boolean mIsWakeUpScreen = false;
    private PowerManager mPowerManager;
    private final ContentResolver mResolver;
    private SettingsObserver mSettingsObserver;
    private FingerprintNavigationInspector mSystemUIBackInspector;
    private FingerprintNavigationInspector mSystemUIHomeInspector;
    private FingerprintNavigationInspector mSystemUIRecentInspector;
    private int mTrikeyNaviMode = -1;
    private int mVibrateHomeUpMode = 8;
    private Vibrator mVibrator = null;

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
            FrontFingerprintNavigation.this.mDeviceProvisioned = Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation.this.mTrikeyNaviMode = System.getIntForUser(FrontFingerprintNavigation.this.mResolver, "swap_key_position", FrontFingerPrintSettings.getDefaultNaviMode(), ActivityManager.getCurrentUser());
            FrontFingerprintNavigation.this.mIsFPKeyguardEnable = Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "fp_keyguard_enable", 0, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation.this.mHapticEnabled = System.getIntForUser(FrontFingerprintNavigation.this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) != 0;
            if (Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "secure_gesture_navigation", 0, ActivityManager.getCurrentUser()) == 0) {
                z = false;
            }
            FrontFingerprintNavigation.this.mIsGestureNavEnable = z;
        }

        public void registerContentObserver(int userId) {
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(System.getUriFor("swap_key_position"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(System.getUriFor("device_provisioned"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor("fp_keyguard_enable"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(System.getUriFor("physic_navi_haptic_feedback_enabled"), false, this, userId);
            FrontFingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor("secure_gesture_navigation"), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            boolean z = true;
            FrontFingerprintNavigation.this.mDeviceProvisioned = Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation.this.mTrikeyNaviMode = System.getIntForUser(FrontFingerprintNavigation.this.mResolver, "swap_key_position", FrontFingerPrintSettings.getDefaultNaviMode(), ActivityManager.getCurrentUser());
            FrontFingerprintNavigation.this.mIsFPKeyguardEnable = Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "fp_keyguard_enable", 0, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation.this.mHapticEnabled = System.getIntForUser(FrontFingerprintNavigation.this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) != 0;
            FrontFingerprintNavigation frontFingerprintNavigation = FrontFingerprintNavigation.this;
            if (Secure.getIntForUser(FrontFingerprintNavigation.this.mResolver, "secure_gesture_navigation", 0, ActivityManager.getCurrentUser()) == 0) {
                z = false;
            }
            frontFingerprintNavigation.mIsGestureNavEnable = z;
        }
    }

    final class FingerPrintHomeUpInspector extends FingerprintNavigationInspector {
        FingerPrintHomeUpInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (!FrontFingerprintNavigation.this.isSpecialKey(event, FrontFingerprintNavigation.FRONT_FINGERPRINT_KEYCODE_HOME_UP) || FrontFingerprintNavigation.this.isScreenOff() || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isStrongBox()) {
                FrontFingerprintNavigation.this.mIsWakeUpScreen = false;
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "FingerPrintHomeUpInspector State ok");
            return true;
        }

        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (!FrontFingerprintNavigation.this.mDeviceProvisioned || ev.getAction() == 0) {
                return;
            }
            if (FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode, do not vibrate for long press");
                return;
            }
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1 && FrontFingerPrintSettings.isSupportTrikey()) {
                if (!FrontFingerprintNavigation.this.mIsWakeUpScreen) {
                    Log.i(FrontFingerprintNavigation.TAG, "handle FingerPrintHomeUpInspector!");
                    FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP);
                }
            } else if (!(FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver) || FrontFingerprintNavigation.this.mIsWakeUpScreen)) {
                Log.i(FrontFingerprintNavigation.TAG, "handle FingerPrintHomeUpInspector!");
                FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.mVibrateHomeUpMode);
            }
            FrontFingerprintNavigation.this.mIsWakeUpScreen = false;
        }
    }

    final class SystemUIBackInspector extends FingerprintNavigationInspector {
        SystemUIBackInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (!FrontFingerprintNavigation.this.isSpecialKey(event, WifiProCommonUtils.RESP_CODE_UNSTABLE) || FrontFingerprintNavigation.this.isScreenOff() || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isStrongBox()) {
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "SystemUIBackInspector State ok");
            return true;
        }

        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (FrontFingerprintNavigation.this.mDeviceProvisioned && ev.getAction() != 0) {
                Log.i(FrontFingerprintNavigation.TAG, "mSystemUIBackInspector handle sendKeyEvent : 4");
                if (!FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                    if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1 && FrontFingerPrintSettings.isSupportTrikey()) {
                        if (!FrontFingerprintNavigation.this.isSingleTrikeyNaviMode()) {
                            FrontFingerprintNavigation.this.sendKeyEvent(3);
                            FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP);
                        } else if (FrontFingerprintNavigation.this.isMMITesting()) {
                            Log.d(FrontFingerprintNavigation.TAG, "MMITesting now.");
                        } else {
                            FrontFingerprintNavigation.this.sendKeyEvent(4);
                            FrontFingerprintNavigation.this.notifyTrikeyEvent(4);
                        }
                    } else if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                        FrontFingerprintNavigation.this.sendKeyEvent(4);
                        FrontFingerprintNavigation.this.notifyTrikeyEvent(4);
                    } else if (FrontFingerprintNavigation.this.isMMITesting()) {
                        Log.d(FrontFingerprintNavigation.TAG, "MMITesting now.");
                    } else if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
                        Log.i(FrontFingerprintNavigation.TAG, "handle home event as NaviBarEnabled.");
                        if (FrontFingerprintNavigation.this.mFingerprintRemoveHome && FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                            Log.d(FrontFingerprintNavigation.TAG, "Clicking to home was removed");
                        } else {
                            FrontFingerprintNavigation.this.sendKeyEvent(3);
                            FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP);
                        }
                    }
                } else if (FrontFingerprintNavigation.this.mFingerprintRemoveHome) {
                    Log.d(FrontFingerprintNavigation.TAG, "Clicking to home was removed in gesture navigation mode");
                } else {
                    FrontFingerprintNavigation.this.sendKeyEvent(3);
                    FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP);
                    Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode,trans to home");
                }
            }
        }
    }

    final class SystemUIHomeInspector extends FingerprintNavigationInspector {
        SystemUIHomeInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if ((!FrontFingerprintNavigation.this.isSpecialKey(event, 66) && !FrontFingerprintNavigation.this.isSpecialKey(event, 502)) || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isStrongBox()) {
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "mSystemUIHomeInspector State ok");
            return true;
        }

        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (FrontFingerprintNavigation.this.mDeviceProvisioned) {
                if (FrontFingerprintNavigation.this.isScreenOff()) {
                    if (ev.getAction() == 1) {
                        Flog.bdReport(FrontFingerprintNavigation.this.mContext, 15);
                        FrontFingerprintNavigation.this.mFingerprintManager = (FingerprintManager) FrontFingerprintNavigation.this.mContext.getSystemService("fingerprint");
                        if (FrontFingerprintNavigation.this.mFingerprintManager == null) {
                            Log.e(FrontFingerprintNavigation.TAG, "mFingerprintManager is null");
                            return;
                        }
                        String access$000 = FrontFingerprintNavigation.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("hasEnrolledFingerprints is:");
                        stringBuilder.append(FrontFingerprintNavigation.this.mFingerprintManager.hasEnrolledFingerprints());
                        stringBuilder.append("mIsFPKeyguardEnable is:");
                        stringBuilder.append(FrontFingerprintNavigation.this.mIsFPKeyguardEnable);
                        Log.i(access$000, stringBuilder.toString());
                        if (!(FrontFingerprintNavigation.this.mFingerprintManager.hasEnrolledFingerprints() && FrontFingerprintNavigation.this.mIsFPKeyguardEnable)) {
                            FrontFingerprintNavigation.this.mIsWakeUpScreen = true;
                            FrontFingerprintNavigation.this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
                        }
                    }
                } else if (ev.getAction() != 0) {
                    Log.i(FrontFingerprintNavigation.TAG, "mSystemUIHomeInspector handle sendKeyEvent KEYCODE_HOME");
                    if (FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                        Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode, do not handle KEYCODE_FINGERPRINT_LONGPRESS");
                    } else if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1 && FrontFingerPrintSettings.isSupportTrikey()) {
                        if (FrontFingerprintNavigation.this.isSingleTrikeyNaviMode()) {
                            Flog.bdReport(FrontFingerprintNavigation.this.mContext, 9);
                            FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_LONG_PRESS_FOR_HOME_FRONT_FP);
                            FrontFingerprintNavigation.this.sendKeyEvent(3);
                            FrontFingerprintNavigation.this.notifyTrikeyEvent(3);
                            FrontFingerprintNavigation.this.checkLockMode();
                        } else if (FrontFingerprintNavigation.this.isKeyguardLocked()) {
                            FrontFingerprintNavigation.this.mIsWakeUpScreen = true;
                        } else if (FrontFingerprintNavigation.this.isMMITesting()) {
                            Log.d(FrontFingerprintNavigation.TAG, "MMITesting now.");
                        } else {
                            FrontFingerprintNavigation.this.startVoiceAssist();
                            FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_LONG_PRESS_FOR_FRONT_FP);
                        }
                    } else if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                        Flog.bdReport(FrontFingerprintNavigation.this.mContext, 9);
                        FrontFingerprintNavigation.this.startVibrate(FrontFingerprintNavigation.this.VIBRATOR_MODE_LONG_PRESS_FOR_HOME_FRONT_FP);
                        FrontFingerprintNavigation.this.sendKeyEvent(3);
                        FrontFingerprintNavigation.this.notifyTrikeyEvent(3);
                        FrontFingerprintNavigation.this.checkLockMode();
                    }
                }
            }
        }
    }

    final class SystemUIRecentInspector extends FingerprintNavigationInspector {
        SystemUIRecentInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FrontFingerprintNavigation.this.isScreenOff() || FrontFingerprintNavigation.this.isKeyguardLocked() || FrontFingerprintNavigation.this.isSettingEnroll() || FrontFingerprintNavigation.this.isSettingCalibrationIntro() || FrontFingerprintNavigation.this.isSuperPowerSaveMode() || FrontFingerprintNavigation.this.isAlarm() || FrontFingerprintNavigation.this.isStrongBox() || !FrontFingerprintNavigation.this.isValidRecentKeyEvent(event)) {
                return false;
            }
            Log.d(FrontFingerprintNavigation.TAG, "SystemUIRecentInspector State ok");
            return true;
        }

        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (!FrontFingerprintNavigation.this.mDeviceProvisioned || ev.getAction() == 0) {
                return;
            }
            if (FrontFingerprintNavigation.this.isTopTaskRecent() && !FrontFingerprintNavigation.this.mDockedStackMinimized) {
                return;
            }
            if (FrontFingerPrintSettings.isGestureNavigationMode(FrontFingerprintNavigation.this.mResolver)) {
                Log.d(FrontFingerprintNavigation.TAG, "now in Gesture Navigation Mode, do not handle KEYCODE_APP_SWITCH");
                return;
            }
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1 && FrontFingerPrintSettings.isSupportTrikey()) {
                if (FrontFingerprintNavigation.this.isSingleTrikeyNaviMode()) {
                    if (FrontFingerprintNavigation.this.isMMITesting()) {
                        Log.d(FrontFingerprintNavigation.TAG, "MMITesting now.");
                        return;
                    }
                    Flog.bdReport(FrontFingerprintNavigation.this.mContext, 10);
                    Log.i(FrontFingerprintNavigation.TAG, "SystemUIRecentInspector handle sendKeyEvent KEYCODE_APP_SWITCH");
                    FrontFingerprintNavigation.this.sendKeyEvent(187);
                    FrontFingerprintNavigation.this.notifyTrikeyEvent(187);
                }
            } else if (!FrontFingerPrintSettings.isNaviBarEnabled(FrontFingerprintNavigation.this.mResolver)) {
                Flog.bdReport(FrontFingerprintNavigation.this.mContext, 10);
                if (HwFrameworkFactory.getVRSystemServiceManager() == null || !HwFrameworkFactory.getVRSystemServiceManager().isVRMode()) {
                    Log.i(FrontFingerprintNavigation.TAG, "SystemUIRecentInspector handle sendKeyEvent KEYCODE_APP_SWITCH");
                    FrontFingerprintNavigation.this.sendKeyEvent(187);
                    FrontFingerprintNavigation.this.notifyTrikeyEvent(187);
                } else {
                    Log.d(FrontFingerprintNavigation.TAG, "Now is VRMode,.return");
                }
            }
        }
    }

    public FrontFingerprintNavigation(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSystemUIBackInspector = new SystemUIBackInspector();
        this.mSystemUIRecentInspector = new SystemUIRecentInspector();
        this.mSystemUIHomeInspector = new SystemUIHomeInspector();
        this.mFingerPrintUpInspector = new FingerPrintHomeUpInspector();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mVibrator = (SystemVibrator) ((Vibrator) this.mContext.getSystemService("vibrator"));
        this.mDmdUtils = new DMDUtils(context);
    }

    private void updateDockedStackFlag() {
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new Stub() {
                public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
                }

                public void onDockedStackExistsChanged(boolean exists) throws RemoteException {
                }

                public void onDockedStackMinimizedChanged(boolean minimized, long animDuration, boolean isHomeStackResizable) throws RemoteException {
                    String access$000 = FrontFingerprintNavigation.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onDockedStackMinimizedChanged:");
                    stringBuilder.append(minimized);
                    Log.d(access$000, stringBuilder.toString());
                    FrontFingerprintNavigation.this.mDockedStackMinimized = minimized;
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
        this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP = SystemProperties.getInt("ro.config.trikey_vibrate_touch", 8);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The trikey touch vibrate config value is:");
        stringBuilder.append(this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP);
        Log.d(str, stringBuilder.toString());
        this.VIBRATOR_MODE_LONG_PRESS_FOR_FRONT_FP = SystemProperties.getInt("ro.config.trikey_vibrate_press", 16);
        this.VIBRATOR_MODE_LONG_PRESS_FOR_HOME_FRONT_FP = this.VIBRATOR_MODE_LONG_PRESS_FOR_FRONT_FP;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("The trikey longPress vibrate config value is:");
        stringBuilder.append(this.VIBRATOR_MODE_LONG_PRESS_FOR_FRONT_FP);
        Log.d(str, stringBuilder.toString());
        this.mIsPadDevice = isPad();
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mIsPadDevice is:");
        stringBuilder.append(this.mIsPadDevice);
        Log.d(str, stringBuilder.toString());
        this.mFingerPrintId = SystemProperties.getInt("sys.fingerprint.deviceId", -1);
        this.mVibrateHomeUpMode = SystemProperties.getInt("ro.config.trikey_vibrate_touch_up", this.VIBRATOR_MODE_SHORT_PRESS_FOR_FRONT_FP);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mVibrateHomeUpMode is:");
        stringBuilder.append(this.mVibrateHomeUpMode);
        Log.d(str, stringBuilder.toString());
    }

    private void initDefaultNaviValue() {
        if (this.mResolver != null && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initDefaultNaviValue with user:");
            stringBuilder.append(ActivityManager.getCurrentUser());
            Log.d(str, stringBuilder.toString());
            initDefaultHapticProp();
            initDefaultNaviBarStatus();
            boolean deviceProvisioned = true;
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                String str2;
                StringBuilder stringBuilder2;
                if (System.getIntForUser(this.mResolver, "swap_key_position", INVALID_NAVIMODE, ActivityManager.getCurrentUser()) == INVALID_NAVIMODE) {
                    if (Secure.getIntForUser(this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) == 0) {
                        deviceProvisioned = false;
                    }
                    if (!deviceProvisioned) {
                        int trikeyNaviMode = FrontFingerPrintSettings.getDefaultNaviMode();
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("init default trikeyNaviMode to:");
                        stringBuilder2.append(trikeyNaviMode);
                        Log.d(str2, stringBuilder2.toString());
                        System.putIntForUser(this.mResolver, "swap_key_position", trikeyNaviMode, ActivityManager.getCurrentUser());
                    } else if (FrontFingerPrintSettings.isChinaArea()) {
                        Log.d(TAG, "init default trikeyNaviMode to singleButtonMode!");
                        System.putIntForUser(this.mResolver, "swap_key_position", -1, ActivityManager.getCurrentUser());
                    }
                }
                if (System.getIntForUser(this.mResolver, "button_light_mode", INVALID_NAVIMODE, ActivityManager.getCurrentUser()) == INVALID_NAVIMODE) {
                    int buttonLightMode = FrontFingerPrintSettings.getDefaultBtnLightMode();
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("init default buttonlight mode to:");
                    stringBuilder2.append(buttonLightMode);
                    Log.d(str2, stringBuilder2.toString());
                    System.putIntForUser(this.mResolver, "button_light_mode", buttonLightMode, ActivityManager.getCurrentUser());
                }
            }
        }
    }

    private void initDefaultHapticProp() {
        if ((FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0 || FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) && System.getIntForUser(this.mResolver, "physic_navi_haptic_feedback_enabled", INVALID_NAVIMODE, ActivityManager.getCurrentUser()) == INVALID_NAVIMODE) {
            Log.d(TAG, "init default hapicProp to enabled!");
            System.putIntForUser(this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser());
        }
    }

    private void initDefaultNaviBarStatus() {
        int userID = ActivityManager.getCurrentUser();
        if (System.getIntForUser(this.mResolver, "enable_navbar", INVALID_NAVIMODE, userID) == INVALID_NAVIMODE) {
            boolean naviBarStatus = FrontFingerPrintSettings.isNaviBarEnabled(this.mResolver);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("init defaultNaviBarStatus to:");
            stringBuilder.append(naviBarStatus);
            Log.d(str, stringBuilder.toString());
            int status = 1;
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
                if (!(Secure.getIntForUser(this.mResolver, "device_provisioned", 0, userID) != 0)) {
                    System.putIntForUser(this.mResolver, "enable_navbar", naviBarStatus, userID);
                } else if (FrontFingerPrintSettings.isChinaArea()) {
                    if (FrontFingerPrintSettings.SINGLE_VIRTUAL_NAVIGATION_MODE != 1) {
                        status = 0;
                    }
                    System.putIntForUser(this.mResolver, "enable_navbar", status, userID);
                } else {
                    System.putIntForUser(this.mResolver, "enable_navbar", 1, userID);
                }
            } else if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                System.putIntForUser(this.mResolver, "enable_navbar", naviBarStatus, userID);
            }
        }
    }

    private boolean isTopTaskRecent() {
        boolean isRunningTaskInRecentStack = false;
        try {
            List<RunningTaskInfo> tasks = ActivityManager.getService().getFilteredTasks(1, 0, 2);
            if (tasks.isEmpty()) {
                return false;
            }
            if (((RunningTaskInfo) tasks.get(0)).configuration.windowConfiguration.getActivityType() == 3) {
                isRunningTaskInRecentStack = true;
            }
            return isRunningTaskInRecentStack;
        } catch (RemoteException e) {
            return false;
        }
    }

    private void notifyTrikeyEvent(int keyCode) {
        if (this.mDeviceProvisioned) {
            int isShowNaviGuide = System.getIntForUser(this.mResolver, "systemui_tips_already_shown", 0, ActivityManager.getCurrentUser());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isShowNaviGuide:");
            stringBuilder.append(isShowNaviGuide);
            Log.d(str, stringBuilder.toString());
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
        } else if (this.mSystemUIBackInspector.probe(event)) {
            this.mSystemUIBackInspector.handle(event);
            return true;
        } else if (this.mSystemUIRecentInspector.probe(event)) {
            this.mSystemUIRecentInspector.handle(event);
            return true;
        } else if (this.mSystemUIHomeInspector.probe(event)) {
            this.mSystemUIHomeInspector.handle(event);
            return true;
        } else if (!this.mFingerPrintUpInspector.probe(event)) {
            return false;
        } else {
            this.mFingerPrintUpInspector.handle(event);
            return true;
        }
    }

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return power.isScreenOn() ^ 1;
        }
        return false;
    }

    private String getTopApp() {
        String pkgName = ((ActivityManagerService) ServiceManager.getService("activity")).topAppName();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TopApp is ");
        stringBuilder.append(pkgName);
        Log.d(str, stringBuilder.toString());
        return pkgName;
    }

    private boolean isSpecialKey(InputEvent event, int code) {
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        KeyEvent ev = (KeyEvent) event;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("keycode is : ");
        stringBuilder.append(ev.getKeyCode());
        Log.d(str, stringBuilder.toString());
        if (ev.getKeyCode() == code) {
            return true;
        }
        return false;
    }

    private void sendKeyEvent(int keycode) {
        int[] actions = new int[]{0, 1};
        KeyEvent ev = null;
        for (int keyEvent : actions) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, keyEvent, keycode, 0, 0, this.mFingerPrintId, 0, 8, 257), 0);
        }
    }

    private boolean isSuperPowerSaveMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    public void setCurrentUser(int newUserId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCurrentUser:");
        stringBuilder.append(newUserId);
        Log.d(str, stringBuilder.toString());
        initDefaultValueWithUser();
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
    }

    private void initDefaultValueWithUser() {
        if (this.mResolver != null && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION) {
            String str;
            int userID = ActivityManager.getCurrentUser();
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initDefaultNaviValue with user:");
            stringBuilder.append(userID);
            Log.d(str2, stringBuilder.toString());
            initDefaultHapticProp();
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                if (System.getIntForUser(this.mResolver, "swap_key_position", INVALID_NAVIMODE, userID) == INVALID_NAVIMODE) {
                    int trikeyNaviMode = FrontFingerPrintSettings.getDefaultNaviMode();
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("init default trikeyNaviMode to:");
                    stringBuilder2.append(trikeyNaviMode);
                    Log.d(str, stringBuilder2.toString());
                    System.putIntForUser(this.mResolver, "swap_key_position", trikeyNaviMode, userID);
                }
                if (System.getIntForUser(this.mResolver, "button_light_mode", INVALID_NAVIMODE, userID) == INVALID_NAVIMODE) {
                    int buttonLightMode = FrontFingerPrintSettings.getDefaultBtnLightMode();
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("init user buttonlight mode to:");
                    stringBuilder3.append(buttonLightMode);
                    Log.d(str3, stringBuilder3.toString());
                    System.putIntForUser(this.mResolver, "button_light_mode", buttonLightMode, userID);
                }
            }
            if (System.getIntForUser(this.mResolver, "enable_navbar", INVALID_NAVIMODE, userID) == INVALID_NAVIMODE) {
                boolean naviBarStatus = FrontFingerPrintSettings.isNaviBarEnabled(this.mResolver);
                str = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("init defaultNaviBarStatus to:");
                stringBuilder4.append(naviBarStatus);
                Log.d(str, stringBuilder4.toString());
                System.putIntForUser(this.mResolver, "enable_navbar", naviBarStatus, userID);
            }
        }
    }

    private boolean isSingleTrikeyNaviMode() {
        return this.mTrikeyNaviMode < 0;
    }

    private void checkLockMode() {
        IActivityManager activityManager = ActivityManager.getService();
        try {
            if (activityManager.isInLockTaskMode()) {
                activityManager.stopSystemLockTaskMode();
                Log.i(TAG, "longclick exit lockMode");
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit lockMode exception ");
            stringBuilder.append(e.toString());
            Log.i(str, stringBuilder.toString());
        }
    }

    private boolean isValidRecentKeyEvent(InputEvent event) {
        boolean z = false;
        if (1 == this.mContext.getResources().getConfiguration().orientation) {
            if (isSpecialKey(event, 514) || isSpecialKey(event, 513)) {
                z = true;
            }
            return z;
        } else if (2 != this.mContext.getResources().getConfiguration().orientation) {
            return false;
        } else {
            if (isSpecialKey(event, 511) || isSpecialKey(event, 512)) {
                z = true;
            }
            return z;
        }
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguard != null) {
            return keyguard.isKeyguardLocked();
        }
        return false;
    }

    private boolean isStrongBox() {
        String activityName = getTopApp();
        String activity_strongbox = "com.huawei.hidisk/.strongbox.ui.activity.StrongBoxVerifyPassActivity";
        if (activityName != null) {
            return activityName.equals(activity_strongbox);
        }
        return false;
    }

    private boolean isAlarm() {
        String pkgName = getTopApp();
        String pkg_alarm = "com.android.deskclock/.alarmclock.LockAlarmFullActivity";
        if (pkgName != null) {
            return pkgName.equals(pkg_alarm);
        }
        return false;
    }

    private boolean isSettingEnroll() {
        String pkgName = getTopApp();
        String pkg_setting = "com.android.settings/.fingerprint.enrollment.FingerprintEnrollActivity";
        if (pkgName != null) {
            return pkgName.equals(pkg_setting);
        }
        return false;
    }

    private boolean isSettingCalibrationIntro() {
        String pkgName = getTopApp();
        String pkg_setting = "com.android.settings/.fingerprint.enrollment.FingerprintCalibrationIntroActivity";
        if (pkgName != null) {
            return pkgName.equals(pkg_setting);
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:13:0x0060, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void startVibrate(int virbateMode) {
        if (!(isKeyguardLocked() || !this.mHapticEnabled || "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false")) || this.mVibrator == null)) {
            String str;
            StringBuilder stringBuilder;
            if (this.mIsPadDevice) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startVibrateWithPattern:");
                stringBuilder.append(virbateMode);
                Log.d(str, stringBuilder.toString());
                this.mVibrator.hwVibrate(null, virbateMode);
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startVibrateWithConfigProp:");
                stringBuilder.append(virbateMode);
                Log.d(str, stringBuilder.toString());
                this.mVibrator.vibrate((long) virbateMode);
            }
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

    private void startVoiceAssist() {
        try {
            ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).startAssist(new Bundle());
        } catch (Exception exp) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startVoiceAssist error:");
            stringBuilder.append(exp.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private boolean isMMITesting() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    private boolean isGestureNavEnable() {
        return IS_SUPPORT_GESTURE_NAV && this.mIsGestureNavEnable;
    }
}
