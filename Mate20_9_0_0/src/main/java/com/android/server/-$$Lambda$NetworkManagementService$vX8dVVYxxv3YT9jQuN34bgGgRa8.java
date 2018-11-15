package com.android.server;

import android.net.INetworkManagementEventObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$vX8dVVYxxv3YT9jQuN34bgGgRa8 implements NetworkManagementEventCallback {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$NetworkManagementService$vX8dVVYxxv3YT9jQuN34bgGgRa8(String str) {
        this.f$0 = str;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.interfaceAdded(this.f$0);
    }
}
