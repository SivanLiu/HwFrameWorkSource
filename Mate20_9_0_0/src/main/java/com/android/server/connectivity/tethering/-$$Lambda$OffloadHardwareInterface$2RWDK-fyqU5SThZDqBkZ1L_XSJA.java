package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.setUpstreamParametersCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$2RWDK-fyqU5SThZDqBkZ1L_XSJA implements setUpstreamParametersCallback {
    private final /* synthetic */ CbResults f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$2RWDK-fyqU5SThZDqBkZ1L_XSJA(CbResults cbResults) {
        this.f$0 = cbResults;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$setUpstreamParameters$5(this.f$0, z, str);
    }
}
