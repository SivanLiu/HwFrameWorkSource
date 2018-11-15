package com.android.server.wm;

import android.os.Debug;
import android.os.Trace;
import android.util.ArraySet;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;
import java.util.function.Predicate;

class WindowSurfacePlacer {
    static final int SET_FORCE_HIDING_CHANGED = 4;
    static final int SET_ORIENTATION_CHANGE_COMPLETE = 8;
    static final int SET_UPDATE_ROTATION = 1;
    static final int SET_WALLPAPER_ACTION_PENDING = 16;
    static final int SET_WALLPAPER_MAY_CHANGE = 2;
    private static final String TAG = "WindowManager";
    private int mDeferDepth = 0;
    private boolean mInLayout = false;
    private boolean mLastIsTopIsFullscreen = false;
    private int mLayoutRepeatCount;
    private final Runnable mPerformSurfacePlacement;
    private final WindowManagerService mService;
    private final SparseIntArray mTempTransitionReasons = new SparseIntArray();
    private final LayerAndToken mTmpLayerAndToken = new LayerAndToken();
    private boolean mTraversalScheduled;
    private final WallpaperController mWallpaperControllerLocked;

    private static final class LayerAndToken {
        public int layer;
        public AppWindowToken token;

        private LayerAndToken() {
        }
    }

    public WindowSurfacePlacer(WindowManagerService service) {
        this.mService = service;
        this.mWallpaperControllerLocked = this.mService.mRoot.mWallpaperController;
        this.mPerformSurfacePlacement = new -$$Lambda$WindowSurfacePlacer$4Hbamt-LFcbu8AoZBoOZN_LveKQ(this);
    }

    public static /* synthetic */ void lambda$new$0(WindowSurfacePlacer windowSurfacePlacer) {
        synchronized (windowSurfacePlacer.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                windowSurfacePlacer.performSurfacePlacement();
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    void deferLayout() {
        this.mDeferDepth++;
    }

    void continueLayout() {
        this.mDeferDepth--;
        if (this.mDeferDepth <= 0) {
            performSurfacePlacement();
        }
    }

    boolean isLayoutDeferred() {
        return this.mDeferDepth > 0;
    }

    final void performSurfacePlacement() {
        performSurfacePlacement(false);
    }

    final void performSurfacePlacement(boolean force) {
        if (this.mDeferDepth <= 0 || force) {
            int loopCount = 6;
            do {
                this.mTraversalScheduled = false;
                performSurfacePlacementLoop();
                this.mService.mAnimationHandler.removeCallbacks(this.mPerformSurfacePlacement);
                loopCount--;
                if (!this.mTraversalScheduled) {
                    break;
                }
            } while (loopCount > 0);
            this.mService.mRoot.mWallpaperActionPending = false;
            boolean isTopIsFullscreen = this.mService.mPolicy.isTopIsFullscreen();
            if (this.mLastIsTopIsFullscreen != isTopIsFullscreen) {
                this.mLastIsTopIsFullscreen = isTopIsFullscreen;
                this.mService.mInputManager.setIsTopFullScreen(this.mLastIsTopIsFullscreen);
            }
        }
    }

    private void performSurfacePlacementLoop() {
        if (this.mInLayout) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("performLayoutAndPlaceSurfacesLocked called while in layout. Callers=");
            stringBuilder.append(Debug.getCallers(3));
            Slog.w(str, stringBuilder.toString());
        } else if (!this.mService.mWaitingForConfig && this.mService.mDisplayReady) {
            Trace.traceBegin(32, "wmLayout");
            this.mInLayout = true;
            boolean recoveringMemory = false;
            if (!this.mService.mForceRemoves.isEmpty()) {
                while (!this.mService.mForceRemoves.isEmpty()) {
                    WindowState ws = (WindowState) this.mService.mForceRemoves.remove(0);
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Force removing: ");
                    stringBuilder2.append(ws);
                    Slog.i(str2, stringBuilder2.toString());
                    ws.removeImmediately();
                }
                Slog.w(TAG, "Due to memory failure, waiting a bit for next layout");
                Object tmp = new Object();
                synchronized (tmp) {
                    try {
                        tmp.wait(250);
                    } catch (InterruptedException e) {
                    }
                }
                recoveringMemory = true;
            }
            try {
                this.mService.mRoot.performSurfacePlacement(recoveringMemory);
                this.mInLayout = false;
                if (this.mService.mRoot.isLayoutNeeded()) {
                    int i = this.mLayoutRepeatCount + 1;
                    this.mLayoutRepeatCount = i;
                    if (i < 6) {
                        requestTraversal();
                    } else {
                        Slog.e(TAG, "Performed 6 layouts in a row. Skipping");
                        this.mLayoutRepeatCount = 0;
                    }
                } else {
                    this.mLayoutRepeatCount = 0;
                }
                if (this.mService.mWindowsChanged && !this.mService.mWindowChangeListeners.isEmpty()) {
                    this.mService.mH.removeMessages(19);
                    this.mService.mH.sendEmptyMessage(19);
                }
            } catch (RuntimeException e2) {
                this.mInLayout = false;
                Slog.wtf(TAG, "Unhandled exception while laying out windows", e2);
            }
            Trace.traceEnd(32);
        }
    }

    void debugLayoutRepeats(String msg, int pendingLayoutChanges) {
        if (this.mLayoutRepeatCount >= 4) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Layouts looping: ");
            stringBuilder.append(msg);
            stringBuilder.append(", mPendingLayoutChanges = 0x");
            stringBuilder.append(Integer.toHexString(pendingLayoutChanges));
            Slog.v(str, stringBuilder.toString());
        }
    }

    boolean isInLayout() {
        return this.mInLayout;
    }

    int handleAppTransitionReadyLocked() {
        Throwable th;
        boolean z;
        AppWindowToken appWindowToken;
        ArraySet<Integer> arraySet;
        int i;
        int appsCount = this.mService.mOpeningApps.size();
        if (!transitionGoodToGo(appsCount, this.mTempTransitionReasons)) {
            return 0;
        }
        AppWindowToken findAnimLayoutParamsToken;
        Trace.traceBegin(32, "AppTransitionReady");
        Flog.i(307, "**** GOOD TO GO");
        int transit = this.mService.mAppTransition.getAppTransition();
        if (this.mService.mSkipAppTransitionAnimation && !AppTransition.isKeyguardGoingAwayTransit(transit)) {
            transit = -1;
        }
        this.mService.mSkipAppTransitionAnimation = false;
        this.mService.mNoAnimationNotifyOnTransitionFinished.clear();
        this.mService.mH.removeMessages(13);
        DisplayContent displayContent = this.mService.getDefaultDisplayContentLocked();
        this.mService.mRoot.mWallpaperMayChange = false;
        for (int i2 = 0; i2 < appsCount; i2++) {
            ((AppWindowToken) this.mService.mOpeningApps.valueAt(i2)).clearAnimatingFlags();
        }
        this.mWallpaperControllerLocked.adjustWallpaperWindowsForAppTransitionIfNeeded(displayContent, this.mService.mOpeningApps);
        boolean hasWallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget() != null;
        boolean openingAppHasWallpaper = canBeWallpaperTarget(this.mService.mOpeningApps) && hasWallpaperTarget;
        boolean closingAppHasWallpaper = canBeWallpaperTarget(this.mService.mClosingApps) && hasWallpaperTarget;
        int transit2 = maybeUpdateTransitToWallpaper(maybeUpdateTransitToTranslucentAnim(transit), openingAppHasWallpaper, closingAppHasWallpaper);
        ArraySet<Integer> activityTypes = collectActivityTypes(this.mService.mOpeningApps, this.mService.mClosingApps);
        if (this.mService.mPolicy.allowAppAnimationsLw()) {
            findAnimLayoutParamsToken = findAnimLayoutParamsToken(transit2, activityTypes);
        } else {
            findAnimLayoutParamsToken = null;
        }
        AppWindowToken animLpToken = findAnimLayoutParamsToken;
        LayoutParams animLp = getAnimLp(animLpToken);
        overrideWithRemoteAnimationIfSet(animLpToken, transit2, activityTypes);
        boolean z2 = containsVoiceInteraction(this.mService.mOpeningApps) || containsVoiceInteraction(this.mService.mOpeningApps);
        boolean voiceInteraction = z2;
        this.mService.mSurfaceAnimationRunner.deferStartingAnimations();
        try {
            AppWindowToken topOpeningApp;
            processApplicationsAnimatingInPlace(transit2);
            this.mTmpLayerAndToken.token = null;
            boolean voiceInteraction2 = voiceInteraction;
            try {
                handleClosingApps(transit2, animLp, voiceInteraction2, this.mTmpLayerAndToken);
                findAnimLayoutParamsToken = this.mTmpLayerAndToken.token;
                AppWindowToken topOpeningApp2 = handleOpeningApps(transit2, animLp, voiceInteraction2);
                if (WindowManagerService.HW_SUPPORT_LAUNCHER_EXIT_ANIM && transit2 == 13) {
                    try {
                        String uniLauncherCmpName = "com.huawei.android.launcher/.unihome.UniHomeLauncher";
                        String drawerLauncherCmpName = "com.huawei.android.launcher/.drawer.DrawerLauncher";
                        topOpeningApp = topOpeningApp2;
                        if (topOpeningApp != null) {
                            try {
                                if (topOpeningApp.toString().contains("com.huawei.android.launcher/.unihome.UniHomeLauncher") || topOpeningApp.toString().contains("com.huawei.android.launcher/.drawer.DrawerLauncher")) {
                                    this.mService.mExitPivotX = -1.0f;
                                    this.mService.mExitPivotY = -1.0f;
                                    this.mService.mExitIconBitmap = null;
                                    this.mService.mExitIconWidth = -1;
                                    this.mService.mExitIconHeight = -1;
                                    this.mService.mExitFlag = -1;
                                    Slog.i(TAG, "exit info has been reset.");
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                z = voiceInteraction2;
                                appWindowToken = animLpToken;
                                arraySet = activityTypes;
                                animLpToken = transit2;
                                this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                                throw th;
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i = appsCount;
                        z = voiceInteraction2;
                        appWindowToken = animLpToken;
                        arraySet = activityTypes;
                        animLpToken = transit2;
                        this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                        throw th;
                    }
                }
                topOpeningApp = topOpeningApp2;
                try {
                    this.mService.mAppTransition.setLastAppTransition(transit2, topOpeningApp, findAnimLayoutParamsToken);
                    appsCount = this.mService.mAppTransition.getTransitFlags();
                } catch (Throwable th4) {
                    th = th4;
                    z = voiceInteraction2;
                    appWindowToken = animLpToken;
                    arraySet = activityTypes;
                    animLpToken = transit2;
                    this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                i = appsCount;
                z = voiceInteraction2;
                appWindowToken = animLpToken;
                arraySet = activityTypes;
                animLpToken = transit2;
                this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                throw th;
            }
            try {
                animLpToken = transit2;
                try {
                    voiceInteraction2 = this.mService.mAppTransition.goodToGo(transit2, topOpeningApp, findAnimLayoutParamsToken, this.mService.mOpeningApps, this.mService.mClosingApps);
                    handleNonAppWindowsInTransition(animLpToken, appsCount);
                    this.mService.mAppTransition.postAnimationCallback();
                    this.mService.mAppTransition.clear();
                    this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                    this.mService.mTaskSnapshotController.onTransitionStarting();
                    this.mService.mOpeningApps.clear();
                    this.mService.mClosingApps.clear();
                    this.mService.mUnknownAppVisibilityController.clear();
                    displayContent.setLayoutNeeded();
                    DisplayContent dc = this.mService.getDefaultDisplayContentLocked();
                    if (HwPCUtils.isPcCastModeInServer()) {
                        DisplayContent pcDC = this.mService.mRoot.getDisplayContent(this.mService.getFocusedDisplayId());
                        if (pcDC != null) {
                            dc = pcDC;
                        }
                    }
                    dc.computeImeTarget(true);
                    this.mService.updateFocusedWindowLocked(2, true);
                    this.mService.mFocusMayChange = false;
                    this.mService.mH.obtainMessage(47, this.mTempTransitionReasons.clone()).sendToTarget();
                    Trace.traceEnd(32);
                    return (voiceInteraction2 | 1) | 2;
                } catch (Throwable th6) {
                    th = th6;
                    this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                appWindowToken = animLpToken;
                arraySet = activityTypes;
                animLpToken = transit2;
                this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                throw th;
            }
        } catch (Throwable th8) {
            th = th8;
            i = appsCount;
            appWindowToken = animLpToken;
            arraySet = activityTypes;
            z = voiceInteraction;
            this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
            throw th;
        }
    }

    private static LayoutParams getAnimLp(AppWindowToken wtoken) {
        WindowState mainWindow = wtoken != null ? wtoken.findMainWindow() : null;
        if (mainWindow != null) {
            return mainWindow.mAttrs;
        }
        return null;
    }

    private void overrideWithRemoteAnimationIfSet(AppWindowToken animLpToken, int transit, ArraySet<Integer> activityTypes) {
        if (transit != 26 && animLpToken != null) {
            RemoteAnimationDefinition definition = animLpToken.getRemoteAnimationDefinition();
            if (definition != null) {
                RemoteAnimationAdapter adapter = definition.getAdapter(transit, activityTypes);
                if (adapter != null) {
                    this.mService.mAppTransition.overridePendingAppTransitionRemote(adapter);
                }
            }
        }
    }

    private AppWindowToken findAnimLayoutParamsToken(int transit, ArraySet<Integer> activityTypes) {
        AppWindowToken result = lookForHighestTokenWithFilter(this.mService.mClosingApps, this.mService.mOpeningApps, new -$$Lambda$WindowSurfacePlacer$AnzDJL6vBWwhbuz7sYsAfUAzZko(transit, activityTypes));
        if (result != null) {
            return result;
        }
        result = lookForHighestTokenWithFilter(this.mService.mClosingApps, this.mService.mOpeningApps, -$$Lambda$WindowSurfacePlacer$wCevQN6hMxiB97Eay8ibpi2Xaxo.INSTANCE);
        if (result != null) {
            return result;
        }
        return lookForHighestTokenWithFilter(this.mService.mClosingApps, this.mService.mOpeningApps, -$$Lambda$WindowSurfacePlacer$tJcqA51ohv9DQjcvHOarwInr01s.INSTANCE);
    }

    static /* synthetic */ boolean lambda$findAnimLayoutParamsToken$1(int transit, ArraySet activityTypes, AppWindowToken w) {
        return w.getRemoteAnimationDefinition() != null && w.getRemoteAnimationDefinition().hasTransition(transit, activityTypes);
    }

    static /* synthetic */ boolean lambda$findAnimLayoutParamsToken$2(AppWindowToken w) {
        return w.fillsParent() && w.findMainWindow() != null;
    }

    static /* synthetic */ boolean lambda$findAnimLayoutParamsToken$3(AppWindowToken w) {
        return w.findMainWindow() != null;
    }

    private ArraySet<Integer> collectActivityTypes(ArraySet<AppWindowToken> array1, ArraySet<AppWindowToken> array2) {
        int i;
        ArraySet<Integer> result = new ArraySet();
        for (i = array1.size() - 1; i >= 0; i--) {
            result.add(Integer.valueOf(((AppWindowToken) array1.valueAt(i)).getActivityType()));
        }
        for (i = array2.size() - 1; i >= 0; i--) {
            result.add(Integer.valueOf(((AppWindowToken) array2.valueAt(i)).getActivityType()));
        }
        return result;
    }

    private AppWindowToken lookForHighestTokenWithFilter(ArraySet<AppWindowToken> array1, ArraySet<AppWindowToken> array2, Predicate<AppWindowToken> filter) {
        int array1count = array1.size();
        int count = array2.size() + array1count;
        int bestPrefixOrderIndex = Integer.MIN_VALUE;
        AppWindowToken bestToken = null;
        for (int i = 0; i < count; i++) {
            AppWindowToken wtoken;
            if (i < array1count) {
                wtoken = (AppWindowToken) array1.valueAt(i);
            } else {
                wtoken = (AppWindowToken) array2.valueAt(i - array1count);
            }
            int prefixOrderIndex = wtoken.getPrefixOrderIndex();
            if (filter.test(wtoken) && prefixOrderIndex > bestPrefixOrderIndex) {
                bestPrefixOrderIndex = prefixOrderIndex;
                bestToken = wtoken;
            }
        }
        return bestToken;
    }

    private boolean containsVoiceInteraction(ArraySet<AppWindowToken> apps) {
        for (int i = apps.size() - 1; i >= 0; i--) {
            if (((AppWindowToken) apps.valueAt(i)).mVoiceInteraction) {
                return true;
            }
        }
        return false;
    }

    private AppWindowToken handleOpeningApps(int transit, LayoutParams animLp, boolean voiceInteraction) {
        int appsCount = this.mService.mOpeningApps.size();
        Object obj = null;
        int topOpeningLayer = Integer.MIN_VALUE;
        AppWindowToken topOpeningApp = null;
        int i = 0;
        while (i < appsCount) {
            AppWindowToken wtoken = (AppWindowToken) this.mService.mOpeningApps.valueAt(i);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Now opening app");
                stringBuilder.append(wtoken);
                Slog.v(str, stringBuilder.toString());
            }
            if (!wtoken.setVisibility(animLp, true, transit, false, voiceInteraction)) {
                this.mService.mNoAnimationNotifyOnTransitionFinished.add(wtoken.token);
            }
            wtoken.updateReportedVisibilityLocked();
            wtoken.waitingToShow = obj;
            this.mService.openSurfaceTransaction();
            try {
                wtoken.showAllWindowsLocked();
                if (animLp != null) {
                    int layer = wtoken.getHighestAnimLayer();
                    if (topOpeningApp == null || layer > topOpeningLayer) {
                        topOpeningApp = wtoken;
                        topOpeningLayer = layer;
                    }
                }
                if (this.mService.mAppTransition.isNextAppTransitionThumbnailUp()) {
                    wtoken.attachThumbnailAnimation();
                } else if (this.mService.mAppTransition.isNextAppTransitionOpenCrossProfileApps()) {
                    wtoken.attachCrossProfileAppsThumbnailAnimation();
                }
                i++;
            } finally {
                this.mService.closeSurfaceTransaction("handleAppTransitionReadyLocked");
            }
        }
        return topOpeningApp;
    }

    private AppWindowToken findMaxWindowSizeToken(int appsCount, int transit) {
        int maxSize = -1;
        AppWindowToken maxSizeToken = null;
        if (WindowManagerService.HW_SUPPORT_LAUNCHER_EXIT_ANIM && transit == 13) {
            for (int i = 0; i < appsCount; i++) {
                AppWindowToken wtoken = (AppWindowToken) this.mService.mClosingApps.valueAt(i);
                if (wtoken != null) {
                    WindowState win = wtoken.findMainWindow();
                    if (win != null && win.mFrame != null && win.mFrame.height() * win.mFrame.width() >= maxSize && this.mService.isSupportHwAppExitAnim(wtoken)) {
                        maxSize = win.mFrame.height() * win.mFrame.width();
                        maxSizeToken = wtoken;
                    }
                }
            }
        }
        return maxSizeToken;
    }

    private void handleClosingApps(int transit, LayoutParams animLp, boolean voiceInteraction, LayerAndToken layerAndToken) {
        LayerAndToken layerAndToken2 = layerAndToken;
        int appsCount = this.mService.mClosingApps.size();
        int i = transit;
        AppWindowToken maxWinHeightToken = findMaxWindowSizeToken(appsCount, i);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("closing apps count = ");
        stringBuilder.append(appsCount);
        Slog.i(str, stringBuilder.toString());
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < appsCount) {
                AppWindowToken wtoken = (AppWindowToken) this.mService.mClosingApps.valueAt(i3);
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Now closing app ");
                    stringBuilder.append(wtoken);
                    Slog.v(str, stringBuilder.toString());
                }
                if (maxWinHeightToken == wtoken) {
                    wtoken.mShouldDrawIcon = true;
                }
                wtoken.setVisibility(animLp, false, i, false, voiceInteraction);
                wtoken.updateReportedVisibilityLocked();
                wtoken.allDrawn = true;
                wtoken.deferClearAllDrawn = false;
                if (!(wtoken.startingWindow == null || wtoken.startingWindow.mAnimatingExit || wtoken.getController() == null)) {
                    wtoken.getController().removeStartingWindow();
                }
                if (animLp != null) {
                    int layer = wtoken.getHighestAnimLayer();
                    if (layerAndToken2.token == null || layer > layerAndToken2.layer) {
                        layerAndToken2.token = wtoken;
                        layerAndToken2.layer = layer;
                    }
                }
                if (this.mService.mAppTransition.isNextAppTransitionThumbnailDown()) {
                    wtoken.attachThumbnailAnimation();
                }
                i2 = i3 + 1;
            } else {
                return;
            }
        }
    }

    private void handleNonAppWindowsInTransition(int transit, int flags) {
        boolean z = false;
        if (transit == 20 && (flags & 4) != 0 && (flags & 2) == 0) {
            Animation anim = this.mService.mPolicy.createKeyguardWallpaperExit((flags & 1) != 0);
            if (anim != null) {
                this.mService.getDefaultDisplayContentLocked().mWallpaperController.startWallpaperAnimation(anim);
            }
        }
        if (transit == 20 || transit == 21) {
            DisplayContent defaultDisplayContentLocked = this.mService.getDefaultDisplayContentLocked();
            boolean z2 = transit == 21;
            if ((flags & 1) != 0) {
                z = true;
            }
            defaultDisplayContentLocked.startKeyguardExitOnNonAppWindows(z2, z);
        }
    }

    private boolean transitionGoodToGo(int appsCount, SparseIntArray outReasons) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Checking ");
        stringBuilder.append(appsCount);
        stringBuilder.append(" opening apps (frozen=");
        stringBuilder.append(this.mService.mDisplayFrozen);
        stringBuilder.append(" timeout=");
        stringBuilder.append(this.mService.mAppTransition.isTimeout());
        stringBuilder.append("), Track: ");
        stringBuilder.append(this.mService.mAppTransitTrack);
        Slog.v(str, stringBuilder.toString());
        this.mService.mAppTransitTrack = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        ScreenRotationAnimation screenRotationAnimation = this.mService.mAnimator.getScreenRotationAnimationLocked(0);
        outReasons.clear();
        if (this.mService.mAppTransition.isTimeout()) {
            return true;
        }
        if (screenRotationAnimation != null && screenRotationAnimation.isAnimating() && this.mService.rotationNeedsUpdateLocked()) {
            Flog.i(310, "wait for screen rotation animation to finish");
            return false;
        }
        for (int i = 0; i < appsCount; i++) {
            AppWindowToken wtoken = (AppWindowToken) this.mService.mOpeningApps.valueAt(i);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Check opening app=");
            stringBuilder2.append(wtoken);
            stringBuilder2.append(": allDrawn=");
            stringBuilder2.append(wtoken.allDrawn);
            stringBuilder2.append(" startingDisplayed=");
            stringBuilder2.append(wtoken.startingDisplayed);
            stringBuilder2.append(" startingMoved=");
            stringBuilder2.append(wtoken.startingMoved);
            stringBuilder2.append(" isRelaunching()=");
            stringBuilder2.append(wtoken.isRelaunching());
            stringBuilder2.append(" startingWindow=");
            stringBuilder2.append(wtoken.startingWindow);
            Slog.v(str2, stringBuilder2.toString());
            boolean allDrawn = wtoken.allDrawn && !wtoken.isRelaunching();
            if (!allDrawn && !wtoken.startingDisplayed && !wtoken.startingMoved) {
                return false;
            }
            int windowingMode = wtoken.getWindowingMode();
            if (allDrawn) {
                outReasons.put(windowingMode, 2);
            } else {
                int i2;
                if (wtoken.startingData instanceof SplashScreenStartingData) {
                    i2 = 1;
                } else {
                    i2 = 4;
                }
                outReasons.put(windowingMode, i2);
            }
        }
        if (this.mService.mAppTransition.isFetchingAppTransitionsSpecs()) {
            Flog.i(310, "wait for the specs to be fetched");
            return false;
        } else if (this.mService.mUnknownAppVisibilityController.allResolved()) {
            boolean wallpaperReady = !this.mWallpaperControllerLocked.isWallpaperVisible() || this.mWallpaperControllerLocked.wallpaperTransitionReady();
            if (wallpaperReady) {
                return true;
            }
            Flog.i(310, "wallpaper is not ready for transition");
            return false;
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("unknownApps is not empty: ");
            stringBuilder3.append(this.mService.mUnknownAppVisibilityController.getDebugMessage());
            Flog.i(310, stringBuilder3.toString());
            return false;
        }
    }

    private int maybeUpdateTransitToWallpaper(int transit, boolean openingAppHasWallpaper, boolean closingAppHasWallpaper) {
        if (transit == 0 || transit == 26 || transit == 19) {
            return transit;
        }
        String str;
        StringBuilder stringBuilder;
        WindowState wallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget();
        WindowState oldWallpaper = this.mWallpaperControllerLocked.isWallpaperTargetAnimating() ? null : wallpaperTarget;
        ArraySet<AppWindowToken> openingApps = this.mService.mOpeningApps;
        ArraySet<AppWindowToken> closingApps = this.mService.mClosingApps;
        AppWindowToken topOpeningApp = getTopApp(this.mService.mOpeningApps, false);
        AppWindowToken topClosingApp = getTopApp(this.mService.mClosingApps, true);
        boolean openingCanBeWallpaperTarget = canBeWallpaperTarget(openingApps);
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("New wallpaper target=");
            stringBuilder2.append(wallpaperTarget);
            stringBuilder2.append(", oldWallpaper=");
            stringBuilder2.append(oldWallpaper);
            stringBuilder2.append(", openingApps=");
            stringBuilder2.append(openingApps);
            stringBuilder2.append(", closingApps=");
            stringBuilder2.append(closingApps);
            Slog.v(str2, stringBuilder2.toString());
        }
        int oldTransit = transit;
        if (openingCanBeWallpaperTarget && transit == 20) {
            transit = 21;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("New transit: ");
                stringBuilder.append(AppTransition.appTransitionToString(21));
                Slog.v(str, stringBuilder.toString());
            }
        } else if (!AppTransition.isKeyguardGoingAwayTransit(transit)) {
            if (closingAppHasWallpaper && openingAppHasWallpaper) {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    Slog.v(TAG, "Wallpaper animation!");
                }
                switch (transit) {
                    case 6:
                    case 8:
                    case 10:
                        transit = 14;
                        break;
                    case 7:
                    case 9:
                    case 11:
                        transit = 15;
                        break;
                }
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("New transit: ");
                    stringBuilder.append(AppTransition.appTransitionToString(transit));
                    Slog.v(str, stringBuilder.toString());
                }
            } else if (oldWallpaper != null && !this.mService.mOpeningApps.isEmpty() && !openingApps.contains(oldWallpaper.mAppToken) && closingApps.contains(oldWallpaper.mAppToken) && topClosingApp == oldWallpaper.mAppToken) {
                transit = 12;
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("New transit away from wallpaper: ");
                    stringBuilder.append(AppTransition.appTransitionToString(12));
                    Slog.v(str, stringBuilder.toString());
                }
            } else if (wallpaperTarget != null && wallpaperTarget.isVisibleLw() && openingApps.contains(wallpaperTarget.mAppToken) && topOpeningApp == wallpaperTarget.mAppToken && transit != 25) {
                transit = 13;
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("New transit into wallpaper: ");
                    stringBuilder.append(AppTransition.appTransitionToString(13));
                    Slog.v(str, stringBuilder.toString());
                }
            }
        }
        if (WindowManagerDebugConfig.HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("New wallpaper target=");
            stringBuilder.append(wallpaperTarget);
            stringBuilder.append(", oldWallpaper=");
            stringBuilder.append(oldWallpaper);
            stringBuilder.append(", oldTransit ");
            stringBuilder.append(AppTransition.appTransitionToString(oldTransit));
            stringBuilder.append(", transit ");
            stringBuilder.append(AppTransition.appTransitionToString(transit));
            Slog.i(str, stringBuilder.toString());
        }
        return transit;
    }

    @VisibleForTesting
    int maybeUpdateTransitToTranslucentAnim(int transit) {
        boolean taskOrActivity = AppTransition.isTaskTransit(transit) || AppTransition.isActivityTransit(transit);
        boolean allOpeningVisible = true;
        boolean allTranslucentOpeningApps = this.mService.mOpeningApps.isEmpty() ^ true;
        for (int i = this.mService.mOpeningApps.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mService.mOpeningApps.valueAt(i);
            if (!token.isVisible()) {
                allOpeningVisible = false;
                if (token.fillsParent()) {
                    allTranslucentOpeningApps = false;
                }
            }
        }
        boolean allTranslucentClosingApps = this.mService.mClosingApps.isEmpty() ^ true;
        int i2 = this.mService.mClosingApps.size() - 1;
        while (true) {
            int i3 = i2;
            if (i3 < 0) {
                break;
            } else if (((AppWindowToken) this.mService.mClosingApps.valueAt(i3)).fillsParent()) {
                allTranslucentClosingApps = false;
                break;
            } else {
                i2 = i3 - 1;
            }
        }
        if (taskOrActivity && allTranslucentClosingApps && allOpeningVisible) {
            return 25;
        }
        if (taskOrActivity && allTranslucentOpeningApps && this.mService.mClosingApps.isEmpty()) {
            return 24;
        }
        return transit;
    }

    private boolean canBeWallpaperTarget(ArraySet<AppWindowToken> apps) {
        for (int i = apps.size() - 1; i >= 0; i--) {
            if (((AppWindowToken) apps.valueAt(i)).windowsCanBeWallpaperTarget()) {
                return true;
            }
        }
        return false;
    }

    private AppWindowToken getTopApp(ArraySet<AppWindowToken> apps, boolean ignoreHidden) {
        int topPrefixOrderIndex = Integer.MIN_VALUE;
        AppWindowToken topApp = null;
        for (int i = apps.size() - 1; i >= 0; i--) {
            AppWindowToken app = (AppWindowToken) apps.valueAt(i);
            if (!ignoreHidden || !app.isHidden()) {
                int prefixOrderIndex = app.getPrefixOrderIndex();
                if (prefixOrderIndex > topPrefixOrderIndex) {
                    topPrefixOrderIndex = prefixOrderIndex;
                    topApp = app;
                }
            }
        }
        return topApp;
    }

    private void processApplicationsAnimatingInPlace(int transit) {
        if (transit == 17) {
            WindowState win = this.mService.getDefaultDisplayContentLocked().findFocusedWindow();
            if (win != null) {
                AppWindowToken wtoken = win.mAppToken;
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Now animating app in place ");
                    stringBuilder.append(wtoken);
                    Slog.v(str, stringBuilder.toString());
                }
                wtoken.cancelAnimation();
                wtoken.applyAnimationLocked(null, transit, false, false);
                wtoken.updateReportedVisibilityLocked();
                wtoken.showAllWindowsLocked();
            }
        }
    }

    void requestTraversal() {
        if (!this.mTraversalScheduled) {
            this.mTraversalScheduled = true;
            this.mService.mAnimationHandler.post(this.mPerformSurfacePlacement);
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mTraversalScheduled=");
        stringBuilder.append(this.mTraversalScheduled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mHoldScreenWindow=");
        stringBuilder.append(this.mService.mRoot.mHoldScreenWindow);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mObscuringWindow=");
        stringBuilder.append(this.mService.mRoot.mObscuringWindow);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mDeferDepth=");
        stringBuilder.append(this.mDeferDepth);
        pw.println(stringBuilder.toString());
    }
}
