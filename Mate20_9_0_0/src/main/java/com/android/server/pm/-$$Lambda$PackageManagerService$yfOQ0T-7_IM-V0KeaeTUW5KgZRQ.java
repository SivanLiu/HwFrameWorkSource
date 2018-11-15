package com.android.server.pm;

import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerService$yfOQ0T-7_IM-V0KeaeTUW5KgZRQ implements ThrowingRunnable {
    private final /* synthetic */ PackageManagerService f$0;
    private final /* synthetic */ String[] f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$PackageManagerService$yfOQ0T-7_IM-V0KeaeTUW5KgZRQ(PackageManagerService packageManagerService, String[] strArr, int i) {
        this.f$0 = packageManagerService;
        this.f$1 = strArr;
        this.f$2 = i;
    }

    public final void runOrThrow() {
        this.f$0.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledTelephonyDataServices(this.f$1, this.f$2);
    }
}
