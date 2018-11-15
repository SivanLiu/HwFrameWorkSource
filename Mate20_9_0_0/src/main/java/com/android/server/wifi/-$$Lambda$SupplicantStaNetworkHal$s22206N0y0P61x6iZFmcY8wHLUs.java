package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getScanSsidCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$s22206N0y0P61x6iZFmcY8wHLUs implements getScanSsidCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$s22206N0y0P61x6iZFmcY8wHLUs(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
        SupplicantStaNetworkHal.lambda$getScanSsid$3(this.f$0, this.f$1, supplicantStatus, z);
    }
}
