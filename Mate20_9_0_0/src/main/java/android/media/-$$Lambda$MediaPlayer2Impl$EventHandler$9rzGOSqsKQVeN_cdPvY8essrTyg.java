package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$9rzGOSqsKQVeN_cdPvY8essrTyg implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$9rzGOSqsKQVeN_cdPvY8essrTyg(EventHandler eventHandler, Pair pair) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onInfo(this.f$0.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, 5, 0);
    }
}
