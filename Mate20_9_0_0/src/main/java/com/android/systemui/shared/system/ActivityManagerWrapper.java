package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.TaskDescription;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IAssistDataReceiver.Stub;
import android.app.WindowConfiguration.ActivityType;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.IRecentsAnimationController;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationTarget;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.Task.TaskKey;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import com.huawei.android.view.HwWindowManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ActivityManagerWrapper {
    public static final String CLOSE_SYSTEM_WINDOWS_REASON_RECENTS = "recentapps";
    private static final String TAG = "ActivityManagerWrapper";
    private static final ActivityManagerWrapper sInstance = new ActivityManagerWrapper();
    private final BackgroundExecutor mBackgroundExecutor = BackgroundExecutor.get();
    private Context mContext;
    private final PackageManager mPackageManager;
    private final TaskStackChangeListeners mTaskStackChangeListeners = new TaskStackChangeListeners(Looper.getMainLooper());

    private ActivityManagerWrapper() {
        Context context = AppGlobals.getInitialApplication();
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    public static ActivityManagerWrapper getInstance() {
        return sInstance;
    }

    public int getCurrentUserId() {
        try {
            UserInfo ui = ActivityManager.getService().getCurrentUser();
            return ui != null ? ui.id : 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public RunningTaskInfo getRunningTask() {
        return getRunningTask(3);
    }

    public RunningTaskInfo getRunningTask(@ActivityType int ignoreActivityType) {
        try {
            List<RunningTaskInfo> tasks = ActivityManager.getService().getFilteredTasks(1, ignoreActivityType, 2);
            if (tasks.isEmpty()) {
                return null;
            }
            return (RunningTaskInfo) tasks.get(0);
        } catch (RemoteException e) {
            return null;
        }
    }

    public List<RecentTaskInfo> getRecentTasks(int numTasks, int userId) {
        try {
            return ActivityManager.getService().getRecentTasks(numTasks, 2, userId).getList();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get recent tasks", e);
            return new ArrayList();
        }
    }

    public ThumbnailData getTaskThumbnail(int taskId, boolean reducedResolution) {
        TaskSnapshot snapshot = null;
        try {
            snapshot = ActivityManager.getService().getTaskSnapshot(taskId, reducedResolution);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to retrieve task snapshot", e);
        }
        if (snapshot != null) {
            return new ThumbnailData(snapshot);
        }
        return new ThumbnailData();
    }

    public String getBadgedActivityLabel(ActivityInfo info, int userId) {
        return getBadgedLabel(info.loadLabel(this.mPackageManager).toString(), userId);
    }

    public String getBadgedApplicationLabel(ApplicationInfo appInfo, int userId) {
        return getBadgedLabel(appInfo.loadLabel(this.mPackageManager).toString(), userId);
    }

    public String getBadgedContentDescription(ActivityInfo info, int userId, TaskDescription td) {
        String activityLabel;
        if (td == null || td.getLabel() == null) {
            activityLabel = info.loadLabel(this.mPackageManager).toString();
        } else {
            activityLabel = td.getLabel();
        }
        String applicationLabel = info.applicationInfo.loadLabel(this.mPackageManager).toString();
        String badgedApplicationLabel = getBadgedLabel(applicationLabel, userId);
        if (applicationLabel.equals(activityLabel)) {
            return badgedApplicationLabel;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(badgedApplicationLabel);
        stringBuilder.append(" ");
        stringBuilder.append(activityLabel);
        return stringBuilder.toString();
    }

    private String getBadgedLabel(String label, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPackageManager.getUserBadgedLabel(label, new UserHandle(userId)).toString();
        }
        return label;
    }

    public void startRecentsActivity(Intent intent, final AssistDataReceiver assistDataReceiver, final RecentsAnimationListener animationHandler, final Consumer<Boolean> resultCallback, Handler resultCallbackHandler) {
        Exception e = null;
        if (assistDataReceiver != null) {
            try {
                e = new Stub() {
                    public void onHandleAssistData(Bundle resultData) {
                        assistDataReceiver.onHandleAssistData(resultData);
                    }

                    public void onHandleAssistScreenshot(Bitmap screenshot) {
                        assistDataReceiver.onHandleAssistScreenshot(screenshot);
                    }
                };
            } catch (Exception e2) {
                if (resultCallback != null) {
                    resultCallbackHandler.post(new Runnable() {
                        public void run() {
                            resultCallback.accept(Boolean.valueOf(false));
                        }
                    });
                    return;
                }
                return;
            }
        }
        IRecentsAnimationRunner runner = null;
        if (animationHandler != null) {
            runner = new IRecentsAnimationRunner.Stub() {
                public void onAnimationStart(IRecentsAnimationController controller, RemoteAnimationTarget[] apps, Rect homeContentInsets, Rect minimizedHomeBounds) {
                    animationHandler.onAnimationStart(new RecentsAnimationControllerCompat(controller), RemoteAnimationTargetCompat.wrap(apps), homeContentInsets, minimizedHomeBounds);
                }

                public void onAnimationCanceled() {
                    animationHandler.onAnimationCanceled();
                }
            };
        }
        ActivityManager.getService().startRecentsActivity(intent, e, runner);
        if (resultCallback != null) {
            resultCallbackHandler.post(new Runnable() {
                public void run() {
                    resultCallback.accept(Boolean.valueOf(true));
                }
            });
        }
    }

    public void cancelRecentsAnimation(boolean restoreHomeStackPosition) {
        try {
            ActivityManager.getService().cancelRecentsAnimation(restoreHomeStackPosition);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to cancel recents animation", e);
        }
    }

    public void startActivityFromRecentsAsync(TaskKey taskKey, ActivityOptions options, Consumer<Boolean> resultCallback, Handler resultCallbackHandler) {
        startActivityFromRecentsAsync(taskKey, options, 0, 0, resultCallback, resultCallbackHandler);
    }

    public void startActivityFromRecentsAsync(TaskKey taskKey, ActivityOptions options, int windowingMode, int activityType, Consumer<Boolean> resultCallback, Handler resultCallbackHandler) {
        if (taskKey.windowingMode == 3) {
            if (options == null) {
                options = ActivityOptions.makeBasic();
            }
            options.setLaunchWindowingMode(4);
        } else if (!(windowingMode == 0 && activityType == 0)) {
            if (options == null) {
                options = ActivityOptions.makeBasic();
            }
            options.setLaunchWindowingMode(windowingMode);
            options.setLaunchActivityType(activityType);
        }
        final ActivityOptions finalOptions = options;
        final TaskKey taskKey2 = taskKey;
        final Consumer<Boolean> consumer = resultCallback;
        final Handler handler = resultCallbackHandler;
        this.mBackgroundExecutor.submit(new Runnable() {
            public void run() {
                boolean result = false;
                try {
                    result = ActivityManagerWrapper.this.startActivityFromRecents(taskKey2.id, finalOptions);
                } catch (Exception e) {
                }
                final boolean finalResult = result;
                if (consumer != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            consumer.accept(Boolean.valueOf(finalResult));
                        }
                    });
                }
            }
        });
    }

    public boolean startActivityFromRecents(int taskId, ActivityOptions options) {
        Bundle optsBundle;
        if (options == null) {
            optsBundle = null;
        } else {
            try {
                optsBundle = options.toBundle();
            } catch (Exception e) {
                return false;
            }
        }
        ActivityManager.getService().startActivityFromRecents(taskId, optsBundle);
        return true;
    }

    public void registerTaskStackListener(TaskStackChangeListener listener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.addListener(ActivityManager.getService(), listener);
        }
    }

    public void unregisterTaskStackListener(TaskStackChangeListener listener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.removeListener(listener);
        }
    }

    public void closeSystemWindows(final String reason) {
        this.mBackgroundExecutor.submit(new Runnable() {
            public void run() {
                try {
                    ActivityManager.getService().closeSystemDialogs(reason);
                } catch (RemoteException e) {
                    Log.w(ActivityManagerWrapper.TAG, "Failed to close system windows", e);
                }
            }
        });
    }

    public void removeTask(final int taskId) {
        this.mBackgroundExecutor.submit(new Runnable() {
            public void run() {
                try {
                    ActivityManager.getService().removeTask(taskId);
                } catch (RemoteException e) {
                    String str = ActivityManagerWrapper.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to remove task=");
                    stringBuilder.append(taskId);
                    Log.w(str, stringBuilder.toString(), e);
                }
            }
        });
    }

    public void cancelWindowTransition(int taskId) {
        try {
            ActivityManager.getService().cancelTaskWindowTransition(taskId);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to cancel window transition for task=");
            stringBuilder.append(taskId);
            Log.w(str, stringBuilder.toString(), e);
        }
    }

    public boolean isScreenPinningActive() {
        boolean z = false;
        try {
            if (ActivityManager.getService().getLockTaskModeState() == 2) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isScreenPinningEnabled() {
        return System.getInt(AppGlobals.getInitialApplication().getContentResolver(), "lock_to_app_enabled", 0) != 0;
    }

    public boolean isLockToAppActive() {
        boolean z = false;
        try {
            if (ActivityManager.getService().getLockTaskModeState() != 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean showVoiceSession(IBinder token, Bundle args, int flags) {
        IVoiceInteractionManagerService service = IVoiceInteractionManagerService.Stub.asInterface(ServiceManager.getService("voiceinteraction"));
        if (service == null) {
            return false;
        }
        try {
            return service.showSessionFromSession(token, args, flags);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void removeHwTask(final Task task) {
        this.mBackgroundExecutor.submit(new Runnable() {
            public void run() {
                HwRecentsTaskUtils.sendRemoveTaskToSystemManager(ActivityManagerWrapper.this.mContext, task);
            }
        });
    }

    public void removeHwAllTask() {
        this.mBackgroundExecutor.submit(new Runnable() {
            public void run() {
                HwRecentsTaskUtils.sendRemoveAllTask(ActivityManagerWrapper.this.mContext);
            }
        });
    }

    public ThumbnailData getTaskThumbnailEx(boolean refresh) {
        HwTaskSnapshotWrapper wrapper = HwWindowManager.getForegroundTaskSnapshotWrapper(refresh);
        TaskSnapshot snapshot = null;
        if (wrapper != null) {
            snapshot = wrapper.getTaskSnapshot();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTaskThumbnailEx HwWindowManager snapshot =");
        stringBuilder.append(snapshot);
        stringBuilder.append(",refresh=");
        stringBuilder.append(refresh);
        Log.w(str, stringBuilder.toString());
        if (snapshot != null) {
            return new ThumbnailData(snapshot);
        }
        return new ThumbnailData();
    }
}
