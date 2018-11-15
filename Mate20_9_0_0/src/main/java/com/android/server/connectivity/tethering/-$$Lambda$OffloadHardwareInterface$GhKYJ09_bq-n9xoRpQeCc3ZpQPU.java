package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.addDownstreamCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$GhKYJ09_bq-n9xoRpQeCc3ZpQPU implements addDownstreamCallback {
    private final /* synthetic */ CbResults f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$GhKYJ09_bq-n9xoRpQeCc3ZpQPU(CbResults cbResults) {
        this.f$0 = cbResults;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$addDownstreamPrefix$6(this.f$0, z, str);
    }
}
