package com.android.server.am;

public class ActivityManagerServiceEx {
    private ActivityManagerService mAms;

    public ActivityManagerService getActivityManagerService() {
        return this.mAms;
    }

    public void setActivityManagerService(ActivityManagerService activityManagerService) {
        this.mAms = activityManagerService;
    }

    public HwActivityManagerService switchToHwActivityManagerService() {
        HwActivityManagerService hwActivityManagerService = this.mAms;
        if (hwActivityManagerService == null || !(hwActivityManagerService instanceof HwActivityManagerService)) {
            return null;
        }
        return hwActivityManagerService;
    }
}
