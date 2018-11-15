package com.android.server.stats;

import com.android.internal.os.KernelUidCpuFreqTimeReader.Callback;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StatsCompanionService$YYl2ZbOgrYdj7ixbv8BOJznEAbA implements Callback {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;

    public /* synthetic */ -$$Lambda$StatsCompanionService$YYl2ZbOgrYdj7ixbv8BOJznEAbA(long j, int i, List list) {
        this.f$0 = j;
        this.f$1 = i;
        this.f$2 = list;
    }

    public final void onUidCpuFreqTime(int i, long[] jArr) {
        StatsCompanionService.lambda$pullKernelUidCpuFreqTime$1(this.f$0, this.f$1, this.f$2, i, jArr);
    }
}
