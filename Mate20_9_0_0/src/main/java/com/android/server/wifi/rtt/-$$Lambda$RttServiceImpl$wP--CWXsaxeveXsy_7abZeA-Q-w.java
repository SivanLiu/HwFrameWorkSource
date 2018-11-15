package com.android.server.wifi.rtt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$wP--CWXsaxeveXsy_7abZeA-Q-w implements Runnable {
    private final /* synthetic */ RttServiceImpl f$0;

    public /* synthetic */ -$$Lambda$RttServiceImpl$wP--CWXsaxeveXsy_7abZeA-Q-w(RttServiceImpl rttServiceImpl) {
        this.f$0 = rttServiceImpl;
    }

    public final void run() {
        this.f$0.mRttServiceSynchronized.cleanUpOnDisable();
    }
}
