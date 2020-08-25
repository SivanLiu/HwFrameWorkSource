package com.android.server.wm;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityTaskManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.IAssistDataReceiver;
import android.app.admin.DevicePolicyCache;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.configstore.V1_0.ISurfaceFlingerConfigs;
import android.hardware.configstore.V1_0.OptionalBool;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.HwFoldScreenState;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.iawareperf.UniPerf;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
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
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.CoordinationModeUtils;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.Choreographer;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.DragEvent;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IDisplayFoldListener;
import android.view.IDockedStackListener;
import android.view.IHwRotateObserver;
import android.view.IInputFilter;
import android.view.IOnKeyguardExitResult;
import android.view.IPinnedStackListener;
import android.view.IRecentsAnimationRunner;
import android.view.IRotationWatcher;
import android.view.ISystemGestureExclusionListener;
import android.view.IWallpaperVisibilityListener;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InsetsState;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowContentFrameStats;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.IResultReceiver;
import com.android.internal.os.ZygoteInit;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.LatencyTracker;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.view.WindowManagerPolicyThread;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.FgThread;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.input.InputManagerService;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.ShutdownThread;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowState;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import com.huawei.android.view.IHwMultiDisplayBitmapDragStartListener;
import com.huawei.android.view.IHwMultiDisplayDragStartListener;
import com.huawei.android.view.IHwMultiDisplayDragStateListener;
import com.huawei.android.view.IHwMultiDisplayDropStartListener;
import com.huawei.android.view.IHwMultiDisplayDroppableListener;
import com.huawei.android.view.IHwMultiDisplayPhoneOperateListener;
import com.huawei.android.view.IHwWMDAMonitorCallback;
import com.huawei.android.view.IHwWindowManager;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WindowManagerService extends AbsWindowManagerService implements IHwWindowManagerInner, Watchdog.Monitor, WindowManagerPolicy.WindowManagerFuncs {
    private static final int ADD_SURFACE = 1;
    private static final long ALLWINDOWDRAW_MAX_TIMEOUT_TIME = 1000;
    private static final boolean ALWAYS_KEEP_CURRENT = true;
    private static final int ANIMATION_COMPLETED_TIMEOUT_MS = 5000;
    private static final int ANIMATION_DURATION_SCALE = 2;
    private static final int BOOT_ANIMATION_POLL_INTERVAL = 200;
    private static final String BOOT_ANIMATION_SERVICE = "bootanim";
    static final boolean CUSTOM_SCREEN_ROTATION = true;
    public static final String DEBUG_ALL_OFF_CMD = "debugAllOff";
    public static final String DEBUG_ALL_ON_CMD = "debugAllOn";
    public static final String DEBUG_APP_TRANSITIONS_CMD = "debugTransition";
    public static final String DEBUG_CONFIGURATION_CMD = "debugConfiguration";
    public static final String DEBUG_DISPLAY_CMD = "debugDisplay";
    public static final String DEBUG_FOCUS_CMD = "debugFocus";
    public static final String DEBUG_INPUT_CMD = "debugInput";
    public static final String DEBUG_LAYERS_CMD = "debugLayer";
    public static final String DEBUG_LAYOUT_CMD = "debugLayout";
    public static final String DEBUG_ORIENTATION_CMD = "debugOrientation";
    private static final String DEBUG_PREFIX = "debug";
    public static final String DEBUG_SCREEN_ON_CMD = "debugScreen";
    public static final String DEBUG_STARTING_WINDOW_CMD = "debugStartingWindow";
    public static final String DEBUG_VISIBILITY_CMD = "debugVisibility";
    public static final String DEBUG_WALLPAPER_CMD = "debugWallpaper";
    private static final int DEFALUT_VALUE = -1;
    static final long DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS = 5000000000L;
    private static final String DENSITY_OVERRIDE = "ro.config.density_override";
    public static final String FROM_FOLD_MODE_KEY = "fromFoldMode";
    static final int GESTURE_NAVUP_TIMEOUT_DURATION = 450;
    private static final int INPUT_DEVICES_READY_FOR_SAFE_MODE_DETECTION_TIMEOUT_MILLIS = 1000;
    static final boolean IS_DEBUG_VERSION = (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3);
    public static final String IS_FOLD_KEY = "isFold";
    public static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    public static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", ""));
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final int LAYER_OFFSET_DIM = 1;
    static final int LAYER_OFFSET_THUMBNAIL = 4;
    static final int LAYOUT_REPEAT_THRESHOLD = 4;
    static final int LAZY_MODE_LEFT = 1;
    static final int LAZY_MODE_RIGHT = 2;
    public static final float LAZY_MODE_SCALE = 0.75f;
    static final int MAX_ANIMATION_DURATION = 10000;
    private static final int MAX_SCREENSHOT_RETRIES = 3;
    private static final int MIN_GESTURE_EXCLUSION_LIMIT_DP = 200;
    static final boolean PROFILE_ORIENTATION = false;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    /* access modifiers changed from: private */
    public static final boolean PROP_FOLD_SWITCH_DEBUG = SystemProperties.getBoolean("persist.debug.fold_switch", true);
    private static final int QUICK_STEP_TOUCH_SLOP_PX = 30;
    private static final int REMOVE_SURFACE = 0;
    static final int SEAMLESS_ROTATION_TIMEOUT_DURATION = 2000;
    private static final String SIZE_OVERRIDE = "ro.config.size_override";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_SECURE = "ro.secure";
    private static final String TAG = "WindowManager";
    public static final String TO_FOLD_MODE_KEY = "toFoldMode";
    private static final int TRANSITION_ANIMATION_SCALE = 1;
    static final int TYPE_LAYER_MULTIPLIER = 10000;
    static final int TYPE_LAYER_OFFSET = 1000;
    static final int UPDATE_FOCUS_NORMAL = 0;
    static final int UPDATE_FOCUS_PLACING_SURFACES = 2;
    static final int UPDATE_FOCUS_REMOVING_FOCUS = 4;
    static final int UPDATE_FOCUS_WILL_ASSIGN_LAYERS = 1;
    static final int UPDATE_FOCUS_WILL_PLACE_SURFACES = 3;
    static final int WINDOWS_FREEZING_SCREENS_ACTIVE = 1;
    static final int WINDOWS_FREEZING_SCREENS_NONE = 0;
    static final int WINDOWS_FREEZING_SCREENS_TIMEOUT = 2;
    private static final int WINDOW_ANIMATION_SCALE = 0;
    static final int WINDOW_FREEZE_TIMEOUT_DURATION = 2000;
    static final int WINDOW_LAYER_MULTIPLIER = 5;
    static final int WINDOW_REPLACEMENT_TIMEOUT_DURATION = 2000;
    private static final String WINDOW_TITLE_PC_LAUNCHER = "SecondLauncher";
    static final boolean localLOGV = false;
    public static boolean mSupporInputMethodFilletAdaptation = SystemProperties.getBoolean("ro.config.support_inputmethod_fillet_adaptation", false);
    private static WindowManagerService sInstance;
    static WindowManagerThreadPriorityBooster sThreadPriorityBooster = new WindowManagerThreadPriorityBooster();
    AccessibilityController mAccessibilityController;
    final IActivityManager mActivityManager;
    final WindowManagerInternal.AppTransitionListener mActivityManagerAppTransitionNotifier;
    final IActivityTaskManager mActivityTaskManager;
    /* access modifiers changed from: private */
    public int mAddValue;
    final boolean mAllowAnimationsInLowPowerMode;
    final boolean mAllowBootMessages;
    boolean mAllowTheaterModeWakeFromLayout;
    final ActivityManagerInternal mAmInternal;
    final Handler mAnimationHandler;
    final ArrayMap<AnimationAdapter, SurfaceAnimator> mAnimationTransferMap;
    /* access modifiers changed from: private */
    public boolean mAnimationsDisabled;
    final WindowAnimator mAnimator;
    /* access modifiers changed from: private */
    public float mAnimatorDurationScaleSetting;
    final ArrayList<AppFreezeListener> mAppFreezeListeners;
    final AppOpsManager mAppOps;
    int mAppsFreezingScreen;
    final ActivityTaskManagerInternal mAtmInternal;
    final ActivityTaskManagerService mAtmService;
    boolean mBootAnimationStopped;
    private int mBootMetricsLogIndex = 0;
    private final BroadcastReceiver mBroadcastReceiver;
    CircularDisplayMask mCircularDisplayMask;
    boolean mClientFreezingScreen;
    final Context mContext;
    private int mCurrentFoldDisplayMode;
    int[] mCurrentProfileIds;
    int mCurrentUserId;
    final ArrayList<WindowState> mDestroyPreservedSurface;
    final ArrayList<WindowState> mDestroySurface;
    boolean mDisableTransitionAnimation;
    boolean mDisplayEnabled;
    long mDisplayFreezeTime;
    boolean mDisplayFrozen;
    final DisplayManager mDisplayManager;
    final DisplayManagerInternal mDisplayManagerInternal;
    boolean mDisplayReady;
    final DisplayWindowSettings mDisplayWindowSettings;
    Rect mDockedStackCreateBounds;
    int mDockedStackCreateMode;
    final DragDropController mDragDropController;
    final long mDrawLockTimeoutMillis;
    EmulatorDisplayOverlay mEmulatorDisplayOverlay;
    private int mEnterAnimId;
    private boolean mEventDispatchingEnabled;
    private int mExitAnimId;
    boolean mFocusMayChange;
    Bundle mFoldScreenInfo;
    boolean mForceDesktopModeOnExternalDisplays;
    boolean mForceDisplayEnabled;
    final ArrayList<WindowState> mForceRemoves;
    boolean mForceResizableTasks;
    private int mFrozenDisplayId;
    private HwFoldScreenManagerInternal mFsmInternal;
    final WindowManagerGlobalLock mGlobalLock;
    final H mH;
    boolean mHardKeyboardAvailable;
    WindowManagerInternal.OnHardKeyboardStatusChangeListener mHardKeyboardStatusChangeListener;
    private boolean mHasHdrSupport;
    final boolean mHasPermanentDpad;
    private boolean mHasWideColorGamutSupport;
    private ArrayList<WindowState> mHidingNonSystemOverlayWindows;
    final HighRefreshRateBlacklist mHighRefreshRateBlacklist;
    private Session mHoldingScreenOn;
    protected PowerManager.WakeLock mHoldingScreenWakeLock;
    HwInnerWindowManagerService mHwInnerService;
    IHwWindowManagerServiceEx mHwWMSEx;
    boolean mInTouchMode;
    private int mInitDownX;
    private int mInitDownY;
    final InputManagerService mInputManager;
    final InputManagerCallback mInputManagerCallback;
    /* access modifiers changed from: private */
    public boolean mIsFoldDisableEventDispatching;
    private boolean mIsFoldRotationFreezed;
    boolean mIsPc;
    public boolean mIsPerfBoost;
    boolean mIsTouchDevice;
    /* access modifiers changed from: private */
    public final KeyguardDisableHandler mKeyguardDisableHandler;
    boolean mKeyguardGoingAway;
    boolean mKeyguardOrAodShowingOnDefaultDisplay;
    String mLastANRState;
    int mLastDisplayFreezeDuration;
    Object mLastFinishedFreezeSource;
    private AppWindowToken mLastFocusedApp;
    WindowState mLastWakeLockHoldingWindow;
    WindowState mLastWakeLockObscuringWindow;
    private final LatencyTracker mLatencyTracker;
    final boolean mLimitedAlphaCompositing;
    final boolean mLowRamTaskSnapshotsAndRecents;
    final int mMaxUiWidth;
    MousePositionTracker mMousePositionTracker;
    final List<IBinder> mNoAnimationNotifyOnTransitionFinished;
    protected boolean mNotifyFocusedWindow = false;
    final boolean mOnlyCore;
    protected PowerManager.WakeLock mPCHoldingScreenWakeLock;
    public IHwPCManager mPCManager;
    final ArrayList<WindowState> mPendingRemove;
    WindowState[] mPendingRemoveTmp;
    @VisibleForTesting
    boolean mPerDisplayFocusEnabled;
    final PackageManagerInternal mPmInternal;
    boolean mPointerLocationEnabled;
    @VisibleForTesting
    WindowManagerPolicy mPolicy;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    private final PriorityDump.PriorityDumper mPriorityDumper;
    final SparseArray<Configuration> mProcessConfigurations;
    /* access modifiers changed from: private */
    public RecentsAnimationController mRecentsAnimationController;
    boolean mResizing;
    final ArrayList<WindowState> mResizingWindows;
    RootWindowContainer mRoot;
    private boolean mRotatingSeamlessly;
    ArrayList<RotationWatcher> mRotationWatchers;
    protected boolean mRtgSchedSwitch;
    boolean mSafeMode;
    private final PowerManager.WakeLock mScreenFrozenLock;
    private int mSeamlessRotationCount;
    final ArraySet<Session> mSessions;
    SettingsObserver mSettingsObserver;
    public boolean mShouldResetTime;
    boolean mShowAlertWindowNotifications;
    boolean mShowingBootMessages;
    StrictModeFlash mStrictModeFlash;
    public float mSubFoldModeScale;
    boolean mSupportsFreeformWindowManagement;
    boolean mSupportsPictureInPicture;
    final SurfaceAnimationRunner mSurfaceAnimationRunner;
    SurfaceBuilderFactory mSurfaceBuilderFactory;
    SurfaceFactory mSurfaceFactory;
    boolean mSwitchingUser;
    private List<String> mSystemAppPkgs;
    boolean mSystemBooted;
    boolean mSystemGestureExcludedByPreQStickyImmersive;
    int mSystemGestureExclusionLimitDp;
    boolean mSystemReady;
    final TaskPositioningController mTaskPositioningController;
    final TaskSnapshotController mTaskSnapshotController;
    final Configuration mTempConfiguration;
    private WindowContentFrameStats mTempWindowRenderStats;
    final float[] mTmpFloats;
    final Rect mTmpRect;
    final Rect mTmpRect2;
    final Rect mTmpRect3;
    final RectF mTmpRectF;
    final Matrix mTmpTransform;
    private final SurfaceControl.Transaction mTransaction;
    TransactionFactory mTransactionFactory;
    int mTransactionSequence;
    /* access modifiers changed from: private */
    public float mTransitionAnimationScaleSetting;
    private ViewServer mViewServer;
    int mVr2dDisplayId;
    boolean mVrModeEnabled;
    private final IVrStateCallbacks mVrStateCallbacks;
    HwWMDAMonitorProxy mWMProxy;
    /* access modifiers changed from: private */
    public long mWaitAllWindowDrawStartTime;
    ArrayList<WindowState> mWaitingForDrawn;
    Runnable mWaitingForDrawnCallback;
    final WallpaperVisibilityListeners mWallpaperVisibilityListeners;
    /* access modifiers changed from: private */
    public WindowState mWallpaperWindow;
    Watermark mWatermark;
    /* access modifiers changed from: private */
    public float mWindowAnimationScaleSetting;
    final ArrayList<WindowChangeListener> mWindowChangeListeners;
    final WindowHashMap mWindowMap;
    final WindowSurfacePlacer mWindowPlacerLocked;
    final ArrayList<AppWindowToken> mWindowReplacementTimeouts;
    final WindowTracing mWindowTracing;
    boolean mWindowsChanged;
    int mWindowsFreezingScreen;

    interface AppFreezeListener {
        void onAppFreezeTimeout();
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface UpdateAnimationScaleMode {
    }

    public interface WindowChangeListener {
        void focusChanged();

        void windowsChanged();
    }

    /* access modifiers changed from: package-private */
    public int getDragLayerLocked() {
        return (this.mPolicy.getWindowLayerFromTypeLw(2016) * 10000) + 1000;
    }

    class RotationWatcher {
        final IBinder.DeathRecipient mDeathRecipient;
        final int mDisplayId;
        final IRotationWatcher mWatcher;

        RotationWatcher(IRotationWatcher watcher, IBinder.DeathRecipient deathRecipient, int displayId) {
            this.mWatcher = watcher;
            this.mDeathRecipient = deathRecipient;
            this.mDisplayId = displayId;
        }
    }

    /* access modifiers changed from: private */
    public final class SettingsObserver extends ContentObserver {
        private final Uri mAnimationDurationScaleUri = Settings.Global.getUriFor("animator_duration_scale");
        private final Uri mDisplayInversionEnabledUri = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
        private final Uri mImmersiveModeConfirmationsUri = Settings.Secure.getUriFor("immersive_mode_confirmations");
        private final Uri mPointerLocationUri = Settings.System.getUriFor("pointer_location");
        private final Uri mPolicyControlUri = Settings.Global.getUriFor("policy_control");
        private final Uri mTransitionAnimationScaleUri = Settings.Global.getUriFor("transition_animation_scale");
        private final Uri mWindowAnimationScaleUri = Settings.Global.getUriFor("window_animation_scale");

        public SettingsObserver() {
            super(new Handler());
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this, -1);
            resolver.registerContentObserver(this.mWindowAnimationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mTransitionAnimationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mAnimationDurationScaleUri, false, this, -1);
            resolver.registerContentObserver(this.mImmersiveModeConfirmationsUri, false, this, -1);
            resolver.registerContentObserver(this.mPolicyControlUri, false, this, -1);
            resolver.registerContentObserver(this.mPointerLocationUri, false, this, -1);
        }

        public void onChange(boolean selfChange, Uri uri) {
            int mode;
            if (uri != null) {
                if (this.mImmersiveModeConfirmationsUri.equals(uri) || this.mPolicyControlUri.equals(uri)) {
                    updateSystemUiSettings();
                } else if (this.mDisplayInversionEnabledUri.equals(uri)) {
                    WindowManagerService.this.updateCircularDisplayMaskIfNeeded();
                } else if (this.mPointerLocationUri.equals(uri)) {
                    updatePointerLocation();
                } else {
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

        /* access modifiers changed from: package-private */
        public void updateSystemUiSettings() {
            boolean changed;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (!ImmersiveModeConfirmation.loadSetting(WindowManagerService.this.mCurrentUserId, WindowManagerService.this.mContext)) {
                        if (!PolicyControl.reloadFromSetting(WindowManagerService.this.mContext)) {
                            changed = false;
                        }
                    }
                    changed = true;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (changed) {
                WindowManagerService.this.updateRotation(false, false);
            }
        }

        /* access modifiers changed from: package-private */
        public void updatePointerLocation() {
            boolean enablePointerLocation = false;
            if (Settings.System.getIntForUser(WindowManagerService.this.mContext.getContentResolver(), "pointer_location", 0, -2) != 0) {
                enablePointerLocation = true;
            }
            if (WindowManagerService.this.mPointerLocationEnabled != enablePointerLocation) {
                if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer()) {
                    WindowManagerService windowManagerService = WindowManagerService.this;
                    windowManagerService.mPointerLocationEnabled = enablePointerLocation;
                    synchronized (windowManagerService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (!HwPCUtils.enabledInPad()) {
                                WindowManagerService.this.mRoot.forAllDisplayPolicies(PooledLambda.obtainConsumer($$Lambda$1z_bkwouqOBIC89HKBNNqb1FoaY.INSTANCE, PooledLambda.__(), Boolean.valueOf(WindowManagerService.this.mPointerLocationEnabled)));
                            } else {
                                WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy().setPointerLocationEnabled(WindowManagerService.this.mPointerLocationEnabled);
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            }
        }
    }

    static void boostPriorityForLockedSection() {
        sThreadPriorityBooster.boost();
    }

    static void resetPriorityAfterLockedSection() {
        sThreadPriorityBooster.reset();
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void openSurfaceTransaction() {
        try {
            Trace.traceBegin(32, "openSurfaceTransaction");
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    SurfaceControl.openTransaction();
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Trace.traceEnd(32);
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void closeSurfaceTransaction(String where) {
        try {
            Trace.traceBegin(32, "closeSurfaceTransaction");
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    SurfaceControl.closeTransaction();
                    this.mWindowTracing.logState(where);
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
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

    public static WindowManagerService main(Context context, InputManagerService im, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm) {
        return main(context, im, showBootMsgs, onlyCore, policy, atm, $$Lambda$hBnABSAsqXWvQ0zKwHWE4BZ3Mc0.INSTANCE);
    }

    @VisibleForTesting
    public static WindowManagerService main(Context context, InputManagerService im, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm, TransactionFactory transactionFactory) {
        DisplayThread.getHandler().runWithScissors(new Runnable(context, im, showBootMsgs, onlyCore, policy, atm, transactionFactory) {
            /* class com.android.server.wm.$$Lambda$WindowManagerService$wGh8jzmWqrd_7ruovSXZoiIk1s0 */
            private final /* synthetic */ Context f$0;
            private final /* synthetic */ InputManagerService f$1;
            private final /* synthetic */ boolean f$2;
            private final /* synthetic */ boolean f$3;
            private final /* synthetic */ WindowManagerPolicy f$4;
            private final /* synthetic */ ActivityTaskManagerService f$5;
            private final /* synthetic */ TransactionFactory f$6;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
                this.f$6 = r7;
            }

            public final void run() {
                WindowManagerService.lambda$main$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
            }
        }, 0);
        return sInstance;
    }

    static /* synthetic */ void lambda$main$0(Context context, InputManagerService im, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm, TransactionFactory transactionFactory) {
        HwServiceFactory.IHwWindowManagerService iwms = HwServiceFactory.getHuaweiWindowManagerService();
        if (iwms != null) {
            sInstance = iwms.getInstance(context, im, showBootMsgs, onlyCore, policy, atm, transactionFactory);
        } else {
            sInstance = new WindowManagerService(context, im, showBootMsgs, onlyCore, policy, atm, transactionFactory);
        }
    }

    private void initPolicy() {
        UiThread.getHandler().runWithScissors(new Runnable() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass5 */

            public void run() {
                WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
                WindowManagerPolicy windowManagerPolicy = WindowManagerService.this.mPolicy;
                Context context = WindowManagerService.this.mContext;
                WindowManagerService windowManagerService = WindowManagerService.this;
                windowManagerPolicy.init(context, windowManagerService, windowManagerService);
            }
        }, 0);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wm.WindowManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
        new WindowManagerShellCommand(this).exec(this, in, out, err, args, callback, result);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v64, resolved type: android.app.AppOpsManager */
    /* JADX DEBUG: Multi-variable search result rejected for r1v65, resolved type: android.app.AppOpsManager */
    /* JADX WARN: Multi-variable type inference failed */
    protected WindowManagerService(Context context, InputManagerService inputManager, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm, TransactionFactory transactionFactory) {
        float f;
        if (HwFoldScreenState.isFoldScreenDevice()) {
            f = (((float) HwFoldScreenState.getScreenPhysicalRect(3).width()) * 1.0f) / ((float) HwFoldScreenState.getScreenPhysicalRect(2).width());
        } else {
            f = 1.0f;
        }
        this.mSubFoldModeScale = f;
        this.mVr2dDisplayId = -1;
        this.mVrModeEnabled = false;
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass1 */

            public void onVrStateChanged(boolean enabled) {
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowManagerService.this.mVrModeEnabled = enabled;
                        WindowManagerService.this.mRoot.forAllDisplayPolicies(PooledLambda.obtainConsumer($$Lambda$h9zRxk6xP2dliCTsIiNVg_lH9kA.INSTANCE, PooledLambda.__(), Boolean.valueOf(enabled)));
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (((action.hashCode() == 988075300 && action.equals("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED")) ? (char) 0 : 65535) == 0) {
                    WindowManagerService.this.mKeyguardDisableHandler.updateKeyguardEnabled(getSendingUserId());
                }
            }
        };
        this.mPriorityDumper = new PriorityDump.PriorityDumper() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass3 */

            public void dumpCritical(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
                if (asProto && WindowManagerService.this.mWindowTracing.isEnabled()) {
                    WindowManagerService.this.mWindowTracing.stopTrace(null, false);
                    BackgroundThread.getHandler().post(new Runnable() {
                        /* class com.android.server.wm.$$Lambda$WindowManagerService$3$FRNc42I1SE4lD0XFYgIp8RCUXng */

                        public final void run() {
                            WindowManagerService.AnonymousClass3.this.lambda$dumpCritical$0$WindowManagerService$3();
                        }
                    });
                }
                WindowManagerService.this.doDump(fd, pw, new String[]{"-a"}, asProto);
            }

            public /* synthetic */ void lambda$dumpCritical$0$WindowManagerService$3() {
                WindowManagerService.this.mWindowTracing.writeTraceToFile();
                WindowManagerService.this.mWindowTracing.startTrace(null);
            }

            public void dump(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
                WindowManagerService.this.doDump(fd, pw, args, asProto);
            }
        };
        this.mCurrentProfileIds = new int[0];
        boolean z = true;
        this.mShowAlertWindowNotifications = true;
        this.mSessions = new ArraySet<>();
        this.mWindowMap = new WindowHashMap();
        this.mWindowReplacementTimeouts = new ArrayList<>();
        this.mResizingWindows = new ArrayList<>();
        this.mPendingRemove = new ArrayList<>();
        this.mPendingRemoveTmp = new WindowState[20];
        this.mProcessConfigurations = new SparseArray<>();
        this.mDestroySurface = new ArrayList<>();
        this.mDestroyPreservedSurface = new ArrayList<>();
        this.mForceRemoves = new ArrayList<>();
        this.mWaitingForDrawn = new ArrayList<>();
        this.mHidingNonSystemOverlayWindows = new ArrayList<>();
        this.mWaitAllWindowDrawStartTime = 0;
        this.mTmpFloats = new float[9];
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpRect3 = new Rect();
        this.mTmpRectF = new RectF();
        this.mTmpTransform = new Matrix();
        this.mDisplayEnabled = false;
        this.mSystemBooted = false;
        this.mForceDisplayEnabled = false;
        this.mShowingBootMessages = false;
        this.mBootAnimationStopped = false;
        this.mSystemReady = false;
        this.mLastWakeLockHoldingWindow = null;
        this.mLastWakeLockObscuringWindow = null;
        this.mDockedStackCreateMode = 0;
        this.mRotationWatchers = new ArrayList<>();
        this.mWallpaperVisibilityListeners = new WallpaperVisibilityListeners();
        this.mDisplayFrozen = false;
        this.mDisplayFreezeTime = 0;
        this.mLastDisplayFreezeDuration = 0;
        this.mLastFinishedFreezeSource = null;
        this.mSwitchingUser = false;
        this.mWindowsFreezingScreen = 0;
        this.mClientFreezingScreen = false;
        this.mAppsFreezingScreen = 0;
        this.mH = new H();
        this.mAnimationHandler = new Handler(AnimationThread.getHandler().getLooper());
        this.mRtgSchedSwitch = false;
        this.mSeamlessRotationCount = 0;
        this.mRotatingSeamlessly = false;
        this.mWindowAnimationScaleSetting = 1.0f;
        this.mTransitionAnimationScaleSetting = 1.0f;
        this.mAnimatorDurationScaleSetting = 1.0f;
        this.mAnimationsDisabled = false;
        this.mPointerLocationEnabled = false;
        this.mAnimationTransferMap = new ArrayMap<>();
        this.mPCManager = null;
        this.mWindowChangeListeners = new ArrayList<>();
        this.mWindowsChanged = false;
        this.mTempConfiguration = new Configuration();
        this.mHighRefreshRateBlacklist = HighRefreshRateBlacklist.create();
        this.mShouldResetTime = false;
        this.mNoAnimationNotifyOnTransitionFinished = new ArrayList();
        this.mSurfaceBuilderFactory = $$Lambda$XZU3HlCFtHp_gydNmNMeRmQMCI.INSTANCE;
        this.mTransactionFactory = $$Lambda$hBnABSAsqXWvQ0zKwHWE4BZ3Mc0.INSTANCE;
        this.mSurfaceFactory = $$Lambda$6DEhn1zqxqV5_Ytb_NyzMW23Ano.INSTANCE;
        this.mActivityManagerAppTransitionNotifier = new WindowManagerInternal.AppTransitionListener() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass4 */

            @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
            public void onAppTransitionCancelledLocked(int transit) {
                WindowManagerService.this.mAtmInternal.notifyAppTransitionCancelled();
            }

            @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
            public void onAppTransitionFinishedLocked(IBinder token) {
                WindowManagerService.this.mAtmInternal.notifyAppTransitionFinished();
                AppWindowToken atoken = WindowManagerService.this.mRoot.getAppWindowToken(token);
                if (atoken != null) {
                    if (atoken.mLaunchTaskBehind) {
                        try {
                            WindowManagerService.this.mActivityTaskManager.notifyLaunchTaskBehindComplete(atoken.token);
                        } catch (RemoteException e) {
                        }
                        atoken.mLaunchTaskBehind = false;
                    } else {
                        atoken.updateReportedVisibilityLocked();
                        if (atoken.mEnteringAnimation) {
                            if (WindowManagerService.this.getRecentsAnimationController() == null || !WindowManagerService.this.getRecentsAnimationController().isTargetApp(atoken)) {
                                atoken.mEnteringAnimation = false;
                                try {
                                    WindowManagerService.this.mActivityTaskManager.notifyEnterAnimationComplete(atoken.token);
                                } catch (RemoteException e2) {
                                }
                            } else {
                                return;
                            }
                        }
                    }
                    Jlog.printActivitySwitchAnimEnd();
                }
            }
        };
        this.mAppFreezeListeners = new ArrayList<>();
        this.mInputManagerCallback = new InputManagerCallback(this);
        this.mMousePositionTracker = new MousePositionTracker();
        this.mHwWMSEx = null;
        this.mHwInnerService = new HwInnerWindowManagerService(this);
        this.mWMProxy = new HwWMDAMonitorProxy();
        this.mIsFoldRotationFreezed = false;
        this.mSystemAppPkgs = new ArrayList();
        this.mAddValue = -1;
        this.mHwWMSEx = HwServiceExFactory.getHwWindowManagerServiceEx(this, context);
        LockGuard.installLock(this, 5);
        this.mGlobalLock = atm.getGlobalLock();
        this.mAtmService = atm;
        this.mContext = context;
        this.mAllowBootMessages = showBootMsgs;
        this.mOnlyCore = onlyCore;
        this.mLimitedAlphaCompositing = context.getResources().getBoolean(17891508);
        this.mHasPermanentDpad = context.getResources().getBoolean(17891463);
        this.mInTouchMode = context.getResources().getBoolean(17891397);
        this.mDrawLockTimeoutMillis = (long) context.getResources().getInteger(17694803);
        this.mAllowAnimationsInLowPowerMode = context.getResources().getBoolean(17891341);
        this.mMaxUiWidth = context.getResources().getInteger(17694837);
        this.mDisableTransitionAnimation = context.getResources().getBoolean(17891408);
        this.mPerDisplayFocusEnabled = context.getResources().getBoolean(17891332);
        this.mLowRamTaskSnapshotsAndRecents = context.getResources().getBoolean(17891476);
        this.mInputManager = inputManager;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplayWindowSettings = new DisplayWindowSettings(this);
        this.mTransactionFactory = transactionFactory;
        this.mTransaction = this.mTransactionFactory.make();
        this.mPolicy = policy;
        this.mAnimator = new WindowAnimator(this);
        this.mRoot = new RootWindowContainer(this);
        this.mWindowPlacerLocked = new WindowSurfacePlacer(this);
        this.mTaskSnapshotController = new TaskSnapshotController(this);
        this.mWindowTracing = WindowTracing.createDefaultAndStartLooper(this, Choreographer.getInstance());
        LocalServices.addService(WindowManagerPolicy.class, this.mPolicy);
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mKeyguardDisableHandler = KeyguardDisableHandler.create(this.mContext, this.mPolicy, this.mH);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        PowerManagerInternal powerManagerInternal = this.mPowerManagerInternal;
        if (powerManagerInternal != null) {
            powerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                /* class com.android.server.wm.WindowManagerService.AnonymousClass6 */

                public int getServiceType() {
                    return 3;
                }

                public void onLowPowerModeChanged(PowerSaveState result) {
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            boolean enabled = result.batterySaverEnabled;
                            if (WindowManagerService.this.mAnimationsDisabled != enabled && !WindowManagerService.this.mAllowAnimationsInLowPowerMode) {
                                boolean unused = WindowManagerService.this.mAnimationsDisabled = enabled;
                                WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            });
            this.mAnimationsDisabled = this.mPowerManagerInternal.getLowPowerState(3).batterySaverEnabled;
        }
        this.mScreenFrozenLock = this.mPowerManager.newWakeLock(1, "SCREEN_FROZEN");
        this.mScreenFrozenLock.setReferenceCounted(false);
        this.mActivityManager = ActivityManager.getService();
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        AppOpsManager.OnOpChangedInternalListener opListener = new AppOpsManager.OnOpChangedInternalListener() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass7 */

            public void onOpChanged(int op, String packageName) {
                WindowManagerService.this.updateAppOpsState();
                WindowManagerService.this.mHwWMSEx.updateAppOpsStateReport(op, packageName);
            }
        };
        this.mAppOps.startWatchingMode(24, null, opListener);
        this.mAppOps.startWatchingMode(45, null, opListener);
        this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        IntentFilter suspendPackagesFilter = new IntentFilter();
        suspendPackagesFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendPackagesFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        context.registerReceiverAsUser(new BroadcastReceiver() {
            /* class com.android.server.wm.WindowManagerService.AnonymousClass8 */

            public void onReceive(Context context, Intent intent) {
                String[] affectedPackages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                WindowManagerService.this.updateHiddenWhileSuspendedState(new ArraySet(Arrays.asList(affectedPackages)), "android.intent.action.PACKAGES_SUSPENDED".equals(intent.getAction()));
            }
        }, UserHandle.ALL, suspendPackagesFilter, null, null);
        ContentResolver resolver = context.getContentResolver();
        this.mWindowAnimationScaleSetting = Settings.Global.getFloat(resolver, "window_animation_scale", this.mWindowAnimationScaleSetting);
        this.mTransitionAnimationScaleSetting = Settings.Global.getFloat(resolver, "transition_animation_scale", context.getResources().getFloat(17105048));
        setAnimatorDurationScale(Settings.Global.getFloat(resolver, "animator_duration_scale", this.mAnimatorDurationScaleSetting));
        this.mForceDesktopModeOnExternalDisplays = Settings.Global.getInt(resolver, "force_desktop_mode_on_external_displays", 0) == 0 ? false : z;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mLatencyTracker = LatencyTracker.getInstance(context);
        this.mSettingsObserver = new SettingsObserver();
        this.mHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870922, TAG);
        this.mHoldingScreenWakeLock.setReferenceCounted(false);
        this.mSurfaceAnimationRunner = new SurfaceAnimationRunner(this.mPowerManagerInternal);
        this.mPCHoldingScreenWakeLock = this.mPowerManager.newWakeLock(536870918, TAG);
        this.mPCHoldingScreenWakeLock.setReferenceCounted(false);
        this.mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(17891357);
        this.mTaskPositioningController = new TaskPositioningController(this, this.mInputManager, this.mActivityTaskManager, this.mH.getLooper());
        this.mDragDropController = new DragDropController(this, this.mH.getLooper());
        this.mSystemGestureExclusionLimitDp = Math.max(200, DeviceConfig.getInt("android:window_manager", "system_gesture_exclusion_limit_dp", 0));
        this.mSystemGestureExcludedByPreQStickyImmersive = DeviceConfig.getBoolean("android:window_manager", "system_gestures_excluded_by_pre_q_sticky_immersive", false);
        DeviceConfig.addOnPropertiesChangedListener("android:window_manager", new HandlerExecutor(this.mH), new DeviceConfig.OnPropertiesChangedListener() {
            /* class com.android.server.wm.$$Lambda$WindowManagerService$vZ2iP62NKu_V2Wh0abrxnOgoI */

            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                WindowManagerService.this.lambda$new$1$WindowManagerService(properties);
            }
        });
        LocalServices.addService(WindowManagerInternal.class, new LocalService());
    }

    public /* synthetic */ void lambda$new$1$WindowManagerService(DeviceConfig.Properties properties) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                int exclusionLimitDp = Math.max(200, properties.getInt("system_gesture_exclusion_limit_dp", 0));
                boolean excludedByPreQSticky = DeviceConfig.getBoolean("android:window_manager", "system_gestures_excluded_by_pre_q_sticky_immersive", false);
                if (!(this.mSystemGestureExcludedByPreQStickyImmersive == excludedByPreQSticky && this.mSystemGestureExclusionLimitDp == exclusionLimitDp)) {
                    this.mSystemGestureExclusionLimitDp = exclusionLimitDp;
                    this.mSystemGestureExcludedByPreQStickyImmersive = excludedByPreQSticky;
                    this.mRoot.forAllDisplays($$Lambda$JQG7CszycLV40zONwvdlvplb1TI.INSTANCE);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public void onInitReady() {
        initPolicy();
        Watchdog.getInstance().addMonitor(this);
        openSurfaceTransaction();
        try {
            createWatermarkInTransaction();
            closeSurfaceTransaction("createWatermarkInTransaction");
            showEmulatorDisplayOverlayIfNeeded();
        } catch (Throwable th) {
            closeSurfaceTransaction("createWatermarkInTransaction");
            throw th;
        }
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public InputManagerCallback getInputManagerCallback() {
        return this.mInputManagerCallback;
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
        if (windowType == 2000 || windowType == 2012 || windowType == 2019) {
            return true;
        }
        return false;
    }

    static boolean excludeWindowsFromTapOutTask(WindowState win) {
        WindowManager.LayoutParams attrs = win == null ? null : win.getAttrs();
        if (attrs == null) {
            return false;
        }
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(win.getDisplayId()) && "com.huawei.desktop.systemui".equals(attrs.packageName)) {
            return true;
        }
        if (attrs.type != 1000 || !"com.baidu.input_huawei".equals(attrs.packageName) || (HwPCUtils.isPcCastModeInServer() && !HwPCUtils.enabledInPad())) {
            return false;
        }
        return true;
    }

    public int addWindow(Session session, IWindow client, int seq, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, DisplayCutout.ParcelableWrapper outDisplayCutout, InputChannel outInputChannel, InsetsState outInsetsState) {
        Object obj;
        IBinder iBinder;
        int callingUid;
        int[] appOp;
        WindowToken token;
        char c;
        int rootType;
        boolean z;
        AppWindowToken atoken;
        int callingUid2;
        long origId;
        boolean imMayMove;
        boolean z2;
        int i;
        DisplayFrames displayFrames;
        boolean floatingStack;
        Rect taskBounds;
        boolean z3;
        char c2;
        int type = displayId;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_ADDWINDOW, new Object[]{attrs});
        int[] appOp2 = new int[1];
        int res = this.mPolicy.checkAddPermission(attrs, appOp2);
        if (res != 0) {
            return res;
        }
        boolean reportNewConfig = false;
        WindowState parentWindow = null;
        int callingUid3 = Binder.getCallingUid();
        int type2 = attrs.type;
        Object obj2 = this.mGlobalLock;
        synchronized (obj2) {
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayReady) {
                    long origId2 = Binder.clearCallingIdentity();
                    try {
                        DisplayContent displayContent = getDisplayContentOrCreate(type, attrs.token);
                        Binder.restoreCallingIdentity(origId2);
                        if (displayContent == null) {
                            try {
                                Slog.w(TAG, "Attempted to add window to a display that does not exist: " + type + ".  Aborting.");
                                resetPriorityAfterLockedSection();
                                return -9;
                            } catch (Throwable th) {
                                th = th;
                                obj = obj2;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else if (!displayContent.hasAccess(session.mUid)) {
                            Slog.w(TAG, "Attempted to add window to a display for which the application does not have access: " + type + ".  Aborting.");
                            resetPriorityAfterLockedSection();
                            return -9;
                        } else if (this.mWindowMap.containsKey(client.asBinder())) {
                            try {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Window ");
                                sb.append(client);
                                sb.append(" is already added");
                                Slog.w(TAG, sb.toString());
                                resetPriorityAfterLockedSection();
                                return -5;
                            } catch (Throwable th2) {
                                th = th2;
                                obj = obj2;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else {
                            if (type2 >= 1000 && type2 <= 1999) {
                                parentWindow = windowForClientLocked((Session) null, attrs.token, false);
                                if (parentWindow == null) {
                                    Slog.w(TAG, "Attempted to add window with token that is not a window: " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -2;
                                } else if (parentWindow.mAttrs.type >= 1000 && parentWindow.mAttrs.type <= 1999) {
                                    Slog.w(TAG, "Attempted to add window with token that is a sub-window: " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -2;
                                }
                            }
                            if (type2 == 2030) {
                                try {
                                    if (!displayContent.isPrivate()) {
                                        Slog.w(TAG, "Attempted to add private presentation window to a non-private display.  Aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -8;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    obj = obj2;
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                            boolean hasParent = parentWindow != null;
                            if (hasParent) {
                                iBinder = parentWindow.mAttrs.token;
                            } else {
                                try {
                                    iBinder = attrs.token;
                                } catch (Throwable th4) {
                                    th = th4;
                                    obj = obj2;
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                            WindowToken token2 = displayContent.getWindowToken(iBinder);
                            int rootType2 = hasParent ? parentWindow.mAttrs.type : type2;
                            boolean addToastWindowRequiresToken = false;
                            if (token2 != null) {
                                rootType = rootType2;
                                obj = obj2;
                                type = type2;
                                appOp = appOp2;
                                z = true;
                                if (rootType < 1 || rootType > 99) {
                                    if (rootType == 2011) {
                                        if (token2.windowType != 2011) {
                                            Slog.w(TAG, "Attempted to add input method window with bad token " + attrs.token + ".  Aborting.");
                                            resetPriorityAfterLockedSection();
                                            return -1;
                                        }
                                        token = token2;
                                        callingUid = callingUid3;
                                        c = 2005;
                                    } else if (rootType == 2031) {
                                        if (token2.windowType != 2031) {
                                            Slog.w(TAG, "Attempted to add voice interaction window with bad token " + attrs.token + ".  Aborting.");
                                            resetPriorityAfterLockedSection();
                                            return -1;
                                        }
                                        token = token2;
                                        callingUid = callingUid3;
                                        c = 2005;
                                    } else if (rootType == 2013) {
                                        if (token2.windowType != 2013) {
                                            Slog.w(TAG, "Attempted to add wallpaper window with bad token " + attrs.token + ".  Aborting.");
                                            resetPriorityAfterLockedSection();
                                            return -1;
                                        }
                                        token = token2;
                                        callingUid = callingUid3;
                                        c = 2005;
                                    } else if (rootType == 2023) {
                                        if (token2.windowType != 2023) {
                                            Slog.w(TAG, "Attempted to add Dream window with bad token " + attrs.token + ".  Aborting.");
                                            resetPriorityAfterLockedSection();
                                            return -1;
                                        }
                                        token = token2;
                                        callingUid = callingUid3;
                                        c = 2005;
                                    } else if (rootType == 2032) {
                                        if (token2.windowType != 2032) {
                                            Slog.w(TAG, "Attempted to add Accessibility overlay window with bad token " + attrs.token + ".  Aborting.");
                                            resetPriorityAfterLockedSection();
                                            return -1;
                                        }
                                        token = token2;
                                        callingUid = callingUid3;
                                        c = 2005;
                                    } else if (type == 2005) {
                                        try {
                                            try {
                                                addToastWindowRequiresToken = doesAddToastWindowRequireToken(attrs.packageName, callingUid3, parentWindow);
                                                if (addToastWindowRequiresToken) {
                                                    c2 = 2005;
                                                    if (token2.windowType != 2005) {
                                                        Slog.w(TAG, "Attempted to add a toast window with bad token " + attrs.token + ".  Aborting.");
                                                        resetPriorityAfterLockedSection();
                                                        return -1;
                                                    }
                                                } else {
                                                    c2 = 2005;
                                                }
                                                c = c2;
                                                callingUid = callingUid3;
                                                token = token2;
                                                atoken = null;
                                            } catch (Throwable th5) {
                                                th = th5;
                                                resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } catch (Throwable th6) {
                                            th = th6;
                                            resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    } else if (type != 2035) {
                                        try {
                                            if (token2.asAppWindowToken() != null) {
                                                try {
                                                    Slog.w(TAG, "Non-null appWindowToken for system window of rootType=" + rootType);
                                                    attrs.token = null;
                                                    c = 2005;
                                                    callingUid = callingUid3;
                                                } catch (Throwable th7) {
                                                    th = th7;
                                                    resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                                try {
                                                    token = new WindowToken(this, client.asBinder(), type, false, displayContent, session.mCanAddInternalSystemWindow);
                                                    atoken = null;
                                                } catch (Throwable th8) {
                                                    th = th8;
                                                    resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            } else {
                                                c = 2005;
                                                callingUid = callingUid3;
                                                token = token2;
                                            }
                                        } catch (Throwable th9) {
                                            th = th9;
                                            resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    } else if (token2.windowType != 2035) {
                                        Slog.w(TAG, "Attempted to add QS dialog window with bad token " + attrs.token + ".  Aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -1;
                                    } else {
                                        c = 2005;
                                        callingUid = callingUid3;
                                        token = token2;
                                    }
                                    atoken = null;
                                } else {
                                    AppWindowToken atoken2 = token2.asAppWindowToken();
                                    if (atoken2 == null) {
                                        Slog.w(TAG, "Attempted to add window with non-application token " + token2 + ".  Aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -3;
                                    } else if (atoken2.removed) {
                                        Slog.w(TAG, "Attempted to add window with exiting application token " + token2 + ".  Aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -4;
                                    } else if (type != 3 || atoken2.startingWindow == null) {
                                        token = token2;
                                        callingUid = callingUid3;
                                        c = 2005;
                                        atoken = atoken2;
                                    } else {
                                        Slog.w(TAG, "Attempted to add starting window to token with already existing starting window");
                                        resetPriorityAfterLockedSection();
                                        return -5;
                                    }
                                }
                            } else if (rootType2 < 1 || rootType2 > 99) {
                                obj = token2;
                                if (rootType2 == 2011) {
                                    Slog.w(TAG, "Attempted to add input method window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                } else if (rootType2 == 2031) {
                                    Slog.w(TAG, "Attempted to add voice interaction window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                } else if (rootType2 == 2013) {
                                    Slog.w(TAG, "Attempted to add wallpaper window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                } else if (rootType2 == 2023) {
                                    Slog.w(TAG, "Attempted to add Dream window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                } else if (rootType2 == 2035) {
                                    Slog.w(TAG, "Attempted to add QS dialog window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                } else if (rootType2 == 2032) {
                                    Slog.w(TAG, "Attempted to add Accessibility overlay window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                } else if (type2 != 2005 || !doesAddToastWindowRequireToken(attrs.packageName, callingUid3, parentWindow)) {
                                    try {
                                        rootType = rootType2;
                                        obj = obj2;
                                        type = type2;
                                        appOp = appOp2;
                                        token = new WindowToken(this, attrs.token != null ? attrs.token : client.asBinder(), type2, false, displayContent, session.mCanAddInternalSystemWindow, (attrs.privateFlags & 1048576) != 0);
                                        atoken = null;
                                        callingUid = callingUid3;
                                        z = true;
                                        c = 2005;
                                    } catch (Throwable th10) {
                                        th = th10;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                } else {
                                    Slog.w(TAG, "Attempted to add a toast window with unknown token " + attrs.token + ".  Aborting.");
                                    resetPriorityAfterLockedSection();
                                    return -1;
                                }
                            } else {
                                Slog.w(TAG, "Attempted to add application window with unknown token " + attrs.token + ".  Aborting.");
                                resetPriorityAfterLockedSection();
                                return -1;
                            }
                            try {
                            } catch (Throwable th11) {
                                th = th11;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                            try {
                                WindowState win = new WindowState(this, session, client, token, parentWindow, appOp[0], seq, attrs, viewVisibility, session.mUid, session.mCanAddInternalSystemWindow);
                                if (win.mDeathRecipient == null) {
                                    try {
                                        Slog.w(TAG, "Adding window client " + client.asBinder() + " that is dead, aborting.");
                                        resetPriorityAfterLockedSection();
                                        return -4;
                                    } catch (Throwable th12) {
                                        th = th12;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                } else if (win.getDisplayContent() == null) {
                                    Slog.w(TAG, "Adding window to Display that has been removed.");
                                    resetPriorityAfterLockedSection();
                                    return -9;
                                } else {
                                    DisplayPolicy displayPolicy = displayContent.getDisplayPolicy();
                                    displayPolicy.adjustWindowParamsLw(win, win.mAttrs, Binder.getCallingPid(), Binder.getCallingUid());
                                    win.setShowToOwnerOnlyLocked(this.mPolicy.checkShowToOwnerOnly(attrs));
                                    int res2 = displayPolicy.prepareAddWindowLw(win, attrs);
                                    if (res2 != 0) {
                                        resetPriorityAfterLockedSection();
                                        return res2;
                                    }
                                    if (outInputChannel != null && (attrs.inputFeatures & 2) == 0) {
                                        win.openInputChannel(outInputChannel);
                                    }
                                    if (type == 2005) {
                                        callingUid2 = callingUid;
                                        try {
                                            if (!displayContent.canAddToastWindowForUid(callingUid2)) {
                                                Slog.w(TAG, "Adding more than one toast window for UID at a time.");
                                                resetPriorityAfterLockedSection();
                                                return -5;
                                            } else if (addToastWindowRequiresToken || (attrs.flags & 8) == 0 || displayContent.mCurrentFocus == null || displayContent.mCurrentFocus.mOwnerUid != callingUid2) {
                                                this.mH.sendMessageDelayed(this.mH.obtainMessage(52, win), win.mAttrs.hideTimeoutMilliseconds);
                                            }
                                        } catch (Throwable th13) {
                                            th = th13;
                                            resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    } else {
                                        callingUid2 = callingUid;
                                    }
                                    int res3 = 0;
                                    try {
                                        if (displayContent.mCurrentFocus == null) {
                                            displayContent.mWinAddedSinceNullFocus.add(win);
                                        }
                                        if (excludeWindowTypeFromTapOutTask(type) || excludeWindowsFromTapOutTask(win)) {
                                            displayContent.mTapExcludedWindows.add(win);
                                        }
                                        origId = Binder.clearCallingIdentity();
                                        win.attach();
                                        this.mWindowMap.put(client.asBinder(), win);
                                        win.initAppOpsState();
                                        win.setHiddenWhileSuspended(this.mPmInternal.isPackageSuspended(win.getOwningPackage(), UserHandle.getUserId(win.getOwningUid())));
                                        win.setForceHideNonSystemOverlayWindowIfNeeded(!this.mHidingNonSystemOverlayWindows.isEmpty());
                                        AppWindowToken aToken = token.asAppWindowToken();
                                        if (type != 3 || aToken == null) {
                                            this.mHwWMSEx.updateHwStartWindowRecord(session.mUid);
                                        } else {
                                            aToken.startingWindow = win;
                                            Flog.i(301, "Start add starting window for:" + aToken + " startingWindow=" + win);
                                        }
                                        win.mToken.addWindow(win);
                                        if (type == 2011) {
                                            displayContent.setInputMethodWindowLocked(win);
                                            imMayMove = false;
                                            z2 = true;
                                        } else if (type == 2012) {
                                            z2 = true;
                                            displayContent.computeImeTarget(true);
                                            imMayMove = false;
                                        } else {
                                            z2 = true;
                                            if (type == 2013) {
                                                displayContent.mWallpaperController.clearLastWallpaperTimeoutTime();
                                                displayContent.pendingLayoutChanges |= 4;
                                            } else if ((attrs.flags & 1048576) != 0) {
                                                displayContent.pendingLayoutChanges |= 4;
                                            } else if (displayContent.mWallpaperController.isBelowWallpaperTarget(win)) {
                                                displayContent.pendingLayoutChanges |= 4;
                                            }
                                            imMayMove = true;
                                        }
                                        win.applyAdjustForImeIfNeeded();
                                        if (type == 2034) {
                                            i = displayId;
                                            try {
                                                this.mRoot.getDisplayContent(i).getDockedDividerController().setWindow(win);
                                            } catch (Throwable th14) {
                                                th = th14;
                                            }
                                        } else {
                                            i = displayId;
                                        }
                                    } catch (Throwable th15) {
                                        th = th15;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                    try {
                                        WindowStateAnimator winAnimator = win.mWinAnimator;
                                        winAnimator.mEnterAnimationPending = z2;
                                        winAnimator.mEnteringAnimation = z2;
                                        if (atoken != null && atoken.isVisible() && !prepareWindowReplacementTransition(atoken)) {
                                            prepareNoneTransitionForRelaunching(atoken);
                                        }
                                        displayFrames = displayContent.mDisplayFrames;
                                        DisplayInfo displayInfo = displayContent.getDisplayInfo();
                                        displayFrames.onDisplayInfoUpdated(displayInfo, displayContent.calculateDisplayCutoutForRotation(displayInfo.rotation));
                                        if (atoken == null || atoken.getTask() == null) {
                                            taskBounds = null;
                                            floatingStack = false;
                                        } else {
                                            taskBounds = this.mTmpRect;
                                            atoken.getTask().getBounds(this.mTmpRect);
                                            floatingStack = atoken.getTask().isFloating();
                                        }
                                    } catch (Throwable th16) {
                                        th = th16;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                    try {
                                        if (displayPolicy.getLayoutHintLw(win.mAttrs, taskBounds, displayFrames, floatingStack, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout)) {
                                            res3 = 0 | 4;
                                        }
                                    } catch (Throwable th17) {
                                        th = th17;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                    try {
                                        outInsetsState.set(displayContent.getInsetsStateController().getInsetsForDispatch(win));
                                        if (this.mInTouchMode) {
                                            res3 |= 1;
                                        }
                                        if (win.mAppToken == null || !win.mAppToken.isClientHidden()) {
                                            res3 |= 2;
                                        }
                                        displayContent.getInputMonitor().setUpdateInputWindowsNeededLw();
                                        boolean focusChanged = false;
                                        if (win.canReceiveKeys()) {
                                            z3 = false;
                                            focusChanged = updateFocusedWindowLocked(1, false);
                                            if (focusChanged) {
                                                imMayMove = false;
                                            }
                                        } else {
                                            z3 = false;
                                        }
                                        if (imMayMove && !this.mHwWMSEx.isSkipComputeImeTargetForHwMultiDisplay(getInputMethodWindowLw(), displayContent)) {
                                            displayContent.computeImeTarget(true);
                                        }
                                        win.getParent().assignChildLayers();
                                        if (focusChanged) {
                                            displayContent.getInputMonitor().setInputFocusLw(displayContent.mCurrentFocus, z3);
                                        }
                                        displayContent.getInputMonitor().updateInputWindowsLw(z3);
                                        Slog.i(TAG, "addWindow: " + win);
                                        if (win.isVisibleOrAdding() && displayContent.updateOrientationFromAppTokens()) {
                                            reportNewConfig = true;
                                        }
                                        resetPriorityAfterLockedSection();
                                        if (reportNewConfig) {
                                            sendNewConfiguration(displayId);
                                        }
                                        Binder.restoreCallingIdentity(origId);
                                        return res3;
                                    } catch (Throwable th18) {
                                        th = th18;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                            } catch (Throwable th19) {
                                th = th19;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                    } catch (Throwable th20) {
                        th = th20;
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                } else {
                    throw new IllegalStateException("Display has not been initialialized");
                }
            } catch (Throwable th21) {
                th = th21;
                obj = obj2;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private DisplayContent getDisplayContentOrCreate(int displayId, IBinder token) {
        Display display;
        WindowToken wToken;
        if (token != null && (wToken = this.mRoot.getWindowToken(token)) != null) {
            return wToken.getDisplayContent();
        }
        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
        if (displayContent != null || (display = this.mDisplayManager.getDisplay(displayId)) == null) {
            return displayContent;
        }
        return this.mRoot.createDisplayContent(display, null);
    }

    private boolean doesAddToastWindowRequireToken(String packageName, int callingUid, WindowState attachedWindow) {
        if (attachedWindow != null) {
            return attachedWindow.mAppToken != null && attachedWindow.mAppToken.mTargetSdk >= 26;
        }
        try {
            ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(callingUid));
            if (appInfo.uid == callingUid) {
                return appInfo.targetSdkVersion >= 26;
            }
            throw new SecurityException("Package " + packageName + " not in UID " + callingUid);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private boolean prepareWindowReplacementTransition(AppWindowToken atoken) {
        atoken.clearAllDrawn();
        WindowState replacedWindow = atoken.getReplacingWindow();
        if (replacedWindow == null) {
            return false;
        }
        Rect frame = replacedWindow.getVisibleFrameLw();
        DisplayContent dc = atoken.getDisplayContent();
        dc.mOpeningApps.add(atoken);
        dc.prepareAppTransition(18, true, 0, false);
        dc.mAppTransition.overridePendingAppTransitionClipReveal(frame.left, frame.top, frame.width(), frame.height());
        dc.executeAppTransition();
        return true;
    }

    private void prepareNoneTransitionForRelaunching(AppWindowToken atoken) {
        DisplayContent dc = atoken.getDisplayContent();
        if (this.mDisplayFrozen && !dc.mOpeningApps.contains(atoken) && atoken.isRelaunching()) {
            dc.mOpeningApps.add(atoken);
            dc.prepareAppTransition(0, false, 0, false);
            dc.executeAppTransition();
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isSecureLocked(WindowState w) {
        if (!this.mHwWMSEx.isSecureForPCDisplay(w)) {
            return false;
        }
        if ((w.mAttrs.flags & 8192) == 0 && !DevicePolicyCache.getInstance().getScreenCaptureDisabled(UserHandle.getUserId(w.mOwnerUid))) {
            return false;
        }
        return true;
    }

    public void refreshScreenCaptureDisabled(int userId) {
        if (Binder.getCallingUid() == 1000) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.setSecureSurfaceState(userId, DevicePolicyCache.getInstance().getScreenCaptureDisabled(userId));
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Only system can call refreshScreenCaptureDisabled.");
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public void updateAppOpsState() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateAppOpsState();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void removeWindow(Session session, IWindow client) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win != null) {
                    win.removeIfPossible();
                    resetPriorityAfterLockedSection();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void postWindowRemoveCleanupLocked(WindowState win) {
        Slog.i(TAG, "postWindowRemoveCleanupLocked: " + win);
        this.mWindowMap.remove(win.mClient.asBinder());
        markForSeamlessRotation(win, false);
        win.resetAppOpsState();
        this.mHwWMSEx.removeWindowReport(win);
        DisplayContent dc = win.getDisplayContent();
        if (dc.mCurrentFocus == null) {
            dc.mWinRemovedSinceNullFocus.add(win);
        }
        this.mPendingRemove.remove(win);
        this.mResizingWindows.remove(win);
        updateNonSystemOverlayWindowsVisibilityIfNeeded(win, false);
        this.mWindowsChanged = true;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent.mInputMethodWindow == win) {
            displayContent.setInputMethodWindowLocked(null);
        }
        WindowToken token = win.mToken;
        AppWindowToken atoken = win.mAppToken;
        Slog.i(TAG, "Removing " + win + " from " + token);
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
        if (win.mAttrs.type == 2013) {
            dc.mWallpaperController.clearLastWallpaperTimeoutTime();
            dc.pendingLayoutChanges |= 4;
        } else if ((win.mAttrs.flags & 1048576) != 0) {
            dc.pendingLayoutChanges |= 4;
        }
        if (!this.mWindowPlacerLocked.isInLayout()) {
            dc.assignWindowLayers(true);
            this.mWindowPlacerLocked.performSurfacePlacement();
            if (win.mAppToken != null) {
                win.mAppToken.updateReportedVisibilityLocked();
            }
        }
        dc.getInputMonitor().updateInputWindowsLw(true);
    }

    /* access modifiers changed from: private */
    public void updateHiddenWhileSuspendedState(ArraySet<String> packages, boolean suspended) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateHiddenWhileSuspendedState(packages, suspended);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    static void logSurface(WindowState w, String msg, boolean withStackTrace) {
        String str = "  SURFACE " + msg + ": " + w;
        if (withStackTrace) {
            logWithStack(TAG, str);
        } else {
            Slog.i(TAG, str);
        }
    }

    static void logSurface(SurfaceControl s, String title, String msg) {
        Slog.i(TAG, "  SURFACE " + s + ": " + msg + " / " + title);
    }

    static void logWithStack(String tag, String s) {
        Slog.i(tag, s, (Throwable) null);
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void setTransparentRegionWindow(Session session, IWindow client, Region region) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    WindowState w = windowForClientLocked(session, client, false);
                    if (w != null && w.mHasSurface) {
                        w.mWinAnimator.setTransparentRegionHintLocked(region);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void setInsetsWindow(Session session, IWindow client, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    WindowState w = windowForClientLocked(session, client, false);
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.d(TAG, "setInsetsWindow " + w + ", contentInsets=" + w.mGivenContentInsets + " -> " + contentInsets + ", visibleInsets=" + w.mGivenVisibleInsets + " -> " + visibleInsets + ", touchableRegion=" + w.mGivenTouchableRegion + " -> " + touchableRegion + ", touchableInsets " + w.mTouchableInsets + " -> " + touchableInsets);
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
                        if (this.mAccessibilityController != null && (w.getDisplayContent().getDisplayId() == 0 || w.getDisplayContent().getParentWindow() != null)) {
                            this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                        }
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void getWindowDisplayFrame(Session session, IWindow client, Rect outDisplayFrame) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    outDisplayFrame.setEmpty();
                    return;
                }
                outDisplayFrame.set(win.getDisplayFrameLw());
                if (win.inSizeCompatMode()) {
                    outDisplayFrame.scale(win.mInvGlobalScale);
                }
                resetPriorityAfterLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        WindowState window;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!(this.mAccessibilityController == null || (window = (WindowState) this.mWindowMap.get(token)) == null || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(window.getDisplayId()) && !HwPCUtils.enabledInPad()))) {
                    this.mAccessibilityController.onRectangleOnScreenRequestedLocked(window.getDisplayId(), rectangle);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public IWindowId getWindowId(IBinder token) {
        WindowState.WindowId windowId;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState window = (WindowState) this.mWindowMap.get(token);
                windowId = window != null ? window.mWindowId : null;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return windowId;
    }

    public void pokeDrawLock(Session session, IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState window = windowForClientLocked(session, token, false);
                if (window != null) {
                    window.pokeDrawLockLw(this.mDrawLockTimeoutMillis);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private boolean hasStatusBarPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.STATUS_BAR", pid, uid) == 0;
    }

    /* JADX INFO: Multiple debug info for r11v6 'winAnimator'  com.android.server.wm.WindowStateAnimator: [D('uid' int), D('winAnimator' com.android.server.wm.WindowStateAnimator)] */
    /* JADX WARNING: Code restructure failed: missing block: B:315:0x064f, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:316:0x0652, code lost:
        if (r0 == false) goto L_0x0664;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:317:0x0654, code lost:
        android.os.Trace.traceBegin(32, "relayoutWindow: sendNewConfiguration");
        sendNewConfiguration(r0);
        android.os.Trace.traceEnd(32);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:319:0x0666, code lost:
        android.os.Binder.restoreCallingIdentity(r12);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:320:0x0669, code lost:
        return r9;
     */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x029f A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02a0 A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x02b8 A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x02bb A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x02db A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x030e A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x0319 A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x031b A[Catch:{ all -> 0x0179, all -> 0x0688 }] */
    /* JADX WARNING: Removed duplicated region for block: B:205:0x0394  */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x0439  */
    /* JADX WARNING: Removed duplicated region for block: B:256:0x04c4  */
    /* JADX WARNING: Removed duplicated region for block: B:257:0x04c6  */
    /* JADX WARNING: Removed duplicated region for block: B:259:0x04c9  */
    /* JADX WARNING: Removed duplicated region for block: B:264:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:266:0x04e6  */
    /* JADX WARNING: Removed duplicated region for block: B:269:0x04f0  */
    /* JADX WARNING: Removed duplicated region for block: B:288:0x057a  */
    /* JADX WARNING: Removed duplicated region for block: B:291:0x0583  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x0591  */
    /* JADX WARNING: Removed duplicated region for block: B:297:0x059a  */
    /* JADX WARNING: Removed duplicated region for block: B:299:0x059f  */
    /* JADX WARNING: Removed duplicated region for block: B:300:0x05a5  */
    /* JADX WARNING: Removed duplicated region for block: B:305:0x05f1 A[Catch:{ all -> 0x06a2 }] */
    /* JADX WARNING: Removed duplicated region for block: B:308:0x0613 A[Catch:{ all -> 0x06a2 }] */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x0616 A[Catch:{ all -> 0x06a2 }] */
    /* JADX WARNING: Removed duplicated region for block: B:312:0x0641 A[Catch:{ all -> 0x06a2 }] */
    /* JADX WARNING: Removed duplicated region for block: B:313:0x064c A[Catch:{ all -> 0x06a2 }] */
    public int relayoutWindow(Session session, IWindow client, int seq, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, long frameNumber, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame, DisplayCutout.ParcelableWrapper outCutout, MergedConfiguration mergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState) {
        DisplayPolicy displayPolicy;
        WindowStateAnimator winAnimator;
        int attrChanges;
        boolean imMayMove;
        boolean focusMayChange;
        boolean wallpaperMayMove;
        boolean becameVisible;
        boolean shouldRelayout;
        boolean focusMayChange2;
        DisplayContent displayContent;
        int result;
        int oldVisibility;
        boolean imMayMove2;
        boolean toBeDisplayed;
        DisplayPolicy displayPolicy2;
        boolean becameVisible2;
        MergedConfiguration mergedConfiguration2;
        long j;
        int result2 = 0;
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    try {
                        resetPriorityAfterLockedSection();
                        return 0;
                    } catch (Throwable th) {
                        th = th;
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                } else {
                    int displayId = win.getDisplayId();
                    DisplayContent displayContent2 = win.getDisplayContent();
                    DisplayPolicy displayPolicy3 = displayContent2.getDisplayPolicy();
                    this.mHwWMSEx.updateWindowReport(win, requestedWidth, requestedHeight);
                    WindowStateAnimator winAnimator2 = win.mWinAnimator;
                    if (viewVisibility != 8) {
                        win.setRequestedSize(requestedWidth, requestedHeight);
                    }
                    try {
                        win.setFrameNumber(frameNumber);
                        DisplayContent dc = win.getDisplayContent();
                        if (!dc.mWaitingForConfig) {
                            try {
                                win.finishSeamlessRotation(false);
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        int flagChanges = 0;
                        if (attrs != null) {
                            displayPolicy = displayPolicy3;
                            try {
                                displayPolicy.adjustWindowParamsLw(win, attrs, pid, uid);
                                if (seq == win.mSeq) {
                                    int systemUiVisibility = attrs.systemUiVisibility | attrs.subtreeSystemUiVisibility;
                                    if ((67043328 & systemUiVisibility) != 0 && !hasStatusBarPermission(pid, uid)) {
                                        systemUiVisibility &= -67043329;
                                    }
                                    win.mSystemUiVisibility = systemUiVisibility;
                                }
                                if (win.mAttrs.type == attrs.type) {
                                    if ((attrs.privateFlags & 8192) != 0) {
                                        attrs.x = win.mAttrs.x;
                                        attrs.y = win.mAttrs.y;
                                        attrs.width = win.mAttrs.width;
                                        attrs.height = win.mAttrs.height;
                                    }
                                    this.mHwWMSEx.updateStatusBarInMagicWindow(win.getWindowingMode(), attrs);
                                    if (IS_TABLET && IS_NOTCH_PROP) {
                                        this.mHwWMSEx.setNotchFlags(win, attrs, displayPolicy, displayPolicy.getLastSystemUiFlags());
                                    }
                                    WindowManager.LayoutParams layoutParams = win.mAttrs;
                                    try {
                                        int i = attrs.flags ^ layoutParams.flags;
                                        layoutParams.flags = i;
                                        flagChanges = i;
                                        int attrChanges2 = win.mAttrs.copyFrom(attrs);
                                        if ((attrChanges2 & 16385) != 0) {
                                            try {
                                                win.mLayoutNeeded = true;
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        }
                                        if (!(win.mAppToken == null || ((flagChanges & 524288) == 0 && (4194304 & flagChanges) == 0))) {
                                            win.mAppToken.checkKeyguardFlagsChanged();
                                        }
                                        if (!((33554432 & attrChanges2) == 0 || this.mAccessibilityController == null || (win.getDisplayId() != 0 && win.getDisplayContent().getParentWindow() == null && (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.enabledInPad())))) {
                                            this.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                                        }
                                        if ((flagChanges & 524288) != 0) {
                                            updateNonSystemOverlayWindowsVisibilityIfNeeded(win, win.mWinAnimator.getShown());
                                        }
                                        if ((131072 & attrChanges2) != 0) {
                                            winAnimator = winAnimator2;
                                            winAnimator.setColorSpaceAgnosticLocked((win.mAttrs.privateFlags & 16777216) != 0);
                                        } else {
                                            winAnimator = winAnimator2;
                                        }
                                        attrChanges = attrChanges2;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                } else {
                                    throw new IllegalArgumentException("Window type can not be changed after the window is added.");
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else {
                            displayPolicy = displayPolicy3;
                            winAnimator = winAnimator2;
                            attrChanges = 0;
                        }
                        if (win.toString().contains("HwFullScreenWindow")) {
                            displayPolicy.setFullScreenWinVisibile(viewVisibility == 0);
                            displayPolicy.setFullScreenWindow(win);
                        }
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            Slog.v(TAG, "Relayout " + win + ": viewVisibility=" + viewVisibility + " req=" + requestedWidth + "x" + requestedHeight + " " + win.mAttrs);
                        }
                        winAnimator.mSurfaceDestroyDeferred = (flags & 2) != 0;
                        if ((attrChanges & 128) != 0) {
                            winAnimator.mAlpha = attrs.alpha;
                        }
                        win.setWindowScale(win.mRequestedWidth, win.mRequestedHeight);
                        if (this.mHwWMSEx != null) {
                            this.mHwWMSEx.clearAppWindowIconInfo(win, viewVisibility);
                        }
                        if (!(win.mAttrs.surfaceInsets.left == 0 && win.mAttrs.surfaceInsets.top == 0 && win.mAttrs.surfaceInsets.right == 0 && win.mAttrs.surfaceInsets.bottom == 0)) {
                            winAnimator.setOpaqueLocked(false);
                        }
                        int oldVisibility2 = win.mViewVisibility;
                        Flog.i(307, "start Relayout " + win + " oldVis=" + oldVisibility2 + " newVis=" + viewVisibility + " width=" + requestedWidth + " height=" + requestedHeight);
                        boolean becameVisible3 = (oldVisibility2 == 4 || oldVisibility2 == 8) && viewVisibility == 0;
                        if ((flagChanges & 131080) == 0) {
                            if (!becameVisible3) {
                                imMayMove = false;
                                if (win.mViewVisibility == viewVisibility && (flagChanges & 8) == 0) {
                                    if (!win.mRelayoutCalled) {
                                        focusMayChange = false;
                                        wallpaperMayMove = (win.mViewVisibility == viewVisibility && (win.mAttrs.flags & 1048576) != 0) | ((flagChanges & 1048576) == 0);
                                        if (!((flagChanges & 8192) == 0 || winAnimator.mSurfaceController == null)) {
                                            winAnimator.mSurfaceController.setSecure(isSecureLocked(win));
                                        }
                                        win.mRelayoutCalled = true;
                                        win.mInRelayout = true;
                                        win.mViewVisibility = viewVisibility;
                                        if (!WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                                            RuntimeException stack = new RuntimeException();
                                            stack.fillInStackTrace();
                                            StringBuilder sb = new StringBuilder();
                                            becameVisible = becameVisible3;
                                            sb.append("Relayout ");
                                            sb.append(win);
                                            sb.append(": oldVis=");
                                            sb.append(oldVisibility2);
                                            sb.append(" newVis=");
                                            sb.append(viewVisibility);
                                            Slog.i(TAG, sb.toString(), stack);
                                        } else {
                                            becameVisible = becameVisible3;
                                        }
                                        win.setDisplayLayoutNeeded();
                                        win.mGivenInsetsPending = (flags & 1) == 0;
                                        shouldRelayout = viewVisibility != 0 && (win.mAppToken == null || win.mAttrs.type == 3 || !win.mAppToken.isClientHidden());
                                        if (!shouldRelayout || !winAnimator.hasSurface() || win.mAnimatingExit) {
                                            focusMayChange2 = focusMayChange;
                                        } else {
                                            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                Slog.i(TAG, "Relayout invis " + win + ": mAnimatingExit=" + win.mAnimatingExit);
                                            }
                                            result2 = 0 | 4;
                                            if (!win.mWillReplaceWindow) {
                                                focusMayChange2 = tryStartExitingAnimation(win, winAnimator, focusMayChange);
                                            } else {
                                                focusMayChange2 = focusMayChange;
                                            }
                                        }
                                        if (shouldRelayout && win.mAttrs.type == 2011 && displayPolicy.isLeftRightSplitStackVisible()) {
                                            displayPolicy.setFocusChangeIMEFrozenTag(false);
                                        }
                                        this.mWindowPlacerLocked.performSurfacePlacement(true);
                                        if (!shouldRelayout) {
                                            Trace.traceBegin(32, "relayoutWindow: viewVisibility_1");
                                            try {
                                                result = createSurfaceControl(outSurfaceControl, win.relayoutVisibleWindow(result2, attrChanges), win, winAnimator);
                                                if ((result & 2) != 0) {
                                                    focusMayChange2 = true;
                                                }
                                                if (win.mAttrs.type == 2011) {
                                                    displayContent = displayContent2;
                                                    if (displayContent.mInputMethodWindow == null || displayContent.mInputMethodWindow != win) {
                                                        displayContent.setInputMethodWindowLocked(win);
                                                        imMayMove = true;
                                                    }
                                                } else {
                                                    displayContent = displayContent2;
                                                }
                                                if (win.mAttrs.type == 2012 && displayContent.mInputMethodWindow == null && (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer())) {
                                                    Slog.d(TAG, "relayoutwindow TYPE_INPUT_METHOD_DIALOG , do setInputMethodWindowLocked");
                                                    displayContent.setInputMethodWindowLocked(win);
                                                }
                                                win.adjustStartingWindowFlags();
                                                Trace.traceEnd(32);
                                                oldVisibility = attrChanges;
                                            } catch (Exception e) {
                                                displayContent2.getInputMonitor().updateInputWindowsLw(true);
                                                Slog.w(TAG, "Exception thrown when creating surface for client " + client + " (" + ((Object) win.mAttrs.getTitle()) + ")", e);
                                                Binder.restoreCallingIdentity(origId);
                                                resetPriorityAfterLockedSection();
                                                return 0;
                                            }
                                        } else {
                                            displayContent = displayContent2;
                                            oldVisibility = attrChanges;
                                            try {
                                                Trace.traceBegin(32, "relayoutWindow: viewVisibility_2");
                                                winAnimator.mEnterAnimationPending = false;
                                                winAnimator.mEnteringAnimation = false;
                                                if (viewVisibility != 0 || !winAnimator.hasSurface()) {
                                                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                        Slog.i(TAG, "Releasing surface in: " + win);
                                                    }
                                                    try {
                                                        j = 32;
                                                        Trace.traceBegin(32, "wmReleaseOutSurface_" + ((Object) win.mAttrs.getTitle()));
                                                        outSurfaceControl.release();
                                                        Trace.traceEnd(32);
                                                    } catch (Throwable th6) {
                                                        th = th6;
                                                        resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                } else {
                                                    Trace.traceBegin(32, "relayoutWindow: getSurface");
                                                    winAnimator.mSurfaceController.getSurfaceControl(outSurfaceControl);
                                                    Trace.traceEnd(32);
                                                    j = 32;
                                                }
                                                Trace.traceEnd(j);
                                                result = result2;
                                                focusMayChange2 = focusMayChange2;
                                            } catch (Throwable th7) {
                                                th = th7;
                                                resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        }
                                        this.mHwWMSEx.setHwSecureScreenShot(win);
                                        if (focusMayChange2 || !updateFocusedWindowLocked(0, true)) {
                                            imMayMove2 = imMayMove;
                                        } else {
                                            imMayMove2 = false;
                                        }
                                        toBeDisplayed = (result & 2) == 0;
                                        if (!imMayMove2) {
                                            if (!this.mHwWMSEx.isSkipComputeImeTargetForHwMultiDisplay(getInputMethodWindowLw(), displayContent)) {
                                                displayContent.computeImeTarget(true);
                                                if (toBeDisplayed) {
                                                    displayContent.assignWindowLayers(false);
                                                }
                                            }
                                        }
                                        if (wallpaperMayMove) {
                                            displayContent.pendingLayoutChanges |= 4;
                                        }
                                        if (win.mAppToken != null) {
                                            displayContent.mUnknownAppVisibilityController.notifyRelayouted(win.mAppToken);
                                        }
                                        Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                        boolean configChanged = displayContent.updateOrientationFromAppTokens();
                                        Trace.traceEnd(32);
                                        if (!shouldRelayout && win.getFrameLw().width() == 0 && win.getFrameLw().height() == 0) {
                                            StringBuilder sb2 = new StringBuilder();
                                            displayPolicy2 = displayPolicy;
                                            sb2.append("force to relayout later when size is 1*1 for:");
                                            sb2.append(win);
                                            Slog.w(TAG, sb2.toString());
                                            if (becameVisible) {
                                                StringBuilder sb3 = new StringBuilder();
                                                sb3.append("becameVisible:");
                                                becameVisible2 = becameVisible;
                                                sb3.append(becameVisible2);
                                                Slog.w(TAG, sb3.toString());
                                                win.reportResized();
                                            } else {
                                                becameVisible2 = becameVisible;
                                            }
                                        } else {
                                            displayPolicy2 = displayPolicy;
                                            becameVisible2 = becameVisible;
                                        }
                                        if (!toBeDisplayed && win.mIsWallpaper) {
                                            DisplayInfo displayInfo = displayContent.getDisplayInfo();
                                            displayContent.mWallpaperController.updateWallpaperOffset(win, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
                                        }
                                        if (win.mAppToken != null) {
                                            win.mAppToken.updateReportedVisibilityLocked();
                                        }
                                        if (winAnimator.mReportSurfaceResized) {
                                            winAnimator.mReportSurfaceResized = false;
                                            result |= 32;
                                        }
                                        if (displayPolicy2.areSystemBarsForcedShownLw(win)) {
                                            result |= 64;
                                        }
                                        if (!win.isGoneForLayoutLw()) {
                                            win.mResizedWhileGone = false;
                                        }
                                        if (!shouldRelayout) {
                                            mergedConfiguration2 = mergedConfiguration;
                                            win.getMergedConfiguration(mergedConfiguration2);
                                        } else {
                                            mergedConfiguration2 = mergedConfiguration;
                                            win.getLastReportedMergedConfiguration(mergedConfiguration2);
                                        }
                                        win.setLastReportedMergedConfiguration(mergedConfiguration2);
                                        win.updateLastInsetValues();
                                        win.getCompatFrame(outFrame);
                                        win.getInsetsForRelayout(outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets, outOutsets);
                                        outCutout.set(win.getWmDisplayCutout().getDisplayCutout());
                                        outBackdropFrame.set(win.getBackdropFrame(win.getFrameLw()));
                                        outInsetsState.set(displayContent.getInsetsStateController().getInsetsForDispatch(win));
                                        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                            Slog.v(TAG, "Relayout of " + win + ": focusMayChange=" + focusMayChange2);
                                        }
                                        int result3 = result | (!this.mInTouchMode ? 1 : 0);
                                        Flog.i(307, "complete Relayout " + win + "  size:" + outFrame.toShortString());
                                        win.mInRelayout = false;
                                        if (viewVisibility != 0) {
                                            dc.checkNeedNotifyFingerWinCovered();
                                            dc.mObserveToken = null;
                                            dc.mTopAboveAppToken = null;
                                        }
                                    }
                                }
                                focusMayChange = true;
                                wallpaperMayMove = (win.mViewVisibility == viewVisibility && (win.mAttrs.flags & 1048576) != 0) | ((flagChanges & 1048576) == 0);
                                winAnimator.mSurfaceController.setSecure(isSecureLocked(win));
                                win.mRelayoutCalled = true;
                                win.mInRelayout = true;
                                win.mViewVisibility = viewVisibility;
                                if (!WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                                }
                                win.setDisplayLayoutNeeded();
                                win.mGivenInsetsPending = (flags & 1) == 0;
                                if (viewVisibility != 0) {
                                }
                                if (!shouldRelayout) {
                                }
                                focusMayChange2 = focusMayChange;
                                displayPolicy.setFocusChangeIMEFrozenTag(false);
                                try {
                                    this.mWindowPlacerLocked.performSurfacePlacement(true);
                                    if (!shouldRelayout) {
                                    }
                                    this.mHwWMSEx.setHwSecureScreenShot(win);
                                    if (focusMayChange2) {
                                    }
                                    imMayMove2 = imMayMove;
                                    if ((result & 2) == 0) {
                                    }
                                    if (!imMayMove2) {
                                    }
                                    if (wallpaperMayMove) {
                                    }
                                    if (win.mAppToken != null) {
                                    }
                                    Trace.traceBegin(32, "relayoutWindow: updateOrientationFromAppTokens");
                                    boolean configChanged2 = displayContent.updateOrientationFromAppTokens();
                                    Trace.traceEnd(32);
                                    if (!shouldRelayout) {
                                    }
                                    displayPolicy2 = displayPolicy;
                                    becameVisible2 = becameVisible;
                                    if (!toBeDisplayed) {
                                    }
                                    if (win.mAppToken != null) {
                                    }
                                    if (winAnimator.mReportSurfaceResized) {
                                    }
                                    if (displayPolicy2.areSystemBarsForcedShownLw(win)) {
                                    }
                                    if (!win.isGoneForLayoutLw()) {
                                    }
                                    if (!shouldRelayout) {
                                    }
                                    win.setLastReportedMergedConfiguration(mergedConfiguration2);
                                    win.updateLastInsetValues();
                                } catch (Throwable th8) {
                                    th = th8;
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                                try {
                                    win.getCompatFrame(outFrame);
                                    win.getInsetsForRelayout(outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets, outOutsets);
                                    outCutout.set(win.getWmDisplayCutout().getDisplayCutout());
                                    outBackdropFrame.set(win.getBackdropFrame(win.getFrameLw()));
                                    outInsetsState.set(displayContent.getInsetsStateController().getInsetsForDispatch(win));
                                    if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                                    }
                                    int result32 = result | (!this.mInTouchMode ? 1 : 0);
                                    Flog.i(307, "complete Relayout " + win + "  size:" + outFrame.toShortString());
                                    win.mInRelayout = false;
                                    if (viewVisibility != 0) {
                                    }
                                } catch (Throwable th9) {
                                    th = th9;
                                    resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                        }
                        imMayMove = true;
                        if (!win.mRelayoutCalled) {
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } catch (Throwable th11) {
                th = th11;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private boolean tryStartExitingAnimation(WindowState win, WindowStateAnimator winAnimator, boolean focusMayChange) {
        int transit = 2;
        if (win.mAttrs.type == 3) {
            transit = 5;
        }
        if (win.isWinVisibleLw() && !shouldHideIMExitAnim(win) && winAnimator.applyAnimationLocked(transit, false)) {
            focusMayChange = true;
            win.mAnimatingExit = true;
        } else if (win.isAnimating() && !shouldHideIMExitAnim(win)) {
            win.mAnimatingExit = true;
        } else if (win.getDisplayContent().mWallpaperController.isWallpaperTarget(win)) {
            win.mAnimatingExit = true;
        } else {
            DisplayContent displayContent = win.getDisplayContent();
            if (displayContent.mInputMethodWindow == win) {
                displayContent.setInputMethodWindowLocked(null);
            }
            boolean stopped = win.mAppToken != null ? win.mAppToken.mAppStopped : true;
            win.mDestroying = true;
            win.destroySurface(false, stopped);
        }
        if (this.mAccessibilityController != null && (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(win.getDisplayId()) || HwPCUtils.enabledInPad())) {
            this.mAccessibilityController.onWindowTransitionLocked(win, transit);
        }
        SurfaceControl.openTransaction();
        winAnimator.detachChildren();
        SurfaceControl.closeTransaction();
        return focusMayChange;
    }

    private int createSurfaceControl(SurfaceControl outSurfaceControl, int result, WindowState win, WindowStateAnimator winAnimator) {
        if (!win.mHasSurface) {
            result |= 4;
        }
        try {
            Trace.traceBegin(32, "createSurfaceControl");
            WindowSurfaceController surfaceController = winAnimator.createSurfaceLocked(win.mAttrs.type, win.mOwnerUid);
            if (surfaceController != null) {
                surfaceController.getSurfaceControl(outSurfaceControl);
            } else {
                Slog.w(TAG, "Failed to create surface control for " + win);
                outSurfaceControl.release();
            }
            return result;
        } finally {
            Trace.traceEnd(32);
        }
    }

    /* JADX INFO: finally extract failed */
    public boolean outOfMemoryWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    WindowState win = windowForClientLocked(session, client, false);
                    if (win == null) {
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean reclaimSomeSurfaceMemory = this.mRoot.reclaimSomeSurfaceMemory(win.mWinAnimator, "from-client", false);
                    resetPriorityAfterLockedSection();
                    Binder.restoreCallingIdentity(origId);
                    return reclaimSomeSurfaceMemory;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void finishDrawingWindow(Session session, IWindow client) {
        Flog.i(307, "finishDrawingWindow...");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    WindowState win = windowForClientLocked(session, client, false);
                    if (win != null && win.mWinAnimator.finishDrawingLocked()) {
                        if ((win.mAttrs.flags & 1048576) != 0) {
                            win.getDisplayContent().pendingLayoutChanges |= 4;
                        }
                        win.setDisplayLayoutNeeded();
                        if (!win.inMultiWindowMode()) {
                            this.mHwWMSEx.setStartWindowTransitionReady(win);
                        }
                        this.mWindowPlacerLocked.requestTraversal();
                        this.mHwWMSEx.updateWindowReport(win, win.mRequestedWidth, win.mRequestedHeight);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean checkCallingPermission(String permission, String func) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        Slog.w(TAG, "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission);
        return false;
    }

    public void addWindowToken(IBinder binder, int type, int displayId) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addWindowToken()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent dc = getDisplayContentOrCreate(displayId, null);
                    if (dc == null) {
                        Slog.w(TAG, "addWindowToken: Attempted to add token: " + binder + " for non-exiting displayId=" + displayId);
                        return;
                    }
                    WindowToken token = dc.getWindowToken(binder);
                    if (token != null) {
                        Slog.w(TAG, "addWindowToken: Attempted to add binder token: " + binder + " for already created window token: " + token + " displayId=" + displayId);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (type == 2013) {
                        new WallpaperWindowToken(this, binder, true, dc, true);
                    } else {
                        new WindowToken(this, binder, type, true, dc, true);
                    }
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
    }

    /* access modifiers changed from: protected */
    public boolean isTokenFound(IBinder binder, DisplayContent dc) {
        return false;
    }

    /* access modifiers changed from: protected */
    public void setFocusedDisplay(int displayId, boolean findTopTask, String reason) {
    }

    /* JADX INFO: finally extract failed */
    public void removeWindowToken(IBinder binder, int displayId) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeWindowToken()")) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                        if (dc == null) {
                            Slog.w(TAG, "removeWindowToken: Attempted to remove token: " + binder + " for non-exiting displayId=" + displayId);
                            resetPriorityAfterLockedSection();
                        } else if (dc.removeWindowToken(binder) != null || isTokenFound(binder, dc)) {
                            dc.getInputMonitor().updateInputWindowsLw(true);
                            resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                        } else {
                            Slog.w(TAG, "removeWindowToken: Attempted to remove non-existing token: " + binder);
                            resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
    }

    /* access modifiers changed from: package-private */
    public void setNewDisplayOverrideConfiguration(Configuration overrideConfig, DisplayContent dc) {
        this.mHwWMSEx.handleNewDisplayConfiguration(overrideConfig, dc.getDisplayId());
        if (dc.mWaitingForConfig) {
            dc.mWaitingForConfig = false;
            this.mLastFinishedFreezeSource = "new-config";
        }
        this.mRoot.setDisplayOverrideConfigurationIfNeeded(overrideConfig, dc);
    }

    public void prepareAppTransition(int transit, boolean alwaysKeepCurrent) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "prepareAppTransition()")) {
            getDefaultDisplayContentLocked().prepareAppTransition(transit, alwaysKeepCurrent, 0, false);
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    Slog.w(TAG, "Attempted to call overridePendingAppTransitionMultiThumbFuture for the display " + displayId + " that does not exist.");
                    return;
                }
                displayContent.mAppTransition.overridePendingAppTransitionMultiThumbFuture(specsFuture, callback, scaleUp);
                resetPriorityAfterLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter, int displayId) {
        if (checkCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "overridePendingAppTransitionRemote()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to call overridePendingAppTransitionRemote for the display " + displayId + " that does not exist.");
                        return;
                    }
                    displayContent.mAppTransition.overridePendingAppTransitionRemote(remoteAnimationAdapter);
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS permission");
        }
    }

    public void endProlongedAnimations() {
    }

    public void executeAppTransition() {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "executeAppTransition()")) {
            getDefaultDisplayContentLocked().executeAppTransition();
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    public void initializeRecentsAnimation(int targetActivityType, IRecentsAnimationRunner recentsAnimationRunner, RecentsAnimationController.RecentsAnimationCallbacks callbacks, int displayId, SparseBooleanArray recentTaskIds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRecentsAnimationController = new RecentsAnimationController(this, recentsAnimationRunner, callbacks, displayId);
                this.mRoot.getDisplayContent(displayId).mAppTransition.updateBooster();
                this.mRecentsAnimationController.initialize(targetActivityType, recentTaskIds);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setRecentsAnimationController(RecentsAnimationController controller) {
        this.mRecentsAnimationController = controller;
    }

    public RecentsAnimationController getRecentsAnimationController() {
        return this.mRecentsAnimationController;
    }

    public boolean canStartRecentsAnimation() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (getDefaultDisplayContentLocked().mAppTransition.isTransitionSet()) {
                    return false;
                }
                resetPriorityAfterLockedSection();
                return true;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void cancelRecentsAnimationSynchronously(@RecentsAnimationController.ReorderMode int reorderMode, String reason) {
        RecentsAnimationController recentsAnimationController = this.mRecentsAnimationController;
        if (recentsAnimationController != null) {
            recentsAnimationController.cancelAnimationSynchronously(reorderMode, reason);
        }
    }

    public void cleanupRecentsAnimation(@RecentsAnimationController.ReorderMode int reorderMode) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mRecentsAnimationController != null) {
                    RecentsAnimationController controller = this.mRecentsAnimationController;
                    this.mRecentsAnimationController = null;
                    controller.cleanupAnimation(reorderMode);
                    getDefaultDisplayContentLocked().mAppTransition.updateBooster();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setAppFullscreen(IBinder token, boolean toOpaque) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken atoken = this.mRoot.getAppWindowToken(token);
                if (atoken != null) {
                    atoken.setFillsParent(toOpaque);
                    setWindowOpaqueLocked(token, toOpaque);
                    this.mWindowPlacerLocked.requestTraversal();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setWindowOpaque(IBinder token, boolean isOpaque) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                setWindowOpaqueLocked(token, isOpaque);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private void setWindowOpaqueLocked(IBinder token, boolean isOpaque) {
        WindowState win;
        AppWindowToken wtoken = this.mRoot.getAppWindowToken(token);
        if (wtoken != null && (win = wtoken.findMainWindow()) != null) {
            boolean isOpaque2 = isOpaque & (!PixelFormat.formatHasAlpha(win.getAttrs().format));
            if (!win.inFreeformWindowingMode()) {
                win.mWinAnimator.setOpaqueLocked(isOpaque2);
            } else {
                win.mWinAnimator.setOpaqueLocked(false);
            }
        }
    }

    public void setDockedStackCreateState(int mode, Rect bounds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                setDockedStackCreateStateLocked(mode, bounds);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setDockedStackCreateStateLocked(int mode, Rect bounds) {
        this.mDockedStackCreateMode = mode;
        this.mDockedStackCreateBounds = bounds;
    }

    public void checkSplitScreenMinimizedChanged(boolean animate) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().checkMinimizeChanged(animate);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isValidPictureInPictureAspectRatio(int displayId, float aspectRatio) {
        return this.mRoot.getDisplayContent(displayId).getPinnedStackController().isValidPictureInPictureAspectRatio(aspectRatio);
    }

    public void getStackBounds(int windowingMode, int activityType, Rect bounds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                TaskStack stack = this.mRoot.getStack(windowingMode, activityType);
                if (stack != null) {
                    stack.getBounds(bounds);
                    return;
                }
                bounds.setEmpty();
                resetPriorityAfterLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyShowingDreamChanged() {
        notifyKeyguardFlagsChanged(null, 0);
    }

    public WindowManagerPolicy.WindowState getInputMethodWindowLw() {
        return this.mRoot.getCurrentInputMethodWindow();
    }

    public void notifyKeyguardTrustedChanged() {
        this.mAtmInternal.notifyKeyguardTrustedChanged();
    }

    public void screenTurningOff(WindowManagerPolicy.ScreenOffListener listener) {
        this.mTaskSnapshotController.screenTurningOff(listener);
    }

    public void triggerAnimationFailsafe() {
        this.mH.sendEmptyMessage(60);
    }

    public void onKeyguardShowingAndNotOccludedChanged() {
        this.mH.sendEmptyMessage(61);
    }

    public void onPowerKeyDown(boolean isScreenOn) {
        this.mRoot.forAllDisplayPolicies(PooledLambda.obtainConsumer($$Lambda$99XNq73vh8e4HVH9BuxFhbLxKVY.INSTANCE, PooledLambda.__(), Boolean.valueOf(isScreenOn)));
    }

    public void onUserSwitched() {
        this.mSettingsObserver.updateSystemUiSettings();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllDisplayPolicies($$Lambda$_jL5KNK44AQYPj1d8Hd3FYO0WM.INSTANCE);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void moveDisplayToTop(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (!(displayContent == null || this.mRoot.getTopChild() == displayContent)) {
                    this.mRoot.positionChildAt(Integer.MAX_VALUE, displayContent, true);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void deferSurfaceLayout() {
        this.mWindowPlacerLocked.deferLayout();
    }

    /* access modifiers changed from: package-private */
    public void continueSurfaceLayout() {
        this.mWindowPlacerLocked.continueLayout();
    }

    /* access modifiers changed from: package-private */
    public void notifyKeyguardFlagsChanged(Runnable callback, int displayId) {
        this.mAtmInternal.notifyKeyguardFlagsChanged(callback, displayId);
    }

    public boolean isKeyguardTrusted() {
        boolean isKeyguardTrustedLw;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                isKeyguardTrustedLw = this.mPolicy.isKeyguardTrustedLw();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return isKeyguardTrustedLw;
    }

    public void setKeyguardGoingAway(boolean keyguardGoingAway) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mKeyguardGoingAway = keyguardGoingAway;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setKeyguardOrAodShowingOnDefaultDisplay(boolean showing) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mKeyguardOrAodShowingOnDefaultDisplay = showing;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void startFreezingScreen(int exitAnim, int enterAnim) {
        if (checkCallingPermission("android.permission.FREEZE_SCREEN", "startFreezingScreen()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    if (!this.mClientFreezingScreen) {
                        this.mClientFreezingScreen = true;
                        long origId = Binder.clearCallingIdentity();
                        try {
                            startFreezingDisplayLocked(exitAnim, enterAnim);
                            this.mH.removeMessages(30);
                            this.mH.sendEmptyMessageDelayed(30, 5000);
                        } finally {
                            Binder.restoreCallingIdentity(origId);
                        }
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires FREEZE_SCREEN permission");
    }

    public void stopFreezingScreen() {
        if (checkCallingPermission("android.permission.FREEZE_SCREEN", "stopFreezingScreen()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    if (this.mClientFreezingScreen) {
                        this.mClientFreezingScreen = false;
                        this.mLastFinishedFreezeSource = "client";
                        long origId = Binder.clearCallingIdentity();
                        try {
                            stopFreezingDisplayLocked();
                        } finally {
                            Binder.restoreCallingIdentity(origId);
                        }
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires FREEZE_SCREEN permission");
    }

    /* JADX INFO: finally extract failed */
    public void disableKeyguard(IBinder token, String tag, int userId) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_DISABLEKEYGUARD);
        int userId2 = this.mAmInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "disableKeyguard", (String) null);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") == 0) {
            int callingUid = Binder.getCallingUid();
            long origIdentity = Binder.clearCallingIdentity();
            try {
                this.mKeyguardDisableHandler.disableKeyguard(token, tag, callingUid, userId2);
                Binder.restoreCallingIdentity(origIdentity);
                Slog.i(TAG, "disableKeyguard pid = " + Binder.getCallingPid() + " ,callers = " + Debug.getCallers(5));
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origIdentity);
                throw th;
            }
        } else {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
    }

    /* JADX INFO: finally extract failed */
    public void reenableKeyguard(IBinder token, int userId) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_REENABLEKEYGUARD);
        int userId2 = this.mAmInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "reenableKeyguard", (String) null);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") == 0) {
            Preconditions.checkNotNull(token, "token is null");
            int callingUid = Binder.getCallingUid();
            long origIdentity = Binder.clearCallingIdentity();
            try {
                this.mKeyguardDisableHandler.reenableKeyguard(token, callingUid, userId2);
                Binder.restoreCallingIdentity(origIdentity);
                Slog.i(TAG, "reenableKeyguard pid = " + Binder.getCallingPid() + " ,callers = " + Debug.getCallers(5));
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origIdentity);
                throw th;
            }
        } else {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
    }

    public void exitKeyguardSecurely(final IOnKeyguardExitResult callback) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_EXITKEYGUARDSECURELY);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        } else if (callback != null) {
            this.mPolicy.exitKeyguardSecurely(new WindowManagerPolicy.OnKeyguardExitResult() {
                /* class com.android.server.wm.WindowManagerService.AnonymousClass9 */

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
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_ISKEYGUARDLOCKED);
        return this.mPolicy.isKeyguardLocked();
    }

    public boolean isKeyguardShowingAndNotOccluded() {
        return this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public boolean isKeyguardSecure(int userId) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_ISKEYGUARDSECURE);
        if (userId == UserHandle.getCallingUserId() || checkCallingPermission("android.permission.INTERACT_ACROSS_USERS", "isKeyguardSecure")) {
            long origId = Binder.clearCallingIdentity();
            try {
                return this.mPolicy.isKeyguardSecure(userId);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            throw new SecurityException("Requires INTERACT_ACROSS_USERS permission");
        }
    }

    public boolean isShowingDream() {
        boolean isShowingDreamLw;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                isShowingDreamLw = getDefaultDisplayContentLocked().getDisplayPolicy().isShowingDreamLw();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return isShowingDreamLw;
    }

    public void dismissKeyguard(IKeyguardDismissCallback callback, CharSequence message) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_DISMISSKEYGUARD);
        if (checkCallingPermission("android.permission.CONTROL_KEYGUARD", "dismissKeyguard")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mPolicy.dismissKeyguardLw(callback, message);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires CONTROL_KEYGUARD permission");
    }

    public void onKeyguardOccludedChanged(boolean occluded) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.onKeyguardOccludedChangedLw(occluded);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setSwitchingUser(boolean switching) {
        if (checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "setSwitchingUser()")) {
            this.mPolicy.setSwitchingUser(switching);
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mSwitchingUser = switching;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires INTERACT_ACROSS_USERS_FULL permission");
    }

    /* access modifiers changed from: package-private */
    public void showGlobalActions() {
        this.mPolicy.showGlobalActions();
    }

    public void closeSystemDialogs(String reason) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.closeSystemDialogs(reason);
            } finally {
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
            float scale2 = fixScale(scale);
            if (which == 0) {
                this.mWindowAnimationScaleSetting = scale2;
            } else if (which == 1) {
                this.mTransitionAnimationScaleSetting = scale2;
            } else if (which == 2) {
                this.mAnimatorDurationScaleSetting = scale2;
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
        if (this.mAnimationsDisabled) {
            return 0.0f;
        }
        return this.mWindowAnimationScaleSetting;
    }

    public float getTransitionAnimationScaleLocked() {
        if (this.mAnimationsDisabled) {
            return 0.0f;
        }
        return this.mTransitionAnimationScaleSetting;
    }

    public float getAnimationScale(int which) {
        if (which == 0) {
            return this.mWindowAnimationScaleSetting;
        }
        if (which == 1) {
            return this.mTransitionAnimationScaleSetting;
        }
        if (which != 2) {
            return 0.0f;
        }
        return this.mAnimatorDurationScaleSetting;
    }

    public float[] getAnimationScales() {
        return new float[]{this.mWindowAnimationScaleSetting, this.mTransitionAnimationScaleSetting, this.mAnimatorDurationScaleSetting};
    }

    public float getCurrentAnimatorScale() {
        float f;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                f = this.mAnimationsDisabled ? 0.0f : this.mAnimatorDurationScaleSetting;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return f;
    }

    /* access modifiers changed from: package-private */
    public void dispatchNewAnimatorScaleLocked(Session session) {
        this.mH.obtainMessage(34, session).sendToTarget();
    }

    public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.registerPointerEventListener(listener);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.unregisterPointerEventListener(listener);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public int getFocusedDisplayId() {
        return 0;
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
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentProfileIds = currentProfileIds;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        int targetDensity;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentUserId = newUserId;
                this.mCurrentProfileIds = currentProfileIds;
                this.mPolicy.setCurrentUserLw(newUserId);
                this.mKeyguardDisableHandler.setCurrentUser(newUserId);
                this.mRoot.switchUser();
                this.mWindowPlacerLocked.performSurfacePlacement();
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                TaskStack stack = displayContent.getSplitScreenPrimaryStackIgnoringVisibility();
                displayContent.mDividerControllerLocked.notifyDockedStackExistsChanged(stack != null && stack.hasTaskForUser(newUserId));
                this.mRoot.forAllDisplays(new Consumer(newUserId) {
                    /* class com.android.server.wm.$$Lambda$WindowManagerService$05fsn8aS3Yh8PJChNK4X3zTgx6M */
                    private final /* synthetic */ int f$0;

                    {
                        this.f$0 = r1;
                    }

                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((DisplayContent) obj).mAppTransition.setCurrentUser(this.f$0);
                    }
                });
                if (this.mDisplayReady) {
                    int forcedDensity = getForcedDisplayDensityForUserLocked(newUserId);
                    if (forcedDensity != 0) {
                        targetDensity = forcedDensity;
                    } else {
                        targetDensity = displayContent.mInitialDisplayDensity;
                    }
                    displayContent.setForcedDensity(targetDensity, -2);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        IHwWindowManagerServiceEx iHwWindowManagerServiceEx = this.mHwWMSEx;
        if (iHwWindowManagerServiceEx != null) {
            iHwWindowManagerServiceEx.setCurrentUser(newUserId, currentProfileIds);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUserId) {
            return true;
        }
        int i = 0;
        while (true) {
            int[] iArr = this.mCurrentProfileIds;
            if (i >= iArr.length) {
                return false;
            }
            if (iArr[i] == userId) {
                return true;
            }
            i++;
        }
    }

    public void enableScreenAfterBoot() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!this.mSystemBooted) {
                    this.mSystemBooted = true;
                    hideBootMessagesLocked();
                    this.mH.sendEmptyMessageDelayed(23, 30000);
                    resetPriorityAfterLockedSection();
                    this.mPolicy.systemBooted();
                    performEnableScreen();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void enableScreenIfNeeded() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                enableScreenIfNeededLocked();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void enableScreenIfNeededLocked() {
        if (!this.mDisplayEnabled) {
            if (this.mSystemBooted || this.mShowingBootMessages) {
                this.mH.sendEmptyMessage(16);
            }
        }
    }

    public void performBootTimeout() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!this.mDisplayEnabled) {
                    Slog.w(TAG, "***** BOOT TIMEOUT: forcing display enabled");
                    this.mForceDisplayEnabled = true;
                    resetPriorityAfterLockedSection();
                    performEnableScreen();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void onSystemUiStarted() {
        this.mPolicy.onSystemUiStarted();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0109, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:?, code lost:
        r9.mActivityManager.bootAnimationComplete();
     */
    public void performEnableScreen() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!this.mDisplayEnabled) {
                    if (!this.mSystemBooted && !this.mShowingBootMessages) {
                        resetPriorityAfterLockedSection();
                        return;
                    } else if (this.mShowingBootMessages || this.mPolicy.canDismissBootAnimation()) {
                        if (this.mBootMetricsLogIndex == 0) {
                            MetricsLogger.histogram((Context) null, "boot_window_manager_keyguard_drawn", (int) SystemClock.uptimeMillis());
                            this.mBootMetricsLogIndex++;
                        }
                        if (this.mForceDisplayEnabled || !getDefaultDisplayContentLocked().checkWaitingForWindows()) {
                            if (this.mBootMetricsLogIndex == 1) {
                                MetricsLogger.histogram((Context) null, "boot_window_manager_windows_drawn", (int) SystemClock.uptimeMillis());
                                this.mBootMetricsLogIndex++;
                            }
                            if (!this.mBootAnimationStopped) {
                                Trace.asyncTraceBegin(32, "Stop bootanim", 0);
                                SystemProperties.set("service.bootanim.exit", "1");
                                this.mBootAnimationStopped = true;
                            }
                            if (this.mForceDisplayEnabled || checkBootAnimationCompleteLocked()) {
                                if (this.mBootMetricsLogIndex == 2) {
                                    MetricsLogger.histogram((Context) null, "boot_window_manager_anim_completed", (int) SystemClock.uptimeMillis());
                                    this.mBootMetricsLogIndex++;
                                }
                                try {
                                    IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                                    if (surfaceFlinger != null) {
                                        Slog.i(TAG, "******* TELLING SURFACE FLINGER WE ARE BOOTED!");
                                        Parcel data = Parcel.obtain();
                                        data.writeInterfaceToken("android.ui.ISurfaceComposer");
                                        surfaceFlinger.transact(1, data, null, 0);
                                        data.recycle();
                                    } else {
                                        Slog.i(TAG, "performEnableScreen: SurfaceFlinger is dead!");
                                    }
                                } catch (RemoteException e) {
                                    Slog.e(TAG, "Boot completed: SurfaceFlinger is dead!");
                                }
                                EventLog.writeEvent(31007, SystemClock.uptimeMillis());
                                Trace.asyncTraceEnd(32, "Stop bootanim", 0);
                                this.mDisplayEnabled = true;
                                if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                                    Slog.i(TAG, "******************** ENABLING SCREEN!");
                                }
                                this.mInputManagerCallback.setEventDispatchingLw(this.mEventDispatchingEnabled);
                            } else {
                                Slog.i(TAG, "Waiting for anim complete");
                                resetPriorityAfterLockedSection();
                                return;
                            }
                        } else {
                            Slog.i(TAG, "Waiting for all visiable windows drawn");
                            resetPriorityAfterLockedSection();
                            return;
                        }
                    } else {
                        Slog.i(TAG, "Keyguard not drawn complete,can not dismiss boot animation");
                        resetPriorityAfterLockedSection();
                        return;
                    }
                } else {
                    return;
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        this.mPolicy.enableScreenAfterBoot();
        updateRotationUnchecked(false, false);
    }

    /* access modifiers changed from: private */
    public boolean checkBootAnimationCompleteLocked() {
        if (!SystemService.isRunning(BOOT_ANIMATION_SERVICE)) {
            return true;
        }
        this.mH.removeMessages(37);
        this.mH.sendEmptyMessageDelayed(37, 200);
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x002e, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0031, code lost:
        if (r0 == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0033, code lost:
        performEnableScreen();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:?, code lost:
        return;
     */
    public void showBootMessage(CharSequence msg, boolean always) {
        boolean first = false;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mAllowBootMessages) {
                    if (!this.mShowingBootMessages) {
                        if (!always) {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        first = true;
                    }
                    if (this.mSystemBooted) {
                        resetPriorityAfterLockedSection();
                    } else {
                        this.mShowingBootMessages = true;
                        this.mPolicy.showBootMessage(msg, always);
                    }
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void hideBootMessagesLocked() {
        if (this.mShowingBootMessages) {
            this.mShowingBootMessages = false;
            this.mPolicy.hideBootMessages();
        }
    }

    public void setInTouchMode(boolean mode) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mInTouchMode = mode;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: private */
    public void updateCircularDisplayMaskIfNeeded() {
        int currentUserId;
        if (this.mContext.getResources().getConfiguration().isScreenRound() && this.mContext.getResources().getBoolean(17891603)) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    currentUserId = this.mCurrentUserId;
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            int showMask = 0;
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, currentUserId) != 1) {
                showMask = 1;
            }
            Message m = this.mH.obtainMessage(35);
            m.arg1 = showMask;
            this.mH.sendMessage(m);
        }
    }

    public void showEmulatorDisplayOverlayIfNeeded() {
        if (this.mContext.getResources().getBoolean(17891599) && SystemProperties.getBoolean(PROPERTY_EMULATOR_CIRCULAR, false) && Build.IS_EMULATOR) {
            H h = this.mH;
            h.sendMessage(h.obtainMessage(36));
        }
    }

    public void showCircularMask(boolean visible) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                openSurfaceTransaction();
                if (visible) {
                    try {
                        if (this.mCircularDisplayMask == null) {
                            this.mCircularDisplayMask = new CircularDisplayMask(getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10, this.mContext.getResources().getInteger(17694959), this.mContext.getResources().getDimensionPixelSize(17105046));
                        }
                        this.mCircularDisplayMask.setVisibility(true);
                    } catch (Throwable th) {
                        closeSurfaceTransaction("showCircularMask");
                        throw th;
                    }
                } else if (this.mCircularDisplayMask != null) {
                    this.mCircularDisplayMask.setVisibility(false);
                    this.mCircularDisplayMask = null;
                }
                closeSurfaceTransaction("showCircularMask");
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void showEmulatorDisplayOverlay() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                openSurfaceTransaction();
                try {
                    if (this.mEmulatorDisplayOverlay == null) {
                        this.mEmulatorDisplayOverlay = new EmulatorDisplayOverlay(this.mContext, getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10);
                    }
                    this.mEmulatorDisplayOverlay.setVisibility(true);
                } finally {
                    closeSurfaceTransaction("showEmulatorDisplayOverlay");
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void showStrictModeViolation(boolean on) {
        int pid = Binder.getCallingPid();
        if (on) {
            H h = this.mH;
            h.sendMessage(h.obtainMessage(25, 1, pid));
            H h2 = this.mH;
            h2.sendMessageDelayed(h2.obtainMessage(25, 0, pid), ALLWINDOWDRAW_MAX_TIMEOUT_TIME);
            return;
        }
        H h3 = this.mH;
        h3.sendMessage(h3.obtainMessage(25, 0, pid));
    }

    /* access modifiers changed from: private */
    public void showStrictModeViolation(int arg, int pid) {
        boolean on = arg != 0;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!on || this.mRoot.canShowStrictModeViolation(pid)) {
                    SurfaceControl.openTransaction();
                    try {
                        if (this.mStrictModeFlash == null) {
                            this.mStrictModeFlash = new StrictModeFlash(getDefaultDisplayContentLocked());
                        }
                        this.mStrictModeFlash.setVisibility(on);
                        resetPriorityAfterLockedSection();
                    } finally {
                        SurfaceControl.closeTransaction();
                    }
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setStrictModeVisualIndicatorPreference(String value) {
        SystemProperties.set("persist.sys.strictmode.visual", value);
    }

    /* JADX INFO: finally extract failed */
    public Bitmap screenshotWallpaper() {
        Bitmap screenshotWallpaperLocked;
        if (checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotWallpaper()")) {
            try {
                Trace.traceBegin(32, "screenshotWallpaper");
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        screenshotWallpaperLocked = this.mRoot.getDisplayContent(0).mWallpaperController.screenshotWallpaperLocked();
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
                return screenshotWallpaperLocked;
            } finally {
                Trace.traceEnd(32);
            }
        } else {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
    }

    /* JADX INFO: finally extract failed */
    public boolean requestAssistScreenshot(IAssistDataReceiver receiver) {
        Bitmap bm;
        if (checkCallingPermission("android.permission.READ_FRAME_BUFFER", "requestAssistScreenshot()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(0);
                    if (displayContent == null) {
                        bm = null;
                    } else {
                        bm = displayContent.screenshotDisplayLocked(Bitmap.Config.ARGB_8888);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            FgThread.getHandler().post(new Runnable(receiver, bm) {
                /* class com.android.server.wm.$$Lambda$WindowManagerService$Zv37mcLTUXyG89YznyHzluaKNE0 */
                private final /* synthetic */ IAssistDataReceiver f$0;
                private final /* synthetic */ Bitmap f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                public final void run() {
                    WindowManagerService.lambda$requestAssistScreenshot$3(this.f$0, this.f$1);
                }
            });
            return true;
        }
        throw new SecurityException("Requires READ_FRAME_BUFFER permission");
    }

    static /* synthetic */ void lambda$requestAssistScreenshot$3(IAssistDataReceiver receiver, Bitmap bm) {
        if (receiver != null) {
            try {
                receiver.onHandleAssistScreenshot(bm);
            } catch (RemoteException e) {
            }
        }
    }

    public ActivityManager.TaskSnapshot getTaskSnapshot(int taskId, int userId, boolean reducedResolution, boolean restoreFromDisk) {
        return this.mTaskSnapshotController.getSnapshot(taskId, userId, restoreFromDisk, reducedResolution);
    }

    public void removeObsoleteTaskFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.removeObsoleteTaskFiles(persistentTaskIds, runningUserIds);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setRotateForApp(int displayId, int fixedToUserRotation) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w(TAG, "Trying to set rotate for app for a missing display.");
                    return;
                }
                display.getDisplayRotation().setFixedToUserRotation(fixedToUserRotation);
                resetPriorityAfterLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void freezeRotation(int rotation) {
        freezeDisplayRotation(0, rotation);
    }

    /* JADX INFO: finally extract failed */
    public void freezeDisplayRotation(int displayId, int rotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        } else if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        } else {
            Flog.i(308, "freezeRotation: displayId=" + displayId + ",rotation=" + rotation + ",by pid=" + Binder.getCallingPid());
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent display = this.mRoot.getDisplayContent(displayId);
                        if (display == null) {
                            Slog.w(TAG, "Trying to freeze rotation for a missing display.");
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        display.getDisplayRotation().freezeRotation(rotation);
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(origId);
                        updateRotationUnchecked(false, false);
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void thawRotation() {
        thawDisplayRotation(0);
    }

    /* JADX INFO: finally extract failed */
    public void thawDisplayRotation(int displayId) {
        if (checkCallingPermission("android.permission.SET_ORIENTATION", "thawRotation()")) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v(TAG, "thawRotation: mRotation=" + getDefaultDisplayRotation());
            }
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent display = this.mRoot.getDisplayContent(displayId);
                        if (display == null) {
                            Slog.w(TAG, "Trying to thaw rotation for a missing display.");
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        display.getDisplayRotation().thawRotation();
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(origId);
                        updateRotationUnchecked(false, false);
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
    }

    public boolean isRotationFrozen() {
        return isDisplayRotationFrozen(0);
    }

    public boolean isDisplayRotationFrozen(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w(TAG, "Trying to thaw rotation for a missing display.");
                    return false;
                }
                boolean isRotationFrozen = display.getDisplayRotation().isRotationFrozen();
                resetPriorityAfterLockedSection();
                return isRotationFrozen;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        updateRotationUnchecked(alwaysSendConfiguration, forceRelayout);
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void updateRotationUnchecked(boolean alwaysSendConfiguration, boolean forceRelayout) {
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v(TAG, "updateRotationUnchecked: alwaysSendConfiguration=" + alwaysSendConfiguration + " forceRelayout=" + forceRelayout);
        }
        long j = 32;
        Trace.traceBegin(32, "updateRotation");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    boolean layoutNeeded = false;
                    int displayCount = this.mRoot.mChildren.size();
                    int i = 0;
                    while (i < displayCount) {
                        DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
                        Trace.traceBegin(j, "updateRotation: display");
                        boolean rotationChanged = displayContent.updateRotationUnchecked();
                        Trace.traceEnd(j);
                        Flog.i(308, "updateRotationUnchecked: alwaysSendConfiguration=" + alwaysSendConfiguration + " forceRelayout=" + forceRelayout + " rotationChanged=" + rotationChanged);
                        if (rotationChanged && !this.mIsPerfBoost) {
                            this.mIsPerfBoost = true;
                            UniPerf.getInstance().uniPerfEvent(4105, "", new int[]{0});
                        }
                        if (!rotationChanged || forceRelayout) {
                            displayContent.setLayoutNeeded();
                            layoutNeeded = true;
                        }
                        if (rotationChanged) {
                            LogPower.push(128);
                        }
                        if (rotationChanged || alwaysSendConfiguration) {
                            displayContent.sendNewConfiguration();
                        }
                        i++;
                        j = 32;
                    }
                    if (layoutNeeded) {
                        Trace.traceBegin(32, "updateRotation: performSurfacePlacement");
                        this.mWindowPlacerLocked.performSurfacePlacement();
                        Trace.traceEnd(32);
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
            Trace.traceEnd(32);
        }
    }

    public int getDefaultDisplayRotation() {
        int rotation;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                rotation = getDefaultDisplayContentLocked().getRotation();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return rotation;
    }

    public int watchRotation(IRotationWatcher watcher, int displayId) {
        DisplayContent displayContent;
        int rotation;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                displayContent = this.mRoot.getDisplayContent(displayId);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        if (displayContent != null) {
            final IBinder watcherBinder = watcher.asBinder();
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                /* class com.android.server.wm.WindowManagerService.AnonymousClass10 */

                public void binderDied() {
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            int i = 0;
                            while (i < WindowManagerService.this.mRotationWatchers.size()) {
                                if (watcherBinder == WindowManagerService.this.mRotationWatchers.get(i).mWatcher.asBinder()) {
                                    IBinder binder = WindowManagerService.this.mRotationWatchers.remove(i).mWatcher.asBinder();
                                    if (binder != null) {
                                        binder.unlinkToDeath(this, 0);
                                    }
                                    i--;
                                }
                                i++;
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            };
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    watcher.asBinder().linkToDeath(dr, 0);
                    this.mRotationWatchers.add(new RotationWatcher(watcher, dr, displayId));
                } catch (RemoteException e) {
                }
                try {
                    rotation = displayContent.getRotation();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return rotation;
        }
        throw new IllegalArgumentException("Trying to register rotation event for invalid display: " + displayId);
    }

    public void removeRotationWatcher(IRotationWatcher watcher) {
        IBinder watcherBinder = watcher.asBinder();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                int i = 0;
                while (i < this.mRotationWatchers.size()) {
                    if (watcherBinder == this.mRotationWatchers.get(i).mWatcher.asBinder()) {
                        RotationWatcher removed = this.mRotationWatchers.remove(i);
                        IBinder binder = removed.mWatcher.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(removed.mDeathRecipient, 0);
                        }
                        i--;
                    }
                    i++;
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) {
        boolean isWallpaperVisible;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    this.mWallpaperVisibilityListeners.registerWallpaperVisibilityListener(listener, displayId);
                    isWallpaperVisible = displayContent.mWallpaperController.isWallpaperVisible();
                } else {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + displayId);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return isWallpaperVisible;
    }

    public void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWallpaperVisibilityListeners.unregisterWallpaperVisibilityListener(listener, displayId);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void registerSystemGestureExclusionListener(ISystemGestureExclusionListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.registerSystemGestureExclusionListener(listener);
                } else {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + displayId);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void unregisterSystemGestureExclusionListener(ISystemGestureExclusionListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.unregisterSystemGestureExclusionListener(listener);
                    resetPriorityAfterLockedSection();
                } else if (Binder.getCallingPid() == Process.myPid()) {
                    Slog.i(TAG, "Do not unregister listener because displaycontent is null.");
                } else {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + displayId);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void reportSystemGestureExclusionChanged(Session session, IWindow window, List<Rect> exclusionRects) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, window, true);
                if (win.setSystemGestureExclusion(exclusionRects)) {
                    win.getDisplayContent().updateSystemGestureExclusion();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void registerDisplayFoldListener(IDisplayFoldListener listener) {
        this.mPolicy.registerDisplayFoldListener(listener);
    }

    public void unregisterDisplayFoldListener(IDisplayFoldListener listener) {
        this.mPolicy.unregisterDisplayFoldListener(listener);
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void setOverrideFoldedArea(Rect area) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        this.mPolicy.setOverrideFoldedArea(area);
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public Rect getFoldedArea() {
        Rect foldedArea;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    foldedArea = this.mPolicy.getFoldedArea();
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            return foldedArea;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getPreferredOptionsPanelGravity(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    return 81;
                }
                int preferredOptionsPanelGravity = displayContent.getPreferredOptionsPanelGravity();
                resetPriorityAfterLockedSection();
                return preferredOptionsPanelGravity;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean startViewServer(int port) {
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "startViewServer") || port < 1024) {
            return false;
        }
        ViewServer viewServer = this.mViewServer;
        if (viewServer != null) {
            if (!viewServer.isRunning()) {
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
        ViewServer viewServer;
        if (!isSystemSecure() && checkCallingPermission("android.permission.DUMP", "stopViewServer") && (viewServer = this.mViewServer) != null) {
            return viewServer.stop();
        }
        return false;
    }

    public boolean isViewServerRunning() {
        ViewServer viewServer;
        if (!isSystemSecure() && checkCallingPermission("android.permission.DUMP", "isViewServerRunning") && (viewServer = this.mViewServer) != null && viewServer.isRunning()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public boolean viewServerListWindows(Socket client) {
        if (isSystemSecure()) {
            return false;
        }
        ArrayList<WindowState> windows = new ArrayList<>();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllWindows((Consumer<WindowState>) new Consumer(windows) {
                    /* class com.android.server.wm.$$Lambda$WindowManagerService$Yf21B7QM1fRVFGIQy6MImYjka28 */
                    private final /* synthetic */ ArrayList f$0;

                    {
                        this.f$0 = r1;
                    }

                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        this.f$0.add((WindowState) obj);
                    }
                }, false);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        BufferedWriter out = null;
        try {
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()), 8192);
            int count = windows.size();
            for (int i = 0; i < count; i++) {
                WindowState w = windows.get(i);
                out2.write(Integer.toHexString(System.identityHashCode(w)));
                out2.write(32);
                out2.append(w.mAttrs.getTitle());
                out2.write(10);
            }
            out2.write("DONE.\n");
            out2.flush();
            try {
                out2.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (Exception e2) {
            if (0 == 0) {
                return false;
            }
            out.close();
            return false;
        } catch (Throwable th2) {
            if (0 != 0) {
                try {
                    out.close();
                } catch (IOException e3) {
                }
            }
            throw th2;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean viewServerGetFocusedWindow(Socket client) {
        if (isSystemSecure()) {
            return false;
        }
        WindowState focusedWindow = getFocusedWindow();
        BufferedWriter out = null;
        try {
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()), 8192);
            if (focusedWindow != null) {
                out2.write(Integer.toHexString(System.identityHashCode(focusedWindow)));
                out2.write(32);
                out2.append(focusedWindow.mAttrs.getTitle());
            }
            out2.write(10);
            out2.flush();
            try {
                out2.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (Exception e2) {
            if (0 == 0) {
                return false;
            }
            out.close();
            return false;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    out.close();
                } catch (IOException e3) {
                }
            }
            throw th;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean viewServerWindowCommand(Socket client, String command, String parameters) {
        String parameters2;
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
                parameters2 = parameters.substring(index + 1);
            } else {
                parameters2 = "";
            }
            WindowState window = findWindow(hashCode);
            if (window == null) {
                if (0 != 0) {
                    data.recycle();
                }
                if (0 != 0) {
                    reply.recycle();
                }
                if (0 != 0) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
                return false;
            }
            Parcel data2 = Parcel.obtain();
            data2.writeInterfaceToken("android.view.IWindow");
            data2.writeString(command);
            data2.writeString(parameters2);
            data2.writeInt(1);
            ParcelFileDescriptor.fromSocket(client).writeToParcel(data2, 0);
            Parcel reply2 = Parcel.obtain();
            window.mClient.asBinder().transact(1, data2, reply2, 0);
            reply2.readException();
            if (!client.isOutputShutdown()) {
                out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                out.write("DONE\n");
                out.flush();
            }
            data2.recycle();
            reply2.recycle();
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e2) {
                }
            }
            return success;
        } catch (Exception e3) {
            Slog.w(TAG, "Could not send command " + command + " with parameters " + parameters, e3);
            success = false;
            if (0 != 0) {
                data.recycle();
            }
            if (0 != 0) {
                reply.recycle();
            }
            if (0 != 0) {
                out.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                data.recycle();
            }
            if (0 != 0) {
                reply.recycle();
            }
            if (0 != 0) {
                try {
                    out.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public void addWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.add(listener);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void removeWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.remove(listener);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0025, code lost:
        resetPriorityAfterLockedSection();
        r0 = r2.length;
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x002a, code lost:
        if (r2 >= r0) goto L_0x0034;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x002c, code lost:
        r2[r2].windowsChanged();
        r2 = r2 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0034, code lost:
        return;
     */
    public void notifyWindowsChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!this.mWindowChangeListeners.isEmpty()) {
                    WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0025, code lost:
        resetPriorityAfterLockedSection();
        r0 = r2.length;
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x002a, code lost:
        if (r2 >= r0) goto L_0x0034;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x002c, code lost:
        r2[r2].focusChanged();
        r2 = r2 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0034, code lost:
        return;
     */
    public void notifyFocusChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!this.mWindowChangeListeners.isEmpty()) {
                    WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    private WindowState findWindow(int hashCode) {
        WindowState window;
        if (hashCode == -1) {
            return getFocusedWindow();
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                window = this.mRoot.getWindow(new Predicate(hashCode) {
                    /* class com.android.server.wm.$$Lambda$WindowManagerService$tOeHm8ndyhv8iLNQ_GHuZ7HhJdw */
                    private final /* synthetic */ int f$0;

                    {
                        this.f$0 = r1;
                    }

                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return WindowManagerService.lambda$findWindow$5(this.f$0, (WindowState) obj);
                    }
                });
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return window;
    }

    static /* synthetic */ boolean lambda$findWindow$5(int hashCode, WindowState w) {
        return System.identityHashCode(w) == hashCode;
    }

    /* access modifiers changed from: package-private */
    public void sendNewConfiguration(int displayId) {
        try {
            if (!this.mActivityTaskManager.updateDisplayOverrideConfiguration((Configuration) null, displayId)) {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                        if (dc != null && dc.mWaitingForConfig) {
                            dc.mWaitingForConfig = false;
                            this.mLastFinishedFreezeSource = "config-unchanged";
                            dc.setLayoutNeeded();
                            this.mWindowPlacerLocked.performSurfacePlacement();
                        }
                    } finally {
                        resetPriorityAfterLockedSection();
                    }
                }
            }
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public Configuration computeNewConfiguration(int displayId) {
        Configuration computeNewConfigurationLocked;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                computeNewConfigurationLocked = computeNewConfigurationLocked(displayId);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return computeNewConfigurationLocked;
    }

    private Configuration computeNewConfigurationLocked(int displayId) {
        if (!this.mDisplayReady) {
            return null;
        }
        Configuration config = new Configuration();
        this.mRoot.getDisplayContent(displayId).computeScreenConfiguration(config);
        return config;
    }

    /* access modifiers changed from: package-private */
    public void notifyHardKeyboardStatusChange() {
        WindowManagerInternal.OnHardKeyboardStatusChangeListener listener;
        boolean available;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                listener = this.mHardKeyboardStatusChangeListener;
                available = this.mHardKeyboardAvailable;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        if (listener != null) {
            listener.onHardKeyboardStatusChange(available);
        }
        if (!HwPCUtils.enabledInPad()) {
            this.mHwWMSEx.relaunchIMEProcess();
        }
    }

    public void setEventDispatching(boolean enabled) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setEventDispatching()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mEventDispatchingEnabled = enabled;
                    if (this.mDisplayEnabled) {
                        this.mInputManagerCallback.setEventDispatchingLw(enabled);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
    }

    /* access modifiers changed from: protected */
    public WindowState getFocusedWindow() {
        WindowState focusedWindowLocked;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                focusedWindowLocked = getFocusedWindowLocked();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return focusedWindowLocked;
    }

    public String getFocusedAppComponentName() {
        DisplayContent topFocusedDisplayContent = this.mRoot.getTopFocusedDisplayContent();
        if (topFocusedDisplayContent.mFocusedApp != null) {
            return topFocusedDisplayContent.mFocusedApp.appComponentName;
        }
        return null;
    }

    /* access modifiers changed from: private */
    public WindowState getFocusedWindowLocked() {
        return this.mRoot.getTopFocusedDisplayContent().mCurrentFocus;
    }

    /* access modifiers changed from: package-private */
    public TaskStack getImeFocusStackLocked() {
        AppWindowToken focusedApp = this.mRoot.getTopFocusedDisplayContent().mFocusedApp;
        if (focusedApp == null || focusedApp.getTask() == null) {
            return null;
        }
        return focusedApp.getTask().mStack;
    }

    public boolean detectSafeMode() {
        if (this.mHwWMSEx.detectSafeMode()) {
            return this.mHwWMSEx.getSafeMode();
        }
        if (!this.mInputManagerCallback.waitForInputDevicesReady(ALLWINDOWDRAW_MAX_TIMEOUT_TIME)) {
            Slog.w(TAG, "Devices still not ready after waiting 1000 milliseconds before attempting to detect safe mode.");
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "safe_boot_disallowed", 0) != 0) {
            return false;
        }
        int menuState = this.mInputManager.getKeyCodeState(-1, -256, 82);
        int sState = this.mInputManager.getKeyCodeState(-1, -256, 47);
        int dpadState = this.mInputManager.getKeyCodeState(-1, 513, 23);
        int trackballState = this.mInputManager.getScanCodeState(-1, 65540, 272);
        this.mSafeMode = menuState > 0 || sState > 0 || dpadState > 0 || trackballState > 0;
        try {
            if (!(SystemProperties.getInt("persist.sys.safemode", 0) == 0 && SystemProperties.getInt("ro.sys.safemode", 0) == 0)) {
                this.mSafeMode = true;
                SystemProperties.set("persist.sys.safemode", "");
            }
        } catch (IllegalArgumentException e) {
        }
        if ("factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            this.mSafeMode = false;
        }
        if (this.mSafeMode) {
            Log.i(TAG, "SAFE MODE ENABLED (menu=" + menuState + " s=" + sState + " dpad=" + dpadState + " trackball=" + trackballState + ")");
            if (SystemProperties.getInt("ro.sys.safemode", 0) == 0) {
                SystemProperties.set("ro.sys.safemode", "1");
            }
        } else {
            Log.i(TAG, "SAFE MODE not enabled");
        }
        this.mPolicy.setSafeMode(this.mSafeMode);
        return this.mSafeMode;
    }

    public void displayReady() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mMaxUiWidth > 0) {
                    this.mRoot.forAllDisplays(new Consumer() {
                        /* class com.android.server.wm.$$Lambda$WindowManagerService$_tfpDlf3MkHSDi8MNIOlvGgvLS8 */

                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WindowManagerService.this.lambda$displayReady$6$WindowManagerService((DisplayContent) obj);
                        }
                    });
                }
                boolean changed = applyForcedPropertiesForDefaultDisplay();
                this.mAnimator.ready();
                this.mDisplayReady = true;
                if (changed || HwFoldScreenState.isFoldScreenDevice()) {
                    reconfigureDisplayLocked(getDefaultDisplayContentLocked());
                }
                this.mIsTouchDevice = this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        try {
            this.mActivityTaskManager.updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
        }
        updateCircularDisplayMaskIfNeeded();
    }

    public /* synthetic */ void lambda$displayReady$6$WindowManagerService(DisplayContent displayContent) {
        displayContent.setMaxUiWidth(this.mMaxUiWidth);
    }

    public void systemReady() {
        this.mSystemReady = true;
        this.mPolicy.systemReady();
        this.mRoot.forAllDisplayPolicies($$Lambda$cJEiQ28RvThCcuht9wXeFzPgo.INSTANCE);
        this.mTaskSnapshotController.systemReady();
        this.mHasWideColorGamutSupport = queryWideColorGamutSupport();
        this.mHasHdrSupport = queryHdrSupport();
        Handler handler = UiThread.getHandler();
        SettingsObserver settingsObserver = this.mSettingsObserver;
        Objects.requireNonNull(settingsObserver);
        handler.post(new Runnable() {
            /* class com.android.server.wm.$$Lambda$iQxeP_PsHHArcPSFabJ3FXyPKNc */

            public final void run() {
                SettingsObserver.this.updateSystemUiSettings();
            }
        });
        Handler handler2 = UiThread.getHandler();
        SettingsObserver settingsObserver2 = this.mSettingsObserver;
        Objects.requireNonNull(settingsObserver2);
        handler2.post(new Runnable() {
            /* class com.android.server.wm.$$Lambda$B58NKEOrr2mhFWeS3bqpaZnd11o */

            public final void run() {
                SettingsObserver.this.updatePointerLocation();
            }
        });
        IVrManager vrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (vrManager != null) {
            try {
                boolean vrModeEnabled = vrManager.getVrModeState();
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        vrManager.registerListener(this.mVrStateCallbacks);
                        if (vrModeEnabled) {
                            this.mVrModeEnabled = vrModeEnabled;
                            this.mVrStateCallbacks.onVrStateChanged(vrModeEnabled);
                        }
                    } finally {
                        resetPriorityAfterLockedSection();
                    }
                }
            } catch (RemoteException e) {
            }
        }
        this.mHwWMSEx.hwSystemReady();
    }

    private static boolean queryWideColorGamutSupport() {
        try {
            OptionalBool hasWideColor = ISurfaceFlingerConfigs.getService().hasWideColorDisplay();
            if (hasWideColor != null) {
                return hasWideColor.value;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private static boolean queryHdrSupport() {
        try {
            OptionalBool hasHdr = ISurfaceFlingerConfigs.getService().hasHDRDisplay();
            if (hasHdr != null) {
                return hasHdr.value;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    final class H extends Handler {
        public static final int ALL_WINDOWS_DRAWN = 33;
        public static final int ANIMATION_FAILSAFE = 60;
        public static final int APP_FREEZE_TIMEOUT = 17;
        public static final int APP_TRANSITION_GETSPECSFUTURE_TIMEOUT = 102;
        public static final int BOOT_TIMEOUT = 23;
        public static final int CHECK_IF_BOOT_ANIMATION_FINISHED = 37;
        public static final int CLIENT_FREEZE_TIMEOUT = 30;
        public static final int ENABLE_EVENT_DISPATCHING = 108;
        public static final int ENABLE_SCREEN = 16;
        public static final int FORCE_GC = 15;
        public static final int GESTURE_NAVUP_TIMEOUT = 106;
        public static final int NEW_ANIMATOR_SCALE = 34;
        public static final int NOTIFY_ACTIVITY_DRAWN = 32;
        public static final int ON_POINTER_DOWN_OUTSIDE_FOCUS = 62;
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
        public static final int SPLIT_RESIZING_TIMEOUT = 105;
        public static final int UNFREEZE_FOLD_ROTATION = 107;
        public static final int UNUSED = 0;
        public static final int UPDATE_ANIMATION_SCALE = 51;
        public static final int UPDATE_DOCKED_STACK_DIVIDER = 41;
        public static final int WAITING_FOR_DRAWN_TIMEOUT = 24;
        public static final int WALLPAPER_DRAW_PENDING_TIMEOUT = 39;
        public static final int WINDOW_FREEZE_TIMEOUT = 11;
        public static final int WINDOW_HIDE_TIMEOUT = 52;
        public static final int WINDOW_REPLACEMENT_TIMEOUT = 46;

        H() {
        }

        /* JADX INFO: finally extract failed */
        /* JADX INFO: Multiple debug info for r0v13 int: [D('window' com.android.server.wm.WindowState), D('mode' int)] */
        /* JADX WARNING: Code restructure failed: missing block: B:403:0x068d, code lost:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:404:0x0690, code lost:
            if (r1 == null) goto L_0x0695;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:405:0x0692, code lost:
            r1.onWindowFocusChangedNotLocked();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:406:0x0695, code lost:
            if (r6 == null) goto L_0x06bd;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:408:0x0699, code lost:
            if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT == false) goto L_0x06b1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:409:0x069b, code lost:
            android.util.Slog.i(com.android.server.wm.WindowManagerService.TAG, "Gaining focus: " + r6);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:410:0x06b1, code lost:
            r6.reportFocusChangedSerialized(true, r10.this$0.mInTouchMode);
            com.android.server.wm.WindowManagerService.access$600(r10.this$0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:411:0x06bd, code lost:
            if (r5 == null) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:413:0x06c1, code lost:
            if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT == false) goto L_0x06d9;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:414:0x06c3, code lost:
            android.util.Slog.i(com.android.server.wm.WindowManagerService.TAG, "Losing focus: " + r5);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:415:0x06d9, code lost:
            r5.reportFocusChangedSerialized(false, r10.this$0.mInTouchMode);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:429:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:471:?, code lost:
            return;
         */
        public void handleMessage(Message msg) {
            ArrayList<WindowState> losers;
            Runnable callback;
            Runnable callback2;
            boolean bootAnimationComplete;
            int i = msg.what;
            boolean z = false;
            boolean z2 = false;
            boolean z3 = false;
            if (i == 2) {
                DisplayContent displayContent = (DisplayContent) msg.obj;
                AccessibilityController accessibilityController = null;
                accessibilityController = null;
                accessibilityController = null;
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (WindowManagerService.this.mAccessibilityController != null && (WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayId() == 0 || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad()))) {
                            accessibilityController = WindowManagerService.this.mAccessibilityController;
                        }
                        WindowState lastFocus = displayContent.mLastFocus;
                        WindowState newFocus = displayContent.mCurrentFocus;
                        if (lastFocus == newFocus) {
                            Slog.i(WindowManagerService.TAG, "Focus is not changing, current focus is " + newFocus);
                            return;
                        }
                        displayContent.mLastFocus = newFocus;
                        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                            Slog.i(WindowManagerService.TAG, "Focus moving from " + lastFocus + " to " + newFocus + " displayId=" + displayContent.getDisplayId());
                        }
                        if (!(newFocus == null || lastFocus == null || newFocus.isDisplayedLw())) {
                            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                                Slog.i(WindowManagerService.TAG, "Delaying loss of focus...");
                            }
                            displayContent.mLosingFocus.add(lastFocus);
                            lastFocus = null;
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 3) {
                DisplayContent displayContent2 = (DisplayContent) msg.obj;
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        losers = displayContent2.mLosingFocus;
                        displayContent2.mLosingFocus = new ArrayList<>();
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                int N = losers.size();
                for (int i2 = 0; i2 < N; i2++) {
                    if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                        Slog.i(WindowManagerService.TAG, "Losing delayed focus: " + losers.get(i2));
                    }
                    losers.get(i2).reportFocusChangedSerialized(false, WindowManagerService.this.mInTouchMode);
                }
            } else if (i == 11) {
                DisplayContent displayContent3 = (DisplayContent) msg.obj;
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (displayContent3.mWaitingForConfig && displayContent3.getRequestedOverrideConfiguration().windowConfiguration.getRotation() == displayContent3.getRotation()) {
                            Slog.i(WindowManagerService.TAG, "not waiting for config if config rotation is updated");
                            displayContent3.mWaitingForConfig = false;
                        }
                        displayContent3.onWindowFreezeTimeout();
                        Slog.w(WindowManagerService.TAG, "Window Freeze TimeOut try stopFreezingDisplayLocked");
                        WindowManagerService.this.stopFreezingDisplayLocked();
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 30) {
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (WindowManagerService.this.mClientFreezingScreen) {
                            WindowManagerService.this.mClientFreezingScreen = false;
                            WindowManagerService.this.mLastFinishedFreezeSource = "client-timeout";
                            WindowManagerService.this.stopFreezingDisplayLocked();
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 41) {
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        DisplayContent displayContent4 = WindowManagerService.this.getDefaultDisplayContentLocked();
                        displayContent4.getDockedDividerController().reevaluateVisibility(false);
                        try {
                            displayContent4.adjustForImeIfNeeded();
                        } catch (IllegalArgumentException e) {
                            Log.e(WindowManagerService.TAG, "catch IllegalArgumentException ");
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 46) {
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        for (int i3 = WindowManagerService.this.mWindowReplacementTimeouts.size() - 1; i3 >= 0; i3--) {
                            WindowManagerService.this.mWindowReplacementTimeouts.get(i3).onWindowReplacementTimeout();
                        }
                        WindowManagerService.this.mWindowReplacementTimeouts.clear();
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 51) {
                int mode = msg.arg1;
                if (mode == 0) {
                    WindowManagerService windowManagerService = WindowManagerService.this;
                    float unused = windowManagerService.mWindowAnimationScaleSetting = Settings.Global.getFloat(windowManagerService.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                } else if (mode == 1) {
                    WindowManagerService windowManagerService2 = WindowManagerService.this;
                    float unused2 = windowManagerService2.mTransitionAnimationScaleSetting = Settings.Global.getFloat(windowManagerService2.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                } else if (mode == 2) {
                    WindowManagerService windowManagerService3 = WindowManagerService.this;
                    float unused3 = windowManagerService3.mAnimatorDurationScaleSetting = Settings.Global.getFloat(windowManagerService3.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                    WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                }
            } else if (i == 52) {
                WindowState window = (WindowState) msg.obj;
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        window.mAttrs.flags &= -129;
                        window.hidePermanentlyLw();
                        window.setDisplayLayoutNeeded();
                        WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 54) {
                DisplayContent displayContent5 = (DisplayContent) msg.obj;
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        displayContent5.onSeamlessRotationTimeout();
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i != 55) {
                switch (i) {
                    case PERSIST_ANIMATION_SCALE /*{ENCODED_INT: 14}*/:
                        Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                        Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                        Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                        return;
                    case FORCE_GC /*{ENCODED_INT: 15}*/:
                        synchronized (WindowManagerService.this.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                if (!WindowManagerService.this.mAnimator.isAnimating()) {
                                    if (!WindowManagerService.this.mAnimator.isAnimationScheduled()) {
                                        if (WindowManagerService.this.mDisplayFrozen) {
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            return;
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        Runtime.getRuntime().gc();
                                        return;
                                    }
                                }
                                sendEmptyMessageDelayed(15, 2000);
                                return;
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                    case ENABLE_SCREEN /*{ENCODED_INT: 16}*/:
                        WindowManagerService.this.performEnableScreen();
                        return;
                    case APP_FREEZE_TIMEOUT /*{ENCODED_INT: 17}*/:
                        synchronized (WindowManagerService.this.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                Slog.w(WindowManagerService.TAG, "App freeze timeout expired.");
                                WindowManagerService.this.mWindowsFreezingScreen = 2;
                                for (int i4 = WindowManagerService.this.mAppFreezeListeners.size() - 1; i4 >= 0; i4--) {
                                    WindowManagerService.this.mAppFreezeListeners.get(i4).onAppFreezeTimeout();
                                }
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        return;
                    case SEND_NEW_CONFIGURATION /*{ENCODED_INT: 18}*/:
                        DisplayContent displayContent6 = (DisplayContent) msg.obj;
                        removeMessages(18, displayContent6);
                        if (displayContent6.isReady()) {
                            WindowManagerService.this.sendNewConfiguration(displayContent6.getDisplayId());
                            return;
                        } else if (WindowManagerDebugConfig.DEBUG_CONFIGURATION) {
                            Slog.w(WindowManagerService.TAG, "Trying to send configuration to " + (displayContent6.getParent() == null ? "detached" : "unready") + " display=" + displayContent6);
                            return;
                        } else {
                            return;
                        }
                    case REPORT_WINDOWS_CHANGE /*{ENCODED_INT: 19}*/:
                        if (WindowManagerService.this.mWindowsChanged) {
                            synchronized (WindowManagerService.this.mGlobalLock) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    WindowManagerService.this.mWindowsChanged = false;
                                } catch (Throwable th2) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th2;
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            WindowManagerService.this.notifyWindowsChanged();
                            return;
                        }
                        return;
                    default:
                        switch (i) {
                            case REPORT_HARD_KEYBOARD_STATUS_CHANGE /*{ENCODED_INT: 22}*/:
                                WindowManagerService.this.notifyHardKeyboardStatusChange();
                                return;
                            case BOOT_TIMEOUT /*{ENCODED_INT: 23}*/:
                                WindowManagerService.this.performBootTimeout();
                                return;
                            case WAITING_FOR_DRAWN_TIMEOUT /*{ENCODED_INT: 24}*/:
                                synchronized (WindowManagerService.this.mGlobalLock) {
                                    try {
                                        WindowManagerService.boostPriorityForLockedSection();
                                        Slog.w(WindowManagerService.TAG, "Timeout waiting for drawn: undrawn=" + WindowManagerService.this.mWaitingForDrawn);
                                        WindowManagerService.this.mWaitingForDrawn.clear();
                                        callback = WindowManagerService.this.mWaitingForDrawnCallback;
                                        WindowManagerService.this.mWaitingForDrawnCallback = null;
                                    } finally {
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                    }
                                }
                                if (callback != null) {
                                    callback.run();
                                    return;
                                }
                                return;
                            case SHOW_STRICT_MODE_VIOLATION /*{ENCODED_INT: 25}*/:
                                WindowManagerService.this.showStrictModeViolation(msg.arg1, msg.arg2);
                                return;
                            default:
                                switch (i) {
                                    case NOTIFY_ACTIVITY_DRAWN /*{ENCODED_INT: 32}*/:
                                        try {
                                            WindowManagerService.this.mActivityTaskManager.notifyActivityDrawn((IBinder) msg.obj);
                                            return;
                                        } catch (RemoteException e2) {
                                            return;
                                        }
                                    case ALL_WINDOWS_DRAWN /*{ENCODED_INT: 33}*/:
                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                            try {
                                                WindowManagerService.boostPriorityForLockedSection();
                                                callback2 = WindowManagerService.this.mWaitingForDrawnCallback;
                                                WindowManagerService.this.mWaitingForDrawnCallback = null;
                                            } finally {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                        if (callback2 != null) {
                                            callback2.run();
                                            return;
                                        }
                                        return;
                                    case NEW_ANIMATOR_SCALE /*{ENCODED_INT: 34}*/:
                                        float scale = WindowManagerService.this.getCurrentAnimatorScale();
                                        ValueAnimator.setDurationScale(scale);
                                        Session session = (Session) msg.obj;
                                        if (session != null) {
                                            try {
                                                session.mCallback.onAnimatorScaleChanged(scale);
                                                return;
                                            } catch (RemoteException e3) {
                                                return;
                                            }
                                        } else {
                                            ArrayList<IWindowSessionCallback> callbacks = new ArrayList<>();
                                            synchronized (WindowManagerService.this.mGlobalLock) {
                                                try {
                                                    WindowManagerService.boostPriorityForLockedSection();
                                                    for (int i5 = 0; i5 < WindowManagerService.this.mSessions.size(); i5++) {
                                                        callbacks.add(WindowManagerService.this.mSessions.valueAt(i5).mCallback);
                                                    }
                                                } catch (Throwable th3) {
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th3;
                                                }
                                            }
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            for (int i6 = 0; i6 < callbacks.size(); i6++) {
                                                try {
                                                    callbacks.get(i6).onAnimatorScaleChanged(scale);
                                                } catch (RemoteException e4) {
                                                }
                                            }
                                            return;
                                        }
                                    case SHOW_CIRCULAR_DISPLAY_MASK /*{ENCODED_INT: 35}*/:
                                        WindowManagerService windowManagerService4 = WindowManagerService.this;
                                        if (msg.arg1 == 1) {
                                            z3 = true;
                                        }
                                        windowManagerService4.showCircularMask(z3);
                                        return;
                                    case SHOW_EMULATOR_DISPLAY_OVERLAY /*{ENCODED_INT: 36}*/:
                                        WindowManagerService.this.showEmulatorDisplayOverlay();
                                        return;
                                    case CHECK_IF_BOOT_ANIMATION_FINISHED /*{ENCODED_INT: 37}*/:
                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                            try {
                                                WindowManagerService.boostPriorityForLockedSection();
                                                bootAnimationComplete = WindowManagerService.this.checkBootAnimationCompleteLocked();
                                            } finally {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                        if (bootAnimationComplete) {
                                            WindowManagerService.this.performEnableScreen();
                                            return;
                                        }
                                        return;
                                    case RESET_ANR_MESSAGE /*{ENCODED_INT: 38}*/:
                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                            try {
                                                WindowManagerService.boostPriorityForLockedSection();
                                                WindowManagerService.this.mLastANRState = null;
                                            } catch (Throwable th4) {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                throw th4;
                                            }
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        WindowManagerService.this.mAtmInternal.clearSavedANRState();
                                        return;
                                    case WALLPAPER_DRAW_PENDING_TIMEOUT /*{ENCODED_INT: 39}*/:
                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                            try {
                                                WindowManagerService.boostPriorityForLockedSection();
                                                WallpaperController wallpaperController = (WallpaperController) msg.obj;
                                                if (wallpaperController != null && wallpaperController.processWallpaperDrawPendingTimeout()) {
                                                    WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                                                }
                                            } finally {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                        return;
                                    default:
                                        switch (i) {
                                            case SET_HAS_OVERLAY_UI /*{ENCODED_INT: 58}*/:
                                                ActivityManagerInternal activityManagerInternal = WindowManagerService.this.mAmInternal;
                                                int i7 = msg.arg1;
                                                if (msg.arg2 == 1) {
                                                    z2 = true;
                                                }
                                                activityManagerInternal.setHasOverlayUi(i7, z2);
                                                return;
                                            case SET_RUNNING_REMOTE_ANIMATION /*{ENCODED_INT: 59}*/:
                                                ActivityManagerInternal activityManagerInternal2 = WindowManagerService.this.mAmInternal;
                                                int i8 = msg.arg1;
                                                if (msg.arg2 == 1) {
                                                    z = true;
                                                }
                                                activityManagerInternal2.setRunningRemoteAnimation(i8, z);
                                                return;
                                            case ANIMATION_FAILSAFE /*{ENCODED_INT: 60}*/:
                                                synchronized (WindowManagerService.this.mGlobalLock) {
                                                    try {
                                                        WindowManagerService.boostPriorityForLockedSection();
                                                        if (WindowManagerService.this.mRecentsAnimationController != null) {
                                                            WindowManagerService.this.mRecentsAnimationController.scheduleFailsafe();
                                                        }
                                                    } finally {
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                return;
                                            case RECOMPUTE_FOCUS /*{ENCODED_INT: 61}*/:
                                                synchronized (WindowManagerService.this.mGlobalLock) {
                                                    try {
                                                        WindowManagerService.boostPriorityForLockedSection();
                                                        WindowManagerService.this.updateFocusedWindowLocked(0, true);
                                                    } finally {
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                return;
                                            case ON_POINTER_DOWN_OUTSIDE_FOCUS /*{ENCODED_INT: 62}*/:
                                                synchronized (WindowManagerService.this.mGlobalLock) {
                                                    try {
                                                        WindowManagerService.boostPriorityForLockedSection();
                                                        WindowManagerService.this.onPointerDownOutsideFocusLocked((IBinder) msg.obj);
                                                    } finally {
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                return;
                                            default:
                                                switch (i) {
                                                    case PC_FREEZE_TIMEOUT /*{ENCODED_INT: 103}*/:
                                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                                            try {
                                                                WindowManagerService.boostPriorityForLockedSection();
                                                                Slog.w(WindowManagerService.TAG, "stopFreezingDisplayLocked  by PC_FREEZE_TIMEOUT");
                                                                WindowManagerService.this.stopFreezingDisplayLocked();
                                                            } finally {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                            }
                                                        }
                                                        return;
                                                    case SET_FOCUSED_TASK /*{ENCODED_INT: 104}*/:
                                                        try {
                                                            WindowManagerService.this.mActivityTaskManager.setFocusedTask(msg.arg1);
                                                            return;
                                                        } catch (RemoteException e5) {
                                                            Log.e(WindowManagerService.TAG, "setFocusedDisplay()");
                                                            return;
                                                        }
                                                    case SPLIT_RESIZING_TIMEOUT /*{ENCODED_INT: 105}*/:
                                                        WindowManagerService windowManagerService5 = WindowManagerService.this;
                                                        windowManagerService5.mResizing = false;
                                                        synchronized (windowManagerService5.mGlobalLock) {
                                                            try {
                                                                WindowManagerService.boostPriorityForLockedSection();
                                                                WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().setResizing(false);
                                                            } finally {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                            }
                                                        }
                                                        return;
                                                    case GESTURE_NAVUP_TIMEOUT /*{ENCODED_INT: 106}*/:
                                                        if (WindowManagerService.this.getWallpaperWindowState() != null) {
                                                            WindowManagerService.this.mWallpaperWindow.destroyInsetSurfaceImmediately(true);
                                                            int unused4 = WindowManagerService.this.mAddValue = -1;
                                                            return;
                                                        }
                                                        return;
                                                    case UNFREEZE_FOLD_ROTATION /*{ENCODED_INT: 107}*/:
                                                        WindowManagerService.this.thawRotation();
                                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                                            try {
                                                                WindowManagerService.boostPriorityForLockedSection();
                                                                WindowManagerService.this.setFoldRotationFreezed(false);
                                                                Slog.i(WindowManagerService.TAG, "handle unfreeze fold rotation msg!");
                                                            } finally {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                            }
                                                        }
                                                        return;
                                                    case ENABLE_EVENT_DISPATCHING /*{ENCODED_INT: 108}*/:
                                                        synchronized (WindowManagerService.this.mGlobalLock) {
                                                            try {
                                                                WindowManagerService.boostPriorityForLockedSection();
                                                                boolean unused5 = WindowManagerService.this.mIsFoldDisableEventDispatching = false;
                                                                WindowManagerService.this.mInputManagerCallback.setEventDispatchingLw(true);
                                                            } finally {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                            }
                                                        }
                                                        return;
                                                    default:
                                                        return;
                                                }
                                        }
                                }
                        }
                }
            } else {
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowManagerService.this.restorePointerIconLocked((DisplayContent) msg.obj, (float) msg.arg1, (float) msg.arg2);
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void sendNewMessageDelayed(int what, Object obj, long delayMillis) {
            removeMessages(what, obj);
            sendMessageDelayed(obtainMessage(what, obj), delayMillis);
        }
    }

    /* access modifiers changed from: package-private */
    public void destroyPreservedSurfaceLocked() {
        for (int i = this.mDestroyPreservedSurface.size() - 1; i >= 0; i--) {
            this.mDestroyPreservedSurface.get(i).mWinAnimator.destroyPreservedSurfaceLocked();
        }
        this.mDestroyPreservedSurface.clear();
    }

    public IWindowSession openSession(IWindowSessionCallback callback) {
        return new Session(this, callback);
    }

    public void getInitialDisplaySize(int displayId, Point size) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    size.x = displayContent.mInitialDisplayWidth;
                    size.y = displayContent.mInitialDisplayHeight;
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void getBaseDisplaySize(int displayId, Point size) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    size.x = displayContent.mBaseDisplayWidth;
                    size.y = displayContent.mBaseDisplayHeight;
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public void setForcedDisplaySize(int displayId, int width, int height) {
        int foldDisplayMode;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                        if (HwFoldScreenState.isFoldScreenDevice() && (foldDisplayMode = this.mDisplayManagerInternal.getDisplayMode()) != this.mCurrentFoldDisplayMode) {
                            this.mCurrentFoldDisplayMode = foldDisplayMode;
                            if (displayContent != null) {
                                displayContent.getDisplayRotation().setDisplayModeChange(true);
                            }
                            this.mTaskSnapshotController.clearSnapshot();
                            if (getLazyMode() != 0 && (this.mCurrentFoldDisplayMode == 1 || this.mCurrentFoldDisplayMode == 3 || this.mCurrentFoldDisplayMode == 4)) {
                                Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
                            }
                            if (this.mIsFoldDisableEventDispatching && this.mCurrentFoldDisplayMode != 4) {
                                this.mH.removeMessages(H.ENABLE_EVENT_DISPATCHING);
                                this.mInputManagerCallback.setEventDispatchingLw(true);
                                this.mIsFoldDisableEventDispatching = false;
                            }
                        }
                        if (displayContent != null) {
                            displayContent.setForcedSize(width, height);
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    /* JADX INFO: finally extract failed */
    public void setForcedDisplayScalingMode(int displayId, int mode) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                        if (displayContent != null) {
                            displayContent.setForcedScalingMode(mode);
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    private boolean applyForcedPropertiesForDefaultDisplay() {
        int pos;
        boolean changed = false;
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        String sizeStr = Settings.Global.getString(this.mContext.getContentResolver(), "display_size_forced");
        if (sizeStr == null || sizeStr.length() == 0) {
            sizeStr = SystemProperties.get(SIZE_OVERRIDE, (String) null);
        }
        boolean z = false;
        if (sizeStr != null && sizeStr.length() > 0 && (pos = sizeStr.indexOf(44)) > 0 && sizeStr.lastIndexOf(44) == pos) {
            try {
                int width = Integer.parseInt(sizeStr.substring(0, pos));
                int height = Integer.parseInt(sizeStr.substring(pos + 1));
                if (!(displayContent.mBaseDisplayWidth == width && displayContent.mBaseDisplayHeight == height)) {
                    Slog.i(TAG, "FORCED DISPLAY SIZE: " + width + "x" + height);
                    displayContent.updateBaseDisplayMetrics(width, height, displayContent.mBaseDisplayDensity);
                    changed = true;
                }
            } catch (NumberFormatException e) {
            }
        }
        int density = getForcedDisplayDensityForUserLocked(this.mCurrentUserId);
        if (!(density == 0 || density == displayContent.mBaseDisplayDensity)) {
            displayContent.mBaseDisplayDensity = density;
            changed = true;
        }
        int mode = Settings.Global.getInt(this.mContext.getContentResolver(), "display_scaling_force", 0);
        boolean z2 = displayContent.mDisplayScalingDisabled;
        if (mode != 0) {
            z = true;
        }
        if (z2 == z) {
            return changed;
        }
        Slog.i(TAG, "FORCED DISPLAY SCALING DISABLED");
        displayContent.mDisplayScalingDisabled = true;
        return true;
    }

    public void resetFoldScreenInfo() {
        if (this.mFoldScreenInfo != null) {
            Flog.d(310, "debug fold screen info, set isFold false when setForcedDisplaySizeLocked!");
            this.mFoldScreenInfo.putBoolean(IS_FOLD_KEY, false);
        }
    }

    /* JADX INFO: finally extract failed */
    public void clearForcedDisplaySize(int displayId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                        if (displayContent != null) {
                            displayContent.setForcedSize(displayContent.mInitialDisplayWidth, displayContent.mInitialDisplayHeight);
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    public int getInitialDisplayDensity(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null || !displayContent.hasAccess(Binder.getCallingUid())) {
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                return displayContent.mInitialDisplayDensity;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public int getBaseDisplayDensity(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null || !displayContent.hasAccess(Binder.getCallingUid())) {
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                return displayContent.mBaseDisplayDensity;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public void setForcedDisplayDensityForUser(int displayId, int density, int userId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            int targetUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "setForcedDisplayDensityForUser", null);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                        if (displayContent != null) {
                            displayContent.setForcedDensity(density, targetUserId);
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    /* JADX INFO: finally extract failed */
    public void clearForcedDisplayDensityForUser(int displayId, int userId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            int callingUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearForcedDisplayDensityForUser", null);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                        if (displayContent != null) {
                            int curWidth = SystemProperties.getInt("persist.sys.rog.width", 0);
                            if (curWidth > 0) {
                                displayContent.setForcedDensity((SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)) * curWidth) / displayContent.mInitialDisplayWidth, callingUserId);
                            } else {
                                displayContent.setForcedDensity(displayContent.mInitialDisplayDensity, callingUserId);
                            }
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
    }

    private int getForcedDisplayDensityForUserLocked(int userId) {
        String densityStr = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", userId);
        if (densityStr == null || densityStr.length() == 0) {
            densityStr = SystemProperties.get(DENSITY_OVERRIDE, (String) null);
        }
        if (densityStr == null || densityStr.length() <= 0) {
            return 0;
        }
        try {
            return Integer.parseInt(densityStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /* access modifiers changed from: package-private */
    public void reconfigureDisplayLocked(DisplayContent displayContent) {
        String str;
        Bundle bundle;
        if (displayContent.isReady()) {
            displayContent.configureDisplayPolicy();
            displayContent.setLayoutNeeded();
            boolean configChanged = displayContent.updateOrientationFromAppTokens();
            Configuration currentDisplayConfig = displayContent.getConfiguration();
            this.mTempConfiguration.setTo(currentDisplayConfig);
            displayContent.computeScreenConfiguration(this.mTempConfiguration);
            boolean configChanged2 = configChanged | (currentDisplayConfig.diff(this.mTempConfiguration) != 0);
            if (configChanged2 || ((bundle = this.mFoldScreenInfo) != null && bundle.getBoolean(IS_FOLD_KEY))) {
                StringBuilder sb = new StringBuilder();
                sb.append("debug reconfigureDisplayLocked: configChanged = ");
                sb.append(configChanged2);
                sb.append(", mFoldScreenInfo = ");
                sb.append(this.mFoldScreenInfo);
                if (this.mFoldScreenInfo == null) {
                    str = "";
                } else {
                    str = ", isFold = " + this.mFoldScreenInfo.getBoolean(IS_FOLD_KEY);
                }
                sb.append(str);
                Flog.d(310, sb.toString());
                displayContent.mWaitingForConfig = true;
                startFreezingDisplayLocked(0, 0, displayContent);
                this.mH.obtainMessage(18, displayContent).sendToTarget();
            }
            this.mWindowPlacerLocked.performSurfacePlacement();
            CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
            if (utils.getCoordinationState() == 1) {
                this.mH.post(new Runnable() {
                    /* class com.android.server.wm.WindowManagerService.AnonymousClass11 */

                    public void run() {
                        WindowManagerService.this.mAtmInternal.enterCoordinationMode();
                    }
                });
            }
            if (utils.getCoordinationState() == 3) {
                this.mH.post(new Runnable() {
                    /* class com.android.server.wm.WindowManagerService.AnonymousClass12 */

                    public void run() {
                        WindowManagerService.this.mAtmInternal.exitCoordinationMode();
                    }
                });
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public void setOverscan(int displayId, int left, int top, int right, int bottom) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                        if (displayContent != null) {
                            setOverscanLocked(displayContent, left, top, right, bottom);
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
            } finally {
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
        this.mDisplayWindowSettings.setOverscanLocked(displayInfo, left, top, right, bottom);
        reconfigureDisplayLocked(displayContent);
    }

    public void startWindowTrace() {
        this.mWindowTracing.startTrace(null);
    }

    public void stopWindowTrace() {
        this.mWindowTracing.stopTrace(null);
    }

    public boolean isWindowTraceEnabled() {
        return this.mWindowTracing.isEnabled();
    }

    /* access modifiers changed from: package-private */
    public final WindowState windowForClientLocked(Session session, IWindow client, boolean throwOnError) {
        return windowForClientLocked(session, client.asBinder(), throwOnError);
    }

    /* access modifiers changed from: package-private */
    public final WindowState windowForClientLocked(Session session, IBinder client, boolean throwOnError) {
        WindowState win = (WindowState) this.mWindowMap.get(client);
        if (win == null) {
            if (!throwOnError) {
                Slog.w(TAG, "Failed looking up window callers=" + Debug.getCallers(3));
                return null;
            }
            throw new IllegalArgumentException("Requested window " + client + " does not exist");
        } else if (session == null || win.mSession == session) {
            return win;
        } else {
            if (!throwOnError) {
                Slog.w(TAG, "Failed looking up window callers=" + Debug.getCallers(3));
                return null;
            }
            throw new IllegalArgumentException("Requested window " + client + " is in session " + win.mSession + ", not " + session);
        }
    }

    /* access modifiers changed from: package-private */
    public void makeWindowFreezingScreenIfNeededLocked(WindowState w) {
        if (!w.mToken.okToDisplay() && this.mWindowsFreezingScreen != 2) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.v(TAG, "Changing surface while display frozen: " + w);
            }
            w.setOrientationChanging(true);
            w.mLastFreezeDuration = 0;
            this.mRoot.mOrientationChangeComplete = false;
            if (this.mWindowsFreezingScreen == 0) {
                this.mWindowsFreezingScreen = 1;
                this.mH.sendNewMessageDelayed(11, w.getDisplayContent(), 2000);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void checkDrawnWindowsLocked() {
        if (!this.mWaitingForDrawn.isEmpty() && this.mWaitingForDrawnCallback != null) {
            for (int j = this.mWaitingForDrawn.size() - 1; j >= 0; j--) {
                WindowState win = this.mWaitingForDrawn.get(j);
                Flog.i(603, "UL_Power Waiting for drawn " + win + ": removed=" + win.mRemoved + " visible=" + win.isVisibleLw() + " mHasSurface=" + win.mHasSurface + " drawState=" + win.mWinAnimator.mDrawState);
                if (win.mRemoved || !win.mHasSurface || !win.isVisibleByPolicy()) {
                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                        Slog.w(TAG, "Aborted waiting for drawn: " + win);
                    }
                    this.mWaitingForDrawn.remove(win);
                } else if (win.hasDrawnLw()) {
                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                        Slog.d(TAG, "Window drawn win=" + win);
                    }
                    this.mWaitingForDrawn.remove(win);
                }
            }
            if (this.mWaitingForDrawn.isEmpty()) {
                this.mH.removeMessages(24);
                Runnable callback = this.mWaitingForDrawnCallback;
                this.mWaitingForDrawnCallback = null;
                Flog.i(307, "All windows drawn!");
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setHoldScreenLocked(Session newHoldScreen) {
        boolean hold = newHoldScreen != null;
        if (hold && this.mHoldingScreenOn != newHoldScreen) {
            this.mHoldingScreenWakeLock.setWorkSource(new WorkSource(newHoldScreen.mUid));
        }
        if (this.mHoldingScreenOn != newHoldScreen) {
            Slog.i("DebugKeepScreenOn", "setHoldScreenLocked newHoldScreen:" + newHoldScreen + " currentHoldScreen:" + this.mHoldingScreenOn + " wakeLockState:" + this.mHoldingScreenWakeLock.isHeld());
        }
        this.mHoldingScreenOn = newHoldScreen;
        if (hold == this.mHoldingScreenWakeLock.isHeld()) {
            return;
        }
        if (hold) {
            Slog.i("DebugKeepScreenOn", "Acquiring screen wakelock due to " + this.mRoot.mHoldScreenWindow);
            this.mLastWakeLockHoldingWindow = this.mRoot.mHoldScreenWindow;
            int displayId = this.mLastWakeLockHoldingWindow.getDisplayId();
            this.mLastWakeLockObscuringWindow = null;
            if (HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayId)) {
                this.mHoldingScreenWakeLock.acquire();
            } else {
                this.mPCHoldingScreenWakeLock.acquire();
            }
            this.mPolicy.keepScreenOnStartedLw();
            return;
        }
        Slog.i("DebugKeepScreenOn", "Releasing screen wakelock, obscured by " + this.mRoot.mObscuringWindow);
        this.mLastWakeLockHoldingWindow = null;
        this.mLastWakeLockObscuringWindow = this.mRoot.mObscuringWindow;
        this.mPolicy.keepScreenOnStoppedLw();
        this.mHoldingScreenWakeLock.release();
        this.mPCHoldingScreenWakeLock.release();
    }

    /* access modifiers changed from: package-private */
    public void requestTraversal() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.requestTraversal();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void scheduleAnimationLocked() {
        WindowAnimator windowAnimator = this.mAnimator;
        if (windowAnimator != null) {
            windowAnimator.scheduleAnimation();
        }
    }

    public void setRtgSchedSwitch(boolean enable) {
        this.mRtgSchedSwitch = enable;
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        Trace.traceBegin(32, "wmUpdateFocus");
        if (this.mNotifyFocusedWindow) {
            IWindow currentWindow = null;
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    Iterator<Map.Entry<IBinder, WindowState>> it = this.mWindowMap.entrySet().iterator();
                    if (it.hasNext()) {
                        currentWindow = IWindow.Stub.asInterface(it.next().getKey());
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
            Slog.i(TAG, "currentWindow = " + currentWindow);
            if (currentWindow != null) {
                try {
                    currentWindow.notifyFocusChanged();
                } catch (RemoteException e) {
                    Slog.w(TAG, "currentWindow.notifyFocusChanged() get RemoteException!");
                }
            }
        }
        boolean changed = this.mRoot.updateFocusedWindowLocked(mode, updateInputWindows);
        Trace.traceEnd(32);
        DisplayPolicy displayPolicy = getDefaultDisplayContentLocked().getDisplayPolicy();
        if (displayPolicy != null && displayPolicy.isLeftRightSplitStackVisible()) {
            displayPolicy.setFocusChangeIMEFrozenTag(changed);
        }
        return changed;
    }

    /* access modifiers changed from: package-private */
    public void startFreezingDisplayLocked(int exitAnim, int enterAnim) {
        startFreezingDisplayLocked(exitAnim, enterAnim, getDefaultDisplayContentLocked());
    }

    /* access modifiers changed from: package-private */
    public void startFreezingDisplayLocked(int exitAnim, int enterAnim, DisplayContent displayContent) {
        ScreenRotationAnimation screenRotationAnimation;
        if (!this.mDisplayFrozen && !this.mRotatingSeamlessly) {
            if (!displayContent.isReady() || !this.mPolicy.isScreenOn() || !displayContent.okToAnimate() || (HwFoldScreenState.isFoldScreenDevice() && this.mPolicy.isScreenOnExBlocking())) {
                handleResumeDispModeChange();
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d(TAG, "startFreezingDisplayLocked: exitAnim=" + exitAnim + " enterAnim=" + enterAnim + " called by " + Debug.getCallers(8));
            }
            this.mScreenFrozenLock.acquire();
            this.mDisplayFrozen = true;
            this.mDisplayFreezeTime = SystemClock.elapsedRealtime();
            this.mLastFinishedFreezeSource = null;
            this.mFrozenDisplayId = displayContent.getDisplayId();
            this.mInputManagerCallback.freezeInputDispatchingLw();
            if (displayContent.mAppTransition.isTransitionSet()) {
                displayContent.mAppTransition.freeze();
            }
            this.mLatencyTracker.onActionStart(6);
            this.mExitAnimId = exitAnim;
            this.mEnterAnimId = enterAnim;
            ScreenRotationAnimation screenRotationAnimation2 = this.mAnimator.getScreenRotationAnimationLocked(this.mFrozenDisplayId);
            if (screenRotationAnimation2 != null) {
                screenRotationAnimation2.kill();
            }
            boolean isSecure = displayContent.hasSecureWindowOnScreen();
            displayContent.updateDisplayInfo();
            if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayContent.getDisplayId())) {
                IHwScreenRotationAnimation hwSRA = HwServiceFactory.getHwScreenRotationAnimation();
                if (hwSRA != null) {
                    screenRotationAnimation = hwSRA.create(this.mContext, displayContent, displayContent.getDisplayRotation().isFixedToUserRotation(), isSecure, this);
                } else {
                    screenRotationAnimation = new ScreenRotationAnimation(this.mContext, displayContent, displayContent.getDisplayRotation().isFixedToUserRotation(), isSecure, this);
                }
                Bundle bundle = this.mFoldScreenInfo;
                if (bundle != null) {
                    screenRotationAnimation.setAnimationTypeInfo(bundle);
                    this.mFoldScreenInfo.putBoolean(IS_FOLD_KEY, false);
                }
                this.mAnimator.setScreenRotationAnimationLocked(this.mFrozenDisplayId, screenRotationAnimation);
                return;
            }
            this.mH.removeMessages(H.PC_FREEZE_TIMEOUT);
            this.mH.sendEmptyMessageDelayed(H.PC_FREEZE_TIMEOUT, 3000);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r12v0, resolved type: android.os.Bundle */
    /* JADX DEBUG: Multi-variable search result rejected for r12v2, resolved type: com.android.server.wm.ScreenRotationAnimation */
    /* JADX DEBUG: Multi-variable search result rejected for r12v3, resolved type: com.android.server.wm.ScreenRotationAnimation */
    /* JADX DEBUG: Multi-variable search result rejected for r12v4, resolved type: com.android.server.wm.ScreenRotationAnimation */
    /* JADX DEBUG: Multi-variable search result rejected for r12v8, resolved type: android.os.Bundle */
    /* JADX DEBUG: Multi-variable search result rejected for r12v9, resolved type: android.os.Bundle */
    /* JADX DEBUG: Multi-variable search result rejected for r12v12, resolved type: android.os.Bundle */
    /* JADX WARN: Multi-variable type inference failed */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0175  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x018a  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x01a7  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x01b3  */
    public void stopFreezingDisplayLocked() {
        Bundle bundle;
        boolean configChanged;
        ScreenRotationAnimation screenRotationAnimation;
        float transitionAnimScale;
        if (this.mDisplayFrozen) {
            DisplayContent displayContent = this.mRoot.getDisplayContent(this.mFrozenDisplayId);
            boolean waitingForConfig = displayContent != null && displayContent.mWaitingForConfig;
            int numOpeningApps = displayContent != null ? displayContent.mOpeningApps.size() : 0;
            if (waitingForConfig || this.mAppsFreezingScreen > 0 || this.mWindowsFreezingScreen == 1 || this.mClientFreezingScreen || numOpeningApps > 0) {
                Slog.i(TAG, "stopFreezingDisplayLocked: Returning mWaitingForConfig=" + waitingForConfig + ", mAppsFreezingScreen=" + this.mAppsFreezingScreen + ", mWindowsFreezingScreen=" + this.mWindowsFreezingScreen + ", mClientFreezingScreen=" + this.mClientFreezingScreen + ", mOpeningApps.size()=" + numOpeningApps);
                return;
            }
            Slog.i(TAG, "stopFreezingDisplayLocked: Unfreezing now");
            if (HwPCUtils.isPcCastModeInServer()) {
                this.mH.removeMessages(H.PC_FREEZE_TIMEOUT);
            }
            int displayId = this.mFrozenDisplayId;
            this.mFrozenDisplayId = -1;
            this.mDisplayFrozen = false;
            this.mInputManagerCallback.thawInputDispatchingLw();
            this.mLastDisplayFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
            StringBuilder sb = new StringBuilder(128);
            sb.append("Screen frozen for ");
            TimeUtils.formatDuration((long) this.mLastDisplayFreezeDuration, sb);
            if (this.mLastFinishedFreezeSource != null) {
                sb.append(" due to ");
                sb.append(this.mLastFinishedFreezeSource);
            }
            Slog.i(TAG, sb.toString());
            this.mH.removeMessages(17);
            this.mH.removeMessages(30);
            boolean updateRotation = false;
            ScreenRotationAnimation screenRotationAnimation2 = this.mAnimator.getScreenRotationAnimationLocked(displayId);
            if (screenRotationAnimation2 == null) {
                screenRotationAnimation = 0;
            } else if (screenRotationAnimation2.hasScreenshot()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.i(TAG, "**** Dismissing screen rotation animation");
                }
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                if (!displayContent.getDisplayPolicy().validateRotationAnimationLw(this.mExitAnimId, this.mEnterAnimId, false)) {
                    this.mEnterAnimId = 0;
                    this.mExitAnimId = 0;
                }
                Bundle animTypeInfo = screenRotationAnimation2.getAnimationTypeInfo();
                boolean isToDismissFoldAnim = animTypeInfo != null && animTypeInfo.getBoolean(IS_FOLD_KEY);
                float transitionAnimScale2 = getTransitionAnimationScaleLocked();
                Flog.d(12345, "transitionAnimScale=" + transitionAnimScale2 + "isToDismissFoldAnim=" + isToDismissFoldAnim);
                if (!isToDismissFoldAnim || Math.abs(transitionAnimScale2 - 0.0f) >= 1.0E-6f) {
                    transitionAnimScale = transitionAnimScale2;
                } else {
                    transitionAnimScale = 1.0f;
                }
                if (screenRotationAnimation2.dismiss(this.mTransaction, 10000, transitionAnimScale, displayInfo.logicalWidth, displayInfo.logicalHeight, this.mExitAnimId, this.mEnterAnimId)) {
                    if (isToDismissFoldAnim) {
                        Flog.d(310, "setDisplayModeChangeDelay is called!");
                        this.mDisplayManagerInternal.setDisplayModeChangeDelay(this.mTransaction, screenRotationAnimation2.getExitAnimDuration());
                    }
                    this.mTransaction.apply();
                    scheduleAnimationLocked();
                    bundle = null;
                } else {
                    screenRotationAnimation2.kill();
                    bundle = null;
                    this.mAnimator.setScreenRotationAnimationLocked(displayId, null);
                    updateRotation = true;
                }
                if (this.mFoldScreenInfo != null) {
                    this.mFoldScreenInfo = bundle;
                }
                configChanged = displayContent == null && displayContent.updateOrientationFromAppTokens();
                if (!ZygoteInit.sIsMygote) {
                    this.mH.removeMessages(15);
                    this.mH.sendEmptyMessageDelayed(15, 2000);
                }
                this.mScreenFrozenLock.release();
                if (updateRotation && displayContent != null && updateRotation) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.d(TAG, "Performing post-rotate rotation");
                    }
                    configChanged |= displayContent.updateRotationUnchecked();
                }
                if (configChanged) {
                    displayContent.sendNewConfiguration();
                }
                this.mLatencyTracker.onActionEnd(6);
            } else {
                screenRotationAnimation = 0;
            }
            if (screenRotationAnimation2 != null) {
                screenRotationAnimation2.kill();
                this.mAnimator.setScreenRotationAnimationLocked(displayId, screenRotationAnimation);
            }
            updateRotation = true;
            bundle = screenRotationAnimation;
            if (this.mFoldScreenInfo != null) {
            }
            configChanged = displayContent == null && displayContent.updateOrientationFromAppTokens();
            if (!ZygoteInit.sIsMygote) {
            }
            this.mScreenFrozenLock.release();
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            }
            configChanged |= displayContent.updateRotationUnchecked();
            if (configChanged) {
            }
            this.mLatencyTracker.onActionEnd(6);
        }
    }

    static int getPropertyInt(String[] tokens, int index, int defUnits, int defDps, DisplayMetrics dm) {
        String str;
        if (index < tokens.length && (str = tokens[index]) != null && str.length() > 0) {
            try {
                return Integer.parseInt(str);
            } catch (Exception e) {
            }
        }
        if (defUnits == 0) {
            return defDps;
        }
        return (int) TypedValue.applyDimension(defUnits, (float) defDps, dm);
    }

    /* access modifiers changed from: package-private */
    public void createWatermarkInTransaction() {
        String[] toks;
        if (this.mWatermark == null) {
            FileInputStream in = null;
            DataInputStream ind = null;
            try {
                DataInputStream ind2 = new DataInputStream(new FileInputStream(new File("/system/etc/setup.conf")));
                String line = ind2.readLine();
                if (!(line == null || (toks = line.split("%")) == null || toks.length <= 0)) {
                    DisplayContent displayContent = getDefaultDisplayContentLocked();
                    this.mWatermark = new Watermark(displayContent, displayContent.mRealDisplayMetrics, toks);
                }
                try {
                    ind2.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e2) {
                if (0 != 0) {
                    ind.close();
                } else if (0 != 0) {
                    in.close();
                }
            } catch (IOException e3) {
                if (0 != 0) {
                    ind.close();
                } else if (0 != 0) {
                    try {
                        in.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        ind.close();
                    } catch (IOException e5) {
                    }
                } else if (0 != 0) {
                    try {
                        in.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        }
    }

    public void setRecentsVisibility(boolean visible) {
        this.mAtmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.STATUS_BAR", "setRecentsVisibility()");
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setRecentsVisibilityLw(visible);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setPipVisibility(boolean visible) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mPolicy.setPipVisibilityLw(visible);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    public void setShelfHeight(boolean visible, int shelfHeight) {
        this.mAtmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.STATUS_BAR", "setShelfHeight()");
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getPinnedStackController().setAdjustedForShelf(visible, shelfHeight);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void statusBarVisibilityChanged(int displayId, int visibility) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null) {
                        displayContent.statusBarVisibilityChanged(visibility);
                    } else {
                        Slog.w(TAG, "statusBarVisibilityChanged with invalid displayId=" + displayId);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    public void setForceShowSystemBars(boolean show) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.forAllDisplayPolicies(PooledLambda.obtainConsumer($$Lambda$XcHmyRxMY5ULhjLiVsIKnPtvOM.INSTANCE, PooledLambda.__(), Boolean.valueOf(show)));
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean enabled) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") == 0) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mPolicy.setNavBarVirtualKeyHapticFeedbackEnabledLw(enabled);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
    }

    public int getNavBarPosition(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    Slog.w(TAG, "getNavBarPosition with invalid displayId=" + displayId + " callers=" + Debug.getCallers(3));
                    return -1;
                }
                displayContent.performLayout(false, false);
                int navBarPosition = displayContent.getDisplayPolicy().getNavBarPosition();
                resetPriorityAfterLockedSection();
                return navBarPosition;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public WindowManagerPolicy.InputConsumer createInputConsumer(Looper looper, String name, InputEventReceiver.Factory inputEventReceiverFactory, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    return displayContent.getInputMonitor().createInputConsumer(looper, name, inputEventReceiverFactory);
                }
                resetPriorityAfterLockedSection();
                return null;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void createInputConsumer(IBinder token, String name, int displayId, InputChannel inputChannel) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display != null) {
                    display.getInputMonitor().createInputConsumer(token, name, inputChannel, Binder.getCallingPid(), Binder.getCallingUserHandle());
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean destroyInputConsumer(String name, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display != null) {
                    return display.getInputMonitor().destroyInputConsumer(name);
                }
                resetPriorityAfterLockedSection();
                return false;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public Region getCurrentImeTouchRegion() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.RESTRICTED_VR_ACCESS") == 0) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    Region r = new Region();
                    for (int i = this.mRoot.mChildren.size() - 1; i >= 0; i--) {
                        DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
                        if (displayContent.mInputMethodWindow != null) {
                            displayContent.mInputMethodWindow.getTouchableRegion(r);
                            return r;
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return r;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("getCurrentImeTouchRegion is restricted to VR services");
        }
    }

    public boolean hasNavigationBar(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    return false;
                }
                boolean hasNavigationBar = dc.getDisplayPolicy().hasNavigationBar();
                resetPriorityAfterLockedSection();
                return hasNavigationBar;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void lockNow(Bundle options) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.WINDOWNMANAGER_LOCKNOW);
        this.mPolicy.lockNow(options);
    }

    public void showRecentApps() {
        this.mPolicy.showRecentApps();
    }

    public boolean isSafeModeEnabled() {
        return this.mSafeMode;
    }

    public boolean clearWindowContentFrameStats(IBinder token) {
        if (checkCallingPermission("android.permission.FRAME_STATS", "clearWindowContentFrameStats()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) this.mWindowMap.get(token);
                    if (windowState == null) {
                        return false;
                    }
                    WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
                    if (surfaceController == null) {
                        resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean clearWindowContentFrameStats = surfaceController.clearWindowContentFrameStats();
                    resetPriorityAfterLockedSection();
                    return clearWindowContentFrameStats;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
    }

    public WindowContentFrameStats getWindowContentFrameStats(IBinder token) {
        if (checkCallingPermission("android.permission.FRAME_STATS", "getWindowContentFrameStats()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) this.mWindowMap.get(token);
                    if (windowState == null) {
                        return null;
                    }
                    WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
                    if (surfaceController == null) {
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                    if (this.mTempWindowRenderStats == null) {
                        this.mTempWindowRenderStats = new WindowContentFrameStats();
                    }
                    WindowContentFrameStats stats = this.mTempWindowRenderStats;
                    if (!surfaceController.getWindowContentFrameStats(stats)) {
                        resetPriorityAfterLockedSection();
                        return null;
                    }
                    resetPriorityAfterLockedSection();
                    return stats;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
    }

    public void notifyAppRelaunching(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.startRelaunching();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppRelaunchingFinished(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.finishRelaunching();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppRelaunchesCleared(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.clearRelaunching();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppResumedFinished(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindow = this.mRoot.getAppWindowToken(token);
                if (appWindow != null) {
                    appWindow.getDisplayContent().mUnknownAppVisibilityController.notifyAppResumedFinished(appWindow);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyTaskRemovedFromRecents(int taskId, int userId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.notifyTaskRemovedFromRecents(taskId, userId);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
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
    }

    private void dumpTraceStatus(PrintWriter pw) {
        pw.println("WINDOW MANAGER TRACE (dumpsys window trace)");
        pw.print(this.mWindowTracing.getStatus() + "\n");
    }

    private void dumpSessionsLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER SESSIONS (dumpsys window sessions)");
        for (int i = 0; i < this.mSessions.size(); i++) {
            Session s = this.mSessions.valueAt(i);
            pw.print("  Session ");
            pw.print(s);
            pw.println(':');
            s.dump(pw, "    ");
        }
    }

    /* access modifiers changed from: package-private */
    public void writeToProtoLocked(ProtoOutputStream proto, int logLevel) {
        this.mPolicy.writeToProto(proto, 1146756268033L);
        this.mRoot.writeToProto(proto, 1146756268034L, logLevel);
        DisplayContent topFocusedDisplayContent = this.mRoot.getTopFocusedDisplayContent();
        if (topFocusedDisplayContent.mCurrentFocus != null) {
            topFocusedDisplayContent.mCurrentFocus.writeIdentifierToProto(proto, 1146756268035L);
        }
        if (topFocusedDisplayContent.mFocusedApp != null) {
            topFocusedDisplayContent.mFocusedApp.writeNameToProto(proto, 1138166333444L);
        }
        WindowState imeWindow = this.mRoot.getCurrentInputMethodWindow();
        if (imeWindow != null) {
            imeWindow.writeIdentifierToProto(proto, 1146756268037L);
        }
        proto.write(1133871366150L, this.mDisplayFrozen);
        DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
        proto.write(1120986464263L, defaultDisplayContent.getRotation());
        proto.write(1120986464264L, defaultDisplayContent.getLastOrientation());
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
            pw.println("Set DEBUG_ORIENTATION, DEBUG_APP_ORIENTATION flag on");
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
            pw.println("Set DEBUG_DISPLAY flag on");
        } else if (cmd.equals(DEBUG_STARTING_WINDOW_CMD)) {
            WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE = true;
            WindowManagerDebugConfig.DEBUG_STARTING_WINDOW = true;
            pw.println("Set DEBUG_STARTING_WINDOW flag on");
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
            WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE = true;
            WindowManagerDebugConfig.DEBUG_STARTING_WINDOW = true;
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
            WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE = false;
            WindowManagerDebugConfig.DEBUG_STARTING_WINDOW = false;
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
        this.mRoot.dumpWindowsNoHeader(pw, dumpAll, windows);
        if (!this.mHidingNonSystemOverlayWindows.isEmpty()) {
            pw.println();
            pw.println("  Hiding System Alert Windows:");
            for (int i = this.mHidingNonSystemOverlayWindows.size() - 1; i >= 0; i--) {
                WindowState w = this.mHidingNonSystemOverlayWindows.get(i);
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
            for (int i2 = this.mPendingRemove.size() - 1; i2 >= 0; i2--) {
                WindowState w2 = this.mPendingRemove.get(i2);
                if (windows == null || windows.contains(w2)) {
                    pw.print("  Remove #");
                    pw.print(i2);
                    pw.print(' ');
                    pw.print(w2);
                    if (dumpAll) {
                        pw.println(":");
                        w2.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        ArrayList<WindowState> arrayList = this.mForceRemoves;
        if (arrayList != null && arrayList.size() > 0) {
            pw.println();
            pw.println("  Windows force removing:");
            for (int i3 = this.mForceRemoves.size() - 1; i3 >= 0; i3--) {
                WindowState w3 = this.mForceRemoves.get(i3);
                pw.print("  Removing #");
                pw.print(i3);
                pw.print(' ');
                pw.print(w3);
                if (dumpAll) {
                    pw.println(":");
                    w3.dump(pw, "    ", true);
                } else {
                    pw.println();
                }
            }
        }
        if (this.mDestroySurface.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to destroy their surface:");
            for (int i4 = this.mDestroySurface.size() - 1; i4 >= 0; i4--) {
                WindowState w4 = this.mDestroySurface.get(i4);
                if (windows == null || windows.contains(w4)) {
                    pw.print("  Destroy #");
                    pw.print(i4);
                    pw.print(' ');
                    pw.print(w4);
                    if (dumpAll) {
                        pw.println(":");
                        w4.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mResizingWindows.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to resize:");
            for (int i5 = this.mResizingWindows.size() - 1; i5 >= 0; i5--) {
                WindowState w5 = this.mResizingWindows.get(i5);
                if (windows == null || windows.contains(w5)) {
                    pw.print("  Resizing #");
                    pw.print(i5);
                    pw.print(' ');
                    pw.print(w5);
                    if (dumpAll) {
                        pw.println(":");
                        w5.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mWaitingForDrawn.size() > 0) {
            pw.println();
            pw.println("  Clients waiting for these windows to be drawn:");
            for (int i6 = this.mWaitingForDrawn.size() - 1; i6 >= 0; i6--) {
                pw.print("  Waiting #");
                pw.print(i6);
                pw.print(' ');
                pw.print(this.mWaitingForDrawn.get(i6));
            }
        }
        pw.println();
        pw.print("  mGlobalConfiguration=");
        pw.println(this.mRoot.getConfiguration());
        pw.print("  mHasPermanentDpad=");
        pw.println(this.mHasPermanentDpad);
        this.mRoot.dumpTopFocusedDisplayId(pw);
        this.mRoot.forAllDisplays(new Consumer(pw) {
            /* class com.android.server.wm.$$Lambda$WindowManagerService$pgbw_FPqeLJMP83kqiaVcOeiDs */
            private final /* synthetic */ PrintWriter f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowManagerService.lambda$dumpWindowsNoHeaderLocked$7(this.f$0, (DisplayContent) obj);
            }
        });
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
        this.mInputManagerCallback.dump(pw, "  ");
        this.mTaskSnapshotController.dump(pw, "  ");
        if (dumpAll) {
            WindowState imeWindow = this.mRoot.getCurrentInputMethodWindow();
            if (imeWindow != null) {
                pw.print("  mInputMethodWindow=");
                pw.println(imeWindow);
            }
            this.mWindowPlacerLocked.dump(pw, "  ");
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
            DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
            pw.print("  mRotation=");
            pw.print(defaultDisplayContent.getRotation());
            pw.print("  mLastWindowForcedOrientation=");
            pw.print(defaultDisplayContent.getLastWindowForcedOrientation());
            pw.print(" mLastOrientation=");
            pw.println(defaultDisplayContent.getLastOrientation());
            pw.print(" waitingForConfig=");
            pw.println(defaultDisplayContent.mWaitingForConfig);
            pw.print("  Animation settings: disabled=");
            pw.print(this.mAnimationsDisabled);
            pw.print(" window=");
            pw.print(this.mWindowAnimationScaleSetting);
            pw.print(" transition=");
            pw.print(this.mTransitionAnimationScaleSetting);
            pw.print(" animator=");
            pw.println(this.mAnimatorDurationScaleSetting);
            if (this.mRecentsAnimationController != null) {
                pw.print("  mRecentsAnimationController=");
                pw.println(this.mRecentsAnimationController);
                this.mRecentsAnimationController.dump(pw, "    ");
            }
            PolicyControl.dump("  ", pw);
        }
    }

    static /* synthetic */ void lambda$dumpWindowsNoHeaderLocked$7(PrintWriter pw, DisplayContent dc) {
        WindowState inputMethodTarget = dc.mInputMethodTarget;
        if (inputMethodTarget != null) {
            pw.print("  mInputMethodTarget in display# ");
            pw.print(dc.getDisplayId());
            pw.print(' ');
            pw.println(inputMethodTarget);
        }
    }

    private boolean dumpWindows(PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<WindowState> windows = new ArrayList<>();
        if ("apps".equals(name) || "visible".equals(name) || "visible-apps".equals(name)) {
            boolean appsOnly = name.contains("apps");
            boolean visibleOnly = name.contains("visible");
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    if (appsOnly) {
                        this.mRoot.dumpDisplayContents(pw);
                    }
                    this.mRoot.forAllWindows((Consumer<WindowState>) new Consumer(visibleOnly, appsOnly, windows) {
                        /* class com.android.server.wm.$$Lambda$WindowManagerService$C4RecYWtrllidEGWyvVvRsY6lno */
                        private final /* synthetic */ boolean f$0;
                        private final /* synthetic */ boolean f$1;
                        private final /* synthetic */ ArrayList f$2;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WindowManagerService.lambda$dumpWindows$8(this.f$0, this.f$1, this.f$2, (WindowState) obj);
                        }
                    }, true);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.getWindowsByName(windows, name);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        }
        if (windows.size() <= 0) {
            return false;
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                dumpWindowsLocked(pw, dumpAll, windows);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return true;
    }

    static /* synthetic */ void lambda$dumpWindows$8(boolean visibleOnly, boolean appsOnly, ArrayList windows, WindowState w) {
        if (visibleOnly && !w.mWinAnimator.getShown()) {
            return;
        }
        if (!appsOnly || w.mAppToken != null) {
            windows.add(w);
        }
    }

    private void dumpLastANRLocked(PrintWriter pw) {
        pw.println("WINDOW MANAGER LAST ANR (dumpsys window lastanr)");
        String str = this.mLastANRState;
        if (str == null) {
            pw.println("  <no ANR has occurred since boot>");
        } else {
            pw.println(str);
        }
    }

    /* access modifiers changed from: package-private */
    public void saveANRStateLocked(AppWindowToken appWindowToken, WindowState windowState, String reason) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter(sw, false, 1024);
        pw.println("  ANR time: " + DateFormat.getDateTimeInstance().format(new Date()));
        if (appWindowToken != null) {
            pw.println("  Application at fault: " + appWindowToken.stringName);
        }
        if (windowState != null) {
            pw.println("  Window at fault: " + ((Object) windowState.mAttrs.getTitle()));
        }
        if (reason != null) {
            pw.println("  Reason: " + reason);
        }
        for (int i = this.mRoot.getChildCount() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mRoot.getChildAt(i);
            int displayId = dc.getDisplayId();
            if (!dc.mWinAddedSinceNullFocus.isEmpty()) {
                pw.println("  Windows added in display #" + displayId + " since null focus: " + dc.mWinAddedSinceNullFocus);
            }
            if (!dc.mWinRemovedSinceNullFocus.isEmpty()) {
                pw.println("  Windows removed in display #" + displayId + " since null focus: " + dc.mWinRemovedSinceNullFocus);
            }
        }
        pw.println();
        dumpWindowsNoHeaderLocked(pw, true, null);
        pw.println();
        pw.println("Last ANR continued");
        this.mRoot.dumpDisplayContents(pw);
        pw.close();
        this.mLastANRState = sw.toString();
        this.mH.removeMessages(38);
        this.mH.sendEmptyMessageDelayed(38, 7200000);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        PriorityDump.dump(this.mPriorityDumper, fd, pw, args);
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: private */
    public void doDump(FileDescriptor fd, PrintWriter pw, String[] args, boolean useProto) {
        String opt;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            boolean dumpAll = false;
            int opti = 0;
            while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
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
                        pw.println("    debug[Focus|Visibility|Layer|Orientation|Transition|Configuration|\n          Screen|Wallpaper|Input|Layout|Display|StartingWindow|AllOn|AllOff]\n          Turn wms debug flag on. Eg. adb shell dumpsys window debugFocus");
                    }
                    pw.println("    trace: print trace status and write Winscope trace to file");
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
                    pw.println("Unknown argument: " + opt + "; use -h for help");
                }
            }
            if (useProto) {
                ProtoOutputStream proto = new ProtoOutputStream(fd);
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        writeToProtoLocked(proto, 0);
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                resetPriorityAfterLockedSection();
                proto.flush();
            } else if (opti < args.length) {
                String cmd = args[opti];
                int opti2 = opti + 1;
                if (ActivityTaskManagerService.DUMP_LASTANR_CMD.equals(cmd) || "l".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpLastANRLocked(pw);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("policy".equals(cmd) || "p".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpPolicyLocked(pw, args, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("animator".equals(cmd) || ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD.equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpAnimatorLocked(pw, args, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("sessions".equals(cmd) || "s".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpSessionsLocked(pw, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("displays".equals(cmd) || "d".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpDisplayContents(pw);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("tokens".equals(cmd) || "t".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpTokensLocked(pw, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("windows".equals(cmd) || "w".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(pw, true, null);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("all".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(pw, true, null);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if (ActivityTaskManagerService.DUMP_CONTAINERS_CMD.equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpChildrenNames(pw, " ");
                            pw.println(" ");
                            this.mRoot.forAllWindows((Consumer<WindowState>) new Consumer(pw) {
                                /* class com.android.server.wm.$$Lambda$WindowManagerService$LbJzcX6LZWc_oRhCOhY74zWzL7Y */
                                private final /* synthetic */ PrintWriter f$0;

                                {
                                    this.f$0 = r1;
                                }

                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    this.f$0.println((WindowState) obj);
                                }
                            }, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if ("trace".equals(cmd)) {
                    dumpTraceStatus(pw);
                } else if ("handler".equals(cmd) || "h".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpHandlerLocked(pw);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                } else if (IS_DEBUG_VERSION && !TextUtils.isEmpty(cmd) && cmd.startsWith(DEBUG_PREFIX)) {
                    setWmsDebugFlag(pw, cmd);
                } else if (!dumpWindows(pw, cmd, args, opti2, dumpAll)) {
                    pw.println("Bad window command, or no windows match: " + cmd);
                    pw.println("Use -h for help.");
                }
            } else {
                synchronized (this.mGlobalLock) {
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
                        if (dumpAll) {
                            pw.println("-------------------------------------------------------------------------------");
                        }
                        dumpTraceStatus(pw);
                    } finally {
                        resetPriorityAfterLockedSection();
                    }
                }
            }
        }
    }

    public void monitor() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public DisplayContent getDefaultDisplayContentLocked() {
        return this.mRoot.getDisplayContent(0);
    }

    public void onOverlayChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllDisplays($$Lambda$WindowManagerService$oXZopye9ykF6MR6QjHAIi3bGRc.INSTANCE);
                requestTraversal();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    static /* synthetic */ void lambda$onOverlayChanged$10(DisplayContent displayContent) {
        displayContent.getDisplayPolicy().onOverlayChangedLw();
        displayContent.updateDisplayInfo();
    }

    public void onDisplayChanged(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.updateDisplayInfo();
                }
                this.mWindowPlacerLocked.requestTraversal();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public Object getWindowManagerLock() {
        return this.mGlobalLock;
    }

    public void setWillReplaceWindow(IBinder token, boolean animate) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(token);
                if (appWindowToken == null) {
                    Slog.w(TAG, "Attempted to set replacing window on non-existing app token " + token);
                } else if (!appWindowToken.hasContentToDisplay()) {
                    Slog.w(TAG, "Attempted to set replacing window on app token with no content" + token);
                    resetPriorityAfterLockedSection();
                } else {
                    appWindowToken.setWillReplaceWindows(animate);
                    resetPriorityAfterLockedSection();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setWillReplaceWindows(IBinder token, boolean childrenOnly) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(token);
                if (appWindowToken == null) {
                    Slog.w(TAG, "Attempted to set replacing window on non-existing app token " + token);
                } else if (!appWindowToken.hasContentToDisplay()) {
                    Slog.w(TAG, "Attempted to set replacing window on app token with no content" + token);
                    resetPriorityAfterLockedSection();
                } else {
                    if (childrenOnly) {
                        appWindowToken.setWillReplaceChildWindows();
                    } else {
                        appWindowToken.setWillReplaceWindows(false);
                    }
                    scheduleClearWillReplaceWindows(token, true);
                    resetPriorityAfterLockedSection();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void scheduleClearWillReplaceWindows(IBinder token, boolean replacing) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                AppWindowToken appWindowToken = this.mRoot.getAppWindowToken(token);
                if (appWindowToken == null) {
                    Slog.w(TAG, "Attempted to reset replacing window on non-existing app token " + token);
                    return;
                }
                if (replacing) {
                    scheduleWindowReplacementTimeouts(appWindowToken);
                } else {
                    appWindowToken.clearWillReplaceWindows();
                }
                resetPriorityAfterLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void scheduleWindowReplacementTimeouts(AppWindowToken appWindowToken) {
        if (!this.mWindowReplacementTimeouts.contains(appWindowToken)) {
            this.mWindowReplacementTimeouts.add(appWindowToken);
        }
        this.mH.removeMessages(46);
        this.mH.sendEmptyMessageDelayed(46, 2000);
    }

    public int getDockedStackSide() {
        int dockSide;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                TaskStack dockedStack = getDefaultDisplayContentLocked().getSplitScreenPrimaryStackIgnoringVisibility();
                dockSide = dockedStack == null ? -1 : dockedStack.getDockSide();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
        return dockSide;
    }

    public void setDockedStackResizing(boolean resizing) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!resizing) {
                    this.mResizing = false;
                    this.mH.removeMessages(H.SPLIT_RESIZING_TIMEOUT);
                }
                getDefaultDisplayContentLocked().getDockedDividerController().setResizing(resizing);
                requestTraversal();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setDockedStackDividerTouchRegion(Rect touchRegion) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = getDefaultDisplayContentLocked();
                dc.getDockedDividerController().setTouchRegion(touchRegion);
                dc.updateTouchExcludeRegion();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setResizeDimLayer(boolean visible, int targetWindowingMode, float alpha) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setResizeDimLayer(visible, targetWindowingMode, alpha);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setForceResizableTasks(boolean forceResizableTasks) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mForceResizableTasks = forceResizableTasks;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setSupportsPictureInPicture(boolean supportsPictureInPicture) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mSupportsPictureInPicture = supportsPictureInPicture;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setSupportsFreeformWindowManagement(boolean supportsFreeformWindowManagement) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mSupportsFreeformWindowManagement = supportsFreeformWindowManagement;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setForceDesktopModeOnExternalDisplays(boolean forceDesktopModeOnExternalDisplays) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mForceDesktopModeOnExternalDisplays = forceDesktopModeOnExternalDisplays;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setIsPc(boolean isPc) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mIsPc = isPc;
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    static int dipToPixel(int dip, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(1, (float) dip, displayMetrics);
    }

    public void registerDockedStackListener(IDockedStackListener listener) {
        this.mAtmInternal.enforceCallerIsRecentsOrHasPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerDockedStackListener()");
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().mDividerControllerLocked.registerDockedStackListener(listener);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void registerPinnedStackListener(int displayId, IPinnedStackListener listener) {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerPinnedStackListener()") || !this.mSupportsPictureInPicture) {
            return;
        }
        if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer()) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.getDisplayContent(displayId).getPinnedStackController().registerPinnedStackListener(listener);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        HwPCUtils.log(TAG, "ignore pinned stack listener in pad pc mode");
    }

    public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
        try {
            WindowState focusedWindow = getFocusedWindow();
            if (focusedWindow != null && focusedWindow.mClient != null) {
                getFocusedWindow().mClient.requestAppKeyboardShortcuts(receiver, deviceId);
            }
        } catch (RemoteException e) {
        }
    }

    public void getStableInsets(int displayId, Rect outInsets) throws RemoteException {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getStableInsetsLocked(displayId, outInsets);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void getStableInsetsLocked(int displayId, Rect outInsets) {
        outInsets.setEmpty();
        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
        if (dc != null) {
            DisplayInfo di = dc.getDisplayInfo();
            dc.getDisplayPolicy().getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, di.displayCutout, outInsets);
        }
    }

    public void setForwardedInsets(int displayId, Insets insets) throws RemoteException {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc != null) {
                    if (Binder.getCallingUid() == dc.getDisplay().getOwnerUid()) {
                        dc.setForwardedInsets(insets);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    throw new SecurityException("Only owner of the display can set ForwardedInsets to it.");
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void intersectDisplayInsetBounds(Rect display, Rect insets, Rect inOutBounds) {
        this.mTmpRect3.set(display);
        this.mTmpRect3.inset(insets);
        inOutBounds.intersect(this.mTmpRect3);
    }

    /* access modifiers changed from: private */
    public static class MousePositionTracker implements WindowManagerPolicyConstants.PointerEventListener {
        /* access modifiers changed from: private */
        public boolean mLatestEventWasMouse;
        /* access modifiers changed from: private */
        public float mLatestMouseX;
        /* access modifiers changed from: private */
        public float mLatestMouseY;

        private MousePositionTracker() {
        }

        /* access modifiers changed from: package-private */
        public void updatePosition(float x, float y) {
            synchronized (this) {
                this.mLatestEventWasMouse = true;
                this.mLatestMouseX = x;
                this.mLatestMouseY = y;
            }
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent.isFromSource(8194)) {
                updatePosition(motionEvent.getRawX(), motionEvent.getRawY());
                return;
            }
            synchronized (this) {
                this.mLatestEventWasMouse = false;
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x001c, code lost:
        monitor-enter(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:?, code lost:
        boostPriorityForLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0026, code lost:
        if (r9.mDragDropController.dragDropActiveLocked() == false) goto L_0x002d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0028, code lost:
        monitor-exit(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x002c, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x002d, code lost:
        r0 = windowForClientLocked((com.android.server.wm.Session) null, r10, false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0033, code lost:
        if (r0 != null) goto L_0x0050;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0035, code lost:
        android.util.Slog.w(com.android.server.wm.WindowManagerService.TAG, "Bad requesting window " + r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x004b, code lost:
        monitor-exit(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004c, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x004f, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0050, code lost:
        r4 = r0.getDisplayContent();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0054, code lost:
        if (r4 != null) goto L_0x005b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0056, code lost:
        monitor-exit(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0057, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x005a, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x005b, code lost:
        r5 = r4.getTouchableWinAtPointLocked(r1, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0060, code lost:
        if (r5 == r0) goto L_0x0067;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0062, code lost:
        monitor-exit(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0063, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0066, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:?, code lost:
        r5.mClient.updatePointerIcon(r5.translateToWindowX(r1), r5.translateToWindowY(r2));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0076, code lost:
        android.util.Slog.w(com.android.server.wm.WindowManagerService.TAG, "unable to update pointer icon");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0082, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x0084, code lost:
        resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x0087, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x001a, code lost:
        r3 = r9.mGlobalLock;
     */
    public void updatePointerIcon(IWindow client) {
        WindowManagerGlobalLock windowManagerGlobalLock;
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

    /* access modifiers changed from: package-private */
    public void restorePointerIconLocked(DisplayContent displayContent, float latestX, float latestY) {
        this.mMousePositionTracker.updatePosition(latestX, latestY);
        WindowState windowUnderPointer = displayContent.getTouchableWinAtPointLocked(latestX, latestY);
        if (windowUnderPointer != null) {
            try {
                windowUnderPointer.mClient.updatePointerIcon(windowUnderPointer.translateToWindowX(latestX), windowUnderPointer.translateToWindowY(latestY));
            } catch (RemoteException e) {
                Slog.w(TAG, "unable to restore pointer icon");
            }
        } else {
            InputManager.getInstance().setPointerIconType(1000);
        }
    }

    private void checkCallerOwnsDisplay(int displayId) {
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display == null) {
            throw new IllegalArgumentException("Cannot find display for non-existent displayId: " + displayId);
        } else if (Binder.getCallingUid() != display.getOwnerUid()) {
            throw new SecurityException("The caller doesn't own the display.");
        }
    }

    /* access modifiers changed from: package-private */
    public void reparentDisplayContent(IWindow client, SurfaceControl sc, int displayId) {
        checkCallerOwnsDisplay(displayId);
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                long token = Binder.clearCallingIdentity();
                try {
                    WindowState win = windowForClientLocked((Session) null, client, false);
                    if (win == null) {
                        Slog.w(TAG, "Bad requesting window " + client);
                        return;
                    }
                    getDisplayContentOrCreate(displayId, null).reparentDisplayContent(win, sc);
                    Binder.restoreCallingIdentity(token);
                    resetPriorityAfterLockedSection();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateDisplayContentLocation(IWindow client, int x, int y, int displayId) {
        checkCallerOwnsDisplay(displayId);
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                long token = Binder.clearCallingIdentity();
                try {
                    WindowState win = windowForClientLocked((Session) null, client, false);
                    if (win == null) {
                        Slog.w(TAG, "Bad requesting window " + client);
                        return;
                    }
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent != null) {
                        displayContent.updateLocation(win, x, y);
                    }
                    Binder.restoreCallingIdentity(token);
                    resetPriorityAfterLockedSection();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateTapExcludeRegion(IWindow client, int regionId, Region region) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState callingWin = windowForClientLocked((Session) null, client, false);
                if (callingWin == null) {
                    Slog.w(TAG, "Bad requesting window " + client);
                    return;
                }
                callingWin.updateTapExcludeRegion(regionId, region);
                resetPriorityAfterLockedSection();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public void dontOverrideDisplayInfo(int displayId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent dc = getDisplayContentOrCreate(displayId, null);
                    if (dc != null) {
                        dc.mShouldOverrideDisplayConfiguration = false;
                        this.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayId, (DisplayInfo) null);
                    } else {
                        throw new IllegalArgumentException("Trying to configure a non existent display.");
                    }
                } catch (Throwable th) {
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public int getWindowingMode(int displayId) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "getWindowingMode()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to get windowing mode of a display that does not exist: " + displayId);
                        return 0;
                    }
                    int windowingModeLocked = this.mDisplayWindowSettings.getWindowingModeLocked(displayContent);
                    resetPriorityAfterLockedSection();
                    return windowingModeLocked;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    /* JADX INFO: finally extract failed */
    public void setWindowingMode(int displayId, int mode) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setWindowingMode()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to set windowing mode to a display that does not exist: " + displayId);
                        return;
                    }
                    int lastWindowingMode = displayContent.getWindowingMode();
                    this.mDisplayWindowSettings.setWindowingModeLocked(displayContent, mode);
                    reconfigureDisplayLocked(displayContent);
                    if (lastWindowingMode != displayContent.getWindowingMode()) {
                        this.mH.removeMessages(18);
                        long origId = Binder.clearCallingIdentity();
                        try {
                            sendNewConfiguration(displayId);
                            Binder.restoreCallingIdentity(origId);
                            displayContent.executeAppTransition();
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(origId);
                            throw th;
                        }
                    }
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    @WindowManager.RemoveContentMode
    public int getRemoveContentMode(int displayId) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "getRemoveContentMode()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to get remove mode of a display that does not exist: " + displayId);
                        return 0;
                    }
                    int removeContentModeLocked = this.mDisplayWindowSettings.getRemoveContentModeLocked(displayContent);
                    resetPriorityAfterLockedSection();
                    return removeContentModeLocked;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public void setRemoveContentMode(int displayId, @WindowManager.RemoveContentMode int mode) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setRemoveContentMode()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to set remove mode to a display that does not exist: " + displayId);
                        return;
                    }
                    this.mDisplayWindowSettings.setRemoveContentModeLocked(displayContent, mode);
                    reconfigureDisplayLocked(displayContent);
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public boolean shouldShowWithInsecureKeyguard(int displayId) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "shouldShowWithInsecureKeyguard()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to get flag of a display that does not exist: " + displayId);
                        return false;
                    }
                    boolean shouldShowWithInsecureKeyguardLocked = this.mDisplayWindowSettings.shouldShowWithInsecureKeyguardLocked(displayContent);
                    resetPriorityAfterLockedSection();
                    return shouldShowWithInsecureKeyguardLocked;
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public void setShouldShowWithInsecureKeyguard(int displayId, boolean shouldShow) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setShouldShowWithInsecureKeyguard()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to set flag to a display that does not exist: " + displayId);
                        return;
                    }
                    this.mDisplayWindowSettings.setShouldShowWithInsecureKeyguardLocked(displayContent, shouldShow);
                    reconfigureDisplayLocked(displayContent);
                    resetPriorityAfterLockedSection();
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public boolean shouldShowSystemDecors(int displayId) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "shouldShowSystemDecors()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to get system decors flag of a display that does not exist: " + displayId);
                        return false;
                    } else if (displayContent.isUntrustedVirtualDisplay()) {
                        resetPriorityAfterLockedSection();
                        return false;
                    } else {
                        boolean supportsSystemDecorations = displayContent.supportsSystemDecorations();
                        resetPriorityAfterLockedSection();
                        return supportsSystemDecorations;
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public void setShouldShowSystemDecors(int displayId, boolean shouldShow) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setShouldShowSystemDecors()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to set system decors flag to a display that does not exist: " + displayId);
                    } else if (!displayContent.isUntrustedVirtualDisplay()) {
                        this.mDisplayWindowSettings.setShouldShowSystemDecorsLocked(displayContent, shouldShow);
                        reconfigureDisplayLocked(displayContent);
                        resetPriorityAfterLockedSection();
                    } else {
                        throw new SecurityException("Attempted to set system decors flag to an untrusted virtual display: " + displayId);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public boolean shouldShowIme(int displayId) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "shouldShowIme()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                    boolean z = false;
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to get IME flag of a display that does not exist: " + displayId);
                        return false;
                    } else if (displayContent.isUntrustedVirtualDisplay()) {
                        resetPriorityAfterLockedSection();
                        return false;
                    } else {
                        if (this.mDisplayWindowSettings.shouldShowImeLocked(displayContent) || this.mForceDesktopModeOnExternalDisplays) {
                            z = true;
                        }
                        resetPriorityAfterLockedSection();
                        return z;
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
    }

    public void setShouldShowIme(int displayId, boolean shouldShow) {
        if (checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setShouldShowIme()")) {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                    if (displayContent == null) {
                        Slog.w(TAG, "Attempted to set IME flag to a display that does not exist: " + displayId);
                    } else if (!displayContent.isUntrustedVirtualDisplay()) {
                        this.mDisplayWindowSettings.setShouldShowImeLocked(displayContent, shouldShow);
                        reconfigureDisplayLocked(displayContent);
                        resetPriorityAfterLockedSection();
                    } else {
                        throw new SecurityException("Attempted to set IME flag to an untrusted virtual display: " + displayId);
                    }
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
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

    /* access modifiers changed from: package-private */
    public void markForSeamlessRotation(WindowState w, boolean seamlesslyRotated) {
        if (seamlesslyRotated != w.mSeamlesslyRotated && !w.mForceSeamlesslyRotate) {
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
                w.getDisplayContent().updateRotationAndSendNewConfigIfNeeded();
            }
        }
    }

    /* access modifiers changed from: private */
    public final class LocalService extends WindowManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void requestTraversalFromDisplayManager() {
            WindowManagerService.this.requestTraversal();
        }

        /* JADX INFO: finally extract failed */
        @Override // com.android.server.wm.WindowManagerInternal
        public void setMagnificationSpec(int displayId, MagnificationSpec spec) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.setMagnificationSpecLocked(displayId, spec);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (Binder.getCallingPid() != Process.myPid()) {
                spec.recycle();
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setForceShowMagnifiableBounds(int displayId, boolean show) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.setForceShowMagnifiableBoundsLocked(displayId, show);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void getMagnificationRegion(int displayId, Region magnificationRegion) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        WindowManagerService.this.mAccessibilityController.getMagnificationRegionLocked(displayId, magnificationRegion);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public MagnificationSpec getCompatibleMagnificationSpecForWindow(IBinder windowToken) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) WindowManagerService.this.mWindowMap.get(windowToken);
                    if (windowState == null) {
                        return null;
                    }
                    MagnificationSpec spec = null;
                    if (WindowManagerService.this.mAccessibilityController != null) {
                        spec = WindowManagerService.this.mAccessibilityController.getMagnificationSpecForWindowLocked(windowState);
                    }
                    if ((spec == null || spec.isNop()) && windowState.mGlobalScale == 1.0f) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    MagnificationSpec spec2 = spec == null ? MagnificationSpec.obtain() : MagnificationSpec.obtain(spec);
                    spec2.scale *= windowState.mGlobalScale;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return spec2;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean setMagnificationCallbacks(int displayId, WindowManagerInternal.MagnificationCallbacks callbacks) {
            boolean result;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController == null) {
                        WindowManagerService.this.mAccessibilityController = new AccessibilityController(WindowManagerService.this);
                    }
                    result = WindowManagerService.this.mAccessibilityController.setMagnificationCallbacksLocked(displayId, callbacks);
                    if (!WindowManagerService.this.mAccessibilityController.hasCallbacksLocked()) {
                        WindowManagerService.this.mAccessibilityController = null;
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return result;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setWindowsForAccessibilityCallback(WindowManagerInternal.WindowsForAccessibilityCallback callback) {
            synchronized (WindowManagerService.this.mGlobalLock) {
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
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setInputFilter(IInputFilter filter) {
            WindowManagerService.this.mInputManager.setInputFilter(filter);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public IBinder getFocusedWindowToken() {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.getFocusedWindowLocked();
                    if (windowState != null) {
                        return windowState.mClient.asBinder();
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isCoverOpen() {
            return WindowManagerService.this.mHwWMSEx.isCoverOpen();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isKeyguardLocked() {
            return WindowManagerService.this.isKeyguardLocked();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isKeyguardShowingAndNotOccluded() {
            return WindowManagerService.this.isKeyguardShowingAndNotOccluded();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void showGlobalActions() {
            WindowManagerService.this.showGlobalActions();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void getWindowFrame(IBinder token, Rect outBounds) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = (WindowState) WindowManagerService.this.mWindowMap.get(token);
                    if (windowState != null) {
                        outBounds.set(windowState.getFrameLw());
                    } else {
                        outBounds.setEmpty();
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void waitForAllWindowsDrawn(Runnable callback, long timeout) {
            boolean allWindowsDrawn = false;
            long unused = WindowManagerService.this.mWaitAllWindowDrawStartTime = SystemClock.elapsedRealtime();
            synchronized (WindowManagerService.this.mGlobalLock) {
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
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (allWindowsDrawn) {
                callback.run();
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setForcedDisplaySize(int displayId, int width, int height) {
            WindowManagerService.this.setForcedDisplaySize(displayId, width, height);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void clearForcedDisplaySize(int displayId) {
            WindowManagerService.this.clearForcedDisplaySize(displayId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void addWindowToken(IBinder token, int type, int displayId) {
            WindowManagerService.this.addWindowToken(token, type, displayId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void removeWindowToken(IBinder binder, boolean removeWindows, int displayId) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (removeWindows) {
                        DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                        if (dc == null) {
                            Slog.w(WindowManagerService.TAG, "removeWindowToken: Attempted to remove token: " + binder + " for non-exiting displayId=" + displayId);
                            return;
                        }
                        WindowToken token = dc.removeWindowToken(binder);
                        if (token == null) {
                            Slog.w(WindowManagerService.TAG, "removeWindowToken: Attempted to remove non-existing token: " + binder);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        token.removeAllWindowsIfPossible();
                    }
                    WindowManagerService.this.removeWindowToken(binder, displayId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void registerAppTransitionListener(WindowManagerInternal.AppTransitionListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.getDefaultDisplayContentLocked().mAppTransition.registerListenerLocked(listener);
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void reportPasswordChanged(int userId) {
            WindowManagerService.this.mKeyguardDisableHandler.updateKeyguardEnabled(userId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getInputMethodWindowVisibleHeight(int displayId) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    if (dc == null) {
                        Slog.d(WindowManagerService.TAG, "displayContent is null, return 0");
                        return 0;
                    }
                    int inputMethodWindowVisibleHeight = dc.mDisplayFrames.getInputMethodWindowVisibleHeight();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return inputMethodWindowVisibleHeight;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void updateInputMethodWindowStatus(IBinder imeToken, boolean imeWindowVisible, boolean dismissImeOnBackKeyPressed) {
            WindowManagerService.this.mPolicy.setDismissImeOnBackKeyPressed(dismissImeOnBackKeyPressed);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void updateInputMethodTargetWindow(IBinder imeToken, IBinder imeTargetWindowToken) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w(WindowManagerService.TAG, "updateInputMethodTargetWindow: imeToken=" + imeToken + " imeTargetWindowToken=" + imeTargetWindowToken);
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isHardKeyboardAvailable() {
            boolean z;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = WindowManagerService.this.mHardKeyboardAvailable;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return z;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setOnHardKeyboardStatusChangeListener(WindowManagerInternal.OnHardKeyboardStatusChangeListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mHardKeyboardStatusChangeListener = listener;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isStackVisibleLw(int windowingMode) {
            return WindowManagerService.this.getDefaultDisplayContentLocked().isStackVisible(windowingMode);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void computeWindowsForAccessibility() {
            AccessibilityController accessibilityController;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    accessibilityController = WindowManagerService.this.mAccessibilityController;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (accessibilityController != null) {
                accessibilityController.performComputeChangedWindowsNotLocked(true);
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setVr2dDisplayId(int vr2dDisplayId) {
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.d(WindowManagerService.TAG, "setVr2dDisplayId called for: " + vr2dDisplayId);
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mVr2dDisplayId = vr2dDisplayId;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void registerDragDropControllerCallback(WindowManagerInternal.IDragDropCallback callback) {
            WindowManagerService.this.mDragDropController.registerCallback(callback);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void lockNow() {
            WindowManagerService.this.lockNow(null);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getWindowOwnerUserId(IBinder token) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState window = (WindowState) WindowManagerService.this.mWindowMap.get(token);
                    if (window != null) {
                        return UserHandle.getUserId(window.mOwnerUid);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -10000;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isUidFocused(int uid) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    for (int i = WindowManagerService.this.mRoot.getChildCount() - 1; i >= 0; i--) {
                        DisplayContent displayContent = (DisplayContent) WindowManagerService.this.mRoot.getChildAt(i);
                        if (displayContent.mCurrentFocus != null && uid == displayContent.mCurrentFocus.getOwningUid()) {
                            return true;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isInputMethodClientFocus(int uid, int pid, int displayId) {
            if (displayId == -1) {
                return false;
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent displayContent = WindowManagerService.this.mRoot.getTopFocusedDisplayContent();
                    if (displayContent != null && displayContent.getDisplayId() == displayId) {
                        if (displayContent.hasAccess(uid)) {
                            if (displayContent.isInputMethodClientFocus(uid, pid)) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return true;
                            }
                            WindowState currentFocus = displayContent.mCurrentFocus;
                            if (currentFocus != null && currentFocus.mSession.mUid == uid && currentFocus.mSession.mPid == pid) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return true;
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                    }
                    return false;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isUidAllowedOnDisplay(int displayId, int uid) {
            boolean z = true;
            if (displayId == 0) {
                return true;
            }
            if (displayId == -1) {
                return false;
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent displayContent = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null || !displayContent.hasAccess(uid)) {
                        z = false;
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return z;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getDisplayIdForWindow(IBinder windowToken) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState window = (WindowState) WindowManagerService.this.mWindowMap.get(windowToken);
                    if (window != null) {
                        return window.getDisplayContent().getDisplayId();
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -1;
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setDockedStackDividerRotation(int rotation) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().setDockedStackDividerRotation(rotation);
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getFocusedDisplayId() {
            return WindowManagerService.this.getFocusedDisplayId();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setFocusedDisplay(int displayId, boolean findTopTask, String reason) {
            WindowManagerService.this.setFocusedDisplay(displayId, findTopTask, reason);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isMinimizedDock() {
            boolean isMinimizedDock;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    isMinimizedDock = WindowManagerService.this.getDefaultDisplayContentLocked().getDockedDividerController().isMinimizedDock();
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return isMinimizedDock;
        }

        @Override // com.android.server.wm.WindowManagerInternal
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

        @Override // com.android.server.wm.WindowManagerInternal
        public void setForcedDisplaySize(int displayId, int width, int height, Bundle foldScreenInfo) {
            WindowManagerService windowManagerService = WindowManagerService.this;
            windowManagerService.mFoldScreenInfo = foldScreenInfo;
            windowManagerService.setForcedDisplaySize(displayId, width, height);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getTopFocusedDisplayId() {
            int displayId;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    displayId = WindowManagerService.this.mRoot.getTopFocusedDisplayContent().getDisplayId();
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return displayId;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean shouldShowSystemDecorOnDisplay(int displayId) {
            boolean shouldShowSystemDecors;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    shouldShowSystemDecors = WindowManagerService.this.shouldShowSystemDecors(displayId);
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return shouldShowSystemDecors;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean shouldShowIme(int displayId) {
            boolean shouldShowIme;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    shouldShowIme = WindowManagerService.this.shouldShowIme(displayId);
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return shouldShowIme;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void addNonHighRefreshRatePackage(String packageName) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer(packageName) {
                        /* class com.android.server.wm.$$Lambda$WindowManagerService$LocalService$_nYJRiVOgbON7mI191FIzNAk4Xs */
                        private final /* synthetic */ String f$0;

                        {
                            this.f$0 = r1;
                        }

                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((DisplayContent) obj).getDisplayPolicy().getRefreshRatePolicy().addNonHighRefreshRatePackage(this.f$0);
                        }
                    });
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void removeNonHighRefreshRatePackage(String packageName) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer(packageName) {
                        /* class com.android.server.wm.$$Lambda$WindowManagerService$LocalService$rEGrcIRCgYp4kzr5xA12LKQX0E */
                        private final /* synthetic */ String f$0;

                        {
                            this.f$0 = r1;
                        }

                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((DisplayContent) obj).getDisplayPolicy().getRefreshRatePolicy().removeNonHighRefreshRatePackage(this.f$0);
                        }
                    });
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void showOrHideInsetSurface(MotionEvent event) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.showOrHideInsetSurface(event);
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void freezeFoldRotation(int rotation) {
            WindowManagerService.this.freezeFoldRotation(rotation);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void unFreezeFoldRotation() {
            WindowManagerService.this.unFreezeFoldRotation();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getFoldDisplayMode() {
            return WindowManagerService.this.getFoldDisplayMode();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void transactionApply(SurfaceControl.Transaction t) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Slog.i(WindowManagerService.TAG, "transactionApply");
                    t.apply();
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setFoldSwitchState(boolean state) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (HwFoldScreenState.isFoldScreenDevice() && WindowManagerService.PROP_FOLD_SWITCH_DEBUG) {
                        WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayRotation().setFoldSwitchState(state);
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    public void freezeFoldRotation(int rotation) {
        Slog.i(TAG, "freezeFoldRotation rotation = " + rotation + " isRotationFrozen = " + isRotationFrozen() + " isFoldRotationFreezed = " + isFoldRotationFreezed());
        if (!isRotationFrozen() && !isFoldRotationFreezed()) {
            freezeRotation(rotation);
            setFoldRotationFreezed(true);
        }
    }

    public void unFreezeFoldRotation() {
        Slog.i(TAG, "unFreezeFoldRotation isFoldRotationFreezed = " + isFoldRotationFreezed());
        if (isFoldRotationFreezed()) {
            thawRotation();
            setFoldRotationFreezed(false);
        }
    }

    public void wakeDisplayModeChange(boolean change) {
        Slog.d(TAG, "wakeDisplayModeChange is called!");
        this.mDisplayManagerInternal.wakeupDisplayModeChange(true);
    }

    /* access modifiers changed from: package-private */
    public void registerAppFreezeListener(AppFreezeListener listener) {
        if (!this.mAppFreezeListeners.contains(listener)) {
            this.mAppFreezeListeners.add(listener);
        }
    }

    /* access modifiers changed from: package-private */
    public void unregisterAppFreezeListener(AppFreezeListener listener) {
        this.mAppFreezeListeners.remove(listener);
    }

    /* access modifiers changed from: package-private */
    public void inSurfaceTransaction(Runnable exec) {
        SurfaceControl.openTransaction();
        try {
            exec.run();
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    public void disableNonVrUi(boolean disable) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                boolean showAlertWindowNotifications = !disable;
                if (showAlertWindowNotifications != this.mShowAlertWindowNotifications) {
                    this.mShowAlertWindowNotifications = showAlertWindowNotifications;
                    for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                        this.mSessions.valueAt(i).setShowingAlertWindowNotificationAllowed(this.mShowAlertWindowNotifications);
                    }
                    resetPriorityAfterLockedSection();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasWideColorGamutSupport() {
        return this.mHasWideColorGamutSupport && SystemProperties.getInt("persist.sys.sf.native_mode", 0) != 1;
    }

    /* access modifiers changed from: package-private */
    public boolean hasHdrSupport() {
        return this.mHasHdrSupport && hasWideColorGamutSupport();
    }

    /* access modifiers changed from: package-private */
    public void updateNonSystemOverlayWindowsVisibilityIfNeeded(WindowState win, boolean surfaceShown) {
        if (win.hideNonSystemOverlayWindowsWhenVisible() || this.mHidingNonSystemOverlayWindows.contains(win)) {
            boolean systemAlertWindowsHidden = !this.mHidingNonSystemOverlayWindows.isEmpty();
            if (!surfaceShown) {
                this.mHidingNonSystemOverlayWindows.remove(win);
            } else if (!this.mHidingNonSystemOverlayWindows.contains(win)) {
                this.mHidingNonSystemOverlayWindows.add(win);
            }
            boolean hideSystemAlertWindows = !this.mHidingNonSystemOverlayWindows.isEmpty();
            if (systemAlertWindowsHidden != hideSystemAlertWindows) {
                this.mRoot.forAllWindows((Consumer<WindowState>) new Consumer(hideSystemAlertWindows) {
                    /* class com.android.server.wm.$$Lambda$WindowManagerService$nQHccAXNqWhpUTYdUQi4f3vYirA */
                    private final /* synthetic */ boolean f$0;

                    {
                        this.f$0 = r1;
                    }

                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WindowState) obj).setForceHideNonSystemOverlayWindowIfNeeded(this.f$0);
                    }
                }, false);
            }
        }
    }

    public void applyMagnificationSpecLocked(int displayId, MagnificationSpec spec) {
        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
        if (displayContent != null) {
            displayContent.applyMagnificationSpec(spec);
        }
    }

    /* access modifiers changed from: package-private */
    public SurfaceControl.Builder makeSurfaceBuilder(SurfaceSession s) {
        return this.mSurfaceBuilderFactory.make(s);
    }

    /* access modifiers changed from: package-private */
    public void sendSetRunningRemoteAnimation(int pid, boolean runningRemoteAnimation) {
        this.mH.obtainMessage(59, pid, runningRemoteAnimation ? 1 : 0).sendToTarget();
    }

    /* access modifiers changed from: package-private */
    public void startSeamlessRotation() {
        this.mSeamlessRotationCount = 0;
        this.mRotatingSeamlessly = true;
    }

    /* access modifiers changed from: package-private */
    public boolean isRotatingSeamlessly() {
        return this.mRotatingSeamlessly;
    }

    /* access modifiers changed from: package-private */
    public void finishSeamlessRotation() {
        this.mRotatingSeamlessly = false;
    }

    /* access modifiers changed from: package-private */
    public void onLockTaskStateChanged(int lockTaskState) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllDisplayPolicies(PooledLambda.obtainConsumer($$Lambda$5zz5Ugt4wxIXoNE3lZS6NA9z_Jk.INSTANCE, PooledLambda.__(), Integer.valueOf(lockTaskState)));
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setAodShowing(boolean aodShowing) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mPolicy.setAodShowing(aodShowing)) {
                    this.mWindowPlacerLocked.performSurfacePlacement();
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public int getRemoteViewsPid() {
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        if (windowManagerPolicy != null) {
            return windowManagerPolicy.getRemoteViewsPid();
        }
        return -1;
    }

    public void handleAppDiedForRemoteViews() {
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        if (windowManagerPolicy != null) {
            windowManagerPolicy.handleAppDiedForRemoteViews();
        }
    }

    public void onWakefulnessChanged(boolean isAwake) {
        WindowManagerPolicy windowManagerPolicy = this.mPolicy;
        if (windowManagerPolicy != null) {
            windowManagerPolicy.onWakefulnessChanged(isAwake);
        }
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.wm.WindowManagerService$HwInnerWindowManagerService, android.os.IBinder] */
    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }

    public void reevaluateStatusBarSize(boolean layoutNaviBar) {
        this.mHwWMSEx.reevaluateStatusBarSize(layoutNaviBar);
    }

    public Configuration getCurNaviConfiguration() {
        return this.mHwWMSEx.getCurNaviConfiguration();
    }

    public IHwWindowManagerServiceEx getWindowManagerServiceEx() {
        return this.mHwWMSEx;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public WindowSurfacePlacer getWindowSurfacePlacer() {
        return this.mWindowPlacerLocked;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public H getWindowMangerServiceHandler() {
        return this.mH;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public HwWMDAMonitorProxy getWMMonitor() {
        return this.mWMProxy;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public WindowHashMap getWindowMap() {
        return this.mWindowMap;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public WindowManagerGlobalLock getGlobalLock() {
        return this.mGlobalLock;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public TaskSnapshotController getTaskSnapshotController() {
        return this.mTaskSnapshotController;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public AppWindowToken getFocusedAppWindowToken() {
        return this.mRoot.getTopFocusedDisplayContent().mFocusedApp;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public AppOpsManager getAppOps() {
        return this.mAppOps;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public WindowManagerPolicy getPolicy() {
        return this.mPolicy;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public RootWindowContainer getRoot() {
        return this.mRoot;
    }

    public void notifyFingerWinCovered(boolean covered, Rect frame) {
        IHwWindowManagerServiceEx iHwWindowManagerServiceEx = this.mHwWMSEx;
        if (iHwWindowManagerServiceEx != null) {
            iHwWindowManagerServiceEx.notifyFingerWinCovered(covered, frame);
        }
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public WindowManagerService getService() {
        return this;
    }

    public void freezeOrThawRotation(int rotation) {
        if (this.mHwWMSEx == null) {
            return;
        }
        if (checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            this.mHwWMSEx.freezeOrThawRotation(rotation);
            updateRotationUnchecked(false, false);
            return;
        }
        throw new SecurityException("Requires SET_ORIENTATION permission");
    }

    public int getLazyMode() {
        return this.mHwWMSEx.getLazyModeEx();
    }

    public void setLazyMode(int lazyMode, boolean hintShowing, String windowName) {
        this.mHwWMSEx.setLazyModeEx(lazyMode, hintShowing, windowName);
    }

    public void getAppDisplayRect(float appMaxRatio, Rect rect, int left) {
        IHwWindowManagerServiceEx iHwWindowManagerServiceEx = this.mHwWMSEx;
        if (iHwWindowManagerServiceEx != null) {
            iHwWindowManagerServiceEx.getAppDisplayRect(appMaxRatio, rect, left, getDefaultDisplayRotation());
        }
    }

    public float getDeviceMaxRatio() {
        IHwWindowManagerServiceEx iHwWindowManagerServiceEx = this.mHwWMSEx;
        if (iHwWindowManagerServiceEx != null) {
            return iHwWindowManagerServiceEx.getDeviceMaxRatio();
        }
        return 0.0f;
    }

    public class HwInnerWindowManagerService extends IHwWindowManager.Stub {
        WindowManagerService mWMS;

        HwInnerWindowManagerService(WindowManagerService wms) {
            this.mWMS = wms;
        }

        public int getRestrictedScreenHeight() {
            return this.mWMS.mPolicy.getRestrictedScreenHeight();
        }

        public boolean isWindowSupportKnuckle() {
            return this.mWMS.mPolicy.isWindowSupportKnuckle();
        }

        public boolean isNavigationBarVisible() {
            return this.mWMS.mPolicy.isNavigationBarVisible();
        }

        public void dismissKeyguardLw() {
            this.mWMS.mPolicy.dismissKeyguardLw((IKeyguardDismissCallback) null, (CharSequence) null);
        }

        public void updateAppView(RemoteViews remoteViews) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.updateAppView(remoteViews);
            }
        }

        public void removeAppView(boolean isNeedAddBtnView) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.removeAppView(isNeedAddBtnView);
            }
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
            WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy().registerRotateObserver(observer);
        }

        public void unregisterRotateObserver(IHwRotateObserver observer) {
            WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy().unregisterRotateObserver(observer);
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

        private boolean isCallingFromSystem() {
            int uid = UserHandle.getAppId(Binder.getCallingUid());
            if (uid == 1000) {
                return true;
            }
            Slog.e(WindowManagerService.TAG, "Process Permission error! uid:" + uid);
            return false;
        }

        public boolean registerWMMonitorCallback(IHwWMDAMonitorCallback callback) {
            if (!isCallingFromSystem()) {
                return false;
            }
            WindowManagerService.this.mWMProxy.registerWMMonitorCallback(callback);
            return true;
        }

        public List<Bundle> getVisibleWindows(int ops) {
            if (!isCallingFromSystem()) {
                return null;
            }
            return WindowManagerService.this.mHwWMSEx.getVisibleWindows(ops);
        }

        public int getFocusWindowWidth() {
            return WindowManagerService.this.mHwWMSEx.getFocusWindowWidth(WindowManagerService.this.getFocusedWindowLocked(), WindowManagerService.this.getDefaultDisplayContentLocked().mInputMethodTarget);
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
            if (isCallingFromSystem()) {
                WindowManagerService.this.mHwWMSEx.getCurrFocusedWinInExtDisplay(outBundle);
            }
        }

        public boolean hasLighterViewInPCCastMode() {
            if (!isCallingFromSystem()) {
                return false;
            }
            return WindowManagerService.this.mHwWMSEx.hasLighterViewInPCCastMode();
        }

        public boolean shouldDropMotionEventForTouchPad(float x, float y) {
            if (!isCallingFromSystem()) {
                return false;
            }
            return WindowManagerService.this.mHwWMSEx.shouldDropMotionEventForTouchPad(x, y);
        }

        public HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(boolean refresh) {
            return WindowManagerService.this.mHwWMSEx.getForegroundTaskSnapshotWrapper(WindowManagerService.this.mTaskSnapshotController, WindowManagerService.this.getFocusedWindow(), refresh);
        }

        public void setAppWindowExitInfo(Bundle bundle, Bitmap iconBitmap) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.setAppWindowExitInfo(bundle, iconBitmap, Binder.getCallingUid());
            }
        }

        public void setCoverManagerState(boolean isCoverOpen) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.setCoverManagerState(isCoverOpen);
            }
        }

        public void freezeOrThawRotation(int rotation) {
        }

        public Bitmap getDisplayBitmap(int displayId, int width, int height) {
            return WindowManagerService.this.mHwWMSEx.getDisplayBitmap(displayId, width, height);
        }

        public void setGestureNavMode(String packageName, int leftMode, int rightMode, int bottomMode) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.setGestureNavMode(packageName, Binder.getCallingUid(), leftMode, rightMode, bottomMode);
            }
        }

        public void setForcedDisplayDensityAndSize(int displayId, int density, int width, int height) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.setForcedDisplayDensityAndSize(displayId, density, width, height);
            }
        }

        public void notifySwingRotation(int rotation) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                WindowManagerService.this.mHwWMSEx.notifySwingRotation(rotation);
            }
        }

        public Rect getSafeInsets(int type) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.getSafeInsets(type);
            }
            return null;
        }

        public List<Rect> getBounds(int type) {
            if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.getBounds(type);
            }
            return null;
        }

        public int getTopActivityAdaptNotchState(String packageName) {
            return WindowManagerService.this.mHwWMSEx.getTopActivityAdaptNotchState(packageName);
        }

        public String getTouchedWinPackageName(float x, float y, int displayId) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "getTouchedWinPackageName check permission error.");
                return "";
            } else if (WindowManagerService.this.mHwWMSEx != null) {
                return WindowManagerService.this.mHwWMSEx.getTouchedWinPackageName(x, y, displayId);
            } else {
                return "";
            }
        }

        public boolean notifyDragAndDropForMultiDisplay(float x, float y, int displayId, DragEvent evt) {
            if (evt.getAction() != 7) {
                return WindowManagerService.this.mHwWMSEx.notifyDragAndDropForMultiDisplay(x, y, displayId, evt);
            }
            Log.d(WindowManagerService.TAG, "notifyDragAndDropForMultiDisplay switch shadow result = " + evt.getResult());
            WindowManagerService.this.mHwWMSEx.switchDragShadow(evt.getResult());
            return true;
        }

        public void registerDragListenerForMultiDisplay(IHwMultiDisplayDragStartListener listener) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "registerDragListenerForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "registerDragListenerForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.registerDragListenerForMultiDisplay(listener);
        }

        public void unregisterDragListenerForMultiDisplay() {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "unregisterDragListenerForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "unregisterDragListenerForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.unregisterDragListenerForMultiDisplay();
        }

        public void registerDropListenerForMultiDisplay(IHwMultiDisplayDropStartListener listener) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "registerDropListenerForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "registerDropListenerForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.registerDropListenerForMultiDisplay(listener);
        }

        public void unregisterDropListenerForMultiDisplay() {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "unregisterDropListenerForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "unregisterDropListenerForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.unregisterDropListenerForMultiDisplay();
        }

        public boolean dropStartForMultiDisplay(DragEvent dragEvent) {
            Log.i(WindowManagerService.TAG, "dropStartForMultiDisplay start.");
            return WindowManagerService.this.mHwWMSEx.dropStartForMultiDisplay(dragEvent);
        }

        public void registerBitmapDragListenerForMultiDisplay(IHwMultiDisplayBitmapDragStartListener listener) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "registerBitmapDragListenerForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "registerBitmapDragListenerForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.registerBitmapDragListenerForMultiDisplay(listener);
        }

        public void unregisterBitmapDragListenerForMultiDisplay() {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "unregisterBitmapDragListenerForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "unregisterBitmapDragListenerForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.unregisterBitmapDragListenerForMultiDisplay();
        }

        public boolean setDragStartBitmap(Bitmap b) {
            Log.i(WindowManagerService.TAG, "setDragStartBitmap start.");
            if (HwPCUtils.getWindowsCastDisplayId() == WindowManagerService.this.getFocusedDisplayId()) {
                return WindowManagerService.this.mHwWMSEx.setDragStartBitmap(b);
            }
            Log.d(WindowManagerService.TAG, "focused display is not cast display.");
            return false;
        }

        public boolean dragStartForMultiDisplay(ClipData clipData) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "dragStartForMultiDisplay check permission error.");
                return false;
            }
            Log.i(WindowManagerService.TAG, "dragStartForMultiDisplay start.");
            return WindowManagerService.this.mHwWMSEx.dragStartForMultiDisplay(clipData);
        }

        public void registerIsDroppableForMultiDisplay(IHwMultiDisplayDroppableListener listener) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "registerIsDroppableForMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "registerIsDroppableForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.registerIsDroppableForMultiDisplay(listener);
        }

        public void setDroppableForMultiDisplay(float x, float y, boolean result) {
            Log.i(WindowManagerService.TAG, "setDroppableForMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.setDroppableForMultiDisplay(x, y, result);
        }

        public void setOriginalDropPoint(float x, float y) {
            Log.i(WindowManagerService.TAG, "setOriginalDropPoint start.");
            WindowManagerService.this.mHwWMSEx.setOriginalDropPoint(x, y);
        }

        public void registerHwMultiDisplayDragStateListener(IHwMultiDisplayDragStateListener listener) {
            Log.i(WindowManagerService.TAG, "registerHwMultiDisplayDragStateListener start.");
            WindowManagerService.this.mHwWMSEx.registerHwMultiDisplayDragStateListener(listener);
        }

        public void unregisterHwMultiDisplayDragStateListener() {
            Log.i(WindowManagerService.TAG, "unregisterHwMultiDisplayDragStateListener start.");
            WindowManagerService.this.mHwWMSEx.unregisterHwMultiDisplayDragStateListener();
        }

        public void updateDragState(int dragState) {
            Log.i(WindowManagerService.TAG, "updateDragState start.");
            WindowManagerService.this.mHwWMSEx.updateDragState(dragState);
        }

        public void restoreShadow() {
            WindowManagerService.this.mHwWMSEx.restoreShadow();
        }

        public boolean isSwitched() {
            return WindowManagerService.this.mHwWMSEx.isSwitched();
        }

        public String getDragSrcPkgName() {
            return WindowManagerService.this.mHwWMSEx.getDragSrcPkgName();
        }

        public void updateFocusWindowFreezed(boolean isGainFocus) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "updateFocusWindowFreezed check permission error.");
            } else {
                WindowManagerService.this.mHwWMSEx.updateFocusWindowFreezed(isGainFocus);
            }
        }

        public void registerPhoneOperateListenerForHwMultiDisplay(IHwMultiDisplayPhoneOperateListener listener) {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "registerPhoneOperateListenerForHwMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "registerPhoneOperateListenerForHwMultiDisplay start.");
            WindowManagerService.this.mHwWMSEx.registerPhoneOperateListenerForHwMultiDisplay(listener);
        }

        public void unregisterPhoneOperateListenerForHwMultiDisplay() {
            if (!isCallingFromSystem()) {
                Log.e(WindowManagerService.TAG, "unregisterPhoneOperateListenerForHwMultiDisplay check permission error.");
                return;
            }
            Log.i(WindowManagerService.TAG, "unregisterPhoneOperateListenerForHwMultiDisplay.");
            WindowManagerService.this.mHwWMSEx.unregisterPhoneOperateListenerForHwMultiDisplay();
        }

        public boolean isNeedExceptDisplaySide(Rect exceptDisplayRect) {
            Task topTask;
            WindowState fullScreenWindow;
            Rect rect = HwFrameworkFactory.getHwExtDisplaySizeUtil().getDisplaySideSafeInsets();
            int lazyMode = WindowManagerService.this.getLazyMode();
            if (lazyMode == 1) {
                exceptDisplayRect.set(rect.left, 0, 0, 0);
                return true;
            } else if (lazyMode == 2) {
                exceptDisplayRect.set(0, 0, rect.right, 0);
                return true;
            } else if (WindowManagerService.this.getDefaultDisplayContentLocked().isStackVisible(3)) {
                exceptDisplayRect.set(rect);
                return true;
            } else {
                TaskStack fullScreenStack = WindowManagerService.this.mRoot.getStack(1, 1);
                if (fullScreenStack == null || (topTask = (Task) fullScreenStack.getTopChild()) == null || (fullScreenWindow = topTask.getTopVisibleAppMainWindow()) == null) {
                    return false;
                }
                DisplayPolicy defaultDisplayPolicy = WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy();
                if (!defaultDisplayPolicy.getHwDisplayPolicyEx().isNeedExceptDisplaySide(fullScreenWindow.getAttrs(), fullScreenWindow, defaultDisplayPolicy.getDisplayRotation())) {
                    return false;
                }
                exceptDisplayRect.set(rect);
                return true;
            }
        }

        public boolean isAppNeedExpand(String pkgName) {
            if (WindowManagerService.this.getDefaultDisplayContentLocked() == null || WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy() == null) {
                return true;
            }
            return WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy().isAppNeedExpand(pkgName);
        }
    }

    public void setTouchWinState(WindowState win) {
        this.mHwWMSEx.setTouchWinState(win);
    }

    public void setDragWinState(WindowState win) {
        this.mHwWMSEx.setDragWinState(win);
    }

    public boolean isInDisplayFrozen() {
        return this.mDisplayFrozen;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public InputManagerService getInputManager() {
        return this.mInputManager;
    }

    public Rect getFocuseWindowVisibleFrame() {
        return this.mHwWMSEx.getFocuseWindowVisibleFrame(this);
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public WindowAnimator getWindowAnimator() {
        return this.mAnimator;
    }

    @Override // com.android.server.wm.IHwWindowManagerInner
    public DisplayManagerInternal getDisplayManagerInternal() {
        return this.mDisplayManagerInternal;
    }

    public boolean isPendingLock() {
        return this.mPolicy.isPendingLock();
    }

    public Point updateLazyModePoint(int type, Point point) {
        return this.mHwWMSEx.updateLazyModePoint(type, point);
    }

    public boolean isScreenFolding() {
        Bundle bundle = this.mFoldScreenInfo;
        return bundle != null && bundle.getBoolean(IS_FOLD_KEY);
    }

    public int getFromFoldDisplayMode() {
        Bundle bundle = this.mFoldScreenInfo;
        if (bundle != null) {
            return bundle.getInt(FROM_FOLD_MODE_KEY);
        }
        return 0;
    }

    public int getToFoldDisplayMode() {
        Bundle bundle = this.mFoldScreenInfo;
        if (bundle != null) {
            return bundle.getInt(TO_FOLD_MODE_KEY);
        }
        return 0;
    }

    public boolean isFoldRotationFreezed() {
        return this.mIsFoldRotationFreezed;
    }

    public void setFoldRotationFreezed(boolean freezed) {
        this.mIsFoldRotationFreezed = freezed;
    }

    public float getLazyModeScale() {
        return this.mHwWMSEx.getLazyModeScale();
    }

    public boolean isInSubFoldScaleMode() {
        return getFoldDisplayMode() == 3;
    }

    public int getFoldDisplayMode() {
        return this.mCurrentFoldDisplayMode;
    }

    public boolean injectInputAfterTransactionsApplied(InputEvent ev, int mode) {
        boolean isDown;
        KeyEvent keyEvent;
        KeyEvent keyEvent2 = null;
        if (ev instanceof KeyEvent) {
            KeyEvent keyEvent3 = (KeyEvent) ev;
            isDown = keyEvent3.getAction() == 0;
            if (keyEvent3.getAction() == 1) {
                keyEvent2 = 1;
            }
            keyEvent = keyEvent2;
        } else {
            MotionEvent motionEvent = (MotionEvent) ev;
            isDown = motionEvent.getAction() == 0;
            if (motionEvent.getAction() == 1) {
                keyEvent2 = 1;
            }
            keyEvent = keyEvent2;
        }
        if (isDown) {
            syncInputTransactions();
        }
        boolean result = ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).injectInputEvent(ev, mode);
        if (keyEvent != null) {
            syncInputTransactions();
        }
        return result;
    }

    /* JADX INFO: finally extract failed */
    public void syncInputTransactions() {
        waitForAnimationsToComplete();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWindowPlacerLocked.performSurfacePlacementIfScheduled();
                this.mRoot.forAllDisplays($$Lambda$WindowManagerService$QGTApvQkj7JVfTvOVrLJ6s24v8.INSTANCE);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        new SurfaceControl.Transaction().syncInputWindows().apply(true);
    }

    private void waitForAnimationsToComplete() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                long timeoutRemaining = 5000;
                while (this.mRoot.isSelfOrChildAnimating() && timeoutRemaining > 0) {
                    long startTime = System.currentTimeMillis();
                    try {
                        this.mGlobalLock.wait(timeoutRemaining);
                    } catch (InterruptedException e) {
                    }
                    timeoutRemaining -= System.currentTimeMillis() - startTime;
                }
                if (this.mRoot.isSelfOrChildAnimating()) {
                    Log.w(TAG, "Timed out waiting for animations to complete.");
                }
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void onAnimationFinished() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mGlobalLock.notifyAll();
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: private */
    public void onPointerDownOutsideFocusLocked(IBinder touchedToken) {
        WindowState touchedWindow = windowForClientLocked((Session) null, touchedToken, false);
        if (touchedWindow == null) {
            return;
        }
        if (!touchedWindow.canReceiveKeys() && !touchedWindow.inHwMagicWindowingMode()) {
            return;
        }
        if (HwMwUtils.ENABLED && getFocusedWindow() != null && getFocusedWindow().inHwMagicWindowingMode() && touchedWindow.getTask() != null && getFocusedWindow().getTask() == touchedWindow.getTask()) {
            HwMwUtils.performPolicy(5, new Object[]{touchedWindow.mAppToken.token});
        } else if (!HwPCUtils.isPcCastModeInServer() || touchedWindow.mAppToken == null || touchedWindow.mAppToken.appComponentName == null || !"com.huawei.desktop.systemui/com.huawei.systemui.mk.activity.ImitateActivity".equalsIgnoreCase(touchedWindow.mAppToken.appComponentName)) {
            if (HwPCUtils.isPcCastModeInServer()) {
                boolean oldPCLauncherFocused = getPCLauncherFocused();
                setPCLauncherFocused(false);
                DisplayContent displayContent = touchedWindow.getDisplayContent();
                if (touchedWindow.getAttrs().getTitle().equals(WINDOW_TITLE_PC_LAUNCHER)) {
                    setPCLauncherFocused(true ^ displayContent.isDefaultDisplay);
                }
                if (oldPCLauncherFocused != getPCLauncherFocused()) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            displayContent.layoutAndAssignWindowLayersIfNeeded();
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                }
            }
            handleTaskFocusChange(touchedWindow.getTask());
            handleDisplayFocusChange(touchedWindow);
        }
    }

    private void handleTaskFocusChange(Task task) {
        if (task != null && !task.mStack.isActivityTypeHome()) {
            try {
                this.mActivityTaskManager.setFocusedTask(task.mTaskId);
            } catch (RemoteException e) {
            }
        }
    }

    private void handleDisplayFocusChange(WindowState window) {
        WindowContainer parent;
        DisplayContent displayContent = window.getDisplayContent();
        if (displayContent != null && window.canReceiveKeys() && (parent = displayContent.getParent()) != null && parent.getTopChild() != displayContent) {
            parent.positionChildAt(Integer.MAX_VALUE, displayContent, true);
            displayContent.mAcitvityDisplay.ensureActivitiesVisible(null, 0, false, true);
        }
    }

    public void setDockedStackDividerRotation(int rotation) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getDefaultDisplayContentLocked().getDockedDividerController().setDockedStackDividerRotation(rotation);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }

    public void setSplitScreenResizing(boolean resizing) {
        this.mResizing = resizing;
    }

    public boolean isResizing() {
        return this.mResizing;
    }

    /* JADX INFO: finally extract failed */
    public WindowState getWallpaperWindowState() {
        WindowState windowState = this.mWallpaperWindow;
        if (windowState != null) {
            return windowState;
        }
        ArrayList<WindowState> windows = new ArrayList<>();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllWindows((Consumer<WindowState>) new Consumer(windows) {
                    /* class com.android.server.wm.$$Lambda$WindowManagerService$0tz9QNMjJ49OnAVVq4bueYdjLU */
                    private final /* synthetic */ ArrayList f$0;

                    {
                        this.f$0 = r1;
                    }

                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WindowManagerService.lambda$getWallpaperWindowState$13(this.f$0, (WindowState) obj);
                    }
                }, false);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (windows.size() != 0) {
            this.mWallpaperWindow = windows.get(0);
        }
        return this.mWallpaperWindow;
    }

    static /* synthetic */ void lambda$getWallpaperWindowState$13(ArrayList windows, WindowState w) {
        if (w != null && w.mIsWallpaper) {
            windows.add(w);
        }
    }

    private boolean shouldAddSurfaceBox() {
        String pkgName;
        WindowState win;
        AppWindowToken topApp = getFocusedAppWindowToken();
        if (topApp == null || topApp.isActivityTypeHome() || topApp.getWindowingMode() != 1 || (pkgName = topApp.appPackageName) == null || (win = topApp.findMainWindow(false)) == null) {
            return false;
        }
        int rotation = getDefaultDisplayRotation();
        if (rotation == 1 || rotation == 3) {
            return true;
        }
        return isNeedExceptDisplaySide(win, pkgName);
    }

    private boolean isFocusAppChange() {
        AppWindowToken topApp = getFocusedAppWindowToken();
        if (topApp == null) {
            return false;
        }
        if (topApp != this.mLastFocusedApp) {
            return true;
        }
        this.mLastFocusedApp = topApp;
        return false;
    }

    public boolean isNeedExceptDisplaySide(WindowState win, String pkgName) {
        int sideMode = win.getAttrs().layoutInDisplaySideMode;
        if (!isSystemApp(pkgName)) {
            return !getDefaultDisplayContentLocked().getDisplayPolicy().isAppNeedExpand(pkgName);
        }
        if (sideMode != 1) {
            return true;
        }
        return false;
    }

    private boolean isSystemApp(String pkgName) {
        IHwWindowManagerServiceEx iHwWindowManagerServiceEx;
        if (this.mSystemAppPkgs.isEmpty() && (iHwWindowManagerServiceEx = this.mHwWMSEx) != null) {
            this.mSystemAppPkgs = iHwWindowManagerServiceEx.getNotchSystemApps();
        }
        for (String pkg : this.mSystemAppPkgs) {
            if (pkgName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    public void showOrHideInsetSurface(MotionEvent event) {
        if (HwDisplaySizeUtil.hasSideInScreen()) {
            if (this.mAddValue == -1 || isFocusAppChange()) {
                this.mAddValue = shouldAddSurfaceBox() ? 1 : 0;
            }
            if (this.mAddValue != 0 && getWallpaperWindowState() != null) {
                int action = event.getActionMasked();
                boolean isStartAnimation = true;
                if (action != 0) {
                    if (action != 1) {
                        if (action == 2) {
                            if (Math.abs(((int) event.getY()) - this.mInitDownY) <= 30) {
                                isStartAnimation = false;
                            }
                            Log.d("InsetSurface", "motion move " + isStartAnimation);
                            if (isStartAnimation && getWallpaperWindowState() != null) {
                                this.mWallpaperWindow.destroyInsetSurfaceImmediately(false);
                                return;
                            }
                            return;
                        } else if (action != 3) {
                            return;
                        }
                    }
                    boolean isCancelAnimation = Math.abs(((int) event.getY()) - this.mInitDownY) < 30;
                    Log.d("InsetSurface", "motion up " + isCancelAnimation);
                    if (isCancelAnimation) {
                        this.mWallpaperWindow.setIsToShowSideSurfaceBox(false);
                        this.mWallpaperWindow = null;
                    }
                    if (!isCancelAnimation && 0 == 0 && getWallpaperWindowState() != null) {
                        this.mWallpaperWindow.destroyInsetSurfaceImmediately(true);
                    }
                    if (!isCancelAnimation) {
                        this.mH.removeMessages(H.GESTURE_NAVUP_TIMEOUT);
                    }
                    this.mAddValue = -1;
                    return;
                }
                this.mInitDownY = (int) event.getY();
                Log.d("InsetSurface", "motion down");
                this.mH.removeMessages(H.GESTURE_NAVUP_TIMEOUT);
                this.mH.sendEmptyMessageDelayed(H.GESTURE_NAVUP_TIMEOUT, 450);
                this.mWallpaperWindow.setIsToShowSideSurfaceBox(true);
            }
        }
    }

    public void clearAddValueState() {
        this.mAddValue = -1;
    }

    public void handlePauseDispModeChange() {
        if (HwFoldScreenState.isFoldScreenDevice() && PROP_FOLD_SWITCH_DEBUG) {
            if (this.mFsmInternal == null) {
                this.mFsmInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
            }
            if (this.mFsmInternal != null) {
                Slog.d(TAG, "pauseDispModeChange in WMS");
                this.mFsmInternal.pauseDispModeChange();
            }
        }
    }

    public void handleResumeDispModeChange() {
        if (HwFoldScreenState.isFoldScreenDevice() && PROP_FOLD_SWITCH_DEBUG) {
            if (this.mFsmInternal == null) {
                this.mFsmInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
            }
            HwFoldScreenManagerInternal hwFoldScreenManagerInternal = this.mFsmInternal;
            if (hwFoldScreenManagerInternal != null && hwFoldScreenManagerInternal.isPausedDispModeChange()) {
                Slog.d(TAG, "resumeDispModeChange in WMS");
                this.mFsmInternal.resumeDispModeChange();
            }
        }
    }

    public void disableEventDispatching(long enableDelayMillis) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mIsFoldDisableEventDispatching = true;
                this.mInputManagerCallback.setEventDispatchingLw(false);
                this.mH.removeMessages(H.ENABLE_EVENT_DISPATCHING);
                this.mH.sendEmptyMessageDelayed(H.ENABLE_EVENT_DISPATCHING, enableDelayMillis);
            } finally {
                resetPriorityAfterLockedSection();
            }
        }
    }
}
