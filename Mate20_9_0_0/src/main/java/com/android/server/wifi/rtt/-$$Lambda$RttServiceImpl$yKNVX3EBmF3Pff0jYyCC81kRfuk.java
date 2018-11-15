package com.android.server.wifi.rtt;

import android.os.WorkSource;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$yKNVX3EBmF3Pff0jYyCC81kRfuk implements Runnable {
    private final /* synthetic */ RttServiceImpl f$0;
    private final /* synthetic */ WorkSource f$1;

    public /* synthetic */ -$$Lambda$RttServiceImpl$yKNVX3EBmF3Pff0jYyCC81kRfuk(RttServiceImpl rttServiceImpl, WorkSource workSource) {
        this.f$0 = rttServiceImpl;
        this.f$1 = workSource;
    }

    public final void run() {
        this.f$0.mRttServiceSynchronized.cleanUpClientRequests(0, this.f$1);
    }
}
