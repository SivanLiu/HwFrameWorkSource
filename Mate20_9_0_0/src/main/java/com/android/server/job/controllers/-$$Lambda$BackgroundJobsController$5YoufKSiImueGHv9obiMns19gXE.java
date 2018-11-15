package com.android.server.job.controllers;

import com.android.internal.util.IndentingPrintWriter;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackgroundJobsController$5YoufKSiImueGHv9obiMns19gXE implements Consumer {
    private final /* synthetic */ BackgroundJobsController f$0;
    private final /* synthetic */ IndentingPrintWriter f$1;

    public /* synthetic */ -$$Lambda$BackgroundJobsController$5YoufKSiImueGHv9obiMns19gXE(BackgroundJobsController backgroundJobsController, IndentingPrintWriter indentingPrintWriter) {
        this.f$0 = backgroundJobsController;
        this.f$1 = indentingPrintWriter;
    }

    public final void accept(Object obj) {
        BackgroundJobsController.lambda$dumpControllerStateLocked$0(this.f$0, this.f$1, (JobStatus) obj);
    }
}
