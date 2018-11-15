package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import com.android.server.pm.dex.DexManager;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerServiceUtils$fMBP3pPR7BB2hICieRxkdNG-3H8 implements Predicate {
    private final /* synthetic */ DexManager f$0;

    public /* synthetic */ -$$Lambda$PackageManagerServiceUtils$fMBP3pPR7BB2hICieRxkdNG-3H8(DexManager dexManager) {
        this.f$0 = dexManager;
    }

    public final boolean test(Object obj) {
        return this.f$0.getPackageUseInfoOrDefault(((Package) obj).packageName).isAnyCodePathUsedByOtherApps();
    }
}
