package com.android.server.job.controllers;

import android.util.proto.ProtoOutputStream;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DeviceIdleJobsController$JMszgdQK87AK2bjaiI_rwQuTKpc implements Consumer {
    private final /* synthetic */ DeviceIdleJobsController f$0;
    private final /* synthetic */ ProtoOutputStream f$1;

    public /* synthetic */ -$$Lambda$DeviceIdleJobsController$JMszgdQK87AK2bjaiI_rwQuTKpc(DeviceIdleJobsController deviceIdleJobsController, ProtoOutputStream protoOutputStream) {
        this.f$0 = deviceIdleJobsController;
        this.f$1 = protoOutputStream;
    }

    public final void accept(Object obj) {
        DeviceIdleJobsController.lambda$dumpControllerStateLocked$1(this.f$0, this.f$1, (JobStatus) obj);
    }
}
