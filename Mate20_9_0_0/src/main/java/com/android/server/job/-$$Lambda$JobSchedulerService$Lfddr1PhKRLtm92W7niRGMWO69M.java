package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$Lfddr1PhKRLtm92W7niRGMWO69M implements Consumer {
    private final /* synthetic */ JobSchedulerService f$0;

    public /* synthetic */ -$$Lambda$JobSchedulerService$Lfddr1PhKRLtm92W7niRGMWO69M(JobSchedulerService jobSchedulerService) {
        this.f$0 = jobSchedulerService;
    }

    public final void accept(Object obj) {
        JobSchedulerService.lambda$onBootPhase$2(this.f$0, (JobStatus) obj);
    }
}
