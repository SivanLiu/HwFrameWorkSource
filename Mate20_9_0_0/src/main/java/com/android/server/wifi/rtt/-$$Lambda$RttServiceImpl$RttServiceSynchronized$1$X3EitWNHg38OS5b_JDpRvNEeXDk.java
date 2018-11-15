package com.android.server.wifi.rtt;

import com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.AnonymousClass1;
import java.util.Map;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ RttRequestInfo f$1;
    private final /* synthetic */ Map f$2;

    public /* synthetic */ -$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk(AnonymousClass1 anonymousClass1, RttRequestInfo rttRequestInfo, Map map) {
        this.f$0 = anonymousClass1;
        this.f$1 = rttRequestInfo;
        this.f$2 = map;
    }

    public final void run() {
        RttServiceSynchronized.this.processReceivedAwarePeerMacAddresses(this.f$1, this.f$2);
    }
}
