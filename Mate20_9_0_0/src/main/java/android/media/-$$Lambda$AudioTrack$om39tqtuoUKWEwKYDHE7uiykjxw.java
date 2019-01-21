package android.media;

import android.media.AudioTrack.StreamEventCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AudioTrack$om39tqtuoUKWEwKYDHE7uiykjxw implements Runnable {
    private final /* synthetic */ StreamEventCallback f$0;
    private final /* synthetic */ AudioTrack f$1;

    public /* synthetic */ -$$Lambda$AudioTrack$om39tqtuoUKWEwKYDHE7uiykjxw(StreamEventCallback streamEventCallback, AudioTrack audioTrack) {
        this.f$0 = streamEventCallback;
        this.f$1 = audioTrack;
    }

    public final void run() {
        this.f$0.onStreamPresentationEnd(this.f$1);
    }
}
