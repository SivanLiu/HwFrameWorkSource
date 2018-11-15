package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.app.WindowConfiguration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.IRecentsAnimationController;
import android.view.IRecentsAnimationController.Stub;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.ArrayList;

public class RecentsAnimationController implements DeathRecipient {
    private static final long FAILSAFE_DELAY = 1000;
    public static final int REORDER_KEEP_IN_PLACE = 0;
    public static final int REORDER_MOVE_TO_ORIGINAL_POSITION = 2;
    public static final int REORDER_MOVE_TO_TOP = 1;
    private static final String TAG = RecentsAnimationController.class.getSimpleName();
    private SurfaceAnimator mAnim;
    private final RecentsAnimationCallbacks mCallbacks;
    private boolean mCanceled;
    private final IRecentsAnimationController mController = new Stub() {
        public TaskSnapshot screenshotTask(int taskId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        Binder.restoreCallingIdentity(token);
                        return null;
                    }
                    for (int i = RecentsAnimationController.this.mPendingAnimations.size() - 1; i >= 0; i--) {
                        if (((TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(i)).mTask.mTaskId == taskId) {
                            TaskSnapshotController snapshotController = RecentsAnimationController.this.mService.mTaskSnapshotController;
                            ArraySet<Task> tasks = Sets.newArraySet(new Task[]{task});
                            snapshotController.snapshotTasks(tasks);
                            snapshotController.addSkipClosingAppSnapshotTasks(tasks);
                            TaskSnapshot snapshot = snapshotController.getSnapshot(taskId, 0, false, false);
                            Binder.restoreCallingIdentity(token);
                            return snapshot;
                        }
                    }
                    Binder.restoreCallingIdentity(token);
                    return null;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX WARNING: Missing block: B:13:?, code:
            r2 = com.android.server.wm.RecentsAnimationController.access$500(r5.this$0);
     */
        /* JADX WARNING: Missing block: B:14:0x004a, code:
            if (r6 == false) goto L_0x004f;
     */
        /* JADX WARNING: Missing block: B:15:0x004c, code:
            r4 = 1;
     */
        /* JADX WARNING: Missing block: B:16:0x004f, code:
            r4 = 2;
     */
        /* JADX WARNING: Missing block: B:17:0x0050, code:
            r2.onAnimationFinished(r4, true);
     */
        /* JADX WARNING: Missing block: B:18:0x0053, code:
            android.os.Binder.restoreCallingIdentity(r0);
     */
        /* JADX WARNING: Missing block: B:19:0x0057, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void finish(boolean moveHomeToTop) {
            String access$400 = RecentsAnimationController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("finish(");
            stringBuilder.append(moveHomeToTop);
            stringBuilder.append("): mCanceled=");
            stringBuilder.append(RecentsAnimationController.this.mCanceled);
            Slog.d(access$400, stringBuilder.toString());
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        Binder.restoreCallingIdentity(token);
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setAnimationTargetsBehindSystemBars(boolean behindSystemBars) throws RemoteException {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    for (int i = RecentsAnimationController.this.mPendingAnimations.size() - 1; i >= 0; i--) {
                        ((TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(i)).mTask.setCanAffectSystemUiFlags(behindSystemBars);
                    }
                    RecentsAnimationController.this.mService.mWindowPlacerLocked.requestTraversal();
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setInputConsumerEnabled(boolean enabled) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        Binder.restoreCallingIdentity(token);
                        return;
                    }
                    RecentsAnimationController.this.mInputConsumerEnabled = enabled;
                    RecentsAnimationController.this.mService.mInputMonitor.updateInputWindowsLw(true);
                    RecentsAnimationController.this.mService.scheduleAnimationLocked();
                    Binder.restoreCallingIdentity(token);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setSplitScreenMinimized(boolean minimized) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        Binder.restoreCallingIdentity(token);
                        return;
                    }
                    RecentsAnimationController.this.mSplitScreenMinimized = minimized;
                    RecentsAnimationController.this.mService.checkSplitScreenMinimizedChanged(true);
                    Binder.restoreCallingIdentity(token);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void hideCurrentInputMethod() {
            long token = Binder.clearCallingIdentity();
            try {
                InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
                if (inputMethodManagerInternal != null) {
                    inputMethodManagerInternal.hideCurrentInputMethod();
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }
    };
    private final int mDisplayId;
    private final Runnable mFailsafeRunnable = new -$$Lambda$RecentsAnimationController$4jQqaDgSmtGCjbUJiVoDh_jr9rY(this);
    private boolean mInputConsumerEnabled;
    private boolean mLinkedToDeathOfRunner;
    private Rect mMinimizedHomeBounds = new Rect();
    private final ArrayList<TaskAnimationAdapter> mPendingAnimations = new ArrayList();
    private SurfaceControl mPendingLeash;
    private boolean mPendingStart = true;
    private IRecentsAnimationRunner mRunner;
    private final WindowManagerService mService;
    private boolean mSplitScreenMinimized;
    private AppWindowToken mTargetAppToken;
    private final Rect mTmpRect = new Rect();

    public interface RecentsAnimationCallbacks {
        void onAnimationFinished(@ReorderMode int i, boolean z);
    }

    public @interface ReorderMode {
    }

    @VisibleForTesting
    class TaskAnimationAdapter implements AnimationAdapter {
        private final Rect mBounds = new Rect();
        private OnAnimationFinishedCallback mCapturedFinishCallback;
        private SurfaceControl mCapturedLeash;
        private final boolean mIsRecentTaskInvisible;
        private final Point mPosition = new Point();
        private RemoteAnimationTarget mTarget;
        private final Task mTask;

        TaskAnimationAdapter(Task task, boolean isRecentTaskInvisible) {
            this.mTask = task;
            this.mIsRecentTaskInvisible = isRecentTaskInvisible;
            WindowContainer container = this.mTask.getParent();
            container.getRelativePosition(this.mPosition);
            container.getBounds(this.mBounds);
        }

        RemoteAnimationTarget createRemoteAnimationApp() {
            WindowState mainWindow;
            AppWindowToken topApp = this.mTask.getTopVisibleAppToken();
            if (topApp != null) {
                mainWindow = topApp.findMainWindow();
            } else {
                mainWindow = null;
            }
            if (mainWindow == null) {
                return null;
            }
            Rect insets = new Rect(mainWindow.mContentInsets);
            InsetUtils.addInsets(insets, mainWindow.mAppToken.getLetterboxInsets());
            this.mTarget = new RemoteAnimationTarget(this.mTask.mTaskId, 1, this.mCapturedLeash, topApp.fillsParent() ^ 1, mainWindow.mWinAnimator.mLastClipRect, insets, this.mTask.getPrefixOrderIndex(), this.mPosition, this.mBounds, this.mTask.getWindowConfiguration(), this.mIsRecentTaskInvisible);
            return this.mTarget;
        }

        public boolean getDetachWallpaper() {
            return false;
        }

        public boolean getShowWallpaper() {
            return false;
        }

        public int getBackgroundColor() {
            return 0;
        }

        public void startAnimation(SurfaceControl animationLeash, Transaction t, OnAnimationFinishedCallback finishCallback) {
            t.setLayer(animationLeash, this.mTask.getPrefixOrderIndex());
            t.setPosition(animationLeash, (float) this.mPosition.x, (float) this.mPosition.y);
            RecentsAnimationController.this.mTmpRect.set(this.mBounds);
            RecentsAnimationController.this.mTmpRect.offsetTo(0, 0);
            t.setWindowCrop(animationLeash, RecentsAnimationController.this.mTmpRect);
            this.mCapturedLeash = animationLeash;
            this.mCapturedFinishCallback = finishCallback;
        }

        public void onAnimationCancelled(SurfaceControl animationLeash) {
            RecentsAnimationController.this.cancelAnimation(2, "taskAnimationAdapterCanceled");
        }

        public long getDurationHint() {
            return 0;
        }

        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis();
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("task=");
            stringBuilder.append(this.mTask);
            pw.println(stringBuilder.toString());
            if (this.mTarget != null) {
                pw.print(prefix);
                pw.println("Target:");
                RemoteAnimationTarget remoteAnimationTarget = this.mTarget;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("  ");
                remoteAnimationTarget.dump(pw, stringBuilder2.toString());
            } else {
                pw.print(prefix);
                pw.println("Target: null");
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("mIsRecentTaskInvisible=");
            stringBuilder.append(this.mIsRecentTaskInvisible);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mPosition=");
            stringBuilder.append(this.mPosition);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mBounds=");
            stringBuilder.append(this.mBounds);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mIsRecentTaskInvisible=");
            stringBuilder.append(this.mIsRecentTaskInvisible);
            pw.println(stringBuilder.toString());
        }

        public void writeToProto(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            if (this.mTarget != null) {
                this.mTarget.writeToProto(proto, 1146756268033L);
            }
            proto.end(token);
        }
    }

    RecentsAnimationController(WindowManagerService service, IRecentsAnimationRunner remoteAnimationRunner, RecentsAnimationCallbacks callbacks, int displayId) {
        this.mService = service;
        this.mRunner = remoteAnimationRunner;
        this.mCallbacks = callbacks;
        this.mDisplayId = displayId;
    }

    public void initialize(int targetActivityType, SparseBooleanArray recentTaskIds) {
        DisplayContent dc = this.mService.mRoot.getDisplayContent(this.mDisplayId);
        ArrayList<Task> visibleTasks = dc.getVisibleTasks();
        int taskCount = visibleTasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = (Task) visibleTasks.get(i);
            WindowConfiguration config = task.getWindowConfiguration();
            if (!(config.tasksAreFloating() || config.getWindowingMode() == 3 || config.getActivityType() == targetActivityType)) {
                addAnimation(task, recentTaskIds.get(task.mTaskId) ^ 1);
            }
        }
        if (this.mPendingAnimations.isEmpty()) {
            cancelAnimation(2, "initialize-noVisibleTasks");
            return;
        }
        try {
            linkToDeathOfRunner();
            AppWindowToken recentsComponentAppToken = ((Task) dc.getStack(0, targetActivityType).getTopChild()).getTopFullscreenAppToken();
            if (recentsComponentAppToken != null) {
                this.mTargetAppToken = recentsComponentAppToken;
                if (recentsComponentAppToken.windowsCanBeWallpaperTarget()) {
                    dc.pendingLayoutChanges |= 4;
                    dc.setLayoutNeeded();
                }
            }
            dc.getDockedDividerController().getHomeStackBoundsInDockedMode(this.mMinimizedHomeBounds);
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        } catch (RemoteException e) {
            cancelAnimation(2, "initialize-failedToLinkToDeath");
        }
    }

    @VisibleForTesting
    AnimationAdapter addAnimation(Task task, boolean isRecentTaskInvisible) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addAnimation(");
        stringBuilder.append(task.getName());
        stringBuilder.append(")");
        Slog.d(str, stringBuilder.toString());
        this.mAnim = new SurfaceAnimator(task, null, this.mService);
        TaskAnimationAdapter taskAdapter = new TaskAnimationAdapter(task, isRecentTaskInvisible);
        this.mAnim.startAnimation(task.getPendingTransaction(), taskAdapter, false);
        this.mPendingLeash = this.mAnim.mLeash;
        task.commitPendingTransaction();
        this.mPendingAnimations.add(taskAdapter);
        return taskAdapter;
    }

    @VisibleForTesting
    void removeAnimation(TaskAnimationAdapter taskAdapter) {
        taskAdapter.mTask.setCanAffectSystemUiFlags(true);
        taskAdapter.mCapturedFinishCallback.onAnimationFinished(taskAdapter);
        this.mPendingAnimations.remove(taskAdapter);
    }

    void startAnimation() {
        if (this.mPendingStart && !this.mCanceled) {
            try {
                ArrayList<RemoteAnimationTarget> appAnimations = new ArrayList();
                for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
                    TaskAnimationAdapter taskAdapter = (TaskAnimationAdapter) this.mPendingAnimations.get(i);
                    RemoteAnimationTarget target = taskAdapter.createRemoteAnimationApp();
                    if (target != null) {
                        appAnimations.add(target);
                    } else {
                        removeAnimation(taskAdapter);
                    }
                }
                if (appAnimations.isEmpty()) {
                    cancelAnimation(2, "startAnimation-noAppWindows");
                    return;
                }
                Rect minimizedHomeBounds;
                RemoteAnimationTarget[] appTargets = (RemoteAnimationTarget[]) appAnimations.toArray(new RemoteAnimationTarget[appAnimations.size()]);
                this.mPendingStart = false;
                Rect contentInsets = null;
                if (this.mTargetAppToken == null || !this.mTargetAppToken.inSplitScreenSecondaryWindowingMode()) {
                    minimizedHomeBounds = null;
                } else {
                    minimizedHomeBounds = this.mMinimizedHomeBounds;
                }
                if (!(this.mTargetAppToken == null || this.mTargetAppToken.findMainWindow() == null)) {
                    contentInsets = this.mTargetAppToken.findMainWindow().mContentInsets;
                }
                this.mRunner.onAnimationStart(this.mController, appTargets, contentInsets, minimizedHomeBounds);
                SparseIntArray reasons = new SparseIntArray();
                reasons.put(1, 5);
                this.mService.mH.obtainMessage(47, reasons).sendToTarget();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to start recents animation", e);
            }
        }
    }

    void cancelAnimation(@ReorderMode int reorderMode, String reason) {
        cancelAnimation(reorderMode, false, reason);
    }

    void cancelAnimationSynchronously(@ReorderMode int reorderMode, String reason) {
        cancelAnimation(reorderMode, true, reason);
    }

    private void cancelAnimation(@ReorderMode int reorderMode, boolean runSynchronously, String reason) {
        synchronized (this.mService.getWindowManagerLock()) {
            if (this.mCanceled) {
                return;
            }
            this.mService.mH.removeCallbacks(this.mFailsafeRunnable);
            this.mCanceled = true;
            try {
                this.mRunner.onAnimationCanceled();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to cancel recents animation", e);
            }
        }
        this.mCallbacks.onAnimationFinished(reorderMode, runSynchronously);
    }

    void cleanupAnimation(@ReorderMode int reorderMode) {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            TaskAnimationAdapter taskAdapter = (TaskAnimationAdapter) this.mPendingAnimations.get(i);
            if (reorderMode == 1 || reorderMode == 0) {
                if (taskAdapter.mTask.getDimmer().mDimState != null) {
                    taskAdapter.mTask.getPendingTransaction().hide(taskAdapter.mTask.getDimmer().mDimState.mDimLayer);
                }
                taskAdapter.mTask.dontAnimateDimExit();
            }
            removeAnimation(taskAdapter);
        }
        this.mService.mH.removeCallbacks(this.mFailsafeRunnable);
        unlinkToDeathOfRunner();
        this.mRunner = null;
        this.mCanceled = true;
        this.mService.mInputMonitor.updateInputWindowsLw(true);
        this.mService.destroyInputConsumer("recents_animation_input_consumer");
        if (this.mTargetAppToken == null) {
            return;
        }
        if (reorderMode == 1 || reorderMode == 0) {
            this.mService.mAppTransition.notifyAppTransitionFinishedLocked(this.mTargetAppToken.token);
        }
    }

    void scheduleFailsafe() {
        this.mService.mH.postDelayed(this.mFailsafeRunnable, 1000);
    }

    private void linkToDeathOfRunner() throws RemoteException {
        if (!this.mLinkedToDeathOfRunner) {
            this.mRunner.asBinder().linkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = true;
        }
    }

    private void unlinkToDeathOfRunner() {
        if (this.mLinkedToDeathOfRunner) {
            this.mRunner.asBinder().unlinkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = false;
        }
    }

    public void binderDied() {
        cancelAnimation(2, "binderDied");
    }

    void checkAnimationReady(WallpaperController wallpaperController) {
        if (this.mPendingStart) {
            boolean wallpaperReady = !isTargetOverWallpaper() || (wallpaperController.getWallpaperTarget() != null && wallpaperController.wallpaperTransitionReady());
            if (wallpaperReady) {
                this.mService.getRecentsAnimationController().startAnimation();
            }
        }
    }

    boolean isSplitScreenMinimized() {
        return this.mSplitScreenMinimized;
    }

    boolean isWallpaperVisible(WindowState w) {
        return w != null && w.mAppToken != null && this.mTargetAppToken == w.mAppToken && isTargetOverWallpaper();
    }

    boolean hasInputConsumerForApp(AppWindowToken appToken) {
        return this.mInputConsumerEnabled && isAnimatingApp(appToken);
    }

    boolean updateInputConsumerForApp(InputConsumerImpl recentsAnimationInputConsumer, boolean hasFocus) {
        WindowState targetAppMainWindow;
        if (this.mTargetAppToken != null) {
            targetAppMainWindow = this.mTargetAppToken.findMainWindow();
        } else {
            targetAppMainWindow = null;
        }
        if (targetAppMainWindow == null) {
            return false;
        }
        targetAppMainWindow.getBounds(this.mTmpRect);
        recentsAnimationInputConsumer.mWindowHandle.hasFocus = hasFocus;
        recentsAnimationInputConsumer.mWindowHandle.touchableRegion.set(this.mTmpRect);
        return true;
    }

    boolean isTargetApp(AppWindowToken token) {
        return this.mTargetAppToken != null && token == this.mTargetAppToken;
    }

    private boolean isTargetOverWallpaper() {
        if (this.mTargetAppToken == null) {
            return false;
        }
        return this.mTargetAppToken.windowsCanBeWallpaperTarget();
    }

    boolean isAnimatingTask(Task task) {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            if (task == ((TaskAnimationAdapter) this.mPendingAnimations.get(i)).mTask) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnimatingApp(AppWindowToken appToken) {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            Task task = ((TaskAnimationAdapter) this.mPendingAnimations.get(i)).mTask;
            for (int j = task.getChildCount() - 1; j >= 0; j--) {
                if (((AppWindowToken) task.getChildAt(j)) == appToken) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = new StringBuilder();
        innerPrefix.append(prefix);
        innerPrefix.append("  ");
        innerPrefix = innerPrefix.toString();
        pw.print(prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RecentsAnimationController.class.getSimpleName());
        stringBuilder.append(":");
        pw.println(stringBuilder.toString());
        pw.print(innerPrefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mPendingStart=");
        stringBuilder.append(this.mPendingStart);
        pw.println(stringBuilder.toString());
        pw.print(innerPrefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCanceled=");
        stringBuilder.append(this.mCanceled);
        pw.println(stringBuilder.toString());
        pw.print(innerPrefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mInputConsumerEnabled=");
        stringBuilder.append(this.mInputConsumerEnabled);
        pw.println(stringBuilder.toString());
        pw.print(innerPrefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSplitScreenMinimized=");
        stringBuilder.append(this.mSplitScreenMinimized);
        pw.println(stringBuilder.toString());
        pw.print(innerPrefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mTargetAppToken=");
        stringBuilder.append(this.mTargetAppToken);
        pw.println(stringBuilder.toString());
        pw.print(innerPrefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("isTargetOverWallpaper=");
        stringBuilder.append(isTargetOverWallpaper());
        pw.println(stringBuilder.toString());
    }
}
