package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$5DmGtkuYQXExyXOBI9Qvu64NQ68 implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ TimedMetaData f$2;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$5DmGtkuYQXExyXOBI9Qvu64NQ68(EventHandler eventHandler, Pair pair, TimedMetaData timedMetaData) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
        this.f$2 = timedMetaData;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onTimedMetaDataAvailable(this.f$0.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, this.f$2);
    }
}
