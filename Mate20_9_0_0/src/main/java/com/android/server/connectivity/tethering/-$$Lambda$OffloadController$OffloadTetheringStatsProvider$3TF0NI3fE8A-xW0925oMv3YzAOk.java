package com.android.server.connectivity.tethering;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadController$OffloadTetheringStatsProvider$3TF0NI3fE8A-xW0925oMv3YzAOk implements Runnable {
    private final /* synthetic */ OffloadTetheringStatsProvider f$0;

    public /* synthetic */ -$$Lambda$OffloadController$OffloadTetheringStatsProvider$3TF0NI3fE8A-xW0925oMv3YzAOk(OffloadTetheringStatsProvider offloadTetheringStatsProvider) {
        this.f$0 = offloadTetheringStatsProvider;
    }

    public final void run() {
        OffloadController.this.updateStatsForCurrentUpstream();
    }
}
