package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.List;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$LocalService$yaChpLJ2odu2Fk7A6H8erUndrN8 implements Consumer {
    private final /* synthetic */ LocalService f$0;
    private final /* synthetic */ List f$1;

    public /* synthetic */ -$$Lambda$JobSchedulerService$LocalService$yaChpLJ2odu2Fk7A6H8erUndrN8(LocalService localService, List list) {
        this.f$0 = localService;
        this.f$1 = list;
    }

    public final void accept(Object obj) {
        LocalService.lambda$getSystemScheduledPendingJobs$0(this.f$0, this.f$1, (JobStatus) obj);
    }
}
