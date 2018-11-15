package com.android.server.content;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$6y-gkGdDn-rSLmR9G8Pz_n9zy2A implements Runnable {
    private final /* synthetic */ SyncManager f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$SyncManager$6y-gkGdDn-rSLmR9G8Pz_n9zy2A(SyncManager syncManager, int i) {
        this.f$0 = syncManager;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mLogger.log("onUnlockUser: user=", Integer.valueOf(this.f$1));
    }
}
