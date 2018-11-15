package com.android.server.power.batterysaver;

import com.android.server.EventLogTags;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatterySaverStateMachine$SSfmWJrD4RBoVg8A8loZrS-jhAo implements Runnable {
    private final /* synthetic */ BatterySaverStateMachine f$0;

    public /* synthetic */ -$$Lambda$BatterySaverStateMachine$SSfmWJrD4RBoVg8A8loZrS-jhAo(BatterySaverStateMachine batterySaverStateMachine) {
        this.f$0 = batterySaverStateMachine;
    }

    public final void run() {
        EventLogTags.writeBatterySaverSetting(this.f$0.mSettingBatterySaverTriggerThreshold);
    }
}
