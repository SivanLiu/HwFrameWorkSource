package com.android.server.wifi;

import android.net.wifi.ScanResult;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiServiceImpl$96A4u2hMi66fUsU9psWDkWSSwro implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$WifiServiceImpl$96A4u2hMi66fUsU9psWDkWSSwro(long j) {
        this.f$0 = j;
    }

    public final boolean test(Object obj) {
        return WifiServiceImpl.lambda$filterScanProxyResultsByAge$5(this.f$0, (ScanResult) obj);
    }
}
