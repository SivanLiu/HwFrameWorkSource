package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.setLocalPrefixesCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$IpWViosH4sGe7yz1VTujaEKIDNQ implements setLocalPrefixesCallback {
    private final /* synthetic */ CbResults f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$IpWViosH4sGe7yz1VTujaEKIDNQ(CbResults cbResults) {
        this.f$0 = cbResults;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$setLocalPrefixes$3(this.f$0, z, str);
    }
}
