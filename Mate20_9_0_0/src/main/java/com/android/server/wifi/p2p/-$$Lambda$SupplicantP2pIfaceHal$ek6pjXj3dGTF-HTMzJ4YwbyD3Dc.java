package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface.createNfcHandoverSelectMessageCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$ek6pjXj3dGTF-HTMzJ4YwbyD3Dc implements createNfcHandoverSelectMessageCallback {
    private final /* synthetic */ SupplicantResult f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$ek6pjXj3dGTF-HTMzJ4YwbyD3Dc(SupplicantResult supplicantResult) {
        this.f$0 = supplicantResult;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        this.f$0.setResult(supplicantStatus, arrayList);
    }
}
