package com.android.server.utils;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ManagedApplicationService$TUtdiUHqGW7Fae8jX7ATvPxzdeM implements Runnable {
    private final /* synthetic */ ManagedApplicationService f$0;

    public /* synthetic */ -$$Lambda$ManagedApplicationService$TUtdiUHqGW7Fae8jX7ATvPxzdeM(ManagedApplicationService managedApplicationService) {
        this.f$0 = managedApplicationService;
    }

    public final void run() {
        this.f$0.doRetry();
    }
}
