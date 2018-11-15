package com.android.server.location;

import java.util.concurrent.Callable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnssGeofenceProvider$n5osOgh5pgunifw_x5yjaRzShkA implements Callable {
    private final /* synthetic */ GnssGeofenceProvider f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ double f$2;
    private final /* synthetic */ double f$3;
    private final /* synthetic */ double f$4;
    private final /* synthetic */ int f$5;
    private final /* synthetic */ int f$6;
    private final /* synthetic */ int f$7;
    private final /* synthetic */ int f$8;

    public /* synthetic */ -$$Lambda$GnssGeofenceProvider$n5osOgh5pgunifw_x5yjaRzShkA(GnssGeofenceProvider gnssGeofenceProvider, int i, double d, double d2, double d3, int i2, int i3, int i4, int i5) {
        this.f$0 = gnssGeofenceProvider;
        this.f$1 = i;
        this.f$2 = d;
        this.f$3 = d2;
        this.f$4 = d3;
        this.f$5 = i2;
        this.f$6 = i3;
        this.f$7 = i4;
        this.f$8 = i5;
    }

    public final Object call() {
        return GnssGeofenceProvider.lambda$addCircularHardwareGeofence$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8);
    }
}
