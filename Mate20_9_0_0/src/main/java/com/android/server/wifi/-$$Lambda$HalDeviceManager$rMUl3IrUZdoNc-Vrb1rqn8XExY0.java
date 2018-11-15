package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.createNanIfaceCallback;
import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$rMUl3IrUZdoNc-Vrb1rqn8XExY0 implements createNanIfaceCallback {
    private final /* synthetic */ Mutable f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$rMUl3IrUZdoNc-Vrb1rqn8XExY0(Mutable mutable, Mutable mutable2) {
        this.f$0 = mutable;
        this.f$1 = mutable2;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
        HalDeviceManager.lambda$executeChipReconfiguration$22(this.f$0, this.f$1, wifiStatus, iWifiNanIface);
    }
}
