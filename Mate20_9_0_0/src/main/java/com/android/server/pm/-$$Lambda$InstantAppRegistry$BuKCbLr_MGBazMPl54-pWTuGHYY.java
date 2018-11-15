package com.android.server.pm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$InstantAppRegistry$BuKCbLr_MGBazMPl54-pWTuGHYY implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$InstantAppRegistry$BuKCbLr_MGBazMPl54-pWTuGHYY(long j) {
        this.f$0 = j;
    }

    public final boolean test(Object obj) {
        return InstantAppRegistry.lambda$pruneInstantApps$3(this.f$0, (UninstalledInstantAppState) obj);
    }
}
