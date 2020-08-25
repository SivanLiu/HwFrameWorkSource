package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import java.util.function.BiConsumer;

/* renamed from: com.android.server.pm.-$$Lambda$PackageManagerService$tQg409Z6C-tp5FPOVwhjUsdZLWA  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PackageManagerService$tQg409Z6Ctp5FPOVwhjUsdZLWA implements BiConsumer {
    public static final /* synthetic */ $$Lambda$PackageManagerService$tQg409Z6Ctp5FPOVwhjUsdZLWA INSTANCE = new $$Lambda$PackageManagerService$tQg409Z6Ctp5FPOVwhjUsdZLWA();

    private /* synthetic */ $$Lambda$PackageManagerService$tQg409Z6Ctp5FPOVwhjUsdZLWA() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((SharedLibraryInfo) obj).addDependency((SharedLibraryInfo) obj2);
    }
}
