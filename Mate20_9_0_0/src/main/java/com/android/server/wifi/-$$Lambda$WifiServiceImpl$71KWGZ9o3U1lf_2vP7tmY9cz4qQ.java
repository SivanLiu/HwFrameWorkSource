package com.android.server.wifi;

import com.android.server.wifi.util.GeneralUtil.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$71KWGZ9o3U1lf_2vP7tmY9cz4qQ implements Runnable {
    private final /* synthetic */ WifiServiceImpl f$0;
    private final /* synthetic */ Mutable f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ String f$3;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$71KWGZ9o3U1lf_2vP7tmY9cz4qQ(WifiServiceImpl wifiServiceImpl, Mutable mutable, int i, String str) {
        this.f$0 = wifiServiceImpl;
        this.f$1 = mutable;
        this.f$2 = i;
        this.f$3 = str;
    }

    public final void run() {
        this.f$1.value = Boolean.valueOf(this.f$0.mScanRequestProxy.startScan(this.f$2, this.f$3));
    }
}
