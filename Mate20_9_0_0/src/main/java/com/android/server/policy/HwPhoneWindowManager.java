package com.android.server.policy;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.app.SynchronousUserSwitchObserver;
import android.common.HwFrameworkFactory;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.cover.CoverManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.input.InputManager;
import android.hdm.HwDeviceManager;
import android.hwcontrol.HwWidgetFactory;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Trace;
import android.os.UserHandle;
import android.os.Vibrator;
import android.pc.IHwPCManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.provider.SettingsEx.Systemex;
import android.telecom.TelecomManager;
import android.telephony.MSimTelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwSlog;
import android.util.HwStylusUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.Toast;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ITelephony;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.HwActivityManagerService;
import com.android.server.am.HwPCMultiWindowPolicy;
import com.android.server.gesture.GestureNavConst;
import com.android.server.gesture.GestureNavManager;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.hidata.wavemapping.cons.WMStateCons;
import com.android.server.input.FrontFingerprintNavigation;
import com.android.server.input.HwInputManagerService.HwInputManagerServiceInternal;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.notch.HwNotchScreenWhiteConfig.NotchSwitchListener;
import com.android.server.pc.HwPCDataReporter;
import com.android.server.policy.PhoneWindowManager.UpdateRunnable;
import com.android.server.policy.SystemGesturesPointerEventListener.Callbacks;
import com.android.server.policy.WindowManagerPolicy.KeyguardDismissDoneListener;
import com.android.server.policy.WindowManagerPolicy.ScreenOnListener;
import com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.policy.keyguard.KeyguardStateMonitor.HwPCKeyguardShowingCallback;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.feature.StartWindowFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.security.tsmagent.server.HttpConnectionBase;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.HwGestureNavWhiteConfig;
import com.android.server.wm.HwStartWindowRecord;
import com.android.server.wm.IntelliServiceManager;
import com.android.server.wm.IntelliServiceManager.FaceRotationCallback;
import com.huawei.android.app.IGameObserver.Stub;
import com.huawei.android.gameassist.HwGameAssistManager;
import com.huawei.android.inputmethod.HwInputMethodManager;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.android.util.NoExtAPIException;
import com.huawei.forcerotation.HwForceRotationManager;
import huawei.android.aod.HwAodManager;
import huawei.android.app.IHwWindowCallback;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import huawei.android.hwutil.HwFullScreenDisplay;
import huawei.android.os.HwGeneralManager;
import huawei.android.provider.FingerSenseSettings;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.com.android.internal.widget.HwWidgetUtils;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.com.android.server.policy.HwFalseTouchMonitor;
import huawei.com.android.server.policy.HwScreenOnProximityLock;
import huawei.com.android.server.policy.fingersense.KnuckGestureSetting;
import huawei.com.android.server.policy.fingersense.SystemWideActionsListener;
import huawei.com.android.server.policy.stylus.StylusGestureListener;
import huawei.cust.HwCustUtils;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwPhoneWindowManager extends PhoneWindowManager implements TouchExplorationStateChangeListener {
    private static final String ACTION_HUAWEI_VASSISTANT_SERVICE = "com.huawei.ziri.model.MODELSERVICE";
    private static final int BACK_HOME_RECENT_DOUBLE_CLICK_TIMEOUT = 300;
    static final boolean DEBUG = false;
    static final boolean DEBUG_IMMERSION = false;
    private static final int DEFAULT_RESULT_VALUE = -2;
    private static final long DISABLE_VOLUMEDOWN_DOUBLE_CLICK_INTERVAl_MAX = 15000;
    private static final long DISABLE_VOLUMEDOWN_DOUBLE_CLICK_INTERVAl_MIN = 1500;
    static final String DROP_SMARTKEY_ACTIVITY = "drop_smartkey_activity";
    private static final int EVENT_DURING_MIN_TIME = 500;
    static final String FINGERPRINT_ANSWER_CALL = "fp_answer_call";
    static final String FINGERPRINT_CAMERA_SWITCH = "fp_take_photo";
    private static final int FINGERPRINT_HARDWARE_OPTICAL = 1;
    static final String FINGERPRINT_STOP_ALARM = "fp_stop_alarm";
    private static final int FLOATING_MASK = Integer.MIN_VALUE;
    private static final int FORCE_STATUS_BAR = 1;
    public static final String FRONT_FINGERPRINT_BUTTON_LIGHT_MODE = "button_light_mode";
    public static final String FRONT_FINGERPRINT_SWAP_KEY_POSITION = "swap_key_position";
    public static final String HAPTIC_FEEDBACK_TRIKEY_SETTINGS = "physic_navi_haptic_feedback_enabled";
    private static final String HUAWEI_RAPIDCAPTURE_START_MODE = "com.huawei.RapidCapture";
    private static final String HUAWEI_SCREENRECORDER_ACTION = "com.huawei.screenrecorder.Start";
    private static final String HUAWEI_SCREENRECORDER_PACKAGE = "com.huawei.screenrecorder";
    private static final String HUAWEI_SCREENRECORDER_START_MODE = "com.huawei.screenrecorder.ScreenRecordService";
    private static final String HUAWEI_SMARTKEY_PACKAGE = "com.huawei.smartkey";
    private static final String HUAWEI_SMARTKEY_SERVICE = "com.android.huawei.smartkey";
    private static final String HUAWEI_VASSISTANT_EXTRA_START_MODE = "com.huawei.vassistant.extra.SERVICE_START_MODE";
    private static final String HUAWEI_VASSISTANT_PACKAGE = "com.huawei.vassistant";
    private static final String HUAWEI_VOICE_DEBUG_BETACLUB = "com.huawei.betaclub";
    private static final String HUAWEI_VOICE_SOUNDTRIGGER_ACTIVITY = "com.mmc.soundtrigger.MainActivity";
    private static final String HUAWEI_VOICE_SOUNDTRIGGER_BROADCAST = "com.mmc.SOUNDTRIGGER";
    private static final String HUAWEI_VOICE_SOUNDTRIGGER_PACKAGE = "com.mmc.soundtrigger";
    private static final boolean HWRIDEMODE_FEATURE_SUPPORTED = SystemProperties.getBoolean("ro.config.ride_mode", false);
    private static final boolean IS_LONG_HOME_VASSITANT = SystemProperties.getBoolean("ro.hw.long.home.vassistant", true);
    private static final List KEYCODE_NOT_FOR_CLOUD = Arrays.asList(new Integer[]{Integer.valueOf(4), Integer.valueOf(3), Integer.valueOf(25), Integer.valueOf(24), Integer.valueOf(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE), Integer.valueOf(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE)});
    private static final String KEY_HWOUC_KEYGUARD_VIEW_ON_TOP = "hwouc_keyguard_view_on_top";
    private static final String KEY_TOUCH_DISABLE_MODE = "touch_disable_mode";
    private static final int MAX_POWERKEY_COUNTDOWN = 5;
    private static final int MSG_BUTTON_LIGHT_TIMEOUT = 4099;
    private static final int MSG_FINGERSENSE_DISABLE = 102;
    private static final int MSG_FINGERSENSE_ENABLE = 101;
    private static final int MSG_FREEZE_POWER_KEY = 4103;
    private static final int MSG_NAVIBAR_DISABLE = 104;
    private static final int MSG_NAVIBAR_ENABLE = 103;
    private static final int MSG_NOTIFY_FINGER_OPTICAL = 4100;
    private static final int MSG_TRIKEY_BACK_LONG_PRESS = 4097;
    private static final int MSG_TRIKEY_RECENT_LONG_PRESS = 4098;
    private static final int NOTCH_ROUND_CORNER_CODE = 8002;
    private static final int NOTCH_ROUND_CORNER_HIDE = 0;
    private static final int NOTCH_ROUND_CORNER_SHOW = 1;
    private static final int NOTCH_STATUS_BAR_BLACK = 1;
    private static final int NOTCH_STATUS_BAR_DEFAULT = 0;
    private static final String NOTEEDITOR_ACTIVITY_NAME = "com.example.android.notepad/com.huawei.android.notepad.views.SketchActivity";
    private static final String PKG_CALCULATOR = "com.android.calculator2";
    private static final String PKG_CAMERA = "com.huawei.camera";
    private static final String PKG_GALLERY = "com.android.gallery3d";
    private static final String PKG_NAME_EMERGENCY = "com.android.emergency";
    private static final String PKG_SCANNER = "com.huawei.scanner";
    private static final String PKG_SOUNDRECORDER = "com.android.soundrecorder";
    private static final long POWER_SOS_MISTOUCH_THRESHOLD = 300;
    private static final long POWER_SOS_TIMEOUT = 500;
    private static final long SCREENRECORDER_DEBOUNCE_DELAY_MILLIS = 150;
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SINGLE_HAND_STATE = 1989;
    private static final String SMARTKEY_CLICK = "Click";
    private static final String SMARTKEY_DCLICK = "DoubleClick";
    private static final long SMARTKEY_DOUBLE_CLICK_TIMEOUT = 400;
    private static final long SMARTKEY_LONG_PRESS_TIMEOUT = 500;
    private static final String SMARTKEY_LP = "LongPress";
    private static final String SMARTKEY_TAG = "command";
    private static final int START_MODE_QUICK_START_CALL = 2;
    static final int START_MODE_VOICE_WAKEUP_ONE_SHOT = 4;
    private static final long SYSTRACELOG_DEBOUNCE_DELAY_MILLIS = 150;
    private static final long SYSTRACELOG_FINGERPRINT_EFFECT_DELAY = 750;
    static final String TAG = "HwPhoneWindowManager";
    private static final boolean TOUCHPLUS_FORCE_VIBRATION = true;
    private static final int TOUCHPLUS_SETTINGS_DISABLED = 0;
    private static final int TOUCHPLUS_SETTINGS_ENABLED = 1;
    private static final String TOUCHPLUS_SETTINGS_VIBRATION = "hw_membrane_touch_vibrate_enabled";
    private static final long TOUCH_DISABLE_DEBOUNCE_DELAY_MILLIS = 150;
    private static final int TOUCH_EXPLR_NAVIGATION_BAR_COLOR = -16777216;
    private static final int TOUCH_EXPLR_STATUS_BAR_COLOR = -16777216;
    private static final long TOUCH_SPINNING_DELAY_MILLIS = 2000;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final int UNDEFINED_TYPE = -1;
    private static final String UPDATE_GESTURE_NAV_ACTION = "huawei.intent.action.FWK_UPDATE_GESTURE_NAV_ACTION";
    private static final String UPDATE_NOTCH_SCREEN_ACTION = "huawei.intent.action.FWK_UPDATE_NOTCH_SCREEN_ACTION";
    private static final String UPDATE_NOTCH_SCREEN_PER = "com.huawei.systemmanager.permission.ACCESS_INTERFACE";
    private static final String VIBRATE_ON_TOUCH = "vibrate_on_touch";
    private static final int VIBRATOR_LONG_PRESS_FOR_FRONT_FP = SystemProperties.getInt("ro.config.trikey_vibrate_press", 16);
    private static final int VIBRATOR_SHORT_PRESS_FOR_FRONT_FP = SystemProperties.getInt("ro.config.trikey_vibrate_touch", 8);
    private static String VOICE_ASSISTANT_ACTION = "com.huawei.action.VOICE_ASSISTANT";
    private static final long VOLUMEDOWN_DOUBLE_CLICK_TIMEOUT = 400;
    private static final long VOLUMEDOWN_LONG_PRESS_TIMEOUT = 500;
    private static final int WAKEUP_NOTEEDITOR_TIMEOUT = 500;
    private static boolean isSwich = false;
    private static boolean mCustBeInit = false;
    private static boolean mCustUsed = false;
    static final boolean mIsHwNaviBar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    private static final boolean mSupportGameAssist = (SystemProperties.getInt("ro.config.gameassist", 0) == 1);
    private static boolean mTplusEnabled = SystemProperties.getBoolean("ro.config.hw_touchplus_enabled", false);
    private static int[] mUnableWakeKey;
    private static boolean mUsingHwNavibar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    private boolean DEBUG_SMARTKEY = false;
    private int TRIKEY_NAVI_DEFAULT_MODE = -1;
    private AlertDialog alertDialog;
    FingerprintActionsListener fingerprintActionsListener;
    private HwNotchScreenWhiteConfig hwNotchScreenWhiteConfig;
    private boolean isAppWindow = false;
    private boolean isFingerAnswerPhoneOn = false;
    private boolean isFingerShotCameraOn = false;
    private boolean isFingerStopAlarmOn = false;
    boolean isHomeAndDBothDown = false;
    boolean isHomeAndEBothDown = false;
    boolean isHomeAndLBothDown = false;
    boolean isHomePressDown = false;
    private boolean isHwOUCKeyguardViewOnTop = false;
    private boolean isNavibarHide;
    private boolean isNotchTemp;
    private boolean isTouchDownUpLeftDoubleClick;
    private boolean isTouchDownUpRightDoubleClick;
    private boolean isVibrateImplemented = SystemProperties.getBoolean("ro.config.touch_vibrate", false);
    private boolean isVoiceRecognitionActive;
    private int lastDensityDpi = -1;
    private int mActionBarHeight;
    private HwActivityManagerService mAms;
    private final IZrHung mAppEyeBackKey = HwFrameworkFactory.getZrHung("appeye_backkey");
    private final IZrHung mAppEyeHomeKey = HwFrameworkFactory.getZrHung("appeye_homekey");
    private boolean mBackKeyPress = false;
    private long mBackKeyPressTime = 0;
    private Light mBackLight = null;
    volatile boolean mBackTrikeyHandled;
    private int mBarVisibility = 1;
    boolean mBooted = false;
    private WakeLock mBroadcastWakeLock;
    private Light mButtonLight = null;
    private int mButtonLightMode = 1;
    private final Runnable mCancleInterceptFingerprintEvent = new Runnable() {
        public void run() {
            HwPhoneWindowManager.this.mNeedDropFingerprintEvent = false;
        }
    };
    private CoverManager mCoverManager = null;
    private boolean mCoverOpen = true;
    private int mCurUser;
    HwCustPhoneWindowManager mCust = ((HwCustPhoneWindowManager) HwCustUtils.createObj(HwCustPhoneWindowManager.class, new Object[0]));
    int mDesiredRotation = -1;
    private boolean mDeviceProvisioned = false;
    private boolean mEnableKeyInCurrentFgGameApp;
    private SystemGesturesPointerEventListener mExternalSystemGestures;
    FaceRotationCallback mFaceRotationCallback = new FaceRotationCallback() {
        public void onEvent(int faceRotation) {
            if (faceRotation == -2) {
                HwPhoneWindowManager.this.mHandler.post(new UpdateRunnable(HwPhoneWindowManager.this, HwPhoneWindowManager.this.mScreenRotation));
            } else {
                HwPhoneWindowManager.this.mHandler.post(new UpdateRunnable(HwPhoneWindowManager.this, faceRotation));
            }
        }
    };
    private HwFalseTouchMonitor mFalseTouchMonitor = null;
    private int mFingerPrintId = -1;
    boolean mFingerSenseEnabled = true;
    private int mFingerprintHardwareType;
    private ContentObserver mFingerprintObserver;
    private boolean mFirstSetCornerDefault = true;
    private boolean mFirstSetCornerInLandNoNotch = true;
    private boolean mFirstSetCornerInLandNotch = true;
    private boolean mFirstSetCornerInPort = true;
    private boolean mFirstSetCornerInReversePortait = true;
    private GestureNavManager mGestureNavManager;
    private final Runnable mHandleVolumeDownKey = new Runnable() {
        public void run() {
            if (HwPhoneWindowManager.this.isMusicActive()) {
                HwPhoneWindowManager.this.handleVolumeKey(3, 25);
            }
        }
    };
    private Handler mHandlerEx;
    private boolean mHapticEnabled = true;
    private boolean mHeadless;
    private boolean mHintShown;
    private ContentObserver mHwOUCObserver;
    private HwScreenOnProximityLock mHwScreenOnProximityLock;
    public IHwWindowCallback mIHwWindowCallback;
    private boolean mInputMethodWindowVisible;
    private boolean mIsForceSetStatusBar = false;
    private boolean mIsFreezePowerkey = false;
    private boolean mIsHasActionBar;
    protected boolean mIsImmersiveMode = false;
    boolean mIsNavibarAlignLeftWhenLand;
    private boolean mIsProximity = false;
    private boolean mIsRestoreStatusBar = false;
    private boolean mIsSmartKeyDoubleClick = false;
    private boolean mIsSmartKeyTripleOrMoreClick = false;
    private boolean mIsTouchExplrEnabled;
    private String[] mKeyguardShortcutApps = new String[]{"com.huawei.camera", PKG_GALLERY, PKG_SCANNER};
    private WindowState mLastColorWin;
    private String mLastFgPackageName;
    private int mLastIsEmuiLightStyle;
    private int mLastIsEmuiStyle;
    private boolean mLastKeyDownDropped;
    private int mLastKeyDownKeyCode;
    private long mLastKeyDownTime;
    private long mLastKeyPointerTime = 0;
    private int mLastNavigationBarColor;
    private long mLastPowerKeyDownTime = 0;
    private long mLastSmartKeyDownTime;
    private long mLastStartVassistantServiceTime;
    private int mLastStatusBarColor;
    private long mLastTouchDownUpLeftKeyDownTime;
    private long mLastTouchDownUpRightKeyDownTime;
    private long mLastVolumeDownKeyDownTime;
    private long mLastVolumeKeyDownTime = 0;
    private long mLastWakeupTime = 0;
    WindowState mLighterDrawView;
    private ProximitySensorListener mListener = null;
    private PointerEventListener mLockScreenBuildInDisplayListener;
    private PointerEventListener mLockScreenListener;
    private String[] mLsKeyguardShortcutApps = new String[]{PKG_SOUNDRECORDER, PKG_CALCULATOR};
    private boolean mMenuClickedOnlyOnce = false;
    private boolean mMenuKeyPress = false;
    private long mMenuKeyPressTime = 0;
    boolean mNaviBarStateInited = false;
    boolean mNavibarEnabled = false;
    final BarController mNavigationBarControllerExternal = new BarController("NavigationBarExternal", 134217728, 536870912, FLOATING_MASK, 2, 134217728, 32768);
    protected WindowState mNavigationBarExternal = null;
    int mNavigationBarHeightExternal = 0;
    int[] mNavigationBarHeightForRotationMax = new int[4];
    int[] mNavigationBarHeightForRotationMin = new int[4];
    protected NavigationBarPolicy mNavigationBarPolicy = null;
    int[] mNavigationBarWidthForRotationMax = new int[4];
    int[] mNavigationBarWidthForRotationMin = new int[4];
    private boolean mNeedDropFingerprintEvent = false;
    private NotchSwitchListener mNotchSwitchListener;
    OverscanTimeout mOverscanTimeout = new OverscanTimeout();
    private int mPowerKeyCount = 0;
    private boolean mPowerKeyDisTouch;
    private long mPowerKeyDisTouchTime;
    private PowerManager mPowerManager;
    final Runnable mProximitySensorTimeoutRunnable = new Runnable() {
        public void run() {
            Log.i(HwPhoneWindowManager.TAG, "mProximitySensorTimeout, unRegisterListener");
            HwPhoneWindowManager.this.turnOffSensorListener();
        }
    };
    volatile boolean mRecentTrikeyHandled;
    private ContentResolver mResolver;
    private boolean mScreenOnForFalseTouch;
    private long mScreenRecorderPowerKeyTime;
    private boolean mScreenRecorderPowerKeyTriggered;
    private final Runnable mScreenRecorderRunnable = new Runnable() {
        public void run() {
            Intent intent = new Intent();
            intent.setAction(HwPhoneWindowManager.HUAWEI_SCREENRECORDER_ACTION);
            intent.setClassName(HwPhoneWindowManager.HUAWEI_SCREENRECORDER_PACKAGE, HwPhoneWindowManager.HUAWEI_SCREENRECORDER_START_MODE);
            try {
                HwPhoneWindowManager.this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
            } catch (Exception e) {
                String str = HwPhoneWindowManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to start screenrecorder service: ");
                stringBuilder.append(intent);
                Slog.e(str, stringBuilder.toString(), e);
            }
            Log.d(HwPhoneWindowManager.TAG, "start screen recorder service");
        }
    };
    private boolean mScreenRecorderVolumeDownKeyTriggered;
    private boolean mScreenRecorderVolumeUpKeyConsumed;
    private long mScreenRecorderVolumeUpKeyTime;
    private boolean mScreenRecorderVolumeUpKeyTriggered;
    private int mSecondToLastKeyDownKeyCode;
    private long mSecondToLastKeyDownTime;
    private SensorManager mSensorManager = null;
    private boolean mSensorRegisted = false;
    final Object mServiceAquireLock = new Object();
    private SettingsObserver mSettingsObserver;
    private final Runnable mSmartKeyClick = new Runnable() {
        public void run() {
            HwPhoneWindowManager.this.cancelSmartKeyClick();
            HwPhoneWindowManager.this.notifySmartKeyEvent(HwPhoneWindowManager.SMARTKEY_CLICK);
        }
    };
    private final Runnable mSmartKeyLongPressed = new Runnable() {
        public void run() {
            HwPhoneWindowManager.this.cancelSmartKeyLongPressed();
            HwPhoneWindowManager.this.notifySmartKeyEvent(HwPhoneWindowManager.SMARTKEY_LP);
        }
    };
    boolean mStatuBarObsecured;
    IStatusBarService mStatusBarService;
    private StylusGestureListener mStylusGestureListener = null;
    private StylusGestureListener mStylusGestureListener4PCMode = null;
    private boolean mSystraceLogCompleted = true;
    private long mSystraceLogFingerPrintTime = 0;
    private boolean mSystraceLogPowerKeyTriggered = false;
    private final Runnable mSystraceLogRunnable = new Runnable() {
        public void run() {
            HwPhoneWindowManager.this.systraceLogDialogThread = new HandlerThread("SystraceLogDialog");
            HwPhoneWindowManager.this.systraceLogDialogThread.start();
            HwPhoneWindowManager.this.systraceLogDialogHandler = new Handler(HwPhoneWindowManager.this.systraceLogDialogThread.getLooper()) {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == 0) {
                        HwPhoneWindowManager.this.alertDialog = new Builder(HwPhoneWindowManager.this.mContext).setTitle(17039380).setMessage(HwPhoneWindowManager.this.mContext.getResources().getQuantityString(34406405, 10, new Object[]{Integer.valueOf(10)})).setCancelable(false).create();
                        HwPhoneWindowManager.this.alertDialog.getWindow().setType(2003);
                        HwPhoneWindowManager.this.alertDialog.getWindow().setFlags(128, 128);
                        HwPhoneWindowManager.this.alertDialog.show();
                    } else if (msg.what <= 0 || msg.what >= 10) {
                        HwPhoneWindowManager.this.alertDialog.dismiss();
                    } else {
                        HwPhoneWindowManager.this.alertDialog.setMessage(HwPhoneWindowManager.this.mContext.getResources().getQuantityString(34406405, 10 - msg.what, new Object[]{Integer.valueOf(10 - msg.what)}));
                        TelecomManager telecomManager = (TelecomManager) HwPhoneWindowManager.this.mContext.getSystemService("telecom");
                        if (telecomManager != null && telecomManager.isRinging()) {
                            HwPhoneWindowManager.this.alertDialog.dismiss();
                        }
                    }
                }
            };
            HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(0);
            new Thread(new Runnable() {
                public void run() {
                    int i = 1;
                    while (!HwPhoneWindowManager.this.mSystraceLogCompleted && i < 11) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.w(HwPhoneWindowManager.TAG, "systrace log not completed,interrupted");
                        }
                        if (!(HwPhoneWindowManager.this.systraceLogDialogHandler == null || HwPhoneWindowManager.this.mSystraceLogCompleted)) {
                            HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(i);
                        }
                        i++;
                    }
                }
            }).start();
            new Thread(new Runnable() {
                public void run() {
                    String str;
                    StringBuilder stringBuilder;
                    IBinder sJankService = ServiceManager.getService("jank");
                    if (sJankService != null) {
                        try {
                            Log.d(HwPhoneWindowManager.TAG, "sJankService is not null");
                            Parcel data = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            data.writeInterfaceToken("android.os.IJankManager");
                            sJankService.transact(2, data, reply, 0);
                            int result = reply.readInt();
                            String str2 = HwPhoneWindowManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("sJankService.transact result = ");
                            stringBuilder2.append(result);
                            Log.d(str2, stringBuilder2.toString());
                        } catch (RemoteException e) {
                            String str3 = HwPhoneWindowManager.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("sJankService.transact remote exception:");
                            stringBuilder3.append(e.getMessage());
                            Log.e(str3, stringBuilder3.toString());
                        } catch (Throwable th) {
                            HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(10);
                        }
                    }
                    HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(10);
                    try {
                        Thread.sleep(1000);
                        HwPhoneWindowManager.this.mSystraceLogCompleted = true;
                        HwPhoneWindowManager.this.systraceLogDialogThread.quitSafely();
                        str = HwPhoneWindowManager.TAG;
                        stringBuilder = new StringBuilder();
                    } catch (InterruptedException e2) {
                        Log.w(HwPhoneWindowManager.TAG, "sJankService transact not completed,interrupted");
                        HwPhoneWindowManager.this.mSystraceLogCompleted = true;
                        HwPhoneWindowManager.this.systraceLogDialogThread.quitSafely();
                        str = HwPhoneWindowManager.TAG;
                        stringBuilder = new StringBuilder();
                    } catch (Throwable th2) {
                        HwPhoneWindowManager.this.mSystraceLogCompleted = true;
                        HwPhoneWindowManager.this.systraceLogDialogThread.quitSafely();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("has quit the systraceLogDialogThread");
                        stringBuilder.append(HwPhoneWindowManager.this.systraceLogDialogThread.getId());
                        Log.d(HwPhoneWindowManager.TAG, stringBuilder.toString());
                    }
                    stringBuilder.append("has quit the systraceLogDialogThread");
                    stringBuilder.append(HwPhoneWindowManager.this.systraceLogDialogThread.getId());
                    Log.d(str, stringBuilder.toString());
                }
            }).start();
        }
    };
    private boolean mSystraceLogVolumeDownKeyTriggered = false;
    private boolean mSystraceLogVolumeUpKeyConsumed = false;
    private long mSystraceLogVolumeUpKeyTime = 0;
    private boolean mSystraceLogVolumeUpKeyTriggered = false;
    private TouchCountPolicy mTouchCountPolicy = new TouchCountPolicy();
    private int mTouchDownUpLeftConsumeCount;
    private int mTouchDownUpRightConsumeCount;
    private int mTrikeyNaviMode = -1;
    private SystemVibrator mVibrator;
    private boolean mVolumeDownKeyDisTouch;
    private final Runnable mVolumeDownLongPressed = new Runnable() {
        public void run() {
            HwPhoneWindowManager.this.cancelVolumeDownKeyPressed();
            if ((!HwPhoneWindowManager.this.mIsProximity && HwPhoneWindowManager.this.mSensorRegisted) || !HwPhoneWindowManager.this.mSensorRegisted) {
                HwPhoneWindowManager.this.notifyVassistantService("start", 2, null);
            }
            HwPhoneWindowManager.this.turnOffSensorListener();
            HwPhoneWindowManager.this.isVoiceRecognitionActive = true;
            HwPhoneWindowManager.this.mLastStartVassistantServiceTime = SystemClock.uptimeMillis();
        }
    };
    private WakeLock mVolumeDownWakeLock;
    private boolean mVolumeUpKeyConsumedByDisTouch;
    private boolean mVolumeUpKeyDisTouch;
    private long mVolumeUpKeyDisTouchTime;
    private BroadcastReceiver mWhitelistReceived = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str;
            if (intent == null || context == null || intent.getAction() == null) {
                str = HwPhoneWindowManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("intent is ");
                stringBuilder.append(intent);
                stringBuilder.append("context is ");
                stringBuilder.append(context);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            String str2;
            StringBuilder stringBuilder2;
            if (PhoneWindowManager.IS_NOTCH_PROP && HwPhoneWindowManager.UPDATE_NOTCH_SCREEN_ACTION.equals(intent.getAction())) {
                str = intent.getStringExtra("uri");
                str2 = HwPhoneWindowManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fileName:");
                stringBuilder2.append(str);
                Slog.i(str2, stringBuilder2.toString());
                if (str != null) {
                    HwNotchScreenWhiteConfig.getInstance().updateWhitelistByHot(context, str);
                }
            }
            if (HwPhoneWindowManager.UPDATE_GESTURE_NAV_ACTION.equals(intent.getAction())) {
                str = intent.getStringExtra("uri");
                str2 = HwPhoneWindowManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fileName:");
                stringBuilder2.append(str);
                Slog.i(str2, stringBuilder2.toString());
                if (str != null) {
                    HwGestureNavWhiteConfig.getInstance().updateWhitelistByHot(context, str);
                }
            }
        }
    };
    private HashSet<String> needDropSmartKeyActivities = new HashSet();
    SystemWideActionsListener systemWideActionsListener;
    private Handler systraceLogDialogHandler;
    private HandlerThread systraceLogDialogThread;

    class OverscanTimeout implements Runnable {
        OverscanTimeout() {
        }

        public void run() {
            Slog.i(HwPhoneWindowManager.TAG, "OverscanTimeout run");
            Global.putString(HwPhoneWindowManager.this.mContext.getContentResolver(), "single_hand_mode", "");
        }
    }

    private class PolicyHandlerEx extends Handler {
        private PolicyHandlerEx() {
        }

        /* synthetic */ PolicyHandlerEx(HwPhoneWindowManager x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != HwPhoneWindowManager.MSG_FREEZE_POWER_KEY) {
                switch (i) {
                    case 101:
                        HwPhoneWindowManager.this.enableSystemWideActions();
                        return;
                    case 102:
                        HwPhoneWindowManager.this.disableSystemWideActions();
                        return;
                    case 103:
                        HwPhoneWindowManager.this.enableFingerPrintActions();
                        return;
                    case 104:
                        HwPhoneWindowManager.this.disableFingerPrintActions();
                        return;
                    default:
                        switch (i) {
                            case HwPhoneWindowManager.MSG_TRIKEY_BACK_LONG_PRESS /*4097*/:
                                HwPhoneWindowManager.this.mBackTrikeyHandled = true;
                                if (HwPhoneWindowManager.this.mTrikeyNaviMode == 1) {
                                    HwPhoneWindowManager.this.startHwVibrate(HwPhoneWindowManager.VIBRATOR_LONG_PRESS_FOR_FRONT_FP);
                                    Log.i(HwPhoneWindowManager.TAG, "LEFT->RECENT; RIGHT->BACK, handle longpress with recentTrikey and toggleSplitScreen");
                                    ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).toggleSplitScreen();
                                    return;
                                } else if (HwPhoneWindowManager.this.mTrikeyNaviMode == 0) {
                                    Log.i(HwPhoneWindowManager.TAG, "LEFT->BACK; RIGHT->RECENT, handle longpress with backTrikey and unlockScreenPinningTest");
                                    HwPhoneWindowManager.this.unlockScreenPinningTest();
                                    return;
                                } else {
                                    return;
                                }
                            case HwPhoneWindowManager.MSG_TRIKEY_RECENT_LONG_PRESS /*4098*/:
                                HwPhoneWindowManager.this.mRecentTrikeyHandled = true;
                                if (HwPhoneWindowManager.this.mTrikeyNaviMode == 0) {
                                    HwPhoneWindowManager.this.startHwVibrate(HwPhoneWindowManager.VIBRATOR_LONG_PRESS_FOR_FRONT_FP);
                                    Log.i(HwPhoneWindowManager.TAG, "LEFT->BACK; RIGHT->RECENT, handle longpress with recentTrikey and toggleSplitScreen");
                                    ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).toggleSplitScreen();
                                    return;
                                } else if (HwPhoneWindowManager.this.mTrikeyNaviMode == 1) {
                                    Log.i(HwPhoneWindowManager.TAG, "LEFT->RECENT; RIGHT->BACK, handle longpress with backTrikey and unlockScreenPinningTest");
                                    HwPhoneWindowManager.this.unlockScreenPinningTest();
                                    return;
                                } else {
                                    return;
                                }
                            case HwPhoneWindowManager.MSG_BUTTON_LIGHT_TIMEOUT /*4099*/:
                                if (HwPhoneWindowManager.this.mButtonLight == null) {
                                    return;
                                }
                                if (HwPhoneWindowManager.this.mPowerManager == null || !HwPhoneWindowManager.this.mPowerManager.isScreenOn()) {
                                    HwPhoneWindowManager.this.setButtonLightTimeout(false);
                                    return;
                                }
                                HwPhoneWindowManager.this.mButtonLight.setBrightness(0);
                                HwPhoneWindowManager.this.setButtonLightTimeout(true);
                                return;
                            case HwPhoneWindowManager.MSG_NOTIFY_FINGER_OPTICAL /*4100*/:
                                HwPhoneWindowManager.this.notifyFingerOptical();
                                return;
                            default:
                                return;
                        }
                }
            }
            Log.d(HwPhoneWindowManager.TAG, "Emergency power FreezePowerkey timeout.");
            HwPhoneWindowManager.this.mIsFreezePowerkey = false;
        }
    }

    private class ProximitySensorListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent arg0) {
            float[] its = arg0.values;
            if (its != null && arg0.sensor.getType() == 8 && its.length > 0) {
                String str = HwPhoneWindowManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sensor value: its[0] = ");
                boolean z = false;
                stringBuilder.append(its[0]);
                Log.i(str, stringBuilder.toString());
                HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                if (its[0] >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && its[0] < HwPhoneWindowManager.TYPICAL_PROXIMITY_THRESHOLD) {
                    z = true;
                }
                hwPhoneWindowManager.mIsProximity = z;
            }
        }
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        private ScreenBroadcastReceiver() {
        }

        /* synthetic */ ScreenBroadcastReceiver(HwPhoneWindowManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                HwPhoneWindowManager.this.sendLightTimeoutMsg();
            }
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
            boolean z = true;
            HwPhoneWindowManager.this.mDeviceProvisioned = Secure.getIntForUser(HwPhoneWindowManager.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            HwPhoneWindowManager.this.mTrikeyNaviMode = System.getIntForUser(HwPhoneWindowManager.this.mResolver, "swap_key_position", HwPhoneWindowManager.this.TRIKEY_NAVI_DEFAULT_MODE, ActivityManager.getCurrentUser());
            HwPhoneWindowManager.this.mButtonLightMode = System.getIntForUser(HwPhoneWindowManager.this.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
            if (System.getIntForUser(HwPhoneWindowManager.this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) == 0) {
                z = false;
            }
            HwPhoneWindowManager.this.mHapticEnabled = z;
        }

        public void registerContentObserver(int userId) {
            HwPhoneWindowManager.this.mResolver.registerContentObserver(System.getUriFor("swap_key_position"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(System.getUriFor("device_provisioned"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(System.getUriFor("button_light_mode"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(System.getUriFor("physic_navi_haptic_feedback_enabled"), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            boolean z = true;
            HwPhoneWindowManager.this.mDeviceProvisioned = Secure.getIntForUser(HwPhoneWindowManager.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            HwPhoneWindowManager.this.mTrikeyNaviMode = System.getIntForUser(HwPhoneWindowManager.this.mResolver, "swap_key_position", HwPhoneWindowManager.this.TRIKEY_NAVI_DEFAULT_MODE, ActivityManager.getCurrentUser());
            HwPhoneWindowManager.this.mButtonLightMode = System.getIntForUser(HwPhoneWindowManager.this.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
            HwPhoneWindowManager.this.resetButtonLightStatus();
            String str = HwPhoneWindowManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mTrikeyNaviMode is:");
            stringBuilder.append(HwPhoneWindowManager.this.mTrikeyNaviMode);
            stringBuilder.append(" mButtonLightMode is:");
            stringBuilder.append(HwPhoneWindowManager.this.mButtonLightMode);
            Slog.i(str, stringBuilder.toString());
            HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
            if (System.getIntForUser(HwPhoneWindowManager.this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) == 0) {
                z = false;
            }
            hwPhoneWindowManager.mHapticEnabled = z;
        }
    }

    public void systemReady() {
        super.systemReady();
        this.mHandler.post(new Runnable() {
            public void run() {
                HwPhoneWindowManager.this.initQuickcall();
            }
        });
        if (IS_NOTCH_PROP) {
            this.hwNotchScreenWhiteConfig = HwNotchScreenWhiteConfig.getInstance();
            HwFullScreenDisplay.setNotchHeight(this.mNotchPropSize);
        }
        this.mHwScreenOnProximityLock = new HwScreenOnProximityLock(this.mContext, this, this.mWindowManagerFuncs);
        if (mIsHwNaviBar) {
            this.mNavigationBarPolicy = new NavigationBarPolicy(this.mContext, this);
            this.mWindowManagerFuncs.registerPointerEventListener(new PointerEventListener() {
                public void onPointerEvent(MotionEvent motionEvent) {
                    if (HwPhoneWindowManager.this.mNavigationBarPolicy != null) {
                        HwPhoneWindowManager.this.mNavigationBarPolicy.addPointerEvent(motionEvent);
                    }
                }
            });
        }
        if (SystemProperties.getBoolean("ro.config.hw_easywakeup", false) && this.mSystemReady) {
            EasyWakeUpManager mWakeUpManager = EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate);
            ServiceManager.addService("easywakeup", mWakeUpManager);
            mWakeUpManager.saveTouchPointNodePath();
        }
        if (this.mListener == null) {
            this.mListener = new ProximitySensorListener();
        }
        this.mResolver = this.mContext.getContentResolver();
        this.TRIKEY_NAVI_DEFAULT_MODE = FrontFingerPrintSettings.getDefaultNaviMode();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mVibrator = (SystemVibrator) ((Vibrator) this.mContext.getSystemService("vibrator"));
        if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION && FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
            LightsManager lights = (LightsManager) LocalServices.getService(LightsManager.class);
            this.mButtonLight = lights.getLight(2);
            this.mBackLight = lights.getLight(0);
            if (this.mContext != null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.SCREEN_ON");
                this.mContext.registerReceiver(new ScreenBroadcastReceiver(this, null), filter);
            }
        }
        this.mFalseTouchMonitor = new HwFalseTouchMonitor();
        if (mSupportGameAssist) {
            this.mAms = HwActivityManagerService.self();
            this.mFingerPrintId = SystemProperties.getInt("sys.fingerprint.deviceId", -1);
            Stub gameObserver = new Stub() {
                public void onGameStatusChanged(String packageName, int event) {
                    String str = HwPhoneWindowManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("currentFgApp=");
                    stringBuilder.append(packageName);
                    stringBuilder.append(", mLastFgPackageName=");
                    stringBuilder.append(HwPhoneWindowManager.this.mLastFgPackageName);
                    Log.i(str, stringBuilder.toString());
                    if (!(packageName == null || packageName.equals(HwPhoneWindowManager.this.mLastFgPackageName))) {
                        HwPhoneWindowManager.this.mEnableKeyInCurrentFgGameApp = false;
                    }
                    HwPhoneWindowManager.this.mLastFgPackageName = packageName;
                }

                public void onGameListChanged() {
                }
            };
            if (this.mAms != null) {
                this.mAms.registerGameObserver(gameObserver);
            }
        }
        if (this.mGestureNavManager != null) {
            this.mGestureNavManager.systemReady();
        }
        if (HwPCUtils.enabled()) {
            this.mExternalSystemGestures = new SystemGesturesPointerEventListener(this.mContext, new Callbacks() {
                public void onSwipeFromTop() {
                    if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                        HwPCUtils.log(HwPhoneWindowManager.TAG, "mExternalSystemGestures onSwipeFromTop");
                        onMouseHoverAtTop();
                    }
                }

                public void onSwipeFromBottom() {
                }

                public void onSwipeFromRight() {
                }

                public void onSwipeFromLeft() {
                }

                public void onFling(int durationMs) {
                }

                public void onDown() {
                }

                public void onUpOrCancel() {
                }

                public void onMouseHoverAtTop() {
                    HwPhoneWindowManager.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            HwPhoneWindowManager.this.showTopBar();
                        }
                    }, 200);
                }

                public void onMouseHoverAtBottom() {
                }

                public void onMouseLeaveFromEdge() {
                }

                public void onDebug() {
                }
            });
            this.mWindowManagerFuncs.registerExternalPointerEventListener(this.mExternalSystemGestures);
            this.mExternalSystemGestures.systemReady();
        }
        if (PickUpWakeScreenManager.isPickupSensorSupport(this.mContext)) {
            PickUpWakeScreenManager.getInstance(this.mContext, this.mHandler, this.mWindowManagerFuncs, this.mKeyguardDelegate).pickUpWakeScreenInit();
        }
    }

    public void registerExternalPointerEventListener() {
        if (HwPCUtils.enabled() && HwPCUtils.isPcCastModeInServer()) {
            unRegisterExternalPointerEventListener();
            this.mLockScreenListener = new PointerEventListener() {
                public void onPointerEvent(MotionEvent motionEvent) {
                    if (motionEvent.getEventTime() - HwPhoneWindowManager.this.mLastKeyPointerTime > 500) {
                        HwPhoneWindowManager.this.mLastKeyPointerTime = motionEvent.getEventTime();
                        HwPhoneWindowManager.this.userActivityOnDesktop();
                    }
                }
            };
            this.mWindowManagerFuncs.registerExternalPointerEventListener(this.mLockScreenListener);
            this.mLockScreenBuildInDisplayListener = new PointerEventListener() {
                public void onPointerEvent(MotionEvent motionEvent) {
                    if (motionEvent.getEventTime() - HwPhoneWindowManager.this.mLastKeyPointerTime > 500) {
                        HwPhoneWindowManager.this.mLastKeyPointerTime = motionEvent.getEventTime();
                        HwPhoneWindowManager.this.userActivityOnDesktop();
                    }
                }
            };
            this.mWindowManagerFuncs.registerPointerEventListener(this.mLockScreenBuildInDisplayListener);
        }
        if (HwPCUtils.enabledInPad() && HwStylusUtils.hasStylusFeature(this.mContext) && HwPCUtils.isPcCastModeInServer() && this.mStylusGestureListener4PCMode == null) {
            this.mStylusGestureListener4PCMode = new StylusGestureListener(this.mContext, this);
            this.mStylusGestureListener4PCMode.setForPCModeOnly(true);
            Log.i(TAG, "HPWM Set For PC Mode Only Flag");
            this.mWindowManagerFuncs.registerExternalPointerEventListener(this.mStylusGestureListener4PCMode);
        }
    }

    public void unRegisterExternalPointerEventListener() {
        if (HwPCUtils.enabled() && HwPCUtils.isPcCastModeInServer()) {
            if (this.mLockScreenListener != null) {
                this.mWindowManagerFuncs.unregisterExternalPointerEventListener(this.mLockScreenListener);
                this.mLockScreenListener = null;
            }
            if (this.mLockScreenBuildInDisplayListener != null) {
                this.mWindowManagerFuncs.unregisterPointerEventListener(this.mLockScreenBuildInDisplayListener);
                this.mLockScreenBuildInDisplayListener = null;
            }
        }
        if (HwPCUtils.enabledInPad() && HwStylusUtils.hasStylusFeature(this.mContext) && HwPCUtils.isPcCastModeInServer() && this.mStylusGestureListener4PCMode != null) {
            this.mWindowManagerFuncs.unregisterExternalPointerEventListener(this.mStylusGestureListener4PCMode);
            this.mStylusGestureListener4PCMode = null;
        }
    }

    public void addPointerEvent(MotionEvent motionEvent) {
        if (this.mNavigationBarPolicy != null) {
            this.mNavigationBarPolicy.addPointerEvent(motionEvent);
        }
    }

    public void init(Context context, IWindowManager windowManager, WindowManagerFuncs windowManagerFuncs) {
        this.mHandlerEx = new PolicyHandlerEx(this, null);
        this.fingersense_enable = "fingersense_smartshot_enabled";
        this.fingersense_letters_enable = "fingersense_letters_enabled";
        this.line_gesture_enable = "fingersense_multiwindow_enabled";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init fingersense_letters_enable with ");
        stringBuilder.append(this.fingersense_letters_enable);
        Flog.i(1503, stringBuilder.toString());
        this.navibar_enable = "enable_navbar";
        this.mCurUser = ActivityManager.getCurrentUser();
        this.mResolver = context.getContentResolver();
        this.mFingerprintObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                HwPhoneWindowManager.this.updateFingerprintNav();
            }
        };
        registerFingerprintObserver(this.mCurUser);
        updateFingerprintNav();
        this.mHwOUCObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                HwPhoneWindowManager.this.updateHwOUCKeyguardViewState();
            }
        };
        registerHwOUCObserver(this.mCurUser);
        updateHwOUCKeyguardViewState();
        initDropSmartKey();
        super.init(context, windowManager, windowManagerFuncs);
        this.mGestureNavManager = new GestureNavManager(context);
        this.mKeyguardDelegate.setHwPCKeyguardShowingCallback(new HwPCKeyguardShowingCallback() {
            public void onShowingChanged(boolean showing) {
                if (HwPCUtils.isPcCastModeInServer()) {
                    HwPhoneWindowManager.this.lockScreen(showing);
                }
                if (!showing) {
                    HwPhoneWindowManager.this.mHwScreenOnProximityLock.releaseLock(3);
                }
                if (HwPhoneWindowManager.this.mInputManagerInternal != null) {
                    HwPhoneWindowManager.this.mInputManagerInternal.onKeyguardStateChanged(showing);
                }
                if (HwPhoneWindowManager.this.mGestureNavManager != null) {
                    HwPhoneWindowManager.this.mGestureNavManager.onKeyguardShowingChanged(showing);
                }
            }
        });
        registerNotchListener();
        registerReceivers(context);
        if (this.mAppEyeBackKey != null) {
            this.mAppEyeBackKey.init(null);
        }
        if (this.mAppEyeHomeKey != null) {
            this.mAppEyeHomeKey.init(null);
        }
        Message msg = this.mHandlerEx.obtainMessage(MSG_NOTIFY_FINGER_OPTICAL);
        msg.setAsynchronous(true);
        this.mHandlerEx.sendMessage(msg);
    }

    private void registerReceivers(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_NOTCH_SCREEN_ACTION);
        filter.addAction(UPDATE_GESTURE_NAV_ACTION);
        context.registerReceiverAsUser(this.mWhitelistReceived, UserHandle.ALL, filter, UPDATE_NOTCH_SCREEN_PER, null);
    }

    private void registerNotchListener() {
        if (IS_NOTCH_PROP) {
            boolean z = true;
            if (Secure.getIntForUser(this.mContext.getContentResolver(), "display_notch_status", 0, this.mCurUser) != 1) {
                z = false;
            }
            this.mIsNotchSwitchOpen = z;
            HwNotchScreenWhiteConfig.getInstance().setNotchSwitchStatus(this.mIsNotchSwitchOpen);
            this.mNotchSwitchListener = new NotchSwitchListener() {
                public void onChange() {
                    HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                    boolean z = true;
                    if (Secure.getIntForUser(HwPhoneWindowManager.this.mContext.getContentResolver(), "display_notch_status", 0, HwPhoneWindowManager.this.mCurUser) != 1) {
                        z = false;
                    }
                    hwPhoneWindowManager.isNotchTemp = z;
                    if (HwPhoneWindowManager.this.mIsNotchSwitchOpen != HwPhoneWindowManager.this.isNotchTemp) {
                        HwPhoneWindowManager.this.mIsNotchSwitchOpen = HwPhoneWindowManager.this.isNotchTemp;
                        HwNotchScreenWhiteConfig.getInstance().setNotchSwitchStatus(HwPhoneWindowManager.this.mIsNotchSwitchOpen);
                    }
                }
            };
            HwNotchScreenWhiteConfig.registerNotchSwitchListener(this.mContext, this.mNotchSwitchListener);
            try {
                ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                    public void onUserSwitching(int newUserId) throws RemoteException {
                        HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                        boolean z = true;
                        if (Secure.getIntForUser(HwPhoneWindowManager.this.mContext.getContentResolver(), "display_notch_status", 0, HwPhoneWindowManager.this.mCurUser) != 1) {
                            z = false;
                        }
                        hwPhoneWindowManager.mIsNotchSwitchOpen = z;
                        HwNotchScreenWhiteConfig.getInstance().setNotchSwitchStatus(HwPhoneWindowManager.this.mIsNotchSwitchOpen);
                    }
                }, TAG);
            } catch (RemoteException e) {
                Log.i(TAG, "registerUserSwitchObserver fail", e);
            }
        }
    }

    private void updateFingerprintNav() {
        boolean z = false;
        this.isFingerShotCameraOn = Secure.getIntForUser(this.mResolver, FINGERPRINT_CAMERA_SWITCH, 1, this.mCurUser) == 1;
        this.isFingerStopAlarmOn = Secure.getIntForUser(this.mResolver, FINGERPRINT_STOP_ALARM, 0, this.mCurUser) == 1;
        if (Secure.getIntForUser(this.mResolver, FINGERPRINT_ANSWER_CALL, 0, this.mCurUser) == 1) {
            z = true;
        }
        this.isFingerAnswerPhoneOn = z;
    }

    private void registerFingerprintObserver(int userId) {
        this.mResolver.registerContentObserver(Secure.getUriFor(FINGERPRINT_CAMERA_SWITCH), true, this.mFingerprintObserver, userId);
        this.mResolver.registerContentObserver(Secure.getUriFor(FINGERPRINT_STOP_ALARM), true, this.mFingerprintObserver, userId);
        this.mResolver.registerContentObserver(Secure.getUriFor(FINGERPRINT_ANSWER_CALL), true, this.mFingerprintObserver, userId);
    }

    public void setCurrentUser(int userId, int[] currentProfileIds) {
        this.mCurUser = userId;
        registerFingerprintObserver(userId);
        this.mFingerprintObserver.onChange(true);
        this.mSettingsObserver.registerContentObserver(userId);
        this.mSettingsObserver.onChange(true);
        if (this.fingerprintActionsListener != null) {
            this.fingerprintActionsListener.setCurrentUser(userId);
        }
        if (this.mGestureNavManager != null) {
            this.mGestureNavManager.onUserChanged(userId);
        }
    }

    private void registerHwOUCObserver(int userId) {
        Log.d(TAG, "register HwOUC Observer");
        this.mResolver.registerContentObserver(Secure.getUriFor(KEY_HWOUC_KEYGUARD_VIEW_ON_TOP), true, this.mHwOUCObserver, userId);
    }

    private void updateHwOUCKeyguardViewState() {
        boolean z = true;
        if (1 != Secure.getIntForUser(this.mResolver, KEY_HWOUC_KEYGUARD_VIEW_ON_TOP, 0, this.mCurUser)) {
            z = false;
        }
        this.isHwOUCKeyguardViewOnTop = z;
    }

    private boolean supportActivityForbidSpecialKey(int keyCode) {
        if (this.isHwOUCKeyguardViewOnTop && (3 == keyCode || 4 == keyCode || 187 == keyCode)) {
            return true;
        }
        return false;
    }

    public int checkAddPermission(LayoutParams attrs, int[] outAppOp) {
        int type = attrs.type;
        if (type == 2101 || type == 2104) {
            return 0;
        }
        return super.checkAddPermission(attrs, outAppOp);
    }

    public int getLastSystemUiFlags() {
        return this.mLastSystemUiFlags;
    }

    public int getWindowLayerFromTypeLw(int type, boolean canAddInternalSystemWindow) {
        if (type == 2104) {
            return 35;
        }
        switch (type) {
            case 2100:
                return 33;
            case 2101:
                return 34;
            default:
                int ret = super.getWindowLayerFromTypeLw(type, canAddInternalSystemWindow);
                return ret >= 33 ? ret + 2 : ret;
        }
    }

    public void freezeOrThawRotation(int rotation) {
        this.mDesiredRotation = rotation;
    }

    public boolean rotationHasCompatibleMetricsLw(int orientation, int rotation) {
        if (this.mDesiredRotation != 0) {
            return super.rotationHasCompatibleMetricsLw(orientation, rotation);
        }
        Slog.d(TAG, "desired rotation is rotation 0");
        return true;
    }

    public int rotationForOrientationLw(int orientation, int lastRotation, boolean defaultDisplay) {
        if (isDefaultOrientationForced()) {
            return super.rotationForOrientationLw(orientation, lastRotation, defaultDisplay);
        }
        int desiredRotation = this.mDesiredRotation;
        if (desiredRotation < 0) {
            return super.rotationForOrientationLw(orientation, lastRotation, defaultDisplay);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mDesiredRotation:");
        stringBuilder.append(this.mDesiredRotation);
        Slog.i(str, stringBuilder.toString());
        return desiredRotation;
    }

    public void beginPostLayoutPolicyLw(int displayWidth, int displayHeight) {
        super.beginPostLayoutPolicyLw(displayWidth, displayHeight);
        this.mStatuBarObsecured = false;
    }

    public void applyPostLayoutPolicyLw(WindowState win, LayoutParams attrs, WindowState attached, WindowState imeTarget) {
        super.applyPostLayoutPolicyLw(win, attrs, attached, imeTarget);
        if (win.isVisibleLw() && win.getSurfaceLayer() > this.mStatusBarLayer && isStatusBarObsecuredByWin(win)) {
            this.mStatuBarObsecured = true;
        }
    }

    protected void setHasAcitionBar(boolean hasActionBar) {
        this.mIsHasActionBar = hasActionBar;
    }

    private View getActionBarView(Context context, int theme) {
        context.setTheme(theme);
        View tmp = new View(context);
        int color = HwWidgetFactory.getPrimaryColor(context);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Starting window for ");
        stringBuilder.append(context.getPackageName());
        stringBuilder.append(" ActionBarView color=0x");
        stringBuilder.append(Integer.toHexString(color));
        Slog.d(str, stringBuilder.toString());
        if (HwWidgetUtils.isActionbarBackgroundThemed(this.mContext) || Color.alpha(color) == 0) {
            return null;
        }
        tmp.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        tmp.setBackgroundDrawable(new ColorDrawable(color));
        return tmp;
    }

    private static boolean isMultiSimEnabled() {
        try {
            return MSimTelephonyManager.getDefault().isMultiSimEnabled();
        } catch (NoExtAPIException e) {
            Log.w(TAG, "CoverManagerService->isMultiSimEnabled->NoExtAPIException!");
            return false;
        }
    }

    private boolean isPhoneInCall() {
        if (isMultiSimEnabled()) {
            int phoneCount = MSimTelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                if (MSimTelephonyManager.getDefault().getCallState(i) != 0) {
                    return true;
                }
            }
            return false;
        } else if (TelephonyManager.getDefault().getCallState(SubscriptionManager.getDefaultSubscriptionId()) != 0) {
            return true;
        } else {
            return false;
        }
    }

    static ITelephony getTelephonyService() {
        return ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
    }

    public boolean needTurnOff(int why) {
        return (isPhoneInCall() && (!isKeyguardSecure(this.mCurrentUserId) || why == 3 || why == 6)) ? false : true;
    }

    public boolean needTurnOffWithDismissFlag(WindowState topAppWin) {
        if (!(topAppWin == null || (topAppWin.getAttrs().flags & 524288) == 0 || isKeyguardSecure(this.mCurrentUserId))) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TurnOffWithDismissFlag ");
            stringBuilder.append(topAppWin);
            Log.w(str, stringBuilder.toString());
        }
        return true;
    }

    public boolean needTurnOffWithDismissFlag() {
        return true;
    }

    protected boolean isWakeKeyWhenScreenOff(int keyCode) {
        if (!mCustUsed) {
            return super.isWakeKeyWhenScreenOff(keyCode);
        }
        for (int i : mUnableWakeKey) {
            if (keyCode == i) {
                return false;
            }
        }
        return true;
    }

    public boolean isWakeKeyFun(int keyCode) {
        if (!mCustBeInit) {
            getKeycodeFromCust();
        }
        if (!mCustUsed) {
            return false;
        }
        for (int i : mUnableWakeKey) {
            if (keyCode == i) {
                return false;
            }
        }
        return true;
    }

    void startDockOrHome(boolean fromHomeKey, boolean awakenFromDreams) {
        if (fromHomeKey) {
            HwInputManagerServiceInternal inputManager = (HwInputManagerServiceInternal) LocalServices.getService(HwInputManagerServiceInternal.class);
            if (inputManager != null) {
                inputManager.notifyHomeLaunching();
            }
        }
        super.startDockOrHome(fromHomeKey, awakenFromDreams);
    }

    private void getKeycodeFromCust() {
        String unableCustomizedWakeKey = null;
        try {
            unableCustomizedWakeKey = Systemex.getString(this.mContext.getContentResolver(), "unable_wake_up_key");
        } catch (Exception e) {
            Log.e(TAG, "Exception when got name value", e);
        }
        if (unableCustomizedWakeKey != null) {
            String[] unableWakeKeyArray = unableCustomizedWakeKey.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            if (!(unableWakeKeyArray == null || unableWakeKeyArray.length == 0)) {
                mUnableWakeKey = new int[unableWakeKeyArray.length];
                int i = 0;
                while (i < mUnableWakeKey.length) {
                    try {
                        mUnableWakeKey[i] = Integer.parseInt(unableWakeKeyArray[i]);
                        i++;
                    } catch (Exception e2) {
                        Log.e(TAG, "Exception when copy the translated value from sting array to int array", e2);
                    }
                }
                mCustUsed = true;
            }
        }
        mCustBeInit = true;
    }

    public int interceptMotionBeforeQueueingNonInteractive(long whenNanos, int policyFlags) {
        if ((FLOATING_MASK & policyFlags) == 0) {
            return super.interceptMotionBeforeQueueingNonInteractive(whenNanos, policyFlags);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("interceptMotionBeforeQueueingNonInteractive policyFlags: ");
        stringBuilder.append(policyFlags);
        Slog.i(str, stringBuilder.toString());
        Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
        return 0;
    }

    protected int getSingleHandState() {
        IBinder windowManagerBinder = WindowManagerGlobal.getWindowManagerService().asBinder();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        if (windowManagerBinder != null) {
            int ex;
            try {
                data.writeInterfaceToken("android.view.IWindowManager");
                windowManagerBinder.transact(1990, data, reply, 0);
                reply.readException();
                ex = reply.readInt();
                return ex;
            } catch (RemoteException e) {
                ex = e;
                return 0;
            } finally {
                data.recycle();
                reply.recycle();
            }
        } else {
            data.recycle();
            reply.recycle();
            return 0;
        }
    }

    protected void unlockScreenPinningTest() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (getHWStatusBarService() != null) {
                IBinder statusBarServiceBinder = getHWStatusBarService().asBinder();
                if (statusBarServiceBinder != null) {
                    Log.d(TAG, "Transact unlockScreenPinningTest to status bar service!");
                    data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                    statusBarServiceBinder.transact(HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START, data, reply, 0);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transactToStatusBarService->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
        reply.recycle();
        data.recycle();
    }

    public void finishedGoingToSleep(int why) {
        this.mHandler.removeCallbacks(this.mOverscanTimeout);
        this.mHandler.postDelayed(this.mOverscanTimeout, 200);
        super.finishedGoingToSleep(why);
    }

    private void interceptSystraceLog() {
        long now = SystemClock.uptimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("now=");
        stringBuilder.append(now);
        stringBuilder.append(" mSystraceLogVolumeUpKeyTime=");
        stringBuilder.append(this.mSystraceLogVolumeUpKeyTime);
        stringBuilder.append(" mSystraceLogFingerPrintTime=");
        stringBuilder.append(this.mSystraceLogFingerPrintTime);
        Log.d(str, stringBuilder.toString());
        if (now <= this.mSystraceLogVolumeUpKeyTime + 150 && now <= this.mSystraceLogFingerPrintTime + SYSTRACELOG_FINGERPRINT_EFFECT_DELAY && this.mSystraceLogCompleted) {
            this.mSystraceLogCompleted = false;
            this.mSystraceLogVolumeUpKeyConsumed = true;
            this.mSystraceLogFingerPrintTime = 0;
            this.mSystraceLogVolumeUpKeyTriggered = false;
            this.mScreenRecorderVolumeUpKeyTriggered = false;
            Trace.traceBegin(8, "invoke_systrace_log_dump");
            Jlog.d(MemoryConstant.MSG_COMPRESS_GPU, "HwPhoneWindowManager Systrace triggered");
            Trace.traceEnd(8);
            Log.d(TAG, "Systrace triggered");
            this.mHandler.postDelayed(this.mSystraceLogRunnable, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
        }
    }

    private void interceptScreenRecorder() {
        if (this.mScreenRecorderVolumeUpKeyTriggered && this.mScreenRecorderPowerKeyTriggered && !this.mScreenRecorderVolumeDownKeyTriggered && !SystemProperties.getBoolean("sys.super_power_save", false) && !keyguardIsShowingTq() && checkPackageInstalled(HUAWEI_SCREENRECORDER_PACKAGE)) {
            if (this.mCust != null && !this.mCust.isSosAllowed()) {
                return;
            }
            if (!HWRIDEMODE_FEATURE_SUPPORTED || !SystemProperties.getBoolean("sys.ride_mode", false)) {
                long now = SystemClock.uptimeMillis();
                if (now <= this.mScreenRecorderVolumeUpKeyTime + 150 && now <= this.mScreenRecorderPowerKeyTime + 150) {
                    this.mScreenRecorderVolumeUpKeyConsumed = true;
                    cancelPendingPowerKeyActionForDistouch();
                    this.mHandler.postDelayed(this.mScreenRecorderRunnable, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                }
            }
        }
    }

    private void cancelPendingScreenRecorderAction() {
        this.mHandler.removeCallbacks(this.mScreenRecorderRunnable);
    }

    boolean isVoiceCall() {
        IAudioService audioService = getAudioService();
        boolean z = false;
        if (audioService != null) {
            try {
                int mode = audioService.getMode();
                if (mode == 3 || mode == 2) {
                    z = true;
                }
                return z;
            } catch (RemoteException e) {
                Log.w(TAG, "getMode exception");
            }
        }
        return false;
    }

    private void sendKeyEvent(int keycode) {
        int[] actions = new int[]{0, 1};
        KeyEvent ev = null;
        for (int keyEvent : actions) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, keyEvent, keycode, 0, 0, -1, 0, 8, 257), 0);
        }
    }

    private boolean isExcluedScene() {
        String pkgName = ((ActivityManagerService) ServiceManager.getService("activity")).topAppName();
        String pkg_alarm = "com.android.deskclock/.alarmclock.LockAlarmFullActivity";
        boolean z = false;
        boolean isSuperPowerMode = SystemProperties.getBoolean("sys.super_power_save", false);
        if (pkgName == null) {
            return false;
        }
        if (pkgName.equals(pkg_alarm) || isSuperPowerMode || !this.mDeviceProvisioned || keyguardOn()) {
            z = true;
        }
        return z;
    }

    private boolean isExcluedBackScene() {
        if (this.mTrikeyNaviMode == 1) {
            return isExcluedScene();
        }
        return this.mDeviceProvisioned ^ true;
    }

    private boolean isExcluedRecentScene() {
        if (this.mTrikeyNaviMode == 1) {
            return this.mDeviceProvisioned ^ true;
        }
        return isExcluedScene();
    }

    private void resetButtonLightStatus() {
        if (this.mButtonLight != null) {
            if (this.mDeviceProvisioned) {
                Slog.i(TAG, "resetButtonLightStatus");
                this.mHandlerEx.removeMessages(MSG_BUTTON_LIGHT_TIMEOUT);
                if (this.mTrikeyNaviMode < 0) {
                    setButtonLightTimeout(false);
                    this.mButtonLight.setBrightness(0);
                } else if (this.mButtonLightMode != 0) {
                    setButtonLightTimeout(false);
                    this.mButtonLight.setBrightness(this.mBackLight.getCurrentBrightness());
                } else if (this.mButtonLight.getCurrentBrightness() > 0) {
                    setButtonLightTimeout(false);
                    Message msg = this.mHandlerEx.obtainMessage(MSG_BUTTON_LIGHT_TIMEOUT);
                    msg.setAsynchronous(true);
                    this.mHandlerEx.sendMessageDelayed(msg, 5000);
                } else {
                    setButtonLightTimeout(true);
                }
            } else {
                setButtonLightTimeout(false);
                this.mButtonLight.setBrightness(0);
            }
        }
    }

    private void setButtonLightTimeout(boolean timeout) {
        SystemProperties.set("sys.button.light.timeout", String.valueOf(timeout));
    }

    private void sendLightTimeoutMsg() {
        if (this.mButtonLight != null && this.mDeviceProvisioned) {
            this.mHandlerEx.removeMessages(MSG_BUTTON_LIGHT_TIMEOUT);
            if (this.mTrikeyNaviMode >= 0) {
                int curButtonBrightness = this.mButtonLight.getCurrentBrightness();
                int curBackBrightness = this.mBackLight.getCurrentBrightness();
                if (this.mButtonLightMode == 0) {
                    if (SystemProperties.getBoolean("sys.button.light.timeout", false) && curButtonBrightness == 0) {
                        this.mButtonLight.setBrightness(curBackBrightness);
                    }
                    setButtonLightTimeout(false);
                    Message msg = this.mHandlerEx.obtainMessage(MSG_BUTTON_LIGHT_TIMEOUT);
                    msg.setAsynchronous(true);
                    this.mHandlerEx.sendMessageDelayed(msg, 5000);
                } else if (curButtonBrightness == 0) {
                    this.mButtonLight.setBrightness(curBackBrightness);
                }
            } else {
                setButtonLightTimeout(false);
                this.mButtonLight.setBrightness(0);
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x003e, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void startHwVibrate(int vibrateMode) {
        if (!(isKeyguardLocked() || !this.mHapticEnabled || "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false")) || this.mVibrator == null)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startVibrateWithConfigProp:");
            stringBuilder.append(vibrateMode);
            Log.d(str, stringBuilder.toString());
            this.mVibrator.vibrate((long) vibrateMode);
        }
    }

    private boolean isMMITesting() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    private void wakeupSOSPage(int keycode) {
        if (keycode == 25 || keycode == 24) {
            this.mLastVolumeKeyDownTime = SystemClock.uptimeMillis();
            return;
        }
        if (keycode == 26) {
            long now = SystemClock.uptimeMillis();
            if (this.mPowerKeyCount <= 0 || (now - this.mLastPowerKeyDownTime <= 500 && now - this.mLastVolumeKeyDownTime >= POWER_SOS_MISTOUCH_THRESHOLD)) {
                this.mPowerKeyCount++;
                this.mLastPowerKeyDownTime = now;
                if (this.mPowerKeyCount == 5) {
                    resetSOS();
                    this.mIsFreezePowerkey = false;
                    PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
                    String str;
                    StringBuilder stringBuilder;
                    try {
                        String pkgName = getTopActivity();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("get Emergency power TopActivity is:");
                        stringBuilder.append(pkgName);
                        Log.d(str, stringBuilder.toString());
                        if ("com.android.emergency/.view.ViewCountDownActivity".equals(pkgName) || "com.android.emergency/.view.EmergencyNumberActivity".equals(pkgName)) {
                            Log.d(TAG, "current topActivity is emergency, return ");
                            if (!(powerManager == null || powerManager.isScreenOn())) {
                                powerManager.wakeUp(SystemClock.uptimeMillis());
                            }
                            return;
                        }
                        Intent intent = new Intent();
                        intent.setPackage(PKG_NAME_EMERGENCY);
                        intent.setAction("android.emergency.COUNT_DOWN");
                        intent.addCategory("android.intent.category.DEFAULT");
                        if (isSoSEmergencyInstalled(intent)) {
                            this.mIsFreezePowerkey = true;
                            this.mHandlerEx.removeMessages(MSG_FREEZE_POWER_KEY);
                            Log.d(TAG, "Emergency power start activity");
                            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                        }
                        if (this.mIsFreezePowerkey) {
                            if (powerManager != null) {
                                powerManager.wakeUp(SystemClock.uptimeMillis());
                            }
                            this.mHandlerEx.removeMessages(MSG_FREEZE_POWER_KEY);
                            Message msg = this.mHandlerEx.obtainMessage(MSG_FREEZE_POWER_KEY);
                            msg.setAsynchronous(true);
                            this.mHandlerEx.sendMessageDelayed(msg, 1000);
                        }
                    } catch (ActivityNotFoundException ex) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ActivityNotFoundException failed message : ");
                        stringBuilder.append(ex);
                        Log.e(str, stringBuilder.toString());
                        this.mIsFreezePowerkey = false;
                    } catch (Exception e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("StartActivity Exception : ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        this.mIsFreezePowerkey = false;
                    }
                }
            } else {
                resetSOS();
            }
        }
    }

    private void resetSOS() {
        this.mLastPowerKeyDownTime = 0;
        this.mPowerKeyCount = 0;
    }

    private boolean isSoSEmergencyInstalled(Intent intent) {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null || intent.resolveActivity(packageManager) == null) {
            return false;
        }
        return true;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        KeyEvent keyEvent = event;
        boolean isPhoneActive = true;
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        int flags = event.getFlags();
        int deviceID = event.getDeviceId();
        String str;
        StringBuilder stringBuilder;
        if (supportActivityForbidSpecialKey(keyCode)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("has intercept Key for block ");
            stringBuilder.append(keyCode);
            stringBuilder.append(", some  ssssuper activity is on top now.");
            Log.d(str, stringBuilder.toString());
            return 0;
        }
        if (down) {
            ZrHungData arg;
            if (keyCode == 4 && this.mAppEyeBackKey != null) {
                arg = new ZrHungData();
                arg.putLong("downTime", event.getDownTime());
                this.mAppEyeBackKey.check(arg);
            }
            if (keyCode == 3 && this.mAppEyeHomeKey != null) {
                arg = new ZrHungData();
                arg.putLong("downTime", event.getDownTime());
                this.mAppEyeHomeKey.check(arg);
            }
        }
        if (!this.mSystraceLogCompleted) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" has intercept Key for block : ");
            stringBuilder.append(keyCode);
            stringBuilder.append(", isdown : ");
            stringBuilder.append(down);
            stringBuilder.append(", flags : ");
            stringBuilder.append(flags);
            Log.d(str, stringBuilder.toString());
            return 0;
        } else if (handleInputEventInPCCastMode(event)) {
            return 0;
        } else {
            if (this.mCust != null) {
                this.mCust.processCustInterceptKey(keyCode, down, this.mContext);
            }
            int origKeyCode = event.getOrigKeyCode();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HwPhoneWindowManager has intercept Key : ");
            stringBuilder2.append(keyCode);
            stringBuilder2.append(", isdown : ");
            stringBuilder2.append(down);
            stringBuilder2.append(", flags : ");
            stringBuilder2.append(flags);
            Flog.i(WifiProCommonUtils.RESP_CODE_INVALID_URL, stringBuilder2.toString());
            boolean isInjected = (policyFlags & 16777216) != 0;
            wakeupNoteEditor(keyEvent, isInjected);
            if (keyCode == 26 && this.mIsFreezePowerkey) {
                return 1;
            }
            boolean isScreenOn = (policyFlags & 536870912) != 0;
            if ((keyCode == 26 || keyCode == 6 || keyCode == 187) && this.mFocusedWindow != null && (this.mFocusedWindow.getAttrs().hwFlags & FLOATING_MASK) == FLOATING_MASK) {
                Log.d(TAG, "power and endcall key received and passsing to user.");
                return 1;
            }
            int navibaron;
            int result;
            if (SystemProperties.getBoolean("ro.config.hw_easywakeup", false) && this.mSystemReady) {
                if (EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate).handleWakeUpKey(keyEvent, isScreenOn ? -1 : this.mScreenOffReason)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("EasyWakeUpManager has handled the keycode : ");
                    stringBuilder.append(event.getKeyCode());
                    Log.d(str, stringBuilder.toString());
                    return 0;
                }
            }
            if (down && event.getRepeatCount() == 0 && SystemProperties.get(VIBRATE_ON_TOUCH, "false").equals("true") && ((keyCode == 82 && (268435456 & flags) == 0) || keyCode == 3 || keyCode == 4 || (policyFlags & 2) != 0)) {
                performHapticFeedbackLw(null, 1, false);
            }
            if ((origKeyCode == 305 || origKeyCode == MemoryConstant.MSG_PROTECTLRU_SWITCH || origKeyCode == MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO) && mTplusEnabled && !isRinging()) {
                ContentResolver resolver = this.mContext.getContentResolver();
                int value = System.getInt(resolver, "hw_membrane_touch_enabled", 0);
                if (value == 0 && down) {
                    notifyTouchplusService(4, 0);
                }
                navibaron = System.getInt(resolver, "hw_membrane_touch_navbar_enabled", 0);
                if (down && 1 == value && 1 == navibaron && System.getInt(resolver, TOUCHPLUS_SETTINGS_VIBRATION, 1) == 1) {
                    Log.v(TAG, "vibration is not disabled by user");
                    performHapticFeedbackLw(null, 1, true);
                }
            }
            boolean isWakeKey = isWakeKeyFun(keyCode) | ((policyFlags & 1) != 0 ? 1 : 0);
            if ((!isScreenOn || this.mHeadless) && (!isInjected || isWakeKey)) {
                result = 0;
                if (down && isWakeKey) {
                    isWakeKeyWhenScreenOff(keyCode);
                }
            } else {
                result = 1;
            }
            int result2 = result;
            if (this.mFocusedWindow != null && (this.mFocusedWindow.getAttrs().hwFlags & 8) == 8 && ((keyCode == 25 || keyCode == 24) && "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false")))) {
                Log.i(TAG, "Prevent hard key volume event to mmi test before queueing.");
                return result2 & -2;
            } else if (lightScreenOnPcMode(keyCode)) {
                return 0;
            } else {
                boolean isInjected2;
                int i;
                int keyCode2;
                boolean down2;
                boolean handled;
                StringBuilder stringBuilder3;
                switch (keyCode) {
                    case 3:
                    case 4:
                    case 187:
                        boolean isScreenOn2 = isScreenOn;
                        isInjected2 = isInjected;
                        result = origKeyCode;
                        navibaron = deviceID;
                        i = flags;
                        keyCode2 = keyCode;
                        down2 = down;
                        if (down2) {
                            if (this.mFalseTouchMonitor != null) {
                                this.mFalseTouchMonitor.handleKeyEvent(keyEvent);
                            }
                            if (this.mHwScreenOnProximityLock != null && this.mHwScreenOnProximityLock.isShowing() && isScreenOn2 && !this.mHintShown && (event.getFlags() & 1024) == 0) {
                                String str2 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("keycode: ");
                                stringBuilder4.append(keyCode2);
                                stringBuilder4.append(" is comsumed by disable touch mode.");
                                Log.d(str2, stringBuilder4.toString());
                                this.mHwScreenOnProximityLock.forceShowHint();
                                this.mHintShown = true;
                                break;
                            }
                        }
                        isScreenOn = FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION;
                        isInjected = FrontFingerPrintSettings.isSupportTrikey();
                        boolean isMMITest = isMMITesting();
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("HwPhoneWindowManagerinterceptKeyBeforeQueueing deviceID:");
                        stringBuilder5.append(navibaron);
                        stringBuilder5.append(" isFrontFpNavi:");
                        stringBuilder5.append(isScreenOn);
                        stringBuilder5.append(" isSupportTrikey:");
                        stringBuilder5.append(isInjected);
                        stringBuilder5.append(" isMMITest:");
                        stringBuilder5.append(isMMITest);
                        Flog.i(WifiProCommonUtils.RESP_CODE_INVALID_URL, stringBuilder5.toString());
                        if (navibaron > 0 && isScreenOn && isInjected && !isMMITest && keyCode2 == 4) {
                            if (isTrikeyNaviKeycodeFromLON(isInjected2, isExcluedBackScene())) {
                                return 0;
                            }
                            sendLightTimeoutMsg();
                            if (down2) {
                                this.mBackTrikeyHandled = false;
                                Message msg = this.mHandlerEx.obtainMessage(MSG_TRIKEY_BACK_LONG_PRESS);
                                msg.setAsynchronous(true);
                                this.mHandlerEx.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                                if (this.mTrikeyNaviMode == 1) {
                                    return 0;
                                }
                            }
                            handled = this.mBackTrikeyHandled;
                            if (!this.mBackTrikeyHandled) {
                                this.mBackTrikeyHandled = true;
                                this.mHandlerEx.removeMessages(MSG_TRIKEY_BACK_LONG_PRESS);
                            }
                            if (handled) {
                                return 0;
                            }
                            startHwVibrate(VIBRATOR_SHORT_PRESS_FOR_FRONT_FP);
                            if (this.mTrikeyNaviMode == 1) {
                                Flog.bdReport(this.mContext, 16);
                                sendKeyEvent(187);
                                return 0;
                            }
                        }
                        if (!this.mHasNavigationBar && keyCode2 == 4 && down2) {
                            if (!isScreenInLockTaskMode()) {
                                this.mBackKeyPress = false;
                                this.mBackKeyPressTime = 0;
                                break;
                            }
                            this.mBackKeyPress = true;
                            this.mBackKeyPressTime = event.getDownTime();
                            interceptBackandMenuKey();
                            break;
                        }
                    case 24:
                    case 25:
                    case 164:
                        if (!HwDeviceManager.disallowOp(38)) {
                            boolean keyguardIsShowingTq;
                            if (keyCode != 25) {
                                if (keyCode == 24) {
                                    if (!down) {
                                        this.mVolumeUpKeyDisTouch = false;
                                        this.mScreenRecorderVolumeUpKeyTriggered = false;
                                        cancelPendingScreenRecorderAction();
                                        this.mSystraceLogVolumeUpKeyTriggered = false;
                                    } else if (this.mHwScreenOnProximityLock != null && this.mHwScreenOnProximityLock.isShowing() && isScreenOn && !this.mVolumeUpKeyDisTouch && (event.getFlags() & 1024) == 0) {
                                        Log.d(TAG, "keycode: KEYCODE_VOLUME_UP is comsumed by disable touch mode.");
                                        this.mVolumeUpKeyDisTouch = true;
                                        this.mVolumeUpKeyDisTouchTime = event.getDownTime();
                                        this.mVolumeUpKeyConsumedByDisTouch = false;
                                        if (!this.mHintShown) {
                                            this.mHwScreenOnProximityLock.forceShowHint();
                                            this.mHintShown = true;
                                        }
                                        cancelPendingPowerKeyActionForDistouch();
                                        interceptTouchDisableMode();
                                    } else {
                                        if (isScreenOn && !this.mScreenRecorderVolumeUpKeyTriggered && (event.getFlags() & 1024) == 0) {
                                            cancelPendingPowerKeyActionForDistouch();
                                            this.mScreenRecorderVolumeUpKeyTriggered = true;
                                            this.mScreenRecorderVolumeUpKeyTime = event.getDownTime();
                                            this.mScreenRecorderVolumeUpKeyConsumed = false;
                                            interceptScreenRecorder();
                                        }
                                        str = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("isScreenOn=");
                                        stringBuilder3.append(isScreenOn);
                                        stringBuilder3.append(" mSystraceLogVolumeUpKeyTriggered=");
                                        stringBuilder3.append(this.mSystraceLogVolumeUpKeyTriggered);
                                        stringBuilder3.append(" mScreenRecorderVolumeUpKeyConsumed=");
                                        stringBuilder3.append(this.mScreenRecorderVolumeUpKeyConsumed);
                                        Log.d(str, stringBuilder3.toString());
                                        if (Jlog.isEnable() && Jlog.isBetaUser() && isScreenOn && !this.mSystraceLogVolumeUpKeyTriggered && !this.mSystraceLogPowerKeyTriggered && !this.mSystraceLogVolumeDownKeyTriggered && !this.mScreenRecorderVolumeUpKeyConsumed && (event.getFlags() & 1024) == 0) {
                                            this.mSystraceLogVolumeUpKeyTriggered = true;
                                            this.mSystraceLogVolumeUpKeyTime = event.getDownTime();
                                            this.mSystraceLogVolumeUpKeyConsumed = false;
                                            interceptSystraceLog();
                                            Log.d(TAG, "volumeup process: fingerprint first, then volumeup");
                                            if (this.mSystraceLogVolumeUpKeyConsumed) {
                                                return result2 & -2;
                                            }
                                        }
                                        if (getTelecommService().isInCall() && (result2 & 1) == 0 && this.mCust != null && this.mCust.isVolumnkeyWakeup(this.mContext)) {
                                            this.mCust.volumnkeyWakeup(this.mContext, isScreenOn, this.mPowerManager);
                                        }
                                    }
                                    if (this.mCust != null) {
                                        HwCustPhoneWindowManager hwCustPhoneWindowManager = this.mCust;
                                        Context context = this.mContext;
                                        keyguardIsShowingTq = keyguardIsShowingTq();
                                        if (!(isMusicActive() || isVoiceCall())) {
                                            isPhoneActive = false;
                                        }
                                        isInjected2 = isInjected;
                                        Context context2 = context;
                                        result = origKeyCode;
                                        navibaron = deviceID;
                                        handled = keyguardIsShowingTq;
                                        i = flags;
                                        boolean z = isPhoneActive;
                                        int keyCode3 = keyCode;
                                        down2 = down;
                                        if (!hwCustPhoneWindowManager.interceptVolumeUpKey(keyEvent, context2, isScreenOn, handled, z, isInjected2, down)) {
                                            keyCode2 = keyCode3;
                                            break;
                                        }
                                        return result2;
                                    }
                                }
                                isInjected2 = isInjected;
                                result = origKeyCode;
                                navibaron = deviceID;
                                i = flags;
                                down2 = down;
                                keyCode2 = keyCode;
                                break;
                            }
                            if (!down) {
                                this.mVolumeDownKeyDisTouch = false;
                                this.mScreenRecorderVolumeDownKeyTriggered = false;
                                cancelPendingScreenRecorderAction();
                                this.mSystraceLogVolumeDownKeyTriggered = false;
                            } else if (this.mHwScreenOnProximityLock != null && this.mHwScreenOnProximityLock.isShowing() && isScreenOn && !this.mVolumeDownKeyDisTouch && (event.getFlags() & 1024) == 0) {
                                Log.d(TAG, "keycode: KEYCODE_VOLUME_DOWN is comsumed by disable touch mode.");
                                this.mVolumeDownKeyDisTouch = true;
                                if (!this.mHintShown) {
                                    this.mHwScreenOnProximityLock.forceShowHint();
                                    this.mHintShown = true;
                                }
                            } else {
                                if (isScreenOn && !this.mScreenRecorderVolumeDownKeyTriggered && (event.getFlags() & 1024) == 0) {
                                    cancelPendingPowerKeyActionForDistouch();
                                    this.mScreenRecorderVolumeDownKeyTriggered = true;
                                    cancelPendingScreenRecorderAction();
                                }
                                if (isScreenOn && !this.mSystraceLogVolumeDownKeyTriggered && (event.getFlags() & 1024) == 0) {
                                    this.mSystraceLogVolumeDownKeyTriggered = true;
                                    this.mSystraceLogFingerPrintTime = 0;
                                    this.mSystraceLogVolumeUpKeyTriggered = false;
                                }
                            }
                            keyguardIsShowingTq = keyguardIsShowingTq();
                            String str3 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("interceptVolumeDownKey down=");
                            stringBuilder2.append(down);
                            stringBuilder2.append(" keyguardShow=");
                            stringBuilder2.append(keyguardIsShowingTq);
                            stringBuilder2.append(" policyFlags=");
                            stringBuilder2.append(Integer.toHexString(policyFlags));
                            Log.d(str3, stringBuilder2.toString());
                            if ((!isScreenOn || keyguardIsShowingTq) && !isInjected && (event.getFlags() & 1024) == 0) {
                                if (!isDeviceProvisioned()) {
                                    Log.i(TAG, "Device is not Provisioned");
                                } else if (down) {
                                    boolean isIntercept;
                                    boolean isVoiceCall = isVoiceCall();
                                    keyCode2 = (isMusicActive() || isVoiceCall) ? 1 : 0;
                                    if (!(isMusicActive() || !isPhoneIdle() || isVoiceCall)) {
                                        isPhoneActive = false;
                                    }
                                    if (this.isVoiceRecognitionActive) {
                                        isIntercept = false;
                                        long interval = event.getEventTime() - this.mLastStartVassistantServiceTime;
                                        if (interval > true) {
                                            this.isVoiceRecognitionActive = false;
                                        } else if (interval > true) {
                                            this.isVoiceRecognitionActive = AudioSystem.isSourceActive(6);
                                        }
                                    } else {
                                        isIntercept = false;
                                    }
                                    str = TAG;
                                    keyguardIsShowingTq = new StringBuilder();
                                    keyguardIsShowingTq.append("isMusicOrFMOrVoiceCallActive=");
                                    keyguardIsShowingTq.append(keyCode2);
                                    keyguardIsShowingTq.append(" isVoiceRecognitionActive=");
                                    keyguardIsShowingTq.append(this.isVoiceRecognitionActive);
                                    Log.i(str, keyguardIsShowingTq.toString());
                                    if (keyCode2 != 0 || this.isVoiceRecognitionActive || SystemProperties.getBoolean("sys.super_power_save", false)) {
                                        boolean z2 = isVoiceCall;
                                        isInjected2 = false;
                                    } else {
                                        boolean isVolumeDownDoubleClick = false;
                                        keyguardIsShowingTq = event.getEventTime() - this.mLastVolumeDownKeyDownTime;
                                        this.mLastVolumeDownKeyDownTime = event.getEventTime();
                                        long timediff;
                                        if (keyguardIsShowingTq < 400) {
                                            isInjected2 = true;
                                            if (this.mListener == null) {
                                                this.mListener = new ProximitySensorListener();
                                            }
                                            turnOnSensorListener();
                                            if ((this.mIsProximity || !this.mSensorRegisted) && this.mSensorRegisted) {
                                                timediff = keyguardIsShowingTq;
                                            } else {
                                                str = TAG;
                                                StringBuilder stringBuilder6 = new StringBuilder();
                                                timediff = keyguardIsShowingTq;
                                                stringBuilder6.append("mIsProximity ");
                                                stringBuilder6.append(this.mIsProximity);
                                                stringBuilder6.append(", mSensorRegisted ");
                                                stringBuilder6.append(this.mSensorRegisted);
                                                Log.i(str, stringBuilder6.toString());
                                                notifyRapidCaptureService("start");
                                            }
                                            turnOffSensorListener();
                                            result2 &= -2;
                                        } else {
                                            timediff = keyguardIsShowingTq;
                                            notifyRapidCaptureService("wakeup");
                                            if (this.mListener == null) {
                                                this.mListener = new ProximitySensorListener();
                                            }
                                            turnOnSensorListener();
                                            isInjected2 = isVolumeDownDoubleClick;
                                        }
                                        if (!isScreenOn || isVolumeDownDoubleClick) {
                                            isIntercept = true;
                                        }
                                    }
                                    if (!(isPhoneActive || isScreenOn || isVolumeDownDoubleClick || !checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE))) {
                                        notifyVassistantService("wakeup", true, keyEvent);
                                        if (this.mListener == null) {
                                            this.mListener = new ProximitySensorListener();
                                        }
                                        turnOnSensorListener();
                                        interceptQuickCallChord();
                                        isIntercept = true;
                                    }
                                    boolean isIntercept2 = isIntercept;
                                    keyguardIsShowingTq = TAG;
                                    StringBuilder stringBuilder7 = new StringBuilder();
                                    stringBuilder7.append("intercept volume down key, isIntercept=");
                                    stringBuilder7.append(isIntercept2);
                                    stringBuilder7.append(" now=");
                                    stringBuilder7.append(SystemClock.uptimeMillis());
                                    stringBuilder7.append(" EventTime=");
                                    stringBuilder7.append(event.getEventTime());
                                    Log.i(keyguardIsShowingTq, stringBuilder7.toString());
                                    if (isInterceptAndCheckRinging(isIntercept2)) {
                                        return result2;
                                    }
                                    if (getTelecommService().isInCall() && !(result2 & 1) && this.mCust && this.mCust.isVolumnkeyWakeup(this.mContext)) {
                                        this.mCust.volumnkeyWakeup(this.mContext, isScreenOn, this.mPowerManager);
                                    }
                                } else {
                                    if (event.getEventTime() - event.getDownTime() >= 500) {
                                        resetVolumeDownKeyLongPressed();
                                    } else {
                                        cancelPendingQuickCallChordAction();
                                    }
                                }
                            }
                        } else {
                            if (down) {
                                this.mHandler.post(new Runnable() {
                                    public void run() {
                                        Toast toast = Toast.makeText(HwPhoneWindowManager.this.mContext, 33685973, 0);
                                        toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                                        LayoutParams windowParams = toast.getWindowParams();
                                        windowParams.privateFlags |= 16;
                                        toast.show();
                                    }
                                });
                            }
                            return result2 & -2;
                        }
                        break;
                    case 26:
                        cancelSmartKeyLongPressed();
                        if (down) {
                            if (this.mHwScreenOnProximityLock != null && this.mHwScreenOnProximityLock.isShowing() && isScreenOn && !this.mPowerKeyDisTouch && (event.getFlags() & 1024) == 0) {
                                this.mPowerKeyDisTouch = true;
                                this.mPowerKeyDisTouchTime = event.getDownTime();
                                interceptTouchDisableMode();
                            }
                            if (isScreenOn && !this.mScreenRecorderPowerKeyTriggered && (event.getFlags() & 1024) == 0) {
                                this.mScreenRecorderPowerKeyTriggered = true;
                                this.mScreenRecorderPowerKeyTime = event.getDownTime();
                                interceptScreenRecorder();
                            }
                            if (isScreenOn && !this.mSystraceLogPowerKeyTriggered && (event.getFlags() & 1024) == 0) {
                                this.mSystraceLogPowerKeyTriggered = true;
                                this.mSystraceLogFingerPrintTime = 0;
                                this.mSystraceLogVolumeUpKeyTriggered = false;
                            }
                        } else {
                            this.mPowerKeyDisTouch = false;
                            this.mScreenRecorderPowerKeyTriggered = false;
                            cancelPendingScreenRecorderAction();
                            this.mSystraceLogPowerKeyTriggered = false;
                        }
                        break;
                    case 82:
                        if (!this.mHasNavigationBar && down) {
                            if (isScreenInLockTaskMode()) {
                                this.mMenuKeyPress = true;
                                this.mMenuKeyPressTime = event.getDownTime();
                                interceptBackandMenuKey();
                            } else {
                                this.mMenuKeyPress = false;
                                this.mMenuKeyPressTime = 0;
                            }
                        }
                        break;
                    case MemoryConstant.MSG_DIRECT_SWAPPINESS /*303*/:
                        if (mTplusEnabled && down) {
                            if (event.getEventTime() - this.mLastTouchDownUpLeftKeyDownTime < 400) {
                                this.isTouchDownUpLeftDoubleClick = true;
                                this.mTouchDownUpLeftConsumeCount = 2;
                                notifyTouchplusService(MemoryConstant.MSG_DIRECT_SWAPPINESS, 1);
                            }
                            this.mLastTouchDownUpLeftKeyDownTime = event.getEventTime();
                        }
                        break;
                    case 304:
                        if (mTplusEnabled && down) {
                            if (event.getEventTime() - this.mLastTouchDownUpRightKeyDownTime < 400) {
                                this.isTouchDownUpRightDoubleClick = true;
                                this.mTouchDownUpRightConsumeCount = 2;
                                notifyTouchplusService(304, 1);
                            }
                            this.mLastTouchDownUpRightKeyDownTime = event.getEventTime();
                        }
                        break;
                    case MemoryConstant.MSG_PROTECTLRU_CONFIG_UPDATE /*308*/:
                        Log.i(TAG, "KeyEvent.KEYCODE_SMARTKEY in");
                        if (!down) {
                            result = 0;
                            if (this.mHintShown) {
                                this.mHintShown = false;
                                return 0;
                            }
                        } else if (this.mHwScreenOnProximityLock != null && this.mHwScreenOnProximityLock.isShowing() && isScreenOn && !this.mHintShown && (event.getFlags() & 1024) == 0) {
                            str = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("keycode: ");
                            stringBuilder3.append(keyCode);
                            stringBuilder3.append(" is comsumed by disable touch mode.");
                            Log.d(str, stringBuilder3.toString());
                            this.mHwScreenOnProximityLock.forceShowHint();
                            this.mHintShown = true;
                            return 0;
                        } else {
                            result = 0;
                        }
                        if (!isScreenOn) {
                            handleSmartKey(this.mContext, keyEvent, this.mHandler, isScreenOn);
                            return result;
                        }
                        break;
                    case AwareJobSchedulerService.MSG_JOB_EXPIRED /*401*/:
                    case AwareJobSchedulerService.MSG_CHECK_JOB /*402*/:
                    case AwareJobSchedulerService.MSG_REMOVE_JOB /*403*/:
                    case AwareJobSchedulerService.MSG_CONTROLLER_CHANGED /*404*/:
                    case 405:
                        processing_KEYCODE_SOUNDTRIGGER_EVENT(keyCode, this.mContext, isMusicActive(), down, keyguardIsShowingTq());
                        break;
                    case 501:
                    case 502:
                    case 511:
                    case 512:
                    case 513:
                    case 514:
                    case WifiProCommonUtils.RESP_CODE_UNSTABLE /*601*/:
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("event.flags=");
                        stringBuilder.append(flags);
                        stringBuilder.append(" previous mSystraceLogFingerPrintTime=");
                        stringBuilder.append(this.mSystraceLogFingerPrintTime);
                        Log.d(str, stringBuilder.toString());
                        if (flags == 8) {
                            if (!Jlog.isEnable() || !Jlog.isBetaUser() || !down || !isScreenOn || this.mSystraceLogPowerKeyTriggered || this.mSystraceLogVolumeDownKeyTriggered) {
                                return result2 & -2;
                            }
                            this.mSystraceLogFingerPrintTime = event.getDownTime();
                            return result2 & -2;
                        }
                        break;
                    case 702:
                        handleGameEvent(event);
                        break;
                    default:
                        break;
                }
                return super.interceptKeyBeforeQueueing(event, policyFlags);
            }
        }
    }

    private boolean getIsSwich() {
        return isSwich;
    }

    private void setIsSwich(boolean isSwich) {
        isSwich = isSwich;
    }

    private boolean lightScreenOnPcMode(int keyCode) {
        boolean isDreaming = this.mPowerManagerInternal.isUserActivityScreenDimOrDream();
        if (HwPCUtils.isPcCastModeInServer()) {
            if (keyCode == WifiProCommonUtils.RESP_CODE_UNSTABLE) {
                setIsSwich(isDreaming);
            }
            if (getIsSwich() && (keyCode == 3 || keyCode == 4)) {
                isSwich = false;
                return true;
            }
        }
        if (HwPCUtils.isPcCastModeInServer() && (keyCode == 26 || keyCode == 502 || keyCode == 511 || keyCode == 512 || keyCode == 513 || keyCode == 514 || keyCode == 501 || keyCode == WifiProCommonUtils.RESP_CODE_UNSTABLE || keyCode == FrontFingerprintNavigation.FRONT_FINGERPRINT_KEYCODE_HOME_UP)) {
            boolean keyHandled = false;
            try {
                IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
                if (!(pcMgr == null || pcMgr.isScreenPowerOn())) {
                    HwPCUtils.log(TAG, "some key set screen from OFF to ON");
                    pcMgr.setScreenPower(true);
                    keyHandled = true;
                    if (keyCode == 26) {
                        cancelPendingPowerKeyActionForDistouch();
                    }
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("lightScreenOnPcMode ");
                stringBuilder.append(e);
                HwPCUtils.log(str, stringBuilder.toString());
                HwPCDataReporter.getInstance().reportFailLightScreen(1, keyCode, "");
            }
            if (isDreaming || keyHandled) {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                return true;
            }
        }
        return false;
    }

    boolean isRinging() {
        TelecomManager telecomManager = getTelecommService();
        return telecomManager != null && telecomManager.isRinging() && "1".equals(SystemProperties.get("persist.sys.show_incallscreen", "0"));
    }

    public KeyEvent dispatchUnhandledKey(WindowState win, KeyEvent event, int policyFlags) {
        int keyCode = event.getKeyCode();
        boolean isScreenOn = (536870912 & policyFlags) != 0;
        if (keyCode != MemoryConstant.MSG_PROTECTLRU_CONFIG_UPDATE) {
            return super.dispatchUnhandledKey(win, event, policyFlags);
        }
        if (event.getRepeatCount() != 0) {
            Log.d(TAG, "event.getRepeatCount() != 0 so just break");
            return null;
        } else if (SystemProperties.getBoolean("ro.config.fingerOnSmartKey", false) && needDropSmartKey()) {
            return null;
        } else {
            handleSmartKey(this.mContext, event, this.mHandler, isScreenOn);
            return null;
        }
    }

    private boolean isHardwareKeyboardConnected() {
        Log.i(TAG, "isHardwareKeyboardConnected--begin");
        int[] devices = InputDevice.getDeviceIds();
        boolean isConnected = false;
        for (InputDevice device : devices) {
            InputDevice device2 = InputDevice.getDevice(device2);
            if (device2 != null) {
                if (device2.getProductId() != 4817 || device2.getVendorId() != 1455) {
                    if (device2.isExternal() && (device2.getSources() & 257) != 0) {
                        isConnected = true;
                        break;
                    }
                }
                isConnected = true;
                break;
            }
        }
        Log.i(TAG, "isHardwareKeyboardConnected--end");
        return isConnected;
    }

    private boolean isRightKey(int keyCode) {
        if ((keyCode < 7 || keyCode > 16) && (keyCode < 29 || keyCode > 54)) {
            return false;
        }
        return true;
    }

    private void setToolType() {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && this.mStylusGestureListener4PCMode != null) {
            this.mStylusGestureListener4PCMode.setToolType();
        } else if (this.mStylusGestureListener != null) {
            this.mStylusGestureListener.setToolType();
        }
    }

    private void handleGameEvent(KeyEvent event) {
        if (!(event.getAction() == 0) && event.getEventTime() - event.getDownTime() <= 500) {
            Slog.d(TAG, "gamekey arrive, notify GameAssist");
            HwGameAssistManager.notifyKeyEvent();
        }
    }

    private void wakeupNoteEditor(KeyEvent event, boolean isInjected) {
        if (!this.mSystemReady) {
            Log.d(TAG, "system not ready, return");
        } else if (!isInjected) {
            int keyCode = event.getKeyCode();
            boolean isWakeupTimeout = false;
            boolean down = event.getAction() == 0;
            if (keyCode == 704 && down) {
                if (this.mDeviceProvisioned) {
                    if (SystemClock.uptimeMillis() - this.mLastWakeupTime >= 500) {
                        isWakeupTimeout = true;
                    }
                    PowerManager power = (PowerManager) this.mContext.getSystemService("power");
                    if (!power.isScreenOn() && isWakeupTimeout) {
                        Log.d(TAG, "wakeup screen and NoteEditor");
                        Intent notePadEditorIntent = new Intent("android.huawei.intent.action.note.handwriting");
                        notePadEditorIntent.setPackage("com.example.android.notepad");
                        String str;
                        StringBuilder stringBuilder;
                        try {
                            String pkgName = getTopActivity();
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("getTopActivity NoteEditor is:");
                            stringBuilder.append(pkgName);
                            Log.d(str, stringBuilder.toString());
                            if (pkgName == null || !pkgName.equals(NOTEEDITOR_ACTIVITY_NAME)) {
                                this.mContext.startActivityAsUser(notePadEditorIntent, UserHandle.CURRENT_OR_SELF);
                            } else {
                                power.wakeUp(SystemClock.uptimeMillis());
                            }
                        } catch (ActivityNotFoundException ex) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("startActivity failed message : ");
                            stringBuilder.append(ex.getMessage());
                            Log.e(str, stringBuilder.toString());
                        } catch (Exception e) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("startActivityAsUser(): Exception = ");
                            stringBuilder.append(e);
                            Log.e(str, stringBuilder.toString());
                        }
                        this.mLastWakeupTime = SystemClock.uptimeMillis();
                    }
                    return;
                }
                Log.d(TAG, "Device not Provisioned, return");
            } else if (keyCode == 705 && down) {
                Log.d(TAG, "recieved KEYCODE_STYLUS_POWERON");
                PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
                if (powerManager != null) {
                    if (powerManager.isScreenOn()) {
                        if (Global.getInt(this.mContext.getContentResolver(), "stylus_state_activate", 0) == 1) {
                            isWakeupTimeout = true;
                        }
                        if (!isWakeupTimeout) {
                            Global.putInt(this.mContext.getContentResolver(), "stylus_state_activate", 1);
                            Log.d(TAG, "recieve stylus signal and activate stylus.");
                        }
                    } else {
                        powerManager.wakeUp(SystemClock.uptimeMillis());
                    }
                }
            } else if (this.mDeviceProvisioned) {
                if (down) {
                    wakeupSOSPage(keyCode);
                }
            } else {
                Log.d(TAG, "Device not Provisioned, return");
            }
        }
    }

    public long interceptKeyBeforeDispatching(WindowState win, KeyEvent event, int policyFlags) {
        KeyEvent keyEvent = event;
        int i = policyFlags;
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int flags = event.getFlags();
        int origKeyCode = event.getOrigKeyCode();
        boolean down = event.getAction() == 0;
        int deviceID = event.getDeviceId();
        boolean isInjected = (16777216 & i) != 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwPhoneWindowManagerinterceptKeyTi keyCode=");
        stringBuilder.append(keyCode);
        stringBuilder.append(" down=");
        stringBuilder.append(down);
        stringBuilder.append(" repeatCount=");
        stringBuilder.append(repeatCount);
        stringBuilder.append(" isInjected=");
        stringBuilder.append(isInjected);
        stringBuilder.append(" origKeyCode=");
        stringBuilder.append(origKeyCode);
        Flog.i(WifiProCommonUtils.RESP_CODE_INVALID_URL, stringBuilder.toString());
        if (HwPCUtils.isPcCastModeInServer() && event.getEventTime() - this.mLastKeyPointerTime > 500) {
            this.mLastKeyPointerTime = event.getEventTime();
            userActivityOnDesktop();
        }
        if (handleInputEventInPCCastMode(keyEvent)) {
            return -1;
        }
        try {
            if (this.mIHwWindowCallback != null) {
                this.mIHwWindowCallback.interceptKeyBeforeDispatching(keyEvent, i);
            }
        } catch (Exception ex) {
            Log.w(TAG, "mIHwWindowCallback interceptKeyBeforeDispatching threw RemoteException", ex);
        }
        int singleAppResult = getSingAppKeyEventResult(keyCode);
        if (-2 != singleAppResult) {
            return (long) singleAppResult;
        }
        int result = getDisabledKeyEventResult(keyCode);
        if (-2 != result) {
            if (down) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        Toast toast = Toast.makeText(HwPhoneWindowManager.this.mContext, 33686060, 0);
                        toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                        LayoutParams windowParams = toast.getWindowParams();
                        windowParams.privateFlags |= 16;
                        toast.show();
                    }
                });
            }
            return (long) result;
        }
        if (isRightKey(keyCode) && isHardwareKeyboardConnected()) {
            String lastIme = Secure.getString(this.mContext.getContentResolver(), "default_input_method");
            if (lastIme != null && lastIme.contains("com.visionobjects.stylusmobile.v3_2_huawei")) {
                HwInputMethodManager.setDefaultIme("");
                setToolType();
            }
        }
        int result1 = getGameControlKeyReslut(keyEvent);
        if (-2 != result1) {
            Log.i(TAG, "getGameControlKeyReslut return !");
            return (long) result1;
        } else if ((keyCode == 3 || keyCode == 187) && win != null && (win.getAttrs().hwFlags & FLOATING_MASK) == FLOATING_MASK) {
            return 0;
        } else {
            long now;
            long timeoutTime;
            int i2;
            if (origKeyCode != 305 && origKeyCode != MemoryConstant.MSG_PROTECTLRU_SWITCH && origKeyCode != MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO) {
                int i3 = singleAppResult;
                i2 = result1;
            } else if (mTplusEnabled) {
                ContentResolver resolver = this.mContext.getContentResolver();
                int touchPlusOn = System.getInt(resolver, "hw_membrane_touch_enabled", 0);
                singleAppResult = System.getInt(resolver, "hw_membrane_touch_navbar_enabled", 0);
                if (touchPlusOn == 0 || singleAppResult == 0 || (isRinging() && origKeyCode != MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO)) {
                    return -1;
                }
            } else {
                i2 = result1;
            }
            if (keyCode == MemoryConstant.MSG_DIRECT_SWAPPINESS && mTplusEnabled) {
                if (this.isTouchDownUpLeftDoubleClick) {
                    if (!down) {
                        this.mTouchDownUpLeftConsumeCount--;
                        if (this.mTouchDownUpLeftConsumeCount == 0) {
                            this.isTouchDownUpLeftDoubleClick = false;
                        }
                    }
                    return -1;
                } else if (repeatCount == 0) {
                    now = SystemClock.uptimeMillis();
                    timeoutTime = event.getEventTime() + 400;
                    if (now < timeoutTime) {
                        return timeoutTime - now;
                    }
                }
            }
            if (keyCode == 304 && mTplusEnabled) {
                if (this.isTouchDownUpRightDoubleClick) {
                    if (!down) {
                        this.mTouchDownUpRightConsumeCount--;
                        if (this.mTouchDownUpRightConsumeCount == 0) {
                            this.isTouchDownUpRightDoubleClick = false;
                        }
                    }
                    return -1;
                } else if (repeatCount == 0) {
                    now = SystemClock.uptimeMillis();
                    timeoutTime = event.getEventTime() + 400;
                    if (now < timeoutTime) {
                        return timeoutTime - now;
                    }
                }
            }
            if (keyCode == 82) {
                if (mTplusEnabled && origKeyCode == MemoryConstant.MSG_PROTECTLRU_SET_PROTECTRATIO) {
                    ContentResolver resolver2 = this.mContext.getContentResolver();
                    if (1 == System.getInt(resolver2, "hw_membrane_touch_navbar_enabled", 0)) {
                        int i4;
                        if (!down) {
                            i4 = origKeyCode;
                            if (this.mMenuClickedOnlyOnce) {
                                this.mMenuClickedOnlyOnce = false;
                                if (this.mLastFocusNeedsMenu) {
                                    sendHwMenuKeyEvent();
                                } else {
                                    toggleRecentApps();
                                }
                            }
                        } else if (repeatCount == 0) {
                            this.mMenuClickedOnlyOnce = true;
                            ContentResolver contentResolver = resolver2;
                            i4 = origKeyCode;
                        } else if (repeatCount == 1) {
                            this.mMenuClickedOnlyOnce = false;
                            transactToStatusBarService(101, "resentapp", "resentapp", 0);
                        } else {
                            i4 = origKeyCode;
                        }
                        return -1;
                    }
                }
                if (!this.mHasNavigationBar && (268435456 & flags) == 0) {
                    if (!down) {
                        if (this.mMenuClickedOnlyOnce) {
                            this.mMenuClickedOnlyOnce = false;
                            sendHwMenuKeyEvent();
                        }
                        cancelPreloadRecentApps();
                    } else if (repeatCount == 0) {
                        this.mMenuClickedOnlyOnce = true;
                        preloadRecentApps();
                    } else if (repeatCount == 1) {
                        this.mMenuClickedOnlyOnce = false;
                        toggleRecentApps();
                    }
                    return -1;
                }
            }
            long now2;
            if (this.mVolumeUpKeyDisTouch && !this.mPowerKeyDisTouch && (flags & 1024) == 0) {
                now2 = SystemClock.uptimeMillis();
                repeatCount = this.mVolumeUpKeyDisTouchTime + 150;
                if (now2 < repeatCount) {
                    return repeatCount - now2;
                }
                return -1;
            }
            int repeatCount2 = repeatCount;
            int flags2 = flags;
            if (keyCode == 24) {
                if (this.mVolumeUpKeyConsumedByDisTouch) {
                    if (!down) {
                        this.mVolumeUpKeyConsumedByDisTouch = false;
                        this.mHintShown = false;
                    }
                    return -1;
                } else if (this.mHintShown) {
                    if (!down) {
                        this.mHintShown = false;
                    }
                    return -1;
                }
            }
            if (isNeedPassEventToCloud(keyCode)) {
                return 0;
            }
            boolean handleResult = handleDesktopKeyEvent(keyEvent);
            if (handleResult) {
                return -1;
            }
            if (!(keyCode == 25 || keyCode == 187)) {
                switch (keyCode) {
                    case 3:
                    case 4:
                        break;
                    default:
                        boolean z = handleResult;
                        break;
                }
            }
            if (this.mHintShown) {
                if (!down) {
                    this.mHintShown = false;
                }
                return -1;
            }
            boolean isFrontFpNavi = FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION;
            boolean isSupportTrikey = FrontFingerPrintSettings.isSupportTrikey();
            boolean isMMITest = isMMITesting();
            stringBuilder = new StringBuilder();
            stringBuilder.append("HwPhoneWindowManagerdeviceID:");
            stringBuilder.append(deviceID);
            stringBuilder.append(" isFrontFpNavi:");
            stringBuilder.append(isFrontFpNavi);
            stringBuilder.append(" isSupportTrikey:");
            stringBuilder.append(isSupportTrikey);
            stringBuilder.append(" isMMITest:");
            stringBuilder.append(isMMITest);
            Flog.i(WifiProCommonUtils.RESP_CODE_INVALID_URL, stringBuilder.toString());
            if (deviceID <= 0 || !isFrontFpNavi || !isSupportTrikey || isMMITest || keyCode != 187) {
                repeatCount = flags2;
                if ((repeatCount & 1024) == 0) {
                    long now3;
                    if (this.mScreenRecorderVolumeUpKeyTriggered && !this.mScreenRecorderPowerKeyTriggered) {
                        now3 = SystemClock.uptimeMillis();
                        now2 = this.mScreenRecorderVolumeUpKeyTime + 150;
                        if (now3 < now2) {
                            return now2 - now3;
                        }
                    }
                    if (keyCode == 24 && this.mScreenRecorderVolumeUpKeyConsumed) {
                        if (!down) {
                            this.mScreenRecorderVolumeUpKeyConsumed = false;
                        }
                        return -1;
                    }
                    String str;
                    if (this.mSystraceLogVolumeUpKeyTriggered) {
                        now3 = SystemClock.uptimeMillis();
                        now2 = this.mSystraceLogVolumeUpKeyTime + 150;
                        if (now3 < now2) {
                            str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("keyCode=");
                            stringBuilder2.append(keyCode);
                            stringBuilder2.append(" down=");
                            stringBuilder2.append(down);
                            stringBuilder2.append(" in queue: now=");
                            stringBuilder2.append(now3);
                            stringBuilder2.append(" timeout=");
                            stringBuilder2.append(now2);
                            Log.d(str, stringBuilder2.toString());
                            return now2 - now3;
                        }
                    }
                    if (keyCode == 24 && this.mSystraceLogVolumeUpKeyConsumed) {
                        if (!down) {
                            this.mSystraceLogVolumeUpKeyConsumed = false;
                        }
                        str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("systracelog volumeup down=");
                        stringBuilder3.append(down);
                        stringBuilder3.append(" leave queue");
                        Log.d(str, stringBuilder3.toString());
                        return -1;
                    }
                }
                return super.interceptKeyBeforeDispatching(win, event, policyFlags);
            } else if (isTrikeyNaviKeycodeFromLON(isInjected, isExcluedRecentScene())) {
                return -1;
            } else {
                sendLightTimeoutMsg();
                boolean z2;
                if (!down) {
                    z2 = isFrontFpNavi;
                    handleResult = this.mRecentTrikeyHandled;
                    if (this.mRecentTrikeyHandled) {
                        isFrontFpNavi = true;
                    } else {
                        isFrontFpNavi = true;
                        this.mRecentTrikeyHandled = true;
                        this.mHandlerEx.removeMessages(MSG_TRIKEY_RECENT_LONG_PRESS);
                    }
                    if (!handleResult) {
                        if (this.mTrikeyNaviMode == isFrontFpNavi) {
                            startHwVibrate(VIBRATOR_SHORT_PRESS_FOR_FRONT_FP);
                            sendKeyEvent(4);
                        } else if (this.mTrikeyNaviMode == 0) {
                            Flog.bdReport(this.mContext, 17);
                            startHwVibrate(VIBRATOR_SHORT_PRESS_FOR_FRONT_FP);
                            toggleRecentApps();
                        }
                    }
                } else if (repeatCount2 == 0) {
                    this.mRecentTrikeyHandled = false;
                    Message msg = this.mHandlerEx.obtainMessage(MSG_TRIKEY_RECENT_LONG_PRESS);
                    msg.setAsynchronous(true);
                    this.mHandlerEx.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                    if (!this.mTrikeyNaviMode) {
                        preloadRecentApps();
                    }
                } else {
                    z2 = isFrontFpNavi;
                }
                return true;
            }
        }
    }

    private void showStartMenu() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.showStartMenu();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException showStartMenu");
            }
        }
    }

    private void screenshotPc() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.screenshotPc();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException screenshotPc");
            }
        }
    }

    private void closeTopWindow() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.closeTopWindow();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException closeTopWindow");
            }
        }
    }

    private void triggerSwitchTaskView(boolean show) {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.triggerSwitchTaskView(show);
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException triggerSwitchTaskView");
            }
        }
    }

    private void lockScreen(boolean lock) {
        if (this.mWindowManagerInternal != null) {
            this.mWindowManagerInternal.setFocusedDisplayId(0, "lockScreen");
        }
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.lockScreen(lock);
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException lockScreen");
            }
        }
    }

    private void toggleHome() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.toggleHome();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException toggleHome");
            }
        }
    }

    private void dispatchKeyEventForExclusiveKeyboard(KeyEvent ke) {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.dispatchKeyEventForExclusiveKeyboard(ke);
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException dispatchKeyEvent");
            }
        }
    }

    private void userActivityOnDesktop() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.userActivityOnDesktop();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException userActivityOnDesktop");
            }
        }
    }

    private final void sendHwMenuKeyEvent() {
        int[] actions = new int[]{0, 1};
        KeyEvent ev = null;
        long curTime = 0;
        for (int keyEvent : actions) {
            curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, keyEvent, 82, 0, 0, -1, 0, 268435464, 257), 0);
        }
    }

    protected void launchAssistAction(String hint, int deviceId) {
        if (checkPackageInstalled("com.google.android.googlequicksearchbox")) {
            super.launchAssistAction(hint, deviceId);
            return;
        }
        sendCloseSystemWindows();
        boolean z = true;
        if (Secure.getInt(this.mContext.getContentResolver(), "hw_long_home_voice_assistant", 0) != 1) {
            z = false;
        }
        boolean enableVoiceAssistant = z;
        if (IS_LONG_HOME_VASSITANT && enableVoiceAssistant) {
            performHapticFeedbackLw(null, 0, false);
            try {
                String intent = "android.intent.action.ASSIST";
                if (checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE)) {
                    intent = VOICE_ASSISTANT_ACTION;
                }
                this.mContext.startActivity(new Intent(intent).setFlags(268435456));
            } catch (ActivityNotFoundException anfe) {
                Slog.w(TAG, "No activity to handle voice assistant action.", anfe);
            }
        }
    }

    private boolean checkPackageInstalled(String packageName) {
        try {
            this.mContext.getPackageManager().getPackageInfo(packageName, 128);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private boolean isHwDarkTheme(Context context, int themeId) {
        boolean z = false;
        try {
            if (context.getResources().getResourceName(themeId).indexOf("Emui.Dark") >= 0) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isMusicActive() {
        if (((AudioManager) this.mContext.getSystemService("audio")) != null) {
            return AudioSystem.isStreamActive(3, 0);
        }
        Log.w(TAG, "isMusicActive: couldn't get AudioManager reference");
        return false;
    }

    boolean isDeviceProvisioned() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    /*  JADX ERROR: NullPointerException in pass: ProcessVariables
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.regions.ProcessVariables.addToUsageMap(ProcessVariables.java:271)
        	at jadx.core.dex.visitors.regions.ProcessVariables.access$000(ProcessVariables.java:31)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processInsn(ProcessVariables.java:152)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processBlockTraced(ProcessVariables.java:129)
        	at jadx.core.dex.visitors.regions.TracedRegionVisitor.processBlock(TracedRegionVisitor.java:23)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:53)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
        	at jadx.core.dex.visitors.regions.ProcessVariables.visit(ProcessVariables.java:183)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    void handleVolumeKey(int r5, int r6) {
        /*
        r4 = this;
        r0 = getAudioService();
        if (r0 != 0) goto L_0x0007;
    L_0x0006:
        return;
    L_0x0007:
        r1 = r4.mBroadcastWakeLock;	 Catch:{ RemoteException -> 0x0029 }
        if (r1 == 0) goto L_0x0010;	 Catch:{ RemoteException -> 0x0029 }
    L_0x000b:
        r1 = r4.mBroadcastWakeLock;	 Catch:{ RemoteException -> 0x0029 }
        r1.acquire();	 Catch:{ RemoteException -> 0x0029 }
        r1 = 24;	 Catch:{ RemoteException -> 0x0029 }
        if (r6 != r1) goto L_0x0017;	 Catch:{ RemoteException -> 0x0029 }
    L_0x0015:
        r1 = 1;	 Catch:{ RemoteException -> 0x0029 }
        goto L_0x0018;	 Catch:{ RemoteException -> 0x0029 }
    L_0x0017:
        r1 = -1;	 Catch:{ RemoteException -> 0x0029 }
    L_0x0018:
        r2 = 0;	 Catch:{ RemoteException -> 0x0029 }
        r3 = r4.mContext;	 Catch:{ RemoteException -> 0x0029 }
        r3 = r3.getOpPackageName();	 Catch:{ RemoteException -> 0x0029 }
        r0.adjustStreamVolume(r5, r1, r2, r3);	 Catch:{ RemoteException -> 0x0029 }
        r1 = r4.mBroadcastWakeLock;
        if (r1 == 0) goto L_0x003a;
    L_0x0026:
        goto L_0x0035;
    L_0x0027:
        r1 = move-exception;
        goto L_0x003b;
    L_0x0029:
        r1 = move-exception;
        r2 = "HwPhoneWindowManager";	 Catch:{ all -> 0x0027 }
        r3 = "IAudioService.adjust*StreamVolume() threw RemoteException";	 Catch:{ all -> 0x0027 }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x0027 }
        r1 = r4.mBroadcastWakeLock;
        if (r1 == 0) goto L_0x003a;
    L_0x0035:
        r1 = r4.mBroadcastWakeLock;
        r1.release();
    L_0x003a:
        return;
    L_0x003b:
        r2 = r4.mBroadcastWakeLock;
        if (r2 == 0) goto L_0x0044;
    L_0x003f:
        r2 = r4.mBroadcastWakeLock;
        r2.release();
    L_0x0044:
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.HwPhoneWindowManager.handleVolumeKey(int, int):void");
    }

    static IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    private void sendVolumeDownKeyPressed() {
        this.mHandler.postDelayed(this.mHandleVolumeDownKey, 500);
    }

    private void cancelVolumeDownKeyPressed() {
        this.mHandler.removeCallbacks(this.mHandleVolumeDownKey);
    }

    private void resetVolumeDownKeyPressed() {
        if (this.mHandler.hasCallbacks(this.mHandleVolumeDownKey)) {
            this.mHandler.removeCallbacks(this.mHandleVolumeDownKey);
            this.mHandler.post(this.mHandleVolumeDownKey);
        }
    }

    private void interceptQuickCallChord() {
        this.mHandler.postDelayed(this.mVolumeDownLongPressed, 500);
    }

    private void cancelPendingQuickCallChordAction() {
        this.mHandler.removeCallbacks(this.mVolumeDownLongPressed);
        resetVolumeDownKeyPressed();
    }

    private void resetVolumeDownKeyLongPressed() {
        if (this.mHandler.hasCallbacks(this.mVolumeDownLongPressed)) {
            this.mHandler.removeCallbacks(this.mVolumeDownLongPressed);
            this.mHandler.post(this.mVolumeDownLongPressed);
        }
    }

    private void initQuickcall() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        if (this.mPowerManager != null) {
            this.mBroadcastWakeLock = this.mPowerManager.newWakeLock(1, "HwPhoneWindowManager.mBroadcastWakeLock");
            this.mVolumeDownWakeLock = this.mPowerManager.newWakeLock(1, "HwPhoneWindowManager.mVolumeDownWakeLock");
        }
        this.mHeadless = "1".equals(SystemProperties.get("ro.config.headless", "0"));
    }

    private void notifyRapidCaptureService(String command) {
        if (this.mSystemReady) {
            Intent intent = new Intent(HUAWEI_RAPIDCAPTURE_START_MODE);
            intent.setPackage("com.huawei.camera");
            intent.putExtra(SMARTKEY_TAG, command);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
            if (this.mVolumeDownWakeLock != null) {
                this.mVolumeDownWakeLock.acquire(500);
            }
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("start Rapid Capture Service, command:");
                stringBuilder.append(extras.get(SMARTKEY_TAG));
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    public void showHwTransientBars() {
        if (this.mStatusBar != null) {
            requestHwTransientBars(this.mStatusBar);
        }
    }

    private void notifyTouchplusService(int kcode, int kval) {
        Intent intent = new Intent("com.huawei.membranetouch.action.MT_MANAGER");
        intent.putExtra("keycode", kcode);
        intent.putExtra("keyvalue", kval);
        intent.setPackage("com.huawei.membranetouch");
        this.mContext.startService(intent);
    }

    public void transactToStatusBarService(int code, String transactName, String paramName, int paramValue) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            IBinder statusBarServiceBinder = getHWStatusBarService().asBinder();
            if (statusBarServiceBinder != null) {
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                if (paramName != null) {
                    data.writeInt(paramValue);
                }
                statusBarServiceBinder.transact(code, data, reply, 0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transactToStatusBarService four params->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
        reply.recycle();
        data.recycle();
    }

    protected void transactToStatusBarService(int code, String transactName, int isEmuiStyle, int statusbarColor, int navigationBarColor, int isEmuiLightStyle) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (getHWStatusBarService() != null) {
                IBinder statusBarServiceBinder = getHWStatusBarService().asBinder();
                if (statusBarServiceBinder != null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transact:");
                    stringBuilder.append(transactName);
                    stringBuilder.append(" to status bar service");
                    Log.d(str, stringBuilder.toString());
                    data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                    data.writeInt(isEmuiStyle);
                    data.writeInt(statusbarColor);
                    data.writeInt(navigationBarColor);
                    data.writeInt(isEmuiLightStyle);
                    statusBarServiceBinder.transact(code, data, reply, 0);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transactToStatusBarService->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
        reply.recycle();
        data.recycle();
    }

    public void updateSystemUiColorLw(WindowState win) {
        WindowState windowState = win;
        if (windowState != null) {
            String str;
            StringBuilder stringBuilder;
            if (isCoverWindow(win)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateSystemUiColorLw isCoverWindow return ");
                stringBuilder.append(windowState);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            LayoutParams attrs = win.getAttrs();
            if (this.mLastColorWin != windowState || this.mLastStatusBarColor != attrs.statusBarColor || this.mLastNavigationBarColor != attrs.navigationBarColor) {
                boolean colorChanged;
                boolean z;
                boolean isFloating = getFloatingValue(attrs.isEmuiStyle);
                boolean isPopup = attrs.type == 1000 || attrs.type == 1002 || attrs.type == HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS || attrs.type == HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL || attrs.type == 2003;
                if (attrs.type == 3) {
                }
                boolean isTouchExplrEnabled = this.mAccessibilityManager.isTouchExplorationEnabled();
                int isEmuiStyle = getEmuiStyleValue(attrs.isEmuiStyle);
                int statusBarColor = attrs.statusBarColor;
                int navigationBarColor = attrs.navigationBarColor;
                int isEmuiLightStyle = getEmuiLightStyleValue(attrs.hwFlags);
                if (isTouchExplrEnabled) {
                    colorChanged = isTouchExplrEnabled != this.mIsTouchExplrEnabled;
                    isEmuiStyle = -2;
                    isEmuiLightStyle = -1;
                } else {
                    z = (this.mLastStatusBarColor == statusBarColor && this.mLastNavigationBarColor == navigationBarColor) ? false : true;
                    colorChanged = z;
                }
                z = isEmuiStyleChanged(isEmuiStyle, isEmuiLightStyle);
                boolean isStatusBarOrKeyGuard = windowState == this.mStatusBar || attrs.type == HwArbitrationDEFS.MSG_VPN_STATE_OPEN || isKeyguardHostWindow(attrs);
                boolean isDockDivider = attrs.type == HwArbitrationDEFS.MSG_Recovery_Flag_By_Notification;
                boolean isInBottomOrRightWindow = win.getWindowingMode() != 3 && win.isInMultiWindowMode();
                boolean ignoreWindow = isStatusBarOrKeyGuard || isFloating || isPopup || isDockDivider || isInBottomOrRightWindow;
                boolean changed = (z && !ignoreWindow) || !(z || ignoreWindow || !colorChanged);
                if (ignoreWindow) {
                } else {
                    boolean z2 = isInBottomOrRightWindow;
                    windowState.setCanCarryColors(true);
                }
                this.mLastNavigationBarColor = isTouchExplrEnabled ? -16777216 : navigationBarColor;
                this.mLastIsEmuiLightStyle = isEmuiLightStyle;
                if (changed) {
                    this.mLastStatusBarColor = isTouchExplrEnabled ? -16777216 : statusBarColor;
                    this.mLastIsEmuiStyle = isEmuiStyle;
                    this.mIsTouchExplrEnabled = isTouchExplrEnabled;
                    this.mLastColorWin = windowState;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateSystemUiColorLw window=");
                    stringBuilder.append(windowState);
                    stringBuilder.append(",EmuiStyle=");
                    stringBuilder.append(isEmuiStyle);
                    stringBuilder.append(",StatusBarColor=0x");
                    stringBuilder.append(Integer.toHexString(statusBarColor));
                    stringBuilder.append(",NavigationBarColor=0x");
                    stringBuilder.append(Integer.toHexString(navigationBarColor));
                    stringBuilder.append(", mLastIsEmuiLightStyle=");
                    stringBuilder.append(this.mLastIsEmuiLightStyle);
                    stringBuilder.append(", mForceNotchStatusBar=");
                    stringBuilder.append(this.mForceNotchStatusBar);
                    Slog.v(str, stringBuilder.toString());
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            if ((!HwPhoneWindowManager.this.mIsNotchSwitchOpen && !HwPhoneWindowManager.this.mForceNotchStatusBar && !HwPhoneWindowManager.this.mIsForceSetStatusBar) || HwPhoneWindowManager.this.mIsNotchSwitchOpen) {
                                if (HwPhoneWindowManager.this.mLastIsEmuiStyle == -1) {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        Slog.v(HwPhoneWindowManager.TAG, "InterruptedException is happed in method updateSystemUiColorLw");
                                    }
                                }
                                HwPhoneWindowManager.this.transactToStatusBarService(106, "setSystemUIColor", HwPhoneWindowManager.this.mLastIsEmuiStyle, HwPhoneWindowManager.this.mLastStatusBarColor, HwPhoneWindowManager.this.mLastNavigationBarColor, HwPhoneWindowManager.this.mLastIsEmuiLightStyle);
                            }
                        }
                    });
                } else {
                    boolean z3 = isPopup;
                }
            }
        }
    }

    protected int getEmuiStyleValue(int styleValue) {
        return styleValue == -1 ? -1 : Integer.MAX_VALUE & styleValue;
    }

    protected int getEmuiLightStyleValue(int styleValue) {
        return (styleValue & 16) != 0 ? 1 : -1;
    }

    protected boolean isEmuiStyleChanged(int isEmuiStyle, int isEmuiLightStyle) {
        return (this.mLastIsEmuiStyle == isEmuiStyle && this.mLastIsEmuiLightStyle == isEmuiLightStyle) ? false : true;
    }

    protected boolean getFloatingValue(int styleValue) {
        return styleValue != -1 && (styleValue & FLOATING_MASK) == FLOATING_MASK;
    }

    public void onTouchExplorationStateChanged(boolean enabled) {
        updateSystemUiColorLw(getCurrentWin());
    }

    protected void hwInit() {
        this.mAccessibilityManager.addTouchExplorationStateChangeListener(this);
    }

    IStatusBarService getHWStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    private void notifyFingerOptical() {
        this.mFingerprintHardwareType = FingerprintManagerEx.getHardwareType();
        this.mWindowManagerFuncs.registerPointerEventListener(new PointerEventListener() {
            public void onPointerEvent(MotionEvent motionEvent) {
                if (HwPhoneWindowManager.this.mFingerprintHardwareType == -1) {
                    HwPhoneWindowManager.this.mFingerprintHardwareType = FingerprintManagerEx.getHardwareType();
                    String str = HwPhoneWindowManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("have not get HardwareType, try again and type is");
                    stringBuilder.append(HwPhoneWindowManager.this.mFingerprintHardwareType);
                    Log.d(str, stringBuilder.toString());
                }
                if (HwPhoneWindowManager.this.mFingerprintHardwareType != 1) {
                    return;
                }
                if (motionEvent.getActionMasked() == 1) {
                    FingerViewController.getInstance(HwPhoneWindowManager.this.mContext).notifyTouchUp(motionEvent.getRawX(), motionEvent.getRawY());
                } else if (motionEvent.getActionMasked() == 6) {
                    int actionIndex = motionEvent.getActionIndex();
                    FingerViewController.getInstance(HwPhoneWindowManager.this.mContext).notifyTouchUp(motionEvent.getX(actionIndex), motionEvent.getY(actionIndex));
                }
            }
        });
    }

    private void interceptTouchDisableMode() {
        if (this.mVolumeUpKeyDisTouch && this.mPowerKeyDisTouch && !this.mVolumeDownKeyDisTouch) {
            long now = SystemClock.uptimeMillis();
            if (now <= this.mVolumeUpKeyDisTouchTime + 150 && now <= this.mPowerKeyDisTouchTime + 150) {
                this.mVolumeUpKeyConsumedByDisTouch = true;
                cancelPendingPowerKeyActionForDistouch();
                if (this.mHwScreenOnProximityLock != null) {
                    this.mHwScreenOnProximityLock.releaseLock(0);
                }
            }
        }
    }

    public boolean checkPhoneOFFHOOK() {
        int callState = ((TelephonyManager) this.mContext.getSystemService("phone")).getCallState();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("callState : ");
        stringBuilder.append(callState);
        Log.d(str, stringBuilder.toString());
        return callState == 2;
    }

    public boolean checkHeadSetIsConnected() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        boolean headSetConnectedState = false;
        if (audioManager == null) {
            return false;
        }
        if (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn() || audioManager.isBluetoothScoOn()) {
            headSetConnectedState = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkHeadSetIsConnected : ");
        stringBuilder.append(headSetConnectedState);
        Log.d(str, stringBuilder.toString());
        return headSetConnectedState;
    }

    public void screenTurningOn(ScreenOnListener screenOnListener) {
        super.screenTurningOn(screenOnListener);
        if (this.mContext == null) {
            Log.d(TAG, "Context object is null.");
            return;
        }
        boolean z = true;
        if (!(this.mFalseTouchMonitor == null || !this.mFalseTouchMonitor.isFalseTouchFeatureOn() || this.mScreenOnForFalseTouch)) {
            this.mScreenOnForFalseTouch = true;
            this.mWindowManagerFuncs.registerPointerEventListener(this.mFalseTouchMonitor.getEventListener());
        }
        if (System.getIntForUser(this.mContext.getContentResolver(), KEY_TOUCH_DISABLE_MODE, 1, ActivityManager.getCurrentUser()) <= 0 || "factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            z = false;
        }
        boolean isModeEnabled = z;
        if (this.mHwScreenOnProximityLock != null && isModeEnabled && this.mDeviceProvisioned) {
            z = checkPhoneOFFHOOK();
            if (!z || (z && checkHeadSetIsConnected())) {
                this.mHwScreenOnProximityLock.acquireLock(this);
                this.mHwScreenOnProximityLock.registerDeviceListener();
            }
        }
        if (SystemProperties.getBoolean("ro.config.hw_easywakeup", false) && this.mSystemReady) {
            EasyWakeUpManager mWakeUpManager = EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate);
            if (mWakeUpManager != null) {
                mWakeUpManager.turnOffSensorListener();
            }
        }
    }

    public void screenTurnedOn() {
        super.screenTurnedOn();
        if (this.mSystemReady) {
            Slog.d(TAG, "screenTurnedOn");
            if (this.mBooted) {
                PickUpWakeScreenManager.getInstance(this.mContext, this.mHandler, this.mWindowManagerFuncs, this.mKeyguardDelegate).enablePickupMotionOrNot(false);
            }
        }
    }

    public void screenTurnedOff() {
        super.screenTurnedOff();
        if (this.mFalseTouchMonitor != null && this.mFalseTouchMonitor.isFalseTouchFeatureOn() && this.mScreenOnForFalseTouch) {
            this.mScreenOnForFalseTouch = false;
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.mFalseTouchMonitor.getEventListener());
        }
        if (this.mHwScreenOnProximityLock != null) {
            this.mHwScreenOnProximityLock.releaseLock(1);
            this.mHwScreenOnProximityLock.unregisterDeviceListener();
        }
        if (SystemProperties.getBoolean("ro.config.hw_easywakeup", false) && this.mSystemReady) {
            EasyWakeUpManager mWakeUpManager = EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate);
            if (mWakeUpManager != null) {
                mWakeUpManager.turnOnSensorListener();
            }
        }
        try {
            if (this.mIHwWindowCallback != null) {
                this.mIHwWindowCallback.screenTurnedOff();
            }
        } catch (Exception ex) {
            Log.w(TAG, "mIHwWindowCallback threw RemoteException", ex);
        }
        if (this.mSystemReady) {
            Slog.d(TAG, "screenTurnedOff");
            PickUpWakeScreenManager.getInstance(this.mContext, this.mHandler, this.mWindowManagerFuncs, this.mKeyguardDelegate).enablePickupMotionOrNot(true);
        }
    }

    public int selectAnimationLw(WindowState win, int transit) {
        if (win != this.mNavigationBar || this.mNavigationBarPosition == 4 || (transit != 1 && transit != 3)) {
            return super.selectAnimationLw(win, transit);
        }
        return mIsHwNaviBar ? 0 : 17432621;
    }

    public void onConfigurationChanged() {
        super.onConfigurationChanged();
        if (this.mNavigationBarPolicy != null) {
            if (this.mNavigationBarPolicy.mMinNavigationBar) {
                this.mNavigationBarHeightForRotationDefault = (int[]) this.mNavigationBarHeightForRotationMin.clone();
                this.mNavigationBarWidthForRotationDefault = (int[]) this.mNavigationBarWidthForRotationMin.clone();
            } else {
                this.mNavigationBarHeightForRotationDefault = (int[]) this.mNavigationBarHeightForRotationMax.clone();
                this.mNavigationBarWidthForRotationDefault = (int[]) this.mNavigationBarWidthForRotationMax.clone();
            }
        }
        if (this.mGestureNavManager != null) {
            this.mGestureNavManager.onConfigurationChanged();
        }
    }

    public void updateNavigationBar(boolean minNaviBar) {
        if (this.mNavigationBarPolicy != null) {
            if (minNaviBar) {
                this.mNavigationBarHeightForRotationDefault = (int[]) this.mNavigationBarHeightForRotationMin.clone();
                this.mNavigationBarWidthForRotationDefault = (int[]) this.mNavigationBarWidthForRotationMin.clone();
            } else {
                this.mNavigationBarHeightForRotationDefault = (int[]) this.mNavigationBarHeightForRotationMax.clone();
                this.mNavigationBarWidthForRotationDefault = (int[]) this.mNavigationBarWidthForRotationMax.clone();
            }
            this.mNavigationBarPolicy.updateNavigationBar(minNaviBar);
        }
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        int width = fullWidth;
        if ((uiMode & 15) == 3) {
            width = super.getNonDecorDisplayWidth(fullWidth, fullHeight, rotation, uiMode, displayId, displayCutout);
        } else if (displayId == 0 && this.mHasNavigationBar) {
            if (this.mNavigationBarCanMove && fullWidth > fullHeight) {
                width = (this.mNavigationBarPolicy == null || !this.mNavigationBarPolicy.mMinNavigationBar) ? !getNavibarAlignLeftWhenLand() ? width - this.mNavigationBarWidthForRotationMax[rotation] : width - this.mContext.getResources().getDimensionPixelSize(34472115) : width - this.mNavigationBarWidthForRotationMin[rotation];
            }
            if (displayCutout != null) {
                width -= displayCutout.getSafeInsetLeft() + displayCutout.getSafeInsetRight();
            }
        } else if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            return width;
        } else {
            return width;
        }
        return width;
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        int height = fullHeight;
        if ((uiMode & 15) == 3) {
            return super.getNonDecorDisplayHeight(fullWidth, fullHeight, rotation, uiMode, displayId, displayCutout);
        }
        if (displayId == 0 && this.mHasNavigationBar) {
            if (!this.mNavigationBarCanMove || fullWidth < fullHeight) {
                if (this.mNavigationBarPolicy == null || !this.mNavigationBarPolicy.mMinNavigationBar) {
                    height -= this.mNavigationBarHeightForRotationMax[rotation];
                } else {
                    height -= this.mNavigationBarHeightForRotationMin[rotation];
                }
            }
            if (displayCutout != null) {
                return height - (displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom());
            }
            return height;
        } else if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId) && getNavigationBarExternal() != null && getNavigationBarExternal().isVisibleLw()) {
            return height - getNavigationBarHeightExternal();
        } else {
            return height;
        }
    }

    public void setInputMethodWindowVisible(boolean visible) {
        this.mInputMethodWindowVisible = visible;
    }

    public void setNaviBarFlag(boolean flag) {
        if (flag != this.isNavibarHide) {
            this.isNavibarHide = flag;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNeedHideWindow setFlag isNavibarHide is ");
            stringBuilder.append(this.isNavibarHide);
            HwSlog.d(str, stringBuilder.toString());
        }
    }

    public int getNaviBarHeightForRotationMin(int index) {
        return this.mNavigationBarHeightForRotationMin[index];
    }

    public int getNaviBarWidthForRotationMin(int index) {
        return this.mNavigationBarWidthForRotationMin[index];
    }

    public int getNaviBarHeightForRotationMax(int index) {
        return this.mNavigationBarHeightForRotationMax[index];
    }

    public int getNaviBarWidthForRotationMax(int index) {
        return this.mNavigationBarWidthForRotationMax[index];
    }

    public void setInitialDisplaySize(Display display, int width, int height, int density) {
        if (density == 0) {
            Log.e(TAG, "density is 0");
            return;
        }
        super.setInitialDisplaySize(display, width, height, density);
        if (this.mContext != null) {
            initNavigationBarHightExternal(display, width, height);
            Resources res = this.mContext.getResources();
            ContentResolver resolver = this.mContext.getContentResolver();
            int[] iArr = this.mNavigationBarHeightForRotationMax;
            int i = this.mPortraitRotation;
            int[] iArr2 = this.mNavigationBarHeightForRotationMax;
            int i2 = this.mUpsideDownRotation;
            int dimensionPixelSize = res.getDimensionPixelSize(17105186);
            iArr2[i2] = dimensionPixelSize;
            iArr[i] = dimensionPixelSize;
            iArr = this.mNavigationBarHeightForRotationMax;
            i = this.mLandscapeRotation;
            iArr2 = this.mNavigationBarHeightForRotationMax;
            i2 = this.mSeascapeRotation;
            dimensionPixelSize = res.getDimensionPixelSize(17105188);
            iArr2[i2] = dimensionPixelSize;
            iArr[i] = dimensionPixelSize;
            iArr = this.mNavigationBarHeightForRotationMin;
            i = this.mPortraitRotation;
            iArr2 = this.mNavigationBarHeightForRotationMin;
            i2 = this.mUpsideDownRotation;
            int[] iArr3 = this.mNavigationBarHeightForRotationMin;
            int i3 = this.mLandscapeRotation;
            int[] iArr4 = this.mNavigationBarHeightForRotationMin;
            int i4 = this.mSeascapeRotation;
            int i5 = System.getInt(resolver, "navigationbar_height_min", 0);
            iArr4[i4] = i5;
            iArr3[i3] = i5;
            iArr2[i2] = i5;
            iArr[i] = i5;
            iArr = this.mNavigationBarWidthForRotationMax;
            i = this.mPortraitRotation;
            iArr2 = this.mNavigationBarWidthForRotationMax;
            i2 = this.mUpsideDownRotation;
            iArr3 = this.mNavigationBarWidthForRotationMax;
            i3 = this.mLandscapeRotation;
            iArr4 = this.mNavigationBarWidthForRotationMax;
            i4 = this.mSeascapeRotation;
            i5 = res.getDimensionPixelSize(17105191);
            iArr4[i4] = i5;
            iArr3[i3] = i5;
            iArr2[i2] = i5;
            iArr[i] = i5;
            iArr = this.mNavigationBarWidthForRotationMin;
            i = this.mPortraitRotation;
            iArr2 = this.mNavigationBarWidthForRotationMin;
            i2 = this.mUpsideDownRotation;
            iArr3 = this.mNavigationBarWidthForRotationMin;
            i3 = this.mLandscapeRotation;
            iArr4 = this.mNavigationBarWidthForRotationMin;
            i4 = this.mSeascapeRotation;
            i5 = System.getInt(resolver, "navigationbar_width_min", 0);
            iArr4[i4] = i5;
            iArr3[i3] = i5;
            iArr2[i2] = i5;
            iArr[i] = i5;
        }
    }

    protected boolean computeNaviBarFlag() {
        LayoutParams focusAttrs = this.mFocusedWindow != null ? this.mFocusedWindow.getAttrs() : null;
        boolean z = false;
        int type = focusAttrs != null ? focusAttrs.type : 0;
        boolean forceNavibar = focusAttrs != null && (focusAttrs.hwFlags & 1) == 1;
        boolean keyguardOn = type == 2101 || type == 2100;
        boolean iskeyguardDialog = type == HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS && keyguardOn();
        boolean dreamOn = focusAttrs != null && focusAttrs.type == HwArbitrationDEFS.MSG_MPLINK_AI_DEVICE_COEX_MODE;
        boolean isNeedHideNaviBarWin = (focusAttrs == null || (focusAttrs.privateFlags & FLOATING_MASK) == 0) ? false : true;
        if (this.mStatusBar == this.mFocusedWindow) {
            return false;
        }
        if (iskeyguardDialog && !forceNavibar) {
            return true;
        }
        if (dreamOn) {
            return false;
        }
        if (keyguardOn || isNeedHideNaviBarWin) {
            return true;
        }
        if (this.isNavibarHide && !this.mInputMethodWindowVisible) {
            z = true;
        }
        return z;
    }

    public boolean isNaviBarMini() {
        if (this.mNavigationBarPolicy == null || !this.mNavigationBarPolicy.mMinNavigationBar) {
            return false;
        }
        return true;
    }

    public boolean swipeFromTop() {
        if (Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
            return true;
        }
        if (!mIsHwNaviBar) {
            return false;
        }
        if (isLastImmersiveMode()) {
            requestHwTransientBars(this.mStatusBar);
        } else {
            requestTransientStatusBars();
        }
        return true;
    }

    public boolean swipeFromBottom() {
        if (Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
            return true;
        }
        if (mIsHwNaviBar && isLastImmersiveMode() && this.mNavigationBar != null && this.mNavigationBarPosition == 4) {
            requestHwTransientBars(this.mNavigationBar);
            return true;
        } else if (GestureNavConst.isGestureNavEnabled(this.mContext, -2)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean swipeFromRight() {
        if (!mIsHwNaviBar || !isLastImmersiveMode() || this.mNavigationBar == null || this.mNavigationBarPosition == 4) {
            return false;
        }
        requestHwTransientBars(this.mNavigationBar);
        return true;
    }

    public boolean isGestureIsolated() {
        WindowState win = this.mFocusedWindow != null ? this.mFocusedWindow : this.mTopFullscreenOpaqueWindowState;
        if (win == null || (win.getAttrs().hwFlags & 512) != 512) {
            return false;
        }
        return true;
    }

    public void requestTransientStatusBars() {
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            BarController barController = getStatusBarController();
            boolean sb = false;
            if (barController != null) {
                sb = barController.checkShowTransientBarLw();
            }
            if (sb && barController != null) {
                barController.showTransient();
            }
            ImmersiveModeConfirmation immer = getImmersiveModeConfirmation();
            if (immer != null) {
                immer.confirmCurrentPrompt();
            }
            updateHwSystemUiVisibilityLw();
        }
    }

    public boolean isTopIsFullscreen() {
        try {
            boolean z = this.mFocusedWindow != null ? (this.mFocusedWindow.getAttrs().flags & 1024) != 0 : this.mTopIsFullscreen;
            return z;
        } catch (NullPointerException e) {
            Log.e(TAG, "isTopIsFullscreen catch null pointer");
            return this.mTopIsFullscreen;
        }
    }

    public boolean okToShowTransientBar() {
        BarController barController = getStatusBarController();
        boolean z = false;
        if (barController == null) {
            return false;
        }
        if (barController.checkShowTransientBarLw()) {
            z = true;
        }
        return z;
    }

    private void turnOnSensorListener() {
        if (this.mSensorManager == null) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        }
        if (this.mCoverManager == null) {
            this.mCoverManager = new CoverManager();
        }
        if (this.mCoverManager != null) {
            this.mCoverOpen = this.mCoverManager.isCoverOpen();
        }
        boolean touchDisableModeOpen = System.getIntForUser(this.mContext.getContentResolver(), KEY_TOUCH_DISABLE_MODE, 1, -2) == 1;
        if (this.mCoverOpen && !this.mSensorRegisted && this.mListener != null && touchDisableModeOpen) {
            Log.i(TAG, "turnOnSensorListener, registerListener");
            this.mSensorManager.registerListener(this.mListener, this.mSensorManager.getDefaultSensor(8), 0);
            this.mSensorRegisted = true;
            this.mHandler.removeCallbacks(this.mProximitySensorTimeoutRunnable);
            this.mHandler.postDelayed(this.mProximitySensorTimeoutRunnable, 1000);
        }
    }

    public void turnOffSensorListener() {
        if (this.mSensorRegisted && this.mListener != null) {
            Log.i(TAG, "turnOffSensorListener, unregisterListener ");
            this.mSensorManager.unregisterListener(this.mListener);
            this.mHandler.removeCallbacks(this.mProximitySensorTimeoutRunnable);
            this.mIsProximity = false;
        }
        this.mSensorRegisted = false;
    }

    public void setHwWindowCallback(IHwWindowCallback hwWindowCallback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setHwWindowCallback=");
        stringBuilder.append(hwWindowCallback);
        Log.i(str, stringBuilder.toString());
        this.mIHwWindowCallback = hwWindowCallback;
    }

    public IHwWindowCallback getHwWindowCallback() {
        return this.mIHwWindowCallback;
    }

    public void updateSettings() {
        Flog.i(1503, "updateSettings");
        super.updateSettings();
        updateFingerSenseSettings();
        setFingerSenseState();
        setNaviBarState();
    }

    private void updateFingerSenseSettings() {
        ContentResolver cr = this.mContext.getContentResolver();
        FingerSenseSettings.updateSmartshotEnabled(cr);
        FingerSenseSettings.updateLineGestureEnabled(cr);
        FingerSenseSettings.updateDrawGestureEnabled(cr);
    }

    public void enableScreenAfterBoot() {
        super.enableScreenAfterBoot();
        this.mBooted = true;
        enableSystemWideAfterBoot(this.mContext);
        enableFingerPrintActionsAfterBoot(this.mContext);
        enableStylusAfterBoot(this.mContext);
    }

    public WindowState getFocusedWindow() {
        return this.mFocusedWindow;
    }

    public WindowState getTopFullscreenWindow() {
        return this.mTopFullscreenOpaqueWindowState;
    }

    public int getRestrictedScreenHeight() {
        return this.mRestrictedScreenHeight;
    }

    private void enableStylusAfterBoot(Context context) {
        if (HwStylusUtils.hasStylusFeature(context)) {
            Log.i(TAG, "enable stylus gesture feature.");
            this.mHandler.post(new Runnable() {
                public void run() {
                    HwPhoneWindowManager.this.enableStylusAction();
                }
            });
        }
    }

    private void enableStylusAction() {
        if (this.mStylusGestureListener == null) {
            this.mStylusGestureListener = new StylusGestureListener(this.mContext, this);
            this.mWindowManagerFuncs.registerPointerEventListener(this.mStylusGestureListener);
        }
    }

    public boolean isNavigationBarVisible() {
        return this.mHasNavigationBar && this.mNavigationBar != null && this.mNavigationBar.isVisibleLw();
    }

    protected void enableSystemWideActions() {
        if (SystemProperties.getBoolean("ro.config.finger_joint", false)) {
            Flog.i(1503, "FingerSense enableSystemWideActions");
            if (this.systemWideActionsListener == null) {
                this.systemWideActionsListener = new SystemWideActionsListener(this.mContext, this);
                this.mWindowManagerFuncs.registerPointerEventListener(this.systemWideActionsListener);
            }
            SystemProperties.set("persist.sys.fingersense", "1");
            return;
        }
        Flog.i(1503, "Can not enable fingersense, ro.config.finger_joint is set to false");
    }

    protected void disableSystemWideActions() {
        Flog.i(1503, "FingerSense disableSystemWideActions");
        if (this.systemWideActionsListener != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.systemWideActionsListener);
            this.systemWideActionsListener.destroyPointerLocationView();
            this.systemWideActionsListener = null;
        }
        SystemProperties.set("persist.sys.fingersense", "0");
    }

    protected void enableFingerPrintActions() {
        Log.d(TAG, "enableFingerPrintActions()");
        if (this.fingerprintActionsListener != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.fingerprintActionsListener);
            this.fingerprintActionsListener.destroySearchPanelView();
            this.fingerprintActionsListener.destroyMultiWinArrowView();
            this.fingerprintActionsListener = null;
        }
        this.fingerprintActionsListener = new FingerprintActionsListener(this.mContext, this);
        this.mWindowManagerFuncs.registerPointerEventListener(this.fingerprintActionsListener);
        this.fingerprintActionsListener.createSearchPanelView();
        this.fingerprintActionsListener.createMultiWinArrowView();
    }

    protected void disableFingerPrintActions() {
        if (this.fingerprintActionsListener != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.fingerprintActionsListener);
            this.fingerprintActionsListener.destroySearchPanelView();
            this.fingerprintActionsListener.destroyMultiWinArrowView();
            this.fingerprintActionsListener = null;
        }
    }

    protected void enableFingerPrintActionsAfterBoot(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        this.mHandler.post(new Runnable() {
            public void run() {
                if (!FrontFingerPrintSettings.isNaviBarEnabled(resolver) || (FrontFingerPrintSettings.isSingleVirtualNavbarEnable(resolver) && !FrontFingerPrintSettings.isSingleNavBarAIEnable(resolver))) {
                    HwPhoneWindowManager.this.enableFingerPrintActions();
                } else {
                    HwPhoneWindowManager.this.disableFingerPrintActions();
                }
            }
        });
    }

    protected void setNaviBarState() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean navibarEnable = FrontFingerPrintSettings.isNaviBarEnabled(resolver);
        boolean singleNavBarEnable = FrontFingerPrintSettings.isSingleVirtualNavbarEnable(resolver);
        boolean singleNavBarAiEnable = FrontFingerPrintSettings.isSingleNavBarAIEnable(resolver);
        boolean z = false;
        boolean singEnarAiEnable = singleNavBarEnable && !singleNavBarAiEnable;
        if (singEnarAiEnable || !navibarEnable) {
            z = true;
        }
        navibarEnable = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setNaviBarState()--navibarEnable:");
        stringBuilder.append(navibarEnable);
        stringBuilder.append(";mNavibarEnabled:");
        stringBuilder.append(this.mNavibarEnabled);
        stringBuilder.append(";singleNavBarEnable:");
        stringBuilder.append(singleNavBarEnable);
        stringBuilder.append(";singleNavBarAiEnable:");
        stringBuilder.append(singleNavBarAiEnable);
        stringBuilder.append(";singEnarAiEnable:");
        stringBuilder.append(singEnarAiEnable);
        Log.d(str, stringBuilder.toString());
        int i = 104;
        Handler handler;
        if (this.mNaviBarStateInited) {
            if (this.mNavibarEnabled != navibarEnable) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setNaviBarState()--");
                stringBuilder2.append(this.mNavibarEnabled);
                Log.d(str, stringBuilder2.toString());
                this.mNavibarEnabled = navibarEnable;
                handler = this.mHandlerEx;
                if (navibarEnable) {
                    i = 103;
                }
                handler.sendEmptyMessage(i);
            }
        } else if (this.mBooted) {
            this.mNavibarEnabled = navibarEnable;
            handler = this.mHandlerEx;
            if (navibarEnable) {
                i = 103;
            }
            handler.sendEmptyMessage(i);
            this.mNaviBarStateInited = true;
        }
    }

    protected void updateSplitScreenView() {
        if (!((HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) || this.fingerprintActionsListener == null)) {
            this.fingerprintActionsListener.createMultiWinArrowView();
        }
    }

    protected void enableSystemWideAfterBoot(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        this.mHandler.post(new Runnable() {
            public void run() {
                if (FingerSenseSettings.isFingerSenseEnabled(resolver)) {
                    HwPhoneWindowManager.this.enableSystemWideActions();
                } else {
                    HwPhoneWindowManager.this.disableSystemWideActions();
                }
            }
        });
    }

    protected void setFingerSenseState() {
        boolean fingersense = FingerSenseSettings.isFingerSenseEnabled(this.mContext.getContentResolver());
        if (this.mFingerSenseEnabled != fingersense) {
            int i;
            this.mFingerSenseEnabled = fingersense;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setFingerSenseState to ");
            stringBuilder.append(fingersense);
            Flog.i(1503, stringBuilder.toString());
            Handler handler = this.mHandlerEx;
            if (fingersense) {
                i = 101;
            } else {
                i = 102;
            }
            handler.sendEmptyMessage(i);
        }
    }

    public void processing_KEYCODE_SOUNDTRIGGER_EVENT(int keyCode, Context context, boolean isMusicOrFMActive, boolean down, boolean keyguardShow) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("intercept DSP WAKEUP EVENT");
        stringBuilder.append(keyCode);
        stringBuilder.append(" down=");
        stringBuilder.append(down);
        stringBuilder.append(" keyguardShow=");
        stringBuilder.append(keyguardShow);
        Log.d(str, stringBuilder.toString());
        this.mContext = context;
        ITelephony telephonyService = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        switch (keyCode) {
            case AwareJobSchedulerService.MSG_JOB_EXPIRED /*401*/:
                if (down) {
                    Log.i(TAG, "soundtrigger wakeup.");
                    if (isTOPActivity(HUAWEI_VOICE_SOUNDTRIGGER_PACKAGE)) {
                        Log.i(TAG, "start SoundTiggerTest");
                        notifySoundTriggerTest();
                        return;
                    } else if (isTOPActivity(HUAWEI_VOICE_DEBUG_BETACLUB)) {
                        Log.i(TAG, "soundtrigger debug during betaclub.");
                        notifySoundTriggerTest();
                        return;
                    } else {
                        Log.i(TAG, "start VA");
                        notifyVassistantService("start", 4, null);
                        return;
                    }
                }
                return;
            case AwareJobSchedulerService.MSG_CHECK_JOB /*402*/:
                if (down) {
                    Log.i(TAG, "command that find my phone.");
                    if (isTOPActivity(HUAWEI_VOICE_SOUNDTRIGGER_PACKAGE)) {
                        Log.i(TAG, "looking for my phone during SoundTiggerTest");
                        return;
                    } else if (isTOPActivity(HUAWEI_VOICE_DEBUG_BETACLUB)) {
                        Log.i(TAG, "looking for my phone during betaclub.");
                        return;
                    } else {
                        Log.i(TAG, "findphone.");
                        notifyVassistantService("findphone", 4, null);
                        return;
                    }
                }
                return;
            default:
                return;
        }
    }

    private boolean isTOPActivity(String appnames) {
        try {
            List<RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) {
                return false;
            }
            for (RunningTaskInfo info : tasks) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("info.topActivity.getPackageName() is ");
                stringBuilder.append(info.topActivity.getPackageName());
                Log.i(str, stringBuilder.toString());
                if (info.topActivity.getPackageName().equals(appnames) && info.baseActivity.getPackageName().equals(appnames)) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            Log.e(TAG, "isTOPActivity->RuntimeException happened");
        } catch (Exception e2) {
            Log.e(TAG, "isTOPActivity->other exception happened");
        }
    }

    private boolean isNeedPassEventToCloud(int keyCode) {
        return !KEYCODE_NOT_FOR_CLOUD.contains(Integer.valueOf(keyCode)) && HwPCUtils.isPcCastModeInServer() && isCloudOnPCTOP();
    }

    private boolean isCloudOnPCTOP() {
        try {
            List<RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) {
                return false;
            }
            for (RunningTaskInfo info : tasks) {
                if (info.topActivity == null || info.baseActivity == null) {
                    return false;
                }
                if ("com.huawei.cloud".equals(info.topActivity.getPackageName()) && "com.huawei.cloud".equals(info.baseActivity.getPackageName()) && HwPCUtils.isPcDynamicStack(info.stackId) && "com.huawei.ahdp.session.VmActivity".equals(info.topActivity.getClassName())) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            HwPCUtils.log(TAG, "isCloudOnPCTOP->RuntimeException happened");
        } catch (Exception e2) {
            HwPCUtils.log(TAG, "isCloudOnPCTOP->other exception happened");
        }
    }

    private void notifyVassistantService(String command, int mode, KeyEvent event) {
        Intent intent = new Intent(ACTION_HUAWEI_VASSISTANT_SERVICE);
        intent.putExtra(HUAWEI_VASSISTANT_EXTRA_START_MODE, mode);
        intent.putExtra(SMARTKEY_TAG, command);
        if (event != null) {
            intent.putExtra("KeyEvent", event);
        }
        intent.setPackage(HUAWEI_VASSISTANT_PACKAGE);
        this.mContext.startService(intent);
        if (this.mVolumeDownWakeLock != null) {
            this.mVolumeDownWakeLock.acquire(500);
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start VASSISTANT Service, state:");
            stringBuilder.append(extras.get(HUAWEI_VASSISTANT_EXTRA_START_MODE));
            stringBuilder.append(" command:");
            stringBuilder.append(extras.get(SMARTKEY_TAG));
            Log.d(str, stringBuilder.toString());
        }
    }

    private void notifySoundTriggerTest() {
        try {
            this.mContext.sendBroadcast(new Intent(HUAWEI_VOICE_SOUNDTRIGGER_BROADCAST));
            Log.i(TAG, "start up HUAWEI_VOICE_SOUNDTRIGGER_BROADCAST");
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No receiver to handle HUAWEI_VOICE_SOUNDTRIGGER_BROADCAST intent", e);
        }
    }

    public void handleSmartKey(Context context, KeyEvent event, Handler handler, boolean isScreenOn) {
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        if (this.mActivityManagerInternal.isSystemReady()) {
            if (this.DEBUG_SMARTKEY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleSmartKey keycode = ");
                stringBuilder.append(keyCode);
                stringBuilder.append(" down = ");
                stringBuilder.append(down);
                stringBuilder.append(" isScreenOn = ");
                stringBuilder.append(isScreenOn);
                Log.d(str, stringBuilder.toString());
            }
            if (keyCode == MemoryConstant.MSG_PROTECTLRU_CONFIG_UPDATE) {
                if (down) {
                    if (SystemProperties.getBoolean("ro.config.fingerOnSmartKey", false)) {
                        this.mNeedDropFingerprintEvent = true;
                        this.mHandler.removeCallbacks(this.mCancleInterceptFingerprintEvent);
                    }
                    if (!isScreenOn) {
                        if (this.mListener == null) {
                            this.mListener = new ProximitySensorListener();
                        }
                        turnOnSensorListener();
                    }
                    sendSmartKeyEvent(SMARTKEY_LP);
                    long timediff = event.getEventTime() - this.mLastSmartKeyDownTime;
                    this.mLastSmartKeyDownTime = event.getEventTime();
                    if (timediff >= 400) {
                        this.mIsSmartKeyTripleOrMoreClick = false;
                        this.mIsSmartKeyDoubleClick = false;
                    } else if (this.mIsSmartKeyDoubleClick || this.mIsSmartKeyTripleOrMoreClick) {
                        this.mIsSmartKeyTripleOrMoreClick = true;
                        this.mIsSmartKeyDoubleClick = false;
                    } else {
                        cancelSmartKeyClick();
                        cancelSmartKeyLongPressed();
                        sendSmartKeyEvent(SMARTKEY_DCLICK);
                        this.mIsSmartKeyDoubleClick = true;
                    }
                } else {
                    if (SystemProperties.getBoolean("ro.config.fingerOnSmartKey", false)) {
                        this.mHandler.postDelayed(this.mCancleInterceptFingerprintEvent, 400);
                    }
                    if (this.mIsSmartKeyDoubleClick || this.mIsSmartKeyTripleOrMoreClick || event.getEventTime() - event.getDownTime() >= 500) {
                        cancelSmartKeyLongPressed();
                    } else {
                        cancelSmartKeyLongPressed();
                        sendSmartKeyEvent(SMARTKEY_CLICK);
                    }
                }
            }
            return;
        }
        Log.d(TAG, "System is not ready, just discard it this time.");
    }

    private void sendSmartKeyEvent(String Type) {
        if (SMARTKEY_LP.equals(Type)) {
            this.mHandler.postDelayed(this.mSmartKeyLongPressed, 500);
        } else if (SMARTKEY_DCLICK.equals(Type)) {
            notifySmartKeyEvent(SMARTKEY_DCLICK);
        } else {
            this.mHandler.postDelayed(this.mSmartKeyClick, 400);
        }
    }

    private void cancelSmartKeyClick() {
        this.mHandler.removeCallbacks(this.mSmartKeyClick);
    }

    private void cancelSmartKeyLongPressed() {
        this.mHandler.removeCallbacks(this.mSmartKeyLongPressed);
    }

    private void notifySmartKeyEvent(String strType) {
        Intent intent = new Intent(HUAWEI_SMARTKEY_PACKAGE);
        intent.setFlags(268435456);
        intent.putExtra(SMARTKEY_TAG, strType);
        this.mContext.sendBroadcast(intent);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("send smart key ");
        stringBuilder.append(strType);
        Log.i(str, stringBuilder.toString());
        if ((!this.mIsProximity && this.mSensorRegisted) || !this.mSensorRegisted || (isPhoneInCall() && SMARTKEY_LP.equals(strType))) {
            intent.setPackage(HUAWEI_SMARTKEY_SERVICE);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notify smartkey service ");
            stringBuilder.append(strType);
            Log.i(str, stringBuilder.toString());
        }
        turnOffSensorListener();
    }

    public boolean getNeedDropFingerprintEvent() {
        return this.mNeedDropFingerprintEvent;
    }

    private String getTopActivity() {
        return ((ActivityManagerService) ServiceManager.getService("activity")).topAppName();
    }

    private void initDropSmartKey() {
        String dropSmartKeyActivity = Systemex.getString(this.mResolver, DROP_SMARTKEY_ACTIVITY);
        if (TextUtils.isEmpty(dropSmartKeyActivity)) {
            Log.w(TAG, "dropSmartKeyActivity not been configured in hw_defaults.xml!");
            return;
        }
        for (String str : dropSmartKeyActivity.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
            this.needDropSmartKeyActivities.add(str);
        }
    }

    private boolean needDropSmartKey() {
        boolean result = false;
        String topActivityName = getTopActivity();
        if (this.needDropSmartKeyActivities != null && this.needDropSmartKeyActivities.contains(topActivityName)) {
            result = true;
            Log.d(TAG, "drop smartkey event because of conflict with fingerprint authentication!");
        }
        if ((!isCamera() || !this.isFingerShotCameraOn) && ((!isInCallUIAndRinging() || !this.isFingerAnswerPhoneOn) && (!isAlarm(this.mCurUser) || !this.isFingerStopAlarmOn))) {
            return result;
        }
        Log.d(TAG, "drop smartkey event because of conflict with fingerprint longpress event!");
        return true;
    }

    private boolean isCamera() {
        String pkgName = getTopActivity();
        return pkgName != null && pkgName.startsWith("com.huawei.camera");
    }

    public boolean isKeyguardShortcutApps() {
        try {
            String focusPackageName = this.mFocusedWindow.getAttrs().packageName;
            if (focusPackageName == null) {
                return false;
            }
            for (String startsWith : this.mKeyguardShortcutApps) {
                if (focusPackageName.startsWith(startsWith)) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isKeyguardShortcutApps error : ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean isLsKeyguardShortcutApps() {
        try {
            String focusPackageName = this.mFocusedWindow.getAttrs().packageName;
            if (focusPackageName == null) {
                return false;
            }
            for (String startsWith : this.mLsKeyguardShortcutApps) {
                if (focusPackageName.startsWith(startsWith)) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isLsKeyguardShortcutApps error : ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void onProximityPositive() {
        Log.i(TAG, "onProximityPositive");
        this.mHwScreenOnProximityLock.releaseLock(1);
        this.mHwScreenOnProximityLock.unregisterDeviceListener();
    }

    private boolean isInCallUIAndRinging() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        return telecomManager != null && telecomManager.isRinging();
    }

    private boolean isAlarm(int user) {
        return ((HwActivityManagerService) ServiceManager.getService("activity")).serviceIsRunning(ComponentName.unflattenFromString("com.android.deskclock/.alarmclock.AlarmKlaxon"), user);
    }

    public void waitKeyguardDismissDone(KeyguardDismissDoneListener listener) {
        synchronized (this.mLock) {
            this.mKeyguardDismissListener = listener;
        }
        this.mWindowManagerInternal.waitForKeyguardDismissDone(this.mKeyguardDismissDoneCallback, POWER_SOS_MISTOUCH_THRESHOLD);
    }

    public void cancelWaitKeyguardDismissDone() {
        synchronized (this.mLock) {
            this.mKeyguardDismissListener = null;
        }
    }

    protected void finishKeyguardDismissDone() {
        KeyguardDismissDoneListener listener;
        synchronized (this.mLock) {
            listener = this.mKeyguardDismissListener;
            this.mKeyguardDismissListener = null;
        }
        if (listener != null) {
            listener.onKeyguardDismissDone();
        }
    }

    public void setInterceptInputForWaitBrightness(boolean intercept) {
        this.mInterceptInputForWaitBrightness = intercept;
    }

    public boolean getInterceptInputForWaitBrightness() {
        return this.mInterceptInputForWaitBrightness;
    }

    private void interceptBackandMenuKey() {
        long now = SystemClock.uptimeMillis();
        if (isScreenInLockTaskMode() && this.mBackKeyPress && this.mMenuKeyPress && now <= this.mBackKeyPressTime + TOUCH_SPINNING_DELAY_MILLIS && now <= this.mMenuKeyPressTime + TOUCH_SPINNING_DELAY_MILLIS) {
            this.mBackKeyPress = false;
            this.mMenuKeyPress = false;
            this.mBackKeyPressTime = 0;
            this.mMenuKeyPressTime = 0;
        }
    }

    private boolean isScreenInLockTaskMode() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            Log.e(TAG, "isScreenInLockTaskMode  ", e);
            return false;
        }
    }

    public boolean isStatusBarObsecured() {
        return this.mStatuBarObsecured;
    }

    /* JADX WARNING: Missing block: B:20:0x0045, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean isStatusBarObsecuredByWin(WindowState win) {
        boolean z = false;
        if (win == null || this.mStatusBar == null || (win.getAttrs().flags & 16) != 0 || win.toString().contains("hwSingleMode_window")) {
            return false;
        }
        Rect winFrame = win.getFrameLw();
        Rect statusbarFrame = this.mStatusBar.getFrameLw();
        if (winFrame.top <= statusbarFrame.top && winFrame.bottom >= statusbarFrame.bottom && winFrame.left <= statusbarFrame.left && winFrame.right >= statusbarFrame.right) {
            z = true;
        }
        return z;
    }

    public void setNaviImmersiveMode(boolean mode) {
        if (this.mNavigationBarPolicy != null) {
            this.mNavigationBarPolicy.setImmersiveMode(mode);
        }
        this.mIsImmersiveMode = mode;
    }

    public boolean getImmersiveMode() {
        return this.mIsImmersiveMode;
    }

    public void adjustConfigurationLw(Configuration config, int keyboardPresence, int navigationPresence) {
        super.adjustConfigurationLw(config, keyboardPresence, navigationPresence);
        int tempDpi = config.densityDpi;
        if (tempDpi != this.lastDensityDpi) {
            if (this.systemWideActionsListener != null) {
                this.systemWideActionsListener.updateConfiguration();
                this.lastDensityDpi = tempDpi;
            }
            if (this.mStylusGestureListener != null) {
                this.mStylusGestureListener.updateConfiguration();
                this.lastDensityDpi = tempDpi;
            }
        }
    }

    public void setRotationLw(int rotation) {
        super.setRotationLw(rotation);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PhoneWindowManager setRotationLw(");
        stringBuilder.append(rotation);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            try {
                hwAft.notifyOrientationChange(rotation);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setRotationLw throw ");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
            }
        }
        if (this.mGestureNavManager != null) {
            this.mGestureNavManager.onRotationChanged(rotation);
        }
    }

    public boolean performHapticFeedbackLw(WindowState win, int effectId, boolean always) {
        if (effectId != 1 || !this.isVibrateImplemented || always) {
            return super.performHapticFeedbackLw(win, effectId, always);
        }
        if (1 != System.getInt(this.mContext.getContentResolver(), "touch_vibrate_mode", 1)) {
            return false;
        }
        HwGeneralManager.getInstance().playIvtEffect("VIRTUAL_KEY");
        return true;
    }

    public void setNavibarAlignLeftWhenLand(boolean isLeft) {
        this.mIsNavibarAlignLeftWhenLand = isLeft;
    }

    public boolean getNavibarAlignLeftWhenLand() {
        return this.mIsNavibarAlignLeftWhenLand;
    }

    public boolean isPhoneIdle() {
        ITelephony telephonyService = getTelephonyService();
        if (telephonyService == null) {
            return false;
        }
        try {
            if (!TelephonyManager.getDefault().isMultiSimEnabled()) {
                return telephonyService.isIdle(this.mContext.getPackageName());
            }
            boolean z = false;
            boolean isIdleForSubscriberSub1 = telephonyService.isIdleForSubscriber(0, this.mContext.getPackageName());
            boolean isIdleForSubscriberSub2 = telephonyService.isIdleForSubscriber(1, this.mContext.getPackageName());
            if (isIdleForSubscriberSub1 && isIdleForSubscriberSub2) {
                z = true;
            }
            return z;
        } catch (RemoteException ex) {
            Log.w(TAG, "ITelephony threw RemoteException", ex);
            return false;
        }
    }

    public int getDisabledKeyEventResult(int keyCode) {
        if (keyCode != 187) {
            switch (keyCode) {
                case 3:
                    if ((this.mCust == null || !this.mCust.disableHomeKey(this.mContext)) && !HwDeviceManager.disallowOp(14)) {
                        return -2;
                    }
                    Log.i(TAG, "the device's home key has been disabled for the user.");
                    return 0;
                case 4:
                    if (!HwDeviceManager.disallowOp(16)) {
                        return -2;
                    }
                    Log.i(TAG, "the device's back key has been disabled for the user.");
                    return -1;
                default:
                    return -2;
            }
        } else if (!HwDeviceManager.disallowOp(15)) {
            return -2;
        } else {
            Log.i(TAG, "the device's task key has been disabled for the user.");
            return 0;
        }
    }

    private int getGameControlKeyReslut(KeyEvent event) {
        int result = -2;
        if (!mSupportGameAssist || this.mAms == null) {
            return -2;
        }
        int keyCode = event.getKeyCode();
        boolean isGameKeyControlOn = (!this.mEnableKeyInCurrentFgGameApp && this.mAms.isGameKeyControlOn()) || this.mLastKeyDownDropped;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deviceId:");
        stringBuilder.append(event.getDeviceId());
        stringBuilder.append(" mFingerPrintId:");
        stringBuilder.append(this.mFingerPrintId);
        stringBuilder.append(" isGameKeyControlOn:");
        stringBuilder.append(isGameKeyControlOn);
        stringBuilder.append(",EnableKey=");
        stringBuilder.append(this.mEnableKeyInCurrentFgGameApp);
        stringBuilder.append(",mLastKeyDownDropped=");
        stringBuilder.append(this.mLastKeyDownDropped);
        Log.d(str, stringBuilder.toString());
        if (isGameKeyControlOn) {
            if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                if (this.mTrikeyNaviMode < 0) {
                    Log.d(TAG, "trikey single mode.");
                    result = performGameMode(event, 0);
                } else {
                    Log.d(TAG, "trikey three mode.");
                    result = performGameMode(event, 1);
                }
            } else if (!FrontFingerPrintSettings.isNaviBarEnabled(this.mResolver)) {
                Log.d(TAG, "single key.");
                result = performGameMode(event, 2);
            } else if (keyCode == 3 && event.getDeviceId() == this.mFingerPrintId) {
                Log.d(TAG, "NaviBarEnabled KEYCODE_HOME !");
                result = performGameMode(event, 3);
            }
        }
        return result;
    }

    private int performGameMode(KeyEvent event, int naviMode) {
        int result = -2;
        boolean z = true;
        boolean isSingleKeyMode = naviMode == 0 || naviMode == 2;
        int keyCode = event.getKeyCode();
        long keyTime = event.getEventTime();
        boolean isKeyDown = event.getAction() == 0;
        if (this.mLastKeyDownDropped && keyTime - this.mLastKeyDownTime > POWER_SOS_MISTOUCH_THRESHOLD) {
            this.mLastKeyDownDropped = false;
        }
        if (keyCode != 187) {
            switch (keyCode) {
                case 3:
                    if (!isSingleKeyMode) {
                        if (!isKeyDown) {
                            result = getClickResult(keyTime, keyCode);
                            break;
                        }
                    }
                    return -2;
                    break;
                case 4:
                    if (isKeyDown) {
                        result = getClickResult(keyTime, keyCode);
                        if (result == -2) {
                            z = false;
                        }
                        this.mLastKeyDownDropped = z;
                        break;
                    } else if (this.mLastKeyDownDropped) {
                        Log.d(TAG, "drop key up for last event beacause down dropped.");
                        this.mLastKeyDownDropped = false;
                        return -1;
                    }
                    break;
            }
        } else if (isSingleKeyMode) {
            return -1;
        } else {
            if (!isKeyDown) {
                result = getClickResult(keyTime, keyCode);
                this.mHandlerEx.removeMessages(MSG_TRIKEY_RECENT_LONG_PRESS);
            }
        }
        return result;
    }

    private int getClickResult(long eventTime, int keyCode) {
        int result;
        if (eventTime - this.mLastKeyDownTime >= POWER_SOS_MISTOUCH_THRESHOLD || this.mLastKeyDownKeyCode != keyCode || this.mLastKeyDownTime - this.mSecondToLastKeyDownTime >= POWER_SOS_MISTOUCH_THRESHOLD || this.mSecondToLastKeyDownKeyCode != this.mLastKeyDownKeyCode) {
            result = -1;
        } else {
            Log.i(TAG, "Navigation keys unlocked.");
            result = -1;
            this.mEnableKeyInCurrentFgGameApp = true;
            showKeyEnableToast();
            Flog.bdReport(this.mContext, HttpConnectionBase.SERVER_OVERLOAD_ERRORCODE);
        }
        this.mSecondToLastKeyDownTime = this.mLastKeyDownTime;
        this.mSecondToLastKeyDownKeyCode = this.mLastKeyDownKeyCode;
        this.mLastKeyDownTime = eventTime;
        this.mLastKeyDownKeyCode = keyCode;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getClickResult result:");
        stringBuilder.append(result);
        stringBuilder.append(",keyCode:");
        stringBuilder.append(keyCode);
        stringBuilder.append(",EnableKey:");
        stringBuilder.append(this.mEnableKeyInCurrentFgGameApp);
        Log.i(str, stringBuilder.toString());
        return result;
    }

    private void showKeyEnableToast() {
        this.mHandler.post(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(HwPhoneWindowManager.this.mContext, 33686186, 0);
                toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                LayoutParams windowParams = toast.getWindowParams();
                windowParams.privateFlags |= 16;
                toast.show();
            }
        });
    }

    public void onPointDown() {
        this.mTouchCountPolicy.updateTouchCountInfo();
    }

    public int[] getTouchCountInfo() {
        return this.mTouchCountPolicy.getTouchCountInfo();
    }

    public int[] getDefaultTouchCountInfo() {
        return this.mTouchCountPolicy.getDefaultTouchCountInfo();
    }

    private boolean isTrikeyNaviKeycodeFromLON(boolean isInjected, boolean excluded) {
        int frontFpNaviTriKey = FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("frontFpNaviTriKey:");
        stringBuilder.append(frontFpNaviTriKey);
        stringBuilder.append(" isInjected:");
        stringBuilder.append(isInjected);
        stringBuilder.append(" mTrikeyNaviMode:");
        stringBuilder.append(this.mTrikeyNaviMode);
        stringBuilder.append(" excluded:");
        stringBuilder.append(excluded);
        Log.d(str, stringBuilder.toString());
        return frontFpNaviTriKey == 0 || ((!isInjected && this.mTrikeyNaviMode < 0) || excluded);
    }

    public boolean isSupportCover() {
        String GSETTINGS_COVER_ENABLED = "cover_enabled";
        return Global.getInt(this.mContext.getContentResolver(), "cover_enabled", 1) != 0;
    }

    public boolean isSmartCoverMode() {
        String SETTINGS_COVER_TYPE = "cover_type";
        return Global.getInt(this.mContext.getContentResolver(), "cover_type", 0) == 1;
    }

    public boolean isInCallActivity() {
        String pkgName = getTopActivity();
        return pkgName != null && pkgName.startsWith("com.android.incallui");
    }

    public boolean isInterceptAndCheckRinging(boolean isIntercept) {
        TelecomManager telecomManager = getTelecommService();
        if (!isIntercept || (telecomManager != null && telecomManager.isRinging())) {
            return false;
        }
        return true;
    }

    public int getSingAppKeyEventResult(int keyCode) {
        String packageName = HwDeviceManager.getString(34);
        if (packageName == null || packageName.isEmpty()) {
            return -2;
        }
        boolean[] results = isNeedStartSingleApp(packageName);
        switch (keyCode) {
            case 3:
                if (!results[0]) {
                    Log.i(TAG, "Single app model running, start the single app's main activity.");
                    startSingleApp(packageName);
                }
                return 0;
            case 4:
                if (results[0]) {
                    return -1;
                }
                if (!results[1]) {
                    return -2;
                }
                Log.i(TAG, "Single app model running, start the single app's main activity.");
                startSingleApp(packageName);
                return -1;
            default:
                return -2;
        }
    }

    private boolean[] isNeedStartSingleApp(String packageName) {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        boolean[] results = new boolean[]{false, false};
        if (activityManager != null) {
            try {
                List<RunningTaskInfo> runningTask = activityManager.getRunningTasks(2);
                if (runningTask != null && runningTask.size() > 0) {
                    String currentAppName;
                    ComponentName cn = ((RunningTaskInfo) runningTask.get(0)).topActivity;
                    if (cn != null) {
                        currentAppName = cn.getPackageName();
                        String currentActivityName = cn.getClassName();
                        PackageManager pm = this.mContext.getPackageManager();
                        String mainClassName = pm.getLaunchIntentForPackage(packageName).resolveActivity(pm).getClassName();
                        if (mainClassName != null && mainClassName.equals(currentActivityName) && packageName.equals(currentAppName)) {
                            results[0] = true;
                        }
                    }
                    currentAppName = null;
                    if (runningTask.size() > 1) {
                        currentAppName = ((RunningTaskInfo) runningTask.get(1)).topActivity.getPackageName();
                    }
                    if ((((RunningTaskInfo) runningTask.get(0)).numActivities <= 1 && (runningTask.size() <= 1 || !packageName.equals(nextAppName))) || FingerViewController.PKGNAME_OF_KEYGUARD.equals(nextAppName)) {
                        results[1] = true;
                    }
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "isTopApp->RuntimeException happened");
            } catch (Exception e2) {
                Log.e(TAG, "isTopApp->other exception happened");
            }
        }
        return results;
    }

    private void startSingleApp(String packageName) {
        Intent launchIntent = this.mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            this.mContext.startActivity(launchIntent);
        }
    }

    public boolean isKeyguardOccluded() {
        return this.mKeyguardOccluded;
    }

    protected void notifyPowerkeyInteractive(boolean bool) {
        AwareFakeActivityRecg.self().notifyPowerkeyInteractive(true);
    }

    public void setNavigationBarExternal(WindowState state) {
        this.mNavigationBarExternal = state;
    }

    public WindowState getNavigationBarExternal() {
        return this.mNavigationBarExternal;
    }

    public void removeWindowLw(WindowState win) {
        super.removeWindowLw(win);
        if (!HwPCUtils.enabled()) {
            return;
        }
        if (getNavigationBarExternal() == win) {
            setNavigationBarExternal(null);
            this.mNavigationBarControllerExternal.setWindow(null);
        } else if (this.mLighterDrawView == win) {
            this.mLighterDrawView = null;
        }
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            return getNonDecorDisplayHeight(fullWidth, fullHeight, rotation, uiMode, displayId, displayCutout);
        }
        return super.getConfigDisplayHeight(fullWidth, fullHeight, rotation, uiMode, displayId, displayCutout);
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        super.dump(prefix, pw, args);
        if (HwPCUtils.isPcCastModeInServer() && getNavigationBarExternal() != null) {
            pw.print(prefix);
            pw.print("mNavigationBarExternal=");
            pw.println(getNavigationBarExternal());
        }
        if (HwPCUtils.isPcCastModeInServer() && this.mNavigationBarControllerExternal != null) {
            this.mNavigationBarControllerExternal.dump(pw, prefix);
        }
        if (this.mGestureNavManager != null) {
            this.mGestureNavManager.dump(prefix, pw, args);
        }
    }

    public int focusChangedLw(WindowState lastFocus, WindowState newFocus) {
        if (HwPCUtils.isPcCastModeInServer() && newFocus != null && HwPCUtils.isValidExtDisplayId(newFocus.getDisplayId())) {
            return 1;
        }
        if (this.mFalseTouchMonitor != null) {
            this.mFalseTouchMonitor.handleFocusChanged(lastFocus, newFocus);
        }
        if (this.mGestureNavManager != null) {
            this.mGestureNavManager.onFocusWindowChanged(lastFocus, newFocus);
        }
        return super.focusChangedLw(lastFocus, newFocus);
    }

    public void layoutWindowLw(WindowState win, WindowState attached, DisplayFrames displayFrames) {
        if (!HwPCUtils.isPcCastModeInServer() || win != getNavigationBarExternal()) {
            super.layoutWindowLw(win, attached, displayFrames);
        }
    }

    public void beginLayoutLw(DisplayFrames displayFrames, int uiMode) {
        DisplayFrames displayFrames2 = displayFrames;
        super.beginLayoutLw(displayFrames, uiMode);
        DisplayFrames displayFrames3;
        if (HwPCUtils.isPcCastModeInServer() && getNavigationBarExternal() != null && HwPCUtils.isValidExtDisplayId(displayFrames2.mDisplayId)) {
            if (this.mNavigationBarHeightExternal == 0 && this.mContext != null) {
                initNavigationBarHightExternal(((DisplayManager) this.mContext.getSystemService("display")).getDisplay(displayFrames2.mDisplayId), displayFrames2.mDisplayWidth, displayFrames2.mDisplayHeight);
            }
            Rect pf = mTmpParentFrame;
            Rect df = mTmpDisplayFrame;
            Rect of = mTmpOverscanFrame;
            Rect vf = mTmpVisibleFrame;
            Rect dcf = mTmpDecorFrame;
            int i = displayFrames2.mDock.left;
            vf.left = i;
            of.left = i;
            df.left = i;
            pf.left = i;
            i = displayFrames2.mDock.top;
            vf.top = i;
            of.top = i;
            df.top = i;
            pf.top = i;
            i = displayFrames2.mDock.right;
            vf.right = i;
            of.right = i;
            df.right = i;
            pf.right = i;
            i = displayFrames2.mDock.bottom;
            vf.bottom = i;
            of.bottom = i;
            df.bottom = i;
            pf.bottom = i;
            dcf.setEmpty();
            Rect dcf2 = dcf;
            Rect vf2 = vf;
            Rect of2 = of;
            Rect df2 = df;
            Rect pf2 = pf;
            layoutNavigationBarExternal(displayFrames2.mDisplayWidth, displayFrames2.mDisplayHeight, displayFrames2.mRotation, uiMode, 0, 0, 0, dcf, true, false, false, false, false, displayFrames);
            displayFrames3 = displayFrames;
            i = displayFrames3.mUnrestricted.left;
            Rect of3 = of2;
            of3.left = i;
            dcf = df2;
            dcf.left = i;
            vf = pf2;
            vf.left = i;
            i = displayFrames3.mUnrestricted.top;
            of3.top = i;
            dcf.top = i;
            vf.top = i;
            i = displayFrames3.mUnrestricted.right;
            of3.right = i;
            dcf.right = i;
            vf.right = i;
            i = displayFrames3.mUnrestricted.bottom;
            of3.bottom = i;
            dcf.bottom = i;
            vf.bottom = i;
            of = vf2;
            of.left = displayFrames3.mStable.left;
            of.top = displayFrames3.mStable.top;
            of.right = displayFrames3.mStable.right;
            of.bottom = displayFrames3.mStable.bottom;
            if (HwPCUtils.enabledInPad()) {
                layoutStatusBarExternal(vf, dcf, of3, of, dcf2, displayFrames3);
                return;
            }
            return;
        }
        displayFrames3 = displayFrames2;
    }

    private boolean layoutStatusBarExternal(Rect pf, Rect df, Rect of, Rect vf, Rect dcf, DisplayFrames displayFrames) {
        Rect rect = pf;
        Rect rect2 = df;
        Rect rect3 = of;
        Rect rect4 = vf;
        DisplayFrames displayFrames2 = displayFrames;
        if (this.mStatusBar != null) {
            int i = displayFrames2.mUnrestricted.left;
            rect3.left = i;
            rect2.left = i;
            rect.left = i;
            i = displayFrames2.mUnrestricted.right;
            rect3.right = i;
            rect2.right = i;
            rect.right = i;
            i = displayFrames2.mUnrestricted.top;
            rect3.top = i;
            rect2.top = i;
            rect.top = i;
            i = displayFrames2.mUnrestricted.bottom;
            rect3.bottom = i;
            rect2.bottom = i;
            rect.bottom = i;
            rect4.left = displayFrames2.mStable.left;
            rect4.top = displayFrames2.mStable.top;
            rect4.right = displayFrames2.mStable.right;
            rect4.bottom = displayFrames2.mStable.bottom;
            this.mStatusBar.computeFrameLw(rect, rect2, rect4, rect4, rect4, dcf, rect4, rect4, displayFrames2.mDisplayCutout, false);
        }
        return false;
    }

    protected boolean layoutNavigationBarExternal(int displayWidth, int displayHeight, int displayRotation, int uiMode, int overscanLeft, int overscanRight, int overscanBottom, Rect dcf, boolean navVisible, boolean navTranslucent, boolean navAllowedHidden, boolean isKeyguardOn, boolean statusBarExpandedNotKeyguard, DisplayFrames displayFrames) {
        DisplayFrames displayFrames2 = displayFrames;
        this.mNavigationBarPosition = 4;
        mTmpNavigationFrame.set(0, (displayHeight - overscanBottom) - getNavigationBarHeightExternal(), displayWidth, displayHeight - overscanBottom);
        Rect rect = displayFrames2.mStable;
        Rect rect2 = displayFrames2.mStableFullscreen;
        int i = mTmpNavigationFrame.top;
        rect2.bottom = i;
        rect.bottom = i;
        this.mNavigationBarControllerExternal.setBarShowingLw(true);
        displayFrames2.mDock.bottom = displayFrames2.mStable.bottom;
        displayFrames2.mRestricted.bottom = displayFrames2.mStable.bottom;
        displayFrames2.mRestrictedOverscan.bottom = displayFrames2.mDock.bottom;
        displayFrames2.mSystem.bottom = displayFrames2.mStable.bottom;
        displayFrames2.mContent.set(displayFrames2.mDock);
        displayFrames2.mVoiceContent.set(displayFrames2.mDock);
        displayFrames2.mCurrent.set(displayFrames2.mDock);
        getNavigationBarExternal().computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, dcf, mTmpNavigationFrame, mTmpNavigationFrame, displayFrames2.mDisplayCutout, false);
        return false;
    }

    protected int getNavigationBarHeightExternal() {
        return this.mNavigationBarHeightExternal;
    }

    public int prepareAddWindowLw(WindowState win, LayoutParams attrs) {
        if (HwPCUtils.isPcCastModeInServer()) {
            if (attrs.type == HwArbitrationDEFS.MSG_MPLINK_ERROR && HwPCUtils.isValidExtDisplayId(win.getDisplayId())) {
                if (getNavigationBarExternal() != null && getNavigationBarExternal().isAlive()) {
                    return -7;
                }
                setNavigationBarExternal(win);
                this.mNavigationBarControllerExternal.setWindow(win);
                return 0;
            } else if (attrs.type == 2104 && HwPCUtils.isValidExtDisplayId(win.getDisplayId())) {
                if (this.mLighterDrawView != null) {
                    return -7;
                }
                this.mLighterDrawView = win;
                return 0;
            }
        }
        return super.prepareAddWindowLw(win, attrs);
    }

    public void getStableInsetsLw(int displayRotation, int displayWidth, int displayHeight, Rect outInsets, int displayId, DisplayCutout displayCutout) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            outInsets.setEmpty();
            getNonDecorInsetsLw(displayRotation, displayWidth, displayHeight, outInsets, displayId, displayCutout);
            outInsets.top = 0;
            return;
        }
        getStableInsetsLw(displayRotation, displayWidth, displayHeight, displayCutout, outInsets);
    }

    public void getNonDecorInsetsLw(int displayRotation, int displayWidth, int displayHeight, Rect outInsets, int displayId, DisplayCutout displayCutout) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            outInsets.setEmpty();
            if (this.mHasNavigationBar) {
                if (getNavigationBarExternal() == null || !getNavigationBarExternal().isVisibleLw()) {
                    outInsets.bottom = 0;
                } else {
                    outInsets.bottom = getNavigationBarHeightExternal();
                }
            }
            return;
        }
        getNonDecorInsetsLw(displayRotation, displayWidth, displayHeight, displayCutout, outInsets);
    }

    public void showTopBar() {
        if (HwPCUtils.enabled() && !isCloudOnPCTOP()) {
            try {
                IHwPCManager pcManager = HwPCUtils.getHwPCManager();
                if (pcManager != null) {
                    pcManager.showTopBar();
                }
            } catch (Exception e) {
                Log.e(TAG, "RemoteException");
            }
        }
    }

    private boolean isScreenLocked() {
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (km == null || !km.isKeyguardLocked()) {
            return false;
        }
        return true;
    }

    private boolean handleDesktopKeyEvent(KeyEvent event) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return false;
        }
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        int repeatCount = event.getRepeatCount();
        int displayId = this.mWindowManagerInternal.getFocusedDisplayId();
        if (!HwPCUtils.isValidExtDisplayId(displayId) && !HwPCUtils.enabledInPad()) {
            this.isHomePressDown = false;
            this.isHomeAndEBothDown = false;
            this.isHomeAndLBothDown = false;
            this.isHomeAndDBothDown = false;
            return false;
        } else if (HwPCUtils.enabledInPad() && handleExclusiveKeykoard(event).booleanValue()) {
            return true;
        } else {
            if (keyCode == 120 && down && repeatCount == 0) {
                screenshotPc();
                return true;
            } else if (keyCode == 134 && down && event.isAltPressed() && repeatCount == 0) {
                closeTopWindow();
                return true;
            } else if (keyCode == 33 && down && this.isHomePressDown && repeatCount == 0) {
                this.isHomeAndEBothDown = true;
                ComponentName componentName = new ComponentName("com.huawei.desktop.explorer", "com.huawei.filemanager.activities.MainActivity");
                Intent intent = new Intent();
                intent.setFlags(402653184);
                intent.setComponent(componentName);
                this.mContext.createDisplayContext(DisplayManagerGlobal.getInstance().getRealDisplay(displayId)).startActivity(intent);
                return true;
            } else if (keyCode == 40 && down && this.isHomePressDown && repeatCount == 0) {
                this.isHomeAndLBothDown = true;
                lockScreen(true);
                return true;
            } else if (keyCode == 32 && down && this.isHomePressDown && repeatCount == 0) {
                this.isHomeAndDBothDown = true;
                toggleHome();
                return true;
            } else {
                if (down && repeatCount == 0 && keyCode == 61) {
                    if (this.mRecentAppsHeldModifiers == 0 && !keyguardOn() && isUserSetupComplete()) {
                        int shiftlessModifiers = event.getModifiers() & -194;
                        if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2)) {
                            this.mRecentAppsHeldModifiers = shiftlessModifiers;
                            triggerSwitchTaskView(true);
                            return true;
                        }
                    }
                } else if (!(down || this.mRecentAppsHeldModifiers == 0 || (event.getMetaState() & this.mRecentAppsHeldModifiers) != 0)) {
                    this.mRecentAppsHeldModifiers = 0;
                    triggerSwitchTaskView(false);
                }
                if ((HwPCUtils.enabledInPad() || keyCode != 3) && keyCode != CPUFeature.MSG_RESET_TOP_APP_CPUSET && keyCode != CPUFeature.MSG_UNIPERF_BOOST_ON) {
                    return false;
                }
                if (down) {
                    this.isHomePressDown = true;
                } else {
                    if (this.isHomeAndEBothDown) {
                        this.isHomeAndEBothDown = false;
                    } else if (this.isHomeAndLBothDown) {
                        this.isHomeAndLBothDown = false;
                    } else if (this.isHomeAndDBothDown) {
                        this.isHomeAndDBothDown = false;
                    } else {
                        showStartMenu();
                    }
                    this.isHomePressDown = false;
                }
                return true;
            }
        }
    }

    private Boolean handleExclusiveKeykoard(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        int repeatCount = event.getRepeatCount();
        String str;
        StringBuilder stringBuilder;
        switch (keyCode) {
            case 3:
                dispatchKeyEventForExclusiveKeyboard(event);
                return Boolean.valueOf(true);
            case 4:
                if (down && repeatCount == 0) {
                    dispatchKeyEventForExclusiveKeyboard(event);
                    return Boolean.valueOf(false);
                }
            case WMStateCons.MSG_FREQUENTLOCATIONSTATE_WIFI_UPDATE_SCAN_RESULT /*62*/:
                if (down && event.isShiftPressed()) {
                    dispatchKeyEventForExclusiveKeyboard(event);
                    return Boolean.valueOf(true);
                }
            case CPUFeature.MSG_UNIPERF_BOOST_ON /*118*/:
                if (down) {
                    dispatchKeyEventForExclusiveKeyboard(event);
                }
                return Boolean.valueOf(true);
            case 187:
                if (isScreenLocked()) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ScreenLocked! Not handle");
                    stringBuilder.append(event);
                    HwPCUtils.log(str, stringBuilder.toString());
                    return Boolean.valueOf(true);
                }
                dispatchKeyEventForExclusiveKeyboard(event);
                return Boolean.valueOf(true);
            case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE /*220*/:
            case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE /*221*/:
                if (!isScreenLocked()) {
                    dispatchKeyEventForExclusiveKeyboard(event);
                    break;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ScreenLocked! Not handle");
                stringBuilder.append(event);
                HwPCUtils.log(str, stringBuilder.toString());
                return Boolean.valueOf(true);
            default:
                return Boolean.valueOf(false);
        }
        return Boolean.valueOf(false);
    }

    public void overrideRectForForceRotation(WindowState win, Rect pf, Rect df, Rect of, Rect cf, Rect vf, Rect dcf) {
        Rect rect = pf;
        Rect rect2 = df;
        Rect rect3 = of;
        Rect rect4 = cf;
        Rect rect5 = vf;
        Rect rect6 = dcf;
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (forceRotationManager.isForceRotationSupported() && forceRotationManager.isForceRotationSwitchOpen(this.mContext) && win != null && win.getAppToken() != null && win.getAttrs() != null) {
            String winTitle = String.valueOf(win.getAttrs().getTitle());
            if (!TextUtils.isEmpty(winTitle) && !winTitle.startsWith("SurfaceView") && !winTitle.startsWith("PopupWindow")) {
                if (win.isInMultiWindowMode()) {
                    Slog.v(TAG, "window is in multiwindow mode");
                    return;
                }
                Display defDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
                DisplayMetrics dm = new DisplayMetrics();
                defDisplay.getMetrics(dm);
                if (dm.widthPixels >= dm.heightPixels) {
                    Rect tmpRect = new Rect(rect5);
                    if (forceRotationManager.isAppForceLandRotatable(win.getAttrs().packageName, win.getAppToken().asBinder())) {
                        forceRotationManager.applyForceRotationLayout(win.getAppToken().asBinder(), tmpRect);
                        if (!tmpRect.equals(rect5)) {
                            int i = tmpRect.left;
                            rect5.left = i;
                            rect4.left = i;
                            rect2.left = i;
                            rect.left = i;
                            rect6.left = i;
                            rect3.left = i;
                            i = tmpRect.right;
                            rect5.right = i;
                            rect4.right = i;
                            rect2.right = i;
                            rect.right = i;
                            rect6.right = i;
                            rect3.right = i;
                        }
                        LayoutParams attrs = win.getAttrs();
                        attrs.privateFlags |= 64;
                    }
                }
            }
        }
    }

    public boolean isIntelliServiceEnabledFR(int orientatin) {
        return IntelliServiceManager.isIntelliServiceEnabled(this.mContext, orientatin, this.mCurrentUserId);
    }

    public void notifyRotationChange(int rotation) {
        this.mHwScreenOnProximityLock.refreshForRotationChange(rotation);
    }

    public int getRotationFromSensorOrFaceFR(int orientation, int lastRotation) {
        if (!IntelliServiceManager.isIntelliServiceEnabled(this.mContext, orientation, this.mCurrentUserId)) {
            return getRotationFromRealSensorFR(lastRotation);
        }
        String str;
        StringBuilder stringBuilder;
        int sensorRotation;
        if (IntelliServiceManager.getInstance(this.mContext).isKeepPortrait()) {
            str = IntelliServiceManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("portraitRotaion:");
            stringBuilder.append(0);
            Slog.d(str, stringBuilder.toString());
            return 0;
        } else if (IntelliServiceManager.getInstance(this.mContext).getFaceRotaion() != -2) {
            sensorRotation = IntelliServiceManager.getInstance(this.mContext).getFaceRotaion();
            str = IntelliServiceManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("faceRotaion:");
            stringBuilder.append(sensorRotation);
            Slog.d(str, stringBuilder.toString());
            return sensorRotation;
        } else {
            sensorRotation = getRotationFromRealSensorFR(lastRotation);
            str = IntelliServiceManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sensorRotaion:");
            stringBuilder.append(sensorRotation);
            Slog.d(str, stringBuilder.toString());
            return sensorRotation;
        }
    }

    public int getRotationFromRealSensorFR(int lastRotation) {
        int sensorRotation = this.mOrientationListener.getProposedRotation();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sensorRotation = ");
        stringBuilder.append(sensorRotation);
        stringBuilder.append(" lastRotation:");
        stringBuilder.append(lastRotation);
        Slog.d(str, stringBuilder.toString());
        if (sensorRotation < 0) {
            return lastRotation;
        }
        return sensorRotation;
    }

    public void setSensorRotationFR(int rotation) {
        IntelliServiceManager.setSensorRotation(rotation);
    }

    protected void notifyFingerSense(int rotation) {
        KnuckGestureSetting.getInstance().setOrientation(rotation);
    }

    public void startIntelliServiceFR() {
        IntelliServiceManager.getInstance(this.mContext).startIntelliService(this.mFaceRotationCallback);
    }

    public void resetCurrentNaviBarHeightExternal() {
        HwPCUtils.log(TAG, "resetCurrentNaviBarHeightExternal");
        if (HwPCUtils.enabled() && this.mNavigationBarHeightExternal != 0) {
            this.mNavigationBarHeightExternal = 0;
        }
    }

    private void initNavigationBarHightExternal(Display display, int width, int height) {
        if (display == null || this.mContext == null) {
            Log.e(TAG, "fail to ini nav, display or context is null");
            return;
        }
        if (HwPCUtils.enabled() && HwPCUtils.isValidExtDisplayId(display.getDisplayId())) {
            this.mNavigationBarHeightExternal = this.mContext.createDisplayContext(display).getResources().getDimensionPixelSize(34472195);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initNavigationBarHightExternal : mNavigationBarHeightExternal = ");
            stringBuilder.append(this.mNavigationBarHeightExternal);
            HwPCUtils.log(str, stringBuilder.toString());
            if (this.mExternalSystemGestures != null) {
                this.mExternalSystemGestures.screenHeight = height;
                this.mExternalSystemGestures.screenWidth = width;
            }
        }
    }

    public void layoutWindowLwForNotch(WindowState win, LayoutParams attrs) {
        if (IS_NOTCH_PROP) {
            boolean isNeverMode = false;
            boolean z = attrs.type >= 1 && attrs.type <= 99;
            this.isAppWindow = z;
            this.mIsNoneNotchAppInHideMode = this.hwNotchScreenWhiteConfig.isNoneNotchAppHideInfo(win);
            z = win.toString().contains("SnapshotStartingWindow");
            notchControlFillet(win);
            if (this.mDisplayRotation == 0 && !this.mIsNotchSwitchOpen) {
                if (this.hwNotchScreenWhiteConfig.getAppUseNotchMode(win.getOwningPackage()) == 2) {
                    isNeverMode = true;
                }
                setStatusBarColorForNotchMode(isNeverMode, z);
            }
        }
    }

    public boolean canLayoutInDisplayCutout(WindowState win) {
        boolean canLayoutInDisplayCutout = true;
        if (!IS_NOTCH_PROP) {
            return true;
        }
        int mode = this.hwNotchScreenWhiteConfig.getAppUseNotchMode(win.getOwningPackage());
        boolean isNeverMode = mode == 2;
        boolean isAlwaysMode = mode == 1;
        if (isNeverMode) {
            canLayoutInDisplayCutout = false;
        } else if (isAlwaysMode) {
            canLayoutInDisplayCutout = true;
        } else if ((this.mIsNotchSwitchOpen || !this.hwNotchScreenWhiteConfig.isNotchAppInfo(win)) && !((this.mIsNotchSwitchOpen && this.hwNotchScreenWhiteConfig.isNotchAppHideInfo(win)) || (win.getAttrs().hwFlags & 65536) != 0 || win.getHwNotchSupport() || win.getAttrs().layoutInDisplayCutoutMode == 1)) {
            canLayoutInDisplayCutout = false;
        }
        return canLayoutInDisplayCutout;
    }

    private void setStatusBarColorForNotchMode(boolean isNeverMode, boolean isSnapshotStartingWindow) {
        if (!isSnapshotStartingWindow && this.isAppWindow && isNeverMode && !this.mIsForceSetStatusBar) {
            notchStatusBarColorUpdate(1);
            this.mIsForceSetStatusBar = true;
            this.mIsRestoreStatusBar = false;
        } else if (!isSnapshotStartingWindow && this.isAppWindow && !this.mIsNotchSwitchOpen && !this.mIsRestoreStatusBar && !isNeverMode) {
            this.mIsForceSetStatusBar = false;
            this.mIsRestoreStatusBar = true;
            notchStatusBarColorUpdate(0);
        }
    }

    private void hideNotchRoundCorner() {
        this.mFirstSetCornerInLandNoNotch = false;
        this.mFirstSetCornerInLandNotch = true;
        transferSwitchStatusToSurfaceFlinger(0);
    }

    private void showNotchRoundCorner() {
        this.mFirstSetCornerInLandNotch = false;
        this.mFirstSetCornerInLandNoNotch = true;
        transferSwitchStatusToSurfaceFlinger(1);
    }

    private void notchControlFillet(WindowState win) {
        boolean flagGroup = false;
        boolean splashScreen;
        if (this.mIsNotchSwitchOpen) {
            if (this.mDisplayRotation == 2) {
                splashScreen = win.toString().contains("Splash Screen");
                boolean isTopWindow = this.mFocusedWindow != null && this.mFocusedWindow.toString().equals(win.toString());
                if (!splashScreen && this.isAppWindow && this.mFirstSetCornerInReversePortait && isTopWindow) {
                    this.mFirstSetCornerInReversePortait = false;
                    this.mFirstSetCornerDefault = true;
                    transferSwitchStatusToSurfaceFlinger(0);
                }
            } else if (this.mFirstSetCornerDefault) {
                this.mFirstSetCornerInReversePortait = true;
                this.mFirstSetCornerDefault = false;
                transferSwitchStatusToSurfaceFlinger(1);
            }
        } else if (this.mDisplayRotation == 1 || this.mDisplayRotation == 3) {
            this.mFirstSetCornerInPort = true;
            splashScreen = win.toString().contains("Splash Screen");
            boolean workSpace = win.toString().contains("com.huawei.intelligent.Workspace");
            boolean isNotchSupport = canLayoutInDisplayCutout(win);
            boolean z = this.mFocusedWindow != null && this.mFocusedWindow.toString().equals(win.toString());
            z = this.isAppWindow && z && !splashScreen;
            if (win.isInMultiWindowMode()) {
                if (!splashScreen && this.isAppWindow) {
                    flagGroup = true;
                }
                if (win.getWindowingMode() != 3) {
                    return;
                }
                if (flagGroup && isNotchSupport && this.mFirstSetCornerInLandNoNotch) {
                    hideNotchRoundCorner();
                } else if (flagGroup && !isNotchSupport && this.mFirstSetCornerInLandNotch) {
                    showNotchRoundCorner();
                }
            } else if (z && isNotchSupport && this.mFirstSetCornerInLandNoNotch) {
                hideNotchRoundCorner();
            } else if (!z || isNotchSupport || !this.mFirstSetCornerInLandNotch) {
            } else {
                if (this.mFirstSetCornerInLandNoNotch || !workSpace) {
                    showNotchRoundCorner();
                }
            }
        } else {
            this.mFirstSetCornerInLandNoNotch = true;
            this.mFirstSetCornerInLandNotch = true;
            if (this.mFirstSetCornerInPort) {
                this.mFirstSetCornerInPort = false;
                transferSwitchStatusToSurfaceFlinger(0);
            }
        }
    }

    private void transferSwitchStatusToSurfaceFlinger(int value) {
        int val = value;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Window issued fillet display val = ");
        stringBuilder.append(val);
        stringBuilder.append(", mIsNotchSwitchOpen = ");
        stringBuilder.append(this.mIsNotchSwitchOpen);
        stringBuilder.append(", mDisplayRotation = ");
        stringBuilder.append(this.mDisplayRotation);
        Slog.d(str, stringBuilder.toString());
        Parcel dataIn = Parcel.obtain();
        try {
            IBinder sfBinder = ServiceManager.getService("SurfaceFlinger");
            dataIn.writeInt(val);
            if (!(sfBinder == null || sfBinder.transact(NOTCH_ROUND_CORNER_CODE, dataIn, null, 1))) {
                Slog.d(TAG, "transferSwitchStatusToSurfaceFlinger error!");
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "transferSwitchStatusToSurfaceFlinger RemoteException on notify screen rotation animation end");
        } catch (Throwable th) {
            dataIn.recycle();
        }
        dataIn.recycle();
    }

    public void layoutWindowForPadPCMode(WindowState win, Rect pf, Rect df, Rect cf, Rect vf, int mContentBottom) {
        HwPCMultiWindowPolicy.layoutWindowForPadPCMode(win, pf, df, cf, vf, mContentBottom);
    }

    public void setSwitchingUser(boolean switching) {
        if (switching) {
            Slog.d(TAG, "face_rotation: switchUser unbindIntelliService");
            IntelliServiceManager.getInstance(this.mContext).unbindIntelliService();
        }
        super.setSwitchingUser(switching);
    }

    public boolean hideNotchStatusBar(int fl) {
        boolean hideNotchStatusBar = true;
        this.mBarVisibility = 1;
        boolean isNotchSupport = this.mFocusedWindow != null ? canLayoutInDisplayCutout(this.mFocusedWindow) : false;
        if (!IS_NOTCH_PROP || this.mDisplayRotation != 0) {
            return true;
        }
        if (this.mFocusedWindow != null && this.mFocusedWindow.toString().contains("com.huawei.intelligent")) {
            return true;
        }
        boolean statusBarFocused;
        if (this.mIsNotchSwitchOpen && !this.mTopFullscreenOpaqueWindowState.toString().contains(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE)) {
            statusBarFocused = (this.mFocusedWindow == null || (PolicyControl.getWindowFlags(null, this.mFocusedWindow.getAttrs()) & 2048) == 0) ? false : true;
            if (!((fl & 1024) == 0 && (this.mLastSystemUiFlags & 4) == 0 && (this.mFocusedWindow == null || (!this.mFocusedWindow.toString().contains("HwGlobalActions") && !this.mFocusedWindow.toString().contains("Sys2023:dream"))))) {
                hideNotchStatusBar = false;
                boolean z = this.mForceNotchStatusBar || !statusBarFocused;
                this.mForceNotchStatusBar = z;
                this.mBarVisibility = 0;
            }
            if (!this.mForceNotchStatusBar || !statusBarFocused) {
                return hideNotchStatusBar;
            }
            this.mBarVisibility = 1;
            this.mForceNotchStatusBar = false;
            this.notchStatusBarColorLw = 0;
            notchStatusBarColorUpdate(1);
            return false;
        } else if (isNotchSupport) {
            return true;
        } else {
            if (this.mFocusedWindow != null && (this.hwNotchScreenWhiteConfig.isNotchAppInfo(this.mFocusedWindow) || this.mFocusedWindow.toString().contains(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE))) {
                return true;
            }
            if (this.mFocusedWindow != null && (this.hwNotchScreenWhiteConfig.isNoneNotchAppWithStatusbarInfo(this.mFocusedWindow) || (((this.mFocusedWindow.getAttrs().hwFlags & 32768) != 0 && ((fl & 1024) != 0 || (this.mLastSystemUiFlags & 4) != 0)) || (this.mForceNotchStatusBar && (this.mFocusedWindow.toString().contains("SearchPanel") || (!this.mTopFullscreenOpaqueWindowState.toString().contains("Splash Screen") && !this.mTopFullscreenOpaqueWindowState.toString().equals(this.mFocusedWindow.toString()))))))) {
                this.mForceNotchStatusBar = true;
                hideNotchStatusBar = false;
                this.mBarVisibility = 0;
            } else if (this.mForceNotchStatusBar && this.mFocusedWindow != null && this.mFocusedWindow.getAttrs().type == 2 && ((PolicyControl.getWindowFlags(null, this.mFocusedWindow.getAttrs()) & 1024) != 0 || ((this.mFocusedWindow.getAttrs().hwFlags & 32768) != 0 && (this.mLastSystemUiFlags & 1024) != 0))) {
                this.mForceNotchStatusBar = true;
                hideNotchStatusBar = false;
                this.mBarVisibility = 0;
            } else if ((this.mFocusedWindow == null && this.mForceNotchStatusBar && !(((fl & 1024) == 0 && (this.mLastSystemUiFlags & 4) == 0) || this.mTopFullscreenOpaqueWindowState.toString().contains("Splash Screen"))) || !(this.mTopFullscreenOpaqueWindowState.getAttrs().type == 3 || (fl & 1024) == 0 || !this.mTopFullscreenOpaqueWindowState.toString().contains("Splash Screen"))) {
                this.mForceNotchStatusBar = true;
                hideNotchStatusBar = false;
                this.mBarVisibility = 0;
            }
            statusBarFocused = isKeyguardShowingOrOccluded() || (this.mKeyguardDelegate != null && this.mKeyguardDelegate.isOccluded());
            if (!statusBarFocused) {
                return hideNotchStatusBar;
            }
            this.mForceNotchStatusBar = false;
            this.mBarVisibility = 1;
            return true;
        }
    }

    public void notchStatusBarColorUpdate(int statusbarStateFlag) {
        if (!this.mIsForceSetStatusBar) {
            if (this.mFocusedWindow != null) {
                LayoutParams attrs = this.mFocusedWindow.getAttrs();
                this.mLastNavigationBarColor = attrs.navigationBarColor;
                this.mLastStatusBarColor = attrs.statusBarColor;
                this.mLastIsEmuiStyle = getEmuiStyleValue(attrs.isEmuiStyle);
                this.mLastIsEmuiLightStyle = getEmuiLightStyleValue(attrs.hwFlags);
            }
            notchTransactToStatusBarService(121, "notchTransactToStatusBarService", this.mLastIsEmuiStyle, this.mLastStatusBarColor, this.mLastNavigationBarColor, this.mLastIsEmuiLightStyle, statusbarStateFlag, this.mBarVisibility);
        }
    }

    protected void wakeUpFromPowerKey(long eventTime) {
        doFaceRecognize(true, "FCDT-POWERKEY");
        super.wakeUpFromPowerKey(eventTime);
    }

    public void doFaceRecognize(boolean detect, String reason) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.doFaceRecognize(detect, reason);
        }
    }

    public void notchTransactToStatusBarService(int code, String transactName, int isEmuiStyle, int statusbarColor, int navigationBarColor, int isEmuiLightStyle, int statusbarStateFlag, int barVisibility) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (getHWStatusBarService() != null) {
                IBinder statusBarServiceBinder = getHWStatusBarService().asBinder();
                if (statusBarServiceBinder != null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("set statusbarColor:");
                    stringBuilder.append(statusbarColor);
                    stringBuilder.append(", barVisibility: ");
                    stringBuilder.append(barVisibility);
                    stringBuilder.append(" to status bar service");
                    Log.d(str, stringBuilder.toString());
                    data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                    data.writeInt(isEmuiStyle);
                    data.writeInt(statusbarColor);
                    data.writeInt(navigationBarColor);
                    data.writeInt(isEmuiLightStyle);
                    data.writeInt(statusbarStateFlag);
                    data.writeInt(barVisibility);
                    statusBarServiceBinder.transact(code, data, reply, 0);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "notchTransactToStatusBarService->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
        }
        reply.recycle();
        data.recycle();
    }

    private boolean handleInputEventInPCCastMode(KeyEvent event) {
        if (HwPCUtils.isPcCastModeInServer() && this.mLighterDrawView != null && this.mLighterDrawView.isVisibleLw()) {
            IHwPCManager pcManager = HwPCUtils.getHwPCManager();
            if (pcManager != null) {
                try {
                    return pcManager.shouldInterceptInputEvent(event, false);
                } catch (RemoteException e) {
                    Log.e(TAG, "interceptInputEventInPCCastMode()");
                }
            }
        }
        return false;
    }

    public void setPowerState(int powerState) {
        HwAodManager.getInstance().setPowerState(powerState);
    }

    public void pause() {
        HwAodManager.getInstance().pause();
    }

    public int getDeviceNodeFD() {
        return HwAodManager.getInstance().getDeviceNodeFD();
    }

    public int getHardwareType() {
        return FingerprintManagerEx.getHardwareType();
    }

    protected void uploadKeyEvent(int keyEvent) {
        if (this.mContext != null) {
            switch (keyEvent) {
                case 3:
                    StatisticalUtils.reportc(this.mContext, 173);
                    break;
                case 4:
                    StatisticalUtils.reportc(this.mContext, 172);
                    break;
            }
        }
    }

    public boolean isHwStartWindowEnabled() {
        return StartWindowFeature.isStartWindowEnable();
    }

    public Context addHwStartWindow(String packageName, Context overrideContext, Context context, TypedArray typedArray, int windowFlags) {
        if (overrideContext == null || context == null || typedArray == null) {
            return null;
        }
        boolean addHwStartWindowFlag = false;
        boolean windowIsTranslucent = typedArray.getBoolean(true, false);
        boolean windowDisableStarting = typedArray.getBoolean(true, false);
        boolean windowShowWallpaper = typedArray.getBoolean(true, false);
        ApplicationInfo appInfo = context.getApplicationInfo();
        boolean isUnistall = (appInfo.isSystemApp() || appInfo.isPrivilegedApp() || appInfo.isUpdatedSystemApp()) ? false : true;
        if ((windowDisableStarting || windowIsTranslucent || (windowShowWallpaper && (windowFlags & HighBitsCompModeID.MODE_COLOR_ENHANCE) != HighBitsCompModeID.MODE_COLOR_ENHANCE)) && HwStartWindowRecord.getInstance().isStartWindowApp(packageName) && HwStartWindowRecord.getInstance().checkStartWindowApp(packageName)) {
            addHwStartWindowFlag = true;
        }
        if (!addHwStartWindowFlag || !isUnistall) {
            return null;
        }
        overrideContext.setTheme(overrideContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.NoActionBar", null, null));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addHwStartWindow set default on app : ");
        stringBuilder.append(packageName);
        Slog.d(str, stringBuilder.toString());
        return overrideContext;
    }
}
