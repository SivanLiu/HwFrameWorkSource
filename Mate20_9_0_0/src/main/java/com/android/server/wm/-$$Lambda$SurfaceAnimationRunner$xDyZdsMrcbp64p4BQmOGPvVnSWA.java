package com.android.server.wm;

import android.view.Choreographer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SurfaceAnimationRunner$xDyZdsMrcbp64p4BQmOGPvVnSWA implements Runnable {
    private final /* synthetic */ SurfaceAnimationRunner f$0;

    public /* synthetic */ -$$Lambda$SurfaceAnimationRunner$xDyZdsMrcbp64p4BQmOGPvVnSWA(SurfaceAnimationRunner surfaceAnimationRunner) {
        this.f$0 = surfaceAnimationRunner;
    }

    public final void run() {
        this.f$0.mChoreographer = Choreographer.getSfInstance();
    }
}
