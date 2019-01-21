package android.media;

import android.media.AudioManager.AudioServerStateCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AudioManager$4$7k7uSoMGULBCueASQSmf9jAil7I implements Runnable {
    private final /* synthetic */ AudioServerStateCallback f$0;

    public /* synthetic */ -$$Lambda$AudioManager$4$7k7uSoMGULBCueASQSmf9jAil7I(AudioServerStateCallback audioServerStateCallback) {
        this.f$0 = audioServerStateCallback;
    }

    public final void run() {
        this.f$0.onAudioServerDown();
    }
}
