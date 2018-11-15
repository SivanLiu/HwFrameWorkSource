package com.android.server.wifi.rtt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$RttServiceSynchronized$nvl34lO7P1KT2zH6q5nTdziEODs implements Runnable {
    private final /* synthetic */ RttServiceSynchronized f$0;

    public /* synthetic */ -$$Lambda$RttServiceImpl$RttServiceSynchronized$nvl34lO7P1KT2zH6q5nTdziEODs(RttServiceSynchronized rttServiceSynchronized) {
        this.f$0 = rttServiceSynchronized;
    }

    public final void run() {
        this.f$0.timeoutRangingRequest();
    }
}
