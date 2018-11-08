package com.android.internal.colorextraction.drawable;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;

final /* synthetic */ class -$Lambda$D0plBYSeplKHUImgLxjOl14-7Rw implements AnimatorUpdateListener {
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ int -$f1;
    private final /* synthetic */ int -$f2;
    private final /* synthetic */ int -$f3;
    private final /* synthetic */ Object -$f4;

    private final /* synthetic */ void $m$0(ValueAnimator arg0) {
        ((GradientDrawable) this.-$f4).lambda$-com_android_internal_colorextraction_drawable_GradientDrawable_3291(this.-$f0, this.-$f1, this.-$f2, this.-$f3, arg0);
    }

    public /* synthetic */ -$Lambda$D0plBYSeplKHUImgLxjOl14-7Rw(int i, int i2, int i3, int i4, Object obj) {
        this.-$f0 = i;
        this.-$f1 = i2;
        this.-$f2 = i3;
        this.-$f3 = i4;
        this.-$f4 = obj;
    }

    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        $m$0(valueAnimator);
    }
}
