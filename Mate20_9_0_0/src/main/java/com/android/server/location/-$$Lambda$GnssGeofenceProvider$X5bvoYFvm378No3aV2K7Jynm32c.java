package com.android.server.location;

import java.util.concurrent.Callable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnssGeofenceProvider$X5bvoYFvm378No3aV2K7Jynm32c implements Callable {
    private final /* synthetic */ GnssGeofenceProvider f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$GnssGeofenceProvider$X5bvoYFvm378No3aV2K7Jynm32c(GnssGeofenceProvider gnssGeofenceProvider, int i, int i2) {
        this.f$0 = gnssGeofenceProvider;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final Object call() {
        return GnssGeofenceProvider.lambda$resumeHardwareGeofence$4(this.f$0, this.f$1, this.f$2);
    }
}
