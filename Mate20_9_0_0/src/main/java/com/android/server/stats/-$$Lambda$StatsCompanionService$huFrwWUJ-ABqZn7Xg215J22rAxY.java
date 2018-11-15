package com.android.server.stats;

import com.android.internal.os.KernelUidCpuTimeReader.Callback;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StatsCompanionService$huFrwWUJ-ABqZn7Xg215J22rAxY implements Callback {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;

    public /* synthetic */ -$$Lambda$StatsCompanionService$huFrwWUJ-ABqZn7Xg215J22rAxY(long j, int i, List list) {
        this.f$0 = j;
        this.f$1 = i;
        this.f$2 = list;
    }

    public final void onUidCpuTime(int i, long j, long j2) {
        StatsCompanionService.lambda$pullKernelUidCpuTime$0(this.f$0, this.f$1, this.f$2, i, j, j2);
    }
}
