package com.android.server.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiStateMachinePrime$k9eVxsOG1LRUZZleL_AuVGTIJGg implements Runnable {
    private final /* synthetic */ WifiStateMachinePrime f$0;
    private final /* synthetic */ SoftApModeConfiguration f$1;

    public /* synthetic */ -$$Lambda$WifiStateMachinePrime$k9eVxsOG1LRUZZleL_AuVGTIJGg(WifiStateMachinePrime wifiStateMachinePrime, SoftApModeConfiguration softApModeConfiguration) {
        this.f$0 = wifiStateMachinePrime;
        this.f$1 = softApModeConfiguration;
    }

    public final void run() {
        this.f$0.startSoftAp(this.f$1);
    }
}
