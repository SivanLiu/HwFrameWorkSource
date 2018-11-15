package com.android.server.usage;

import android.util.ArraySet;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserUsageStatsService$aWxPyFEggMep-oyju6mPXDEUesw implements StatCombiner {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ long f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ ArraySet f$3;

    public /* synthetic */ -$$Lambda$UserUsageStatsService$aWxPyFEggMep-oyju6mPXDEUesw(long j, long j2, String str, ArraySet arraySet) {
        this.f$0 = j;
        this.f$1 = j2;
        this.f$2 = str;
        this.f$3 = arraySet;
    }

    public final void combine(IntervalStats intervalStats, boolean z, List list) {
        UserUsageStatsService.lambda$queryEventsForPackage$0(this.f$0, this.f$1, this.f$2, this.f$3, intervalStats, z, list);
    }
}
