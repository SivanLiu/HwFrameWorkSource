package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicant.listInterfacesCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$xxcrLmh4P3s14clwwWnJ79St0UM implements listInterfacesCallback {
    private final /* synthetic */ ArrayList f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$xxcrLmh4P3s14clwwWnJ79St0UM(ArrayList arrayList) {
        this.f$0 = arrayList;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        SupplicantP2pIfaceHal.lambda$getIfaceV1_0$2(this.f$0, supplicantStatus, arrayList);
    }
}
