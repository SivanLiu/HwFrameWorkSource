package com.android.server.wm;

import android.content.Intent;
import android.content.res.Configuration;
import android.freeform.HwFreeFormUtils;
import android.os.SystemProperties;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Slog;
import com.android.server.rms.iaware.cpu.CPUFeature;
import java.util.ArrayList;

public class HwActivityStack extends ActivityStack implements IHwActivityStack {
    private static final boolean IS_HW_NAVI_BAR = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    public boolean isMwNewTaskSplitStack = false;

    public HwActivityStack(ActivityDisplay display, int stackId, ActivityStackSupervisor supervisor, int windowingMode, int activityType, boolean isOnTop) {
        super(display, stackId, supervisor, windowingMode, activityType, isOnTop);
    }

    public int getInvalidFlag(int changes, Configuration newConfig, Configuration naviConfig) {
        int retChanges = changes;
        if (newConfig == null || naviConfig == null || !IS_HW_NAVI_BAR) {
            return retChanges;
        }
        int newChanges = naviConfig.diff(newConfig);
        if ((newChanges & 1280) == 0) {
            return retChanges & -1281;
        }
        if ((newChanges & 128) != 0) {
            if (retChanges == 1280 || retChanges == 1024) {
                retChanges &= -1025;
            }
            return retChanges & -257;
        }
        Slog.i("ActivityTaskManager", "Get invalid flag nothing. newChanges = " + newChanges);
        return retChanges;
    }

    /* access modifiers changed from: package-private */
    public void moveHomeStackTaskToTop() {
    }

    public boolean isSplitActivity(Intent intent) {
        return (intent == null || (intent.getHwFlags() & 4) == 0) ? false : true;
    }

    /* access modifiers changed from: protected */
    public void setKeepPortraitFR() {
        IntelliServiceManager.getInstance(this.mService.mContext).setKeepPortrait(true);
    }

    /* access modifiers changed from: protected */
    public boolean shouldBeVisible(ActivityRecord starting) {
        if (HwPCUtils.isExtDynamicStack(this.mStackId) && ((this.mHwActivityStackEx != null && !this.mHwActivityStackEx.getStackVisible()) || this.mTaskHistory.isEmpty())) {
            return false;
        }
        if (!HwFreeFormUtils.isFreeFormEnable() || !inFreeformWindowingMode()) {
            boolean result = HwActivityStack.super.shouldBeVisible(starting);
            if (inHwMagicWindowingMode() && !result) {
                if (HwMwUtils.performPolicy((int) CPUFeature.MSG_SET_BOOST_CPUS, new Object[]{Integer.valueOf(this.mStackId)}).getBoolean("RESULT_STACK_VISIBILITY", result)) {
                    Slog.i("ActivityTaskManager", " shouldBeVisible : visbile + magicwindow " + this.mStackId);
                    return true;
                }
            }
            return result;
        } else if (!this.mTaskHistory.isEmpty() && getFreeFormStackVisible()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isVisibleLocked(String packageName, boolean isDeepRecur) {
        ArrayList<ActivityRecord> activities;
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        TaskRecord lastTask = null;
        HwTaskRecord hwTask = null;
        int size = this.mTaskHistory.size();
        if (size <= 0) {
            return false;
        }
        int maxTaskIdx = isDeepRecur ? 0 : size - 1;
        for (int taskNdx = size - 1; taskNdx >= maxTaskIdx; taskNdx--) {
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx);
            if (task instanceof HwTaskRecord) {
                hwTask = (HwTaskRecord) task;
            }
            if (!(hwTask == null || (activities = hwTask.getActivities()) == null || activities.size() <= 0)) {
                int numActivities = activities.size();
                for (int activityNdx = 0; activityNdx < numActivities; activityNdx++) {
                    try {
                        ActivityRecord activityRecord = activities.get(activityNdx);
                        if (activityRecord != null && (packageName.equals(activityRecord.packageName) || activityRecord.getTaskRecord() == lastTask)) {
                            if (activityRecord.visible || activityRecord.visibleIgnoringKeyguard) {
                                return true;
                            }
                            lastTask = activityRecord.getTaskRecord();
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Slog.e("ActivityTaskManager", "IndexOutOfBoundsException: Index: +" + activityNdx + ", Size: " + numActivities);
                    }
                }
                continue;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void moveToBack(String reason, TaskRecord task) {
        HwActivityStack.super.moveToBack(reason, task);
        if (inHwMagicWindowingMode()) {
            HwMwUtils.performPolicy(133, new Object[]{Integer.valueOf(this.mStackId)});
        }
    }
}
