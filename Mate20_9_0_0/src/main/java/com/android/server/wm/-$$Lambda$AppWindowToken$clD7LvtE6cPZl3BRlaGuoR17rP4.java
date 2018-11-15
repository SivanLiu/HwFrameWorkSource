package com.android.server.wm;

import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppWindowToken$clD7LvtE6cPZl3BRlaGuoR17rP4 implements Supplier {
    private final /* synthetic */ AppWindowToken f$0;

    public /* synthetic */ -$$Lambda$AppWindowToken$clD7LvtE6cPZl3BRlaGuoR17rP4(AppWindowToken appWindowToken) {
        this.f$0 = appWindowToken;
    }

    public final Object get() {
        return this.f$0.makeChildSurface(null);
    }
}
