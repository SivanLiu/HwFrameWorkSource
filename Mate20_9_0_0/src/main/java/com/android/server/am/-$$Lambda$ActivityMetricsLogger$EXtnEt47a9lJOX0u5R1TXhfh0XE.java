package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityMetricsLogger$EXtnEt47a9lJOX0u5R1TXhfh0XE implements Runnable {
    private final /* synthetic */ ActivityMetricsLogger f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ WindowingModeTransitionInfoSnapshot f$3;

    public /* synthetic */ -$$Lambda$ActivityMetricsLogger$EXtnEt47a9lJOX0u5R1TXhfh0XE(ActivityMetricsLogger activityMetricsLogger, int i, int i2, WindowingModeTransitionInfoSnapshot windowingModeTransitionInfoSnapshot) {
        this.f$0 = activityMetricsLogger;
        this.f$1 = i;
        this.f$2 = i2;
        this.f$3 = windowingModeTransitionInfoSnapshot;
    }

    public final void run() {
        this.f$0.logAppTransition(this.f$1, this.f$2, this.f$3);
    }
}
