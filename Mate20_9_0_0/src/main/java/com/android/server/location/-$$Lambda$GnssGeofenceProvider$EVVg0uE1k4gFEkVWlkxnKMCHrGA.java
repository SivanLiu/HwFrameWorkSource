package com.android.server.location;

import java.util.concurrent.Callable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnssGeofenceProvider$EVVg0uE1k4gFEkVWlkxnKMCHrGA implements Callable {
    private final /* synthetic */ GnssGeofenceProvider f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$GnssGeofenceProvider$EVVg0uE1k4gFEkVWlkxnKMCHrGA(GnssGeofenceProvider gnssGeofenceProvider, int i) {
        this.f$0 = gnssGeofenceProvider;
        this.f$1 = i;
    }

    public final Object call() {
        return GnssGeofenceProvider.lambda$removeHardwareGeofence$2(this.f$0, this.f$1);
    }
}
