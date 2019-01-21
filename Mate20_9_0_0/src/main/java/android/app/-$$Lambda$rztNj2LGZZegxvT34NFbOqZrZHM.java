package android.app;

import android.app.KeyguardManager.KeyguardDismissCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$rztNj2LGZZegxvT34NFbOqZrZHM implements Runnable {
    private final /* synthetic */ KeyguardDismissCallback f$0;

    public /* synthetic */ -$$Lambda$rztNj2LGZZegxvT34NFbOqZrZHM(KeyguardDismissCallback keyguardDismissCallback) {
        this.f$0 = keyguardDismissCallback;
    }

    public final void run() {
        this.f$0.onDismissError();
    }
}
