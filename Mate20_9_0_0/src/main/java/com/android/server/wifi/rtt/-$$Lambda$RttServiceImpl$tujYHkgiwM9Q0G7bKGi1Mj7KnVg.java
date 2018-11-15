package com.android.server.wifi.rtt;

import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$tujYHkgiwM9Q0G7bKGi1Mj7KnVg implements Runnable {
    private final /* synthetic */ RttServiceImpl f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;

    public /* synthetic */ -$$Lambda$RttServiceImpl$tujYHkgiwM9Q0G7bKGi1Mj7KnVg(RttServiceImpl rttServiceImpl, int i, List list) {
        this.f$0 = rttServiceImpl;
        this.f$1 = i;
        this.f$2 = list;
    }

    public final void run() {
        this.f$0.mRttServiceSynchronized.onRangingResults(this.f$1, this.f$2);
    }
}
