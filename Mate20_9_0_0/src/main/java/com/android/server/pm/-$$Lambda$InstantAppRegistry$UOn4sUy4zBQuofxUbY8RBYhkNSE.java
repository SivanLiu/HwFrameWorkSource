package com.android.server.pm;

import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$InstantAppRegistry$UOn4sUy4zBQuofxUbY8RBYhkNSE implements Comparator {
    private final /* synthetic */ InstantAppRegistry f$0;

    public /* synthetic */ -$$Lambda$InstantAppRegistry$UOn4sUy4zBQuofxUbY8RBYhkNSE(InstantAppRegistry instantAppRegistry) {
        this.f$0 = instantAppRegistry;
    }

    public final int compare(Object obj, Object obj2) {
        return InstantAppRegistry.lambda$pruneInstantApps$2(this.f$0, (String) obj, (String) obj2);
    }
}
