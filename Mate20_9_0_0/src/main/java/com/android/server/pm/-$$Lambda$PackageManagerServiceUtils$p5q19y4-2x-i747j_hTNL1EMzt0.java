package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerServiceUtils$p5q19y4-2x-i747j_hTNL1EMzt0 implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$PackageManagerServiceUtils$p5q19y4-2x-i747j_hTNL1EMzt0(long j) {
        this.f$0 = j;
    }

    public final boolean test(Object obj) {
        return PackageManagerServiceUtils.lambda$getPackagesForDexopt$5(this.f$0, (Package) obj);
    }
}
