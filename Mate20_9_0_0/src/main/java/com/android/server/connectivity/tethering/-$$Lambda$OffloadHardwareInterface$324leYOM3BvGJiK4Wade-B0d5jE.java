package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.initOffloadCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$324leYOM3BvGJiK4Wade-B0d5jE implements initOffloadCallback {
    private final /* synthetic */ CbResults f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$324leYOM3BvGJiK4Wade-B0d5jE(CbResults cbResults) {
        this.f$0 = cbResults;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$initOffloadControl$0(this.f$0, z, str);
    }
}
