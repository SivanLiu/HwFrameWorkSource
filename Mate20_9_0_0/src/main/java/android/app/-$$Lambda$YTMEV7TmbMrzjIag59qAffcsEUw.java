package android.app;

import android.app.KeyguardManager.KeyguardDismissCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$YTMEV7TmbMrzjIag59qAffcsEUw implements Runnable {
    private final /* synthetic */ KeyguardDismissCallback f$0;

    public /* synthetic */ -$$Lambda$YTMEV7TmbMrzjIag59qAffcsEUw(KeyguardDismissCallback keyguardDismissCallback) {
        this.f$0 = keyguardDismissCallback;
    }

    public final void run() {
        this.f$0.onDismissSucceeded();
    }
}
