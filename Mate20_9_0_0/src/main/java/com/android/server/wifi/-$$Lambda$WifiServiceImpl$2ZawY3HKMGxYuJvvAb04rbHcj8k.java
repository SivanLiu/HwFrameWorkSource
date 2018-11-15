package com.android.server.wifi;

import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$2ZawY3HKMGxYuJvvAb04rbHcj8k implements Runnable {
    private final /* synthetic */ WifiServiceImpl f$0;
    private final /* synthetic */ List f$1;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$2ZawY3HKMGxYuJvvAb04rbHcj8k(WifiServiceImpl wifiServiceImpl, List list) {
        this.f$0 = wifiServiceImpl;
        this.f$1 = list;
    }

    public final void run() {
        this.f$1.addAll(this.f$0.mScanRequestProxy.getScanResults());
    }
}
