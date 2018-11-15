package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getLinkLayerStatsCallback;
import android.hardware.wifi.V1_0.StaLinkLayerStats;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass1AnswerBox;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$Cu5ECBYZ9xFCAH1Q99vuft6nyvY implements getLinkLayerStatsCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass1AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$Cu5ECBYZ9xFCAH1Q99vuft6nyvY(WifiVendorHal wifiVendorHal, AnonymousClass1AnswerBox anonymousClass1AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass1AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, StaLinkLayerStats staLinkLayerStats) {
        WifiVendorHal.lambda$getWifiLinkLayerStats$1(this.f$0, this.f$1, wifiStatus, staLinkLayerStats);
    }
}
