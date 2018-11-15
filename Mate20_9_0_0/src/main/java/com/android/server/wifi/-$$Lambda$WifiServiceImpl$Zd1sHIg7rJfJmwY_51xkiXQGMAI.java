package com.android.server.wifi;

import android.net.wifi.ISoftApCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$Zd1sHIg7rJfJmwY_51xkiXQGMAI implements Runnable {
    private final /* synthetic */ WifiServiceImpl f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ ISoftApCallback f$2;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$Zd1sHIg7rJfJmwY_51xkiXQGMAI(WifiServiceImpl wifiServiceImpl, int i, ISoftApCallback iSoftApCallback) {
        this.f$0 = wifiServiceImpl;
        this.f$1 = i;
        this.f$2 = iSoftApCallback;
    }

    public final void run() {
        WifiServiceImpl.lambda$registerSoftApCallback$3(this.f$0, this.f$1, this.f$2);
    }
}
