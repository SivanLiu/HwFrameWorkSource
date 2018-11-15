package com.android.systemui.shared.system;

import android.app.ActivityManager.TaskSnapshot;
import android.app.IActivityManager;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import com.android.systemui.shared.recents.model.ThumbnailData;
import java.util.ArrayList;
import java.util.List;

public class TaskStackChangeListeners extends TaskStackListener {
    private static final String TAG = TaskStackChangeListeners.class.getSimpleName();
    private final Handler mHandler;
    private boolean mRegistered;
    private final List<TaskStackChangeListener> mTaskStackListeners = new ArrayList();
    private final List<TaskStackChangeListener> mTmpListeners = new ArrayList();

    private final class H extends Handler {
        private static final int ON_ACTIVITY_DISMISSING_DOCKED_STACK = 7;
        private static final int ON_ACTIVITY_FORCED_RESIZABLE = 6;
        private static final int ON_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED = 11;
        private static final int ON_ACTIVITY_PINNED = 3;
        private static final int ON_ACTIVITY_REQUESTED_ORIENTATION_CHANGE = 15;
        private static final int ON_ACTIVITY_UNPINNED = 10;
        private static final int ON_PINNED_ACTIVITY_RESTART_ATTEMPT = 4;
        private static final int ON_PINNED_STACK_ANIMATION_ENDED = 5;
        private static final int ON_PINNED_STACK_ANIMATION_STARTED = 9;
        private static final int ON_TASK_CREATED = 12;
        private static final int ON_TASK_MOVED_TO_FRONT = 14;
        private static final int ON_TASK_PROFILE_LOCKED = 8;
        private static final int ON_TASK_REMOVED = 13;
        private static final int ON_TASK_SNAPSHOT_CHANGED = 2;
        private static final int ON_TASK_STACK_CHANGED = 1;

        public H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            synchronized (TaskStackChangeListeners.this.mTaskStackListeners) {
                int i;
                switch (msg.what) {
                    case 1:
                        Trace.beginSection("onTaskStackChanged");
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onTaskStackChanged();
                        }
                        Trace.endSection();
                        break;
                    case 2:
                        Trace.beginSection("onTaskSnapshotChanged");
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onTaskSnapshotChanged(msg.arg1, new ThumbnailData((TaskSnapshot) msg.obj));
                        }
                        Trace.endSection();
                        break;
                    case 3:
                        PinnedActivityInfo info = msg.obj;
                        int i2 = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1;
                        while (true) {
                            int i3 = i2;
                            if (i3 < 0) {
                                break;
                            }
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i3)).onActivityPinned(info.mPackageName, info.mUserId, info.mTaskId, info.mStackId);
                            i2 = i3 - 1;
                        }
                    case 4:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onPinnedActivityRestartAttempt(msg.arg1 != 0);
                        }
                        break;
                    case 5:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onPinnedStackAnimationEnded();
                        }
                        break;
                    case 6:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onActivityForcedResizable((String) msg.obj, msg.arg1, msg.arg2);
                        }
                        break;
                    case 7:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onActivityDismissingDockedStack();
                        }
                        break;
                    case 8:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onTaskProfileLocked(msg.arg1, msg.arg2);
                        }
                        break;
                    case 9:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onPinnedStackAnimationStarted();
                        }
                        break;
                    case 10:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onActivityUnpinned();
                        }
                        break;
                    case 11:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onActivityLaunchOnSecondaryDisplayFailed();
                        }
                        break;
                    case 12:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onTaskCreated(msg.arg1, (ComponentName) msg.obj);
                        }
                        break;
                    case 13:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onTaskRemoved(msg.arg1);
                        }
                        break;
                    case 14:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onTaskMovedToFront(msg.arg1);
                        }
                        break;
                    case 15:
                        for (i = TaskStackChangeListeners.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                            ((TaskStackChangeListener) TaskStackChangeListeners.this.mTaskStackListeners.get(i)).onActivityRequestedOrientationChanged(msg.arg1, msg.arg2);
                        }
                        break;
                }
            }
        }
    }

    private static class PinnedActivityInfo {
        final String mPackageName;
        final int mStackId;
        final int mTaskId;
        final int mUserId;

        PinnedActivityInfo(String packageName, int userId, int taskId, int stackId) {
            this.mPackageName = packageName;
            this.mUserId = userId;
            this.mTaskId = taskId;
            this.mStackId = stackId;
        }
    }

    public TaskStackChangeListeners(Looper looper) {
        this.mHandler = new H(looper);
    }

    public void addListener(IActivityManager am, TaskStackChangeListener listener) {
        this.mTaskStackListeners.add(listener);
        if (!this.mRegistered) {
            try {
                am.registerTaskStackListener(this);
                this.mRegistered = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to call registerTaskStackListener", e);
            }
        }
    }

    public void removeListener(TaskStackChangeListener listener) {
        this.mTaskStackListeners.remove(listener);
    }

    public void onTaskStackChanged() throws RemoteException {
        synchronized (this.mTaskStackListeners) {
            this.mTmpListeners.clear();
            this.mTmpListeners.addAll(this.mTaskStackListeners);
        }
        for (int i = this.mTmpListeners.size() - 1; i >= 0; i--) {
            ((TaskStackChangeListener) this.mTmpListeners.get(i)).onTaskStackChangedBackground();
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void onActivityPinned(String packageName, int userId, int taskId, int stackId) throws RemoteException {
        this.mHandler.removeMessages(3);
        this.mHandler.obtainMessage(3, new PinnedActivityInfo(packageName, userId, taskId, stackId)).sendToTarget();
    }

    public void onActivityUnpinned() throws RemoteException {
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    public void onPinnedActivityRestartAttempt(boolean clearedTask) throws RemoteException {
        this.mHandler.removeMessages(4);
        this.mHandler.obtainMessage(4, clearedTask, 0).sendToTarget();
    }

    public void onPinnedStackAnimationStarted() throws RemoteException {
        this.mHandler.removeMessages(9);
        this.mHandler.sendEmptyMessage(9);
    }

    public void onPinnedStackAnimationEnded() throws RemoteException {
        this.mHandler.removeMessages(5);
        this.mHandler.sendEmptyMessage(5);
    }

    public void onActivityForcedResizable(String packageName, int taskId, int reason) throws RemoteException {
        this.mHandler.obtainMessage(6, taskId, reason, packageName).sendToTarget();
    }

    public void onActivityDismissingDockedStack() throws RemoteException {
        this.mHandler.sendEmptyMessage(7);
    }

    public void onActivityLaunchOnSecondaryDisplayFailed() throws RemoteException {
        this.mHandler.sendEmptyMessage(11);
    }

    public void onTaskProfileLocked(int taskId, int userId) throws RemoteException {
        this.mHandler.obtainMessage(8, taskId, userId).sendToTarget();
    }

    public void onTaskSnapshotChanged(int taskId, TaskSnapshot snapshot) throws RemoteException {
        this.mHandler.obtainMessage(2, taskId, 0, snapshot).sendToTarget();
    }

    public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
        this.mHandler.obtainMessage(12, taskId, 0, componentName).sendToTarget();
    }

    public void onTaskRemoved(int taskId) throws RemoteException {
        this.mHandler.obtainMessage(13, taskId, 0).sendToTarget();
    }

    public void onTaskMovedToFront(int taskId) throws RemoteException {
        this.mHandler.obtainMessage(14, taskId, 0).sendToTarget();
    }

    public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation) throws RemoteException {
        this.mHandler.obtainMessage(15, taskId, requestedOrientation).sendToTarget();
    }
}
