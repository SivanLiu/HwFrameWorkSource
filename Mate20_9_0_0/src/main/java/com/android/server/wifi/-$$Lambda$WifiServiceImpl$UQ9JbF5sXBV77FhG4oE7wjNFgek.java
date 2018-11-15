package com.android.server.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$UQ9JbF5sXBV77FhG4oE7wjNFgek implements Runnable {
    private final /* synthetic */ WifiServiceImpl f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$UQ9JbF5sXBV77FhG4oE7wjNFgek(WifiServiceImpl wifiServiceImpl, String str, int i) {
        this.f$0 = wifiServiceImpl;
        this.f$1 = str;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.updateInterfaceIpStateInternal(this.f$1, this.f$2);
    }
}
