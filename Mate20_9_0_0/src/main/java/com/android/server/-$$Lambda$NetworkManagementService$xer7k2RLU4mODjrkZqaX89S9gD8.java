package com.android.server;

import android.net.INetworkManagementEventObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkManagementService$xer7k2RLU4mODjrkZqaX89S9gD8 implements NetworkManagementEventCallback {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$NetworkManagementService$xer7k2RLU4mODjrkZqaX89S9gD8(String str, String str2) {
        this.f$0 = str;
        this.f$1 = str2;
    }

    public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        iNetworkManagementEventObserver.limitReached(this.f$0, this.f$1);
    }
}
