package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RecentsAnimationController$4jQqaDgSmtGCjbUJiVoDh_jr9rY implements Runnable {
    private final /* synthetic */ RecentsAnimationController f$0;

    public /* synthetic */ -$$Lambda$RecentsAnimationController$4jQqaDgSmtGCjbUJiVoDh_jr9rY(RecentsAnimationController recentsAnimationController) {
        this.f$0 = recentsAnimationController;
    }

    public final void run() {
        this.f$0.cancelAnimation(2, "failSafeRunnable");
    }
}
