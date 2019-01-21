package com.android.internal.os;

import com.android.internal.os.KernelUidCpuClusterTimeReader.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryStatsImpl$WJBQdQHGlhcwV7yfM8vNEWWvVp0 implements Callback {
    private final /* synthetic */ BatteryStatsImpl f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$BatteryStatsImpl$WJBQdQHGlhcwV7yfM8vNEWWvVp0(BatteryStatsImpl batteryStatsImpl, boolean z) {
        this.f$0 = batteryStatsImpl;
        this.f$1 = z;
    }

    public final void onUidCpuPolicyTime(int i, long[] jArr) {
        BatteryStatsImpl.lambda$readKernelUidCpuClusterTimesLocked$3(this.f$0, this.f$1, i, jArr);
    }
}
