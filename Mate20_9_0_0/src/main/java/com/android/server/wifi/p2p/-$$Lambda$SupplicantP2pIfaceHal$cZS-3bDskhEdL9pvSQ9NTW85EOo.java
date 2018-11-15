package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.connectCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$cZS-3bDskhEdL9pvSQ9NTW85EOo implements connectCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$cZS-3bDskhEdL9pvSQ9NTW85EOo(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, String str) {
        this.f$0.setResult(supplicantStatus, str);
    }
}
