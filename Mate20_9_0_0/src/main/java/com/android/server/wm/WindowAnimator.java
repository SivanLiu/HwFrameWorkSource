package com.android.server.wm;

import android.content.Context;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;
import android.view.SurfaceControl.Transaction;
import com.android.server.AnimationThread;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;

public class WindowAnimator {
    private static final long KEYGUARD_ANIM_TIMEOUT_MS = 2000;
    private static final String TAG = "WindowManager";
    private final ArrayList<Runnable> mAfterPrepareSurfacesRunnables = new ArrayList();
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
    private boolean mInExecuteAfterPrepareSurfacesRunnables;
    private boolean mInitialized = false;
    boolean mIsLazying = false;
    private boolean mLastRootAnimating;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    private boolean mRemoveReplacedWindows = false;
    final WindowManagerService mService;
    private final Transaction mTransaction = new Transaction();
    WindowState mWindowDetachedWallpaper = null;
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
        AnimationThread.getHandler().runWithScissors(new -$$Lambda$WindowAnimator$U3Fu5_RzEyNo8Jt6zTb2ozdXiqM(this), 0);
        this.mAnimationFrameCallback = new -$$Lambda$WindowAnimator$ddXU8gK8rmDqri0OZVMNa3Y4GHk(this);
    }

    public static /* synthetic */ void lambda$new$1(WindowAnimator windowAnimator, long frameTimeNs) {
        synchronized (windowAnimator.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                windowAnimator.mAnimationFrameCallbackScheduled = false;
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        windowAnimator.animate(frameTimeNs);
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

    /* JADX WARNING: Missing block: B:10:0x0015, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            r1 = r12.mService.mWindowMap;
     */
    /* JADX WARNING: Missing block: B:11:0x001c, code:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            com.android.server.wm.WindowManagerService.boostPriorityForLockedSection();
            r12.mCurrentTime = r13 / 1000000;
            r12.mBulkUpdateParams = 8;
            r12.mAnimating = false;
            r12.mIsLazying = false;
            r12.mService.openSurfaceTransaction();
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r3 = r12.mService.mAccessibilityController;
            r4 = r12.mDisplayContentsAnimators.size();
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:17:0x0041, code:
            if (r5 >= r4) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:18:0x0043, code:
            r7 = r12.mService.mRoot.getDisplayContent(r12.mDisplayContentsAnimators.keyAt(r5));
            r8 = (com.android.server.wm.WindowAnimator.DisplayContentsAnimator) r12.mDisplayContentsAnimators.valueAt(r5);
            r9 = r8.mScreenRotationAnimation;
     */
    /* JADX WARNING: Missing block: B:19:0x005b, code:
            if (r9 == null) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:21:0x0061, code:
            if (r9.isAnimating() == false) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:23:0x0069, code:
            if (r9.stepAnimationLocked(r12.mCurrentTime) == false) goto L_0x006f;
     */
    /* JADX WARNING: Missing block: B:24:0x006b, code:
            setAnimating(true);
     */
    /* JADX WARNING: Missing block: B:25:0x006f, code:
            r12.mBulkUpdateParams |= 1;
            r9.kill();
            r8.mScreenRotationAnimation = null;
     */
    /* JADX WARNING: Missing block: B:26:0x007a, code:
            if (r3 == null) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:28:0x007e, code:
            if (r7.isDefaultDisplay == false) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:29:0x0080, code:
            r3.onRotationChangedLocked(r12.mService.getDefaultDisplayContentLocked());
     */
    /* JADX WARNING: Missing block: B:30:0x0089, code:
            r12.mAnimTransactionSequence++;
            r7.updateWindowsForAnimator(r12);
            r7.updateWallpaperForAnimator(r12);
            r7.prepareSurfaces();
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:31:0x009a, code:
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:32:0x009b, code:
            if (r5 >= r4) goto L_0x00e7;
     */
    /* JADX WARNING: Missing block: B:33:0x009d, code:
            r6 = r12.mDisplayContentsAnimators.keyAt(r5);
            r7 = r12.mService.mRoot.getDisplayContent(r6);
            r7.checkAppWindowsReadyToShow();
            r8 = ((com.android.server.wm.WindowAnimator.DisplayContentsAnimator) r12.mDisplayContentsAnimators.valueAt(r5)).mScreenRotationAnimation;
     */
    /* JADX WARNING: Missing block: B:34:0x00b8, code:
            if (r8 == null) goto L_0x00bf;
     */
    /* JADX WARNING: Missing block: B:35:0x00ba, code:
            r8.updateSurfaces(r12.mTransaction);
     */
    /* JADX WARNING: Missing block: B:36:0x00bf, code:
            orAnimating(r7.getDockedDividerController().animate(r12.mCurrentTime));
            updateBlurLayers(r6);
     */
    /* JADX WARNING: Missing block: B:37:0x00cf, code:
            if (r3 == null) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:39:0x00d3, code:
            if (r7.isDefaultDisplay != false) goto L_0x00e1;
     */
    /* JADX WARNING: Missing block: B:41:0x00d9, code:
            if (android.util.HwPCUtils.enabledInPad() == false) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:43:0x00df, code:
            if (android.util.HwPCUtils.isPcCastModeInServer() == false) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:44:0x00e1, code:
            r3.drawMagnifiedRegionBorderIfNeededLocked();
     */
    /* JADX WARNING: Missing block: B:45:0x00e4, code:
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:47:0x00e9, code:
            if (r12.mAnimating != false) goto L_0x00f2;
     */
    /* JADX WARNING: Missing block: B:49:0x00ed, code:
            if (r12.mIsLazying != false) goto L_0x00f2;
     */
    /* JADX WARNING: Missing block: B:50:0x00ef, code:
            cancelAnimation();
     */
    /* JADX WARNING: Missing block: B:52:0x00f6, code:
            if (r12.mService.mWatermark == null) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:53:0x00f8, code:
            r12.mService.mWatermark.drawIfNeeded();
     */
    /* JADX WARNING: Missing block: B:54:0x00ff, code:
            android.view.SurfaceControl.mergeToGlobalTransaction(r12.mTransaction);
     */
    /* JADX WARNING: Missing block: B:56:?, code:
            r3 = r12.mService;
            r4 = "WindowAnimator";
     */
    /* JADX WARNING: Missing block: B:58:0x010c, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:60:?, code:
            android.util.Slog.wtf(TAG, "Unhandled exception in Window Manager", r3);
     */
    /* JADX WARNING: Missing block: B:62:?, code:
            r3 = r12.mService;
            r4 = "WindowAnimator";
     */
    /* JADX WARNING: Missing block: B:63:0x0118, code:
            r3.closeSurfaceTransaction(r4);
            r3 = r12.mService.mRoot.hasPendingLayoutChanges(r12);
            r4 = false;
     */
    /* JADX WARNING: Missing block: B:64:0x0127, code:
            if (r12.mBulkUpdateParams != 0) goto L_0x0129;
     */
    /* JADX WARNING: Missing block: B:65:0x0129, code:
            r4 = r12.mService.mRoot.copyAnimToLayoutParams();
     */
    /* JADX WARNING: Missing block: B:68:0x0136, code:
            r12.mService.mWindowPlacerLocked.requestTraversal();
     */
    /* JADX WARNING: Missing block: B:69:0x013d, code:
            r5 = r12.mService.mRoot.isSelfOrChildAnimating();
     */
    /* JADX WARNING: Missing block: B:73:0x014d, code:
            r12.mService.mTaskSnapshotController.setPersisterPaused(true);
     */
    /* JADX WARNING: Missing block: B:74:0x0158, code:
            if (android.util.Jlog.isPerfTest() != false) goto L_0x015a;
     */
    /* JADX WARNING: Missing block: B:75:0x015a, code:
            android.util.Jlog.i(3052, android.util.Jlog.getMessage("WindowAnimator", "animate", "ANIMATE_BEGIN"));
     */
    /* JADX WARNING: Missing block: B:76:0x0169, code:
            android.os.Trace.asyncTraceBegin(32, "animating", 0);
     */
    /* JADX WARNING: Missing block: B:80:0x0174, code:
            r12.mService.mWindowPlacerLocked.requestTraversal();
            r12.mService.mTaskSnapshotController.setPersisterPaused(false);
            android.os.Trace.asyncTraceEnd(32, "animating", 0);
     */
    /* JADX WARNING: Missing block: B:81:0x018b, code:
            if (android.util.Jlog.isPerfTest() != false) goto L_0x018d;
     */
    /* JADX WARNING: Missing block: B:82:0x018d, code:
            android.util.Jlog.i(3055, android.util.Jlog.getMessage("WindowAnimator", "animate", "ANIMATE_END"));
     */
    /* JADX WARNING: Missing block: B:83:0x019c, code:
            r12.mLastRootAnimating = r5;
     */
    /* JADX WARNING: Missing block: B:84:0x01a0, code:
            if (r12.mRemoveReplacedWindows != false) goto L_0x01a2;
     */
    /* JADX WARNING: Missing block: B:85:0x01a2, code:
            r12.mService.mRoot.removeReplacedWindows();
            r12.mRemoveReplacedWindows = false;
     */
    /* JADX WARNING: Missing block: B:86:0x01ab, code:
            r12.mService.destroyPreservedSurfaceLocked();
            executeAfterPrepareSurfacesRunnables();
     */
    /* JADX WARNING: Missing block: B:87:0x01b3, code:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:88:0x01b4, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:89:0x01b7, code:
            return;
     */
    /* JADX WARNING: Missing block: B:91:?, code:
            r12.mService.closeSurfaceTransaction("WindowAnimator");
     */
    /* JADX WARNING: Missing block: B:94:0x01c2, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void animate(long frameTimeNs) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mInitialized) {
                    scheduleAnimation();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
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
        return builder.toString();
    }

    public void dumpLocked(PrintWriter pw, String prefix, boolean dumpAll) {
        String subPrefix = new StringBuilder();
        subPrefix.append("  ");
        subPrefix.append(prefix);
        subPrefix = subPrefix.toString();
        String subSubPrefix = new StringBuilder();
        subSubPrefix.append("  ");
        subSubPrefix.append(subPrefix);
        subSubPrefix = subSubPrefix.toString();
        for (int i = 0; i < this.mDisplayContentsAnimators.size(); i++) {
            pw.print(prefix);
            pw.print("DisplayContentsAnimator #");
            pw.print(this.mDisplayContentsAnimators.keyAt(i));
            pw.println(":");
            DisplayContentsAnimator displayAnimator = (DisplayContentsAnimator) this.mDisplayContentsAnimators.valueAt(i);
            this.mService.mRoot.getDisplayContent(this.mDisplayContentsAnimators.keyAt(i)).dumpWindowAnimators(pw, subPrefix);
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
        DisplayContent displayContent = this.mService.mRoot.getDisplayContent(displayId);
        if (displayContent != null) {
            i = displayContent.pendingLayoutChanges;
        }
        return i;
    }

    void setPendingLayoutChanges(int displayId, int changes) {
        if (displayId >= 0) {
            DisplayContent displayContent = this.mService.mRoot.getDisplayContent(displayId);
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

    private void cancelAnimation() {
        if (this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = false;
            this.mChoreographer.removeFrameCallback(this.mAnimationFrameCallback);
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

    void addAfterPrepareSurfacesRunnable(Runnable r) {
        if (this.mInExecuteAfterPrepareSurfacesRunnables) {
            r.run();
            return;
        }
        this.mAfterPrepareSurfacesRunnables.add(r);
        scheduleAnimation();
    }

    void executeAfterPrepareSurfacesRunnables() {
        if (!this.mInExecuteAfterPrepareSurfacesRunnables) {
            this.mInExecuteAfterPrepareSurfacesRunnables = true;
            int size = this.mAfterPrepareSurfacesRunnables.size();
            for (int i = 0; i < size; i++) {
                ((Runnable) this.mAfterPrepareSurfacesRunnables.get(i)).run();
            }
            this.mAfterPrepareSurfacesRunnables.clear();
            this.mInExecuteAfterPrepareSurfacesRunnables = false;
        }
    }

    void setIsLazying(boolean isLazying) {
        this.mIsLazying = isLazying;
    }

    private void updateBlurLayers(int displayId) {
        DisplayContent displayContent = this.mService.mRoot.getDisplayContent(displayId);
        if (displayContent != null) {
            displayContent.forAllWindows((Consumer) -$$Lambda$WindowAnimator$QPolCGKJCfzSmj0sq1NSNBg04bk.INSTANCE, false);
        }
    }

    static /* synthetic */ void lambda$updateBlurLayers$2(WindowState win) {
        if ((win.mAttrs.flags & 4) != 0 && win.isDisplayedLw() && !win.mAnimatingExit) {
            win.mWinAnimator.updateBlurLayer(win.mAttrs);
        }
    }
}
