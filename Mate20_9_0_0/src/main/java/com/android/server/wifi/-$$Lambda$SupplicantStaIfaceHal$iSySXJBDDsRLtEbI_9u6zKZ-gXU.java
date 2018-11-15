package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$iSySXJBDDsRLtEbI_9u6zKZ-gXU implements getNetworkCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$iSySXJBDDsRLtEbI_9u6zKZ-gXU(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
        SupplicantStaIfaceHal.lambda$getNetwork$6(this.f$0, this.f$1, supplicantStatus, iSupplicantNetwork);
    }
}
