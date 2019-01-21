package android.app;

import android.app.VrManager.CallbackEntry.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VrManager$CallbackEntry$1$rgUBVVG1QhelpvAp8W3UQHDHJdU implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$VrManager$CallbackEntry$1$rgUBVVG1QhelpvAp8W3UQHDHJdU(AnonymousClass1 anonymousClass1, boolean z) {
        this.f$0 = anonymousClass1;
        this.f$1 = z;
    }

    public final void run() {
        CallbackEntry.this.mCallback.onVrStateChanged(this.f$1);
    }
}
