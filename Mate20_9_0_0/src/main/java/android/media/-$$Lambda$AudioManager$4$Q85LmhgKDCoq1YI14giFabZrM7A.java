package android.media;

import android.media.AudioManager.AudioServerStateCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AudioManager$4$Q85LmhgKDCoq1YI14giFabZrM7A implements Runnable {
    private final /* synthetic */ AudioServerStateCallback f$0;

    public /* synthetic */ -$$Lambda$AudioManager$4$Q85LmhgKDCoq1YI14giFabZrM7A(AudioServerStateCallback audioServerStateCallback) {
        this.f$0 = audioServerStateCallback;
    }

    public final void run() {
        this.f$0.onAudioServerUp();
    }
}
