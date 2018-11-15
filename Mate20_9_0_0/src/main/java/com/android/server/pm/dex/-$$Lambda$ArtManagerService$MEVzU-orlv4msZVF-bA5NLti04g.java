package com.android.server.pm.dex;

import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.os.ParcelFileDescriptor;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ArtManagerService$MEVzU-orlv4msZVF-bA5NLti04g implements Runnable {
    private final /* synthetic */ ParcelFileDescriptor f$0;
    private final /* synthetic */ ISnapshotRuntimeProfileCallback f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$ArtManagerService$MEVzU-orlv4msZVF-bA5NLti04g(ParcelFileDescriptor parcelFileDescriptor, ISnapshotRuntimeProfileCallback iSnapshotRuntimeProfileCallback, String str) {
        this.f$0 = parcelFileDescriptor;
        this.f$1 = iSnapshotRuntimeProfileCallback;
        this.f$2 = str;
    }

    public final void run() {
        ArtManagerService.lambda$postSuccess$1(this.f$0, this.f$1, this.f$2);
    }
}
