package com.android.server;

import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$V2aaK7-IK-mKPVvhONFoyFWi4zM implements NetworkManagementEventCallback {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ LinkAddress f$1;

    public /* synthetic */ -$$Lambda$NetworkManagementService$V2aaK7-IK-mKPVvhONFoyFWi4zM(String str, LinkAddress linkAddress) {
        this.f$0 = str;
        this.f$1 = linkAddress;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.addressUpdated(this.f$0, this.f$1);
    }
}
