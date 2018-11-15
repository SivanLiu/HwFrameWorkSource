package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getDebugHostWakeReasonStatsCallback;
import android.hardware.wifi.V1_0.WifiDebugHostWakeReasonStats;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass9AnswerBox;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$9OKuBaEsJa-3ksFDFIHk8H-fn6Q implements getDebugHostWakeReasonStatsCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass9AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$9OKuBaEsJa-3ksFDFIHk8H-fn6Q(WifiVendorHal wifiVendorHal, AnonymousClass9AnswerBox anonymousClass9AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass9AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats) {
        WifiVendorHal.lambda$getWlanWakeReasonCount$14(this.f$0, this.f$1, wifiStatus, wifiDebugHostWakeReasonStats);
    }
}
