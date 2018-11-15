package com.android.server.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$PKEfnjx72TG33VenAsL_32TGLPg implements Runnable {
    private final /* synthetic */ RemoteFillService f$0;
    private final /* synthetic */ PendingFillRequest f$1;

    public /* synthetic */ -$$Lambda$RemoteFillService$PKEfnjx72TG33VenAsL_32TGLPg(RemoteFillService remoteFillService, PendingFillRequest pendingFillRequest) {
        this.f$0 = remoteFillService;
        this.f$1 = pendingFillRequest;
    }

    public final void run() {
        RemoteFillService.lambda$dispatchOnFillRequestTimeout$2(this.f$0, this.f$1);
    }
}
