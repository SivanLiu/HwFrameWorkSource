package com.android.server.location;

import java.util.concurrent.Callable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$nmIoImstXHuMaecjUXtG9FcFizs implements Callable {
    private final /* synthetic */ GnssGeofenceProviderNative f$0;

    public /* synthetic */ -$$Lambda$nmIoImstXHuMaecjUXtG9FcFizs(GnssGeofenceProviderNative gnssGeofenceProviderNative) {
        this.f$0 = gnssGeofenceProviderNative;
    }

    public final Object call() {
        return Boolean.valueOf(this.f$0.isGeofenceSupported());
    }
}
