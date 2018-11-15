package com.android.server.policy;

import com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$oXa0y3A-00RiQs6-KTPBgpkGtgw implements Runnable {
    private final /* synthetic */ WindowManagerFuncs f$0;

    public /* synthetic */ -$$Lambda$oXa0y3A-00RiQs6-KTPBgpkGtgw(WindowManagerFuncs windowManagerFuncs) {
        this.f$0 = windowManagerFuncs;
    }

    public final void run() {
        this.f$0.triggerAnimationFailsafe();
    }
}
