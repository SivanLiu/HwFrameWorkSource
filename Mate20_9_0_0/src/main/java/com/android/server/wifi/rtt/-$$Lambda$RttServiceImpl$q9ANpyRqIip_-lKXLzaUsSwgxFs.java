package com.android.server.wifi.rtt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$q9ANpyRqIip_-lKXLzaUsSwgxFs implements Runnable {
    private final /* synthetic */ RttServiceImpl f$0;

    public /* synthetic */ -$$Lambda$RttServiceImpl$q9ANpyRqIip_-lKXLzaUsSwgxFs(RttServiceImpl rttServiceImpl) {
        this.f$0 = rttServiceImpl;
    }

    public final void run() {
        this.f$0.mRttServiceSynchronized.executeNextRangingRequestIfPossible(false);
    }
}
