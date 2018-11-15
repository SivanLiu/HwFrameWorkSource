package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;
import vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface.getWpasConfigCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$C_rmp4-E7MovlV0Isb8I9Q4Ttd4 implements getWpasConfigCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$C_rmp4-E7MovlV0Isb8I9Q4Ttd4(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, String str) {
        SupplicantStaIfaceHal.lambda$getWpasConfig$13(this.f$0, this.f$1, supplicantStatus, str);
    }
}
