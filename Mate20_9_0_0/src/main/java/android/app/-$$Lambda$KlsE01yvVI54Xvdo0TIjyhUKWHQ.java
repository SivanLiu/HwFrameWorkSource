package android.app;

import android.app.KeyguardManager.KeyguardDismissCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KlsE01yvVI54Xvdo0TIjyhUKWHQ implements Runnable {
    private final /* synthetic */ KeyguardDismissCallback f$0;

    public /* synthetic */ -$$Lambda$KlsE01yvVI54Xvdo0TIjyhUKWHQ(KeyguardDismissCallback keyguardDismissCallback) {
        this.f$0 = keyguardDismissCallback;
    }

    public final void run() {
        this.f$0.onDismissCancelled();
    }
}
