package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.createStaIfaceCallback;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$csull9RuGux3O9fMU2TmHd3K8YE implements createStaIfaceCallback {
    private final /* synthetic */ Mutable f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$csull9RuGux3O9fMU2TmHd3K8YE(Mutable mutable, Mutable mutable2) {
        this.f$0 = mutable;
        this.f$1 = mutable2;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
        HalDeviceManager.lambda$executeChipReconfiguration$19(this.f$0, this.f$1, wifiStatus, iWifiStaIface);
    }
}
