package com.android.server.wm;

public class ActivityStackEx {
    private ActivityStack mActivityStack;

    public ActivityStack getActivityStack() {
        return this.mActivityStack;
    }

    public void setActivityStack(ActivityStack activityStack) {
        this.mActivityStack = activityStack;
    }

    public int getDisplayId() {
        ActivityStack activityStack = this.mActivityStack;
        if (activityStack == null) {
            return 0;
        }
        return activityStack.mDisplayId;
    }

    public int getChildCount() {
        ActivityStack activityStack = this.mActivityStack;
        if (activityStack == null) {
            return 0;
        }
        return activityStack.getChildCount();
    }

    public boolean shouldBeVisible(ActivityRecordEx activityRecordEx) {
        ActivityStack activityStack = this.mActivityStack;
        if (activityStack == null) {
            return false;
        }
        return activityStack.shouldBeVisible(activityRecordEx == null ? null : activityRecordEx.getActivityRecord());
    }

    public TaskRecordEx getChildAt(int index) {
        if (this.mActivityStack == null) {
            return null;
        }
        TaskRecordEx taskRecordEx = new TaskRecordEx();
        taskRecordEx.setTaskRecord(this.mActivityStack.getChildAt(index));
        return taskRecordEx;
    }

    public TaskRecordEx topTask() {
        ActivityStack activityStack = this.mActivityStack;
        if (activityStack == null || activityStack.topTask() == null) {
            return null;
        }
        TaskRecordEx taskRecordEx = new TaskRecordEx();
        taskRecordEx.setTaskRecord(this.mActivityStack.topTask());
        return taskRecordEx;
    }
}
