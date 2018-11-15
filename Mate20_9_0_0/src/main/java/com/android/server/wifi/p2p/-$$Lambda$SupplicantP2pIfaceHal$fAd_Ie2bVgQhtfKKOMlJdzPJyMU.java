package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.getGroupCapabilityCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$fAd_Ie2bVgQhtfKKOMlJdzPJyMU implements getGroupCapabilityCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$fAd_Ie2bVgQhtfKKOMlJdzPJyMU(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, int i) {
        this.f$0.setResult(supplicantStatus, Integer.valueOf(i));
    }
}
