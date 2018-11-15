package com.android.server;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ConnectivityService$SFqiR4Pfksb1C7csMC3uNxCllR8 implements Runnable {
    private final /* synthetic */ ConnectivityService f$0;

    public /* synthetic */ -$$Lambda$ConnectivityService$SFqiR4Pfksb1C7csMC3uNxCllR8(ConnectivityService connectivityService) {
        this.f$0 = connectivityService;
    }

    public final void run() {
        this.f$0.rematchForAvoidBadWifiUpdate();
    }
}
