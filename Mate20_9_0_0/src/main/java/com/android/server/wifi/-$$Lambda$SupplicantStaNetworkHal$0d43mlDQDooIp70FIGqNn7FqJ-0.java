package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapIdentityCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$0d43mlDQDooIp70FIGqNn7FqJ-0 implements getEapIdentityCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$0d43mlDQDooIp70FIGqNn7FqJ-0(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        SupplicantStaNetworkHal.lambda$getEapIdentity$16(this.f$0, this.f$1, supplicantStatus, arrayList);
    }
}
