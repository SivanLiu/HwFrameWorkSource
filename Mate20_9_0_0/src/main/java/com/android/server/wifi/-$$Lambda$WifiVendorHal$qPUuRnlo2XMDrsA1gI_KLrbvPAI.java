package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getBackgroundScanCapabilitiesCallback;
import android.hardware.wifi.V1_0.StaBackgroundScanCapabilities;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import com.android.server.wifi.WifiNative.ScanCapabilities;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$qPUuRnlo2XMDrsA1gI_KLrbvPAI implements getBackgroundScanCapabilitiesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ ScanCapabilities f$1;
    private final /* synthetic */ MutableBoolean f$2;

    public /* synthetic */ -$$Lambda$WifiVendorHal$qPUuRnlo2XMDrsA1gI_KLrbvPAI(WifiVendorHal wifiVendorHal, ScanCapabilities scanCapabilities, MutableBoolean mutableBoolean) {
        this.f$0 = wifiVendorHal;
        this.f$1 = scanCapabilities;
        this.f$2 = mutableBoolean;
    }

    public final void onValues(WifiStatus wifiStatus, StaBackgroundScanCapabilities staBackgroundScanCapabilities) {
        WifiVendorHal.lambda$getBgScanCapabilities$0(this.f$0, this.f$1, this.f$2, wifiStatus, staBackgroundScanCapabilities);
    }
}
