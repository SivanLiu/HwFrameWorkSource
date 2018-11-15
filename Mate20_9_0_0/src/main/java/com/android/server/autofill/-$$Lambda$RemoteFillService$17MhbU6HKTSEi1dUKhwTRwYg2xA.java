package com.android.server.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$17MhbU6HKTSEi1dUKhwTRwYg2xA implements Runnable {
    private final /* synthetic */ RemoteFillService f$0;
    private final /* synthetic */ PendingFillRequest f$1;
    private final /* synthetic */ CharSequence f$2;

    public /* synthetic */ -$$Lambda$RemoteFillService$17MhbU6HKTSEi1dUKhwTRwYg2xA(RemoteFillService remoteFillService, PendingFillRequest pendingFillRequest, CharSequence charSequence) {
        this.f$0 = remoteFillService;
        this.f$1 = pendingFillRequest;
        this.f$2 = charSequence;
    }

    public final void run() {
        RemoteFillService.lambda$dispatchOnFillRequestFailure$1(this.f$0, this.f$1, this.f$2);
    }
}
