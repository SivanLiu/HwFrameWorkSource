package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$iPmZQ0HxMVwbBcbhgpHbun3WGTk implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$iPmZQ0HxMVwbBcbhgpHbun3WGTk(EventHandler eventHandler, Pair pair, int i, int i2) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
        this.f$2 = i;
        this.f$3 = i2;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onInfo(this.f$0.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, this.f$2, this.f$3);
    }
}
