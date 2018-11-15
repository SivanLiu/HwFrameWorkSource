package com.android.server.wifi.rtt;

import com.android.server.wifi.rtt.RttServiceImpl.AnonymousClass5;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$5$I2FdRwlbNnYI33vDhQLuFz17gV4 implements Runnable {
    private final /* synthetic */ AnonymousClass5 f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$RttServiceImpl$5$I2FdRwlbNnYI33vDhQLuFz17gV4(AnonymousClass5 anonymousClass5, int i) {
        this.f$0 = anonymousClass5;
        this.f$1 = i;
    }

    public final void run() {
        RttServiceImpl.this.mRttServiceSynchronized.cleanUpClientRequests(this.f$1, null);
    }
}
