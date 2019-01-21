package android.media;

import android.media.MediaPlayer2.MediaPlayer2EventCallback;
import android.media.MediaPlayer2Impl.AnonymousClass12;
import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaPlayer2Impl$12$GAwhcv62KlexkkYkbjb8-qEksjI implements Runnable {
    private final /* synthetic */ AnonymousClass12 f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ Object f$2;

    public /* synthetic */ -$$Lambda$MediaPlayer2Impl$12$GAwhcv62KlexkkYkbjb8-qEksjI(AnonymousClass12 anonymousClass12, Pair pair, Object obj) {
        this.f$0 = anonymousClass12;
        this.f$1 = pair;
        this.f$2 = obj;
    }

    public final void run() {
        ((MediaPlayer2EventCallback) this.f$1.second).onCommandLabelReached(MediaPlayer2Impl.this, this.f$2);
    }
}
