package com.android.server.am;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.WindowConfiguration.ActivityType;
import android.app.WindowConfiguration.WindowingMode;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

class RunningTasks {
    private static final Comparator<TaskRecord> LAST_ACTIVE_TIME_COMPARATOR = -$$Lambda$RunningTasks$BGar3HlUsTw-0HzSmfkEWly0moY.INSTANCE;
    private final TaskActivitiesReport mTmpReport = new TaskActivitiesReport();
    private final TreeSet<TaskRecord> mTmpSortedSet = new TreeSet(LAST_ACTIVE_TIME_COMPARATOR);
    private final ArrayList<TaskRecord> mTmpStackTasks = new ArrayList();

    RunningTasks() {
    }

    void getTasks(int maxNum, List<RunningTaskInfo> list, @ActivityType int ignoreActivityType, @WindowingMode int ignoreWindowingMode, SparseArray<ActivityDisplay> activityDisplays, int callingUid, boolean allowed) {
        if (maxNum > 0) {
            this.mTmpSortedSet.clear();
            int numDisplays = activityDisplays.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                ActivityDisplay display = (ActivityDisplay) activityDisplays.valueAt(displayNdx);
                for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    ActivityStack stack = display.getChildAt(stackNdx);
                    this.mTmpStackTasks.clear();
                    stack.getRunningTasks(this.mTmpStackTasks, ignoreActivityType, ignoreWindowingMode, callingUid, allowed);
                    this.mTmpSortedSet.addAll(this.mTmpStackTasks);
                }
            }
            SparseArray<ActivityDisplay> sparseArray = activityDisplays;
            Iterator<TaskRecord> iter = this.mTmpSortedSet.iterator();
            int maxNum2 = maxNum;
            while (iter.hasNext() && maxNum2 != 0) {
                list.add(createRunningTaskInfo((TaskRecord) iter.next()));
                maxNum2--;
            }
            List<RunningTaskInfo> list2 = list;
        }
    }

    private RunningTaskInfo createRunningTaskInfo(TaskRecord task) {
        task.getNumRunningActivities(this.mTmpReport);
        RunningTaskInfo ci = new RunningTaskInfo();
        ci.id = task.taskId;
        ci.stackId = task.getStackId();
        ci.baseActivity = this.mTmpReport.base.intent.getComponent();
        ci.topActivity = this.mTmpReport.top.intent.getComponent();
        ci.lastActiveTime = task.lastActiveTime;
        ci.description = task.lastDescription;
        ci.numActivities = this.mTmpReport.numActivities;
        ci.numRunning = this.mTmpReport.numRunning;
        ci.supportsSplitScreenMultiWindow = task.supportsSplitScreenWindowingMode();
        ci.resizeMode = task.mResizeMode;
        ci.configuration.setTo(task.getConfiguration());
        return ci;
    }
}
