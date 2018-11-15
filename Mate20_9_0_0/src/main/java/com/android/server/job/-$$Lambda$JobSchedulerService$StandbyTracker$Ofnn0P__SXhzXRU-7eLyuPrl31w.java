package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$StandbyTracker$Ofnn0P__SXhzXRU-7eLyuPrl31w implements Consumer {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$JobSchedulerService$StandbyTracker$Ofnn0P__SXhzXRU-7eLyuPrl31w(String str, int i) {
        this.f$0 = str;
        this.f$1 = i;
    }

    public final void accept(Object obj) {
        StandbyTracker.lambda$onAppIdleStateChanged$0(this.f$0, this.f$1, (JobStatus) obj);
    }
}
