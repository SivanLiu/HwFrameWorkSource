package com.android.server.wifi.rtt;

import android.hardware.wifi.V1_0.IWifiRttController.getCapabilitiesCallback;
import android.hardware.wifi.V1_0.RttCapabilities;
import android.hardware.wifi.V1_0.WifiStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttNative$nRSOFcP2WhqxmfStf2OeZAekTCY implements getCapabilitiesCallback {
    private final /* synthetic */ RttNative f$0;

    public /* synthetic */ -$$Lambda$RttNative$nRSOFcP2WhqxmfStf2OeZAekTCY(RttNative rttNative) {
        this.f$0 = rttNative;
    }

    public final void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
        RttNative.lambda$updateRttCapabilities$1(this.f$0, wifiStatus, rttCapabilities);
    }
}
