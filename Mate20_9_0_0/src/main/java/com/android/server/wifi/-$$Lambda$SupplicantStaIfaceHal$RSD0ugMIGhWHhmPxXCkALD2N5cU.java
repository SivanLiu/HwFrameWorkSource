package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicant.listInterfacesCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$RSD0ugMIGhWHhmPxXCkALD2N5cU implements listInterfacesCallback {
    private final /* synthetic */ ArrayList f$0;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$RSD0ugMIGhWHhmPxXCkALD2N5cU(ArrayList arrayList) {
        this.f$0 = arrayList;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
        SupplicantStaIfaceHal.lambda$getIfaceV1_0$2(this.f$0, supplicantStatus, arrayList);
    }
}
