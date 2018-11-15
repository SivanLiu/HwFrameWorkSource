package com.android.server.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnssLocationProvider$2m3d6BkqWO0fZAJAijxHyPDHfxI implements Runnable {
    private final /* synthetic */ int[] f$0;
    private final /* synthetic */ int[] f$1;

    public /* synthetic */ -$$Lambda$GnssLocationProvider$2m3d6BkqWO0fZAJAijxHyPDHfxI(int[] iArr, int[] iArr2) {
        this.f$0 = iArr;
        this.f$1 = iArr2;
    }

    public final void run() {
        GnssLocationProvider.native_set_satellite_blacklist(this.f$0, this.f$1);
    }
}
