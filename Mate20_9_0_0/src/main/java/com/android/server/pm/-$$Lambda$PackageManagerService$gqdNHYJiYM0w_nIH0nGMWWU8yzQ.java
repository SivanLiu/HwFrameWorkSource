package com.android.server.pm;

import android.content.IntentSender;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerService$gqdNHYJiYM0w_nIH0nGMWWU8yzQ implements Runnable {
    private final /* synthetic */ PackageManagerService f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ IntentSender f$4;

    public /* synthetic */ -$$Lambda$PackageManagerService$gqdNHYJiYM0w_nIH0nGMWWU8yzQ(PackageManagerService packageManagerService, String str, long j, int i, IntentSender intentSender) {
        this.f$0 = packageManagerService;
        this.f$1 = str;
        this.f$2 = j;
        this.f$3 = i;
        this.f$4 = intentSender;
    }

    public final void run() {
        PackageManagerService.lambda$freeStorage$2(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
