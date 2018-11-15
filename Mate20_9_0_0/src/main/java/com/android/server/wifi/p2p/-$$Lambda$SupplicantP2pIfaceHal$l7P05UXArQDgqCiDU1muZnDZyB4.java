package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicant.getInterfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$l7P05UXArQDgqCiDU1muZnDZyB4 implements getInterfaceCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$l7P05UXArQDgqCiDU1muZnDZyB4(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        SupplicantP2pIfaceHal.lambda$getIfaceV1_0$3(this.f$0, supplicantStatus, iSupplicantIface);
    }
}
