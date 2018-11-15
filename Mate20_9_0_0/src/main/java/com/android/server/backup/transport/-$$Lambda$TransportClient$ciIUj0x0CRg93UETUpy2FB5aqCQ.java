package com.android.server.backup.transport;

import com.android.internal.backup.IBackupTransport;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TransportClient$ciIUj0x0CRg93UETUpy2FB5aqCQ implements Runnable {
    private final /* synthetic */ TransportClient f$0;
    private final /* synthetic */ TransportConnectionListener f$1;
    private final /* synthetic */ IBackupTransport f$2;

    public /* synthetic */ -$$Lambda$TransportClient$ciIUj0x0CRg93UETUpy2FB5aqCQ(TransportClient transportClient, TransportConnectionListener transportConnectionListener, IBackupTransport iBackupTransport) {
        this.f$0 = transportClient;
        this.f$1 = transportConnectionListener;
        this.f$2 = iBackupTransport;
    }

    public final void run() {
        this.f$1.onTransportConnectionResult(this.f$2, this.f$0);
    }
}
