package huawei.android.widget.effect.engine;

import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwBlurEngine$mT50yEF0clUy9ydDQRLag1Qem98 implements OnGlobalLayoutListener {
    private final /* synthetic */ HwBlurEngine f$0;

    public /* synthetic */ -$$Lambda$HwBlurEngine$mT50yEF0clUy9ydDQRLag1Qem98(HwBlurEngine hwBlurEngine) {
        this.f$0 = hwBlurEngine;
    }

    public final void onGlobalLayout() {
        this.f$0.updateUnionArea();
    }
}
