package com.android.server.wm;

import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$TaskStackContainers$rQnI0Y8R9ptQ09cGHwbCHDiG2FY implements Consumer {
    private final /* synthetic */ ArrayList f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$TaskStackContainers$rQnI0Y8R9ptQ09cGHwbCHDiG2FY(ArrayList arrayList) {
        this.f$0 = arrayList;
    }

    public final void accept(Object obj) {
        TaskStackContainers.lambda$getVisibleTasks$0(this.f$0, (Task) obj);
    }
}
