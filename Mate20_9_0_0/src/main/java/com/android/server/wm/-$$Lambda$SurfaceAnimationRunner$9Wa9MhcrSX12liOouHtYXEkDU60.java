package com.android.server.wm;

import android.view.Choreographer.FrameCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60 implements FrameCallback {
    private final /* synthetic */ SurfaceAnimationRunner f$0;

    public /* synthetic */ -$$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(SurfaceAnimationRunner surfaceAnimationRunner) {
        this.f$0 = surfaceAnimationRunner;
    }

    public final void doFrame(long j) {
        this.f$0.startAnimations(j);
    }
}
