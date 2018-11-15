package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerServiceUtils$QMV-UHbRIK26QMZL5iM27MchX7U implements Predicate {
    public static final /* synthetic */ -$$Lambda$PackageManagerServiceUtils$QMV-UHbRIK26QMZL5iM27MchX7U INSTANCE = new -$$Lambda$PackageManagerServiceUtils$QMV-UHbRIK26QMZL5iM27MchX7U();

    private /* synthetic */ -$$Lambda$PackageManagerServiceUtils$QMV-UHbRIK26QMZL5iM27MchX7U() {
    }

    public final boolean test(Object obj) {
        return ((Package) obj).coreApp;
    }
}
