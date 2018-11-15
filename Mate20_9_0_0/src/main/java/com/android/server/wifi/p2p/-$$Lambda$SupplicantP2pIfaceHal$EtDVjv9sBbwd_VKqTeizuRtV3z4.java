package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.listNetworksCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$EtDVjv9sBbwd_VKqTeizuRtV3z4 implements listNetworksCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$EtDVjv9sBbwd_VKqTeizuRtV3z4(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        this.f$0.setResult(supplicantStatus, arrayList);
    }
}
