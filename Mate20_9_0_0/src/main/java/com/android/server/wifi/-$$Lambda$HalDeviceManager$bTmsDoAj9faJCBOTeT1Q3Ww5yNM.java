package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiIface.getNameCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$bTmsDoAj9faJCBOTeT1Q3Ww5yNM implements getNameCallback {
    private final /* synthetic */ Mutable f$0;

    public /* synthetic */ -$$Lambda$HalDeviceManager$bTmsDoAj9faJCBOTeT1Q3Ww5yNM(Mutable mutable) {
        this.f$0 = mutable;
    }

    public final void onValues(WifiStatus wifiStatus, String str) {
        HalDeviceManager.lambda$getName$0(this.f$0, wifiStatus, str);
    }
}
