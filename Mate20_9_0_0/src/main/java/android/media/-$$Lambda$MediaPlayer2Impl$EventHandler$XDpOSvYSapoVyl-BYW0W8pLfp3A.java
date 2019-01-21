package android.media;

import android.media.MediaPlayer2.DrmEventCallback;
import android.media.MediaPlayer2Impl.DrmInfoImpl;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$XDpOSvYSapoVyl-BYW0W8pLfp3A implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ DrmInfoImpl f$2;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$XDpOSvYSapoVyl-BYW0W8pLfp3A(EventHandler eventHandler, Pair pair, DrmInfoImpl drmInfoImpl) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
        this.f$2 = drmInfoImpl;
    }

    public final void run() {
        ((DrmEventCallback) this.f$1.second).onDrmInfo(this.f$0.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, this.f$2);
    }
}
