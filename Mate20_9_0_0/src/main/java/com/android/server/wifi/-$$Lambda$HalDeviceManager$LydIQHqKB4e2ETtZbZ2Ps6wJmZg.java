package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.createP2pIfaceCallback;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$LydIQHqKB4e2ETtZbZ2Ps6wJmZg implements createP2pIfaceCallback {
    private final /* synthetic */ Mutable f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$LydIQHqKB4e2ETtZbZ2Ps6wJmZg(Mutable mutable, Mutable mutable2) {
        this.f$0 = mutable;
        this.f$1 = mutable2;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
        HalDeviceManager.lambda$executeChipReconfiguration$21(this.f$0, this.f$1, wifiStatus, iWifiP2pIface);
    }
}
