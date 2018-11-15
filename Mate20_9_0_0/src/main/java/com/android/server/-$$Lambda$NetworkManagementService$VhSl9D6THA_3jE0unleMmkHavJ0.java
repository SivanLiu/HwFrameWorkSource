package com.android.server;

import android.net.INetworkManagementEventObserver;
import android.net.RouteInfo;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$VhSl9D6THA_3jE0unleMmkHavJ0 implements NetworkManagementEventCallback {
    private final /* synthetic */ RouteInfo f$0;

    public /* synthetic */ -$$Lambda$NetworkManagementService$VhSl9D6THA_3jE0unleMmkHavJ0(RouteInfo routeInfo) {
        this.f$0 = routeInfo;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.routeRemoved(this.f$0);
    }
}
