package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$e8zIA2HHN2tnGMuc6TZ2xWw_c20 implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$JobSchedulerService$e8zIA2HHN2tnGMuc6TZ2xWw_c20(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return JobSchedulerService.lambda$dumpInternal$3(this.f$0, (JobStatus) obj);
    }
}
