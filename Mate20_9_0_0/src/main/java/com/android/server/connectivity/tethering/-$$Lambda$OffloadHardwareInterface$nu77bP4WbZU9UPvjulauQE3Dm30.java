package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.getForwardedStatsCallback;
import com.android.server.connectivity.tethering.OffloadHardwareInterface.ForwardedStats;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadHardwareInterface$nu77bP4WbZU9UPvjulauQE3Dm30 implements getForwardedStatsCallback {
    private final /* synthetic */ ForwardedStats f$0;

    public /* synthetic */ -$$Lambda$OffloadHardwareInterface$nu77bP4WbZU9UPvjulauQE3Dm30(ForwardedStats forwardedStats) {
        this.f$0 = forwardedStats;
    }

    public final void onValues(long j, long j2) {
        OffloadHardwareInterface.lambda$getForwardedStats$2(this.f$0, j, j2);
    }
}
