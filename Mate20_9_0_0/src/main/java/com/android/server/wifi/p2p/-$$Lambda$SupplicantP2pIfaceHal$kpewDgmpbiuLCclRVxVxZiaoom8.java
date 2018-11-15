package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.getSsidCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$kpewDgmpbiuLCclRVxVxZiaoom8 implements getSsidCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$kpewDgmpbiuLCclRVxVxZiaoom8(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        SupplicantP2pIfaceHal.lambda$getSsid$8(this.f$0, supplicantStatus, arrayList);
    }
}
