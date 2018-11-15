package com.android.server.wm;

import android.app.ActivityManager.TaskDescription;
import android.app.ActivityManager.TaskSnapshot;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.EventLogTags;
import com.android.server.os.HwBootFail;
import java.lang.ref.WeakReference;

public class TaskWindowContainerController extends WindowContainerController<Task, TaskWindowContainerListener> {
    private final H mHandler;
    private final int mTaskId;

    private static final class H extends Handler {
        static final int REPORT_SNAPSHOT_CHANGED = 0;
        static final int REQUEST_RESIZE = 1;
        private final WeakReference<TaskWindowContainerController> mController;

        H(WeakReference<TaskWindowContainerController> controller, Looper looper) {
            super(looper);
            this.mController = controller;
        }

        public void handleMessage(Message msg) {
            TaskWindowContainerController controller = (TaskWindowContainerController) this.mController.get();
            TaskWindowContainerListener listener = controller != null ? (TaskWindowContainerListener) controller.mListener : null;
            if (listener != null) {
                switch (msg.what) {
                    case 0:
                        listener.onSnapshotChanged((TaskSnapshot) msg.obj);
                        break;
                    case 1:
                        listener.requestResize((Rect) msg.obj, msg.arg1);
                        break;
                }
            }
        }
    }

    public TaskWindowContainerController(int taskId, TaskWindowContainerListener listener, StackWindowController stackController, int userId, Rect bounds, int resizeMode, boolean supportsPictureInPicture, boolean toTop, boolean showForAllUsers, TaskDescription taskDescription) {
        this(taskId, listener, stackController, userId, bounds, resizeMode, supportsPictureInPicture, toTop, showForAllUsers, taskDescription, WindowManagerService.getInstance());
    }

    public TaskWindowContainerController(int taskId, TaskWindowContainerListener listener, StackWindowController stackController, int userId, Rect bounds, int resizeMode, boolean supportsPictureInPicture, boolean toTop, boolean showForAllUsers, TaskDescription taskDescription, WindowManagerService service) {
        Throwable th;
        StackWindowController stackWindowController = stackController;
        boolean z = toTop;
        WindowManagerService windowManagerService = service;
        super(listener, windowManagerService);
        int i = taskId;
        this.mTaskId = i;
        this.mHandler = new H(new WeakReference(this), windowManagerService.mH.getLooper());
        synchronized (this.mWindowMap) {
            boolean z2;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskStack stack = stackWindowController.mContainer;
                if (stack != null) {
                    EventLog.writeEvent(EventLogTags.WM_TASK_CREATED, new Object[]{Integer.valueOf(taskId), Integer.valueOf(stack.mStackId)});
                    stack.addTask(createTask(i, stack, userId, resizeMode, supportsPictureInPicture, taskDescription), z ? HwBootFail.STAGE_BOOT_SUCCESS : Integer.MIN_VALUE, showForAllUsers, z);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                z2 = showForAllUsers;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("TaskWindowContainerController: invalid stack=");
                stringBuilder.append(stackWindowController);
                throw new IllegalArgumentException(stringBuilder.toString());
            } catch (Throwable th2) {
                th = th2;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @VisibleForTesting
    Task createTask(int taskId, TaskStack stack, int userId, int resizeMode, boolean supportsPictureInPicture, TaskDescription taskDescription) {
        return new Task(taskId, stack, userId, this.mService, resizeMode, supportsPictureInPicture, taskDescription, this);
    }

    public void removeContainer() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    ((Task) this.mContainer).removeIfPossible();
                    super.removeContainer();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void positionChildAtTop(AppWindowContainerController childController) {
        positionChildAt(childController, HwBootFail.STAGE_BOOT_SUCCESS);
    }

    public void positionChildAt(AppWindowContainerController childController, int position) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                AppWindowToken aToken = childController.mContainer;
                if (aToken == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to position of non-existing app : ");
                    stringBuilder.append(childController);
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    Task task = this.mContainer;
                    if (task != null) {
                        task.positionChildAt(position, aToken, false);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("positionChildAt: invalid task=");
                    stringBuilder2.append(this);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void reparent(StackWindowController stackController, int position, boolean moveParents) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    TaskStack stack = stackController.mContainer;
                    if (stack != null) {
                        ((Task) this.mContainer).reparent(stack, position, moveParents);
                        ((Task) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("reparent: could not find stack=");
                    stringBuilder.append(stackController);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setResizeable(int resizeMode) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    ((Task) this.mContainer).setResizeable(resizeMode);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void resize(boolean relayout, boolean forced) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    if (((Task) this.mContainer).setBounds(((Task) this.mContainer).getOverrideBounds(), forced) != 0 && relayout) {
                        ((Task) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("resizeTask: taskId ");
                    stringBuilder.append(this.mTaskId);
                    stringBuilder.append(" not found.");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void getBounds(Rect bounds) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    ((Task) this.mContainer).getBounds(bounds);
                } else {
                    bounds.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setTaskDockedResizing(boolean resizing) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setTaskDockedResizing: taskId ");
                    stringBuilder.append(this.mTaskId);
                    stringBuilder.append(" not found.");
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((Task) this.mContainer).setDragResizing(resizing, 1);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void cancelWindowTransition() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cancelWindowTransition: taskId ");
                    stringBuilder.append(this.mTaskId);
                    stringBuilder.append(" not found.");
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((Task) this.mContainer).cancelTaskWindowTransition();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setTaskDescription(TaskDescription taskDescription) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setTaskDescription: taskId ");
                    stringBuilder.append(this.mTaskId);
                    stringBuilder.append(" not found.");
                    Slog.w("WindowManager", stringBuilder.toString());
                } else {
                    ((Task) this.mContainer).setTaskDescription(taskDescription);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    void reportSnapshotChanged(TaskSnapshot snapshot) {
        this.mHandler.obtainMessage(0, snapshot).sendToTarget();
    }

    void requestResize(Rect bounds, int resizeMode) {
        this.mHandler.obtainMessage(1, resizeMode, 0, bounds).sendToTarget();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{TaskWindowContainerController taskId=");
        stringBuilder.append(this.mTaskId);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
