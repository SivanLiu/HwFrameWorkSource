package com.android.server.ethernet;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetNetworkFactory$b1ndnzBiSX1ihvZw7GtATwTUsto implements Predicate {
    private final /* synthetic */ boolean f$0;

    public /* synthetic */ -$$Lambda$EthernetNetworkFactory$b1ndnzBiSX1ihvZw7GtATwTUsto(boolean z) {
        this.f$0 = z;
    }

    public final boolean test(Object obj) {
        return EthernetNetworkFactory.lambda$getAvailableInterfaces$0(this.f$0, (NetworkInterfaceState) obj);
    }
}
