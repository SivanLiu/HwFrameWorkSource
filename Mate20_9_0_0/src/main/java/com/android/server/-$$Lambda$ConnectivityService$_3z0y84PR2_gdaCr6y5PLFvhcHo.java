package com.android.server;

import android.net.Network;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ConnectivityService$_3z0y84PR2_gdaCr6y5PLFvhcHo implements Runnable {
    private final /* synthetic */ ConnectivityService f$0;
    private final /* synthetic */ Network f$1;

    public /* synthetic */ -$$Lambda$ConnectivityService$_3z0y84PR2_gdaCr6y5PLFvhcHo(ConnectivityService connectivityService, Network network) {
        this.f$0 = connectivityService;
        this.f$1 = network;
    }

    public final void run() {
        ConnectivityService.lambda$startCaptivePortalApp$1(this.f$0, this.f$1);
    }
}
