package android.widget;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SmartSelectSprite$2pck5xTffRWoiD4l_tkO_IIf5iM implements AnimatorUpdateListener {
    private final /* synthetic */ SmartSelectSprite f$0;

    public /* synthetic */ -$$Lambda$SmartSelectSprite$2pck5xTffRWoiD4l_tkO_IIf5iM(SmartSelectSprite smartSelectSprite) {
        this.f$0 = smartSelectSprite;
    }

    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.f$0.mInvalidator.run();
    }
}
