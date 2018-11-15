package com.android.server.slice;

import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SliceManagerService$LkusK1jmu9JKJTiMRWqWxNGEGbY implements Supplier {
    private final /* synthetic */ SliceManagerService f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$SliceManagerService$LkusK1jmu9JKJTiMRWqWxNGEGbY(SliceManagerService sliceManagerService, int i) {
        this.f$0 = sliceManagerService;
        this.f$1 = i;
    }

    public final Object get() {
        return this.f$0.getDefaultHome(this.f$1);
    }
}
