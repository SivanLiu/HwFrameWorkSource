package com.android.server.stats;

import com.android.internal.os.KernelUidCpuActiveTimeReader.Callback;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StatsCompanionService$jXfS7_WmvALP_3l6Dg3O1qMWGdk implements Callback {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;

    public /* synthetic */ -$$Lambda$StatsCompanionService$jXfS7_WmvALP_3l6Dg3O1qMWGdk(long j, int i, List list) {
        this.f$0 = j;
        this.f$1 = i;
        this.f$2 = list;
    }

    public final void onUidCpuActiveTime(int i, long j) {
        StatsCompanionService.lambda$pullKernelUidCpuActiveTime$3(this.f$0, this.f$1, this.f$2, i, j);
    }
}
