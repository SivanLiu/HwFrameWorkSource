package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.listNetworksCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$oY40I1ZV1zNoEKNITjSxjIr7WaE implements listNetworksCallback {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$oY40I1ZV1zNoEKNITjSxjIr7WaE(SupplicantStaIfaceHal supplicantStaIfaceHal, Mutable mutable) {
        this.f$0 = supplicantStaIfaceHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        SupplicantStaIfaceHal.lambda$listNetworks$7(this.f$0, this.f$1, supplicantStatus, arrayList);
    }
}
