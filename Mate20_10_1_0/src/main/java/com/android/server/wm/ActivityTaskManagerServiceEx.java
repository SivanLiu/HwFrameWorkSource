package com.android.server.wm;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Looper;

public class ActivityTaskManagerServiceEx {
    private ActivityTaskManagerService mATMS;

    public ActivityTaskManagerService getActivityTaskManagerService() {
        return this.mATMS;
    }

    public void setActivityTaskManagerService(ActivityTaskManagerService activityTaskManagerService) {
        this.mATMS = activityTaskManagerService;
    }

    public Context getContext() {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null) {
            return null;
        }
        return activityTaskManagerService.mContext;
    }

    public TaskRecordEx anyTaskForId(int taskId) {
        TaskRecord taskRecord;
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mStackSupervisor == null || this.mATMS.mStackSupervisor.mRootActivityContainer == null || (taskRecord = this.mATMS.mStackSupervisor.mRootActivityContainer.anyTaskForId(taskId)) == null) {
            return null;
        }
        TaskRecordEx taskRecordEx = new TaskRecordEx();
        taskRecordEx.setTaskRecord(taskRecord);
        return taskRecordEx;
    }

    public DisplayContentEx getDisplayContentEx(int displayId) {
        DisplayContent displayContent;
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mWindowManager == null || this.mATMS.mWindowManager.getRoot() == null || (displayContent = this.mATMS.mWindowManager.getRoot().getDisplayContent(displayId)) == null) {
            return null;
        }
        DisplayContentEx displayContentEx = new DisplayContentEx();
        displayContentEx.setDisplayContent(displayContent);
        return displayContentEx;
    }

    public boolean isHwWindowManagerService() {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mWindowManager == null) {
            return false;
        }
        return this.mATMS.mWindowManager instanceof HwWindowManagerService;
    }

    public DisplayManager getDisplayManager() {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mWindowManager == null || !(this.mATMS.mWindowManager instanceof HwWindowManagerService)) {
            return null;
        }
        return this.mATMS.mWindowManager.getDisplayManager();
    }

    public Looper getLooper() {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mH == null) {
            return null;
        }
        return this.mATMS.mH.getLooper();
    }

    public void moveTaskBackwards(int toBackTaskId) {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService != null && activityTaskManagerService.mHwATMSEx != null) {
            this.mATMS.mHwATMSEx.moveTaskBackwards(toBackTaskId);
        }
    }

    public Object getGlobalLock() {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null) {
            return new Object();
        }
        return activityTaskManagerService.mGlobalLock;
    }

    public ActivityDisplayEx getActivityDisplayEx(int displayId) {
        ActivityDisplay activityDisplay;
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mRootActivityContainer == null || (activityDisplay = this.mATMS.mRootActivityContainer.getActivityDisplay(displayId)) == null) {
            return null;
        }
        ActivityDisplayEx activityDisplayEx = new ActivityDisplayEx();
        activityDisplayEx.setActivityDisplay(activityDisplay);
        return activityDisplayEx;
    }

    public ActivityDisplayEx getActivityDisplayExFromStackSupervisorByIndex(int index) {
        ActivityDisplay activityDisplay;
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mStackSupervisor == null || this.mATMS.mStackSupervisor.mRootActivityContainer == null || (activityDisplay = this.mATMS.mStackSupervisor.mRootActivityContainer.getChildAt(index)) == null) {
            return null;
        }
        ActivityDisplayEx activityDisplayEx = new ActivityDisplayEx();
        activityDisplayEx.setActivityDisplay(activityDisplay);
        return activityDisplayEx;
    }

    public int getSizeOfActivityDisplayFromStackSupervisor() {
        ActivityTaskManagerService activityTaskManagerService = this.mATMS;
        if (activityTaskManagerService == null || activityTaskManagerService.mStackSupervisor == null || this.mATMS.mStackSupervisor.mRootActivityContainer == null) {
            return 0;
        }
        return this.mATMS.mStackSupervisor.mRootActivityContainer.getChildCount();
    }
}
