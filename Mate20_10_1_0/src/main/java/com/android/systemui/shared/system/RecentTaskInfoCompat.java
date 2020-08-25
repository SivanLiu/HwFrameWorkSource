package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.util.Log;

public class RecentTaskInfoCompat {
    private ActivityManager.RecentTaskInfo mInfo;

    public RecentTaskInfoCompat(ActivityManager.RecentTaskInfo info) {
        this.mInfo = info;
    }

    public int getUserId() {
        return this.mInfo.userId;
    }

    public boolean supportsSplitScreenMultiWindow() {
        boolean isSupport = this.mInfo.supportsSplitScreenMultiWindow;
        try {
            return this.mInfo.supportsHwSplitScreenMultiWindow;
        } catch (NoSuchFieldError e) {
            Log.e("RecentTaskInfoCompat", "no supportsHwSplitScreenMultiWindow");
            return isSupport;
        }
    }

    public ComponentName getTopActivity() {
        return this.mInfo.topActivity;
    }

    public ActivityManager.TaskDescription getTaskDescription() {
        return this.mInfo.taskDescription;
    }
}
