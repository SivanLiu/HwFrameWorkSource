package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getP2pIfaceCallback;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4 implements getP2pIfaceCallback {
    private final /* synthetic */ HalDeviceManager f$0;
    private final /* synthetic */ MutableBoolean f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ WifiIfaceInfo[] f$3;
    private final /* synthetic */ MutableInt f$4;

    public /* synthetic */ -$$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4(HalDeviceManager halDeviceManager, MutableBoolean mutableBoolean, String str, WifiIfaceInfo[] wifiIfaceInfoArr, MutableInt mutableInt) {
        this.f$0 = halDeviceManager;
        this.f$1 = mutableBoolean;
        this.f$2 = str;
        this.f$3 = wifiIfaceInfoArr;
        this.f$4 = mutableInt;
    }

    public final void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
        HalDeviceManager.lambda$getAllChipInfo$15(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, wifiStatus, iWifiP2pIface);
    }
}
