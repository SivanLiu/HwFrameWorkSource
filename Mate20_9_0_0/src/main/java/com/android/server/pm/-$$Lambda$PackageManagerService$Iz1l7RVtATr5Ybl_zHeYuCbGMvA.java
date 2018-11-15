package com.android.server.pm;

import android.content.pm.IPackageDataObserver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerService$Iz1l7RVtATr5Ybl_zHeYuCbGMvA implements Runnable {
    private final /* synthetic */ PackageManagerService f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ IPackageDataObserver f$4;

    public /* synthetic */ -$$Lambda$PackageManagerService$Iz1l7RVtATr5Ybl_zHeYuCbGMvA(PackageManagerService packageManagerService, String str, long j, int i, IPackageDataObserver iPackageDataObserver) {
        this.f$0 = packageManagerService;
        this.f$1 = str;
        this.f$2 = j;
        this.f$3 = i;
        this.f$4 = iPackageDataObserver;
    }

    public final void run() {
        PackageManagerService.lambda$freeStorageAndNotify$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
