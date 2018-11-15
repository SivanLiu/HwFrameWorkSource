package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantP2pIface.getP2pLinkspeedCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$3zapzn-CcC1guWThXscIB8Q43MM implements getP2pLinkspeedCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$3zapzn-CcC1guWThXscIB8Q43MM(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, int i) {
        this.f$0.setResult(supplicantStatus, Integer.valueOf(i));
    }
}
