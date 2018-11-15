package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityStackSupervisor$2EfPspQe887pLmnBFuHkVjyLdzE implements Runnable {
    private final /* synthetic */ ActivityStackSupervisor f$0;
    private final /* synthetic */ ActivityStack f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ boolean f$3;

    public /* synthetic */ -$$Lambda$ActivityStackSupervisor$2EfPspQe887pLmnBFuHkVjyLdzE(ActivityStackSupervisor activityStackSupervisor, ActivityStack activityStack, int i, boolean z) {
        this.f$0 = activityStackSupervisor;
        this.f$1 = activityStack;
        this.f$2 = i;
        this.f$3 = z;
    }

    public final void run() {
        this.f$0.moveTasksToFullscreenStackInSurfaceTransaction(this.f$1, this.f$2, this.f$3);
    }
}
