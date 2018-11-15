package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerServiceUtils$ePZ6rsJ05hJ2glmOqcq1_jX6J8w implements Comparator {
    public static final /* synthetic */ -$$Lambda$PackageManagerServiceUtils$ePZ6rsJ05hJ2glmOqcq1_jX6J8w INSTANCE = new -$$Lambda$PackageManagerServiceUtils$ePZ6rsJ05hJ2glmOqcq1_jX6J8w();

    private /* synthetic */ -$$Lambda$PackageManagerServiceUtils$ePZ6rsJ05hJ2glmOqcq1_jX6J8w() {
    }

    public final int compare(Object obj, Object obj2) {
        return Long.compare(((Package) obj2).getLatestForegroundPackageUseTimeInMills(), ((Package) obj).getLatestForegroundPackageUseTimeInMills());
    }
}
