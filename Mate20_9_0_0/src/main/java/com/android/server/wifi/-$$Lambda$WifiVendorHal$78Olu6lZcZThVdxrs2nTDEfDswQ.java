package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.ChipDebugInfo;
import android.hardware.wifi.V1_0.IWifiChip.requestChipDebugInfoCallback;
import android.hardware.wifi.V1_0.WifiStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$78Olu6lZcZThVdxrs2nTDEfDswQ implements requestChipDebugInfoCallback {
    private final /* synthetic */ WifiVendorHal f$0;

    public /* synthetic */ -$$Lambda$WifiVendorHal$78Olu6lZcZThVdxrs2nTDEfDswQ(WifiVendorHal wifiVendorHal) {
        this.f$0 = wifiVendorHal;
    }

    public final void onValues(WifiStatus wifiStatus, ChipDebugInfo chipDebugInfo) {
        WifiVendorHal.lambda$requestChipDebugInfo$8(this.f$0, wifiStatus, chipDebugInfo);
    }
}
