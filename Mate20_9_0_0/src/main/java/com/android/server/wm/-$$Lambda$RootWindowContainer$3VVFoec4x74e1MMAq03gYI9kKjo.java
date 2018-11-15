package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$3VVFoec4x74e1MMAq03gYI9kKjo implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$RootWindowContainer$3VVFoec4x74e1MMAq03gYI9kKjo(int i, boolean z) {
        this.f$0 = i;
        this.f$1 = z;
    }

    public final void accept(Object obj) {
        RootWindowContainer.lambda$setSecureSurfaceState$3(this.f$0, this.f$1, (WindowState) obj);
    }
}
