package com.android.server.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
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
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.cover.CoverManager;
import android.database.ContentObserver;
import android.freeform.HwFreeFormManager;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.HwFoldScreenState;
import android.hardware.input.InputManager;
import android.hdm.HwDeviceManager;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Trace;
import android.os.UserHandle;
import android.os.Vibrator;
import android.pc.IHwPCManager;
import android.provider.Settings;
import android.provider.SettingsEx;
import android.swing.HwSwingManager;
import android.swing.IHwSwingService;
import android.telecom.TelecomManager;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.HwStylusUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyConstants;
import android.view.accessibility.AccessibilityManager;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ITelephony;
import com.android.server.CoordinationStackDividerManager;
import com.android.server.LocalServices;
import com.android.server.am.HwActivityManagerService;
import com.android.server.displayside.HwDisplaySideRegionConfig;
import com.android.server.foldscreenview.SubScreenViewEntry;
import com.android.server.gesture.GestureNavConst;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.gesture.HwGestureNavWhiteConfig;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.appmng.ContinuePowerDevMng;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.feature.StartWindowFeature;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.tv.HwTvPowerManagerPolicy;
import com.android.server.utils.CommonThread;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.HwStartWindowRecord;
import com.android.server.wm.IntelliServiceManager;
import com.android.server.wm.WindowState;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.app.IGameObserver;
import com.huawei.android.app.WindowManagerExt;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import com.huawei.android.gameassist.HwGameAssistManager;
import com.huawei.android.inputmethod.HwInputMethodManager;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.android.util.NoExtAPIException;
import com.huawei.android.view.HwExtDisplaySizeUtil;
import com.huawei.bd.Reporter;
import com.huawei.forcerotation.HwForceRotationManager;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.HwPCFactory;
import com.huawei.server.hwmultidisplay.DefaultHwMultiDisplayUtils;
import com.huawei.server.hwmultidisplay.windows.DefaultHwWindowsCastManager;
import com.huawei.server.policy.HwAccessibilityWaterMark;
import com.huawei.server.security.behaviorcollect.BehaviorCollector;
import com.huawei.server.wm.IHwDisplayPolicyEx;
import com.huawei.sidetouch.HwSideStatusManager;
import com.huawei.sidetouch.HwSideTouchDataReport;
import com.huawei.sidetouch.HwSideTouchManager;
import com.huawei.sidetouch.HwSideVibrationManager;
import com.huawei.sidetouch.IHwSideTouchCallback;
import com.huawei.systemmanager.power.HwDeviceIdleController;
import dalvik.system.DexClassLoader;
import huawei.android.aod.HwAodManager;
import huawei.android.app.IHwWindowCallback;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import huawei.android.hwutil.HwFullScreenDisplay;
import huawei.android.provider.FrontFingerPrintSettings;
import huawei.android.security.facerecognition.FaceReportEventToIaware;
import huawei.android.view.HwWindowManager;
import huawei.com.android.server.fingerprint.FingerViewController;
import huawei.com.android.server.policy.HwFalseTouchMonitor;
import huawei.com.android.server.policy.HwScreenOnProximityLock;
import huawei.com.android.server.policy.stylus.StylusGestureListener;
import huawei.cust.HwCustUtils;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;
import vendor.huawei.hardware.tp.V1_0.ITouchscreen;

public class HwPhoneWindowManager extends PhoneWindowManager implements AccessibilityManager.TouchExplorationStateChangeListener {
    private static final String ACTION_ACTURAL_SHUTDOWN = "com.android.internal.app.SHUTDOWNBROADCAST";
    private static final String ACTION_HUAWEI_VASSISTANT_SERVICE = "com.huawei.ziri.model.MODELSERVICE";
    private static final String ACTIVITY_NAME_EMERGENCY_SIMPLIFIEDINFO = "com.android.emergency/.view.ViewSimplifiedInfoActivity";
    private static final String AOD_GOING_TO_SLEEP_ACTION = "com.huawei.aod.action.AOD_GOING_TO_SLEEP";
    private static final int AOD_TOUCH_SWITCH_CTRL = 6;
    private static final int AOD_TOUCH_TIME_INDEX = 1;
    private static final String AOD_WAKE_UP_ACTION = "com.huawei.aod.action.AOD_WAKE_UP";
    private static final int BACK_HOME_RECENT_DOUBLE_CLICK_TIMEOUT = 300;
    private static final String BOOT_ANIM_EXIT_PROP = "service.bootanim.exit";
    private static final String BOOT_ANIM_NOT_EXIT_PROP_VALUE = "0";
    static final boolean DEBUG = false;
    static final boolean DEBUG_IMMERSION = false;
    private static final int DEFAULT_MAX_POWERKEY_COUNTDOWN = 5;
    private static final int DEFAULT_RESULT_VALUE = -2;
    private static final long DISABLE_VOLUMEDOWN_DOUBLE_CLICK_INTERVAl_MAX = 15000;
    private static final long DISABLE_VOLUMEDOWN_DOUBLE_CLICK_INTERVAl_MIN = 1500;
    private static final int DISPLAY_MODE_TYPE_INDEX = 0;
    static final String DROP_SMARTKEY_ACTIVITY = "drop_smartkey_activity";
    private static final int EVENT_DURING_MIN_TIME = 500;
    private static final long FILM_REPORT_TIME_INTERVAL = 86400000;
    private static final String FILM_STATE = "film_state";
    private static final int FILM_STATE_INIT_VALUE = -1;
    static final String FINGERPRINT_ANSWER_CALL = "fp_answer_call";
    static final String FINGERPRINT_CAMERA_SWITCH = "fp_take_photo";
    private static final int FINGERPRINT_HARDWARE_OPTICAL = 1;
    static final String FINGERPRINT_STOP_ALARM = "fp_stop_alarm";
    private static final String FINGERSENSE_JAR_PATH = "/hw_product/jar/FingerSense/hwFingerSense.jar";
    private static final int FINGER_PRINT_TOUCH_TIME_INDEX = 2;
    private static final int FLOATING_MASK = Integer.MIN_VALUE;
    private static final int FORCE_STATUS_BAR = 1;
    public static final String FRONT_FINGERPRINT_BUTTON_LIGHT_MODE = "button_light_mode";
    public static final String FRONT_FINGERPRINT_SWAP_KEY_POSITION = "swap_key_position";
    public static final String HAPTIC_FEEDBACK_TRIKEY_SETTINGS = "physic_navi_haptic_feedback_enabled";
    private static final String HIVOICE_PRESS_TYPE_POWER = "power";
    private static final String HOMOAI_EVENT_TAG = "event_type";
    private static final String HOMOAI_PRESS_TAG = "press_type";
    private static final int HOMOKEY_LONGPRESS_EVENT = 1;
    private static final String HUAWEI_ANDROID_INCALL_UI = "com.android.incallui";
    private static final String HUAWEI_HIACTION_ACTION = "com.huawei.hiaction.HOMOAI";
    private static final String HUAWEI_HIACTION_PACKAGE = "com.huawei.hiaction";
    private static final String HUAWEI_RAPIDCAPTURE_START_MODE = "com.huawei.RapidCapture";
    private static final String HUAWEI_SCREENRECORDER_ACTION = "com.huawei.screenrecorder.Start";
    private static final String HUAWEI_SCREENRECORDER_PACKAGE = "com.huawei.screenrecorder";
    private static final String HUAWEI_SCREENRECORDER_START_MODE = "com.huawei.screenrecorder.ScreenRecordService";
    private static final String HUAWEI_SHUTDOWN_PERMISSION = "huawei.android.permission.HWSHUTDOWN";
    private static final String HUAWEI_VASSISTANT_EXTRA_START_MODE = "com.huawei.vassistant.extra.SERVICE_START_MODE";
    private static final String HUAWEI_VASSISTANT_PACKAGE = "com.huawei.vassistant";
    private static final String HUAWEI_VASSISTANT_PACKAGE_OVERSEA = "com.huawei.hiassistantoversea";
    private static final String HUAWEI_VOICE_DEBUG_BETACLUB = "com.huawei.betaclub";
    private static final String HUAWEI_VOICE_SOUNDTRIGGER_BROADCAST = "com.mmc.SOUNDTRIGGER";
    private static final String HUAWEI_VOICE_SOUNDTRIGGER_PACKAGE = "com.mmc.soundtrigger";
    private static final boolean HWRIDEMODE_FEATURE_SUPPORTED = SystemProperties.getBoolean("ro.config.ride_mode", false);
    private static final long HWVASSISTANT_STAY_IN_WHITELIST_TIMEOUT = 3000;
    private static final String HW_NOTEEDITOR_ACTIVITY_NAME = "com.huawei.notepad/com.huawei.android.notepad.views.SketchActivity";
    private static final int INVALID_HARDWARE_TYPE = -1;
    private static final int IN_SCREEN_OPTIC_TYPE = 1;
    private static final int IN_SCREEN_ULTRA_TYPE = 2;
    private static final boolean IS_LONG_HOME_VASSITANT = SystemProperties.getBoolean("ro.hw.long.home.vassistant", true);
    private static final boolean IS_POWER_HIACTION_KEY = SystemProperties.getBoolean("ro.config.hw_power_voice_key", true);
    private static final boolean IS_SUPPORT_FOLD_SCREEN = (!SystemProperties.get("ro.config.hw_fold_disp").isEmpty());
    private static final boolean IS_SUPPORT_HW_BEHAVIOR_AUTH = SystemProperties.getBoolean("hw_mc.authentication.behavior_auth_bot", true);
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", MemoryConstant.MEM_SCENE_DEFAULT));
    private static final List KEYCODE_NOT_FOR_CLOUD = Arrays.asList(4, 3, 25, 24, Integer.valueOf((int) HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE), Integer.valueOf((int) HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE));
    private static final String KEY_DOUBLE_TAP_PAY = "double_tap_enable_pay";
    private static final String KEY_HIVOICE_PRESS_TYPE = "invoke_hivoice_keypress_type";
    private static final String KEY_HWOUC_KEYGUARD_VIEW_ON_TOP = "hwouc_keyguard_view_on_top";
    private static final String KEY_POWER_LONGPRESS_TIMEOUT = "power_longpress_timeout";
    private static final String KEY_TOAST_POWER_OFF = "toast_power_off";
    private static final String KEY_TOUCH_DISABLE_MODE = "touch_disable_mode";
    private static final String[] LMT_CONFIGS = LMT_DISPLAY_CONFIG.split(",");
    private static final int LMT_CONFIG_LENGTH = 3;
    private static final String LMT_DISPLAY_CONFIG = SystemProperties.get("hw_mc.aod.support_display_style", "");
    private static final int LMT_MODE_MOVE = 1;
    private static final int LMT_MODE_MOVE_TOUCH = 3;
    private static final int LMT_MODE_TOUCH = 2;
    private static final int LMT_SWITCH_DEFAULT = 0;
    private static final int LMT_SWITCH_MOVE_TOUCH = 3;
    private static final int LMT_SWITCH_TOUCH = 2;
    private static final int LMT_TOUCH_OFF = 0;
    private static final int LMT_TOUCH_ON = 1;
    private static final int MAX_POWERKEY_COUNTDOWN = SystemProperties.getInt("hw_mc.power_manage.active_sos_times", 5);
    private static final int MAX_POWEROFF_TOAST_SHOW = 2;
    private static final int MRX_KEYBOARD_PID = 4253;
    private static final int MRX_KEYBOARD_VID = 4817;
    private static final int MSG_BUTTON_LIGHT_TIMEOUT = 4099;
    private static final int MSG_DISABLE_SWING = 106;
    private static final int MSG_ENABLE_SWING = 105;
    private static final int MSG_FREEZE_POWER_KEY = 4103;
    private static final int MSG_NAVIBAR_DISABLE = 104;
    private static final int MSG_NAVIBAR_ENABLE = 103;
    private static final int MSG_NOTIFY_FINGER_OPTICAL = 4100;
    private static final int MSG_POWER_KEY_PRESS = 109;
    private static final int MSG_SCREEN_ON_EX_FINISHED = 108;
    private static final int MSG_SET_FOLD_MODE_FINISHED = 107;
    private static final int MSG_TRIKEY_BACK_LONG_PRESS = 4097;
    private static final int MSG_TRIKEY_RECENT_LONG_PRESS = 4098;
    private static final int MSG_VOICE_ASSIST_KEY_PRESS = 110;
    private static final boolean NEED_TAILORED = SystemProperties.getBoolean("ro.config.need_tailored", false);
    private static final String NEW_HWNOTEPAD_PKG_NAME = "com.huawei.notepad";
    private static final int NON_TOUGHENED_FILM = 1;
    private static final int NOTCH_ROUND_CORNER_CODE = 8002;
    private static final int NOTCH_ROUND_CORNER_HIDE = 0;
    private static final int NOTCH_ROUND_CORNER_SHOW = 1;
    private static final int NOTCH_ROUND_CORNER_SIDE_COMPRESS = 1;
    private static final int NOTCH_ROUND_CORNER_SIDE_EXPAND = 0;
    private static final int NOTCH_STATUS_BAR_BLACK = 1;
    private static final int NOTCH_STATUS_BAR_DEFAULT = 0;
    private static final String NOTEEDITOR_ACTIVITY_NAME = "com.example.android.notepad/com.huawei.android.notepad.views.SketchActivity";
    private static final int NOTEPAD_START_DELAY = 500;
    private static final int NOTEPAD_START_DURATION = 2000;
    private static final int NOTIFY_HIACTION_LONGPRESS_TYPE_HOMO = 1;
    private static final int NOTIFY_HIACTION_LONGPRESS_TYPE_NONE = 0;
    private static final int NOTIFY_HIACTION_LONGPRESS_TYPE_POWER = 2;
    private static final boolean OPEN_PROXIMITY_DISPALY = SystemProperties.getBoolean("ro.config.open_proximity_display", false);
    private static final String PACKAGE_NAME_HWPCASSISTANT = "com.huawei.pcassistant";
    private static final String PKG_CALCULATOR = "com.huawei.calculator";
    private static final String PKG_CALCULATOR_OLD = "com.android.calculator2";
    private static final String PKG_CAMERA = "com.huawei.camera";
    private static final String PKG_GALLERY = "com.huawei.photos";
    private static final String PKG_GALLERY_OLD = "com.android.gallery3d";
    private static final String PKG_HWNOTEPAD = "com.example.android.notepad";
    private static final String PKG_NAME_EMERGENCY = "com.android.emergency";
    private static final String PKG_SCANNER = "com.huawei.scanner";
    private static final String PKG_SOUNDRECORDER = "com.huawei.soundrecorder";
    private static final String PKG_SOUNDRECORDER_OLD = "com.android.soundrecorder";
    private static final int POWERKEY_LONG_PRESS_TIMEOUT = 700;
    private static final String POWERKEY_QUICKPAY_ACTION = "com.huawei.oto.intent.action.QUICKPAY";
    private static final String POWERKEY_QUICKPAY_PACKAGE = "com.huawei.wallet";
    private static final long POWER_SOS_MISTOUCH_THRESHOLD = 300;
    private static final long POWER_SOS_TIMEOUT = 500;
    private static final String PROXIMITY_UI_WINDOW_TITLE = "Emui:ProximityWnd";
    private static final long SCREENRECORDER_DEBOUNCE_DELAY_MILLIS = 150;
    private static final int SCREEN_CHANGE_REASON_FEATURE = 4;
    private static final String SCREEN_POWER_CHANGED_ACTION = "com.huawei.action.ACTION_SCREEN_POWER_CHANGED";
    private static final String SCREEN_POWER_CHANGED_FLAG_EXTRA = "flag";
    private static final int SIDE_POWER_FP_COMB = 3;
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SINGLE_HAND_STATE = 1989;
    private static final long SMARTKEY_DOUBLE_CLICK_TIMEOUT = 400;
    private static final long SMARTKEY_LONG_PRESS_TIMEOUT = 500;
    private static final int START_MODE_QUICK_START_CALL = 2;
    static final int START_MODE_VOICE_WAKEUP_ONE_SHOT = 4;
    private static final boolean SUPPORT_RAPID_CAPTURE = SystemProperties.getBoolean("ro.hwcamera.fastcapture", true);
    private static final long SYSTRACELOG_DEBOUNCE_DELAY_MILLIS = 150;
    private static final long SYSTRACELOG_FINGERPRINT_EFFECT_DELAY = 750;
    static final String TAG = "HwPhoneWindowManager";
    private static final int TOAST_POWER_OFF_TIMEOUT = 1000;
    private static final int TOAST_TYPE_COVER_SCREEN = 2101;
    private static final long TOUCH_DISABLE_DEBOUNCE_DELAY_MILLIS = 150;
    private static final int TOUCH_EXPLR_NAVIGATION_BAR_COLOR = -16777216;
    private static final int TOUCH_EXPLR_STATUS_BAR_COLOR = -16777216;
    private static final long TOUCH_SPINNING_DELAY_MILLIS = 2000;
    private static final int TOUGHENED_FILM = 2;
    private static final int TP_HAL_DEATH_COOKIE = 1001;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final int UNDEFINED_TYPE = -1;
    private static final String UPDATE_GESTURE_NAV_ACTION = "huawei.intent.action.FWK_UPDATE_GESTURE_NAV_ACTION";
    private static final String UPDATE_NOTCH_SCREEN_ACTION = "huawei.intent.action.FWK_UPDATE_NOTCH_SCREEN_ACTION";
    private static final String UPDATE_NOTCH_SCREEN_PER = "com.huawei.systemmanager.permission.ACCESS_INTERFACE";
    private static final String VIBRATE_ON_TOUCH = "vibrate_on_touch";
    /* access modifiers changed from: private */
    public static final int VIBRATOR_LONG_PRESS_FOR_FRONT_FP = SystemProperties.getInt("ro.config.trikey_vibrate_press", 16);
    private static final int VIBRATOR_SHORT_PRESS_FOR_FRONT_FP = SystemProperties.getInt("ro.config.trikey_vibrate_touch", 8);
    private static String VOICE_ASSISTANT_ACTION = "com.huawei.action.VOICE_ASSISTANT";
    private static final String VOICE_ASSIST_KEY_DOWN_ACTION = "com.huawei.key.action.VOICE_ASSIST_KEY_DOWN";
    private static final String VOICE_ASSIST_KEY_UP_ACTION = "com.huawei.key.action.VOICE_ASSIST_KEY_UP";
    private static final long VOLUMEDOWN_DOUBLE_CLICK_TIMEOUT = 400;
    private static final long VOLUMEDOWN_LONG_PRESS_TIMEOUT = 500;
    private static final long WAITING_SCREEN_ON_EX_TIMEOUT = 1500;
    private static final int WAKEUP_NOTEEDITOR_TIMEOUT = 500;
    private static boolean isSwich = false;
    private static final boolean isVibrateImplemented = false;
    private static boolean mCustBeInit = false;
    private static boolean mCustUsed = false;
    private static final boolean mIsHwEasyWakeup = SystemProperties.getBoolean("ro.config.hw_easywakeup", false);
    /* access modifiers changed from: private */
    public static boolean mIsNeedMuteByPowerKeyDown = false;
    private static boolean mIsSidePowerFpComb = false;
    private static PhoneStateListener mPhoneListener = new PhoneStateListener() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass1 */

        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.i(HwPhoneWindowManager.TAG, "PhoneState = " + state);
            boolean unused = HwPhoneWindowManager.mIsNeedMuteByPowerKeyDown = state == 1;
            if (state == 1) {
                HwPhoneWindowManager.sHwMultiDisplayUtils.lightScreenOnForHwMultiDisplay();
            }
        }
    };
    private static final List<String> mSceneActivitys = Arrays.asList("com.huawei.camera/com.huawei.camera", "com.android.deskclock/com.android.deskclock.timer.TimerAlertActivity", "com.huawei.deskclock/com.android.deskclock.timer.TimerAlertActivity", "com.android.deskclock/com.android.deskclock.alarmclock.LockAlarmFullActivity", "com.huawei.deskclock/com.android.deskclock.alarmclock.LockAlarmFullActivity", "com.huawei.camera/com.huawei.camera.controller.SecureCameraActivity");
    private static final boolean mSupportDoubleTapPay = SystemProperties.getBoolean("ro.config.support_doubletap_pay", false);
    private static final boolean mSupportGameAssist = (SystemProperties.getInt("ro.config.gameassist", 0) == 1);
    private static int[] mUnableWakeKey;
    private static DexClassLoader sDexClassLoader;
    /* access modifiers changed from: private */
    public static DefaultHwMultiDisplayUtils sHwMultiDisplayUtils = HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwMultiDisplayUtils();
    private static DefaultHwWindowsCastManager sHwWindowsCastManager = HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwWindowsCastManager();
    private static final String sRingVolumeBarDisp = SystemProperties.get("ro.config.hw_curved_side_disp", "");
    private boolean DEBUG_SMARTKEY = false;
    private final int KEYGUARD_PROXIMITY_OFF = 21;
    private final int KEYGUARD_PROXIMITY_ON = 20;
    /* access modifiers changed from: private */
    public int TRIKEY_NAVI_DEFAULT_MODE = -1;
    /* access modifiers changed from: private */
    public AlertDialog alertDialog;
    /* access modifiers changed from: private */
    public BehaviorCollector behaviorCollector = null;
    FingerprintActionsListener fingerprintActionsListener;
    private HwNotchScreenWhiteConfig hwNotchScreenWhiteConfig;
    private boolean isAppWindow = false;
    private boolean isFingerAnswerPhoneOn = false;
    private boolean isFingerShotCameraOn = false;
    private boolean isFingerStopAlarmOn = false;
    boolean isHomeAndDBothDown = false;
    boolean isHomeAndEBothDown = false;
    boolean isHomeAndLBothDown = false;
    private boolean isHomeAndLBothPressed = false;
    boolean isHomePressDown = false;
    private boolean isHwOUCKeyguardViewOnTop = false;
    private boolean isLDown = false;
    private boolean isNotchTemp;
    /* access modifiers changed from: private */
    public boolean isVoiceRecognitionActive;
    private int lastDensityDpi = -1;
    private final Runnable mAIPowerLongPressed = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass30 */

        public void run() {
            Log.i(HwPhoneWindowManager.TAG, "handle power long press");
            HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
            hwPhoneWindowManager.mPowerKeyHandledByHiaction = true;
            hwPhoneWindowManager.handleHomoAiKeyLongPress(2);
        }
    };
    /* access modifiers changed from: private */
    public HwAccessibilityWaterMark mAccessbilityWaterMark = null;
    private final IZrHung mAppEyeBackKey = HwFrameworkFactory.getZrHung("appeye_backkey");
    private final IZrHung mAppEyeHomeKey = HwFrameworkFactory.getZrHung("appeye_homekey");
    private AudioManager.AudioPlaybackCallback mAudioPlaybackCallback = null;
    private boolean mBackKeyPress = false;
    private long mBackKeyPressTime = 0;
    private Light mBackLight = null;
    volatile boolean mBackTrikeyHandled;
    private int mBarVisibility = 1;
    private PowerManager.WakeLock mBlePowerKeyWakeLock;
    boolean mBooted = false;
    private PowerManager.WakeLock mBroadcastWakeLock;
    /* access modifiers changed from: private */
    public Light mButtonLight = null;
    /* access modifiers changed from: private */
    public int mButtonLightMode = 1;
    private boolean mCanSearch = false;
    private final Runnable mCancleInterceptFingerprintEvent = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass29 */

        public void run() {
            boolean unused = HwPhoneWindowManager.this.mNeedDropFingerprintEvent = false;
        }
    };
    private CoverManager mCoverManager = null;
    private boolean mCoverOpen = true;
    private int mCurUser;
    HwCustPhoneWindowManager mCust = ((HwCustPhoneWindowManager) HwCustUtils.createObj(HwCustPhoneWindowManager.class, new Object[0]));
    int mDesiredRotation = -1;
    private int mDeviceNodeFD = -2147483647;
    /* access modifiers changed from: private */
    public boolean mDeviceProvisioned = false;
    /* access modifiers changed from: private */
    public boolean mDoubleTapPay = false;
    /* access modifiers changed from: private */
    public boolean mEnableKeyInCurrentFgGameApp;
    private HwFalseTouchMonitor mFalseTouchMonitor = null;
    /* access modifiers changed from: private */
    public int mFilmState;
    private int mFingerPrintId = -2;
    private int mFingerprintHardwareType = -1;
    private ContentObserver mFingerprintObserver;
    private int mFingerprintType = -1;
    private boolean mFirstSetCornerDefault = true;
    private boolean mFirstSetCornerInLandNoNotch = true;
    private boolean mFirstSetCornerInLandNotch = true;
    private boolean mFirstSetCornerInPort = true;
    private boolean mFirstSetCornerInReversePortait = true;
    private HwFoldScreenManagerInternal mFoldScreenManagerService;
    private FoldScreenOnUnblocker mFoldScreenOnUnblocker;
    private final Runnable mFoldWindowDrawCallback = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass36 */

        public void run() {
            if (PhoneWindowManager.DEBUG_WAKEUP) {
                Slog.i(HwPhoneWindowManager.TAG, "All windows draw complete for fold screen");
            }
            synchronized (HwPhoneWindowManager.this.mScreenOnExLock) {
                HwPhoneWindowManager.this.mHandlerEx.sendEmptyMessage(108);
            }
        }
    };
    private Class mFsManagerCls = null;
    private Object mFsManagerObj = null;
    private HwGameDockGesture mGameDockGesture = null;
    private GestureNavPolicy mGestureNavPolicy;
    private final Runnable mHandleVolumeDownKey = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass19 */

        public void run() {
            if (HwPhoneWindowManager.this.isMusicActive()) {
                HwPhoneWindowManager.this.handleVolumeKey(3, 25);
            }
        }
    };
    /* access modifiers changed from: private */
    public Handler mHandlerEx;
    /* access modifiers changed from: private */
    public boolean mHapticEnabled = true;
    private boolean mHeadless;
    /* access modifiers changed from: private */
    public String mHiVoiceKeyType;
    private boolean mHintShown;
    private HwGameSpaceToggleManager mHwGameSpaceToggleManager = null;
    private ContentObserver mHwOUCObserver;
    /* access modifiers changed from: private */
    public HwScreenOnProximityLock mHwScreenOnProximityLock;
    public IHwWindowCallback mIHwWindowCallback;
    /* access modifiers changed from: private */
    public boolean mIsActuralShutDown = false;
    /* access modifiers changed from: private */
    public boolean mIsAudioPlaying = false;
    /* access modifiers changed from: private */
    public boolean mIsForceSetStatusBar = false;
    /* access modifiers changed from: private */
    public boolean mIsFreezePowerkey = false;
    private boolean mIsHasActionBar;
    private boolean mIsLayoutBelowWhenHideNotch = false;
    boolean mIsNavibarAlignLeftWhenLand;
    public boolean mIsNoneNotchAppInHideMode = false;
    protected boolean mIsNotchSwitchOpen = false;
    /* access modifiers changed from: private */
    public boolean mIsProximity = false;
    private boolean mIsRestoreStatusBar = false;
    private boolean mIsSideTouchEvent = false;
    private boolean mIsSupportSideScreen = false;
    private boolean mIsSwingMotionEnable = false;
    private boolean mIsTouchExplrEnabled;
    private boolean mIsVolumePanelVisible = false;
    private String[] mKeyguardShortcutApps = {"com.huawei.camera", PKG_GALLERY, PKG_GALLERY_OLD, PKG_SCANNER};
    private WindowState mLastColorWin;
    /* access modifiers changed from: private */
    public String mLastFgPackageName;
    /* access modifiers changed from: private */
    public long mLastFilmReportTimeMs;
    /* access modifiers changed from: private */
    public int mLastIsEmuiStyle;
    private boolean mLastKeyDownDropped;
    private int mLastKeyDownKeyCode;
    private long mLastKeyDownTime;
    private long mLastKeyPointerTime = 0;
    /* access modifiers changed from: private */
    public int mLastNavigationBarColor;
    private long mLastPowerKeyDownTime = 0;
    /* access modifiers changed from: private */
    public long mLastPowerWalletDownTime = 0;
    /* access modifiers changed from: private */
    public long mLastStartVassistantServiceTime;
    /* access modifiers changed from: private */
    public int mLastStatusBarColor;
    private long mLastVolumeDownKeyDownTime;
    private long mLastVolumeKeyDownTime = 0;
    private long mLastWakeupTime = 0;
    public WindowState mLighterDrawView;
    private ProximitySensorListener mListener = null;
    private WindowManagerPolicyConstants.PointerEventListener mLockScreenBuildInDisplayListener;
    private WindowManagerPolicyConstants.PointerEventListener mLockScreenListener;
    private String[] mLsKeyguardShortcutApps = {PKG_SOUNDRECORDER, PKG_CALCULATOR, PKG_CALCULATOR_OLD, PKG_HWNOTEPAD, PKG_SOUNDRECORDER_OLD, NEW_HWNOTEPAD_PKG_NAME};
    private boolean mMenuClickedOnlyOnce = false;
    private boolean mMenuKeyPress = false;
    private long mMenuKeyPressTime = 0;
    boolean mNaviBarStateInited = false;
    boolean mNavibarEnabled = false;
    /* access modifiers changed from: private */
    public boolean mNeedDropFingerprintEvent = false;
    private HwNotchScreenWhiteConfig.NotchSwitchListener mNotchSwitchListener;
    OverscanTimeout mOverscanTimeout = new OverscanTimeout();
    private int mPowerKeyCount = 0;
    private boolean mPowerKeyDisTouch;
    private long mPowerKeyDisTouchTime;
    private final Runnable mPowerKeyStartWallet = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass13 */

        public void run() {
            int unused = HwPhoneWindowManager.this.mPowerWalletCount = 0;
            long unused2 = HwPhoneWindowManager.this.mLastPowerWalletDownTime = 0;
            HwPhoneWindowManager.this.powerPressBDReport(985);
            HwPhoneWindowManager.this.notifyWallet();
        }
    };
    /* access modifiers changed from: private */
    public int mPowerLongPressTimeout;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    private final Runnable mPowerOffRunner = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass31 */

        public void run() {
            HwPhoneWindowManager.this.powerOffToast();
        }
    };
    /* access modifiers changed from: private */
    public int mPowerWalletCount = 0;
    private boolean mProximitySensorEnabled = false;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass34 */

        public void onSensorChanged(SensorEvent event) {
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    final Runnable mProximitySensorTimeoutRunnable = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass23 */

        public void run() {
            Log.i(HwPhoneWindowManager.TAG, "mProximitySensorTimeout, unRegisterListener");
            HwPhoneWindowManager.this.turnOffSensorListener();
        }
    };
    private boolean mProximityTop = SystemProperties.getBoolean("ro.config.proximity_top", false);
    volatile boolean mRecentTrikeyHandled;
    private final Runnable mRemovePowerSaveWhitelistRunnable = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass28 */

        public void run() {
            try {
                if (HwPhoneWindowManager.this.checkPackageInstalled(HwPhoneWindowManager.HUAWEI_VASSISTANT_PACKAGE)) {
                    HwDeviceIdleController.removePowerSaveWhitelistApp(HwPhoneWindowManager.HUAWEI_VASSISTANT_PACKAGE);
                } else if (HwPhoneWindowManager.this.checkPackageInstalled(HwPhoneWindowManager.HUAWEI_VASSISTANT_PACKAGE_OVERSEA)) {
                    HwDeviceIdleController.removePowerSaveWhitelistApp(HwPhoneWindowManager.HUAWEI_VASSISTANT_PACKAGE_OVERSEA);
                } else {
                    Log.w(HwPhoneWindowManager.TAG, "vassistant not exists");
                }
            } catch (RemoteException e) {
                Slog.e(HwPhoneWindowManager.TAG, "remove hwvassistant exception!");
            }
        }
    };
    /* access modifiers changed from: private */
    public ContentResolver mResolver;
    private long mScreenOffTime = 0;
    private ScreenOnExListener mScreenOnExListener;
    /* access modifiers changed from: private */
    public final Object mScreenOnExLock = new Object();
    private boolean mScreenOnForFalseTouch;
    private long mScreenRecorderPowerKeyTime;
    private boolean mScreenRecorderPowerKeyTriggered;
    private final Runnable mScreenRecorderRunnable = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass12 */

        public void run() {
            Intent intent = new Intent();
            intent.setAction(HwPhoneWindowManager.HUAWEI_SCREENRECORDER_ACTION);
            intent.setClassName(HwPhoneWindowManager.HUAWEI_SCREENRECORDER_PACKAGE, HwPhoneWindowManager.HUAWEI_SCREENRECORDER_START_MODE);
            HwPhoneWindowManager.this.powerPressBDReport(987);
            try {
                HwPhoneWindowManager.this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
            } catch (Exception e) {
                Slog.e(HwPhoneWindowManager.TAG, "unable to start screenrecorder service: " + intent, e);
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
    /* access modifiers changed from: private */
    public boolean mSensorRegisted = false;
    final Object mServiceAquireLock = new Object();
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private SettingsObserver mSettingsObserver;
    private BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                Slog.i(HwPhoneWindowManager.TAG, "onReceive action: " + action);
                if (HwPhoneWindowManager.ACTION_ACTURAL_SHUTDOWN.equals(action)) {
                    boolean unused = HwPhoneWindowManager.this.mIsActuralShutDown = true;
                }
            }
        }
    };
    private HwSideStatusManager mSideStatusManager = null;
    private IHwSideTouchCallback mSideTouchHandConfigCallback = null;
    /* access modifiers changed from: private */
    public HwSideTouchManager mSideTouchManager = null;
    private HwSideVibrationManager mSideVibrationManager = null;
    boolean mStatuBarObsecured;
    IStatusBarService mStatusBarService;
    private StylusGestureListener mStylusGestureListener = null;
    private StylusGestureListener mStylusGestureListener4PCMode = null;
    /* access modifiers changed from: private */
    public SubScreenViewEntry mSubScreenViewEntry;
    private WindowManagerPolicyConstants.PointerEventListener mSwingMotionPointerEventListener = new WindowManagerPolicyConstants.PointerEventListener() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass25 */

        public void onPointerEvent(MotionEvent motionEvent) {
            IHwSwingService hwSwing = HwSwingManager.getService();
            if (hwSwing != null) {
                try {
                    int action = motionEvent.getAction();
                    if (action == 0) {
                        hwSwing.notifyFingersTouching(true);
                    } else if (action == 1 || action == 3) {
                        hwSwing.notifyFingersTouching(false);
                    }
                } catch (RemoteException e) {
                    Log.e(HwPhoneWindowManager.TAG, "notifyFingersTouching error : " + e.getMessage());
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mSystraceLogCompleted = true;
    private long mSystraceLogFingerPrintTime = 0;
    private boolean mSystraceLogPowerKeyTriggered = false;
    private final Runnable mSystraceLogRunnable = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass11 */

        public void run() {
            HandlerThread unused = HwPhoneWindowManager.this.systraceLogDialogThread = new HandlerThread("SystraceLogDialog");
            HwPhoneWindowManager.this.systraceLogDialogThread.start();
            HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
            Handler unused2 = hwPhoneWindowManager.systraceLogDialogHandler = new Handler(hwPhoneWindowManager.systraceLogDialogThread.getLooper()) {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass11.AnonymousClass1 */

                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == 0) {
                        AlertDialog unused = HwPhoneWindowManager.this.alertDialog = new AlertDialog.Builder(HwPhoneWindowManager.this.mContext).setTitle(17039380).setMessage(HwPhoneWindowManager.this.mContext.getResources().getQuantityString(34406405, 10, 10)).setCancelable(false).create();
                        HwPhoneWindowManager.this.alertDialog.getWindow().setType(2003);
                        HwPhoneWindowManager.this.alertDialog.getWindow().setFlags(128, 128);
                        HwPhoneWindowManager.this.alertDialog.show();
                    } else if (msg.what <= 0 || msg.what >= 10) {
                        HwPhoneWindowManager.this.alertDialog.dismiss();
                    } else {
                        HwPhoneWindowManager.this.alertDialog.setMessage(HwPhoneWindowManager.this.mContext.getResources().getQuantityString(34406405, 10 - msg.what, Integer.valueOf(10 - msg.what)));
                        TelecomManager telecomManager = (TelecomManager) HwPhoneWindowManager.this.mContext.getSystemService("telecom");
                        if (telecomManager != null && telecomManager.isRinging()) {
                            HwPhoneWindowManager.this.alertDialog.dismiss();
                        }
                    }
                }
            };
            HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(0);
            new Thread(new Runnable() {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass11.AnonymousClass2 */

                public void run() {
                    int i = 1;
                    while (!HwPhoneWindowManager.this.mSystraceLogCompleted && i < 11) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.w(HwPhoneWindowManager.TAG, "systrace log not completed,interrupted");
                        }
                        if (HwPhoneWindowManager.this.systraceLogDialogHandler != null && !HwPhoneWindowManager.this.mSystraceLogCompleted) {
                            HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(i);
                        }
                        i++;
                    }
                }
            }).start();
            new Thread(new Runnable() {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass11.AnonymousClass3 */

                public void run() {
                    StringBuilder sb;
                    IBinder sJankService = ServiceManager.getService("jank");
                    if (sJankService != null) {
                        try {
                            Log.d(HwPhoneWindowManager.TAG, "sJankService is not null");
                            Parcel data = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            data.writeInterfaceToken("android.os.IJankManager");
                            sJankService.transact(2, data, reply, 0);
                            int result = reply.readInt();
                            Log.d(HwPhoneWindowManager.TAG, "sJankService.transact result = " + result);
                        } catch (RemoteException e) {
                            Log.e(HwPhoneWindowManager.TAG, "sJankService.transact remote exception:" + e.getMessage());
                        } catch (Throwable th) {
                            HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(10);
                            throw th;
                        }
                    }
                    HwPhoneWindowManager.this.systraceLogDialogHandler.sendEmptyMessage(10);
                    try {
                        Thread.sleep(1000);
                        boolean unused = HwPhoneWindowManager.this.mSystraceLogCompleted = true;
                        HwPhoneWindowManager.this.systraceLogDialogThread.quitSafely();
                        sb = new StringBuilder();
                    } catch (InterruptedException e2) {
                        Log.w(HwPhoneWindowManager.TAG, "sJankService transact not completed,interrupted");
                        boolean unused2 = HwPhoneWindowManager.this.mSystraceLogCompleted = true;
                        HwPhoneWindowManager.this.systraceLogDialogThread.quitSafely();
                        sb = new StringBuilder();
                    } catch (Throwable th2) {
                        boolean unused3 = HwPhoneWindowManager.this.mSystraceLogCompleted = true;
                        HwPhoneWindowManager.this.systraceLogDialogThread.quitSafely();
                        Log.d(HwPhoneWindowManager.TAG, "has quit the systraceLogDialogThread" + HwPhoneWindowManager.this.systraceLogDialogThread.getId());
                        throw th2;
                    }
                    sb.append("has quit the systraceLogDialogThread");
                    sb.append(HwPhoneWindowManager.this.systraceLogDialogThread.getId());
                    Log.d(HwPhoneWindowManager.TAG, sb.toString());
                }
            }).start();
        }
    };
    private boolean mSystraceLogVolumeDownKeyTriggered = false;
    private boolean mSystraceLogVolumeUpKeyConsumed = false;
    private long mSystraceLogVolumeUpKeyTime = 0;
    private boolean mSystraceLogVolumeUpKeyTriggered = false;
    private TouchCountPolicy mTouchCountPolicy = new TouchCountPolicy();
    private int mTpDeviceId = -1;
    /* access modifiers changed from: private */
    public ITouchscreen mTpTouchSwitch = null;
    /* access modifiers changed from: private */
    public int mTrikeyNaviMode = -1;
    private HwTvPowerManagerPolicy mTvPolicy;
    private SystemVibrator mVibrator;
    private boolean mVolumeDownKeyDisTouch;
    private final Runnable mVolumeDownLongPressed = new Runnable() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass20 */

        public void run() {
            HwPhoneWindowManager.this.cancelVolumeDownKeyPressed();
            if ((!HwPhoneWindowManager.this.mIsProximity && HwPhoneWindowManager.this.mSensorRegisted) || !HwPhoneWindowManager.this.mSensorRegisted) {
                HwPhoneWindowManager.this.notifyVassistantService("start", 2, null);
            }
            HwPhoneWindowManager.this.turnOffSensorListener();
            boolean unused = HwPhoneWindowManager.this.isVoiceRecognitionActive = true;
            long unused2 = HwPhoneWindowManager.this.mLastStartVassistantServiceTime = SystemClock.uptimeMillis();
        }
    };
    private PowerManager.WakeLock mVolumeDownWakeLock;
    private boolean mVolumeUpKeyConsumedByDisTouch;
    private boolean mVolumeUpKeyDisTouch;
    private long mVolumeUpKeyDisTouchTime;
    private BroadcastReceiver mWhitelistReceived = new BroadcastReceiver() {
        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass8 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null || context == null || intent.getAction() == null) {
                Slog.i(HwPhoneWindowManager.TAG, "intent is " + intent + "context is " + context);
                return;
            }
            if (PhoneWindowManager.IS_NOTCH_PROP && HwPhoneWindowManager.UPDATE_NOTCH_SCREEN_ACTION.equals(intent.getAction())) {
                String fileName = intent.getStringExtra("uri");
                Slog.i(HwPhoneWindowManager.TAG, "fileName:" + fileName);
                if (fileName != null) {
                    HwNotchScreenWhiteConfig.getInstance().updateWhitelistByHot(context, fileName);
                }
            }
            if (HwPhoneWindowManager.UPDATE_GESTURE_NAV_ACTION.equals(intent.getAction())) {
                String fileName2 = intent.getStringExtra("uri");
                Slog.i(HwPhoneWindowManager.TAG, "fileName:" + fileName2);
                if (fileName2 != null) {
                    HwGestureNavWhiteConfig.getInstance().updateWhitelistByHot(context, fileName2);
                }
            }
        }
    };
    private HashSet<String> needDropSmartKeyActivities = new HashSet<>();
    /* access modifiers changed from: private */
    public int showPowerOffToastTimes = 0;
    private final List<String> specialWindowPackageNames = Arrays.asList("com.android.permissioncontroller");
    /* access modifiers changed from: private */
    public Handler systraceLogDialogHandler;
    /* access modifiers changed from: private */
    public HandlerThread systraceLogDialogThread;

    /* JADX WARN: Type inference failed for: r2v13, types: [com.android.server.policy.EasyWakeUpManager, android.os.IBinder] */
    public void systemReady() {
        HwPhoneWindowManager.super.systemReady();
        this.mHandler.post(new Runnable() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass3 */

            public void run() {
                HwPhoneWindowManager.this.initQuickcall();
                HwPhoneWindowManager.this.filmStateReporter();
            }
        });
        if (IS_NOTCH_PROP) {
            this.hwNotchScreenWhiteConfig = HwNotchScreenWhiteConfig.getInstance();
            HwFullScreenDisplay.setNotchHeight(this.mContext.getResources().getDimensionPixelSize(17105443));
        }
        if (IS_SUPPORT_FOLD_SCREEN) {
            this.mFoldScreenManagerService = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
            this.mFoldScreenOnUnblocker = new FoldScreenOnUnblocker();
        }
        if (HwDisplaySizeUtil.hasSideInScreen()) {
            HwDisplaySideRegionConfig.getInstance();
        }
        Handler proximityHandler = CommonThread.getHandler();
        proximityHandler.post(new Runnable(proximityHandler) {
            /* class com.android.server.policy.$$Lambda$HwPhoneWindowManager$mScAKIwiyTz_1H89FmolqtXFyM */
            private final /* synthetic */ Handler f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwPhoneWindowManager.this.lambda$systemReady$0$HwPhoneWindowManager(this.f$1);
            }
        });
        if (mIsHwEasyWakeup && this.mSystemReady) {
            ?? instance = EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate);
            ServiceManager.addService("easywakeup", (IBinder) instance);
            instance.saveTouchPointNodePath();
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
            IntentFilter filter = new IntentFilter();
            filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
            this.mContext.registerReceiver(new ScreenBroadcastReceiver(), filter);
        }
        this.mFalseTouchMonitor = HwFalseTouchMonitor.getInstance();
        this.mGameDockGesture = (HwGameDockGesture) LocalServices.getService(HwGameDockGesture.class);
        this.mGameDockGesture.systemReadyAndInit(this.mWindowManagerFuncs, this.mGestureNavPolicy);
        if (mSupportGameAssist) {
            this.mFingerPrintId = SystemProperties.getInt("sys.fingerprint.deviceId", -2);
            ActivityManagerEx.registerGameObserver(new IGameObserver.Stub() {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass4 */

                public void onGameStatusChanged(String packageName, int event) {
                    Log.i(HwPhoneWindowManager.TAG, "currentFgApp=" + packageName + ", mLastFgPackageName=" + HwPhoneWindowManager.this.mLastFgPackageName);
                    if (packageName != null && !packageName.equals(HwPhoneWindowManager.this.mLastFgPackageName)) {
                        boolean unused = HwPhoneWindowManager.this.mEnableKeyInCurrentFgGameApp = false;
                        if (ActivityManagerEx.isGameDndOn()) {
                            HwPhoneWindowManager.this.registerBuoyListener();
                            if (HwPhoneWindowManager.this.behaviorCollector != null) {
                                HwPhoneWindowManager.this.behaviorCollector.notifyEvent(1);
                            }
                        } else {
                            HwPhoneWindowManager.this.unRegisterBuoyListener();
                            if (HwPhoneWindowManager.this.behaviorCollector != null) {
                                HwPhoneWindowManager.this.behaviorCollector.notifyEvent(2);
                            }
                        }
                    }
                    String unused2 = HwPhoneWindowManager.this.mLastFgPackageName = packageName;
                }

                public void onGameListChanged() {
                }
            });
        }
        IGameObserver.Stub gameObserver = this.mGestureNavPolicy;
        if (gameObserver != null) {
            gameObserver.systemReady();
        }
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.init();
        }
        PickUpWakeScreenManager.getInstance().initIfNeed(this.mContext, this.mHandler, this.mWindowManagerFuncs, this.mKeyguardDelegate);
        this.mTpDeviceId = getInputDeviceId(MSG_TRIKEY_RECENT_LONG_PRESS);
        Message msg = this.mHandlerEx.obtainMessage(MSG_NOTIFY_FINGER_OPTICAL);
        msg.setAsynchronous(true);
        this.mHandlerEx.sendMessage(msg);
        mIsSidePowerFpComb = getPowerFpType();
        if (ActivityManagerEx.isGameDndOn()) {
            registerBuoyListener();
        }
        HwAccessibilityWaterMark hwAccessibilityWaterMark = this.mAccessbilityWaterMark;
        if (hwAccessibilityWaterMark != null) {
            hwAccessibilityWaterMark.systemReady();
        }
        HwGameSpaceToggleManager hwGameSpaceToggleManager = this.mHwGameSpaceToggleManager;
        if (hwGameSpaceToggleManager != null) {
            hwGameSpaceToggleManager.init();
        }
        this.mTvPolicy = (HwTvPowerManagerPolicy) LocalServices.getService(HwTvPowerManagerPolicy.class);
    }

    public /* synthetic */ void lambda$systemReady$0$HwPhoneWindowManager(Handler proximityHandler) {
        this.mHwScreenOnProximityLock = new HwScreenOnProximityLock(this.mContext, this, this.mWindowManagerFuncs, proximityHandler);
    }

    private boolean getPowerFpType() {
        String[] fpType = SystemProperties.get("ro.config.hw_fp_type").split(",");
        if (fpType.length != 4) {
            return false;
        }
        int type = -1;
        try {
            type = Integer.parseInt(fpType[0]);
        } catch (NumberFormatException e) {
            Log.e(TAG, "getPowerFpType NumberFormatException");
        }
        if (type != 3) {
            return false;
        }
        return true;
    }

    public HwScreenOnProximityLock getScreenOnProximity() {
        return this.mHwScreenOnProximityLock;
    }

    public void addPointerEvent(MotionEvent motionEvent) {
        getDefaultDisplayPolicy().getHwDisplayPolicyEx().addPointerEvent(motionEvent);
    }

    public void init(Context context, IWindowManager windowManager, WindowManagerFuncs windowManagerFuncs) {
        this.mHandlerEx = new PolicyHandlerEx();
        this.navibar_enable = "enable_navbar";
        this.mCurUser = ActivityManager.getCurrentUser();
        this.mResolver = context.getContentResolver();
        this.mFingerprintObserver = new ContentObserver(this.mHandler) {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass5 */

            public void onChange(boolean selfChange) {
                HwPhoneWindowManager.this.updateFingerprintNav();
            }
        };
        registerFingerprintObserver(this.mCurUser);
        updateFingerprintNav();
        this.mHwOUCObserver = new ContentObserver(this.mHandler) {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass6 */

            public void onChange(boolean selfChange) {
                HwPhoneWindowManager.this.updateHwOUCKeyguardViewState();
            }
        };
        registerHwOUCObserver(this.mCurUser);
        updateHwOUCKeyguardViewState();
        initDropSmartKey();
        HwPhoneWindowManager.super.init(context, windowManager, windowManagerFuncs);
        context.registerReceiver(this.mShutdownReceiver, new IntentFilter(ACTION_ACTURAL_SHUTDOWN), HUAWEI_SHUTDOWN_PERMISSION, null);
        this.mGestureNavPolicy = (GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class);
        this.mAccessbilityWaterMark = new HwAccessibilityWaterMark(this.mContext);
        if (HwFoldScreenState.isFoldScreenDevice()) {
            this.mSubScreenViewEntry = new SubScreenViewEntry(context);
        }
        this.mKeyguardDelegate.setHwPCKeyguardShowingCallback(new KeyguardStateMonitor.HwPCKeyguardShowingCallback() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass7 */

            public void onShowingChanged(boolean showing) {
                if (HwPCUtils.isPcCastModeInServer() || HwPCUtils.isInWindowsCastMode()) {
                    HwPhoneWindowManager.this.lockScreen(showing);
                }
                if (HwPhoneWindowManager.this.mHwScreenOnProximityLock != null && !showing) {
                    HwPhoneWindowManager.this.mHwScreenOnProximityLock.releaseLock(3);
                }
                if (HwPhoneWindowManager.this.mAccessbilityWaterMark != null) {
                    HwPhoneWindowManager.this.mAccessbilityWaterMark.isKeyGuardShowing(showing);
                }
                Slog.i(HwPhoneWindowManager.TAG, "onShowingChangedL: " + showing);
                if (HwPhoneWindowManager.this.mSubScreenViewEntry != null) {
                    HwPhoneWindowManager.this.mSubScreenViewEntry.handleLockScreenShowChanged(showing);
                }
            }
        });
        registerNotchListener();
        registerReceivers(context);
        IZrHung iZrHung = this.mAppEyeBackKey;
        if (iZrHung != null) {
            iZrHung.init((ZrHungData) null);
        }
        IZrHung iZrHung2 = this.mAppEyeHomeKey;
        if (iZrHung2 != null) {
            iZrHung2.init((ZrHungData) null);
        }
        initTpKeepParamters();
        HwExtDisplaySizeUtil displaySizeUtil = HwExtDisplaySizeUtil.getInstance();
        if (displaySizeUtil != null) {
            this.mIsSupportSideScreen = displaySizeUtil.hasSideInScreen();
        }
        loadFingerSenseManager();
        this.mSideTouchManager = HwSideTouchManager.getInstance(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (tm != null) {
            tm.listen(mPhoneListener, 32);
        }
        this.mHwGameSpaceToggleManager = HwGameSpaceToggleManager.getInstance(this.mContext);
    }

    private void registerReceivers(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_NOTCH_SCREEN_ACTION);
        filter.addAction(UPDATE_GESTURE_NAV_ACTION);
        context.registerReceiverAsUser(this.mWhitelistReceived, UserHandle.ALL, filter, "com.huawei.systemmanager.permission.ACCESS_INTERFACE", null);
    }

    private void registerNotchListener() {
        if (IS_NOTCH_PROP) {
            updateNotchSwitchStatus(true);
            this.mNotchSwitchListener = new HwNotchScreenWhiteConfig.NotchSwitchListener() {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass9 */

                @Override // com.android.server.notch.HwNotchScreenWhiteConfig.NotchSwitchListener
                public void onChange() {
                    HwPhoneWindowManager.this.updateNotchSwitchStatus(false);
                }
            };
            HwNotchScreenWhiteConfig.registerNotchSwitchListener(this.mContext, this.mNotchSwitchListener);
            try {
                ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                    /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass10 */

                    public void onUserSwitching(int newUserId) throws RemoteException {
                        HwPhoneWindowManager.this.updateNotchSwitchStatus(true);
                    }
                }, TAG);
            } catch (RemoteException e) {
                Log.i(TAG, "registerUserSwitchObserver fail", e);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateNotchSwitchStatus(boolean forceUpdate) {
        boolean oldStatus = this.mIsNotchSwitchOpen;
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "display_notch_status", 0, this.mCurUser) != 1) {
            z = false;
        }
        this.mIsNotchSwitchOpen = z;
        if (this.mIsNotchSwitchOpen != oldStatus || forceUpdate) {
            HwNotchScreenWhiteConfig.getInstance().setNotchSwitchStatus(this.mIsNotchSwitchOpen);
            if (IS_NOTCH_PROP && this.mGameDockGesture != null && HwGameDockGesture.isGameDockGestureFeatureOn()) {
                this.mGameDockGesture.updateOnNotchSwitchChange();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateFingerprintNav() {
        boolean z = false;
        this.isFingerShotCameraOn = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_CAMERA_SWITCH, 1, this.mCurUser) == 1;
        this.isFingerStopAlarmOn = Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_STOP_ALARM, 0, this.mCurUser) == 1;
        if (Settings.Secure.getIntForUser(this.mResolver, FINGERPRINT_ANSWER_CALL, 0, this.mCurUser) == 1) {
            z = true;
        }
        this.isFingerAnswerPhoneOn = z;
    }

    private void registerFingerprintObserver(int userId) {
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FINGERPRINT_CAMERA_SWITCH), true, this.mFingerprintObserver, userId);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FINGERPRINT_STOP_ALARM), true, this.mFingerprintObserver, userId);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(FINGERPRINT_ANSWER_CALL), true, this.mFingerprintObserver, userId);
    }

    public void setCurrentUser(int userId, int[] currentProfileIds) {
        this.mCurUser = userId;
        registerFingerprintObserver(userId);
        this.mFingerprintObserver.onChange(true);
        this.mSettingsObserver.registerContentObserver(userId);
        this.mSettingsObserver.onChange(true);
        FingerprintActionsListener fingerprintActionsListener2 = this.fingerprintActionsListener;
        if (fingerprintActionsListener2 != null) {
            fingerprintActionsListener2.setCurrentUser(userId);
        }
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy != null) {
            gestureNavPolicy.onUserChanged(userId);
        }
        HwAccessibilityWaterMark hwAccessibilityWaterMark = this.mAccessbilityWaterMark;
        if (hwAccessibilityWaterMark != null) {
            hwAccessibilityWaterMark.setCurrentUser(userId);
        }
    }

    private void registerHwOUCObserver(int userId) {
        Log.d(TAG, "register HwOUC Observer");
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(KEY_HWOUC_KEYGUARD_VIEW_ON_TOP), true, this.mHwOUCObserver, userId);
    }

    /* access modifiers changed from: private */
    public void updateHwOUCKeyguardViewState() {
        boolean z = true;
        if (1 != Settings.Secure.getIntForUser(this.mResolver, KEY_HWOUC_KEYGUARD_VIEW_ON_TOP, 0, this.mCurUser)) {
            z = false;
        }
        this.isHwOUCKeyguardViewOnTop = z;
    }

    private boolean supportActivityForbidSpecialKey(int keyCode) {
        if (!this.isHwOUCKeyguardViewOnTop) {
            return false;
        }
        if (3 == keyCode || 4 == keyCode || 187 == keyCode) {
            return true;
        }
        return false;
    }

    public int checkAddPermission(WindowManager.LayoutParams attrs, int[] outAppOp) {
        int type = attrs.type;
        if (type == TOAST_TYPE_COVER_SCREEN) {
            outAppOp[0] = -1;
            return 0;
        } else if (type == 2104) {
            return 0;
        } else {
            return HwPhoneWindowManager.super.checkAddPermission(attrs, outAppOp);
        }
    }

    public void onKeyguardOccludedChangedLw(boolean occluded) {
        HwPhoneWindowManager.super.onKeyguardOccludedChangedLw(occluded);
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.handleOccludedChanged(occluded);
        }
        if (HwPCUtils.isInWindowsCastMode()) {
            sHwWindowsCastManager.onKeyguardOccludedChangedLw(occluded);
        }
    }

    public int getLastSystemUiFlags() {
        return this.mDefaultDisplayPolicy.getLastSystemUiFlags();
    }

    public int getWindowLayerFromTypeLw(int type, boolean canAddInternalSystemWindow) {
        if (type == 2100) {
            return 33;
        }
        if (type == TOAST_TYPE_COVER_SCREEN) {
            return 34;
        }
        if (type == 2104) {
            return 35;
        }
        if (type == 2105) {
            return 34;
        }
        int ret = HwPhoneWindowManager.super.getWindowLayerFromTypeLw(type, canAddInternalSystemWindow);
        return ret >= 33 ? ret + 2 : ret;
    }

    public void freezeOrThawRotation(int rotation) {
        this.mDesiredRotation = rotation;
    }

    /* access modifiers changed from: protected */
    public void setHasAcitionBar(boolean hasActionBar) {
        this.mIsHasActionBar = hasActionBar;
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
        if (!isPhoneInCall()) {
            return true;
        }
        if (!isKeyguardSecure(this.mCurrentUserId) || why == 3 || why == 6) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isWakeKeyWhenScreenOff(int keyCode) {
        if (!mCustUsed) {
            return HwPhoneWindowManager.super.isWakeKeyWhenScreenOff(keyCode);
        }
        int i = 0;
        while (true) {
            int[] iArr = mUnableWakeKey;
            if (i >= iArr.length) {
                return true;
            }
            if (keyCode == iArr[i]) {
                return false;
            }
            i++;
        }
    }

    public boolean isWakeKeyFun(int keyCode) {
        if (!mCustBeInit) {
            getKeycodeFromCust();
        }
        if (!mCustUsed) {
            return false;
        }
        int i = 0;
        while (true) {
            int[] iArr = mUnableWakeKey;
            if (i >= iArr.length) {
                return true;
            }
            if (keyCode == iArr[i]) {
                return false;
            }
            i++;
        }
    }

    private void getKeycodeFromCust() {
        String[] unableWakeKeyArray;
        String unableCustomizedWakeKey = null;
        try {
            unableCustomizedWakeKey = SettingsEx.Systemex.getString(this.mContext.getContentResolver(), "unable_wake_up_key");
        } catch (Exception e) {
            Log.e(TAG, "Exception when got name value", e);
        }
        if (!(unableCustomizedWakeKey == null || (unableWakeKeyArray = unableCustomizedWakeKey.split(";")) == null || unableWakeKeyArray.length == 0)) {
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
        mCustBeInit = true;
    }

    public int interceptMotionBeforeQueueingNonInteractive(int displayId, long whenNanos, int policyFlags) {
        if (isTvMode()) {
            return 0;
        }
        if ((FLOATING_MASK & policyFlags) == 0) {
            return HwPhoneWindowManager.super.interceptMotionBeforeQueueingNonInteractive(displayId, whenNanos, policyFlags);
        }
        Slog.i(TAG, "interceptMotionBeforeQueueingNonInteractive policyFlags: " + policyFlags);
        Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
        return 0;
    }

    /* access modifiers changed from: protected */
    public int getSingleHandState() {
        IBinder windowManagerBinder = WindowManagerGlobal.getWindowManagerService().asBinder();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        if (windowManagerBinder != null) {
            try {
                data.writeInterfaceToken("android.view.IWindowManager");
                windowManagerBinder.transact(1990, data, reply, 0);
                reply.readException();
                return reply.readInt();
            } catch (RemoteException e) {
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

    /* access modifiers changed from: protected */
    public void unlockScreenPinningTest() {
        IBinder statusBarServiceBinder;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (!(getHWStatusBarService() == null || (statusBarServiceBinder = getHWStatusBarService().asBinder()) == null)) {
                Log.d(TAG, "Transact unlockScreenPinningTest to status bar service!");
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                statusBarServiceBinder.transact(111, data, reply, 0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transactToStatusBarService->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
        reply.recycle();
        data.recycle();
    }

    public void finishedGoingToSleep(int why) {
        this.mHandler.removeCallbacks(this.mOverscanTimeout);
        this.mHandler.postDelayed(this.mOverscanTimeout, 200);
        HwPhoneWindowManager.super.finishedGoingToSleep(why);
    }

    class OverscanTimeout implements Runnable {
        OverscanTimeout() {
        }

        public void run() {
            Slog.i(HwPhoneWindowManager.TAG, "OverscanTimeout run");
            Settings.Global.putString(HwPhoneWindowManager.this.mContext.getContentResolver(), "single_hand_mode", "");
        }
    }

    private void interceptSystraceLog() {
        long now = SystemClock.uptimeMillis();
        Log.d(TAG, "now=" + now + " mSystraceLogVolumeUpKeyTime=" + this.mSystraceLogVolumeUpKeyTime + " mSystraceLogFingerPrintTime=" + this.mSystraceLogFingerPrintTime);
        if (now <= this.mSystraceLogVolumeUpKeyTime + 150 && now <= this.mSystraceLogFingerPrintTime + SYSTRACELOG_FINGERPRINT_EFFECT_DELAY && this.mSystraceLogCompleted) {
            this.mSystraceLogCompleted = false;
            this.mSystraceLogVolumeUpKeyConsumed = true;
            this.mSystraceLogFingerPrintTime = 0;
            this.mSystraceLogVolumeUpKeyTriggered = false;
            this.mScreenRecorderVolumeUpKeyTriggered = false;
            Trace.traceBegin(8, "invoke_systrace_log_dump");
            Trace.traceEnd(8);
            Log.d(TAG, "Systrace triggered");
            this.mHandler.postDelayed(this.mSystraceLogRunnable, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
        }
    }

    /* access modifiers changed from: private */
    public void powerPressBDReport(int eventId) {
        if (Log.HWINFO) {
            Flog.bdReport(this.mContext, eventId);
        }
    }

    private void interceptScreenRecorder() {
        if (isTvMode()) {
            Slog.d(TAG, "ScreenRecorder not support on TV");
        } else if (this.mScreenRecorderVolumeUpKeyTriggered && this.mScreenRecorderPowerKeyTriggered && !this.mScreenRecorderVolumeDownKeyTriggered && !SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false) && !keyguardIsShowingTq() && checkPackageInstalled(HUAWEI_SCREENRECORDER_PACKAGE)) {
            HwCustPhoneWindowManager hwCustPhoneWindowManager = this.mCust;
            if (hwCustPhoneWindowManager != null && !hwCustPhoneWindowManager.isSosAllowed()) {
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

    /* access modifiers changed from: package-private */
    public boolean isVoiceCall() {
        IAudioService audioService = getAudioService();
        if (audioService != null) {
            try {
                int mode = audioService.getMode();
                if (mode == 3 || mode == 2) {
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                Log.w(TAG, "getMode exception");
            }
        }
        return false;
    }

    private void sendKeyEvent(int keycode) {
        int[] actions;
        for (int i : new int[]{0, 1}) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, i, keycode, 0, 0, -1, 0, 8, 257), 0);
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
            boolean z = true;
            boolean unused = HwPhoneWindowManager.this.mDeviceProvisioned = Settings.Secure.getIntForUser(HwPhoneWindowManager.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            int unused2 = HwPhoneWindowManager.this.mTrikeyNaviMode = Settings.System.getIntForUser(HwPhoneWindowManager.this.mResolver, "swap_key_position", HwPhoneWindowManager.this.TRIKEY_NAVI_DEFAULT_MODE, ActivityManager.getCurrentUser());
            int unused3 = HwPhoneWindowManager.this.mButtonLightMode = Settings.System.getIntForUser(HwPhoneWindowManager.this.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
            boolean unused4 = HwPhoneWindowManager.this.mHapticEnabled = Settings.System.getIntForUser(HwPhoneWindowManager.this.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) != 0;
            String unused5 = HwPhoneWindowManager.this.mHiVoiceKeyType = Settings.Secure.getStringForUser(HwPhoneWindowManager.this.mResolver, HwPhoneWindowManager.KEY_HIVOICE_PRESS_TYPE, ActivityManager.getCurrentUser());
            boolean unused6 = HwPhoneWindowManager.this.mDoubleTapPay = Settings.Secure.getIntForUser(HwPhoneWindowManager.this.mResolver, HwPhoneWindowManager.KEY_DOUBLE_TAP_PAY, 0, ActivityManager.getCurrentUser()) != 1 ? false : z;
            int unused7 = HwPhoneWindowManager.this.mPowerLongPressTimeout = Settings.Secure.getIntForUser(HwPhoneWindowManager.this.mResolver, HwPhoneWindowManager.KEY_POWER_LONGPRESS_TIMEOUT, 700, ActivityManager.getCurrentUser());
            int unused8 = HwPhoneWindowManager.this.showPowerOffToastTimes = Settings.Secure.getIntForUser(HwPhoneWindowManager.this.mResolver, HwPhoneWindowManager.KEY_TOAST_POWER_OFF, 0, ActivityManager.getCurrentUser());
        }

        public void registerContentObserver(int userId) {
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.System.getUriFor("swap_key_position"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.System.getUriFor("device_provisioned"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.System.getUriFor("button_light_mode"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.System.getUriFor("physic_navi_haptic_feedback_enabled"), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwPhoneWindowManager.KEY_HIVOICE_PRESS_TYPE), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwPhoneWindowManager.KEY_DOUBLE_TAP_PAY), false, this, userId);
            HwPhoneWindowManager.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(HwPhoneWindowManager.KEY_POWER_LONGPRESS_TIMEOUT), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
            boolean z = true;
            boolean unused = hwPhoneWindowManager.mDeviceProvisioned = Settings.Secure.getIntForUser(hwPhoneWindowManager.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            HwPhoneWindowManager hwPhoneWindowManager2 = HwPhoneWindowManager.this;
            int unused2 = hwPhoneWindowManager2.mTrikeyNaviMode = Settings.System.getIntForUser(hwPhoneWindowManager2.mResolver, "swap_key_position", HwPhoneWindowManager.this.TRIKEY_NAVI_DEFAULT_MODE, ActivityManager.getCurrentUser());
            HwPhoneWindowManager hwPhoneWindowManager3 = HwPhoneWindowManager.this;
            int unused3 = hwPhoneWindowManager3.mButtonLightMode = Settings.System.getIntForUser(hwPhoneWindowManager3.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
            HwPhoneWindowManager.this.resetButtonLightStatus();
            Slog.i(HwPhoneWindowManager.TAG, "mTrikeyNaviMode is:" + HwPhoneWindowManager.this.mTrikeyNaviMode + " mButtonLightMode is:" + HwPhoneWindowManager.this.mButtonLightMode);
            HwPhoneWindowManager hwPhoneWindowManager4 = HwPhoneWindowManager.this;
            boolean unused4 = hwPhoneWindowManager4.mHapticEnabled = Settings.System.getIntForUser(hwPhoneWindowManager4.mResolver, "physic_navi_haptic_feedback_enabled", 1, ActivityManager.getCurrentUser()) != 0;
            HwPhoneWindowManager hwPhoneWindowManager5 = HwPhoneWindowManager.this;
            String unused5 = hwPhoneWindowManager5.mHiVoiceKeyType = Settings.Secure.getStringForUser(hwPhoneWindowManager5.mResolver, HwPhoneWindowManager.KEY_HIVOICE_PRESS_TYPE, ActivityManager.getCurrentUser());
            HwPhoneWindowManager hwPhoneWindowManager6 = HwPhoneWindowManager.this;
            int unused6 = hwPhoneWindowManager6.mPowerLongPressTimeout = Settings.Secure.getIntForUser(hwPhoneWindowManager6.mResolver, HwPhoneWindowManager.KEY_POWER_LONGPRESS_TIMEOUT, 700, ActivityManager.getCurrentUser());
            HwPhoneWindowManager hwPhoneWindowManager7 = HwPhoneWindowManager.this;
            if (Settings.Secure.getIntForUser(hwPhoneWindowManager7.mResolver, HwPhoneWindowManager.KEY_DOUBLE_TAP_PAY, 0, ActivityManager.getCurrentUser()) != 1) {
                z = false;
            }
            boolean unused7 = hwPhoneWindowManager7.mDoubleTapPay = z;
            Slog.i(HwPhoneWindowManager.TAG, "onChange mHiVoiceKeyType is:" + HwPhoneWindowManager.this.mHiVoiceKeyType + " mDoubleTapPay " + HwPhoneWindowManager.this.mDoubleTapPay + " powerLongPressTimeout " + HwPhoneWindowManager.this.mPowerLongPressTimeout);
        }
    }

    private boolean isExcluedScene() {
        ServiceManager.getService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        String pkgName = getTopActivity();
        boolean isSuperPowerMode = SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false);
        if (pkgName == null) {
            return false;
        }
        if ("com.android.deskclock/.alarmclock.LockAlarmFullActivity".equals(pkgName) || "com.huawei.deskclock/.alarmclock.LockAlarmFullActivity".equals(pkgName) || isSuperPowerMode || !this.mDeviceProvisioned || keyguardOn()) {
            return true;
        }
        return false;
    }

    private boolean isExcluedBackScene() {
        if (this.mTrikeyNaviMode == 1) {
            return isExcluedScene();
        }
        return !this.mDeviceProvisioned;
    }

    private boolean isExcluedRecentScene() {
        if (this.mTrikeyNaviMode == 1) {
            return !this.mDeviceProvisioned;
        }
        return isExcluedScene();
    }

    /* access modifiers changed from: private */
    public void resetButtonLightStatus() {
        if (this.mButtonLight == null) {
            return;
        }
        if (!this.mDeviceProvisioned) {
            setButtonLightTimeout(false);
            this.mButtonLight.setBrightness(0);
            return;
        }
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
    }

    /* access modifiers changed from: private */
    public void setButtonLightTimeout(boolean timeout) {
        SystemProperties.set("sys.button.light.timeout", String.valueOf(timeout));
    }

    /* access modifiers changed from: private */
    public void sendLightTimeoutMsg() {
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

    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        private ScreenBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON.equals(intent.getAction())) {
                HwPhoneWindowManager.this.sendLightTimeoutMsg();
            }
        }
    }

    /* access modifiers changed from: private */
    public void startHwVibrate(int vibrateMode) {
        if (!isKeyguardLocked() && this.mHapticEnabled && !"true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false")) && this.mVibrator != null) {
            Log.d(TAG, "startVibrateWithConfigProp:" + vibrateMode);
            this.mVibrator.vibrate((long) vibrateMode);
        }
    }

    private boolean isMMITesting() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    private void cancelPowerKeyStartWallet() {
        this.mHandler.removeCallbacks(this.mPowerKeyStartWallet);
    }

    /* access modifiers changed from: private */
    public void notifyWallet() {
        Intent intent = new Intent(POWERKEY_QUICKPAY_ACTION);
        intent.setPackage(POWERKEY_QUICKPAY_PACKAGE);
        intent.putExtra("channel", "doubleClickPowerBtn");
        try {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Log.e(TAG, "start wallet server err");
        }
        Log.i(TAG, "notifyWallet");
    }

    private boolean isNeedNotifyWallet() {
        if (!mSupportDoubleTapPay) {
            return false;
        }
        if (mIsNeedMuteByPowerKeyDown && isNeedMuteFirstPowerKeyDown()) {
            return false;
        }
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            if (HWFLOW) {
                Log.i(TAG, "PC mode");
            }
            return false;
        } else if (mIsSidePowerFpComb && this.mHwPWMEx.isPowerFpForbidGotoSleep()) {
            if (HWFLOW) {
                Log.d(TAG, "wallet Is Side powerFp comb");
            }
            return false;
        } else if (this.mDoubleTapPay) {
            return true;
        } else {
            if (HWFLOW) {
                Log.d(TAG, "DoubleTapPay switch is not open: ");
            }
            return false;
        }
    }

    private boolean isNeedMuteFirstPowerKeyDown() {
        WindowState focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow();
        if (isIncallui(focusedWindow)) {
            return true;
        }
        if (focusedWindow == null || focusedWindow.getAttrs() == null || !String.valueOf(focusedWindow.getAttrs().getTitle()).contains(PROXIMITY_UI_WINDOW_TITLE)) {
            return false;
        }
        return isIncallui(this.mDefaultDisplayPolicy.getLastFocusedWindow());
    }

    private boolean isIncallui(WindowState focusedWindow) {
        if (focusedWindow == null || focusedWindow.getAttrs() == null) {
            return false;
        }
        return String.valueOf(focusedWindow.getAttrs().getTitle()).contains(HUAWEI_ANDROID_INCALL_UI);
    }

    private void startWallet(int eventAction) {
        if (isNeedNotifyWallet()) {
            long now = SystemClock.uptimeMillis();
            boolean down = eventAction == 0;
            if (HWFLOW) {
                Log.i(TAG, "Down " + down + " eventAction " + eventAction);
            }
            if (down) {
                Log.i(TAG, "mPowerWalletCount " + this.mPowerWalletCount + " now " + now + " last " + this.mLastPowerWalletDownTime);
                if (this.mPowerWalletCount <= 0 || now - this.mLastPowerWalletDownTime <= 500) {
                    this.mPowerWalletCount++;
                    this.mLastPowerWalletDownTime = now;
                    int i = this.mPowerWalletCount;
                    if (i == 2) {
                        cancelAIPowerLongPressed();
                        this.mHandler.postDelayed(this.mPowerKeyStartWallet, 500);
                    } else if (i > 2) {
                        cancelPowerKeyStartWallet();
                    }
                } else {
                    this.mPowerWalletCount = 1;
                    this.mLastPowerWalletDownTime = now;
                }
            }
        }
    }

    private void wakeupSOSPage(int keycode) {
        if (isTvMode()) {
            Slog.d(TAG, "SOS not support on TV");
        } else if (keycode == 25 || keycode == 24) {
            this.mLastVolumeKeyDownTime = SystemClock.uptimeMillis();
        } else if (keycode == 26) {
            long now = SystemClock.uptimeMillis();
            if (this.mPowerKeyCount <= 0 || (now - this.mLastPowerKeyDownTime <= 500 && now - this.mLastVolumeKeyDownTime >= POWER_SOS_MISTOUCH_THRESHOLD)) {
                if (now - this.mLastNotifyWalletTime < 500) {
                    this.mHwPWMEx.cancelWalletSwipe(this.mHandler);
                }
                this.mPowerKeyCount++;
                this.mLastPowerKeyDownTime = now;
                if (this.mPowerKeyCount == MAX_POWERKEY_COUNTDOWN) {
                    resetSOS();
                    this.mIsFreezePowerkey = false;
                    PowerManager powerManager = (PowerManager) this.mContext.getSystemService(HIVOICE_PRESS_TYPE_POWER);
                    powerPressBDReport(986);
                    try {
                        String pkgName = getTopActivity();
                        Log.d(TAG, "get Emergency power TopActivity is:" + pkgName);
                        if (!"com.android.emergency/.view.ViewCountDownActivity".equals(pkgName) && !"com.android.emergency/.view.EmergencyNumberActivity".equals(pkgName)) {
                            if (!ACTIVITY_NAME_EMERGENCY_SIMPLIFIEDINFO.equals(pkgName)) {
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
                                    powerManager.wakeUp(SystemClock.uptimeMillis());
                                    this.mHandlerEx.removeMessages(MSG_FREEZE_POWER_KEY);
                                    Message msg = this.mHandlerEx.obtainMessage(MSG_FREEZE_POWER_KEY);
                                    msg.setAsynchronous(true);
                                    this.mHandlerEx.sendMessageDelayed(msg, 1000);
                                    return;
                                }
                                return;
                            }
                        }
                        Log.d(TAG, "current topActivity is emergency, return ");
                        if (powerManager != null && !powerManager.isScreenOn()) {
                            powerManager.wakeUp(SystemClock.uptimeMillis());
                        }
                    } catch (ActivityNotFoundException ex) {
                        Log.e(TAG, "ActivityNotFoundException failed message : " + ex);
                        this.mIsFreezePowerkey = false;
                    } catch (Exception e) {
                        Log.e(TAG, "StartActivity Exception");
                        this.mIsFreezePowerkey = false;
                    }
                }
            } else {
                this.mPowerKeyCount = 1;
                this.mLastPowerKeyDownTime = now;
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

    private boolean isSideTouchVolumeKey(KeyEvent event) {
        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        if (device == null) {
            return true;
        }
        if (device.isExternal() || device.isVirtual()) {
            return false;
        }
        return true;
    }

    private boolean preventVolumeKeyForSideScreen(KeyEvent event) {
        WindowState focusedWindow;
        if (HwPCUtils.isPcCastModeInServer() || !isSideTouchVolumeKey(event) || this.mDefaultDisplayPolicy == null || (focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow()) == null) {
            return false;
        }
        String winTitle = String.valueOf(focusedWindow.getAttrs().getTitle());
        if (TextUtils.isEmpty(winTitle) || !mSceneActivitys.contains(winTitle)) {
            return false;
        }
        Slog.i(TAG, "preventVolumeKeyForSideScreen case prevent wintitle :" + winTitle);
        return true;
    }

    private void lockOnPadAssistMode(int keyCode) {
        if (keyCode == 26 && HwPCUtils.isPadAssistantMode()) {
            boolean isScreenPowerOn = false;
            try {
                IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
                if (pcMgr != null && !pcMgr.isScreenPowerOn()) {
                    isScreenPowerOn = true;
                }
            } catch (RemoteException e) {
                isScreenPowerOn = false;
            }
            if (isScreenPowerOn) {
                lockNow(null);
            }
        }
    }

    /* JADX INFO: Multiple debug info for r6v8 boolean: [D('handled' boolean), D('isInjected' boolean)] */
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        int result;
        boolean down;
        boolean isScreenOn;
        int keyCode;
        KeyEvent keyEvent;
        boolean isInjected;
        boolean isInjected2;
        int keyCode2;
        HwCustPhoneWindowManager hwCustPhoneWindowManager;
        boolean isInjected3;
        boolean isPhoneActive;
        HwCustPhoneWindowManager hwCustPhoneWindowManager2;
        boolean isCameraOpened;
        boolean z;
        HwTvPowerManagerPolicy hwTvPowerManagerPolicy;
        boolean down2 = event.getAction() == 0;
        int keyCode3 = event.getKeyCode();
        int flags = event.getFlags();
        int deviceID = event.getDeviceId();
        boolean initialDown = down2 && event.getRepeatCount() == 0;
        boolean isScreenOn2 = (policyFlags & 536870912) != 0;
        if (isTvMode() && isInterceptKeyBeforeQueueingTv(event, isScreenOn2)) {
            return 0;
        }
        if (supportActivityForbidSpecialKey(keyCode3)) {
            Log.d(TAG, "isScreenOn = " + isScreenOn2 + ", has intercept Key for block " + keyCode3 + ", some  ssssuper activity is on top now.");
            return 0;
        }
        if (down2) {
            if (keyCode3 == 4 && this.mAppEyeBackKey != null) {
                ZrHungData arg = new ZrHungData();
                arg.putLong("downTime", event.getDownTime());
                this.mAppEyeBackKey.check(arg);
            }
            if (keyCode3 == 3 && this.mAppEyeHomeKey != null) {
                ZrHungData arg2 = new ZrHungData();
                arg2.putLong("downTime", event.getDownTime());
                this.mAppEyeHomeKey.check(arg2);
            }
        }
        if (!this.mSystraceLogCompleted) {
            Log.d(TAG, " has intercept Key for block : " + keyCode3 + ", isdown : " + down2 + ", flags : " + flags);
            return 0;
        } else if (handleInputEventInPCCastMode(event)) {
            return 0;
        } else {
            HwCustPhoneWindowManager hwCustPhoneWindowManager3 = this.mCust;
            if (hwCustPhoneWindowManager3 != null) {
                hwCustPhoneWindowManager3.processCustInterceptKey(keyCode3, down2, this.mContext);
            }
            Flog.i((int) WifiProCommonUtils.RESP_CODE_INVALID_URL, "HwPhoneWindowManager has intercept Key : " + keyCode3 + ", isdown : " + down2 + ", flags : " + flags);
            lockOnPadAssistMode(keyCode3);
            if (initialDown && (hwTvPowerManagerPolicy = this.mTvPolicy) != null) {
                hwTvPowerManagerPolicy.onKeyOperation();
            }
            reportMonitorData(keyCode3, down2);
            HwFreeFormManager.getInstance(this.mContext).removeFloatListView();
            boolean isInjected4 = (policyFlags & 16777216) != 0;
            wakeupNoteEditor(event, isInjected4);
            if (keyCode3 == 26 && this.mIsFreezePowerkey) {
                return 1;
            }
            if ((keyCode3 == 26 || keyCode3 == 6 || keyCode3 == 187 || keyCode3 == 715) && this.mDefaultDisplayPolicy.getFocusedWindow() != null && (this.mDefaultDisplayPolicy.getFocusedWindow().getAttrs().hwFlags & FLOATING_MASK) == FLOATING_MASK) {
                Log.i(TAG, "power and endcall key received and passsing to user.");
                return 1;
            }
            if (mIsHwEasyWakeup && this.mSystemReady) {
                if (EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate).handleWakeUpKey(event, isScreenOn2 ? -1 : this.mScreenOffReason)) {
                    Log.i(TAG, "EasyWakeUpManager has handled the keycode : " + event.getKeyCode());
                    return 0;
                }
            }
            if (initialDown) {
                if (((keyCode3 == 82 && (268435456 & flags) == 0) || keyCode3 == 3 || keyCode3 == 4) && SystemProperties.get(VIBRATE_ON_TOUCH, "false").equals("true") && (policyFlags & 2) != 0) {
                    performHapticFeedbackLw(null, 1, false, "Huawei  --- TBD");
                }
            }
            boolean isWakeKey = isWakeKeyFun(keyCode3) | ((policyFlags & 1) != 0);
            if ((!isScreenOn2 || this.mHeadless) && (!isInjected4 || isWakeKey)) {
                if (down2 && isWakeKey) {
                    isWakeKeyWhenScreenOff(keyCode3);
                }
                result = 0;
            } else {
                result = 1;
            }
            if ((keyCode3 == 25 || keyCode3 == 24) && this.mDefaultDisplayPolicy.getFocusedWindow() != null && (this.mDefaultDisplayPolicy.getFocusedWindow().getAttrs().hwFlags & 8) == 8 && "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"))) {
                Log.i(TAG, "Prevent hard key volume event to mmi test before queueing.");
                return result & -2;
            } else if (lightScreenOnPcMode(keyCode3)) {
                return 0;
            } else {
                if (keyCode3 == 3 || keyCode3 == 4) {
                    isInjected = isInjected4;
                    isScreenOn = isScreenOn2;
                    keyCode = keyCode3;
                    down = down2;
                    keyEvent = event;
                } else {
                    if (keyCode3 != 82) {
                        if (keyCode3 != 164) {
                            if (keyCode3 == 187) {
                                isInjected = isInjected4;
                                isScreenOn = isScreenOn2;
                                keyCode = keyCode3;
                                down = down2;
                                keyEvent = event;
                            } else if (keyCode3 != 231) {
                                if (keyCode3 != 601) {
                                    if (keyCode3 == 702) {
                                        handleGameEvent(event);
                                    } else if (keyCode3 == 715) {
                                        handleGameSpace(event);
                                    } else if (keyCode3 != 717) {
                                        if (!(keyCode3 == 501 || keyCode3 == 502)) {
                                            if (keyCode3 != 708 && keyCode3 != 709) {
                                                switch (keyCode3) {
                                                    case 24:
                                                    case 25:
                                                        reportVolumeData(event, isInjected4, isScreenOn2);
                                                        break;
                                                    case 26:
                                                        if (!HwFrameworkFactory.getVRSystemServiceManager().isVRDeviceConnected()) {
                                                            notifyPowerKeyEventToHiAction(this.mContext, event);
                                                            if (!down2) {
                                                                powerPressBDReport(981);
                                                                this.mPowerKeyDisTouch = false;
                                                                this.mScreenRecorderPowerKeyTriggered = false;
                                                                cancelPendingScreenRecorderAction();
                                                                this.mSystraceLogPowerKeyTriggered = false;
                                                                cancelPowerOffToast();
                                                                break;
                                                            } else {
                                                                setIsNeedNotifyWallet(isNeedNotifyWallet());
                                                                powerPressBDReport(980);
                                                                PowerManager powerManager = this.mPowerManager;
                                                                if (powerManager == null || !isScreenOn2) {
                                                                    z = false;
                                                                } else {
                                                                    z = false;
                                                                    powerManager.userActivity(SystemClock.uptimeMillis(), false);
                                                                }
                                                                this.mPowerOffToastShown = z;
                                                                showPowerOffToast(isScreenOn2);
                                                                if (mIsNeedMuteByPowerKeyDown && isScreenOn2 && isNeedMuteFirstPowerKeyDown()) {
                                                                    mIsNeedMuteByPowerKeyDown = false;
                                                                    mNeedHushByPowerKeyDown = true;
                                                                }
                                                                HwScreenOnProximityLock hwScreenOnProximityLock = this.mHwScreenOnProximityLock;
                                                                if (hwScreenOnProximityLock != null && hwScreenOnProximityLock.isShowing() && isScreenOn2 && !this.mPowerKeyDisTouch && (event.getFlags() & 1024) == 0) {
                                                                    this.mPowerKeyDisTouch = true;
                                                                    this.mPowerKeyDisTouchTime = event.getDownTime();
                                                                }
                                                                if (isScreenOn2 && !this.mScreenRecorderPowerKeyTriggered && (event.getFlags() & 1024) == 0) {
                                                                    this.mScreenRecorderPowerKeyTriggered = true;
                                                                    this.mScreenRecorderPowerKeyTime = event.getDownTime();
                                                                    interceptScreenRecorder();
                                                                }
                                                                if (isScreenOn2 && !this.mSystraceLogPowerKeyTriggered && (event.getFlags() & 1024) == 0) {
                                                                    this.mSystraceLogPowerKeyTriggered = true;
                                                                    this.mSystraceLogFingerPrintTime = 0;
                                                                    this.mSystraceLogVolumeUpKeyTriggered = false;
                                                                }
                                                                if (!isTvMode()) {
                                                                    break;
                                                                } else {
                                                                    interceptPowerKeyDownTv(event, isScreenOn2);
                                                                    break;
                                                                }
                                                            }
                                                        } else {
                                                            return 1;
                                                        }
                                                    default:
                                                        switch (keyCode3) {
                                                            case AwareJobSchedulerService.MSG_JOB_EXPIRED:
                                                            case AwareJobSchedulerService.MSG_CHECK_JOB:
                                                            case AwareJobSchedulerService.MSG_REMOVE_JOB:
                                                            case AwareJobSchedulerService.MSG_CONTROLLER_CHANGED:
                                                            case 405:
                                                                processing_KEYCODE_SOUNDTRIGGER_EVENT(keyCode3, this.mContext, isMusicActive() || isFMActive(), down2, keyguardIsShowingTq());
                                                                break;
                                                            default:
                                                                switch (keyCode3) {
                                                                    case AwarenessConstants.SWING_GESTURE_ACTION_MAX /*{ENCODED_INT: 511}*/:
                                                                    case 512:
                                                                    case 513:
                                                                    case 514:
                                                                        break;
                                                                    default:
                                                                        switch (keyCode3) {
                                                                            case 723:
                                                                                if (down2) {
                                                                                    interceptBlePowerKeyDown(event, isScreenOn2);
                                                                                } else {
                                                                                    interceptBlePowerKeyUp(event, isScreenOn2, event.isCanceled());
                                                                                }
                                                                                return result & -2;
                                                                            case 724:
                                                                                if (isFocusApp("com.huawei.camera")) {
                                                                                    break;
                                                                                } else {
                                                                                    launchUnderWaterCamera();
                                                                                    Log.i(TAG, "camera not front, not pass ble double click keyevent");
                                                                                    return result & -2;
                                                                                }
                                                                            case 725:
                                                                                if (isFocusApp("com.huawei.camera")) {
                                                                                    break;
                                                                                } else {
                                                                                    Log.i(TAG, "camera not front, not pass ble triple click keyevent");
                                                                                    return result & -2;
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                            } else {
                                                setTpKeep(keyCode3, down2);
                                                return result & -2;
                                            }
                                        }
                                    } else {
                                        WindowState focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow();
                                        if (focusedWindow == null || focusedWindow.getAttrs() == null) {
                                            isCameraOpened = false;
                                        } else {
                                            isCameraOpened = focusedWindow.getAttrs().packageName.contains("com.huawei.camera");
                                        }
                                        Log.d(TAG, "Camera keycode: " + keyCode3 + " isDwon:" + down2 + " isCamera: " + isCameraOpened);
                                        if (initialDown && !isCameraOpened) {
                                            Log.d(TAG, "Open Camara keycode: " + keyCode3 + " is comsuming.");
                                            Intent intent = new Intent(HUAWEI_RAPIDCAPTURE_START_MODE);
                                            intent.setPackage("com.huawei.camera");
                                            intent.putExtra("command", "launchHwCamera");
                                            try {
                                                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
                                            } catch (Exception e) {
                                                Slog.e(TAG, "unable to start service:" + intent, e);
                                            }
                                        }
                                    }
                                }
                                Log.d(TAG, "event.flags=" + flags + " previous mSystraceLogFingerPrintTime=" + this.mSystraceLogFingerPrintTime);
                                if (flags == 8) {
                                    if (!Jlog.isEnable() || !Jlog.isBetaUser() || !down2 || !isScreenOn2 || this.mSystraceLogPowerKeyTriggered || this.mSystraceLogVolumeDownKeyTriggered) {
                                        return result & -2;
                                    }
                                    this.mSystraceLogFingerPrintTime = event.getDownTime();
                                    return result & -2;
                                }
                            } else if (isTvMode()) {
                                if (initialDown || !down2) {
                                    Message message = this.mHandlerEx.obtainMessage(110, Boolean.valueOf(down2));
                                    message.setAsynchronous(true);
                                    message.sendToTarget();
                                }
                                return result & -2;
                            }
                        }
                        if (HwDeviceManager.disallowOp(38)) {
                            if (down2) {
                                this.mHandler.post(new Runnable() {
                                    /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass14 */

                                    public void run() {
                                        Toast toast = Toast.makeText(HwPhoneWindowManager.this.mContext, 33685973, 0);
                                        toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                                        toast.getWindowParams().privateFlags |= 16;
                                        toast.show();
                                    }
                                });
                            }
                            return result & -2;
                        } else if (HwDisplaySizeUtil.hasSideInScreen() && preventVolumeKeyForSideScreen(event)) {
                            return result & -2;
                        } else {
                            if (keyCode3 == 25) {
                                if (down2) {
                                    HwScreenOnProximityLock hwScreenOnProximityLock2 = this.mHwScreenOnProximityLock;
                                    if (hwScreenOnProximityLock2 == null || !hwScreenOnProximityLock2.isShowing() || !isScreenOn2 || this.mVolumeDownKeyDisTouch || (event.getFlags() & 1024) != 0) {
                                        if (isScreenOn2 && !this.mScreenRecorderVolumeDownKeyTriggered && (event.getFlags() & 1024) == 0) {
                                            cancelPendingPowerKeyActionForDistouch();
                                            this.mScreenRecorderVolumeDownKeyTriggered = true;
                                            cancelPendingScreenRecorderAction();
                                        }
                                        if (isScreenOn2 && !this.mSystraceLogVolumeDownKeyTriggered) {
                                            if ((event.getFlags() & 1024) == 0) {
                                                this.mSystraceLogVolumeDownKeyTriggered = true;
                                                this.mSystraceLogFingerPrintTime = 0;
                                                this.mSystraceLogVolumeUpKeyTriggered = false;
                                            }
                                        }
                                    } else {
                                        Log.d(TAG, "keycode: KEYCODE_VOLUME_DOWN is comsumed by disable touch mode.");
                                        this.mVolumeDownKeyDisTouch = true;
                                        if (!this.mHintShown) {
                                            this.mHwScreenOnProximityLock.forceShowHint();
                                            this.mHintShown = true;
                                        }
                                    }
                                } else {
                                    this.mVolumeDownKeyDisTouch = false;
                                    this.mScreenRecorderVolumeDownKeyTriggered = false;
                                    cancelPendingScreenRecorderAction();
                                    this.mSystraceLogVolumeDownKeyTriggered = false;
                                }
                                boolean keyguardShow = keyguardIsShowingTq();
                                Log.i(TAG, "interceptVolumeDownKey down=" + down2 + " keyguardShow=" + keyguardShow + " policyFlags=" + Integer.toHexString(policyFlags));
                                if (!isScreenOn2 || keyguardShow) {
                                    if (!isInjected4) {
                                        if ((event.getFlags() & 1024) == 0) {
                                            if (!isDeviceProvisioned()) {
                                                Log.i(TAG, "Device is not Provisioned");
                                            } else if (down2) {
                                                boolean isIntercept = false;
                                                boolean isVolumeDownDoubleClick = false;
                                                boolean isPhoneActive2 = isMusicActive() || isPhoneInCall() || isVoiceCall() || isFMActive();
                                                if (this.isVoiceRecognitionActive) {
                                                    isInjected3 = isInjected4;
                                                    long interval = event.getEventTime() - this.mLastStartVassistantServiceTime;
                                                    if (interval > 15000) {
                                                        this.isVoiceRecognitionActive = false;
                                                    } else if (interval > 1500) {
                                                        this.isVoiceRecognitionActive = AudioSystem.isSourceActive(6);
                                                    }
                                                } else {
                                                    isInjected3 = isInjected4;
                                                }
                                                if (isDoubleClickEnabled()) {
                                                    isPhoneActive = isPhoneActive2;
                                                    long timediff = event.getEventTime() - this.mLastVolumeDownKeyDownTime;
                                                    this.mLastVolumeDownKeyDownTime = event.getEventTime();
                                                    if (timediff < 400) {
                                                        isVolumeDownDoubleClick = true;
                                                        if (this.mListener == null) {
                                                            this.mListener = new ProximitySensorListener();
                                                        }
                                                        turnOnSensorListener();
                                                        if ((!this.mIsProximity && this.mSensorRegisted) || !this.mSensorRegisted) {
                                                            Log.i(TAG, "mIsProximity " + this.mIsProximity + ", mSensorRegisted " + this.mSensorRegisted);
                                                            notifyRapidCaptureService("start");
                                                        }
                                                        turnOffSensorListener();
                                                        result &= -2;
                                                    } else {
                                                        notifyRapidCaptureService("wakeup");
                                                        if (this.mListener == null) {
                                                            this.mListener = new ProximitySensorListener();
                                                        }
                                                        turnOnSensorListener();
                                                    }
                                                    if (!isScreenOn2 || isVolumeDownDoubleClick) {
                                                        isIntercept = true;
                                                    }
                                                } else {
                                                    isPhoneActive = isPhoneActive2;
                                                }
                                                boolean isVassistantInstall = checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE) || checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE_OVERSEA);
                                                if (!isPhoneActive && !isScreenOn2 && !isVolumeDownDoubleClick && isVassistantInstall) {
                                                    notifyVassistantService("wakeup", 2, event);
                                                    if (this.mListener == null) {
                                                        this.mListener = new ProximitySensorListener();
                                                    }
                                                    turnOnSensorListener();
                                                    interceptQuickCallChord();
                                                    isIntercept = true;
                                                }
                                                Log.i(TAG, "intercept volume down key, isIntercept=" + isIntercept + " now=" + SystemClock.uptimeMillis() + " EventTime=" + event.getEventTime());
                                                if (isInterceptAndCheckRinging(isIntercept)) {
                                                    return result;
                                                }
                                                if (getTelecommService().isInCall() && (result & 1) == 0 && (hwCustPhoneWindowManager2 = this.mCust) != null && hwCustPhoneWindowManager2.isVolumnkeyWakeup(this.mContext)) {
                                                    this.mCust.volumnkeyWakeup(this.mContext, isScreenOn2, this.mPowerManager);
                                                }
                                            } else {
                                                isInjected3 = isInjected4;
                                                if (event.getEventTime() - event.getDownTime() >= 500) {
                                                    resetVolumeDownKeyLongPressed();
                                                } else {
                                                    cancelPendingQuickCallChordAction();
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (keyCode3 == 24) {
                                if (down2) {
                                    HwScreenOnProximityLock hwScreenOnProximityLock3 = this.mHwScreenOnProximityLock;
                                    if (hwScreenOnProximityLock3 == null || !hwScreenOnProximityLock3.isShowing() || !isScreenOn2 || this.mVolumeUpKeyDisTouch || (event.getFlags() & 1024) != 0) {
                                        if (isScreenOn2 && !this.mScreenRecorderVolumeUpKeyTriggered && (event.getFlags() & 1024) == 0) {
                                            cancelPendingPowerKeyActionForDistouch();
                                            this.mScreenRecorderVolumeUpKeyTriggered = true;
                                            this.mScreenRecorderVolumeUpKeyTime = event.getDownTime();
                                            this.mScreenRecorderVolumeUpKeyConsumed = false;
                                            interceptScreenRecorder();
                                        }
                                        Log.d(TAG, "isScreenOn=" + isScreenOn2 + " mSystraceLogVolumeUpKeyTriggered=" + this.mSystraceLogVolumeUpKeyTriggered + " mScreenRecorderVolumeUpKeyConsumed=" + this.mScreenRecorderVolumeUpKeyConsumed);
                                        if (Jlog.isEnable() && Jlog.isBetaUser() && isScreenOn2 && !this.mSystraceLogVolumeUpKeyTriggered && !this.mSystraceLogPowerKeyTriggered && !this.mSystraceLogVolumeDownKeyTriggered && !this.mScreenRecorderVolumeUpKeyConsumed && (event.getFlags() & 1024) == 0) {
                                            this.mSystraceLogVolumeUpKeyTriggered = true;
                                            this.mSystraceLogVolumeUpKeyTime = event.getDownTime();
                                            this.mSystraceLogVolumeUpKeyConsumed = false;
                                            interceptSystraceLog();
                                            Log.d(TAG, "volumeup process: fingerprint first, then volumeup");
                                            if (this.mSystraceLogVolumeUpKeyConsumed) {
                                                return result & -2;
                                            }
                                        }
                                        if (getTelecommService().isInCall() && (result & 1) == 0 && (hwCustPhoneWindowManager = this.mCust) != null && hwCustPhoneWindowManager.isVolumnkeyWakeup(this.mContext)) {
                                            this.mCust.volumnkeyWakeup(this.mContext, isScreenOn2, this.mPowerManager);
                                        }
                                    } else {
                                        Log.d(TAG, "keycode: KEYCODE_VOLUME_UP is comsumed by disable touch mode.");
                                        this.mVolumeUpKeyDisTouch = true;
                                        this.mVolumeUpKeyDisTouchTime = event.getDownTime();
                                        this.mVolumeUpKeyConsumedByDisTouch = false;
                                        if (!this.mHintShown) {
                                            this.mHwScreenOnProximityLock.forceShowHint();
                                            this.mHintShown = true;
                                        }
                                        cancelPendingPowerKeyActionForDistouch();
                                    }
                                } else {
                                    this.mVolumeUpKeyDisTouch = false;
                                    this.mScreenRecorderVolumeUpKeyTriggered = false;
                                    cancelPendingScreenRecorderAction();
                                    this.mSystraceLogVolumeUpKeyTriggered = false;
                                }
                                HwCustPhoneWindowManager hwCustPhoneWindowManager4 = this.mCust;
                                if (hwCustPhoneWindowManager4 != null) {
                                    isInjected2 = isInjected4;
                                    keyCode2 = keyCode3;
                                    if (hwCustPhoneWindowManager4.interceptVolumeUpKey(event, this.mContext, isScreenOn2, keyguardIsShowingTq(), isMusicActive() || isVoiceCall() || isFMActive(), isInjected2, down2)) {
                                        return result;
                                    }
                                } else {
                                    keyCode2 = keyCode3;
                                    isInjected2 = isInjected4;
                                }
                            }
                        }
                    } else if (!this.mDefaultDisplayPolicy.hasNavigationBar() && down2) {
                        if (isScreenInLockTaskMode()) {
                            this.mMenuKeyPress = true;
                            this.mMenuKeyPressTime = event.getDownTime();
                            interceptBackandMenuKey();
                        } else {
                            this.mMenuKeyPress = false;
                            this.mMenuKeyPressTime = 0;
                        }
                    }
                    return HwPhoneWindowManager.super.interceptKeyBeforeQueueing(event, policyFlags);
                }
                if (down) {
                    HwFalseTouchMonitor hwFalseTouchMonitor = this.mFalseTouchMonitor;
                    if (hwFalseTouchMonitor != null) {
                        hwFalseTouchMonitor.handleKeyEvent(keyEvent);
                    }
                    HwScreenOnProximityLock hwScreenOnProximityLock4 = this.mHwScreenOnProximityLock;
                    if (hwScreenOnProximityLock4 != null && hwScreenOnProximityLock4.isShowing() && isScreenOn && !this.mHintShown && (event.getFlags() & 1024) == 0) {
                        Log.d(TAG, "keycode: " + keyCode + " is comsumed by disable touch mode.");
                        this.mHwScreenOnProximityLock.forceShowHint();
                        this.mHintShown = true;
                        return HwPhoneWindowManager.super.interceptKeyBeforeQueueing(event, policyFlags);
                    }
                }
                boolean isFrontFpNavi = FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION;
                boolean isSupportTrikey = FrontFingerPrintSettings.isSupportTrikey();
                boolean isMMITest = isMMITesting();
                Flog.i((int) WifiProCommonUtils.RESP_CODE_INVALID_URL, "HwPhoneWindowManagerinterceptKeyBeforeQueueing deviceID:" + deviceID + " isFrontFpNavi:" + isFrontFpNavi + " isSupportTrikey:" + isSupportTrikey + " isMMITest:" + isMMITest);
                if (deviceID > 0 && isFrontFpNavi && isSupportTrikey && !isMMITest) {
                    if (keyCode == 4) {
                        if (isTrikeyNaviKeycodeFromLON(isInjected, isExcluedBackScene())) {
                            return 0;
                        }
                        sendLightTimeoutMsg();
                        if (down) {
                            this.mBackTrikeyHandled = false;
                            Message msg = this.mHandlerEx.obtainMessage(MSG_TRIKEY_BACK_LONG_PRESS);
                            msg.setAsynchronous(true);
                            this.mHandlerEx.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                            if (this.mTrikeyNaviMode == 1) {
                                return 0;
                            }
                        } else {
                            boolean isInjected5 = this.mBackTrikeyHandled;
                            if (!this.mBackTrikeyHandled) {
                                this.mBackTrikeyHandled = true;
                                this.mHandlerEx.removeMessages(MSG_TRIKEY_BACK_LONG_PRESS);
                            }
                            if (isInjected5) {
                                return 0;
                            }
                            startHwVibrate(VIBRATOR_SHORT_PRESS_FOR_FRONT_FP);
                            if (this.mTrikeyNaviMode == 1) {
                                Flog.bdReport(this.mContext, 16);
                                sendKeyEvent(187);
                                return 0;
                            }
                        }
                    }
                }
                if (!this.mDefaultDisplayPolicy.hasNavigationBar() && keyCode == 4 && down) {
                    if (isScreenInLockTaskMode()) {
                        this.mBackKeyPress = true;
                        this.mBackKeyPressTime = event.getDownTime();
                        interceptBackandMenuKey();
                    } else {
                        this.mBackKeyPress = false;
                        this.mBackKeyPressTime = 0;
                    }
                }
                return HwPhoneWindowManager.super.interceptKeyBeforeQueueing(event, policyFlags);
            }
        }
    }

    private boolean isDoubleClickEnabled() {
        HwSideTouchManager hwSideTouchManager;
        boolean isMusicOrFMOrVoiceCallActive = isMusicActive() || isVoiceCall() || isFMActive();
        boolean isSideScreenAndTalkbackEnable = this.mIsSupportSideScreen && (hwSideTouchManager = this.mSideTouchManager) != null && hwSideTouchManager.getTalkBackEnableState();
        Log.i(TAG, "isMusicOrFMOrVoiceCallActive=" + isMusicOrFMOrVoiceCallActive + " isVoiceRecognitionActive=" + this.isVoiceRecognitionActive + " isSideScreenAndTalkbackEnable=" + isSideScreenAndTalkbackEnable);
        return !isMusicOrFMOrVoiceCallActive && !this.isVoiceRecognitionActive && !SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false) && !isSideScreenAndTalkbackEnable;
    }

    private void reportMonitorData(int keyCode, boolean down) {
        TurnOnWakeScreenManager turnOnWakeScreenManager;
        ContinuePowerDevMng.getInstance().keyDownEvent(keyCode, down);
        if (!down && keyCode == 26 && (turnOnWakeScreenManager = TurnOnWakeScreenManager.getInstance(this.mContext)) != null && turnOnWakeScreenManager.isTurnOnSensorSupport()) {
            TurnOnWakeScreenManager.getInstance(this.mContext).reportMonitorData("action=pressPowerKey");
        }
    }

    private boolean getIsSwich() {
        return isSwich;
    }

    private void setIsSwich(boolean isSwich2) {
        isSwich = isSwich2;
    }

    private boolean lightScreenOnPcMode(int keyCode) {
        boolean isDreaming = this.mPowerManagerInternal.isUserActivityScreenDimOrDream();
        if (HwPCUtils.isPcCastModeInServer() || HwPCUtils.getPhoneDisplayID() != -1) {
            if (keyCode == 601) {
                setIsSwich(isDreaming);
            }
            if (getIsSwich() && (keyCode == 3 || keyCode == 4)) {
                isSwich = false;
                return true;
            }
        }
        if ((HwPCUtils.isPcCastModeInServer() || HwPCUtils.getPhoneDisplayID() != -1 || HwPCUtils.isInWindowsCastMode() || HwPCUtils.isDisallowLockScreenForHwMultiDisplay()) && !HwPCUtils.enabledInPad() && (keyCode == 26 || keyCode == 502 || keyCode == 511 || keyCode == 512 || keyCode == 513 || keyCode == 514 || keyCode == 501 || keyCode == 601 || keyCode == 515)) {
            boolean keyHandled = false;
            try {
                IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
                if (pcMgr != null && !pcMgr.isScreenPowerOn()) {
                    HwPCUtils.log(TAG, "some key set screen from OFF to ON");
                    pcMgr.setScreenPower(true);
                    if (this.mPowerManager != null) {
                        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                    }
                    notifyScreenPowerChanged(true);
                    boolean isPhoneModeLocked = HwPCUtils.getPhoneDisplayID() != -1 && isScreenLocked() && keyCode == 26;
                    if (HwPCUtils.isDisallowLockScreenForHwMultiDisplay() || !isPhoneModeLocked) {
                        keyHandled = true;
                        if (keyCode == 26) {
                            cancelPendingPowerKeyActionForDistouch();
                        }
                    }
                }
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "lightScreenOnPcMode " + e);
                HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwPCDataReporter().reportFailLightScreen(1, keyCode, "");
            }
            if (isDreaming || keyHandled) {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean isRinging() {
        TelecomManager telecomManager = getTelecommService();
        return telecomManager != null && telecomManager.isRinging() && "1".equals(SystemProperties.get("persist.sys.show_incallscreen", "0"));
    }

    public KeyEvent dispatchUnhandledKey(WindowState win, KeyEvent event, int policyFlags) {
        IHwSwingService hwSwing = HwSwingManager.getService();
        String pkgName = win == null ? "NA" : win.getOwningPackage();
        if (hwSwing != null) {
            try {
                if (hwSwing.dispatchUnhandledKey(event, pkgName)) {
                    return null;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "dispatchUnhandledKey error : " + e.getMessage());
            }
        }
        return HwPhoneWindowManager.super.dispatchUnhandledKey(win, event, policyFlags);
    }

    private boolean isHardwareKeyboardConnected() {
        Log.i(TAG, "isHardwareKeyboardConnected--begin");
        int[] devices = InputDevice.getDeviceIds();
        boolean isConnected = false;
        int i = 0;
        while (true) {
            if (i >= devices.length) {
                break;
            }
            InputDevice device = InputDevice.getDevice(devices[i]);
            if (device != null) {
                if (device.getProductId() != MRX_KEYBOARD_VID || device.getVendorId() != 1455) {
                    if (device.isExternal() && (device.getSources() & 257) != 0) {
                        isConnected = true;
                        break;
                    }
                } else {
                    isConnected = true;
                    break;
                }
            }
            i++;
        }
        Log.i(TAG, "isHardwareKeyboardConnected--end");
        return isConnected;
    }

    private boolean isRightKey(int keyCode) {
        if (keyCode >= 7 && keyCode <= 16) {
            return true;
        }
        if (keyCode < 29 || keyCode > 54) {
            return false;
        }
        return true;
    }

    private void setToolType() {
        StylusGestureListener stylusGestureListener;
        if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || (stylusGestureListener = this.mStylusGestureListener4PCMode) == null) {
            StylusGestureListener stylusGestureListener2 = this.mStylusGestureListener;
            if (stylusGestureListener2 != null) {
                stylusGestureListener2.setToolType();
                return;
            }
            return;
        }
        stylusGestureListener.setToolType();
    }

    private void handleGameEvent(KeyEvent event) {
        if (!(event.getAction() == 0) && event.getEventTime() - event.getDownTime() <= 500) {
            Slog.d(TAG, "gamekey arrive, notify GameAssist");
            HwGameAssistManager.notifyKeyEvent();
        }
    }

    private void handleGameSpace(KeyEvent event) {
        HwGameSpaceToggleManager hwGameSpaceToggleManager = this.mHwGameSpaceToggleManager;
        if (hwGameSpaceToggleManager != null) {
            hwGameSpaceToggleManager.handleGameSpace(event);
        }
    }

    private void interceptBlePowerKeyDown(KeyEvent event, boolean isScreenOn) {
        PowerManager.WakeLock wakeLock = this.mBlePowerKeyWakeLock;
        if (wakeLock != null && !wakeLock.isHeld()) {
            this.mBlePowerKeyWakeLock.acquire();
        }
        if (!isScreenOn) {
            wakeUpFromPowerKey(event.getDownTime());
        }
    }

    private void interceptBlePowerKeyUp(KeyEvent event, boolean isScreenOn, boolean isCanceled) {
        PowerManager.WakeLock wakeLock = this.mBlePowerKeyWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            this.mBlePowerKeyWakeLock.release();
        }
        if (isScreenOn && !isCanceled) {
            this.mRequestedOrGoingToSleep = true;
            this.mPowerManager.goToSleep(event.getDownTime(), 4, 0);
        }
    }

    private boolean isFocusApp(String pkgName) {
        if (pkgName == null) {
            Log.e(TAG, "isFocusApp, pkgName is null");
            return false;
        }
        WindowState focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow();
        if (focusedWindow == null || focusedWindow.getAttrs() == null) {
            Log.e(TAG, "isFocusApp, focusedWindow or getAttrs is null");
            return false;
        }
        String focusPackageName = focusedWindow.getAttrs().packageName;
        Log.i(TAG, "isFocusApp, focusPackageName:" + focusPackageName + ",pkgName:" + pkgName);
        return pkgName.equals(focusPackageName);
    }

    private void launchUnderWaterCamera() {
        Log.i(TAG, "launchUnderWaterCamera");
        Intent intent = new Intent(HUAWEI_RAPIDCAPTURE_START_MODE);
        intent.setPackage("com.huawei.camera");
        intent.putExtra("command", "launchUnderWaterCamera");
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to launchUnderWaterCamera throw IllegalStateException");
        } catch (ServiceConfigurationError e2) {
            Log.e(TAG, "Failed to launchUnderWaterCamera throw ServiceConfigurationError");
        } catch (SecurityException e3) {
            Log.e(TAG, "Failed to launchUnderWaterCamera throw SecurityException");
        }
    }

    /* access modifiers changed from: private */
    public void startNotepadActivity() {
        Intent notePadEditorIntent = new Intent("android.huawei.intent.action.note.handwriting");
        LogPower.push((int) CPUFeature.MSG_EXIT_GAME_SCENE, "visible", NEW_HWNOTEPAD_PKG_NAME);
        LogPower.push((int) CPUFeature.MSG_EXIT_GAME_SCENE, "visible", PKG_HWNOTEPAD);
        Log.i(TAG, "LogPower push notepad FREEZER_EXCEPTION success");
        try {
            this.mContext.startActivityAsUser(notePadEditorIntent, UserHandle.CURRENT_OR_SELF);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "startActivity notepad activity failed message : " + ex.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "startActivityAsUser(): notepad activity Exception");
        }
    }

    private void wakeupNoteEditor(KeyEvent event, boolean isInjected) {
        Log.i(TAG, "enter wakeupNoteEditor");
        if (!this.mSystemReady) {
            Log.d(TAG, "system not ready, return");
        } else if (!isInjected) {
            int keyCode = event.getKeyCode();
            boolean isWakeupTimeout = false;
            boolean down = event.getAction() == 0;
            if (keyCode != 704 || !down) {
                if (keyCode == 705 && down) {
                    Log.d(TAG, "recieved KEYCODE_STYLUS_POWERON");
                    PowerManager powerManager = (PowerManager) this.mContext.getSystemService(HIVOICE_PRESS_TYPE_POWER);
                    if (powerManager == null) {
                        return;
                    }
                    if (!powerManager.isScreenOn()) {
                        powerManager.wakeUp(SystemClock.uptimeMillis());
                        return;
                    }
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "stylus_state_activate", 0) == 1) {
                        isWakeupTimeout = true;
                    }
                    if (!isWakeupTimeout) {
                        Settings.Global.putInt(this.mContext.getContentResolver(), "stylus_state_activate", 1);
                        Log.d(TAG, "recieve stylus signal and activate stylus.");
                    }
                } else if (!this.mDeviceProvisioned) {
                    Log.d(TAG, "Device not Provisioned, return");
                } else if (down) {
                    wakeupSOSPage(keyCode);
                    if (this.mIsFreezePowerkey && keyCode == 26) {
                        powerPressBDReport(980);
                    }
                } else if (this.mIsFreezePowerkey && keyCode == 26) {
                    powerPressBDReport(981);
                }
            } else if (!this.mDeviceProvisioned) {
                Log.d(TAG, "Device not Provisioned, return");
            } else {
                long now = SystemClock.uptimeMillis();
                if (now - this.mLastWakeupTime >= 500) {
                    isWakeupTimeout = true;
                }
                if (!((PowerManager) this.mContext.getSystemService(HIVOICE_PRESS_TYPE_POWER)).isScreenOn() && isWakeupTimeout) {
                    String pkgName = getTopActivity();
                    Log.d(TAG, "wakeup NoteEditor now " + now + " screenOffTime " + this.mScreenOffTime);
                    if (!NOTEEDITOR_ACTIVITY_NAME.equals(pkgName) && !HW_NOTEEDITOR_ACTIVITY_NAME.equals(pkgName)) {
                        startNotepadActivity();
                        this.mLastWakeupTime = SystemClock.uptimeMillis();
                    } else if (now - this.mScreenOffTime < TOUCH_SPINNING_DELAY_MILLIS) {
                        this.mHandler.postDelayed(new Runnable() {
                            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass15 */

                            public void run() {
                                if (PhoneWindowManager.HWFLOW) {
                                    Log.d(HwPhoneWindowManager.TAG, "start activity delayed");
                                }
                                HwPhoneWindowManager.this.startNotepadActivity();
                            }
                        }, 500);
                        this.mLastWakeupTime = SystemClock.uptimeMillis();
                    }
                }
            }
        }
    }

    private boolean setGSensorEnabled(int keyCode, boolean down, int deviceID) {
        boolean z = false;
        if (keyCode != 703 || deviceID != this.mTpDeviceId) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("keyCode=");
        sb.append(keyCode);
        sb.append(" down=");
        sb.append(down);
        sb.append(" isFromTP=");
        if (deviceID == this.mTpDeviceId) {
            z = true;
        }
        sb.append(z);
        Log.i(TAG, sb.toString());
        if (down) {
            TurnOnWakeScreenManager.getInstance(this.mContext).setAccSensorEnabled(true);
        }
        return true;
    }

    public long interceptKeyBeforeDispatching(WindowState win, KeyEvent event, int policyFlags) {
        boolean down;
        boolean handleResult;
        String lastIme;
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int flags = event.getFlags();
        boolean down2 = event.getAction() == 0;
        int deviceID = event.getDeviceId();
        boolean isInjected = (16777216 & policyFlags) != 0;
        Flog.i((int) WifiProCommonUtils.RESP_CODE_INVALID_URL, "HwPhoneWindowManagerinterceptKeyTi keyCode=" + keyCode + " down=" + down2 + " repeatCount=" + repeatCount + " isInjected=" + isInjected);
        if (setGSensorEnabled(keyCode, down2, deviceID)) {
            return -1;
        }
        if (HwPCUtils.isPcCastModeInServer()) {
            down = down2;
            if (event.getEventTime() - this.mLastKeyPointerTime > 500) {
                this.mLastKeyPointerTime = event.getEventTime();
                userActivityOnDesktop();
            }
        } else {
            down = down2;
            if (HwPCUtils.isInWindowsCastMode()) {
                WindowManagerExt.updateFocusWindowFreezed(true);
            }
        }
        if (handleInputEventInPCCastMode(event)) {
            return -1;
        }
        if (win != null && win.getWindowingMode() == 103 && keyCode == 66) {
            HwMwUtils.performPolicy(128, new Object[]{win});
        }
        try {
            if (this.mIHwWindowCallback != null) {
                this.mIHwWindowCallback.interceptKeyBeforeDispatching(event, policyFlags);
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
                    /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass16 */

                    public void run() {
                        Toast toast = Toast.makeText(HwPhoneWindowManager.this.mContext, 33685654, 0);
                        toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                        toast.getWindowParams().privateFlags |= 16;
                        toast.show();
                    }
                });
            }
            return (long) result;
        }
        if (isRightKey(keyCode) && isHardwareKeyboardConnected() && (lastIme = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method")) != null && lastIme.contains("com.visionobjects.stylusmobile.v3_2_huawei")) {
            HwInputMethodManager.setDefaultIme("");
            setToolType();
        }
        int result1 = getGameControlKeyReslut(event);
        if (-2 != result1) {
            Log.i(TAG, "getGameControlKeyReslut return !");
            return (long) result1;
        } else if ((keyCode == 3 || keyCode == 187) && win != null && (win.getAttrs().hwFlags & FLOATING_MASK) == FLOATING_MASK) {
            return 0;
        } else {
            if (keyCode == 82 && !this.mDefaultDisplayPolicy.hasNavigationBar() && (268435456 & flags) == 0) {
                if (!down) {
                    if (this.mMenuClickedOnlyOnce) {
                        this.mMenuClickedOnlyOnce = false;
                        sendHwMenuKeyEvent();
                    }
                    cancelPreloadRecentApps();
                    return -1;
                } else if (repeatCount == 0) {
                    this.mMenuClickedOnlyOnce = true;
                    preloadRecentApps();
                    return -1;
                } else if (repeatCount != 1) {
                    return -1;
                } else {
                    this.mMenuClickedOnlyOnce = false;
                    toggleRecentApps();
                    return -1;
                }
            } else if (!this.mVolumeUpKeyDisTouch || this.mPowerKeyDisTouch || (flags & 1024) != 0) {
                if (keyCode == 24) {
                    if (this.mVolumeUpKeyConsumedByDisTouch) {
                        if (down) {
                            return -1;
                        }
                        this.mVolumeUpKeyConsumedByDisTouch = false;
                        this.mHintShown = false;
                        return -1;
                    } else if (this.mHintShown) {
                        if (down) {
                            return -1;
                        }
                        this.mHintShown = false;
                        return -1;
                    }
                }
                if (isNeedPassEventToCloud(keyCode)) {
                    return 0;
                }
                if (handleDesktopKeyEvent(event)) {
                    return -1;
                }
                if (IS_TABLET && handlePadKeyEvent(event)) {
                    return -1;
                }
                if (keyCode == 3 || keyCode == 4 || keyCode == 25 || keyCode == 187) {
                    if (!this.mHintShown) {
                        boolean isFrontFpNavi = FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION;
                        boolean isSupportTrikey = FrontFingerPrintSettings.isSupportTrikey();
                        boolean isMMITest = isMMITesting();
                        Flog.i((int) WifiProCommonUtils.RESP_CODE_INVALID_URL, "HwPhoneWindowManagerdeviceID:" + deviceID + " isFrontFpNavi:" + isFrontFpNavi + " isSupportTrikey:" + isSupportTrikey + " isMMITest:" + isMMITest);
                        if (deviceID > 0 && isFrontFpNavi && isSupportTrikey && !isMMITest) {
                            if (keyCode == 187) {
                                if (isTrikeyNaviKeycodeFromLON(isInjected, isExcluedRecentScene())) {
                                    return -1;
                                }
                                sendLightTimeoutMsg();
                                if (!down) {
                                    boolean handled = this.mRecentTrikeyHandled;
                                    if (!this.mRecentTrikeyHandled) {
                                        this.mRecentTrikeyHandled = true;
                                        this.mHandlerEx.removeMessages(MSG_TRIKEY_RECENT_LONG_PRESS);
                                    }
                                    if (handled) {
                                        return -1;
                                    }
                                    int i = this.mTrikeyNaviMode;
                                    if (i == 1) {
                                        startHwVibrate(VIBRATOR_SHORT_PRESS_FOR_FRONT_FP);
                                        sendKeyEvent(4);
                                        return -1;
                                    } else if (i != 0) {
                                        return -1;
                                    } else {
                                        Flog.bdReport(this.mContext, 17);
                                        startHwVibrate(VIBRATOR_SHORT_PRESS_FOR_FRONT_FP);
                                        toggleRecentApps();
                                        return -1;
                                    }
                                } else if (repeatCount != 0) {
                                    return -1;
                                } else {
                                    this.mRecentTrikeyHandled = false;
                                    Message msg = this.mHandlerEx.obtainMessage(MSG_TRIKEY_RECENT_LONG_PRESS);
                                    msg.setAsynchronous(true);
                                    this.mHandlerEx.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                                    if (this.mTrikeyNaviMode != 0) {
                                        return -1;
                                    }
                                    preloadRecentApps();
                                    return -1;
                                }
                            }
                        }
                    } else if (down) {
                        return -1;
                    } else {
                        this.mHintShown = false;
                        return -1;
                    }
                }
                if ((flags & 1024) == 0) {
                    if (this.mScreenRecorderVolumeUpKeyTriggered && !this.mScreenRecorderPowerKeyTriggered) {
                        long now = SystemClock.uptimeMillis();
                        long timeoutTime = this.mScreenRecorderVolumeUpKeyTime + 150;
                        if (now < timeoutTime) {
                            return timeoutTime - now;
                        }
                    }
                    if (keyCode != 24 || !this.mScreenRecorderVolumeUpKeyConsumed) {
                        if (this.mSystraceLogVolumeUpKeyTriggered) {
                            long now2 = SystemClock.uptimeMillis();
                            long timeoutTime2 = this.mSystraceLogVolumeUpKeyTime + 150;
                            if (now2 < timeoutTime2) {
                                Log.d(TAG, "keyCode=" + keyCode + " down=" + down + " in queue: now=" + now2 + " timeout=" + timeoutTime2);
                                return timeoutTime2 - now2;
                            }
                            handleResult = down;
                        } else {
                            handleResult = down;
                        }
                        if (keyCode == 24 && this.mSystraceLogVolumeUpKeyConsumed) {
                            if (!handleResult) {
                                this.mSystraceLogVolumeUpKeyConsumed = false;
                            }
                            Log.d(TAG, "systracelog volumeup down=" + handleResult + " leave queue");
                            return -1;
                        }
                    } else if (down) {
                        return -1;
                    } else {
                        this.mScreenRecorderVolumeUpKeyConsumed = false;
                        return -1;
                    }
                }
                return HwPhoneWindowManager.super.interceptKeyBeforeDispatching(win, event, policyFlags);
            } else {
                long now3 = SystemClock.uptimeMillis();
                long timeoutTime3 = this.mVolumeUpKeyDisTouchTime + 150;
                if (now3 < timeoutTime3) {
                    return timeoutTime3 - now3;
                }
                return -1;
            }
        }
    }

    private int getInputDeviceId(int inputSource) {
        int[] devIds = InputDevice.getDeviceIds();
        for (int devId : devIds) {
            InputDevice inputDev = InputDevice.getDevice(devId);
            if (inputDev != null && inputDev.supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }

    private void initTpKeepParamters() {
        boolean ret = false;
        try {
            IServiceManager serviceManager = IServiceManager.getService();
            if (serviceManager != null) {
                ret = serviceManager.registerForNotifications(ITouchscreen.kInterfaceName, "", this.mServiceNotification);
            }
            if (!ret) {
                Slog.e(TAG, "Failed to register service start notification");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register service start notification", e);
        }
        connectToProxy();
    }

    private final class ServiceNotification extends IServiceNotification.Stub {
        private ServiceNotification() {
        }

        public void onRegistration(String fqName, String name, boolean preexisting) {
            Slog.i(HwPhoneWindowManager.TAG, "tp hal service started " + fqName + " " + name);
            HwPhoneWindowManager.this.connectToProxy();
        }
    }

    private final class TPKeepDeathRecipient implements IHwBinder.DeathRecipient {
        private TPKeepDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            if (cookie == 1001) {
                Slog.e(HwPhoneWindowManager.TAG, "tp hal service died cookie: " + cookie);
                synchronized (HwPhoneWindowManager.this.mLock) {
                    ITouchscreen unused = HwPhoneWindowManager.this.mTpTouchSwitch = null;
                    if (HwPhoneWindowManager.this.mTpKeepListener != null) {
                        HwPhoneWindowManager.this.mTpKeepListener.setTpKeep(false);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void connectToProxy() {
        synchronized (this.mLock) {
            if (this.mTpTouchSwitch == null) {
                try {
                    this.mTpTouchSwitch = ITouchscreen.getService();
                    if (this.mTpTouchSwitch != null) {
                        this.mTpTouchSwitch.linkToDeath(new TPKeepDeathRecipient(), 1001);
                    }
                } catch (NoSuchElementException e) {
                    Slog.e(TAG, "connectToProxy: tp hal service not found. Did the service fail to start?", e);
                } catch (RemoteException e2) {
                    Slog.e(TAG, "connectToProxy: tp hal service not responding", e2);
                }
            }
        }
    }

    private void setTpKeep(int keyCode, boolean down) {
        if (this.mProximityTop && down && this.mTpKeepListener != null) {
            if (keyCode == 708) {
                this.mTpKeepListener.setTpKeep(true);
            } else if (keyCode == 709) {
                this.mTpKeepListener.setTpKeep(false);
            }
        }
    }

    private void initAudioCallback() {
        this.mAudioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass17 */

            @Override // android.media.AudioManager.AudioPlaybackCallback
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                super.onPlaybackConfigChanged(configs);
                if (configs != null) {
                    boolean isPlaying = false;
                    Iterator<AudioPlaybackConfiguration> it = configs.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (it.next().getPlayerState() == 2) {
                                isPlaying = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (HwPhoneWindowManager.this.mIsAudioPlaying != isPlaying) {
                        boolean unused = HwPhoneWindowManager.this.mIsAudioPlaying = isPlaying;
                        Log.i(HwPhoneWindowManager.TAG, "AudioManagerPlaybackListener isPlaying:" + isPlaying);
                        HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                        hwPhoneWindowManager.updateAudioStatus(hwPhoneWindowManager.mIsAudioPlaying);
                    }
                }
            }
        };
    }

    private void initSideConfigCallback() {
        this.mSideTouchHandConfigCallback = new IHwSideTouchCallback() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass18 */

            public void notifySideConfig(String sideConfig) {
                if (HwPhoneWindowManager.this.mTpTouchSwitch != null) {
                    try {
                        HwPhoneWindowManager.this.mTpTouchSwitch.hwSetFeatureConfig(3, sideConfig);
                        Log.i(HwPhoneWindowManager.TAG, "notifySideConfig config is: " + sideConfig);
                        HwPhoneWindowManager.this.mTpTouchSwitch.hwSetFeatureConfig(3, "0");
                        Log.i(HwPhoneWindowManager.TAG, "notifySideConfig audio status is: " + "0");
                    } catch (RemoteException e) {
                        Log.e(HwPhoneWindowManager.TAG, "updateAudioStatus RemoteException");
                    }
                }
            }
        };
        HwSideTouchManager.getInstance(this.mContext).registerCallback(this.mSideTouchHandConfigCallback);
    }

    /* access modifiers changed from: private */
    public void updateAudioStatus(boolean isPlaybackActive) {
        String sideConfig;
        if (this.mTpTouchSwitch == null) {
            Log.d(TAG, "get touch service failed");
            return;
        }
        try {
            if (!(this.mSideTouchManager == null || (sideConfig = this.mSideTouchManager.getSideConfig()) == null)) {
                int result = this.mTpTouchSwitch.hwSetFeatureConfig(3, sideConfig);
                Log.i(TAG, "updateAudioStatus sideConfig: " + sideConfig + " result = " + result);
            }
            if (this.mSideStatusManager != null) {
                String status = this.mSideStatusManager.getAudioStatus(isPlaybackActive);
                Log.i(TAG, "updateAudioStatus isPlaybackActive: " + isPlaybackActive + " status = " + status);
                this.mTpTouchSwitch.hwSetFeatureConfig(3, status);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "updateAudioStatus RemoteException");
        }
    }

    public void setTPDozeMode(int scene, int mode) {
        ITouchscreen iTouchscreen = this.mTpTouchSwitch;
        if (iTouchscreen == null) {
            Slog.d(TAG, "get touch service failed");
            return;
        }
        try {
            boolean isSuccess = iTouchscreen.hwTsSetDozeMode(scene, mode, 0);
            if (Log.isLoggable(TAG, 3)) {
                Slog.d(TAG, "set parameter scene:" + scene + ",mode:" + mode + "  sucess:" + isSuccess);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "set doze mode RemoteException:" + e.getMessage());
        } catch (Exception e2) {
            Slog.e(TAG, "get service  error");
        }
    }

    public void setScreenChangedReason(int reason) {
        ITouchscreen iTouchscreen = this.mTpTouchSwitch;
        if (iTouchscreen == null) {
            Slog.e(TAG, "touch service not available");
            return;
        }
        try {
            int result = iTouchscreen.hwSetFeatureConfig(4, Integer.toString(reason));
            Slog.d(TAG, "finish hwSetFeatureConfig with result: " + result + " reasonString: " + Integer.toString(reason));
        } catch (RemoteException e) {
            Log.e(TAG, "updateAudioStatus RemoteException");
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

    /* access modifiers changed from: private */
    public void lockScreen(boolean lock) {
        if (HwPCUtils.isInWindowsCastMode()) {
            if (!lock || !isKeyguardLocked() || !isKeyguardShowingAndNotOccluded()) {
                sHwWindowsCastManager.sendHideViewMsg(1);
            } else {
                sHwWindowsCastManager.sendShowViewMsg(1);
            }
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
        int[] actions;
        for (int i : new int[]{0, 1}) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, i, 82, 0, 0, -1, 0, 268435464, 257), 0);
        }
    }

    /* access modifiers changed from: protected */
    public void launchAssistAction(String hint, int deviceId) {
        if (!checkPackageInstalled("com.google.android.googlequicksearchbox")) {
            sendCloseSystemWindows();
            boolean enableVoiceAssistant = true;
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "hw_long_home_voice_assistant", 0) != 1) {
                enableVoiceAssistant = false;
            }
            if (IS_LONG_HOME_VASSITANT && enableVoiceAssistant) {
                performHapticFeedbackLw(null, 0, false, "launchAssistAction");
                String intent = "android.intent.action.ASSIST";
                try {
                    if (checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE) || checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE_OVERSEA)) {
                        intent = VOICE_ASSISTANT_ACTION;
                    }
                    this.mContext.startActivity(new Intent(intent).setFlags(268435456));
                } catch (ActivityNotFoundException anfe) {
                    Slog.w(TAG, "No activity to handle voice assistant action.", anfe);
                }
            }
        } else {
            HwPhoneWindowManager.super.launchAssistAction(hint, deviceId);
        }
    }

    /* access modifiers changed from: private */
    public boolean checkPackageInstalled(String packageName) {
        try {
            this.mContext.getPackageManager().getPackageInfo(packageName, 128);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isFMActive() {
        return 1 == AudioSystem.getDeviceConnectionState(HighBitsCompModeID.MODE_COLOR_ENHANCE, "");
    }

    /* access modifiers changed from: package-private */
    public boolean isMusicActive() {
        if (((AudioManager) this.mContext.getSystemService("audio")) != null) {
            return AudioSystem.isStreamActive(3, 0);
        }
        Log.w(TAG, "isMusicActive: couldn't get AudioManager reference");
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    /* access modifiers changed from: package-private */
    public void handleVolumeKey(int stream, int keycode) {
        PowerManager.WakeLock wakeLock;
        int i;
        IAudioService audioService = getAudioService();
        if (audioService != null) {
            try {
                if (this.mBroadcastWakeLock != null) {
                    this.mBroadcastWakeLock.acquire();
                }
                if (keycode == 24) {
                    i = 1;
                } else {
                    i = -1;
                }
                audioService.adjustStreamVolume(stream, i, 0, this.mContext.getOpPackageName());
                wakeLock = this.mBroadcastWakeLock;
                if (wakeLock == null) {
                    return;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "IAudioService.adjust*StreamVolume() threw RemoteException");
                wakeLock = this.mBroadcastWakeLock;
                if (wakeLock == null) {
                    return;
                }
            } catch (Throwable th) {
                PowerManager.WakeLock wakeLock2 = this.mBroadcastWakeLock;
                if (wakeLock2 != null) {
                    wakeLock2.release();
                }
                throw th;
            }
            wakeLock.release();
        }
    }

    private void sendVolumeDownKeyPressed() {
        this.mHandler.postDelayed(this.mHandleVolumeDownKey, 500);
    }

    /* access modifiers changed from: private */
    public void cancelVolumeDownKeyPressed() {
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

    /* access modifiers changed from: private */
    public void initQuickcall() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(HIVOICE_PRESS_TYPE_POWER);
        PowerManager powerManager = this.mPowerManager;
        if (powerManager != null) {
            this.mBroadcastWakeLock = powerManager.newWakeLock(1, "HwPhoneWindowManager.mBroadcastWakeLock");
            this.mVolumeDownWakeLock = this.mPowerManager.newWakeLock(1, "HwPhoneWindowManager.mVolumeDownWakeLock");
            this.mBlePowerKeyWakeLock = this.mPowerManager.newWakeLock(1, "HwPhoneWindowManager.mBlePowerKeyWakeLock");
        }
        this.mHeadless = "1".equals(SystemProperties.get("ro.config.headless", "0"));
    }

    private void notifyRapidCaptureService(String command) {
        if (this.mSystemReady) {
            if (!SUPPORT_RAPID_CAPTURE) {
                Slog.i(TAG, "not support rapid capture");
            } else if (isTvMode()) {
                Slog.d(TAG, "Rapid capture not support on TV");
            } else {
                HwExtDisplaySizeUtil extDisplayInstance = HwExtDisplaySizeUtil.getInstance();
                if (!(extDisplayInstance != null ? extDisplayInstance.hasSideInScreen() : false)) {
                    Intent intent = new Intent(HUAWEI_RAPIDCAPTURE_START_MODE);
                    intent.setPackage("com.huawei.camera");
                    intent.putExtra("command", command);
                    try {
                        this.mContext.startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    } catch (Exception e) {
                        Slog.e(TAG, "unable to start service:" + intent, e);
                    }
                    PowerManager.WakeLock wakeLock = this.mVolumeDownWakeLock;
                    if (wakeLock != null) {
                        wakeLock.acquire(500);
                    }
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        Log.i(TAG, "start Rapid Capture Service, command:" + extras.get("command"));
                    }
                }
            }
        }
    }

    public void showHwTransientBars() {
        this.mDefaultDisplayPolicy.getStatusBar();
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
            throw th;
        }
        reply.recycle();
        data.recycle();
    }

    /* access modifiers changed from: protected */
    public void transactToStatusBarService(int code, String transactName, int isEmuiStyle, int statusbarColor, int navigationBarColor, int isEmuiLightStyle) {
        IBinder statusBarServiceBinder;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (!(getHWStatusBarService() == null || (statusBarServiceBinder = getHWStatusBarService().asBinder()) == null)) {
                Log.d(TAG, "Transact:" + transactName + " to status bar service");
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                data.writeInt(isEmuiStyle);
                data.writeInt(statusbarColor);
                data.writeInt(navigationBarColor);
                data.writeInt(isEmuiLightStyle);
                statusBarServiceBinder.transact(code, data, reply, 0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transactToStatusBarService->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
        reply.recycle();
        data.recycle();
    }

    public void updateSystemUiColorLw(WindowState win) {
        boolean colorChanged;
        if (win != null) {
            if (isCoverWindow(win)) {
                Slog.i(TAG, "updateSystemUiColorLw isCoverWindow return " + win);
                return;
            }
            WindowManager.LayoutParams attrs = win.getAttrs();
            if (this.mLastColorWin != win || this.mLastStatusBarColor != attrs.statusBarColor || this.mLastNavigationBarColor != attrs.navigationBarColor) {
                boolean isFloating = getFloatingValue(attrs.isEmuiStyle);
                boolean isPopup = attrs.type == 1000 || attrs.type == 1002 || attrs.type == 2009 || attrs.type == 2010 || attrs.type == 2003;
                if (attrs.type == 3) {
                }
                boolean isTouchExplrEnabled = this.mAccessibilityManager.isTouchExplorationEnabled();
                int isEmuiStyle = getEmuiStyleValue(attrs.isEmuiStyle);
                int statusBarColor = attrs.statusBarColor;
                int navigationBarColor = attrs.navigationBarColor;
                if (!isTouchExplrEnabled) {
                    colorChanged = (this.mLastStatusBarColor == statusBarColor && this.mLastNavigationBarColor == navigationBarColor) ? false : true;
                } else {
                    colorChanged = isTouchExplrEnabled != this.mIsTouchExplrEnabled;
                    isEmuiStyle = -2;
                }
                boolean styleChanged = isEmuiStyleChanged(isEmuiStyle);
                boolean ignoreWindow = (win == this.mDefaultDisplayPolicy.getStatusBar() || attrs.type == 2024 || isKeyguardHostWindow(attrs)) || isFloating || isPopup || (attrs.type == 2034) || (win.getWindowingMode() != 3 && win.inMultiWindowMode());
                boolean changed = (styleChanged && !ignoreWindow) || (!styleChanged && !ignoreWindow && colorChanged);
                if (!ignoreWindow) {
                    win.setCanCarryColors(true);
                }
                this.mLastNavigationBarColor = isTouchExplrEnabled ? -16777216 : navigationBarColor;
                if (changed) {
                    this.mLastStatusBarColor = isTouchExplrEnabled ? -16777216 : statusBarColor;
                    this.mLastIsEmuiStyle = isEmuiStyle;
                    this.mIsTouchExplrEnabled = isTouchExplrEnabled;
                    this.mLastColorWin = win;
                    Slog.v(TAG, "updateSystemUiColorLw window=" + win + ",EmuiStyle=" + isEmuiStyle + ",StatusBarColor=0x" + Integer.toHexString(statusBarColor) + ",NavigationBarColor=0x" + Integer.toHexString(navigationBarColor) + ", mForceNotchStatusBar=" + getForceNotchStatusBar());
                    this.mHandler.post(new Runnable() {
                        /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass21 */

                        public void run() {
                            if ((!HwPhoneWindowManager.this.mIsNotchSwitchOpen && !HwPhoneWindowManager.this.getForceNotchStatusBar() && !HwPhoneWindowManager.this.mIsForceSetStatusBar) || HwPhoneWindowManager.this.mIsNotchSwitchOpen) {
                                if (HwPhoneWindowManager.this.mLastIsEmuiStyle == -1) {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        Slog.v(HwPhoneWindowManager.TAG, "InterruptedException is happed in method updateSystemUiColorLw");
                                    }
                                }
                                HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                                hwPhoneWindowManager.transactToStatusBarService(106, "setSystemUIColor", hwPhoneWindowManager.mLastIsEmuiStyle, HwPhoneWindowManager.this.mLastStatusBarColor, HwPhoneWindowManager.this.mLastNavigationBarColor, -1);
                            }
                        }
                    });
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public int getEmuiStyleValue(int styleValue) {
        if (styleValue == -1) {
            return -1;
        }
        return Integer.MAX_VALUE & styleValue;
    }

    /* access modifiers changed from: protected */
    public boolean isEmuiStyleChanged(int isEmuiStyle) {
        return this.mLastIsEmuiStyle != isEmuiStyle;
    }

    /* access modifiers changed from: protected */
    public boolean getFloatingValue(int styleValue) {
        return styleValue != -1 && (styleValue & FLOATING_MASK) == FLOATING_MASK;
    }

    public void onTouchExplorationStateChanged(boolean enabled) {
    }

    /* access modifiers changed from: protected */
    public void hwInit() {
        this.mAccessibilityManager.addTouchExplorationStateChangeListener(this);
    }

    /* access modifiers changed from: package-private */
    public IStatusBarService getHWStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    private class PolicyHandlerEx extends Handler {
        private PolicyHandlerEx() {
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != HwPhoneWindowManager.MSG_FREEZE_POWER_KEY) {
                switch (i) {
                    case 103:
                        HwPhoneWindowManager.this.enableFingerPrintActions();
                        return;
                    case 104:
                        HwPhoneWindowManager.this.disableFingerPrintActions();
                        return;
                    case 105:
                        HwPhoneWindowManager.this.enableSwingMotion();
                        return;
                    case 106:
                        HwPhoneWindowManager.this.disableSwingMotion();
                        return;
                    case 107:
                        HwPhoneWindowManager.this.finishSetFoldMode(msg.arg1, msg.arg2);
                        return;
                    case 108:
                        HwPhoneWindowManager.this.finishScreenTurningOnEx();
                        return;
                    case 109:
                        if (!HwPhoneWindowManager.this.isTvMode()) {
                            return;
                        }
                        if (msg.obj instanceof Boolean) {
                            HwPhoneWindowManager.this.noticePowerKeyPressed(((Boolean) msg.obj).booleanValue());
                            return;
                        } else {
                            Log.e(HwPhoneWindowManager.TAG, "MSG_POWER_KEY_PRESS msg.obj invalid.");
                            return;
                        }
                    case 110:
                        if (!HwPhoneWindowManager.this.isTvMode()) {
                            return;
                        }
                        if (msg.obj instanceof Boolean) {
                            HwPhoneWindowManager.this.noticeVoiceAssistKeyPressed(((Boolean) msg.obj).booleanValue());
                            return;
                        } else {
                            Log.e(HwPhoneWindowManager.TAG, "MSG_VOICE_ASSIST_KEY_PRESS msg.obj invalid.");
                            return;
                        }
                    default:
                        switch (i) {
                            case HwPhoneWindowManager.MSG_TRIKEY_BACK_LONG_PRESS /*{ENCODED_INT: 4097}*/:
                                HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                                hwPhoneWindowManager.mBackTrikeyHandled = true;
                                if (hwPhoneWindowManager.mTrikeyNaviMode == 1) {
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
                            case HwPhoneWindowManager.MSG_TRIKEY_RECENT_LONG_PRESS /*{ENCODED_INT: 4098}*/:
                                HwPhoneWindowManager hwPhoneWindowManager2 = HwPhoneWindowManager.this;
                                hwPhoneWindowManager2.mRecentTrikeyHandled = true;
                                if (hwPhoneWindowManager2.mTrikeyNaviMode == 0) {
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
                            case HwPhoneWindowManager.MSG_BUTTON_LIGHT_TIMEOUT /*{ENCODED_INT: 4099}*/:
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
                            case HwPhoneWindowManager.MSG_NOTIFY_FINGER_OPTICAL /*{ENCODED_INT: 4100}*/:
                                HwPhoneWindowManager.this.notifyFingerOptical();
                                return;
                            default:
                                return;
                        }
                }
            } else {
                Log.d(HwPhoneWindowManager.TAG, "Emergency power FreezePowerkey timeout.");
                boolean unused = HwPhoneWindowManager.this.mIsFreezePowerkey = false;
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyFingerOptical() {
        Log.i(TAG, "system ready, register pointer event listenr for UD Optical fingerprint");
        this.mFingerprintHardwareType = FingerprintManagerEx.getHardwareType();
        if (this.mFingerprintHardwareType == 1) {
            this.mWindowManagerFuncs.registerPointerEventListener(new WindowManagerPolicyConstants.PointerEventListener() {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass22 */

                public void onPointerEvent(MotionEvent motionEvent) {
                    if (motionEvent.getActionMasked() == 1) {
                        FingerViewController.getInstance(HwPhoneWindowManager.this.mContext).notifyTouchUp(motionEvent.getRawX(), motionEvent.getRawY());
                    } else if (motionEvent.getActionMasked() == 6) {
                        int actionIndex = motionEvent.getActionIndex();
                        FingerViewController.getInstance(HwPhoneWindowManager.this.mContext).notifyTouchUp(motionEvent.getX(actionIndex), motionEvent.getY(actionIndex));
                    }
                }
            }, 0);
        }
    }

    public boolean checkPhoneOFFHOOK() {
        int callState = ((TelephonyManager) this.mContext.getSystemService("phone")).getCallState();
        Log.i(TAG, "callState : " + callState);
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
        Log.i(TAG, "checkHeadSetIsConnected : " + headSetConnectedState);
        return headSetConnectedState;
    }

    /* access modifiers changed from: private */
    public void registerBuoyListener() {
        if (this.mGameDockGesture == null || !HwGameDockGesture.isGameDockGestureFeatureOn()) {
            StringBuilder sb = new StringBuilder();
            sb.append("not Support: ");
            HwGameDockGesture hwGameDockGesture = this.mGameDockGesture;
            sb.append(HwGameDockGesture.isGameDockGestureFeatureOn());
            Log.i("HwGameDockGesture", sb.toString());
            return;
        }
        this.mGameDockGesture.enableGameDockGesture(true);
    }

    /* access modifiers changed from: private */
    public void unRegisterBuoyListener() {
        if (this.mGameDockGesture != null && HwGameDockGesture.isGameDockGestureFeatureOn()) {
            this.mGameDockGesture.enableGameDockGesture(false);
        }
    }

    public void screenTurningOn(ScreenOnListener screenOnListener) {
        EasyWakeUpManager mWakeUpManager;
        HwPhoneWindowManager.super.screenTurningOn(screenOnListener);
        if (this.mContext == null) {
            Log.i(TAG, "Context object is null.");
            return;
        }
        setProximitySensorEnabled(true);
        HwFalseTouchMonitor hwFalseTouchMonitor = this.mFalseTouchMonitor;
        if (hwFalseTouchMonitor != null && hwFalseTouchMonitor.isFalseTouchFeatureOn() && !this.mScreenOnForFalseTouch) {
            this.mScreenOnForFalseTouch = true;
            this.mWindowManagerFuncs.registerPointerEventListener(this.mFalseTouchMonitor.getEventListener(), 0);
        }
        if (mIsHwEasyWakeup && this.mSystemReady && (mWakeUpManager = EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate)) != null) {
            mWakeUpManager.turnOffSensorListener();
        }
    }

    public void screenTurnedOn() {
        HwPhoneWindowManager.super.screenTurnedOn();
        FaceReportEventToIaware.reportEventToIaware(this.mContext, 20023);
        if (this.mSystemReady) {
            Slog.i(TAG, "UL_PowerscreenTurnedOn");
            if (this.mBooted) {
                PickUpWakeScreenManager.getInstance().enablePickupMotionOrNot(false);
            }
            if (isAcquireProximityLock()) {
                this.mHwScreenOnProximityLock.acquireLock(this, 0);
                this.mHwScreenOnProximityLock.registerDeviceListener();
            }
            HwSideStatusManager hwSideStatusManager = this.mSideStatusManager;
            if (hwSideStatusManager != null) {
                hwSideStatusManager.unregisterAudioPlaybackListener(this.mAudioPlaybackCallback);
            }
            HwFoldScreenManagerInternal hwFoldScreenManagerInternal = this.mFoldScreenManagerService;
            if (hwFoldScreenManagerInternal != null) {
                hwFoldScreenManagerInternal.notifyScreenOn();
            }
            initBehaviorCollector();
        }
    }

    public void screenTurnedOff() {
        EasyWakeUpManager mWakeUpManager;
        HwPhoneWindowManager.super.screenTurnedOff();
        FaceReportEventToIaware.reportEventToIaware(this.mContext, 90023);
        setProximitySensorEnabled(false);
        HwFalseTouchMonitor hwFalseTouchMonitor = this.mFalseTouchMonitor;
        if (hwFalseTouchMonitor != null && hwFalseTouchMonitor.isFalseTouchFeatureOn() && this.mScreenOnForFalseTouch) {
            this.mScreenOnForFalseTouch = false;
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.mFalseTouchMonitor.getEventListener(), 0);
        }
        HwScreenOnProximityLock hwScreenOnProximityLock = this.mHwScreenOnProximityLock;
        if (hwScreenOnProximityLock != null) {
            hwScreenOnProximityLock.releaseLock(1);
            this.mHwScreenOnProximityLock.unregisterDeviceListener();
        }
        if (mIsHwEasyWakeup && this.mSystemReady && (mWakeUpManager = EasyWakeUpManager.getInstance(this.mContext, this.mHandler, this.mKeyguardDelegate)) != null) {
            mWakeUpManager.turnOnSensorListener();
        }
        try {
            if (this.mIHwWindowCallback != null) {
                this.mIHwWindowCallback.screenTurnedOff();
            }
        } catch (Exception ex) {
            Log.w(TAG, "mIHwWindowCallback threw RemoteException", ex);
        }
        if (this.mSystemReady) {
            this.mScreenOffTime = SystemClock.uptimeMillis();
            Slog.i(TAG, "UL_PowerscreenTurnedOff");
            PickUpWakeScreenManager.getInstance().enablePickupMotionOrNot(true);
            HwSideStatusManager hwSideStatusManager = this.mSideStatusManager;
            if (hwSideStatusManager != null) {
                this.mIsAudioPlaying = hwSideStatusManager.isAudioPlaybackActive();
                updateAudioStatus(this.mIsAudioPlaying);
                this.mSideStatusManager.registerAudioPlaybackListener(this.mAudioPlaybackCallback, (Handler) null);
            }
            HwFoldScreenManagerInternal hwFoldScreenManagerInternal = this.mFoldScreenManagerService;
            if (hwFoldScreenManagerInternal != null) {
                hwFoldScreenManagerInternal.notifySleep();
            }
        }
    }

    public void onConfigurationChanged() {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.onConfigurationChanged();
        }
    }

    public void setRotationLw(int rotation) {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.onRotationChanged(rotation);
        }
    }

    public void notifyDispalyModeChangeBefore(int displayMode) {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.handleDisplayModeChangeBefore(displayMode);
        }
    }

    public void updateAppView(RemoteViews remoteViews) {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.updateAppView(remoteViews, "");
        }
    }

    public void removeAppView(boolean isNeedAddBtnView) {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.removeAppView(isNeedAddBtnView);
        }
    }

    public int getRemoteViewsPid() {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            return subScreenViewEntry.getRemoteViewsPid();
        }
        return -1;
    }

    public void handleAppDiedForRemoteViews() {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.handleAppDiedForRemoteViews();
        }
    }

    public void handleCloseMobileViewChanged(boolean closeMobileView) {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.handleCloseMobileViewChanged(closeMobileView);
        }
    }

    public void notifyUpdateAftPolicy(int ownerPid, int mode) {
        SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
        if (subScreenViewEntry != null) {
            subScreenViewEntry.notifyUpdateAftPolicy(ownerPid, mode);
        }
    }

    public boolean isGestureIsolated() {
        WindowState win = this.mDefaultDisplayPolicy.getFocusedWindow() != null ? this.mDefaultDisplayPolicy.getFocusedWindow() : this.mDefaultDisplayPolicy.getTopFullscreenOpaqueWindowState();
        if (win == null || (win.getAttrs().hwFlags & 512) != 512) {
            return false;
        }
        return true;
    }

    public void requestTransientStatusBars() {
        getDefaultDisplayPolicy().requestTransientStatusBars();
    }

    public boolean isTopIsFullscreen() {
        WindowState focusWindow = this.mDefaultDisplayPolicy.getFocusedWindow();
        if (focusWindow == null || focusWindow.getAttrs() == null) {
            return this.mDefaultDisplayPolicy.isTopIsFullscreen();
        }
        return ((focusWindow.getAttrs().flags & 1024) == 0 && (this.mDefaultDisplayPolicy.getLastSystemUiFlags() & 4) == 0) ? false : true;
    }

    public boolean okToShowTransientBar() {
        return this.mDefaultDisplayPolicy.checkShowTransientBarLw();
    }

    private void turnOnSensorListener() {
        if (this.mSensorManager == null) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        }
        if (this.mCoverManager == null) {
            this.mCoverManager = new CoverManager();
        }
        CoverManager coverManager = this.mCoverManager;
        if (coverManager != null) {
            this.mCoverOpen = coverManager.isCoverOpen();
        }
        boolean touchDisableModeOpen = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_TOUCH_DISABLE_MODE, 1, -2) == 1;
        if (this.mCoverOpen && !this.mSensorRegisted && this.mListener != null && touchDisableModeOpen) {
            Log.i(TAG, "turnOnSensorListener, registerListener");
            SensorManager sensorManager = this.mSensorManager;
            sensorManager.registerListener(this.mListener, sensorManager.getDefaultSensor(8), 0);
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
        Log.i(TAG, "setHwWindowCallback=" + hwWindowCallback);
        this.mIHwWindowCallback = hwWindowCallback;
    }

    public IHwWindowCallback getHwWindowCallback() {
        return this.mIHwWindowCallback;
    }

    private class ProximitySensorListener implements SensorEventListener {
        public ProximitySensorListener() {
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent arg0) {
            float[] its = arg0.values;
            if (its != null && arg0.sensor.getType() == 8 && its.length > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("sensor value: its[0] = ");
                boolean z = false;
                sb.append(its[0]);
                Log.i(HwPhoneWindowManager.TAG, sb.toString());
                HwPhoneWindowManager hwPhoneWindowManager = HwPhoneWindowManager.this;
                if (its[0] >= 0.0f && its[0] < HwPhoneWindowManager.TYPICAL_PROXIMITY_THRESHOLD) {
                    z = true;
                }
                boolean unused = hwPhoneWindowManager.mIsProximity = z;
            }
        }
    }

    public void updateSettings() {
        Flog.i(1503, "updateSettings");
        HwPhoneWindowManager.super.updateSettings();
        setNaviBarState();
        updateSwingMotionState();
    }

    public void enableScreenAfterBoot() {
        HwPhoneWindowManager.super.enableScreenAfterBoot();
        this.mBooted = true;
        enableSystemWideAfterBoot(this.mContext);
        enableFingerPrintActionsAfterBoot(this.mContext);
        enableStylusAfterBoot(this.mContext);
        if (HwExtDisplaySizeUtil.getInstance().hasSideInScreen()) {
            initSideConfigCallback();
            HwSideTouchManager.getInstance(this.mContext).systemReady(true);
            this.mSideStatusManager = HwSideStatusManager.getInstance(this.mContext);
            initAudioCallback();
            this.mSideVibrationManager = HwSideVibrationManager.getInstance(this.mContext);
        }
    }

    public WindowState getFocusedWindow() {
        return this.mDefaultDisplayPolicy.getFocusedWindow();
    }

    public WindowState getInputMethodWindow() {
        return this.mDefaultDisplayPolicy.getInputMethodWindow();
    }

    public WindowState getNavigationBar() {
        return this.mDefaultDisplayPolicy.getNavigationBar();
    }

    public int getRestrictedScreenHeight() {
        return getDefaultDisplayPolicy().getRestrictedScreenHeight();
    }

    private void enableStylusAfterBoot(Context context) {
        if (HwStylusUtils.hasStylusFeature(context)) {
            Log.i(TAG, "enable stylus gesture feature.");
            this.mHandler.post(new Runnable() {
                /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass24 */

                public void run() {
                    HwPhoneWindowManager.this.enableStylusAction();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void enableStylusAction() {
        if (this.mStylusGestureListener == null) {
            this.mStylusGestureListener = new StylusGestureListener(this.mContext, this);
            this.mWindowManagerFuncs.registerPointerEventListener(this.mStylusGestureListener, 0);
        }
    }

    public boolean isNavigationBarVisible() {
        return this.mDefaultDisplayPolicy.hasNavigationBar() && this.mDefaultDisplayPolicy.getNavigationBar() != null && this.mDefaultDisplayPolicy.getNavigationBar().isVisibleLw();
    }

    /* access modifiers changed from: protected */
    public void enableFingerPrintActions() {
        Log.d(TAG, "enableFingerPrintActions()");
        if (this.fingerprintActionsListener != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.fingerprintActionsListener, 0);
            this.fingerprintActionsListener.destroySearchPanelView();
            this.fingerprintActionsListener.destroyMultiWinArrowView();
            this.fingerprintActionsListener = null;
        }
        this.fingerprintActionsListener = new FingerprintActionsListener(this.mContext, this);
        this.mWindowManagerFuncs.registerPointerEventListener(this.fingerprintActionsListener, 0);
        this.fingerprintActionsListener.createSearchPanelView();
        this.fingerprintActionsListener.createMultiWinArrowView();
    }

    /* access modifiers changed from: protected */
    public void disableFingerPrintActions() {
        if (this.fingerprintActionsListener != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.fingerprintActionsListener, 0);
            this.fingerprintActionsListener.destroySearchPanelView();
            this.fingerprintActionsListener.destroyMultiWinArrowView();
            this.fingerprintActionsListener = null;
        }
    }

    /* access modifiers changed from: private */
    public void enableSwingMotion() {
        Log.i(TAG, "enableSwingMotion");
        this.mWindowManagerFuncs.registerPointerEventListener(this.mSwingMotionPointerEventListener, 0);
    }

    /* access modifiers changed from: private */
    public void disableSwingMotion() {
        Log.i(TAG, "disableSwingMotion");
        this.mWindowManagerFuncs.unregisterPointerEventListener(this.mSwingMotionPointerEventListener, 0);
    }

    private void updateSwingMotionState() {
        boolean isSwingMotionEnable = false;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "item_space_sliding_switch", 0, -2) == 1) {
            isSwingMotionEnable = true;
        }
        if (isSwingMotionEnable != this.mIsSwingMotionEnable) {
            this.mHandlerEx.sendEmptyMessage(isSwingMotionEnable ? 105 : 106);
            this.mIsSwingMotionEnable = isSwingMotionEnable;
        }
    }

    /* access modifiers changed from: protected */
    public void enableFingerPrintActionsAfterBoot(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        this.mHandler.post(new Runnable() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass26 */

            public void run() {
                if (!FrontFingerPrintSettings.isNaviBarEnabled(resolver) || (FrontFingerPrintSettings.isSingleVirtualNavbarEnable(resolver) && !FrontFingerPrintSettings.isSingleNavBarAIEnable(resolver))) {
                    HwPhoneWindowManager.this.enableFingerPrintActions();
                } else {
                    HwPhoneWindowManager.this.disableFingerPrintActions();
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void filmStateReporter() {
        this.mFilmState = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), FILM_STATE, -1, UserHandle.myUserId());
        this.mWindowManagerFuncs.registerPointerEventListener(new WindowManagerPolicyConstants.PointerEventListener() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass27 */

            public void onPointerEvent(MotionEvent motionEvent) {
                if (motionEvent.getAction() == 0) {
                    int currentFilmState = (int) motionEvent.getAxisValue(47);
                    if (currentFilmState != 1 && currentFilmState != 2) {
                        return;
                    }
                    if (HwPhoneWindowManager.this.mFilmState != currentFilmState) {
                        HwPhoneWindowManager.this.reportFilmState(currentFilmState);
                        int unused = HwPhoneWindowManager.this.mFilmState = currentFilmState;
                        Settings.Secure.putIntForUser(HwPhoneWindowManager.this.mContext.getContentResolver(), HwPhoneWindowManager.FILM_STATE, currentFilmState, UserHandle.myUserId());
                    } else if (SystemClock.uptimeMillis() - HwPhoneWindowManager.this.mLastFilmReportTimeMs >= 86400000) {
                        HwPhoneWindowManager.this.reportFilmState(currentFilmState);
                    }
                }
            }
        }, 0);
    }

    /* access modifiers changed from: private */
    public void reportFilmState(int currentFilmState) {
        String filmStateString = "{filmState:" + currentFilmState + "}";
        Slog.i(TAG, "filmStateString:" + filmStateString);
        Reporter.e(this.mContext, 210, filmStateString);
        this.mLastFilmReportTimeMs = SystemClock.uptimeMillis();
    }

    /* access modifiers changed from: protected */
    public void setNaviBarState() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean navibarEnable = FrontFingerPrintSettings.isNaviBarEnabled(resolver);
        boolean singleNavBarEnable = FrontFingerPrintSettings.isSingleVirtualNavbarEnable(resolver);
        boolean singleNavBarAiEnable = FrontFingerPrintSettings.isSingleNavBarAIEnable(resolver);
        boolean navibarEnable2 = false;
        boolean singEnarAiEnable = singleNavBarEnable && !singleNavBarAiEnable;
        if (singEnarAiEnable || !navibarEnable) {
            navibarEnable2 = true;
        }
        Log.d(TAG, "setNaviBarState()--navibarEnable:" + navibarEnable2 + ";mNavibarEnabled:" + this.mNavibarEnabled + ";singleNavBarEnable:" + singleNavBarEnable + ";singleNavBarAiEnable:" + singleNavBarAiEnable + ";singEnarAiEnable:" + singEnarAiEnable);
        int i = 103;
        if (!this.mNaviBarStateInited) {
            if (this.mBooted) {
                this.mNavibarEnabled = navibarEnable2;
                Handler handler = this.mHandlerEx;
                if (!navibarEnable2) {
                    i = 104;
                }
                handler.sendEmptyMessage(i);
                this.mNaviBarStateInited = true;
            }
        } else if (this.mNavibarEnabled != navibarEnable2) {
            Log.d(TAG, "setNaviBarState()--" + this.mNavibarEnabled);
            this.mNavibarEnabled = navibarEnable2;
            Handler handler2 = this.mHandlerEx;
            if (!navibarEnable2) {
                i = 104;
            }
            handler2.sendEmptyMessage(i);
        }
    }

    /* access modifiers changed from: protected */
    public void updateSplitScreenView() {
        FingerprintActionsListener fingerprintActionsListener2;
        if ((!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer()) && (fingerprintActionsListener2 = this.fingerprintActionsListener) != null) {
            fingerprintActionsListener2.createMultiWinArrowView();
        }
    }

    public DexClassLoader getDexClassLoader() {
        if (sDexClassLoader == null) {
            sDexClassLoader = new DexClassLoader(FINGERSENSE_JAR_PATH, null, null, this.mContext.getClassLoader());
        }
        return sDexClassLoader;
    }

    private void loadFingerSenseManager() {
        try {
            this.mFsManagerCls = getDexClassLoader().loadClass("com.huawei.fingersense.HwFingerSenseManager");
            this.mFsManagerObj = this.mFsManagerCls.getConstructor(Context.class, WindowManagerFuncs.class).newInstance(this.mContext, this.mWindowManagerFuncs);
            Log.i(TAG, "loadFingerSenseManager success");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "loadFingerSenseManager error : " + e);
        } catch (NoSuchMethodException e2) {
            Log.e(TAG, "loadFingerSenseManager error : " + e2);
        } catch (IllegalAccessException e3) {
            Log.e(TAG, "loadFingerSenseManager error : " + e3);
        } catch (InstantiationException e4) {
            Log.e(TAG, "loadFingerSenseManager error : " + e4);
        } catch (InvocationTargetException e5) {
            Log.e(TAG, "loadFingerSenseManager error : " + e5);
        }
    }

    /* access modifiers changed from: protected */
    public void enableSystemWideAfterBoot() {
        Class cls = this.mFsManagerCls;
        if (cls == null || this.mFsManagerObj == null) {
            Log.e(TAG, "enableSystemWideAfterBoot fingersense object load fail");
            return;
        }
        try {
            cls.getMethod("enableSystemWideAfterBoot", new Class[0]).invoke(this.mFsManagerObj, new Object[0]);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "enableSystemWideAfterBoot error : " + e);
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "enableSystemWideAfterBoot error : " + e2);
        } catch (InvocationTargetException e3) {
            Log.e(TAG, "enableSystemWideAfterBoot error : " + e3);
        }
    }

    public void processing_KEYCODE_SOUNDTRIGGER_EVENT(int keyCode, Context context, boolean isMusicOrFMActive, boolean down, boolean keyguardShow) {
        Log.d(TAG, "intercept DSP WAKEUP EVENT" + keyCode + " down=" + down + " keyguardShow=" + keyguardShow);
        ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        switch (keyCode) {
            case AwareJobSchedulerService.MSG_JOB_EXPIRED:
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
                } else {
                    return;
                }
            case AwareJobSchedulerService.MSG_CHECK_JOB:
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
                } else {
                    return;
                }
            case AwareJobSchedulerService.MSG_REMOVE_JOB:
            case AwareJobSchedulerService.MSG_CONTROLLER_CHANGED:
            default:
                return;
        }
    }

    private boolean isTOPActivity(String appnames) {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningTasks(1);
            if (tasks != null) {
                if (!tasks.isEmpty()) {
                    for (ActivityManager.RunningTaskInfo info : tasks) {
                        Log.i(TAG, "info.topActivity.getPackageName() is " + info.topActivity.getPackageName());
                        if (info.topActivity.getPackageName().equals(appnames) && info.baseActivity.getPackageName().equals(appnames)) {
                            return true;
                        }
                    }
                    return false;
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
        return !KEYCODE_NOT_FOR_CLOUD.contains(Integer.valueOf(keyCode)) && (IS_TABLET || HwPCUtils.isPcCastModeInServer()) && isCloudOnTOP();
    }

    private boolean isCloudActivityOnTop(ActivityManager.RunningTaskInfo info) {
        return "com.huawei.cloud".equals(info.topActivity.getPackageName()) && "com.huawei.cloud".equals(info.baseActivity.getPackageName()) && "com.huawei.ahdp.session.VmActivity".equals(info.topActivity.getClassName());
    }

    private boolean isCloudOnTOP() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningTasks(1);
            if (tasks != null) {
                if (!tasks.isEmpty()) {
                    for (ActivityManager.RunningTaskInfo info : tasks) {
                        if (info.topActivity != null) {
                            if (info.baseActivity != null) {
                                if (isCloudActivityOnTop(info) && (IS_TABLET || HwPCUtils.isPcDynamicStack(info.stackId))) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                    return false;
                }
            }
            return false;
        } catch (RuntimeException e) {
            HwPCUtils.log(TAG, "isCloudOnTOP->RuntimeException happened");
        } catch (Exception e2) {
            HwPCUtils.log(TAG, "isCloudOnTOP->other exception happened");
        }
    }

    private void removeHwVAFromPowerSaveWhitelist() {
        this.mHandler.postDelayed(this.mRemovePowerSaveWhitelistRunnable, 3000);
    }

    private void addHwVAToPowerSaveWhitelist() {
        if (this.mHandler.hasCallbacks(this.mRemovePowerSaveWhitelistRunnable)) {
            this.mHandler.removeCallbacks(this.mRemovePowerSaveWhitelistRunnable);
        }
        try {
            if (checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE)) {
                HwDeviceIdleController.addPowerSaveWhitelistApp(HUAWEI_VASSISTANT_PACKAGE);
            } else if (checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE_OVERSEA)) {
                HwDeviceIdleController.addPowerSaveWhitelistApp(HUAWEI_VASSISTANT_PACKAGE_OVERSEA);
            } else {
                Log.w(TAG, "vassistant not exists");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "add hwvassistant exception!");
        }
    }

    /* access modifiers changed from: private */
    public void notifyVassistantService(String command, int mode, KeyEvent event) {
        Intent intent = new Intent(ACTION_HUAWEI_VASSISTANT_SERVICE);
        intent.putExtra(HUAWEI_VASSISTANT_EXTRA_START_MODE, mode);
        intent.putExtra("command", command);
        if (event != null) {
            intent.putExtra("KeyEvent", event);
        }
        if (checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE)) {
            intent.setPackage(HUAWEI_VASSISTANT_PACKAGE);
        } else if (checkPackageInstalled(HUAWEI_VASSISTANT_PACKAGE_OVERSEA)) {
            intent.setPackage(HUAWEI_VASSISTANT_PACKAGE_OVERSEA);
        } else {
            Log.w(TAG, "vassistant not exists");
            return;
        }
        addHwVAToPowerSaveWhitelist();
        try {
            this.mContext.startService(intent);
        } catch (Exception e) {
            Slog.e(TAG, "unable to start service:" + intent, e);
        }
        removeHwVAFromPowerSaveWhitelist();
        PowerManager.WakeLock wakeLock = this.mVolumeDownWakeLock;
        if (wakeLock != null) {
            wakeLock.acquire(500);
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(TAG, "start VASSISTANT Service, state:" + extras.get(HUAWEI_VASSISTANT_EXTRA_START_MODE) + " command:" + extras.get("command"));
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

    private void notifyScreenPowerChanged(boolean state) {
        Intent intent = new Intent(SCREEN_POWER_CHANGED_ACTION);
        intent.putExtra("flag", state);
        intent.setPackage(PACKAGE_NAME_HWPCASSISTANT);
        intent.addFlags(268435456);
        intent.addFlags(16777216);
        this.mContext.sendBroadcast(intent);
    }

    private void cancelAIPowerLongPressed() {
        this.mHandler.removeCallbacks(this.mAIPowerLongPressed);
        Log.i(TAG, "cancel power long press");
    }

    /* access modifiers changed from: private */
    public void powerOffToast() {
        Toast toast = Toast.makeText(new ContextThemeWrapper(this.mContext, 33947656), this.mContext.getString(33686231, 3), 1);
        toast.getWindowParams().type = TOAST_TYPE_COVER_SCREEN;
        toast.getWindowParams().privateFlags |= 16;
        toast.show();
        this.showPowerOffToastTimes++;
        this.mPowerOffToastShown = true;
        Settings.Secure.putIntForUser(this.mResolver, KEY_TOAST_POWER_OFF, this.showPowerOffToastTimes, ActivityManager.getCurrentUser());
    }

    private void cancelPowerOffToast() {
        this.mHandler.removeCallbacks(this.mPowerOffRunner);
    }

    private void showPowerOffToast(boolean isScreenOn) {
        if (isScreenOn && this.mBooted) {
            if ((!IS_POWER_HIACTION_KEY || !HIVOICE_PRESS_TYPE_POWER.equals(this.mHiVoiceKeyType)) && this.showPowerOffToastTimes < 2) {
                this.mHandler.postDelayed(this.mPowerOffRunner, 1000);
            }
        }
    }

    private void processPowerKey(Context context, int eventAction, int eventCode, long eventDnTime, long eventTime) {
        if (!mIsSidePowerFpComb || !this.mHwPWMEx.isPowerFpForbidGotoSleep()) {
            boolean down = eventAction == 0;
            if (eventCode != 26) {
                Log.i(TAG, "Not POWER Key." + eventCode);
            } else if (down) {
                this.mPowerKeyHandledByHiaction = false;
                this.mHandler.postDelayed(this.mAIPowerLongPressed, (long) this.mPowerLongPressTimeout);
            } else {
                Log.i(TAG, "Power up eventTime " + eventTime + " DnTime " + eventDnTime);
                cancelAIPowerLongPressed();
            }
        } else {
            Log.i(TAG, "Is Side PowerFpComb ");
        }
    }

    private void notifyHomoAiKeyEvent(int homoType, int pressType) {
        Intent intent = new Intent(HUAWEI_HIACTION_ACTION);
        intent.setPackage(HUAWEI_HIACTION_PACKAGE);
        intent.putExtra(HOMOAI_EVENT_TAG, homoType);
        intent.putExtra(HOMOAI_PRESS_TAG, pressType);
        if (homoType == 1) {
            powerPressBDReport(984);
        }
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Log.e(TAG, "start HiAction server err");
        }
        Log.i(TAG, "send HomoAi key " + homoType + " pressType " + pressType);
    }

    /* access modifiers changed from: private */
    public void handleHomoAiKeyLongPress(int pressType) {
        notifyHomoAiKeyEvent(1, pressType);
    }

    private void notifyPowerKeyEventToHiAction(Context context, KeyEvent event) {
        if (HWFLOW) {
            Log.i(TAG, "mHiVoiceKeyType: " + this.mHiVoiceKeyType);
        }
        if (IS_POWER_HIACTION_KEY && HIVOICE_PRESS_TYPE_POWER.equals(this.mHiVoiceKeyType)) {
            processPowerKey(this.mContext, event.getAction(), event.getKeyCode(), event.getDownTime(), event.getEventTime());
        }
    }

    public boolean getNeedDropFingerprintEvent() {
        return this.mNeedDropFingerprintEvent;
    }

    private String getTopActivity() {
        ActivityInfo activityInfo = HwActivityTaskManager.getLastResumedActivity();
        if (activityInfo == null || activityInfo.getComponentName() == null) {
            return null;
        }
        return activityInfo.getComponentName().flattenToShortString();
    }

    private void initDropSmartKey() {
        String dropSmartKeyActivity = SettingsEx.Systemex.getString(this.mResolver, DROP_SMARTKEY_ACTIVITY);
        if (TextUtils.isEmpty(dropSmartKeyActivity)) {
            Log.w(TAG, "dropSmartKeyActivity not been configured in hw_defaults.xml!");
            return;
        }
        for (String str : dropSmartKeyActivity.split(";")) {
            this.needDropSmartKeyActivities.add(str);
        }
    }

    private boolean needDropSmartKey() {
        boolean result = false;
        String topActivityName = getTopActivity();
        HashSet<String> hashSet = this.needDropSmartKeyActivities;
        if (hashSet != null && hashSet.contains(topActivityName)) {
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
        WindowState focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow();
        if (focusedWindow == null || focusedWindow.getAttrs() == null) {
            Log.e(TAG, "isKeyguardShortcutApps, focusedWindow or getAttrs is null");
            return false;
        }
        String focusPackageName = focusedWindow.getAttrs().packageName;
        if (focusPackageName == null) {
            return false;
        }
        int len = this.mKeyguardShortcutApps.length;
        for (int i = 0; i < len; i++) {
            if (focusPackageName.startsWith(this.mKeyguardShortcutApps[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean isLsKeyguardShortcutApps() {
        WindowState focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow();
        if (focusedWindow == null || focusedWindow.getAttrs() == null) {
            Log.e(TAG, "isLsKeyguardShortcutApps, focusedWindow or getAttrs is null");
            return false;
        }
        String focusPackageName = focusedWindow.getAttrs().packageName;
        if (focusPackageName == null) {
            return false;
        }
        int len = this.mLsKeyguardShortcutApps.length;
        for (int i = 0; i < len; i++) {
            if (focusPackageName.startsWith(this.mLsKeyguardShortcutApps[i])) {
                return true;
            }
        }
        return false;
    }

    public void onProximityPositive() {
        HwSideStatusManager hwSideStatusManager;
        Log.i(TAG, "onProximityPositive");
        HwScreenOnProximityLock hwScreenOnProximityLock = this.mHwScreenOnProximityLock;
        if (hwScreenOnProximityLock != null) {
            hwScreenOnProximityLock.releaseLock(1);
            this.mHwScreenOnProximityLock.unregisterDeviceListener();
        }
        if (this.mIsSupportSideScreen && (hwSideStatusManager = this.mSideStatusManager) != null) {
            this.mIsAudioPlaying = hwSideStatusManager.isAudioPlaybackActive();
            updateAudioStatus(this.mIsAudioPlaying);
        }
    }

    private boolean isInCallUIAndRinging() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        return telecomManager != null && telecomManager.isRinging();
    }

    private boolean isAlarm(int user) {
        ComponentName oldCmpName = ComponentName.unflattenFromString("com.android.deskclock/.alarmclock.AlarmKlaxon");
        ComponentName newCmpName = ComponentName.unflattenFromString("com.huawei.deskclock/.alarmclock.AlarmKlaxon");
        HwActivityManagerService hwAms = (HwActivityManagerService) ServiceManager.getService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        return hwAms.serviceIsRunning(oldCmpName, user) || hwAms.serviceIsRunning(newCmpName, user);
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

    /* access modifiers changed from: package-private */
    public boolean isStatusBarObsecuredByWin(WindowState win) {
        if (win == null || this.mDefaultDisplayPolicy.getStatusBar() == null || (win.getAttrs().flags & 16) != 0 || win.toString().contains("hwSingleMode_window")) {
            return false;
        }
        Rect winFrame = win.getFrameLw();
        Rect statusbarFrame = this.mDefaultDisplayPolicy.getStatusBar().getFrameLw();
        if (winFrame.top > statusbarFrame.top || winFrame.bottom < statusbarFrame.bottom || winFrame.left > statusbarFrame.left || winFrame.right < statusbarFrame.right) {
            return false;
        }
        return true;
    }

    public void adjustConfigurationLw(Configuration config, int keyboardPresence, int navigationPresence) {
        HwPhoneWindowManager.super.adjustConfigurationLw(config, keyboardPresence, navigationPresence);
        int tempDpi = config.densityDpi;
        if (tempDpi != this.lastDensityDpi) {
            updateSystemWideConfiguration();
            StylusGestureListener stylusGestureListener = this.mStylusGestureListener;
            if (stylusGestureListener != null) {
                stylusGestureListener.updateConfiguration();
            }
            this.lastDensityDpi = tempDpi;
        }
    }

    private void updateSystemWideConfiguration() {
        Class cls = this.mFsManagerCls;
        if (cls == null || this.mFsManagerObj == null) {
            Log.e(TAG, "updateConfiguration fingersense object load fail");
            return;
        }
        try {
            cls.getMethod("updateConfiguration", new Class[0]).invoke(this.mFsManagerObj, new Object[0]);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "updateSystemWideConfiguration NoSuchMethodException");
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "updateSystemWideConfiguration IllegalAccessException");
        } catch (InvocationTargetException e3) {
            Log.e(TAG, "updateSystemWideConfiguration InvocationTargetException");
        }
    }

    public boolean performHapticFeedbackLw(WindowState win, int effectId, boolean always, String reason) {
        String owningPackage;
        int owningUid;
        if (win != null) {
            owningUid = win.getOwningUid();
            owningPackage = win.getOwningPackage();
        } else {
            owningUid = Process.myUid();
            owningPackage = this.mContext.getOpPackageName();
        }
        return HwPhoneWindowManager.super.performHapticFeedback(owningUid, owningPackage, effectId, always, reason);
    }

    public void setNavibarAlignLeftWhenLand(boolean isLeft) {
        this.mIsNavibarAlignLeftWhenLand = isLeft;
    }

    public boolean getNavibarAlignLeftWhenLand() {
        return this.mIsNavibarAlignLeftWhenLand;
    }

    public boolean isPhoneIdle() {
        if (getTelephonyService() != null) {
            try {
                TelephonyManager.getDefault().isMultiSimEnabled();
            } catch (Exception ex) {
                Log.w(TAG, "ITelephony threw RemoteException", ex);
            }
        }
        return false;
    }

    public int getDisabledKeyEventResult(int keyCode) {
        if (keyCode == 3) {
            HwCustPhoneWindowManager hwCustPhoneWindowManager = this.mCust;
            if ((hwCustPhoneWindowManager == null || !hwCustPhoneWindowManager.disableHomeKey(this.mContext)) && !HwDeviceManager.disallowOp(14)) {
                return -2;
            }
            Log.i(TAG, "the device's home key has been disabled for the user.");
            return 0;
        } else if (keyCode != 4) {
            if (keyCode != 187 || !HwDeviceManager.disallowOp(15)) {
                return -2;
            }
            Log.i(TAG, "the device's task key has been disabled for the user.");
            return 0;
        } else if (!HwDeviceManager.disallowOp(16)) {
            return -2;
        } else {
            Log.i(TAG, "the device's back key has been disabled for the user.");
            return -1;
        }
    }

    private int getGameControlKeyReslut(KeyEvent event) {
        if (!mSupportGameAssist || !FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION) {
            return -2;
        }
        int keyCode = event.getKeyCode();
        boolean isGameKeyControlOn = (!this.mEnableKeyInCurrentFgGameApp && ActivityManagerEx.isGameKeyControlOn()) || this.mLastKeyDownDropped;
        Log.d(TAG, "deviceId:" + event.getDeviceId() + " mFingerPrintId:" + this.mFingerPrintId + " isGameKeyControlOn:" + isGameKeyControlOn + ",EnableKey=" + this.mEnableKeyInCurrentFgGameApp + ",mLastKeyDownDropped=" + this.mLastKeyDownDropped);
        if (!isGameKeyControlOn) {
            return -2;
        }
        if (FrontFingerPrintSettings.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
            if (this.mTrikeyNaviMode < 0) {
                Log.d(TAG, "trikey single mode.");
                return performGameMode(event, 0);
            }
            Log.d(TAG, "trikey three mode.");
            return performGameMode(event, 1);
        } else if (!FrontFingerPrintSettings.isNaviBarEnabled(this.mResolver)) {
            Log.d(TAG, "single key.");
            return performGameMode(event, 2);
        } else if (keyCode != 3 || event.getDeviceId() != this.mFingerPrintId) {
            return -2;
        } else {
            Log.d(TAG, "NaviBarEnabled KEYCODE_HOME !");
            return performGameMode(event, 3);
        }
    }

    private int performGameMode(KeyEvent event, int naviMode) {
        boolean z = true;
        boolean isSingleKeyMode = naviMode == 0 || naviMode == 2;
        int keyCode = event.getKeyCode();
        long keyTime = event.getEventTime();
        boolean isKeyDown = event.getAction() == 0;
        boolean initialDown = isKeyDown && event.getRepeatCount() == 0;
        if (this.mLastKeyDownDropped && initialDown) {
            this.mLastKeyDownDropped = false;
        }
        if (keyCode != 3) {
            if (keyCode != 4) {
                if (keyCode != 187) {
                    return -2;
                }
                if (isSingleKeyMode) {
                    return -1;
                }
                if (isKeyDown) {
                    return -2;
                }
                int result = getClickResult(keyTime, keyCode);
                this.mHandlerEx.removeMessages(MSG_TRIKEY_RECENT_LONG_PRESS);
                return result;
            } else if (initialDown) {
                int result2 = getClickResult(keyTime, keyCode);
                if (result2 == -2) {
                    z = false;
                }
                this.mLastKeyDownDropped = z;
                return result2;
            } else if (!this.mLastKeyDownDropped) {
                return -2;
            } else {
                Log.d(TAG, "drop key up for last event beacause down dropped.");
                if (!isKeyDown) {
                    this.mLastKeyDownDropped = false;
                }
                return -1;
            }
        } else if (!isSingleKeyMode && !isKeyDown) {
            return getClickResult(keyTime, keyCode);
        } else {
            return -2;
        }
    }

    private int getClickResult(long eventTime, int keyCode) {
        int result;
        int i;
        if (!this.mEnableKeyInCurrentFgGameApp) {
            long j = this.mLastKeyDownTime;
            if (eventTime - j < POWER_SOS_MISTOUCH_THRESHOLD && (i = this.mLastKeyDownKeyCode) == keyCode && j - this.mSecondToLastKeyDownTime < POWER_SOS_MISTOUCH_THRESHOLD && this.mSecondToLastKeyDownKeyCode == i) {
                Log.i(TAG, "Navigation keys unlocked.");
                result = -1;
                this.mEnableKeyInCurrentFgGameApp = true;
                showKeyEnableToast();
                Flog.bdReport(this.mContext, 503);
                this.mSecondToLastKeyDownTime = this.mLastKeyDownTime;
                this.mSecondToLastKeyDownKeyCode = this.mLastKeyDownKeyCode;
                this.mLastKeyDownTime = eventTime;
                this.mLastKeyDownKeyCode = keyCode;
                Log.i(TAG, "getClickResult result:" + result + ",keyCode:" + keyCode + ",EnableKey:" + this.mEnableKeyInCurrentFgGameApp);
                return result;
            }
        }
        result = -1;
        this.mSecondToLastKeyDownTime = this.mLastKeyDownTime;
        this.mSecondToLastKeyDownKeyCode = this.mLastKeyDownKeyCode;
        this.mLastKeyDownTime = eventTime;
        this.mLastKeyDownKeyCode = keyCode;
        Log.i(TAG, "getClickResult result:" + result + ",keyCode:" + keyCode + ",EnableKey:" + this.mEnableKeyInCurrentFgGameApp);
        return result;
    }

    private void showKeyEnableToast() {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass32 */

            public void run() {
                Toast toast = Toast.makeText(HwPhoneWindowManager.this.mContext, 33686230, 0);
                toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                toast.getWindowParams().privateFlags |= 16;
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
        Log.d(TAG, "frontFpNaviTriKey:" + frontFpNaviTriKey + " isInjected:" + isInjected + " mTrikeyNaviMode:" + this.mTrikeyNaviMode + " excluded:" + excluded);
        return frontFpNaviTriKey == 0 || (!isInjected && this.mTrikeyNaviMode < 0) || excluded;
    }

    public boolean isSupportCover() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "cover_enabled", 1) != 0;
    }

    public boolean isSmartCoverMode() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "cover_type", 0) == 1;
    }

    public boolean isInCallActivity() {
        String pkgName = getTopActivity();
        return pkgName != null && pkgName.startsWith(HUAWEI_ANDROID_INCALL_UI);
    }

    public boolean isInterceptAndCheckRinging(boolean isIntercept) {
        TelecomManager telecomManager = getTelecommService();
        if (!isIntercept) {
            return false;
        }
        if (telecomManager == null || !telecomManager.isRinging()) {
            return true;
        }
        return false;
    }

    public int getSingAppKeyEventResult(int keyCode) {
        String packageName = HwDeviceManager.getString(34);
        if (packageName == null || packageName.isEmpty()) {
            return -2;
        }
        boolean[] results = isNeedStartSingleApp(packageName);
        if (keyCode == 3) {
            if (!results[0]) {
                Log.i(TAG, "Single app model running, start the single app's main activity.");
                startSingleApp(packageName);
            }
            return 0;
        } else if (keyCode != 4) {
            return -2;
        } else {
            if (results[0]) {
                return -1;
            }
            if (!results[1]) {
                return -2;
            }
            Log.i(TAG, "Single app model running, start the single app's main activity.");
            startSingleApp(packageName);
            return -1;
        }
    }

    private boolean[] isNeedStartSingleApp(String packageName) {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        boolean[] results = {false, false};
        if (activityManager != null) {
            try {
                List<ActivityManager.RunningTaskInfo> runningTask = activityManager.getRunningTasks(2);
                if (runningTask != null && runningTask.size() > 0) {
                    ComponentName cn = runningTask.get(0).topActivity;
                    if (cn != null) {
                        String currentAppName = cn.getPackageName();
                        String currentActivityName = cn.getClassName();
                        PackageManager pm = this.mContext.getPackageManager();
                        String mainClassName = pm.getLaunchIntentForPackage(packageName).resolveActivity(pm).getClassName();
                        if (mainClassName != null && mainClassName.equals(currentActivityName) && packageName.equals(currentAppName)) {
                            results[0] = true;
                        }
                    }
                    String nextAppName = null;
                    if (runningTask.size() > 1) {
                        nextAppName = runningTask.get(1).topActivity.getPackageName();
                    }
                    if ((runningTask.get(0).numActivities <= 1 && (runningTask.size() <= 1 || !packageName.equals(nextAppName))) || FingerViewController.PKGNAME_OF_KEYGUARD.equals(nextAppName)) {
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

    /* access modifiers changed from: protected */
    public void notifyPowerkeyInteractive(boolean bool) {
        AwareFakeActivityRecg.self().notifyPowerkeyInteractive(true);
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        HwPhoneWindowManager.super.dump(prefix, pw, args);
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy != null) {
            gestureNavPolicy.dump(prefix, pw, args);
        }
    }

    private boolean layoutStatusBarExternal(Rect pf, Rect df, Rect of, Rect vf, Rect dcf, DisplayFrames displayFrames) {
        if (this.mDefaultDisplayPolicy.getStatusBar() == null) {
            return false;
        }
        int i = displayFrames.mUnrestricted.left;
        of.left = i;
        df.left = i;
        pf.left = i;
        int i2 = displayFrames.mUnrestricted.right;
        of.right = i2;
        df.right = i2;
        pf.right = i2;
        int i3 = displayFrames.mUnrestricted.top;
        of.top = i3;
        df.top = i3;
        pf.top = i3;
        int i4 = displayFrames.mUnrestricted.bottom;
        of.bottom = i4;
        df.bottom = i4;
        pf.bottom = i4;
        vf.left = displayFrames.mStable.left;
        vf.top = displayFrames.mStable.top;
        vf.right = displayFrames.mStable.right;
        vf.bottom = displayFrames.mStable.bottom;
        this.mDefaultDisplayPolicy.getStatusBar().computeFrameLw();
        return false;
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
        if (!(!HwPCUtils.enabledInPad() || keyCode == 40 || keyCode == 117 || keyCode == 118)) {
            this.isHomeAndLBothPressed = false;
            this.isLDown = false;
        }
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
            } else if (keyCode != 33 || !down || !this.isHomePressDown || repeatCount != 0) {
                if (keyCode == 40) {
                    if (down && this.isHomePressDown && repeatCount == 0) {
                        this.isHomeAndLBothDown = true;
                        if (HwPCUtils.enabledInPad()) {
                            this.isHomeAndLBothPressed = true;
                            this.isLDown = true;
                        } else {
                            lockScreen(true);
                        }
                        return true;
                    } else if (HwPCUtils.enabledInPad() && !down && this.isHomeAndLBothPressed) {
                        this.isLDown = false;
                        if (!this.isHomePressDown) {
                            HwPCUtils.log(TAG, "will turnOffScreen in DesktopMode");
                            turnOffScreenInDeskMode();
                            this.isHomeAndLBothPressed = false;
                        }
                        return true;
                    }
                }
                if (keyCode != 32 || !down || !this.isHomePressDown || repeatCount != 0) {
                    if (down && repeatCount == 0 && keyCode == 61) {
                        if (this.mRecentAppsHeldModifiers == 0 && !keyguardOn() && isUserSetupComplete()) {
                            int shiftlessModifiers = event.getModifiers() & -194;
                            if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2)) {
                                this.mRecentAppsHeldModifiers = shiftlessModifiers;
                                triggerSwitchTaskView(true);
                                return true;
                            }
                        }
                    } else if (!down && this.mRecentAppsHeldModifiers != 0 && (event.getMetaState() & this.mRecentAppsHeldModifiers) == 0) {
                        this.mRecentAppsHeldModifiers = 0;
                        triggerSwitchTaskView(false);
                    }
                    if ((HwPCUtils.enabledInPad() || keyCode != 3) && keyCode != 117 && keyCode != 118) {
                        return false;
                    }
                    if (!down) {
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
                        if (HwPCUtils.enabledInPad() && this.isHomeAndLBothPressed && !this.isLDown) {
                            HwPCUtils.log(TAG, "the home key up, will turnOffScreen in DesktopMode");
                            turnOffScreenInDeskMode();
                            this.isHomeAndLBothPressed = false;
                        }
                    } else {
                        this.isHomePressDown = true;
                    }
                    return true;
                }
                this.isHomeAndDBothDown = true;
                toggleHome();
                return true;
            } else {
                this.isHomeAndEBothDown = true;
                ComponentName componentName = new ComponentName("com.huawei.hidisk", "com.huawei.hidisk.filemanager.FileManager");
                Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
                intent.setFlags(268435456);
                intent.setComponent(componentName);
                this.mContext.createDisplayContext(DisplayManagerGlobal.getInstance().getRealDisplay(displayId)).startActivity(intent);
                return true;
            }
        }
    }

    private boolean handlePadKeyEvent(KeyEvent event) {
        PowerManager powerManager;
        if (HwPCUtils.isPcCastModeInServer() || isKeyguardLocked()) {
            return false;
        }
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        int repeatCount = event.getRepeatCount();
        boolean isMetaPressed = event.isMetaPressed();
        if (!KeyEvent.isMetaKey(keyCode)) {
            this.mCanSearch = false;
            if (!down || repeatCount != 0) {
                return false;
            }
            if (isMetaPressed) {
                if (keyCode == 40 && (powerManager = this.mPowerManager) != null) {
                    powerManager.goToSleep(SystemClock.uptimeMillis());
                    return true;
                } else if (keyCode == 32) {
                    launchHomeFromHotKey(event.getDisplayId());
                    return true;
                }
            }
            if (keyCode == 61 && event.isAltPressed()) {
                try {
                    this.mWindowManager.setInTouchMode(false);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to set touch mode!");
                }
            }
            return false;
        } else if (down) {
            this.mCanSearch = true;
            return false;
        } else if (!this.mCanSearch) {
            return true;
        } else {
            this.mCanSearch = false;
            return false;
        }
    }

    private void turnOffScreenInDeskMode() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService(HIVOICE_PRESS_TYPE_POWER);
        if (powerManager != null && powerManager.isScreenOn() && !isScreenLocked()) {
            powerManager.goToSleep(SystemClock.uptimeMillis());
        }
    }

    private Boolean handleExclusiveKeykoard(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean down = false;
        if (event.getAction() == 0) {
            down = true;
        }
        int repeatCount = event.getRepeatCount();
        if (keyCode != 3) {
            if (keyCode != 4) {
                if (keyCode == 118) {
                    if (down) {
                        dispatchKeyEventForExclusiveKeyboard(event);
                    }
                    return true;
                } else if (keyCode != 187) {
                    if (keyCode != 220 && keyCode != 221) {
                        return false;
                    }
                    if (isScreenLocked()) {
                        HwPCUtils.log(TAG, "ScreenLocked! Not handle" + event);
                        return true;
                    }
                    dispatchKeyEventForExclusiveKeyboard(event);
                } else if (isScreenLocked()) {
                    HwPCUtils.log(TAG, "ScreenLocked! Not handle" + event);
                    return true;
                } else {
                    dispatchKeyEventForExclusiveKeyboard(event);
                    return true;
                }
            } else if (down && repeatCount == 0) {
                dispatchKeyEventForExclusiveKeyboard(event);
                return false;
            }
            return false;
        }
        dispatchKeyEventForExclusiveKeyboard(event);
        return true;
    }

    public void overrideRectForForceRotation(WindowState win, Rect pf, Rect df, Rect of, Rect cf, Rect vf, Rect dcf) {
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (forceRotationManager.isForceRotationSupported() && forceRotationManager.isForceRotationSwitchOpen(this.mContext) && win != null && win.getAppToken() != null && win.getAttrs() != null) {
            String winTitle = String.valueOf(win.getAttrs().getTitle());
            if (TextUtils.isEmpty(winTitle) || winTitle.startsWith("SurfaceView") || winTitle.startsWith("PopupWindow")) {
                return;
            }
            if (win.inMultiWindowMode()) {
                Slog.v(TAG, "window is in multiwindow mode");
                return;
            }
            Display defDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
            DisplayMetrics dm = new DisplayMetrics();
            defDisplay.getMetrics(dm);
            if (dm.widthPixels >= dm.heightPixels) {
                Rect tmpRect = new Rect(vf);
                if (forceRotationManager.isAppForceLandRotatable(win.getAttrs().packageName, win.getAppToken().asBinder())) {
                    forceRotationManager.applyForceRotationLayout(win.getAppToken().asBinder(), tmpRect);
                    if (!tmpRect.equals(vf)) {
                        int i = tmpRect.left;
                        vf.left = i;
                        cf.left = i;
                        df.left = i;
                        pf.left = i;
                        dcf.left = i;
                        of.left = i;
                        int i2 = tmpRect.right;
                        vf.right = i2;
                        cf.right = i2;
                        df.right = i2;
                        pf.right = i2;
                        dcf.right = i2;
                        of.right = i2;
                    }
                    win.getAttrs().privateFlags |= 64;
                }
            }
        }
    }

    public void notifyRotationChange(int rotation) {
        HwScreenOnProximityLock hwScreenOnProximityLock = this.mHwScreenOnProximityLock;
        if (hwScreenOnProximityLock != null) {
            hwScreenOnProximityLock.refreshForRotationChange(rotation);
            if (CoordinationStackDividerManager.getInstance(this.mContext).isVisible() && getDefaultDisplayPolicy() != null) {
                CoordinationStackDividerManager.getInstance(this.mContext).updateDividerView(isLandscape(getDefaultDisplayPolicy().getDisplayRotation()));
            }
        }
        IHwSwingService hwSwing = HwSwingManager.getService();
        if (hwSwing != null) {
            try {
                hwSwing.notifyRotationChange(rotation);
            } catch (RemoteException e) {
                Log.e(TAG, "notifyRotationChange error : " + e.getMessage());
            }
        }
    }

    private boolean isLandscape(int rotation) {
        return rotation == 1 || rotation == 3;
    }

    public void notifyFingerSense(int rotation) {
        Class cls = this.mFsManagerCls;
        if (cls != null && this.mFsManagerObj != null) {
            try {
                cls.getMethod("notifyFingerSense", Integer.TYPE).invoke(this.mFsManagerObj, Integer.valueOf(rotation));
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "notifyFingerSense NoSuchMethodException");
            } catch (IllegalAccessException e2) {
                Log.e(TAG, "notifyFingerSense IllegalAccessException");
            } catch (InvocationTargetException e3) {
                Log.e(TAG, "notifyFingerSense InvocationTargetException");
            }
        }
    }

    public void layoutWindowLwForNotch(WindowState win, WindowManager.LayoutParams attrs) {
        if (IS_NOTCH_PROP) {
            int displayRotation = getDefaultDisplayPolicy().getDisplayRotation();
            boolean isNeverMode = false;
            this.isAppWindow = attrs.type >= 1 && attrs.type <= 99;
            this.mIsNoneNotchAppInHideMode = this.hwNotchScreenWhiteConfig.isNoneNotchAppHideInfo(win);
            boolean isSnapshotStartingWindow = win.toString().contains("SnapshotStartingWindow");
            if (!HwDisplaySizeUtil.hasSideInScreen()) {
                notchControlFillet(win);
            }
            if (displayRotation == 0 && !this.mIsNotchSwitchOpen) {
                if ((this.mDefaultDisplayPolicy.getFocusedWindow() != null ? this.hwNotchScreenWhiteConfig.getAppUseNotchMode(this.mDefaultDisplayPolicy.getFocusedWindow().getOwningPackage()) : 0) == 2) {
                    isNeverMode = true;
                }
                setStatusBarColorForNotchMode(isNeverMode, isSnapshotStartingWindow);
            }
        }
    }

    public boolean isWindowNeedLayoutBelowWhenHideNotch() {
        return this.mIsLayoutBelowWhenHideNotch;
    }

    public boolean isWindowNeedLayoutBelowNotch(WindowState win) {
        boolean z = false;
        this.mIsLayoutBelowWhenHideNotch = false;
        if (!IS_NOTCH_PROP) {
            return false;
        }
        if (this.mIsNotchSwitchOpen) {
            int displayRotation = getDefaultDisplayPolicy().getDisplayRotation();
            if (displayRotation == 1 || displayRotation == 3) {
                this.mLayoutBelowNotch = !(win.toString().contains("com.qeexo.smartshot.CropActivity") || win.toString().contains("com.huawei.smartshot.CropActivity") || win.toString().contains("com.huawei.ucd.walllpaper1.GLWallpaperService") || win.toString().contains("com.huawei.android.launcher"));
            } else {
                boolean z2 = this.mIsNoneNotchAppInHideMode;
                this.mIsLayoutBelowWhenHideNotch = z2;
                this.mLayoutBelowNotch = z2 || !canLayoutInDisplayCutout(win);
            }
        } else {
            this.mLayoutBelowNotch = !canLayoutInDisplayCutout(win);
        }
        if (this.mLayoutBelowNotch && !win.toString().contains("PointerLocation")) {
            z = true;
        }
        this.mLayoutBelowNotch = z;
        return this.mLayoutBelowNotch;
    }

    public boolean canLayoutInDisplayCutout(WindowState win) {
        if (!IS_NOTCH_PROP) {
            return true;
        }
        int mode = this.mDefaultDisplayPolicy.getFocusedWindow() != null ? this.hwNotchScreenWhiteConfig.getAppUseNotchMode(this.mDefaultDisplayPolicy.getFocusedWindow().getOwningPackage()) : 0;
        boolean isNeverMode = mode == 2;
        boolean isAlwaysMode = mode == 1;
        if (isNeverMode) {
            return false;
        }
        if (isAlwaysMode) {
            return true;
        }
        if (!this.mIsNotchSwitchOpen && this.hwNotchScreenWhiteConfig.isNotchAppInfo(win)) {
            return true;
        }
        if ((!this.mIsNotchSwitchOpen || !this.hwNotchScreenWhiteConfig.isNotchAppHideInfo(win)) && (win.getAttrs().hwFlags & 65536) == 0 && !win.getHwNotchSupport() && win.getAttrs().layoutInDisplayCutoutMode != 1) {
            return false;
        }
        return true;
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
        this.mHwPWMEx.setIntersectCutoutForNotch(false);
        transferSwitchStatusToSurfaceFlinger(0);
    }

    private void showNotchRoundCorner() {
        this.mFirstSetCornerInLandNotch = false;
        this.mFirstSetCornerInLandNoNotch = true;
        this.mHwPWMEx.setIntersectCutoutForNotch(true);
        transferSwitchStatusToSurfaceFlinger(1);
    }

    private boolean shouldSkipSpecialWindow(WindowState win) {
        for (String packageName : this.specialWindowPackageNames) {
            if (packageName.equals(win.getOwningPackage())) {
                return true;
            }
        }
        CharSequence title = win.getAttrs().getTitle();
        if (title == null || title.toString() == null || !title.toString().startsWith("PopupWindow:")) {
            return false;
        }
        return true;
    }

    private boolean shouldPreventNotchFilletForSideScreen(WindowState win) {
        if (shouldSkipSpecialWindow(win)) {
            return true;
        }
        return false;
    }

    public void notchControlFilletForSideScreen(WindowState win, boolean isForced) {
        int notchMode;
        if (win != null) {
            int displayRotation = getDefaultDisplayPolicy().getDisplayRotation();
            int sideMode = 0;
            if ("com.huawei.android.extdisplay".equals(win.getOwningPackage()) && win.getAttrs().type == 2003) {
                sideMode = 1;
            } else if (isForced || !shouldPreventNotchFilletForSideScreen(win)) {
                IHwDisplayPolicyEx iHwDisplayPolicyEx = this.mDefaultDisplayPolicy.getHwDisplayPolicyEx();
                if (iHwDisplayPolicyEx != null) {
                    WindowState windowState = (WindowState) win;
                    windowState.mIsNeedExceptDisplaySide = iHwDisplayPolicyEx.isNeedExceptDisplaySide(win.getAttrs(), windowState, displayRotation);
                    sideMode = windowState.mIsNeedExceptDisplaySide ? 1 : 0;
                }
            } else {
                return;
            }
            if (!this.mIsNotchSwitchOpen) {
                if (displayRotation != 1 && displayRotation != 3) {
                    notchMode = 0;
                } else if (canLayoutInDisplayCutout(win)) {
                    this.mHwPWMEx.setIntersectCutoutForNotch(false);
                    notchMode = 0;
                } else {
                    this.mHwPWMEx.setIntersectCutoutForNotch(true);
                    notchMode = 1;
                }
            } else if (displayRotation == 2) {
                notchMode = 0;
            } else {
                notchMode = 1;
            }
            transferSwitchStatusToSurfaceFlingerForSideScreen(notchMode, sideMode);
        }
    }

    private void transferSwitchStatusToSurfaceFlingerForSideScreen(int notchMode, int sideMode) {
        Parcel dataIn = Parcel.obtain();
        int val = (notchMode & 3) | ((sideMode << 2) & 12);
        try {
            Slog.d(TAG, "transferSwitchStatusToSurfaceFlingerForSideScreen sideMode = " + sideMode + " notchMode = " + notchMode + " dataIn val = " + val);
            dataIn.writeInt(val);
            IBinder sfBinder = ServiceManager.getService("SurfaceFlinger");
            if (sfBinder != null && !sfBinder.transact(NOTCH_ROUND_CORNER_CODE, dataIn, null, 1)) {
                Slog.d(TAG, "transferSwitchStatusToSurfaceFlingerForSideScreen error!");
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "transferSwitchStatusToSurfaceFlingerForSideScreen RemoteException on notify screen rotation animation end");
        } catch (Throwable th) {
            dataIn.recycle();
            throw th;
        }
        dataIn.recycle();
    }

    private void notchControlFillet(WindowState win) {
        int displayRotation = getDefaultDisplayPolicy().getDisplayRotation();
        boolean flagGroup = false;
        if (!this.mIsNotchSwitchOpen) {
            if (displayRotation == 1 || displayRotation == 3) {
                this.mFirstSetCornerInPort = true;
                boolean splashScreen = win.toString().contains("Splash Screen");
                boolean workSpace = win.toString().contains("com.huawei.intelligent.Workspace");
                boolean isNotchSupport = canLayoutInDisplayCutout(win);
                boolean flagGroup2 = this.isAppWindow && (this.mDefaultDisplayPolicy.getFocusedWindow() != null && this.mDefaultDisplayPolicy.getFocusedWindow().toString().equals(win.toString())) && !splashScreen;
                if (win.inMultiWindowMode()) {
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
                } else if (flagGroup2 && isNotchSupport && this.mFirstSetCornerInLandNoNotch) {
                    hideNotchRoundCorner();
                } else if (flagGroup2 && !isNotchSupport && this.mFirstSetCornerInLandNotch) {
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
        } else if (displayRotation == 2) {
            boolean splashScreen2 = win.toString().contains("Splash Screen");
            boolean isTopWindow = this.mDefaultDisplayPolicy.getFocusedWindow() != null && this.mDefaultDisplayPolicy.getFocusedWindow().toString().equals(win.toString());
            if (!splashScreen2 && this.isAppWindow && this.mFirstSetCornerInReversePortait && isTopWindow) {
                this.mFirstSetCornerInReversePortait = false;
                this.mFirstSetCornerDefault = true;
                transferSwitchStatusToSurfaceFlinger(0);
            }
        } else if (this.mFirstSetCornerDefault) {
            this.mFirstSetCornerInReversePortait = true;
            this.mFirstSetCornerDefault = false;
            transferSwitchStatusToSurfaceFlinger(1);
        }
    }

    private void transferSwitchStatusToSurfaceFlinger(int notchMode) {
        Slog.d(TAG, "Window issued fillet display notchMode = " + notchMode + ", mIsNotchSwitchOpen = " + this.mIsNotchSwitchOpen + ", mDisplayRotation = " + getDefaultDisplayPolicy().getDisplayRotation());
        Parcel dataIn = Parcel.obtain();
        try {
            dataIn.writeInt(notchMode);
            IBinder sfBinder = ServiceManager.getService("SurfaceFlinger");
            if (sfBinder != null && !sfBinder.transact(NOTCH_ROUND_CORNER_CODE, dataIn, null, 1)) {
                Slog.d(TAG, "transferSwitchStatusToSurfaceFlinger error!");
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "transferSwitchStatusToSurfaceFlinger RemoteException on notify screen rotation animation end");
        } catch (Throwable th) {
            dataIn.recycle();
            throw th;
        }
        dataIn.recycle();
    }

    public void layoutWindowForPadPCMode(WindowState win, Rect pf, Rect df, Rect cf, Rect vf, int mContentBottom) {
    }

    public void setSwitchingUser(boolean switching) {
        if (switching) {
            Slog.d(TAG, "face_rotation: switchUser unbindIntelliService");
            IntelliServiceManager.getInstance(this.mContext).unbindIntelliService();
            SubScreenViewEntry subScreenViewEntry = this.mSubScreenViewEntry;
            if (subScreenViewEntry != null) {
                subScreenViewEntry.handleSwitchingUserForRemoteViews();
            }
        }
        HwPhoneWindowManager.super.setSwitchingUser(switching);
    }

    private boolean isRightNotchState() {
        WindowState focusedWindow = getDefaultDisplayPolicy().getFocusedWindow();
        boolean isTopWindowLauncher = getDefaultDisplayPolicy().getTopFullscreenOpaqueWindowState().toString().contains("com.huawei.android.launcher");
        return !isTopWindowLauncher || (focusedWindow == null && isTopWindowLauncher);
    }

    private boolean isWhiteFocusedWindow() {
        WindowState focusedWindow = getDefaultDisplayPolicy().getFocusedWindow();
        String focusedWindowString = focusedWindow != null ? focusedWindow.toString() : "";
        return focusedWindowString.contains("HwGlobalActions") || focusedWindowString.contains("Sys2023:dream");
    }

    public boolean getForceNotchStatusBar() {
        return getDefaultDisplayPolicy().getForceNotchStatusBar();
    }

    public void setForceNotchStatusBar(boolean forceNotchStatusBar) {
        getDefaultDisplayPolicy().setForceNotchStatusBar(forceNotchStatusBar);
    }

    private boolean isPowerOffScreen() {
        WindowState topFullOpaqueWin = getDefaultDisplayPolicy().getTopFullscreenOpaqueWindowState();
        WindowState focusedWindow = getDefaultDisplayPolicy().getFocusedWindow();
        if (topFullOpaqueWin == null || focusedWindow == null) {
            return false;
        }
        boolean isLauncherWin = topFullOpaqueWin.toString().contains("com.huawei.android.launcher");
        boolean isPowerOffWin = focusedWindow.toString().contains("HwGlobalActions");
        if (!isLauncherWin || !isPowerOffWin || !this.mIsNotchSwitchOpen) {
            return false;
        }
        return true;
    }

    public boolean hideNotchStatusBar(int fl) {
        boolean hideNotchStatusBar = true;
        this.mBarVisibility = 1;
        WindowState focusedWindow = getDefaultDisplayPolicy().getFocusedWindow();
        int lastSystemUiFlags = getDefaultDisplayPolicy().getLastSystemUiFlags();
        boolean isNotchSupport = focusedWindow != null ? canLayoutInDisplayCutout(focusedWindow) : false;
        int displayRotation = getDefaultDisplayPolicy().getDisplayRotation();
        if (!IS_NOTCH_PROP || displayRotation != 0) {
            return true;
        }
        if (focusedWindow != null && focusedWindow.toString().contains("com.huawei.intelligent")) {
            return true;
        }
        if (isPowerOffScreen()) {
            setForceNotchStatusBar(true);
            this.mBarVisibility = 0;
            return false;
        } else if (!this.mIsNotchSwitchOpen || !isRightNotchState()) {
            WindowState topFullscreenOpaqueWindowState = getDefaultDisplayPolicy().getTopFullscreenOpaqueWindowState();
            if (isNotchSupport) {
                return true;
            }
            if (focusedWindow != null && (this.hwNotchScreenWhiteConfig.isNotchAppInfo(focusedWindow) || focusedWindow.toString().contains("com.huawei.android.launcher"))) {
                return true;
            }
            if (focusedWindow != null && (this.hwNotchScreenWhiteConfig.isNoneNotchAppWithStatusbarInfo(focusedWindow) || (((focusedWindow.getAttrs().hwFlags & AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION) != 0 && ((fl & 1024) != 0 || (lastSystemUiFlags & 4) != 0)) || (getForceNotchStatusBar() && (focusedWindow.toString().contains("SearchPanel") || (!topFullscreenOpaqueWindowState.toString().contains("Splash Screen") && !topFullscreenOpaqueWindowState.toString().equals(focusedWindow.toString()))))))) {
                setForceNotchStatusBar(true);
                hideNotchStatusBar = false;
                this.mBarVisibility = 0;
            } else if (getForceNotchStatusBar() && focusedWindow != null && focusedWindow.getAttrs().type == 2 && ((getDefaultDisplayPolicy().getWindowFlags((WindowState) null, focusedWindow.getAttrs()) & 1024) != 0 || ((focusedWindow.getAttrs().hwFlags & AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION) != 0 && (lastSystemUiFlags & 1024) != 0))) {
                setForceNotchStatusBar(true);
                hideNotchStatusBar = false;
                this.mBarVisibility = 0;
            } else if ((focusedWindow == null && getForceNotchStatusBar() && (!((fl & 1024) == 0 && (lastSystemUiFlags & 4) == 0) && !topFullscreenOpaqueWindowState.toString().contains("Splash Screen"))) || !(topFullscreenOpaqueWindowState.getAttrs().type == 3 || (fl & 1024) == 0 || !topFullscreenOpaqueWindowState.toString().contains("Splash Screen"))) {
                setForceNotchStatusBar(true);
                hideNotchStatusBar = false;
                this.mBarVisibility = 0;
            }
            if (!(isKeyguardShowingOrOccluded() || (this.mKeyguardDelegate != null && this.mKeyguardDelegate.isOccluded()))) {
                return hideNotchStatusBar;
            }
            setForceNotchStatusBar(false);
            this.mBarVisibility = 1;
            return true;
        } else {
            boolean statusBarFocused = (focusedWindow == null || (getDefaultDisplayPolicy().getWindowFlags(null, focusedWindow.getAttrs()) & 2048) == 0) ? false : true;
            if ((((fl & 1024) == 0 && (lastSystemUiFlags & 4) == 0) ? false : true) || isWhiteFocusedWindow()) {
                hideNotchStatusBar = false;
                setForceNotchStatusBar(getForceNotchStatusBar() || !statusBarFocused);
                this.mBarVisibility = 0;
            }
            if (!getForceNotchStatusBar() || !statusBarFocused) {
                return hideNotchStatusBar;
            }
            this.mBarVisibility = 1;
            setForceNotchStatusBar(false);
            getDefaultDisplayPolicy().setNotchStatusBarColorLw(0);
            notchStatusBarColorUpdate(1);
            return false;
        }
    }

    public void notchStatusBarColorUpdate(int statusbarStateFlag) {
        if (!this.mIsForceSetStatusBar) {
            if (this.mDefaultDisplayPolicy.getFocusedWindow() != null) {
                WindowManager.LayoutParams attrs = this.mDefaultDisplayPolicy.getFocusedWindow().getAttrs();
                this.mLastNavigationBarColor = attrs.navigationBarColor;
                this.mLastStatusBarColor = attrs.statusBarColor;
                this.mLastIsEmuiStyle = getEmuiStyleValue(attrs.isEmuiStyle);
            }
            notchTransactToStatusBarService(121, "notchTransactToStatusBarService", this.mLastIsEmuiStyle, this.mLastStatusBarColor, this.mLastNavigationBarColor, -1, statusbarStateFlag, this.mBarVisibility);
        }
    }

    /* access modifiers changed from: protected */
    public void wakeUpFromPowerKey(long eventTime) {
        doFaceRecognize(true, "FCDT-POWERKEY");
        HwPhoneWindowManager.super.wakeUpFromPowerKey(eventTime);
    }

    public void doFaceRecognize(boolean detect, String reason) {
        if (this.mKeyguardDelegate != null) {
            FaceReportEventToIaware.reportEventToIaware(this.mContext, 20025);
            this.mKeyguardDelegate.doFaceRecognize(detect, reason);
        }
    }

    public void notchTransactToStatusBarService(int code, String transactName, int isEmuiStyle, int statusbarColor, int navigationBarColor, int isEmuiLightStyle, int statusbarStateFlag, int barVisibility) {
        IBinder statusBarServiceBinder;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (!(getHWStatusBarService() == null || (statusBarServiceBinder = getHWStatusBarService().asBinder()) == null)) {
                Log.d(TAG, "set statusbarColor:" + statusbarColor + ", barVisibility: " + barVisibility + " to status bar service");
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                data.writeInt(isEmuiStyle);
                data.writeInt(statusbarColor);
                data.writeInt(navigationBarColor);
                data.writeInt(isEmuiLightStyle);
                data.writeInt(statusbarStateFlag);
                data.writeInt(barVisibility);
                statusBarServiceBinder.transact(code, data, reply, 0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "notchTransactToStatusBarService->threw remote exception");
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
        reply.recycle();
        data.recycle();
    }

    private boolean handleInputEventInPCCastMode(KeyEvent event) {
        WindowState windowState;
        IHwPCManager pcManager;
        if (HwPCUtils.isPcCastModeInServer() && (windowState = this.mLighterDrawView) != null && windowState.isVisibleLw() && (pcManager = HwPCUtils.getHwPCManager()) != null) {
            try {
                return pcManager.shouldInterceptInputEvent(event, false);
            } catch (RemoteException e) {
                Log.e(TAG, "interceptInputEventInPCCastMode()");
            }
        }
        return false;
    }

    public void setPowerState(int powerState) {
        HwAodManager.getInstance().setPowerState(powerState);
    }

    private boolean isInScreenFingerprint(int type) {
        return 1 == type || 2 == type;
    }

    private void pauseAOD() {
        if (this.mDeviceNodeFD > 0) {
            HwAodManager.getInstance().pause();
        }
    }

    private int getDeviceNodeFD() {
        return HwAodManager.getInstance().getDeviceNodeFD();
    }

    public void setAodState(final int aodState) {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass33 */

            public void run() {
                HwPhoneWindowManager.this.startAodService(aodState);
            }
        });
    }

    /* access modifiers changed from: private */
    public void startAodService(int aodState) {
        Slog.i("AOD_HwPWM", "startAodService mAodSwitch=" + this.mAodSwitch + " mAODState=" + this.mAODState + " aodState=" + aodState + " mIsFingerprintEnabledBySettings=" + this.mIsFingerprintEnabledBySettings + " mFingerprintType=" + this.mFingerprintType);
        if (!this.mIsActuralShutDown || !(aodState == 101 || aodState == 103)) {
            if (aodState == 101 || aodState == 103) {
                if (this.mDeviceNodeFD == -2147483647) {
                    this.mDeviceNodeFD = getDeviceNodeFD();
                }
                setLmtSwitch();
            }
            if (-1 == this.mFingerprintType) {
                this.mFingerprintType = getHardwareType();
            }
            if (this.mAodSwitch == 0) {
                boolean isFpEnable = false;
                if (isInScreenFingerprint(this.mFingerprintType) && this.mIsFingerprintEnabledBySettings) {
                    Slog.i("AOD_HwPWM", "startAodService fp enabled");
                    isFpEnable = true;
                }
                boolean isAudioStreamEnable = false;
                if (!TextUtils.isEmpty(sRingVolumeBarDisp)) {
                    Slog.i("AOD_HwPWM", "startAodService audio enabled");
                    isAudioStreamEnable = true;
                }
                if (!isFpEnable && !isAudioStreamEnable) {
                    return;
                }
            }
            if (this.mAODState == aodState) {
                Slog.i("AOD_HwPWM", "handleAodState mAODState equal, state =" + aodState);
                return;
            }
            this.mAODState = aodState;
            handleAodState(aodState);
            Slog.w("AOD_HwPWM", " startAodService end success.");
        }
    }

    private void handleAodState(int aodState) {
        Intent intent = null;
        Slog.w("AOD_HwPWM", " handleAodState aodState:" + aodState);
        switch (aodState) {
            case 100:
            case 102:
                setPowerState(aodState);
                pauseAOD();
                intent = new Intent(AOD_WAKE_UP_ACTION);
                break;
            case 101:
                setPowerState(10);
            case 103:
                setPowerState(101);
                intent = new Intent(AOD_GOING_TO_SLEEP_ACTION);
                break;
        }
        if (intent != null) {
            intent.setComponent(new ComponentName("com.huawei.aod", "com.huawei.aod.AODService"));
            this.mContext.startService(intent);
        }
    }

    private void setLmtSwitch() {
        if (!isSupportLmtDisplay()) {
            Slog.i(TAG, "AOD does not support lmt display ");
            return;
        }
        connectToProxy();
        int lmtSwitch = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_display_type", 0, ActivityManager.getCurrentUser());
        if (lmtSwitch == 2 || lmtSwitch == 3) {
            setAodTouchState(1);
        } else {
            setAodTouchState(0);
        }
    }

    private boolean isSupportLmtDisplay() {
        int aodTouchTime;
        int displayModeType;
        int displayModeType2;
        String[] strArr = LMT_CONFIGS;
        if (strArr.length != 3) {
            return false;
        }
        try {
            displayModeType2 = Integer.parseInt(strArr[0]);
            displayModeType = Integer.parseInt(LMT_CONFIGS[1]);
            aodTouchTime = Integer.parseInt(LMT_CONFIGS[2]);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "hw_mc.aod.support_display_style contains invalid item.");
            aodTouchTime = 0;
            displayModeType2 = 0;
            displayModeType = 0;
        }
        if ((displayModeType2 == 1 || displayModeType2 == 2 || displayModeType2 == 3) && displayModeType > 0 && aodTouchTime > 0) {
            return true;
        }
        return false;
    }

    private void setAodTouchState(int state) {
        ITouchscreen iTouchscreen = this.mTpTouchSwitch;
        if (iTouchscreen == null) {
            Slog.e(TAG, "touch service not available");
            return;
        }
        try {
            int result = iTouchscreen.hwSetFeatureConfig(6, Integer.toString(state));
            Slog.d(TAG, "finish hwSetFeatureConfig with state: " + state + " stateString: " + Integer.toString(state) + " result: " + result);
        } catch (RemoteException e) {
            Log.e(TAG, "updateAudioStatus RemoteException");
        }
    }

    public int getHardwareType() {
        return FingerprintManagerEx.getHardwareType();
    }

    public boolean isWindowSupportKnuckle() {
        if (getFocusedWindow() == null || (getFocusedWindow().getAttrs().flags & HwWindowManager.LayoutParams.FLAG_DISABLE_KNUCKLE_TO_LAUNCH_APP) == 0) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void uploadKeyEvent(int keyEvent) {
        if (this.mContext != null) {
            if (keyEvent == 3) {
                StatisticalUtils.reportc(this.mContext, 173);
            } else if (keyEvent == 4) {
                StatisticalUtils.reportc(this.mContext, 172);
            }
        }
    }

    public boolean isHwStartWindowEnabled(int type) {
        if (type == 0) {
            return StartWindowFeature.isStartWindowEnable();
        }
        return false;
    }

    public Context addHwStartWindow(ApplicationInfo appInfo, Context overrideContext, Context context, TypedArray typedArray, int windowFlags) {
        if (overrideContext == null || context == null || typedArray == null || appInfo == null) {
            return null;
        }
        boolean addHwStartWindowFlag = false;
        boolean windowIsTranslucent = typedArray.getBoolean(5, false);
        boolean windowDisableStarting = typedArray.getBoolean(12, false);
        boolean windowShowWallpaper = typedArray.getBoolean(14, false);
        if ((windowDisableStarting || windowIsTranslucent || (windowShowWallpaper && (windowFlags & HighBitsCompModeID.MODE_COLOR_ENHANCE) != 1048576)) && HwStartWindowRecord.getInstance().checkStartWindowApp(Integer.valueOf(appInfo.uid))) {
            addHwStartWindowFlag = true;
        }
        if (!addHwStartWindowFlag) {
            return null;
        }
        overrideContext.setTheme(overrideContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.NoActionBar", null, null));
        Slog.d(TAG, "addHwStartWindow set default on app : " + appInfo.packageName);
        return overrideContext;
    }

    public boolean isNotchDisplayDisabled() {
        return this.mIsNotchSwitchOpen;
    }

    public boolean getWindowLayoutBelowNotch() {
        return this.mLayoutBelowNotch;
    }

    /* access modifiers changed from: protected */
    public void cancelPendingPowerKeyAction() {
        HwPhoneWindowManager.super.cancelPendingPowerKeyAction();
        cancelAIPowerLongPressed();
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (OPEN_PROXIMITY_DISPALY) {
            if (this.mSensorManager == null) {
                this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            }
            if (enable) {
                if (!this.mProximitySensorEnabled) {
                    this.mProximitySensorEnabled = true;
                    SensorManager sensorManager = this.mSensorManager;
                    sensorManager.registerListener(this.mProximitySensorListener, sensorManager.getDefaultSensor(8), 3);
                }
            } else if (this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = false;
                this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            }
        }
    }

    public void notifyVolumePanelStatus(boolean isVolumePanelVisible) {
        HwSideVibrationManager hwSideVibrationManager;
        this.mIsVolumePanelVisible = isVolumePanelVisible;
        Log.i(TAG, "notifyVolumePanelStatus isPanelVisible: " + isVolumePanelVisible);
        if (this.mIsSupportSideScreen && (hwSideVibrationManager = this.mSideVibrationManager) != null) {
            if (isVolumePanelVisible) {
                hwSideVibrationManager.registerVolumeChangedListener();
            } else {
                hwSideVibrationManager.unregisterVolumeChangedListener();
            }
        }
    }

    private boolean shouldSendVolumeKeyForWhilitelistApp() {
        WindowState focusedWindow;
        HwDisplaySideRegionConfig configInstance;
        WindowManager.LayoutParams params;
        if (!(!this.mIsSupportSideScreen || this.mDefaultDisplayPolicy == null || (focusedWindow = this.mDefaultDisplayPolicy.getFocusedWindow()) == null || (configInstance = HwDisplaySideRegionConfig.getInstance()) == null || (params = focusedWindow.getAttrs()) == null)) {
            String packageName = params.packageName;
            if (packageName == null) {
                Log.e(TAG, "packageName is null");
                return false;
            } else if (configInstance.isAppShouldSendVolumeKey(packageName)) {
                Log.d(TAG, "current package will send volume key on Lion " + packageName);
                return true;
            }
        }
        return false;
    }

    public int checkActionResult(KeyEvent event, boolean isInjected, int result) {
        HwSideStatusManager hwSideStatusManager;
        boolean isSideTouchEvent;
        HwSideStatusManager hwSideStatusManager2;
        int checkResult = result;
        if (!this.mSystemReady || isInjected || event == null) {
            return result;
        }
        if (shouldSendVolumeKeyForWhilitelistApp()) {
            Log.w(TAG, "current app is in whitelist, we will suffer it get volume key event");
            return result;
        }
        if (this.mIsSupportSideScreen && (hwSideStatusManager = this.mSideStatusManager) != null && (isSideTouchEvent = hwSideStatusManager.isSideTouchEvent(event)) && isScreenTurnedOn()) {
            this.mIsSideTouchEvent = isSideTouchEvent;
            Log.d(TAG, "checkActionResult ");
            checkResult &= -2;
            if (this.mSideTouchManager == null) {
                Log.w(TAG, "mSideTouchManager is null");
                return checkResult;
            }
            boolean isAudioPlaying = false;
            boolean isKeyguardShowing = isKeyguardShowingAndNotOccluded();
            if (isKeyguardShowing && (hwSideStatusManager2 = this.mSideStatusManager) != null) {
                isAudioPlaying = hwSideStatusManager2.isAudioPlaybackActive();
            }
            if (isAudioPlaying || !isKeyguardShowing) {
                this.mSideTouchManager.notifySendVolumeKeyToSystem(event);
            }
        }
        return checkResult;
    }

    private boolean isScreenTurnedOn() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService(HIVOICE_PRESS_TYPE_POWER);
        if (powerManager == null) {
            return true;
        }
        Log.i(TAG, "isScreenTurnedOn " + powerManager.isScreenOn());
        return powerManager.isScreenOn();
    }

    public boolean isMusicOnly() {
        if (!this.mIsSupportSideScreen || !this.mIsSideTouchEvent) {
            return true;
        }
        this.mIsSideTouchEvent = false;
        if (!isScreenOn()) {
            return true;
        }
        Log.d(TAG, "isMusicOnly ");
        return false;
    }

    private void reportVolumeData(KeyEvent event, boolean isInjected, boolean isScreenOn) {
        if (!this.mIsSupportSideScreen || !this.mSystemReady) {
            Log.w(TAG, "reportVolumeData not support or system not ready");
            return;
        }
        Log.d(TAG, "enter reportVolumeData");
        if (isInjected) {
            Log.w(TAG, "reportVolumeData isInjected");
        } else if (event == null || event.getAction() != 0) {
            Log.w(TAG, "reportVolumeData event is null or not action down");
        } else if (preventVolumeKeyForSideScreen(event)) {
            Log.w(TAG, "reportVolumeData not isVibrate some interface");
        } else {
            HwSideStatusManager hwSideStatusManager = this.mSideStatusManager;
            if (hwSideStatusManager != null) {
                hwSideStatusManager.updateVolumeTriggerStatus(event);
            }
            queryHandStatus(event);
            boolean isKeyguardShowing = false;
            if (isScreenOn) {
                isKeyguardShowing = isKeyguardShowingAndNotOccluded();
            }
            HwSideVibrationManager hwSideVibrationManager = this.mSideVibrationManager;
            if (hwSideVibrationManager != null) {
                hwSideVibrationManager.vibrateFromVolumeKey(event, this.mIsAudioPlaying, isScreenOn, isKeyguardShowing);
            }
            HwSideTouchDataReport.getInstance().reportVolumeBtnKeyEvent(event);
        }
    }

    private void queryHandStatus(KeyEvent event) {
        if (event.getKeyCode() == 25) {
            if (((event.getFlags() & 4096) != 0) && this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    /* class com.android.server.policy.HwPhoneWindowManager.AnonymousClass35 */

                    public void run() {
                        if (HwPhoneWindowManager.this.mSideTouchManager != null) {
                            HwPhoneWindowManager.this.mSideTouchManager.queryHandStatus();
                        }
                    }
                });
            }
        }
    }

    private boolean isAcquireProximityLock() {
        if (this.mHwScreenOnProximityLock == null) {
            return false;
        }
        boolean isModeEnabled = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_TOUCH_DISABLE_MODE, 1, ActivityManager.getCurrentUser()) <= 0 || "factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            isModeEnabled = false;
        }
        if (!isModeEnabled || !this.mDeviceProvisioned) {
            return false;
        }
        boolean isPhoneCallState = checkPhoneOFFHOOK();
        if (!isPhoneCallState || (isPhoneCallState && checkHeadSetIsConnected())) {
            return true;
        }
        return false;
    }

    public void setDisplayMode(int mode) {
        HwScreenOnProximityLock hwScreenOnProximityLock = this.mHwScreenOnProximityLock;
        if (hwScreenOnProximityLock != null) {
            if (3 == mode) {
                hwScreenOnProximityLock.releaseLock(0);
            } else if (isAcquireProximityLock() && isKeyguardLocked()) {
                if (isScreenTurnedOn()) {
                    this.mHwScreenOnProximityLock.acquireLock(this, mode);
                }
                this.mHwScreenOnProximityLock.forceRefreshHintView();
            }
        }
    }

    public boolean shouldWaitScreenOnExBlocker() {
        return IS_SUPPORT_FOLD_SCREEN;
    }

    public boolean isScreenOnExBlocking() {
        boolean z;
        synchronized (this.mScreenOnExLock) {
            z = this.mScreenOnExListener != null;
        }
        return z;
    }

    public void screenTurningOnEx(ScreenOnExListener screenOnExListener) {
        if (shouldWaitScreenOnExBlocker()) {
            if (DEBUG_WAKEUP) {
                Slog.i(TAG, "Screen turning on ex...");
            }
            synchronized (this.mScreenOnExLock) {
                this.mScreenOnExListener = screenOnExListener;
                if (this.mFoldScreenManagerService == null || this.mFoldScreenOnUnblocker == null) {
                    this.mHandlerEx.sendEmptyMessage(108);
                } else {
                    this.mHandlerEx.removeMessages(108);
                    this.mHandlerEx.sendEmptyMessageDelayed(108, 1500);
                    this.mFoldScreenManagerService.foldScreenTurningOn(this.mFoldScreenOnUnblocker);
                }
            }
        } else if (screenOnExListener != null) {
            screenOnExListener.onScreenOnEx();
        }
    }

    private final class FoldScreenOnUnblocker implements HwFoldScreenManagerInternal.FoldScreenOnListener {
        private FoldScreenOnUnblocker() {
        }

        public void onFoldScreenOn(int newMode, int oldMode) {
            Slog.i(HwPhoneWindowManager.TAG, "onFoldScreenOn newMode:" + newMode + ", oldMode:" + oldMode);
            HwPhoneWindowManager.this.mHandlerEx.sendMessage(HwPhoneWindowManager.this.mHandlerEx.obtainMessage(107, newMode, oldMode));
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0021, code lost:
        r1 = r7.mDefaultDisplayPolicy.getScreenOnListener();
        r3 = r7.mDefaultDisplayPolicy.isWindowManagerDrawComplete();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x002f, code lost:
        if (com.android.server.policy.HwPhoneWindowManager.DEBUG_WAKEUP == false) goto L_0x004f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0031, code lost:
        android.util.Slog.i(com.android.server.policy.HwPhoneWindowManager.TAG, "finishSetFoldMode listener:" + r1 + ", windowDrawComplete:" + r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x004f, code lost:
        if (r1 != null) goto L_0x005f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0051, code lost:
        if (r3 == false) goto L_0x005f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0053, code lost:
        r7.mWindowManagerInternal.waitForAllWindowsDrawn(r7.mFoldWindowDrawCallback, 1000);
        r4 = 1000;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x005f, code lost:
        r4 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0061, code lost:
        r6 = r7.mScreenOnExLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0063, code lost:
        monitor-enter(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:?, code lost:
        r7.mHandlerEx.sendEmptyMessageDelayed(108, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0069, code lost:
        monitor-exit(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x006a, code lost:
        return;
     */
    public void finishSetFoldMode(int newMode, int oldMode) {
        synchronized (this.mScreenOnExLock) {
            if (this.mScreenOnExListener != null) {
                this.mHandlerEx.removeMessages(107);
                if (!(newMode != oldMode)) {
                    this.mHandlerEx.sendEmptyMessage(108);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void finishScreenTurningOnEx() {
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "finish screen turning on ex");
        }
        synchronized (this.mScreenOnExLock) {
            this.mHandlerEx.removeMessages(108);
            ScreenOnExListener screenOnExListener = this.mScreenOnExListener;
            if (screenOnExListener != null) {
                screenOnExListener.onScreenOnEx();
            }
            this.mScreenOnExListener = null;
        }
    }

    private void initBehaviorCollector() {
        if (IS_SUPPORT_HW_BEHAVIOR_AUTH && this.behaviorCollector == null) {
            this.behaviorCollector = BehaviorCollector.getInstance();
            this.behaviorCollector.init(this.mContext, this.mWindowManagerFuncs);
            Log.i(TAG, "behaviorCollector init success");
        }
    }

    /* access modifiers changed from: protected */
    public void checkFaceDetect(boolean interactive, String reason, KeyEvent event) {
        InputDevice inputDevice;
        if (IS_TABLET && !interactive && event != null && (inputDevice = InputManager.getInstance().getInputDevice(event.getDeviceId())) != null) {
            int productId = inputDevice.getProductId();
            int vendorId = inputDevice.getVendorId();
            if (productId == MRX_KEYBOARD_PID && vendorId == MRX_KEYBOARD_VID) {
                doFaceRecognize(true, reason);
            }
        }
    }

    private boolean isTvButtonPress(KeyEvent event) {
        return event.getSource() == 257;
    }

    private boolean isInputBlock(int keyCode, boolean isDownAction, boolean isInteractive) {
        if (isInteractive || keyCode == 224 || keyCode == 26 || keyCode == 231) {
            return false;
        }
        if ((keyCode == 24 || keyCode == 25) && !isDownAction) {
            return false;
        }
        Slog.i(TAG, "interactive = false, ignore other keys except wakeup/power/voice/volume(action up). keycode:" + keyCode);
        return true;
    }

    private boolean isInterceptKeyBeforeQueueingTv(KeyEvent event, boolean isInteractive) {
        int keyCode = event.getKeyCode();
        if (keyCode != 26 || !"0".equals(SystemProperties.get(BOOT_ANIM_EXIT_PROP))) {
            if (!isInputBlock(keyCode, event.getAction() == 0, isInteractive)) {
                return false;
            }
            Slog.i(TAG, "isInterceptKeyBeforeQueueingTv isInputBlock=true");
            return true;
        }
        Slog.i(TAG, "system init not compeleted, ignore power_key.");
        return true;
    }

    private void interceptPowerKeyDownTv(KeyEvent event, boolean isInteractive) {
        Log.i(TAG, "interceptPowerKeyDownTv: event " + event + " isInteractive " + isInteractive);
        this.mPowerKeyHandledByTv = false;
        if (isInteractive) {
            Message msg = this.mHandlerEx.obtainMessage(109, Boolean.valueOf(isTvButtonPress(event)));
            msg.setAsynchronous(true);
            this.mHandlerEx.sendMessage(msg);
            this.mPowerKeyHandledByTv = true;
            Slog.i(TAG, "interceptPowerKeyDownTv: sendMessage MSG_POWER_KEY_PRESS");
        }
    }

    /* access modifiers changed from: private */
    public void noticePowerKeyPressed(boolean isTvButtonPress) {
        if (isTvButtonPress) {
            Slog.i(TAG, "noticePowerKeyPressed: goToSleep shutdown");
            goToSleep(SystemClock.uptimeMillis(), 4, 65536);
            return;
        }
        Slog.i(TAG, "noticePowerKeyPressed: call the shutdown menu");
        showGlobalActionsInternal();
    }

    /* access modifiers changed from: private */
    public void noticeVoiceAssistKeyPressed(boolean isDown) {
        if (isDown) {
            this.mContext.sendBroadcastAsUser(new Intent(VOICE_ASSIST_KEY_DOWN_ACTION), UserHandle.ALL);
            Log.i(TAG, "noticeVoiceAssistKeyPressed: broadcast VOICE_ASSIST_KEY_DOWN_ACTION.");
            return;
        }
        this.mContext.sendBroadcastAsUser(new Intent(VOICE_ASSIST_KEY_UP_ACTION), UserHandle.ALL);
        Log.i(TAG, "noticeVoiceAssistKeyPressed: broadcast VOICE_ASSIST_KEY_UP_ACTION.");
    }
}
