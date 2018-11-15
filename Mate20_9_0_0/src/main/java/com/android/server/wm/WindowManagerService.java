package com.android.server.wm;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.animation.AnimationHandler;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OnOpChangedInternalListener;
import android.app.IActivityManager;
import android.app.IAssistDataReceiver;
import android.app.admin.DevicePolicyCache;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.configstore.V1_0.ISurfaceFlingerConfigs;
import android.hardware.configstore.V1_0.OptionalBool;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManager;
import android.iawareperf.UniPerf;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemService;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.pc.IHwPCManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.rms.HwSysResource;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.Display;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.DisplayInfo;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IDockedStackListener;
import android.view.IHwRotateObserver;
import android.view.IInputFilter;
import android.view.IOnKeyguardExitResult;
import android.view.IPinnedStackListener;
import android.view.IRecentsAnimationRunner;
import android.view.IRotationWatcher;
import android.view.IWallpaperVisibilityListener;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.InputEventReceiver.Factory;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceSession;
import android.view.WindowContentFrameStats;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerPolicyConstants.PointerEventListener;
import android.view.inputmethod.InputMethodManagerInternal;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.LatencyTracker;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.WindowManagerPolicyThread;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwWindowManagerService;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.Watchdog.Monitor;
import com.android.server.input.InputManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowManagerPolicy.InputConsumer;
import com.android.server.policy.WindowManagerPolicy.OnKeyguardExitResult;
import com.android.server.policy.WindowManagerPolicy.ScreenOffListener;
import com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.power.IHwShutdownThread;
import com.android.server.power.ShutdownThread;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.PriorityDump;
import com.android.server.utils.PriorityDump.PriorityDumper;
import com.android.server.wm.RecentsAnimationController.RecentsAnimationCallbacks;
import com.android.server.wm.RecentsAnimationController.ReorderMode;
import com.android.server.wm.WindowManagerInternal.AppTransitionListener;
import com.android.server.wm.WindowManagerInternal.IDragDropCallback;
import com.android.server.wm.WindowManagerInternal.MagnificationCallbacks;
import com.android.server.wm.WindowManagerInternal.OnHardKeyboardStatusChangeListener;
import com.android.server.wm.WindowManagerInternal.WindowsForAccessibilityCallback;
import com.android.server.zrhung.IZRHungService;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import com.huawei.android.view.IHwWMDAMonitorCallback;
import com.huawei.android.view.IHwWindowManager.Stub;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class WindowManagerService extends AbsWindowManagerService implements IHwWindowManagerInner, Monitor, WindowManagerFuncs {
    private static final long ALLWINDOWDRAW_MAX_TIMEOUT_TIME = 1000;
    private static final boolean ALWAYS_KEEP_CURRENT = true;
    private static final int ANIMATION_DURATION_SCALE = 2;
    static final int APP_ANIMATION_DURATION = 300;
    private static final int BOOT_ANIMATION_POLL_INTERVAL = 200;
    private static final String BOOT_ANIMATION_SERVICE = "bootanim";
    public static final int COMPAT_MODE_DISABLED = 0;
    public static final int COMPAT_MODE_ENABLED = 1;
    public static final int COMPAT_MODE_MATCH_PARENT = -3;
    static final boolean CUSTOM_SCREEN_ROTATION = true;
    public static final String DEBUG_ALL_OFF_CMD = "hwDebugWmsAllOff";
    public static final String DEBUG_ALL_ON_CMD = "hwDebugWmsAllOn";
    public static final String DEBUG_APP_TRANSITIONS_CMD = "hwDebugWmsTransition";
    public static final String DEBUG_CONFIGURATION_CMD = "hwDebugWmsConfiguration";
    public static final String DEBUG_DISPLAY_CMD = "hwDebugWmsDisplay";
    public static final String DEBUG_FOCUS_CMD = "hwDebugWmsFocus";
    public static final String DEBUG_INPUT_CMD = "hwDebugWmsInput";
    public static final String DEBUG_LAYERS_CMD = "hwDebugWmsLayer";
    public static final String DEBUG_LAYOUT_CMD = "hwDebugWmsLayout";
    public static final String DEBUG_ORIENTATION_CMD = "hwDebugWmsOrientation";
    public static final String DEBUG_PREFIX = "hwDebugWms";
    public static final String DEBUG_SCREEN_ON_CMD = "hwDebugWmsScreen";
    public static final String DEBUG_VISIBILITY_CMD = "hwDebugWmsVisibility";
    public static final String DEBUG_WALLPAPER_CMD = "hwDebugWmsWallpaper";
    static final long DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS = 5000000000L;
    private static final String DENSITY_OVERRIDE = "ro.config.density_override";
    static final String[] DISABLE_HW_LAUNCHER_EXIT_ANIM_CMP_LIST = new String[]{PERMISSION_DIALOG_CMP, STK_DIALOG_CMP};
    static final String DRAWER_LAUNCHER_CMP = "com.huawei.android.launcher/.drawer.DrawerLauncher";
    static final boolean HWFLOW = true;
    static final boolean HW_SUPPORT_LAUNCHER_EXIT_ANIM = (SystemProperties.getBoolean("ro.config.disable_launcher_exit_anim", false) ^ true);
    private static final int INPUT_DEVICES_READY_FOR_SAFE_MODE_DETECTION_TIMEOUT_MILLIS = 1000;
    static final boolean IS_DEBUG_VERSION = (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3);
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final int LAYER_OFFSET_DIM = 1;
    static final int LAYER_OFFSET_THUMBNAIL = 4;
    static final int LAYOUT_REPEAT_THRESHOLD = 4;
    public static final float LAZY_MODE_SCALE = 0.75f;
    static final int MAX_ANIMATION_DURATION = 10000;
    private static final int MAX_SCREENSHOT_RETRIES = 3;
    static final String NEW_SIMPLE_LAUNCHER_CMP = "com.huawei.android.launcher/.newsimpleui.NewSimpleLauncher";
    static final String PERMISSION_DIALOG_CMP = "com.android.packageinstaller/.permission.ui.GrantPermissionsActivity";
    static final boolean PROFILE_ORIENTATION = false;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    static final boolean SCREENSHOT_FORCE_565 = true;
    static final int SEAMLESS_ROTATION_TIMEOUT_DURATION = 2000;
    private static final String SIZE_OVERRIDE = "ro.config.size_override";
    static final String STK_DIALOG_CMP = "com.android.stk/.StkDialogActivity";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_SECURE = "ro.secure";
    private static final String TAG = "WindowManager";
    private static final int TRANSITION_ANIMATION_SCALE = 1;
    static final int TYPE_LAYER_MULTIPLIER = 10000;
    static final int TYPE_LAYER_OFFSET = 1000;
    static final String UNI_LAUNCHER_CMP = "com.huawei.android.launcher/.unihome.UniHomeLauncher";
    static final int UPDATE_FOCUS_NORMAL = 0;
    static final int UPDATE_FOCUS_PLACING_SURFACES = 2;
    static final int UPDATE_FOCUS_WILL_ASSIGN_LAYERS = 1;
    static final int UPDATE_FOCUS_WILL_PLACE_SURFACES = 3;
    static final int WINDOWS_FREEZING_SCREENS_ACTIVE = 1;
    static final int WINDOWS_FREEZING_SCREENS_NONE = 0;
    static final int WINDOWS_FREEZING_SCREENS_TIMEOUT = 2;
    private static final int WINDOW_ANIMATION_SCALE = 0;
    static final int WINDOW_FREEZE_TIMEOUT_DURATION = 2000;
    static final int WINDOW_LAYER_MULTIPLIER = 5;
    static final int WINDOW_REPLACEMENT_TIMEOUT_DURATION = 2000;
    static final boolean localLOGV = false;
    public static boolean mSupporInputMethodFilletAdaptation = SystemProperties.getBoolean("ro.config.support_inputmethod_fillet_adaptation", false);
    private static WindowManagerService sInstance;
    static WindowManagerThreadPriorityBooster sThreadPriorityBooster = new WindowManagerThreadPriorityBooster();
    AccessibilityController mAccessibilityController;
    final IActivityManager mActivityManager;
    final AppTransitionListener mActivityManagerAppTransitionNotifier = new AppTransitionListener() {
        public void onAppTransitionCancelledLocked(int transit) {
            WindowManagerService.this.mH.sendEmptyMessage(48);
        }

        public void onAppTransitionFinishedLocked(IBinder token) {
            WindowManagerService.this.mH.sendEmptyMessage(49);
            AppWindowToken atoken = WindowManagerService.this.mRoot.getAppWindowToken(token);
            if (atoken != null) {
                if (atoken.mLaunchTaskBehind) {
                    try {
                        WindowManagerService.this.mActivityManager.notifyLaunchTaskBehindComplete(atoken.token);
                    } catch (RemoteException e) {
                    }
                    atoken.mLaunchTaskBehind = false;
                } else {
                    atoken.updateReportedVisibilityLocked();
                    if (atoken.mEnteringAnimation && (WindowManagerService.this.getRecentsAnimationController() == null || !WindowManagerService.this.getRecentsAnimationController().isTargetApp(atoken))) {
                        atoken.mEnteringAnimation = false;
                        try {
                            WindowManagerService.this.mActivityManager.notifyEnterAnimationComplete(atoken.token);
                        } catch (RemoteException e2) {
                        }
                    }
                }
            }
        }
    };
    private HwSysResource mActivityResource;
    final boolean mAllowAnimationsInLowPowerMode;
    final boolean mAllowBootMessages;
    boolean mAllowTheaterModeWakeFromLayout;
    final ActivityManagerInternal mAmInternal;
    final Handler mAnimationHandler = new Handler(AnimationThread.getHandler().getLooper());
    final ArrayMap<AnimationAdapter, SurfaceAnimator> mAnimationTransferMap = new ArrayMap();
    private boolean mAnimationsDisabled = false;
    final WindowAnimator mAnimator;
    private float mAnimatorDurationScaleSetting = 1.0f;
    final ArrayList<AppFreezeListener> mAppFreezeListeners = new ArrayList();
    final AppOpsManager mAppOps;
    public String mAppTransitTrack = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    final AppTransition mAppTransition;
    int mAppsFreezingScreen = 0;
    boolean mBootAnimationStopped = false;
    final BoundsAnimationController mBoundsAnimationController;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Object obj = (action.hashCode() == 988075300 && action.equals("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED")) ? null : -1;
            if (obj == null) {
                WindowManagerService.this.mKeyguardDisableHandler.sendEmptyMessage(3);
            }
        }
    };
    CircularDisplayMask mCircularDisplayMask;
    boolean mClientFreezingScreen = false;
    final ArraySet<AppWindowToken> mClosingApps = new ArraySet();
    protected final Context mContext;
    WindowState mCurrentFocus = null;
    int[] mCurrentProfileIds = new int[0];
    int mCurrentUserId;
    private HwCustWindowManagerService mCust;
    final ArrayList<WindowState> mDeferRelayoutWindow = new ArrayList();
    int mDeferredRotationPauseCount;
    final ArrayList<WindowState> mDestroyPreservedSurface = new ArrayList();
    final ArrayList<WindowState> mDestroySurface = new ArrayList();
    boolean mDisableTransitionAnimation = false;
    boolean mDisplayEnabled = false;
    long mDisplayFreezeTime = 0;
    boolean mDisplayFrozen = false;
    final DisplayManager mDisplayManager;
    final DisplayManagerInternal mDisplayManagerInternal;
    boolean mDisplayReady;
    final DisplaySettings mDisplaySettings;
    Rect mDockedStackCreateBounds;
    int mDockedStackCreateMode = 0;
    final DragDropController mDragDropController;
    final long mDrawLockTimeoutMillis;
    EmulatorDisplayOverlay mEmulatorDisplayOverlay;
    private int mEnterAnimId;
    private boolean mEventDispatchingEnabled;
    private int mExitAnimId;
    int mExitFlag = -1;
    Bitmap mExitIconBitmap;
    int mExitIconHeight = 0;
    int mExitIconWidth = 0;
    float mExitPivotX = -1.0f;
    float mExitPivotY = -1.0f;
    private final PointerEventDispatcher mExternalPointerEventDispatcher;
    private HandlerThread mFingerHandlerThread = new HandlerThread("hw_finger_unlock_handler");
    private FingerUnlockHandler mFingerUnlockHandler;
    final ArrayList<AppWindowToken> mFinishedStarting = new ArrayList();
    boolean mFocusMayChange;
    AppWindowToken mFocusedApp = null;
    boolean mForceDisplayEnabled = false;
    final ArrayList<WindowState> mForceRemoves = new ArrayList();
    boolean mForceResizableTasks = false;
    private int mFrozenDisplayId;
    final H mH = new H();
    boolean mHardKeyboardAvailable;
    OnHardKeyboardStatusChangeListener mHardKeyboardStatusChangeListener;
    final boolean mHasPermanentDpad;
    private boolean mHasWideColorGamutSupport;
    final boolean mHaveInputMethods;
    private ArrayList<WindowState> mHidingNonSystemOverlayWindows = new ArrayList();
    private Session mHoldingScreenOn;
    protected WakeLock mHoldingScreenWakeLock;
    HwInnerWindowManagerService mHwInnerService = new HwInnerWindowManagerService(this);
    IHwWindowManagerServiceEx mHwWMSEx = null;
    boolean mInTouchMode;
    final InputManagerService mInputManager;
    IInputMethodManager mInputMethodManager;
    WindowState mInputMethodTarget = null;
    boolean mInputMethodTargetWaitingAnim;
    WindowState mInputMethodWindow = null;
    final InputMonitor mInputMonitor = new InputMonitor(this);
    public boolean mIsPerfBoost;
    boolean mIsTouchDevice;
    private final KeyguardDisableHandler mKeyguardDisableHandler;
    boolean mKeyguardGoingAway;
    boolean mKeyguardOrAodShowingOnDefaultDisplay;
    String mLastANRState;
    int mLastDispatchedSystemUiVisibility = 0;
    int mLastDisplayFreezeDuration = 0;
    Object mLastFinishedFreezeSource = null;
    WindowState mLastFocus = null;
    int mLastStatusBarVisibility = 0;
    WindowState mLastWakeLockHoldingWindow = null;
    WindowState mLastWakeLockObscuringWindow = null;
    private final LatencyTracker mLatencyTracker;
    public int mLazyModeOn = 0;
    final boolean mLimitedAlphaCompositing;
    ArrayList<WindowState> mLosingFocus = new ArrayList();
    final int mMaxUiWidth;
    MousePositionTracker mMousePositionTracker = new MousePositionTracker();
    final List<IBinder> mNoAnimationNotifyOnTransitionFinished = new ArrayList();
    protected boolean mNotifyFocusedWindow = false;
    final boolean mOnlyCore;
    final ArraySet<AppWindowToken> mOpeningApps = new ArraySet();
    protected WakeLock mPCHoldingScreenWakeLock;
    public IHwPCManager mPCManager = null;
    final ArrayList<WindowState> mPendingRemove = new ArrayList();
    WindowState[] mPendingRemoveTmp = new WindowState[20];
    final PackageManagerInternal mPmInternal;
    private final PointerEventDispatcher mPointerEventDispatcher;
    final WindowManagerPolicy mPolicy;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    private final PriorityDumper mPriorityDumper = new PriorityDumper() {
        public void dumpCritical(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            WindowManagerService.this.doDump(fd, pw, new String[]{"-a"}, asProto);
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            WindowManagerService.this.doDump(fd, pw, args, asProto);
        }
    };
    private RecentsAnimationController mRecentsAnimationController;
    final ArrayList<WindowState> mResizingWindows = new ArrayList();
    RootWindowContainer mRoot;
    private boolean mRotatingSeamlessly = false;
    ArrayList<RotationWatcher> mRotationWatchers = new ArrayList();
    boolean mSafeMode;
    private final WakeLock mScreenFrozenLock;
    final Rect mScreenRect = new Rect();
    private int mSeamlessRotationCount = 0;
    final ArraySet<Session> mSessions = new ArraySet();
    SettingsObserver mSettingsObserver;
    public boolean mShouldResetTime = false;
    boolean mShowAlertWindowNotifications = true;
    boolean mShowingBootMessages = false;
    boolean mSkipAppTransitionAnimation = false;
    StrictModeFlash mStrictModeFlash;
    boolean mSupportsPictureInPicture = false;
    final SurfaceAnimationRunner mSurfaceAnimationRunner;
    SurfaceBuilderFactory mSurfaceBuilderFactory = -$$Lambda$WindowManagerService$XZ-U3HlCFtHp_gydNmNMeRmQMCI.INSTANCE;
    boolean mSwitchingUser = false;
    boolean mSystemBooted = false;
    int mSystemDecorLayer = 0;
    final TaskPositioningController mTaskPositioningController;
    final TaskSnapshotController mTaskSnapshotController;
    final Configuration mTempConfiguration = new Configuration();
    private WindowContentFrameStats mTempWindowRenderStats;
    final float[] mTmpFloats = new float[9];
    final Rect mTmpRect = new Rect();
    final Rect mTmpRect2 = new Rect();
    final Rect mTmpRect3 = new Rect();
    final RectF mTmpRectF = new RectF();
    final Matrix mTmpTransform = new Matrix();
    private final Transaction mTransaction = this.mTransactionFactory.make();
    TransactionFactory mTransactionFactory = -$$Lambda$WindowManagerService$hBnABSAsqXWvQ0zKwHWE4BZ3Mc0.INSTANCE;
    int mTransactionSequence;
    private float mTransitionAnimationScaleSetting = 1.0f;
    final UnknownAppVisibilityController mUnknownAppVisibilityController = new UnknownAppVisibilityController(this);
    private ViewServer mViewServer;
    int mVr2dDisplayId = -1;
    HwWMDAMonitorProxy mWMProxy = new HwWMDAMonitorProxy();
    private long mWaitAllWindowDrawStartTime = 0;
    boolean mWaitingForConfig = false;
    ArrayList<WindowState> mWaitingForDrawn = new ArrayList();
    Runnable mWaitingForDrawnCallback;
    final WallpaperVisibilityListeners mWallpaperVisibilityListeners = new WallpaperVisibilityListeners();
    Watermark mWatermark;
    private final ArrayList<WindowState> mWinAddedSinceNullFocus = new ArrayList();
    private final ArrayList<WindowState> mWinRemovedSinceNullFocus = new ArrayList();
    private float mWindowAnimationScaleSetting = 1.0f;
    final ArrayList<WindowChangeListener> mWindowChangeListeners = new ArrayList();
    final WindowHashMap mWindowMap = new WindowHashMap();
    final WindowSurfacePlacer mWindowPlacerLocked;
    final ArrayList<AppWindowToken> mWindowReplacementTimeouts = new ArrayList();
    final WindowTracing mWindowTracing;
    boolean mWindowsChanged = false;
    int mWindowsFreezingScreen = 0;

    interface AppFreezeListener {
        void onAppFreezeTimeout();
    }

    private class FingerUnlockHandler extends Handler {
        public FingerUnlockHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 33) {
                Runnable callback;
                if (WindowManagerService.this.isPrintAllWindowsDrawnLogs()) {
                    Flog.i(NativeResponseCode.SERVICE_FOUND, "ALL_WINDOWS_DRAWN timeout");
                }
                Flog.i(307, "ALL_WINDOWS_DRAWN start callback!");
                synchronized (WindowManagerService.this.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        callback = WindowManagerService.this.mWaitingForDrawnCallback;
                        WindowManagerService.this.mWaitingForDrawnCallback = null;
                    } finally {
                        while (true) {
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    public final class H extends Handler {
        public static final int ALL_WINDOWS_DRAWN = 33;
        public static final int ANIMATION_FAILSAFE = 60;
        public static final int APP_FREEZE_TIMEOUT = 17;
        public static final int APP_TRANSITION_GETSPECSFUTURE_TIMEOUT = 102;
        public static final int APP_TRANSITION_TIMEOUT = 13;
        public static final int BOOT_TIMEOUT = 23;
        public static final int CHECK_IF_BOOT_ANIMATION_FINISHED = 37;
        public static final int CLIENT_FREEZE_TIMEOUT = 30;
        public static final int DO_ANIMATION_CALLBACK = 26;
        public static final int ENABLE_SCREEN = 16;
        public static final int FORCE_GC = 15;
        public static final int KEYGUARD_DISMISS_DONE = 101;
        public static final int NEW_ANIMATOR_SCALE = 34;
        public static final int NOTIFY_ACTIVITY_DRAWN = 32;
        public static final int NOTIFY_APP_TRANSITION_CANCELLED = 48;
        public static final int NOTIFY_APP_TRANSITION_FINISHED = 49;
        public static final int NOTIFY_APP_TRANSITION_STARTING = 47;
        public static final int NOTIFY_DOCKED_STACK_MINIMIZED_CHANGED = 53;
        public static final int NOTIFY_KEYGUARD_FLAGS_CHANGED = 56;
        public static final int NOTIFY_KEYGUARD_TRUSTED_CHANGED = 57;
        public static final int PC_FREEZE_TIMEOUT = 103;
        public static final int PERSIST_ANIMATION_SCALE = 14;
        public static final int RECOMPUTE_FOCUS = 61;
        public static final int REPORT_FOCUS_CHANGE = 2;
        public static final int REPORT_HARD_KEYBOARD_STATUS_CHANGE = 22;
        public static final int REPORT_LOSING_FOCUS = 3;
        public static final int REPORT_WINDOWS_CHANGE = 19;
        public static final int RESET_ANR_MESSAGE = 38;
        public static final int RESTORE_POINTER_ICON = 55;
        public static final int SEAMLESS_ROTATION_TIMEOUT = 54;
        public static final int SEND_NEW_CONFIGURATION = 18;
        public static final int SET_FOCUSED_TASK = 104;
        public static final int SET_HAS_OVERLAY_UI = 58;
        public static final int SET_RUNNING_REMOTE_ANIMATION = 59;
        public static final int SHOW_CIRCULAR_DISPLAY_MASK = 35;
        public static final int SHOW_EMULATOR_DISPLAY_OVERLAY = 36;
        public static final int SHOW_STRICT_MODE_VIOLATION = 25;
        public static final int UNUSED = 0;
        public static final int UPDATE_ANIMATION_SCALE = 51;
        public static final int UPDATE_DOCKED_STACK_DIVIDER = 41;
        public static final int WAITING_FOR_DRAWN_TIMEOUT = 24;
        public static final int WAIT_KEYGUARD_DISMISS_DONE_TIMEOUT = 100;
        public static final int WALLPAPER_DRAW_PENDING_TIMEOUT = 39;
        public static final int WINDOW_FREEZE_TIMEOUT = 11;
        public static final int WINDOW_HIDE_TIMEOUT = 52;
        public static final int WINDOW_REPLACEMENT_TIMEOUT = 46;

        /* JADX WARNING: Missing block: B:253:0x048e, code:
            return;
     */
        /* JADX WARNING: Missing block: B:256:0x0490, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            java.lang.Runtime.getRuntime().gc();
     */
        /* JADX WARNING: Missing block: B:315:0x061f, code:
            return;
     */
        /* JADX WARNING: Missing block: B:326:0x0659, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            r3 = r4;
            r4 = r5;
     */
        /* JADX WARNING: Missing block: B:327:0x065e, code:
            if (r0 == null) goto L_0x0663;
     */
        /* JADX WARNING: Missing block: B:328:0x0660, code:
            r0.onWindowFocusChangedNotLocked();
     */
        /* JADX WARNING: Missing block: B:329:0x0663, code:
            if (r4 == null) goto L_0x068b;
     */
        /* JADX WARNING: Missing block: B:331:0x0667, code:
            if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT == false) goto L_0x067f;
     */
        /* JADX WARNING: Missing block: B:332:0x0669, code:
            r5 = com.android.server.wm.WindowManagerService.TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("Gaining focus: ");
            r6.append(r4);
            android.util.Slog.i(r5, r6.toString());
     */
        /* JADX WARNING: Missing block: B:333:0x067f, code:
            r4.reportFocusChangedSerialized(true, r9.this$0.mInTouchMode);
            com.android.server.wm.WindowManagerService.access$600(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:334:0x068b, code:
            if (r3 == null) goto L_0x0745;
     */
        /* JADX WARNING: Missing block: B:336:0x068f, code:
            if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT == false) goto L_0x06a7;
     */
        /* JADX WARNING: Missing block: B:337:0x0691, code:
            r1 = com.android.server.wm.WindowManagerService.TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Losing focus: ");
            r5.append(r3);
            android.util.Slog.i(r1, r5.toString());
     */
        /* JADX WARNING: Missing block: B:338:0x06a7, code:
            r3.reportFocusChangedSerialized(false, r9.this$0.mInTouchMode);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 11) {
                int i2 = 0;
                if (i == 30) {
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mClientFreezingScreen) {
                                WindowManagerService.this.mClientFreezingScreen = false;
                                WindowManagerService.this.mLastFinishedFreezeSource = "client-timeout";
                                WindowManagerService.this.stopFreezingDisplayLocked();
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                } else if (i != 41) {
                    boolean z = true;
                    int i3;
                    switch (i) {
                        case 2:
                            AccessibilityController accessibilityController = null;
                            synchronized (WindowManagerService.this.mWindowMap) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    if (WindowManagerService.this.mAccessibilityController != null && (WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayId() == 0 || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad()))) {
                                        accessibilityController = WindowManagerService.this.mAccessibilityController;
                                    }
                                    WindowState lastFocus = WindowManagerService.this.mLastFocus;
                                    WindowState newFocus = WindowManagerService.this.mCurrentFocus;
                                    if (lastFocus != newFocus) {
                                        WindowManagerService.this.mLastFocus = newFocus;
                                        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                                            String str = WindowManagerService.TAG;
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("Focus moving from ");
                                            stringBuilder.append(lastFocus);
                                            stringBuilder.append(" to ");
                                            stringBuilder.append(newFocus);
                                            Slog.i(str, stringBuilder.toString());
                                        }
                                        if (!(newFocus == null || lastFocus == null || newFocus.isDisplayedLw())) {
                                            WindowManagerService.this.mLosingFocus.add(lastFocus);
                                            lastFocus = null;
                                            break;
                                        }
                                    }
                                } finally {
                                    while (true) {
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        break;
                                    }
                                }
                            }
                        case 3:
                            ArrayList<WindowState> losers;
                            synchronized (WindowManagerService.this.mWindowMap) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    losers = WindowManagerService.this.mLosingFocus;
                                    WindowManagerService.this.mLosingFocus = new ArrayList();
                                } finally {
                                    while (true) {
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        break;
                                    }
                                }
                            }
                            i = losers.size();
                            for (i3 = 0; i3 < i; i3++) {
                                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                                    String str2 = WindowManagerService.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Losing delayed focus: ");
                                    stringBuilder2.append(losers.get(i3));
                                    Slog.i(str2, stringBuilder2.toString());
                                }
                                ((WindowState) losers.get(i3)).reportFocusChangedSerialized(false, WindowManagerService.this.mInTouchMode);
                            }
                            break;
                        default:
                            String str3;
                            StringBuilder stringBuilder3;
                            int N;
                            switch (i) {
                                case 13:
                                    synchronized (WindowManagerService.this.mWindowMap) {
                                        try {
                                            WindowManagerService.boostPriorityForLockedSection();
                                            if (!(!WindowManagerService.this.mAppTransition.isTransitionSet() && WindowManagerService.this.mOpeningApps.isEmpty() && WindowManagerService.this.mClosingApps.isEmpty())) {
                                                z = WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS;
                                                str3 = WindowManagerService.TAG;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("*** APP TRANSITION TIMEOUT. isTransitionSet()=");
                                                stringBuilder3.append(WindowManagerService.this.mAppTransition.isTransitionSet());
                                                stringBuilder3.append(" mOpeningApps.size()=");
                                                stringBuilder3.append(WindowManagerService.this.mOpeningApps.size());
                                                stringBuilder3.append(" mClosingApps.size()=");
                                                stringBuilder3.append(WindowManagerService.this.mClosingApps.size());
                                                Slog.w(str3, stringBuilder3.toString());
                                                WindowManagerService.this.mAppTransition.setTimeout();
                                                N = WindowManagerService.this.mOpeningApps.size();
                                                for (i3 = 0; i3 < N; i3++) {
                                                    AppWindowToken appToken = (AppWindowToken) WindowManagerService.this.mOpeningApps.valueAt(i3);
                                                    appToken.mPendingRelaunchCount = 0;
                                                    appToken.mFrozenBounds.clear();
                                                    appToken.mFrozenMergedConfig.clear();
                                                }
                                                WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                                            }
                                        } finally {
                                            while (true) {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case 14:
                                    Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                                    Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                                    Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                                    break;
                                case 15:
                                    synchronized (WindowManagerService.this.mWindowMap) {
                                        try {
                                            WindowManagerService.boostPriorityForLockedSection();
                                            if (!WindowManagerService.this.mAnimator.isAnimating() && !WindowManagerService.this.mAnimator.isAnimationScheduled()) {
                                                if (!WindowManagerService.this.mDisplayFrozen) {
                                                    break;
                                                }
                                            } else {
                                                sendEmptyMessageDelayed(15, 2000);
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                return;
                                            }
                                        } finally {
                                            while (true) {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case 16:
                                    WindowManagerService.this.performEnableScreen();
                                    break;
                                case 17:
                                    synchronized (WindowManagerService.this.mWindowMap) {
                                        try {
                                            WindowManagerService.boostPriorityForLockedSection();
                                            Slog.w(WindowManagerService.TAG, "App freeze timeout expired.");
                                            WindowManagerService.this.mWindowsFreezingScreen = 2;
                                            i2 = 0;
                                            i3 = WindowManagerService.this.mAppFreezeListeners.size() - 1;
                                            while (true) {
                                                N = i3;
                                                if (N >= 0) {
                                                    i2++;
                                                    ((AppFreezeListener) WindowManagerService.this.mAppFreezeListeners.get(N)).onAppFreezeTimeout();
                                                    i3 = N - 1;
                                                } else {
                                                    if (i2 == 0) {
                                                        Slog.e(WindowManagerService.TAG, "mAppFreezeListeners is empty ! so stopFreezingDisplayLocked");
                                                        WindowManagerService.this.stopFreezingDisplayLocked();
                                                    }
                                                }
                                            }
                                        } finally {
                                            while (true) {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case 18:
                                    removeMessages(18, msg.obj);
                                    i = ((Integer) msg.obj).intValue();
                                    if (WindowManagerService.this.mRoot.getDisplayContent(i) == null) {
                                        if (WindowManagerDebugConfig.DEBUG_CONFIGURATION) {
                                            str3 = WindowManagerService.TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Trying to send configuration to non-existing displayId=");
                                            stringBuilder3.append(i);
                                            Slog.w(str3, stringBuilder3.toString());
                                        }
                                        Slog.w(WindowManagerService.TAG, "non-existing display ,so reset mWaitingForConfig");
                                        if (WindowManagerService.this.mWaitingForConfig) {
                                            WindowManagerService.this.mWaitingForConfig = false;
                                            break;
                                        }
                                    }
                                    WindowManagerService.this.sendNewConfiguration(i);
                                    break;
                                    break;
                                case REPORT_WINDOWS_CHANGE /*19*/:
                                    if (WindowManagerService.this.mWindowsChanged) {
                                        synchronized (WindowManagerService.this.mWindowMap) {
                                            try {
                                                WindowManagerService.boostPriorityForLockedSection();
                                                WindowManagerService.this.mWindowsChanged = false;
                                            } finally {
                                                while (true) {
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    break;
                                                }
                                            }
                                        }
                                        WindowManagerService.this.notifyWindowsChanged();
                                        break;
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                                            WindowManagerService.this.notifyHardKeyboardStatusChange();
                                            break;
                                        case BOOT_TIMEOUT /*23*/:
                                            WindowManagerService.this.performBootTimeout();
                                            break;
                                        case 24:
                                            Runnable callback = null;
                                            synchronized (WindowManagerService.this.mWindowMap) {
                                                try {
                                                    WindowManagerService.boostPriorityForLockedSection();
                                                    StringBuilder stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("Timeout waiting for drawn: undrawn=");
                                                    stringBuilder4.append(WindowManagerService.this.mWaitingForDrawn);
                                                    Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder4.toString());
                                                    WindowManagerService.this.mWaitingForDrawn.clear();
                                                    callback = WindowManagerService.this.mWaitingForDrawnCallback;
                                                    WindowManagerService.this.mWaitingForDrawnCallback = null;
                                                } finally {
                                                    while (true) {
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                        break;
                                                    }
                                                }
                                            }
                                            if (callback != null) {
                                                callback.run();
                                                break;
                                            }
                                            break;
                                        case SHOW_STRICT_MODE_VIOLATION /*25*/:
                                            WindowManagerService.this.showStrictModeViolation(msg.arg1, msg.arg2);
                                            break;
                                        case DO_ANIMATION_CALLBACK /*26*/:
                                            try {
                                                ((IRemoteCallback) msg.obj).sendResult(null);
                                                break;
                                            } catch (RemoteException e) {
                                                break;
                                            }
                                        default:
                                            switch (i) {
                                                case 32:
                                                    try {
                                                        WindowManagerService.this.mActivityManager.notifyActivityDrawn((IBinder) msg.obj);
                                                        break;
                                                    } catch (RemoteException e2) {
                                                        break;
                                                    }
                                                case 33:
                                                    Runnable callback2;
                                                    if (WindowManagerService.this.isPrintAllWindowsDrawnLogs()) {
                                                        Flog.i(NativeResponseCode.SERVICE_FOUND, "ALL_WINDOWS_DRAWN timeout");
                                                    }
                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                        try {
                                                            WindowManagerService.boostPriorityForLockedSection();
                                                            callback2 = WindowManagerService.this.mWaitingForDrawnCallback;
                                                            WindowManagerService.this.mWaitingForDrawnCallback = null;
                                                        } finally {
                                                            while (true) {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (callback2 != null) {
                                                        callback2.run();
                                                        break;
                                                    }
                                                    break;
                                                case 34:
                                                    float scale = WindowManagerService.this.getCurrentAnimatorScale();
                                                    ValueAnimator.setDurationScale(scale);
                                                    Session session = msg.obj;
                                                    if (session == null) {
                                                        ArrayList<IWindowSessionCallback> callbacks = new ArrayList();
                                                        synchronized (WindowManagerService.this.mWindowMap) {
                                                            try {
                                                                WindowManagerService.boostPriorityForLockedSection();
                                                                for (i3 = 0; i3 < WindowManagerService.this.mSessions.size(); i3++) {
                                                                    callbacks.add(((Session) WindowManagerService.this.mSessions.valueAt(i3)).mCallback);
                                                                }
                                                            } finally {
                                                                while (true) {
                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        while (i2 < callbacks.size()) {
                                                            try {
                                                                ((IWindowSessionCallback) callbacks.get(i2)).onAnimatorScaleChanged(scale);
                                                            } catch (RemoteException e3) {
                                                            }
                                                            i2++;
                                                        }
                                                        break;
                                                    }
                                                    try {
                                                        session.mCallback.onAnimatorScaleChanged(scale);
                                                        break;
                                                    } catch (RemoteException e4) {
                                                        break;
                                                    }
                                                case 35:
                                                    WindowManagerService windowManagerService = WindowManagerService.this;
                                                    if (msg.arg1 != 1) {
                                                        z = false;
                                                    }
                                                    windowManagerService.showCircularMask(z);
                                                    break;
                                                case 36:
                                                    WindowManagerService.this.showEmulatorDisplayOverlay();
                                                    break;
                                                case 37:
                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                        try {
                                                            WindowManagerService.boostPriorityForLockedSection();
                                                            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                                                                Slog.i(WindowManagerService.TAG, "CHECK_IF_BOOT_ANIMATION_FINISHED:");
                                                            }
                                                            z = WindowManagerService.this.checkBootAnimationCompleteLocked();
                                                        } finally {
                                                            while (true) {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (z) {
                                                        WindowManagerService.this.performEnableScreen();
                                                        break;
                                                    }
                                                    break;
                                                case 38:
                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                        try {
                                                            WindowManagerService.boostPriorityForLockedSection();
                                                            WindowManagerService.this.mLastANRState = null;
                                                        } finally {
                                                            while (true) {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    WindowManagerService.this.mAmInternal.clearSavedANRState();
                                                    break;
                                                case 39:
                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                        try {
                                                            WindowManagerService.boostPriorityForLockedSection();
                                                            if (WindowManagerService.this.mRoot.mWallpaperController.processWallpaperDrawPendingTimeout()) {
                                                                WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                                                            }
                                                        } finally {
                                                            while (true) {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case WINDOW_REPLACEMENT_TIMEOUT /*46*/:
                                                            synchronized (WindowManagerService.this.mWindowMap) {
                                                                try {
                                                                    WindowManagerService.boostPriorityForLockedSection();
                                                                    i2 = WindowManagerService.this.mWindowReplacementTimeouts.size() - 1;
                                                                    while (true) {
                                                                        N = i2;
                                                                        if (N >= 0) {
                                                                            ((AppWindowToken) WindowManagerService.this.mWindowReplacementTimeouts.get(N)).onWindowReplacementTimeout();
                                                                            i2 = N - 1;
                                                                        } else {
                                                                            WindowManagerService.this.mWindowReplacementTimeouts.clear();
                                                                        }
                                                                    }
                                                                } finally {
                                                                    while (true) {
                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            break;
                                                        case 47:
                                                            WindowManagerService.this.mAmInternal.notifyAppTransitionStarting((SparseIntArray) msg.obj, msg.getWhen());
                                                            break;
                                                        case 48:
                                                            WindowManagerService.this.mAmInternal.notifyAppTransitionCancelled();
                                                            break;
                                                        case 49:
                                                            WindowManagerService.this.mAmInternal.notifyAppTransitionFinished();
                                                            break;
                                                        default:
                                                            ActivityManagerInternal activityManagerInternal;
                                                            switch (i) {
                                                                case 51:
                                                                    switch (msg.arg1) {
                                                                        case 0:
                                                                            WindowManagerService.this.mWindowAnimationScaleSetting = Global.getFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                                                                            break;
                                                                        case 1:
                                                                            WindowManagerService.this.mTransitionAnimationScaleSetting = Global.getFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                                                                            break;
                                                                        case 2:
                                                                            WindowManagerService.this.mAnimatorDurationScaleSetting = Global.getFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                                                                            WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                                                                            break;
                                                                    }
                                                                    break;
                                                                case 52:
                                                                    WindowState window = msg.obj;
                                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                                        try {
                                                                            WindowManagerService.boostPriorityForLockedSection();
                                                                            LayoutParams layoutParams = window.mAttrs;
                                                                            layoutParams.flags &= -129;
                                                                            window.hidePermanentlyLw();
                                                                            window.setDisplayLayoutNeeded();
                                                                            WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                                                                        } finally {
                                                                            while (true) {
                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    break;
                                                                case 53:
                                                                    activityManagerInternal = WindowManagerService.this.mAmInternal;
                                                                    if (msg.arg1 != 1) {
                                                                        z = false;
                                                                    }
                                                                    activityManagerInternal.notifyDockedStackMinimizedChanged(z);
                                                                    break;
                                                                case 54:
                                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                                        try {
                                                                            WindowManagerService.boostPriorityForLockedSection();
                                                                            WindowManagerService.this.getDefaultDisplayContentLocked().onSeamlessRotationTimeout();
                                                                        } finally {
                                                                            while (true) {
                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    break;
                                                                case 55:
                                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                                        try {
                                                                            WindowManagerService.boostPriorityForLockedSection();
                                                                            WindowManagerService.this.restorePointerIconLocked((DisplayContent) msg.obj, (float) msg.arg1, (float) msg.arg2);
                                                                        } finally {
                                                                            while (true) {
                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    break;
                                                                case 56:
                                                                    WindowManagerService.this.mAmInternal.notifyKeyguardFlagsChanged((Runnable) msg.obj);
                                                                    break;
                                                                case NOTIFY_KEYGUARD_TRUSTED_CHANGED /*57*/:
                                                                    WindowManagerService.this.mAmInternal.notifyKeyguardTrustedChanged();
                                                                    break;
                                                                case SET_HAS_OVERLAY_UI /*58*/:
                                                                    activityManagerInternal = WindowManagerService.this.mAmInternal;
                                                                    i3 = msg.arg1;
                                                                    if (msg.arg2 != 1) {
                                                                        z = false;
                                                                    }
                                                                    activityManagerInternal.setHasOverlayUi(i3, z);
                                                                    break;
                                                                case SET_RUNNING_REMOTE_ANIMATION /*59*/:
                                                                    activityManagerInternal = WindowManagerService.this.mAmInternal;
                                                                    i3 = msg.arg1;
                                                                    if (msg.arg2 != 1) {
                                                                        z = false;
                                                                    }
                                                                    activityManagerInternal.setRunningRemoteAnimation(i3, z);
                                                                    break;
                                                                case 60:
                                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                                        try {
                                                                            WindowManagerService.boostPriorityForLockedSection();
                                                                            if (WindowManagerService.this.mRecentsAnimationController != null) {
                                                                                WindowManagerService.this.mRecentsAnimationController.scheduleFailsafe();
                                                                            }
                                                                        } finally {
                                                                            while (true) {
                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    break;
                                                                case RECOMPUTE_FOCUS /*61*/:
                                                                    synchronized (WindowManagerService.this.mWindowMap) {
                                                                        try {
                                                                            WindowManagerService.boostPriorityForLockedSection();
                                                                            WindowManagerService.this.updateFocusedWindowLocked(0, true);
                                                                        } finally {
                                                                            while (true) {
                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    break;
                                                                default:
                                                                    switch (i) {
                                                                        case 102:
                                                                            Flog.w(307, "APP_TRANSITION_ANIMATIONS_SPECSFUTURE timeout");
                                                                            break;
                                                                        case 103:
                                                                            synchronized (WindowManagerService.this.mWindowMap) {
                                                                                try {
                                                                                    WindowManagerService.boostPriorityForLockedSection();
                                                                                    Slog.w(WindowManagerService.TAG, "stopFreezingDisplayLocked  by PC_FREEZE_TIMEOUT");
                                                                                    WindowManagerService.this.stopFreezingDisplayLocked();
                                                                                } finally {
                                                                                    while (true) {
                                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                                        break;
                                                                                    }
                                                                                }
                                                                            }
                                                                            break;
                                                                        case 104:
                                                                            try {
                                                                                WindowManagerService.this.mActivityManager.setFocusedTask(msg.arg1);
                                                                                break;
                                                                            } catch (RemoteException e5) {
                                                                                Log.e(WindowManagerService.TAG, "setFocusedDisplay()");
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
                } else {
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            DisplayContent displayContent = WindowManagerService.this.getDefaultDisplayContentLocked();
                            displayContent.getDockedDividerController().reevaluateVisibility(false);
                            displayContent.adjustForImeIfNeeded();
                        } catch (IllegalArgumentException e6) {
                            Log.e(WindowManagerService.TAG, "catch IllegalArgumentException ");
                        } catch (Throwable th) {
                            while (true) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } else {
                synchronized (WindowManagerService.this.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowManagerService.this.getDefaultDisplayContentLocked().onWindowFreezeTimeout();
                        if (HwPCUtils.isPcCastModeInServer()) {
                            HwPCUtils.log(WindowManagerService.TAG, "Window Freeze TimeOut try stopFreezingDisplayLocked");
                            WindowManagerService.this.stopFreezingDisplayLocked();
                        } else if (WindowManagerService.this.mDisplayFrozen) {
                            Slog.w(WindowManagerService.TAG, "freeze TimeOut, call stopFreezingDisplayLocked");
                            WindowManagerService.this.stopFreezingDisplayLocked();
                        }
                    } finally {
                        while (true) {
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        }
    }

    public class HwInnerWindowManagerService extends Stub {
        WindowManagerService mWMS;

        HwInnerWindowManagerService(WindowManagerService wms) {
            this.mWMS = wms;
        }

        public boolean isFullScreenDevice() {
            if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.isFullScreenDevice();
            }
            return false;
        }

        public float getDeviceMaxRatio() {
            if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.getDeviceMaxRatio();
            }
            return 0.0f;
        }

        public Rect getTopAppDisplayBounds(float appMaxRatioint, int rotation) {
            WindowState focusedWindow = WindowManagerService.this.getFocusedWindow();
            if (focusedWindow != null) {
                return focusedWindow.getBounds();
            }
            return null;
        }

        public void registerRotateObserver(IHwRotateObserver observer) {
            WindowManagerService.this.mPolicy.registerRotateObserver(observer);
        }

        public void unregisterRotateObserver(IHwRotateObserver observer) {
            WindowManagerService.this.mPolicy.unregisterRotateObserver(observer);
        }

        public List<String> getNotchSystemApps() {
            if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.getNotchSystemApps();
            }
            return null;
        }

        public int getAppUseNotchMode(String packageName) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.getAppUseNotchMode(packageName);
            }
            return -1;
        }

        private boolean checkPermission() {
            int uid = UserHandle.getAppId(Binder.getCallingUid());
            if (uid == 1000) {
                return true;
            }
            String str = WindowManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process Permission error! uid:");
            stringBuilder.append(uid);
            Slog.e(str, stringBuilder.toString());
            return false;
        }

        public boolean registerWMMonitorCallback(IHwWMDAMonitorCallback callback) {
            if (!checkPermission()) {
                return false;
            }
            WindowManagerService.this.mWMProxy.registerWMMonitorCallback(callback);
            return true;
        }

        public List<Bundle> getVisibleWindows(int ops) {
            if (checkPermission()) {
                return WindowManagerService.this.mHwWMSEx.getVisibleWindows(ops);
            }
            return null;
        }

        public int getFocusWindowWidth() {
            return WindowManagerService.this.mHwWMSEx.getFocusWindowWidth(WindowManagerService.this.mCurrentFocus, WindowManagerService.this.mInputMethodTarget);
        }

        public void startNotifyWindowFocusChange() {
            if (WindowManagerService.this.checkCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "startNotifyWindowFocusChange()")) {
                this.mWMS.mNotifyFocusedWindow = true;
            }
        }

        public void stopNotifyWindowFocusChange() {
            if (WindowManagerService.this.checkCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "stopNotifyWindowFocusChange()")) {
                this.mWMS.mNotifyFocusedWindow = false;
            }
        }

        public void getCurrFocusedWinInExtDisplay(Bundle outBundle) {
            if (checkPermission()) {
                WindowManagerService.this.mHwWMSEx.getCurrFocusedWinInExtDisplay(outBundle);
            }
        }

        public boolean hasLighterViewInPCCastMode() {
            if (checkPermission()) {
                return WindowManagerService.this.mHwWMSEx.hasLighterViewInPCCastMode();
            }
            return false;
        }

        public boolean shouldDropMotionEventForTouchPad(float x, float y) {
            if (checkPermission()) {
                return WindowManagerService.this.mHwWMSEx.shouldDropMotionEventForTouchPad(x, y);
            }
            return false;
        }

        public HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(boolean refresh) {
            return WindowManagerService.this.mHwWMSEx.getForegroundTaskSnapshotWrapper(WindowManagerService.this.mTaskSnapshotController, WindowManagerService.this.getFocusedWindow(), refresh);
        }
    }

    private static class MousePositionTracker implements PointerEventListener {
        private boolean mLatestEventWasMouse;
        private float mLatestMouseX;
        private float mLatestMouseY;

        private MousePositionTracker() {
        }

        /* synthetic */ MousePositionTracker(AnonymousClass1 x0) {
            this();
        }

        void updatePosition(float x, float y) {
            synchronized (this) {
                this.mLatestEventWasMouse = true;
                this.mLatestMouseX = x;
                this.mLatestMouseY = y;
            }
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                updatePosition(motionEvent.getRawX(), motionEvent.getRawY());
                return;
            }
            synchronized (this) {
                this.mLatestEventWasMouse = false;
            }
        }
    }

    class RotationWatcher {
        final DeathRecipient mDeathRecipient;
        final int mDisplayId;
        final IRotationWatcher mWatcher;

        RotationWatcher(IRotationWatcher watcher, DeathRecipient deathRecipient, int displayId) {
            this.mWatcher = watcher;
            this.mDeathRecipient = deathRecipient;
            this.mDisplayId = displayId;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri mAnimationDurationScaleUri = Global.getUriFor("animator_duration_scale");
        private final Uri mDisplayInversionEnabledUri = Secure.getUriFor("accessibility_display_inversion_enabled");
        private final Uri mTransitionAnimationScaleUri = Global.getUriFor("transition_animation_scale");
        private final Uri mWindowAnimationScaleUri = Global.getUriFor("window_animation_scale");

        public SettingsObserver() {
            super(new Handler());
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this, -1);
            resolver.registerContentObserver(this.mWindowAnimationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mTransitionAnimationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mAnimationDurationScaleUri, false, this, -1);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                if (this.mDisplayInversionEnabledUri.equals(uri)) {
                    WindowManagerService.this.updateCircularDisplayMaskIfNeeded();
                } else {
                    int mode;
                    if (this.mWindowAnimationScaleUri.equals(uri)) {
                        mode = 0;
                    } else if (this.mTransitionAnimationScaleUri.equals(uri)) {
                        mode = 1;
                    } else if (this.mAnimationDurationScaleUri.equals(uri)) {
                        mode = 2;
                    } else {
                        return;
                    }
                    WindowManagerService.this.mH.sendMessage(WindowManagerService.this.mH.obtainMessage(51, mode, 0));
                }
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface UpdateAnimationScaleMode {
    }

    public interface WindowChangeListener {
        void focusChanged();

        void windowsChanged();
    }

    private final class LocalService extends WindowManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(WindowManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void requestTraversalFromDisplayManager() {
            WindowManagerService.this.requestTraversal();
        }

        public void setMagnificationSpec(MagnificationSpec spec) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.setMagnificationSpecLocked(spec);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (Binder.getCallingPid() != Process.myPid()) {
                spec.recycle();
            }
        }

        public void setForceShowMagnifiableBounds(boolean show) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.setForceShowMagnifiableBoundsLocked(show);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void getMagnificationRegion(Region magnificationRegion) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.getMagnificationRegionLocked(magnificationRegion);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public MagnificationSpec getCompatibleMagnificationSpecForWindow(IBinder windowToken) {
            MagnificationSpec magnificationSpec;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) WindowManagerService.this.mWindowMap.get(windowToken);
                    magnificationSpec = null;
                    if (windowState == null) {
                    } else {
                        MagnificationSpec spec = null;
                        if (WindowManagerService.this.mAccessibilityController != null) {
                            spec = WindowManagerService.this.mAccessibilityController.getMagnificationSpecForWindowLocked(windowState);
                        }
                        if ((spec == null || spec.isNop()) && windowState.mGlobalScale == 1.0f) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        }
                        magnificationSpec = spec == null ? MagnificationSpec.obtain() : MagnificationSpec.obtain(spec);
                        magnificationSpec.scale *= windowState.mGlobalScale;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return magnificationSpec;
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return magnificationSpec;
        }

        public void setMagnificationCallbacks(MagnificationCallbacks callbacks) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController == null) {
                        WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                    }
                    WindowManagerService.this.mAccessibilityController.setMagnificationCallbacksLocked(callbacks);
                    if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                        WindowManagerService.this.mAccessibilityController = null;
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void setWindowsForAccessibilityCallback(WindowsForAccessibilityCallback callback) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController == null) {
                        WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                    }
                    WindowManagerService.this.mAccessibilityController.setWindowsForAccessibilityCallback(callback);
                    if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                        WindowManagerService.this.mAccessibilityController = null;
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void setInputFilter(IInputFilter filter) {
            WindowManagerService.this.mInputManager.setInputFilter(filter);
        }

        public IBinder getFocusedWindowToken() {
            IBinder asBinder;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.getFocusedWindowLocked();
                    if (windowState != null) {
                        asBinder = windowState.mClient.asBinder();
                    } else {
                        asBinder = null;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return asBinder;
        }

        public boolean isCoverOpen() {
            return WindowManagerService.this.isCoverOpen();
        }

        public boolean isKeyguardLocked() {
            return WindowManagerService.this.isKeyguardLocked();
        }

        public boolean isKeyguardShowingAndNotOccluded() {
            return WindowManagerService.this.isKeyguardShowingAndNotOccluded();
        }

        public void showGlobalActions() {
            WindowManagerService.this.showGlobalActions();
        }

        public void getWindowFrame(IBinder token, Rect outBounds) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) WindowManagerService.this.mWindowMap.get(token);
                    if (windowState != null) {
                        outBounds.set(windowState.mFrame);
                    } else {
                        outBounds.setEmpty();
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void waitForAllWindowsDrawn(Runnable callback, long timeout) {
            boolean allWindowsDrawn = false;
            WindowManagerService.this.mWaitAllWindowDrawStartTime = SystemClock.elapsedRealtime();
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mWaitingForDrawnCallback = callback;
                    WindowManagerService.this.getDefaultDisplayContentLocked().waitForAllWindowsDrawn();
                    WindowManagerService.this.mWindowPlacerLocked.requestTraversal();
                    WindowManagerService.this.mH.removeMessages(24);
                    if (WindowManagerService.this.mWaitingForDrawn.isEmpty()) {
                        allWindowsDrawn = true;
                    } else {
                        WindowManagerService.this.mH.sendEmptyMessageDelayed(24, timeout);
                        WindowManagerService.this.checkDrawnWindowsLocked();
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (allWindowsDrawn) {
                callback.run();
            }
        }

        public void addWindowToken(IBinder token, int type, int displayId) {
            WindowManagerService.this.addWindowToken(token, type, displayId);
        }

        public void removeWindowToken(IBinder binder, boolean removeWindows, int displayId) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (removeWindows) {
                        DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                        if (dc == null) {
                            String str = WindowManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("removeWindowToken: Attempted to remove token: ");
                            stringBuilder.append(binder);
                            stringBuilder.append(" for non-exiting displayId=");
                            stringBuilder.append(displayId);
                            Slog.w(str, stringBuilder.toString());
                        } else {
                            WindowToken token = dc.removeWindowToken(binder);
                            if (token == null) {
                                String str2 = WindowManagerService.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("removeWindowToken: Attempted to remove non-existing token: ");
                                stringBuilder2.append(binder);
                                Slog.w(str2, stringBuilder2.toString());
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            token.removeAllWindowsIfPossible();
                        }
                    }
                    WindowManagerService.this.removeWindowToken(binder, displayId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void registerAppTransitionListener(AppTransitionListener listener) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mAppTransition.registerListenerLocked(listener);
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public int getInputMethodWindowVisibleHeight() {
            int inputMethodWindowVisibleHeight;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    inputMethodWindowVisibleHeight = WindowManagerService.this.getDefaultDisplayContentLocked().mDisplayFrames.getInputMethodWindowVisibleHeight();
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return inputMethodWindowVisibleHeight;
        }

        public void saveLastInputMethodWindowForTransition() {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mInputMethodWindow != null) {
                        WindowManagerService.this.mPolicy.setLastInputMethodWindowLw(WindowManagerService.this.mInputMethodWindow, WindowManagerService.this.mInputMethodTarget);
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void clearLastInputMethodWindowForTransition() {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mPolicy.setLastInputMethodWindowLw(null, null);
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void updateInputMethodWindowStatus(IBinder imeToken, boolean imeWindowVisible, boolean dismissImeOnBackKeyPressed, IBinder targetWindowToken) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                String str = WindowManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateInputMethodWindowStatus: imeToken=");
                stringBuilder.append(imeToken);
                stringBuilder.append(" dismissImeOnBackKeyPressed=");
                stringBuilder.append(dismissImeOnBackKeyPressed);
                stringBuilder.append(" imeWindowVisible=");
                stringBuilder.append(imeWindowVisible);
                stringBuilder.append(" targetWindowToken=");
                stringBuilder.append(targetWindowToken);
                Slog.w(str, stringBuilder.toString());
            }
            WindowManagerService.this.mPolicy.setDismissImeOnBackKeyPressed(dismissImeOnBackKeyPressed);
        }

        public boolean isHardKeyboardAvailable() {
            boolean z;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = WindowManagerService.this.mHardKeyboardAvailable;
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return z;
        }

        public void setOnHardKeyboardStatusChangeListener(OnHardKeyboardStatusChangeListener listener) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mHardKeyboardStatusChangeListener = listener;
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public boolean isStackVisible(int windowingMode) {
            boolean isStackVisible;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    isStackVisible = WindowManagerService.this.getDefaultDisplayContentLocked().isStackVisible(windowingMode);
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return isStackVisible;
        }

        public boolean isDockedDividerResizing() {
            boolean isResizing;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    isResizing = WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().isResizing();
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return isResizing;
        }

        public void computeWindowsForAccessibility() {
            AccessibilityController accessibilityController;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    accessibilityController = WindowManagerService.this.mAccessibilityController;
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (accessibilityController != null) {
                accessibilityController.performComputeChangedWindowsNotLocked();
            }
        }

        public void setVr2dDisplayId(int vr2dDisplayId) {
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                String str = WindowManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setVr2dDisplayId called for: ");
                stringBuilder.append(vr2dDisplayId);
                Slog.d(str, stringBuilder.toString());
            }
            synchronized (WindowManagerService.this) {
                WindowManagerService.this.mVr2dDisplayId = vr2dDisplayId;
            }
        }

        public void registerDragDropControllerCallback(IDragDropCallback callback) {
            WindowManagerService.this.mDragDropController.registerCallback(callback);
        }

        public void lockNow() {
            WindowManagerService.this.lockNow(null);
        }

        public int getWindowOwnerUserId(IBinder token) {
            int userId;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState window = (WindowState) WindowManagerService.this.mWindowMap.get(token);
                    if (window != null) {
                        userId = UserHandle.getUserId(window.mOwnerUid);
                    } else {
                        userId = -10000;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return -10000;
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return userId;
        }

        public void waitForKeyguardDismissDone(Runnable callback, long timeout) {
            Slog.i(WindowManagerService.TAG, "fingerunlock--waitForKeyguardDismissDone there is no keyguard.");
            callback.run();
        }

        public void setDockedStackDividerRotation(int rotation) {
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().setDockedStackDividerRotation(rotation);
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public int getFocusedDisplayId() {
            return WindowManagerService.this.getFocusedDisplayId();
        }

        public void setFocusedDisplayId(int displayId, String reason) {
            WindowManagerService.this.setFocusedDisplay(displayId, true, reason);
        }

        public boolean isMinimizedDock() {
            boolean isMinimizedDock;
            synchronized (WindowManagerService.this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    isMinimizedDock = WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().isMinimizedDock();
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return isMinimizedDock;
        }

        public String getFullStackTopWindow() {
            TaskStack stack = null;
            try {
                stack = WindowManagerService.this.mRoot.getStack(4, 1);
            } catch (IndexOutOfBoundsException e) {
                Log.i(WindowManagerService.TAG, "not find secondary stack");
            }
            if (stack == null || stack.getTopChild() == null || ((Task) stack.getTopChild()).getTopFullscreenAppToken() == null) {
                return null;
            }
            return ((Task) stack.getTopChild()).getTopFullscreenAppToken().appComponentName;
        }
    }

    AppWindowToken getTopOpeningApp() {
        int topOpeningLayer = 0;
        AppWindowToken topOpeningApp = null;
        int appsCount = this.mOpeningApps.size();
        for (int i = 0; i < appsCount; i++) {
            AppWindowToken wtoken = (AppWindowToken) this.mOpeningApps.valueAt(i);
            int layer = wtoken.getHighestAnimLayer();
            if (topOpeningApp == null || layer > topOpeningLayer) {
                topOpeningApp = wtoken;
                topOpeningLayer = layer;
            }
        }
        return topOpeningApp;
    }

    boolean isSupportHwAppExitAnim(AppWindowToken aToken) {
        if (aToken == null) {
            Slog.w(TAG, "check app support hw app exit animation failed!");
            return false;
        }
        String aTokenStr = aToken.toString();
        if (aTokenStr == null) {
            Slog.w(TAG, "check app support hw app exit animation failed!");
            return false;
        }
        for (CharSequence contains : DISABLE_HW_LAUNCHER_EXIT_ANIM_CMP_LIST) {
            if (aTokenStr.contains(contains)) {
                return false;
            }
        }
        return true;
    }

    int getDragLayerLocked() {
        return (this.mPolicy.getWindowLayerFromTypeLw(2016) * 10000) + 1000;
    }

    static void boostPriorityForLockedSection() {
        sThreadPriorityBooster.boost();
    }

    static void resetPriorityAfterLockedSection() {
        sThreadPriorityBooster.reset();
    }

    void openSurfaceTransaction() {
        try {
            Trace.traceBegin(32, "openSurfaceTransaction");
            synchronized (this.mWindowMap) {
                boostPriorityForLockedSection();
                SurfaceControl.openTransaction();
            }
            resetPriorityAfterLockedSection();
            Trace.traceEnd(32);
        } catch (Throwable th) {
            Trace.traceEnd(32);
        }
    }

    void closeSurfaceTransaction(String where) {
        try {
            Trace.traceBegin(32, "closeSurfaceTransaction");
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    traceStateLocked(where);
                    SurfaceControl.closeTransaction();
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Trace.traceEnd(32);
        }
    }

    static WindowManagerService getInstance() {
        return sInstance;
    }

    public static WindowManagerService main(Context context, InputManagerService im, boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy) {
        WindowManagerService[] holder = new WindowManagerService[1];
        DisplayThread.getHandler().runWithScissors(new -$$Lambda$WindowManagerService$qOaUiWHWefHk1N5K-T4WND2mknQ(context, im, haveInputMethods, showBootMsgs, onlyCore, policy), 0);
        return sInstance;
    }

    static /* synthetic */ void lambda$main$0(Context context, InputManagerService im, boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy) {
        IHwWindowManagerService iwms = HwServiceFactory.getHuaweiWindowManagerService();
        if (iwms != null) {
            sInstance = iwms.getInstance(context, im, haveInputMethods, showBootMsgs, onlyCore, policy);
        } else {
            sInstance = new WindowManagerService(context, im, haveInputMethods, showBootMsgs, onlyCore, policy);
        }
    }

    private void initPolicy() {
        UiThread.getHandler().runWithScissors(new Runnable() {
            public void run() {
                WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
                WindowManagerService.this.mPolicy.init(WindowManagerService.this.mContext, WindowManagerService.this, WindowManagerService.this);
            }
        }, 0);
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
        new WindowManagerShellCommand(this).exec(this, in, out, err, args, callback, result);
    }

    protected WindowManagerService(Context context, InputManagerService inputManager, boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy) {
        InputChannel inputChannel;
        Context context2 = context;
        this.mFingerHandlerThread.start();
        this.mFingerUnlockHandler = new FingerUnlockHandler(this.mFingerHandlerThread.getLooper());
        this.mHwWMSEx = HwServiceExFactory.getHwWindowManagerServiceEx(this, context);
        LockGuard.installLock((Object) this, 5);
        this.mContext = context2;
        this.mHaveInputMethods = haveInputMethods;
        this.mAllowBootMessages = showBootMsgs;
        this.mOnlyCore = onlyCore;
        this.mLimitedAlphaCompositing = context.getResources().getBoolean(17957015);
        this.mHasPermanentDpad = context.getResources().getBoolean(17956981);
        this.mInTouchMode = context.getResources().getBoolean(17956919);
        this.mDrawLockTimeoutMillis = (long) context.getResources().getInteger(17694782);
        this.mAllowAnimationsInLowPowerMode = context.getResources().getBoolean(17956871);
        this.mMaxUiWidth = context.getResources().getInteger(17694812);
        this.mDisableTransitionAnimation = context.getResources().getBoolean(17956930);
        this.mInputManager = inputManager;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplaySettings = new DisplaySettings();
        this.mDisplaySettings.readSettingsLocked();
        this.mPolicy = policy;
        this.mAnimator = new WindowAnimator(this);
        this.mRoot = new RootWindowContainer(this);
        this.mWindowPlacerLocked = new WindowSurfacePlacer(this);
        this.mTaskSnapshotController = new TaskSnapshotController(this);
        this.mWindowTracing = WindowTracing.createDefaultAndStartLooper(context);
        LocalServices.addService(WindowManagerPolicy.class, this.mPolicy);
        if (this.mInputManager != null) {
            inputChannel = this.mInputManager.monitorInput(TAG);
            this.mPointerEventDispatcher = inputChannel != null ? new PointerEventDispatcher(inputChannel) : null;
        } else {
            this.mPointerEventDispatcher = null;
        }
        if (this.mInputManager != null) {
            inputChannel = this.mInputManager.monitorInput("ExternalPCChannel");
            this.mExternalPointerEventDispatcher = inputChannel != null ? new PointerEventDispatcher(inputChannel) : null;
        } else {
            this.mExternalPointerEventDispatcher = null;
        }
        this.mDisplayManager = (DisplayManager) context2.getSystemService("display");
        this.mKeyguardDisableHandler = new KeyguardDisableHandler(this.mContext, this.mPolicy);
        this.mPowerManager = (PowerManager) context2.getSystemService("power");
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        if (this.mPowerManagerInternal != null) {
            this.mPowerManagerInternal.registerLowPowerModeObserver(new LowPowerModeListener() {
                public int getServiceType() {
                    return 3;
                }

                public void onLowPowerModeChanged(PowerSaveState result) {
                    synchronized (WindowManagerService.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            boolean enabled = result.batterySaverEnabled;
                            if (!(WindowManagerService.this.mAnimationsDisabled == enabled || WindowManagerService.this.mAllowAnimationsInLowPowerMode)) {
                                WindowManagerService.this.mAnimationsDisabled = enabled;
                                WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            });
            this.mAnimationsDisabled = this.mPowerManagerInternal.getLowPowerState(3).batterySaverEnabled;
        }
        this.mScreenFrozenLock = this.mPowerManager.newWakeLock(1, "SCREEN_FROZEN");
        this.mScreenFrozenLock.setReferenceCounted(false);
        this.mAppTransition = new AppTransition(context2, this);
        this.mAppTransition.registerListenerLocked(this.mActivityManagerAppTransitionNotifier);
        AnimationHandler animationHandler = new AnimationHandler();
        animationHandler.setProvider(new SfVsyncFrameCallbackProvider());
        this.mBoundsAnimationController = new BoundsAnimationController(context2, this.mAppTransition, AnimationThread.getHandler(), animationHandler);
        this.mActivityManager = ActivityManager.getService();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAppOps = (AppOpsManager) context2.getSystemService("appops");
        OnOpChangedInternalListener opListener = new OnOpChangedInternalListener() {
            public void onOpChanged(int op, String packageName) {
                if (WindowManagerService.this.mHwWMSEx != null) {
                    WindowManagerService.this.mHwWMSEx.sendUpdateAppOpsState();
                }
                WindowManagerService.this.mHwWMSEx.updateAppOpsStateReport(op, packageName);
            }
        };
        this.mAppOps.startWatchingMode(24, null, opListener);
        this.mAppOps.startWatchingMode(45, null, opListener);
        this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        IntentFilter suspendPackagesFilter = new IntentFilter();
        suspendPackagesFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendPackagesFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        context2.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String[] affectedPackages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                WindowManagerService.this.updateHiddenWhileSuspendedState(new ArraySet(Arrays.asList(affectedPackages)), "android.intent.action.PACKAGES_SUSPENDED".equals(intent.getAction()));
            }
        }, UserHandle.ALL, suspendPackagesFilter, null, null);
        this.mWindowAnimationScaleSetting = Global.getFloat(context.getContentResolver(), "window_animation_scale", this.mWindowAnimationScaleSetting);
        this.mTransitionAnimationScaleSetting = Global.getFloat(context.getContentResolver(), "transition_animation_scale", context.getResources().getFloat(17104958));
        setAnimatorDurationScale(Global.getFloat(context.getContentResolver(), "animator_duration_scale", this.mAnimatorDurationScaleSetting));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mLatencyTracker = LatencyTracker.getInstance(context);
        this.mSettingsObserver = new SettingsObserver();
        this.mHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870922, TAG);
        this.mHoldingScreenWakeLock.setReferenceCounted(false);
        this.mPCHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870918, TAG);
        this.mPCHoldingScreenWakeLock.setReferenceCounted(false);
        this.mSurfaceAnimationRunner = new SurfaceAnimationRunner();
        this.mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(17956886);
        this.mTaskPositioningController = new TaskPositioningController(this, this.mInputManager, this.mInputMonitor, this.mActivityManager, this.mH.getLooper());
        this.mDragDropController = new DragDropController(this, this.mH.getLooper());
        LocalServices.addService(WindowManagerInternal.class, new LocalService(this, null));
    }

    public void onInitReady() {
        initPolicy();
        Watchdog.getInstance().addMonitor(this);
        IZrHung iZrHung = HwFrameworkFactory.getZrHung("appeye_frameworkblock");
        if (iZrHung != null) {
            ZrHungData data = new ZrHungData();
            data.put("monitor", this);
            iZrHung.check(data);
        }
        openSurfaceTransaction();
        try {
            createWatermarkInTransaction();
            showEmulatorDisplayOverlayIfNeeded();
        } finally {
            closeSurfaceTransaction("createWatermarkInTransaction");
        }
    }

    public InputMonitor getInputMonitor() {
        return this.mInputMonitor;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Window Manager Crash", e);
            }
            throw e;
        }
    }

    static boolean excludeWindowTypeFromTapOutTask(int windowType) {
        if (windowType == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || windowType == 2012 || windowType == 2019) {
            return true;
        }
        return false;
    }

    static boolean excludeWindowsFromTapOutTask(WindowState win) {
        LayoutParams attrs = win == null ? null : win.getAttrs();
        if (attrs == null) {
            return false;
        }
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(win.getDisplayId()) && "com.huawei.desktop.systemui".equals(attrs.packageName)) {
            return true;
        }
        if (attrs.type == 1000 && "com.baidu.input_huawei".equals(attrs.packageName) && (!HwPCUtils.isPcCastModeInServer() || HwPCUtils.enabledInPad())) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:386:0x0725 A:{Catch:{ all -> 0x06ce }} */
    /* JADX WARNING: Removed duplicated region for block: B:412:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:389:0x072e A:{SYNTHETIC, Splitter: B:389:0x072e} */
    /* JADX WARNING: Removed duplicated region for block: B:417:0x0784 A:{SYNTHETIC, Splitter: B:417:0x0784} */
    /* JADX WARNING: Removed duplicated region for block: B:422:0x078f A:{SYNTHETIC, Splitter: B:422:0x078f} */
    /* JADX WARNING: Removed duplicated region for block: B:442:0x07f8 A:{Catch:{ all -> 0x076a }} */
    /* JADX WARNING: Removed duplicated region for block: B:439:0x07f0  */
    /* JADX WARNING: Removed duplicated region for block: B:473:0x0853  */
    /* JADX WARNING: Removed duplicated region for block: B:463:0x0836 A:{SYNTHETIC, Splitter: B:463:0x0836} */
    /* JADX WARNING: Removed duplicated region for block: B:484:0x0872  */
    /* JADX WARNING: Removed duplicated region for block: B:477:0x0860  */
    /* JADX WARNING: Removed duplicated region for block: B:497:0x08c5 A:{Catch:{ all -> 0x0988 }} */
    /* JADX WARNING: Removed duplicated region for block: B:500:0x08cd A:{Catch:{ all -> 0x0988 }} */
    /* JADX WARNING: Removed duplicated region for block: B:511:0x08fb A:{Catch:{ all -> 0x0988 }} */
    /* JADX WARNING: Removed duplicated region for block: B:508:0x08ed A:{Catch:{ all -> 0x0988 }} */
    /* JADX WARNING: Removed duplicated region for block: B:527:0x094e A:{Catch:{ all -> 0x0988 }} */
    /* JADX WARNING: Removed duplicated region for block: B:522:0x0921 A:{Catch:{ all -> 0x0988 }} */
    /* JADX WARNING: Removed duplicated region for block: B:536:0x0981  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x06d7 A:{SYNTHETIC, Splitter: B:358:0x06d7} */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x06a9 A:{SYNTHETIC, Splitter: B:351:0x06a9} */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x06a9 A:{SYNTHETIC, Splitter: B:351:0x06a9} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x06d7 A:{SYNTHETIC, Splitter: B:358:0x06d7} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x06d7 A:{SYNTHETIC, Splitter: B:358:0x06d7} */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x06a9 A:{SYNTHETIC, Splitter: B:351:0x06a9} */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x06a9 A:{SYNTHETIC, Splitter: B:351:0x06a9} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x06d7 A:{SYNTHETIC, Splitter: B:358:0x06d7} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x06d7 A:{SYNTHETIC, Splitter: B:358:0x06d7} */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x06a9 A:{SYNTHETIC, Splitter: B:351:0x06a9} */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x06a9 A:{SYNTHETIC, Splitter: B:351:0x06a9} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x06d7 A:{SYNTHETIC, Splitter: B:358:0x06d7} */
    /* JADX WARNING: Missing block: B:406:0x0756, code:
            if (r14.mCurrentFocus.mOwnerUid == r8) goto L_0x077e;
     */
    /* JADX WARNING: Missing block: B:424:0x0793, code:
            if (excludeWindowsFromTapOutTask(r1) != false) goto L_0x0795;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int addWindow(Session session, IWindow client, int seq, LayoutParams attrs, int viewVisibility, int displayId, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, ParcelableWrapper outDisplayCutout, InputChannel outInputChannel) {
        Throwable displayContent;
        WindowHashMap windowHashMap;
        int i;
        WindowState parentWindow;
        int callingUid;
        int type;
        WindowState parentWindow2 = session;
        LayoutParams layoutParams = attrs;
        int i2 = displayId;
        InputChannel inputChannel = outInputChannel;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_ADDWINDOW, new Object[]{layoutParams});
        int[] appOp = new int[1];
        int res = this.mPolicy.checkAddPermission(layoutParams, appOp);
        if (res != 0) {
            return res;
        }
        int forceCompatMode;
        String str;
        boolean reportNewConfig = false;
        WindowContainer parentWindow3 = null;
        int callingUid2 = Binder.getCallingUid();
        int type2 = layoutParams.type;
        if (layoutParams.type >= 1000 && layoutParams.type <= 1999) {
            forceCompatMode = -3;
        } else if (layoutParams.packageName == null) {
            forceCompatMode = -3;
        } else if ((layoutParams.privateFlags & 128) != 0) {
            forceCompatMode = 1;
        } else {
            forceCompatMode = 0;
        }
        int forceCompatMode2 = forceCompatMode;
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("!!!!!!! addWindow: pkg:");
            stringBuilder.append(layoutParams.packageName);
            stringBuilder.append(" forceCompatMode:");
            stringBuilder.append(forceCompatMode2);
            Slog.e(str, stringBuilder.toString());
        }
        WindowHashMap windowHashMap2 = this.mWindowMap;
        synchronized (windowHashMap2) {
            int i3;
            int i4;
            int i5;
            int[] iArr;
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayReady) {
                    DisplayContent displayContent2 = getDisplayContentOrCreate(i2);
                    String str2;
                    StringBuilder stringBuilder2;
                    if (displayContent2 == null) {
                        try {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add window to a display that does not exist: ");
                            stringBuilder2.append(i2);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(str2, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -9;
                        } catch (Throwable th) {
                            displayContent = th;
                            windowHashMap = windowHashMap2;
                            i3 = i2;
                            resetPriorityAfterLockedSection();
                            throw displayContent;
                        }
                    }
                    if (!displayContent2.hasAccess(parentWindow2.mUid)) {
                        if (!this.mDisplayManagerInternal.isUidPresentOnDisplay(parentWindow2.mUid, i2)) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add window to a display for which the application does not have access: ");
                            stringBuilder2.append(i2);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(str2, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -9;
                        }
                    }
                    if (this.mWindowMap.containsKey(client.asBinder())) {
                        try {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Window ");
                            stringBuilder2.append(client);
                            stringBuilder2.append(" is already added");
                            Slog.w(str2, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -5;
                        } catch (Throwable th2) {
                            displayContent = th2;
                            IWindow iWindow = client;
                            windowHashMap = windowHashMap2;
                            i3 = i2;
                            resetPriorityAfterLockedSection();
                            throw displayContent;
                        }
                    }
                    WindowToken token;
                    WindowToken token2;
                    Object obj;
                    WindowState parentWindow4;
                    AppWindowToken atoken;
                    Session parentWindow5;
                    IBinder iBinder;
                    WindowState windowState;
                    Session session2;
                    IWindow iWindow2;
                    boolean z;
                    int i6;
                    Object obj2;
                    AppWindowToken atoken2;
                    WindowToken token3;
                    IBinder iBinder2;
                    int type3;
                    LayoutParams layoutParams2;
                    int type4 = client;
                    if (type2 >= 1000 && type2 <= 1999) {
                        parentWindow3 = windowForClientLocked(null, layoutParams.token, false);
                        if (parentWindow3 == null) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add window with token that is not a window: ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(str2, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -2;
                        } else if (parentWindow3.mAttrs.type >= 1000 && parentWindow3.mAttrs.type <= 1999) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add window with token that is a sub-window: ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(str2, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -2;
                        }
                    }
                    WindowContainer parentWindow6 = parentWindow3;
                    if (type2 == 2030) {
                        try {
                            if (!displayContent2.isPrivate()) {
                                Slog.w(TAG, "Attempted to add private presentation window to a non-private display.  Aborting.");
                                resetPriorityAfterLockedSection();
                                return -8;
                            }
                        } catch (Throwable th3) {
                            displayContent = th3;
                            parentWindow3 = parentWindow6;
                        }
                    }
                    boolean hasParent = parentWindow6 != null;
                    if (hasParent) {
                        token = parentWindow6.mAttrs.token;
                    } else {
                        try {
                            token = layoutParams.token;
                        } catch (Throwable th4) {
                            displayContent = th4;
                            WindowContainer windowContainer = parentWindow6;
                            i4 = forceCompatMode2;
                            i = type2;
                            i5 = callingUid2;
                            iArr = appOp;
                            windowHashMap = windowHashMap2;
                            parentWindow = i2;
                            parentWindow3 = windowContainer;
                            resetPriorityAfterLockedSection();
                            throw displayContent;
                        }
                    }
                    token = displayContent2.getWindowToken(token);
                    if (token == null) {
                        if ("com.google.android.marvin.talkback".equals(layoutParams.packageName) && HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                            DisplayContent defaultDisplayContent = this.mRoot.getDisplayContent(0);
                            if (defaultDisplayContent != null) {
                                token = defaultDisplayContent.getWindowToken(hasParent ? parentWindow6.mAttrs.token : layoutParams.token);
                            }
                        }
                    }
                    i5 = hasParent ? parentWindow6.mAttrs.type : type2;
                    boolean addToastWindowRequiresToken = false;
                    int callingUid3;
                    int forceCompatMode3;
                    if (token != null) {
                        token2 = token;
                        parentWindow2 = parentWindow6;
                        i4 = forceCompatMode2;
                        type4 = type2;
                        callingUid3 = callingUid2;
                        iArr = appOp;
                        i2 = i5;
                        obj = 1;
                        if (i2 < 1 || i2 > 99) {
                            String str3;
                            StringBuilder stringBuilder3;
                            if (i2 == 2011) {
                                if (token2.windowType != 2011) {
                                    str3 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Attempted to add input method window with bad token ");
                                    stringBuilder3.append(layoutParams.token);
                                    stringBuilder3.append(".  Aborting.");
                                    Slog.w(str3, stringBuilder3.toString());
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else if (i2 != 2031) {
                                int i7;
                                if (i2 == 2013) {
                                    if (token2.windowType != 2013) {
                                        str3 = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Attempted to add wallpaper window with bad token ");
                                        stringBuilder3.append(layoutParams.token);
                                        stringBuilder3.append(".  Aborting.");
                                        Slog.w(str3, stringBuilder3.toString());
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    }
                                } else if (i2 == 2023) {
                                    if (token2.windowType != 2023) {
                                        str3 = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Attempted to add Dream window with bad token ");
                                        stringBuilder3.append(layoutParams.token);
                                        stringBuilder3.append(".  Aborting.");
                                        Slog.w(str3, stringBuilder3.toString());
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    }
                                } else if (i2 == 2032) {
                                    if (token2.windowType != 2032) {
                                        str3 = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Attempted to add Accessibility overlay window with bad token ");
                                        stringBuilder3.append(layoutParams.token);
                                        stringBuilder3.append(".  Aborting.");
                                        Slog.w(str3, stringBuilder3.toString());
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    }
                                } else if (type4 == 2005) {
                                    try {
                                        type2 = callingUid3;
                                        try {
                                            addToastWindowRequiresToken = doesAddToastWindowRequireToken(layoutParams.packageName, type2, parentWindow2);
                                            if (!addToastWindowRequiresToken || token2.windowType == 2005) {
                                                callingUid = type2;
                                                i7 = 2013;
                                                parentWindow4 = parentWindow2;
                                                atoken = null;
                                                parentWindow5 = session;
                                                iBinder = null;
                                                windowState = windowState;
                                                session2 = parentWindow5;
                                                iWindow2 = client;
                                                z = parentWindow5.mCanAddInternalSystemWindow;
                                                type2 = token2;
                                                i6 = parentWindow5.mUid;
                                                obj2 = obj;
                                                atoken2 = atoken;
                                                windowHashMap = windowHashMap2;
                                                token3 = token2;
                                                type = type4;
                                                iBinder2 = iBinder;
                                                type3 = outInputChannel;
                                                layoutParams2 = layoutParams;
                                                windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                                                if (windowState.mDeathRecipient == null) {
                                                    try {
                                                        str2 = TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Adding window client ");
                                                        stringBuilder2.append(client.asBinder());
                                                        stringBuilder2.append(" that is dead, aborting.");
                                                        Slog.w(str2, stringBuilder2.toString());
                                                        resetPriorityAfterLockedSection();
                                                        return -4;
                                                    } catch (Throwable th5) {
                                                        displayContent = th5;
                                                        i5 = callingUid;
                                                        i = type;
                                                        i3 = displayId;
                                                        resetPriorityAfterLockedSection();
                                                        throw displayContent;
                                                    }
                                                } else if (windowState.getDisplayContent() == null) {
                                                    Slog.w(TAG, "Adding window to Display that has been removed.");
                                                    resetPriorityAfterLockedSection();
                                                    return -9;
                                                } else {
                                                    boolean hasStatusBarServicePermission = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") == 0;
                                                    this.mPolicy.adjustWindowParamsLw(windowState, windowState.mAttrs, hasStatusBarServicePermission);
                                                    windowState.setShowToOwnerOnlyLocked(this.mPolicy.checkShowToOwnerOnly(layoutParams2));
                                                    res = this.mPolicy.prepareAddWindowLw(windowState, layoutParams2);
                                                    if (res != 0) {
                                                        resetPriorityAfterLockedSection();
                                                        return res;
                                                    }
                                                    boolean type5;
                                                    WindowStateAnimator winAnimator;
                                                    AppWindowToken atoken3;
                                                    boolean openInputChannels;
                                                    Object obj3;
                                                    Rect taskBounds;
                                                    boolean focusChanged;
                                                    LayoutParams layoutParams3 = layoutParams2;
                                                    InputChannel inputChannel2 = outInputChannel;
                                                    if (inputChannel2 != null) {
                                                        if ((layoutParams3.inputFeatures & 2) == 0) {
                                                            boolean imMayMove;
                                                            type5 = true;
                                                            if (type5) {
                                                                windowState.openInputChannel(inputChannel2);
                                                            }
                                                            callingUid2 = type;
                                                            if (callingUid2 != 2005) {
                                                                try {
                                                                    i5 = callingUid;
                                                                    try {
                                                                        if (getDefaultDisplayContentLocked().canAddToastWindowForUid(i5)) {
                                                                            if (!addToastWindowRequiresToken) {
                                                                                if ((layoutParams3.flags & 8) != 0) {
                                                                                    if (this.mCurrentFocus != null) {
                                                                                    }
                                                                                }
                                                                            }
                                                                            this.mH.sendMessageDelayed(this.mH.obtainMessage(52, windowState), windowState.mAttrs.hideTimeoutMilliseconds);
                                                                        } else {
                                                                            Slog.w(TAG, "Adding more than one toast window for UID at a time.");
                                                                            resetPriorityAfterLockedSection();
                                                                            return -5;
                                                                        }
                                                                    } catch (Throwable th6) {
                                                                        displayContent = th6;
                                                                        i = callingUid2;
                                                                    }
                                                                } catch (Throwable th7) {
                                                                    displayContent = th7;
                                                                    i5 = callingUid;
                                                                    i = callingUid2;
                                                                    i3 = displayId;
                                                                    resetPriorityAfterLockedSection();
                                                                    throw displayContent;
                                                                }
                                                            }
                                                            res = 0;
                                                            if (this.mCurrentFocus == null) {
                                                                this.mWinAddedSinceNullFocus.add(windowState);
                                                            }
                                                            if (!excludeWindowTypeFromTapOutTask(callingUid2)) {
                                                            }
                                                            displayContent2.mTapExcludedWindows.add(windowState);
                                                            type4 = Binder.clearCallingIdentity();
                                                            windowState.attach();
                                                            this.mWindowMap.put(client.asBinder(), windowState);
                                                            windowState.initAppOpsState();
                                                            windowState.setHiddenWhileSuspended(this.mPmInternal.isPackageSuspended(windowState.getOwningPackage(), UserHandle.getUserId(windowState.getOwningUid())));
                                                            windowState.setForceHideNonSystemOverlayWindowIfNeeded(this.mHidingNonSystemOverlayWindows.isEmpty() ^ true);
                                                            parentWindow2 = token3.asAppWindowToken();
                                                            if (callingUid2 == 3 || parentWindow2 == null) {
                                                                this.mHwWMSEx.updateHwStartWindowRecord(layoutParams3.packageName);
                                                            } else {
                                                                parentWindow2.startingWindow = windowState;
                                                                boolean z2 = hasStatusBarServicePermission;
                                                            }
                                                            hasStatusBarServicePermission = true;
                                                            windowState.mToken.addWindow(windowState);
                                                            if (callingUid2 != 2011) {
                                                                windowState.mGivenInsetsPending = true;
                                                                setInputMethodWindowLocked(windowState);
                                                                hasStatusBarServicePermission = false;
                                                            } else if (callingUid2 == 2012) {
                                                                displayContent2.computeImeTarget(true);
                                                                hasStatusBarServicePermission = false;
                                                            } else if (callingUid2 == 2013) {
                                                                displayContent2.mWallpaperController.clearLastWallpaperTimeoutTime();
                                                                displayContent2.pendingLayoutChanges |= 4;
                                                            } else if ((layoutParams3.flags & DumpState.DUMP_DEXOPT) != 0) {
                                                                displayContent2.pendingLayoutChanges |= 4;
                                                            } else if (displayContent2.mWallpaperController.isBelowWallpaperTarget(windowState)) {
                                                                displayContent2.pendingLayoutChanges |= 4;
                                                            }
                                                            windowState.applyAdjustForImeIfNeeded();
                                                            if (callingUid2 != 2034) {
                                                                try {
                                                                    imMayMove = hasStatusBarServicePermission;
                                                                    i3 = displayId;
                                                                    try {
                                                                        this.mRoot.getDisplayContent(i3).getDockedDividerController().setWindow(windowState);
                                                                    } catch (Throwable th8) {
                                                                        displayContent = th8;
                                                                    }
                                                                } catch (Throwable th9) {
                                                                    displayContent = th9;
                                                                    i3 = displayId;
                                                                    i = callingUid2;
                                                                    windowState = parentWindow4;
                                                                    resetPriorityAfterLockedSection();
                                                                    throw displayContent;
                                                                }
                                                            }
                                                            imMayMove = hasStatusBarServicePermission;
                                                            i3 = displayId;
                                                            try {
                                                                winAnimator = windowState.mWinAnimator;
                                                                winAnimator.mEnterAnimationPending = true;
                                                                winAnimator.mEnteringAnimation = true;
                                                                if (atoken2 == null) {
                                                                    atoken3 = atoken2;
                                                                    if (atoken3.isVisible() && !prepareWindowReplacementTransition(atoken3)) {
                                                                        prepareNoneTransitionForRelaunching(atoken3);
                                                                    }
                                                                } else {
                                                                    atoken3 = atoken2;
                                                                }
                                                                forceCompatMode2 = displayContent2.mDisplayFrames;
                                                                openInputChannels = type5;
                                                                i = callingUid2;
                                                                type2 = displayContent2.getDisplayInfo();
                                                                forceCompatMode2.onDisplayInfoUpdated(type2, displayContent2.calculateDisplayCutoutForRotation(type2.rotation));
                                                                if (atoken3 != null || atoken3.getTask() == 0) {
                                                                    obj3 = type2;
                                                                    taskBounds = iBinder2;
                                                                } else {
                                                                    callingUid2 = this.mTmpRect;
                                                                    DisplayInfo displayInfo = type2;
                                                                    atoken3.getTask().getBounds(this.mTmpRect);
                                                                    taskBounds = callingUid2;
                                                                }
                                                                if (this.mPolicy.getLayoutHintLw(windowState.mAttrs, taskBounds, forceCompatMode2, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout)) {
                                                                    res = 0 | 4;
                                                                }
                                                                if (this.mInTouchMode) {
                                                                    res |= 1;
                                                                }
                                                                if (windowState.mAppToken == null || !windowState.mAppToken.isClientHidden()) {
                                                                    res |= 2;
                                                                }
                                                                this.mInputMonitor.setUpdateInputWindowsNeededLw();
                                                                focusChanged = false;
                                                                if (windowState.canReceiveKeys() == 0) {
                                                                    focusChanged = updateFocusedWindowLocked(1, 0);
                                                                    if (focusChanged) {
                                                                        imMayMove = false;
                                                                    }
                                                                }
                                                                if (imMayMove && (HwPCUtils.enabledInPad() == 0 || HwPCUtils.isPcCastModeInServer() == 0 || HwPCUtils.isValidExtDisplayId(displayContent2.getDisplayId()) != 0)) {
                                                                    displayContent2.computeImeTarget(1);
                                                                }
                                                                windowState.getParent().assignChildLayers();
                                                                if (focusChanged) {
                                                                } else {
                                                                    if (HwPCUtils.isPcCastModeInServer() != 0) {
                                                                        type2 = TAG;
                                                                        callingUid2 = new StringBuilder();
                                                                        callingUid2.append("updateFocusedWindowLocked mCurrentFocus = ");
                                                                        callingUid2.append(this.mCurrentFocus);
                                                                        HwPCUtils.log(type2, callingUid2.toString());
                                                                    }
                                                                    this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, 0);
                                                                }
                                                                this.mInputMonitor.updateInputWindowsLw(0);
                                                                str = TAG;
                                                                type2 = new StringBuilder();
                                                                type2.append("addWindow: ");
                                                                type2.append(windowState);
                                                                Slog.v(str, type2.toString());
                                                                if (windowState.isVisibleOrAdding() && updateOrientationFromAppTokensLocked(i3)) {
                                                                    reportNewConfig = true;
                                                                }
                                                                resetPriorityAfterLockedSection();
                                                                if (reportNewConfig) {
                                                                    sendNewConfiguration(i3);
                                                                }
                                                                Binder.restoreCallingIdentity(type4);
                                                                return res;
                                                            } catch (Throwable th10) {
                                                                displayContent = th10;
                                                                i = callingUid2;
                                                                resetPriorityAfterLockedSection();
                                                                throw displayContent;
                                                            }
                                                        }
                                                    }
                                                    type5 = false;
                                                    if (type5) {
                                                    }
                                                    callingUid2 = type;
                                                    if (callingUid2 != 2005) {
                                                    }
                                                    res = 0;
                                                    if (this.mCurrentFocus == null) {
                                                    }
                                                    try {
                                                        if (excludeWindowTypeFromTapOutTask(callingUid2)) {
                                                        }
                                                        displayContent2.mTapExcludedWindows.add(windowState);
                                                        type4 = Binder.clearCallingIdentity();
                                                        windowState.attach();
                                                        this.mWindowMap.put(client.asBinder(), windowState);
                                                        windowState.initAppOpsState();
                                                        windowState.setHiddenWhileSuspended(this.mPmInternal.isPackageSuspended(windowState.getOwningPackage(), UserHandle.getUserId(windowState.getOwningUid())));
                                                        windowState.setForceHideNonSystemOverlayWindowIfNeeded(this.mHidingNonSystemOverlayWindows.isEmpty() ^ true);
                                                        parentWindow2 = token3.asAppWindowToken();
                                                        if (callingUid2 == 3) {
                                                        }
                                                        this.mHwWMSEx.updateHwStartWindowRecord(layoutParams3.packageName);
                                                        hasStatusBarServicePermission = true;
                                                        windowState.mToken.addWindow(windowState);
                                                        if (callingUid2 != 2011) {
                                                        }
                                                        windowState.applyAdjustForImeIfNeeded();
                                                        if (callingUid2 != 2034) {
                                                        }
                                                        winAnimator = windowState.mWinAnimator;
                                                        winAnimator.mEnterAnimationPending = true;
                                                        winAnimator.mEnteringAnimation = true;
                                                        if (atoken2 == null) {
                                                        }
                                                        forceCompatMode2 = displayContent2.mDisplayFrames;
                                                        openInputChannels = type5;
                                                        i = callingUid2;
                                                        type2 = displayContent2.getDisplayInfo();
                                                    } catch (Throwable th11) {
                                                        displayContent = th11;
                                                        i = callingUid2;
                                                        i3 = displayId;
                                                        resetPriorityAfterLockedSection();
                                                        throw displayContent;
                                                    }
                                                    try {
                                                        forceCompatMode2.onDisplayInfoUpdated(type2, displayContent2.calculateDisplayCutoutForRotation(type2.rotation));
                                                        if (atoken3 != null) {
                                                        }
                                                        obj3 = type2;
                                                        taskBounds = iBinder2;
                                                        if (this.mPolicy.getLayoutHintLw(windowState.mAttrs, taskBounds, forceCompatMode2, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout)) {
                                                        }
                                                        if (this.mInTouchMode) {
                                                        }
                                                        res |= 2;
                                                        this.mInputMonitor.setUpdateInputWindowsNeededLw();
                                                        focusChanged = false;
                                                        if (windowState.canReceiveKeys() == 0) {
                                                        }
                                                        displayContent2.computeImeTarget(1);
                                                        windowState.getParent().assignChildLayers();
                                                        if (focusChanged) {
                                                        }
                                                        this.mInputMonitor.updateInputWindowsLw(0);
                                                        str = TAG;
                                                        type2 = new StringBuilder();
                                                        type2.append("addWindow: ");
                                                        type2.append(windowState);
                                                        Slog.v(str, type2.toString());
                                                        reportNewConfig = true;
                                                        resetPriorityAfterLockedSection();
                                                        if (reportNewConfig) {
                                                        }
                                                        Binder.restoreCallingIdentity(type4);
                                                        return res;
                                                    } catch (Throwable th12) {
                                                        displayContent = th12;
                                                        windowState = parentWindow4;
                                                        resetPriorityAfterLockedSection();
                                                        throw displayContent;
                                                    }
                                                }
                                            }
                                            str3 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Attempted to add a toast window with bad token ");
                                            stringBuilder3.append(layoutParams.token);
                                            stringBuilder3.append(".  Aborting.");
                                            Slog.w(str3, stringBuilder3.toString());
                                            resetPriorityAfterLockedSection();
                                            return -1;
                                        } catch (Throwable th13) {
                                            displayContent = th13;
                                            i5 = type2;
                                            i = type4;
                                            windowHashMap = windowHashMap2;
                                            windowState = parentWindow2;
                                            i3 = displayId;
                                            resetPriorityAfterLockedSection();
                                            throw displayContent;
                                        }
                                    } catch (Throwable th14) {
                                        displayContent = th14;
                                        i = type4;
                                        windowHashMap = windowHashMap2;
                                        windowState = parentWindow2;
                                        i5 = callingUid3;
                                        i3 = displayId;
                                        resetPriorityAfterLockedSection();
                                        throw displayContent;
                                    }
                                } else {
                                    type2 = callingUid3;
                                    if (type4 != 2035) {
                                        try {
                                            if (token2.asAppWindowToken() != null) {
                                                try {
                                                    str2 = TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("Non-null appWindowToken for system window of rootType=");
                                                    stringBuilder2.append(i2);
                                                    Slog.w(str2, stringBuilder2.toString());
                                                    layoutParams.token = null;
                                                    int i8 = 2011;
                                                    parentWindow4 = parentWindow2;
                                                    parentWindow5 = session;
                                                } catch (Throwable th15) {
                                                    displayContent = th15;
                                                    i5 = type2;
                                                    i = type4;
                                                    windowHashMap = windowHashMap2;
                                                    windowState = parentWindow2;
                                                    i3 = displayId;
                                                    resetPriorityAfterLockedSection();
                                                    throw displayContent;
                                                }
                                                try {
                                                    token = token;
                                                    iBinder = null;
                                                    callingUid = type2;
                                                    i7 = 2013;
                                                    try {
                                                        token2 = new WindowToken(this, client.asBinder(), type4, 0, displayContent2, parentWindow5.mCanAddInternalSystemWindow);
                                                        atoken = null;
                                                        windowState = windowState;
                                                        session2 = parentWindow5;
                                                        iWindow2 = client;
                                                        z = parentWindow5.mCanAddInternalSystemWindow;
                                                        type2 = token2;
                                                        i6 = parentWindow5.mUid;
                                                        obj2 = obj;
                                                        atoken2 = atoken;
                                                        windowHashMap = windowHashMap2;
                                                        token3 = token2;
                                                        type = type4;
                                                        iBinder2 = iBinder;
                                                        type3 = outInputChannel;
                                                        layoutParams2 = layoutParams;
                                                        windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                                                        if (windowState.mDeathRecipient == null) {
                                                        }
                                                    } catch (Throwable th16) {
                                                        displayContent = th16;
                                                        i = type4;
                                                        windowHashMap = windowHashMap2;
                                                        windowState = parentWindow4;
                                                        i5 = callingUid;
                                                        i3 = displayId;
                                                        resetPriorityAfterLockedSection();
                                                        throw displayContent;
                                                    }
                                                } catch (Throwable th17) {
                                                    displayContent = th17;
                                                    i5 = type2;
                                                    i = type4;
                                                    windowHashMap = windowHashMap2;
                                                    windowState = parentWindow4;
                                                    i3 = displayId;
                                                    resetPriorityAfterLockedSection();
                                                    throw displayContent;
                                                }
                                            }
                                            callingUid = type2;
                                            i7 = 2013;
                                            parentWindow4 = parentWindow2;
                                            parentWindow5 = session;
                                            iBinder = null;
                                            try {
                                                if (this.mCust != null) {
                                                    if (this.mCust.isChargingAlbumType(type4) && !this.mCust.isChargingAlbumType(token2.windowType)) {
                                                        str3 = TAG;
                                                        stringBuilder3 = new StringBuilder();
                                                        stringBuilder3.append("Attempted to add Dream window with bad token ");
                                                        stringBuilder3.append(layoutParams.token);
                                                        stringBuilder3.append(".  Aborting.");
                                                        Slog.w(str3, stringBuilder3.toString());
                                                        resetPriorityAfterLockedSection();
                                                        return -1;
                                                    }
                                                }
                                                atoken = null;
                                                windowState = windowState;
                                                session2 = parentWindow5;
                                                iWindow2 = client;
                                                z = parentWindow5.mCanAddInternalSystemWindow;
                                                type2 = token2;
                                                i6 = parentWindow5.mUid;
                                                obj2 = obj;
                                                atoken2 = atoken;
                                                windowHashMap = windowHashMap2;
                                                token3 = token2;
                                                type = type4;
                                                iBinder2 = iBinder;
                                                type3 = outInputChannel;
                                                layoutParams2 = layoutParams;
                                                windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                                                if (windowState.mDeathRecipient == null) {
                                                }
                                            } catch (Throwable th18) {
                                                displayContent = th18;
                                                i = type4;
                                                windowHashMap = windowHashMap2;
                                                i5 = callingUid;
                                                i3 = displayId;
                                                windowState = parentWindow4;
                                                resetPriorityAfterLockedSection();
                                                throw displayContent;
                                            }
                                        } catch (Throwable th19) {
                                            displayContent = th19;
                                            i5 = type2;
                                            i = type4;
                                            windowHashMap = windowHashMap2;
                                            parentWindow = displayId;
                                            windowState = parentWindow2;
                                            resetPriorityAfterLockedSection();
                                            throw displayContent;
                                        }
                                    } else if (token2.windowType != 2035) {
                                        str3 = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Attempted to add QS dialog window with bad token ");
                                        stringBuilder3.append(layoutParams.token);
                                        stringBuilder3.append(".  Aborting.");
                                        Slog.w(str3, stringBuilder3.toString());
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    } else {
                                        callingUid = type2;
                                        i7 = 2013;
                                        parentWindow4 = parentWindow2;
                                        parentWindow5 = session;
                                        iBinder = null;
                                        atoken = null;
                                        windowState = windowState;
                                        session2 = parentWindow5;
                                        iWindow2 = client;
                                        z = parentWindow5.mCanAddInternalSystemWindow;
                                        type2 = token2;
                                        i6 = parentWindow5.mUid;
                                        obj2 = obj;
                                        atoken2 = atoken;
                                        windowHashMap = windowHashMap2;
                                        token3 = token2;
                                        type = type4;
                                        iBinder2 = iBinder;
                                        type3 = outInputChannel;
                                        layoutParams2 = layoutParams;
                                        windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                                        if (windowState.mDeathRecipient == null) {
                                        }
                                    }
                                }
                                i7 = 2013;
                                parentWindow4 = parentWindow2;
                                callingUid = callingUid3;
                                parentWindow5 = session;
                                iBinder = null;
                                atoken = null;
                                windowState = windowState;
                                session2 = parentWindow5;
                                iWindow2 = client;
                                z = parentWindow5.mCanAddInternalSystemWindow;
                                type2 = token2;
                                i6 = parentWindow5.mUid;
                                obj2 = obj;
                                atoken2 = atoken;
                                windowHashMap = windowHashMap2;
                                token3 = token2;
                                type = type4;
                                iBinder2 = iBinder;
                                type3 = outInputChannel;
                                layoutParams2 = layoutParams;
                                windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                                if (windowState.mDeathRecipient == null) {
                                }
                            } else if (token2.windowType != 2031) {
                                str3 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Attempted to add voice interaction window with bad token ");
                                stringBuilder3.append(layoutParams.token);
                                stringBuilder3.append(".  Aborting.");
                                Slog.w(str3, stringBuilder3.toString());
                                resetPriorityAfterLockedSection();
                                return -1;
                            }
                            parentWindow4 = parentWindow2;
                            callingUid = callingUid3;
                            parentWindow5 = session;
                            iBinder = null;
                            atoken = null;
                            windowState = windowState;
                            session2 = parentWindow5;
                            iWindow2 = client;
                            z = parentWindow5.mCanAddInternalSystemWindow;
                            type2 = token2;
                            i6 = parentWindow5.mUid;
                            obj2 = obj;
                            atoken2 = atoken;
                            windowHashMap = windowHashMap2;
                            token3 = token2;
                            type = type4;
                            iBinder2 = iBinder;
                            type3 = outInputChannel;
                            layoutParams2 = layoutParams;
                            windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                            if (windowState.mDeathRecipient == null) {
                            }
                        } else {
                            AppWindowToken atoken4 = token2.asAppWindowToken();
                            if (atoken4 == null) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Attempted to add window with non-application token ");
                                stringBuilder2.append(token2);
                                stringBuilder2.append(".  Aborting.");
                                Slog.w(str2, stringBuilder2.toString());
                                resetPriorityAfterLockedSection();
                                return -3;
                            } else if (atoken4.removed) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Attempted to add window with exiting application token ");
                                stringBuilder2.append(token2);
                                stringBuilder2.append(".  Aborting.");
                                Slog.w(str2, stringBuilder2.toString());
                                resetPriorityAfterLockedSection();
                                return -4;
                            } else {
                                if (type4 == 3) {
                                    if (atoken4.startingWindow != null) {
                                        Slog.w(TAG, "Attempted to add starting window to token with already existing starting window");
                                        resetPriorityAfterLockedSection();
                                        return -5;
                                    }
                                }
                                atoken = atoken4;
                                parentWindow4 = parentWindow2;
                                callingUid = callingUid3;
                            }
                        }
                    } else if (i5 < 1 || i5 > 99) {
                        WindowToken token4 = token;
                        forceCompatMode3 = forceCompatMode2;
                        if (i5 == 2011) {
                            token = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add input method window with unknown token ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(token, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -1;
                        } else if (i5 == 2031) {
                            token = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add voice interaction window with unknown token ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(token, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -1;
                        } else if (i5 == 2013) {
                            token = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add wallpaper window with unknown token ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(token, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -1;
                        } else if (i5 == 2023) {
                            token = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add Dream window with unknown token ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(token, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -1;
                        } else if (i5 == 2035) {
                            token = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add QS dialog window with unknown token ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(token, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -1;
                        } else if (i5 == 2032) {
                            token = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to add Accessibility overlay window with unknown token ");
                            stringBuilder2.append(layoutParams.token);
                            stringBuilder2.append(".  Aborting.");
                            Slog.w(token, stringBuilder2.toString());
                            resetPriorityAfterLockedSection();
                            return -1;
                        } else {
                            if (type2 == 2005) {
                                if (doesAddToastWindowRequireToken(layoutParams.packageName, callingUid2, parentWindow6) != null) {
                                    token = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Attempted to add a toast window with unknown token ");
                                    stringBuilder2.append(layoutParams.token);
                                    stringBuilder2.append(".  Aborting.");
                                    Slog.w(token, stringBuilder2.toString());
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            }
                            try {
                                i2 = 2013;
                                WindowToken binder = layoutParams.token != null ? layoutParams.token : client.asBinder();
                                try {
                                    i2 = i5;
                                    boolean isRoundedCornerOverlay = (layoutParams.privateFlags & DumpState.DUMP_DEXOPT) != null ? true : null;
                                    WindowToken windowToken = parentWindow2.mCanAddInternalSystemWindow;
                                    token = token;
                                    parentWindow2 = parentWindow6;
                                    type4 = 2011;
                                    i4 = forceCompatMode3;
                                    type4 = type2;
                                    callingUid3 = callingUid2;
                                    iArr = appOp;
                                    token2 = new WindowToken(this, binder, type2, false, displayContent2, windowToken, isRoundedCornerOverlay);
                                    parentWindow4 = parentWindow2;
                                    atoken = null;
                                    callingUid = callingUid3;
                                    obj = 1;
                                } catch (Throwable th20) {
                                    displayContent = th20;
                                    i = type4;
                                    windowHashMap = windowHashMap2;
                                    i3 = displayId;
                                    resetPriorityAfterLockedSection();
                                    throw displayContent;
                                }
                            } catch (Throwable th21) {
                                displayContent = th21;
                                WindowContainer windowContainer2 = parentWindow6;
                                iArr = appOp;
                                i4 = forceCompatMode3;
                                i = type2;
                                i5 = callingUid2;
                                windowHashMap = windowHashMap2;
                                parentWindow = i2;
                                parentWindow3 = windowContainer2;
                                resetPriorityAfterLockedSection();
                                throw displayContent;
                            }
                        }
                    } else {
                        try {
                            String str4 = TAG;
                            token = new StringBuilder();
                            forceCompatMode3 = forceCompatMode2;
                            try {
                                token.append("Attempted to add application window with unknown token ");
                                token.append(layoutParams.token);
                                token.append(".  Aborting.");
                                Slog.w(str4, token.toString());
                                resetPriorityAfterLockedSection();
                                return -1;
                            } catch (Throwable th22) {
                                displayContent = th22;
                                parentWindow3 = parentWindow6;
                                i = type2;
                                i5 = callingUid2;
                                iArr = appOp;
                                windowHashMap = windowHashMap2;
                                parentWindow = i2;
                                i4 = forceCompatMode3;
                            }
                        } catch (Throwable th23) {
                            displayContent = th23;
                            parentWindow3 = parentWindow6;
                            i4 = forceCompatMode2;
                            i = type2;
                            i5 = callingUid2;
                            iArr = appOp;
                            windowHashMap = windowHashMap2;
                            parentWindow = i2;
                            resetPriorityAfterLockedSection();
                            throw displayContent;
                        }
                    }
                    parentWindow5 = session;
                    iBinder = null;
                    windowState = windowState;
                    session2 = parentWindow5;
                    iWindow2 = client;
                    z = parentWindow5.mCanAddInternalSystemWindow;
                    type2 = token2;
                    i6 = parentWindow5.mUid;
                    obj2 = obj;
                    atoken2 = atoken;
                    windowHashMap = windowHashMap2;
                    token3 = token2;
                    type = type4;
                    iBinder2 = iBinder;
                    type3 = outInputChannel;
                    layoutParams2 = layoutParams;
                    try {
                        windowState = new WindowState(this, session2, iWindow2, type2, parentWindow4, iArr[0], seq, layoutParams, viewVisibility, i6, z, i4);
                        if (windowState.mDeathRecipient == null) {
                        }
                    } catch (Throwable th24) {
                        displayContent = th24;
                        i5 = callingUid;
                        i = type;
                        i3 = displayId;
                        windowState = parentWindow4;
                        resetPriorityAfterLockedSection();
                        throw displayContent;
                    }
                }
                i = type2;
                i5 = callingUid2;
                iArr = appOp;
                windowHashMap = windowHashMap2;
                i3 = i2;
                throw new IllegalStateException("Display has not been initialialized");
            } catch (Throwable th25) {
                displayContent = th25;
                resetPriorityAfterLockedSection();
                throw displayContent;
            }
        }
    }

    private DisplayContent getDisplayContentOrCreate(int displayId) {
        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
        if (displayContent != null) {
            return displayContent;
        }
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display != null) {
            return this.mRoot.createDisplayContent(display, null);
        }
        return displayContent;
    }

    private boolean doesAddToastWindowRequireToken(String packageName, int callingUid, WindowState attachedWindow) {
        boolean z = true;
        if (attachedWindow != null) {
            if (attachedWindow.mAppToken == null || attachedWindow.mAppToken.mTargetSdk < 26) {
                z = false;
            }
            return z;
        }
        try {
            ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(callingUid));
            if (appInfo.uid == callingUid) {
                return appInfo.targetSdkVersion >= 26;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(packageName);
                stringBuilder.append(" not in UID ");
                stringBuilder.append(callingUid);
                throw new SecurityException(stringBuilder.toString());
            }
        } catch (NameNotFoundException e) {
        }
    }

    private boolean prepareWindowReplacementTransition(AppWindowToken atoken) {
        atoken.clearAllDrawn();
        WindowState replacedWindow = atoken.getReplacingWindow();
        if (replacedWindow == null) {
            return false;
        }
        Rect frame = replacedWindow.mVisibleFrame;
        this.mOpeningApps.add(atoken);
        prepareAppTransition(18, true);
        this.mAppTransition.overridePendingAppTransitionClipReveal(frame.left, frame.top, frame.width(), frame.height());
        executeAppTransition();
        return true;
    }

    private void prepareNoneTransitionForRelaunching(AppWindowToken atoken) {
        if (this.mDisplayFrozen && !this.mOpeningApps.contains(atoken) && atoken.isRelaunching()) {
            this.mOpeningApps.add(atoken);
            prepareAppTransition(0, false);
            executeAppTransition();
        }
    }

    boolean isSecureLocked(WindowState w) {
        if ((w.mAttrs.flags & 8192) == 0 && !DevicePolicyCache.getInstance().getScreenCaptureDisabled(UserHandle.getUserId(w.mOwnerUid))) {
            return false;
        }
        return true;
    }

    public void refreshScreenCaptureDisabled(int userId) {
        if (Binder.getCallingUid() == 1000) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.setSecureSurfaceState(userId, DevicePolicyCache.getInstance().getScreenCaptureDisabled(userId));
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Only system can call refreshScreenCaptureDisabled.");
    }

    public void updateAppOpsState() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateAppOpsState();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void removeWindow(Session session, IWindow client) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, (boolean) null);
                if (win == null) {
                } else {
                    win.removeIfPossible();
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void postWindowRemoveCleanupLocked(WindowState win) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("postWindowRemoveCleanupLocked: ");
        stringBuilder.append(win);
        Slog.v(str, stringBuilder.toString());
        this.mWindowMap.remove(win.mClient.asBinder());
        markForSeamlessRotation(win, false);
        win.resetAppOpsState();
        this.mHwWMSEx.removeWindowReport(win);
        if (this.mCurrentFocus == null) {
            this.mWinRemovedSinceNullFocus.add(win);
        }
        this.mPendingRemove.remove(win);
        this.mResizingWindows.remove(win);
        updateNonSystemOverlayWindowsVisibilityIfNeeded(win, false);
        this.mWindowsChanged = true;
        if (this.mInputMethodWindow == win) {
            setInputMethodWindowLocked(null);
        }
        WindowToken token = win.mToken;
        AppWindowToken atoken = win.mAppToken;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Removing ");
        stringBuilder2.append(win);
        stringBuilder2.append(" from ");
        stringBuilder2.append(token);
        Slog.v(str2, stringBuilder2.toString());
        if (token.isEmpty()) {
            if (!token.mPersistOnEmpty) {
                token.removeImmediately();
            } else if (atoken != null) {
                atoken.firstWindowDrawn = false;
                atoken.clearAllDrawn();
                TaskStack stack = atoken.getStack();
                if (stack != null) {
                    stack.mExitingAppTokens.remove(atoken);
                }
            }
        }
        if (atoken != null) {
            atoken.postWindowRemoveStartingWindowCleanup(win);
        }
        DisplayContent dc = win.getDisplayContent();
        if (win.mAttrs.type == 2013) {
            dc.mWallpaperController.clearLastWallpaperTimeoutTime();
            dc.pendingLayoutChanges |= 4;
        } else if ((win.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
            dc.pendingLayoutChanges |= 4;
        }
        if (!(dc == null || this.mWindowPlacerLocked.isInLayout())) {
            dc.assignWindowLayers(true);
            this.mWindowPlacerLocked.performSurfacePlacement();
            if (win.mAppToken != null) {
                win.mAppToken.updateReportedVisibilityLocked();
            }
        }
        this.mInputMonitor.updateInputWindowsLw(true);
    }

    void setInputMethodWindowLocked(WindowState win) {
        if (win != null && ((this.mInputMethodWindow == null || this.mInputMethodWindow != win) && isLandScapeMultiWindowMode() && (this.mPolicy instanceof PhoneWindowManager))) {
            ((PhoneWindowManager) this.mPolicy).setFocusChangeIMEFrozenTag(false);
        }
        this.mInputMethodWindow = win;
        (win != null ? win.getDisplayContent() : getDefaultDisplayContentLocked()).computeImeTarget(true);
    }

    private void updateHiddenWhileSuspendedState(ArraySet<String> packages, boolean suspended) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateHiddenWhileSuspendedState(packages, suspended);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    static void logSurface(WindowState w, String msg, boolean withStackTrace) {
        String str = new StringBuilder();
        str.append("  SURFACE ");
        str.append(msg);
        str.append(": ");
        str.append(w);
        str = str.toString();
        if (withStackTrace) {
            logWithStack(TAG, str);
        } else {
            Slog.i(TAG, str);
        }
    }

    static void logSurface(SurfaceControl s, String title, String msg) {
        String str = new StringBuilder();
        str.append("  SURFACE ");
        str.append(s);
        str.append(": ");
        str.append(msg);
        str.append(" / ");
        str.append(title);
        Slog.i(TAG, str.toString());
    }

    static void logWithStack(String tag, String s) {
        Slog.i(tag, s, null);
    }

    void setTransparentRegionWindow(Session session, IWindow client, Region region) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                boostPriorityForLockedSection();
                WindowState w = windowForClientLocked(session, client, (boolean) null);
                if (w != null && w.mHasSurface) {
                    w.mWinAnimator.setTransparentRegionHintLocked(region);
                }
            }
            resetPriorityAfterLockedSection();
            Binder.restoreCallingIdentity(origId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void setInsetsWindow(Session session, IWindow client, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                boostPriorityForLockedSection();
                WindowState w = windowForClientLocked(session, client, false);
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setInsetsWindow ");
                    stringBuilder.append(w);
                    stringBuilder.append(", contentInsets=");
                    stringBuilder.append(w.mGivenContentInsets);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(contentInsets);
                    stringBuilder.append(", visibleInsets=");
                    stringBuilder.append(w.mGivenVisibleInsets);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(visibleInsets);
                    stringBuilder.append(", touchableRegion=");
                    stringBuilder.append(w.mGivenTouchableRegion);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(touchableRegion);
                    stringBuilder.append(", touchableInsets ");
                    stringBuilder.append(w.mTouchableInsets);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(touchableInsets);
                    Slog.d(str, stringBuilder.toString());
                }
                if (w != null) {
                    w.mGivenInsetsPending = false;
                    w.mGivenContentInsets.set(contentInsets);
                    w.mGivenVisibleInsets.set(visibleInsets);
                    w.mGivenTouchableRegion.set(touchableRegion);
                    w.mTouchableInsets = touchableInsets;
                    if (w.mGlobalScale != 1.0f) {
                        w.mGivenContentInsets.scale(w.mGlobalScale);
                        w.mGivenVisibleInsets.scale(w.mGlobalScale);
                        w.mGivenTouchableRegion.scale(w.mGlobalScale);
                    }
                    w.setDisplayLayoutNeeded();
                    this.mWindowPlacerLocked.performSurfacePlacement();
                    if (this.mAccessibilityController != null && w.getDisplayContent().getDisplayId() == 0) {
                        this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                    }
                }
            }
            resetPriorityAfterLockedSection();
            Binder.restoreCallingIdentity(origId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void getWindowDisplayFrame(Session session, IWindow client, Rect outDisplayFrame) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, (boolean) null);
                if (win == null) {
                    outDisplayFrame.setEmpty();
                } else {
                    outDisplayFrame.set(win.mDisplayFrame);
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mAccessibilityController != null) {
                    WindowState window = (WindowState) this.mWindowMap.get(token);
                    if (window != null && (window.getDisplayId() == 0 || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad()))) {
                        this.mAccessibilityController.onRectangleOnScreenRequestedLocked(rectangle);
                    }
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public IWindowId getWindowId(IBinder token) {
        IWindowId iWindowId;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState window = (WindowState) this.mWindowMap.get(token);
                iWindowId = window != null ? window.mWindowId : null;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return iWindowId;
    }

    public void pokeDrawLock(Session session, IBinder token) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState window = windowForClientLocked(session, token, (boolean) null);
                if (window != null) {
                    window.pokeDrawLockLw(this.mDrawLockTimeoutMillis);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:238:0x0348 A:{SYNTHETIC, Splitter: B:238:0x0348} */
    /* JADX WARNING: Removed duplicated region for block: B:298:0x0434  */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0365 A:{SYNTHETIC, Splitter: B:248:0x0365} */
    /* JADX WARNING: Removed duplicated region for block: B:330:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:340:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x04e0  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x04e9  */
    /* JADX WARNING: Removed duplicated region for block: B:350:0x04f5 A:{Catch:{ Exception -> 0x03e9, all -> 0x0429, all -> 0x04d8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:354:0x0505 A:{SYNTHETIC, Splitter: B:354:0x0505} */
    /* JADX WARNING: Removed duplicated region for block: B:362:0x0521 A:{SYNTHETIC, Splitter: B:362:0x0521} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0578 A:{SYNTHETIC, Splitter: B:379:0x0578} */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x0581  */
    /* JADX WARNING: Removed duplicated region for block: B:391:0x058f A:{Catch:{ all -> 0x067c }} */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0598  */
    /* JADX WARNING: Removed duplicated region for block: B:399:0x05a3  */
    /* JADX WARNING: Removed duplicated region for block: B:398:0x059d A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x05f9 A:{SYNTHETIC, Splitter: B:407:0x05f9} */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x0312 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0338 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0348 A:{SYNTHETIC, Splitter: B:238:0x0348} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0365 A:{SYNTHETIC, Splitter: B:248:0x0365} */
    /* JADX WARNING: Removed duplicated region for block: B:298:0x0434  */
    /* JADX WARNING: Removed duplicated region for block: B:330:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x04e0  */
    /* JADX WARNING: Removed duplicated region for block: B:340:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x04e9  */
    /* JADX WARNING: Removed duplicated region for block: B:350:0x04f5 A:{Catch:{ Exception -> 0x03e9, all -> 0x0429, all -> 0x04d8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:354:0x0505 A:{SYNTHETIC, Splitter: B:354:0x0505} */
    /* JADX WARNING: Removed duplicated region for block: B:362:0x0521 A:{SYNTHETIC, Splitter: B:362:0x0521} */
    /* JADX WARNING: Removed duplicated region for block: B:372:0x0557 A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0578 A:{SYNTHETIC, Splitter: B:379:0x0578} */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x0581  */
    /* JADX WARNING: Removed duplicated region for block: B:391:0x058f A:{Catch:{ all -> 0x067c }} */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0598  */
    /* JADX WARNING: Removed duplicated region for block: B:398:0x059d A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:399:0x05a3  */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x05f9 A:{SYNTHETIC, Splitter: B:407:0x05f9} */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x0280 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x027d A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x02da  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x02a7 A:{SYNTHETIC, Splitter: B:202:0x02a7} */
    /* JADX WARNING: Removed duplicated region for block: B:209:0x02e7 A:{Catch:{ all -> 0x06c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:208:0x02e5 A:{Catch:{ all -> 0x06c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:213:0x02ed A:{SYNTHETIC, Splitter: B:213:0x02ed} */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x0312 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0338 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0348 A:{SYNTHETIC, Splitter: B:238:0x0348} */
    /* JADX WARNING: Removed duplicated region for block: B:298:0x0434  */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0365 A:{SYNTHETIC, Splitter: B:248:0x0365} */
    /* JADX WARNING: Removed duplicated region for block: B:330:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:340:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x04e0  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x04e9  */
    /* JADX WARNING: Removed duplicated region for block: B:350:0x04f5 A:{Catch:{ Exception -> 0x03e9, all -> 0x0429, all -> 0x04d8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:354:0x0505 A:{SYNTHETIC, Splitter: B:354:0x0505} */
    /* JADX WARNING: Removed duplicated region for block: B:362:0x0521 A:{SYNTHETIC, Splitter: B:362:0x0521} */
    /* JADX WARNING: Removed duplicated region for block: B:372:0x0557 A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0578 A:{SYNTHETIC, Splitter: B:379:0x0578} */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x0581  */
    /* JADX WARNING: Removed duplicated region for block: B:391:0x058f A:{Catch:{ all -> 0x067c }} */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0598  */
    /* JADX WARNING: Removed duplicated region for block: B:399:0x05a3  */
    /* JADX WARNING: Removed duplicated region for block: B:398:0x059d A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x05f9 A:{SYNTHETIC, Splitter: B:407:0x05f9} */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x026e A:{SYNTHETIC, Splitter: B:183:0x026e} */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x027d A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x0280 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:194:0x028a A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x02a7 A:{SYNTHETIC, Splitter: B:202:0x02a7} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x02da  */
    /* JADX WARNING: Removed duplicated region for block: B:208:0x02e5 A:{Catch:{ all -> 0x06c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:209:0x02e7 A:{Catch:{ all -> 0x06c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:213:0x02ed A:{SYNTHETIC, Splitter: B:213:0x02ed} */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x0312 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0338 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0348 A:{SYNTHETIC, Splitter: B:238:0x0348} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0365 A:{SYNTHETIC, Splitter: B:248:0x0365} */
    /* JADX WARNING: Removed duplicated region for block: B:298:0x0434  */
    /* JADX WARNING: Removed duplicated region for block: B:330:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x04e0  */
    /* JADX WARNING: Removed duplicated region for block: B:340:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x04e9  */
    /* JADX WARNING: Removed duplicated region for block: B:350:0x04f5 A:{Catch:{ Exception -> 0x03e9, all -> 0x0429, all -> 0x04d8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:354:0x0505 A:{SYNTHETIC, Splitter: B:354:0x0505} */
    /* JADX WARNING: Removed duplicated region for block: B:362:0x0521 A:{SYNTHETIC, Splitter: B:362:0x0521} */
    /* JADX WARNING: Removed duplicated region for block: B:372:0x0557 A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0578 A:{SYNTHETIC, Splitter: B:379:0x0578} */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x0581  */
    /* JADX WARNING: Removed duplicated region for block: B:391:0x058f A:{Catch:{ all -> 0x067c }} */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0598  */
    /* JADX WARNING: Removed duplicated region for block: B:398:0x059d A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:399:0x05a3  */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x05f9 A:{SYNTHETIC, Splitter: B:407:0x05f9} */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x0254 A:{SYNTHETIC, Splitter: B:168:0x0254} */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x026e A:{SYNTHETIC, Splitter: B:183:0x026e} */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x0280 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x027d A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:194:0x028a A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x02da  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x02a7 A:{SYNTHETIC, Splitter: B:202:0x02a7} */
    /* JADX WARNING: Removed duplicated region for block: B:209:0x02e7 A:{Catch:{ all -> 0x06c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:208:0x02e5 A:{Catch:{ all -> 0x06c0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:213:0x02ed A:{SYNTHETIC, Splitter: B:213:0x02ed} */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x0312 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0338 A:{Catch:{ all -> 0x0262 }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0348 A:{SYNTHETIC, Splitter: B:238:0x0348} */
    /* JADX WARNING: Removed duplicated region for block: B:298:0x0434  */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0365 A:{SYNTHETIC, Splitter: B:248:0x0365} */
    /* JADX WARNING: Removed duplicated region for block: B:330:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:340:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x04e0  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x04e9  */
    /* JADX WARNING: Removed duplicated region for block: B:350:0x04f5 A:{Catch:{ Exception -> 0x03e9, all -> 0x0429, all -> 0x04d8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:354:0x0505 A:{SYNTHETIC, Splitter: B:354:0x0505} */
    /* JADX WARNING: Removed duplicated region for block: B:362:0x0521 A:{SYNTHETIC, Splitter: B:362:0x0521} */
    /* JADX WARNING: Removed duplicated region for block: B:372:0x0557 A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0578 A:{SYNTHETIC, Splitter: B:379:0x0578} */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x0581  */
    /* JADX WARNING: Removed duplicated region for block: B:391:0x058f A:{Catch:{ all -> 0x067c }} */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0598  */
    /* JADX WARNING: Removed duplicated region for block: B:399:0x05a3  */
    /* JADX WARNING: Removed duplicated region for block: B:398:0x059d A:{Catch:{ all -> 0x054e }} */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x05f9 A:{SYNTHETIC, Splitter: B:407:0x05f9} */
    /* JADX WARNING: Missing block: B:136:0x01d9, code:
            if (r12.mAttrs.surfaceInsets.bottom == 0) goto L_0x01df;
     */
    /* JADX WARNING: Missing block: B:422:0x0660, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:423:0x0664, code:
            if (r13 == false) goto L_0x0674;
     */
    /* JADX WARNING: Missing block: B:424:0x0666, code:
            android.os.Trace.traceBegin(32, "relayoutWindow: sendNewConfiguration");
            sendNewConfiguration(r2);
            android.os.Trace.traceEnd(32);
     */
    /* JADX WARNING: Missing block: B:425:0x0674, code:
            android.os.Binder.restoreCallingIdentity(r38);
     */
    /* JADX WARNING: Missing block: B:426:0x0679, code:
            return r10;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int relayoutWindow(Session session, IWindow client, int seq, LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, long frameNumber, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame, ParcelableWrapper outCutout, MergedConfiguration mergedConfiguration, Surface outSurface) {
        Throwable th;
        Rect rect;
        boolean z;
        boolean z2;
        long j;
        int displayId;
        long origId;
        StringBuilder stringBuilder;
        IWindow iWindow = client;
        LayoutParams layoutParams = attrs;
        int i = requestedWidth;
        int i2 = requestedHeight;
        int i3 = viewVisibility;
        MergedConfiguration mergedConfiguration2 = mergedConfiguration;
        Surface surface = outSurface;
        int result = 0;
        boolean hasStatusBarPermission = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0;
        boolean hasStatusBarServicePermission = this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") == 0;
        long origId2 = Binder.clearCallingIdentity();
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                long origId3 = origId2;
                try {
                    WindowState win = windowForClientLocked(session, iWindow, false);
                    Surface surface2;
                    if (win == null) {
                        try {
                            resetPriorityAfterLockedSection();
                            return 0;
                        } catch (Throwable th2) {
                            th = th2;
                            rect = outFrame;
                            surface2 = surface;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    int i4;
                    int attrChanges;
                    boolean imMayMove;
                    boolean focusMayChange;
                    boolean wallpaperMayMove;
                    boolean wallpaperMayMove2;
                    boolean shouldRelayout;
                    WindowState win2;
                    WindowStateAnimator winAnimator;
                    StringBuilder stringBuilder2;
                    int i5;
                    DisplayContent dc;
                    long origId4;
                    MergedConfiguration mergedConfiguration3;
                    DisplayContent displayContent;
                    int displayId2 = win.getDisplayId();
                    this.mHwWMSEx.updateWindowReport(win, i, i2);
                    WindowStateAnimator winAnimator2 = win.mWinAnimator;
                    if (i3 != 8) {
                        win.setRequestedSize(i, i2);
                    }
                    try {
                        win.setFrameNumber(frameNumber);
                        int flagChanges = 0;
                        if (layoutParams != null) {
                            try {
                                int systemUiVisibility;
                                this.mPolicy.adjustWindowParamsLw(win, layoutParams, hasStatusBarServicePermission);
                                if (seq == win.mSeq) {
                                    systemUiVisibility = layoutParams.systemUiVisibility | layoutParams.subtreeSystemUiVisibility;
                                    if (!((67043328 & systemUiVisibility) == 0 || hasStatusBarPermission)) {
                                        systemUiVisibility &= -67043329;
                                    }
                                    win.mSystemUiVisibility = systemUiVisibility;
                                }
                                if (win.mAttrs.type == layoutParams.type) {
                                    if ((layoutParams.privateFlags & 8192) != 0) {
                                        layoutParams.x = win.mAttrs.x;
                                        layoutParams.y = win.mAttrs.y;
                                        layoutParams.width = win.mAttrs.width;
                                        layoutParams.height = win.mAttrs.height;
                                    }
                                    LayoutParams layoutParams2 = win.mAttrs;
                                    i4 = layoutParams.flags ^ layoutParams2.flags;
                                    layoutParams2.flags = i4;
                                    flagChanges = i4;
                                    systemUiVisibility = win.mAttrs.copyFrom(layoutParams);
                                    if ((systemUiVisibility & 16385) != 0) {
                                        win.mLayoutNeeded = true;
                                    }
                                    if (!(win.mAppToken == null || ((flagChanges & DumpState.DUMP_FROZEN) == 0 && (flagChanges & DumpState.DUMP_CHANGES) == 0))) {
                                        win.mAppToken.checkKeyguardFlagsChanged();
                                    }
                                    if (!((DumpState.DUMP_HANDLE & systemUiVisibility) == 0 || this.mAccessibilityController == null || (win.getDisplayId() != 0 && (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.enabledInPad())))) {
                                        this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                                    }
                                    if ((flagChanges & DumpState.DUMP_FROZEN) != 0) {
                                        updateNonSystemOverlayWindowsVisibilityIfNeeded(win, win.mWinAnimator.getShown());
                                    }
                                    attrChanges = systemUiVisibility;
                                } else {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Window type can not be changed after the window is added, type changed from ");
                                    stringBuilder3.append(win.mAttrs.type);
                                    stringBuilder3.append(" to ");
                                    stringBuilder3.append(layoutParams.type);
                                    throw new IllegalArgumentException(stringBuilder3.toString());
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                rect = outFrame;
                                z = hasStatusBarPermission;
                                surface2 = outSurface;
                                while (true) {
                                    break;
                                }
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        attrChanges = 0;
                        i4 = flagChanges;
                        z = hasStatusBarPermission;
                        if (win.toString().contains("HwFullScreenWindow")) {
                            try {
                                this.mPolicy.setFullScreenWinVisibile(i3 == 0);
                                this.mPolicy.setFullScreenWindow(win);
                            } catch (Throwable th5) {
                                th = th5;
                                rect = outFrame;
                                surface2 = outSurface;
                                while (true) {
                                    break;
                                }
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        rect = outFrame;
                        z = hasStatusBarPermission;
                        z2 = hasStatusBarServicePermission;
                        j = origId3;
                        surface2 = outSurface;
                        while (true) {
                            break;
                        }
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                    try {
                        boolean isDefaultDisplay;
                        if (HW_SUPPORT_LAUNCHER_EXIT_ANIM) {
                            if (win.mWinAnimator != null && win.mAnimatingExit && i3 == 0) {
                                win.mWinAnimator.setWindowClipFlag(0);
                                Log.d(TAG, "Relayout clear glRoundRectFlag");
                            }
                        }
                        WindowStateAnimator winAnimator3 = winAnimator2;
                        winAnimator3.mSurfaceDestroyDeferred = (flags & 2) != 0;
                        if (this.mCurrentFocus == win && (Integer.MIN_VALUE & attrChanges) != 0) {
                            this.mPolicy.updateSystemUiColorLw(win);
                        }
                        win.mEnforceSizeCompat = (win.mAttrs.privateFlags & 128) != 0;
                        if ((attrChanges & 128) != 0) {
                            winAnimator3.mAlpha = layoutParams.alpha;
                        }
                        win.setWindowScale(win.mRequestedWidth, win.mRequestedHeight);
                        if (win.mAttrs.surfaceInsets.left == 0) {
                            if (win.mAttrs.surfaceInsets.top == 0) {
                                if (win.mAttrs.surfaceInsets.right == 0) {
                                }
                            }
                        }
                        winAnimator3.setOpaqueLocked(false);
                        int oldVisibility = win.mViewVisibility;
                        boolean shouldPrintLog = false;
                        if (!WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            if (win.mViewVisibility == i3) {
                                int result2;
                                z2 = hasStatusBarServicePermission;
                                hasStatusBarServicePermission = (oldVisibility != 4 || oldVisibility == 8) && i3 == 0;
                                imMayMove = (i4 & 131080) == 0 || hasStatusBarServicePermission;
                                isDefaultDisplay = win.isDefaultDisplay();
                                if (isDefaultDisplay) {
                                    try {
                                        if (!(win.mViewVisibility == i3 && (i4 & 8) == 0 && win.mRelayoutCalled)) {
                                            focusMayChange = true;
                                            if (win.mViewVisibility != i3) {
                                                if ((win.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
                                                    wallpaperMayMove = true;
                                                    wallpaperMayMove |= (i4 & DumpState.DUMP_DEXOPT) == 0 ? 1 : 0;
                                                    if ((i4 & 8192) || winAnimator3.mSurfaceController == null) {
                                                    } else {
                                                        winAnimator3.mSurfaceController.setSecure(isSecureLocked(win));
                                                    }
                                                    win.mRelayoutCalled = true;
                                                    win.mInRelayout = true;
                                                    win.mViewVisibility = i3;
                                                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                                                        wallpaperMayMove2 = wallpaperMayMove;
                                                        displayId = displayId2;
                                                    } else {
                                                        RuntimeException stack = new RuntimeException();
                                                        stack.fillInStackTrace();
                                                        String str = TAG;
                                                        displayId = displayId2;
                                                        StringBuilder stringBuilder4 = new StringBuilder();
                                                        wallpaperMayMove2 = wallpaperMayMove;
                                                        stringBuilder4.append("Relayout ");
                                                        stringBuilder4.append(win);
                                                        stringBuilder4.append(": oldVis=");
                                                        stringBuilder4.append(oldVisibility);
                                                        stringBuilder4.append(" newVis=");
                                                        stringBuilder4.append(i3);
                                                        Slog.i(str, stringBuilder4.toString(), stack);
                                                    }
                                                    win.setDisplayLayoutNeeded();
                                                    win.mGivenInsetsPending = (flags & 1) == 0;
                                                    if (i3 == 0) {
                                                        if (win.mAppToken == null || win.mAttrs.type == 3 || !win.mAppToken.isClientHidden()) {
                                                            shouldRelayout = true;
                                                            if (!(shouldRelayout || !winAnimator3.hasSurface() || win.mAnimatingExit)) {
                                                                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                                    String str2 = TAG;
                                                                    StringBuilder stringBuilder5 = new StringBuilder();
                                                                    stringBuilder5.append("Relayout invis ");
                                                                    stringBuilder5.append(win);
                                                                    stringBuilder5.append(": mAnimatingExit=");
                                                                    stringBuilder5.append(win.mAnimatingExit);
                                                                    Slog.i(str2, stringBuilder5.toString());
                                                                }
                                                                result = 0 | 4;
                                                                if (!win.mWillReplaceWindow) {
                                                                    String str3;
                                                                    wallpaperMayMove = tryStartExitingAnimation(win, winAnimator3, isDefaultDisplay, focusMayChange);
                                                                    this.mAppTransitTrack = "relayout";
                                                                    if (this.mWaitingForConfig) {
                                                                        if (!(win.mAppToken == null || this.mDeferRelayoutWindow.contains(win))) {
                                                                            this.mDeferRelayoutWindow.add(win);
                                                                        }
                                                                    }
                                                                    this.mWindowPlacerLocked.performSurfacePlacement(true);
                                                                    win2 = win;
                                                                    int i6;
                                                                    boolean z3;
                                                                    int i7;
                                                                    if (shouldRelayout) {
                                                                        long j2;
                                                                        i6 = oldVisibility;
                                                                        z3 = isDefaultDisplay;
                                                                        origId = origId3;
                                                                        hasStatusBarServicePermission = win2;
                                                                        surface2 = outSurface;
                                                                        try {
                                                                            WindowStateAnimator winAnimator4 = winAnimator3;
                                                                            Trace.traceBegin(32, "relayoutWindow: viewVisibility_2");
                                                                            winAnimator = winAnimator4;
                                                                            winAnimator.mEnterAnimationPending = false;
                                                                            winAnimator.mEnteringAnimation = false;
                                                                            if (i3 == 0) {
                                                                                try {
                                                                                    if (winAnimator.hasSurface()) {
                                                                                        result2 = result;
                                                                                        try {
                                                                                            Trace.traceBegin(32, "relayoutWindow: getSurface");
                                                                                            winAnimator.mSurfaceController.getSurface(surface2);
                                                                                            Trace.traceEnd(32);
                                                                                            j2 = 32;
                                                                                            Trace.traceEnd(j2);
                                                                                            result = result2;
                                                                                        } catch (Throwable th7) {
                                                                                            th = th7;
                                                                                            j = origId;
                                                                                            result = result2;
                                                                                        }
                                                                                    }
                                                                                } catch (Throwable th8) {
                                                                                    th = th8;
                                                                                    result2 = result;
                                                                                    j = origId;
                                                                                    rect = outFrame;
                                                                                    while (true) {
                                                                                        break;
                                                                                    }
                                                                                    resetPriorityAfterLockedSection();
                                                                                    throw th;
                                                                                }
                                                                            }
                                                                            i7 = attrChanges;
                                                                            result2 = result;
                                                                        } catch (Throwable th9) {
                                                                            th = th9;
                                                                            j = origId;
                                                                            result2 = result;
                                                                            rect = outFrame;
                                                                            while (true) {
                                                                                break;
                                                                            }
                                                                            resetPriorityAfterLockedSection();
                                                                            throw th;
                                                                        }
                                                                        try {
                                                                            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                                                str3 = TAG;
                                                                                StringBuilder stringBuilder6 = new StringBuilder();
                                                                                stringBuilder6.append("Releasing surface in: ");
                                                                                stringBuilder6.append(hasStatusBarServicePermission);
                                                                                Slog.i(str3, stringBuilder6.toString());
                                                                            }
                                                                        } catch (Throwable th10) {
                                                                            th = th10;
                                                                            j = origId;
                                                                            rect = outFrame;
                                                                            result = result2;
                                                                            while (true) {
                                                                                break;
                                                                            }
                                                                            resetPriorityAfterLockedSection();
                                                                            throw th;
                                                                        }
                                                                        try {
                                                                            stringBuilder2 = new StringBuilder();
                                                                            stringBuilder2.append("wmReleaseOutSurface_");
                                                                            stringBuilder2.append(hasStatusBarServicePermission.mAttrs.getTitle());
                                                                            j2 = 32;
                                                                            Trace.traceBegin(32, stringBuilder2.toString());
                                                                            outSurface.release();
                                                                            try {
                                                                                Trace.traceEnd(32);
                                                                                Trace.traceEnd(j2);
                                                                                result = result2;
                                                                            } catch (Throwable th11) {
                                                                                th = th11;
                                                                                rect = outFrame;
                                                                                j = origId;
                                                                                result = result2;
                                                                                while (true) {
                                                                                    break;
                                                                                }
                                                                                resetPriorityAfterLockedSection();
                                                                                throw th;
                                                                            }
                                                                        } catch (Throwable th12) {
                                                                            th = th12;
                                                                            result = result2;
                                                                            while (true) {
                                                                                break;
                                                                            }
                                                                            resetPriorityAfterLockedSection();
                                                                            throw th;
                                                                        }
                                                                    }
                                                                    try {
                                                                        Trace.traceBegin(32, "relayoutWindow: viewVisibility_1");
                                                                        hasStatusBarServicePermission = win2;
                                                                        try {
                                                                            result = createSurfaceControl(outSurface, hasStatusBarServicePermission.relayoutVisibleWindow(result, attrChanges, oldVisibility), hasStatusBarServicePermission, winAnimator3);
                                                                            if ((result & 2) != 0) {
                                                                                wallpaperMayMove = isDefaultDisplay;
                                                                            }
                                                                            try {
                                                                                if (hasStatusBarServicePermission.mAttrs.type == 2011 && (this.mInputMethodWindow == null || this.mInputMethodWindow != hasStatusBarServicePermission)) {
                                                                                    setInputMethodWindowLocked(hasStatusBarServicePermission);
                                                                                    imMayMove = true;
                                                                                }
                                                                                if (hasStatusBarServicePermission.mAttrs.type == 2011 && isLandScapeMultiWindowMode() && (this.mPolicy instanceof PhoneWindowManager)) {
                                                                                    ((PhoneWindowManager) this.mPolicy).setFocusChangeIMEFrozenTag(false);
                                                                                }
                                                                                if (hasStatusBarServicePermission.mAttrs.type == 2012 && this.mInputMethodWindow == null && !(HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer())) {
                                                                                    Slog.d(TAG, "relayoutwindow TYPE_INPUT_METHOD_DIALOG , do setInputMethodWindowLocked");
                                                                                    setInputMethodWindowLocked(hasStatusBarServicePermission);
                                                                                }
                                                                                hasStatusBarServicePermission.adjustStartingWindowFlags();
                                                                                Trace.traceEnd(32);
                                                                                i7 = attrChanges;
                                                                                winAnimator = winAnimator3;
                                                                                origId = origId3;
                                                                            } catch (Throwable th13) {
                                                                                th = th13;
                                                                                rect = outFrame;
                                                                                while (true) {
                                                                                    break;
                                                                                }
                                                                                resetPriorityAfterLockedSection();
                                                                                throw th;
                                                                            }
                                                                        } catch (Exception e) {
                                                                            i6 = oldVisibility;
                                                                            z3 = isDefaultDisplay;
                                                                            oldVisibility = e;
                                                                            this.mInputMonitor.updateInputWindowsLw(true);
                                                                            oldVisibility = TAG;
                                                                            stringBuilder = new StringBuilder();
                                                                            stringBuilder.append("Exception thrown when creating surface for client ");
                                                                            stringBuilder.append(iWindow);
                                                                            stringBuilder.append(" (");
                                                                            stringBuilder.append(hasStatusBarServicePermission.mAttrs.getTitle());
                                                                            stringBuilder.append(")");
                                                                            Slog.w(oldVisibility, stringBuilder.toString(), e);
                                                                            origId = origId3;
                                                                            Binder.restoreCallingIdentity(origId);
                                                                            resetPriorityAfterLockedSection();
                                                                            return 0;
                                                                        } catch (Throwable th14) {
                                                                            th = th14;
                                                                            j = origId;
                                                                            rect = outFrame;
                                                                            while (true) {
                                                                                break;
                                                                            }
                                                                            resetPriorityAfterLockedSection();
                                                                            throw th;
                                                                        }
                                                                    } catch (Throwable th15) {
                                                                        th = th15;
                                                                        surface2 = outSurface;
                                                                        rect = outFrame;
                                                                        while (true) {
                                                                            break;
                                                                        }
                                                                        resetPriorityAfterLockedSection();
                                                                        throw th;
                                                                    }
                                                                    setHwSecureScreen(hasStatusBarServicePermission);
                                                                    if (wallpaperMayMove) {
                                                                        if (updateFocusedWindowLocked(3, false)) {
                                                                            imMayMove = false;
                                                                        }
                                                                    }
                                                                    focusMayChange = (result & 2) == 0;
                                                                    dc = hasStatusBarServicePermission.getDisplayContent();
                                                                    if (imMayMove) {
                                                                        dc.computeImeTarget(true);
                                                                        if (focusMayChange) {
                                                                            dc.assignWindowLayers(false);
                                                                        }
                                                                    }
                                                                    if (wallpaperMayMove2) {
                                                                        DisplayContent displayContent2 = hasStatusBarServicePermission.getDisplayContent();
                                                                        displayContent2.pendingLayoutChanges |= 4;
                                                                    }
                                                                    if (hasStatusBarServicePermission.mAppToken != null) {
                                                                        this.mUnknownAppVisibilityController.notifyRelayouted(hasStatusBarServicePermission.mAppToken);
                                                                    }
                                                                    origId4 = origId;
                                                                    Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                                                    i5 = displayId;
                                                                    hasStatusBarPermission = updateOrientationFromAppTokensLocked(i5);
                                                                    Trace.traceEnd(32);
                                                                    if (shouldRelayout) {
                                                                        try {
                                                                            if (hasStatusBarServicePermission.mFrame.width() == 0 && hasStatusBarServicePermission.mFrame.height() == 0) {
                                                                                String str4 = TAG;
                                                                                stringBuilder = new StringBuilder();
                                                                                stringBuilder.append("force to relayout later when size is 1*1 for:");
                                                                                stringBuilder.append(hasStatusBarServicePermission);
                                                                                Slog.w(str4, stringBuilder.toString());
                                                                                this.mWindowPlacerLocked.performSurfacePlacement(true);
                                                                            }
                                                                        } catch (Throwable th16) {
                                                                            th = th16;
                                                                            rect = outFrame;
                                                                            while (true) {
                                                                                break;
                                                                            }
                                                                            resetPriorityAfterLockedSection();
                                                                            throw th;
                                                                        }
                                                                    }
                                                                    if (focusMayChange || !hasStatusBarServicePermission.mIsWallpaper) {
                                                                    } else {
                                                                        DisplayInfo displayInfo = hasStatusBarServicePermission.getDisplayContent().getDisplayInfo();
                                                                        DisplayInfo displayInfo2 = displayInfo;
                                                                        dc.mWallpaperController.updateWallpaperOffset(hasStatusBarServicePermission, displayInfo.logicalWidth, displayInfo.logicalHeight, null);
                                                                    }
                                                                    if (hasStatusBarServicePermission.mAppToken != null) {
                                                                        hasStatusBarServicePermission.mAppToken.updateReportedVisibilityLocked();
                                                                    }
                                                                    if (winAnimator.mReportSurfaceResized) {
                                                                        winAnimator.mReportSurfaceResized = false;
                                                                        result |= 32;
                                                                    }
                                                                    if (this.mPolicy.isNavBarForcedShownLw(hasStatusBarServicePermission)) {
                                                                        result |= 64;
                                                                    }
                                                                    if (!hasStatusBarServicePermission.isGoneForLayoutLw()) {
                                                                        hasStatusBarServicePermission.mResizedWhileGone = false;
                                                                    }
                                                                    if (shouldRelayout) {
                                                                        mergedConfiguration3 = mergedConfiguration;
                                                                        hasStatusBarServicePermission.getLastReportedMergedConfiguration(mergedConfiguration3);
                                                                    } else {
                                                                        mergedConfiguration3 = mergedConfiguration;
                                                                        hasStatusBarServicePermission.getMergedConfiguration(mergedConfiguration3);
                                                                    }
                                                                    hasStatusBarServicePermission.setLastReportedMergedConfiguration(mergedConfiguration3);
                                                                    hasStatusBarServicePermission.updateLastInsetValues();
                                                                    outFrame.set(hasStatusBarServicePermission.mCompatFrame);
                                                                    outOverscanInsets.set(hasStatusBarServicePermission.mOverscanInsets);
                                                                    outContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                                                    hasStatusBarServicePermission.mLastRelayoutContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                                                    outVisibleInsets.set(hasStatusBarServicePermission.mVisibleInsets);
                                                                    outStableInsets.set(hasStatusBarServicePermission.mStableInsets);
                                                                    outCutout.set(hasStatusBarServicePermission.mDisplayCutout.getDisplayCutout());
                                                                    outOutsets.set(hasStatusBarServicePermission.mOutsets);
                                                                    outBackdropFrame.set(hasStatusBarServicePermission.getBackdropFrame(hasStatusBarServicePermission.mFrame));
                                                                    if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                                                        try {
                                                                            str3 = TAG;
                                                                            StringBuilder stringBuilder7 = new StringBuilder();
                                                                            stringBuilder7.append("Relayout of ");
                                                                            stringBuilder7.append(hasStatusBarServicePermission);
                                                                            stringBuilder7.append(": focusMayChange=");
                                                                            stringBuilder7.append(wallpaperMayMove);
                                                                            Slog.v(str3, stringBuilder7.toString());
                                                                        } catch (Throwable th17) {
                                                                            th = th17;
                                                                        }
                                                                    }
                                                                    result |= this.mInTouchMode;
                                                                    this.mInputMonitor.updateInputWindowsLw(true);
                                                                    if (WindowManagerDebugConfig.DEBUG_LAYOUT || shouldPrintLog) {
                                                                        stringBuilder2 = new StringBuilder();
                                                                        stringBuilder2.append("complete Relayout ");
                                                                        stringBuilder2.append(hasStatusBarServicePermission);
                                                                        stringBuilder2.append("  size:");
                                                                        stringBuilder2.append(outFrame.toShortString());
                                                                        Flog.i(307, stringBuilder2.toString());
                                                                    }
                                                                    hasStatusBarServicePermission.mInRelayout = false;
                                                                    displayContent = getDefaultDisplayContentLocked();
                                                                    if (displayContent != null && i3 == 0) {
                                                                        displayContent.checkNeedNotifyFingerWinCovered();
                                                                        displayContent.mObserveToken = null;
                                                                        displayContent.mTopAboveAppToken = null;
                                                                    }
                                                                }
                                                            }
                                                            wallpaperMayMove = focusMayChange;
                                                            this.mAppTransitTrack = "relayout";
                                                            if (this.mWaitingForConfig) {
                                                            }
                                                            this.mWindowPlacerLocked.performSurfacePlacement(true);
                                                            win2 = win;
                                                            if (shouldRelayout) {
                                                            }
                                                            setHwSecureScreen(hasStatusBarServicePermission);
                                                            if (wallpaperMayMove) {
                                                            }
                                                            if ((result & 2) == 0) {
                                                            }
                                                            dc = hasStatusBarServicePermission.getDisplayContent();
                                                            if (imMayMove) {
                                                            }
                                                            if (wallpaperMayMove2) {
                                                            }
                                                            if (hasStatusBarServicePermission.mAppToken != null) {
                                                            }
                                                            origId4 = origId;
                                                            Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                                            i5 = displayId;
                                                            hasStatusBarPermission = updateOrientationFromAppTokensLocked(i5);
                                                            Trace.traceEnd(32);
                                                            if (shouldRelayout) {
                                                            }
                                                            if (focusMayChange) {
                                                            }
                                                            if (hasStatusBarServicePermission.mAppToken != null) {
                                                            }
                                                            if (winAnimator.mReportSurfaceResized) {
                                                            }
                                                            if (this.mPolicy.isNavBarForcedShownLw(hasStatusBarServicePermission)) {
                                                            }
                                                            if (hasStatusBarServicePermission.isGoneForLayoutLw()) {
                                                            }
                                                            if (shouldRelayout) {
                                                            }
                                                            hasStatusBarServicePermission.setLastReportedMergedConfiguration(mergedConfiguration3);
                                                            hasStatusBarServicePermission.updateLastInsetValues();
                                                            outFrame.set(hasStatusBarServicePermission.mCompatFrame);
                                                            outOverscanInsets.set(hasStatusBarServicePermission.mOverscanInsets);
                                                            outContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                                            hasStatusBarServicePermission.mLastRelayoutContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                                            outVisibleInsets.set(hasStatusBarServicePermission.mVisibleInsets);
                                                            outStableInsets.set(hasStatusBarServicePermission.mStableInsets);
                                                            outCutout.set(hasStatusBarServicePermission.mDisplayCutout.getDisplayCutout());
                                                            outOutsets.set(hasStatusBarServicePermission.mOutsets);
                                                            outBackdropFrame.set(hasStatusBarServicePermission.getBackdropFrame(hasStatusBarServicePermission.mFrame));
                                                            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                                            }
                                                            result |= this.mInTouchMode;
                                                            this.mInputMonitor.updateInputWindowsLw(true);
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("complete Relayout ");
                                                            stringBuilder2.append(hasStatusBarServicePermission);
                                                            stringBuilder2.append("  size:");
                                                            stringBuilder2.append(outFrame.toShortString());
                                                            Flog.i(307, stringBuilder2.toString());
                                                            hasStatusBarServicePermission.mInRelayout = false;
                                                            displayContent = getDefaultDisplayContentLocked();
                                                            displayContent.checkNeedNotifyFingerWinCovered();
                                                            displayContent.mObserveToken = null;
                                                            displayContent.mTopAboveAppToken = null;
                                                        }
                                                    }
                                                    shouldRelayout = false;
                                                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                    }
                                                    result = 0 | 4;
                                                    if (win.mWillReplaceWindow) {
                                                    }
                                                    wallpaperMayMove = focusMayChange;
                                                    this.mAppTransitTrack = "relayout";
                                                    if (this.mWaitingForConfig) {
                                                    }
                                                    this.mWindowPlacerLocked.performSurfacePlacement(true);
                                                    win2 = win;
                                                    if (shouldRelayout) {
                                                    }
                                                    setHwSecureScreen(hasStatusBarServicePermission);
                                                    if (wallpaperMayMove) {
                                                    }
                                                    if ((result & 2) == 0) {
                                                    }
                                                    dc = hasStatusBarServicePermission.getDisplayContent();
                                                    if (imMayMove) {
                                                    }
                                                    if (wallpaperMayMove2) {
                                                    }
                                                    if (hasStatusBarServicePermission.mAppToken != null) {
                                                    }
                                                    origId4 = origId;
                                                    Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                                    i5 = displayId;
                                                    hasStatusBarPermission = updateOrientationFromAppTokensLocked(i5);
                                                    Trace.traceEnd(32);
                                                    if (shouldRelayout) {
                                                    }
                                                    if (focusMayChange) {
                                                    }
                                                    if (hasStatusBarServicePermission.mAppToken != null) {
                                                    }
                                                    if (winAnimator.mReportSurfaceResized) {
                                                    }
                                                    if (this.mPolicy.isNavBarForcedShownLw(hasStatusBarServicePermission)) {
                                                    }
                                                    if (hasStatusBarServicePermission.isGoneForLayoutLw()) {
                                                    }
                                                    if (shouldRelayout) {
                                                    }
                                                    hasStatusBarServicePermission.setLastReportedMergedConfiguration(mergedConfiguration3);
                                                    hasStatusBarServicePermission.updateLastInsetValues();
                                                    outFrame.set(hasStatusBarServicePermission.mCompatFrame);
                                                    outOverscanInsets.set(hasStatusBarServicePermission.mOverscanInsets);
                                                    outContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                                    hasStatusBarServicePermission.mLastRelayoutContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                                    outVisibleInsets.set(hasStatusBarServicePermission.mVisibleInsets);
                                                    outStableInsets.set(hasStatusBarServicePermission.mStableInsets);
                                                    outCutout.set(hasStatusBarServicePermission.mDisplayCutout.getDisplayCutout());
                                                    outOutsets.set(hasStatusBarServicePermission.mOutsets);
                                                    outBackdropFrame.set(hasStatusBarServicePermission.getBackdropFrame(hasStatusBarServicePermission.mFrame));
                                                    if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                                    }
                                                    result |= this.mInTouchMode;
                                                    this.mInputMonitor.updateInputWindowsLw(true);
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("complete Relayout ");
                                                    stringBuilder2.append(hasStatusBarServicePermission);
                                                    stringBuilder2.append("  size:");
                                                    stringBuilder2.append(outFrame.toShortString());
                                                    Flog.i(307, stringBuilder2.toString());
                                                    hasStatusBarServicePermission.mInRelayout = false;
                                                    displayContent = getDefaultDisplayContentLocked();
                                                    displayContent.checkNeedNotifyFingerWinCovered();
                                                    displayContent.mObserveToken = null;
                                                    displayContent.mTopAboveAppToken = null;
                                                }
                                            }
                                            wallpaperMayMove = false;
                                            if ((i4 & DumpState.DUMP_DEXOPT) == 0) {
                                            }
                                            wallpaperMayMove |= (i4 & DumpState.DUMP_DEXOPT) == 0 ? 1 : 0;
                                            if (i4 & 8192) {
                                            }
                                            win.mRelayoutCalled = true;
                                            win.mInRelayout = true;
                                            win.mViewVisibility = i3;
                                            if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                                            }
                                            win.setDisplayLayoutNeeded();
                                            if ((flags & 1) == 0) {
                                            }
                                            win.mGivenInsetsPending = (flags & 1) == 0;
                                            if (i3 == 0) {
                                            }
                                            shouldRelayout = false;
                                            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                            }
                                            result = 0 | 4;
                                            if (win.mWillReplaceWindow) {
                                            }
                                            wallpaperMayMove = focusMayChange;
                                            this.mAppTransitTrack = "relayout";
                                            if (this.mWaitingForConfig) {
                                            }
                                            this.mWindowPlacerLocked.performSurfacePlacement(true);
                                            win2 = win;
                                            if (shouldRelayout) {
                                            }
                                            setHwSecureScreen(hasStatusBarServicePermission);
                                            if (wallpaperMayMove) {
                                            }
                                            if ((result & 2) == 0) {
                                            }
                                            dc = hasStatusBarServicePermission.getDisplayContent();
                                            if (imMayMove) {
                                            }
                                            if (wallpaperMayMove2) {
                                            }
                                            if (hasStatusBarServicePermission.mAppToken != null) {
                                            }
                                            origId4 = origId;
                                            Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                            i5 = displayId;
                                            hasStatusBarPermission = updateOrientationFromAppTokensLocked(i5);
                                            Trace.traceEnd(32);
                                            if (shouldRelayout) {
                                            }
                                            if (focusMayChange) {
                                            }
                                            if (hasStatusBarServicePermission.mAppToken != null) {
                                            }
                                            if (winAnimator.mReportSurfaceResized) {
                                            }
                                            if (this.mPolicy.isNavBarForcedShownLw(hasStatusBarServicePermission)) {
                                            }
                                            if (hasStatusBarServicePermission.isGoneForLayoutLw()) {
                                            }
                                            if (shouldRelayout) {
                                            }
                                            hasStatusBarServicePermission.setLastReportedMergedConfiguration(mergedConfiguration3);
                                            hasStatusBarServicePermission.updateLastInsetValues();
                                            outFrame.set(hasStatusBarServicePermission.mCompatFrame);
                                            outOverscanInsets.set(hasStatusBarServicePermission.mOverscanInsets);
                                            outContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                            hasStatusBarServicePermission.mLastRelayoutContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                            outVisibleInsets.set(hasStatusBarServicePermission.mVisibleInsets);
                                            outStableInsets.set(hasStatusBarServicePermission.mStableInsets);
                                            outCutout.set(hasStatusBarServicePermission.mDisplayCutout.getDisplayCutout());
                                            outOutsets.set(hasStatusBarServicePermission.mOutsets);
                                            outBackdropFrame.set(hasStatusBarServicePermission.getBackdropFrame(hasStatusBarServicePermission.mFrame));
                                            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                            }
                                            result |= this.mInTouchMode;
                                            this.mInputMonitor.updateInputWindowsLw(true);
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("complete Relayout ");
                                            stringBuilder2.append(hasStatusBarServicePermission);
                                            stringBuilder2.append("  size:");
                                            stringBuilder2.append(outFrame.toShortString());
                                            Flog.i(307, stringBuilder2.toString());
                                            hasStatusBarServicePermission.mInRelayout = false;
                                            displayContent = getDefaultDisplayContentLocked();
                                            displayContent.checkNeedNotifyFingerWinCovered();
                                            displayContent.mObserveToken = null;
                                            displayContent.mTopAboveAppToken = null;
                                        }
                                    } catch (Throwable th18) {
                                        th = th18;
                                        rect = outFrame;
                                    }
                                }
                                focusMayChange = false;
                                if (win.mViewVisibility != i3) {
                                }
                                wallpaperMayMove = false;
                                if ((i4 & DumpState.DUMP_DEXOPT) == 0) {
                                }
                                wallpaperMayMove |= (i4 & DumpState.DUMP_DEXOPT) == 0 ? 1 : 0;
                                if (i4 & 8192) {
                                }
                                win.mRelayoutCalled = true;
                                win.mInRelayout = true;
                                win.mViewVisibility = i3;
                                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                                }
                                win.setDisplayLayoutNeeded();
                                if ((flags & 1) == 0) {
                                }
                                win.mGivenInsetsPending = (flags & 1) == 0;
                                if (i3 == 0) {
                                }
                                shouldRelayout = false;
                                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                }
                                result = 0 | 4;
                                if (win.mWillReplaceWindow) {
                                }
                                wallpaperMayMove = focusMayChange;
                                try {
                                    this.mAppTransitTrack = "relayout";
                                    if (this.mWaitingForConfig) {
                                    }
                                    this.mWindowPlacerLocked.performSurfacePlacement(true);
                                    win2 = win;
                                    if (shouldRelayout) {
                                    }
                                    setHwSecureScreen(hasStatusBarServicePermission);
                                    if (wallpaperMayMove) {
                                    }
                                    if ((result & 2) == 0) {
                                    }
                                    dc = hasStatusBarServicePermission.getDisplayContent();
                                    if (imMayMove) {
                                    }
                                    if (wallpaperMayMove2) {
                                    }
                                    if (hasStatusBarServicePermission.mAppToken != null) {
                                    }
                                    origId4 = origId;
                                    Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                    i5 = displayId;
                                    hasStatusBarPermission = updateOrientationFromAppTokensLocked(i5);
                                    Trace.traceEnd(32);
                                    if (shouldRelayout) {
                                    }
                                    if (focusMayChange) {
                                    }
                                } catch (Throwable th19) {
                                    th = th19;
                                    rect = outFrame;
                                    result2 = result;
                                    j = origId3;
                                    surface2 = outSurface;
                                    while (true) {
                                        break;
                                    }
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                                try {
                                    if (hasStatusBarServicePermission.mAppToken != null) {
                                    }
                                    if (winAnimator.mReportSurfaceResized) {
                                    }
                                    if (this.mPolicy.isNavBarForcedShownLw(hasStatusBarServicePermission)) {
                                    }
                                    if (hasStatusBarServicePermission.isGoneForLayoutLw()) {
                                    }
                                    if (shouldRelayout) {
                                    }
                                    hasStatusBarServicePermission.setLastReportedMergedConfiguration(mergedConfiguration3);
                                    hasStatusBarServicePermission.updateLastInsetValues();
                                    outFrame.set(hasStatusBarServicePermission.mCompatFrame);
                                    outOverscanInsets.set(hasStatusBarServicePermission.mOverscanInsets);
                                    outContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                    hasStatusBarServicePermission.mLastRelayoutContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                                    outVisibleInsets.set(hasStatusBarServicePermission.mVisibleInsets);
                                    outStableInsets.set(hasStatusBarServicePermission.mStableInsets);
                                    outCutout.set(hasStatusBarServicePermission.mDisplayCutout.getDisplayCutout());
                                    outOutsets.set(hasStatusBarServicePermission.mOutsets);
                                    outBackdropFrame.set(hasStatusBarServicePermission.getBackdropFrame(hasStatusBarServicePermission.mFrame));
                                    if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                    }
                                } catch (Throwable th20) {
                                    th = th20;
                                    rect = outFrame;
                                    while (true) {
                                        break;
                                    }
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                                try {
                                    result |= this.mInTouchMode;
                                    this.mInputMonitor.updateInputWindowsLw(true);
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("complete Relayout ");
                                    stringBuilder2.append(hasStatusBarServicePermission);
                                    stringBuilder2.append("  size:");
                                    stringBuilder2.append(outFrame.toShortString());
                                    Flog.i(307, stringBuilder2.toString());
                                    hasStatusBarServicePermission.mInRelayout = false;
                                    displayContent = getDefaultDisplayContentLocked();
                                    displayContent.checkNeedNotifyFingerWinCovered();
                                    displayContent.mObserveToken = null;
                                    displayContent.mTopAboveAppToken = null;
                                } catch (Throwable th21) {
                                    th = th21;
                                    while (true) {
                                        break;
                                    }
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                        }
                        boolean shouldPrintLog2 = true;
                        stringBuilder2 = new StringBuilder();
                        z2 = hasStatusBarServicePermission;
                        stringBuilder2.append("start Relayout ");
                        stringBuilder2.append(win);
                        stringBuilder2.append(" oldVis=");
                        stringBuilder2.append(oldVisibility);
                        stringBuilder2.append(" newVis=");
                        stringBuilder2.append(i3);
                        stringBuilder2.append(" width=");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" height=");
                        stringBuilder2.append(i2);
                        Flog.i(307, stringBuilder2.toString());
                        shouldPrintLog = shouldPrintLog2;
                        if (oldVisibility != 4) {
                        }
                        if ((i4 & 131080) == 0) {
                        }
                        isDefaultDisplay = win.isDefaultDisplay();
                        if (isDefaultDisplay) {
                        }
                        focusMayChange = false;
                    } catch (Throwable th22) {
                        th = th22;
                        rect = outFrame;
                        z2 = hasStatusBarServicePermission;
                        j = origId3;
                        surface2 = outSurface;
                        while (true) {
                            break;
                        }
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                    try {
                        if (win.mViewVisibility != i3) {
                        }
                        wallpaperMayMove = false;
                        if ((i4 & DumpState.DUMP_DEXOPT) == 0) {
                        }
                        wallpaperMayMove |= (i4 & DumpState.DUMP_DEXOPT) == 0 ? 1 : 0;
                        if (i4 & 8192) {
                        }
                        win.mRelayoutCalled = true;
                        win.mInRelayout = true;
                        win.mViewVisibility = i3;
                        if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                        }
                        win.setDisplayLayoutNeeded();
                        if ((flags & 1) == 0) {
                        }
                        win.mGivenInsetsPending = (flags & 1) == 0;
                        if (i3 == 0) {
                        }
                        shouldRelayout = false;
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                        }
                        result = 0 | 4;
                        if (win.mWillReplaceWindow) {
                        }
                        wallpaperMayMove = focusMayChange;
                        this.mAppTransitTrack = "relayout";
                        if (this.mWaitingForConfig) {
                        }
                        this.mWindowPlacerLocked.performSurfacePlacement(true);
                        win2 = win;
                        if (shouldRelayout) {
                        }
                    } catch (Throwable th23) {
                        th = th23;
                        rect = outFrame;
                        j = origId3;
                        surface2 = outSurface;
                        while (true) {
                            break;
                        }
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                    try {
                        setHwSecureScreen(hasStatusBarServicePermission);
                        if (wallpaperMayMove) {
                        }
                        if ((result & 2) == 0) {
                        }
                        dc = hasStatusBarServicePermission.getDisplayContent();
                        if (imMayMove) {
                        }
                        if (wallpaperMayMove2) {
                        }
                        if (hasStatusBarServicePermission.mAppToken != null) {
                        }
                        origId4 = origId;
                        Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                        i5 = displayId;
                        hasStatusBarPermission = updateOrientationFromAppTokensLocked(i5);
                        Trace.traceEnd(32);
                        if (shouldRelayout) {
                        }
                        if (focusMayChange) {
                        }
                        if (hasStatusBarServicePermission.mAppToken != null) {
                        }
                        if (winAnimator.mReportSurfaceResized) {
                        }
                        if (this.mPolicy.isNavBarForcedShownLw(hasStatusBarServicePermission)) {
                        }
                        if (hasStatusBarServicePermission.isGoneForLayoutLw()) {
                        }
                        if (shouldRelayout) {
                        }
                        hasStatusBarServicePermission.setLastReportedMergedConfiguration(mergedConfiguration3);
                        hasStatusBarServicePermission.updateLastInsetValues();
                        outFrame.set(hasStatusBarServicePermission.mCompatFrame);
                        outOverscanInsets.set(hasStatusBarServicePermission.mOverscanInsets);
                        outContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                        hasStatusBarServicePermission.mLastRelayoutContentInsets.set(hasStatusBarServicePermission.mContentInsets);
                        outVisibleInsets.set(hasStatusBarServicePermission.mVisibleInsets);
                        outStableInsets.set(hasStatusBarServicePermission.mStableInsets);
                        outCutout.set(hasStatusBarServicePermission.mDisplayCutout.getDisplayCutout());
                        outOutsets.set(hasStatusBarServicePermission.mOutsets);
                        outBackdropFrame.set(hasStatusBarServicePermission.getBackdropFrame(hasStatusBarServicePermission.mFrame));
                        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                        }
                        result |= this.mInTouchMode;
                        this.mInputMonitor.updateInputWindowsLw(true);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("complete Relayout ");
                        stringBuilder2.append(hasStatusBarServicePermission);
                        stringBuilder2.append("  size:");
                        stringBuilder2.append(outFrame.toShortString());
                        Flog.i(307, stringBuilder2.toString());
                        hasStatusBarServicePermission.mInRelayout = false;
                        displayContent = getDefaultDisplayContentLocked();
                        displayContent.checkNeedNotifyFingerWinCovered();
                        displayContent.mObserveToken = null;
                        displayContent.mTopAboveAppToken = null;
                    } catch (Throwable th24) {
                        th = th24;
                        rect = outFrame;
                        j = origId;
                        while (true) {
                            break;
                        }
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th25) {
                    th = th25;
                    rect = outFrame;
                    origId2 = surface;
                    z = hasStatusBarPermission;
                    z2 = hasStatusBarServicePermission;
                    j = origId3;
                    while (true) {
                        break;
                    }
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th26) {
                th = th26;
                rect = outFrame;
                j = origId2;
                z = hasStatusBarPermission;
                z2 = hasStatusBarServicePermission;
                origId2 = surface;
                while (true) {
                    break;
                }
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private boolean tryStartExitingAnimation(WindowState win, WindowStateAnimator winAnimator, boolean isDefaultDisplay, boolean focusMayChange) {
        int transit = 2;
        if (win.mAttrs.type == 3) {
            transit = 5;
        }
        if (win.isWinVisibleLw() && !shouldHideIMExitAnim(win) && winAnimator.applyAnimationLocked(transit, false)) {
            focusMayChange = isDefaultDisplay;
            win.mAnimatingExit = true;
        } else if (win.mWinAnimator.isAnimationSet() && !shouldHideIMExitAnim(win)) {
            win.mAnimatingExit = true;
        } else if (win.getDisplayContent().mWallpaperController.isWallpaperTarget(win)) {
            win.mAnimatingExit = true;
        } else {
            if (this.mInputMethodWindow == win) {
                setInputMethodWindowLocked(null);
            }
            boolean stopped = win.mAppToken != null ? win.mAppToken.mAppStopped : true;
            win.mDestroying = true;
            win.destroySurface(false, stopped);
        }
        if (this.mAccessibilityController != null && (win.getDisplayId() == 0 || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad()))) {
            this.mAccessibilityController.onWindowTransitionLocked(win, transit);
        }
        SurfaceControl.openTransaction();
        winAnimator.detachChildren();
        SurfaceControl.closeTransaction();
        return focusMayChange;
    }

    private int createSurfaceControl(Surface outSurface, int result, WindowState win, WindowStateAnimator winAnimator) {
        if (!win.mHasSurface) {
            result |= 4;
        }
        try {
            Trace.traceBegin(32, "createSurfaceControl");
            WindowSurfaceController surfaceController = winAnimator.createSurfaceLocked(win.mAttrs.type, win.mOwnerUid);
            if (surfaceController != null) {
                surfaceController.getSurface(outSurface);
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to create surface control for ");
                stringBuilder.append(win);
                Slog.w(str, stringBuilder.toString());
                outSurface.release();
            }
            return result;
        } finally {
            Trace.traceEnd(32);
        }
    }

    public boolean outOfMemoryWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    resetPriorityAfterLockedSection();
                    Binder.restoreCallingIdentity(origId);
                    return false;
                }
                boolean reclaimSomeSurfaceMemory = this.mRoot.reclaimSomeSurfaceMemory(win.mWinAnimator, "from-client", false);
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(origId);
                return reclaimSomeSurfaceMemory;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void finishDrawingWindow(Session session, IWindow client) {
        Flog.i(307, "finishDrawingWindow...");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mWindowMap) {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, (boolean) null);
                if (win != null && win.mWinAnimator.finishDrawingLocked()) {
                    if ((win.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
                        DisplayContent displayContent = win.getDisplayContent();
                        displayContent.pendingLayoutChanges |= 4;
                    }
                    win.setDisplayLayoutNeeded();
                    this.mWindowPlacerLocked.requestTraversal();
                    this.mHwWMSEx.updateWindowReport(win, win.mRequestedWidth, win.mRequestedHeight);
                }
            }
            resetPriorityAfterLockedSection();
            Binder.restoreCallingIdentity(origId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    protected boolean isDisplayOkForAnimation(int width, int height, int transit, AppWindowToken atoken) {
        return true;
    }

    boolean checkCallingPermission(String permission, String func) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        String msg = new StringBuilder();
        msg.append("Permission Denial: ");
        msg.append(func);
        msg.append(" from pid=");
        msg.append(Binder.getCallingPid());
        msg.append(", uid=");
        msg.append(Binder.getCallingUid());
        msg.append(" requires ");
        msg.append(permission);
        Slog.w(TAG, msg.toString());
        return false;
    }

    /* JADX WARNING: Missing block: B:16:0x0063, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:17:0x0066, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addWindowToken(IBinder binder, int type, int displayId) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addWindowToken()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                    WindowToken token = dc.getWindowToken(binder);
                    if (token != null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("addWindowToken: Attempted to add binder token: ");
                        stringBuilder.append(binder);
                        stringBuilder.append(" for already created window token: ");
                        stringBuilder.append(token);
                        stringBuilder.append(" displayId=");
                        stringBuilder.append(displayId);
                        Slog.w(str, stringBuilder.toString());
                    } else if (type == 2013) {
                        WallpaperWindowToken wallpaperWindowToken = new WallpaperWindowToken(this, binder, true, dc, true);
                    } else {
                        WindowToken windowToken = new WindowToken(this, binder, type, true, dc, true);
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
    }

    protected boolean isTokenFound(IBinder binder, DisplayContent dc) {
        return false;
    }

    protected void setFocusedDisplay(int displayId, boolean findTopTask, String reason) {
    }

    public void removeWindowToken(IBinder binder, int displayId) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeWindowToken()")) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                    if (dc == null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("removeWindowToken: Attempted to remove token: ");
                        stringBuilder.append(binder);
                        stringBuilder.append(" for non-exiting displayId=");
                        stringBuilder.append(displayId);
                        Slog.w(str, stringBuilder.toString());
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(origId);
                    } else if (dc.removeWindowToken(binder) != null || isTokenFound(binder, dc)) {
                        this.mInputMonitor.updateInputWindowsLw(true);
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(origId);
                    } else {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("removeWindowToken: Attempted to remove non-existing token: ");
                        stringBuilder2.append(binder);
                        Slog.w(str2, stringBuilder2.toString());
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(origId);
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
    }

    public Configuration updateOrientationFromAppTokens(Configuration currentConfig, IBinder freezeThisOneIfNeeded, int displayId) {
        return updateOrientationFromAppTokens(currentConfig, freezeThisOneIfNeeded, displayId, false);
    }

    public Configuration updateOrientationFromAppTokens(Configuration currentConfig, IBinder freezeThisOneIfNeeded, int displayId, boolean forceUpdate) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "updateOrientationFromAppTokens()")) {
            long ident = Binder.clearCallingIdentity();
            try {
                Configuration config;
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    config = updateOrientationFromAppTokensLocked(currentConfig, freezeThisOneIfNeeded, displayId, forceUpdate);
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
                return config;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
    }

    private Configuration updateOrientationFromAppTokensLocked(Configuration currentConfig, IBinder freezeThisOneIfNeeded, int displayId, boolean forceUpdate) {
        if (this.mDisplayReady) {
            Configuration config = null;
            if (updateOrientationFromAppTokensLocked(displayId, forceUpdate)) {
                if (!(freezeThisOneIfNeeded == null || this.mRoot.mOrientationChangeComplete)) {
                    AppWindowToken atoken = this.mRoot.getAppWindowToken(freezeThisOneIfNeeded);
                    if (atoken != null) {
                        atoken.startFreezingScreen();
                    }
                }
                config = computeNewConfigurationLocked(displayId);
            } else if (currentConfig != null) {
                this.mTempConfiguration.unset();
                this.mTempConfiguration.updateFrom(currentConfig);
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                displayContent.computeScreenConfiguration(this.mTempConfiguration);
                if (currentConfig.diff(this.mTempConfiguration) != 0) {
                    this.mWaitingForConfig = true;
                    displayContent.setLayoutNeeded();
                    int[] anim = new int[2];
                    this.mPolicy.selectRotationAnimationLw(anim);
                    if (!this.mIgnoreFrozen) {
                        startFreezingDisplayLocked(anim[0], anim[1], displayContent);
                    }
                    config = new Configuration(this.mTempConfiguration);
                }
            }
            if (this.mIgnoreFrozen) {
                this.mIgnoreFrozen = false;
            }
            return config;
        }
        Flog.i(308, "display is not ready");
        return null;
    }

    boolean updateOrientationFromAppTokensLocked(int displayId) {
        return updateOrientationFromAppTokensLocked(displayId, false);
    }

    boolean updateOrientationFromAppTokensLocked(int displayId, boolean forceUpdate) {
        long ident = Binder.clearCallingIdentity();
        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
        boolean z = false;
        if (dc == null) {
            return z;
        }
        if (HwVRUtils.isVRMode() && HwVRUtils.isValidVRDisplayId(displayId)) {
            Binder.restoreCallingIdentity(ident);
            return z;
        }
        try {
            int req = dc.getOrientation();
            if (req != dc.getLastOrientation() || forceUpdate) {
                startIntelliServiceFR(req);
                dc.setLastOrientation(req);
                if (dc.isDefaultDisplay) {
                    this.mPolicy.setCurrentOrientationLw(req);
                }
                z = dc.updateRotationUnchecked(forceUpdate);
                Binder.restoreCallingIdentity(ident);
                return z;
            }
            Binder.restoreCallingIdentity(ident);
            return z;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean rotationNeedsUpdateLocked() {
        DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
        int lastOrientation = defaultDisplayContent.getLastOrientation();
        int oldRotation = defaultDisplayContent.getRotation();
        boolean oldAltOrientation = defaultDisplayContent.getAltOrientation();
        int rotation = this.mPolicy.rotationForOrientationLw(lastOrientation, oldRotation, true);
        boolean altOrientation = this.mPolicy.rotationHasCompatibleMetricsLw(lastOrientation, rotation) ^ true;
        if (oldRotation == rotation && oldAltOrientation == altOrientation) {
            return false;
        }
        return true;
    }

    public int[] setNewDisplayOverrideConfiguration(Configuration overrideConfig, int displayId) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setNewDisplayOverrideConfiguration()")) {
            int[] displayOverrideConfigurationIfNeeded;
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mHwWMSEx.handleNewDisplayConfiguration(overrideConfig, displayId);
                    if (this.mWaitingForConfig) {
                        this.mWaitingForConfig = false;
                        this.mLastFinishedFreezeSource = "new-config";
                    }
                    displayOverrideConfigurationIfNeeded = this.mRoot.setDisplayOverrideConfigurationIfNeeded(overrideConfig, displayId);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return displayOverrideConfigurationIfNeeded;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    void setFocusTaskRegionLocked(AppWindowToken previousFocus) {
        Task focusedTask = this.mFocusedApp != null ? this.mFocusedApp.getTask() : null;
        Task previousTask = previousFocus != null ? previousFocus.getTask() : null;
        DisplayContent focusedDisplayContent = focusedTask != null ? focusedTask.getDisplayContent() : null;
        DisplayContent previousDisplayContent = previousTask != null ? previousTask.getDisplayContent() : null;
        if (HwPCUtils.isPcCastModeInServer()) {
            int count = this.mRoot.mChildren.size();
            for (int j = 0; j < count; j++) {
                DisplayContent dc = (DisplayContent) this.mRoot.mChildren.get(j);
                if (focusedDisplayContent == null || focusedDisplayContent.getDisplayId() != dc.getDisplayId()) {
                    dc.setTouchExcludeRegion(null);
                } else {
                    dc.setTouchExcludeRegion(focusedTask);
                }
            }
            return;
        }
        if (!(previousDisplayContent == null || previousDisplayContent == focusedDisplayContent)) {
            previousDisplayContent.setTouchExcludeRegion(null);
        }
        if (focusedDisplayContent != null) {
            focusedDisplayContent.setTouchExcludeRegion(focusedTask);
        }
    }

    public void setFocusedApp(IBinder token, boolean moveFocusNow) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setFocusedApp()")) {
            synchronized (this.mWindowMap) {
                try {
                    AppWindowToken newFocus;
                    boostPriorityForLockedSection();
                    if (token == null) {
                        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Clearing focused app, was ");
                            stringBuilder.append(this.mFocusedApp);
                            Slog.v(str, stringBuilder.toString());
                        }
                        newFocus = null;
                    } else {
                        String str2;
                        StringBuilder stringBuilder2;
                        newFocus = this.mRoot.getAppWindowToken(token);
                        if (newFocus == null) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Attempted to set focus to non-existing app token: ");
                            stringBuilder2.append(token);
                            Slog.w(str2, stringBuilder2.toString());
                        }
                        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Set focused app to: ");
                            stringBuilder2.append(newFocus);
                            stringBuilder2.append(" old focus=");
                            stringBuilder2.append(this.mFocusedApp);
                            stringBuilder2.append(" moveFocusNow=");
                            stringBuilder2.append(moveFocusNow);
                            Slog.v(str2, stringBuilder2.toString());
                        }
                    }
                    this.mHwWMSEx.checkSingleHandMode(this.mFocusedApp, newFocus);
                    boolean changed = this.mFocusedApp != newFocus;
                    if (changed) {
                        AppWindowToken prev = this.mFocusedApp;
                        this.mFocusedApp = newFocus;
                        if (!(!HwPCUtils.isPcCastModeInServer() || this.mFocusedApp == null || this.mFocusedApp.getDisplayContent().isDefaultDisplay)) {
                            setPCLauncherFocused(false);
                        }
                        this.mInputMonitor.setFocusedAppLw(newFocus);
                        setFocusTaskRegionLocked(prev);
                    }
                    if (moveFocusNow && changed) {
                        long origId = Binder.clearCallingIdentity();
                        updateFocusedWindowLocked(0, true);
                        Binder.restoreCallingIdentity(origId);
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    public void prepareAppTransition(int transit, boolean alwaysKeepCurrent) {
        prepareAppTransition(transit, alwaysKeepCurrent, 0, false);
    }

    public void prepareAppTransition(int transit, boolean alwaysKeepCurrent, int flags, boolean forceOverride) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "prepareAppTransition()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    boolean prepared = this.mAppTransition.prepareAppTransitionLocked(transit, alwaysKeepCurrent, flags, forceOverride);
                    DisplayContent dc = this.mRoot.getDisplayContent(0);
                    if (prepared && dc != null && dc.okToAnimate()) {
                        this.mSkipAppTransitionAnimation = false;
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    public int getPendingAppTransition() {
        return this.mAppTransition.getAppTransition();
    }

    public void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, IRemoteCallback startedCallback) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransition(packageName, enterAnim, exitAnim, startedCallback);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionScaleUp(startX, startY, startWidth, startHeight);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionClipReveal(int startX, int startY, int startWidth, int startHeight) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionClipReveal(startX, startY, startWidth, startHeight);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionThumb(GraphicBuffer srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionThumb(srcThumb, startX, startY, startedCallback, scaleUp);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionAspectScaledThumb(srcThumb, startX, startY, targetWidth, targetHeight, startedCallback, scaleUp);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] specs, IRemoteCallback onAnimationStartedCallback, IRemoteCallback onAnimationFinishedCallback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionMultiThumb(specs, onAnimationStartedCallback, onAnimationFinishedCallback, scaleUp);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionStartCrossProfileApps() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionStartCrossProfileApps();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionInPlace(String packageName, int anim) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overrideInPlaceAppTransition(packageName, anim);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAppTransition.overridePendingAppTransitionMultiThumbFuture(specsFuture, callback, scaleUp);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) {
        if (checkCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "overridePendingAppTransitionRemote()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mAppTransition.overridePendingAppTransitionRemote(remoteAnimationAdapter);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS permission");
    }

    public void endProlongedAnimations() {
    }

    public void executeAppTransition() {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "executeAppTransition()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Execute app transition: ");
                        stringBuilder.append(this.mAppTransition);
                        stringBuilder.append(" Callers=");
                        stringBuilder.append(Debug.getCallers(5));
                        Flog.i(307, stringBuilder.toString());
                    }
                    if (this.mAppTransition.isTransitionSet()) {
                        this.mAppTransition.setReady();
                        this.mWindowPlacerLocked.requestTraversal();
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    public void initializeRecentsAnimation(int targetActivityType, IRecentsAnimationRunner recentsAnimationRunner, RecentsAnimationCallbacks callbacks, int displayId, SparseBooleanArray recentTaskIds) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRecentsAnimationController = new RecentsAnimationController(this, recentsAnimationRunner, callbacks, displayId);
                this.mAppTransition.updateBooster();
                this.mRecentsAnimationController.initialize(targetActivityType, recentTaskIds);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public RecentsAnimationController getRecentsAnimationController() {
        return this.mRecentsAnimationController;
    }

    public boolean canStartRecentsAnimation() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (!this.mAppTransition.isTransitionSet()) {
                    resetPriorityAfterLockedSection();
                    return true;
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return false;
    }

    public void cancelRecentsAnimationSynchronously(@ReorderMode int reorderMode, String reason) {
        if (this.mRecentsAnimationController != null) {
            this.mRecentsAnimationController.cancelAnimationSynchronously(reorderMode, reason);
        }
    }

    public void cleanupRecentsAnimation(@ReorderMode int reorderMode) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mRecentsAnimationController != null) {
                    this.mRecentsAnimationController.cleanupAnimation(reorderMode);
                    this.mRecentsAnimationController = null;
                    this.mAppTransition.updateBooster();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setAppFullscreen(IBinder token, boolean toOpaque) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken atoken = this.mRoot.getAppWindowToken(token);
                if (atoken != null) {
                    atoken.setFillsParent(toOpaque);
                    setWindowOpaqueLocked(token, toOpaque);
                    this.mWindowPlacerLocked.requestTraversal();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setWindowOpaque(IBinder token, boolean isOpaque) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                setWindowOpaqueLocked(token, isOpaque);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    private void setWindowOpaqueLocked(IBinder token, boolean isOpaque) {
        AppWindowToken wtoken = this.mRoot.getAppWindowToken(token);
        if (wtoken != null) {
            WindowState win = wtoken.findMainWindow();
            if (win != null) {
                win.mWinAnimator.setOpaqueLocked(isOpaque);
            }
        }
    }

    public void setDockedStackCreateState(int mode, Rect bounds) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                setDockedStackCreateStateLocked(mode, bounds);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void setDockedStackCreateStateLocked(int mode, Rect bounds) {
        this.mDockedStackCreateMode = mode;
        this.mDockedStackCreateBounds = bounds;
    }

    public void checkSplitScreenMinimizedChanged(boolean animate) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().checkMinimizeChanged(animate);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isValidPictureInPictureAspectRatio(int displayId, float aspectRatio) {
        return this.mRoot.getDisplayContent(displayId).getPinnedStackController().isValidPictureInPictureAspectRatio(aspectRatio);
    }

    public void getStackBounds(int windowingMode, int activityType, Rect bounds) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                TaskStack stack = this.mRoot.getStack(windowingMode, activityType);
                if (stack != null) {
                    stack.getBounds(bounds);
                } else {
                    bounds.setEmpty();
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyShowingDreamChanged() {
        notifyKeyguardFlagsChanged(null);
    }

    public WindowState getInputMethodWindowLw() {
        return this.mInputMethodWindow;
    }

    public void notifyKeyguardTrustedChanged() {
        this.mH.sendEmptyMessage(57);
    }

    public void screenTurningOff(ScreenOffListener listener) {
        this.mTaskSnapshotController.screenTurningOff(listener);
    }

    public void triggerAnimationFailsafe() {
        this.mH.sendEmptyMessage(60);
    }

    public void onKeyguardShowingAndNotOccludedChanged() {
        this.mH.sendEmptyMessage(61);
    }

    public void deferSurfaceLayout() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.deferLayout();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void continueSurfaceLayout() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.continueLayout();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean containsShowWhenLockedWindow(IBinder token) {
        boolean z;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken wtoken = this.mRoot.getAppWindowToken(token);
                z = wtoken != null && wtoken.containsShowWhenLockedWindow();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    public boolean containsDismissKeyguardWindow(IBinder token) {
        boolean z;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken wtoken = this.mRoot.getAppWindowToken(token);
                z = wtoken != null && wtoken.containsDismissKeyguardWindow();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    void notifyKeyguardFlagsChanged(Runnable callback) {
        Runnable wrappedCallback;
        if (callback != null) {
            wrappedCallback = new -$$Lambda$WindowManagerService$5dMkMeana3BB2vTfpghrIR2jQMg(this, callback);
        } else {
            wrappedCallback = null;
        }
        this.mH.obtainMessage(56, wrappedCallback).sendToTarget();
    }

    public static /* synthetic */ void lambda$notifyKeyguardFlagsChanged$1(WindowManagerService windowManagerService, Runnable callback) {
        synchronized (windowManagerService.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                callback.run();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isKeyguardTrusted() {
        boolean isKeyguardTrustedLw;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                isKeyguardTrustedLw = this.mPolicy.isKeyguardTrustedLw();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return isKeyguardTrustedLw;
    }

    public void setKeyguardGoingAway(boolean keyguardGoingAway) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mKeyguardGoingAway = keyguardGoingAway;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setKeyguardOrAodShowingOnDefaultDisplay(boolean showing) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mKeyguardOrAodShowingOnDefaultDisplay = showing;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void startFreezingScreen(int exitAnim, int enterAnim) {
        if (checkCallingPermission("android.permission.FREEZE_SCREEN", "startFreezingScreen()")) {
            synchronized (this.mWindowMap) {
                long origId;
                try {
                    boostPriorityForLockedSection();
                    if (!this.mClientFreezingScreen) {
                        this.mClientFreezingScreen = true;
                        origId = Binder.clearCallingIdentity();
                        startFreezingDisplayLocked(exitAnim, enterAnim);
                        this.mH.removeMessages(30);
                        this.mH.sendEmptyMessageDelayed(30, 5000);
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    while (true) {
                        resetPriorityAfterLockedSection();
                    }
                }
            }
            resetPriorityAfterLockedSection();
            return;
        }
        throw new SecurityException("Requires FREEZE_SCREEN permission");
    }

    public void stopFreezingScreen() {
        if (checkCallingPermission("android.permission.FREEZE_SCREEN", "stopFreezingScreen()")) {
            synchronized (this.mWindowMap) {
                long origId;
                try {
                    boostPriorityForLockedSection();
                    if (this.mClientFreezingScreen) {
                        this.mClientFreezingScreen = false;
                        this.mLastFinishedFreezeSource = "client";
                        origId = Binder.clearCallingIdentity();
                        stopFreezingDisplayLocked();
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    while (true) {
                        resetPriorityAfterLockedSection();
                    }
                }
            }
            resetPriorityAfterLockedSection();
            return;
        }
        throw new SecurityException("Requires FREEZE_SCREEN permission");
    }

    public void disableKeyguard(IBinder token, String tag) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_DISABLEKEYGUARD);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        } else if (Binder.getCallingUid() != 1000 && isKeyguardSecure()) {
            Log.d(TAG, "current mode is SecurityMode, ignore disableKeyguard");
        } else if (!isCurrentProfileLocked(UserHandle.getCallingUserId())) {
            Log.d(TAG, "non-current profiles, ignore disableKeyguard");
        } else if (token != null) {
            this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(1, new Pair(token, tag)));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("disableKeyguard pid = ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(" ,callers = ");
            stringBuilder.append(Debug.getCallers(5));
            Slog.i(str, stringBuilder.toString());
        } else {
            throw new IllegalArgumentException("token == null");
        }
    }

    public void reenableKeyguard(IBinder token) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_REENABLEKEYGUARD);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            Log.e(TAG, "SecurityException is in reenableKeyguard!");
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        } else if (token != null) {
            this.mKeyguardDisableHandler.sendMessage(this.mKeyguardDisableHandler.obtainMessage(2, token));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reenableKeyguard pid = ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(" ,callers = ");
            stringBuilder.append(Debug.getCallers(5));
            Slog.i(str, stringBuilder.toString());
        } else {
            Log.e(TAG, "IllegalArgumentException is in reenableKeyguard!");
            throw new IllegalArgumentException("token == null");
        }
    }

    public void exitKeyguardSecurely(final IOnKeyguardExitResult callback) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_EXITKEYGUARDSECURELY);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        } else if (callback != null) {
            this.mPolicy.exitKeyguardSecurely(new OnKeyguardExitResult() {
                public void onKeyguardExitResult(boolean success) {
                    try {
                        callback.onKeyguardExitResult(success);
                    } catch (RemoteException e) {
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("callback == null");
        }
    }

    public boolean isKeyguardLocked() {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_ISKEYGUARDLOCKED);
        return this.mPolicy.isKeyguardLocked();
    }

    public boolean isKeyguardShowingAndNotOccluded() {
        return this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public boolean isKeyguardSecure() {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_ISKEYGUARDSECURE);
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        try {
            boolean isKeyguardSecure = this.mPolicy.isKeyguardSecure(userId);
            return isKeyguardSecure;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isShowingDream() {
        boolean isShowingDreamLw;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                isShowingDreamLw = this.mPolicy.isShowingDreamLw();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return isShowingDreamLw;
    }

    public void dismissKeyguard(IKeyguardDismissCallback callback, CharSequence message) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_DISMISSKEYGUARD);
        if (checkCallingPermission("android.permission.CONTROL_KEYGUARD", "dismissKeyguard")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mPolicy.dismissKeyguardLw(callback, message);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires CONTROL_KEYGUARD permission");
    }

    public void onKeyguardOccludedChanged(boolean occluded) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onKeyguardOccludedChangedLw(occluded);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setSwitchingUser(boolean switching) {
        if (checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "setSwitchingUser()")) {
            this.mPolicy.setSwitchingUser(switching);
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mSwitchingUser = switching;
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires INTERACT_ACROSS_USERS_FULL permission");
    }

    void showGlobalActions() {
        this.mPolicy.showGlobalActions();
    }

    public void closeSystemDialogs(String reason) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.closeSystemDialogs(reason);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    static float fixScale(float scale) {
        if (scale < 0.0f) {
            scale = 0.0f;
        } else if (scale > 20.0f) {
            scale = 20.0f;
        }
        return Math.abs(scale);
    }

    public void setAnimationScale(int which, float scale) {
        if (checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            scale = fixScale(scale);
            switch (which) {
                case 0:
                    this.mWindowAnimationScaleSetting = scale;
                    break;
                case 1:
                    this.mTransitionAnimationScaleSetting = scale;
                    break;
                case 2:
                    this.mAnimatorDurationScaleSetting = scale;
                    break;
            }
            this.mH.sendEmptyMessage(14);
            return;
        }
        throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
    }

    public void setAnimationScales(float[] scales) {
        if (checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            if (scales != null) {
                if (scales.length >= 1) {
                    this.mWindowAnimationScaleSetting = fixScale(scales[0]);
                }
                if (scales.length >= 2) {
                    this.mTransitionAnimationScaleSetting = fixScale(scales[1]);
                }
                if (scales.length >= 3) {
                    this.mAnimatorDurationScaleSetting = fixScale(scales[2]);
                    dispatchNewAnimatorScaleLocked(null);
                }
            }
            this.mH.sendEmptyMessage(14);
            return;
        }
        throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
    }

    private void setAnimatorDurationScale(float scale) {
        this.mAnimatorDurationScaleSetting = scale;
        ValueAnimator.setDurationScale(scale);
    }

    public float getWindowAnimationScaleLocked() {
        return this.mAnimationsDisabled ? 0.0f : this.mWindowAnimationScaleSetting;
    }

    public float getTransitionAnimationScaleLocked() {
        return this.mAnimationsDisabled ? 0.0f : this.mTransitionAnimationScaleSetting;
    }

    public float getAnimationScale(int which) {
        switch (which) {
            case 0:
                return this.mWindowAnimationScaleSetting;
            case 1:
                return this.mTransitionAnimationScaleSetting;
            case 2:
                return this.mAnimatorDurationScaleSetting;
            default:
                return 0.0f;
        }
    }

    public float[] getAnimationScales() {
        return new float[]{this.mWindowAnimationScaleSetting, this.mTransitionAnimationScaleSetting, this.mAnimatorDurationScaleSetting};
    }

    public float getCurrentAnimatorScale() {
        float f;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                f = this.mAnimationsDisabled ? 0.0f : this.mAnimatorDurationScaleSetting;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return f;
    }

    void dispatchNewAnimatorScaleLocked(Session session) {
        this.mH.obtainMessage(34, session).sendToTarget();
    }

    public void registerPointerEventListener(PointerEventListener listener) {
        if (listener != null) {
            this.mPointerEventDispatcher.registerInputEventListener(listener);
        }
    }

    public void unregisterPointerEventListener(PointerEventListener listener) {
        this.mPointerEventDispatcher.unregisterInputEventListener(listener);
    }

    boolean canDispatchPointerEvents() {
        return this.mPointerEventDispatcher != null;
    }

    public void registerExternalPointerEventListener(PointerEventListener listener) {
        if (listener != null) {
            this.mExternalPointerEventDispatcher.registerInputEventListener(listener);
        }
    }

    public void unregisterExternalPointerEventListener(PointerEventListener listener) {
        this.mExternalPointerEventDispatcher.unregisterInputEventListener(listener);
    }

    public int getFocusedDisplayId() {
        return 0;
    }

    boolean canDispatchExternalPointerEvents() {
        return this.mExternalPointerEventDispatcher != null;
    }

    public int getLidState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 0);
        if (sw > 0) {
            return 0;
        }
        if (sw == 0) {
            return 1;
        }
        return -1;
    }

    public void lockDeviceNow() {
        lockNow(null);
    }

    public int getCameraLensCoverState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 9);
        if (sw > 0) {
            return 1;
        }
        if (sw == 0) {
            return 0;
        }
        return -1;
    }

    public void switchKeyboardLayout(int deviceId, int direction) {
        this.mInputManager.switchKeyboardLayout(deviceId, direction);
    }

    public void switchInputMethod(boolean forwardDirection) {
        InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        if (inputMethodManagerInternal != null) {
            inputMethodManagerInternal.switchInputMethod(forwardDirection);
        }
    }

    public void shutdown(boolean confirm) {
        ShutdownThread.shutdown(ActivityThread.currentActivityThread().getSystemUiContext(), "userrequested", confirm);
    }

    public void reboot(boolean confirm) {
        ShutdownThread.reboot(ActivityThread.currentActivityThread().getSystemUiContext(), "userrequested", confirm);
    }

    public void rebootSafeMode(boolean confirm) {
        ShutdownThread.rebootSafeMode(ActivityThread.currentActivityThread().getSystemUiContext(), confirm);
    }

    public void setCurrentProfileIds(int[] currentProfileIds) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentProfileIds = currentProfileIds;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentUserId = newUserId;
                this.mCurrentProfileIds = currentProfileIds;
                this.mAppTransition.setCurrentUser(newUserId);
                this.mPolicy.setCurrentUserLw(newUserId);
                boolean z = true;
                this.mPolicy.enableKeyguard(true);
                this.mRoot.switchUser();
                this.mWindowPlacerLocked.performSurfacePlacement();
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                TaskStack stack = displayContent.getSplitScreenPrimaryStackIgnoringVisibility();
                DockedStackDividerController dockedStackDividerController = displayContent.mDividerControllerLocked;
                if (stack == null || !stack.hasTaskForUser(newUserId)) {
                    z = false;
                }
                dockedStackDividerController.notifyDockedStackExistsChanged(z);
                if (this.mDisplayReady) {
                    int forcedDensity = getForcedDisplayDensityForUserLocked(newUserId);
                    setForcedDisplayDensityLocked(displayContent, forcedDensity != 0 ? forcedDensity : displayContent.mInitialDisplayDensity);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUserId) {
            return true;
        }
        for (int i : this.mCurrentProfileIds) {
            if (i == userId) {
                return true;
            }
        }
        return false;
    }

    public void enableScreenAfterBoot() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_BOOT) {
                    RuntimeException here = new RuntimeException("here");
                    here.fillInStackTrace();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("enableScreenAfterBoot: mDisplayEnabled=");
                    stringBuilder.append(this.mDisplayEnabled);
                    stringBuilder.append(" mForceDisplayEnabled=");
                    stringBuilder.append(this.mForceDisplayEnabled);
                    stringBuilder.append(" mShowingBootMessages=");
                    stringBuilder.append(this.mShowingBootMessages);
                    stringBuilder.append(" mSystemBooted=");
                    stringBuilder.append(this.mSystemBooted);
                    Slog.i(str, stringBuilder.toString(), here);
                }
                if (this.mSystemBooted) {
                } else {
                    this.mSystemBooted = true;
                    hideBootMessagesLocked();
                    this.mH.sendEmptyMessageDelayed(23, 30000);
                    resetPriorityAfterLockedSection();
                    this.mPolicy.systemBooted();
                    performEnableScreen();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void enableScreenIfNeeded() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                enableScreenIfNeededLocked();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void enableScreenIfNeededLocked() {
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableScreenIfNeededLocked: mDisplayEnabled=");
            stringBuilder.append(this.mDisplayEnabled);
            stringBuilder.append(" mForceDisplayEnabled=");
            stringBuilder.append(this.mForceDisplayEnabled);
            stringBuilder.append(" mShowingBootMessages=");
            stringBuilder.append(this.mShowingBootMessages);
            stringBuilder.append(" mSystemBooted=");
            stringBuilder.append(this.mSystemBooted);
            Slog.i(str, stringBuilder.toString(), here);
        }
        if (!this.mDisplayEnabled) {
            if (this.mSystemBooted || this.mShowingBootMessages) {
                this.mH.sendEmptyMessage(16);
            }
        }
    }

    public void performBootTimeout() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayEnabled) {
                } else {
                    Slog.w(TAG, "***** BOOT TIMEOUT: forcing display enabled");
                    this.mForceDisplayEnabled = true;
                    resetPriorityAfterLockedSection();
                    performEnableScreen();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onSystemUiStarted() {
        this.mPolicy.onSystemUiStarted();
    }

    /* JADX WARNING: Missing block: B:64:0x011d, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:66:?, code:
            r9.mActivityManager.bootAnimationComplete();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void performEnableScreen() {
        synchronized (this.mWindowMap) {
            boostPriorityForLockedSection();
            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("performEnableScreen: mDisplayEnabled=");
                stringBuilder.append(this.mDisplayEnabled);
                stringBuilder.append(" mForceDisplayEnabled=");
                stringBuilder.append(this.mForceDisplayEnabled);
                stringBuilder.append(" mShowingBootMessages=");
                stringBuilder.append(this.mShowingBootMessages);
                stringBuilder.append(" mSystemBooted=");
                stringBuilder.append(this.mSystemBooted);
                stringBuilder.append(" mOnlyCore=");
                stringBuilder.append(this.mOnlyCore);
                Slog.i(str, stringBuilder.toString(), new RuntimeException("here").fillInStackTrace());
            }
            if (this.mDisplayEnabled) {
                resetPriorityAfterLockedSection();
                return;
            } else if (this.mSystemBooted || this.mShowingBootMessages) {
                try {
                    if (!this.mShowingBootMessages && !this.mPolicy.canDismissBootAnimation()) {
                        Flog.i(305, "Keyguard not drawn complete,can not dismiss boot animation");
                        resetPriorityAfterLockedSection();
                        return;
                    } else if (this.mForceDisplayEnabled || !getDefaultDisplayContentLocked().checkWaitingForWindows()) {
                        if (!this.mBootAnimationStopped) {
                            Trace.asyncTraceBegin(32, "Stop bootanim", 0);
                            SystemProperties.set("service.bootanim.exit", "1");
                            this.mBootAnimationStopped = true;
                        }
                        if (this.mForceDisplayEnabled || checkBootAnimationCompleteLocked()) {
                            IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                            if (surfaceFlinger != null) {
                                Flog.i(304, "******* TELLING SURFACE FLINGER WE ARE BOOTED!");
                                Parcel data = Parcel.obtain();
                                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                                surfaceFlinger.transact(1, data, null, 0);
                                data.recycle();
                            } else {
                                Flog.i(304, "performEnableScreen: SurfaceFlinger is dead!");
                            }
                            EventLog.writeEvent(EventLogTags.WM_BOOT_ANIMATION_DONE, SystemClock.uptimeMillis());
                            Trace.asyncTraceEnd(32, "Stop bootanim", 0);
                            this.mDisplayEnabled = true;
                            if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
                                Slog.i(TAG, "******************** ENABLING SCREEN!");
                            }
                            this.mInputMonitor.setEventDispatchingLw(this.mEventDispatchingEnabled);
                        } else {
                            Flog.i(304, "Waiting for anim complete");
                            resetPriorityAfterLockedSection();
                            return;
                        }
                    } else {
                        Flog.i(304, "Waiting for all visiable windows drawn");
                        resetPriorityAfterLockedSection();
                        return;
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Boot completed: SurfaceFlinger is dead!");
                } catch (Throwable th) {
                    while (true) {
                        resetPriorityAfterLockedSection();
                    }
                }
            } else {
                resetPriorityAfterLockedSection();
                return;
            }
        }
        this.mPolicy.enableScreenAfterBoot();
        updateRotationUnchecked(false, false);
    }

    private boolean checkBootAnimationCompleteLocked() {
        if (SystemService.isRunning(BOOT_ANIMATION_SERVICE)) {
            this.mH.removeMessages(37);
            this.mH.sendEmptyMessageDelayed(37, 200);
            if (WindowManagerDebugConfig.DEBUG_BOOT) {
                Slog.i(TAG, "checkBootAnimationComplete: Waiting for anim complete");
            }
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            Slog.i(TAG, "checkBootAnimationComplete: Animation complete!");
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:28:0x0079, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:29:0x007c, code:
            if (r0 == false) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:30:0x007e, code:
            performEnableScreen();
     */
    /* JADX WARNING: Missing block: B:31:0x0081, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void showBootMessage(CharSequence msg, boolean always) {
        boolean first = false;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_BOOT) {
                    RuntimeException here = new RuntimeException("here");
                    here.fillInStackTrace();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("showBootMessage: msg=");
                    stringBuilder.append(msg);
                    stringBuilder.append(" always=");
                    stringBuilder.append(always);
                    stringBuilder.append(" mAllowBootMessages=");
                    stringBuilder.append(this.mAllowBootMessages);
                    stringBuilder.append(" mShowingBootMessages=");
                    stringBuilder.append(this.mShowingBootMessages);
                    stringBuilder.append(" mSystemBooted=");
                    stringBuilder.append(this.mSystemBooted);
                    Slog.i(str, stringBuilder.toString(), here);
                }
                if (this.mAllowBootMessages) {
                    if (!this.mShowingBootMessages) {
                        if (always) {
                            first = true;
                        } else {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                    }
                    if (this.mSystemBooted) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mShowingBootMessages = true;
                    this.mPolicy.showBootMessage(msg, always);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void hideBootMessagesLocked() {
        if (WindowManagerDebugConfig.DEBUG_BOOT) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hideBootMessagesLocked: mDisplayEnabled=");
            stringBuilder.append(this.mDisplayEnabled);
            stringBuilder.append(" mForceDisplayEnabled=");
            stringBuilder.append(this.mForceDisplayEnabled);
            stringBuilder.append(" mShowingBootMessages=");
            stringBuilder.append(this.mShowingBootMessages);
            stringBuilder.append(" mSystemBooted=");
            stringBuilder.append(this.mSystemBooted);
            Slog.i(str, stringBuilder.toString(), here);
        }
        if (this.mShowingBootMessages) {
            this.mShowingBootMessages = false;
            this.mPolicy.hideBootMessages();
        }
    }

    public void setInTouchMode(boolean mode) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mInTouchMode = mode;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    private void updateCircularDisplayMaskIfNeeded() {
        if (this.mContext.getResources().getConfiguration().isScreenRound() && this.mContext.getResources().getBoolean(17957094)) {
            int currentUserId;
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    currentUserId = this.mCurrentUserId;
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            int showMask = 1;
            if (Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, currentUserId) == 1) {
                showMask = 0;
            }
            Message m = this.mH.obtainMessage(35);
            m.arg1 = showMask;
            this.mH.sendMessage(m);
        }
    }

    public void showEmulatorDisplayOverlayIfNeeded() {
        if (this.mContext.getResources().getBoolean(17957090) && SystemProperties.getBoolean(PROPERTY_EMULATOR_CIRCULAR, false) && Build.IS_EMULATOR) {
            this.mH.sendMessage(this.mH.obtainMessage(36));
        }
    }

    public void showCircularMask(boolean visible) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                openSurfaceTransaction();
                if (visible) {
                    if (this.mCircularDisplayMask == null) {
                        this.mCircularDisplayMask = new CircularDisplayMask(getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10, this.mContext.getResources().getInteger(17694930), this.mContext.getResources().getDimensionPixelSize(17104956));
                    }
                    this.mCircularDisplayMask.setVisibility(true);
                } else if (this.mCircularDisplayMask != null) {
                    this.mCircularDisplayMask.setVisibility(false);
                    this.mCircularDisplayMask = null;
                }
                closeSurfaceTransaction("showCircularMask");
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setExitInfo(float pivotX, float pivotY, int iconWidth, int iconHeight, Bitmap iconBitmap, int flag) {
        this.mExitIconWidth = iconBitmap != null ? iconBitmap.getWidth() : iconWidth;
        this.mExitIconHeight = iconBitmap != null ? iconBitmap.getHeight() : iconHeight;
        this.mExitPivotX = pivotX;
        this.mExitPivotY = pivotY;
        this.mExitIconBitmap = iconBitmap;
        this.mExitFlag = flag;
    }

    public void showEmulatorDisplayOverlay() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                openSurfaceTransaction();
                if (this.mEmulatorDisplayOverlay == null) {
                    this.mEmulatorDisplayOverlay = new EmulatorDisplayOverlay(this.mContext, getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10);
                }
                this.mEmulatorDisplayOverlay.setVisibility(true);
                closeSurfaceTransaction("showEmulatorDisplayOverlay");
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void showStrictModeViolation(boolean on) {
        int pid = Binder.getCallingPid();
        if (on) {
            this.mH.sendMessage(this.mH.obtainMessage(25, 1, pid));
            this.mH.sendMessageDelayed(this.mH.obtainMessage(25, 0, pid), 1000);
            return;
        }
        this.mH.sendMessage(this.mH.obtainMessage(25, 0, pid));
    }

    private void showStrictModeViolation(int arg, int pid) {
        boolean on = arg != 0;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (!on || this.mRoot.canShowStrictModeViolation(pid)) {
                    SurfaceControl.openTransaction();
                    if (this.mStrictModeFlash == null) {
                        this.mStrictModeFlash = new StrictModeFlash(getDefaultDisplayContentLocked());
                    }
                    this.mStrictModeFlash.setVisibility(on);
                    SurfaceControl.closeTransaction();
                    resetPriorityAfterLockedSection();
                    return;
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setStrictModeVisualIndicatorPreference(String value) {
        SystemProperties.set("persist.sys.strictmode.visual", value);
    }

    public Bitmap screenshotWallpaper() {
        if (checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotWallpaper()")) {
            try {
                Bitmap screenshotWallpaperLocked;
                Trace.traceBegin(32, "screenshotWallpaper");
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    screenshotWallpaperLocked = this.mRoot.mWallpaperController.screenshotWallpaperLocked();
                }
                resetPriorityAfterLockedSection();
                Trace.traceEnd(32);
                return screenshotWallpaperLocked;
            } catch (Throwable th) {
                Trace.traceEnd(32);
            }
        } else {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
    }

    public boolean requestAssistScreenshot(IAssistDataReceiver receiver) {
        if (checkCallingPermission("android.permission.READ_FRAME_BUFFER", "requestAssistScreenshot()")) {
            Bitmap bm;
            synchronized (this.mWindowMap) {
                try {
                    Bitmap bm2;
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(0);
                    if (displayContent == null) {
                        bm2 = null;
                    } else {
                        bm2 = displayContent.screenshotDisplayLocked(Config.ARGB_8888);
                    }
                    bm = bm2;
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            FgThread.getHandler().post(new -$$Lambda$WindowManagerService$CbEzJbdxOpfZ-AMUAcOVQZxepOo(receiver, bm));
            return true;
        }
        throw new SecurityException("Requires READ_FRAME_BUFFER permission");
    }

    static /* synthetic */ void lambda$requestAssistScreenshot$2(IAssistDataReceiver receiver, Bitmap bm) {
        if (receiver != null) {
            try {
                receiver.onHandleAssistScreenshot(bm);
            } catch (RemoteException e) {
            }
        }
    }

    public TaskSnapshot getTaskSnapshot(int taskId, int userId, boolean reducedResolution) {
        return this.mTaskSnapshotController.getSnapshot(taskId, userId, true, reducedResolution);
    }

    public void removeObsoleteTaskFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.removeObsoleteTaskFiles(persistentTaskIds, runningUserIds);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void freezeRotation(int rotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        } else if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        } else {
            int defaultDisplayRotation = getDefaultDisplayRotation();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("freezeRotation: mRotation=");
            stringBuilder.append(defaultDisplayRotation);
            stringBuilder.append(",rotation=");
            stringBuilder.append(rotation);
            stringBuilder.append(",by pid=");
            stringBuilder.append(Binder.getCallingPid());
            Flog.i(308, stringBuilder.toString());
            long origId = Binder.clearCallingIdentity();
            try {
                this.mPolicy.setUserRotationMode(1, rotation == -1 ? defaultDisplayRotation : rotation);
                updateRotationUnchecked(false, false);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void thawRotation() {
        if (checkCallingPermission("android.permission.SET_ORIENTATION", "thawRotation()")) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thawRotation: mRotation=");
                stringBuilder.append(getDefaultDisplayRotation());
                stringBuilder.append(", by pid=");
                stringBuilder.append(Binder.getCallingPid());
                Slog.v(str, stringBuilder.toString());
            }
            long origId = Binder.clearCallingIdentity();
            try {
                boolean z = false;
                this.mPolicy.setUserRotationMode(z, 777);
                updateRotationUnchecked(z, z);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
    }

    public void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        updateRotationUnchecked(alwaysSendConfiguration, forceRelayout);
    }

    void pauseRotationLocked() {
        this.mDeferredRotationPauseCount++;
    }

    void resumeRotationLocked() {
        if (this.mDeferredRotationPauseCount > 0) {
            this.mDeferredRotationPauseCount--;
            if (this.mDeferredRotationPauseCount == 0) {
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                if (displayContent.updateRotationUnchecked()) {
                    this.mH.obtainMessage(18, Integer.valueOf(displayContent.getDisplayId())).sendToTarget();
                }
            }
        }
    }

    void updateRotationUnchecked(boolean alwaysSendConfiguration, boolean forceRelayout) {
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRotationUnchecked: alwaysSendConfiguration=");
            stringBuilder.append(alwaysSendConfiguration);
            stringBuilder.append(" forceRelayout=");
            stringBuilder.append(forceRelayout);
            Slog.v(str, stringBuilder.toString());
        }
        Trace.traceBegin(32, "updateRotation");
        long origId = Binder.clearCallingIdentity();
        try {
            boolean rotationChanged;
            int displayId;
            synchronized (this.mWindowMap) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                Trace.traceBegin(32, "updateRotation: display");
                rotationChanged = displayContent.updateRotationUnchecked();
                Trace.traceEnd(32);
                if (rotationChanged) {
                    LogPower.push(128);
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateRotationUnchecked: alwaysSendConfiguration=");
                stringBuilder2.append(alwaysSendConfiguration);
                stringBuilder2.append(" forceRelayout=");
                stringBuilder2.append(forceRelayout);
                stringBuilder2.append(" rotationChanged=");
                stringBuilder2.append(rotationChanged);
                Flog.i(308, stringBuilder2.toString());
                if (rotationChanged) {
                    if (!this.mIsPerfBoost) {
                        this.mIsPerfBoost = true;
                        UniPerf.getInstance().uniPerfEvent(4105, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new int[]{0});
                    }
                    if (this.mLastFinishedFreezeSource != null) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        stringBuilder3.append(this.mLastFinishedFreezeSource);
                        Jlog.d(58, stringBuilder3.toString());
                    } else {
                        Jlog.d(58, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                }
                if (!rotationChanged || forceRelayout) {
                    displayContent.setLayoutNeeded();
                    Trace.traceBegin(32, "updateRotation: performSurfacePlacement");
                    this.mWindowPlacerLocked.performSurfacePlacement();
                    Trace.traceEnd(32);
                }
                displayId = displayContent.getDisplayId();
            }
            resetPriorityAfterLockedSection();
            if (rotationChanged || alwaysSendConfiguration) {
                Trace.traceBegin(32, "updateRotation: sendNewConfiguration");
                sendNewConfiguration(displayId);
                Trace.traceEnd(32);
            }
            Binder.restoreCallingIdentity(origId);
            Trace.traceEnd(32);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            Trace.traceEnd(32);
        }
    }

    public int getDefaultDisplayRotation() {
        int rotation;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                rotation = getDefaultDisplayContentLocked().getRotation();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return rotation;
    }

    public boolean isRotationFrozen() {
        return this.mPolicy.getUserRotationMode() == 1;
    }

    public int watchRotation(IRotationWatcher watcher, int displayId) {
        int defaultDisplayRotation;
        final IBinder watcherBinder = watcher.asBinder();
        DeathRecipient dr = new DeathRecipient() {
            public void binderDied() {
                synchronized (WindowManagerService.this.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        int i = 0;
                        while (i < WindowManagerService.this.mRotationWatchers.size()) {
                            if (watcherBinder == ((RotationWatcher) WindowManagerService.this.mRotationWatchers.get(i)).mWatcher.asBinder()) {
                                IBinder binder = ((RotationWatcher) WindowManagerService.this.mRotationWatchers.remove(i)).mWatcher.asBinder();
                                if (binder != null) {
                                    binder.unlinkToDeath(this, 0);
                                }
                                i--;
                            }
                            i++;
                        }
                    } finally {
                        while (true) {
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        };
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                watcher.asBinder().linkToDeath(dr, 0);
                this.mRotationWatchers.add(new RotationWatcher(watcher, dr, displayId));
            } catch (RemoteException e) {
            }
            try {
                defaultDisplayRotation = getDefaultDisplayRotation();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return defaultDisplayRotation;
    }

    public void removeRotationWatcher(IRotationWatcher watcher) {
        IBinder watcherBinder = watcher.asBinder();
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                int i = 0;
                while (i < this.mRotationWatchers.size()) {
                    if (watcherBinder == ((RotationWatcher) this.mRotationWatchers.get(i)).mWatcher.asBinder()) {
                        RotationWatcher removed = (RotationWatcher) this.mRotationWatchers.remove(i);
                        IBinder binder = removed.mWatcher.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(removed.mDeathRecipient, 0);
                        }
                        i--;
                    }
                    i++;
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) {
        boolean isWallpaperVisible;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    this.mWallpaperVisibilityListeners.registerWallpaperVisibilityListener(listener, displayId);
                    isWallpaperVisible = displayContent.mWallpaperController.isWallpaperVisible();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Trying to register visibility event for invalid display: ");
                    stringBuilder.append(displayId);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return isWallpaperVisible;
    }

    public void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWallpaperVisibilityListeners.unregisterWallpaperVisibilityListener(listener, displayId);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x004b A:{LOOP_START, SKIP, LOOP:1: B:35:0x004b->B:39:0x004b, PHI: r7 , Splitter: B:2:0x0003, ExcHandler:  FINALLY} */
    /* JADX WARNING: Missing block: B:18:0x0032, code:
            return r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getPreferredOptionsPanelGravity() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                int rotation = displayContent.getRotation();
                int i = 81;
                if (displayContent.mInitialDisplayWidth < displayContent.mInitialDisplayHeight) {
                    switch (rotation) {
                        case 1:
                            resetPriorityAfterLockedSection();
                            return 85;
                        case 2:
                            resetPriorityAfterLockedSection();
                            return 81;
                        case 3:
                            resetPriorityAfterLockedSection();
                            return 8388691;
                    }
                    resetPriorityAfterLockedSection();
                } else {
                    switch (rotation) {
                        case 1:
                            resetPriorityAfterLockedSection();
                            return 81;
                        case 2:
                            resetPriorityAfterLockedSection();
                            return 8388691;
                        case 3:
                            resetPriorityAfterLockedSection();
                            return 81;
                        default:
                            resetPriorityAfterLockedSection();
                            return 85;
                    }
                    while (true) {
                        resetPriorityAfterLockedSection();
                    }
                }
            } finally {
            }
        }
    }

    public boolean startViewServer(int port) {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "startViewServer") || port < 1024) {
            return false;
        }
        if (this.mViewServer != null) {
            if (!this.mViewServer.isRunning()) {
                try {
                    return this.mViewServer.start();
                } catch (IOException e) {
                    Slog.w(TAG, "View server did not start");
                }
            }
            return false;
        }
        try {
            this.mViewServer = new ViewServer(this, port);
            return this.mViewServer.start();
        } catch (IOException e2) {
            Slog.w(TAG, "View server did not start");
            return false;
        }
    }

    private boolean isSystemSecure() {
        return "1".equals(SystemProperties.get(SYSTEM_SECURE, "1")) && "0".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
    }

    public boolean stopViewServer() {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "stopViewServer") || this.mViewServer == null) {
            return false;
        }
        return this.mViewServer.stop();
    }

    public boolean isViewServerRunning() {
        boolean z = false;
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "isViewServerRunning")) {
            return false;
        }
        if (this.mViewServer != null && this.mViewServer.isRunning()) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x0078 A:{REMOVE} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean viewServerListWindows(Socket client) {
        int i = 0;
        if (isSystemSecure()) {
            return false;
        }
        boolean result = true;
        ArrayList<WindowState> windows = new ArrayList();
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllWindows((Consumer) new -$$Lambda$WindowManagerService$CIuXGvNhVwi8txA2L_PmZnPJavk(windows), false);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()), 8192);
            int count = windows.size();
            while (i < count) {
                WindowState w = (WindowState) windows.get(i);
                out.write(Integer.toHexString(System.identityHashCode(w)));
                out.write(32);
                out.append(w.mAttrs.getTitle());
                out.write(10);
                i++;
            }
            out.write("DONE.\n");
            out.flush();
            try {
                out.close();
            } catch (IOException e) {
                result = false;
            }
        } catch (Exception e2) {
            result = false;
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                }
            }
        }
        return result;
    }

    boolean viewServerGetFocusedWindow(Socket client) {
        if (isSystemSecure()) {
            return false;
        }
        boolean result = true;
        WindowState focusedWindow = getFocusedWindow();
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()), 8192);
            if (focusedWindow != null) {
                out.write(Integer.toHexString(System.identityHashCode(focusedWindow)));
                out.write(32);
                out.append(focusedWindow.mAttrs.getTitle());
            }
            out.write(10);
            out.flush();
            try {
                out.close();
            } catch (IOException e) {
                result = false;
            }
        } catch (Exception e2) {
            result = false;
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                }
            }
        }
        return result;
    }

    boolean viewServerWindowCommand(Socket client, String command, String parameters) {
        if (isSystemSecure()) {
            return false;
        }
        boolean success = true;
        Parcel data = null;
        Parcel reply = null;
        BufferedWriter out = null;
        try {
            int index = parameters.indexOf(32);
            if (index == -1) {
                index = parameters.length();
            }
            int hashCode = (int) Long.parseLong(parameters.substring(0, index), 16);
            if (index < parameters.length()) {
                parameters = parameters.substring(index + 1);
            } else {
                parameters = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            WindowState window = findWindow(hashCode);
            if (window == null) {
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
                return false;
            }
            data = Parcel.obtain();
            data.writeInterfaceToken("android.view.IWindow");
            data.writeString(command);
            data.writeString(parameters);
            data.writeInt(1);
            ParcelFileDescriptor.fromSocket(client).writeToParcel(data, 0);
            reply = Parcel.obtain();
            window.mClient.asBinder().transact(1, data, reply, 0);
            reply.readException();
            if (!client.isOutputShutdown()) {
                out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                out.write("DONE\n");
                out.flush();
            }
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e2) {
                }
            }
            return success;
        } catch (Exception e3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not send command ");
            stringBuilder.append(command);
            stringBuilder.append(" with parameters ");
            stringBuilder.append(parameters);
            Slog.w(str, stringBuilder.toString(), e3);
            success = false;
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    public void addWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.add(listener);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void removeWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.remove(listener);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0025, code:
            resetPriorityAfterLockedSection();
            r0 = r1.length;
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:11:0x002a, code:
            if (r2 >= r0) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:12:0x002c, code:
            r1[r2].windowsChanged();
            r2 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:13:0x0034, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyWindowsChanged() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mWindowChangeListeners.isEmpty()) {
                } else {
                    WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0025, code:
            resetPriorityAfterLockedSection();
            r0 = r1.length;
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:11:0x002a, code:
            if (r2 >= r0) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:12:0x002c, code:
            r1[r2].focusChanged();
            r2 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:13:0x0034, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyFocusChanged() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mWindowChangeListeners.isEmpty()) {
                } else {
                    WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    private WindowState findWindow(int hashCode) {
        if (hashCode == -1) {
            return getFocusedWindow();
        }
        WindowState window;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                window = this.mRoot.getWindow(new -$$Lambda$WindowManagerService$r4TV5nJBkjzvUCeyV6sY2bt-bEA(hashCode));
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return window;
    }

    static /* synthetic */ boolean lambda$findWindow$4(int hashCode, WindowState w) {
        return System.identityHashCode(w) == hashCode;
    }

    void sendNewConfiguration(int displayId) {
        try {
            if (!this.mActivityManager.updateDisplayOverrideConfiguration(null, displayId)) {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    if (this.mWaitingForConfig) {
                        this.mWaitingForConfig = false;
                        this.mLastFinishedFreezeSource = "config-unchanged";
                        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                        if (dc != null) {
                            dc.setLayoutNeeded();
                        }
                        this.mWindowPlacerLocked.performSurfacePlacement();
                    }
                }
                resetPriorityAfterLockedSection();
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            while (true) {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public Configuration computeNewConfiguration(int displayId) {
        Configuration computeNewConfigurationLocked;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                computeNewConfigurationLocked = computeNewConfigurationLocked(displayId);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return computeNewConfigurationLocked;
    }

    Configuration computeNewConfigurationLocked(int displayId) {
        if (!this.mDisplayReady) {
            return null;
        }
        Configuration config = new Configuration();
        this.mRoot.getDisplayContent(displayId).computeScreenConfiguration(config);
        return config;
    }

    void notifyHardKeyboardStatusChange() {
        OnHardKeyboardStatusChangeListener listener;
        boolean available;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                listener = this.mHardKeyboardStatusChangeListener;
                available = this.mHardKeyboardAvailable;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        if (listener != null) {
            listener.onHardKeyboardStatusChange(available);
        }
    }

    public void setEventDispatching(boolean enabled) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setEventDispatching()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mEventDispatchingEnabled = enabled;
                    if (this.mDisplayEnabled) {
                        this.mInputMonitor.setEventDispatchingLw(enabled);
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    protected WindowState getFocusedWindow() {
        WindowState focusedWindowLocked;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                focusedWindowLocked = getFocusedWindowLocked();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return focusedWindowLocked;
    }

    public String getFocusedAppComponentName() {
        if (this.mFocusedApp != null) {
            return this.mFocusedApp.appComponentName;
        }
        return null;
    }

    private WindowState getFocusedWindowLocked() {
        return this.mCurrentFocus;
    }

    TaskStack getImeFocusStackLocked() {
        return (this.mFocusedApp == null || this.mFocusedApp.getTask() == null) ? null : this.mFocusedApp.getTask().mStack;
    }

    public boolean detectSafeMode() {
        if (!this.mInputMonitor.waitForInputDevicesReady(1000)) {
            Slog.w(TAG, "Devices still not ready after waiting 1000 milliseconds before attempting to detect safe mode.");
        }
        if (Global.getInt(this.mContext.getContentResolver(), "safe_boot_disallowed", 0) != 0) {
            return false;
        }
        int menuState = this.mInputManager.getKeyCodeState(-1, -256, 82);
        int sState = this.mInputManager.getKeyCodeState(-1, -256, 47);
        int dpadState = this.mInputManager.getKeyCodeState(-1, UsbTerminalTypes.TERMINAL_IN_MIC, 23);
        int trackballState = this.mInputManager.getScanCodeState(-1, 65540, 272);
        boolean z = menuState > 0 || sState > 0 || dpadState > 0 || trackballState > 0 || this.mInputManager.getKeyCodeState(-1, -256, 25) > 0;
        this.mSafeMode = z;
        try {
            if (!(SystemProperties.getInt(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, 0) == 0 && SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) == 0)) {
                this.mSafeMode = true;
                SystemProperties.set(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
        } catch (IllegalArgumentException e) {
        }
        if ("factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            this.mSafeMode = false;
        }
        if (this.mSafeMode) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SAFE MODE ENABLED (menu=");
            stringBuilder.append(menuState);
            stringBuilder.append(" s=");
            stringBuilder.append(sState);
            stringBuilder.append(" dpad=");
            stringBuilder.append(dpadState);
            stringBuilder.append(" trackball=");
            stringBuilder.append(trackballState);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
            if (SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) == 0) {
                SystemProperties.set(ShutdownThread.RO_SAFEMODE_PROPERTY, "1");
            }
        } else {
            Log.i(TAG, "SAFE MODE not enabled");
        }
        this.mPolicy.setSafeMode(this.mSafeMode);
        return this.mSafeMode;
    }

    public void displayReady() {
        int displayCount = this.mRoot.mChildren.size();
        for (int i = 0; i < displayCount; i++) {
            displayReady(((DisplayContent) this.mRoot.mChildren.get(i)).getDisplayId());
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                if (this.mMaxUiWidth > 0) {
                    displayContent.setMaxUiWidth(this.mMaxUiWidth);
                }
                readForcedDisplayPropertiesLocked(displayContent);
                this.mDisplayReady = true;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        try {
            this.mActivityManager.updateConfiguration(null);
        } catch (RemoteException e) {
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mIsTouchDevice = this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
                getDefaultDisplayContentLocked().configureDisplayPolicy();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        try {
            this.mActivityManager.updateConfiguration(null);
        } catch (RemoteException e2) {
        }
        updateCircularDisplayMaskIfNeeded();
    }

    private void displayReady(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    this.mAnimator.addDisplayLocked(displayId);
                    displayContent.initializeDisplayBaseInfo();
                    reconfigureDisplayLocked(displayContent);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void systemReady() {
        this.mPolicy.systemReady();
        this.mTaskSnapshotController.systemReady();
        this.mHasWideColorGamutSupport = queryWideColorGamutSupport();
    }

    private static boolean queryWideColorGamutSupport() {
        try {
            OptionalBool hasWideColor = ISurfaceFlingerConfigs.getService().hasWideColorDisplay();
            if (hasWideColor != null) {
                return hasWideColor.value;
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    void destroyPreservedSurfaceLocked() {
        for (int i = this.mDestroyPreservedSurface.size() - 1; i >= 0; i--) {
            ((WindowState) this.mDestroyPreservedSurface.get(i)).mWinAnimator.destroyPreservedSurfaceLocked();
        }
        this.mDestroyPreservedSurface.clear();
    }

    public IWindowSession openSession(IWindowSessionCallback callback, IInputMethodClient client, IInputContext inputContext) {
        if (client == null) {
            throw new IllegalArgumentException("null client");
        } else if (inputContext != null) {
            return new Session(this, callback, client, inputContext);
        } else {
            throw new IllegalArgumentException("null inputContext");
        }
    }

    public boolean inputMethodClientHasFocus(IInputMethodClient client) {
        boolean z;
        synchronized (this.mWindowMap) {
            boostPriorityForLockedSection();
            z = true;
            if (HwPCUtils.isPcCastModeInServer()) {
                DisplayContent displayContent = this.mRoot.getDisplayContent(getFocusedDisplayId());
                if (displayContent != null && displayContent.inputMethodClientHasFocus(client)) {
                }
            } else if (getDefaultDisplayContentLocked().inputMethodClientHasFocus(client)) {
                resetPriorityAfterLockedSection();
                return true;
            }
            try {
                if (this.mCurrentFocus == null || this.mCurrentFocus.mSession.mClient == null || this.mCurrentFocus.mSession.mClient.asBinder() != client.asBinder()) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                resetPriorityAfterLockedSection();
                return true;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    public void getInitialDisplaySize(int displayId, Point size) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    size.x = displayContent.mInitialDisplayWidth;
                    size.y = displayContent.mInitialDisplayHeight;
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void getBaseDisplaySize(int displayId, Point size) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    size.x = displayContent.mBaseDisplayWidth;
                    size.y = displayContent.mBaseDisplayHeight;
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setForcedDisplaySize(int displayId, int width, int height) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        } else if (displayId == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null) {
                        width = Math.min(Math.max(width, 200), displayContent.mInitialDisplayWidth * 2);
                        height = Math.min(Math.max(height, 200), displayContent.mInitialDisplayHeight * 2);
                        Slog.d(TAG, "setForcedDisplaySize and updateResourceConfiguration for HW_ROG_SUPPORT");
                        updateResourceConfiguration(displayId, displayContent.mBaseDisplayDensity, width, height);
                        setForcedDisplaySizeLocked(displayContent, width, height);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(width);
                        stringBuilder.append(",");
                        stringBuilder.append(height);
                        Global.putString(this.mContext.getContentResolver(), "display_size_forced", stringBuilder.toString());
                    }
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new IllegalArgumentException("Can only set the default display");
        }
    }

    public void setForcedDisplayScalingMode(int displayId, int mode) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        } else if (displayId == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null) {
                        if (mode < 0 || mode > 1) {
                            mode = 0;
                        }
                        setForcedDisplayScalingModeLocked(displayContent, mode);
                        Global.putInt(this.mContext.getContentResolver(), "display_scaling_force", mode);
                    }
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new IllegalArgumentException("Can only set the default display");
        }
    }

    private void setForcedDisplayScalingModeLocked(DisplayContent displayContent, int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Using display scaling mode: ");
        stringBuilder.append(mode == 0 ? Shell.NIGHT_MODE_STR_AUTO : "off");
        Slog.i(str, stringBuilder.toString());
        displayContent.mDisplayScalingDisabled = mode != 0;
        reconfigureDisplayLocked(displayContent);
    }

    private void readForcedDisplayPropertiesLocked(DisplayContent displayContent) {
        int width;
        String sizeStr = Global.getString(this.mContext.getContentResolver(), "display_size_forced");
        if (sizeStr == null || sizeStr.length() == 0) {
            sizeStr = SystemProperties.get(SIZE_OVERRIDE, null);
        }
        if (sizeStr != null && sizeStr.length() > 0) {
            int pos = sizeStr.indexOf(44);
            if (pos > 0 && sizeStr.lastIndexOf(44) == pos) {
                try {
                    width = Integer.parseInt(sizeStr.substring(0, pos));
                    int height = Integer.parseInt(sizeStr.substring(pos + 1));
                    if (!(displayContent.mBaseDisplayWidth == width && displayContent.mBaseDisplayHeight == height)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("FORCED DISPLAY SIZE: ");
                        stringBuilder.append(width);
                        stringBuilder.append("x");
                        stringBuilder.append(height);
                        Slog.i(str, stringBuilder.toString());
                        displayContent.updateBaseDisplayMetrics(width, height, displayContent.mBaseDisplayDensity);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        width = getForcedDisplayDensityForUserLocked(this.mCurrentUserId);
        if (width != 0) {
            displayContent.mBaseDisplayDensity = width;
        }
        if (Global.getInt(this.mContext.getContentResolver(), "display_scaling_force", 0) != 0) {
            Slog.i(TAG, "FORCED DISPLAY SCALING DISABLED");
            displayContent.mDisplayScalingDisabled = true;
        }
    }

    private void setForcedDisplaySizeLocked(DisplayContent displayContent, int width, int height) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Using new display size: ");
        stringBuilder.append(width);
        stringBuilder.append("x");
        stringBuilder.append(height);
        Slog.i(str, stringBuilder.toString());
        displayContent.updateBaseDisplayMetrics(width, height, displayContent.mBaseDisplayDensity);
        reconfigureDisplayLocked(displayContent);
    }

    public void clearForcedDisplaySize(int displayId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        } else if (displayId == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null) {
                        setForcedDisplaySizeLocked(displayContent, displayContent.mInitialDisplayWidth, displayContent.mInitialDisplayHeight);
                        Global.putString(this.mContext.getContentResolver(), "display_size_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new IllegalArgumentException("Can only set the default display");
        }
    }

    public int getInitialDisplayDensity(int displayId) {
        int hasAccess;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    hasAccess = displayContent.hasAccess(Binder.getCallingUid());
                    if (hasAccess != 0) {
                        hasAccess = displayContent.mInitialDisplayDensity;
                    }
                }
                resetPriorityAfterLockedSection();
                return -1;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return hasAccess;
    }

    public int getBaseDisplayDensity(int displayId) {
        int hasAccess;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    hasAccess = displayContent.hasAccess(Binder.getCallingUid());
                    if (hasAccess != 0) {
                        hasAccess = displayContent.mBaseDisplayDensity;
                    }
                }
                resetPriorityAfterLockedSection();
                return -1;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return hasAccess;
    }

    public void setForcedDisplayDensityForUser(int displayId, int density, int userId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        } else if (displayId == 0) {
            int targetUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "setForcedDisplayDensityForUser", null);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null && this.mCurrentUserId == targetUserId) {
                        Slog.d(TAG, "setForcedDisplayDensityForUser and updateResourceConfiguration for HW_ROG_SUPPORT");
                        updateResourceConfiguration(displayId, density, displayContent.mBaseDisplayWidth, displayContent.mBaseDisplayHeight);
                        setForcedDisplayDensityLocked(displayContent, density);
                    }
                    Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", Integer.toString(density), targetUserId);
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new IllegalArgumentException("Can only set the default display");
        }
    }

    public void clearForcedDisplayDensityForUser(int displayId, int userId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        } else if (displayId == 0) {
            int callingUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearForcedDisplayDensityForUser", null);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null && this.mCurrentUserId == callingUserId) {
                        int curWidth = SystemProperties.getInt("persist.sys.rog.width", 0);
                        if (curWidth > 0) {
                            setForcedDisplayDensityLocked(displayContent, (SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)) * curWidth) / displayContent.mInitialDisplayWidth);
                        } else {
                            setForcedDisplayDensityLocked(displayContent, displayContent.mInitialDisplayDensity);
                        }
                    }
                    Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, callingUserId);
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new IllegalArgumentException("Can only set the default display");
        }
    }

    private int getForcedDisplayDensityForUserLocked(int userId) {
        String densityStr = Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", userId);
        if (densityStr == null || densityStr.length() == 0) {
            densityStr = SystemProperties.get(DENSITY_OVERRIDE, null);
        }
        if (densityStr != null && densityStr.length() > 0) {
            try {
                return Integer.parseInt(densityStr);
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    private void setForcedDisplayDensityLocked(DisplayContent displayContent, int density) {
        displayContent.mBaseDisplayDensity = density;
        reconfigureDisplayLocked(displayContent);
    }

    protected void reconfigureDisplayLocked(DisplayContent displayContent) {
        if (displayContent.isReady()) {
            displayContent.configureDisplayPolicy();
            displayContent.setLayoutNeeded();
            int displayId = displayContent.getDisplayId();
            boolean configChanged = updateOrientationFromAppTokensLocked(displayId);
            Configuration currentDisplayConfig = displayContent.getConfiguration();
            this.mTempConfiguration.setTo(currentDisplayConfig);
            displayContent.computeScreenConfiguration(this.mTempConfiguration);
            if (configChanged | (currentDisplayConfig.diff(this.mTempConfiguration) != 0 ? 1 : 0)) {
                this.mWaitingForConfig = true;
                startFreezingDisplayLocked(0, 0, displayContent);
                this.mH.obtainMessage(18, Integer.valueOf(displayId)).sendToTarget();
            }
            this.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    public void getDisplaysInFocusOrder(SparseIntArray displaysInFocusOrder) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.getDisplaysInFocusOrder(displaysInFocusOrder);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setOverscan(int displayId, int left, int top, int right, int bottom) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null) {
                        setOverscanLocked(displayContent, left, top, right, bottom);
                    }
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    private void setOverscanLocked(DisplayContent displayContent, int left, int top, int right, int bottom) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        displayInfo.overscanLeft = left;
        displayInfo.overscanTop = top;
        displayInfo.overscanRight = right;
        displayInfo.overscanBottom = bottom;
        this.mDisplaySettings.setOverscanLocked(displayInfo.uniqueId, displayInfo.name, left, top, right, bottom);
        this.mDisplaySettings.writeSettingsLocked();
        reconfigureDisplayLocked(displayContent);
    }

    public void startWindowTrace() {
        try {
            this.mWindowTracing.startTrace(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopWindowTrace() {
        this.mWindowTracing.stopTrace(null);
    }

    public boolean isWindowTraceEnabled() {
        return this.mWindowTracing.isEnabled();
    }

    final WindowState windowForClientLocked(Session session, IWindow client, boolean throwOnError) {
        return windowForClientLocked(session, client.asBinder(), throwOnError);
    }

    final WindowState windowForClientLocked(Session session, IBinder client, boolean throwOnError) {
        WindowState win = (WindowState) this.mWindowMap.get(client);
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        if (win == null) {
            if (throwOnError) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Requested window ");
                stringBuilder.append(client);
                stringBuilder.append(" does not exist");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed looking up window callers=");
            stringBuilder2.append(Debug.getCallers(3));
            Slog.w(str, stringBuilder2.toString());
            return null;
        } else if (session == null || win.mSession == session) {
            return win;
        } else {
            if (throwOnError) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Requested window ");
                stringBuilder.append(client);
                stringBuilder.append(" is in session ");
                stringBuilder.append(win.mSession);
                stringBuilder.append(", not ");
                stringBuilder.append(session);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed looking up window callers=");
            stringBuilder2.append(Debug.getCallers(3));
            Slog.w(str, stringBuilder2.toString());
            return null;
        }
    }

    void makeWindowFreezingScreenIfNeededLocked(WindowState w) {
        if (!w.mToken.okToDisplay() && this.mWindowsFreezingScreen != 2) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Changing surface while display frozen: ");
                stringBuilder.append(w);
                Slog.v(str, stringBuilder.toString());
            }
            w.setOrientationChanging(true);
            w.mLastFreezeDuration = 0;
            this.mRoot.mOrientationChangeComplete = false;
            if (this.mWindowsFreezingScreen == 0) {
                this.mWindowsFreezingScreen = 1;
                this.mH.removeMessages(11);
                this.mH.sendEmptyMessageDelayed(11, 2000);
            }
        }
    }

    int handleAnimatingStoppedAndTransitionLocked() {
        this.mAppTransition.setIdle();
        for (int i = this.mNoAnimationNotifyOnTransitionFinished.size() - 1; i >= 0; i--) {
            this.mAppTransition.notifyAppTransitionFinishedLocked((IBinder) this.mNoAnimationNotifyOnTransitionFinished.get(i));
        }
        this.mNoAnimationNotifyOnTransitionFinished.clear();
        DisplayContent dc = getDefaultDisplayContentLocked();
        dc.mWallpaperController.hideDeferredWallpapersIfNeeded();
        dc.onAppTransitionDone();
        int changes = 0 | 1;
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v(TAG, "Wallpaper layer changed: assigning layers + relayout");
        }
        if (getFocusedDisplayId() == 0) {
            dc.computeImeTarget(true);
        }
        this.mRoot.mWallpaperMayChange = true;
        this.mFocusMayChange = true;
        return changes;
    }

    void checkDrawnWindowsLocked() {
        if (!this.mWaitingForDrawn.isEmpty() && this.mWaitingForDrawnCallback != null) {
            boolean printLog = isPrintAllWindowsDrawnLogs();
            for (int j = this.mWaitingForDrawn.size() - 1; j >= 0; j--) {
                WindowState win = (WindowState) this.mWaitingForDrawn.get(j);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power Waiting for drawn ");
                stringBuilder.append(win);
                stringBuilder.append(": removed=");
                stringBuilder.append(win.mRemoved);
                stringBuilder.append(" visible=");
                stringBuilder.append(win.isVisibleLw());
                stringBuilder.append(" mHasSurface=");
                stringBuilder.append(win.mHasSurface);
                stringBuilder.append(" drawState=");
                stringBuilder.append(win.mWinAnimator.mDrawState);
                Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
                String str;
                if (win.mRemoved || !win.mHasSurface || !win.mPolicyVisibility) {
                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Aborted waiting for drawn: ");
                        stringBuilder.append(win);
                        Slog.w(str, stringBuilder.toString());
                    }
                    this.mWaitingForDrawn.remove(win);
                } else if (win.hasDrawnLw()) {
                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Window drawn win=");
                        stringBuilder.append(win);
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mWaitingForDrawn.remove(win);
                }
            }
            if (this.mWaitingForDrawn.isEmpty()) {
                Flog.i(307, "All windows drawn!");
                this.mH.removeMessages(24);
                this.mFingerUnlockHandler.sendEmptyMessage(33);
            }
        }
    }

    void setHoldScreenLocked(Session newHoldScreen) {
        boolean hold = newHoldScreen != null;
        if (hold && this.mHoldingScreenOn != newHoldScreen) {
            this.mHoldingScreenWakeLock.setWorkSource(new WorkSource(newHoldScreen.mUid));
        }
        this.mHoldingScreenOn = newHoldScreen;
        if (hold == this.mHoldingScreenWakeLock.isHeld()) {
            return;
        }
        if (hold) {
            this.mLastWakeLockHoldingWindow = this.mRoot.mHoldScreenWindow;
            int displayId = this.mLastWakeLockHoldingWindow.getDisplayId();
            this.mLastWakeLockObscuringWindow = null;
            if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
                this.mPCHoldingScreenWakeLock.acquire();
            } else {
                this.mHoldingScreenWakeLock.acquire();
            }
            this.mPolicy.keepScreenOnStartedLw();
            return;
        }
        this.mLastWakeLockHoldingWindow = null;
        this.mLastWakeLockObscuringWindow = this.mRoot.mObscuringWindow;
        this.mPolicy.keepScreenOnStoppedLw();
        this.mHoldingScreenWakeLock.release();
        this.mPCHoldingScreenWakeLock.release();
    }

    void requestTraversal() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.requestTraversal();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void scheduleAnimationLocked() {
        if (this.mAnimator != null) {
            this.mAnimator.scheduleAnimation();
        }
    }

    boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        int i = mode;
        boolean z = updateInputWindows;
        WindowState newFocus = this.mRoot.computeFocusedWindow();
        if (this.mCurrentFocus == newFocus) {
            return false;
        }
        String str;
        Trace.traceBegin(32, "wmUpdateFocus");
        this.mH.removeMessages(2);
        this.mH.sendEmptyMessage(2);
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        if (isLandScapeMultiWindowMode() && (this.mPolicy instanceof PhoneWindowManager)) {
            ((PhoneWindowManager) this.mPolicy).setFocusChangeIMEFrozenTag(true);
        }
        if (HwPCUtils.isPcCastModeInServer()) {
            DisplayContent pcDC;
            if (HwPCUtils.enabledInPad()) {
                pcDC = this.mRoot.getDisplayContent(getFocusedDisplayId());
                if (pcDC != null) {
                    displayContent = pcDC;
                }
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateFocusedWindowLocked: ");
                stringBuilder.append(newFocus);
                stringBuilder.append(", focusedDisplayId:");
                stringBuilder.append(getFocusedDisplayId());
                Slog.d(str2, stringBuilder.toString());
                if (newFocus != null) {
                    displayContent = newFocus.getDisplayContent();
                    if (getFocusedDisplayId() != displayContent.getDisplayId()) {
                        Slog.w(TAG, "newFocus is not in focused PC displayContent");
                    }
                } else {
                    pcDC = this.mRoot.getDisplayContent(getFocusedDisplayId());
                    if (pcDC != null) {
                        displayContent = pcDC;
                    }
                }
            }
        }
        boolean imWindowChanged = false;
        if (this.mInputMethodWindow != null) {
            imWindowChanged = this.mInputMethodTarget != displayContent.computeImeTarget(true) ? true : null;
            if (!(i == 1 || i == 3)) {
                int prevImeAnimLayer = this.mInputMethodWindow.mWinAnimator.mAnimLayer;
                displayContent.assignWindowLayers(false);
                imWindowChanged |= prevImeAnimLayer != this.mInputMethodWindow.mWinAnimator.mAnimLayer ? 1 : 0;
            }
        }
        if (imWindowChanged) {
            this.mWindowsChanged = true;
            displayContent.setLayoutNeeded();
            newFocus = this.mRoot.computeFocusedWindow();
        }
        WindowState newFocus2 = newFocus;
        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Changing focus from ");
            stringBuilder2.append(this.mCurrentFocus);
            stringBuilder2.append(" to ");
            stringBuilder2.append(newFocus2);
            stringBuilder2.append(" Callers=");
            stringBuilder2.append(Debug.getCallers(4));
            Slog.v(str, stringBuilder2.toString());
        }
        WindowState oldFocus = this.mCurrentFocus;
        this.mCurrentFocus = newFocus2;
        if (this.mNotifyFocusedWindow) {
            IWindow currentWindow = null;
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    for (Entry<IBinder, WindowState> entry : this.mWindowMap.entrySet()) {
                        if (this.mCurrentFocus == entry.getValue()) {
                            currentWindow = IWindow.Stub.asInterface((IBinder) entry.getKey());
                            break;
                        }
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("currentWindow = ");
            stringBuilder3.append(currentWindow);
            Slog.i(str, stringBuilder3.toString());
            if (currentWindow != null) {
                try {
                    currentWindow.notifyFocusChanged();
                } catch (RemoteException e) {
                    RemoteException remoteException = e;
                    Slog.w(TAG, "currentWindow.notifyFocusChanged() get RemoteException!");
                }
            }
        }
        this.mInputManager.setCurFocusWindow(this.mCurrentFocus);
        this.mLosingFocus.remove(newFocus2);
        if (this.mCurrentFocus != null) {
            this.mWinAddedSinceNullFocus.clear();
            this.mWinRemovedSinceNullFocus.clear();
        }
        if (this.mCurrentFocus != null) {
            this.mWinAddedSinceNullFocus.clear();
            this.mWinRemovedSinceNullFocus.clear();
        }
        int focusChanged = this.mPolicy.focusChangedLw(oldFocus, newFocus2);
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            if (newFocus2 != null) {
                try {
                    if (newFocus2.getAttrs() != null) {
                        hwAft.notifyFocusChange(newFocus2.mSession.mPid, newFocus2.getAttrs().getTitle().toString());
                    }
                } catch (RemoteException e2) {
                    String str3 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("binder call hwAft throw ");
                    stringBuilder4.append(e2);
                    Slog.e(str3, stringBuilder4.toString());
                }
            }
            hwAft.notifyFocusChange(0, null);
        }
        if (imWindowChanged && oldFocus != this.mInputMethodWindow) {
            if (i == 2) {
                displayContent.performLayout(true, z);
                focusChanged &= -2;
            } else if (i == 3) {
                displayContent.assignWindowLayers(false);
            }
        }
        if ((focusChanged & 1) != 0) {
            displayContent.setLayoutNeeded();
            if (i == 2) {
                displayContent.performLayout(true, z);
            }
        }
        if (i != 1) {
            this.mInputMonitor.setInputFocusLw(this.mCurrentFocus, z);
        }
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("oldFocusWindow: ");
        stringBuilder5.append(oldFocus);
        stringBuilder5.append(", currentFocusWindow: ");
        stringBuilder5.append(this.mCurrentFocus);
        stringBuilder5.append(", currentFocusApp: ");
        stringBuilder5.append(this.mFocusedApp);
        Flog.i(304, stringBuilder5.toString());
        newFocus = new ArrayMap();
        if (this.mCurrentFocus != null) {
            newFocus.put("focusedWindowName", this.mCurrentFocus.toString());
            newFocus.put("layoutParams", this.mCurrentFocus.getAttrs());
            if (this.mCurrentFocus.mSession != null) {
                newFocus.put(IZRHungService.PARAM_PID, Integer.valueOf(this.mCurrentFocus.mSession.mPid));
            }
        } else {
            newFocus.put("focusedWindowName", "null");
            newFocus.put("layoutParams", "null");
            newFocus.put(IZRHungService.PARAM_PID, Integer.valueOf(0));
        }
        if (this.mFocusedApp != null) {
            newFocus.put("focusedPackageName", this.mFocusedApp.appPackageName);
            newFocus.put("focusedActivityName", this.mFocusedApp.appComponentName);
        } else {
            newFocus.put("focusedPackageName", "null");
            newFocus.put("focusedActivityName", "null");
        }
        ZrHungData arg = new ZrHungData();
        arg.putAll(newFocus);
        IZrHung focusWindowZrHung = HwFrameworkFactory.getZrHung("appeye_nofocuswindow");
        if (focusWindowZrHung != null) {
            if (this.mCurrentFocus == null) {
                focusWindowZrHung.check(arg);
            } else {
                focusWindowZrHung.cancelCheck(arg);
            }
        }
        IZrHung transWindowZrHung = HwFrameworkFactory.getZrHung("appeye_transparentwindow");
        if (transWindowZrHung != null) {
            transWindowZrHung.cancelCheck(arg);
            if (this.mCurrentFocus != null) {
                transWindowZrHung.check(arg);
            }
        }
        displayContent.adjustForImeIfNeeded();
        displayContent.scheduleToastWindowsTimeoutIfNeededLocked(oldFocus, newFocus2);
        Trace.traceEnd(32);
        return true;
    }

    void startFreezingDisplayLocked(int exitAnim, int enterAnim) {
        startFreezingDisplayLocked(exitAnim, enterAnim, getDefaultDisplayContentLocked());
    }

    /* JADX WARNING: Missing block: B:33:0x00f4, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void startFreezingDisplayLocked(int exitAnim, int enterAnim, DisplayContent displayContent) {
        if (!this.mDisplayFrozen && !this.mRotatingSeamlessly && displayContent.isReady() && this.mPolicy.isScreenOn() && displayContent.okToAnimate()) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startFreezingDisplayLocked: exitAnim=");
                stringBuilder.append(exitAnim);
                stringBuilder.append(" enterAnim=");
                stringBuilder.append(enterAnim);
                stringBuilder.append(" called by ");
                stringBuilder.append(Debug.getCallers(8));
                Slog.d(str, stringBuilder.toString());
            }
            this.mScreenFrozenLock.acquire();
            this.mDisplayFrozen = true;
            this.mDisplayFreezeTime = SystemClock.elapsedRealtime();
            this.mLastFinishedFreezeSource = null;
            this.mFrozenDisplayId = displayContent.getDisplayId();
            this.mInputMonitor.freezeInputDispatchingLw();
            this.mPolicy.setLastInputMethodWindowLw(null, null);
            if (this.mAppTransition.isTransitionSet()) {
                this.mAppTransition.freeze();
            }
            this.mLatencyTracker.onActionStart(6);
            if (displayContent.isDefaultDisplay) {
                this.mExitAnimId = exitAnim;
                this.mEnterAnimId = enterAnim;
                ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(this.mFrozenDisplayId);
                if (screenRotationAnimation != null) {
                    screenRotationAnimation.kill();
                }
                boolean isSecure = displayContent.hasSecureWindowOnScreen();
                displayContent.updateDisplayInfo();
                if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayContent.getDisplayId())) {
                    this.mH.removeMessages(103);
                    this.mH.sendEmptyMessageDelayed(103, 3000);
                } else {
                    IHwScreenRotationAnimation hwSRA = HwServiceFactory.getHwScreenRotationAnimation();
                    if (hwSRA != null) {
                        screenRotationAnimation = hwSRA.create(this.mContext, displayContent, this.mPolicy.isDefaultOrientationForced(), isSecure, this);
                    } else {
                        screenRotationAnimation = new ScreenRotationAnimation(this.mContext, displayContent, this.mPolicy.isDefaultOrientationForced(), isSecure, this);
                    }
                    this.mAnimator.setScreenRotationAnimationLocked(this.mFrozenDisplayId, screenRotationAnimation);
                }
            }
        }
    }

    void stopFreezingDisplayLocked() {
        if (this.mDisplayFrozen) {
            boolean z = true;
            if (!(this.mWaitingForConfig || this.mAppsFreezingScreen > 0 || this.mWindowsFreezingScreen == 1 || this.mClientFreezingScreen)) {
                z = false;
            }
            boolean bReturn = z;
            int size = this.mOpeningApps.size();
            boolean configChanged;
            String str;
            if (bReturn || size > 0) {
                configChanged = WindowManagerDebugConfig.DEBUG_ORIENTATION;
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopFreezingDisplayLocked: Returning mWaitingForConfig=");
                stringBuilder.append(this.mWaitingForConfig);
                stringBuilder.append(", mAppsFreezingScreen=");
                stringBuilder.append(this.mAppsFreezingScreen);
                stringBuilder.append(", mWindowsFreezingScreen=");
                stringBuilder.append(this.mWindowsFreezingScreen);
                stringBuilder.append(", mClientFreezingScreen=");
                stringBuilder.append(this.mClientFreezingScreen);
                stringBuilder.append(", mOpeningApps.size()=");
                stringBuilder.append(this.mOpeningApps.size());
                Slog.d(str, stringBuilder.toString());
                if (!bReturn && size > 0) {
                    printFreezingDisplayLogs();
                }
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d(TAG, "stopFreezingDisplayLocked: Unfreezing now");
            }
            if (HwPCUtils.isPcCastModeInServer()) {
                this.mH.removeMessages(103);
            }
            DisplayContent displayContent = this.mRoot.getDisplayContent(this.mFrozenDisplayId);
            StringBuilder stringBuilder2;
            if (displayContent == null) {
                this.mH.removeMessages(17);
                this.mH.removeMessages(30);
                this.mInputMonitor.thawInputDispatchingLw();
                this.mH.removeMessages(15);
                this.mH.sendEmptyMessageDelayed(15, 2000);
                this.mScreenFrozenLock.release();
                this.mFrozenDisplayId = -1;
                this.mDisplayFrozen = false;
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopFreezingDisplayLocked: Attempted to updateRotation for non-exiting displayId = ");
                stringBuilder2.append(this.mFrozenDisplayId);
                Slog.w(str, stringBuilder2.toString());
                return;
            }
            int displayId = this.mFrozenDisplayId;
            this.mFrozenDisplayId = -1;
            this.mDisplayFrozen = false;
            this.mInputMonitor.thawInputDispatchingLw();
            this.mLastDisplayFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
            stringBuilder2 = new StringBuilder(128);
            stringBuilder2.append("Screen frozen for ");
            TimeUtils.formatDuration((long) this.mLastDisplayFreezeDuration, stringBuilder2);
            if (this.mLastFinishedFreezeSource != null) {
                stringBuilder2.append(" due to ");
                stringBuilder2.append(this.mLastFinishedFreezeSource);
            }
            Slog.i(TAG, stringBuilder2.toString());
            this.mH.removeMessages(17);
            this.mH.removeMessages(30);
            boolean updateRotation = false;
            ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
            if (screenRotationAnimation == null || !screenRotationAnimation.hasScreenshot()) {
                if (screenRotationAnimation != null) {
                    screenRotationAnimation.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                }
                updateRotation = true;
            } else {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.i(TAG, "**** Dismissing screen rotation animation");
                }
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                if (!this.mPolicy.validateRotationAnimationLw(this.mExitAnimId, this.mEnterAnimId, false)) {
                    this.mEnterAnimId = 0;
                    this.mExitAnimId = 0;
                }
                Transaction transaction = this.mTransaction;
                float transitionAnimationScaleLocked = getTransitionAnimationScaleLocked();
                int i = displayInfo.logicalWidth;
                int i2 = displayInfo.logicalHeight;
                if (screenRotationAnimation.dismiss(transaction, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, transitionAnimationScaleLocked, i, i2, this.mExitAnimId, this.mEnterAnimId)) {
                    this.mTransaction.apply();
                    scheduleAnimationLocked();
                } else {
                    screenRotationAnimation.kill();
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                    updateRotation = true;
                }
            }
            configChanged = updateOrientationFromAppTokensLocked(displayId);
            this.mH.removeMessages(15);
            this.mH.sendEmptyMessageDelayed(15, 2000);
            this.mScreenFrozenLock.release();
            if (updateRotation) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.d(TAG, "Performing post-rotate rotation");
                }
                configChanged |= displayContent.updateRotationUnchecked();
            }
            if (configChanged) {
                this.mH.obtainMessage(18, Integer.valueOf(displayId)).sendToTarget();
            }
            this.mLatencyTracker.onActionEnd(6);
        }
    }

    static int getPropertyInt(String[] tokens, int index, int defUnits, int defDps, DisplayMetrics dm) {
        if (index < tokens.length) {
            String str = tokens[index];
            if (str != null && str.length() > 0) {
                try {
                    return Integer.parseInt(str);
                } catch (Exception e) {
                }
            }
        }
        if (defUnits == 0) {
            return defDps;
        }
        return (int) TypedValue.applyDimension(defUnits, (float) defDps, dm);
    }

    void createWatermarkInTransaction() {
        if (this.mWatermark == null) {
            FileInputStream in = null;
            DataInputStream ind = null;
            try {
                ind = new DataInputStream(new FileInputStream(new File("/system/etc/setup.conf")));
                String line = ind.readLine();
                if (line != null) {
                    String[] toks = line.split("%");
                    if (toks != null && toks.length > 0) {
                        DisplayContent displayContent = getDefaultDisplayContentLocked();
                        this.mWatermark = new Watermark(displayContent, displayContent.mRealDisplayMetrics, toks);
                    }
                }
                try {
                    ind.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e2) {
                if (ind != null) {
                    ind.close();
                } else if (in != null) {
                    in.close();
                }
            } catch (IOException e3) {
                if (ind != null) {
                    ind.close();
                } else if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Throwable th) {
                if (ind != null) {
                    try {
                        ind.close();
                    } catch (IOException e5) {
                    }
                } else if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e6) {
                    }
                }
            }
        }
    }

    public void setRecentsVisibility(boolean visible) {
        this.mAmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.STATUS_BAR", "setRecentsVisibility()");
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setRecentsVisibilityLw(visible);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setPipVisibility(boolean visible) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mPolicy.setPipVisibilityLw(visible);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    public void setShelfHeight(boolean visible, int shelfHeight) {
        this.mAmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.STATUS_BAR", "setShelfHeight()");
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getPinnedStackController().setAdjustedForShelf(visible, shelfHeight);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void statusBarVisibilityChanged(int visibility) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    int diff = this.mLastStatusBarVisibility ^ visibility;
                    if (diff != 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("statusBarVisibilityChanged,vis=");
                        stringBuilder.append(Integer.toHexString(visibility));
                        stringBuilder.append(",diff=");
                        stringBuilder.append(Integer.toHexString(diff));
                        Flog.i(303, stringBuilder.toString());
                    }
                    this.mLastStatusBarVisibility = visibility;
                    visibility = this.mPolicy.adjustSystemUiVisibilityLw(visibility);
                    if ((diff & 201326592) == 201326592 || (diff & 134217728) == 134217728) {
                        DisplayContent displayContent = getDefaultDisplayContentLocked();
                        displayContent.pendingLayoutChanges |= 1;
                        this.mRoot.performSurfacePlacement(false);
                    }
                    updateStatusBarVisibilityLocked(visibility);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean enabled) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mPolicy.setNavBarVirtualKeyHapticFeedbackEnabledLw(enabled);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    private boolean updateStatusBarVisibilityLocked(int visibility) {
        if (this.mLastDispatchedSystemUiVisibility == visibility) {
            return false;
        }
        int globalDiff = ((this.mLastDispatchedSystemUiVisibility ^ visibility) & 7) & (~visibility);
        this.mLastDispatchedSystemUiVisibility = visibility;
        this.mInputManager.setSystemUiVisibility(visibility);
        getDefaultDisplayContentLocked().updateSystemUiVisibility(visibility, globalDiff);
        return true;
    }

    public void reevaluateStatusBarVisibility() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (updateStatusBarVisibilityLocked(this.mPolicy.adjustSystemUiVisibilityLw(this.mLastStatusBarVisibility))) {
                    this.mWindowPlacerLocked.requestTraversal();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public int getNavBarPosition() {
        int navBarPosition;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().performLayout(false, false);
                navBarPosition = this.mPolicy.getNavBarPosition();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return navBarPosition;
    }

    public InputConsumer createInputConsumer(Looper looper, String name, Factory inputEventReceiverFactory) {
        InputConsumer createInputConsumer;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                createInputConsumer = this.mInputMonitor.createInputConsumer(looper, name, inputEventReceiverFactory);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return createInputConsumer;
    }

    public void createInputConsumer(IBinder token, String name, InputChannel inputChannel) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mInputMonitor.createInputConsumer(token, name, inputChannel, Binder.getCallingPid(), Binder.getCallingUserHandle());
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean destroyInputConsumer(String name) {
        boolean destroyInputConsumer;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                destroyInputConsumer = this.mInputMonitor.destroyInputConsumer(name);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return destroyInputConsumer;
    }

    public Region getCurrentImeTouchRegion() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.RESTRICTED_VR_ACCESS") == 0) {
            Region r;
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    r = new Region();
                    if (this.mInputMethodWindow != null) {
                        this.mInputMethodWindow.getTouchableRegion(r);
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
            return r;
        }
        throw new SecurityException("getCurrentImeTouchRegion is restricted to VR services");
    }

    public boolean hasNavigationBar() {
        return this.mPolicy.hasNavigationBar();
    }

    public void lockNow(Bundle options) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WINDOWNMANAGER_LOCKNOW);
        this.mPolicy.lockNow(options);
    }

    public void showRecentApps() {
        this.mPolicy.showRecentApps();
    }

    public boolean isSafeModeEnabled() {
        return this.mSafeMode;
    }

    public boolean clearWindowContentFrameStats(IBinder token) {
        boolean z;
        if (checkCallingPermission("android.permission.FRAME_STATS", "clearWindowContentFrameStats()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) this.mWindowMap.get(token);
                    z = false;
                    if (windowState == null) {
                    } else {
                        WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
                        if (surfaceController == null) {
                            resetPriorityAfterLockedSection();
                            return false;
                        }
                        z = surfaceController.clearWindowContentFrameStats();
                        resetPriorityAfterLockedSection();
                        return z;
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        return z;
    }

    public WindowContentFrameStats getWindowContentFrameStats(IBinder token) {
        WindowContentFrameStats windowContentFrameStats;
        if (checkCallingPermission("android.permission.FRAME_STATS", "getWindowContentFrameStats()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) this.mWindowMap.get(token);
                    windowContentFrameStats = null;
                    if (windowState == null) {
                    } else {
                        WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
                        if (surfaceController == null) {
                            resetPriorityAfterLockedSection();
                            return null;
                        }
                        if (this.mTempWindowRenderStats == null) {
                            this.mTempWindowRenderStats = new WindowContentFrameStats();
                        }
                        WindowContentFrameStats stats = this.mTempWindowRenderStats;
                        if (surfaceController.getWindowContentFrameStats(stats)) {
                            resetPriorityAfterLockedSection();
                            return stats;
                        }
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        return windowContentFrameStats;
    }

    public void notifyAppRelaunching(IBinder token) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.startRelaunching();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppRelaunchingFinished(IBinder token) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.finishRelaunching();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppRelaunchesCleared(IBinder token) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.clearRelaunching();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppResumedFinished(IBinder token) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    this.mUnknownAppVisibilityController.notifyAppResumedFinished(appWindow);
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyTaskRemovedFromRecents(int taskId, int userId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.notifyTaskRemovedFromRecents(taskId, userId);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public int getDockedDividerInsetsLw() {
        return getDefaultDisplayContentLocked().getDockedDividerController().getContentInsets();
    }

    private void dumpPolicyLocked(PrintWriter pw, String[] args, boolean dumpAll) {
        pw.println("WINDOW MANAGER POLICY STATE (dumpsys window policy)");
        this.mPolicy.dump("    ", pw, args);
    }

    private void dumpAnimatorLocked(PrintWriter pw, String[] args, boolean dumpAll) {
        pw.println("WINDOW MANAGER ANIMATOR STATE (dumpsys window animator)");
        this.mAnimator.dumpLocked(pw, "    ", dumpAll);
    }

    private void dumpTokensLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER TOKENS (dumpsys window tokens)");
        this.mRoot.dumpTokens(pw, dumpAll);
        if (!this.mOpeningApps.isEmpty() || !this.mClosingApps.isEmpty()) {
            pw.println();
            if (this.mOpeningApps.size() > 0) {
                pw.print("  mOpeningApps=");
                pw.println(this.mOpeningApps);
            }
            if (this.mClosingApps.size() > 0) {
                pw.print("  mClosingApps=");
                pw.println(this.mClosingApps);
            }
        }
    }

    private void dumpSessionsLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER SESSIONS (dumpsys window sessions)");
        for (int i = 0; i < this.mSessions.size(); i++) {
            Session s = (Session) this.mSessions.valueAt(i);
            pw.print("  Session ");
            pw.print(s);
            pw.println(':');
            s.dump(pw, "    ");
        }
    }

    void writeToProtoLocked(ProtoOutputStream proto, boolean trim) {
        this.mPolicy.writeToProto(proto, 1146756268033L);
        this.mRoot.writeToProto(proto, 1146756268034L, trim);
        if (this.mCurrentFocus != null) {
            this.mCurrentFocus.writeIdentifierToProto(proto, 1146756268035L);
        }
        if (this.mFocusedApp != null) {
            this.mFocusedApp.writeNameToProto(proto, 1138166333444L);
        }
        if (this.mInputMethodWindow != null) {
            this.mInputMethodWindow.writeIdentifierToProto(proto, 1146756268037L);
        }
        proto.write(1133871366150L, this.mDisplayFrozen);
        DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
        proto.write(1120986464263L, defaultDisplayContent.getRotation());
        proto.write(1120986464264L, defaultDisplayContent.getLastOrientation());
        this.mAppTransition.writeToProto(proto, 1146756268041L);
    }

    void traceStateLocked(String where) {
        Trace.traceBegin(32, "traceStateLocked");
        try {
            this.mWindowTracing.traceStateLocked(where, this);
        } catch (Exception e) {
            Log.wtf(TAG, "Exception while tracing state", e);
        } catch (Throwable th) {
            Trace.traceEnd(32);
        }
        Trace.traceEnd(32);
    }

    private void dumpHandlerLocked(PrintWriter pw) {
        pw.println("  mHandler:");
        this.mH.dump(new PrintWriterPrinter(pw), "    ");
    }

    private void setWmsDebugFlag(PrintWriter pw, String cmd) {
        if (cmd.equals(DEBUG_FOCUS_CMD)) {
            WindowManagerDebugConfig.DEBUG_FOCUS = true;
            pw.println("Set DEBUG_FOCUS flag on");
        } else if (cmd.equals(DEBUG_VISIBILITY_CMD)) {
            WindowManagerDebugConfig.DEBUG_VISIBILITY = true;
            pw.println("Set DEBUG_VISIBILITY flag on");
        } else if (cmd.equals(DEBUG_LAYERS_CMD)) {
            WindowManagerDebugConfig.DEBUG_LAYERS = true;
            pw.println("Set DEBUG_LAYERS flag on");
        } else if (cmd.equals(DEBUG_ORIENTATION_CMD)) {
            WindowManagerDebugConfig.DEBUG_ORIENTATION = true;
            WindowManagerDebugConfig.DEBUG_APP_ORIENTATION = true;
            pw.println("Set DEBUG_ORIENTATION,DEBUG_APP_ORIENTATION  flag on");
        } else if (cmd.equals(DEBUG_APP_TRANSITIONS_CMD)) {
            WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS = true;
            pw.println("Set DEBUG_APP_TRANSITIONS flag on");
        } else if (cmd.equals(DEBUG_CONFIGURATION_CMD)) {
            WindowManagerDebugConfig.DEBUG_CONFIGURATION = true;
            pw.println("Set DEBUG_CONFIGURATION flag on");
        } else if (cmd.equals(DEBUG_SCREEN_ON_CMD)) {
            WindowManagerDebugConfig.DEBUG_SCREEN_ON = true;
            pw.println("Set DEBUG_SCREEN_ON flag on");
        } else if (cmd.equals(DEBUG_WALLPAPER_CMD)) {
            WindowManagerDebugConfig.DEBUG_WALLPAPER = true;
            WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT = true;
            pw.println("Set DEBUG_WALLPAPER flag on");
        } else if (cmd.equals(DEBUG_INPUT_CMD)) {
            WindowManagerDebugConfig.DEBUG_INPUT_METHOD = true;
            WindowManagerDebugConfig.DEBUG_INPUT = true;
            pw.println("Set DEBUG_INPUT flag on");
        } else if (cmd.equals(DEBUG_LAYOUT_CMD)) {
            WindowManagerDebugConfig.DEBUG_LAYOUT = true;
            pw.println("Set DEBUG_LAYOUT flag on");
        } else if (cmd.equals(DEBUG_DISPLAY_CMD)) {
            WindowManagerDebugConfig.DEBUG_DISPLAY = true;
            pw.println("Set DEBUG_LAYOUT flag on");
        } else if (cmd.equals(DEBUG_ALL_ON_CMD)) {
            WindowManagerDebugConfig.DEBUG_FOCUS = true;
            WindowManagerDebugConfig.DEBUG_VISIBILITY = true;
            WindowManagerDebugConfig.DEBUG_LAYERS = true;
            WindowManagerDebugConfig.DEBUG_ORIENTATION = true;
            WindowManagerDebugConfig.DEBUG_APP_ORIENTATION = true;
            WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS = true;
            WindowManagerDebugConfig.DEBUG_CONFIGURATION = true;
            WindowManagerDebugConfig.DEBUG_SCREEN_ON = true;
            WindowManagerDebugConfig.DEBUG_WALLPAPER = true;
            WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT = true;
            WindowManagerDebugConfig.DEBUG_INPUT_METHOD = true;
            WindowManagerDebugConfig.DEBUG_INPUT = true;
            WindowManagerDebugConfig.DEBUG_LAYOUT = true;
            WindowManagerDebugConfig.DEBUG_DISPLAY = true;
            pw.println("Set wms debug flag allOn");
        } else if (cmd.equals(DEBUG_ALL_OFF_CMD)) {
            WindowManagerDebugConfig.DEBUG_FOCUS = false;
            WindowManagerDebugConfig.DEBUG_VISIBILITY = false;
            WindowManagerDebugConfig.DEBUG_LAYERS = false;
            WindowManagerDebugConfig.DEBUG_ORIENTATION = false;
            WindowManagerDebugConfig.DEBUG_APP_ORIENTATION = false;
            WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS = false;
            WindowManagerDebugConfig.DEBUG_CONFIGURATION = false;
            WindowManagerDebugConfig.DEBUG_SCREEN_ON = false;
            WindowManagerDebugConfig.DEBUG_WALLPAPER = false;
            WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT = false;
            WindowManagerDebugConfig.DEBUG_INPUT_METHOD = false;
            WindowManagerDebugConfig.DEBUG_INPUT = false;
            WindowManagerDebugConfig.DEBUG_LAYOUT = false;
            WindowManagerDebugConfig.DEBUG_DISPLAY = false;
            pw.println("Set wms debug flag allOff");
        } else {
            pw.println("Bad window command.");
        }
    }

    private void dumpWindowsLocked(PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        pw.println("WINDOW MANAGER WINDOWS (dumpsys window windows)");
        dumpWindowsNoHeaderLocked(pw, dumpAll, windows);
    }

    private void dumpWindowsNoHeaderLocked(PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        int i;
        WindowState w;
        this.mRoot.dumpWindowsNoHeader(pw, dumpAll, windows);
        if (!this.mHidingNonSystemOverlayWindows.isEmpty()) {
            pw.println();
            pw.println("  Hiding System Alert Windows:");
            for (i = this.mHidingNonSystemOverlayWindows.size() - 1; i >= 0; i--) {
                w = (WindowState) this.mHidingNonSystemOverlayWindows.get(i);
                pw.print("  #");
                pw.print(i);
                pw.print(' ');
                pw.print(w);
                if (dumpAll) {
                    pw.println(":");
                    w.dump(pw, "    ", true);
                } else {
                    pw.println();
                }
            }
        }
        if (this.mPendingRemove.size() > 0) {
            pw.println();
            pw.println("  Remove pending for:");
            for (i = this.mPendingRemove.size() - 1; i >= 0; i--) {
                w = (WindowState) this.mPendingRemove.get(i);
                if (windows == null || windows.contains(w)) {
                    pw.print("  Remove #");
                    pw.print(i);
                    pw.print(' ');
                    pw.print(w);
                    if (dumpAll) {
                        pw.println(":");
                        w.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mForceRemoves != null && this.mForceRemoves.size() > 0) {
            pw.println();
            pw.println("  Windows force removing:");
            for (i = this.mForceRemoves.size() - 1; i >= 0; i--) {
                w = (WindowState) this.mForceRemoves.get(i);
                pw.print("  Removing #");
                pw.print(i);
                pw.print(' ');
                pw.print(w);
                if (dumpAll) {
                    pw.println(":");
                    w.dump(pw, "    ", true);
                } else {
                    pw.println();
                }
            }
        }
        if (this.mDestroySurface.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to destroy their surface:");
            for (i = this.mDestroySurface.size() - 1; i >= 0; i--) {
                w = (WindowState) this.mDestroySurface.get(i);
                if (windows == null || windows.contains(w)) {
                    pw.print("  Destroy #");
                    pw.print(i);
                    pw.print(' ');
                    pw.print(w);
                    if (dumpAll) {
                        pw.println(":");
                        w.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mLosingFocus.size() > 0) {
            pw.println();
            pw.println("  Windows losing focus:");
            for (i = this.mLosingFocus.size() - 1; i >= 0; i--) {
                w = (WindowState) this.mLosingFocus.get(i);
                if (windows == null || windows.contains(w)) {
                    pw.print("  Losing #");
                    pw.print(i);
                    pw.print(' ');
                    pw.print(w);
                    if (dumpAll) {
                        pw.println(":");
                        w.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mResizingWindows.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to resize:");
            for (i = this.mResizingWindows.size() - 1; i >= 0; i--) {
                w = (WindowState) this.mResizingWindows.get(i);
                if (windows == null || windows.contains(w)) {
                    pw.print("  Resizing #");
                    pw.print(i);
                    pw.print(' ');
                    pw.print(w);
                    if (dumpAll) {
                        pw.println(":");
                        w.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mWaitingForDrawn.size() > 0) {
            pw.println();
            pw.println("  Clients waiting for these windows to be drawn:");
            for (i = this.mWaitingForDrawn.size() - 1; i >= 0; i--) {
                WindowState win = (WindowState) this.mWaitingForDrawn.get(i);
                pw.print("  Waiting #");
                pw.print(i);
                pw.print(' ');
                pw.print(win);
            }
        }
        pw.println();
        pw.print("  mGlobalConfiguration=");
        pw.println(this.mRoot.getConfiguration());
        pw.print("  mHasPermanentDpad=");
        pw.println(this.mHasPermanentDpad);
        pw.print("  mCurrentFocus=");
        pw.println(this.mCurrentFocus);
        if (this.mLastFocus != this.mCurrentFocus) {
            pw.print("  mLastFocus=");
            pw.println(this.mLastFocus);
        }
        pw.print("  mFocusedApp=");
        pw.println(this.mFocusedApp);
        if (this.mInputMethodTarget != null) {
            pw.print("  mInputMethodTarget=");
            pw.println(this.mInputMethodTarget);
        }
        pw.print("  mInTouchMode=");
        pw.println(this.mInTouchMode);
        pw.print("  mLastDisplayFreezeDuration=");
        TimeUtils.formatDuration((long) this.mLastDisplayFreezeDuration, pw);
        if (this.mLastFinishedFreezeSource != null) {
            pw.print(" due to ");
            pw.print(this.mLastFinishedFreezeSource);
        }
        pw.println();
        pw.print("  mLastWakeLockHoldingWindow=");
        pw.print(this.mLastWakeLockHoldingWindow);
        pw.print(" mLastWakeLockObscuringWindow=");
        pw.print(this.mLastWakeLockObscuringWindow);
        pw.println();
        this.mInputMonitor.dump(pw, "  ");
        this.mUnknownAppVisibilityController.dump(pw, "  ");
        this.mTaskSnapshotController.dump(pw, "  ");
        if (dumpAll) {
            pw.print("  mSystemDecorLayer=");
            pw.print(this.mSystemDecorLayer);
            pw.print(" mScreenRect=");
            pw.println(this.mScreenRect.toShortString());
            if (this.mLastStatusBarVisibility != 0) {
                pw.print("  mLastStatusBarVisibility=0x");
                pw.println(Integer.toHexString(this.mLastStatusBarVisibility));
            }
            if (this.mInputMethodWindow != null) {
                pw.print("  mInputMethodWindow=");
                pw.println(this.mInputMethodWindow);
            }
            this.mWindowPlacerLocked.dump(pw, "  ");
            this.mRoot.mWallpaperController.dump(pw, "  ");
            pw.print("  mSystemBooted=");
            pw.print(this.mSystemBooted);
            pw.print(" mDisplayEnabled=");
            pw.println(this.mDisplayEnabled);
            this.mRoot.dumpLayoutNeededDisplayIds(pw);
            pw.print("  mTransactionSequence=");
            pw.println(this.mTransactionSequence);
            pw.print("  mDisplayFrozen=");
            pw.print(this.mDisplayFrozen);
            pw.print(" windows=");
            pw.print(this.mWindowsFreezingScreen);
            pw.print(" client=");
            pw.print(this.mClientFreezingScreen);
            pw.print(" apps=");
            pw.print(this.mAppsFreezingScreen);
            pw.print(" waitingForConfig=");
            pw.println(this.mWaitingForConfig);
            DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
            pw.print("  mRotation=");
            pw.print(defaultDisplayContent.getRotation());
            pw.print(" mAltOrientation=");
            pw.println(defaultDisplayContent.getAltOrientation());
            pw.print("  mLastWindowForcedOrientation=");
            pw.print(defaultDisplayContent.getLastWindowForcedOrientation());
            pw.print(" mLastOrientation=");
            pw.println(defaultDisplayContent.getLastOrientation());
            pw.print("  mDeferredRotationPauseCount=");
            pw.println(this.mDeferredRotationPauseCount);
            pw.print("  Animation settings: disabled=");
            pw.print(this.mAnimationsDisabled);
            pw.print(" window=");
            pw.print(this.mWindowAnimationScaleSetting);
            pw.print(" transition=");
            pw.print(this.mTransitionAnimationScaleSetting);
            pw.print(" animator=");
            pw.println(this.mAnimatorDurationScaleSetting);
            pw.print("  mSkipAppTransitionAnimation=");
            pw.println(this.mSkipAppTransitionAnimation);
            pw.println("  mLayoutToAnim:");
            this.mAppTransition.dump(pw, "    ");
            if (this.mRecentsAnimationController != null) {
                pw.print("  mRecentsAnimationController=");
                pw.println(this.mRecentsAnimationController);
                this.mRecentsAnimationController.dump(pw, "    ");
            }
        }
    }

    private boolean dumpWindows(PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<WindowState> windows = new ArrayList();
        if ("apps".equals(name) || "visible".equals(name) || "visible-apps".equals(name)) {
            boolean appsOnly = name.contains("apps");
            boolean visibleOnly = name.contains("visible");
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    if (appsOnly) {
                        this.mRoot.dumpDisplayContents(pw);
                    }
                    this.mRoot.forAllWindows((Consumer) new -$$Lambda$WindowManagerService$oNT-Y2LsGFr06rEAi5_MG-71m5U(visibleOnly, appsOnly, windows), true);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.getWindowsByName(windows, name);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
        }
        if (windows.size() <= 0) {
            return false;
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                dumpWindowsLocked(pw, dumpAll, windows);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return true;
    }

    static /* synthetic */ void lambda$dumpWindows$5(boolean visibleOnly, boolean appsOnly, ArrayList windows, WindowState w) {
        if (visibleOnly && !w.mWinAnimator.getShown()) {
            return;
        }
        if (!appsOnly || w.mAppToken != null) {
            windows.add(w);
        }
    }

    private void dumpLastANRLocked(PrintWriter pw) {
        pw.println("WINDOW MANAGER LAST ANR (dumpsys window lastanr)");
        if (this.mLastANRState == null) {
            pw.println("  <no ANR has occurred since boot>");
        } else {
            pw.println(this.mLastANRState);
        }
    }

    void saveANRStateLocked(AppWindowToken appWindowToken, WindowState windowState, String reason) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter(sw, false, 1024);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ANR time: ");
        stringBuilder.append(DateFormat.getDateTimeInstance().format(new Date()));
        pw.println(stringBuilder.toString());
        if (appWindowToken != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Application at fault: ");
            stringBuilder.append(appWindowToken.stringName);
            pw.println(stringBuilder.toString());
        }
        if (windowState != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Window at fault: ");
            stringBuilder.append(windowState.mAttrs.getTitle());
            pw.println(stringBuilder.toString());
        }
        if (reason != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Reason: ");
            stringBuilder.append(reason);
            pw.println(stringBuilder.toString());
        }
        if (!this.mWinAddedSinceNullFocus.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Windows added since null focus: ");
            stringBuilder.append(this.mWinAddedSinceNullFocus);
            pw.println(stringBuilder.toString());
        }
        if (!this.mWinRemovedSinceNullFocus.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Windows removed since null focus: ");
            stringBuilder.append(this.mWinRemovedSinceNullFocus);
            pw.println(stringBuilder.toString());
        }
        pw.println();
        dumpWindowsNoHeaderLocked(pw, true, null);
        pw.println();
        pw.println("Last ANR continued");
        this.mRoot.dumpDisplayContents(pw);
        pw.close();
        this.mLastANRState = sw.toString();
        this.mH.removeMessages(38);
        this.mH.sendEmptyMessageDelayed(38, SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        PriorityDump.dump(this.mPriorityDumper, fd, pw, args);
    }

    private void doDump(FileDescriptor fd, PrintWriter pw, String[] args, boolean useProto) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            boolean dumpAll = false;
            int opti = 0;
            while (opti < args.length) {
                String opt = args[opti];
                if (opt == null || opt.length() <= 0 || opt.charAt(0) != '-') {
                    break;
                }
                opti++;
                if ("-a".equals(opt)) {
                    dumpAll = true;
                } else if ("-h".equals(opt)) {
                    pw.println("Window manager dump options:");
                    pw.println("  [-a] [-h] [cmd] ...");
                    pw.println("  cmd may be one of:");
                    pw.println("    l[astanr]: last ANR information");
                    pw.println("    p[policy]: policy state");
                    pw.println("    a[animator]: animator state");
                    pw.println("    s[essions]: active sessions");
                    pw.println("    surfaces: active surfaces (debugging enabled only)");
                    pw.println("    d[isplays]: active display contents");
                    pw.println("    t[okens]: token list");
                    pw.println("    w[indows]: window list");
                    pw.println("    h[andler]: dump message queue");
                    if (IS_DEBUG_VERSION) {
                        pw.println("    hwDebugWms[Focus|Visibility|Layer|Orientation|Transition|Configuration|Screen|AllOn|AllOff]: turn wms debug flag on or off");
                    }
                    pw.println("  cmd may also be a NAME to dump windows.  NAME may");
                    pw.println("    be a partial substring in a window name, a");
                    pw.println("    Window hex object identifier, or");
                    pw.println("    \"all\" for all windows, or");
                    pw.println("    \"visible\" for the visible windows.");
                    pw.println("    \"visible-apps\" for the visible app windows.");
                    pw.println("  -a: include all available server state.");
                    pw.println("  --proto: output dump in protocol buffer format.");
                    return;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown argument: ");
                    stringBuilder.append(opt);
                    stringBuilder.append("; use -h for help");
                    pw.println(stringBuilder.toString());
                }
            }
            if (useProto) {
                ProtoOutputStream proto = new ProtoOutputStream(fd);
                synchronized (this.mWindowMap) {
                    try {
                        boostPriorityForLockedSection();
                        writeToProtoLocked(proto, false);
                    } finally {
                        while (true) {
                        }
                        resetPriorityAfterLockedSection();
                    }
                }
                proto.flush();
            } else if (opti < args.length) {
                String cmd = args[opti];
                int opti2 = opti + 1;
                if ("lastanr".equals(cmd) || "l".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpLastANRLocked(pw);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("policy".equals(cmd) || "p".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpPolicyLocked(pw, args, true);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("animator".equals(cmd) || "a".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpAnimatorLocked(pw, args, true);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("sessions".equals(cmd) || "s".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpSessionsLocked(pw, true);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("displays".equals(cmd) || "d".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpDisplayContents(pw);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("tokens".equals(cmd) || "t".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpTokensLocked(pw, true);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("windows".equals(cmd) || "w".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(pw, true, null);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("all".equals(cmd) || "a".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(pw, true, null);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("containers".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpChildrenNames(pw, " ");
                            pw.println(" ");
                            this.mRoot.forAllWindows((Consumer) new -$$Lambda$WindowManagerService$bLtA9qjvcyGYU0ingebsLSeUie8(pw), true);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("handler".equals(cmd) || "h".equals(cmd)) {
                    synchronized (this.mWindowMap) {
                        try {
                            boostPriorityForLockedSection();
                            dumpHandlerLocked(pw);
                        } finally {
                            while (true) {
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if (IS_DEBUG_VERSION && !TextUtils.isEmpty(cmd) && cmd.startsWith(DEBUG_PREFIX)) {
                    setWmsDebugFlag(pw, cmd);
                } else {
                    if (!dumpWindows(pw, cmd, args, opti2, dumpAll)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Bad window command, or no windows match: ");
                        stringBuilder2.append(cmd);
                        pw.println(stringBuilder2.toString());
                        pw.println("Use -h for help.");
                    }
                }
            } else {
                synchronized (this.mWindowMap) {
                    try {
                        boostPriorityForLockedSection();
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpLastANRLocked(pw);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpPolicyLocked(pw, args, dumpAll);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpAnimatorLocked(pw, args, dumpAll);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpSessionsLocked(pw, dumpAll);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        this.mRoot.dumpDisplayContents(pw);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpTokensLocked(pw, dumpAll);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpWindowsLocked(pw, dumpAll, null);
                        pw.println();
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpHandlerLocked(pw);
                    } finally {
                        while (true) {
                        }
                        resetPriorityAfterLockedSection();
                    }
                }
            }
        }
    }

    public void monitor() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    DisplayContent getDefaultDisplayContentLocked() {
        return this.mRoot.getDisplayContent(0);
    }

    public void onDisplayAdded(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayManager.getDisplay(displayId) != null) {
                    displayReady(displayId);
                }
                this.mWindowPlacerLocked.requestTraversal();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onDisplayRemoved(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mAnimator.removeDisplayLocked(displayId);
                this.mWindowPlacerLocked.requestTraversal();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onOverlayChanged() {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onOverlayChangedLw();
                getDefaultDisplayContentLocked().updateDisplayInfo();
                requestTraversal();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onDisplayChanged(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.updateDisplayInfo();
                }
                this.mWindowPlacerLocked.requestTraversal();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public Object getWindowManagerLock() {
        return this.mWindowMap;
    }

    public void setWillReplaceWindow(IBinder token, boolean animate) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(token);
                String str;
                StringBuilder stringBuilder;
                if (appWindowToken == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set replacing window on non-existing app token ");
                    stringBuilder.append(token);
                    Slog.w(str, stringBuilder.toString());
                } else if (appWindowToken.hasContentToDisplay()) {
                    appWindowToken.setWillReplaceWindows(animate);
                    resetPriorityAfterLockedSection();
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set replacing window on app token with no content");
                    stringBuilder.append(token);
                    Slog.w(str, stringBuilder.toString());
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void setWillReplaceWindows(IBinder token, boolean childrenOnly) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(token);
                String str;
                StringBuilder stringBuilder;
                if (appWindowToken == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set replacing window on non-existing app token ");
                    stringBuilder.append(token);
                    Slog.w(str, stringBuilder.toString());
                } else if (appWindowToken.hasContentToDisplay()) {
                    if (childrenOnly) {
                        appWindowToken.setWillReplaceChildWindows();
                    } else {
                        appWindowToken.setWillReplaceWindows(false);
                    }
                    scheduleClearWillReplaceWindows(token, true);
                    resetPriorityAfterLockedSection();
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set replacing window on app token with no content");
                    stringBuilder.append(token);
                    Slog.w(str, stringBuilder.toString());
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0033, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:14:0x0036, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void scheduleClearWillReplaceWindows(IBinder token, boolean replacing) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(token);
                if (appWindowToken == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to reset replacing window on non-existing app token ");
                    stringBuilder.append(token);
                    Slog.w(str, stringBuilder.toString());
                } else if (replacing) {
                    scheduleWindowReplacementTimeouts(appWindowToken);
                } else {
                    appWindowToken.clearWillReplaceWindows();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void scheduleWindowReplacementTimeouts(AppWindowToken appWindowToken) {
        if (!this.mWindowReplacementTimeouts.contains(appWindowToken)) {
            this.mWindowReplacementTimeouts.add(appWindowToken);
        }
        this.mH.removeMessages(46);
        this.mH.sendEmptyMessageDelayed(46, 2000);
    }

    public int getDockedStackSide() {
        int dockSide;
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                TaskStack dockedStack = getDefaultDisplayContentLocked().getSplitScreenPrimaryStackIgnoringVisibility();
                dockSide = dockedStack == null ? -1 : dockedStack.getDockSide();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
        return dockSide;
    }

    public void setDockedStackResizing(boolean resizing) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setResizing(resizing);
                requestTraversal();
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setDockedStackDividerTouchRegion(Rect touchRegion) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setTouchRegion(touchRegion);
                setFocusTaskRegionLocked(null);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setResizeDimLayer(boolean visible, int targetWindowingMode, float alpha) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setResizeDimLayer(visible, targetWindowingMode, alpha);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setForceResizableTasks(boolean forceResizableTasks) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mForceResizableTasks = forceResizableTasks;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setSupportsPictureInPicture(boolean supportsPictureInPicture) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mSupportsPictureInPicture = supportsPictureInPicture;
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    static int dipToPixel(int dip, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(1, (float) dip, displayMetrics);
    }

    public void registerDockedStackListener(IDockedStackListener listener) {
        if (checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerDockedStackListener()") || checkCallingPermission("huawei.android.permission.MULTIWINDOW_SDK", "registerDockedStackListener()")) {
            synchronized (this.mWindowMap) {
                try {
                    boostPriorityForLockedSection();
                    getDefaultDisplayContentLocked().mDividerControllerLocked.registerDockedStackListener(listener);
                } finally {
                    while (true) {
                    }
                    resetPriorityAfterLockedSection();
                }
            }
        }
    }

    public void registerPinnedStackListener(int displayId, IPinnedStackListener listener) {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerPinnedStackListener()") || !this.mSupportsPictureInPicture) {
            return;
        }
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            HwPCUtils.log(TAG, "ignore pinned stack listener in pad pc mode");
            return;
        }
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.getDisplayContent(displayId).getPinnedStackController().registerPinnedStackListener(listener);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
        try {
            WindowState focusedWindow = getFocusedWindow();
            if (focusedWindow != null && focusedWindow.mClient != null) {
                focusedWindow.mClient.requestAppKeyboardShortcuts(receiver, deviceId);
            }
        } catch (RemoteException e) {
        }
    }

    public void getStableInsets(int displayId, Rect outInsets) throws RemoteException {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                getStableInsetsLocked(displayId, outInsets);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    void getStableInsetsLocked(int displayId, Rect outInsets) {
        outInsets.setEmpty();
        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
        if (dc != null) {
            DisplayInfo di = dc.getDisplayInfo();
            this.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, di.displayCutout, outInsets);
        }
    }

    void intersectDisplayInsetBounds(Rect display, Rect insets, Rect inOutBounds) {
        this.mTmpRect3.set(display);
        this.mTmpRect3.inset(insets);
        inOutBounds.intersect(this.mTmpRect3);
    }

    /* JADX WARNING: Missing block: B:9:0x001a, code:
            r3 = r9.mWindowMap;
     */
    /* JADX WARNING: Missing block: B:10:0x001c, code:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            boostPriorityForLockedSection();
     */
    /* JADX WARNING: Missing block: B:13:0x0026, code:
            if (r9.mDragDropController.dragDropActiveLocked() == false) goto L_0x002d;
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:15:0x0029, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:16:0x002c, code:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code:
            r0 = windowForClientLocked((com.android.server.wm.Session) null, r10, false);
     */
    /* JADX WARNING: Missing block: B:20:0x0033, code:
            if (r0 != null) goto L_0x0050;
     */
    /* JADX WARNING: Missing block: B:21:0x0035, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Bad requesting window ");
            r5.append(r10);
            android.util.Slog.w(r4, r5.toString());
     */
    /* JADX WARNING: Missing block: B:22:0x004b, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:23:0x004c, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:24:0x004f, code:
            return;
     */
    /* JADX WARNING: Missing block: B:26:?, code:
            r4 = r0.getDisplayContent();
     */
    /* JADX WARNING: Missing block: B:27:0x0054, code:
            if (r4 != null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:28:0x0056, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:29:0x0057, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:30:0x005a, code:
            return;
     */
    /* JADX WARNING: Missing block: B:32:?, code:
            r5 = r4.getTouchableWinAtPointLocked(r1, r2);
     */
    /* JADX WARNING: Missing block: B:33:0x0060, code:
            if (r5 == r0) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:34:0x0062, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:35:0x0063, code:
            resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:36:0x0066, code:
            return;
     */
    /* JADX WARNING: Missing block: B:38:?, code:
            r5.mClient.updatePointerIcon(r5.translateToWindowX(r1), r5.translateToWindowY(r2));
     */
    /* JADX WARNING: Missing block: B:41:?, code:
            android.util.Slog.w(TAG, "unable to update pointer icon");
     */
    /* JADX WARNING: Missing block: B:48:0x0085, code:
            resetPriorityAfterLockedSection();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void updatePointerIcon(IWindow client) {
        synchronized (this.mMousePositionTracker) {
            if (this.mMousePositionTracker.mLatestEventWasMouse) {
                float mouseX = this.mMousePositionTracker.mLatestMouseX;
                float mouseY = this.mMousePositionTracker.mLatestMouseY;
            } else {
                return;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void restorePointerIconLocked(DisplayContent displayContent, float latestX, float latestY) {
        this.mMousePositionTracker.updatePosition(latestX, latestY);
        WindowState windowUnderPointer = displayContent.getTouchableWinAtPointLocked(latestX, latestY);
        if (windowUnderPointer != null) {
            try {
                windowUnderPointer.mClient.updatePointerIcon(windowUnderPointer.translateToWindowX(latestX), windowUnderPointer.translateToWindowY(latestY));
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "unable to restore pointer icon");
                return;
            }
        }
        InputManager.getInstance().setPointerIconType(1000);
    }

    void updateTapExcludeRegion(IWindow client, int regionId, int left, int top, int width, int height) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                WindowState callingWin = windowForClientLocked((Session) null, client, false);
                if (callingWin == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad requesting window ");
                    stringBuilder.append(client);
                    Slog.w(str, stringBuilder.toString());
                } else {
                    callingWin.updateTapExcludeRegion(regionId, left, top, width, height);
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void dontOverrideDisplayInfo(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = getDisplayContentOrCreate(displayId);
                if (dc != null) {
                    dc.mShouldOverrideDisplayConfiguration = false;
                    this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayId, null);
                } else {
                    throw new IllegalArgumentException("Trying to register a non existent display.");
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutKeyReceiver) throws RemoteException {
        if (checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerShortcutKey")) {
            this.mPolicy.registerShortcutKey(shortcutCode, shortcutKeyReceiver);
            return;
        }
        throw new SecurityException("Requires REGISTER_WINDOW_MANAGER_LISTENERS permission");
    }

    public void requestUserActivityNotification() {
        if (checkCallingPermission("android.permission.USER_ACTIVITY", "requestUserActivityNotification()")) {
            this.mPolicy.requestUserActivityNotification();
            return;
        }
        throw new SecurityException("Requires USER_ACTIVITY permission");
    }

    void markForSeamlessRotation(WindowState w, boolean seamlesslyRotated) {
        if (seamlesslyRotated != w.mSeamlesslyRotated) {
            w.mSeamlesslyRotated = seamlesslyRotated;
            if (seamlesslyRotated) {
                this.mSeamlessRotationCount++;
            } else {
                this.mSeamlessRotationCount--;
            }
            if (this.mSeamlessRotationCount == 0) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.i(TAG, "Performing post-rotate rotation after seamless rotation");
                }
                finishSeamlessRotation();
                DisplayContent displayContent = w.getDisplayContent();
                if (displayContent.updateRotationUnchecked()) {
                    this.mH.obtainMessage(18, Integer.valueOf(displayContent.getDisplayId())).sendToTarget();
                }
            }
        }
    }

    void registerAppFreezeListener(AppFreezeListener listener) {
        if (!this.mAppFreezeListeners.contains(listener)) {
            this.mAppFreezeListeners.add(listener);
        }
    }

    void unregisterAppFreezeListener(AppFreezeListener listener) {
        this.mAppFreezeListeners.remove(listener);
    }

    public final void performhwLayoutAndPlaceSurfacesLocked() {
        this.mWindowPlacerLocked.performSurfacePlacement();
    }

    protected boolean canBeFloatImeTarget(WindowState w) {
        int fl = w.mAttrs.flags & 131080;
        if (fl == 0 || fl == 131080 || w.mAttrs.type == 3) {
            return w.isVisibleOrAdding();
        }
        return false;
    }

    private void printFreezingDisplayLogs() {
        int appsCount = this.mOpeningApps.size();
        for (int i = 0; i < appsCount; i++) {
            AppWindowToken wtoken = (AppWindowToken) this.mOpeningApps.valueAt(i);
            StringBuilder builder = new StringBuilder();
            builder.append("opening app wtoken = ");
            builder.append(wtoken.toString());
            builder.append(", allDrawn= ");
            builder.append(wtoken.allDrawn);
            builder.append(", startingDisplayed =  ");
            builder.append(wtoken.startingDisplayed);
            builder.append(", startingMoved =  ");
            builder.append(wtoken.startingMoved);
            builder.append(", isRelaunching =  ");
            builder.append(wtoken.isRelaunching());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("printFreezingDisplayLogs");
            stringBuilder.append(builder.toString());
            Slog.d(str, stringBuilder.toString());
        }
    }

    private boolean isPrintAllWindowsDrawnLogs() {
        if (SystemClock.elapsedRealtime() - this.mWaitAllWindowDrawStartTime > 1000) {
            return true;
        }
        return false;
    }

    private boolean isLandScapeMultiWindowMode() {
        int dockSide = getDockedStackSide();
        int rot = getDefaultDisplayContentLocked().getDisplay().getRotation();
        return (rot == 1 || rot == 3) && dockSide != -1;
    }

    public void inSurfaceTransaction(Runnable exec) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                SurfaceControl.openTransaction();
                exec.run();
                SurfaceControl.closeTransaction();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void disableNonVrUi(boolean disable) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                boolean showAlertWindowNotifications = disable ^ 1;
                if (showAlertWindowNotifications == this.mShowAlertWindowNotifications) {
                } else {
                    this.mShowAlertWindowNotifications = showAlertWindowNotifications;
                    for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                        ((Session) this.mSessions.valueAt(i)).setShowingAlertWindowNotificationAllowed(this.mShowAlertWindowNotifications);
                    }
                    resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    boolean hasWideColorGamutSupport() {
        return this.mHasWideColorGamutSupport && SystemProperties.getInt("persist.sys.sf.native_mode", 0) != 1;
    }

    void updateNonSystemOverlayWindowsVisibilityIfNeeded(WindowState win, boolean surfaceShown) {
        if (win.hideNonSystemOverlayWindowsWhenVisible() || this.mHidingNonSystemOverlayWindows.contains(win)) {
            boolean systemAlertWindowsHidden = this.mHidingNonSystemOverlayWindows.isEmpty() ^ 1;
            if (!surfaceShown) {
                this.mHidingNonSystemOverlayWindows.remove(win);
            } else if (!this.mHidingNonSystemOverlayWindows.contains(win)) {
                this.mHidingNonSystemOverlayWindows.add(win);
            }
            boolean hideSystemAlertWindows = this.mHidingNonSystemOverlayWindows.isEmpty() ^ 1;
            if (systemAlertWindowsHidden != hideSystemAlertWindows) {
                this.mRoot.forAllWindows((Consumer) new -$$Lambda$WindowManagerService$Mfs-IxxijHiEAEKbLIL1x_17ck0(hideSystemAlertWindows), false);
            }
        }
    }

    public void applyMagnificationSpec(MagnificationSpec spec) {
        getDefaultDisplayContentLocked().applyMagnificationSpec(spec);
    }

    Builder makeSurfaceBuilder(SurfaceSession s) {
        return this.mSurfaceBuilderFactory.make(s);
    }

    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }

    public IHwWindowManagerServiceEx getWindowManagerServiceEx() {
        return this.mHwWMSEx;
    }

    public HwWMDAMonitorProxy getWMMonitor() {
        return this.mWMProxy;
    }

    public WindowHashMap getWindowMap() {
        return this.mWindowMap;
    }

    public TaskSnapshotController getTaskSnapshotController() {
        return this.mTaskSnapshotController;
    }

    public AppOpsManager getAppOps() {
        return this.mAppOps;
    }

    public WindowManagerPolicy getPolicy() {
        return this.mPolicy;
    }

    public RootWindowContainer getRoot() {
        return this.mRoot;
    }

    public void getAppDisplayRect(float appMaxRatio, Rect rect, int left) {
        if (this.mHwWMSEx != null) {
            this.mHwWMSEx.getAppDisplayRect(appMaxRatio, rect, left, getDefaultDisplayContentLocked().getRotation());
        }
    }

    public float getDeviceMaxRatio() {
        if (this.mHwWMSEx != null) {
            return this.mHwWMSEx.getDeviceMaxRatio();
        }
        return 0.0f;
    }

    public void notifyFingerWinCovered(boolean covered, Rect frame) {
        if (this.mHwWMSEx != null) {
            this.mHwWMSEx.notifyFingerWinCovered(covered, frame);
        }
    }

    public void layoutWindowForPadPCMode(WindowState win, Rect pf, Rect df, Rect cf, Rect vf, int contentBottom) {
        WindowState windowState = win;
        if (windowState instanceof WindowState) {
            this.mHwWMSEx.layoutWindowForPadPCMode((WindowState) windowState, this.mInputMethodTarget, this.mInputMethodWindow, pf, df, cf, vf, contentBottom);
        }
    }

    void sendSetRunningRemoteAnimation(int pid, boolean runningRemoteAnimation) {
        this.mH.obtainMessage(59, pid, runningRemoteAnimation).sendToTarget();
    }

    void startSeamlessRotation() {
        this.mSeamlessRotationCount = 0;
        this.mRotatingSeamlessly = true;
    }

    void finishSeamlessRotation() {
        this.mRotatingSeamlessly = false;
    }

    public void onLockTaskStateChanged(int lockTaskState) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onLockTaskStateChangedLw(lockTaskState);
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setAodShowing(boolean aodShowing) {
        synchronized (this.mWindowMap) {
            try {
                boostPriorityForLockedSection();
                if (this.mPolicy.setAodShowing(aodShowing)) {
                    this.mWindowPlacerLocked.performSurfacePlacement();
                }
            } finally {
                while (true) {
                }
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isInDisplayFrozen() {
        return this.mDisplayFrozen;
    }
}
