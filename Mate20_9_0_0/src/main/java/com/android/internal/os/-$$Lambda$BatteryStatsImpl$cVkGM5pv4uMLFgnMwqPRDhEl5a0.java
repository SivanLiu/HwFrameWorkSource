package com.android.internal.os;

import android.util.SparseLongArray;
import com.android.internal.os.KernelUidCpuTimeReader.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryStatsImpl$cVkGM5pv4uMLFgnMwqPRDhEl5a0 implements Callback {
    private final /* synthetic */ BatteryStatsImpl f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ SparseLongArray f$3;

    public /* synthetic */ -$$Lambda$BatteryStatsImpl$cVkGM5pv4uMLFgnMwqPRDhEl5a0(BatteryStatsImpl batteryStatsImpl, int i, boolean z, SparseLongArray sparseLongArray) {
        this.f$0 = batteryStatsImpl;
        this.f$1 = i;
        this.f$2 = z;
        this.f$3 = sparseLongArray;
    }

    public final void onUidCpuTime(int i, long j, long j2) {
        BatteryStatsImpl.lambda$readKernelUidCpuTimesLocked$0(this.f$0, this.f$1, this.f$2, this.f$3, i, j, j2);
    }
}
