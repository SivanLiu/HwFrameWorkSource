package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getCapabilitiesCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$Lnl0TvBZpgQMVgoYAtSlApp_k88 implements getCapabilitiesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ MutableInt f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$Lnl0TvBZpgQMVgoYAtSlApp_k88(WifiVendorHal wifiVendorHal, MutableInt mutableInt) {
        this.f$0 = wifiVendorHal;
        this.f$1 = mutableInt;
    }

    public final void onValues(WifiStatus wifiStatus, int i) {
        WifiVendorHal.lambda$getSupportedFeatureSet$3(this.f$0, this.f$1, wifiStatus, i);
    }
}
