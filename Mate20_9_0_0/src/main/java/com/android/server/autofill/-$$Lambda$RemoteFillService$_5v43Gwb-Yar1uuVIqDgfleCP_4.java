package com.android.server.autofill;

import android.service.autofill.FillResponse;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$_5v43Gwb-Yar1uuVIqDgfleCP_4 implements Runnable {
    private final /* synthetic */ RemoteFillService f$0;
    private final /* synthetic */ PendingFillRequest f$1;
    private final /* synthetic */ FillResponse f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$RemoteFillService$_5v43Gwb-Yar1uuVIqDgfleCP_4(RemoteFillService remoteFillService, PendingFillRequest pendingFillRequest, FillResponse fillResponse, int i) {
        this.f$0 = remoteFillService;
        this.f$1 = pendingFillRequest;
        this.f$2 = fillResponse;
        this.f$3 = i;
    }

    public final void run() {
        RemoteFillService.lambda$dispatchOnFillRequestSuccess$0(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
