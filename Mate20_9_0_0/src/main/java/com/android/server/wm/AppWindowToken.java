package com.android.server.wm;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.IApplicationToken;
import android.view.RemoteAnimationDefinition;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.input.InputApplicationHandle;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy.StartingSurface;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

class AppWindowToken extends WindowToken implements AppFreezeListener {
    private static final int APP_ANIMATION_DURATION = 300;
    private static final String TAG = "WindowManager";
    private static final String TAG_VISIBILITY = "WindowManager_visibility";
    private static final int Z_BOOST_BASE = 800570000;
    boolean allDrawn;
    String appComponentName;
    String appPackageName;
    int appPid;
    String appProcessName;
    final IApplicationToken appToken;
    boolean deferClearAllDrawn;
    boolean firstWindowDrawn;
    boolean hiddenRequested;
    boolean inPendingTransaction;
    boolean layoutConfigChanges;
    private boolean mAlwaysFocusable;
    private AnimatingAppWindowTokenRegistry mAnimatingAppWindowTokenRegistry;
    boolean mAppStopped;
    private boolean mCanTurnScreenOn;
    private boolean mClientHidden;
    boolean mDeferHidingClient;
    private boolean mDisablePreviewScreenshots;
    boolean mEnteringAnimation;
    private boolean mFillsParent;
    private boolean mFreezingScreen;
    ArrayDeque<Rect> mFrozenBounds;
    ArrayDeque<Configuration> mFrozenMergedConfig;
    private boolean mHiddenSetFromTransferredStartingWindow;
    private int mHwGestureNavOptions;
    private boolean mHwNotchSupport;
    final InputApplicationHandle mInputApplicationHandle;
    long mInputDispatchingTimeoutNanos;
    boolean mIsExiting;
    private boolean mLastAllDrawn;
    private boolean mLastContainsDismissKeyguardWindow;
    private boolean mLastContainsShowWhenLockedWindow;
    private Task mLastParent;
    private boolean mLastSurfaceShowing;
    private long mLastTransactionSequence;
    boolean mLaunchTaskBehind;
    private Letterbox mLetterbox;
    private boolean mNeedsZBoost;
    private int mNumDrawnWindows;
    private int mNumInterestingWindows;
    int mPendingRelaunchCount;
    private RemoteAnimationDefinition mRemoteAnimationDefinition;
    private boolean mRemovingFromDisplay;
    private boolean mReparenting;
    private final UpdateReportedVisibilityResults mReportedVisibilityResults;
    int mRotationAnimationHint;
    boolean mShouldDrawIcon;
    boolean mShowForAllUsers;
    int mTargetSdk;
    private AppWindowThumbnail mThumbnail;
    private final Point mTmpPoint;
    private final Rect mTmpRect;
    private int mTransit;
    private int mTransitFlags;
    final boolean mVoiceInteraction;
    private boolean mWillCloseOrEnterPip;
    boolean navigationBarHide;
    boolean removed;
    private boolean reportedDrawn;
    boolean reportedVisible;
    StartingData startingData;
    boolean startingDisplayed;
    boolean startingMoved;
    StartingSurface startingSurface;
    WindowState startingWindow;

    public boolean getHwNotchSupport() {
        return this.mHwNotchSupport;
    }

    AppWindowToken(WindowManagerService service, IApplicationToken token, boolean voiceInteraction, DisplayContent dc, long inputDispatchingTimeoutNanos, boolean fullscreen, boolean showForAllUsers, int targetSdk, int orientation, int rotationAnimationHint, int configChanges, boolean launchTaskBehind, boolean alwaysFocusable, AppWindowContainerController controller, boolean naviBarHide, ActivityInfo info) {
        ActivityInfo activityInfo = info;
        this(service, token, voiceInteraction, dc, fullscreen);
        setController(controller);
        this.mInputDispatchingTimeoutNanos = inputDispatchingTimeoutNanos;
        this.mShowForAllUsers = showForAllUsers;
        this.mTargetSdk = targetSdk;
        this.mOrientation = orientation;
        this.navigationBarHide = naviBarHide;
        this.layoutConfigChanges = (configChanges & 1152) != 0;
        this.mLaunchTaskBehind = launchTaskBehind;
        this.mAlwaysFocusable = alwaysFocusable;
        this.mRotationAnimationHint = rotationAnimationHint;
        setHidden(true);
        this.hiddenRequested = true;
        this.mHwNotchSupport = activityInfo.hwNotchSupport;
        this.appPackageName = activityInfo.packageName;
        this.appComponentName = info.getComponentName().flattenToShortString();
        this.appProcessName = activityInfo.applicationInfo.processName;
        this.mHwGestureNavOptions = activityInfo.hwGestureNavOptions;
    }

    AppWindowToken(WindowManagerService service, IApplicationToken token, boolean voiceInteraction, DisplayContent dc, boolean fillsParent) {
        super(service, token != null ? token.asBinder() : null, 2, true, dc, false);
        this.mShouldDrawIcon = false;
        this.mRemovingFromDisplay = false;
        this.mLastTransactionSequence = Long.MIN_VALUE;
        this.mReportedVisibilityResults = new UpdateReportedVisibilityResults();
        this.mFrozenBounds = new ArrayDeque();
        this.mFrozenMergedConfig = new ArrayDeque();
        this.mCanTurnScreenOn = true;
        this.mLastSurfaceShowing = true;
        this.mTmpPoint = new Point();
        this.mTmpRect = new Rect();
        this.appToken = token;
        this.mVoiceInteraction = voiceInteraction;
        this.mFillsParent = fillsParent;
        this.mInputApplicationHandle = new InputApplicationHandle(this);
    }

    void onFirstWindowDrawn(WindowState win, WindowStateAnimator winAnimator) {
        this.firstWindowDrawn = true;
        removeDeadWindows();
        if (this.startingWindow != null) {
            win.cancelAnimation();
            if (getController() != null) {
                getController().removeStartingWindow();
            }
        }
        updateReportedVisibilityLocked();
    }

    void updateReportedVisibilityLocked() {
        if (this.appToken != null) {
            int i;
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Update reported visibility: ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            int count = this.mChildren.size();
            this.mReportedVisibilityResults.reset();
            boolean nowVisible = false;
            for (i = 0; i < count; i++) {
                ((WindowState) this.mChildren.get(i)).updateReportedVisibility(this.mReportedVisibilityResults);
            }
            i = this.mReportedVisibilityResults.numInteresting;
            int numVisible = this.mReportedVisibilityResults.numVisible;
            int numDrawn = this.mReportedVisibilityResults.numDrawn;
            boolean nowGone = this.mReportedVisibilityResults.nowGone;
            boolean nowDrawn = i > 0 && numDrawn >= i;
            if (i > 0 && numVisible >= i && !isHidden()) {
                nowVisible = true;
            }
            if (!nowGone) {
                if (!nowDrawn) {
                    nowDrawn = this.reportedDrawn;
                }
                if (!nowVisible) {
                    nowVisible = this.reportedVisible;
                }
            }
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("VIS ");
                stringBuilder2.append(this);
                stringBuilder2.append(": interesting=");
                stringBuilder2.append(i);
                stringBuilder2.append(" visible=");
                stringBuilder2.append(numVisible);
                Slog.v(str2, stringBuilder2.toString());
            }
            AppWindowContainerController controller = getController();
            if (nowDrawn != this.reportedDrawn) {
                if (nowDrawn && controller != null) {
                    controller.reportWindowsDrawn();
                }
                this.reportedDrawn = nowDrawn;
            }
            if (nowVisible != this.reportedVisible) {
                String str3;
                StringBuilder stringBuilder3;
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Visibility changed in ");
                    stringBuilder3.append(this);
                    stringBuilder3.append(": vis=");
                    stringBuilder3.append(nowVisible);
                    Slog.v(str3, stringBuilder3.toString());
                }
                this.reportedVisible = nowVisible;
                if (controller != null) {
                    if (nowVisible) {
                        controller.reportWindowsVisible();
                    } else {
                        if (toString().contains("com.android.incallui/.InCallActivity")) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("InCallActivity windowsGone, numInteresting=");
                            stringBuilder3.append(i);
                            stringBuilder3.append(" numVisible=");
                            stringBuilder3.append(numVisible);
                            stringBuilder3.append(" numDrawn=");
                            stringBuilder3.append(numDrawn);
                            stringBuilder3.append(" nowGone=");
                            stringBuilder3.append(nowGone);
                            stringBuilder3.append(" callers=");
                            stringBuilder3.append(Debug.getCallers(4));
                            Slog.i(str3, stringBuilder3.toString());
                        }
                        controller.reportWindowsGone();
                    }
                }
            }
        }
    }

    boolean isClientHidden() {
        return this.mClientHidden;
    }

    void setClientHidden(boolean hideClient) {
        if (this.mClientHidden != hideClient && (!hideClient || !this.mDeferHidingClient)) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setClientHidden: ");
                stringBuilder.append(this);
                stringBuilder.append(" clientHidden=");
                stringBuilder.append(hideClient);
                stringBuilder.append(" Callers=");
                stringBuilder.append(Debug.getCallers(5));
                Slog.v(str, stringBuilder.toString());
            }
            this.mClientHidden = hideClient;
            sendAppVisibilityToClients();
        }
    }

    boolean setVisibility(LayoutParams lp, boolean visible, int transit, boolean performLayout, boolean isVoiceInteraction) {
        int i;
        boolean z = visible;
        int i2 = transit;
        boolean z2 = performLayout;
        boolean delayed = false;
        this.inPendingTransaction = false;
        this.mHiddenSetFromTransferredStartingWindow = false;
        boolean visibilityChanged = false;
        LayoutParams layoutParams;
        boolean z3;
        if (isHidden() == z || ((isHidden() && this.mIsExiting) || (z && waitingForReplacement()))) {
            AccessibilityController accessibilityController = this.mService.mAccessibilityController;
            boolean changed = false;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Changing app ");
                stringBuilder.append(this);
                stringBuilder.append(" hidden=");
                stringBuilder.append(isHidden());
                stringBuilder.append(" performLayout=");
                stringBuilder.append(z2);
                Slog.v(str, stringBuilder.toString());
            }
            boolean runningAppAnimation = false;
            if (i2 != -1) {
                if (applyAnimationLocked(lp, i2, z, isVoiceInteraction)) {
                    runningAppAnimation = true;
                    delayed = true;
                }
                WindowState window = findMainWindow();
                if (!(window == null || accessibilityController == null || getDisplayContent().getDisplayId() != 0)) {
                    accessibilityController.onAppWindowTransitionLocked(window, i2);
                }
                changed = true;
            } else {
                layoutParams = lp;
                z3 = isVoiceInteraction;
            }
            boolean changed2 = changed;
            for (int i3 = 0; i3 < this.mChildren.size(); i3++) {
                changed2 |= ((WindowState) this.mChildren.get(i3)).onAppVisibilityChanged(z, runningAppAnimation);
            }
            setHidden(z ^ 1);
            this.hiddenRequested = z ^ 1;
            visibilityChanged = true;
            if (z) {
                if (!(this.startingWindow == null || this.startingWindow.isDrawnLw())) {
                    this.startingWindow.mPolicyVisibility = false;
                    this.startingWindow.mPolicyVisibilityAfterAnim = false;
                }
                WindowManagerService windowManagerService = this.mService;
                Objects.requireNonNull(windowManagerService);
                forAllWindows((Consumer) new -$$Lambda$2KrtdmjrY7Nagc4IRqzCk9gDuQU(windowManagerService), true);
            } else {
                stopFreezingScreen(true, true);
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setVisibility: ");
                stringBuilder2.append(this);
                stringBuilder2.append(": hidden=");
                stringBuilder2.append(isHidden());
                stringBuilder2.append(" hiddenRequested=");
                stringBuilder2.append(this.hiddenRequested);
                Slog.v(str2, stringBuilder2.toString());
            }
            if (changed2) {
                this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
                if (z2) {
                    this.mService.updateFocusedWindowLocked(3, false);
                    this.mService.mWindowPlacerLocked.performSurfacePlacement();
                }
                this.mService.mInputMonitor.updateInputWindowsLw(false);
            }
        } else {
            layoutParams = lp;
            z3 = isVoiceInteraction;
        }
        if (isReallyAnimating()) {
            delayed = true;
        } else {
            onAnimationFinished();
        }
        for (i = this.mChildren.size() - 1; i >= 0 && !delayed; i--) {
            if (((WindowState) this.mChildren.get(i)).isSelfOrChildAnimating()) {
                delayed = true;
            }
        }
        if (visibilityChanged) {
            if (z && !delayed) {
                this.mEnteringAnimation = true;
                this.mService.mActivityManagerAppTransitionNotifier.onAppTransitionFinishedLocked(this.token);
            }
            if (z || !isReallyAnimating()) {
                setClientHidden(z ^ 1);
            }
            if (!(this.mService.mClosingApps.contains(this) || this.mService.mOpeningApps.contains(this))) {
                this.mService.getDefaultDisplayContentLocked().getDockedDividerController().notifyAppVisibilityChanged();
                this.mService.mTaskSnapshotController.notifyAppVisibilityChanged(this, z);
            }
            if (!(!isHidden() || delayed || this.mService.mAppTransition.isTransitionSet())) {
                SurfaceControl.openTransaction();
                for (i = this.mChildren.size() - 1; i >= 0; i--) {
                    ((WindowState) this.mChildren.get(i)).mWinAnimator.hide("immediately hidden");
                }
                SurfaceControl.closeTransaction();
            }
        }
        return delayed;
    }

    WindowState getTopFullscreenWindow() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) this.mChildren.get(i);
            if (win != null && win.mAttrs.isFullscreen()) {
                return win;
            }
        }
        return null;
    }

    WindowState findMainWindow() {
        return findMainWindow(true);
    }

    WindowState findMainWindow(boolean includeStartingApp) {
        WindowState candidate = null;
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            WindowState win = (WindowState) this.mChildren.get(j);
            if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || win != null) {
                int type = win.mAttrs.type;
                if (type == 1 || (includeStartingApp && type == 3)) {
                    if (!win.mAnimatingExit) {
                        return win;
                    }
                    candidate = win;
                }
            }
        }
        return candidate;
    }

    boolean windowsAreFocusable() {
        return getWindowConfiguration().canReceiveKeys() || this.mAlwaysFocusable;
    }

    AppWindowContainerController getController() {
        WindowContainerController controller = super.getController();
        return controller != null ? (AppWindowContainerController) controller : null;
    }

    boolean isVisible() {
        return isHidden() ^ 1;
    }

    void removeImmediately() {
        onRemovedFromDisplay();
        super.removeImmediately();
    }

    void removeIfPossible() {
        this.mIsExiting = false;
        removeAllWindowsIfPossible();
        removeImmediately();
    }

    boolean checkCompleteDeferredRemoval() {
        if (this.mIsExiting) {
            removeIfPossible();
        }
        return super.checkCompleteDeferredRemoval();
    }

    void onRemovedFromDisplay() {
        if (!this.mRemovingFromDisplay) {
            String str;
            StringBuilder stringBuilder;
            String str2;
            StringBuilder stringBuilder2;
            this.mRemovingFromDisplay = true;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Removing app token: ");
                stringBuilder3.append(this);
                Slog.v(str3, stringBuilder3.toString());
            }
            boolean delayed = setVisibility(null, false, -1, true, this.mVoiceInteraction);
            this.mService.mOpeningApps.remove(this);
            this.mService.mUnknownAppVisibilityController.appRemovedOrHidden(this);
            this.mService.mTaskSnapshotController.onAppRemoved(this);
            this.waitingToShow = false;
            if (this.mService.mClosingApps.contains(this)) {
                delayed = true;
            } else if (this.mService.mAppTransition.isTransitionSet()) {
                this.mService.mClosingApps.add(this);
                delayed = true;
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Removing app ");
                stringBuilder.append(this);
                stringBuilder.append(" delayed=");
                stringBuilder.append(delayed);
                stringBuilder.append(" animation=");
                stringBuilder.append(getAnimation());
                stringBuilder.append(" animating=");
                stringBuilder.append(isSelfAnimating());
                Slog.v(str, stringBuilder.toString());
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeAppToken: ");
            stringBuilder.append(this);
            stringBuilder.append(" delayed=");
            stringBuilder.append(delayed);
            stringBuilder.append(" Callers=");
            stringBuilder.append(Debug.getCallers(4));
            Slog.v(str, stringBuilder.toString());
            if (!(this.startingData == null || getController() == null)) {
                getController().removeStartingWindow();
            }
            if (isSelfAnimating()) {
                this.mService.mNoAnimationNotifyOnTransitionFinished.add(this.token);
            }
            TaskStack stack = getStack();
            if (!delayed || isEmpty()) {
                cancelAnimation();
                if (stack != null) {
                    stack.mExitingAppTokens.remove(this);
                }
                removeIfPossible();
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("removeAppToken make exiting: ");
                stringBuilder2.append(this);
                Slog.v(str2, stringBuilder2.toString());
                if (stack != null) {
                    stack.mExitingAppTokens.add(this);
                }
                this.mIsExiting = true;
            }
            this.removed = true;
            stopFreezingScreen(true, true);
            if (this.mService.mFocusedApp == this) {
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removing focused app token:");
                    stringBuilder2.append(this);
                    Slog.v(str2, stringBuilder2.toString());
                }
                this.mService.mFocusedApp = null;
                this.mService.updateFocusedWindowLocked(0, true);
                this.mService.mInputMonitor.setFocusedAppLw(null);
            }
            if (!delayed) {
                updateReportedVisibilityLocked();
            }
            this.mRemovingFromDisplay = false;
        }
    }

    void clearAnimatingFlags() {
        boolean wallpaperMightChange = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            wallpaperMightChange |= ((WindowState) this.mChildren.get(i)).clearAnimatingFlags();
        }
        if (wallpaperMightChange) {
            requestUpdateWallpaperIfNeeded();
        }
    }

    void destroySurfaces() {
        destroySurfaces(false);
    }

    private void destroySurfaces(boolean cleanupOnResume) {
        boolean destroyedSomething = false;
        ArrayList<WindowState> children = new ArrayList(this.mChildren);
        for (int i = children.size() - 1; i >= 0; i--) {
            destroyedSomething |= ((WindowState) children.get(i)).destroySurface(cleanupOnResume, this.mAppStopped);
        }
        if (destroyedSomething) {
            getDisplayContent().assignWindowLayers(true);
            updateLetterboxSurface(null);
        }
    }

    void notifyAppResumed(boolean wasStopped) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyAppResumed: wasStopped=");
        stringBuilder.append(wasStopped);
        stringBuilder.append(" ");
        stringBuilder.append(this);
        Slog.v(str, stringBuilder.toString());
        this.mAppStopped = false;
        setCanTurnScreenOn(true);
        if (!wasStopped) {
            destroySurfaces(true);
        }
    }

    void notifyAppStopped() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyAppStopped: ");
        stringBuilder.append(this);
        Slog.v(str, stringBuilder.toString());
        this.mAppStopped = true;
        destroySurfaces();
        if (getController() != null) {
            getController().removeStartingWindow();
        }
    }

    void clearAllDrawn() {
        this.allDrawn = false;
        this.deferClearAllDrawn = false;
    }

    Task getTask() {
        return (Task) getParent();
    }

    TaskStack getStack() {
        Task task = getTask();
        if (task != null) {
            return task.mStack;
        }
        return null;
    }

    void onParentSet() {
        AnimatingAppWindowTokenRegistry animatingAppWindowTokenRegistry;
        super.onParentSet();
        Task task = getTask();
        if (!this.mReparenting) {
            if (task == null) {
                this.mService.mClosingApps.remove(this);
            } else if (!(this.mLastParent == null || this.mLastParent.mStack == null)) {
                task.mStack.mExitingAppTokens.remove(this);
            }
        }
        TaskStack stack = getStack();
        if (this.mAnimatingAppWindowTokenRegistry != null) {
            this.mAnimatingAppWindowTokenRegistry.notifyFinished(this);
        }
        if (stack != null) {
            animatingAppWindowTokenRegistry = stack.getAnimatingAppWindowTokenRegistry();
        } else {
            animatingAppWindowTokenRegistry = null;
        }
        this.mAnimatingAppWindowTokenRegistry = animatingAppWindowTokenRegistry;
        this.mLastParent = task;
    }

    void postWindowRemoveStartingWindowCleanup(WindowState win) {
        if (this.startingWindow == win) {
            if (getController() != null) {
                getController().removeStartingWindow();
            }
        } else if (this.mChildren.size() == 0) {
            this.startingData = null;
            if (this.mHiddenSetFromTransferredStartingWindow) {
                setHidden(true);
            }
        } else if (this.mChildren.size() == 1 && this.startingSurface != null && !isRelaunching() && getController() != null) {
            getController().removeStartingWindow();
        }
    }

    void removeDeadWindows() {
        for (int winNdx = this.mChildren.size() - 1; winNdx >= 0; winNdx--) {
            WindowState win = (WindowState) this.mChildren.get(winNdx);
            if (win.mAppDied) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeDeadWindows: ");
                stringBuilder.append(win);
                Slog.w(str, stringBuilder.toString());
                win.mDestroying = true;
                win.removeIfPossible();
            }
        }
    }

    boolean hasWindowsAlive() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (!((WindowState) this.mChildren.get(i)).mAppDied) {
                return true;
            }
        }
        return false;
    }

    void setWillReplaceWindows(boolean animate) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Marking app token ");
        stringBuilder.append(this);
        stringBuilder.append(" with replacing windows.");
        Slog.d(str, stringBuilder.toString());
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).setWillReplaceWindow(animate);
        }
    }

    void setWillReplaceChildWindows() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Marking app token ");
        stringBuilder.append(this);
        stringBuilder.append(" with replacing child windows.");
        Slog.d(str, stringBuilder.toString());
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).setWillReplaceChildWindows();
        }
    }

    void clearWillReplaceWindows() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Resetting app token ");
        stringBuilder.append(this);
        stringBuilder.append(" of replacing window marks.");
        Slog.d(str, stringBuilder.toString());
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).clearWillReplaceWindow();
        }
    }

    void requestUpdateWallpaperIfNeeded() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).requestUpdateWallpaperIfNeeded();
        }
    }

    boolean isRelaunching() {
        return this.mPendingRelaunchCount > 0;
    }

    boolean shouldFreezeBounds() {
        Task task = getTask();
        return (task == null || task.inFreeformWindowingMode() || task.inHwPCFreeformWindowingMode()) ? false : getTask().isDragResizing();
    }

    void startRelaunching() {
        if (shouldFreezeBounds()) {
            freezeBounds();
        }
        detachChildren();
        this.mPendingRelaunchCount++;
    }

    void detachChildren() {
        SurfaceControl.openTransaction();
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).mWinAnimator.detachChildren();
        }
        SurfaceControl.closeTransaction();
    }

    void finishRelaunching() {
        unfreezeBounds();
        if (this.mPendingRelaunchCount > 0) {
            this.mPendingRelaunchCount--;
        } else {
            checkKeyguardFlagsChanged();
        }
    }

    void clearRelaunching() {
        if (this.mPendingRelaunchCount != 0) {
            unfreezeBounds();
            this.mPendingRelaunchCount = 0;
        }
    }

    protected boolean isFirstChildWindowGreaterThanSecond(WindowState newWindow, WindowState existingWindow) {
        int type1 = newWindow.mAttrs.type;
        int type2 = existingWindow.mAttrs.type;
        if (type1 == 1 && type2 != 1) {
            return false;
        }
        if (type1 == 1 || type2 != 1) {
            return (type1 == 3 && type2 != 3) || type1 == 3 || type2 != 3;
        } else {
            return true;
        }
    }

    void addWindow(WindowState w) {
        super.addWindow(w);
        boolean gotReplacementWindow = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            gotReplacementWindow |= ((WindowState) this.mChildren.get(i)).setReplacementWindowIfNeeded(w);
        }
        if (gotReplacementWindow) {
            this.mService.scheduleWindowReplacementTimeouts(this);
        }
        checkKeyguardFlagsChanged();
    }

    void removeChild(WindowState child) {
        super.removeChild(child);
        checkKeyguardFlagsChanged();
        updateLetterboxSurface(child);
    }

    private boolean waitingForReplacement() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowState) this.mChildren.get(i)).waitingForReplacement()) {
                return true;
            }
        }
        return false;
    }

    void onWindowReplacementTimeout() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).onWindowReplacementTimeout();
        }
    }

    void reparent(Task task, int position) {
        Task currentTask = getTask();
        StringBuilder stringBuilder;
        if (task == currentTask) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("window token=");
            stringBuilder.append(this);
            stringBuilder.append(" already child of task=");
            stringBuilder.append(currentTask);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (currentTask.mStack == task.mStack) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("reParentWindowToken: removing window token=");
            stringBuilder.append(this);
            stringBuilder.append(" from task=");
            stringBuilder.append(currentTask);
            Slog.i(str, stringBuilder.toString());
            DisplayContent prevDisplayContent = getDisplayContent();
            this.mReparenting = true;
            getParent().removeChild(this);
            task.addChild(this, position);
            this.mReparenting = false;
            DisplayContent displayContent = task.getDisplayContent();
            displayContent.setLayoutNeeded();
            if (prevDisplayContent != displayContent) {
                onDisplayChanged(displayContent);
                prevDisplayContent.setLayoutNeeded();
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("window token=");
            stringBuilder.append(this);
            stringBuilder.append(" current task=");
            stringBuilder.append(currentTask);
            stringBuilder.append(" belongs to a different stack than ");
            stringBuilder.append(task);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private void freezeBounds() {
        Task task = getTask();
        this.mFrozenBounds.offer(new Rect(task.mPreparedFrozenBounds));
        if (task.mPreparedFrozenMergedConfig.equals(Configuration.EMPTY)) {
            this.mFrozenMergedConfig.offer(new Configuration(task.getConfiguration()));
        } else {
            this.mFrozenMergedConfig.offer(new Configuration(task.mPreparedFrozenMergedConfig));
        }
        task.mPreparedFrozenMergedConfig.unset();
    }

    private void unfreezeBounds() {
        if (!this.mFrozenBounds.isEmpty()) {
            this.mFrozenBounds.remove();
            if (!this.mFrozenMergedConfig.isEmpty()) {
                this.mFrozenMergedConfig.remove();
            }
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                ((WindowState) this.mChildren.get(i)).onUnfreezeBounds();
            }
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    void setAppLayoutChanges(int changes, String reason) {
        if (!this.mChildren.isEmpty()) {
            DisplayContent dc = getDisplayContent();
            dc.pendingLayoutChanges |= changes;
        }
    }

    void removeReplacedWindowIfNeeded(WindowState replacement) {
        int i = this.mChildren.size() - 1;
        while (i >= 0 && !((WindowState) this.mChildren.get(i)).removeReplacedWindowIfNeeded(replacement)) {
            i--;
        }
    }

    void startFreezingScreen() {
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Set freezing of ");
            stringBuilder.append(this.appToken);
            stringBuilder.append(": hidden=");
            stringBuilder.append(isHidden());
            stringBuilder.append(" freezing=");
            stringBuilder.append(this.mFreezingScreen);
            stringBuilder.append(" hiddenRequested=");
            stringBuilder.append(this.hiddenRequested);
            WindowManagerService.logWithStack(str, stringBuilder.toString());
        }
        if (!this.hiddenRequested) {
            int i = 0;
            if (!this.mFreezingScreen) {
                this.mFreezingScreen = true;
                this.mService.registerAppFreezeListener(this);
                WindowManagerService windowManagerService = this.mService;
                windowManagerService.mAppsFreezingScreen++;
                if (this.mService.mAppsFreezingScreen == 1) {
                    this.mService.startFreezingDisplayLocked(0, 0, getDisplayContent());
                    this.mService.mH.removeMessages(17);
                    long delayTime = 2000;
                    if (this.mService.mShouldResetTime) {
                        this.mService.mShouldResetTime = false;
                        delayTime = 800;
                    }
                    this.mService.mH.sendEmptyMessageDelayed(17, delayTime);
                }
            }
            int count = this.mChildren.size();
            while (i < count) {
                ((WindowState) this.mChildren.get(i)).onStartFreezingScreen();
                i++;
            }
        }
    }

    void stopFreezingScreen(boolean unfreezeSurfaceNow, boolean force) {
        if (this.mFreezingScreen) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Clear freezing of ");
                stringBuilder.append(this);
                stringBuilder.append(" force=");
                stringBuilder.append(force);
                Slog.v(str, stringBuilder.toString());
            }
            boolean unfrozeWindows = false;
            for (int i = 0; i < this.mChildren.size(); i++) {
                unfrozeWindows |= ((WindowState) this.mChildren.get(i)).onStopFreezingScreen();
            }
            if (force || unfrozeWindows) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No longer freezing: ");
                    stringBuilder2.append(this);
                    Slog.v(str2, stringBuilder2.toString());
                }
                this.mFreezingScreen = false;
                this.mService.unregisterAppFreezeListener(this);
                WindowManagerService windowManagerService = this.mService;
                windowManagerService.mAppsFreezingScreen--;
                this.mService.mLastFinishedFreezeSource = this;
            }
            if (unfreezeSurfaceNow) {
                if (unfrozeWindows) {
                    this.mService.mWindowPlacerLocked.performSurfacePlacement();
                }
                this.mService.stopFreezingDisplayLocked();
            }
        }
    }

    public void onAppFreezeTimeout() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Force clearing freeze: ");
        stringBuilder.append(this);
        Slog.w(str, stringBuilder.toString());
        stopFreezingScreen(true, true);
    }

    void transferStartingWindowFromHiddenAboveTokenIfNeeded() {
        Task task = getTask();
        int i = task.mChildren.size() - 1;
        while (i >= 0) {
            AppWindowToken fromToken = (AppWindowToken) task.mChildren.get(i);
            if (fromToken != this) {
                if (!fromToken.hiddenRequested || !transferStartingWindow(fromToken.token)) {
                    i--;
                } else {
                    return;
                }
            }
            return;
        }
    }

    boolean transferStartingWindow(IBinder transferFrom) {
        AppWindowToken fromToken = getDisplayContent().getAppWindowToken(transferFrom);
        if (fromToken == null || this.startingWindow != null) {
            return false;
        }
        WindowState tStartingWindow = fromToken.startingWindow;
        if (tStartingWindow != null && fromToken.startingSurface != null) {
            this.mService.mSkipAppTransitionAnimation = true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Moving existing starting ");
            stringBuilder.append(tStartingWindow);
            stringBuilder.append(" from ");
            stringBuilder.append(fromToken);
            stringBuilder.append(" to ");
            stringBuilder.append(this);
            Flog.i(301, stringBuilder.toString());
            long origId = Binder.clearCallingIdentity();
            try {
                this.startingData = fromToken.startingData;
                this.startingSurface = fromToken.startingSurface;
                this.startingDisplayed = fromToken.startingDisplayed;
                fromToken.startingDisplayed = false;
                this.startingWindow = tStartingWindow;
                this.reportedVisible = fromToken.reportedVisible;
                fromToken.startingData = null;
                fromToken.startingSurface = null;
                fromToken.startingWindow = null;
                fromToken.startingMoved = true;
                tStartingWindow.mToken = this;
                tStartingWindow.mAppToken = this;
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Removing starting ");
                stringBuilder2.append(tStartingWindow);
                stringBuilder2.append(" from ");
                stringBuilder2.append(fromToken);
                Slog.v(str, stringBuilder2.toString());
                fromToken.removeChild(tStartingWindow);
                fromToken.postWindowRemoveStartingWindowCleanup(tStartingWindow);
                fromToken.mHiddenSetFromTransferredStartingWindow = false;
                addWindow(tStartingWindow);
                if (fromToken.allDrawn) {
                    this.allDrawn = true;
                    this.deferClearAllDrawn = fromToken.deferClearAllDrawn;
                }
                if (fromToken.firstWindowDrawn) {
                    this.firstWindowDrawn = true;
                }
                if (!fromToken.isHidden()) {
                    setHidden(false);
                    this.hiddenRequested = false;
                    this.mHiddenSetFromTransferredStartingWindow = true;
                }
                setClientHidden(fromToken.mClientHidden);
                transferAnimation(fromToken);
                this.mService.mOpeningApps.remove(this);
                this.mService.updateFocusedWindowLocked(3, true);
                getDisplayContent().setLayoutNeeded();
                this.mService.mWindowPlacerLocked.performSurfacePlacement();
                return true;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else if (fromToken.startingData == null) {
            return false;
        } else {
            this.startingData = fromToken.startingData;
            fromToken.startingData = null;
            fromToken.startingMoved = true;
            if (getController() != null) {
                getController().scheduleAddStartingWindow();
            }
            return true;
        }
    }

    boolean isLastWindow(WindowState win) {
        return this.mChildren.size() == 1 && this.mChildren.get(0) == win;
    }

    void onAppTransitionDone() {
        this.sendingToBottom = false;
    }

    int getOrientation(int candidate) {
        if (this.mService.checkAppOrientationForForceRotation(this)) {
            return -1;
        }
        if (candidate == 3) {
            return this.mOrientation;
        }
        if (this.sendingToBottom || this.mService.mClosingApps.contains(this) || (!isVisible() && !this.mService.mOpeningApps.contains(this))) {
            return -2;
        }
        return this.mOrientation;
    }

    int getOrientationIgnoreVisibility() {
        return this.mOrientation;
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        int prevWinMode = getWindowingMode();
        super.onConfigurationChanged(newParentConfig);
        int winMode = getWindowingMode();
        if (prevWinMode != winMode) {
            if (prevWinMode != 0 && winMode == 2) {
                this.mDisplayContent.mPinnedStackControllerLocked.resetReentrySnapFraction(this);
            } else if (!(prevWinMode != 2 || winMode == 0 || isHidden())) {
                TaskStack pinnedStack = this.mDisplayContent.getPinnedStack();
                if (pinnedStack != null) {
                    Rect stackBounds;
                    if (pinnedStack.lastAnimatingBoundsWasToFullscreen()) {
                        stackBounds = pinnedStack.mPreAnimationBounds;
                    } else {
                        stackBounds = this.mTmpRect;
                        pinnedStack.getBounds(stackBounds);
                    }
                    this.mDisplayContent.mPinnedStackControllerLocked.saveReentrySnapFraction(this, stackBounds);
                }
            }
        }
    }

    void checkAppWindowsReadyToShow() {
        if (this.allDrawn != this.mLastAllDrawn) {
            this.mLastAllDrawn = this.allDrawn;
            if (this.allDrawn) {
                if (this.mFreezingScreen) {
                    showAllWindowsLocked();
                    stopFreezingScreen(false, true);
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Setting mOrientationChangeComplete=true because wtoken ");
                        stringBuilder.append(this);
                        stringBuilder.append(" numInteresting=");
                        stringBuilder.append(this.mNumInterestingWindows);
                        stringBuilder.append(" numDrawn=");
                        stringBuilder.append(this.mNumDrawnWindows);
                        Slog.i(str, stringBuilder.toString());
                    }
                    setAppLayoutChanges(4, "checkAppWindowsReadyToShow: freezingScreen");
                } else {
                    setAppLayoutChanges(8, "checkAppWindowsReadyToShow");
                    if (!this.mService.mOpeningApps.contains(this)) {
                        showAllWindowsLocked();
                    }
                }
            }
        }
    }

    private boolean allDrawnStatesConsidered() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState child = (WindowState) this.mChildren.get(i);
            if (child.mightAffectAllDrawn() && !child.getDrawnStateEvaluated()) {
                return false;
            }
        }
        return true;
    }

    void updateAllDrawn() {
        String str = TAG_VISIBILITY;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateAllDrawn allDrawn=");
        stringBuilder.append(this.allDrawn);
        stringBuilder.append(" interesting=");
        stringBuilder.append(this.mNumInterestingWindows);
        stringBuilder.append(" drawn=");
        stringBuilder.append(this.mNumDrawnWindows);
        stringBuilder.append(" relaunchCount=");
        stringBuilder.append(this.mPendingRelaunchCount);
        stringBuilder.append(" for ");
        stringBuilder.append(this);
        Slog.v(str, stringBuilder.toString());
        if (!this.allDrawn) {
            int numInteresting = this.mNumInterestingWindows;
            if (numInteresting > 0 && allDrawnStatesConsidered() && this.mNumDrawnWindows >= numInteresting && !isRelaunching()) {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("allDrawn: ");
                    stringBuilder2.append(this);
                    stringBuilder2.append(" interesting=");
                    stringBuilder2.append(numInteresting);
                    stringBuilder2.append(" drawn=");
                    stringBuilder2.append(this.mNumDrawnWindows);
                    Slog.v(str2, stringBuilder2.toString());
                }
                this.allDrawn = true;
                if (this.mDisplayContent != null) {
                    this.mDisplayContent.setLayoutNeeded();
                }
                this.mService.mH.obtainMessage(32, this.token).sendToTarget();
                TaskStack pinnedStack = this.mDisplayContent.getPinnedStack();
                if (pinnedStack != null) {
                    pinnedStack.onAllWindowsDrawn();
                }
            }
        }
    }

    boolean updateDrawnWindowStates(WindowState w) {
        w.setDrawnStateEvaluated(true);
        if (this.allDrawn && !this.mFreezingScreen) {
            return false;
        }
        if (this.mLastTransactionSequence != ((long) this.mService.mTransactionSequence)) {
            this.mLastTransactionSequence = (long) this.mService.mTransactionSequence;
            this.mNumDrawnWindows = 0;
            this.startingDisplayed = false;
            this.mNumInterestingWindows = findMainWindow(false) != null ? 1 : 0;
        }
        WindowStateAnimator winAnimator = w.mWinAnimator;
        boolean isInterestingAndDrawn = false;
        if (!this.allDrawn && w.mightAffectAllDrawn()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Eval win ");
                stringBuilder.append(w);
                stringBuilder.append(": isDrawn=");
                stringBuilder.append(w.isDrawnLw());
                stringBuilder.append(", isAnimationSet=");
                stringBuilder.append(isSelfAnimating());
                Slog.v(str, stringBuilder.toString());
                if (!w.isDrawnLw()) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Not displayed: s=");
                    stringBuilder.append(winAnimator.mSurfaceController);
                    stringBuilder.append(" pv=");
                    stringBuilder.append(w.mPolicyVisibility);
                    stringBuilder.append(" mDrawState=");
                    stringBuilder.append(winAnimator.drawStateToString());
                    stringBuilder.append(" ph=");
                    stringBuilder.append(w.isParentWindowHidden());
                    stringBuilder.append(" th=");
                    stringBuilder.append(this.hiddenRequested);
                    stringBuilder.append(" a=");
                    stringBuilder.append(isSelfAnimating());
                    Slog.v(str, stringBuilder.toString());
                }
            }
            if (w != this.startingWindow) {
                if (w.isInteresting()) {
                    if (findMainWindow(false) != w) {
                        this.mNumInterestingWindows++;
                    }
                    if (w.isDrawnLw()) {
                        this.mNumDrawnWindows++;
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("tokenMayBeDrawn: ");
                            stringBuilder2.append(this);
                            stringBuilder2.append(" w=");
                            stringBuilder2.append(w);
                            stringBuilder2.append(" numInteresting=");
                            stringBuilder2.append(this.mNumInterestingWindows);
                            stringBuilder2.append(" freezingScreen=");
                            stringBuilder2.append(this.mFreezingScreen);
                            stringBuilder2.append(" mAppFreezing=");
                            stringBuilder2.append(w.mAppFreezing);
                            Slog.v(str2, stringBuilder2.toString());
                        }
                        isInterestingAndDrawn = true;
                    }
                }
            } else if (w.isDrawnLw()) {
                if (getController() != null) {
                    getController().reportStartingWindowDrawn();
                }
                this.startingDisplayed = true;
            }
        }
        return isInterestingAndDrawn;
    }

    void layoutLetterbox(WindowState winHint) {
        WindowState w = findMainWindow();
        if (w != null && (winHint == null || w == winHint)) {
            boolean needsLetterbox = true;
            boolean surfaceReady = w.isDrawnLw() || w.mWinAnimator.mSurfaceDestroyDeferred || w.isDragResizeChanged();
            if (!(w.isLetterboxedAppWindow() && fillsParent() && surfaceReady)) {
                needsLetterbox = false;
            }
            if (needsLetterbox) {
                if (this.mLetterbox == null) {
                    this.mLetterbox = new Letterbox(new -$$Lambda$AppWindowToken$clD7LvtE6cPZl3BRlaGuoR17rP4(this));
                }
                this.mLetterbox.layout(getParent().getBounds(), w.mFrame);
            } else if (this.mLetterbox != null) {
                this.mLetterbox.hide();
            }
        }
    }

    void updateLetterboxSurface(WindowState winHint) {
        WindowState w = findMainWindow();
        if (w == winHint || winHint == null || w == null) {
            layoutLetterbox(winHint);
            if (this.mLetterbox != null && this.mLetterbox.needsApplySurfaceChanges()) {
                this.mLetterbox.applySurfaceChanges(this.mPendingTransaction);
            }
        }
    }

    boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        if (!this.mIsExiting || waitingForReplacement()) {
            return forAllWindowsUnchecked(callback, traverseTopToBottom);
        }
        return false;
    }

    boolean forAllWindowsUnchecked(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        return super.forAllWindows((ToBooleanFunction) callback, traverseTopToBottom);
    }

    AppWindowToken asAppWindowToken() {
        return this;
    }

    boolean fillsParent() {
        return this.mFillsParent;
    }

    void setFillsParent(boolean fillsParent) {
        this.mFillsParent = fillsParent;
    }

    boolean containsDismissKeyguardWindow() {
        if (isRelaunching()) {
            return this.mLastContainsDismissKeyguardWindow;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if ((((WindowState) this.mChildren.get(i)).mAttrs.flags & DumpState.DUMP_CHANGES) != 0) {
                return true;
            }
        }
        return false;
    }

    boolean containsShowWhenLockedWindow() {
        if (isRelaunching()) {
            return this.mLastContainsShowWhenLockedWindow;
        }
        if (this.mChildren.size() == 0 && this.mService.mPolicy.isKeyguardOccluded()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Keyguard is occluded and there is no window in ");
            stringBuilder.append(this);
            Slog.w(str, stringBuilder.toString());
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if ((((WindowState) this.mChildren.get(i)).mAttrs.flags & DumpState.DUMP_FROZEN) != 0) {
                return true;
            }
        }
        return false;
    }

    void checkKeyguardFlagsChanged() {
        boolean containsDismissKeyguard = containsDismissKeyguardWindow();
        boolean containsShowWhenLocked = containsShowWhenLockedWindow();
        if (!(containsDismissKeyguard == this.mLastContainsDismissKeyguardWindow && containsShowWhenLocked == this.mLastContainsShowWhenLockedWindow)) {
            this.mService.notifyKeyguardFlagsChanged(null);
        }
        this.mLastContainsDismissKeyguardWindow = containsDismissKeyguard;
        this.mLastContainsShowWhenLockedWindow = containsShowWhenLocked;
    }

    WindowState getImeTargetBelowWindow(WindowState w) {
        int index = this.mChildren.indexOf(w);
        if (index > 0) {
            WindowState target = (WindowState) this.mChildren.get(index - 1);
            if (target.canBeImeTarget()) {
                return target;
            }
        }
        return null;
    }

    int getLowestAnimLayer() {
        for (int i = 0; i < this.mChildren.size(); i++) {
            WindowState w = (WindowState) this.mChildren.get(i);
            if (!w.mRemoved) {
                return w.mWinAnimator.mAnimLayer;
            }
        }
        return HwBootFail.STAGE_BOOT_SUCCESS;
    }

    WindowState getHighestAnimLayerWindow(WindowState currentTarget) {
        WindowState candidate = null;
        for (int i = this.mChildren.indexOf(currentTarget); i >= 0; i--) {
            WindowState w = (WindowState) this.mChildren.get(i);
            if (!w.mRemoved && (candidate == null || w.mWinAnimator.mAnimLayer > candidate.mWinAnimator.mAnimLayer)) {
                candidate = w;
            }
        }
        return candidate;
    }

    void setDisablePreviewScreenshots(boolean disable) {
        this.mDisablePreviewScreenshots = disable;
    }

    void setCanTurnScreenOn(boolean canTurnScreenOn) {
        this.mCanTurnScreenOn = canTurnScreenOn;
    }

    boolean canTurnScreenOn() {
        return this.mCanTurnScreenOn;
    }

    static /* synthetic */ boolean lambda$shouldUseAppThemeSnapshot$1(WindowState w) {
        return (w.mAttrs.flags & 8192) != 0;
    }

    boolean shouldUseAppThemeSnapshot() {
        return this.mDisablePreviewScreenshots || forAllWindows(-$$Lambda$AppWindowToken$ErIvy8Kb9OulX2W0_mr0NNBS-KE.INSTANCE, true);
    }

    SurfaceControl getAppAnimationLayer() {
        int i;
        if (isActivityTypeHome()) {
            i = 2;
        } else if (needsZBoost()) {
            i = 1;
        } else {
            i = 0;
        }
        return getAppAnimationLayer(i);
    }

    public SurfaceControl getAnimationLeashParent() {
        if (inPinnedWindowingMode()) {
            return getStack().getSurfaceControl();
        }
        return getAppAnimationLayer();
    }

    private boolean shouldAnimate(int transit) {
        boolean isSplitScreenPrimary = getWindowingMode() == 3;
        boolean allowSplitScreenPrimaryAnimation = transit != 13;
        if (!isSplitScreenPrimary || allowSplitScreenPrimaryAnimation) {
            return true;
        }
        return false;
    }

    boolean applyAnimationLocked(LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction) {
        int i = transit;
        if (this.mService.mDisableTransitionAnimation || !shouldAnimate(i)) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("applyAnimation: transition animation is disabled or skipped. atoken=");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            cancelAnimation();
            return false;
        }
        Trace.traceBegin(32, "AWT#applyAnimationLocked");
        if (okToAnimate()) {
            AnimationAdapter adapter;
            TaskStack stack = getStack();
            this.mTmpPoint.set(0, 0);
            this.mTmpRect.setEmpty();
            if (stack != null) {
                stack.getRelativePosition(this.mTmpPoint);
                stack.getBounds(this.mTmpRect);
                if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getTask().getDisplayContent().mDisplayId)) {
                    WindowState win = findMainWindow();
                    if (win != null) {
                        this.mTmpRect.set(win.mFrame);
                        this.mTmpPoint.set(this.mTmpRect.left, this.mTmpRect.top);
                    }
                }
                if (this.mService.getLazyMode() != 0 && inMultiWindowMode()) {
                    this.mTmpPoint.set(stack.mLastSurfacePosition.x, stack.mLastSurfacePosition.y);
                }
                this.mTmpRect.offsetTo(0, 0);
            }
            if (this.mService.mAppTransition.getRemoteAnimationController() == null || this.mSurfaceAnimator.isAnimationStartDelayed()) {
                Animation a = loadAnimation(lp, transit, enter, isVoiceInteraction);
                if (a != null) {
                    AnimationAdapter adapter2 = new LocalAnimationAdapter(new WindowAnimationSpec(a, this.mTmpPoint, this.mTmpRect, this.mService.mAppTransition.canSkipFirstFrame(), this.mService.mAppTransition.getAppStackClipMode(), true), this.mService.mSurfaceAnimationRunner);
                    if (a.getZAdjustment() == 1) {
                        this.mNeedsZBoost = true;
                    }
                    this.mTransit = i;
                    this.mTransitFlags = this.mService.mAppTransition.getTransitFlags();
                    adapter = adapter2;
                } else {
                    adapter = null;
                }
            } else {
                adapter = this.mService.mAppTransition.getRemoteAnimationController().createAnimationAdapter(this, this.mTmpPoint, this.mTmpRect);
            }
            if (adapter != null) {
                startAnimation(getPendingTransaction(), adapter, true ^ isVisible());
                if (adapter.getShowWallpaper()) {
                    DisplayContent displayContent = this.mDisplayContent;
                    displayContent.pendingLayoutChanges |= 4;
                }
            }
        } else {
            cancelAnimation();
        }
        Trace.traceEnd(32);
        return isReallyAnimating();
    }

    private Animation loadAnimation(LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction) {
        int i = transit;
        DisplayContent displayContent = getTask().getDisplayContent();
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int width = displayInfo.appWidth;
        int height = displayInfo.appHeight;
        if (!this.mService.isDisplayOkForAnimation(width, height, i, this)) {
            return null;
        }
        String str;
        StringBuilder stringBuilder;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("applyAnimation: atoken=");
            stringBuilder.append(this);
            Slog.v(str, stringBuilder.toString());
        }
        WindowState win = findMainWindow();
        boolean z = false;
        Rect frame = new Rect(0, 0, width, height);
        Rect displayFrame = new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
        Rect insets = new Rect();
        Rect stableInsets = new Rect();
        Rect surfaceInsets = null;
        if (win != null && win.inFreeformWindowingMode()) {
            z = true;
        }
        boolean freeform = z;
        if (win != null) {
            if (freeform || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayContent.mDisplayId))) {
                frame.set(win.mFrame);
            } else if (win.isLetterboxedAppWindow()) {
                frame.set(getTask().getBounds());
            } else if (win.isDockedResizing()) {
                frame.set(getTask().getParent().getBounds());
            } else {
                frame.set(win.mContainingFrame);
            }
            surfaceInsets = win.getAttrs().surfaceInsets;
            insets.set(win.mContentInsets);
            stableInsets.set(win.mStableInsets);
        }
        Rect surfaceInsets2 = surfaceInsets;
        boolean enter2 = this.mLaunchTaskBehind ? false : enter;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Loading animation for app transition. transit=");
            stringBuilder.append(AppTransition.appTransitionToString(transit));
            stringBuilder.append(" enter=");
            stringBuilder.append(enter2);
            stringBuilder.append(" frame=");
            stringBuilder.append(frame);
            stringBuilder.append(" insets=");
            stringBuilder.append(insets);
            stringBuilder.append(" surfaceInsets=");
            stringBuilder.append(surfaceInsets2);
            Slog.d(str, stringBuilder.toString());
        }
        Configuration displayConfig = displayContent.getConfiguration();
        boolean enter3 = enter2;
        Rect surfaceInsets3 = surfaceInsets2;
        Rect frame2 = frame;
        WindowState win2 = win;
        int height2 = height;
        int width2 = width;
        DisplayContent displayContent2 = displayContent;
        Animation a = this.mService.mAppTransition.loadAnimation(lp, i, enter2, displayConfig.uiMode, displayConfig.orientation, frame, displayFrame, insets, surfaceInsets3, stableInsets, isVoiceInteraction, freeform, getTask().mTaskId);
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayContent2.mDisplayId)) {
            a = this.mService.mAppTransition.loadAnimationRes(lp, enter3 ? 17432576 : 17432577);
            a.setDuration(300);
        } else {
            LayoutParams layoutParams = lp;
        }
        Rect frame3 = frame2;
        a = tryToOverrideAppExitToLancherAnimation(a, transit, frame3);
        if (a != null) {
            int containingWidth = frame3.width();
            int containingHeight = frame3.height();
            if (this.mService.getLazyMode() != 0) {
                float f = (float) containingWidth;
                WindowManagerService windowManagerService = this.mService;
                containingWidth = (int) (f * 0.75f);
                f = (float) containingHeight;
                WindowManagerService windowManagerService2 = this.mService;
                containingHeight = (int) (f * 0.75f);
            }
            a.initialize(containingWidth, containingHeight, width2, height2);
            if (win2 != null) {
                win = win2;
                if (win.toString().contains("com.android.contacts.activities.PeopleActivity") || win.toString().contains("com.android.contacts.activities.DialtactsActivity")) {
                    a.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked() * 0.7f);
                }
            }
            a.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked());
        } else {
            int i2 = height2;
            int i3 = width2;
        }
        return a;
    }

    public boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
        return this.mAnimatingAppWindowTokenRegistry != null && this.mAnimatingAppWindowTokenRegistry.notifyAboutToFinish(this, endDeferFinishCallback);
    }

    public void onAnimationLeashDestroyed(Transaction t) {
        super.onAnimationLeashDestroyed(t);
        if (this.mAnimatingAppWindowTokenRegistry != null) {
            this.mAnimatingAppWindowTokenRegistry.notifyFinished(this);
        }
    }

    protected void setLayer(Transaction t, int layer) {
        if (!this.mSurfaceAnimator.hasLeash()) {
            t.setLayer(this.mSurfaceControl, layer);
        }
    }

    protected void setRelativeLayer(Transaction t, SurfaceControl relativeTo, int layer) {
        if (!this.mSurfaceAnimator.hasLeash()) {
            t.setRelativeLayer(this.mSurfaceControl, relativeTo, layer);
        }
    }

    protected void reparentSurfaceControl(Transaction t, SurfaceControl newParent) {
        if (!this.mSurfaceAnimator.hasLeash()) {
            t.reparent(this.mSurfaceControl, newParent.getHandle());
        }
    }

    public void onAnimationLeashCreated(Transaction t, SurfaceControl leash) {
        int layer;
        if (inPinnedWindowingMode()) {
            layer = getParent().getPrefixOrderIndex();
        } else {
            layer = getPrefixOrderIndex();
        }
        if (this.mNeedsZBoost) {
            layer += Z_BOOST_BASE;
        }
        leash.setLayer(layer);
        getDisplayContent().assignStackOrdering();
        if (this.mAnimatingAppWindowTokenRegistry != null) {
            this.mAnimatingAppWindowTokenRegistry.notifyStarting(this);
        }
    }

    void showAllWindowsLocked() {
        forAllWindows((Consumer) -$$Lambda$AppWindowToken$jSO6pNpAHzC89v5XTI_Oj39kDGg.INSTANCE, false);
    }

    static /* synthetic */ void lambda$showAllWindowsLocked$2(WindowState windowState) {
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("performing show on: ");
            stringBuilder.append(windowState);
            Slog.v(str, stringBuilder.toString());
        }
        windowState.performShowLocked();
    }

    protected void onAnimationFinished() {
        super.onAnimationFinished();
        this.mTransit = -1;
        boolean z = false;
        this.mTransitFlags = 0;
        this.mNeedsZBoost = false;
        setAppLayoutChanges(12, "AppWindowToken");
        clearThumbnail();
        if (isHidden() && this.hiddenRequested) {
            z = true;
        }
        setClientHidden(z);
        if (this.mService.mInputMethodTarget != null && this.mService.mInputMethodTarget.mAppToken == this) {
            getDisplayContent().computeImeTarget(true);
        }
        new ArrayList(this.mChildren).forEach(-$$Lambda$01bPtngJg5AqEoOWfW3rWfV7MH4.INSTANCE);
        this.mService.mAppTransition.notifyAppTransitionFinishedLocked(this.token);
        scheduleAnimation();
    }

    boolean isAppAnimating() {
        return isSelfAnimating();
    }

    boolean isSelfAnimating() {
        return isWaitingForTransitionStart() || isReallyAnimating();
    }

    private boolean isReallyAnimating() {
        return super.isSelfAnimating();
    }

    void cancelAnimation() {
        super.cancelAnimation();
        clearThumbnail();
    }

    boolean isWaitingForTransitionStart() {
        return this.mService.mAppTransition.isTransitionSet() && (this.mService.mOpeningApps.contains(this) || this.mService.mClosingApps.contains(this));
    }

    public int getTransit() {
        return this.mTransit;
    }

    int getTransitFlags() {
        return this.mTransitFlags;
    }

    void attachThumbnailAnimation() {
        if (isReallyAnimating()) {
            int taskId = getTask().mTaskId;
            GraphicBuffer thumbnailHeader = this.mService.mAppTransition.getAppTransitionThumbnailHeader(taskId);
            if (thumbnailHeader == null) {
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No thumbnail header bitmap for: ");
                    stringBuilder.append(taskId);
                    Slog.d(str, stringBuilder.toString());
                }
                return;
            }
            clearThumbnail();
            this.mThumbnail = new AppWindowThumbnail(getPendingTransaction(), this, thumbnailHeader);
            this.mThumbnail.startAnimation(getPendingTransaction(), loadThumbnailAnimation(thumbnailHeader));
        }
    }

    void attachCrossProfileAppsThumbnailAnimation() {
        if (isReallyAnimating()) {
            clearThumbnail();
            WindowState win = findMainWindow();
            if (win != null) {
                int thumbnailDrawableRes;
                Rect frame = win.mFrame;
                if (getTask().mUserId == this.mService.mCurrentUserId) {
                    thumbnailDrawableRes = 17302260;
                } else {
                    thumbnailDrawableRes = 17302337;
                }
                GraphicBuffer thumbnail = this.mService.mAppTransition.createCrossProfileAppsThumbnail(thumbnailDrawableRes, frame);
                if (thumbnail != null) {
                    this.mThumbnail = new AppWindowThumbnail(getPendingTransaction(), this, thumbnail);
                    this.mThumbnail.startAnimation(getPendingTransaction(), this.mService.mAppTransition.createCrossProfileAppsThumbnailAnimationLocked(win.mFrame), new Point(frame.left, frame.top));
                }
            }
        }
    }

    private Animation loadThumbnailAnimation(GraphicBuffer thumbnailHeader) {
        Rect contentFrameLw;
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        WindowState win = findMainWindow();
        if (win != null) {
            contentFrameLw = win.getContentFrameLw();
        } else {
            contentFrameLw = new Rect(0, 0, displayInfo.appWidth, displayInfo.appHeight);
        }
        Rect appRect = contentFrameLw;
        Rect insets = win != null ? win.mContentInsets : null;
        Configuration displayConfig = this.mDisplayContent.getConfiguration();
        return this.mService.mAppTransition.createThumbnailAspectScaleAnimationLocked(appRect, insets, thumbnailHeader, getTask().mTaskId, displayConfig.uiMode, displayConfig.orientation);
    }

    private void clearThumbnail() {
        if (this.mThumbnail != null) {
            this.mThumbnail.destroy();
            this.mThumbnail = null;
        }
    }

    void registerRemoteAnimations(RemoteAnimationDefinition definition) {
        this.mRemoteAnimationDefinition = definition;
    }

    RemoteAnimationDefinition getRemoteAnimationDefinition() {
        return this.mRemoteAnimationDefinition;
    }

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        StringBuilder stringBuilder;
        String stringBuilder2;
        super.dump(pw, prefix, dumpAll);
        if (this.appToken != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("app=true mVoiceInteraction=");
            stringBuilder.append(this.mVoiceInteraction);
            pw.println(stringBuilder.toString());
        }
        pw.print(prefix);
        pw.print("task=");
        pw.println(getTask());
        pw.print(prefix);
        pw.print(" mFillsParent=");
        pw.print(this.mFillsParent);
        pw.print(" mOrientation=");
        pw.println(this.mOrientation);
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("hiddenRequested=");
        stringBuilder.append(this.hiddenRequested);
        stringBuilder.append(" mClientHidden=");
        stringBuilder.append(this.mClientHidden);
        if (this.mDeferHidingClient) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" mDeferHidingClient=");
            stringBuilder3.append(this.mDeferHidingClient);
            stringBuilder2 = stringBuilder3.toString();
        } else {
            stringBuilder2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        stringBuilder.append(stringBuilder2);
        stringBuilder.append(" reportedDrawn=");
        stringBuilder.append(this.reportedDrawn);
        stringBuilder.append(" reportedVisible=");
        stringBuilder.append(this.reportedVisible);
        pw.println(stringBuilder.toString());
        if (this.paused) {
            pw.print(prefix);
            pw.print("paused=");
            pw.println(this.paused);
        }
        if (this.mAppStopped) {
            pw.print(prefix);
            pw.print("mAppStopped=");
            pw.println(this.mAppStopped);
        }
        if (this.mNumInterestingWindows != 0 || this.mNumDrawnWindows != 0 || this.allDrawn || this.mLastAllDrawn) {
            pw.print(prefix);
            pw.print("mNumInterestingWindows=");
            pw.print(this.mNumInterestingWindows);
            pw.print(" mNumDrawnWindows=");
            pw.print(this.mNumDrawnWindows);
            pw.print(" inPendingTransaction=");
            pw.print(this.inPendingTransaction);
            pw.print(" allDrawn=");
            pw.print(this.allDrawn);
            pw.print(" lastAllDrawn=");
            pw.print(this.mLastAllDrawn);
            pw.println(")");
        }
        if (this.inPendingTransaction) {
            pw.print(prefix);
            pw.print("inPendingTransaction=");
            pw.println(this.inPendingTransaction);
        }
        if (this.startingData != null || this.removed || this.firstWindowDrawn || this.mIsExiting) {
            pw.print(prefix);
            pw.print("startingData=");
            pw.print(this.startingData);
            pw.print(" removed=");
            pw.print(this.removed);
            pw.print(" firstWindowDrawn=");
            pw.print(this.firstWindowDrawn);
            pw.print(" mIsExiting=");
            pw.println(this.mIsExiting);
        }
        if (this.startingWindow != null || this.startingSurface != null || this.startingDisplayed || this.startingMoved || this.mHiddenSetFromTransferredStartingWindow) {
            pw.print(prefix);
            pw.print("startingWindow=");
            pw.print(this.startingWindow);
            pw.print(" startingSurface=");
            pw.print(this.startingSurface);
            pw.print(" startingDisplayed=");
            pw.print(this.startingDisplayed);
            pw.print(" startingMoved=");
            pw.print(this.startingMoved);
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mHiddenSetFromTransferredStartingWindow=");
            stringBuilder.append(this.mHiddenSetFromTransferredStartingWindow);
            pw.println(stringBuilder.toString());
        }
        if (!this.mFrozenBounds.isEmpty()) {
            pw.print(prefix);
            pw.print("mFrozenBounds=");
            pw.println(this.mFrozenBounds);
            pw.print(prefix);
            pw.print("mFrozenMergedConfig=");
            pw.println(this.mFrozenMergedConfig);
        }
        if (this.mPendingRelaunchCount != 0) {
            pw.print(prefix);
            pw.print("mPendingRelaunchCount=");
            pw.println(this.mPendingRelaunchCount);
        }
        if (getController() != null) {
            pw.print(prefix);
            pw.print("controller=");
            pw.println(getController());
        }
        if (this.mRemovingFromDisplay) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mRemovingFromDisplay=");
            stringBuilder.append(this.mRemovingFromDisplay);
            pw.println(stringBuilder.toString());
        }
    }

    void setHidden(boolean hidden) {
        super.setHidden(hidden);
        if (hidden) {
            this.mDisplayContent.mPinnedStackControllerLocked.resetReentrySnapFraction(this);
        }
        scheduleAnimation();
    }

    void prepareSurfaces() {
        boolean show = !isHidden() || super.isSelfAnimating();
        if (show && !this.mLastSurfaceShowing) {
            this.mPendingTransaction.show(this.mSurfaceControl);
        } else if (!show && this.mLastSurfaceShowing) {
            this.mPendingTransaction.hide(this.mSurfaceControl);
        }
        if (this.mThumbnail != null) {
            this.mThumbnail.setShowing(this.mPendingTransaction, show);
        }
        this.mLastSurfaceShowing = show;
        super.prepareSurfaces();
    }

    boolean isSurfaceShowing() {
        return this.mLastSurfaceShowing;
    }

    boolean isFreezingScreen() {
        return this.mFreezingScreen;
    }

    boolean needsZBoost() {
        return this.mNeedsZBoost || super.needsZBoost();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        long token = proto.start(fieldId);
        writeNameToProto(proto, 1138166333441L);
        super.writeToProto(proto, 1146756268034L, trim);
        proto.write(1133871366147L, this.mLastSurfaceShowing);
        proto.write(1133871366148L, isWaitingForTransitionStart());
        proto.write(1133871366149L, isReallyAnimating());
        if (this.mThumbnail != null) {
            this.mThumbnail.writeToProto(proto, 1146756268038L);
        }
        proto.write(1133871366151L, this.mFillsParent);
        proto.write(1133871366152L, this.mAppStopped);
        proto.write(1133871366153L, this.hiddenRequested);
        proto.write(1133871366154L, this.mClientHidden);
        proto.write(1133871366155L, this.mDeferHidingClient);
        proto.write(1133871366156L, this.reportedDrawn);
        proto.write(1133871366157L, this.reportedVisible);
        proto.write(1120986464270L, this.mNumInterestingWindows);
        proto.write(1120986464271L, this.mNumDrawnWindows);
        proto.write(1133871366160L, this.allDrawn);
        proto.write(1133871366161L, this.mLastAllDrawn);
        proto.write(1133871366162L, this.removed);
        if (this.startingWindow != null) {
            this.startingWindow.writeIdentifierToProto(proto, 1146756268051L);
        }
        proto.write(1133871366164L, this.startingDisplayed);
        proto.write(1133871366165L, this.startingMoved);
        proto.write(1133871366166L, this.mHiddenSetFromTransferredStartingWindow);
        Iterator it = this.mFrozenBounds.iterator();
        while (it.hasNext()) {
            ((Rect) it.next()).writeToProto(proto, 2246267895831L);
        }
        proto.end(token);
    }

    void writeNameToProto(ProtoOutputStream proto, long fieldId) {
        if (this.appToken != null) {
            try {
                proto.write(fieldId, this.appToken.getName());
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }
    }

    public String toString() {
        StringBuilder sb;
        if (this.stringName == null) {
            sb = new StringBuilder();
            sb.append("AppWindowToken{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" token=");
            sb.append(this.token);
            sb.append('}');
            this.stringName = sb.toString();
        }
        sb = new StringBuilder();
        sb.append(this.stringName);
        sb.append(this.mIsExiting ? " mIsExiting=" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return sb.toString();
    }

    Rect getLetterboxInsets() {
        if (this.mLetterbox != null) {
            return this.mLetterbox.getInsets();
        }
        return new Rect();
    }

    boolean isLetterboxOverlappingWith(Rect rect) {
        return this.mLetterbox != null && this.mLetterbox.isOverlappingWith(rect);
    }

    void setWillCloseOrEnterPip(boolean willCloseOrEnterPip) {
        this.mWillCloseOrEnterPip = willCloseOrEnterPip;
    }

    boolean isClosingOrEnteringPip() {
        return (isAnimating() && this.hiddenRequested) || this.mWillCloseOrEnterPip;
    }

    private boolean isAppExitToLauncher(AppWindowToken atoken, int transit) {
        if (atoken == null) {
            Slog.w(TAG, "app exit to launcher find no app window token!");
            return false;
        }
        WindowState window = atoken.findMainWindow();
        AppWindowToken topOpeningApp = this.mService.getTopOpeningApp();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("is app exit to launcher info: transit = ");
        stringBuilder.append(transit);
        stringBuilder.append(", app = ");
        stringBuilder.append(atoken);
        stringBuilder.append(", window = ");
        stringBuilder.append(window);
        stringBuilder.append("mClosingApps = ");
        stringBuilder.append(this.mService.mClosingApps);
        stringBuilder.append(", topOpeningApp = ");
        stringBuilder.append(topOpeningApp);
        stringBuilder.append(", mExitIconBitmap = ");
        stringBuilder.append(this.mService.mExitIconBitmap);
        stringBuilder.append(", mExitIconHeight = ");
        stringBuilder.append(this.mService.mExitIconHeight);
        stringBuilder.append(", mExitIconWidth = ");
        stringBuilder.append(this.mService.mExitIconWidth);
        Flog.i(310, stringBuilder.toString());
        if (!(window == null || transit != 13 || this.mService.mClosingApps == null || !this.mService.mClosingApps.contains(atoken) || atoken.toString().contains("com.android.stk/.StkDialogActivity") || topOpeningApp == null || this.mService.mExitIconBitmap == null || this.mService.mExitIconHeight <= 0 || this.mService.mExitIconWidth <= 0)) {
            boolean isOpeningUniLauncherCmpName = topOpeningApp.toString().contains("com.huawei.android.launcher/.unihome.UniHomeLauncher");
            boolean isOpeningDrawerLauncherCmpName = topOpeningApp.toString().contains("com.huawei.android.launcher/.drawer.DrawerLauncher");
            boolean isOpeningNewSimpleLauncherCmpName = topOpeningApp.toString().contains("com.huawei.android.launcher/.newsimpleui.NewSimpleLauncher");
            if (isOpeningUniLauncherCmpName || isOpeningDrawerLauncherCmpName || isOpeningNewSimpleLauncherCmpName) {
                if (window.mAttrs == null || (window.mAttrs.flags & DumpState.DUMP_FROZEN) != DumpState.DUMP_FROZEN || (window.mAttrs.flags & DumpState.DUMP_CHANGES) != DumpState.DUMP_CHANGES) {
                    return true;
                }
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("app to launcher window flag = ");
                stringBuilder2.append(window.mAttrs.flags);
                Slog.d(str, stringBuilder2.toString());
                return false;
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:11:0x0037, code:
            if (r1.contains("com.huawei.android.launcher/.newsimpleui.NewSimpleLauncher") != false) goto L_0x0039;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isLauncherOpen(AppWindowToken atoken, int transit) {
        if (atoken == null) {
            Slog.w(TAG, "find no atoken when check is Launcher open");
            return false;
        }
        if (transit == 13) {
            String appWindowToken = atoken.toString();
            WindowManagerService windowManagerService = this.mService;
            if (!appWindowToken.contains("com.huawei.android.launcher/.unihome.UniHomeLauncher")) {
                appWindowToken = atoken.toString();
                windowManagerService = this.mService;
                if (!appWindowToken.contains("com.huawei.android.launcher/.drawer.DrawerLauncher")) {
                    appWindowToken = atoken.toString();
                    windowManagerService = this.mService;
                }
            }
            if (this.mService.mClosingApps != null && this.mService.mClosingApps.size() > 0 && this.mService.mOpeningApps != null && this.mService.mOpeningApps.contains(atoken)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.mService.mClosingApps);
                stringBuilder.append(" is closing and ");
                stringBuilder.append(this.mService.mOpeningApps);
                stringBuilder.append("is opening");
                Slog.i(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    private Animation tryToOverrideAppExitToLancherAnimation(Animation a, int transit, Rect frame) {
        WindowManagerService windowManagerService = this.mService;
        if (!WindowManagerService.HW_SUPPORT_LAUNCHER_EXIT_ANIM) {
            return a;
        }
        Animation appExitToIconAnimation;
        if (isAppExitToLauncher(this, transit) && frame != null) {
            appExitToIconAnimation = this.mService.mAppTransition.createAppExitToIconAnimation(this, frame.height(), this.mService.mExitIconWidth, this.mService.mExitIconHeight, this.mService.mExitPivotX, this.mService.mExitPivotY, this.mService.mExitIconBitmap);
            if (appExitToIconAnimation != null) {
                return appExitToIconAnimation;
            }
            return a;
        } else if (!isLauncherOpen(this, transit) || frame == null) {
            return a;
        } else {
            appExitToIconAnimation = this.mService.mAppTransition.createLauncherEnterAnimation(this, frame.height(), this.mService.mExitIconWidth, this.mService.mExitIconHeight, this.mService.mExitPivotX, this.mService.mExitPivotY);
            if (appExitToIconAnimation != null) {
                return appExitToIconAnimation;
            }
            return a;
        }
    }

    public int getHwGestureNavOptions() {
        return this.mHwGestureNavOptions;
    }
}
