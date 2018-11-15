package com.android.server.wm;

import android.util.SparseIntArray;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$utugHDPHgMp2b3JwigOH_-Y0P1Q implements Consumer {
    private final /* synthetic */ RootWindowContainer f$0;
    private final /* synthetic */ SparseIntArray f$1;

    public /* synthetic */ -$$Lambda$RootWindowContainer$utugHDPHgMp2b3JwigOH_-Y0P1Q(RootWindowContainer rootWindowContainer, SparseIntArray sparseIntArray) {
        this.f$0 = rootWindowContainer;
        this.f$1 = sparseIntArray;
    }

    public final void accept(Object obj) {
        RootWindowContainer.lambda$reclaimSomeSurfaceMemory$7(this.f$0, this.f$1, (WindowState) obj);
    }
}
