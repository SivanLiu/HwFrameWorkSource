package com.android.server.wm;

import android.view.DisplayCutout;
import com.android.server.wm.utils.RotationCache.RotationDependentComputation;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$fiC19lMy-d_-rvza7hhOSw6bOM8 implements RotationDependentComputation {
    private final /* synthetic */ DisplayContent f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$fiC19lMy-d_-rvza7hhOSw6bOM8(DisplayContent displayContent) {
        this.f$0 = displayContent;
    }

    public final Object compute(Object obj, int i) {
        return this.f$0.calculateDisplayCutoutForRotationUncached((DisplayCutout) obj, i);
    }
}
