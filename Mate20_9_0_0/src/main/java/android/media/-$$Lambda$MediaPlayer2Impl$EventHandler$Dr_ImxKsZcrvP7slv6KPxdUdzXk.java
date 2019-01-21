package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$Dr_ImxKsZcrvP7slv6KPxdUdzXk implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$Dr_ImxKsZcrvP7slv6KPxdUdzXk(EventHandler eventHandler, Pair pair, int i) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
        this.f$2 = i;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onInfo(this.f$0.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, this.f$2);
    }
}
