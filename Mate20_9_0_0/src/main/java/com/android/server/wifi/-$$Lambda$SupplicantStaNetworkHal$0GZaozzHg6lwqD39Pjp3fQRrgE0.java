package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.os.HidlSupport.Mutable;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$0GZaozzHg6lwqD39Pjp3fQRrgE0 implements getWpsNfcConfigurationTokenCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ Mutable f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$0GZaozzHg6lwqD39Pjp3fQRrgE0(SupplicantStaNetworkHal supplicantStaNetworkHal, Mutable mutable) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutable;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        SupplicantStaNetworkHal.lambda$getWpsNfcConfigurationTokenInternal$29(this.f$0, this.f$1, supplicantStatus, arrayList);
    }
}
