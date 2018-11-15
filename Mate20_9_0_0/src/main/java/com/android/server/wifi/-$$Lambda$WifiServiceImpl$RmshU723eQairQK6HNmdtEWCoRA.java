package com.android.server.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$RmshU723eQairQK6HNmdtEWCoRA implements Runnable {
    private final /* synthetic */ WifiServiceImpl f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$RmshU723eQairQK6HNmdtEWCoRA(WifiServiceImpl wifiServiceImpl, int i) {
        this.f$0 = wifiServiceImpl;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mRegisteredSoftApCallbacks.remove(Integer.valueOf(this.f$1));
    }
}
