package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pNetwork.isCurrentCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$DZ5hjM0K-k-jbWASpzD6nJ3e6xU implements isCurrentCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$DZ5hjM0K-k-jbWASpzD6nJ3e6xU(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
        this.f$0.setResult(supplicantStatus, Boolean.valueOf(z));
    }
}
