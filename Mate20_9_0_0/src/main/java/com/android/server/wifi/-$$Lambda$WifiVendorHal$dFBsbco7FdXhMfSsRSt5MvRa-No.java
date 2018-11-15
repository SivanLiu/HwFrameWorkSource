package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getRoamingCapabilitiesCallback;
import android.hardware.wifi.V1_0.StaRoamingCapabilities;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import com.android.server.wifi.WifiNative.RoamingCapabilities;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$dFBsbco7FdXhMfSsRSt5MvRa-No implements getRoamingCapabilitiesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ RoamingCapabilities f$1;
    private final /* synthetic */ MutableBoolean f$2;

    public /* synthetic */ -$$Lambda$WifiVendorHal$dFBsbco7FdXhMfSsRSt5MvRa-No(WifiVendorHal wifiVendorHal, RoamingCapabilities roamingCapabilities, MutableBoolean mutableBoolean) {
        this.f$0 = wifiVendorHal;
        this.f$1 = roamingCapabilities;
        this.f$2 = mutableBoolean;
    }

    public final void onValues(WifiStatus wifiStatus, StaRoamingCapabilities staRoamingCapabilities) {
        WifiVendorHal.lambda$getRoamingCapabilities$15(this.f$0, this.f$1, this.f$2, wifiStatus, staRoamingCapabilities);
    }
}
