package com.android.server.power.batterysaver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FileUpdater$NUmipjKCJwbgmFbIcGS3uaz3QFk implements Runnable {
    private final /* synthetic */ FileUpdater f$0;

    public /* synthetic */ -$$Lambda$FileUpdater$NUmipjKCJwbgmFbIcGS3uaz3QFk(FileUpdater fileUpdater) {
        this.f$0 = fileUpdater;
    }

    public final void run() {
        this.f$0.handleWriteOnHandler();
    }
}
