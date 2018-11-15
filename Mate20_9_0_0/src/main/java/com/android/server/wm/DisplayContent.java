package com.android.server.wm;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.hardware.display.DisplayManagerInternal;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.InputDevice;
import android.view.MagnificationSpec;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceSession;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.view.IInputMethodClient;
import com.android.server.HwServiceFactory;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.power.IHwShutdownThread;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.utils.CoordinateTransforms;
import com.android.server.wm.utils.RotationCache;
import com.android.server.wm.utils.WmDisplayCutout;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DisplayContent extends AbsDisplayContent {
    private static final boolean IS_DEBUG_VERSION;
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", HealthServiceWrapper.INSTANCE_VENDOR));
    private static final int PAD_DISPLAY_ID = 100000;
    private static final String TAG = "WindowManager";
    private static final String TAG_VISIBILITY = "WindowManager_visibility";
    private static final String TAG_VR = "VRService";
    private static final String UNIQUE_ID_PREFIX_LOCAL = "local:";
    final int[] index = new int[1];
    boolean isDefaultDisplay;
    protected final AboveAppWindowContainers mAboveAppWindowsContainers = new AboveAppWindowContainers("mAboveAppWindowsContainers", this.mService);
    private boolean mAltOrientation = false;
    private final Consumer<WindowState> mApplyPostLayoutPolicy = new -$$Lambda$DisplayContent$JibsaX4YnJd0ta_wiDDdSp-PjQk(this);
    private final Consumer<WindowState> mApplySurfaceChangesTransaction = new -$$Lambda$DisplayContent$qxt4izS31fb0LF2uo_OF9DMa7gc(this);
    int mBaseDisplayDensity = 0;
    int mBaseDisplayHeight = 0;
    private Rect mBaseDisplayRect = new Rect();
    int mBaseDisplayWidth = 0;
    private final NonAppWindowContainers mBelowAppWindowsContainers = new NonAppWindowContainers("mBelowAppWindowsContainers", this.mService);
    private final DisplayMetrics mCompatDisplayMetrics = new DisplayMetrics();
    float mCompatibleScreenScale;
    private final Predicate<WindowState> mComputeImeTargetPredicate = new -$$Lambda$DisplayContent$TPj3OjTsuIg5GTLb5nMmFqIghA4(this);
    private int mDeferUpdateImeTargetCount;
    private boolean mDeferredRemoval;
    protected final Display mDisplay;
    private final RotationCache<DisplayCutout, WmDisplayCutout> mDisplayCutoutCache = new RotationCache(new -$$Lambda$DisplayContent$fiC19lMy-d_-rvza7hhOSw6bOM8(this));
    DisplayFrames mDisplayFrames;
    protected int mDisplayId = -1;
    protected final DisplayInfo mDisplayInfo = new DisplayInfo();
    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private boolean mDisplayReady = false;
    boolean mDisplayScalingDisabled;
    final DockedStackDividerController mDividerControllerLocked;
    final ArrayList<WindowToken> mExitingTokens = new ArrayList();
    private final ToBooleanFunction<WindowState> mFindFocusedWindow = new -$$Lambda$DisplayContent$7uZtakUXzuXqF_Qht5Uq7LUvubI(this);
    float mForceCompatibleScreenScale;
    private boolean mHaveApp = false;
    private boolean mHaveBootMsg = false;
    private boolean mHaveKeyguard = true;
    private boolean mHaveWallpaper = false;
    private final NonMagnifiableWindowContainers mImeWindowsContainers = new NonMagnifiableWindowContainers("mImeWindowsContainers", this.mService);
    DisplayCutout mInitialDisplayCutout;
    int mInitialDisplayDensity = 0;
    int mInitialDisplayHeight = 0;
    int mInitialDisplayWidth = 0;
    private int mLastKeyguardForcedOrientation = -1;
    private int mLastOrientation = -1;
    private boolean mLastWallpaperVisible = false;
    private int mLastWindowForcedOrientation = -1;
    private boolean mLayoutNeeded;
    int mLayoutSeq = 0;
    private MagnificationSpec mMagnificationSpec;
    private int mMaxUiWidth;
    WindowToken mObserveToken = null;
    WindowState mObserveWin = null;
    String mObserveWinTitle = "FingerprintDialogView";
    private SurfaceControl mOverlayLayer;
    private final Region mPcTouchExcludeRegion = new Region();
    private final Consumer<WindowState> mPerformLayout = new -$$Lambda$DisplayContent$qT01Aq6xt_ZOs86A1yDQe-qmPFQ(this);
    private final Consumer<WindowState> mPerformLayoutAttached = new -$$Lambda$DisplayContent$7voe_dEKk2BYMriCvPuvaznb9WQ(this);
    final PinnedStackController mPinnedStackControllerLocked;
    private boolean mQuickBoot = true;
    final DisplayMetrics mRealDisplayMetrics = new DisplayMetrics();
    private boolean mRemovingDisplay = false;
    private int mRotation = (SystemProperties.getInt("ro.panel.hw_orientation", 0) / 90);
    private final Consumer<WindowState> mScheduleToastTimeout = new -$$Lambda$DisplayContent$hRKjZwmneu0T85LNNY6_Zcs4gKM(this);
    private final SurfaceSession mSession = new SurfaceSession();
    boolean mShouldOverrideDisplayConfiguration = true;
    private int mSurfaceSize;
    TaskTapPointerEventListener mTapDetector;
    final ArraySet<WindowState> mTapExcludeProvidingWindows = new ArraySet();
    final ArrayList<WindowState> mTapExcludedWindows = new ArrayList();
    protected final TaskStackContainers mTaskStackContainers = new TaskStackContainers(this.mService);
    private final ApplySurfaceChangesTransactionState mTmpApplySurfaceChangesTransactionState = new ApplySurfaceChangesTransactionState();
    private final Rect mTmpBounds = new Rect();
    private final DisplayMetrics mTmpDisplayMetrics = new DisplayMetrics();
    private final float[] mTmpFloats = new float[9];
    private boolean mTmpInitial;
    private final Matrix mTmpMatrix = new Matrix();
    private boolean mTmpRecoveringMemory;
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final RectF mTmpRectF = new RectF();
    private final Region mTmpRegion = new Region();
    private final TaskForResizePointSearchResult mTmpTaskForResizePointSearchResult = new TaskForResizePointSearchResult();
    private final LinkedList<AppWindowToken> mTmpUpdateAllDrawn = new LinkedList();
    private WindowState mTmpWindow;
    private WindowState mTmpWindow2;
    WindowAnimator mTmpWindowAnimator;
    private final HashMap<IBinder, WindowToken> mTokenMap = new HashMap();
    WindowToken mTopAboveAppToken = null;
    private Region mTouchExcludeRegion = new Region();
    private boolean mUpdateImeTarget;
    private final Consumer<WindowState> mUpdateWallpaperForAnimator = new -$$Lambda$DisplayContent$D0QJUvhaQkGgoMtOmjw5foY9F8M(this);
    private final Consumer<WindowState> mUpdateWindowsForAnimator = new -$$Lambda$DisplayContent$0yxrqH9eGY2qTjH1u_BvaVrXCSA(this);
    WallpaperController mWallpaperController;
    boolean mWinEverCovered = false;
    private SurfaceControl mWindowingLayer;
    int pendingLayoutChanges;

    private static final class ApplySurfaceChangesTransactionState {
        boolean displayHasContent;
        boolean focusDisplayed;
        boolean obscured;
        int preferredModeId;
        float preferredRefreshRate;
        boolean syswin;

        private ApplySurfaceChangesTransactionState() {
        }

        /* synthetic */ ApplySurfaceChangesTransactionState(AnonymousClass1 x0) {
            this();
        }

        void reset() {
            this.displayHasContent = false;
            this.obscured = false;
            this.syswin = false;
            this.focusDisplayed = false;
            this.preferredRefreshRate = 0.0f;
            this.preferredModeId = 0;
        }
    }

    static final class ScreenshotApplicationState {
        WindowState appWin;
        int maxLayer;
        int minLayer;
        boolean screenshotReady;

        ScreenshotApplicationState() {
        }

        void reset(boolean screenshotReady) {
            this.appWin = null;
            int i = 0;
            this.maxLayer = 0;
            this.minLayer = 0;
            this.screenshotReady = screenshotReady;
            if (!screenshotReady) {
                i = HwBootFail.STAGE_BOOT_SUCCESS;
            }
            this.minLayer = i;
        }
    }

    static final class TaskForResizePointSearchResult {
        boolean searchDone;
        Task taskForResize;

        TaskForResizePointSearchResult() {
        }

        void reset() {
            this.searchDone = false;
            this.taskForResize = null;
        }
    }

    static class DisplayChildWindowContainer<E extends WindowContainer> extends WindowContainer<E> {
        DisplayChildWindowContainer(WindowManagerService service) {
            super(service);
        }

        boolean fillsParent() {
            return true;
        }

        boolean isVisible() {
            return true;
        }
    }

    protected class NonAppWindowContainers extends DisplayChildWindowContainer<WindowToken> {
        private final Dimmer mDimmer = new Dimmer(this);
        private final Predicate<WindowState> mGetOrientingWindow = -$$Lambda$DisplayContent$NonAppWindowContainers$FI_O7m2qEDfIRZef3D32AxG-rcs.INSTANCE;
        private final String mName;
        private final Rect mTmpDimBoundsRect = new Rect();
        private final Comparator<WindowToken> mWindowComparator = new -$$Lambda$DisplayContent$NonAppWindowContainers$nqCymC3xR9b3qaeohnnJJpSiajc(this);

        public static /* synthetic */ int lambda$new$0(NonAppWindowContainers nonAppWindowContainers, WindowToken token1, WindowToken token2) {
            return nonAppWindowContainers.mService.mPolicy.getWindowLayerFromTypeLw(token1.windowType, token1.mOwnerCanManageAppTokens) < nonAppWindowContainers.mService.mPolicy.getWindowLayerFromTypeLw(token2.windowType, token2.mOwnerCanManageAppTokens) ? -1 : 1;
        }

        static /* synthetic */ boolean lambda$new$1(WindowState w) {
            if (!w.isVisibleLw() || !w.mPolicyVisibilityAfterAnim) {
                return false;
            }
            int req = w.mAttrs.screenOrientation;
            if (req == -1 || req == 3 || req == -2) {
                return false;
            }
            return true;
        }

        NonAppWindowContainers(String name, WindowManagerService service) {
            super(service);
            this.mName = name;
        }

        void addChild(WindowToken token) {
            addChild((WindowContainer) token, this.mWindowComparator);
        }

        int getOrientation() {
            WindowManagerPolicy policy = this.mService.mPolicy;
            WindowState win = getWindow(this.mGetOrientingWindow);
            if (win != null) {
                int req = win.mAttrs.screenOrientation;
                if (policy.isKeyguardHostWindow(win.mAttrs)) {
                    DisplayContent.this.mLastKeyguardForcedOrientation = req;
                    if (this.mService.mKeyguardGoingAway) {
                        DisplayContent.this.mLastWindowForcedOrientation = -1;
                        return -2;
                    }
                }
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    String str = DisplayContent.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(win);
                    stringBuilder.append(" forcing orientation to ");
                    stringBuilder.append(req);
                    stringBuilder.append(" for display id=");
                    stringBuilder.append(DisplayContent.this.mDisplayId);
                    Slog.v(str, stringBuilder.toString());
                }
                return DisplayContent.this.mLastWindowForcedOrientation = req;
            }
            DisplayContent.this.mLastWindowForcedOrientation = -1;
            boolean isUnoccluding = this.mService.mAppTransition.getAppTransition() == 23 && this.mService.mUnknownAppVisibilityController.allResolved();
            if (policy.isKeyguardShowingAndNotOccluded() || isUnoccluding) {
                return DisplayContent.this.mLastKeyguardForcedOrientation;
            }
            return -2;
        }

        String getName() {
            return this.mName;
        }

        Dimmer getDimmer() {
            return this.mDimmer;
        }

        void prepareSurfaces() {
            this.mDimmer.resetDimStates();
            super.prepareSurfaces();
            getBounds(this.mTmpDimBoundsRect);
            if (this.mDimmer.updateDims(getPendingTransaction(), this.mTmpDimBoundsRect)) {
                scheduleAnimation();
            }
        }
    }

    protected final class TaskStackContainers extends DisplayChildWindowContainer<TaskStack> {
        SurfaceControl mAppAnimationLayer = null;
        SurfaceControl mBoostedAppAnimationLayer = null;
        SurfaceControl mHomeAppAnimationLayer = null;
        private TaskStack mHomeStack = null;
        private TaskStack mPinnedStack = null;
        SurfaceControl mSplitScreenDividerAnchor = null;
        private TaskStack mSplitScreenPrimaryStack = null;

        TaskStackContainers(WindowManagerService service) {
            super(service);
        }

        TaskStack getStack(int windowingMode, int activityType) {
            if (activityType == 2) {
                return this.mHomeStack;
            }
            if (windowingMode == 2) {
                return this.mPinnedStack;
            }
            if (windowingMode == 3) {
                return this.mSplitScreenPrimaryStack;
            }
            for (int i = DisplayContent.this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
                TaskStack stack = (TaskStack) DisplayContent.this.mTaskStackContainers.getChildAt(i);
                if (activityType == 0 && stack != null && windowingMode == stack.getWindowingMode()) {
                    return stack;
                }
                if (stack != null && stack.isCompatible(windowingMode, activityType)) {
                    return stack;
                }
            }
            return null;
        }

        @VisibleForTesting
        TaskStack getTopStack() {
            return DisplayContent.this.mTaskStackContainers.getChildCount() > 0 ? (TaskStack) DisplayContent.this.mTaskStackContainers.getChildAt(DisplayContent.this.mTaskStackContainers.getChildCount() - 1) : null;
        }

        TaskStack getHomeStack() {
            if (this.mHomeStack == null && DisplayContent.this.mDisplayId == 0) {
                String str = DisplayContent.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getHomeStack: Returning null from this=");
                stringBuilder.append(this);
                Slog.e(str, stringBuilder.toString());
            }
            return this.mHomeStack;
        }

        TaskStack getPinnedStack() {
            return this.mPinnedStack;
        }

        TaskStack getSplitScreenPrimaryStack() {
            return this.mSplitScreenPrimaryStack;
        }

        ArrayList<Task> getVisibleTasks() {
            ArrayList<Task> visibleTasks = new ArrayList();
            forAllTasks(new -$$Lambda$DisplayContent$TaskStackContainers$rQnI0Y8R9ptQ09cGHwbCHDiG2FY(visibleTasks));
            return visibleTasks;
        }

        static /* synthetic */ void lambda$getVisibleTasks$0(ArrayList visibleTasks, Task task) {
            if (task.isVisible()) {
                visibleTasks.add(task);
            }
        }

        void addStackToDisplay(TaskStack stack, boolean onTop) {
            addStackReferenceIfNeeded(stack);
            addChild(stack, onTop);
            stack.onDisplayChanged(DisplayContent.this);
        }

        void onStackWindowingModeChanged(TaskStack stack) {
            removeStackReferenceIfNeeded(stack);
            addStackReferenceIfNeeded(stack);
            if (stack == this.mPinnedStack && getTopStack() != stack) {
                positionChildAt((int) HwBootFail.STAGE_BOOT_SUCCESS, stack, false);
            }
        }

        private void addStackReferenceIfNeeded(TaskStack stack) {
            if (stack.isActivityTypeHome()) {
                if (this.mHomeStack == null) {
                    this.mHomeStack = stack;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("addStackReferenceIfNeeded: home stack=");
                    stringBuilder.append(this.mHomeStack);
                    stringBuilder.append(" already exist on display=");
                    stringBuilder.append(this);
                    stringBuilder.append(" stack=");
                    stringBuilder.append(stack);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            int windowingMode = stack.getWindowingMode();
            StringBuilder stringBuilder2;
            if (windowingMode == 2) {
                if (this.mPinnedStack == null) {
                    this.mPinnedStack = stack;
                    return;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("addStackReferenceIfNeeded: pinned stack=");
                stringBuilder2.append(this.mPinnedStack);
                stringBuilder2.append(" already exist on display=");
                stringBuilder2.append(this);
                stringBuilder2.append(" stack=");
                stringBuilder2.append(stack);
                throw new IllegalArgumentException(stringBuilder2.toString());
            } else if (windowingMode != 3) {
            } else {
                if (this.mSplitScreenPrimaryStack == null) {
                    this.mSplitScreenPrimaryStack = stack;
                    DisplayContent.this.mDividerControllerLocked.notifyDockedStackExistsChanged(true);
                    return;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("addStackReferenceIfNeeded: split-screen-primary stack=");
                stringBuilder2.append(this.mSplitScreenPrimaryStack);
                stringBuilder2.append(" already exist on display=");
                stringBuilder2.append(this);
                stringBuilder2.append(" stack=");
                stringBuilder2.append(stack);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }

        private void removeStackReferenceIfNeeded(TaskStack stack) {
            if (stack == this.mHomeStack) {
                this.mHomeStack = null;
            } else if (stack == this.mPinnedStack) {
                this.mPinnedStack = null;
            } else if (stack == this.mSplitScreenPrimaryStack) {
                this.mSplitScreenPrimaryStack = null;
                this.mService.setDockedStackCreateStateLocked(0, null);
                DisplayContent.this.mDividerControllerLocked.notifyDockedStackExistsChanged(false);
            }
        }

        private void addChild(TaskStack stack, boolean toTop) {
            addChild((WindowContainer) stack, findPositionForStack(toTop ? this.mChildren.size() : 0, stack, true));
            DisplayContent.this.setLayoutNeeded();
        }

        protected void removeChild(TaskStack stack) {
            super.removeChild(stack);
            removeStackReferenceIfNeeded(stack);
        }

        boolean isOnTop() {
            return true;
        }

        void positionChildAt(int position, TaskStack child, boolean includingParents) {
            if (!child.getWindowConfiguration().isAlwaysOnTop() || position == HwBootFail.STAGE_BOOT_SUCCESS) {
                super.positionChildAt(findPositionForStack(position, child, false), child, includingParents);
                DisplayContent.this.setLayoutNeeded();
                return;
            }
            String str = DisplayContent.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring move of always-on-top stack=");
            stringBuilder.append(this);
            stringBuilder.append(" to bottom");
            Slog.w(str, stringBuilder.toString());
            super.positionChildAt(this.mChildren.indexOf(child), child, false);
        }

        private int findPositionForStack(int requestedPosition, TaskStack stack, boolean adding) {
            boolean toTop = true;
            int topChildPosition = this.mChildren.size() - 1;
            boolean toTop2 = requestedPosition == HwBootFail.STAGE_BOOT_SUCCESS;
            if (adding ? requestedPosition < topChildPosition + 1 : requestedPosition < topChildPosition) {
                toTop = false;
            }
            int targetPosition = requestedPosition;
            if (!(toTop | toTop2) || stack.getWindowingMode() == 2 || !DisplayContent.this.hasPinnedStack()) {
                return targetPosition;
            }
            if (((TaskStack) this.mChildren.get(topChildPosition)).getWindowingMode() == 2) {
                return adding ? topChildPosition : topChildPosition - 1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Pinned stack isn't top stack??? ");
            stringBuilder.append(this.mChildren);
            throw new IllegalStateException(stringBuilder.toString());
        }

        boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
            if (traverseTopToBottom) {
                if (super.forAllWindows((ToBooleanFunction) callback, traverseTopToBottom) || forAllExitingAppTokenWindows(callback, traverseTopToBottom)) {
                    return true;
                }
            } else if (forAllExitingAppTokenWindows(callback, traverseTopToBottom) || super.forAllWindows((ToBooleanFunction) callback, traverseTopToBottom)) {
                return true;
            }
            return false;
        }

        private boolean forAllExitingAppTokenWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
            int i;
            if (traverseTopToBottom) {
                for (i = this.mChildren.size() - 1; i >= 0; i--) {
                    AppTokenList appTokens = ((TaskStack) this.mChildren.get(i)).mExitingAppTokens;
                    for (int j = appTokens.size() - 1; j >= 0; j--) {
                        if (((AppWindowToken) appTokens.get(j)).forAllWindowsUnchecked(callback, traverseTopToBottom)) {
                            return true;
                        }
                    }
                }
            } else {
                i = this.mChildren.size();
                for (int i2 = 0; i2 < i; i2++) {
                    AppTokenList appTokens2 = ((TaskStack) this.mChildren.get(i2)).mExitingAppTokens;
                    int appTokensCount = appTokens2.size();
                    for (int j2 = 0; j2 < appTokensCount; j2++) {
                        if (((AppWindowToken) appTokens2.get(j2)).forAllWindowsUnchecked(callback, traverseTopToBottom)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        void setExitingTokensHasVisible(boolean hasVisible) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                AppTokenList appTokens = ((TaskStack) this.mChildren.get(i)).mExitingAppTokens;
                for (int j = appTokens.size() - 1; j >= 0; j--) {
                    ((AppWindowToken) appTokens.get(j)).hasVisible = hasVisible;
                }
            }
        }

        void removeExistingAppTokensIfPossible() {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                AppTokenList appTokens = ((TaskStack) this.mChildren.get(i)).mExitingAppTokens;
                for (int j = appTokens.size() - 1; j >= 0; j--) {
                    AppWindowToken token = (AppWindowToken) appTokens.get(j);
                    if (!(token.hasVisible || this.mService.mClosingApps.contains(token) || (token.mIsExiting && !token.isEmpty()))) {
                        cancelAnimation();
                        String str = DisplayContent.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("performLayout: App token exiting now removed");
                        stringBuilder.append(token);
                        Slog.v(str, stringBuilder.toString());
                        token.removeIfPossible();
                    }
                }
            }
        }

        int getOrientation() {
            int orientation;
            if (DisplayContent.this.isStackVisible(3) || DisplayContent.this.isStackVisible(5)) {
                if (this.mHomeStack != null && this.mHomeStack.isVisible() && DisplayContent.this.mDividerControllerLocked.isMinimizedDock() && !(DisplayContent.this.mDividerControllerLocked.isHomeStackResizable() && this.mHomeStack.matchParentBounds())) {
                    orientation = this.mHomeStack.getOrientation();
                    if (orientation != -2) {
                        return orientation;
                    }
                }
                return -1;
            }
            orientation = super.getOrientation();
            String str;
            StringBuilder stringBuilder;
            if (this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    str = DisplayContent.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Forcing UNSPECIFIED orientation in car for display id=");
                    stringBuilder.append(DisplayContent.this.mDisplayId);
                    stringBuilder.append(". Ignoring ");
                    stringBuilder.append(orientation);
                    Slog.v(str, stringBuilder.toString());
                }
                return -1;
            } else if (orientation == -2 || orientation == 3) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    str = DisplayContent.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No app is requesting an orientation, return ");
                    stringBuilder.append(DisplayContent.this.mLastOrientation);
                    stringBuilder.append(" for display id=");
                    stringBuilder.append(DisplayContent.this.mDisplayId);
                    Slog.v(str, stringBuilder.toString());
                }
                return DisplayContent.this.mLastOrientation;
            } else {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    str = DisplayContent.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("App is requesting an orientation, return ");
                    stringBuilder.append(orientation);
                    stringBuilder.append(" for display id=");
                    stringBuilder.append(DisplayContent.this.mDisplayId);
                    Slog.v(str, stringBuilder.toString());
                }
                return orientation;
            }
        }

        void assignChildLayers(Transaction t) {
            assignStackOrdering(t);
            for (int i = 0; i < this.mChildren.size(); i++) {
                ((TaskStack) this.mChildren.get(i)).assignChildLayers(t);
            }
        }

        void assignStackOrdering(Transaction t) {
            int layerForAnimationLayer = 0;
            int layerForBoostedAnimationLayer = 0;
            int layerForHomeAnimationLayer = 0;
            int layer = 0;
            int state = 0;
            while (state <= 2) {
                int layerForBoostedAnimationLayer2 = layerForBoostedAnimationLayer;
                layerForBoostedAnimationLayer = layerForAnimationLayer;
                for (layerForAnimationLayer = 0; layerForAnimationLayer < this.mChildren.size(); layerForAnimationLayer++) {
                    TaskStack s = (TaskStack) this.mChildren.get(layerForAnimationLayer);
                    if ((state != 0 || s.isActivityTypeHome()) && (!(state == 1 && (s.isActivityTypeHome() || s.isAlwaysOnTop())) && (state != 2 || s.isAlwaysOnTop()))) {
                        int layer2 = layer + 1;
                        s.assignLayer(t, layer);
                        if (s.inSplitScreenWindowingMode() && this.mSplitScreenDividerAnchor != null) {
                            int layer3 = layer2 + 1;
                            t.setLayer(this.mSplitScreenDividerAnchor, layer2);
                            layer2 = layer3;
                        }
                        if ((s.isTaskAnimating() || s.isAppAnimating()) && state != 2) {
                            layerForBoostedAnimationLayer = layer2;
                            layer2++;
                        }
                        if (state != 2) {
                            layer = layer2 + 1;
                            layerForBoostedAnimationLayer2 = layer2;
                        } else {
                            layer = layer2;
                        }
                    }
                }
                if (state == 0) {
                    layerForHomeAnimationLayer = layer;
                    layer++;
                }
                state++;
                layerForAnimationLayer = layerForBoostedAnimationLayer;
                layerForBoostedAnimationLayer = layerForBoostedAnimationLayer2;
            }
            if (this.mAppAnimationLayer != null) {
                t.setLayer(this.mAppAnimationLayer, layerForAnimationLayer);
            }
            if (this.mBoostedAppAnimationLayer != null) {
                t.setLayer(this.mBoostedAppAnimationLayer, layerForBoostedAnimationLayer);
            }
            if (this.mHomeAppAnimationLayer != null) {
                t.setLayer(this.mHomeAppAnimationLayer, layerForHomeAnimationLayer);
            }
        }

        SurfaceControl getAppAnimationLayer(@AnimationLayer int animationLayer) {
            switch (animationLayer) {
                case 1:
                    return this.mBoostedAppAnimationLayer;
                case 2:
                    return this.mHomeAppAnimationLayer;
                default:
                    return this.mAppAnimationLayer;
            }
        }

        SurfaceControl getSplitScreenDividerAnchor() {
            return this.mSplitScreenDividerAnchor;
        }

        void onParentSet() {
            super.onParentSet();
            if (getParent() != null) {
                this.mAppAnimationLayer = makeChildSurface(null).setName("animationLayer").build();
                this.mBoostedAppAnimationLayer = makeChildSurface(null).setName("boostedAnimationLayer").build();
                this.mHomeAppAnimationLayer = makeChildSurface(null).setName("homeAnimationLayer").build();
                this.mSplitScreenDividerAnchor = makeChildSurface(null).setName("splitScreenDividerAnchor").build();
                getPendingTransaction().show(this.mAppAnimationLayer).show(this.mBoostedAppAnimationLayer).show(this.mHomeAppAnimationLayer).show(this.mSplitScreenDividerAnchor);
                scheduleAnimation();
                return;
            }
            this.mAppAnimationLayer.destroy();
            this.mAppAnimationLayer = null;
            this.mBoostedAppAnimationLayer.destroy();
            this.mBoostedAppAnimationLayer = null;
            this.mHomeAppAnimationLayer.destroy();
            this.mHomeAppAnimationLayer = null;
            this.mSplitScreenDividerAnchor.destroy();
            this.mSplitScreenDividerAnchor = null;
        }
    }

    protected final class AboveAppWindowContainers extends NonAppWindowContainers {
        AboveAppWindowContainers(String name, WindowManagerService service) {
            super(name, service);
        }

        void assignChildLayers(Transaction t) {
            assignChildLayers(t, null);
        }

        void assignChildLayers(Transaction t, WindowContainer imeContainer) {
            int j = 0;
            boolean needAssignIme = (imeContainer == null || imeContainer.getSurfaceControl() == null) ? false : true;
            while (j < this.mChildren.size()) {
                WindowToken wt = (WindowToken) this.mChildren.get(j);
                if (wt.windowType == 2034) {
                    wt.assignRelativeLayer(t, DisplayContent.this.mTaskStackContainers.getSplitScreenDividerAnchor(), 1);
                } else {
                    wt.assignLayer(t, j);
                    wt.assignChildLayers(t);
                    int layer = this.mService.mPolicy.getWindowLayerFromTypeLw(wt.windowType, wt.mOwnerCanManageAppTokens);
                    if (needAssignIme && layer >= this.mService.mPolicy.getWindowLayerFromTypeLw(2012, true)) {
                        imeContainer.assignRelativeLayer(t, wt.getSurfaceControl(), -1);
                        needAssignIme = false;
                    }
                }
                j++;
            }
            if (needAssignIme) {
                imeContainer.assignRelativeLayer(t, getSurfaceControl(), HwBootFail.STAGE_BOOT_SUCCESS);
            }
            DisplayContent.this.checkNeedNotifyFingerWinCovered();
            DisplayContent.this.mObserveToken = null;
            DisplayContent.this.mTopAboveAppToken = null;
        }
    }

    private class NonMagnifiableWindowContainers extends NonAppWindowContainers {
        NonMagnifiableWindowContainers(String name, WindowManagerService service) {
            super(name, service);
        }

        void applyMagnificationSpec(Transaction t, MagnificationSpec spec) {
        }
    }

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        IS_DEBUG_VERSION = z;
    }

    boolean isCoverOpen() {
        return this.mService.isCoverOpen();
    }

    public static /* synthetic */ void lambda$new$0(DisplayContent displayContent, WindowState w) {
        WindowStateAnimator winAnimator = w.mWinAnimator;
        AppWindowToken atoken = w.mAppToken;
        if (winAnimator.mDrawState != 3) {
            return;
        }
        if ((atoken == null || atoken.allDrawn) && w.performShowLocked()) {
            displayContent.pendingLayoutChanges |= 8;
        }
    }

    public static /* synthetic */ void lambda$new$1(DisplayContent displayContent, WindowState w) {
        WindowStateAnimator winAnimator = w.mWinAnimator;
        if (winAnimator.mSurfaceController != null && winAnimator.hasSurface()) {
            TaskStack stack;
            int flags = w.mAttrs.flags;
            if (winAnimator.isAnimationSet()) {
                AnimationAdapter anim = w.getAnimation();
                if (anim != null) {
                    if ((flags & DumpState.DUMP_DEXOPT) != 0 && anim.getDetachWallpaper()) {
                        displayContent.mTmpWindow = w;
                    }
                    int color = anim.getBackgroundColor();
                    if (color != 0) {
                        stack = w.getStack();
                        if (stack != null) {
                            stack.setAnimationBackground(winAnimator, color);
                        }
                    }
                }
            }
            AppWindowToken atoken = winAnimator.mWin.mAppToken;
            AnimationAdapter animation = atoken != null ? atoken.getAnimation() : null;
            if (animation != null) {
                if ((DumpState.DUMP_DEXOPT & flags) != 0 && animation.getDetachWallpaper()) {
                    displayContent.mTmpWindow = w;
                }
                int color2 = animation.getBackgroundColor();
                if (color2 != 0) {
                    stack = w.getStack();
                    if (stack != null) {
                        stack.setAnimationBackground(winAnimator, color2);
                    }
                }
            }
        }
    }

    public static /* synthetic */ void lambda$new$2(DisplayContent displayContent, WindowState w) {
        int lostFocusUid = displayContent.mTmpWindow.mOwnerUid;
        Handler handler = displayContent.mService.mH;
        if (w.mAttrs.type == 2005 && w.mOwnerUid == lostFocusUid && !handler.hasMessages(52, w)) {
            handler.sendMessageDelayed(handler.obtainMessage(52, w), w.mAttrs.hideTimeoutMilliseconds);
        }
    }

    public static /* synthetic */ boolean lambda$new$3(DisplayContent displayContent, WindowState w) {
        String str;
        AppWindowToken focusedApp = displayContent.mService.mFocusedApp;
        if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Looking for focus: ");
            stringBuilder.append(w);
            stringBuilder.append(", flags=");
            stringBuilder.append(w.mAttrs.flags);
            stringBuilder.append(", canReceive=");
            stringBuilder.append(w.canReceiveKeys());
            Slog.v(str, stringBuilder.toString());
        }
        if (!w.canReceiveKeys()) {
            return false;
        }
        if (HwPCUtils.isPcCastModeInServer() && !displayContent.isDefaultDisplay) {
            if (displayContent.mService.getPCLauncherFocused() && w.mAttrs != null && w.mAttrs.type != 2103 && w.mAppToken != null) {
                return false;
            }
            if (HwPCUtils.enabledInPad() && !displayContent.mService.mPolicy.isKeyguardLocked() && w.mAttrs != null && w.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Skipping ");
                stringBuilder2.append(w);
                stringBuilder2.append(", flags=");
                stringBuilder2.append(w.mAttrs.flags);
                stringBuilder2.append(", canReceive=");
                stringBuilder2.append(w.canReceiveKeys());
                HwPCUtils.log(str, stringBuilder2.toString());
                return false;
            }
        }
        AppWindowToken wtoken = w.mAppToken;
        String str2;
        StringBuilder stringBuilder3;
        if (wtoken != null && (wtoken.removed || wtoken.sendingToBottom)) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Skipping ");
                stringBuilder3.append(wtoken);
                stringBuilder3.append(" because ");
                stringBuilder3.append(wtoken.removed ? "removed" : "sendingToBottom");
                Slog.v(str2, stringBuilder3.toString());
            }
            return false;
        } else if (focusedApp == null) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("findFocusedWindow: focusedApp=null using new focus @ ");
                stringBuilder3.append(w);
                Slog.v(str2, stringBuilder3.toString());
            }
            displayContent.mTmpWindow = w;
            return true;
        } else if (!focusedApp.windowsAreFocusable()) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("findFocusedWindow: focusedApp windows not focusable using new focus @ ");
                stringBuilder3.append(w);
                Slog.v(str2, stringBuilder3.toString());
            }
            displayContent.mTmpWindow = w;
            return true;
        } else if (wtoken == null || w.mAttrs.type == 3 || focusedApp.compareTo((WindowContainer) wtoken) <= 0 || (HwPCUtils.isPcCastModeInServer() && focusedApp.getDisplayContent() != displayContent)) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("findFocusedWindow: Found new focus @ ");
                stringBuilder3.append(w);
                Slog.v(str2, stringBuilder3.toString());
            }
            displayContent.mTmpWindow = w;
            return true;
        } else {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("findFocusedWindow: ");
                stringBuilder3.append(wtoken);
                stringBuilder3.append(" below Reached focused app=");
                stringBuilder3.append(focusedApp);
                Slog.v(str2, stringBuilder3.toString());
            }
            displayContent.mTmpWindow = null;
            return true;
        }
    }

    public static /* synthetic */ void lambda$new$4(DisplayContent displayContent, WindowState w) {
        boolean isCoverOpen = IS_TABLET ? displayContent.isCoverOpen() || displayContent.mService.mPowerManager.isScreenOn() : displayContent.isCoverOpen();
        if (displayContent.mTmpInitial && IS_DEBUG_VERSION) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put("checkType", "HighWindowLayerScene");
            params.put("number", Integer.valueOf(displayContent.index[0]));
            params.put("windowState", w);
            if (HwServiceFactory.getWinFreezeScreenMonitor() != null) {
                HwServiceFactory.getWinFreezeScreenMonitor().checkFreezeScreen(params);
            }
            displayContent.index[0] = displayContent.index[0] + 1;
        }
        boolean ggGone = (displayContent.mTmpWindow != null && displayContent.mService.mPolicy.canBeHiddenByKeyguardLw(w)) || w.isGoneForLayoutLw();
        boolean mNeededRelayoutWallpaper = w.mIsWallpaper && w.mReportOrientationChanged;
        boolean gone = ((!ggGone && isCoverOpen) || w.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || w.mAttrs.type == 2101 || w.mAttrs.type == 2100 || mNeededRelayoutWallpaper) ? false : true;
        if (WindowManagerDebugConfig.DEBUG_LAYOUT && !w.mLayoutAttached) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("1ST PASS ");
            stringBuilder.append(w);
            stringBuilder.append(": gone=");
            stringBuilder.append(gone);
            stringBuilder.append("<-");
            stringBuilder.append(ggGone);
            stringBuilder.append(" mHaveFrame=");
            stringBuilder.append(w.mHaveFrame);
            stringBuilder.append(" mLayoutAttached=");
            stringBuilder.append(w.mLayoutAttached);
            stringBuilder.append(" screen changed=");
            stringBuilder.append(w.isConfigChanged());
            Slog.v(str, stringBuilder.toString());
            AppWindowToken atoken = w.mAppToken;
            String str2;
            StringBuilder stringBuilder2;
            boolean z;
            if (gone) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  GONE: mViewVisibility=");
                stringBuilder2.append(w.mViewVisibility);
                stringBuilder2.append(" mRelayoutCalled=");
                stringBuilder2.append(w.mRelayoutCalled);
                stringBuilder2.append(" hidden=");
                stringBuilder2.append(w.mToken.isHidden());
                stringBuilder2.append(" hiddenRequested=");
                z = atoken != null && atoken.hiddenRequested;
                stringBuilder2.append(z);
                stringBuilder2.append(" parentHidden=");
                stringBuilder2.append(w.isParentWindowHidden());
                Slog.v(str2, stringBuilder2.toString());
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  VIS: mViewVisibility=");
                stringBuilder2.append(w.mViewVisibility);
                stringBuilder2.append(" mRelayoutCalled=");
                stringBuilder2.append(w.mRelayoutCalled);
                stringBuilder2.append(" hidden=");
                stringBuilder2.append(w.mToken.isHidden());
                stringBuilder2.append(" hiddenRequested=");
                z = atoken != null && atoken.hiddenRequested;
                stringBuilder2.append(z);
                stringBuilder2.append(" parentHidden=");
                stringBuilder2.append(w.isParentWindowHidden());
                Slog.v(str2, stringBuilder2.toString());
            }
        }
        if (gone && w.mHaveFrame && !w.mLayoutNeeded) {
            if ((!w.isConfigChanged() && !w.setReportResizeHints()) || w.isGoneForLayoutLw()) {
                return;
            }
            if ((w.mAttrs.privateFlags & 1024) == 0 && !(w.mHasSurface && w.mAppToken != null && w.mAppToken.layoutConfigChanges)) {
                return;
            }
        }
        if (!w.mLayoutAttached) {
            if (displayContent.mTmpInitial) {
                w.mContentChanged = false;
            }
            if (w.mAttrs.type == 2023) {
                displayContent.mTmpWindow = w;
            }
            w.mLayoutNeeded = false;
            w.prelayout();
            boolean firstLayout = true ^ w.isLaidOut();
            displayContent.mService.mPolicy.layoutWindowLw(w, null, displayContent.mDisplayFrames);
            w.mLayoutSeq = displayContent.mLayoutSeq;
            if (firstLayout) {
                w.updateLastInsetValues();
            }
            if (w.mAppToken != null) {
                w.mAppToken.layoutLetterbox(w);
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  LAYOUT: mFrame=");
                stringBuilder3.append(w.mFrame);
                stringBuilder3.append(" mContainingFrame=");
                stringBuilder3.append(w.mContainingFrame);
                stringBuilder3.append(" mDisplayFrame=");
                stringBuilder3.append(w.mDisplayFrame);
                Slog.v(str3, stringBuilder3.toString());
            }
        }
    }

    public static /* synthetic */ void lambda$new$5(DisplayContent displayContent, WindowState w) {
        if (w.mLayoutAttached) {
            String str;
            StringBuilder stringBuilder;
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("2ND PASS ");
                stringBuilder.append(w);
                stringBuilder.append(" mHaveFrame=");
                stringBuilder.append(w.mHaveFrame);
                stringBuilder.append(" mViewVisibility=");
                stringBuilder.append(w.mViewVisibility);
                stringBuilder.append(" mRelayoutCalled=");
                stringBuilder.append(w.mRelayoutCalled);
                Slog.v(str, stringBuilder.toString());
            }
            if (displayContent.mTmpWindow != null && displayContent.mService.mPolicy.canBeHiddenByKeyguardLw(w)) {
                return;
            }
            if ((w.mViewVisibility != 8 && w.mRelayoutCalled) || !w.mHaveFrame || w.mLayoutNeeded) {
                if (displayContent.mTmpInitial) {
                    w.mContentChanged = false;
                }
                w.mLayoutNeeded = false;
                w.prelayout();
                displayContent.mService.mPolicy.layoutWindowLw(w, w.getParentWindow(), displayContent.mDisplayFrames);
                w.mLayoutSeq = displayContent.mLayoutSeq;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" LAYOUT: mFrame=");
                    stringBuilder.append(w.mFrame);
                    stringBuilder.append(" mContainingFrame=");
                    stringBuilder.append(w.mContainingFrame);
                    stringBuilder.append(" mDisplayFrame=");
                    stringBuilder.append(w.mDisplayFrame);
                    Slog.v(str, stringBuilder.toString());
                }
            }
        } else if (w.mAttrs.type == 2023) {
            displayContent.mTmpWindow = displayContent.mTmpWindow2;
        }
    }

    public static /* synthetic */ boolean lambda$new$6(DisplayContent displayContent, WindowState w) {
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && displayContent.mUpdateImeTarget) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Checking window @");
            stringBuilder.append(w);
            stringBuilder.append(" fl=0x");
            stringBuilder.append(Integer.toHexString(w.mAttrs.flags));
            Slog.i(str, stringBuilder.toString());
        }
        return w.canBeImeTarget();
    }

    public static /* synthetic */ void lambda$new$8(DisplayContent displayContent, WindowState w) {
        WindowSurfacePlacer surfacePlacer = displayContent.mService.mWindowPlacerLocked;
        boolean obscuredChanged = w.mObscured != displayContent.mTmpApplySurfaceChangesTransactionState.obscured;
        RootWindowContainer root = displayContent.mService.mRoot;
        boolean someoneLosingFocus = displayContent.mService.mLosingFocus.isEmpty() ^ true;
        w.mObscured = displayContent.mTmpApplySurfaceChangesTransactionState.obscured;
        if (!displayContent.mTmpApplySurfaceChangesTransactionState.obscured) {
            boolean isDisplayed = w.isDisplayedLw();
            if (isDisplayed && w.isObscuringDisplay()) {
                root.mObscuringWindow = w;
                displayContent.mTmpApplySurfaceChangesTransactionState.obscured = true;
            }
            ApplySurfaceChangesTransactionState applySurfaceChangesTransactionState = displayContent.mTmpApplySurfaceChangesTransactionState;
            applySurfaceChangesTransactionState.displayHasContent |= root.handleNotObscuredLocked(w, displayContent.mTmpApplySurfaceChangesTransactionState.obscured, displayContent.mTmpApplySurfaceChangesTransactionState.syswin);
            if (w.mHasSurface && isDisplayed) {
                int type = w.mAttrs.type;
                if (type == 2008 || type == 2010 || (w.mAttrs.privateFlags & 1024) != 0) {
                    displayContent.mTmpApplySurfaceChangesTransactionState.syswin = true;
                }
                if (displayContent.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate == 0.0f && w.mAttrs.preferredRefreshRate != 0.0f) {
                    displayContent.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate = w.mAttrs.preferredRefreshRate;
                }
                if (displayContent.mTmpApplySurfaceChangesTransactionState.preferredModeId == 0 && w.mAttrs.preferredDisplayModeId != 0) {
                    displayContent.mTmpApplySurfaceChangesTransactionState.preferredModeId = w.mAttrs.preferredDisplayModeId;
                }
            }
        }
        if (displayContent.isDefaultDisplay && obscuredChanged && w.isVisibleLw() && displayContent.mWallpaperController.isWallpaperTarget(w)) {
            displayContent.mWallpaperController.updateWallpaperVisibility();
        }
        w.handleWindowMovedIfNeeded();
        WindowStateAnimator winAnimator = w.mWinAnimator;
        w.mContentChanged = false;
        if (w.mHasSurface) {
            boolean committed = winAnimator.commitFinishDrawingLocked();
            if (displayContent.isDefaultDisplay && committed) {
                if (w.mAttrs.type == 2023) {
                    displayContent.pendingLayoutChanges |= 1;
                }
                if ((w.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0) {
                    if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("First draw done in potential wallpaper target ");
                        stringBuilder.append(w);
                        Slog.v(str, stringBuilder.toString());
                    }
                    root.mWallpaperMayChange = true;
                    displayContent.pendingLayoutChanges |= 4;
                }
            }
        }
        AppWindowToken atoken = w.mAppToken;
        if (atoken != null) {
            atoken.updateLetterboxSurface(w);
            if (atoken.updateDrawnWindowStates(w) && !displayContent.mTmpUpdateAllDrawn.contains(atoken)) {
                String str2 = TAG_VISIBILITY;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateAllDrawn Add ");
                stringBuilder2.append(atoken);
                Slog.v(str2, stringBuilder2.toString());
                displayContent.mTmpUpdateAllDrawn.add(atoken);
            }
        }
        if (displayContent.isDefaultDisplay && someoneLosingFocus && w == displayContent.mService.mCurrentFocus && w.isDisplayedLw()) {
            displayContent.mTmpApplySurfaceChangesTransactionState.focusDisplayed = true;
        }
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayContent.mDisplayId) && someoneLosingFocus && w == displayContent.mService.mCurrentFocus && w.isDisplayedLw()) {
            displayContent.mTmpApplySurfaceChangesTransactionState.focusDisplayed = true;
        }
        w.updateResizingWindowIfNeeded();
    }

    public DisplayContent(Display display, WindowManagerService service, WallpaperController wallpaperController, DisplayWindowController controller) {
        super(service);
        setController(controller);
        if (service.mRoot.getDisplayContent(display.getDisplayId()) == null) {
            this.mDisplay = display;
            this.mDisplayId = display.getDisplayId();
            this.mWallpaperController = wallpaperController;
            display.getDisplayInfo(this.mDisplayInfo);
            display.getMetrics(this.mDisplayMetrics);
            this.isDefaultDisplay = this.mDisplayId == 0;
            this.mDisplayFrames = new DisplayFrames(this.mDisplayId, this.mDisplayInfo, calculateDisplayCutoutForRotation(this.mDisplayInfo.rotation));
            if (!(this.isDefaultDisplay || "local:100000".equals(this.mDisplayInfo.uniqueId) || SystemProperties.getInt("ro.panel.hw_orientation", 0) / 90 != 1)) {
                this.mRotation = 0;
            }
            initializeDisplayBaseInfo();
            this.mDividerControllerLocked = new DockedStackDividerController(service, this);
            this.mPinnedStackControllerLocked = new PinnedStackController(service, this);
            this.mSurfaceSize = Math.max(this.mBaseDisplayHeight, this.mBaseDisplayWidth) * 2;
            Builder b = this.mService.makeSurfaceBuilder(this.mSession).setSize(this.mSurfaceSize, this.mSurfaceSize).setOpaque(true);
            this.mWindowingLayer = b.setName("Display Root").build();
            this.mOverlayLayer = b.setName("Display Overlays").build();
            getPendingTransaction().setLayer(this.mWindowingLayer, 0).setLayerStack(this.mWindowingLayer, this.mDisplayId).show(this.mWindowingLayer).setLayer(this.mOverlayLayer, 1).setLayerStack(this.mOverlayLayer, this.mDisplayId).show(this.mOverlayLayer);
            getPendingTransaction().apply();
            super.addChild(this.mBelowAppWindowsContainers, null);
            super.addChild(this.mTaskStackContainers, null);
            super.addChild(this.mAboveAppWindowsContainers, null);
            super.addChild(this.mImeWindowsContainers, null);
            this.mService.mRoot.addChild((WindowContainer) this, null);
            this.mDisplayReady = true;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display with ID=");
        stringBuilder.append(display.getDisplayId());
        stringBuilder.append(" already exists=");
        stringBuilder.append(service.mRoot.getDisplayContent(display.getDisplayId()));
        stringBuilder.append(" new=");
        stringBuilder.append(display);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean isReady() {
        return this.mService.mDisplayReady && this.mDisplayReady;
    }

    int getDisplayId() {
        return this.mDisplayId;
    }

    WindowToken getWindowToken(IBinder binder) {
        return (WindowToken) this.mTokenMap.get(binder);
    }

    AppWindowToken getAppWindowToken(IBinder binder) {
        WindowToken token = getWindowToken(binder);
        if (token == null) {
            return null;
        }
        return token.asAppWindowToken();
    }

    private void addWindowToken(IBinder binder, WindowToken token) {
        DisplayContent dc = this.mService.mRoot.getWindowTokenDisplay(token);
        StringBuilder stringBuilder;
        if (dc != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't map token=");
            stringBuilder.append(token);
            stringBuilder.append(" to display=");
            stringBuilder.append(getName());
            stringBuilder.append(" already mapped to display=");
            stringBuilder.append(dc);
            stringBuilder.append(" tokens=");
            stringBuilder.append(dc.mTokenMap);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (binder == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't map token=");
            stringBuilder.append(token);
            stringBuilder.append(" to display=");
            stringBuilder.append(getName());
            stringBuilder.append(" binder is null");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (token != null) {
            if (HwVRUtils.isVRMode() && IS_DEBUG_VERSION) {
                String str = TAG_VR;
                stringBuilder = new StringBuilder();
                stringBuilder.append("displaycontent addWindowToken binder = ");
                stringBuilder.append(binder);
                stringBuilder.append("token = ");
                stringBuilder.append(token);
                stringBuilder.append("displayid = ");
                stringBuilder.append(this.mDisplayId);
                Log.i(str, stringBuilder.toString());
            }
            this.mTokenMap.put(binder, token);
            if (token.asAppWindowToken() == null) {
                int i = token.windowType;
                if (i != 2103) {
                    switch (i) {
                        case 2011:
                        case 2012:
                            this.mImeWindowsContainers.addChild(token);
                            return;
                        case 2013:
                            break;
                        default:
                            this.mAboveAppWindowsContainers.addChild(token);
                            return;
                    }
                }
                this.mBelowAppWindowsContainers.addChild(token);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't map null token to display=");
            stringBuilder.append(getName());
            stringBuilder.append(" binder=");
            stringBuilder.append(binder);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    WindowToken removeWindowToken(IBinder binder) {
        WindowToken token = (WindowToken) this.mTokenMap.remove(binder);
        if (token != null && token.asAppWindowToken() == null) {
            token.setExiting();
            if (token.isEmpty()) {
                token.removeImmediately();
            }
        }
        return token;
    }

    void reParentWindowToken(WindowToken token) {
        DisplayContent prevDc = token.getDisplayContent();
        if (prevDc != this) {
            if (!(prevDc == null || prevDc.mTokenMap.remove(token.token) == null || token.asAppWindowToken() != null)) {
                token.getParent().removeChild(token);
            }
            addWindowToken(token.token, token);
        }
    }

    void removeAppToken(IBinder binder) {
        WindowToken token = removeWindowToken(binder);
        if (token == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeAppToken: Attempted to remove non-existing token: ");
            stringBuilder.append(binder);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        AppWindowToken appToken = token.asAppWindowToken();
        if (appToken == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Attempted to remove non-App token: ");
            stringBuilder2.append(binder);
            stringBuilder2.append(" token=");
            stringBuilder2.append(token);
            Slog.w(str2, stringBuilder2.toString());
            return;
        }
        appToken.onRemovedFromDisplay();
    }

    Display getDisplay() {
        return this.mDisplay;
    }

    DisplayInfo getDisplayInfo() {
        return this.mDisplayInfo;
    }

    DisplayMetrics getDisplayMetrics() {
        return this.mDisplayMetrics;
    }

    int getRotation() {
        return this.mRotation;
    }

    @VisibleForTesting
    void setRotation(int newRotation) {
        this.mRotation = newRotation;
    }

    int getLastOrientation() {
        return this.mLastOrientation;
    }

    void setLastOrientation(int orientation) {
        this.mLastOrientation = orientation;
    }

    boolean getAltOrientation() {
        return this.mAltOrientation;
    }

    void setAltOrientation(boolean altOrientation) {
        this.mAltOrientation = altOrientation;
    }

    int getLastWindowForcedOrientation() {
        return this.mLastWindowForcedOrientation;
    }

    boolean updateRotationUnchecked() {
        return updateRotationUnchecked(false);
    }

    protected boolean updateRotationUnchecked(boolean forceUpdate) {
        if (!forceUpdate) {
            if (this.mService.mDeferredRotationPauseCount > 0) {
                Flog.i(308, "Deferring rotation, rotation is paused.");
                return false;
            }
            ScreenRotationAnimation screenRotationAnimation = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
            if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
                Flog.i(308, "Deferring rotation, animation in progress.");
                return false;
            } else if (this.mService.mDisplayFrozen) {
                Flog.i(308, "Deferring rotation, still finishing previous rotation");
                return false;
            }
        }
        if (this.mService.mDisplayEnabled) {
            int rotation;
            String str;
            StringBuilder stringBuilder;
            int oldRotation = this.mRotation;
            int lastOrientation = this.mLastOrientation;
            boolean oldAltOrientation = this.mAltOrientation;
            if (HwPCUtils.enabledInPad() && !this.isDefaultDisplay && "local:100000".equals(this.mDisplayInfo.uniqueId)) {
                rotation = 1;
            } else {
                rotation = this.mService.mPolicy.rotationForOrientationLw(lastOrientation, oldRotation, this.isDefaultDisplay);
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Computed rotation=");
                stringBuilder.append(rotation);
                stringBuilder.append(" for display id=");
                stringBuilder.append(this.mDisplayId);
                stringBuilder.append(" based on lastOrientation=");
                stringBuilder.append(lastOrientation);
                stringBuilder.append(" and oldRotation=");
                stringBuilder.append(oldRotation);
                Slog.v(str, stringBuilder.toString());
            }
            boolean mayRotateSeamlessly = this.mService.mPolicy.shouldRotateSeamlessly(oldRotation, rotation);
            if (mayRotateSeamlessly) {
                if (getWindow(-$$Lambda$DisplayContent$05CtqlkxQvjLanO8D5BmaCdILKQ.INSTANCE) != null && !forceUpdate) {
                    return false;
                }
                if (hasPinnedStack()) {
                    mayRotateSeamlessly = false;
                }
                for (int i = 0; i < this.mService.mSessions.size(); i++) {
                    if (((Session) this.mService.mSessions.valueAt(i)).hasAlertWindowSurfaces()) {
                        mayRotateSeamlessly = false;
                        break;
                    }
                }
            }
            boolean rotateSeamlessly = mayRotateSeamlessly;
            boolean altOrientation = this.mService.mPolicy.rotationHasCompatibleMetricsLw(lastOrientation, rotation) ^ 1;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Display id=");
                stringBuilder.append(this.mDisplayId);
                stringBuilder.append(" selected orientation ");
                stringBuilder.append(lastOrientation);
                stringBuilder.append(", got rotation ");
                stringBuilder.append(rotation);
                stringBuilder.append(" which has ");
                stringBuilder.append(altOrientation ? "incompatible" : "compatible");
                stringBuilder.append(" metrics");
                Slog.v(str, stringBuilder.toString());
            }
            StringBuilder stringBuilder2;
            if (oldRotation == rotation && oldAltOrientation == altOrientation) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No changes, Selected orientation ");
                stringBuilder2.append(lastOrientation);
                stringBuilder2.append(", got rotation ");
                stringBuilder2.append(rotation);
                stringBuilder2.append(" altOrientation ");
                stringBuilder2.append(altOrientation);
                Flog.i(308, stringBuilder2.toString());
                return false;
            }
            ScreenRotationAnimation screenRotationAnimation2;
            boolean lastOrientation2;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Display id=");
            stringBuilder2.append(this.mDisplayId);
            stringBuilder2.append(" rotation changed to ");
            stringBuilder2.append(rotation);
            stringBuilder2.append(altOrientation ? " (alt)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder2.append(" from ");
            stringBuilder2.append(oldRotation);
            stringBuilder2.append(oldAltOrientation ? " (alt)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder2.append(", lastOrientation=");
            stringBuilder2.append(lastOrientation);
            stringBuilder2.append(", rotateSeamlessly=");
            stringBuilder2.append(rotateSeamlessly);
            Flog.i(308, stringBuilder2.toString());
            if (deltaRotation(rotation, oldRotation) != 2) {
                this.mService.mWaitingForConfig = true;
            }
            if (!(this.mService == null || -1 == this.mService.getDockedStackSide())) {
                uploadOrientation(rotation);
            }
            this.mRotation = rotation;
            setDisplayRotationFR(this.mRotation);
            this.mAltOrientation = altOrientation;
            if (this.isDefaultDisplay) {
                this.mService.mPolicy.setRotationLw(rotation);
            }
            this.mService.mWindowsFreezingScreen = 1;
            this.mService.mH.removeMessages(11);
            this.mService.mH.sendEmptyMessageDelayed(11, 2000);
            setLayoutNeeded();
            int[] anim = new int[2];
            this.mService.mPolicy.selectRotationAnimationLw(anim);
            if (rotateSeamlessly) {
                screenRotationAnimation2 = null;
                this.mService.startSeamlessRotation();
            } else {
                this.mService.startFreezingDisplayLocked(anim[0], anim[1], this);
                screenRotationAnimation2 = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
            }
            ScreenRotationAnimation screenRotationAnimation3 = screenRotationAnimation2;
            updateDisplayAndOrientation(getConfiguration().uiMode);
            if (screenRotationAnimation3 == null || !screenRotationAnimation3.hasScreenshot()) {
                boolean z = oldAltOrientation;
                int[] iArr = anim;
                boolean z2 = altOrientation;
                lastOrientation2 = true;
                oldAltOrientation = rotateSeamlessly;
            } else {
                lastOrientation2 = true;
                oldAltOrientation = rotateSeamlessly;
                if (screenRotationAnimation3.setRotation(getPendingTransaction(), rotation, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, this.mService.getTransitionAnimationScaleLocked(), this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight)) {
                    this.mService.scheduleAnimationLocked();
                }
            }
            if (oldAltOrientation) {
                forAllWindows((Consumer) new -$$Lambda$DisplayContent$1GkjuS_pAduq4UeRzcVLneyJbGM(this, oldRotation, rotation), lastOrientation2);
            }
            this.mService.mDisplayManagerInternal.performTraversal(getPendingTransaction());
            scheduleAnimation();
            forAllWindows((Consumer) new -$$Lambda$DisplayContent$mKe0fxS63Jo2y7lFQaTOMepRJDc(this, oldAltOrientation), lastOrientation2);
            if (oldAltOrientation) {
                this.mService.mH.removeMessages(54);
                this.mService.mH.sendEmptyMessageDelayed(54, 2000);
            }
            int i2 = this.mService.mRotationWatchers.size() - lastOrientation2;
            while (true) {
                int i3 = i2;
                if (i3 < 0) {
                    break;
                }
                RotationWatcher rotationWatcher = (RotationWatcher) this.mService.mRotationWatchers.get(i3);
                if (rotationWatcher.mDisplayId == this.mDisplayId) {
                    try {
                        rotationWatcher.mWatcher.onRotationChanged(rotation);
                    } catch (RemoteException e) {
                    }
                }
                i2 = i3 - 1;
            }
            if (screenRotationAnimation3 == null && this.mService.mAccessibilityController != null && this.isDefaultDisplay) {
                this.mService.mAccessibilityController.onRotationChangedLocked(this);
            }
            this.mService.mPolicy.notifyRotationChange(this.mRotation);
            WindowManagerService windowManagerService = this.mService;
            if (WindowManagerService.mSupporInputMethodFilletAdaptation && rotation == 0 && !this.mService.mPolicy.isNavBarVisible() && this.mService.mInputMethodWindow != null && this.mService.mInputMethodWindow.isImeWithHwFlag() && (this.mService.mInputMethodWindow.getAttrs().hwFlags & DumpState.DUMP_DEXOPT) == 0 && this.mService.mInputMethodWindow.isVisible()) {
                this.mService.mH.post(new Runnable() {
                    public void run() {
                        synchronized (DisplayContent.this.mService.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                if (DisplayContent.this.mService.mInputMethodWindow != null) {
                                    DisplayContent.this.mService.mInputMethodWindow.showInsetSurfaceOverlayImmediately();
                                }
                            } finally {
                                while (true) {
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                    }
                });
            }
            return lastOrientation2;
        }
        Flog.i(308, "Deferring rotation, display is not enabled.");
        return false;
    }

    public static /* synthetic */ void lambda$updateRotationUnchecked$11(DisplayContent displayContent, boolean rotateSeamlessly, WindowState w) {
        if (w.mHasSurface && !rotateSeamlessly) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Set mOrientationChanging of ");
                stringBuilder.append(w);
                Slog.v(str, stringBuilder.toString());
            }
            w.setOrientationChanging(true);
            displayContent.mService.mRoot.mOrientationChangeComplete = false;
            w.mLastFreezeDuration = 0;
        }
        w.mReportOrientationChanged = true;
    }

    void configureDisplayPolicy() {
        this.mService.mPolicy.setInitialDisplaySize(getDisplay(), this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mBaseDisplayDensity);
        this.mDisplayFrames.onDisplayInfoUpdated(this.mDisplayInfo, calculateDisplayCutoutForRotation(this.mDisplayInfo.rotation));
    }

    private DisplayInfo updateDisplayAndOrientation(int uiMode) {
        int maxw;
        boolean z = true;
        if (!(this.mRotation == 1 || this.mRotation == 3)) {
            z = false;
        }
        boolean rotated = z;
        int realdw = rotated ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int realdh = rotated ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        int dw = realdw;
        int dh = realdh;
        if (this.mAltOrientation) {
            if (realdw > realdh) {
                maxw = (int) (((float) realdh) / 1.3f);
                if (maxw < realdw) {
                    dw = maxw;
                }
            } else {
                maxw = (int) (((float) realdw) / 1.3f);
                if (maxw < realdh) {
                    dh = maxw;
                }
            }
        }
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(this.mRotation).getDisplayCutout();
        int i = dw;
        int i2 = dh;
        int i3 = uiMode;
        int appWidth = this.mService.mPolicy.getNonDecorDisplayWidth(i, i2, this.mRotation, i3, this.mDisplayId, displayCutout);
        maxw = this.mService.mPolicy.getNonDecorDisplayHeight(i, i2, this.mRotation, i3, this.mDisplayId, displayCutout);
        this.mDisplayInfo.rotation = this.mRotation;
        this.mDisplayInfo.logicalWidth = dw;
        this.mDisplayInfo.logicalHeight = dh;
        this.mDisplayInfo.logicalDensityDpi = this.mBaseDisplayDensity;
        this.mDisplayInfo.appWidth = appWidth;
        this.mDisplayInfo.appHeight = maxw;
        Configuration configuration = null;
        if (this.isDefaultDisplay) {
            this.mDisplayInfo.getLogicalMetrics(this.mRealDisplayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, null);
        }
        this.mDisplayInfo.displayCutout = displayCutout.isEmpty() ? null : displayCutout;
        this.mDisplayInfo.getAppMetrics(this.mDisplayMetrics);
        DisplayInfo displayInfo;
        if (this.mDisplayScalingDisabled) {
            displayInfo = this.mDisplayInfo;
            displayInfo.flags |= 1073741824;
        } else {
            displayInfo = this.mDisplayInfo;
            displayInfo.flags &= -1073741825;
        }
        if (this.mShouldOverrideDisplayConfiguration) {
            configuration = this.mDisplayInfo;
        }
        this.mService.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(this.mDisplayId, configuration);
        this.mBaseDisplayRect.set(0, 0, dw, dh);
        if (this.isDefaultDisplay) {
            DisplayMetrics dm = this.mDisplayMetrics;
            this.mCompatibleScreenScale = CompatibilityInfo.computeCompatibleScaling(this.mDisplayMetrics, this.mCompatDisplayMetrics);
        }
        updateBounds();
        return this.mDisplayInfo;
    }

    WmDisplayCutout calculateDisplayCutoutForRotation(int rotation) {
        return (WmDisplayCutout) this.mDisplayCutoutCache.getOrCompute(this.mInitialDisplayCutout, rotation);
    }

    private WmDisplayCutout calculateDisplayCutoutForRotationUncached(DisplayCutout cutout, int rotation) {
        if (cutout == null || cutout == DisplayCutout.NO_CUTOUT) {
            return WmDisplayCutout.NO_CUTOUT;
        }
        if (rotation == 0) {
            return WmDisplayCutout.computeSafeInsets(cutout, this.mBaseDisplayWidth, this.mBaseDisplayHeight);
        }
        boolean rotated = true;
        if (!(rotation == 1 || rotation == 3)) {
            rotated = false;
        }
        Path bounds = cutout.getBounds().getBoundaryPath();
        CoordinateTransforms.transformPhysicalToLogicalCoordinates(rotation, this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mTmpMatrix);
        bounds.transform(this.mTmpMatrix);
        return WmDisplayCutout.computeSafeInsets(DisplayCutout.fromBounds(bounds), rotated ? this.mBaseDisplayHeight : this.mBaseDisplayWidth, rotated ? this.mBaseDisplayWidth : this.mBaseDisplayHeight);
    }

    /* JADX WARNING: Removed duplicated region for block: B:72:0x0158 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x0153  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void computeScreenConfiguration(Configuration config) {
        Configuration configuration = config;
        DisplayInfo displayInfo = updateDisplayAndOrientation(configuration.uiMode);
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        configuration.orientation = dw <= dh ? 1 : 2;
        configuration.windowConfiguration.setWindowingMode(1);
        float density = this.mDisplayMetrics.density;
        int i = dw;
        int i2 = dh;
        configuration.screenWidthDp = (int) (((float) this.mService.mPolicy.getConfigDisplayWidth(i, i2, displayInfo.rotation, configuration.uiMode, this.mDisplayId, displayInfo.displayCutout)) / density);
        configuration.screenHeightDp = (int) (((float) this.mService.mPolicy.getConfigDisplayHeight(i, i2, displayInfo.rotation, configuration.uiMode, this.mDisplayId, displayInfo.displayCutout)) / density);
        this.mService.mPolicy.getNonDecorInsetsLw(displayInfo.rotation, dw, dh, displayInfo.displayCutout, this.mTmpRect);
        int leftInset = this.mTmpRect.left;
        int topInset = this.mTmpRect.top;
        configuration.windowConfiguration.setAppBounds(leftInset, topInset, displayInfo.appWidth + leftInset, displayInfo.appHeight + topInset);
        boolean z = false;
        boolean rotated = displayInfo.rotation == 1 || displayInfo.rotation == 3;
        Object obj = 3;
        computeSizeRangesAndScreenLayout(displayInfo, this.mDisplayId, rotated, configuration.uiMode, dw, dh, density, configuration);
        int i3 = configuration.screenLayout & -769;
        if ((displayInfo.flags & 16) != 0) {
            i = 512;
        } else {
            i = 256;
        }
        configuration.screenLayout = i3 | i;
        configuration.compatScreenWidthDp = (int) (((float) configuration.screenWidthDp) / this.mCompatibleScreenScale);
        configuration.compatScreenHeightDp = (int) (((float) configuration.screenHeightDp) / this.mCompatibleScreenScale);
        i = 1;
        configuration.compatSmallestScreenWidthDp = computeCompatSmallestWidth(rotated, configuration.uiMode, dw, dh, this.mDisplayId);
        configuration.densityDpi = displayInfo.logicalDensityDpi;
        if (displayInfo.isHdr()) {
            i3 = 8;
        } else {
            i3 = 4;
        }
        i2 = (displayInfo.isWideColorGamut() && this.mService.hasWideColorGamutSupport()) ? 2 : i;
        configuration.colorMode = i3 | i2;
        configuration.touchscreen = i;
        configuration.keyboard = i;
        configuration.navigation = i;
        i2 = 0;
        InputDevice[] devices = this.mService.mInputManager.getInputDevices();
        int len = devices != null ? devices.length : 0;
        topInset = 0;
        i3 = 0;
        while (i3 < len) {
            InputDevice device = devices[i3];
            if (!device.isVirtual()) {
                int sources = device.getSources();
                int presenceFlag = device.isExternal() ? 2 : i;
                if (!this.mService.mIsTouchDevice) {
                    Object obj2 = obj;
                    configuration.touchscreen = 1;
                } else if ((sources & UsbACInterface.FORMAT_II_AC3) == UsbACInterface.FORMAT_II_AC3) {
                    configuration.touchscreen = 3;
                }
                if ((sources & 65540) == 65540) {
                    configuration.navigation = 3;
                    i2 |= presenceFlag;
                } else if ((sources & UsbTerminalTypes.TERMINAL_IN_MIC) == UsbTerminalTypes.TERMINAL_IN_MIC && configuration.navigation == 1) {
                    i = 2;
                    configuration.navigation = 2;
                    i2 |= presenceFlag;
                    if (device.getKeyboardType() != i) {
                        configuration.keyboard = i;
                        topInset |= presenceFlag;
                    }
                }
                i = 2;
                if (device.getKeyboardType() != i) {
                }
            }
            i3++;
            i = 1;
            obj = 3;
        }
        if (configuration.navigation == 1 && this.mService.mHasPermanentDpad) {
            configuration.navigation = 2;
            i2 |= 1;
        }
        if (configuration.keyboard != 1) {
            z = true;
        }
        boolean hardKeyboardAvailable = z;
        if (hardKeyboardAvailable != this.mService.mHardKeyboardAvailable) {
            this.mService.mHardKeyboardAvailable = hardKeyboardAvailable;
            this.mService.mH.removeMessages(22);
            this.mService.mH.sendEmptyMessage(22);
        }
        configuration.keyboardHidden = 1;
        configuration.hardKeyboardHidden = 1;
        configuration.navigationHidden = 1;
        this.mService.mPolicy.adjustConfigurationLw(configuration, topInset, i2);
    }

    private int computeCompatSmallestWidth(boolean rotated, int uiMode, int dw, int dh, int displayId) {
        int unrotDw;
        int unrotDh;
        this.mTmpDisplayMetrics.setTo(this.mDisplayMetrics);
        DisplayMetrics tmpDm = this.mTmpDisplayMetrics;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDh = dh;
            unrotDw = dw;
        }
        int sw = reduceCompatConfigWidthSize(0, 0, uiMode, tmpDm, unrotDw, unrotDh, displayId);
        int i = uiMode;
        DisplayMetrics tmpDm2 = tmpDm;
        int i2 = displayId;
        DisplayMetrics displayMetrics = tmpDm2;
        return reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(sw, 1, i, tmpDm, unrotDh, unrotDw, i2), 2, i, displayMetrics, unrotDw, unrotDh, i2), 3, i, displayMetrics, unrotDh, unrotDw, i2);
    }

    private int reduceCompatConfigWidthSize(int curSize, int rotation, int uiMode, DisplayMetrics dm, int dw, int dh, int displayId) {
        int i = dw;
        int i2 = dh;
        int i3 = rotation;
        int i4 = uiMode;
        int i5 = displayId;
        dm.noncompatWidthPixels = this.mService.mPolicy.getNonDecorDisplayWidth(i, i2, i3, i4, i5, this.mDisplayInfo.displayCutout);
        dm.noncompatHeightPixels = this.mService.mPolicy.getNonDecorDisplayHeight(i, i2, i3, i4, i5, this.mDisplayInfo.displayCutout);
        int size = (int) (((((float) dm.noncompatWidthPixels) / CompatibilityInfo.computeCompatibleScaling(dm, 0.0f)) / dm.density) + 1056964608);
        if (curSize == 0 || size < curSize) {
            return size;
        }
        return curSize;
    }

    private void computeSizeRangesAndScreenLayout(DisplayInfo displayInfo, int displayId, boolean rotated, int uiMode, int dw, int dh, float density, Configuration outConfig) {
        int unrotDw;
        int unrotDh;
        DisplayInfo displayInfo2 = displayInfo;
        Configuration configuration = outConfig;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDh = dh;
            unrotDw = dw;
        }
        displayInfo2.smallestNominalAppWidth = 1073741824;
        displayInfo2.smallestNominalAppHeight = 1073741824;
        displayInfo2.largestNominalAppWidth = 0;
        displayInfo2.largestNominalAppHeight = 0;
        adjustDisplaySizeRanges(displayInfo2, displayId, 0, uiMode, unrotDw, unrotDh);
        DisplayInfo displayInfo3 = displayInfo2;
        int i = displayId;
        int i2 = uiMode;
        adjustDisplaySizeRanges(displayInfo3, i, 1, i2, unrotDh, unrotDw);
        adjustDisplaySizeRanges(displayInfo3, i, 2, i2, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo3, i, 3, i2, unrotDh, unrotDw);
        float f = density;
        int i3 = uiMode;
        int i4 = displayId;
        int sl = reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(Configuration.resetScreenLayout(configuration.screenLayout), 0, f, unrotDw, unrotDh, i3, i4), 1, f, unrotDh, unrotDw, i3, i4), 2, f, unrotDw, unrotDh, i3, i4), 3, f, unrotDh, unrotDw, i3, i4);
        configuration.smallestScreenWidthDp = (int) (((float) displayInfo2.smallestNominalAppWidth) / density);
        configuration.screenLayout = sl;
    }

    private int reduceConfigLayout(int curLayout, int rotation, float density, int dw, int dh, int uiMode, int displayId) {
        int w = this.mService.mPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode, displayId, this.mDisplayInfo.displayCutout);
        int longSize = w;
        int shortSize = this.mService.mPolicy.getNonDecorDisplayHeight(dw, dh, rotation, uiMode, displayId, this.mDisplayInfo.displayCutout);
        if (longSize < shortSize) {
            int tmp = longSize;
            longSize = shortSize;
            shortSize = tmp;
        }
        return Configuration.reduceScreenLayout(curLayout, (int) (((float) longSize) / density), (int) (((float) shortSize) / density));
    }

    private void adjustDisplaySizeRanges(DisplayInfo displayInfo, int displayId, int rotation, int uiMode, int dw, int dh) {
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
        int width = this.mService.mPolicy.getConfigDisplayWidth(dw, dh, rotation, uiMode, displayId, displayCutout);
        if (width < displayInfo.smallestNominalAppWidth) {
            displayInfo.smallestNominalAppWidth = width;
        }
        if (width > displayInfo.largestNominalAppWidth) {
            displayInfo.largestNominalAppWidth = width;
        }
        int height = this.mService.mPolicy.getConfigDisplayHeight(dw, dh, rotation, uiMode, displayId, displayCutout);
        if (height < displayInfo.smallestNominalAppHeight) {
            displayInfo.smallestNominalAppHeight = height;
        }
        if (height > displayInfo.largestNominalAppHeight) {
            displayInfo.largestNominalAppHeight = height;
        }
    }

    DockedStackDividerController getDockedDividerController() {
        return this.mDividerControllerLocked;
    }

    PinnedStackController getPinnedStackController() {
        return this.mPinnedStackControllerLocked;
    }

    boolean hasAccess(int uid) {
        return this.mDisplay.hasAccess(uid);
    }

    boolean isPrivate() {
        return (this.mDisplay.getFlags() & 4) != 0;
    }

    TaskStack getHomeStack() {
        return this.mTaskStackContainers.getHomeStack();
    }

    TaskStack getSplitScreenPrimaryStack() {
        TaskStack stack = this.mTaskStackContainers.getSplitScreenPrimaryStack();
        return (stack == null || !stack.isVisible()) ? null : stack;
    }

    TaskStack getSplitScreenPrimaryStackIgnoringVisibility() {
        return this.mTaskStackContainers.getSplitScreenPrimaryStack();
    }

    TaskStack getPinnedStack() {
        return this.mTaskStackContainers.getPinnedStack();
    }

    private boolean hasPinnedStack() {
        return this.mTaskStackContainers.getPinnedStack() != null;
    }

    TaskStack getTopStackInWindowingMode(int windowingMode) {
        return getStack(windowingMode, 0);
    }

    TaskStack getStack(int windowingMode, int activityType) {
        return this.mTaskStackContainers.getStack(windowingMode, activityType);
    }

    @VisibleForTesting
    TaskStack getTopStack() {
        return this.mTaskStackContainers.getTopStack();
    }

    ArrayList<Task> getVisibleTasks() {
        return this.mTaskStackContainers.getVisibleTasks();
    }

    void onStackWindowingModeChanged(TaskStack stack) {
        this.mTaskStackContainers.onStackWindowingModeChanged(stack);
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        if (HwPCUtils.enabledInPad() && !HwPCUtils.isPcCastModeInServer() && !this.isDefaultDisplay && "local:100000".equals(this.mDisplayInfo.uniqueId) && this.mDisplayInfo.rotation == 1) {
            Slog.v(TAG, "onConfigurationChanged() not handle");
            return;
        }
        super.onConfigurationChanged(newParentConfig);
        this.mService.reconfigureDisplayLocked(this);
        if (getDockedDividerController() != null) {
            getDockedDividerController().onConfigurationChanged();
        }
        if (getPinnedStackController() != null) {
            getPinnedStackController().onConfigurationChanged();
        }
    }

    void updateStackBoundsAfterConfigChange(List<TaskStack> changedStackList) {
        for (int i = this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
            TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(i);
            if (stack.updateBoundsAfterConfigChange()) {
                changedStackList.add(stack);
            }
        }
        if (!hasPinnedStack()) {
            this.mPinnedStackControllerLocked.onDisplayInfoChanged();
        }
    }

    boolean fillsParent() {
        return true;
    }

    boolean isVisible() {
        return true;
    }

    void onAppTransitionDone() {
        super.onAppTransitionDone();
        this.mService.mWindowsChanged = true;
    }

    boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        int i;
        if (traverseTopToBottom) {
            for (i = this.mChildren.size() - 1; i >= 0; i--) {
                DisplayChildWindowContainer child = (DisplayChildWindowContainer) this.mChildren.get(i);
                if ((child != this.mImeWindowsContainers || this.mService.mInputMethodTarget == null) && child.forAllWindows((ToBooleanFunction) callback, traverseTopToBottom)) {
                    return true;
                }
            }
        } else {
            i = this.mChildren.size();
            for (int i2 = 0; i2 < i; i2++) {
                DisplayChildWindowContainer child2 = (DisplayChildWindowContainer) this.mChildren.get(i2);
                if ((child2 != this.mImeWindowsContainers || this.mService.mInputMethodTarget == null) && child2.forAllWindows((ToBooleanFunction) callback, traverseTopToBottom)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean forAllImeWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        return this.mImeWindowsContainers.forAllWindows((ToBooleanFunction) callback, traverseTopToBottom);
    }

    int getOrientation() {
        WindowManagerPolicy policy = this.mService.mPolicy;
        String str;
        StringBuilder stringBuilder;
        if (!this.mService.mDisplayFrozen) {
            int orientation = this.mAboveAppWindowsContainers.getOrientation();
            if (orientation != -2) {
                return orientation;
            }
        } else if (this.mLastWindowForcedOrientation != -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Display id=");
            stringBuilder.append(this.mDisplayId);
            stringBuilder.append(" is frozen, return ");
            stringBuilder.append(this.mLastWindowForcedOrientation);
            Slog.v(str, stringBuilder.toString());
            return this.mLastWindowForcedOrientation;
        } else if (policy.isKeyguardLocked()) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Display id=");
                stringBuilder.append(this.mDisplayId);
                stringBuilder.append(" is frozen while keyguard locked, return ");
                stringBuilder.append(this.mLastOrientation);
                Slog.v(str, stringBuilder.toString());
            }
            return this.mLastOrientation;
        }
        return this.mTaskStackContainers.getOrientation();
    }

    void updateDisplayInfo() {
        updateBaseDisplayMetricsIfNeeded();
        this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        for (int i = this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(i)).updateDisplayInfo(null);
        }
    }

    void initializeDisplayBaseInfo() {
        DisplayManagerInternal displayManagerInternal = this.mService.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            DisplayInfo newDisplayInfo = displayManagerInternal.getDisplayInfo(this.mDisplayId);
            if (newDisplayInfo != null) {
                this.mDisplayInfo.copyFrom(newDisplayInfo);
            }
        }
        updateBaseDisplayMetrics(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight, this.mDisplayInfo.logicalDensityDpi);
        this.mInitialDisplayWidth = this.mDisplayInfo.logicalWidth;
        this.mInitialDisplayHeight = this.mDisplayInfo.logicalHeight;
        this.mInitialDisplayDensity = this.mDisplayInfo.logicalDensityDpi;
        this.mInitialDisplayCutout = this.mDisplayInfo.displayCutout;
    }

    private void updateBaseDisplayMetricsIfNeeded() {
        DisplayInfo newDisplayInfo = new DisplayInfo();
        this.mService.mDisplayManagerInternal.getNonOverrideDisplayInfo(this.mDisplayId, newDisplayInfo);
        int orientation = newDisplayInfo.rotation;
        boolean isDisplayDensityForced = false;
        boolean rotated = orientation == 1 || orientation == 3;
        int newWidth = rotated ? newDisplayInfo.logicalHeight : newDisplayInfo.logicalWidth;
        int newHeight = rotated ? newDisplayInfo.logicalWidth : newDisplayInfo.logicalHeight;
        int newDensity = newDisplayInfo.logicalDensityDpi;
        DisplayCutout newCutout = newDisplayInfo.displayCutout;
        boolean displayMetricsChanged = (this.mInitialDisplayWidth == newWidth && this.mInitialDisplayHeight == newHeight && this.mInitialDisplayDensity == newDensity && Objects.equals(this.mInitialDisplayCutout, newCutout)) ? false : true;
        if (displayMetricsChanged) {
            int i;
            int i2;
            int i3;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("the display metrics changed.  mInitialDisplayWidth = ");
            stringBuilder.append(this.mInitialDisplayWidth);
            stringBuilder.append("; mInitialDisplayHeight = ");
            stringBuilder.append(this.mInitialDisplayHeight);
            stringBuilder.append("; mInitialDisplayDensity = ");
            stringBuilder.append(this.mInitialDisplayDensity);
            stringBuilder.append("; newWidth = ");
            stringBuilder.append(newWidth);
            stringBuilder.append("; newHeight = ");
            stringBuilder.append(newHeight);
            stringBuilder.append("; newDensity = ");
            stringBuilder.append(newDensity);
            stringBuilder.append("; mBaseDisplayWidth = ");
            stringBuilder.append(this.mBaseDisplayWidth);
            stringBuilder.append("; mBaseDisplayHeight = ");
            stringBuilder.append(this.mBaseDisplayHeight);
            stringBuilder.append("; mBaseDisplayDensity = ");
            stringBuilder.append(this.mBaseDisplayDensity);
            stringBuilder.append("; rotated = ");
            stringBuilder.append(rotated);
            stringBuilder.append("; mDisplayId = ");
            stringBuilder.append(this.mDisplayId);
            Slog.v(str, stringBuilder.toString());
            boolean isDisplaySizeForced = (this.mBaseDisplayWidth == this.mInitialDisplayWidth && this.mBaseDisplayHeight == this.mInitialDisplayHeight) ? false : true;
            if (this.mBaseDisplayDensity != this.mInitialDisplayDensity) {
                isDisplayDensityForced = true;
            }
            if (isDisplaySizeForced) {
                i = this.mBaseDisplayWidth;
            } else {
                i = newWidth;
            }
            if (isDisplaySizeForced) {
                i2 = this.mBaseDisplayHeight;
            } else {
                i2 = newHeight;
            }
            if (isDisplayDensityForced) {
                i3 = this.mBaseDisplayDensity;
            } else {
                i3 = newDensity;
            }
            updateBaseDisplayMetrics(i, i2, i3);
            this.mInitialDisplayWidth = newWidth;
            this.mInitialDisplayHeight = newHeight;
            this.mInitialDisplayDensity = newDensity;
            this.mInitialDisplayCutout = newCutout;
            this.mService.reconfigureDisplayLocked(this);
        }
    }

    void setMaxUiWidth(int width) {
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting max ui width:");
            stringBuilder.append(width);
            stringBuilder.append(" on display:");
            stringBuilder.append(getDisplayId());
            Slog.v(str, stringBuilder.toString());
        }
        this.mMaxUiWidth = width;
        updateBaseDisplayMetrics(this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mBaseDisplayDensity);
    }

    void updateBaseDisplayMetrics(int baseWidth, int baseHeight, int baseDensity) {
        this.mBaseDisplayWidth = baseWidth;
        this.mBaseDisplayHeight = baseHeight;
        this.mBaseDisplayDensity = baseDensity;
        if (this.mMaxUiWidth > 0 && this.mBaseDisplayWidth > this.mMaxUiWidth) {
            this.mBaseDisplayHeight = (this.mMaxUiWidth * this.mBaseDisplayHeight) / this.mBaseDisplayWidth;
            this.mBaseDisplayDensity = (this.mMaxUiWidth * this.mBaseDisplayDensity) / this.mBaseDisplayWidth;
            this.mBaseDisplayWidth = this.mMaxUiWidth;
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Applying config restraints:");
                stringBuilder.append(this.mBaseDisplayWidth);
                stringBuilder.append("x");
                stringBuilder.append(this.mBaseDisplayHeight);
                stringBuilder.append(" at density:");
                stringBuilder.append(this.mBaseDisplayDensity);
                stringBuilder.append(" on display:");
                stringBuilder.append(getDisplayId());
                Slog.v(str, stringBuilder.toString());
            }
        }
        this.mBaseDisplayRect.set(0, 0, this.mBaseDisplayWidth, this.mBaseDisplayHeight);
        updateBounds();
        this.mDisplayCutoutCache.clearCacheTable();
    }

    void getStableRect(Rect out) {
        out.set(this.mDisplayFrames.mStable);
    }

    TaskStack createStack(int stackId, boolean onTop, StackWindowController controller) {
        TaskStack stack;
        if (HwPCUtils.isExtDynamicStack(stackId)) {
            stack = HwServiceFactory.createTaskStack(this.mService, stackId, controller);
        } else {
            stack = new TaskStack(this.mService, stackId, controller);
        }
        this.mTaskStackContainers.addStackToDisplay(stack, onTop);
        return stack;
    }

    void moveStackToDisplay(TaskStack stack, boolean onTop) {
        DisplayContent prevDc = stack.getDisplayContent();
        StringBuilder stringBuilder;
        if (prevDc == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to move stackId=");
            stringBuilder.append(stack.mStackId);
            stringBuilder.append(" which is not currently attached to any display");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (prevDc.getDisplayId() != this.mDisplayId) {
            prevDc.mTaskStackContainers.removeChild(stack);
            this.mTaskStackContainers.addStackToDisplay(stack, onTop);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to move stackId=");
            stringBuilder.append(stack.mStackId);
            stringBuilder.append(" to its current displayId=");
            stringBuilder.append(this.mDisplayId);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    protected void addChild(DisplayChildWindowContainer child, Comparator<DisplayChildWindowContainer> comparator) {
        throw new UnsupportedOperationException("See DisplayChildWindowContainer");
    }

    protected void addChild(DisplayChildWindowContainer child, int index) {
        throw new UnsupportedOperationException("See DisplayChildWindowContainer");
    }

    protected void removeChild(DisplayChildWindowContainer child) {
        if (this.mRemovingDisplay) {
            super.removeChild(child);
            return;
        }
        throw new UnsupportedOperationException("See DisplayChildWindowContainer");
    }

    void positionChildAt(int position, DisplayChildWindowContainer child, boolean includingParents) {
        getParent().positionChildAt(position, this, includingParents);
    }

    void positionStackAt(int position, TaskStack child) {
        this.mTaskStackContainers.positionChildAt(position, child, false);
        layoutAndAssignWindowLayersIfNeeded();
    }

    int taskIdFromPoint(int x, int y) {
        int stackNdx = this.mTaskStackContainers.getChildCount();
        while (true) {
            stackNdx--;
            if (stackNdx < 0) {
                return -1;
            }
            int taskId = ((TaskStack) this.mTaskStackContainers.getChildAt(stackNdx)).taskIdFromPoint(x, y);
            if (taskId != -1) {
                return taskId;
            }
        }
    }

    Task findTaskForResizePoint(int x, int y) {
        int delta = WindowManagerService.dipToPixel(10, this.mDisplayMetrics);
        this.mTmpTaskForResizePointSearchResult.reset();
        int stackNdx = this.mTaskStackContainers.getChildCount();
        while (true) {
            stackNdx--;
            if (stackNdx < 0) {
                return null;
            }
            TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(stackNdx);
            if (stack != null) {
                if (!stack.getWindowConfiguration().canResizeTask()) {
                    return null;
                }
                stack.findTaskForResizePoint(x, y, delta, this.mTmpTaskForResizePointSearchResult);
                if (this.mTmpTaskForResizePointSearchResult.searchDone) {
                    return this.mTmpTaskForResizePointSearchResult.taskForResize;
                }
            }
        }
    }

    void setTouchExcludeRegion(Task focusedTask) {
        int stackNdx;
        if (focusedTask == null) {
            this.mTouchExcludeRegion.setEmpty();
        } else {
            this.mTouchExcludeRegion.set(this.mBaseDisplayRect);
            int delta = WindowManagerService.dipToPixel(10, this.mDisplayMetrics);
            this.mTmpRect2.setEmpty();
            for (stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ((TaskStack) this.mTaskStackContainers.getChildAt(stackNdx)).setTouchExcludeRegion(focusedTask, delta, this.mTouchExcludeRegion, this.mDisplayFrames.mContent, this.mTmpRect2);
            }
            if (!this.mTmpRect2.isEmpty()) {
                this.mTouchExcludeRegion.op(this.mTmpRect2, Op.UNION);
            }
        }
        if (HwPCUtils.isPcCastModeInServer()) {
            if (!this.isDefaultDisplay) {
                this.mTouchExcludeRegion.setEmpty();
            }
            this.mPcTouchExcludeRegion.setEmpty();
        }
        WindowState inputMethod = this.mService.mInputMethodWindow;
        if (inputMethod != null && inputMethod.isVisibleLw()) {
            inputMethod.getTouchableRegion(this.mTmpRegion);
            if (HwPCUtils.isPcCastModeInServer()) {
                if (inputMethod.getDisplayId() == this.mDisplayId) {
                    this.mPcTouchExcludeRegion.op(this.mTmpRegion, Op.UNION);
                }
            } else if (inputMethod.getDisplayId() == this.mDisplayId) {
                this.mTouchExcludeRegion.op(this.mTmpRegion, Op.UNION);
            } else {
                inputMethod.getDisplayContent().setTouchExcludeRegion(null);
            }
        }
        if (HwPCUtils.isPcCastModeInServer() && this.mDisplayId == 0) {
            TaskStack taskStack = (TaskStack) this.mTaskStackContainers.getTopChild();
            if (taskStack != null) {
                Task task = (Task) taskStack.getTopChild();
                if (task != null) {
                    AppWindowToken appWindowToken = (AppWindowToken) task.getTopChild();
                    if (!(appWindowToken == null || appWindowToken.appComponentName == null || !"com.huawei.desktop.systemui/com.huawei.systemui.mk.activity.ImitateActivity".equalsIgnoreCase(appWindowToken.appComponentName))) {
                        Rect touchpadBounds = new Rect();
                        task.getDimBounds(touchpadBounds);
                        this.mPcTouchExcludeRegion.op(touchpadBounds, Op.UNION);
                        WindowState touchpadState = (WindowState) appWindowToken.getTopChild();
                        if (touchpadState != null) {
                            WindowState popupWindowState = (WindowState) touchpadState.getTopChild();
                            if (popupWindowState != null && popupWindowState.mAttrs.type == 1000) {
                                Region popupRegion = new Region();
                                popupWindowState.getTouchableRegion(popupRegion);
                                this.mPcTouchExcludeRegion.op(popupRegion, Op.DIFFERENCE);
                            }
                        }
                    }
                }
            }
        }
        for (stackNdx = this.mTapExcludedWindows.size() - 1; stackNdx >= 0; stackNdx--) {
            WindowState win = (WindowState) this.mTapExcludedWindows.get(stackNdx);
            if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(this.mDisplayId) || win.isVisible()) {
                win.getTouchableRegion(this.mTmpRegion);
                this.mTouchExcludeRegion.op(this.mTmpRegion, Op.UNION);
                if (HwPCUtils.isPcCastModeInServer() && this.mDisplayId == 0 && win.isVisible() && !this.mPcTouchExcludeRegion.isEmpty()) {
                    this.mPcTouchExcludeRegion.op(this.mTmpRegion, Op.DIFFERENCE);
                }
            }
        }
        if (this.mTapDetector != null) {
            this.mTapDetector.setHwPCTouchExcludeRegion(this.mPcTouchExcludeRegion);
        }
        for (stackNdx = this.mTapExcludeProvidingWindows.size() - 1; stackNdx >= 0; stackNdx--) {
            ((WindowState) this.mTapExcludeProvidingWindows.valueAt(stackNdx)).amendTapExcludeRegion(this.mTouchExcludeRegion);
        }
        if (this.mDisplayId == 0 && getSplitScreenPrimaryStack() != null) {
            this.mDividerControllerLocked.getTouchRegion(this.mTmpRect);
            this.mTmpRegion.set(this.mTmpRect);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Op.UNION);
        }
        if (this.mTapDetector != null) {
            this.mTapDetector.setTouchExcludeRegion(this.mTouchExcludeRegion);
        }
    }

    void switchUser() {
        super.switchUser();
        this.mService.mWindowsChanged = true;
    }

    private void resetAnimationBackgroundAnimator() {
        for (int stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(stackNdx)).resetAnimationBackgroundAnimator();
        }
    }

    void removeIfPossible() {
        if (isAnimating()) {
            this.mDeferredRemoval = true;
        } else {
            removeImmediately();
        }
    }

    void removeImmediately() {
        boolean isVRDisplay = true;
        this.mRemovingDisplay = true;
        try {
            super.removeImmediately();
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Removing display=");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            boolean isPCDisplay = HwPCUtils.enabled() && this.mDisplayId != -1 && this.mDisplayId != 0 && (this.mDisplayInfo.type == 2 || this.mDisplayInfo.type == 3 || (((this.mDisplayInfo.type == 5 || this.mDisplayInfo.type == 4) && SystemProperties.getBoolean("hw_pc_support_overlay", false)) || (this.mDisplayInfo.type == 5 && ("com.hpplay.happycast".equals(this.mDisplayInfo.ownerPackageName) || "com.huawei.works".equals(this.mDisplayInfo.ownerPackageName)))));
            if (!(HwVRUtils.isVRMode() && HwVRUtils.isValidVRDisplayId(this.mDisplayId))) {
                isVRDisplay = false;
            }
            if (this.mService.canDispatchPointerEvents()) {
                if (!(this.mTapDetector == null || isPCDisplay || isVRDisplay)) {
                    this.mService.unregisterPointerEventListener(this.mTapDetector);
                }
                if (this.mDisplayId == 0 && this.mService.mMousePositionTracker != null) {
                    this.mService.unregisterPointerEventListener(this.mService.mMousePositionTracker);
                }
            }
            this.mService.mAnimator.removeDisplayLocked(this.mDisplayId);
            if (isPCDisplay && this.mDisplayId != 0 && this.mService.canDispatchExternalPointerEvents() && this.mTapDetector != null) {
                this.mService.unregisterExternalPointerEventListener(this.mTapDetector);
                try {
                    this.mService.unregisterExternalPointerEventListener(this.mService.mMousePositionTracker);
                } catch (Exception e) {
                }
            }
        } catch (IllegalStateException e2) {
            Slog.w(TAG, "TaskTapPointerEventListener  not registered");
        } catch (Throwable th) {
            this.mRemovingDisplay = false;
        }
        this.mRemovingDisplay = false;
        this.mService.onDisplayRemoved(this.mDisplayId);
    }

    boolean checkCompleteDeferredRemoval() {
        if (super.checkCompleteDeferredRemoval() || !this.mDeferredRemoval) {
            return true;
        }
        removeImmediately();
        return false;
    }

    boolean isRemovalDeferred() {
        return this.mDeferredRemoval;
    }

    boolean animateForIme(float interpolatedValue, float animationTarget, float dividerAnimationTarget) {
        boolean updated = false;
        for (int i = this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
            TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(i);
            if (stack != null && stack.isAdjustedForIme()) {
                if (interpolatedValue >= 1.0f && animationTarget == 0.0f && dividerAnimationTarget == 0.0f) {
                    stack.resetAdjustedForIme(true);
                    updated = true;
                } else {
                    this.mDividerControllerLocked.mLastAnimationProgress = this.mDividerControllerLocked.getInterpolatedAnimationValue(interpolatedValue);
                    this.mDividerControllerLocked.mLastDividerProgress = this.mDividerControllerLocked.getInterpolatedDividerValue(interpolatedValue);
                    updated |= stack.updateAdjustForIme(this.mDividerControllerLocked.mLastAnimationProgress, this.mDividerControllerLocked.mLastDividerProgress, false);
                }
                if (interpolatedValue >= 1.0f) {
                    stack.endImeAdjustAnimation();
                }
            }
        }
        return updated;
    }

    boolean clearImeAdjustAnimation() {
        boolean changed = false;
        for (int i = this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
            TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(i);
            if (stack != null && stack.isAdjustedForIme()) {
                stack.resetAdjustedForIme(true);
                changed = true;
            }
        }
        return changed;
    }

    void beginImeAdjustAnimation() {
        for (int i = this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
            TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(i);
            if (stack.isVisible() && stack.isAdjustedForIme()) {
                stack.beginImeAdjustAnimation();
            }
        }
    }

    void adjustForImeIfNeeded() {
        WindowState imeWin = this.mService.mInputMethodWindow;
        boolean z = imeWin != null && imeWin.isVisibleLw() && imeWin.isDisplayedLw() && !this.mDividerControllerLocked.isImeHideRequested();
        boolean imeVisible = z;
        boolean dockVisible = isStackVisible(3);
        TaskStack imeTargetStack = this.mService.getImeFocusStackLocked();
        int dockSide = (!dockVisible || imeTargetStack == null) ? -1 : imeTargetStack.getDockSide();
        int imeDockSide = dockSide;
        boolean imeOnTop = imeDockSide == 2;
        dockSide = 4;
        boolean imeOnBottom = imeDockSide == 4;
        boolean dockMinimized = this.mDividerControllerLocked.isMinimizedDock();
        int imeHeight = this.mDisplayFrames.getInputMethodWindowVisibleHeight();
        boolean z2 = imeVisible && imeHeight != this.mDividerControllerLocked.getImeHeightAdjustedFor();
        boolean imeHeightChanged = z2;
        z = this.mRotation == 1 || this.mRotation == 3;
        boolean rotated = z;
        int i;
        if (imeVisible && dockVisible && ((imeOnTop || imeOnBottom) && !dockMinimized)) {
            i = this.mTaskStackContainers.getChildCount() - 1;
            while (i >= 0) {
                TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(i);
                boolean isDockedOnBottom = stack.getDockSide() == dockSide;
                if (stack.isVisible() && ((imeOnBottom || isDockedOnBottom) && stack.inSplitScreenWindowingMode())) {
                    boolean z3 = imeOnBottom && imeHeightChanged;
                    stack.setAdjustedForIme(imeWin, z3);
                } else {
                    stack.resetAdjustedForIme(false);
                }
                i--;
                dockSide = 4;
            }
            this.mDividerControllerLocked.setAdjustedForIme(imeOnBottom, true, true, imeWin, imeHeight);
        } else {
            for (i = this.mTaskStackContainers.getChildCount() - 1; i >= 0; i--) {
                ((TaskStack) this.mTaskStackContainers.getChildAt(i)).resetAdjustedForIme(dockVisible ^ 1);
            }
            DockedStackDividerController dockedStackDividerController = this.mDividerControllerLocked;
            z2 = imeVisible && dockVisible && rotated;
            dockedStackDividerController.setAdjustedForIme(false, z2, dockVisible, imeWin, imeHeight);
        }
        this.mPinnedStackControllerLocked.setAdjustedForIme(imeVisible, imeHeight);
    }

    int getLayerForAnimationBackground(WindowStateAnimator winAnimator) {
        WindowState visibleWallpaper = this.mBelowAppWindowsContainers.getWindow(-$$Lambda$DisplayContent$Po0ivnfO2TfRfOth5ZIOFcmugs4.INSTANCE);
        if (visibleWallpaper != null) {
            return visibleWallpaper.mWinAnimator.mAnimLayer;
        }
        return winAnimator.mAnimLayer;
    }

    static /* synthetic */ boolean lambda$getLayerForAnimationBackground$12(WindowState w) {
        return w.mIsWallpaper && w.isVisibleNow();
    }

    void prepareFreezingTaskBounds() {
        for (int stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(stackNdx)).prepareFreezingTaskBounds();
        }
    }

    void rotateBounds(int oldRotation, int newRotation, Rect bounds) {
        getBounds(this.mTmpRect, newRotation);
        createRotationMatrix(deltaRotation(newRotation, oldRotation), (float) this.mTmpRect.width(), (float) this.mTmpRect.height(), this.mTmpMatrix);
        this.mTmpRectF.set(bounds);
        this.mTmpMatrix.mapRect(this.mTmpRectF);
        this.mTmpRectF.round(bounds);
    }

    static int deltaRotation(int oldRotation, int newRotation) {
        int delta = newRotation - oldRotation;
        if (delta < 0) {
            return delta + 4;
        }
        return delta;
    }

    private static void createRotationMatrix(int rotation, float displayWidth, float displayHeight, Matrix outMatrix) {
        createRotationMatrix(rotation, 0.0f, 0.0f, displayWidth, displayHeight, outMatrix);
    }

    static void createRotationMatrix(int rotation, float rectLeft, float rectTop, float displayWidth, float displayHeight, Matrix outMatrix) {
        switch (rotation) {
            case 0:
                outMatrix.reset();
                return;
            case 1:
                outMatrix.setRotate(90.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(displayWidth, 0.0f);
                outMatrix.postTranslate(-rectTop, rectLeft);
                return;
            case 2:
                outMatrix.reset();
                return;
            case 3:
                outMatrix.setRotate(270.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(0.0f, displayHeight);
                outMatrix.postTranslate(rectTop, 0.0f);
                return;
            default:
                return;
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        int stackNdx;
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, trim);
        proto.write(1120986464258L, this.mDisplayId);
        for (stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ((TaskStack) this.mTaskStackContainers.getChildAt(stackNdx)).writeToProto(proto, 2246267895811L, trim);
        }
        this.mDividerControllerLocked.writeToProto(proto, 1146756268036L);
        this.mPinnedStackControllerLocked.writeToProto(proto, 1146756268037L);
        for (stackNdx = this.mAboveAppWindowsContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ((WindowToken) this.mAboveAppWindowsContainers.getChildAt(stackNdx)).writeToProto(proto, 2246267895814L, trim);
        }
        for (stackNdx = this.mBelowAppWindowsContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ((WindowToken) this.mBelowAppWindowsContainers.getChildAt(stackNdx)).writeToProto(proto, 2246267895815L, trim);
        }
        for (stackNdx = this.mImeWindowsContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ((WindowToken) this.mImeWindowsContainers.getChildAt(stackNdx)).writeToProto(proto, 2246267895816L, trim);
        }
        proto.write(1120986464265L, this.mBaseDisplayDensity);
        this.mDisplayInfo.writeToProto(proto, 1146756268042L);
        proto.write(1120986464267L, this.mRotation);
        ScreenRotationAnimation screenRotationAnimation = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
        if (screenRotationAnimation != null) {
            screenRotationAnimation.writeToProto(proto, 1146756268044L);
        }
        this.mDisplayFrames.writeToProto(proto, 1146756268045L);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        int stackNdx;
        TaskStack stack;
        StringBuilder stringBuilder;
        super.dump(pw, prefix, dumpAll);
        pw.print(prefix);
        pw.print("Display: mDisplayId=");
        pw.println(this.mDisplayId);
        String subPrefix = new StringBuilder();
        subPrefix.append("  ");
        subPrefix.append(prefix);
        subPrefix = subPrefix.toString();
        pw.print(subPrefix);
        pw.print("init=");
        pw.print(this.mInitialDisplayWidth);
        pw.print("x");
        pw.print(this.mInitialDisplayHeight);
        pw.print(" ");
        pw.print(this.mInitialDisplayDensity);
        pw.print("dpi");
        if (!(this.mInitialDisplayWidth == this.mBaseDisplayWidth && this.mInitialDisplayHeight == this.mBaseDisplayHeight && this.mInitialDisplayDensity == this.mBaseDisplayDensity)) {
            pw.print(" base=");
            pw.print(this.mBaseDisplayWidth);
            pw.print("x");
            pw.print(this.mBaseDisplayHeight);
            pw.print(" ");
            pw.print(this.mBaseDisplayDensity);
            pw.print("dpi");
        }
        if (this.mDisplayScalingDisabled) {
            pw.println(" noscale");
        }
        pw.print(" cur=");
        pw.print(this.mDisplayInfo.logicalWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.logicalHeight);
        pw.print(" app=");
        pw.print(this.mDisplayInfo.appWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.appHeight);
        pw.print(" rng=");
        pw.print(this.mDisplayInfo.smallestNominalAppWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.smallestNominalAppHeight);
        pw.print("-");
        pw.print(this.mDisplayInfo.largestNominalAppWidth);
        pw.print("x");
        pw.println(this.mDisplayInfo.largestNominalAppHeight);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(subPrefix);
        stringBuilder2.append("deferred=");
        stringBuilder2.append(this.mDeferredRemoval);
        stringBuilder2.append(" mLayoutNeeded=");
        stringBuilder2.append(this.mLayoutNeeded);
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mTouchExcludeRegion=");
        stringBuilder2.append(this.mTouchExcludeRegion);
        pw.println(stringBuilder2.toString());
        pw.println();
        pw.print(prefix);
        pw.print("mLayoutSeq=");
        pw.println(this.mLayoutSeq);
        pw.println();
        pw.print(prefix);
        pw.print("mDeferUpdateImeTargetCount=");
        pw.println(this.mDeferUpdateImeTargetCount);
        pw.println();
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("Application tokens in top down Z order:");
        pw.println(stringBuilder2.toString());
        for (stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            stack = (TaskStack) this.mTaskStackContainers.getChildAt(stackNdx);
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            stack.dump(pw, stringBuilder.toString(), dumpAll);
        }
        pw.println();
        if (!this.mExitingTokens.isEmpty()) {
            pw.println();
            pw.println("  Exiting tokens:");
            for (stackNdx = this.mExitingTokens.size() - 1; stackNdx >= 0; stackNdx--) {
                WindowToken token = (WindowToken) this.mExitingTokens.get(stackNdx);
                pw.print("  Exiting #");
                pw.print(stackNdx);
                pw.print(' ');
                pw.print(token);
                pw.println(':');
                token.dump(pw, "    ", dumpAll);
            }
        }
        pw.println();
        TaskStack homeStack = getHomeStack();
        if (homeStack != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("homeStack=");
            stringBuilder3.append(homeStack.getName());
            pw.println(stringBuilder3.toString());
        }
        stack = getPinnedStack();
        if (stack != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("pinnedStack=");
            stringBuilder.append(stack.getName());
            pw.println(stringBuilder.toString());
        }
        TaskStack splitScreenPrimaryStack = getSplitScreenPrimaryStack();
        if (splitScreenPrimaryStack != null) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(prefix);
            stringBuilder4.append("splitScreenPrimaryStack=");
            stringBuilder4.append(splitScreenPrimaryStack.getName());
            pw.println(stringBuilder4.toString());
        }
        pw.println();
        this.mDividerControllerLocked.dump(prefix, pw);
        pw.println();
        this.mPinnedStackControllerLocked.dump(prefix, pw);
        pw.println();
        this.mDisplayFrames.dump(prefix, pw);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display ");
        stringBuilder.append(this.mDisplayId);
        stringBuilder.append(" info=");
        stringBuilder.append(this.mDisplayInfo);
        stringBuilder.append(" stacks=");
        stringBuilder.append(this.mChildren);
        return stringBuilder.toString();
    }

    String getName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display ");
        stringBuilder.append(this.mDisplayId);
        stringBuilder.append(" name=\"");
        stringBuilder.append(this.mDisplayInfo.name);
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    boolean isStackVisible(int windowingMode) {
        TaskStack stack = getTopStackInWindowingMode(windowingMode);
        return stack != null && stack.isVisible();
    }

    WindowState getTouchableWinAtPointLocked(float xf, float yf) {
        return getWindow(new -$$Lambda$DisplayContent$_XfE1uZ9VUv6i0SxWUvqu69FNb4(this, (int) xf, (int) yf));
    }

    public static /* synthetic */ boolean lambda$getTouchableWinAtPointLocked$13(DisplayContent displayContent, int x, int y, WindowState w) {
        int flags = w.mAttrs.flags;
        boolean z = false;
        if (!w.isVisibleLw() || (flags & 16) != 0) {
            return false;
        }
        w.getVisibleBounds(displayContent.mTmpRect);
        if (!displayContent.mTmpRect.contains(x, y)) {
            return false;
        }
        w.getTouchableRegion(displayContent.mTmpRegion);
        int touchFlags = flags & 40;
        if (displayContent.mTmpRegion.contains(x, y) || touchFlags == 0) {
            z = true;
        }
        return z;
    }

    boolean canAddToastWindowForUid(int uid) {
        boolean z = true;
        if (getWindow(new -$$Lambda$DisplayContent$2VlyMN8z2sOPqE9-yf-z3-peRMI(uid)) != null) {
            return true;
        }
        if (getWindow(new -$$Lambda$DisplayContent$JYsrGdifTPH6ASJDC3B9YWMD2pw(uid)) != null) {
            z = false;
        }
        return z;
    }

    static /* synthetic */ boolean lambda$canAddToastWindowForUid$14(int uid, WindowState w) {
        return w.mOwnerUid == uid && w.isFocused();
    }

    static /* synthetic */ boolean lambda$canAddToastWindowForUid$15(int uid, WindowState w) {
        return w.mAttrs.type == 2005 && w.mOwnerUid == uid && !w.mPermanentlyHidden && !w.mWindowRemovalAllowed;
    }

    void scheduleToastWindowsTimeoutIfNeededLocked(WindowState oldFocus, WindowState newFocus) {
        if (oldFocus != null && (newFocus == null || newFocus.mOwnerUid != oldFocus.mOwnerUid)) {
            this.mTmpWindow = oldFocus;
            forAllWindows(this.mScheduleToastTimeout, false);
        }
    }

    WindowState findFocusedWindow() {
        this.mTmpWindow = null;
        forAllWindows(this.mFindFocusedWindow, true);
        if (this.mTmpWindow != null) {
            return this.mTmpWindow;
        }
        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
            Slog.v(TAG, "findFocusedWindow: No focusable windows.");
        }
        return null;
    }

    void checkNeedNotifyFingerWinCovered() {
        if (this.mObserveWin != null && this.mObserveWin.isVisibleOrAdding()) {
            boolean fingerWinCovered;
            boolean needNotify;
            for (int i = this.mAboveAppWindowsContainers.getChildCount() - 1; i >= 0; i--) {
                WindowToken windowToken = (WindowToken) this.mAboveAppWindowsContainers.getChildAt(i);
                if (windowToken.mChildren.contains(this.mObserveWin)) {
                    this.mObserveToken = windowToken;
                } else if (!(windowToken.getTopChild() == null || !((WindowState) windowToken.getTopChild()).isVisibleOrAdding() || ((WindowState) windowToken.getTopChild()).getAttrs().type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || ((WindowState) windowToken.getTopChild()).getAttrs().type == 2019 || (this.mTopAboveAppToken != null && this.mTopAboveAppToken.getLayer() >= windowToken.getLayer()))) {
                    this.mTopAboveAppToken = windowToken;
                }
            }
            if (this.mObserveToken == null || this.mTopAboveAppToken == null || this.mObserveToken.getLayer() >= this.mTopAboveAppToken.getLayer()) {
                fingerWinCovered = false;
                needNotify = this.mWinEverCovered;
                this.mWinEverCovered = false;
            } else {
                fingerWinCovered = true;
                this.mWinEverCovered = true;
                needNotify = true;
            }
            if (needNotify) {
                Rect winFrame = new Rect();
                if (this.mTopAboveAppToken != null) {
                    WindowState topAboveAppWindow = (WindowState) this.mTopAboveAppToken.getTopChild();
                    if (topAboveAppWindow != null) {
                        winFrame = topAboveAppWindow.getVisibleFrameLw();
                    }
                }
                this.mService.notifyFingerWinCovered(fingerWinCovered, winFrame);
            }
        }
    }

    void assignWindowLayers(boolean setLayoutNeeded) {
        Trace.traceBegin(32, "assignWindowLayers");
        assignChildLayers(getPendingTransaction());
        if (setLayoutNeeded) {
            setLayoutNeeded();
        }
        scheduleAnimation();
        Trace.traceEnd(32);
    }

    void layoutAndAssignWindowLayersIfNeeded() {
        this.mService.mWindowsChanged = true;
        setLayoutNeeded();
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mDisplayId)) {
            this.mService.updateFocusedWindowLocked(3, false);
            assignWindowLayers(false);
        } else if (!this.mService.updateFocusedWindowLocked(3, false)) {
            assignWindowLayers(false);
        }
        this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
        this.mService.mInputMonitor.updateInputWindowsLw(false);
    }

    boolean destroyLeakedSurfaces() {
        this.mTmpWindow = null;
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$rF1ZhFUTWyZqcBK8Oea3g5-uNlM(this), false);
        return this.mTmpWindow != null;
    }

    public static /* synthetic */ void lambda$destroyLeakedSurfaces$16(DisplayContent displayContent, WindowState w) {
        WindowStateAnimator wsa = w.mWinAnimator;
        if (wsa.mSurfaceController != null) {
            String str;
            StringBuilder stringBuilder;
            if (!displayContent.mService.mSessions.contains(wsa.mSession)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LEAKED SURFACE (session doesn't exist): ");
                stringBuilder.append(w);
                stringBuilder.append(" surface=");
                stringBuilder.append(wsa.mSurfaceController);
                stringBuilder.append(" token=");
                stringBuilder.append(w.mToken);
                stringBuilder.append(" pid=");
                stringBuilder.append(w.mSession.mPid);
                stringBuilder.append(" uid=");
                stringBuilder.append(w.mSession.mUid);
                Slog.w(str, stringBuilder.toString());
                wsa.destroySurface();
                displayContent.mService.mForceRemoves.add(w);
                displayContent.mTmpWindow = w;
            } else if (w.mAppToken != null && w.mAppToken.isClientHidden()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LEAKED SURFACE (app token hidden): ");
                stringBuilder.append(w);
                stringBuilder.append(" surface=");
                stringBuilder.append(wsa.mSurfaceController);
                stringBuilder.append(" token=");
                stringBuilder.append(w.mAppToken);
                Slog.w(str, stringBuilder.toString());
                wsa.destroySurface();
                displayContent.mTmpWindow = w;
            }
        }
    }

    WindowState computeImeTarget(boolean updateImeTarget) {
        AppWindowToken token = null;
        StringBuilder stringBuilder;
        if (this.mService.mInputMethodWindow == null) {
            if (updateImeTarget) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Moving IM target from ");
                    stringBuilder.append(this.mService.mInputMethodTarget);
                    stringBuilder.append(" to null since mInputMethodWindow is null");
                    Slog.w(str, stringBuilder.toString());
                }
                setInputMethodTarget(null, this.mService.mInputMethodTargetWaitingAnim);
            }
            return null;
        }
        WindowState curTarget = this.mService.mInputMethodTarget;
        String str2;
        if (canUpdateImeTarget()) {
            String str3;
            StringBuilder stringBuilder2;
            this.mUpdateImeTarget = updateImeTarget;
            WindowState target = getWindow(this.mComputeImeTargetPredicate);
            if (target != null && target.mAttrs.type == 3) {
                AppWindowToken token2 = target.mAppToken;
                if (token2 != null) {
                    WindowState betterTarget = token2.getImeTargetBelowWindow(target);
                    if (betterTarget != null) {
                        target = betterTarget;
                    }
                }
            }
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Proposed new IME target: ");
                stringBuilder2.append(target);
                stringBuilder2.append(" curTarget:");
                stringBuilder2.append(curTarget);
                Slog.v(str3, stringBuilder2.toString());
            }
            boolean isClosing = curTarget != null && ((curTarget.mAnimatingExit && curTarget.mWinAnimator.getShown()) || curTarget.mService.mClosingApps.contains(curTarget.mAppToken));
            if (curTarget != null && curTarget.isDisplayedLw() && isClosing && (target == null || target.isActivityTypeHome())) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("New target is home while current target is closing, not changing:");
                    stringBuilder3.append(curTarget);
                    Slog.v(str2, stringBuilder3.toString());
                }
                return curTarget;
            }
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Desired input method target=");
                stringBuilder4.append(target);
                stringBuilder4.append(" updateImeTarget=");
                stringBuilder4.append(updateImeTarget);
                Slog.v(str4, stringBuilder4.toString());
            }
            if (target == null) {
                if (updateImeTarget) {
                    if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Moving IM target from ");
                        stringBuilder2.append(curTarget);
                        stringBuilder2.append(" to null.");
                        stringBuilder2.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        Slog.w(str3, stringBuilder2.toString());
                    }
                    setInputMethodTarget(null, this.mService.mInputMethodTargetWaitingAnim);
                }
                return null;
            }
            if (updateImeTarget) {
                if (curTarget != null) {
                    token = curTarget.mAppToken;
                }
                if (token != null) {
                    WindowState highestTarget = null;
                    if (token.isSelfAnimating()) {
                        highestTarget = token.getHighestAnimLayerWindow(curTarget);
                    }
                    if (highestTarget != null) {
                        AppTransition appTransition = this.mService.mAppTransition;
                        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                            String str5 = TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append(appTransition);
                            stringBuilder5.append(" ");
                            stringBuilder5.append(highestTarget);
                            stringBuilder5.append(" animating=");
                            stringBuilder5.append(highestTarget.mWinAnimator.isAnimationSet());
                            stringBuilder5.append(" layer=");
                            stringBuilder5.append(highestTarget.mWinAnimator.mAnimLayer);
                            stringBuilder5.append(" new layer=");
                            stringBuilder5.append(target.mWinAnimator.mAnimLayer);
                            Slog.v(str5, stringBuilder5.toString());
                        }
                        if (appTransition.isTransitionSet()) {
                            setInputMethodTarget(highestTarget, true);
                            return highestTarget;
                        } else if (highestTarget.mWinAnimator.isAnimationSet() && highestTarget.mWinAnimator.mAnimLayer > target.mWinAnimator.mAnimLayer) {
                            setInputMethodTarget(highestTarget, true);
                            return highestTarget;
                        }
                    }
                }
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    String str6 = TAG;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("Moving IM target from ");
                    stringBuilder6.append(curTarget);
                    stringBuilder6.append(" to ");
                    stringBuilder6.append(target);
                    stringBuilder6.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    Slog.w(str6, stringBuilder6.toString());
                }
                setInputMethodTarget(target, false);
            }
            return target;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Defer updating IME target:");
            stringBuilder.append(curTarget);
            Slog.w(str2, stringBuilder.toString());
        }
        return curTarget;
    }

    private void setInputMethodTarget(WindowState target, boolean targetWaitingAnim) {
        if (target != this.mService.mInputMethodTarget || this.mService.mInputMethodTargetWaitingAnim != targetWaitingAnim) {
            String str;
            StringBuilder stringBuilder;
            if (HwPCUtils.isPcCastModeInServer() && this.mService.mHardKeyboardAvailable && this.mService.getFocusedDisplayId() != 0 && this.mDisplayId == 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setInputMethodTarget: target = ");
                stringBuilder.append(target);
                stringBuilder.append(", mDisplayId = ");
                stringBuilder.append(this.mDisplayId);
                stringBuilder.append(", mInputMethodTarget =");
                stringBuilder.append(this.mService.mInputMethodTarget);
                Slog.i(str, stringBuilder.toString());
                if (target != null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setInputMethodTarget ");
                    stringBuilder.append(Debug.getCallers(8));
                    Slog.i(str, stringBuilder.toString());
                }
            }
            if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(this.mDisplayId) || this.mService.mInputMethodTarget == null || this.mService.mInputMethodTarget.getDisplayId() == this.mDisplayId) {
                this.mService.mInputMethodTarget = target;
                WindowManagerPolicy policy = this.mService.mPolicy;
                if (policy instanceof PhoneWindowManager) {
                    ((PhoneWindowManager) policy).setInputMethodTargetWindow(target);
                }
                this.mService.mInputMethodTargetWaitingAnim = targetWaitingAnim;
                assignWindowLayers(false);
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setInputMethodTarget inputmethod on default display return, mDisplayId = ");
            stringBuilder.append(this.mDisplayId);
            stringBuilder.append(", mHardKeyboardAvailable =");
            stringBuilder.append(this.mService.mHardKeyboardAvailable);
            stringBuilder.append(", mService.mInputMethodTarget =");
            stringBuilder.append(this.mService.mInputMethodTarget);
            stringBuilder.append(", target =");
            stringBuilder.append(target);
            Slog.i(str, stringBuilder.toString());
        }
    }

    boolean getNeedsMenu(WindowState top, WindowState bottom) {
        boolean z = false;
        if (top.mAttrs.needsMenuKey != 0) {
            if (top.mAttrs.needsMenuKey == 1) {
                z = true;
            }
            return z;
        }
        this.mTmpWindow = null;
        WindowState candidate = getWindow(new -$$Lambda$DisplayContent$jJlRHCiYzTPceX3tUkQ_1wUz71E(this, top, bottom));
        if (candidate != null && candidate.mAttrs.needsMenuKey == 1) {
            z = true;
        }
        return z;
    }

    public static /* synthetic */ boolean lambda$getNeedsMenu$17(DisplayContent displayContent, WindowState top, WindowState bottom, WindowState w) {
        if (w == top) {
            displayContent.mTmpWindow = w;
        }
        if (displayContent.mTmpWindow == null) {
            return false;
        }
        if (w.mAttrs.needsMenuKey == 0 && w != bottom) {
            return false;
        }
        return true;
    }

    void setLayoutNeeded() {
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLayoutNeeded: callers=");
            stringBuilder.append(Debug.getCallers(3));
            Slog.w(str, stringBuilder.toString());
        }
        this.mLayoutNeeded = true;
    }

    private void clearLayoutNeeded() {
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clearLayoutNeeded: callers=");
            stringBuilder.append(Debug.getCallers(3));
            Slog.w(str, stringBuilder.toString());
        }
        this.mLayoutNeeded = false;
    }

    boolean isLayoutNeeded() {
        return this.mLayoutNeeded;
    }

    void dumpTokens(PrintWriter pw, boolean dumpAll) {
        if (!this.mTokenMap.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  Display #");
            stringBuilder.append(this.mDisplayId);
            pw.println(stringBuilder.toString());
            for (WindowToken token : this.mTokenMap.values()) {
                pw.print("  ");
                pw.print(token);
                if (dumpAll) {
                    pw.println(':');
                    token.dump(pw, "    ", dumpAll);
                } else {
                    pw.println();
                }
            }
        }
    }

    void dumpWindowAnimators(PrintWriter pw, String subPrefix) {
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$iSsga4uJnJzBuUddn6uWEUo6xO8(pw, subPrefix, new int[1]), false);
    }

    static /* synthetic */ void lambda$dumpWindowAnimators$18(PrintWriter pw, String subPrefix, int[] index, WindowState w) {
        if (!w.toString().contains("hwSingleMode_window")) {
            WindowStateAnimator wAnim = w.mWinAnimator;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(subPrefix);
            stringBuilder.append("Window #");
            stringBuilder.append(index[0]);
            stringBuilder.append(": ");
            stringBuilder.append(wAnim);
            pw.println(stringBuilder.toString());
            index[0] = index[0] + 1;
        }
    }

    void startKeyguardExitOnNonAppWindows(boolean onWallpaper, boolean goingToShade) {
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$68_t-1mHyvN9aDP5Tt_BKUPoYT8(this.mService.mPolicy, onWallpaper, goingToShade), true);
    }

    static /* synthetic */ void lambda$startKeyguardExitOnNonAppWindows$19(WindowManagerPolicy policy, boolean onWallpaper, boolean goingToShade, WindowState w) {
        if (w.mAppToken == null && policy.canBeHiddenByKeyguardLw(w) && w.wouldBeVisibleIfPolicyIgnored() && !w.isVisible()) {
            w.startAnimation(policy.createHiddenByKeyguardExit(onWallpaper, goingToShade));
        }
    }

    boolean checkWaitingForWindows() {
        this.mHaveBootMsg = false;
        this.mHaveApp = false;
        this.mHaveWallpaper = false;
        this.mHaveKeyguard = this.mService.mBootAnimationStopped;
        this.mQuickBoot = false;
        WindowState visibleWindow = getWindow(new -$$Lambda$DisplayContent$BgTlvHbVclnASz-MrvERWxyMV-A(this));
        if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("******** booted=");
            stringBuilder.append(this.mService.mSystemBooted);
            stringBuilder.append(" msg=");
            stringBuilder.append(this.mService.mShowingBootMessages);
            stringBuilder.append(" haveBoot=");
            stringBuilder.append(this.mHaveBootMsg);
            stringBuilder.append(" haveApp=");
            stringBuilder.append(this.mHaveApp);
            stringBuilder.append(" haveWall=");
            stringBuilder.append(this.mHaveWallpaper);
            stringBuilder.append(" haveKeyguard=");
            stringBuilder.append(this.mHaveKeyguard);
            stringBuilder.append(" mQuickBoot=");
            stringBuilder.append(this.mQuickBoot);
            Slog.i(str, stringBuilder.toString());
        }
        if (visibleWindow != null) {
            return true;
        }
        boolean wallpaperEnabled = this.mService.mContext.getResources().getBoolean(17956968) && this.mService.mContext.getResources().getBoolean(17956916) && !this.mService.mOnlyCore;
        if (WindowManagerDebugConfig.DEBUG_SCREEN_ON || WindowManagerDebugConfig.DEBUG_BOOT) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" wallEnabled=");
            stringBuilder2.append(wallpaperEnabled);
            Slog.i(str2, stringBuilder2.toString());
        }
        if (!this.mService.mSystemBooted && !this.mHaveBootMsg) {
            return true;
        }
        if (!this.mService.mSystemBooted || ((this.mHaveApp || this.mHaveKeyguard) && (!wallpaperEnabled || this.mHaveWallpaper))) {
            return false;
        }
        return true;
    }

    public static /* synthetic */ boolean lambda$checkWaitingForWindows$20(DisplayContent displayContent, WindowState w) {
        if (w.mAttrs.type != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && w.mAttrs.type != 2013 && w.isVisibleLw() && !w.mObscured && !w.isDrawnLw()) {
            return true;
        }
        if (w.isDrawnLw()) {
            if (w.mAttrs.type == 2021) {
                displayContent.mHaveBootMsg = true;
            } else if (w.mAttrs.type == 2 || w.mAttrs.type == 4) {
                displayContent.mHaveApp = true;
            } else if (w.mAttrs.type == 2013) {
                displayContent.mHaveWallpaper = true;
            } else if (w.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
                displayContent.mHaveKeyguard = displayContent.mService.mPolicy.isKeyguardDrawnLw();
            }
        } else if (w.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            displayContent.mHaveKeyguard = displayContent.mService.mPolicy.isKeyguardDrawnLw();
            displayContent.mQuickBoot = true;
        } else if (w.mAttrs.type == 2013) {
            displayContent.mHaveWallpaper = true;
            displayContent.mQuickBoot = true;
        }
        return false;
    }

    void updateWindowsForAnimator(WindowAnimator animator) {
        this.mTmpWindowAnimator = animator;
        forAllWindows(this.mUpdateWindowsForAnimator, true);
    }

    void updateWallpaperForAnimator(WindowAnimator animator) {
        resetAnimationBackgroundAnimator();
        this.mTmpWindow = null;
        this.mTmpWindowAnimator = animator;
        forAllWindows(this.mUpdateWallpaperForAnimator, true);
        if (animator.mWindowDetachedWallpaper != this.mTmpWindow) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Detached wallpaper changed from ");
                stringBuilder.append(animator.mWindowDetachedWallpaper);
                stringBuilder.append(" to ");
                stringBuilder.append(this.mTmpWindow);
                Slog.v(str, stringBuilder.toString());
            }
            animator.mWindowDetachedWallpaper = this.mTmpWindow;
            animator.mBulkUpdateParams |= 2;
        }
    }

    boolean inputMethodClientHasFocus(IInputMethodClient client) {
        boolean z = false;
        WindowState imFocus = computeImeTarget(false);
        if (imFocus == null) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Desired input method target: ");
            stringBuilder.append(imFocus);
            Slog.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Current focus: ");
            stringBuilder.append(this.mService.mCurrentFocus);
            Slog.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Last focus: ");
            stringBuilder.append(this.mService.mLastFocus);
            Slog.i(str, stringBuilder.toString());
        }
        IInputMethodClient imeClient = imFocus.mSession.mClient;
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IM target client: ");
            stringBuilder2.append(imeClient);
            Slog.i(str2, stringBuilder2.toString());
            if (imeClient != null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IM target client binder: ");
                stringBuilder2.append(imeClient.asBinder());
                Slog.i(str2, stringBuilder2.toString());
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Requesting client binder: ");
                stringBuilder2.append(client.asBinder());
                Slog.i(str2, stringBuilder2.toString());
            }
        }
        if (imeClient != null && imeClient.asBinder() == client.asBinder()) {
            z = true;
        }
        return z;
    }

    boolean hasSecureWindowOnScreen() {
        return getWindow(-$$Lambda$DisplayContent$5D_ifLpk7QwG-e9ZLZynNnDca9g.INSTANCE) != null;
    }

    static /* synthetic */ boolean lambda$hasSecureWindowOnScreen$21(WindowState w) {
        return w.isOnScreen() && (w.mAttrs.flags & 8192) != 0;
    }

    void updateSystemUiVisibility(int visibility, int globalDiff) {
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$1C_-u_mpQFfKL_O8K1VFzBgPg50(visibility, globalDiff), true);
    }

    static /* synthetic */ void lambda$updateSystemUiVisibility$22(int visibility, int globalDiff, WindowState w) {
        try {
            int curValue = w.mSystemUiVisibility;
            int diff = (curValue ^ visibility) & globalDiff;
            int newValue = ((~diff) & curValue) | (visibility & diff);
            if (newValue != curValue) {
                w.mSeq++;
                w.mSystemUiVisibility = newValue;
            }
            if (newValue != curValue || w.mAttrs.hasSystemUiListeners) {
                w.mClient.dispatchSystemUiVisibilityChanged(w.mSeq, visibility, newValue, diff);
            }
        } catch (RemoteException e) {
        }
    }

    void onWindowFreezeTimeout() {
        Slog.w(TAG, "Window freeze timeout expired.");
        this.mService.mWindowsFreezingScreen = 2;
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$2HHBX1R6lnY5GedkE9LUBwsCPoE(this), true);
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
    }

    public static /* synthetic */ void lambda$onWindowFreezeTimeout$23(DisplayContent displayContent, WindowState w) {
        if (w.getOrientationChanging()) {
            w.orientationChangeTimedOut();
            w.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - displayContent.mService.mDisplayFreezeTime);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Force clearing orientation change: ");
            stringBuilder.append(w);
            Slog.w(str, stringBuilder.toString());
        }
    }

    void waitForAllWindowsDrawn() {
        WindowManagerPolicy policy = this.mService.mPolicy;
        if (!this.isDefaultDisplay || isCoverOpen()) {
            forAllWindows((Consumer) new -$$Lambda$DisplayContent$_dZRryJQYdwokr9IRKPu2KCZZu4(this, policy), true);
            return;
        }
        Slog.w(TAG, "waitForAllWindowsDrawn cover is closed on default Display");
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$oqhmXZMcpcvgI50swQTzosAcjac(this, policy), true);
    }

    public static /* synthetic */ void lambda$waitForAllWindowsDrawn$24(DisplayContent displayContent, WindowManagerPolicy policy, WindowState w) {
        boolean keyguard = policy.isKeyguardHostWindow(w.mAttrs);
        if ((w.isVisibleLw() && (w.mAppToken != null || keyguard)) || w.mAttrs.type == 2100 || w.mAttrs.type == 2101) {
            w.mWinAnimator.mDrawState = 1;
            w.mLastContentInsets.set(-1, -1, -1, -1);
            displayContent.mService.mWaitingForDrawn.add(w);
        }
    }

    public static /* synthetic */ void lambda$waitForAllWindowsDrawn$25(DisplayContent displayContent, WindowManagerPolicy policy, WindowState w) {
        boolean keyguard = policy.isKeyguardHostWindow(w.mAttrs);
        if (!w.isVisibleLw()) {
            return;
        }
        if (w.mAppToken != null || keyguard) {
            w.mWinAnimator.mDrawState = 1;
            w.mLastContentInsets.set(-1, -1, -1, -1);
            displayContent.mService.mWaitingForDrawn.add(w);
        }
    }

    boolean applySurfaceChangesTransaction(boolean recoveringMemory) {
        int dw = this.mDisplayInfo.logicalWidth;
        int dh = this.mDisplayInfo.logicalHeight;
        WindowSurfacePlacer surfacePlacer = this.mService.mWindowPlacerLocked;
        this.mTmpUpdateAllDrawn.clear();
        int repeats = 0;
        do {
            repeats++;
            if (repeats > 6) {
                Slog.w(TAG, "Animation repeat aborted after too many iterations");
                clearLayoutNeeded();
                break;
            }
            if (this.isDefaultDisplay && (this.pendingLayoutChanges & 4) != 0) {
                this.mWallpaperController.adjustWallpaperWindows(this);
            }
            if (this.isDefaultDisplay && (this.pendingLayoutChanges & 2) != 0) {
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.v(TAG, "Computing new config from layout");
                }
                if (this.mService.updateOrientationFromAppTokensLocked(this.mDisplayId)) {
                    setLayoutNeeded();
                    this.mService.mH.obtainMessage(18, Integer.valueOf(this.mDisplayId)).sendToTarget();
                }
            }
            if ((this.pendingLayoutChanges & 1) != 0) {
                setLayoutNeeded();
            }
            if (repeats < 4) {
                performLayout(repeats == 1, false);
            } else {
                Slog.w(TAG, "Layout repeat skipped after too many iterations");
            }
            this.pendingLayoutChanges = 0;
            if (this.isDefaultDisplay) {
                this.mService.mPolicy.beginPostLayoutPolicyLw(dw, dh);
                forAllWindows(this.mApplyPostLayoutPolicy, true);
                this.pendingLayoutChanges |= this.mService.mPolicy.finishPostLayoutPolicyLw();
            }
        } while (this.pendingLayoutChanges != 0);
        this.mTmpApplySurfaceChangesTransactionState.reset();
        this.mTmpRecoveringMemory = recoveringMemory;
        forAllWindows(this.mApplySurfaceChangesTransaction, true);
        prepareSurfaces();
        this.mService.mDisplayManagerInternal.setDisplayProperties(this.mDisplayId, this.mTmpApplySurfaceChangesTransactionState.displayHasContent, this.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate, this.mTmpApplySurfaceChangesTransactionState.preferredModeId, true);
        boolean wallpaperVisible = this.mWallpaperController.isWallpaperVisible();
        if (wallpaperVisible != this.mLastWallpaperVisible) {
            this.mLastWallpaperVisible = wallpaperVisible;
            this.mService.mWallpaperVisibilityListeners.notifyWallpaperVisibilityChanged(this);
        }
        while (!this.mTmpUpdateAllDrawn.isEmpty()) {
            ((AppWindowToken) this.mTmpUpdateAllDrawn.removeLast()).updateAllDrawn();
        }
        return this.mTmpApplySurfaceChangesTransactionState.focusDisplayed;
    }

    private void updateBounds() {
        calculateBounds(this.mTmpBounds);
        setBounds(this.mTmpBounds);
    }

    private void calculateBounds(Rect out) {
        int orientation = this.mDisplayInfo.rotation;
        boolean rotated = true;
        if (!(orientation == 1 || orientation == 3)) {
            rotated = false;
        }
        int physWidth = rotated ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int physHeight = rotated ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        int width = this.mDisplayInfo.logicalWidth;
        int left = (physWidth - width) / 2;
        int height = this.mDisplayInfo.logicalHeight;
        int top = (physHeight - height) / 2;
        out.set(left, top, left + width, top + height);
    }

    public void getBounds(Rect out) {
        calculateBounds(out);
    }

    private void getBounds(Rect out, int orientation) {
        getBounds(out);
        int rotationDelta = deltaRotation(this.mDisplayInfo.rotation, orientation);
        if (rotationDelta == 1 || rotationDelta == 3) {
            createRotationMatrix(rotationDelta, (float) this.mBaseDisplayWidth, (float) this.mBaseDisplayHeight, this.mTmpMatrix);
            this.mTmpRectF.set(out);
            this.mTmpMatrix.mapRect(this.mTmpRectF);
            this.mTmpRectF.round(out);
        }
    }

    void performLayout(boolean initial, boolean updateInputWindows) {
        if (isLayoutNeeded()) {
            clearLayoutNeeded();
            int dw = this.mDisplayInfo.logicalWidth;
            int dh = this.mDisplayInfo.logicalHeight;
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v(TAG, "-------------------------------------");
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("performLayout: needed=");
                stringBuilder.append(isLayoutNeeded());
                stringBuilder.append(" dw=");
                stringBuilder.append(dw);
                stringBuilder.append(" dh=");
                stringBuilder.append(dh);
                Slog.v(str, stringBuilder.toString());
            }
            this.mDisplayFrames.onDisplayInfoUpdated(this.mDisplayInfo, calculateDisplayCutoutForRotation(this.mDisplayInfo.rotation));
            this.mDisplayFrames.mRotation = this.mRotation;
            this.mService.setNaviBarFlag();
            this.mService.mPolicy.beginLayoutLw(this.mDisplayFrames, getConfiguration().uiMode);
            if (this.isDefaultDisplay) {
                this.mService.mSystemDecorLayer = this.mService.mPolicy.getSystemDecorLayerLw();
                this.mService.mScreenRect.set(0, 0, dw, dh);
            }
            int seq = this.mLayoutSeq + 1;
            if (seq < 0) {
                seq = 0;
            }
            this.mLayoutSeq = seq;
            this.mTmpWindow = null;
            this.mTmpInitial = initial;
            forAllWindows(this.mPerformLayout, true);
            this.mTmpWindow2 = this.mTmpWindow;
            this.mTmpWindow = null;
            forAllWindows(this.mPerformLayoutAttached, true);
            this.mService.mInputMonitor.layoutInputConsumers(dw, dh);
            this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
            if (updateInputWindows) {
                this.mService.mInputMonitor.updateInputWindowsLw(false);
            }
            this.mService.mH.sendEmptyMessage(41);
        }
    }

    Bitmap screenshotDisplayLocked(Config config) {
        if (!this.mService.mPolicy.isScreenOn()) {
            return null;
        }
        int dw = this.mDisplayInfo.logicalWidth;
        int dh = this.mDisplayInfo.logicalHeight;
        if (dw <= 0 || dh <= 0) {
            return null;
        }
        Rect frame = new Rect(0, 0, dw, dh);
        int rot = this.mDisplay.getRotation();
        int i = 3;
        if (rot == 1 || rot == 3) {
            if (rot != 1) {
                i = 1;
            }
            rot = i;
        }
        int rot2 = rot;
        convertCropForSurfaceFlinger(frame, rot2, dw, dh);
        ScreenRotationAnimation screenRotationAnimation = this.mService.mAnimator.getScreenRotationAnimationLocked(0);
        boolean inRotation = screenRotationAnimation != null && screenRotationAnimation.isAnimating();
        Bitmap bitmap = SurfaceControl.screenshot(frame, dw, dh, 0, 1, inRotation, rot2);
        if (bitmap == null) {
            Slog.w(TAG, "Failed to take screenshot");
            return null;
        }
        Bitmap ret = bitmap.createAshmemBitmap(config);
        bitmap.recycle();
        return ret;
    }

    protected static void convertCropForSurfaceFlinger(Rect crop, int rot, int dw, int dh) {
        int tmp;
        if (rot == 1) {
            tmp = crop.top;
            crop.top = dw - crop.right;
            crop.right = crop.bottom;
            crop.bottom = dw - crop.left;
            crop.left = tmp;
        } else if (rot == 2) {
            tmp = crop.top;
            crop.top = dh - crop.bottom;
            crop.bottom = dh - tmp;
            tmp = crop.right;
            crop.right = dw - crop.left;
            crop.left = dw - tmp;
        } else if (rot == 3) {
            tmp = crop.top;
            crop.top = crop.left;
            crop.left = dh - crop.bottom;
            crop.bottom = crop.right;
            crop.right = dh - tmp;
        }
    }

    void onSeamlessRotationTimeout() {
        this.mTmpWindow = null;
        forAllWindows((Consumer) new -$$Lambda$DisplayContent$fJACJZmXtOEEtwlcx1f9zVkhr30(this), true);
        if (this.mTmpWindow != null) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    public static /* synthetic */ void lambda$onSeamlessRotationTimeout$26(DisplayContent displayContent, WindowState w) {
        if (w.mSeamlesslyRotated) {
            displayContent.mTmpWindow = w;
            w.setDisplayLayoutNeeded();
            displayContent.mService.markForSeamlessRotation(w, false);
        }
    }

    void setExitingTokensHasVisible(boolean hasVisible) {
        for (int i = this.mExitingTokens.size() - 1; i >= 0; i--) {
            ((WindowToken) this.mExitingTokens.get(i)).hasVisible = hasVisible;
        }
        this.mTaskStackContainers.setExitingTokensHasVisible(hasVisible);
    }

    void removeExistingTokensIfPossible() {
        for (int i = this.mExitingTokens.size() - 1; i >= 0; i--) {
            if (!((WindowToken) this.mExitingTokens.get(i)).hasVisible) {
                this.mExitingTokens.remove(i);
            }
        }
        this.mTaskStackContainers.removeExistingAppTokensIfPossible();
    }

    void onDescendantOverrideConfigurationChanged() {
        setLayoutNeeded();
        this.mService.requestTraversal();
    }

    boolean okToDisplay() {
        boolean z = false;
        if (this.mDisplayId == 0) {
            if (!this.mService.mDisplayFrozen && this.mService.mDisplayEnabled && this.mService.mPolicy.isScreenOn()) {
                z = true;
            }
            return z;
        }
        if (this.mDisplayInfo.state == 2) {
            z = true;
        }
        return z;
    }

    boolean okToAnimate() {
        return okToDisplay() && (this.mDisplayId != 0 || this.mService.mPolicy.okToAnimate());
    }

    Builder makeSurface(SurfaceSession s) {
        return this.mService.makeSurfaceBuilder(s).setParent(this.mWindowingLayer);
    }

    SurfaceSession getSession() {
        return this.mSession;
    }

    Builder makeChildSurface(WindowContainer child) {
        Builder b = this.mService.makeSurfaceBuilder(child != null ? child.getSession() : getSession());
        b.setSize(this.mSurfaceSize, this.mSurfaceSize);
        if (child == null) {
            return b;
        }
        return b.setName(child.getName()).setParent(this.mWindowingLayer);
    }

    Builder makeOverlay() {
        return this.mService.makeSurfaceBuilder(this.mSession).setParent(this.mOverlayLayer);
    }

    void reparentToOverlay(Transaction transaction, SurfaceControl surface) {
        transaction.reparent(surface, this.mOverlayLayer.getHandle());
    }

    void applyMagnificationSpec(MagnificationSpec spec) {
        if (((double) spec.scale) != 1.0d) {
            this.mMagnificationSpec = spec;
        } else {
            this.mMagnificationSpec = null;
        }
        applyMagnificationSpec(getPendingTransaction(), spec);
        getPendingTransaction().apply();
    }

    void reapplyMagnificationSpec() {
        if (this.mMagnificationSpec != null) {
            applyMagnificationSpec(getPendingTransaction(), this.mMagnificationSpec);
        }
    }

    void onParentSet() {
    }

    void assignChildLayers(Transaction t) {
        this.mBelowAppWindowsContainers.assignLayer(t, 0);
        this.mTaskStackContainers.assignLayer(t, 1);
        this.mAboveAppWindowsContainers.assignLayer(t, 2);
        WindowState imeTarget = this.mService.mInputMethodTarget;
        boolean needAssignIme = true;
        if (!(imeTarget == null || imeTarget.inSplitScreenWindowingMode() || imeTarget.mToken.isAppAnimating() || imeTarget.getSurfaceControl() == null)) {
            this.mImeWindowsContainers.assignRelativeLayer(t, imeTarget.getSurfaceControl(), 1);
            needAssignIme = false;
        }
        this.mBelowAppWindowsContainers.assignChildLayers(t);
        this.mTaskStackContainers.assignChildLayers(t);
        this.mAboveAppWindowsContainers.assignChildLayers(t, needAssignIme ? this.mImeWindowsContainers : null);
        this.mImeWindowsContainers.assignChildLayers(t);
    }

    void assignRelativeLayerForImeTargetChild(Transaction t, WindowContainer child) {
        child.assignRelativeLayer(t, this.mImeWindowsContainers.getSurfaceControl(), 1);
    }

    void prepareSurfaces() {
        ScreenRotationAnimation screenRotationAnimation = this.mService.mAnimator.getScreenRotationAnimationLocked(this.mDisplayId);
        if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
            screenRotationAnimation.getEnterTransformation().getMatrix().getValues(this.mTmpFloats);
            this.mPendingTransaction.setMatrix(this.mWindowingLayer, this.mTmpFloats[0], this.mTmpFloats[3], this.mTmpFloats[1], this.mTmpFloats[4]);
            this.mPendingTransaction.setPosition(this.mWindowingLayer, this.mTmpFloats[2], this.mTmpFloats[5]);
            this.mPendingTransaction.setAlpha(this.mWindowingLayer, screenRotationAnimation.getEnterTransformation().getAlpha());
        }
        int i = this.mTaskStackContainers.getChildCount() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(i2);
                stack.updateSurfacePosition();
                stack.updateSurfaceSize(stack.getPendingTransaction());
                i = i2 - 1;
            } else {
                super.prepareSurfaces();
                return;
            }
        }
    }

    void assignStackOrdering() {
        this.mTaskStackContainers.assignStackOrdering(getPendingTransaction());
    }

    void deferUpdateImeTarget() {
        this.mDeferUpdateImeTargetCount++;
    }

    void continueUpdateImeTarget() {
        if (this.mDeferUpdateImeTargetCount != 0) {
            this.mDeferUpdateImeTargetCount--;
            if (this.mDeferUpdateImeTargetCount == 0) {
                computeImeTarget(true);
            }
        }
    }

    private boolean canUpdateImeTarget() {
        return this.mDeferUpdateImeTargetCount == 0;
    }
}
