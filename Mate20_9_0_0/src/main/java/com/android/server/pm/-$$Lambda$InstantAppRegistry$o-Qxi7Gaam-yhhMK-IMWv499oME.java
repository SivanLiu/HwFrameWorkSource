package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$InstantAppRegistry$o-Qxi7Gaam-yhhMK-IMWv499oME implements Predicate {
    private final /* synthetic */ Package f$0;

    public /* synthetic */ -$$Lambda$InstantAppRegistry$o-Qxi7Gaam-yhhMK-IMWv499oME(Package packageR) {
        this.f$0 = packageR;
    }

    public final boolean test(Object obj) {
        return ((UninstalledInstantAppState) obj).mInstantAppInfo.getPackageName().equals(this.f$0.packageName);
    }
}
