package com.android.server.display;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AmbientBrightnessStatsTracker$vQZYn_dAhbvzT-Un4vvpuyIATII implements Clock {
    private final /* synthetic */ AmbientBrightnessStatsTracker f$0;

    public /* synthetic */ -$$Lambda$AmbientBrightnessStatsTracker$vQZYn_dAhbvzT-Un4vvpuyIATII(AmbientBrightnessStatsTracker ambientBrightnessStatsTracker) {
        this.f$0 = ambientBrightnessStatsTracker;
    }

    public final long elapsedTimeMillis() {
        return this.f$0.mInjector.elapsedRealtimeMillis();
    }
}
