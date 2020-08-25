package com.android.server.pm;

import java.util.function.Predicate;

/* renamed from: com.android.server.pm.-$$Lambda$PackageManagerService$8F_cRTr5jDNmLElQPCCP6cIQBH4  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PackageManagerService$8F_cRTr5jDNmLElQPCCP6cIQBH4 implements Predicate {
    public static final /* synthetic */ $$Lambda$PackageManagerService$8F_cRTr5jDNmLElQPCCP6cIQBH4 INSTANCE = new $$Lambda$PackageManagerService$8F_cRTr5jDNmLElQPCCP6cIQBH4();

    private /* synthetic */ $$Lambda$PackageManagerService$8F_cRTr5jDNmLElQPCCP6cIQBH4() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return PackageManagerService.lambda$unsuspendForNonSystemSuspendingPackages$13((String) obj);
    }
}
