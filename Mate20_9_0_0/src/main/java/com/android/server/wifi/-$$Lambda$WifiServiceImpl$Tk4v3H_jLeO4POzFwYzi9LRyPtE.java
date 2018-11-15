package com.android.server.wifi;

import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$Tk4v3H_jLeO4POzFwYzi9LRyPtE implements Runnable {
    private final /* synthetic */ WifiServiceImpl f$0;
    private final /* synthetic */ MutableInt f$1;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$Tk4v3H_jLeO4POzFwYzi9LRyPtE(WifiServiceImpl wifiServiceImpl, MutableInt mutableInt) {
        this.f$0 = wifiServiceImpl;
        this.f$1 = mutableInt;
    }

    public final void run() {
        this.f$1.value = this.f$0.mSoftApState;
    }
}
