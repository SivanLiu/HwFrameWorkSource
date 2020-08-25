package com.android.server.wm;

import android.content.pm.ActivityInfo;
import android.graphics.Rect;

public class TaskRecordEx {
    private static final int WINDOW_FULLSCREEN = 4;
    private TaskRecord mTaskRecord;

    public void setTaskRecord(TaskRecord taskRecord) {
        this.mTaskRecord = taskRecord;
    }

    public TaskRecord getTaskRecord() {
        return this.mTaskRecord;
    }

    public int getWindowState() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return 4;
        }
        return taskRecord.mWindowState;
    }

    public int getTaskId() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return 0;
        }
        return taskRecord.taskId;
    }

    public boolean isTopActivityEmpty() {
        TaskRecord taskRecord = this.mTaskRecord;
        return taskRecord == null || taskRecord.getTopActivity() == null;
    }

    public void setNextWindowState(int state) {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord != null) {
            taskRecord.mNextWindowState = state;
        }
    }

    public int getNextWindowState() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return 0;
        }
        return taskRecord.mNextWindowState;
    }

    public int getHwTaskRecordWindowState() {
        if (instanceOfHwTaskRecord()) {
            return this.mTaskRecord.getWindowState();
        }
        return 0;
    }

    public Rect getRequestedOverrideBounds() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return null;
        }
        return taskRecord.getRequestedOverrideBounds();
    }

    public boolean instanceOfHwTaskRecord() {
        TaskRecord taskRecord = this.mTaskRecord;
        return taskRecord != null && (taskRecord instanceof HwTaskRecord);
    }

    public void setSaveBounds(boolean isSaveBounds) {
        if (instanceOfHwTaskRecord()) {
            this.mTaskRecord.setSaveBounds(isSaveBounds);
        }
    }

    public boolean isSaveBounds() {
        if (instanceOfHwTaskRecord()) {
            return this.mTaskRecord.isSaveBounds();
        }
        return false;
    }

    public boolean resize(Rect bounds, int resizeMode, boolean preserveWindow, boolean deferResume) {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return false;
        }
        return taskRecord.resize(bounds, resizeMode, preserveWindow, deferResume);
    }

    public ActivityInfo getRootActivityInfo() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return null;
        }
        return taskRecord.mRootActivityInfo;
    }

    public boolean isRootActivityEmpty() {
        TaskRecord taskRecord = this.mTaskRecord;
        return taskRecord == null || taskRecord.getRootActivity() == null;
    }

    public String getPkgNameFromRootActivity() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.getRootActivity() == null) {
            return "";
        }
        return this.mTaskRecord.getRootActivity().packageName;
    }

    public String getPkgNameFromRootActivityInfo() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.mRootActivityInfo == null) {
            return "";
        }
        return this.mTaskRecord.mRootActivityInfo.packageName;
    }

    public int getResizeMode() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return 0;
        }
        return taskRecord.mResizeMode;
    }

    public int getOriginalWindowState() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return 4;
        }
        return taskRecord.mOriginalWindowState;
    }

    public void setOriginalWindowState(int state) {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord != null) {
            taskRecord.mOriginalWindowState = state;
        }
    }

    public int getStackId() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return 0;
        }
        return taskRecord.getStackId();
    }

    public String getPkgNameFromTopActivity() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.getTopActivity() == null) {
            return "";
        }
        return this.mTaskRecord.getTopActivity().packageName;
    }

    public boolean isEmpty() {
        return this.mTaskRecord == null;
    }

    public boolean isVisible() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null) {
            return false;
        }
        return taskRecord.isVisible();
    }

    public int getDisplayChildCount() {
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.mStack == null || this.mTaskRecord.mStack.getDisplay() == null) {
            return 0;
        }
        return this.mTaskRecord.mStack.getDisplay().getChildCount();
    }

    public ActivityStackEx getActivityStackExByIndex(int index) {
        ActivityStack activityStack;
        TaskRecord taskRecord = this.mTaskRecord;
        if (taskRecord == null || taskRecord.mStack == null || this.mTaskRecord.mStack.getDisplay() == null || (activityStack = this.mTaskRecord.mStack.getDisplay().getChildAt(index)) == null) {
            return null;
        }
        ActivityStackEx activityStackEx = new ActivityStackEx();
        activityStackEx.setActivityStack(activityStack);
        return activityStackEx;
    }
}
