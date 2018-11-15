package com.android.server.wifi;

import com.android.server.wifi.WifiServiceImpl.AnonymousClass15;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$15$bcYgGZKA_iNmZg53oTMy98qRxpY implements Runnable {
    private final /* synthetic */ AnonymousClass15 f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$15$bcYgGZKA_iNmZg53oTMy98qRxpY(AnonymousClass15 anonymousClass15, String str, int i) {
        this.f$0 = anonymousClass15;
        this.f$1 = str;
        this.f$2 = i;
    }

    public final void run() {
        WifiServiceImpl.this.mScanRequestProxy.clearScanRequestTimestampsForApp(this.f$1, this.f$2);
    }
}
