package com.android.server.job;

import com.android.server.job.JobStore.AnonymousClass1;
import com.android.server.job.controllers.JobStatus;
import java.util.List;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobStore$1$Wgepg1oHZp0-Q01q1baIVZKWujU implements Consumer {
    private final /* synthetic */ List f$0;

    public /* synthetic */ -$$Lambda$JobStore$1$Wgepg1oHZp0-Q01q1baIVZKWujU(List list) {
        this.f$0 = list;
    }

    public final void accept(Object obj) {
        AnonymousClass1.lambda$run$0(this.f$0, (JobStatus) obj);
    }
}
