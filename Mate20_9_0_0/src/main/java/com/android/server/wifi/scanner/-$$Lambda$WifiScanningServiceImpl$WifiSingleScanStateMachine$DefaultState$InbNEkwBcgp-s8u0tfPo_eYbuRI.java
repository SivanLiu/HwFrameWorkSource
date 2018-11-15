package com.android.server.wifi.scanner;

import android.net.wifi.ScanResult;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiScanningServiceImpl$WifiSingleScanStateMachine$DefaultState$InbNEkwBcgp-s8u0tfPo_eYbuRI implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$WifiScanningServiceImpl$WifiSingleScanStateMachine$DefaultState$InbNEkwBcgp-s8u0tfPo_eYbuRI(long j) {
        this.f$0 = j;
    }

    public final boolean test(Object obj) {
        return DefaultState.lambda$filterCachedScanResultsByAge$0(this.f$0, (ScanResult) obj);
    }
}
