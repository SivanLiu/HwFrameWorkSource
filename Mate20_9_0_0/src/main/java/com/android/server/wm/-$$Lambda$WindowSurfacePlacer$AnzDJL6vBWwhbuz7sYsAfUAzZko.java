package com.android.server.wm;

import android.util.ArraySet;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowSurfacePlacer$AnzDJL6vBWwhbuz7sYsAfUAzZko implements Predicate {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ ArraySet f$1;

    public /* synthetic */ -$$Lambda$WindowSurfacePlacer$AnzDJL6vBWwhbuz7sYsAfUAzZko(int i, ArraySet arraySet) {
        this.f$0 = i;
        this.f$1 = arraySet;
    }

    public final boolean test(Object obj) {
        return WindowSurfacePlacer.lambda$findAnimLayoutParamsToken$1(this.f$0, this.f$1, (AppWindowToken) obj);
    }
}
