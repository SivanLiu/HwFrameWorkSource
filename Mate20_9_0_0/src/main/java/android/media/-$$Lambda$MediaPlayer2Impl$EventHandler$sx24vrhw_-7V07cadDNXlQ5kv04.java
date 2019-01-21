package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$EventHandler$sx24vrhw_-7V07cadDNXlQ5kv04 implements Runnable {
    private final /* synthetic */ EventHandler f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ TimedText f$2;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$EventHandler$sx24vrhw_-7V07cadDNXlQ5kv04(EventHandler eventHandler, Pair pair, TimedText timedText) {
        this.f$0 = eventHandler;
        this.f$1 = pair;
        this.f$2 = timedText;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onTimedText(this.f$0.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, this.f$2);
    }
}
