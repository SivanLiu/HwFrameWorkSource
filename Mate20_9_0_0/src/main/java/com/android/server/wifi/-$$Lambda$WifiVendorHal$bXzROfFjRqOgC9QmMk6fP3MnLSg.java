package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getCapabilitiesCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$bXzROfFjRqOgC9QmMk6fP3MnLSg implements getCapabilitiesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ MutableInt f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$bXzROfFjRqOgC9QmMk6fP3MnLSg(WifiVendorHal wifiVendorHal, MutableInt mutableInt) {
        this.f$0 = wifiVendorHal;
        this.f$1 = mutableInt;
    }

    public final void onValues(WifiStatus wifiStatus, int i) {
        WifiVendorHal.lambda$getSupportedFeatureSet$2(this.f$0, this.f$1, wifiStatus, i);
    }
}
