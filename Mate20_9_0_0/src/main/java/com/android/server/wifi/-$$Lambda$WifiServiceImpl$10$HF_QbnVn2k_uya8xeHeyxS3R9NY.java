package com.android.server.wifi;

import com.android.server.wifi.WifiServiceImpl.AnonymousClass10;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$10$HF_QbnVn2k_uya8xeHeyxS3R9NY implements Runnable {
    private final /* synthetic */ AnonymousClass10 f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$10$HF_QbnVn2k_uya8xeHeyxS3R9NY(AnonymousClass10 anonymousClass10, int i) {
        this.f$0 = anonymousClass10;
        this.f$1 = i;
    }

    public final void run() {
        WifiServiceImpl.this.mRegisteredSoftApCallbacks.remove(Integer.valueOf(this.f$1));
    }
}
