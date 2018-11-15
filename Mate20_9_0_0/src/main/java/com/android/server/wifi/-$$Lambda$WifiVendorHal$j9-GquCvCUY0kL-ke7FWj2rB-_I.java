package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiRttController.getCapabilitiesCallback;
import android.hardware.wifi.V1_0.RttCapabilities;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass2AnswerBox;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$j9-GquCvCUY0kL-ke7FWj2rB-_I implements getCapabilitiesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass2AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$j9-GquCvCUY0kL-ke7FWj2rB-_I(WifiVendorHal wifiVendorHal, AnonymousClass2AnswerBox anonymousClass2AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass2AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
        WifiVendorHal.lambda$getRttCapabilities$4(this.f$0, this.f$1, wifiStatus, rttCapabilities);
    }
}
