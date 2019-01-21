package android.content.pm.dex;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ArtManager$SnapshotRuntimeProfileCallbackDelegate$m2Wpsf6LxhWt_1tS6tQt3B8QcGo implements Runnable {
    private final /* synthetic */ SnapshotRuntimeProfileCallbackDelegate f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ArtManager$SnapshotRuntimeProfileCallbackDelegate$m2Wpsf6LxhWt_1tS6tQt3B8QcGo(SnapshotRuntimeProfileCallbackDelegate snapshotRuntimeProfileCallbackDelegate, int i) {
        this.f$0 = snapshotRuntimeProfileCallbackDelegate;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mCallback.onError(this.f$1);
    }
}
