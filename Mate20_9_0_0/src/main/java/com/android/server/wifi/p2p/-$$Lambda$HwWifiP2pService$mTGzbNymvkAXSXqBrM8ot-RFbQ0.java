package com.android.server.wifi.p2p;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwWifiP2pService$mTGzbNymvkAXSXqBrM8ot-RFbQ0 implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$HwWifiP2pService$mTGzbNymvkAXSXqBrM8ot-RFbQ0(long j) {
        this.f$0 = j;
    }

    public final boolean test(Object obj) {
        return HwWifiP2pService.lambda$allowP2pFindByTime$0(this.f$0, (P2pFindProcessInfo) obj);
    }
}
