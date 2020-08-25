package com.android.server.wm;

import android.app.TaskInfo;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.HwMwUtils;
import com.huawei.server.wm.IHwTaskRecordEx;
import java.util.ArrayList;
import java.util.Locale;

public class HwTaskRecordEx implements IHwTaskRecordEx {
    public static final String TAG = "HwActivityRecordEx";

    public void forceNewConfigWhenReuseActivity(ArrayList<ActivityRecord> mActivities) {
        for (int activityNdx = mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            mActivities.get(activityNdx).forceNewConfig = true;
        }
    }

    public void updateMagicWindowTaskInfo(TaskRecord taskRecord, TaskInfo info) {
        int[] splitScreenTaskIds;
        char c;
        int i;
        if (taskRecord.mStack != null && info != null && HwMwUtils.ENABLED && taskRecord.mStack.inHwMagicWindowingMode() && (splitScreenTaskIds = HwMagicWinCombineManager.getInstance().getForegroundTaskIds(HwMagicWinCombineManager.getInstance().getTaskPackageName(taskRecord), taskRecord.userId)) != null) {
            char c2 = 0;
            boolean isCurrentRTL = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
            if (isCurrentRTL) {
                c = 1;
            } else {
                c = 0;
            }
            int primaryTaskId = splitScreenTaskIds[c];
            if (!isCurrentRTL) {
                c2 = 1;
            }
            int seconrdaryTaskId = splitScreenTaskIds[c2];
            if (taskRecord.taskId == primaryTaskId || taskRecord.taskId == seconrdaryTaskId) {
                info.combinedTaskIds = splitScreenTaskIds;
                if (taskRecord.taskId == primaryTaskId) {
                    i = 100;
                } else {
                    i = 101;
                }
                info.windowMode = i;
                info.bounds = new Rect(taskRecord.mStack.getBounds());
                info.supportsSplitScreenMultiWindow = true;
                info.configuration.windowConfiguration.setWindowingMode(info.windowMode);
            }
        }
    }
}
