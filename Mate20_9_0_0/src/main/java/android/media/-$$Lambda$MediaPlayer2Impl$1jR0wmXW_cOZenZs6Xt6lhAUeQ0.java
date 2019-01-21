package android.media;

import android.media.MediaPlayer2.DrmEventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$1jR0wmXW_cOZenZs6Xt6lhAUeQ0 implements Runnable {
    private final /* synthetic */ MediaPlayer2Impl f$0;
    private final /* synthetic */ Pair f$1;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$1jR0wmXW_cOZenZs6Xt6lhAUeQ0(MediaPlayer2Impl mediaPlayer2Impl, Pair pair) {
        this.f$0 = mediaPlayer2Impl;
        this.f$1 = pair;
    }

    public final void run() {
        ((DrmEventCallback) this.f$1.second).onDrmPrepared(this.f$0, this.f$0.mCurrentDSD, 0);
    }
}
