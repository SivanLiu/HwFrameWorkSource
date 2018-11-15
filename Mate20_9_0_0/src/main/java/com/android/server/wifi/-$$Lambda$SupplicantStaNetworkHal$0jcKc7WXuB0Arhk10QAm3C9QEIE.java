package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getPairwiseCipherCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$0jcKc7WXuB0Arhk10QAm3C9QEIE implements getPairwiseCipherCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$0jcKc7WXuB0Arhk10QAm3C9QEIE(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(SupplicantStatus supplicantStatus, int i) {
        SupplicantStaNetworkHal.lambda$getPairwiseCipher$8(this.f$0, this.f$1, supplicantStatus, i);
    }
}
