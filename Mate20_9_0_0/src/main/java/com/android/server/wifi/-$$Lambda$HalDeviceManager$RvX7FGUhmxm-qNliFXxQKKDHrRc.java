package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getIdCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$RvX7FGUhmxm-qNliFXxQKKDHrRc implements getIdCallback {
    private final /* synthetic */ MutableInt f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$RvX7FGUhmxm-qNliFXxQKKDHrRc(MutableInt mutableInt, MutableBoolean mutableBoolean) {
        this.f$0 = mutableInt;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(WifiStatus wifiStatus, int i) {
        HalDeviceManager.lambda$getSupportedIfaceTypesInternal$18(this.f$0, this.f$1, wifiStatus, i);
    }
}
