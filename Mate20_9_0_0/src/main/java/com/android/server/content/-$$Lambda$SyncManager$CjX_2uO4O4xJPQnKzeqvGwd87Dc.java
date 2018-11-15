package com.android.server.content;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$CjX_2uO4O4xJPQnKzeqvGwd87Dc implements Runnable {
    private final /* synthetic */ SyncManager f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$SyncManager$CjX_2uO4O4xJPQnKzeqvGwd87Dc(SyncManager syncManager, int i) {
        this.f$0 = syncManager;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mLogger.log("onStartUser: user=", Integer.valueOf(this.f$1));
    }
}
