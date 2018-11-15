package com.android.server;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ConnectivityService$HR6p9H95BgebyI-3AFU2mC38SI0 implements Runnable {
    private final /* synthetic */ ConnectivityService f$0;

    public /* synthetic */ -$$Lambda$ConnectivityService$HR6p9H95BgebyI-3AFU2mC38SI0(ConnectivityService connectivityService) {
        this.f$0 = connectivityService;
    }

    public final void run() {
        this.f$0.notifyIfacesChangedForNetworkStats();
    }
}
