package com.android.server.autofill;

import android.os.ICancellationSignal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$lfx4anCMpwM99MhvsITDjU9sFRA implements Runnable {
    private final /* synthetic */ ICancellationSignal f$0;

    public /* synthetic */ -$$Lambda$RemoteFillService$lfx4anCMpwM99MhvsITDjU9sFRA(ICancellationSignal iCancellationSignal) {
        this.f$0 = iCancellationSignal;
    }

    public final void run() {
        RemoteFillService.lambda$dispatchOnFillTimeout$3(this.f$0);
    }
}
