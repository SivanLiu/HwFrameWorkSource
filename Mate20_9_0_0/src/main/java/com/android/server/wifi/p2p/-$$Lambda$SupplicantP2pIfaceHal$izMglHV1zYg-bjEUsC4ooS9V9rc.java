package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.requestServiceDiscoveryCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$izMglHV1zYg-bjEUsC4ooS9V9rc implements requestServiceDiscoveryCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$izMglHV1zYg-bjEUsC4ooS9V9rc(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, long j) {
        this.f$0.setResult(supplicantStatus, new Long(j));
    }
}
