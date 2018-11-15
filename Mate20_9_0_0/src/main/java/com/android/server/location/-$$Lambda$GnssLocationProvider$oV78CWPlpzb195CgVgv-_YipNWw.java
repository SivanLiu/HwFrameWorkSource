package com.android.server.location;

import android.location.LocationManager;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnssLocationProvider$oV78CWPlpzb195CgVgv-_YipNWw implements Runnable {
    private final /* synthetic */ LocationChangeListener f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ LocationManager f$2;

    public /* synthetic */ -$$Lambda$GnssLocationProvider$oV78CWPlpzb195CgVgv-_YipNWw(LocationChangeListener locationChangeListener, String str, LocationManager locationManager) {
        this.f$0 = locationChangeListener;
        this.f$1 = str;
        this.f$2 = locationManager;
    }

    public final void run() {
        GnssLocationProvider.lambda$handleRequestLocation$1(this.f$0, this.f$1, this.f$2);
    }
}
