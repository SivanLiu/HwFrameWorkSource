package com.android.server.wm;

import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$NonAppWindowContainers$nqCymC3xR9b3qaeohnnJJpSiajc implements Comparator {
    private final /* synthetic */ NonAppWindowContainers f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$NonAppWindowContainers$nqCymC3xR9b3qaeohnnJJpSiajc(NonAppWindowContainers nonAppWindowContainers) {
        this.f$0 = nonAppWindowContainers;
    }

    public final int compare(Object obj, Object obj2) {
        return NonAppWindowContainers.lambda$new$0(this.f$0, (WindowToken) obj, (WindowToken) obj2);
    }
}
