package com.android.server.wm;

import android.common.HwFrameworkFactory;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.iawareperf.UniPerf;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Jlog;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.WindowManager.LayoutParams;
import android.vrsystem.IVRSystemServiceManager;
import com.android.internal.util.ArrayUtils;
import com.android.server.EventLogTags;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IDisplayEffectMonitor;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.IHwShutdownThread;
import com.huawei.pgmng.log.LogPower;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

class RootWindowContainer extends WindowContainer<DisplayContent> {
    private static final int SET_SCREEN_BRIGHTNESS_OVERRIDE = 1;
    private static final int SET_USER_ACTIVITY_TIMEOUT = 2;
    private static final String TAG = "WindowManager";
    private static final Consumer<WindowState> sRemoveReplacedWindowsConsumer = -$$Lambda$RootWindowContainer$Vvv8jzH2oSE9-eakZwTuKd5NpsU.INSTANCE;
    private float mAppBrightnessLast = -1.0f;
    String mAppBrightnessPackageName;
    private String mAppBrightnessPackageNameLast = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private final Consumer<WindowState> mCloseSystemDialogsConsumer = new -$$Lambda$RootWindowContainer$qT2ficAmvrvFcBdiJIGNKxJ8Z9Q(this);
    private String mCloseSystemDialogsReason;
    private IDisplayEffectMonitor mDisplayEffectMonitor;
    private final Transaction mDisplayTransaction = new Transaction();
    private final Handler mHandler;
    private Session mHoldScreen = null;
    WindowState mHoldScreenWindow = null;
    private Object mLastWindowFreezeSource = null;
    private boolean mObscureApplicationContentOnSecondaryDisplays = false;
    WindowState mObscuringWindow = null;
    boolean mOrientationChangeComplete = true;
    private float mScreenBrightness = -1.0f;
    private boolean mSustainedPerformanceModeCurrent = false;
    private boolean mSustainedPerformanceModeEnabled = false;
    private final ArrayList<Integer> mTmpStackIds = new ArrayList();
    private final ArrayList<TaskStack> mTmpStackList = new ArrayList();
    private boolean mUpdateRotation = false;
    private long mUserActivityTimeout = -1;
    boolean mWallpaperActionPending = false;
    final WallpaperController mWallpaperController;
    private boolean mWallpaperForceHidingChanged = false;
    boolean mWallpaperMayChange = false;

    private final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    RootWindowContainer.this.mService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(msg.arg1);
                    return;
                case 2:
                    RootWindowContainer.this.mService.mPowerManagerInternal.setUserActivityTimeoutOverrideFromWindowManager(((Long) msg.obj).longValue());
                    return;
                default:
                    return;
            }
        }
    }

    public static /* synthetic */ void lambda$new$0(RootWindowContainer rootWindowContainer, WindowState w) {
        if (w.mHasSurface) {
            try {
                w.mClient.closeSystemDialogs(rootWindowContainer.mCloseSystemDialogsReason);
            } catch (RemoteException e) {
            }
        }
    }

    static /* synthetic */ void lambda$static$1(WindowState w) {
        AppWindowToken aToken = w.mAppToken;
        if (aToken != null) {
            aToken.removeReplacedWindowIfNeeded(w);
        }
    }

    private void sendBrightnessToMonitor(float brightness, String packageName) {
        if (this.mDisplayEffectMonitor != null && packageName != null) {
            if (((double) Math.abs(brightness - this.mAppBrightnessLast)) > 1.0E-7d || !this.mAppBrightnessPackageNameLast.equals(packageName)) {
                ArrayMap<String, Object> params = new ArrayMap();
                params.put("paramType", "windowManagerBrightness");
                params.put("brightness", Integer.valueOf(toBrightnessOverride(brightness)));
                params.put("packageName", packageName);
                this.mDisplayEffectMonitor.sendMonitorParam(params);
                this.mAppBrightnessLast = brightness;
                this.mAppBrightnessPackageNameLast = packageName;
            }
        }
    }

    RootWindowContainer(WindowManagerService service) {
        super(service);
        this.mHandler = new MyHandler(service.mH.getLooper());
        this.mWallpaperController = new WallpaperController(this.mService);
        this.mDisplayEffectMonitor = HwServiceFactory.getDisplayEffectMonitor(this.mService.mContext);
        if (this.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "HwServiceFactory getDisplayEffectMonitor failed!");
        }
    }

    WindowState computeFocusedWindow() {
        if (HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode()) {
            DisplayContent dc;
            if (HwVRUtils.isVRMode()) {
                dc = getDisplayContent(HwVRUtils.getVRDisplayID());
                IVRSystemServiceManager vrMananger = HwFrameworkFactory.getVRSystemServiceManager();
                if (vrMananger != null && vrMananger.isVirtualScreenMode()) {
                    dc = getDisplayContent(0);
                }
            } else {
                dc = getDisplayContent(this.mService.getFocusedDisplayId());
            }
            if (dc != null) {
                WindowState win = dc.findFocusedWindow();
                if (win != null) {
                    return win;
                }
            }
        }
        boolean forceDefaultDisplay = this.mService.isKeyguardShowingAndNotOccluded();
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc2 = (DisplayContent) this.mChildren.get(i);
            WindowState win2 = dc2.findFocusedWindow();
            if (win2 != null) {
                if (!forceDefaultDisplay || dc2.isDefaultDisplay) {
                    return win2;
                }
                EventLog.writeEvent(1397638484, new Object[]{"71786287", Integer.valueOf(win2.mOwnerUid), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS});
            }
        }
        return null;
    }

    void getDisplaysInFocusOrder(SparseIntArray displaysInFocusOrder) {
        displaysInFocusOrder.clear();
        int size = this.mChildren.size();
        for (int i = 0; i < size; i++) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(i);
            if (!displayContent.isRemovalDeferred()) {
                displaysInFocusOrder.put(i, displayContent.getDisplayId());
            }
        }
    }

    DisplayContent getDisplayContent(int displayId) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent current = (DisplayContent) this.mChildren.get(i);
            if (current.getDisplayId() == displayId) {
                return current;
            }
        }
        return null;
    }

    DisplayContent createDisplayContent(Display display, DisplayWindowController controller) {
        int displayId = display.getDisplayId();
        DisplayContent existing = getDisplayContent(displayId);
        if (existing != null) {
            existing.setController(controller);
            return existing;
        }
        DisplayContent dc = HwServiceFactory.createDisplayContent(display, this.mService, this.mWallpaperController, controller);
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding display=");
            stringBuilder.append(display);
            Slog.v(str, stringBuilder.toString());
        }
        DisplayInfo displayInfo = dc.getDisplayInfo();
        Rect rect = new Rect();
        this.mService.mDisplaySettings.getOverscanLocked(displayInfo.name, displayInfo.uniqueId, rect);
        displayInfo.overscanLeft = rect.left;
        displayInfo.overscanTop = rect.top;
        displayInfo.overscanRight = rect.right;
        displayInfo.overscanBottom = rect.bottom;
        if (this.mService.mDisplayManagerInternal != null) {
            this.mService.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(displayId, displayInfo);
            dc.configureDisplayPolicy();
            if (this.mService.canDispatchPointerEvents()) {
                if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Registering PointerEventListener for DisplayId: ");
                    stringBuilder2.append(displayId);
                    Slog.d(str2, stringBuilder2.toString());
                }
                dc.mTapDetector = new TaskTapPointerEventListener(this.mService, dc);
                this.mService.registerPointerEventListener(dc.mTapDetector);
                if (displayId == 0) {
                    this.mService.registerPointerEventListener(this.mService.mMousePositionTracker);
                }
            }
            boolean z = true;
            boolean displayInfoType = displayInfo.type == 2;
            if (HwPCUtils.isWirelessProjectionEnabled()) {
                if (!(displayInfo.type == 2 || displayInfo.type == 3)) {
                    z = false;
                }
                displayInfoType = z;
            }
            if (HwPCUtils.enabled() && displayId != -1 && displayId != 0 && ((displayInfoType || (((displayInfo.type == 5 || displayInfo.type == 4) && SystemProperties.getBoolean("hw_pc_support_overlay", false)) || (displayInfo.type == 5 && ("com.hpplay.happycast".equals(displayInfo.ownerPackageName) || "com.huawei.works".equals(displayInfo.ownerPackageName))))) && this.mService.canDispatchExternalPointerEvents())) {
                dc.mTapDetector = new TaskTapPointerEventListener(this.mService, dc);
                this.mService.registerExternalPointerEventListener(dc.mTapDetector);
                try {
                    this.mService.registerExternalPointerEventListener(this.mService.mMousePositionTracker);
                } catch (Exception e) {
                    Slog.w(TAG, "register external pointer event listener", e);
                }
            }
        }
        return dc;
    }

    boolean isLayoutNeeded() {
        int numDisplays = this.mChildren.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            if (((DisplayContent) this.mChildren.get(displayNdx)).isLayoutNeeded()) {
                return true;
            }
        }
        return false;
    }

    void getWindowsByName(ArrayList<WindowState> output, String name) {
        int objectId = 0;
        try {
            objectId = Integer.parseInt(name, 16);
            name = null;
        } catch (RuntimeException e) {
        }
        getWindowsByName(output, name, objectId);
    }

    private void getWindowsByName(ArrayList<WindowState> output, String name, int objectId) {
        forAllWindows((Consumer) new -$$Lambda$RootWindowContainer$O6gArs92KbWUhitra1og4WTg69c(name, output, objectId), true);
    }

    static /* synthetic */ void lambda$getWindowsByName$2(String name, ArrayList output, int objectId, WindowState w) {
        if (name != null) {
            if (w.mAttrs.getTitle().toString().contains(name)) {
                output.add(w);
            }
        } else if (System.identityHashCode(w) == objectId) {
            output.add(w);
        }
    }

    AppWindowToken getAppWindowToken(IBinder binder) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken atoken = ((DisplayContent) this.mChildren.get(i)).getAppWindowToken(binder);
            if (atoken != null) {
                return atoken;
            }
        }
        return null;
    }

    DisplayContent getWindowTokenDisplay(WindowToken token) {
        if (token == null) {
            return null;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(i);
            if (dc.getWindowToken(token.token) == token) {
                return dc;
            }
        }
        return null;
    }

    int[] setDisplayOverrideConfigurationIfNeeded(Configuration newConfiguration, int displayId) {
        DisplayContent displayContent = getDisplayContent(displayId);
        if (displayContent != null) {
            int i = 0;
            int[] iArr = null;
            if (displayContent.getOverrideConfiguration().diff(newConfiguration) != 0) {
                displayContent.onOverrideConfigurationChanged(newConfiguration);
                this.mTmpStackList.clear();
                if (displayId == 0) {
                    setGlobalConfigurationIfNeeded(newConfiguration, this.mTmpStackList);
                } else {
                    updateStackBoundsAfterConfigChange(displayId, this.mTmpStackList);
                }
                this.mTmpStackIds.clear();
                int stackCount = this.mTmpStackList.size();
                while (i < stackCount) {
                    TaskStack stack = (TaskStack) this.mTmpStackList.get(i);
                    if (!stack.mDeferRemoval) {
                        this.mTmpStackIds.add(Integer.valueOf(stack.mStackId));
                    }
                    i++;
                }
                if (!this.mTmpStackIds.isEmpty()) {
                    iArr = ArrayUtils.convertToIntArray(this.mTmpStackIds);
                }
                return iArr;
            }
            Slog.i(TAG, "Do not change display override configuration.");
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display not found for id: ");
        stringBuilder.append(displayId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void setGlobalConfigurationIfNeeded(Configuration newConfiguration, List<TaskStack> changedStacks) {
        if (getConfiguration().diff(newConfiguration) != 0) {
            onConfigurationChanged(newConfiguration);
            updateStackBoundsAfterConfigChange(changedStacks);
        }
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        prepareFreezingTaskBounds();
        super.onConfigurationChanged(newParentConfig);
        this.mService.mPolicy.onConfigurationChanged();
    }

    private void updateStackBoundsAfterConfigChange(List<TaskStack> changedStacks) {
        int numDisplays = this.mChildren.size();
        for (int i = 0; i < numDisplays; i++) {
            ((DisplayContent) this.mChildren.get(i)).updateStackBoundsAfterConfigChange(changedStacks);
        }
    }

    private void updateStackBoundsAfterConfigChange(int displayId, List<TaskStack> changedStacks) {
        getDisplayContent(displayId).updateStackBoundsAfterConfigChange(changedStacks);
    }

    private void prepareFreezingTaskBounds() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((DisplayContent) this.mChildren.get(i)).prepareFreezingTaskBounds();
        }
    }

    TaskStack getStack(int windowingMode, int activityType) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            TaskStack stack = ((DisplayContent) this.mChildren.get(i)).getStack(windowingMode, activityType);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    void setSecureSurfaceState(int userId, boolean disabled) {
        forAllWindows((Consumer) new -$$Lambda$RootWindowContainer$3VVFoec4x74e1MMAq03gYI9kKjo(userId, disabled), true);
    }

    static /* synthetic */ void lambda$setSecureSurfaceState$3(int userId, boolean disabled, WindowState w) {
        if (w.mHasSurface && userId == UserHandle.getUserId(w.mOwnerUid)) {
            w.mWinAnimator.setSecureLocked(disabled);
        }
    }

    void updateHiddenWhileSuspendedState(ArraySet<String> packages, boolean suspended) {
        forAllWindows((Consumer) new -$$Lambda$RootWindowContainer$9Gi6QLDM5W-SF-EH_zfgZZvIlo0(packages, suspended), false);
    }

    static /* synthetic */ void lambda$updateHiddenWhileSuspendedState$4(ArraySet packages, boolean suspended, WindowState w) {
        if (packages.contains(w.getOwningPackage())) {
            w.setHiddenWhileSuspended(suspended);
        }
    }

    void updateAppOpsState() {
        forAllWindows((Consumer) -$$Lambda$RootWindowContainer$0aCEx04eIvMHmZVtI4ucsiK5s9I.INSTANCE, false);
    }

    static /* synthetic */ boolean lambda$canShowStrictModeViolation$6(int pid, WindowState w) {
        return w.mSession.mPid == pid && w.isVisibleLw();
    }

    boolean canShowStrictModeViolation(int pid) {
        return getWindow(new -$$Lambda$RootWindowContainer$ZTXupc1zKRWZgWpo-r3so3blHoI(pid)) != null;
    }

    void closeSystemDialogs(String reason) {
        this.mCloseSystemDialogsReason = reason;
        forAllWindows(this.mCloseSystemDialogsConsumer, false);
    }

    void removeReplacedWindows() {
        this.mService.openSurfaceTransaction();
        try {
            forAllWindows(sRemoveReplacedWindowsConsumer, true);
        } finally {
            this.mService.closeSurfaceTransaction("removeReplacedWindows");
        }
    }

    boolean hasPendingLayoutChanges(WindowAnimator animator) {
        boolean hasChanges = false;
        int count = this.mChildren.size();
        for (int i = 0; i < count; i++) {
            int pendingChanges = animator.getPendingLayoutChanges(((DisplayContent) this.mChildren.get(i)).getDisplayId());
            if ((pendingChanges & 4) != 0) {
                animator.mBulkUpdateParams |= 16;
            }
            if (pendingChanges != 0) {
                hasChanges = true;
            }
        }
        return hasChanges;
    }

    boolean reclaimSomeSurfaceMemory(WindowStateAnimator winAnimator, String operation, boolean secure) {
        Throwable th;
        WindowStateAnimator windowStateAnimator = winAnimator;
        WindowSurfaceController surfaceController = windowStateAnimator.mSurfaceController;
        boolean leakedSurface = false;
        boolean killedApps = false;
        r0 = new Object[3];
        boolean z = false;
        r0[0] = windowStateAnimator.mWin.toString();
        r0[1] = Integer.valueOf(windowStateAnimator.mSession.mPid);
        r0[2] = operation;
        EventLog.writeEvent(EventLogTags.WM_NO_SURFACE_MEMORY, r0);
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            int displayNdx;
            Slog.i(TAG, "Out of memory for surface!  Looking for leaks...");
            int numDisplays = this.mChildren.size();
            for (displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                leakedSurface |= ((DisplayContent) this.mChildren.get(displayNdx)).destroyLeakedSurfaces();
            }
            if (!leakedSurface) {
                Slog.w(TAG, "No leaked surfaces; killing applications!");
                SparseIntArray pidCandidates = new SparseIntArray();
                displayNdx = 0;
                while (true) {
                    int displayNdx2 = displayNdx;
                    if (displayNdx2 >= numDisplays) {
                        break;
                    }
                    ((DisplayContent) this.mChildren.get(displayNdx2)).forAllWindows((Consumer) new -$$Lambda$RootWindowContainer$utugHDPHgMp2b3JwigOH_-Y0P1Q(this, pidCandidates), z);
                    if (pidCandidates.size() > 0) {
                        int[] pids = new int[pidCandidates.size()];
                        for (displayNdx = z; displayNdx < pids.length; displayNdx++) {
                            pids[displayNdx] = pidCandidates.keyAt(displayNdx);
                        }
                        try {
                            try {
                                if (this.mService.mActivityManager.killPids(pids, "Free memory", secure)) {
                                    killedApps = true;
                                }
                            } catch (RemoteException e) {
                            }
                        } catch (RemoteException e2) {
                            z = secure;
                        }
                    } else {
                        z = secure;
                    }
                    displayNdx = displayNdx2 + 1;
                    z = false;
                }
            }
            z = secure;
            if (leakedSurface || killedApps) {
                try {
                    Slog.w(TAG, "Looks like we have reclaimed some memory, clearing surface for retry.");
                    if (surfaceController != null) {
                        winAnimator.destroySurface();
                        if (!(windowStateAnimator.mWin.mAppToken == null || windowStateAnimator.mWin.mAppToken.getController() == null)) {
                            windowStateAnimator.mWin.mAppToken.getController().removeStartingWindow();
                        }
                    }
                    try {
                        windowStateAnimator.mWin.mClient.dispatchGetNewSurface();
                    } catch (RemoteException e3) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(callingIdentity);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(callingIdentity);
            return leakedSurface || killedApps;
        } catch (Throwable th3) {
            th = th3;
            z = secure;
            Binder.restoreCallingIdentity(callingIdentity);
            throw th;
        }
    }

    public static /* synthetic */ void lambda$reclaimSomeSurfaceMemory$7(RootWindowContainer rootWindowContainer, SparseIntArray pidCandidates, WindowState w) {
        if (!rootWindowContainer.mService.mForceRemoves.contains(w)) {
            WindowStateAnimator wsa = w.mWinAnimator;
            if (wsa.mSurfaceController != null) {
                pidCandidates.append(wsa.mSession.mPid, wsa.mSession.mPid);
            }
        }
    }

    void performSurfacePlacement(boolean recoveringMemory) {
        Throwable th;
        DisplayInfo displayInfo;
        int displayNdx;
        DisplayContent displayContent;
        DisplayContent displayContent2;
        int i;
        boolean updateInputWindowsNeeded = false;
        boolean z = false;
        if (this.mService.mFocusMayChange) {
            this.mService.mFocusMayChange = false;
            updateInputWindowsNeeded = this.mService.updateFocusedWindowLocked(3, false);
        }
        boolean updateInputWindowsNeeded2 = updateInputWindowsNeeded;
        int numDisplays = this.mChildren.size();
        for (int displayNdx2 = 0; displayNdx2 < numDisplays; displayNdx2++) {
            ((DisplayContent) this.mChildren.get(displayNdx2)).setExitingTokensHasVisible(false);
        }
        this.mHoldScreen = null;
        this.mScreenBrightness = -1.0f;
        this.mUserActivityTimeout = -1;
        this.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mSustainedPerformanceModeCurrent = false;
        WindowManagerService windowManagerService = this.mService;
        windowManagerService.mTransactionSequence++;
        DisplayContent defaultDisplay = this.mService.getDefaultDisplayContentLocked();
        DisplayInfo defaultInfo = defaultDisplay.getDisplayInfo();
        int defaultDw = defaultInfo.logicalWidth;
        int defaultDh = defaultInfo.logicalHeight;
        this.mService.openSurfaceTransaction();
        try {
            applySurfaceChangesTransaction(recoveringMemory, defaultDw, defaultDh);
        } catch (RuntimeException e) {
            RuntimeException runtimeException = e;
            Slog.wtf(TAG, "Unhandled exception in Window Manager", e);
        } catch (Throwable th2) {
            th = th2;
            displayInfo = defaultInfo;
        }
        this.mService.closeSurfaceTransaction("performLayoutAndPlaceSurfaces");
        this.mService.mAnimator.executeAfterPrepareSurfacesRunnables();
        WindowSurfacePlacer surfacePlacer = this.mService.mWindowPlacerLocked;
        if (this.mService.mAppTransition.isReady()) {
            StringBuilder stringBuilder = new StringBuilder();
            WindowManagerService windowManagerService2 = this.mService;
            stringBuilder.append(windowManagerService2.mAppTransitTrack);
            stringBuilder.append(" performsurface");
            windowManagerService2.mAppTransitTrack = stringBuilder.toString();
            defaultDisplay.pendingLayoutChanges |= surfacePlacer.handleAppTransitionReadyLocked();
        }
        if (!isAppAnimating() && this.mService.mAppTransition.isRunning()) {
            defaultDisplay.pendingLayoutChanges |= this.mService.handleAnimatingStoppedAndTransitionLocked();
        }
        RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
        if (recentsAnimationController != null) {
            recentsAnimationController.checkAnimationReady(this.mWallpaperController);
        }
        if (this.mWallpaperForceHidingChanged && defaultDisplay.pendingLayoutChanges == 0 && !this.mService.mAppTransition.isReady()) {
            defaultDisplay.pendingLayoutChanges |= 1;
        }
        this.mWallpaperForceHidingChanged = false;
        if (this.mWallpaperMayChange) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v(TAG, "Wallpaper may change!  Adjusting");
            }
            defaultDisplay.pendingLayoutChanges |= 4;
        }
        if (this.mService.mFocusMayChange) {
            this.mService.mFocusMayChange = false;
            if (this.mService.updateFocusedWindowLocked(2, false)) {
                updateInputWindowsNeeded2 = true;
                defaultDisplay.pendingLayoutChanges |= 8;
            }
        }
        if (isLayoutNeeded()) {
            defaultDisplay.pendingLayoutChanges |= 1;
        }
        ArraySet<DisplayContent> touchExcludeRegionUpdateDisplays = handleResizingWindows();
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION && this.mService.mDisplayFrozen) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("With display frozen, orientationChangeComplete=");
            stringBuilder2.append(this.mOrientationChangeComplete);
            Slog.v(str, stringBuilder2.toString());
        }
        if (this.mOrientationChangeComplete) {
            if (this.mService.mDisplayFrozen) {
                if (this.mLastWindowFreezeSource != null) {
                    Jlog.d(59, Jlog.extractAppName(this.mLastWindowFreezeSource.toString()), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                } else {
                    Jlog.d(59, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                if (this.mService.mIsPerfBoost) {
                    this.mService.mIsPerfBoost = false;
                    UniPerf.getInstance().uniPerfEvent(4105, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new int[]{-1});
                }
                LogPower.push(130, Integer.toString(this.mService.getDefaultDisplayRotation()));
            }
            if (this.mService.mWindowsFreezingScreen != 0) {
                this.mService.mWindowsFreezingScreen = 0;
                this.mService.mLastFinishedFreezeSource = this.mLastWindowFreezeSource;
                this.mService.mH.removeMessages(11);
            }
            if (this.mService.mDisplayFrozen) {
                Slog.i(TAG, "orientation change is complete, call stopFreezingDisplayLocked");
            }
            this.mService.stopFreezingDisplayLocked();
            if (!(this.mService.mDisplayFrozen || HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode())) {
                reLayoutIfNeed();
            }
        }
        updateInputWindowsNeeded = false;
        int i2 = this.mService.mDestroySurface.size();
        if (i2 > 0) {
            while (true) {
                i2--;
                WindowState win = (WindowState) this.mService.mDestroySurface.get(i2);
                win.mDestroying = z;
                if (this.mService.mInputMethodWindow == win) {
                    this.mService.setInputMethodWindowLocked(null);
                }
                if (win.getDisplayContent().mWallpaperController.isWallpaperTarget(win)) {
                    updateInputWindowsNeeded = true;
                }
                win.destroySurfaceUnchecked();
                win.mWinAnimator.destroyPreservedSurfaceLocked();
                if (i2 <= 0) {
                    break;
                }
                z = false;
            }
            this.mService.mDestroySurface.clear();
        }
        for (displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ((DisplayContent) this.mChildren.get(displayNdx)).removeExistingTokensIfPossible();
        }
        if (updateInputWindowsNeeded) {
            defaultDisplay.pendingLayoutChanges |= 4;
            defaultDisplay.setLayoutNeeded();
        }
        for (displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            displayContent = (DisplayContent) this.mChildren.get(displayNdx);
            if (displayContent.pendingLayoutChanges != 0) {
                displayContent.setLayoutNeeded();
            }
        }
        this.mService.mInputMonitor.updateInputWindowsLw(true);
        this.mService.setHoldScreenLocked(this.mHoldScreen);
        if (this.mService.mDisplayFrozen) {
        } else {
            if (this.mScreenBrightness < 0.0f) {
                this.mAppBrightnessPackageName = PackageManagerService.PLATFORM_PACKAGE_NAME;
                sendBrightnessToMonitor(-1.0f, this.mAppBrightnessPackageName);
            } else {
                sendBrightnessToMonitor(this.mScreenBrightness, this.mAppBrightnessPackageName);
            }
            this.mHandler.obtainMessage(1, this.mScreenBrightness < 0.0f ? -1 : toBrightnessOverride(this.mScreenBrightness), 0).sendToTarget();
            this.mHandler.obtainMessage(2, Long.valueOf(this.mUserActivityTimeout)).sendToTarget();
        }
        if (this.mSustainedPerformanceModeCurrent != this.mSustainedPerformanceModeEnabled) {
            this.mSustainedPerformanceModeEnabled = this.mSustainedPerformanceModeCurrent;
            this.mService.mPowerManagerInternal.powerHint(6, this.mSustainedPerformanceModeEnabled);
        }
        if (this.mUpdateRotation) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d(TAG, "Performing post-rotate rotation");
            }
            displayNdx = defaultDisplay.getDisplayId();
            if (defaultDisplay.updateRotationUnchecked()) {
                this.mService.mH.obtainMessage(18, Integer.valueOf(displayNdx)).sendToTarget();
            } else {
                this.mUpdateRotation = false;
            }
            displayContent = this.mService.mVr2dDisplayId != -1 ? getDisplayContent(this.mService.mVr2dDisplayId) : null;
            if (displayContent != null && displayContent.updateRotationUnchecked()) {
                this.mService.mH.obtainMessage(18, Integer.valueOf(this.mService.mVr2dDisplayId)).sendToTarget();
            }
        }
        if (!(this.mService.mWaitingForDrawnCallback == null && (!this.mOrientationChangeComplete || defaultDisplay.isLayoutNeeded() || this.mUpdateRotation))) {
            this.mService.checkDrawnWindowsLocked();
        }
        displayNdx = this.mService.mPendingRemove.size();
        if (displayNdx > 0) {
            if (this.mService.mPendingRemoveTmp.length < displayNdx) {
                this.mService.mPendingRemoveTmp = new WindowState[(displayNdx + 10)];
            }
            this.mService.mPendingRemove.toArray(this.mService.mPendingRemoveTmp);
            this.mService.mPendingRemove.clear();
            ArrayList<DisplayContent> displayList = new ArrayList();
            for (i2 = 0; i2 < displayNdx; i2++) {
                WindowState w = this.mService.mPendingRemoveTmp[i2];
                w.removeImmediately();
                displayContent2 = w.getDisplayContent();
                if (!(displayContent2 == null || displayList.contains(displayContent2))) {
                    displayList.add(displayContent2);
                }
            }
            i = 1;
            for (int j = displayList.size() - 1; j >= 0; j--) {
                ((DisplayContent) displayList.get(j)).assignWindowLayers(true);
            }
        } else {
            i = 1;
        }
        for (int displayNdx3 = this.mChildren.size() - i; displayNdx3 >= 0; displayNdx3--) {
            ((DisplayContent) this.mChildren.get(displayNdx3)).checkCompleteDeferredRemoval();
        }
        if (updateInputWindowsNeeded2) {
            this.mService.mInputMonitor.updateInputWindowsLw(false);
        }
        this.mService.setFocusTaskRegionLocked(null);
        if (touchExcludeRegionUpdateDisplays != null) {
            displayContent = this.mService.mFocusedApp != null ? this.mService.mFocusedApp.getDisplayContent() : null;
            Iterator it = touchExcludeRegionUpdateDisplays.iterator();
            while (it.hasNext()) {
                displayContent2 = (DisplayContent) it.next();
                if (displayContent != displayContent2) {
                    displayContent2.setTouchExcludeRegion(null);
                }
            }
        }
        this.mService.enableScreenIfNeededLocked();
        this.mService.scheduleAnimationLocked();
        return;
        this.mService.closeSurfaceTransaction("performLayoutAndPlaceSurfaces");
        throw th;
    }

    private void applySurfaceChangesTransaction(boolean recoveringMemory, int defaultDw, int defaultDh) {
        this.mHoldScreenWindow = null;
        this.mObscuringWindow = null;
        if (this.mService.mWatermark != null) {
            this.mService.mWatermark.positionSurface(defaultDw, defaultDh);
        }
        if (this.mService.mStrictModeFlash != null) {
            this.mService.mStrictModeFlash.positionSurface(defaultDw, defaultDh);
        }
        if (this.mService.mCircularDisplayMask != null) {
            this.mService.mCircularDisplayMask.positionSurface(defaultDw, defaultDh, this.mService.getDefaultDisplayRotation());
        }
        if (this.mService.mEmulatorDisplayOverlay != null) {
            this.mService.mEmulatorDisplayOverlay.positionSurface(defaultDw, defaultDh, this.mService.getDefaultDisplayRotation());
        }
        boolean focusDisplayed = false;
        for (int j = 0; j < this.mChildren.size(); j++) {
            focusDisplayed |= ((DisplayContent) this.mChildren.get(j)).applySurfaceChangesTransaction(recoveringMemory);
        }
        if (focusDisplayed) {
            this.mService.mH.sendEmptyMessage(3);
        }
        this.mService.mDisplayManagerInternal.performTraversal(this.mDisplayTransaction);
        SurfaceControl.mergeToGlobalTransaction(this.mDisplayTransaction);
    }

    private ArraySet<DisplayContent> handleResizingWindows() {
        ArraySet<DisplayContent> touchExcludeRegionUpdateSet = null;
        for (int i = this.mService.mResizingWindows.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) this.mService.mResizingWindows.get(i);
            if (!win.mAppFreezing) {
                win.reportResized();
                this.mService.mResizingWindows.remove(i);
                if (WindowManagerService.excludeWindowTypeFromTapOutTask(win.mAttrs.type)) {
                    DisplayContent dc = win.getDisplayContent();
                    if (touchExcludeRegionUpdateSet == null) {
                        touchExcludeRegionUpdateSet = new ArraySet();
                    }
                    touchExcludeRegionUpdateSet.add(dc);
                }
            }
        }
        return touchExcludeRegionUpdateSet;
    }

    boolean handleNotObscuredLocked(WindowState w, boolean obscured, boolean syswin) {
        LayoutParams attrs = w.mAttrs;
        int attrFlags = attrs.flags;
        boolean onScreen = w.isOnScreen();
        boolean canBeSeen = w.isDisplayedLw();
        int privateflags = attrs.privateFlags;
        boolean displayHasContent = false;
        if (w.mHasSurface && onScreen && !syswin && w.mAttrs.userActivityTimeout >= 0 && this.mUserActivityTimeout < 0) {
            if ((w.mAttrs.privateFlags & 1024) == 0 || !this.mService.mDestroySurface.contains(w)) {
                this.mUserActivityTimeout = w.mAttrs.userActivityTimeout;
            } else {
                Slog.e(TAG, "do not set userActivityTimeout this time");
            }
        }
        if (w.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && !canBeSeen) {
            boolean z = w.mHasSurface && this.mService.mPolicy.isKeyguardShowingOrOccluded();
            canBeSeen = z;
            if (canBeSeen) {
                Slog.w(TAG, "reset canBeSeen for statusbar when keyguard on");
            }
        }
        if (w.mHasSurface && canBeSeen) {
            if ((attrFlags & 128) != 0 && isWindowVisibleInKeyguard(attrs)) {
                this.mHoldScreen = w.mSession;
                this.mHoldScreenWindow = w;
            }
            if (!syswin && w.mAttrs.screenBrightness >= 0.0f && this.mScreenBrightness < 0.0f && w.isVisibleLw()) {
                this.mScreenBrightness = w.mAttrs.screenBrightness;
                this.mAppBrightnessPackageName = w.mAttrs.packageName;
            }
            int type = attrs.type;
            DisplayContent displayContent = w.getDisplayContent();
            if (displayContent != null && displayContent.isDefaultDisplay) {
                if (type == 2023 || (attrs.privateFlags & 1024) != 0) {
                    this.mObscureApplicationContentOnSecondaryDisplays = true;
                }
                displayHasContent = true;
            } else if (displayContent != null && (!this.mObscureApplicationContentOnSecondaryDisplays || (obscured && type == 2009))) {
                displayHasContent = true;
            }
            if ((262144 & privateflags) != 0) {
                this.mSustainedPerformanceModeCurrent = true;
            }
        }
        return displayHasContent;
    }

    boolean copyAnimToLayoutParams() {
        boolean doRequest = false;
        int bulkUpdateParams = this.mService.mAnimator.mBulkUpdateParams;
        if ((bulkUpdateParams & 1) != 0) {
            this.mUpdateRotation = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 2) != 0) {
            this.mWallpaperMayChange = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 4) != 0) {
            this.mWallpaperForceHidingChanged = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 8) == 0) {
            if (this.mService.mDisplayFrozen) {
                Flog.i(308, "Orientation change is not complete");
            }
            this.mOrientationChangeComplete = false;
        } else {
            if (this.mService.mDisplayFrozen) {
                Flog.i(308, "Orientation change is complete");
            }
            this.mOrientationChangeComplete = true;
            this.mLastWindowFreezeSource = this.mService.mAnimator.mLastWindowFreezeSource;
            if (this.mService.mWindowsFreezingScreen != 0) {
                doRequest = true;
            }
        }
        if ((bulkUpdateParams & 16) != 0) {
            this.mWallpaperActionPending = true;
        }
        return doRequest;
    }

    private static int toBrightnessOverride(float value) {
        return (int) (255.0f * value);
    }

    void dumpDisplayContents(PrintWriter pw) {
        pw.println("WINDOW MANAGER DISPLAY CONTENTS (dumpsys window displays)");
        if (this.mService.mDisplayReady) {
            int count = this.mChildren.size();
            for (int i = 0; i < count; i++) {
                ((DisplayContent) this.mChildren.get(i)).dump(pw, "  ", true);
            }
            return;
        }
        pw.println("  NO DISPLAY");
    }

    void dumpLayoutNeededDisplayIds(PrintWriter pw) {
        if (isLayoutNeeded()) {
            pw.print("  mLayoutNeeded on displays=");
            int count = this.mChildren.size();
            for (int displayNdx = 0; displayNdx < count; displayNdx++) {
                DisplayContent displayContent = (DisplayContent) this.mChildren.get(displayNdx);
                if (displayContent.isLayoutNeeded()) {
                    pw.print(displayContent.getDisplayId());
                }
            }
            pw.println();
        }
    }

    void dumpWindowsNoHeader(PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        forAllWindows((Consumer) new -$$Lambda$RootWindowContainer$lQbVdBqi1IIiuRy86WremqX682s(windows, pw, new int[1], dumpAll), true);
    }

    static /* synthetic */ void lambda$dumpWindowsNoHeader$8(ArrayList windows, PrintWriter pw, int[] index, boolean dumpAll, WindowState w) {
        if (windows == null || windows.contains(w)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  Window #");
            stringBuilder.append(index[0]);
            stringBuilder.append(" ");
            stringBuilder.append(w);
            stringBuilder.append(":");
            pw.println(stringBuilder.toString());
            String str = "    ";
            boolean z = dumpAll || windows != null;
            w.dump(pw, str, z);
            index[0] = index[0] + 1;
        }
    }

    void dumpTokens(PrintWriter pw, boolean dumpAll) {
        pw.println("  All tokens:");
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((DisplayContent) this.mChildren.get(i)).dumpTokens(pw, dumpAll);
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, trim);
        if (this.mService.mDisplayReady) {
            int count = this.mChildren.size();
            for (int i = 0; i < count; i++) {
                ((DisplayContent) this.mChildren.get(i)).writeToProto(proto, 2246267895810L, trim);
            }
        }
        if (!trim) {
            forAllWindows((Consumer) new -$$Lambda$RootWindowContainer$WK0a_BR42j4A-e0Xx1wj4BL8rUk(proto), true);
        }
        proto.end(token);
    }

    String getName() {
        return "ROOT";
    }

    void scheduleAnimation() {
        this.mService.scheduleAnimationLocked();
    }

    private boolean isWindowVisibleInKeyguard(LayoutParams attrs) {
        boolean z = true;
        if (!this.mService.mPolicy.isKeyguardShowingOrOccluded() || (attrs.flags & DumpState.DUMP_FROZEN) != 0) {
            return true;
        }
        if (attrs.type == 1) {
            z = false;
        }
        return z;
    }

    private void reLayoutIfNeed() {
        int i = this.mService.mDeferRelayoutWindow.size();
        if (i > 0) {
            do {
                i--;
                WindowState win = (WindowState) this.mService.mDeferRelayoutWindow.get(i);
                if (win != null && win.isVisible()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("reLayoutIfNeed win:");
                    stringBuilder.append(win);
                    Slog.d(str, stringBuilder.toString());
                    win.mSeq++;
                    try {
                        win.mClient.dispatchSystemUiVisibilityChanged(win.mSeq, win.mSystemUiVisibility, 0, 0);
                        continue;
                    } catch (RemoteException e) {
                        continue;
                    }
                }
            } while (i > 0);
            this.mService.mDeferRelayoutWindow.clear();
        }
    }
}
