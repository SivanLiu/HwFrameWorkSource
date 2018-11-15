package com.android.server.utils;

import com.android.server.utils.ManagedApplicationService.LogEvent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ManagedApplicationService$7a-sAFwcUuC9yt8nXYlr0jScFcs implements Runnable {
    private final /* synthetic */ ManagedApplicationService f$0;
    private final /* synthetic */ long f$1;

    public /* synthetic */ -$$Lambda$ManagedApplicationService$7a-sAFwcUuC9yt8nXYlr0jScFcs(ManagedApplicationService managedApplicationService, long j) {
        this.f$0 = managedApplicationService;
        this.f$1 = j;
    }

    public final void run() {
        this.f$0.mEventCb.onServiceEvent(new LogEvent(this.f$1, this.f$0.mComponent, 4));
    }
}
