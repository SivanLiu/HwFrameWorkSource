package com.android.server.am;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo.WindowLayout;
import android.graphics.Rect;

public class ActivityLaunchParamsModifier implements LaunchParamsModifier {
    private final ActivityStackSupervisor mSupervisor;

    ActivityLaunchParamsModifier(ActivityStackSupervisor activityStackSupervisor) {
        this.mSupervisor = activityStackSupervisor;
    }

    public int onCalculate(TaskRecord task, WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, LaunchParams currentParams, LaunchParams outParams) {
        if (activity == null || !this.mSupervisor.canUseActivityOptionsLaunchBounds(options) || (!activity.isResizeable() && (task == null || !task.isResizeable()))) {
            return 0;
        }
        Rect bounds = options.getLaunchBounds();
        if (bounds == null || bounds.isEmpty()) {
            return 0;
        }
        outParams.mBounds.set(bounds);
        return 1;
    }
}
