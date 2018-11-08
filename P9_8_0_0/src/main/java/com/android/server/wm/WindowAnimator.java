package com.android.server.wm;

import android.content.Context;
import android.os.Trace;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;
import android.view.WindowManagerPolicy;
import com.android.server.AnimationThread;
import com.android.server.wm.-$Lambda$OQfQhd_xsxt9hoLAjIbVfOwa-jY.AnonymousClass1;
import com.android.server.wm.-$Lambda$OQfQhd_xsxt9hoLAjIbVfOwa-jY.AnonymousClass2;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class WindowAnimator {
    private static final long KEYGUARD_ANIM_TIMEOUT_MS = 2000;
    private static final String TAG = "WindowManager";
    int mAnimTransactionSequence;
    private boolean mAnimating;
    final FrameCallback mAnimationFrameCallback;
    private boolean mAnimationFrameCallbackScheduled;
    boolean mAppWindowAnimating;
    int mBulkUpdateParams = 0;
    private Choreographer mChoreographer;
    final Context mContext;
    long mCurrentTime;
    SparseArray<DisplayContentsAnimator> mDisplayContentsAnimators = new SparseArray(2);
    boolean mInitialized = false;
    boolean mIsLazying = false;
    private boolean mLastAnimating;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    private boolean mRemoveReplacedWindows = false;
    final WindowManagerService mService;
    WindowState mWindowDetachedWallpaper = null;
    private final WindowSurfacePlacer mWindowPlacerLocked;
    int offsetLayer = 0;

    private class DisplayContentsAnimator {
        ScreenRotationAnimation mScreenRotationAnimation;

        private DisplayContentsAnimator() {
            this.mScreenRotationAnimation = null;
        }
    }

    WindowAnimator(WindowManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mPolicy = service.mPolicy;
        this.mWindowPlacerLocked = service.mWindowPlacerLocked;
        AnimationThread.getHandler().runWithScissors(new AnonymousClass2(this), 0);
        this.mAnimationFrameCallback = new AnonymousClass1(this);
    }

    /* synthetic */ void lambda$-com_android_server_wm_WindowAnimator_4391() {
        this.mChoreographer = Choreographer.getSfInstance();
    }

    /* synthetic */ void lambda$-com_android_server_wm_WindowAnimator_4498(long frameTimeNs) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAnimationFrameCallbackScheduled = false;
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        animate(frameTimeNs);
    }

    void addDisplayLocked(int displayId) {
        getDisplayContentsAnimatorLocked(displayId);
        if (displayId == 0) {
            this.mInitialized = true;
        }
    }

    void removeDisplayLocked(int displayId) {
        DisplayContentsAnimator displayAnimator = (DisplayContentsAnimator) this.mDisplayContentsAnimators.get(displayId);
        if (!(displayAnimator == null || displayAnimator.mScreenRotationAnimation == null)) {
            displayAnimator.mScreenRotationAnimation.kill();
            displayAnimator.mScreenRotationAnimation = null;
        }
        this.mDisplayContentsAnimators.delete(displayId);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void animate(long frameTimeNs) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mInitialized) {
                    scheduleAnimation();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        boolean hasPendingLayoutChanges = this.mService.mRoot.hasPendingLayoutChanges(this);
        boolean doRequest = false;
        if (this.mBulkUpdateParams != 0) {
            doRequest = this.mService.mRoot.copyAnimToLayoutParams();
        }
        if (hasPendingLayoutChanges || r6) {
            this.mWindowPlacerLocked.requestTraversal();
        }
        if (this.mAnimating && (this.mLastAnimating ^ 1) != 0) {
            this.mService.mTaskSnapshotController.setPersisterPaused(true);
            Trace.asyncTraceBegin(32, "animating", 0);
        }
        if (!this.mAnimating && this.mLastAnimating) {
            this.mWindowPlacerLocked.requestTraversal();
            this.mService.mTaskSnapshotController.setPersisterPaused(false);
            Trace.asyncTraceEnd(32, "animating", 0);
        }
        this.mLastAnimating = this.mAnimating;
        if (this.mRemoveReplacedWindows) {
            this.mService.mRoot.removeReplacedWindows();
            this.mRemoveReplacedWindows = false;
        }
        this.mService.stopUsingSavedSurfaceLocked();
        this.mService.destroyPreservedSurfaceLocked();
        this.mService.mWindowPlacerLocked.destroyPendingSurfaces();
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private void cancelAnimation() {
        if (this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = false;
            this.mChoreographer.removeFrameCallback(this.mAnimationFrameCallback);
        }
    }

    private static String bulkUpdateParamsToString(int bulkUpdateParams) {
        StringBuilder builder = new StringBuilder(128);
        if ((bulkUpdateParams & 1) != 0) {
            builder.append(" UPDATE_ROTATION");
        }
        if ((bulkUpdateParams & 2) != 0) {
            builder.append(" WALLPAPER_MAY_CHANGE");
        }
        if ((bulkUpdateParams & 4) != 0) {
            builder.append(" FORCE_HIDING_CHANGED");
        }
        if ((bulkUpdateParams & 8) != 0) {
            builder.append(" ORIENTATION_CHANGE_COMPLETE");
        }
        if ((bulkUpdateParams & 16) != 0) {
            builder.append(" TURN_ON_SCREEN");
        }
        return builder.toString();
    }

    public void dumpLocked(PrintWriter pw, String prefix, boolean dumpAll) {
        String subPrefix = "  " + prefix;
        String subSubPrefix = "  " + subPrefix;
        for (int i = 0; i < this.mDisplayContentsAnimators.size(); i++) {
            pw.print(prefix);
            pw.print("DisplayContentsAnimator #");
            pw.print(this.mDisplayContentsAnimators.keyAt(i));
            pw.println(":");
            DisplayContentsAnimator displayAnimator = (DisplayContentsAnimator) this.mDisplayContentsAnimators.valueAt(i);
            this.mService.mRoot.getDisplayContentOrCreate(this.mDisplayContentsAnimators.keyAt(i)).dumpWindowAnimators(pw, subPrefix);
            if (displayAnimator.mScreenRotationAnimation != null) {
                pw.print(subPrefix);
                pw.println("mScreenRotationAnimation:");
                displayAnimator.mScreenRotationAnimation.printTo(subSubPrefix, pw);
            } else if (dumpAll) {
                pw.print(subPrefix);
                pw.println("no ScreenRotationAnimation ");
            }
            pw.println();
        }
        pw.println();
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mAnimTransactionSequence=");
            pw.print(this.mAnimTransactionSequence);
            pw.print(prefix);
            pw.print("mCurrentTime=");
            pw.println(TimeUtils.formatUptime(this.mCurrentTime));
        }
        if (this.mBulkUpdateParams != 0) {
            pw.print(prefix);
            pw.print("mBulkUpdateParams=0x");
            pw.print(Integer.toHexString(this.mBulkUpdateParams));
            pw.println(bulkUpdateParamsToString(this.mBulkUpdateParams));
        }
        if (this.mWindowDetachedWallpaper != null) {
            pw.print(prefix);
            pw.print("mWindowDetachedWallpaper=");
            pw.println(this.mWindowDetachedWallpaper);
        }
    }

    int getPendingLayoutChanges(int displayId) {
        int i = 0;
        if (displayId < 0) {
            return 0;
        }
        DisplayContent displayContent = this.mService.mRoot.getDisplayContentOrCreate(displayId);
        if (displayContent != null) {
            i = displayContent.pendingLayoutChanges;
        }
        return i;
    }

    void setPendingLayoutChanges(int displayId, int changes) {
        if (displayId >= 0) {
            DisplayContent displayContent = this.mService.mRoot.getDisplayContentOrCreate(displayId);
            if (displayContent != null) {
                displayContent.pendingLayoutChanges |= changes;
            }
        }
    }

    private DisplayContentsAnimator getDisplayContentsAnimatorLocked(int displayId) {
        if (displayId < 0) {
            return null;
        }
        DisplayContentsAnimator displayAnimator = (DisplayContentsAnimator) this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator == null && this.mService.mRoot.getDisplayContent(displayId) != null) {
            displayAnimator = new DisplayContentsAnimator();
            this.mDisplayContentsAnimators.put(displayId, displayAnimator);
        }
        return displayAnimator;
    }

    void setScreenRotationAnimationLocked(int displayId, ScreenRotationAnimation animation) {
        DisplayContentsAnimator animator = getDisplayContentsAnimatorLocked(displayId);
        if (animator != null) {
            animator.mScreenRotationAnimation = animation;
        }
    }

    ScreenRotationAnimation getScreenRotationAnimationLocked(int displayId) {
        ScreenRotationAnimation screenRotationAnimation = null;
        if (displayId < 0) {
            return null;
        }
        DisplayContentsAnimator animator = getDisplayContentsAnimatorLocked(displayId);
        if (animator != null) {
            screenRotationAnimation = animator.mScreenRotationAnimation;
        }
        return screenRotationAnimation;
    }

    void requestRemovalOfReplacedWindows(WindowState win) {
        this.mRemoveReplacedWindows = true;
    }

    void scheduleAnimation() {
        if (!this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = true;
            this.mChoreographer.postFrameCallback(this.mAnimationFrameCallback);
        }
    }

    boolean isAnimating() {
        return this.mAnimating;
    }

    boolean isAnimationScheduled() {
        return this.mAnimationFrameCallbackScheduled;
    }

    Choreographer getChoreographer() {
        return this.mChoreographer;
    }

    void setAnimating(boolean animating) {
        this.mAnimating = animating;
    }

    void orAnimating(boolean animating) {
        this.mAnimating |= animating;
    }

    void setIsLazying(boolean isLazying) {
        this.mIsLazying = isLazying;
    }

    private void updateBlurLayers(int displayId) {
        this.mService.mRoot.getDisplayContentOrCreate(displayId).forAllWindows((Consumer) new -$Lambda$OQfQhd_xsxt9hoLAjIbVfOwa-jY(), false);
    }

    static /* synthetic */ void lambda$-com_android_server_wm_WindowAnimator_20360(WindowState win) {
        if ((win.mAttrs.flags & 4) != 0 && win.isDisplayedLw() && (win.mAnimatingExit ^ 1) != 0) {
            win.mWinAnimator.updateBlurLayer(win.mAttrs);
        }
    }
}
