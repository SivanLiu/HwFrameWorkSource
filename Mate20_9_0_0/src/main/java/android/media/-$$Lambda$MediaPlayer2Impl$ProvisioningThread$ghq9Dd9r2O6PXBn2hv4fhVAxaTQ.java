package android.media;

import android.media.MediaPlayer2.DrmEventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$ProvisioningThread$ghq9Dd9r2O6PXBn2hv4fhVAxaTQ implements Runnable {
    private final /* synthetic */ ProvisioningThread f$0;
    private final /* synthetic */ Pair f$1;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$ProvisioningThread$ghq9Dd9r2O6PXBn2hv4fhVAxaTQ(ProvisioningThread provisioningThread, Pair pair) {
        this.f$0 = provisioningThread;
        this.f$1 = pair;
    }

    public final void run() {
        ((DrmEventCallback) this.f$1.second).onDrmPrepared(this.f$0.mediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, this.f$0.status);
    }
}
