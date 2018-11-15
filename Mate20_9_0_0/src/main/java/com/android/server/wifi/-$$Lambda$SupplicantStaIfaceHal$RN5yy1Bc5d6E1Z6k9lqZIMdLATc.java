package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getMacAddressCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$RN5yy1Bc5d6E1Z6k9lqZIMdLATc implements getMacAddressCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$RN5yy1Bc5d6E1Z6k9lqZIMdLATc(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
        SupplicantStaIfaceHal.lambda$getMacAddress$8(this.f$0, this.f$1, supplicantStatus, bArr);
    }
}
