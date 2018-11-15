package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobStore$apkqpwp0Wau6LvC-MCNG2GidMkM implements Consumer {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ ArrayList f$1;
    private final /* synthetic */ ArrayList f$2;

    public /* synthetic */ -$$Lambda$JobStore$apkqpwp0Wau6LvC-MCNG2GidMkM(long j, ArrayList arrayList, ArrayList arrayList2) {
        this.f$0 = j;
        this.f$1 = arrayList;
        this.f$2 = arrayList2;
    }

    public final void accept(Object obj) {
        JobStore.lambda$getRtcCorrectedJobsLocked$0(this.f$0, this.f$1, this.f$2, (JobStatus) obj);
    }
}
