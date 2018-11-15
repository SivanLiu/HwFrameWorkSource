package com.android.server.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$5U-_NhZgxqnYDZhpyacq4qBxh8k implements Runnable {
    private final /* synthetic */ GnssSatelliteBlacklistHelper f$0;

    public /* synthetic */ -$$Lambda$5U-_NhZgxqnYDZhpyacq4qBxh8k(GnssSatelliteBlacklistHelper gnssSatelliteBlacklistHelper) {
        this.f$0 = gnssSatelliteBlacklistHelper;
    }

    public final void run() {
        this.f$0.updateSatelliteBlacklist();
    }
}
