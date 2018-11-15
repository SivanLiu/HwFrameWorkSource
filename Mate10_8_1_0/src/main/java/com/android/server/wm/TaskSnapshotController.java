package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityManager.StackId;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityThread;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.ThreadedRenderer;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerPolicy.ScreenOffListener;
import android.view.WindowManagerPolicy.StartingSurface;
import com.android.server.am.ActivityRecord;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.google.android.collect.Sets;
import java.io.PrintWriter;

class TaskSnapshotController {
    static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    static final int SNAPSHOT_MODE_APP_THEME = 1;
    static final int SNAPSHOT_MODE_NONE = 2;
    static final int SNAPSHOT_MODE_REAL = 0;
    private static final String TAG = "WindowManager";
    private final TaskSnapshotCache mCache;
    private final Handler mHandler = new Handler();
    private final boolean mIsRunningOnIoT;
    private final boolean mIsRunningOnTv;
    private final boolean mIsRunningOnWear;
    private final TaskSnapshotLoader mLoader = new TaskSnapshotLoader(this.mPersister);
    private final TaskSnapshotPersister mPersister = new TaskSnapshotPersister(-$Lambda$v2Yn08uofw54W8n_7KsmBjqR0Z8.$INST$0);
    private final WindowManagerService mService;
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
    }

    private void snapshotTasks(ArraySet<Task> tasks) {
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
                    Slog.e(TAG, "Invalid task snapshot dimensions " + buffer.getWidth() + "x" + buffer.getHeight());
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
        boolean z;
        TaskSnapshotCache taskSnapshotCache = this.mCache;
        if (reducedResolution) {
            z = true;
        } else {
            z = TaskSnapshotPersister.DISABLE_FULL_SIZED_BITMAPS;
        }
        return taskSnapshotCache.getSnapshot(taskId, userId, restoreFromDisk, z);
    }

    StartingSurface createStartingSurface(AppWindowToken token, TaskSnapshot snapshot) {
        return TaskSnapshotSurface.create(this.mService, token, snapshot);
    }

    private TaskSnapshot snapshotTask(Task task) {
        AppWindowToken top = (AppWindowToken) task.getTopChild();
        if (top == null) {
            return null;
        }
        WindowState mainWindow = top.findMainWindow();
        if (mainWindow == null) {
            return null;
        }
        boolean isLowRamDevice = ActivityManager.isLowRamDeviceStatic();
        float scaleFraction = isLowRamDevice ? TaskSnapshotPersister.REDUCED_SCALE : 1.0f;
        if (!isLowRamDevice && IS_EMUI_LITE) {
            Context context = ActivityThread.currentApplication();
            if (context != null) {
                scaleFraction *= context.getResources().getFraction(34668545, 1, 1);
            }
        }
        GraphicBuffer buffer = top.mDisplayContent.screenshotApplicationsToBuffer(top.token, -1, -1, false, scaleFraction, false, true);
        if (buffer == null || buffer.getWidth() <= 1 || buffer.getHeight() <= 1) {
            return null;
        }
        return new TaskSnapshot(buffer, top.getConfiguration().orientation, minRect(mainWindow.mContentInsets, mainWindow.mStableInsets), isLowRamDevice, scaleFraction);
    }

    private boolean shouldDisableSnapshots() {
        return (!ActivityManager.ENABLE_TASK_SNAPSHOTS || this.mIsRunningOnWear || this.mIsRunningOnTv || this.mIsRunningOnIoT) ? true : this.mService.getGestureState();
    }

    private Rect minRect(Rect rect1, Rect rect2) {
        return new Rect(Math.min(rect1.left, rect2.left), Math.min(rect1.top, rect2.top), Math.min(rect1.right, rect2.right), Math.min(rect1.bottom, rect2.bottom));
    }

    void getClosingTasks(ArraySet<AppWindowToken> closingApps, ArraySet<Task> outClosingTasks) {
        outClosingTasks.clear();
        for (int i = closingApps.size() - 1; i >= 0; i--) {
            Task task = ((AppWindowToken) closingApps.valueAt(i)).getTask();
            if (!(task == null || (task.isVisible() ^ 1) == 0)) {
                outClosingTasks.add(task);
            }
        }
    }

    int getSnapshotMode(Task task) {
        AppWindowToken topChild = (AppWindowToken) task.getTopChild();
        if (StackId.isHomeOrRecentsStack(task.mStack.mStackId)) {
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
        int color = task.getTaskDescription().getBackgroundColor();
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
        return new TaskSnapshot(hwBitmap.createGraphicBufferHandle(), topChild.getConfiguration().orientation, mainWindow.mStableInsets, ActivityManager.isLowRamDeviceStatic(), 1.0f);
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
            this.mHandler.post(new -$Lambda$jlKbn4GPn9-0nFmS_2KB8vTwgFI((byte) 1, this, listener));
        }
    }

    /* synthetic */ void lambda$-com_android_server_wm_TaskSnapshotController_15354(ScreenOffListener listener) {
        try {
            synchronized (this.mService.mWindowMap) {
                WindowManagerService.boostPriorityForLockedSection();
                this.mTmpTasks.clear();
                this.mService.mRoot.forAllTasks(new -$Lambda$YIZfR4m-B8z_tYbP2x4OJ3o7OYE(UsbDescriptor.CLASSID_BILLBOARD, this));
                snapshotTasks(this.mTmpTasks);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            listener.onScreenOff();
        } catch (Throwable th) {
            listener.onScreenOff();
        }
    }

    /* synthetic */ void lambda$-com_android_server_wm_TaskSnapshotController_15519(Task task) {
        if (task.isVisible()) {
            this.mTmpTasks.add(task);
        }
    }

    void dump(PrintWriter pw, String prefix) {
        this.mCache.dump(pw, prefix);
    }
}
