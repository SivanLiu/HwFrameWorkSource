package com.android.server.backup;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$pM_c5tVAGDtxjxLF_ONtACWWq6Q implements Runnable {
    private final /* synthetic */ TransportManager f$0;

    public /* synthetic */ -$$Lambda$pM_c5tVAGDtxjxLF_ONtACWWq6Q(TransportManager transportManager) {
        this.f$0 = transportManager;
    }

    public final void run() {
        this.f$0.registerTransports();
    }
}
