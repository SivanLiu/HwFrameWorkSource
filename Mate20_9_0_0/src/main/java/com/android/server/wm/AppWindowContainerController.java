package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.content.pm.ActivityInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.IApplicationToken;
import android.view.RemoteAnimationDefinition;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.AttributeCache;
import com.android.server.AttributeCache.Entry;
import com.android.server.HwServiceFactory;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy.StartingSurface;
import java.util.HashSet;
import java.util.Set;

public class AppWindowContainerController extends WindowContainerController<AppWindowToken, AppWindowContainerListener> {
    private static final int STARTING_WINDOW_TYPE_NONE = 0;
    private static final int STARTING_WINDOW_TYPE_SNAPSHOT = 1;
    private static final int STARTING_WINDOW_TYPE_SPLASH_SCREEN = 2;
    private static Set<String> sSkipStartingWindowActivitys = new HashSet();
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
            StringBuilder stringBuilder;
            switch (msg.what) {
                case 1:
                    if (AppWindowContainerController.this.mListener != null) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Reporting drawn in ");
                            stringBuilder.append(AppWindowContainerController.this.mToken);
                            Slog.v("WindowManager", stringBuilder.toString());
                        }
                        ((AppWindowContainerListener) AppWindowContainerController.this.mListener).onWindowsDrawn(msg.getWhen());
                        break;
                    }
                    return;
                case 2:
                    if (AppWindowContainerController.this.mListener != null) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Reporting drawn in ");
                            stringBuilder.append(AppWindowContainerController.this.mToken);
                            Slog.v("WindowManager", stringBuilder.toString());
                        }
                        ((AppWindowContainerListener) AppWindowContainerController.this.mListener).onStartingWindowDrawn(msg.getWhen());
                        break;
                    }
                    return;
            }
        }
    }

    static {
        sSkipStartingWindowActivitys.add("com.tencent.mm/.plugin.voip.ui.VideoActivity");
        sSkipStartingWindowActivitys.add("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteFull");
        sSkipStartingWindowActivitys.add("com.tencent.tim/com.tencent.av.ui.VideoInviteFull");
        sSkipStartingWindowActivitys.add("com.tencent.mobileqq/com.tencent.av.ui.VChatActivity");
    }

    public static /* synthetic */ void lambda$new$0(AppWindowContainerController appWindowContainerController) {
        if (appWindowContainerController.mListener != null) {
            ((AppWindowContainerListener) appWindowContainerController.mListener).onWindowsVisible();
        }
    }

    public static /* synthetic */ void lambda$new$1(AppWindowContainerController appWindowContainerController) {
        if (appWindowContainerController.mListener != null) {
            ((AppWindowContainerListener) appWindowContainerController.mListener).onWindowsGone();
        }
    }

    public AppWindowContainerController(TaskWindowContainerController taskController, IApplicationToken token, AppWindowContainerListener listener, int index, int requestedOrientation, boolean fullscreen, boolean showForAllUsers, int configChanges, boolean voiceInteraction, boolean launchTaskBehind, boolean alwaysFocusable, int targetSdkVersion, int rotationAnimationHint, long inputDispatchingTimeoutNanos, boolean naviBarHide, ActivityInfo info) {
        this(taskController, token, listener, index, requestedOrientation, fullscreen, showForAllUsers, configChanges, voiceInteraction, launchTaskBehind, alwaysFocusable, targetSdkVersion, rotationAnimationHint, inputDispatchingTimeoutNanos, WindowManagerService.getInstance(), naviBarHide, info);
    }

    public AppWindowContainerController(TaskWindowContainerController taskController, IApplicationToken token, AppWindowContainerListener listener, int index, int requestedOrientation, boolean fullscreen, boolean showForAllUsers, int configChanges, boolean voiceInteraction, boolean launchTaskBehind, boolean alwaysFocusable, int targetSdkVersion, int rotationAnimationHint, long inputDispatchingTimeoutNanos, WindowManagerService service, boolean naviBarHide, ActivityInfo info) {
        Throwable atoken;
        TaskWindowContainerController taskWindowContainerController = taskController;
        int i = index;
        WindowManagerService windowManagerService = service;
        super(listener, windowManagerService);
        this.mTask = null;
        this.mOnWindowsVisible = new -$$Lambda$AppWindowContainerController$BD6wMjkwgPM5dckzkeLRiPrmx9Y(this);
        this.mOnWindowsGone = new -$$Lambda$AppWindowContainerController$mZqlV7Ety8-HHzaQXVEl4hu-8mc(this);
        this.mAddStartingWindow = new Runnable() {
            /* JADX WARNING: Removed duplicated region for block: B:31:0x0064  */
            /* JADX WARNING: Missing block: B:10:0x002b, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
            /* JADX WARNING: Missing block: B:11:0x002e, code:
            if (r1 != null) goto L_0x0031;
     */
            /* JADX WARNING: Missing block: B:12:0x0030, code:
            return;
     */
            /* JADX WARNING: Missing block: B:13:0x0031, code:
            r3 = null;
     */
            /* JADX WARNING: Missing block: B:16:0x0037, code:
            r3 = r1.createStartingSurface(r2);
     */
            /* JADX WARNING: Missing block: B:17:0x0039, code:
            r4 = move-exception;
     */
            /* JADX WARNING: Missing block: B:18:0x003a, code:
            android.util.Slog.w("WindowManager", "Exception when adding starting window", r4);
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                AppWindowToken container;
                synchronized (AppWindowContainerController.this.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (AppWindowContainerController.this.mContainer == null) {
                        } else {
                            AppWindowContainerController.this.mService.mAnimationHandler.removeCallbacks(this);
                            StartingData startingData = ((AppWindowToken) AppWindowContainerController.this.mContainer).startingData;
                            container = AppWindowContainerController.this.mContainer;
                        }
                    } finally {
                        while (true) {
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
                boolean abort;
                if (surface != null) {
                    abort = false;
                    synchronized (AppWindowContainerController.this.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (container.removed || container.startingData == null) {
                                container.startingWindow = null;
                                container.startingData = null;
                                abort = true;
                            } else {
                                container.startingSurface = surface;
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    if (abort) {
                        surface.remove();
                    }
                }
                if (abort) {
                }
            }
        };
        this.mHandler = new H(windowManagerService.mH.getLooper());
        IApplicationToken iApplicationToken = token;
        this.mToken = iApplicationToken;
        WindowHashMap windowHashMap = this.mWindowMap;
        synchronized (windowHashMap) {
            WindowHashMap windowHashMap2;
            int i2;
            TaskWindowContainerController taskWindowContainerController2;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                AppWindowToken atoken2 = this.mRoot.getAppWindowToken(this.mToken.asBinder());
                if (atoken2 != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to add existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    WindowContainer task = (Task) taskWindowContainerController.mContainer;
                    StringBuilder stringBuilder2;
                    if (task != null) {
                        AppWindowToken atoken3;
                        String str;
                        WindowContainer atoken4 = task;
                        windowHashMap2 = windowHashMap;
                        try {
                            atoken3 = createAppWindow(this.mService, iApplicationToken, voiceInteraction, task.getDisplayContent(), inputDispatchingTimeoutNanos, fullscreen, showForAllUsers, targetSdkVersion, requestedOrientation, rotationAnimationHint, configChanges, launchTaskBehind, alwaysFocusable, this, naviBarHide, info);
                            str = "WindowManager";
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("addAppToken: ");
                            stringBuilder2.append(atoken3);
                            stringBuilder2.append(" controller=");
                            try {
                                stringBuilder2.append(taskController);
                                stringBuilder2.append(" at ");
                                i2 = index;
                            } catch (Throwable th) {
                                atoken = th;
                                i2 = index;
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw atoken;
                            }
                        } catch (Throwable th2) {
                            atoken = th2;
                            taskWindowContainerController2 = taskController;
                            i2 = index;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw atoken;
                        }
                        try {
                            stringBuilder2.append(i2);
                            Slog.v(str, stringBuilder2.toString());
                            atoken4.addChild(atoken3, i2);
                            this.mTask = atoken4;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } catch (Throwable th3) {
                            atoken = th3;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw atoken;
                        }
                    }
                    AppWindowToken appWindowToken = atoken2;
                    windowHashMap2 = windowHashMap;
                    i2 = i;
                    taskWindowContainerController2 = taskWindowContainerController;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("AppWindowContainerController: invalid  controller=");
                    stringBuilder2.append(taskWindowContainerController2);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            } catch (Throwable th4) {
                atoken = th4;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw atoken;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    @VisibleForTesting
    AppWindowToken createAppWindow(WindowManagerService service, IApplicationToken token, boolean voiceInteraction, DisplayContent dc, long inputDispatchingTimeoutNanos, boolean fullscreen, boolean showForAllUsers, int targetSdk, int orientation, int rotationAnimationHint, int configChanges, boolean launchTaskBehind, boolean alwaysFocusable, AppWindowContainerController controller, boolean naviBarHide, ActivityInfo info) {
        return new AppWindowToken(service, token, voiceInteraction, dc, inputDispatchingTimeoutNanos, fullscreen, showForAllUsers, targetSdk, orientation, rotationAnimationHint, configChanges, launchTaskBehind, alwaysFocusable, controller, naviBarHide, info);
    }

    public void removeContainer(int displayId) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeAppToken: Attempted to remove binder token: ");
                    stringBuilder.append(this.mToken);
                    stringBuilder.append(" from non-existing displayId=");
                    stringBuilder.append(displayId);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    dc.removeAppToken(this.mToken.asBinder());
                    super.removeContainer();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void removeContainer() {
        throw new UnsupportedOperationException("Use removeContainer(displayId) instead.");
    }

    public void reparent(TaskWindowContainerController taskController, int position) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reparent: moving app token=");
                stringBuilder.append(this.mToken);
                stringBuilder.append(" to task=");
                stringBuilder.append(taskController);
                stringBuilder.append(" at ");
                stringBuilder.append(position);
                Slog.i("WindowManager", stringBuilder.toString());
                if (this.mContainer == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("reparent: could not find app token=");
                    stringBuilder.append(this.mToken);
                    Slog.i("WindowManager", stringBuilder.toString());
                } else {
                    Task task = taskController.mContainer;
                    if (task != null) {
                        ((AppWindowToken) this.mContainer).reparent(task, position);
                        ((AppWindowToken) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("reparent: could not find task=");
                    stringBuilder2.append(taskController);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public Configuration setOrientation(int requestedOrientation, int displayId, Configuration displayConfig, boolean freezeScreenIfNeeded) {
        Configuration configuration;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                configuration = null;
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set orientation of non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    IBinder asBinder;
                    ((AppWindowToken) this.mContainer).setOrientation(requestedOrientation);
                    if (freezeScreenIfNeeded) {
                        asBinder = this.mToken.asBinder();
                    }
                    configuration = this.mService.updateOrientationFromAppTokens(displayConfig, asBinder, displayId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return configuration;
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return configuration;
    }

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
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return -1;
    }

    public void setDisablePreviewScreenshots(boolean disable) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set disable screenshots of non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((AppWindowToken) this.mContainer).setDisablePreviewScreenshots(disable);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x003e, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:19:0x0041, code:
            return;
     */
    /* JADX WARNING: Missing block: B:61:0x0165, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:62:0x0168, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setVisibility(boolean visible, boolean deferHidingClient) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set visibility of non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    AppWindowToken wtoken = this.mContainer;
                    if (visible || !wtoken.hiddenRequested) {
                        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("setAppVisibility(");
                            stringBuilder2.append(this.mToken);
                            stringBuilder2.append(", visible=");
                            stringBuilder2.append(visible);
                            stringBuilder2.append("): ");
                            stringBuilder2.append(this.mService.mAppTransition);
                            stringBuilder2.append(" hidden=");
                            stringBuilder2.append(wtoken.isHidden());
                            stringBuilder2.append(" hiddenRequested=");
                            stringBuilder2.append(wtoken.hiddenRequested);
                            stringBuilder2.append(" Callers=");
                            stringBuilder2.append(Debug.getCallers(6));
                            Slog.v("WindowManager", stringBuilder2.toString());
                        }
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
                            if (wtoken.isHidden() || wtoken.mAppStopped) {
                                wtoken.clearAllDrawn();
                                if (wtoken.isHidden()) {
                                    wtoken.waitingToShow = true;
                                }
                            }
                            wtoken.setClientHidden(false);
                            wtoken.requestUpdateWallpaperIfNeeded();
                            wtoken.mAppStopped = false;
                            ((AppWindowToken) this.mContainer).transferStartingWindowFromHiddenAboveTokenIfNeeded();
                        } else {
                            wtoken.removeDeadWindows();
                        }
                        if (wtoken.okToAnimate() && this.mService.mAppTransition.isTransitionSet()) {
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
                                        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("TRANSIT_TASK_OPEN_BEHIND,  adding ");
                                            stringBuilder3.append(focusedToken);
                                            stringBuilder3.append(" to mOpeningApps");
                                            Slog.d("WindowManager", stringBuilder3.toString());
                                        }
                                        focusedToken.setHidden(true);
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
                while (true) {
                }
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
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean addStartingWindow(String pkg, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, IBinder transferFrom, boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean fromRecents) {
        Throwable th;
        WindowHashMap windowHashMap;
        String str = pkg;
        int i = theme;
        WindowHashMap windowHashMap2 = this.mWindowMap;
        synchronized (windowHashMap2) {
            String str2;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int i2;
                IBinder iBinder;
                if (this.mContainer == null) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Attempted to set icon of non-existing app token: ");
                        stringBuilder.append(this.mToken);
                        Slog.w("WindowManager", stringBuilder.toString());
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    } catch (Throwable th2) {
                        th = th2;
                        i2 = windowFlags;
                        iBinder = transferFrom;
                        windowHashMap = windowHashMap2;
                        str2 = str;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } else if (!((AppWindowToken) this.mContainer).okToDisplay()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (((AppWindowToken) this.mContainer).startingData != null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    WindowState mainWin = ((AppWindowToken) this.mContainer).findMainWindow();
                    if (mainWin != null) {
                        if (mainWin.mWinAnimator.getShown()) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                    }
                    if (sSkipStartingWindowActivitys.contains(this.mToken.getName())) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    TaskSnapshot snapshot = this.mService.mTaskSnapshotController.getSnapshot(((AppWindowToken) this.mContainer).getTask().mTaskId, ((AppWindowToken) this.mContainer).getTask().mUserId, false, false);
                    int type = getStartingWindowType(newTask, taskSwitch, processRunning, allowTaskSnapshot, activityCreated, fromRecents, snapshot);
                    if ("com.android.settings".equals(str)) {
                        if (!this.mService.isSplitMode() && this.mService.mAppTransition.getAppTransition() == 6) {
                            type = 2;
                        }
                    }
                    if (type != 1 || "com.android.contacts".equals(str) || "com.huawei.camera".equals(str) || "com.android.incallui".equals(str) || "com.android.gallery3d".equals(str) || "com.huawei.systemmanager".equals(str) || !this.mTask.isSamePackageInTask()) {
                        int i3;
                        boolean z;
                        WindowState windowState;
                        TaskSnapshot taskSnapshot;
                        if (i != 0) {
                            Entry ent = AttributeCache.instance().get(str, i, R.styleable.Window, this.mService.mCurrentUserId);
                            if (ent == null) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return false;
                            }
                            boolean windowIsTranslucent = ent.array.getBoolean(5, false);
                            boolean windowIsFloating = ent.array.getBoolean(4, false);
                            boolean windowShowWallpaper = ent.array.getBoolean(14, false);
                            boolean windowDisableStarting = ent.array.getBoolean(12, false);
                            Entry entry;
                            if ("com.huawei.android.launcher".equals(str)) {
                                i3 = type;
                                entry = ent;
                                windowState = mainWin;
                                z = false;
                                windowHashMap = windowHashMap2;
                                str2 = str;
                            } else if (this.mService.isSplitMode()) {
                                taskSnapshot = snapshot;
                                i3 = type;
                                entry = ent;
                                windowState = mainWin;
                                z = false;
                                windowHashMap = windowHashMap2;
                                str2 = str;
                            } else {
                                boolean hwStartWindow = HwServiceFactory.getHwAppWindowContainerController() == null ? false : HwServiceFactory.getHwAppWindowContainerController().isHwStartWindowEnabled();
                                if (hwStartWindow) {
                                    i3 = type;
                                    z = false;
                                    Entry entry2 = ent;
                                    windowHashMap = windowHashMap2;
                                    str2 = str;
                                    try {
                                        snapshot = HwServiceFactory.getHwAppWindowContainerController().continueHwStartWindow(str, entry2, compatInfo.mAppInfo, processRunning, windowIsFloating, windowIsTranslucent, windowDisableStarting, newTask, taskSwitch, windowShowWallpaper, transferFrom, this.mToken, this.mRoot);
                                        if (snapshot < null) {
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            return z;
                                        }
                                        if (snapshot > null) {
                                            snapshot = HwServiceFactory.getHwAppWindowContainerController().getTransferFrom(str2);
                                        } else {
                                            snapshot = transferFrom;
                                        }
                                        iBinder = snapshot;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                i3 = type;
                                entry = ent;
                                windowState = mainWin;
                                z = false;
                                windowHashMap = windowHashMap2;
                                str2 = str;
                                if (windowIsTranslucent) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return z;
                                } else if (windowIsFloating || windowDisableStarting) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return z;
                                } else {
                                    iBinder = transferFrom;
                                }
                                if (windowShowWallpaper) {
                                    try {
                                        if (((AppWindowToken) this.mContainer).getDisplayContent().mWallpaperController.getWallpaperTarget() == null) {
                                            i2 = windowFlags | DumpState.DUMP_DEXOPT;
                                        } else if (!hwStartWindow) {
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            return z;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        i2 = windowFlags;
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                i2 = windowFlags;
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return z;
                        }
                        taskSnapshot = snapshot;
                        i3 = type;
                        windowState = mainWin;
                        z = false;
                        windowHashMap = windowHashMap2;
                        str2 = str;
                        i2 = windowFlags;
                        iBinder = transferFrom;
                        try {
                            if (((AppWindowToken) this.mContainer).transferStartingWindow(iBinder)) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return true;
                            } else if (i3 != 2) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return z;
                            } else {
                                ((AppWindowToken) this.mContainer).startingData = new SplashScreenStartingData(this.mService, str2, theme, compatInfo, nonLocalizedLabel, labelRes, icon, logo, i2, ((AppWindowToken) this.mContainer).getMergedOverrideConfiguration());
                                scheduleAddStartingWindow();
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return true;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    boolean createSnapshot = createSnapshot(snapshot);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return createSnapshot;
                }
            } catch (RemoteException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fail to getName for ");
                stringBuilder2.append(this.mToken);
                Slog.w("WindowManager", stringBuilder2.toString());
            } catch (Throwable th6) {
                th = th6;
                windowHashMap = windowHashMap2;
                str2 = str;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
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
        int nType = 2;
        if (newTask || !processRunning || (taskSwitch && !activityCreated)) {
            return 2;
        }
        if (!taskSwitch || !allowTaskSnapshot) {
            return 0;
        }
        if (snapshot == null) {
            nType = 0;
        } else if (snapshotOrientationSameAsTask(snapshot) || fromRecents) {
            nType = 1;
        }
        if (1 == nType && !isContainedOnlyOneVisibleWindow()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Skip adding snapshot startingWindow for activity with more than one window, ");
            stringBuilder.append(((AppWindowToken) this.mContainer).toString());
            Slog.d("WindowManager", stringBuilder.toString());
            nType = 0;
        }
        return nType;
    }

    void scheduleAddStartingWindow() {
        if (!this.mService.mAnimationHandler.hasCallbacks(this.mAddStartingWindow)) {
            if (HwPCUtils.isPcCastModeInServer() && (this.mContainer instanceof AppWindowToken)) {
                TaskStack ts = ((AppWindowToken) this.mContainer).getTask().mStack;
                if (ts != null && HwPCUtils.isPcDynamicStack(ts.mStackId)) {
                    return;
                }
            }
            this.mService.mAnimationHandler.postAtFrontOfQueue(this.mAddStartingWindow);
        }
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
        boolean z = false;
        if (snapshot == null) {
            return false;
        }
        if (((AppWindowToken) this.mContainer).getTask().getConfiguration().orientation == snapshot.getOrientation()) {
            z = true;
        }
        return z;
    }

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
                    this.mService.mAnimationHandler.post(new -$$Lambda$AppWindowContainerController$8qyUV78Is6_I1WVMp6w8VGpeuOE(surface));
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    static /* synthetic */ void lambda$removeStartingWindow$2(StartingSurface surface) {
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
                while (true) {
                }
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
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppResumed(boolean wasStopped) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to notify resumed of non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((AppWindowToken) this.mContainer).notifyAppResumed(wasStopped);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppStopping() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to notify stopping on non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((AppWindowToken) this.mContainer).detachChildren();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void notifyAppStopped() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to notify stopped of non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((AppWindowToken) this.mContainer).notifyAppStopped();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0050, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:17:0x0053, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startFreezingScreen(int configChanges) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                StringBuilder stringBuilder;
                if (this.mContainer == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to freeze screen with non-existing app token: ");
                    stringBuilder.append(this.mContainer);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    if (configChanges == 0) {
                        if (((AppWindowToken) this.mContainer).okToDisplay()) {
                            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Skipping set freeze of ");
                                stringBuilder.append(this.mToken);
                                Slog.v("WindowManager", stringBuilder.toString());
                            }
                        }
                    }
                    ((AppWindowToken) this.mContainer).startFreezingScreen();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void stopFreezingScreen(boolean force) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Clear freezing of ");
                        stringBuilder.append(this.mToken);
                        stringBuilder.append(": hidden=");
                        stringBuilder.append(((AppWindowToken) this.mContainer).isHidden());
                        stringBuilder.append(" freezing=");
                        stringBuilder.append(((AppWindowToken) this.mContainer).isFreezingScreen());
                        Slog.v("WindowManager", stringBuilder.toString());
                    }
                    ((AppWindowToken) this.mContainer).stopFreezingScreen(true, force);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void registerRemoteAnimations(RemoteAnimationDefinition definition) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to register remote animations with non-existing app token: ");
                    stringBuilder.append(this.mToken);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((AppWindowToken) this.mContainer).registerRemoteAnimations(definition);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
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
        return this.mListener != null && ((AppWindowContainerListener) this.mListener).keyDispatchingTimedOut(reason, windowPid);
    }

    public void setWillCloseOrEnterPip(boolean willCloseOrEnterPip) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    ((AppWindowToken) this.mContainer).setWillCloseOrEnterPip(willCloseOrEnterPip);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AppWindowContainerController{ token=");
        stringBuilder.append(this.mToken);
        stringBuilder.append(" mContainer=");
        stringBuilder.append(this.mContainer);
        stringBuilder.append(" mListener=");
        stringBuilder.append(this.mListener);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
