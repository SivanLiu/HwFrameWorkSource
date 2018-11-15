package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.setDataLimitCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$4gz9PGx-iHz6VaJglXvPXV_YCTo implements setDataLimitCallback {
    private final /* synthetic */ CbResults f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$4gz9PGx-iHz6VaJglXvPXV_YCTo(CbResults cbResults) {
        this.f$0 = cbResults;
    }

    public final void onValues(boolean z, String str) {
        OffloadHardwareInterface.lambda$setDataLimit$4(this.f$0, z, str);
    }
}
