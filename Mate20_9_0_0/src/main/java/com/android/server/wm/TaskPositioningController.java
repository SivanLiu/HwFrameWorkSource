package com.android.server.wm;

import android.app.IActivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import com.android.internal.annotations.GuardedBy;
import com.android.server.input.InputManagerService;
import com.android.server.input.InputWindowHandle;

class TaskPositioningController {
    private final IActivityManager mActivityManager;
    private final Handler mHandler;
    private final InputManagerService mInputManager;
    private final InputMonitor mInputMonitor;
    private final WindowManagerService mService;
    @GuardedBy("WindowManagerSerivce.mWindowMap")
    private TaskPositioner mTaskPositioner;

    boolean isPositioningLocked() {
        return this.mTaskPositioner != null;
    }

    InputWindowHandle getDragWindowHandleLocked() {
        return this.mTaskPositioner != null ? this.mTaskPositioner.mDragWindowHandle : null;
    }

    TaskPositioningController(WindowManagerService service, InputManagerService inputManager, InputMonitor inputMonitor, IActivityManager activityManager, Looper looper) {
        this.mService = service;
        this.mInputMonitor = inputMonitor;
        this.mInputManager = inputManager;
        this.mActivityManager = activityManager;
        this.mHandler = new Handler(looper);
    }

    /* JADX WARNING: Missing block: B:9:0x0024, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:11:?, code:
            r11.mActivityManager.setFocusedTask(r0.getTask().mTaskId);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean startMovingTask(IWindow window, float startX, float startY) {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = false;
                WindowState win = this.mService.windowForClientLocked(null, window, false);
                if (!startPositioningLocked(win, false, false, startX, startY)) {
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return z;
        return true;
    }

    void handleTapOutsideTask(DisplayContent displayContent, int x, int y) {
        this.mHandler.post(new -$$Lambda$TaskPositioningController$WvS6bGwsoNKniWwQXf4LtUhPblY(this, x, y, displayContent));
    }

    /* JADX WARNING: Missing block: B:20:0x0054, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:21:0x005b, code:
            if (android.util.HwPCUtils.isPcCastModeInServer() == false) goto L_0x0075;
     */
    /* JADX WARNING: Missing block: B:22:0x005d, code:
            r12.mService.setFocusedDisplay(r15.getDisplayId(), false, "handleTapOutsideTaskXY");
            r4 = r12.mService;
     */
    /* JADX WARNING: Missing block: B:23:0x006a, code:
            if (r2 >= 0) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:25:0x006e, code:
            if (r15.isDefaultDisplay != false) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:26:0x0071, code:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:27:0x0072, code:
            r4.setPCLauncherFocused(r1);
     */
    /* JADX WARNING: Missing block: B:28:0x0075, code:
            if (r2 < 0) goto L_0x007e;
     */
    /* JADX WARNING: Missing block: B:30:?, code:
            r12.mActivityManager.setFocusedTask(r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$handleTapOutsideTask$0(TaskPositioningController taskPositioningController, int x, int y, DisplayContent displayContent) {
        boolean oldPCLauncherFocused;
        boolean z = true;
        if (HwPCUtils.isPcCastModeInServer() && x == -1 && y == -1) {
            taskPositioningController.mService.setFocusedDisplay(displayContent.getDisplayId(), true, "handleTapOutsideTask-1-1");
            return;
        }
        oldPCLauncherFocused = taskPositioningController.mService.getPCLauncherFocused();
        taskPositioningController.mService.setPCLauncherFocused(false);
        synchronized (taskPositioningController.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = displayContent.findTaskForResizePoint(x, y);
                int taskId;
                if (task != null) {
                    if (taskPositioningController.startPositioningLocked(task.getTopVisibleAppMainWindow(), true, task.preserveOrientationOnResize(), (float) x, (float) y)) {
                        taskId = task.mTaskId;
                    }
                } else {
                    taskId = displayContent.taskIdFromPoint(x, y);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return;
        if (oldPCLauncherFocused != taskPositioningController.mService.getPCLauncherFocused()) {
            synchronized (taskPositioningController.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    displayContent.layoutAndAssignWindowLayersIfNeeded();
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    private boolean startPositioningLocked(WindowState win, boolean resize, boolean preserveOrientation, float startX, float startY) {
        WindowState windowState = win;
        StringBuilder stringBuilder;
        if (windowState == null || windowState.getAppToken() == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("startPositioningLocked: Bad window ");
            stringBuilder.append(windowState);
            Slog.w("WindowManager", stringBuilder.toString());
            return false;
        } else if (windowState.mInputChannel == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("startPositioningLocked: ");
            stringBuilder.append(windowState);
            stringBuilder.append(" has no input channel,  probably being removed");
            Slog.wtf("WindowManager", stringBuilder.toString());
            return false;
        } else {
            DisplayContent displayContent = windowState.getDisplayContent();
            if (displayContent == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("startPositioningLocked: Invalid display content ");
                stringBuilder.append(windowState);
                Slog.w("WindowManager", stringBuilder.toString());
                return false;
            }
            Display display = displayContent.getDisplay();
            this.mTaskPositioner = TaskPositioner.create(this.mService);
            this.mTaskPositioner.register(displayContent);
            this.mInputMonitor.updateInputWindowsLw(true);
            WindowState transferFocusFromWin = windowState;
            if (!(this.mService.mCurrentFocus == null || this.mService.mCurrentFocus == windowState || this.mService.mCurrentFocus.mAppToken != windowState.mAppToken)) {
                transferFocusFromWin = this.mService.mCurrentFocus;
            }
            if (this.mInputManager.transferTouchFocus(transferFocusFromWin.mInputChannel, this.mTaskPositioner.mServerChannel)) {
                this.mTaskPositioner.startDrag(windowState, resize, preserveOrientation, startX, startY);
                return true;
            }
            Slog.e("WindowManager", "startPositioningLocked: Unable to transfer touch focus");
            this.mTaskPositioner.unregister();
            this.mTaskPositioner = null;
            this.mInputMonitor.updateInputWindowsLw(true);
            return false;
        }
    }

    void finishTaskPositioning() {
        this.mHandler.post(new -$$Lambda$TaskPositioningController$z3n1stJjOdhDbXXrvPlvlqmON6k(this));
    }

    public static /* synthetic */ void lambda$finishTaskPositioning$1(TaskPositioningController taskPositioningController) {
        synchronized (taskPositioningController.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (taskPositioningController.mTaskPositioner != null) {
                    taskPositioningController.mTaskPositioner.unregister();
                    taskPositioningController.mTaskPositioner = null;
                    taskPositioningController.mInputMonitor.updateInputWindowsLw(true);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
