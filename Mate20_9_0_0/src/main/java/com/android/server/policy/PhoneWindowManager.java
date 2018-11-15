package com.android.server.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerInternal.SleepToken;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.IUiModeManager;
import android.app.IUiModeManager.Stub;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.UiModeManager;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiPlaybackClient.OneTouchPlayCallback;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.hdm.HwDeviceManager;
import android.hsm.HwSystemManager;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioManagerInternal;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.session.MediaSessionLegacyHelper;
import android.net.dhcp.DhcpPacket;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IAodStateCallback;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.pc.IHwPCManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.dreams.DreamManagerInternal;
import android.service.dreams.IDreamManager;
import android.service.vr.IPersistentVrStateCallbacks;
import android.telecom.TelecomManager;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.IApplicationToken;
import android.view.IHwRotateObserver;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyCharacterMap;
import android.view.KeyCharacterMap.FallbackAction;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.autofill.AutofillManagerInternal;
import android.view.inputmethod.InputMethodManagerInternal;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.policy.KeyguardDismissCallback;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ScreenShapeHelper;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.widget.PointerLocationView;
import com.android.server.GestureLauncherService;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.audio.AudioService;
import com.android.server.display.DisplayTransformManager;
import com.android.server.lights.LightsManager;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy.InputConsumer;
import com.android.server.policy.WindowManagerPolicy.KeyguardDismissDoneListener;
import com.android.server.policy.WindowManagerPolicy.OnKeyguardExitResult;
import com.android.server.policy.WindowManagerPolicy.ScreenOffListener;
import com.android.server.policy.WindowManagerPolicy.ScreenOnListener;
import com.android.server.policy.WindowManagerPolicy.StartingSurface;
import com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.policy.keyguard.KeyguardServiceDelegate.DrawnListener;
import com.android.server.policy.keyguard.KeyguardStateMonitor.StateCallback;
import com.android.server.power.IHwShutdownThread;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.vr.VrManagerInternal;
import com.android.server.wm.AppTransition;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerInternal.AppTransitionListener;
import com.android.server.wm.utils.WmDisplayCutout;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneWindowManager extends AbsPhoneWindowManager implements WindowManagerPolicy {
    private static final String ACTION_ACTURAL_SHUTDOWN = "com.android.internal.app.SHUTDOWNBROADCAST";
    public static final String ACTIVITY_NAME_EMERGENCY_COUNTDOWN = "com.android.emergency/.view.ViewCountDownActivity";
    public static final String ACTIVITY_NAME_EMERGENCY_NUMBER = "com.android.emergency/.view.EmergencyNumberActivity";
    static final boolean ALTERNATE_CAR_MODE_NAV_SIZE = false;
    private static final int BRIGHTNESS_STEPS = 10;
    private static final long BUGREPORT_TV_GESTURE_TIMEOUT_MILLIS = 1000;
    static final boolean DEBUG = false;
    static final boolean DEBUG_INPUT = false;
    static final boolean DEBUG_KEYGUARD = false;
    static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_SPLASH_SCREEN = false;
    static final boolean DEBUG_VOLUME_KEY = true;
    static final boolean DEBUG_WAKEUP;
    static final boolean DISABLE_VOLUME_HUSH = true;
    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;
    static final boolean ENABLE_VR_HEADSET_HOME_CAPTURE = true;
    private static final String FINGER_PRINT_ENABLE = "fp_keyguard_enable";
    private static final String FOCUSED_SPLIT_APP_ACTIVITY = "com.huawei.android.launcher/.splitscreen.SplitScreenAppActivity";
    private static final int FP_SWITCH_OFF = 0;
    private static final int FP_SWITCH_ON = 1;
    private static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    private static final String HUAWEI_PRE_CAMERA_START_MODE = "com.huawei.RapidCapture";
    private static final String HUAWEI_SHUTDOWN_PERMISSION = "huawei.android.permission.HWSHUTDOWN";
    protected static final boolean HWFLOW;
    private static final int INVALID_HARDWARE_TYPE = -1;
    private static final int IN_SCREEN_OPTIC_TYPE = 1;
    private static final int IN_SCREEN_ULTRA_TYPE = 2;
    protected static final boolean IS_NOTCH_PROP = (SystemProperties.get("ro.config.hw_notch_size", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) ^ true);
    private static final boolean IS_lOCK_UNNATURAL_ORIENTATION = SystemProperties.getBoolean("ro.config.lock_land_screen", false);
    private static final float KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER = 2.5f;
    static final int LAST_LONG_PRESS_HOME_BEHAVIOR = 2;
    static final int LONG_PRESS_BACK_GO_TO_VOICE_ASSIST = 1;
    static final int LONG_PRESS_BACK_NOTHING = 0;
    static final int LONG_PRESS_HOME_ALL_APPS = 1;
    static final int LONG_PRESS_HOME_ASSIST = 2;
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_GO_TO_VOICE_ASSIST = 4;
    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;
    private static final int MSG_ACCESSIBILITY_SHORTCUT = 20;
    private static final int MSG_ACCESSIBILITY_TV = 22;
    private static final int MSG_BACK_LONG_PRESS = 18;
    private static final int MSG_BUGREPORT_TV = 21;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_BACK_KEY_TO_AUTOFILL = 23;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_SHOW_GLOBAL_ACTIONS = 10;
    private static final int MSG_DISPATCH_SHOW_RECENTS = 9;
    private static final int MSG_DISPOSE_INPUT_CONSUMER = 19;
    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_HANDLE_ALL_APPS = 25;
    private static final int MSG_HIDE_BOOT_MESSAGE = 11;
    private static final int MSG_KEYGUARD_DRAWN_COMPLETE = 5;
    private static final int MSG_KEYGUARD_DRAWN_TIMEOUT = 6;
    private static final int MSG_LAUNCH_ASSIST = 26;
    private static final int MSG_LAUNCH_ASSIST_LONG_PRESS = 27;
    private static final int MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK = 12;
    private static final int MSG_NOTIFY_USER_ACTIVITY = 29;
    private static final int MSG_POWER_DELAYED_PRESS = 13;
    private static final int MSG_POWER_LONG_PRESS = 14;
    private static final int MSG_POWER_VERY_LONG_PRESS = 28;
    private static final int MSG_REQUEST_TRANSIENT_BARS = 16;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_NAVIGATION = 1;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_STATUS = 0;
    private static final int MSG_RINGER_TOGGLE_CHORD = 30;
    private static final int MSG_SHOW_PICTURE_IN_PICTURE_MENU = 17;
    private static final int MSG_SYSTEM_KEY_PRESS = 24;
    private static final int MSG_UPDATE_DREAMING_SLEEP_TOKEN = 15;
    private static final int MSG_WINDOW_MANAGER_DRAWN_COMPLETE = 7;
    static final int MULTI_PRESS_POWER_BRIGHTNESS_BOOST = 2;
    static final int MULTI_PRESS_POWER_NOTHING = 0;
    static final int MULTI_PRESS_POWER_THEATER_MODE = 1;
    static final int NAV_BAR_OPAQUE_WHEN_FREEFORM_OR_DOCKED = 0;
    static final int NAV_BAR_TRANSLUCENT_WHEN_FREEFORM_OPAQUE_OTHERWISE = 1;
    public static final String NAV_TAG = "NavigationBar";
    private static final String NEED_START_AOD_WHEN_SCREEN_OFF = "needStartAODWhenScreenOff";
    private static final int NaviHide = 1;
    private static final int NaviInit = -1;
    private static final int NaviShow = 0;
    private static final int NaviTransientShow = 2;
    private static final long PANIC_GESTURE_EXPIRATION = 30000;
    static final int PENDING_KEY_NULL = -1;
    private static final int POWERDOWN_MAX_TIMEOUT = 200;
    private static final int POWER_SCREENOF_AFTER_TURNINGON = 9;
    private static final int POWER_STARTED_GOING_TO_SLEEP = 1;
    private static final int POWER_STARTED_WAKING_UP = 0;
    private static final int POWER_TURNINGON_BEFORE_WAKINGUP = 5;
    static final boolean PRINT_ANIM = false;
    private static final long SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS = 150;
    private static final int SCREENSHOT_DELAY = 0;
    private static final int SET_FINGER_INIT_STATUS = 8;
    static final int SHORT_PRESS_POWER_CLOSE_IME_OR_GO_HOME = 5;
    static final int SHORT_PRESS_POWER_GO_HOME = 4;
    static final int SHORT_PRESS_POWER_GO_TO_SLEEP = 1;
    static final int SHORT_PRESS_POWER_NOTHING = 0;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP = 2;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME = 3;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP = 0;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME = 1;
    static final int SHORT_PRESS_WINDOW_NOTHING = 0;
    static final int SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE = 1;
    static final boolean SHOW_SPLASH_SCREENS = true;
    private static final String SINGLE_VIRTAL_NAVBAR = "ai_navigationbar";
    private static final String SINGLE_VIRTAL_NAVBAR_SWITCH = "ai_enable";
    public static final int START_AOD_BOOT = 1;
    public static final int START_AOD_GOING_TO_SLEEP = 7;
    public static final int START_AOD_SCREEN_OFF = 3;
    public static final int START_AOD_SCREEN_ON = 2;
    public static final int START_AOD_TURNING_ON = 5;
    public static final int START_AOD_USER_SWITCHED = 6;
    public static final int START_AOD_WAKE_UP = 4;
    public static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
    public static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    public static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    public static final String SYSTEM_DIALOG_REASON_SCREENSHOT = "screenshot";
    static final int SYSTEM_UI_CHANGING_LAYOUT = -1073709042;
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";
    private static final String SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService";
    static final String TAG = "WindowManager";
    public static final int TOAST_WINDOW_TIMEOUT = 3500;
    private static final int USER_ACTIVITY_NOTIFICATION_DELAY = 200;
    static final int VERY_LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int VERY_LONG_PRESS_POWER_NOTHING = 0;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    static final int WAITING_FOR_DRAWN_TIMEOUT = 1000;
    static final int WAITING_FOR_KEYGUARD_DISMISS_TIMEOUT = 300;
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = new int[]{2003, 2010};
    private static boolean bHasFrontFp = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    static final int deviceGlobalActionKeyTimeout = 1000;
    static final boolean localLOGV = false;
    public static boolean mSupporInputMethodFilletAdaptation = SystemProperties.getBoolean("ro.config.support_inputmethod_fillet_adaptation", false);
    private static final boolean mSupportAod = "1".equals(SystemProperties.get("ro.config.support_aod", null));
    static final Rect mTmpContentFrame = new Rect();
    static final Rect mTmpDecorFrame = new Rect();
    private static final Rect mTmpDisplayCutoutSafeExceptMaybeBarsRect = new Rect();
    static final Rect mTmpDisplayFrame = new Rect();
    static final Rect mTmpNavigationFrame = new Rect();
    static final Rect mTmpOutsetFrame = new Rect();
    static final Rect mTmpOverscanFrame = new Rect();
    static final Rect mTmpParentFrame = new Rect();
    private static final Rect mTmpRect = new Rect();
    static final Rect mTmpStableFrame = new Rect();
    static final Rect mTmpVisibleFrame = new Rect();
    private static boolean mUsingHwNavibar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    static SparseArray<String> sApplicationLaunchKeyCategories = new SparseArray();
    private static final String sProximityWndName = "Emui:ProximityWnd";
    boolean ifBootMessageShowing = false;
    private boolean mA11yShortcutChordVolumeUpKeyConsumed;
    private long mA11yShortcutChordVolumeUpKeyTime;
    private boolean mA11yShortcutChordVolumeUpKeyTriggered;
    private int mAODState = 1;
    AccessibilityManager mAccessibilityManager;
    private AccessibilityShortcutController mAccessibilityShortcutController;
    private boolean mAccessibilityTvKey1Pressed;
    private boolean mAccessibilityTvKey2Pressed;
    private boolean mAccessibilityTvScheduled;
    private final Runnable mAcquireSleepTokenRunnable = new -$$Lambda$PhoneWindowManager$qkEs_boDTAbqA6wKqcLwnsgoklc(this);
    ActivityManagerInternal mActivityManagerInternal;
    int mAllowAllRotations = -1;
    boolean mAllowLockscreenWhenOn;
    boolean mAllowStartActivityForLongPressOnPowerDuringSetup;
    private boolean mAllowTheaterModeWakeFromCameraLens;
    private boolean mAllowTheaterModeWakeFromKey;
    private boolean mAllowTheaterModeWakeFromLidSwitch;
    private boolean mAllowTheaterModeWakeFromMotion;
    private boolean mAllowTheaterModeWakeFromMotionWhenNotDreaming;
    private boolean mAllowTheaterModeWakeFromPowerKey;
    private boolean mAllowTheaterModeWakeFromWakeGesture;
    private boolean mAodShowing;
    private int mAodSwitch = 1;
    private AodSwitchObserver mAodSwitchObserver;
    AppOpsManager mAppOpsManager;
    AudioManagerInternal mAudioManagerInternal;
    AutofillManagerInternal mAutofillManagerInternal;
    volatile boolean mAwake;
    volatile boolean mBackKeyHandled;
    volatile boolean mBeganFromNonInteractive;
    boolean mBootMessageNeedsHiding;
    ProgressDialog mBootMsgDialog = null;
    WakeLock mBroadcastWakeLock;
    private boolean mBugreportTvKey1Pressed;
    private boolean mBugreportTvKey2Pressed;
    private boolean mBugreportTvScheduled;
    BurnInProtectionHelper mBurnInProtectionHelper;
    long[] mCalendarDateVibePattern;
    volatile boolean mCameraGestureTriggeredDuringGoingToSleep;
    int mCameraLensCoverState = -1;
    boolean mCarDockEnablesAccelerometer;
    Intent mCarDockIntent;
    int mCarDockRotation;
    private final Runnable mClearHideNavigationFlag = new Runnable() {
        public void run() {
            synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                PhoneWindowManager phoneWindowManager = PhoneWindowManager.this;
                phoneWindowManager.mForceClearedSystemUiFlags &= -3;
            }
            PhoneWindowManager.this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    };
    boolean mConsumeSearchKeyUp;
    Context mContext;
    int mCurrentAppOrientation = -1;
    protected int mCurrentUserId;
    private HwCustPhoneWindowManager mCust = ((HwCustPhoneWindowManager) HwCustUtils.createObj(HwCustPhoneWindowManager.class, new Object[0]));
    public int mDefaultNavBarHeight;
    int mDemoHdmiRotation;
    boolean mDemoHdmiRotationLock;
    int mDemoRotation;
    boolean mDemoRotationLock;
    boolean mDeskDockEnablesAccelerometer;
    Intent mDeskDockIntent;
    int mDeskDockRotation;
    private int mDeviceNodeFD = -2147483647;
    private volatile boolean mDismissImeOnBackKeyPressed;
    Display mDisplay;
    int mDisplayRotation;
    int mDockLayer;
    int mDockMode = 0;
    BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                PhoneWindowManager.this.mDockMode = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
            } else {
                try {
                    IUiModeManager uiModeService = Stub.asInterface(ServiceManager.getService("uimode"));
                    PhoneWindowManager.this.mUiMode = uiModeService.getCurrentModeType();
                } catch (RemoteException e) {
                }
            }
            PhoneWindowManager.this.updateRotation(true);
            synchronized (PhoneWindowManager.this.mLock) {
                PhoneWindowManager.this.updateOrientationListenerLp();
            }
        }
    };
    final Rect mDockedStackBounds = new Rect();
    int mDoublePressOnPowerBehavior;
    private int mDoubleTapOnHomeBehavior;
    DreamManagerInternal mDreamManagerInternal;
    BroadcastReceiver mDreamReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DREAMING_STARTED".equals(intent.getAction())) {
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.onDreamingStarted();
                }
            } else if ("android.intent.action.DREAMING_STOPPED".equals(intent.getAction()) && PhoneWindowManager.this.mKeyguardDelegate != null) {
                PhoneWindowManager.this.mKeyguardDelegate.onDreamingStopped();
            }
        }
    };
    boolean mDreamingLockscreen;
    SleepToken mDreamingSleepToken;
    boolean mDreamingSleepTokenNeeded;
    private boolean mEnableCarDockHomeCapture = true;
    boolean mEnableShiftMenuBugReports = false;
    volatile boolean mEndCallKeyHandled;
    private final Runnable mEndCallLongPress = new Runnable() {
        public void run() {
            PhoneWindowManager.this.mEndCallKeyHandled = true;
            PhoneWindowManager.this.performHapticFeedbackLw(null, 0, false);
            PhoneWindowManager.this.showGlobalActionsInternal();
        }
    };
    int mEndcallBehavior;
    private final SparseArray<FallbackAction> mFallbackActions = new SparseArray();
    private int mFingerprintType = -1;
    private boolean mFirstIncall;
    IApplicationToken mFocusedApp;
    WindowState mFocusedWindow;
    int mForceClearedSystemUiFlags = 0;
    private boolean mForceDefaultOrientation = false;
    protected boolean mForceNotchStatusBar = false;
    boolean mForceShowSystemBars;
    boolean mForceStatusBar;
    boolean mForceStatusBarFromKeyguard;
    private boolean mForceStatusBarTransparent;
    WindowState mForceStatusBarTransparentWin;
    boolean mForcingShowNavBar;
    int mForcingShowNavBarLayer;
    private boolean mFrozen = false;
    GlobalActions mGlobalActions;
    private GlobalKeyManager mGlobalKeyManager;
    private boolean mGoToSleepOnButtonPressTheaterMode;
    volatile boolean mGoingToSleep;
    private UEventObserver mHDMIObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            PhoneWindowManager.this.setHdmiPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };
    private boolean mHandleVolumeKeysInWM;
    Handler mHandler;
    protected boolean mHasCoverView = false;
    private boolean mHasFeatureLeanback;
    private boolean mHasFeatureWatch;
    boolean mHasNavigationBar = false;
    boolean mHasSoftInput = false;
    boolean mHaveBuiltInKeyboard;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;
    HdmiControl mHdmiControl;
    boolean mHdmiPlugged;
    private final Runnable mHiddenNavPanic = new Runnable() {
        /* JADX WARNING: Missing block: B:11:0x0030, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                if (PhoneWindowManager.this.isUserSetupComplete()) {
                    PhoneWindowManager.this.mPendingPanicGestureUptime = SystemClock.uptimeMillis();
                    if (!PhoneWindowManager.isNavBarEmpty(PhoneWindowManager.this.mLastSystemUiFlags)) {
                        PhoneWindowManager.this.mNavigationBarController.showTransient();
                    }
                }
            }
        }
    };
    boolean mHomeConsumed;
    boolean mHomeDoubleTapPending;
    private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() {
        public void run() {
            if (PhoneWindowManager.this.mHomeDoubleTapPending) {
                PhoneWindowManager.this.mHomeDoubleTapPending = false;
                PhoneWindowManager.this.handleShortPressOnHome();
            }
        }
    };
    Intent mHomeIntent;
    boolean mHomePressed;
    private boolean mHwFullScreenWinVisibility = false;
    private WindowState mHwFullScreenWindow = null;
    private RemoteCallbackList<IHwRotateObserver> mHwRotateObservers = new RemoteCallbackList();
    IAodStateCallback mIAodStateCallback;
    private ImmersiveModeConfirmation mImmersiveModeConfirmation;
    boolean mImmersiveStatusChanged;
    int mIncallBackBehavior;
    int mIncallPowerBehavior;
    int mInitialMetaState;
    InputConsumer mInputConsumer = null;
    protected InputManagerInternal mInputManagerInternal;
    InputMethodManagerInternal mInputMethodManagerInternal;
    public boolean mInputMethodMovedUp;
    private WindowState mInputMethodTarget;
    protected boolean mInterceptInputForWaitBrightness = false;
    private boolean mIsActuralShutDown = false;
    private boolean mIsFingerprintEnabledBySettings = false;
    public boolean mIsFloatIME;
    protected boolean mIsNoneNotchAppInHideMode = false;
    protected boolean mIsNotchSwitchOpen = false;
    private boolean mKeyguardBound;
    KeyguardServiceDelegate mKeyguardDelegate;
    final Runnable mKeyguardDismissDoneCallback = new Runnable() {
        public void run() {
            Slog.i(PhoneWindowManager.TAG, "keyguard dismiss done!");
            PhoneWindowManager.this.finishKeyguardDismissDone();
        }
    };
    KeyguardDismissDoneListener mKeyguardDismissListener;
    boolean mKeyguardDrawComplete;
    final DrawnListener mKeyguardDrawnCallback = new DrawnListener() {
        public void onDrawn() {
            Slog.d(PhoneWindowManager.TAG, "UL_Power mKeyguardDelegate.ShowListener.onDrawn.");
            if (!PhoneWindowManager.this.mFirstIncall && PhoneWindowManager.this.isSupportCover() && PhoneWindowManager.this.isSmartCoverMode() && !HwFrameworkFactory.getCoverManager().isCoverOpen() && PhoneWindowManager.this.isInCallActivity()) {
                PhoneWindowManager.this.mFirstIncall = true;
                PhoneWindowManager.this.mHandler.sendEmptyMessageDelayed(5, 300);
                return;
            }
            PhoneWindowManager.this.mHandler.sendEmptyMessage(5);
        }
    };
    private boolean mKeyguardDrawnOnce;
    volatile boolean mKeyguardOccluded;
    private boolean mKeyguardOccludedChanged;
    int mLandscapeRotation = 0;
    boolean mLanguageSwitchKeyPressed;
    final Rect mLastDockedStackBounds = new Rect();
    int mLastDockedStackSysUiFlags;
    public boolean mLastFocusNeedsMenu = false;
    int mLastFullscreenStackSysUiFlags;
    int mLastHideNaviDockBottom = 0;
    WindowState mLastInputMethodTargetWindow = null;
    WindowState mLastInputMethodWindow = null;
    int mLastNaviStatus = -1;
    final Rect mLastNonDockedStackBounds = new Rect();
    int mLastShowNaviDockBottom = 0;
    private boolean mLastShowingDream;
    private WindowState mLastStartingWindow;
    int mLastSystemUiFlags;
    int mLastSystemUiFlagsTmp;
    int mLastTransientNaviDockBottom = 0;
    private boolean mLastWindowSleepTokenNeeded;
    protected boolean mLayoutBeyondDisplayCutout = false;
    boolean mLidControlsScreenLock;
    boolean mLidControlsSleep;
    int mLidKeyboardAccessibility;
    int mLidNavigationAccessibility;
    int mLidOpenRotation;
    int mLidState = -1;
    protected final Object mLock = new Object();
    int mLockScreenTimeout;
    boolean mLockScreenTimerActive;
    private final LogDecelerateInterpolator mLogDecelerateInterpolator = new LogDecelerateInterpolator(100, 0);
    MetricsLogger mLogger;
    int mLongPressOnBackBehavior;
    private int mLongPressOnHomeBehavior;
    int mLongPressOnPowerBehavior;
    long[] mLongPressVibePattern;
    int mMetaState;
    BroadcastReceiver mMultiuserReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                PhoneWindowManager.this.mSettingsObserver.onChange(false);
                if (PhoneWindowManager.mSupportAod) {
                    Slog.i(PhoneWindowManager.TAG, "AOD mAodSwitchObserver.onChange");
                    PhoneWindowManager.this.mAodSwitchObserver.onChange(false);
                    if (!(PhoneWindowManager.this.mScreenOnEarly || PhoneWindowManager.this.mScreenOnFully)) {
                        PhoneWindowManager.this.startAodService(6);
                    }
                }
                synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                    PhoneWindowManager.this.mLastSystemUiFlags = 0;
                    PhoneWindowManager.this.updateSystemUiVisibilityLw();
                }
            }
        }
    };
    int mNavBarOpacityMode = 0;
    volatile boolean mNavBarVirtualKeyHapticFeedbackEnabled = true;
    private final OnBarVisibilityChangedListener mNavBarVisibilityListener = new OnBarVisibilityChangedListener() {
        public void onBarVisibilityChanged(boolean visible) {
            if (PhoneWindowManager.bHasFrontFp && PhoneWindowManager.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                visible = false;
            }
            String str = PhoneWindowManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyAccessibilityButtonVisibilityChanged visible:");
            stringBuilder.append(visible);
            Slog.i(str, stringBuilder.toString());
            PhoneWindowManager.this.mAccessibilityManager.notifyAccessibilityButtonVisibilityChanged(visible);
        }
    };
    WindowState mNavigationBar = null;
    boolean mNavigationBarCanMove = false;
    private final BarController mNavigationBarController = new BarController(NAV_TAG, 134217728, 536870912, Integer.MIN_VALUE, 2, 134217728, 32768);
    int[] mNavigationBarHeightForRotationDefault = new int[4];
    int[] mNavigationBarHeightForRotationInCarMode = new int[4];
    int mNavigationBarPosition = 4;
    int[] mNavigationBarWidthForRotationDefault = new int[4];
    int[] mNavigationBarWidthForRotationInCarMode = new int[4];
    private boolean mNeedPauseAodWhileSwitchUser = false;
    final Rect mNonDockedStackBounds = new Rect();
    protected int mNotchPropSize = 0;
    private boolean mNotifyUserActivity;
    MyOrientationListener mOrientationListener;
    boolean mOrientationSensorEnabled = false;
    boolean mPendingCapsLockToggle;
    private boolean mPendingKeyguardOccluded;
    boolean mPendingMetaAction;
    private long mPendingPanicGestureUptime;
    volatile int mPendingWakeKey = -1;
    private volatile boolean mPersistentVrModeEnabled;
    final IPersistentVrStateCallbacks mPersistentVrModeListener = new IPersistentVrStateCallbacks.Stub() {
        public void onPersistentVrStateChanged(boolean enabled) {
            PhoneWindowManager.this.mPersistentVrModeEnabled = enabled;
            String str = PhoneWindowManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPersistentVrStateChanged enabled:");
            stringBuilder.append(PhoneWindowManager.this.mPersistentVrModeEnabled);
            Log.d(str, stringBuilder.toString());
        }
    };
    private boolean mPickUpFlag = false;
    volatile boolean mPictureInPictureVisible;
    int mPointerLocationMode = 0;
    PointerLocationView mPointerLocationView;
    int mPortraitRotation = 0;
    volatile boolean mPowerKeyHandled;
    volatile int mPowerKeyPressCounter;
    WakeLock mPowerKeyWakeLock;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    boolean mPreloadedRecentApps;
    int mRecentAppsHeldModifiers;
    volatile boolean mRecentsVisible;
    private final Runnable mReleaseSleepTokenRunnable = new -$$Lambda$PhoneWindowManager$SMVPfeuVGHeByGLchxVc-pxEEMw(this);
    volatile boolean mRequestedOrGoingToSleep;
    int mResettingSystemUiFlags = 0;
    int mRestrictedScreenHeight;
    private int mRingerToggleChord = 0;
    boolean mSafeMode;
    long[] mSafeModeEnabledVibePattern;
    private final ArraySet<WindowState> mScreenDecorWindows = new ArraySet();
    ScreenLockTimeout mScreenLockTimeout = new ScreenLockTimeout();
    protected int mScreenOffReason = -1;
    SleepToken mScreenOffSleepToken;
    boolean mScreenOnEarly;
    boolean mScreenOnFully;
    ScreenOnListener mScreenOnListener;
    protected int mScreenRotation = 0;
    private boolean mScreenshotChordEnabled;
    private long mScreenshotChordPowerKeyTime;
    private boolean mScreenshotChordPowerKeyTriggered;
    private boolean mScreenshotChordVolumeDownKeyConsumed;
    private long mScreenshotChordVolumeDownKeyTime;
    private boolean mScreenshotChordVolumeDownKeyTriggered;
    private ScreenshotHelper mScreenshotHelper;
    private final ScreenshotRunnable mScreenshotRunnable = new ScreenshotRunnable(this, null);
    boolean mSearchKeyShortcutPending;
    SearchManager mSearchManager;
    int mSeascapeRotation = 0;
    final Object mServiceAquireLock = new Object();
    SettingsObserver mSettingsObserver;
    int mShortPressOnPowerBehavior;
    int mShortPressOnSleepBehavior;
    int mShortPressOnWindowBehavior;
    private LongSparseArray<IShortcutService> mShortcutKeyServices = new LongSparseArray();
    ShortcutManager mShortcutManager;
    int mShowRotationSuggestions;
    boolean mShowingDream;
    BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (PhoneWindowManager.ACTION_ACTURAL_SHUTDOWN.equals(intent.getAction())) {
                PhoneWindowManager.this.mOrientationListener.disable();
                PhoneWindowManager.this.mOrientationSensorEnabled = false;
                PhoneWindowManager.this.mIsActuralShutDown = true;
                PhoneWindowManager.this.notifyFingerSense(-1);
            }
        }
    };
    WindowState mStatusBar = null;
    private final StatusBarController mStatusBarController = new StatusBarController();
    private final int[] mStatusBarHeightForRotation = new int[4];
    int mStatusBarLayer;
    StatusBarManagerInternal mStatusBarManagerInternal;
    IStatusBarService mStatusBarService;
    boolean mSupportAutoRotation;
    private boolean mSupportLongPressPowerWhenNonInteractive;
    private boolean mSyncPowerStateFlag = false;
    boolean mSystemBooted;
    @VisibleForTesting
    SystemGesturesPointerEventListener mSystemGestures;
    boolean mSystemNavigationKeysEnabled;
    boolean mSystemReady;
    private final MutableBoolean mTmpBoolean = new MutableBoolean(false);
    WindowState mTopDockedOpaqueOrDimmingWindowState;
    WindowState mTopDockedOpaqueWindowState;
    WindowState mTopFullscreenOpaqueOrDimmingWindowState;
    WindowState mTopFullscreenOpaqueWindowState;
    boolean mTopIsFullscreen;
    boolean mTranslucentDecorEnabled = true;
    int mTriplePressOnPowerBehavior;
    int mUiMode;
    IUiModeManager mUiModeManager;
    int mUndockedHdmiRotation;
    int mUpsideDownRotation = 0;
    boolean mUseTvRouting;
    int mUserRotation = 0;
    int mUserRotationMode = 0;
    int mVeryLongPressOnPowerBehavior;
    int mVeryLongPressTimeout;
    Vibrator mVibrator;
    Intent mVrHeadsetHomeIntent;
    volatile VrManagerInternal mVrManagerInternal;
    boolean mWakeGestureEnabledSetting;
    MyWakeGestureListener mWakeGestureListener;
    IWindowManager mWindowManager;
    final Runnable mWindowManagerDrawCallback = new Runnable() {
        public void run() {
            Slog.i(PhoneWindowManager.TAG, "UL_Power All windows ready for display!");
            PhoneWindowManager.this.mHandler.sendEmptyMessage(7);
        }
    };
    boolean mWindowManagerDrawComplete;
    WindowManagerFuncs mWindowManagerFuncs;
    WindowManagerInternal mWindowManagerInternal;
    @GuardedBy("mHandler")
    private SleepToken mWindowSleepToken;
    private boolean mWindowSleepTokenNeeded;
    protected int notchStatusBarColorLw = 0;
    boolean notchWindowChange = false;
    boolean notchWindowChangeState = false;

    class AodSwitchObserver extends ContentObserver {
        AodSwitchObserver(Handler handler) {
            super(handler);
            PhoneWindowManager.this.mAodSwitch = getAodSwitch();
        }

        void observe() {
            Slog.i(PhoneWindowManager.TAG, "AOD AodSwitchObserver observe");
            if (PhoneWindowManager.mSupportAod) {
                PhoneWindowManager.this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("aod_switch"), false, this, -1);
            }
        }

        public void onChange(boolean selfChange) {
            PhoneWindowManager.this.mAodSwitch = Secure.getIntForUser(PhoneWindowManager.this.mContext.getContentResolver(), "aod_switch", 0, ActivityManager.getCurrentUser());
        }

        private int getAodSwitch() {
            Slog.i(PhoneWindowManager.TAG, "AOD getAodSwitch ");
            if (!PhoneWindowManager.mSupportAod) {
                return 0;
            }
            PhoneWindowManager.this.mAodSwitch = Secure.getIntForUser(PhoneWindowManager.this.mContext.getContentResolver(), "aod_switch", 0, ActivityManager.getCurrentUser());
            return PhoneWindowManager.this.mAodSwitch;
        }
    }

    private final class AppDeathRecipient implements DeathRecipient {
        AppDeathRecipient() {
        }

        public void binderDied() {
            String str = PhoneWindowManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Death received in ");
            stringBuilder.append(this);
            stringBuilder.append(" for thread ");
            stringBuilder.append(PhoneWindowManager.this.mIAodStateCallback.asBinder());
            Slog.i(str, stringBuilder.toString());
            if (PhoneWindowManager.mSupportAod) {
                PhoneWindowManager.this.unregeditAodStateCallback(PhoneWindowManager.this.mIAodStateCallback);
                PhoneWindowManager.this.mPowerManager.setDozeOverrideFromAod(1, 0, null);
                PhoneWindowManager.this.mPowerManager.setAodState(-1, 0);
                PhoneWindowManager.this.mPowerManager.setAodState(-1, 2);
                PhoneWindowManager.this.startAodService(7);
            }
        }
    }

    private static class HdmiControl {
        private final HdmiPlaybackClient mClient;

        /* synthetic */ HdmiControl(HdmiPlaybackClient x0, AnonymousClass1 x1) {
            this(x0);
        }

        private HdmiControl(HdmiPlaybackClient client) {
            this.mClient = client;
        }

        public void turnOnTv() {
            if (this.mClient != null) {
                this.mClient.oneTouchPlay(new OneTouchPlayCallback() {
                    public void onComplete(int result) {
                        if (result != 0) {
                            String str = PhoneWindowManager.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("One touch play failed: ");
                            stringBuilder.append(result);
                            Log.w(str, stringBuilder.toString());
                        }
                    }
                });
            }
        }
    }

    final class HideNavInputEventReceiver extends InputEventReceiver {
        public HideNavInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        /* JADX WARNING: Missing block: B:24:0x0061, code:
            if (r2 == false) goto L_0x006e;
     */
        /* JADX WARNING: Missing block: B:26:?, code:
            r9.this$0.mWindowManagerFuncs.reevaluateStatusBarVisibility();
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onInputEvent(InputEvent event, int displayId) {
            try {
                if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0 && ((MotionEvent) event).getAction() == 0) {
                    boolean changed = false;
                    synchronized (PhoneWindowManager.this.mWindowManagerFuncs.getWindowManagerLock()) {
                        if (PhoneWindowManager.this.mInputConsumer == null) {
                            finishInputEvent(event, false);
                            return;
                        }
                        int newVal = ((PhoneWindowManager.this.mResettingSystemUiFlags | 2) | 1) | 4;
                        if (PhoneWindowManager.this.mResettingSystemUiFlags != newVal) {
                            PhoneWindowManager.this.mResettingSystemUiFlags = newVal;
                            changed = true;
                        }
                        newVal = PhoneWindowManager.this.mForceClearedSystemUiFlags | 2;
                        if (PhoneWindowManager.this.mForceClearedSystemUiFlags != newVal) {
                            PhoneWindowManager.this.mForceClearedSystemUiFlags = newVal;
                            changed = true;
                            PhoneWindowManager.this.mHandler.postDelayed(PhoneWindowManager.this.mClearHideNavigationFlag, 1000);
                        }
                    }
                }
                finishInputEvent(event, false);
            } catch (Throwable th) {
                finishInputEvent(event, false);
            }
        }
    }

    private class PolicyHandler extends Handler {
        private PolicyHandler() {
        }

        /* synthetic */ PolicyHandler(PhoneWindowManager x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            PhoneWindowManager phoneWindowManager;
            switch (msg.what) {
                case 1:
                    PhoneWindowManager.this.enablePointerLocation();
                    return;
                case 2:
                    PhoneWindowManager.this.disablePointerLocation();
                    return;
                case 3:
                    PhoneWindowManager.this.dispatchMediaKeyWithWakeLock((KeyEvent) msg.obj);
                    return;
                case 4:
                    PhoneWindowManager.this.dispatchMediaKeyRepeatWithWakeLock((KeyEvent) msg.obj);
                    return;
                case 5:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w(PhoneWindowManager.TAG, "UL_Power Setting mKeyguardDrawComplete");
                    }
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case 6:
                    Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power Keyguard drawn timeout. Setting mKeyguardDrawComplete");
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case 7:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w(PhoneWindowManager.TAG, "UL_Power Setting mWindowManagerDrawComplete");
                    }
                    PhoneWindowManager.this.finishWindowsDrawn();
                    return;
                case 9:
                    PhoneWindowManager.this.showRecentApps(false);
                    return;
                case 10:
                    PhoneWindowManager.this.showGlobalActionsInternal();
                    return;
                case 11:
                    PhoneWindowManager.this.handleHideBootMessage();
                    return;
                case 12:
                    PhoneWindowManager.this.launchVoiceAssistWithWakeLock();
                    return;
                case 13:
                    phoneWindowManager = PhoneWindowManager.this;
                    long longValue = ((Long) msg.obj).longValue();
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    phoneWindowManager.powerPress(longValue, z, msg.arg2);
                    PhoneWindowManager.this.finishPowerKeyPress();
                    return;
                case 14:
                    PhoneWindowManager.this.powerLongPress();
                    return;
                case 15:
                    phoneWindowManager = PhoneWindowManager.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    phoneWindowManager.updateDreamingSleepToken(z);
                    return;
                case 16:
                    WindowState targetBar = msg.arg1 == 0 ? PhoneWindowManager.this.mStatusBar : PhoneWindowManager.this.mNavigationBar;
                    if (targetBar != null) {
                        PhoneWindowManager.this.requestTransientBars(targetBar);
                        return;
                    }
                    return;
                case 17:
                    PhoneWindowManager.this.showPictureInPictureMenuInternal();
                    return;
                case 18:
                    PhoneWindowManager.this.backLongPress();
                    return;
                case 19:
                    PhoneWindowManager.this.disposeInputConsumer((InputConsumer) msg.obj);
                    return;
                case 20:
                    PhoneWindowManager.this.accessibilityShortcutActivated();
                    return;
                case 21:
                    PhoneWindowManager.this.requestFullBugreport();
                    return;
                case 22:
                    if (PhoneWindowManager.this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(false)) {
                        PhoneWindowManager.this.accessibilityShortcutActivated();
                        return;
                    }
                    return;
                case 23:
                    PhoneWindowManager.this.mAutofillManagerInternal.onBackKeyPressed();
                    return;
                case 24:
                    PhoneWindowManager.this.sendSystemKeyToStatusBar(msg.arg1);
                    return;
                case 25:
                    PhoneWindowManager.this.launchAllAppsAction();
                    return;
                case 26:
                    PhoneWindowManager.this.launchAssistAction(msg.obj, msg.arg1);
                    return;
                case PhoneWindowManager.MSG_LAUNCH_ASSIST_LONG_PRESS /*27*/:
                    PhoneWindowManager.this.launchAssistLongPressAction();
                    return;
                case 28:
                    PhoneWindowManager.this.powerVeryLongPress();
                    return;
                case 29:
                    removeMessages(29);
                    Intent intent = new Intent("android.intent.action.USER_ACTIVITY_NOTIFICATION");
                    intent.addFlags(1073741824);
                    PhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.USER_ACTIVITY");
                    break;
                case 30:
                    break;
                default:
                    return;
            }
            PhoneWindowManager.this.handleRingerChordGesture();
        }
    }

    class ScreenLockTimeout implements Runnable {
        Bundle options;

        ScreenLockTimeout() {
        }

        public void run() {
            synchronized (this) {
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.doKeyguardTimeout(this.options);
                }
                PhoneWindowManager.this.mLockScreenTimerActive = false;
                this.options = null;
            }
        }

        public void setLockOptions(Bundle options) {
            this.options = options;
        }
    }

    private class ScreenshotRunnable implements Runnable {
        private int mScreenshotType;

        private ScreenshotRunnable() {
            this.mScreenshotType = 1;
        }

        /* synthetic */ ScreenshotRunnable(PhoneWindowManager x0, AnonymousClass1 x1) {
            this();
        }

        public void setScreenshotType(int screenshotType) {
            this.mScreenshotType = screenshotType;
        }

        public void run() {
            if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                IHwPCManager pcManager = HwPCUtils.getHwPCManager();
                if (pcManager != null) {
                    try {
                        pcManager.screenshotPc();
                    } catch (RemoteException e) {
                        HwPCUtils.log(PhoneWindowManager.TAG, "RemoteException screenshotPc");
                    }
                    return;
                }
            }
            if (PhoneWindowManager.HWFLOW) {
                Slog.i(PhoneWindowManager.TAG, "hardware_keys ScreenShot enter.");
            }
            sendScreenshotNotification();
            ScreenshotHelper access$3000 = PhoneWindowManager.this.mScreenshotHelper;
            int i = this.mScreenshotType;
            boolean z = false;
            boolean z2 = PhoneWindowManager.this.mStatusBar != null && PhoneWindowManager.this.mStatusBar.isVisibleLw();
            if (PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBar.isVisibleLw()) {
                z = true;
            }
            access$3000.takeScreenshot(i, z2, z, PhoneWindowManager.this.mHandler);
        }

        private void sendScreenshotNotification() {
            Intent screenshotIntent = new Intent("com.huawei.recsys.action.RECEIVE_EVENT");
            screenshotIntent.putExtra("eventOperator", "sysScreenShot");
            screenshotIntent.putExtra("eventItem", "hardware_keys");
            screenshotIntent.setPackage("com.huawei.recsys");
            PhoneWindowManager.this.mContext.sendBroadcastAsUser(screenshotIntent, UserHandle.CURRENT, "com.huawei.tips.permission.SHOW_TIPS");
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = PhoneWindowManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(System.getUriFor("end_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("incall_power_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("incall_back_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("wake_gesture_enabled"), false, this, -1);
            resolver.registerContentObserver(System.getUriFor("accelerometer_rotation"), false, this, -1);
            resolver.registerContentObserver(System.getUriFor("user_rotation"), false, this, -1);
            resolver.registerContentObserver(System.getUriFor("screen_off_timeout"), false, this, -1);
            resolver.registerContentObserver(System.getUriFor("pointer_location"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("default_input_method"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("immersive_mode_confirmations"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("show_rotation_suggestions"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("volume_hush_gesture"), false, this, -1);
            resolver.registerContentObserver(Global.getUriFor("policy_control"), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor("system_navigation_keys_enabled"), false, this, -1);
            resolver.registerContentObserver(System.getUriFor(PhoneWindowManager.this.fingersense_enable), false, this, -1);
            resolver.registerContentObserver(System.getUriFor(PhoneWindowManager.this.fingersense_letters_enable), false, this, -1);
            resolver.registerContentObserver(System.getUriFor(PhoneWindowManager.this.line_gesture_enable), false, this, -1);
            resolver.registerContentObserver(System.getUriFor(PhoneWindowManager.this.navibar_enable), false, this, -1);
            resolver.registerContentObserver(Secure.getUriFor(PhoneWindowManager.FINGER_PRINT_ENABLE), false, this, -1);
            resolver.registerContentObserver(System.getUriFor(PhoneWindowManager.SINGLE_VIRTAL_NAVBAR), false, this, -1);
            resolver.registerContentObserver(System.getUriFor(PhoneWindowManager.SINGLE_VIRTAL_NAVBAR_SWITCH), false, this, -1);
            PhoneWindowManager.this.updateSettings();
        }

        public void onChange(boolean selfChange) {
            PhoneWindowManager.this.updateSettings();
            PhoneWindowManager.this.updateRotation(false);
        }
    }

    protected final class UpdateRunnable implements Runnable {
        private final int mRotation;

        public UpdateRunnable(int rotation) {
            this.mRotation = rotation;
        }

        public void run() {
            PhoneWindowManager.this.mPowerManagerInternal.powerHint(2, 0);
            if (PhoneWindowManager.this.mWindowManagerInternal.isDockedDividerResizing()) {
                PhoneWindowManager.this.mWindowManagerInternal.setDockedStackDividerRotation(this.mRotation);
            } else {
                Slog.i(PhoneWindowManager.TAG, "MyOrientationListener: updateRotation.");
                if (PhoneWindowManager.this.isRotationChoicePossible(PhoneWindowManager.this.mCurrentAppOrientation)) {
                    PhoneWindowManager.this.sendProposedRotationChangeToStatusBarInternal(this.mRotation, PhoneWindowManager.this.isValidRotationChoice(PhoneWindowManager.this.mCurrentAppOrientation, this.mRotation));
                } else {
                    PhoneWindowManager.this.updateRotation(false);
                }
            }
            if (PhoneWindowManager.bHasFrontFp) {
                PhoneWindowManager.this.updateSplitScreenView();
            }
        }
    }

    class MyOrientationListener extends WindowOrientationListener {
        private SparseArray<Runnable> mRunnableCache = new SparseArray(5);

        MyOrientationListener(Context context, Handler handler) {
            super(context, handler);
        }

        public void onProposedRotationChanged(int rotation) {
            PhoneWindowManager.this.mScreenRotation = rotation;
            PhoneWindowManager.this.notifyFingerSense(rotation);
            String str = PhoneWindowManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onProposedRotationChanged, rotation=");
            stringBuilder.append(rotation);
            Slog.i(str, stringBuilder.toString());
            Jlog.d(57, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            PhoneWindowManager.this.setSensorRotationFR(rotation);
            Runnable r = (Runnable) this.mRunnableCache.get(rotation, null);
            if (r == null) {
                r = new UpdateRunnable(rotation);
                this.mRunnableCache.put(rotation, r);
            }
            if (PhoneWindowManager.this.isIntelliServiceEnabledFR(PhoneWindowManager.this.mCurrentAppOrientation)) {
                PhoneWindowManager.this.startIntelliServiceFR();
            } else {
                PhoneWindowManager.this.mHandler.post(r);
            }
        }
    }

    class MyWakeGestureListener extends WakeGestureListener {
        MyWakeGestureListener(Context context, Handler handler) {
            super(context, handler);
        }

        public void onWakeUp() {
            synchronized (PhoneWindowManager.this.mLock) {
                if (PhoneWindowManager.this.shouldEnableWakeGestureLp()) {
                    PhoneWindowManager.this.performHapticFeedbackLw(null, 1, false);
                    PhoneWindowManager.this.wakeUp(SystemClock.uptimeMillis(), PhoneWindowManager.this.mAllowTheaterModeWakeFromWakeGesture, "android.policy:GESTURE");
                }
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG_WAKEUP = z;
        z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
        sApplicationLaunchKeyCategories.append(64, "android.intent.category.APP_BROWSER");
        sApplicationLaunchKeyCategories.append(65, "android.intent.category.APP_EMAIL");
        sApplicationLaunchKeyCategories.append(207, "android.intent.category.APP_CONTACTS");
        sApplicationLaunchKeyCategories.append(208, "android.intent.category.APP_CALENDAR");
        sApplicationLaunchKeyCategories.append(209, "android.intent.category.APP_MUSIC");
        sApplicationLaunchKeyCategories.append(NetdResponseCode.TetherStatusResult, "android.intent.category.APP_CALCULATOR");
    }

    public boolean isLandscape() {
        return this.mScreenRotation == 1 || this.mScreenRotation == 3;
    }

    public static /* synthetic */ void lambda$new$0(PhoneWindowManager phoneWindowManager) {
        if (phoneWindowManager.mWindowSleepToken == null) {
            phoneWindowManager.mWindowSleepToken = phoneWindowManager.mActivityManagerInternal.acquireSleepToken("WindowSleepToken", 0);
        }
    }

    public static /* synthetic */ void lambda$new$1(PhoneWindowManager phoneWindowManager) {
        if (phoneWindowManager.mWindowSleepToken != null) {
            phoneWindowManager.mWindowSleepToken.release();
            phoneWindowManager.mWindowSleepToken = null;
        }
    }

    private void handleRingerChordGesture() {
        if (this.mRingerToggleChord != 0) {
            getAudioManagerInternal();
            this.mAudioManagerInternal.silenceRingerModeInternal("volume_hush");
            Secure.putInt(this.mContext.getContentResolver(), "hush_gesture_used", 1);
            this.mLogger.action(1440, this.mRingerToggleChord);
        }
    }

    IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    StatusBarManagerInternal getStatusBarManagerInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarManagerInternal == null) {
                this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarManagerInternal;
        }
        return statusBarManagerInternal;
    }

    AudioManagerInternal getAudioManagerInternal() {
        AudioManagerInternal audioManagerInternal;
        synchronized (this.mServiceAquireLock) {
            if (this.mAudioManagerInternal == null) {
                this.mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
            }
            audioManagerInternal = this.mAudioManagerInternal;
        }
        return audioManagerInternal;
    }

    boolean needSensorRunningLp() {
        boolean z = true;
        if (this.mSupportAutoRotation && (this.mCurrentAppOrientation == 4 || this.mCurrentAppOrientation == 10 || this.mCurrentAppOrientation == 7 || this.mCurrentAppOrientation == 6)) {
            return true;
        }
        if ((this.mCarDockEnablesAccelerometer && this.mDockMode == 2) || (this.mDeskDockEnablesAccelerometer && (this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4))) {
            return true;
        }
        if (IS_lOCK_UNNATURAL_ORIENTATION || this.mUserRotationMode != 1) {
            return this.mSupportAutoRotation;
        }
        if (!(this.mSupportAutoRotation && this.mShowRotationSuggestions == 1)) {
            z = false;
        }
        return z;
    }

    void updateOrientationListenerLp() {
        if (this.mOrientationListener.canDetectOrientation()) {
            boolean disable = true;
            if (this.mScreenOnEarly && this.mAwake && this.mKeyguardDrawComplete && this.mWindowManagerDrawComplete && needSensorRunningLp()) {
                disable = false;
                if (!this.mOrientationSensorEnabled) {
                    this.mOrientationListener.enable(true);
                    this.mOrientationSensorEnabled = true;
                }
            }
            if (disable && this.mOrientationSensorEnabled) {
                this.mOrientationListener.disable();
                this.mOrientationSensorEnabled = false;
                notifyFingerSense(-1);
            }
        }
    }

    private void interceptBackKeyDown() {
        MetricsLogger.count(this.mContext, "key_back_down", 1);
        this.mBackKeyHandled = false;
        if (hasLongPressOnBackBehavior()) {
            Message msg = this.mHandler.obtainMessage(18);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
        }
    }

    private boolean interceptBackKeyUp(KeyEvent event) {
        boolean handled = this.mBackKeyHandled;
        cancelPendingBackKeyAction();
        if (this.mHasFeatureWatch) {
            TelecomManager telecomManager = getTelecommService();
            if (telecomManager != null) {
                if (telecomManager.isRinging()) {
                    telecomManager.silenceRinger();
                    return false;
                } else if ((this.mIncallBackBehavior & 1) != 0 && telecomManager.isInCall()) {
                    return telecomManager.endCall();
                }
            }
        }
        if (this.mAutofillManagerInternal != null && event.getKeyCode() == 4) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(23));
        }
        return handled;
    }

    private void interceptPowerKeyDown(KeyEvent event, boolean interactive) {
        boolean z = interactive;
        HwFrameworkFactory.getHwApsImpl().StopSdrForSpecial("powerdown", 26);
        long startTime = SystemClock.elapsedRealtime();
        if (!this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.acquire();
        }
        startTime = printTimeoutLog("PowerManager_screenOn", startTime, "acquire wakeLock timeout", DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE);
        if (this.mPowerKeyPressCounter != 0) {
            this.mHandler.removeMessages(13);
        }
        if (this.mImmersiveModeConfirmation.onPowerKeyDown(z, SystemClock.elapsedRealtime(), isImmersiveMode(this.mLastSystemUiFlags), isNavBarEmpty(this.mLastSystemUiFlags))) {
            this.mHandler.post(this.mHiddenNavPanic);
        }
        Handler handler = this.mHandler;
        WindowManagerFuncs windowManagerFuncs = this.mWindowManagerFuncs;
        Objects.requireNonNull(windowManagerFuncs);
        handler.post(new -$$Lambda$oXa0y3A-00RiQs6-KTPBgpkGtgw(windowManagerFuncs));
        if (z && !this.mScreenshotChordPowerKeyTriggered && (event.getFlags() & 1024) == 0) {
            this.mScreenshotChordPowerKeyTriggered = true;
            this.mScreenshotChordPowerKeyTime = event.getDownTime();
            interceptScreenshotChord();
            interceptRingerToggleChord();
        }
        TelecomManager telecomManager = getTelecommService();
        boolean hungUp = false;
        if (telecomManager != null) {
            if (telecomManager.isRinging()) {
                telecomManager.silenceRinger();
            } else if ((this.mIncallPowerBehavior & 2) != 0 && telecomManager.isInCall() && z) {
                hungUp = telecomManager.endCall();
            }
        }
        boolean hungUp2 = hungUp;
        startTime = printTimeoutLog("PowerManager_screenOn", startTime, "telecomManager timeout", DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE);
        GestureLauncherService gestureService = (GestureLauncherService) LocalServices.getService(GestureLauncherService.class);
        hungUp = false;
        if (gestureService != null) {
            boolean gesturedServiceIntercepted = gestureService.interceptPowerKeyDown(event, z, this.mTmpBoolean);
            if (this.mTmpBoolean.value && this.mRequestedOrGoingToSleep) {
                this.mCameraGestureTriggeredDuringGoingToSleep = true;
            }
            startTime = printTimeoutLog("PowerManager_screenOn", startTime, "GestureLauncherService timeout", DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE);
            hungUp = gesturedServiceIntercepted;
        } else {
            KeyEvent keyEvent = event;
        }
        sendSystemKeyToStatusBarAsync(event.getKeyCode());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hungUp=");
        stringBuilder.append(hungUp2);
        stringBuilder.append(", mScreenshotChordVolumeDownKeyTriggered=");
        stringBuilder.append(this.mScreenshotChordVolumeDownKeyTriggered);
        stringBuilder.append(", mA11yShortcutChordVolumeUpKeyTriggered=");
        stringBuilder.append(this.mA11yShortcutChordVolumeUpKeyTriggered);
        stringBuilder.append(", gesturedServiceIntercepted=");
        stringBuilder.append(hungUp);
        Slog.i(str, stringBuilder.toString());
        boolean z2 = hungUp2 || this.mScreenshotChordVolumeDownKeyTriggered || this.mA11yShortcutChordVolumeUpKeyTriggered || hungUp;
        this.mPowerKeyHandled = z2;
        Message msg;
        Message longMsg;
        if (this.mPowerKeyHandled) {
        } else if (z) {
            if (hasLongPressOnPowerBehavior()) {
                if ((event.getFlags() & 128) != 0) {
                    powerLongPress();
                    TelecomManager telecomManager2 = telecomManager;
                } else {
                    msg = this.mHandler.obtainMessage(14);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(msg, 1000);
                    if (hasVeryLongPressOnPowerBehavior()) {
                        longMsg = this.mHandler.obtainMessage(28);
                        longMsg.setAsynchronous(true);
                        this.mHandler.sendMessageDelayed(longMsg, (long) this.mVeryLongPressTimeout);
                    }
                }
                notifyPowerkeyInteractive(true);
            }
            notifyPowerkeyInteractive(true);
        } else {
            wakeUpFromPowerKey(event.getDownTime());
            if (this.mSupportLongPressPowerWhenNonInteractive && hasLongPressOnPowerBehavior()) {
                boolean z3;
                if ((event.getFlags() & 128) != 0) {
                    powerLongPress();
                    z3 = true;
                } else {
                    msg = this.mHandler.obtainMessage(14);
                    z3 = true;
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(msg, 1000);
                    if (hasVeryLongPressOnPowerBehavior()) {
                        longMsg = this.mHandler.obtainMessage(28);
                        longMsg.setAsynchronous(true);
                        this.mHandler.sendMessageDelayed(longMsg, (long) this.mVeryLongPressTimeout);
                    }
                }
                this.mBeganFromNonInteractive = z3;
            } else if (getMaxMultiPressPowerCount() <= 1) {
                this.mPowerKeyHandled = true;
            } else {
                this.mBeganFromNonInteractive = true;
            }
        }
    }

    private void interceptPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        boolean handled = canceled || this.mPowerKeyHandled;
        this.mScreenshotChordPowerKeyTriggered = false;
        cancelPendingScreenshotChordAction();
        cancelPendingPowerKeyAction();
        if (!handled) {
            this.mPowerKeyPressCounter++;
            int maxCount = getMaxMultiPressPowerCount();
            long eventTime = event.getDownTime();
            if (this.mPowerKeyPressCounter < maxCount) {
                Message msg = this.mHandler.obtainMessage(13, interactive, this.mPowerKeyPressCounter, Long.valueOf(eventTime));
                msg.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg, (long) ViewConfiguration.getMultiPressTimeout());
                return;
            }
            powerPress(eventTime, interactive, this.mPowerKeyPressCounter);
        }
        finishPowerKeyPress();
    }

    private void finishPowerKeyPress() {
        this.mBeganFromNonInteractive = false;
        this.mPowerKeyPressCounter = 0;
        if (this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.release();
        }
    }

    private void cancelPendingPowerKeyAction() {
        if (!this.mPowerKeyHandled) {
            this.mPowerKeyHandled = true;
            this.mHandler.removeMessages(14);
        }
        if (hasVeryLongPressOnPowerBehavior()) {
            this.mHandler.removeMessages(28);
        }
    }

    private void cancelPendingBackKeyAction() {
        if (!this.mBackKeyHandled) {
            this.mBackKeyHandled = true;
            this.mHandler.removeMessages(18);
        }
    }

    private void powerPress(long eventTime, boolean interactive, int count) {
        if (!this.mScreenOnEarly || this.mScreenOnFully) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("powerPress: eventTime=");
            stringBuilder.append(eventTime);
            stringBuilder.append(" interactive=");
            stringBuilder.append(interactive);
            stringBuilder.append(" count=");
            stringBuilder.append(count);
            stringBuilder.append(" beganFromNonInteractive=");
            stringBuilder.append(this.mBeganFromNonInteractive);
            stringBuilder.append(" mShortPressOnPowerBehavior=");
            stringBuilder.append(this.mShortPressOnPowerBehavior);
            Slog.d(str, stringBuilder.toString());
            if (count != 2) {
                if (count != 3) {
                    if (interactive && !this.mBeganFromNonInteractive) {
                        switch (this.mShortPressOnPowerBehavior) {
                            case 1:
                                goToSleep(eventTime, 4, 0);
                                break;
                            case 2:
                                goToSleep(eventTime, 4, 1);
                                break;
                            case 3:
                                goToSleep(eventTime, 4, 1);
                                launchHomeFromHotKey();
                                break;
                            case 4:
                                shortPressPowerGoHome();
                                break;
                            case 5:
                                if (!this.mDismissImeOnBackKeyPressed) {
                                    shortPressPowerGoHome();
                                    break;
                                }
                                if (this.mInputMethodManagerInternal == null) {
                                    this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
                                }
                                if (this.mInputMethodManagerInternal != null) {
                                    this.mInputMethodManagerInternal.hideCurrentInputMethod();
                                    break;
                                }
                                break;
                        }
                    }
                }
                powerMultiPressAction(eventTime, interactive, this.mTriplePressOnPowerBehavior);
            } else {
                powerMultiPressAction(eventTime, interactive, this.mDoublePressOnPowerBehavior);
            }
            return;
        }
        Slog.i(TAG, "Suppressed redundant power key press while already in the process of turning the screen on.");
    }

    private void goToSleep(long eventTime, int reason, int flags) {
        this.mRequestedOrGoingToSleep = true;
        this.mPowerManager.goToSleep(eventTime, reason, flags);
    }

    private void shortPressPowerGoHome() {
        launchHomeFromHotKey(true, false);
        if (isKeyguardShowingAndNotOccluded()) {
            this.mKeyguardDelegate.onShortPowerPressedGoHome();
        }
    }

    private void powerMultiPressAction(long eventTime, boolean interactive, int behavior) {
        switch (behavior) {
            case 1:
                if (!isUserSetupComplete()) {
                    Slog.i(TAG, "Ignoring toggling theater mode - device not setup.");
                    return;
                } else if (isTheaterModeEnabled()) {
                    Slog.i(TAG, "Toggling theater mode off.");
                    Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
                    if (!interactive) {
                        wakeUpFromPowerKey(eventTime);
                        return;
                    }
                    return;
                } else {
                    Slog.i(TAG, "Toggling theater mode on.");
                    Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 1);
                    if (this.mGoToSleepOnButtonPressTheaterMode && interactive) {
                        goToSleep(eventTime, 4, 0);
                        return;
                    }
                    return;
                }
            case 2:
                Slog.i(TAG, "Starting brightness boost.");
                if (!interactive) {
                    wakeUpFromPowerKey(eventTime);
                }
                this.mPowerManager.boostScreenBrightness(eventTime);
                return;
            default:
                return;
        }
    }

    private int getMaxMultiPressPowerCount() {
        if (this.mTriplePressOnPowerBehavior != 0) {
            return 3;
        }
        if (this.mDoublePressOnPowerBehavior != 0) {
            return 2;
        }
        return 1;
    }

    private void powerLongPress() {
        int behavior = getResolvedLongPressOnPowerBehavior();
        boolean z = true;
        boolean z2 = false;
        switch (behavior) {
            case 1:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                showGlobalActionsInternal();
                return;
            case 2:
            case 3:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                WindowManagerFuncs windowManagerFuncs = this.mWindowManagerFuncs;
                if (behavior != 2) {
                    z = false;
                }
                windowManagerFuncs.shutdown(z);
                return;
            case 4:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                if (this.mKeyguardDelegate != null) {
                    z2 = this.mKeyguardDelegate.isShowing();
                }
                if (!z2) {
                    Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                    if (this.mAllowStartActivityForLongPressOnPowerDuringSetup) {
                        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                        return;
                    } else {
                        startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                        return;
                    }
                }
                return;
            default:
                return;
        }
    }

    private void powerVeryLongPress() {
        switch (this.mVeryLongPressOnPowerBehavior) {
            case 1:
                this.mPowerKeyHandled = true;
                performHapticFeedbackLw(null, 0, false);
                showGlobalActionsInternal();
                return;
            default:
                return;
        }
    }

    private void backLongPress() {
        this.mBackKeyHandled = true;
        switch (this.mLongPressOnBackBehavior) {
            case 1:
                boolean keyguardActive;
                if (this.mKeyguardDelegate == null) {
                    keyguardActive = false;
                } else {
                    keyguardActive = this.mKeyguardDelegate.isShowing();
                }
                if (!keyguardActive) {
                    startActivityAsUser(new Intent("android.intent.action.VOICE_ASSIST"), UserHandle.CURRENT_OR_SELF);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void accessibilityShortcutActivated() {
        this.mAccessibilityShortcutController.performAccessibilityShortcut();
    }

    private void disposeInputConsumer(InputConsumer inputConsumer) {
        if (inputConsumer != null) {
            inputConsumer.dismiss();
        }
        this.mInputConsumer = null;
    }

    private void sleepPress() {
        if (this.mShortPressOnSleepBehavior == 1) {
            launchHomeFromHotKey(false, true);
        }
    }

    private void sleepRelease(long eventTime) {
        switch (this.mShortPressOnSleepBehavior) {
            case 0:
            case 1:
                Slog.i(TAG, "sleepRelease() calling goToSleep(GO_TO_SLEEP_REASON_SLEEP_BUTTON)");
                goToSleep(eventTime, 6, 0);
                return;
            default:
                return;
        }
    }

    private int getResolvedLongPressOnPowerBehavior() {
        if (FactoryTest.isLongPressOnPowerOffEnabled()) {
            return 3;
        }
        return this.mLongPressOnPowerBehavior;
    }

    private boolean hasLongPressOnPowerBehavior() {
        return getResolvedLongPressOnPowerBehavior() != 0;
    }

    private boolean hasVeryLongPressOnPowerBehavior() {
        return this.mVeryLongPressOnPowerBehavior != 0;
    }

    private boolean hasLongPressOnBackBehavior() {
        return this.mLongPressOnBackBehavior != 0;
    }

    private void interceptScreenshotChord() {
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("takeScreenshot enabled  ");
            stringBuilder.append(this.mScreenshotChordEnabled);
            stringBuilder.append("  VolumeDownKeyTriggered  ");
            stringBuilder.append(this.mScreenshotChordVolumeDownKeyTriggered);
            stringBuilder.append(" PowerKeyTriggered  ");
            stringBuilder.append(this.mScreenshotChordPowerKeyTriggered);
            stringBuilder.append(" VolumeUpKeyTriggered  ");
            stringBuilder.append(this.mA11yShortcutChordVolumeUpKeyTriggered);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mScreenshotChordEnabled && this.mScreenshotChordVolumeDownKeyTriggered && this.mScreenshotChordPowerKeyTriggered && !this.mA11yShortcutChordVolumeUpKeyTriggered) {
            long now = SystemClock.uptimeMillis();
            if (HWFLOW) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("takeScreenshot downKeyTime=  ");
                stringBuilder2.append(this.mScreenshotChordVolumeDownKeyTime);
                stringBuilder2.append(" powerKeyTime=  ");
                stringBuilder2.append(this.mScreenshotChordPowerKeyTime);
                stringBuilder2.append(" now = ");
                stringBuilder2.append(now);
                Slog.i(str2, stringBuilder2.toString());
            }
            if (now <= this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && now <= this.mScreenshotChordPowerKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mScreenshotChordVolumeDownKeyConsumed = true;
                cancelPendingPowerKeyAction();
                this.mScreenshotRunnable.setScreenshotType(1);
                Flog.bdReport(this.mContext, 71);
                this.mHandler.postDelayed(this.mScreenshotRunnable, getScreenshotChordLongPressDelay());
            }
        }
    }

    private void interceptAccessibilityShortcutChord() {
        if (this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(isKeyguardLocked()) && this.mScreenshotChordVolumeDownKeyTriggered && this.mA11yShortcutChordVolumeUpKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
            long now = SystemClock.uptimeMillis();
            if (now <= this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS && now <= this.mA11yShortcutChordVolumeUpKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                this.mScreenshotChordVolumeDownKeyConsumed = true;
                this.mA11yShortcutChordVolumeUpKeyConsumed = true;
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(20), getAccessibilityShortcutTimeout());
            }
        }
    }

    private void interceptRingerToggleChord() {
    }

    private long getAccessibilityShortcutTimeout() {
        ViewConfiguration config = ViewConfiguration.get(this.mContext);
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_shortcut_dialog_shown", 0, this.mCurrentUserId) == 0) {
            return config.getAccessibilityShortcutKeyTimeout();
        }
        return config.getAccessibilityShortcutKeyTimeoutAfterConfirmation();
    }

    private long getScreenshotChordLongPressDelay() {
        if (this.mKeyguardDelegate.isShowing()) {
            return (long) (KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER * ((float) ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout()));
        }
        return 0;
    }

    private long getRingerToggleChordDelay() {
        return (long) ViewConfiguration.getTapTimeout();
    }

    private void cancelPendingScreenshotChordAction() {
        if (HWFLOW) {
            Slog.i(TAG, "takeScreenshot cancelPendingScreenshotChordAction");
        }
        this.mHandler.removeCallbacks(this.mScreenshotRunnable);
    }

    private void cancelPendingAccessibilityShortcutAction() {
        this.mHandler.removeMessages(20);
    }

    private void cancelPendingRingerToggleChordAction() {
        this.mHandler.removeMessages(30);
    }

    public void showGlobalActions() {
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    void showGlobalActionsInternal() {
        if (HwDeviceManager.disallowOp(37)) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(PhoneWindowManager.this.mContext, 33685954, 0);
                    toast.getWindowParams().type = 2010;
                    LayoutParams windowParams = toast.getWindowParams();
                    windowParams.privateFlags |= 16;
                    toast.show();
                }
            });
        } else if (!HwPolicyFactory.ifUseHwGlobalActions()) {
            if (this.mGlobalActions == null) {
                this.mGlobalActions = new GlobalActions(this.mContext, this.mWindowManagerFuncs);
            }
            boolean keyguardShowing = isKeyguardShowingAndNotOccluded();
            this.mGlobalActions.showDialog(keyguardShowing, isDeviceProvisioned());
            if (keyguardShowing) {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
            }
        } else if (!isEmergencySoSActivity()) {
            HwPolicyFactory.showHwGlobalActionsFragment(this.mContext, this.mWindowManagerFuncs, this.mPowerManager, isKeyguardShowingAndNotOccluded(), isKeyguardSecure(this.mCurrentUserId), isDeviceProvisioned());
        }
    }

    private boolean isEmergencySoSActivity() {
        ActivityManagerService am = (ActivityManagerService) ServiceManager.getService("activity");
        if (am == null || ((!ACTIVITY_NAME_EMERGENCY_COUNTDOWN.equals(am.topAppName()) && !ACTIVITY_NAME_EMERGENCY_NUMBER.equals(am.topAppName())) || this.mFocusedWindow == null || (this.mFocusedWindow.getAttrs().hwFlags & Integer.MIN_VALUE) != Integer.MIN_VALUE)) {
            return false;
        }
        Log.d(TAG, "Emergency power focusWindow is Emergency");
        return true;
    }

    boolean isDeviceProvisioned() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    boolean isUserSetupComplete() {
        boolean z = false;
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0) {
            z = true;
        }
        boolean isSetupComplete = z;
        if (this.mHasFeatureLeanback) {
            return isSetupComplete & isTvUserSetupComplete();
        }
        return isSetupComplete;
    }

    private boolean isTvUserSetupComplete() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "tv_user_setup_complete", 0, -2) != 0;
    }

    private void handleShortPressOnHome() {
        HdmiControl hdmiControl = getHdmiControl();
        if (hdmiControl != null) {
            hdmiControl.turnOnTv();
        }
        if (this.mDreamManagerInternal == null || !this.mDreamManagerInternal.isDreaming()) {
            launchHomeFromHotKey();
        } else {
            this.mDreamManagerInternal.stopDream(false);
        }
    }

    private HdmiControl getHdmiControl() {
        if (this.mHdmiControl == null) {
            if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.hdmi.cec")) {
                return null;
            }
            HdmiControlManager manager = (HdmiControlManager) this.mContext.getSystemService("hdmi_control");
            HdmiPlaybackClient client = null;
            if (manager != null) {
                client = manager.getPlaybackClient();
            }
            this.mHdmiControl = new HdmiControl(client, null);
        }
        return this.mHdmiControl;
    }

    private void handleLongPressOnHome(int deviceId) {
        if (this.mLongPressOnHomeBehavior != 0) {
            this.mHomeConsumed = true;
            performHapticFeedbackLw(null, 0, false);
            switch (this.mLongPressOnHomeBehavior) {
                case 1:
                    launchAllAppsAction();
                    break;
                case 2:
                    launchAssistAction(null, deviceId);
                    break;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Undefined home long press behavior: ");
                    stringBuilder.append(this.mLongPressOnHomeBehavior);
                    Log.w(str, stringBuilder.toString());
                    break;
            }
        }
    }

    private void launchAllAppsAction() {
        Intent intent = new Intent("android.intent.action.ALL_APPS");
        if (this.mHasFeatureLeanback) {
            PackageManager pm = this.mContext.getPackageManager();
            Intent intentLauncher = new Intent("android.intent.action.MAIN");
            intentLauncher.addCategory("android.intent.category.HOME");
            ResolveInfo resolveInfo = pm.resolveActivityAsUser(intentLauncher, DumpState.DUMP_DEXOPT, this.mCurrentUserId);
            if (resolveInfo != null) {
                intent.setPackage(resolveInfo.activityInfo.packageName);
            }
        }
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private void handleDoubleTapOnHome() {
        if (this.mDoubleTapOnHomeBehavior == 1) {
            this.mHomeConsumed = true;
            toggleRecentApps();
        }
    }

    private void showPictureInPictureMenu(KeyEvent event) {
        this.mHandler.removeMessages(17);
        Message msg = this.mHandler.obtainMessage(17);
        msg.setAsynchronous(true);
        msg.sendToTarget();
    }

    private void showPictureInPictureMenuInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.showPictureInPictureMenu();
        }
    }

    private boolean isRoundWindow() {
        return this.mContext.getResources().getConfiguration().isScreenRound();
    }

    public void init(Context context, IWindowManager windowManager, WindowManagerFuncs windowManagerFuncs) {
        Context context2 = context;
        this.mContext = context2;
        this.mWindowManager = windowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mHasFeatureWatch = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        this.mHasFeatureLeanback = this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mAccessibilityShortcutController = new AccessibilityShortcutController(this.mContext, new Handler(), this.mCurrentUserId);
        this.mLogger = new MetricsLogger();
        boolean burnInProtectionEnabled = context.getResources().getBoolean(17956950);
        boolean burnInProtectionDevMode = SystemProperties.getBoolean("persist.debug.force_burn_in", false);
        if (burnInProtectionEnabled || burnInProtectionDevMode) {
            int minHorizontal;
            int maxHorizontal;
            int minVertical;
            int maxVertical;
            int maxRadius;
            if (burnInProtectionDevMode) {
                minHorizontal = -8;
                maxHorizontal = 8;
                minVertical = -8;
                maxVertical = -4;
                maxRadius = isRoundWindow() ? 6 : -1;
            } else {
                Resources resources = context.getResources();
                int minHorizontal2 = resources.getInteger(17694750);
                int maxHorizontal2 = resources.getInteger(17694747);
                int minVertical2 = resources.getInteger(17694751);
                int maxVertical2 = resources.getInteger(17694749);
                maxRadius = resources.getInteger(17694748);
                minHorizontal = minHorizontal2;
                maxHorizontal = maxHorizontal2;
                minVertical = minVertical2;
                maxVertical = maxVertical2;
            }
            BurnInProtectionHelper burnInProtectionHelper = r2;
            BurnInProtectionHelper burnInProtectionHelper2 = new BurnInProtectionHelper(context2, minHorizontal, maxHorizontal, minVertical, maxVertical, maxRadius);
            this.mBurnInProtectionHelper = burnInProtectionHelper;
        }
        this.mHandler = new PolicyHandler(this, null);
        this.mWakeGestureListener = new MyWakeGestureListener(this.mContext, this.mHandler);
        this.mOrientationListener = new MyOrientationListener(this.mContext, this.mHandler);
        try {
            this.mOrientationListener.setCurrentRotation(windowManager.getDefaultDisplayRotation());
        } catch (RemoteException e) {
        }
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        if (mSupportAod) {
            Slog.i(TAG, "AOD mAodSwitchObserver.observe");
            this.mAodSwitchObserver = new AodSwitchObserver(this.mHandler);
            this.mAodSwitchObserver.observe();
        }
        this.mShortcutManager = new ShortcutManager(context2);
        this.mUiMode = context.getResources().getInteger(17694773);
        this.mHomeIntent = new Intent("android.intent.action.MAIN", null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270533120);
        this.mEnableCarDockHomeCapture = context.getResources().getBoolean(17956951);
        this.mCarDockIntent = new Intent("android.intent.action.MAIN", null);
        this.mCarDockIntent.addCategory("android.intent.category.CAR_DOCK");
        this.mCarDockIntent.addFlags(270532608);
        this.mDeskDockIntent = new Intent("android.intent.action.MAIN", null);
        this.mDeskDockIntent.addCategory("android.intent.category.DESK_DOCK");
        this.mDeskDockIntent.addFlags(270532608);
        this.mVrHeadsetHomeIntent = new Intent("android.intent.action.MAIN", null);
        this.mVrHeadsetHomeIntent.addCategory("android.intent.category.VR_HOME");
        this.mVrHeadsetHomeIntent.addFlags(270532608);
        this.mPowerManager = (PowerManager) context2.getSystemService("power");
        boolean z = true;
        this.mBroadcastWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mBroadcastWakeLock");
        this.mPowerKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mPowerKeyWakeLock");
        this.mEnableShiftMenuBugReports = "1".equals(SystemProperties.get("ro.debuggable"));
        this.mSupportAutoRotation = this.mContext.getResources().getBoolean(17957033);
        this.mLidOpenRotation = readRotation(17694797);
        this.mCarDockRotation = readRotation(17694755);
        this.mDeskDockRotation = readRotation(17694776);
        this.mUndockedHdmiRotation = readRotation(17694876);
        this.mCarDockEnablesAccelerometer = this.mContext.getResources().getBoolean(17956910);
        this.mDeskDockEnablesAccelerometer = this.mContext.getResources().getBoolean(17956923);
        this.mLidKeyboardAccessibility = this.mContext.getResources().getInteger(17694795);
        this.mLidNavigationAccessibility = this.mContext.getResources().getInteger(17694796);
        this.mLidControlsScreenLock = this.mContext.getResources().getBoolean(17956986);
        this.mLidControlsSleep = this.mContext.getResources().getBoolean(17956987);
        this.mTranslucentDecorEnabled = this.mContext.getResources().getBoolean(17956966);
        this.mAllowTheaterModeWakeFromKey = this.mContext.getResources().getBoolean(17956880);
        boolean z2 = this.mAllowTheaterModeWakeFromKey || this.mContext.getResources().getBoolean(17956884);
        this.mAllowTheaterModeWakeFromPowerKey = z2;
        this.mAllowTheaterModeWakeFromMotion = this.mContext.getResources().getBoolean(17956882);
        this.mAllowTheaterModeWakeFromMotionWhenNotDreaming = this.mContext.getResources().getBoolean(17956883);
        this.mAllowTheaterModeWakeFromCameraLens = this.mContext.getResources().getBoolean(17956877);
        this.mAllowTheaterModeWakeFromLidSwitch = this.mContext.getResources().getBoolean(17956881);
        this.mAllowTheaterModeWakeFromWakeGesture = this.mContext.getResources().getBoolean(17956879);
        this.mGoToSleepOnButtonPressTheaterMode = this.mContext.getResources().getBoolean(17956978);
        this.mSupportLongPressPowerWhenNonInteractive = this.mContext.getResources().getBoolean(17957036);
        this.mLongPressOnBackBehavior = this.mContext.getResources().getInteger(17694800);
        this.mShortPressOnPowerBehavior = this.mContext.getResources().getInteger(17694864);
        this.mLongPressOnPowerBehavior = this.mContext.getResources().getInteger(17694802);
        this.mVeryLongPressOnPowerBehavior = this.mContext.getResources().getInteger(17694878);
        this.mDoublePressOnPowerBehavior = this.mContext.getResources().getInteger(17694778);
        this.mTriplePressOnPowerBehavior = this.mContext.getResources().getInteger(17694875);
        this.mShortPressOnSleepBehavior = this.mContext.getResources().getInteger(17694865);
        this.mVeryLongPressTimeout = this.mContext.getResources().getInteger(17694879);
        this.mAllowStartActivityForLongPressOnPowerDuringSetup = this.mContext.getResources().getBoolean(17956876);
        if (AudioSystem.getPlatformType(this.mContext) != 2) {
            z = false;
        }
        this.mUseTvRouting = z;
        this.mHandleVolumeKeysInWM = this.mContext.getResources().getBoolean(17956980);
        readConfigurationDependentBehaviors();
        this.mAccessibilityManager = (AccessibilityManager) context2.getSystemService("accessibility");
        IntentFilter filter = new IntentFilter();
        filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_ENTER_DESK_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_DESK_MODE);
        filter.addAction("android.intent.action.DOCK_EVENT");
        Intent intent = context2.registerReceiver(this.mDockReceiver, filter);
        context2.registerReceiver(this.mShutdownReceiver, new IntentFilter(ACTION_ACTURAL_SHUTDOWN), HUAWEI_SHUTDOWN_PERMISSION, null);
        if (intent != null) {
            this.mDockMode = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
        }
        filter = new IntentFilter();
        filter.addAction("android.intent.action.DREAMING_STARTED");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        context2.registerReceiver(this.mDreamReceiver, filter);
        context2.registerReceiver(this.mMultiuserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        this.mSystemGestures = new SystemGesturesPointerEventListener(context2, new Callbacks() {
            public void onSwipeFromTop() {
                if (!(PhoneWindowManager.this.isGestureIsolated() || Secure.getInt(PhoneWindowManager.this.mContext.getContentResolver(), "device_provisioned", 1) == 0 || PhoneWindowManager.this.swipeFromTop() || PhoneWindowManager.this.mStatusBar == null)) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mStatusBar);
                }
            }

            public void onSwipeFromBottom() {
                if (!(PhoneWindowManager.this.isGestureIsolated() || Secure.getInt(PhoneWindowManager.this.mContext.getContentResolver(), "device_provisioned", 1) == 0 || PhoneWindowManager.this.swipeFromBottom() || PhoneWindowManager.this.mNavigationBar == null || PhoneWindowManager.this.mNavigationBarPosition != 4)) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
            }

            public void onSwipeFromRight() {
                if (!(PhoneWindowManager.this.isGestureIsolated() || PhoneWindowManager.this.swipeFromRight() || PhoneWindowManager.this.mNavigationBar == null || PhoneWindowManager.this.mNavigationBarPosition != 2)) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
            }

            public void onSwipeFromLeft() {
                if (PhoneWindowManager.this.mNavigationBar != null && PhoneWindowManager.this.mNavigationBarPosition == 1) {
                    PhoneWindowManager.this.requestTransientBars(PhoneWindowManager.this.mNavigationBar);
                }
            }

            public void onFling(int duration) {
                if (PhoneWindowManager.this.mPowerManagerInternal != null) {
                    PhoneWindowManager.this.mPowerManagerInternal.powerHint(2, duration);
                }
            }

            public void onDebug() {
            }

            public void onDown() {
                PhoneWindowManager.this.mOrientationListener.onTouchStart();
                PhoneWindowManager.this.onPointDown();
            }

            public void onUpOrCancel() {
                PhoneWindowManager.this.mOrientationListener.onTouchEnd();
            }

            public void onMouseHoverAtTop() {
                PhoneWindowManager.this.mHandler.removeMessages(16);
                Message msg = PhoneWindowManager.this.mHandler.obtainMessage(16);
                msg.arg1 = 0;
                PhoneWindowManager.this.mHandler.sendMessageDelayed(msg, 500);
            }

            public void onMouseHoverAtBottom() {
                PhoneWindowManager.this.mHandler.removeMessages(16);
                Message msg = PhoneWindowManager.this.mHandler.obtainMessage(16);
                msg.arg1 = 1;
                PhoneWindowManager.this.mHandler.sendMessageDelayed(msg, 500);
            }

            public void onMouseLeaveFromEdge() {
                PhoneWindowManager.this.mHandler.removeMessages(16);
            }
        });
        this.mImmersiveModeConfirmation = new ImmersiveModeConfirmation(this.mContext);
        this.mWindowManagerFuncs.registerPointerEventListener(this.mSystemGestures);
        this.mVibrator = (Vibrator) context2.getSystemService("vibrator");
        this.mLongPressVibePattern = getLongIntArray(this.mContext.getResources(), 17236015);
        this.mCalendarDateVibePattern = getLongIntArray(this.mContext.getResources(), 17235990);
        this.mSafeModeEnabledVibePattern = getLongIntArray(this.mContext.getResources(), 17236030);
        this.mScreenshotChordEnabled = this.mContext.getResources().getBoolean(17956965);
        this.mGlobalKeyManager = new GlobalKeyManager(this.mContext);
        initializeHdmiState();
        if (!this.mPowerManager.isInteractive()) {
            startedGoingToSleep(2);
            finishedGoingToSleep(2);
        }
        this.mWindowManagerInternal.registerAppTransitionListener(this.mStatusBarController.getAppTransitionListener());
        this.mWindowManagerInternal.registerAppTransitionListener(new AppTransitionListener() {
            public int onAppTransitionStartingLocked(int transit, IBinder openToken, IBinder closeToken, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
                return PhoneWindowManager.this.handleStartTransitionForKeyguardLw(transit, duration);
            }

            public void onAppTransitionCancelledLocked(int transit) {
                PhoneWindowManager.this.handleStartTransitionForKeyguardLw(transit, 0);
            }
        });
        this.mKeyguardDelegate = new KeyguardServiceDelegate(this.mContext, new StateCallback() {
            public void onTrustedChanged() {
                PhoneWindowManager.this.mWindowManagerFuncs.notifyKeyguardTrustedChanged();
            }

            public void onShowingChanged() {
                PhoneWindowManager.this.mWindowManagerFuncs.onKeyguardShowingAndNotOccludedChanged();
            }
        });
        this.mScreenshotHelper = new ScreenshotHelper(this.mContext);
        hwInit();
    }

    private void readConfigurationDependentBehaviors() {
        Resources res = this.mContext.getResources();
        this.mLongPressOnHomeBehavior = res.getInteger(17694801);
        if (this.mLongPressOnHomeBehavior < 0 || this.mLongPressOnHomeBehavior > 2) {
            this.mLongPressOnHomeBehavior = 0;
        }
        this.mDoubleTapOnHomeBehavior = res.getInteger(17694779);
        if (this.mDoubleTapOnHomeBehavior < 0 || this.mDoubleTapOnHomeBehavior > 1) {
            this.mDoubleTapOnHomeBehavior = 0;
        }
        this.mShortPressOnWindowBehavior = 0;
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.picture_in_picture")) {
            this.mShortPressOnWindowBehavior = 1;
        }
        this.mNavBarOpacityMode = res.getInteger(17694824);
    }

    public void setInitialDisplaySize(Display display, int width, int height, int density) {
        int i = width;
        int i2 = height;
        if (this.mContext == null || display.getDisplayId() != 0) {
            Display display2 = display;
            return;
        }
        int shortSize;
        int longSize;
        this.mDisplay = display;
        Resources res = this.mContext.getResources();
        if (i > i2) {
            shortSize = i2;
            longSize = i;
            this.mLandscapeRotation = 0;
            this.mSeascapeRotation = 2;
            if (res.getBoolean(17957010)) {
                this.mPortraitRotation = 1;
                this.mUpsideDownRotation = 3;
            } else {
                this.mPortraitRotation = 3;
                this.mUpsideDownRotation = 1;
            }
        } else {
            shortSize = i;
            longSize = i2;
            this.mPortraitRotation = 0;
            this.mUpsideDownRotation = 2;
            if (res.getBoolean(17957010)) {
                this.mLandscapeRotation = 3;
                this.mSeascapeRotation = 1;
            } else {
                this.mLandscapeRotation = 1;
                this.mSeascapeRotation = 3;
            }
        }
        int shortSizeDp = (shortSize * 160) / density;
        int longSizeDp = (longSize * 160) / density;
        boolean z = i != i2 && shortSizeDp <= System.getInt(this.mContext.getContentResolver(), "hw_split_navigation_bar_dp", 600);
        this.mNavigationBarCanMove = z;
        if (this.mNavigationBarCanMove) {
            int defaultRotation = SystemProperties.getInt("ro.panel.hw_orientation", 0) / 90;
            if (defaultRotation == 1 || defaultRotation == 3) {
                this.mNavigationBarCanMove = false;
            }
        }
        this.mHasNavigationBar = res.getBoolean(17957019);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            this.mHasNavigationBar = false;
        } else if ("0".equals(navBarOverride)) {
            this.mHasNavigationBar = true;
        }
        if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
            this.mDemoHdmiRotation = this.mPortraitRotation;
        } else {
            this.mDemoHdmiRotation = this.mLandscapeRotation;
        }
        this.mDemoHdmiRotationLock = SystemProperties.getBoolean("persist.demo.hdmirotationlock", false);
        if ("portrait".equals(SystemProperties.get("persist.demo.remoterotation"))) {
            this.mDemoRotation = this.mPortraitRotation;
        } else {
            this.mDemoRotation = this.mLandscapeRotation;
        }
        this.mDemoRotationLock = SystemProperties.getBoolean("persist.demo.rotationlock", false);
        boolean z2 = ((longSizeDp >= 960 && shortSizeDp >= 720) || this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive") || this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) && res.getBoolean(17956975) && !"true".equals(SystemProperties.get("config.override_forced_orient"));
        this.mForceDefaultOrientation = z2;
    }

    private boolean canHideNavigationBar() {
        return this.mHasNavigationBar;
    }

    public boolean isDefaultOrientationForced() {
        return this.mForceDefaultOrientation;
    }

    public void updateSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean updateRotation = false;
        synchronized (this.mLock) {
            int i = 2;
            this.mEndcallBehavior = System.getIntForUser(resolver, "end_button_behavior", 2, -2);
            this.mIncallPowerBehavior = Secure.getIntForUser(resolver, "incall_power_button_behavior", 1, -2);
            boolean z = false;
            this.mIncallBackBehavior = Secure.getIntForUser(resolver, "incall_back_button_behavior", 0, -2);
            this.mSystemNavigationKeysEnabled = Secure.getIntForUser(resolver, "system_navigation_keys_enabled", 0, -2) == 1;
            this.mRingerToggleChord = Secure.getIntForUser(resolver, "volume_hush_gesture", 0, -2);
            if (!this.mContext.getResources().getBoolean(17957068)) {
                this.mRingerToggleChord = 0;
            }
            int showRotationSuggestions = Secure.getIntForUser(resolver, "show_rotation_suggestions", 1, -2);
            if (this.mShowRotationSuggestions != showRotationSuggestions) {
                this.mShowRotationSuggestions = showRotationSuggestions;
                updateOrientationListenerLp();
            }
            boolean wakeGestureEnabledSetting = Secure.getIntForUser(resolver, "wake_gesture_enabled", 0, -2) != 0;
            if (this.mWakeGestureEnabledSetting != wakeGestureEnabledSetting) {
                this.mWakeGestureEnabledSetting = wakeGestureEnabledSetting;
                updateWakeGestureListenerLp();
            }
            int userRotation = System.getIntForUser(resolver, "user_rotation", 0, -2);
            if (this.mUserRotation != userRotation) {
                this.mUserRotation = userRotation;
                updateRotation = true;
            }
            int userRotationMode = System.getIntForUser(resolver, "accelerometer_rotation", 0, -2) != 0 ? 0 : 1;
            if (this.mUserRotationMode != userRotationMode) {
                this.mUserRotationMode = userRotationMode;
                updateRotation = true;
                updateOrientationListenerLp();
            }
            if (this.mSystemReady) {
                int pointerLocation = System.getIntForUser(resolver, "pointer_location", 0, -2);
                if (this.mPointerLocationMode != pointerLocation) {
                    this.mPointerLocationMode = pointerLocation;
                    Handler handler = this.mHandler;
                    if (pointerLocation != 0) {
                        i = 1;
                    }
                    handler.sendEmptyMessage(i);
                }
            }
            this.mLockScreenTimeout = System.getIntForUser(resolver, "screen_off_timeout", 0, -2);
            String imId = Secure.getStringForUser(resolver, "default_input_method", -2);
            boolean hasSoftInput = imId != null && imId.length() > 0;
            if (this.mHasSoftInput != hasSoftInput) {
                this.mHasSoftInput = hasSoftInput;
                updateRotation = true;
            }
            if (this.mImmersiveModeConfirmation != null) {
                this.mImmersiveModeConfirmation.loadSetting(this.mCurrentUserId);
            }
            if (Secure.getIntForUser(resolver, FINGER_PRINT_ENABLE, 0, -2) == 1) {
                z = true;
            }
            this.mIsFingerprintEnabledBySettings = z;
        }
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            PolicyControl.reloadFromSetting(this.mContext);
        }
        if (updateRotation) {
            updateRotation(true);
        }
    }

    private void updateWakeGestureListenerLp() {
        if (shouldEnableWakeGestureLp()) {
            this.mWakeGestureListener.requestWakeUpTrigger();
        } else {
            this.mWakeGestureListener.cancelWakeUpTrigger();
        }
    }

    private boolean shouldEnableWakeGestureLp() {
        return this.mWakeGestureEnabledSetting && !this.mAwake && (!(this.mLidControlsSleep && this.mLidState == 0) && this.mWakeGestureListener.isSupported());
    }

    private void enablePointerLocation() {
        if (this.mPointerLocationView == null) {
            this.mPointerLocationView = new PointerLocationView(this.mContext);
            this.mPointerLocationView.setPrintCoords(false);
            LayoutParams lp = new LayoutParams(-1, -1);
            lp.type = 2015;
            lp.flags = 1304;
            lp.layoutInDisplayCutoutMode = 1;
            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= DumpState.DUMP_SERVICE_PERMISSIONS;
                lp.privateFlags |= 2;
            }
            lp.format = -3;
            lp.setTitle("PointerLocation");
            WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
            lp.inputFeatures |= 2;
            wm.addView(this.mPointerLocationView, lp);
            this.mWindowManagerFuncs.registerPointerEventListener(this.mPointerLocationView);
        }
    }

    private void disablePointerLocation() {
        if (this.mPointerLocationView != null) {
            this.mWindowManagerFuncs.unregisterPointerEventListener(this.mPointerLocationView);
            ((WindowManager) this.mContext.getSystemService("window")).removeView(this.mPointerLocationView);
            this.mPointerLocationView = null;
        }
    }

    private int readRotation(int resID) {
        try {
            int rotation = this.mContext.getResources().getInteger(resID);
            if (rotation == 0) {
                return 0;
            }
            if (rotation == 90) {
                return 1;
            }
            if (rotation == 180) {
                return 2;
            }
            if (rotation == 270) {
                return 3;
            }
            return -1;
        } catch (NotFoundException e) {
        }
    }

    public int checkAddPermission(LayoutParams attrs, int[] outAppOp) {
        int type = attrs.type;
        int i = 0;
        if (((attrs.privateFlags & DumpState.DUMP_DEXOPT) != 0) && this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0) {
            return -8;
        }
        outAppOp[0] = -1;
        if ((type < 1 || type > 99) && ((type < 1000 || type > 1999) && (type < IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || type > 2999))) {
            return -10;
        }
        if (type < IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || type > 2999) {
            return 0;
        }
        if (LayoutParams.isSystemAlertWindowType(type)) {
            outAppOp[0] = 24;
            int callingUid = Binder.getCallingUid();
            if (UserHandle.getAppId(callingUid) == 1000) {
                return 0;
            }
            ApplicationInfo appInfo;
            try {
                appInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(attrs.packageName, 0, UserHandle.getUserId(callingUid));
            } catch (NameNotFoundException e) {
                appInfo = null;
            }
            if (HwSystemManager.checkWindowType(attrs.privateFlags) && appInfo != null && appInfo.targetSdkVersion < 26) {
                return 0;
            }
            if (appInfo == null || (type != 2038 && appInfo.targetSdkVersion >= 26)) {
                Slog.w(TAG, "This alert window type is deprecated, Pls use TYPE_APPLICATION_OVERLAY type.");
                if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0) {
                    i = -8;
                }
                return i;
            }
            switch (this.mAppOpsManager.noteOpNoThrow(outAppOp[0], callingUid, attrs.packageName)) {
                case 0:
                case 1:
                    return 0;
                case 2:
                    if (appInfo.targetSdkVersion < 23) {
                        return 0;
                    }
                    return -8;
                default:
                    if (this.mContext.checkCallingOrSelfPermission("android.permission.SYSTEM_ALERT_WINDOW") != 0) {
                        i = -8;
                    }
                    return i;
            }
        } else if (type != 2005) {
            if (!(type == 2011 || type == 2013 || type == 2023 || type == 2035 || type == 2037)) {
                switch (type) {
                    case 2030:
                    case 2031:
                    case 2032:
                        break;
                    default:
                        switch (type) {
                            case 2102:
                            case 2103:
                                break;
                            default:
                                if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0) {
                                    i = -8;
                                }
                                return i;
                        }
                }
            }
            return 0;
        } else {
            outAppOp[0] = 45;
            return 0;
        }
    }

    public boolean checkShowToOwnerOnly(LayoutParams attrs) {
        int i = attrs.type;
        boolean z = true;
        if (!(i == 3 || i == 2014 || i == 2024 || i == 2030 || i == 2034 || i == 2037)) {
            switch (i) {
                case IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME /*2000*/:
                case 2001:
                case 2002:
                    break;
                default:
                    switch (i) {
                        case 2007:
                        case 2008:
                        case 2009:
                            break;
                        default:
                            switch (i) {
                                case 2017:
                                case 2018:
                                case 2019:
                                case 2020:
                                case 2021:
                                case 2022:
                                    break;
                                default:
                                    switch (i) {
                                        case 2026:
                                        case 2027:
                                            break;
                                        default:
                                            if ((attrs.privateFlags & 16) == 0) {
                                                return true;
                                            }
                                            break;
                                    }
                            }
                    }
            }
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0) {
            z = false;
        }
        return z;
    }

    public void adjustWindowParamsLw(WindowState win, LayoutParams attrs, boolean hasStatusBarServicePermission) {
        boolean isScreenDecor = (attrs.privateFlags & DumpState.DUMP_CHANGES) != 0;
        if (this.mScreenDecorWindows.contains(win)) {
            if (!isScreenDecor) {
                this.mScreenDecorWindows.remove(win);
            }
        } else if (isScreenDecor && hasStatusBarServicePermission) {
            this.mScreenDecorWindows.add(win);
        }
        int i = attrs.type;
        if (i != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            if (i != 2013) {
                if (i != 2015) {
                    if (i != 2023) {
                        if (i != 2036) {
                            switch (i) {
                                case 2005:
                                    if (attrs.hideTimeoutMilliseconds < 0 || attrs.hideTimeoutMilliseconds > 3500) {
                                        attrs.hideTimeoutMilliseconds = 3500;
                                    }
                                    attrs.windowAnimations = 16973828;
                                    break;
                                case 2006:
                                    break;
                            }
                        }
                        attrs.flags |= 8;
                    }
                }
                attrs.flags |= 24;
                attrs.flags &= -262145;
            }
            attrs.layoutInDisplayCutoutMode = 1;
        } else if (this.mKeyguardOccluded) {
            attrs.flags &= -1048577;
            attrs.privateFlags &= -1025;
        }
        if (attrs.type != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            attrs.privateFlags &= -1025;
        }
    }

    private int getImpliedSysUiFlagsForLayout(LayoutParams attrs) {
        int impliedFlags = 0;
        if ((attrs.flags & Integer.MIN_VALUE) != 0) {
            impliedFlags = 0 | 512;
        }
        boolean forceWindowDrawsStatusBarBackground = (attrs.privateFlags & 131072) != 0;
        if ((Integer.MIN_VALUE & attrs.flags) != 0 || (forceWindowDrawsStatusBarBackground && attrs.height == -1 && attrs.width == -1)) {
            return impliedFlags | 1024;
        }
        return impliedFlags;
    }

    void readLidState() {
        this.mLidState = this.mWindowManagerFuncs.getLidState();
    }

    private void readCameraLensCoverState() {
        this.mCameraLensCoverState = this.mWindowManagerFuncs.getCameraLensCoverState();
    }

    private boolean isHidden(int accessibilityMode) {
        boolean z = true;
        switch (accessibilityMode) {
            case 1:
                if (this.mLidState != 0) {
                    z = false;
                }
                return z;
            case 2:
                if (this.mLidState != 1) {
                    z = false;
                }
                return z;
            default:
                return false;
        }
    }

    public void adjustConfigurationLw(Configuration config, int keyboardPresence, int navigationPresence) {
        this.mHaveBuiltInKeyboard = (keyboardPresence & 1) != 0;
        readConfigurationDependentBehaviors();
        readLidState();
        if (config.keyboard == 1 || (keyboardPresence == 1 && isHidden(this.mLidKeyboardAccessibility))) {
            config.hardKeyboardHidden = 2;
            if (!this.mHasSoftInput) {
                config.keyboardHidden = 2;
            }
        }
        if (config.navigation == 1 || (navigationPresence == 1 && isHidden(this.mLidNavigationAccessibility))) {
            config.navigationHidden = 2;
        }
    }

    public boolean getLayoutBeyondDisplayCutout() {
        return this.mLayoutBeyondDisplayCutout;
    }

    public void onOverlayChangedLw() {
        onConfigurationChanged();
    }

    public void onConfigurationChanged() {
        Resources res = getSystemUiContext().getResources();
        int[] iArr = this.mStatusBarHeightForRotation;
        int i = this.mPortraitRotation;
        int[] iArr2 = this.mStatusBarHeightForRotation;
        int i2 = this.mUpsideDownRotation;
        int dimensionPixelSize = res.getDimensionPixelSize(17105320);
        iArr2[i2] = dimensionPixelSize;
        iArr[i] = dimensionPixelSize;
        iArr = this.mStatusBarHeightForRotation;
        i = this.mLandscapeRotation;
        iArr2 = this.mStatusBarHeightForRotation;
        i2 = this.mSeascapeRotation;
        dimensionPixelSize = res.getDimensionPixelSize(17105319);
        iArr2[i2] = dimensionPixelSize;
        iArr[i] = dimensionPixelSize;
        iArr = this.mNavigationBarHeightForRotationDefault;
        i = this.mPortraitRotation;
        iArr2 = this.mNavigationBarHeightForRotationDefault;
        i2 = this.mUpsideDownRotation;
        int dimensionPixelSize2 = res.getDimensionPixelSize(17105186);
        iArr2[i2] = dimensionPixelSize2;
        iArr[i] = dimensionPixelSize2;
        iArr = this.mNavigationBarHeightForRotationDefault;
        i = this.mLandscapeRotation;
        iArr2 = this.mNavigationBarHeightForRotationDefault;
        i2 = this.mSeascapeRotation;
        dimensionPixelSize2 = res.getDimensionPixelSize(17105188);
        iArr2[i2] = dimensionPixelSize2;
        iArr[i] = dimensionPixelSize2;
        iArr = this.mNavigationBarWidthForRotationDefault;
        i = this.mPortraitRotation;
        iArr2 = this.mNavigationBarWidthForRotationDefault;
        i2 = this.mUpsideDownRotation;
        int[] iArr3 = this.mNavigationBarWidthForRotationDefault;
        int i3 = this.mLandscapeRotation;
        int[] iArr4 = this.mNavigationBarWidthForRotationDefault;
        int i4 = this.mSeascapeRotation;
        int dimensionPixelSize3 = res.getDimensionPixelSize(17105191);
        iArr4[i4] = dimensionPixelSize3;
        iArr3[i3] = dimensionPixelSize3;
        iArr2[i2] = dimensionPixelSize3;
        iArr[i] = dimensionPixelSize3;
        this.mDefaultNavBarHeight = res.getDimensionPixelSize(17105186);
    }

    @VisibleForTesting
    Context getSystemUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    public int getMaxWallpaperLayer() {
        return getWindowLayerFromTypeLw(IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME);
    }

    private int getNavigationBarWidth(int rotation, int uiMode) {
        return this.mNavigationBarWidthForRotationDefault[rotation];
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        int width = fullWidth;
        if (displayId == 0 && this.mHasNavigationBar && this.mNavigationBarCanMove && fullWidth > fullHeight) {
            width -= getNavigationBarWidth(rotation, uiMode);
        }
        if (displayCutout != null) {
            return width - (displayCutout.getSafeInsetLeft() + displayCutout.getSafeInsetRight());
        }
        return width;
    }

    private int getNavigationBarHeight(int rotation, int uiMode) {
        return this.mNavigationBarHeightForRotationDefault[rotation];
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        int height = fullHeight;
        if (displayId == 0 && this.mHasNavigationBar && (!this.mNavigationBarCanMove || fullWidth < fullHeight)) {
            height -= getNavigationBarHeight(rotation, uiMode);
        }
        if (displayCutout != null) {
            return height - (displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom());
        }
        return height;
    }

    public int getConfigDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        return getNonDecorDisplayWidth(fullWidth, fullHeight, rotation, uiMode, displayId, displayCutout);
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, int displayId, DisplayCutout displayCutout) {
        if (displayId != 0) {
            return fullHeight;
        }
        int statusBarHeight = this.mStatusBarHeightForRotation[rotation];
        if (displayCutout != null) {
            statusBarHeight = Math.max(0, statusBarHeight - displayCutout.getSafeInsetTop());
        }
        return getNonDecorDisplayHeight(fullWidth, fullHeight, rotation, uiMode, displayId, displayCutout) - statusBarHeight;
    }

    public boolean isKeyguardHostWindow(LayoutParams attrs) {
        return attrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME;
    }

    public boolean canBeHiddenByKeyguardLw(WindowState win) {
        int i = win.getAttrs().type;
        boolean z = false;
        if (i == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || i == 2013 || i == 2019 || i == 2023 || i == 2102) {
            return false;
        }
        if (getWindowLayerLw(win) < getWindowLayerFromTypeLw(IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME)) {
            z = true;
        }
        return z;
    }

    private boolean shouldBeHiddenByKeyguard(WindowState win, WindowState imeTarget) {
        boolean z = false;
        if (win.getAppToken() != null) {
            return false;
        }
        LayoutParams attrs = win.getAttrs();
        boolean showImeOverKeyguard = (imeTarget == null || !imeTarget.isVisibleLw() || ((imeTarget.getAttrs().flags & DumpState.DUMP_FROZEN) == 0 && canBeHiddenByKeyguardLw(imeTarget))) ? false : true;
        boolean allowWhenLocked = (win.isInputMethodWindow() || imeTarget == this) && showImeOverKeyguard;
        if (isKeyguardLocked() && isKeyguardOccluded()) {
            int i = ((DumpState.DUMP_FROZEN & attrs.flags) == 0 && (attrs.privateFlags & 256) == 0) ? 0 : 1;
            allowWhenLocked |= i;
        }
        boolean keyguardLocked = isKeyguardLocked();
        boolean hideDockDivider = attrs.type == 2034 && !this.mWindowManagerInternal.isStackVisible(3);
        boolean hideIme = win.isInputMethodWindow() && (this.mAodShowing || !this.mWindowManagerDrawComplete);
        if ((keyguardLocked && !allowWhenLocked && win.getDisplayId() == 0) || hideDockDivider || hideIme) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:54:0x00ee A:{Catch:{ BadTokenException -> 0x026c, RuntimeException -> 0x0262, all -> 0x0257 }} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00ff A:{Catch:{ BadTokenException -> 0x026c, RuntimeException -> 0x0262, all -> 0x0257 }} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00fa A:{Catch:{ BadTokenException -> 0x026c, RuntimeException -> 0x0262, all -> 0x0257 }} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x010d A:{SYNTHETIC, Splitter: B:60:0x010d} */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02e4  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02e4  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02e4  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02e4  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x02e4  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x02a8  */
    /* JADX WARNING: Missing block: B:69:0x011e, code:
            if (android.hwcontrol.HwWidgetFactory.isHwDarkTheme(r1) == false) goto L_0x0124;
     */
    /* JADX WARNING: Missing block: B:70:0x0120, code:
            r5 = 134217728 | r5;
     */
    /* JADX WARNING: Missing block: B:71:0x0124, code:
            r2.setFlags(((r5 | 16) | 8) | 131072, ((r5 | 16) | 8) | 131072);
            r2.setDefaultIcon(r13);
     */
    /* JADX WARNING: Missing block: B:74:?, code:
            r2.setDefaultLogo(r33);
            r2.setLayout(-1, -1);
            r0 = r2.getAttributes();
            r0.token = r8;
            r0.packageName = r9;
            r23 = r3;
            r0.windowAnimations = r2.getWindowStyle().getResourceId(8, r15);
            r0.privateFlags |= 1;
            r0.privateFlags |= 16;
     */
    /* JADX WARNING: Missing block: B:75:0x0167, code:
            if (r29.supportsScreen() != null) goto L_0x016f;
     */
    /* JADX WARNING: Missing block: B:76:0x0169, code:
            r0.privateFlags |= 128;
     */
    /* JADX WARNING: Missing block: B:77:0x016f, code:
            r3 = new java.lang.StringBuilder();
            r3.append("Splash Screen ");
            r3.append(r9);
            r0.setTitle(r3.toString());
            addSplashscreenContent(r2, r1);
            r3 = (android.view.WindowManager) r1.getSystemService("window");
     */
    /* JADX WARNING: Missing block: B:79:?, code:
            r4 = r2.getDecorView();
     */
    /* JADX WARNING: Missing block: B:81:?, code:
            r15 = new java.lang.StringBuilder();
            r24 = r1;
            r15.append("addStartingWindow ");
            r15.append(r9);
            r15.append(": nonLocalizedLabel=");
            r15.append(r11);
            r15.append(" theme=");
            r15.append(java.lang.Integer.toHexString(r28));
            r15.append(" windowFlags=");
            r15.append(java.lang.Integer.toHexString(r5));
            r15.append(" isFloating=");
            r15.append(r2.isFloating());
            r15.append(" appToken=");
            r15.append(r8);
            android.util.Flog.i(301, r15.toString());
            setHasAcitionBar(r2.hasFeature(8));
            r3.addView(r4, r0);
     */
    /* JADX WARNING: Missing block: B:82:0x01ef, code:
            if (r4.getParent() == null) goto L_0x01f9;
     */
    /* JADX WARNING: Missing block: B:84:0x01f6, code:
            r22 = new com.android.server.policy.SplashScreenSurface(r4, r8);
     */
    /* JADX WARNING: Missing block: B:85:0x01f9, code:
            r22 = null;
     */
    /* JADX WARNING: Missing block: B:86:0x01fc, code:
            if (r4 == null) goto L_0x020f;
     */
    /* JADX WARNING: Missing block: B:88:0x0202, code:
            if (r4.getParent() != null) goto L_0x020f;
     */
    /* JADX WARNING: Missing block: B:89:0x0204, code:
            android.util.Log.w(TAG, "view not successfully added to wm, removing view");
            r3.removeViewImmediate(r4);
     */
    /* JADX WARNING: Missing block: B:90:0x020f, code:
            return r22;
     */
    /* JADX WARNING: Missing block: B:91:0x0210, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:92:0x0213, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:93:0x0214, code:
            r1 = r5;
            r5 = r4;
     */
    /* JADX WARNING: Missing block: B:94:0x0218, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:95:0x0219, code:
            r1 = r5;
            r5 = r4;
     */
    /* JADX WARNING: Missing block: B:96:0x021d, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:97:0x021e, code:
            r4 = r18;
     */
    /* JADX WARNING: Missing block: B:98:0x0222, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:99:0x0223, code:
            r1 = r5;
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:100:0x0228, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:101:0x0229, code:
            r1 = r5;
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:102:0x022e, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:103:0x022f, code:
            r6 = r33;
     */
    /* JADX WARNING: Missing block: B:104:0x0232, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:105:0x0233, code:
            r6 = r33;
     */
    /* JADX WARNING: Missing block: B:106:0x0236, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:107:0x0237, code:
            r6 = r33;
     */
    /* JADX WARNING: Missing block: B:116:0x024d, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:117:0x024f, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:118:0x0250, code:
            r1 = r5;
     */
    /* JADX WARNING: Missing block: B:119:0x0252, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:120:0x0253, code:
            r1 = r5;
     */
    /* JADX WARNING: Missing block: B:140:0x02ac, code:
            if (r5.getParent() == null) goto L_0x02ae;
     */
    /* JADX WARNING: Missing block: B:141:0x02ae, code:
            android.util.Log.w(TAG, "view not successfully added to wm, removing view");
            r3.removeViewImmediate(r5);
     */
    /* JADX WARNING: Missing block: B:148:0x02e8, code:
            if (r5.getParent() == null) goto L_0x02ae;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public StartingSurface addSplashScreen(IBinder appToken, String packageName, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, Configuration overrideConfig, int displayId) {
        BadTokenException e;
        int windowFlags2;
        WindowManager wm;
        String str;
        StringBuilder stringBuilder;
        RuntimeException e2;
        Throwable th;
        View view;
        View view2;
        WindowManager wm2;
        int i;
        int i2;
        Context context;
        CharSequence charSequence;
        IBinder iBinder = appToken;
        String str2 = packageName;
        int i3 = theme;
        CharSequence charSequence2 = nonLocalizedLabel;
        int i4 = labelRes;
        final int i5 = icon;
        Configuration configuration = overrideConfig;
        if (str2 == null) {
            return null;
        }
        WindowManager wm3 = null;
        View view3 = null;
        try {
            Context displayContext = getDisplayContext(this.mContext, displayId);
            if (displayContext == null) {
                if (view3 != null && view3.getParent() == null) {
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm3.removeViewImmediate(view3);
                }
                return null;
            }
            boolean z;
            final PhoneWindow win;
            CharSequence wm4;
            Context context2 = displayContext;
            if (!(i3 == context2.getThemeResId() && i4 == 0)) {
                try {
                    context2 = context2.createPackageContext(str2, 4);
                    context2.setTheme(i3);
                } catch (NameNotFoundException e3) {
                } catch (BadTokenException e4) {
                    e = e4;
                    windowFlags2 = windowFlags;
                    wm = wm3;
                    wm3 = logo;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(iBinder);
                    stringBuilder.append(" already running, starting window not displayed. ");
                    stringBuilder.append(e.getMessage());
                    Log.w(str, stringBuilder.toString());
                    if (view3 != null) {
                    }
                    return null;
                } catch (RuntimeException e5) {
                    e2 = e5;
                    windowFlags2 = windowFlags;
                    wm = wm3;
                    wm3 = logo;
                    try {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(iBinder);
                        stringBuilder.append(" failed creating starting window");
                        Log.w(str, stringBuilder.toString(), e2);
                        if (view3 != null) {
                        }
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        view = view3;
                        Log.w(TAG, "view not successfully added to wm, removing view");
                        wm.removeViewImmediate(view);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    view = view3;
                    wm = wm3;
                    wm3 = logo;
                    view3 = windowFlags;
                    Log.w(TAG, "view not successfully added to wm, removing view");
                    wm.removeViewImmediate(view);
                    throw th;
                }
            }
            Context context3 = context2;
            Context contextStartWindow = null;
            if (configuration != null) {
                if (!configuration.equals(Configuration.EMPTY)) {
                    Context overrideContext;
                    TypedArray typedArray;
                    context2 = context3.createConfigurationContext(configuration);
                    context2.setTheme(i3);
                    TypedArray typedArray2 = context2.obtainStyledAttributes(R.styleable.Window);
                    if (isHwStartWindowEnabled()) {
                        overrideContext = context2;
                        TypedArray typedArray3 = typedArray2;
                        z = false;
                        view2 = view3;
                        wm2 = wm3;
                        try {
                            contextStartWindow = addHwStartWindow(str2, overrideContext, context3, typedArray3, windowFlags);
                            if (contextStartWindow != null) {
                                context3 = contextStartWindow;
                                typedArray = typedArray3;
                                typedArray.recycle();
                            } else {
                                typedArray = typedArray3;
                            }
                        } catch (BadTokenException e6) {
                            e = e6;
                            i = logo;
                            windowFlags2 = windowFlags;
                            view3 = view2;
                            wm = wm2;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(iBinder);
                            stringBuilder.append(" already running, starting window not displayed. ");
                            stringBuilder.append(e.getMessage());
                            Log.w(str, stringBuilder.toString());
                            if (view3 != null) {
                            }
                            return null;
                        } catch (RuntimeException e7) {
                            e2 = e7;
                            i = logo;
                            windowFlags2 = windowFlags;
                            view3 = view2;
                            wm = wm2;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(iBinder);
                            stringBuilder.append(" failed creating starting window");
                            Log.w(str, stringBuilder.toString(), e2);
                            if (view3 != null) {
                            }
                            return null;
                        } catch (Throwable th4) {
                            th = th4;
                            i = logo;
                            i2 = windowFlags;
                            view = view2;
                            wm = wm2;
                            Log.w(TAG, "view not successfully added to wm, removing view");
                            wm.removeViewImmediate(view);
                            throw th;
                        }
                    }
                    overrideContext = context2;
                    typedArray = typedArray2;
                    z = false;
                    view2 = view3;
                    wm2 = wm3;
                    if (contextStartWindow == null) {
                        int resId = typedArray.getResourceId(1, z);
                        if (resId != 0) {
                            Context overrideContext2 = overrideContext;
                            if (overrideContext2.getDrawable(resId) != null) {
                                context3 = overrideContext2;
                            }
                        }
                        typedArray.recycle();
                    }
                    context2 = context3;
                    win = (PhoneWindow) HwPolicyFactory.getHwPhoneWindow(context2);
                    win.setIsStartingWindow(true);
                    wm4 = context2.getResources().getText(i4, null);
                    if (i5 != 0) {
                        new Thread("iconPreloadingThread") {
                            public void run() {
                                try {
                                    win.getContext().getDrawable(i5);
                                } catch (NotFoundException e) {
                                    String str = PhoneWindowManager.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(i5);
                                    stringBuilder.append(" NotFoundException");
                                    Log.w(str, stringBuilder.toString());
                                }
                            }
                        }.start();
                    }
                    if (wm4 == null) {
                        win.setTitle(wm4, true);
                    } else {
                        win.setTitle(charSequence2, z);
                    }
                    win.setType(3);
                    synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
                        try {
                            if (this.mKeyguardOccluded) {
                                i2 = windowFlags | DumpState.DUMP_FROZEN;
                            } else {
                                i2 = windowFlags;
                            }
                            try {
                            } catch (Throwable th5) {
                                th = th5;
                                i = logo;
                                context = context2;
                                charSequence = wm4;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th6) {
                                        th = th6;
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            i = logo;
                            context = context2;
                            charSequence = wm4;
                            i2 = windowFlags;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                }
            }
            z = false;
            view2 = view3;
            wm2 = wm3;
            context2 = context3;
            win = (PhoneWindow) HwPolicyFactory.getHwPhoneWindow(context2);
            win.setIsStartingWindow(true);
            wm4 = context2.getResources().getText(i4, null);
            if (i5 != 0) {
            }
            if (wm4 == null) {
            }
            win.setType(3);
            synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            }
        } catch (BadTokenException e8) {
            e = e8;
            view2 = view3;
            wm2 = wm3;
            wm3 = logo;
            windowFlags2 = windowFlags;
            wm = wm2;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(iBinder);
            stringBuilder.append(" already running, starting window not displayed. ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
            if (view3 != null) {
            }
            return null;
        } catch (RuntimeException e9) {
            e2 = e9;
            view2 = view3;
            wm2 = wm3;
            wm3 = logo;
            windowFlags2 = windowFlags;
            wm = wm2;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(iBinder);
            stringBuilder.append(" failed creating starting window");
            Log.w(str, stringBuilder.toString(), e2);
            if (view3 != null) {
            }
            return null;
        } catch (Throwable th8) {
            th = th8;
            view2 = view3;
            wm2 = wm3;
            wm3 = logo;
            view3 = windowFlags;
            view = view2;
            wm = wm2;
            if (view != null && view.getParent() == null) {
                Log.w(TAG, "view not successfully added to wm, removing view");
                wm.removeViewImmediate(view);
            }
            throw th;
        }
    }

    private void addSplashscreenContent(PhoneWindow win, Context ctx) {
        TypedArray a = ctx.obtainStyledAttributes(R.styleable.Window);
        int resId = a.getResourceId(48, 0);
        a.recycle();
        if (resId != 0) {
            Drawable drawable = ctx.getDrawable(resId);
            if (drawable != null) {
                View v = new View(ctx);
                v.setBackground(drawable);
                win.setContentView(v);
            }
        }
    }

    private Context getDisplayContext(Context context, int displayId) {
        if (displayId == 0) {
            return context;
        }
        Display targetDisplay = ((DisplayManager) context.getSystemService("display")).getDisplay(displayId);
        if (targetDisplay == null) {
            return null;
        }
        return context.createDisplayContext(targetDisplay);
    }

    /* JADX WARNING: Missing block: B:14:0x002f, code:
            if (r0 != 2033) goto L_0x009d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int prepareAddWindowLw(WindowState win, LayoutParams attrs) {
        if ((attrs.privateFlags & DumpState.DUMP_CHANGES) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
            this.mScreenDecorWindows.add(win);
        }
        int i = attrs.type;
        if (i != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            if (!(i == 2014 || i == 2017)) {
                if (i == 2019) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
                    if (this.mNavigationBar != null && this.mNavigationBar.isAlive()) {
                        return -7;
                    }
                    this.mNavigationBar = win;
                    this.mNavigationBarController.setWindow(win);
                    this.mNavigationBarController.setOnBarVisibilityChangedListener(this.mNavBarVisibilityListener, true);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mNavigationBar:");
                    stringBuilder.append(this.mNavigationBar);
                    Flog.i(303, stringBuilder.toString());
                } else if (i != 2024) {
                }
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
        } else {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "PhoneWindowManager");
            if (this.mStatusBar != null && this.mStatusBar.isAlive()) {
                return -7;
            }
            this.mStatusBar = win;
            this.mStatusBarController.setWindow(win);
            setKeyguardOccludedLw(this.mKeyguardOccluded, true);
        }
        return 0;
    }

    public void removeWindowLw(WindowState win) {
        if (this.mStatusBar == win) {
            this.mStatusBar = null;
            this.mStatusBarController.setWindow(null);
        } else if (this.mNavigationBar == win) {
            this.mNavigationBar = null;
            this.mNavigationBarController.setWindow(null);
        }
        this.mScreenDecorWindows.remove(win);
    }

    public int selectAnimationLw(WindowState win, int transit) {
        if (win == this.mStatusBar) {
            boolean isKeyguard = (win.getAttrs().privateFlags & 1024) != 0;
            boolean expanded = win.getAttrs().height == -1 && win.getAttrs().width == -1;
            if (isKeyguard || expanded) {
                return -1;
            }
            if (transit == 2 || transit == 4) {
                return 17432624;
            }
            if (transit == 1 || transit == 3) {
                return 17432623;
            }
        } else if (win == this.mNavigationBar) {
            if (win.getAttrs().windowAnimations != 0) {
                return 0;
            }
            if (this.mNavigationBarPosition == 4) {
                if (transit == 2 || transit == 4) {
                    if (isKeyguardShowingAndNotOccluded()) {
                        return 17432618;
                    }
                    return 17432617;
                } else if (transit == 1 || transit == 3) {
                    return 17432616;
                }
            } else if (this.mNavigationBarPosition == 2) {
                if (transit == 2 || transit == 4) {
                    return 17432622;
                }
                if (transit == 1 || transit == 3) {
                    return 17432621;
                }
            } else if (this.mNavigationBarPosition == 1) {
                if (transit == 2 || transit == 4) {
                    return 17432620;
                }
                if (transit == 1 || transit == 3) {
                    return 17432619;
                }
            }
        } else if (win.getAttrs().type == 2034) {
            if ((win.getAttrs().flags & 536870912) != 0) {
                return selectDockedDividerAnimationLw(win, transit);
            }
            return 0;
        }
        if (transit == 5) {
            if (win.hasAppShownWindows()) {
                return 17432597;
            }
        } else if (win.getAttrs().type == 2023 && this.mDreamingLockscreen && transit == 1) {
            return -1;
        } else {
            if (this.mCust != null && this.mCust.isChargingAlbumSupported() && win.getAttrs().type == 2102 && this.mDreamingLockscreen) {
                return this.mCust.selectAnimationLw(transit);
            }
        }
        return 0;
    }

    private int selectDockedDividerAnimationLw(WindowState win, int transit) {
        int insets = this.mWindowManagerFuncs.getDockedDividerInsetsLw();
        Rect frame = win.getFrameLw();
        boolean behindNavBar = this.mNavigationBar != null && ((this.mNavigationBarPosition == 4 && frame.top + insets >= this.mNavigationBar.getFrameLw().top) || ((this.mNavigationBarPosition == 2 && frame.left + insets >= this.mNavigationBar.getFrameLw().left) || (this.mNavigationBarPosition == 1 && frame.right - insets <= this.mNavigationBar.getFrameLw().right)));
        boolean landscape = frame.height() > frame.width();
        boolean offscreenLandscape = landscape && (frame.right - insets <= 0 || frame.left + insets >= win.getDisplayFrameLw().right);
        boolean offscreenPortrait = !landscape && (frame.top - insets <= 0 || frame.bottom + insets >= win.getDisplayFrameLw().bottom);
        boolean offscreen = offscreenLandscape || offscreenPortrait;
        if (behindNavBar || offscreen) {
            return 0;
        }
        if (transit == 1 || transit == 3) {
            return 17432576;
        }
        if (transit == 2) {
            return 17432577;
        }
        return 0;
    }

    public void selectRotationAnimationLw(int[] anim) {
        boolean forceJumpcut = (this.mScreenOnFully && okToAnimate()) ? false : true;
        if (forceJumpcut) {
            anim[0] = 17432696;
            anim[1] = 17432695;
            return;
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            int animationHint = this.mTopFullscreenOpaqueWindowState.getRotationAnimationHint();
            if (animationHint < 0 && this.mTopIsFullscreen) {
                animationHint = this.mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation;
            }
            switch (animationHint) {
                case 1:
                case 3:
                    anim[0] = 17432697;
                    anim[1] = 17432695;
                    break;
                case 2:
                    anim[0] = 17432696;
                    anim[1] = 17432695;
                    break;
                default:
                    anim[1] = 0;
                    anim[0] = 0;
                    break;
            }
        }
        anim[1] = 0;
        anim[0] = 0;
    }

    public boolean validateRotationAnimationLw(int exitAnimId, int enterAnimId, boolean forceDefault) {
        boolean z = true;
        switch (exitAnimId) {
            case 17432696:
            case 17432697:
                if (forceDefault) {
                    return false;
                }
                int[] anim = new int[2];
                selectRotationAnimationLw(anim);
                if (!(exitAnimId == anim[0] && enterAnimId == anim[1])) {
                    z = false;
                }
                return z;
            default:
                return true;
        }
    }

    public Animation createHiddenByKeyguardExit(boolean onWallpaper, boolean goingToNotificationShade) {
        if (goingToNotificationShade) {
            return AnimationUtils.loadAnimation(this.mContext, 17432667);
        }
        int i;
        Context context = this.mContext;
        if (onWallpaper) {
            i = 17432668;
        } else {
            i = 17432666;
        }
        return (AnimationSet) AnimationUtils.loadAnimation(context, i);
    }

    public Animation createKeyguardWallpaperExit(boolean goingToNotificationShade) {
        if (goingToNotificationShade) {
            return null;
        }
        return AnimationUtils.loadAnimation(this.mContext, 17432672);
    }

    private static void awakenDreams() {
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager != null) {
            try {
                dreamManager.awaken();
            } catch (RemoteException e) {
            }
        }
    }

    static IDreamManager getDreamManager() {
        return IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
    }

    TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    static IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    boolean keyguardOn() {
        return isKeyguardShowingAndNotOccluded() || inKeyguardRestrictedKeyInputMode();
    }

    public long interceptKeyBeforeDispatching(WindowState win, KeyEvent event, int policyFlags) {
        String str;
        long now;
        long timeoutTime;
        String str2;
        StringBuilder stringBuilder;
        KeyEvent keyEvent = event;
        boolean keyguardOn = keyguardOn();
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int metaState = event.getMetaState();
        int flags = event.getFlags();
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        if (HWFLOW) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("interceptKeyTi keyCode=");
            stringBuilder2.append(keyCode);
            stringBuilder2.append(" down=");
            stringBuilder2.append(down);
            stringBuilder2.append(" repeatCount=");
            stringBuilder2.append(repeatCount);
            stringBuilder2.append(" keyguardOn=");
            stringBuilder2.append(keyguardOn);
            stringBuilder2.append(" mHomePressed=");
            stringBuilder2.append(this.mHomePressed);
            stringBuilder2.append(" canceled=");
            stringBuilder2.append(canceled);
            Log.d(str, stringBuilder2.toString());
        }
        if (this.mScreenshotChordEnabled && (flags & 1024) == 0) {
            if (this.mScreenshotChordVolumeDownKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
                now = SystemClock.uptimeMillis();
                timeoutTime = this.mScreenshotChordVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (now < timeoutTime) {
                    return timeoutTime - now;
                }
            }
            if (keyCode == 25 && this.mScreenshotChordVolumeDownKeyConsumed) {
                if (!down) {
                    this.mScreenshotChordVolumeDownKeyConsumed = false;
                }
                return -1;
            }
        }
        if (this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(false) && (flags & 1024) == 0) {
            if ((this.mScreenshotChordVolumeDownKeyTriggered ^ this.mA11yShortcutChordVolumeUpKeyTriggered) != 0) {
                now = SystemClock.uptimeMillis();
                timeoutTime = (this.mScreenshotChordVolumeDownKeyTriggered ? this.mScreenshotChordVolumeDownKeyTime : this.mA11yShortcutChordVolumeUpKeyTime) + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (now < timeoutTime) {
                    return timeoutTime - now;
                }
            }
            if (keyCode == 25 && this.mScreenshotChordVolumeDownKeyConsumed) {
                if (!down) {
                    this.mScreenshotChordVolumeDownKeyConsumed = false;
                }
                Log.d(TAG, "volume_down is consumed");
                return -1;
            } else if (keyCode == 24 && this.mA11yShortcutChordVolumeUpKeyConsumed) {
                if (!down) {
                    this.mA11yShortcutChordVolumeUpKeyConsumed = false;
                }
                Log.d(TAG, "volume_up is consumed");
                return -1;
            }
        }
        if (this.mRingerToggleChord != 0 && (flags & 1024) == 0) {
            if (this.mA11yShortcutChordVolumeUpKeyTriggered && !this.mScreenshotChordPowerKeyTriggered) {
                timeoutTime = SystemClock.uptimeMillis();
                long timeoutTime2 = this.mA11yShortcutChordVolumeUpKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (timeoutTime < timeoutTime2) {
                    return timeoutTime2 - timeoutTime;
                }
            }
            if (keyCode == 24 && this.mA11yShortcutChordVolumeUpKeyConsumed) {
                if (!down) {
                    this.mA11yShortcutChordVolumeUpKeyConsumed = false;
                }
                Log.d(TAG, "volume_up is consumed by A11");
                return -1;
            }
        }
        HwFrameworkFactory.getHwApsImpl().StopSdrForSpecial("interceptKeyBeforeDispatching", keyCode);
        if (this.mPendingMetaAction && !KeyEvent.isMetaKey(keyCode)) {
            this.mPendingMetaAction = false;
        }
        if (!(!this.mPendingCapsLockToggle || KeyEvent.isMetaKey(keyCode) || KeyEvent.isAltKey(keyCode))) {
            this.mPendingCapsLockToggle = false;
        }
        int i;
        int min;
        int max;
        if (keyCode != 3) {
            int i2;
            int direction;
            i = 2;
            if (keyCode == 82) {
                if (down && repeatCount == 0 && this.mEnableShiftMenuBugReports && (metaState & 1) == 1) {
                    this.mContext.sendOrderedBroadcastAsUser(new Intent("android.intent.action.BUG_REPORT"), UserHandle.CURRENT, null, null, null, 0, null, null);
                    return -1;
                }
            } else if (keyCode == 84) {
                if (!down) {
                    this.mSearchKeyShortcutPending = false;
                    if (this.mConsumeSearchKeyUp) {
                        this.mConsumeSearchKeyUp = false;
                        return -1;
                    }
                } else if (repeatCount == 0) {
                    this.mSearchKeyShortcutPending = true;
                    this.mConsumeSearchKeyUp = false;
                }
                return 0;
            } else if (keyCode == 187) {
                if (!keyguardOn) {
                    if (down && repeatCount == 0) {
                        preloadRecentApps();
                    } else if (!down) {
                        Jlog.d(NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION, "JLID_SYSTEMUI_START_RECENT");
                        toggleRecentApps();
                    }
                }
                return -1;
            } else if (keyCode == 42 && event.isMetaPressed()) {
                if (down) {
                    IStatusBarService service = getStatusBarService();
                    if (service != null) {
                        try {
                            service.expandNotificationsPanel();
                        } catch (RemoteException e) {
                        }
                    }
                }
            } else if (keyCode == 47 && event.isMetaPressed() && event.isCtrlPressed()) {
                if (down && repeatCount == 0) {
                    if (!event.isShiftPressed()) {
                        i = 1;
                    }
                    this.mScreenshotRunnable.setScreenshotType(i);
                    this.mHandler.post(this.mScreenshotRunnable);
                    return -1;
                }
            } else if (keyCode == 76 && event.isMetaPressed()) {
                if (down && repeatCount == 0 && !isKeyguardLocked()) {
                    toggleKeyboardShortcutsMenu(event.getDeviceId());
                }
            } else if (keyCode == 219) {
                Slog.wtf(TAG, "KEYCODE_ASSIST should be handled in interceptKeyBeforeQueueing");
                return -1;
            } else if (keyCode == 231) {
                Slog.wtf(TAG, "KEYCODE_VOICE_ASSIST should be handled in interceptKeyBeforeQueueing");
                return -1;
            } else if (keyCode == 120) {
                if (down && repeatCount == 0) {
                    this.mScreenshotRunnable.setScreenshotType(1);
                    this.mHandler.post(this.mScreenshotRunnable);
                }
                return -1;
            } else {
                if (keyCode == NetdResponseCode.TetheringStatsResult) {
                } else if (keyCode == 220) {
                    i2 = flags;
                } else if (keyCode == 24 || keyCode == 25 || keyCode == 164) {
                    if (this.mUseTvRouting) {
                    } else if (this.mHandleVolumeKeysInWM) {
                        i2 = flags;
                    } else if (this.mPersistentVrModeEnabled) {
                        InputDevice d = event.getDevice();
                        if (!(d == null || d.isExternal())) {
                            Log.d(TAG, "volume key is consumed by VrMode");
                            return -1;
                        }
                    }
                    dispatchDirectAudioEvent(keyEvent);
                    str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("volume key is consumed tvRouting:");
                    stringBuilder3.append(this.mUseTvRouting);
                    stringBuilder3.append(",KeysInWM:");
                    stringBuilder3.append(this.mHandleVolumeKeysInWM);
                    Log.d(str, stringBuilder3.toString());
                    return -1;
                } else if (keyCode == 61 && event.isMetaPressed()) {
                    return 0;
                } else {
                    if (this.mHasFeatureLeanback && interceptBugreportGestureTv(keyCode, down)) {
                        return -1;
                    }
                    if (this.mHasFeatureLeanback && interceptAccessibilityGestureTv(keyCode, down)) {
                        return -1;
                    }
                    if (keyCode == 284) {
                        if (!down) {
                            this.mHandler.removeMessages(25);
                            Message msg = this.mHandler.obtainMessage(25);
                            msg.setAsynchronous(true);
                            msg.sendToTarget();
                        }
                        return -1;
                    }
                }
                if (down) {
                    direction = keyCode == NetdResponseCode.TetheringStatsResult ? 1 : -1;
                    if (System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3) != 0) {
                        System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                    }
                    min = this.mPowerManager.getMinimumScreenBrightnessSetting();
                    max = this.mPowerManager.getMaximumScreenBrightnessSetting();
                    System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", Math.max(min, Math.min(max, System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mPowerManager.getDefaultScreenBrightnessSetting(), -3) + (((((max - min) + 10) - 1) / 10) * direction))), -3);
                    startActivityAsUser(new Intent("com.android.intent.action.SHOW_BRIGHTNESS_DIALOG"), UserHandle.CURRENT_OR_SELF);
                }
                return -1;
            }
            boolean actionTriggered = false;
            if (KeyEvent.isModifierKey(keyCode)) {
                if (!this.mPendingCapsLockToggle) {
                    this.mInitialMetaState = this.mMetaState;
                    this.mPendingCapsLockToggle = true;
                } else if (event.getAction() == 1) {
                    min = this.mMetaState & 50;
                    int metaOnMask = this.mMetaState & 458752;
                    if (!(metaOnMask == 0 || min == 0 || this.mInitialMetaState != (this.mMetaState ^ (min | metaOnMask)))) {
                        this.mInputManagerInternal.toggleCapsLock(event.getDeviceId());
                        actionTriggered = true;
                    }
                    this.mPendingCapsLockToggle = false;
                }
            }
            boolean actionTriggered2 = actionTriggered;
            this.mMetaState = metaState;
            if (actionTriggered2) {
                return -1;
            }
            long j;
            if (KeyEvent.isMetaKey(keyCode)) {
                if (down) {
                    this.mPendingMetaAction = true;
                } else if (this.mPendingMetaAction) {
                    if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
                        Slog.w(TAG, "Not start voice assistant because on OOBE.");
                        return -1;
                    }
                    j = -1;
                    launchAssistAction("android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD", event.getDeviceId());
                    return j;
                }
                j = -1;
                return j;
            }
            Intent shortcutIntent;
            if (this.mSearchKeyShortcutPending) {
                KeyCharacterMap kcm = event.getKeyCharacterMap();
                if (kcm.isPrintingKey(keyCode)) {
                    this.mConsumeSearchKeyUp = true;
                    this.mSearchKeyShortcutPending = false;
                    if (down && repeatCount == 0 && !keyguardOn) {
                        Intent shortcutIntent2 = this.mShortcutManager.getIntent(kcm, keyCode, metaState);
                        if (shortcutIntent2 != null) {
                            shortcutIntent2.addFlags(268435456);
                            try {
                                startActivityAsUser(shortcutIntent2, UserHandle.CURRENT);
                                dismissKeyboardShortcutsMenu();
                                i2 = flags;
                            } catch (ActivityNotFoundException ex) {
                                str2 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Dropping shortcut key combination because the activity to which it is registered was not found: SEARCH+");
                                stringBuilder4.append(KeyEvent.keyCodeToString(keyCode));
                                Slog.w(str2, stringBuilder4.toString(), ex);
                            }
                        } else {
                            str = TAG;
                            flags = new StringBuilder();
                            flags.append("Dropping unregistered shortcut key combination: SEARCH+");
                            flags.append(KeyEvent.keyCodeToString(keyCode));
                            Slog.i(str, flags.toString());
                        }
                    }
                    return -1;
                }
            }
            if (down && repeatCount == 0 && !keyguardOn && (65536 & metaState) != 0) {
                flags = event.getKeyCharacterMap();
                if (flags.isPrintingKey(keyCode)) {
                    shortcutIntent = this.mShortcutManager.getIntent(flags, keyCode, -458753 & metaState);
                    if (shortcutIntent != null) {
                        shortcutIntent.addFlags(268435456);
                        try {
                            startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
                            dismissKeyboardShortcutsMenu();
                        } catch (ActivityNotFoundException ex2) {
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Dropping shortcut key combination because the activity to which it is registered was not found: META+");
                            stringBuilder.append(KeyEvent.keyCodeToString(keyCode));
                            Slog.w(str2, stringBuilder.toString(), ex2);
                        }
                        return -1;
                    }
                }
            }
            if (down && repeatCount == 0 && !keyguardOn) {
                String flags2 = (String) sApplicationLaunchKeyCategories.get(keyCode);
                if (flags2 != null) {
                    shortcutIntent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", flags2);
                    shortcutIntent.setFlags(268435456);
                    if (ActivityManager.isUserAMonkey()) {
                        shortcutIntent.addFlags(DumpState.DUMP_COMPILER_STATS);
                    }
                    try {
                        startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
                        dismissKeyboardShortcutsMenu();
                    } catch (ActivityNotFoundException ex22) {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Dropping application launch key because the activity to which it is registered was not found: keyCode=");
                        stringBuilder.append(keyCode);
                        stringBuilder.append(", category=");
                        stringBuilder.append(flags2);
                        Slog.w(str2, stringBuilder.toString(), ex22);
                    }
                    return -1;
                }
            }
            if (down && repeatCount == 0 && keyCode == 61) {
                if (this.mRecentAppsHeldModifiers == 0 && !keyguardOn && isUserSetupComplete()) {
                    direction = event.getModifiers() & -194;
                    if (KeyEvent.metaStateHasModifiers(direction, 2) != 0) {
                        this.mRecentAppsHeldModifiers = direction;
                        showRecentApps(1);
                        return -1;
                    }
                }
            } else if (!(down || this.mRecentAppsHeldModifiers == 0 || (this.mRecentAppsHeldModifiers & metaState) != 0)) {
                this.mRecentAppsHeldModifiers = 0;
                hideRecentApps(true, false);
            }
            if (down && repeatCount == 0 && keyCode == 62 && (metaState & 28672) != 0) {
                this.mWindowManagerFuncs.switchKeyboardLayout(event.getDeviceId(), (metaState & HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS) != 0 ? -1 : 1);
                return -1;
            } else if (down && repeatCount == 0 && (keyCode == 204 || (keyCode == 62 && (458752 & metaState) != 0))) {
                this.mWindowManagerFuncs.switchInputMethod((metaState & HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS) == 0);
                return -1;
            } else if (this.mLanguageSwitchKeyPressed != 0 && !down && (keyCode == 204 || keyCode == 62)) {
                this.mLanguageSwitchKeyPressed = false;
                return -1;
            } else if (isValidGlobalKey(keyCode) && this.mGlobalKeyManager.handleGlobalKey(this.mContext, keyCode, keyEvent)) {
                return -1;
            } else {
                if (down) {
                    j = (long) keyCode;
                    if (event.isCtrlPressed()) {
                        j |= 17592186044416L;
                    }
                    if (event.isAltPressed()) {
                        j |= 8589934592L;
                    }
                    if (event.isShiftPressed()) {
                        j |= 4294967296L;
                    }
                    if (event.isMetaPressed()) {
                        j |= 281474976710656L;
                    }
                    IShortcutService flags3 = (IShortcutService) this.mShortcutKeyServices.get(j);
                    if (flags3 != null) {
                        try {
                            if (isUserSetupComplete()) {
                                flags3.notifyShortcutKeyPressed(j);
                            }
                        } catch (RemoteException e2) {
                            this.mShortcutKeyServices.delete(j);
                        }
                        return -1;
                    }
                }
                if ((65536 & metaState) != 0) {
                    return -1;
                }
                return 0;
            }
        } else if (down) {
            LayoutParams attrs = win != null ? win.getAttrs() : null;
            if (attrs != null) {
                min = attrs.type;
                if (min == 2009 || (attrs.privateFlags & 1024) != 0) {
                    return 0;
                }
                for (int i3 : WINDOW_TYPES_WHERE_HOME_DOESNT_WORK) {
                    if (min == i3) {
                        return -1;
                    }
                }
            }
            if (repeatCount == 0) {
                this.mHomePressed = true;
                if (this.mHomeDoubleTapPending) {
                    this.mHomeDoubleTapPending = false;
                    this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    handleDoubleTapOnHome();
                } else if (this.mDoubleTapOnHomeBehavior == 1) {
                    preloadRecentApps();
                }
            } else if (!((event.getFlags() & 128) == 0 || keyguardOn)) {
                handleLongPressOnHome(event.getDeviceId());
            }
            return -1;
        } else {
            cancelPreloadRecentApps();
            this.mHomePressed = false;
            if (this.mHomeConsumed) {
                this.mHomeConsumed = false;
                Log.w(TAG, "Ignoring HOME; event consumed");
                return -1;
            } else if (canceled) {
                Log.w(TAG, "Ignoring HOME; event canceled.");
                return -1;
            } else if (this.mDoubleTapOnHomeBehavior != 0) {
                this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                this.mHomeDoubleTapPending = true;
                this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, (long) ViewConfiguration.getDoubleTapTimeout());
                Log.w(TAG, "Ignoring HOME; doubleTap home");
                return -1;
            } else {
                Jlog.d(382, "JLID_HOME_KEY_PRESS");
                handleShortPressOnHome();
                try {
                    if (!(this.mWindowManager == null || -1 == this.mWindowManager.getDockedStackSide())) {
                        uploadKeyEvent(3);
                    }
                } catch (RemoteException e3) {
                    Slog.e(TAG, "Remote Exception failed getting dockside!");
                }
                return -1;
            }
        }
    }

    private boolean interceptBugreportGestureTv(int keyCode, boolean down) {
        if (keyCode == 23) {
            this.mBugreportTvKey1Pressed = down;
        } else if (keyCode == 4) {
            this.mBugreportTvKey2Pressed = down;
        }
        if (this.mBugreportTvKey1Pressed && this.mBugreportTvKey2Pressed) {
            if (!this.mBugreportTvScheduled) {
                this.mBugreportTvScheduled = true;
                Message msg = Message.obtain(this.mHandler, 21);
                msg.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg, 1000);
            }
        } else if (this.mBugreportTvScheduled) {
            this.mHandler.removeMessages(21);
            this.mBugreportTvScheduled = false;
        }
        return this.mBugreportTvScheduled;
    }

    private boolean interceptAccessibilityGestureTv(int keyCode, boolean down) {
        if (keyCode == 4) {
            this.mAccessibilityTvKey1Pressed = down;
        } else if (keyCode == 20) {
            this.mAccessibilityTvKey2Pressed = down;
        }
        if (this.mAccessibilityTvKey1Pressed && this.mAccessibilityTvKey2Pressed) {
            if (!this.mAccessibilityTvScheduled) {
                this.mAccessibilityTvScheduled = true;
                Message msg = Message.obtain(this.mHandler, 22);
                msg.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg, getAccessibilityShortcutTimeout());
            }
        } else if (this.mAccessibilityTvScheduled) {
            this.mHandler.removeMessages(22);
            this.mAccessibilityTvScheduled = false;
        }
        return this.mAccessibilityTvScheduled;
    }

    private void requestFullBugreport() {
        if ("1".equals(SystemProperties.get("ro.debuggable")) || Global.getInt(this.mContext.getContentResolver(), "development_settings_enabled", 0) == 1) {
            try {
                ActivityManager.getService().requestBugReport(0);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error taking bugreport", e);
            }
        }
    }

    public KeyEvent dispatchUnhandledKey(WindowState win, KeyEvent event, int policyFlags) {
        KeyEvent fallbackEvent = null;
        if ((event.getFlags() & 1024) == 0) {
            FallbackAction fallbackAction;
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            int keyCode = event.getKeyCode();
            int metaState = event.getMetaState();
            boolean initialDown = event.getAction() == 0 && event.getRepeatCount() == 0;
            if (initialDown) {
                fallbackAction = kcm.getFallbackAction(keyCode, metaState);
            } else {
                fallbackAction = (FallbackAction) this.mFallbackActions.get(keyCode);
            }
            if (fallbackAction != null) {
                fallbackEvent = KeyEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), fallbackAction.keyCode, event.getRepeatCount(), fallbackAction.metaState, event.getDeviceId(), event.getScanCode(), event.getFlags() | 1024, event.getSource(), null);
                if (!interceptFallback(win, fallbackEvent, policyFlags)) {
                    fallbackEvent.recycle();
                    fallbackEvent = null;
                }
                if (initialDown) {
                    this.mFallbackActions.put(keyCode, fallbackAction);
                } else if (event.getAction() == 1) {
                    this.mFallbackActions.remove(keyCode);
                    fallbackAction.recycle();
                }
                return fallbackEvent;
            }
        }
        WindowState windowState = win;
        int i = policyFlags;
        return fallbackEvent;
    }

    private boolean interceptFallback(WindowState win, KeyEvent fallbackEvent, int policyFlags) {
        if ((interceptKeyBeforeQueueing(fallbackEvent, policyFlags) & 1) == 0 || interceptKeyBeforeDispatching(win, fallbackEvent, policyFlags) != 0) {
            return false;
        }
        return true;
    }

    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutService) throws RemoteException {
        synchronized (this.mLock) {
            IShortcutService service = (IShortcutService) this.mShortcutKeyServices.get(shortcutCode);
            if (service == null || !service.asBinder().pingBinder()) {
                this.mShortcutKeyServices.put(shortcutCode, shortcutService);
            } else {
                throw new RemoteException("Key already exists.");
            }
        }
    }

    public void onKeyguardOccludedChangedLw(boolean occluded) {
        if (this.mKeyguardDelegate == null || !this.mKeyguardDelegate.isShowing()) {
            if (!occluded && this.mKeyguardOccludedChanged && this.mPendingKeyguardOccluded) {
                this.mKeyguardOccludedChanged = false;
                Slog.d(TAG, "Change mKeyguardOccludedChanged false");
            }
            setKeyguardOccludedLw(occluded, false);
            return;
        }
        this.mPendingKeyguardOccluded = occluded;
        this.mKeyguardOccludedChanged = true;
    }

    private int handleStartTransitionForKeyguardLw(int transit, long duration) {
        if (this.mKeyguardOccludedChanged) {
            this.mKeyguardOccludedChanged = false;
            if (setKeyguardOccludedLw(this.mPendingKeyguardOccluded, false)) {
                return 5;
            }
        }
        if (AppTransition.isKeyguardGoingAwayTransit(transit)) {
            startKeyguardExitAnimation(SystemClock.uptimeMillis(), duration);
        }
        return 0;
    }

    private void launchAssistLongPressAction() {
        performHapticFeedbackLw(null, 0, false);
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        Intent intent = new Intent("android.intent.action.SEARCH_LONG_PRESS");
        intent.setFlags(268435456);
        try {
            SearchManager searchManager = getSearchManager();
            if (searchManager != null) {
                searchManager.stopSearch();
            }
            startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (ActivityNotFoundException e) {
            Slog.w(TAG, "No activity to handle assist long press action.", e);
        }
    }

    protected void launchAssistAction() {
        launchAssistAction(null, Integer.MIN_VALUE);
    }

    private void launchAssistAction(String hint) {
        launchAssistAction(hint, Integer.MIN_VALUE);
    }

    protected void launchAssistAction(String hint, int deviceId) {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (isUserSetupComplete()) {
            Bundle args = null;
            if (deviceId > Integer.MIN_VALUE) {
                args = new Bundle();
                args.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", deviceId);
            }
            if ((this.mContext.getResources().getConfiguration().uiMode & 15) == 4) {
                ((SearchManager) this.mContext.getSystemService("search")).launchLegacyAssist(hint, UserHandle.myUserId(), args);
            } else {
                if (hint != null) {
                    if (args == null) {
                        args = new Bundle();
                    }
                    args.putBoolean(hint, true);
                }
                StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
                if (statusbar != null) {
                    statusbar.startAssist(args);
                }
            }
        }
    }

    private void startActivityAsUser(Intent intent, UserHandle handle) {
        if (isUserSetupComplete()) {
            this.mContext.startActivityAsUser(intent, handle);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ignoring HOME or Not starting activity because user setup is in progress: ");
        stringBuilder.append(intent);
        Slog.i(str, stringBuilder.toString());
    }

    private SearchManager getSearchManager() {
        if (this.mSearchManager == null) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        }
        return this.mSearchManager;
    }

    protected void preloadRecentApps() {
        this.mPreloadedRecentApps = true;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.preloadRecentApps();
        }
    }

    protected void cancelPreloadRecentApps() {
        if (this.mPreloadedRecentApps) {
            this.mPreloadedRecentApps = false;
            StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
            if (statusbar != null) {
                statusbar.cancelPreloadRecentApps();
            }
        }
    }

    protected void toggleRecentApps() {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.toggleRecentApps();
        }
    }

    public void showRecentApps() {
        this.mHandler.removeMessages(9);
        this.mHandler.obtainMessage(9).sendToTarget();
    }

    private void showRecentApps(boolean triggeredFromAltTab) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.showRecentApps(triggeredFromAltTab);
        }
    }

    private void toggleKeyboardShortcutsMenu(int deviceId) {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.toggleKeyboardShortcutsMenu(deviceId);
        }
    }

    private void dismissKeyboardShortcutsMenu() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.dismissKeyboardShortcutsMenu();
        }
    }

    private void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHome) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.hideRecentApps(triggeredFromAltTab, triggeredFromHome);
        }
    }

    void launchHomeFromHotKey() {
        launchHomeFromHotKey(true, true);
    }

    private void noticeHomePressed() {
        String action = "com.huawei.action.HOME_PRESSED";
        String extra_type = "extra_type";
        Intent intent = new Intent("com.huawei.action.HOME_PRESSED");
        intent.addFlags(1342177280);
        intent.putExtra("extra_type", "delay_as_locked");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    void launchHomeFromHotKey(final boolean awakenFromDreams, boolean respectKeyguard) {
        Handler handler = this.mHandler;
        WindowManagerFuncs windowManagerFuncs = this.mWindowManagerFuncs;
        Objects.requireNonNull(windowManagerFuncs);
        handler.post(new -$$Lambda$oXa0y3A-00RiQs6-KTPBgpkGtgw(windowManagerFuncs));
        if (respectKeyguard) {
            if (isKeyguardShowingAndNotOccluded()) {
                Log.w(TAG, "Ignoring HOME; keyguard is showing");
                return;
            } else if (this.mKeyguardOccluded && this.mKeyguardDelegate.isShowing()) {
                noticeHomePressed();
                this.mKeyguardDelegate.dismiss(new KeyguardDismissCallback() {
                    public void onDismissSucceeded() throws RemoteException {
                        PhoneWindowManager.this.mHandler.post(new -$$Lambda$PhoneWindowManager$14$BoOeOHeJpJnJexDq9S0FNcstRfE(this, awakenFromDreams));
                    }
                }, null);
                return;
            } else if (!this.mKeyguardOccluded && this.mKeyguardDelegate.isInputRestricted()) {
                Slog.w(TAG, "Ignoring HOME ? verify unlock before launching");
                this.mKeyguardDelegate.verifyUnlock(new OnKeyguardExitResult() {
                    public void onKeyguardExitResult(boolean success) {
                        String str = PhoneWindowManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring HOME ? onKeyguardExitResult is ? ");
                        stringBuilder.append(success);
                        Slog.w(str, stringBuilder.toString());
                        if (success) {
                            long origId = Binder.clearCallingIdentity();
                            try {
                                PhoneWindowManager.this.startDockOrHome(true, awakenFromDreams);
                            } finally {
                                Binder.restoreCallingIdentity(origId);
                            }
                        }
                    }
                });
                return;
            }
        }
        if (this.mRecentsVisible) {
            try {
                ActivityManager.getService().stopAppSwitches();
            } catch (RemoteException e) {
            }
            if (awakenFromDreams) {
                awakenDreams();
            }
            if (HWFLOW) {
                Log.i(TAG, "hideRecentApps from home");
            }
            hideRecentApps(false, true);
        } else {
            startDockOrHome(true, awakenFromDreams);
        }
    }

    public void setRecentsVisibilityLw(boolean visible) {
        this.mRecentsVisible = visible;
    }

    public void setPipVisibilityLw(boolean visible) {
        this.mPictureInPictureVisible = visible;
    }

    public void setNavBarVirtualKeyHapticFeedbackEnabledLw(boolean enabled) {
        this.mNavBarVirtualKeyHapticFeedbackEnabled = enabled;
    }

    public int adjustSystemUiVisibilityLw(int visibility) {
        this.mStatusBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, visibility);
        this.mNavigationBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, visibility);
        this.mResettingSystemUiFlags &= visibility;
        return ((~this.mResettingSystemUiFlags) & visibility) & (~this.mForceClearedSystemUiFlags);
    }

    public boolean getLayoutHintLw(LayoutParams attrs, Rect taskBounds, DisplayFrames displayFrames, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, ParcelableWrapper outDisplayCutout) {
        LayoutParams layoutParams = attrs;
        Rect rect = taskBounds;
        DisplayFrames displayFrames2 = displayFrames;
        Rect rect2 = outFrame;
        Rect rect3 = outContentInsets;
        Rect rect4 = outStableInsets;
        Rect rect5 = outOutsets;
        ParcelableWrapper parcelableWrapper = outDisplayCutout;
        int fl = PolicyControl.getWindowFlags(null, layoutParams);
        int pfl = layoutParams.privateFlags;
        int requestedSysUiVis = PolicyControl.getSystemUiVisibility(null, layoutParams);
        int sysUiVis = getImpliedSysUiFlagsForLayout(attrs) | requestedSysUiVis;
        int displayRotation = displayFrames2.mRotation;
        int displayWidth = displayFrames2.mDisplayWidth;
        int displayHeight = displayFrames2.mDisplayHeight;
        boolean useOutsets = rect5 != null && shouldUseOutsets(layoutParams, fl);
        if (useOutsets) {
            requestedSysUiVis = ScreenShapeHelper.getWindowOutsetBottomPx(this.mContext.getResources());
            if (requestedSysUiVis > 0) {
                if (displayRotation == 0) {
                    rect5.bottom += requestedSysUiVis;
                } else if (displayRotation == 1) {
                    rect5.right += requestedSysUiVis;
                } else if (displayRotation == 2) {
                    rect5.top += requestedSysUiVis;
                } else if (displayRotation == 3) {
                    rect5.left += requestedSysUiVis;
                }
            }
        }
        boolean layoutInScreen = (fl & 256) != 0;
        boolean layoutInScreenAndInsetDecor = layoutInScreen && (65536 & fl) != 0;
        boolean screenDecor = (pfl & DumpState.DUMP_CHANGES) != 0;
        if (!layoutInScreenAndInsetDecor || screenDecor) {
            boolean z = layoutInScreenAndInsetDecor;
            int i = pfl;
            int i2 = displayRotation;
            if (layoutInScreen) {
                rect2.set(displayFrames2.mUnrestricted);
            } else {
                rect2.set(displayFrames2.mStable);
            }
            if (rect != null) {
                rect2.intersect(rect);
            }
            outContentInsets.setEmpty();
            outStableInsets.setEmpty();
            parcelableWrapper.set(DisplayCutout.NO_CUTOUT);
            return this.mForceShowSystemBars;
        }
        int availBottom;
        int availRight;
        if (!canHideNavigationBar() || (sysUiVis & 512) == 0) {
            rect2.set(displayFrames2.mRestricted);
            int availRight2 = displayFrames2.mRestricted.right;
            availBottom = displayFrames2.mRestricted.bottom;
            availRight = availRight2;
        } else {
            rect2.set(displayFrames2.mUnrestricted);
            availRight = displayFrames2.mUnrestricted.right;
            availBottom = displayFrames2.mUnrestricted.bottom;
        }
        rect4.set(displayFrames2.mStable.left, displayFrames2.mStable.top, availRight - displayFrames2.mStable.right, availBottom - displayFrames2.mStable.bottom);
        if ((sysUiVis & 256) != 0) {
            if ((fl & 1024) != 0) {
                rect3.set(displayFrames2.mStableFullscreen.left, displayFrames2.mStableFullscreen.top, availRight - displayFrames2.mStableFullscreen.right, availBottom - displayFrames2.mStableFullscreen.bottom);
            } else {
                outContentInsets.set(outStableInsets);
            }
        } else if ((fl & 1024) == 0 && (DumpState.DUMP_HANDLE & fl) == 0) {
            rect3.set(displayFrames2.mCurrent.left, displayFrames2.mCurrent.top, availRight - displayFrames2.mCurrent.right, availBottom - displayFrames2.mCurrent.bottom);
        } else {
            outContentInsets.setEmpty();
        }
        if (rect != null) {
            calculateRelevantTaskInsets(rect, rect3, displayWidth, displayHeight);
            calculateRelevantTaskInsets(rect, rect4, displayWidth, displayHeight);
            rect2.intersect(rect);
        }
        parcelableWrapper.set(displayFrames2.mDisplayCutout.calculateRelativeTo(rect2).getDisplayCutout());
        return this.mForceShowSystemBars;
    }

    private void calculateRelevantTaskInsets(Rect taskBounds, Rect inOutInsets, int displayWidth, int displayHeight) {
        mTmpRect.set(0, 0, displayWidth, displayHeight);
        mTmpRect.inset(inOutInsets);
        mTmpRect.intersect(taskBounds);
        inOutInsets.set(mTmpRect.left - taskBounds.left, mTmpRect.top - taskBounds.top, taskBounds.right - mTmpRect.right, taskBounds.bottom - mTmpRect.bottom);
    }

    private boolean shouldUseOutsets(LayoutParams attrs, int fl) {
        return attrs.type == 2013 || (33555456 & fl) != 0;
    }

    public void beginLayoutLw(DisplayFrames displayFrames, int uiMode) {
        Rect dcf;
        Rect df;
        Rect pf;
        DisplayFrames displayFrames2;
        DisplayFrames displayFrames3 = displayFrames;
        displayFrames.onBeginLayout();
        this.mDisplayRotation = displayFrames3.mRotation;
        this.mSystemGestures.screenWidth = displayFrames3.mUnrestricted.width();
        SystemGesturesPointerEventListener systemGesturesPointerEventListener = this.mSystemGestures;
        int height = displayFrames3.mUnrestricted.height();
        systemGesturesPointerEventListener.screenHeight = height;
        this.mRestrictedScreenHeight = height;
        this.mDockLayer = 268435456;
        this.mStatusBarLayer = -1;
        Rect pf2 = mTmpParentFrame;
        Rect df2 = mTmpDisplayFrame;
        Rect of = mTmpOverscanFrame;
        Rect vf = mTmpVisibleFrame;
        Rect dcf2 = mTmpDecorFrame;
        vf.set(displayFrames3.mDock);
        of.set(displayFrames3.mDock);
        df2.set(displayFrames3.mDock);
        pf2.set(displayFrames3.mDock);
        dcf2.setEmpty();
        if (IS_NOTCH_PROP) {
            this.mNotchPropSize = this.mContext.getResources().getDimensionPixelSize(17105318);
        }
        if (displayFrames3.mDisplayId == 0) {
            boolean navVisible;
            int sysui = this.mLastSystemUiFlags;
            boolean navVisible2 = (sysui & 2) == 0;
            boolean navTranslucent = (-2147450880 & sysui) != 0;
            boolean immersive = (sysui & 2048) != 0;
            boolean immersiveSticky = (sysui & 4096) != 0;
            LayoutParams focusAttrs = this.mFocusedWindow != null ? this.mFocusedWindow.getAttrs() : null;
            boolean z = (focusAttrs == null || (focusAttrs.privateFlags & Integer.MIN_VALUE) == 0) ? false : true;
            z = immersive || immersiveSticky || z;
            boolean navAllowedHidden = z;
            navTranslucent &= !immersiveSticky ? 1 : 0;
            z = isStatusBarKeyguard() && !this.mKeyguardOccluded;
            boolean isKeyguardShowing = z;
            if (!isKeyguardShowing) {
                navTranslucent &= areTranslucentBarsAllowed();
            }
            boolean navTranslucent2 = navTranslucent;
            boolean statusBarExpandedNotKeyguard = (isKeyguardShowing || this.mStatusBar == null || HwPolicyFactory.isHwGlobalActionsShowing() || this.mStatusBar.getAttrs().height != -1 || this.mStatusBar.getAttrs().width != -1) ? false : true;
            if (navVisible2 || navAllowedHidden) {
                if (this.mInputConsumer != null) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(19, this.mInputConsumer));
                }
            } else if (this.mInputConsumer == null && this.mStatusBar != null && canHideNavigationBar()) {
                this.mInputConsumer = this.mWindowManagerFuncs.createInputConsumer(this.mHandler.getLooper(), "nav_input_consumer", new -$$Lambda$PhoneWindowManager$t7VYWSe1_o50lulMFjRyMrKODrg(this));
                InputManager.getInstance().setPointerIconType(0);
            }
            boolean navVisible3 = (canHideNavigationBar() ^ true) | navVisible2;
            if (navVisible3 && mUsingHwNavibar) {
                boolean z2 = navVisible3 && !computeNaviBarFlag();
                navVisible3 = z2;
            }
            String activityName = "com.google.android.gms/com.google.android.gms.auth.login.ShowErrorActivity";
            String windowName = null;
            if (this.mFocusedWindow != null) {
                windowName = this.mFocusedWindow.toString();
            }
            String windowName2 = windowName;
            height = focusAttrs != null ? focusAttrs.type : 0;
            int flag = focusAttrs != null ? focusAttrs.privateFlags : 0;
            boolean z3 = (height == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && (flag & 1024) != 0) || height == 2101 || height == 2100;
            z = z3;
            int type;
            if (windowName2 == null || !windowName2.contains(activityName)) {
                navVisible = navVisible3;
                type = height;
            } else {
                navVisible = navVisible3;
                type = height;
                if (Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
                    navVisible = true;
                }
            }
            int sysui2 = sysui;
            dcf = dcf2;
            df = df2;
            pf = pf2;
            displayFrames2 = displayFrames3;
            if (layoutNavigationBar(displayFrames3, uiMode, dcf2, navVisible, navTranslucent2, navAllowedHidden, z, statusBarExpandedNotKeyguard) | layoutStatusBar(displayFrames3, pf2, df2, of, vf, dcf, sysui2, isKeyguardShowing)) {
                updateSystemUiVisibilityLw();
            }
        } else {
            dcf = dcf2;
            Rect rect = vf;
            Rect rect2 = of;
            df = df2;
            pf = pf2;
            displayFrames2 = displayFrames3;
        }
        layoutScreenDecorWindows(displayFrames2, pf, df, dcf);
        if (displayFrames2.mDisplayCutoutSafe.top > displayFrames2.mUnrestricted.top) {
            displayFrames2.mDisplayCutoutSafe.top = Math.max(displayFrames2.mDisplayCutoutSafe.top, displayFrames2.mStable.top);
        }
    }

    private void layoutScreenDecorWindows(DisplayFrames displayFrames, Rect pf, Rect df, Rect dcf) {
        DisplayFrames displayFrames2 = displayFrames;
        if (!this.mScreenDecorWindows.isEmpty()) {
            int displayId = displayFrames2.mDisplayId;
            Rect dockFrame = displayFrames2.mDock;
            int displayHeight = displayFrames2.mDisplayHeight;
            int displayWidth = displayFrames2.mDisplayWidth;
            for (int i = this.mScreenDecorWindows.size() - 1; i >= 0; i--) {
                WindowState w = (WindowState) this.mScreenDecorWindows.valueAt(i);
                if (w.getDisplayId() == displayId && w.isVisibleLw()) {
                    w.computeFrameLw(pf, df, df, df, df, dcf, df, df, displayFrames2.mDisplayCutout, false);
                    Rect frame = w.getFrameLw();
                    String str;
                    StringBuilder stringBuilder;
                    if (frame.left > 0 || frame.top > 0) {
                        if (frame.right < displayWidth || frame.bottom < displayHeight) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("layoutScreenDecorWindows: Ignoring decor win=");
                            stringBuilder.append(w);
                            stringBuilder.append(" not docked on one of the sides of the display. frame=");
                            stringBuilder.append(frame);
                            stringBuilder.append(" displayWidth=");
                            stringBuilder.append(displayWidth);
                            stringBuilder.append(" displayHeight=");
                            stringBuilder.append(displayHeight);
                            Slog.w(str, stringBuilder.toString());
                        } else if (frame.top <= 0) {
                            dockFrame.right = Math.min(frame.left, dockFrame.right);
                        } else if (frame.left <= 0) {
                            dockFrame.bottom = Math.min(frame.top, dockFrame.bottom);
                        } else {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("layoutScreenDecorWindows: Ignoring decor win=");
                            stringBuilder.append(w);
                            stringBuilder.append(" not docked on right or bottom of display. frame=");
                            stringBuilder.append(frame);
                            stringBuilder.append(" displayWidth=");
                            stringBuilder.append(displayWidth);
                            stringBuilder.append(" displayHeight=");
                            stringBuilder.append(displayHeight);
                            Slog.w(str, stringBuilder.toString());
                        }
                    } else if (frame.bottom >= displayHeight) {
                        dockFrame.left = Math.max(frame.right, dockFrame.left);
                    } else if (frame.right >= displayWidth) {
                        dockFrame.top = Math.max(frame.bottom, dockFrame.top);
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("layoutScreenDecorWindows: Ignoring decor win=");
                        stringBuilder.append(w);
                        stringBuilder.append(" not docked on left or top of display. frame=");
                        stringBuilder.append(frame);
                        stringBuilder.append(" displayWidth=");
                        stringBuilder.append(displayWidth);
                        stringBuilder.append(" displayHeight=");
                        stringBuilder.append(displayHeight);
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            }
            displayFrames2.mRestricted.set(dockFrame);
            displayFrames2.mCurrent.set(dockFrame);
            displayFrames2.mVoiceContent.set(dockFrame);
            displayFrames2.mSystem.set(dockFrame);
            displayFrames2.mContent.set(dockFrame);
            displayFrames2.mRestrictedOverscan.set(dockFrame);
        }
    }

    private boolean layoutStatusBar(DisplayFrames displayFrames, Rect pf, Rect df, Rect of, Rect vf, Rect dcf, int sysui, boolean isKeyguardShowing) {
        DisplayFrames displayFrames2 = displayFrames;
        if (this.mStatusBar == null) {
            return false;
        }
        of.set(displayFrames2.mUnrestricted);
        Rect rect = df;
        rect.set(displayFrames2.mUnrestricted);
        Rect rect2 = pf;
        rect2.set(displayFrames2.mUnrestricted);
        Rect rect3 = vf;
        rect3.set(displayFrames2.mStable);
        this.mStatusBarLayer = this.mStatusBar.getSurfaceLayer();
        this.mStatusBar.computeFrameLw(rect2, rect, rect3, rect3, rect3, dcf, rect3, vf, displayFrames2.mDisplayCutout, false);
        displayFrames2.mStable.top = displayFrames2.mUnrestricted.top + this.mStatusBarHeightForRotation[displayFrames2.mRotation];
        displayFrames2.mStable.top = Math.max(displayFrames2.mStable.top, displayFrames2.mDisplayCutoutSafe.top);
        mTmpRect.set(this.mStatusBar.getContentFrameLw());
        mTmpRect.intersect(displayFrames2.mDisplayCutoutSafe);
        mTmpRect.top = this.mStatusBar.getContentFrameLw().top;
        mTmpRect.bottom = displayFrames2.mStable.top;
        this.mStatusBarController.setContentFrame(mTmpRect);
        boolean z = true;
        boolean statusBarTransient = (sysui & 67108864) != 0;
        if ((sysui & 1073741832) == 0) {
            z = false;
        }
        boolean statusBarTranslucent = z;
        if (!isKeyguardShowing) {
            statusBarTranslucent &= areTranslucentBarsAllowed();
        }
        if (this.mStatusBar.isVisibleLw() && !statusBarTransient) {
            Rect dockFrame = displayFrames2.mDock;
            dockFrame.top = displayFrames2.mStable.top;
            displayFrames2.mContent.set(dockFrame);
            displayFrames2.mVoiceContent.set(dockFrame);
            displayFrames2.mCurrent.set(dockFrame);
            if (!(this.mStatusBar.isAnimatingLw() || statusBarTranslucent || this.mStatusBarController.wasRecentlyTranslucent())) {
                displayFrames2.mSystem.top = displayFrames2.mStable.top;
            }
        }
        return this.mStatusBarController.checkHiddenLw();
    }

    private boolean layoutNavigationBar(DisplayFrames displayFrames, int uiMode, Rect dcf, boolean navVisible, boolean navTranslucent, boolean navAllowedHidden, boolean isKeyguardOn, boolean statusBarExpandedNotKeyguard) {
        DisplayFrames displayFrames2 = displayFrames;
        int i = uiMode;
        boolean z = statusBarExpandedNotKeyguard;
        if (this.mNavigationBar == null) {
            return false;
        }
        boolean transientNavBarShowing = this.mNavigationBarController.isTransientShowing();
        int rotation = displayFrames2.mRotation;
        int displayHeight = displayFrames2.mDisplayHeight;
        int displayWidth = displayFrames2.mDisplayWidth;
        Rect dockFrame = displayFrames2.mDock;
        this.mNavigationBarPosition = navigationBarPosition(displayWidth, displayHeight, rotation);
        Rect cutoutSafeUnrestricted = mTmpRect;
        cutoutSafeUnrestricted.set(displayFrames2.mUnrestricted);
        cutoutSafeUnrestricted.intersectUnchecked(displayFrames2.mDisplayCutoutSafe);
        int top;
        Rect rect;
        int naviBarHeightForRotationMin;
        Rect cutoutSafeUnrestricted2;
        int i2;
        Rect rect2;
        if (this.mNavigationBarPosition == 4) {
            top = cutoutSafeUnrestricted.bottom - getNavigationBarHeight(rotation, i);
            mTmpNavigationFrame.set(0, top, displayWidth, (displayFrames2.mUnrestricted.bottom + getNaviBarHeightForRotationMax(rotation)) - this.mNavigationBarHeightForRotationDefault[rotation]);
            if (isNaviBarMini()) {
                rect = displayFrames2.mStable;
                naviBarHeightForRotationMin = displayHeight - getNaviBarHeightForRotationMin(rotation);
                displayFrames2.mStableFullscreen.bottom = naviBarHeightForRotationMin;
                rect.bottom = naviBarHeightForRotationMin;
            } else {
                rect = displayFrames2.mStable;
                displayFrames2.mStableFullscreen.bottom = top;
                rect.bottom = top;
            }
            if (transientNavBarShowing) {
                this.mLastNaviStatus = 2;
                this.mLastTransientNaviDockBottom = dockFrame.bottom;
                this.mNavigationBarController.setBarShowingLw(true);
            } else if (navVisible) {
                if (this.mNavigationBarController.isTransientHiding()) {
                    Slog.v(TAG, "navigationbar is visible, but transientBarState is hiding, so reset a portrait screen");
                    this.mNavigationBarController.sethwTransientBarState(0);
                }
                this.mNavigationBarController.setBarShowingLw(true);
                rect = displayFrames2.mRestricted;
                Rect rect3 = displayFrames2.mRestrictedOverscan;
                naviBarHeightForRotationMin = displayFrames2.mStable.bottom;
                rect3.bottom = naviBarHeightForRotationMin;
                rect.bottom = naviBarHeightForRotationMin;
                dockFrame.bottom = naviBarHeightForRotationMin;
                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames2.mRestricted.top;
                this.mLastNaviStatus = 0;
                this.mLastShowNaviDockBottom = dockFrame.bottom;
            } else {
                this.mNavigationBarController.setBarShowingLw(z);
                if (isKeyguardOn) {
                    switch (this.mLastNaviStatus) {
                        case 0:
                            if (this.mLastShowNaviDockBottom != 0) {
                                dockFrame.bottom = this.mLastShowNaviDockBottom;
                                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames2.mRestrictedOverscan.top;
                                displayFrames2.mRestricted.bottom = dockFrame.bottom;
                                displayFrames2.mRestrictedOverscan.bottom = dockFrame.bottom;
                                break;
                            }
                            break;
                        case 1:
                            if (this.mLastHideNaviDockBottom != 0) {
                                dockFrame.bottom = this.mLastHideNaviDockBottom;
                                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames2.mRestrictedOverscan.top;
                                displayFrames2.mRestricted.bottom = dockFrame.bottom;
                                displayFrames2.mRestrictedOverscan.bottom = dockFrame.bottom;
                                break;
                            }
                            break;
                        case 2:
                            if (this.mLastTransientNaviDockBottom != 0) {
                                dockFrame.bottom = this.mLastTransientNaviDockBottom;
                                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames2.mRestrictedOverscan.top;
                                displayFrames2.mRestricted.bottom = dockFrame.bottom;
                                displayFrames2.mRestrictedOverscan.bottom = dockFrame.bottom;
                                break;
                            }
                            break;
                        default:
                            Slog.v(TAG, "keyguard mLastNaviStatus is init");
                            break;
                    }
                }
                this.mLastNaviStatus = 1;
                this.mLastHideNaviDockBottom = dockFrame.bottom;
            }
            if (!(!navVisible || navTranslucent || navAllowedHidden || this.mNavigationBar.isAnimatingLw() || this.mNavigationBarController.wasRecentlyTranslucent() || mUsingHwNavibar)) {
                displayFrames2.mSystem.bottom = displayFrames2.mStable.bottom;
            }
        } else if (this.mNavigationBarPosition == 2) {
            boolean isShowLeftNavBar = getNavibarAlignLeftWhenLand();
            top = cutoutSafeUnrestricted.right - getNavigationBarWidth(rotation, uiMode);
            if (isShowLeftNavBar) {
                cutoutSafeUnrestricted2 = cutoutSafeUnrestricted;
                mTmpNavigationFrame.set(0, 0, this.mContext.getResources().getDimensionPixelSize(34472115), displayHeight);
            } else {
                cutoutSafeUnrestricted2 = cutoutSafeUnrestricted;
                mTmpNavigationFrame.set(top, 0, (displayFrames2.mUnrestricted.right + getNaviBarWidthForRotationMax(rotation)) - this.mNavigationBarWidthForRotationDefault[rotation], displayHeight);
            }
            if (isNaviBarMini()) {
                rect = displayFrames2.mStable;
                naviBarHeightForRotationMin = displayWidth - getNaviBarWidthForRotationMin(rotation);
                displayFrames2.mStableFullscreen.right = naviBarHeightForRotationMin;
                rect.right = naviBarHeightForRotationMin;
            } else if (isShowLeftNavBar) {
                rect = displayFrames2.mStable;
                cutoutSafeUnrestricted = displayFrames2.mStableFullscreen;
                naviBarHeightForRotationMin = mTmpNavigationFrame.right;
                cutoutSafeUnrestricted.left = naviBarHeightForRotationMin;
                rect.left = naviBarHeightForRotationMin;
                displayFrames2.mStable.right = displayWidth;
            } else {
                rect = displayFrames2.mStable;
                displayFrames2.mStableFullscreen.right = top;
                rect.right = top;
            }
            if (!(navVisible || this.mLastSystemUiFlags == this.mLastSystemUiFlagsTmp)) {
                this.mLastSystemUiFlagsTmp = this.mLastSystemUiFlags;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("transientNavBarShowing:");
                stringBuilder.append(transientNavBarShowing);
                stringBuilder.append(",statusBarExpandedNotKeyguard:");
                stringBuilder.append(z);
                stringBuilder.append(",mLastSystemUiFlags:");
                stringBuilder.append(Integer.toHexString(this.mLastSystemUiFlags));
                Flog.i(303, stringBuilder.toString());
            }
            if (transientNavBarShowing) {
                this.mNavigationBarController.setBarShowingLw(true);
                this.mLastNaviStatus = 2;
            } else if (navVisible) {
                if (this.mNavigationBarController.isTransientHiding()) {
                    Slog.v(TAG, "navigationbar is visible, but transientBarState is hiding, so reset a landscape screen");
                    this.mNavigationBarController.sethwTransientBarState(0);
                }
                this.mNavigationBarController.setBarShowingLw(true);
                dockFrame.right = displayFrames2.mStable.right;
                if (isShowLeftNavBar) {
                    rect = displayFrames2.mRestricted;
                    i2 = displayFrames2.mStable.left;
                    dockFrame.left = i2;
                    rect.left = i2;
                    displayFrames2.mRestricted.right = displayFrames2.mRestricted.left + displayFrames2.mStable.right;
                    displayFrames2.mRestricted.right = displayFrames2.mRestricted.left + displayFrames2.mStable.right;
                } else {
                    displayFrames2.mRestricted.right = (displayFrames2.mRestricted.left + dockFrame.right) - displayFrames2.mRestricted.left;
                    displayFrames2.mRestrictedOverscan.right = dockFrame.right - displayFrames2.mRestrictedOverscan.left;
                }
                this.mLastNaviStatus = 0;
            } else {
                this.mNavigationBarController.setBarShowingLw(z);
                this.mLastNaviStatus = 1;
            }
            if (!(!navVisible || navTranslucent || navAllowedHidden || this.mNavigationBar.isAnimatingLw() || this.mNavigationBarController.wasRecentlyTranslucent() || mUsingHwNavibar)) {
                displayFrames2.mSystem.right = displayFrames2.mStable.right;
            }
            rect2 = cutoutSafeUnrestricted2;
        } else {
            cutoutSafeUnrestricted2 = cutoutSafeUnrestricted;
            if (this.mNavigationBarPosition == 1) {
                rect = cutoutSafeUnrestricted2;
                int right = rect.left + getNavigationBarWidth(rotation, uiMode);
                rect2 = rect;
                mTmpNavigationFrame.set(displayFrames2.mUnrestricted.left, null, right, displayHeight);
                rect = displayFrames2.mStable;
                displayFrames2.mStableFullscreen.left = right;
                rect.left = right;
                if (transientNavBarShowing) {
                    this.mNavigationBarController.setBarShowingLw(true);
                } else if (navVisible) {
                    this.mNavigationBarController.setBarShowingLw(true);
                    rect = displayFrames2.mRestricted;
                    displayFrames2.mRestrictedOverscan.left = right;
                    rect.left = right;
                    dockFrame.left = right;
                } else {
                    this.mNavigationBarController.setBarShowingLw(z);
                }
                if (!(!navVisible || navTranslucent || navAllowedHidden || this.mNavigationBar.isAnimatingLw() || this.mNavigationBarController.wasRecentlyTranslucent())) {
                    displayFrames2.mSystem.left = right;
                }
            } else {
                i2 = uiMode;
            }
            displayFrames2.mCurrent.set(dockFrame);
            displayFrames2.mVoiceContent.set(dockFrame);
            displayFrames2.mContent.set(dockFrame);
            this.mStatusBarLayer = this.mNavigationBar.getSurfaceLayer();
            this.mNavigationBar.computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, displayFrames2.mDisplayCutoutSafe, mTmpNavigationFrame, dcf, mTmpNavigationFrame, displayFrames2.mDisplayCutoutSafe, displayFrames2.mDisplayCutout, false);
            this.mNavigationBarController.setContentFrame(this.mNavigationBar.getContentFrameLw());
            return this.mNavigationBarController.checkHiddenLw();
        }
        cutoutSafeUnrestricted = uiMode;
        displayFrames2.mCurrent.set(dockFrame);
        displayFrames2.mVoiceContent.set(dockFrame);
        displayFrames2.mContent.set(dockFrame);
        this.mStatusBarLayer = this.mNavigationBar.getSurfaceLayer();
        this.mNavigationBar.computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, displayFrames2.mDisplayCutoutSafe, mTmpNavigationFrame, dcf, mTmpNavigationFrame, displayFrames2.mDisplayCutoutSafe, displayFrames2.mDisplayCutout, false);
        this.mNavigationBarController.setContentFrame(this.mNavigationBar.getContentFrameLw());
        return this.mNavigationBarController.checkHiddenLw();
    }

    private int navigationBarPosition(int displayWidth, int displayHeight, int displayRotation) {
        if (!this.mNavigationBarCanMove || displayWidth <= displayHeight) {
            return 4;
        }
        return displayRotation == 3 ? 2 : 2;
    }

    public int getSystemDecorLayerLw() {
        if (this.mStatusBar != null && this.mStatusBar.isVisibleLw()) {
            return this.mStatusBar.getSurfaceLayer();
        }
        if (this.mNavigationBar == null || !this.mNavigationBar.isVisibleLw()) {
            return 0;
        }
        return this.mNavigationBar.getSurfaceLayer();
    }

    private void setAttachedWindowFrames(WindowState win, int fl, int adjust, WindowState attached, boolean insetDecors, Rect pf, Rect df, Rect of, Rect cf, Rect vf, DisplayFrames displayFrames) {
        if (win.isInputMethodTarget() || !attached.isInputMethodTarget()) {
            if (adjust != 16) {
                cf.set((1073741824 & fl) != 0 ? attached.getContentFrameLw() : attached.getOverscanFrameLw());
                if (attached.getAttrs().type == 2011 && cf.bottom > attached.getContentFrameLw().bottom) {
                    cf.bottom = attached.getContentFrameLw().bottom;
                }
            } else {
                cf.set(attached.getContentFrameLw());
                if (attached.isVoiceInteraction()) {
                    cf.intersectUnchecked(displayFrames.mVoiceContent);
                } else if (win.isInputMethodTarget() || attached.isInputMethodTarget()) {
                    cf.intersectUnchecked(displayFrames.mContent);
                }
            }
            df.set(insetDecors ? attached.getDisplayFrameLw() : cf);
            of.set(insetDecors ? attached.getOverscanFrameLw() : cf);
            vf.set(attached.getVisibleFrameLw());
        } else {
            vf.set(displayFrames.mDock);
            cf.set(displayFrames.mDock);
            of.set(displayFrames.mDock);
            df.set(displayFrames.mDock);
        }
        pf.set((fl & 256) == 0 ? attached.getFrameLw() : df);
    }

    private void applyStableConstraints(int sysui, int fl, Rect r, DisplayFrames displayFrames) {
        if ((sysui & 256) != 0) {
            if ((fl & 1024) != 0) {
                r.intersectUnchecked(displayFrames.mStableFullscreen);
            } else {
                r.intersectUnchecked(displayFrames.mStable);
            }
        }
    }

    private boolean canReceiveInput(WindowState win) {
        if (((win.getAttrs().flags & 8) != 0) ^ ((win.getAttrs().flags & 131072) != 0)) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:135:0x0357  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x0350  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x0396  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x0387  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09d8  */
    /* JADX WARNING: Removed duplicated region for block: B:425:0x09d2  */
    /* JADX WARNING: Removed duplicated region for block: B:425:0x09d2  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09d8  */
    /* JADX WARNING: Removed duplicated region for block: B:375:0x089c  */
    /* JADX WARNING: Removed duplicated region for block: B:374:0x0887  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09d8  */
    /* JADX WARNING: Removed duplicated region for block: B:425:0x09d2  */
    /* JADX WARNING: Removed duplicated region for block: B:425:0x09d2  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09d8  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x09d8  */
    /* JADX WARNING: Removed duplicated region for block: B:425:0x09d2  */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x069a  */
    /* JADX WARNING: Removed duplicated region for block: B:265:0x0618  */
    /* JADX WARNING: Removed duplicated region for block: B:304:0x06a9  */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06d4  */
    /* JADX WARNING: Removed duplicated region for block: B:307:0x06b6  */
    /* JADX WARNING: Removed duplicated region for block: B:265:0x0618  */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x069a  */
    /* JADX WARNING: Removed duplicated region for block: B:304:0x06a9  */
    /* JADX WARNING: Removed duplicated region for block: B:307:0x06b6  */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06d4  */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x069a  */
    /* JADX WARNING: Removed duplicated region for block: B:265:0x0618  */
    /* JADX WARNING: Removed duplicated region for block: B:304:0x06a9  */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06d4  */
    /* JADX WARNING: Removed duplicated region for block: B:307:0x06b6  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0260  */
    /* JADX WARNING: Missing block: B:242:0x05a1, code:
            if (r12 <= 1999) goto L_0x05ac;
     */
    /* JADX WARNING: Missing block: B:244:0x05a5, code:
            if (r12 == 2020) goto L_0x05ac;
     */
    /* JADX WARNING: Missing block: B:283:0x065d, code:
            r3 = r60;
     */
    /* JADX WARNING: Missing block: B:284:0x0664, code:
            if ((r3.flags & Integer.MIN_VALUE) != 0) goto L_0x068e;
     */
    /* JADX WARNING: Missing block: B:286:0x066b, code:
            if ((r3.flags & 67108864) != 0) goto L_0x068e;
     */
    /* JADX WARNING: Missing block: B:288:0x066f, code:
            if ((r6 & 3076) != 0) goto L_0x068e;
     */
    /* JADX WARNING: Missing block: B:290:0x0674, code:
            if (r3.type == 3) goto L_0x068e;
     */
    /* JADX WARNING: Missing block: B:292:0x067a, code:
            if (r3.type == 2038) goto L_0x068e;
     */
    /* JADX WARNING: Missing block: B:293:0x067c, code:
            r0 = r10.mUnrestricted.top + r13.mStatusBarHeightForRotation[r10.mRotation];
            r15.top = r0;
            r2 = r61;
            r2.top = r0;
     */
    /* JADX WARNING: Missing block: B:294:0x068e, code:
            r2 = r61;
     */
    /* JADX WARNING: Missing block: B:403:0x08f1, code:
            if (r6.type != 2013) goto L_0x08f6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void layoutWindowLw(WindowState win, WindowState attached, DisplayFrames displayFrames) {
        Throwable th;
        PhoneWindowManager type = this;
        WindowState windowState = win;
        DisplayFrames displayFrames2 = displayFrames;
        DisplayFrames displayFrames3;
        PhoneWindowManager phoneWindowManager;
        if ((windowState == type.mStatusBar && !canReceiveInput(win)) || windowState == type.mNavigationBar || type.mScreenDecorWindows.contains(windowState)) {
            displayFrames3 = displayFrames2;
            phoneWindowManager = type;
            return;
        }
        boolean hasNavBar;
        Rect of;
        Rect pf;
        Rect cf;
        LayoutParams attrs;
        int sysUiFl;
        Rect dcf;
        Rect vf;
        Rect sf;
        int min;
        boolean topAtRest;
        int type2;
        int type3;
        int i;
        Rect df;
        boolean parentFrameWasClippedByDisplayCutout;
        int type4;
        LayoutParams attrs2 = win.getAttrs();
        boolean isDefaultDisplay = win.isDefaultDisplay();
        boolean z = isDefaultDisplay && windowState == type.mLastInputMethodTargetWindow && type.mLastInputMethodWindow != null;
        if (z) {
            type.offsetInputMethodWindowLw(type.mLastInputMethodWindow, displayFrames2);
        }
        int type5 = attrs2.type;
        int fl = PolicyControl.getWindowFlags(windowState, attrs2);
        int pfl = attrs2.privateFlags;
        int sim = attrs2.softInputMode;
        int requestedSysUiFl = PolicyControl.getSystemUiVisibility(null, attrs2);
        int sysUiFl2 = requestedSysUiFl | type.getImpliedSysUiFlagsForLayout(attrs2);
        Rect pf2 = mTmpParentFrame;
        Rect df2 = mTmpDisplayFrame;
        Rect of2 = mTmpOverscanFrame;
        Rect cf2 = mTmpContentFrame;
        Rect vf2 = mTmpVisibleFrame;
        Rect dcf2 = mTmpDecorFrame;
        int type6 = type5;
        Rect sf2 = mTmpStableFrame;
        Rect osf = null;
        dcf2.setEmpty();
        boolean isNeedHideNaviBarWin = mUsingHwNavibar && (attrs2.privateFlags & Integer.MIN_VALUE) != 0;
        boolean isPCDisplay = false;
        if (HwPCUtils.isPcCastModeInServer()) {
            isPCDisplay = HwPCUtils.isValidExtDisplayId(win.getDisplayId());
        }
        if (HwPCUtils.isPcCastModeInServer() && !isDefaultDisplay && isPCDisplay) {
            isNeedHideNaviBarWin = false;
            hasNavBar = type.mHasNavigationBar && getNavigationBarExternal() != null && getNavigationBarExternal().isVisibleLw();
        } else {
            hasNavBar = isDefaultDisplay && type.mHasNavigationBar && type.mNavigationBar != null && type.mNavigationBar.isVisibleLw();
        }
        boolean isNeedHideNaviBarWin2 = isNeedHideNaviBarWin;
        boolean hasNavBar2 = hasNavBar;
        boolean isBaiDuOrSwiftkey = isBaiDuOrSwiftkey(win);
        boolean isLandScapeMultiWindowMode = isLandScapeMultiWindowMode();
        boolean isDocked = false;
        if (attrs2.type) {
            isNeedHideNaviBarWin = isLandScapeMultiWindowMode && windowState != type.mInputMethodTarget;
            isDocked = isNeedHideNaviBarWin;
        }
        int adjust = (!isLandScapeMultiWindowMode || windowState == type.mInputMethodTarget) ? sim & 240 : 48;
        hasNavBar = ((fl & 1024) == 0 && (requestedSysUiFl & 4) == 0) ? false : true;
        boolean requestedFullscreen = hasNavBar;
        Rect of3 = of2;
        boolean layoutInScreen = (fl & 256) == 256;
        boolean layoutInsetDecor = (65536 & fl) == 65536;
        sf2.set(displayFrames2.mStable);
        int sim2 = sim;
        int adjust2;
        int fl2;
        Rect dcf3;
        int i2;
        int i3;
        Rect vf3;
        int sysUiFl3;
        LayoutParams attrs3;
        if (HwPCUtils.isPcCastModeInServer() && isPCDisplay) {
            int sysUiFl4;
            Rect sf3;
            int type7;
            Rect vf4;
            LayoutParams attrs4;
            Rect df3;
            Rect pf3;
            Rect cf3;
            if (attached != null) {
                of = of3;
                df3 = df2;
                pf3 = pf2;
                sysUiFl4 = sysUiFl2;
                adjust2 = adjust;
                int i4 = 2011;
                of3 = pfl;
                fl2 = fl;
                sf3 = sf2;
                type7 = type6;
                dcf3 = dcf2;
                cf3 = cf2;
                vf4 = vf2;
                attrs4 = attrs2;
                displayFrames2 = displayFrames;
                type.setAttachedWindowFrames(windowState, fl, adjust, attached, true, pf3, df3, of, cf3, vf4, displayFrames2);
                dcf2 = of;
                sf2 = df3;
                pf = pf3;
                cf = cf3;
            } else {
                df3 = df2;
                pf3 = pf2;
                sysUiFl4 = sysUiFl2;
                adjust2 = adjust;
                fl2 = fl;
                sf3 = sf2;
                dcf3 = dcf2;
                attrs4 = attrs2;
                cf3 = cf2;
                vf4 = vf2;
                type7 = type6;
                of = of3;
                i2 = sim2;
                of3 = pfl;
                displayFrames2 = displayFrames;
                i3 = displayFrames2.mOverscan.left;
                cf = cf3;
                cf.left = i3;
                dcf2 = of;
                dcf2.left = i3;
                sf2 = df3;
                sf2.left = i3;
                pf = pf3;
                pf.left = i3;
                i3 = displayFrames2.mOverscan.top;
                cf.top = i3;
                dcf2.top = i3;
                sf2.top = i3;
                pf.top = i3;
                i3 = displayFrames2.mOverscan.right;
                cf.right = i3;
                dcf2.right = i3;
                sf2.right = i3;
                pf.right = i3;
                i3 = displayFrames2.mOverscan.bottom;
                cf.bottom = i3;
                dcf2.bottom = i3;
                sf2.bottom = i3;
                pf.bottom = i3;
            }
            if (attrs4.type == 2011) {
                i3 = displayFrames2.mDock.left;
                vf3 = vf4;
                vf3.left = i3;
                cf.left = i3;
                dcf2.left = i3;
                sf2.left = i3;
                pf.left = i3;
                i3 = displayFrames2.mDock.top;
                vf3.top = i3;
                cf.top = i3;
                dcf2.top = i3;
                sf2.top = i3;
                pf.top = i3;
                i3 = displayFrames2.mDock.right;
                vf3.right = i3;
                cf.right = i3;
                dcf2.right = i3;
                sf2.right = i3;
                pf.right = i3;
                i3 = displayFrames2.mUnrestricted.top + displayFrames2.mUnrestricted.height();
                dcf2.bottom = i3;
                sf2.bottom = i3;
                pf.bottom = i3;
                i3 = displayFrames2.mStable.bottom;
                vf3.bottom = i3;
                cf.bottom = i3;
                attrs4.gravity = 80;
                type.mDockLayer = win.getSurfaceLayer();
                attrs = attrs4;
                sysUiFl = sysUiFl4;
                adjust = fl2;
                sysUiFl2 = type7;
                cf2 = sf3;
                dcf = dcf3;
                vf = vf3;
                pfl = adjust2;
            } else {
                vf3 = vf4;
                sim = sysUiFl4;
                if ((sim & 2) != 0) {
                    sf = sf3;
                } else if (getNavigationBarExternal() == null || getNavigationBarExternal().isVisibleLw()) {
                    sf = sf3;
                    cf.set(sf);
                    if (attrs4.type == 2008 && HwPCUtils.enabledInPad()) {
                        cf.bottom = displayFrames2.mContent.bottom;
                    }
                    if (attrs4.gravity == 80) {
                        attrs4.gravity = 17;
                    }
                    cf2 = sf;
                    sysUiFl3 = sim;
                    attrs3 = attrs4;
                    vf = vf3;
                    type.layoutWindowForPadPCMode(windowState, pf, sf2, cf, vf3, displayFrames2.mContent.bottom);
                    pfl = adjust2;
                    adjust = fl2;
                    sysUiFl2 = type7;
                    dcf = dcf3;
                    sysUiFl = sysUiFl3;
                    attrs = attrs3;
                } else {
                    sf = sf3;
                }
                cf.set(sf2);
                cf.bottom = displayFrames2.mContent.bottom;
                if (attrs4.gravity == 80) {
                }
                cf2 = sf;
                sysUiFl3 = sim;
                attrs3 = attrs4;
                vf = vf3;
                type.layoutWindowForPadPCMode(windowState, pf, sf2, cf, vf3, displayFrames2.mContent.bottom);
                pfl = adjust2;
                adjust = fl2;
                sysUiFl2 = type7;
                dcf = dcf3;
                sysUiFl = sysUiFl3;
                attrs = attrs3;
            }
        } else {
            sysUiFl3 = sysUiFl2;
            adjust2 = adjust;
            fl2 = fl;
            dcf3 = dcf2;
            attrs3 = attrs2;
            cf = cf2;
            vf = vf2;
            dcf2 = of3;
            i2 = sim2;
            pf = pf2;
            int pfl2 = pfl;
            cf2 = sf2;
            sf2 = df2;
            pfl = type6;
            LayoutParams attrs5;
            if (pfl == 2011) {
                LayoutParams focusAttrs;
                vf.set(displayFrames2.mDock);
                cf.set(displayFrames2.mDock);
                dcf2.set(displayFrames2.mDock);
                sf2.set(displayFrames2.mDock);
                pf.set(displayFrames2.mDock);
                if (isLandScapeMultiWindowMode && type.mInputMethodTarget != null && isBaiDuOrSwiftkey) {
                    if (!type.mFrozen) {
                        if (type.mInputMethodTarget.getAttrs().type == 2) {
                            df2 = type.mInputMethodTarget.getDisplayFrameLw();
                        } else {
                            df2 = type.mInputMethodTarget.getContentFrameLw();
                        }
                        sysUiFl = df2.left;
                        vf.left = sysUiFl;
                        cf.left = sysUiFl;
                        dcf2.left = sysUiFl;
                        sf2.left = sysUiFl;
                        pf.left = sysUiFl;
                        sysUiFl = df2.right;
                        vf.right = sysUiFl;
                        cf.right = sysUiFl;
                        dcf2.right = sysUiFl;
                        sf2.right = sysUiFl;
                        pf.right = sysUiFl;
                    } else {
                        return;
                    }
                }
                min = Math.min(displayFrames2.mUnrestricted.bottom, displayFrames2.mDisplayCutoutSafe.bottom);
                dcf2.bottom = min;
                sf2.bottom = min;
                pf.bottom = min;
                min = displayFrames2.mStable.bottom;
                vf.bottom = min;
                cf.bottom = min;
                if (mSupporInputMethodFilletAdaptation && !hasNavBar2 && type.mDisplayRotation == 0 && win.isImeWithHwFlag()) {
                    attrs5 = attrs3;
                    if ((attrs5.hwFlags & DumpState.DUMP_DEXOPT) == 0) {
                        min = displayFrames2.mStable.bottom - (type.mDefaultNavBarHeight / 2);
                        vf.bottom = min;
                        cf.bottom = min;
                        type.mInputMethodMovedUp = true;
                        focusAttrs = type.mFocusedWindow == null ? type.mFocusedWindow.getAttrs() : null;
                        if (!(focusAttrs == null || (focusAttrs.privateFlags & 1024) == 0 || hasNavBar2)) {
                            sysUiFl = displayFrames2.mSystem.bottom;
                            dcf2.bottom = sysUiFl;
                            vf.bottom = sysUiFl;
                            cf.bottom = sysUiFl;
                            sf2.bottom = sysUiFl;
                            pf.bottom = sysUiFl;
                        }
                        if (type.mStatusBar != null && type.mFocusedWindow == type.mStatusBar && type.canReceiveInput(type.mStatusBar)) {
                            if (type.mNavigationBarPosition != 2) {
                                sysUiFl = displayFrames2.mStable.right;
                                vf.right = sysUiFl;
                                cf.right = sysUiFl;
                                dcf2.right = sysUiFl;
                                sf2.right = sysUiFl;
                                pf.right = sysUiFl;
                            } else if (type.mNavigationBarPosition == 1) {
                                sysUiFl = displayFrames2.mStable.left;
                                vf.left = sysUiFl;
                                cf.left = sysUiFl;
                                dcf2.left = sysUiFl;
                                sf2.left = sysUiFl;
                                pf.left = sysUiFl;
                            }
                        }
                        attrs5.gravity = 80;
                        type.mDockLayer = win.getSurfaceLayer();
                        attrs = attrs5;
                        sysUiFl2 = pfl;
                        pfl = adjust2;
                        adjust = fl2;
                        dcf = dcf3;
                        sysUiFl = sysUiFl3;
                    }
                } else {
                    attrs5 = attrs3;
                }
                type.mInputMethodMovedUp = false;
                if (type.mFocusedWindow == null) {
                }
                sysUiFl = displayFrames2.mSystem.bottom;
                dcf2.bottom = sysUiFl;
                vf.bottom = sysUiFl;
                cf.bottom = sysUiFl;
                sf2.bottom = sysUiFl;
                pf.bottom = sysUiFl;
                if (type.mNavigationBarPosition != 2) {
                }
                attrs5.gravity = 80;
                type.mDockLayer = win.getSurfaceLayer();
                attrs = attrs5;
                sysUiFl2 = pfl;
                pfl = adjust2;
                adjust = fl2;
                dcf = dcf3;
                sysUiFl = sysUiFl3;
            } else {
                attrs5 = attrs3;
                if (pfl == 2031) {
                    dcf2.set(displayFrames2.mUnrestricted);
                    sf2.set(displayFrames2.mUnrestricted);
                    pf.set(displayFrames2.mUnrestricted);
                    sysUiFl2 = adjust2;
                    if (sysUiFl2 != 16) {
                        cf.set(displayFrames2.mDock);
                    } else {
                        cf.set(displayFrames2.mContent);
                    }
                    if (sysUiFl2 != 48) {
                        vf.set(displayFrames2.mCurrent);
                    } else {
                        vf.set(cf);
                    }
                    attrs = attrs5;
                    adjust = fl2;
                    dcf = dcf3;
                    sysUiFl = sysUiFl3;
                    int i5 = pfl;
                    pfl = sysUiFl2;
                    sysUiFl2 = i5;
                } else {
                    int type8;
                    LayoutParams attrs6;
                    sysUiFl2 = adjust2;
                    int i6;
                    if (pfl == 2013) {
                        type8 = pfl;
                        pfl = sysUiFl2;
                        attrs6 = attrs5;
                        i6 = 2011;
                        type.layoutWallpaper(displayFrames2, pf, sf2, dcf2, cf);
                    } else {
                        attrs6 = attrs5;
                        i6 = 2011;
                        type8 = pfl;
                        pfl = sysUiFl2;
                        if (windowState == type.mStatusBar) {
                            dcf2.set(displayFrames2.mUnrestricted);
                            sf2.set(displayFrames2.mUnrestricted);
                            pf.set(displayFrames2.mUnrestricted);
                            cf.set(displayFrames2.mStable);
                            vf.set(displayFrames2.mStable);
                            if (pfl == 16) {
                                cf.bottom = displayFrames2.mContent.bottom;
                            } else {
                                cf.bottom = displayFrames2.mDock.bottom;
                                vf.bottom = displayFrames2.mContent.bottom;
                            }
                        } else {
                            Rect dcf4 = dcf3;
                            dcf4.set(displayFrames2.mSystem);
                            attrs5 = attrs6;
                            boolean inheritTranslucentDecor = (attrs5.privateFlags & 512) != 0;
                            sysUiFl2 = type8;
                            z = sysUiFl2 >= 1 && sysUiFl2 <= 99;
                            boolean isAppWindow = z;
                            z = windowState == type.mTopFullscreenOpaqueWindowState && !win.isAnimatingLw();
                            topAtRest = z;
                            if (!isAppWindow || inheritTranslucentDecor || topAtRest) {
                                i3 = fl2;
                                sysUiFl = sysUiFl3;
                            } else {
                                sysUiFl = sysUiFl3;
                                if ((sysUiFl & 4) == 0) {
                                    i3 = fl2;
                                    if ((i3 & 1024) == 0 && (i3 & 67108864) == 0 && (i3 & Integer.MIN_VALUE) == 0 && (pfl2 & 131072) == 0 && type.mStatusBar != null && type.mStatusBar.isVisibleLw() && (type.mLastSystemUiFlags & 67108864) == 0) {
                                        dcf4.top = displayFrames2.mStable.top;
                                    }
                                } else {
                                    i3 = fl2;
                                }
                                if ((134217728 & i3) == 0 && (sysUiFl & 2) == 0 && !isNeedHideNaviBarWin2 && (i3 & Integer.MIN_VALUE) == 0) {
                                    dcf4.bottom = displayFrames2.mStable.bottom;
                                    dcf4.right = displayFrames2.mStable.right;
                                }
                            }
                            LayoutParams attrs7;
                            int fl3;
                            int sysUiFl5;
                            LayoutParams attrs8;
                            Rect dcf5;
                            int adjust3;
                            Rect pf4;
                            Rect df4;
                            Rect of4;
                            Rect cf4;
                            if (!layoutInScreen || !layoutInsetDecor) {
                                int sysUiFl6;
                                int fl4;
                                Rect rect = cf;
                                int fl5 = i3;
                                of2 = dcf4;
                                sim = sysUiFl;
                                attrs7 = attrs5;
                                sf = rect;
                                DisplayFrames displayFrames4 = displayFrames2;
                                type2 = sysUiFl2;
                                sysUiFl2 = pfl;
                                vf3 = pf;
                                pf = sf2;
                                sf2 = dcf2;
                                DisplayFrames displayFrames5 = displayFrames4;
                                if (layoutInScreen) {
                                    dcf = of2;
                                    sysUiFl6 = sim;
                                    fl4 = fl5;
                                    type3 = type2;
                                    attrs = attrs7;
                                    cf = sf;
                                    displayFrames2 = displayFrames5;
                                    dcf2 = sf2;
                                    sf2 = pf;
                                    pf = vf3;
                                    pfl = sysUiFl2;
                                } else if ((sim & 1536) == 0 || ((attrs7.flags & Integer.MIN_VALUE) != 0 && type.getEmuiStyleValue(attrs7.isEmuiStyle) == 1)) {
                                    LayoutParams attrs9;
                                    int adjust4;
                                    Rect cf5;
                                    Rect pf5;
                                    Rect df5;
                                    Rect of5;
                                    if (attached != null) {
                                        dcf = of2;
                                        attrs9 = attrs7;
                                        adjust4 = sysUiFl2;
                                        cf5 = sf;
                                        sysUiFl6 = sim;
                                        pf5 = vf3;
                                        df5 = pf;
                                        of5 = sf2;
                                        fl4 = fl5;
                                        type3 = type2;
                                        displayFrames2 = displayFrames;
                                        type.setAttachedWindowFrames(windowState, fl5, sysUiFl2, attached, false, vf3, pf, sf2, cf5, vf, displayFrames2);
                                        sysUiFl2 = type3;
                                        attrs = attrs9;
                                        pfl = adjust4;
                                        cf = cf5;
                                        sysUiFl = sysUiFl6;
                                        pf = pf5;
                                        sf2 = df5;
                                        dcf2 = of5;
                                    } else {
                                        dcf = of2;
                                        attrs9 = attrs7;
                                        adjust4 = sysUiFl2;
                                        cf5 = sf;
                                        sysUiFl6 = sim;
                                        pf5 = vf3;
                                        df5 = pf;
                                        of5 = sf2;
                                        fl4 = fl5;
                                        type3 = type2;
                                        if (type3 == 2014) {
                                            displayFrames2 = displayFrames;
                                            cf = cf5;
                                            cf.set(displayFrames2.mRestricted);
                                            dcf2 = of5;
                                            dcf2.set(displayFrames2.mRestricted);
                                            sf2 = df5;
                                            sf2.set(displayFrames2.mRestricted);
                                            pf = pf5;
                                            pf.set(displayFrames2.mRestricted);
                                            sysUiFl2 = type3;
                                            attrs = attrs9;
                                            pfl = adjust4;
                                        } else {
                                            displayFrames2 = displayFrames;
                                            cf = cf5;
                                            pf = pf5;
                                            sf2 = df5;
                                            dcf2 = of5;
                                            if (type3 == 2005) {
                                                pfl = adjust4;
                                            } else if (type3 == 2003) {
                                                pfl = adjust4;
                                            } else {
                                                pf.set(displayFrames2.mContent);
                                                if (isDocked) {
                                                    pf.bottom = displayFrames2.mDock.bottom;
                                                }
                                                if (win.isVoiceInteraction()) {
                                                    cf.set(displayFrames2.mVoiceContent);
                                                    dcf2.set(displayFrames2.mVoiceContent);
                                                    sf2.set(displayFrames2.mVoiceContent);
                                                    pfl = adjust4;
                                                } else {
                                                    pfl = adjust4;
                                                    if (pfl != 16) {
                                                        cf.set(displayFrames2.mDock);
                                                        dcf2.set(displayFrames2.mDock);
                                                        sf2.set(displayFrames2.mDock);
                                                    } else {
                                                        cf.set(displayFrames2.mContent);
                                                        dcf2.set(displayFrames2.mContent);
                                                        sf2.set(displayFrames2.mContent);
                                                    }
                                                }
                                                if (pfl != 48) {
                                                    vf.set(displayFrames2.mCurrent);
                                                } else {
                                                    vf.set(cf);
                                                }
                                                sysUiFl2 = type3;
                                                attrs = attrs9;
                                            }
                                            cf.set(displayFrames2.mStable);
                                            dcf2.set(displayFrames2.mStable);
                                            sf2.set(displayFrames2.mStable);
                                            attrs = attrs9;
                                            if (attrs.type == 2003) {
                                                pf.set(displayFrames2.mCurrent);
                                            } else {
                                                pf.set(displayFrames2.mStable);
                                            }
                                            sysUiFl2 = type3;
                                        }
                                        sysUiFl = sysUiFl6;
                                    }
                                    adjust = fl4;
                                    type = this;
                                } else {
                                    dcf = of2;
                                    sysUiFl6 = sim;
                                    fl4 = fl5;
                                    type3 = type2;
                                    attrs = attrs7;
                                    cf = sf;
                                    displayFrames2 = displayFrames5;
                                    dcf2 = sf2;
                                    sf2 = pf;
                                    pf = vf3;
                                    pfl = sysUiFl2;
                                }
                                if (type3 == 2014) {
                                    sysUiFl2 = type3;
                                    sysUiFl = sysUiFl6;
                                    adjust = fl4;
                                    type = this;
                                } else if (type3 == 2017) {
                                    sysUiFl2 = type3;
                                    sysUiFl = sysUiFl6;
                                    adjust = fl4;
                                    type = this;
                                } else {
                                    if (type3 == 2019) {
                                        sysUiFl2 = type3;
                                        sysUiFl = sysUiFl6;
                                        adjust = fl4;
                                        type = this;
                                    } else if (type3 == 2024) {
                                        sysUiFl2 = type3;
                                        sysUiFl = sysUiFl6;
                                        adjust = fl4;
                                        type = this;
                                    } else {
                                        if (type3 == 2015 || type3 == 2036) {
                                            adjust = fl4;
                                            if ((adjust & 1024) != 0) {
                                                cf.set(displayFrames2.mOverscan);
                                                dcf2.set(displayFrames2.mOverscan);
                                                sf2.set(displayFrames2.mOverscan);
                                                pf.set(displayFrames2.mOverscan);
                                            }
                                            if (type3 != 2021) {
                                                cf.set(displayFrames2.mOverscan);
                                                dcf2.set(displayFrames2.mOverscan);
                                                sf2.set(displayFrames2.mOverscan);
                                                pf.set(displayFrames2.mOverscan);
                                            } else if ((DumpState.DUMP_HANDLE & adjust) == 0 || type3 < 1 || type3 > 1999) {
                                                sysUiFl2 = type3;
                                                type = this;
                                                if (canHideNavigationBar()) {
                                                    sysUiFl = sysUiFl6;
                                                    if ((sysUiFl & 512) != 0 && (sysUiFl2 == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || sysUiFl2 == 2005 || sysUiFl2 == 2034 || sysUiFl2 == 2033 || (sysUiFl2 >= 1 && sysUiFl2 <= 1999))) {
                                                        cf.set(displayFrames2.mUnrestricted);
                                                        dcf2.set(displayFrames2.mUnrestricted);
                                                        sf2.set(displayFrames2.mUnrestricted);
                                                        pf.set(displayFrames2.mUnrestricted);
                                                        type.applyStableConstraints(sysUiFl, adjust, cf, displayFrames2);
                                                        if (pfl == 48) {
                                                            vf.set(displayFrames2.mCurrent);
                                                        } else {
                                                            vf.set(cf);
                                                        }
                                                    }
                                                } else {
                                                    sysUiFl = sysUiFl6;
                                                }
                                                if (mUsingHwNavibar) {
                                                }
                                                if (!isNeedHideNaviBarWin2) {
                                                    if ("com.huawei.systemui.mk.lighterdrawer.LighterDrawView".equals(attrs.getTitle())) {
                                                        cf.set(displayFrames2.mUnrestricted);
                                                        dcf2.set(displayFrames2.mUnrestricted);
                                                        sf2.set(displayFrames2.mUnrestricted);
                                                        pf.set(displayFrames2.mUnrestricted);
                                                        vf.set(displayFrames2.mUnrestricted);
                                                    } else if ((sysUiFl & 1024) != 0) {
                                                        dcf2.set(displayFrames2.mRestricted);
                                                        sf2.set(displayFrames2.mRestricted);
                                                        pf.set(displayFrames2.mRestricted);
                                                        if (pfl != 16) {
                                                            cf.set(displayFrames2.mDock);
                                                        } else {
                                                            cf.set(displayFrames2.mContent);
                                                        }
                                                    } else {
                                                        cf.set(displayFrames2.mRestricted);
                                                        dcf2.set(displayFrames2.mRestricted);
                                                        sf2.set(displayFrames2.mRestricted);
                                                        pf.set(displayFrames2.mRestricted);
                                                    }
                                                    type.applyStableConstraints(sysUiFl, adjust, cf, displayFrames2);
                                                    if (pfl == 48) {
                                                    }
                                                }
                                                cf.set(displayFrames2.mUnrestricted);
                                                dcf2.set(displayFrames2.mUnrestricted);
                                                sf2.set(displayFrames2.mUnrestricted);
                                                pf.set(displayFrames2.mUnrestricted);
                                                type.applyStableConstraints(sysUiFl, adjust, cf, displayFrames2);
                                                if (pfl == 48) {
                                                }
                                            } else {
                                                cf.set(displayFrames2.mOverscan);
                                                dcf2.set(displayFrames2.mOverscan);
                                                sf2.set(displayFrames2.mOverscan);
                                                pf.set(displayFrames2.mOverscan);
                                            }
                                        } else {
                                            adjust = fl4;
                                            if (type3 != 2021) {
                                            }
                                        }
                                        sysUiFl2 = type3;
                                        sysUiFl = sysUiFl6;
                                        type = this;
                                        type.applyStableConstraints(sysUiFl, adjust, cf, displayFrames2);
                                        if (pfl == 48) {
                                        }
                                    }
                                    dcf2.set(displayFrames2.mUnrestricted);
                                    sf2.set(displayFrames2.mUnrestricted);
                                    pf.set(displayFrames2.mUnrestricted);
                                    type.applyStableConstraints(sysUiFl, adjust, cf, displayFrames2);
                                    if (pfl == 48) {
                                    }
                                }
                                cf.set(displayFrames2.mUnrestricted);
                                dcf2.set(displayFrames2.mUnrestricted);
                                sf2.set(displayFrames2.mUnrestricted);
                                pf.set(displayFrames2.mUnrestricted);
                                if (hasNavBar2) {
                                    i = displayFrames2.mDock.left;
                                    cf.left = i;
                                    dcf2.left = i;
                                    sf2.left = i;
                                    pf.left = i;
                                    i = displayFrames2.mRestricted.right;
                                    cf.right = i;
                                    dcf2.right = i;
                                    sf2.right = i;
                                    pf.right = i;
                                    i = displayFrames2.mRestricted.bottom;
                                    cf.bottom = i;
                                    dcf2.bottom = i;
                                    sf2.bottom = i;
                                    pf.bottom = i;
                                }
                                type.applyStableConstraints(sysUiFl, adjust, cf, displayFrames2);
                                if (pfl == 48) {
                                }
                            } else if (attached != null) {
                                fl3 = i3;
                                sysUiFl5 = sysUiFl;
                                attrs8 = attrs5;
                                dcf5 = dcf4;
                                adjust3 = pfl;
                                int type9 = sysUiFl2;
                                pf4 = pf;
                                df4 = sf2;
                                of4 = dcf2;
                                cf4 = cf;
                                displayFrames2 = displayFrames;
                                type.setAttachedWindowFrames(windowState, fl3, pfl, attached, true, pf, sf2, dcf2, cf, vf, displayFrames2);
                                adjust = fl3;
                                sysUiFl = sysUiFl5;
                                attrs = attrs8;
                                dcf = dcf5;
                                sysUiFl2 = type9;
                                pfl = adjust3;
                                pf = pf4;
                                sf2 = df4;
                                dcf2 = of4;
                                cf = cf4;
                            } else {
                                LayoutParams layoutParams;
                                Rect rect2;
                                Rect rect3;
                                fl3 = i3;
                                sysUiFl5 = sysUiFl;
                                attrs8 = attrs5;
                                dcf5 = dcf4;
                                adjust3 = pfl;
                                pf4 = pf;
                                df4 = sf2;
                                of4 = dcf2;
                                cf4 = cf;
                                type2 = sysUiFl2;
                                if (type2 == 2014) {
                                    dcf2 = displayFrames;
                                    cf = fl3;
                                    attrs = sysUiFl5;
                                    pfl = pf4;
                                    pf = df4;
                                    sf2 = of4;
                                } else if (type2 == 2017) {
                                    dcf2 = displayFrames;
                                    cf = fl3;
                                    attrs = sysUiFl5;
                                    pfl = pf4;
                                    pf = df4;
                                    sf2 = of4;
                                } else {
                                    cf = fl3;
                                    if ((DumpState.DUMP_HANDLE & cf) == 0 || type2 < 1 || type2 > 1999) {
                                        dcf2 = displayFrames;
                                        pfl = pf4;
                                        pf = df4;
                                        sf2 = of4;
                                        if (canHideNavigationBar()) {
                                            attrs = sysUiFl5;
                                            if ((attrs & 512) != 0) {
                                                if (type2 >= 1) {
                                                }
                                            }
                                        } else {
                                            attrs = sysUiFl5;
                                        }
                                        if (!isNeedHideNaviBarWin2) {
                                            pf.set(dcf2.mRestrictedOverscan);
                                            pfl.set(dcf2.mRestrictedOverscan);
                                            sf2.set(dcf2.mUnrestricted);
                                            if ((cf & 1024) != 0) {
                                                if (win.isVoiceInteraction()) {
                                                    attrs5 = cf4;
                                                    attrs5.set(dcf2.mVoiceContent);
                                                    sysUiFl2 = adjust3;
                                                } else {
                                                    attrs5 = cf4;
                                                    sysUiFl2 = adjust3;
                                                    if (sysUiFl2 != 16) {
                                                        attrs5.set(dcf2.mDock);
                                                    } else {
                                                        attrs5.set(dcf2.mContent);
                                                    }
                                                }
                                                synchronized (type.mWindowManagerFuncs.getWindowManagerLock()) {
                                                    try {
                                                        if (!win.hasDrawnLw()) {
                                                            try {
                                                                attrs5.top = dcf2.mUnrestricted.top + type.mStatusBarHeightForRotation[dcf2.mRotation];
                                                            } catch (Throwable th2) {
                                                                th = th2;
                                                                attrs7 = attrs8;
                                                                df2 = dcf5;
                                                            }
                                                        }
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        attrs7 = attrs8;
                                                        df2 = dcf5;
                                                        while (true) {
                                                            try {
                                                                break;
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                            }
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            }
                                            sysUiFl = attrs8;
                                            df2 = dcf5;
                                            sysUiFl2 = adjust3;
                                            attrs5 = cf4;
                                            attrs5.set(dcf2.mRestricted);
                                            if (isNeedHideNaviBarWin2) {
                                                attrs5.bottom = dcf2.mUnrestricted.bottom;
                                            }
                                            type.applyStableConstraints(attrs, cf, attrs5, dcf2);
                                            if (sysUiFl2 == 48) {
                                                vf.set(dcf2.mCurrent);
                                            } else {
                                                vf.set(attrs5);
                                            }
                                            dcf = df2;
                                            layoutParams = attrs;
                                            attrs = sysUiFl;
                                            sysUiFl = layoutParams;
                                            rect2 = pfl;
                                            pfl = sysUiFl2;
                                            sysUiFl2 = type2;
                                            displayFrames2 = dcf2;
                                            dcf2 = sf2;
                                            sf2 = pf;
                                            pf = rect2;
                                            rect3 = cf;
                                            cf = attrs5;
                                            adjust = rect3;
                                        }
                                        pf.set(dcf2.mOverscan);
                                        pfl.set(dcf2.mOverscan);
                                        sf2.set(dcf2.mUnrestricted);
                                        if ((cf & 1024) != 0) {
                                        }
                                        if (isNeedHideNaviBarWin2) {
                                        }
                                        type.applyStableConstraints(attrs, cf, attrs5, dcf2);
                                        if (sysUiFl2 == 48) {
                                        }
                                        dcf = df2;
                                        layoutParams = attrs;
                                        attrs = sysUiFl;
                                        sysUiFl = layoutParams;
                                        rect2 = pfl;
                                        pfl = sysUiFl2;
                                        sysUiFl2 = type2;
                                        displayFrames2 = dcf2;
                                        dcf2 = sf2;
                                        sf2 = pf;
                                        pf = rect2;
                                        rect3 = cf;
                                        cf = attrs5;
                                        adjust = rect3;
                                    } else {
                                        dcf2 = displayFrames;
                                        sf2 = of4;
                                        sf2.set(dcf2.mOverscan);
                                        pf = df4;
                                        pf.set(dcf2.mOverscan);
                                        pfl = pf4;
                                        pfl.set(dcf2.mOverscan);
                                        attrs = sysUiFl5;
                                        if ((cf & 1024) != 0) {
                                        }
                                        if (isNeedHideNaviBarWin2) {
                                        }
                                        type.applyStableConstraints(attrs, cf, attrs5, dcf2);
                                        if (sysUiFl2 == 48) {
                                        }
                                        dcf = df2;
                                        layoutParams = attrs;
                                        attrs = sysUiFl;
                                        sysUiFl = layoutParams;
                                        rect2 = pfl;
                                        pfl = sysUiFl2;
                                        sysUiFl2 = type2;
                                        displayFrames2 = dcf2;
                                        dcf2 = sf2;
                                        sf2 = pf;
                                        pf = rect2;
                                        rect3 = cf;
                                        cf = attrs5;
                                        adjust = rect3;
                                    }
                                }
                                i3 = (hasNavBar2 ? dcf2.mDock : dcf2.mUnrestricted).left;
                                sf2.left = i3;
                                pf.left = i3;
                                pfl.left = i3;
                                i3 = dcf2.mUnrestricted.top;
                                sf2.top = i3;
                                pf.top = i3;
                                pfl.top = i3;
                                if (hasNavBar2) {
                                    i3 = dcf2.mRestricted.right;
                                } else {
                                    i3 = dcf2.mUnrestricted.right;
                                }
                                sf2.right = i3;
                                pf.right = i3;
                                pfl.right = i3;
                                if (hasNavBar2) {
                                    i3 = dcf2.mRestricted.bottom;
                                } else {
                                    i3 = dcf2.mUnrestricted.bottom;
                                }
                                sf2.bottom = i3;
                                pf.bottom = i3;
                                pfl.bottom = i3;
                                if ((cf & 1024) != 0) {
                                }
                                if (isNeedHideNaviBarWin2) {
                                }
                                type.applyStableConstraints(attrs, cf, attrs5, dcf2);
                                if (sysUiFl2 == 48) {
                                }
                                dcf = df2;
                                layoutParams = attrs;
                                attrs = sysUiFl;
                                sysUiFl = layoutParams;
                                rect2 = pfl;
                                pfl = sysUiFl2;
                                sysUiFl2 = type2;
                                displayFrames2 = dcf2;
                                dcf2 = sf2;
                                sf2 = pf;
                                pf = rect2;
                                rect3 = cf;
                                cf = attrs5;
                                adjust = rect3;
                            }
                            if (!(type.mHwFullScreenWindow == null || !type.mHwFullScreenWinVisibility || type.mDisplayRotation != 0 || type.isAboveFullScreen(windowState, type.mHwFullScreenWindow) || win.toString().contains("GestureNavBottom") || win.toString().contains("ChargingAnimView") || win.toString().contains("ProximityWnd") || win.toString().contains("fingerprint_alpha_layer") || win.toString().contains("fingerprintview_button") || win.toString().contains("fingerprint_mask") || (win.getOwningPackage() != null && (win.getOwningPackage().contains("com.huawei.android.launcher") || win.getOwningPackage().contains("com.huawei.aod"))))) {
                                Rect cf_fullWinBtn = type.mHwFullScreenWindow.getContentFrameLw();
                                z = cf.bottom > cf_fullWinBtn.top;
                                if (z) {
                                    i3 = (displayFrames2.mUnrestricted.top + type.mRestrictedScreenHeight) - (cf.bottom - cf_fullWinBtn.top);
                                    cf.bottom = i3;
                                    dcf2.bottom = i3;
                                    sf2.bottom = i3;
                                    pf.bottom = i3;
                                }
                            }
                        }
                    }
                    adjust = fl2;
                    dcf = dcf3;
                    sysUiFl = sysUiFl3;
                    sysUiFl2 = type8;
                    attrs = attrs6;
                }
            }
        }
        type2 = sysUiFl2;
        int fl6 = adjust;
        sf = dcf2;
        Rect of6 = dcf2;
        LayoutParams attrs10 = attrs;
        Rect df6 = sf2;
        sf2 = pf;
        type.overrideRectForForceRotation(windowState, pf, sf2, sf, cf, vf, dcf);
        if ((isNeedHideNaviBarWin2 || !mUsingHwNavibar) && attrs10.type == 2004) {
            z = false;
            vf.top = 0;
            cf.top = 0;
            min = sf2.bottom;
            vf.bottom = min;
            cf.bottom = min;
        } else {
            z = false;
        }
        if (!mUsingHwNavibar) {
            pf = dcf;
        } else if (isNeedHideNaviBarWin2 || type.isNavibarHide) {
            min = sf2.bottom;
            vf.bottom = min;
            cf.bottom = min;
            pf = dcf;
            pf.bottom = min;
        } else {
            pf = dcf;
        }
        boolean parentFrameWasClippedByDisplayCutout2 = false;
        pfl = attrs10.layoutInDisplayCutoutMode;
        boolean z2 = (attached == null || layoutInScreen) ? z : true;
        boolean attachedInParent = z2;
        boolean requestedHideNavigation = (requestedSysUiFl & 2) != 0 ? true : z;
        z2 = (attrs10.isFullscreen() || !layoutInScreen || type2 == 1) ? z : true;
        topAtRest = z2;
        type.layoutWindowLwForNotch(windowState, attrs10);
        z2 = false;
        if (IS_NOTCH_PROP) {
            if (!type.mIsNotchSwitchOpen) {
                type.mLayoutBeyondDisplayCutout = canLayoutInDisplayCutout(win) ^ true;
            } else if (type.mDisplayRotation == 1 || type.mDisplayRotation == 3) {
                type.mLayoutBeyondDisplayCutout = win.toString().contains("com.qeexo.smartshot.CropActivity") ^ true;
            } else {
                z2 = type.mIsNoneNotchAppInHideMode;
                boolean z3 = (!type.mIsNoneNotchAppInHideMode && canLayoutInDisplayCutout(win)) ? z : true;
                type.mLayoutBeyondDisplayCutout = z3;
            }
            if (type.mLayoutBeyondDisplayCutout && !win.toString().contains("PointerLocation")) {
                z = true;
            }
            type.mLayoutBeyondDisplayCutout = z;
        }
        boolean isNoneNotchAppInHideMode = z2;
        if (type.mLayoutBeyondDisplayCutout || !(IS_NOTCH_PROP || pfl == 1)) {
            of2 = mTmpDisplayCutoutSafeExceptMaybeBarsRect;
            sim = type2;
            displayFrames2 = displayFrames;
            of2.set(displayFrames2.mDisplayCutoutSafe);
            if (layoutInScreen && layoutInsetDecor && !requestedFullscreen && pfl == 0 && !isNoneNotchAppInHideMode) {
                of2.top = Integer.MIN_VALUE;
            }
            if (layoutInScreen && layoutInsetDecor && !requestedHideNavigation && pfl == 0) {
                sysUiFl2 = type.mNavigationBarPosition;
                if (sysUiFl2 != 4) {
                    switch (sysUiFl2) {
                        case 1:
                            of2.left = Integer.MIN_VALUE;
                            break;
                        case 2:
                            of2.right = HwBootFail.STAGE_BOOT_SUCCESS;
                            break;
                    }
                }
                of2.bottom = HwBootFail.STAGE_BOOT_SUCCESS;
            }
            if (sim == 2011 && type.mNavigationBarPosition == 4) {
                of2.bottom = HwBootFail.STAGE_BOOT_SUCCESS;
            }
            if (!(attachedInParent || topAtRest)) {
                mTmpRect.set(sf2);
                sf2.intersectUnchecked(of2);
                parentFrameWasClippedByDisplayCutout2 = false | (mTmpRect.equals(sf2) ^ 1);
            }
            df = df6;
            df.intersectUnchecked(of2);
            parentFrameWasClippedByDisplayCutout = parentFrameWasClippedByDisplayCutout2;
        } else {
            parentFrameWasClippedByDisplayCutout = false;
            sim = type2;
            df = df6;
            displayFrames2 = displayFrames;
        }
        cf.intersectUnchecked(displayFrames2.mDisplayCutoutSafe);
        if (!HwPCUtils.isPcCastModeInServer() || !isPCDisplay) {
            pf2 = of6;
            min = fl6;
            if (!((min & 512) == 0 || sim == 2010 || (win.isInMultiWindowMode() && !win.toString().contains("com.huawei.android.launcher")))) {
                df.top = -10000;
                df.left = -10000;
                df.bottom = 10000;
                df.right = 10000;
                if (sim != 2013) {
                    vf.top = -10000;
                    vf.left = -10000;
                    cf.top = -10000;
                    cf.left = -10000;
                    pf2.top = -10000;
                    pf2.left = -10000;
                    vf.bottom = 10000;
                    vf.right = 10000;
                    cf.bottom = 10000;
                    cf.right = 10000;
                    pf2.bottom = 10000;
                    pf2.right = 10000;
                }
            }
        } else if (attrs10.type == 2005) {
            i = displayFrames2.mStable.left;
            cf.left = i;
            pf2 = of6;
            pf2.left = i;
            df.left = i;
            sf2.left = i;
            i = displayFrames2.mStable.top;
            cf.top = i;
            pf2.top = i;
            df.top = i;
            sf2.top = i;
            i = displayFrames2.mStable.right;
            cf.right = i;
            pf2.right = i;
            df.right = i;
            sf2.right = i;
            i = displayFrames2.mStable.bottom;
            cf.bottom = i;
            pf2.bottom = i;
            df.bottom = i;
            sf2.bottom = i;
            min = fl6;
        } else {
            pf2 = of6;
            min = fl6;
        }
        hasNavBar = type.shouldUseOutsets(attrs10, min);
        int fl7;
        if (isDefaultDisplay && hasNavBar) {
            of2 = mTmpOutsetFrame;
            fl7 = min;
            type4 = sim;
            of2.set(cf.left, cf.top, cf.right, cf.bottom);
            hasNavBar = ScreenShapeHelper.getWindowOutsetBottomPx(type.mContext.getResources());
            if (hasNavBar <= false) {
                min = displayFrames2.mRotation;
                if (min == 0) {
                    of2.bottom += hasNavBar;
                } else if (min == 1) {
                    of2.right += hasNavBar;
                } else if (min == 2) {
                    of2.top -= hasNavBar;
                } else if (min == 3) {
                    of2.left -= hasNavBar;
                }
            }
            osf = of2;
        } else {
            fl7 = min;
            type4 = sim;
        }
        if (win.toString().contains("DividerMenusView")) {
            sf2.top = displayFrames2.mUnrestricted.top;
            sf2.bottom = displayFrames2.mUnrestricted.bottom;
        }
        if (!(HwPCUtils.isPcCastModeInServer() && isPCDisplay) && win.toString().contains("com.android.packageinstaller.permission.ui.GrantPermissionsActivity")) {
            i = displayFrames2.mUnrestricted.top + type.mStatusBarHeightForRotation[displayFrames2.mRotation];
            vf.top = i;
            cf.top = i;
            pf2.top = i;
            df.top = i;
            sf2.top = i;
        }
        WmDisplayCutout wmDisplayCutout = displayFrames2.mDisplayCutout;
        of = pf2;
        type3 = 2011;
        type2 = type4;
        windowState.computeFrameLw(sf2, df, of, cf, vf, pf, cf2, osf, wmDisplayCutout, parentFrameWasClippedByDisplayCutout);
        if (type2 == type3 && win.isVisibleLw() && !win.getGivenInsetsPendingLw()) {
            phoneWindowManager = this;
            phoneWindowManager.setLastInputMethodWindowLw(null, null);
            sysUiFl = type2;
            displayFrames3 = displayFrames;
            phoneWindowManager.offsetInputMethodWindowLw(windowState, displayFrames3);
        } else {
            sysUiFl = type2;
            phoneWindowManager = this;
            displayFrames3 = displayFrames;
        }
        if (sysUiFl == 2031 && win.isVisibleLw() && !win.getGivenInsetsPendingLw()) {
            phoneWindowManager.offsetVoiceInputWindowLw(windowState, displayFrames3);
        }
    }

    private boolean isAboveFullScreen(WindowState win, WindowState fullScreenWin) {
        boolean z = false;
        if (!win.isInAboveAppWindows()) {
            return false;
        }
        if (fullScreenWin.getLayer() < win.getLayer()) {
            z = true;
        }
        return z;
    }

    private boolean isBaiDuOrSwiftkey(WindowState win) {
        String packageName = win.getAttrs().packageName;
        if (packageName == null || !packageName.contains("com.baidu.input_huawei")) {
            return false;
        }
        return true;
    }

    public void setInputMethodTargetWindow(WindowState target) {
        this.mInputMethodTarget = target;
    }

    public void setFocusChangeIMEFrozenTag(boolean frozen) {
        this.mFrozen = frozen;
    }

    private boolean isLandScapeMultiWindowMode() {
        int dockSide = -1;
        try {
            dockSide = this.mWindowManager.getDockedStackSide();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get dock side: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
        }
        boolean isLandscape = this.mDisplayRotation == 1 || this.mDisplayRotation == 3;
        if (!isLandscape || dockSide == -1) {
            return false;
        }
        return true;
    }

    private void layoutWallpaper(DisplayFrames displayFrames, Rect pf, Rect df, Rect of, Rect cf) {
        df.set(displayFrames.mOverscan);
        pf.set(displayFrames.mOverscan);
        cf.set(displayFrames.mUnrestricted);
        of.set(displayFrames.mUnrestricted);
    }

    private void offsetInputMethodWindowLw(WindowState win, DisplayFrames displayFrames) {
        int top = Math.max(win.getDisplayFrameLw().top, win.getContentFrameLw().top) + win.getGivenContentInsetsLw().top;
        adjustInsetSurfaceState(win, displayFrames, top);
        displayFrames.mContent.bottom = Math.min(displayFrames.mContent.bottom, top);
        displayFrames.mVoiceContent.bottom = Math.min(displayFrames.mVoiceContent.bottom, top);
        top = win.getVisibleFrameLw().top + win.getGivenVisibleInsetsLw().top;
        displayFrames.mCurrent.bottom = Math.min(displayFrames.mCurrent.bottom, top);
    }

    private void offsetVoiceInputWindowLw(WindowState win, DisplayFrames displayFrames) {
        int top = Math.max(win.getDisplayFrameLw().top, win.getContentFrameLw().top) + win.getGivenContentInsetsLw().top;
        displayFrames.mVoiceContent.bottom = Math.min(displayFrames.mVoiceContent.bottom, top);
    }

    public void beginPostLayoutPolicyLw(int displayWidth, int displayHeight) {
        this.mTopFullscreenOpaqueWindowState = null;
        this.mTopFullscreenOpaqueOrDimmingWindowState = null;
        this.mTopDockedOpaqueWindowState = null;
        this.mTopDockedOpaqueOrDimmingWindowState = null;
        this.mForceStatusBarTransparentWin = null;
        this.mForceStatusBar = false;
        this.mForceStatusBarFromKeyguard = false;
        this.mForceStatusBarTransparent = false;
        this.mForcingShowNavBar = false;
        this.mForcingShowNavBarLayer = -1;
        this.mAllowLockscreenWhenOn = false;
        this.mShowingDream = false;
        this.mWindowSleepTokenNeeded = false;
        this.mHasCoverView = false;
    }

    public void applyPostLayoutPolicyLw(WindowState win, LayoutParams attrs, WindowState attached, WindowState imeTarget) {
        boolean affectsSystemUi = win.canAffectSystemUiFlags();
        applyKeyguardPolicyLw(win, imeTarget);
        int fl = PolicyControl.getWindowFlags(win, attrs);
        if (this.mTopFullscreenOpaqueWindowState == null && affectsSystemUi && attrs.type == 2011) {
            this.mForcingShowNavBar = true;
            this.mForcingShowNavBarLayer = win.getSurfaceLayer();
        }
        if (attrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            if ((attrs.privateFlags & 1024) != 0) {
                this.mForceStatusBarFromKeyguard = true;
            }
            if ((attrs.privateFlags & 4096) != 0) {
                this.mForceStatusBarTransparent = true;
                this.mForceStatusBarTransparentWin = win;
            }
        }
        boolean inFullScreenOrSplitScreenSecondaryWindowingMode = false;
        boolean appWindow = (attrs.type >= 1 && attrs.type < IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) || attrs.type == 2100 || attrs.type == 2101;
        int windowingMode = win.getWindowingMode();
        if (windowingMode == 1 || windowingMode == 4) {
            inFullScreenOrSplitScreenSecondaryWindowingMode = true;
        }
        if (this.mTopFullscreenOpaqueWindowState == null && (attrs.type == 2100 || attrs.type == 2101)) {
            this.mHasCoverView = sProximityWndName.equals(attrs.getTitle()) ^ true;
        } else if (this.mTopFullscreenOpaqueWindowState == null && affectsSystemUi) {
            if ((fl & 2048) != 0) {
                this.mForceStatusBar = true;
            }
            if ((attrs.type == 2023 || attrs.type == 2102) && (!this.mDreamingLockscreen || (win.isVisibleLw() && win.hasDrawnLw()))) {
                this.mShowingDream = true;
                appWindow = true;
            }
            if (appWindow && attached == null && attrs.isFullscreen() && inFullScreenOrSplitScreenSecondaryWindowingMode) {
                if (!this.mHasCoverView || isCoverWindow(win)) {
                    this.mTopFullscreenOpaqueWindowState = win;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("skip show window when cover added. Target: ");
                    stringBuilder.append(win);
                    Slog.i(str, stringBuilder.toString());
                }
                if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                    this.mTopFullscreenOpaqueOrDimmingWindowState = win;
                }
                if ((fl & 1) != 0) {
                    this.mAllowLockscreenWhenOn = true;
                }
            }
        }
        if (affectsSystemUi && win.getAttrs().type == 2031) {
            if (this.mTopFullscreenOpaqueWindowState == null) {
                this.mTopFullscreenOpaqueWindowState = win;
                if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                    this.mTopFullscreenOpaqueOrDimmingWindowState = win;
                }
            }
            if (this.mTopDockedOpaqueWindowState == null) {
                this.mTopDockedOpaqueWindowState = win;
                if (this.mTopDockedOpaqueOrDimmingWindowState == null) {
                    this.mTopDockedOpaqueOrDimmingWindowState = win;
                }
            }
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState == null && affectsSystemUi && win.isDimming() && inFullScreenOrSplitScreenSecondaryWindowingMode) {
            this.mTopFullscreenOpaqueOrDimmingWindowState = win;
        }
        if (this.mTopDockedOpaqueWindowState == null && affectsSystemUi && appWindow && attached == null && attrs.isFullscreen() && windowingMode == 3) {
            this.mTopDockedOpaqueWindowState = win;
            if (this.mTopDockedOpaqueOrDimmingWindowState == null) {
                this.mTopDockedOpaqueOrDimmingWindowState = win;
            }
        }
        if (this.mTopDockedOpaqueOrDimmingWindowState == null && affectsSystemUi && win.isDimming() && windowingMode == 3) {
            this.mTopDockedOpaqueOrDimmingWindowState = win;
        }
        if (win.isVisibleLw() && (attrs.privateFlags & DumpState.DUMP_COMPILER_STATS) != 0 && win.canAcquireSleepToken()) {
            this.mWindowSleepTokenNeeded = true;
        }
    }

    private void applyKeyguardPolicyLw(WindowState win, WindowState imeTarget) {
        if (!canBeHiddenByKeyguardLw(win)) {
            return;
        }
        if (shouldBeHiddenByKeyguard(win, imeTarget)) {
            win.hideLw(false);
        } else {
            win.showLw(false);
        }
    }

    boolean isCoverWindow(WindowState win) {
        LayoutParams attrs = win == null ? null : win.getAttrs();
        int type = attrs == null ? 0 : attrs.type;
        if (type == 2100 || type == 2101) {
            return true;
        }
        return false;
    }

    private boolean isFullscreen(LayoutParams attrs) {
        return attrs.x == 0 && attrs.y == 0 && attrs.width == -1 && attrs.height == -1;
    }

    public int finishPostLayoutPolicyLw() {
        int changes = 0;
        boolean topIsFullscreen = false;
        if (this.mTopFullscreenOpaqueWindowState != null) {
            LayoutParams attrs = this.mTopFullscreenOpaqueWindowState.getAttrs();
        }
        if (!this.mShowingDream) {
            this.mDreamingLockscreen = isKeyguardShowingAndNotOccluded();
            if (this.mDreamingSleepTokenNeeded) {
                this.mDreamingSleepTokenNeeded = false;
                this.mHandler.obtainMessage(15, 0, 1).sendToTarget();
            }
        } else if (!this.mDreamingSleepTokenNeeded) {
            this.mDreamingSleepTokenNeeded = true;
            this.mHandler.obtainMessage(15, 1, 1).sendToTarget();
        }
        if (this.mStatusBar != null) {
            boolean shouldBeTransparent = (!this.mForceStatusBarTransparent || this.mForceStatusBar || this.mForceStatusBarFromKeyguard) ? false : true;
            if (!shouldBeTransparent) {
                this.mStatusBarController.setShowTransparent(false);
            } else if (!this.mStatusBar.isVisibleLw()) {
                this.mStatusBarController.setShowTransparent(true);
            }
            LayoutParams statusBarAttrs = this.mStatusBar.getAttrs();
            boolean statusBarExpanded = statusBarAttrs.height == -1 && statusBarAttrs.width == -1;
            boolean topAppHidesStatusBar = topAppHidesStatusBar();
            if (this.mForceStatusBar || this.mForceStatusBarFromKeyguard || this.mForceStatusBarTransparent || ((this.mForceStatusBarTransparent && this.mForceStatusBarTransparentWin != null && this.mForceStatusBarTransparentWin.isVisibleLw()) || statusBarExpanded)) {
                if (this.mStatusBarController.setBarShowingLw(true)) {
                    changes = 0 | 1;
                }
                boolean z = this.mTopIsFullscreen && this.mStatusBar.isAnimatingLw();
                topIsFullscreen = z;
                if ((this.mForceStatusBarFromKeyguard || statusBarExpanded) && this.mStatusBarController.isTransientShowing()) {
                    this.mStatusBarController.updateVisibilityLw(false, this.mLastSystemUiFlags, this.mLastSystemUiFlags);
                }
                if (statusBarExpanded && this.mNavigationBar != null && !computeNaviBarFlag() && this.mNavigationBarController.setBarShowingLw(true)) {
                    changes |= 1;
                }
            } else if (this.mTopFullscreenOpaqueWindowState != null) {
                topIsFullscreen = topAppHidesStatusBar;
                if (IS_NOTCH_PROP && this.mDisplayRotation == 0) {
                    int notchStatusBarColorStatus = this.mForceNotchStatusBar;
                    this.notchWindowChangeState = this.notchStatusBarColorLw != notchStatusBarColorStatus;
                    if (this.notchWindowChangeState) {
                        this.notchStatusBarColorLw = notchStatusBarColorStatus;
                        notchStatusBarColorUpdate(notchStatusBarColorStatus);
                        this.notchWindowChange = true;
                    } else if (!(this.mFocusedWindow == null || this.mTopFullscreenOpaqueWindowState.toString().equals(this.mFocusedWindow.toString()) || this.notchWindowChangeState || !this.mForceNotchStatusBar || !this.notchWindowChange)) {
                        notchStatusBarColorUpdate(notchStatusBarColorStatus);
                        this.notchWindowChange = false;
                    }
                }
                if (this.mStatusBarController.isTransientShowing()) {
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 0 | 1;
                    }
                } else if (!topIsFullscreen || this.mWindowManagerInternal.isStackVisible(5) || this.mWindowManagerInternal.isStackVisible(3)) {
                    if (this.mStatusBarController.isTransientHiding()) {
                        Slog.v(TAG, "not fullscreen but transientBarState is hiding, so reset");
                        this.mStatusBarController.sethwTransientBarState(0);
                    }
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 0 | 1;
                    }
                    topAppHidesStatusBar = false;
                } else if (this.mStatusBarController.setBarShowingLw(false)) {
                    changes = 0 | 1;
                }
            }
            this.mStatusBarController.setTopAppHidesStatusBar(topAppHidesStatusBar);
        }
        if (this.mTopIsFullscreen != topIsFullscreen) {
            if (!topIsFullscreen) {
                changes |= 1;
            }
            this.mTopIsFullscreen = topIsFullscreen;
        }
        if ((updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            changes |= 1;
        }
        if (this.mShowingDream != this.mLastShowingDream) {
            this.mLastShowingDream = this.mShowingDream;
            this.mWindowManagerFuncs.notifyShowingDreamChanged();
        }
        updateWindowSleepToken();
        updateLockScreenTimeout();
        return changes;
    }

    private void updateWindowSleepToken() {
        if (this.mWindowSleepTokenNeeded && !this.mLastWindowSleepTokenNeeded) {
            this.mHandler.removeCallbacks(this.mReleaseSleepTokenRunnable);
            this.mHandler.post(this.mAcquireSleepTokenRunnable);
        } else if (!this.mWindowSleepTokenNeeded && this.mLastWindowSleepTokenNeeded) {
            this.mHandler.removeCallbacks(this.mAcquireSleepTokenRunnable);
            this.mHandler.post(this.mReleaseSleepTokenRunnable);
        }
        this.mLastWindowSleepTokenNeeded = this.mWindowSleepTokenNeeded;
    }

    private boolean topAppHidesStatusBar() {
        boolean z = false;
        if (this.mTopFullscreenOpaqueWindowState == null) {
            return false;
        }
        int fl = PolicyControl.getWindowFlags(0, this.mTopFullscreenOpaqueWindowState.getAttrs());
        if (IS_NOTCH_PROP) {
            if (!hideNotchStatusBar(fl)) {
                return false;
            }
            this.mForceNotchStatusBar = false;
        }
        if (!((fl & 1024) == 0 && (this.mLastSystemUiFlags & 4) == 0)) {
            z = true;
        }
        return z;
    }

    protected boolean setKeyguardOccludedLw(boolean isOccluded, boolean force) {
        boolean wasOccluded = this.mKeyguardOccluded;
        boolean showing = this.mKeyguardDelegate.isShowing();
        boolean changed = wasOccluded != isOccluded || force;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setKeyguardOccluded occluded=");
        stringBuilder.append(isOccluded);
        stringBuilder.append(" showing=");
        stringBuilder.append(showing);
        stringBuilder.append(" changed=");
        stringBuilder.append(changed);
        Slog.d(str, stringBuilder.toString());
        LayoutParams attrs;
        if (!isOccluded && changed && showing) {
            this.mKeyguardOccluded = false;
            this.mKeyguardDelegate.setOccluded(false, true);
            if (this.mStatusBar != null) {
                attrs = this.mStatusBar.getAttrs();
                attrs.privateFlags |= 1024;
                if (!this.mKeyguardDelegate.hasLockscreenWallpaper()) {
                    attrs = this.mStatusBar.getAttrs();
                    attrs.flags |= DumpState.DUMP_DEXOPT;
                }
            }
            return true;
        } else if (isOccluded && changed && showing) {
            this.mKeyguardOccluded = true;
            this.mKeyguardDelegate.setOccluded(true, false);
            if (this.mStatusBar != null) {
                attrs = this.mStatusBar.getAttrs();
                attrs.privateFlags &= -1025;
                attrs = this.mStatusBar.getAttrs();
                attrs.flags &= -1048577;
            }
            return true;
        } else if (!changed) {
            return false;
        } else {
            this.mKeyguardOccluded = isOccluded;
            this.mKeyguardDelegate.setOccluded(isOccluded, false);
            return false;
        }
    }

    private boolean isStatusBarKeyguard() {
        return (this.mStatusBar == null || (this.mStatusBar.getAttrs().privateFlags & 1024) == 0) ? false : true;
    }

    public boolean allowAppAnimationsLw() {
        return this.mShowingDream ^ 1;
    }

    public int focusChangedLw(WindowState lastFocus, WindowState newFocus) {
        this.mFocusedWindow = newFocus;
        if (this.mFocusedWindow != null) {
            if (IS_NOTCH_PROP && this.mFocusedWindow.toString().contains("com.huawei.android.launcher")) {
                this.mForceNotchStatusBar = false;
            }
            updateSystemUiColorLw(this.mFocusedWindow);
            if (this.mLastStartingWindow != null && this.mLastStartingWindow.isVisibleLw() && this.mFocusedWindow.getAttrs() != null && this.mFocusedWindow.getAttrs().type == 2003) {
                updateSystemUiColorLw(this.mLastStartingWindow);
            }
        }
        if ((updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            return 1;
        }
        return 0;
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        boolean newLidState = lidOpen;
        if (newLidState != this.mLidState) {
            this.mLidState = newLidState;
            applyLidSwitchState();
            updateRotation(true);
            if (lidOpen) {
                wakeUp(SystemClock.uptimeMillis(), this.mAllowTheaterModeWakeFromLidSwitch, "android.policy:LID");
            } else if (!this.mLidControlsSleep) {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
            }
        }
    }

    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        boolean lensCoverState = lensCovered;
        if (this.mCameraLensCoverState != lensCoverState) {
            if (this.mCameraLensCoverState == 1 && !lensCoverState) {
                boolean keyguardActive;
                Intent intent;
                if (this.mKeyguardDelegate == null) {
                    keyguardActive = false;
                } else {
                    keyguardActive = this.mKeyguardDelegate.isShowing();
                }
                if (keyguardActive) {
                    intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
                } else {
                    intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
                }
                wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromCameraLens, "android.policy:CAMERA_COVER");
                startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
            }
            this.mCameraLensCoverState = lensCoverState;
        }
    }

    void setHdmiPlugged(boolean plugged) {
        if (this.mHdmiPlugged != plugged) {
            this.mHdmiPlugged = plugged;
            updateRotation(true, true);
            Intent intent = new Intent("android.intent.action.HDMI_PLUGGED");
            intent.addFlags(67108864);
            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, plugged);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    void initializeHdmiState() {
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        try {
            initializeHdmiStateInternal();
        } finally {
            StrictMode.setThreadPolicyMask(oldMask);
        }
    }

    void initializeHdmiStateInternal() {
        String str;
        StringBuilder stringBuilder;
        boolean plugged = false;
        boolean z = false;
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            this.mHDMIObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi");
            String filename = "/sys/class/switch/hdmi/state";
            FileReader reader = null;
            try {
                reader = new FileReader("/sys/class/switch/hdmi/state");
                char[] buf = new char[15];
                int n = reader.read(buf);
                if (n > 1) {
                    plugged = Integer.parseInt(new String(buf, 0, n + -1)) != 0;
                }
                try {
                    reader.close();
                } catch (IOException e) {
                }
            } catch (IOException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't read hdmi state from /sys/class/switch/hdmi/state: ");
                stringBuilder.append(ex);
                Slog.w(str, stringBuilder.toString());
                if (reader != null) {
                    reader.close();
                }
            } catch (NumberFormatException ex2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't read hdmi state from /sys/class/switch/hdmi/state: ");
                stringBuilder.append(ex2);
                Slog.w(str, stringBuilder.toString());
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2) {
                    }
                }
            }
        }
        if (!plugged) {
            z = true;
        }
        this.mHdmiPlugged = z;
        setHdmiPlugged(this.mHdmiPlugged ^ true);
    }

    /* JADX WARNING: Removed duplicated region for block: B:248:0x043a  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0441  */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x043a  */
    /* JADX WARNING: Removed duplicated region for block: B:250:0x0441  */
    /* JADX WARNING: Missing block: B:87:0x0149, code:
            r17 = r10;
     */
    /* JADX WARNING: Missing block: B:96:0x016a, code:
            r17 = r10;
            r19 = r11;
            r10 = false;
     */
    /* JADX WARNING: Missing block: B:119:0x01de, code:
            r17 = r10;
     */
    /* JADX WARNING: Missing block: B:120:0x01e2, code:
            r17 = r10;
     */
    /* JADX WARNING: Missing block: B:132:0x021f, code:
            if (android.media.session.MediaSessionLegacyHelper.getHelper(r1.mContext).isGlobalPriorityActive() == false) goto L_0x0223;
     */
    /* JADX WARNING: Missing block: B:133:0x0221, code:
            r13 = r13 & -2;
     */
    /* JADX WARNING: Missing block: B:135:0x0225, code:
            if ((r13 & 1) != 0) goto L_0x01f3;
     */
    /* JADX WARNING: Missing block: B:136:0x0227, code:
            r1.mBroadcastWakeLock.acquire();
            r0 = r1.mHandler.obtainMessage(3, new android.view.KeyEvent(r2));
            r0.setAsynchronous(true);
            r0.sendToTarget();
     */
    /* JADX WARNING: Missing block: B:142:0x0253, code:
            if (r9 != 25) goto L_0x0288;
     */
    /* JADX WARNING: Missing block: B:143:0x0255, code:
            if (r7 == false) goto L_0x027e;
     */
    /* JADX WARNING: Missing block: B:144:0x0257, code:
            cancelPendingRingerToggleChordAction();
     */
    /* JADX WARNING: Missing block: B:145:0x025a, code:
            if (r6 == false) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:147:0x025e, code:
            if (r1.mScreenshotChordVolumeDownKeyTriggered != false) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:149:0x0266, code:
            if ((r21.getFlags() & 1024) != 0) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:150:0x0268, code:
            r1.mScreenshotChordVolumeDownKeyTriggered = true;
            r1.mScreenshotChordVolumeDownKeyTime = r21.getDownTime();
            r1.mScreenshotChordVolumeDownKeyConsumed = false;
            cancelPendingPowerKeyAction();
            interceptScreenshotChord();
            interceptAccessibilityShortcutChord();
     */
    /* JADX WARNING: Missing block: B:151:0x027e, code:
            r1.mScreenshotChordVolumeDownKeyTriggered = false;
            cancelPendingScreenshotChordAction();
            cancelPendingAccessibilityShortcutAction();
     */
    /* JADX WARNING: Missing block: B:153:0x028a, code:
            if (r9 != 24) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:154:0x028c, code:
            if (r7 == false) goto L_0x02b8;
     */
    /* JADX WARNING: Missing block: B:155:0x028e, code:
            if (r6 == false) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:157:0x0292, code:
            if (r1.mA11yShortcutChordVolumeUpKeyTriggered != false) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:159:0x029a, code:
            if ((r21.getFlags() & 1024) != 0) goto L_0x02c4;
     */
    /* JADX WARNING: Missing block: B:160:0x029c, code:
            r1.mA11yShortcutChordVolumeUpKeyTriggered = true;
            r1.mA11yShortcutChordVolumeUpKeyTime = r21.getDownTime();
            r1.mA11yShortcutChordVolumeUpKeyConsumed = false;
            cancelPendingPowerKeyAction();
            cancelPendingScreenshotChordAction();
            cancelPendingRingerToggleChordAction();
            interceptAccessibilityShortcutChord();
            interceptRingerToggleChord();
     */
    /* JADX WARNING: Missing block: B:161:0x02b8, code:
            r1.mA11yShortcutChordVolumeUpKeyTriggered = false;
            cancelPendingScreenshotChordAction();
            cancelPendingAccessibilityShortcutAction();
            cancelPendingRingerToggleChordAction();
     */
    /* JADX WARNING: Missing block: B:162:0x02c4, code:
            if (r7 == false) goto L_0x0326;
     */
    /* JADX WARNING: Missing block: B:163:0x02c6, code:
            sendSystemKeyToStatusBarAsync(r21.getKeyCode());
            r4 = getTelecommService();
     */
    /* JADX WARNING: Missing block: B:164:0x02d1, code:
            if (r4 == null) goto L_0x02eb;
     */
    /* JADX WARNING: Missing block: B:166:0x02d5, code:
            if (r1.mHandleVolumeKeysInWM != false) goto L_0x02eb;
     */
    /* JADX WARNING: Missing block: B:168:0x02db, code:
            if (r4.isRinging() == false) goto L_0x02eb;
     */
    /* JADX WARNING: Missing block: B:169:0x02dd, code:
            android.util.Log.i(TAG, "interceptKeyBeforeQueueing: VOLUME key-down while ringing: Silence ringer!");
            r4.silenceRinger();
            r13 = r13 & -2;
     */
    /* JADX WARNING: Missing block: B:170:0x02eb, code:
            r16 = 0;
     */
    /* JADX WARNING: Missing block: B:172:?, code:
            r0 = getAudioService().getMode();
     */
    /* JADX WARNING: Missing block: B:173:0x02f6, code:
            r16 = r0;
     */
    /* JADX WARNING: Missing block: B:174:0x02f9, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:175:0x02fa, code:
            android.util.Log.e(TAG, "Error getting AudioService in interceptKeyBeforeQueueing.", r0);
            r0 = r16;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        int keyCode;
        boolean isInjected;
        String str;
        int result;
        boolean isWakeKey;
        boolean useHapticFeedback;
        boolean hungUp;
        KeyEvent keyEvent = event;
        if (this.mSystemBooted) {
            boolean interactive = (policyFlags & 536870912) != 0;
            boolean down = event.getAction() == 0;
            boolean canceled = event.isCanceled();
            keyCode = event.getKeyCode();
            isInjected = (policyFlags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
            boolean isKeyguardShowingAndNotOccluded = this.mKeyguardDelegate == null ? false : interactive ? isKeyguardShowingAndNotOccluded() : this.mKeyguardDelegate.isShowing();
            boolean keyguardActive = isKeyguardShowingAndNotOccluded;
            if (HWFLOW) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("interceptKeyTq keycode=");
                stringBuilder.append(keyCode);
                stringBuilder.append(" interactive=");
                stringBuilder.append(interactive);
                stringBuilder.append(" keyguardActive=");
                stringBuilder.append(keyguardActive);
                stringBuilder.append(" policyFlags=");
                stringBuilder.append(Integer.toHexString(policyFlags));
                stringBuilder.append(" down ");
                stringBuilder.append(down);
                stringBuilder.append(" canceled ");
                stringBuilder.append(canceled);
                Log.d(str, stringBuilder.toString());
            }
            isKeyguardShowingAndNotOccluded = (policyFlags & 1) != 0 || event.isWakeKey();
            if (interactive || (isInjected && !isKeyguardShowingAndNotOccluded)) {
                result = 1;
                isKeyguardShowingAndNotOccluded = false;
                if (interactive) {
                    if (keyCode == this.mPendingWakeKey && !down) {
                        result = 0;
                    }
                    this.mPendingWakeKey = -1;
                }
            } else if (interactive || !shouldDispatchInputWhenNonInteractive(event)) {
                result = 0;
                if (isKeyguardShowingAndNotOccluded && !(down && isWakeKeyWhenScreenOff(keyCode))) {
                    isKeyguardShowingAndNotOccluded = false;
                }
                if (isKeyguardShowingAndNotOccluded && down) {
                    this.mPendingWakeKey = keyCode;
                }
            } else {
                result = 1;
                this.mPendingWakeKey = -1;
            }
            isWakeKey = isKeyguardShowingAndNotOccluded;
            if (!isValidGlobalKey(keyCode) || !this.mGlobalKeyManager.shouldHandleGlobalKey(keyCode, keyEvent)) {
                isKeyguardShowingAndNotOccluded = down && (policyFlags & 2) != 0 && ((!((event.getFlags() & 64) != 0) || this.mNavBarVirtualKeyHapticFeedbackEnabled) && event.getRepeatCount() == 0);
                useHapticFeedback = isKeyguardShowingAndNotOccluded;
                boolean z;
                TelecomManager telecomManager;
                switch (keyCode) {
                    case 4:
                        z = keyguardActive;
                        if (!down) {
                            Jlog.d(381, "JLID_BACK_KEY_PRESS");
                            if (interceptBackKeyUp(event)) {
                                result &= -2;
                            }
                            boolean isMiniDock = this.mWindowManagerInternal.isMinimizedDock();
                            WindowState imeWin = this.mWindowManagerFuncs.getInputMethodWindowLw();
                            if (canceled || !isMiniDock || ((imeWin == null || imeWin.isVisibleLw()) && imeWin != null)) {
                                if (isEqualBottomApp()) {
                                    isInjected = false;
                                    hideRecentApps(false, false);
                                    break;
                                }
                            }
                            try {
                                ActivityManager.getService().dismissSplitScreenMode(false);
                                uploadKeyEvent(4);
                            } catch (Exception e) {
                                Slog.i(TAG, "back press error!");
                            }
                            isInjected = false;
                            break;
                        }
                        interceptBackKeyDown();
                        break;
                    case 5:
                        z = keyguardActive;
                        if (down) {
                            telecomManager = getTelecommService();
                            if (telecomManager != null && telecomManager.isRinging()) {
                                Log.i(TAG, "interceptKeyBeforeQueueing: CALL key-down while ringing: Answer the call!");
                                telecomManager.acceptRingingCall();
                                result &= -2;
                                break;
                            }
                        }
                        isInjected = false;
                        break;
                    case 6:
                        result &= -2;
                        if (!down) {
                            if (!this.mEndCallKeyHandled) {
                                this.mHandler.removeCallbacks(this.mEndCallLongPress);
                                if (!canceled && (((this.mEndcallBehavior & 1) == 0 || !goHome()) && (this.mEndcallBehavior & 2) != 0)) {
                                    goToSleep(event.getEventTime(), 4, 0);
                                    isWakeKey = false;
                                    break;
                                }
                            }
                        }
                        telecomManager = getTelecommService();
                        hungUp = false;
                        if (telecomManager != null) {
                            hungUp = telecomManager.endCall();
                        }
                        if (interactive && !hungUp) {
                            this.mEndCallKeyHandled = false;
                            z = keyguardActive;
                            this.mHandler.postDelayed(this.mEndCallLongPress, ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout());
                            break;
                        }
                        this.mEndCallKeyHandled = true;
                        break;
                        break;
                    default:
                        switch (keyCode) {
                            case 24:
                            case 25:
                                break;
                            case 26:
                                cancelPendingAccessibilityShortcutAction();
                                result &= -2;
                                isWakeKey = false;
                                if (!down) {
                                    interceptPowerKeyUp(keyEvent, interactive, canceled);
                                    break;
                                }
                                interceptPowerKeyDown(keyEvent, interactive);
                                break;
                            default:
                                switch (keyCode) {
                                    case HdmiCecKeycode.CEC_KEYCODE_INITIAL_CONFIGURATION /*85*/:
                                    case HdmiCecKeycode.CEC_KEYCODE_SELECT_BROADCAST_TYPE /*86*/:
                                    case HdmiCecKeycode.CEC_KEYCODE_SELECT_SOUND_PRESENTATION /*87*/:
                                    case 88:
                                    case 89:
                                    case 90:
                                    case 91:
                                        break;
                                    default:
                                        switch (keyCode) {
                                            case 126:
                                            case 127:
                                                break;
                                            default:
                                                switch (keyCode) {
                                                    case NetdResponseCode.DnsProxyQueryResult /*222*/:
                                                        break;
                                                    case NetdResponseCode.ClatdStatusResult /*223*/:
                                                        result &= -2;
                                                        isWakeKey = false;
                                                        if (!this.mPowerManager.isInteractive()) {
                                                            useHapticFeedback = false;
                                                        }
                                                        if (!down) {
                                                            sleepRelease(event.getEventTime());
                                                            break;
                                                        }
                                                        sleepPress();
                                                        break;
                                                    case UsbDescriptor.CLASSID_WIRELESS /*224*/:
                                                        result &= -2;
                                                        isWakeKey = true;
                                                        break;
                                                    default:
                                                        switch (keyCode) {
                                                            case 280:
                                                            case 281:
                                                            case 282:
                                                            case 283:
                                                                result &= -2;
                                                                interceptSystemNavigationKey(event);
                                                                break;
                                                            default:
                                                                switch (keyCode) {
                                                                    case HdmiCecKeycode.CEC_KEYCODE_RESERVED /*79*/:
                                                                    case 130:
                                                                        break;
                                                                    case 164:
                                                                        break;
                                                                    case 171:
                                                                        if (this.mShortPressOnWindowBehavior == 1 && this.mPictureInPictureVisible) {
                                                                            if (!down) {
                                                                                showPictureInPictureMenu(event);
                                                                            }
                                                                            result &= -2;
                                                                            break;
                                                                        }
                                                                    case 219:
                                                                        isKeyguardShowingAndNotOccluded = event.getRepeatCount() > 0;
                                                                        if (down && isKeyguardShowingAndNotOccluded) {
                                                                            Message msg = this.mHandler.obtainMessage(MSG_LAUNCH_ASSIST_LONG_PRESS);
                                                                            msg.setAsynchronous(true);
                                                                            msg.sendToTarget();
                                                                        }
                                                                        boolean z2;
                                                                        if (down || isKeyguardShowingAndNotOccluded) {
                                                                            z2 = isInjected;
                                                                        } else {
                                                                            z2 = isInjected;
                                                                            isKeyguardShowingAndNotOccluded = this.mHandler.obtainMessage(26, event.getDeviceId(), 0, false);
                                                                            isKeyguardShowingAndNotOccluded.setAsynchronous(true);
                                                                            isKeyguardShowingAndNotOccluded.sendToTarget();
                                                                        }
                                                                        result &= -2;
                                                                        break;
                                                                    case 231:
                                                                        if (!down) {
                                                                            this.mBroadcastWakeLock.acquire();
                                                                            Message msg2 = this.mHandler.obtainMessage(12);
                                                                            msg2.setAsynchronous(true);
                                                                            msg2.sendToTarget();
                                                                        }
                                                                        result &= -2;
                                                                        break;
                                                                    case 276:
                                                                        result &= -2;
                                                                        isWakeKey = false;
                                                                        if (!down) {
                                                                            this.mPowerManagerInternal.setUserInactiveOverrideFromWindowManager();
                                                                            break;
                                                                        }
                                                                        break;
                                                                    case 701:
                                                                        if (down && !SystemProperties.getBoolean("sys.super_power_save", false)) {
                                                                            quickOpenCameraService("flip");
                                                                            break;
                                                                        }
                                                                }
                                                                break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
            }
            if (isWakeKey) {
                wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey, "android.policy:KEY");
            }
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("key: ");
            stringBuilder2.append(keyCode);
            stringBuilder2.append(" , handled globally, just return the result ");
            stringBuilder2.append(result);
            Log.d(str, stringBuilder2.toString());
            return result;
        }
        Log.d(TAG, "we have not yet booted, don't let key events do anything.");
        return 0;
        StringBuilder stringBuilder3;
        hungUp = (telecomManager != null && telecomManager.isInCall()) || audioMode == 3;
        if (hungUp && (result & 1) == 0) {
            MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(keyEvent, true, false);
            isInjected = false;
            if (useHapticFeedback) {
                performHapticFeedbackLw(null, 1, isInjected);
            }
            if (isWakeKey) {
                wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey, "android.policy:KEY");
            }
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("interceptKeyBeforeQueueing: key ");
            stringBuilder3.append(keyCode);
            stringBuilder3.append(" , result : ");
            stringBuilder3.append(result);
            Log.d(str, stringBuilder3.toString());
            return result;
        }
        str = TAG;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Volume key pass to user ");
        stringBuilder3.append((result & 1) != 0);
        Log.i(str, stringBuilder3.toString());
        if (this.mUseTvRouting || this.mHandleVolumeKeysInWM) {
            result |= 1;
            isInjected = false;
            if (useHapticFeedback) {
            }
            if (isWakeKey) {
            }
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("interceptKeyBeforeQueueing: key ");
            stringBuilder3.append(keyCode);
            stringBuilder3.append(" , result : ");
            stringBuilder3.append(result);
            Log.d(str, stringBuilder3.toString());
            return result;
        }
        if ((result & 1) == 0) {
            MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(keyEvent, Integer.MIN_VALUE, true);
        }
        isInjected = false;
        if (useHapticFeedback) {
        }
        if (isWakeKey) {
        }
        str = TAG;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("interceptKeyBeforeQueueing: key ");
        stringBuilder3.append(keyCode);
        stringBuilder3.append(" , result : ");
        stringBuilder3.append(result);
        Log.d(str, stringBuilder3.toString());
        return result;
    }

    public boolean isEqualBottomApp() {
        try {
            return FOCUSED_SPLIT_APP_ACTIVITY.equals(this.mWindowManager.getFocusedAppComponentName());
        } catch (RemoteException e) {
            Log.e(TAG, "Error get Focused App ComponentName.");
            return false;
        }
    }

    private void quickOpenCameraService(String command) {
        Intent intent = new Intent(HUAWEI_PRE_CAMERA_START_MODE);
        intent.setPackage("com.huawei.camera");
        intent.putExtra("command", command);
        this.mContext.startService(intent);
    }

    private void interceptSystemNavigationKey(KeyEvent event) {
        if (event.getAction() != 1) {
            return;
        }
        if (!(this.mAccessibilityManager.isEnabled() && this.mAccessibilityManager.sendFingerprintGesture(event.getKeyCode())) && this.mSystemNavigationKeysEnabled) {
            sendSystemKeyToStatusBarAsync(event.getKeyCode());
        }
    }

    private void sendSystemKeyToStatusBar(int keyCode) {
        IStatusBarService statusBar = getStatusBarService();
        if (statusBar != null) {
            try {
                statusBar.handleSystemKey(keyCode);
            } catch (RemoteException e) {
            }
        }
    }

    private void sendSystemKeyToStatusBarAsync(int keyCode) {
        Message message = this.mHandler.obtainMessage(24, keyCode, 0);
        message.setAsynchronous(true);
        this.mHandler.sendMessage(message);
    }

    private void sendProposedRotationChangeToStatusBarInternal(int rotation, boolean isValid) {
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            statusBar.onProposedRotationChanged(rotation, isValid);
        }
    }

    private static boolean isValidGlobalKey(int keyCode) {
        if (keyCode != 26) {
            switch (keyCode) {
                case NetdResponseCode.ClatdStatusResult /*223*/:
                case UsbDescriptor.CLASSID_WIRELESS /*224*/:
                    break;
                default:
                    return true;
            }
        }
        return false;
    }

    protected boolean isWakeKeyWhenScreenOff(int keyCode) {
        boolean z = false;
        if (!(keyCode == MSG_LAUNCH_ASSIST_LONG_PRESS || keyCode == 79 || keyCode == 130)) {
            if (keyCode != 164) {
                if (keyCode != NetdResponseCode.DnsProxyQueryResult) {
                    switch (keyCode) {
                        case 24:
                        case 25:
                            break;
                        default:
                            switch (keyCode) {
                                case HdmiCecKeycode.CEC_KEYCODE_INITIAL_CONFIGURATION /*85*/:
                                case HdmiCecKeycode.CEC_KEYCODE_SELECT_BROADCAST_TYPE /*86*/:
                                case HdmiCecKeycode.CEC_KEYCODE_SELECT_SOUND_PRESENTATION /*87*/:
                                case 88:
                                case 89:
                                case 90:
                                case 91:
                                    break;
                                default:
                                    switch (keyCode) {
                                        case 126:
                                        case 127:
                                            break;
                                        default:
                                            return true;
                                    }
                            }
                    }
                }
            }
            if (this.mDockMode != 0) {
                z = true;
            }
            return z;
        }
        return false;
    }

    public int interceptMotionBeforeQueueingNonInteractive(long whenNanos, int policyFlags) {
        if ((policyFlags & 1) != 0 && wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotion, "android.policy:MOTION")) {
            return 0;
        }
        if (shouldDispatchInputWhenNonInteractive(null) && !this.mInterceptInputForWaitBrightness) {
            return 1;
        }
        if (isTheaterModeEnabled() && (policyFlags & 1) != 0) {
            wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotionWhenNotDreaming, "android.policy:MOTION");
        }
        return 0;
    }

    private boolean shouldDispatchInputWhenNonInteractive(KeyEvent event) {
        boolean displayOff = this.mDisplay == null || this.mDisplay.getState() == 1;
        if (displayOff && !this.mHasFeatureWatch) {
            return false;
        }
        if (isKeyguardShowingAndNotOccluded() && !displayOff) {
            return true;
        }
        if (this.mHasFeatureWatch && event != null && (event.getKeyCode() == 4 || event.getKeyCode() == DhcpPacket.MIN_PACKET_LENGTH_L3)) {
            return false;
        }
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager != null) {
            try {
                if (dreamManager.isDreaming()) {
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException when checking if dreaming", e);
            }
        }
        return false;
    }

    private void dispatchDirectAudioEvent(KeyEvent event) {
        if (event.getAction() == 0) {
            int keyCode = event.getKeyCode();
            String pkgName = this.mContext.getOpPackageName();
            if (keyCode != 164) {
                switch (keyCode) {
                    case 24:
                        try {
                            getAudioService().adjustSuggestedStreamVolume(1, Integer.MIN_VALUE, 4101, pkgName, TAG);
                            break;
                        } catch (Exception e) {
                            Log.e(TAG, "Error dispatching volume up in dispatchTvAudioEvent.", e);
                            break;
                        }
                    case 25:
                        try {
                            getAudioService().adjustSuggestedStreamVolume(-1, Integer.MIN_VALUE, 4101, pkgName, TAG);
                            break;
                        } catch (Exception e2) {
                            Log.e(TAG, "Error dispatching volume down in dispatchTvAudioEvent.", e2);
                            break;
                        }
                }
            }
            try {
                if (event.getRepeatCount() == 0) {
                    getAudioService().adjustSuggestedStreamVolume(101, Integer.MIN_VALUE, 4101, pkgName, TAG);
                }
            } catch (Exception e22) {
                Log.e(TAG, "Error dispatching mute in dispatchTvAudioEvent.", e22);
            }
        }
    }

    void dispatchMediaKeyWithWakeLock(KeyEvent event) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dispatchMediaKeyWithWakeLock: action=");
        stringBuilder.append(event.getAction());
        stringBuilder.append(", keyCode=");
        stringBuilder.append(event.getKeyCode());
        stringBuilder.append(", flags=");
        stringBuilder.append(event.getFlags());
        stringBuilder.append(", repeatCount=");
        stringBuilder.append(event.getRepeatCount());
        Slog.d(str, stringBuilder.toString());
        if (this.mHavePendingMediaKeyRepeatWithWakeLock) {
            Slog.d(TAG, "dispatchMediaKeyWithWakeLock: canceled repeat");
            this.mHandler.removeMessages(4);
            this.mHavePendingMediaKeyRepeatWithWakeLock = false;
            this.mBroadcastWakeLock.release();
        }
        dispatchMediaKeyWithWakeLockToAudioService(event);
        if (event.getAction() == 0 && event.getRepeatCount() == 0) {
            this.mHavePendingMediaKeyRepeatWithWakeLock = true;
            Slog.d(TAG, "dispatchMediaKeyWithWakeLock: send repeat event");
            Message msg = this.mHandler.obtainMessage(4, event);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, (long) ViewConfiguration.getKeyRepeatTimeout());
            return;
        }
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent event) {
        this.mHavePendingMediaKeyRepeatWithWakeLock = false;
        KeyEvent repeatEvent = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(), 1, event.getFlags() | 128);
        Slog.d(TAG, "dispatchMediaKeyRepeatWithWakeLock: dispatch media key with long press");
        dispatchMediaKeyWithWakeLockToAudioService(repeatEvent);
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent event) {
        if (this.mActivityManagerInternal.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(this.mContext).sendMediaButtonEvent(event, true);
        }
    }

    void launchVoiceAssistWithWakeLock() {
        Intent voiceIntent;
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (keyguardOn()) {
            IDeviceIdleController dic = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            if (dic != null) {
                try {
                    dic.exitIdle("voice-search");
                } catch (RemoteException e) {
                }
            }
            Intent voiceIntent2 = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
            voiceIntent2.putExtra("android.speech.extras.EXTRA_SECURE", true);
            voiceIntent = voiceIntent2;
        } else {
            voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
        }
        startActivityAsUser(voiceIntent, UserHandle.CURRENT_OR_SELF);
        this.mBroadcastWakeLock.release();
    }

    /* JADX WARNING: Missing block: B:26:0x004b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void requestTransientBars(WindowState swipeTarget) {
        synchronized (this.mWindowManagerFuncs.getWindowManagerLock()) {
            if (isUserSetupComplete()) {
                boolean sb = this.mStatusBarController.checkShowTransientBarLw();
                boolean nb = this.mNavigationBarController.checkShowTransientBarLw() && !isNavBarEmpty(this.mLastSystemUiFlags);
                if (sb || nb) {
                    if (nb || swipeTarget != this.mNavigationBar) {
                        if (sb) {
                            this.mStatusBarController.showTransient();
                        }
                        if (nb) {
                            this.mNavigationBarController.showTransient();
                        }
                        this.mImmersiveModeConfirmation.confirmCurrentPrompt();
                        updateSystemUiVisibilityLw();
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void startedGoingToSleep(int why) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power Started going to sleep... (why=");
        stringBuilder.append(why);
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
        if (mSupportAod && !this.mIsActuralShutDown) {
            startAodService(7);
        }
        this.mGoingToSleep = true;
        this.mRequestedOrGoingToSleep = true;
        if (this.mCurrentUserId == 0) {
            Secure.putInt(this.mContext.getContentResolver(), "lock_screen_state", 0);
        }
        if (this.mKeyguardDelegate != null && needTurnOff(why) && needTurnOffWithDismissFlag(this.mTopFullscreenOpaqueWindowState)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("call onScreenTurnedOff(");
            stringBuilder.append(why);
            stringBuilder.append(")");
            Flog.i(305, stringBuilder.toString());
            this.mKeyguardDelegate.onStartedGoingToSleep(why);
        }
    }

    public void finishedGoingToSleep(int why) {
        EventLog.writeEvent(70000, 0);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power Finished going to sleep... (why=");
        stringBuilder.append(why);
        stringBuilder.append(")");
        Flog.i(NativeResponseCode.SERVICE_LOST, stringBuilder.toString());
        MetricsLogger.histogram(this.mContext, "screen_timeout", this.mLockScreenTimeout / 1000);
        this.mGoingToSleep = false;
        this.mRequestedOrGoingToSleep = false;
        synchronized (this.mLock) {
            this.mAwake = false;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onFinishedGoingToSleep(why, this.mCameraGestureTriggeredDuringGoingToSleep);
        }
        this.mCameraGestureTriggeredDuringGoingToSleep = false;
    }

    public void startedWakingUp() {
        EventLog.writeEvent(70000, 1);
        Slog.i(TAG, "UL_Power Started waking up...");
        if (mSupportAod) {
            if (this.mPickUpFlag) {
                this.mPickUpFlag = false;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startedWakingUp mPickUpFlag:");
                stringBuilder.append(this.mPickUpFlag);
                Slog.i(str, stringBuilder.toString());
            }
            startAodService(4);
        }
        synchronized (this.mLock) {
            this.mAwake = true;
            updateWakeGestureListenerLp();
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
        if (this.mCurrentUserId == 0) {
            Secure.putInt(this.mContext.getContentResolver(), "lock_screen_state", 1);
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onStartedWakingUp();
        }
    }

    public void finishedWakingUp() {
        if (DEBUG_WAKEUP) {
            Slog.i(TAG, "UL_PowerFinished waking up...");
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.onFinishedWakingUp();
        }
    }

    public boolean isStatusBarKeyguardShowing() {
        return isStatusBarKeyguard() && !this.mKeyguardOccluded;
    }

    protected void wakeUpFromPowerKey(long eventTime) {
        Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power Wakeing Up From PowerKey...");
        if (Jlog.isPerfTest()) {
            Jlog.i(2201, "JL_PWRSCRON_PWM_GETMESSAGE");
        }
        wakeUp(eventTime, this.mAllowTheaterModeWakeFromPowerKey, "android.policy:POWER");
    }

    private boolean wakeUp(long wakeTime, boolean wakeInTheaterMode, String reason) {
        boolean theaterModeEnabled = isTheaterModeEnabled();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power Wake Up wakeInTheaterMode=");
        stringBuilder.append(wakeInTheaterMode);
        stringBuilder.append(", theaterModeEnabled=");
        stringBuilder.append(theaterModeEnabled);
        Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
        if (!wakeInTheaterMode && theaterModeEnabled) {
            return false;
        }
        if (theaterModeEnabled) {
            Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
        }
        this.mPowerManager.wakeUp(wakeTime, reason);
        return true;
    }

    private void finishKeyguardDrawn() {
        synchronized (this.mLock) {
            if (!this.mScreenOnEarly || this.mKeyguardDrawComplete) {
                return;
            }
            this.mKeyguardDrawComplete = true;
            if (this.mKeyguardDelegate != null) {
                this.mHandler.removeMessages(6);
            }
            this.mWindowManagerDrawComplete = false;
            Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power finishKeyguardDrawn -> waitForAllWindowsDrawn");
            this.mWindowManagerInternal.waitForAllWindowsDrawn(this.mWindowManagerDrawCallback, 1000);
        }
    }

    public void screenTurnedOff() {
        Flog.i(305, "Screen turned off...");
        updateScreenOffSleepToken(true);
        synchronized (this.mLock) {
            this.mScreenOnEarly = false;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = null;
            updateOrientationListenerLp();
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurnedOff();
            }
            if (mSupportAod && !this.mIsActuralShutDown) {
                startAodService(3);
            }
        }
        reportScreenStateToVrManager(false);
    }

    private long getKeyguardDrawnTimeout() {
        return ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).isBootCompleted() ? 1000 : 5000;
    }

    public void screenTurningOn(ScreenOnListener screenOnListener) {
        Slog.i(TAG, "UL_Power Screen turning on...");
        updateScreenOffSleepToken(false);
        boolean isKeyguardDrawComplete = false;
        synchronized (this.mLock) {
            Slog.i(TAG, "UL_Power screen Turning On begin...");
            if (mSupportAod) {
                startAodService(5);
            }
            this.mScreenOnEarly = true;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = screenOnListener;
            if (this.mKeyguardDelegate == null || !this.mKeyguardDelegate.hasKeyguard()) {
                Flog.i(NativeResponseCode.SERVICE_FOUND, " null mKeyguardDelegate: setting mKeyguardDrawComplete.");
                isKeyguardDrawComplete = true;
            } else {
                this.mHandler.removeMessages(6);
                this.mHandler.sendEmptyMessageDelayed(6, getKeyguardDrawnTimeout());
                this.mKeyguardDelegate.onScreenTurningOn(this.mKeyguardDrawnCallback);
            }
        }
        if (isKeyguardDrawComplete) {
            finishKeyguardDrawn();
        }
    }

    public void screenTurnedOn() {
        synchronized (this.mLock) {
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurnedOn();
            }
            if (mSupportAod) {
                startAodService(2);
            }
        }
        if (!this.mWindowManagerInternal.isMinimizedDock()) {
            String topWindowName = this.mWindowManagerInternal.getFullStackTopWindow();
            if (topWindowName != null && topWindowName.contains("SplitScreenAppActivity")) {
                Flog.i(300, "start home for minimize dock.");
                startDockOrHome(true, false);
            }
        }
        reportScreenStateToVrManager(true);
    }

    public void screenTurningOff(ScreenOffListener screenOffListener) {
        this.mWindowManagerFuncs.screenTurningOff(screenOffListener);
        synchronized (this.mLock) {
            if (this.mKeyguardDelegate != null) {
                this.mKeyguardDelegate.onScreenTurningOff();
            }
        }
    }

    private void reportScreenStateToVrManager(boolean isScreenOn) {
        if (this.mVrManagerInternal != null) {
            this.mVrManagerInternal.onScreenStateChanged(isScreenOn);
        }
    }

    private void finishWindowsDrawn() {
        synchronized (this.mLock) {
            if (DEBUG_WAKEUP) {
                Slog.i(TAG, "UL_Power finish Windows Drawn begin...");
            }
            if (!this.mScreenOnEarly || this.mWindowManagerDrawComplete) {
                return;
            }
            this.mWindowManagerDrawComplete = true;
            finishScreenTurningOn();
        }
    }

    /* JADX WARNING: Missing block: B:33:0x0095, code:
            if (r0 == null) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:34:0x0097, code:
            r0.onScreenOn();
     */
    /* JADX WARNING: Missing block: B:35:0x009a, code:
            if (r2 == false) goto L_0x00a3;
     */
    /* JADX WARNING: Missing block: B:37:?, code:
            r5.mWindowManager.enableScreenIfNeeded();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void finishScreenTurningOn() {
        synchronized (this.mLock) {
            if (DEBUG_WAKEUP) {
                Slog.i(TAG, "UL_Power finish Screen Turning On begin...");
            }
            updateOrientationListenerLp();
        }
        synchronized (this.mLock) {
            if (DEBUG_WAKEUP) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power finishScreenTurningOn: mAwake=");
                stringBuilder.append(this.mAwake);
                stringBuilder.append(", mScreenOnEarly=");
                stringBuilder.append(this.mScreenOnEarly);
                stringBuilder.append(", mScreenOnFully=");
                stringBuilder.append(this.mScreenOnFully);
                stringBuilder.append(", mKeyguardDrawComplete=");
                stringBuilder.append(this.mKeyguardDrawComplete);
                stringBuilder.append(", mWindowManagerDrawComplete=");
                stringBuilder.append(this.mWindowManagerDrawComplete);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mScreenOnFully || !this.mScreenOnEarly || !this.mWindowManagerDrawComplete || (this.mAwake && !this.mKeyguardDrawComplete)) {
                return;
            }
            Slog.i(TAG, "UL_Power Finished screen turning on...");
            ScreenOnListener listener = this.mScreenOnListener;
            this.mScreenOnListener = null;
            this.mScreenOnFully = true;
            boolean enableScreen;
            if (this.mKeyguardDrawnOnce || !this.mAwake) {
                enableScreen = false;
            } else {
                this.mKeyguardDrawnOnce = true;
                enableScreen = true;
                if (this.mBootMessageNeedsHiding) {
                    this.mBootMessageNeedsHiding = false;
                    hideBootMessages();
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x000f, code:
            if (r2.ifBootMessageShowing == false) goto L_0x0029;
     */
    /* JADX WARNING: Missing block: B:12:0x0013, code:
            if (DEBUG_WAKEUP == false) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:13:0x0015, code:
            android.util.Slog.d(TAG, "HOTA handleHideBootMessage: dismissing");
     */
    /* JADX WARNING: Missing block: B:14:0x001c, code:
            r2.mHandler.post(new com.android.server.policy.PhoneWindowManager.AnonymousClass22(r2));
            r2.ifBootMessageShowing = false;
     */
    /* JADX WARNING: Missing block: B:16:0x002b, code:
            if (r2.mBootMsgDialog == null) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:18:0x002f, code:
            if (DEBUG_WAKEUP == false) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:19:0x0031, code:
            android.util.Slog.d(TAG, "handleHideBootMessage: dismissing");
     */
    /* JADX WARNING: Missing block: B:20:0x0038, code:
            r2.mBootMsgDialog.dismiss();
            r2.mBootMsgDialog = null;
     */
    /* JADX WARNING: Missing block: B:21:0x0040, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleHideBootMessage() {
        synchronized (this.mLock) {
            if (!this.mKeyguardDrawnOnce) {
                this.mBootMessageNeedsHiding = true;
            }
        }
    }

    public boolean isScreenOn() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mScreenOnEarly;
        }
        return z;
    }

    public boolean okToAnimate() {
        return this.mAwake && !this.mGoingToSleep;
    }

    public void enableKeyguard(boolean enabled) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.setKeyguardEnabled(enabled);
        }
    }

    public void exitKeyguardSecurely(OnKeyguardExitResult callback) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.verifyUnlock(callback);
        }
    }

    public boolean isKeyguardShowingAndNotOccluded() {
        boolean z = false;
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        if (this.mKeyguardDelegate.isShowing() && !this.mKeyguardOccluded) {
            z = true;
        }
        return z;
    }

    protected boolean keyguardIsShowingTq() {
        return isKeyguardShowingAndNotOccluded();
    }

    public boolean isKeyguardTrustedLw() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isTrusted();
    }

    public boolean isKeyguardLocked() {
        return keyguardOn();
    }

    public boolean isKeyguardSecure(int userId) {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isSecure(userId);
    }

    public boolean isKeyguardOccluded() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardOccluded;
    }

    public boolean inKeyguardRestrictedKeyInputMode() {
        if (this.mKeyguardDelegate == null) {
            return false;
        }
        return this.mKeyguardDelegate.isInputRestricted();
    }

    public void dismissKeyguardLw(IKeyguardDismissCallback callback, CharSequence message) {
        if (this.mKeyguardDelegate != null && this.mKeyguardDelegate.isShowing()) {
            this.mKeyguardDelegate.dismiss(callback, message);
        } else if (callback != null) {
            try {
                callback.onDismissError();
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to call callback", e);
            }
        }
    }

    public boolean isKeyguardDrawnLw() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawnOnce || this.mKeyguardDrawComplete;
        }
        return z;
    }

    public boolean isShowingDreamLw() {
        return this.mShowingDream;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }
    }

    public void getStableInsetsLw(int displayRotation, int displayWidth, int displayHeight, DisplayCutout displayCutout, Rect outInsets) {
        outInsets.setEmpty();
        getNonDecorInsetsLw(displayRotation, displayWidth, displayHeight, displayCutout, outInsets);
        outInsets.top = Math.max(outInsets.top, this.mStatusBarHeightForRotation[displayRotation]);
    }

    public void onProximityPositive() {
    }

    public void notifyRotationChange(int rotation) {
    }

    public void getNonDecorInsetsLw(int displayRotation, int displayWidth, int displayHeight, DisplayCutout displayCutout, Rect outInsets) {
        outInsets.setEmpty();
        if (this.mHasNavigationBar) {
            int position = navigationBarPosition(displayWidth, displayHeight, displayRotation);
            if (position == 4) {
                outInsets.bottom = getNavigationBarHeight(displayRotation, this.mUiMode);
            } else if (position == 2) {
                outInsets.right = getNavigationBarWidth(displayRotation, this.mUiMode);
            } else if (position == 1) {
                outInsets.left = getNavigationBarWidth(displayRotation, this.mUiMode);
            }
        }
        if (displayCutout != null) {
            outInsets.left += displayCutout.getSafeInsetLeft();
            outInsets.top += displayCutout.getSafeInsetTop();
            outInsets.right += displayCutout.getSafeInsetRight();
            outInsets.bottom += displayCutout.getSafeInsetBottom();
        }
    }

    public boolean isNavBarForcedShownLw(WindowState windowState) {
        return this.mForceShowSystemBars;
    }

    public int getNavBarPosition() {
        return this.mNavigationBarPosition;
    }

    public boolean isDockSideAllowed(int dockSide, int originalDockSide, int displayWidth, int displayHeight, int displayRotation) {
        return isDockSideAllowed(dockSide, originalDockSide, navigationBarPosition(displayWidth, displayHeight, displayRotation), this.mNavigationBarCanMove);
    }

    @VisibleForTesting
    static boolean isDockSideAllowed(int dockSide, int originalDockSide, int navBarPosition, boolean navigationBarCanMove) {
        boolean z = true;
        if (dockSide == 2) {
            return true;
        }
        if (navigationBarCanMove) {
            if (!((dockSide == 1 && navBarPosition == 2) || (dockSide == 3 && navBarPosition == 1))) {
                z = false;
            }
            return z;
        } else if (dockSide == originalDockSide) {
            return true;
        } else {
            if (dockSide == 1 && originalDockSide == 2) {
                return true;
            }
            return false;
        }
    }

    void sendCloseSystemWindows() {
        PhoneWindow.sendCloseSystemWindows(this.mContext, null);
    }

    void sendCloseSystemWindows(String reason) {
        PhoneWindow.sendCloseSystemWindows(this.mContext, reason);
    }

    public int rotationForOrientationLw(int orientation, int lastRotation, boolean defaultDisplay) {
        int i = 0;
        int defaultRotation = SystemProperties.getInt("ro.panel.hw_orientation", 0) / 90;
        if (this.mForceDefaultOrientation) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rotationForOrientationLw defaultRotation ");
            stringBuilder.append(defaultRotation);
            Flog.i(308, stringBuilder.toString());
            return defaultRotation;
        }
        synchronized (this.mLock) {
            int sensorRotation = getRotationFromSensorOrFaceFR(orientation, lastRotation);
            int preferredRotation = -1;
            if (!defaultDisplay) {
                preferredRotation = 0;
            } else if (this.mLidState == 1 && this.mLidOpenRotation >= 0) {
                preferredRotation = this.mLidOpenRotation;
            } else if (this.mDockMode == 2 && (this.mCarDockEnablesAccelerometer || this.mCarDockRotation >= 0)) {
                preferredRotation = this.mCarDockEnablesAccelerometer ? sensorRotation : this.mCarDockRotation;
            } else if ((this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4) && (this.mDeskDockEnablesAccelerometer || this.mDeskDockRotation >= 0)) {
                if (this.mDeskDockEnablesAccelerometer) {
                    i = sensorRotation;
                } else {
                    i = this.mDeskDockRotation;
                }
                preferredRotation = i;
            } else if (this.mHdmiPlugged && this.mDemoHdmiRotationLock) {
                preferredRotation = this.mDemoHdmiRotation;
            } else if (this.mHdmiPlugged && this.mDockMode == 0 && this.mUndockedHdmiRotation >= 0) {
                preferredRotation = this.mUndockedHdmiRotation;
            } else if (this.mDemoRotationLock) {
                preferredRotation = this.mDemoRotation;
            } else if (this.mPersistentVrModeEnabled) {
                preferredRotation = this.mPortraitRotation;
            } else if (orientation == 14) {
                preferredRotation = lastRotation;
            } else if (!this.mSupportAutoRotation) {
                preferredRotation = -1;
            } else if ((this.mUserRotationMode == 0 && (orientation == 2 || orientation == -1 || orientation == 11 || orientation == 12 || orientation == 13)) || orientation == 4 || orientation == 10 || orientation == 6 || orientation == 7) {
                if (this.mAllowAllRotations < 0) {
                    if (this.mContext.getResources().getBoolean(17956870)) {
                        i = 1;
                    }
                    this.mAllowAllRotations = i;
                }
                preferredRotation = (sensorRotation != 2 || this.mAllowAllRotations == 1 || orientation == 10 || orientation == 13) ? sensorRotation : lastRotation;
            } else if (this.mUserRotationMode == 1 && orientation != 5) {
                i = -1;
                try {
                    i = this.mWindowManager.getDockedStackSide();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Remote Exception while getting dockside!");
                }
                if (i != -1) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Keep last Rotate as preferred in DockMode while auto-rotation off with lastRotation = ");
                    stringBuilder2.append(lastRotation);
                    Slog.i(str, stringBuilder2.toString());
                    preferredRotation = lastRotation;
                } else {
                    preferredRotation = this.mUserRotation;
                }
            }
            i = preferredRotation;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("rotationForOrientationLw orientation=");
            stringBuilder3.append(orientation);
            stringBuilder3.append(", lastRotation=");
            stringBuilder3.append(lastRotation);
            stringBuilder3.append(", sensorRotation=");
            stringBuilder3.append(sensorRotation);
            stringBuilder3.append(", preferredRotation=");
            stringBuilder3.append(i);
            stringBuilder3.append("); user=");
            stringBuilder3.append(this.mUserRotation);
            stringBuilder3.append(" ");
            stringBuilder3.append(this.mUserRotationMode == 1 ? "USER_ROTATION_LOCKED" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            Flog.i(308, stringBuilder3.toString());
            int i2;
            switch (orientation) {
                case 0:
                    if (isLandscapeOrSeascape(i)) {
                        return i;
                    }
                    i2 = this.mLandscapeRotation;
                    return i2;
                case 1:
                    if (isAnyPortrait(i)) {
                        return i;
                    }
                    i2 = this.mPortraitRotation;
                    return i2;
                case 6:
                case 11:
                    if (isLandscapeOrSeascape(i)) {
                        return i;
                    } else if (isLandscapeOrSeascape(lastRotation)) {
                        return lastRotation;
                    } else {
                        i2 = this.mLandscapeRotation;
                        return i2;
                    }
                case 7:
                case 12:
                    if (isAnyPortrait(i)) {
                        return i;
                    } else if (isAnyPortrait(lastRotation)) {
                        return lastRotation;
                    } else {
                        i2 = this.mPortraitRotation;
                        return i2;
                    }
                case 8:
                    if (isLandscapeOrSeascape(i)) {
                        return i;
                    }
                    i2 = this.mSeascapeRotation;
                    return i2;
                case 9:
                    if (isAnyPortrait(i)) {
                        return i;
                    }
                    i2 = this.mUpsideDownRotation;
                    return i2;
                default:
                    if (i >= 0) {
                        return i;
                    }
                    return defaultRotation;
            }
        }
    }

    /* JADX WARNING: Missing block: B:5:0x000c, code:
            return isAnyPortrait(r3);
     */
    /* JADX WARNING: Missing block: B:7:0x0011, code:
            return isLandscapeOrSeascape(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean rotationHasCompatibleMetricsLw(int orientation, int rotation) {
        switch (orientation) {
            case 0:
                break;
            case 1:
                break;
            default:
                switch (orientation) {
                    case 6:
                    case 8:
                        break;
                    case 7:
                    case 9:
                        break;
                    default:
                        return true;
                }
        }
    }

    public void setRotationLw(int rotation) {
        this.mOrientationListener.setCurrentRotation(rotation);
    }

    public boolean isRotationChoicePossible(int orientation) {
        if (this.mUserRotationMode != 1 || this.mForceDefaultOrientation) {
            return false;
        }
        if (this.mLidState == 1 && this.mLidOpenRotation >= 0) {
            return false;
        }
        if (this.mDockMode == 2 && !this.mCarDockEnablesAccelerometer) {
            return false;
        }
        if ((this.mDockMode == 1 || this.mDockMode == 3 || this.mDockMode == 4) && !this.mDeskDockEnablesAccelerometer) {
            return false;
        }
        if (this.mHdmiPlugged && this.mDemoHdmiRotationLock) {
            return false;
        }
        if ((this.mHdmiPlugged && this.mDockMode == 0 && this.mUndockedHdmiRotation >= 0) || this.mDemoRotationLock || this.mPersistentVrModeEnabled || !this.mSupportAutoRotation) {
            return false;
        }
        if (!(orientation == -1 || orientation == 2)) {
            switch (orientation) {
                case 11:
                case 12:
                case 13:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    public boolean isValidRotationChoice(int orientation, int preferredRotation) {
        boolean z = true;
        if (orientation == -1 || orientation == 2) {
            if (preferredRotation < 0 || preferredRotation == this.mUpsideDownRotation) {
                z = false;
            }
            return z;
        }
        switch (orientation) {
            case 11:
                return isLandscapeOrSeascape(preferredRotation);
            case 12:
                if (preferredRotation != this.mPortraitRotation) {
                    z = false;
                }
                return z;
            case 13:
                if (preferredRotation < 0) {
                    z = false;
                }
                return z;
            default:
                return false;
        }
    }

    private boolean isLandscapeOrSeascape(int rotation) {
        return rotation == this.mLandscapeRotation || rotation == this.mSeascapeRotation;
    }

    private boolean isAnyPortrait(int rotation) {
        return rotation == this.mPortraitRotation || rotation == this.mUpsideDownRotation;
    }

    public int getUserRotationMode() {
        if (System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2) != 0) {
            return 0;
        }
        return 1;
    }

    public void setUserRotationMode(int mode, int rot) {
        ContentResolver res = this.mContext.getContentResolver();
        if (mode == 1) {
            System.putIntForUser(res, "user_rotation", rot, -2);
            System.putIntForUser(res, "accelerometer_rotation", 0, -2);
            return;
        }
        System.putIntForUser(res, "accelerometer_rotation", 1, -2);
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
        if (safeMode) {
            performHapticFeedbackLw(null, 10001, true);
        }
    }

    static long[] getLongIntArray(Resources r, int resid) {
        return ArrayUtils.convertToLongArray(r.getIntArray(resid));
    }

    private void bindKeyguard() {
        synchronized (this.mLock) {
            if (this.mKeyguardBound) {
                return;
            }
            this.mKeyguardBound = true;
            this.mKeyguardDelegate.bindService(this.mContext);
        }
    }

    public void onSystemUiStarted() {
        bindKeyguard();
    }

    public void systemReady() {
        this.mKeyguardDelegate.onSystemReady();
        this.mVrManagerInternal = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (this.mVrManagerInternal != null) {
            this.mVrManagerInternal.addPersistentVrModeStateListener(this.mPersistentVrModeListener);
        }
        readCameraLensCoverState();
        updateUiMode();
        synchronized (this.mLock) {
            updateOrientationListenerLp();
            this.mSystemReady = true;
            this.mHandler.post(new Runnable() {
                public void run() {
                    PhoneWindowManager.this.updateSettings();
                }
            });
            if (this.mSystemBooted) {
                this.mKeyguardDelegate.onBootCompleted();
            }
        }
        this.mSystemGestures.systemReady();
        this.mImmersiveModeConfirmation.systemReady();
        this.mAutofillManagerInternal = (AutofillManagerInternal) LocalServices.getService(AutofillManagerInternal.class);
    }

    public void systemBooted() {
        bindKeyguard();
        synchronized (this.mLock) {
            this.mSystemBooted = true;
            if (this.mSystemReady) {
                this.mKeyguardDelegate.onBootCompleted();
            }
        }
        startedWakingUp();
        screenTurningOn(null);
        screenTurnedOn();
    }

    public boolean canDismissBootAnimation() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawComplete;
        }
        return z;
    }

    public void showBootMessage(final CharSequence msg, boolean always) {
        final String[] s = msg.toString().split(":");
        if (s[0].equals("HOTA") && s.length == 3) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    int curr = 0;
                    int total = 0;
                    try {
                        curr = Integer.parseInt(s[1]);
                        total = Integer.parseInt(s[2]);
                    } catch (NumberFormatException e) {
                        Log.e(PhoneWindowManager.TAG, "showBootMessage->NumberFormatException happened");
                    }
                    HwPolicyFactory.showBootMessage(PhoneWindowManager.this.mContext, curr, total);
                }
            });
            this.ifBootMessageShowing = true;
        } else if (!this.ifBootMessageShowing) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (PhoneWindowManager.this.mBootMsgDialog == null) {
                        int theme;
                        if (PhoneWindowManager.this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) {
                            theme = 16974829;
                        } else {
                            theme = 0;
                        }
                        PhoneWindowManager.this.mBootMsgDialog = new ProgressDialog(PhoneWindowManager.this.mContext, theme) {
                            public boolean dispatchKeyEvent(KeyEvent event) {
                                return true;
                            }

                            public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                                return true;
                            }

                            public boolean dispatchTouchEvent(MotionEvent ev) {
                                return true;
                            }

                            public boolean dispatchTrackballEvent(MotionEvent ev) {
                                return true;
                            }

                            public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                                return true;
                            }

                            public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                                return true;
                            }
                        };
                        if (PhoneWindowManager.this.mContext.getPackageManager().isUpgrade()) {
                            PhoneWindowManager.this.mBootMsgDialog.setTitle(17039587);
                        } else {
                            PhoneWindowManager.this.mBootMsgDialog.setTitle(17039579);
                        }
                        PhoneWindowManager.this.mBootMsgDialog.setProgressStyle(0);
                        PhoneWindowManager.this.mBootMsgDialog.setIndeterminate(true);
                        PhoneWindowManager.this.mBootMsgDialog.getWindow().setType(2021);
                        PhoneWindowManager.this.mBootMsgDialog.getWindow().addFlags(LightsManager.LIGHT_ID_AUTOCUSTOMBACKLIGHT);
                        PhoneWindowManager.this.mBootMsgDialog.getWindow().setDimAmount(1.0f);
                        LayoutParams lp = PhoneWindowManager.this.mBootMsgDialog.getWindow().getAttributes();
                        lp.screenOrientation = 5;
                        PhoneWindowManager.this.mBootMsgDialog.getWindow().setAttributes(lp);
                        PhoneWindowManager.this.mBootMsgDialog.setCancelable(false);
                        PhoneWindowManager.this.mBootMsgDialog.show();
                    }
                    PhoneWindowManager.this.mBootMsgDialog.setMessage(msg);
                }
            });
        }
    }

    public void hideBootMessages() {
        this.mHandler.sendEmptyMessage(11);
    }

    public void requestUserActivityNotification() {
        if (!this.mNotifyUserActivity && !this.mHandler.hasMessages(29)) {
            this.mNotifyUserActivity = true;
        }
    }

    public void userActivity() {
        synchronized (this.mScreenLockTimeout) {
            if (this.mLockScreenTimerActive) {
                this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                this.mHandler.postDelayed(this.mScreenLockTimeout, (long) this.mLockScreenTimeout);
            }
        }
        if (this.mAwake && this.mNotifyUserActivity) {
            this.mHandler.sendEmptyMessageDelayed(29, 200);
            this.mNotifyUserActivity = false;
        }
    }

    public void lockNow(Bundle options) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
        this.mHandler.removeCallbacks(this.mScreenLockTimeout);
        if (options != null) {
            this.mScreenLockTimeout.setLockOptions(options);
        }
        this.mHandler.post(this.mScreenLockTimeout);
    }

    private void updateLockScreenTimeout() {
        synchronized (this.mScreenLockTimeout) {
            boolean enable = this.mAllowLockscreenWhenOn && this.mAwake && this.mKeyguardDelegate != null && this.mKeyguardDelegate.isSecure(this.mCurrentUserId);
            if (this.mLockScreenTimerActive != enable) {
                if (enable) {
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                    this.mHandler.postDelayed(this.mScreenLockTimeout, (long) this.mLockScreenTimeout);
                } else {
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                }
                this.mLockScreenTimerActive = enable;
            }
        }
    }

    private void updateDreamingSleepToken(boolean acquire) {
        if (acquire) {
            if (this.mDreamingSleepToken == null) {
                this.mDreamingSleepToken = this.mActivityManagerInternal.acquireSleepToken("Dream", 0);
            }
        } else if (this.mDreamingSleepToken != null) {
            this.mDreamingSleepToken.release();
            this.mDreamingSleepToken = null;
        }
    }

    private void updateScreenOffSleepToken(boolean acquire) {
        if (acquire) {
            if (this.mScreenOffSleepToken == null) {
                this.mScreenOffSleepToken = this.mActivityManagerInternal.acquireSleepToken("ScreenOff", 0);
            }
        } else if (this.mScreenOffSleepToken != null) {
            this.mScreenOffSleepToken.release();
            this.mScreenOffSleepToken = null;
        }
    }

    public void enableScreenAfterBoot() {
        readLidState();
        applyLidSwitchState();
    }

    private void applyLidSwitchState() {
        if (this.mLidState == 0 && this.mLidControlsSleep) {
            goToSleep(SystemClock.uptimeMillis(), 3, 1);
        } else if (this.mLidState == 0 && this.mLidControlsScreenLock) {
            this.mWindowManagerFuncs.lockDeviceNow();
        }
        synchronized (this.mLock) {
            updateWakeGestureListenerLp();
        }
    }

    void updateUiMode() {
        if (this.mUiModeManager == null) {
            this.mUiModeManager = Stub.asInterface(ServiceManager.getService("uimode"));
        }
        try {
            this.mUiMode = this.mUiModeManager.getCurrentModeType();
        } catch (RemoteException e) {
        }
    }

    void updateRotation(boolean alwaysSendConfiguration) {
        try {
            this.mWindowManager.updateRotation(alwaysSendConfiguration, false);
        } catch (RemoteException e) {
        }
    }

    void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        try {
            this.mWindowManager.updateRotation(alwaysSendConfiguration, forceRelayout);
        } catch (RemoteException e) {
        }
    }

    Intent createHomeDockIntent() {
        Intent intent = null;
        if (this.mUiMode == 3) {
            if (this.mEnableCarDockHomeCapture) {
                intent = this.mCarDockIntent;
            }
        } else if (this.mUiMode != 2) {
            if (this.mUiMode == 6 && (this.mDockMode == 1 || this.mDockMode == 4 || this.mDockMode == 3)) {
                intent = this.mDeskDockIntent;
            } else if (this.mUiMode == 7) {
                intent = this.mVrHeadsetHomeIntent;
            }
        }
        if (intent == null) {
            return null;
        }
        ActivityInfo ai = null;
        ResolveInfo info = this.mContext.getPackageManager().resolveActivityAsUser(intent, 65664, this.mCurrentUserId);
        if (info != null) {
            ai = info.activityInfo;
        }
        if (ai == null || ai.metaData == null || !ai.metaData.getBoolean("android.dock_home")) {
            return null;
        }
        intent = new Intent(intent);
        intent.setClassName(ai.packageName, ai.name);
        return intent;
    }

    void startDockOrHome(boolean fromHomeKey, boolean awakenFromDreams) {
        Intent intent;
        try {
            ActivityManager.getService().stopAppSwitches();
        } catch (RemoteException e) {
        }
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
        if (awakenFromDreams) {
            awakenDreams();
        }
        Intent dock = createHomeDockIntent();
        if (dock != null) {
            if (fromHomeKey) {
                try {
                    dock.putExtra("android.intent.extra.FROM_HOME_KEY", fromHomeKey);
                } catch (ActivityNotFoundException e2) {
                }
            }
            startActivityAsUser(dock, UserHandle.CURRENT);
            return;
        }
        if (fromHomeKey) {
            intent = new Intent(this.mHomeIntent);
            intent.putExtra("android.intent.extra.FROM_HOME_KEY", fromHomeKey);
        } else {
            intent = this.mHomeIntent;
        }
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

    boolean goHome() {
        if (isUserSetupComplete()) {
            try {
                if (SystemProperties.getInt("persist.sys.uts-test-mode", 0) == 1) {
                    Log.d(TAG, "UTS-TEST-MODE");
                } else {
                    ActivityManager.getService().stopAppSwitches();
                    sendCloseSystemWindows();
                    Intent dock = createHomeDockIntent();
                    if (dock != null) {
                        if (ActivityManager.getService().startActivityAsUser(null, null, dock, dock.resolveTypeIfNeeded(this.mContext.getContentResolver()), null, null, 0, 1, null, null, -2) == 1) {
                            return false;
                        }
                    }
                }
                if (ActivityManager.getService().startActivityAsUser(null, null, this.mHomeIntent, this.mHomeIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), null, null, 0, 1, null, null, -2) == 1) {
                    return false;
                }
                return true;
            } catch (RemoteException e) {
            }
        } else {
            Slog.i(TAG, "Not going home because user setup is in progress.");
            return false;
        }
    }

    public void setCurrentOrientationLw(int newOrientation) {
        synchronized (this.mLock) {
            if (newOrientation != this.mCurrentAppOrientation) {
                this.mCurrentAppOrientation = newOrientation;
                updateOrientationListenerLp();
            }
        }
    }

    private boolean isTheaterModeEnabled() {
        return Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
    }

    /* JADX WARNING: Missing block: B:25:0x0060, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean performHapticFeedbackLw(WindowState win, int effectId, boolean always) {
        if (this.mVibrator == null) {
            this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        }
        if (this.mVibrator == null || this.mKeyguardDelegate == null || !this.mVibrator.hasVibrator()) {
            return false;
        }
        if ((System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0) && !always) {
            return false;
        }
        VibrationEffect effect = getVibrationEffect(effectId);
        if (effect == null) {
            return false;
        }
        int owningUid;
        String owningPackage;
        if (win != null) {
            owningUid = win.getOwningUid();
            owningPackage = win.getOwningPackage();
        } else {
            owningUid = Process.myUid();
            owningPackage = this.mContext.getOpPackageName();
        }
        this.mVibrator.vibrate(owningUid, owningPackage, effect, VIBRATION_ATTRIBUTES);
        return true;
    }

    /* JADX WARNING: Missing block: B:14:0x0025, code:
            return android.os.VibrationEffect.get(0);
     */
    /* JADX WARNING: Missing block: B:16:0x002b, code:
            return android.os.VibrationEffect.get(5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private VibrationEffect getVibrationEffect(int effectId) {
        long[] pattern;
        if (effectId != 10001) {
            switch (effectId) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    switch (effectId) {
                        case 3:
                        case 12:
                        case 15:
                        case 16:
                            break;
                        case 4:
                        case 6:
                            return VibrationEffect.get(2);
                        case 5:
                            pattern = this.mCalendarDateVibePattern;
                            break;
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 13:
                            return VibrationEffect.get(2, false);
                        case 14:
                            break;
                        case 17:
                            return VibrationEffect.get(1);
                        default:
                            return null;
                    }
            }
        }
        pattern = this.mSafeModeEnabledVibePattern;
        if (pattern.length == 0) {
            return null;
        }
        if (pattern.length == 1) {
            return VibrationEffect.createOneShot(pattern[0], -1);
        }
        return VibrationEffect.createWaveform(pattern, -1);
    }

    public void keepScreenOnStartedLw() {
    }

    public void keepScreenOnStoppedLw() {
        if (isKeyguardShowingAndNotOccluded()) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    private int updateSystemUiVisibilityLw() {
        WindowState winCandidate;
        if (this.mFocusedWindow != null) {
            winCandidate = this.mFocusedWindow;
        } else {
            winCandidate = this.mTopFullscreenOpaqueWindowState;
        }
        if (winCandidate == null) {
            return 0;
        }
        if (winCandidate.getAttrs().token == this.mImmersiveModeConfirmation.getWindowToken()) {
            winCandidate = isStatusBarKeyguard() ? this.mStatusBar : this.mTopFullscreenOpaqueWindowState;
            if (winCandidate == null) {
                return 0;
            }
        }
        WindowState winCandidate2 = winCandidate;
        WindowState win = winCandidate2;
        if (win.getAttrs().type == 3 && this.mLastStartingWindow != win) {
            this.mLastStartingWindow = win;
        }
        if ((win.getAttrs().privateFlags & 1024) != 0 && this.mKeyguardOccluded) {
            return 0;
        }
        int tmpVisibility = (PolicyControl.getSystemUiVisibility(win, null) & (~this.mResettingSystemUiFlags)) & (~this.mForceClearedSystemUiFlags);
        if (this.mForcingShowNavBar && win.getSurfaceLayer() < this.mForcingShowNavBarLayer) {
            tmpVisibility &= ~PolicyControl.adjustClearableFlags(win, 7);
        }
        int tmpVisibility2 = tmpVisibility;
        int fullscreenVisibility = updateLightStatusBarLw(0, this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState);
        int dockedVisibility = updateLightStatusBarLw(0, this.mTopDockedOpaqueWindowState, this.mTopDockedOpaqueOrDimmingWindowState);
        this.mWindowManagerFuncs.getStackBounds(0, 2, this.mNonDockedStackBounds);
        boolean z = true;
        this.mWindowManagerFuncs.getStackBounds(3, 1, this.mDockedStackBounds);
        int visibility = updateSystemBarsLw(win, this.mLastSystemUiFlags, tmpVisibility2);
        int visSysui = (win.getAttrs().privateFlags & Integer.MIN_VALUE) != 0 ? visibility | 2 : visibility;
        int diff = visSysui ^ this.mLastSystemUiFlags;
        int fullscreenDiff = fullscreenVisibility ^ this.mLastFullscreenStackSysUiFlags;
        int dockedDiff = dockedVisibility ^ this.mLastDockedStackSysUiFlags;
        if ((diff & 4096) == 0) {
            z = false;
        }
        this.mImmersiveStatusChanged = z;
        z = win.getNeedsMenuLw(this.mTopFullscreenOpaqueWindowState);
        if (diff == 0 && fullscreenDiff == 0 && dockedDiff == 0 && this.mLastFocusNeedsMenu == z && this.mFocusedApp == win.getAppToken() && this.mLastNonDockedStackBounds.equals(this.mNonDockedStackBounds) && this.mLastDockedStackBounds.equals(this.mDockedStackBounds)) {
            return 0;
        }
        Pattern pattern = Pattern.compile("[0-9]++");
        Matcher matcherForWin = pattern.matcher(win.getAttrs().getTitle());
        if (!(matcherForWin.find() || diff == 0)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Policy setSystemUiVisibility, vis=");
            stringBuilder.append(Integer.toHexString(visibility));
            stringBuilder.append(",lastVis=");
            stringBuilder.append(Integer.toHexString(this.mLastSystemUiFlags));
            stringBuilder.append(",diff=");
            stringBuilder.append(Integer.toHexString(diff));
            stringBuilder.append(",win=");
            stringBuilder.append(win.toString());
            Flog.i(303, stringBuilder.toString());
        }
        this.mLastSystemUiFlags = visSysui;
        this.mLastFullscreenStackSysUiFlags = fullscreenVisibility;
        this.mLastDockedStackSysUiFlags = dockedVisibility;
        this.mLastFocusNeedsMenu = z;
        this.mFocusedApp = win.getAppToken();
        final Rect fullscreenStackBounds = new Rect(this.mNonDockedStackBounds);
        final Rect dockedStackBounds = new Rect(this.mDockedStackBounds);
        final int i = visibility;
        boolean needsMenu = z;
        final int i2 = fullscreenVisibility;
        int diff2 = diff;
        diff = dockedVisibility;
        final WindowState windowState = win;
        final boolean visibility2 = needsMenu;
        this.mHandler.post(new Runnable() {
            public void run() {
                StatusBarManagerInternal statusbar = PhoneWindowManager.this.getStatusBarManagerInternal();
                if (statusbar != null) {
                    statusbar.setSystemUiVisibility(i, i2, diff, -1, fullscreenStackBounds, dockedStackBounds, windowState.toString());
                    statusbar.topAppWindowChanged(visibility2);
                }
            }
        });
        if (!(win.getAttrs().type == 3 || this.mFocusedWindow == null)) {
            updateSystemUiColorLw(win);
        }
        return diff2;
    }

    public void updateSystemUiColorLw(WindowState win) {
    }

    private int updateLightStatusBarLw(int vis, WindowState opaque, WindowState opaqueOrDimming) {
        boolean onKeyguard = isStatusBarKeyguard() && !this.mKeyguardOccluded;
        WindowState statusColorWin = onKeyguard ? this.mStatusBar : opaqueOrDimming;
        if (statusColorWin != null && (statusColorWin == opaque || onKeyguard)) {
            return (vis & -8193) | (PolicyControl.getSystemUiVisibility(statusColorWin, null) & 8192);
        }
        if (statusColorWin == null || !statusColorWin.isDimming()) {
            return vis;
        }
        return vis & -8193;
    }

    @VisibleForTesting
    static WindowState chooseNavigationColorWindowLw(WindowState opaque, WindowState opaqueOrDimming, WindowState imeWindow, int navBarPosition) {
        WindowState windowState = null;
        boolean imeWindowCanNavColorWindow = imeWindow != null && imeWindow.isVisibleLw() && navBarPosition == 4 && (PolicyControl.getWindowFlags(imeWindow, null) & Integer.MIN_VALUE) != 0;
        if (opaque != null && opaqueOrDimming == opaque) {
            return imeWindowCanNavColorWindow ? imeWindow : opaque;
        } else if (opaqueOrDimming == null || !opaqueOrDimming.isDimming()) {
            if (imeWindowCanNavColorWindow) {
                windowState = imeWindow;
            }
            return windowState;
        } else if (imeWindowCanNavColorWindow && LayoutParams.mayUseInputMethod(PolicyControl.getWindowFlags(opaqueOrDimming, null))) {
            return imeWindow;
        } else {
            return opaqueOrDimming;
        }
    }

    @VisibleForTesting
    static int updateLightNavigationBarLw(int vis, WindowState opaque, WindowState opaqueOrDimming, WindowState imeWindow, WindowState navColorWin) {
        if (navColorWin == null) {
            return vis;
        }
        if (navColorWin == imeWindow || navColorWin == opaque) {
            return (vis & -17) | (PolicyControl.getSystemUiVisibility(navColorWin, null) & 16);
        }
        if (navColorWin == opaqueOrDimming && navColorWin.isDimming()) {
            return vis & -17;
        }
        return vis;
    }

    private int updateSystemBarsLw(WindowState win, int oldVis, int vis) {
        WindowState fullscreenTransWin;
        int i = oldVis;
        boolean dockedStackVisible = this.mWindowManagerInternal.isStackVisible(3);
        boolean freeformStackVisible = this.mWindowManagerInternal.isStackVisible(5);
        boolean resizing = this.mWindowManagerInternal.isDockedDividerResizing();
        boolean z = dockedStackVisible || freeformStackVisible || resizing;
        this.mForceShowSystemBars = z;
        z = this.mForceShowSystemBars && !this.mForceStatusBarFromKeyguard;
        if (!isStatusBarKeyguard() || this.mKeyguardOccluded) {
            fullscreenTransWin = this.mTopFullscreenOpaqueWindowState;
        } else {
            fullscreenTransWin = this.mStatusBar;
        }
        int vis2 = this.mNavigationBarController.applyTranslucentFlagLw(fullscreenTransWin, this.mStatusBarController.applyTranslucentFlagLw(fullscreenTransWin, vis, i), i);
        if (z && win.getAttrs().isEmuiStyle != 0) {
            vis2 |= 1073741824;
        }
        int dockedVis = this.mStatusBarController.applyTranslucentFlagLw(this.mTopDockedOpaqueWindowState, 0, 0);
        boolean fullscreenDrawsStatusBarBackground = drawsStatusBarBackground(vis2, this.mTopFullscreenOpaqueWindowState);
        boolean dockedDrawsStatusBarBackground = drawsStatusBarBackground(dockedVis, this.mTopDockedOpaqueWindowState);
        boolean statusBarHasFocus = win.getAttrs().type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME;
        if (statusBarHasFocus && !isStatusBarKeyguard()) {
            int flags = 14342;
            if (this.mKeyguardOccluded) {
                flags = 14342 | -1073741824;
            }
            vis2 = ((~flags) & vis2) | (i & flags);
        }
        if (fullscreenDrawsStatusBarBackground && dockedDrawsStatusBarBackground) {
            vis2 = -1073741825 & (vis2 | 8);
        } else if (!(areTranslucentBarsAllowed() || fullscreenTransWin == this.mStatusBar)) {
            vis2 &= -1073741833;
        }
        int vis3 = configureNavBarOpacity(vis2, dockedStackVisible, freeformStackVisible, resizing);
        boolean immersiveSticky = (vis3 & 4096) != 0;
        boolean hideStatusBarWM = (this.mTopFullscreenOpaqueWindowState == null || (PolicyControl.getWindowFlags(this.mTopFullscreenOpaqueWindowState, null) & 1024) == 0) ? false : true;
        boolean hideStatusBarSysui = (vis3 & 4) != 0;
        boolean hideNavBarSysui = (vis3 & 2) != 0;
        if (this.mCust) {
            vis3 = this.mCust.updateSystemBarsLw(this.mContext, this.mFocusedWindow, vis3);
        } else {
            boolean z2 = resizing;
        }
        dockedStackVisible = this.mStatusBar != null && (statusBarHasFocus || (!this.mForceShowSystemBars && ((hideStatusBarWM && !this.mImmersiveStatusChanged) || (hideStatusBarSysui && immersiveSticky))));
        freeformStackVisible = this.mNavigationBar != null && ((!this.mForceShowSystemBars && hideNavBarSysui && hideStatusBarWM) || (!this.mForceShowSystemBars && hideNavBarSysui && immersiveSticky));
        resizing = this.mPendingPanicGestureUptime != 0 && SystemClock.uptimeMillis() - this.mPendingPanicGestureUptime <= 30000;
        if (resizing && hideNavBarSysui && !isStatusBarKeyguard() && this.mKeyguardDrawComplete) {
            this.mPendingPanicGestureUptime = 0;
            this.mStatusBarController.showTransient();
            if (!isNavBarEmpty(vis3)) {
                this.mNavigationBarController.showTransient();
            }
        }
        hideStatusBarWM = this.mStatusBarController.isTransientShowRequested() && !dockedStackVisible && hideStatusBarSysui;
        z = this.mNavigationBarController.isTransientShowRequested() && !freeformStackVisible;
        if (hideStatusBarWM || z || this.mForceShowSystemBars) {
            clearClearableFlagsLw();
            vis3 &= -8;
        }
        boolean immersive = (vis3 & 2048) != 0;
        boolean navAllowedHidden = immersive || ((vis3 & 4096) != 0);
        if (!hideNavBarSysui || navAllowedHidden) {
            boolean z3 = hideStatusBarWM;
        } else {
            if (getWindowLayerLw(win) > getWindowLayerFromTypeLw(true)) {
                vis3 &= -3;
            }
        }
        int vis4 = this.mStatusBarController.updateVisibilityLw(dockedStackVisible, i, vis3);
        boolean oldImmersiveMode = isImmersiveMode(i);
        hideStatusBarWM = isImmersiveMode(vis4);
        setNaviImmersiveMode(hideStatusBarWM);
        if (win == null || oldImmersiveMode == hideStatusBarWM) {
            boolean z4 = oldImmersiveMode;
            boolean z5 = z;
            boolean z6 = immersive;
        } else {
            this.mImmersiveModeConfirmation.immersiveModeChangedLw(win.getOwningPackage(), hideStatusBarWM, isUserSetupComplete(), isNavBarEmpty(win.getSystemUiVisibility()));
        }
        return updateLightNavigationBarLw(this.mNavigationBarController.updateVisibilityLw(freeformStackVisible, i, vis4), this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState, this.mWindowManagerFuncs.getInputMethodWindowLw(), chooseNavigationColorWindowLw(this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState, this.mWindowManagerFuncs.getInputMethodWindowLw(), this.mNavigationBarPosition));
    }

    private boolean drawsStatusBarBackground(int vis, WindowState win) {
        if (!this.mStatusBarController.isTransparentAllowed(win)) {
            return false;
        }
        boolean z = true;
        if (win == null) {
            return true;
        }
        boolean drawsSystemBars = (win.getAttrs().flags & Integer.MIN_VALUE) != 0;
        if (!(((win.getAttrs().privateFlags & 131072) != 0) || (drawsSystemBars && (1073741824 & vis) == 0))) {
            z = false;
        }
        return z;
    }

    private int configureNavBarOpacity(int visibility, boolean dockedStackVisible, boolean freeformStackVisible, boolean isDockedDividerResizing) {
        if (this.mNavBarOpacityMode == 0) {
            if (dockedStackVisible || freeformStackVisible || isDockedDividerResizing) {
                visibility = setNavBarOpaqueFlag(visibility);
            }
        } else if (this.mNavBarOpacityMode == 1) {
            if (isDockedDividerResizing) {
                visibility = setNavBarOpaqueFlag(visibility);
            } else if (freeformStackVisible) {
                visibility = setNavBarTranslucentFlag(visibility);
            } else {
                visibility = setNavBarOpaqueFlag(visibility);
            }
        }
        if (areTranslucentBarsAllowed()) {
            return visibility;
        }
        return visibility & HwBootFail.STAGE_BOOT_SUCCESS;
    }

    private int setNavBarOpaqueFlag(int visibility) {
        int i = 2147450879 & visibility;
        visibility = i;
        return i;
    }

    private int setNavBarTranslucentFlag(int visibility) {
        int i = Integer.MIN_VALUE | (visibility & -32769);
        visibility = i;
        return i;
    }

    private void clearClearableFlagsLw() {
        int newVal = this.mResettingSystemUiFlags | 7;
        if (newVal != this.mResettingSystemUiFlags) {
            this.mResettingSystemUiFlags = newVal;
            this.mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    }

    private boolean isImmersiveMode(int vis) {
        return (this.mNavigationBar == null || (vis & 2) == 0 || (vis & 6144) == 0 || !canHideNavigationBar()) ? false : true;
    }

    private static boolean isNavBarEmpty(int systemUiFlags) {
        return (systemUiFlags & 23068672) == 23068672;
    }

    private boolean areTranslucentBarsAllowed() {
        return this.mTranslucentDecorEnabled;
    }

    public boolean hasNavigationBar() {
        return this.mHasNavigationBar;
    }

    public void setLastInputMethodWindowLw(WindowState ime, WindowState target) {
        this.mLastInputMethodWindow = ime;
        this.mLastInputMethodTargetWindow = target;
    }

    public void setDismissImeOnBackKeyPressed(boolean newValue) {
        this.mDismissImeOnBackKeyPressed = newValue;
    }

    public void setCurrentUserLw(int newUserId) {
        this.mCurrentUserId = newUserId;
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.setCurrentUser(newUserId);
        }
        if (this.mAccessibilityShortcutController != null) {
            this.mAccessibilityShortcutController.setCurrentUser(newUserId);
        }
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            statusBar.setCurrentUser(newUserId);
        }
        setLastInputMethodWindowLw(null, null);
    }

    public void setSwitchingUser(boolean switching) {
        this.mKeyguardDelegate.setSwitchingUser(switching);
    }

    public boolean isTopLevelWindow(int windowType) {
        boolean z = true;
        if (windowType < 1000 || windowType > 1999) {
            return true;
        }
        if (windowType != 1003) {
            z = false;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:26:0x003f, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean shouldRotateSeamlessly(int oldRotation, int newRotation) {
        if (oldRotation == this.mUpsideDownRotation || newRotation == this.mUpsideDownRotation || !this.mNavigationBarCanMove) {
            return false;
        }
        int delta = newRotation - oldRotation;
        if (delta < 0) {
            delta += 4;
        }
        notifyRotate(oldRotation, newRotation);
        if (delta == 2) {
            return false;
        }
        WindowState w = this.mTopFullscreenOpaqueWindowState;
        if (w == this.mFocusedWindow && w != null && !w.isAnimatingLw() && (w.getAttrs().rotationAnimation == 2 || w.getAttrs().rotationAnimation == 3)) {
            return true;
        }
        return false;
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.mLastSystemUiFlags);
        proto.write(1159641169922L, this.mUserRotationMode);
        proto.write(1159641169923L, this.mUserRotation);
        proto.write(1159641169924L, this.mCurrentAppOrientation);
        proto.write(1133871366149L, this.mScreenOnFully);
        proto.write(1133871366150L, this.mKeyguardDrawComplete);
        proto.write(1133871366151L, this.mWindowManagerDrawComplete);
        if (this.mFocusedApp != null) {
            proto.write(1138166333448L, this.mFocusedApp.toString());
        }
        if (this.mFocusedWindow != null) {
            this.mFocusedWindow.writeIdentifierToProto(proto, 1146756268041L);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            this.mTopFullscreenOpaqueWindowState.writeIdentifierToProto(proto, 1146756268042L);
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState != null) {
            this.mTopFullscreenOpaqueOrDimmingWindowState.writeIdentifierToProto(proto, 1146756268043L);
        }
        proto.write(1133871366156L, this.mKeyguardOccluded);
        proto.write(1133871366157L, this.mKeyguardOccludedChanged);
        proto.write(1133871366158L, this.mPendingKeyguardOccluded);
        proto.write(1133871366159L, this.mForceStatusBar);
        proto.write(1133871366160L, this.mForceStatusBarFromKeyguard);
        this.mStatusBarController.writeToProto(proto, 1146756268049L);
        this.mNavigationBarController.writeToProto(proto, 1146756268050L);
        if (this.mOrientationListener != null) {
            this.mOrientationListener.writeToProto(proto, 1146756268051L);
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.writeToProto(proto, 1146756268052L);
        }
        proto.end(token);
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        pw.print("mSafeMode=");
        pw.print(this.mSafeMode);
        pw.print(" mSystemReady=");
        pw.print(this.mSystemReady);
        pw.print(" mSystemBooted=");
        pw.println(this.mSystemBooted);
        pw.print(prefix);
        pw.print("mLidState=");
        pw.print(WindowManagerFuncs.lidStateToString(this.mLidState));
        pw.print(" mLidOpenRotation=");
        pw.println(Surface.rotationToString(this.mLidOpenRotation));
        pw.print(prefix);
        pw.print("mCameraLensCoverState=");
        pw.print(WindowManagerFuncs.cameraLensStateToString(this.mCameraLensCoverState));
        pw.print(" mHdmiPlugged=");
        pw.println(this.mHdmiPlugged);
        if (!(this.mLastSystemUiFlags == 0 && this.mResettingSystemUiFlags == 0 && this.mForceClearedSystemUiFlags == 0)) {
            pw.print(prefix);
            pw.print("mLastSystemUiFlags=0x");
            pw.print(Integer.toHexString(this.mLastSystemUiFlags));
            pw.print(" mResettingSystemUiFlags=0x");
            pw.print(Integer.toHexString(this.mResettingSystemUiFlags));
            pw.print(" mForceClearedSystemUiFlags=0x");
            pw.println(Integer.toHexString(this.mForceClearedSystemUiFlags));
        }
        if (this.mLastFocusNeedsMenu) {
            pw.print(prefix);
            pw.print("mLastFocusNeedsMenu=");
            pw.println(this.mLastFocusNeedsMenu);
        }
        pw.print(prefix);
        pw.print("mWakeGestureEnabledSetting=");
        pw.println(this.mWakeGestureEnabledSetting);
        pw.print(prefix);
        pw.print("mSupportAutoRotation=");
        pw.print(this.mSupportAutoRotation);
        pw.print(" mOrientationSensorEnabled=");
        pw.println(this.mOrientationSensorEnabled);
        pw.print(prefix);
        pw.print("mUiMode=");
        pw.print(Configuration.uiModeToString(this.mUiMode));
        pw.print(" mDockMode=");
        pw.println(Intent.dockStateToString(this.mDockMode));
        pw.print(prefix);
        pw.print("mEnableCarDockHomeCapture=");
        pw.print(this.mEnableCarDockHomeCapture);
        pw.print(" mCarDockRotation=");
        pw.print(Surface.rotationToString(this.mCarDockRotation));
        pw.print(" mDeskDockRotation=");
        pw.println(Surface.rotationToString(this.mDeskDockRotation));
        pw.print(prefix);
        pw.print("mUserRotationMode=");
        pw.print(WindowManagerPolicy.userRotationModeToString(this.mUserRotationMode));
        pw.print(" mUserRotation=");
        pw.print(Surface.rotationToString(this.mUserRotation));
        pw.print(" mAllowAllRotations=");
        pw.println(allowAllRotationsToString(this.mAllowAllRotations));
        pw.print(prefix);
        pw.print("mCurrentAppOrientation=");
        pw.println(ActivityInfo.screenOrientationToString(this.mCurrentAppOrientation));
        pw.print(prefix);
        pw.print("mCarDockEnablesAccelerometer=");
        pw.print(this.mCarDockEnablesAccelerometer);
        pw.print(" mDeskDockEnablesAccelerometer=");
        pw.println(this.mDeskDockEnablesAccelerometer);
        pw.print(prefix);
        pw.print("mLidKeyboardAccessibility=");
        pw.print(this.mLidKeyboardAccessibility);
        pw.print(" mLidNavigationAccessibility=");
        pw.print(this.mLidNavigationAccessibility);
        pw.print(" mLidControlsScreenLock=");
        pw.println(this.mLidControlsScreenLock);
        pw.print(prefix);
        pw.print("mLidControlsSleep=");
        pw.println(this.mLidControlsSleep);
        pw.print(prefix);
        pw.print("mLongPressOnBackBehavior=");
        pw.println(longPressOnBackBehaviorToString(this.mLongPressOnBackBehavior));
        pw.print(prefix);
        pw.print("mLongPressOnHomeBehavior=");
        pw.println(longPressOnHomeBehaviorToString(this.mLongPressOnHomeBehavior));
        pw.print(prefix);
        pw.print("mDoubleTapOnHomeBehavior=");
        pw.println(doubleTapOnHomeBehaviorToString(this.mDoubleTapOnHomeBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnPowerBehavior=");
        pw.println(shortPressOnPowerBehaviorToString(this.mShortPressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mLongPressOnPowerBehavior=");
        pw.println(longPressOnPowerBehaviorToString(this.mLongPressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mVeryLongPressOnPowerBehavior=");
        pw.println(veryLongPressOnPowerBehaviorToString(this.mVeryLongPressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mDoublePressOnPowerBehavior=");
        pw.println(multiPressOnPowerBehaviorToString(this.mDoublePressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mTriplePressOnPowerBehavior=");
        pw.println(multiPressOnPowerBehaviorToString(this.mTriplePressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnSleepBehavior=");
        pw.println(shortPressOnSleepBehaviorToString(this.mShortPressOnSleepBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnWindowBehavior=");
        pw.println(shortPressOnWindowBehaviorToString(this.mShortPressOnWindowBehavior));
        pw.print(prefix);
        pw.print("mAllowStartActivityForLongPressOnPowerDuringSetup=");
        pw.println(this.mAllowStartActivityForLongPressOnPowerDuringSetup);
        pw.print(prefix);
        pw.print("mHasSoftInput=");
        pw.print(this.mHasSoftInput);
        pw.print(" mDismissImeOnBackKeyPressed=");
        pw.println(this.mDismissImeOnBackKeyPressed);
        pw.print(prefix);
        pw.print("mIncallPowerBehavior=");
        pw.print(incallPowerBehaviorToString(this.mIncallPowerBehavior));
        pw.print(" mIncallBackBehavior=");
        pw.print(incallBackBehaviorToString(this.mIncallBackBehavior));
        pw.print(" mEndcallBehavior=");
        pw.println(endcallBehaviorToString(this.mEndcallBehavior));
        pw.print(prefix);
        pw.print("mHomePressed=");
        pw.println(this.mHomePressed);
        pw.print(prefix);
        pw.print("mAwake=");
        pw.print(this.mAwake);
        pw.print("mScreenOnEarly=");
        pw.print(this.mScreenOnEarly);
        pw.print(" mScreenOnFully=");
        pw.println(this.mScreenOnFully);
        pw.print(prefix);
        pw.print("mKeyguardDrawComplete=");
        pw.print(this.mKeyguardDrawComplete);
        pw.print(" mWindowManagerDrawComplete=");
        pw.println(this.mWindowManagerDrawComplete);
        pw.print(prefix);
        pw.print("mDockLayer=");
        pw.print(this.mDockLayer);
        pw.print(" mStatusBarLayer=");
        pw.println(this.mStatusBarLayer);
        pw.print(prefix);
        pw.print("mShowingDream=");
        pw.print(this.mShowingDream);
        pw.print(" mDreamingLockscreen=");
        pw.print(this.mDreamingLockscreen);
        pw.print(" mDreamingSleepToken=");
        pw.println(this.mDreamingSleepToken);
        if (this.mLastInputMethodWindow != null) {
            pw.print(prefix);
            pw.print("mLastInputMethodWindow=");
            pw.println(this.mLastInputMethodWindow);
        }
        if (this.mLastInputMethodTargetWindow != null) {
            pw.print(prefix);
            pw.print("mLastInputMethodTargetWindow=");
            pw.println(this.mLastInputMethodTargetWindow);
        }
        if (this.mStatusBar != null) {
            pw.print(prefix);
            pw.print("mStatusBar=");
            pw.print(this.mStatusBar);
            pw.print(" isStatusBarKeyguard=");
            pw.println(isStatusBarKeyguard());
        }
        if (this.mNavigationBar != null) {
            pw.print(prefix);
            pw.print("mNavigationBar=");
            pw.println(this.mNavigationBar);
        }
        if (this.mFocusedWindow != null) {
            pw.print(prefix);
            pw.print("mFocusedWindow=");
            pw.println(this.mFocusedWindow);
        }
        if (this.mFocusedApp != null) {
            pw.print(prefix);
            pw.print("mFocusedApp=");
            pw.println(this.mFocusedApp);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            pw.print(prefix);
            pw.print("mTopFullscreenOpaqueWindowState=");
            pw.println(this.mTopFullscreenOpaqueWindowState);
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState != null) {
            pw.print(prefix);
            pw.print("mTopFullscreenOpaqueOrDimmingWindowState=");
            pw.println(this.mTopFullscreenOpaqueOrDimmingWindowState);
        }
        if (this.mForcingShowNavBar) {
            pw.print(prefix);
            pw.print("mForcingShowNavBar=");
            pw.println(this.mForcingShowNavBar);
            pw.print("mForcingShowNavBarLayer=");
            pw.println(this.mForcingShowNavBarLayer);
        }
        pw.print(prefix);
        pw.print("mTopIsFullscreen=");
        pw.print(this.mTopIsFullscreen);
        pw.print(" mKeyguardOccluded=");
        pw.println(this.mKeyguardOccluded);
        pw.print(prefix);
        pw.print("mKeyguardOccludedChanged=");
        pw.print(this.mKeyguardOccludedChanged);
        pw.print(" mPendingKeyguardOccluded=");
        pw.println(this.mPendingKeyguardOccluded);
        pw.print(prefix);
        pw.print("mForceStatusBar=");
        pw.print(this.mForceStatusBar);
        pw.print(" mForceStatusBarFromKeyguard=");
        pw.println(this.mForceStatusBarFromKeyguard);
        pw.print(prefix);
        pw.print("mAllowLockscreenWhenOn=");
        pw.print(this.mAllowLockscreenWhenOn);
        pw.print(" mLockScreenTimeout=");
        pw.print(this.mLockScreenTimeout);
        pw.print(" mLockScreenTimerActive=");
        pw.println(this.mLockScreenTimerActive);
        pw.print(prefix);
        pw.print("mLandscapeRotation=");
        pw.print(Surface.rotationToString(this.mLandscapeRotation));
        pw.print(" mSeascapeRotation=");
        pw.println(Surface.rotationToString(this.mSeascapeRotation));
        pw.print(prefix);
        pw.print("mPortraitRotation=");
        pw.print(Surface.rotationToString(this.mPortraitRotation));
        pw.print(" mUpsideDownRotation=");
        pw.println(Surface.rotationToString(this.mUpsideDownRotation));
        pw.print(prefix);
        pw.print("mDemoHdmiRotation=");
        pw.print(Surface.rotationToString(this.mDemoHdmiRotation));
        pw.print(" mDemoHdmiRotationLock=");
        pw.println(this.mDemoHdmiRotationLock);
        pw.print(prefix);
        pw.print("mUndockedHdmiRotation=");
        pw.println(Surface.rotationToString(this.mUndockedHdmiRotation));
        if (this.mHasFeatureLeanback) {
            pw.print(prefix);
            pw.print("mAccessibilityTvKey1Pressed=");
            pw.println(this.mAccessibilityTvKey1Pressed);
            pw.print(prefix);
            pw.print("mAccessibilityTvKey2Pressed=");
            pw.println(this.mAccessibilityTvKey2Pressed);
            pw.print(prefix);
            pw.print("mAccessibilityTvScheduled=");
            pw.println(this.mAccessibilityTvScheduled);
        }
        this.mGlobalKeyManager.dump(prefix, pw);
        this.mStatusBarController.dump(pw, prefix);
        this.mNavigationBarController.dump(pw, prefix);
        PolicyControl.dump(prefix, pw);
        if (this.mWakeGestureListener != null) {
            this.mWakeGestureListener.dump(pw, prefix);
        }
        if (this.mOrientationListener != null) {
            this.mOrientationListener.dump(pw, prefix);
        }
        if (this.mBurnInProtectionHelper != null) {
            this.mBurnInProtectionHelper.dump(prefix, pw);
        }
        if (this.mKeyguardDelegate != null) {
            this.mKeyguardDelegate.dump(prefix, pw);
        }
        pw.print(prefix);
        pw.println("Looper state:");
        Looper looper = this.mHandler.getLooper();
        Printer printWriterPrinter = new PrintWriterPrinter(pw);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  ");
        looper.dump(printWriterPrinter, stringBuilder.toString());
    }

    private static String allowAllRotationsToString(int allowAll) {
        switch (allowAll) {
            case -1:
                return Shell.NIGHT_MODE_STR_UNKNOWN;
            case 0:
                return "false";
            case 1:
                return "true";
            default:
                return Integer.toString(allowAll);
        }
    }

    private static String endcallBehaviorToString(int behavior) {
        StringBuilder sb = new StringBuilder();
        if ((behavior & 1) != 0) {
            sb.append("home|");
        }
        if ((behavior & 2) != 0) {
            sb.append("sleep|");
        }
        int N = sb.length();
        if (N == 0) {
            return "<nothing>";
        }
        return sb.substring(0, N - 1);
    }

    private static String incallPowerBehaviorToString(int behavior) {
        if ((behavior & 2) != 0) {
            return "hangup";
        }
        return "sleep";
    }

    private static String incallBackBehaviorToString(int behavior) {
        if ((behavior & 1) != 0) {
            return "hangup";
        }
        return "<nothing>";
    }

    private static String longPressOnBackBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_BACK_NOTHING";
            case 1:
                return "LONG_PRESS_BACK_GO_TO_VOICE_ASSIST";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String longPressOnHomeBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_HOME_NOTHING";
            case 1:
                return "LONG_PRESS_HOME_ALL_APPS";
            case 2:
                return "LONG_PRESS_HOME_ASSIST";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String doubleTapOnHomeBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "DOUBLE_TAP_HOME_NOTHING";
            case 1:
                return "DOUBLE_TAP_HOME_RECENT_SYSTEM_UI";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_POWER_NOTHING";
            case 1:
                return "SHORT_PRESS_POWER_GO_TO_SLEEP";
            case 2:
                return "SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP";
            case 3:
                return "SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME";
            case 4:
                return "SHORT_PRESS_POWER_GO_HOME";
            case 5:
                return "SHORT_PRESS_POWER_CLOSE_IME_OR_GO_HOME";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String longPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_POWER_NOTHING";
            case 1:
                return "LONG_PRESS_POWER_GLOBAL_ACTIONS";
            case 2:
                return "LONG_PRESS_POWER_SHUT_OFF";
            case 3:
                return "LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String veryLongPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "VERY_LONG_PRESS_POWER_NOTHING";
            case 1:
                return "VERY_LONG_PRESS_POWER_GLOBAL_ACTIONS";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String multiPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "MULTI_PRESS_POWER_NOTHING";
            case 1:
                return "MULTI_PRESS_POWER_THEATER_MODE";
            case 2:
                return "MULTI_PRESS_POWER_BRIGHTNESS_BOOST";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnSleepBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_SLEEP_GO_TO_SLEEP";
            case 1:
                return "SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnWindowBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_WINDOW_NOTHING";
            case 1:
                return "SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE";
            default:
                return Integer.toString(behavior);
        }
    }

    public void onLockTaskStateChangedLw(int lockTaskState) {
        this.mImmersiveModeConfirmation.onLockTaskModeChangedLw(lockTaskState);
    }

    public boolean setAodShowing(boolean aodShowing) {
        if (this.mAodShowing == aodShowing) {
            return false;
        }
        this.mAodShowing = aodShowing;
        return true;
    }

    protected void cancelPendingPowerKeyActionForDistouch() {
        cancelPendingPowerKeyAction();
    }

    public boolean isTopIsFullscreen() {
        return this.mTopIsFullscreen;
    }

    public boolean isLastImmersiveMode() {
        return isImmersiveMode(this.mLastSystemUiFlags);
    }

    protected BarController getStatusBarController() {
        return this.mStatusBarController;
    }

    protected ImmersiveModeConfirmation getImmersiveModeConfirmation() {
        return this.mImmersiveModeConfirmation;
    }

    protected void updateHwSystemUiVisibilityLw() {
        updateSystemUiVisibilityLw();
    }

    protected void requestHwTransientBars(WindowState swipeTarget) {
        requestTransientBars(swipeTarget);
    }

    protected WindowState getCurrentWin() {
        if (this.mFocusedWindow != null) {
            return this.mFocusedWindow;
        }
        return this.mTopFullscreenOpaqueWindowState;
    }

    protected void hwInit() {
    }

    protected int getEmuiStyleValue(int styleValue) {
        return styleValue;
    }

    protected boolean getFloatingValue(int styleValue) {
        return false;
    }

    public void setPickUpFlag() {
        if (mSupportAod) {
            this.mPickUpFlag = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPickUpFlag mPickUpFlag:");
            stringBuilder.append(this.mPickUpFlag);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public void setSyncPowerStateFlag() {
        if (mSupportAod && getSupportSensorHub()) {
            this.mSyncPowerStateFlag = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSyncPowerStateFlag mSyncPowerStateFlag:");
            stringBuilder.append(this.mSyncPowerStateFlag);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public void onPowerStateChange(int state) {
        if (mSupportAod && getSupportSensorHub()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPowerStateChange state:");
            stringBuilder.append(state);
            stringBuilder.append(" mSyncPowerStateFlag:");
            stringBuilder.append(this.mSyncPowerStateFlag);
            Slog.i(str, stringBuilder.toString());
            if (this.mSyncPowerStateFlag) {
                switch (state) {
                    case 1:
                        Slog.i(TAG, "onPowerStateChange Display.STATE_OFF setPowerState POWER_SCREENOF_AFTER_TURNINGON");
                        setPowerState(9);
                        break;
                    case 2:
                        Slog.i(TAG, "onPowerStateChange Display.STATE_ON setPowerState POWER_TURNINGON_BEFORE_WAKINGUP");
                        setPowerState(5);
                        break;
                }
                this.mSyncPowerStateFlag = false;
            }
        }
    }

    private boolean isInScreenFingerprint(int type) {
        return 1 == type || 2 == type;
    }

    private boolean getSupportSensorHub() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSupportSensorHub mDeviceNodeFD = ");
        stringBuilder.append(this.mDeviceNodeFD);
        Slog.i(str, stringBuilder.toString());
        return this.mDeviceNodeFD > 0;
    }

    private void pauseSensorhubAOD() {
        if (getSupportSensorHub()) {
            pause();
        }
    }

    public boolean startAodService(int startState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AOD startAodService mAodSwitch=");
        stringBuilder.append(this.mAodSwitch);
        stringBuilder.append(" mAODState=");
        stringBuilder.append(this.mAODState);
        stringBuilder.append(" startState=");
        stringBuilder.append(startState);
        stringBuilder.append(" mIsFingerprintEnabledBySettings=");
        stringBuilder.append(this.mIsFingerprintEnabledBySettings);
        stringBuilder.append(" mFingerprintType=");
        stringBuilder.append(this.mFingerprintType);
        Slog.i(str, stringBuilder.toString());
        boolean needSetPowerState = startState == 5 && this.mAODState != 4;
        boolean z = this.mNeedPauseAodWhileSwitchUser || (startState == 6 && this.mAODState == 3);
        this.mNeedPauseAodWhileSwitchUser = z;
        if (startState == 7 && this.mDeviceNodeFD == -2147483647) {
            this.mDeviceNodeFD = getDeviceNodeFD();
        }
        if (!mSupportAod) {
            return false;
        }
        if (-1 == this.mFingerprintType) {
            this.mFingerprintType = getHardwareType();
        }
        if (this.mAodSwitch == 0) {
            if (!isInScreenFingerprint(this.mFingerprintType)) {
                this.mAODState = startState;
                return false;
            } else if (!(this.mIsFingerprintEnabledBySettings || this.mNeedPauseAodWhileSwitchUser)) {
                this.mAODState = startState;
                return false;
            }
        }
        if (this.mAODState == startState) {
            return false;
        }
        this.mAODState = startState;
        if (startState == 4) {
            setPowerState(0);
            pauseSensorhubAOD();
            this.mNeedPauseAodWhileSwitchUser = false;
        } else if (needSetPowerState) {
            setPowerState(5);
            this.mNeedPauseAodWhileSwitchUser = false;
        } else if (startState == 7) {
            setPowerState(8);
        } else if (this.mPickUpFlag && startState == 3) {
            setPowerState(9);
        }
        handleAodState(startState, this.mPickUpFlag);
        if (this.mPickUpFlag && startState == 3) {
            Slog.w(TAG, "reset mPickUpFlag to false after handleAodState");
            this.mPickUpFlag = false;
        }
        return true;
    }

    private void handleAodState(final int startState, final boolean pickUpFlag) {
        this.mHandler.post(new Runnable() {
            public void run() {
                Intent intent = null;
                switch (startState) {
                    case 1:
                        intent = new Intent("com.huawei.aod.action.AOD_SERVICE_START");
                        break;
                    case 2:
                        synchronized (this) {
                            if (PhoneWindowManager.this.mIAodStateCallback != null) {
                                try {
                                    PhoneWindowManager.this.mIAodStateCallback.onScreenOn();
                                } catch (RemoteException e) {
                                }
                            } else {
                                intent = new Intent("com.huawei.aod.action.AOD_SCREEN_ON");
                            }
                        }
                    case 3:
                        synchronized (this) {
                            String str = PhoneWindowManager.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("handleAodState START_AOD_SCREEN_OFF mPickUpFlag:");
                            stringBuilder.append(PhoneWindowManager.this.mPickUpFlag);
                            stringBuilder.append(", pickUpFlag ");
                            stringBuilder.append(pickUpFlag);
                            Slog.i(str, stringBuilder.toString());
                            if (PhoneWindowManager.this.mIAodStateCallback != null) {
                                try {
                                    if (pickUpFlag) {
                                        PhoneWindowManager.this.mIAodStateCallback.onScreenOffWhenRaisePhone();
                                    } else {
                                        PhoneWindowManager.this.mIAodStateCallback.onScreenOff();
                                    }
                                } catch (RemoteException e2) {
                                }
                            } else {
                                intent = new Intent("com.huawei.aod.action.AOD_SCREEN_OFF");
                                intent.putExtra(PhoneWindowManager.NEED_START_AOD_WHEN_SCREEN_OFF, pickUpFlag);
                            }
                        }
                    case 4:
                        synchronized (this) {
                            if (PhoneWindowManager.this.mIAodStateCallback != null) {
                                try {
                                    PhoneWindowManager.this.mIAodStateCallback.onWakingUp();
                                } catch (RemoteException e3) {
                                }
                            } else {
                                intent = new Intent("com.huawei.aod.action.AOD_WAKE_UP");
                            }
                        }
                    case 5:
                        synchronized (this) {
                            if (PhoneWindowManager.this.mIAodStateCallback != null) {
                                try {
                                    PhoneWindowManager.this.mIAodStateCallback.onTurningOn();
                                } catch (RemoteException e4) {
                                }
                            }
                        }
                    case 6:
                        intent = new Intent("com.huawei.aod.action.AOD_SCREEN_OFF");
                        break;
                    case 7:
                        PhoneWindowManager.this.setPowerState(1);
                        synchronized (this) {
                            if (PhoneWindowManager.this.mIAodStateCallback != null) {
                                try {
                                    PhoneWindowManager.this.mIAodStateCallback.startedGoingToSleep();
                                } catch (RemoteException e5) {
                                }
                            } else {
                                intent = new Intent("com.huawei.aod.action.AOD_GOING_TO_SLEEP");
                            }
                        }
                }
                if (intent != null) {
                    intent.setComponent(new ComponentName("com.huawei.aod", "com.huawei.aod.AODService"));
                    PhoneWindowManager.this.mContext.startService(intent);
                }
            }
        });
    }

    public void regeditAodStateCallback(IAodStateCallback callback) {
        Slog.i(TAG, "AOD regeditAodStateCallback ");
        if (mSupportAod) {
            synchronized (this) {
                this.mIAodStateCallback = callback;
            }
            if (callback != null) {
                try {
                    callback.asBinder().linkToDeath(new AppDeathRecipient(), 0);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void unregeditAodStateCallback(IAodStateCallback callback) {
        Slog.i(TAG, "AOD unregeditAodStateCallback ");
        if (mSupportAod) {
            synchronized (this) {
                this.mIAodStateCallback = null;
            }
        }
    }

    private long printTimeoutLog(String functionName, long startTime, String type, int timeout) {
        long endTime = SystemClock.elapsedRealtime();
        if (endTime - startTime > ((long) timeout)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append(functionName);
            stringBuilder.append(" ");
            stringBuilder.append(type);
            stringBuilder.append(" duration: ");
            stringBuilder.append(endTime - startTime);
            Log.i(str, stringBuilder.toString());
        }
        return endTime;
    }

    public boolean isKeyguardShowingOrOccluded() {
        return this.mKeyguardDelegate == null ? false : this.mKeyguardDelegate.isShowing();
    }

    public void setFullScreenWinVisibile(boolean visible) {
        this.mHwFullScreenWinVisibility = visible;
    }

    public void setFullScreenWindow(WindowState win) {
        this.mHwFullScreenWindow = win;
    }

    public void registerRotateObserver(IHwRotateObserver observer) {
        synchronized (this.mHwRotateObservers) {
            this.mHwRotateObservers.register(observer);
        }
    }

    public void unregisterRotateObserver(IHwRotateObserver observer) {
        synchronized (this.mHwRotateObservers) {
            this.mHwRotateObservers.unregister(observer);
        }
    }

    private void notifyRotate(int oldRotation, int newRotation) {
        if (this.mHwRotateObservers != null && newRotation != oldRotation) {
            int i = this.mHwRotateObservers.beginBroadcast();
            while (i > 0) {
                i--;
                IHwRotateObserver observer = (IHwRotateObserver) this.mHwRotateObservers.getBroadcastItem(i);
                try {
                    observer.onRotate(oldRotation, newRotation);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("musa3 notifyRotate() to observer ");
                    stringBuilder.append(observer);
                    Log.i(str, stringBuilder.toString());
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RemoteException in notifyRotate(), remove observer ");
                    stringBuilder2.append(observer);
                    Log.w(str2, stringBuilder2.toString());
                    unregisterRotateObserver(observer);
                }
            }
            this.mHwRotateObservers.finishBroadcast();
        }
    }

    public int getDefaultNavBarHeight() {
        return this.mDefaultNavBarHeight;
    }

    public boolean isInputMethodMovedUp() {
        return this.mInputMethodMovedUp;
    }

    public boolean isNavBarVisible() {
        return this.mHasNavigationBar && this.mNavigationBar != null && this.mNavigationBar.isVisibleLw();
    }

    /* JADX WARNING: Missing block: B:19:0x0046, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void adjustInsetSurfaceState(final WindowState win, DisplayFrames displayFrames, int top) {
        if (mSupporInputMethodFilletAdaptation && this.mDisplayRotation == 0 && win.isImeWithHwFlag() && win.getAttrs().type == 2011) {
            if (displayFrames.mContent.bottom - top > 100) {
                if (this.mIsFloatIME) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            win.showInsetSurfaceOverlayImmediately();
                        }
                    });
                }
                this.mIsFloatIME = false;
            } else {
                if (!this.mIsFloatIME) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            win.hideInsetSurfaceOverlayImmediately();
                        }
                    });
                }
                this.mIsFloatIME = true;
            }
        }
    }
}
