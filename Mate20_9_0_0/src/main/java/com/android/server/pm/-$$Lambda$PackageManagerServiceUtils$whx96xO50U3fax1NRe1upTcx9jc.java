package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerServiceUtils$whx96xO50U3fax1NRe1upTcx9jc implements Comparator {
    public static final /* synthetic */ -$$Lambda$PackageManagerServiceUtils$whx96xO50U3fax1NRe1upTcx9jc INSTANCE = new -$$Lambda$PackageManagerServiceUtils$whx96xO50U3fax1NRe1upTcx9jc();

    private /* synthetic */ -$$Lambda$PackageManagerServiceUtils$whx96xO50U3fax1NRe1upTcx9jc() {
    }

    public final int compare(Object obj, Object obj2) {
        return Long.compare(((Package) obj).getLatestForegroundPackageUseTimeInMills(), ((Package) obj2).getLatestForegroundPackageUseTimeInMills());
    }
}
