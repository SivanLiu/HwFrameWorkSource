package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.createRttControllerCallback;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$joTzPjiPCypwHxT_jbl9OKHFMJo implements createRttControllerCallback {
    private final /* synthetic */ Mutable f$0;

    public /* synthetic */ -$$Lambda$HalDeviceManager$joTzPjiPCypwHxT_jbl9OKHFMJo(Mutable mutable) {
        this.f$0 = mutable;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiRttController iWifiRttController) {
        HalDeviceManager.lambda$createRttController$1(this.f$0, wifiStatus, iWifiRttController);
    }
}
