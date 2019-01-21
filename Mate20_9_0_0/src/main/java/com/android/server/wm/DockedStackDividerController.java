package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IDockedStackListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.server.LocalServices;
import com.android.server.os.HwBootFail;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class DockedStackDividerController {
    private static final float CLIP_REVEAL_MEET_EARLIEST = 0.6f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MAX = 0.8f;
    private static final float CLIP_REVEAL_MEET_FRACTION_MIN = 0.4f;
    private static final float CLIP_REVEAL_MEET_LAST = 1.0f;
    private static final int DIVIDER_WIDTH_INACTIVE_DP = 4;
    private static final long IME_ADJUST_ANIM_DURATION = 280;
    private static final long IME_ADJUST_DRAWN_TIMEOUT = 200;
    private static final Interpolator IME_ADJUST_ENTRY_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f);
    private static final String TAG = "WindowManager";
    private boolean mAdjustedForDivider;
    private boolean mAdjustedForIme;
    private boolean mAnimatingForIme;
    private boolean mAnimatingForMinimizedDockedStack;
    private long mAnimationDuration;
    private float mAnimationStart;
    private boolean mAnimationStartDelayed;
    private long mAnimationStartTime;
    private boolean mAnimationStarted;
    private float mAnimationTarget;
    private WindowState mDelayedImeWin;
    private TaskStack mDimmedStack;
    private final DisplayContent mDisplayContent;
    private float mDividerAnimationStart;
    private float mDividerAnimationTarget;
    private int mDividerInsets;
    private int mDividerWindowWidth;
    private int mDividerWindowWidthInactive;
    private final RemoteCallbackList<IDockedStackListener> mDockedStackListeners = new RemoteCallbackList();
    private final Handler mHandler = new Handler();
    private int mImeHeight;
    private boolean mImeHideRequested;
    float mLastAnimationProgress;
    private float mLastDimLayerAlpha;
    private final Rect mLastDimLayerRect = new Rect();
    float mLastDividerProgress;
    private final Rect mLastRect = new Rect();
    private boolean mLastVisibility = false;
    private float mMaximizeMeetFraction;
    private boolean mMinimizedDock;
    private final Interpolator mMinimizedDockInterpolator;
    private int mOriginalDockedSide = -1;
    private boolean mResizing;
    private int mRotation = 0;
    private final WindowManagerService mService;
    private final DividerSnapAlgorithm[] mSnapAlgorithmForRotation = new DividerSnapAlgorithm[4];
    private int mTaskHeightInMinimizedMode;
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Rect mTmpRect3 = new Rect();
    private final Rect mTouchRegion = new Rect();
    private WindowState mWindow;

    DockedStackDividerController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mMinimizedDockInterpolator = AnimationUtils.loadInterpolator(service.mContext, 17563661);
        loadDimens();
    }

    int getSmallestWidthDpForBounds(Rect bounds) {
        DisplayInfo di = this.mDisplayContent.getDisplayInfo();
        int baseDisplayWidth = this.mDisplayContent.mBaseDisplayWidth;
        int baseDisplayHeight = this.mDisplayContent.mBaseDisplayHeight;
        int minWidth = HwBootFail.STAGE_BOOT_SUCCESS;
        int rotation = 0;
        while (rotation < 4) {
            this.mTmpRect.set(bounds);
            this.mDisplayContent.rotateBounds(di.rotation, rotation, this.mTmpRect);
            int i = 1;
            boolean z = rotation == 1 || rotation == 3;
            boolean rotated = z;
            this.mTmpRect2.set(0, 0, rotated ? baseDisplayHeight : baseDisplayWidth, rotated ? baseDisplayWidth : baseDisplayHeight);
            if (this.mTmpRect2.width() > this.mTmpRect2.height()) {
                i = 2;
            }
            int dockSide = getDockSide(this.mTmpRect, this.mTmpRect2, i);
            int position = DockedDividerUtils.calculatePositionForBounds(this.mTmpRect, dockSide, getContentWidth());
            DisplayCutout displayCutout = this.mDisplayContent.calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
            int snappedPosition = this.mSnapAlgorithmForRotation[rotation].calculateNonDismissingSnapTarget(position).position;
            DockedDividerUtils.calculateBoundsForPosition(snappedPosition, dockSide, this.mTmpRect, this.mTmpRect2.width(), this.mTmpRect2.height(), getContentWidth());
            this.mService.mPolicy.getStableInsetsLw(rotation, this.mTmpRect2.width(), this.mTmpRect2.height(), displayCutout, this.mTmpRect3);
            this.mService.intersectDisplayInsetBounds(this.mTmpRect2, this.mTmpRect3, this.mTmpRect);
            minWidth = Math.min(this.mTmpRect.width(), minWidth);
            rotation++;
        }
        Rect rect = bounds;
        return (int) (((float) minWidth) / this.mDisplayContent.getDisplayMetrics().density);
    }

    int getDockSide(Rect bounds, Rect displayRect, int orientation) {
        int i = 2;
        int i2 = 1;
        if (orientation == 1) {
            i2 = (displayRect.bottom - bounds.bottom) - (bounds.top - displayRect.top);
            if (i2 > 0) {
                return 2;
            }
            if (i2 < 0) {
                return 4;
            }
            if (!canPrimaryStackDockTo(2)) {
                i = 4;
            }
            return i;
        } else if (orientation != 2) {
            return -1;
        } else {
            i = (displayRect.right - bounds.right) - (bounds.left - displayRect.left);
            if (i > 0) {
                return 1;
            }
            if (i < 0) {
                return 3;
            }
            if (!canPrimaryStackDockTo(1)) {
                i2 = 3;
            }
            return i2;
        }
    }

    void getHomeStackBoundsInDockedMode(Rect outBounds) {
        DisplayInfo di = this.mDisplayContent.getDisplayInfo();
        this.mService.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, di.displayCutout, this.mTmpRect);
        int dividerSize = this.mDividerWindowWidth - (2 * this.mDividerInsets);
        if (this.mDisplayContent.getConfiguration().orientation == 1) {
            outBounds.set(0, (this.mTaskHeightInMinimizedMode + dividerSize) + this.mTmpRect.top, di.logicalWidth, di.logicalHeight);
            return;
        }
        TaskStack stack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        int primaryTaskWidth = (this.mTaskHeightInMinimizedMode + dividerSize) + this.mTmpRect.top;
        int left = this.mTmpRect.left;
        int right = di.logicalWidth - this.mTmpRect.right;
        if (stack != null) {
            if (stack.getDockSide() == 1) {
                left += primaryTaskWidth;
            } else if (stack.getDockSide() == 3) {
                right -= primaryTaskWidth;
            }
        }
        outBounds.set(left, 0, right, di.logicalHeight);
    }

    boolean isHomeStackResizable() {
        TaskStack homeStack = this.mDisplayContent.getHomeStack();
        boolean z = false;
        if (homeStack == null) {
            return false;
        }
        Task homeTask = homeStack.findHomeTask();
        if (homeTask != null && homeTask.isResizeable()) {
            z = true;
        }
        return z;
    }

    private void initSnapAlgorithmForRotations() {
        Configuration baseConfig = this.mDisplayContent.getConfiguration();
        Configuration config = new Configuration();
        int rotation = 0;
        while (rotation < 4) {
            int i;
            boolean z = rotation == 1 || rotation == 3;
            boolean rotated = z;
            if (rotated) {
                i = this.mDisplayContent.mBaseDisplayHeight;
            } else {
                i = this.mDisplayContent.mBaseDisplayWidth;
            }
            int dw = i;
            if (rotated) {
                i = this.mDisplayContent.mBaseDisplayWidth;
            } else {
                i = this.mDisplayContent.mBaseDisplayHeight;
            }
            int dh = i;
            DisplayCutout displayCutout = this.mDisplayContent.calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
            this.mService.mPolicy.getStableInsetsLw(rotation, dw, dh, displayCutout, this.mTmpRect);
            config.unset();
            config.orientation = dw <= dh ? 1 : 2;
            int displayId = this.mDisplayContent.getDisplayId();
            int i2 = dw;
            int i3 = dh;
            int i4 = rotation;
            int i5 = displayId;
            DisplayCutout displayCutout2 = displayCutout;
            int appWidth = this.mService.mPolicy.getNonDecorDisplayWidth(i2, i3, i4, baseConfig.uiMode, i5, displayCutout2);
            int appHeight = this.mService.mPolicy.getNonDecorDisplayHeight(i2, i3, i4, baseConfig.uiMode, i5, displayCutout2);
            this.mService.mPolicy.getNonDecorInsetsLw(rotation, dw, dh, displayCutout, this.mTmpRect);
            int leftInset = this.mTmpRect.left;
            i5 = this.mTmpRect.top;
            config.windowConfiguration.setAppBounds(leftInset, i5, leftInset + appWidth, i5 + appHeight);
            i2 = dw;
            i3 = dh;
            i4 = rotation;
            float density = this.mDisplayContent.getDisplayMetrics().density;
            i5 = displayId;
            displayCutout2 = displayCutout;
            config.screenWidthDp = (int) (((float) this.mService.mPolicy.getConfigDisplayWidth(i2, i3, i4, baseConfig.uiMode, i5, displayCutout2)) / density);
            config.screenHeightDp = (int) (((float) this.mService.mPolicy.getConfigDisplayHeight(i2, i3, i4, baseConfig.uiMode, i5, displayCutout2)) / density);
            this.mSnapAlgorithmForRotation[rotation] = new DividerSnapAlgorithm(this.mService.mContext.createConfigurationContext(config).getResources(), dw, dh, getContentWidth(), config.orientation == 1, this.mTmpRect);
            rotation++;
        }
    }

    private void loadDimens() {
        Context context = this.mService.mContext;
        this.mDividerWindowWidth = context.getResources().getDimensionPixelSize(17105035);
        this.mDividerInsets = context.getResources().getDimensionPixelSize(17105034);
        this.mDividerWindowWidthInactive = WindowManagerService.dipToPixel(4, this.mDisplayContent.getDisplayMetrics());
        this.mTaskHeightInMinimizedMode = context.getResources().getDimensionPixelSize(17105326);
        initSnapAlgorithmForRotations();
    }

    void onConfigurationChanged() {
        loadDimens();
    }

    boolean isResizing() {
        return this.mResizing;
    }

    int getContentWidth() {
        return this.mDividerWindowWidth - (2 * this.mDividerInsets);
    }

    int getContentInsets() {
        return this.mDividerInsets;
    }

    int getContentWidthInactive() {
        return this.mDividerWindowWidthInactive;
    }

    void setResizing(boolean resizing) {
        if (this.mResizing != resizing) {
            this.mResizing = resizing;
            resetDragResizingChangeReported();
            if (!this.mResizing && this.mRotation != this.mDisplayContent.getDisplay().getRotation()) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        DockedStackDividerController.this.mService.updateRotation(false, false);
                    }
                });
            }
        }
    }

    void setTouchRegion(Rect touchRegion) {
        this.mTouchRegion.set(touchRegion);
    }

    void getTouchRegion(Rect outRegion) {
        outRegion.set(this.mTouchRegion);
        outRegion.offset(this.mWindow.getFrameLw().left, this.mWindow.getFrameLw().top);
    }

    private void resetDragResizingChangeReported() {
        this.mDisplayContent.forAllWindows((Consumer) -$$Lambda$vhwCX-wzYksBgFM46tASKUCeQRc.INSTANCE, true);
    }

    void setWindow(WindowState window) {
        this.mWindow = window;
        reevaluateVisibility(false);
    }

    void reevaluateVisibility(boolean force) {
        if (this.mWindow != null) {
            boolean visible = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null;
            if (this.mLastVisibility != visible || force) {
                this.mLastVisibility = visible;
                notifyDockedDividerVisibilityChanged(visible);
                if (!visible) {
                    setResizeDimLayer(false, 0, 0.0f);
                }
            }
        }
    }

    private boolean wasVisible() {
        return this.mLastVisibility;
    }

    void setAdjustedForIme(boolean adjustedForIme, boolean adjustedForDivider, boolean animate, WindowState imeWin, int imeHeight) {
        if (this.mAdjustedForIme != adjustedForIme || ((adjustedForIme && this.mImeHeight != imeHeight) || this.mAdjustedForDivider != adjustedForDivider)) {
            if (!animate || this.mAnimatingForMinimizedDockedStack) {
                boolean z = adjustedForIme || adjustedForDivider;
                notifyAdjustedForImeChanged(z, 0);
            } else {
                startImeAdjustAnimation(adjustedForIme, adjustedForDivider, imeWin);
            }
            this.mAdjustedForIme = adjustedForIme;
            this.mImeHeight = imeHeight;
            this.mAdjustedForDivider = adjustedForDivider;
        }
    }

    int getImeHeightAdjustedFor() {
        return this.mImeHeight;
    }

    void positionDockedStackedDivider(Rect frame) {
        TaskStack stack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (stack == null) {
            frame.set(this.mLastRect);
            return;
        }
        stack.getDimBounds(this.mTmpRect);
        switch (stack.getDockSide()) {
            case 1:
                frame.set(this.mTmpRect.right - this.mDividerInsets, frame.top, (this.mTmpRect.right + frame.width()) - this.mDividerInsets, frame.bottom);
                break;
            case 2:
                frame.set(frame.left, this.mTmpRect.bottom - this.mDividerInsets, this.mTmpRect.right, (this.mTmpRect.bottom + frame.height()) - this.mDividerInsets);
                break;
            case 3:
                frame.set((this.mTmpRect.left - frame.width()) + this.mDividerInsets, frame.top, this.mTmpRect.left + this.mDividerInsets, frame.bottom);
                break;
            case 4:
                frame.set(frame.left, (this.mTmpRect.top - frame.height()) + this.mDividerInsets, frame.right, this.mTmpRect.top + this.mDividerInsets);
                break;
        }
        this.mLastRect.set(frame);
    }

    private void notifyDockedDividerVisibilityChanged(boolean visible) {
        synchronized (this.mDockedStackListeners) {
            int size = this.mDockedStackListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                try {
                    ((IDockedStackListener) this.mDockedStackListeners.getBroadcastItem(i)).onDividerVisibilityChanged(visible);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error delivering divider visibility changed event.", e);
                }
            }
            this.mDockedStackListeners.finishBroadcast();
        }
    }

    boolean canPrimaryStackDockTo(int dockSide) {
        DisplayInfo di = this.mDisplayContent.getDisplayInfo();
        return this.mService.mPolicy.isDockSideAllowed(dockSide, this.mOriginalDockedSide, di.logicalWidth, di.logicalHeight, di.rotation);
    }

    void notifyDockedStackExistsChanged(boolean exists) {
        synchronized (this.mDockedStackListeners) {
            int size = this.mDockedStackListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                try {
                    ((IDockedStackListener) this.mDockedStackListeners.getBroadcastItem(i)).onDockedStackExistsChanged(exists);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error delivering docked stack exists changed event.", e);
                }
            }
            this.mDockedStackListeners.finishBroadcast();
        }
        if (exists) {
            InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            if (inputMethodManagerInternal != null) {
                inputMethodManagerInternal.hideCurrentInputMethod();
                this.mImeHideRequested = true;
            }
            this.mOriginalDockedSide = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility().getDockSideForDisplay(this.mDisplayContent);
            return;
        }
        this.mOriginalDockedSide = -1;
        setMinimizedDockedStack(false, false);
        if (this.mDimmedStack != null) {
            this.mDimmedStack.stopDimming();
            this.mDimmedStack = null;
        }
    }

    void resetImeHideRequested() {
        this.mImeHideRequested = false;
    }

    boolean isImeHideRequested() {
        return this.mImeHideRequested;
    }

    private void notifyDockedStackMinimizedChanged(boolean minimizedDock, boolean animate, boolean isHomeStackResizable) {
        long animDuration = 0;
        if (animate) {
            long transitionDuration;
            TaskStack stack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
            if (isAnimationMaximizing()) {
                transitionDuration = this.mService.mAppTransition.getLastClipRevealTransitionDuration();
            } else {
                transitionDuration = 250;
            }
            this.mAnimationDuration = (long) (((float) transitionDuration) * this.mService.getTransitionAnimationScaleLocked());
            this.mMaximizeMeetFraction = getClipRevealMeetFraction(stack);
            animDuration = (long) (((float) this.mAnimationDuration) * this.mMaximizeMeetFraction);
        }
        this.mService.mH.removeMessages(53);
        int i = 0;
        this.mService.mH.obtainMessage(53, minimizedDock, 0).sendToTarget();
        synchronized (this.mDockedStackListeners) {
            int size = this.mDockedStackListeners.beginBroadcast();
            while (i < size) {
                try {
                    ((IDockedStackListener) this.mDockedStackListeners.getBroadcastItem(i)).onDockedStackMinimizedChanged(minimizedDock, animDuration, isHomeStackResizable);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error delivering minimized dock changed event.", e);
                }
                i++;
            }
            this.mDockedStackListeners.finishBroadcast();
        }
    }

    void notifyDockSideChanged(int newDockSide) {
        synchronized (this.mDockedStackListeners) {
            int size = this.mDockedStackListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                try {
                    ((IDockedStackListener) this.mDockedStackListeners.getBroadcastItem(i)).onDockSideChanged(newDockSide);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error delivering dock side changed event.", e);
                }
            }
            this.mDockedStackListeners.finishBroadcast();
        }
    }

    private void notifyAdjustedForImeChanged(boolean adjustedForIme, long animDuration) {
        synchronized (this.mDockedStackListeners) {
            int size = this.mDockedStackListeners.beginBroadcast();
            for (int i = 0; i < size; i++) {
                try {
                    ((IDockedStackListener) this.mDockedStackListeners.getBroadcastItem(i)).onAdjustedForImeChanged(adjustedForIme, animDuration);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error delivering adjusted for ime changed event.", e);
                }
            }
            this.mDockedStackListeners.finishBroadcast();
        }
    }

    void registerDockedStackListener(IDockedStackListener listener) {
        boolean z;
        synchronized (this.mDockedStackListeners) {
            this.mDockedStackListeners.register(listener);
        }
        notifyDockedDividerVisibilityChanged(wasVisible());
        if (this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null) {
            z = true;
        } else {
            z = false;
        }
        notifyDockedStackExistsChanged(z);
        notifyDockedStackMinimizedChanged(this.mMinimizedDock, false, isHomeStackResizable());
        notifyAdjustedForImeChanged(this.mAdjustedForIme, 0);
    }

    void setResizeDimLayer(boolean visible, int targetWindowingMode, float alpha) {
        TaskStack stack;
        if (targetWindowingMode != 0) {
            stack = this.mDisplayContent.getTopStackInWindowingMode(targetWindowingMode);
        } else {
            stack = null;
        }
        boolean visibleAndValid = (!visible || stack == null || this.mDisplayContent.getSplitScreenPrimaryStack() == null) ? false : true;
        if (!(this.mDimmedStack == null || this.mDimmedStack == stack)) {
            this.mDimmedStack.stopDimming();
            this.mDimmedStack = null;
        }
        if (visibleAndValid) {
            this.mDimmedStack = stack;
            stack.dim(alpha);
        }
        if (!visibleAndValid && stack != null) {
            this.mDimmedStack = null;
            stack.stopDimming();
        }
    }

    private int getResizeDimLayer() {
        return this.mWindow != null ? this.mWindow.mLayer - 1 : 1;
    }

    void notifyAppVisibilityChanged() {
        checkMinimizeChanged(false);
    }

    void notifyAppTransitionStarting(ArraySet<AppWindowToken> openingApps, int appTransition) {
        boolean wasMinimized = this.mMinimizedDock;
        checkMinimizeChanged(true);
        if (wasMinimized && this.mMinimizedDock && containsAppInDockedStack(openingApps) && appTransition != 0 && !AppTransition.isKeyguardGoingAwayTransit(appTransition) && !this.mService.mAmInternal.isRecentsComponentHomeActivity(this.mService.mCurrentUserId)) {
            if (this.mService.mAppTransition.mIgnoreShowRecentApps) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyAppTransitionStarting Ignore Show RecentApps ");
                stringBuilder.append(this.mService.mAppTransition.mIgnoreShowRecentApps);
                Slog.d(str, stringBuilder.toString());
                return;
            }
            this.mService.showRecentApps();
        }
    }

    private boolean containsAppInDockedStack(ArraySet<AppWindowToken> apps) {
        for (int i = apps.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) apps.valueAt(i);
            if (token.getTask() != null && token.inSplitScreenPrimaryWindowingMode()) {
                return true;
            }
        }
        return false;
    }

    boolean isMinimizedDock() {
        return this.mMinimizedDock;
    }

    void checkMinimizeChanged(boolean animate) {
        if (this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null) {
            TaskStack homeStack = this.mDisplayContent.getHomeStack();
            if (homeStack != null) {
                Task homeTask = homeStack.findHomeTask();
                if (homeTask != null && isWithinDisplay(homeTask)) {
                    if (!this.mMinimizedDock || !this.mService.mKeyguardOrAodShowingOnDefaultDisplay) {
                        TaskStack topSecondaryStack = this.mDisplayContent.getTopStackInWindowingMode(4);
                        RecentsAnimationController recentsAnim = this.mService.getRecentsAnimationController();
                        boolean z = false;
                        boolean minimizedForRecentsAnimation = recentsAnim != null && recentsAnim.isSplitScreenMinimized();
                        boolean homeVisible = homeTask.getTopVisibleAppToken() != null;
                        if (homeVisible && topSecondaryStack != null) {
                            homeVisible = homeStack.compareTo(topSecondaryStack) >= 0;
                        }
                        if (homeVisible || minimizedForRecentsAnimation) {
                            z = true;
                        }
                        setMinimizedDockedStack(z, animate);
                    }
                }
            }
        }
    }

    private boolean isWithinDisplay(Task task) {
        task.getBounds(this.mTmpRect);
        this.mDisplayContent.getBounds(this.mTmpRect2);
        return this.mTmpRect.intersect(this.mTmpRect2);
    }

    private void setMinimizedDockedStack(boolean minimizedDock, boolean animate) {
        boolean wasMinimized = this.mMinimizedDock;
        this.mMinimizedDock = minimizedDock;
        if (minimizedDock != wasMinimized) {
            boolean imeChanged = clearImeAdjustAnimation();
            boolean minimizedChange = false;
            if (isHomeStackResizable()) {
                notifyDockedStackMinimizedChanged(minimizedDock, animate, true);
                minimizedChange = true;
            } else if (minimizedDock) {
                if (animate) {
                    startAdjustAnimation(0.0f, 1.0f);
                } else {
                    minimizedChange = false | setMinimizedDockedStack(true);
                }
            } else if (animate) {
                startAdjustAnimation(1.0f, 0.0f);
            } else {
                minimizedChange = false | setMinimizedDockedStack(false);
            }
            if (imeChanged || minimizedChange) {
                if (imeChanged && !minimizedChange) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setMinimizedDockedStack: IME adjust changed due to minimizing, minimizedDock=");
                    stringBuilder.append(minimizedDock);
                    stringBuilder.append(" minimizedChange=");
                    stringBuilder.append(minimizedChange);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mService.mWindowPlacerLocked.performSurfacePlacement();
            }
        }
    }

    private boolean clearImeAdjustAnimation() {
        boolean changed = this.mDisplayContent.clearImeAdjustAnimation();
        this.mAnimatingForIme = false;
        return changed;
    }

    private void startAdjustAnimation(float from, float to) {
        this.mAnimatingForMinimizedDockedStack = true;
        this.mAnimationStarted = false;
        this.mAnimationStart = from;
        this.mAnimationTarget = to;
    }

    private void startImeAdjustAnimation(boolean adjustedForIme, boolean adjustedForDivider, WindowState imeWin) {
        float f = 0.0f;
        if (this.mAnimatingForIme) {
            this.mAnimationStart = this.mLastAnimationProgress;
            this.mDividerAnimationStart = this.mLastDividerProgress;
        } else {
            this.mAnimationStart = this.mAdjustedForIme ? 1.0f : 0.0f;
            this.mDividerAnimationStart = this.mAdjustedForDivider ? 1.0f : 0.0f;
            this.mLastAnimationProgress = this.mAnimationStart;
            this.mLastDividerProgress = this.mDividerAnimationStart;
        }
        boolean z = true;
        this.mAnimatingForIme = true;
        this.mAnimationStarted = false;
        this.mAnimationTarget = adjustedForIme ? 1.0f : 0.0f;
        if (adjustedForDivider) {
            f = 1.0f;
        }
        this.mDividerAnimationTarget = f;
        this.mDisplayContent.beginImeAdjustAnimation();
        if (this.mService.mWaitingForDrawn.isEmpty()) {
            if (!(adjustedForIme || adjustedForDivider)) {
                z = false;
            }
            notifyAdjustedForImeChanged(z, IME_ADJUST_ANIM_DURATION);
            return;
        }
        this.mService.mH.removeMessages(24);
        this.mService.mH.sendEmptyMessageDelayed(24, IME_ADJUST_DRAWN_TIMEOUT);
        this.mAnimationStartDelayed = true;
        if (imeWin != null) {
            if (this.mDelayedImeWin != null) {
                this.mDelayedImeWin.endDelayingAnimationStart();
            }
            this.mDelayedImeWin = imeWin;
            imeWin.startDelayingAnimationStart();
        }
        if (this.mService.mWaitingForDrawnCallback != null) {
            this.mService.mWaitingForDrawnCallback.run();
        }
        this.mService.mWaitingForDrawnCallback = new -$$Lambda$DockedStackDividerController$5bA1vUPZ2WAWRKwBSEsFIfWUu9o(this, adjustedForIme, adjustedForDivider);
    }

    public static /* synthetic */ void lambda$startImeAdjustAnimation$0(DockedStackDividerController dockedStackDividerController, boolean adjustedForIme, boolean adjustedForDivider) {
        synchronized (dockedStackDividerController.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean z = false;
                dockedStackDividerController.mAnimationStartDelayed = false;
                if (dockedStackDividerController.mDelayedImeWin != null) {
                    dockedStackDividerController.mDelayedImeWin.endDelayingAnimationStart();
                }
                long duration = 0;
                if (dockedStackDividerController.mAdjustedForIme == adjustedForIme && dockedStackDividerController.mAdjustedForDivider == adjustedForDivider) {
                    duration = IME_ADJUST_ANIM_DURATION;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IME adjust changed while waiting for drawn: adjustedForIme=");
                    stringBuilder.append(adjustedForIme);
                    stringBuilder.append(" adjustedForDivider=");
                    stringBuilder.append(adjustedForDivider);
                    stringBuilder.append(" mAdjustedForIme=");
                    stringBuilder.append(dockedStackDividerController.mAdjustedForIme);
                    stringBuilder.append(" mAdjustedForDivider=");
                    stringBuilder.append(dockedStackDividerController.mAdjustedForDivider);
                    Slog.w(str, stringBuilder.toString());
                }
                if (!dockedStackDividerController.mAdjustedForIme) {
                    if (!dockedStackDividerController.mAdjustedForDivider) {
                        dockedStackDividerController.notifyAdjustedForImeChanged(z, duration);
                    }
                }
                z = true;
                dockedStackDividerController.notifyAdjustedForImeChanged(z, duration);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private boolean setMinimizedDockedStack(boolean minimized) {
        TaskStack stack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        notifyDockedStackMinimizedChanged(minimized, false, isHomeStackResizable());
        if (stack == null) {
            return false;
        }
        if (stack.setAdjustedForMinimizedDock(minimized ? 1.0f : 0.0f)) {
            return true;
        }
        return false;
    }

    private boolean isAnimationMaximizing() {
        return this.mAnimationTarget == 0.0f;
    }

    public boolean animate(long now) {
        if (this.mWindow == null) {
            return false;
        }
        if (this.mAnimatingForMinimizedDockedStack) {
            return animateForMinimizedDockedStack(now);
        }
        if (this.mAnimatingForIme) {
            return animateForIme(now);
        }
        return false;
    }

    private boolean animateForIme(long now) {
        if (!this.mAnimationStarted || this.mAnimationStartDelayed) {
            this.mAnimationStarted = true;
            this.mAnimationStartTime = now;
            this.mAnimationDuration = (long) (280.0f * this.mService.getWindowAnimationScaleLocked());
        }
        float t = (this.mAnimationTarget == 1.0f ? IME_ADJUST_ENTRY_INTERPOLATOR : AppTransition.TOUCH_RESPONSE_INTERPOLATOR).getInterpolation(Math.min(1.0f, ((float) (now - this.mAnimationStartTime)) / ((float) this.mAnimationDuration)));
        if (this.mDisplayContent.animateForIme(t, this.mAnimationTarget, this.mDividerAnimationTarget)) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (t < 1.0f) {
            return true;
        }
        this.mLastAnimationProgress = this.mAnimationTarget;
        this.mLastDividerProgress = this.mDividerAnimationTarget;
        this.mAnimatingForIme = false;
        return false;
    }

    private boolean animateForMinimizedDockedStack(long now) {
        TaskStack stack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (!this.mAnimationStarted) {
            this.mAnimationStarted = true;
            this.mAnimationStartTime = now;
            notifyDockedStackMinimizedChanged(this.mMinimizedDock, true, isHomeStackResizable());
        }
        float t = (isAnimationMaximizing() ? AppTransition.TOUCH_RESPONSE_INTERPOLATOR : this.mMinimizedDockInterpolator).getInterpolation(Math.min(1.0f, ((float) (now - this.mAnimationStartTime)) / ((float) this.mAnimationDuration)));
        if (stack != null && stack.setAdjustedForMinimizedDock(getMinimizeAmount(stack, t))) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (t < 1.0f) {
            return true;
        }
        this.mAnimatingForMinimizedDockedStack = false;
        return false;
    }

    float getInterpolatedAnimationValue(float t) {
        return (this.mAnimationTarget * t) + ((1.0f - t) * this.mAnimationStart);
    }

    float getInterpolatedDividerValue(float t) {
        return (this.mDividerAnimationTarget * t) + ((1.0f - t) * this.mDividerAnimationStart);
    }

    private float getMinimizeAmount(TaskStack stack, float t) {
        float naturalAmount = getInterpolatedAnimationValue(t);
        if (isAnimationMaximizing()) {
            return adjustMaximizeAmount(stack, t, naturalAmount);
        }
        return naturalAmount;
    }

    private float adjustMaximizeAmount(TaskStack stack, float t, float naturalAmount) {
        if (this.mMaximizeMeetFraction == 1.0f) {
            return naturalAmount;
        }
        float amountPrime = (this.mAnimationTarget * t) + ((1.0f - t) * (((float) this.mService.mAppTransition.getLastClipRevealMaxTranslation()) / ((float) stack.getMinimizeDistance())));
        float t2 = Math.min(t / this.mMaximizeMeetFraction, 1.0f);
        return (amountPrime * t2) + ((1.0f - t2) * naturalAmount);
    }

    private float getClipRevealMeetFraction(TaskStack stack) {
        if (!isAnimationMaximizing() || stack == null || !this.mService.mAppTransition.hadClipRevealAnimation()) {
            return 1.0f;
        }
        return CLIP_REVEAL_MEET_EARLIEST + ((1.0f - Math.max(0.0f, Math.min(1.0f, ((((float) Math.abs(this.mService.mAppTransition.getLastClipRevealMaxTranslation())) / ((float) stack.getMinimizeDistance())) - CLIP_REVEAL_MEET_FRACTION_MIN) / CLIP_REVEAL_MEET_FRACTION_MIN))) * 0.39999998f);
    }

    public String toShortString() {
        return TAG;
    }

    WindowState getWindow() {
        return this.mWindow;
    }

    void dump(String prefix, PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("DockedStackDividerController");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mLastVisibility=");
        stringBuilder.append(this.mLastVisibility);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mMinimizedDock=");
        stringBuilder.append(this.mMinimizedDock);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mAdjustedForIme=");
        stringBuilder.append(this.mAdjustedForIme);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mAdjustedForDivider=");
        stringBuilder.append(this.mAdjustedForDivider);
        pw.println(stringBuilder.toString());
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mMinimizedDock);
        proto.end(token);
    }

    public void setDockedStackDividerRotation(int rotation) {
        this.mRotation = rotation;
    }

    public void adjustBoundsForSingleHand() {
    }
}
