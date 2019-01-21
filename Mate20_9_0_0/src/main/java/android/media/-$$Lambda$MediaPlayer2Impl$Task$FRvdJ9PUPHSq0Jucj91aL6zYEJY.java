package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$Task$FRvdJ9PUPHSq0Jucj91aL6zYEJY implements Runnable {
    private final /* synthetic */ Task f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$Task$FRvdJ9PUPHSq0Jucj91aL6zYEJY(Task task, Pair pair, int i) {
        this.f$0 = task;
        this.f$1 = pair;
        this.f$2 = i;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onCallCompleted(MediaPlayer2Impl.this, this.f$0.mDSD, this.f$0.mMediaCallType, this.f$2);
    }
}
