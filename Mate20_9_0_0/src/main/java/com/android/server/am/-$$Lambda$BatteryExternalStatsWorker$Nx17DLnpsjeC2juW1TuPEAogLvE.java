package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryExternalStatsWorker$Nx17DLnpsjeC2juW1TuPEAogLvE implements Runnable {
    private final /* synthetic */ BatteryExternalStatsWorker f$0;

    public /* synthetic */ -$$Lambda$BatteryExternalStatsWorker$Nx17DLnpsjeC2juW1TuPEAogLvE(BatteryExternalStatsWorker batteryExternalStatsWorker) {
        this.f$0 = batteryExternalStatsWorker;
    }

    public final void run() {
        this.f$0.mStats.postBatteryNeedsCpuUpdateMsg();
    }
}
