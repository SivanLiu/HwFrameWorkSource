package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.getEapClientCertCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.util.MutableBoolean;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaNetworkHal$FcnwBKRdWmEJtv77LMWU0WTUZJk implements getEapClientCertCallback {
    private final /* synthetic */ SupplicantStaNetworkHal f$0;
    private final /* synthetic */ MutableBoolean f$1;

    public /* synthetic */ -$$Lambda$SupplicantStaNetworkHal$FcnwBKRdWmEJtv77LMWU0WTUZJk(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean mutableBoolean) {
        this.f$0 = supplicantStaNetworkHal;
        this.f$1 = mutableBoolean;
    }

    public final void onValues(SupplicantStatus supplicantStatus, String str) {
        SupplicantStaNetworkHal.lambda$getEapClientCert$21(this.f$0, this.f$1, supplicantStatus, str);
    }
}
