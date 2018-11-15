package com.android.server.slice;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SliceManagerService$EsoJb3dNe0G_qzoQixj72OS5gnw implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$SliceManagerService$EsoJb3dNe0G_qzoQixj72OS5gnw(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return SliceManagerService.lambda$onStopUser$0(this.f$0, (PinnedSliceState) obj);
    }
}
