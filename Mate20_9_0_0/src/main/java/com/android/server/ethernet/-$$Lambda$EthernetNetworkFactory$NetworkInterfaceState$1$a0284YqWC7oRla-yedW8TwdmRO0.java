package com.android.server.ethernet;

import android.net.LinkProperties;
import com.android.server.ethernet.EthernetNetworkFactory.NetworkInterfaceState.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetNetworkFactory$NetworkInterfaceState$1$a0284YqWC7oRla-yedW8TwdmRO0 implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ LinkProperties f$1;

    public /* synthetic */ -$$Lambda$EthernetNetworkFactory$NetworkInterfaceState$1$a0284YqWC7oRla-yedW8TwdmRO0(AnonymousClass1 anonymousClass1, LinkProperties linkProperties) {
        this.f$0 = anonymousClass1;
        this.f$1 = linkProperties;
    }

    public final void run() {
        NetworkInterfaceState.this.updateLinkProperties(this.f$1);
    }
}
