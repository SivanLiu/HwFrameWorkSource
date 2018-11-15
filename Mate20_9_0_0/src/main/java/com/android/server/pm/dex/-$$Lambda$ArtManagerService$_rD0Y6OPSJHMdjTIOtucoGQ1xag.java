package com.android.server.pm.dex;

import android.content.pm.dex.ISnapshotRuntimeProfileCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ArtManagerService$_rD0Y6OPSJHMdjTIOtucoGQ1xag implements Runnable {
    private final /* synthetic */ ISnapshotRuntimeProfileCallback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$ArtManagerService$_rD0Y6OPSJHMdjTIOtucoGQ1xag(ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback, int i, String str) {
        this.f$0 = iSnapshotRuntimeProfileCallback;
        this.f$1 = i;
        this.f$2 = str;
    }

    public final void run() {
        ArtManagerService.lambda$postError$0(this.f$0, this.f$1, this.f$2);
    }
}
