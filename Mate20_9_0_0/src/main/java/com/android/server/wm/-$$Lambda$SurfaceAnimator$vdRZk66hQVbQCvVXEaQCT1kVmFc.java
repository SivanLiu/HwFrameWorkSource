package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SurfaceAnimator$vdRZk66hQVbQCvVXEaQCT1kVmFc implements OnAnimationFinishedCallback {
    private final /* synthetic */ SurfaceAnimator f$0;
    private final /* synthetic */ Runnable f$1;

    public /* synthetic */ -$$Lambda$SurfaceAnimator$vdRZk66hQVbQCvVXEaQCT1kVmFc(SurfaceAnimator surfaceAnimator, Runnable runnable) {
        this.f$0 = surfaceAnimator;
        this.f$1 = runnable;
    }

    public final void onAnimationFinished(AnimationAdapter animationAdapter) {
        SurfaceAnimator.lambda$getFinishedCallback$1(this.f$0, this.f$1, animationAdapter);
    }
}
