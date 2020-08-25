package com.android.server.wm;

import android.content.Context;
import android.hardware.display.HwFoldScreenState;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.SurfaceControl;
import com.android.server.AnimationThread;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;

public class WindowAnimator {
    private static final boolean PROP_FOLD_SWITCH_DEBUG = SystemProperties.getBoolean("persist.debug.fold_switch", true);
    private static final String TAG = "WindowManager";
    private final ArrayList<Runnable> mAfterPrepareSurfacesRunnables = new ArrayList<>();
    private boolean mAnimating;
    final Choreographer.FrameCallback mAnimationFrameCallback;
    private boolean mAnimationFrameCallbackScheduled;
    int mBulkUpdateParams = 0;
    private Choreographer mChoreographer;
    final Context mContext;
    long mCurrentTime;
    SparseArray<DisplayContentsAnimator> mDisplayContentsAnimators = new SparseArray<>(2);
    private HwFoldScreenManagerInternal mFsmInternal;
    private boolean mInExecuteAfterPrepareSurfacesRunnables;
    private boolean mInitialized = false;
    boolean mIsLazying = false;
    private boolean mLastRootAnimating;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    private boolean mRemoveReplacedWindows = false;
    final WindowManagerService mService;
    private final SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();
    int offsetLayer = 0;

    WindowAnimator(WindowManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mPolicy = service.mPolicy;
        AnimationThread.getHandler().runWithScissors(new Runnable() {
            /* class com.android.server.wm.$$Lambda$WindowAnimator$U3Fu5_RzEyNo8Jt6zTb2ozdXiqM */

            public final void run() {
                WindowAnimator.this.lambda$new$0$WindowAnimator();
            }
        }, 0);
        this.mAnimationFrameCallback = new Choreographer.FrameCallback() {
            /* class com.android.server.wm.$$Lambda$WindowAnimator$ddXU8gK8rmDqri0OZVMNa3Y4GHk */

            public final void doFrame(long j) {
                WindowAnimator.this.lambda$new$1$WindowAnimator(j);
            }
        };
    }

    public /* synthetic */ void lambda$new$0$WindowAnimator() {
        this.mChoreographer = Choreographer.getSfInstance();
    }

    /* JADX INFO: finally extract failed */
    public /* synthetic */ void lambda$new$1$WindowAnimator(long frameTimeNs) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAnimationFrameCallbackScheduled = false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        animate(frameTimeNs);
    }

    /* access modifiers changed from: package-private */
    public void addDisplayLocked(int displayId) {
        getDisplayContentsAnimatorLocked(displayId);
    }

    /* access modifiers changed from: package-private */
    public void removeDisplayLocked(int displayId) {
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (!(displayAnimator == null || displayAnimator.mScreenRotationAnimation == null)) {
            displayAnimator.mScreenRotationAnimation.kill();
            displayAnimator.mScreenRotationAnimation = null;
        }
        this.mDisplayContentsAnimators.delete(displayId);
    }

    /* access modifiers changed from: package-private */
    public void ready() {
        this.mInitialized = true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0015, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
        r1 = r12.mService.mGlobalLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x001c, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:?, code lost:
        com.android.server.wm.WindowManagerService.boostPriorityForLockedSection();
        r12.mCurrentTime = r13 / 1000000;
        r12.mBulkUpdateParams = 4;
        r12.mAnimating = false;
        r12.mIsLazying = false;
        r12.mService.openSurfaceTransaction();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:?, code lost:
        r3 = r12.mService.mAccessibilityController;
        r4 = r12.mDisplayContentsAnimators.size();
        r5 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0040, code lost:
        if (r5 >= r4) goto L_0x00b2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0042, code lost:
        r7 = r12.mService.mRoot.getDisplayContent(r12.mDisplayContentsAnimators.keyAt(r5));
        r8 = r12.mDisplayContentsAnimators.valueAt(r5);
        r9 = r8.mScreenRotationAnimation;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x005a, code lost:
        if (r9 == null) goto L_0x00a6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0060, code lost:
        if (r9.isAnimating() == false) goto L_0x00a6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0068, code lost:
        if (r9.stepAnimationLocked(r12.mCurrentTime) == false) goto L_0x006e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x006a, code lost:
        setAnimating(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x006e, code lost:
        r12.mBulkUpdateParams |= 1;
        r9.kill();
        r8.mScreenRotationAnimation = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0079, code lost:
        if (r3 == null) goto L_0x007e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x007b, code lost:
        r3.onRotationChangedLocked(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0082, code lost:
        if (android.hardware.display.HwFoldScreenState.isFoldScreenDevice() == false) goto L_0x00a3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x008a, code lost:
        if (r12.mService.isFoldRotationFreezed() == false) goto L_0x00a3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x008c, code lost:
        android.util.Slog.i(com.android.server.wm.WindowAnimator.TAG, "screen fold enter anim done: send unfreeze fold rotation msg");
        r12.mService.mH.removeMessages(com.android.server.wm.WindowManagerService.H.UNFREEZE_FOLD_ROTATION);
        r12.mService.mH.sendEmptyMessage(com.android.server.wm.WindowManagerService.H.UNFREEZE_FOLD_ROTATION);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00a3, code lost:
        handleResumeDispModeChange(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x00a6, code lost:
        r7.updateWindowsForAnimator();
        r7.updateBackgroundForAnimator();
        r7.prepareSurfaces();
        r5 = r5 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00b2, code lost:
        r5 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00b3, code lost:
        if (r5 >= r4) goto L_0x00ec;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00b5, code lost:
        r6 = r12.mDisplayContentsAnimators.keyAt(r5);
        r7 = r12.mService.mRoot.getDisplayContent(r6);
        r7.checkAppWindowsReadyToShow();
        r8 = r12.mDisplayContentsAnimators.valueAt(r5).mScreenRotationAnimation;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00d0, code lost:
        if (r8 == null) goto L_0x00d7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00d2, code lost:
        r8.updateSurfaces(r12.mTransaction);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00d7, code lost:
        orAnimating(r7.getDockedDividerController().animate(r12.mCurrentTime));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00e4, code lost:
        if (r3 == null) goto L_0x00e9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00e6, code lost:
        r3.drawMagnifiedRegionBorderIfNeededLocked(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00e9, code lost:
        r5 = r5 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00ee, code lost:
        if (r12.mAnimating != false) goto L_0x00f7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00f2, code lost:
        if (r12.mIsLazying != false) goto L_0x00f7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00f4, code lost:
        cancelAnimation();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00fb, code lost:
        if (r12.mService.mWatermark == null) goto L_0x0104;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00fd, code lost:
        r12.mService.mWatermark.drawIfNeeded();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x0104, code lost:
        android.view.SurfaceControl.mergeToGlobalTransaction(r12.mTransaction);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x0109, code lost:
        r3 = r12.mService;
        r4 = "WindowAnimator";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x010e, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x0111, code lost:
        r3 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:?, code lost:
        android.util.Slog.wtf(com.android.server.wm.WindowAnimator.TAG, "Unhandled exception in Window Manager", r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0119, code lost:
        r3 = r12.mService;
        r4 = "WindowAnimator";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0194, code lost:
        r12.mService.closeSurfaceTransaction("WindowAnimator");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x019b, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x019c, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x019e, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x01a1, code lost:
        throw r0;
     */
    private void animate(long frameTimeNs) {
        WindowManagerGlobalLock windowManagerGlobalLock;
        String str;
        WindowManagerService windowManagerService;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mInitialized) {
                    scheduleAnimation();
                } else {
                    return;
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        windowManagerService.closeSurfaceTransaction(str);
        boolean hasPendingLayoutChanges = this.mService.mRoot.hasPendingLayoutChanges(this);
        boolean doRequest = false;
        if (this.mBulkUpdateParams != 0) {
            doRequest = this.mService.mRoot.copyAnimToLayoutParams();
        }
        if (hasPendingLayoutChanges || doRequest) {
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        boolean rootAnimating = this.mService.mRoot.isSelfOrChildAnimating();
        if (rootAnimating && !this.mLastRootAnimating) {
            this.mService.mTaskSnapshotController.setPersisterPaused(true);
            Trace.asyncTraceBegin(32, "animating", 0);
        }
        if (!rootAnimating && this.mLastRootAnimating) {
            this.mService.mWindowPlacerLocked.requestTraversal();
            this.mService.mTaskSnapshotController.setPersisterPaused(false);
            Trace.asyncTraceEnd(32, "animating", 0);
        }
        this.mLastRootAnimating = rootAnimating;
        if (this.mRemoveReplacedWindows) {
            this.mService.mRoot.removeReplacedWindows();
            this.mRemoveReplacedWindows = false;
        }
        this.mService.destroyPreservedSurfaceLocked();
        executeAfterPrepareSurfacesRunnables();
    }

    private static String bulkUpdateParamsToString(int bulkUpdateParams) {
        StringBuilder builder = new StringBuilder(128);
        if ((bulkUpdateParams & 1) != 0) {
            builder.append(" UPDATE_ROTATION");
        }
        if ((bulkUpdateParams & 4) != 0) {
            builder.append(" ORIENTATION_CHANGE_COMPLETE");
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
            DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.valueAt(i);
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
            pw.print("mCurrentTime=");
            pw.println(TimeUtils.formatUptime(this.mCurrentTime));
        }
        if (this.mBulkUpdateParams != 0) {
            pw.print(prefix);
            pw.print("mBulkUpdateParams=0x");
            pw.print(Integer.toHexString(this.mBulkUpdateParams));
            pw.println(bulkUpdateParamsToString(this.mBulkUpdateParams));
        }
    }

    /* access modifiers changed from: package-private */
    public int getPendingLayoutChanges(int displayId) {
        DisplayContent displayContent;
        if (displayId >= 0 && (displayContent = this.mService.mRoot.getDisplayContent(displayId)) != null) {
            return displayContent.pendingLayoutChanges;
        }
        return 0;
    }

    /* access modifiers changed from: package-private */
    public void setPendingLayoutChanges(int displayId, int changes) {
        DisplayContent displayContent;
        if (displayId >= 0 && (displayContent = this.mService.mRoot.getDisplayContent(displayId)) != null) {
            displayContent.pendingLayoutChanges |= changes;
        }
    }

    private DisplayContentsAnimator getDisplayContentsAnimatorLocked(int displayId) {
        if (displayId < 0) {
            return null;
        }
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator != null || this.mService.mRoot.getDisplayContent(displayId) == null) {
            return displayAnimator;
        }
        DisplayContentsAnimator displayAnimator2 = new DisplayContentsAnimator();
        this.mDisplayContentsAnimators.put(displayId, displayAnimator2);
        return displayAnimator2;
    }

    /* access modifiers changed from: package-private */
    public void setScreenRotationAnimationLocked(int displayId, ScreenRotationAnimation animation) {
        DisplayContentsAnimator animator = getDisplayContentsAnimatorLocked(displayId);
        if (animator != null) {
            animator.mScreenRotationAnimation = animation;
        }
    }

    /* access modifiers changed from: package-private */
    public ScreenRotationAnimation getScreenRotationAnimationLocked(int displayId) {
        DisplayContentsAnimator animator;
        if (displayId >= 0 && (animator = getDisplayContentsAnimatorLocked(displayId)) != null) {
            return animator.mScreenRotationAnimation;
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public void requestRemovalOfReplacedWindows(WindowState win) {
        this.mRemoveReplacedWindows = true;
    }

    /* access modifiers changed from: package-private */
    public void scheduleAnimation() {
        if (!this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = true;
            this.mChoreographer.postFrameCallback(this.mAnimationFrameCallback);
        }
    }

    private void cancelAnimation() {
        if (this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = false;
            this.mChoreographer.removeFrameCallback(this.mAnimationFrameCallback);
            handleResumeDispModeChange(false);
        }
    }

    private class DisplayContentsAnimator {
        ScreenRotationAnimation mScreenRotationAnimation;

        private DisplayContentsAnimator() {
            this.mScreenRotationAnimation = null;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isAnimating() {
        return this.mAnimating;
    }

    /* access modifiers changed from: package-private */
    public boolean isAnimationScheduled() {
        return this.mAnimationFrameCallbackScheduled;
    }

    /* access modifiers changed from: package-private */
    public Choreographer getChoreographer() {
        return this.mChoreographer;
    }

    /* access modifiers changed from: package-private */
    public void setAnimating(boolean animating) {
        this.mAnimating = animating;
    }

    /* access modifiers changed from: package-private */
    public void orAnimating(boolean animating) {
        this.mAnimating |= animating;
    }

    /* access modifiers changed from: package-private */
    public void addAfterPrepareSurfacesRunnable(Runnable r) {
        if (this.mInExecuteAfterPrepareSurfacesRunnables) {
            r.run();
            return;
        }
        this.mAfterPrepareSurfacesRunnables.add(r);
        scheduleAnimation();
    }

    /* access modifiers changed from: package-private */
    public void executeAfterPrepareSurfacesRunnables() {
        if (!this.mInExecuteAfterPrepareSurfacesRunnables) {
            this.mInExecuteAfterPrepareSurfacesRunnables = true;
            int size = this.mAfterPrepareSurfacesRunnables.size();
            for (int i = 0; i < size; i++) {
                this.mAfterPrepareSurfacesRunnables.get(i).run();
            }
            this.mAfterPrepareSurfacesRunnables.clear();
            this.mInExecuteAfterPrepareSurfacesRunnables = false;
        }
    }

    /* access modifiers changed from: package-private */
    public void setIsLazying(boolean isLazying) {
        this.mIsLazying = isLazying;
    }

    private void handleResumeDispModeChange(boolean force) {
        if (HwFoldScreenState.isFoldScreenDevice() && PROP_FOLD_SWITCH_DEBUG) {
            if (this.mFsmInternal == null) {
                this.mFsmInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
            }
            if (this.mFsmInternal == null) {
                return;
            }
            if (force || isAnimating()) {
                Slog.i(TAG, "resumeDispModeChange from WA force=" + force);
                this.mFsmInternal.resumeDispModeChange();
            }
        }
    }
}
