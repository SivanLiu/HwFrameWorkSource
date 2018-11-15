package com.android.server.am;

import android.app.ActivityManager.TaskDescription;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ITaskStackListener;
import android.content.ComponentName;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import java.util.ArrayList;

class TaskChangeNotificationController {
    private static final int LOG_STACK_STATE_MSG = 1;
    private static final int NOTIFY_ACTIVITY_DISMISSING_DOCKED_STACK_MSG = 7;
    private static final int NOTIFY_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED_MSG = 18;
    private static final int NOTIFY_ACTIVITY_PINNED_LISTENERS_MSG = 3;
    private static final int NOTIFY_ACTIVITY_REQUESTED_ORIENTATION_CHANGED_LISTENERS = 12;
    private static final int NOTIFY_ACTIVITY_UNPINNED_LISTENERS_MSG = 17;
    private static final int NOTIFY_FORCED_RESIZABLE_MSG = 6;
    private static final int NOTIFY_PINNED_ACTIVITY_RESTART_ATTEMPT_LISTENERS_MSG = 4;
    private static final int NOTIFY_PINNED_STACK_ANIMATION_ENDED_LISTENERS_MSG = 5;
    private static final int NOTIFY_PINNED_STACK_ANIMATION_STARTED_LISTENERS_MSG = 16;
    private static final int NOTIFY_TASK_ADDED_LISTENERS_MSG = 8;
    private static final int NOTIFY_TASK_DESCRIPTION_CHANGED_LISTENERS_MSG = 11;
    private static final int NOTIFY_TASK_MOVED_TO_FRONT_LISTENERS_MSG = 10;
    private static final int NOTIFY_TASK_PROFILE_LOCKED_LISTENERS_MSG = 14;
    private static final int NOTIFY_TASK_REMOVAL_STARTED_LISTENERS = 13;
    private static final int NOTIFY_TASK_REMOVED_LISTENERS_MSG = 9;
    private static final int NOTIFY_TASK_SNAPSHOT_CHANGED_LISTENERS_MSG = 15;
    private static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_DELAY = 100;
    private static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_MSG = 2;
    private final Handler mHandler;
    private final ArrayList<ITaskStackListener> mLocalTaskStackListeners = new ArrayList();
    private final TaskStackConsumer mNotifyActivityDismissingDockedStack = -$$Lambda$TaskChangeNotificationController$d9Depygk2x7Vm_pl1RSk9_SSjvA.INSTANCE;
    private final TaskStackConsumer mNotifyActivityForcedResizable = -$$Lambda$TaskChangeNotificationController$sw023kIrIGSeLwYwKC0ioKX3zEA.INSTANCE;
    private final TaskStackConsumer mNotifyActivityLaunchOnSecondaryDisplayFailed = -$$Lambda$TaskChangeNotificationController$Ln9-GPCsfrWRlWBInk_Po_Uv-_U.INSTANCE;
    private final TaskStackConsumer mNotifyActivityPinned = -$$Lambda$TaskChangeNotificationController$a1rNhcYLIsgLeCng0_osaimgbqE.INSTANCE;
    private final TaskStackConsumer mNotifyActivityRequestedOrientationChanged = -$$Lambda$TaskChangeNotificationController$grn5FwM5ofT98exjpSvrJhz-e7s.INSTANCE;
    private final TaskStackConsumer mNotifyActivityUnpinned = -$$Lambda$TaskChangeNotificationController$3Qs2duXCIzQ1W3uon7k5iYUmOy8.INSTANCE;
    private final TaskStackConsumer mNotifyPinnedActivityRestartAttempt = -$$Lambda$TaskChangeNotificationController$YVmGNqlD5lzQCN49aly8kWWz1po.INSTANCE;
    private final TaskStackConsumer mNotifyPinnedStackAnimationEnded = -$$Lambda$TaskChangeNotificationController$5xMsPmGMl_n12-F1m2p9OBuXGrA.INSTANCE;
    private final TaskStackConsumer mNotifyPinnedStackAnimationStarted = -$$Lambda$TaskChangeNotificationController$iVGVcx2Ee37igl6ebl_htq_WO9o.INSTANCE;
    private final TaskStackConsumer mNotifyTaskCreated = -$$Lambda$TaskChangeNotificationController$YDk9fnP8p2R_OweiU9rSGaheQeE.INSTANCE;
    private final TaskStackConsumer mNotifyTaskDescriptionChanged = -$$Lambda$TaskChangeNotificationController$bteC39aBoUFmJeWf3dk2BX1xZ6k.INSTANCE;
    private final TaskStackConsumer mNotifyTaskMovedToFront = -$$Lambda$TaskChangeNotificationController$IPqcWaWHIL4UnZEYJhAve5H7KmE.INSTANCE;
    private final TaskStackConsumer mNotifyTaskProfileLocked = -$$Lambda$TaskChangeNotificationController$FNdlAMBaRkRCa4U_pc-uamD9VHw.INSTANCE;
    private final TaskStackConsumer mNotifyTaskRemovalStarted = -$$Lambda$TaskChangeNotificationController$O2UuB84QeMcZfsRHiuiFSTwwWHY.INSTANCE;
    private final TaskStackConsumer mNotifyTaskRemoved = -$$Lambda$TaskChangeNotificationController$K9kaSj6_p5pzfyRh9i93xiC9T3s.INSTANCE;
    private final TaskStackConsumer mNotifyTaskSnapshotChanged = -$$Lambda$TaskChangeNotificationController$1RAH1a7gRlnrDczBty2_cTiNlBI.INSTANCE;
    private final TaskStackConsumer mNotifyTaskStackChanged = -$$Lambda$TaskChangeNotificationController$kftD881t3KfWCASQEbeTkieVI2M.INSTANCE;
    private final RemoteCallbackList<ITaskStackListener> mRemoteTaskStackListeners = new RemoteCallbackList();
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (TaskChangeNotificationController.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            TaskChangeNotificationController.this.mStackSupervisor.logStackState();
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 2:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskStackChanged, msg);
                    return;
                case 3:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityPinned, msg);
                    return;
                case 4:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyPinnedActivityRestartAttempt, msg);
                    return;
                case 5:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyPinnedStackAnimationEnded, msg);
                    return;
                case 6:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityForcedResizable, msg);
                    return;
                case 7:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityDismissingDockedStack, msg);
                    return;
                case 8:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskCreated, msg);
                    return;
                case 9:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskRemoved, msg);
                    return;
                case 10:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskMovedToFront, msg);
                    return;
                case 11:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskDescriptionChanged, msg);
                    return;
                case 12:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityRequestedOrientationChanged, msg);
                    return;
                case 13:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskRemovalStarted, msg);
                    return;
                case 14:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskProfileLocked, msg);
                    return;
                case 15:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyTaskSnapshotChanged, msg);
                    return;
                case 16:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyPinnedStackAnimationStarted, msg);
                    return;
                case 17:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityUnpinned, msg);
                    return;
                case 18:
                    TaskChangeNotificationController.this.forAllRemoteListeners(TaskChangeNotificationController.this.mNotifyActivityLaunchOnSecondaryDisplayFailed, msg);
                    return;
                default:
                    return;
            }
        }
    }

    @FunctionalInterface
    public interface TaskStackConsumer {
        void accept(ITaskStackListener iTaskStackListener, Message message) throws RemoteException;
    }

    static /* synthetic */ void lambda$new$9(ITaskStackListener l, Message m) throws RemoteException {
        l.onPinnedActivityRestartAttempt(m.arg1 != 0);
    }

    public TaskChangeNotificationController(ActivityManagerService service, ActivityStackSupervisor stackSupervisor, Handler handler) {
        this.mService = service;
        this.mStackSupervisor = stackSupervisor;
        this.mHandler = new MainHandler(handler.getLooper());
    }

    public void registerTaskStackListener(ITaskStackListener listener) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (listener != null) {
                    if (Binder.getCallingPid() != Process.myPid()) {
                        this.mRemoteTaskStackListeners.register(listener);
                    } else if (!this.mLocalTaskStackListeners.contains(listener)) {
                        this.mLocalTaskStackListeners.add(listener);
                    }
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void unregisterTaskStackListener(ITaskStackListener listener) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (listener != null) {
                    if (Binder.getCallingPid() == Process.myPid()) {
                        this.mLocalTaskStackListeners.remove(listener);
                    } else {
                        this.mRemoteTaskStackListeners.unregister(listener);
                    }
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void forAllRemoteListeners(TaskStackConsumer callback, Message message) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int i = this.mRemoteTaskStackListeners.beginBroadcast() - 1; i >= 0; i--) {
                    try {
                        callback.accept((ITaskStackListener) this.mRemoteTaskStackListeners.getBroadcastItem(i), message);
                    } catch (RemoteException e) {
                    }
                }
                this.mRemoteTaskStackListeners.finishBroadcast();
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void forAllLocalListeners(TaskStackConsumer callback, Message message) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int i = this.mLocalTaskStackListeners.size() - 1; i >= 0; i--) {
                    try {
                        callback.accept((ITaskStackListener) this.mLocalTaskStackListeners.get(i), message);
                    } catch (RemoteException e) {
                    }
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    void notifyTaskStackChanged() {
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.removeMessages(2);
        Message msg = this.mHandler.obtainMessage(2);
        forAllLocalListeners(this.mNotifyTaskStackChanged, msg);
        this.mHandler.sendMessageDelayed(msg, 100);
    }

    void notifyActivityPinned(ActivityRecord r) {
        this.mHandler.removeMessages(3);
        Message msg = this.mHandler.obtainMessage(3, r.getTask().taskId, r.getStackId(), r.packageName);
        msg.sendingUid = r.userId;
        forAllLocalListeners(this.mNotifyActivityPinned, msg);
        msg.sendToTarget();
    }

    void notifyActivityUnpinned() {
        this.mHandler.removeMessages(17);
        Message msg = this.mHandler.obtainMessage(17);
        forAllLocalListeners(this.mNotifyActivityUnpinned, msg);
        msg.sendToTarget();
    }

    void notifyPinnedActivityRestartAttempt(boolean clearedTask) {
        this.mHandler.removeMessages(4);
        Message msg = this.mHandler.obtainMessage(4, clearedTask, 0);
        forAllLocalListeners(this.mNotifyPinnedActivityRestartAttempt, msg);
        msg.sendToTarget();
    }

    void notifyPinnedStackAnimationStarted() {
        this.mHandler.removeMessages(16);
        Message msg = this.mHandler.obtainMessage(16);
        forAllLocalListeners(this.mNotifyPinnedStackAnimationStarted, msg);
        msg.sendToTarget();
    }

    void notifyPinnedStackAnimationEnded() {
        this.mHandler.removeMessages(5);
        Message msg = this.mHandler.obtainMessage(5);
        forAllLocalListeners(this.mNotifyPinnedStackAnimationEnded, msg);
        msg.sendToTarget();
    }

    void notifyActivityDismissingDockedStack() {
        this.mHandler.removeMessages(7);
        Message msg = this.mHandler.obtainMessage(7);
        forAllLocalListeners(this.mNotifyActivityDismissingDockedStack, msg);
        msg.sendToTarget();
    }

    void notifyActivityForcedResizable(int taskId, int reason, String packageName) {
        this.mHandler.removeMessages(6);
        Message msg = this.mHandler.obtainMessage(6, taskId, reason, packageName);
        forAllLocalListeners(this.mNotifyActivityForcedResizable, msg);
        msg.sendToTarget();
    }

    void notifyActivityLaunchOnSecondaryDisplayFailed() {
        this.mHandler.removeMessages(18);
        Message msg = this.mHandler.obtainMessage(18);
        forAllLocalListeners(this.mNotifyActivityLaunchOnSecondaryDisplayFailed, msg);
        msg.sendToTarget();
    }

    void notifyTaskCreated(int taskId, ComponentName componentName) {
        Message msg = this.mHandler.obtainMessage(8, taskId, 0, componentName);
        forAllLocalListeners(this.mNotifyTaskCreated, msg);
        msg.sendToTarget();
    }

    void notifyTaskRemoved(int taskId) {
        Message msg = this.mHandler.obtainMessage(9, taskId, 0);
        forAllLocalListeners(this.mNotifyTaskRemoved, msg);
        msg.sendToTarget();
    }

    void notifyTaskMovedToFront(int taskId) {
        Message msg = this.mHandler.obtainMessage(10, taskId, 0);
        forAllLocalListeners(this.mNotifyTaskMovedToFront, msg);
        msg.sendToTarget();
    }

    void notifyTaskDescriptionChanged(int taskId, TaskDescription taskDescription) {
        Message msg = this.mHandler.obtainMessage(11, taskId, 0, taskDescription);
        forAllLocalListeners(this.mNotifyTaskDescriptionChanged, msg);
        msg.sendToTarget();
    }

    void notifyActivityRequestedOrientationChanged(int taskId, int orientation) {
        Message msg = this.mHandler.obtainMessage(12, taskId, orientation);
        forAllLocalListeners(this.mNotifyActivityRequestedOrientationChanged, msg);
        msg.sendToTarget();
    }

    void notifyTaskRemovalStarted(int taskId) {
        Message msg = this.mHandler.obtainMessage(13, taskId, 0);
        forAllLocalListeners(this.mNotifyTaskRemovalStarted, msg);
        msg.sendToTarget();
    }

    void notifyTaskProfileLocked(int taskId, int userId) {
        Message msg = this.mHandler.obtainMessage(14, taskId, userId);
        forAllLocalListeners(this.mNotifyTaskProfileLocked, msg);
        msg.sendToTarget();
    }

    void notifyTaskSnapshotChanged(int taskId, TaskSnapshot snapshot) {
        Message msg = this.mHandler.obtainMessage(15, taskId, 0, snapshot);
        forAllLocalListeners(this.mNotifyTaskSnapshotChanged, msg);
        msg.sendToTarget();
    }
}
