package com.android.server.job.controllers;

import android.app.AlarmManager.OnAlarmListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IdleController$IdlenessTracker$nTdS-lGBXcES5VWKcJFmQFgU7IU implements OnAlarmListener {
    private final /* synthetic */ IdlenessTracker f$0;

    public /* synthetic */ -$$Lambda$IdleController$IdlenessTracker$nTdS-lGBXcES5VWKcJFmQFgU7IU(IdlenessTracker idlenessTracker) {
        this.f$0 = idlenessTracker;
    }

    public final void onAlarm() {
        this.f$0.handleIdleTrigger();
    }
}
