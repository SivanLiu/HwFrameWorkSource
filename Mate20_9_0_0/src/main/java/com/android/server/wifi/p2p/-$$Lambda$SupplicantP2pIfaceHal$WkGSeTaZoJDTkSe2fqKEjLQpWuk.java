package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.getDeviceAddressCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$WkGSeTaZoJDTkSe2fqKEjLQpWuk implements getDeviceAddressCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$WkGSeTaZoJDTkSe2fqKEjLQpWuk(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
        SupplicantP2pIfaceHal.lambda$getDeviceAddress$7(this.f$0, supplicantStatus, bArr);
    }
}
