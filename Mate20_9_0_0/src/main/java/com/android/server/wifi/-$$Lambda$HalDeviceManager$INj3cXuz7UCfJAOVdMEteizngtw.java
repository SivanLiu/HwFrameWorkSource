package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getP2pIfaceNamesCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;
import android.util.MutableBoolean;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$INj3cXuz7UCfJAOVdMEteizngtw implements getP2pIfaceNamesCallback {
    private final /* synthetic */ MutableBoolean f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$INj3cXuz7UCfJAOVdMEteizngtw(MutableBoolean mutableBoolean, Mutable mutable) {
        this.f$0 = mutableBoolean;
        this.f$1 = mutable;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        HalDeviceManager.lambda$getAllChipInfo$14(this.f$0, this.f$1, wifiStatus, arrayList);
    }
}
