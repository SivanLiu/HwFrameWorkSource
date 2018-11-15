package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork.getIdCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$IRxqwt7Zayh6hYF7VQ3jt-Epcrc implements getIdCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$IRxqwt7Zayh6hYF7VQ3jt-Epcrc(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(SupplicantStatus supplicantStatus, int i) {
        SupplicantStaNetworkHal.lambda$getId$0(this.f$0, this.f$1, supplicantStatus, i);
    }
}
