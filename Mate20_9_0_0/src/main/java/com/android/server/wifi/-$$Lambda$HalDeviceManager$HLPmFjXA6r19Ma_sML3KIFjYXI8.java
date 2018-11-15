package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getStaIfaceCallback;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$HLPmFjXA6r19Ma_sML3KIFjYXI8 implements getStaIfaceCallback {
    private final /* synthetic */ HalDeviceManager f$0;
    private final /* synthetic */ MutableBoolean f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ WifiIfaceInfo[] f$3;
    private final /* synthetic */ MutableInt f$4;

    public /* synthetic */ -$$Lambda$HalDeviceManager$HLPmFjXA6r19Ma_sML3KIFjYXI8(HalDeviceManager halDeviceManager, MutableBoolean mutableBoolean, String str, WifiIfaceInfo[] wifiIfaceInfoArr, MutableInt mutableInt) {
        this.f$0 = halDeviceManager;
        this.f$1 = mutableBoolean;
        this.f$2 = str;
        this.f$3 = wifiIfaceInfoArr;
        this.f$4 = mutableInt;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
        HalDeviceManager.lambda$getAllChipInfo$11(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, wifiStatus, iWifiStaIface);
    }
}
