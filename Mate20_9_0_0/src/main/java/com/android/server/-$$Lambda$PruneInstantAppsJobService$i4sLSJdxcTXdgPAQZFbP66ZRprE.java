package com.android.server;

import android.app.job.JobParameters;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PruneInstantAppsJobService$i4sLSJdxcTXdgPAQZFbP66ZRprE implements Runnable {
    private final /* synthetic */ PruneInstantAppsJobService f$0;
    private final /* synthetic */ JobParameters f$1;

    public /* synthetic */ -$$Lambda$PruneInstantAppsJobService$i4sLSJdxcTXdgPAQZFbP66ZRprE(PruneInstantAppsJobService pruneInstantAppsJobService, JobParameters jobParameters) {
        this.f$0 = pruneInstantAppsJobService;
        this.f$1 = jobParameters;
    }

    public final void run() {
        PruneInstantAppsJobService.lambda$onStartJob$0(this.f$0, this.f$1);
    }
}
