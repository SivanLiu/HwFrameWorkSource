package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityThread;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.WindowManager.LayoutParams;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;
import com.android.server.am.ActivityRecord;
import com.android.server.policy.WindowManagerPolicy.ScreenOffListener;
import com.android.server.policy.WindowManagerPolicy.StartingSurface;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.PrintWriter;

class TaskSnapshotController {
    static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    protected static final boolean IS_NOTCH_PROP = (SystemProperties.get("ro.config.hw_notch_size", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) ^ 1);
    @VisibleForTesting
    static final int SNAPSHOT_MODE_APP_THEME = 1;
    @VisibleForTesting
    static final int SNAPSHOT_MODE_NONE = 2;
    @VisibleForTesting
    static final int SNAPSHOT_MODE_REAL = 0;
    private static final String TAG = "WindowManager";
    private final TaskSnapshotCache mCache;
    private final Handler mHandler = new Handler();
    private final boolean mIsRunningOnIoT;
    private final boolean mIsRunningOnTv;
    private final boolean mIsRunningOnWear;
    private final TaskSnapshotLoader mLoader = new TaskSnapshotLoader(this.mPersister);
    private final TaskSnapshotPersister mPersister = new TaskSnapshotPersister(-$$Lambda$TaskSnapshotController$OPdXuZQLetMnocdH6XV32JbNQ3I.INSTANCE);
    private final WindowManagerService mService;
    private final ArraySet<Task> mSkipClosingAppSnapshotTasks = new ArraySet();
    private final Rect mTmpRect = new Rect();
    private final ArraySet<Task> mTmpTasks = new ArraySet();

    TaskSnapshotController(WindowManagerService service) {
        this.mService = service;
        this.mCache = new TaskSnapshotCache(this.mService, this.mLoader);
        this.mIsRunningOnTv = this.mService.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mIsRunningOnIoT = this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded");
        this.mIsRunningOnWear = this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
    }

    void systemReady() {
        this.mPersister.start();
    }

    void onTransitionStarting() {
        handleClosingApps(this.mService.mClosingApps);
    }

    void notifyAppVisibilityChanged(AppWindowToken appWindowToken, boolean visible) {
        if (!visible) {
            handleClosingApps(Sets.newArraySet(new AppWindowToken[]{appWindowToken}));
        }
    }

    private boolean checkWhiteListApp(ArraySet<AppWindowToken> closingApps) {
        for (int i = closingApps.size() - 1; i >= 0; i--) {
            ActivityRecord tmp = ActivityRecord.forToken(((AppWindowToken) closingApps.valueAt(i)).token);
            if (tmp != null && (tmp.realActivity.getPackageName().equals("com.sina.weibo") || tmp.realActivity.getPackageName().equals("com.taobao.taobao"))) {
                return true;
            }
        }
        return false;
    }

    private void handleClosingApps(ArraySet<AppWindowToken> closingApps) {
        if (shouldDisableSnapshots()) {
            if (checkWhiteListApp(closingApps)) {
                Slog.i(TAG, "We got the white list app");
            } else {
                return;
            }
        }
        getClosingTasks(closingApps, this.mTmpTasks);
        snapshotTasks(this.mTmpTasks);
        this.mSkipClosingAppSnapshotTasks.clear();
    }

    @VisibleForTesting
    void addSkipClosingAppSnapshotTasks(ArraySet<Task> tasks) {
        this.mSkipClosingAppSnapshotTasks.addAll(tasks);
    }

    void snapshotTasks(ArraySet<Task> tasks) {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskSnapshot snapshot;
            Task task = (Task) tasks.valueAt(i);
            switch (getSnapshotMode(task)) {
                case 0:
                    snapshot = snapshotTask(task);
                    break;
                case 1:
                    snapshot = drawAppThemeSnapshot(task);
                    break;
                case 2:
                    break;
                default:
                    snapshot = null;
                    break;
            }
            if (snapshot != null) {
                GraphicBuffer buffer = snapshot.getSnapshot();
                if (buffer.getWidth() == 0 || buffer.getHeight() == 0) {
                    buffer.destroy();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid task snapshot dimensions ");
                    stringBuilder.append(buffer.getWidth());
                    stringBuilder.append("x");
                    stringBuilder.append(buffer.getHeight());
                    Slog.e(str, stringBuilder.toString());
                } else {
                    this.mCache.putSnapshot(task, snapshot);
                    this.mPersister.persistSnapshot(task.mTaskId, task.mUserId, snapshot);
                    if (task.getController() != null) {
                        task.getController().reportSnapshotChanged(snapshot);
                    }
                }
            }
        }
    }

    TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk, boolean reducedResolution) {
        TaskSnapshotCache taskSnapshotCache = this.mCache;
        boolean z = reducedResolution || TaskSnapshotPersister.DISABLE_FULL_SIZED_BITMAPS;
        return taskSnapshotCache.getSnapshot(taskId, userId, restoreFromDisk, z);
    }

    StartingSurface createStartingSurface(AppWindowToken token, TaskSnapshot snapshot) {
        return TaskSnapshotSurface.create(this.mService, token, snapshot);
    }

    private TaskSnapshot snapshotTask(Task task) {
        WindowContainer windowContainer = task;
        AppWindowToken top = (AppWindowToken) task.getTopChild();
        if (top == null) {
            return null;
        }
        WindowState mainWindow = top.findMainWindow();
        if (mainWindow == null || !this.mService.mPolicy.isScreenOn() || task.getSurfaceControl() == null || top.hasCommittedReparentToAnimationLeash()) {
            return null;
        }
        boolean z = true;
        if (top.forAllWindows(-$$Lambda$TaskSnapshotController$1IXTXVXjIGs9ncGKW_v40ivZeoI.INSTANCE, true)) {
            boolean isLowRamDevice = ActivityManager.isLowRamDeviceStatic();
            float scaleFraction = isLowRamDevice ? TaskSnapshotPersister.REDUCED_SCALE : 1.0f;
            if (!isLowRamDevice && IS_EMUI_LITE) {
                Context context = ActivityThread.currentApplication();
                if (context != null) {
                    scaleFraction *= context.getResources().getFraction(34668545, 1, 1);
                }
            }
            float scaleFraction2 = scaleFraction;
            windowContainer.getBounds(this.mTmpRect);
            if (!HwPCUtils.isValidExtDisplayId(mainWindow.getDisplayId()) || this.mService.mHwWMSEx.getPCScreenDisplayMode() == 0) {
                this.mTmpRect.offsetTo(0, 0);
            } else {
                Point p = new Point(0, 0);
                this.mService.mHwWMSEx.updateDimPositionForPCMode(windowContainer, this.mTmpRect);
                this.mService.mHwWMSEx.updateSurfacePositionForPCMode(mainWindow, p);
                this.mTmpRect.offsetTo(p.x + mainWindow.mAttrs.surfaceInsets.left, p.y + mainWindow.mAttrs.surfaceInsets.top);
            }
            if (IS_NOTCH_PROP && this.mService.getDefaultDisplayContentLocked().getRotation() == 1 && !mainWindow.mFrame.isEmpty()) {
                this.mTmpRect.intersect(mainWindow.mFrame);
            }
            GraphicBuffer buffer = null;
            if (this.mService.getLazyMode() == 0) {
                buffer = SurfaceControl.captureLayers(task.getSurfaceControl().getHandle(), this.mTmpRect, scaleFraction2);
            } else if (mainWindow.getSurfaceControl() != null) {
                this.mTmpRect.scale(0.75f);
                buffer = SurfaceControl.captureLayers(mainWindow.getSurfaceControl().getHandle(), this.mTmpRect, scaleFraction2 / 0.75f);
            }
            GraphicBuffer buffer2 = buffer;
            boolean isWindowTranslucent = mainWindow.getAttrs().format != -1;
            float f;
            if (buffer2 == null || buffer2.getWidth() <= 1) {
                f = scaleFraction2;
            } else if (buffer2.getHeight() <= 1) {
                GraphicBuffer graphicBuffer = buffer2;
                f = scaleFraction2;
            } else {
                int i = top.getConfiguration().orientation;
                Rect insets = getInsets(mainWindow);
                int windowingMode = task.getWindowingMode();
                int systemUiVisibility = getSystemUiVisibility(task);
                if (top.fillsParent() && !isWindowTranslucent) {
                    z = false;
                }
                return new TaskSnapshot(buffer2, i, insets, isLowRamDevice, scaleFraction2, true, windowingMode, systemUiVisibility, z);
            }
            return null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to take screenshot. No visible windows for ");
        stringBuilder.append(windowContainer);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    static /* synthetic */ boolean lambda$snapshotTask$0(WindowState ws) {
        return (ws.mAppToken == null || ws.mAppToken.isSurfaceShowing()) && ws.mWinAnimator != null && ws.mWinAnimator.getShown() && ws.mWinAnimator.mLastAlpha > 0.0f;
    }

    private boolean shouldDisableSnapshots() {
        return this.mIsRunningOnWear || this.mIsRunningOnTv || this.mIsRunningOnIoT;
    }

    private Rect getInsets(WindowState state) {
        Rect insets = minRect(state.mContentInsets, state.mStableInsets);
        InsetUtils.addInsets(insets, state.mAppToken.getLetterboxInsets());
        return insets;
    }

    private Rect minRect(Rect rect1, Rect rect2) {
        return new Rect(Math.min(rect1.left, rect2.left), Math.min(rect1.top, rect2.top), Math.min(rect1.right, rect2.right), Math.min(rect1.bottom, rect2.bottom));
    }

    @VisibleForTesting
    void getClosingTasks(ArraySet<AppWindowToken> closingApps, ArraySet<Task> outClosingTasks) {
        outClosingTasks.clear();
        for (int i = closingApps.size() - 1; i >= 0; i--) {
            Task task = ((AppWindowToken) closingApps.valueAt(i)).getTask();
            if (!(task == null || task.isVisible() || this.mSkipClosingAppSnapshotTasks.contains(task))) {
                outClosingTasks.add(task);
            }
        }
    }

    @VisibleForTesting
    int getSnapshotMode(Task task) {
        AppWindowToken topChild = (AppWindowToken) task.getTopChild();
        if (!task.isActivityTypeStandardOrUndefined() && !task.isActivityTypeAssistant()) {
            return 2;
        }
        if (topChild == null || !topChild.shouldUseAppThemeSnapshot()) {
            return 0;
        }
        return 1;
    }

    private TaskSnapshot drawAppThemeSnapshot(Task task) {
        AppWindowToken topChild = (AppWindowToken) task.getTopChild();
        if (topChild == null) {
            return null;
        }
        WindowState mainWindow = topChild.findMainWindow();
        if (mainWindow == null) {
            return null;
        }
        int color = ColorUtils.setAlphaComponent(task.getTaskDescription().getBackgroundColor(), 255);
        int statusBarColor = task.getTaskDescription().getStatusBarColor();
        int navigationBarColor = task.getTaskDescription().getNavigationBarColor();
        LayoutParams attrs = mainWindow.getAttrs();
        SystemBarBackgroundPainter decorPainter = new SystemBarBackgroundPainter(attrs.flags, attrs.privateFlags, attrs.systemUiVisibility, statusBarColor, navigationBarColor);
        int width = mainWindow.getFrameLw().width();
        int height = mainWindow.getFrameLw().height();
        RenderNode node = RenderNode.create("TaskSnapshotController", null);
        node.setLeftTopRightBottom(0, 0, width, height);
        node.setClipToBounds(false);
        DisplayListCanvas c = node.start(width, height);
        c.drawColor(color);
        decorPainter.setInsets(mainWindow.mContentInsets, mainWindow.mStableInsets);
        decorPainter.drawDecors(c, null);
        node.end(c);
        Bitmap hwBitmap = ThreadedRenderer.createHardwareBitmap(node, width, height);
        if (hwBitmap == null) {
            return null;
        }
        return new TaskSnapshot(hwBitmap.createGraphicBufferHandle(), topChild.getConfiguration().orientation, mainWindow.mStableInsets, ActivityManager.isLowRamDeviceStatic(), 1.0f, false, task.getWindowingMode(), getSystemUiVisibility(task), false);
    }

    void onAppRemoved(AppWindowToken wtoken) {
        this.mCache.onAppRemoved(wtoken);
    }

    void onAppDied(AppWindowToken wtoken) {
        this.mCache.onAppDied(wtoken);
    }

    void notifyTaskRemovedFromRecents(int taskId, int userId) {
        this.mCache.onTaskRemoved(taskId);
        this.mPersister.onTaskRemovedFromRecents(taskId, userId);
    }

    void removeObsoleteTaskFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        this.mPersister.removeObsoleteFiles(persistentTaskIds, runningUserIds);
    }

    void setPersisterPaused(boolean paused) {
        this.mPersister.setPaused(paused);
    }

    void screenTurningOff(ScreenOffListener listener) {
        if (shouldDisableSnapshots()) {
            listener.onScreenOff();
        } else {
            this.mHandler.post(new -$$Lambda$TaskSnapshotController$q-BG2kMqHK9gvuY43J0TfS4aSVU(this, listener));
        }
    }

    public static /* synthetic */ void lambda$screenTurningOff$2(TaskSnapshotController taskSnapshotController, ScreenOffListener listener) {
        try {
            synchronized (taskSnapshotController.mService.mWindowMap) {
                WindowManagerService.boostPriorityForLockedSection();
                taskSnapshotController.mTmpTasks.clear();
                taskSnapshotController.mService.mRoot.forAllTasks(new -$$Lambda$TaskSnapshotController$ewi-Dm2ws6pdTXd1elso7FtoLKw(taskSnapshotController));
                taskSnapshotController.snapshotTasks(taskSnapshotController.mTmpTasks);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            listener.onScreenOff();
        } catch (Throwable th) {
            listener.onScreenOff();
        }
    }

    public static /* synthetic */ void lambda$screenTurningOff$1(TaskSnapshotController taskSnapshotController, Task task) {
        if (task.isVisible()) {
            taskSnapshotController.mTmpTasks.add(task);
        }
    }

    private int getSystemUiVisibility(Task task) {
        WindowState topFullscreenWindow;
        AppWindowToken topFullscreenToken = task.getTopFullscreenAppToken();
        if (topFullscreenToken != null) {
            topFullscreenWindow = topFullscreenToken.getTopFullscreenWindow();
        } else {
            topFullscreenWindow = null;
        }
        if (topFullscreenWindow != null) {
            return topFullscreenWindow.getSystemUiVisibility();
        }
        return 0;
    }

    void dump(PrintWriter pw, String prefix) {
        this.mCache.dump(pw, prefix);
    }

    public TaskSnapshot getForegroundTaskSnapshot() {
        return this.mCache.getLastForegroundSnapshot();
    }

    public void clearForegroundTaskSnapshot() {
        this.mCache.clearForegroundTaskSnapshot();
    }

    public TaskSnapshot createForegroundTaskSnapshot(AppWindowToken appWindowToken) {
        TaskSnapshot snapshot;
        Task task = appWindowToken.getTask();
        switch (getSnapshotMode(task)) {
            case 0:
                snapshot = snapshotTask(task);
                break;
            case 1:
                snapshot = drawAppThemeSnapshot(task);
                break;
            case 2:
                snapshot = null;
                break;
            default:
                snapshot = null;
                break;
        }
        if (snapshot != null) {
            GraphicBuffer buffer = snapshot.getSnapshot();
            if (buffer.getWidth() == 0 || buffer.getHeight() == 0) {
                buffer.destroy();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getForgroundTaskSnapshot Invalid task snapshot dimensions ");
                stringBuilder.append(buffer.getWidth());
                stringBuilder.append("x");
                stringBuilder.append(buffer.getHeight());
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            this.mCache.putForegroundSnapShot(task, snapshot);
        }
        return snapshot;
    }
}
