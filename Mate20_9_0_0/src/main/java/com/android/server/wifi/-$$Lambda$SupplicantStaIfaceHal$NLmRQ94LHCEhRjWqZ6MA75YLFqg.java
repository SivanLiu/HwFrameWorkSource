package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;
import vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface.getCapabRsdbCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$NLmRQ94LHCEhRjWqZ6MA75YLFqg implements getCapabRsdbCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$NLmRQ94LHCEhRjWqZ6MA75YLFqg(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, String str) {
        SupplicantStaIfaceHal.lambda$getRsdbCapability$12(this.f$0, this.f$1, supplicantStatus, str);
    }
}
