package com.android.server.utils;

import com.android.server.utils.ManagedApplicationService.AnonymousClass1;
import com.android.server.utils.ManagedApplicationService.LogEvent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ManagedApplicationService$1$iBg5-L6PAieAfuWNXxIPqvSlAAg implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ long f$1;

    public /* synthetic */ -$$Lambda$ManagedApplicationService$1$iBg5-L6PAieAfuWNXxIPqvSlAAg(AnonymousClass1 anonymousClass1, long j) {
        this.f$0 = anonymousClass1;
        this.f$1 = j;
    }

    public final void run() {
        ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(this.f$1, ManagedApplicationService.this.mComponent, 2));
    }
}
