package com.android.server.input;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Debug;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telecom.TelecomManager;
import android.util.HwPCUtils;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.accessibility.AccessibilityManager;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.HwActivityManagerService;
import com.android.server.policy.AbsPhoneWindowManager;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.statusbar.HwStatusBarManagerService;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.cust.HwCustUtils;
import huawei.android.provider.FrontFingerPrintSettings;

public final class FingerprintNavigation {
    private static final boolean DEBUG;
    static final boolean ENABLE_BACK_TO_HOME = false;
    static final boolean ENABLE_DOUBLE_TAP = false;
    static final boolean ENABLE_LOCK_DEVICE = false;
    static final boolean ENABLE_SHOW_FINGERPRINT_TIPS = false;
    static final boolean ENABLE_SHOW_NOTIFICATION = false;
    static final String FINGERPRINT_ANSWER_CALL = "fp_answer_call";
    static final String FINGERPRINT_BACK_TO_HOME = "fp_return_desk";
    static final String FINGERPRINT_CAMERA_SWITCH = "fp_take_photo";
    static final String FINGERPRINT_GALLERY_SLIDE = "fingerprint_gallery_slide";
    static final String FINGERPRINT_GO_BACK = "fp_go_back";
    static final String FINGERPRINT_LOCK_DEVICE = "fp_lock_device";
    static final String FINGERPRINT_MARKET_DEMO_PKG = "com.szdv";
    static final String FINGERPRINT_RECENT_APP = "fp_recent_application";
    static final String FINGERPRINT_SHOW_NOTIFICATION = "fp_show_notification";
    static final String FINGERPRINT_SLIDE_SWITCH = "fingerprint_slide_switch";
    static final String FINGERPRINT_STOP_ALARM = "fp_stop_alarm";
    static final String FINGERPRINT_USED_FIRSTLY = "fp_used_firstyl";
    public static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int SINGLETAP_DELAY_TIMEOUT = 300;
    static final String TAG = "FingerprintNavigation";
    static final String TAG_FP = "FPNavigation";
    static final int VERIFY_MSG = 1;
    private AccessibilityManager mAccessibilityManager;
    final ComponentName mAlarmServiceCmp = ComponentName.unflattenFromString("com.android.deskclock/.alarmclock.AlarmKlaxon");
    boolean mAnswerCall = false;
    boolean mBackToHome = false;
    FingerprintNavigationInspector mCameraInspector;
    private final Runnable mCameraLongPress = new Runnable() {
        public void run() {
            Log.d(FingerprintNavigation.TAG, "CameraLongPress ,so send KeyEvent.KEYCODE_CAMERA");
            FingerprintNavigation.this.sendKeyEvent(27);
        }
    };
    FingerprintNavigationInspector mCollapsePanelsInspector;
    private Context mContext;
    int mCurUser;
    FingerprintNavigationInspector mDefaultInspector;
    private boolean mDeviceProvisioned;
    FingerprintNavigationInspector mDoubleTapInspector;
    private int mFingerPrintId = -1;
    FingerprintNavigationInspector mFingerprintDemoInspector;
    boolean mFingerprintMarketDemoSwitch = false;
    private boolean mFingerprintUsedFirstly;
    private FrontFingerprintNavigation mFrontFingerprintNav = null;
    FingerprintNavigationInspector mGalleryInspector;
    boolean mGallerySlide;
    boolean mGoBack = false;
    final Handler mHandler;
    boolean mHasReadDB = false;
    private HwCustFingerprintNavigation mHwCust;
    FingerprintNavigationInspector mInCallInspector;
    boolean mInjectCamera = false;
    boolean mInjectSlide = false;
    FingerprintNavigationInspector mLauncherInspector;
    private final Runnable mLauncherLongPress = new Runnable() {
        public void run() {
            Log.d(FingerprintNavigation.TAG, "LauncherLongPress ,so expandNotificationsPanel");
            ((StatusBarManager) FingerprintNavigation.this.mContext.getSystemService("statusbar")).expandNotificationsPanel();
        }
    };
    boolean mLockDevice = false;
    FingerprintNavigationInspector mLongPressOnScreenOffInspector;
    private CheckForSingleTap mPendingCheckForSingleTap;
    PowerManager mPowerManager;
    boolean mRecentApp = false;
    private final ContentResolver mResolver;
    final SettingsObserver mSettingsObserver;
    boolean mShowNotification = false;
    FingerprintNavigationInspector mSingleTapInspector;
    FingerprintNavigationInspector mStartHomeInspector;
    boolean mStopAlarm = false;
    FingerprintNavigationInspector mStopAlarmInspector;
    private WindowManagerInternal mWindowManagerInternal;

    private final class CheckForSingleTap implements Runnable {
        private CheckForSingleTap() {
        }

        /* synthetic */ CheckForSingleTap(FingerprintNavigation x0, AnonymousClass1 x1) {
            this();
        }

        public void run() {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendBack, goBack=");
                stringBuilder.append(FingerprintNavigation.this.mGoBack);
                Log.d(str, stringBuilder.toString());
            }
            if (FingerprintNavigation.this.isInCallUI()) {
                FingerprintNavigation.this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
            }
            if (FingerprintNavigation.this.mGoBack) {
                FingerprintNavigation.this.showNotificationTips();
                StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 3);
                FingerprintNavigation.this.mSingleTapInspector.handleTap();
            }
        }
    }

    abstract class FingerprintNavigationInspector {
        FingerprintNavigationInspector() {
        }

        public boolean probe(InputEvent event) {
            return false;
        }

        public boolean probe(InputEvent event, boolean unHandledKey) {
            return false;
        }

        public void handle(InputEvent event) {
        }

        public void handleTap() {
        }

        public void handleDoubleTap() {
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
        }

        public void registerContentObserver(int userId) {
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_SLIDE_SWITCH), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_CAMERA_SWITCH), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_ANSWER_CALL), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_SHOW_NOTIFICATION), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_BACK_TO_HOME), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_STOP_ALARM), false, this, userId);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_LOCK_DEVICE), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_GO_BACK), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_RECENT_APP), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Global.getUriFor("device_provisioned"), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(System.getUriFor("fingerprint_market_demo_switch"), false, this);
            FingerprintNavigation.this.mResolver.registerContentObserver(Secure.getUriFor(FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            Log.d(FingerprintNavigation.TAG, "SettingDB has Changed");
            boolean z = false;
            FingerprintNavigation.this.updateFingerNaviSwitchValue(false);
            FingerprintNavigation.this.mDeviceProvisioned = Secure.getInt(FingerprintNavigation.this.mResolver, "device_provisioned", 0) != 0;
            FingerprintNavigation.this.mFingerprintMarketDemoSwitch = System.getInt(FingerprintNavigation.this.mResolver, "fingerprint_market_demo_switch", 0) == 1;
            FingerprintNavigation fingerprintNavigation;
            if (FingerprintNavigation.FRONT_FINGERPRINT_NAVIGATION) {
                fingerprintNavigation = FingerprintNavigation.this;
                if (Secure.getIntForUser(FingerprintNavigation.this.mResolver, FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE, 0, ActivityManager.getCurrentUser()) != 0) {
                    z = true;
                }
                fingerprintNavigation.mGallerySlide = z;
                return;
            }
            fingerprintNavigation = FingerprintNavigation.this;
            if (Secure.getIntForUser(FingerprintNavigation.this.mResolver, FingerprintNavigation.FINGERPRINT_GALLERY_SLIDE, 1, ActivityManager.getCurrentUser()) != 0) {
                z = true;
            }
            fingerprintNavigation.mGallerySlide = z;
        }
    }

    final class CameraInspector extends FingerprintNavigationInspector {
        CameraInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            String pkgName = FingerprintNavigation.this.getTopApp();
            if (pkgName == null) {
                return false;
            }
            String pkg_gallery = "com.android.gallery3d";
            if ((!FingerprintNavigation.this.isCamera() && !pkgName.startsWith(pkg_gallery)) || !FingerprintNavigation.this.mInjectCamera) {
                if (FingerprintNavigation.this.mInjectCamera) {
                    Log.d(FingerprintNavigation.TAG, "Top app is not available,reset settings DB!");
                }
                return false;
            } else if (FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, 66)) {
                return false;
            } else {
                return true;
            }
        }

        public void handle(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                Log.d(FingerprintNavigation.TAG, "Current State is Camera and KEYCODE_ENTER Event!");
            }
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() == 0) {
                FingerprintNavigation.this.mHandler.postDelayed(FingerprintNavigation.this.mCameraLongPress, 0);
            } else if (ev.getAction() == 1) {
                FingerprintNavigation.this.mHandler.removeCallbacks(FingerprintNavigation.this.mCameraLongPress);
            }
        }
    }

    final class CollapsePanelsInspector extends FingerprintNavigationInspector {
        CollapsePanelsInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                Log.d(FingerprintNavigation.TAG, "CollapsePanelsInspector prob");
            }
            if (FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isVRMode() || !FingerprintNavigation.this.isSpecialKey(event, 511)) {
                return false;
            }
            return true;
        }

        public void handle(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Current State is Launcher and KEYCODE_DPAD_TOP Event, event=");
                stringBuilder.append(event);
                Log.d(str, stringBuilder.toString());
            }
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.showNotificationTips();
                HwStatusBarManagerService hwStatusBarService = (HwStatusBarManagerService) ServiceManager.getService("statusbar");
                if (hwStatusBarService != null) {
                    if (FingerprintNavigation.DEBUG) {
                        String str2 = FingerprintNavigation.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("collapse process, isScenseExcluded=");
                        stringBuilder2.append(FingerprintNavigation.this.isScenseExcluded());
                        stringBuilder2.append(", mShowNotification=");
                        stringBuilder2.append(FingerprintNavigation.this.mShowNotification);
                        stringBuilder2.append(",statusBarExpanded=");
                        stringBuilder2.append(hwStatusBarService.statusBarExpanded());
                        stringBuilder2.append(",isAlarm()=");
                        stringBuilder2.append(FingerprintNavigation.this.isAlarm());
                        stringBuilder2.append(",isLandscape=");
                        stringBuilder2.append(FingerprintNavigation.this.isLandscape());
                        stringBuilder2.append(",mRecentApp=");
                        stringBuilder2.append(FingerprintNavigation.this.mRecentApp);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    FingerprintNavigation.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                    if (!FingerprintNavigation.this.mShowNotification || (FingerprintNavigation.this.mRecentApp && !hwStatusBarService.statusBarExpanded())) {
                        if (!FingerprintNavigation.this.mRecentApp || FingerprintNavigation.this.isAlarm() || FingerprintNavigation.this.isInCallUIAndRinging() || FingerprintNavigation.this.isLandscape()) {
                            Log.w(FingerprintNavigation.TAG, "No switch is oppened for process");
                        } else {
                            StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 5);
                            FingerprintNavigation.this.sendKeyEvent(187);
                        }
                    } else if (!(FingerprintNavigation.this.isScenseExcluded() || ((FingerprintNavigation.this.isAlarm() && FingerprintNavigation.this.topIsFullScreen()) || FingerprintNavigation.this.isLandscape()))) {
                        StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 2);
                        hwStatusBarService.collapsePanels();
                    }
                }
            }
        }
    }

    final class DefaultInspector extends FingerprintNavigationInspector {
        DefaultInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isKeyguardLocked() || !FingerprintNavigation.this.mInjectSlide) {
                return false;
            }
            return true;
        }

        public void handle(InputEvent event) {
            InputEvent inputEvent = event;
            if (inputEvent instanceof MotionEvent) {
                Log.d(FingerprintNavigation.TAG, "Start Inject Motionevent!");
                MotionEvent mv = (MotionEvent) inputEvent;
                long curTime = SystemClock.uptimeMillis();
                int pointCount = mv.getPointerCount();
                PointerProperties[] ppt = PointerProperties.createArray(pointCount);
                PointerCoords[] pcd = PointerCoords.createArray(pointCount);
                for (int i = 0; i < pointCount; i++) {
                    mv.getPointerProperties(i, ppt[i]);
                    mv.getPointerCoords(i, pcd[i]);
                }
                int i2 = 0;
                InputManager.getInstance().injectInputEvent(MotionEvent.obtain(curTime, curTime, mv.getAction(), pointCount, ppt, pcd, mv.getMetaState(), mv.getButtonState(), mv.getXPrecision(), mv.getYPrecision(), 5, mv.getEdgeFlags(), mv.getSource(), mv.getFlags()), i2);
            }
        }
    }

    final class DoubleTapInspector extends FingerprintNavigationInspector {
        DoubleTapInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (!FingerprintNavigation.this.mLockDevice || FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isInCallUI() || FingerprintNavigation.this.isCamera() || FingerprintNavigation.this.isAlarm() || !FingerprintNavigation.this.isSpecialKey(event, 26)) {
                return false;
            }
            return true;
        }

        public void handle(InputEvent event) {
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.sendKeyEvent(26);
            }
        }

        public void handleDoubleTap() {
            Log.e(FingerprintNavigation.TAG, "sendDoubleTap");
            FingerprintNavigation.this.sendKeyEvent(501);
        }
    }

    final class FingerprintDemoInspector extends FingerprintNavigationInspector {
        FingerprintDemoInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isKeyguardLocked() || !FingerprintNavigation.this.mFingerprintMarketDemoSwitch || !FingerprintNavigation.this.isFingerprintDemo()) {
                return false;
            }
            if (FingerprintNavigation.DEBUG) {
                Log.d(FingerprintNavigation.TAG, "FingerprintDemoInspector check ok");
            }
            return true;
        }

        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() != 0) {
                FingerprintNavigation.this.sendKeyEvent(ev.getKeyCode());
            }
        }
    }

    final class GalleryInspector extends FingerprintNavigationInspector {
        GalleryInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                Log.d(FingerprintNavigation.TAG, "GalleryInspector State probe");
            }
            if (!FingerprintNavigation.this.mGallerySlide || !FingerprintNavigation.this.isGallery() || FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isVRMode() || (!FingerprintNavigation.this.isSpecialKey(event, 513) && !FingerprintNavigation.this.isSpecialKey(event, 514))) {
                return false;
            }
            if (FingerprintNavigation.DEBUG) {
                Log.d(FingerprintNavigation.TAG, "GalleryInspector State ok");
            }
            return true;
        }

        public void handle(InputEvent event) {
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() != 0) {
                FingerprintNavigation.this.sendKeyEvent(ev.getKeyCode());
            }
        }
    }

    final class InCallInspector extends FingerprintNavigationInspector {
        InCallInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.mAnswerCall && !FingerprintNavigation.this.isScreenOff() && (FingerprintNavigation.this.isSpecialKey(event, 66) || FingerprintNavigation.this.isSpecialKey(event, 502))) {
                TelecomManager telecomManager = (TelecomManager) FingerprintNavigation.this.mContext.getSystemService("telecom");
                if (telecomManager != null && telecomManager.isRinging()) {
                    return true;
                }
            }
            return false;
        }

        public void handle(InputEvent event) {
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.showNotificationTips();
                if (FingerprintNavigation.this.mAnswerCall) {
                    TelecomManager telecomManager = (TelecomManager) FingerprintNavigation.this.mContext.getSystemService("telecom");
                    if (telecomManager != null) {
                        StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 6);
                        telecomManager.acceptRingingCall();
                    }
                }
            }
        }
    }

    final class LauncherInspector extends FingerprintNavigationInspector {
        LauncherInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LauncherInspector State probe, isScreenOff() = ");
                stringBuilder.append(FingerprintNavigation.this.isScreenOff());
                stringBuilder.append(",isSpecialKey = ");
                stringBuilder.append(FingerprintNavigation.this.isSpecialKey(event, 512));
                stringBuilder.append(",isScenseExcluded = ");
                stringBuilder.append(FingerprintNavigation.this.isScenseExcluded());
                stringBuilder.append(",isAlarm = ");
                stringBuilder.append(FingerprintNavigation.this.isAlarm());
                stringBuilder.append(",isLandscape()=");
                stringBuilder.append(FingerprintNavigation.this.isLandscape());
                Log.d(str, stringBuilder.toString());
            }
            if (FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, 512) || FingerprintNavigation.this.isScenseExcluded() || ((FingerprintNavigation.this.isAlarm() && FingerprintNavigation.this.topIsFullScreen()) || FingerprintNavigation.this.isLandscape())) {
                return false;
            }
            Log.d(FingerprintNavigation.TAG, "LauncherInspector State ok");
            return true;
        }

        public void handle(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handle Launcher  KEYCODE_DPAD_DOWN Event, topIsFullScreen: ");
                stringBuilder.append(FingerprintNavigation.this.topIsFullScreen());
                stringBuilder.append(", focusedWinOverStatusBar: ");
                stringBuilder.append(FingerprintNavigation.this.focusedWinOverStatusBar());
                stringBuilder.append(", obs: ");
                stringBuilder.append(FingerprintNavigation.this.statusBarObsecured());
                Log.d(str, stringBuilder.toString());
            }
            if (((KeyEvent) event).getAction() == 0) {
                Log.d(FingerprintNavigation.TAG, "getAction is ACTION_DOWN");
                return;
            }
            FingerprintNavigation.this.showNotificationTips();
            String str2 = FingerprintNavigation.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("LauncherInspector handle mShowNotification :");
            stringBuilder2.append(FingerprintNavigation.this.mShowNotification);
            Log.d(str2, stringBuilder2.toString());
            if (FingerprintNavigation.this.mShowNotification && !FingerprintNavigation.this.focusedWinOverStatusBar() && !FingerprintNavigation.this.statusBarObsecured()) {
                StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 1);
                FingerprintNavigation.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                if (FingerprintNavigation.this.topIsFullScreen() && FingerprintNavigation.this.canShowTransientBar()) {
                    FingerprintNavigation.this.requestTransientStatusBars();
                } else {
                    ((StatusBarManager) FingerprintNavigation.this.mContext.getSystemService("statusbar")).expandNotificationsPanel();
                }
            }
        }
    }

    final class LongPressOnScreenOffInspector extends FingerprintNavigationInspector {
        LongPressOnScreenOffInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() && FingerprintNavigation.this.isSpecialKey(event, 66)) {
                return true;
            }
            return false;
        }

        public void handle(InputEvent event) {
            ((PowerManager) FingerprintNavigation.this.mContext.getSystemService("power")).newWakeLock(1, "COVER_WAKE_LOCK").acquire(MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
            Log.d(FingerprintNavigation.TAG, "Current State is LongPressOnScreenOff , Start dealwith event!");
            KeyEvent ev = (KeyEvent) event;
            long curTime = SystemClock.uptimeMillis();
            Intent intent = new Intent("com.android.server.input.fpn");
            intent.putExtra("keytype", ev.getAction());
            intent.putExtra("eventtime", curTime);
            FingerprintNavigation.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    final class SingleTapInspector extends FingerprintNavigationInspector {
        SingleTapInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.this.isScreenOff() || !FingerprintNavigation.this.isSpecialKey(event, WifiProCommonUtils.RESP_CODE_UNSTABLE)) {
                return false;
            }
            return true;
        }

        public void handle(InputEvent event) {
            if (((KeyEvent) event).getAction() != 0) {
                FingerprintNavigation.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
            }
        }

        public void handleTap() {
            if (!FingerprintNavigation.this.isInCallUIAndRinging()) {
                FingerprintNavigation.this.sendKeyEvent(4);
            }
        }
    }

    final class StartHomeInspector extends FingerprintNavigationInspector {
        StartHomeInspector() {
            super();
        }

        public boolean probe(InputEvent event, boolean unHandledKey) {
            int i = 66;
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StartHomeInspector State probe, isEnter: ");
                stringBuilder.append(FingerprintNavigation.this.isSpecialKey(event, 66));
                stringBuilder.append(",isScreenOff(): ");
                stringBuilder.append(FingerprintNavigation.this.isScreenOff());
                stringBuilder.append(",isKeyguardLocked: ");
                stringBuilder.append(FingerprintNavigation.this.isKeyguardLocked());
                stringBuilder.append(",isCamera: ");
                stringBuilder.append(FingerprintNavigation.this.isCamera());
                Log.d(str, stringBuilder.toString());
            }
            if (!(!FingerprintNavigation.this.mBackToHome || FingerprintNavigation.this.isScreenOff() || FingerprintNavigation.this.isKeyguardLocked())) {
                FingerprintNavigation fingerprintNavigation = FingerprintNavigation.this;
                if (unHandledKey) {
                    i = 502;
                }
                if (!(!fingerprintNavigation.isSpecialKey(event, i) || FingerprintNavigation.this.isAlarm() || FingerprintNavigation.this.isInCallUIAndRinging())) {
                    return true;
                }
            }
            return false;
        }

        public void handle(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StartHomeInspector State handle, event: ");
                stringBuilder.append(event);
                Log.d(str, stringBuilder.toString());
            }
            if (((KeyEvent) event).getAction() != 0) {
                StatisticalUtils.reportc(FingerprintNavigation.this.mContext, 4);
                FingerprintNavigation.this.sendKeyEvent(3);
            }
        }
    }

    final class StopAlarmInspector extends FingerprintNavigationInspector {
        StopAlarmInspector() {
            super();
        }

        public boolean probe(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StopAlarm State prob, mStopAlarm: ");
                stringBuilder.append(FingerprintNavigation.this.mStopAlarm);
                stringBuilder.append(",isAlarm: ");
                stringBuilder.append(FingerprintNavigation.this.isAlarm());
                Log.d(str, stringBuilder.toString());
            }
            if (FingerprintNavigation.this.mStopAlarm && !FingerprintNavigation.this.isScreenOff() && FingerprintNavigation.this.isAlarm() && (FingerprintNavigation.this.isSpecialKey(event, 66) || FingerprintNavigation.this.isSpecialKey(event, 502))) {
                return true;
            }
            return false;
        }

        public void handle(InputEvent event) {
            if (FingerprintNavigation.DEBUG) {
                String str = FingerprintNavigation.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StopAlarm State handle, event: ");
                stringBuilder.append(event);
                Log.d(str, stringBuilder.toString());
            }
            KeyEvent ev = (KeyEvent) event;
            if (ev.getAction() != 0) {
                FingerprintNavigation.this.showNotificationTips();
                if (FingerprintNavigation.this.mStopAlarm) {
                    long curTime = SystemClock.uptimeMillis();
                    Intent intent = new Intent("com.android.server.input.fpn.stopalarm");
                    intent.putExtra("keytype", ev.getAction());
                    intent.putExtra("eventtime", curTime);
                    FingerprintNavigation.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
            }
        }
    }

    static {
        boolean z = true;
        if (1 != SystemProperties.getInt("ro.debuggable", 0)) {
            z = false;
        }
        DEBUG = z;
    }

    FingerprintNavigation(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mHwCust = (HwCustFingerprintNavigation) HwCustUtils.createObj(HwCustFingerprintNavigation.class, new Object[]{this.mContext});
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        FingerprintNavigationInspector longpress_onscreenoffIns = new LongPressOnScreenOffInspector();
        FingerprintNavigationInspector launcherIns = new LauncherInspector();
        FingerprintNavigationInspector cameraIns = new CameraInspector();
        FingerprintNavigationInspector defaultIns = new DefaultInspector();
        this.mStartHomeInspector = new StartHomeInspector();
        this.mSingleTapInspector = new SingleTapInspector();
        this.mDoubleTapInspector = new DoubleTapInspector();
        this.mCollapsePanelsInspector = new CollapsePanelsInspector();
        this.mInCallInspector = new InCallInspector();
        this.mStopAlarmInspector = new StopAlarmInspector();
        this.mFingerprintDemoInspector = new FingerprintDemoInspector();
        this.mGalleryInspector = new GalleryInspector();
        this.mLongPressOnScreenOffInspector = longpress_onscreenoffIns;
        this.mLauncherInspector = launcherIns;
        this.mCameraInspector = cameraIns;
        this.mDefaultInspector = defaultIns;
        this.mGallerySlide = false;
        this.mFrontFingerprintNav = new FrontFingerprintNavigation(context);
    }

    public void showNotificationTips() {
    }

    public void systemRunning() {
        if (this.mFrontFingerprintNav != null) {
            this.mFrontFingerprintNav.systemRunning();
        }
        boolean z = true;
        this.mDeviceProvisioned = Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        if (Secure.getInt(this.mContext.getContentResolver(), FINGERPRINT_USED_FIRSTLY, 1) == 0) {
            z = false;
        }
        this.mFingerprintUsedFirstly = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SystemReady mDeviceProvisioned: ");
        stringBuilder.append(this.mDeviceProvisioned);
        stringBuilder.append(",mFingerprintUsedFirstly: ");
        stringBuilder.append(this.mFingerprintUsedFirstly);
        Log.d(str, stringBuilder.toString());
    }

    boolean dispatchUnhandledKey(InputEvent event, int policyFlags) {
        if (event instanceof KeyEvent) {
            KeyEvent kv = (KeyEvent) event;
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unhandled fingprint event=");
                stringBuilder.append(kv);
                stringBuilder.append(",fromFingerprint=");
                stringBuilder.append(kv.getFlags() & 2048);
                Log.d(str, stringBuilder.toString());
            }
            if ((kv.getFlags() & 2048) == 0) {
                return false;
            }
            if (this.mInCallInspector.probe(kv)) {
                this.mInCallInspector.handle(kv);
            } else if (this.mStopAlarmInspector.probe(kv)) {
                this.mStopAlarmInspector.handle(kv);
            } else if (this.mStartHomeInspector.probe(kv, true)) {
                this.mStartHomeInspector.handle(kv);
                return true;
            }
        }
        return false;
    }

    private boolean interceptSystemNavigationKeyAsGoogle(KeyEvent event) {
        int code = event.getKeyCode();
        int transCode = -1;
        if (this.mAccessibilityManager == null) {
            Log.e(TAG, "Accessibility is Null !");
            return false;
        }
        switch (code) {
            case 511:
                transCode = 280;
                break;
            case 512:
                transCode = 281;
                break;
            case 513:
                transCode = 282;
                break;
            case 514:
                transCode = 283;
                break;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("interceptSystemNavigation code:");
        stringBuilder.append(code);
        stringBuilder.append(" transCode:");
        stringBuilder.append(transCode);
        Log.d(str, stringBuilder.toString());
        if (-1 == transCode || !this.mAccessibilityManager.isEnabled() || !this.mAccessibilityManager.sendFingerprintGesture(transCode)) {
            return false;
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Accessibility consume transCode:");
        stringBuilder2.append(transCode);
        Log.d(str, stringBuilder2.toString());
        return true;
    }

    private void updateSwitchValue() {
        if (!this.mHasReadDB) {
            updateFingerNaviSwitchValue(true);
            boolean z = false;
            this.mFingerprintMarketDemoSwitch = System.getInt(this.mResolver, "fingerprint_market_demo_switch", 0) == 1;
            if (Secure.getIntForUser(this.mResolver, FINGERPRINT_GALLERY_SLIDE, 1, ActivityManager.getCurrentUser()) != 0) {
                z = true;
            }
            this.mGallerySlide = z;
            this.mHasReadDB = true;
        }
    }

    private boolean handleFingerTapEvent(KeyEvent kv) {
        if (kv != null) {
            int keyCode = kv.getKeyCode();
            String compName;
            if (keyCode == 66) {
                compName = getTopApp();
                if (compName == null || !compName.startsWith("com.android.gallery3d") || isAlarm()) {
                    if (isInCallUI()) {
                        this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
                    }
                } else if (kv.getAction() == 0) {
                    return true;
                } else {
                    sendKeyEvent(502);
                    return true;
                }
            } else if (keyCode != 501) {
                if (keyCode == WifiProCommonUtils.RESP_CODE_UNSTABLE) {
                    if (kv.getAction() == 0) {
                        return true;
                    }
                    handleSingleTapEvent(kv);
                    return true;
                }
            } else if (kv.getAction() == 0) {
                return true;
            } else {
                this.mHandler.removeCallbacks(this.mPendingCheckForSingleTap);
                this.mPendingCheckForSingleTap = null;
                if (DEBUG) {
                    compName = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("keycode is : ");
                    stringBuilder.append(keyCode);
                    stringBuilder.append(" sent to app");
                    Log.d(compName, stringBuilder.toString());
                }
                if (!isAlarm()) {
                    sendKeyEvent(keyCode);
                }
                return true;
            }
        }
        return false;
    }

    private void handleCustFingerEvent(InputEvent event) {
        boolean isCustHandled = this.mHwCust != null && this.mHwCust.handleFingerprintEvent(event);
        if (!isCustHandled) {
            if (this.mLongPressOnScreenOffInspector.probe(event)) {
                this.mLongPressOnScreenOffInspector.handle(event);
            } else if (this.mFingerprintDemoInspector.probe(event)) {
                this.mFingerprintDemoInspector.handle(event);
            } else if (this.mSingleTapInspector.probe(event)) {
                this.mSingleTapInspector.handle(event);
            } else if (this.mDoubleTapInspector.probe(event)) {
                this.mDoubleTapInspector.handle(event);
            } else if (this.mStopAlarmInspector.probe(event)) {
                this.mStopAlarmInspector.handle(event);
            } else if (this.mInCallInspector.probe(event)) {
                this.mInCallInspector.handle(event);
            } else if (this.mCameraInspector.probe(event)) {
                this.mCameraInspector.handle(event);
            } else if (this.mStartHomeInspector.probe(event, false)) {
                this.mStartHomeInspector.handle(event);
            } else if (this.mLauncherInspector.probe(event)) {
                this.mLauncherInspector.handle(event);
            } else if (this.mCollapsePanelsInspector.probe(event)) {
                this.mCollapsePanelsInspector.handle(event);
            } else if (this.mGalleryInspector.probe(event)) {
                this.mGalleryInspector.handle(event);
            } else if (this.mDefaultInspector.probe(event)) {
                this.mDefaultInspector.handle(event);
            }
        }
    }

    public boolean filterInputEvent(InputEvent event, int policyFlags) {
        if (this.mFingerPrintId < 0) {
            this.mFingerPrintId = SystemProperties.getInt("sys.fingerprint.deviceId", -1);
        }
        int fpdeviceId = -1;
        KeyEvent kv = null;
        if (event instanceof MotionEvent) {
            fpdeviceId = ((MotionEvent) event).getDeviceId();
        } else if (event instanceof KeyEvent) {
            kv = (KeyEvent) event;
            fpdeviceId = kv.getDeviceId();
        }
        if (fpdeviceId < 0 || fpdeviceId != this.mFingerPrintId) {
            return false;
        }
        if (HwPCUtils.isPcCastModeInServer() && kv != null && kv.getAction() == 0) {
            if (this.mWindowManagerInternal == null) {
                this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            }
            if (this.mWindowManagerInternal != null && HwPCUtils.isValidExtDisplayId(this.mWindowManagerInternal.getFocusedDisplayId())) {
                if (!HwPCUtils.enabledInPad()) {
                    this.mWindowManagerInternal.setFocusedDisplayId(0, "FingerprintNav");
                }
                return true;
            }
        }
        if (FRONT_FINGERPRINT_NAVIGATION && kv != null && !isVRMode() && this.mFrontFingerprintNav != null && this.mFrontFingerprintNav.handleFingerprintEvent(event)) {
            return true;
        }
        if (kv != null && !FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION && interceptSystemNavigationKeyAsGoogle(kv)) {
            return true;
        }
        if (SystemProperties.getBoolean("ro.config.fingerOnSmartKey", false) && needDropFingerprintEvent()) {
            Log.d(TAG, "drop fingerprintnavigation event!");
            return true;
        }
        updateSwitchValue();
        if (handleFingerTapEvent(kv)) {
            return true;
        }
        handleCustFingerEvent(event);
        event.recycle();
        return true;
    }

    private void handleSingleTapEvent(InputEvent event) {
        if (this.mHandler.hasCallbacks(this.mPendingCheckForSingleTap)) {
            this.mPendingCheckForSingleTap = null;
            this.mHandler.removeCallbacks(this.mPendingCheckForSingleTap);
            this.mDoubleTapInspector.handleDoubleTap();
            return;
        }
        this.mPendingCheckForSingleTap = new CheckForSingleTap(this, null);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkSingleTap, caller=");
            stringBuilder.append(Debug.getCallers(4));
            Log.d(str, stringBuilder.toString());
        }
        this.mHandler.postDelayed(this.mPendingCheckForSingleTap, 300);
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguard != null) {
            return keyguard.isKeyguardLocked();
        }
        return false;
    }

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) this.mContext.getSystemService("power");
        if (power != null) {
            return power.isScreenOn() ^ 1;
        }
        return false;
    }

    private boolean isInCallUIAndRinging() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        return "com.android.incallui/.InCallActivity".equals(getTopApp()) && telecomManager != null && telecomManager.isRinging();
    }

    private boolean isInCallUI() {
        return "com.android.incallui/.InCallActivity".equals(getTopApp());
    }

    boolean isAlarm() {
        return serviceIsRunning(this.mAlarmServiceCmp, this.mCurUser);
    }

    private boolean isLandscape() {
        return 2 == this.mContext.getResources().getConfiguration().orientation;
    }

    private boolean isCamera() {
        String pkgName = getTopApp();
        return pkgName != null && pkgName.startsWith(MemoryConstant.CAMERA_PACKAGE_NAME);
    }

    private boolean isFingerprintDemo() {
        String pkgName = getTopApp();
        return pkgName != null && pkgName.startsWith(FINGERPRINT_MARKET_DEMO_PKG);
    }

    private boolean isSpecialKey(InputEvent event, int code) {
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        KeyEvent ev = (KeyEvent) event;
        String str = TAG_FP;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("keycode is : ");
        stringBuilder.append(ev.getKeyCode());
        Log.i(str, stringBuilder.toString());
        if (ev.getKeyCode() == code) {
            return true;
        }
        return false;
    }

    private void broadCastToKeyguard(boolean validated) {
        Intent intent = new Intent("com.android.server.input.fpn");
        intent.putExtra("validated", validated);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendKeyEvent(int keycode) {
        int[] actions = new int[]{0, 1};
        KeyEvent ev = null;
        for (int keyEvent : actions) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, keyEvent, keycode, 0, 0, 6, 0, 2056, 257), 0);
        }
    }

    private String getTopApp() {
        String pkgName = ((ActivityManagerService) ServiceManager.getService("activity")).topAppName();
        if (DEBUG && pkgName != null) {
            String[] names = pkgName.split("/");
            String str = TAG_FP;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TopApp is ");
            stringBuilder.append(names[0]);
            Log.d(str, stringBuilder.toString());
        }
        return pkgName;
    }

    private boolean serviceIsRunning(ComponentName cmpName, int user) {
        return ((HwActivityManagerService) ServiceManager.getService("activity")).serviceIsRunning(cmpName, user);
    }

    private boolean statusBarObsecured() {
        boolean isStatusBarObsecured = ((HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class)).isStatusBarObsecured();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isStatusBarObsecured: ");
        stringBuilder.append(isStatusBarObsecured);
        Log.d(str, stringBuilder.toString());
        return isStatusBarObsecured;
    }

    private boolean focusedWinOverStatusBar() {
        HwPhoneWindowManager policy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        WindowState focusedWin = (WindowState) policy.getFocusedWindow();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("focusedWinOverStatusBar: ");
        stringBuilder.append(focusedWin != null ? policy.getWindowLayerFromTypeLw(focusedWin.getBaseType()) : 0);
        stringBuilder.append(", statusbarLayer=");
        stringBuilder.append(policy.getWindowLayerFromTypeLw(2000));
        Log.d(str, stringBuilder.toString());
        if (focusedWin == null || policy.getWindowLayerFromTypeLw(focusedWin.getBaseType()) <= policy.getWindowLayerFromTypeLw(2000)) {
            return false;
        }
        return true;
    }

    private void startVibrate() {
        Vibrator vb = (Vibrator) this.mContext.getSystemService("vibrator");
        Log.d(TAG, "startVibrate");
        if (vb.hasVibrator()) {
            Log.d(TAG, "real startVibrate");
            vb.vibrate(500);
        }
    }

    private void updateFingerNaviSwitchValue(boolean careReadDb) {
        int defaultValue = 1;
        if (FRONT_FINGERPRINT_NAVIGATION && !careReadDb) {
            defaultValue = 0;
        }
        boolean z = true;
        this.mInjectSlide = Secure.getIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, defaultValue, ActivityManager.getCurrentUser()) != 0;
        this.mInjectCamera = Secure.getIntForUser(this.mResolver, FINGERPRINT_CAMERA_SWITCH, defaultValue, ActivityManager.getCurrentUser()) != 0;
        this.mAnswerCall = Secure.getIntForUser(this.mResolver, FINGERPRINT_ANSWER_CALL, 0, ActivityManager.getCurrentUser()) != 0;
        this.mShowNotification = Secure.getIntForUser(this.mResolver, FINGERPRINT_SHOW_NOTIFICATION, 0, ActivityManager.getCurrentUser()) != 0;
        if (this.mHwCust != null && this.mHwCust.needCustNavigation()) {
            this.mBackToHome = this.mHwCust.getCustNeedValue(this.mResolver, FINGERPRINT_BACK_TO_HOME, 0, ActivityManager.getCurrentUser(), 0);
            this.mGoBack = this.mHwCust.getCustNeedValue(this.mResolver, FINGERPRINT_GO_BACK, 0, ActivityManager.getCurrentUser(), 0);
            this.mRecentApp = this.mHwCust.getCustNeedValue(this.mResolver, FINGERPRINT_RECENT_APP, 0, ActivityManager.getCurrentUser(), 0);
        }
        if (Secure.getIntForUser(this.mResolver, FINGERPRINT_STOP_ALARM, 0, ActivityManager.getCurrentUser()) == 0) {
            z = false;
        }
        this.mStopAlarm = z;
    }

    boolean isScenseExcluded() {
        String appName = getTopApp();
        return appName != null && (appName.equals("com.huawei.hidisk/.strongbox.ui.activity.StrongBoxVerifyPassActivity") || appName.equals("com.android.settings/.fingerprint.enrollment.FingerprintEnrollActivity") || appName.equals("com.android.settings/.fingerprint.FingerprintSettingsActivity") || appName.equals("com.android.settings/.ConfirmLockPassword") || appName.equals("com.android.settings/.password.ConfirmLockPassword") || !this.mDeviceProvisioned);
    }

    private boolean canShowTransientBar() {
        boolean okToShowTransientBar = ((AbsPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class)).okToShowTransientBar();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("canShowTransientBar=");
        stringBuilder.append(okToShowTransientBar);
        Log.d(str, stringBuilder.toString());
        return okToShowTransientBar;
    }

    private boolean topIsFullScreen() {
        boolean isTopIsFullscreen = ((AbsPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class)).isTopIsFullscreen();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isTopIsFullscreen=");
        stringBuilder.append(isTopIsFullscreen);
        Log.d(str, stringBuilder.toString());
        return isTopIsFullscreen;
    }

    private void requestTransientStatusBars() {
        ((AbsPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class)).requestTransientStatusBars();
    }

    private boolean isGallery() {
        String activityName = getTopApp();
        if (activityName != null) {
            return activityName.startsWith("com.android.gallery3d/com.huawei.gallery.app");
        }
        Log.d(TAG, "gallery name is null");
        return false;
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        if (this.mFrontFingerprintNav != null) {
            this.mFrontFingerprintNav.setCurrentUser(newUserId);
        }
        this.mCurUser = newUserId;
        this.mSettingsObserver.registerContentObserver(newUserId);
        this.mSettingsObserver.onChange(true);
    }

    private boolean needDropFingerprintEvent() {
        HwPhoneWindowManager policy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        if (policy != null) {
            return policy.getNeedDropFingerprintEvent();
        }
        return false;
    }

    private boolean isVRMode() {
        if (HwFrameworkFactory.getVRSystemServiceManager() != null) {
            return HwFrameworkFactory.getVRSystemServiceManager().isVRMode();
        }
        return false;
    }
}
