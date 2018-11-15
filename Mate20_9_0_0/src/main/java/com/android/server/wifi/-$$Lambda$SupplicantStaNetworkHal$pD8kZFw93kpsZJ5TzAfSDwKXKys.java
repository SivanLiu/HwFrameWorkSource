package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getRequirePmfCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$pD8kZFw93kpsZJ5TzAfSDwKXKys implements getRequirePmfCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$pD8kZFw93kpsZJ5TzAfSDwKXKys(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
        SupplicantStaNetworkHal.lambda$getRequirePmf$13(this.f$0, this.f$1, supplicantStatus, z);
    }
}
