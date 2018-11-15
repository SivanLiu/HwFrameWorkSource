package com.android.server;

import android.net.INetworkManagementEventObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$D43p3Tqq7B3qaMs9AGb_3j0KZd0 implements NetworkManagementEventCallback {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ boolean f$1;
    private final /* synthetic */ long f$2;

    public /* synthetic */ -$$Lambda$NetworkManagementService$D43p3Tqq7B3qaMs9AGb_3j0KZd0(int i, boolean z, long j) {
        this.f$0 = i;
        this.f$1 = z;
        this.f$2 = j;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.interfaceClassDataActivityChanged(Integer.toString(this.f$0), this.f$1, this.f$2);
    }
}
