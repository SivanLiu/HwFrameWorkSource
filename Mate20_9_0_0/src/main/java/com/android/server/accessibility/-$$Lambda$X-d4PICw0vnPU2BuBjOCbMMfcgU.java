package com.android.server.accessibility;

import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$X-d4PICw0vnPU2BuBjOCbMMfcgU implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$X-d4PICw0vnPU2BuBjOCbMMfcgU INSTANCE = new -$$Lambda$X-d4PICw0vnPU2BuBjOCbMMfcgU();

    private /* synthetic */ -$$Lambda$X-d4PICw0vnPU2BuBjOCbMMfcgU() {
    }

    public final void accept(Object obj, Object obj2) {
        ((AccessibilityManagerService) obj).clearAccessibilityFocus((IntSupplier) obj2);
    }
}
