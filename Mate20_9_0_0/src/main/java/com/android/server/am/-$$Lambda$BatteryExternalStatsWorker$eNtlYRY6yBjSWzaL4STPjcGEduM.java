package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryExternalStatsWorker$eNtlYRY6yBjSWzaL4STPjcGEduM implements Runnable {
    private final /* synthetic */ BatteryExternalStatsWorker f$0;

    public /* synthetic */ -$$Lambda$BatteryExternalStatsWorker$eNtlYRY6yBjSWzaL4STPjcGEduM(BatteryExternalStatsWorker batteryExternalStatsWorker) {
        this.f$0 = batteryExternalStatsWorker;
    }

    public final void run() {
        this.f$0.scheduleSync("battery-level", 31);
    }
}
