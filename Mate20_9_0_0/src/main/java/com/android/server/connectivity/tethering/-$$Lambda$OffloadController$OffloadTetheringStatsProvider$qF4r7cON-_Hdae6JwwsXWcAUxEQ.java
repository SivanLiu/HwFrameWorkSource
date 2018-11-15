package com.android.server.connectivity.tethering;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OffloadController$OffloadTetheringStatsProvider$qF4r7cON-_Hdae6JwwsXWcAUxEQ implements Runnable {
    private final /* synthetic */ OffloadTetheringStatsProvider f$0;
    private final /* synthetic */ long f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$OffloadController$OffloadTetheringStatsProvider$qF4r7cON-_Hdae6JwwsXWcAUxEQ(OffloadTetheringStatsProvider offloadTetheringStatsProvider, long j, String str) {
        this.f$0 = offloadTetheringStatsProvider;
        this.f$1 = j;
        this.f$2 = str;
    }

    public final void run() {
        OffloadTetheringStatsProvider.lambda$setInterfaceQuota$1(this.f$0, this.f$1, this.f$2);
    }
}
