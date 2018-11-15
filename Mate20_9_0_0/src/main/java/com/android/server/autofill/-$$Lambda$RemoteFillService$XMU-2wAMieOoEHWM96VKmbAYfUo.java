package com.android.server.autofill;

import android.content.IntentSender;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$XMU-2wAMieOoEHWM96VKmbAYfUo implements Runnable {
    private final /* synthetic */ RemoteFillService f$0;
    private final /* synthetic */ PendingRequest f$1;
    private final /* synthetic */ IntentSender f$2;

    public /* synthetic */ -$$Lambda$RemoteFillService$XMU-2wAMieOoEHWM96VKmbAYfUo(RemoteFillService remoteFillService, PendingRequest pendingRequest, IntentSender intentSender) {
        this.f$0 = remoteFillService;
        this.f$1 = pendingRequest;
        this.f$2 = intentSender;
    }

    public final void run() {
        RemoteFillService.lambda$dispatchOnSaveRequestSuccess$4(this.f$0, this.f$1, this.f$2);
    }
}
