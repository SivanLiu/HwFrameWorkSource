package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LocalAnimationAdapter$X--EomqUvw4qy89IeeTFTH7aCMo implements Runnable {
    private final /* synthetic */ LocalAnimationAdapter f$0;
    private final /* synthetic */ OnAnimationFinishedCallback f$1;

    public /* synthetic */ -$$Lambda$LocalAnimationAdapter$X--EomqUvw4qy89IeeTFTH7aCMo(LocalAnimationAdapter localAnimationAdapter, OnAnimationFinishedCallback onAnimationFinishedCallback) {
        this.f$0 = localAnimationAdapter;
        this.f$1 = onAnimationFinishedCallback;
    }

    public final void run() {
        this.f$1.onAnimationFinished(this.f$0);
    }
}
