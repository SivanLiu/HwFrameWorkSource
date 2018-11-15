package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip.createApIfaceCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$Sk1PB19thsUnVIURe7jAUQxhiGk implements createApIfaceCallback {
    private final /* synthetic */ Mutable f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$Sk1PB19thsUnVIURe7jAUQxhiGk(Mutable mutable, Mutable mutable2) {
        this.f$0 = mutable;
        this.f$1 = mutable2;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
        HalDeviceManager.lambda$executeChipReconfiguration$20(this.f$0, this.f$1, wifiStatus, iWifiApIface);
    }
}
