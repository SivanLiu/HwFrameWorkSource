package com.android.server.wm;

import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$CIuXGvNhVwi8txA2L_PmZnPJavk implements Consumer {
    private final /* synthetic */ ArrayList f$0;

    public /* synthetic */ -$$Lambda$WindowManagerService$CIuXGvNhVwi8txA2L_PmZnPJavk(ArrayList arrayList) {
        this.f$0 = arrayList;
    }

    public final void accept(Object obj) {
        this.f$0.add((WindowState) obj);
    }
}
