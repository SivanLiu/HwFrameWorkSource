package com.android.server;

import android.net.INetworkManagementEventObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$fl14NirBlFUd6eJkGcL0QWd5-w0 implements NetworkManagementEventCallback {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$NetworkManagementService$fl14NirBlFUd6eJkGcL0QWd5-w0(String str, boolean z) {
        this.f$0 = str;
        this.f$1 = z;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.interfaceStatusChanged(this.f$0, this.f$1);
    }
}
