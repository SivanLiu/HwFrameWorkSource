package com.android.server.wm;

import android.view.Choreographer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowAnimator$U3Fu5_RzEyNo8Jt6zTb2ozdXiqM implements Runnable {
    private final /* synthetic */ WindowAnimator f$0;

    public /* synthetic */ -$$Lambda$WindowAnimator$U3Fu5_RzEyNo8Jt6zTb2ozdXiqM(WindowAnimator windowAnimator) {
        this.f$0 = windowAnimator;
    }

    public final void run() {
        this.f$0.mChoreographer = Choreographer.getSfInstance();
    }
}
