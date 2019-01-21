package android.content.pm.dex;

import android.os.ParcelFileDescriptor;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ArtManager$SnapshotRuntimeProfileCallbackDelegate$OOdGv4iFxuVpH2kzFMr8KwX3X8s implements Runnable {
    private final /* synthetic */ SnapshotRuntimeProfileCallbackDelegate f$0;
    private final /* synthetic */ ParcelFileDescriptor f$1;

    public /* synthetic */ -$$Lambda$ArtManager$SnapshotRuntimeProfileCallbackDelegate$OOdGv4iFxuVpH2kzFMr8KwX3X8s(SnapshotRuntimeProfileCallbackDelegate snapshotRuntimeProfileCallbackDelegate, ParcelFileDescriptor parcelFileDescriptor) {
        this.f$0 = snapshotRuntimeProfileCallbackDelegate;
        this.f$1 = parcelFileDescriptor;
    }

    public final void run() {
        this.f$0.mCallback.onSuccess(this.f$1);
    }
}
