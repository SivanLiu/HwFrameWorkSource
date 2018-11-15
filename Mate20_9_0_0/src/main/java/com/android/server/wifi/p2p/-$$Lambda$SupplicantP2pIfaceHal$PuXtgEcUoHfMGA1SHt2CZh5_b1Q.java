package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$PuXtgEcUoHfMGA1SHt2CZh5_b1Q implements getNetworkCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$PuXtgEcUoHfMGA1SHt2CZh5_b1Q(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
        this.f$0.setResult(supplicantStatus, iSupplicantNetwork);
    }
}
