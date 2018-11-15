package com.android.server.ethernet;

import android.net.IpConfiguration;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetTracker$WrfGoZ0jmrS_2ZYW4ZE33ZnJcBI implements Runnable {
    private final /* synthetic */ EthernetTracker f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ IpConfiguration f$2;

    public /* synthetic */ -$$Lambda$EthernetTracker$WrfGoZ0jmrS_2ZYW4ZE33ZnJcBI(EthernetTracker ethernetTracker, String str, IpConfiguration ipConfiguration) {
        this.f$0 = ethernetTracker;
        this.f$1 = str;
        this.f$2 = ipConfiguration;
    }

    public final void run() {
        this.f$0.mFactory.updateIpConfiguration(this.f$1, this.f$2);
    }
}
