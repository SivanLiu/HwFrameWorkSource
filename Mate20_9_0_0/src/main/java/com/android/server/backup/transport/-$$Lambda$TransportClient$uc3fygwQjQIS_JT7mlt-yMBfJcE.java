package com.android.server.backup.transport;

import com.android.internal.backup.IBackupTransport;
import java.util.concurrent.CompletableFuture;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TransportClient$uc3fygwQjQIS_JT7mlt-yMBfJcE implements TransportConnectionListener {
    private final /* synthetic */ CompletableFuture f$0;

    public /* synthetic */ -$$Lambda$TransportClient$uc3fygwQjQIS_JT7mlt-yMBfJcE(CompletableFuture completableFuture) {
        this.f$0 = completableFuture;
    }

    public final void onTransportConnectionResult(IBackupTransport iBackupTransport, TransportClient transportClient) {
        this.f$0.complete(iBackupTransport);
    }
}
