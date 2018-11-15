package com.android.server.stats;

import com.android.internal.os.KernelUidCpuClusterTimeReader.Callback;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StatsCompanionService$HnKmFmrhuaLvGqFujHXRVkF_MsY implements Callback {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;

    public /* synthetic */ -$$Lambda$StatsCompanionService$HnKmFmrhuaLvGqFujHXRVkF_MsY(long j, int i, List list) {
        this.f$0 = j;
        this.f$1 = i;
        this.f$2 = list;
    }

    public final void onUidCpuPolicyTime(int i, long[] jArr) {
        StatsCompanionService.lambda$pullKernelUidCpuClusterTime$2(this.f$0, this.f$1, this.f$2, i, jArr);
    }
}
