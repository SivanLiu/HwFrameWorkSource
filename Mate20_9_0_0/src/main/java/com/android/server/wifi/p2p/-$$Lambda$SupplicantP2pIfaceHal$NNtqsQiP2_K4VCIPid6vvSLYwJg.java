package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pNetwork.isGoCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$NNtqsQiP2_K4VCIPid6vvSLYwJg implements isGoCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$NNtqsQiP2_K4VCIPid6vvSLYwJg(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
        this.f$0.setResult(supplicantStatus, Boolean.valueOf(z));
    }
}
