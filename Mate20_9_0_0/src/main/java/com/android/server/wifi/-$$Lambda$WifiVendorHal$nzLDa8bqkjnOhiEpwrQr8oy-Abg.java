package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getApfPacketFilterCapabilitiesCallback;
import android.hardware.wifi.V1_0.StaApfPacketFilterCapabilities;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass4AnswerBox;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$nzLDa8bqkjnOhiEpwrQr8oy-Abg implements getApfPacketFilterCapabilitiesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass4AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$nzLDa8bqkjnOhiEpwrQr8oy-Abg(WifiVendorHal wifiVendorHal, AnonymousClass4AnswerBox anonymousClass4AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass4AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, StaApfPacketFilterCapabilities staApfPacketFilterCapabilities) {
        WifiVendorHal.lambda$getApfCapabilities$6(this.f$0, this.f$1, wifiStatus, staApfPacketFilterCapabilities);
    }
}
