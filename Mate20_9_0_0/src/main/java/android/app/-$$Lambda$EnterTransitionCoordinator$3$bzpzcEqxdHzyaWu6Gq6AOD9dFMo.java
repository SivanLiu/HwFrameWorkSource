package android.app;

import android.app.EnterTransitionCoordinator.AnonymousClass3;
import android.os.Bundle;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EnterTransitionCoordinator$3$bzpzcEqxdHzyaWu6Gq6AOD9dFMo implements Runnable {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ Bundle f$1;

    public /* synthetic */ -$$Lambda$EnterTransitionCoordinator$3$bzpzcEqxdHzyaWu6Gq6AOD9dFMo(AnonymousClass3 anonymousClass3, Bundle bundle) {
        this.f$0 = anonymousClass3;
        this.f$1 = bundle;
    }

    public final void run() {
        EnterTransitionCoordinator.this.startSharedElementTransition(this.f$1);
    }
}
