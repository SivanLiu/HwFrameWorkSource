package com.android.server.job.controllers;

import com.android.internal.util.IndentingPrintWriter;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DeviceIdleJobsController$essc-q8XD1L8ojfbmN1Aow_AVPk implements Consumer {
    private final /* synthetic */ DeviceIdleJobsController f$0;
    private final /* synthetic */ IndentingPrintWriter f$1;

    public /* synthetic */ -$$Lambda$DeviceIdleJobsController$essc-q8XD1L8ojfbmN1Aow_AVPk(DeviceIdleJobsController deviceIdleJobsController, IndentingPrintWriter indentingPrintWriter) {
        this.f$0 = deviceIdleJobsController;
        this.f$1 = indentingPrintWriter;
    }

    public final void accept(Object obj) {
        DeviceIdleJobsController.lambda$dumpControllerStateLocked$0(this.f$0, this.f$1, (JobStatus) obj);
    }
}
