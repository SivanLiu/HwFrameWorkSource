package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicant.getInterfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$RyQnT_v7B4l3vVijvOVBxHlvVoY implements getInterfaceCallback {
    private final /* synthetic */ Mutable f$0;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$RyQnT_v7B4l3vVijvOVBxHlvVoY(Mutable mutable) {
        this.f$0 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        SupplicantStaIfaceHal.lambda$getIfaceV1_0$3(this.f$0, supplicantStatus, iSupplicantIface);
    }
}
