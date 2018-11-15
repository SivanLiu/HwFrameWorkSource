package com.android.server.job.controllers;

import android.util.proto.ProtoOutputStream;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackgroundJobsController$ypgNv91qX_67RP8z3Z9CsC0SRRs implements Consumer {
    private final /* synthetic */ BackgroundJobsController f$0;
    private final /* synthetic */ ProtoOutputStream f$1;

    public /* synthetic */ -$$Lambda$BackgroundJobsController$ypgNv91qX_67RP8z3Z9CsC0SRRs(BackgroundJobsController backgroundJobsController, ProtoOutputStream protoOutputStream) {
        this.f$0 = backgroundJobsController;
        this.f$1 = protoOutputStream;
    }

    public final void accept(Object obj) {
        BackgroundJobsController.lambda$dumpControllerStateLocked$1(this.f$0, this.f$1, (JobStatus) obj);
    }
}
