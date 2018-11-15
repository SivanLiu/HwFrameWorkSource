package com.android.server.wm;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SurfaceAnimationRunner$puhYAP5tF0mSSJva-eUz59HnrkA implements AnimatorUpdateListener {
    private final /* synthetic */ SurfaceAnimationRunner f$0;
    private final /* synthetic */ RunningAnimation f$1;
    private final /* synthetic */ ValueAnimator f$2;

    public /* synthetic */ -$$Lambda$SurfaceAnimationRunner$puhYAP5tF0mSSJva-eUz59HnrkA(SurfaceAnimationRunner surfaceAnimationRunner, RunningAnimation runningAnimation, ValueAnimator valueAnimator) {
        this.f$0 = surfaceAnimationRunner;
        this.f$1 = runningAnimation;
        this.f$2 = valueAnimator;
    }

    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        SurfaceAnimationRunner.lambda$startAnimationLocked$3(this.f$0, this.f$1, this.f$2, valueAnimator);
    }
}
