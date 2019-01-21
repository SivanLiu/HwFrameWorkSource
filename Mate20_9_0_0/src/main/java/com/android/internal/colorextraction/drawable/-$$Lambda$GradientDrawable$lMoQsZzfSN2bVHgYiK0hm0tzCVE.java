package com.android.internal.colorextraction.drawable;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GradientDrawable$lMoQsZzfSN2bVHgYiK0hm0tzCVE implements AnimatorUpdateListener {
    private final /* synthetic */ GradientDrawable f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ int f$4;

    public /* synthetic */ -$$Lambda$GradientDrawable$lMoQsZzfSN2bVHgYiK0hm0tzCVE(GradientDrawable gradientDrawable, int i, int i2, int i3, int i4) {
        this.f$0 = gradientDrawable;
        this.f$1 = i;
        this.f$2 = i2;
        this.f$3 = i3;
        this.f$4 = i4;
    }

    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        GradientDrawable.lambda$setColors$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, valueAnimator);
    }
}
