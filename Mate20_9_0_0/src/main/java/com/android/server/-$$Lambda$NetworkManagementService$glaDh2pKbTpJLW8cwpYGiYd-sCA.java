package com.android.server;

import android.net.INetworkManagementEventObserver;
import android.net.RouteInfo;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$glaDh2pKbTpJLW8cwpYGiYd-sCA implements NetworkManagementEventCallback {
    private final /* synthetic */ RouteInfo f$0;

    public /* synthetic */ -$$Lambda$NetworkManagementService$glaDh2pKbTpJLW8cwpYGiYd-sCA(RouteInfo routeInfo) {
        this.f$0 = routeInfo;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.routeUpdated(this.f$0);
    }
}
