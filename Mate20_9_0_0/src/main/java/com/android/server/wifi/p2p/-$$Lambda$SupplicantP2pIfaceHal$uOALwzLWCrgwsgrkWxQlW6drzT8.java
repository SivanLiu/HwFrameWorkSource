package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_1.ISupplicant.addInterfaceCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$uOALwzLWCrgwsgrkWxQlW6drzT8 implements addInterfaceCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$uOALwzLWCrgwsgrkWxQlW6drzT8(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        SupplicantP2pIfaceHal.lambda$addIfaceV1_1$4(this.f$0, supplicantStatus, iSupplicantIface);
    }
}
