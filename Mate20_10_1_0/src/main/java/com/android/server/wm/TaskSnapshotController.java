package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.hardware.display.HwFoldScreenState;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.TaskSnapshotSurface;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.function.Consumer;

class TaskSnapshotController {
    static final int CONFIG_SCALE = SystemProperties.getInt("ro.config.hw_snapshot_scale", -1);
    private static final boolean HW_SNAPSHOT = SystemProperties.getBoolean("ro.huawei.only_hwsnapshot", true);
    protected static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    @VisibleForTesting
    static final int SNAPSHOT_MODE_APP_THEME = 1;
    @VisibleForTesting
    static final int SNAPSHOT_MODE_NONE = 2;
    @VisibleForTesting
    static final int SNAPSHOT_MODE_REAL = 0;
    private static final String TAG = "WindowManager";
    static final boolean USE_CONFIG_SCALE;
    private final TaskSnapshotCache mCache;
    private final float mFullSnapshotScale;
    private final Handler mHandler = new Handler();
    private final boolean mIsRunningOnIoT;
    private final boolean mIsRunningOnTv;
    private final boolean mIsRunningOnWear;
    private final TaskSnapshotLoader mLoader;
    private final TaskSnapshotPersister mPersister;
    private final WindowManagerService mService;
    private final ArraySet<Task> mSkipClosingAppSnapshotTasks = new ArraySet<>();
    private final Rect mTmpRect = new Rect();
    private final ArraySet<Task> mTmpTasks = new ArraySet<>();

    static {
        int i = CONFIG_SCALE;
        USE_CONFIG_SCALE = i >= 50 && i < 100;
    }

    TaskSnapshotController(WindowManagerService service) {
        this.mService = service;
        this.mPersister = new TaskSnapshotPersister(this.mService, $$Lambda$OPdXuZQLetMnocdH6XV32JbNQ3I.INSTANCE);
        this.mLoader = new TaskSnapshotLoader(this.mPersister);
        this.mCache = new TaskSnapshotCache(this.mService, this.mLoader);
        this.mIsRunningOnTv = this.mService.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mIsRunningOnIoT = this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded");
        this.mIsRunningOnWear = this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        this.mFullSnapshotScale = this.mService.mContext.getResources().getFloat(17105058);
    }

    /* access modifiers changed from: package-private */
    public void systemReady() {
        this.mPersister.start();
    }

    /* access modifiers changed from: package-private */
    public void onTransitionStarting(DisplayContent displayContent) {
        handleClosingApps(displayContent.mClosingApps);
    }

    /* access modifiers changed from: package-private */
    public void notifyAppVisibilityChanged(AppWindowToken appWindowToken, boolean visible) {
        if (!visible) {
            handleClosingApps(Sets.newArraySet(new AppWindowToken[]{appWindowToken}));
        }
    }

    private void handleClosingApps(ArraySet<AppWindowToken> closingApps) {
        if (!shouldDisableSnapshots()) {
            getClosingTasks(closingApps, this.mTmpTasks);
            snapshotTasks(this.mTmpTasks);
            this.mSkipClosingAppSnapshotTasks.clear();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void addSkipClosingAppSnapshotTasks(ArraySet<Task> tasks) {
        this.mSkipClosingAppSnapshotTasks.addAll((ArraySet<? extends Task>) tasks);
    }

    /* access modifiers changed from: package-private */
    public void snapshotTasks(ArraySet<Task> tasks) {
        ActivityManager.TaskSnapshot snapshot;
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task task = tasks.valueAt(i);
            int mode = getSnapshotMode(task);
            if (mode == 0) {
                snapshot = snapshotTask(task);
            } else if (mode == 1) {
                snapshot = drawAppThemeSnapshot(task);
            } else if (mode != 2) {
                snapshot = null;
            }
            if (snapshot != null) {
                GraphicBuffer buffer = snapshot.getSnapshot();
                if (buffer.getWidth() == 0 || buffer.getHeight() == 0) {
                    buffer.destroy();
                    Slog.e(TAG, "Invalid task snapshot dimensions " + buffer.getWidth() + "x" + buffer.getHeight());
                } else {
                    this.mCache.putSnapshot(task, snapshot);
                    if (HwDisplaySizeUtil.hasSideInScreen()) {
                        this.mPersister.persistSnapshot(task.mTaskId, task.mUserId, snapshot, ColorUtils.setAlphaComponent(task.getTaskDescription().getBackgroundColor(), 255));
                    } else {
                        this.mPersister.persistSnapshot(task.mTaskId, task.mUserId, snapshot);
                    }
                    task.onSnapshotChanged(snapshot);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public ActivityManager.TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk, boolean reducedResolution) {
        return this.mCache.getSnapshot(taskId, userId, restoreFromDisk, reducedResolution || TaskSnapshotPersister.DISABLE_FULL_SIZED_BITMAPS || USE_CONFIG_SCALE);
    }

    /* access modifiers changed from: package-private */
    public WindowManagerPolicy.StartingSurface createStartingSurface(AppWindowToken token, ActivityManager.TaskSnapshot snapshot) {
        return TaskSnapshotSurface.create(this.mService, token, snapshot);
    }

    private AppWindowToken findAppTokenForSnapshot(Task task) {
        for (int i = task.getChildCount() - 1; i >= 0; i--) {
            AppWindowToken appWindowToken = (AppWindowToken) task.getChildAt(i);
            if (appWindowToken != null && appWindowToken.isSurfaceShowing() && appWindowToken.findMainWindow() != null && appWindowToken.forAllWindows((ToBooleanFunction<WindowState>) $$Lambda$TaskSnapshotController$b7mc92hqzbRpmpc99dYS4wKuL6Y.INSTANCE, true)) {
                return appWindowToken;
            }
        }
        return null;
    }

    static /* synthetic */ boolean lambda$findAppTokenForSnapshot$0(WindowState ws) {
        return ws.mWinAnimator != null && ws.mWinAnimator.getShown() && ws.mWinAnimator.mLastAlpha > 0.0f;
    }

    /* access modifiers changed from: package-private */
    public SurfaceControl.ScreenshotGraphicBuffer createTaskSnapshot(Task task, float scaleFraction) {
        SurfaceControl.ScreenshotGraphicBuffer screenshotBuffer;
        AppWindowToken appWindowToken = findAppTokenForSnapshot(task);
        if (appWindowToken == null) {
            Slog.w(TAG, "Failed to take screenshot. No visible windows for " + task);
            return null;
        }
        WindowState mainWindow = appWindowToken.findMainWindow();
        if (mainWindow == null) {
            Slog.w(TAG, "Failed to take screenshot. No main window for " + task);
            return null;
        } else if (task.getSurfaceControl() == null) {
            return null;
        } else {
            task.getBounds(this.mTmpRect);
            if (!appWindowToken.inHwMultiStackWindowingMode() && appWindowToken.getConfiguration().orientation == 2 && this.mTmpRect.width() < this.mTmpRect.height()) {
                Rect rect = this.mTmpRect;
                rect.set(rect.top, this.mTmpRect.left, this.mTmpRect.bottom, this.mTmpRect.right);
                Slog.i(TAG, "Screenshot bounds is updated to: " + this.mTmpRect);
            }
            boolean isWindowDismatchTask = task.inHwFreeFormWindowingMode() && !this.mTmpRect.equals(mainWindow.mWindowFrames.mDisplayFrame) && mainWindow.getSurfaceControl() != null;
            if (isWindowDismatchTask) {
                this.mTmpRect.set(mainWindow.mWindowFrames.mDisplayFrame);
            }
            this.mTmpRect.offsetTo(0, 0);
            if (task.getDisplayContent() != null && task.getDisplayContent().getDisplayId() == 0 && !task.inHwMultiStackWindowingMode()) {
                int rotation = this.mService.getDefaultDisplayContentLocked().getRotation();
                if (IS_NOTCH_PROP && rotation == 1 && !mainWindow.mWindowFrames.mFrame.isEmpty() && this.mService.mPolicy.isNotchDisplayDisabled() && !this.mService.mAtmService.mInFreeformSnapshot) {
                    this.mTmpRect.intersect(mainWindow.mWindowFrames.mDisplayFrame);
                }
            }
            SurfaceControl.ScreenshotGraphicBuffer screenshotBuffer2 = null;
            GraphicBuffer buffer = null;
            if (this.mService.getLazyMode() == 0) {
                if (task.inFreeformWindowingMode()) {
                    DisplayInfo displayInfo = appWindowToken.mDisplayContent.getDisplayInfo();
                    int dw = displayInfo.logicalWidth;
                    int dh = displayInfo.logicalHeight;
                    this.mTmpRect.set(new Rect(0, 0, dw, dh));
                    int rot = this.mService.getDefaultDisplayContentLocked().getRotation();
                    int i = 3;
                    if (rot == 1 || rot == 3) {
                        if (rot != 1) {
                            i = 1;
                        }
                        rot = i;
                    }
                    this.mTmpRect.intersect(mainWindow.mWindowFrames.mFrame);
                    DisplayContent displayContent = appWindowToken.mDisplayContent;
                    DisplayContent.convertCropForSurfaceFlinger(this.mTmpRect, rot, dw, dh);
                    ScreenRotationAnimation screenRotationAnimation = this.mService.mAnimator.getScreenRotationAnimationLocked(0);
                    boolean inRotation = screenRotationAnimation != null && screenRotationAnimation.isAnimating();
                    if (SurfaceControl.getInternalDisplayToken() != null) {
                        screenshotBuffer2 = SurfaceControl.screenshotToBuffer(SurfaceControl.getInternalDisplayToken(), this.mTmpRect, dw, dh, inRotation, rot);
                        buffer = screenshotBuffer2 != null ? screenshotBuffer2.getGraphicBuffer() : null;
                    }
                } else {
                    if (isWindowDismatchTask) {
                        this.mTmpRect.offsetTo(mainWindow.mAttrs.surfaceInsets.left, mainWindow.mAttrs.surfaceInsets.top);
                        screenshotBuffer = SurfaceControl.captureLayers(mainWindow.getSurfaceControl().getHandle(), this.mTmpRect, scaleFraction);
                    } else {
                        screenshotBuffer = SurfaceControl.captureLayers(task.getSurfaceControl().getHandle(), this.mTmpRect, scaleFraction);
                    }
                    if (!HwFoldScreenState.isFoldScreenDevice() || !this.mService.isInSubFoldScaleMode() || HwPCUtils.isValidExtDisplayId(mainWindow.getDisplayId()) || mainWindow.getSurfaceControl() == null) {
                        screenshotBuffer2 = screenshotBuffer;
                    } else {
                        if (task.inHwFreeFormWindowingMode()) {
                            this.mTmpRect.offsetTo(mainWindow.mAttrs.surfaceInsets.left, mainWindow.mAttrs.surfaceInsets.top);
                        }
                        this.mTmpRect.scale(this.mService.mSubFoldModeScale);
                        screenshotBuffer2 = SurfaceControl.captureLayers(mainWindow.getSurfaceControl().getHandle(), this.mTmpRect, scaleFraction / this.mService.mSubFoldModeScale);
                    }
                    buffer = screenshotBuffer2 != null ? screenshotBuffer2.getGraphicBuffer() : null;
                }
            } else if (mainWindow.getSurfaceControl() != null) {
                if (task.inHwFreeFormWindowingMode()) {
                    this.mTmpRect.offsetTo(mainWindow.mAttrs.surfaceInsets.left, mainWindow.mAttrs.surfaceInsets.top);
                }
                this.mTmpRect.scale(0.75f);
                screenshotBuffer2 = SurfaceControl.captureLayers(mainWindow.getSurfaceControl().getHandle(), this.mTmpRect, scaleFraction / 0.75f);
                buffer = screenshotBuffer2 != null ? screenshotBuffer2.getGraphicBuffer() : null;
            }
            if (buffer != null && buffer.getWidth() > 1 && buffer.getHeight() > 1) {
                return screenshotBuffer2;
            }
            Slog.w(TAG, "Failed to take screenshot for " + task);
            return null;
        }
    }

    private ActivityManager.TaskSnapshot snapshotTask(Task task) {
        return snapshotTask(task, false);
    }

    private ActivityManager.TaskSnapshot snapshotTask(Task task, boolean animationLeashDelay) {
        float scaleFraction;
        if (!this.mService.mPolicy.isScreenOn()) {
            Slog.i(TAG, "Attempted to take screenshot while display was off.");
            return null;
        }
        AppWindowToken appWindowToken = findAppTokenForSnapshot(task);
        if (appWindowToken == null) {
            Slog.w(TAG, "Failed to take screenshot. No visible windows for " + task);
            return null;
        } else if (appWindowToken.mHadTakenSnapShot && HW_SNAPSHOT) {
            Slog.w(TAG, "Failed to take screenshot " + appWindowToken + " mHadTakenSnapShot " + appWindowToken.mHadTakenSnapShot);
            return null;
        } else if (appWindowToken.hasCommittedReparentToAnimationLeash(animationLeashDelay)) {
            Slog.w(TAG, "Failed to take screenshot. App is animating " + appWindowToken);
            return null;
        } else {
            boolean isLowRamDevice = ActivityManager.isLowRamDeviceStatic();
            if (isLowRamDevice) {
                scaleFraction = this.mPersister.getReducedScale();
            } else {
                scaleFraction = this.mFullSnapshotScale;
            }
            boolean isReduced = isLowRamDevice;
            if (USE_CONFIG_SCALE) {
                scaleFraction = ((float) CONFIG_SCALE) / 100.0f;
                isReduced = true;
            }
            WindowState mainWindow = appWindowToken.findMainWindow();
            if (mainWindow == null) {
                Slog.w(TAG, "Failed to take screenshot. No main window for " + task);
                return null;
            }
            SurfaceControl.ScreenshotGraphicBuffer screenshotBuffer = createTaskSnapshot(task, scaleFraction);
            if (screenshotBuffer == null) {
                return null;
            }
            boolean isAdjustForIme = true;
            appWindowToken.mHadTakenSnapShot = true;
            ActivityManager.TaskSnapshot taskSnapshot = new ActivityManager.TaskSnapshot(appWindowToken.mActivityComponent, screenshotBuffer.getGraphicBuffer(), screenshotBuffer.getColorSpace(), appWindowToken.getTask().getConfiguration().orientation, getInsets(mainWindow), isReduced, scaleFraction, true, task.getWindowingMode(), getSystemUiVisibility(task), !appWindowToken.fillsParent() || (mainWindow.getAttrs().format != -1));
            taskSnapshot.setWindowBounds(task.getBounds());
            if (!task.inHwSplitScreenWindowingMode() || task.mStack == null || !task.mStack.isAdjustedForIme()) {
                isAdjustForIme = false;
            }
            taskSnapshot.setWindowBounds(isAdjustForIme ? task.mStack.getRawBounds() : task.getBounds());
            return taskSnapshot;
        }
    }

    private boolean shouldDisableSnapshots() {
        return this.mIsRunningOnWear || this.mIsRunningOnTv || this.mIsRunningOnIoT;
    }

    private Rect getInsets(WindowState state) {
        Rect insets = minRect(state.getContentInsets(), state.getStableInsets());
        InsetUtils.addInsets(insets, state.mAppToken.getLetterboxInsets());
        return insets;
    }

    private Rect minRect(Rect rect1, Rect rect2) {
        return new Rect(Math.min(rect1.left, rect2.left), Math.min(rect1.top, rect2.top), Math.min(rect1.right, rect2.right), Math.min(rect1.bottom, rect2.bottom));
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void getClosingTasks(ArraySet<AppWindowToken> closingApps, ArraySet<Task> outClosingTasks) {
        outClosingTasks.clear();
        for (int i = closingApps.size() - 1; i >= 0; i--) {
            Task task = closingApps.valueAt(i).getTask();
            if (task != null && !task.isVisible() && !this.mSkipClosingAppSnapshotTasks.contains(task)) {
                outClosingTasks.add(task);
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public int getSnapshotMode(Task task) {
        AppWindowToken topChild = (AppWindowToken) task.getTopChild();
        if (!task.isActivityTypeStandardOrUndefined() && !task.isActivityTypeAssistant()) {
            return 2;
        }
        if (topChild != null && topChild.toString().contains("VolumeAdjustmentTutorialMainActivity")) {
            return 2;
        }
        if (topChild == null || !topChild.shouldUseAppThemeSnapshot()) {
            return 0;
        }
        return 1;
    }

    private ActivityManager.TaskSnapshot drawAppThemeSnapshot(Task task) {
        WindowState mainWindow;
        AppWindowToken topChild = (AppWindowToken) task.getTopChild();
        if (topChild == null || (mainWindow = topChild.findMainWindow()) == null) {
            return null;
        }
        int color = ColorUtils.setAlphaComponent(task.getTaskDescription().getBackgroundColor(), 255);
        WindowManager.LayoutParams attrs = mainWindow.getAttrs();
        TaskSnapshotSurface.SystemBarBackgroundPainter decorPainter = new TaskSnapshotSurface.SystemBarBackgroundPainter(attrs.flags, attrs.privateFlags, attrs.systemUiVisibility, task.getTaskDescription(), this.mFullSnapshotScale);
        int width = (int) (((float) task.getBounds().width()) * this.mFullSnapshotScale);
        int height = (int) (((float) task.getBounds().height()) * this.mFullSnapshotScale);
        RenderNode node = RenderNode.create("TaskSnapshotController", null);
        node.setLeftTopRightBottom(0, 0, width, height);
        node.setClipToBounds(false);
        RecordingCanvas c = node.start(width, height);
        c.drawColor(color);
        decorPainter.setInsets(mainWindow.getContentInsets(), mainWindow.getStableInsets());
        decorPainter.drawDecors(c, null);
        node.end(c);
        Bitmap hwBitmap = ThreadedRenderer.createHardwareBitmap(node, width, height);
        if (hwBitmap == null) {
            return null;
        }
        ActivityManager.TaskSnapshot taskSnapshot = new ActivityManager.TaskSnapshot(topChild.mActivityComponent, hwBitmap.createGraphicBufferHandle(), hwBitmap.getColorSpace(), topChild.getTask().getConfiguration().orientation, getInsets(mainWindow), ActivityManager.isLowRamDeviceStatic(), this.mFullSnapshotScale, false, task.getWindowingMode(), getSystemUiVisibility(task), false);
        taskSnapshot.setWindowBounds(task.getBounds());
        return taskSnapshot;
    }

    /* access modifiers changed from: package-private */
    public void onAppRemoved(AppWindowToken wtoken) {
        this.mCache.onAppRemoved(wtoken);
    }

    /* access modifiers changed from: package-private */
    public void onAppDied(AppWindowToken wtoken) {
        this.mCache.onAppDied(wtoken);
    }

    /* access modifiers changed from: package-private */
    public void notifyTaskRemovedFromRecents(int taskId, int userId) {
        this.mCache.onTaskRemoved(taskId);
        this.mPersister.onTaskRemovedFromRecents(taskId, userId);
    }

    /* access modifiers changed from: package-private */
    public void removeObsoleteTaskFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        this.mPersister.removeObsoleteFiles(persistentTaskIds, runningUserIds);
    }

    /* access modifiers changed from: package-private */
    public void setPersisterPaused(boolean paused) {
        this.mPersister.setPaused(paused);
    }

    /* access modifiers changed from: package-private */
    public void screenTurningOff(WindowManagerPolicy.ScreenOffListener listener) {
        if (shouldDisableSnapshots()) {
            listener.onScreenOff();
        } else {
            this.mHandler.post(new Runnable(listener) {
                /* class com.android.server.wm.$$Lambda$TaskSnapshotController$qBG2kMqHK9gvuY43J0TfS4aSVU */
                private final /* synthetic */ WindowManagerPolicy.ScreenOffListener f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    TaskSnapshotController.this.lambda$screenTurningOff$2$TaskSnapshotController(this.f$1);
                }
            });
        }
    }

    /* JADX INFO: finally extract failed */
    public /* synthetic */ void lambda$screenTurningOff$2$TaskSnapshotController(WindowManagerPolicy.ScreenOffListener listener) {
        try {
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mTmpTasks.clear();
                    this.mService.mRoot.forAllTasks(new Consumer() {
                        /* class com.android.server.wm.$$Lambda$TaskSnapshotController$ewiDm2ws6pdTXd1elso7FtoLKw */

                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            TaskSnapshotController.this.lambda$screenTurningOff$1$TaskSnapshotController((Task) obj);
                        }
                    });
                    snapshotTasks(this.mTmpTasks);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            listener.onScreenOff();
        }
    }

    public /* synthetic */ void lambda$screenTurningOff$1$TaskSnapshotController(Task task) {
        if (task.isVisible()) {
            this.mTmpTasks.add(task);
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

    /* access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "mFullSnapshotScale=" + this.mFullSnapshotScale);
        this.mCache.dump(pw, prefix);
    }

    public ActivityManager.TaskSnapshot getForegroundTaskSnapshot() {
        return this.mCache.getLastForegroundSnapshot();
    }

    public void clearForegroundTaskSnapshot() {
        this.mCache.clearForegroundTaskSnapshot();
    }

    public ActivityManager.TaskSnapshot createForegroundTaskSnapshot(AppWindowToken appWindowToken) {
        final ActivityManager.TaskSnapshot snapshot;
        final Task task = appWindowToken.getTask();
        int mode = getSnapshotMode(task);
        if (mode == 0) {
            appWindowToken.mHadTakenSnapShot = false;
            snapshot = snapshotTask(task, true);
        } else if (mode == 1) {
            snapshot = drawAppThemeSnapshot(task);
        } else if (mode != 2) {
            snapshot = null;
        } else {
            snapshot = null;
        }
        if (snapshot != null) {
            GraphicBuffer buffer = snapshot.getSnapshot();
            if (buffer.getWidth() == 0 || buffer.getHeight() == 0) {
                buffer.destroy();
                Slog.e(TAG, "getForgroundTaskSnapshot Invalid task snapshot dimensions " + buffer.getWidth() + "x" + buffer.getHeight());
                return null;
            }
            this.mCache.putForegroundSnapShot(task, snapshot);
            if (HW_SNAPSHOT) {
                this.mCache.putSnapshot(task, snapshot);
                if (HwDisplaySizeUtil.hasSideInScreen()) {
                    this.mPersister.persistSnapshot(task.mTaskId, task.mUserId, snapshot, ColorUtils.setAlphaComponent(task.getTaskDescription().getBackgroundColor(), 255));
                } else {
                    this.mPersister.persistSnapshot(task.mTaskId, task.mUserId, snapshot);
                }
                this.mHandler.post(new Runnable() {
                    /* class com.android.server.wm.TaskSnapshotController.AnonymousClass1 */

                    public void run() {
                        task.onSnapshotChanged(snapshot);
                    }
                });
            }
        }
        return snapshot;
    }

    public void clearSnapshot() {
        this.mCache.clearSnapshot();
    }
}
