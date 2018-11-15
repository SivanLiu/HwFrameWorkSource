package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteAnimationController$uQS8vaPKQ-E3x_9G8NCxPQmw1fw implements Runnable {
    private final /* synthetic */ RemoteAnimationController f$0;

    public /* synthetic */ -$$Lambda$RemoteAnimationController$uQS8vaPKQ-E3x_9G8NCxPQmw1fw(RemoteAnimationController remoteAnimationController) {
        this.f$0 = remoteAnimationController;
    }

    public final void run() {
        this.f$0.cancelAnimation("timeoutRunnable");
    }
}
