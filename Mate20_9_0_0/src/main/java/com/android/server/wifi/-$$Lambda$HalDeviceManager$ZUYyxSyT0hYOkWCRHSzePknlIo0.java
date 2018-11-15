package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifi.getChipCallback;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;
import android.util.MutableBoolean;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$ZUYyxSyT0hYOkWCRHSzePknlIo0 implements getChipCallback {
    private final /* synthetic */ MutableBoolean f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$ZUYyxSyT0hYOkWCRHSzePknlIo0(MutableBoolean mutableBoolean, Mutable mutable) {
        this.f$0 = mutableBoolean;
        this.f$1 = mutable;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiChip iWifiChip) {
        HalDeviceManager.lambda$getAllChipInfo$7(this.f$0, this.f$1, wifiStatus, iWifiChip);
    }
}
