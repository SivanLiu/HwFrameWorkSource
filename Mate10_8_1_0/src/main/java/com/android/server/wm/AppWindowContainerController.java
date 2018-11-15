package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.IApplicationToken;
import android.view.WindowManagerPolicy.StartingSurface;
import com.android.internal.R;
import com.android.server.AttributeCache;
import com.android.server.AttributeCache.Entry;

public class AppWindowContainerController extends WindowContainerController<AppWindowToken, AppWindowContainerListener> {
    private static final int DELAY_FOR_REMOVING_STARTING_WINDOW = 30;
    private static final int STARTING_WINDOW_TYPE_NONE = 0;
    private static final int STARTING_WINDOW_TYPE_SNAPSHOT = 1;
    private static final int STARTING_WINDOW_TYPE_SPLASH_SCREEN = 2;
    private final Runnable mAddStartingWindow;
    private final Handler mHandler;
    private final Runnable mOnWindowsGone;
    private final Runnable mOnWindowsVisible;
    private Task mTask;
    private final IApplicationToken mToken;

    private final class H extends Handler {
        public static final int NOTIFY_STARTING_WINDOW_DRAWN = 2;
        public static final int NOTIFY_WINDOWS_DRAWN = 1;

        public H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (AppWindowContainerController.this.mListener != null) {
                        ((AppWindowContainerListener) AppWindowContainerController.this.mListener).onWindowsDrawn(msg.getWhen());
                        break;
                    }
                    return;
                case 2:
                    if (AppWindowContainerController.this.mListener != null) {
                        ((AppWindowContainerListener) AppWindowContainerController.this.mListener).onStartingWindowDrawn(msg.getWhen());
                        break;
                    }
                    return;
            }
        }
    }

    /* synthetic */ void lambda$-com_android_server_wm_AppWindowContainerController_4943() {
        if (this.mListener != null) {
            ((AppWindowContainerListener) this.mListener).onWindowsVisible();
        }
    }

    /* synthetic */ void lambda$-com_android_server_wm_AppWindowContainerController_5234() {
        if (this.mListener != null) {
            ((AppWindowContainerListener) this.mListener).onWindowsGone();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    /* synthetic */ void lambda$-com_android_server_wm_AppWindowContainerController_5523() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    StartingData startingData = ((AppWindowToken) this.mContainer).startingData;
                    AppWindowToken container = this.mContainer;
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (surface != null) {
            boolean z = false;
            synchronized (this.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (container.removed || container.startingData == null) {
                        container.startingWindow = null;
                        container.startingData = null;
                        z = true;
                    } else {
                        container.startingSurface = surface;
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            if (z) {
                surface.remove();
            }
        }
    }

    public AppWindowContainerController(TaskWindowContainerController taskController, IApplicationToken token, AppWindowContainerListener listener, int index, int requestedOrientation, boolean fullscreen, boolean showForAllUsers, int configChanges, boolean voiceInteraction, boolean launchTaskBehind, boolean alwaysFocusable, int targetSdkVersion, int rotationAnimationHint, long inputDispatchingTimeoutNanos, Configuration overrideConfig, Rect bounds, boolean naviBarHide, boolean hwNotchSupport, int hwGestureNavOptions) {
        this(taskController, token, listener, index, requestedOrientation, fullscreen, showForAllUsers, configChanges, voiceInteraction, launchTaskBehind, alwaysFocusable, targetSdkVersion, rotationAnimationHint, inputDispatchingTimeoutNanos, WindowManagerService.getInstance(), overrideConfig, bounds, naviBarHide, hwNotchSupport, hwGestureNavOptions);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AppWindowContainerController(TaskWindowContainerController taskController, IApplicationToken token, AppWindowContainerListener listener, int index, int requestedOrientation, boolean fullscreen, boolean showForAllUsers, int configChanges, boolean voiceInteraction, boolean launchTaskBehind, boolean alwaysFocusable, int targetSdkVersion, int rotationAnimationHint, long inputDispatchingTimeoutNanos, WindowManagerService service, Configuration overrideConfig, Rect bounds, boolean naviBarHide, boolean hwNotchSupport, int hwGestureNavOptions) {
        super(listener, service);
        this.mTask = null;
        this.mOnWindowsVisible = new -$Lambda$aEpJ2RCAIjecjyIIYTv6ricEwh4((byte) 2, this);
        this.mOnWindowsGone = new -$Lambda$aEpJ2RCAIjecjyIIYTv6ricEwh4((byte) 3, this);
        this.mAddStartingWindow = new -$Lambda$aEpJ2RCAIjecjyIIYTv6ricEwh4((byte) 4, this);
        this.mHandler = new H(service.mH.getLooper());
        this.mToken = token;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mRoot.getAppWindowToken(this.mToken.asBinder()) != null) {
                    Slog.w("WindowManager", "Attempted to add existing app token: " + this.mToken);
                } else {
                    Task task = (Task) taskController.mContainer;
                    if (task == null) {
                        throw new IllegalArgumentException("AppWindowContainerController: invalid  controller=" + taskController);
                    }
                    AppWindowToken atoken = createAppWindow(this.mService, token, voiceInteraction, task.getDisplayContent(), inputDispatchingTimeoutNanos, fullscreen, showForAllUsers, targetSdkVersion, requestedOrientation, rotationAnimationHint, configChanges, launchTaskBehind, alwaysFocusable, this, overrideConfig, bounds, naviBarHide, hwNotchSupport, hwGestureNavOptions);
                    Slog.v("WindowManager", "addAppToken: " + atoken + " controller=" + taskController + " at " + index);
                    task.addChild(atoken, index);
                    this.mTask = task;
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    AppWindowToken createAppWindow(WindowManagerService service, IApplicationToken token, boolean voiceInteraction, DisplayContent dc, long inputDispatchingTimeoutNanos, boolean fullscreen, boolean showForAllUsers, int targetSdk, int orientation, int rotationAnimationHint, int configChanges, boolean launchTaskBehind, boolean alwaysFocusable, AppWindowContainerController controller, Configuration overrideConfig, Rect bounds, boolean naviBarHide, boolean hwNotchSupport, int hwGestureNavOptions) {
        return new AppWindowToken(service, token, voiceInteraction, dc, inputDispatchingTimeoutNanos, fullscreen, showForAllUsers, targetSdk, orientation, rotationAnimationHint, configChanges, launchTaskBehind, alwaysFocusable, controller, overrideConfig, bounds, naviBarHide, hwNotchSupport, hwGestureNavOptions);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeContainer(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    Slog.w("WindowManager", "removeAppToken: Attempted to remove binder token: " + this.mToken + " from non-existing displayId=" + displayId);
                } else {
                    dc.removeAppToken(this.mToken.asBinder());
                    super.removeContainer();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void removeContainer() {
        throw new UnsupportedOperationException("Use removeContainer(displayId) instead.");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reparent(TaskWindowContainerController taskController, int position) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Slog.i("WindowManager", "reparent: moving app token=" + this.mToken + " to task=" + taskController + " at " + position);
                if (this.mContainer == null) {
                    Slog.i("WindowManager", "reparent: could not find app token=" + this.mToken);
                } else {
                    Task task = taskController.mContainer;
                    if (task == null) {
                        throw new IllegalArgumentException("reparent: could not find task=" + taskController);
                    }
                    ((AppWindowToken) this.mContainer).reparent(task, position);
                    ((AppWindowToken) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Configuration setOrientation(int requestedOrientation, int displayId, Configuration displayConfig, boolean freezeScreenIfNeeded) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to set orientation of non-existing app token: " + this.mToken);
                } else {
                    ((AppWindowToken) this.mContainer).setOrientation(requestedOrientation);
                    Configuration updateOrientationFromAppTokens = this.mService.updateOrientationFromAppTokens(displayConfig, freezeScreenIfNeeded ? this.mToken.asBinder() : null, displayId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return updateOrientationFromAppTokens;
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getOrientation() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    int orientationIgnoreVisibility = ((AppWindowToken) this.mContainer).getOrientationIgnoreVisibility();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return orientationIgnoreVisibility;
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onOverrideConfigurationChanged(Configuration overrideConfiguration, Rect bounds) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    ((AppWindowToken) this.mContainer).onOverrideConfigurationChanged(overrideConfiguration, bounds);
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setDisablePreviewScreenshots(boolean disable) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to set disable screenshots of non-existing app token: " + this.mToken);
                } else {
                    ((AppWindowToken) this.mContainer).setDisablePreviewScreenshots(disable);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setVisibility(boolean visible, boolean deferHidingClient) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to set visibility of non-existing app token: " + this.mToken);
                } else {
                    AppWindowToken wtoken = this.mContainer;
                    if (visible || !wtoken.hiddenRequested) {
                        this.mService.mOpeningApps.remove(wtoken);
                        this.mService.mClosingApps.remove(wtoken);
                        wtoken.waitingToShow = false;
                        wtoken.hiddenRequested = visible ^ 1;
                        wtoken.mDeferHidingClient = deferHidingClient;
                        if (visible) {
                            if (!this.mService.mAppTransition.isTransitionSet() && this.mService.mAppTransition.isReady()) {
                                this.mService.mOpeningApps.add(wtoken);
                            }
                            wtoken.startingMoved = false;
                            if (wtoken.hidden || wtoken.mAppStopped) {
                                wtoken.clearAllDrawn();
                                if (wtoken.hidden) {
                                    wtoken.waitingToShow = true;
                                }
                                if (wtoken.isClientHidden()) {
                                    wtoken.setClientHidden(false);
                                }
                            }
                            wtoken.requestUpdateWallpaperIfNeeded();
                            Slog.v("WindowManager", "No longer Stopped: " + wtoken);
                            wtoken.mAppStopped = false;
                        } else {
                            wtoken.removeDeadWindows();
                            wtoken.setVisibleBeforeClientHidden();
                        }
                        if (wtoken.okToAnimate() && this.mService.mAppTransition.isTransitionSet()) {
                            if (wtoken.mAppAnimator.usingTransferredAnimation && wtoken.mAppAnimator.animation == null) {
                                Slog.wtf("WindowManager", "Will NOT set dummy animation on: " + wtoken + ", using null transferred animation!");
                            }
                            if (!wtoken.mAppAnimator.usingTransferredAnimation && (!wtoken.startingDisplayed || this.mService.mSkipAppTransitionAnimation)) {
                                wtoken.mAppAnimator.setDummyAnimation();
                            }
                            wtoken.inPendingTransaction = true;
                            if (visible) {
                                this.mService.mOpeningApps.add(wtoken);
                                wtoken.mEnteringAnimation = true;
                            } else {
                                this.mService.mClosingApps.add(wtoken);
                                wtoken.mEnteringAnimation = false;
                            }
                            if (this.mService.mAppTransition.getAppTransition() == 16) {
                                WindowState win = this.mService.getDefaultDisplayContentLocked().findFocusedWindow();
                                if (win != null) {
                                    AppWindowToken focusedToken = win.mAppToken;
                                    if (focusedToken != null) {
                                        focusedToken.hidden = true;
                                        this.mService.mOpeningApps.add(focusedToken);
                                    }
                                }
                            }
                        } else {
                            wtoken.setVisibility(null, visible, -1, true, wtoken.mVoiceInteraction);
                            wtoken.updateReportedVisibilityLocked();
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    } else if (!deferHidingClient && wtoken.mDeferHidingClient) {
                        wtoken.mDeferHidingClient = deferHidingClient;
                        wtoken.setClientHidden(true);
                    }
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyUnknownVisibilityLaunched() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    this.mService.mUnknownAppVisibilityController.notifyLaunched((AppWindowToken) this.mContainer);
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean addStartingWindow(String pkg, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, IBinder transferFrom, boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean fromRecents) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to set icon of non-existing app token: " + this.mToken);
                } else if (!((AppWindowToken) this.mContainer).okToDisplay()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (((AppWindowToken) this.mContainer).startingData != null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    WindowState mainWin = ((AppWindowToken) this.mContainer).findMainWindow();
                    if (mainWin == null || !mainWin.mWinAnimator.getShown()) {
                        TaskSnapshot snapshot = this.mService.mTaskSnapshotController.getSnapshot(((AppWindowToken) this.mContainer).getTask().mTaskId, ((AppWindowToken) this.mContainer).getTask().mUserId, false, false);
                        int type = getStartingWindowType(newTask, taskSwitch, processRunning, allowTaskSnapshot, activityCreated, fromRecents, snapshot);
                        if ("com.android.settings".equals(pkg) && (this.mService.isSplitMode() ^ 1) != 0 && this.mService.mAppTransition.getAppTransition() == 6) {
                            type = 2;
                        }
                        if (type == 1) {
                            int i;
                            if ("com.android.contacts".equals(pkg) || "com.huawei.camera".equals(pkg) || "com.android.incallui".equals(pkg)) {
                                i = 1;
                            } else {
                                i = "com.android.gallery3d".equals(pkg);
                            }
                            if ((i ^ 1) != 0 && this.mTask.isSamePackageInTask()) {
                                boolean createSnapshot = createSnapshot(snapshot);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return createSnapshot;
                            }
                        }
                        if (theme != 0) {
                            Entry ent = AttributeCache.instance().get(pkg, theme, R.styleable.Window, this.mService.mCurrentUserId);
                            if (ent == null) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return false;
                            }
                            boolean windowIsTranslucent = ent.array.getBoolean(5, false);
                            boolean windowIsFloating = ent.array.getBoolean(4, false);
                            boolean windowShowWallpaper = ent.array.getBoolean(14, false);
                            boolean windowDisableStarting = ent.array.getBoolean(12, false);
                            if ("com.huawei.android.launcher".equals(pkg)) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return false;
                            } else if (windowIsTranslucent) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return false;
                            } else if (windowIsFloating || windowDisableStarting) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return false;
                            } else if (windowShowWallpaper) {
                                if (((AppWindowToken) this.mContainer).getDisplayContent().mWallpaperController.getWallpaperTarget() == null) {
                                    windowFlags |= DumpState.DUMP_DEXOPT;
                                } else {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return false;
                                }
                            }
                        }
                        if (((AppWindowToken) this.mContainer).transferStartingWindow(transferFrom)) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return true;
                        } else if (type != 2) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        } else {
                            ((AppWindowToken) this.mContainer).startingData = new SplashScreenStartingData(this.mService, pkg, theme, compatInfo, nonLocalizedLabel, labelRes, icon, logo, windowFlags, ((AppWindowToken) this.mContainer).getMergedOverrideConfiguration());
                            scheduleAddStartingWindow();
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return true;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private boolean isContainedOnlyOneVisibleWindow() {
        WindowList<WindowState> child = ((AppWindowToken) this.mContainer).mChildren;
        for (int i = child.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) child.get(i);
            if ((win.mAttrs.flags & 2) != 0 || (win.mAttrs.flags & 4) != 0) {
                return false;
            }
        }
        return true;
    }

    private int getStartingWindowType(boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean fromRecents, TaskSnapshot snapshot) {
        if (this.mService.mAppTransition.getAppTransition() == 19) {
            return 0;
        }
        if (newTask || (processRunning ^ 1) != 0 || (taskSwitch && (activityCreated ^ 1) != 0)) {
            return 2;
        }
        if (!taskSwitch || !allowTaskSnapshot) {
            return 0;
        }
        int nType = snapshot == null ? 0 : (snapshotOrientationSameAsTask(snapshot) || fromRecents) ? 1 : 2;
        if (1 == nType && (isContainedOnlyOneVisibleWindow() ^ 1) != 0) {
            Slog.d("WindowManager", "Skip adding snapshot startingWindow for activity with more than one window, " + ((AppWindowToken) this.mContainer).toString());
            nType = 0;
        }
        return nType;
    }

    void scheduleAddStartingWindow() {
        if (HwPCUtils.isPcCastModeInServer() && (this.mContainer instanceof AppWindowToken)) {
            TaskStack ts = ((AppWindowToken) this.mContainer).getTask().mStack;
            if (ts != null && HwPCUtils.isPcDynamicStack(ts.mStackId)) {
                return;
            }
        }
        this.mService.mAnimationHandler.postAtFrontOfQueue(this.mAddStartingWindow);
    }

    private boolean createSnapshot(TaskSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        ((AppWindowToken) this.mContainer).startingData = new SnapshotStartingData(this.mService, snapshot);
        scheduleAddStartingWindow();
        return true;
    }

    private boolean snapshotOrientationSameAsTask(TaskSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        return ((AppWindowToken) this.mContainer).getTask().getConfiguration().orientation == snapshot.getOrientation();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeStartingWindow() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (((AppWindowToken) this.mContainer).startingWindow == null) {
                    if (((AppWindowToken) this.mContainer).startingData != null) {
                        ((AppWindowToken) this.mContainer).startingData = null;
                    }
                } else if (((AppWindowToken) this.mContainer).startingData != null) {
                    StartingSurface surface = ((AppWindowToken) this.mContainer).startingSurface;
                    ((AppWindowToken) this.mContainer).startingData = null;
                    ((AppWindowToken) this.mContainer).startingSurface = null;
                    ((AppWindowToken) this.mContainer).startingWindow = null;
                    ((AppWindowToken) this.mContainer).startingDisplayed = false;
                    if (surface == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mService.mAnimationHandler.postDelayed(new -$Lambda$aEpJ2RCAIjecjyIIYTv6ricEwh4((byte) 5, surface), 30);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    static /* synthetic */ void lambda$-com_android_server_wm_AppWindowContainerController_37095(StartingSurface surface) {
        try {
            surface.remove();
        } catch (Exception e) {
            Slog.w("WindowManager", "Exception when removing starting window", e);
        }
    }

    public void pauseKeyDispatching() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    this.mService.mInputMonitor.pauseDispatchingLw((WindowToken) this.mContainer);
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void resumeKeyDispatching() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    this.mService.mInputMonitor.resumeDispatchingLw((WindowToken) this.mContainer);
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyAppResumed(boolean wasStopped) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to notify resumed of non-existing app token: " + this.mToken);
                } else {
                    ((AppWindowToken) this.mContainer).notifyAppResumed(wasStopped);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyAppStopped() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to notify stopped of non-existing app token: " + this.mToken);
                } else {
                    ((AppWindowToken) this.mContainer).notifyAppStopped();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startFreezingScreen(int configChanges) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    Slog.w("WindowManager", "Attempted to freeze screen with non-existing app token: " + this.mContainer);
                } else {
                    if (configChanges == 0) {
                        if (((AppWindowToken) this.mContainer).okToDisplay()) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                    }
                    ((AppWindowToken) this.mContainer).startFreezingScreen();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void stopFreezingScreen(boolean force) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    ((AppWindowToken) this.mContainer).stopFreezingScreen(true, force);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public Bitmap screenshotApplications(int displayId, int width, int height, float frameScale) {
        try {
            Trace.traceBegin(32, "screenshotApplications");
            synchronized (this.mWindowMap) {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContentOrCreate(displayId);
                if (dc == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    Trace.traceEnd(32);
                    return null;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                Bitmap screenshotApplications = dc.screenshotApplications(this.mToken.asBinder(), width, height, false, frameScale, Config.RGB_565, false, false);
                Trace.traceEnd(32);
                return screenshotApplications;
            }
        } catch (Throwable th) {
            Trace.traceEnd(32);
        }
    }

    void reportStartingWindowDrawn() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
    }

    void reportWindowsDrawn() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
    }

    void reportWindowsVisible() {
        this.mHandler.post(this.mOnWindowsVisible);
    }

    void reportWindowsGone() {
        this.mHandler.post(this.mOnWindowsGone);
    }

    boolean keyDispatchingTimedOut(String reason, int windowPid) {
        return this.mListener != null ? ((AppWindowContainerListener) this.mListener).keyDispatchingTimedOut(reason, windowPid) : false;
    }

    public String toString() {
        return "AppWindowContainerController{ token=" + this.mToken + " mContainer=" + this.mContainer + " mListener=" + this.mListener + "}";
    }
}
