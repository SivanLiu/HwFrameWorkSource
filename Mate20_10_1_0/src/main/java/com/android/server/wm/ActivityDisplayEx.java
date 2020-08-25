package com.android.server.wm;

public class ActivityDisplayEx {
    private ActivityDisplay mActivityDisplay;

    public ActivityDisplay getActivityDisplay() {
        return this.mActivityDisplay;
    }

    public void setActivityDisplay(ActivityDisplay activityDisplay) {
        this.mActivityDisplay = activityDisplay;
    }

    public int getChildCount() {
        ActivityDisplay activityDisplay = this.mActivityDisplay;
        if (activityDisplay == null) {
            return 0;
        }
        return activityDisplay.getChildCount();
    }

    public ActivityStackEx getChildAt(int index) {
        ActivityDisplay activityDisplay = this.mActivityDisplay;
        if (activityDisplay == null || activityDisplay.getChildAt(index) == null) {
            return null;
        }
        ActivityStackEx activityStackEx = new ActivityStackEx();
        activityStackEx.setActivityStack(this.mActivityDisplay.getChildAt(index));
        return activityStackEx;
    }
}
