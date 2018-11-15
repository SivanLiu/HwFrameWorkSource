package com.android.systemui.shared.recents.model;

import android.app.ActivityManager.RecentTaskInfo;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import com.android.systemui.shared.recents.model.Task.TaskKey;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecentsTaskLoadPlan {
    private static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    private static final boolean IS_NOVA_PERFORMANCE = SystemProperties.getBoolean("ro.config.hw_nova_performance", false);
    private static final int MAX_RECENT_TASK_EMUI = 20;
    private static final int MAX_RECENT_TASK_EMUI_LITE = 15;
    public static final String TAG = "RecentsTaskLoadPlan";
    private final Context mContext;
    private final KeyguardManager mKeyguardManager;
    private List<RecentTaskInfo> mRawTasks;
    private TaskStack mStack;
    private final SparseBooleanArray mTmpLockedUsers = new SparseBooleanArray();

    public static class Options {
        public boolean loadIcons = true;
        public boolean loadThumbnails = false;
        public int numVisibleTaskThumbnails = 0;
        public int numVisibleTasks = 0;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int runningTaskId = -1;
    }

    public static class PreloadOptions {
        public boolean loadTitles = true;
    }

    public static int getMaxRecentTasks() {
        if (isEmuiLite()) {
            return 15;
        }
        return 20;
    }

    public RecentsTaskLoadPlan(Context context) {
        this.mContext = context;
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
    }

    public void preloadPlan(PreloadOptions opts, RecentsTaskLoader loader, int runningTaskId, int currentUserId) {
        int i;
        int taskCount;
        PreloadOptions preloadOptions = opts;
        RecentsTaskLoader recentsTaskLoader = loader;
        Map<String, Boolean> map = HwRecentsTaskUtils.refreshToCache(this.mContext);
        HwRecentsTaskUtils.refreshPlayingMusicUidSet();
        Resources res = this.mContext.getResources();
        List allTasks = new ArrayList();
        if (this.mRawTasks == null) {
            this.mRawTasks = ActivityManagerWrapper.getInstance().getRecentTasks(getMaxRecentTasks(), currentUserId);
            Collections.reverse(this.mRawTasks);
        } else {
            i = currentUserId;
        }
        int taskCount2 = this.mRawTasks.size();
        int i2 = 0;
        while (i2 < taskCount2) {
            Resources res2;
            int i3;
            RecentTaskInfo t = (RecentTaskInfo) this.mRawTasks.get(i2);
            int windowingMode = t.configuration.windowConfiguration.getWindowingMode();
            TaskKey taskKey = new TaskKey(t.persistentId, windowingMode, t.baseIntent, t.userId, t.lastActiveTime);
            boolean isFreeformTask = windowingMode == 5;
            boolean isStackTask = !isFreeformTask;
            boolean isLaunchTarget = taskKey.id == runningTaskId;
            ActivityInfo info = recentsTaskLoader.getAndUpdateActivityInfo(taskKey);
            if (info == null) {
                res2 = res;
                taskCount = taskCount2;
                i3 = i2;
            } else {
                String andUpdateActivityTitle;
                boolean z;
                Drawable andUpdateActivityIcon;
                if (preloadOptions.loadTitles) {
                    andUpdateActivityTitle = recentsTaskLoader.getAndUpdateActivityTitle(taskKey, t.taskDescription);
                } else {
                    andUpdateActivityTitle = "";
                }
                String title = andUpdateActivityTitle;
                if (preloadOptions.loadTitles) {
                    andUpdateActivityTitle = recentsTaskLoader.getAndUpdateContentDescription(taskKey, t.taskDescription);
                } else {
                    andUpdateActivityTitle = "";
                }
                String titleDescription = andUpdateActivityTitle;
                if (isStackTask) {
                    z = false;
                    andUpdateActivityIcon = recentsTaskLoader.getAndUpdateActivityIcon(taskKey, t.taskDescription, false);
                } else {
                    z = false;
                    andUpdateActivityIcon = null;
                }
                Drawable icon = andUpdateActivityIcon;
                ThumbnailData thumbnail = recentsTaskLoader.getAndUpdateThumbnail(taskKey, z, z);
                int activityColor = recentsTaskLoader.getActivityPrimaryColor(t.taskDescription);
                res2 = res;
                res = recentsTaskLoader.getActivityBackgroundColor(t.taskDescription);
                boolean isSystemApp = (info == null || (info.applicationInfo.flags & 1) == 0) ? false : true;
                taskCount = taskCount2;
                if (this.mTmpLockedUsers.indexOfKey(t.userId) < 0) {
                    this.mTmpLockedUsers.put(t.userId, this.mKeyguardManager.isDeviceLocked(t.userId));
                } else {
                    int i4 = windowingMode;
                }
                boolean isLocked = this.mTmpLockedUsers.get(t.userId);
                String packageName = info != null ? info.packageName : "";
                i3 = i2;
                Task task = new Task(taskKey, icon, thumbnail, title, titleDescription, activityColor, res, isLaunchTarget, isStackTask, isSystemApp, t.supportsSplitScreenMultiWindow, t.taskDescription, t.resizeMode, t.topActivity, isLocked);
                task.setPakcageName(packageName);
                if (!shouldSkipLoadTask(map, t, task)) {
                    allTasks.add(task);
                }
            }
            i2 = i3 + 1;
            res = res2;
            taskCount2 = taskCount;
            preloadOptions = opts;
            recentsTaskLoader = loader;
            i = currentUserId;
        }
        taskCount = taskCount2;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("to show tasks size is ");
        stringBuilder.append(allTasks.size());
        Log.i(str, stringBuilder.toString());
        this.mStack = new TaskStack();
        this.mStack.setTasks(allTasks, false);
    }

    public void executePlan(Options opts, RecentsTaskLoader loader) {
        Resources res = this.mContext.getResources();
        ArrayList<Task> tasks = this.mStack.getTasks();
        int taskCount = tasks.size();
        int i = 0;
        while (i < taskCount) {
            Task task = (Task) tasks.get(i);
            TaskKey taskKey = task.key;
            boolean isRunningTask = task.key.id == opts.runningTaskId;
            boolean isVisibleTask = i >= taskCount - opts.numVisibleTasks;
            boolean isVisibleThumbnail = i >= taskCount - opts.numVisibleTaskThumbnails;
            if (!opts.onlyLoadPausedActivities || !isRunningTask) {
                if (opts.loadIcons && ((isRunningTask || isVisibleTask) && task.icon == null)) {
                    task.icon = loader.getAndUpdateActivityIcon(taskKey, task.taskDescription, true);
                }
                if (opts.loadThumbnails && isVisibleThumbnail) {
                    task.thumbnail = loader.getAndUpdateThumbnail(taskKey, true, true);
                }
            }
            i++;
        }
    }

    public TaskStack getTaskStack() {
        return this.mStack;
    }

    public boolean hasTasks() {
        boolean z = false;
        if (this.mStack == null) {
            return false;
        }
        if (this.mStack.getTaskCount() > 0) {
            z = true;
        }
        return z;
    }

    private boolean shouldSkipLoadTask(Map<String, Boolean> lockMap, RecentTaskInfo recentInfo, Task task) {
        if (!HwRecentsTaskUtils.willRemovedTask(recentInfo)) {
            return false;
        }
        StringBuilder stringBuilder;
        if (lockMap.get(task.packageName) == null ? false : ((Boolean) lockMap.get(task.packageName)).booleanValue()) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(task.packageName);
            stringBuilder.append("is locked, so need load it");
            Log.d(str, stringBuilder.toString());
            return false;
        } else if (HwRecentsTaskUtils.getPlayingMusicUid(this.mContext, task)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(task.packageName);
            stringBuilder2.append("is music, so need load it");
            Log.d(str2, stringBuilder2.toString());
            return false;
        } else {
            String str3 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("in removing, will remove task: ");
            stringBuilder.append(task.packageName);
            Log.i(str3, stringBuilder.toString());
            return true;
        }
    }

    public static boolean isEmuiLite() {
        return IS_EMUI_LITE || IS_NOVA_PERFORMANCE;
    }
}
