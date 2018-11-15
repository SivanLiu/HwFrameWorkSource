package com.android.server.wifi.rtt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$ehyq-_xe9BYccoyltP3Gc2lh51g implements Runnable {
    private final /* synthetic */ RttServiceImpl f$0;
    private final /* synthetic */ RttNative f$1;

    public /* synthetic */ -$$Lambda$RttServiceImpl$ehyq-_xe9BYccoyltP3Gc2lh51g(RttServiceImpl rttServiceImpl, RttNative rttNative) {
        this.f$0 = rttServiceImpl;
        this.f$1 = rttNative;
    }

    public final void run() {
        this.f$1.start(this.f$0.mRttServiceSynchronized.mHandler);
    }
}
