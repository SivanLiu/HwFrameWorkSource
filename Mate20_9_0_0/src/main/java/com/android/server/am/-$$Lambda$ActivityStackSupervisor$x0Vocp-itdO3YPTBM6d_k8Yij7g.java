package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityStackSupervisor$x0Vocp-itdO3YPTBM6d_k8Yij7g implements Runnable {
    private final /* synthetic */ ActivityStackSupervisor f$0;
    private final /* synthetic */ ActivityStack f$1;

    public /* synthetic */ -$$Lambda$ActivityStackSupervisor$x0Vocp-itdO3YPTBM6d_k8Yij7g(ActivityStackSupervisor activityStackSupervisor, ActivityStack activityStack) {
        this.f$0 = activityStackSupervisor;
        this.f$1 = activityStack;
    }

    public final void run() {
        this.f$0.removeStackInSurfaceTransaction(this.f$1);
    }
}
