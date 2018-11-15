package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;
import vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaIface.getApVendorInfoCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$_DgO3AASha_6QaWN6lPwzwiPLYM implements getApVendorInfoCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$_DgO3AASha_6QaWN6lPwzwiPLYM(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, String str) {
        SupplicantStaIfaceHal.lambda$getApVendorInfo$15(this.f$0, this.f$1, supplicantStatus, str);
    }
}
