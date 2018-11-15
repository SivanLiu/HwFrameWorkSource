package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiIface.getTypeCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$ErxCpEghr4yhQpGHX1NQPumvouc implements getTypeCallback {
    private final /* synthetic */ MutableInt f$0;

    public /* synthetic */ -$$Lambda$HalDeviceManager$ErxCpEghr4yhQpGHX1NQPumvouc(MutableInt mutableInt) {
        this.f$0 = mutableInt;
    }

    public final void onValues(WifiStatus wifiStatus, int i) {
        HalDeviceManager.lambda$getType$23(this.f$0, wifiStatus, i);
    }
}
