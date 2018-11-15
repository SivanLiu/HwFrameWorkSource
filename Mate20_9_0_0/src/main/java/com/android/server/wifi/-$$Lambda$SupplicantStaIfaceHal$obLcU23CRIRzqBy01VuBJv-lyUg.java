package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.startWpsPinDisplayCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$obLcU23CRIRzqBy01VuBJv-lyUg implements startWpsPinDisplayCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$obLcU23CRIRzqBy01VuBJv-lyUg(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, String str) {
        SupplicantStaIfaceHal.lambda$startWpsPinDisplay$9(this.f$0, this.f$1, supplicantStatus, str);
    }
}
