package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.removeDownstreamCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$w6w__dI5-bH4oSI_P9WIdOzlG28 implements removeDownstreamCallback {
    private final /* synthetic */ CbResults f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$w6w__dI5-bH4oSI_P9WIdOzlG28(CbResults cbResults) {
        this.f$0 = cbResults;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$removeDownstreamPrefix$7(this.f$0, z, str);
    }
}
