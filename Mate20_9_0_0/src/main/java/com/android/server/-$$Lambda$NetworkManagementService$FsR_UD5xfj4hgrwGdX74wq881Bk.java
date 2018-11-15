package com.android.server;

import android.net.INetworkManagementEventObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$FsR_UD5xfj4hgrwGdX74wq881Bk implements NetworkManagementEventCallback {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$NetworkManagementService$FsR_UD5xfj4hgrwGdX74wq881Bk(String str) {
        this.f$0 = str;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.interfaceRemoved(this.f$0);
    }
}
