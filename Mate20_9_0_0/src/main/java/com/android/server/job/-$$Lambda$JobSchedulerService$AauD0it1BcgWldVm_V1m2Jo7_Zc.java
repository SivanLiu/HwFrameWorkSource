package com.android.server.job;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$AauD0it1BcgWldVm_V1m2Jo7_Zc implements Predicate {
    private final /* synthetic */ JobSchedulerService f$0;

    public /* synthetic */ -$$Lambda$JobSchedulerService$AauD0it1BcgWldVm_V1m2Jo7_Zc(JobSchedulerService jobSchedulerService) {
        this.f$0 = jobSchedulerService;
    }

    public final boolean test(Object obj) {
        return this.f$0.isUidActive(((Integer) obj).intValue());
    }
}
