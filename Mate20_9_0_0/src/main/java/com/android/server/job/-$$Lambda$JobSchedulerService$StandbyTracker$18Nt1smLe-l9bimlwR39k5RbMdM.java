package com.android.server.job;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobSchedulerService$StandbyTracker$18Nt1smLe-l9bimlwR39k5RbMdM implements Runnable {
    private final /* synthetic */ StandbyTracker f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ String f$3;

    public /* synthetic */ -$$Lambda$JobSchedulerService$StandbyTracker$18Nt1smLe-l9bimlwR39k5RbMdM(StandbyTracker standbyTracker, int i, int i2, String str) {
        this.f$0 = standbyTracker;
        this.f$1 = i;
        this.f$2 = i2;
        this.f$3 = str;
    }

    public final void run() {
        StandbyTracker.lambda$onAppIdleStateChanged$1(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
