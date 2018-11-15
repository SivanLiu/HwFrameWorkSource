package com.android.server.devicepolicy;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkLoggingHandler$VKC_fB9Ws13yQKJ8zNkiF3Wp0Jk implements Runnable {
    private final /* synthetic */ NetworkLoggingHandler f$0;
    private final /* synthetic */ long f$1;

    public /* synthetic */ -$$Lambda$NetworkLoggingHandler$VKC_fB9Ws13yQKJ8zNkiF3Wp0Jk(NetworkLoggingHandler networkLoggingHandler, long j) {
        this.f$0 = networkLoggingHandler;
        this.f$1 = j;
    }

    public final void run() {
        NetworkLoggingHandler.lambda$retrieveFullLogBatch$0(this.f$0, this.f$1);
    }
}
