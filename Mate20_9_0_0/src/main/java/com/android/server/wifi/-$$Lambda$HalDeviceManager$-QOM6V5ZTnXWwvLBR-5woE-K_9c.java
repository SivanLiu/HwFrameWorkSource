package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getModeCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$-QOM6V5ZTnXWwvLBR-5woE-K_9c implements getModeCallback {
    private final /* synthetic */ MutableBoolean f$0;
    private final /* synthetic */ MutableBoolean f$1;
    private final /* synthetic */ MutableInt f$2;

    public /* synthetic */ -$$Lambda$HalDeviceManager$-QOM6V5ZTnXWwvLBR-5woE-K_9c(MutableBoolean mutableBoolean, MutableBoolean mutableBoolean2, MutableInt mutableInt) {
        this.f$0 = mutableBoolean;
        this.f$1 = mutableBoolean2;
        this.f$2 = mutableInt;
    }

    public final void onValues(WifiStatus wifiStatus, int i) {
        HalDeviceManager.lambda$getAllChipInfo$9(this.f$0, this.f$1, this.f$2, wifiStatus, i);
    }
}
