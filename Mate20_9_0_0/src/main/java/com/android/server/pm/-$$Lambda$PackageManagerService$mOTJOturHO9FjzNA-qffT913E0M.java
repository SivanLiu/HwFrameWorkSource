package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerService$mOTJOturHO9FjzNA-qffT913E0M implements Runnable {
    private final /* synthetic */ PackageManagerService f$0;
    private final /* synthetic */ Package f$1;
    private final /* synthetic */ Package f$2;
    private final /* synthetic */ ArrayList f$3;

    public /* synthetic */ -$$Lambda$PackageManagerService$mOTJOturHO9FjzNA-qffT913E0M(PackageManagerService packageManagerService, Package packageR, Package packageR2, ArrayList arrayList) {
        this.f$0 = packageManagerService;
        this.f$1 = packageR;
        this.f$2 = packageR2;
        this.f$3 = arrayList;
    }

    public final void run() {
        this.f$0.mPermissionManager.revokeRuntimePermissionsIfGroupChanged(this.f$1, this.f$2, this.f$3, this.f$0.mPermissionCallback);
    }
}
