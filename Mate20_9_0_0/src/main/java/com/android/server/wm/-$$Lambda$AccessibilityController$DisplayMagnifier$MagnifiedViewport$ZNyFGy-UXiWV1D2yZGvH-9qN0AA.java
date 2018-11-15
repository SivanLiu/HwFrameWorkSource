package com.android.server.wm;

import android.util.SparseArray;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityController$DisplayMagnifier$MagnifiedViewport$ZNyFGy-UXiWV1D2yZGvH-9qN0AA implements Consumer {
    private final /* synthetic */ MagnifiedViewport f$0;
    private final /* synthetic */ SparseArray f$1;

    public /* synthetic */ -$$Lambda$AccessibilityController$DisplayMagnifier$MagnifiedViewport$ZNyFGy-UXiWV1D2yZGvH-9qN0AA(MagnifiedViewport magnifiedViewport, SparseArray sparseArray) {
        this.f$0 = magnifiedViewport;
        this.f$1 = sparseArray;
    }

    public final void accept(Object obj) {
        MagnifiedViewport.lambda$populateWindowsOnScreenLocked$0(this.f$0, this.f$1, (WindowState) obj);
    }
}
