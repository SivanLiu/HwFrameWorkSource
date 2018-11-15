package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RecentsAnimation$1UHkVDWv9CBej8qt8TWQICpmP60 implements Runnable {
    private final /* synthetic */ RecentsAnimation f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$RecentsAnimation$1UHkVDWv9CBej8qt8TWQICpmP60(RecentsAnimation recentsAnimation, int i) {
        this.f$0 = recentsAnimation;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.finishAnimation(this.f$1);
    }
}
