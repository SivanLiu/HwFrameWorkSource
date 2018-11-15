package com.android.server;

import android.net.INetworkManagementEventObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$8J1LB_n8vMkXxx2KS06P_lQCw6w implements NetworkManagementEventCallback {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ long f$1;
    private final /* synthetic */ String[] f$2;

    public /* synthetic */ -$$Lambda$NetworkManagementService$8J1LB_n8vMkXxx2KS06P_lQCw6w(String str, long j, String[] strArr) {
        this.f$0 = str;
        this.f$1 = j;
        this.f$2 = strArr;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.interfaceDnsServerInfo(this.f$0, this.f$1, this.f$2);
    }
}
