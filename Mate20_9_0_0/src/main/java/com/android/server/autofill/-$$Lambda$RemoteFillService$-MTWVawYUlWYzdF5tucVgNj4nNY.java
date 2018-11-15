package com.android.server.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$-MTWVawYUlWYzdF5tucVgNj4nNY implements Runnable {
    private final /* synthetic */ RemoteFillService f$0;
    private final /* synthetic */ PendingRequest f$1;
    private final /* synthetic */ CharSequence f$2;

    public /* synthetic */ -$$Lambda$RemoteFillService$-MTWVawYUlWYzdF5tucVgNj4nNY(RemoteFillService remoteFillService, PendingRequest pendingRequest, CharSequence charSequence) {
        this.f$0 = remoteFillService;
        this.f$1 = pendingRequest;
        this.f$2 = charSequence;
    }

    public final void run() {
        RemoteFillService.lambda$dispatchOnSaveRequestFailure$5(this.f$0, this.f$1, this.f$2);
    }
}
