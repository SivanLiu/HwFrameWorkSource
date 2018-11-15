package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.stopOffloadCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$AOzzTRw82KskEfgGFRGSy26wGv8 implements stopOffloadCallback {
    private final /* synthetic */ OffloadHardwareInterface f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$AOzzTRw82KskEfgGFRGSy26wGv8(OffloadHardwareInterface offloadHardwareInterface) {
        this.f$0 = offloadHardwareInterface;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$stopOffloadControl$1(this.f$0, z, str);
    }
}
