package com.android.server;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryService$D1kwd7L7yyqN5niz3KWkTepVmUk implements Runnable {
    private final /* synthetic */ BatteryService f$0;

    public /* synthetic */ -$$Lambda$BatteryService$D1kwd7L7yyqN5niz3KWkTepVmUk(BatteryService batteryService) {
        this.f$0 = batteryService;
    }

    public final void run() {
        this.f$0.sendEnqueuedBatteryLevelChangedEvents();
    }
}
