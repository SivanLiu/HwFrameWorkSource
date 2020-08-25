package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.app.ResourcesManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.freeform.HwFreeFormUtils;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.HwFoldScreenState;
import android.hardware.input.InputManager;
import android.hdm.HwDeviceManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.CoordinationModeUtils;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.IApplicationToken;
import android.view.IHwRotateObserver;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.policy.ScreenDecorationsUtils;
import com.android.internal.util.ScreenShapeHelper;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.widget.PointerLocationView;
import com.android.server.HwServiceExFactory;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.policy.IHwPhoneWindowManagerEx;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowOrientationListener;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wallpaper.WallpaperManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.BarController;
import com.android.server.wm.SystemGesturesPointerEventListener;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.android.server.wm.utils.InsetUtils;
import com.huawei.server.wm.IHwDisplayPolicyEx;
import com.huawei.server.wm.IHwDisplayPolicyInner;
import java.io.PrintWriter;

public class DisplayPolicy implements IHwDisplayPolicyInner {
    private static final boolean ALTERNATE_CAR_MODE_NAV_SIZE = false;
    private static final boolean DEBUG = false;
    private static final boolean IS_HW_MULTIWINDOW_SUPPORTED = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    private static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    private static final int MSG_DISABLE_POINTER_LOCATION = 5;
    private static final int MSG_DISPOSE_INPUT_CONSUMER = 3;
    private static final int MSG_ENABLE_POINTER_LOCATION = 4;
    private static final int MSG_REQUEST_TRANSIENT_BARS = 2;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_NAVIGATION = 1;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_STATUS = 0;
    private static final int MSG_UPDATE_DREAMING_SLEEP_TOKEN = 1;
    private static final int NAV_BAR_FORCE_TRANSPARENT = 2;
    private static final int NAV_BAR_OPAQUE_WHEN_FREEFORM_OR_DOCKED = 0;
    private static final int NAV_BAR_TRANSLUCENT_WHEN_FREEFORM_OPAQUE_OTHERWISE = 1;
    private static final int NaviHide = 1;
    private static final int NaviInit = -1;
    private static final int NaviShow = 0;
    private static final int NaviTransientShow = 2;
    private static final long PANIC_GESTURE_EXPIRATION = 30000;
    private static final String SEC_IME_PACKAGE = "com.huawei.secime";
    private static final int SEC_IME_RAISE_HEIGHT = SystemProperties.getInt("ro.config.sec_ime_raise_height_dp", 140);
    private static final int SYSTEM_UI_CHANGING_LAYOUT = -1073709042;
    private static final String TAG = "WindowManager";
    private static final boolean mIsHwNaviBar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    private static boolean mUsingHwNavibar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    private static final Rect sTmpDisplayCutoutSafeExceptMaybeBarsRect = new Rect();
    private static final Rect sTmpDockedFrame = new Rect();
    private static final Rect sTmpLastParentFrame = new Rect();
    private static final Rect sTmpNavFrame = new Rect();
    private static final Rect sTmpRect = new Rect();
    private boolean isHwFullScreenWinVisibility;
    /* access modifiers changed from: private */
    public final AccessibilityManager mAccessibilityManager;
    private final Runnable mAcquireSleepTokenRunnable;
    private boolean mAllowLockscreenWhenOn;
    private volatile boolean mAllowSeamlessRotationDespiteNavBarMoving;
    private WindowState mAloneTarget;
    private volatile boolean mAwake;
    private int mBottomGestureAdditionalInset;
    private final boolean mCarDockEnablesAccelerometer;
    /* access modifiers changed from: private */
    public final Runnable mClearHideNavigationFlag;
    private final Context mContext;
    private Resources mCurrentUserResources;
    private final boolean mDeskDockEnablesAccelerometer;
    /* access modifiers changed from: private */
    public final DisplayContent mDisplayContent;
    int mDisplayRotation;
    private volatile int mDockMode = 0;
    private final Rect mDockedStackBounds;
    private boolean mDreamingLockscreen;
    @GuardedBy({"mHandler"})
    private ActivityTaskManagerInternal.SleepToken mDreamingSleepToken;
    private boolean mDreamingSleepTokenNeeded;
    IApplicationToken mFocusedApp;
    /* access modifiers changed from: private */
    public WindowState mFocusedWindow;
    /* access modifiers changed from: private */
    public int mForceClearedSystemUiFlags;
    protected boolean mForceNotchStatusBar = false;
    private boolean mForceShowSystemBars;
    private boolean mForceShowSystemBarsFromExternal;
    private boolean mForceStatusBar;
    private boolean mForceStatusBarFromKeyguard;
    private boolean mForceStatusBarTransparent;
    private boolean mForcingShowNavBar;
    private int mForcingShowNavBarLayer;
    private Insets mForwardedInsets;
    private boolean mFrozen;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private volatile boolean mHasNavigationBar;
    private volatile boolean mHasStatusBar;
    private volatile boolean mHdmiPlugged;
    private final Runnable mHiddenNavPanic;
    protected IHwDisplayPolicyEx mHwDisplayPolicyEx;
    private WindowState mHwFullScreenWindow;
    private boolean mHwNavColor;
    private final RemoteCallbackList<IHwRotateObserver> mHwRotateObservers = new RemoteCallbackList<>();
    private final ImmersiveModeConfirmation mImmersiveModeConfirmation;
    /* access modifiers changed from: private */
    public WindowManagerPolicy.InputConsumer mInputConsumer;
    private WindowState mInputMethodTarget;
    private volatile boolean mKeyguardDrawComplete;
    private final Rect mLastDockedStackBounds;
    private int mLastDockedStackSysUiFlags;
    private boolean mLastFocusNeedsMenu;
    private WindowState mLastFocusedWindow;
    private int mLastFullscreenStackSysUiFlags;
    int mLastHideNaviDockBottom;
    private boolean mLastHwNavColor;
    int mLastNaviStatus;
    private final Rect mLastNonDockedStackBounds;
    int mLastShowNaviDockBottom;
    private boolean mLastShowingDream;
    private WindowState mLastStartingWindow;
    int mLastSystemUiFlags;
    int mLastTransientNaviDockBottom;
    private boolean mLastWindowSleepTokenNeeded;
    private volatile int mLidState = -1;
    /* access modifiers changed from: private */
    public final Object mLock;
    private int mNavBarOpacityMode;
    private final BarController.OnBarVisibilityChangedListener mNavBarVisibilityListener;
    /* access modifiers changed from: private */
    public WindowState mNavigationBar;
    /* access modifiers changed from: private */
    public volatile boolean mNavigationBarAlwaysShowOnSideGesture;
    private volatile boolean mNavigationBarCanMove;
    /* access modifiers changed from: private */
    public final BarController mNavigationBarController;
    private int[] mNavigationBarFrameHeightForRotationDefault;
    private int[] mNavigationBarHeightForRotationDefault;
    private int[] mNavigationBarHeightForRotationInCarMode;
    private volatile boolean mNavigationBarLetsThroughTaps;
    /* access modifiers changed from: private */
    public int mNavigationBarPosition;
    private int[] mNavigationBarWidthForRotationDefault;
    private int[] mNavigationBarWidthForRotationInCarMode;
    private final Rect mNonDockedStackBounds;
    protected int mNotchStatusBarColorLw = 0;
    /* access modifiers changed from: private */
    public long mPendingPanicGestureUptime;
    private volatile boolean mPersistentVrModeEnabled;
    private PointerLocationView mPointerLocationView;
    private RefreshRatePolicy mRefreshRatePolicy;
    private final Runnable mReleaseSleepTokenRunnable;
    /* access modifiers changed from: private */
    public int mResettingSystemUiFlags;
    int mRestrictedScreenHeight;
    private final ArraySet<WindowState> mScreenDecorWindows = new ArraySet<>();
    private volatile boolean mScreenOnEarly;
    private volatile boolean mScreenOnFully;
    private volatile WindowManagerPolicy.ScreenOnListener mScreenOnListener;
    private final ScreenshotHelper mScreenshotHelper;
    /* access modifiers changed from: private */
    public final WindowManagerService mService;
    private final Object mServiceAcquireLock = new Object();
    private boolean mShowingDream;
    private int mSideGestureInset;
    /* access modifiers changed from: private */
    public WindowState mStatusBar;
    private final StatusBarController mStatusBarController;
    private final int[] mStatusBarHeightForRotation;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    /* access modifiers changed from: private */
    public final SystemGesturesPointerEventListener mSystemGestures;
    private WindowState mTopDockedOpaqueOrDimmingWindowState;
    private WindowState mTopDockedOpaqueWindowState;
    private WindowState mTopFullscreenOpaqueOrDimmingWindowState;
    /* access modifiers changed from: private */
    public WindowState mTopFullscreenOpaqueWindowState;
    /* access modifiers changed from: private */
    public boolean mTopIsFullscreen;
    private volatile boolean mWindowManagerDrawComplete;
    private int mWindowOutsetBottom;
    @GuardedBy({"mHandler"})
    private ActivityTaskManagerInternal.SleepToken mWindowSleepToken;
    private boolean mWindowSleepTokenNeeded;
    boolean notchWindowChange = false;
    boolean notchWindowChangeState = false;

    static /* synthetic */ int access$1972(DisplayPolicy x0, int x1) {
        int i = x0.mForceClearedSystemUiFlags & x1;
        x0.mForceClearedSystemUiFlags = i;
        return i;
    }

    private StatusBarManagerInternal getStatusBarManagerInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAcquireLock) {
            if (this.mStatusBarManagerInternal == null) {
                this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarManagerInternal;
        }
        return statusBarManagerInternal;
    }

    private class PolicyHandler extends Handler {
        PolicyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            boolean z = true;
            if (i == 1) {
                DisplayPolicy displayPolicy = DisplayPolicy.this;
                if (msg.arg1 == 0) {
                    z = false;
                }
                displayPolicy.updateDreamingSleepToken(z);
            } else if (i == 2) {
                WindowState targetBar = msg.arg1 == 0 ? DisplayPolicy.this.mStatusBar : DisplayPolicy.this.mNavigationBar;
                if (targetBar != null) {
                    DisplayPolicy.this.requestTransientBars(targetBar);
                }
            } else if (i == 3) {
                DisplayPolicy.this.disposeInputConsumer((WindowManagerPolicy.InputConsumer) msg.obj);
            } else if (i == 4) {
                DisplayPolicy.this.enablePointerLocation();
            } else if (i == 5) {
                DisplayPolicy.this.disablePointerLocation();
            }
        }
    }

    DisplayPolicy(WindowManagerService service, DisplayContent displayContent) {
        Context context;
        ScreenshotHelper screenshotHelper = null;
        this.mStatusBar = null;
        this.mStatusBarHeightForRotation = new int[4];
        this.mNavigationBar = null;
        this.mNavigationBarPosition = 4;
        this.mNavigationBarHeightForRotationDefault = new int[4];
        this.mNavigationBarWidthForRotationDefault = new int[4];
        this.mNavigationBarHeightForRotationInCarMode = new int[4];
        this.mNavigationBarWidthForRotationInCarMode = new int[4];
        this.mNavigationBarFrameHeightForRotationDefault = new int[4];
        this.mNavBarVisibilityListener = new BarController.OnBarVisibilityChangedListener() {
            /* class com.android.server.wm.DisplayPolicy.AnonymousClass1 */

            @Override // com.android.server.wm.BarController.OnBarVisibilityChangedListener
            public void onBarVisibilityChanged(boolean visible) {
                if (DisplayPolicy.this.mAccessibilityManager != null) {
                    DisplayPolicy.this.mAccessibilityManager.notifyAccessibilityButtonVisibilityChanged(visible);
                }
            }
        };
        this.mResettingSystemUiFlags = 0;
        this.mForceClearedSystemUiFlags = 0;
        this.mNonDockedStackBounds = new Rect();
        this.mDockedStackBounds = new Rect();
        this.mLastNonDockedStackBounds = new Rect();
        this.mLastDockedStackBounds = new Rect();
        this.mLastFocusNeedsMenu = false;
        this.mNavBarOpacityMode = 0;
        this.mInputConsumer = null;
        this.mForwardedInsets = Insets.NONE;
        this.mLastShowNaviDockBottom = 0;
        this.mLastHideNaviDockBottom = 0;
        this.mLastTransientNaviDockBottom = 0;
        this.mLastNaviStatus = -1;
        this.mHwFullScreenWindow = null;
        this.isHwFullScreenWinVisibility = false;
        this.mClearHideNavigationFlag = new Runnable() {
            /* class com.android.server.wm.DisplayPolicy.AnonymousClass3 */

            public void run() {
                synchronized (DisplayPolicy.this.mLock) {
                    DisplayPolicy.access$1972(DisplayPolicy.this, -3);
                    DisplayPolicy.this.mDisplayContent.reevaluateStatusBarVisibility();
                }
            }
        };
        this.mFrozen = false;
        this.mLastHwNavColor = false;
        this.mHwNavColor = false;
        this.mHiddenNavPanic = new Runnable() {
            /* class com.android.server.wm.DisplayPolicy.AnonymousClass4 */

            public void run() {
                synchronized (DisplayPolicy.this.mLock) {
                    if (DisplayPolicy.this.mService.mPolicy.isUserSetupComplete()) {
                        long unused = DisplayPolicy.this.mPendingPanicGestureUptime = SystemClock.uptimeMillis();
                        if (!DisplayPolicy.isNavBarEmpty(DisplayPolicy.this.mLastSystemUiFlags)) {
                            DisplayPolicy.this.mNavigationBarController.showTransient();
                        }
                    }
                }
            }
        };
        this.mService = service;
        if (displayContent.isDefaultDisplay) {
            context = service.mContext;
        } else {
            context = service.mContext.createDisplayContext(displayContent.getDisplay());
        }
        this.mContext = context;
        this.mDisplayContent = displayContent;
        this.mLock = service.getWindowManagerLock();
        int displayId = displayContent.getDisplayId();
        this.mStatusBarController = new StatusBarController(displayId);
        this.mNavigationBarController = new BarController("NavigationBar", displayId, 134217728, 536870912, Integer.MIN_VALUE, 2, 134217728, 32768);
        Resources r = this.mContext.getResources();
        this.mCarDockEnablesAccelerometer = r.getBoolean(17891386);
        this.mDeskDockEnablesAccelerometer = r.getBoolean(17891401);
        this.mForceShowSystemBarsFromExternal = r.getBoolean(17891457);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        if (!displayContent.isDefaultDisplay) {
            this.mAwake = true;
            this.mScreenOnEarly = true;
            this.mScreenOnFully = true;
        }
        Looper looper = UiThread.getHandler().getLooper();
        this.mHandler = new PolicyHandler(looper);
        this.mSystemGestures = new SystemGesturesPointerEventListener(this.mContext, this.mHandler, new SystemGesturesPointerEventListener.Callbacks() {
            /* class com.android.server.wm.DisplayPolicy.AnonymousClass2 */

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromTop() {
                if (!DisplayPolicy.this.mHwDisplayPolicyEx.isGestureIsolated(DisplayPolicy.this.mFocusedWindow, DisplayPolicy.this.mTopFullscreenOpaqueWindowState)) {
                    if (!DisplayPolicy.this.mTopIsFullscreen || !HwDeviceManager.disallowOp(71)) {
                        DisplayPolicy.this.mHwDisplayPolicyEx.showTopBar(DisplayPolicy.this.mHandler, DisplayPolicy.this.getDisplayId());
                        if (!DisplayPolicy.this.swipeFromTop() && DisplayPolicy.this.mStatusBar != null) {
                            DisplayPolicy displayPolicy = DisplayPolicy.this;
                            displayPolicy.requestTransientBars(displayPolicy.mStatusBar);
                        }
                    }
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromBottom() {
                if (!DisplayPolicy.this.mHwDisplayPolicyEx.isGestureIsolated(DisplayPolicy.this.mFocusedWindow, DisplayPolicy.this.mTopFullscreenOpaqueWindowState) && !DisplayPolicy.this.mHwDisplayPolicyEx.swipeFromBottom() && DisplayPolicy.this.mNavigationBar != null && DisplayPolicy.this.mNavigationBarPosition == 4) {
                    DisplayPolicy displayPolicy = DisplayPolicy.this;
                    displayPolicy.requestTransientBars(displayPolicy.mNavigationBar);
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromRight() {
                Region excludedRegion;
                if (!DisplayPolicy.this.mHwDisplayPolicyEx.isGestureIsolated(DisplayPolicy.this.mFocusedWindow, DisplayPolicy.this.mTopFullscreenOpaqueWindowState) && !DisplayPolicy.this.mHwDisplayPolicyEx.swipeFromRight()) {
                    synchronized (DisplayPolicy.this.mLock) {
                        excludedRegion = DisplayPolicy.this.mDisplayContent.calculateSystemGestureExclusion();
                    }
                    boolean sideAllowed = DisplayPolicy.this.mNavigationBarAlwaysShowOnSideGesture || DisplayPolicy.this.mNavigationBarPosition == 2;
                    if (DisplayPolicy.this.mNavigationBar != null && sideAllowed && !DisplayPolicy.this.mSystemGestures.currentGestureStartedInRegion(excludedRegion)) {
                        DisplayPolicy displayPolicy = DisplayPolicy.this;
                        displayPolicy.requestTransientBars(displayPolicy.mNavigationBar);
                    }
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromLeft() {
                Region excludedRegion;
                if (!DisplayPolicy.this.mHwDisplayPolicyEx.swipeFromLeft()) {
                    synchronized (DisplayPolicy.this.mLock) {
                        excludedRegion = DisplayPolicy.this.mDisplayContent.calculateSystemGestureExclusion();
                    }
                    boolean sideAllowed = true;
                    if (!DisplayPolicy.this.mNavigationBarAlwaysShowOnSideGesture && DisplayPolicy.this.mNavigationBarPosition != 1) {
                        sideAllowed = false;
                    }
                    if (DisplayPolicy.this.mNavigationBar != null && sideAllowed && !DisplayPolicy.this.mSystemGestures.currentGestureStartedInRegion(excludedRegion)) {
                        DisplayPolicy displayPolicy = DisplayPolicy.this;
                        displayPolicy.requestTransientBars(displayPolicy.mNavigationBar);
                    }
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onFling(int duration) {
                if (DisplayPolicy.this.mService.mPowerManagerInternal != null) {
                    DisplayPolicy.this.mService.mPowerManagerInternal.powerHint(2, duration);
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onDebug() {
            }

            private WindowOrientationListener getOrientationListener() {
                DisplayRotation rotation = DisplayPolicy.this.mDisplayContent.getDisplayRotation();
                if (rotation != null) {
                    return rotation.getOrientationListener();
                }
                return null;
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onDown() {
                WindowOrientationListener listener = getOrientationListener();
                if (listener != null) {
                    listener.onTouchStart();
                }
                DisplayPolicy.this.mHwDisplayPolicyEx.onPointDown();
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onUpOrCancel() {
                WindowOrientationListener listener = getOrientationListener();
                if (listener != null) {
                    listener.onTouchEnd();
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onMouseHoverAtTop() {
                DisplayPolicy.this.mHandler.removeMessages(2);
                Message msg = DisplayPolicy.this.mHandler.obtainMessage(2);
                msg.arg1 = 0;
                DisplayPolicy.this.mHandler.sendMessageDelayed(msg, 500);
                DisplayPolicy.this.mHwDisplayPolicyEx.showTopBar(DisplayPolicy.this.mHandler, DisplayPolicy.this.getDisplayId());
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onMouseHoverAtBottom() {
                DisplayPolicy.this.mHandler.removeMessages(2);
                Message msg = DisplayPolicy.this.mHandler.obtainMessage(2);
                msg.arg1 = 1;
                DisplayPolicy.this.mHandler.sendMessageDelayed(msg, 500);
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onMouseLeaveFromEdge() {
                DisplayPolicy.this.mHandler.removeMessages(2);
            }
        });
        this.mSystemGestures.setDisplayContent(this.mDisplayContent);
        displayContent.registerPointerEventListener(this.mSystemGestures);
        displayContent.mAppTransition.registerListenerLocked(this.mStatusBarController.getAppTransitionListener());
        this.mImmersiveModeConfirmation = new ImmersiveModeConfirmation(this.mContext, looper, this.mService.mVrModeEnabled);
        this.mAcquireSleepTokenRunnable = new Runnable(service, displayId) {
            /* class com.android.server.wm.$$Lambda$DisplayPolicy$j3sY1jb4WFF_F3wOT9D2fB2mOts */
            private final /* synthetic */ WindowManagerService f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                DisplayPolicy.this.lambda$new$0$DisplayPolicy(this.f$1, this.f$2);
            }
        };
        this.mReleaseSleepTokenRunnable = new Runnable() {
            /* class com.android.server.wm.$$Lambda$DisplayPolicy$_FsvHpVUigbWmSpT009cJNNmgM */

            public final void run() {
                DisplayPolicy.this.lambda$new$1$DisplayPolicy();
            }
        };
        this.mScreenshotHelper = displayContent.isDefaultDisplay ? new ScreenshotHelper(this.mContext) : screenshotHelper;
        this.mHwDisplayPolicyEx = HwServiceExFactory.getHwDisplayPolicyEx(this.mService, this, this.mDisplayContent, this.mContext, displayContent.isDefaultDisplay);
        if (this.mDisplayContent.isDefaultDisplay) {
            this.mHasStatusBar = true;
            this.mHasNavigationBar = this.mContext.getResources().getBoolean(17891513);
            String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                this.mHasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                this.mHasNavigationBar = true;
            }
        } else {
            this.mHasStatusBar = false;
            this.mHasNavigationBar = this.mDisplayContent.supportsSystemDecorations();
        }
        this.mRefreshRatePolicy = new RefreshRatePolicy(this.mService, this.mDisplayContent.getDisplayInfo(), this.mService.mHighRefreshRateBlacklist);
    }

    public /* synthetic */ void lambda$new$0$DisplayPolicy(WindowManagerService service, int displayId) {
        if (this.mWindowSleepToken == null) {
            ActivityTaskManagerInternal activityTaskManagerInternal = service.mAtmInternal;
            this.mWindowSleepToken = activityTaskManagerInternal.acquireSleepToken("WindowSleepTokenOnDisplay" + displayId, displayId);
        }
    }

    public /* synthetic */ void lambda$new$1$DisplayPolicy() {
        ActivityTaskManagerInternal.SleepToken sleepToken = this.mWindowSleepToken;
        if (sleepToken != null) {
            sleepToken.release();
            this.mWindowSleepToken = null;
        }
    }

    /* access modifiers changed from: package-private */
    public void systemReady() {
        this.mSystemGestures.systemReady();
        this.mHwDisplayPolicyEx.systemReadyEx();
        if (this.mService.mPointerLocationEnabled) {
            if (!(HwPCUtils.enabledInPad() && "HUAWEI PAD PC Display".equals(this.mDisplayContent.getDisplayInfo().name))) {
                setPointerLocationEnabled(true);
            }
        }
    }

    /* access modifiers changed from: private */
    public int getDisplayId() {
        return this.mDisplayContent.getDisplayId();
    }

    public void setHdmiPlugged(boolean plugged) {
        setHdmiPlugged(plugged, false);
    }

    public void setHdmiPlugged(boolean plugged, boolean force) {
        if (force || this.mHdmiPlugged != plugged) {
            this.mHdmiPlugged = plugged;
            this.mService.updateRotation(true, true);
            Intent intent = new Intent("android.intent.action.HDMI_PLUGGED");
            intent.addFlags(67108864);
            intent.putExtra("state", plugged);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isHdmiPlugged() {
        return this.mHdmiPlugged;
    }

    /* access modifiers changed from: package-private */
    public boolean isCarDockEnablesAccelerometer() {
        return this.mCarDockEnablesAccelerometer;
    }

    /* access modifiers changed from: package-private */
    public boolean isDeskDockEnablesAccelerometer() {
        return this.mDeskDockEnablesAccelerometer;
    }

    public void setPersistentVrModeEnabled(boolean persistentVrModeEnabled) {
        this.mPersistentVrModeEnabled = persistentVrModeEnabled;
    }

    public boolean isPersistentVrModeEnabled() {
        return this.mPersistentVrModeEnabled;
    }

    public void setDockMode(int dockMode) {
        this.mDockMode = dockMode;
    }

    public int getDockMode() {
        return this.mDockMode;
    }

    /* access modifiers changed from: package-private */
    public void setForceShowSystemBars(boolean forceShowSystemBars) {
        this.mForceShowSystemBarsFromExternal = forceShowSystemBars;
    }

    public boolean hasNavigationBar() {
        return this.mHasNavigationBar;
    }

    public boolean hasStatusBar() {
        return this.mHasStatusBar;
    }

    public boolean navigationBarCanMove() {
        return this.mNavigationBarCanMove;
    }

    public void setLidState(int lidState) {
        this.mLidState = lidState;
    }

    public int getLidState() {
        return this.mLidState;
    }

    public void setAwake(boolean awake) {
        this.mAwake = awake;
    }

    public boolean isAwake() {
        return this.mAwake;
    }

    public boolean isScreenOnEarly() {
        return this.mScreenOnEarly;
    }

    public boolean isScreenOnFully() {
        return this.mScreenOnFully;
    }

    public boolean isKeyguardDrawComplete() {
        return this.mKeyguardDrawComplete;
    }

    public boolean isWindowManagerDrawComplete() {
        return this.mWindowManagerDrawComplete;
    }

    public WindowManagerPolicy.ScreenOnListener getScreenOnListener() {
        return this.mScreenOnListener;
    }

    public void screenTurnedOn(WindowManagerPolicy.ScreenOnListener screenOnListener) {
        synchronized (this.mLock) {
            this.mScreenOnEarly = true;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = screenOnListener;
        }
    }

    public void screenTurnedOff() {
        synchronized (this.mLock) {
            this.mScreenOnEarly = false;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = null;
        }
    }

    public boolean finishKeyguardDrawn() {
        synchronized (this.mLock) {
            if (this.mScreenOnEarly) {
                if (!this.mKeyguardDrawComplete) {
                    this.mKeyguardDrawComplete = true;
                    this.mWindowManagerDrawComplete = false;
                    return true;
                }
            }
            return false;
        }
    }

    public boolean finishWindowsDrawn() {
        synchronized (this.mLock) {
            if (this.mScreenOnEarly) {
                if (!this.mWindowManagerDrawComplete) {
                    this.mWindowManagerDrawComplete = true;
                    return true;
                }
            }
            return false;
        }
    }

    public boolean finishScreenTurningOn() {
        synchronized (this.mLock) {
            if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                Slog.d(TAG, "finishScreenTurningOn: mAwake=" + this.mAwake + ", mScreenOnEarly=" + this.mScreenOnEarly + ", mScreenOnFully=" + this.mScreenOnFully + ", mKeyguardDrawComplete=" + this.mKeyguardDrawComplete + ", mWindowManagerDrawComplete=" + this.mWindowManagerDrawComplete);
            }
            if (!this.mScreenOnFully && this.mScreenOnEarly && this.mWindowManagerDrawComplete) {
                if (!this.mAwake || this.mKeyguardDrawComplete) {
                    if (WindowManagerDebugConfig.DEBUG_SCREEN_ON) {
                        Slog.i(TAG, "Finished screen turning on...");
                    }
                    this.mScreenOnListener = null;
                    this.mScreenOnFully = true;
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasStatusBarServicePermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.STATUS_BAR_SERVICE", pid, uid) == 0;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0044, code lost:
        if (r2 != 2006) goto L_0x00b0;
     */
    public void adjustWindowParamsLw(WindowState win, WindowManager.LayoutParams attrs, int callingPid, int callingUid) {
        boolean isScreenDecor = (attrs.privateFlags & 4194304) != 0;
        if (this.mScreenDecorWindows.contains(win)) {
            if (!isScreenDecor) {
                this.mScreenDecorWindows.remove(win);
            }
        } else if (isScreenDecor && hasStatusBarServicePermission(callingPid, callingUid)) {
            this.mScreenDecorWindows.add(win);
        }
        int i = attrs.type;
        if (i != 2000) {
            if (i != 2013) {
                if (i != 2015) {
                    if (i != 2023) {
                        if (i == 2036) {
                            attrs.flags |= 8;
                        } else if (i == 2005) {
                            if (attrs.hideTimeoutMilliseconds < 0 || attrs.hideTimeoutMilliseconds > 3500) {
                                attrs.hideTimeoutMilliseconds = 3500;
                            }
                            attrs.hideTimeoutMilliseconds = (long) this.mAccessibilityManager.getRecommendedTimeoutMillis((int) attrs.hideTimeoutMilliseconds, 2);
                            attrs.windowAnimations = 16973828;
                            if (canToastShowWhenLocked(callingPid)) {
                                attrs.flags |= 524288;
                            }
                            attrs.flags |= 16;
                        }
                    }
                }
                attrs.flags |= 24;
                attrs.flags &= -262145;
            }
            attrs.layoutInDisplayCutoutMode = 1;
        } else if (this.mService.mPolicy.isKeyguardOccluded()) {
            attrs.flags &= -1048577;
            attrs.privateFlags &= -1025;
        }
        if (attrs.type != 2000) {
            attrs.privateFlags &= -1025;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean canToastShowWhenLocked(int callingPid) {
        return this.mDisplayContent.forAllWindows((ToBooleanFunction<WindowState>) new ToBooleanFunction(callingPid) {
            /* class com.android.server.wm.$$Lambda$DisplayPolicy$pqtzqy0ticsynvTP9P1eQUEgE */
            private final /* synthetic */ int f$0;

            {
                this.f$0 = r1;
            }

            public final boolean apply(Object obj) {
                return DisplayPolicy.lambda$canToastShowWhenLocked$2(this.f$0, (WindowState) obj);
            }
        }, true);
    }

    static /* synthetic */ boolean lambda$canToastShowWhenLocked$2(int callingPid, WindowState w) {
        return callingPid == w.mSession.mPid && w.isVisible() && w.canShowWhenLocked();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0038, code lost:
        if (r0 != 2033) goto L_0x00ed;
     */
    public int prepareAddWindowLw(WindowState win, WindowManager.LayoutParams attrs) {
        if (this.mHwDisplayPolicyEx.prepareAddWindowForPC(win, attrs) == 0) {
            return 0;
        }
        if ((attrs.privateFlags & 4194304) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "DisplayPolicy");
            this.mScreenDecorWindows.add(win);
        }
        int i = attrs.type;
        if (i != 2000) {
            if (!(i == 2014 || i == 2017)) {
                if (i == 2019) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "DisplayPolicy");
                    WindowState windowState = this.mNavigationBar;
                    if (windowState != null && windowState.isAlive()) {
                        return -7;
                    }
                    this.mNavigationBar = win;
                    this.mNavigationBarController.setWindow(win);
                    this.mNavigationBarController.setOnBarVisibilityChangedListener(this.mNavBarVisibilityListener, true);
                    this.mDisplayContent.setInsetProvider(1, win, null);
                    this.mDisplayContent.setInsetProvider(5, win, new TriConsumer() {
                        /* class com.android.server.wm.$$Lambda$DisplayPolicy$52bg3qYmo5Unt8Q07j9d6hFQG2o */

                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.this.lambda$prepareAddWindowLw$4$DisplayPolicy((DisplayFrames) obj, (WindowState) obj2, (Rect) obj3);
                        }
                    });
                    this.mDisplayContent.setInsetProvider(6, win, new TriConsumer() {
                        /* class com.android.server.wm.$$Lambda$DisplayPolicy$XeqRJzc7ac4NU1zAF74Hsb20Oyg */

                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.this.lambda$prepareAddWindowLw$5$DisplayPolicy((DisplayFrames) obj, (WindowState) obj2, (Rect) obj3);
                        }
                    });
                    this.mDisplayContent.setInsetProvider(7, win, new TriConsumer() {
                        /* class com.android.server.wm.$$Lambda$DisplayPolicy$2VfPB7jRHi3x9grU1pG8ihi_Ga4 */

                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.this.lambda$prepareAddWindowLw$6$DisplayPolicy((DisplayFrames) obj, (WindowState) obj2, (Rect) obj3);
                        }
                    });
                    this.mDisplayContent.setInsetProvider(9, win, new TriConsumer() {
                        /* class com.android.server.wm.$$Lambda$DisplayPolicy$LmU9vcWscAr5f4KqPLDYJTaZBVU */

                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.this.lambda$prepareAddWindowLw$7$DisplayPolicy((DisplayFrames) obj, (WindowState) obj2, (Rect) obj3);
                        }
                    });
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.i(TAG, "NAVIGATION BAR: " + this.mNavigationBar);
                    }
                } else if (i != 2024) {
                }
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "DisplayPolicy");
        } else {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "DisplayPolicy");
            WindowState windowState2 = this.mStatusBar;
            if (windowState2 != null && windowState2.isAlive()) {
                return -7;
            }
            this.mStatusBar = win;
            this.mStatusBarController.setWindow(win);
            if (this.mDisplayContent.isDefaultDisplay) {
                this.mService.mPolicy.setKeyguardCandidateLw(win);
            }
            TriConsumer<DisplayFrames, WindowState, Rect> frameProvider = new TriConsumer() {
                /* class com.android.server.wm.$$Lambda$DisplayPolicy$sDsfACJdM5Dc_VvZ4b6PthimRJY */

                public final void accept(Object obj, Object obj2, Object obj3) {
                    DisplayPolicy.this.lambda$prepareAddWindowLw$3$DisplayPolicy((DisplayFrames) obj, (WindowState) obj2, (Rect) obj3);
                }
            };
            this.mDisplayContent.setInsetProvider(0, win, frameProvider);
            this.mDisplayContent.setInsetProvider(4, win, frameProvider);
            this.mDisplayContent.setInsetProvider(8, win, frameProvider);
        }
        return 0;
    }

    public /* synthetic */ void lambda$prepareAddWindowLw$3$DisplayPolicy(DisplayFrames displayFrames, WindowState windowState, Rect rect) {
        rect.top = 0;
        rect.bottom = getStatusBarHeight(displayFrames);
    }

    public /* synthetic */ void lambda$prepareAddWindowLw$4$DisplayPolicy(DisplayFrames displayFrames, WindowState windowState, Rect inOutFrame) {
        inOutFrame.top -= this.mBottomGestureAdditionalInset;
    }

    public /* synthetic */ void lambda$prepareAddWindowLw$5$DisplayPolicy(DisplayFrames displayFrames, WindowState windowState, Rect inOutFrame) {
        inOutFrame.left = 0;
        inOutFrame.top = 0;
        inOutFrame.bottom = displayFrames.mDisplayHeight;
        inOutFrame.right = displayFrames.mUnrestricted.left + this.mSideGestureInset;
    }

    public /* synthetic */ void lambda$prepareAddWindowLw$6$DisplayPolicy(DisplayFrames displayFrames, WindowState windowState, Rect inOutFrame) {
        inOutFrame.left = displayFrames.mUnrestricted.right - this.mSideGestureInset;
        inOutFrame.top = 0;
        inOutFrame.bottom = displayFrames.mDisplayHeight;
        inOutFrame.right = displayFrames.mDisplayWidth;
    }

    public /* synthetic */ void lambda$prepareAddWindowLw$7$DisplayPolicy(DisplayFrames displayFrames, WindowState windowState, Rect inOutFrame) {
        if ((windowState.getAttrs().flags & 16) != 0 || this.mNavigationBarLetsThroughTaps) {
            inOutFrame.setEmpty();
        }
    }

    public void removeWindowLw(WindowState win) {
        if (this.mStatusBar == win) {
            this.mStatusBar = null;
            this.mStatusBarController.setWindow(null);
            if (this.mDisplayContent.isDefaultDisplay) {
                this.mService.mPolicy.setKeyguardCandidateLw((WindowManagerPolicy.WindowState) null);
            }
            this.mDisplayContent.setInsetProvider(0, null, null);
        } else if (this.mNavigationBar == win) {
            this.mNavigationBar = null;
            this.mNavigationBarController.setWindow(null);
            this.mDisplayContent.setInsetProvider(1, null, null);
        }
        if (this.mLastFocusedWindow == win) {
            this.mLastFocusedWindow = null;
        }
        this.mScreenDecorWindows.remove(win);
        this.mHwDisplayPolicyEx.removeWindowForPC(win);
    }

    private int getStatusBarHeight(DisplayFrames displayFrames) {
        return Math.max(this.mStatusBarHeightForRotation[displayFrames.mRotation], displayFrames.mDisplayCutoutSafe.top);
    }

    public int selectAnimationLw(WindowState win, int transit) {
        if (win == this.mStatusBar) {
            boolean isKeyguard = (win.getAttrs().privateFlags & 1024) != 0;
            boolean expanded = win.getAttrs().height == -1 && win.getAttrs().width == -1;
            if (isKeyguard || expanded) {
                return -1;
            }
            if (transit == 2 || transit == 4) {
                return 17432759;
            }
            if (transit == 1 || transit == 3) {
                return 17432758;
            }
        } else if (win == this.mNavigationBar) {
            if (win.getAttrs().windowAnimations != 0) {
                return 0;
            }
            int i = this.mNavigationBarPosition;
            if (i == 4) {
                if (transit == 2 || transit == 4) {
                    if (this.mService.mPolicy.isKeyguardShowingAndNotOccluded()) {
                        return 17432753;
                    }
                    return 17432752;
                } else if (transit == 1 || transit == 3) {
                    return 17432751;
                }
            } else if (i == 2) {
                if (transit == 2 || transit == 4) {
                    return 17432757;
                }
                if (transit == 1 || transit == 3) {
                    return 17432756;
                }
            } else if (i == 1) {
                if (transit == 2 || transit == 4) {
                    return 17432755;
                }
                if (transit == 1 || transit == 3) {
                    return 17432754;
                }
            }
        } else if (win.getAttrs().type == 2034) {
            return selectDockedDividerAnimationLw(win, transit);
        }
        if (transit != 5) {
            return (win.getAttrs().type == 2023 && this.mDreamingLockscreen && transit == 1) ? -1 : 0;
        }
        if (win.hasAppShownWindows()) {
            return 17432732;
        }
    }

    private int selectDockedDividerAnimationLw(WindowState win, int transit) {
        int insets = this.mDisplayContent.getDockedDividerController().getContentInsets();
        Rect frame = win.getFrameLw();
        boolean behindNavBar = this.mNavigationBar != null && ((this.mNavigationBarPosition == 4 && frame.top + insets >= this.mNavigationBar.getFrameLw().top) || ((this.mNavigationBarPosition == 2 && frame.left + insets >= this.mNavigationBar.getFrameLw().left) || (this.mNavigationBarPosition == 1 && frame.right - insets <= this.mNavigationBar.getFrameLw().right)));
        boolean landscape = frame.height() > frame.width();
        boolean offscreen = (landscape && (frame.right - insets <= 0 || frame.left + insets >= win.getDisplayFrameLw().right)) || (!landscape && (frame.top - insets <= 0 || frame.bottom + insets >= win.getDisplayFrameLw().bottom));
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
        if (!this.mScreenOnFully || !this.mService.mPolicy.okToAnimate()) {
            anim[0] = 17432853;
            anim[1] = 17432852;
            return;
        }
        WindowState windowState = this.mTopFullscreenOpaqueWindowState;
        if (windowState != null) {
            int animationHint = windowState.getRotationAnimationHint();
            if (animationHint < 0 && this.mTopIsFullscreen) {
                animationHint = this.mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation;
            }
            if (animationHint != 1) {
                if (animationHint == 2) {
                    anim[0] = 17432853;
                    anim[1] = 17432852;
                    return;
                } else if (animationHint != 3) {
                    anim[1] = 0;
                    anim[0] = 0;
                    return;
                }
            }
            anim[0] = 17432854;
            anim[1] = 17432852;
            return;
        }
        anim[1] = 0;
        anim[0] = 0;
    }

    public boolean validateRotationAnimationLw(int exitAnimId, int enterAnimId, boolean forceDefault) {
        switch (exitAnimId) {
            case 17432853:
            case 17432854:
                if (forceDefault) {
                    return false;
                }
                int[] anim = new int[2];
                selectRotationAnimationLw(anim);
                if (exitAnimId == anim[0] && enterAnimId == anim[1]) {
                    return true;
                }
                return false;
            default:
                return true;
        }
    }

    public int adjustSystemUiVisibilityLw(int visibility) {
        this.mStatusBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, visibility);
        this.mNavigationBarController.adjustSystemUiVisibilityLw(this.mLastSystemUiFlags, visibility);
        this.mResettingSystemUiFlags &= visibility;
        return (~this.mResettingSystemUiFlags) & visibility & (~this.mForceClearedSystemUiFlags);
    }

    public boolean areSystemBarsForcedShownLw(WindowState windowState) {
        return this.mForceShowSystemBars;
    }

    public boolean getLayoutHintLw(WindowManager.LayoutParams attrs, Rect taskBounds, DisplayFrames displayFrames, boolean floatingStack, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, DisplayCutout.ParcelableWrapper outDisplayCutout) {
        Rect sf;
        Rect cf;
        int outset;
        int fl = PolicyControl.getWindowFlags(null, attrs);
        int pfl = attrs.privateFlags;
        int sysUiVis = getImpliedSysUiFlagsForLayout(attrs) | PolicyControl.getSystemUiVisibility(null, attrs);
        int displayRotation = displayFrames.mRotation;
        boolean screenDecor = true;
        if ((outOutsets != null && shouldUseOutsets(attrs, fl)) && (outset = this.mWindowOutsetBottom) > 0) {
            if (displayRotation == 0) {
                outOutsets.bottom += outset;
            } else if (displayRotation == 1) {
                outOutsets.right += outset;
            } else if (displayRotation == 2) {
                outOutsets.top += outset;
            } else if (displayRotation == 3) {
                outOutsets.left += outset;
            }
        }
        boolean layoutInScreen = (fl & 256) != 0;
        boolean layoutInScreenAndInsetDecor = layoutInScreen && (65536 & fl) != 0;
        if ((pfl & 4194304) == 0) {
            screenDecor = false;
        }
        if (!layoutInScreenAndInsetDecor || screenDecor) {
            if (layoutInScreen) {
                outFrame.set(displayFrames.mUnrestricted);
            } else {
                outFrame.set(displayFrames.mStable);
            }
            if (taskBounds != null) {
                outFrame.intersect(taskBounds);
            }
            outContentInsets.setEmpty();
            outStableInsets.setEmpty();
            outDisplayCutout.set(DisplayCutout.NO_CUTOUT);
            return this.mForceShowSystemBars;
        }
        if ((sysUiVis & 512) != 0) {
            outFrame.set(displayFrames.mUnrestricted);
        } else {
            outFrame.set(displayFrames.mRestricted);
        }
        if (floatingStack) {
            sf = null;
        } else {
            sf = displayFrames.mStable;
        }
        if (floatingStack) {
            cf = null;
        } else if ((sysUiVis & 256) != 0) {
            if ((fl & 1024) != 0) {
                cf = displayFrames.mStableFullscreen;
            } else {
                cf = displayFrames.mStable;
            }
        } else if ((fl & 1024) == 0 && (33554432 & fl) == 0) {
            cf = displayFrames.mCurrent;
        } else {
            cf = displayFrames.mOverscan;
        }
        if (taskBounds != null) {
            outFrame.intersect(taskBounds);
        }
        InsetUtils.insetsBetweenFrames(outFrame, cf, outContentInsets);
        InsetUtils.insetsBetweenFrames(outFrame, sf, outStableInsets);
        outDisplayCutout.set(displayFrames.mDisplayCutout.calculateRelativeTo(outFrame).getDisplayCutout());
        return this.mForceShowSystemBars;
    }

    private static int getImpliedSysUiFlagsForLayout(WindowManager.LayoutParams attrs) {
        int impliedFlags = 0;
        if ((attrs.flags & Integer.MIN_VALUE) != 0) {
            impliedFlags = 0 | 512;
        }
        boolean forceWindowDrawsBarBackgrounds = (attrs.privateFlags & 131072) != 0 && attrs.height == -1 && attrs.width == -1;
        if ((Integer.MIN_VALUE & attrs.flags) != 0 || forceWindowDrawsBarBackgrounds) {
            return impliedFlags | 1024;
        }
        return impliedFlags;
    }

    private static boolean shouldUseOutsets(WindowManager.LayoutParams attrs, int fl) {
        return attrs.type == 2013 || (33555456 & fl) != 0;
    }

    private final class HideNavInputEventReceiver extends InputEventReceiver {
        HideNavInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent event) {
            try {
                if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0 && ((MotionEvent) event).getAction() == 0) {
                    boolean changed = false;
                    synchronized (DisplayPolicy.this.mLock) {
                        if (DisplayPolicy.this.mInputConsumer != null) {
                            int newVal = DisplayPolicy.this.mResettingSystemUiFlags | 2 | 1 | 4;
                            if (DisplayPolicy.this.mResettingSystemUiFlags != newVal) {
                                int unused = DisplayPolicy.this.mResettingSystemUiFlags = newVal;
                                changed = true;
                            }
                            int newVal2 = DisplayPolicy.this.mForceClearedSystemUiFlags | 2;
                            if (DisplayPolicy.this.mForceClearedSystemUiFlags != newVal2) {
                                int unused2 = DisplayPolicy.this.mForceClearedSystemUiFlags = newVal2;
                                changed = true;
                                DisplayPolicy.this.mHandler.postDelayed(DisplayPolicy.this.mClearHideNavigationFlag, 1000);
                            }
                            if (changed) {
                                DisplayPolicy.this.mDisplayContent.reevaluateStatusBarVisibility();
                            }
                        } else {
                            return;
                        }
                    }
                }
                finishInputEvent(event, false);
            } finally {
                finishInputEvent(event, false);
            }
        }
    }

    public void beginLayoutLw(DisplayFrames displayFrames, int uiMode) {
        boolean isNeedHideNaviBarWin;
        String windowName;
        boolean navVisible;
        WindowState windowState;
        WindowState windowState2;
        displayFrames.onBeginLayout();
        this.mDisplayRotation = displayFrames.mRotation;
        this.mSystemGestures.screenWidth = displayFrames.mUnrestricted.width();
        SystemGesturesPointerEventListener systemGesturesPointerEventListener = this.mSystemGestures;
        int height = displayFrames.mUnrestricted.height();
        systemGesturesPointerEventListener.screenHeight = height;
        this.mRestrictedScreenHeight = height;
        int sysui = this.mLastSystemUiFlags;
        boolean navVisible2 = (sysui & 2) == 0;
        boolean navTranslucent = (-2147450880 & sysui) != 0;
        boolean immersive = (sysui & 2048) != 0;
        boolean immersiveSticky = (sysui & 4096) != 0;
        WindowState windowState3 = this.mFocusedWindow;
        WindowManager.LayoutParams focusAttrs = windowState3 != null ? windowState3.getAttrs() : null;
        if (focusAttrs != null) {
            isNeedHideNaviBarWin = (focusAttrs.privateFlags & Integer.MIN_VALUE) != 0;
        } else {
            isNeedHideNaviBarWin = false;
        }
        boolean navAllowedHidden = immersive || immersiveSticky || isNeedHideNaviBarWin;
        boolean navTranslucent2 = navTranslucent & (!immersiveSticky);
        boolean isKeyguardShowing = isStatusBarKeyguard() && !this.mService.mPolicy.isKeyguardOccluded();
        IHwPhoneWindowManagerEx hwPWMEx = this.mService.getPolicy().getPhoneWindowManagerEx();
        boolean statusBarForcesShowingNavigation = !isKeyguardShowing && (windowState2 = this.mStatusBar) != null && (windowState2.getAttrs().privateFlags & 8388608) != 0 && (hwPWMEx == null || !hwPWMEx.getFPAuthState());
        if (navVisible2 || navAllowedHidden) {
            WindowManagerPolicy.InputConsumer inputConsumer = this.mInputConsumer;
            if (inputConsumer != null) {
                Handler handler = this.mHandler;
                handler.sendMessage(handler.obtainMessage(3, inputConsumer));
            }
        } else if (this.mInputConsumer == null && this.mStatusBar != null && canHideNavigationBar()) {
            this.mInputConsumer = this.mService.createInputConsumer(this.mHandler.getLooper(), "nav_input_consumer", (InputEventReceiver.Factory) new InputEventReceiver.Factory() {
                /* class com.android.server.wm.$$Lambda$DisplayPolicy$FpQuLkFb2EnHvk4Uzhr9G5Rn_xI */

                public final InputEventReceiver createInputEventReceiver(InputChannel inputChannel, Looper looper) {
                    return DisplayPolicy.this.lambda$beginLayoutLw$8$DisplayPolicy(inputChannel, looper);
                }
            }, displayFrames.mDisplayId);
            InputManager.getInstance().setPointerIconType(0);
        }
        boolean navVisible3 = navVisible2 | (!canHideNavigationBar());
        if (navVisible3 && mUsingHwNavibar) {
            navVisible3 = navVisible3 && !this.mHwDisplayPolicyEx.computeNaviBarFlag();
        }
        WindowState windowState4 = this.mFocusedWindow;
        if (windowState4 != null) {
            windowName = windowState4.toString();
        } else {
            windowName = null;
        }
        int type = focusAttrs != null ? focusAttrs.type : 0;
        boolean isKeyguardOn = (type == 2000 && ((focusAttrs != null ? focusAttrs.privateFlags : 0) & 1024) != 0) || type == 2101 || type == 2100;
        if (windowName == null || !windowName.contains("com.google.android.gms/com.google.android.gms.auth.login.ShowErrorActivity")) {
            navVisible = navVisible3;
        } else {
            navVisible = navVisible3;
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
                navVisible = true;
            }
        }
        boolean updateSysUiVisibility = layoutNavigationBar(displayFrames, uiMode, navVisible, navTranslucent2, navAllowedHidden, isKeyguardOn, statusBarForcesShowingNavigation);
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.i(TAG, "mDock rect:" + displayFrames.mDock);
        }
        if (updateSysUiVisibility || layoutStatusBar(displayFrames, sysui, isKeyguardShowing)) {
            updateSystemUiVisibilityLw();
        }
        layoutScreenDecorWindows(displayFrames);
        if (displayFrames.mDisplayCutoutSafe.top > displayFrames.mUnrestricted.top) {
            displayFrames.mDisplayCutoutSafe.top = Math.max(displayFrames.mDisplayCutoutSafe.top, displayFrames.mStable.top);
        }
        if (HwMwUtils.ENABLED && (windowState = this.mFocusedWindow) != null) {
            HwMwUtils.performPolicy((int) WindowManagerService.H.PC_FREEZE_TIMEOUT, new Object[]{windowState});
        }
        this.mHwDisplayPolicyEx.beginLayoutForPC(displayFrames);
        displayFrames.mCurrent.inset(this.mForwardedInsets);
        displayFrames.mContent.inset(this.mForwardedInsets);
    }

    public /* synthetic */ InputEventReceiver lambda$beginLayoutLw$8$DisplayPolicy(InputChannel x$0, Looper x$1) {
        return new HideNavInputEventReceiver(x$0, x$1);
    }

    private void layoutScreenDecorWindows(DisplayFrames displayFrames) {
        DisplayPolicy displayPolicy = this;
        if (!displayPolicy.mScreenDecorWindows.isEmpty()) {
            sTmpRect.setEmpty();
            int displayId = displayFrames.mDisplayId;
            Rect dockFrame = displayFrames.mDock;
            int displayHeight = displayFrames.mDisplayHeight;
            int displayWidth = displayFrames.mDisplayWidth;
            int i = displayPolicy.mScreenDecorWindows.size() - 1;
            while (i >= 0) {
                WindowState w = displayPolicy.mScreenDecorWindows.valueAt(i);
                if (w.getDisplayId() == displayId && w.isVisibleLw()) {
                    w.getWindowFrames().setFrames(displayFrames.mUnrestricted, displayFrames.mUnrestricted, displayFrames.mUnrestricted, displayFrames.mUnrestricted, displayFrames.mUnrestricted, sTmpRect, displayFrames.mUnrestricted, displayFrames.mUnrestricted);
                    w.getWindowFrames().setDisplayCutout(displayFrames.mDisplayCutout);
                    w.computeFrameLw();
                    Rect frame = w.getFrameLw();
                    if (frame.left > 0 || frame.top > 0) {
                        if (frame.right < displayWidth || frame.bottom < displayHeight) {
                            Slog.w(TAG, "layoutScreenDecorWindows: Ignoring decor win=" + w + " not docked on one of the sides of the display. frame=" + frame + " displayWidth=" + displayWidth + " displayHeight=" + displayHeight);
                        } else if (frame.top <= 0) {
                            dockFrame.right = Math.min(frame.left, dockFrame.right);
                        } else if (frame.left <= 0) {
                            dockFrame.bottom = Math.min(frame.top, dockFrame.bottom);
                        } else {
                            Slog.w(TAG, "layoutScreenDecorWindows: Ignoring decor win=" + w + " not docked on right or bottom of display. frame=" + frame + " displayWidth=" + displayWidth + " displayHeight=" + displayHeight);
                        }
                    } else if (frame.bottom >= displayHeight) {
                        dockFrame.left = Math.max(frame.right, dockFrame.left);
                    } else if (frame.right >= displayWidth) {
                        dockFrame.top = Math.max(frame.bottom, dockFrame.top);
                    } else {
                        Slog.w(TAG, "layoutScreenDecorWindows: Ignoring decor win=" + w + " not docked on left or top of display. frame=" + frame + " displayWidth=" + displayWidth + " displayHeight=" + displayHeight);
                    }
                }
                i--;
                displayPolicy = this;
            }
            displayFrames.mRestricted.set(dockFrame);
            displayFrames.mCurrent.set(dockFrame);
            displayFrames.mVoiceContent.set(dockFrame);
            displayFrames.mSystem.set(dockFrame);
            displayFrames.mContent.set(dockFrame);
            displayFrames.mRestrictedOverscan.set(dockFrame);
        }
    }

    private boolean layoutStatusBar(DisplayFrames displayFrames, int sysui, boolean isKeyguardShowing) {
        if (this.mStatusBar == null) {
            return false;
        }
        sTmpRect.setEmpty();
        Rect offsetRect = new Rect(displayFrames.mUnrestricted);
        boolean isLandscape = HwDisplaySizeUtil.hasSideInScreen() && (displayFrames.mRotation == 1 || displayFrames.mRotation == 3);
        if (isLandscape) {
            if (displayFrames.mDisplaySideSafe.top != Integer.MIN_VALUE) {
                offsetRect.top += displayFrames.mDisplaySideSafe.top;
                offsetRect.bottom -= displayFrames.mDisplaySideSafe.top;
            } else {
                int sideWidth = HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
                offsetRect.top += sideWidth;
                offsetRect.bottom -= sideWidth;
                Slog.e(TAG, "layoutStatusBar mDisplaySideSafe is invaild " + displayFrames.mDisplaySideSafe);
            }
        }
        WindowFrames windowFrames = this.mStatusBar.getWindowFrames();
        windowFrames.setFrames(displayFrames.mUnrestricted, offsetRect, displayFrames.mStable, displayFrames.mStable, displayFrames.mStable, sTmpRect, displayFrames.mStable, displayFrames.mStable);
        windowFrames.setDisplayCutout(displayFrames.mDisplayCutout);
        this.mStatusBar.computeFrameLw();
        displayFrames.mStable.top = displayFrames.mUnrestricted.top + this.mStatusBarHeightForRotation[displayFrames.mRotation];
        displayFrames.mStable.top = Math.max(displayFrames.mStable.top, displayFrames.mDisplayCutoutSafe.top);
        if (isLandscape) {
            if (displayFrames.mDisplaySideSafe.top != Integer.MIN_VALUE) {
                displayFrames.mStable.top += displayFrames.mDisplaySideSafe.top;
            } else {
                displayFrames.mStable.top += HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
            }
        }
        sTmpRect.set(this.mStatusBar.getContentFrameLw());
        sTmpRect.intersect(displayFrames.mDisplayCutoutSafe);
        sTmpRect.top = this.mStatusBar.getContentFrameLw().top;
        sTmpRect.bottom = displayFrames.mStable.top;
        this.mStatusBarController.setContentFrame(sTmpRect);
        boolean statusBarTransient = (sysui & 67108864) != 0;
        boolean statusBarTranslucent = (sysui & 1073741832) != 0;
        if (this.mStatusBar.isVisibleLw() && !statusBarTransient) {
            Rect dockFrame = displayFrames.mDock;
            dockFrame.top = displayFrames.mStable.top;
            displayFrames.mContent.set(dockFrame);
            displayFrames.mVoiceContent.set(dockFrame);
            displayFrames.mCurrent.set(dockFrame);
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v(TAG, "Status bar: " + String.format("dock=%s content=%s cur=%s", dockFrame.toString(), displayFrames.mContent.toString(), displayFrames.mCurrent.toString()));
            }
            if (!statusBarTranslucent && !this.mStatusBarController.wasRecentlyTranslucent() && !this.mStatusBar.isAnimatingLw()) {
                displayFrames.mSystem.top = displayFrames.mStable.top;
            }
        }
        return this.mStatusBarController.checkHiddenLw();
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x00dc  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00ef  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00fb  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x010a  */
    private boolean layoutNavigationBar(DisplayFrames displayFrames, int uiMode, boolean navVisible, boolean navTranslucent, boolean navAllowedHidden, boolean isKeyguardOn, boolean statusBarForcesShowingNavigation) {
        IHwPhoneWindowManagerEx hwPWMEx;
        int displayWidth;
        AppWindowToken appWindowToken;
        WindowState win;
        int bottom;
        int top;
        int bottom2;
        int top2;
        int displayWidth2;
        int right;
        int left;
        if (this.mNavigationBar == null) {
            return false;
        }
        Rect navigationFrame = sTmpNavFrame;
        boolean transientNavBarShowing = this.mNavigationBarController.isTransientShowing();
        int rotation = displayFrames.mRotation;
        int displayHeight = displayFrames.mDisplayHeight;
        int displayWidth3 = displayFrames.mDisplayWidth;
        Rect dockFrame = displayFrames.mDock;
        this.mNavigationBarPosition = navigationBarPosition(displayWidth3, displayHeight, rotation);
        Rect cutoutSafeUnrestricted = sTmpRect;
        cutoutSafeUnrestricted.set(displayFrames.mUnrestricted);
        IHwPhoneWindowManagerEx hwPWMEx2 = this.mService.getPolicy().getPhoneWindowManagerEx();
        boolean isNotchSwitchOpen = this.mService.getPolicy().isNotchDisplayDisabled();
        if (hwPWMEx2 == null || hwPWMEx2.isIntersectCutoutForNotch(displayFrames, isNotchSwitchOpen)) {
            cutoutSafeUnrestricted.intersectUnchecked(displayFrames.mDisplayCutoutSafe);
        }
        int i = this.mNavigationBarPosition;
        if (i == 4) {
            int top3 = cutoutSafeUnrestricted.bottom - getNavigationBarHeight(rotation, uiMode);
            int topNavBar = cutoutSafeUnrestricted.bottom - getNavigationBarFrameHeight(rotation, uiMode);
            if (CoordinationModeUtils.isFoldable()) {
                CoordinationModeUtils utils = CoordinationModeUtils.getInstance(this.mContext);
                hwPWMEx = hwPWMEx2;
                displayWidth2 = displayWidth3;
                if (utils.getCoordinationCreateMode() == 3) {
                    left = 0;
                    right = CoordinationModeUtils.getFoldScreenSubWidth();
                } else if ((utils.getCoordinationCreateMode() == 4 || utils.getCoordinationCreateMode() == 2) && utils.getCoordinationState() != 1) {
                    int left2 = CoordinationModeUtils.getFoldScreenEdgeWidth() + CoordinationModeUtils.getFoldScreenSubWidth();
                    left = left2;
                    right = left2 + CoordinationModeUtils.getFoldScreenMainWidth();
                }
                navigationFrame.set(left, topNavBar, right, (displayFrames.mUnrestricted.bottom + this.mHwDisplayPolicyEx.getNaviBarHeightForRotationMax(rotation)) - this.mNavigationBarHeightForRotationDefault[rotation]);
                if (!this.mHwDisplayPolicyEx.isNaviBarMini()) {
                    Rect rect = displayFrames.mStable;
                    Rect rect2 = displayFrames.mStableFullscreen;
                    int naviBarHeightForRotationMin = displayHeight - this.mHwDisplayPolicyEx.getNaviBarHeightForRotationMin(rotation);
                    rect2.bottom = naviBarHeightForRotationMin;
                    rect.bottom = naviBarHeightForRotationMin;
                } else {
                    Rect rect3 = displayFrames.mStable;
                    displayFrames.mStableFullscreen.bottom = top3;
                    rect3.bottom = top3;
                }
                if (!transientNavBarShowing) {
                    this.mLastNaviStatus = 2;
                    this.mLastTransientNaviDockBottom = dockFrame.bottom;
                    this.mNavigationBarController.setBarShowingLw(true);
                } else if (navVisible) {
                    if (this.mNavigationBarController.isTransientHiding()) {
                        Slog.v(TAG, "navigationbar is visible, but transientBarState is hiding, so reset a portrait screen");
                        this.mNavigationBarController.sethwTransientBarState(0);
                    }
                    this.mNavigationBarController.setBarShowingLw(true);
                    Rect rect4 = displayFrames.mRestricted;
                    Rect rect5 = displayFrames.mRestrictedOverscan;
                    int i2 = displayFrames.mStable.bottom;
                    rect5.bottom = i2;
                    rect4.bottom = i2;
                    dockFrame.bottom = i2;
                    this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames.mRestricted.top;
                } else {
                    this.mNavigationBarController.setBarShowingLw(statusBarForcesShowingNavigation);
                    if (isKeyguardOn) {
                        int i3 = this.mLastNaviStatus;
                        if (i3 == 0) {
                            int i4 = this.mLastShowNaviDockBottom;
                            if (i4 != 0) {
                                dockFrame.bottom = i4;
                                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames.mRestrictedOverscan.top;
                                displayFrames.mRestricted.bottom = dockFrame.bottom;
                                displayFrames.mRestrictedOverscan.bottom = dockFrame.bottom;
                            }
                        } else if (i3 == 1) {
                            int i5 = this.mLastHideNaviDockBottom;
                            if (i5 != 0) {
                                dockFrame.bottom = i5;
                                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames.mRestrictedOverscan.top;
                                displayFrames.mRestricted.bottom = dockFrame.bottom;
                                displayFrames.mRestrictedOverscan.bottom = dockFrame.bottom;
                            }
                        } else if (i3 != 2) {
                            Slog.v(TAG, "keyguard mLastNaviStatus is init");
                        } else {
                            int i6 = this.mLastTransientNaviDockBottom;
                            if (i6 != 0) {
                                dockFrame.bottom = i6;
                                this.mRestrictedScreenHeight = dockFrame.bottom - displayFrames.mRestrictedOverscan.top;
                                displayFrames.mRestricted.bottom = dockFrame.bottom;
                                displayFrames.mRestrictedOverscan.bottom = dockFrame.bottom;
                            }
                        }
                    } else {
                        this.mLastNaviStatus = 1;
                        this.mLastHideNaviDockBottom = dockFrame.bottom;
                    }
                }
                if (navVisible && !navTranslucent && !navAllowedHidden && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent() && !mUsingHwNavibar) {
                    displayFrames.mSystem.bottom = displayFrames.mStable.bottom;
                }
                displayWidth = displayWidth2;
            } else {
                hwPWMEx = hwPWMEx2;
                displayWidth2 = displayWidth3;
            }
            left = 0;
            right = displayWidth3;
            navigationFrame.set(left, topNavBar, right, (displayFrames.mUnrestricted.bottom + this.mHwDisplayPolicyEx.getNaviBarHeightForRotationMax(rotation)) - this.mNavigationBarHeightForRotationDefault[rotation]);
            if (!this.mHwDisplayPolicyEx.isNaviBarMini()) {
            }
            if (!transientNavBarShowing) {
            }
            displayFrames.mSystem.bottom = displayFrames.mStable.bottom;
            displayWidth = displayWidth2;
        } else {
            hwPWMEx = hwPWMEx2;
            if (i == 2) {
                boolean isShowLeftNavBar = this.mService.getPolicy().getNavibarAlignLeftWhenLand();
                int left3 = cutoutSafeUnrestricted.right - getNavigationBarWidth(rotation, uiMode);
                if (!isShowLeftNavBar) {
                    if (CoordinationModeUtils.isFoldable()) {
                        CoordinationModeUtils utils2 = CoordinationModeUtils.getInstance(this.mContext);
                        top2 = 0;
                        bottom2 = displayHeight;
                        if (utils2.getCoordinationCreateMode() == 3) {
                            top = CoordinationModeUtils.getFoldScreenMainWidth() + CoordinationModeUtils.getFoldScreenEdgeWidth();
                            bottom = CoordinationModeUtils.getFoldScreenSubWidth() + top;
                        } else if (utils2.getCoordinationCreateMode() == 4 || utils2.getCoordinationCreateMode() == 2) {
                            top = 0;
                            bottom = CoordinationModeUtils.getFoldScreenMainWidth() + 0;
                        }
                        navigationFrame.set(left3, top, (displayFrames.mUnrestricted.right + this.mHwDisplayPolicyEx.getNaviBarWidthForRotationMax(rotation)) - this.mNavigationBarWidthForRotationDefault[rotation], bottom);
                    } else {
                        top2 = 0;
                        bottom2 = displayHeight;
                    }
                    top = top2;
                    bottom = bottom2;
                    navigationFrame.set(left3, top, (displayFrames.mUnrestricted.right + this.mHwDisplayPolicyEx.getNaviBarWidthForRotationMax(rotation)) - this.mNavigationBarWidthForRotationDefault[rotation], bottom);
                } else {
                    navigationFrame.set(0, 0, this.mContext.getResources().getDimensionPixelSize(34472115), displayHeight);
                }
                if (this.mHwDisplayPolicyEx.isNaviBarMini()) {
                    Rect rect6 = displayFrames.mStable;
                    Rect rect7 = displayFrames.mStableFullscreen;
                    int naviBarWidthForRotationMin = displayWidth3 - this.mHwDisplayPolicyEx.getNaviBarWidthForRotationMin(rotation);
                    rect7.right = naviBarWidthForRotationMin;
                    rect6.right = naviBarWidthForRotationMin;
                    displayWidth = displayWidth3;
                } else if (!isShowLeftNavBar) {
                    Rect rect8 = displayFrames.mStable;
                    displayFrames.mStableFullscreen.right = left3;
                    rect8.right = left3;
                    displayWidth = displayWidth3;
                } else {
                    Rect rect9 = displayFrames.mStable;
                    Rect rect10 = displayFrames.mStableFullscreen;
                    int i7 = navigationFrame.right;
                    rect10.left = i7;
                    rect9.left = i7;
                    displayWidth = displayWidth3;
                    displayFrames.mStable.right = displayWidth;
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
                    dockFrame.right = displayFrames.mStable.right;
                    if (!isShowLeftNavBar) {
                        displayFrames.mRestricted.right = (displayFrames.mRestricted.left + dockFrame.right) - displayFrames.mRestricted.left;
                        displayFrames.mRestrictedOverscan.right = dockFrame.right - displayFrames.mRestrictedOverscan.left;
                    } else {
                        Rect rect11 = displayFrames.mRestricted;
                        int i8 = displayFrames.mStable.left;
                        dockFrame.left = i8;
                        rect11.left = i8;
                        displayFrames.mRestricted.right = displayFrames.mRestricted.left + displayFrames.mStable.right;
                        displayFrames.mRestricted.right = displayFrames.mRestricted.left + displayFrames.mStable.right;
                    }
                    this.mLastNaviStatus = 0;
                } else {
                    this.mNavigationBarController.setBarShowingLw(statusBarForcesShowingNavigation);
                    this.mLastNaviStatus = 1;
                }
                if (navVisible && !navTranslucent && !navAllowedHidden && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent() && !mUsingHwNavibar) {
                    displayFrames.mSystem.right = displayFrames.mStable.right;
                }
            } else {
                displayWidth = displayWidth3;
                if (i == 1) {
                    int right2 = cutoutSafeUnrestricted.left + getNavigationBarWidth(rotation, uiMode);
                    navigationFrame.set(displayFrames.mUnrestricted.left, 0, right2, displayHeight);
                    Rect rect12 = displayFrames.mStable;
                    displayFrames.mStableFullscreen.left = right2;
                    rect12.left = right2;
                    if (transientNavBarShowing) {
                        this.mNavigationBarController.setBarShowingLw(true);
                    } else if (navVisible) {
                        this.mNavigationBarController.setBarShowingLw(true);
                        Rect rect13 = displayFrames.mRestricted;
                        displayFrames.mRestrictedOverscan.left = right2;
                        rect13.left = right2;
                        dockFrame.left = right2;
                    } else {
                        this.mNavigationBarController.setBarShowingLw(statusBarForcesShowingNavigation);
                    }
                    if (navVisible && !navTranslucent && !navAllowedHidden && !this.mNavigationBar.isAnimatingLw() && !this.mNavigationBarController.wasRecentlyTranslucent()) {
                        displayFrames.mSystem.left = right2;
                    }
                }
            }
        }
        displayFrames.mCurrent.set(dockFrame);
        displayFrames.mVoiceContent.set(dockFrame);
        displayFrames.mContent.set(dockFrame);
        sTmpRect.setEmpty();
        if (HwDisplaySizeUtil.hasSideInScreen() && this.mNavigationBar.isVisibleLw() && this.mFocusedApp != null && (appWindowToken = this.mService.getRoot().getAppWindowToken(this.mFocusedApp.asBinder())) != null && (win = appWindowToken.findMainWindow()) != null && win.mIsNeedExceptDisplaySide) {
            compressNavigationRect(navigationFrame);
        }
        this.mNavigationBar.getWindowFrames().setFrames(navigationFrame, navigationFrame, navigationFrame, displayFrames.mDisplayCutoutSafe, navigationFrame, sTmpRect, navigationFrame, displayFrames.mDisplayCutoutSafe);
        this.mNavigationBar.getWindowFrames().setDisplayCutout(displayFrames.mDisplayCutout);
        this.mNavigationBar.computeFrameLw();
        this.mNavigationBarController.setContentFrame(this.mNavigationBar.getContentFrameLw());
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.i(TAG, "mNavigationBar frame: " + navigationFrame);
        }
        return this.mNavigationBarController.checkHiddenLw();
    }

    private void compressNavigationRect(Rect navigationFrame) {
        int width = HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
        if (this.mNavigationBarPosition == 4) {
            navigationFrame.set(navigationFrame.left + width, navigationFrame.top, navigationFrame.right - width, navigationFrame.bottom);
        } else {
            navigationFrame.set(navigationFrame.left, navigationFrame.top + width, navigationFrame.right, navigationFrame.bottom - width);
        }
    }

    private void setAttachedWindowFrames(WindowState win, int fl, int adjust, WindowState attached, boolean insetDecors, Rect pf, Rect df, Rect of, Rect cf, Rect vf, DisplayFrames displayFrames) {
        if (win.isInputMethodTarget() || !attached.isInputMethodTarget()) {
            Rect parentDisplayFrame = attached.getDisplayFrameLw();
            Rect parentOverscan = attached.getOverscanFrameLw();
            WindowManager.LayoutParams attachedAttrs = attached.mAttrs;
            if ((attachedAttrs.privateFlags & 131072) != 0 && (attachedAttrs.flags & Integer.MIN_VALUE) == 0 && (attachedAttrs.systemUiVisibility & 512) == 0) {
                parentOverscan = new Rect(parentOverscan);
                parentOverscan.intersect(displayFrames.mRestrictedOverscan);
                parentDisplayFrame = new Rect(parentDisplayFrame);
                parentDisplayFrame.intersect(displayFrames.mRestrictedOverscan);
            }
            if (adjust != 16) {
                cf.set((1073741824 & fl) != 0 ? attached.getContentFrameLw() : parentOverscan);
            } else {
                cf.set(attached.getContentFrameLw());
                if (attached.isVoiceInteraction()) {
                    cf.intersectUnchecked(displayFrames.mVoiceContent);
                } else if (win.isInputMethodTarget() || attached.isInputMethodTarget()) {
                    cf.intersectUnchecked(displayFrames.mContent);
                }
            }
            df.set(insetDecors ? parentDisplayFrame : cf);
            of.set(insetDecors ? parentOverscan : cf);
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
        return !(((win.getAttrs().flags & 8) != 0) ^ ((win.getAttrs().flags & 131072) != 0));
    }

    /* JADX INFO: Multiple debug info for r6v1 android.graphics.Rect: [D('sysUiFl' int), D('vf' android.graphics.Rect)] */
    /* JADX INFO: Multiple debug info for r6v2 android.graphics.Rect: [D('dcf' android.graphics.Rect), D('vf' android.graphics.Rect)] */
    /* JADX INFO: Multiple debug info for r10v1 android.graphics.Rect: [D('sf' android.graphics.Rect), D('type' int)] */
    /* JADX INFO: Multiple debug info for r13v8 'fl'  int: [D('fl' int), D('type' int)] */
    /* JADX WARNING: Code restructure failed: missing block: B:284:0x0738, code lost:
        if (r10 <= 1999) goto L_0x0746;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:288:0x0740, code lost:
        if (r10 == 2009) goto L_0x0746;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:335:0x0828, code lost:
        r4 = r11;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:336:0x082f, code lost:
        if ((r4.flags & Integer.MIN_VALUE) != 0) goto L_0x085f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:338:0x0836, code lost:
        if ((r4.flags & 67108864) != 0) goto L_0x085f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:340:0x083a, code lost:
        if ((r6 & 3076) != 0) goto L_0x085f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:342:0x083f, code lost:
        if (r4.type == 3) goto L_0x085f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:344:0x0845, code lost:
        if (r4.type == 2038) goto L_0x085f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:345:0x0847, code lost:
        if (r6 != false) goto L_0x085f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:346:0x0849, code lost:
        r0 = r14.mUnrestricted.top + r6.mStatusBarHeightForRotation[r14.mRotation];
        r3 = r6;
        r3.top = r0;
        r6.top = r0;
        r62 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:347:0x085f, code lost:
        r3 = r6;
        r62 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:479:0x0be9, code lost:
        if (r6.type != 2013) goto L_0x0bee;
     */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0298  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x02a4  */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x02ae  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x0309  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x04d6  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:310:0x07c6  */
    /* JADX WARNING: Removed duplicated region for block: B:354:0x0871  */
    /* JADX WARNING: Removed duplicated region for block: B:356:0x0887  */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x0894  */
    /* JADX WARNING: Removed duplicated region for block: B:360:0x08b1  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00f9  */
    /* JADX WARNING: Removed duplicated region for block: B:507:0x0cfc  */
    /* JADX WARNING: Removed duplicated region for block: B:508:0x0d02  */
    /* JADX WARNING: Removed duplicated region for block: B:543:0x0d8a  */
    /* JADX WARNING: Removed duplicated region for block: B:544:0x0db1  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e11  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e2a  */
    /* JADX WARNING: Removed duplicated region for block: B:565:0x0e3d  */
    /* JADX WARNING: Removed duplicated region for block: B:566:0x0e3f  */
    /* JADX WARNING: Removed duplicated region for block: B:576:0x0e63  */
    /* JADX WARNING: Removed duplicated region for block: B:606:0x0ee1  */
    /* JADX WARNING: Removed duplicated region for block: B:627:0x0f31  */
    /* JADX WARNING: Removed duplicated region for block: B:637:0x0f4e  */
    /* JADX WARNING: Removed duplicated region for block: B:690:0x1041  */
    /* JADX WARNING: Removed duplicated region for block: B:691:0x104a  */
    /* JADX WARNING: Removed duplicated region for block: B:697:0x1067  */
    /* JADX WARNING: Removed duplicated region for block: B:706:0x10a0  */
    /* JADX WARNING: Removed duplicated region for block: B:707:0x10d5  */
    /* JADX WARNING: Removed duplicated region for block: B:731:0x1153  */
    /* JADX WARNING: Removed duplicated region for block: B:753:0x11e3  */
    /* JADX WARNING: Removed duplicated region for block: B:754:0x129b  */
    /* JADX WARNING: Removed duplicated region for block: B:757:0x12a7  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0138  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x013a  */
    /* JADX WARNING: Removed duplicated region for block: B:784:0x1347  */
    /* JADX WARNING: Removed duplicated region for block: B:785:0x134f  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0144  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0146  */
    public void layoutWindowLw(WindowState win, WindowState attached, DisplayFrames displayFrames) {
        boolean isNeedHideNaviBarWin;
        boolean isPCDisplay;
        boolean hasNavBar;
        boolean isNeedHideNaviBarWin2;
        int fl;
        Rect dcf;
        WindowFrames windowFrames;
        Rect sf;
        int sim;
        Rect of;
        DisplayPolicy displayPolicy;
        DisplayFrames displayFrames2;
        int type;
        Rect cf;
        WindowState windowState;
        Rect df;
        Rect pf;
        WindowManager.LayoutParams attrs;
        Rect vf;
        Rect dcf2;
        int cutoutMode;
        Rect dcf3;
        int fl2;
        WindowFrames windowFrames2;
        int cutoutMode2;
        Rect of2;
        boolean layoutBelowNotch;
        WindowState windowState2;
        WindowState windowState3;
        WindowFrames windowFrames3;
        WindowState windowState4;
        int uiMode;
        IHwDisplayPolicyEx iHwDisplayPolicyEx;
        boolean skipSetUnLimitHeight;
        Rect displayCutoutSafeExceptMaybeBars;
        int i;
        int i2;
        Rect vf2;
        WindowManager.LayoutParams attrs2;
        int fl3;
        int i3;
        WindowState windowState5;
        Rect dcf4;
        WindowFrames windowFrames4;
        Rect sf2;
        DisplayPolicy displayPolicy2;
        int type2;
        Rect vf3;
        Rect cf2;
        Rect df2;
        Rect pf2;
        int fl4;
        int i4;
        int adjust;
        Rect of3;
        WindowManager.LayoutParams attrs3;
        int fl5;
        int sysUiFl;
        char c;
        boolean isCoverByFullWinBtn;
        Rect sf3;
        int fl6;
        int sysUiFl2;
        int sysUiFl3;
        int fl7;
        char c2;
        Rect cf3;
        int adjust2;
        int adjust3;
        Rect of4;
        Rect df3;
        Rect pf3;
        boolean z;
        Rect cf4;
        int adjust4;
        DisplayPolicy displayPolicy3;
        WindowManager.LayoutParams attrs4;
        Rect vf4;
        int i5;
        int i6;
        Rect dcf5;
        WindowState windowState6;
        Rect rect;
        WindowState windowState7;
        WindowManager.LayoutParams attrs5;
        int type3;
        int fl8;
        int adjust5;
        WindowFrames windowFrames5;
        Rect dcf6;
        Rect vf5;
        int sysUiFl4;
        Rect cf5;
        Rect of5;
        Rect df4;
        Rect pf4;
        Rect sf4;
        WindowState windowState8;
        if (!this.mHwDisplayPolicyEx.layoutWindowForPCNavigationBar(win)) {
            if (win == this.mStatusBar && !canReceiveInput(win)) {
                return;
            }
            if (win == this.mNavigationBar) {
                return;
            }
            if (!this.mScreenDecorWindows.contains(win)) {
                WindowManager.LayoutParams attrs6 = win.getAttrs();
                boolean isDefaultDisplay = win.isDefaultDisplay();
                int type4 = attrs6.type;
                int fl9 = PolicyControl.getWindowFlags(win, attrs6);
                int pfl = attrs6.privateFlags;
                int sim2 = attrs6.softInputMode;
                int requestedSysUiFl = PolicyControl.getSystemUiVisibility(null, attrs6);
                int sysUiFl5 = requestedSysUiFl | getImpliedSysUiFlagsForLayout(attrs6);
                WindowFrames windowFrames6 = win.getWindowFrames();
                windowFrames6.setHasOutsets(false);
                sTmpLastParentFrame.set(windowFrames6.mParentFrame);
                Rect pf5 = windowFrames6.mParentFrame;
                Rect df5 = windowFrames6.mDisplayFrame;
                Rect of6 = windowFrames6.mOverscanFrame;
                Rect cf6 = windowFrames6.mContentFrame;
                Rect vf6 = windowFrames6.mVisibleFrame;
                Rect vf7 = windowFrames6.mDecorFrame;
                Rect sf5 = windowFrames6.mStableFrame;
                vf7.setEmpty();
                windowFrames6.setParentFrameWasClippedByDisplayCutout(false);
                windowFrames6.setDisplayCutout(displayFrames.mDisplayCutout);
                if (mUsingHwNavibar) {
                    if ((attrs6.privateFlags & Integer.MIN_VALUE) != 0) {
                        isNeedHideNaviBarWin = true;
                        if (!HwPCUtils.isPcCastModeInServer()) {
                            isPCDisplay = HwPCUtils.isValidExtDisplayId(win.getDisplayId());
                        } else {
                            isPCDisplay = false;
                        }
                        if (HwPCUtils.isPcCastModeInServer() || isDefaultDisplay || !isPCDisplay) {
                            isNeedHideNaviBarWin2 = isNeedHideNaviBarWin;
                            hasNavBar = !hasNavigationBar() && (windowState8 = this.mNavigationBar) != null && windowState8.isVisibleLw();
                        } else {
                            isNeedHideNaviBarWin2 = false;
                            hasNavBar = hasNavigationBar() && getNavigationBarExternal() != null && getNavigationBarExternal().isVisibleLw();
                        }
                        boolean isBaiDuOrSwiftkey = isBaiDuOrSwiftkey(win);
                        boolean isLeftRightSplitStackVisible = isLeftRightSplitStackVisible();
                        boolean isDocked = false;
                        if (attrs6.type == 2) {
                            isDocked = isLeftRightSplitStackVisible && win != this.mAloneTarget;
                        }
                        boolean isLeftRightSplitStack = !isLeftRightSplitStackVisible && (win.inHwSplitScreenWindowingMode() || win.inSplitScreenWindowingMode());
                        int adjust6 = (isLeftRightSplitStackVisible || win == this.mAloneTarget) ? sim2 & 240 : 48;
                        boolean requestedFullscreen = (fl9 & 1024) == 0 || (requestedSysUiFl & 4) != 0;
                        boolean layoutInScreen = (fl9 & 256) != 256;
                        boolean layoutInsetDecor = (65536 & fl9) != 65536;
                        sf5.set(displayFrames.mStable);
                        if (HwPCUtils.isPcCastModeInServer() || !isPCDisplay) {
                            sim = sim2;
                            displayFrames2 = displayFrames;
                            if (type4 != 2011) {
                                WindowState windowState9 = this.mInputMethodTarget;
                                if (!(windowState9 == null || !windowState9.inHwMagicWindowingMode() || (windowState7 = this.mFocusedWindow) == null || windowState7.getAttrs() == null || (this.mFocusedWindow.getAttrs().flags & 1024) == 0)) {
                                    displayFrames2.mStable.top = 0;
                                    sf5.top = 0;
                                    Rect rect2 = displayFrames2.mContent;
                                    Rect rect3 = displayFrames2.mCurrent;
                                    displayFrames2.mDock.top = 0;
                                    rect3.top = 0;
                                    rect2.top = 0;
                                }
                                vf6.set(displayFrames2.mDock);
                                cf6.set(displayFrames2.mDock);
                                of6.set(displayFrames2.mDock);
                                df5.set(displayFrames2.mDock);
                                windowFrames6.mParentFrame.set(displayFrames2.mDock);
                                boolean isPopIME = (this.mAloneTarget == null || (win.getAttrs().hwFlags & 1048576) == 0) ? false : true;
                                if (isLeftRightSplitStackVisible && (windowState6 = this.mAloneTarget) != null && isBaiDuOrSwiftkey && !isPopIME && ((windowState6.inSplitScreenWindowingMode() || this.mAloneTarget.inHwSplitScreenWindowingMode()) && this.mService.mAtmService.mHwATMSEx.isPhoneLandscape(win.getDisplayContent()))) {
                                    if (!this.mFrozen) {
                                        if (this.mAloneTarget.getAttrs().type == 2) {
                                            rect = this.mAloneTarget.getDisplayFrameLw();
                                        } else {
                                            rect = this.mAloneTarget.getContentFrameLw();
                                        }
                                        int i7 = rect.left;
                                        vf6.left = i7;
                                        cf6.left = i7;
                                        of6.left = i7;
                                        df5.left = i7;
                                        pf5.left = i7;
                                        int i8 = rect.right;
                                        vf6.right = i8;
                                        cf6.right = i8;
                                        of6.right = i8;
                                        df5.right = i8;
                                        pf5.right = i8;
                                    } else {
                                        return;
                                    }
                                }
                                int min = Math.min(displayFrames2.mUnrestricted.bottom, displayFrames2.mDisplayCutoutSafe.bottom);
                                of6.bottom = min;
                                df5.bottom = min;
                                pf5.bottom = min;
                                int i9 = displayFrames2.mStable.bottom;
                                vf6.bottom = i9;
                                cf6.bottom = i9;
                                WindowState windowState10 = this.mFocusedWindow;
                                WindowManager.LayoutParams focusAttrs = windowState10 != null ? windowState10.getAttrs() : null;
                                WindowState windowState11 = this.mInputMethodTarget;
                                WindowManager.LayoutParams inputMethodAttrs = (windowState11 == null && (windowState11 = this.mStatusBar) == null) ? null : windowState11.getAttrs();
                                if (!(focusAttrs == null || (focusAttrs.privateFlags & 1024) == 0 || hasNavBar)) {
                                    int i10 = displayFrames2.mSystem.bottom;
                                    of6.bottom = i10;
                                    vf6.bottom = i10;
                                    cf6.bottom = i10;
                                    df5.bottom = i10;
                                    pf5.bottom = i10;
                                }
                                if (!(inputMethodAttrs == null || !SEC_IME_PACKAGE.equals(win.getOwningPackage()) || (inputMethodAttrs.hwFlags & 32) == 0)) {
                                    int calculateSecImeRaisePx = displayFrames2.mSystem.bottom - calculateSecImeRaisePx(this.mContext);
                                    of6.bottom = calculateSecImeRaisePx;
                                    vf6.bottom = calculateSecImeRaisePx;
                                    cf6.bottom = calculateSecImeRaisePx;
                                    df5.bottom = calculateSecImeRaisePx;
                                    pf5.bottom = calculateSecImeRaisePx;
                                }
                                WindowState windowState12 = this.mStatusBar;
                                if (windowState12 != null && this.mFocusedWindow == windowState12 && canReceiveInput(windowState12)) {
                                    int i11 = this.mNavigationBarPosition;
                                    if (i11 == 2) {
                                        int i12 = displayFrames2.mStable.right;
                                        vf6.right = i12;
                                        cf6.right = i12;
                                        of6.right = i12;
                                        df5.right = i12;
                                        pf5.right = i12;
                                    } else if (i11 == 1) {
                                        int i13 = displayFrames2.mStable.left;
                                        vf6.left = i13;
                                        cf6.left = i13;
                                        of6.left = i13;
                                        df5.left = i13;
                                        pf5.left = i13;
                                    }
                                }
                                if (this.mNavigationBarPosition == 4) {
                                    int rotation = displayFrames2.mRotation;
                                    int uimode = this.mService.mPolicy.getUiMode();
                                    int navHeightOffset = getNavigationBarFrameHeight(rotation, uimode) - getNavigationBarHeight(rotation, uimode);
                                    if (navHeightOffset > 0) {
                                        cf6.bottom -= navHeightOffset;
                                        sf5.bottom -= navHeightOffset;
                                        vf6.bottom -= navHeightOffset;
                                        dcf5 = vf7;
                                        dcf5.bottom -= navHeightOffset;
                                    } else {
                                        dcf5 = vf7;
                                    }
                                } else {
                                    dcf5 = vf7;
                                }
                                attrs6.gravity = 80;
                                dcf = dcf5;
                                windowFrames = windowFrames6;
                                of = of6;
                                sf = sf5;
                                fl = fl9;
                                windowState = win;
                                displayPolicy = this;
                                type = type4;
                                vf = cf6;
                                cf = vf6;
                                attrs = attrs6;
                                df = pf5;
                                pf = df5;
                            } else if (type4 == 2031) {
                                of6.set(displayFrames2.mUnrestricted);
                                df5.set(displayFrames2.mUnrestricted);
                                pf5.set(displayFrames2.mUnrestricted);
                                if (adjust6 != 16) {
                                    cf6.set(displayFrames2.mDock);
                                } else {
                                    cf6.set(displayFrames2.mContent);
                                }
                                if (adjust6 != 48) {
                                    vf6.set(displayFrames2.mCurrent);
                                    dcf = vf7;
                                    windowFrames = windowFrames6;
                                    of = of6;
                                    sf = sf5;
                                    fl = fl9;
                                    windowState = win;
                                    displayPolicy = this;
                                    type = type4;
                                    vf = cf6;
                                    cf = vf6;
                                    attrs = attrs6;
                                    df = pf5;
                                    pf = df5;
                                } else {
                                    vf6.set(cf6);
                                    dcf = vf7;
                                    windowFrames = windowFrames6;
                                    of = of6;
                                    sf = sf5;
                                    fl = fl9;
                                    windowState = win;
                                    displayPolicy = this;
                                    type = type4;
                                    vf = cf6;
                                    cf = vf6;
                                    attrs = attrs6;
                                    df = pf5;
                                    pf = df5;
                                }
                            } else {
                                if (type4 == 2013) {
                                    windowState5 = win;
                                    dcf4 = vf7;
                                    windowFrames4 = windowFrames6;
                                    sf2 = sf5;
                                    fl3 = fl9;
                                    attrs2 = attrs6;
                                    displayPolicy2 = this;
                                    i3 = sysUiFl5;
                                    type2 = type4;
                                    vf3 = cf6;
                                    cf2 = vf6;
                                    vf2 = of6;
                                    i2 = adjust6;
                                    df2 = pf5;
                                    pf2 = df5;
                                } else if (type4 == 2103 && !HwPCUtils.isPcCastModeInServer()) {
                                    windowState5 = win;
                                    dcf4 = vf7;
                                    windowFrames4 = windowFrames6;
                                    sf2 = sf5;
                                    fl3 = fl9;
                                    attrs2 = attrs6;
                                    displayPolicy2 = this;
                                    i3 = sysUiFl5;
                                    type2 = type4;
                                    vf3 = cf6;
                                    cf2 = vf6;
                                    vf2 = of6;
                                    i2 = adjust6;
                                    df2 = pf5;
                                    pf2 = df5;
                                } else if (win == this.mStatusBar) {
                                    of6.set(displayFrames2.mUnrestricted);
                                    df5.set(displayFrames2.mUnrestricted);
                                    pf5.set(displayFrames2.mUnrestricted);
                                    cf6.set(displayFrames2.mStable);
                                    vf6.set(displayFrames2.mStable);
                                    if (adjust6 == 16) {
                                        cf6.bottom = displayFrames2.mContent.bottom;
                                        dcf = vf7;
                                        of = of6;
                                        sf = sf5;
                                        windowFrames = windowFrames6;
                                        fl = fl9;
                                        windowState = win;
                                        displayPolicy = this;
                                        type = type4;
                                        vf = cf6;
                                        cf = vf6;
                                        attrs = attrs6;
                                        df = pf5;
                                        pf = df5;
                                    } else {
                                        cf6.bottom = displayFrames2.mDock.bottom;
                                        vf6.bottom = displayFrames2.mContent.bottom;
                                        dcf = vf7;
                                        of = of6;
                                        sf = sf5;
                                        windowFrames = windowFrames6;
                                        fl = fl9;
                                        windowState = win;
                                        displayPolicy = this;
                                        type = type4;
                                        vf = cf6;
                                        cf = vf6;
                                        attrs = attrs6;
                                        df = pf5;
                                        pf = df5;
                                    }
                                } else {
                                    vf7.set(displayFrames2.mSystem);
                                    boolean inheritTranslucentDecor = (attrs6.privateFlags & 512) != 0;
                                    boolean isAppWindow = type4 >= 1 && type4 <= 99;
                                    boolean topAtRest = win == this.mTopFullscreenOpaqueWindowState && !win.isAnimatingLw();
                                    if (!isAppWindow || inheritTranslucentDecor || topAtRest) {
                                        fl4 = fl9;
                                        i4 = Integer.MIN_VALUE;
                                    } else {
                                        if ((sysUiFl5 & 4) == 0) {
                                            fl4 = fl9;
                                            if ((fl4 & 1024) == 0 && (67108864 & fl4) == 0) {
                                                i4 = Integer.MIN_VALUE;
                                                if ((fl4 & Integer.MIN_VALUE) == 0 && (pfl & 131072) == 0 && !isLeftRightSplitStack) {
                                                    vf7.top = displayFrames2.mStable.top;
                                                }
                                            } else {
                                                i4 = Integer.MIN_VALUE;
                                            }
                                        } else {
                                            fl4 = fl9;
                                            i4 = Integer.MIN_VALUE;
                                        }
                                        if ((134217728 & fl4) == 0 && (sysUiFl5 & 2) == 0 && (fl4 & i4) == 0 && (pfl & 131072) == 0) {
                                            vf7.bottom = displayFrames2.mStable.bottom;
                                            vf7.right = displayFrames2.mStable.right;
                                        }
                                    }
                                    if (!layoutInScreen || !layoutInsetDecor) {
                                        dcf = vf7;
                                        sf3 = sf5;
                                        windowFrames = windowFrames6;
                                        if (layoutInScreen) {
                                            fl6 = fl4;
                                            sysUiFl2 = sysUiFl5;
                                            displayPolicy = this;
                                            type = type4;
                                            cf3 = vf6;
                                            attrs3 = attrs6;
                                            adjust2 = adjust6;
                                            vf = cf6;
                                            of3 = of6;
                                            df = pf5;
                                            pf = df5;
                                        } else if ((sysUiFl5 & 1536) != 0) {
                                            fl6 = fl4;
                                            sysUiFl2 = sysUiFl5;
                                            displayPolicy = this;
                                            type = type4;
                                            cf3 = vf6;
                                            attrs3 = attrs6;
                                            adjust2 = adjust6;
                                            vf = cf6;
                                            of3 = of6;
                                            df = pf5;
                                            pf = df5;
                                        } else if (attached != null) {
                                            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                                Slog.v(TAG, "layoutWindowLw(" + ((Object) attrs6.getTitle()) + "): attached to " + attached);
                                            }
                                            displayPolicy = this;
                                            type = type4;
                                            setAttachedWindowFrames(win, fl4, adjust6, attached, false, pf5, df5, of6, cf6, vf6, displayFrames);
                                            cf = vf6;
                                            sysUiFl = sysUiFl5;
                                            vf = cf6;
                                            fl5 = fl4;
                                            attrs3 = attrs6;
                                            adjust = adjust6;
                                            df = pf5;
                                            pf = df5;
                                            of3 = of6;
                                            c = 2013;
                                        } else {
                                            displayPolicy = this;
                                            type = type4;
                                            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                                Slog.v(TAG, "layoutWindowLw(" + ((Object) attrs6.getTitle()) + "): normal window");
                                            }
                                            if (type == 2014) {
                                                vf = cf6;
                                                vf.set(displayFrames2.mRestricted);
                                                of3 = of6;
                                                of3.set(displayFrames2.mRestricted);
                                                pf = df5;
                                                pf.set(displayFrames2.mRestricted);
                                                df = pf5;
                                                df.set(displayFrames2.mRestricted);
                                                cf = vf6;
                                                sysUiFl = sysUiFl5;
                                                fl5 = fl4;
                                                attrs3 = attrs6;
                                                adjust = adjust6;
                                                c = 2013;
                                            } else {
                                                vf = cf6;
                                                df = pf5;
                                                pf = df5;
                                                of3 = of6;
                                                if (type == 2005) {
                                                    cf = vf6;
                                                    adjust3 = adjust6;
                                                } else if (type == 2003) {
                                                    cf = vf6;
                                                    adjust3 = adjust6;
                                                } else {
                                                    df.set(displayFrames2.mContent);
                                                    if (isDocked) {
                                                        df.bottom = displayFrames2.mDock.bottom;
                                                    }
                                                    if (win.isVoiceInteraction()) {
                                                        vf.set(displayFrames2.mVoiceContent);
                                                        of3.set(displayFrames2.mVoiceContent);
                                                        pf.set(displayFrames2.mVoiceContent);
                                                        adjust = adjust6;
                                                    } else {
                                                        adjust = adjust6;
                                                        if (adjust != 16) {
                                                            vf.set(displayFrames2.mDock);
                                                            of3.set(displayFrames2.mDock);
                                                            pf.set(displayFrames2.mDock);
                                                        } else {
                                                            vf.set(displayFrames2.mContent);
                                                            of3.set(displayFrames2.mContent);
                                                            pf.set(displayFrames2.mContent);
                                                        }
                                                    }
                                                    if (adjust != 48) {
                                                        cf = vf6;
                                                        cf.set(displayFrames2.mCurrent);
                                                        sysUiFl = sysUiFl5;
                                                        fl5 = fl4;
                                                        attrs3 = attrs6;
                                                        c = 2013;
                                                    } else {
                                                        cf = vf6;
                                                        cf.set(vf);
                                                        sysUiFl = sysUiFl5;
                                                        fl5 = fl4;
                                                        attrs3 = attrs6;
                                                        c = 2013;
                                                    }
                                                }
                                                vf.set(displayFrames2.mStable);
                                                of3.set(displayFrames2.mStable);
                                                pf.set(displayFrames2.mStable);
                                                attrs3 = attrs6;
                                                if (attrs3.type == 2003) {
                                                    df.set(displayFrames2.mCurrent);
                                                } else {
                                                    df.set(displayFrames2.mStable);
                                                }
                                                if (CoordinationModeUtils.isFoldable()) {
                                                    int createState = CoordinationModeUtils.getInstance(displayPolicy.mContext).getCoordinationCreateMode();
                                                    if (displayFrames2.mRotation == 0 || displayFrames2.mRotation == 2) {
                                                        if (createState == 3) {
                                                            int foldScreenSubWidth = CoordinationModeUtils.getFoldScreenSubWidth();
                                                            cf.right = foldScreenSubWidth;
                                                            df.right = foldScreenSubWidth;
                                                            pf.right = foldScreenSubWidth;
                                                            of3.right = foldScreenSubWidth;
                                                        } else if (createState == 4 || createState == 2) {
                                                            int foldScreenEdgeWidth = CoordinationModeUtils.getFoldScreenEdgeWidth() + CoordinationModeUtils.getFoldScreenSubWidth();
                                                            cf.left = foldScreenEdgeWidth;
                                                            df.left = foldScreenEdgeWidth;
                                                            pf.left = foldScreenEdgeWidth;
                                                            of3.left = foldScreenEdgeWidth;
                                                        }
                                                    } else if (createState == 3) {
                                                        int foldScreenSubWidth2 = CoordinationModeUtils.getFoldScreenSubWidth();
                                                        cf.top = foldScreenSubWidth2;
                                                        df.top = foldScreenSubWidth2;
                                                        pf.top = foldScreenSubWidth2;
                                                        of3.top = foldScreenSubWidth2;
                                                    } else if (createState == 4 || createState == 2) {
                                                        int foldScreenEdgeWidth2 = CoordinationModeUtils.getFoldScreenEdgeWidth() + CoordinationModeUtils.getFoldScreenSubWidth();
                                                        cf.top = foldScreenEdgeWidth2;
                                                        df.top = foldScreenEdgeWidth2;
                                                        pf.top = foldScreenEdgeWidth2;
                                                        of3.top = foldScreenEdgeWidth2;
                                                    }
                                                    sysUiFl = sysUiFl5;
                                                    fl5 = fl4;
                                                    c = 2013;
                                                } else {
                                                    sysUiFl = sysUiFl5;
                                                    fl5 = fl4;
                                                    c = 2013;
                                                }
                                            }
                                        }
                                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                            Slog.v(TAG, "layoutWindowLw(" + ((Object) attrs3.getTitle()) + "): IN_SCREEN");
                                        }
                                        if (type == 2014) {
                                            sysUiFl3 = sysUiFl2;
                                            fl7 = fl6;
                                            c2 = 2013;
                                        } else if (type == 2017) {
                                            sysUiFl3 = sysUiFl2;
                                            fl7 = fl6;
                                            c2 = 2013;
                                        } else {
                                            if (type == 2019) {
                                                sysUiFl = sysUiFl2;
                                                fl5 = fl6;
                                                c = 2013;
                                            } else if (type == 2024) {
                                                sysUiFl = sysUiFl2;
                                                fl5 = fl6;
                                                c = 2013;
                                            } else {
                                                if (type == 2015 || type == 2036) {
                                                    fl5 = fl6;
                                                    if ((fl5 & 1024) != 0) {
                                                        vf.set(displayFrames2.mOverscan);
                                                        of3.set(displayFrames2.mOverscan);
                                                        pf.set(displayFrames2.mOverscan);
                                                        df.set(displayFrames2.mOverscan);
                                                        sysUiFl = sysUiFl2;
                                                        c = 2013;
                                                        displayPolicy.applyStableConstraints(sysUiFl, fl5, vf, displayFrames2);
                                                        if (adjust == 48) {
                                                            cf.set(displayFrames2.mCurrent);
                                                        } else {
                                                            cf.set(vf);
                                                        }
                                                    }
                                                } else {
                                                    fl5 = fl6;
                                                }
                                                if (type == 2021) {
                                                    vf.set(displayFrames2.mOverscan);
                                                    of3.set(displayFrames2.mOverscan);
                                                    pf.set(displayFrames2.mOverscan);
                                                    df.set(displayFrames2.mOverscan);
                                                    sysUiFl = sysUiFl2;
                                                    c = 2013;
                                                } else if ((33554432 & fl5) == 0 || type < 1 || type > 1999) {
                                                    if (canHideNavigationBar()) {
                                                        sysUiFl = sysUiFl2;
                                                        if ((sysUiFl & 512) != 0 && (type == 2000 || type == 2005 || type == 2034 || type == 2033 || (type >= 1 && type <= 1999))) {
                                                            c = 2013;
                                                            vf.set(displayFrames2.mUnrestricted);
                                                            of3.set(displayFrames2.mUnrestricted);
                                                            pf.set(displayFrames2.mUnrestricted);
                                                            df.set(displayFrames2.mUnrestricted);
                                                        }
                                                    } else {
                                                        sysUiFl = sysUiFl2;
                                                    }
                                                    if (mUsingHwNavibar) {
                                                        c = 2013;
                                                    } else {
                                                        c = 2013;
                                                    }
                                                    if (!isNeedHideNaviBarWin2) {
                                                        if ("com.huawei.systemui.mk.lighterdrawer.LighterDrawView".equals(attrs3.getTitle())) {
                                                            vf.set(displayFrames2.mUnrestricted);
                                                            of3.set(displayFrames2.mUnrestricted);
                                                            pf.set(displayFrames2.mUnrestricted);
                                                            df.set(displayFrames2.mUnrestricted);
                                                            cf.set(displayFrames2.mUnrestricted);
                                                        } else if ((sysUiFl & 1024) != 0) {
                                                            of3.set(displayFrames2.mRestricted);
                                                            pf.set(displayFrames2.mRestricted);
                                                            df.set(displayFrames2.mRestricted);
                                                            if (ViewRootImpl.sNewInsetsMode == 0 && adjust == 16) {
                                                                vf.set(displayFrames2.mContent);
                                                            } else {
                                                                vf.set(displayFrames2.mDock);
                                                            }
                                                        } else {
                                                            vf.set(displayFrames2.mRestricted);
                                                            of3.set(displayFrames2.mRestricted);
                                                            pf.set(displayFrames2.mRestricted);
                                                            df.set(displayFrames2.mRestricted);
                                                        }
                                                    }
                                                    vf.set(displayFrames2.mUnrestricted);
                                                    of3.set(displayFrames2.mUnrestricted);
                                                    pf.set(displayFrames2.mUnrestricted);
                                                    df.set(displayFrames2.mUnrestricted);
                                                } else {
                                                    vf.set(displayFrames2.mOverscan);
                                                    of3.set(displayFrames2.mOverscan);
                                                    pf.set(displayFrames2.mOverscan);
                                                    df.set(displayFrames2.mOverscan);
                                                    sysUiFl = sysUiFl2;
                                                    c = 2013;
                                                }
                                                displayPolicy.applyStableConstraints(sysUiFl, fl5, vf, displayFrames2);
                                                if (adjust == 48) {
                                                }
                                            }
                                            of3.set(displayFrames2.mUnrestricted);
                                            pf.set(displayFrames2.mUnrestricted);
                                            df.set(displayFrames2.mUnrestricted);
                                            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                                Slog.v(TAG, "Laying out navigation bar window: " + df);
                                            }
                                            displayPolicy.applyStableConstraints(sysUiFl, fl5, vf, displayFrames2);
                                            if (adjust == 48) {
                                            }
                                        }
                                        vf.set(displayFrames2.mUnrestricted);
                                        of3.set(displayFrames2.mUnrestricted);
                                        pf.set(displayFrames2.mUnrestricted);
                                        df.set(displayFrames2.mUnrestricted);
                                        if (hasNavBar) {
                                            int i14 = displayFrames2.mDock.left;
                                            vf.left = i14;
                                            of3.left = i14;
                                            pf.left = i14;
                                            df.left = i14;
                                            int i15 = displayFrames2.mRestricted.right;
                                            vf.right = i15;
                                            of3.right = i15;
                                            pf.right = i15;
                                            df.right = i15;
                                            int i16 = displayFrames2.mRestricted.bottom;
                                            vf.bottom = i16;
                                            of3.bottom = i16;
                                            pf.bottom = i16;
                                            df.bottom = i16;
                                        }
                                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                            Slog.v(TAG, "Laying out IN_SCREEN status bar window: " + df);
                                        }
                                        displayPolicy.applyStableConstraints(sysUiFl, fl5, vf, displayFrames2);
                                        if (adjust == 48) {
                                        }
                                    } else {
                                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                            Slog.v(TAG, "layoutWindowLw(" + ((Object) attrs6.getTitle()) + "): IN_SCREEN, INSET_DECOR");
                                        }
                                        sf3 = sf5;
                                        if (attached != null) {
                                            windowFrames = windowFrames6;
                                            setAttachedWindowFrames(win, fl4, adjust6, attached, true, pf5, df5, of6, cf6, vf6, displayFrames);
                                            fl5 = fl4;
                                            sysUiFl = sysUiFl5;
                                            cf = vf6;
                                            adjust = adjust6;
                                            dcf = vf7;
                                            type = type4;
                                            attrs3 = attrs6;
                                            df = pf5;
                                            pf = df5;
                                            of3 = of6;
                                            vf = cf6;
                                            c = 2013;
                                            displayPolicy = this;
                                        } else {
                                            windowFrames = windowFrames6;
                                            if (type4 == 2014) {
                                                pf3 = pf5;
                                                df3 = df5;
                                                of4 = of6;
                                                z = true;
                                            } else if (type4 == 2017) {
                                                pf3 = pf5;
                                                df3 = df5;
                                                of4 = of6;
                                                z = true;
                                            } else if ((33554432 & fl4) == 0 || type4 < 1 || type4 > 1999) {
                                                pf3 = pf5;
                                                df3 = df5;
                                                of4 = of6;
                                                if (!canHideNavigationBar() || (sysUiFl5 & 512) == 0) {
                                                    z = true;
                                                } else {
                                                    z = true;
                                                    if (type4 >= 1) {
                                                    }
                                                    if (type4 != 2020) {
                                                    }
                                                    df3.set(displayFrames2.mOverscan);
                                                    pf3.set(displayFrames2.mOverscan);
                                                    of4.set(displayFrames2.mUnrestricted);
                                                    if ((fl4 & 1024) == 0) {
                                                        if (win.isVoiceInteraction()) {
                                                            cf4 = cf6;
                                                            cf4.set(displayFrames2.mVoiceContent);
                                                            adjust4 = adjust6;
                                                        } else {
                                                            cf4 = cf6;
                                                            if (ViewRootImpl.sNewInsetsMode == 0) {
                                                                adjust4 = adjust6;
                                                                if (adjust4 == 16) {
                                                                    cf4.set(displayFrames2.mContent);
                                                                }
                                                            } else {
                                                                adjust4 = adjust6;
                                                            }
                                                            if (!HwFreeFormUtils.isFreeFormEnable() || win.getWindowingMode() != 5) {
                                                                cf4.set(displayFrames2.mDock);
                                                            } else {
                                                                cf4.set(displayFrames2.mContent);
                                                            }
                                                        }
                                                        displayPolicy3 = this;
                                                        synchronized (displayPolicy3.mService.getWindowManagerLock()) {
                                                            try {
                                                                if (!win.hasDrawnLw()) {
                                                                    try {
                                                                        cf4.top = displayFrames2.mUnrestricted.top + displayPolicy3.mStatusBarHeightForRotation[displayFrames2.mRotation];
                                                                    } catch (Throwable th) {
                                                                        th = th;
                                                                    }
                                                                }
                                                            } catch (Throwable th2) {
                                                                th = th2;
                                                                while (true) {
                                                                    try {
                                                                        break;
                                                                    } catch (Throwable th3) {
                                                                        th = th3;
                                                                    }
                                                                }
                                                                throw th;
                                                            }
                                                        }
                                                    } else {
                                                        displayPolicy3 = this;
                                                        vf4 = vf6;
                                                        adjust4 = adjust6;
                                                        dcf = vf7;
                                                        attrs4 = attrs6;
                                                        cf4 = cf6;
                                                        cf4.set(displayFrames2.mRestricted);
                                                    }
                                                    if (isNeedHideNaviBarWin2) {
                                                        cf4.bottom = displayFrames2.mUnrestricted.bottom;
                                                    }
                                                    displayPolicy3.applyStableConstraints(sysUiFl5, fl4, cf4, displayFrames2);
                                                    if (adjust4 != 48) {
                                                        vf4.set(displayFrames2.mCurrent);
                                                        fl5 = fl4;
                                                        type = type4;
                                                        cf = vf4;
                                                        c = 2013;
                                                        attrs3 = attrs4;
                                                        sysUiFl = sysUiFl5;
                                                        displayPolicy = displayPolicy3;
                                                        adjust = adjust4;
                                                        vf = cf4;
                                                        of3 = of4;
                                                        df = pf3;
                                                        pf = df3;
                                                    } else {
                                                        vf4.set(cf4);
                                                        fl5 = fl4;
                                                        type = type4;
                                                        cf = vf4;
                                                        c = 2013;
                                                        attrs3 = attrs4;
                                                        sysUiFl = sysUiFl5;
                                                        displayPolicy = displayPolicy3;
                                                        adjust = adjust4;
                                                        vf = cf4;
                                                        of3 = of4;
                                                        df = pf3;
                                                        pf = df3;
                                                    }
                                                }
                                                if (!isNeedHideNaviBarWin2) {
                                                    df3.set(displayFrames2.mRestrictedOverscan);
                                                    pf3.set(displayFrames2.mRestrictedOverscan);
                                                    of4.set(displayFrames2.mUnrestricted);
                                                    if ((fl4 & 1024) == 0) {
                                                    }
                                                    if (isNeedHideNaviBarWin2) {
                                                    }
                                                    displayPolicy3.applyStableConstraints(sysUiFl5, fl4, cf4, displayFrames2);
                                                    if (adjust4 != 48) {
                                                    }
                                                }
                                                df3.set(displayFrames2.mOverscan);
                                                pf3.set(displayFrames2.mOverscan);
                                                of4.set(displayFrames2.mUnrestricted);
                                                if ((fl4 & 1024) == 0) {
                                                }
                                                if (isNeedHideNaviBarWin2) {
                                                }
                                                displayPolicy3.applyStableConstraints(sysUiFl5, fl4, cf4, displayFrames2);
                                                if (adjust4 != 48) {
                                                }
                                            } else {
                                                of4 = of6;
                                                of4.set(displayFrames2.mOverscan);
                                                df3 = df5;
                                                df3.set(displayFrames2.mOverscan);
                                                pf3 = pf5;
                                                pf3.set(displayFrames2.mOverscan);
                                                z = true;
                                                if ((fl4 & 1024) == 0) {
                                                }
                                                if (isNeedHideNaviBarWin2) {
                                                }
                                                displayPolicy3.applyStableConstraints(sysUiFl5, fl4, cf4, displayFrames2);
                                                if (adjust4 != 48) {
                                                }
                                            }
                                            int i17 = (hasNavBar ? displayFrames2.mDock : displayFrames2.mUnrestricted).left;
                                            of4.left = i17;
                                            df3.left = i17;
                                            pf3.left = i17;
                                            int i18 = displayFrames2.mUnrestricted.top;
                                            of4.top = i18;
                                            df3.top = i18;
                                            pf3.top = i18;
                                            if (hasNavBar) {
                                                i5 = displayFrames2.mRestricted.right;
                                            } else {
                                                i5 = displayFrames2.mUnrestricted.right;
                                            }
                                            of4.right = i5;
                                            df3.right = i5;
                                            pf3.right = i5;
                                            if (hasNavBar) {
                                                i6 = displayFrames2.mRestricted.bottom;
                                            } else {
                                                i6 = displayFrames2.mUnrestricted.bottom;
                                            }
                                            of4.bottom = i6;
                                            df3.bottom = i6;
                                            pf3.bottom = i6;
                                            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                                Slog.v(TAG, "Laying out status bar window: " + pf3);
                                            }
                                            if ((fl4 & 1024) == 0) {
                                            }
                                            if (isNeedHideNaviBarWin2) {
                                            }
                                            displayPolicy3.applyStableConstraints(sysUiFl5, fl4, cf4, displayFrames2);
                                            if (adjust4 != 48) {
                                            }
                                        }
                                    }
                                    if (win.mAppToken == null || win.mAppToken.isSystemUiFullScreenWindowShow) {
                                        WindowState windowState13 = displayPolicy.mHwFullScreenWindow;
                                        if (windowState13 == null || !displayPolicy.isHwFullScreenWinVisibility || displayPolicy.mDisplayRotation != 0) {
                                            fl = fl5;
                                            of = of3;
                                        } else if (!displayPolicy.isAboveFullScreen(win, windowState13)) {
                                            if (win.getOwningPackage() != null) {
                                                if (win.getOwningPackage().contains("com.huawei.android.launcher")) {
                                                    fl = fl5;
                                                    of = of3;
                                                    windowState = win;
                                                    attrs = attrs3;
                                                } else if (win.getOwningPackage().contains("com.huawei.aod")) {
                                                    fl = fl5;
                                                    of = of3;
                                                    windowState = win;
                                                    attrs = attrs3;
                                                }
                                            }
                                            Rect fullWinBtn = displayPolicy.mHwFullScreenWindow.getContentFrameLw();
                                            if (fullWinBtn.top > 0 && fullWinBtn.left == 0) {
                                                if (vf.bottom > fullWinBtn.top) {
                                                    isCoverByFullWinBtn = true;
                                                    if (!isCoverByFullWinBtn) {
                                                        int i19 = (displayFrames2.mUnrestricted.top + displayPolicy.mRestrictedScreenHeight) - (vf.bottom - fullWinBtn.top);
                                                        vf.bottom = i19;
                                                        of3.bottom = i19;
                                                        pf.bottom = i19;
                                                        df.bottom = i19;
                                                        fl = fl5;
                                                        of = of3;
                                                        windowState = win;
                                                        attrs = attrs3;
                                                    } else {
                                                        fl = fl5;
                                                        of = of3;
                                                        windowState = win;
                                                        attrs = attrs3;
                                                    }
                                                }
                                            }
                                            isCoverByFullWinBtn = false;
                                            if (!isCoverByFullWinBtn) {
                                            }
                                        } else {
                                            fl = fl5;
                                            of = of3;
                                            windowState = win;
                                            attrs = attrs3;
                                        }
                                    } else {
                                        fl = fl5;
                                        of = of3;
                                    }
                                    windowState = win;
                                    attrs = attrs3;
                                }
                                windowState = windowState5;
                                fl = fl3;
                                of = vf2;
                                attrs = attrs2;
                                layoutWallpaper(displayFrames, df, pf, vf2, vf);
                            }
                        } else {
                            if (attached != null) {
                                adjust5 = adjust6;
                                windowFrames5 = windowFrames6;
                                sysUiFl4 = sysUiFl5;
                                vf5 = vf6;
                                dcf6 = vf7;
                                sim = sim2;
                                fl8 = fl9;
                                type3 = type4;
                                attrs5 = attrs6;
                                sf = sf5;
                                displayFrames2 = displayFrames;
                                setAttachedWindowFrames(win, fl9, adjust6, attached, true, pf5, df5, of6, cf6, vf5, displayFrames);
                                of5 = of6;
                                cf5 = cf6;
                                df4 = df5;
                                pf4 = pf5;
                            } else {
                                windowFrames5 = windowFrames6;
                                adjust5 = adjust6;
                                sim = sim2;
                                fl8 = fl9;
                                sf = sf5;
                                attrs5 = attrs6;
                                displayFrames2 = displayFrames;
                                sysUiFl4 = sysUiFl5;
                                vf5 = vf6;
                                type3 = type4;
                                dcf6 = vf7;
                                int i20 = displayFrames2.mOverscan.left;
                                cf5 = cf6;
                                cf5.left = i20;
                                of5 = of6;
                                of5.left = i20;
                                df4 = df5;
                                df4.left = i20;
                                pf4 = pf5;
                                pf4.left = i20;
                                int i21 = displayFrames2.mOverscan.top;
                                cf5.top = i21;
                                of5.top = i21;
                                df4.top = i21;
                                pf4.top = i21;
                                int i22 = displayFrames2.mOverscan.right;
                                cf5.right = i22;
                                of5.right = i22;
                                df4.right = i22;
                                pf4.right = i22;
                                int i23 = displayFrames2.mOverscan.bottom;
                                cf5.bottom = i23;
                                of5.bottom = i23;
                                df4.bottom = i23;
                                pf4.bottom = i23;
                            }
                            attrs = attrs5;
                            if (attrs.type == 2011) {
                                int i24 = displayFrames2.mDock.left;
                                vf5.left = i24;
                                cf5.left = i24;
                                of5.left = i24;
                                df4.left = i24;
                                pf4.left = i24;
                                int i25 = displayFrames2.mDock.top;
                                vf5.top = i25;
                                cf5.top = i25;
                                of5.top = i25;
                                df4.top = i25;
                                pf4.top = i25;
                                int inputMethodRightForHwMultiDisplay = this.mHwDisplayPolicyEx.getInputMethodRightForHwMultiDisplay(displayFrames2.mDock.left, displayFrames2.mDock.right);
                                vf5.right = inputMethodRightForHwMultiDisplay;
                                cf5.right = inputMethodRightForHwMultiDisplay;
                                of5.right = inputMethodRightForHwMultiDisplay;
                                df4.right = inputMethodRightForHwMultiDisplay;
                                pf4.right = inputMethodRightForHwMultiDisplay;
                                int height = displayFrames2.mUnrestricted.top + displayFrames2.mUnrestricted.height();
                                of5.bottom = height;
                                df4.bottom = height;
                                pf4.bottom = height;
                                int i26 = displayFrames2.mStable.bottom;
                                vf5.bottom = i26;
                                cf5.bottom = i26;
                                attrs.gravity = 80;
                                of = of5;
                                dcf = dcf6;
                                windowFrames = windowFrames5;
                                fl = fl8;
                                windowState = win;
                                displayPolicy = this;
                                type = type3;
                                cf = vf5;
                                vf = cf5;
                                df = pf4;
                                pf = df4;
                            } else {
                                if ((sysUiFl4 & 2) != 0) {
                                    sf4 = sf;
                                } else if (getNavigationBarExternal() == null || getNavigationBarExternal().isVisibleLw()) {
                                    sf4 = sf;
                                    cf5.set(sf4);
                                    if (attrs.type == 2008) {
                                        cf5.bottom = displayFrames2.mContent.bottom;
                                    }
                                    if (attrs.gravity == 80) {
                                        attrs.gravity = 17;
                                    }
                                    if (adjust5 != 48) {
                                        vf5.set(displayFrames2.mCurrent);
                                    }
                                    layoutWindowForPadPCMode(win, pf4, df4, cf5, vf5, displayFrames2.mContent.bottom);
                                    of = of5;
                                    vf = cf5;
                                    sf = sf4;
                                    dcf = dcf6;
                                    windowFrames = windowFrames5;
                                    fl = fl8;
                                    windowState = win;
                                    cf = vf5;
                                    displayPolicy = this;
                                    type = type3;
                                    attrs = attrs;
                                    df = pf4;
                                    pf = df4;
                                } else {
                                    sf4 = sf;
                                }
                                cf5.set(df4);
                                if (attrs.type == 2008) {
                                }
                                if (attrs.gravity == 80) {
                                }
                                if (adjust5 != 48) {
                                }
                                layoutWindowForPadPCMode(win, pf4, df4, cf5, vf5, displayFrames2.mContent.bottom);
                                of = of5;
                                vf = cf5;
                                sf = sf4;
                                dcf = dcf6;
                                windowFrames = windowFrames5;
                                fl = fl8;
                                windowState = win;
                                cf = vf5;
                                displayPolicy = this;
                                type = type3;
                                attrs = attrs;
                                df = pf4;
                                pf = df4;
                            }
                        }
                        if (mUsingHwNavibar) {
                            dcf2 = dcf;
                        } else if (isNeedHideNaviBarWin2 || displayPolicy.mHwDisplayPolicyEx.getNaviBarFlag()) {
                            int i27 = df.bottom;
                            cf.bottom = i27;
                            vf.bottom = i27;
                            dcf2 = dcf;
                            dcf2.bottom = i27;
                        } else {
                            dcf2 = dcf;
                        }
                        cutoutMode = attrs.layoutInDisplayCutoutMode;
                        boolean attachedInParent = attached == null && !layoutInScreen;
                        boolean requestedHideNavigation = (requestedSysUiFl & 2) == 0;
                        boolean floatingInScreenWindow = attrs.isFullscreen() && layoutInScreen && type != 1;
                        windowState.mIsNeedExceptDisplaySide = displayPolicy.mHwDisplayPolicyEx.isNeedExceptDisplaySide(attrs, windowState, displayPolicy.mDisplayRotation);
                        if (!windowState.mIsNeedExceptDisplaySide) {
                            Rect displaySideSafeExceptMaybeBars = new Rect();
                            displaySideSafeExceptMaybeBars.set(displayFrames2.mDisplaySideSafe);
                            int i28 = displayPolicy.mNavigationBarPosition;
                            if (i28 == 1) {
                                displaySideSafeExceptMaybeBars.left = Integer.MIN_VALUE;
                            } else if (i28 == 2) {
                                displaySideSafeExceptMaybeBars.right = Integer.MAX_VALUE;
                            } else if (i28 == 4) {
                                displaySideSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
                            }
                            if (attachedInParent || floatingInScreenWindow) {
                                windowFrames2 = windowFrames;
                            } else {
                                sTmpRect.set(df);
                                df.intersectUnchecked(displaySideSafeExceptMaybeBars);
                                windowFrames2 = windowFrames;
                                windowFrames2.setParentFrameWasClippedByDisplayCutout(!sTmpRect.equals(df));
                            }
                            pf.intersectUnchecked(displaySideSafeExceptMaybeBars);
                            if (isPCDisplay || !layoutInScreen || !layoutInsetDecor) {
                                dcf3 = dcf2;
                                fl2 = fl;
                            } else {
                                fl2 = fl;
                                if ((fl2 & 1024) != 0) {
                                    dcf3 = dcf2;
                                } else if (!win.inMultiWindowMode()) {
                                    int i29 = displayPolicy.mDisplayRotation;
                                    dcf3 = dcf2;
                                    if (i29 == 1 || i29 == 3) {
                                        displaySideSafeExceptMaybeBars.top += displayPolicy.mStatusBarHeightForRotation[displayFrames2.mRotation];
                                        vf.top = displaySideSafeExceptMaybeBars.top;
                                    }
                                } else {
                                    dcf3 = dcf2;
                                }
                            }
                        } else {
                            dcf3 = dcf2;
                            windowFrames2 = windowFrames;
                            fl2 = fl;
                        }
                        displayPolicy.mService.getPolicy().layoutWindowLwForNotch(windowState, attrs);
                        boolean layoutBelowNotch2 = displayPolicy.mService.getPolicy().isWindowNeedLayoutBelowNotch(windowState);
                        if (layoutBelowNotch2 || (!IS_NOTCH_PROP && cutoutMode != 1)) {
                            displayCutoutSafeExceptMaybeBars = sTmpDisplayCutoutSafeExceptMaybeBarsRect;
                            displayCutoutSafeExceptMaybeBars.set(displayFrames2.mDisplayCutoutSafe);
                            if (layoutInScreen && layoutInsetDecor && !requestedFullscreen && cutoutMode == 0 && !displayPolicy.mService.getPolicy().isWindowNeedLayoutBelowWhenHideNotch()) {
                                displayCutoutSafeExceptMaybeBars.top = Integer.MIN_VALUE;
                            }
                            if (layoutInScreen && layoutInsetDecor && !requestedHideNavigation && cutoutMode == 0) {
                                i = displayPolicy.mNavigationBarPosition;
                                if (i != 1) {
                                    displayCutoutSafeExceptMaybeBars.left = Integer.MIN_VALUE;
                                } else if (i != 2) {
                                    if (i == 4) {
                                        displayCutoutSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
                                    }
                                } else if (!IS_NOTCH_PROP || displayPolicy.mDisplayRotation != 3) {
                                    displayCutoutSafeExceptMaybeBars.right = Integer.MAX_VALUE;
                                }
                            }
                            if (type == 2011 && displayPolicy.mNavigationBarPosition == 4) {
                                displayCutoutSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
                            }
                            if (!attachedInParent && !floatingInScreenWindow) {
                                sTmpRect.set(df);
                                df.intersectUnchecked(displayCutoutSafeExceptMaybeBars);
                                windowFrames2.setParentFrameWasClippedByDisplayCutout(!sTmpRect.equals(df));
                            }
                            pf.intersectUnchecked(displayCutoutSafeExceptMaybeBars);
                        }
                        vf.intersectUnchecked(displayFrames2.mDisplayCutoutSafe);
                        if (HwPCUtils.isPcCastModeInServer() || !isPCDisplay) {
                            of2 = of;
                            if ((fl2 & 512) != 0 || type == 2010) {
                                cutoutMode2 = cutoutMode;
                            } else if (!win.inMultiWindowMode() || win.toString().contains("com.huawei.android.launcher")) {
                                boolean skipSetUnLimitWidth = HwDisplaySizeUtil.hasSideInScreen() && win.toString().contains("hwSingleMode_window");
                                if (!HwDisplaySizeUtil.hasSideInScreen() || type != 2013) {
                                    cutoutMode2 = cutoutMode;
                                } else {
                                    int i30 = displayPolicy.mDisplayRotation;
                                    cutoutMode2 = cutoutMode;
                                    if (i30 == 1 || i30 == 3) {
                                        skipSetUnLimitHeight = true;
                                        if (!skipSetUnLimitWidth) {
                                            pf.top = -10000;
                                            pf.bottom = 10000;
                                        } else if (skipSetUnLimitHeight) {
                                            pf.left = -10000;
                                            pf.right = 10000;
                                        } else {
                                            pf.top = -10000;
                                            pf.left = -10000;
                                            pf.bottom = 10000;
                                            pf.right = 10000;
                                        }
                                        if (type != 2013) {
                                            cf.top = -10000;
                                            cf.left = -10000;
                                            vf.top = -10000;
                                            vf.left = -10000;
                                            of2.top = -10000;
                                            of2.left = -10000;
                                            cf.bottom = 10000;
                                            cf.right = 10000;
                                            vf.bottom = 10000;
                                            vf.right = 10000;
                                            of2.bottom = 10000;
                                            of2.right = 10000;
                                        }
                                    }
                                }
                                skipSetUnLimitHeight = false;
                                if (!skipSetUnLimitWidth) {
                                }
                                if (type != 2013) {
                                }
                            } else {
                                cutoutMode2 = cutoutMode;
                            }
                        } else {
                            if (attrs.type == 2005) {
                                int i31 = displayFrames2.mStable.left;
                                vf.left = i31;
                                of2 = of;
                                of2.left = i31;
                                pf.left = i31;
                                df.left = i31;
                                int i32 = displayFrames2.mStable.top;
                                vf.top = i32;
                                of2.top = i32;
                                pf.top = i32;
                                df.top = i32;
                                int i33 = displayFrames2.mStable.right;
                                vf.right = i33;
                                of2.right = i33;
                                pf.right = i33;
                                df.right = i33;
                                int i34 = displayFrames2.mStable.bottom;
                                vf.bottom = i34;
                                of2.bottom = i34;
                                pf.bottom = i34;
                                df.bottom = i34;
                            } else {
                                of2 = of;
                            }
                            if (!HwPCUtils.enabledInPad() || (fl2 & 512) == 0) {
                                cutoutMode2 = cutoutMode;
                            } else if (win.toString().contains("com.huawei.associateassistant")) {
                                pf.top = -10000;
                                pf.left = -10000;
                                pf.bottom = 10000;
                                pf.right = 10000;
                                cutoutMode2 = cutoutMode;
                            } else {
                                cutoutMode2 = cutoutMode;
                            }
                        }
                        if (win.inHwMagicWindowingMode() && !"MagicWindowGuideDialog".equals(attrs.getTitle())) {
                            layoutBelowNotch = layoutBelowNotch2;
                        } else if (!displayPolicy.mHasNavigationBar) {
                            layoutBelowNotch = layoutBelowNotch2;
                            HwMwUtils.performPolicy((int) WindowManagerService.H.APP_TRANSITION_GETSPECSFUTURE_TIMEOUT, new Object[]{windowState, new Rect[]{pf, df, vf, cf, sf}, displayPolicy.mNavigationBar, Boolean.valueOf(displayPolicy.mHwDisplayPolicyEx.isNaviBarMini())});
                        } else {
                            layoutBelowNotch = layoutBelowNotch2;
                        }
                        boolean useOutsets = shouldUseOutsets(attrs, fl2);
                        if (!isDefaultDisplay && useOutsets) {
                            Rect osf = windowFrames2.mOutsetFrame;
                            osf.set(vf.left, vf.top, vf.right, vf.bottom);
                            windowFrames2.setHasOutsets(true);
                            int outset = displayPolicy.mWindowOutsetBottom;
                            if (outset > 0) {
                                int rotation2 = displayFrames2.mRotation;
                                if (rotation2 == 0) {
                                    osf.bottom += outset;
                                } else if (rotation2 == 1) {
                                    osf.right += outset;
                                } else if (rotation2 == 2) {
                                    osf.top -= outset;
                                } else if (rotation2 == 3) {
                                    osf.left -= outset;
                                }
                                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                    Slog.v(TAG, "applying bottom outset of " + outset + " with rotation " + rotation2 + ", result: " + osf);
                                }
                            }
                        }
                        if (win.toString().contains("DividerMenusView")) {
                            df.top = displayFrames2.mUnrestricted.top;
                            df.bottom = displayFrames2.mUnrestricted.bottom;
                        }
                        if (HwFoldScreenState.isFoldScreenDevice() && type == 2039) {
                            int i35 = displayFrames2.mUnrestricted.bottom;
                            cf.bottom = i35;
                            vf.bottom = i35;
                            of2.bottom = i35;
                            pf.bottom = i35;
                            df.bottom = i35;
                        }
                        if ((!HwPCUtils.isPcCastModeInServer() || !isPCDisplay) && win.toString().contains("com.android.packageinstaller.permission.ui.GrantPermissionsActivity")) {
                            int i36 = displayFrames2.mUnrestricted.top + displayPolicy.mStatusBarHeightForRotation[displayFrames2.mRotation];
                            cf.top = i36;
                            vf.top = i36;
                            of2.top = i36;
                            pf.top = i36;
                            df.top = i36;
                        }
                        if (hasNavBar && win.inHwSplitScreenWindowingMode() && type >= 1 && type <= 99) {
                            pf.bottom = Math.min(pf.bottom, displayFrames2.mRestrictedOverscan.bottom);
                            pf.right = Math.min(pf.right, displayFrames2.mRestrictedOverscan.right);
                            df.bottom = Math.min(df.bottom, displayFrames2.mRestrictedOverscan.bottom);
                            df.right = Math.min(df.right, displayFrames2.mRestrictedOverscan.right);
                        }
                        if (!WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Compute frame ");
                            sb.append((Object) attrs.getTitle());
                            sb.append(": sim=#");
                            sb.append(Integer.toHexString(sim));
                            sb.append(" attach=");
                            windowState3 = attached;
                            sb.append(windowState3);
                            sb.append(" type=");
                            sb.append(type);
                            sb.append(String.format(" flags=0x%08x", Integer.valueOf(fl2)));
                            sb.append(" pf=");
                            sb.append(df.toShortString());
                            sb.append(" df=");
                            sb.append(pf.toShortString());
                            sb.append(" of=");
                            sb.append(of2.toShortString());
                            sb.append(" cf=");
                            sb.append(vf.toShortString());
                            sb.append(" vf=");
                            sb.append(cf.toShortString());
                            sb.append(" dcf=");
                            sb.append(dcf3.toShortString());
                            sb.append(" sf=");
                            sb.append(sf.toShortString());
                            sb.append(" osf=");
                            sb.append(windowFrames2.mOutsetFrame.toShortString());
                            sb.append(" ");
                            windowState2 = win;
                            sb.append(windowState2);
                            Slog.v(TAG, sb.toString());
                        } else {
                            windowState2 = win;
                            windowState3 = attached;
                        }
                        if (!sTmpLastParentFrame.equals(df)) {
                            windowFrames2.setContentChanged(true);
                        }
                        WindowManagerService windowManagerService = displayPolicy.mService;
                        if (WindowManagerService.IS_TABLET || !IS_NOTCH_PROP) {
                            windowFrames3 = windowFrames2;
                            uiMode = 2011;
                            windowState4 = windowState3;
                        } else if (!HwPCUtils.isPcCastModeInServer() || !isPCDisplay) {
                            int uiMode2 = displayPolicy.mService.mPolicy.getUiMode();
                            IHwDisplayPolicyEx iHwDisplayPolicyEx2 = displayPolicy.mHwDisplayPolicyEx;
                            int i37 = displayPolicy.mLastSystemUiFlags;
                            int navigationBarHeight = displayPolicy.getNavigationBarHeight(displayPolicy.mDisplayRotation, uiMode2);
                            windowFrames3 = windowFrames2;
                            uiMode = 2011;
                            windowState4 = windowState3;
                            iHwDisplayPolicyEx2.updateDisplayFrames(win, displayFrames, i37, vf, navigationBarHeight);
                        } else {
                            windowFrames3 = windowFrames2;
                            uiMode = 2011;
                            windowState4 = windowState3;
                        }
                        displayPolicy.mHwDisplayPolicyEx.updateWindowFramesForPC(windowFrames3, df, pf, vf, cf, isPCDisplay);
                        win.computeFrameLw();
                        if (type == uiMode && win.isVisibleLw() && !win.getGivenInsetsPendingLw()) {
                            displayPolicy.offsetInputMethodWindowLw(windowState2, displayFrames2);
                        }
                        if (type == 2031 && win.isVisibleLw() && !win.getGivenInsetsPendingLw()) {
                            displayPolicy.offsetVoiceInputWindowLw(windowState2, displayFrames2);
                        }
                        iHwDisplayPolicyEx = displayPolicy.mHwDisplayPolicyEx;
                        if (iHwDisplayPolicyEx == null) {
                            iHwDisplayPolicyEx.layoutWindowLw(windowState2, windowState4, displayPolicy.mFocusedWindow, layoutBelowNotch);
                            return;
                        }
                        return;
                    }
                }
                isNeedHideNaviBarWin = false;
                if (!HwPCUtils.isPcCastModeInServer()) {
                }
                if (HwPCUtils.isPcCastModeInServer()) {
                }
                isNeedHideNaviBarWin2 = isNeedHideNaviBarWin;
                hasNavBar = !hasNavigationBar() && (windowState8 = this.mNavigationBar) != null && windowState8.isVisibleLw();
                boolean isBaiDuOrSwiftkey2 = isBaiDuOrSwiftkey(win);
                boolean isLeftRightSplitStackVisible2 = isLeftRightSplitStackVisible();
                boolean isDocked2 = false;
                if (attrs6.type == 2) {
                }
                if (!isLeftRightSplitStackVisible2) {
                }
                if (isLeftRightSplitStackVisible2) {
                }
                if ((fl9 & 1024) == 0) {
                }
                if ((fl9 & 256) != 256) {
                }
                if ((65536 & fl9) != 65536) {
                }
                sf5.set(displayFrames.mStable);
                if (HwPCUtils.isPcCastModeInServer()) {
                }
                sim = sim2;
                displayFrames2 = displayFrames;
                if (type4 != 2011) {
                }
                if (mUsingHwNavibar) {
                }
                cutoutMode = attrs.layoutInDisplayCutoutMode;
                if (attached == null) {
                }
                if ((requestedSysUiFl & 2) == 0) {
                }
                if (attrs.isFullscreen()) {
                }
                windowState.mIsNeedExceptDisplaySide = displayPolicy.mHwDisplayPolicyEx.isNeedExceptDisplaySide(attrs, windowState, displayPolicy.mDisplayRotation);
                if (!windowState.mIsNeedExceptDisplaySide) {
                }
                displayPolicy.mService.getPolicy().layoutWindowLwForNotch(windowState, attrs);
                boolean layoutBelowNotch22 = displayPolicy.mService.getPolicy().isWindowNeedLayoutBelowNotch(windowState);
                displayCutoutSafeExceptMaybeBars = sTmpDisplayCutoutSafeExceptMaybeBarsRect;
                displayCutoutSafeExceptMaybeBars.set(displayFrames2.mDisplayCutoutSafe);
                displayCutoutSafeExceptMaybeBars.top = Integer.MIN_VALUE;
                i = displayPolicy.mNavigationBarPosition;
                if (i != 1) {
                }
                displayCutoutSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
                sTmpRect.set(df);
                df.intersectUnchecked(displayCutoutSafeExceptMaybeBars);
                windowFrames2.setParentFrameWasClippedByDisplayCutout(!sTmpRect.equals(df));
                pf.intersectUnchecked(displayCutoutSafeExceptMaybeBars);
                vf.intersectUnchecked(displayFrames2.mDisplayCutoutSafe);
                if (HwPCUtils.isPcCastModeInServer()) {
                }
                of2 = of;
                if ((fl2 & 512) != 0) {
                }
                cutoutMode2 = cutoutMode;
                if (win.inHwMagicWindowingMode()) {
                }
                if (!displayPolicy.mHasNavigationBar) {
                }
                boolean useOutsets2 = shouldUseOutsets(attrs, fl2);
                if (!isDefaultDisplay) {
                }
                if (win.toString().contains("DividerMenusView")) {
                }
                int i352 = displayFrames2.mUnrestricted.bottom;
                cf.bottom = i352;
                vf.bottom = i352;
                of2.bottom = i352;
                pf.bottom = i352;
                df.bottom = i352;
                int i362 = displayFrames2.mUnrestricted.top + displayPolicy.mStatusBarHeightForRotation[displayFrames2.mRotation];
                cf.top = i362;
                vf.top = i362;
                of2.top = i362;
                pf.top = i362;
                df.top = i362;
                pf.bottom = Math.min(pf.bottom, displayFrames2.mRestrictedOverscan.bottom);
                pf.right = Math.min(pf.right, displayFrames2.mRestrictedOverscan.right);
                df.bottom = Math.min(df.bottom, displayFrames2.mRestrictedOverscan.bottom);
                df.right = Math.min(df.right, displayFrames2.mRestrictedOverscan.right);
                if (!WindowManagerDebugConfig.DEBUG_LAYOUT) {
                }
                if (!sTmpLastParentFrame.equals(df)) {
                }
                WindowManagerService windowManagerService2 = displayPolicy.mService;
                if (WindowManagerService.IS_TABLET) {
                }
                windowFrames3 = windowFrames2;
                uiMode = 2011;
                windowState4 = windowState3;
                displayPolicy.mHwDisplayPolicyEx.updateWindowFramesForPC(windowFrames3, df, pf, vf, cf, isPCDisplay);
                win.computeFrameLw();
                displayPolicy.offsetInputMethodWindowLw(windowState2, displayFrames2);
                displayPolicy.offsetVoiceInputWindowLw(windowState2, displayFrames2);
                iHwDisplayPolicyEx = displayPolicy.mHwDisplayPolicyEx;
                if (iHwDisplayPolicyEx == null) {
                }
            }
        }
    }

    private boolean isAboveFullScreen(WindowState win, WindowState fullScreenWin) {
        if (win.isInAboveAppWindows() && fullScreenWin.getLayer() < win.getLayer()) {
            return true;
        }
        return false;
    }

    private int calculateSecImeRaisePx(Context context) {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
        float f = context.getResources().getDisplayMetrics().density;
        return (int) ((((float) SEC_IME_RAISE_HEIGHT) * f * ((((float) lcdDpi) * 1.0f) / ((float) SystemProperties.getInt("persist.sys.dpi", lcdDpi)))) + 0.5f);
    }

    /* access modifiers changed from: package-private */
    public boolean isBaiDuOrSwiftkey(WindowState win) {
        String packageName = win.getAttrs().packageName;
        if (packageName == null || !packageName.contains("com.baidu.input_huawei")) {
            return false;
        }
        return true;
    }

    public void setInputMethodTargetWindow(WindowState target) {
        this.mInputMethodTarget = target;
        if (target == null || target.getAttrs() == null || target.getAttrs().type < 1 || target.getAttrs().type > 99) {
            Log.d(TAG, "please check When in dockside this window can be IME size target " + target);
            return;
        }
        this.mAloneTarget = target;
    }

    public void setFocusChangeIMEFrozenTag(boolean frozen) {
        this.mFrozen = frozen;
    }

    private void layoutWallpaper(DisplayFrames displayFrames, Rect pf, Rect df, Rect of, Rect cf) {
        df.set(displayFrames.mOverscan);
        pf.set(displayFrames.mOverscan);
        cf.set(displayFrames.mUnrestricted);
        of.set(displayFrames.mUnrestricted);
    }

    private void offsetInputMethodWindowLw(WindowState win, DisplayFrames displayFrames) {
        int top = Math.max(win.getDisplayFrameLw().top, win.getContentFrameLw().top) + win.getGivenContentInsetsLw().top;
        displayFrames.mContent.bottom = Math.min(displayFrames.mContent.bottom, top);
        displayFrames.mVoiceContent.bottom = Math.min(displayFrames.mVoiceContent.bottom, top);
        int top2 = win.getVisibleFrameLw().top + win.getGivenVisibleInsetsLw().top;
        displayFrames.mCurrent.bottom = Math.min(displayFrames.mCurrent.bottom, top2);
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.v(TAG, "Input method: mDockBottom=" + displayFrames.mDock.bottom + " mContentBottom=" + displayFrames.mContent.bottom + " mCurBottom=" + displayFrames.mCurrent.bottom);
        }
    }

    private void offsetVoiceInputWindowLw(WindowState win, DisplayFrames displayFrames) {
        int top = Math.max(win.getDisplayFrameLw().top, win.getContentFrameLw().top) + win.getGivenContentInsetsLw().top;
        displayFrames.mVoiceContent.bottom = Math.min(displayFrames.mVoiceContent.bottom, top);
    }

    public void beginPostLayoutPolicyLw() {
        this.mTopFullscreenOpaqueWindowState = null;
        this.mTopFullscreenOpaqueOrDimmingWindowState = null;
        this.mTopDockedOpaqueWindowState = null;
        this.mTopDockedOpaqueOrDimmingWindowState = null;
        this.mForceStatusBar = false;
        this.mForceStatusBarFromKeyguard = false;
        this.mForceStatusBarTransparent = false;
        this.mForcingShowNavBar = false;
        this.mForcingShowNavBarLayer = -1;
        this.mAllowLockscreenWhenOn = false;
        this.mShowingDream = false;
        this.mWindowSleepTokenNeeded = false;
    }

    public void applyPostLayoutPolicyLw(WindowState win, WindowManager.LayoutParams attrs, WindowState attached, WindowState imeTarget) {
        boolean affectsSystemUi = win.canAffectSystemUiFlags();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.i(TAG, "Win " + win + ": affectsSystemUi=" + affectsSystemUi);
        }
        this.mService.mPolicy.applyKeyguardPolicyLw(win, imeTarget);
        int fl = PolicyControl.getWindowFlags(win, attrs);
        boolean isPrimaryWindowingMode = true;
        if (this.mTopFullscreenOpaqueWindowState == null && affectsSystemUi && attrs.type == 2011) {
            this.mForcingShowNavBar = true;
            this.mForcingShowNavBarLayer = win.getSurfaceLayer();
        }
        if (attrs.type == 2000) {
            if ((attrs.privateFlags & 1024) != 0) {
                this.mForceStatusBarFromKeyguard = true;
            }
            if ((attrs.privateFlags & 4096) != 0) {
                this.mForceStatusBarTransparent = true;
            }
        }
        boolean appWindow = attrs.type >= 1 && attrs.type < 2000;
        int windowingMode = win.getWindowingMode();
        boolean inFullScreenOrSplitScreenSecondaryWindowingMode = windowingMode == 1 || windowingMode == 4 || windowingMode == 101;
        if (this.mTopFullscreenOpaqueWindowState == null && affectsSystemUi) {
            if ((fl & 2048) != 0 && !isLeftRightSplitStackVisible()) {
                this.mForceStatusBar = true;
            }
            if (attrs.type == 2023 && (!this.mDreamingLockscreen || (win.isVisibleLw() && win.hasDrawnLw()))) {
                this.mShowingDream = true;
                appWindow = true;
            }
            if (appWindow && attached == null && attrs.isFullscreen() && inFullScreenOrSplitScreenSecondaryWindowingMode) {
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.v(TAG, "Fullscreen window: " + win);
                }
                this.mTopFullscreenOpaqueWindowState = win;
                if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                    this.mTopFullscreenOpaqueOrDimmingWindowState = win;
                }
                if ((fl & 1) != 0) {
                    this.mAllowLockscreenWhenOn = true;
                }
            }
        }
        if (affectsSystemUi && attrs.type == 2031) {
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
        if (!(windowingMode == 3 || windowingMode == 100)) {
            isPrimaryWindowingMode = false;
        }
        if (this.mTopDockedOpaqueWindowState == null && affectsSystemUi && appWindow && attached == null && attrs.isFullscreen() && isPrimaryWindowingMode) {
            this.mTopDockedOpaqueWindowState = win;
            if (this.mTopDockedOpaqueOrDimmingWindowState == null) {
                this.mTopDockedOpaqueOrDimmingWindowState = win;
            }
        }
        if (this.mTopDockedOpaqueOrDimmingWindowState == null && affectsSystemUi && win.isDimming() && isPrimaryWindowingMode) {
            this.mTopDockedOpaqueOrDimmingWindowState = win;
        }
        if (windowingMode != 103 || this.mTopFullscreenOpaqueWindowState != null) {
            return;
        }
        if (!((win.getAttrs().flags & 1024) == 0 && (win.getAttrs().flags & 2048) == 0) && win.canAffectSystemUiFlags()) {
            this.mTopFullscreenOpaqueWindowState = win;
            if (this.mTopFullscreenOpaqueOrDimmingWindowState == null) {
                this.mTopFullscreenOpaqueOrDimmingWindowState = win;
            }
        }
    }

    /* JADX WARN: Type inference failed for: r8v12, types: [int, boolean] */
    public int finishPostLayoutPolicyLw() {
        int i;
        int changes = 0;
        changes = 0;
        changes = 0;
        changes = 0;
        changes = 0;
        changes = 0;
        int changes2 = 0;
        boolean topIsFullscreen = false;
        topIsFullscreen = false;
        boolean z = true;
        if (!this.mShowingDream) {
            this.mDreamingLockscreen = this.mService.mPolicy.isKeyguardShowingAndNotOccluded();
            if (this.mDreamingSleepTokenNeeded) {
                this.mDreamingSleepTokenNeeded = false;
                this.mHandler.obtainMessage(1, 0, 1).sendToTarget();
            }
        } else if (!this.mDreamingSleepTokenNeeded) {
            this.mDreamingSleepTokenNeeded = true;
            this.mHandler.obtainMessage(1, 1, 1).sendToTarget();
        }
        if (this.mStatusBar != null) {
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.i(TAG, "force=" + this.mForceStatusBar + " forcefkg=" + this.mForceStatusBarFromKeyguard + " top=" + this.mTopFullscreenOpaqueWindowState);
            }
            if (!(this.mForceStatusBarTransparent && !this.mForceStatusBar && !this.mForceStatusBarFromKeyguard)) {
                this.mStatusBarController.setShowTransparent(false);
            } else if (!this.mStatusBar.isVisibleLw()) {
                this.mStatusBarController.setShowTransparent(true);
            }
            boolean statusBarForcesShowingNavigation = (this.mStatusBar.getAttrs().privateFlags & 8388608) != 0;
            boolean topAppHidesStatusBar = topAppHidesStatusBar();
            if (this.mForceStatusBar || this.mForceStatusBarFromKeyguard || this.mForceStatusBarTransparent || statusBarForcesShowingNavigation) {
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.v(TAG, "Showing status bar: forced");
                }
                if (this.mStatusBarController.setBarShowingLw(true)) {
                    changes2 = 0 | 1;
                }
                if (!this.mTopIsFullscreen || !this.mStatusBar.isAnimatingLw()) {
                    z = false;
                }
                topIsFullscreen = z;
                if ((this.mForceStatusBarFromKeyguard || statusBarForcesShowingNavigation) && this.mStatusBarController.isTransientShowing()) {
                    StatusBarController statusBarController = this.mStatusBarController;
                    int i2 = this.mLastSystemUiFlags;
                    statusBarController.updateVisibilityLw(false, i2, i2);
                }
            } else if (this.mTopFullscreenOpaqueWindowState != null) {
                topIsFullscreen = topAppHidesStatusBar;
                if (IS_NOTCH_PROP && this.mDisplayRotation == 0) {
                    ?? r8 = this.mForceNotchStatusBar;
                    this.notchWindowChangeState = this.mNotchStatusBarColorLw != r8;
                    if (this.notchWindowChangeState) {
                        this.mNotchStatusBarColorLw = r8;
                        this.mService.getPolicy().notchStatusBarColorUpdate((int) r8);
                        this.notchWindowChange = true;
                    } else if (this.mFocusedWindow != null && !this.mTopFullscreenOpaqueWindowState.toString().equals(this.mFocusedWindow.toString()) && !this.notchWindowChangeState && this.mForceNotchStatusBar && this.notchWindowChange) {
                        this.mService.getPolicy().notchStatusBarColorUpdate(r8 == true ? 1 : 0);
                        this.notchWindowChange = false;
                    }
                }
                boolean isHideStatusbarInFreeform = this.mService.getPolicy().isNotchDisplayDisabled() && ((i = this.mDisplayRotation) == 0 || i == 2);
                if (this.mStatusBarController.isTransientShowing()) {
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 0 | 1;
                    }
                } else if ((!topIsFullscreen || (((!HwFreeFormUtils.isFreeFormEnable() || isHideStatusbarInFreeform) && this.mDisplayContent.isStackVisible(5)) || this.mDisplayContent.isStackVisible(3) || this.mService.mAtmService.mHwATMSEx.isSplitStackVisible(this.mDisplayContent.mAcitvityDisplay, 0))) && !isLeftRightSplitStackVisible()) {
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.v(TAG, "** SHOWING status bar: top is not fullscreen");
                    }
                    if (this.mStatusBarController.setBarShowingLw(true)) {
                        changes = 0 | 1;
                    }
                    topAppHidesStatusBar = false;
                } else {
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.v(TAG, "** HIDING status bar");
                    }
                    if (this.mStatusBarController.setBarShowingLw(false)) {
                        changes = 0 | 1;
                    } else if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.v(TAG, "Status bar already hiding");
                    }
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
        boolean z2 = this.mShowingDream;
        if (z2 != this.mLastShowingDream) {
            this.mLastShowingDream = z2;
            this.mService.notifyShowingDreamChanged();
        }
        updateWindowSleepToken();
        this.mService.mPolicy.setAllowLockscreenWhenOn(getDisplayId(), this.mAllowLockscreenWhenOn);
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
        boolean isFreeformHideStatusBar;
        if (this.mTopFullscreenOpaqueWindowState == null) {
            return false;
        }
        if (isLeftRightSplitStackVisible()) {
            return true;
        }
        int fl = PolicyControl.getWindowFlags(null, this.mTopFullscreenOpaqueWindowState.getAttrs());
        if (IS_NOTCH_PROP) {
            if (this.mService.getPolicy().isNotchDisplayDisabled()) {
                isFreeformHideStatusBar = true;
            } else {
                WindowState windowState = this.mFocusedWindow;
                isFreeformHideStatusBar = windowState == null || !windowState.inFreeformWindowingMode();
            }
            if (!this.mService.getPolicy().hideNotchStatusBar(fl) && isFreeformHideStatusBar) {
                return false;
            }
            this.mForceNotchStatusBar = false;
        }
        if ((fl != false && true) || (this.mLastSystemUiFlags & 4) != 0) {
            return true;
        }
        return false;
    }

    public void switchUser() {
        updateCurrentUserResources();
    }

    public void onOverlayChangedLw() {
        updateCurrentUserResources();
        onConfigurationChanged();
        this.mSystemGestures.onConfigurationChanged();
    }

    public void onConfigurationChanged() {
        DisplayRotation displayRotation = this.mDisplayContent.getDisplayRotation();
        Resources res = getCurrentUserResources();
        int portraitRotation = displayRotation.getPortraitRotation();
        int upsideDownRotation = displayRotation.getUpsideDownRotation();
        int landscapeRotation = displayRotation.getLandscapeRotation();
        int seascapeRotation = displayRotation.getSeascapeRotation();
        int uiMode = this.mService.mPolicy.getUiMode();
        if (hasStatusBar()) {
            int[] iArr = this.mStatusBarHeightForRotation;
            int dimensionPixelSize = res.getDimensionPixelSize(17105445);
            iArr[upsideDownRotation] = dimensionPixelSize;
            iArr[portraitRotation] = dimensionPixelSize;
            int[] iArr2 = this.mStatusBarHeightForRotation;
            int dimensionPixelSize2 = res.getDimensionPixelSize(17105444);
            iArr2[seascapeRotation] = dimensionPixelSize2;
            iArr2[landscapeRotation] = dimensionPixelSize2;
        } else {
            int[] iArr3 = this.mStatusBarHeightForRotation;
            iArr3[seascapeRotation] = 0;
            iArr3[landscapeRotation] = 0;
            iArr3[upsideDownRotation] = 0;
            iArr3[portraitRotation] = 0;
        }
        int[] iArr4 = this.mNavigationBarHeightForRotationDefault;
        int dimensionPixelSize3 = res.getDimensionPixelSize(17105305);
        iArr4[upsideDownRotation] = dimensionPixelSize3;
        iArr4[portraitRotation] = dimensionPixelSize3;
        int[] iArr5 = this.mNavigationBarHeightForRotationDefault;
        int dimensionPixelSize4 = res.getDimensionPixelSize(17105307);
        iArr5[seascapeRotation] = dimensionPixelSize4;
        iArr5[landscapeRotation] = dimensionPixelSize4;
        int[] iArr6 = this.mNavigationBarFrameHeightForRotationDefault;
        int dimensionPixelSize5 = res.getDimensionPixelSize(17105302);
        iArr6[upsideDownRotation] = dimensionPixelSize5;
        iArr6[portraitRotation] = dimensionPixelSize5;
        int[] iArr7 = this.mNavigationBarFrameHeightForRotationDefault;
        int dimensionPixelSize6 = res.getDimensionPixelSize(17105303);
        iArr7[seascapeRotation] = dimensionPixelSize6;
        iArr7[landscapeRotation] = dimensionPixelSize6;
        int[] iArr8 = this.mNavigationBarWidthForRotationDefault;
        int dimensionPixelSize7 = res.getDimensionPixelSize(17105310);
        iArr8[seascapeRotation] = dimensionPixelSize7;
        iArr8[landscapeRotation] = dimensionPixelSize7;
        iArr8[upsideDownRotation] = dimensionPixelSize7;
        iArr8[portraitRotation] = dimensionPixelSize7;
        this.mNavBarOpacityMode = res.getInteger(17694850);
        this.mSideGestureInset = res.getDimensionPixelSize(17105049);
        this.mNavigationBarLetsThroughTaps = res.getBoolean(17891484);
        this.mNavigationBarAlwaysShowOnSideGesture = res.getBoolean(17891481);
        this.mBottomGestureAdditionalInset = res.getDimensionPixelSize(17105304) - getNavigationBarFrameHeight(portraitRotation, uiMode);
        updateConfigurationAndScreenSizeDependentBehaviors();
        this.mWindowOutsetBottom = ScreenShapeHelper.getWindowOutsetBottomPx(this.mContext.getResources());
        IHwDisplayPolicyEx iHwDisplayPolicyEx = this.mHwDisplayPolicyEx;
        if (iHwDisplayPolicyEx != null) {
            iHwDisplayPolicyEx.initialNavigationSize(this.mDisplayContent.getDisplay(), this.mDisplayContent.mBaseDisplayWidth, this.mDisplayContent.mBaseDisplayHeight, this.mDisplayContent.mBaseDisplayDensity);
            this.mHwDisplayPolicyEx.onConfigurationChanged();
        }
    }

    /* access modifiers changed from: package-private */
    public void updateConfigurationAndScreenSizeDependentBehaviors() {
        Resources res = getCurrentUserResources();
        this.mNavigationBarCanMove = this.mDisplayContent.mBaseDisplayWidth != this.mDisplayContent.mBaseDisplayHeight && res.getBoolean(17891482);
        this.mAllowSeamlessRotationDespiteNavBarMoving = res.getBoolean(17891346);
    }

    private void updateCurrentUserResources() {
        int userId = this.mService.mAmInternal.getCurrentUserId();
        Context uiContext = getSystemUiContext();
        if (userId == 0) {
            this.mCurrentUserResources = uiContext.getResources();
            return;
        }
        LoadedApk pi = ActivityThread.currentActivityThread().getPackageInfo(uiContext.getPackageName(), (CompatibilityInfo) null, 0, userId);
        this.mCurrentUserResources = ResourcesManager.getInstance().getResources((IBinder) null, pi.getResDir(), (String[]) null, pi.getOverlayDirs(), pi.getApplicationInfo().sharedLibraryFiles, this.mDisplayContent.getDisplayId(), (Configuration) null, uiContext.getResources().getCompatibilityInfo(), (ClassLoader) null);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public Resources getCurrentUserResources() {
        if (this.mCurrentUserResources == null) {
            updateCurrentUserResources();
        }
        return this.mCurrentUserResources;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public Context getContext() {
        return this.mContext;
    }

    private Context getSystemUiContext() {
        Context uiContext = ActivityThread.currentActivityThread().getSystemUiContext();
        if (this.mDisplayContent.isDefaultDisplay) {
            return uiContext;
        }
        return uiContext.createDisplayContext(this.mDisplayContent.getDisplay());
    }

    private int getNavigationBarWidth(int rotation, int uiMode) {
        return this.mNavigationBarWidthForRotationDefault[rotation];
    }

    /* access modifiers changed from: package-private */
    public void notifyDisplayReady() {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.wm.$$Lambda$DisplayPolicy$mUPXUZKrPpeFUjrauzoJMNbYjM */

            public final void run() {
                DisplayPolicy.this.lambda$notifyDisplayReady$9$DisplayPolicy();
            }
        });
    }

    public /* synthetic */ void lambda$notifyDisplayReady$9$DisplayPolicy() {
        int displayId = getDisplayId();
        getStatusBarManagerInternal().onDisplayReady(displayId);
        ((WallpaperManagerInternal) LocalServices.getService(WallpaperManagerInternal.class)).onDisplayReady(displayId);
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        int navBarPosition;
        int widthForCar = this.mHwDisplayPolicyEx.getNonDecorDisplayWidthForExtraDisplay(fullWidth, getDisplayId());
        if (widthForCar != -1) {
            return widthForCar;
        }
        int width = fullWidth;
        if (hasNavigationBar() && ((navBarPosition = navigationBarPosition(fullWidth, fullHeight, rotation)) == 1 || navBarPosition == 2)) {
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

    private int getNavigationBarFrameHeight(int rotation, int uiMode) {
        return this.mNavigationBarFrameHeightForRotationDefault[rotation];
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        int height = fullHeight;
        if ((uiMode & 15) != 3 && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getDisplayId())) {
            return this.mHwDisplayPolicyEx.getNonDecorDisplayHeight(fullHeight, getDisplayId());
        }
        if (hasNavigationBar() && navigationBarPosition(fullWidth, fullHeight, rotation) == 4) {
            height -= getNavigationBarHeight(rotation, uiMode);
        }
        if (displayCutout != null) {
            return height - (displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom());
        }
        return height;
    }

    public int getConfigDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        return getNonDecorDisplayWidth(fullWidth, fullHeight, rotation, uiMode, displayCutout);
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        int statusBarHeight = this.mStatusBarHeightForRotation[rotation];
        if (displayCutout != null) {
            statusBarHeight = Math.max(0, statusBarHeight - displayCutout.getSafeInsetTop());
        }
        return getNonDecorDisplayHeight(fullWidth, fullHeight, rotation, uiMode, displayCutout) - statusBarHeight;
    }

    /* access modifiers changed from: package-private */
    public float getWindowCornerRadius() {
        if (this.mDisplayContent.getDisplay().getType() == 1) {
            return ScreenDecorationsUtils.getWindowCornerRadius(this.mContext.getResources());
        }
        return 0.0f;
    }

    /* access modifiers changed from: package-private */
    public boolean isShowingDreamLw() {
        return this.mShowingDream;
    }

    /* access modifiers changed from: package-private */
    public void convertNonDecorInsetsToStableInsets(Rect inOutInsets, int rotation) {
        inOutInsets.top = Math.max(inOutInsets.top, this.mStatusBarHeightForRotation[rotation]);
    }

    public void getStableInsetsLw(int displayRotation, int displayWidth, int displayHeight, DisplayCutout displayCutout, Rect outInsets) {
        if (!this.mHwDisplayPolicyEx.getStableInsetsForPC(outInsets, getDisplayId())) {
            outInsets.setEmpty();
            getNonDecorInsetsLw(displayRotation, displayWidth, displayHeight, displayCutout, outInsets);
            convertNonDecorInsetsToStableInsets(outInsets, displayRotation);
        }
    }

    public void getNonDecorInsetsLw(int displayRotation, int displayWidth, int displayHeight, DisplayCutout displayCutout, Rect outInsets) {
        if (!this.mHwDisplayPolicyEx.getNonDecorInsetsForPC(outInsets, getDisplayId())) {
            outInsets.setEmpty();
            if (hasNavigationBar()) {
                int uiMode = this.mService.mPolicy.getUiMode();
                int position = navigationBarPosition(displayWidth, displayHeight, displayRotation);
                if (position == 4) {
                    outInsets.bottom = getNavigationBarHeight(displayRotation, uiMode);
                } else if (position == 2) {
                    outInsets.right = getNavigationBarWidth(displayRotation, uiMode);
                } else if (position == 1) {
                    outInsets.left = getNavigationBarWidth(displayRotation, uiMode);
                }
            }
            if (displayCutout != null) {
                outInsets.left += displayCutout.getSafeInsetLeft();
                outInsets.top += displayCutout.getSafeInsetTop();
                outInsets.right += displayCutout.getSafeInsetRight();
                outInsets.bottom += displayCutout.getSafeInsetBottom();
            }
        }
    }

    public void setForwardedInsets(Insets forwardedInsets) {
        this.mForwardedInsets = forwardedInsets;
    }

    public Insets getForwardedInsets() {
        return this.mForwardedInsets;
    }

    /* access modifiers changed from: package-private */
    public int navigationBarPosition(int displayWidth, int displayHeight, int displayRotation) {
        if (!navigationBarCanMove() || displayWidth <= displayHeight) {
            return 4;
        }
        if (displayRotation == 3 || displayRotation == 1) {
            return 2;
        }
        return 4;
    }

    public int getNavBarPosition() {
        return this.mNavigationBarPosition;
    }

    public int focusChangedLw(WindowState lastFocus, WindowState newFocus) {
        if (this.mHwDisplayPolicyEx.focusChangedLwForPC(newFocus)) {
            return 1;
        }
        IHwDisplayPolicyEx iHwDisplayPolicyEx = this.mHwDisplayPolicyEx;
        if (iHwDisplayPolicyEx != null) {
            iHwDisplayPolicyEx.focusChangedLw(lastFocus, newFocus);
        }
        this.mFocusedWindow = newFocus;
        this.mLastFocusedWindow = lastFocus;
        if (this.mDisplayContent.isDefaultDisplay) {
            this.mService.mPolicy.onDefaultDisplayFocusChangedLw(newFocus);
        }
        WindowState windowState = this.mFocusedWindow;
        if (windowState != null) {
            if (IS_NOTCH_PROP && windowState.toString().contains("com.huawei.android.launcher")) {
                this.mForceNotchStatusBar = false;
            }
            updateSystemUiColorLw(this.mFocusedWindow);
            WindowState windowState2 = this.mLastStartingWindow;
            if (windowState2 != null && windowState2.isVisibleLw() && this.mFocusedWindow.getAttrs() != null && this.mFocusedWindow.getAttrs().type == 2003) {
                updateSystemUiColorLw(this.mLastStartingWindow);
            }
        }
        if ((updateSystemUiVisibilityLw() & SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            return 1;
        }
        return 0;
    }

    public boolean allowAppAnimationsLw() {
        return !this.mShowingDream;
    }

    /* access modifiers changed from: private */
    public void updateDreamingSleepToken(boolean acquire) {
        if (acquire) {
            int displayId = getDisplayId();
            if (this.mDreamingSleepToken == null) {
                ActivityTaskManagerInternal activityTaskManagerInternal = this.mService.mAtmInternal;
                this.mDreamingSleepToken = activityTaskManagerInternal.acquireSleepToken("DreamOnDisplay" + displayId, displayId);
                return;
            }
            return;
        }
        ActivityTaskManagerInternal.SleepToken sleepToken = this.mDreamingSleepToken;
        if (sleepToken != null) {
            sleepToken.release();
            this.mDreamingSleepToken = null;
        }
    }

    /* access modifiers changed from: private */
    public void requestTransientBars(WindowState swipeTarget) {
        synchronized (this.mLock) {
            if (this.mService.mPolicy.isUserSetupComplete()) {
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
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void disposeInputConsumer(WindowManagerPolicy.InputConsumer inputConsumer) {
        if (inputConsumer != null) {
            inputConsumer.dismiss();
        }
        this.mInputConsumer = null;
    }

    private boolean isStatusBarKeyguard() {
        WindowState windowState = this.mStatusBar;
        return (windowState == null || (windowState.getAttrs().privateFlags & 1024) == 0) ? false : true;
    }

    private boolean isKeyguardOccluded() {
        return this.mService.mPolicy.isKeyguardOccluded();
    }

    /* access modifiers changed from: package-private */
    public void resetSystemUiVisibilityLw() {
        this.mLastSystemUiFlags = 0;
        updateSystemUiVisibilityLw();
    }

    private boolean shouldSkipUpdateSystemUiVisibility(WindowState win) {
        return win != null && (win.inFreeformWindowingMode() || win.inHwFreeFormWindowingMode() || (win.getAttrs().hwFlags & 256) != 0);
    }

    private int updateSystemUiVisibilityLw() {
        WindowState winCandidate;
        WindowState winCandidate2;
        int tmpVisibility;
        WindowState winCandidate3;
        WindowState windowState = this.mFocusedWindow;
        if (windowState == null || shouldSkipUpdateSystemUiVisibility(windowState)) {
            winCandidate = this.mTopFullscreenOpaqueWindowState;
        } else {
            winCandidate = this.mFocusedWindow;
        }
        if (winCandidate == null) {
            return 0;
        }
        if (winCandidate.getAttrs().token == this.mImmersiveModeConfirmation.getWindowToken()) {
            WindowState windowState2 = this.mLastFocusedWindow;
            boolean lastFocusCanReceiveKeys = windowState2 != null && windowState2.canReceiveKeys();
            if (isStatusBarKeyguard()) {
                winCandidate3 = this.mStatusBar;
            } else if (lastFocusCanReceiveKeys) {
                winCandidate3 = this.mLastFocusedWindow;
            } else {
                winCandidate3 = this.mTopFullscreenOpaqueWindowState;
            }
            if (winCandidate3 == null) {
                return 0;
            }
            winCandidate2 = winCandidate3;
        } else {
            winCandidate2 = winCandidate;
        }
        if (winCandidate2.getAttrs().type == 3 && this.mLastStartingWindow != winCandidate2) {
            this.mLastStartingWindow = winCandidate2;
        }
        if ((winCandidate2.getAttrs().privateFlags & 1024) != 0 && isKeyguardOccluded()) {
            return 0;
        }
        this.mDisplayContent.getInsetsStateController().onBarControllingWindowChanged(this.mTopFullscreenOpaqueWindowState);
        int tmpVisibility2 = PolicyControl.getSystemUiVisibility(winCandidate2, null) & (~this.mResettingSystemUiFlags) & (~this.mForceClearedSystemUiFlags);
        if (!this.mForcingShowNavBar || winCandidate2.getSurfaceLayer() >= this.mForcingShowNavBarLayer) {
            tmpVisibility = tmpVisibility2;
        } else {
            tmpVisibility = tmpVisibility2 & (~PolicyControl.adjustClearableFlags(winCandidate2, 7));
        }
        int fullscreenVisibility = updateLightStatusBarLw(0, this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState);
        int dockedVisibility = updateLightStatusBarLw(0, this.mTopDockedOpaqueWindowState, this.mTopDockedOpaqueOrDimmingWindowState);
        this.mService.getStackBounds(0, 2, this.mNonDockedStackBounds);
        this.mService.getStackBounds(3, 1, this.mDockedStackBounds);
        if (IS_HW_MULTIWINDOW_SUPPORTED && this.mDockedStackBounds.isEmpty()) {
            synchronized (this.mService.getGlobalLock()) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    TaskStack stack = this.mService.getRoot().getStack(100, 1);
                    if (stack == null || !stack.isVisible()) {
                        this.mDockedStackBounds.setEmpty();
                    } else {
                        stack.getBounds(this.mDockedStackBounds);
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
        Pair<Integer, Boolean> result = updateSystemBarsLw(winCandidate2, this.mLastSystemUiFlags, tmpVisibility);
        int visibility = ((Integer) result.first).intValue();
        int diff = visibility ^ this.mLastSystemUiFlags;
        int fullscreenDiff = fullscreenVisibility ^ this.mLastFullscreenStackSysUiFlags;
        int dockedDiff = dockedVisibility ^ this.mLastDockedStackSysUiFlags;
        boolean needsMenu = winCandidate2.getNeedsMenuLw(this.mTopFullscreenOpaqueWindowState);
        if (diff == 0 && fullscreenDiff == 0 && dockedDiff == 0 && this.mLastFocusNeedsMenu == needsMenu && this.mFocusedApp == winCandidate2.getAppToken() && this.mLastNonDockedStackBounds.equals(this.mNonDockedStackBounds) && this.mLastDockedStackBounds.equals(this.mDockedStackBounds) && this.mLastHwNavColor == this.mHwNavColor) {
            return 0;
        }
        this.mLastSystemUiFlags = visibility;
        this.mLastHwNavColor = this.mHwNavColor;
        this.mLastFullscreenStackSysUiFlags = fullscreenVisibility;
        this.mLastDockedStackSysUiFlags = dockedVisibility;
        this.mLastFocusNeedsMenu = needsMenu;
        this.mFocusedApp = winCandidate2.getAppToken();
        this.mLastNonDockedStackBounds.set(this.mNonDockedStackBounds);
        this.mLastDockedStackBounds.set(this.mDockedStackBounds);
        this.mHandler.post(new Runnable(visibility, winCandidate2, fullscreenVisibility, dockedVisibility, new Rect(this.mNonDockedStackBounds), new Rect(this.mDockedStackBounds), ((Boolean) result.second).booleanValue(), needsMenu) {
            /* class com.android.server.wm.$$Lambda$DisplayPolicy$WwV5WoEtlIbZTXpSEJQf7ozaI */
            private final /* synthetic */ int f$1;
            private final /* synthetic */ WindowState f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ int f$4;
            private final /* synthetic */ Rect f$5;
            private final /* synthetic */ Rect f$6;
            private final /* synthetic */ boolean f$7;
            private final /* synthetic */ boolean f$8;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
                this.f$6 = r7;
                this.f$7 = r8;
                this.f$8 = r9;
            }

            public final void run() {
                DisplayPolicy.this.lambda$updateSystemUiVisibilityLw$10$DisplayPolicy(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8);
            }
        });
        return diff;
    }

    public /* synthetic */ void lambda$updateSystemUiVisibilityLw$10$DisplayPolicy(int visibility, WindowState win, int fullscreenVisibility, int dockedVisibility, Rect fullscreenStackBounds, Rect dockedStackBounds, boolean isNavbarColorManagedByIme, boolean needsMenu) {
        int vis;
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            int displayId = getDisplayId();
            int vis2 = visibility;
            if (this.mHwNavColor) {
                vis2 = visibility | 16;
            }
            if (win.inHwMagicWindowingMode()) {
                Object[] objArr = new Object[3];
                boolean z = false;
                objArr[0] = win;
                objArr[1] = Integer.valueOf(vis2);
                if (this.mNavigationBarPosition == 4) {
                    z = true;
                }
                objArr[2] = Boolean.valueOf(z);
                vis = HwMwUtils.performPolicy((int) WindowManagerService.H.UNFREEZE_FOLD_ROTATION, objArr).getInt("RESULT_UPDATE_SYSUIVISIBILITY", vis2);
            } else {
                vis = vis2;
            }
            statusBar.setSystemUiVisibility(displayId, vis, fullscreenVisibility, dockedVisibility, -1, fullscreenStackBounds, dockedStackBounds, isNavbarColorManagedByIme, win.toString());
            statusBar.topAppWindowChanged(displayId, needsMenu);
        }
    }

    public void updateSystemUiColorLw(WindowState win) {
        this.mService.getPolicy().updateSystemUiColorLw(win);
    }

    private int updateLightStatusBarLw(int vis, WindowState opaque, WindowState opaqueOrDimming) {
        boolean onKeyguard = isStatusBarKeyguard() && !isKeyguardOccluded();
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
        boolean imeWindowCanNavColorWindow = imeWindow != null && imeWindow.isVisibleLw() && navBarPosition == 4 && (PolicyControl.getWindowFlags(imeWindow, null) & Integer.MIN_VALUE) != 0;
        if (opaque != null && opaqueOrDimming == opaque) {
            return imeWindowCanNavColorWindow ? imeWindow : opaque;
        }
        if (opaqueOrDimming == null || !opaqueOrDimming.isDimming()) {
            if (imeWindowCanNavColorWindow) {
                return imeWindow;
            }
            return null;
        } else if (imeWindowCanNavColorWindow && WindowManager.LayoutParams.mayUseInputMethod(PolicyControl.getWindowFlags(opaqueOrDimming, null))) {
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
            int vis2 = vis & -17;
            if (navColorWin == imeWindow && (navColorWin.getAttrs().hwFlags & 16) != 0) {
                vis2 |= 16;
                Slog.i(TAG, "navColorWin was set to default navbar color so should add SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR " + Integer.toHexString(vis2));
            }
            return vis2 | (PolicyControl.getSystemUiVisibility(navColorWin, null) & 16);
        } else if (navColorWin != opaqueOrDimming || !navColorWin.isDimming()) {
            return vis;
        } else {
            return vis & -17;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:123:0x01b5, code lost:
        if (r37.mForceShowSystemBars != false) goto L_0x01ba;
     */
    private Pair<Integer, Boolean> updateSystemBarsLw(WindowState win, int oldVis, int vis) {
        boolean freeformStackVisible;
        WindowState fullscreenTransWin;
        int vis2;
        boolean z;
        boolean isManagedByIme;
        boolean dockedStackVisible = this.mDisplayContent.isStackVisible(3) || this.mDisplayContent.isStackVisible(100);
        if (HwFreeFormUtils.isFreeFormEnable()) {
            freeformStackVisible = false;
        } else {
            freeformStackVisible = this.mDisplayContent.isStackVisible(5);
        }
        boolean resizing = this.mDisplayContent.getDockedDividerController().isResizing();
        this.mForceShowSystemBars = dockedStackVisible || freeformStackVisible || resizing || this.mForceShowSystemBarsFromExternal;
        boolean forceOpaqueStatusBar = this.mForceShowSystemBars && !this.mForceStatusBarFromKeyguard;
        if (!isStatusBarKeyguard() || isKeyguardOccluded()) {
            fullscreenTransWin = this.mTopFullscreenOpaqueWindowState;
        } else {
            fullscreenTransWin = this.mStatusBar;
        }
        int vis3 = this.mNavigationBarController.applyTranslucentFlagLw(fullscreenTransWin, this.mStatusBarController.applyTranslucentFlagLw(fullscreenTransWin, vis, oldVis), oldVis);
        int dockedVis = this.mNavigationBarController.applyTranslucentFlagLw(this.mTopDockedOpaqueWindowState, this.mStatusBarController.applyTranslucentFlagLw(this.mTopDockedOpaqueWindowState, 0, 0), 0);
        boolean fullscreenDrawsStatusBarBackground = drawsStatusBarBackground(vis3, this.mTopFullscreenOpaqueWindowState);
        boolean dockedDrawsStatusBarBackground = drawsStatusBarBackground(dockedVis, this.mTopDockedOpaqueWindowState);
        boolean fullscreenDrawsNavBarBackground = drawsNavigationBarBackground(vis3, this.mTopFullscreenOpaqueWindowState);
        boolean dockedDrawsNavigationBarBackground = drawsNavigationBarBackground(dockedVis, this.mTopDockedOpaqueWindowState);
        boolean statusBarHasFocus = win.getAttrs().type == 2000;
        if (statusBarHasFocus && !isStatusBarKeyguard()) {
            int flags = 14342;
            if (isKeyguardOccluded()) {
                flags = 14342 | -1073741824;
            }
            vis3 = ((~flags) & vis3) | (oldVis & flags);
        }
        if (fullscreenDrawsStatusBarBackground && dockedDrawsStatusBarBackground) {
            vis2 = (vis3 | 8) & -1073741825;
        } else if (forceOpaqueStatusBar) {
            vis2 = vis3 & -1073741833;
        } else {
            vis2 = vis3;
        }
        int vis4 = configureNavBarOpacity(vis2, dockedStackVisible, freeformStackVisible, resizing, fullscreenDrawsNavBarBackground, dockedDrawsNavigationBarBackground);
        boolean immersiveSticky = (vis4 & 4096) != 0;
        boolean isLeftRightSplitStackVisible = false;
        if (dockedStackVisible) {
            isLeftRightSplitStackVisible = isLeftRightSplitStackVisible();
        }
        WindowState windowState = this.mTopFullscreenOpaqueWindowState;
        boolean hideStatusBarWM = !(windowState == null || (PolicyControl.getWindowFlags(windowState, null) & 1024) == 0) || isLeftRightSplitStackVisible;
        boolean hideStatusBarSysui = (vis4 & 4) != 0;
        boolean hideNavBarSysui = (vis4 & 2) != 0;
        boolean transientStatusBarAllowed = this.mStatusBar != null && (statusBarHasFocus || ((!this.mForceShowSystemBars && (hideStatusBarWM || (hideStatusBarSysui && immersiveSticky))) || isLeftRightSplitStackVisible));
        boolean transientNavBarAllowed = this.mNavigationBar != null && ((!this.mForceShowSystemBars && hideNavBarSysui && hideStatusBarWM) || (!this.mForceShowSystemBars && hideNavBarSysui && immersiveSticky));
        long now = SystemClock.uptimeMillis();
        long j = this.mPendingPanicGestureUptime;
        boolean pendingPanic = j != 0 && now - j <= PANIC_GESTURE_EXPIRATION;
        DisplayPolicy defaultDisplayPolicy = this.mService.getDefaultDisplayContentLocked().getDisplayPolicy();
        if (pendingPanic && hideNavBarSysui && !isStatusBarKeyguard() && defaultDisplayPolicy.isKeyguardDrawComplete()) {
            this.mPendingPanicGestureUptime = 0;
            this.mStatusBarController.showTransient();
            if (!isNavBarEmpty(vis4)) {
                this.mNavigationBarController.showTransient();
            }
        }
        boolean denyTransientStatus = this.mStatusBarController.isTransientShowRequested() && !transientStatusBarAllowed && hideStatusBarSysui;
        boolean denyTransientNav = this.mNavigationBarController.isTransientShowRequested() && !transientNavBarAllowed;
        if (denyTransientStatus || denyTransientNav) {
        }
        clearClearableFlagsLw();
        vis4 &= -8;
        boolean navAllowedHidden = ((vis4 & 2048) != 0) || (vis4 & true);
        if (hideNavBarSysui && !navAllowedHidden) {
            if (this.mService.mPolicy.getWindowLayerLw(win) > this.mService.mPolicy.getWindowLayerFromTypeLw(2022)) {
                vis4 &= -3;
            }
        }
        if (IS_NOTCH_PROP && this.mService.mAtmService.mHwATMSEx.isSplitStackVisible(this.mDisplayContent.mAcitvityDisplay, 0) && !win.toString().contains("HwGlobalActions")) {
            vis4 &= -8201;
        }
        int vis5 = this.mStatusBarController.updateVisibilityLw(transientStatusBarAllowed, oldVis, vis4);
        boolean oldImmersiveMode = isImmersiveMode(oldVis);
        boolean newImmersiveMode = isImmersiveMode(vis5);
        this.mHwDisplayPolicyEx.setNaviImmersiveMode(newImmersiveMode);
        if (oldImmersiveMode != newImmersiveMode) {
            this.mImmersiveModeConfirmation.immersiveModeChangedLw(win.getOwningPackage(), newImmersiveMode, this.mService.mPolicy.isUserSetupComplete(), isNavBarEmpty(win.getSystemUiVisibility()));
        }
        int vis6 = this.mNavigationBarController.updateVisibilityLw(transientNavBarAllowed, oldVis, vis5);
        WindowState navColorWin = chooseNavigationColorWindowLw(this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState, this.mDisplayContent.mInputMethodWindow, this.mNavigationBarPosition);
        int vis7 = updateLightNavigationBarLw(vis6, this.mTopFullscreenOpaqueWindowState, this.mTopFullscreenOpaqueOrDimmingWindowState, this.mDisplayContent.mInputMethodWindow, navColorWin);
        if ((win.getAttrs().hwFlags & 16) == 0 || navColorWin != this.mTopFullscreenOpaqueWindowState) {
            isManagedByIme = true;
            z = false;
            this.mHwNavColor = false;
        } else {
            isManagedByIme = true;
            this.mHwNavColor = true;
            z = false;
        }
        if (navColorWin == null || navColorWin != this.mDisplayContent.mInputMethodWindow) {
            isManagedByIme = z;
        }
        return Pair.create(Integer.valueOf(vis7), Boolean.valueOf(isManagedByIme));
    }

    private boolean drawsBarBackground(int vis, WindowState win, BarController controller, int translucentFlag) {
        if (!controller.isTransparentAllowed(win)) {
            return false;
        }
        if (win == null) {
            return true;
        }
        boolean drawsSystemBars = (win.getAttrs().flags & Integer.MIN_VALUE) != 0;
        if ((win.getAttrs().privateFlags & 131072) != 0) {
            return true;
        }
        if (!drawsSystemBars || (vis & translucentFlag) != 0) {
            return false;
        }
        return true;
    }

    private boolean drawsStatusBarBackground(int vis, WindowState win) {
        return drawsBarBackground(vis, win, this.mStatusBarController, 67108864);
    }

    private boolean drawsNavigationBarBackground(int vis, WindowState win) {
        return drawsBarBackground(vis, win, this.mNavigationBarController, 134217728);
    }

    private int configureNavBarOpacity(int visibility, boolean dockedStackVisible, boolean freeformStackVisible, boolean isDockedDividerResizing, boolean fullscreenDrawsBackground, boolean dockedDrawsNavigationBarBackground) {
        int i = this.mNavBarOpacityMode;
        if (i == 2) {
            if (fullscreenDrawsBackground && dockedDrawsNavigationBarBackground) {
                return setNavBarTransparentFlag(visibility);
            }
            if (dockedStackVisible) {
                return setNavBarOpaqueFlag(visibility);
            }
            return visibility;
        } else if (i == 0) {
            if (dockedStackVisible || freeformStackVisible || isDockedDividerResizing) {
                return setNavBarOpaqueFlag(visibility);
            }
            if (fullscreenDrawsBackground) {
                return setNavBarTransparentFlag(visibility);
            }
            return visibility;
        } else if (i != 1) {
            return visibility;
        } else {
            if (isDockedDividerResizing) {
                return setNavBarOpaqueFlag(visibility);
            }
            if (freeformStackVisible) {
                return setNavBarTranslucentFlag(visibility);
            }
            return setNavBarOpaqueFlag(visibility);
        }
    }

    private int setNavBarOpaqueFlag(int visibility) {
        return 2147450879 & visibility;
    }

    private int setNavBarTranslucentFlag(int visibility) {
        return Integer.MIN_VALUE | (visibility & -32769);
    }

    private int setNavBarTransparentFlag(int visibility) {
        return 32768 | (visibility & Integer.MAX_VALUE);
    }

    private void clearClearableFlagsLw() {
        int i = this.mResettingSystemUiFlags;
        int newVal = i | 7;
        if (newVal != i) {
            this.mResettingSystemUiFlags = newVal;
            this.mDisplayContent.reevaluateStatusBarVisibility();
        }
    }

    private boolean isImmersiveMode(int vis) {
        return (this.mNavigationBar == null || (vis & 2) == 0 || (vis & 6144) == 0 || !canHideNavigationBar()) ? false : true;
    }

    private boolean canHideNavigationBar() {
        return hasNavigationBar();
    }

    /* access modifiers changed from: private */
    public static boolean isNavBarEmpty(int systemUiFlags) {
        return (systemUiFlags & 23068672) == 23068672;
    }

    /* access modifiers changed from: package-private */
    public boolean shouldRotateSeamlessly(DisplayRotation displayRotation, int oldRotation, int newRotation) {
        if (oldRotation == displayRotation.getUpsideDownRotation() || newRotation == displayRotation.getUpsideDownRotation()) {
            return false;
        }
        if (!navigationBarCanMove() && !this.mAllowSeamlessRotationDespiteNavBarMoving) {
            return false;
        }
        notifyRotate(oldRotation, newRotation);
        WindowState w = this.mTopFullscreenOpaqueWindowState;
        if (w == null || w != this.mFocusedWindow) {
            return false;
        }
        if ((w.mAppToken == null || w.mAppToken.matchParentBounds()) && !w.isAnimatingLw() && w.getAttrs().rotationAnimation == 3) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void onPowerKeyDown(boolean isScreenOn) {
        if (this.mImmersiveModeConfirmation.onPowerKeyDown(isScreenOn, SystemClock.elapsedRealtime(), isImmersiveMode(this.mLastSystemUiFlags), isNavBarEmpty(this.mLastSystemUiFlags))) {
            this.mHandler.post(this.mHiddenNavPanic);
        }
    }

    /* access modifiers changed from: package-private */
    public void onVrStateChangedLw(boolean enabled) {
        this.mImmersiveModeConfirmation.onVrStateChangedLw(enabled);
    }

    public void onLockTaskStateChangedLw(int lockTaskState) {
        this.mImmersiveModeConfirmation.onLockTaskModeChangedLw(lockTaskState);
        IHwDisplayPolicyEx iHwDisplayPolicyEx = this.mHwDisplayPolicyEx;
        if (iHwDisplayPolicyEx != null) {
            iHwDisplayPolicyEx.onLockTaskStateChangedLw(lockTaskState);
        }
    }

    public void takeScreenshot(int screenshotType) {
        ScreenshotHelper screenshotHelper = this.mScreenshotHelper;
        if (screenshotHelper != null) {
            WindowState windowState = this.mStatusBar;
            boolean z = true;
            boolean z2 = windowState != null && windowState.isVisibleLw();
            WindowState windowState2 = this.mNavigationBar;
            if (windowState2 == null || !windowState2.isVisibleLw()) {
                z = false;
            }
            screenshotHelper.takeScreenshot(screenshotType, z2, z, this.mHandler);
        }
    }

    /* access modifiers changed from: package-private */
    public RefreshRatePolicy getRefreshRatePolicy() {
        return this.mRefreshRatePolicy;
    }

    /* access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("DisplayPolicy");
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mCarDockEnablesAccelerometer=");
        pw.print(this.mCarDockEnablesAccelerometer);
        pw.print(" mDeskDockEnablesAccelerometer=");
        pw.println(this.mDeskDockEnablesAccelerometer);
        pw.print(prefix2);
        pw.print("mDockMode=");
        pw.print(Intent.dockStateToString(this.mDockMode));
        pw.print(" mLidState=");
        pw.println(WindowManagerPolicy.WindowManagerFuncs.lidStateToString(this.mLidState));
        pw.print(prefix2);
        pw.print("mAwake=");
        pw.print(this.mAwake);
        pw.print(" mScreenOnEarly=");
        pw.print(this.mScreenOnEarly);
        pw.print(" mScreenOnFully=");
        pw.println(this.mScreenOnFully);
        pw.print(prefix2);
        pw.print("mKeyguardDrawComplete=");
        pw.print(this.mKeyguardDrawComplete);
        pw.print(" mWindowManagerDrawComplete=");
        pw.println(this.mWindowManagerDrawComplete);
        pw.print(prefix2);
        pw.print("mHdmiPlugged=");
        pw.println(this.mHdmiPlugged);
        if (!(this.mLastSystemUiFlags == 0 && this.mResettingSystemUiFlags == 0 && this.mForceClearedSystemUiFlags == 0)) {
            pw.print(prefix2);
            pw.print("mLastSystemUiFlags=0x");
            pw.print(Integer.toHexString(this.mLastSystemUiFlags));
            pw.print(" mResettingSystemUiFlags=0x");
            pw.print(Integer.toHexString(this.mResettingSystemUiFlags));
            pw.print(" mForceClearedSystemUiFlags=0x");
            pw.println(Integer.toHexString(this.mForceClearedSystemUiFlags));
        }
        if (this.mLastFocusNeedsMenu) {
            pw.print(prefix2);
            pw.print("mLastFocusNeedsMenu=");
            pw.println(this.mLastFocusNeedsMenu);
        }
        pw.print(prefix2);
        pw.print("mShowingDream=");
        pw.print(this.mShowingDream);
        pw.print(" mDreamingLockscreen=");
        pw.print(this.mDreamingLockscreen);
        pw.print(" mDreamingSleepToken=");
        pw.println(this.mDreamingSleepToken);
        if (this.mStatusBar != null) {
            pw.print(prefix2);
            pw.print("mStatusBar=");
            pw.print(this.mStatusBar);
            pw.print(" isStatusBarKeyguard=");
            pw.println(isStatusBarKeyguard());
        }
        if (this.mNavigationBar != null) {
            pw.print(prefix2);
            pw.print("mNavigationBar=");
            pw.println(this.mNavigationBar);
            pw.print(prefix2);
            pw.print("mNavBarOpacityMode=");
            pw.println(this.mNavBarOpacityMode);
            pw.print(prefix2);
            pw.print("mNavigationBarCanMove=");
            pw.println(this.mNavigationBarCanMove);
            pw.print(prefix2);
            pw.print("mNavigationBarPosition=");
            pw.println(this.mNavigationBarPosition);
        }
        if (this.mFocusedWindow != null) {
            pw.print(prefix2);
            pw.print("mFocusedWindow=");
            pw.println(this.mFocusedWindow);
        }
        if (this.mFocusedApp != null) {
            pw.print(prefix2);
            pw.print("mFocusedApp=");
            pw.println(this.mFocusedApp);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            pw.print(prefix2);
            pw.print("mTopFullscreenOpaqueWindowState=");
            pw.println(this.mTopFullscreenOpaqueWindowState);
        }
        if (this.mTopFullscreenOpaqueOrDimmingWindowState != null) {
            pw.print(prefix2);
            pw.print("mTopFullscreenOpaqueOrDimmingWindowState=");
            pw.println(this.mTopFullscreenOpaqueOrDimmingWindowState);
        }
        if (this.mForcingShowNavBar) {
            pw.print(prefix2);
            pw.print("mForcingShowNavBar=");
            pw.println(this.mForcingShowNavBar);
            pw.print(prefix2);
            pw.print("mForcingShowNavBarLayer=");
            pw.println(this.mForcingShowNavBarLayer);
        }
        pw.print(prefix2);
        pw.print("mTopIsFullscreen=");
        pw.print(this.mTopIsFullscreen);
        pw.print(prefix2);
        pw.print("mForceStatusBar=");
        pw.print(this.mForceStatusBar);
        pw.print(" mForceStatusBarFromKeyguard=");
        pw.println(this.mForceStatusBarFromKeyguard);
        pw.print(" mForceShowSystemBarsFromExternal=");
        pw.println(this.mForceShowSystemBarsFromExternal);
        pw.print(prefix2);
        pw.print("mAllowLockscreenWhenOn=");
        pw.println(this.mAllowLockscreenWhenOn);
        this.mStatusBarController.dump(pw, prefix2);
        this.mNavigationBarController.dump(pw, prefix2);
        pw.print(prefix2);
        pw.println("Looper state:");
        this.mHandler.getLooper().dump(new PrintWriterPrinter(pw), prefix2 + "  ");
        this.mHwDisplayPolicyEx.dumpPC(prefix2, pw);
    }

    @Override // com.huawei.server.wm.IHwDisplayPolicyInner
    public WindowState getStatusBar() {
        return this.mStatusBar;
    }

    public WindowState getNavigationBar() {
        return this.mNavigationBar;
    }

    @Override // com.huawei.server.wm.IHwDisplayPolicyInner
    public WindowState getFocusedWindow() {
        return this.mFocusedWindow;
    }

    public WindowState getInputMethodWindow() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent == null) {
            return null;
        }
        return displayContent.mInputMethodWindow;
    }

    public WindowState getLastFocusedWindow() {
        return this.mLastFocusedWindow;
    }

    public WindowState getTopFullscreenOpaqueWindowState() {
        return this.mTopFullscreenOpaqueWindowState;
    }

    public int getLastSystemUiFlags() {
        return this.mLastSystemUiFlags;
    }

    @Override // com.huawei.server.wm.IHwDisplayPolicyInner
    public IHwDisplayPolicyEx getHwDisplayPolicyEx() {
        return this.mHwDisplayPolicyEx;
    }

    private WindowState getNavigationBarExternal() {
        return this.mHwDisplayPolicyEx.getNavigationBarExternal();
    }

    public void layoutWindowForPadPCMode(WindowState win, Rect pf, Rect df, Rect cf, Rect vf, int mContentBottom) {
    }

    public void registerExternalPointerEventListener() {
        this.mHwDisplayPolicyEx.registerExternalPointerEventListener();
    }

    public void unRegisterExternalPointerEventListener() {
        this.mHwDisplayPolicyEx.unRegisterExternalPointerEventListener();
    }

    public void setNaviBarFlag(AppWindowToken focuseApp, WindowState inputMethodWindow) {
        this.mHwDisplayPolicyEx.setInputMethodWindowVisible(inputMethodWindow == null ? false : inputMethodWindow.isVisibleLw());
        if (focuseApp != null) {
            this.mHwDisplayPolicyEx.setNaviBarFlag(focuseApp.navigationBarHide);
        }
    }

    public boolean isTopIsFullscreen() {
        return this.mTopIsFullscreen;
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

    public void requestHwTransientBars(WindowState swipeTarget) {
        requestTransientBars(swipeTarget);
    }

    private void notifyRotate(int oldRotation, int newRotation) {
        RemoteCallbackList<IHwRotateObserver> remoteCallbackList = this.mHwRotateObservers;
        if (remoteCallbackList != null && newRotation != oldRotation) {
            int i = remoteCallbackList.beginBroadcast();
            while (i > 0) {
                i--;
                IHwRotateObserver observer = this.mHwRotateObservers.getBroadcastItem(i);
                try {
                    observer.onRotate(oldRotation, newRotation);
                    Slog.i(TAG, "notifyRotate() to observer " + observer);
                } catch (Exception e) {
                    Slog.w(TAG, "Exception in notifyRotate(), remove observer " + observer);
                    unregisterRotateObserver(observer);
                }
            }
            this.mHwRotateObservers.finishBroadcast();
        }
    }

    @Override // com.huawei.server.wm.IHwDisplayPolicyInner
    public void setNavigationBarHeightDef(int[] values) {
        this.mNavigationBarHeightForRotationDefault = values;
        this.mNavigationBarFrameHeightForRotationDefault = values;
    }

    public Point getGestureStartedPoint() {
        return this.mSystemGestures.getGestureStartedPoint();
    }

    @Override // com.huawei.server.wm.IHwDisplayPolicyInner
    public void setNavigationBarWidthDef(int[] values) {
        this.mNavigationBarWidthForRotationDefault = values;
    }

    public void updateNavigationBar(boolean minNaviBar) {
        this.mHwDisplayPolicyEx.updateNavigationBar(minNaviBar);
    }

    public boolean isLastImmersiveMode() {
        return isImmersiveMode(this.mLastSystemUiFlags);
    }

    public boolean swipeFromTop() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
            return true;
        }
        if (!mIsHwNaviBar) {
            return false;
        }
        if (!isLastImmersiveMode()) {
            requestTransientStatusBars();
        } else {
            requestTransientBars(this.mStatusBar);
        }
        return true;
    }

    public void requestTransientStatusBars() {
        synchronized (this.mService.getGlobalLock()) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                BarController barController = this.mStatusBarController;
                boolean sb = false;
                if (barController != null) {
                    sb = barController.checkShowTransientBarLw();
                }
                if (sb && barController != null) {
                    barController.showTransient();
                }
                if (this.mImmersiveModeConfirmation != null) {
                    this.mImmersiveModeConfirmation.confirmCurrentPrompt();
                }
                updateSystemUiVisibilityLw();
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public int getRestrictedScreenHeight() {
        return this.mRestrictedScreenHeight;
    }

    public int getDisplayRotation() {
        return this.mDisplayRotation;
    }

    public boolean getForceNotchStatusBar() {
        return this.mForceNotchStatusBar;
    }

    public void setForceNotchStatusBar(boolean forceNotchStatusBar) {
        this.mForceNotchStatusBar = forceNotchStatusBar;
    }

    public void setNotchStatusBarColorLw(int color) {
        this.mNotchStatusBarColorLw = color;
    }

    public int getWindowFlags(WindowState win, WindowManager.LayoutParams attrs) {
        return PolicyControl.getWindowFlags(win, attrs);
    }

    private boolean supportsPointerLocation() {
        return this.mDisplayContent.isDefaultDisplay || !this.mDisplayContent.isPrivate();
    }

    /* access modifiers changed from: package-private */
    public void setPointerLocationEnabled(boolean pointerLocationEnabled) {
        if (supportsPointerLocation()) {
            this.mHandler.sendEmptyMessage(pointerLocationEnabled ? 4 : 5);
        }
    }

    /* access modifiers changed from: private */
    public void enablePointerLocation() {
        if (this.mPointerLocationView == null) {
            this.mPointerLocationView = new PointerLocationView(this.mContext);
            this.mPointerLocationView.setPrintCoords(false);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1);
            lp.type = 2015;
            lp.flags = 1304;
            lp.layoutInDisplayCutoutMode = 1;
            lp.layoutInDisplaySideMode = 1;
            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= 16777216;
                lp.privateFlags |= 2;
            }
            lp.format = -3;
            lp.setTitle("PointerLocation - display " + getDisplayId());
            lp.inputFeatures = lp.inputFeatures | 2;
            ((WindowManager) this.mContext.getSystemService(WindowManager.class)).addView(this.mPointerLocationView, lp);
            this.mDisplayContent.registerPointerEventListener(this.mPointerLocationView);
        }
    }

    /* access modifiers changed from: private */
    public void disablePointerLocation() {
        WindowManagerPolicyConstants.PointerEventListener pointerEventListener = this.mPointerLocationView;
        if (pointerEventListener != null) {
            this.mDisplayContent.unregisterPointerEventListener(pointerEventListener);
            ((WindowManager) this.mContext.getSystemService(WindowManager.class)).removeView(this.mPointerLocationView);
            this.mPointerLocationView = null;
        }
    }

    public boolean isLeftRightSplitStackVisible() {
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            return this.mService.mAtmService.mHwATMSEx.isSplitStackVisible(this.mDisplayContent.mAcitvityDisplay, 1);
        }
        int i = this.mDisplayRotation;
        if ((i == 1 || i == 3) && this.mDisplayContent.isStackVisible(3)) {
            return true;
        }
        return false;
    }

    public void setFullScreenWinVisibile(boolean visible) {
        this.isHwFullScreenWinVisibility = visible;
    }

    public void setFullScreenWindow(WindowState win) {
        this.mHwFullScreenWindow = win;
    }

    public boolean isAppNeedExpand(String packageName) {
        return this.mHwDisplayPolicyEx.isAppNeedExpand(packageName);
    }

    public boolean checkShowTransientBarLw() {
        StatusBarController statusBarController = this.mStatusBarController;
        if (statusBarController != null) {
            return statusBarController.checkShowTransientBarLw();
        }
        return false;
    }
}
