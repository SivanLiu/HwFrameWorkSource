package com.android.internal.os;

import com.android.internal.os.KernelUidCpuActiveTimeReader.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryStatsImpl$mMCK0IbpOZu45KINuNCoRayjoDU implements Callback {
    private final /* synthetic */ BatteryStatsImpl f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$BatteryStatsImpl$mMCK0IbpOZu45KINuNCoRayjoDU(BatteryStatsImpl batteryStatsImpl, boolean z) {
        this.f$0 = batteryStatsImpl;
        this.f$1 = z;
    }

    public final void onUidCpuActiveTime(int i, long j) {
        BatteryStatsImpl.lambda$readKernelUidCpuActiveTimesLocked$2(this.f$0, this.f$1, i, j);
    }
}
