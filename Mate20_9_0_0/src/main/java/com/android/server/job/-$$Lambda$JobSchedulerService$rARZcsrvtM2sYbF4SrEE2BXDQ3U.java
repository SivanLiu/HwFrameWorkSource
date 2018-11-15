package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$rARZcsrvtM2sYbF4SrEE2BXDQ3U implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$JobSchedulerService$rARZcsrvtM2sYbF4SrEE2BXDQ3U(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return JobSchedulerService.lambda$dumpInternalProto$4(this.f$0, (JobStatus) obj);
    }
}
