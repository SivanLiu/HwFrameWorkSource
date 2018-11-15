package com.android.server.wm;

import android.animation.ValueAnimator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SurfaceAnimationRunner$we7K92eAl3biB_bzyqbv5xCmasE implements AnimatorFactory {
    private final /* synthetic */ SurfaceAnimationRunner f$0;

    public /* synthetic */ -$$Lambda$SurfaceAnimationRunner$we7K92eAl3biB_bzyqbv5xCmasE(SurfaceAnimationRunner surfaceAnimationRunner) {
        this.f$0 = surfaceAnimationRunner;
    }

    public final ValueAnimator makeAnimator() {
        return new SfValueAnimator();
    }
}
