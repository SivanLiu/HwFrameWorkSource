package android.app;

import android.app.VrManager.CallbackEntry.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VrManager$CallbackEntry$2$KvHLIXm3-7igcOqTEl46YdjhHMk implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$VrManager$CallbackEntry$2$KvHLIXm3-7igcOqTEl46YdjhHMk(AnonymousClass2 anonymousClass2, boolean z) {
        this.f$0 = anonymousClass2;
        this.f$1 = z;
    }

    public final void run() {
        CallbackEntry.this.mCallback.onPersistentVrStateChanged(this.f$1);
    }
}
