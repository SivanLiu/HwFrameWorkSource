package com.android.server.pm;

import java.io.File;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ParallelPackageParser$FTtinPrp068lVeI7K6bC1tNE3iM implements Runnable {
    private final /* synthetic */ ParallelPackageParser f$0;
    private final /* synthetic */ File f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$ParallelPackageParser$FTtinPrp068lVeI7K6bC1tNE3iM(ParallelPackageParser parallelPackageParser, File file, int i) {
        this.f$0 = parallelPackageParser;
        this.f$1 = file;
        this.f$2 = i;
    }

    public final void run() {
        ParallelPackageParser.lambda$submit$0(this.f$0, this.f$1, this.f$2);
    }
}
