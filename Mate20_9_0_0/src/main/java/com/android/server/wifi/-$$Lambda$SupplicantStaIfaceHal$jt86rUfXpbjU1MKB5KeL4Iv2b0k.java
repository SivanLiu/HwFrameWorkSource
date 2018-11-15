package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_1.ISupplicant.addInterfaceCallback;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$jt86rUfXpbjU1MKB5KeL4Iv2b0k implements addInterfaceCallback {
    private final /* synthetic */ Mutable f$0;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$jt86rUfXpbjU1MKB5KeL4Iv2b0k(Mutable mutable) {
        this.f$0 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        SupplicantStaIfaceHal.lambda$addIfaceV1_1$4(this.f$0, supplicantStatus, iSupplicantIface);
    }
}
