package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.util.ArraySet;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerServiceUtils$nPt0Hym3GvYeWA2vwfOLFDxZmCE implements Predicate {
    private final /* synthetic */ ArraySet f$0;

    public /* synthetic */ -$$Lambda$PackageManagerServiceUtils$nPt0Hym3GvYeWA2vwfOLFDxZmCE(ArraySet arraySet) {
        this.f$0 = arraySet;
    }

    public final boolean test(Object obj) {
        return this.f$0.contains(((Package) obj).packageName);
    }
}
