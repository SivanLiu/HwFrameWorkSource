package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$hsCyoCNpv30l9tb7sOpVC4dnMy8 implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ DataSourceDesc f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$hsCyoCNpv30l9tb7sOpVC4dnMy8(EventHandler eventHandler, Pair pair, DataSourceDesc dataSourceDesc, int i) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
        this.f$2 = dataSourceDesc;
        this.f$3 = i;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onInfo(this.f$0.mMediaPlayer, this.f$2, MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, this.f$3);
    }
}
